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

  public boolean areEqual(String smiles1, String smiles2) {
    BitSet[] result = find(smiles1, smiles2, false, true, false);
    return (result != null && result.length == 1);  
  }
  
  /**
   * for JUnit test, mainly
   * 
   * @param smiles1
   * @param molecule
   * @return        true only if the SMILES strings match and there are no errors
   */
  public boolean areEqual(String smiles1, SmilesSearch molecule) {
    BitSet[] ret = find(smiles1, molecule, false, true);
    return (ret != null && ret.length == 1);
  }

  /**
   * 
   * Searches for all matches of a pattern within a SMILES string.
   * 
   * 
   * @param pattern
   * @param smiles
   * @param isSmarts when set TRUE, forces an all-atom match (same as "isEqual")
   * @param matchAllAtoms 
   * @param firstMatchOnly 
   * @return number of occurances of pattern within smiles
   */
  public BitSet[] find(String pattern, String smiles, boolean isSmarts,
                       boolean matchAllAtoms, boolean firstMatchOnly) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(smiles, false);
      return find(pattern, search, isSmarts, matchAllAtoms);
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private BitSet[] find(String pattern, SmilesSearch search, boolean isSmarts, boolean matchAllAtoms) {
    // create a topological model set from smiles
    // do not worry about stereochemistry -- this
    // will be handled by SmilesSearch.setSmilesCoordinates
    BitSet bsAromatic = new BitSet();
    search.createTopoMap(bsAromatic);
    return (BitSet[]) match(pattern, search.jmolAtoms,
        -search.jmolAtoms.length, null, null, bsAromatic, isSmarts, true,
        false, MODE_ARRAY);
  }


  public String getLastException() {
    return InvalidSmilesException.getLastError();
  }

  /**
   * Returns a bitset matching the pattern within atoms.
   * 
   * @param pattern
   *          SMILES pattern.
   * @param atoms
   * @param atomCount
   * @param bsSelected
   * @param isSmarts
   * @param firstMatchOnly
   * @return BitSet indicating which atoms match the pattern.
   */

  public BitSet getSubstructureSet(String pattern, JmolNode[] atoms,
                                   int atomCount, BitSet bsSelected,
                                   boolean isSmarts, boolean firstMatchOnly) {
    return (BitSet) match(pattern, atoms, atomCount, bsSelected, null, null,
        isSmarts, false, firstMatchOnly, MODE_BITSET);
  }

  /**
   * Returns a vector of bits indicating which atoms match the pattern.
   * @param pattern SMILES pattern.
   * @param atoms 
   * @param atomCount 
   * @param bsSelected 
   * @param bsRequired 
   * @param bsNot 
   * @param bsAromatic 
   * @param isSmarts 
   * @param firstMatchOnly
   * 
   * @return BitSet Array indicating which atoms match the pattern.
   */
  public BitSet[] getSubstructureSetArray(String pattern, JmolNode[] atoms,
                                          int atomCount, BitSet bsSelected,
                                          BitSet bsRequired, BitSet bsNot,
                                          BitSet bsAromatic, boolean isSmarts,
                                          boolean firstMatchOnly) {
    return (BitSet[]) match(pattern, atoms, atomCount, bsSelected, bsRequired,
        bsAromatic, isSmarts, false, firstMatchOnly, MODE_ARRAY);
  }

  /**
   * Rather than returning bitsets, this method returns the
   * sets of matching atoms so that a direct atom-atom correlation can be done.
   * 
   * @param pattern 
   * @param atoms 
   * @param atomCount 
   * @param bsSelected 
   * @param isSmarts 
   * @param firstMatchOnly
   * @return      a set of atom correlations
   * 
   */
  public int[][] getCorrelationMaps(String pattern, JmolNode[] atoms,
                                    int atomCount, BitSet bsSelected,
                                    boolean isSmarts, boolean firstMatchOnly) {
      return (int[][]) match(pattern, atoms, atomCount, bsSelected,
          null, null, isSmarts, false, firstMatchOnly, MODE_MAP);
  }

  private final static int MODE_MAP = 3;
  private final static int MODE_ARRAY = 2;
  private final static int MODE_BITSET = 1;
  
  private Object match(String pattern, JmolNode[] atoms,
                                          int atomCount, BitSet bsSelected,
                                          BitSet bsRequired, BitSet bsAromatic,
                                          boolean isSmarts, boolean matchAllAtoms,
                                          boolean firstMatchOnly, 
                                          int mode) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(pattern, isSmarts);
      search.jmolAtoms = atoms;
      search.jmolAtomCount = Math.abs(atomCount);
      if (atomCount < 0)
        search.isSmilesFind = true;
      search.bsSelected = bsSelected;
      search.bsRequired = (bsRequired != null && bsRequired.cardinality() > 0 ? bsRequired
          : null);
      search.setRingData(bsAromatic);
      Vector vSubstructures;
      search.firstMatchOnly = firstMatchOnly;
      search.matchAllAtoms = matchAllAtoms;
      switch(mode) {
      case MODE_BITSET:
        search.asVector = false;
        return (BitSet) search.search(false);
      case MODE_ARRAY:
        search.asVector = true;
        vSubstructures = (Vector) search.search(false);
        BitSet[] bitsets = new BitSet[vSubstructures.size()];
        for (int i = 0; i < bitsets.length; i++)
          bitsets[i] = (BitSet) vSubstructures.get(i);
        return bitsets;
      case MODE_MAP:
        search.getMaps = true;
        vSubstructures = (Vector) search.search(false);
        int[][] maps = new int[vSubstructures.size()][];
        for (int i = 0; i < maps.length; i++)
          maps[i] = (int[]) vSubstructures.get(i);
        return maps;
      }
    } catch (Exception e) {
      if (InvalidSmilesException.getLastError() == null)
        InvalidSmilesException.setLastError(e.getMessage());
      e.printStackTrace();
    }
    return null;
  }

  public String getMolecularFormula(String pattern, boolean isSmarts) {
    InvalidSmilesException.setLastError(null);
    try {
      SmilesSearch search = SmilesParser.getMolecule(pattern, isSmarts);
      search.createTopoMap(null);
      search.nodes = search.jmolAtoms;
      return search.getMolecularFormula(!isSmarts);
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

}
