/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.adapter.smarter;

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

class QchemReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("qchem");

    try {
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("Standard Nuclear Orientation") >= 0) {
          readAtoms(reader);
        } else if (line.indexOf("VIBRATIONAL FREQUENCIES") >= 0) {
          readFrequencies(reader);
          break;
        } else if (line.indexOf("Mulliken Net Atomic Charges") >= 0){
          readPartialCharges(reader);
        } 
        ++lineNum;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      return atomSetCollection;
    }
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

/* Q-chem 2.1 format:
       Standard Nuclear Orientation (Angstroms)
    I     Atom         X            Y            Z
 ----------------------------------------------------
    1      H       0.000000     0.000000     4.756791
*/

  // offset of coordinates within 'Standard Nuclear Orientation:'
  int coordinateBase = 16;
  // number of lines to skip after 'Frequencies:' to get to the vectors
  int frequencyLineSkipCount = 4;

  int atomCount;

  void readAtoms(BufferedReader reader) throws Exception {
    // we only take the last set of atoms before the frequencies
    atomSetCollection.discardPreviousAtoms();
    atomCount = 0;
    discardLines(reader, 2);
    String line;
    while ((line = reader.readLine()) != null &&
           !line.startsWith(" --")) {
    /*String centerNumber = */parseToken(line, 0, 5);
    String aname = parseToken(line, 6, 12);
    if (aname.indexOf("X") == 1) {
      // skip dummy atoms
      continue;
    }
      
      //q-chem specific offsets
      float x = parseFloat(line, coordinateBase     , coordinateBase + 13);
      float y = parseFloat(line, coordinateBase + 13, coordinateBase + 26);
      float z = parseFloat(line, coordinateBase + 26, coordinateBase + 39);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = aname;
      atom.x = x; atom.y = y; atom.z = z;
      ++atomCount;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    int modelNumber = 1;
    String line;
    while ((line = reader.readLine()) != null &&
           ! line.startsWith(" Frequency:")) {
    }
    if (line == null)
      return;
    do {
      // FIXME  We'll want to read in the frequency of the vibration
      // at some point
      discardLines(reader, frequencyLineSkipCount);
      for (int i = 0; i < atomCount; ++i) {
        line = reader.readLine();
        for (int j = 0, col = 12; j < 3; ++j, col += 23) {
          float x = parseFloat(line, col     , col +  5);
          float y = parseFloat(line, col +  7, col + 12);
          float z = parseFloat(line, col + 14, col + 19);

          recordAtomVector(modelNumber + j, i+1, x, y, z);
        }
      }
     discardLines(reader, 1);
     modelNumber += 3;
    } while ((line = reader.readLine()) != null &&
             line.startsWith(" Frequency:"));
  }

  void recordAtomVector(int modelNumber, int atomCenterNumber,
                        float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return; // no data found
    if (atomCenterNumber <= 0 || atomCenterNumber > atomCount)
      return;
    if (atomCenterNumber == 1 && modelNumber > 1)
      atomSetCollection.cloneFirstAtomSet();
    
    Atom atom = atomSetCollection.atoms[(modelNumber - 1) * atomCount +
                            atomCenterNumber - 1];
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }

  void readPartialCharges(BufferedReader reader) throws Exception {
    discardLines(reader, 3);
    String line;
    for (int i = 0;
         i < atomCount && (line = reader.readLine()) != null;
         ++i)
      atomSetCollection.atoms[i].partialCharge = parseFloat(line, 29, 38);
  }
}
