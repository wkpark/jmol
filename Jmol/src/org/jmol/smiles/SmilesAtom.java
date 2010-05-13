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

import javax.vecmath.Point3f;

import org.jmol.api.JmolEdge;
import org.jmol.api.JmolNode;
import org.jmol.util.Elements;
//import org.jmol.util.Logger;

/**
 * This class represents an atom in a <code>SmilesMolecule</code>.
 */
public class SmilesAtom extends Point3f implements JmolNode {
  
  final static int CHIRALITY_DEFAULT = 0;
  final static int CHIRALITY_ALLENE = 2;
  final static int CHIRALITY_TETRAHEDRAL = 4;
  final static int CHIRALITY_TRIGONAL_BIPYRAMIDAL = 5;
  final static int CHIRALITY_OCTAHEDRAL = 6;
  final static int CHIRALITY_SQUARE_PLANAR = 8;
  
  static int getChiralityClass(String xx) {
    return ("0;11;AL;33;TH;TP;OH;77;SP;".indexOf(xx) + 1)/ 3;
  }

  static boolean allowSmilesUnbracketed(String xx) {
    return ("B, C, N, O, P, S, F, Cl, Br, I,".indexOf(xx + ",") >= 0);
  }
  
  int index;
  boolean not;
  boolean selected;
  boolean hasSymbol;
  boolean isFirst = true;
  
  short elementNumber = -2; // UNDEFINED (could be A or a or *)
  
  private short atomicMass = Short.MIN_VALUE;
  private int charge;
  int explicitHydrogenCount = Integer.MIN_VALUE;
  int implicitHydrogenCount = Integer.MIN_VALUE;
  private int matchingAtom = -1;
  private int chiralClass = Integer.MIN_VALUE;
  private int chiralOrder = Integer.MIN_VALUE;
  private boolean isAromatic;
  SmilesBond[] bonds = new SmilesBond[INITIAL_BONDS];
  private int bondsCount;

  public void setBonds(SmilesBond[] bonds) {
    this.bonds = bonds;
    bondsCount = bonds.length;
  }


  int iNested = 0;
  
  SmilesAtom[] atomsOr;
  int nAtomsOr;
  
  SmilesAtom[] primitives;
  int nPrimitives;
  
  public SmilesAtom addAtomOr() {
    if (atomsOr == null)
      atomsOr = new SmilesAtom[2];
    if (nAtomsOr >= atomsOr.length) {
      SmilesAtom[] tmp = new SmilesAtom[atomsOr.length * 2];
      System.arraycopy(atomsOr, 0, tmp, 0, atomsOr.length);
      atomsOr = tmp;
    }
    SmilesAtom sAtom = new SmilesAtom(index);
    atomsOr[nAtomsOr] = sAtom;
    nAtomsOr++;
    return sAtom;
  }

  public SmilesAtom addPrimitive() {
    if (primitives == null)
      primitives = new SmilesAtom[2];
    if (nPrimitives >= primitives.length) {
      SmilesAtom[] tmp = new SmilesAtom[primitives.length * 2];
      System.arraycopy(primitives, 0, tmp, 0, primitives.length);
      primitives = tmp;
    }
    SmilesAtom sAtom = new SmilesAtom(index);
    primitives[nPrimitives] = sAtom;
    setSymbol("*");
    hasSymbol = false;
    nPrimitives++;
    return sAtom;
  }

  
  public String toString() {
    return "[" + Elements.elementSymbolFromNumber(elementNumber)
    + index + (matchingAtom >= 0 ? "(" + matchingAtom + ")" : "")
//    + " ch:" + charge 
//    + " ar:" + isAromatic 
//    + " H:" + explicitHydrogenCount
//    + " h:" + implicitHydrogenCount
    + "]";
  }
  
  private final static int INITIAL_BONDS = 4;

  /**
   * Constructs a <code>SmilesAtom</code>.
   * 
   * @param index Atom number in the molecule. 
   */
  public SmilesAtom(int index) {
    this.index = index;
  }

  int component;
  int atomSite;
  
  public SmilesAtom(int iComponent, int ptAtom, int flags, short atomicNumber, int charge) {
    component = iComponent;
    index = ptAtom;
    this.atomSite = flags;
    this.elementNumber = atomicNumber;
    this.charge = charge;
  }

