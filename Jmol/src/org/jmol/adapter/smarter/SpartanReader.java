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
import java.util.Hashtable;

import org.jmol.util.Logger;

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

class SpartanReader extends AtomSetCollectionReader {

  String modelName = "Spartan file";
  int atomCount;
  Hashtable moData = new Hashtable();

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {

    atomSetCollection = new AtomSetCollection("spartan");
    String cartesianHeader = "Cartesian Coordinates (Ang";
    try {
      if (isSpartanArchive(reader, cartesianHeader)) {
        SpartanArchive spartanArchive = new SpartanArchive(this, logger,
            atomSetCollection, moData);
        atomCount = spartanArchive.readArchive(reader, line, true);
        if (atomCount > 0)
          atomSetCollection.setAtomSetName(modelName);
      } else if (line.indexOf(cartesianHeader)>=0){
          readAtoms(reader);
        if (discardLinesUntilContains(reader, "Vibrational Frequencies") != null)
          readFrequencies(reader);
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

  boolean isSpartanArchive(BufferedReader reader, String strNotArchive)
      throws Exception {
    String lastLine = "";
    while ((line = reader.readLine()) != null) {
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

  void readAtoms(BufferedReader reader) throws Exception {
    discardLinesUntilBlank(reader);
    String line;
    while ((line = reader.readLine()) != null
        && (/*atomNum = */parseInt(line, 0, 3)) > 0) {
      String elementSymbol = parseToken(line, 4, 6);
      String atomName = parseToken(line, 7, 13);
      float x = parseFloat(line, 17, 30);
      float y = parseFloat(line, 31, 44);
      float z = parseFloat(line, 45, 58);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.x = x;
      atom.y = y;
      atom.z = z;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    int totalFrequencyCount = 0;

    while (true) {
      String line = discardLinesUntilNonBlank(reader);
      int lineBaseFreqCount = totalFrequencyCount;
      ichNextParse = 16;
      int lineFreqCount;
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
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
