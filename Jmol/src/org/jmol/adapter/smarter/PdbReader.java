/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.jmol.api.ModelAdapter;

import java.io.BufferedReader;
import java.util.StringTokenizer;

class PdbReader extends ModelReader {
  String line;
  int lineLength;
  // index into atoms array + 1
  // so that 0 can be used for the null value
  int currentModelNumber;
  int[] serialMap = new int[512];

  boolean isNMRdata;

  Model readModel(BufferedReader reader) throws Exception {

    model = new Model(ModelAdapter.MODEL_TYPE_PDB);

    model.pdbStructureRecords = new String[32];
    model.fileHeader = "";
    boolean accumulatingHeader = true;
    while ((line = reader.readLine()) != null) {
      lineLength = line.length();
      if (line.startsWith("ATOM  ") ||
          line.startsWith("HETATM")) {
        atom();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("CONECT")) {
        conect();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("HELIX ") ||
          line.startsWith("SHEET ") ||
          line.startsWith("TURN  ")) {
        structure();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("MODEL ")) {
        model();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("CRYST1")) {
        cryst1();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE1")) {
        scale1();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE2")) {
        scale2();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("SCALE3")) {
        scale3();
        accumulatingHeader = false;
        continue;
      }
      if (line.startsWith("EXPDTA")) {
        expdta();
        continue;
      }
      if (line.startsWith("HEADER") && lineLength >= 66) {
        model.setModelName(line.substring(62, 66));
        continue;
      }
      if (accumulatingHeader) {
        model.fileHeader += line + '\n';
      }
    }
    serialMap = null;
    if (isNMRdata)
      model.notionalUnitcell =
        model.pdbScaleMatrix = model.pdbScaleTranslate = null;
    return model;
  }

  void atom() {
    try {
      // for now, we are only taking alternate location 'A'
      char charAlternateLocation = line.charAt(16);
      if (charAlternateLocation != ' ' && charAlternateLocation != 'A')
        return;
      int len = lineLength;
      /****************************************************************
       * extract elementSymbol
       ****************************************************************/
      String elementSymbol = null;
      if (len >= 78) {
        String candidate = line.substring(76, 78).trim();
        int candidateLength = candidate.length();
        if (candidateLength > 0) {
          char chFirst = candidate.charAt(0);
          if (Atom.isValidFirstSymbolChar(chFirst) &&
              (candidateLength == 1 ||
               (candidateLength == 2 &&
                Atom.isValidSecondSymbolChar(chFirst, candidate.charAt(1)))))
            elementSymbol = candidate;
        }
      }
      if (elementSymbol == null) {
        char ch12 = line.charAt(12);
        char ch13 = line.charAt(13);
        if (Atom.isValidFirstSymbolChar(ch12)) {
          if (Atom.isValidSecondSymbolChar(ch12, ch13))
            elementSymbol = "" + ch12 + ch13;
          else
            elementSymbol = "" + ch12;
        } else if (Atom.isValidFirstSymbolChar(ch13))
          elementSymbol = "" + ch13;
        else
          elementSymbol = "Xx";
      }
      /****************************************************************
       * atomName
       ****************************************************************/
      String rawAtomName = line.substring(12, 16);
      String atomName = rawAtomName.trim();
      /****************************************************************
       * calculate the charge from cols 79 & 80 (1-based)
       * 2+, 3-, etc
       ****************************************************************/
      int charge = 0;
      if (len >= 80) {
        char chMag = line.charAt(78);
        char chSign = line.charAt(79);
        if (chMag >= '0' && chMag <= '7' &&
            (chSign == '+' || chSign == '-' || chSign == ' ')) {
          charge = chMag - '0';
          if (chSign == '-')
            charge = -charge;
        }
      }

      /****************************************************************
       * read the bfactor from cols 61-66 (1-based)
       ****************************************************************/
      float bfactor = parseFloat(line, 60, 66);

      /****************************************************************
       * read the occupancy from cols 55-60 (1-based)
       * should be in the range 0.00 - 1.00
       ****************************************************************/
      int occupancy = 100;
      float floatOccupancy = parseFloat(line, 54, 60);
      if (floatOccupancy != Float.NaN)
        occupancy = (int)(floatOccupancy * 100);
      
      /****************************************************************/
      int serial = parseInt(line, 6, 11);
      /****************************************************************
       * coordinates
       ****************************************************************/
      float x = parseFloat(line, 30, 38);
      float y = parseFloat(line, 38, 46);
      float z = parseFloat(line, 46, 54);
      /****************************************************************/
      if (serial >= serialMap.length) {
        int[] t = new int[serial + 500];
        System.arraycopy(serialMap, 0, t, 0, serialMap.length);
        serialMap = t;
      }
      Atom atom = model.newAtom();
      atom.modelNumber = currentModelNumber;
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.charge = charge;
      atom.occupancy = occupancy;
      atom.bfactor = bfactor;
      atom.x = x; atom.y = y; atom.z = z;
      atom.pdbAtomRecord = line;

      // note that values are +1 in this serial map
      serialMap[serial] = model.atomCount;
    } catch (NumberFormatException e) {
      logger.log("bad record", "" + line);
    }
  }

