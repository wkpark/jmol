/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Joerg Meyer, FHI Berlin
 *
 * Contact: meyer@fhi-berlin.mpg.de
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
 *
 */

package org.jmol.adapter.readers.xtal;

import org.jmol.adapter.smarter.*;
import java.io.BufferedReader;

/**
 * 
 * http://www.cse.clrc.ac.uk/cmg/CRYSTAL/   
 * 
 * very preliminary -- untested
 * 
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  public void readAtomSetCollection(BufferedReader reader) {

    atomSetCollection = new AtomSetCollection("Crystal", this);
    try {
      this.reader = reader;
      atomSetCollection.setCollectionName(readLine());
      while (readLine() != null) {
        if (line.startsWith(" MOLECULE")) {
          readAtomCoords(false);
          break;
        }
        if (line.startsWith(" CRYSTAL") || line.startsWith(" SLAB")
            || line.startsWith(" POLYMER") || line.startsWith(" EXTERNAL")) {
          atomSetCollection.setAtomSetAuxiliaryInfo("periodicity", line);
          readCellParams();
          readAtomCoords(true);
          break;
        }
      }
      
      applySymmetryAndSetTrajectory();
      
    } catch (Exception e) {
      setError(e);
    }
  }

  private void readCellParams() throws Exception {

    discardLinesUntilContains(" LATTICE PARAMETERS  (ANGSTROMS AND DEGREES) - PRIMITIVE CELL");
    readLine();
    readLine();
    float a = parseFloat(line.substring(2, 17));
    float b = parseFloat(line.substring(18, 33));
    float c = parseFloat(line.substring(34, 42));
    float alpha = parseFloat(line.substring(43, 60));
    float beta = parseFloat(line.substring(61, 71));
    float gamma = parseFloat(line.substring(72, 80));
    // this method works fine so far
    setUnitCell(a, b, c, alpha, beta, gamma);
  }

  private void readAtomCoords(boolean isFractional) throws Exception {
    setFractionalCoordinates(isFractional);
    discardLinesUntilContains("ATOMS IN THE ASYMMETRIC");
    int atomCount = parseInt(line.substring(61, 65));
    readLine();
    readLine();
    for (int i = 0; i < atomCount; i++) {
      readLine();
      String atomName = line.substring(8, 11);
      int atomicnumber = parseInt(atomName.substring(0, 2).trim());
      float x = parseFloat(line.substring(15, 35));
      float y = parseFloat(line.substring(36, 55));
      float z = parseFloat(line.substring(56, 75));
      Atom atom = atomSetCollection.addNewAtom();
      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicnumber);
      atom.atomName = atomName + "_" + i;
    }
  }

}
