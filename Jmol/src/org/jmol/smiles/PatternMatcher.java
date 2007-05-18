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

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

/**
 * A class to match a SMILES pattern with a Jmol molecule.
 * <p>
 * The SMILES specification can been found at the
 * <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * <p>
 * An example on how to use it:
 * <pre><code>
 * PatternMatcher matcher = new PatternMatcher(jmolViewer);
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

  private int atomCount = 0;
  private ModelSet modelSet = null;

  /**
   * Constructs a <code>PatternMatcher</code>.
   * 
   */
  public PatternMatcher() {
  }
  public void setViewer(Viewer viewer) {
    this.modelSet = viewer.getModelSet();
    this.atomCount = viewer.getAtomCount();     
  }
  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @return BitSet Array indicating which atoms match the pattern.
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
   * @param pattern SMILES pattern.
   * @return BitSet Array indicating which atoms match the pattern.
   */
  public BitSet getSubstructureSet(SmilesMolecule pattern) {
    BitSet bsSubstructure = new BitSet();
    searchMatch(bsSubstructure, pattern, 0);
    return bsSubstructure;
  }

  /**
   * Recursively search matches.
   * 
   * @param bs Resulting BitSet (each atom in a structure is set to 1).
   * @param pattern SMILES pattern.
   * @param atomNum Current atom of the pattern.
   */
  private void searchMatch(BitSet bs, SmilesMolecule pattern, int atomNum) {
    //Logger.debug("Begin match:" + atomNum);
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
              searchMatch(bs, pattern, patternAtom, atomNum, bonds[j].getAtomIndex2());
            }
            if (bonds[j].getAtomIndex2() == matchingAtom) {
              searchMatch(bs, pattern, patternAtom, atomNum, bonds[j].getAtomIndex1());
            }
          }
        }
        return;
      }
    }
    for (int i = 0; i < atomCount; i++) {
      searchMatch(bs, pattern, patternAtom, atomNum, i);
    }
    //Logger.debug("End match:" + atomNum);
  }
  
  /**
   * Recursively search matches.
   * 
   * @param bs Resulting BitSet (each atom in a structure is set to 1).
   * @param pattern SMILES pattern.
   * @param patternAtom Atom of the pattern that is currently tested.
   * @param atomNum Current atom of the pattern.
   * @param i Atom number of the atom that is currently tested to match <code>patternAtom</code>.
   */
  private void searchMatch(BitSet bs, SmilesMolecule pattern, SmilesAtom patternAtom, int atomNum, int i) {
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
        searchMatch(bs, pattern, atomNum + 1);
      } else {
        for (int k = 0; k < pattern.getAtomsCount(); k++) {
          SmilesAtom matching = pattern.getAtom(k);
          bs.set(matching.getMatchingAtom());
        }
      }
      patternAtom.setMatchingAtom(-1);
  }
}
