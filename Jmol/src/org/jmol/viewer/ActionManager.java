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

import javajs.api.GenericPlatform;
import javajs.api.EventManager;
import javajs.awt.event.Event;
import javajs.util.PT;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.MeasurementPending;
import org.jmol.script.T;
import org.jmol.thread.HoverWatcherThread;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Point3fi;

import javajs.util.P3;

import org.jmol.util.Rectangle;
import org.jmol.viewer.binding.Binding;
import org.jmol.viewer.binding.JmolBinding;

public class ActionManager implements EventManager {

  protected Viewer vwr;
  protected boolean haveMultiTouchInput = false;
  
  public Binding b;

  private Binding jmolBinding;
  private Binding pfaatBinding;
  private Binding dragBinding;
  private Binding rasmolBinding;
  private Binding predragBinding;
  private int LEFT_CLICKED;
  private int LEFT_DRAGGED;
  
  /**
   * 
   * @param vwr
   * @param commandOptions
   */
  public void setViewer(Viewer vwr, String commandOptions) {
    this.vwr = vwr;
    setBinding(jmolBinding = new JmolBinding());
    LEFT_CLICKED = Binding.getMouseAction(1, Binding.LEFT, Event.CLICKED);
    LEFT_DRAGGED = Binding.getMouseAction(1, Binding.LEFT, Event.DRAGGED);
  }

  protected Thread hoverWatcherThread;

  public void checkHover() {
    if (!vwr.getInMotion(true) && !vwr.tm.spinOn && !vwr.tm.navOn
        && !vwr.checkObjectHovered(current.x, current.y)) {
      int atomIndex = vwr.findNearestAtomIndex(current.x, current.y);
      if (atomIndex < 0)
        return;
      boolean isLabel = (getAtomPickingMode() == PICKING_LABEL && bnd(
          Binding
              .getMouseAction(clickedCount, moved.modifiers, Event.DRAGGED),
          ACTION_dragLabel));
      vwr.hoverOn(atomIndex, isLabel);
    }
  }


  /**
   * 
   * Specific to ActionManagerMT -- for processing SparshUI gestures
   * 
   * @param groupID
   * @param eventType
   * @param touchID
   * @param iData
   * @param pt
   * @param time
   */
  public void processMultitouchEvent(int groupID, int eventType, int touchID, int iData,
                           P3 pt, long time) {
  }

  boolean bnd(int mouseAction, int jmolAction) {
    return b.isBound(mouseAction, jmolAction);
  }

  /**
   * 
   * @param desc
   * @param name
   */
  void bind(String desc, String name) {
    int jmolAction = getActionFromName(name);
    int mouseAction = Binding.getMouseActionStr(desc);
    if (mouseAction == 0)
      return;
    if (jmolAction >= 0) {
      b.bindAction(mouseAction, jmolAction);
    } else {
      b.bindName(mouseAction, name);
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
    int mouseAction = Binding.getMouseActionStr(desc);
    if (jmolAction >= 0)
      b.unbindAction(mouseAction, jmolAction);
    else if (mouseAction != 0)
      b.unbindName(mouseAction, name);
    if (name == null)
      b.unbindUserAction(desc);
  }

  //// Gestures
  
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

    @Override
    public String toString() {
      return "[x = " + x + " y = " + y + " time = " + time + " ]";
    }
  }

  protected Gesture dragGesture = new Gesture(20);

  public class Gesture {
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

    int add(int action, int x, int y, long time) {
      this.action = action;
      getNode(ptNext).set(ptNext, x, y, time - time0);
      ptNext++;
      return ptNext;
    }

    public long getTimeDifference(int nPoints) {
      nPoints = getPointCount2(nPoints, 0);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1);
      MotionPoint mp0 = getNode(ptNext - nPoints);
      return mp1.time - mp0.time;
    }

