/* $RCSfile$
 * $Author: Maria Brandl$
 * $10.10.2005$
 * $Revision$
 *
 * Copyright (C) 2000-2006  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *  
 *
 *  JmolExportExample.java
 *  event tracking in a Jmol-application following
 *  Bob Hanson's concept 
 *  compiled with: javac -classpath `pwd`:/where/ever/jmol-10.2.0/Jmol.jar JmolExportExample.java
 *  ran with: java -classpath `pwd`:/where/ever/jmol-10.2.0/Jmol.jar JmolExportExample
 * */

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.openscience.jmol.app.Jmol;

public class Export {
  
  public static void main(String args[]) {
    // build TextField (monitor) to track atom info
    JFrame monitorFrame = new JFrame();
    JTextField monitor = new JTextField("Please load a molecule and click on atoms");
    monitorFrame.getContentPane().add(monitor);
    monitorFrame.pack();

    // build Jmol
    JFrame baseframe = new JFrame();
    Jmol jmolPanel = Jmol.getJmol(baseframe,300, 300, "");
    JmolViewer viewer = jmolPanel.viewer;

    // build and register event listener (implementation of JmolStatusListener)
    // point "monitor"-variable in event listener to "monitor"
    MyStatusListener myStatusListener = new MyStatusListener();
    myStatusListener.monitor = monitor;
    viewer.setJmolStatusListener(myStatusListener);

    // showing monitor frame on top of Jmol
    monitorFrame.setVisible(true);
  } 
} 

class MyStatusListener implements JmolStatusListener {
  // JTextField monitor used to broadcast atom tracking out of Jmol
  public JTextField monitor;

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyFileLoaded(java.lang.String, java.lang.String, java.lang.String, java.lang.Object, java.lang.String)
   */
  public void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile,
                               String errorMessage) {
    System.out.println("loaded " + fileName);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyScriptTermination(java.lang.String, int)
   */
  public void notifyScriptTermination(String statusMessage, int msWalltime) {
    System.out.println(statusMessage);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#handlePopupMenu(int, int)
   */
  public void handlePopupMenu(int x, int y) {
    System.out.println("");
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyAtomPicked(int, java.lang.String)
   */
  public void notifyAtomPicked(int atomIndex, String strInfo) {
    System.out.println(strInfo);
    monitor.setText(strInfo);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#showUrl(java.lang.String)
   */
  public void showUrl(String url) {
    System.out.println(url);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#showConsole(boolean)
   */
  public void showConsole(boolean showConsole) {
    System.out.println("Status of Console " + showConsole);
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#createImage(java.lang.String, java.lang.String, int)
   */
  public void createImage(String file, String type, int quality) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#functionXY(java.lang.String, int, int)
   */
  public float functionXY(String functionName, int x, int y) {
    return 0;
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyAtomHovered(int, java.lang.String)
   */
  public void notifyAtomHovered(int atomIndex, String strInfo) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyNewDefaultModeMeasurement(int, java.lang.String)
   */
  public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyNewPickingModeMeasurement(int, java.lang.String)
   */
  public void notifyNewPickingModeMeasurement(int iatom, String strMeasure) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyScriptStart(java.lang.String, java.lang.String)
   */
  public void notifyScriptStart(String statusMessage, String additionalInfo) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#sendConsoleEcho(java.lang.String)
   */
  public void sendConsoleEcho(String strEcho) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#sendConsoleMessage(java.lang.String)
   */
  public void sendConsoleMessage(String strStatus) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#sendSyncScript(java.lang.String, java.lang.String)
   */
  public void sendSyncScript(String script, String appletName) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#setCallbackFunction(java.lang.String, java.lang.String)
   */
  public void setCallbackFunction(String callbackType, String callbackFunction) {
    //
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#eval(java.lang.String)
   */
  public String eval(String strEval) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.jmol.api.JmolStatusListener#notifyFrameChanged(int, int, int, int, int)
   */
  public void notifyFrameChanged(int frameNo, int fileNo, int modelNo, int firstNo, int LastNo) {
    //
  }
}
