
/*
 * Copyright 2002 The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.Atom;
import org.openscience.jmol.DisplayControl;

import java.awt.Color;
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
  private DisplayControl control;

  private int xPrevious, yPrevious;
  private int xCurrent, yCurrent;

  private boolean rubberbandSelectionMode = false;
  private int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  public MouseManager(Component component, DisplayControl control) {
    this.component = component;
    this.control = control;
    component.addMouseListener(new MyMouseListener());
    component.addMouseMotionListener(new MyMouseMotionListener());
    /*
    REMOVE COMMENT TO ENABLE WHEELMOUSE
    if (control.jvm14orGreater)
      component.addMouseWheelListener(new MyMouseWheelListener());
    */
  }

  public static final int ROTATE =     DisplayControl.ROTATE;
  public static final int ZOOM =       DisplayControl.ZOOM;
  public static final int XLATE =      DisplayControl.XLATE;
  public static final int PICK =       DisplayControl.PICK;
  public static final int DELETE =     DisplayControl.DELETE;
  public static final int MEASURE =    DisplayControl.MEASURE;
  public static final int DEFORM =     DisplayControl.DEFORM;
  public static final int ROTATE_Z =   DisplayControl.ROTATE_Z;
  public static final int SLAB_PLANE = DisplayControl.SLAB_PLANE;

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

  // FIXME -- figure out a way to get measure to work with both
  // applet and app
  Measure measure;
  public void setMeasure(Measure measure) {
    this.measure = measure;
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
      if (modeMouse == PICK) {
        rubberbandSelectionMode = true;
        xAnchor = xCurrent;
        yAnchor = yCurrent;
        calcRectRubberBand();
      }
    }

    public void mouseClicked(MouseEvent e) {
      if (control.haveFile()) {
        if ((e.getModifiers() & MIDDLE) == MIDDLE) {
          control.homePosition();
          return;
        }
        int atomIndex = control.findNearestAtomIndex(e.getX(), e.getY());
        switch (modeMouse) {
        case PICK:
          if (!e.isShiftDown()) {
            control.clearSelection();
            if (atomIndex != -1)
              control.addSelection(atomIndex);
          } else {
            if (atomIndex != -1) 
              control.toggleSelection(atomIndex);
          }
          break;
        case DELETE:
          if (atomIndex != -1)
            control.deleteAtom(atomIndex);
          break;
        case MEASURE:
          if (atomIndex != -1 && measure != null) {
            measure.firePicked(atomIndex);
          }
        }
      }
    }

    public void mouseReleased(MouseEvent e) {
      control.setInMotion(false);
      if (modeMouse == PICK) {
        rubberbandSelectionMode = false;
        component.repaint();
      }
    }
  }

  class MyMouseMotionListener extends MouseMotionAdapter {

    int getMode(int modifiers) {
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
      // if ((modifiers & RIGHT) == RIGHT)
      //   popup menu
      if ((modifiers & SHIFT_LEFT) == SHIFT_LEFT)
        return ZOOM;
      if ((modifiers & CTRL_LEFT) == CTRL_LEFT)
        return SLAB_PLANE;
      if ((modifiers & LEFT) == LEFT)
        return ROTATE;
      return modeMouse;
    }

    public void mouseDragged(MouseEvent e) {

      if (! control.inMotion) {
        control.setInMotion(true);
      }
      xCurrent = e.getX();
      yCurrent = e.getY();
      switch (getMode(e.getModifiers())) {
      case ROTATE:
        control.rotateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case ROTATE_Z:
        control.rotateZBy(xPrevious - xCurrent);
        break;
      case XLATE:
        control.translateXYBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case ZOOM:
        control.zoomBy(yCurrent - yPrevious);
        break;
      case SLAB_PLANE:
        control.slabBy(yCurrent - yPrevious);
        break;
      case PICK:
        calcRectRubberBand();
        if (control.haveFile()) {
          BitSet selectedAtoms = control.findAtomsInRectangle(rectRubber);
          if (e.isShiftDown()) {
            control.addSelection(selectedAtoms);
          } else {
            control.setSelectionSet(selectedAtoms);
          }
        }
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
        control.slabByPercent(rotation * wheelClickPercentage);
      else
        control.zoomByPercent(rotation * -wheelClickPercentage);
    }
  }
  */
}
