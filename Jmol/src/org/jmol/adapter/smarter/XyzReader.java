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

/**
 * Have not been able to find any good description/reference of this
 * file format. Suggestions appreciated
 */

class XyzReader extends AtomSetCollectionReader {
    
  AtomSetCollection readAtomSetCollection(BufferedReader reader) throws Exception {

    atomSetCollection = new AtomSetCollection("xyz");

    try {
      int modelNumber = 1;
      int modelAtomCount;
      while ((modelAtomCount = readAtomCount(reader)) > 0) {
        if (modelNumber == 1)
          atomSetCollection.setModelName(reader.readLine());
        else
          reader.readLine();
        readAtoms(reader, modelNumber, modelAtomCount);
        ++modelNumber;
      }
    } catch (Exception ex) {
      atomSetCollection.errorMessage = "Could not read file:" + ex;
    }
    return atomSetCollection;
  }
    
  int readAtomCount(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    if (line != null) {
      int atomCount = parseInt(line);
      if (atomCount > 0)
        return atomCount;
    }
    return 0;
  }
  
  final float[] chargeAndOrVector = new float[4];
  final boolean isNaN[] = new boolean[4];
  
  void readAtoms(BufferedReader reader,
                 int modelNumber, int modelAtomCount) throws Exception {
    for (int i = 0; i < modelAtomCount; ++i) {
      String line = reader.readLine();
      Atom atom = atomSetCollection.addNewAtom();
      atom.modelNumber = modelNumber;
      atom.elementSymbol = parseToken(line);
      atom.x = parseFloat(line, ichNextParse);
      atom.y = parseFloat(line, ichNextParse);
      atom.z = parseFloat(line, ichNextParse);
      for (int j = 0; j < 4; ++j)
        isNaN[j] =
          Float.isNaN(chargeAndOrVector[j] = parseFloat(line, ichNextParse));
      if (isNaN[0])
        continue;
      if (isNaN[1]) {
        atom.formalCharge = (int)chargeAndOrVector[0];
        continue;
      }
      if (isNaN[3]) {
        atom.vectorX = chargeAndOrVector[0];
        atom.vectorY = chargeAndOrVector[1];
        atom.vectorZ = chargeAndOrVector[2];
        continue;
      }
      atom.formalCharge = (int)chargeAndOrVector[0];
      atom.vectorX = chargeAndOrVector[1];
      atom.vectorY = chargeAndOrVector[2];
      atom.vectorZ = chargeAndOrVector[3];
    }
  }
}
