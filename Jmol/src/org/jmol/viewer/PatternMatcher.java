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
 * Pattern matching class for comparing Jmol molecule with SMILES pattern 
 */
class PatternMatcher {

  private int atomCount = 0;
  private Frame frame = null;

  /**
   * @param viewer
   */
  public PatternMatcher(Viewer viewer) {
    this.frame = viewer.getFrame();
    this.atomCount = viewer.getAtomCount(); 
  }

  /**
   * @param smiles SMILES pattern
   * @return BitSet indicating which atoms match the pattern
   * @throws InvalidSmilesException
   */
  public BitSet getSubstructureSet(String smiles) throws InvalidSmilesException {
    SmilesParser parser = new SmilesParser();
    SmilesMolecule pattern = parser.parseSmiles(smiles);
    return getSubstructureSet(pattern);
  }

  /**
   * @param pattern SMILES pattern
   * @return BitSet indicating which atoms match the pattern
   */
  public BitSet getSubstructureSet(SmilesMolecule pattern) {
    BitSet bsSubstructure = new BitSet();
    searchMatch(bsSubstructure, pattern, 0);
    return bsSubstructure;
  }

  /**
   * Recursively search matches
   * 
   * @param bs Resulting BitSet (each atom in a structure is set to 1)
   * @param pattern SMILES pattern
   * @param atomNum Current atom of the pattern
   */
  private void searchMatch(BitSet bs, SmilesMolecule pattern, int atomNum) {
    //System.out.println("Begin match:" + atomNum);
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
    //System.out.println("End match:" + atomNum);
  }
  
  private void searchMatch(BitSet bs, SmilesMolecule pattern, SmilesAtom patternAtom, int atomNum, int i) {
    // Check that an atom is not used twice
    for (int j = 0; j < atomNum; j++) {
      SmilesAtom previousAtom = pattern.getAtom(j);
      if (previousAtom.getMatchingAtom() == i) {
        return;
      }
    }
    
    boolean canMatch = true;
    Atom atom = frame.getAtomAt(i);

    // Check symbol
    if ((patternAtom.getSymbol() != "*") &&
        (patternAtom.getSymbol() != atom.getElementSymbol())) {
      canMatch = false;
    }
    // Check atomic mass : NO because Jmol doesn't know about atomic mass
    // Check charge
    if (patternAtom.getCharge() != atom.getFormalCharge()) {
      canMatch = false;
    }

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
        if (!bondFound) {
          canMatch = false;
        }
      }
    }

    // Finish matching
    if (canMatch) {
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
}
