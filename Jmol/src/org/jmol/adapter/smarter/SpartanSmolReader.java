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

class SpartanSmolReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("spartan .smol");

    try {
      discardLinesUntilStartsWith(reader, "BEGINOUTPUT");
      if (discardLinesUntilContains(reader, "Standard Nuclear Orientation (Ang") !=
          null)
        readAtoms(reader);
      if (discardLinesUntilContains(reader, "VIBRATIONAL FREQUENCIES") !=
          null)
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

  void readAtoms(BufferedReader reader) throws Exception {
    discardLines(reader, 2);
    String line;
    System.out.println("Reading atoms...");
    while ((line = reader.readLine()) != null &&
           (/*atomNum = */parseInt(line, 0, 5)) > 0) {
      System.out.println("atom: " + line);
      String elementSymbol = parseToken(line, 10, 12);
      float x = parseFloat(line, 17, 30);
      float y = parseFloat(line, 31, 43);
      float z = parseFloat(line, 44, 58);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.x = x;
      atom.y = y;
      atom.z = z;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    discardLinesUntilBlank(reader);
    
    int totalFrequencyCount = 0;
    while (true) {
      String line = discardLinesUntilNonBlank(reader);
      int lineBaseFreqCount = totalFrequencyCount;
      //      System.out.println("lineBaseFreqCount=" + lineBaseFreqCount);
      ichNextParse = 16;
      int lineFreqCount;
      line = line.substring(13); // skip the " Frequency:"
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
        //        System.out.println("frequency=" + frequency);
        if (Float.isNaN(frequency))
          break; //////////////// loop exit is here
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
      }
      if (lineFreqCount == 0)
        return;
      Atom[] atoms = atomSetCollection.atoms;
      discardLines(reader, 2);
      int firstAtomSetAtomCount = atomSetCollection.getFirstAtomSetAtomCount();
      for (int i = 0; i < firstAtomSetAtomCount; ++i) {
        line = reader.readLine();
        for (int j = 0; j < lineFreqCount; ++j) {
          int ichCoords = j * 23 + 10;
          float x = parseFloat(line, ichCoords,      ichCoords +  7);
          float y = parseFloat(line, ichCoords +  7, ichCoords + 14);
          float z = parseFloat(line, ichCoords + 14, ichCoords + 21);
          int atomIndex = (lineBaseFreqCount + j) * firstAtomSetAtomCount + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = x;
          atom.vectorY = y;
          atom.vectorZ = z;
          //          System.out.println("x=" + x + " y=" + y + " z=" + z);
        }
      }
    }
  }
}
