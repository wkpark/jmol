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
  private ModelSet modelSet;

  /**
   * Constructs a <code>PatternMatcher</code>.
   * 
   */
  public PatternMatcher() {
  }
  public void setModelSet(ModelSet modelSet) {
    this.modelSet = modelSet;
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
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  public BitSet[] getSubstructureSetArray(String smiles) throws Exception {
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
    return bitsets;
  }

  /**
   * Recursively search matches.
   * 
   * @param ret Resulting BitSet or Vector(BitSet).
   * @param pattern SMILES pattern.
   * @param atomNum Current atom of the pattern.
   */
  private void searchMatch(Object ret, SmilesMolecule pattern, int atomNum) {
    SmilesAtom patternAtom = pattern.getAtom(atomNum);
    for (int i = 0; i < patternAtom.getBondsCount(); i++) {
      SmilesBond patternBond = patternAtom.getBond(i);
      if (patternBond.getAtom2() == patternAtom) {
        int matchingAtom = patternBond.getAtom1().getMatchingAtom();
        Atom atom = modelSet.getAtomAt(matchingAtom);
        Bond[] bonds = atom.getBonds();
        if (bonds != null) {
          for (int j = 0; j < bonds.length; j++) {
            if (bonds[j].getAtomIndex1() == matchingAtom) {
              searchMatch(ret, pattern, patternAtom, atomNum, bonds[j].getAtomIndex2());
            }
            if (bonds[j].getAtomIndex2() == matchingAtom) {
              searchMatch(ret, pattern, patternAtom, atomNum, bonds[j].getAtomIndex1());
            }
          }
        }
        return;
      }
    }
    for (int i = 0; i < atomCount; i++) {
      searchMatch(ret, pattern, patternAtom, atomNum, i);
    }
  }
  
  /**
   * Recursively search matches.
   * 
   * @param ret Resulting BitSet or Vector(BitSet).
   * @param pattern SMILES pattern.
   * @param patternAtom Atom of the pattern that is currently tested.
   * @param atomNum Current atom of the pattern.
   * @param i Atom number of the atom that is currently tested to match <code>patternAtom</code>.
   */
  private void searchMatch(Object ret, SmilesMolecule pattern, SmilesAtom patternAtom, int atomNum, int i) {
    // Check that an atom is not used twice
    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = pattern.getAtom(j);
      if (previousAtom.getMatchingAtom() == i) {
        return;
      }
    }
    
    Atom atom = modelSet.getAtomAt(i);

    // Check symbol -- not isotope-sensitive
    String targetSym = patternAtom.getSymbol();
    int n = atom.getElementNumber();
    if (targetSym != "*" && targetSym != JmolConstants.elementSymbolFromNumber(n))
      return;
    
    int targetMass = patternAtom.getAtomicMass();
    if (targetMass > 0) {
      // smiles indicates [13C] or [12C]
      // must match perfectly -- [12C] matches only explicit C-12, not "unlabeled" C
      int isotopeMass = atom.getIsotopeNumber();
      if (isotopeMass != targetMass)
          return;
    }
    // Check charge
    if (patternAtom.getCharge() != atom.getFormalCharge())
      return;

    // Check bonds
    for (int j = 0; j < patternAtom.getBondsCount(); j++) {
      SmilesBond patternBond = patternAtom.getBond(j);
      // Check only if the current atom is the second atom of the bond
      if (patternBond.getAtom2() == patternAtom) {
        int matchingAtom = patternBond.getAtom1().getMatchingAtom();
        Bond[] bonds = atom.getBonds();
        boolean bondFound = false;
        for (int k = 0; k < bonds.length; k++) {
          if ((bonds[k].getAtomIndex1() == matchingAtom) ||
              (bonds[k].getAtomIndex2() == matchingAtom)) {
            switch (patternBond.getBondType()) {
            case SmilesBond.TYPE_AROMATIC:
              if ((bonds[k].getOrder() & JmolConstants.BOND_AROMATIC_MASK) != 0) {
                bondFound = true;
              }
              break;
            case SmilesBond.TYPE_DOUBLE:
              if ((bonds[k].getOrder() & JmolConstants.BOND_COVALENT_DOUBLE) != 0) {
                bondFound = true;
              }
              break;
            case SmilesBond.TYPE_SINGLE:
            case SmilesBond.TYPE_DIRECTIONAL_1:
            case SmilesBond.TYPE_DIRECTIONAL_2:
              if ((bonds[k].getOrder() & JmolConstants.BOND_COVALENT_SINGLE) != 0) {
                bondFound = true;
              }
              break;
            case SmilesBond.TYPE_TRIPLE:
              if ((bonds[k].getOrder() & JmolConstants.BOND_COVALENT_TRIPLE) != 0) {
                bondFound = true;
              }
              break;
            case SmilesBond.TYPE_UNKOWN:
              bondFound = true;
              break;
            }
          }
        }
        if (!bondFound)
          return;
      }
    }

    // Finish matching
      patternAtom.setMatchingAtom(i);
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
          for (int j = v.size(); --j >= 0 && isOK;)
            isOK = !(((BitSet) v.get(j)).equals(bs));
          if (isOK)
            v.add(bs);
        }
      }
      patternAtom.setMatchingAtom(-1);
  }
}
