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

import org.jmol.viewer.JmolConstants;

/**
 * This class represents an atom in a <code>SmilesMolecule</code>.
 */
public class SmilesAtom {

  private int index;
  private short atomicNumber = -1;
  private int atomicMass = Integer.MIN_VALUE;
  private int charge;
  private int hydrogenCount = Integer.MIN_VALUE;
  private int matchingAtom = -1;
  private String chiralClass;
  private int chiralOrder = Integer.MIN_VALUE;
  private boolean isAromatic;
  private SmilesBond[] bonds = new SmilesBond[INITIAL_BONDS];
  private int bondsCount;

  private final static int INITIAL_BONDS = 4;

  
  /**
   * Constant used for default chirality.
   */
  public final static String DEFAULT_CHIRALITY = "";
  /**
   * Constant used for Allene chirality.
   */
  public final static String CHIRALITY_ALLENE = "AL";
  /**
   * Constant used for Octahedral chirality.
   */
  public final static String CHIRALITY_OCTAHEDRAL = "OH";
  /**
   * Constant used for Square Planar chirality.
   */
  public final static String CHIRALITY_SQUARE_PLANAR = "SP";
  /**
   * Constant used for Tetrahedral chirality.
   */
  public final static String CHIRALITY_TETRAHEDRAL = "TH";
  /**
   * Constant used for Trigonal Bipyramidal chirality.
   */
  public final static String CHIRALITY_TRIGONAL_BIPYRAMIDAL = "TB";

  /**
   * Constructs a <code>SmilesAtom</code>.
   * 
   * @param index Atom number in the molecule. 
   */
  public SmilesAtom(int index) {
    this.index = index;
  }

  /**
   * Creates missing hydrogen atoms in a <code>SmilesMolecule</code>.
   * 
   * @param molecule Molecule containing the atom.
   */
  public void createMissingHydrogen(SmilesMolecule molecule) {
  	// Determining max count
  	int count = 0;
  	if (hydrogenCount == Integer.MIN_VALUE) {
  	  // not a complete set...
  	  switch (atomicNumber) {
  	  case -1:
  	    break;
  	  case 1: // H
      case 9: // F
      case 17: // Cl
      case 35: // Br
      case 53: // At
      case 85: // I
        count = 1;
        break;
      case 8: // O
      case 16: // S
      case 34: // Se
      case 52: // Te
        count = 2;
        break;
      case 5:  // B
      case 7:  // N
      case 13: // Al
      case 15: // P
      case 33: // As
        count = 3;
        break;
      case 6: // C
        count = (isAromatic ? 3 : 4);
        break;
  	  }

  	  //System.out.println(" assigning " + count + " valence to atom " + number + " " + atomicNumber);

      for (int i = 0; i < bondsCount; i++) {
        SmilesBond bond = bonds[i];
        switch (bond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
          count -= 1;
          break;
        case SmilesBond.TYPE_DOUBLE:
          count -= 2;
          break;
        case SmilesBond.TYPE_TRIPLE:
          count -= 3;
          break;
        }
      }
  	} else {
  	  count = hydrogenCount;
  	}

    // Adding hydrogens
    //System.out.println(" adding " + count + " H atoms to atom " + number + " " + symbol);
    for (int i = 0; i < count; i++) {
      SmilesAtom hydrogen = molecule.createAtom();
      hydrogen.setAtomicNumber((short) 1);
      molecule.createBond(this, hydrogen, SmilesBond.TYPE_SINGLE);
    }
  }

  /**
   * Returns the atom index of the atom.
   * 
   * @return Atom index.
   */
  public int getIndex() {
    return index;
  }

  /**
   * 
   * @return whether symbol was lower case
   */
  public boolean isAromatic() {
    return isAromatic;
  }

  /**
   * Sets the symbol of the atm.
   * 
   * @param symbol Atom symbol.
   */
  public void setSymbol(String symbol) {
    if (symbol.equals("*")) {
      atomicNumber = 0;
      return;
    }
    isAromatic = symbol.equals(symbol.toLowerCase()); // BH added
    if (isAromatic)
      symbol = symbol.substring(0, 1).toUpperCase() 
          + (symbol.length() == 1 ? "" : symbol.substring(1));
    atomicNumber = JmolConstants.elementNumberFromSymbol(symbol);
  }

  /**
   *  Returns the atomic number of the element or 0
   * 
   * @return atomicNumber
   */
  public short getAtomicNumber() {
    return atomicNumber;
  }

  private void setAtomicNumber(short i) {
    atomicNumber = i;
  }

  /**
   * Returns the atomic mass of the atom.
   * 
   * @return Atomic mass.
   */
  public int getAtomicMass() {
    return atomicMass;
  }

  /**
   * Sets the atomic mass of the atom.
   * 
   * @param mass Atomic mass.
   */
  public void setAtomicMass(int mass) {
    this.atomicMass = mass;
  }
  
  /**
   * Returns the charge of the atom.
   * 
   * @return Charge.
   */
  public int getCharge() {
    return charge;
  }

  /**
   * Sets the charge of the atom.
   * 
   * @param charge Charge.
   */
  public void setCharge(int charge) {
    this.charge = charge;
  }

  /**
   * Returns the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * 
   * @return matching atom.
   */
  public int getMatchingAtom() {
    return matchingAtom;
  }

  /**
   * Sets the number of a matching atom in a molecule.
   * This value is temporary, it is used during the pattern matching algorithm.
   * 
   * @param atom Temporary: number of a matching atom in a molecule.
   */
  public void setMatchingAtom(int atom) {
    this.matchingAtom = atom;
  }

  /**
   * Returns the chiral class of the atom.
   * (see <code>CHIRALITY_...</code> constants)
   * 
   * @return Chiral class.
   */
  public String getChiralClass() {
    return chiralClass;
  }

  /**
   * Sets the chiral class of the atom.
   * (see <code>CHIRALITY_...</code> constants)
   * 
   * @param chiralClass Chiral class.
   */
  public void setChiralClass(String chiralClass) {
    this.chiralClass = (chiralClass != null) ? chiralClass.intern() : null;
  }

  /**
   * Returns the chiral order of the atom.
   * 
   * @return Chiral order.
   */
  public int getChiralOrder() {
    return chiralOrder;
  }

  /**
   * Sets the chiral order of the atom.
   * 
   * @param chiralOrder Chiral order.
   */
  public void setChiralOrder(int chiralOrder) {
    this.chiralOrder = chiralOrder;
  }

  /**
   * Returns the number of hydrogen atoms bonded with this atom.
   * 
   * @return Number of hydrogen atoms.
   */
  public int getHydrogenCount() {
    return hydrogenCount;
  }

  /**
   * Sets the number of hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setHydrogenCount(int count) {
    this.hydrogenCount = count;
  }

  /**
   * Returns the number of bonds of this atom.
   * 
   * @return Number of bonds.
   */
  public int getBondsCount() {
    return bondsCount;
  }

  /**
   * Returns the bond at index <code>number</code>.
   * 
   * @param number Bond number.
   * @return Bond.
   */
  public SmilesBond getBond(int number) {
    if ((number >= 0) && (number < bondsCount)) {
      return bonds[number];
    }
    return null;
  }
  
  /**
   * Add a bond to the atom.
   * 
   * @param bond Bond to add.
   */
  public void addBond(SmilesBond bond) {
    if (bondsCount >= bonds.length) {
      SmilesBond[] tmp = new SmilesBond[bonds.length * 2];
      System.arraycopy(bonds, 0, tmp, 0, bonds.length);
      bonds = tmp;
    }
    bonds[bondsCount] = bond;
    bondsCount++;
  }
}
