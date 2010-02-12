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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;

import org.jmol.i18n.GT;
import org.jmol.modelset.MeasurementPending;
import org.jmol.script.ScriptEvaluator;
import org.jmol.script.Token;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;
import org.jmol.util.TextFormat;
import org.jmol.viewer.binding.DragBinding;
import org.jmol.viewer.binding.Binding;
import org.jmol.viewer.binding.PfaatBinding;
import org.jmol.viewer.binding.RasmolBinding;
import org.jmol.viewer.binding.JmolBinding;

public class ActionManager {

  private final static String[] actionInfo = new String[] {
    //0
    GT._("rotate"),
    GT._("zoom"),
    GT._("rotate Z"),
    GT._("rotate Z (horizontal motion of mouse) or zoom (vertical motion of mouse)"),
    GT._("translate"),
    //5
    GT._("center"),
    GT._("zoom (along right edge of window)"),
    GT._("move selected atoms (requires {0})", "SET DRAGSELECTED"), 
    GT._("select and drag atoms (requires {0})", "SET DRAGSELECTED"), 
    GT._("rotate selected atoms (requires {0})", "SET DRAGSELECTED"),
    GT._("delete bond (requires {0})", "SET picking DELETE BOND"),
    GT._("connect atoms (requires {0})", "SET picking CONNECT"),
    GT._("move label (requires {0})", "SET picking LABEL"),
    //10
    GT._("move specific DRAW point (requires {0})", "SET picking DRAW"),
    GT._("move whole DRAW object (requires {0})", "SET picking DRAW"),
    GT._("spin model (swipe and release button and stop motion simultaneously)"),
    GT._("click on two points to spin around axis clockwise (requires SET picking SPIN)"),
    GT._("click on two points to spin around axis counterclockwise (requires SET picking SPIN)"),
    //15
    GT._("adjust slab (front plane; requires {0})", "SLAB ON"),
    GT._("adjust depth (back plane; requires {0})", "SLAB ON"),
    GT._("move slab/depth window (both planes; requires {0})", "SLAB ON"),
    GT._("pop up the full context menu"),
    GT._("pop up recent context menu (click on Jmol frank)"),
    //20
    GT._("translate navigation point (requires {0} and {1})", new String[] {"SET NAVIGATIONMODE", "SET picking NAVIGATE"}),
    GT._("pick an atom"),
    GT._("pick a DRAW point (for measurements)"),
    GT._("pick a label to toggle it hidden/displayed (requires {0})", "SET picking LABEL"),
    GT._("pick an atom to include it in a measurement (after starting a measurement or after {0})", "SET picking DISTANCE/ANGLE/TORSION"),
    //25
    GT._("pick an atom to initiate or conclude a measurement"),
    GT._("pick an ISOSURFACE point"),
    GT._("pick a point or atom to navigate to (requires SET NAVIGATIONMODE; undocumented)"),
    GT._("select an atom (requires {0})", "SET pickingStyle EXTENDEDSELECT"),
    GT._("select NONE (requires {0})", "SET pickingStyle EXTENDEDSELECT"),
    //30
    GT._("toggle selection (requires {0} or {1})", new String[] {
        "SET pickingStyle DRAG/EXTENDEDSELECT/RASMOL"}),
    GT._("unselect this group of atoms (requires {0})", "SET pickingStyle DRAG/EXTENDEDSELECT"),
    GT._("add this group of atoms to the set of selected atoms (requires {0})", "SET pickingStyle DRAG/EXTENDEDSELECT"),
    GT._("if all are selected, unselect all, otherwise add this group of atoms to the set of selected atoms (requires {0})", "SET pickingStyle DRAG"),    

    GT._("reset (when clicked off the model)"),    
    GT._("simulate multi-touch using the mouse)"),
    GT._("stop motion (requires {0})", "SET waitForMoveTo FALSE"),
  };

  public String getBindingInfo(String qualifiers) {
    return binding.getBindingInfo(actionInfo, qualifiers);  
  }

  public Hashtable getMouseInfo() {
    Hashtable info = new Hashtable();
    Vector vb = new Vector();
    Enumeration e = binding.getBindings().elements();
    while (e.hasMoreElements()) {
      Object obj = e.nextElement();
      if (obj instanceof Boolean)
        continue;
      if (obj instanceof int[]) {
        int[] binding = (int[]) obj;
        obj = new String[] { Binding.getMouseActionName(binding[0], false),
            getActionName(binding[1]) };
      }
      vb.add(obj);
    }
    info.put("bindings", vb);
    info.put("bindingName", binding.getName());
    info.put("actionNames", actionNames);
    info.put("actionInfo", actionInfo);
    info.put("bindingInfo", TextFormat.split(getBindingInfo(null), '\n'));
    return info;
  }

  private final static String[] actionNames = new String[] {
    //0
    "_rotate",
    "_wheelZoom",
    "_rotateZ",
    "_rotateZorZoom",
    "_translate",
    //5
    "_center",
    "_slideZoom",
    "_dragSelected",
    "_selectAndDrag",
    "_rotateSelected",
    "_deleteBond",
    "_pickConnect",
    "_dragLabel",
    //10
    "_dragDrawPoint",
    "_dragDrawObject",
    "_swipe",
    "_spinDrawObjectCW",
    //15    
    "_spinDrawObjectCCW",
    "_slab",
    "_depth",
    "_slabAndDepth",
    "_popupMenu",
    //20
    "_clickFrank",
    "_navTranslate",
    "_pickAtom",
    "_pickPoint",
    "_pickLabel",
    //25
    "_pickMeasure",
    "_setMeasure",
    "_pickIsosurface",
    "_pickNavigate",
    //30
    "_select",
    "_selectNone",
    "_selectToggle",
    "_selectAndNot",
    "_selectOr",
    //35
    "_selectToggleOr",
    "_reset",
    "_multiTouchSimulation",
    "_stopMotion",
  };
  public static String getActionName(int i) {
    return (i < actionNames.length ? actionNames[i] : null);
  }
  
  public static int getActionFromName(String name) {
    for (int i = 0; i < actionNames.length; i++)
      if (actionNames[i].equalsIgnoreCase(name))
        return i;
    return -1;
  }
  
