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
import org.openscience.jmol.viewer.*;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Event;
import java.util.BitSet;

public class MouseManager10 extends MouseManager {

  public MouseManager10(Component component, JmolViewer viewer) {
    super(component, viewer);
  }

  private int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & MIDDLE_RIGHT) == 0)  ? (modifiers | LEFT) : modifiers;
  }

  public boolean handleEvent(Event e) {
    int x = e.x, y = e.y, modifiers = e.modifiers, clickCount = e.clickCount;
    switch (e.id) {
    case Event.MOUSE_DOWN:
      mousePressed(x, y, applyLeftMouse(modifiers));
      break;
    case Event.MOUSE_DRAG:
      mouseDragged(x, y, applyLeftMouse(modifiers));
      break;
    case Event.MOUSE_ENTER:
      mouseEntered(x, y, applyLeftMouse(modifiers));
      break;
    case Event.MOUSE_EXIT:
      mouseExited(x, y, applyLeftMouse(modifiers));
      break;
    case Event.MOUSE_MOVE:
      mouseMoved(x, y, applyLeftMouse(modifiers));
      break;
    case Event.MOUSE_UP:
      mouseReleased(x, y, applyLeftMouse(modifiers));
      break;
    default:
      return false;
    }
    return true;
  }
}
