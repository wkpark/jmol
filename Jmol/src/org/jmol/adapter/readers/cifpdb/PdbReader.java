/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-15 17:34:01 -0500 (Sun, 15 Oct 2006) $
 * $Revision: 5957 $
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

package org.jmol.adapter.readers.cifpdb;

import org.jmol.adapter.smarter.*;


import org.jmol.api.JmolAdapter;
import java.io.BufferedReader;
import java.util.Hashtable;

/**
 * PDB file reader.
 *
 *<p>
 * <a href='http://www.rcsb.org'>
 * http://www.rcsb.org
 * </a>
 *
 * @author Miguel, Egon, and Bob (hansonr@stolaf.edu)
 * 
 * symmetry added by Bob Hanson:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  initializeCartesianToFractional();
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetry()
 *  
 */

public class PdbReader extends AtomSetCollectionReader {
  int lineLength;
  // index into atoms array + 1
  // so that 0 can be used for the null value
  boolean isNMRdata;
  final Hashtable htFormul = new Hashtable();
  Hashtable htHetero = null;
  Hashtable htSites = null;
  protected String fileType = "pdb";  
  String currentGroup3;
  Hashtable htElementsInCurrentGroup;

 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection(fileType);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("isPDB", Boolean.TRUE);
    setFractionalCoordinates(false);
    htFormul.clear();
    currentGroup3 = null;
    isNMRdata = false;
    boolean iHaveModel = false;
    boolean iHaveModelStatement = false;
    try {
      while (readLine() != null) {
        lineLength = line.length();
        if (line.startsWith("MODEL ")) {
          iHaveModelStatement = true;
          if (++modelNumber != desiredModelNumber && desiredModelNumber > 0) {
            if (iHaveModel)
              break;
            continue;
          }
          iHaveModel = true;
          applySymmetry();
          //supposedly MODEL is only for NMR
          model();
          continue;
        }
        if (line.startsWith("HELIX ") || line.startsWith("SHEET ")
            || line.startsWith("TURN  ")) {
          structure();
          continue;
        }
        if (line.startsWith("HET   ")) {
          het();
          continue;
        }
        if (line.startsWith("HETNAM")) {
          hetnam();
          continue;
        }
        if (line.startsWith("SITE  ")) {
          site();
          continue;
        }
        if (line.startsWith("CRYST1")) {
          cryst1();
          continue;
        }
        if (line.startsWith("SCALE1")) {
          scale(1);
          continue;
        }
        if (line.startsWith("SCALE2")) {
          scale(2);
          continue;
        }
        if (line.startsWith("SCALE3")) {
          scale(3);
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
        if (line.startsWith("REMARK")) {
          //Logger.debug(line);
          checkLineForScript();
          continue;
        }
        if (line.startsWith("HEADER") && lineLength >= 66) {
          atomSetCollection.setCollectionName(line.substring(62, 66));
          continue;
        }
        /*
         * OK, the PDB file format is messed up here, because the 
         * above commands are all OUTSIDE of the Model framework. 
         * Of course, different models might have different 
         * secondary structures, but it is not clear that PDB actually
         * supports this. So you can't concatinate PDB files the way
         * you can CIF files. --Bob Hanson 8/30/06
         */
        if (iHaveModelStatement && !iHaveModel)
          continue;
        if (line.startsWith("ATOM  ") || line.startsWith("HETATM")) {
          atom();
          continue;
        }
        if (line.startsWith("CONECT")) {
          conect();
          continue;
        }
      }
      //if (!isNMRdata)
      atomSetCollection.connectAll();
      applySymmetry();
      if (htSites != null)
        addSites(htSites);
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

 int atomCount;
  void atom() {
    boolean isHetero = line.startsWith("HETATM");
    char charAlternateLocation = line.charAt(16);

    // get the group so that we can check the formul
    int serial = parseInt(line, 6, 11);
    char chainID = line.charAt(21);
    int sequenceNumber = parseInt(line, 22, 26);
    char insertionCode = line.charAt(26);
    String group3 = parseToken(line, 17, 20);
    if (group3 == null) {
      currentGroup3 = null;
      htElementsInCurrentGroup = null;
    } else if (!group3.equals(currentGroup3)) {
      currentGroup3 = group3;
      htElementsInCurrentGroup = (Hashtable) htFormul.get(group3);
    }

    ////////////////////////////////////////////////////////////////
    // extract elementSymbol
    String elementSymbol = deduceElementSymbol(isHetero);

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
      if ((chSign == '+' || chSign == '-' || chSign == ' ')
          && chMagnitude >= '0' && chMagnitude <= '7') {
        charge = chMagnitude - '0';
        if (chSign == '-')
          charge = -charge;
      }
    }

    float bfactor = readBFactor();
    int occupancy = readOccupancy();
    float partialCharge = readPartialCharge();
    float radius = readRadius();

    /****************************************************************
     * coordinates
     ****************************************************************/
    float x = parseFloat(line, 30, 38);
    float y = parseFloat(line, 38, 46);
    float z = parseFloat(line, 46, 54);
    /****************************************************************/
    Atom atom = new Atom();
    atom.elementSymbol = elementSymbol;
    atom.atomName = atomName;
    if (charAlternateLocation != ' ')
      atom.alternateLocationID = charAlternateLocation;
    atom.formalCharge = charge;
    if (partialCharge != Float.MAX_VALUE)
      atom.partialCharge = partialCharge;
    atom.occupancy = occupancy;
    atom.bfactor = bfactor;
    setAtomCoord(atom, x, y, z);
    atom.isHetero = isHetero;
    atom.chainID = chainID;
    atom.atomSerial = serial;
    atom.group3 = currentGroup3;
    atom.sequenceNumber = sequenceNumber;
    atom.insertionCode = JmolAdapter.canonizeInsertionCode(insertionCode);
    atom.radius = radius;
    atomSetCollection.addAtom(atom);
    if (atomCount++ == 0)
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
    // note that values are +1 in this serial map
    if (isHetero) {
      if (htHetero != null) {
        atomSetCollection.setAtomSetAuxiliaryInfo("hetNames", htHetero);
        htHetero = null;
      }
    }
  }

  protected int readOccupancy() {

    /****************************************************************
     * read the occupancy from cols 55-60 (1-based)
     * should be in the range 0.00 - 1.00
     ****************************************************************/
    int occupancy = 100;
    float floatOccupancy = parseFloat(line, 54, 60);
    if (!Float.isNaN(floatOccupancy))
      occupancy = (int) (floatOccupancy * 100);
    return occupancy;
  }
  
  protected float readBFactor() {
    /****************************************************************
     * read the bfactor from cols 61-66 (1-based)
     ****************************************************************/
    return parseFloat(line, 60, 66);
  }
  
  protected float readPartialCharge() {
    return Float.MAX_VALUE; 
  }
  
  protected float readRadius() {
    return Float.NaN; 
  }
  
  String deduceElementSymbol(boolean isHetero) {
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
      return (isHetero || ch12 != 'H' ? "" + ch12 + ch13 : "H");
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
    try {
      sourceSerial = parseInt(line, 6, 11);
      if (sourceSerial < 0)
        return;
      for (int i = 0; i < 9; i += (i == 5 ? 2 : 1)) {
        int offset = i * 5 + 11;
        int offsetEnd = offset + 5;
        int targetSerial = (offsetEnd <= lineLength ? parseInt(line, offset,
            offsetEnd) : -1);
        if (targetSerial < 0)
          continue;
        atomSetCollection.addConnection(new int[] { sourceSerial, targetSerial,
            i < 4 ? 1 : JmolAdapter.ORDER_HBOND });
      }
    } catch (Exception e) {
      //ignore connection errors
    }
  }

  /*
          1         2         3
0123456789012345678901234567890123456
HELIX    1  H1 ILE      7  LEU     18
HELIX    2  H2 PRO     19  PRO     19
HELIX    3  H3 GLU     23  TYR     29
HELIX    4  H4 THR     30  THR     30
SHEET    1  S1 2 THR     2  CYS     4
SHEET    2  S2 2 CYS    32  ILE    35
SHEET    3  S3 2 THR    39  PRO    41
TURN     1  T1 GLY    42  TYR    44
   */
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

    if (lineLength < endIndex + 4)
      return;

    char startChainID = line.charAt(startChainIDIndex);
    int startSequenceNumber = parseInt(line, startIndex, startIndex + 4);
    char startInsertionCode = line.charAt(startIndex + 4);
    char endChainID = line.charAt(endChainIDIndex);
    int endSequenceNumber = parseInt(line, endIndex, endIndex + 4);
    // some files are chopped to remove trailing whitespace
    char endInsertionCode = ' ';
    if (lineLength > endIndex + 4)
      endInsertionCode = line.charAt(endIndex + 4);

    // this should probably call Structure.validateAndAllocate
    // in order to check validity of parameters
    // 0 here is just a placeholder; actual model number is set in addStructure()
    Structure structure = new Structure(0, structureType, startChainID,
                                        startSequenceNumber,
                                        startInsertionCode, endChainID,
                                        endSequenceNumber, endInsertionCode);
    atomSetCollection.addStructure(structure);
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
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
      atomSetCollection.setAtomSetNumber(modelNumber);
    } catch (NumberFormatException e) {
      //ingore model number errors
    }
  }

  void cryst1() throws Exception {
    setUnitCell(getFloat(6, 9), getFloat(15, 9), getFloat(24, 9), getFloat(33,
        7), getFloat(40, 7), getFloat(47, 7));
    setSpaceGroupName(parseTrimmed(line, 55, 66));
  }

  float getFloat(int ich, int cch) throws Exception {
    return parseFloat(line, ich, ich+cch);
  }

  void scale(int n) throws Exception {
    int pt = n * 4 + 2;
    setUnitCellItem(pt++,getFloat(10, 10));
    setUnitCellItem(pt++,getFloat(20, 10));
    setUnitCellItem(pt++,getFloat(30, 10));
    setUnitCellItem(pt++,getFloat(45, 10));
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
      if (ichRightParen < 0 || ichLeftParen >= ichRightParen ||
          ichLeftParen + 1 == ichRightParen ) // pick up () case in 1SOM.pdb
        return; // invalid formula;
      formula = parseTrimmed(formula, ichLeftParen + 1, ichRightParen);
    }
    Hashtable htElementsInGroup = (Hashtable)htFormul.get(groupName);
    if (htElementsInGroup == null)
      htFormul.put(groupName, htElementsInGroup = new Hashtable());
    // now, look for atom names in the formula
    next[0] = 0;
    String elementWithCount;
    while ((elementWithCount = parseTokenNext(formula)) != null) {
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
  
  void het() {
    if (line.length() < 30)
      return;
    if (htHetero == null)
      htHetero = new Hashtable();
    String groupName = parseToken(line, 7, 10);
    if (htHetero.contains(groupName))
      return;
    String hetName = parseTrimmed(line, 30, 70);
    htHetero.put(groupName, hetName);
  }
  
  void hetnam() {
    if (htHetero == null)
      htHetero = new Hashtable();
    String groupName = parseToken(line, 11, 14);
    String hetName = parseTrimmed(line, 15, 70);
    String htName = (String) htHetero.get(groupName);
    if (htName != null)
      hetName = htName + hetName;
    htHetero.put(groupName, hetName);
    //Logger.debug("hetero: "+groupName+" "+hetName);
  }
  
  /*
   * http://www.wwpdb.org/documentation/format23/sect7.html
   * 
 Record Format

COLUMNS       DATA TYPE         FIELD            DEFINITION
------------------------------------------------------------------------
 1 -  6       Record name       "SITE    "
 8 - 10       Integer           seqNum      Sequence number.
12 - 14       LString(3)        siteID      Site name.
16 - 17       Integer           numRes      Number of residues comprising 
                                            site.

19 - 21       Residue name      resName1    Residue name for first residue
                                            comprising site.
23            Character         chainID1    Chain identifier for first residue
                                            comprising site.
24 - 27       Integer           seq1        Residue sequence number for first
                                            residue comprising site.
28            AChar             iCode1      Insertion code for first residue
                                            comprising site.
30 - 32       Residue name      resName2    Residue name for second residue
...
41 - 43       Residue name      resName3    Residue name for third residue
...
52 - 54       Residue name      resName4    Residue name for fourth residue
 
   */
  
  private void site() {
    if (htSites == null)
      htSites = new Hashtable();
    int seqNum = parseInt(line, 7, 10);
    int nResidues = parseInt(line, 15, 17);
    String siteID = parseTrimmed(line, 11, 14);
    Hashtable htSite = (Hashtable) htSites.get(siteID);
    if (htSite == null) {
      htSite = new Hashtable();
      htSite.put("seqNum", "site_" + seqNum);
      htSite.put("nResidues", new Integer(nResidues));
      htSite.put("groups", "");
      htSites.put(siteID, htSite);
    }
    String groups = (String)htSite.get("groups");
    for (int i = 0; i < 4; i++) {
      int pt = 18 + i * 11;
      String resName = parseTrimmed(line, pt, pt + 3);
      if (resName.length() == 0)
        break;
      String chainID = parseTrimmed(line, pt + 4, pt + 5);
      String seq = parseTrimmed(line, pt + 5, pt + 9);
      String iCode = parseTrimmed(line, pt + 9, pt + 10);
      groups += (groups.length() == 0 ? "" : ",") + "[" + resName + "]" + seq;
      if (iCode.length() > 0)
        groups += "^" + iCode;
      if (chainID.length() > 0)
        groups += ":" + chainID;
      htSite.put("groups", groups);
    }
  }

  public void applySymmetry() throws Exception {
    if (needToApplySymmetry && !isNMRdata) {
      // problem with PDB is that they don't give origins, 
      // so we must force the issue
      if(spaceGroup.indexOf(":") < 0)
        spaceGroup += ":?";
    }
    super.applySymmetry();
  }
}

