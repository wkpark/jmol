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

class ScriptManager {

  Viewer viewer;
  Thread queueThread;
  Vector scriptQueue = new Vector();
  boolean useQueue = true;
  
  ScriptManager(Viewer viewer) {
    this.viewer = viewer;
  }

  public void setQueue(boolean TF) {
    useQueue = TF;
    if (!TF)
      clearQueue();
  }
  
  public String addScript(String strScript) {
    return (String) addScript("string", strScript, "", false, false);
  }

  public String addScript(String strScript, boolean isScriptFile, boolean isQuiet) {
    return (String) addScript("String", strScript, "", isScriptFile, isQuiet);
  }

  public synchronized void flushQueue(String command) {
    for (int i = scriptQueue.size(); --i >= 0;) {
      String strScript = (String)(((Vector)scriptQueue.elementAt(i)).elementAt(0));
      if (strScript.indexOf(command) == 0) {
        scriptQueue.removeElementAt(i);
        if (Logger.debugging)
          Logger.debug(scriptQueue.size() + " scripts; removed: " + strScript);
      }
    }
  }

  public Object addScript(String returnType, String strScript,
                          String statusList, boolean isScriptFile,
                          boolean isQuiet) {
    Vector scriptItem = new Vector();
    scriptItem.addElement(strScript);
    scriptItem.addElement(statusList);
    scriptItem.addElement(returnType);
    scriptItem.addElement(isScriptFile ? Boolean.TRUE : Boolean.FALSE);
    scriptItem.addElement(isQuiet ? Boolean.TRUE : Boolean.FALSE);
    
    if (!useQueue) {
      clearQueue();
      viewer.haltScriptExecution();
    }
    scriptQueue.addElement(scriptItem);
    if (Logger.debugging) {
      Logger.debug(scriptQueue.size() + " scripts; added: " + strScript);
    }
    startScriptQueue();
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
    while (queueThread != null) {
      try {
        Thread.sleep(100);
        if (((n++) % 10) == 0)
          if (Logger.debugging) {
            Logger.debug(
                "...scriptManager waiting for queue: " + scriptQueue.size());
          }
      } catch (InterruptedException e) {
      }
    }
  }

  Object runNextScript() {
    if (scriptQueue.size() == 0)
      return null;
    Vector scriptItem = (Vector) scriptQueue.elementAt(0);
    String script = (String) scriptItem.elementAt(0);
    String statusList = (String) scriptItem.elementAt(1);
    String returnType = (String) scriptItem.elementAt(2);
    boolean isScriptFile = ((Boolean) scriptItem.elementAt(3)).booleanValue();
    boolean isQuiet = ((Boolean) scriptItem.elementAt(4)).booleanValue();
    if (Logger.debugging) {
      Logger.debug(scriptQueue.size() + " scripts; running: " + script);
    }
    scriptQueue.removeElement(scriptItem);
    Object returnInfo = runScript(returnType, script, statusList, isScriptFile,
        isQuiet);
    if (scriptQueue.size() == 0) // might have been cleared with an exit
      return null;
    return returnInfo;
  }

    
  private Object runScript(String returnType, String strScript,
                           String statusList, boolean isScriptFile,
                           boolean isQuiet) {
    return viewer.evalStringWaitStatus(returnType, strScript, statusList,
        isScriptFile, isQuiet);
  }

  private void startScriptQueue() {
    if (scriptQueueRunning)
      return;
    scriptQueueRunning = true;
    queueThread = new Thread(new ScriptQueueRunnable());
    queueThread.start();
  }
  
  boolean scriptQueueRunning;
  //int level;
  class ScriptQueueRunnable implements Runnable {
    public void run() {
      while (scriptQueue.size() != 0) {
        runNextScript();
      }
      queueThread = null;
      stop();
    }
    
    public void stop() {
      scriptQueueRunning = false;
      viewer.setSyncDriver(StatusManager.SYNC_ENABLE);
    }
  }
  
  
}
