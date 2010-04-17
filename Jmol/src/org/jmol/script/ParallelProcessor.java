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
  transient int counter = 0 ;
  

  public void runAllProcesses(Viewer viewer) {
    if (processes.size() == 0)
      return;
    this.viewer = viewer;
    viewer.setParallel(true);
    counter = 0;
    System.out.println("running " + processes.size() + " processes on " + Viewer.nProcessors + " processesors");
    try {
    for (int i = processes.size(); --i >= 0;) {
      counter++;
      runProcess((Process) processes.remove(0));
    }
    while (counter >= 0) {
      Thread.yield();
    }
    } 
    catch (Exception e) {
      // could be memory errors here as well
    }
    viewer.setParallel(false);
  }
  
  void mergeResults() {
    for (int i = 0; i < vShapeManagers.size(); i++)
      viewer.mergeShapes(((ShapeManager) vShapeManagers.get(i)).getShapes());
    vShapeManagers = new Vector();
    counter = -1;
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
    String name;
    Lock(String name) {
      this.name = name;
    }
  }
  
  Hashtable locks = new Hashtable();

  Lock getLock(final String name) {
    if (!locks.containsKey(name))
      locks.put(name, new Lock(name));    
    return (Lock) locks.get(name);
  }
  
  private void runProcess(final Process process) {
    Runnable r = new Runnable() {
      public void run() {
        // not exactly sure what to do about the lock here. 
        //Lock lock = getLock(process.processName);
        //synchronized (lock) {
          try {
            System.out.println("Running process " + process.processName + " " + process.context.pc + " - " + (process.context.pcEnd - 1));
            ShapeManager shapeManager = new ShapeManager(viewer, viewer.getModelSet());
            vShapeManagers.add(shapeManager);
            viewer.eval(process.context, shapeManager);
            System.out.println("Process " + process.processName + " complete");
          } catch (Exception e) {
            e.printStackTrace();
          }
          //lock.depth--;
          if (--counter == 0)
            mergeResults();
        }
      //}
    };

    //Lock lock = getLock(process.processName);
    //while (lock.depth != 0) {
      //Thread.yield();
    //}

    //synchronized (lock) {
      //lock.depth++;
      if (viewer.getExecutor() != null && viewer.getBooleanProperty("multiProcessor")) {
        ((Executor) viewer.getExecutor()).execute(r);
      } else {
        r.run();
      }
    //}
  }
}
