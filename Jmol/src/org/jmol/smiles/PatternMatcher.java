/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-26 16:57:51 -0500 (Thu, 26 Apr 2007) $
 * $Revision: 7502 $
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

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.viewer.JmolConstants;

/**
 * A class to match a SMILES pattern with a Jmol molecule.
 * <p>
 * The SMILES specification can been found at the
 * <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * <p>
 * An example on how to use it:
 * <pre><code>
 * PatternMatcher matcher = new PatternMatcher(modelSet);
 * try {
 *   BitSet bitSet = matcher.getSubstructureSet(smilesString);
 *   // Use bitSet...
 * } catch (InvalidSmilesException e) {
 *   // Exception management
 * }
 * </code></pre>
 * 
 * @author Nicolas Vervelle
 * @see org.jmol.smiles.SmilesMolecule
 */
public class PatternMatcher implements SmilesMatcherInterface {

  /**
   * 
   * searches for matches of smiles1 within smiles2
   * 
   * @param smiles1
   * @param smiles2
   * @return            number of occurances of smiles1 within smiles2
   */
  public int find(String smiles1, String smiles2) {
    BitSet[] list = null;
    try {
      // create a topological model set from smiles2
      SmilesMolecule pattern = (new SmilesParser()).parseSmiles(smiles2);
      int atomCount = pattern.getAtomsCount();
      Atom[] atoms = new Atom[atomCount];
      for (int i = 0; i < atomCount; i++) {
        SmilesAtom sAtom = pattern.getAtom(i);
        Atom atom = atoms[i] = new Atom(0, i, 0, 0, 0, 0, null, 0, 
            sAtom.getAtomicNumber(), sAtom.getCharge(), false, '\0', '\0');
        atom.setBonds(new Bond[sAtom.getBondsCount()]);
      }
      int[] bondCounts = new int[atomCount];
      for (int i = pattern.getBondsCount(); --i >= 0; ) {
        SmilesBond sBond = pattern.getBond(i);
        int order = 1;
        switch (sBond.getBondType()) {
        case SmilesBond.TYPE_UNKNOWN:
        case SmilesBond.TYPE_NONE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
        case SmilesBond.TYPE_SINGLE:
          order = JmolConstants.BOND_COVALENT_SINGLE;
          break;
        case SmilesBond.TYPE_AROMATIC:
          order = JmolConstants.BOND_AROMATIC_SINGLE;
          break;
        case SmilesBond.TYPE_DOUBLE:
          order = JmolConstants.BOND_COVALENT_DOUBLE;
          break;
        case SmilesBond.TYPE_TRIPLE:
          order = JmolConstants.BOND_COVALENT_TRIPLE;
          break;
        }
        int i1 = sBond.getAtom1().getIndex();
        int i2 = sBond.getAtom2().getIndex();
        Atom atom1 = (Atom) atoms[i1];
        Atom atom2 = (Atom) atoms[i2];
        Bond b = new Bond(atom1, atom2, 
            order, (short) 0, (short) 0);
        atom1.bonds[bondCounts[i1]++] = b;
        atom2.bonds[bondCounts[i2]++] = b;
      }
      list = getSubstructureSetArray(smiles1, atoms, atomCount, null, null, null);
      return list.length;
    } catch (Exception e) {
      return 0;
    }
  }
  

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @param atoms 
   * @param atomCount 
   * @return BitSet indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  
  public BitSet getSubstructureSet(String smiles, Atom[] atoms, int atomCount) throws Exception {
    SmilesMolecule pattern = SmilesParser.getMolecule(smiles);
    pattern.jmolAtoms = atoms;
    pattern.jmolAtomCount = atomCount;
    BitSet bsSubstructure = new BitSet();
    search(bsSubstructure, pattern);
    return bsSubstructure;
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @param bsSelected 
   * @param atoms 
   * @param atomCount 
   * @param bsRequired 
   * @param bsNot 
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  public BitSet[] getSubstructureSetArray(String smiles, Atom[] atoms, int atomCount, 
                                          BitSet bsSelected, 
                                          BitSet bsRequired, BitSet bsNot)
      throws Exception {
    SmilesMolecule pattern = SmilesParser.getMolecule(smiles);
    pattern.bsSelected = bsSelected;
    pattern.bsRequired = (bsRequired != null && bsRequired.cardinality() > 0 ? bsRequired : null);
    pattern.bsNot = bsNot;
    pattern.jmolAtoms = atoms;
    pattern.jmolAtomCount = atomCount;
    Vector vSubstructures = new Vector();
    search(vSubstructures, pattern);
    BitSet[] bitsets = new BitSet[vSubstructures.size()];
    for (int i = 0; i < bitsets.length; i++)
      bitsets[i] = (BitSet) vSubstructures.get(i);
    return bitsets;
  }

  private void search(Object ret, SmilesMolecule pattern) {
    for (int i = 0; i < pattern.jmolAtomCount; i++) {
      searchMatch(ret, pattern, pattern.getAtom(0), 0, i);
    }
  }

  /**
   * Recursively search matches.
   * 
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param pattern
   *          SMILES pattern.
   * @param atomNum
   *          Current atom of the pattern.
   */
  private void searchMatch(Object ret, SmilesMolecule pattern, int atomNum) {
    // consider a specific pattern atom...
    SmilesAtom patternAtom = pattern.getAtom(atomNum);
    // for all the pattern bonds for this atom...
      for (int i = 0; i < patternAtom.getBondsCount(); i++) {
        SmilesBond patternBond = patternAtom.getBond(i);
        // find the one bond to atoms already assigned
        // note that it must be there, because SMILES strings
        // are parsed in order, from left to right. You can't
        // have two fragments going at the same time.
        if (patternBond.getAtom2() == patternAtom) {
          // run through the bonds of that assigned atom
          Atom atom = pattern.jmolAtoms[patternBond.getAtom1().getMatchingAtom()];
          Bond[] bonds = atom.getBonds();
          // now run through all the bonds looking for atoms that might match
          if (bonds != null)
            for (int j = 0; j < bonds.length; j++)
              searchMatch(ret, pattern, patternAtom, atomNum, atom.getBondedAtomIndex(j));
          return;
        }
      }
  }
  
  /**
   * Recursively search matches.
   * 
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param pattern
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
   */
  private void searchMatch(Object ret, SmilesMolecule pattern,  
                           SmilesAtom patternAtom, int atomNum, int i) {
    
    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = pattern.getAtom(j);
      if (previousAtom.getMatchingAtom() == i) {
        return;
      }
    }
    Atom atom = pattern.jmolAtoms[i];

    // Check atomic number
    int targetAtomicNumber = patternAtom.getAtomicNumber();
    if (targetAtomicNumber != 0 && targetAtomicNumber != (atom.getElementNumber()))
      return;
    int targetMass = patternAtom.getAtomicMass();
    if (targetMass > 0 && targetMass != atom.getIsotopeNumber()) {
      // smiles indicates [13C] or [12C]
      // must match perfectly -- [12C] matches only explicit C-12, not
      // "unlabeled" C
        return;
    }
    // Check charge
    if (patternAtom.getCharge() != atom.getFormalCharge())
      return;

    // Check bonds

    Bond[] bonds = atom.getBonds();
    
    /* the JME test for aromatic carbon is simply that the atom
     * is carbon and is in a ring and is double bonded or aromatic-bonded
     * nothing more than that
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
        return;
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
        bondFound = false;
        if (matchAnyBond) {
          // disregard bond type when aromatic -- could be single, double, or aromatic
          bondFound = true;
          break;
        }
        if (!bonds[k].isCovalent())
          continue;
        int order = bonds[k].getCovalentOrder();
        switch (patternBond.getBondType()) {
        case SmilesBond.TYPE_SINGLE:
        case SmilesBond.TYPE_DIRECTIONAL_1:
        case SmilesBond.TYPE_DIRECTIONAL_2:
          if (order == JmolConstants.BOND_COVALENT_SINGLE) {
            bondFound = true;
          }
          break;
        case SmilesBond.TYPE_DOUBLE:
          if (order == JmolConstants.BOND_COVALENT_DOUBLE) {
            bondFound = true;
          }
          break;
        case SmilesBond.TYPE_TRIPLE:
          if (order == JmolConstants.BOND_COVALENT_TRIPLE) {
            bondFound = true;
          }
          break;
        case SmilesBond.TYPE_AROMATIC: // not implemented
          if ((order & JmolConstants.BOND_AROMATIC_MASK) != 0) {
            bondFound = true;
          }
          break;
        case SmilesBond.TYPE_UNKNOWN:
          bondFound = true;
          break;
        }
        if (!bondFound)
          return;
      }
    }

    // add this atom to the growing list
    patternAtom.setMatchingAtom(i);
/*
    for (int k = 0; k <= atomNum; k++) {
      System.out.print("-" + pattern.getAtom(k).getMatchingAtom());
    }
    System.out.println(" " + (atomNum+1) + "/" + pattern.getAtomsCount());
*/    
    if (atomNum + 1 < pattern.getAtomsCount()) {
      searchMatch(ret, pattern, atomNum + 1);
    } else {
      BitSet bs;
      if (ret instanceof BitSet) {
        bs = (BitSet) ret;
      } else {
        bs = new BitSet();
      }
      for (int k = 0; k < pattern.getAtomsCount(); k++) {
        SmilesAtom matching = pattern.getAtom(k);
        bs.set(matching.getMatchingAtom());
      }
      if (ret instanceof Vector) {
        Vector v = (Vector) ret;
        boolean isOK = true;
        if (pattern.bsNot != null && pattern.bsNot.intersects(bs))
          isOK = false;
        else if (pattern.bsRequired != null && !pattern.bsRequired.intersects(bs))
          isOK = false;
        else if (pattern.bsSelected != null)
          for (int j = bs.nextSetBit(0); j >= 0 && isOK; j = bs
              .nextSetBit(j + 1))
            isOK = pattern.bsSelected.get(j);
        for (int j = v.size(); --j >= 0 && isOK;)
          isOK = !(((BitSet) v.get(j)).equals(bs));
        if (isOK)
          v.add(bs);
      }
    }
    patternAtom.setMatchingAtom(-1);
  }
  
}
