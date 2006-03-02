/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.util.BitSet;

import org.jmol.g3d.Graphics3D;

class Stars extends Shape {

  short[] mads;
  short[] colixes;

  void setSize(int size, BitSet bsSelected) {
    
    
    Atom[] atoms = frame.atoms;
    boolean isVisible = (size != 0);
    int bsLength = bsSelected.length();
    if (mads == null && bsLength > 0)
      mads = new short[frame.atomCount];
    for (int i = bsLength; --i >= 0; ) {
      if (bsSelected.get(i)) {
        Atom atom = atoms[i];
        atom.setShapeVisibility(myVisibilityFlag,isVisible);
        mads[i] = atom.convertEncodedMad(size);
      }
    }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null)
            colixes = new short[atomCount];
          colixes[i] = ((colix != Graphics3D.UNRECOGNIZED)
                        ? colix
                        : viewer.getColixAtomPalette(atoms[i], (String)value));
        }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null)
            colixes = new short[atomCount];
          colixes[i] = Graphics3D.setTranslucent(colixes[i], isTranslucent);
        }
      return;
    }
  }

  void setModelVisibility() {
    if (mads == null)
      return;
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if ((atom.visibilityFlags & JmolConstants.VISIBLE_MODEL) != 0
          && mads[i] > 0) 
        atom.visibilityFlags |= JmolConstants.VISIBLE_STAR;
    }
  }
}
