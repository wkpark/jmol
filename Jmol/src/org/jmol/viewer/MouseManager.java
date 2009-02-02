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

import org.jmol.modelset.MeasurementPending;
import org.jmol.util.Logger;
import java.awt.event.*;
import java.awt.Component;

import javax.vecmath.Point3f;

public abstract class MouseManager implements KeyListener {

  Viewer viewer;

  Thread hoverWatcherThread;

  int previousDragX, previousDragY;
  int xCurrent = -1000;
  int yCurrent = -1000;
  long timeCurrent = -1;

  boolean drawMode = false;
  boolean measuresEnabled = true;
  MeasurementPending measurementPending;

  boolean hoverActive = false;

  private boolean rubberbandSelectionMode = false;
  int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  abstract boolean handleOldJvm10Event(Event e);

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
    if (viewer.isPreviewOnly())
      return;
    try {
      if (isStart) {
        if (hoverWatcherThread != null)
          return;
        timeCurrent = -1;
        hoverWatcherThread = new Thread(new HoverWatcher());
        hoverWatcherThread.setName("HoverWatcher");
        hoverWatcherThread.start();
      } else {
        if (hoverWatcherThread == null)
          return;
        timeCurrent = -1;
        hoverWatcherThread.interrupt();
        hoverWatcherThread = null;
      }
    } catch (Exception e) {
      // is possible -- seen once hoverWatcherThread.start() had null pointer.
    }
  }

  void removeMouseListeners11() {
  }

  void removeMouseListeners14() {
  }

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
      viewer.script("!set echo bottom left;!echo "
          + (i == 0 ? "" : i + " " + m));
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
    //System.out.println("processKeyEvent"+ke);
  }

  Rectangle getRubberBand() {
    if (!rubberbandSelectionMode || rectRubber.x == Integer.MAX_VALUE)
      return null;
    return rectRubber;
  }

  private void calcRectRubberBand() {
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
  final static int MIDDLE = Event.ALT_MASK; // 8 note that MIDDLE
  final static int ALT = Event.ALT_MASK; // 8 and ALT are the same
  final static int RIGHT = Event.META_MASK; // 4
  final static int CTRL = Event.CTRL_MASK; // 2
  public final static int SHIFT = Event.SHIFT_MASK; // 1
  final static int MIDDLE_RIGHT = MIDDLE | RIGHT;
  final static int LEFT_MIDDLE_RIGHT = LEFT | MIDDLE | RIGHT;
  final static int CTRL_SHIFT = CTRL | SHIFT;
  final static int CTRL_LEFT = CTRL | LEFT;
  final static int CTRL_RIGHT = CTRL | RIGHT;
  final static int CTRL_MIDDLE = CTRL | MIDDLE;
  final static int CTRL_ALT_LEFT = CTRL | ALT | LEFT;
  public final static int ALT_LEFT = ALT | LEFT;
  public final static int ALT_SHIFT_LEFT = ALT | SHIFT | LEFT;
  public final static int SHIFT_LEFT = SHIFT | LEFT;
  final static int CTRL_SHIFT_LEFT = CTRL | SHIFT | LEFT;
  final static int CTRL_ALT_SHIFT_LEFT = CTRL | ALT | SHIFT | LEFT;
  final static int SHIFT_MIDDLE = SHIFT | MIDDLE;
  final static int CTRL_SHIFT_MIDDLE = CTRL | SHIFT | MIDDLE;
  final static int SHIFT_RIGHT = SHIFT | RIGHT;
  final static int CTRL_SHIFT_RIGHT = CTRL | SHIFT | RIGHT;
  final static int CTRL_ALT_SHIFT_RIGHT = CTRL | ALT | SHIFT | RIGHT;
  public final static int BUTTON_MODIFIER_MASK = CTRL | ALT | SHIFT | LEFT
      | MIDDLE | RIGHT;

  int previousPressedX, previousPressedY;
  int previousPressedModifiers;
  long previousPressedTime;
  int pressedCount;

  int mouseMovedX, mouseMovedY;
  long mouseMovedTime;

  void mouseMoved(long time, int x, int y, int modifiers) {
    hoverOff();
    if (hoverWatcherThread == null)
      startHoverWatcher(true);
    timeCurrent = mouseMovedTime = time;
    mouseMovedX = xCurrent = x;
    mouseMovedY = yCurrent = y;
    if (measurementPending != null || hoverActive)
      checkPointOrAtomClicked(x, y, 0, 0);
  }

  final static float wheelClickFractionUp = 1.15f;
  final static float wheelClickFractionDown = 1 / wheelClickFractionUp;

  void mouseWheel(long time, int rotation, int modifiers) {
    if (!viewer.getAwtComponent().hasFocus())
      return;
    // sun bug? noted by Charles Xie that wheeling on a Java page
    // effected inappropriate wheeling on this Java component

    hoverOff();
    timeCurrent = time;
    if (rotation == 0)
      return;
    if ((modifiers & BUTTON_MODIFIER_MASK) == 0) {
      float zoomFactor = 1f;
      if (rotation > 0)
        while (--rotation >= 0)
          zoomFactor *= wheelClickFractionUp;
      else
        while (++rotation <= 0)
          zoomFactor *= wheelClickFractionDown;
      viewer.zoomByFactor(zoomFactor);
    }
  }

  void mousePressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {

    if (previousPressedX == x && previousPressedY == y
        && previousPressedModifiers == modifiers
        && (time - previousPressedTime) < MAX_DOUBLE_CLICK_MILLIS) {
      ++pressedCount;
    } else {
      pressedCount = 1;
    }

    hoverOff();
    xAnchor = previousPressedX = previousDragX = xCurrent = x;
    yAnchor = previousPressedY = previousDragY = yCurrent = y;
    previousPressedModifiers = modifiers;
    previousPressedTime = timeCurrent = time;

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
      break;
    case ALT_LEFT:
    case SHIFT_LEFT:
    case ALT_SHIFT_LEFT:
      if (drawMode)
        viewer.checkObjectDragged(Integer.MIN_VALUE, 0, x, y, modifiers);
      break;
    }
  }

  void mouseEntered(long time, int x, int y) {
    hoverOff();
    timeCurrent = time;
    xCurrent = x;
    yCurrent = y;
  }

  void mouseExited(long time, int x, int y) {
    hoverOff();
    timeCurrent = time;
    xCurrent = x;
    yCurrent = y;
    exitMeasurementMode();
  }

  void mouseReleased(long time, int x, int y, int modifiers) {
    hoverOff();
    timeCurrent = time;
    xCurrent = x;
    yCurrent = y;
    viewer.setInMotion(false);
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
    if (rubberbandSelectionMode
        && ((modifiers & BUTTON_MODIFIER_MASK) == SHIFT_LEFT)) {
      viewer.selectRectangle(rectRubber, modifiers);
      viewer.refresh(3, "mouseReleased");
    }
    rubberbandSelectionMode = false;
    rectRubber.x = Integer.MAX_VALUE;
    if (drawMode)
      switch (modifiers & BUTTON_MODIFIER_MASK) {
      case ALT_LEFT:
      case SHIFT_LEFT:
      case ALT_SHIFT_LEFT:
        viewer.checkObjectDragged(Integer.MAX_VALUE, 0, x, y, modifiers);
        break;
      }
  }

  int previousClickX, previousClickY;
  int previousClickModifiers, previousClickCount;
  long previousClickTime;

  void clearClickCount() {
    previousClickX = -1;
  }

  void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    // clickCount is not reliable on some platforms
    // so we will just deal with it ourselves
    //viewer.setStatusUserAction("mouseClicked: " + modifiers);
    setMouseMode();
    clickCount = 1;
    if (previousClickX == x && previousClickY == y
        && previousClickModifiers == modifiers
        && (time - previousClickTime) < MAX_DOUBLE_CLICK_MILLIS) {
      clickCount = previousClickCount + 1;
    }
    if (!viewer.getAwtComponent().hasFocus())
      viewer.getAwtComponent().requestFocusInWindow();
    hoverOff();
    xCurrent = previousClickX = x;
    yCurrent = previousClickY = y;
    previousClickModifiers = modifiers;
    previousClickCount = clickCount;
    timeCurrent = previousClickTime = time;
    if (viewer.haveModelSet())
      checkPointOrAtomClicked(x, y, modifiers, clickCount);
  }

  void setMouseMode() {
    drawMode = false;
    rubberbandSelectionMode = (viewer.getPickingStyle() == JmolConstants.PICKINGSTYLE_SELECT_DRAG);
    measuresEnabled = true;
    switch (viewer.getPickingMode()) {
    default:
      return;
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
    }
    exitMeasurementMode();
  }

  private void checkPointOrAtomClicked(int x, int y, int modifiers,
                                       int clickCount) {
    // points are always picked up first, then atoms
    // so that atom picking can be superceded by draw picking
    Point3f nearestPoint = (drawMode ? null : viewer.checkObjectClicked(x, y,
        modifiers));
    int nearestAtomIndex = (drawMode || nearestPoint != null ? -1 : viewer
        .findNearestAtomIndex(x, y));
    if (nearestAtomIndex >= 0 && (clickCount > 0 || measurementPending == null)
        && !viewer.isInSelectionSubset(nearestAtomIndex))
      nearestAtomIndex = -1;
    switch (clickCount) {
    case 0:
      // mouse move
      if (measurementPending == null)
        return;
      if (nearestPoint != null
          || measurementPending.getIndexOf(nearestAtomIndex) == 0)
        measurementPending.addPoint(nearestAtomIndex, nearestPoint, false);
      if (measurementPending.haveModified())
        viewer.setPendingMeasurement(measurementPending);
      viewer.refresh(3, "measurementPending");
      return;
    case 1:
      // mouse single click
      setMouseMode();
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
        viewer.atomPicked(nearestAtomIndex, nearestPoint, modifiers);
        if (measurementPending != null)
          if (addToMeasurement(nearestAtomIndex, nearestPoint, false) == 4) {
            previousClickCount = 0;
            toggleMeasurement();
          }
        break;
      case ALT_LEFT:
      case SHIFT_LEFT:
      case ALT_SHIFT_LEFT:
        if (!drawMode)
          viewer.atomPicked(nearestAtomIndex, nearestPoint, modifiers);
        break;
      }
      return;
    case 2:
      // mouse double click
      setMouseMode();
      switch (modifiers & BUTTON_MODIFIER_MASK) {
      case LEFT:
        if (measurementPending != null) {
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
          toggleMeasurement();
        } else if (!drawMode && measuresEnabled) {
          enterMeasurementMode();
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
        }
        break;
      case ALT_LEFT:
      case MIDDLE:
      case SHIFT_LEFT:
        if (nearestAtomIndex < 0)
          viewer.script("!reset");
        break;
      }
      return;
    }
  }

  void mouseDragged(long time, int x, int y, int modifiers) {
    setMouseMode();
    int deltaX = x - previousDragX;
    int deltaY = y - previousDragY;
    hoverOff();
    timeCurrent = time;
    xCurrent = previousDragX = x;
    yCurrent = previousDragY = y;

    switch (pressedCount) {
    case 2:
      //viewer.setStatusUserAction("mouseDoublePressDrag: " + modifiers);
      switch (modifiers & BUTTON_MODIFIER_MASK) {
      case SHIFT_LEFT:
      case ALT_LEFT:
      case MIDDLE:
        checkMotion();
        viewer.translateXYBy(deltaX, deltaY);
        return;
      case CTRL_SHIFT_LEFT:
        if (viewer.getSlabEnabled())
          viewer.depthByPixels(deltaY);
        return;
      }
      return;
    case 1:
      switch (modifiers & BUTTON_MODIFIER_MASK) {
      case LEFT:
        checkMotion();
        viewer.rotateXYBy(deltaX, deltaY);
        return;
      case ALT_LEFT:
        if (viewer.allowRotateSelected()) {
          checkMotion();
          viewer.rotateMolecule(deltaX, deltaY);
          return;
        }
      case SHIFT_LEFT:
      case ALT_SHIFT_LEFT:
        if (drawMode) {
          checkMotion();
          viewer.checkObjectDragged(previousDragX, previousDragY, x, y,
              modifiers);
          return;
        } else if (rubberbandSelectionMode) {
          calcRectRubberBand();
          viewer.refresh(3, "mouse-drag selection");
          return;
        }
      case MIDDLE:
        //      if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
        if (Math.abs(deltaY) > 5 * Math.abs(deltaX))
          checkMotion();
        viewer.zoomBy(deltaY);
        //      if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
        if (Math.abs(deltaX) > 5 * Math.abs(deltaY))
          checkMotion();
        viewer.rotateZBy(-deltaX);
        return;
      case SHIFT_RIGHT: // the one-button Mac folks won't get this gesture
        checkMotion();
        viewer
            .rotateZBy((Math.abs(deltaY) > 5 * Math.abs(deltaX) ? (xCurrent < viewer
                .getScreenWidth() / 2 ? deltaY : -deltaY)
                : 0)
                + (yCurrent > viewer.getScreenHeight() / 2 ? deltaX : -deltaX));
        return;
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
        checkMotion();
        viewer.translateXYBy(deltaX, deltaY);
        return;
      case CTRL_SHIFT_LEFT:
        if (viewer.getSlabEnabled())
          viewer.slabByPixels(deltaY);
        return;
      case CTRL_ALT_SHIFT_LEFT:
        if (viewer.getSlabEnabled())
          viewer.slabDepthByPixels(deltaY);
        return;
      }
    }
  }

  void checkMotion() {
    if (!viewer.getInMotion())
      viewer.setCursor(Viewer.CURSOR_MOVE);
    viewer.setInMotion(true);
  }

  private int addToMeasurement(int atomIndex, Point3f nearestPoint,
                               boolean dblClick) {
    if (atomIndex == -1 && nearestPoint == null) {
      exitMeasurementMode();
      return 0;
    }
    int measurementCount = measurementPending.getCount();
    return (measurementCount == 4 && !dblClick ? measurementCount
        : measurementPending.addPoint(atomIndex, nearestPoint, true));
  }

  private void enterMeasurementMode() {
    viewer.setCursor(Viewer.CURSOR_CROSSHAIR);
    viewer.setPendingMeasurement(measurementPending = new MeasurementPending(
        viewer.getModelSet()));
  }

  private void exitMeasurementMode() {
    if (measurementPending == null)
      return;
    viewer.setPendingMeasurement(measurementPending = null);
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
  }

  private void toggleMeasurement() {
    if (measurementPending == null)
      return;
    int measurementCount = measurementPending.getCount();
    if (measurementCount >= 2 && measurementCount <= 4) {
      viewer.script("!measure " + measurementPending.getMeasurementScript(" "));
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
      try {
        while (hoverWatcherThread != null
            && (hoverDelay = viewer.getHoverDelay()) > 0) {
          Thread.sleep(hoverDelay);
          if (xCurrent == mouseMovedX && yCurrent == mouseMovedY
              && timeCurrent == mouseMovedTime) { // the last event was mouse move
            long currentTime = System.currentTimeMillis();
            int howLong = (int) (currentTime - mouseMovedTime);
            if (howLong > hoverDelay) {
              if (hoverWatcherThread != null && !viewer.getInMotion()
                  && !viewer.getSpinOn()
                  && !viewer.checkObjectHovered(xCurrent, yCurrent)) {
                int atomIndex = viewer.findNearestAtomIndex(xCurrent, yCurrent);
                if (atomIndex >= 0)
                  hoverOn(atomIndex);
              }
            }
          }
        }
      } catch (InterruptedException ie) {
        Logger.debug("Hover InterruptedException!");
      } catch (Exception ie) {
        Logger.debug("Hover Exception: " + ie);
      }
      hoverWatcherThread = null;
    }
  }
}
