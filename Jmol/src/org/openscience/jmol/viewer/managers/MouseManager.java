/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 */
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.*;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Event;
import java.util.BitSet;

public abstract class MouseManager {

  Component component;
  JmolViewer viewer;

  int previousDragX, previousDragY;
  public int xCurrent, yCurrent;

  int modifiersWhenPressed;
  boolean wasDragged;

  boolean measurementMode = false;
  boolean hoverActive = false;

  boolean rubberbandSelectionMode = false;
  int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  private static final boolean logMouseEvents = false;

  public MouseManager(Component component, JmolViewer viewer) {
    this.component = component;
    this.viewer = viewer;
  }

  public static final String[] modeNames = {
    "ROTATE", "ZOOM", "XLATE", "PICK", "DELETE", "MEASURE", "DEFORM",
    "ROTATE_Z", "SLAB_PLANE"};

  public int modeMouse = JmolConstants.MOUSE_ROTATE;
  public void setMode(int mode) {
    if (logMouseEvents)
      System.out.println("MouseManager.setMode(" + modeNames[mode] + ")");
    modeMouse = mode;
  }

  public int getMode() {
    return modeMouse;
  }

  public Rectangle getRubberBand() {
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
  final static int CTRL_SHIFT = CTRL | SHIFT;
  final static int CTRL_LEFT = CTRL | LEFT;
  final static int CTRL_RIGHT = CTRL | RIGHT;
  final static int CTRL_MIDDLE = CTRL | MIDDLE;
  final static int CTRL_ALT_LEFT = CTRL | ALT | LEFT;
  final static int ALT_LEFT = ALT | LEFT;
  final static int SHIFT_LEFT = SHIFT | LEFT;
  final static int CTRL_SHIFT_LEFT = CTRL | SHIFT | LEFT;
  final static int SHIFT_MIDDLE = SHIFT | MIDDLE;
  final static int SHIFT_RIGHT = SHIFT | RIGHT;
  final static int CTRL_SHIFT_RIGHT = CTRL | SHIFT | RIGHT;
  final static int CTRL_ALT_SHIFT_RIGHT = CTRL | ALT | SHIFT | RIGHT;
  final static int BUTTON_MODIFIER_MASK =
    CTRL | ALT | SHIFT | LEFT | MIDDLE | RIGHT;

  int previousPressedX, previousPressedY;
  int previousPressedModifiers, previousPressedCount;
  long previousPressedTime;
  int pressedCount;

  void mousePressed(int x, int y, int modifiers, boolean isPopupTrigger,
                    long pressedTime) {
    if (previousPressedX == x && previousPressedY == y &&
        previousPressedModifiers == modifiers && 
        (pressedTime - previousPressedTime) < MAX_DOUBLE_CLICK_MILLIS) {
      ++pressedCount;
    } else {
      pressedCount = 1;
    }

    previousPressedX = previousDragX = xCurrent = x;
    previousPressedY = previousDragY = yCurrent = y;
    previousPressedModifiers = modifiers;
    previousPressedTime = pressedTime;

    if (logMouseEvents)
      System.out.println("mousePressed("+x+","+y+","+modifiers+
                         " isPopupTrigger=" + isPopupTrigger+")");

    modifiersWhenPressed = modifiers;
    wasDragged = false;

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

  void mouseEntered(int x, int y) {
    xCurrent = x; yCurrent = y;
  }

  void mouseExited(int x, int y) {
    xCurrent = x; yCurrent = y;
    exitMeasurementMode();
  }

  void mouseReleased(int x, int y, int modifiers) {
    xCurrent = x; yCurrent = y;
    if (logMouseEvents)
      System.out.println("mouseReleased("+x+","+y+","+modifiers+")");
    viewer.setInMotion(false);
  }

  int previousClickX, previousClickY;
  int previousClickModifiers, previousClickCount;
  long previousClickTime;

  void mouseClicked(int x, int y, int modifiers, int clickCount,
                    long clickTime) {
    // clickCount is not reliable on some platforms
    // so we will just deal with it ourselves
    if (previousClickX == x && previousClickY == y &&
        previousClickModifiers == modifiers && clickCount == 1 &&
        (clickTime - previousClickTime) < MAX_DOUBLE_CLICK_MILLIS) {
      clickCount = previousClickCount + 1;
    }
    xCurrent = previousClickX = x; yCurrent = previousClickY = y;
    previousClickModifiers = modifiers;
    previousClickCount = clickCount;
    previousClickTime = clickTime;

    if (logMouseEvents)
      System.out.println("mouseClicked("+x+","+y+","+modifiers+
                         ",clickCount="+clickCount+
                         ",time=" + (clickTime - previousClickTime) +
                         ")");
    if (! viewer.haveFrame())
      return;

    int nearestAtomIndex = viewer.findNearestAtomIndex(x, y);
    if (clickCount == 1)
      mouseSingleClick(x, y, modifiers, nearestAtomIndex);
    else if (clickCount == 2)
      mouseDoubleClick(x, y, modifiers, nearestAtomIndex);
  }

  void mouseSingleClick(int x, int y, int modifiers, int nearestAtomIndex) {
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      if (viewer.frankClicked(x, y)) {
        viewer.popupMenu(x, y);
        return;
      }
      if (measurementMode) {
        addToMeasurement(nearestAtomIndex, false);
      } else {
        viewer.notifyPicked(nearestAtomIndex);
      }
      break;
    case ALT_LEFT:
    case MIDDLE:
      viewer.zoomToPercent(100);
      break;
    }
  }

  void mouseDoubleClick(int x, int y, int modifiers, int nearestAtomIndex) {
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      if (measurementMode) {
        addToMeasurement(nearestAtomIndex, true);
        toggleMeasurement();
      } else {
        enterMeasurementMode();
        addToMeasurement(nearestAtomIndex, true);
      }
      break;
    case ALT_LEFT:
    case MIDDLE:
      viewer.homePosition();
      break;
    }
  }

