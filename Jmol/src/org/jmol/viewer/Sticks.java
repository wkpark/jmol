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
  float minBondingDistance;
  short order;
  boolean isConnectStatic;
  BitSet bsSource;
  
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
        order = ((Short)value).shortValue();
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

    //sorry; I couldn't figure out the type-casting business! -BH
    if ("connectStatic" == propertyName) {
      isConnectStatic = true;
      return;
    }

    if ("connectDynamic" == propertyName) {
      isConnectStatic = false;
      return;
    }

    if ("connectBondOrder" == propertyName) {
      if (value instanceof Short) {
        order = ((Short)value).shortValue();
      }
      if (value instanceof Float) {
        order = ((Float)value).shortValue();
      }
      if (value instanceof String) {
        String str = (String)value;
        for (int i = JmolConstants.bondOrderNames.length; --i >= 0; ) {
          if (str.equals(JmolConstants.bondOrderNames[i])) {
            order = JmolConstants.bondOrderValues[i];
            break;
          }
        }
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
    if ("connectDistance" == propertyName) {
      if(maxBondingDistance >  100000.0) {
        maxBondingDistance = ((Float)value).floatValue();
      } else {
        minBondingDistance = maxBondingDistance;
        maxBondingDistance = ((Float)value).floatValue();
      }
      return;
    }
    if ("sourceSet" == propertyName) {
      bsSource = (BitSet)value;
      return;
    }
    if ("targetSet" == propertyName) {
      BitSet bsTarget = (BitSet)value;
      if(minBondingDistance < 0.0F) 
        minBondingDistance = 0.0F;
      makeConnections(minBondingDistance, maxBondingDistance, (bsSource != null ? bsSource : bsSelected), bsTarget, order, isConnectStatic);
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

  void makeConnections(float minDistance, float maxDistance, BitSet bsA, BitSet bsB, short order, boolean isStatic) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    int nbonds = 0;
    boolean bondmode = viewer.getBondSelectionModeOr();
    viewer.setBondSelectionModeOr(false);
    
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    //System.out.println("distances "+minDistance+" "+maxDistance);
    for (int iA = atomCount; --iA >= 0; ) {
      if (! bsA.get(iA))
        continue;
      Atom atomA = atoms[iA];
      Point3f pointA = atomA.point3f;
      for (int iB = atomCount; --iB >= 0; ) {
        if (! bsB.get(iB))
          continue;
        //System.out.println(iA+" "+iB+" "+atomA.isBonded(atoms[iB]));
        if (   iB == iA
            || (frame.getAtomAt(iA).getModelIndex() != frame.getAtomAt(iB).getModelIndex())
            || (iB < iA && bsA.get(iB) && bsB.get(iA))
            || (isStatic && !atomA.isBonded(atoms[iB]))
           )
          continue;
        //System.out.println(iA+" "+iB+" continuing");
        Atom atomB = atoms[iB];
        float distanceSquared = pointA.distanceSquared(atomB.point3f);
        if (distanceSquared > minDistanceSquared && distanceSquared <= maxDistanceSquared) {
          BitSet bsTwoAtoms=new BitSet();
          bsTwoAtoms.set(iA);
          bsTwoAtoms.set(iB);
          nbonds++;
          if (order == 0){
            deleteSelectedBonds(bsTwoAtoms);
          } else {
            if (!frame.bondAtomsByNumber(iA, iB, (int)order)) {
              setOrderBond(order, bsTwoAtoms);
            }
          }
        }
      }
    }
    System.out.println("Sticks.java::makeConnections: " + nbonds + " bonds " + (order == 0 ? " deleted":" formed or modified"));
    viewer.setBondSelectionModeOr(bondmode);
  }
}
