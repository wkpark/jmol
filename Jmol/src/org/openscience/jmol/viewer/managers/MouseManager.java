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

import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Event;

public abstract class MouseManager {

  Component component;
  JmolViewer viewer;

  int xPrevious, yPrevious;
  int xCurrent, yCurrent;
  int modifiersWhenPressed;

  boolean rubberbandSelectionMode = false;
  int xAnchor, yAnchor;
  final static Rectangle rectRubber = new Rectangle();

  public MouseManager(Component component, JmolViewer viewer) {
    this.component = component;
    this.viewer = viewer;
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

  public abstract boolean handleEvent(Event e);
}
