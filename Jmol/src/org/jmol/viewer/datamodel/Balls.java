/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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

package org.jmol.viewer.datamodel;

import java.awt.Color;
import java.util.BitSet;

public class Balls extends Shape {
  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; )
      if (bsSelected.get(i))
        atoms[i].setMadAtom(mad);
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      short colix = g3d.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          atoms[i].setColixAtom(colix);
      return;
    }
    if ("colorScheme" == propertyName) {
      if (value != null) {
        byte palette = viewer.getPalette((String)value);
        for (int i = atomCount; --i >= 0; ) {
          Atom atom = atoms[i];
          if (bs.get(i))
            atom.setColixAtom(viewer.getColixAtomPalette(atom, palette));
        }
      }
      return;
    }
  }

  final static int minimumPixelSelectionRadius = 4;

  /**
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  void findNearestAtomIndex(int x, int y, Closest closest) {
    if (frame.atomCount == 0)
      return;
    Atom champion = null;
    int championIndex = -1;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom contender = frame.atoms[i];
      if (contender.isCursorOnTopOfVisibleAtom(x, y,
                                               minimumPixelSelectionRadius,
                                               champion)) {
        champion = contender;
        championIndex = i;
      }
    }
    closest.atom = champion;
  }
}
