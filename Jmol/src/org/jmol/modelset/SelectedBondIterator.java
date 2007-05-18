/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import org.jmol.viewer.JmolConstants;
import java.util.BitSet;

/* 
 * The first half of these methods create the model set;
 * the second half are accessed after model set creation.
 *  
 */
class SelectedBondIterator implements BondIterator {

  ModelSet modelSet;
  short bondType;
  int iBond;
  BitSet bsSelected;
  boolean bondSelectionModeOr;
  boolean isBondBitSet;

  SelectedBondIterator(ModelSet modelSet, short bondType, BitSet bsSelected, boolean bondSelectionModeOr) {
    this.modelSet = modelSet;
    this.bondType = bondType;
    this.bsSelected = bsSelected;
    this.bondSelectionModeOr = bondSelectionModeOr;
    isBondBitSet = false;
    iBond = 0;
  }

  SelectedBondIterator(ModelSet modelSet, BitSet bsSelected) {
    this.modelSet = modelSet;
    this.bsSelected = bsSelected;
    isBondBitSet = true;
    iBond = 0;
  }

  public boolean hasNext() {
    for (; iBond < modelSet.bondCount; ++iBond) {
      Bond bond = modelSet.bonds[iBond];
      if (isBondBitSet) {
        if (bsSelected.get(iBond))
          return true;
        continue;
      } else if (bondType != JmolConstants.BOND_ALL_MASK
          && (bond.order & bondType) == 0) {
        continue;
      }
      boolean isSelected1 = bsSelected.get(bond.atom1.atomIndex);
      boolean isSelected2 = bsSelected.get(bond.atom2.atomIndex);
      if ((!bondSelectionModeOr && isSelected1 && isSelected2)
          || (bondSelectionModeOr && (isSelected1 || isSelected2)))
        return true;
    }
    return false;
  }

  public int nextIndex() {
    return iBond;
  }

  public Bond next() {
    return modelSet.bonds[iBond++];
  }
}
