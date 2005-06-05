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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;
import java.util.Hashtable;

class PdbReader extends AtomSetCollectionReader {
  String line;
  int lineLength;
  // index into atoms array + 1
  // so that 0 can be used for the null value
  int[] serialMap = new int[512];

  boolean isNMRdata;

  final Hashtable htFormul = new Hashtable();

  String currentGroup3;
  Hashtable htElementsInCurrentGroup;

  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("pdb");

    atomSetCollection.pdbStructureRecords = new String[32];
    initialize();
    while ((line = reader.readLine()) != null) {
      lineLength = line.length();
      if (line.startsWith("ATOM  ") ||
          line.startsWith("HETATM")) {
        atom();
        continue;
      }
      if (line.startsWith("CONECT")) {
        conect();
        continue;
      }
      if (line.startsWith("HELIX ") ||
          line.startsWith("SHEET ") ||
          line.startsWith("TURN  ")) {
        structure();
        continue;
      }
      if (line.startsWith("MODEL ")) {
        model();
        continue;
      }
      if (line.startsWith("CRYST1")) {
        cryst1();
        continue;
      }
      if (line.startsWith("SCALE1")) {
        scale1();
        continue;
      }
      if (line.startsWith("SCALE2")) {
        scale2();
        continue;
      }
      if (line.startsWith("SCALE3")) {
        scale3();
        continue;
      }
      if (line.startsWith("EXPDTA")) {
        expdta();
        continue;
      }
      if (line.startsWith("FORMUL")) {
        formul();
        continue;
      }
      if (line.startsWith("HEADER") && lineLength >= 66) {
        atomSetCollection.setCollectionName(line.substring(62, 66));
        continue;
      }
    }
    serialMap = null;
    if (isNMRdata)
      atomSetCollection.notionalUnitcell =
        atomSetCollection.pdbScaleMatrix = atomSetCollection.pdbScaleTranslate = null;
    return atomSetCollection;
  }

  void initialize() {
    htFormul.clear();
    currentGroup3 = null;
  }

  void atom() {
    boolean isHetero = line.startsWith("HETATM");
    try {
      // for now, we are only taking alternate location 'A'
      char charAlternateLocation = line.charAt(16);
      //      if (charAlternateLocation != ' ' && charAlternateLocation != 'A')
      //        return;
      ////////////////////////////////////////////////////////////////
      // get the group so that we can check the formul
      int serial = parseInt(line, 6, 11);
      char chainID = line.charAt(21);
      int sequenceNumber = parseInt(line, 22, 26);
      char insertionCode = line.charAt(26);
      String group3 = parseToken(line, 17, 20);
      if (group3 == null) {
        currentGroup3 = group3;
        htElementsInCurrentGroup = null;
      } else if (! group3.equals(currentGroup3)) {
        currentGroup3 = group3;
        htElementsInCurrentGroup = (Hashtable)htFormul.get(group3);
      }

      ////////////////////////////////////////////////////////////////
      // extract elementSymbol
      String elementSymbol = deduceElementSymbol();

      /****************************************************************
       * atomName
       ****************************************************************/
      String rawAtomName = line.substring(12, 16);
      // confusion|concern about the effect this will have on
      // atom expressions
      // but we have to do it to support mmCIF
      String atomName = rawAtomName.trim();
      /****************************************************************
       * calculate the charge from cols 79 & 80 (1-based)
       * 2+, 3-, etc
       ****************************************************************/
      int charge = 0;
      if (lineLength >= 80) {
        char chMagnitude = line.charAt(78);
        char chSign = line.charAt(79);
        if (chSign >= '0' && chSign <= '7') {
          char chT = chSign;
          chSign = chMagnitude;
          chMagnitude = chT;
        }
        if ((chSign == '+' || chSign == '-' || chSign == ' ') &&
            chMagnitude >= '0' && chMagnitude <= '7') {
          charge = chMagnitude - '0';
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

      /****************************************************************
       * coordinates
       ****************************************************************/
      float x = parseFloat(line, 30, 38);
      float y = parseFloat(line, 38, 46);
      float z = parseFloat(line, 46, 54);
      /****************************************************************/
      if (serial >= serialMap.length)
        serialMap = setLength(serialMap, serial + 500);
      Atom atom = new Atom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.alternateLocationID = charAlternateLocation;
      atom.formalCharge = charge;
      atom.occupancy = occupancy;
      atom.bfactor = bfactor;
      atom.x = x; atom.y = y; atom.z = z;
      atom.isHetero = isHetero;
      atom.chainID = chainID;
      atom.atomSerial = serial;
      atom.group3 = currentGroup3;
      atom.sequenceNumber = sequenceNumber;
      atom.insertionCode = JmolAdapter.canonizeInsertionCode(insertionCode);

      atomSetCollection.addAtom(atom);
      // note that values are +1 in this serial map
      serialMap[serial] = atomSetCollection.atomCount;
    } catch (NumberFormatException e) {
      logger.log("bad record", "" + line);
    }
  }

  String deduceElementSymbol() {
    if (lineLength >= 78) {
      char ch76 = line.charAt(76);
      char ch77 = line.charAt(77);
      if (ch76 == ' ' && Atom.isValidElementSymbol(ch77))
        return "" + ch77;
      if (Atom.isValidElementSymbolNoCaseSecondChar(ch76, ch77))
        return "" + ch76 + ch77;
    }
    char ch12 = line.charAt(12);
    char ch13 = line.charAt(13);
    if ((htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get(line.substring(12, 14)) != null) &&
        Atom.isValidElementSymbolNoCaseSecondChar(ch12, ch13))
      return "" + ch12 + ch13;
    if ((htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get("" + ch13) != null) &&
        Atom.isValidElementSymbol(ch13))
      return "" + ch13;
    if ((htElementsInCurrentGroup == null ||
         htElementsInCurrentGroup.get("" + ch12) != null) &&
        Atom.isValidElementSymbol(ch12))
      return "" + ch12;
    return "Xx";
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
        if (atomSetCollection.bondCount > 0) {
          Bond bond = atomSetCollection.bonds[atomSetCollection.bondCount - 1];
          if (i < 4 &&
              bond.atomIndex1 == sourceIndex &&
              bond.atomIndex2 == targetIndex) {
            ++bond.order;
            continue;
          }
        }
        //        if (i >= 4)
        //          logger.log("hbond:" + sourceIndex + "->" + targetIndex);
        atomSetCollection.addBond(new Bond(sourceIndex, targetIndex,
                               i < 4
                               ? 1 : JmolAdapter.ORDER_HBOND));
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
    String structureType = "none";
    int startChainIDIndex;
    int startIndex;
    int endChainIDIndex;
    int endIndex;
    if (line.startsWith("HELIX ")) {
      structureType = "helix";
      startChainIDIndex = 19;
      startIndex = 21;
      endChainIDIndex = 31;
      endIndex = 33;
    } else if (line.startsWith("SHEET ")) {
      structureType = "sheet";
      startChainIDIndex = 21;
      startIndex = 22;
      endChainIDIndex = 32;
      endIndex = 33;
    } else if (line.startsWith("TURN  ")) {
      structureType = "turn";
      startChainIDIndex = 19;
      startIndex = 20;
      endChainIDIndex = 30;
      endIndex = 31;
    } else
      return;

    char startChainID = line.charAt(startChainIDIndex);
    int startSequenceNumber = parseInt(line, startIndex, startIndex + 4);
    char startInsertionCode = line.charAt(startIndex + 4);
    char endChainID = line.charAt(endChainIDIndex);
    int endSequenceNumber = parseInt(line, endIndex, endIndex + 4);
    char endInsertionCode = line.charAt(endIndex + 4);

    atomSetCollection.addStructure(new Structure(structureType, startChainID,
                                     startSequenceNumber, startInsertionCode,
                                     endChainID, endSequenceNumber, endInsertionCode));
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
      atomSetCollection.newAtomSet();
      atomSetCollection.setAtomSetNumber(modelNumber);
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
      float[] notionalUnitcell = atomSetCollection.notionalUnitcell = new float[6];
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
    atomSetCollection.pdbScaleMatrix[n*3 + 0] = getFloat(10, 10);
    atomSetCollection.pdbScaleMatrix[n*3 + 1] = getFloat(20, 10);
    atomSetCollection.pdbScaleMatrix[n*3 + 2] = getFloat(30, 10);
    float translation = getFloat(45, 10);
    if (translation != 0) {
      if (atomSetCollection.pdbScaleTranslate == null)
        atomSetCollection.pdbScaleTranslate = new float[3];
      atomSetCollection.pdbScaleTranslate[n] = translation;
    }
  }

  void scale1() {
    try {
      atomSetCollection.pdbScaleMatrix = new float[9];
      scale(0);
    } catch (Exception e) {
      atomSetCollection.pdbScaleMatrix = null;
      logger.log("scale1 died:" + 3);
    }
  }

  void scale2() {
    try {
      scale(1);
    } catch (Exception e) {
      atomSetCollection.pdbScaleMatrix = null;
      logger.log("scale2 died");
    }
  }

  void scale3() {
    try {
      scale(2);
    } catch (Exception e) {
      atomSetCollection.pdbScaleMatrix = null;
      logger.log("scale3 died");
    }
  }

  void expdta() {
    String technique = parseTrimmed(line, 10).toLowerCase();
    if (technique.regionMatches(true, 0, "nmr", 0, 3))
      isNMRdata = true;
  }

  void formul() {
    String groupName = parseToken(line, 12, 15);
    String formula = parseTrimmed(line, 19, 70);
    int ichLeftParen = formula.indexOf('(');
    if (ichLeftParen >= 0) {
      int ichRightParen = formula.indexOf(')');
      if (ichRightParen < 0 || ichLeftParen >= ichRightParen)
        return; // invalid formula;
      formula = parseTrimmed(formula, ichLeftParen + 1, ichRightParen);
    }
    Hashtable htElementsInGroup = (Hashtable)htFormul.get(groupName);
    if (htElementsInGroup == null)
      htFormul.put(groupName, htElementsInGroup = new Hashtable());
    // now, look for atom names in the formula
    ichNextParse = 0;
    String elementWithCount;
    while ((elementWithCount = parseToken(formula, ichNextParse)) != null) {
      if (elementWithCount.length() < 2)
        continue;
      char chFirst = elementWithCount.charAt(0);
      char chSecond = elementWithCount.charAt(1);
      if (Atom.isValidElementSymbolNoCaseSecondChar(chFirst, chSecond))
        htElementsInGroup.put("" + chFirst + chSecond, Boolean.TRUE);
      else if (Atom.isValidElementSymbol(chFirst))
        htElementsInGroup.put("" + chFirst, Boolean.TRUE);
    }
  }
}

