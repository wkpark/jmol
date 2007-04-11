/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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


import java.awt.Rectangle;
import java.awt.Event;
import org.jmol.util.Logger;
import java.awt.event.*;
import java.awt.Component;

abstract class MouseManager implements KeyListener {

  Viewer viewer;

  Thread hoverWatcherThread;

  int previousDragX, previousDragY;
  int xCurrent, yCurrent;
  long timeCurrent;
  
  boolean measurementMode = false;
  boolean drawMode = false;
  boolean measuresEnabled = true;
  boolean hoverActive = false;

  boolean rubberbandSelectionMode = false;
  int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  private static final boolean logMouseEvents = false;

  MouseManager(Viewer viewer) {
    this.viewer = viewer;
    Component display = viewer.getAwtComponent();
    if (display != null)
      display.addKeyListener(this);
  }
  
  void clear() {
    startHoverWatcher(false);  
  }
  
  synchronized void startHoverWatcher(boolean isStart) {
    if (isStart) {
      if (hoverWatcherThread != null)
        return;
      hoverWatcherThread = new Thread(new HoverWatcher());
      hoverWatcherThread.start();
    } else {
      if (hoverWatcherThread == null)
        return;
      hoverWatcherThread.interrupt();
      hoverWatcherThread = null;
    }
  }

  void removeMouseListeners11() {}
  void removeMouseListeners14() {}

  void setModeMouse(int modeMouse) {
    if (modeMouse == JmolConstants.MOUSE_NONE) {
      startHoverWatcher(false);
      Component display = viewer.getAwtComponent();
      if (display == null)
        return;
      removeMouseListeners11();
      removeMouseListeners14();
      display.removeKeyListener(this);
    }
  }

  public void keyTyped(KeyEvent ke) {
  }

  public void keyPressed(KeyEvent ke) {
    if (!viewer.getNavigationMode())
      return;
    int i = ke.getKeyCode();
    int m = ke.getModifiers();
    if (viewer.getBooleanProperty("showKeyStrokes", false))
      viewer.script("!set echo bottom left;!echo " + (i == 0 ? "" : i + " " + m));
    switch (i) {
    case KeyEvent.VK_UP:
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_LEFT:
    case KeyEvent.VK_RIGHT:
      viewer.navigate(i, m);
      break;
    }
  }
  
  public void keyReleased(KeyEvent ke) {
    if (!viewer.getNavigationMode())
      return;
    if (viewer.getBooleanProperty("showKeyStrokes", false))
      viewer.script("!set echo bottom left;!echo;");
    int i = ke.getKeyCode();
    switch (i) {
    case KeyEvent.VK_UP:
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_LEFT:
    case KeyEvent.VK_RIGHT:
      viewer.navigate(0, 0);
      break;
    }
  }
    
  protected void processKeyEvent(KeyEvent ke) {
    System.out.println("processKeyEvent"+ke);
  }

  Rectangle getRubberBand() {
    if (!rubberbandSelectionMode)
      return null;
    return rectRubber;
  }

  void calcRectRubberBand() {
    if (xCurrent < xAnchor) {
      rectRubber.x = xCurrent;
      rectRubber.width = xAnchor - xCurrent;
    } else {
      rectRubber.x = xAnchor;
      rectRubber.width = xCurrent - xAnchor;
    }
    if (yCurrent < yAnchor) {
      rectRubber.y = yCurrent;
      rectRubber.height = yAnchor - yCurrent;
    } else {
      rectRubber.y = yAnchor;
      rectRubber.height = yCurrent - yAnchor;
    }
  }

  final static long MAX_DOUBLE_CLICK_MILLIS = 700;
  
