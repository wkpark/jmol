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

import java.util.ArrayList;
import java.util.List;

import org.jmol.thread.CommandWatcherThread;
import org.jmol.thread.ScriptQueueThread;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

public class ScriptManager {

  private Viewer viewer;
  private Thread[] queueThreads = new Thread[2];
  private boolean[] scriptQueueRunning = new boolean[2];
  private CommandWatcherThread commandWatcherThread;

  public List<List<Object>> scriptQueue = new ArrayList<List<Object>>();

  ScriptManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    startCommandWatcher(false);
    interruptQueueThreads();
  }

  //public String addScript(String strScript) {
  //  return (String) addScript("string", strScript, "", false, false);
  //}

  String addScript(String strScript, boolean isScriptFile,
                          boolean isQuiet) {
    return (String) addScript("String", strScript, "", isScriptFile, isQuiet);
  }

  private Object addScript(String returnType, String strScript,
                          String statusList, boolean isScriptFile,
                          boolean isQuiet) {
    /**
     * @j2sNative
     *  this.useCommandWatcherThread = false; 
     *  //return this.viewer.evalStringWaitStatus(returnType, strScript, statusList, isScriptFile, isQuiet, true);
     */
    {}
        
    if (!viewer.usingScriptQueue()) {
      clearQueue();
      viewer.haltScriptExecution();
    }
    if (commandWatcherThread == null && useCommandWatcherThread)
      startCommandWatcher(true);
    if (commandWatcherThread != null && strScript.indexOf("/*SPLIT*/") >= 0) {
      String[] scripts = TextFormat.splitChars(strScript, "/*SPLIT*/");
      for (int i = 0; i < scripts.length; i++)
        addScript(returnType, scripts[i], statusList, isScriptFile, isQuiet);
      return "split into " + scripts.length + " sections for processing";
    }
    boolean useCommandThread = (commandWatcherThread != null && 
        (strScript.indexOf("javascript") < 0 
            || strScript.indexOf("#javascript ") >= 0));
    // scripts with #javascript will be processed at the browser end
    List<Object> scriptItem = new ArrayList<Object>();
    scriptItem.add(strScript);
    scriptItem.add(statusList);
    scriptItem.add(returnType);
    scriptItem.add(isScriptFile ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.add(isQuiet ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.add(Integer.valueOf(useCommandThread ? -1 : 1));
    scriptQueue.add(scriptItem);
    //if (Logger.debugging)
    //  Logger.info("ScriptManager queue size=" + scriptQueue.size() + " scripts; added: " 
      //    + strScript + " " + Thread.currentThread().getName());
    startScriptQueue(false);
    //System.out.println("ScriptManager queue 'pending'");
    return "pending";
  }

  //public int getScriptCount() {
  //  return scriptQueue.size();
  //}

  void clearQueue() {
    scriptQueue.clear();
  }

  void waitForQueue() {
    // just can't do this in JavaScript. 
    // if we are here and it is single-threaded, and there is
    // a script running, then that's a problem.
    
    if (viewer.isSingleThreaded())
      return;
    int n = 0;
    while (queueThreads[0] != null || queueThreads[1] != null) {
      try {
        Thread.sleep(100);
        if (((n++) % 10) == 0)
          if (Logger.debugging) {
            Logger.info("...scriptManager waiting for queue: "
                + scriptQueue.size() + " thread="
                + Thread.currentThread().getName());
          }
      } catch (InterruptedException e) {
      }
    }
  }

  synchronized void flushQueue(String command) {
    for (int i = scriptQueue.size(); --i >= 0;) {
      String strScript = (String) (scriptQueue.get(i).get(0));
      if (strScript.indexOf(command) == 0) {
        scriptQueue.remove(i);
        if (Logger.debugging)
          Logger.debug(scriptQueue.size() + " scripts; removed: " + strScript);
      }
    }
  }

  private void startScriptQueue(boolean startedByCommandWatcher) {
    int pt = (startedByCommandWatcher ? 1 : 0);
    if (scriptQueueRunning[pt])
      return;
    scriptQueueRunning[pt] = true;
    queueThreads[pt] = new ScriptQueueThread(this, viewer,
        startedByCommandWatcher, pt);
    queueThreads[pt].start();
  }

  public List<Object> getScriptItem(boolean watching, boolean isByCommandWatcher) {
    if (viewer.isSingleThreaded() && viewer.queueOnHold)
      return null;
    List<Object> scriptItem = scriptQueue.get(0);
    int flag = (((Integer) scriptItem.get(5)).intValue());
    boolean isOK = (watching ? flag < 0 
        : isByCommandWatcher ? flag == 0
        : flag == 1);
    //System.out.println("checking queue for thread " + (watching ? 1 : 0) + "watching = " + watching + " isbycommandthread=" + isByCommandWatcher + "  flag=" + flag + " isOK = " + isOK + " " + scriptItem.get(0));
    return (isOK ? scriptItem : null);
  }

 private boolean useCommandWatcherThread = false;

  synchronized void startCommandWatcher(boolean isStart) {
    useCommandWatcherThread = isStart;
    if (isStart) {
      if (commandWatcherThread != null)
        return;
      commandWatcherThread = new CommandWatcherThread(viewer, this);
      commandWatcherThread.start();
    } else {
      if (commandWatcherThread == null)
        return;
      clearCommandWatcherThread();
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
   *    0  -- Owned by CommandWatcher; running
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

  void interruptQueueThreads() {
    for (int i = 0; i < queueThreads.length; i++) {
      if (queueThreads[i] != null)
        queueThreads[i].interrupt();
    }
  }

  public void clearCommandWatcherThread() {
    if (commandWatcherThread == null)
      return;
    commandWatcherThread.interrupt();
    commandWatcherThread = null;
  }

  public void queueThreadFinished(int pt) {
    queueThreads[pt].interrupt();
    scriptQueueRunning[pt] = false;
    queueThreads[pt] = null;
    viewer.setSyncDriver(StatusManager.SYNC_ENABLE);
  }

  public void runScriptNow() {
    // from ScriptQueueThread
    if (scriptQueue.size() > 0) {
      List<Object> scriptItem = getScriptItem(true, true);
      if (scriptItem != null) {
        scriptItem.set(5, Integer.valueOf(0));
        startScriptQueue(true);
      }
    }
  }
}
