/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-11 23:56:13 -0500 (Mon, 11 Sep 2006) $
 * $Revision: 5499 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;


import java.io.BufferedReader;
import java.util.Hashtable;

/*
 * perhaps unnecessary ? I think this was for when all you had was 
 * a piece of the archive file that started with:
 * Spartan '04 Quantum Mechanics Program:  (PC/x86)           Release  121
 * 
 * but now we can read SMOL files, so this should not be necessary 
 * 
 * no bonds here.
 * 
 */

public class SpartanReader extends AtomSetCollectionReader {

  String modelName = "Spartan file";
  int atomCount;
  Hashtable moData = new Hashtable();

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("spartan");
    String cartesianHeader = "Cartesian Coordinates (Ang";
    try {
      if (isSpartanArchive(cartesianHeader)) {
        SpartanArchive spartanArchive = new SpartanArchive(this,
            atomSetCollection, moData);
        atomCount = spartanArchive.readArchive(line, true);
        if (atomCount > 0)
          atomSetCollection.setAtomSetName(modelName);
      } else if (line.indexOf(cartesianHeader)>=0){
          readAtoms();
          discardLinesUntilContains("Vibrational Frequencies");
        if (line != null)
          readFrequencies();
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  boolean isSpartanArchive(String strNotArchive)
      throws Exception {
    String lastLine = "";
    while (readLine() != null) {
      if (line.equals("GEOMETRY")) {
        line = lastLine;
        return true;
      }
      if (line.indexOf(strNotArchive) >= 0)
        return false;
      lastLine = line;
    }
    return false;
  }

  void readAtoms() throws Exception {
    discardLinesUntilBlank();
    while (readLine() != null
        && (/*atomNum = */parseInt(line, 0, 3)) > 0) {
      String elementSymbol = parseToken(line, 4, 6);
      String atomName = parseToken(line, 7, 13);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.set(parseFloat(line, 17, 30), parseFloat(line, 31, 44), parseFloat(line, 45, 58));
    }
  }

  void readFrequencies() throws Exception {
    int totalFrequencyCount = 0;

    while (true) {
      discardLinesUntilNonBlank();
      int lineBaseFreqCount = totalFrequencyCount;
      next[0] = 16;
      int lineFreqCount;
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat();
        if (Float.isNaN(frequency))
          break; //////////////// loop exit is here
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          atomSetCollection.cloneFirstAtomSet();
      }
      if (lineFreqCount == 0)
        return;
      Atom[] atoms = atomSetCollection.getAtoms();
      discardLines(2);
      int firstAtomSetAtomCount = atomSetCollection.getFirstAtomSetAtomCount();
      for (int i = 0; i < firstAtomSetAtomCount; ++i) {
        readLine();
        for (int j = 0; j < lineFreqCount; ++j) {
          int ichCoords = j * 23 + 10;
          float x = parseFloat(line, ichCoords, ichCoords + 7);
          float y = parseFloat(line, ichCoords + 7, ichCoords + 14);
          float z = parseFloat(line, ichCoords + 14, ichCoords + 21);
          int atomIndex = (lineBaseFreqCount + j) * firstAtomSetAtomCount + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = x;
          atom.vectorY = y;
          atom.vectorZ = z;
        }
      }
    }
  }
}
