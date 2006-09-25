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
package org.jmol.applet;

import org.jmol.api.*;
import java.awt.*;

class Jvm12 {

  Component awtComponent;
  Console console;
  JmolViewer viewer;

  Jvm12(Component awtComponent, JmolViewer viewer) {
    this.awtComponent = awtComponent;
    this.viewer = viewer;
  }

  final Rectangle rectClip = new Rectangle();
  final Dimension dimSize = new Dimension();
  Rectangle getClipBounds(Graphics g) {
    return g.getClipBounds(rectClip);
  }

  Dimension getSize() {
    return awtComponent.getSize(dimSize);
  }

  void showConsole(boolean showConsole) {
    System.out.println("Jvm12.showConsole(" + showConsole + ")");
    if (! showConsole) {
      if (console != null) {
        console.setVisible(false);
        console = null;
      }
      return;
    }
    if (console == null)
      console = new Console(awtComponent, viewer, this);
    console.setVisible(true);
  }

  void consoleMessage(String message) {
    if (console != null)
      console.output(message);
  }
  
  void addCommand(String command) {
    if (console != null)
      console.addCommand(command);
  }
}
