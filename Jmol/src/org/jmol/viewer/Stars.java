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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import java.util.BitSet;

class Stars extends Shape {

  short[] mads;
  short[] colixes;

  void setSize(int size, BitSet bsSelected) {
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; )
      if (bsSelected.get(i)) {
        if (mads == null)
          mads = new short[frame.atomCount];
        mads[i] = Atom.convertEncodedMad(atoms[i], size);
      }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      short colix = g3d.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null)
            colixes = new short[atomCount];
          colixes[i] = colix;
        }
      return;
    }
    if ("colorScheme" == propertyName) {
      if (value != null) {
        byte palette = viewer.getPalette((String)value);
        for (int i = atomCount; --i >= 0; ) {
          Atom atom = atoms[i];
          if (bs.get(i)) {
            if (colixes == null)
              colixes = new short[atomCount];
            colixes[i] = viewer.getColixAtomPalette(atom, palette);
          }
        }
      }
      return;
    }
  }
}
