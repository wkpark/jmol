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

import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Canvas;
/*
  see comment below
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
*/

public class AppletCanvas extends Canvas {

  private JmolViewer viewer;

  public void setJmolViewer(JmolViewer viewer) {
    this.viewer = viewer;
    /*
    viewer.setScreenDimension(getSize());
    addComponentListener(new MyComponentListener());
    */
  }

  /*
  private void updateSize() {
    viewer.setScreenDimension(getSize());
  }
  */

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics g) {
    viewer.setScreenDimension(getSize());
    g.drawImage(viewer.renderScreenImage(g.getClipBounds()), 0, 0, null);
  }

  // Make sure AWT knows we are using a buffered image.
  public boolean isDoubleBuffered() {
    return true;
  }

  // Make sure AWT knows we repaint the entire canvas.
  public boolean isOpaque() {
    return true;
  }

  /*
    There is a problem on old macintoshes with
      ClassNotFound java.awt.ComponentEvent
    getting rid of this *might* solve the problem

  class MyComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      updateSize();
    }
    public void componentShown(ComponentEvent e) {
      updateSize();
    }
  }
  */
}
