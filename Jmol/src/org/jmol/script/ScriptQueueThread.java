/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.script;

import javajs.util.Lst;

import org.jmol.api.JmolScriptManager;
import org.jmol.thread.JmolThread;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

public class ScriptQueueThread extends JmolThread {
  /**
   * 
   */
  private ScriptManager scriptManager;
  private boolean startedByCommandThread = false;
  private int pt;

  /**
   * @param scriptManager 
   * @param vwr 
   * @param startedByCommandThread 
   * @param pt 
   * @j2sIgnoreSuperConstructor
   * 
   */
  public ScriptQueueThread(ScriptManager scriptManager, Viewer vwr, boolean startedByCommandThread, int pt) {
    //super();
    setViewer(vwr, "QueueThread" + pt);
    this.scriptManager = scriptManager;
    this.vwr = vwr;
    this.startedByCommandThread = startedByCommandThread;
    this.pt = pt;
  }

  @Override
  protected void run1(int mode) throws InterruptedException {
    while (true) {
      //System.out.println("sqt run1 mode=" + mode);
      switch (mode) {
      case INIT:
        mode = MAIN;
        break;
      case MAIN:
        //System.out.println("sq run1 stopped=" + stopped + " size="+scriptManager.getScriptQueue().size());
        if (stopped || scriptManager.getScriptQueue().size() == 0) {
          mode = FINISH;
          break;
        }
        /*  System.out.println("run while size != 0: " + this + " pt=" + this.pt + " size=" + scriptQueue.size());
        for (int i = 0; i < scriptQueue.size(); i++)
        System.out.println("queue: " + i + " " + scriptQueue.get(i));
        System.out.println("running: " + scriptQueueRunning[0] + " "  + queueThreads[0]);
        System.out.println("running: " + scriptQueueRunning[1] + " "  + queueThreads[1]);
        */
        if (!runNextScript() && !runSleep(100, MAIN)) {
          //System.out.println("sq run1 return1");
          return;
        }
        break;
      case FINISH:
        //System.out.println("sq run1 finished");
        scriptManager.queueThreadFinished(pt);
        return;
      }
    }
  }

  private boolean runNextScript() {
    Lst<Lst<Object>> queue = scriptManager.getScriptQueue();
    if (queue.size() == 0) {
      return false;
    }
    //Logger.info("SCRIPT QUEUE BUSY" +  scriptQueue.size());
    Lst<Object> scriptItem = scriptManager.getScriptItem(false, startedByCommandThread);
    if (scriptItem == null) {
      return false; 
    }
    String script = (String) scriptItem.get(0);
    String statusList = (String) scriptItem.get(1);
    String returnType = (String) scriptItem.get(2);
    //boolean isScriptFile = ((Boolean) scriptItem.get(3)).booleanValue();
    boolean isQuiet = ((Boolean) scriptItem.get(3)).booleanValue();
    if (Logger.debugging) {
      Logger.debug("Queue[" + pt + "][" + queue.size()
          + "] scripts; running: " + script);
    }
    queue.removeItemAt(0);
//    if (isScriptFile) {
//      script = "script " + PT.esc(script);
//      isScriptFile = false;
//    }

    vwr.evalStringWaitStatusQueued(returnType, script, statusList, isQuiet, true);
    if (queue.size() == 0) {// might have been cleared with an exit
      return false;
    }
    return true;
  }


}