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

class MolModel extends Model {
    
  MolModel(BufferedReader reader) throws Exception {
    setModelName(reader.readLine());
    reader.readLine();
    reader.readLine();
    String countLine = reader.readLine();
    int atomCount = Integer.parseInt(countLine.substring(0, 3).trim());
    int bondCount = Integer.parseInt(countLine.substring(3, 6).trim());
    readAtoms(reader, atomCount);
    readBonds(reader, bondCount);
  }
  
  // www.mdli.com/downloads/public/ctfile/ctfile.jsp
  void readAtoms(BufferedReader reader, int atomCount) throws Exception {
    for (int i = 0; i < atomCount; ++i) {
      String line = reader.readLine();
      String elementSymbol = line.substring(31,34).trim();
      float x = Float.valueOf(line.substring( 0,10).trim()).floatValue();
      float y = Float.valueOf(line.substring(10,20).trim()).floatValue();
      float z = Float.valueOf(line.substring(20,30).trim()).floatValue();
      int charge = 0;
      if (line.length() >= 39) {
        int chargeCode = Integer.parseInt(line.substring(36, 39).trim());
        if (chargeCode != 0)
          charge = 4 - chargeCode;
      }

      addAtom(new Atom(elementSymbol, charge, x, y, z));
    }
  }

  void readBonds(BufferedReader reader, int bondCount) throws Exception {
    for (int i = 0; i < bondCount; ++i) {
      String line = reader.readLine();
      int atomIndex1 = Integer.parseInt(line.substring(0, 3).trim());
      int atomIndex2 = Integer.parseInt(line.substring(3, 6).trim());
      int order = Integer.parseInt(line.substring(6, 9).trim());
      if (order == 4)
        order = ModelAdapter.ORDER_AROMATIC;
      addBond(new Bond(atomIndex1-1, atomIndex2-1, order));
    }
  }
}
