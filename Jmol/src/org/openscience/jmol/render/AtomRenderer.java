/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.openscience.jmol.render;

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;
import java.util.Enumeration;

public class AtomRenderer {

  DisplayControl control;
  public AtomRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics25D g25d;
  Rectangle clip;

  public void setGraphicsContext(Graphics25D g25d, Rectangle clip) {
    this.g25d = g25d;
    this.clip = clip;

    fastRendering = control.getFastRendering();
    useGraphics2D = control.getUseGraphics2D();
    colorSelection = control.getColorSelection();
  }

  boolean fastRendering;
  boolean useGraphics2D;
  Color colorSelection;

  ShadedSphereRenderer shadedSphereRenderer;

  Atom atom;
  int x;
  int y;
  int z;
  int diameter;
  byte styleAtom;
  Color color;
  Color colorOutline;

  int radius;
  int xUpperLeft;
  int yUpperLeft;

  public void render(AtomShape atomShape) {
    styleAtom = atomShape.styleAtom;
    atom = atomShape.atom;
    x = atomShape.x;
    y = atomShape.y;
    z = atomShape.z;
    diameter = atomShape.diameter;
    radius = (diameter + 1) / 2;
    xUpperLeft = x - radius;
    yUpperLeft = y - radius;
    color = atomShape.colorAtom;
    colorOutline = control.getColorAtomOutline(styleAtom, color);
    if (control.hasSelectionHalo(atom))
      renderHalo();
    if (styleAtom != DisplayControl.NONE &&
        styleAtom != DisplayControl.INVISIBLE)
      renderAtom();
  }

  private void renderHalo() {
    int halowidth = diameter / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int halodiameter = diameter + 2 * halowidth;
    int haloradius = (halodiameter + 1) / 2;
    g25d.fillCircleCentered(colorSelection, x, y, z+1, halodiameter);
  }

  private void renderAtom() {
    if (styleAtom == DisplayControl.SHADING && !fastRendering) {
      if (shadedSphereRenderer == null)
        shadedSphereRenderer = new ShadedSphereRenderer(control);
      shadedSphereRenderer.render(g25d, xUpperLeft, yUpperLeft, z, diameter,
                                  color, colorOutline);
      return;
    }
    if (fastRendering || styleAtom == DisplayControl.WIREFRAME) {
      g25d.drawCircleCentered(color, x, y, z, diameter);
    } else {
      g25d.fillCircleCentered(colorOutline, color, x, y, z, diameter);
    }
  }
}