    public float getSpeedPixelsPerMillisecond(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount2(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      float dx = ((float) (mp1.x - mp0.x)) / vwr.getScreenWidth() * 360;
      float dy = ((float) (mp1.y - mp0.y)) / vwr.getScreenHeight() * 360;
      return (float) Math.sqrt(dx * dx + dy * dy) / (mp1.time - mp0.time);
    }

    int getDX(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount2(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      return mp1.x - mp0.x;
    }

    int getDY(int nPoints, int nPointsPrevious) {
      nPoints = getPointCount2(nPoints, nPointsPrevious);
      if (nPoints < 2)
        return 0;
      MotionPoint mp1 = getNode(ptNext - 1 - nPointsPrevious);
      MotionPoint mp0 = getNode(ptNext - nPoints - nPointsPrevious);
      return mp1.y - mp0.y;
    }

    int getPointCount() {
      return ptNext;
    }

    private int getPointCount2(int nPoints, int nPointsPrevious) {
      if (nPoints > nodes.length - nPointsPrevious)
        nPoints = nodes.length - nPointsPrevious;
      int n = nPoints + 1;
      for (; --n >= 0;)
        if (getNode(ptNext - n - nPointsPrevious).index >= 0)
          break;
      return n;
    }

    MotionPoint getNode(int i) {
      return nodes[(i + nodes.length + nodes.length) % nodes.length];
    }

    @Override
    public String toString() {
      if (nodes.length == 0)
        return "" + this;
      return Binding.getMouseActionName(action, false) + " nPoints = " + ptNext
          + " " + nodes[0];
    }
  }

  /*
   * a "Jmol action" is one of these:
   * 
   * A Jmol action is "bound" to a mouse action by the 
   * simple act of concatenating string "jmol action" + \t + "mouse action"
   *  
   * 
   */
  public final static int ACTION_assignNew = 0;
  public final static int ACTION_center = 1;
  public final static int ACTION_clickFrank = 2;
  public final static int ACTION_connectAtoms = 3;
  public final static int ACTION_deleteAtom = 4;
  public final static int ACTION_deleteBond = 5;
  public final static int ACTION_depth = 6;
  public final static int ACTION_dragAtom = 7;
  public final static int ACTION_dragDrawObject = 8;
  public final static int ACTION_dragDrawPoint = 9;
  public final static int ACTION_dragLabel = 10;
  public final static int ACTION_dragMinimize = 11;
  public final static int ACTION_dragMinimizeMolecule = 12;
  public final static int ACTION_dragSelected = 13;
  public final static int ACTION_dragZ = 14;
  public final static int ACTION_multiTouchSimulation = 15;
  public final static int ACTION_navTranslate = 16;
  public final static int ACTION_pickAtom = 17;
  public final static int ACTION_pickIsosurface = 18;
  public final static int ACTION_pickLabel = 19;
  public final static int ACTION_pickMeasure = 20;
  public final static int ACTION_pickNavigate = 21;
  public final static int ACTION_pickPoint = 22;
  public final static int ACTION_popupMenu = 23;
  public final static int ACTION_reset = 24;
  public final static int ACTION_rotate = 25;
  public final static int ACTION_rotateBranch = 26;
  public final static int ACTION_rotateSelected = 27;
  public final static int ACTION_rotateZ = 28;
  public final static int ACTION_rotateZorZoom = 29;
  public final static int ACTION_select = 30;
  public final static int ACTION_selectAndDrag = 31;
  public final static int ACTION_selectAndNot = 32;
  public final static int ACTION_selectNone = 33;
  public final static int ACTION_selectOr = 34;
  public final static int ACTION_selectToggle = 35;
  public final static int ACTION_selectToggleExtended = 36;
  public final static int ACTION_setMeasure = 37;
  public final static int ACTION_slab = 38;
  public final static int ACTION_slabAndDepth = 39;
  public final static int ACTION_slideZoom = 40;
  public final static int ACTION_spinDrawObjectCCW = 41;
  public final static int ACTION_spinDrawObjectCW = 42;
  public final static int ACTION_stopMotion = 43;
  public final static int ACTION_swipe = 44;
  public final static int ACTION_translate = 45;
  public final static int ACTION_wheelZoom = 46;
  public final static int ACTION_count = 47;

  final static String[] actionInfo = new String[ACTION_count];
  final static String[] actionNames = new String[ACTION_count];

  static void newAction(int i, String name, String info) {
    actionInfo[i] = info;
    actionNames[i] = name;
  }

  static {
    // OK for J2S because actionInfo and actionNames are both private
    newAction(ACTION_assignNew, "_assignNew", GT.o(GT._(
        "assign/new atom or bond (requires {0})"),
        "set picking assignAtom_??/assignBond_?"));
    newAction(ACTION_center, "_center", GT._("center"));
    newAction(ACTION_clickFrank, "_clickFrank", GT
        ._("pop up recent context menu (click on Jmol frank)"));
    newAction(ACTION_deleteAtom, "_deleteAtom", GT.o(GT._(
        "delete atom (requires {0})"), "set picking DELETE ATOM"));
    newAction(ACTION_deleteBond, "_deleteBond", GT.o(GT._(
        "delete bond (requires {0})"), "set picking DELETE BOND"));
    newAction(ACTION_depth, "_depth", GT.o(GT._(
        "adjust depth (back plane; requires {0})"), "SLAB ON"));
    newAction(ACTION_dragAtom, "_dragAtom", GT.o(GT._("move atom (requires {0})"),
        "set picking DRAGATOM"));
    newAction(ACTION_dragDrawObject, "_dragDrawObject", GT.o(GT._(
        "move whole DRAW object (requires {0})"), "set picking DRAW"));
    newAction(ACTION_dragDrawPoint, "_dragDrawPoint", GT.o(GT._(
        "move specific DRAW point (requires {0})"), "set picking DRAW"));
    newAction(ACTION_dragLabel, "_dragLabel", GT.o(GT._("move label (requires {0})"),
        "set picking LABEL"));
    newAction(ACTION_dragMinimize, "_dragMinimize", GT.o(GT._(
        "move atom and minimize molecule (requires {0})"),
        "set picking DRAGMINIMIZE"));
    newAction(ACTION_dragMinimizeMolecule, "_dragMinimizeMolecule", GT.o(GT._(
        "move and minimize molecule (requires {0})"),
        "set picking DRAGMINIMIZEMOLECULE"));
    newAction(ACTION_dragSelected, "_dragSelected", GT.o(GT._(
        "move selected atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_dragZ, "_dragZ", GT.o(GT._(
        "drag atoms in Z direction (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_multiTouchSimulation, "_multiTouchSimulation", GT
        ._("simulate multi-touch using the mouse)"));
    newAction(ACTION_navTranslate, "_navTranslate", GT.o(GT._(
        "translate navigation point (requires {0} and {1})"), new String[] {
            "set NAVIGATIONMODE", "set picking NAVIGATE" }));
    newAction(ACTION_pickAtom, "_pickAtom", GT._("pick an atom"));
    newAction(ACTION_connectAtoms, "_pickConnect", GT.o(GT._(
        "connect atoms (requires {0})"), "set picking CONNECT"));
    newAction(ACTION_pickIsosurface, "_pickIsosurface", GT.o(GT._(
        "pick an ISOSURFACE point (requires {0}"), "set DRAWPICKING"));
    newAction(ACTION_pickLabel, "_pickLabel", GT.o(GT._(
        "pick a label to toggle it hidden/displayed (requires {0})"),
        "set picking LABEL"));
    newAction(
        ACTION_pickMeasure,
        "_pickMeasure",
        GT.o(GT
            ._(
                "pick an atom to include it in a measurement (after starting a measurement or after {0})"),
                "set picking DISTANCE/ANGLE/TORSION"));
    newAction(ACTION_pickNavigate, "_pickNavigate", GT.o(GT._(
        "pick a point or atom to navigate to (requires {0})"),
        "set NAVIGATIONMODE"));
    newAction(ACTION_pickPoint, "_pickPoint", GT.o(GT
        ._("pick a DRAW point (for measurements) (requires {0}"),
            "set DRAWPICKING"));
    newAction(ACTION_popupMenu, "_popupMenu", GT
        ._("pop up the full context menu"));
    newAction(ACTION_reset, "_reset", GT
        ._("reset (when clicked off the model)"));
    newAction(ACTION_rotate, "_rotate", GT._("rotate"));
    newAction(ACTION_rotateBranch, "_rotateBranch", GT.o(GT._(
        "rotate branch around bond (requires {0})"), "set picking ROTATEBOND"));
    newAction(ACTION_rotateSelected, "_rotateSelected", GT.o(GT._(
        "rotate selected atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_rotateZ, "_rotateZ", GT._("rotate Z"));
    newAction(
        ACTION_rotateZorZoom,
        "_rotateZorZoom",
        GT
            ._("rotate Z (horizontal motion of mouse) or zoom (vertical motion of mouse)"));
    newAction(ACTION_select, "_select", GT.o(GT._("select an atom (requires {0})"),
        "set pickingStyle EXTENDEDSELECT"));
    newAction(ACTION_selectAndDrag, "_selectAndDrag", GT.o(GT._(
        "select and drag atoms (requires {0})"), "set DRAGSELECTED"));
    newAction(ACTION_selectAndNot, "_selectAndNot", GT.o(GT._(
        "unselect this group of atoms (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT"));
    newAction(ACTION_selectNone, "_selectNone", GT.o(GT._(
        "select NONE (requires {0})"), "set pickingStyle EXTENDEDSELECT"));
    newAction(ACTION_selectOr, "_selectOr", GT.o(GT._(
        "add this group of atoms to the set of selected atoms (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT"));
    newAction(ACTION_selectToggle, "_selectToggle", GT.o(GT._(
        "toggle selection (requires {0})"),
        "set pickingStyle DRAG/EXTENDEDSELECT/RASMOL"));
    newAction(
        ACTION_selectToggleExtended,
        "_selectToggleOr",
        GT.o(GT
            ._(
                "if all are selected, unselect all, otherwise add this group of atoms to the set of selected atoms (requires {0})"),
                "set pickingStyle DRAG"));
    newAction(ACTION_setMeasure, "_setMeasure", GT
        ._("pick an atom to initiate or conclude a measurement"));
    newAction(ACTION_slab, "_slab", GT.o(GT._(
        "adjust slab (front plane; requires {0})"), "SLAB ON"));
    newAction(ACTION_slabAndDepth, "_slabAndDepth", GT.o(GT._(
        "move slab/depth window (both planes; requires {0})"), "SLAB ON"));
    newAction(ACTION_slideZoom, "_slideZoom", GT
        ._("zoom (along right edge of window)"));
    newAction(
        ACTION_spinDrawObjectCCW,
        "_spinDrawObjectCCW",
        GT.o(GT
            ._(
                "click on two points to spin around axis counterclockwise (requires {0})"),
                "set picking SPIN"));
    newAction(ACTION_spinDrawObjectCW, "_spinDrawObjectCW", GT.o(GT._(
        "click on two points to spin around axis clockwise (requires {0})"),
        "set picking SPIN"));
    newAction(ACTION_stopMotion, "_stopMotion", GT.o(GT._(
        "stop motion (requires {0})"), "set waitForMoveTo FALSE"));
    newAction(
        ACTION_swipe,
        "_swipe",
        GT
            ._("spin model (swipe and release button and stop motion simultaneously)"));
    newAction(ACTION_translate, "_translate", GT._("translate"));
    newAction(ACTION_wheelZoom, "_wheelZoom", GT._("zoom"));

  }

  public static String getActionName(int i) {
    return (i < actionNames.length ? actionNames[i] : null);
  }

  public static int getActionFromName(String name) {
    for (int i = 0; i < actionNames.length; i++)
      if (actionNames[i].equalsIgnoreCase(name))
        return i;
    return -1;
  }

  public String getBindingInfo(String qualifiers) {
    return b.getBindingInfo(actionInfo, actionNames, qualifiers);
  }

  protected void setBinding(Binding newBinding) {
    // overridden in ActionManagerMT
    b = newBinding;
  }

  /**
   * picking modes set picking....
   */
  
  private int apm = PICKING_IDENTIFY;
  private int bondPickingMode;

  public final static int PICKING_OFF = 0;
  public final static int PICKING_IDENTIFY = 1;
  public final static int PICKING_LABEL = 2;
  public final static int PICKING_CENTER = 3;
  public final static int PICKING_DRAW = 4;
  public final static int PICKING_SPIN = 5;
  public final static int PICKING_SYMMETRY = 6;
  public final static int PICKING_DELETE_ATOM = 7;
  public final static int PICKING_DELETE_BOND = 8;
  public final static int PICKING_SELECT_ATOM = 9;
  public final static int PICKING_SELECT_GROUP = 10;
  public final static int PICKING_SELECT_CHAIN = 11;
  public final static int PICKING_SELECT_MOLECULE = 12;
  public final static int PICKING_SELECT_POLYMER = 13;
  public final static int PICKING_SELECT_STRUCTURE = 14;
  public final static int PICKING_SELECT_SITE = 15;
  public final static int PICKING_SELECT_MODEL = 16;
  public final static int PICKING_SELECT_ELEMENT = 17;
  public final static int PICKING_MEASURE = 18;
  public final static int PICKING_MEASURE_DISTANCE = 19;
  public final static int PICKING_MEASURE_ANGLE = 20;
  public final static int PICKING_MEASURE_TORSION = 21;
  public final static int PICKING_MEASURE_SEQUENCE = 22;
  public final static int PICKING_NAVIGATE = 23;
  public final static int PICKING_CONNECT = 24;
  public final static int PICKING_STRUTS = 25;
  public final static int PICKING_DRAG_SELECTED = 26;
  public final static int PICKING_DRAG_MOLECULE = 27;
  public final static int PICKING_DRAG_ATOM = 28;
  public final static int PICKING_DRAG_MINIMIZE = 29;
  public final static int PICKING_DRAG_MINIMIZE_MOLECULE = 30; // for docking
  public final static int PICKING_INVERT_STEREO = 31;
  public final static int PICKING_ASSIGN_ATOM = 32;
  public final static int PICKING_ASSIGN_BOND = 33;
  public final static int PICKING_ROTATE_BOND = 34;
  public final static int PICKING_IDENTIFY_BOND = 35;
  public final static int PICKING_DRAG_LIGAND = 36;

  /**
   * picking styles
   */
  public final static int PICKINGSTYLE_SELECT_JMOL = 0;
  public final static int PICKINGSTYLE_SELECT_CHIME = 0;
  public final static int PICKINGSTYLE_SELECT_RASMOL = 1;
  public final static int PICKINGSTYLE_SELECT_PFAAT = 2;
  public final static int PICKINGSTYLE_SELECT_DRAG = 3;
  public final static int PICKINGSTYLE_MEASURE_ON = 4;
  public final static int PICKINGSTYLE_MEASURE_OFF = 5;

  private final static String[] pickingModeNames;
  static {
    pickingModeNames = "off identify label center draw spin symmetry deleteatom deletebond atom group chain molecule polymer structure site model element measure distance angle torsion sequence navigate connect struts dragselected dragmolecule dragatom dragminimize dragminimizemolecule invertstereo assignatom assignbond rotatebond identifybond dragligand".split(" ");
  }
  
  public final static String getPickingModeName(int pickingMode) {
    return (pickingMode < 0 || pickingMode >= pickingModeNames.length ? "off"
        : pickingModeNames[pickingMode]);
  }

  public final static int getPickingMode(String str) {
    for (int i = pickingModeNames.length; --i >= 0;)
      if (str.equalsIgnoreCase(pickingModeNames[i]))
        return i;
    return -1;
  }

  private final static String[] pickingStyleNames;
  
  static {
    pickingStyleNames = "toggle selectOrToggle extendedSelect drag measure measureoff".split(" ");
  }
  
  public final static String getPickingStyleName(int pickingStyle) {
    return (pickingStyle < 0 || pickingStyle >= pickingStyleNames.length ? "toggle"
        : pickingStyleNames[pickingStyle]);
  }

  public final static int getPickingStyleIndex(String str) {
    for (int i = pickingStyleNames.length; --i >= 0;)
      if (str.equalsIgnoreCase(pickingStyleNames[i]))
        return i;
    return -1;
  }

  int getAtomPickingMode() {
    return apm;
  }

  void setPickingMode(int pickingMode) {
    boolean isNew = false;
    switch (pickingMode) {
    case -1: // from  set modelkit OFF
      isNew = true;
      bondPickingMode = PICKING_IDENTIFY_BOND;
      pickingMode = PICKING_IDENTIFY;
      break;
    case PICKING_IDENTIFY_BOND:
    case PICKING_ROTATE_BOND:
    case PICKING_ASSIGN_BOND:
      vwr.setBooleanProperty("bondPicking", true);
      bondPickingMode = pickingMode;
      return;
    case PICKING_DELETE_BOND:
      bondPickingMode = pickingMode;
      if (vwr.getBondPicking())
        return;
      isNew = true;
      break;
    // if we have bondPicking mode, then we don't set atomPickingMode to this
    }
    isNew |= (apm != pickingMode);
    apm = pickingMode;
    if (isNew)
      resetMeasurement();
  }

  void setAtomPickingOption(String option) {
    switch (apm) {
    case PICKING_ASSIGN_ATOM:
      pickAtomAssignType = option;
      isPickAtomAssignCharge = (pickAtomAssignType.equals("Pl") || pickAtomAssignType
          .equals("Mi"));
      break;
    }
  }

  void setBondPickingOption(String option) {
    switch (bondPickingMode) {
    case PICKING_ASSIGN_BOND:
      pickBondAssignType = Character.toLowerCase(option.charAt(0));
      break;
    }
  }

  private int pickingStyle;
  private int pickingStyleSelect = PICKINGSTYLE_SELECT_JMOL;
  private int pickingStyleMeasure = PICKINGSTYLE_MEASURE_OFF;
  private int rootPickingStyle = PICKINGSTYLE_SELECT_JMOL;
  private String pickAtomAssignType = "C";
  private char pickBondAssignType = 'p';
  private boolean isPickAtomAssignCharge;

  public String getPickingState() {
    // the pickingMode is not reported in the state. But when we do an UNDO,
    // we want to restore this.
    String script = ";set modelkitMode " + vwr.getBoolean(T.modelkitmode)
        + ";set picking " + getPickingModeName(apm);
    if (apm == PICKING_ASSIGN_ATOM)
      script += "_" + pickAtomAssignType;
    script += ";";
    if (bondPickingMode != PICKING_OFF)
      script += "set picking " + getPickingModeName(bondPickingMode);
    if (bondPickingMode == PICKING_ASSIGN_BOND)
      script += "_" + pickBondAssignType;
    script += ";";
    return script;
  }

  int getPickingStyle() {
    return pickingStyle;
  }

  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (pickingStyle >= PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      resetMeasurement();
    } else {
      if (pickingStyle < PICKINGSTYLE_SELECT_DRAG)
        rootPickingStyle = pickingStyle;
      pickingStyleSelect = pickingStyle;
    }
    rubberbandSelectionMode = false;
    switch (pickingStyleSelect) {
    case PICKINGSTYLE_SELECT_PFAAT:
      if (!b.name.equals("extendedSelect"))
        setBinding(pfaatBinding == null ? pfaatBinding = Binding
            .newBinding("Pfaat") : pfaatBinding);
      break;
    case PICKINGSTYLE_SELECT_DRAG:
      if (!b.name.equals("drag"))
        setBinding(dragBinding == null ? dragBinding = Binding
            .newBinding("Drag") : dragBinding);
      rubberbandSelectionMode = true;
      break;
    case PICKINGSTYLE_SELECT_RASMOL:
      if (!b.name.equals("selectOrToggle"))
        setBinding(rasmolBinding == null ? rasmolBinding = Binding
            .newBinding("Rasmol") : rasmolBinding);
      break;
    default:
      if (b != jmolBinding)
        setBinding(jmolBinding);
    }
    if (!b.name.equals("drag"))
      predragBinding = b;
  }


  
  private final static long MAX_DOUBLE_CLICK_MILLIS = 700;
  protected final static long MININUM_GESTURE_DELAY_MILLISECONDS = 10;
  private final static int SLIDE_ZOOM_X_PERCENT = 98;
  public final static float DEFAULT_MOUSE_DRAG_FACTOR = 1f;
  public final static float DEFAULT_MOUSE_WHEEL_FACTOR = 1.15f;
  public final static float DEFAULT_GESTURE_SWIPE_FACTOR = 1f;


  protected int xyRange = 0;

  private float gestureSwipeFactor = DEFAULT_GESTURE_SWIPE_FACTOR;
  protected float mouseDragFactor = DEFAULT_MOUSE_DRAG_FACTOR;
  protected float mouseWheelFactor = DEFAULT_MOUSE_WHEEL_FACTOR;

  void setGestureSwipeFactor(float factor) {
    gestureSwipeFactor = factor;
  }

  void setMouseDragFactor(float factor) {
    mouseDragFactor = factor;
  }

  void setMouseWheelFactor(float factor) {
    mouseWheelFactor = factor;
  }

  protected final MouseState current = new MouseState("current");
  protected final MouseState moved = new MouseState("moved");
  private final MouseState clicked = new MouseState("clicked");
  private final MouseState pressed = new MouseState("pressed");
  private final MouseState dragged = new MouseState("dragged");

  protected void setCurrent(long time, int x, int y, int mods) {
    vwr.hoverOff();
    current.set(time, x, y, mods);
  }

  int getCurrentX() {
    return current.x;
  }

  int getCurrentY() {
    return current.y;
  }

  protected int pressedCount;
  protected int clickedCount;

  private boolean drawMode;
  private boolean labelMode;
  private boolean dragSelectedMode;
  private boolean measuresEnabled = true;
  private boolean haveSelection;

  public void setMouseMode() {
    drawMode = labelMode = false;
    dragSelectedMode = vwr.getDragSelected();
    measuresEnabled = !dragSelectedMode;
    if (!dragSelectedMode)
      switch (apm) {
      default:
        return;
      case PICKING_ASSIGN_ATOM:
        measuresEnabled = !isPickAtomAssignCharge;
        return;
      case PICKING_DRAW:
        drawMode = true;
        // drawMode and dragSelectedMode are incompatible
        measuresEnabled = false;
        break;
      //other cases here?
      case PICKING_LABEL:
        labelMode = true;
        measuresEnabled = false;
        break;
      case PICKING_SELECT_ATOM:
        measuresEnabled = false;
        break;
      case PICKING_MEASURE_DISTANCE:
      case PICKING_MEASURE_SEQUENCE:
      case PICKING_MEASURE_ANGLE:
      case PICKING_MEASURE_TORSION:
        measuresEnabled = false;
        return;
        //break;
      }
    exitMeasurementMode(null);
  }

  protected void clearMouseInfo() {
    // when a second touch is made, this clears all record of first touch
    pressedCount = clickedCount = 0;
    dragGesture.setAction(0, 0);
    exitMeasurementMode(null);
  }

  private boolean hoverActive = false;

  private MeasurementPending mp;
  private int dragAtomIndex = -1;

  private boolean rubberbandSelectionMode = false;
  private final Rectangle rectRubber = new Rectangle();

  private boolean isAltKeyReleased = true;
  private boolean keyProcessing;

  protected boolean isMultiTouchClient;
  protected boolean isMultiTouchServer;

  public boolean isMTClient() {
    return isMultiTouchClient;
  }

  public boolean isMTServer() {
    return isMultiTouchServer;
  }

  public void dispose() {
    clear();
  }

  public void clear() {
    startHoverWatcher(false);
    if (predragBinding != null)
      b = predragBinding;
    vwr.setPickingMode(null, PICKING_IDENTIFY);
    vwr.setPickingStyle(null, rootPickingStyle);
    isAltKeyReleased = true;
  }

  synchronized public void startHoverWatcher(boolean isStart) {
    if (vwr.isPreviewOnly())
      return;
    try {
      if (isStart) {
        if (hoverWatcherThread != null)
          return;
        current.time = -1;
        hoverWatcherThread = new HoverWatcherThread(this, current, moved,
            vwr);
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

  /**
   * only NONE (-1) is implemented; it just stops the hoverWatcher thread so
   * that the vwr references are all removed
   * 
   * @param modeMouse
   */
  public void setModeMouse(int modeMouse) {
    if (modeMouse == JC.MOUSE_NONE) {
      startHoverWatcher(false);
    }
  }

  /**
   * called by MouseManager.keyPressed
   * 
   * @param key
   * @param modifiers
   * @return true if handled 
   */
  @Override
  public boolean keyPressed(int key, int modifiers) {
    if (keyProcessing)
      return false;
    vwr.hoverOff();
    keyProcessing = true;
    switch (key) {
    case Event.VK_ALT:
      if (dragSelectedMode && isAltKeyReleased)
        vwr.moveSelected(Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      isAltKeyReleased = false;
      moved.modifiers |= Binding.ALT;
      break;
    case Event.VK_SHIFT:
      dragged.modifiers |= Binding.SHIFT;
      moved.modifiers |= Binding.SHIFT;
      break;
    case Event.VK_CONTROL:
      moved.modifiers |= Binding.CTRL;
      break;
    case Event.VK_ESCAPE:
      exitMeasurementMode("escape");
      break;
    }
    int action = Binding.LEFT | Binding.SINGLE | Binding.DRAG | moved.modifiers;
    if (!labelMode && !b.isUserAction(action))
      checkMotionRotateZoom(action, current.x, 0, 0, false);
    if (vwr.getBoolean(T.navigationmode)) {
      // if (vwr.getBooleanProperty("showKeyStrokes", false))
      // vwr.evalStringQuiet("!set echo bottom left;echo "
      // + (i == 0 ? "" : i + " " + m));
      switch (key) {
      case Event.VK_UP:
      case Event.VK_DOWN:
      case Event.VK_LEFT:
      case Event.VK_RIGHT:
      case Event.VK_SPACE:
      case Event.VK_PERIOD:
        vwr.navigate(key, modifiers);
        break;
      }
    }
    keyProcessing = false;
    return true;
  }

  @Override
  public void keyReleased(int key) {
    switch (key) {
    case Event.VK_ALT:
      if (dragSelectedMode)
        vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      isAltKeyReleased = true;
      moved.modifiers &= ~Binding.ALT;
      break;
    case Event.VK_SHIFT:
      moved.modifiers &= ~Binding.SHIFT;
      break;
    case Event.VK_CONTROL:
      moved.modifiers &= ~Binding.CTRL;
    }
    if (moved.modifiers == 0)
      vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    if (!vwr.getBoolean(T.navigationmode))
      return;
    //if (vwr.getBooleanProperty("showKeyStrokes", false))
    //vwr.evalStringQuiet("!set echo bottom left;echo;");
    switch (key) {
    case Event.VK_UP:
    case Event.VK_DOWN:
    case Event.VK_LEFT:
    case Event.VK_RIGHT:
      vwr.navigate(0, 0);
      break;
    }
  }

  @Override
  public void mouseEnterExit(long time, int x, int y, boolean isExit) {
    setCurrent(time, x, y, 0);
    if (isExit)
      exitMeasurementMode("mouseExit");
  }

  private int pressAction;
  private int dragAction;
  private int clickAction;

  private void setMouseActions(int count, int buttonMods, boolean isRelease) {
    pressAction = Binding.getMouseAction(count, buttonMods,
        isRelease ? Event.RELEASED : Event.PRESSED);
    dragAction = Binding.getMouseAction(count, buttonMods, Event.DRAGGED);
    clickAction = Binding.getMouseAction(count, buttonMods, Event.CLICKED);
  }

  /**
   * 
   * @param mode
   *        MOVED PRESSED DRAGGED RELEASED CLICKED WHEELED
   * @param time
   * @param x
   * @param y
   * @param count
   * @param buttonMods
   *        LEFT RIGHT MIDDLE WHEEL SHIFT ALT CTRL
   */
  @Override
  public void mouseAction(int mode, long time, int x, int y, int count,
                          int buttonMods) {
    if (!vwr.getMouseEnabled())
      return;
    if (Logger.debuggingHigh && mode != Event.MOVED)
      vwr.showString("mouse action: " + mode + " " + buttonMods + " " + Binding.getMouseActionName(Binding.getMouseAction(count, buttonMods, mode), false), false);

    switch (mode) {
    case Event.MOVED:
      setCurrent(time, x, y, buttonMods);
      moved.setCurrent(current, 0);
      if (mp != null || hoverActive) {
        clickAction = Binding.getMouseAction(clickedCount, buttonMods,
            Event.MOVED);
        checkClickAction(x, y, time, 0);
        return;
      }
      if (isZoomArea(x)) {
        checkMotionRotateZoom(LEFT_DRAGGED, 0, 0, 0, false);
        return;
      }
      if (vwr.currentCursor == GenericPlatform.CURSOR_ZOOM)//if (dragSelectedMode)
        vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
      return;
    case Event.PRESSED:
      setMouseMode();
      pressedCount = (pressed.check(0, 0, 0, buttonMods, time,
          MAX_DOUBLE_CLICK_MILLIS) ? pressedCount + 1 : 1);
      if (pressedCount == 1) {
        vwr.checkInMotion(1);
        setCurrent(time, x, y, buttonMods);
      }
      pressAction = Binding.getMouseAction(pressedCount, buttonMods,
          Event.PRESSED);
      vwr.setCursor(GenericPlatform.CURSOR_HAND);
      pressed.setCurrent(current, 1);
      dragged.setCurrent(current, 1);
      vwr.setFocus();
      dragGesture.setAction(dragAction, time);
      checkPressedAction(x, y, time);
      return;
    case Event.DRAGGED:
      setMouseMode();
      setMouseActions(pressedCount, buttonMods, false);
      int deltaX = x - dragged.x;
      int deltaY = y - dragged.y;
      setCurrent(time, x, y, buttonMods);
      dragged.setCurrent(current, -1);
      if (apm != PICKING_ASSIGN_ATOM)
        exitMeasurementMode(null);
      dragGesture.add(dragAction, x, y, time);
      checkDragWheelAction(dragAction, x, y, deltaX, deltaY, time,
          Event.DRAGGED);
      return;
    case Event.RELEASED:
      setMouseActions(pressedCount, buttonMods, true);
      setCurrent(time, x, y, buttonMods);
      vwr.spinXYBy(0, 0, 0);
      boolean dragRelease = !pressed.check(xyRange, x, y, buttonMods, time,
          Long.MAX_VALUE);
      checkReleaseAction(x, y, time, dragRelease);
      return;
    case Event.WHEELED:
      if (vwr.isApplet() && !vwr.hasFocus())
        return;
      setCurrent(time, current.x, current.y, buttonMods);
      checkDragWheelAction(Binding.getMouseAction(0, buttonMods,
          Event.WHEELED), current.x, current.y, 0, y, time, Event.WHEELED);
      return;
    case Event.CLICKED:
      setMouseMode();
      clickedCount = (count > 1 ? count : clicked.check(0, 0, 0, buttonMods,
          time, MAX_DOUBLE_CLICK_MILLIS) ? clickedCount + 1 : 1);
      if (clickedCount == 1) {
        setCurrent(time, x, y, buttonMods);
      }
      setMouseActions(clickedCount, buttonMods, false);
      clicked.setCurrent(current, clickedCount);
      vwr.setFocus();
      if (apm != PICKING_SELECT_ATOM
          && bnd(Binding.getMouseAction(1, buttonMods, Event.PRESSED),
              ACTION_selectAndDrag))
        return;
      clickAction = Binding.getMouseAction(clickedCount, buttonMods,
          Event.CLICKED);
      checkClickAction(x, y, time, clickedCount);
      return;
    }
  }

  private void checkPressedAction(int x, int y, long time) {
    int buttonMods = Binding.getButtonMods(pressAction);
    boolean isSelectAndDrag = bnd(Binding.getMouseAction(1, buttonMods,
        Event.PRESSED), ACTION_selectAndDrag);
    if (buttonMods != 0) {
      pressAction = vwr.notifyMouseClicked(x, y, pressAction,
          Event.PRESSED);
      if (pressAction == 0)
        return;
      buttonMods = Binding.getButtonMods(pressAction);
    }
    setMouseActions(pressedCount, buttonMods, false);
    if (Logger.debugging)
      Logger.debug(Binding.getMouseActionName(pressAction, false));

    if (drawMode
        && (bnd(dragAction, ACTION_dragDrawObject) || bnd(dragAction,
            ACTION_dragDrawPoint)) || labelMode
        && bnd(dragAction, ACTION_dragLabel)) {
      vwr.checkObjectDragged(Integer.MIN_VALUE, 0, x, y, dragAction);
      return;
    }
    checkUserAction(pressAction, x, y, 0, 0, time, Event.PRESSED);
    boolean isBound = false;
    switch (apm) {
    case PICKING_ASSIGN_ATOM:
      isBound = bnd(clickAction, ACTION_assignNew);
      break;
    case PICKING_DRAG_ATOM:
      isBound = bnd(dragAction, ACTION_dragAtom)
          || bnd(dragAction, ACTION_dragZ);
      break;
    case PICKING_DRAG_SELECTED:
    case PICKING_DRAG_LIGAND:
    case PICKING_DRAG_MOLECULE:
      isBound = bnd(dragAction, ACTION_dragAtom)
          || bnd(dragAction, ACTION_rotateSelected)
          || bnd(dragAction, ACTION_dragZ);
      break;
    case PICKING_DRAG_MINIMIZE:
      isBound = bnd(dragAction, ACTION_dragMinimize)
          || bnd(dragAction, ACTION_dragZ);
      break;
    case PICKING_DRAG_MINIMIZE_MOLECULE:
      isBound = bnd(dragAction, ACTION_dragMinimizeMolecule)
          || bnd(dragAction, ACTION_rotateSelected)
          || bnd(dragAction, ACTION_dragZ);
      break;
    }
    if (isBound) {
      dragAtomIndex = vwr.findNearestAtomIndexMovable(x, y, true);
      if (dragAtomIndex >= 0
          && (apm == PICKING_ASSIGN_ATOM || apm == PICKING_INVERT_STEREO)
          && vwr.isAtomAssignable(dragAtomIndex)) {
        enterMeasurementMode(dragAtomIndex);
        mp.addPoint(dragAtomIndex, null, false);
      }
      return;
    }
    if (bnd(pressAction, ACTION_popupMenu)) {
      char type = 'j';
      if (vwr.getBoolean(T.modelkitmode)) {
        Map<String, Object> t = vwr.checkObjectClicked(x, y, LEFT_CLICKED);
        type = (t != null && "bond".equals(t.get("type")) ? 'b' : vwr
            .findNearestAtomIndex(x, y) >= 0 ? 'a' : 'm');
      }
      vwr.popupMenu(x, y, type);
      return;
    }
    if (dragSelectedMode) {
      haveSelection = true;
      if (isSelectAndDrag) {
        haveSelection = (vwr.findNearestAtomIndexMovable(x, y, true) >= 0);
        // checkPointOrAtomClicked(x, y, mods, pressedCount, true);
      }
      if (!haveSelection)
        return;
      if (bnd(dragAction, ACTION_dragSelected)
          || bnd(dragAction, ACTION_dragZ))
        vwr.moveSelected(Integer.MIN_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      return;
    }
    if (vwr.g.useArcBall)
      vwr.rotateArcBall(x, y, 0);
    checkMotionRotateZoom(dragAction, x, 0, 0, true);
  }

  private void checkDragWheelAction(int dragWheelAction, int x, int y,
                                    int deltaX, int deltaY, long time, int mode) {
    int buttonmods = Binding.getButtonMods(dragWheelAction);
    if (buttonmods != 0) {
      int newAction = vwr.notifyMouseClicked(x, y, Binding.getMouseAction(
          pressedCount, buttonmods, mode), mode); // why was this "-pressedCount"? passing to user?
      if (newAction == 0)
        return;
      if (newAction > 0)
        dragWheelAction = newAction;
    }

    if (isRubberBandSelect(dragWheelAction)) {
      calcRectRubberBand();
      vwr.refresh(3, "rubberBand selection");
      return;
    }

    if (checkUserAction(dragWheelAction, x, y, deltaX, deltaY, time, mode))
      return;

    if (vwr.getRotateBondIndex() >= 0) {
      if (bnd(dragWheelAction, ACTION_rotateBranch)) {
        vwr.moveSelected(deltaX, deltaY, Integer.MIN_VALUE, x, y, null,
            false, false);
        return;
      }
      if (!bnd(dragWheelAction, ACTION_rotate))
        vwr.setRotateBondIndex(-1);
    }
    BS bs = null;
    if (dragAtomIndex >= 0) {
      switch (apm) {
      case PICKING_DRAG_SELECTED:
        setMotion(GenericPlatform.CURSOR_MOVE, true);
        if (bnd(dragWheelAction, ACTION_rotateSelected)
            && vwr.getBoolean(T.allowrotateselected)) {
          vwr.rotateSelected(getDegrees(deltaX, true), getDegrees(deltaY, false),
              null);
        } else {
          vwr.moveSelected(deltaX, deltaY, (bnd(dragWheelAction,
              ACTION_dragZ) ? -deltaY : Integer.MIN_VALUE), Integer.MIN_VALUE,
              Integer.MIN_VALUE, null, true, false);
        }
        return;
      case PICKING_DRAG_LIGAND:
      case PICKING_DRAG_MOLECULE:
      case PICKING_DRAG_MINIMIZE_MOLECULE:
        bs = vwr.ms.getAtoms(T.molecule, BSUtil.newAndSetBit(dragAtomIndex));
        if (apm == PICKING_DRAG_LIGAND)
          bs.and(vwr.getAtomBitSet("ligand"));
        //$FALL-THROUGH$
      case PICKING_DRAG_ATOM:
      case PICKING_DRAG_MINIMIZE:
        if (dragGesture.getPointCount() == 1)
          vwr.undoMoveActionClear(dragAtomIndex, AtomCollection.TAINT_COORD,
              true);
        setMotion(GenericPlatform.CURSOR_MOVE, true);
        if (bnd(dragWheelAction, ACTION_rotateSelected)) {
          vwr.rotateSelected(getDegrees(deltaX, true), getDegrees(deltaY, false),
              bs);
        } else {
          switch (apm) {
          case PICKING_DRAG_LIGAND:
          case PICKING_DRAG_MOLECULE:
          case PICKING_DRAG_MINIMIZE_MOLECULE:
            vwr.select(bs, false, 0, true);
            break;
          }
          vwr
              .moveAtomWithHydrogens(dragAtomIndex, deltaX, deltaY,
                  (bnd(dragWheelAction, ACTION_dragZ) ? -deltaY
                      : Integer.MIN_VALUE), bs);
        }
        // NAH! if (atomPickingMode == PICKING_DRAG_MINIMIZE_MOLECULE && (dragGesture.getPointCount() % 5 == 0))
        //  minimize(false);
        return;
      }
    }

    if (dragAtomIndex >= 0 && mode == Event.DRAGGED && bnd(clickAction, ACTION_assignNew)
        && apm == PICKING_ASSIGN_ATOM) {
      int nearestAtomIndex = vwr.findNearestAtomIndexMovable(x, y, false);
      if (nearestAtomIndex >= 0) {
        if (mp != null) {
          mp.setCount(1);
        } else if (measuresEnabled) {
          enterMeasurementMode(nearestAtomIndex);
        }
        addToMeasurement(nearestAtomIndex, null, true);
        mp.colix = C.MAGENTA;
      } else if (mp != null) {
        mp.setCount(1);
        mp.colix = C.GOLD;
      }
      if (mp == null)
        return;
      mp.traceX = x;
      mp.traceY = y;
      vwr.refresh(3, "assignNew");
      return;
    }

    if (!drawMode && !labelMode && bnd(dragWheelAction, ACTION_translate)) {
      vwr.translateXYBy(deltaX, deltaY);
      return;
    }
    if (dragSelectedMode
        && haveSelection
        && (bnd(dragWheelAction, ACTION_dragSelected) || bnd(
            dragWheelAction, ACTION_rotateSelected))) {
      int iatom = vwr.bsA().nextSetBit(0);
      if (iatom < 0)
        return;
      if (dragGesture.getPointCount() == 1)
        vwr.undoMoveActionClear(iatom, AtomCollection.TAINT_COORD, true);
      else
        vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);
      setMotion(GenericPlatform.CURSOR_MOVE, true);
      if (bnd(dragWheelAction, ACTION_rotateSelected)
          && vwr.getBoolean(T.allowrotateselected))
        vwr.rotateSelected(getDegrees(deltaX, true), getDegrees(deltaY, false),
            null);
      else
        vwr.moveSelected(deltaX, deltaY, Integer.MIN_VALUE,
            Integer.MIN_VALUE, Integer.MIN_VALUE, null, true, false);
      return;
    }

    if (drawMode
        && (bnd(dragWheelAction, ACTION_dragDrawObject) || bnd(
            dragWheelAction, ACTION_dragDrawPoint)) || labelMode
        && bnd(dragWheelAction, ACTION_dragLabel)) {
      setMotion(GenericPlatform.CURSOR_MOVE, true);
      vwr.checkObjectDragged(dragged.x, dragged.y, x, y, dragWheelAction);
      return;
    }
    if (checkMotionRotateZoom(dragWheelAction, x, deltaX, deltaY, true)) {
      if (vwr.tm.slabEnabled && checkSlideZoom(dragWheelAction))
        vwr.slabDepthByPixels(deltaY);
      else
        vwr.zoomBy(deltaY);
      return;
    }
    if (bnd(dragWheelAction, ACTION_rotate)) {
      float degX = getDegrees(deltaX, true);
      float degY = getDegrees(deltaY, false);
      if (vwr.g.useArcBall)
        vwr.rotateArcBall(x, y, mouseDragFactor);
      else
        vwr.rotateXYBy(degX, degY);
      return;
    }
    if (bnd(dragWheelAction, ACTION_rotateZorZoom)) {
      if (deltaX == 0 && Math.abs(deltaY) > 1) {
        // if (deltaY < 0 && deltaX > deltaY || deltaY > 0 && deltaX < deltaY)
        setMotion(GenericPlatform.CURSOR_ZOOM, true);
        vwr.zoomBy(deltaY + (deltaY > 0 ? -1 : 1));
      } else if (deltaY == 0 && Math.abs(deltaX) > 1) {
        // if (deltaX < 0 && deltaY > deltaX || deltaX > 0 && deltaY < deltaX)
        setMotion(GenericPlatform.CURSOR_MOVE, true);
        vwr.rotateZBy(-deltaX + (deltaX > 0 ? 1 : -1), Integer.MAX_VALUE,
            Integer.MAX_VALUE);
      }
      return;
    } else if (bnd(dragWheelAction, ACTION_wheelZoom)) {
      zoomByFactor(deltaY, Integer.MAX_VALUE, Integer.MAX_VALUE);
      return;
    } else if (bnd(dragWheelAction, ACTION_rotateZ)) {
      setMotion(GenericPlatform.CURSOR_MOVE, true);
      vwr.rotateZBy(-deltaX, Integer.MAX_VALUE, Integer.MAX_VALUE);
      return;
    }
    if (vwr.tm.slabEnabled) {
      if (bnd(dragWheelAction, ACTION_depth)) {
        vwr.depthByPixels(deltaY);
        return;
      }
      if (bnd(dragWheelAction, ACTION_slab)) {
        vwr.slabByPixels(deltaY);
        return;
      }
      if (bnd(dragWheelAction, ACTION_slabAndDepth)) {
        vwr.slabDepthByPixels(deltaY);
        return;
      }
    }
  }

  private void checkReleaseAction(int x, int y, long time, boolean dragRelease) {
    if (Logger.debugging)
      Logger.debug(Binding.getMouseActionName(pressAction, false));
    vwr.checkInMotion(0);
    vwr.setInMotion(false);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    dragGesture.add(dragAction, x, y, time);
    if (dragRelease)
      vwr.setRotateBondIndex(Integer.MIN_VALUE);
    if (dragAtomIndex >= 0) {
      if (apm == PICKING_DRAG_MINIMIZE
          || apm == PICKING_DRAG_MINIMIZE_MOLECULE)
        minimize(true);
    }
    if (apm == PICKING_ASSIGN_ATOM
        && bnd(clickAction, ACTION_assignNew)) {
      if (mp == null || dragAtomIndex < 0)
        return;
      assignNew(x, y);
      return;
    }
    dragAtomIndex = -1;
    boolean isRbAction = isRubberBandSelect(dragAction);
    if (isRbAction)
      selectRb(clickAction);
    rubberbandSelectionMode = (b.name.equals("drag"));
    rectRubber.x = Integer.MAX_VALUE;
    if (dragRelease) {
      vwr.notifyMouseClicked(x, y, Binding.getMouseAction(pressedCount, 0,
          Event.RELEASED), Event.RELEASED);
    }
    if (drawMode
        && (bnd(dragAction, ACTION_dragDrawObject) || bnd(dragAction,
            ACTION_dragDrawPoint)) || labelMode
        && bnd(dragAction, ACTION_dragLabel)) {
      vwr.checkObjectDragged(Integer.MAX_VALUE, 0, x, y, dragAction);
      return;
    }
    if (dragSelectedMode && bnd(dragAction, ACTION_dragSelected)
        && haveSelection)
      vwr.moveSelected(Integer.MAX_VALUE, 0, Integer.MIN_VALUE,
          Integer.MIN_VALUE, Integer.MIN_VALUE, null, false, false);

    if (dragRelease
        && checkUserAction(pressAction, x, y, 0, 0, time, Event.RELEASED))
      return;

    if (vwr.getBoolean(T.allowgestures)) {
      if (bnd(dragAction, ACTION_swipe)) {
        float speed = getExitRate();
        if (speed > 0)
          vwr.spinXYBy(dragGesture.getDX(4, 2), dragGesture.getDY(4, 2),
              speed * 30 * gestureSwipeFactor);
        if (vwr.g.logGestures)
          vwr.log("$NOW$ swipe " + dragGesture + " " + speed);
        return;
      }

    }
  }

  private void checkClickAction(int x, int y, long time, int clickedCount) {
    if (!vwr.haveModelSet())
      return;
    // points are always picked up first, then atoms
    // so that atom picking can be superceded by draw picking
    // Binding.MOVED is used for some vwr methods.
    if (clickedCount > 0) {
      if (checkUserAction(clickAction, x, y, 0, 0, time, Binding.CLICK))
        return;
      clickAction = vwr.notifyMouseClicked(x, y, clickAction, Binding.CLICK);
      if (clickAction == 0)
        return;
    }
    if (Logger.debugging)
      Logger.debug(Binding.getMouseActionName(clickAction, false));
    if (bnd(clickAction, ACTION_clickFrank)) {
      if (vwr.frankClicked(x, y)) {
        vwr.popupMenu(-x, y, 'j');
        return;
      }
      if (vwr.frankClickedModelKit(x, y)) {
        vwr.popupMenu(0, 0, 'm');
        return;
      }
    }
    Point3fi nearestPoint = null;
    boolean isBond = false;
    boolean isIsosurface = false;
    Map<String, Object> t = null;
    // t.tok will let us know if this is an atom or a bond that was clicked
    if (!drawMode) {
      t = vwr.checkObjectClicked(x, y, clickAction);
      if (t != null) {
        isBond = "bond".equals(t.get("type"));
        isIsosurface = "isosurface".equals(t.get("type"));
        nearestPoint = getPoint(t);
      }
    }
    if (isBond)
      clickedCount = 1;

    if (nearestPoint != null && Float.isNaN(nearestPoint.x))
      return;
    int nearestAtomIndex = findNearestAtom(x, y, nearestPoint, clickedCount > 0);

    if (clickedCount == 0 && apm != PICKING_ASSIGN_ATOM) {
      // mouse move
      if (mp == null)
        return;
      if (nearestPoint != null
          || mp.getIndexOf(nearestAtomIndex) == 0)
        mp.addPoint(nearestAtomIndex, nearestPoint, false);
      if (mp.haveModified)
        vwr.setPendingMeasurement(mp);
      vwr.refresh(3, "measurementPending");
      return;
    }
    setMouseMode();

    if (bnd(clickAction, ACTION_stopMotion)) {
      vwr.tm.stopMotion();
      // continue checking --- no need to exit here
    }

    if (vwr.getBoolean(T.navigationmode)
        && apm == PICKING_NAVIGATE
        && bnd(clickAction, ACTION_pickNavigate)) {
      vwr.navTranslatePercent(x * 100f / vwr.getScreenWidth() - 50f, y
          * 100f / vwr.getScreenHeight() - 50f);
      return;
    }

    // bond change by clicking on a bond
    // bond deletion by clicking a bond
    if (isBond) {
      if (bnd(clickAction, bondPickingMode == PICKING_ROTATE_BOND
          || bondPickingMode == PICKING_ASSIGN_BOND ? ACTION_assignNew
          : ACTION_deleteBond)) {
        bondPicked(((Integer) t.get("index")).intValue());
        return;
      }
    } else if (isIsosurface) {
      return;
    } else {
      if (apm != PICKING_ASSIGN_ATOM && mp != null
          && bnd(clickAction, ACTION_pickMeasure)) {
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
        if (addToMeasurement(nearestAtomIndex, nearestPoint, false) == 4)
          toggleMeasurement();
        return;
      }

      if (bnd(clickAction, ACTION_setMeasure)) {
        if (mp != null) {
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
          toggleMeasurement();
        } else if (!drawMode && !labelMode && !dragSelectedMode
            && measuresEnabled) {
          enterMeasurementMode(nearestAtomIndex);
          addToMeasurement(nearestAtomIndex, nearestPoint, true);
        }
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
        return;
      }
    }
    if (isSelectAction(clickAction)) {
      // TODO: in drawMode the binding changes
      if (!isIsosurface)
        atomOrPointPicked(nearestAtomIndex, nearestPoint);
      return;
    }
    if (bnd(clickAction, ACTION_reset)) {
      if (nearestAtomIndex < 0)
        reset();
      return;
    }
  }

  private boolean checkUserAction(int mouseAction, int x, int y, int deltaX,
                                  int deltaY, long time, int mode) {
    if (!b.isUserAction(mouseAction))
      return false;
    boolean passThrough = false;
    Object obj;
    Map<String, Object> ht = b.getBindings();
    String mkey = mouseAction + "\t";
    for (String key : ht.keySet()) {
      if (key.indexOf(mkey) != 0 || !PT.isAS(obj = ht.get(key)))
        continue;
      String script = ((String[]) obj)[1];
      P3 nearestPoint = null;
      if (script.indexOf("_ATOM") >= 0) {
        int iatom = findNearestAtom(x, y, null, true);
        script = PT.rep(script, "_ATOM", "({"
            + (iatom >= 0 ? "" + iatom : "") + "})");
        if (iatom >= 0)
          script = PT.rep(script, "_POINT", Escape.eP(vwr
              .ms.at[iatom]));
      }
      if (!drawMode
          && (script.indexOf("_POINT") >= 0 || script.indexOf("_OBJECT") >= 0 || script
              .indexOf("_BOND") >= 0)) {
        Map<String, Object> t = vwr.checkObjectClicked(x, y, mouseAction);
        if (t != null && (nearestPoint = (P3) t.get("pt")) != null) {
          boolean isBond = t.get("type").equals("bond");
          if (isBond)
            script = PT.rep(script, "_BOND", "[{"
                + t.get("index") + "}]");
          script = PT.rep(script, "_POINT", Escape
              .eP(nearestPoint));
          script = PT.rep(script, "_OBJECT", Escape
              .escapeMap(t));
        }
        script = PT.rep(script, "_BOND", "[{}]");
        script = PT.rep(script, "_OBJECT", "{}");
      }
      script = PT.rep(script, "_POINT", "{}");
      script = PT.rep(script, "_ACTION", "" + mouseAction);
      script = PT.rep(script, "_X", "" + x);
      script = PT.rep(script, "_Y", ""
          + (vwr.getScreenHeight() - y));
      script = PT.rep(script, "_DELTAX", "" + deltaX);
      script = PT.rep(script, "_DELTAY", "" + deltaY);
      script = PT.rep(script, "_TIME", "" + time);
      script = PT.rep(script, "_MODE", "" + mode);
      if (script.startsWith("+:")) {
        passThrough = true;
        script = script.substring(2);
      }
      vwr.evalStringQuiet(script);
    }
    return !passThrough;
  }

  /**
   * 
   * @param mouseAction
   * @param x
   * @param deltaX
   * @param deltaY
   * @param isDrag
   * @return TRUE if motion was a zoom
   */
  private boolean checkMotionRotateZoom(int mouseAction, int x, int deltaX,
                                        int deltaY, boolean isDrag) {
    boolean isSlideZoom = checkSlideZoom(mouseAction);
    boolean isRotateXY = bnd(mouseAction, ACTION_rotate);
    boolean isRotateZorZoom = bnd(mouseAction, ACTION_rotateZorZoom);
    if (!isSlideZoom && !isRotateXY && !isRotateZorZoom)
      return false;
    boolean isZoom = (isRotateZorZoom && (deltaX == 0 || Math.abs(deltaY) > 5 * Math
        .abs(deltaX)));
    int cursor = (isZoom || isZoomArea(moved.x)
        || bnd(mouseAction, ACTION_wheelZoom) ? GenericPlatform.CURSOR_ZOOM : isRotateXY
        || isRotateZorZoom ? GenericPlatform.CURSOR_MOVE : bnd(mouseAction,
        ACTION_center) ? GenericPlatform.CURSOR_HAND : GenericPlatform.CURSOR_DEFAULT);
    setMotion(cursor, isDrag);
    return (isZoom || isSlideZoom);
  }

  protected float getExitRate() {
    long dt = dragGesture.getTimeDifference(2);
    return (dt > MININUM_GESTURE_DELAY_MILLISECONDS ? 0 : dragGesture
        .getSpeedPixelsPerMillisecond(4, 2));
  }

  private boolean isRubberBandSelect(int action) {
    // drag and wheel and release
    action = action & ~Binding.DRAG | Binding.CLICK;
    return rubberbandSelectionMode
        && (bnd(action, ACTION_selectToggle)
            || bnd(action, ACTION_selectOr) || bnd(action,
            ACTION_selectAndNot));
  }

  Rectangle getRubberBand() {
    return (rubberbandSelectionMode && rectRubber.x != Integer.MAX_VALUE ? rectRubber
        : null);
  }

  private void calcRectRubberBand() {
    int factor = (vwr.antialiased ? 2 : 1);
    if (current.x < pressed.x) {
      rectRubber.x = current.x * factor;
      rectRubber.width = (pressed.x - current.x) * factor;
    } else {
      rectRubber.x = pressed.x * factor;
      rectRubber.width = (current.x - pressed.x) * factor;
    }
    if (current.y < pressed.y) {
      rectRubber.y = current.y * factor;
      rectRubber.height = (pressed.y - current.y) * factor;
    } else {
      rectRubber.y = pressed.y * factor;
      rectRubber.height = (current.y - pressed.y) * factor;
    }
  }

  /**
   * Transform a screen pixel change to an angular change
   * such that a full sweep of the dimension (up to 500 pixels)
   * corresponds to 180 degrees of rotation.
   *  
   * @param delta
   * @param isX
   * @return desired scaled rotation, in degrees
   */
  protected float getDegrees(float delta, boolean isX) {
    return delta / Math.min(500, isX ? vwr.getScreenWidth() 
        : vwr.getScreenHeight()) * 180 * mouseDragFactor;
  }

  private boolean checkSlideZoom(int action) {
    return bnd(action, ACTION_slideZoom) && isZoomArea(pressed.x);
  }

  private boolean isZoomArea(int x) {
    return x > vwr.getScreenWidth() * (vwr.isStereoDouble() ? 2 : 1)
        * SLIDE_ZOOM_X_PERCENT / 100f;
  }

  private Point3fi getPoint(Map<String, Object> t) {
    Point3fi pt = new Point3fi();
    pt.setT((P3) t.get("pt"));
    pt.mi = (short) ((Integer) t.get("modelIndex")).intValue();
    return pt;
  }

  private int findNearestAtom(int x, int y, Point3fi nearestPoint,
                              boolean isClicked) {
    int index = (drawMode || nearestPoint != null ? -1 : vwr
        .findNearestAtomIndexMovable(x, y, false));
    return (index >= 0 && (isClicked || mp == null)
        && !vwr.slm.isInSelectionSubset(index) ? -1 : index);
  }

  private boolean isSelectAction(int action) {
    return (bnd(action, ACTION_pickAtom)
        || !drawMode
        && !labelMode
        && apm == PICKING_IDENTIFY
        && bnd(action, ACTION_center)
        || dragSelectedMode
        && (bnd(dragAction, ACTION_rotateSelected) || bnd(dragAction,
            ACTION_dragSelected)) || bnd(action, ACTION_pickPoint)
        || bnd(action, ACTION_selectToggle)
        || bnd(action, ACTION_selectAndNot)
        || bnd(action, ACTION_selectOr)
        || bnd(action, ACTION_selectToggleExtended) || bnd(action,
        ACTION_select));
  }

  //////////// specific actions ////////////////

  private MeasurementPending measurementQueued;

  private void enterMeasurementMode(int iAtom) {
    vwr.setPicked(-1);
    vwr.setPicked(iAtom);
    vwr.setCursor(GenericPlatform.CURSOR_CROSSHAIR);
    vwr.setPendingMeasurement(mp = getMP());
    measurementQueued = mp;
  }

  private MeasurementPending getMP() {
    return ((MeasurementPending) Interface
        .getInterface("org.jmol.modelset.MeasurementPending")).set(vwr.ms);
  }

  private int addToMeasurement(int atomIndex, Point3fi nearestPoint,
                               boolean dblClick) {
    if (atomIndex == -1 && nearestPoint == null || mp == null) {
      exitMeasurementMode(null);
      return 0;
    }
    int measurementCount = mp.count;
    if (mp.traceX != Integer.MIN_VALUE && measurementCount == 2)
      mp.setCount(measurementCount = 1);
    return (measurementCount == 4 && !dblClick ? measurementCount
        : mp.addPoint(atomIndex, nearestPoint, true));
  }

  private void resetMeasurement() {
    // doesn't reset the measurement that is being picked using
    // double-click, just the one using set picking measure.
    exitMeasurementMode(null);
    measurementQueued = getMP();
  }

  private void exitMeasurementMode(String refreshWhy) {
    if (mp == null)
      return;
    vwr.setPendingMeasurement(mp = null);
    vwr.setCursor(GenericPlatform.CURSOR_DEFAULT);
    if (refreshWhy != null)
      vwr.refresh(3, refreshWhy);
  }

  private void getSequence() {
    int a1 = measurementQueued.getAtomIndex(1);
    int a2 = measurementQueued.getAtomIndex(2);
    if (a1 < 0 || a2 < 0)
      return;
    try {
      String sequence = vwr.getSmilesOpt(null, a1, a2, false, true, false, false, false);
      vwr.setStatusMeasuring("measureSequence", -2, sequence, 0);
    } catch (Exception e) {
      Logger.error(e.toString());
    }
  }

  private void minimize(boolean dragDone) {
    vwr.stopMinimization();
    int iAtom = dragAtomIndex;
    if (dragDone)
      dragAtomIndex = -1;
    BS bs = (vwr.getMotionFixedAtoms().cardinality() == 0 ? vwr
        .ms.getAtoms((vwr.isAtomPDB(iAtom) ? T.group : T.molecule), BSUtil
            .newAndSetBit(iAtom)) : BSUtil.setAll(vwr.getAtomCount()));
    vwr.minimize(Integer.MAX_VALUE, 0, bs, null, 0, false, false, false, false);
  }

  private int queueAtom(int atomIndex, Point3fi ptClicked) {
    int n = measurementQueued.addPoint(atomIndex, ptClicked, true);
    if (atomIndex >= 0)
      vwr.setStatusAtomPicked(atomIndex, "Atom #" + n + ":"
          + vwr.getAtomInfo(atomIndex), null);
    return n;
  }

  protected void setMotion(int cursor, boolean inMotion) {
    switch (vwr.currentCursor) {
    case GenericPlatform.CURSOR_WAIT:
      break;
    default:
      vwr.setCursor(cursor);
    }
    if (inMotion)
      vwr.setInMotion(true);
  }

  protected void zoomByFactor(int dz, int x, int y) {
    if (dz == 0)
      return;
    setMotion(GenericPlatform.CURSOR_ZOOM, true);
    vwr.zoomByFactor((float) Math.pow(mouseWheelFactor, dz), x, y);
    vwr.setInMotion(false);
  }

  
  /// methods that utilize vwr.script

  private void runScript(String script) {
    vwr.script(script);
  }

  private void atomOrPointPicked(int atomIndex, Point3fi ptClicked) {
    // atomIndex < 0 is off structure.
    // if picking spin or picking symmetry is on, then 
    // we need to enter this method to process those events.
    if (atomIndex < 0) {
      resetMeasurement(); // for set picking measure only
      if (bnd(clickAction, ACTION_selectNone)) {
        runScript("select none");
        return;
      }
      if (apm != PICKING_SPIN
          && apm != PICKING_SYMMETRY)
        return;
    }
    int n = 2;
    switch (apm) {
    case PICKING_DRAG_ATOM:
      // this is done in mouse drag, not mouse release
    case PICKING_DRAG_MINIMIZE:
      return;
    case PICKING_OFF:
      return;
    case PICKING_STRUTS:
    case PICKING_CONNECT:
    case PICKING_DELETE_BOND:
      boolean isDelete = (apm == PICKING_DELETE_BOND);
      boolean isStruts = (apm == PICKING_STRUTS);
      if (!bnd(clickAction, (isDelete ? ACTION_deleteBond
          : ACTION_connectAtoms)))
        return;
      if (measurementQueued == null || measurementQueued.count == 0
          || measurementQueued.count > 2) {
        resetMeasurement();
        enterMeasurementMode(atomIndex);
      }
      addToMeasurement(atomIndex, ptClicked, true);
      if (queueAtom(atomIndex, ptClicked) != 2)
        return;
      String cAction = (isDelete
          || measurementQueued.isConnected(vwr.ms.at, 2) ? " DELETE"
          : isStruts ? "STRUTS" : "");
      runScript("connect " + measurementQueued.getMeasurementScript(" ", true)
          + cAction);
      resetMeasurement();
      return;
    case PICKING_MEASURE_TORSION:
      n++;
      //$FALL-THROUGH$
    case PICKING_MEASURE_ANGLE:
      n++;
      //$FALL-THROUGH$
    case PICKING_MEASURE:
    case PICKING_MEASURE_DISTANCE:
    case PICKING_MEASURE_SEQUENCE:
      if (!bnd(clickAction, ACTION_pickMeasure))
        return;
      if (measurementQueued == null || measurementQueued.count == 0
          || measurementQueued.count > n) {
        resetMeasurement();
        enterMeasurementMode(atomIndex);
      }
      addToMeasurement(atomIndex, ptClicked, true);
      queueAtom(atomIndex, ptClicked);
      int i = measurementQueued.count;
      if (i == 1) {
        vwr.setPicked(-1);
        vwr.setPicked(atomIndex);
      }
      if (i < n)
        return;
      if (apm == PICKING_MEASURE_SEQUENCE) {
        getSequence();
      } else {
        vwr.setStatusMeasuring("measurePicked", n, measurementQueued
            .getStringDetail(), measurementQueued.value);
        if (apm == PICKING_MEASURE
            || pickingStyleMeasure == PICKINGSTYLE_MEASURE_ON) {
          runScript("measure "
              + measurementQueued.getMeasurementScript(" ", true));
        }
      }
      resetMeasurement();
      return;
    }
    int mode = (mp != null
        && apm != PICKING_IDENTIFY ? PICKING_IDENTIFY
        : apm);
    switch (mode) {
    case PICKING_CENTER:
      if (!bnd(clickAction, ACTION_pickAtom))
        return;
      if (ptClicked == null) {
        zoomTo(atomIndex);
      } else {
        runScript("zoomTo " + Escape.eP(ptClicked));
      }
      return;
    case PICKING_SPIN:
    case PICKING_SYMMETRY:
      if (bnd(clickAction, ACTION_pickAtom))
        checkTwoAtomAction(ptClicked, atomIndex);
    }
    if (ptClicked != null)
      return;
    // atoms only here:
    BS bs;
    switch (mode) {
    case PICKING_IDENTIFY:
      if (!drawMode && !labelMode && bnd(clickAction, ACTION_center))
        zoomTo(atomIndex);
      else if (bnd(clickAction, ACTION_pickAtom))
        vwr.setStatusAtomPicked(atomIndex, null, null);
      return;
    case PICKING_LABEL:
      if (bnd(clickAction, ACTION_pickLabel)) {
        runScript("set labeltoggle {atomindex=" + atomIndex + "}");
        vwr.setStatusAtomPicked(atomIndex, null, null);
      }
      return;
    case PICKING_INVERT_STEREO:
      if (bnd(clickAction, ACTION_assignNew)) {
        bs = vwr.getAtomBitSet("connected(atomIndex=" + atomIndex
            + ") and !within(SMARTS,'[r50,R]')");
        int nb = bs.cardinality();
        switch (nb) {
        case 0:
        case 1:
          // not enough non-ring atoms
          return;
        case 2:
          break;
        case 3:
        case 4:
          // three or four are not in a ring. So let's find the shortest two
          // branches and invert them.
          int[] lengths = new int[nb];
          int[] points = new int[nb];
          int ni = 0;
          for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1), ni++) {
            lengths[ni] = vwr.getBranchBitSet(i, atomIndex, true)
                .cardinality();
            points[ni] = i;
          }
          for (int j = 0; j < nb - 2; j++) {
            int max = Integer.MIN_VALUE;
            int imax = 0;
            for (int i = 0; i < nb; i++)
              if (lengths[i] >= max && bs.get(points[i])) {
                imax = points[i];
                max = lengths[i];
              }
            bs.clear(imax);
          }
        }
        vwr.undoMoveActionClear(atomIndex, AtomCollection.TAINT_COORD, true);
        vwr.invertSelected(null, null, atomIndex, bs);
        vwr.setStatusAtomPicked(atomIndex, "inverted: " + Escape.eBS(bs), null);
      }
      return;
    case PICKING_DELETE_ATOM:
      if (bnd(clickAction, ACTION_deleteAtom)) {
        bs = BSUtil.newAndSetBit(atomIndex);
        vwr.deleteAtoms(bs, false);
        vwr.setStatusAtomPicked(atomIndex, "deleted: " + Escape.eBS(bs), null);
      }
      return;
    }
    // set picking select options:
    String spec = "atomindex=" + atomIndex;
    switch (apm) {
    default:
      return;
    case PICKING_SELECT_ATOM:
      selectAtoms(spec);
      break;
    case PICKING_SELECT_GROUP:
      selectAtoms("within(group, " + spec + ")");
      break;
    case PICKING_SELECT_CHAIN:
      selectAtoms("within(chain, " + spec + ")");
      break;
    case PICKING_SELECT_POLYMER:
      selectAtoms("within(polymer, " + spec + ")");
      break;
    case PICKING_SELECT_STRUCTURE:
      selectAtoms("within(structure, " + spec + ")");
      break;
    case PICKING_SELECT_MOLECULE:
      selectAtoms("within(molecule, " + spec + ")");
      break;
    case PICKING_SELECT_MODEL:
      selectAtoms("within(model, " + spec + ")");
      break;
    // only the next two use VISIBLE (as per the documentation)
    case PICKING_SELECT_ELEMENT:
      selectAtoms("visible and within(element, " + spec + ")");
      break;
    case PICKING_SELECT_SITE:
      selectAtoms("visible and within(site, " + spec + ")");
      break;
    }
    vwr.clearClickCount();
    vwr.setStatusAtomPicked(atomIndex, null, null);
  }

  private void assignNew(int x, int y) {
    // H C + -, etc.
    // also check valence and add/remove H atoms as necessary?
    if (mp.count == 2) {
      vwr.undoMoveActionClear(-1, T.save, true);
      runScript("assign connect "
          + mp.getMeasurementScript(" ", false));
    } else if (pickAtomAssignType.equals("Xx")) {
      exitMeasurementMode("bond dropped");
    } else {
      if (pressed.inRange(xyRange, dragged.x, dragged.y)) {
        String s = "assign atom ({" + dragAtomIndex + "}) \""
            + pickAtomAssignType + "\"";
        if (isPickAtomAssignCharge) {
          s += ";{atomindex=" + dragAtomIndex + "}.label='%C'; ";
          vwr.undoMoveActionClear(dragAtomIndex,
              AtomCollection.TAINT_FORMALCHARGE, true);
        } else {
          vwr.undoMoveActionClear(-1, T.save, true);
        }
        runScript(s);
      } else if (!isPickAtomAssignCharge) {
        vwr.undoMoveActionClear(-1, T.save, true);
        Atom a = vwr.ms.at[dragAtomIndex];
        if (a.getElementNumber() == 1) {
          runScript("assign atom ({" + dragAtomIndex + "}) \"X\"");
        } else {
          P3 ptNew = P3.new3(x, y, a.sZ);
          vwr.tm.unTransformPoint(ptNew, ptNew);
          runScript("assign atom ({" + dragAtomIndex + "}) \""
              + pickAtomAssignType + "\" " + Escape.eP(ptNew));
        }
      }
    }
    exitMeasurementMode(null);
  }

  private void bondPicked(int index) {    
    if (bondPickingMode == PICKING_ASSIGN_BOND)
      vwr.undoMoveActionClear(-1, T.save, true);
    
    switch (bondPickingMode) {
    case PICKING_ASSIGN_BOND:
      runScript("assign bond [{" + index + "}] \"" + pickBondAssignType
          + "\"");
      break;
    case PICKING_ROTATE_BOND:
      vwr.setRotateBondIndex(index);
      break;
    case PICKING_DELETE_BOND:
      vwr.deleteBonds(BSUtil.newAndSetBit(index));
    }
  }

  private void checkTwoAtomAction(Point3fi ptClicked, int atomIndex) {
    boolean isSpin = (apm == PICKING_SPIN);
    if (vwr.tm.spinOn || vwr.tm.navOn
        || vwr.getPendingMeasurement() != null) {
      resetMeasurement();
      if (vwr.tm.spinOn)
        runScript("spin off");
      return;
    }
    if (measurementQueued.count >= 2)
      resetMeasurement();
    int queuedAtomCount = measurementQueued.count;
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
        vwr.scriptStatus(queuedAtomCount == 1 ? GT
            ._("pick one more atom in order to spin the model around an axis")
            : GT._("pick two atoms in order to spin the model around an axis"));
      else
        vwr
            .scriptStatus(queuedAtomCount == 1 ? GT
                ._("pick one more atom in order to display the symmetry relationship")
                : GT
                    ._("pick two atoms in order to display the symmetry relationship between them"));
      return;
    }
    String s = measurementQueued.getMeasurementScript(" ", false);
    if (isSpin)
      runScript("spin" + s + " " + vwr.getInt(T.pickingspinrate));
    else
      runScript("draw symop" + s + ";show symop" + s);
  }

  private void reset() {
    runScript("!reset");
  }

  private boolean selectionWorking = false;

  private void selectAtoms(String item) {
    if (mp != null || selectionWorking)
      return;
    selectionWorking = true;
    String s = (rubberbandSelectionMode
        || bnd(clickAction, ACTION_selectToggle) ? "selected and not ("
        + item + ") or (not selected) and " : bnd(clickAction,
        ACTION_selectAndNot) ? "selected and not " : bnd(clickAction,
        ACTION_selectOr) ? "selected or " : clickAction == 0
        || bnd(clickAction, ACTION_selectToggleExtended) ? "selected tog "
        : bnd(clickAction, ACTION_select) ? "" : null);
    if (s != null) {
      s += "(" + item + ")";
      try {
        BS bs = vwr.getAtomBitSetEval(null, s);
        vwr.select(bs, false, 0, false);
        vwr.refresh(3, "selections set");
      } catch (Exception e) {
        // ignore
      }
    }
    selectionWorking = false;
  }

  private void selectRb(int action) {
    BS bs = vwr.ms.findAtomsInRectangle(rectRubber);
    if (bs.length() > 0) {
      String s = Escape.eBS(bs);
      if (bnd(action, ACTION_selectOr))
        runScript("selectionHalos on;select selected or " + s);
      else if (bnd(action, ACTION_selectAndNot))
        runScript("selectionHalos on;select selected and not " + s);
      else
        // ACTION_selectToggle
        runScript("selectionHalos on;select selected tog " + s);
    }
    vwr.refresh(3, "mouseReleased");
  }

  private void toggleMeasurement() {
    if (mp == null)
      return;
    int measurementCount = mp.count;
    if (measurementCount >= 2 && measurementCount <= 4)
      runScript("!measure "
          + mp.getMeasurementScript(" ", true));
    exitMeasurementMode(null);
  }

  private void zoomTo(int atomIndex) {
    runScript("zoomTo (atomindex=" + atomIndex + ")");
    vwr.setStatusAtomPicked(atomIndex, null, null);
  }

  @Override
  public boolean keyTyped(int keyChar, int modifiers) {
    return false;
  }

}
