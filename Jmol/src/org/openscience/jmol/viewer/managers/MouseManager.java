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
import java.awt.Rectangle;
import java.awt.Event;
import java.util.BitSet;

public abstract class MouseManager {

  Component component;
  JmolViewer viewer;

  int xClickPressed, yClickPressed;
  int xPrevious, yPrevious;
  int xCurrent, yCurrent;
  int modifiersWhenPressed;

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

  void mousePressed(int x, int y, int modifiers, int clickCount) {
    if (logMouseEvents)
      System.out.println("mousePressed("+x+","+y+","+modifiers+")");
    if (! viewer.haveFrame())
      return;
    xClickPressed = xCurrent = xPrevious = x;
    yClickPressed = yCurrent = yPrevious = y;
    modifiersWhenPressed = modifiers;
    if ((modifiers & MIDDLE) != 0) {
      viewer.homePosition();
      return;
    }
    if ((modifiers & RIGHT) != 0)
      return;
    // left button was pressed

    if (viewer.frankClicked(x, y)) {
      viewer.popupMenu(x, y);
      return;
    }

    int atomIndex = viewer.findNearestAtomIndex(x, y);
    switch (modeMouse) {
    case JmolConstants.MOUSE_PICK:
      rubberbandSelectionMode = true;
      xAnchor = x;
      yAnchor = y;
      calcRectRubberBand();
      if ((modifiers & SHIFT) == 0) {
        viewer.clearSelection();
        if (atomIndex != -1)
          viewer.addSelection(atomIndex);
      } else {
        if (atomIndex != -1) 
          viewer.toggleSelection(atomIndex);
      }
      break;
    case JmolConstants.MOUSE_DELETE:
      if (atomIndex != -1)
        viewer.deleteAtom(atomIndex);
      break;
    case JmolConstants.MOUSE_MEASURE:
      if (atomIndex != -1) {
        viewer.measureSelection(atomIndex);
      }
    }
  }

  final static int LEFT = 16;
  final static int MIDDLE = Event.ALT_MASK;  // 8
  final static int ALT = Event.ALT_MASK;     // 8
  final static int RIGHT = Event.META_MASK;  // 4
  final static int CTRL = Event.CTRL_MASK;   // 2
  final static int SHIFT = Event.SHIFT_MASK; // 1
  final static int MIDDLE_RIGHT = MIDDLE | RIGHT;
  final static int CTRL_SHIFT = CTRL | SHIFT;
  final static int CTRL_LEFT = CTRL | LEFT;
  final static int SHIFT_LEFT = SHIFT | LEFT;
  final static int CTRL_SHIFT_LEFT = CTRL | SHIFT | LEFT;
  final static int CTRL_RIGHT = CTRL | RIGHT;
  final static int SHIFT_RIGHT = SHIFT | RIGHT;
  final static int CTRL_SHIFT_RIGHT = CTRL | SHIFT | RIGHT;
  final static int CTRL_ALT_SHIFT_RIGHT = CTRL | ALT | SHIFT | RIGHT;

  void mouseClicked(int x, int y, int modifiers, int clickCount) {
    // this event is not reliable on older platforms
    //    if (logMouseEvents)
    //      System.out.println("mouseClicked("+x+","+y+","+modifiers+")");
  }

  void mouseEntered(int x, int y, int modifiers, int clickCount) {
  }

  void mouseExited(int x, int y, int modifiers, int clickCount) {
  }

  void mouseReleased(int x, int y, int modifiers, int clickCount) {
    if (logMouseEvents)
      System.out.println("mouseReleased("+x+","+y+","+modifiers+")");
    viewer.setInMotion(false);
    if ((modifiers & CTRL_ALT_SHIFT_RIGHT) == RIGHT &&
        (modifiersWhenPressed & CTRL_ALT_SHIFT_RIGHT) == RIGHT) {
      // mth 2003 05 27
      // the reason I am checking for RIGHT is because e.isPopupTrigger()
      // was failing on some platforms
      // mth 2003 07 07
      // added this modifiersWhenPressed check because of bad
      // behavior on WinME
      viewer.popupMenu(x, y);
    } else if (modeMouse == JmolConstants.MOUSE_PICK) {
      rubberbandSelectionMode = false;
      component.repaint();
    } else if (x == xClickPressed && y == yClickPressed) {
      int atomIndex = viewer.findNearestAtomIndex(x, y);
      if (atomIndex != -1)
        viewer.notifyPicked(atomIndex);
    }
  }

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
    */
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

  public void mouseDragged(int x, int y, int modifiers, int clickCount) {

    viewer.setInMotion(true);
    xClickPressed = -1; // to invalidate a 'click'
    xCurrent = x;
    yCurrent = y;
    switch (getMode(modifiers)) {
    case JmolConstants.MOUSE_ROTATE:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Rotate("+x+","+y+","+modifiers+","+clickCount+")");
      viewer.rotateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_ROTATE_Z:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged RotateZ("+x+","+y+","+modifiers+","+clickCount+")");
      viewer.rotateZBy(xPrevious - xCurrent);
      break;
    case JmolConstants.MOUSE_XLATE:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Translate("+x+","+y+","+modifiers+","+clickCount+")");
      viewer.translateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
      break;
    case JmolConstants.MOUSE_ZOOM:
		//if (logMouseEvents)
		  //System.out.println("mouseDragged Zoom("+x+","+y+","+modifiers+","+clickCount+")");
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

  void mouseMoved(int x, int y, int modifiers, int clickCount) {
  }

  public abstract boolean handleEvent(Event e);
}
