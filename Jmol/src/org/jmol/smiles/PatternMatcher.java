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
import org.jmol.modelset.ModelSet;
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

  private int atomCount;
  private Atom[] atoms;
  private BitSet bsSelected;
  private BitSet bsRequired;
  private BitSet bsNot;

  /**
   * Constructs a <code>PatternMatcher</code>.
   * 
   */
  public PatternMatcher() {
  }
  public void setModelSet(ModelSet modelSet) {
    atoms = modelSet.atoms;
    atomCount = (modelSet == null ? 0 : modelSet.getAtomCount());     
  }
  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @return BitSet indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  public BitSet getSubstructureSet(String smiles) throws Exception {
    SmilesParser parser = new SmilesParser();
    SmilesMolecule pattern = parser.parseSmiles(smiles);
    return getSubstructureSet(pattern);
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @param bsSelected 
   * @param bsRequired 
   * @param bsNot 
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  public BitSet[] getSubstructureSetArray(String smiles, BitSet bsSelected,
                                          BitSet bsRequired, BitSet bsNot)
      throws Exception {
    this.bsSelected = bsSelected;
    this.bsRequired = (bsRequired != null && bsRequired.cardinality() > 0 ? bsRequired : null);
    this.bsNot = bsNot;
    SmilesParser parser = new SmilesParser();
    SmilesMolecule pattern = parser.parseSmiles(smiles);
    return getSubstructureSetArray(pattern);
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param pattern SMILES pattern.
   * @return BitSet Array indicating which atoms match the pattern.
   */
  public BitSet getSubstructureSet(SmilesMolecule pattern) {
    BitSet bsSubstructure = new BitSet();
    searchMatch(bsSubstructure, pattern, 0);
    return bsSubstructure;
  }

  /**
   * Returns a vector of bitsets indicating which atoms match the pattern.
   * 
   * @param pattern SMILES pattern.
   * @return BitSet Array indicating which atoms match the pattern.
   */
  public BitSet[] getSubstructureSetArray(SmilesMolecule pattern) {
    Vector vSubstructures = new Vector();
    searchMatch(vSubstructures, pattern, 0);
    BitSet[] bitsets = new BitSet[vSubstructures.size()];
    for (int i = 0; i < bitsets.length; i++)
      bitsets[i] = (BitSet) vSubstructures.get(i);
    bsSelected = bsRequired = bsNot = null;
    return bitsets;
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
    if (atomNum > 0)
      for (int i = 0; i < patternAtom.getBondsCount(); i++) {
        SmilesBond patternBond = patternAtom.getBond(i);
        // if the SECOND atom in the bond is this atom...
        if (patternBond.getAtom2() == patternAtom) {
          // get the matching atom for the FIRST atom
          int matchingAtom = patternBond.getAtom1().getMatchingAtom();
          // get all the bonds for the corresponding Jmol model atom
          Atom atom = atoms[matchingAtom];
          Bond[] bonds = atom.getBonds();
          if (bonds != null) {
            // for all these bonds, if either of the
            // bonded atoms is the FIRST atom,
            for (int j = 0; j < bonds.length; j++) {
              if (bonds[j].getAtomIndex1() == matchingAtom) {
                searchMatch(ret, pattern, patternAtom, atomNum, bonds[j]
                    .getAtomIndex2());
              } else if (bonds[j].getAtomIndex2() == matchingAtom) {
                searchMatch(ret, pattern, patternAtom, atomNum, bonds[j]
                    .getAtomIndex1());
              }
            }
          }
          return;
        }
      }
    else
      for (int i = 0; i < atomCount; i++) {
        searchMatch(ret, pattern, patternAtom, atomNum, i);
      }
  }
  
  /**
   * Recursively search matches.
   * 
   * @param ret
   *          Resulting BitSet or Vector(BitSet).
   * @param pattern
   *          SMILES pattern.
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
    // Check that an atom is not used twice
    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = pattern.getAtom(j);
      if (previousAtom.getMatchingAtom() == i) {
        return;
      }
    }

    Atom atom = atoms[i];

    // Check symbol -- not isotope-sensitive and not case sensitive
    String targetSym = patternAtom.getSymbol();
    if (targetSym != "*"
        && !targetSym.equalsIgnoreCase(atom.getElementSymbol(false)))
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
    if (isAromatic && targetSym == "c") {
      // for aromatic carbon specifically, match any atom 
      // that is either doubly bonded or aromatic bonded 
      for (int k = 0; k < bonds.length; k++) {
        int order = bonds[k].getOrder();
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
        int order = bonds[k].getOrder();
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
        case SmilesBond.TYPE_UNKOWN:
          bondFound = true;
          break;
        }
        if (!bondFound)
          return;
      }
    }

    patternAtom.setMatchingAtom(i);
/*    
    for (int k = 0; k <= atomNum; k++) {
      System.out.print("-" + pattern.getAtom(k).getMatchingAtom());
    }
    System.out.println("");
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
        if (bsNot != null && bsNot.intersects(bs))
          isOK = false;
        else if (bsRequired != null && !bsRequired.intersects(bs))
          isOK = false;
        else if (bsSelected != null)
          for (int j = bs.nextSetBit(0); j >= 0 && isOK; j = bs
              .nextSetBit(j + 1))
            isOK = bsSelected.get(j);
        for (int j = v.size(); --j >= 0 && isOK;)
          isOK = !(((BitSet) v.get(j)).equals(bs));
        if (isOK)
          v.add(bs);
      }
    }
    patternAtom.setMatchingAtom(-1);
  }
}