  public final static int ACTION_rotate = 0;
  public final static int ACTION_wheelZoom = 1;
  public final static int ACTION_rotateZ = 2;
  public final static int ACTION_rotateZorZoom = 3;
  public final static int ACTION_translate = 4;
  
  public final static int ACTION_center = 5;
  public final static int ACTION_slideZoom = 6;
  public final static int ACTION_dragSelected = 7;
  public final static int ACTION_selectAndDrag = 8;
  public final static int ACTION_rotateSelected = 9;
  public static final int ACTION_deleteBond = 10;
  public final static int ACTION_connectAtoms = 11;
  public final static int ACTION_dragLabel = 12;
  public final static int ACTION_dragDrawPoint = 13;
  public final static int ACTION_dragDrawObject = 14;
  public final static int ACTION_swipe = 15;

  public final static int ACTION_spinDrawObjectCW = 16;
  public final static int ACTION_spinDrawObjectCCW = 17;

  public final static int ACTION_slab = 18;
  public final static int ACTION_depth = 19;
  public final static int ACTION_slabAndDepth = 20;

  public final static int ACTION_popupMenu = 21;
  public final static int ACTION_clickFrank = 22;
  public final static int ACTION_navTranslate = 23;

  public final static int ACTION_pickAtom = 24;
  public final static int ACTION_pickPoint = 25;
  public final static int ACTION_pickLabel = 26;
  public final static int ACTION_pickMeasure = 27;
  public final static int ACTION_setMeasure = 28;
  public static final int ACTION_pickIsosurface = 29;
  public static final int ACTION_pickNavigate = 30;
  
  public static final int ACTION_select = 31;
  public static final int ACTION_selectNone = 32;
  public static final int ACTION_selectToggle = 33;  
  public static final int ACTION_selectAndNot = 34;
  public static final int ACTION_selectOr = 35;
  public static final int ACTION_selectToggleExtended = 36;
  
  public final static int ACTION_reset = 37;
  public final static int ACTION_multiTouchSimulation = 38;
  public final static int ACTION_stopMotion = 39;
  public final static int ACTION_count = 40;
  
  static {
    if (actionNames.length != ACTION_count)
      Logger.error("ERROR IN ActionManager: actionNames length?");
    if (actionInfo.length != ACTION_count)
      Logger.error("ERROR IN ActionManager: actionInfo length?");
  }

  private final static long MAX_DOUBLE_CLICK_MILLIS = 700;
  protected static final long MININUM_GESTURE_DELAY_MILLISECONDS = 5;
  private static final int SLIDE_ZOOM_X_PERCENT = 98;
  public static final float DEFAULT_MOUSE_DRAG_FACTOR = 1f;
  public static final float DEFAULT_MOUSE_WHEEL_FACTOR = 1.02f;
  public static final float DEFAULT_GESTURE_SWIPE_FACTOR = 1f;
 
  protected Viewer viewer;
  
  Binding binding;
  Binding jmolBinding;
  Binding pfaatBinding;
  Binding dragBinding;
  Binding rasmolBinding;
  Binding predragBinding;

  ActionManager aman;
  ActionManager(Viewer viewer) {
    this.viewer = viewer;
    aman = this;
    setBinding(jmolBinding = new JmolBinding());
  }

  boolean isBound(int gesture, int action) {
    return binding.isBound(gesture, action);
  }

  void bindAction(String desc, String name, Point3f range1,
                         Point3f range2) {
    int jmolAction = getActionFromName(name);
    int mouseAction = Binding.getMouseAction(desc);
    if (jmolAction >= 0) {
      binding.bind(mouseAction, jmolAction);
    } else {
      binding.bind(mouseAction, name);
    }
  }

  protected void clearBindings() {
    setBinding(jmolBinding = new JmolBinding());
    pfaatBinding = null;
    dragBinding = null;
    rasmolBinding = null; 
  }
  
  void unbindAction(String desc, String name) {
    if (desc == null && name == null) {
      clearBindings();
      return;
    }
    int jmolAction = getActionFromName(name);
    int mouseAction = Binding.getMouseAction(desc);
    if (jmolAction >= 0)
      binding.unbind(mouseAction, jmolAction);
    else
      binding.unbind(mouseAction, name);
    if (name == null)
      binding.unbindUserAction(desc);    
  }

  protected Thread hoverWatcherThread;
  protected boolean haveMultiTouchInput = false;

  protected int xyRange = 0;
  
  private float gestureSwipeFactor = DEFAULT_GESTURE_SWIPE_FACTOR;
  private float mouseDragFactor = DEFAULT_MOUSE_DRAG_FACTOR;
  private float mouseWheelFactor = DEFAULT_MOUSE_WHEEL_FACTOR;
  
  void setGestureSwipeFactor(float factor) {
    gestureSwipeFactor = factor;
  }
  
  void setMouseDragFactor(float factor) {
    mouseDragFactor = factor;
  }
  
  void setMouseWheelFactor(float factor) {
    mouseWheelFactor = factor;
  }
  
  protected class Mouse {
    protected int x = -1000;
    protected int y = -1000;
    protected int modifiers = 0;
    protected long time = -1;
    
    protected void set(long time, int x, int y, int modifiers) {
      this.time = time;
      this.x = x;
      this.y = y;
      this.modifiers = modifiers;
    }

    protected void setCurrent() {
      time = current.time;
      x = current.x;
      y = current.y;
      modifiers = current.modifiers;
    }

    public boolean check(int x, int y, int modifiers, long time, long delayMax) {
      return (Math.abs(this.x - x) <= xyRange 
          && Math.abs(this.y - y) <= xyRange 
          && this.modifiers == modifiers
        && (time - this.time) < delayMax);
    }
  }
  
  protected final Mouse current = new Mouse();
  protected final Mouse moved = new Mouse();
  private final Mouse clicked = new Mouse();
  private final Mouse pressed = new Mouse();
  private final Mouse dragged = new Mouse();

  protected void setCurrent(long time, int x, int y, int mods) {
    hoverOff();
    current.set(time, x, y, mods);
  }
  
  int getCurrentX() {
    return current.x;
  }
  int getCurrentY() {
    return current.y;
  }

  protected int pressedCount;
  private int pressedAtomIndex;
  
  protected int clickedCount;

  private boolean drawMode = false;
  private boolean labelMode = false;
  private boolean dragSelectedMode = false;
  private boolean measuresEnabled = true;
  private MeasurementPending measurementPending;

