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

import org.jmol.api.JmolNode;
import org.jmol.api.SmilesMatcherInterface;

/**
 * Originating author: Nicholas Vervelle
 * 
 * A class to handle a variety of SMILES/SMARTS-related functions, including:
 *  -- determining if two SMILES strings are equivalent
 *  -- determining the molecular formula of a SMILES or SMARTS string
 *  -- searching for specific runs of atoms in a 3D model
 *  -- searching for specific runs of atoms in a SMILES description
 *  -- generating valid (though not canonical) SMILES and bioSMILES strings
 *  -- getting atom-atom correlation maps to be used with biomolecular alignment methods
 *  
 * <p>
 * The SMILES specification can been found at the
 * <a href="http://www.daylight.com/smiles/">SMILES Home Page</a>.
 * <p>
 * <pre><code>
 * public methods:
 * 
 * boolean areEqual  -- checks a SMILES string against a reference
 * 
 * BitSet[] find  -- finds one or more occurances of a SMILES or SMARTS string within a SMILES string
 * 
 * int[][] getCorrelationMaps  -- returns correlated arrays of atoms
 * 
 * String getLastError  -- returns any error that was last encountered.
 * 
 * String getMolecularFormula   -- returns the MF of a SMILES or SMARTS string
 * 
 * String getSmiles  -- returns a standard SMILES string or a
 *                  Jmol BIOSMILES string with comment header.
 * 
 * BitSet getSubstructureSet  -- returns a single BitSet with all found atoms included
 *   
 *   
 *   in Jmol script:
 *   
 *   string2.find("SMILES", string1)
 *   string2.find("SMARTS", string1)
 *   
 *   e.g.
 *   
 *     print "CCCC".find("SMILES", "C[C]")
 *
 *   select search("smartsString")
 *   
 *   All bioSMARTS strings begin with ~ (tilde).
 *   
 * </code></pre>
 * 
 * @author Bob Hanson
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
  public BitSet[] find(String pattern, String smiles, boolean isSearch,
                       boolean isAll) {
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

  public BitSet getSubstructureSet(String smiles, JmolNode[] atoms,
                                   int atomCount, BitSet bsSelected,
                                   boolean isSearch, boolean isAll) {
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
   * @param pattern SMILES pattern.
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
  public BitSet[] getSubstructureSetArray(String pattern, JmolNode[] atoms,
                                          int atomCount, BitSet bsSelected,
                                          BitSet bsRequired, BitSet bsNot,
                                          BitSet bsAromatic, boolean isSearch,
                                          boolean isAll) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(pattern, isSearch);
      search.jmolAtoms = atoms;
      search.jmolAtomCount = Math.abs(atomCount);
      if (atomCount < 0)
        search.isSmilesFind = true;
      search.bsSelected = bsSelected;
      search.bsRequired = (bsRequired != null && bsRequired.cardinality() > 0 ? bsRequired
          : null);
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
      search.createTopoMap(null);
      search.nodes = search.jmolAtoms;
      return search.getMolecularFormula(!isSearch);
    } catch (InvalidSmilesException e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      return null;
    }
  }

  public String getSmiles(JmolNode[] atoms, int atomCount,
                             BitSet bsSelected, String comment, boolean asBioSmiles) {
    InvalidSmilesException.setLastError(null);
    try {
      if (asBioSmiles)
        return (new SmilesGenerator()).getBioSmiles(atoms, atomCount, bsSelected,
            comment);
      return (new SmilesGenerator()).getSmiles(atoms, atomCount, bsSelected);
    } catch (InvalidSmilesException e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      return null;
    }
  }

  private BitSet[] find(String pattern, SmilesSearch search, boolean isSearch,
                        boolean isAll) {
    // create a topological model set from smiles
    // do not worry about stereochemistry -- this
    // will be handled by SmilesSearch.setSmilesCoordinates
    BitSet bsAromatic = new BitSet();
    search.createTopoMap(bsAromatic);
    return getSubstructureSetArray(pattern, search.jmolAtoms, -search.jmolAtoms.length, null, null,
        null, bsAromatic, isSearch, isAll);
  }


}
