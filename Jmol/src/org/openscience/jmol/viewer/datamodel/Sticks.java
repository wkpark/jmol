/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;

import java.awt.Color;
import java.util.BitSet;

public class Sticks extends Shape {

  public void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    setMadBond(mad, JmolConstants.BOND_COVALENT, bsSelected);
  }
  
  public void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("color".equals(propertyName)) {
      short colix = g3d.getColix(value);
      setColixBond(colix, JmolConstants.BOND_COVALENT, bsSelected);
      return;
    }

    byte bondTypeMask;
    if (propertyName.startsWith("ssbond")) {
      bondTypeMask = JmolConstants.BOND_SULFUR_MASK;
    } else if (propertyName.startsWith("hbond")) {
      frame.calcHbonds();
      bondTypeMask = JmolConstants.BOND_HYDROGEN;
    } else if (propertyName.startsWith("all")) {
      bondTypeMask = JmolConstants.BOND_ALL_MASK;
    } else {
      System.out.println("Sticks does not recognize propertyName:" +
                         propertyName);
      return;
    }
    if (propertyName.endsWith("Mad")) {
      short mad = (short)((Integer)value).intValue();
      setMadBond(mad, bondTypeMask, bsSelected);
    } else if (propertyName.endsWith("Color")) {
      short colix = g3d.getColix(value);
      setColixBond(colix, bondTypeMask, bsSelected);
    } else {
      System.out.println("Sticks does not recognize propertyName:" +
                         propertyName);
      return;
    }
  }

  void setMadBond(short mad, byte bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setMad(mad);
  }

  void setColixBond(short colix, byte bondTypeMask, BitSet bs) {
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setColix(colix);
  }
}
