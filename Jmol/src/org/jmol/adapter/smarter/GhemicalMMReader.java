/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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

/**
 * Reads Ghemical (<a href="http://www.uku.fi/~thassine/ghemical/">
 * http://www.uku.fi/~thassine/ghemical</a>)
 * molecular mechanics (*.mm1gp) files.
 * <code>
 * !Header mm1gp 100
 * !Info 1
 * !Atoms 6
 * 0 6 
 * 1 6 
 * 2 1 
 * 3 1 
 * 4 1 
 * 5 1 
 * !Bonds 5
 * 1 0 D 
 * 2 0 S 
 * 3 0 S 
 * 4 1 S 
 * 5 1 S 
 * !Coord
 * 0 0.06677 -0.00197151 4.968e-07 
 * 1 -0.0667699 0.00197154 -5.19252e-07 
 * 2 0.118917 -0.097636 2.03406e-06 
 * 3 0.124471 0.0904495 -4.84021e-07 
 * 4 -0.118917 0.0976359 -2.04017e-06 
 * 5 -0.124471 -0.0904493 5.12591e-07 
 * !Charges
 * 0 -0.2
 * 1 -0.2
 * 2 0.1
 * 3 0.1
 * 4 0.1
 * 5 0.1
 * !End
 * </code>
 *
 * @author Egon Willighagen <egonw@sci.kun.nl>
 */
class GhemicalMMReader extends ModelReader {
    
  Model readModel(BufferedReader input) throws Exception {

    model = new Model("ghemicalMM");

    String line;
    while ((line = input.readLine()) != null) {
      if (line.startsWith("!Header"))
        processHeader(line);
      else if (line.startsWith("!Info"))
        processInfo(line);
      else if (line.startsWith("!Atoms"))
        processAtoms(input, line);
      else if (line.startsWith("!Bonds"))
        processBonds(input, line);
      else if (line.startsWith("!Coord"))
        processCoord(input, line);
      else if (line.startsWith("!Charges"))
        processCharges(input, line);
      else if (line.startsWith("!End")) {
        return model;
      }
    }
    model.errorMessage = "unexpected end of file";
    return model;
  }

  void processHeader(String line) {
  }

  void processInfo(String line) {
  }

  void processAtoms(BufferedReader input, String line) throws Exception {
    int atomCount = parseInt(line, 6);
    System.out.println("atomCount=" + atomCount);
    for (int i = 0; i < atomCount; ++i) {
      if (model.atomCount != i)
        throw new Exception("GhemicalMMReader error #1");
      line = input.readLine();
      int atomIndex = parseInt(line);
      if (atomIndex != i)
        throw new Exception("bad atom index in !Atoms" +
                            "expected: " + i + " saw:" + atomIndex);
      int elementNumber = parseInt(line, ichNextParse);
      Atom atom = model.addNewAtom();
      atom.elementNumber = (byte)elementNumber;
    }
  }

  void processBonds(BufferedReader input, String line) throws Exception {
    int bondCount = parseInt(line, 6);
    for (int i = 0; i < bondCount; ++i) {
      line = input.readLine();
      int atomIndex1 = parseInt(line);
      int atomIndex2 = parseInt(line, ichNextParse);
      String orderCode = parseToken(line, ichNextParse);
      int order = 0;
      switch(orderCode.charAt(0)) {
      case 'C': // Conjugated (aromatic)
        ++order; // our code for aromatic is 4;
      case 'T':
        ++order;
      case 'D':
        ++order;
      case 'S':
      default:
        ++order;
      }
      model.addNewBond(atomIndex1, atomIndex2, order);
    }
  }

  void processCoord(BufferedReader input, String line) throws Exception {
    for (int i = 0; i < model.atomCount; ++i) {
      line = input.readLine();
      int atomIndex = parseInt(line);
      if (atomIndex != i)
        throw new Exception("bad atom index in !Coord" +
                            "expected: " + i + " saw:" + atomIndex);
      Atom atom = model.atoms[i];
      atom.x = parseFloat(line, ichNextParse) * 10;
      atom.y = parseFloat(line, ichNextParse) * 10;
      atom.z = parseFloat(line, ichNextParse) * 10;
    }
  }

  void processCharges(BufferedReader input, String line) throws Exception {
    for (int i = 0; i < model.atomCount; ++i) {
      line = input.readLine();
      int atomIndex = parseInt(line);
      if (atomIndex != i)
        throw new Exception("bad atom index in !Charges" +
                            "expected: " + i + " saw:" + atomIndex);
      Atom atom = model.atoms[i];
      atom.partialCharge = parseFloat(line, ichNextParse);
    }
  }
}
