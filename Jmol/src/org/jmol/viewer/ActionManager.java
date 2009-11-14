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

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.BitSet;

import org.jmol.i18n.GT;
import org.jmol.modelset.MeasurementPending;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;
import org.jmol.viewer.binding.DragBinding;
import org.jmol.viewer.binding.Binding;
import org.jmol.viewer.binding.PfaatBinding;
import org.jmol.viewer.binding.RasmolBinding;
import org.jmol.viewer.binding.JmolBinding;

public class ActionManager {

  public final static int ACTION_rotateXY = 1;
  public final static int ACTION_rotateZ = 2;
  public final static int ACTION_zoom = 3;
  public final static int ACTION_translateXY = 4;
  public final static int ACTION_rotateMolecule = 5;

  public final static int ACTION_dragSelected = 101;
  public final static int ACTION_dragLabel = 102;
  public final static int ACTION_dragDrawPoint = 103;
  public final static int ACTION_dragDrawObject = 104;
  public final static int ACTION_rubberBandSelectToggle = 105;
  public final static int ACTION_rubberBandSelectOr = 106;
  public final static int ACTION_rubberBandSelectAndNot = 107;

  public final static int ACTION_spinDrawObjectCW = 201;
  public final static int ACTION_spinDrawObjectCCW = 202;

  public final static int ACTION_slab = 301;
  public final static int ACTION_depth = 302;
  public final static int ACTION_slabDepth = 303;

  public final static int ACTION_popupMenu = 401;
  public final static int ACTION_clickFrank = 402;

  public final static int ACTION_navTranslate = 501;

  public final static int ACTION_pickAtom = 601;
  public final static int ACTION_pickPoint = 602;
  public final static int ACTION_pickLabel = 603;
  public final static int ACTION_pickMeasure = 604;
  public final static int ACTION_setMeasure = 605;
  public static final int ACTION_pickIsosurface = 606;
  public static final int ACTION_pickNavigate = 607;
  
  public static final int ACTION_select = 701;
  public static final int ACTION_selectNot = 702;
  public static final int ACTION_selectToggle = 703;  
  public static final int ACTION_selectNone = 704;
  public static final int ACTION_selectAndNot = 705;
  public static final int ACTION_selectOr = 706;

  public final static int ACTION_reset = 999;

  final static float wheelClickFractionUp = 1.15f;
  final static float wheelClickFractionDown = 1 / wheelClickFractionUp;

  
  private final static long MAX_DOUBLE_CLICK_MILLIS = 700;
 
  protected Viewer viewer;
  
  Binding binding;

  ActionManager(Viewer viewer) {
    this.viewer = viewer;
    binding = new JmolBinding();
  }

  public boolean isBound(int gesture, int action) {
    return binding.isBound(gesture, action);
  }

  protected Thread hoverWatcherThread;

  private int previousDragX, previousDragY;
  protected int xCurrent = -1000;
  protected int yCurrent = -1000;
  protected long timeCurrent = -1;

  private boolean drawMode = false;
  private boolean labelMode = false;
  private boolean dragSelectedMode = false;
  private boolean measuresEnabled = true;
  private MeasurementPending measurementPending;

  private boolean hoverActive = false;

  private boolean rubberbandSelectionMode = false;
  private int xAnchor, yAnchor;
  private final Rectangle rectRubber = new Rectangle();

  private int previousClickX, previousClickY;
  private int previousClickModifiers, previousClickCount;
  private long previousClickTime;

  private int previousPressedX, previousPressedY;
  private int previousPressedModifiers;
  private long previousPressedTime;
  private int pressedCount;

  protected int mouseMovedX, mouseMovedY;
  protected long mouseMovedTime;

  boolean isAltKeyReleased = true;  
  private boolean keyProcessing;

