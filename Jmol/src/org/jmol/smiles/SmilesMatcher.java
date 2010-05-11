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

import org.jmol.api.JmolEdge;
import org.jmol.api.JmolNode;
import org.jmol.api.SmilesMatcherInterface;

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
    InvalidSmilesException.setLastError(null);
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
      SmilesAtom[] atoms = new SmilesAtom[nAtomsNew];
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
        SmilesAtom atom = atoms[ptAtom] = new SmilesAtom(0, ptAtom, cclass == Integer.MIN_VALUE ? cclass :
            (cclass << 8) + sAtom.getChiralOrder(), 
            sAtom.elementNumber, sAtom.getCharge());
        // we pass on the aromatic flag because
        // we don't want SmilesSearch to calculate
        // that for us
        if (sAtom.isAromatic())
          bsAromatic.set(ptAtom);
        sAtom.setMatchingAtom(ptAtom);
        // set up the bonds array and fill with H atoms
        bondCounts[ptAtom++] = n;
        SmilesBond[] bonds = new SmilesBond[sAtom.getBondsCount() + n];
        atom.setBonds(bonds);
        while (--n >= 0) {
          SmilesAtom atomH = atoms[ptAtom] = new SmilesAtom(0, ptAtom, 0, (short) 1, 0);
          ptAtom++;
          atomH.setBonds(new SmilesBond[1]);
          atomH.bonds[0] = bonds[n] = new SmilesBond(atom, atomH,
              JmolEdge.BOND_COVALENT_SINGLE);
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
          // these first two are for cis/trans alkene
          // stereochemistry; we co-opt stereo near/far here
          case SmilesBond.TYPE_DIRECTIONAL_1:
            order = JmolEdge.BOND_STEREO_NEAR;
            break;
          case SmilesBond.TYPE_DIRECTIONAL_2:
            order = JmolEdge.BOND_STEREO_FAR;
            break;
          case SmilesBond.TYPE_SINGLE:
            order = JmolEdge.BOND_COVALENT_SINGLE;
            break;
          case SmilesBond.TYPE_AROMATIC:
            order = JmolEdge.BOND_AROMATIC_SINGLE;
            break;
          case SmilesBond.TYPE_DOUBLE:
            order = JmolEdge.BOND_COVALENT_DOUBLE;
            break;
          case SmilesBond.TYPE_TRIPLE:
            order = JmolEdge.BOND_COVALENT_TRIPLE;
            break;
          }
          SmilesAtom sAtom2 = sBond.getAtom2();
          int i2 = sAtom2.getMatchingAtom();
          SmilesAtom atom1 = atoms[i1];
          SmilesAtom atom2 = atoms[i2];
          SmilesBond b = new SmilesBond(atom1, atom2, order);
          atom1.bonds[bondCounts[atom1.index]++] = atom2.bonds[bondCounts[atom2.index]++] = b;
        }
      }
      list = getSubstructureSetArray(pattern, atoms, -nAtomsNew, null, null,
          null, bsAromatic, isSearch, isAll);
      return list.length;
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
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

  public BitSet getSubstructureSet(String smiles, JmolNode[] atoms, int atomCount,
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
  public BitSet[] getSubstructureSetArray(String smiles, JmolNode[] atoms, int atomCount, 
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
