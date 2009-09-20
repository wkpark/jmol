/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 04:01:47 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11126 $
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

import java.awt.*;
import javax.swing.UIManager;

import org.jmol.api.*;

 public class Jvm12 {

  protected JmolViewer viewer;
  public Component awtComponent;
  
  protected String appletContext;

  Jvm12(Component awtComponent, JmolViewer viewer, String appletContext) {
    this.awtComponent = awtComponent;
    this.viewer = viewer;
    this.appletContext = appletContext;
    try {
      UIManager
          .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception exc) {
      System.err.println("Error loading L&F: " + exc);
    }
  }

  private final Rectangle rectClip = new Rectangle();
  private final Dimension dimSize = new Dimension();

  Rectangle getClipBounds(Graphics g) {
    return g.getClipBounds(rectClip);
  }

  public Dimension getSize() {
    return awtComponent.getSize(dimSize);
  }

}
