/* $RCSfile$
 * $Author: nicove $
 * $Date: 2006-08-30 13:20:20 -0500 (Wed, 30 Aug 2006) $
 * $Revision: 5447 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;

/**
 * A reader for Q-Chem 2.1
 * Q-Chem  is a quantum chemistry program developed
 * by Q-Chem, Inc. (http://www.q-chem.com/)
 *
 * <p> Molecular coordinates and normal coordinates of
 * vibrations are read. 
 *
 * <p> This reader was developed from a single
 * output file, and therefore, is not guaranteed to
 * properly read all Q-chem output. If you have problems,
 * please contact the author of this code, not the developers
 * of Q-chem.
 *
 * <p> This is a hacked version of Miguel's GaussianReader
 *
 * @author Steven E. Wheeler (swheele2@ccqc.uga.edu)
 * @version 1.0
 */

public class QchemReader extends AtomSetCollectionReader {
    
 public AtomSetCollection readAtomSetCollection(BufferedReader reader)  {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("qchem");
    try {
      int lineNum = 0;
      while (readLine() != null) {
        if (line.indexOf("Standard Nuclear Orientation") >= 0) {
          readAtoms();
        } else if (line.indexOf("VIBRATIONAL FREQUENCIES") >= 0) {
          readFrequencies();
          break;
        } else if (line.indexOf("Mulliken Net Atomic Charges") >= 0){
          readPartialCharges();
        } 
        ++lineNum;
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

/* Q-chem 2.1 format:
       Standard Nuclear Orientation (Angstroms)
    I     Atom         X            Y            Z
 ----------------------------------------------------
    1      H       0.000000     0.000000     4.756791
*/

  int atomCount;

  void readAtoms() throws Exception {
    // we only take the last set of atoms before the frequencies
    atomSetCollection.discardPreviousAtoms();
    atomCount = 0;
    discardLines(2);
    String[] tokens;
    while (readLine() != null && !line.startsWith(" --")) {
      tokens = getTokens();
      if (tokens.length < 5)
        continue;
      String symbol = tokens[1];
      if (JmolAdapter.getElementNumber(symbol) < 1)
        continue;
      //q-chem specific offsets
      float x = parseFloat(tokens[2]);
      float y = parseFloat(tokens[3]);
      float z = parseFloat(tokens[4]);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = symbol;
      atom.set(x, y, z);
      ++atomCount;
    }
  }

  void readFrequencies() throws Exception {
    int modelNumber = 1;
    discardLinesUntilStartsWith(" Frequency:");
    while (line != null && line.startsWith(" Frequency:")) {
      String[] frequencies = getTokens();
      int nModels = frequencies.length - 1;
      discardLines(4);
      for (int i = 0; i < atomCount; ++i) {
        readLine();
        String[] tokens = getTokens();
        for (int j = 0, offset = 0; j < nModels; j++) {
          float x = parseFloat(tokens[++offset]);
          float y = parseFloat(tokens[++offset]);
          float z = parseFloat(tokens[++offset]);
          recordAtomVector(modelNumber + j, i + 1, x, y, z);
        }
      }
      discardLines(1);
      modelNumber += 3;
      readLine();
    }
  }

  void recordAtomVector(int modelNumber, int atomCenterNumber,
                        float x, float y, float z) throws Exception {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return; // no data found
    if (atomCenterNumber <= 0 || atomCenterNumber > atomCount)
      return;
    if (atomCenterNumber == 1 && modelNumber > 1)
      atomSetCollection.cloneFirstAtomSet();
    
    Atom atom = atomSetCollection.getAtom((modelNumber - 1) * atomCount +
                            atomCenterNumber - 1);
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }

  void readPartialCharges() throws Exception {
    discardLines(3);
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = 0; i < atomCount && readLine() != null; ++i)
      atoms[i].partialCharge = parseFloat(getTokens()[2]);
  }
}
