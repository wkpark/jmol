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

import org.jmol.g3d.Graphics3D;

class Hsticks extends Sticks {

  void setSize(int size, BitSet bsSelected) {
    //Logger.debug("Hsticks.setSize()");
    //frame.calcHbonds();
    short mad = (short)size;
    setMadBond(mad, JmolConstants.BOND_HYDROGEN_MASK, bsSelected);
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    if ("color" == propertyName) {
      short colix = Graphics3D.getColix(value);
      if (colix == Graphics3D.UNRECOGNIZED && "type" == (String)value) {
        BondIterator iter =
          frame.getBondIterator(JmolConstants.BOND_HYDROGEN_MASK,
                                bsSelected);
        while (iter.hasNext()) {
          Bond bond = iter.next();
          bond.setColix(viewer.getColixHbondType(bond.order));
        }
        return;
      }
      setColixBond(colix,
                   (colix != Graphics3D.UNRECOGNIZED) ? null : (String)value,
                   JmolConstants.BOND_HYDROGEN_MASK,
                   bsSelected);
      return;
    }
    if ("translucency" == propertyName) {
      setTranslucencyBond(value == "translucent",
                          JmolConstants.BOND_HYDROGEN_MASK, bsSelected);
      return;
    }
  }
}
