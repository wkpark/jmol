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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.smiles;

/**
 * Bond in a SmilesMolecule
 */
public class SmilesBond {

  // Bond orders
  public final static int TYPE_UNKOWN = -1;
  public final static int TYPE_SINGLE = 1;
  public final static int TYPE_DOUBLE = 2;
  public final static int TYPE_TRIPLE = 3;
  public final static int TYPE_AROMATIC = 4;

  // Bond expressions
  public final static char CODE_SINGLE = '-';
  public final static char CODE_DOUBLE = '=';
  public final static char CODE_TRIPLE = '#';
  public final static char CODE_AROMATIC = ':';

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
  }

  /**
   * @param code Bond code
   * @return Bond type
   */
  public static int getBondTypeFromCode(char code) {
    switch (code) {
    case CODE_SINGLE:
      return TYPE_SINGLE;
    case CODE_DOUBLE:
      return TYPE_DOUBLE;
    case CODE_TRIPLE:
      return TYPE_TRIPLE;
    case CODE_AROMATIC:
      return TYPE_AROMATIC;
    }
    return TYPE_UNKOWN;
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
  }

  public int getBondType() {
    return bondType;
  }

  public void setBondType(int bondType) {
    this.bondType = bondType;
  }
}
