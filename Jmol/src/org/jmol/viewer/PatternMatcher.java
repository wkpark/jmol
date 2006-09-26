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

package org.jmol.viewer;

import java.util.BitSet;

import org.jmol.smiles.InvalidSmilesException;
import org.jmol.smiles.SmilesAtom;
import org.jmol.smiles.SmilesBond;
import org.jmol.smiles.SmilesMolecule;
import org.jmol.smiles.SmilesParser;

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
class PatternMatcher {

  private int atomCount = 0;
  private Frame frame = null;

  /**
   * Constructs a <code>PatternMatcher</code>.
   * 
   * @param viewer Jmol viewer.
   */
  PatternMatcher(Viewer viewer) {
    this.frame = viewer.getFrame();
    this.atomCount = viewer.getAtomCount(); 
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws InvalidSmilesException Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  BitSet getSubstructureSet(String smiles) throws InvalidSmilesException {
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
  BitSet getSubstructureSet(SmilesMolecule pattern) {
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
        Atom atom = frame.getAtomAt(matchingAtom);
        Bond[] bonds = atom.getBonds();
        if (bonds != null) {
          for (int j = 0; j < bonds.length; j++) {
            if (bonds[j].getAtom1().atomIndex == matchingAtom) {
              searchMatch(bs, pattern, patternAtom, atomNum, bonds[j].getAtom2().atomIndex);
            }
            if (bonds[j].getAtom2().atomIndex == matchingAtom) {
              searchMatch(bs, pattern, patternAtom, atomNum, bonds[j].getAtom1().atomIndex);
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
    
    Atom atom = frame.getAtomAt(i);

    // Check symbol -- not isotope-sensitive
    String targetSym = patternAtom.getSymbol();
    int n = atom.getElementNumber();
    if (targetSym != "*" && targetSym != JmolConstants.elementSymbolFromNumber(n))
      return;
    

    //nothing more needs to be done here
    //this is fully generalized
    //to match 
    int targetMass = patternAtom.getAtomicMass();
    if (targetMass > 0) {
      // smiles indicates [13C] or [12C]
      int isotopeMass = atom.getIsotopeNumber();
      if (isotopeMass != targetMass) {
        if (isotopeMass > 0) {
          //both are indicating and not the same
          //no match: 12C != 13C
          return;
        }
        // we let "C" match only [12C] (naturl)
        // If it is not listed in JmolConstants as natural,
        // then we it will consider it unmatched
        if(!patternAtom.getMatchNatural())
          return;  
        // if here, we must have "C"/[12C]; "N"/[14N]; "H"/[1H]
        // we use the convention that no label is natural
      }
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
          if ((bonds[k].getAtom1().atomIndex == matchingAtom) ||
              (bonds[k].getAtom2().atomIndex == matchingAtom)) {
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
