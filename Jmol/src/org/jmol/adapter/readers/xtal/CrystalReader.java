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

import org.jmol.adapter.smarter.*; //import org.jmol.api.JmolAdapter;

import java.io.BufferedReader;

//import java.util.Vector;

/**
 * 
 * http://www.crystal.unito.it/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom
 * 
 * @version 1.3
 * 
 *          for a specific model in the set, use
 * 
 *          load "xxx.out" n
 * 
 *          as for all readers, where n is an integer > 0
 * 
 *          for final optimized geometry use
 * 
 *          load "xxx.out" 0
 * 
 *          (that is, "read the last model") as for all readers
 * 
 *          for conventional unit cell -- input coordinates only, use
 * 
 *          load "xxx.out" filter "conventional"
 * 
 *          TODO: vibrational frequencies
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  private boolean isPrimitive = true;
  private boolean isPolymer = false;
  private boolean isSlab = false;
  private boolean isMolecular = false;
  private boolean doReadAtoms = false;

  public void readAtomSetCollection(BufferedReader reader) {
    readAtomSetCollection(reader, "Crystal");
  }

  protected void initializeReader(BufferedReader reader, String type)
      throws Exception {
    super.initializeReader(reader, type);
    isPrimitive = (filter == null || filter.indexOf("conv") < 0);
    atomSetCollection.setAtomSetAuxiliaryInfo("unitCellType",
        (isPrimitive ? "primitive" : "conventional"));
    setFractionalCoordinates(readHeader());
  }

  protected boolean checkLine() throws Exception {
    // starting point for any calculation is the definition of the lattice
    // parameters similar to the "data" statement of a CIF file
    if (line.startsWith(" LATTICE PARAMETER")
        && (isPrimitive
            && (line.contains("- PRIMITIVE") || line.contains("- BOHR")) || !isPrimitive
            && line.contains("- CONVENTIONAL"))) {
      if (doneReadingModels()) {
        continuing = false;
        return false;
      }
      if (!isPrimitive || doGetModel(++modelNumber)) {
        readCellParams();
        doReadAtoms = true;
      } else {
        doReadAtoms = false;
      }
      return true;
    }
    if (!doReadAtoms)
      return true;
    if (!isPrimitive) {
      if (line.startsWith(" INPUT COORDINATES")) {
        readInputCoords();
        continuing = false;
        // because if we are reading the conventional cell,
        // there won't be anything else we can do here.
      }
      return true;
    }

    // from here on -- must be primitive

    if (line.startsWith(" ATOMS IN THE ASYMMETRIC UNIT")) {
      readFractionalCoords();
      return true;
    }
    if (line.startsWith(" * OPT END - CONVERGED")
        || line.startsWith("== SCF ENDED") || line.startsWith(" TOTAL ENERGY")) {
      readEnergy();
      return true;
    }

    if (line.startsWith(" TYPE OF CALCULATION")) {
      calculationType = line.substring(line.indexOf(":") + 1).trim();
      return true;
    }
    return true;
  }

  private boolean readHeader() throws Exception {
    discardLinesUntilContains("*                                CRYSTAL");
    discardLinesUntilContains("EEEEEEEEEE");
    atomSetCollection.setCollectionName(readLine().trim()
        + (desiredModelNumber == 0 ? " (optimized)" : ""));
    readLine();
    String type = readLine().trim();
    /*
     * This is when the initial geometry is read from an external file GEOMETRY
     * INPUT FROM EXTERNAL FILE (FORTRAN UNIT 34)
     */
    if (type.equals("GEOMETRY INPUT FROM EXTERNAL FILE (FORTRAN UNIT 34)")) {
      type = readLine().trim();
      isPolymer = (type.equals("1D - POLYMER"));
      isSlab = (type.equals("2D - SLAB"));
    } else {
      isPolymer = (type.equals("POLYMER CALCULATION"));
      isSlab = (type.equals("SLAB CALCULATION"));
    }
    atomSetCollection.setAtomSetAuxiliaryInfo("symmetryType", type);

    if (type.indexOf("MOLECULAR") >= 0) {
      isMolecular = doReadAtoms = true;
      return false;
    }
    if (!isPrimitive) {
      discardLines(5);
      setSpaceGroupName(line.substring(line.indexOf(":") + 1).trim());
    }
    return true;
  }

  private void readCellParams() throws Exception {
    newAtomSet();
    if (isPolymer && !isPrimitive) {
      float a = parseFloat(line.substring(line.indexOf("CELL") + 4));
      setUnitCell(a, -1, -1, 90, 90, 90);
      return;
    }
    discardLinesUntilContains("GAMMA");
    String[] tokens = getTokens(readLine());
    if (isSlab) {
      if (isPrimitive)
        setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1,
            parseFloat(tokens[3]), parseFloat(tokens[4]), parseFloat(tokens[5]));
      else
        setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1, 90, 90,
            parseFloat(tokens[2]));
    } else if (isPolymer) {
      setUnitCell(parseFloat(tokens[0]), -1, -1, parseFloat(tokens[3]),
          parseFloat(tokens[4]), parseFloat(tokens[5]));
    } else {
      setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]),
          parseFloat(tokens[2]), parseFloat(tokens[3]), parseFloat(tokens[4]),
          parseFloat(tokens[5]));
    }
  }

  /*
   * ATOMS IN THE ASYMMETRIC UNIT 30 - ATOMS IN THE UNIT CELL: 30 
   * ATOM X/A Y/B Z(ANGSTROM)
   * *****************************************************************
   * 1 T 26 FE 3.332220233571E-01 1.664350001467E-01 5.975038441891E+00 
   * 2 T 8 O -3.289334452690E-01 1.544678332212E-01 5.601153565811E+00
   */
  private void readFractionalCoords() throws Exception {
    if (isMolecular)
      newAtomSet();
    readLine();
    readLine();
    boolean doNormalizePrimitive = isPrimitive && !isMolecular && !isPolymer && !isSlab;
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = getAtomicNumber(tokens[2]);
      float x = parseFloat(tokens[4]);
      float y = parseFloat(tokens[5]);
      float z = parseFloat(tokens[6]);
      // because with these we cannot use the "packed" keyword
      if (x < 0 && (isPolymer || isSlab || doNormalizePrimitive))
        x += 1;
      if (y < 0 && (isSlab || doNormalizePrimitive))
        y += 1;
      if (z < 0 && doNormalizePrimitive)
        z += 1;
      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicNumber);
    }
  }

  /*
   * Crystal adds 100 to the atomic number when the same atom will be
   * described with different basis sets. It also adds 200 when ECP
   * are used: 
   * 
   * 1 T 282 PB 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00
   * 2 T 16 S -5.000000000000E-01 -5.000000000000E-01 -5.000000000000E-01
   */
  private int getAtomicNumber(String token) {
    int atomicNumber = parseInt(token);
    while (atomicNumber >= 100)
      atomicNumber -= 100;
    return atomicNumber;
  }

  /*
   * INPUT COORDINATES
   * 
   * ATOM AT. N. COORDINATES 
   * 1 12 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 
   * 2 8 5.000000000000E-01 5.000000000000E-01 5.000000000000E-01
   * 
   */
  private void readInputCoords() throws Exception {
    readLine();
    readLine();
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = getAtomicNumber(tokens[1]);
      float x = parseFloat(tokens[2]);
      float y = parseFloat(tokens[3]);
      float z = parseFloat(tokens[4]);
      /*
       * we do not do this, because we have other ways to do it namely, "packed"
       * or "{555 555 1}" In this way, we can check those input coordinates
       * exactly
       * 
       * if (x < 0) x += 1; if (y < 0) y += 1; if (z < 0) z += 1;
       */

      setAtomCoord(atom, x, y, z);
      atom.elementSymbol = getElementSymbol(atomicNumber);
    }
  }

  private void newAtomSet() throws Exception {
    if (atomSetCollection.getAtomCount() == 0)
      return;
    applySymmetryAndSetTrajectory();
    atomSetCollection.newAtomSet();
  }

  private void readEnergy() {
    boolean isGlobal = (line.startsWith(" * OPT END"));
    String[] tokens = getTokens(line.substring(line.lastIndexOf(isGlobal ? ":"
        : ")") + 1));
    Double energy = new Double(Double.parseDouble(tokens[0]));
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", energy);
    if (isGlobal)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetName("Energy = " + energy + " Hartree");
  }

}
