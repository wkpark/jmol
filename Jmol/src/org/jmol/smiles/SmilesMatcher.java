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
 * SmilesMatcher matcher = new SmilesMatcher();
 * try {
 *   BitSet bitSet = matcher.getSubstructureSet(smilesString, atoms, atomCount);
 *   
 *   // or, to get the exact finds:
 *   
 *   BitSet[] matcher.getSubstructureSetArray(smiles, atoms, atomCount, 
 *                                         bsSelected, bsRequired, bsNot);
 *                                         
 *   //The input BitSets may be null
 * 
 *   // or, to count the matches of one Smiles string in another:
 *   
 *   int nFound = matcher.find(smilesString1, smilesString2, oneOnly);
 *   // smilesString1 is found in smilesString2 or return -1 for parsing error
 *   
 * } catch (InvalidSmilesException e) {
 *   // Exception management
 * }
 *   
 *   in Jmol script:
 *   
 *   string2.find("SMILES", string1)
 *   
 *   e.g.
 *   
 *     print "CCCC".find("SMILES", "C[C]")
 *
 *   2
 *   
 * </code></pre>
 * 
 * @author Nicolas Vervelle
 * @see org.jmol.smiles.SmilesParser
 * @see org.jmol.smiles.SmilesSearch
 * 
 */
public class SmilesMatcher implements SmilesMatcherInterface {

