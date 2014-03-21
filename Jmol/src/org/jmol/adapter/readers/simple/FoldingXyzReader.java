/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-10 10:36:58 -0500 (Sun, 10 Sep 2006) $
 * $Revision: 5478 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.adapter.readers.simple;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.PT;


/**
 * This reader is for current.xyz files generated by Folding@Home project
 * (see <a href="http://folding.stanford.edu">http://folding.stanford.edu</a>)
 * 
 * I have not found a precise description of the file format.
 * I used source code from fpd from Dick Howell to analyze the file format.
 * (see <a href="http://boston.quik.com/rph">http://boston.quik.com/rph</a>)
 * -- Nico Vervelle
 * 
 * Extended by Bob Hanson 2/2014:
 * 
 *   - adds support for newer Tinker files (see data/folding)
 *   - adds desired model options
 *   - adds atom type if available
 * 
 */

public class FoldingXyzReader extends AtomSetCollectionReader {

  private boolean haveBonds;

  @Override
  protected void initializeReader() {
  }
  
  @Override
  protected void finalizeReader() throws Exception {
    if (haveBonds)
      asc.setNoAutoBond();
    isTrajectory = false;
    finalizeReaderASCR();
  }
  
  /**
   * @return true if next line needs to be read.
   * 
   * Note that just a single token on line 1 is NOT possible. 
   * If that were the case, the xyz reader would have captured this.
   * 
   */
  @Override
  protected boolean checkLine() throws Exception {
    int[] next = new int[] { 0 };
    String token = PT.parseTokenNext(line, next);
    if (token == null)
      return true;
    boolean addAtoms = doGetModel(++modelNumber, null);
    int modelAtomCount = parseIntStr(token);
    if (addAtoms) {
      asc.newAtomSet();
      String[] tokens = getTokens();
      asc.setAtomSetName(tokens.length == 2 ? "Protein "
          + tokens[1] : line.substring(next[0]).trim());
    }
    boolean readLine = readAtoms(modelAtomCount + 1, addAtoms); // Some Tinker files are one off!
    continuing = !addAtoms || !isLastModel(modelNumber);
    return readLine;
  }
	    
  /**
   * Lots of possibilities here:
   * 
   * atom count is real; atom count is one less than true atom count
   * sixth column is atom type; sixth column is first bond
   * 
   * 
   * @param ac
   * @param addAtoms
   * @return true if next line needs to be read
   * @throws Exception
   */
  boolean readAtoms(int ac, boolean addAtoms) throws Exception {
    // Stores bond informations
    Map<String, int[]> htBondCounts = new Hashtable<String, int[]>();
    int[][] bonds = AU.newInt2(ac);
    boolean haveAtomTypes = true;
    boolean checking = true;
    String lastAtom = null;
    boolean readNextLine = true;
    for (int i = 0; i < ac; i++) {
      discardLinesUntilNonBlank();
      if (line == null)
        break; // no problem.
      String[] tokens = getTokensStr(line);
      String sIndex = tokens[0];
      if (sIndex.equals(lastAtom)) {
        readNextLine = false;
        break; // end; next structure;
      }
      lastAtom = sIndex;
      if (!addAtoms)
        continue;
      Atom atom = new Atom();
      atom.atomName = tokens[1];
      atom.elementSymbol = getElement(tokens[1]);
      atom.atomSerial = parseIntStr(sIndex);
      if (!filterAtom(atom, i))
        continue;
      setAtomCoordTokens(atom, tokens, 2);
      asc.addAtomWithMappedSerialNumber(atom);
      int n = tokens.length - 5;
      bonds[i] = new int[n + 1];
      bonds[i][n] = atom.atomSerial;
      for (int j = 0; j < n; j++) {
        String t = tokens[j + 5];
        int i2 = parseIntStr(t);
        bonds[i][j] = i2;
        if (checking) {
          // Tinker files may or may not include an atom type in column 6
          if (n == 0 || t.equals(sIndex) || i2 <= 0 || i2 > ac) {
            haveAtomTypes = (n > 0);
            checking = false;
          } else {
            int[] count = htBondCounts.get(t);
            if (count == null)
              htBondCounts.put(t, count = new int[1]);
            if (++count[0] > 10) // even 10 is quite many bonds!
              haveAtomTypes = !(checking = false);
          }
        }
      }
    }
    if (addAtoms) {
      makeBonds(bonds, !checking && haveAtomTypes);
      applySymmetryAndSetTrajectory();
    }
    return readNextLine;
  }

  private void makeBonds(int[][] bonds, boolean haveAtomTypes) {
    Atom[] atoms = asc.atoms;
    for (int i = bonds.length; --i >= 0;) {
      int[] b = bonds[i];
      if (b == null)
        continue; // discarded atom
      Atom a1 = atoms[asc.getAtomIndexFromSerial(b[b.length - 1])];
      int b0 = 0;
      if (haveAtomTypes)
        a1.atomName += "\0" + (b[b0++]);
      for (int j = b.length - 1; --j >= b0;)
        if (b[j] > i && asc.addNewBondWithOrder(a1.index, asc.getAtomIndexFromSerial(b[j]), 1) != null)
            haveBonds = true;
    }
  }

  private String getElement(String name) {
    int n = name.length();
    switch (n) {
    case 1:
      break;
    default:
      char c1 = name.charAt(0);
      char c2 = name.charAt(1);
      n = (Atom.isValidElementSymbol2(c1, c2) || c1 == 'C' && c2 == 'L' ? 2 : 1);
    }
    return name.substring(0, n);
  }
}
