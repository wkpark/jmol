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
package org.openscience.jmol.applet;

import org.openscience.jmol.Atom;
import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.render.ChemFrameRenderer;
import org.openscience.jmol.render.MeasureRenderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Canvas;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

public class AppletCanvas extends Canvas {

  private DisplayControl control;

  public void setDisplayControl(DisplayControl control) {
    this.control = control;
    control.setScreenDimension(getSize());
    addComponentListener(new MyComponentListener());
  }

  public void updateSize() {
    control.setScreenDimension(getSize());
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics g) {
    g.drawImage(control.renderScreenImage(g.getClipBounds()), 0, 0, null);
  }

  // Make sure AWT knows we are using a buffered image.
  public boolean isDoubleBuffered() {
    return true;
  }

  // Make sure AWT knows we repaint the entire canvas.
  public boolean isOpaque() {
    return true;
  }

  class MyComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      updateSize();
    }
    public void componentShown(ComponentEvent e) {
      updateSize();
    }
  }
}
