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

import java.util.BitSet;
import java.util.Vector;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;

/**
 *  -- was SmilesMolecule, 
 * but this now includes more data than that and the search itself
 * so as to keep this thread safe
 * 
 */
public class SmilesSearch {

  Atom[] jmolAtoms;
  int jmolAtomCount;
  BitSet bsSelected;
  BitSet bsRequired;
  BitSet bsNot;      
  boolean isAll;
  
  private final static int INITIAL_ATOMS = 16;
  private SmilesAtom[] atoms = new SmilesAtom[INITIAL_ATOMS];
  int patternAtomCount;
  boolean asVector;
  private Vector vReturn;
  private BitSet bsReturn = new BitSet();

    
  /* ============================================================= */
  /*                             Atoms                             */
  /* ============================================================= */

  public SmilesAtom createAtom() {
    if (patternAtomCount >= atoms.length) {
      SmilesAtom[] tmp = new SmilesAtom[atoms.length * 2];
      System.arraycopy(atoms, 0, tmp, 0, atoms.length);
      atoms = tmp;
    }
    SmilesAtom atom = new SmilesAtom(patternAtomCount);
    atoms[patternAtomCount] = atom;
    patternAtomCount++;
    return atom;
  }

  public int getPatternAtomCount() {
    return patternAtomCount;
  }

  public SmilesAtom getAtom(int number) {
    return atoms[number];
  }
  
  /* ============================================================= */
  /* Bonds */
  /* ============================================================= */

  public SmilesBond createBond(SmilesAtom atom1, SmilesAtom atom2, int bondType) {
    SmilesBond bond = new SmilesBond(atom1, atom2, bondType);
    if (atom1 != null)
      atom1.addBond(bond);
    if (atom2 != null)
      atom2.addBond(bond);
    return bond;
  }
  
  /** 
   * the start of the search. ret will be either a Vector or a BitSet
   * @return BitSet or Vector
   * 
   */
  Object search() {

    /*
     * The essence of the search process is as follows:
     * 
     * 1) From the pattern, create an ordered set of atoms connected by bonds.
     *    
     * 2) Try all model set atoms for position 0.
     * 
     * 3) For each atom that matches position N
     *    we move to position N+1 and run through all 
     *    of the pattern bonds TO this atom (atom in position 2).
     *    Those bonds will be to atoms that have already
     *    been assigned. There may be more than one of these
     *    if the atom is associated with a ring junction.
     *    
     *    We check that previously assigned model atom,
     *    looking at all of its bonded atoms to check for 
     *    a match for our N+1 atom. This works because if 
     *    this atom is going to work in this position, then 
     *    it must be bound to the atom assigned to position N
     *    
     *    There is no need to check more than one route to this
     *    atom in this position - if it is found to be good once,
     *    that is all we need, and if it is found to be bad once,
     *    that is all we need as well.
     *    
     */
    if (asVector)
      vReturn = new Vector();    
    for (int i = 0; i < jmolAtomCount; i++)
      if (!checkMatch(atoms[0], 0, i))
        break;
    return (asVector ? (Object) vReturn : bsReturn);
  }

  /**
   * Check for a specific match of a model set atom with a pattern position
   * 
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param search
   *          SMILES pattern.
   * @param atoms
   * @param atomCount
   * @param patternAtom
   *          Atom of the pattern that is currently tested.
   * @param atomNum
   *          Current atom of the pattern.
   * @param i
   *          Atom number of the atom that is currently tested to match
   *          <code>patternAtom</code>.
   * @return true to continue or false if oneOnly
   */
  private final boolean checkMatch(SmilesAtom patternAtom, int atomNum, int i) {

    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = atoms[j];
      if (previousAtom.getMatchingAtom() == i) {
        return true;
      }
    }
    Atom atom = jmolAtoms[i];

    // Check atomic number
    int targetAtomicNumber = patternAtom.getAtomicNumber();
    if (targetAtomicNumber != 0
        && targetAtomicNumber != (atom.getElementNumber()))
      return true;
    int targetMass = patternAtom.getAtomicMass();
    if (targetMass > 0 && targetMass != atom.getIsotopeNumber()) {
      // smiles indicates [13C] or [12C]
      // must match perfectly -- [12C] matches only explicit C-12, not
      // "unlabeled" C
      return true;
    }
    // Check charge
    if (patternAtom.getCharge() != atom.getFormalCharge())
      return true;

    // Check bonds

    Bond[] bonds = atom.getBonds();

    /*
     * the JME test for aromatic carbon is simply that the atom is carbon and is
     * in a ring and is double bonded or aromatic-bonded nothing more than that
     */
    boolean bondFound = false;
    boolean isAromatic = patternAtom.isAromatic();
    if (isAromatic && targetAtomicNumber == 6) {
      // for aromatic carbon specifically, match any atom
      // that is either doubly bonded or aromatic bonded
      for (int k = 0; k < bonds.length; k++) {
        int order = bonds[k].getCovalentOrder();
        if (order == JmolConstants.BOND_COVALENT_DOUBLE
            || (order & JmolConstants.BOND_AROMATIC_MASK) != 0) {
          bondFound = true;
          break;
        }
      }
      if (!bondFound)
        return true;
    }

