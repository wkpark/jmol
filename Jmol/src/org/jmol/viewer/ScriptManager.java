/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-01 16:40:46 -0500 (Mon, 01 May 2006) $
 * $Revision: 5041 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
package org.jmol.viewer;

import java.util.Vector;

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

class ScriptManager {

  Viewer viewer;
  Thread[] queueThreads = new Thread[2];
  boolean[] scriptQueueRunning = new boolean[2];
  Vector scriptQueue = new Vector();
  boolean useQueue = true;
  Thread commandWatcherThread;

  ScriptManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    startCommandWatcher(false);
  }

  public void setQueue(boolean TF) {
    useQueue = TF;
    if (!TF)
      clearQueue();
  }

  public String addScript(String strScript) {
    return (String) addScript("string", strScript, "", false, false);
  }

  public String addScript(String strScript, boolean isScriptFile,
                          boolean isQuiet) {
    return (String) addScript("String", strScript, "", isScriptFile, isQuiet);
  }

  public Object addScript(String returnType, String strScript,
                          String statusList, boolean isScriptFile,
                          boolean isQuiet) {
    if (!useQueue) {
      clearQueue();
      viewer.haltScriptExecution();
    }
    if (commandWatcherThread == null && useCommandWatcherThread)
      startCommandWatcher(true);
    if (commandWatcherThread != null && strScript.indexOf("/*SPLIT*/") >= 0) {
      String[] scripts = TextFormat.split(strScript, "/*SPLIT*/");
      for (int i = 0; i < scripts.length; i++)
        addScript(returnType, scripts[i], statusList, isScriptFile, isQuiet);
      return "split into " + scripts.length + " sections for processing";
    }
    boolean useCommandThread = (commandWatcherThread != null && 
        (strScript.indexOf("javascript") < 0 
            || strScript.indexOf("#javascript ") >= 0));
    // scripts with #javascript will be processed at the browser end
    Vector scriptItem = new Vector();
    scriptItem.addElement(strScript);
    scriptItem.addElement(statusList);
    scriptItem.addElement(returnType);
    scriptItem.addElement(isScriptFile ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.addElement(isQuiet ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.addElement(new Integer(useCommandThread ? -1 : 1));
    scriptQueue.addElement(scriptItem);
    if (Logger.debugging)
      Logger.info(scriptQueue.size() + " scripts; added: " + strScript);
    startScriptQueue(false);
    return "pending";
  }

  public int getScriptCount() {
    return scriptQueue.size();
  }

  public void clearQueue() {
    scriptQueue.clear();
  }

  public void waitForQueue() {
    int n = 0;
    while (queueThreads[0] != null || queueThreads[1] != null) {
      try {
        Thread.sleep(100);
        if (((n++) % 10) == 0)
          if (Logger.debugging) {
            Logger.debug("...scriptManager waiting for queue: "
                + scriptQueue.size());
          }
      } catch (InterruptedException e) {
      }
    }
  }

  public synchronized void flushQueue(String command) {
    for (int i = scriptQueue.size(); --i >= 0;) {
      String strScript = (String) (((Vector) scriptQueue.elementAt(i))
          .elementAt(0));
      if (strScript.indexOf(command) == 0) {
        scriptQueue.removeElementAt(i);
        if (Logger.debugging)
          Logger.debug(scriptQueue.size() + " scripts; removed: " + strScript);
      }
    }
  }

  void startScriptQueue(boolean startedByCommandWatcher) {
    int pt = (startedByCommandWatcher ? 1 : 0);
    if (scriptQueueRunning[pt])
      return;
    //System.out.println("Script Queue["+pt+"] started with size " + scriptQueue.size());
    scriptQueueRunning[pt] = true;
    queueThreads[pt] = new Thread(new ScriptQueueRunnable(
        startedByCommandWatcher, pt));
    queueThreads[pt].start();
  }

  Vector getScriptItem(boolean watching, boolean isByCommandWatcher) {
    Vector scriptItem = (Vector) scriptQueue.elementAt(0);
    int flag = (((Integer) scriptItem.elementAt(5)).intValue());
    boolean isOK = (watching ? flag < 0 : isByCommandWatcher ? flag == 0
        : flag == 1);
    //System.out.println("checking queue for thread " + (watching ? 1 : 0) + "watching = " + watching + " flag=" + flag + " isOK = " + isOK + " " + scriptItem.get(0));
    return (isOK ? scriptItem : null);
  }

  //int level;
  class ScriptQueueRunnable implements Runnable {
    boolean startedByCommandThread = false;
    int pt;

    public ScriptQueueRunnable(boolean startedByCommandThread, int pt) {
      this.startedByCommandThread = startedByCommandThread;
      this.pt = pt;
    }

    public void run() {
      while (scriptQueue.size() != 0) {
        if (!runNextScript())
          try {
            Thread.sleep(100); //cycle for the command watcher thread
          } catch (Exception e) {
            break; //-- interrupt? 
          }
      }
      queueThreads[pt] = null;
      stop();
    }

    public void stop() {
      scriptQueueRunning[pt] = false;
      viewer.setSyncDriver(StatusManager.SYNC_ENABLE);
    }

    private boolean runNextScript() {
      if (scriptQueue.size() == 0)
        return false;
      //Logger.info("SCRIPT QUEUE BUSY" +  scriptQueue.size());
      Vector scriptItem = getScriptItem(false, startedByCommandThread);
      if (scriptItem == null)
        return false;
      String script = (String) scriptItem.elementAt(0);
      String statusList = (String) scriptItem.elementAt(1);
      String returnType = (String) scriptItem.elementAt(2);
      boolean isScriptFile = ((Boolean) scriptItem.elementAt(3)).booleanValue();
      boolean isQuiet = ((Boolean) scriptItem.elementAt(4)).booleanValue();
      if (Logger.debugging) {
        Logger.info("Queue[" + pt + "][" + scriptQueue.size()
            + "] scripts; running: " + script);
      }
      scriptQueue.removeElement(scriptItem);
      runScript(returnType, script, statusList, isScriptFile, isQuiet);
      if (scriptQueue.size() == 0) {// might have been cleared with an exit
        //Logger.info("SCRIPT QUEUE READY", 0);
        return false;
      }
      return true;
    }

    private void runScript(String returnType, String strScript,
                           String statusList, boolean isScriptFile,
                           boolean isQuiet) {
      viewer.evalStringWaitStatus(returnType, strScript, statusList,
          isScriptFile, isQuiet, true);
    }

  }

  boolean useCommandWatcherThread = false;

  synchronized void startCommandWatcher(boolean isStart) {
    useCommandWatcherThread = isStart;
    if (isStart) {
      if (commandWatcherThread != null)
        return;
      commandWatcherThread = new Thread(new CommandWatcher());
      commandWatcherThread.start();
    } else {
      if (commandWatcherThread == null)
        return;
      commandWatcherThread.interrupt();
      commandWatcherThread = null;
    }
    if (Logger.debugging) {
      Logger.info("command watcher " + (isStart ? "started" : "stopped")
          + commandWatcherThread);
    }
  }

  /*
   * CommandWatcher thread handles processing of 
   * command scripts independently of the user thread.
   * This is important for the signed applet, where the
   * thread opening remote files cannot be the browser's,
   * and commands that utilize JavaScript must.
   * 
   * We need two threads for the signed applet, because commands
   * that involve JavaScript -- the "javascript" command or math javascript() --
   * must run on a thread created by the thread generating the applet call.
   * 
   * This CommandWatcher thread, on the other hand, is created by the applet at 
   * start up -- it can cross domains, but it can't run JavaScript. 
   * 
   * The 5th vector position is an Integer flag.
   * 
   *   -1  -- Owned by CommandWatcher; ready for thread assignment
   *    0  -- Owned by CommmadWatcher; running
   *    1  -- Owned by the JavaScript-enabled/browser-limited thread
   * 
   * If the command is to be ignored by the CommandWatcher, the flag is set 
   * to 1. For the watcher, the flag is first set to -1. This means the
   * command watcher owns it, and the standard script thread should
   * ignore it. The current script queue cycles.
   * 
   * if the CommandWatcher sees a -1 in element 5 of the 0 (next) queue position
   * vector, then it says, "That's mine -- I'll take it." It sets the
   * flag to 0 and starts the script queue. When that script queue removes the
   * 0-position item, the previous script queue takes off again and 
   * finishes the run.
   *  
   */

  class CommandWatcher implements Runnable {
    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      int commandDelay = 200;
      while (commandWatcherThread != null) {
        try {
          Thread.sleep(commandDelay);
          if (commandWatcherThread != null) {
            if (scriptQueue.size() > 0) {
              Vector scriptItem = getScriptItem(true, true);
              if (scriptItem != null) {
                scriptItem.setElementAt(new Integer(0), 5);
                startScriptQueue(true);
              }
            }
          }
        } catch (InterruptedException ie) {
          Logger.info("CommandWatcher InterruptedException!");
          break;
        } catch (Exception ie) {
          String s = "script processing ERROR:\n\n" + ie.toString();
          for (int i = 0; i < ie.getStackTrace().length; i++) {
            s += "\n" + ie.getStackTrace()[i].toString();
          }
          Logger.info("CommandWatcher Exception! " + s);
          viewer.showString(s);
          break;
        }
      }
      commandWatcherThread = null;
    }
  }
}