  private boolean hoverActive = false;

  private boolean rubberbandSelectionMode = false;
  private final Rectangle rectRubber = new Rectangle();

  private boolean isAltKeyReleased = true;  
  private boolean keyProcessing;

  protected boolean isMultiTouchClient;
  protected boolean isMultiTouchServer;

  boolean isMTClient() {
    return isMultiTouchClient;
  }

  boolean isMTServer() {
    return isMultiTouchServer;
  }

  void dispose() {
    clear();
  }

  void clear() {
    startHoverWatcher(false);
    clearTimeouts();
    if (predragBinding != null)
      binding = predragBinding;
    viewer.setPickingMode(null, JmolConstants.PICKING_IDENTIFY);
    viewer.setPickingStyle(null, rootPickingStyle);
    eval = null;
  }

  synchronized void startHoverWatcher(boolean isStart) {
    if (viewer.isPreviewOnly())
      return;
    try {
      if (isStart) {
        if (hoverWatcherThread != null)
          return;
        current.time = -1;
        hoverWatcherThread = new Thread(new HoverWatcher());
        hoverWatcherThread.setName("HoverWatcher");
        hoverWatcherThread.start();
      } else {
        if (hoverWatcherThread == null)
          return;
        current.time = -1;
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

  /**
   * called by MouseManager.keyPressed
   * @param ke
   */
  void keyPressed(KeyEvent ke) {
    ke.consume();
    if (keyProcessing)
      return;
    if (Logger.debugging)
      Logger.debug("ActionmManager keyPressed: " + ke.getKeyCode());
    keyProcessing = true;
    int i = ke.getKeyCode();
    switch(i) {
    case KeyEvent.VK_ALT:
      if (dragSelectedMode && isAltKeyReleased)
        viewer.moveSelected(Integer.MIN_VALUE, 0, 0, 0, false);
      isAltKeyReleased = false;
      moved.modifiers |= Binding.ALT;
      break;
    case KeyEvent.VK_SHIFT:
      moved.modifiers |= Binding.SHIFT;
      break;
    case KeyEvent.VK_CONTROL:
      moved.modifiers |= Binding.CTRL;
    }
    int action = Binding.LEFT+Binding.SINGLE_CLICK+moved.modifiers;
    if(!binding.isUserAction(action))
      checkMotionRotateZoom(action, current.x, 0, 0, false);
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
    if (Logger.debugging)
      Logger.debug("ActionmManager keyReleased: " + ke.getKeyCode());
    ke.consume();
    int i = ke.getKeyCode();
    switch(i) {
    case KeyEvent.VK_ALT:
      if (dragSelectedMode)
        viewer.moveSelected(Integer.MAX_VALUE, 0, 0, 0, false);
      isAltKeyReleased = true;
      moved.modifiers &= ~Binding.ALT;
      break;
    case KeyEvent.VK_SHIFT:
      moved.modifiers &= ~Binding.SHIFT;
      break;
    case KeyEvent.VK_CONTROL:
      moved.modifiers &= ~Binding.CTRL;
    }
    if (moved.modifiers == 0)
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
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

  void mouseEntered(long time, int x, int y) {
    setCurrent(time, x, y, 0);
  }

  void mouseExited(long time, int x, int y) {
    setCurrent(time, x, y, 0);
    exitMeasurementMode();
  }

  void setMouseMode() {
    drawMode = labelMode = false;
    dragSelectedMode = viewer.getDragSelected();
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
      case JmolConstants.PICKING_SELECT_ATOM:
      case JmolConstants.PICKING_MEASURE_DISTANCE:
      case JmolConstants.PICKING_MEASURE_ANGLE:
      case JmolConstants.PICKING_MEASURE_TORSION:
        measuresEnabled = false;
        break;
      }
    exitMeasurementMode();
  }
  
  protected void clearMouseInfo() {
    // when a second touch is made, this clears all record of first touch
    pressedCount = clickedCount = 0;
    dragGesture.setAction(0, 0);
    exitMeasurementMode();
  }

  void mouseMoved(long time, int x, int y, int modifiers) {
    setCurrent(time, x, y, modifiers);
    moved.setCurrent();
    if (measurementPending != null || hoverActive)
      checkPointOrAtomClicked(x, y, 0, 0);
    else if (isZoomArea(x))
      checkMotionRotateZoom(Binding.getMouseAction(1, Binding.LEFT), 0, 0, 0, false);
    else
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
  }

  void mouseWheel(long time, int rotation, int mods) {
    if (viewer.isApplet() && !viewer.hasFocus())
      return;
    // sun bug? noted by Charles Xie that wheeling on a Java page
    // effected inappropriate wheeling on this Java component
    setCurrent(time, current.x, current.y, mods);
    checkAction(Binding.getMouseAction(0, mods), current.x, current.y, 0, rotation, time, 3);
  }

  private boolean haveSelection;
  
  void mousePressed(long time, int x, int y, int mods) {
    setCurrent(time, x, y, mods);
    pressedCount = (pressed.check(x, y, mods, time, MAX_DOUBLE_CLICK_MILLIS)
        ? pressedCount + 1 : 1);
    pressed.setCurrent();
    dragged.setCurrent();
    viewer.setFocus();
    boolean isSelectAndDrag = isBound(Binding.getMouseAction(Integer.MIN_VALUE, mods), ACTION_selectAndDrag);
    int action = Binding.getMouseAction(pressedCount, mods);
    dragGesture.setAction(action, time);
    if (Binding.getModifiers(action) != 0) {
      action = viewer.notifyMouseClicked(x, y, action);
      if (action == 0)
        return;
    }    
    pressedAtomIndex = Integer.MAX_VALUE;
    if (checkUserAction(action, x, y, 0, 0, time, 0))
      return;
    if (drawMode && (
        isBound(action, ACTION_dragDrawObject)
        || isBound(action, ACTION_dragDrawPoint))
      || labelMode && isBound(action, ACTION_dragLabel)) {
      viewer.checkObjectDragged(Integer.MIN_VALUE, 0, x, y, action);
      return;
    }
    if (dragSelectedMode) {
      haveSelection = true;
      if (isSelectAndDrag) {
        haveSelection = checkPointOrAtomClicked(x, y, mods, pressedCount); 
      }
      if (isBound(action, ACTION_dragSelected) && haveSelection) {
        viewer.moveSelected(Integer.MIN_VALUE, 0, 0, 0, false);
      }
      return;
    }
    if (isBound(action, ACTION_popupMenu)) {
      viewer.popupMenu(x, y);
      return;
    }
    checkMotionRotateZoom(action, x, 0, 0, true);
  }

  void mouseDragged(long time, int x, int y, int mods) {
    setMouseMode();
    int deltaX = x - dragged.x;
    int deltaY = y - dragged.y;
    setCurrent(time, x, y, mods);
    dragged.setCurrent();
    exitMeasurementMode();
    int action = Binding.getMouseAction(pressedCount, mods);
    dragGesture.add(action, x, y, time);
    checkAction(action, x, y, deltaX, deltaY, time, 1);
  }

  void mouseReleased(long time, int x, int y, int mods) {
    setCurrent(time, x, y, mods);
    viewer.spinXYBy(0, 0, 0);
    boolean dragRelease = !pressed.check(x, y, mods, time, Long.MAX_VALUE);
    viewer.setInMotion(false);
    viewer.setCursor(Viewer.CURSOR_DEFAULT);
    int action = Binding.getMouseAction(pressedCount, mods);
    dragGesture.add(action, x, y, time);
    boolean isRbAction = isRubberBandSelect(action);
    if (isRbAction) {
      BitSet bs = viewer.findAtomsInRectangle(rectRubber);
      if (bs.length() > 0) {
        String s = Escape.escape(bs);
        if (isBound(action, ACTION_selectOr))
          viewer.script("select selected or " + s);
        else if (isBound(action, ACTION_selectAndNot))
          viewer.script("select selected and not " + s);
        else
          // ACTION_selectToggle
          viewer.script("select selected tog " + s);
      }
      viewer.refresh(3, "mouseReleased");
    }
    rubberbandSelectionMode = false;
    rectRubber.x = Integer.MAX_VALUE;
    if (dragRelease)
      viewer.notifyMouseClicked(x, y, Binding.getMouseAction(pressedCount, 0));

    if (drawMode
        && (isBound(action, ACTION_dragDrawObject) || isBound(action,
            ACTION_dragDrawPoint)) || labelMode
        && isBound(action, ACTION_dragLabel)) {
      viewer.checkObjectDragged(Integer.MAX_VALUE, 0, x, y, action);
      return;
    }
    if (dragSelectedMode && isBound(action, ACTION_dragSelected)
        && haveSelection)
      viewer.moveSelected(Integer.MAX_VALUE, 0, 0, 0, false);

    if (dragRelease && checkUserAction(action, x, y, 0, 0, time, 2))
      return;

    if (viewer.getAllowGestures()) {
      if (isBound(action, ACTION_swipe)) {
        float speed = getExitRate();
        if (speed > 0)
          viewer.spinXYBy(dragGesture.getDX(4, 2), dragGesture.getDY(4, 2),
              speed * 30 * gestureSwipeFactor);
        if (viewer.getLogGestures())
          viewer.log("NOW swipe " + dragGesture + " " + speed);
        return;
      }

    }
  }

  protected float getExitRate() {
    long dt = dragGesture.getTimeDifference(2);
    return (dt > MININUM_GESTURE_DELAY_MILLISECONDS ? 0 : 
        dragGesture.getSpeedPixelsPerMillisecond(4, 2));
  }


  void mouseClicked(long time, int x, int y, int mods, int count) {
    setMouseMode();
    setCurrent(time, x, y, mods);
    clickedCount = (count > 1 ? count : clicked.check(x, y, mods, time,
        MAX_DOUBLE_CLICK_MILLIS) ? clickedCount + 1 : 1);
    clicked.setCurrent();
    viewer.setFocus();
    boolean isSelectAndDrag = isBound(Binding.getMouseAction(Integer.MIN_VALUE, mods), ACTION_selectAndDrag);
    if (isSelectAndDrag)
      return;
    checkPointOrAtomClicked(x, y, mods, clickedCount);
  }

  private boolean isRubberBandSelect(int action) {
    return rubberbandSelectionMode && 
        (  isBound(action, ACTION_selectToggle)
        || isBound(action, ACTION_selectOr)
        || isBound(action, ACTION_selectAndNot)
        );
  }

  Rectangle getRubberBand() {
    if (!rubberbandSelectionMode || rectRubber.x == Integer.MAX_VALUE)
      return null;
    return rectRubber;
  }

  private void calcRectRubberBand() {
    if (current.x < pressed.x) {
      rectRubber.x = current.x;
      rectRubber.width = pressed.x - current.x;
    } else {
      rectRubber.x = pressed.x;
      rectRubber.width = current.x - pressed.x;
    }
    if (current.y < pressed.y) {
      rectRubber.y = current.y;
      rectRubber.height = pressed.y - current.y;
    } else {
      rectRubber.y = pressed.y;
      rectRubber.height = current.y - pressed.y;
    }
  }

  private void checkAction(int action, int x, int y, int deltaX, int deltaY,
                           long time, int mode) {
    int mods = Binding.getModifiers(action);
    if (Binding.getModifiers(action) != 0) {
      int newAction = viewer.notifyMouseClicked(x, y, Binding.getMouseAction(
          -pressedCount, mods));
      if (newAction == 0)
        return;
      if (newAction > 0)
        action = newAction;
    }

    if (isRubberBandSelect(action)) {
      calcRectRubberBand();
      viewer.refresh(3, "rubberBand selection");
      return;
    }

    if (checkUserAction(action, x, y, deltaX, deltaY, time, mode))
      return;

    if (isBound(action, ACTION_translate)) {
      viewer.translateXYBy(deltaX, deltaY);
      return;
    }

    if (isBound(action, ACTION_center)) {
      if (pressedAtomIndex == Integer.MAX_VALUE)
        pressedAtomIndex = viewer.findNearestAtomIndex(pressed.x, pressed.y);
      Point3f pt = (pressedAtomIndex < 0 ? null : viewer
          .getAtomPoint3f(pressedAtomIndex));
      if (pt == null)
        viewer.translateXYBy(deltaX, deltaY);
      else
        viewer.centerAt(x, y, pt);
      return;
    }

    if (dragSelectedMode && isBound(action, ACTION_dragSelected)
        && haveSelection) {
      checkMotion(Viewer.CURSOR_MOVE);
      viewer.moveSelected(deltaX, deltaY, x, y, true);
      return;
    }

    if (checkMotionRotateZoom(action, x, deltaX, deltaY, true)) {
      viewer.zoomBy(deltaY);
      return;
    }

    boolean isRotate = isBound(action, ACTION_rotate);
    if (isRotate || viewer.allowRotateSelected()
        && isBound(action, ACTION_rotateSelected)) {
      float degX = ((float) deltaX) / viewer.getScreenWidth() * 180
          * mouseDragFactor;
      float degY = ((float) deltaY) / viewer.getScreenHeight() * 180
          * mouseDragFactor;
      if (isRotate) {
        viewer.rotateXYBy(degX, degY);
      } else {
        checkMotion(Viewer.CURSOR_MOVE);
        viewer.rotateMolecule(degX, degY);
      }
      return;
    }
    if (drawMode
        && (isBound(action, ACTION_dragDrawObject) || isBound(action,
            ACTION_dragDrawPoint)) || labelMode
        && isBound(action, ACTION_dragLabel)) {
      checkMotion(Viewer.CURSOR_MOVE);
      viewer.checkObjectDragged(dragged.x, dragged.y, x, y, action);
      return;
    }
    if (dragSelectedMode && isBound(action, ACTION_dragSelected)) {
      checkMotion(Viewer.CURSOR_MOVE);
      viewer.moveSelected(deltaX, deltaY, x, y, true);
      return;
    }
    if (isBound(action, ACTION_rotateZorZoom)) {
      if (Math.abs(deltaY) > 5 * Math.abs(deltaX)) {
        // if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
        checkMotion(Viewer.CURSOR_ZOOM);
        viewer.zoomBy(deltaY);
      } else if (Math.abs(deltaX) > 5 * Math.abs(deltaY)) {
        // if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
        checkMotion(Viewer.CURSOR_MOVE);
        viewer.rotateZBy(-deltaX, Integer.MAX_VALUE, Integer.MAX_VALUE);
      }
      return;
    } else if (isBound(action, ACTION_wheelZoom)) {
      zoomByFactor(deltaY, Integer.MAX_VALUE, Integer.MAX_VALUE);
      return;
    } else if (isBound(action, ACTION_rotateZ)) {
      checkMotion(Viewer.CURSOR_MOVE);
      viewer.rotateZBy(-deltaX, Integer.MAX_VALUE, Integer.MAX_VALUE);
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
      if (isBound(action, ACTION_slabAndDepth)) {
        viewer.slabDepthByPixels(deltaY);
        return;
      }
    }
  }

  protected void zoomByFactor(int dz, int x, int y) {
    if (dz == 0)
      return;
    checkMotion(Viewer.CURSOR_ZOOM);
    viewer.zoomByFactor((float) Math.pow(mouseWheelFactor, dz), x, y);
    viewer.setInMotion(false);
  }

  private boolean checkUserAction(int action, int x, int y, 
                                  int deltaX, int deltaY, long time, int mode) {
    if (!binding.isUserAction(action))
      return false;
    Hashtable ht = binding.getBindings();
    Enumeration e = ht.keys();
    boolean ret = false;
    Object obj;
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.indexOf(action + "\t") != 0 
          || !((obj = ht.get(key)) instanceof String[]))
        continue;
      String script = ((String[]) obj)[1];
      script = TextFormat.simpleReplace(script,"_ACTION", "" + action);
      script = TextFormat.simpleReplace(script,"_X", "" + x);
      script = TextFormat.simpleReplace(script,"_Y", "" + (viewer.getScreenHeight() - y));
      script = TextFormat.simpleReplace(script,"_DELTAX", "" + deltaX);
      script = TextFormat.simpleReplace(script,"_DELTAY", "" + deltaY);
      script = TextFormat.simpleReplace(script,"_TIME", "" + time);
      script = TextFormat.simpleReplace(script,"_MODE", "" + mode);
      viewer.evalStringQuiet(script);
      ret = true;
    }
    return ret;
  }

  private boolean checkMotionRotateZoom(int action, int x, 
                                        int deltaX, int deltaY,
                                        boolean inMotion) {
    boolean isSlideZoom = isBound(action, ACTION_slideZoom);
    boolean isRotateXY = isBound(action, ACTION_rotate);
    boolean isRotateZorZoom = isBound(action, ACTION_rotateZorZoom);
    if (!isSlideZoom && !isRotateXY && !isRotateZorZoom) 
      return false;
    boolean isZoom = false;
    if (isRotateZorZoom)
      isZoom = (deltaX == 0 || Math.abs(deltaY) > 5 * Math.abs(deltaX));
    if (isSlideZoom)
      isZoom = isZoomArea(moved.x);
    int cursor = (isZoom || isBound(action, ACTION_wheelZoom) ? Viewer.CURSOR_ZOOM 
        : isRotateXY || isRotateZorZoom ? Viewer.CURSOR_MOVE : Viewer.CURSOR_DEFAULT);
    viewer.setCursor(cursor);
    if (inMotion)
      viewer.setInMotion(true);
    return isZoom;
  }

  private boolean isZoomArea(int x) {
    return x > viewer.getScreenWidth() * (viewer.isStereoDouble() ? 2 : 1)
        * SLIDE_ZOOM_X_PERCENT / 100f;
  }

  private boolean checkPointOrAtomClicked(int x, int y, int mods, int clickedCount) {
    if (!viewer.haveModelSet())
      return false;
    // points are always picked up first, then atoms
    // so that atom picking can be superceded by draw picking
    int action = Binding.getMouseAction(clickedCount, mods);
    if (action != 0) {
      action = viewer.notifyMouseClicked(x, y, action);
      if (action == 0)
        return false;
    }
    Point3fi nearestPoint = null;
    int tokType = 0;
    if (!drawMode) {
      Token t = viewer.checkObjectClicked(x, y, action);
      if (t != null) {
        tokType = t.tok;
        nearestPoint = (Point3fi) t.value;
      }
    }
    if (nearestPoint != null && Float.isNaN(nearestPoint.x))
      return false;
    int nearestAtomIndex = (drawMode || nearestPoint != null ? 
        -1 : viewer.findNearestAtomIndex(x, y));
    if (nearestAtomIndex >= 0
        && (clickedCount > 0 || measurementPending == null)
        && !viewer.isInSelectionSubset(nearestAtomIndex))
      nearestAtomIndex = -1;

    if (clickedCount == 0) {
      // mouse move
      if (measurementPending == null)
        return (nearestAtomIndex >= 0);
      if (nearestPoint != null
          || measurementPending.getIndexOf(nearestAtomIndex) == 0)
        measurementPending.addPoint(nearestAtomIndex, nearestPoint, false);
      if (measurementPending.haveModified())
        viewer.setPendingMeasurement(measurementPending);
      viewer.refresh(3, "measurementPending");
      return (nearestAtomIndex >= 0);
    }
    setMouseMode();
    
    if (isBound(action, ACTION_stopMotion)) {
        viewer.stopMotion();
      // continue checking --- no need to exit here
    }

    if (isBound(action, ACTION_clickFrank) && viewer.frankClicked(x, y)) {
      viewer.popupMenu(-x, y);
      return false;
    }
    if (viewer.getNavigationMode()
        && pickingMode == JmolConstants.PICKING_NAVIGATE
        && isBound(action, ACTION_pickNavigate)) {
      viewer.navTranslatePercent(0f, x * 100f / viewer.getScreenWidth() - 50f,
          y * 100f / viewer.getScreenHeight() - 50f);
      return false;
    }

    if (measurementPending != null && isBound(action, ACTION_pickMeasure)) {
      atomPicked(nearestAtomIndex, nearestPoint, action);
      if (addToMeasurement(nearestAtomIndex, nearestPoint, false) == 4) {
        clickedCount = 0;
        toggleMeasurement();
      }
      return false;
    }
   
    if (isBound(action, ACTION_setMeasure)) {
      if (measurementPending != null) {
        addToMeasurement(nearestAtomIndex, nearestPoint, true);
        toggleMeasurement();
      } else if (!drawMode && !labelMode && !dragSelectedMode
          && measuresEnabled) {
        enterMeasurementMode();
        addToMeasurement(nearestAtomIndex, nearestPoint, true);
      }
      atomPicked(nearestAtomIndex, nearestPoint, action);
      return false;
    }
    if (isBound(action, ACTION_deleteBond) && tokType == Token.bonds) {
      BitSet bs = new BitSet();
      bs.set(nearestPoint.index);
      viewer.deleteBonds(bs);
      return false;
    }
    if (isBound(action, ACTION_pickAtom) || isBound(action, ACTION_pickPoint)) {
      // TODO: in drawMode the binding changes
      atomPicked(nearestAtomIndex, nearestPoint, action);
      return (nearestAtomIndex >= 0);
    }
    if (isBound(action, ACTION_reset)) {
      if (nearestAtomIndex < 0)
        viewer.script("!reset");
      return false;
    }
    return (nearestAtomIndex >= 0);
  }

  protected void checkMotion(int cursor) {
    viewer.setCursor(cursor);
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
    if (measurementCount >= 2 && measurementCount <= 4)
      viewer.script("!measure " + measurementPending.getMeasurementScript(" ", true));
    exitMeasurementMode();
  }

  Hashtable timeouts;
  
  String showTimeout(String name) {
    StringBuffer sb = new StringBuffer();
    if (timeouts != null) {
      Enumeration e = timeouts.elements();
      while (e.hasMoreElements()) {
        TimeoutThread t = (TimeoutThread) e.nextElement();
        if (name == null || t.name.equalsIgnoreCase(name))
          sb.append(t.toString()).append("\n");
      }
    }
    return (sb.length() > 0 ? sb.toString() : "<no timeouts set>");
  }

  void clearTimeouts() {
    if (timeouts == null)
      return;
    Enumeration e = timeouts.elements();
    while (e.hasMoreElements())
      ((Thread) e.nextElement()).interrupt();
    timeouts.clear();    
  }
  
  void setTimeout(String name, int mSec, String script) {
    if (name == null) {
      clearTimeouts();
      return;
    }
    if (timeouts == null)
      timeouts = new Hashtable();
    if (mSec == 0) {
      Thread t = (Thread) timeouts.get(name);
      if (t != null) {
        t.interrupt();
        timeouts.remove(name);
      }
      return;
    }
    TimeoutThread t = (TimeoutThread) timeouts.get(name);
    if (t != null) {
      t.set(mSec, script);
      return;
    }
    t = new TimeoutThread(name, mSec, script);
    timeouts.put(name, t);
    t.start();
  }

  class TimeoutThread extends Thread {
    String name;
    private int ms;
    private long targetTime;
    private int status;
    private String script;
    
    TimeoutThread(String name, int ms, String script) {
      this.name = name;
      this.ms = ms;
      this.script = script;
      targetTime = System.currentTimeMillis() + Math.abs(ms);
    }
    
    void set(int ms, String script) {
      this.ms = ms;
      if (script != null && script.length() != 0)
        this.script = script; 
    }

    public String toString() {
      return "timeout name=" + name + " executions=" + status + " mSec=" + ms 
      + " secRemaining=" + (targetTime - System.currentTimeMillis())/1000f + " script=" + script + " thread=" + Thread.currentThread().getName();      
    }
    
    public void run() {
      if (script == null || script.length() == 0 || ms == 0)
        return;
      //System.out.println("I am the timeout thread, and my name is " + Thread.currentThread().getName());
      Thread.currentThread().setName("timeout " + name);
      //if (true || Logger.debugging) 
        //Logger.info(toString());
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      try {
        while (true) {
          Thread.sleep(26);
          if (targetTime > System.currentTimeMillis())
            continue;
          status++;
          targetTime += Math.abs(ms);
          if (timeouts.get(name) == null)
            break;
          if (ms > 0)
            timeouts.remove(name);
          //System.out.println("I'm going to execute " + script + " now");
          //if (Logger.debugging)
            //viewer.script(script);
          //else 
          viewer.evalStringQuiet(script);
          if (ms > 0)
            break;
        }
      } catch (InterruptedException ie) {
        Logger.info("Timeout " + name + " interrupted");
      } catch (Exception ie) {
        Logger.info("Timeout " + name + " Exception: " + ie);
      }
      //System.out.println("I'm done");
      timeouts.remove(name);
    }
  }
  
  void hoverOn(int atomIndex) {
    viewer.hoverOn(atomIndex, Binding.getMouseAction(clickedCount, moved.modifiers));
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
        while (Thread.currentThread().equals(hoverWatcherThread) && (hoverDelay = viewer.getHoverDelay()) > 0) {
          Thread.sleep(hoverDelay);
          if (!viewer.isHoverEnabled())
            continue;
          if (current.x == moved.x && current.y == moved.y
              && current.time == moved.time) { // the last event was mouse
                                                  // move
            long currentTime = System.currentTimeMillis();
            int howLong = (int) (currentTime - moved.time);
            if (howLong > hoverDelay) {
              if (Thread.currentThread().equals(hoverWatcherThread) && !viewer.getInMotion()
                  && !viewer.getSpinOn() && !viewer.getNavOn()
                  && !viewer.checkObjectHovered(current.x, current.y)) {
                int atomIndex = viewer.findNearestAtomIndex(current.x, current.y);
                if (atomIndex >= 0) {
                  hoverOn(atomIndex);
                }
              }
            }
          }
        }
      } catch (InterruptedException ie) {
        Logger.debug("Hover interrupted");
      } catch (Exception ie) {
        Logger.debug("Hover Exception: " + ie);
      }
    }
  }
  
  //////////////// picking ///////////////////
  
  private int pickingStyle;
  private int pickingMode = JmolConstants.PICKING_IDENTIFY;
  private int pickingStyleSelect = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
  private int pickingStyleMeasure = JmolConstants.PICKINGSTYLE_MEASURE_OFF;
  private int rootPickingStyle = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
  
  private MeasurementPending measurementQueued;
  
  private void resetMeasurement() {
    measurementQueued = new MeasurementPending(viewer.getModelSet());    
  }

  int getPickingMode() {
    return pickingMode;
  }
    
  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    resetMeasurement();
  }

  int getPickingStyle() {
    return pickingStyle;
  }

  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (pickingStyle >= JmolConstants.PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      resetMeasurement();
    } else {
      if (pickingStyle < JmolConstants.PICKINGSTYLE_SELECT_DRAG)
        rootPickingStyle = pickingStyle;
      pickingStyleSelect = pickingStyle;
    }
    rubberbandSelectionMode = false;
    switch (pickingStyleSelect) {
    case JmolConstants.PICKINGSTYLE_SELECT_PFAAT:
      if (binding.getName() != "Pfaat") 
        setBinding(pfaatBinding = (pfaatBinding == null ? new PfaatBinding() : pfaatBinding));
      break;
    case JmolConstants.PICKINGSTYLE_SELECT_DRAG:
      if (binding.getName() != "Drag")
        setBinding(dragBinding = (dragBinding == null ? new DragBinding() : dragBinding));
      rubberbandSelectionMode = true;
      break;
    case JmolConstants.PICKINGSTYLE_SELECT_RASMOL:
      if (binding.getName() != "Rasmol")
        setBinding(rasmolBinding = (rasmolBinding == null ? new RasmolBinding() : rasmolBinding));
      break;
    default:
      if (binding != jmolBinding)
        setBinding(jmolBinding);
    }
    if (binding.getName() != "Drag")
      predragBinding = binding;
  }

  protected void setBinding(Binding newBinding) {
    binding = newBinding;
  }
  
  private void atomPicked(int atomIndex, Point3fi ptClicked, int action) {
    // atomIndex < 0 is off structure.
    if (atomIndex < 0) {
      resetMeasurement();
      if (isBound(action, ACTION_selectNone)) {
        viewer.script("select none");
        return;
      }
      if (pickingMode != JmolConstants.PICKING_SPIN
          && pickingMode != JmolConstants.PICKING_SYMMETRY)
        return;
    }
    int n = 2;
    switch (pickingMode) {
    case JmolConstants.PICKING_OFF:
      return;
    case JmolConstants.PICKING_STRUTS:
    case JmolConstants.PICKING_CONNECT:
    case JmolConstants.PICKING_DELETE_BOND:
      boolean isDelete = (pickingMode == JmolConstants.PICKING_DELETE_BOND);
      boolean isStruts = (pickingMode == JmolConstants.PICKING_STRUTS);
      if (!isBound(action, (isDelete ? ACTION_deleteBond : ACTION_connectAtoms)))
        return;
      if (measurementQueued == null || measurementQueued.getCount() >= 2)
        resetMeasurement();
      if (queueAtom(atomIndex, ptClicked) < n)  
        return;
      viewer.script("connect "
          + measurementQueued.getMeasurementScript(" ", true)
          + (isDelete || measurementQueued.isConnected(viewer.getModelSet().getAtoms(), 2)? " DELETE" : isStruts ? "STRUTS" : ""));
      return;
    case JmolConstants.PICKING_MEASURE_TORSION:
      n++;
      // fall through
    case JmolConstants.PICKING_MEASURE_ANGLE:
      n++;
      // fall through
    case JmolConstants.PICKING_MEASURE:
    case JmolConstants.PICKING_MEASURE_DISTANCE:
      if (!isBound(action, ACTION_pickMeasure))
        return;
      if (measurementQueued == null || measurementQueued.getCount() >= n)
        resetMeasurement();
      if (queueAtom(atomIndex, ptClicked) < n)
        return;
      viewer.setStatusMeasuring("measurePicked", n, measurementQueued
          .getStringDetail());
      if (pickingMode == JmolConstants.PICKING_MEASURE
          || pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON) {
        viewer.script("measure "
            + measurementQueued.getMeasurementScript(" ", true));
      }
      return;
    case JmolConstants.PICKING_CENTER:
      if (!isBound(action, ACTION_pickAtom))
        return;
      if (ptClicked == null)
        viewer.script("zoomTo (atomindex=" + atomIndex + ")");
      else
        viewer.script("zoomTo " + Escape.escape(ptClicked));
      return;
    case JmolConstants.PICKING_SPIN:
    case JmolConstants.PICKING_SYMMETRY:
      checkTwoAtomAction(action, ptClicked, atomIndex);
    }
    if (ptClicked != null)
      return;
    switch (pickingMode) {
    case JmolConstants.PICKING_IDENTIFY:
      if (isBound(action, ACTION_pickAtom))
        viewer.setStatusAtomPicked(atomIndex, null);
      return;
    case JmolConstants.PICKING_LABEL:
      if (isBound(action, ACTION_pickLabel))
        viewer.script("set labeltoggle {atomindex=" + atomIndex + "}");
      return;
    }
    String spec = "atomindex=" + atomIndex;
    switch (pickingMode) {
    case JmolConstants.PICKING_LABEL:
      viewer.script("set labeltoggle " + spec);
      return;
    default:
      return;
    case JmolConstants.PICKING_DELETE_ATOM:
      BitSet bs = getSelectionSet("(" + spec + ")");
      viewer.deleteAtoms(bs, false);
      break;
    case JmolConstants.PICKING_IDENTIFY:
    case JmolConstants.PICKING_SELECT_ATOM:
      applySelectStyle(spec, action);
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      applySelectStyle("within(group, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      applySelectStyle("within(chain, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_POLYMER:
      applySelectStyle("within(polymer, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_STRUCTURE:
      applySelectStyle("within(structure, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_MOLECULE:
      applySelectStyle("within(molecule, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_MODEL:
      applySelectStyle("within(model, " + spec + ")", action);
      break;
      // only the next two use VISIBLE (as per the documentation)
    case JmolConstants.PICKING_SELECT_ELEMENT:
      applySelectStyle("visible and within(element, " + spec + ")", action);
      break;
    case JmolConstants.PICKING_SELECT_SITE:
      applySelectStyle("visible and within(site, " + spec + ")", action);
      break;
    }
    viewer.clearClickCount();
  }

  private void checkTwoAtomAction(int action, Point3fi ptClicked, int atomIndex) {
    if (!isBound(action, ACTION_pickAtom))
      return;
    boolean isSpin = (pickingMode == JmolConstants.PICKING_SPIN);
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
      if (isSpin)
      viewer.scriptStatus(queuedAtomCount == 1 ?
          GT._("pick one more atom in order to spin the model around an axis") :
          GT._("pick two atoms in order to spin the model around an axis"));
      else
        viewer.scriptStatus(queuedAtomCount == 1 ?
            GT._("pick one more atom in order to display the symmetry relationship") :
            GT._("pick two atoms in order to display the symmetry relationship between them"));
      return;
    }
    String s = measurementQueued.getMeasurementScript(" ", false);
    if (isSpin)
      viewer.script("spin" + s + " " + viewer.getPickingSpinRate());
    else  
      viewer.script("draw symop" + s + ";show symop" + s);
  }

  private int queueAtom(int atomIndex, Point3fi ptClicked) {
    int n = measurementQueued.addPoint(atomIndex, ptClicked, true);
    if (atomIndex >= 0)
      viewer.setStatusAtomPicked(atomIndex, "Atom #" + n + ":"
          + viewer.getAtomInfo(atomIndex));
    return n;
  }

  boolean selectionWorking = false;
  ScriptEvaluator eval;
  private void applySelectStyle(String item, int action) {
    if (measurementPending != null || selectionWorking)
      return;
    selectionWorking = true;
    String s = (isBound(action, ACTION_selectAndNot) ? "selected and not "
        : isBound(action, ACTION_selectOr) ? "selected or " : isBound(action,
            ACTION_selectToggle) ? "selected and not (" + item
            + ") or (not selected) and " : isBound(action,
            ACTION_selectToggleExtended) ? "selected tog " : isBound(action,
            ACTION_select) ? "" : null);
    if (s != null) {
      BitSet bs = getSelectionSet(s + "(" + item + ")");
      if (bs != null)
        viewer.setSelectionSet(bs);
    }
    selectionWorking = false;
  }

  private BitSet getSelectionSet(String script) {
    try {
      if (eval == null)
        eval = new ScriptEvaluator(viewer);
      return viewer.getAtomBitSet(eval, script);
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  protected class MotionPoint {
    int index;
    int x;
    int y;
    long time;

    void set(int index, int x, int y, long time) {
      this.index = index;
      this.x = x;
      this.y = y;
      this.time = time;
    }
    
    public String toString() {
      return "[x = " + x + " y = " + y + " time = " + time + " ]";
    }
  }
  
  protected Gesture dragGesture = new Gesture(20);
  
  protected class Gesture {
    private int action;
    MotionPoint[] nodes;
    private int ptNext;
    private long time0;

    Gesture(int nPoints) {
      nodes = new MotionPoint[nPoints];
      for (int i = 0; i < nPoints; i++)
        nodes[i] = new MotionPoint();
    }
    
    void setAction(int action, long time) {
      this.action = action;
      ptNext = 0;
      time0 = time;
      for (int i = 0; i < nodes.length; i++)
        nodes[i].index = -1;
    }
    
    int getAction() {
      return action;
    }
  
    int add(int action, int x, int y, long time) {
      this.action = action;
      getNode(ptNext).set(ptNext, x, y, time - time0);
      ptNext++;
      return ptNext;
    }
    
    long getTimeDifference(int nPoints) {
      nPoints = getPointCount(nPoints, 0);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1);
      MotionPoint mp0 = getNode(ptNext - nPoints);
      return mp1.time - mp0.time;
    }

    float getSpeedPixelsPerMillisecond(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      float dx = ((float) (mp1.x - mp0.x)) / viewer.getScreenWidth() * 360;
      float dy = ((float) (mp1.y - mp0.y)) / viewer.getScreenHeight() * 360;
      return (float) Math.sqrt(dx * dx + dy * dy) / (mp1.time - mp0.time);
    }

    int getDX(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      return mp1.x - mp0.x;
    }

    int getDY(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      return mp1.y - mp0.y;
    }

    int getPointCount(int nPoints, int nPointsPrevious) {
      if (nPoints > nodes.length - nPointsPrevious)
        nPoints = nodes.length - nPointsPrevious;
      int n = nPoints + 1;
      for (; --n >= 0; )
        if (getNode(ptNext - n - nPointsPrevious).index >= 0)
          break;
      return n;
    }

    MotionPoint getNode(int i) {
      return nodes[(i + nodes.length + nodes.length) % nodes.length];
    }
    
    public String toString() {
      if (nodes.length == 0) return "" + this;
      return Binding.getMouseActionName(action, false) + " nPoints = " + ptNext + " " + nodes[0];
    }
  }

} 
