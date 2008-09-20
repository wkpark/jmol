/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
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

import org.jmol.api.JmolAdapter;

/**
 * A minimal multi-file reader for TRIPOS SYBYL mol2 files.
 *<p>
 * <a href='http://www.tripos.com/data/support/mol2.pdf '>
 * http://www.tripos.com/data/support/mol2.pdf 
 * </a>
 *<p>
 */

/*
 * symmetry added by Bob Hanson:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetry()
 *  
 */

public class Mol2Reader extends AtomSetCollectionReader {

  int nAtoms = 0;
  int atomCount = 0;
  boolean isPDB = false;
  
 public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("mol2");
    try {
      setFractionalCoordinates(false);
      readLine();
      modelNumber = 0;
      while (line != null) {
        if (line.equals("@<TRIPOS>MOLECULE")) {
          if (++modelNumber == desiredModelNumber || desiredModelNumber <= 0) {
            processMolecule();
            if (desiredModelNumber > 0)
              break;
            continue;
          }
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  void processMolecule() throws Exception {
    /* 4-6 lines:
     ZINC02211856
     55    58     0     0     0
     SMALL
     USER_CHARGES
     2-diethylamino-1-[2-(2-naphthyl)-4-quinolyl]-ethanol

     mol_name
     num_atoms [num_bonds [num_subst [num_feat [num_sets]]]]
     mol_type
     charge_type
     [status_bits
     [mol_comment]]

     */

    isPDB = false;
    String thisDataSetName = readLineTrimmed();
    readLine();
    line += " 0 0 0 0 0 0";
    atomCount = parseInt(line);
    int bondCount = parseInt();
    int resCount = parseInt();
    readLine();//mol_type
    readLine();//charge_type
    boolean iHaveCharges = (line.indexOf("NO_CHARGES") != 0);
    //optional SYBYL status
    if (readLine() != null && (line.length() == 0 || line.charAt(0) != '@')) {
      //optional comment -- but present if comment is present
      if (readLine() != null && line.length() != 0 && line.charAt(0) != '@') {
        thisDataSetName += ": " + line.trim();
      }
    }
    newAtomSet(thisDataSetName);
    while (line != null && !line.equals("@<TRIPOS>MOLECULE")) {
      if (line.equals("@<TRIPOS>ATOM")) {
        readAtoms(atomCount, iHaveCharges);
        atomSetCollection.setAtomSetName(thisDataSetName);
      } else if (line.equals("@<TRIPOS>BOND")) {
        readBonds(bondCount);
      } else if (line.equals("@<TRIPOS>SUBSTRUCTURE")) {
        readResInfo(resCount);
      } else if (line.equals("@<TRIPOS>CRYSIN")) {
        readCrystalInfo();
      }
      readLine();
    }
    nAtoms += atomCount;
    if (isPDB) {
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("isPDB",
          Boolean.TRUE);
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
    }
    applySymmetry();
  }

  void readAtoms(int atomCount, boolean iHaveCharges) throws Exception {
    //     1 Cs       0.0000   4.1230   0.0000   Cs        1 RES1   0.0000
    //  1 C1          7.0053   11.3096   -1.5429 C.3       1 <0>        -0.1912
    // free format, but no blank lines
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens(readLine());
      //Logger.debug(tokens.length + " -" + tokens[5] + "- " + line);
      atom.atomName = tokens[1];
      setAtomCoord(atom, parseFloat(tokens[2]), parseFloat(tokens[3]),
          parseFloat(tokens[4]));
      String elementSymbol = tokens[5];
      if (elementSymbol.length() > 1 && elementSymbol.charAt(1) == '.')
        elementSymbol = elementSymbol.substring(0, 1);
      if (elementSymbol.length() > 2)
        elementSymbol = elementSymbol.substring(0, 2);
      atom.elementSymbol = elementSymbol;
      // apparently "NO_CHARGES" is not strictly enforced
      //      if (iHaveCharges)
      if (tokens.length > 6)
        atom.sequenceNumber = parseInt(tokens[6]);
      if (tokens.length > 7) {
        atom.group3 = tokens[7];
        atom.isHetero = JmolAdapter.isHetero(atom.group3);
        if (atom.group3.length() == 3
            && JmolAdapter.lookupGroupID(atom.group3) >= 0) {
          isPDB = true;
        }      
        if (isPDB)
          atom.elementSymbol = deduceElementSymbol(atom.isHetero, tokens[5], atom.group3);
      }
      if (tokens.length > 8)
        atom.partialCharge = parseFloat(tokens[8]);
    }
  }

  String deduceElementSymbol(boolean isHetero, String XX, String group3) {
    // short of having an entire table, 
    if (XX.equalsIgnoreCase(group3)) 
        return XX; // Cd Mg etc.
    char ch1 = XX.charAt(0);
    char ch2 = (XX.length() > 1 ? XX.charAt(1) : ' ');
    String full = group3 + "." + ch1 + ch2;
    // Cd Nd Ne are not in complex hetero; Ca is in these:
    if (("OEC.CA ICA.CA OC1.CA OC2.CA OC4.CA").indexOf(full)>=0)
      return "Ca";
    if ("NA NB NC ND NE NF NG NH CA CB CC CD CE CF CG CH OA OB OC OD OE OF OG OH".indexOf(XX) >= 0)
      return "" + ch1;
    if (isHetero && Atom.isValidElementSymbolNoCaseSecondChar(ch1, ch2))
      return (isHetero || ch1 != 'H' ? XX.trim() : "H");
    if (Atom.isValidElementSymbol(ch1))
      return "" + ch1;
    if (Atom.isValidElementSymbol(ch2))
      return "" + ch2;
    return "Xx";
  }


  void readBonds(int bondCount) throws Exception {
    //     6     1    42    1
    // free format, but no blank lines
    for (int i = 0; i < bondCount; ++i) {
      String[] tokens = getTokens(readLine());
      int atomIndex1 = parseInt(tokens[1]);
      int atomIndex2 = parseInt(tokens[2]);
      int order = parseInt(tokens[3]);
      if (order == Integer.MIN_VALUE)
        order = (tokens[3].equals("ar") ? JmolAdapter.ORDER_AROMATIC : JmolAdapter.ORDER_UNSPECIFIED);
      atomSetCollection
          .addBond(new Bond(nAtoms + atomIndex1 - 1, nAtoms + atomIndex2 - 1, order));
    }
  }

  void readResInfo(int resCount) throws Exception {
    // free format, but no blank lines
    for (int i = 0; i < resCount; ++i) {
      readLine();
      //to be determined -- not implemented
    }
  }

  void readCrystalInfo() throws Exception {
    //    4.1230    4.1230    4.1230   90.0000   90.0000   90.0000   221     1
    readLine();
    String[] tokens = getTokens();
    if (tokens.length < 6)
      return;
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, parseFloat(tokens[i]));
    String name = "";
    for (int i = 6; i < tokens.length; i++)
      name += " " + tokens[i];
    if (name == "")
      name = " P1";
    else
      name += " *";
    name = name.substring(1);
    setSpaceGroupName(name);
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = 0; i < atomCount; ++i)
      setAtomCoord(atoms[nAtoms + i]);
  }
}