  /**
   * Finalizes the hydrogen count for implicit hydrogens in a <code>SmilesMolecule</code>.
   * 
   * @param molecule Molecule containing the atom.
   * @return false if inappropriate
   */
  public boolean setHydrogenCount(SmilesSearch molecule) {
    // only called for SMILES search -- simple C or [C]
    if (explicitHydrogenCount != Integer.MIN_VALUE)
      return true;
    // Determining max count
  	int count = 0;
  	  // not a complete set...
  	  // B, C, N, O, P, S, F, Cl, Br, and I
  	  // B (3), C (4), N (3,5), O (2), P (3,5), S (2,4,6), and 1 for the halogens
  	  
  	  switch (elementNumber) {
  	  default:
  	    return false;
  	  case 0:
  	  case -1: // A a
  	    return true;
      case 6: // C
        count = (isAromatic ? 3 : 4);
        break;
      case 8: // O
      case 16: // S
        count = 2;
        break;
      case 5:  // B
      case 7:  // N
      case 15: // P
        count = 3;
        break;
      case 9: // F
      case 17: // Cl
      case 35: // Br
      case 53: // I
        count = 1;
        break;
  	  }
  	  
      for (int i = 0; i < bondsCount; i++) {
        SmilesBond bond = bonds[i];
        switch (bond.getBondType()) {
        case SmilesBond.TYPE_ANY: // for aromatics
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
      
      if (count > 0)
        explicitHydrogenCount = count;
      return true;
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
   * @return  false if invalid symbol
   */
  public boolean setSymbol(String symbol) {
    isAromatic = symbol.equals(symbol.toLowerCase()); // BH added
    hasSymbol = true;
    if (symbol.equals("*")) {
      isAromatic = false;
      elementNumber = -2;
      return true;
    }
    if (symbol.equals("a") || symbol.equals("A")) {
      elementNumber = -1;
      return true;
    }
    if (symbol.equals("Xx")) {
      elementNumber = 0;
      return true;
    }
    
    if (isAromatic)
      symbol = symbol.substring(0, 1).toUpperCase() 
          + (symbol.length() == 1 ? "" : symbol.substring(1));
    elementNumber = Elements.elementNumberFromSymbol(symbol, true);
    return (elementNumber != 0);
  }

  /**
   *  Returns the atomic number of the element or 0
   * 
   * @return atomicNumber
   */
  public short getElementNumber() {
    return elementNumber;
  }

  /**
   * Returns the atomic mass of the atom.
   * 
   * @return Atomic mass.
   */
  public short getAtomicMass() {
    return atomicMass;
  }

  /**
   * Sets the atomic mass of the atom.
   * 
   * @param mass Atomic mass.
   */
  public void setAtomicMass(int mass) {
    atomicMass = (short) mass;
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
  public int getChiralClass() {
    return chiralClass;
  }

  /**
   * Sets the chiral class of the atom.
   * (see <code>CHIRALITY_...</code> constants)
   * 
   * @param chiralClass Chiral class.
   */
  public void setChiralClass(int chiralClass) {
    this.chiralClass = chiralClass;
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
   * Sets the number of explicit hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setExplicitHydrogenCount(int count) {
    if (count == 2)
      System.out.println("hmm");

    explicitHydrogenCount = count;
  }

  /**
   * Sets the number of implicit hydrogen atoms bonded with this atom.
   * 
   * @param count Number of hydrogen atoms.
   */
  public void setImplicitHydrogenCount(int count) {
    implicitHydrogenCount = count;
  }

  /**
   * Returns the number of bonds of this atom.
   * 
   * @return Number of bonds.
   */
  public int getCovalentBondCount() {
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
    //if (Logger.debugging)
      //Logger.debug("adding bond to " + this + ": " + bond.getAtom1() + " " + bond.getAtom2());
    bonds[bondsCount] = bond;
    bondsCount++;
  }

  public int getMatchingBondedAtom(int i) {
    if (i >= bondsCount)
      return -1;
    SmilesBond b = bonds[i];
    return (b.getAtom1() == this ? b.getAtom2() : b.getAtom1()).matchingAtom;
  }



  int degree = -1;
  public void setDegree(int degree) {
    this.degree = degree;
  }

  int valence = -1;
  public void setValence(int valence) {
    this.valence = valence;
  }

  int connectivity = -1;
  public void setConnectivity(int connectivity) {
    this.connectivity = connectivity;
  }

  int ringMembership = -1;
  public void setRingMembership(int rm) {
    ringMembership = rm;
  }

  int ringSize = -1;
  public void setRingSize(int rs) {
    ringSize = rs;
  }

  int ringConnectivity = -1;
  public void setRingConnectivity(int rc) {
    ringConnectivity = rc;
  }

  public int getModelIndex() {
    return component;
  }

  public JmolEdge[] getEdges() {
    return bonds;
  }

  public int getAtomSite() {
    return atomSite;
  }

  public int getBondedAtomIndex(int j) {
    return bonds[j].getOtherAtom(this).index;
  }

  public int getCovalentHydrogenCount() {
    int n = 0;
    for (int k = 0; k < bonds.length; k++)
      if (bonds[k].getOtherAtom(this).elementNumber == 1)
        n++;
    return n;
  }

  public int getImplicitHydrogenCount() {
    // searching a SMILES string all H atoms will 
    // be explicitly defined
    return 0;
  }

  public int getFormalCharge() {
    return charge;
  }

  public short getIsotopeNumber() {
    return atomicMass;
  }

  public int getValence() {
    int n = valence;
    if (n < 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    valence = n;
    return n;
  }

  public SmilesBond getBondTo(SmilesAtom atom) {
    for (int k = 0; k < bonds.length; k++)
      if (bonds[k] != null && bonds[k].getOtherAtom(this) == atom)
      return bonds[k];
    return null;
  }


}
