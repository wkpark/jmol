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
import org.jmol.i18n.GT;
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

  private boolean allowStatusReporting = true;
  
  void setAllowStatusReporting(boolean TF){
     allowStatusReporting = TF;
  }
  
  private Viewer viewer;
  private JmolStatusListener jmolStatusListener;
  private String statusList = "";
  String getStatusList() {
    return statusList;
  }
  
  private Hashtable messageQueue = new Hashtable();
  Hashtable getMessageQueue() {
    return messageQueue;
  }
  
  private int statusPtr = 0;
  private static int MAXIMUM_QUEUE_LENGTH = 16;
  private StringBuffer outputBuffer;
  
  StatusManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    viewer.setStatusFileLoaded(0, null, null, null, null);
  }
  
  private synchronized boolean resetMessageQueue(String statusList) {
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
  
  private synchronized boolean setStatusList(String statusList) {
    return resetMessageQueue(statusList);
  }

  private Hashtable htCallbacks = new Hashtable();
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
  
  private boolean notifyEnabled(int type) {
    return jmolStatusListener != null && jmolStatusListener.notifyEnabled(type);
  }
  
  synchronized void setStatusAtomPicked(String sJmol, int atomIndex, String strInfo) {
    if (atomIndex == -1)
      return;
    Logger.info("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_PICK,
          new Object[] { sJmol, strInfo, new Integer(atomIndex) });
  }

  synchronized void setStatusResized(String sJmol, int width, int height){
    if (jmolStatusListener != null && jmolStatusListener.notifyEnabled(JmolConstants.CALLBACK_RESIZE))
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_RESIZE,
          new Object[] { sJmol, new Integer(width), new Integer(height) }); 
  }

  synchronized void setStatusAtomHovered(String sJmol, int iatom, String strInfo) {
    if (notifyEnabled(JmolConstants.CALLBACK_HOVER))
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_HOVER, 
          new Object[] {sJmol, strInfo, new Integer(iatom) });
  }
  
  synchronized void setStatusFileLoaded(String sJmol, String fullPathName, String fileName,
                                        String modelName, String errorMsg,
                                        int ptLoad) {
    setStatusChanged("fileLoaded", ptLoad, fullPathName, false);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", ptLoad, errorMsg, false);
    if (jmolStatusListener != null && (ptLoad <= 0 || ptLoad == 3))
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_LOADSTRUCT,
          new Object[] { sJmol, fullPathName, fileName, modelName, errorMsg });
  }

  synchronized void setStatusFrameChanged(String sJmol, int frameNo, int fileNo, int modelNo, int firstNo, int lastNo) {
    //System.out.println("setStatusFrameChanged modelSet=" + viewer.getModelSet());
    if (viewer.getModelSet() == null)
      return;
    boolean isAnimationRunning = (frameNo <= -2);
    int f = frameNo;
    if (isAnimationRunning)
      f = -2 - f;
    setStatusChanged("frameChanged", frameNo, (f >= 0 ? viewer
        .getModelNumberDotted(f) : ""), false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_ANIMFRAME, 
          new Object[] {sJmol, new int[] {frameNo, fileNo, modelNo, firstNo, lastNo}} );
  }

  synchronized void setScriptEcho(String strEcho,
                                  boolean isScriptQueued) {
    if (strEcho == null)
      return;
    setStatusChanged("scriptEcho", 0, strEcho, false);
    if (outputBuffer != null)
      outputBuffer.append(strEcho);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_ECHO,
          new Object[] { null, strEcho, new Integer(isScriptQueued ? 1 : 0) });
  }

  synchronized void setStatusMeasurePicked(int iatom,
                                           String strMeasure) {
    setStatusChanged("measurePicked", iatom, strMeasure, false);
    Logger.info("measurePicked " + iatom + " " + strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_MEASURE,
          new Object[] { null, strMeasure, new Integer(iatom) });
  }
  
  synchronized void setStatusMeasuring(String status, int count, String strMeasure) {
    setStatusChanged(status, count, strMeasure, false);
    if(status.equals("measureCompleted")) 
      Logger.info("measurement["+count+"] = "+strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_MEASURE, new Object[] { null, strMeasure,  new Integer(count), status });
  }
  
  synchronized void notifyError(String errType, String errMsg) {
    if (jmolStatusListener == null
        || !jmolStatusListener
            .notifyEnabled(JmolConstants.CALLBACK_ERROR))
      return;
    jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_ERROR,
        new Object[] { errType, errMsg, viewer.getShapeErrorState() });
  }
  
  synchronized void notifyMinimizationStatus(String sJmol) {
    if (jmolStatusListener == null
        || !jmolStatusListener
            .notifyEnabled(JmolConstants.CALLBACK_MINIMIZATION))
      return;
    jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_MINIMIZATION,
        new Object[] { sJmol, viewer.getParameter("_minimizationStatus"),
            viewer.getParameter("_minimizationSteps"),
            viewer.getParameter("_minimizationEnergy"),
            viewer.getParameter("_minimizationEnergyDiff"), });
  }
  
  synchronized void setStatusScriptStarted(int iscript, String script) {
    setStatusChanged("scriptStarted", iscript, script, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT, new Object[] { null, 
          "script " + iscript + " started", script, new Integer(-2) });
  }

  synchronized void setScriptStatus(String strStatus, String statusMessage, int msWalltime) {
    if (strStatus == null)
      return;
    boolean isScriptCompletion = (strStatus == Eval.SCRIPT_COMPLETED);
    
    // --------------------------
    // This is for the older status logging business, which isn't recommended.
    
    // note: this next bit is not reliable, because translations can change the text.
    boolean isError = strStatus.indexOf("ERROR:") >= 0;
    setStatusChanged((isError ? "scriptError" : "scriptStatus"), 0, strStatus,
        false);
    if (isError || isScriptCompletion)
      setStatusChanged("scriptTerminated", 1, "Jmol script terminated"
          + (isError ? " unsuccessfully: " + strStatus : " successfully"),
          false);
    
    // ---------------------------
    
   if (jmolStatusListener != null) {
      if (isScriptCompletion && viewer.getMessageStyleChime()
          && viewer.getDebugScript()) {
        jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT,
            new Object[] { null, "script <exiting>", statusMessage, new Integer(-1) });
        strStatus = "Jmol script completed.";
      }
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT,
          new Object[] { null, strStatus, statusMessage,
              new Integer(isScriptCompletion ? -1 : msWalltime) });
    }
  }
  
  private int minSyncRepeatMs = 100;
  boolean syncingScripts = false;
  boolean syncingMouse = false;
  boolean doSync() {
    return (isSynced && drivingSync && !syncDisabled);
  }
  
  synchronized void setSync(String mouseCommand) {
    if (syncingMouse) {
      if (mouseCommand != null)
        syncSend(mouseCommand, "*");
    } else if (!syncingScripts)
      syncSend("!" + viewer.getMoveToText(minSyncRepeatMs / 1000f), "*");
  }

  synchronized void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  boolean drivingSync = false;
  boolean isSynced = false;
  boolean syncDisabled = false;
  boolean stereoSync = false;
  
  final static int SYNC_OFF = 0;
  final static int SYNC_DRIVER = 1;
  final static int SYNC_SLAVE = 2;
  final static int SYNC_DISABLE = 3;
  final static int SYNC_ENABLE = 4;
  final static int SYNC_STEREO = 5;
  
  void setSyncDriver(int syncMode) {
 
    // -1 slave   turn off driving, but not syncing
    //  0 off
    //  1 driving on as driver
    //  2 sync    turn on, but set as slave
    //  5 stereo
    //System.out.println(viewer.getHtmlName() +" setting mode=" + syncMode);
    if (stereoSync && syncMode != SYNC_ENABLE) {
      syncSend(Viewer.SYNC_NO_GRAPHICS_MESSAGE, "*");
      stereoSync = false;
    }
    switch (syncMode) {
    case SYNC_ENABLE:
      if (!syncDisabled)
        return;
      syncDisabled = false;
      break;
    case SYNC_DISABLE:
      syncDisabled = true;
      break;
    case SYNC_STEREO:
      drivingSync = true;
      isSynced = true;
      stereoSync = true;
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
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_SYNC,
          new Object[] { null, script, appletName });
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
      jmolStatusListener.notifyCallback(JmolConstants.CALLBACK_MESSAGE, null);
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
  
  synchronized Object getStatusChanged(String statusNameList) {
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
    if (statusNameList.equals("output")) {
      if (outputBuffer == null) {
        outputBuffer = new StringBuffer();
        return "";
      }
      String s = outputBuffer.toString();
      outputBuffer = null;
      return s;
    }
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

  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return          null (canceled) or a message starting with OK or an error message
   */
  String createImage(String fileName, String type, Object text_or_bytes, int quality) {
    if (jmolStatusListener == null)
      return GT._("File creation failed.");
    if (fileName != null && fileName.startsWith("?")) {
      fileName = fileName.substring(1);
      if (!viewer.isSignedApplet()) {
        fileName = dialogAsk(quality == Integer.MIN_VALUE ? "save" : "saveImage",
            fileName);
        if (fileName == null)
          return null;
      }
    }
    return jmolStatusListener.createImage(fileName, type, text_or_bytes, quality);
  }

  public Hashtable getRegistryInfo() {
    /* 

     //note that the following JavaScript retrieves the registry:
     
        var registry = jmolGetPropertyAsJavaObject("appletInfo").get("registry")
      
     // and the following code then retrieves an array of applets:
     
        var AppletNames = registry.keySet().toArray()
      
     // and the following sends commands to an applet in the registry:
      
        registry.get(AppletNames[0]).script("background white")
        
     */
    return (jmolStatusListener == null ? null : jmolStatusListener.getRegistryInfo());
  }

  public String dialogAsk(String type, String fileName) {
    if (jmolStatusListener != null)
      return jmolStatusListener.dialogAsk(type, fileName);
    return "";
  }

}

