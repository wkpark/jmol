/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import org.jmol.g3d.*;

class BallsRenderer extends ShapeRenderer {

  int minX, maxX, minY, maxY;
  boolean wireframeRotating;
  short colixSelection;

  void render() {
    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;

    wireframeRotating = viewer.getWireframeRotating();
    colixSelection = viewer.getColixSelection();
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if ((atom.visibilityFlags & JmolConstants.VISIBLE_MODEL) == 0)
        continue;
      atom.transform(viewer);
      if ((atom.visibilityFlags & JmolConstants.VISIBLE_BALL) != 0)
        renderBall(atom);     
      if ((atom.visibilityFlags & JmolConstants.VISIBLE_HALO) != 0)
        renderHalo(atom);     
    }
  }

  void renderBall(Atom atom) {
    long xyzd = atom.xyzd;
    if (!wireframeRotating)
      g3d.fillSphereCentered(atom.colixAtom, xyzd);
    else
      g3d.drawCircleCentered(atom.colixAtom, xyzd);
  }

  void renderHalo(Atom atom) {
    long xyzd = atom.xyzd;
    int diameter = Xyzd.getD(xyzd);
    int halowidth = diameter / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int haloDiameter = diameter + 2 * halowidth;
    g3d.fillScreenedCircleCentered(colixSelection,
                                     haloDiameter,
                                     Xyzd.getX(xyzd), Xyzd.getY(xyzd),
                                     Xyzd.getZ(xyzd));
  }
}
