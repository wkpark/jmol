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

class StarsRenderer extends ShapeRenderer {

  void render() {
    Stars stars = (Stars)shape;
    if (stars.mads == null)
      return;
    Atom[] atoms = frame.atoms;
    int myVisibilityFlag = stars.myVisibilityFlag;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom.isShapeVisible(myVisibilityFlag)) {
        short colix = stars.colixes == null ? 0 : stars.colixes[i];
        render1(atom, stars.mads[i], colix);
      }
    }
  }

  void render1(Atom atom, short mad, short colix) {
    long xyzd = atom.xyzd;
    int x = Xyzd.getX(xyzd);
    int y = Xyzd.getY(xyzd);
    int z = Xyzd.getZ(xyzd);
    int d = viewer.scaleToScreen(z, mad);
    d -= (d & 1) ^ 1; // round down to odd value
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    int r = d / 2;
    g3d.drawLine(colix, x - r, y, z, x - r + d, y, z);
    g3d.drawLine(colix, x, y - r, z, x, y - r + d, z);
  }

}
