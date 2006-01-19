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
import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;

class Sticks extends Shape {

  float maxBondingDistance;

  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    setMadBond(mad, JmolConstants.BOND_COVALENT_MASK, bsSelected);
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
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
    if ("bondOrder" == propertyName) {
      if (value instanceof Short) {
        short order = ((Short)value).shortValue();
        setOrderBond(order, bsSelected);
      }
      if (value instanceof String) {
        String str = (String)value;
        for (int i = JmolConstants.bondOrderNames.length; --i >= 0; )
          if (str.equals(JmolConstants.bondOrderNames[i]))
            setOrderBond(JmolConstants.bondOrderValues[i], bsSelected);
      }
      return;
    }
    if ("delete" == propertyName) {
      deleteSelectedBonds(bsSelected);
      return;
    }
    if ("maxDistance" == propertyName) {
      maxBondingDistance = ((Float)value).floatValue();
      return;
    }
    if ("targetSet" == propertyName) {
      BitSet bsTarget = (BitSet)value;
      addBonds(maxBondingDistance, bsSelected, bsTarget);
      return;
    }
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
      System.out.println("setColixBond called with palette:" + palette);
    }
  }

  void setTranslucencyBond(boolean isTranslucent,
                           short bondTypeMask, BitSet bs) {
    System.out.println("setTranslucencyBond " + isTranslucent);
    BondIterator iter = frame.getBondIterator(bondTypeMask, bs);
    while (iter.hasNext())
      iter.next().setTranslucent(isTranslucent);
  }

  void setOrderBond(short order, BitSet bs) {
    BondIterator iter = frame.getBondIterator(JmolConstants.BOND_ALL_MASK, bs);
    while (iter.hasNext())
      iter.next().setOrder(order);
  }

  void deleteSelectedBonds(BitSet bs) {
    BondIterator iter = frame.getBondIterator(JmolConstants.BOND_ALL_MASK, bs);
    BitSet bsDelete = new BitSet();
    while (iter.hasNext()) {
      bsDelete.set(iter.nextIndex());
      iter.next();
    }
    frame.deleteBonds(bsDelete);
  }

  void addBonds(float maxDistance, BitSet bsA, BitSet bsB) {
    System.out.println("addBonds was called with maxDistance=" + maxDistance +
                       "\nbsA=" + bsA + " bsB=" + bsB);
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    float maxDistanceSquared = maxDistance * maxDistance;
    for (int iA = atomCount; --iA >= 0; ) {
      if (! bsA.get(iA))
        continue;
      Atom atomA = atoms[iA];
      Point3f pointA = atomA.point3f;
      for (int iB = atomCount; --iB >= 0; ) {
        if (! bsB.get(iB))
          continue;
        if (iB == iA ||
            (iB < iA && bsA.get(iB) && bsB.get(iA)))
          continue;
        Atom atomB = atoms[iB];
        float distanceSquared = pointA.distanceSquared(atomB.point3f);
        if (distanceSquared <= maxDistanceSquared)
          frame.bondAtoms(atomA, atomB, JmolConstants.BOND_COVALENT_SINGLE);
      }
    }
  }
}
