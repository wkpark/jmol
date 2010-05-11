/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import org.jmol.api.JmolEdge;
import org.jmol.api.JmolNode;

/**
 * Bond in a SmilesMolecule
 */
public class SmilesBond implements JmolEdge {

  // Bond orders
  public final static int TYPE_UNKNOWN = -1;
  public final static int TYPE_NONE = 0;
  public final static int TYPE_SINGLE = 1;
  public final static int TYPE_DOUBLE = 2;
  public final static int TYPE_TRIPLE = 3;
  public final static int TYPE_AROMATIC = 4;
  public final static int TYPE_DIRECTIONAL_1 = 5;
  public final static int TYPE_DIRECTIONAL_2 = 6;
  public final static int TYPE_RING_BOND = 7;
  public final static int TYPE_ANY = 8;

  private SmilesAtom atom1;
  private SmilesAtom atom2;
  private int bondType;
  
  /**
   * SmilesBond constructor
   * 
   * @param atom1 First atom
   * @param atom2 Second atom
   * @param bondType Bond type
   */
  public SmilesBond(SmilesAtom atom1, SmilesAtom atom2, int bondType) {
    this.atom1 = atom1;
    this.atom2 = atom2;
    this.bondType = bondType;
    if (atom2 != null)
      atom2.isFirst = false;
  }

  /**
   * @param code Bond code
   * @return Bond type
   */
  public static int getBondTypeFromCode(char code) {
    switch (code) {
    case '.':
      return TYPE_NONE;
    case '-':
      return TYPE_SINGLE;
    case '=':
      return TYPE_DOUBLE;
    case '#':
      return TYPE_TRIPLE;
    case ':':
      return TYPE_AROMATIC;
    case '/':
      return TYPE_DIRECTIONAL_1;
    case '\\':
      return TYPE_DIRECTIONAL_2;
    case '@':
      return TYPE_RING_BOND;
    case '~':
      return TYPE_ANY;
    }
    return TYPE_UNKNOWN;
  }

  public SmilesAtom getAtom1() {
    return atom1;
  }

  public void setAtom1(SmilesAtom atom) {
    this.atom1 = atom;
  }

  public SmilesAtom getAtom2() {
    return atom2;
  }

  public void setAtom2(SmilesAtom atom) {
    this.atom2 = atom;
    if (atom2 != null)
      atom2.isFirst = false;
  }

  public int getBondType() {
    return bondType;
  }

  public void setBondType(int bondType) {
    this.bondType = bondType;
  }
  
  public SmilesAtom getOtherAtom(SmilesAtom a) {
    return (atom1 == a ? atom2 : atom1);
  }

  public int getAtomIndex1() {
    return atom1.index;
  }

  public int getAtomIndex2() {
    return atom2.index;
  }

  public int getCovalentOrder() {
    return bondType;
  }

  public int getOrder() {
    return bondType;
  }

  public JmolNode getOtherAtom(JmolNode atom) {
    return (atom == atom1 ? atom2 : atom == atom2 ? atom1 : null);
  }

  public boolean isCovalent() {
    return true;
  }

  public int getValence() {
    return (bondType & 7);
  }
  
}
