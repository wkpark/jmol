package org.jmol.script;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.Executor;

import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.Viewer;

class ParallelProcessor extends ScriptFunction {

  ParallelProcessor(String name) {
    super(name);
    typeName = "parallel";
    tok = Token.parallel;
  }
  
  /**
   * the idea here is that the process { ... } command
   * would collect and preprocess the scripts, then pass them
   * here for storage until the end of the parallel block is reached.
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
  Vector vShapeManagers = new Vector();

  public void runAllProcesses(Viewer viewer) {
    this.viewer = viewer;
    for (int i = 0; i < processes.size(); i++)
      runProcess((Process) processes.get(i));
    for (int i = 0; i < vShapeManagers.size(); i++)
      viewer.mergeShapes(((ShapeManager) vShapeManagers.get(i)).getShapes());
    vShapeManagers = new Vector();
  }

  Vector processes = new Vector();
  void addProcess(String name, ScriptContext context) {
    processes.add(new Process(name, context));
  }

  /**
   * don't really know what to do about the lock
   * 
   */
  
  static class Lock { 
    transient int depth = 0 ;
    int lockID = 0 ;
  }
  
  Hashtable locks = new Hashtable();

  Lock getLock(final String name) {
    if (!locks.containsKey(name))
      locks.put(name, new Lock());    
    return (Lock) locks.get(name);
  }
  
  transient int counter = 0 ;
  
  private void runProcess(final Process process) {
    Runnable r = new Runnable() {
      public void run() {
        // not exactly sure what to do about the lock here. 
        Lock lock = getLock(process.processName);
        synchronized (lock) {
          try {
            System.out.println("Running process " + process.processName + " " + process.context.pc + " - " + (process.context.pcEnd - 1));
            ShapeManager shapeManager = new ShapeManager(viewer, viewer.getModelSet());
            vShapeManagers.add(shapeManager);
            viewer.eval(process.context, shapeManager);
            System.out.println("Process " + process.processName + " complete");
          } catch (Exception e) {
            e.printStackTrace();
          }
          counter--;
          lock.depth--;
        }
      }
    };

    counter++;
    Lock lock = getLock(process.processName);
    while (lock.depth != 0) {
      Thread.yield();
    }

    synchronized (lock) {
      lock.depth++;
      if (viewer.getExecutor() != null && viewer.getTestFlag1()) {
        ((Executor) viewer.getExecutor()).execute(r);
      } else {
        r.run();
      }
    }
  }
}
