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
 * Molecule created from a SMILES String
 */
public class SmilesMolecule {

  private SmilesAtom[] atoms;
  private int atomsCount;
  private SmilesBond[] bonds;
  private int bondsCount;

  private final static int INITIAL_ATOMS = 16;
  private final static int INITIAL_BONDS = 16;
  
  /**
   * SmilesMolecule constructor  
   */
  public SmilesMolecule() {
    atoms = new SmilesAtom[INITIAL_ATOMS];
    atomsCount = 0;
    bonds = new SmilesBond[INITIAL_BONDS];
    bondsCount = 0;
  }
  
  /* ============================================================= */
  /*                             Atoms                             */
  /* ============================================================= */

  public SmilesAtom createAtom() {
    if (atomsCount >= atoms.length) {
      SmilesAtom[] tmp = new SmilesAtom[atoms.length * 2];
      for (int i = 0; i < atoms.length; i++) {
        tmp[i] = atoms[i];
      }
      atoms = tmp;
    }
    SmilesAtom atom = new SmilesAtom(atomsCount);
    atoms[atomsCount] = atom;
    atomsCount++;
    return atom;
  }

  public int getAtomsCount() {
    return atomsCount;
  }

  public SmilesAtom getAtom(int number) {
    if ((number >= 0) && (number < atomsCount)) {
      return atoms[number];
    }
    return null;
  }
  
  /* ============================================================= */
  /*                             Bonds                             */
  /* ============================================================= */

  public SmilesBond createBond(
      SmilesAtom atom1,
      SmilesAtom atom2,
      int bondType) {
    if (bondsCount >= bonds.length) {
      SmilesBond[] tmp = new SmilesBond[bonds.length * 2];
      for (int i = 0; i < bonds.length; i++) {
        tmp[i] = bonds[i];
      }
      bonds = tmp;
    }
    SmilesBond bond = new SmilesBond(atom1, atom2, bondType);
    bonds[bondsCount] = bond;
    bondsCount++;
    if (atom1 != null) {
      atom1.addBond(bond);
    }
    if (atom2 != null) {
      atom2.addBond(bond);
    }
    return bond;
  }

  public int getBondsCount() {
    return bondsCount;
  }

  public SmilesBond getBond(int number) {
    if ((number >= 0) && (number < bondsCount)) {
      return bonds[number];
    }
    return null;
  }
}
