/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent , UK
 *
 * Contact: pc229@kent.ac.uk or pieremanuele.canepa@gmail.com
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
 * http://www.crystal.unito.it/
 *
 * @author Pieremanuele Canepa, Room 104, FM Group
 * School of Physical Sciences, Ingram Building,
 * University of Kent, Canterbury, Kent,
 * CT2 7NH
 * United Kingdom
 *
 * @version 1.0
 *
 * This version works and has been well tested on several structures!
 * 
 * TODO: Needs adaptation to be more modular and flexible
 *
 */

public class CrystalReader extends AtomSetCollectionReader {
 private String calculationType;

 public void readAtomSetCollection(BufferedReader reader) {

    boolean isPeriodic = true;
    boolean isFractional = true;

    atomSetCollection = new AtomSetCollection("Crystal", this);
    try {
      this.reader = reader;
      discardLinesUntilContains("*                                CRYSTAL");
      discardLinesUntilContains("EEEEEEEEEE");
      atomSetCollection.setCollectionName(readLine().trim());
      readLine();
      calculationType = readLine().trim();
      atomSetCollection.setAtomSetAuxiliaryInfo("calculationType", calculationType);
      if (calculationType.indexOf("MOLECULAR") >= 0) {
        isFractional = false;
        isPeriodic = false;
      }
      while (readLine() != null) {
        if (line.startsWith(" SPACE GROUP ") || line.startsWith(" CORRESPONDING SPACE GROUP")) {
          readSpaceGroup();
          continue;
        }
        if (line.startsWith(" LATTICE PARAMETERS  (ANGSTROMS AND DEGREES) - CONVENTIONAL CELL")) {
          isFractional = true;
          readCellParams();
          continue;
        }
        if (line.startsWith(" LATTICE PARAMETER (ANGSTROMS) - CONVENTIONAL CELL")) {
          isFractional = true;
          readPolymerCell();
        }
        if (line.startsWith(" INPUT COORDINATES")) {
          readAtomCoords(isFractional, isPeriodic);
          continue;
        }
      }
      applySymmetryAndSetTrajectory();

    } catch (Exception e) {
      setError(e);
    }
  }

  private void readSpaceGroup() {
    // SPACE GROUP (CENTROSYMMETRIC) : F M 3 M
    String name = line.substring(line.indexOf(":") + 1).trim();
    setSpaceGroupName(name);
  }

  private void readCellParams() throws Exception {
    readLine();
    String[] tokens = getTokens(readLine());
    if (calculationType.equals("SLAB CALCULATION")) {
      setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), 1, 90, 90,
          parseFloat(tokens[2]));
    } else {
      setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]),
          parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]),
          parseFloat(tokens[5]));
    }
  }

  private void readPolymerCell() {
    float a = parseFloat(line.substring(line.indexOf("CELL") + 4));
    setUnitCell(a, 1, 1, 90, 90, 90);
    
  }


 /*
 INPUT COORDINATES

 ATOM AT. N.              COORDINATES
   1  12     0.000000000000E+00  0.000000000000E+00  0.000000000000E+00
   2   8     5.000000000000E-01  5.000000000000E-01  5.000000000000E-01
  */
 private void readAtomCoords(boolean isFractional, boolean isthisPeriodic)
      throws Exception {
    setFractionalCoordinates(isFractional);
    readLine();
    readLine();
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = parseInt(tokens[1]);
      float x = parseFloat(tokens[2]);
      float y = parseFloat(tokens[3]);
      float z = parseFloat(tokens[4]);
      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicNumber);
    }
  }

}
