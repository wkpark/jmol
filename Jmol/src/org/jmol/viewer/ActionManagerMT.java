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
package org.jmol.viewer;

import java.awt.Component;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Point3f;

import org.jmol.api.Interface;
import org.jmol.api.JmolSparshAdapter;
import org.jmol.api.JmolSparshClient;
import org.jmol.api.JmolTouchSimulatorInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.binding.Binding;

public class ActionManagerMT extends ActionManager implements JmolSparshClient {

  ///////////// sparsh client interaction ////////////////

  JmolSparshAdapter adapter;
  JmolTouchSimulatorInterface simulator;
  int groupID;
  int simulationPhase;
  boolean resetNeeded = true;
  
  ActionManagerMT(Viewer viewer, boolean isSimulated) {
    super(viewer);
    adapter = (JmolSparshAdapter) Interface
    .getOptionInterface("multitouch.sparshui.JmolSparshClientAdapter");
    groupID = ((int) (Math.random() * 0xFFFFFF)) << 4;
    Logger.info("ActionManagerMT SparshUI groupID=" + groupID);
    startSparshUIService(isSimulated);
    setBinding(binding);
  }

  private void startSparshUIService(boolean isSimulated) {
    if (adapter == null)
      return;
    if (simulator != null) { // a restart
      simulator.dispose();
      simulator = null;
    }
    if (isSimulated)
      Logger.error("ActionManagerMT -- for now just using touch simulation.\nPress CTRL-LEFT and then draw two traces on the window.");    

    Component display = viewer.getDisplay();
    adapter.setSparshClient(display, this);
    if (isSimulated) {
      simulator = (JmolTouchSimulatorInterface) Interface
      .getInterface("com.sparshui.inputdevice.JmolTouchSimulator");
      if (simulator != null) {
        Logger.info("ActionManagerMT simulating SparshUI");
        simulator.startSimulator(display);
      }
    }
  }

  protected void setBinding(Binding newBinding) {
    super.setBinding(newBinding);
    if (simulator != null && binding != null) {
      binding.unbind(Binding.CTRL + Binding.LEFT + Binding.SINGLE_CLICK, null);
      binding.bind(Binding.CTRL + Binding.LEFT + Binding.SINGLE_CLICK, ACTION_multiTouchSimulation);
    }
  }

  void clear() {
    // per file load
    simulationPhase = 0;
    resetNeeded = true;
    super.clear();
  }
  
  void dispose() {
    // per applet/application instance
    adapter.dispose();
    if (simulator != null)
      simulator.dispose();
    super.dispose();
  }

  // these must match those in com.sparshui.gestures.GestureTypes
  // reproduced here so there are no references to that code in applet module
  
  public final static int DRAG_GESTURE = 0;
  public final static int MULTI_POINT_DRAG_GESTURE = 1;
  public final static int ROTATE_GESTURE = 2;
  public final static int SPIN_GESTURE = 3;
  public final static int TOUCH_GESTURE = 4;
  public final static int ZOOM_GESTURE = 5;
  public final static int DBLCLK_GESTURE = 6;
  public final static int FLICK_GESTURE = 7;
  public final static int RELATIVE_DRAG_GESTURE = 8;
  public final static int INVALID_GESTURE = 9;
  
  // adaptation to allow user-defined gesture types
  
  public final static String TWO_POINT_GESTURE = "org.jmol.multitouch.sparshui.TwoPointGesture";

  //these must match those in com.sparshui.common.messages.events.EventType
  // reproduced here so there are no references to that code in applet module
  
  public static final int SERVICE_LOST = -1;
  public static final int DRAG_EVENT = 0;
  public static final int ROTATE_EVENT = 1;
  public static final int SPIN_EVENT = 2;
  public final static int TOUCH_EVENT = 3;
  public final static int ZOOM_EVENT = 4;
  public final static int DBLCLK_EVENT = 5;
  public final static int FLICK_EVENT = 6;
  public final static int RELATIVE_DRAG_EVENT = 7;

  private final static String[] eventNames = new String[] {
    "drag", "rotate", "spin", "touch", "zoom",
    "double-click", "flick", "relative-drag",
  };

  
  private static String getEventName(int i) {
    try {
      return eventNames[i];
    } catch (Exception e) {
      return "?";
    }
  }
  
