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
       
    resetCallbackStatus(callbackList);
    return true;
  }
  
  void notifyAtomPicked(int atomIndex, String strInfo){
    if (atomIndex < 0) return;
    System.out.println("notifyAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPick", atomIndex, strInfo, false);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex, strInfo);
  }

  void notifyFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile, String errorMsg) {
    setStatusChanged("fileLoad", 0, fullPathName, true);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", 0, errorMsg, true);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
             modelName, clientFile, errorMsg);
  }

  void notifyFrameChanged(int frameNo) {
    setStatusChanged("frameChange", frameNo, "", true);
    // System.out.println("notifyFrameChanged(" + frameNo +")");

    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo);
  }

  void notifyMeasureSelection(int iatom, String strMeasure) {
    setStatusChanged("measureSelection", iatom, strMeasure, false);
    System.out.println("measureSelection " + iatom + " " + strMeasure);
    scriptStatus(strMeasure);
    if (jmolStatusListener != null)
      jmolStatusListener.notifyMeasureSelection(iatom, strMeasure);
  }
  
  void notifyMeasurementsChanged(String status, int count, String strMeasure) {
    setStatusChanged("measurementsChange", count, strMeasure, false);
    // System.out.println("notifyMeasurementsChanged()");
    if (jmolStatusListener != null)
      jmolStatusListener.notifyMeasurementsChanged(count, status + ": " + strMeasure);
  }
  
  void notifyScriptTermination(String statusMessage, int msWalltime){
    if(statusMessage == null) return;
    setStatusChanged("scriptTermination", msWalltime, statusMessage,
        false);
    // System.out.println("notifyStriptTermination " + errorMessage + " " +
    // msWalltime);
    if (jmolStatusListener == null)
      return;
    jmolStatusListener.notifyScriptTermination(statusMessage, msWalltime);
  }

  void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  void setStatusMessage(String statusMessage){
    if (statusMessage == null) return;
    setStatusChanged("statusMessage", 0, statusMessage, false);
    // System.out.println("setStatusMessage " + statusMessage);
    if (jmolStatusListener != null)
      jmolStatusListener.setStatusMessage(statusMessage);
  }

  void setStatusMessage(String statusMessage, String additionalInfo){
    if (statusMessage == null) return;
    setStatusChanged("statusMessage", 1, statusMessage + "/"
        + additionalInfo, false);
    // System.out.println("setStatusMessage " + statusMessage + " , " +
    // additionalInfo);
    if (jmolStatusListener != null)
      jmolStatusListener.setStatusMessage(statusMessage, additionalInfo);
  }
  
  void scriptEcho(String strEcho) {
    setStatusChanged("scriptEcho", 0, strEcho, false);
    // System.out.println("scriptEcho " + strEcho);
    scriptStatus(strEcho);
    if (jmolStatusListener != null)
      jmolStatusListener.scriptEcho(strEcho);
  }

  void scriptStatus(String strStatus) {
    if (strStatus == null) return; 
    setStatusChanged("scriptStatus", 0, strStatus, false);
    // System.out.println("scriptStatus " + strStatus);
    if (jmolStatusListener != null)
      jmolStatusListener.scriptStatus(strStatus);
  }
  
  void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  void showConsole(boolean showConsole) {
    if (jmolStatusListener != null)
      jmolStatusListener.showConsole(showConsole);
  }


////////////////////callback status --- should be a new class //////////////

  void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (callbackList != "all" && callbackList.indexOf(statusName) < 0)
      return;
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
