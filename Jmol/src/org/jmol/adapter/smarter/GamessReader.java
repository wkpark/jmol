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

class GamessReader extends ModelReader {
    
  final static float angstromsPerBohr = 0.529177f;

  Model readModel(BufferedReader reader) throws Exception {

    model = new Model("gaussian");

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("COORDINATES (BOHR)") >= 0) {
          readAtomsInBohrCoordinates(reader);
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

  void readAtomsInBohrCoordinates(BufferedReader reader) throws Exception {
    reader.readLine(); // discard one line
    String line;
    String atomName;
    while ((line = reader.readLine()) != null &&
           (atomName = parseToken(line, 1, 6)) != null) {
      float x = parseFloat(line, 17, 37);
      float y = parseFloat(line, 37, 57);
      float z = parseFloat(line, 57, 77);
      if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
        break;
      Atom atom = model.addNewAtom();
      atom.atomName = atomName;
      atom.x = x * angstromsPerBohr;
      atom.y = y * angstromsPerBohr;
      atom.z = z * angstromsPerBohr;
    }
  }
}
