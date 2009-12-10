/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-31 09:22:19 -0500 (Fri, 31 Jul 2009) $
 * $Revision: 11291 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.multitouch.sparshui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3f;

import org.jmol.api.Interface;
import org.jmol.api.JmolGestureServerInterface;
import org.jmol.api.JmolMultiTouchAdapter;
import org.jmol.api.JmolMultiTouchClient;
import org.jmol.util.Logger;
import org.jmol.viewer.ActionManagerMT;
import org.jmol.viewer.Viewer;

import com.sparshui.client.SparshClient;
import com.sparshui.client.ClientServerConnection;
import com.sparshui.common.Event;
import com.sparshui.common.Location;
import com.sparshui.common.NetworkConfiguration;
import com.sparshui.common.messages.events.DragEvent;
import com.sparshui.common.messages.events.FlickEvent;
import com.sparshui.common.messages.events.RelativeDragEvent;
import com.sparshui.common.messages.events.RotateEvent;
import com.sparshui.common.messages.events.SpinEvent;
import com.sparshui.common.messages.events.TouchEvent;
import com.sparshui.common.messages.events.ZoomEvent;

public class JmolSparshClientAdapter implements SparshClient, JmolMultiTouchAdapter {

  ///
  //
  // see http://code.google.com/p/sparsh-ui/
  // 
  // The JmolSparshClientAdapter fulfills three functions:
  // 
  // 1) initializing a SparshUI network/server connection
  // 2) acting as a SparshUI Client Adapter to communicate with the 
  //    SparshUI server over port 5945
  // 3) translating the server messages from SparshUI-specific
  //    classes to simpler Java classes that ActionManagerMT can use.
  // 
  // The JmolSparshAdapter interface allows the applet to be 
  // modularized, with this package optional (param multiTouchSparshUI true).
  //
  // Bob Hanson 11/2009
  //
  ///
  
  ///////////// sparsh client interaction ////////////////

  private JmolMultiTouchClient actionManager;
  private ClientServerConnection serverConnection;
  private Component display;
  
  public JmolSparshClientAdapter() {
  }

  // methods Jmol needs -- from viewer.ActionManagerMT

  public void dispose() {
    try {
      if (serverConnection != null) {
        serverConnection.close();
        serverConnection.interrupt();
      }
    } catch (Exception e) {
      //
    }
    try {
      if (gestureServer != null) {
        gestureServer.dispose();
      }
    } catch (Exception e) {
      //
    }
  }
  
