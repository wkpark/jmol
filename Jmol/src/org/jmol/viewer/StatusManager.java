/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.jmol.api.*;
/**
 * 
 * The StatusManager class handles all details of status reporting, including:
 * 
 * 1) saving the message in a queue that replaces the "callback" mechanism,
 * 2) sending messages off to the console, and
 * 3) delivering messages back to the main Jmol.java class in app or applet
 *    to handle differences in capabilities, including true callbacks.

atomPicked

fileLoaded
fileLoadError
frameChanged

measureCompleted
measurePending
measurePicked

newOrientation 

scriptEcho
scriptError
scriptMessage
scriptStarted
scriptStatus
scriptTerminated

userAction
viewerRefreshed

   
 * Bob Hanson hansonr@stolaf.edu  2/2006
 * 
 */

class StatusManager {

  boolean allowStatusReporting = true;
  
  void setAllowStatusReporting(boolean TF){
     allowStatusReporting = TF;
  }
  
  Viewer viewer;
  JmolStatusListener jmolStatusListener;
  String statusList = "";
  Hashtable messageQueue = new Hashtable();
  int statusPtr = 0;
  static int MAXIMUM_QUEUE_LENGTH = 16;
  
  StatusManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    setStatusFileLoaded(null, null, null, null, null, 0);
  }
  
  synchronized boolean resetMessageQueue(String statusList) {
    boolean isRemove = (statusList.length() > 0 && statusList.charAt(0) == '-');
    boolean isAdd = (statusList.length() > 0 && statusList.charAt(0) == '+');
    String oldList = this.statusList;
    if (isRemove) {
      this.statusList = TextFormat.simpleReplace(oldList, statusList.substring(1,statusList.length()), "");
      messageQueue = new Hashtable();
      statusPtr = 0;
      return true;
    }
    statusList = TextFormat.simpleReplace(statusList, "+", "");
    if(oldList.equals(statusList) 
        || isAdd && oldList.indexOf(statusList) >= 0)
      return false;
    if (! isAdd) {
      messageQueue = new Hashtable();
      statusPtr = 0;
      this.statusList = "";
    }
    this.statusList += statusList;
    if (Logger.debugging) {
      Logger.debug(oldList + "\nmessageQueue = " + this.statusList);
    }
    return true;
  }

  synchronized void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }
  
  synchronized boolean setStatusList(String statusList) {
    return resetMessageQueue(statusList);
  }

  Hashtable htCallbacks = new Hashtable();
  synchronized void setCallbackFunction(String callbackType,
                                        String callbackFunction) {
    if (callbackFunction == null)
      htCallbacks.remove(callbackType.toLowerCase());
    else if (callbackFunction.toLowerCase().indexOf("script:") == 0)
      htCallbacks.put(callbackType.toLowerCase(), callbackFunction.substring(7));
    // either format is ok; jmolscript: preferred, because that is the same as embedded scripts.
    else if (callbackFunction.toLowerCase().indexOf("jmolscript:") == 0)
      htCallbacks.put(callbackType.toLowerCase(), callbackFunction.substring(11));
    if (jmolStatusListener != null)
      jmolStatusListener.setCallbackFunction(callbackType, callbackFunction);
  }
  
  String getCallbackScript(String callbackType) {
    return (String) htCallbacks.get(callbackType);
  }
  
  synchronized void setStatusAtomPicked(int atomIndex, String strInfo){
    if (atomIndex == -1) return;
    Logger.info("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex, strInfo);
  }

  synchronized void setStatusResized(int width, int height){
    if (jmolStatusListener != null)
      jmolStatusListener.notifyResized(width, height);
  }

  synchronized void setStatusAtomHovered(int iatom, String strInfo) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyAtomHovered(iatom, strInfo);
  }
  
  synchronized void setStatusFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile, String errorMsg, int ptLoad) {
    setStatusChanged("fileLoaded", ptLoad, fullPathName, false);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", ptLoad, errorMsg, false);
    if (jmolStatusListener != null && (ptLoad <=0 || ptLoad == 3))
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
             modelName, clientFile, errorMsg);
  }

  synchronized void setStatusFrameChanged(int frameNo, int fileNo, int modelNo, int firstNo, int lastNo) {
    if (viewer.getModelSet() == null)
      return;
    boolean isAnimationRunning = (frameNo <= -2);
    int f = frameNo;
    if (isAnimationRunning)
      f = -2 - f;
    setStatusChanged("frameChanged", frameNo, (f >= 0 ? viewer
        .getModelNumberDotted(f) : ""), false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo, fileNo, modelNo, firstNo, lastNo);
  }

  synchronized void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
    setStatusChanged("measurePicked", iatom, strMeasure, false);
    Logger.info("measurePicked " + iatom + " " + strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyNewPickingModeMeasurement(iatom, strMeasure);
  }
  
  synchronized void setStatusNewDefaultModeMeasurement(String status, int count, String strMeasure) {
    setStatusChanged(status, count, strMeasure, false);
    if(status == "measureCompleted") 
      Logger.info("measurement["+count+"] = "+strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyNewDefaultModeMeasurement(count, status + ": " + strMeasure);
  }
  
  synchronized void setStatusScriptStarted(int iscript, String script) {
    setStatusChanged("scriptStarted", iscript, script, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyScriptStart("script " + iscript + " started", script);
  }

  synchronized void setStatusScriptTermination(String statusMessage, int msWalltime){
    statusMessage = "Jmol script terminated";// + (statusMessage != null ? ": " + statusMessage : "");
    if (jmolStatusListener == null)
      return;
    jmolStatusListener.notifyScriptTermination(statusMessage, msWalltime);
  }

  synchronized void setScriptEcho(String strEcho) {
    if (strEcho == null) return; 
    setStatusChanged("scriptEcho", 0, strEcho, false);
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleEcho(strEcho);
  }

  synchronized void setScriptStatus(String strStatus) {
    if (strStatus == null)
      return;
    boolean isError = strStatus.indexOf("ERROR:") >= 0;
    setStatusChanged((isError ? "scriptError" : "scriptStatus"), 0, strStatus,
        false);

    if (isError || strStatus.equals("Script completed"))
      setStatusChanged("scriptTerminated", 1, "Jmol script terminated"
          + (isError ? " unsuccessfully: " + strStatus : " successfully"), false);

    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleMessage(strStatus);
  }
  
  int minSyncRepeatMs = 100;
  synchronized void setStatusViewerRefreshed(int isOrientationChange,
                                             String strWhy) {
    if (isOrientationChange > 0 && isSynced && drivingSync && !syncDisabled) {
      if (Logger.debugging)
        Logger.debug("sending sync");
      if (!viewer.isSyncingScripts())
        syncSend("!" + viewer.getMoveToText(minSyncRepeatMs / 1000f), null);
      else if (isOrientationChange == 2)
        syncSend(strWhy, null);
    }
  }

  synchronized void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  boolean drivingSync = false;
  boolean isSynced = false;
  boolean syncDisabled = false;
  
  final static int SYNC_OFF = 0;
  final static int SYNC_DRIVER = 1;
  final static int SYNC_SLAVE = 2;
  final static int SYNC_DISABLE = 3;
  final static int SYNC_ENABLE = 4;
  
  void setSyncDriver(int syncMode) {
 
    // -1 slave   turn off driving, but not syncing
    //  0 off
    //  1 driving on as driver
    //  2 sync    turn on, but set as slave
    //System.out.println(viewer.getHtmlName() +" setting mode=" + syncMode);
    switch (syncMode) {
    case SYNC_ENABLE:
      if (!syncDisabled)
        return;
      syncDisabled = false;
      break;
    case SYNC_DISABLE:
      syncDisabled = true;
      break;
    case SYNC_DRIVER:
      drivingSync = true;
      isSynced = true;
      break;
    case SYNC_SLAVE:
      drivingSync = false;
      isSynced = true;
      break;
    default:
      drivingSync = false;
      isSynced = false;
    }
    if (Logger.debugging) {
      Logger.debug(
          viewer.getHtmlName() + " sync mode=" + syncMode +
          "; synced? " + isSynced + "; driving? " + drivingSync + "; disabled? " + syncDisabled);
    }
  }

  void syncSend(String script, String appletName) {
    if (jmolStatusListener != null)
      jmolStatusListener.sendSyncScript(script, appletName);
  }
  
  int getSyncMode() {
    return (!isSynced ? SYNC_OFF : drivingSync ? SYNC_DRIVER : SYNC_SLAVE);
  }
  
  synchronized void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  synchronized void clearConsole() {
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleMessage(null);
  }

  synchronized void showConsole(boolean showConsole) {
    if (jmolStatusListener != null)
      jmolStatusListener.showConsole(showConsole);
  }

////////////////////Jmol status //////////////

  synchronized void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (!allowStatusReporting || statusList.length() == 0 
        || statusList != "all" && statusList.indexOf(statusName) < 0)
      return;
    statusPtr++;
    Vector statusRecordSet;
    Vector msgRecord = new Vector();
    msgRecord.addElement(new Integer(statusPtr));
    msgRecord.addElement(statusName);
    msgRecord.addElement(new Integer(intInfo));
    msgRecord.addElement(statusInfo);
    if (isReplace && messageQueue.containsKey(statusName)) {
      messageQueue.remove(statusName);
    }
    if (messageQueue.containsKey(statusName)) {
      statusRecordSet = (Vector)messageQueue.remove(statusName);
    } else {
      statusRecordSet = new Vector();
    }
    if (statusRecordSet.size() == MAXIMUM_QUEUE_LENGTH)
      statusRecordSet.removeElementAt(0);
    
    statusRecordSet.addElement(msgRecord);
    messageQueue.put(statusName, statusRecordSet);
  }
  
  synchronized Vector getStatusChanged(String statusNameList) {
    /*
     * returns a Vector of statusRecordSets, one per status type,
     * where each statusRecordSet is itself a vector of vectors:
     * [int statusPtr,String statusName,int intInfo, String statusInfo]
     * 
     * This allows selection of just the type desired as well as sorting
     * by time overall.
     * 
     */
    Vector msgList = new Vector();
    if (setStatusList(statusNameList)) return msgList;
    Enumeration e = messageQueue.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String statusName = (String)e.nextElement();
      msgList.addElement(messageQueue.remove(statusName));
      n++;
    }
    return msgList;
  }
  
  float[][] functionXY(String functionName, int nX, int nY) {
    return (jmolStatusListener == null ? new float[Math.abs(nX)][Math.abs(nY)] :
      jmolStatusListener.functionXY(functionName, nX, nY));
  }
  
  String eval(String strEval) {
    return (jmolStatusListener == null ? "" : jmolStatusListener.eval(strEval));
  }

  void createImage(String file, Object type_or_text_or_bytes, int quality) {
    if (jmolStatusListener == null)
      return;
    jmolStatusListener.createImage(file, type_or_text_or_bytes, quality);
  }

}