  final static int LEFT = 16;
  final static int MIDDLE = Event.ALT_MASK;  // 8 note that MIDDLE
  final static int ALT = Event.ALT_MASK;     // 8 and ALT are the same
  final static int RIGHT = Event.META_MASK;  // 4
  final static int CTRL = Event.CTRL_MASK;   // 2
  final static int SHIFT = Event.SHIFT_MASK; // 1
  final static int MIDDLE_RIGHT = MIDDLE | RIGHT;
  final static int LEFT_MIDDLE_RIGHT = LEFT | MIDDLE | RIGHT;
  final static int CTRL_SHIFT = CTRL | SHIFT;
  final static int CTRL_LEFT = CTRL | LEFT;
  final static int CTRL_RIGHT = CTRL | RIGHT;
  final static int CTRL_MIDDLE = CTRL | MIDDLE;
  final static int CTRL_ALT_LEFT = CTRL | ALT | LEFT;
  final static int ALT_LEFT = ALT | LEFT;
  final static int ALT_SHIFT_LEFT = ALT | SHIFT | LEFT;
  final static int SHIFT_LEFT = SHIFT | LEFT;
  final static int CTRL_SHIFT_LEFT = CTRL | SHIFT | LEFT;
  final static int CTRL_ALT_SHIFT_LEFT = CTRL | ALT | SHIFT | LEFT;
  final static int SHIFT_MIDDLE = SHIFT | MIDDLE;
  final static int CTRL_SHIFT_MIDDLE = CTRL | SHIFT | MIDDLE;
  final static int SHIFT_RIGHT = SHIFT | RIGHT;
  final static int CTRL_SHIFT_RIGHT = CTRL | SHIFT | RIGHT;
  final static int CTRL_ALT_SHIFT_RIGHT = CTRL | ALT | SHIFT | RIGHT;
  final static int BUTTON_MODIFIER_MASK =
    CTRL | ALT | SHIFT | LEFT | MIDDLE | RIGHT;

  int previousPressedX, previousPressedY;
  int previousPressedModifiers;
  long previousPressedTime;
  int pressedCount;

  void mousePressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {

    if (previousPressedX == x && previousPressedY == y &&
        previousPressedModifiers == modifiers && 
        (time - previousPressedTime) < MAX_DOUBLE_CLICK_MILLIS) {
      ++pressedCount;
    } else {
      pressedCount = 1;
    }

    hoverOff();
    previousPressedX = previousDragX = xCurrent = x;
    previousPressedY = previousDragY = yCurrent = y;
    previousPressedModifiers = modifiers;
    previousPressedTime = timeCurrent = time;

    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mousePressed("+x+","+y+","+modifiers+
                         " isPopupTrigger=" + isPopupTrigger+")");

    //viewer.setStatusUserAction("mousePressed: " + modifiers);
    
    switch (modifiers & BUTTON_MODIFIER_MASK) {
      /****************************************************************
       * mth 2004 03 17
       * this isPopupTrigger stuff just doesn't work reliably for me
       * and I don't have a Mac to test out CTRL-CLICK behavior
       * Therefore ... we are going to implement both gestures
       * to bring up the popup menu
       * The fact that we are using CTRL_LEFT may 
       * interfere with other platforms if/when we
       * need to support multiple selections, but we will
       * cross that bridge when we come to it
       ****************************************************************/
    case CTRL_LEFT: // on MacOSX this brings up popup
    case RIGHT: // with multi-button mice, this will too
      viewer.popupMenu(x, y);
      return;
    }
  }

  void mouseEntered(long time, int x, int y) {
    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseEntered("+x+","+y+")");
    hoverOff();
    timeCurrent = time;
    xCurrent = x; yCurrent = y;
  }

  void mouseExited(long time, int x, int y) {
    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseExited("+x+","+y+")");
    hoverOff();
    timeCurrent = time;
    xCurrent = x; yCurrent = y;
    exitMeasurementMode();
  }

  void mouseReleased(long time, int x, int y, int modifiers) {
    hoverOff();
    timeCurrent = time;
    xCurrent = x; yCurrent = y;
    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseReleased("+x+","+y+","+modifiers+")");
    viewer.setInMotion(false);
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
  }

  int previousClickX, previousClickY;
  int previousClickModifiers, previousClickCount;
  long previousClickTime;

  void clearClickCount() {
    previousClickX = -1;
  }

