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

class Vectors extends AtomShape {

  String[] strings;
  boolean isApplicable;

  void initShape() {
    isApplicable = frame.hasVibrationVectors;
    if (!isApplicable)
      return;
    super.initShape();
  }

  void setSize(int size, BitSet bsSelected) {
    if (!isApplicable)
      return;
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    short mad = (short) size;
    boolean isVisible = (mad != 0);
    for (int i = atomCount; --i >= 0;)
      if (bsSelected.get(i)) {
        if (mads == null)
          mads = new short[atomCount];
        mads[i] = mad;
        bsSizeSet.set(i, isVisible);
        atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
      }
  }

  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    if (!isApplicable)
      return;
    if ("color" == propertyName) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      int pid = (value instanceof Byte ? ((Byte) value).intValue() : -1);
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0;)
        if (bsSelected.get(i))
          setColixAndPalette(colix, pid, i);
    }
  }

  String getShapeState() {
    if (!isApplicable)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    for (int i = atomCount; --i >= 0;) {
      if (bsSizeSet == null || !bsSizeSet.get(i))
        continue;
      setStateInfo(temp, i, "vector " + (mads[i] / 1000f));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand("vector", paletteIDs[i],
            colixes[i]));
    }
    return getShapeCommands(temp, temp2, atomCount);
  }
}
