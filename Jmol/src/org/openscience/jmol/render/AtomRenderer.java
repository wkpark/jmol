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

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.g3d.Graphics3D;
import java.awt.Rectangle;

public class AtomRenderer {

  DisplayControl control;
  public AtomRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics3D g3d;
  Rectangle clip;

  public void setGraphicsContext(Graphics3D g3d, Rectangle clip) {
    this.g3d = g3d;
    this.clip = clip;

    fastRendering = control.getFastRendering();
    colixSelection = control.getColixSelection();
  }

  boolean fastRendering;
  short colixSelection;

  int x;
  int y;
  int z;
  int diameter;
  byte styleAtom;
  short colix;

  int radius;
  int xUpperLeft;
  int yUpperLeft;

  public void render(AtomShape atomShape) {
    styleAtom = atomShape.styleAtom;
    x = atomShape.x;
    y = atomShape.y;
    z = atomShape.z;
    diameter = atomShape.diameter;
    radius = (diameter + 1) / 2;
    xUpperLeft = x - radius;
    yUpperLeft = y - radius;
    colix = atomShape.colixAtom;
    if (control.hasSelectionHalo(atomShape))
      renderHalo();
    if (atomShape.marDots > 0)
      renderDots(atomShape.colixDots, atomShape.diameterDots);
    renderAtom();
  }

  private void renderHalo() {
    int halowidth = diameter / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int halodiameter = diameter + 2 * halowidth;
    int haloradius = (halodiameter + 1) / 2;
    g3d.fillCircleCentered(colixSelection, x, y, z+1, halodiameter);
  }

  private void renderDots(short colixDots, int diameterDots) {
    g3d.drawDotsCentered(colixDots, x, y, z, diameterDots);
  }

  private void renderAtom() {
    if (diameter > 0) {
      if (styleAtom == DisplayControl.SHADED && !fastRendering)
        g3d.fillSphereCentered(colix, diameter, x, y, z);
      else if (styleAtom != DisplayControl.NONE)
        g3d.drawCircleCentered(colix, diameter, x, y, z);
    }
  }
}