  void setMouseMode() {
    drawMode = false;
    measuresEnabled = true;
    switch (viewer.getPickingMode()) {
    case JmolConstants.PICKING_DRAW:
      drawMode = true;
      measuresEnabled = false;
      break;
    //other cases here?
    case JmolConstants.PICKING_LABEL:
    case JmolConstants.PICKING_MEASURE_DISTANCE:
    case JmolConstants.PICKING_MEASURE_ANGLE:
    case JmolConstants.PICKING_MEASURE_TORSION:
      measuresEnabled = false;
      break;
    default:
      return;
    }
    exitMeasurementMode();
  }
  
  void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    // clickCount is not reliable on some platforms
    // so we will just deal with it ourselves
    //viewer.setStatusUserAction("mouseClicked: " + modifiers);
    setMouseMode();
    clickCount = 1;
    if (previousClickX == x && previousClickY == y &&
        previousClickModifiers == modifiers && 
        (time - previousClickTime) < MAX_DOUBLE_CLICK_MILLIS) {
      clickCount = previousClickCount + 1;
    }
    if (!viewer.getAwtComponent().hasFocus())
      viewer.getAwtComponent().requestFocusInWindow();
    hoverOff();
    xCurrent = previousClickX = x; yCurrent = previousClickY = y;
    previousClickModifiers = modifiers;
    previousClickCount = clickCount;
    timeCurrent = previousClickTime = time;

    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseClicked("+x+","+y+","+modifiers+
                         ",clickCount="+clickCount+
                         ",time=" + (time - previousClickTime) +
                         ")");
    if (! viewer.haveFrame())
      return;

