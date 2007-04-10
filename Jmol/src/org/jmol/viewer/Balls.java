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
import java.util.Hashtable;

class Balls extends AtomShape {
  
  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    int bsLength = bsSelected.length();
    for (int i = bsLength; --i >= 0; ) {
      if (bsSelected.get(i)) {
        Atom atom = atoms[i];
        atom.setMadAtom(mad);
        bsSizeSet.set(i);
      }
    }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      if (colix == Graphics3D.INHERIT_ALL)
        colix = Graphics3D.USE_PALETTE;
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      byte pid = JmolConstants.pidOf(value);
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          Atom atom = atoms[i];
          atom.setColixAtom(setColix(colix, pid, atom));
          bsColixSet.set(i, colix != Graphics3D.USE_PALETTE
              || pid != JmolConstants.PALETTE_NONE);
          atom.setPaletteID(pid);
        }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = (((String)value).equals("translucent"));
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          atoms[i].setTranslucent(isTranslucent, translucentLevel);
          if (isTranslucent)
            bsColixSet.set(i);
        }
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  void setModelClickability() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      atom.clickabilityFlags = 0;
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(i))
        continue;
      atom.clickabilityFlags |= myVisibilityFlag;
    }
  }
  
  void setVisibilityFlags(BitSet bs) {
    int displayModelIndex = viewer.getDisplayModelIndex();
    boolean isOneFrame = (displayModelIndex >= 0); 
    boolean showHydrogens = viewer.getShowHydrogens();
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      atom.shapeVisibilityFlags &= (
          ~JmolConstants.ATOM_IN_MODEL
          & ~myVisibilityFlag);
      if (! showHydrogens && atom.getElementNumber() == 1)
        continue;
      if (! isOneFrame && bs.get(atom.modelIndex) 
          || atom.modelIndex == displayModelIndex) { 
        atom.shapeVisibilityFlags |= JmolConstants.ATOM_IN_MODEL;
        if (atom.madAtom != 0 &&  !frame.bsHidden.get(i))
          atom.shapeVisibilityFlags |= myVisibilityFlag;
      }
    }
  }

  String getShapeState() {
    Hashtable temp = new Hashtable();
    for (int i = 0; i < atomCount; i++) {
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, "Spacefill " + (atoms[i].madAtom / 2000f));
      if (bsColixSet != null && bsColixSet.get(i)) {
        byte pid = atoms[i].getPaletteID();
        if (pid != JmolConstants.PALETTE_CPK || atoms[i].isTranslucent())
          setStateInfo(temp, i, getColorCommand("atoms", pid, atoms[i].colixAtom));
      }
    }
    return getShapeCommands(temp, null, atomCount);
  }
  
  boolean checkObjectHovered(int x, int y) {
    //just for debugging
    if (!viewer.getNavigationMode())
      return false;
    viewer.hoverOn(x, y, x + " " + y);
    return true;
  }


}
