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

import org.jmol.util.Logger;


import java.util.BitSet;
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;

class Sticks extends Shape {
  
  short myMask;
  
  void initShape() {
    myMask = JmolConstants.BOND_COVALENT_MASK;
  }

  BitSet bsOrderSet;
  
  void setSize(int size, BitSet bsSelected) {
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    boolean isBonds = viewer.isBondSelection();
    BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
        .getBondIterator(myMask, bsSelected));
    short mad = (short) size;
    while (iter.hasNext()) {
      bsSizeSet.set(iter.nextIndex());
      iter.next().setMad(mad);
    }
  }
  
  void setProperty(String propertyName, Object value, BitSet bsSelected) {
    Logger.debug(propertyName + " " + value + " " + bsSelected);
    boolean isBonds = viewer.isBondSelection();
    
    if ("bondOrder" == propertyName) {
      if (bsOrderSet == null)
        bsOrderSet = new BitSet();
      short order = ((Short) value).shortValue();
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        bsOrderSet.set(iter.nextIndex());
        iter.next().setOrder(order);
      }
      return;      
    }
    if (bsColixSet == null)
      bsColixSet = new BitSet();
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      int pid = (value instanceof Byte ? ((Byte) value).intValue() : -1);
      if (pid == JmolConstants.PALETTE_TYPE) {
        //only for hydrogen bonds
        BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected)
            : frame.getBondIterator(myMask, bsSelected));
        while (iter.hasNext()) {
          bsColixSet.set(iter.nextIndex());
          Bond bond = iter.next();
          bond.setColix(viewer.getColixHbondType(bond.order));
        }
        return;
      }
      if (colix == Graphics3D.UNRECOGNIZED)
        return; //palettes not implemented for bonds
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        int iBond = iter.nextIndex();
        Bond bond = iter.next();
        bond.setColix(colix);
        bsColixSet.set(iBond, colix != 0 || bond.isTranslucent());
      }
      return;
    }
    if ("translucency" == propertyName) {
      boolean isTranslucent = (((String) value).equals("translucent"));
      BondIterator iter = (isBonds ? frame.getBondIterator(bsSelected) : frame
          .getBondIterator(myMask, bsSelected));
      while (iter.hasNext()) {
        bsColixSet.set(iter.nextIndex());
        iter.next().setTranslucent(isTranslucent);
      }
      return;
    }
    //better not be here
    super.setProperty(propertyName, value, bsSelected);
  }

  void setModelClickability() {
    Bond[] bonds = frame.bonds;
    for (int i = frame.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if ((bond.shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(bond.atom1.atomIndex)
          || frame.bsHidden.get(bond.atom2.atomIndex))
        continue;
      bond.atom1.clickabilityFlags |= myVisibilityFlag;
      bond.atom2.clickabilityFlags |= myVisibilityFlag;
    }
  }
  
  String getShapeState() {
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    Bond[] bonds = frame.bonds;
    String type = JmolConstants.shapeClassBases[shapeID];
    for (int i = frame.bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, type + " " + (bond.mad / 2000f));
      if (bsColixSet != null && bsColixSet.get(i)) {
        setStateInfo(temp2, i, "color bonds [x"
            + g3d.getHexColorFromIndex(bond.colix) + "]");
        if (Graphics3D.isColixTranslucent(bond.colix))
          setStateInfo(temp2, i, "color bonds translucent");
      }
      setStateInfo(temp2, i, "bondOrder " + JmolConstants.getBondOrderNameFromOrder(bond.order));
    }
    return getShapeCommands(temp, temp2, -1, "select BONDS") + "select *;\n";
  }  
}
