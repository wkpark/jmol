/* $Author: hansonr $
 * $Date: 2010-04-22 13:16:44 -0500 (Thu, 22 Apr 2010) $
 * $Revision: 12904 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.script;

import java.util.Vector;
import java.util.concurrent.Executor;

import org.jmol.util.Logger;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

class ParallelProcessor extends ScriptFunction {

  ParallelProcessor(String name) {
    super(name);
    typeName = "parallel";
    tok = Token.parallel;
  }

  /**
   * the idea here is that the process { ... } command would collect and
   * preprocess the scripts, then pass them here for storage until the end of
   * the parallel block is reached.
   */

  static class Process {
    String processName;
    ScriptContext context;

    Process(String name, ScriptContext context) {
      System.out.println("Creating process " + name);
      processName = name;
      this.context = context;
    }
  }

  Viewer viewer;
  volatile Vector vShapeManagers = null;
  volatile int counter = 0;
  volatile Error error = null;

  public void runAllProcesses(Viewer viewer) {
    if (processes.size() == 0)
      return;
    this.viewer = viewer;
    viewer.setParallel(true);
    vShapeManagers = new Vector();
    error = null;
    counter = 0;
    if (Logger.debugging)
      Logger.debug("running " + processes.size() + " processes on "
          + Viewer.nProcessors + " processesors");

    counter = processes.size();
    for (int i = processes.size(); --i >= 0;) {
      runProcess((Process) processes.remove(0));
    }

    synchronized (this) {
      while (counter > 0) {
        try {
          this.wait();
        } catch (InterruptedException e) {
        }
        if (error != null)
          throw error;
      }
    }
    mergeResults();
    viewer.setParallel(false);
  }

  void mergeResults() {
    try {
      for (int i = 0; i < vShapeManagers.size(); i++)
        viewer.mergeShapes(((ShapeManager) vShapeManagers.get(i)).getShapes());
    } catch (Error e) {
      throw e;
    } finally {
      counter = -1;
      vShapeManagers = null;
    }
  }

  void clearShapeManager(Error er) {
    synchronized (this) {
      this.error = er;
      vShapeManagers = null;
      this.notifyAll();
    }
  }

  Vector processes = new Vector();

  void addProcess(String name, ScriptContext context) {
    processes.add(new Process(name, context));
  }

  class RunProcess implements Runnable {
    Process process;
    Object lock;

    public RunProcess(Process process, Object lock) {
      this.process = process;
      this.lock = lock;
    }

    public void run() {
      try {
        if (error == null) {
          if (Logger.debugging)
            Logger.debug("Running process " + process.processName + " "
                + process.context.pc + " - " + (process.context.pcEnd - 1));
          ShapeManager shapeManager = new ShapeManager(viewer, viewer
              .getModelSet());
          vShapeManagers.add(shapeManager);
          viewer.eval(process.context, shapeManager);
          if (Logger.debugging)
            Logger.debug("Process " + process.processName + " complete");
        }
      } catch (Exception e) {
        e.printStackTrace();
      } catch (Error er) {
        clearShapeManager(er);
      } finally {
        --counter;
        synchronized (lock) {
          lock.notifyAll();
        }
      }
    }
  }

  private void runProcess(final Process process) {
    RunProcess r = new RunProcess(process, this);

    if (viewer.getExecutor() != null
        && viewer.getBooleanProperty("multiProcessor")) {
      ((Executor) viewer.getExecutor()).execute(r);
    } else {
      r.run();
    }
  }
}
