
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.util.Enumeration;

/**
 * Represents the connection between two atoms.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class Bond {

  private Atom atom1;
  private Atom atom2;

  public Bond(Atom a1, Atom a2) {
    this.atom1 = a1;
    this.atom2 = a2;
  }

  public boolean bindsHydrogen() {

    if ((atom1.getType().getAtomicNumber() == 1)
            || (atom2.getType().getAtomicNumber() == 1)) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Finds the order associated with the bond between atom1 and atom2.
   *
   * @param atom1 the atom whose bonds are searched.
   * @param atom2 the bonded atom for which to search.
   * @return the bond order, or -1 if the bond was not found.
   */
  public static int getBondOrder(Atom atom1, Atom atom2) {
    int bondOrder = -1;
    Enumeration bonds = atom1.getBondedAtoms();
    Enumeration bondOrders = atom1.getBondOrders();
    while (bonds.hasMoreElements() && bondOrders.hasMoreElements()) {
      Atom atom = (Atom) bonds.nextElement();
      Integer bondOrderInteger = (Integer) bondOrders.nextElement();
      if (atom == atom2) {
        bondOrder = bondOrderInteger.intValue();
        break;
      }
    }
    return bondOrder;
  }
}