  public List getAllowedGestures(int groupID) {
    if (groupID != this.groupID)
      return null;
    Vector list = new Vector();
    //list.add(new Integer(DRAG_GESTURE));
    //list.add(new Integer(MULTI_POINT_DRAG_GESTURE));
    //list.add(new Integer(SPIN_GESTURE));
    //list.add(new Integer(DBLCLK_GESTURE));
    list.add(TWO_POINT_GESTURE);
    //list.add(new Integer(TOUCH_GESTURE));
    //list.add(new Integer(ZOOM_GESTURE));
    //list.add(new Integer(FLICK_GESTURE));
    //list.add(new Integer(RELATIVE_DRAG_GESTURE));    
    return list;
  }

  public int getGroupID(int x, int y) {
    int gid = (x < 0 || y < 0 || x >= viewer.getScreenWidth()
        || y >= viewer.getScreenHeight() ? 0 : groupID);
    if (resetNeeded) {
      gid |= 0x10000000;
      lastPoint = null;
      resetNeeded = false;
    }
    return gid;
  }

  Point3f lastPoint;
  
  public void processEvent(int groupID, int eventType, int touchID, int iData,
                           Point3f pt, long time) {
    if (true || Logger.debugging)
      Logger.info(this + " time=" + time + " groupID=" + groupID + " "
          + Integer.toHexString(groupID) + " eventType=" + eventType + "("
          + getEventName(eventType) + ") iData=" + iData + " pt=" + pt);
    switch (eventType) {
    case SERVICE_LOST:
      startSparshUIService(simulator != null);  
      break;
    case ZOOM_EVENT:
      float scale = pt.z;
      if (scale == -1 || scale == 1) {
        pt.z = Float.NaN;
        zoomByFactor((int)scale, (int) pt.x, (int) pt.y);
      }
      break;
    case ROTATE_EVENT:
      checkMotion(Viewer.CURSOR_MOVE);
      viewer.rotateZBy((int) pt.z, (int) pt.x, (int) pt.y);
      break;
    case DRAG_EVENT:
      if (iData == 2) {
        // This is a 2-finger drag
        if (lastPoint == null) {
          lastPoint = new Point3f(pt);
          break;
        }
        if (Math.abs(pt.x - lastPoint.x) > Math.abs(pt.y - lastPoint.y) * 5) {
          // horizontal 2-finger drag
          if (pt.x > lastPoint.x + 20) {
            lastPoint.set(pt);
            viewer.evalStringQuiet("frame next");
          } else if (pt.x < lastPoint.x - 20) {
            lastPoint.set(pt);
            viewer.evalStringQuiet("frame previous");
          }
        }
      }
      break;
    case SPIN_EVENT:
      break;
    case TOUCH_EVENT:
      break;
    case DBLCLK_EVENT:
      break;
    case FLICK_EVENT:
      break;
    case RELATIVE_DRAG_EVENT:
      break;
    }

  }

  void mouseEntered(long time, int x, int y) {
    super.mouseEntered(time, x, y);    
  }
  
  void mouseExited(long time, int x, int y) {
    super.mouseExited(time, x, y);    
  }
  
  void mouseClicked(long time, int x, int y, int modifiers) {
    super.mouseClicked(time, x, y, modifiers);
  }

  void mouseMoved(long time, int x, int y, int modifiers) {
    adapter.mouseMoved(x, y);
    super.mouseMoved(time, x, y, modifiers);
  }

  void mouseWheel(long time, int rotation, int mods) {
    super.mouseWheel(time, rotation, mods);
  }

  void mousePressed(long time, int x, int y, int mods) {
    if (simulator != null) {
      int action = Binding.getMouseAction(1, mods);
      if (binding.isBound(action, ACTION_multiTouchSimulation)) {
        setCurrent(0, x, y, mods);
        setFocus();
        if (simulationPhase++ == 0)
          simulator.startRecording();
        simulator.mousePressed(time, x, y);
        return;
      }
      simulationPhase = 0;
    }
    super.mousePressed(time, x, y, mods);
  }

  void mouseDragged(long time, int x, int y, int mods) {
    if (simulator != null && simulationPhase > 0) {
      setCurrent(time, x, y, mods);
      simulator.mouseDragged(time, x, y);
      return;
    }
    super.mouseDragged(time, x, y, mods);
  }

  void mouseReleased(long time, int x, int y, int mods) {
    if (simulator != null && simulationPhase > 0) {
      setCurrent(time, x, y, mods);
      viewer.spinXYBy(0, 0, 0);
      simulator.mouseReleased(time, x, y);
      if (simulationPhase >= 2) {
        // two strokes only
        resetNeeded = true;
        simulator.endRecording();
        simulationPhase = 0;
      }
      return;
    }
    super.mouseReleased(time, x, y, mods);
  }

} 
