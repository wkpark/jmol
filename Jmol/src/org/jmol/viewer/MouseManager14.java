/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

import java.awt.Component;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.jmol.viewer.binding.Binding;

class MouseManager14 extends MouseManager11
  implements MouseWheelListener {

  MouseManager14(Component display, Viewer viewer, ActionManager actionManager) {
    super(display, viewer, actionManager);
    //Logger.debug("MouseManager14 implemented");
    if (display == null)
      return;
    display.addMouseWheelListener(this);
  }

  void removeMouseListeners14() {
    viewer.getDisplay().removeMouseWheelListener(this);
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    mouseWheel(e.getWhen(), e.getWheelRotation(), e.getModifiers() | Binding.WHEEL);
  }
}