    for (int j = 0; j < patternAtom.getBondsCount(); j++) {
      SmilesBond patternBond = patternAtom.getBond(j);
      // Check only if the current atom is the second atom of the bond
      if (patternBond.getAtom2() != patternAtom)
        continue;
      SmilesAtom atom1 = patternBond.getAtom1();
      int matchingAtom = atom1.getMatchingAtom();
      // we don't care what the bond is designated as for an aromatic atom.
      // That may seem strange, but it's true for aromatic carbon, as we
      // already know it is double- or aromatic-bonded.
      // for N, we assume it is attached to at least one aromatic atom,
      // and that is enough for us.
      boolean matchAnyBond = (isAromatic && atom1.isAromatic());
      for (int k = 0; k < bonds.length; k++) {
        if (bonds[k].getAtomIndex1() != matchingAtom
            && bonds[k].getAtomIndex2() != matchingAtom)
          continue;
        if (matchAnyBond) {
          // disregard bond type when aromatic -- could be single, double, or
          // aromatic
          bondFound = true;
          break;
        }
        bondFound = false;
        if (!bonds[k].isCovalent())
          continue;
        int order = bonds[k].getCovalentOrder();
        switch (patternBond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
          bondFound = (order == JmolConstants.BOND_COVALENT_SINGLE);
          break;
        case SmilesBond.TYPE_DOUBLE:
          bondFound = (order == JmolConstants.BOND_COVALENT_DOUBLE);
          break;
        case SmilesBond.TYPE_TRIPLE:
          bondFound = (order == JmolConstants.BOND_COVALENT_TRIPLE);
          break;
        case SmilesBond.TYPE_AROMATIC: // not implemented
          bondFound = ((order & JmolConstants.BOND_AROMATIC_MASK) != 0);
          break;
        case SmilesBond.TYPE_UNKNOWN:
          bondFound = true;
          break;
        }
        if (!bondFound)
          return true;
      }
    }

    // add this atom to the growing list
    patternAtom.setMatchingAtom(i);

    if (++atomNum < patternAtomCount) {
      // next position...
      patternAtom = atoms[atomNum];
      // for all the pattern bonds for this atom...
      for (int iAtom = 0; iAtom < patternAtom.getBondsCount(); iAtom++) {
        SmilesBond patternBond = patternAtom.getBond(iAtom);
        // find the bond to atoms already assigned
        // note that it must be there, because SMILES strings
        // are parsed in order, from left to right. You can't
        // have two fragments going at the same time.
        if (patternBond.getAtom2() == patternAtom) {
          // run through the bonds of that assigned atom
          atom = jmolAtoms[patternBond.getAtom1().getMatchingAtom()];
          bonds = atom.getBonds();
          // now run through all the bonds looking for atoms that might match
          // this is the iterative step
          if (bonds != null)
            for (int j = 0; j < bonds.length; j++)
              if (!checkMatch(patternAtom, atomNum, atom
                  .getBondedAtomIndex(j)))
                return false;
          break; // once through
        }
      }
    } else {
      boolean isOK = true;
      if (asVector) {
        bsReturn = new BitSet();
      }
      for (int k = 0; k < patternAtomCount; k++) {
        SmilesAtom matching = atoms[k];
        bsReturn.set(matching.getMatchingAtom());
      }
      if (asVector) {
        if (bsNot != null && bsNot.intersects(bsReturn))
          isOK = false;
        else if (bsRequired != null && !bsRequired.intersects(bsReturn))
          isOK = false;
        else if (bsSelected != null)
          for (int j = bsReturn.nextSetBit(0); j >= 0 && isOK; j = bsReturn
              .nextSetBit(j + 1))
            isOK = bsSelected.get(j);
        for (int j = vReturn.size(); --j >= 0 && isOK;)
          isOK = !(((BitSet) vReturn.get(j)).equals(bsReturn));
        if (isOK)
          vReturn.add(bsReturn);
      }
      if (Logger.debugging) {
        StringBuffer s = new StringBuffer();
        for (int k = 0; k < atomNum; k++) {
          s.append("-").append(atoms[k].getMatchingAtom());
        }
        s.append(" ").append(atomNum).append("/")
            .append(getPatternAtomCount());
        Logger.debug(s.toString());
        if (isOK)
          Logger.debug("match: " + Escape.escape(bsReturn));
      }
      if (!isAll || bsReturn.cardinality() == jmolAtomCount)
        return false;
    }
    patternAtom.setMatchingAtom(-1);
    return true;
  }  

}
