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

class MolReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {
    model = new Model(ModelAdapter.MODEL_TYPE_OTHER);
    model.setModelName(reader.readLine());
    reader.readLine();
    reader.readLine();
    String countLine = reader.readLine();
    int atomCount = parseInt(countLine, 0, 3);
    int bondCount = parseInt(countLine, 3, 6);
    readAtoms(reader, atomCount);
    readBonds(reader, bondCount);
    return model;
  }
  
  // www.mdli.com/downloads/public/ctfile/ctfile.jsp
  void readAtoms(BufferedReader reader, int atomCount)
    throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = reader.readLine();
      String elementSymbol = line.substring(31,34).trim().intern();
      float x = parseFloat(line,  0, 10);
      float y = parseFloat(line, 10, 20);
      float z = parseFloat(line, 20, 30);
      int charge = 0;
      if (line.length() >= 39) {
        int chargeCode = parseInt(line, 36, 39);
        if (chargeCode != 0)
          charge = 4 - chargeCode;
      }
      Atom atom = model.newAtom();
      atom.elementSymbol = elementSymbol;
      atom.charge = charge;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  void readBonds(BufferedReader reader, int bondCount)
    throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      int atomIndex1 = parseInt(line, 0, 3);
      int atomIndex2 = parseInt(line, 3, 6);
      int order = parseInt(line, 6, 9);
      if (order == 4)
        order = ModelAdapter.ORDER_AROMATIC;
      model.addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}
