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
 * Atom in a SmilesMolecule
 */
public class SmilesAtom {

  private int number;
  private String symbol;
  private Integer atomicMass;
  private int charge;
  private Integer hydrogenCount;

  private SmilesBond[] bonds;
  private int bondsCount;

  private final static int INITIAL_BONDS = 4;

  /**
   * SmilesAtom constructor
   * 
   * @param number Atom number in the molecule 
   */
  public SmilesAtom(int number) {
    this.number = number;
    this.symbol = null;
    this.atomicMass = null;
    this.charge = 0;
    bonds = new SmilesBond[INITIAL_BONDS];
    bondsCount = 0;
  }

  /**
   * Creates missing hydrogen
   * 
   * @param molecule Molecule containing the atom
   */
  public void createMissingHydrogen(SmilesMolecule molecule) {
  	// Determing max count
  	int count = 0;
  	if (hydrogenCount == null) {
      if (symbol != null) {
        if (symbol.equals("B")) {
          count = 3;
        } else if (symbol.equals("Br")) {
          count = 1;
        } else if (symbol.equals("C")) {
          count = 4;
        } else if (symbol.equals("Cl")) {
          count = 1;
        } else if (symbol.equals("F")) {
          count = 1;
        } else if (symbol.equals("I")) {
          count = 1;
        } else if (symbol.equals("N")) {
          count = 3;
        } else if (symbol.equals("O")) {
          count = 2;
        } else if (symbol.equals("P")) {
          count = 3;
        } else if (symbol.equals("S")) {
          count = 2;
        }
      }
      for (int i = 0; i < bondsCount; i++) {
        SmilesBond bond = bonds[i];
        switch (bond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
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
  	  count = hydrogenCount.intValue();
  	}

    // Adding hydrogens
    for (int i = 0; i < count; i++) {
      SmilesAtom hydrogen = molecule.createAtom();
      molecule.createBond(this, hydrogen, SmilesBond.TYPE_SINGLE);
      hydrogen.setSymbol("H");
    }
  }

  public int getNumber() {
    return number;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Integer getAtomicMass() {
    return atomicMass;
  }

  public void setAtomicMass(Integer mass) {
    this.atomicMass = mass;
  }

  public int getCharge() {
    return charge;
  }

  public void setCharge(int charge) {
    this.charge = charge;
  }

  public Integer getHydrogenCount() {
    return hydrogenCount;
  }

  public void setHydrogenCount(Integer count) {
    this.hydrogenCount = count;
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
  
  public void addBond(SmilesBond bond) {
    if (bondsCount >= bonds.length) {
      SmilesBond[] tmp = new SmilesBond[bonds.length * 2];
      for (int i = 0; i < bonds.length; i++) {
        tmp[i] = bonds[i];
      }
      bonds = tmp;
    }
    bonds[bondsCount] = bond;
    bondsCount++;
  }
}
