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

class Sticks extends Shape {

  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    setMadBond(mad, JmolConstants.BOND_COVALENT_MASK, bsSelected);
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("color" == propertyName) {
      short colix = g3d.getColix(value);
      setColixBond(colix, JmolConstants.BOND_COVALENT_MASK, bsSelected);
      return;
    }
    if ("colorScheme" == propertyName) {
      if (value instanceof String && "cpk" == (String)value) {
        setColixBond((short)0, JmolConstants.BOND_COVALENT_MASK, bsSelected);
        return;
      }
    }
    if ("bondOrder" == propertyName) {
      if (value instanceof Short) {
        short order = ((Short)value).shortValue();
        setOrderBond(order, bsSelected);
      }
    }
  }

  void setMadBond(short mad, short bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setMad(mad);
  }

  void setColixBond(short colix, short bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setColix(colix);
  }

  void setOrderBond(short order, BitSet bs) {
    BondIterator iter = frame.getBondIterator(JmolConstants.BOND_ALL_MASK, bs);
    while (iter.hasNext())
      iter.next().setOrder(order);
  }
}
