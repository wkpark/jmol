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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import java.util.BitSet;
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;

class Vectors extends Shape {

  String[] strings;
  short[] mads;
  short[] colixes;
  BitSet bsSizeSet;
  BitSet bsColixSet;

  void initShape() {
    if (frame.hasVibrationVectors) {
      mads = new short[frame.atomCount];
      colixes = new short[frame.atomCount];
    }
  }

  void setSize(int size, BitSet bsSelected) {
    if (frame.hasVibrationVectors) {
      if (bsSizeSet == null)
        bsSizeSet = new BitSet();
      short mad = (short) size;
      //Atom[] atoms = frame.atoms;
      boolean isVisible = (mad != 0);
      for (int i = frame.atomCount; --i >= 0;)
        if (bsSelected.get(i)) {
          mads[i] = mad;
          frame.atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
          bsSizeSet.set(i, isVisible);
        }
    }
  }

  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if (frame.hasVibrationVectors) {
      //Atom[] atoms = frame.atoms;
      if ("color" == propertyName) {
        if (bsColixSet == null)
          bsColixSet = new BitSet();
        short colix = Graphics3D.getColix(value);
        for (int i = frame.atomCount; --i >= 0; )
          if (bsSelected.get(i))
            bsColixSet.set(i, (colixes[i] = colix) != 0);
      }
    } 
  }
  
  void setModelClickability() {
    if (mads == null)
      return;
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) != 0)
        atom.clickabilityFlags |= myVisibilityFlag;
    }
  }

  String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    for (int i = frame.atomCount; --i >= 0;) {
      if (bsSizeSet == null || !bsSizeSet.get(i))
        continue;
      setStateInfo(temp, i, "vector " + (mads[i] / 1000f));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand("vector", colixes[i]));
    }
    return getShapeCommands(temp, temp2, frame.atomCount);
  }  
}