  void conect() {
    int sourceSerial = -1;
    int sourceIndex = -1;
    try {
      sourceSerial = parseInt(line, 6, 11);
      sourceIndex = serialMap[sourceSerial] - 1;
      if (sourceIndex < 0)
        return;
      // use this for HBONDS
      for (int i = 0; i < 9; i += (i == 5 ? 2 : 1)) {
      //      for (int i = 0; i < 4; i += (i == 5 ? 2 : 1)) {
        int targetSerial = getTargetSerial(i);
        if (targetSerial < 0)
          continue;
        int targetIndex = serialMap[targetSerial] - 1;
        if (targetIndex < 0)
          continue;
        if (model.bondCount > 0) {
          Bond bond = model.bonds[model.bondCount - 1];
          if (i < 4 &&
              bond.atomIndex1 == sourceIndex &&
              bond.atomIndex2 == targetIndex) {
            ++bond.order;
            continue;
          }
        }
        if (i >= 4)
          logger.log("hbond:" + sourceIndex + "->" + targetIndex);
        model.addBond(new Bond(sourceIndex, targetIndex,
                               i < 4
                               ? 1 : ModelAdapter.ORDER_HBOND));
      }
    } catch (Exception e) {
    }
  }

  int getTargetSerial(int i) {
    int offset = i * 5 + 11;
    int offsetEnd = offset + 5;
    if (offsetEnd <= lineLength)
      return parseInt(line, offset, offsetEnd);
    return Integer.MIN_VALUE;
  }

  void structure() {
    if (model.pdbStructureRecordCount == model.pdbStructureRecords.length) {
      String[] t = new String[2 * model.pdbStructureRecordCount];
      System.arraycopy(model.pdbStructureRecords, 0, t, 0,
                       model.pdbStructureRecordCount);
      model.pdbStructureRecords = t;
    }
    model.pdbStructureRecords[model.pdbStructureRecordCount++] = line;
  }

  void model() {
    /****************************************************************
     * mth 2004 02 28
     * note that the pdb spec says:
     * COLUMNS       DATA TYPE      FIELD         DEFINITION
     * ----------------------------------------------------------------------
     *  1 -  6       Record name    "MODEL "
     * 11 - 14       Integer        serial        Model serial number.
     *
     * but I received a file with the serial
     * number right after the word MODEL :-(
     ****************************************************************/
    try {
      int startModelColumn = 6; // should be 10 0-based
      int endModelColumn = 14;
      if (endModelColumn > lineLength)
        endModelColumn = lineLength;
      int modelNumber = parseInt(line, startModelColumn, endModelColumn);
      if (modelNumber != currentModelNumber + 1)
        logger.log("Model number sequence seems confused");
      currentModelNumber = modelNumber;
    } catch (NumberFormatException e) {
    }
  }

  void cryst1() {
    try {
      float a = getFloat( 6, 9);
      float b = getFloat(15, 9);
      float c = getFloat(24, 9);
      float alpha = getFloat(33, 7);
      float beta  = getFloat(40, 7);
      float gamma = getFloat(47, 7);
      float[] notionalUnitcell = model.notionalUnitcell = new float[6];
      notionalUnitcell[0] = a;
      notionalUnitcell[1] = b;
      notionalUnitcell[2] = c;
      notionalUnitcell[3] = alpha;
      notionalUnitcell[4] = beta;
      notionalUnitcell[5] = gamma;
    } catch (Exception e) {
    }
  }

  float getFloat(int ich, int cch) throws Exception {
    return parseFloat(line, ich, ich+cch);
  }

  void scale(int n) throws Exception {
    model.pdbScaleMatrix[n*3 + 0] = getFloat(10, 10);
    model.pdbScaleMatrix[n*3 + 1] = getFloat(20, 10);
    model.pdbScaleMatrix[n*3 + 2] = getFloat(30, 10);
    float translation = getFloat(45, 10);
    if (translation != 0) {
      if (model.pdbScaleTranslate == null)
        model.pdbScaleTranslate = new float[3];
      model.pdbScaleTranslate[n] = translation;
    }
  }

  void scale1() {
    try {
      model.pdbScaleMatrix = new float[9];
      scale(0);
    } catch (Exception e) {
      model.pdbScaleMatrix = null;
      logger.log("scale1 died:" + 3);
    }
  }

  void scale2() {
    try {
      scale(1);
    } catch (Exception e) {
      model.pdbScaleMatrix = null;
      logger.log("scale2 died");
    }
  }

  void scale3() {
    try {
      scale(2);
    } catch (Exception e) {
      model.pdbScaleMatrix = null;
      logger.log("scale3 died");
    }
  }

  void expdta() {
    String technique = line.substring(10).trim().toLowerCase();
    if (technique.regionMatches(true, 0, "nmr", 0, 3))
      isNMRdata = true;
  }
}
