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

class JaguarReader extends ModelReader {
    
  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("jaguar");

    try {
      String line;
      int lineNum = 0;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("  final geometry:")) {
          readAtoms(reader);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      model.errorMessage = "Could not read file:" + ex;
      return model;
    }
    if (model.atomCount == 0) {
      model.errorMessage = "No atoms in file";
    }
    return model;
  }

  void readAtoms(BufferedReader reader) throws Exception {
    discardLines(reader, 2);
    String line;
    while ((line = reader.readLine()) != null &&
           line.length() >= 60 &&
           line.charAt(2) != ' ') {
      String atomName = parseToken(line, 2, 7);
      float x = parseFloat(line,  8, 24);
      float y = parseFloat(line, 26, 42);
      float z = parseFloat(line, 44, 60);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        return;
      int len = atomName.length();
      if (len < 2)
        return;
      String elementSymbol;
      char ch2 = atomName.charAt(1);
      if (ch2 >= 'a' && ch2 <= 'z')
        elementSymbol = atomName.substring(0, 2);
      else
        elementSymbol = atomName.substring(0, 1);
      Atom atom = model.addNewAtom();
      atom.elementSymbol = elementSymbol;
      atom.atomName = atomName;
      atom.x = x; atom.y = y; atom.z = z;
    }
  }

  void discardLines(BufferedReader reader, int nLines) throws Exception {
    for (int i = nLines; --i >= 0; )
      reader.readLine();
  }
  
}
