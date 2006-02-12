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

  String callbackList = "";
  
  Hashtable callbackStatus = new Hashtable();

  int callbackptr = 0;

  StatusManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void resetCallbackStatus(String callbackList) {
    callbackStatus = new Hashtable();
    callbackptr = 0;
    this.callbackList = callbackList;
  }

  void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }
  
  boolean setCallbackList(String callbackList) {
    //System.out.println(this.callbackList+"\n setting "+callbackList);
    
    if (this.callbackList.equals(callbackList))
      return false;
    //System.out.println("true -- resetting");
       
    System.out.println("Setting callback list: "+callbackList);
    resetCallbackStatus(callbackList);
    return true;
  }
  
  void setStatusAtomPicked(int atomIndex, String strInfo){
    if (atomIndex < 0) return;
    System.out.println("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex, strInfo);
  }

  void setStatusFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile, String errorMsg) {
    setStatusChanged("fileLoaded", 0, fullPathName, true);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", 0, errorMsg, true);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
             modelName, clientFile, errorMsg);
  }

  void setStatusFrameChanged(int frameNo) {
    setStatusChanged("frameChanged", frameNo, "", true);
    //System.out.println("setStatusFrameChanged(" + frameNo +")");
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo);
  }

  void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
    setStatusChanged("measurePicked", iatom, strMeasure, false);
    System.out.println("measurePicked " + iatom + " " + strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyNewPickingModeMeasurement(iatom, strMeasure);
  }
  
  void setStatusNewDefaultModeMeasurement(String status, int count, String strMeasure) {
    setStatusChanged(status, count, strMeasure, false);
    if(status == "measureCompleted") 
      System.out.println("measurement["+count+"] = "+strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notityNewDefaultModeMeasurement(count, status + ": " + strMeasure);
  }
  
  void setStatusScriptStarted(int iscript, String script, String strError) {
    if (strError == null)
      strError = GT._("Jmol executing script ...");
    setStatusChanged("scriptStarted", iscript, script, false);   
    setStatusChanged("scriptMessage", 0, strError, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyScriptStart(strError, script);
    
  }

  void setStatusScriptTermination(String statusMessage, int msWalltime){
    if(statusMessage == null) 
      statusMessage = "Jmol script completed";
    setStatusChanged("scriptTerminated", msWalltime, statusMessage, false);
    if (jmolStatusListener == null)
      return;
    jmolStatusListener.notifyScriptTermination(statusMessage, msWalltime);
  }

  void setStatusViewerRefreshed() {
    setStatusChanged("viewerRefreshed", 0, "", true);   
  }
  
  void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  void setScriptEcho(String strEcho) {
    if (strEcho == null) return; 
    setStatusChanged("scriptEcho", 0, strEcho, false);
    // System.out.println("scriptEcho " + strEcho);
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleEcho(strEcho);
  }

  void setScriptStatus(String strStatus) {
    if (strStatus == null) return; 
    setStatusChanged("scriptStatus", 0, strStatus, false);
    // System.out.println("sendConsoleMessage " + strStatus);
    if (jmolStatusListener != null)
      jmolStatusListener.sendConsoleMessage(strStatus);
  }
  
  void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  void showConsole(boolean showConsole) {
    if (jmolStatusListener != null)
      jmolStatusListener.showConsole(showConsole);
  }


////////////////////callback status //////////////

  void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (callbackList != "all" && callbackList.indexOf(statusName) < 0)
      return;
    //System.out.println(statusName +"----"+ callbackList);
    callbackptr++;
    Vector statusRecordSet;
    Vector msgRecord = new Vector();
    msgRecord.add(new Integer(callbackptr));
    msgRecord.add(statusName);
    msgRecord.add(new Integer(intInfo));
    msgRecord.add(statusInfo);
    if (isReplace && callbackStatus.containsKey(statusName)) {
      callbackStatus.remove(statusName);
    }
    if (callbackStatus.containsKey(statusName)) {
      statusRecordSet = (Vector)callbackStatus.remove(statusName);
    } else {
      statusRecordSet = new Vector();
    }
    statusRecordSet.add(msgRecord);
    callbackStatus.put(statusName, statusRecordSet);

    //System.out.println(callbackStatus);
  }
  
  Vector getStatusChanged(String statusNameList) {
    Vector msgList = new Vector();
    if (setCallbackList(statusNameList)) return msgList;
    Enumeration e = callbackStatus.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String statusName = (String)e.nextElement();
      msgList.add(callbackStatus.remove(statusName));
      n++;
    }
    //System.out.println("done with " + n + ": " + msgList);
    return msgList;
  }
}
