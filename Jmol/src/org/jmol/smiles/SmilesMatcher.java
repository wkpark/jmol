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

  public boolean areEqual(String smiles, SmilesSearch search) {
    InvalidSmilesException.setLastError(null);
    try {
      return (find(smiles, search, false, false).length == 1);
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
    }
    return false;
  }
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
  public BitSet[] find(String pattern, String smiles, boolean isSearch, boolean isAll) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(smiles, false);
      return find(pattern, search, isSearch, isAll);
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
  
  public String getLastException() {
    return InvalidSmilesException.getLastError();
  }
  
  private BitSet[] find(String pattern, SmilesSearch search, boolean isSearch, boolean isAll) {
    // create a topological model set from smiles
    // do not worry about stereochemistry -- this
    // will be handled by SmilesSearch.setSmilesCoordinates
    BitSet bsAromatic = new BitSet();
    int atomCount = search.atomCount;
    int nAtomsMissing = search.getMissingHydrogenCount();
    SmilesAtom[] atoms = new SmilesAtom[atomCount + nAtomsMissing];
    int ptAtom = 0;
    BitSet bsFixH = new BitSet();
    for (int i = 0; i < atomCount; i++) {
      SmilesAtom sAtom = search.patternAtoms[i];
      int cclass = sAtom.getChiralClass();
      int n = sAtom.missingHydrogenCount;
      if (n < 0)
        n = 0;
      // create a Jmol atom for this pattern atom
      // we co-opt atom.matchingAtom here
      // because this search will never actually be run
      SmilesAtom atom = atoms[ptAtom] = new SmilesAtom(0, ptAtom,
          cclass == Integer.MIN_VALUE ? cclass : (cclass << 8)
              + sAtom.getChiralOrder(), sAtom.elementNumber, sAtom
              .getCharge());
      atom.atomName = sAtom.atomName;
      atom.residueName = sAtom.residueName;
      atom.residueChar = sAtom.residueChar;
      atom.isBioAtom = sAtom.isBioAtom;
      atom.isLeadAtom = sAtom.isLeadAtom;
      atom.setAtomicMass(sAtom.getAtomicMass());
      //System.out.println(atom);
      // we pass on the aromatic flag because
      // we don't want SmilesSearch to calculate
      // that for us
      if (sAtom.isAromatic())
        bsAromatic.set(ptAtom);
      // set up the bonds array and fill with H atoms
      // when there is only 1 H and the atom is NOT FIRST, then it will
      // be important to designate the bonds in order -- with the
      // H SECOND not first
      // "Double Bond" here just to remind us to switch this one.
      if (!sAtom.isFirst && n == 1 && cclass > 0)
        bsFixH.set(ptAtom);
      sAtom.setMatchingAtom(ptAtom++);
      SmilesBond[] bonds = new SmilesBond[sAtom.getCovalentBondCount() + n];
      atom.setBonds(bonds);
      while (--n >= 0) {
        SmilesAtom atomH = atoms[ptAtom] = new SmilesAtom(0, ptAtom, 0,
            (short) 1, 0);
        //System.out.println(atomH);
        ptAtom++;
        atomH.setBonds(new SmilesBond[1]);
        new SmilesBond(atom, atomH, JmolEdge.BOND_COVALENT_SINGLE, false);
      }
    }

    // set up bonds
    for (int i = 0; i < atomCount; i++) {
      SmilesAtom sAtom = search.patternAtoms[i];
      int i1 = sAtom.getMatchingAtom();
      SmilesAtom atom1 = atoms[i1];
      int n = sAtom.getCovalentBondCount();
      for (int j = 0; j < n; j++) {
        SmilesBond sBond = sAtom.getBond(j);
        boolean firstAtom = (sBond.getAtom1() == sAtom);
        //SmilesBond b;
        if (firstAtom) {
          int order = 1;
          switch (sBond.bondType) {
          // these first two are for cis/trans alkene
          // stereochemistry; we co-opt stereo near/far here
          case SmilesBond.TYPE_BIO_PAIR:
          case SmilesBond.TYPE_BIO_SEQUENCE:
            order = sBond.bondType;
            break;
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
            order = JmolEdge.BOND_AROMATIC_DOUBLE;
            break;
          case SmilesBond.TYPE_DOUBLE:
            order = JmolEdge.BOND_COVALENT_DOUBLE;
            break;
          case SmilesBond.TYPE_TRIPLE:
            order = JmolEdge.BOND_COVALENT_TRIPLE;
            break;
          }
          SmilesAtom atom2 = atoms[sBond.getAtom2().getMatchingAtom()];
          /*b = */new SmilesBond(atom1, atom2, order, false);
          //if (firstAtom)
            //System.out.println(b);
        } else {
          //SmilesAtom atom2 = atoms[sBond.getAtom1().getMatchingAtom()];
          //b = atom2.getBondTo(atom1);
        }
      }
    }
    // fix H atoms
    for (int i = bsFixH.nextSetBit(0); i >= 0; i = bsFixH.nextSetBit(i + 1)) {
      JmolEdge[] bonds = atoms[i].getEdges();
      JmolEdge b = bonds[0];
      bonds[0] = bonds[1];
      bonds[1] = b;
    }
    return getSubstructureSetArray(pattern, atoms, -atoms.length, null, null,
        null, bsAromatic, isSearch, isAll);
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
   */

  public BitSet getSubstructureSet(String smiles, JmolNode[] atoms, int atomCount,
                                   BitSet bsSelected, boolean isSearch,
                                   boolean isAll) {
    InvalidSmilesException.setLastError(null);
    try {
    SmilesSearch search = SmilesParser.getMolecule(smiles, isSearch);
    search.jmolAtoms = atoms;
    search.bsSelected = bsSelected;
    search.jmolAtomCount = atomCount;
    search.setRingData(null);
    search.isAll = isAll;
    return (BitSet) search.search(false);
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
      return null;
    }
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
   */
  public BitSet[] getSubstructureSetArray(String smiles, JmolNode[] atoms, int atomCount, 
                                          BitSet bsSelected, 
                                          BitSet bsRequired, BitSet bsNot, 
                                          BitSet bsAromatic, boolean isSearch, boolean isAll) {
    InvalidSmilesException.setLastError(null);
    try {
    SmilesSearch search = SmilesParser.getMolecule(smiles, isSearch);
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
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Rather than returning bitsets, this method returns the
   * sets of matching atoms so that a direct atom-atom correlation can be done.
   * 
   * @param smiles 
   * @param atoms 
   * @param atomCount 
   * @param bsSelected 
   * @param isSearch 
   * @param isAll 
   * @return      a set of atom correlations
   * 
   */
  public int[][] getCorrelationMaps(String smiles, JmolNode[] atoms,
                                    int atomCount, BitSet bsSelected,
                                    boolean isSearch, boolean isAll) {
    InvalidSmilesException.setLastError(null);
    try {
    SmilesSearch search = SmilesParser.getMolecule(smiles, isSearch);
    search.jmolAtoms = atoms;
    search.jmolAtomCount = Math.abs(atomCount);
    if (atomCount < 0)
      search.isSmilesFind = true;
    search.bsSelected = bsSelected;
    search.bsRequired = null;
    search.bsNot = null;
    search.setRingData(null);
    search.isAll = isAll;
    search.asVector = true;
    search.getMaps = true;
    Vector vSubstructures = (Vector) search.search(false);
    int[][] maps = new int[vSubstructures.size()][];
    for (int i = 0; i < maps.length; i++)
      maps[i] = (int[]) vSubstructures.get(i);
    return maps;
  } catch (Exception e) {
    if (InvalidSmilesException.getLastError() == null)
      InvalidSmilesException.setLastError(e.getMessage());
    e.printStackTrace();
    return null;
  }
  }
  
  public String getMolecularFormula(String pattern, boolean isSearch) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(pattern, isSearch);
      return search.getMolecularFormula();
    } catch (InvalidSmilesException e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      return null;
    }
  }

  public String getBioSmiles(JmolNode[] atoms, int atomCount,
                                   BitSet bsSelected, String comment) {
    InvalidSmilesException.setLastError(null);
    try {
      return SmilesGenerator.getBioSmiles(atoms, atomCount, bsSelected, comment);
    } catch (InvalidSmilesException e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      return null;
    }
  }

}
