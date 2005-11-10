/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import org.jmol.api.JmolAdapter;
import java.io.BufferedReader;

/**
 * Support for .hin, HyperChem's native file format.
 * http://www.hyper.com
 * <p />
 * Record format is:
 * <code>
 * atom 1 - C ** - -0.06040 0.00000 0.00000 0.00000 3 2 a 6 a 38 s
 * ...
 * atom 67 - H ** - 0.17710 -7.10260 -3.74840 2.24660 1 34 s
 * endmol 1
 * </code>
 * interesting fields are partialCharge, x, y, z, bondCount<br />
 * bonds are atom number and s/d/t/a for single/double/triple/aromatic
 */
class HinReader extends AtomSetCollectionReader {
  
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {
    
    atomSetCollection = new AtomSetCollection("hin");
    
    readAtoms(reader);
    if (errorMessage != null)
      atomSetCollection.errorMessage = errorMessage;
    else if (atomSetCollection.atomCount == 0)
      atomSetCollection.errorMessage = "No atoms in file";
    return atomSetCollection;
  }
  
  String errorMessage;

  int atomIndex;
  int baseAtomIndex;
  String[] tokens;

  final static int MAX_TOKENS = 40; // should be plenty

  void readAtoms(BufferedReader reader) throws Exception {

    tokens = new String[MAX_TOKENS];
    errorMessage = null;

    String line;
    while (errorMessage == null && (line = reader.readLine()) != null ) {
      if (line.length() == 0 || line.charAt(0) == ';') // comment
        continue;
      if (line.startsWith("mol ")) // we have reached the start of a molecule
        processMol(line);
      else if (line.startsWith("atom "))
        processAtom(line);
      else if (line.startsWith("endmol "))
        processEndmol(line);
    }
    tokens = null;
  }

  void processMol(String line) {
    atomSetCollection.newAtomSet();
    String molName = getMolName(line);
    atomSetCollection.setAtomSetName(molName);
    atomIndex = 0;
    baseAtomIndex = atomSetCollection.atomCount;
  }

  String getMolName(String line) {
    parseToken(line);
    parseToken(line, ichNextParse);
    return parseToken(line, ichNextParse);
  }

  void processAtom(String line) {

    int fileAtomNumber = parseInt(line, 5);
    if (fileAtomNumber - 1 != atomIndex) {
      errorMessage = "bad atom number sequence ... expected:" +
        (atomIndex + 1) + " found:" + fileAtomNumber;
      return;
    }

    Atom atom = atomSetCollection.addNewAtom();
    parseToken(line, ichNextParse); // discard
    atom.elementSymbol = parseToken(line, ichNextParse);
    parseToken(line, ichNextParse); // discard
    parseToken(line, ichNextParse); // discard
    atom.partialCharge = parseFloat(line, ichNextParse);
    atom.x = parseFloat(line, ichNextParse);
    atom.y = parseFloat(line, ichNextParse);
    atom.z = parseFloat(line, ichNextParse);
    
    int bondCount = parseInt(line, ichNextParse);
    for (int i = 0; i < bondCount; ++i) {
      int otherAtomNumber = parseInt(line, ichNextParse);
      String bondTypeToken = parseToken(line, ichNextParse);
      if (otherAtomNumber > atomIndex)
        continue;
      int bondOrder;
      switch(bondTypeToken.charAt(0)) {
      case 's': 
        bondOrder = 1;
        break;
      case 'd': 
        bondOrder = 2;
        break;
      case 't': 
        bondOrder = 3;
        break;      
      case 'a':
        bondOrder = JmolAdapter.ORDER_AROMATIC;
        break;
      default:
        errorMessage = "unrecognized bond type:" + bondTypeToken +
          " atom #" + fileAtomNumber;
        return;
      }
      atomSetCollection.addNewBond(baseAtomIndex + atomIndex,
                       baseAtomIndex + otherAtomNumber - 1,
                       bondOrder);
    }
    ++atomIndex;
  }

  void processEndmol(String line) {
  }
}
