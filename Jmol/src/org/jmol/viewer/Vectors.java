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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import java.util.BitSet;

class Vectors extends Shape {

  String[] strings;
  short[] mads;
  short[] colixes;

  void initShape() {
    if (frame.hasVibrationVectors) {
      mads = new short[frame.atomCount];
      colixes = new short[frame.atomCount];
    }
  }

  void setSize(int size, BitSet bsSelected) {
    if (frame.hasVibrationVectors) {
      short mad = (short)size;
      //Atom[] atoms = frame.atoms;
      for (int i = frame.atomCount; --i >= 0; )
        if (bsSelected.get(i))
          mads[i] = mad;
    }
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if (frame.hasVibrationVectors) {
      //Atom[] atoms = frame.atoms;
      if ("color" == propertyName) {
        short colix = g3d.getColix(value);
        for (int i = frame.atomCount; --i >= 0; )
          if (bsSelected.get(i))
            colixes[i] = colix;
      }
    } 
  }
}