  void clear() {
    startHoverWatcher(false);
    pickingMode = JmolConstants.PICKING_IDENT;
    drawHover = false;
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

  void setModeMouse(int modeMouse) {
    if (modeMouse == JmolConstants.MOUSE_NONE) {
      startHoverWatcher(false);
    }
  }

  private int mouseMovedModifiers = Integer.MAX_VALUE;

  public void keyPressed(KeyEvent ke) {
    if (keyProcessing)
      return;
    keyProcessing = true;
    int i = ke.getKeyCode();
    if (i == KeyEvent.VK_ALT) {
      if (dragSelectedMode && isAltKeyReleased)
        viewer.moveSelected(Integer.MIN_VALUE, 0, 0, 0, false);
      isAltKeyReleased = false;
    } else if (i == KeyEvent.VK_SHIFT) {
      mouseMovedModifiers = Binding.SHIFT;
    }
    if (viewer.getNavigationMode()) {
      int m = ke.getModifiers();
      // if (viewer.getBooleanProperty("showKeyStrokes", false))
      // viewer.evalStringQuiet("!set echo bottom left;echo "
      // + (i == 0 ? "" : i + " " + m));
      switch (i) {
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_SPACE:
      case KeyEvent.VK_PERIOD:
        viewer.navigate(i, m);
        break;
      }
    }
    keyProcessing = false;
  }

  public void keyReleased(KeyEvent ke) {
    int i = ke.getKeyCode();
    if (i == KeyEvent.VK_ALT) {
      if (dragSelectedMode)
        viewer.moveSelected(Integer.MAX_VALUE, 0, 0, 0, false);
      isAltKeyReleased = true;
    } else if (i == KeyEvent.VK_SHIFT) {
      mouseMovedModifiers = 0;
    }
    if (!viewer.getNavigationMode())
      return;
    //if (viewer.getBooleanProperty("showKeyStrokes", false))
      //viewer.evalStringQuiet("!set echo bottom left;echo;");
    switch (i) {
    case KeyEvent.VK_UP:
    case KeyEvent.VK_DOWN:
    case KeyEvent.VK_LEFT:
    case KeyEvent.VK_RIGHT:
      viewer.navigate(0, 0);
      break;
    }
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

  void setMouseMode() {
    drawMode = labelMode = false;
    dragSelectedMode = viewer.getBooleanProperty("dragSelected");
    rubberbandSelectionMode = (pickingStyle == JmolConstants.PICKINGSTYLE_SELECT_DRAG);
    measuresEnabled = !dragSelectedMode;
    if (!dragSelectedMode)
      switch (pickingMode) {
      default:
        return;
      case JmolConstants.PICKING_DRAW:
        drawMode = true;
        // drawMode and dragSelectedMode are incompatible
        measuresEnabled = false;
        break;
      //other cases here?
      case JmolConstants.PICKING_LABEL:
        labelMode = true;
        measuresEnabled = false;
        break;
      case JmolConstants.PICKING_MEASURE_DISTANCE:
      case JmolConstants.PICKING_MEASURE_ANGLE:
      case JmolConstants.PICKING_MEASURE_TORSION:
        measuresEnabled = false;
        break;
      }
    exitMeasurementMode();
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
    if (!viewer.getDisplay().hasFocus())
      viewer.getDisplay().requestFocusInWindow();
    hoverOff();
    xCurrent = previousClickX = x;
    yCurrent = previousClickY = y;
    previousClickModifiers = modifiers;
    previousClickCount = clickCount;
    timeCurrent = previousClickTime = time;
    checkPointOrAtomClicked(x, y, modifiers, clickCount);
  }

  protected void mouseMoved(long time, int x, int y, int modifiers) {
    hoverOff();
    timeCurrent = mouseMovedTime = time;
    mouseMovedX = xCurrent = x;
    mouseMovedY = yCurrent = y;
    if (measurementPending != null || hoverActive)
      checkPointOrAtomClicked(x, y, 0, 0);
  }

  void mouseWheel(long time, int rotation, int mods) {
    if (!viewer.getDisplay().hasFocus())
      return;
    // sun bug? noted by Charles Xie that wheeling on a Java page
    // effected inappropriate wheeling on this Java component
    hoverOff();
    timeCurrent = time;
    if (rotation == 0 || Binding.getMouseAction(0, mods) != 0)
      return;
    float zoomFactor = 1f;
    if (rotation > 0)
      while (--rotation >= 0)
        zoomFactor *= wheelClickFractionUp;
    else
      while (++rotation <= 0)
        zoomFactor *= wheelClickFractionDown;
    viewer.zoomByFactor(zoomFactor);
  }

  void mousePressed(long time, int x, int y, int mods,
                    boolean isPopupTrigger) {
    if (previousPressedX == x && previousPressedY == y
        && previousPressedModifiers == mods
        && (time - previousPressedTime) < MAX_DOUBLE_CLICK_MILLIS) {
      ++pressedCount;
    } else {
      pressedCount = 1;
    }

    hoverOff();
    xAnchor = previousPressedX = previousDragX = xCurrent = x;
    yAnchor = previousPressedY = previousDragY = yCurrent = y;
    previousPressedModifiers = mods;
    previousPressedTime = timeCurrent = time;

    int action = Binding.getMouseAction(pressedCount, mods);
    if (Binding.getModifiers(action) != 0) {
      action = viewer.notifyMouseClicked(x, y, action);
      if (action == 0)
        return;
    }    
    if (isBound(action, ACTION_popupMenu)) {
      viewer.popupMenu(x, y);
      return;
    }
    if (drawMode && (
        isBound(action, ACTION_dragDrawObject)
        || isBound(action, ACTION_dragDrawPoint))
      || labelMode && isBound(action, ACTION_dragLabel)) {
      viewer.checkObjectDragged(Integer.MIN_VALUE, 0, x, y, action);
      return;
    }
    if (dragSelectedMode)
      viewer.moveSelected(Integer.MIN_VALUE, 0, 0, 0, false);
  }

  void mouseReleased(long time, int x, int y, int mods) {
    hoverOff();
    timeCurrent = time;
    xCurrent = x;
    yCurrent = y;
    viewer.setInMotion(false);
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
    int action = Binding.getMouseAction(pressedCount, mods);
    if (rubberbandSelectionMode && 
        (  isBound(action, ACTION_rubberBandSelectToggle)
        || isBound(action, ACTION_rubberBandSelectOr)
        || isBound(action, ACTION_rubberBandSelectAndNot)
        )) {
      BitSet bs = viewer.findAtomsInRectangle(rectRubber);
      if (BitSetUtil.firstSetBit(bs) >= 0) {
        String s = Escape.escape(bs);
        if (isBound(action, ACTION_rubberBandSelectOr))
          viewer.script("select selected or " + s);
        else if (isBound(action, ACTION_rubberBandSelectAndNot))
          viewer.script("select selected and not " + s);
        else 
          viewer.script("select selected tog " + s);
      }
      viewer.refresh(3, "mouseReleased");
    }
    rubberbandSelectionMode = false;
    rectRubber.x = Integer.MAX_VALUE;
    if (previousPressedX != x || previousPressedY != y)
      viewer.notifyMouseClicked(x, y, Binding.getMouseAction(pressedCount, 0));
    if (drawMode && (
        isBound(action, ACTION_dragDrawObject)
        || isBound(action, ACTION_dragDrawPoint))
        || labelMode && isBound(action, ACTION_dragLabel)) {
      viewer.checkObjectDragged(Integer.MAX_VALUE, 0, x, y, action);
      return;
    }
    if (dragSelectedMode)
      viewer.moveSelected(Integer.MAX_VALUE, 0, 0, 0, false);
  }

  void mouseDragged(long time, int x, int y, int mods) {
    setMouseMode();
    int deltaX = x - previousDragX;
    int deltaY = y - previousDragY;
    hoverOff();
    timeCurrent = time;
    xCurrent = previousDragX = x;
    yCurrent = previousDragY = y;
    int action = Binding.getMouseAction(pressedCount, mods);
    if (Binding.getModifiers(action) != 0) {
      int newAction = viewer.notifyMouseClicked(x, y,  Binding.getMouseAction(-pressedCount, mods));
      if (newAction == 0)
        return;
      if (newAction > 0)
        action = newAction;
    }
    
    if (isBound(action, ACTION_translateXY)) {
      viewer.translateXYBy(deltaX, deltaY);
      return;
    }
    if (isBound(action, ACTION_rotateXY)) {
        checkMotion();
        viewer.rotateXYBy(deltaX, deltaY);
        return;
    }
    if (dragSelectedMode && isBound(action, ACTION_dragSelected)) {
      checkMotion();
      viewer.moveSelected(deltaX, deltaY, x, y, false);
      return;
    }
    if (viewer.allowRotateSelected() && isBound(action, ACTION_rotateMolecule)) {
      checkMotion();
      viewer.rotateMolecule(deltaX, deltaY);
      return;
    }
    if (drawMode && (
          isBound(action, ACTION_dragDrawObject)
          || isBound(action, ACTION_dragDrawPoint))
          || labelMode && isBound(action, ACTION_dragLabel)) {
      checkMotion();
      viewer.checkObjectDragged(previousDragX, previousDragY, x, y,
          action);
      return;
    }
    if (dragSelectedMode && isBound(action, ACTION_dragSelected)) {
      checkMotion();
      viewer.moveSelected(deltaX, deltaY, x, y, true);
      return;
    } 
    if (rubberbandSelectionMode && 
        (  isBound(action, ACTION_rubberBandSelectToggle)
        || isBound(action, ACTION_rubberBandSelectOr)
        || isBound(action, ACTION_rubberBandSelectAndNot)
        )) {
      calcRectRubberBand();
      viewer.refresh(3, "mouse-drag selection");
      return;
    }
    boolean isZoom = isBound(action, ACTION_zoom);
    boolean isRotateZ = isBound(action, ACTION_rotateZ);
    if (isZoom && isRotateZ) {
      if (Math.abs(deltaY) > 5 * Math.abs(deltaX)) {
        //      if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
        checkMotion();
        viewer.zoomBy(deltaY);
      } else if (Math.abs(deltaX) > 5 * Math.abs(deltaY)) {
        //      if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
        checkMotion();
        viewer.rotateZBy(-deltaX);
      }
      return;
    } else if (isZoom) {
      checkMotion();
      viewer.zoomBy(deltaY);
      return;
    } else if (isRotateZ) {
      checkMotion();
      viewer.rotateZBy(-deltaX);
      return;
    }
    if (viewer.getSlabEnabled()) {
      if (isBound(action, ACTION_depth)) {
        viewer.depthByPixels(deltaY);
        return;
      }
      if (isBound(action, ACTION_slab)) {
        viewer.slabByPixels(deltaY);
        return;
      }
      if (isBound(action, ACTION_slabDepth)) {
        viewer.slabDepthByPixels(deltaY);
        return;
      }
    }
  }

  private void checkPointOrAtomClicked(int x, int y, int mods,
                                       int clickCount) {
    if (!viewer.haveModelSet())
      return;
    // points are always picked up first, then atoms
    // so that atom picking can be superceded by draw picking
    int action = Binding.getMouseAction(clickCount, mods);
    if (action != 0) {
      action = viewer.notifyMouseClicked(x, y, action);
      if (action == 0)
        return;
    }
    Point3fi nearestPoint = (drawMode ? null : viewer.checkObjectClicked(x, y,
        action));
    if (nearestPoint != null && Float.isNaN(nearestPoint.x))
      return;
    int nearestAtomIndex = (drawMode || nearestPoint != null ? -1 : viewer
        .findNearestAtomIndex(x, y));
    if (nearestAtomIndex >= 0 && (clickCount > 0 || measurementPending == null)
        && !viewer.isInSelectionSubset(nearestAtomIndex))
      nearestAtomIndex = -1;
    
    if (clickCount == 0) {
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
    }
    setMouseMode();
    if (isBound(action, ACTION_clickFrank) && viewer.frankClicked(x, y)) {
      viewer.popupMenu(-x, y);
      return;
    }
    if (viewer.getNavigationMode() 
        && pickingMode == JmolConstants.PICKING_NAVIGATE
        && isBound(action, ACTION_pickNavigate)) {
      viewer.navTranslatePercent(0f, x * 100f / viewer.getScreenWidth()
          - 50f, y * 100f / viewer.getScreenHeight() - 50f);
      return;
    }
    
    if (measurementPending != null && isBound(action, ACTION_pickMeasure)) {
      atomPicked(nearestAtomIndex, nearestPoint, action);
      if (addToMeasurement(nearestAtomIndex, nearestPoint, false) == 4) {
        previousClickCount = 0;
        toggleMeasurement();
      }
      return;
    }      
    if (isBound(action, ACTION_setMeasure)) {
      if (measurementPending != null) {
        addToMeasurement(nearestAtomIndex, nearestPoint, true);
        toggleMeasurement();
      } else if (!drawMode && !labelMode && !dragSelectedMode && measuresEnabled) {
        enterMeasurementMode();
        addToMeasurement(nearestAtomIndex, nearestPoint, true);
      }
      atomPicked(nearestAtomIndex, nearestPoint, action);
      return;
    }      
    if (isBound(action, ACTION_pickAtom) || isBound(action, ACTION_pickPoint)) {
      //TODO: in drawMode the binding changes
      atomPicked(nearestAtomIndex, nearestPoint, action);
      return;
    }
    if (isBound(action, ACTION_reset)) {
      if (nearestAtomIndex < 0)
        viewer.script("!reset");
      return;
    }
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  void checkMotion() {
    if (!viewer.getInMotion())
      viewer.setCursor(Viewer.CURSOR_MOVE);
    viewer.setInMotion(true);
  }

  private int addToMeasurement(int atomIndex, Point3fi nearestPoint,
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
      viewer.script("!measure " + measurementPending.getMeasurementScript(" ", true));
    }
    exitMeasurementMode();
  }

  void hoverOn(int atomIndex) {
    viewer.hoverOn(atomIndex, Binding.getMouseAction(previousClickCount, mouseMovedModifiers));
  }

  void hoverOff() {
    try {
      viewer.hoverOff();
    } catch (Exception e) {
      // ignore
    }
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
              && timeCurrent == mouseMovedTime) { // the last event was mouse
                                                  // move
            long currentTime = System.currentTimeMillis();
            int howLong = (int) (currentTime - mouseMovedTime);
            if (howLong > hoverDelay) {
              if (hoverWatcherThread != null && !viewer.getInMotion()
                  && !viewer.getSpinOn() && !viewer.getNavOn()
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
  
  //////////////// picking ///////////////////
  
  
  private int pickingMode = JmolConstants.PICKING_IDENT;
  private int pickingStyleSelect = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
  private int pickingStyleMeasure = JmolConstants.PICKINGSTYLE_MEASURE_OFF;

  private boolean drawHover;
  private int pickingStyle;
    
  private MeasurementPending measurementQueued;
  
  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    resetMeasurement();
  }

  int getPickingMode() {
    return pickingMode;
  }
    
  private void resetMeasurement() {
    measurementQueued = new MeasurementPending(viewer.getModelSet());    
  }

  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (pickingStyle >= JmolConstants.PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      resetMeasurement();
    } else {
      pickingStyleSelect = pickingStyle;
    }
    switch (pickingStyleSelect) {
    case JmolConstants.PICKINGSTYLE_SELECT_PFAAT:
      if (binding.getName() != "Pfaat")
        binding = new PfaatBinding();
      break;
    case JmolConstants.PICKINGSTYLE_SELECT_DRAG:
      if (binding.getName() != "Drag")
        binding = new DragBinding();
      break;
    case JmolConstants.PICKINGSTYLE_SELECT_RASMOL:
      if (binding.getName() != "Rasmol")
        binding = new RasmolBinding();
      break;
    default:
      if (binding.getName() != "Jmol")
      binding = new JmolBinding();
    }
  }
  
  int getPickingStyle() {
    return pickingStyle;
  }

  void setDrawHover(boolean TF) {
    drawHover = TF;
  }
  
  boolean getDrawHover() {
    return drawHover;
  }

  private void pickSelected(String spec, int action) {
    switch (pickingMode) {
    case JmolConstants.PICKING_IDENT:
    case JmolConstants.PICKING_SELECT_ATOM:
      applySelectStyle(spec, action);
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      applySelectStyle("within(group, " + spec +")", action);
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      applySelectStyle("within(chain, " + spec +")", action);
      break;
    case JmolConstants.PICKING_SELECT_MOLECULE:
      applySelectStyle("visible and within(molecule, " + spec +")", action);
      break;
    case JmolConstants.PICKING_SELECT_SITE:
      applySelectStyle("visible and within(site, " + spec +")", action);
      break;
    case JmolConstants.PICKING_SELECT_MODEL:
      applySelectStyle("within(model, " + spec +")", action);
      break;
    case JmolConstants.PICKING_SELECT_ELEMENT:
      applySelectStyle("visible and within(element, " + spec +")", action);
      break;
    case JmolConstants.PICKING_LABEL:
      viewer.script("set labeltoggle " + spec);
      return;
    default:
      return;
    }
    viewer.clearClickCount();
  }
  
  private void atomPicked(int atomIndex, Point3fi ptClicked, int action) {
    // atomIndex < 0 is possible here.
    if (atomIndex < 0) {
      if (isBound(action, ACTION_selectNone))
        viewer.script("select none");
      resetMeasurement();
      if (pickingMode != JmolConstants.PICKING_SPIN)
        return;
    }
    int n = 2;
    switch (pickingMode) {
    case JmolConstants.PICKING_OFF:
      return;
    case JmolConstants.PICKING_MEASURE_TORSION:
      n++;
      //fall through
    case JmolConstants.PICKING_MEASURE_ANGLE:
      n++;
      //fall through
    case JmolConstants.PICKING_MEASURE:
    case JmolConstants.PICKING_MEASURE_DISTANCE:
      if (!isBound(action, ACTION_pickMeasure))
        return;
      if (measurementQueued == null || measurementQueued.getCount() >= n)
        resetMeasurement();
      if (queueAtom(atomIndex, ptClicked) < n)
        return;
      viewer.setStatusMeasuring("measurePicked", n, measurementQueued.getStringDetail());
      if (pickingMode == JmolConstants.PICKING_MEASURE
          || pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON) {
        viewer.script("measure " + measurementQueued.getMeasurementScript(" ", true));
      }
      return;
    case JmolConstants.PICKING_CENTER:
      if (!isBound(action, ACTION_pickAtom))
        return;
      if (ptClicked == null)
        viewer.script("zoomTo (atomindex=" + atomIndex+")");
      else
        viewer.script("zoomTo " + Escape.escape(ptClicked));
      return;
    case JmolConstants.PICKING_SPIN:
      if (!isBound(action, ACTION_pickAtom))
        return;
      if (viewer.getSpinOn() || viewer.getNavOn() || viewer.getPendingMeasurement() != null) {
        resetMeasurement();
        viewer.script("spin off");
        return;
      }
      if (measurementQueued.getCount() >= 2)
        resetMeasurement();
      int queuedAtomCount = measurementQueued.getCount(); 
      if (queuedAtomCount == 1) {
        if (ptClicked == null) {
          if (measurementQueued.getAtomIndex(1) == atomIndex)
            return;
        } else {
          if (measurementQueued.getAtom(1).distance(ptClicked) == 0)
            return;
        }
      }
      if (atomIndex >= 0 || ptClicked != null)
        queuedAtomCount = queueAtom(atomIndex, ptClicked);
      if (queuedAtomCount < 2) {
        viewer.scriptStatus(queuedAtomCount == 1 ?
            GT._("pick one more atom in order to spin the model around an axis") :
            GT._("pick two atoms in order to spin the model around an axis"));
        return;
      }
      viewer.script("spin" + measurementQueued.getMeasurementScript(" ", false) + " " + viewer.getPickingSpinRate());
    }
    if (ptClicked != null)
      return;
    switch (pickingMode) {
    case JmolConstants.PICKING_IDENT:
      if (isBound(action, ACTION_pickAtom))
        viewer.setStatusAtomPicked(atomIndex, null);
      return;
    case JmolConstants.PICKING_LABEL:
      if (isBound(action, ACTION_pickLabel))
        viewer.script("set labeltoggle {atomindex="+atomIndex+"}");
      return;
    }
    pickSelected("atomindex=" + atomIndex, action);
  }

  private int queueAtom(int atomIndex, Point3fi ptClicked) {
    int n = measurementQueued.addPoint(atomIndex, ptClicked, true);
    if (atomIndex >= 0)
      viewer.setStatusAtomPicked(atomIndex, "Atom #" + n + ":"
          + viewer.getAtomInfo(atomIndex));
    return n;
  }

  private void applySelectStyle(String item, int action) {
    item = "(" + item + ")";
    String select = "";
    if (isBound(action, ACTION_selectAndNot)) {
      select = "selected and not ";
    } else if (isBound(action, ACTION_selectOr)) {
      select = "selected or ";
    } else if (isBound(action, ACTION_selectToggle)) {
      select = "selected tog ";
    }    
    viewer.script("select " + select + item);
  }


} 
