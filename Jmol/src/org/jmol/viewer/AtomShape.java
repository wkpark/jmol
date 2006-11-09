/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-22 03:13:40 -0500 (Tue, 22 Aug 2006) $
 * $Revision: 5412 $

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
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;

class AtomShape extends Shape {

  short[] mads;
  short[] colixes;

  void setSize(int size, BitSet bsSelected) {
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    Atom[] atoms = frame.atoms;
    for (int i = frame.atomCount; --i >= 0;)
      if (bsSelected.get(i)) {
        if (mads == null)
          mads = new short[frame.atomCount];
        Atom atom = atoms[i];
        mads[i] = atom.convertEncodedMad(size);
        bsSizeSet.set(i);
      }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      int pid = (value instanceof Byte ? ((Byte) value).intValue() : -1);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null)
            colixes = new short[atomCount];
          colixes[i] = ((colix != Graphics3D.UNRECOGNIZED)
                        ? colix
                        : viewer.getColixAtomPalette(atoms[i], pid));
          bsColixSet.set(i, colixes[i] != 0 || atoms[i].isTranslucent());
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
          if (isTranslucent)
            bsColixSet.set(i);

        }
      return;
    }
  }
  
  void setVisibilityFlags() {
    BitSet bsSelected = (viewer.getSelectionHaloEnabled() ? viewer
        .getSelectionSet() : null);
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = frame.atoms[i];
      boolean isVisible = bsSelected != null && bsSelected.get(i)
          || (mads != null && mads[i] != 0);
      atom.setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }

  void setModelClickability() {
    if (mads == null)
      return;
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = frame.atoms[i];
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0)
        atom.clickabilityFlags |= myVisibilityFlag;
    }
  }
  
  String getShapeState() {
    if (bsSizeSet == null)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    String type = JmolConstants.shapeClassBases[shapeID];
    for (int i = frame.atomCount; --i >= 0;) {
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, type + " " + (mads[i] / 2000f));
      if (bsColixSet != null && bsColixSet.get(i)) {
        setStateInfo(temp2, i, "color " + type + " [x"
            + g3d.getHexColorFromIndex(colixes[i]) + "]");
        if (Graphics3D.isColixTranslucent(colixes[i]))
          setStateInfo(temp2, i, "color " + type + " translucent");
      }
    }
    return getShapeCommands(temp, temp2, frame.atomCount);
  }  

}
