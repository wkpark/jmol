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
import java.util.StringTokenizer;

class GaussianReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("gaussian");

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("Standard orientation:") >= 0) {
          // we only take the last set of atoms before the frequencies
          model.discardPreviousAtoms();
          readAtoms(reader);
        } else if (line.startsWith(" Harmonic frequencies")) {
          readFrequencies(reader);
          break;
        }
      }
    } catch (Exception ex) {
      model.errorMessage = "Could not read file:" + ex;
      return model;
    }
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }
    
  void readAtoms(BufferedReader reader) throws Exception {
    discardLines(reader, 4);
    String line;
    while ((line = reader.readLine()) != null &&
           !line.startsWith(" --")) {
      String centerNumber = parseToken(line, 0, 5);
      int elementNumber = parseInt(line, 13);
      if (elementNumber <= 0 || elementNumber > 110)
        continue;
      // these are the Gaussian 98 offsets
      // others seem to have different values
      float x = parseFloat(line, 34, 46);
      float y = parseFloat(line, 46, 58);
      float z = parseFloat(line, 58, 70);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        continue;
      Atom atom = model.newAtom();
      atom.elementNumber = (byte)elementNumber;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  int baseAtomCount;
  void readFrequencies(BufferedReader reader) throws Exception {
    discardLines(reader, 4);
    baseAtomCount = model.atomCount;
    int modelNumber = 1;
    String line;
    while ((line = reader.readLine()) != null &&
           line.startsWith(" Frequencies --")) {
      discardLines(reader, 6);
      for (int i = 0; i < baseAtomCount; ++i) {
        line = reader.readLine();
        int atomCenterNumber = parseInt(line, 0, 4);
        for (int j = 0, col = 11; j < 3; ++j, col += 23) {
          float x = parseFloat(line, col     , col +  6);
          float y = parseFloat(line, col +  7, col + 13);
          float z = parseFloat(line, col + 14, col + 20);
          recordAtomVector(modelNumber + j, atomCenterNumber, x, y, z);
        }
      }
     discardLines(reader, 2);
      modelNumber += 3;
    }
  }

  void recordAtomVector(int modelNumber, int atomCenterNumber,
                        float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return;
    if (atomCenterNumber <= 0 || atomCenterNumber > baseAtomCount)
      return;
    if (atomCenterNumber == 1) {
      if (modelNumber > 1)
        createNewModel(modelNumber);
    }
    Atom atom = model.atoms[(modelNumber - 1) * baseAtomCount +
                            atomCenterNumber - 1];
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }

  void createNewModel(int modelNumber) {
    System.out.println("creating new model:" + modelNumber);
    Atom[] atoms = model.atoms;
    for (int i = 0; i < baseAtomCount; ++i) {
      Atom atomNew = model.newCloneAtom(atoms[i]);
      atomNew.modelNumber = modelNumber;
    }
  }

  void discardLines(BufferedReader reader, int nLines) throws Exception {
    System.out.println("discardLines:");
    for (int i = nLines; --i >= 0; )
      System.out.println(reader.readLine());
  }
  
}
