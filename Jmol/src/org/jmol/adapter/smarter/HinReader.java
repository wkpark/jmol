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

import org.jmol.api.ModelAdapter;
import java.util.StringTokenizer;
import java.io.BufferedReader;

/**
 * Support for .hin, Hyperchem's native file format.
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
class HinReader extends ModelReader {
  
  Model readModel(BufferedReader reader) throws Exception {
    
    model = new Model("hin");
    
    readAtoms(reader);
    if (errorMessage != null)
      model.errorMessage = errorMessage;
    else if (model.atomCount == 0)
      model.errorMessage = "No atoms in file";
    return model;
  }
  
  String errorMessage;

  int modelNumber;
  int atomIndex;
  int modelBaseAtomIndex;
  String[] tokens;

  void readAtoms(BufferedReader reader) throws Exception {

    modelNumber = 0;
    tokens = new String[40]; // should be plenty
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
    model.setModelName(getMolName(line));
    atomIndex = 0;
    modelBaseAtomIndex = model.atomCount;
    ++modelNumber;
  }

  String getMolName(String line) {
    StringTokenizer st = new StringTokenizer(line);
    if (st.countTokens() == 3) {
      st.nextToken();
      st.nextToken();
      return st.nextToken();
    }
    return "";
  }

  void processAtom(String line) {
    StringTokenizer tokenizer = new StringTokenizer(line, " ");
    
    int tokenCount = tokenizer.countTokens();
    for (int i = 0; i < tokenCount; i++)
      tokens[i] = tokenizer.nextToken();
    
    int fileAtomNumber = parseInt(tokens[1]);
    if (fileAtomNumber - 1 != atomIndex) {
      errorMessage = "bad atom number sequence ... expected:" +
        (atomIndex + 1) + " found:" + fileAtomNumber;
      return;
    }

    Atom atom = model.newAtom();
    atom.modelNumber = modelNumber;
    atom.elementSymbol = tokens[3];
    atom.partialCharge = parseFloat(tokens[6]);
    atom.x = parseFloat(tokens[7]);
    atom.y = parseFloat(tokens[8]);
    atom.z = parseFloat(tokens[9]);
    
    int bondCount = parseInt(tokens[10]);
    for (int i = 0; i < bondCount; ++i) {
      int tokenIndex = 11 + i * 2;
      int otherAtomNumber = parseInt(tokens[tokenIndex]);
      if (otherAtomNumber > atomIndex)
        continue;
      int bondOrder;
      String bondTypeToken = tokens[tokenIndex + 1];
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
        bondOrder = ModelAdapter.ORDER_AROMATIC;
        break;
      default:
        errorMessage = "unrecognized bond type:" + bondTypeToken +
          " atom #" + fileAtomNumber;
        return;
      }
      model.newBond(modelBaseAtomIndex + atomIndex,
                    modelBaseAtomIndex + otherAtomNumber - 1,
                    bondOrder);
    }
    ++atomIndex;
  }

  void processEndmol(String line) {
  }
}