  void mouseDragged(int x, int y, int modifiers) {
    int deltaX = x - previousDragX;
    int deltaY = y - previousDragY;
    xCurrent = previousDragX = x; yCurrent = previousDragY = y;
    wasDragged = true;
    viewer.setInMotion(true);
    if (pressedCount == 1)
      mouseSinglePressDrag(deltaX, deltaY, modifiers);
    else if (pressedCount == 2)
      mouseDoublePressDrag(deltaX, deltaY, modifiers);
  }

  void mouseSinglePressDrag(int deltaX, int deltaY, int modifiers) {
    switch (modifiers & BUTTON_MODIFIER_MASK) {
    case LEFT:
      viewer.rotateXYBy(deltaX, deltaY);
      break;
    case ALT_LEFT:
    case MIDDLE:
      viewer.zoomBy(deltaY);
      viewer.rotateZBy(-deltaX);
      break;
    case CTRL_ALT_LEFT:
    case CTRL_MIDDLE:
      viewer.translateXYBy(deltaX, deltaY);
      break;
    }
  }

  void mouseDoublePressDrag(int deltaX, int deltaY, int modifiers) {
  }


  /*
  int getMode(int modifiers) {
    if (modeMouse != JmolConstants.MOUSE_ROTATE)
      return modeMouse;
    /* RASMOL
    // mth - I think that right click should be reserved for a popup menu
    if ((modifiers & CTRL_LEFT) == CTRL_LEFT)
    return SLAB_PLANE;
    if ((modifiers & SHIFT_LEFT) == SHIFT_LEFT)
    return ZOOM;
    if ((modifiers & SHIFT_RIGHT) == SHIFT_RIGHT)
    return ROTATE_Z;
    if ((modifiers & RIGHT) == RIGHT)
    return XLATE;
    if ((modifiers & LEFT) == LEFT)
    return ROTATE;
    /
    if ((modifiers & SHIFT_RIGHT) == SHIFT_RIGHT)
      return JmolConstants.MOUSE_ROTATE_Z;
    if ((modifiers & CTRL_RIGHT) == CTRL_RIGHT)
      return JmolConstants.MOUSE_XLATE;
    if ((modifiers & RIGHT) == RIGHT)
      return JmolConstants.MOUSE_POPUP_MENU;
    if ((modifiers & SHIFT_LEFT) == SHIFT_LEFT)
      return JmolConstants.MOUSE_ZOOM;
    if ((modifiers & CTRL_LEFT) == CTRL_LEFT)
      return JmolConstants.MOUSE_SLAB_PLANE;
    if ((modifiers & LEFT) == LEFT)
      return JmolConstants.MOUSE_ROTATE;
    return modeMouse;
  }

  public void mouseDragged(int x, int y, int modifiers) {
    xCurrent = x; yCurrent = y;
    wasDragged = true;
    viewer.setInMotion(true);
    switch (getMode(modifiers)) {
    case JmolConstants.MOUSE_MEASURE:
    case JmolConstants.MOUSE_ROTATE:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Rotate("+x+","+y+","+modifiers+")");
      viewer.rotateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_ROTATE_Z:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged RotateZ("+x+","+y+","+modifiers+")");
      viewer.rotateZBy(xPrevious - xCurrent);
      break;
    case JmolConstants.MOUSE_XLATE:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Translate("+x+","+y+","+modifiers+")");
      viewer.translateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_ZOOM:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Zoom("+x+","+y+","+modifiers+")");
      viewer.zoomBy(yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_SLAB_PLANE:
      viewer.slabBy(yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_PICK:
      if (viewer.haveFrame()) {
        calcRectRubberBand();
        BitSet selectedAtoms = viewer.findAtomsInRectangle(rectRubber);
        if ((modifiers & SHIFT) != 0) {
          viewer.addSelection(selectedAtoms);
        } else {
          viewer.setSelectionSet(selectedAtoms);
        }
      }
      break;
    case JmolConstants.MOUSE_POPUP_MENU:
      break;
    }
    xPrevious = xCurrent;
    yPrevious = yCurrent;
  }

*/

