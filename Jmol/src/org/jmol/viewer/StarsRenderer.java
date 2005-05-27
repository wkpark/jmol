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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;

class StarsRenderer extends ShapeRenderer {

  void render() {
    Stars stars = (Stars)shape;
    if (stars.mads == null)
      return;
    Atom[] atoms = frame.atoms;
    int displayModelIndex = this.displayModelIndex;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (displayModelIndex >= 0 && atom.modelIndex != displayModelIndex)
        continue;
      short mad = stars.mads[i];
      if (mad == 0)
        continue;
      short colix = stars.colixes == null ? 0 : stars.colixes[i];
      render1(atom, mad, colix);
    }
  }

  void render1(Atom atom, short mad, short colix) {
    long xyzd = atom.xyzd;
    int x = Xyzd.getX(xyzd);
    int y = Xyzd.getY(xyzd);
    int z = Xyzd.getZ(xyzd);
    int d = viewer.scaleToScreen(z, mad);
    // make available for hover/click/measure
    atom.formalChargeAndFlags |= Atom.VISIBLE_FLAG;
    d -= (d & 1) ^ 1; // round down to odd value
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    int r = d / 2;
    g3d.drawLine(colix, x - r, y, z, x - r + d, y, z);
    g3d.drawLine(colix, x, y - r, z, x, y - r + d, z);
    /*
    long xyzd = atom.xyzd;
    int diameter = Xyzd.getD(xyzd);
    boolean hasHalo = viewer.hasSelectionHalo(atom.atomIndex);
    if (diameter == 0 && !hasHalo) {
      atom.formalChargeAndFlags &= ~Atom.VISIBLE_FLAG;
      return;
    }
    // mth 2004 04 02 ... hmmm ... I don't like this here ... looks ugly
    atom.formalChargeAndFlags |= Atom.VISIBLE_FLAG;

    if (!wireframeRotating)
      g3d.fillSphereCentered(atom.colixAtom, xyzd);
    else
      g3d.drawCircleCentered(atom.colixAtom, xyzd);

    if (hasHalo) {
      int halowidth = diameter / 4;
      if (halowidth < 4) halowidth = 4;
      if (halowidth > 10) halowidth = 10;
      int haloDiameter = diameter + 2 * halowidth;
      g3d.fillScreenedCircleCentered(colixSelection,
                                     haloDiameter,
                                     Xyzd.getX(xyzd), Xyzd.getY(xyzd),
                                     Xyzd.getZ(xyzd));
    }
    */
  }

}
