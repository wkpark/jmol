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
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom
 * 
 * @version 1.0
 * 
 * 
 * for final optimized geometry use
 * 
 * load "xxx.out" filter "optimized"
 * 
 * for conventional unit cell, use
 * 
 * load "xxx.out" filter "conventional"
 * 
 * TODO: vibrational frequencies
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  private boolean isPrimitive = true;
  private boolean isPolymer = false;
  private boolean isSlab = false;

  public void readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("Crystal", this);

    isPrimitive = (filter == null || filter.indexOf("conv") < 0);
    atomSetCollection.setAtomSetAuxiliaryInfo("unitCellType", (isPrimitive ? "primitive" : "conventional"));
    boolean isFinal = (filter != null && filter.indexOf("opt") >= 0);
    try {
      setFractionalCoordinates(readHeader());
      if (isFinal) {
        discardLinesUntilContains("FINAL OPTIMIZED GEOMETRY");
        atomSetCollection.setCollectionName(name + " (optimized)");
        isPrimitive = true;
      }
      while (readLine() != null) {
        if (line.startsWith(" LATTICE PARAMETER") && 
            (isFinal || isPrimitive && line.contains("- PRIMITIVE") || !isPrimitive && line.contains("- CONVENTIONAL"))) {
          if (isFinal)
            readLine();
          readCellParams();
          continue;
        }
        if (isPrimitive) {
          if (line
              .startsWith(" ATOMS IN THE ASYMMETRIC UNIT")) {
            readFractionalCoords();
            continue;
          }
        } else {
          if (line.startsWith(" INPUT COORDINATES")) {
            readInputCoords();
            continue;
          }
        }
      }
      applySymmetryAndSetTrajectory();

    } catch (Exception e) {
      setError(e);
    }
  }

  private String name;
  private boolean readHeader() throws Exception {
    discardLinesUntilContains("*                                CRYSTAL");
    discardLinesUntilContains("EEEEEEEEEE");
    atomSetCollection.setCollectionName(name = readLine().trim());
    readLine();
    calculationType = readLine().trim();
    isPolymer = (calculationType.equals("POLYMER CALCULATION"));
    isSlab = (calculationType.equals("SLAB CALCULATION"));
    atomSetCollection.setAtomSetAuxiliaryInfo("calculationType",
        calculationType);
    if (calculationType.indexOf("MOLECULAR") >= 0)
      return false;
    if (!isPrimitive) {
      readLine();
      readLine();
      readSpaceGroup();
    }
    return true;
  }

  private void readSpaceGroup() {
    // SPACE GROUP (CENTROSYMMETRIC) : F M 3 M
    String name = line.substring(line.indexOf(":") + 1).trim();
    setSpaceGroupName(name);
  }

  private void readCellParams() throws Exception {
    if (isPolymer && !isPrimitive) {
      float a = parseFloat(line.substring(line.indexOf("CELL") + 4));
      setUnitCell(a, -1, -1, 90, 90, 90);
      return;
    }
    readLine();
    String[] tokens = getTokens(readLine());
    if (isSlab) {
      if (isPrimitive)
        setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1,
            parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
      else
        setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1,
            90, 90, parseFloat(tokens[2]));
    } else if (isPolymer) {
      setUnitCell(parseFloat(tokens[0]), -1, -1,
          parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
    } else {
      setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), parseFloat(tokens[2]),
          parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
    }
  }

  /*
   * ATOMS IN THE ASYMMETRIC UNIT 30 - ATOMS IN THE UNIT CELL: 30 ATOM X/A Y/B
   * Z(ANGSTROM)
   * *****************************************************************
   * ************* 1 T 26 FE 3.331306436039E-01 1.663395164811E-01
   * 6.035011342353E+00 2 T 8 O -3.291645441100E-01 1.554613095970E-01
   * 5.654299584852E+00
   */
  private void readFractionalCoords() throws Exception {
    readLine();
    readLine();
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = parseInt(tokens[2]);
      float x = parseFloat(tokens[4]);
      float y = parseFloat(tokens[5]);
      float z = parseFloat(tokens[6]);
      // because with these we cannot use the "packed" keyword
      if ((isPolymer || isSlab) && x < 0)
        x += 1;
      if (isSlab && y < 0)
        y += 1;
      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicNumber);
    }
  }

  /*
   * INPUT COORDINATES
   * 
   * ATOM AT. N. COORDINATES 
   * 1 12 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 
   * 2 8 5.000000000000E-01 5.000000000000E-01 5.000000000000E-01
   */
  private void readInputCoords()
      throws Exception {
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
