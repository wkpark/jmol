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
import java.awt.Event;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseManager11 extends MouseManager
  implements MouseListener, MouseMotionListener {

  public MouseManager11(Component component, JmolViewer viewer) {
    super(component, viewer);
    component.addMouseListener(this);
    component.addMouseMotionListener(this);
  }

  public boolean handleEvent(Event e) {
    System.out.println("MouseManager11 does not handle events");
    return false;
  }

  public void mouseClicked(MouseEvent e) {
    mouseClicked(e.getX(), e.getY(), e.getModifiers(), e.getClickCount(),
                 e.getWhen());
  }

  public void mouseEntered(MouseEvent e) {
    mouseEntered(e.getX(), e.getY());
  }
  
  public void mouseExited(MouseEvent e) {
    mouseExited(e.getX(), e.getY());
  }
  
  public void mousePressed(MouseEvent e) {
    mousePressed(e.getX(), e.getY(), e.getModifiers(), e.isPopupTrigger(),
                 e.getWhen());
  }
  
  public void mouseReleased(MouseEvent e) {
    mouseReleased(e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseDragged(MouseEvent e) {
    mouseDragged(e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseMoved(MouseEvent e) {
    mouseMoved(e.getX(), e.getY(), e.getModifiers());
  }
}
