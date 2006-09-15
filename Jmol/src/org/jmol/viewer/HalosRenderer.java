/* $RCSfile$
 * $Author: migueljmol $
 * $Date: 2006-03-25 09:27:43 -0600 (Sat, 25 Mar 2006) $
 * $Revision: 4696 $

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

import org.jmol.g3d.Graphics3D;
import java.util.BitSet;

class HalosRenderer extends ShapeRenderer {

  void render() {
    Halos halos = (Halos) shape;
    boolean selectDisplayTrue = viewer.getSelectionHaloEnabled();
    if (halos.mads == null && !selectDisplayTrue)
      return;
    Atom[] atoms = frame.atoms;
    BitSet bsSelected = (selectDisplayTrue ? viewer.getSelectionSet() : null);
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) == 0)
        continue;
      short mad = (halos.mads == null ? 0 : halos.mads[i]);
      short colix = (halos.colixes == null ? 0 : halos.colixes[i]);
      if (selectDisplayTrue && bsSelected.get(i)) {
        if (mad == 0)
          mad = -1; // unsized
        if (colix == 0)
          colix = viewer.getColixSelection();
        if (colix == Graphics3D.UNRECOGNIZED)
          colix = Graphics3D.GOLD;
      }
      if (mad == 0)
        continue;
      render1(atom, mad, colix);
    }
  }

  void render1(Atom atom, short mad, short colix) {
    int z = atom.screenZ;
    int diameter = mad;
    if (diameter < 0) { //unsized selection
      diameter = atom.screenDiameter;
      if (diameter == 0)
        diameter = viewer.scaleToScreen(z, 500);
    } else {
      diameter = viewer.scaleToScreen(z, mad);
    }
    int halowidth = (diameter / 4);
    if (halowidth < 4)
      halowidth = 4;
    if (halowidth > 10)
      halowidth = 10;
    int haloDiameter = diameter + 2 * halowidth;
    if (haloDiameter <= 0)
      return;
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    g3d.fillScreenedCircleCentered(colix, haloDiameter, atom.screenX,
        atom.screenY, atom.screenZ);
  }
}
