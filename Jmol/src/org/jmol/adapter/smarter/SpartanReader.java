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

class SpartanReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("spartan");

    try {
      String line;
      if (discardLinesUntilContains(reader, "Cartesian Coordinates (Ang") !=
          null)
        readAtoms(reader);
      if (discardLinesUntilContains(reader, "Vibrational Frequencies") !=
          null)
        readFrequencies(reader);
    } catch (Exception ex) {
      ex.printStackTrace();
      model.errorMessage = "Could not read file:" + ex;
      return model;
    }
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  void readAtoms(BufferedReader reader) throws Exception {
    discardLinesUntilBlank(reader);
    String line;
    int atomNum;
    while ((line = reader.readLine()) != null &&
           (atomNum = parseInt(line, 0, 3)) > 0) {
      String elementSymbol = parseToken(line, 4, 6);
      String atomName = parseToken(line, 7, 13);
      float x = parseFloat(line, 17, 30);
      float y = parseFloat(line, 31, 44);
      float z = parseFloat(line, 45, 58);
      Atom atom = model.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.x = x;
      atom.y = y;
      atom.z = z;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    int totalFrequencyCount = 0;
    atomCountInFirstModel = model.atomCount;
    float[] frequencies = new float[5];
    float[] xComponents = new float[5];
    float[] yComponents = new float[5];
    float[] zComponents = new float[5];

    while (true) {
      String line = discardLinesUntilNonBlank(reader);
      int lineBaseFreqCount = totalFrequencyCount;
      System.out.println("lineBaseFreqCount=" + lineBaseFreqCount);
      ichNextParse = 16;
      int lineFreqCount;
      for (lineFreqCount = 0; lineFreqCount < 3; ++lineFreqCount) {
        float frequency = parseFloat(line, ichNextParse);
        //        System.out.println("frequency=" + frequency);
        if (Float.isNaN(frequency))
          break; //////////////// loop exit is here
        ++totalFrequencyCount;
        if (totalFrequencyCount > 1)
          createNewModel(totalFrequencyCount);
      }
      if (lineFreqCount == 0)
        return;
      Atom[] atoms = model.atoms;
      discardLines(reader, 2);
      for (int i = 0; i < atomCountInFirstModel; ++i) {
        line = reader.readLine();
        for (int j = 0; j < lineFreqCount; ++j) {
          int ichCoords = j * 23 + 10;
          float x = parseFloat(line, ichCoords,      ichCoords +  7);
          float y = parseFloat(line, ichCoords +  7, ichCoords + 14);
          float z = parseFloat(line, ichCoords + 14, ichCoords + 21);
          int atomIndex = (lineBaseFreqCount + j) * atomCountInFirstModel + i;
          Atom atom = atoms[atomIndex];
          atom.vectorX = x;
          atom.vectorY = y;
          atom.vectorZ = z;
          System.out.println("x=" + x + " y=" + y + " z=" + z);
        }
      }
    }
  }

  int atomCountInFirstModel;
  
  void createNewModel(int modelNumber) {
    //    System.out.println("createNewModel(" + modelNumber + ")");
    Atom[] atoms = model.atoms;
    for (int i = 0; i < atomCountInFirstModel; ++i) {
      Atom atomNew = model.newCloneAtom(atoms[i]);
      atomNew.modelNumber = modelNumber;
    }
  }
}
