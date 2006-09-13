/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 18:41:50 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5311 $
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
 * Wavefunction Odyssey reader -- old style
 * 
 */

class OdysseyReader extends AtomSetCollectionReader {

  String modelName = "Odyssey file";
  int atomCount, bondCount;
  Hashtable moData = new Hashtable();

  AtomSetCollection readAtomSetCollection(BufferedReader reader)
      throws Exception {

    atomSetCollection = new AtomSetCollection("odyssey)");
    try {
      readHeader(reader);
      if (discardLinesUntilContains(reader, "0 1") == null)
        return atomSetCollection;
      readAtoms(reader);
      if (discardLinesUntilContains(reader, "ATOMLABELS") != null)
        readAtomNames(reader);
      if (discardLinesUntilContains(reader, "HESSIAN") != null)
        readBonds(reader);
    } catch (Exception ex) {
      Logger.error("Could not read file", ex);
      atomSetCollection.errorMessage = "Could not read file:" + ex;
      return atomSetCollection;
    }
    if (atomSetCollection.atomCount == 0)
      atomSetCollection.errorMessage = "No atoms in file";
    else
      atomSetCollection.setAtomSetName(modelName);
    return atomSetCollection;
  }

  void readHeader(BufferedReader reader) throws Exception {
    while ((line = reader.readLine()) != null
        && !line.startsWith(" ")) {}
    line = reader.readLine();
    modelName = line + ";";
    modelName = modelName.substring(0, modelName.indexOf(";")).trim();
  }
  
  void readAtoms(BufferedReader reader) throws Exception {
    String line;
    atomCount = 0;
    while ((line = reader.readLine()) != null
        && !line.startsWith("ENDCART")) {
      String[] tokens = getTokens(line);
      int elementNumber = parseInt(tokens[0]);      
      String elementSymbol = getElementSymbol(elementNumber);
      float x = parseFloat(tokens[1]);
      float y = parseFloat(tokens[2]);
      float z = parseFloat(tokens[3]);
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.x = x;
      atom.y = y;
      atom.z = z;
      atomCount++;
    }
  }

  void readAtomNames(BufferedReader reader) throws Exception {
    for (int i = 0; i < atomCount; i++) {
      String line = reader.readLine();
      atomSetCollection.atoms[i].atomName = line
          .substring(1, line.length() - 1);
    }
  }
  
  void readBonds(BufferedReader reader) throws Exception {
    int nAtoms = atomCount;
    /*
     <one number per atom>
     1    2    1
     1    3    1
     1    4    1
     1    5    1
     1    6    1
     1    7    1
     */
    while ((line = reader.readLine()) != null && !line.startsWith("ENDHESS")) {
      String[] tokens = getTokens(line);
      if (nAtoms == 0) {
        int sourceIndex = parseInt(tokens[0]) - 1;
        int targetIndex = parseInt(tokens[1]) - 1;
        int bondOrder = parseInt(tokens[2]);
        if (bondOrder > 0) {
          atomSetCollection.addBond(new Bond(sourceIndex, targetIndex,
              bondOrder < 4 ? bondOrder : 1)); //aromatic would be 5
          bondCount++;
        }
      } else {
        nAtoms -= tokens.length;
      }
    }
    logger.log(bondCount + " bonds read");
  }
}