  private JmolGestureServerInterface gestureServer;
  public void setMultiTouchClient(Viewer viewer, JmolMultiTouchClient client,
                              boolean isSimulation) {
    String err;
    this.display = viewer.getDisplay();
    gestureServer = (JmolGestureServerInterface) Interface
        .getInterface("com.sparshui.server.GestureServer");
    gestureServer.startGestureServer();
    if (true || isSimulation) {
      Logger.info("JmolSparshClientAdapter skipping driver startup");
    } else {
      try {
        String driver = (new File("JmolMultiTouchDriver.exe")).getAbsolutePath();
        Logger.info("JmolSparshClientAdapter starting " + driver);
        Process p = Runtime.getRuntime().exec(driver);
        Logger.info("JmolSparshClientAdapter process " + p);
        //BufferedReader input = new BufferedReader(new InputStreamReader(p
          //  .getInputStream()));
        Thread.sleep(2000);
        //String line = input.readLine();
        //System.out.println(line);
        //input.close();
        Logger.info("JmolSparshClientAdapter successful starting driver process");
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }
    int port = NetworkConfiguration.CLIENT_PORT;
    try {
      actionManager = client; // ActionManagerMT
      serverConnection = new ClientServerConnection("127.0.0.1", this);
      Logger.info("SparshUI connection established at 127.0.0.1 port " + port);
      return;
    } catch (UnknownHostException e) {
      err = e.getMessage();
    } catch (IOException e) {
      err = e.getMessage();
    }
    actionManager = null;
    Logger.error("Cannot create SparshUI connection at 127.0.0.1 port " + port
        + ": " + err);
  }
  
  // methods the Sparsh server needs -- from com.sparshui.client.ClientToServerProtocol
  
  public List getAllowedGestures(int groupID) {
    return (actionManager == null ? null : actionManager.getAllowedGestures(groupID));
  }

  public int getGroupID(Location location) {
    if (actionManager == null)
      return 0;
    fixXY(location.getX(), location.getY());
    return (actionManager == null ? 0 : actionManager.getGroupID(xyTemp.x, xyTemp.y));
  }

  int x0, y0;
  static int screenWidth, screenHeight;
  static {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    screenWidth = screen.width;
    screenHeight = screen.height;
    if (Logger.debugging)
      Logger.info("screen resolution: " + screenWidth + " x " + screenHeight);
  }
  
  /* mouse click:

ActionManagerMT.processEvent groupID=16777100 eventType=3 iData=0 pt=(329.0, 313.0, NaN)
ActionManagerMT.processEvent groupID=16777100 eventType=3 iData=0 pt=(329.0, 313.0, NaN)
ActionManagerMT.processEvent groupID=16777100 eventType=3 iData=1 pt=(329.0, 313.0, NaN)
ActionManagerMT.processEvent groupID=16777100 eventType=6 iData=0 pt=(-1.0, -1.0, 1.0)
ActionManagerMT.processEvent groupID=16777100 eventType=3 iData=1 pt=(329.0, 313.0, NaN)
ActionManagerMT.processEvent groupID=16777100 eventType=6 iData=0 pt=(-1.0, -1.0, 1.0)

   */
  /**
   * Translate the specialized Sparsh UI information into
   * a format that Jmol's ActionManager can understand
   * without any special classes. This allows the applet
   * to modularize the multitouch business into an optional JAR file
   * 
   * @param groupID 
   * @param event 
   * 
   */
  public void processEvent(int groupID, Event event) {
    if (actionManager == null)
      return;
    if (event == null) {
      //use groupID as eventType
      int errorType = groupID;
      switch (errorType) {
      case ActionManagerMT.SERVICE_LOST:
        dispose();
        break;
      case ActionManagerMT.DRIVER_NONE:
        break;
      }
      actionManager.processEvent(-1, errorType, -1, -1, null, -1);
      return;
    }
    int id = 0;
    int iData = 0;
    int type = event.getEventType();
    long time = 0;
    switch (type) {
    case ActionManagerMT.DRAG_EVENT:
      fixXY((((DragEvent) event).getAbsX()), ((DragEvent) event).getAbsY());
      time = ((DragEvent) event).getTime();
      iData = ((DragEvent) event).getNPoints();
      break;
    case ActionManagerMT.RELATIVE_DRAG_EVENT:
      fixXY(((RelativeDragEvent) event).getChangeInX(),
          ((RelativeDragEvent) event).getChangeInY());
      break;
    case ActionManagerMT.ROTATE_EVENT:
      fixXY((((RotateEvent) event).getCenter().getX()), ((RotateEvent) event).getCenter().getY());
      ptTemp.z = ((RotateEvent) event).getRotation();
      break;
    case ActionManagerMT.SPIN_EVENT:
      ptTemp.set(((SpinEvent) event).getRotationX(),
          ((SpinEvent) event).getRotationY(),
          ((SpinEvent) event).getRotationZ());
      break;
    case ActionManagerMT.TOUCH_EVENT:
      id = ((TouchEvent) event).getTouchID();
      fixXY(((TouchEvent) event).getX(), ((TouchEvent) event).getY());
      iData = ((TouchEvent) event).getState();
      time = ((TouchEvent) event).getTime();
      break;
    case ActionManagerMT.ZOOM_EVENT:
      fixXY(((ZoomEvent) event).getCenter().getX(), ((ZoomEvent) event).getCenter().getY());
      ptTemp.z = ((ZoomEvent) event).getScale();
      break;
    case ActionManagerMT.FLICK_EVENT:
      fixXY(((FlickEvent) event).getXdirection(), ((FlickEvent) event).getYdirection());
      ptTemp.z = ((FlickEvent) event).getSpeedLevel();
      break;
    }
    actionManager.processEvent(groupID, type, id, iData, ptTemp, time);
  }

  public void mouseMoved(int x, int y) {
    // for debugging purposes
    //System.out.println("mouseMove " + x + " " + y);
  }

  Point xyTemp = new Point();
  Point3f ptTemp = new Point3f();
  private void fixXY(float x, float y) {
    xyTemp.setLocation(x * screenWidth, y * screenHeight);
    SwingUtilities.convertPointFromScreen(xyTemp, display);
    ptTemp.set(xyTemp.x, xyTemp.y, Float.NaN);
  }
} 
