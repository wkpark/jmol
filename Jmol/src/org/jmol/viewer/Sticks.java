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
import org.jmol.g3d.Graphics3D;

class Sticks extends Shape {
  
  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    setMadBond(mad, JmolConstants.BOND_COVALENT_MASK, bsSelected);
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    Logger.debug(propertyName+" "+value+" "+bsSelected);
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      setColixBond(colix,
                   (colix != Graphics3D.UNRECOGNIZED) ? null : (String)value,
                   JmolConstants.BOND_COVALENT_MASK,
                   bsSelected);
      return;
    }
    if ("translucency" == propertyName) {
      setTranslucencyBond(value == "translucent",
                          JmolConstants.BOND_COVALENT_MASK, bsSelected);
      return;
    }
    super.setProperty(propertyName, value, bsSelected);
  }

  void setMadBond(short mad, short bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setMad(mad);
  }

  void setColixBond(short colix, String palette,
                    short bondTypeMask, BitSet bs) {
    if (colix != Graphics3D.UNRECOGNIZED) {
      BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
      while (iter.hasNext())
        iter.next().setColix(colix);
    } else {
      Logger.error("setColixBond called with palette:" + palette);
    }
  }

  void setTranslucencyBond(boolean isTranslucent,
                           short bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setTranslucent(isTranslucent);
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

}

