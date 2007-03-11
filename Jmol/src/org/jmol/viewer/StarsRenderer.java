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

class StarsRenderer extends ShapeRenderer {

  void render() {
    Stars stars = (Stars) shape;
    if (stars.mads == null)
      return;
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || frame.bsHidden.get(i)
          || !g3d.setColix(Shape.getColix(stars.colixes, i, atom)))
        continue;
      render1(atom, stars.mads[i]);
    }
  }

  void render1(Atom atom, short mad) {
    int x = atom.screenX;
    int y = atom.screenY;
    int z = atom.screenZ;
    int d = viewer.scaleToScreen(z, mad);
    d -= (d & 1) ^ 1; // round down to odd value
    int r = d / 2;
    g3d.drawLine(x - r, y, z, x - r + d, y, z);
    g3d.drawLine(x, y - r, z, x, y - r + d, z);
  }

}
