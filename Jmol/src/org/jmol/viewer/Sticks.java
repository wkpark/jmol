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

  final float[] connectDistances = new float[2];
  int connectDistanceCount;
  final BitSet[] connectSets = new BitSet[2];
  int connectSetCount;
  // initialized to -1;
  // for delete this means 'delete all'
  // for connect this gets turned into 'single'
  short connectBondOrder;
  int connectOperation;

  private final static float DEFAULT_MAX_CONNECT_DISTANCE = 100000000f;
  private final static float DEFAULT_MIN_CONNECT_DISTANCE = 0.1f;
  
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
    if ("resetConnectParameters" == propertyName) {
      connectDistanceCount = 0;
      connectSetCount = 0;
      connectBondOrder = JmolConstants.BOND_ORDER_NULL;
      connectOperation = JmolConstants.MODIFY_OR_CREATE;
      return;
    }
    if ("connectDistance" == propertyName) {
      if (connectDistanceCount < connectDistances.length)
        connectDistances[connectDistanceCount++] = ((Float)value).floatValue();
      else
        Logger.error("too many connect distances specified");
      return;
    }
    if ("connectSet" == propertyName) {
      if (connectSetCount < connectSets.length)
        connectSets[connectSetCount++] = (BitSet)value;
      else
        Logger.error("too many connect sets specified");
      return;
    }
    if ("connectBondOrder" == propertyName) {
      connectBondOrder = JmolConstants.getBondOrderFromString((String)value);
      return;
    }
    if ("connectOperation" == propertyName) {
      connectOperation = JmolConstants.connectOperationFromString((String)value);
      return;
    }
    if ("applyConnectParameters" == propertyName) {
      if (connectDistanceCount < 2) {
        if (connectDistanceCount == 0)
          connectDistances[0] = DEFAULT_MAX_CONNECT_DISTANCE;
        connectDistances[1] = connectDistances[0];
        connectDistances[0] = DEFAULT_MIN_CONNECT_DISTANCE;
      }
      if (connectSetCount < 2) {
        if (connectSetCount == 0)
          connectSets[0] = bsSelected;
        connectSets[1] = connectSets[0];
        connectSets[0] = bsSelected;
      }
      if (connectOperation >= 0)
        frame.makeConnections(connectDistances[0], connectDistances[1],
                        connectBondOrder, connectOperation,
                        connectSets[0], connectSets[1]);
      return;
    }
    if ("rasmolCompatibleConnect" == propertyName) {
      // miguel 2006 04 02
      // use of 'connect', 'connect on', 'connect off' is deprecated
      // I suggest that support be dropped at some point in the near future
      frame.deleteAllBonds();
      // go ahead and test out the autoBond(null, null) code a bit
      frame.autoBond(null, null);
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
    for (int i = frame.bondCount; --i >= 0; ) {
      Bond bond = bonds[i];
      if((bond.shapeVisibilityFlags & myVisibilityFlag) != 0) {
        bond.atom1.clickabilityFlags |= myVisibilityFlag;
        bond.atom2.clickabilityFlags |= myVisibilityFlag;
      }
    }
  }

}