  void mouseMoved(int x, int y, int modifiers) {
    xCurrent = x; yCurrent = y;
    if (measurementMode | hoverActive) {
      int atomIndex = viewer.findNearestAtomIndex(x, y);
      if (measurementMode)
        setAttractiveMeasurementTarget(atomIndex);
    }
  }

  public abstract boolean handleEvent(Event e);

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
      viewer.refresh();
      return;
    }
    for (int i = measurementCount; i > 0; --i)
      if (measurementCountPlusIndices[i] == atomIndex) {
        viewer.refresh();
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
    if (measurementCount == 3 & !dblClick)
      return;
    measurementCountPlusIndices[++measurementCount] = atomIndex;
    measurementCountPlusIndices[0] = measurementCount;
    if (measurementCount == 4)
      toggleMeasurement();
    else
      viewer.setPendingMeasurement(measurementCountPlusIndices);
  }

  void exitMeasurementMode() {
    if (measurementMode) {
      viewer.getAwtComponent().setCursor(null);
      measurementCount = 0;
      viewer.setPendingMeasurement(null);
      measurementMode = false;
    }
  }

  void enterMeasurementMode() {
    viewer.getAwtComponent()
      .setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    measurementCount = 0;
    measurementMode = true;
  }

  void toggleMeasurement() {
    if (measurementCount >= 2 && measurementCount <= 4) {
      measurementCountPlusIndices[0] = measurementCount;
      viewer.toggleMeasurement(measurementCountPlusIndices);
    }
    exitMeasurementMode();
  }
}
