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
import java.util.StringTokenizer;

class XyzReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("xyz");

    try {
      int modelNumber = 1;
      int modelCount;
      while ((modelCount = readAtomCount(reader)) > 0) {
        if (modelNumber == 1)
          model.setModelName(reader.readLine());
        else
          reader.readLine();
        readAtoms(reader, modelNumber, modelCount);
        ++modelNumber;
      }
    } catch (Exception ex) {
      model.errorMessage = "Could not read file:" + ex;
    }
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }
    
  int readAtomCount(BufferedReader reader) throws Exception {
    String line = reader.readLine();
    if (line == null)
      return 0;
    StringTokenizer tokenizer = new StringTokenizer(line, "\t ");
    if (! tokenizer.hasMoreTokens())
      return 0;
    return Integer.parseInt(tokenizer.nextToken());
  }
  
  void readAtoms(BufferedReader reader,
                 int modelNumber, int modelCount) throws Exception {
    float[] chargeAndOrVector = new float[4];
    for (int i = 0; i < modelCount; ++i) {
      StringTokenizer tokenizer =
        new StringTokenizer(reader.readLine(), "\t ");
      String elementSymbol = tokenizer.nextToken().intern();
      float x = parseFloat(tokenizer.nextToken());
      float y = parseFloat(tokenizer.nextToken());
      float z = parseFloat(tokenizer.nextToken());
      int j;
      for (j = 0; j < 4 && tokenizer.hasMoreTokens(); ++j)
        chargeAndOrVector[j] = parseFloat(tokenizer.nextToken());
      int charge = (j == 1 || j == 4) ? (int)chargeAndOrVector[0] : 0;
      float vectorX, vectorY, vectorZ;
      vectorX = vectorY = vectorZ = Float.NaN;
      if (j >= 3) {
        vectorX = chargeAndOrVector[j - 3];
        vectorY = chargeAndOrVector[j - 2];
        vectorZ = chargeAndOrVector[j - 1];
      }
      Atom atom = model.addNewAtom();
      atom.modelNumber = modelNumber;
      atom.elementSymbol = elementSymbol;
      atom.formalCharge = charge;
      atom.x = x; atom.y = y; atom.z = z;
      atom.vectorX = vectorX; atom.vectorY = vectorY; atom.vectorZ = vectorZ;
    }
  }
}
