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

class GamessReader extends AtomSetCollectionReader {
    
  final static float angstromsPerBohr = 0.529177f;

  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("gamess");

    try {
      String line;
      discardLinesUntilContains(reader, "COORDINATES (BOHR)");
      readAtomsInBohrCoordinates(reader);
      discardLinesUntilContains(reader, "FREQUENCIES IN CM");
      readFrequencies(reader);
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

  void readAtomsInBohrCoordinates(BufferedReader reader) throws Exception {
    reader.readLine(); // discard one line
    String line;
    String atomName;
    atomSetCollection.newAtomSet();
    while ((line = reader.readLine()) != null &&
           (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 17, 37);
      float y = parseFloat(line, 37, 57);
      float z = parseFloat(line, 57, 77);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = atomName;
      atom.x = x * angstromsPerBohr;
      atom.y = y * angstromsPerBohr;
      atom.z = z * angstromsPerBohr;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    int totalFrequencyCount = 0;
    int atomCountInFirstModel = atomSetCollection.atomCount;
    float[] frequencies = new float[5];
    float[] xComponents = new float[5];
    float[] yComponents = new float[5];
    float[] zComponents = new float[5];

    String line = discardLinesUntilContains(reader, "FREQUENCY:");
    while (line != null && line.indexOf("FREQUENCY:") >= 0) {
      int lineBaseFreqCount = totalFrequencyCount;
      ichNextParse = 17;
      int lineFreqCount;
      for (lineFreqCount = 0; lineFreqCount < 5; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
        //        System.out.println("frequency=" + frequency);
        if (Float.isNaN(frequency))
          break;
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
      }
      Atom[] atoms = atomSetCollection.atoms;
      discardLinesUntilBlank(reader);
      for (int i = 0; i < atomCountInFirstModel; ++i) {
        readComponents(reader.readLine(), lineFreqCount, xComponents);
        readComponents(reader.readLine(), lineFreqCount, yComponents);
        readComponents(reader.readLine(), lineFreqCount, zComponents);
        for (int j = 0; j < lineFreqCount; ++j) {
          int atomIndex = (lineBaseFreqCount + j) * atomCountInFirstModel + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = xComponents[j];
          atom.vectorY = yComponents[j];
          atom.vectorZ = zComponents[j];
        }
      }
      discardLines(reader, 12);
      line = reader.readLine();
    }
  }

  void readComponents(String line, int count, float[] components) {
    for (int i = 0, start = 20; i < count; ++i, start += 12)
      components[i] = parseFloat(line, start, start + 12);
  }
}