    int nearestAtomIndex = (drawMode ? -1 : viewer.findNearestAtomIndex(x, y));
    if (nearestAtomIndex >= 0 && !viewer.isInSelectionSubset(nearestAtomIndex))
        nearestAtomIndex = -1;
    if (clickCount == 1)
      mouseSingleClick(x, y, modifiers, nearestAtomIndex);
    else if (clickCount == 2)
      mouseDoubleClick(x, y, modifiers, nearestAtomIndex);
  }

  void mouseSingleClick(int x, int y, int modifiers, int nearestAtomIndex) {
    //viewer.setStatusUserAction("mouseSingleClick: " + modifiers);
    setMouseMode();
    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseSingleClick(" + x + "," + y + "," + modifiers
          + " nearestAtom=" + nearestAtomIndex);
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      if (viewer.frankClicked(x, y)) {
        viewer.popupMenu(-x, y);
        return;
      }
      if (viewer.getPickingMode() == JmolConstants.PICKING_NAVIGATE) {
        if (viewer.getNavigationMode())
          viewer.navTranslatePercent(0f, x * 100f / viewer.getScreenWidth()
              - 50f, y * 100f / viewer.getScreenHeight() - 50f);
        return;
      }
      if (!viewer.checkObjectClicked(x, y, modifiers, drawMode)) {
        viewer.atomPicked(nearestAtomIndex, modifiers);
        if (measurementMode)
          addToMeasurement(nearestAtomIndex, false);
      }
      break;
    case ALT_LEFT:
    case SHIFT_LEFT:
    case ALT_SHIFT_LEFT:
      if (!drawMode && !viewer.checkObjectClicked(x, y, modifiers, false))
        viewer.atomPicked(nearestAtomIndex, modifiers);
      break;
    }
  }

  void mouseDoubleClick(int x, int y, int modifiers, int nearestAtomIndex) {
    //viewer.setStatusUserAction("mouseDoubleClick: " + modifiers);
    setMouseMode();
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      if (measurementMode) {
        addToMeasurement(nearestAtomIndex, true);
        toggleMeasurement();
      } else if (!drawMode && measuresEnabled) {
        enterMeasurementMode();
        addToMeasurement(nearestAtomIndex, true);
      }
      break;
    case ALT_LEFT:
    case MIDDLE:
    case SHIFT_LEFT:
      if (nearestAtomIndex < 0)
        viewer.script("!reset");
      break;
    }
  }

  void mouseDragged(long time, int x, int y, int modifiers) {
    setMouseMode();
    if (logMouseEvents && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("mouseDragged("+x+","+y+","+modifiers + ")");
    int deltaX = x - previousDragX;
    int deltaY = y - previousDragY;
    hoverOff();
    timeCurrent = time;
    xCurrent = previousDragX = x; yCurrent = previousDragY = y;
    if (!viewer.getInMotion())
      viewer.setCursor(Viewer.CURSOR_MOVE);
    viewer.setInMotion(true);
    if (pressedCount == 1)
      mouseSinglePressDrag(deltaX, deltaY, modifiers);
    else if (pressedCount == 2)
      mouseDoublePressDrag(deltaX, deltaY, modifiers);
  }

  void mouseSinglePressDrag(int deltaX, int deltaY, int modifiers) {
    //viewer.setStatusUserAction("mouseSinglePressDrag: " + modifiers);
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      viewer.rotateXYBy(deltaX, deltaY);
      break;
    case ALT_LEFT:
      if (viewer.allowRotateSelected()) {
        viewer.rotateMolecule(deltaX, deltaY);
        break;
      }
    case SHIFT_LEFT:
    case ALT_SHIFT_LEFT:
      if (drawMode) {
        viewer.checkObjectDragged(previousDragX, previousDragY, deltaX, deltaY,
            modifiers);
        break;
      }
    case MIDDLE:
      //      if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
      if (Math.abs(deltaY) > 5 * Math.abs(deltaX))
        viewer.zoomBy(deltaY);
      //      if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
      if (Math.abs(deltaX) > 5 * Math.abs(deltaY))
        viewer.rotateZBy(-deltaX);
      break;
    case SHIFT_RIGHT: // the one-button Mac folks won't get this gesture
      viewer.rotateZBy(-deltaX);
      break;
    case CTRL_ALT_LEFT:
    /*
     * miguel 2004 11 23
     * CTRL_ALT_LEFT *should* work on the mac
     * however, Apple has a bug in that mouseDragged events
     * do not get passed through if the CTL button is held down
     *
     * I submitted a bug to apple
     */
    case CTRL_RIGHT:
      viewer.translateXYBy(deltaX, deltaY);
      break;
    case CTRL_SHIFT_LEFT:
      if (viewer.getSlabEnabled())
        viewer.slabByPixels(deltaY);
      break;
    case CTRL_ALT_SHIFT_LEFT:
      if (viewer.getSlabEnabled())
        viewer.slabDepthByPixels(deltaY);
    }
  }

  void mouseDoublePressDrag(int deltaX, int deltaY, int modifiers) {
    //viewer.setStatusUserAction("mouseDoublePressDrag: " + modifiers);
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case SHIFT_LEFT:
    case ALT_LEFT:
    case MIDDLE:
      viewer.translateXYBy(deltaX, deltaY);
      break;
    case CTRL_SHIFT_LEFT:
      if (viewer.getSlabEnabled())
        viewer.depthByPixels(deltaY);
      break;
    }
  }

  int mouseMovedX, mouseMovedY;
  long mouseMovedTime;

  void mouseMoved(long time, int x, int y, int modifiers) {
    /*
    if (logMouseEvents)
      Logger.debug("mouseMoved("+x+","+y+","+modifiers"+)");
    */
    hoverOff();
    timeCurrent = mouseMovedTime = time;
    mouseMovedX = xCurrent = x; mouseMovedY = yCurrent = y;
    if (measurementMode || hoverActive) {
      int atomIndex = viewer.findNearestAtomIndex(x, y);
      if (!measurementMode && atomIndex >= 0 && viewer.isInSelectionSubset(atomIndex))
          atomIndex = -1;
      setAttractiveMeasurementTarget(atomIndex);
    }
  }

  final static float wheelClickFractionUp = 1.25f;
  final static float wheelClickFractionDown = 1/wheelClickFractionUp;

  void mouseWheel(long time, int rotation, int modifiers) {
    if (!viewer.getAwtComponent().hasFocus())
      return;  
    // sun bug? noted by Charles Xie that wheeling on a Java page
    // effected inappropriate wheeling on this Java component
    
    hoverOff();
    timeCurrent = time;
    //Logger.debug("mouseWheel time:" + time + " rotation:" + rotation + " modifiers:" + modifiers);
    if (rotation == 0)
      return;
    if ((modifiers & BUTTON_MODIFIER_MASK) == 0) {
      float zoomLevel = viewer.getZoomPercentFloat() / 100f;
      if (rotation > 0) {
        while (--rotation >= 0)
          zoomLevel *= wheelClickFractionUp;
      } else {
        while (++rotation <= 0)
          zoomLevel *= wheelClickFractionDown;
      }
      viewer.zoomToPercent(zoomLevel * 100 + 0.5f);
    }
  }
  

  abstract boolean handleOldJvm10Event(Event e);

  // note that these two may *not* be consistent
  // this term refers to the count of what has actually been selected
  int measurementCount = 0;
  // measurementCountPlusIndices[0] may be one higher if there is
  // an attractive measurement target
  // ie. the cursor is hovering near an atom
  int[] measurementCountPlusIndices = new int[5];

  // the attractive target may be -1
  void setAttractiveMeasurementTarget(int atomIndex) {
    if (measurementCountPlusIndices[0] == measurementCount + 1 &&
        measurementCountPlusIndices[measurementCount + 1] == atomIndex) {
      viewer.refresh(0, "MouseManager:setAttractiveMeasurementTarget("+atomIndex+")");
      return;
    }
    for (int i = measurementCount; i > 0; --i)
      if (measurementCountPlusIndices[i] == atomIndex) {
        viewer.refresh(0, "MouseManager:setAttractiveMeasurementTarget("+atomIndex+")");
        return;
      }
    int attractiveCount = measurementCount + 1;
    measurementCountPlusIndices[0] = attractiveCount;
    measurementCountPlusIndices[attractiveCount] = atomIndex;
    viewer.setPendingMeasurement(measurementCountPlusIndices);
  }

  void addToMeasurement(int atomIndex, boolean dblClick) {
    if (atomIndex == -1) {
      exitMeasurementMode();
      return;
    }
    for (int i = measurementCount; --i >= 0; )
      if (measurementCountPlusIndices[i + 1] == atomIndex) {
        //        exitMeasurementMode();
        return;
      }
    if (measurementCount == 3 && !dblClick)
      return;
    measurementCountPlusIndices[++measurementCount] = atomIndex;
    measurementCountPlusIndices[0] = measurementCount;
    if (measurementCount == 4)
      toggleMeasurement();
    else
      viewer.setPendingMeasurement(measurementCountPlusIndices);
  }

  void exitMeasurementMode() {
    if (!measurementMode)
      return;
    viewer.setPendingMeasurement(null);
    measurementMode = false;
    measurementCount = 0;
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
  }

  void enterMeasurementMode() {
    viewer.setCursor(Viewer.CURSOR_CROSSHAIR);
    measurementCount = 0;
    measurementMode = true;
  }

  void toggleMeasurement() {
    if (measurementCount >= 2 && measurementCount <= 4) {
      measurementCountPlusIndices[0] = measurementCount;
      viewer.script("!" + Measurement.getMeasurementScript(measurementCountPlusIndices));
    }
    exitMeasurementMode();
  }

  void hoverOn(int atomIndex) {
    viewer.hoverOn(atomIndex);
  }

  void hoverOff() {
    viewer.hoverOff();
  }

  class HoverWatcher implements Runnable {
    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      int hoverDelay;
      while (hoverWatcherThread != null
          && (hoverDelay = viewer.getHoverDelay()) > 0) {
        try {
          Thread.sleep(hoverDelay);
          if (xCurrent == mouseMovedX && yCurrent == mouseMovedY
              && timeCurrent == mouseMovedTime) { // the last event was mouse move
            long currentTime = System.currentTimeMillis();
            int howLong = (int) (currentTime - mouseMovedTime);
            if (howLong > hoverDelay) {
              if (!viewer.checkObjectHovered(xCurrent, yCurrent)) {
                int atomIndex = viewer.findNearestAtomIndex(xCurrent, yCurrent);
                if (atomIndex >= 0)
                  hoverOn(atomIndex);
              }
            }
          }
        } catch (InterruptedException ie) {
          Logger.debug("Hover InterruptedException!");
          break;
        } catch (Exception ie) {
          Logger.debug("Hover Exception: " + ie);
          break;
        }
      }
      hoverWatcherThread = null;
    }
  }
}
