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
      connectBondOrder = -1;
      connectOperation = MODIFY_OR_CREATE;
      return;
    }
    if ("connectDistance" == propertyName) {
      if (connectDistanceCount < connectDistances.length)
        connectDistances[connectDistanceCount++] = ((Float)value).floatValue();
      else
        System.out.println("too many connect distances specified");
      return;
    }
    if ("connectSet" == propertyName) {
      if (connectSetCount < connectSets.length)
        connectSets[connectSetCount++] = (BitSet)value;
      else
        System.out.println("too many connect sets specified");
      return;
    }
    if ("connectBondOrder" == propertyName) {
      connectBondOrder = bondOrderFromString((String)value);
      return;
    }
    if ("connectOperation" == propertyName) {
      connectOperation = connectOperationFromString((String)value);
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
        makeConnections(connectDistances[0], connectDistances[1],
                        connectBondOrder, connectOperation,
                        connectSets[0], connectSets[1]);
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

  private final static int DELETE_BONDS     = 0;
  private final static int MODIFY_ONLY      = 1;
  private final static int CREATE_ONLY      = 2;
  private final static int MODIFY_OR_CREATE = 3;

  void makeConnections(float minDistance, float maxDistance,
                       short order, int connectOperation,
                       BitSet bsA, BitSet bsB) {
    /*
    System.out.println("makeConnections(" + minDistance + "," +
                       maxDistance + "," + order + "," + connectOperation +
                       "," + bsA + "," + bsB + ")";)
    */
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if (connectOperation == DELETE_BONDS) {
      deleteConnections(minDistance, maxDistance, order, bsA, bsB);
      return;
    }
    if (order <= 0)
      order = 1; // default 
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    for (int iA = atomCount; --iA >= 0; ) {
      if (! bsA.get(iA))
        continue;
      Atom atomA = atoms[iA];
      Point3f pointA = atomA.point3f;
      for (int iB = atomCount; --iB >= 0; ) {
        if (iB == iA)
          continue;
        if (! bsB.get(iB))
          continue;
        Atom atomB = atoms[iB];
        if (atomA.modelIndex != atomB.modelIndex)
          continue;
        Bond bondAB = atomA.getBond(atomB);
        if (MODIFY_ONLY == connectOperation && bondAB == null)
          continue;
        if (CREATE_ONLY == connectOperation && bondAB != null)
          continue;
        float distanceSquared = pointA.distanceSquared(atomB.point3f);
        if (distanceSquared < minDistanceSquared ||
            distanceSquared > maxDistanceSquared)
          continue;
        if (bondAB != null)
          bondAB.setOrder(order);
        else
          frame.bondAtoms(atomA, atomB, order);
        BitSet bsTwoAtoms=new BitSet();
      }
    }
  }

  void deleteConnections(float minDistance, float maxDistance, short order,
                         BitSet bsA, BitSet bsB) {
    int bondCount = frame.bondCount;
    Bond[] bonds = frame.bonds;
    BitSet bsDelete = new BitSet();
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    for (int i = bondCount; --i >= 0; ) {
      Bond bond = bonds[i];
      Atom atom1 = bond.atom1;
      Atom atom2 = bond.atom2;
      if (bsA.get(atom1.atomIndex) && bsB.get(atom2.atomIndex) ||
          bsA.get(atom2.atomIndex) && bsB.get(atom1.atomIndex)) {
        if (bond.atom1.isBonded(bond.atom2)) {
          float distanceSquared = atom1.point3f.distanceSquared(atom2.point3f);
          if (distanceSquared >= minDistanceSquared &&
              distanceSquared <= maxDistanceSquared)
            if (order <= 0 || // order defaulted to -1
                order == bond.order ||
                (order & bond.order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
              bsDelete.set(i);
        }
      }
    }
    frame.deleteBonds(bsDelete);
  }

  short bondOrderFromString(String bondOrderString) {
    if (bondOrderString != null) 
      for (int i = JmolConstants.bondOrderNames.length; --i >= 0; ) {
        if (bondOrderString.equalsIgnoreCase(JmolConstants.bondOrderNames[i]))
          return JmolConstants.bondOrderValues[i];
      }
    return 0;
  }

  int connectOperationFromString(String connectOperationString) {
    if ("delete".equalsIgnoreCase(connectOperationString))
      return DELETE_BONDS;
    if ("modify".equalsIgnoreCase(connectOperationString))
      return MODIFY_ONLY;
    if ("create".equalsIgnoreCase(connectOperationString))
      return CREATE_ONLY;
    if ("createOrModify".equalsIgnoreCase(connectOperationString))
      return MODIFY_OR_CREATE;
    System.out.println("unrecognized connect operation:" +
                       connectOperationString);
    return -1;
  }
}
