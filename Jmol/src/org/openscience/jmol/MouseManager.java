
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

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
  }

  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DELETE = 4;
  public static final int MEASURE = 5;
  public static final int DEFORM = 6; // mth -- what is this?

  public static final String[] modeNames = {
    "ROTATE", "ZOOM", "XLATE", "PICK", "DELETE", "MEASURE", "DEFORM" };

  private int modeMouse = ROTATE;
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
        Atom atom = control.getFrame().getNearestAtom(e.getX(), e.getY());
        switch (modeMouse) {
        case PICK:
          if (atom == null) {
            control.clearSelection();
            break;
          }
          if (!e.isShiftDown()) {
            int selectionCount = control.countSelection();
            boolean wasSelected = control.isSelected(atom);
            control.clearSelection();
            if (selectionCount == 1 && wasSelected)
              break;
          }
          control.toggleSelection(atom);
          break;
        case DELETE:
          if (atom != null) {
            control.getFrame().deleteAtom(atom);
            //            status.setStatus(2, "Atom deleted"); 
          }
          component.repaint();
          break;
        case MEASURE:
          System.out.println("MEASURE clicked");
          if (atom != null && measure != null) {
            System.out.println("firing");
            measure.firePicked(atom.getAtomNumber());
          }
        }
      }
    }

    public void mouseReleased(MouseEvent e) {
      control.setMouseDragged(false);
      if (modeMouse == PICK) {
        rubberbandSelectionMode = false;
        component.repaint();
      }
    }
  }

  class MyMouseMotionListener extends MouseMotionAdapter {

    public void mouseDragged(MouseEvent e) {

      xCurrent = e.getX();
      yCurrent = e.getY();

      if (! control.mouseDragged) {
        control.setMouseDragged(true);
      }
      switch (modeMouse) {
      case ROTATE:
        control.rotateBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case XLATE:
        control.translateBy(xCurrent - xPrevious, yCurrent - yPrevious);
        break;
      case ZOOM:
        control.zoomBy((xCurrent - xPrevious) + (yPrevious - yCurrent));
        break;
      case PICK:
        calcRectRubberBand();
        if (control.haveFile()) {
          // FIXME -- do this work inside the control 
          Atom[] selectedAtoms =control.getFrame().
            findAtomsInRegion(rectRubber.x,
                              rectRubber.y,
                              rectRubber.x + rectRubber.width,
                              rectRubber.y + rectRubber.height);
          if (e.isShiftDown()) {
            control.addSelection(selectedAtoms);
          } else {
            control.clearSelection();
            control.addSelection(selectedAtoms);
          }
        }
        break;
      }
      xPrevious = xCurrent;
      yPrevious = yCurrent;
    }
  }
}