  /**
   * 
   * searches for matches of pattern within smiles
   * 
   * @param pattern
   * @param smiles
   * @param isSearch
   * @param isAll
   * @return number of occurances of pattern within smiles
   */
  public int find(String pattern, String smiles, boolean isSearch, boolean isAll) {
    BitSet[] list = null;
    try {
      // create a topological model set from smiles
      // do not worry about stereochemistry -- this
      // will be handled by SmilesSearch.setSmilesCoordinates
      SmilesSearch search = SmilesParser.getMolecule(false, smiles);
      BitSet bsAromatic = new BitSet();
      int atomCount = search.patternAtomCount;
      int nAtomsNew = atomCount;
      for (int i = 0; i < atomCount; i++) {
        int nH = search.atoms[i].explicitHydrogenCount;
        if (nH < 0)
          nH = 0;
        nAtomsNew += nH;
      }
      Atom[] atoms = new Atom[nAtomsNew];
      int ptAtom = 0;
      int[] bondCounts = new int[nAtomsNew];
      for (int i = 0; i < atomCount; i++) {
        SmilesAtom sAtom = search.getAtom(i);
        int cclass = sAtom.getChiralClass();
        int n = sAtom.explicitHydrogenCount;
        if (n < 0)
          n = 0;
        // create a Jmol atom for this pattern atom
        // we co-opt atom.matchingAtom here
        // because this search will never actually be run
        Atom atom = atoms[ptAtom] = new Atom(0, ptAtom, 0, 0, 0, 0, null,
            cclass == Integer.MIN_VALUE ? cclass :
            (cclass << 8) + sAtom.getChiralOrder(), 
            sAtom.atomicNumber, sAtom.getCharge(),
            false, '\0', '\0');
        // we pass on the aromatic flag because
        // we don't want SmilesSearch to calculate
        // that for us
        if (sAtom.isAromatic())
          bsAromatic.set(ptAtom);
        sAtom.setMatchingAtom(ptAtom);
        // set up the bonds array and fill with H atoms
        bondCounts[ptAtom++] = n;
        Bond[] bonds = new Bond[sAtom.getBondsCount() + n];
        atom.setBonds(bonds);
        while (--n >= 0) {
          Atom atomH = atoms[ptAtom] = new Atom(0, ptAtom, 0, 0, 0, 0, null, 0,
              (short) 1, 0, false, '\0', '\0');
          ptAtom++;
          atomH.setBonds(new Bond[1]);
          atomH.bonds[0] = bonds[n] = new Bond(atom, atomH,
              JmolConstants.BOND_COVALENT_SINGLE, (short) 0, (short) 0);
        }
      }
      
      // set up bonds
      for (int i = 0; i < atomCount; i++) {
        SmilesAtom sAtom = search.getAtom(i);
        int i1 = sAtom.getMatchingAtom();
        for (int j = sAtom.getBondsCount(); --j >= 0;) {
          SmilesBond sBond = sAtom.getBond(j);
          if (sBond.getAtom1() != sAtom)
            continue;
          int order = 1;
          switch (sBond.getBondType()) {
          case SmilesBond.TYPE_UNKNOWN:
          case SmilesBond.TYPE_NONE:
          case SmilesBond.TYPE_DIRECTIONAL_1:
          case SmilesBond.TYPE_DIRECTIONAL_2:
          case SmilesBond.TYPE_RING_BOND:
          case SmilesBond.TYPE_SINGLE:
          case SmilesBond.TYPE_ANY:
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
          SmilesAtom sAtom2 = sBond.getAtom2();
          int i2 = sAtom2.getMatchingAtom();
          Atom atom1 = (Atom) atoms[i1];
          Atom atom2 = (Atom) atoms[i2];
          Bond b = new Bond(atom1, atom2, order, (short) 0, (short) 0);
          atom1.bonds[bondCounts[atom1.index]++] = atom2.bonds[bondCounts[atom2.index]++] = b;
        }
      }
      list = getSubstructureSetArray(pattern, atoms, -atomCount, null, null,
          null, bsAromatic, isSearch, isAll);
      return list.length;
    } catch (Exception e) {
      return -1;
    }
  }
  

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles
   *          SMILES pattern.
   * @param atoms
   * @param atomCount
   * @param bsSelected
   * @param isSearch
   * @param isAll
   * @return BitSet indicating which atoms match the pattern.
   * @throws Exception
   *           Raised if <code>smiles</code> is not a valid SMILES pattern.
   */

  public BitSet getSubstructureSet(String smiles, Atom[] atoms, int atomCount,
                                   BitSet bsSelected, boolean isSearch,
                                   boolean isAll) throws Exception {
    SmilesSearch search = SmilesParser.getMolecule(isSearch, smiles);
    search.jmolAtoms = atoms;
    search.bsSelected = bsSelected;
    search.jmolAtomCount = atomCount;
    search.setRingData(null);
    search.isAll = isAll;
    return (BitSet) search.search(false);
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * 
   * @param smiles SMILES pattern.
   * @param atoms 
   * @param atomCount 
   * @param bsSelected 
   * @param bsRequired 
   * @param bsNot 
   * @param bsAromatic 
   * @param isSearch 
   * @param isAll 
   * @return BitSet Array indicating which atoms match the pattern.
   * @throws Exception Raised if <code>smiles</code> is not a valid SMILES pattern.
   */
  public BitSet[] getSubstructureSetArray(String smiles, Atom[] atoms, int atomCount, 
                                          BitSet bsSelected, 
                                          BitSet bsRequired, BitSet bsNot, 
                                          BitSet bsAromatic, boolean isSearch, boolean isAll)
      throws Exception {
    SmilesSearch search = SmilesParser.getMolecule(isSearch, smiles);
    search.jmolAtoms = atoms;
    search.jmolAtomCount = Math.abs(atomCount);
    if (atomCount < 0)
      search.isSmilesFind = true;
    search.bsSelected = bsSelected;
    search.bsRequired = (bsRequired != null && bsRequired.cardinality() > 0 ? bsRequired : null);
    search.bsNot = bsNot;
    search.setRingData(bsAromatic);
    search.isAll = isAll;
    search.asVector = true;
    Vector vSubstructures = (Vector) search.search(false);
    BitSet[] bitsets = new BitSet[vSubstructures.size()];
    for (int i = 0; i < bitsets.length; i++)
      bitsets[i] = (BitSet) vSubstructures.get(i);
    return bitsets;
  }
}
