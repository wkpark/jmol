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

import org.openscience.jmol.Atom;
import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.util.BitSet;
/*
    REMOVE COMMENT TO ENABLE WHEELMOUSE
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
*/

public class MouseManager {

  private Component component;
  private JmolViewer viewer;

  private int xPrevious, yPrevious;
  private int xCurrent, yCurrent;
  private int modifiersWhenPressed;

  private boolean rubberbandSelectionMode = false;
  private int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  public MouseManager(Component component, JmolViewer viewer) {
    this.component = component;
    this.viewer = viewer;
    component.addMouseListener(new MyMouseListener());
    component.addMouseMotionListener(new MyMouseMotionListener());
    /*
    REMOVE COMMENT TO ENABLE WHEELMOUSE
    if (viewer.jvm14orGreater)
      component.addMouseWheelListener(new MyMouseWheelListener());
    */
  }

  public static final int ROTATE =     JmolViewer.ROTATE;
  public static final int ZOOM =       JmolViewer.ZOOM;
  public static final int XLATE =      JmolViewer.XLATE;
  public static final int PICK =       JmolViewer.PICK;
  public static final int DELETE =     JmolViewer.DELETE;
  public static final int MEASURE =    JmolViewer.MEASURE;
  public static final int DEFORM =     JmolViewer.DEFORM;
  public static final int ROTATE_Z =   JmolViewer.ROTATE_Z;
  public static final int SLAB_PLANE = JmolViewer.SLAB_PLANE;
  public static final int POPUP_MENU = JmolViewer.POPUP_MENU;

  public static final String[] modeNames = {
    "ROTATE", "ZOOM", "XLATE", "PICK", "DELETE", "MEASURE", "DEFORM",
    "ROTATE_Z", "SLAB_PLANE"};

  public int modeMouse = ROTATE;
  public void setMode(int mode) {
    //    System.out.println("MouseManager.setMode(" + modeNames[mode] + ")");
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

  final static int LEFT = InputEvent.BUTTON1_MASK;
  final static int MIDDLE = InputEvent.BUTTON2_MASK;
  final static int RIGHT = InputEvent.BUTTON3_MASK;
  final static int SHIFT = InputEvent.SHIFT_MASK;
  final static int CTRL = InputEvent.CTRL_MASK;
  final static int CTRL_SHIFT = CTRL | SHIFT;
  final static int CTRL_LEFT = CTRL | LEFT;
  final static int SHIFT_LEFT = SHIFT | LEFT;
  final static int CTRL_SHIFT_LEFT = CTRL | SHIFT | LEFT;
  final static int CTRL_RIGHT = CTRL | RIGHT;
  final static int SHIFT_RIGHT = SHIFT | RIGHT;
  final static int CTRL_SHIFT_RIGHT = CTRL | SHIFT | RIGHT;

  class MyMouseListener extends MouseAdapter {
    public void mousePressed(MouseEvent e) {
      xCurrent = xPrevious = e.getX();
      yCurrent = yPrevious = e.getY();
      modifiersWhenPressed = e.getModifiers();
      if (modeMouse == PICK) {
        rubberbandSelectionMode = true;
        xAnchor = xCurrent;
        yAnchor = yCurrent;
        calcRectRubberBand();
      }
    }


    public void mouseClicked(MouseEvent e) {
      int modifiers = e.getModifiers();
      if (viewer.haveFile()) {
        if ((e.getModifiers() & MIDDLE) == MIDDLE) {
          viewer.homePosition();
          return;
        }
        int atomIndex = viewer.findNearestAtomIndex(e.getX(), e.getY());
        switch (modeMouse) {
        case PICK:
          if (!e.isShiftDown()) {
            viewer.clearSelection();
            if (atomIndex != -1)
              viewer.addSelection(atomIndex);
          } else {
            if (atomIndex != -1) 
              viewer.toggleSelection(atomIndex);
          }
          break;
        case DELETE:
          if (atomIndex != -1)
            viewer.deleteAtom(atomIndex);
          break;
        case MEASURE:
          if (atomIndex != -1) {
            viewer.measureSelection(atomIndex);
          }
        }
      }
    }

    public void mouseReleased(MouseEvent e) {
      viewer.setInMotion(false);
      int modifiers = e.getModifiers();
      if ((modifiersWhenPressed & CTRL_SHIFT) == 0 &&
          (e.isPopupTrigger() || (modifiers & CTRL_SHIFT_RIGHT) == RIGHT)) {
        // mth 2003 05 27
        // the reason I am checking for RIGHT is because e.isPopupTrigger()
        // was failing on some platforms
        // mth 2003 07 07
        // added this modifiersWhenPressed check because of bad
        // behavior on WinME
        viewer.popupMenu(e.getComponent(), e.getX(), e.getY());
      } else if (modeMouse == PICK) {
        rubberbandSelectionMode = false;
        component.repaint();
      }
    }
  }

  class MyMouseMotionListener extends MouseMotionAdapter {

    int getMode(MouseEvent e) {
      int modifiers = e.getModifiers();
      if (modeMouse != ROTATE)
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
        return ROTATE_Z;
      if ((modifiers & CTRL_RIGHT) == CTRL_RIGHT)
        return XLATE;
      if ((modifiers & RIGHT) == RIGHT)
        return POPUP_MENU;
      if ((modifiers & SHIFT_LEFT) == SHIFT_LEFT)
        return ZOOM;
      if ((modifiers & CTRL_LEFT) == CTRL_LEFT)
        return SLAB_PLANE;
      if ((modifiers & LEFT) == LEFT)
        return ROTATE;
      return modeMouse;
    }

    public void mouseDragged(MouseEvent e) {

      viewer.setInMotion(true);
      xCurrent = e.getX();
      yCurrent = e.getY();
      switch (getMode(e)) {
      case ROTATE:
        viewer.rotateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case ROTATE_Z:
        viewer.rotateZBy(xPrevious - xCurrent);
        break;
      case XLATE:
        viewer.translateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case ZOOM:
        viewer.zoomBy(yCurrent - yPrevious);
        break;
      case SLAB_PLANE:
        viewer.slabBy(yCurrent - yPrevious);
        break;
      case PICK:
        calcRectRubberBand();
        if (viewer.haveFile()) {
          BitSet selectedAtoms = viewer.findAtomsInRectangle(rectRubber);
          if (e.isShiftDown()) {
            viewer.addSelection(selectedAtoms);
          } else {
            viewer.setSelectionSet(selectedAtoms);
          }
        }
        break;
      case POPUP_MENU:
        break;
      }
      xPrevious = xCurrent;
      yPrevious = yCurrent;
    }
  }

  /*
    REMOVE COMMENT TO ENABLE WHEELMOUSE
  final static int wheelClickPercentage = 10;

  class MyMouseWheelListener implements MouseWheelListener {
    public void mouseWheelMoved(MouseWheelEvent e) {
      int rotation = e.getWheelRotation();
      int modifiers = e.getModifiers();
      if ((modifiers & SHIFT) == SHIFT)
        viewer.slabByPercent(rotation * wheelClickPercentage);
      else
        viewer.zoomByPercent(rotation * -wheelClickPercentage);
    }
  }
  */
}
