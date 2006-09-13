/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

import org.jmol.util.Logger;

/**
 * Jaguar reader tested for the two samples files in CVS. Both
 * these files were created with Jaguar version 4.0, release 20.
 */
class JaguarReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("jaguar");

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("  final geometry:")) {
          readAtoms(reader);
        } else if (line.startsWith("  harmonic frequencies in")) {
          readFrequencies(reader);
          break;
        }
      }
    } catch (Exception ex) {
      Logger.error("Could not read file", ex);
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      return atomSetCollection;
    }
    if (atomSetCollection.atomCount == 0) {
      atomSetCollection.errorMessage = "No atoms in file";
    }
    return atomSetCollection;
  }

  void readAtoms(BufferedReader reader) throws Exception {
    // we only take the last set of atoms before the frequencies
    atomSetCollection.discardPreviousAtoms();
    // start parsing the atoms
    discardLines(reader, 2);
    String line;
    while ((line = reader.readLine()) != null &&
           line.length() >= 60 &&
           line.charAt(2) != ' ') {
      String atomName = parseToken(line, 2, 7);
      float x = parseFloat(line,  8, 24);
      float y = parseFloat(line, 26, 42);
      float z = parseFloat(line, 44, 60);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        return;
      int len = atomName.length();
      if (len < 2)
        return;
      String elementSymbol;
      char ch2 = atomName.charAt(1);
      if (ch2 >= 'a' && ch2 <= 'z')
        elementSymbol = atomName.substring(0, 2);
      else
        elementSymbol = atomName.substring(0, 1);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  /* A block without symmetry, looks like:

  harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
  
  frequencies  1350.52  1354.79  1354.91  1574.28  1577.58  3047.10  3165.57
  intensities    14.07    13.95    13.92     0.00     0.00     0.00    25.19
    C1   X     0.00280 -0.11431  0.01076 -0.00008 -0.00001 -0.00028 -0.00406
    C1   Y    -0.00528  0.01062  0.11423 -0.00015 -0.00001 -0.00038  0.00850
    C1   Z     0.11479  0.00330  0.00502 -0.00006  0.00000  0.00007 -0.08748
    
    With symmetry:
    
  harmonic frequencies in cm**-1, IR intensities in km/mol, and normal modes:
  
  frequencies  1352.05  1352.11  1352.16  1574.91  1574.92  3046.33  3164.52
  symmetries   B3       B1       B3       A        A        A        B1      
  intensities    14.01    14.00    14.00     0.00     0.00     0.00    25.06
    C1   X     0.08399 -0.00233 -0.07841  0.00000  0.00000  0.00000 -0.01133
    C1   Y     0.06983 -0.05009  0.07631 -0.00001  0.00000  0.00000 -0.00283
    C1   Z     0.03571  0.10341  0.03519  0.00001  0.00000  0.00001 -0.08724
  */
  int atomCount;
  
  void readFrequencies(BufferedReader reader) throws Exception {
    atomCount = atomSetCollection.getFirstAtomSetAtomCount();
    int modelNumber = 1;
    String line;
    while ((line = reader.readLine()) != null &&
           ! line.startsWith("  frequencies ")) {
    }
    if (line == null)
      return;
    // determine number of freqs on this line (starting with "frequencies")
    do {
      int freqCount = new StringTokenizer(line).countTokens() - 1;
      while ((line = reader.readLine()) != null &&
           ! line.startsWith("  intensities ")) {
      }
      for (int atomCenterNumber = 0; atomCenterNumber < atomCount; atomCenterNumber++) {
        // this assumes that the atoms are given in the same order as their
        // atomic coordinates, and disregards the label which is should use
        line = reader.readLine();
        StringTokenizer tokenizerX = new StringTokenizer(line);
        tokenizerX.nextToken(); tokenizerX.nextToken(); // disregard label and X/Y/Z
        StringTokenizer tokenizerY = new StringTokenizer(reader.readLine());
        tokenizerY.nextToken(); tokenizerY.nextToken();
        StringTokenizer tokenizerZ = new StringTokenizer(reader.readLine());
        tokenizerZ.nextToken(); tokenizerZ.nextToken();
        for (int j = 0; j < freqCount; j++) {
          float x = parseFloat(tokenizerX.nextToken());
          float y = parseFloat(tokenizerY.nextToken());
          float z = parseFloat(tokenizerZ.nextToken());
          recordAtomVector(modelNumber + j, atomCenterNumber, x, y, z);
        }
      }
      discardLines(reader, 1);
      modelNumber += freqCount;
    } while ((line = reader.readLine()) != null &&
             (line.startsWith("  frequencies ")));
  }

  void recordAtomVector(int modelNumber, int atomCenterNumber,
                        float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return; // line is too short -- no data found
    if (atomCenterNumber <= 0 || atomCenterNumber > atomCount)
      return;
    if (atomCenterNumber == 1) {
      if (modelNumber > 1)
        atomSetCollection.cloneFirstAtomSet();
    }
    Atom atom = atomSetCollection.atoms[(modelNumber - 1) * atomCount +
                            atomCenterNumber - 1];
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }

  void discardLines(BufferedReader reader, int nLines) throws Exception {
    for (int i = nLines; --i >= 0; )
      reader.readLine();
  }
}
