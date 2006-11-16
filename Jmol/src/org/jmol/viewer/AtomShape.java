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
import org.jmol.util.ArrayUtil;

class AtomShape extends Shape {

  // Balls, Halos, Labels, MpsShapes, Stars, Vectors
  
  short[] mads;
  short[] colixes;
  short[] paletteIDs;
  BitSet bsSizeSet;
  BitSet bsColixSet;
  int atomCount;
  Atom[] atoms;
  boolean isActive;
  
  void initShape() {
    atomCount = frame.atomCount;
    atoms = frame.atoms;  
  }
  
  void setSize(int size, BitSet bsSelected) {
    //Halos Stars Vectors only
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    boolean isVisible = (size != 0);
    for (int i = atomCount; --i >= 0;)
      if (bsSelected.get(i)) {
        if (mads == null)
          mads = new short[atomCount];
        Atom atom = atoms[i];
        mads[i] = atom.convertEncodedMad(size);
        bsSizeSet.set(i, isVisible);
        atom.setShapeVisibility(myVisibilityFlag, isVisible);
      }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      isActive = true;
      short colix = Graphics3D.getColix(value);
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      int pid = (value instanceof Byte ? ((Byte) value).intValue() : -1);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null) {
            colixes = new short[atomCount];
            paletteIDs = new short[atomCount];
          }
          colixes[i] = setColix(colix, pid, atoms[i]);
          paletteIDs[i] = (short) pid;
          bsColixSet.set(i, colixes[i] != Graphics3D.INHERIT);
        }
      return;
    }
    if ("translucency" == propertyName) {
      isActive = true;
      boolean isTranslucent = ("translucent" == value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i)) {
          if (colixes == null)
            colixes = new short[atomCount];
          colixes[i] = Graphics3D.getColixTranslucent(colixes[i], isTranslucent);
          if (isTranslucent)
            bsColixSet.set(i);
        }
      return;
    }
  }

  void setColixAndPalette(short colix, int paletteID, int atomIndex) {
    if (colixes == null || atomIndex >= colixes.length) {
      if (colix == 0)
        return;
      colixes = ArrayUtil.ensureLength(colixes, atomIndex + 1);
      paletteIDs = ArrayUtil.ensureLength(paletteIDs, atomIndex + 1);
    }
    if (bsColixSet == null)
      bsColixSet = new BitSet();
    bsColixSet.set(atomIndex, colix != Graphics3D.INHERIT || paletteID > 0);    
    colixes[atomIndex] = setColix(colix, paletteID, atomIndex);
    paletteIDs[atomIndex] = (short) paletteID;
  }
  
  void setModelClickability() {
    if (mads == null)
      return;
    for (int i = atomCount; --i >= 0; )
      if ((atoms[i].shapeVisibilityFlags & myVisibilityFlag) != 0)
        atoms[i].clickabilityFlags |= myVisibilityFlag;
  }

  String getShapeState() {
    if (!isActive)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    String type = JmolConstants.shapeClassBases[shapeID];
    for (int i = atomCount; --i >= 0;) {
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, type + " " + (mads[i] / 2000f));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand(type, paletteIDs[i], colixes[i]));
    }
    return getShapeCommands(temp, temp2, atomCount);
  }
}
