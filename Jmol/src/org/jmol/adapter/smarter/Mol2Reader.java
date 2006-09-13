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

package org.jmol.adapter.smarter;

import java.io.BufferedReader;

import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

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

class Mol2Reader extends AtomSetCollectionReader {

  int nAtoms = 0;

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {
    atomSetCollection = new AtomSetCollection("mol2");
    setFractionalCoordinates(false);
    line = reader.readLine();
    modelNumber = 0;
    while (line != null) {
      if (line.equals("@<TRIPOS>MOLECULE")) {
        if (++modelNumber == desiredModelNumber || desiredModelNumber <= 0) {
          try {
            processMolecule(reader);
          } catch (Exception e) {
            Logger.error("Could not read file at line: " + line, e);
          }
          if (desiredModelNumber > 0)
            break;
          continue;
        }
      }
      line = reader.readLine();
    }
    return atomSetCollection;
  }

  void processMolecule(BufferedReader reader) throws Exception {
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

    String thisDataSetName = reader.readLine().trim();
    line = reader.readLine() + " 0 0 0 0 0 0";
    int atomCount = parseInt(line);
    int bondCount = parseInt(line, ichNextParse);
    int resCount = parseInt(line, ichNextParse);
    reader.readLine();//mol_type
    line = reader.readLine();//charge_type
    boolean iHaveCharges = (line.indexOf("NO_CHARGES") != 0);
    line = reader.readLine(); //optional SYBYL status
    if (line != null && line.charAt(0) != '@') {
      line = reader.readLine(); //optional comment
      if (line != null && line.charAt(0) != '@') {
        thisDataSetName += ": " + reader.readLine().trim();
        line = reader.readLine();
      }
    }
    if (atomSetCollection.currentAtomSetIndex >= 0) {
      atomSetCollection.newAtomSet();
      atomSetCollection.setCollectionName("<collection of "
          + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
    } else {
      atomSetCollection.setCollectionName(thisDataSetName);
    }
    logger.log(thisDataSetName);
    while (line != null && !line.equals("@<TRIPOS>MOLECULE")) {
      if (line.equals("@<TRIPOS>ATOM")) {
        readAtoms(reader, atomCount, iHaveCharges);
        atomSetCollection.setAtomSetName(thisDataSetName);
      } else if (line.equals("@<TRIPOS>BOND")) {
        readBonds(reader, bondCount);
      } else if (line.equals("@<TRIPOS>SUBSTRUCTURE")) {
        readResInfo(reader, resCount);
      } else if (line.equals("@<TRIPOS>CRYSIN")) {
        readCrystalInfo(reader);
      }
      line = reader.readLine();
    }
    nAtoms+=atomCount;
    applySymmetry();
  }

  void readAtoms(BufferedReader reader, int atomCount, boolean iHaveCharges)
      throws Exception {
    //     1 Cs       0.0000   4.1230   0.0000   Cs        1 RES1   0.0000
    //  1 C1          7.0053   11.3096   -1.5429 C.3       1 <0>        -0.1912
    // free format, but no blank lines
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atomSetCollection.addNewAtom();
      line = reader.readLine();
      String[] tokens = getTokens(line);
      //Logger.debug(tokens.length + " -" + tokens[5] + "- " + line);
      setAtomCoord(atom, parseFloat(tokens[2]), parseFloat(tokens[3]),
          parseFloat(tokens[4]));
      String elementSymbol = tokens[5];
      if (elementSymbol.length() > 1 && elementSymbol.charAt(1) == '.')
        elementSymbol = elementSymbol.substring(0, 1);
      if (elementSymbol.length() > 2)
        elementSymbol = elementSymbol.substring(0, 2);
      atom.elementSymbol = elementSymbol;
      if (iHaveCharges)
        atom.partialCharge = parseFloat(tokens[8]);
    }
  }

  void readBonds(BufferedReader reader, int bondCount) throws Exception {
    //     6     1    42    1
    // free format, but no blank lines
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      String[] tokens = getTokens(line);
      int atomIndex1 = parseInt(tokens[1]);
      int atomIndex2 = parseInt(tokens[2]);
      int order = parseInt(tokens[3]);
      if (order == Integer.MIN_VALUE)
        order = (tokens[3].equals("ar") ? JmolAdapter.ORDER_AROMATIC : 1);
      atomSetCollection
          .addBond(new Bond(nAtoms + atomIndex1 - 1, nAtoms + atomIndex2 - 1, order));
    }
  }

  void readResInfo(BufferedReader reader, int resCount) throws Exception {
    // free format, but no blank lines
    for (int i = 0; i < resCount; ++i) {
      reader.readLine();
      //to be determined -- not implemented
    }
  }

  void readCrystalInfo(BufferedReader reader) throws Exception {
    //    4.1230    4.1230    4.1230   90.0000   90.0000   90.0000   221     1
    line = reader.readLine();
    ichNextParse = 0;
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, parseFloat(line, ichNextParse));
    setSpaceGroupName(line.substring(ichNextParse, line.length()).trim());
  }
}
