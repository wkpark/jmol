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

class GaussianReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("gaussian");

    try {
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("Standard orientation:") >= 0) {
          readAtoms(reader);
        } else if (line.startsWith(" Harmonic frequencies")) {
          readFrequencies(reader);
          break;
        } else if (line.startsWith(" Total atomic charges:") ||
                   line.startsWith(" Mulliken atomic charges:")) {
          readPartialCharges(reader);
        } else if (lineNum < 20) {
          if (line.indexOf("This is part of the Gaussian 94(TM) system")
              >= 0)
            setGaussian94Offsets();
          else if (line.indexOf("This is part of the Gaussian(R) 98 program.")
                   >= 0)
            setGaussian98Offsets();
          else if (line.indexOf("This is the Gaussian(R) 03 program.")
                   >= 0)
            setGaussian03Offsets();
            
        }
        ++lineNum;
      }
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

  // offset of coordinates within 'Standard orientation:'
  int coordinateBase = 34;

  void setGaussian94Offsets() {
    /*
                   Standard orientation:
 ----------------------------------------------------------
 Center     Atomic              Coordinates (Angstroms)
 Number     Number             X           Y           Z
 ----------------------------------------------------------
    1          6           0.000000    0.000000    1.043880
    */
    coordinateBase = 23;
  }

  void setGaussian98Offsets() {
    /*
                         Standard orientation:                         
 ---------------------------------------------------------------------
 Center     Atomic     Atomic              Coordinates (Angstroms)
 Number     Number      Type              X           Y           Z
 ---------------------------------------------------------------------
    1          6             0        0.852764   -0.020119    0.050711
    */
    coordinateBase = 34;
  }

  void setGaussian03Offsets() {
    coordinateBase = 34;
  }


  int atomCount;
  int modelCount;

  void readAtoms(BufferedReader reader) throws Exception {
    // we only take the last set of atoms before the frequencies
    model.discardPreviousAtoms();
    atomCount = 0;
    modelCount = 1;
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
      float x = parseFloat(line, coordinateBase     , coordinateBase + 12);
      float y = parseFloat(line, coordinateBase + 12, coordinateBase + 24);
      float z = parseFloat(line, coordinateBase + 24, coordinateBase + 36);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        continue;
      Atom atom = model.addNewAtom();
      atom.elementNumber = (byte)elementNumber;
      atom.x = x; atom.y = y; atom.z = z;
      ++atomCount;
    }
  }

  void readFrequencies(BufferedReader reader) throws Exception {
    int modelNumber = 1;
    String line;
    while ((line = reader.readLine()) != null &&
           ! line.startsWith(" Frequencies --"))
      ;
    if (line == null)
      return;
    do {
      // FIXME deal with frequency line here
      discardLinesThroughStartsWith(reader, " Atom AN");
      for (int i = 0; i < atomCount; ++i) {
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
    } while ((line = reader.readLine()) != null &&
             (line.startsWith(" Frequencies --")));
  }

  void recordAtomVector(int modelNumber, int atomCenterNumber,
                        float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return; // line is too short -- no data found
    if (atomCenterNumber <= 0 || atomCenterNumber > atomCount)
      return;
    if (atomCenterNumber == 1) {
      if (modelNumber > 1)
        createNewModel(modelNumber);
    }
    Atom atom = model.atoms[(modelNumber - 1) * atomCount +
                            atomCenterNumber - 1];
    atom.vectorX = x;
    atom.vectorY = y;
    atom.vectorZ = z;
  }

  void readPartialCharges(BufferedReader reader) throws Exception {
    discardLines(reader, 1);
    String line;
    for (int i = 0;
         i < atomCount && (line = reader.readLine()) != null;
         ++i)
      model.atoms[i].partialCharge = parseFloat(line, 9, 18);
  }

  void createNewModel(int modelNumber) {
    modelCount = modelNumber - 1;
    Atom[] atoms = model.atoms;
    for (int i = 0; i < atomCount; ++i) {
      Atom atomNew = model.newCloneAtom(atoms[i]);
      atomNew.modelNumber = modelNumber;
    }
  }

}
