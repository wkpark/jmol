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
 *   
 * Bob Hanson hansonr@stolaf.edu  2/2006
 * 
 */

class StatusManager {

  Viewer viewer;
  JmolStatusListener jmolStatusListener;
  String statusList = "";
  Hashtable messageQueue = new Hashtable();
  int statusPtr = 0;
  static int MAXIMUM_QUEUE_LENGTH = 16;
  String compileError;
  
  StatusManager(Viewer viewer) {
    this.viewer = viewer;
  }

  synchronized boolean resetMessageQueue(String statusList) {
    boolean isRemove = (statusList.charAt(0) == '-');
    boolean isAdd = (statusList.charAt(0) == '+');
    String oldList = this.statusList;
    if (isRemove) {
      this.statusList = viewer.simpleReplace(oldList, statusList.substring(1,statusList.length()), "");
      messageQueue = new Hashtable();
      statusPtr = 0;
      return true;
    }
    statusList = viewer.simpleReplace(statusList, "+", "");
    if(oldList.equals(statusList) 
        || isAdd && oldList.indexOf(statusList) >= 0)
      return false;
    if (! isAdd) {
      messageQueue = new Hashtable();
      statusPtr = 0;
      this.statusList = "";
    }
    this.statusList += statusList;
    System.out.println(oldList + "\nmessageQueue = " + this.statusList);
    return true;
  }

  synchronized void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }
  
  synchronized boolean setStatusList(String statusList) {
    return resetMessageQueue(statusList);
  }
  
  synchronized void setStatusAtomPicked(int atomIndex, String strInfo){
    if (atomIndex < 0) return;
    System.out.println("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex, strInfo);
  }

  synchronized void setStatusFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile, String errorMsg) {
    setStatusChanged("fileLoaded", 0, fullPathName, true);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", 0, errorMsg, true);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
             modelName, clientFile, errorMsg);
  }

  synchronized void setStatusFrameChanged(int frameNo) {
    setStatusChanged("frameChanged", frameNo, (frameNo >=0 ? viewer.getModelName(frameNo) : ""), false);
    //System.out.println("setStatusFrameChanged(" + frameNo +")");
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo);
  }

  synchronized void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
    setStatusChanged("measurePicked", iatom, strMeasure, false);
    System.out.println("measurePicked " + iatom + " " + strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyNewPickingModeMeasurement(iatom, strMeasure);
  }
  
  synchronized void setStatusNewDefaultModeMeasurement(String status, int count, String strMeasure) {
    setStatusChanged(status, count, strMeasure, false);
    if(status == "measureCompleted") 
      System.out.println("measurement["+count+"] = "+strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notityNewDefaultModeMeasurement(count, status + ": " + strMeasure);
  }
  
  synchronized void setStatusScriptStarted(int iscript, String script, String compileError) {
    this.compileError = compileError;
    if (compileError == null)
      compileError = GT._("Jmol executing script ...");
    setStatusChanged("scriptStarted", iscript, script, false);   
    setStatusChanged("scriptMessage", 0, compileError, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyScriptStart(compileError, script); 
  }

  synchronized void setStatusScriptTermination(String statusMessage, int msWalltime){
    statusMessage = "Jmol script completed" + (compileError == null ? "" : ": " + compileError);
    setStatusChanged("scriptTerminated", msWalltime, statusMessage, false);
    if (jmolStatusListener == null)
      return;
    jmolStatusListener.notifyScriptTermination(statusMessage, msWalltime);
  }

  synchronized void setStatusUserAction(String strInfo){
    System.out.println("userAction(" + strInfo + ")");
    setStatusChanged("userAction", 0, strInfo, false);
  }

  synchronized void setStatusViewerRefreshed(int isOrientationChange, String strWhy) {
    if(isOrientationChange == 1){
      setStatusChanged("newOrientation", 0, strWhy, true);   
    } else {
      setStatusChanged("viewerRefreshed", 0, strWhy, false);   
    }
  }

  synchronized void setScriptEcho(String strEcho) {
    if (strEcho == null) return; 
    setStatusChanged("scriptEcho", 0, strEcho, false);
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleEcho(strEcho);
  }

  synchronized void setScriptStatus(String strStatus) {
    if (strStatus == null) return; 
    setStatusChanged((strStatus.indexOf("ERROR:") >= 0 ? "scriptError" : "scriptStatus"), 0, strStatus, false);
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleMessage(strStatus);
  }
  
  synchronized void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  synchronized void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  synchronized void showConsole(boolean showConsole) {
    if (jmolStatusListener != null)
      jmolStatusListener.showConsole(showConsole);
  }

////////////////////Jmol status //////////////

  synchronized void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (statusList != "all" && statusList.indexOf(statusName) < 0)
      return;
    //System.out.println(statusName +"----"+ statusList);
    statusPtr++;
    Vector statusRecordSet;
    Vector msgRecord = new Vector();
    msgRecord.add(new Integer(statusPtr));
    msgRecord.add(statusName);
    msgRecord.add(new Integer(intInfo));
    msgRecord.add(statusInfo);
    if (isReplace && messageQueue.containsKey(statusName)) {
      messageQueue.remove(statusName);
    }
    if (messageQueue.containsKey(statusName)) {
      statusRecordSet = (Vector)messageQueue.remove(statusName);
    } else {
      statusRecordSet = new Vector();
    }
    if (statusRecordSet.size() == MAXIMUM_QUEUE_LENGTH)
      statusRecordSet.remove(0);
    
    statusRecordSet.add(msgRecord);
    messageQueue.put(statusName, statusRecordSet);

    //System.out.println(messageQueue);
  }
  
  synchronized Vector getStatusChanged(String statusNameList) {
    Vector msgList = new Vector();
    if (setStatusList(statusNameList)) return msgList;
    Enumeration e = messageQueue.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String statusName = (String)e.nextElement();
      msgList.add(messageQueue.remove(statusName));
      n++;
    }
    //System.out.println("done with " + n + ": " + msgList);
    return msgList;
  }
}

