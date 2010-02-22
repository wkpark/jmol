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

import org.jmol.adapter.readers.quantum.MOReader;
import org.jmol.adapter.smarter.*;
//import org.jmol.api.JmolAdapter;

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
 * @version 1.2
 * 
 * for a specific model in the set, use
 * 
 * load "xxx.out" n
 * 
 * where n is an integer > 0
 * 
 * for final optimized geometry use
 * 
 * load "xxx.out" 0
 * 
 * for conventional unit cell -- input coordinates only, use
 * 
 * load "xxx.out" filter "conventional"
 * 
 * TODO: vibrational frequencies
 * 
 * TODO: molecular orbitals are in, but no MO coefficient data are in the output files.
 * 
 */

public class CrystalReader extends MOReader {

  private boolean isPrimitive = true;
  private boolean isPolymer = false;
  private boolean isSlab = false;
  private boolean doReadAtoms = false;

  public void readAtomSetCollection(BufferedReader reader) {
    readAtomSetCollection(reader, "Crystal");
  }

  protected void initializeMoReader(BufferedReader reader, String type) throws Exception {
    super.initializeMoReader(reader, type);
    isPrimitive = (filter == null || filter.indexOf("conv") < 0);
    atomSetCollection.setAtomSetAuxiliaryInfo("unitCellType", (isPrimitive ? "primitive" : "conventional"));
    setFractionalCoordinates(readHeader());
  }

  protected boolean checkLine() throws Exception {
    if (line.startsWith(" LATTICE PARAMETER")
        && (isPrimitive
            && (line.contains("- PRIMITIVE") || line.contains("- BOHR")) || !isPrimitive
            && line.contains("- CONVENTIONAL"))) {
      if (!isPrimitive || doGetModel(++modelNumber)) {
        readCellParams();
        doReadAtoms = true;
      } else {
        doReadAtoms = false;
      }
      return true;
    }
    if (line.startsWith(" TYPE OF CALCULATION")) {
      calculationType = line.substring(line.indexOf(":") + 1).trim();
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
      if (isLastModel(modelNumber))
        continuing = false;
      return true;
    }

    if (line.indexOf(" LOCAL ATOMIC FUNCTIONS BASIS SET") >= 0) {
      // readBasisSet();
      return true;
    }
    if (line.indexOf("A.O. POPULATION") >= 0) {
      // readMolecularOrbitals();
      return true;
    }
    if (line.startsWith(" * OPT END - CONVERGED")) {
      setEnergy(parseFloat(line.substring(line.indexOf(":") + 1)), true);
      return true;
    }
    if (line.startsWith("== SCF ENDED")) {
      setEnergy(parseFloat(line.substring(line.indexOf(")") + 1)), true);
      return true;
    }
    if (line.startsWith(" TOTAL ENERGY")) {
      setEnergy(parseFloat(line.substring(line.lastIndexOf(")") + 1)), false);
      return true;
    }
    return true;
  }

  protected void finalizeMoReader() throws Exception {
    applySymmetryAndSetTrajectory();
  }
  
  private boolean readHeader() throws Exception {
    discardLinesUntilContains("*                                CRYSTAL");
    discardLinesUntilContains("EEEEEEEEEE");
    atomSetCollection.setCollectionName(readLine().trim()
        + (desiredModelNumber == 0 ? " (optimized)" : ""));
    readLine();
    String type = readLine().trim();
    /* This is when the initial geometry is read from an external file
     * GEOMETRY INPUT FROM EXTERNAL FILE (FORTRAN UNIT 34)
     */
    if(type.equals("GEOMETRY INPUT FROM EXTERNAL FILE (FORTRAN UNIT 34)")){
      type = readLine().trim();
      isPolymer = (type.equals("1D - POLYMER"));
      isSlab = (type.equals("2D - SLAB"));
    }else {
      isPolymer = (type.equals("POLYMER CALCULATION"));
      isSlab = (type.equals("SLAB CALCULATION"));
    }
    atomSetCollection.setAtomSetAuxiliaryInfo("symmetryType", type);  

    if (type.indexOf("MOLECULAR") >= 0) {
      doReadAtoms = true;
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
    boolean doNormalizePrimitive = !isPolymer && !isSlab && isPrimitive;
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = parseInt(tokens[2]);
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
      /* we do not do this, because we have other ways to do it
       * namely, "packed" or "{555 555 1}"
       * In this way, we can check those input coordinates exactly
      
      if (x < 0)
        x += 1;
      if (y < 0)
        y += 1;
      if (z < 0)
        z += 1;
        
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

  private void setEnergy(float energy, boolean isGlobal) {
    if (isGlobal)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", new Float(energy));
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", new Float(energy));
    atomSetCollection.setAtomSetName("Energy = " + energy);
  }

  /*
  LOCAL ATOMIC FUNCTIONS BASIS SET
  *******************************************************************************
    ATOM  X(AU)  Y(AU)  Z(AU)    NO. TYPE  EXPONENT  S COEF   P COEF   D/F/G COEF
  *******************************************************************************
    1 FE  2.746 -4.757  0.614
                                   1 S  
                                          3.154E+05 2.275E-04 0.000E+00 0.000E+00
                                          4.569E+04 1.900E-03 0.000E+00 0.000E+00
                                          9.677E+03 1.110E-02 0.000E+00 0.000E+00
                                          2.521E+03 5.010E-02 0.000E+00 0.000E+00
                                          7.597E+02 1.705E-01 0.000E+00 0.000E+00
                                          2.630E+02 3.692E-01 0.000E+00 0.000E+00
                                          1.028E+02 4.033E-01 0.000E+00 0.000E+00
                                          4.297E+01 1.434E-01 0.000E+00 0.000E+00
                              2-   5 SP 
                                          7.983E+02-5.200E-03 8.500E-03 0.000E+00
                                          1.912E+02-6.800E-02 6.080E-02 0.000E+00
                                          6.369E+01-1.314E-01 2.114E-01 0.000E+00
                                          2.536E+01 2.517E-01 3.944E-01 0.000E+00
                                          1.073E+01 6.433E-01 3.980E-01 0.000E+00
                                          3.764E+00 2.825E-01 2.251E-01 0.000E+00
                              6-   9 SP 

    */
  /*
  private void readBasisSet() throws Exception {
     discardLines(3);
     shells = new Vector();
     Vector gdata = new Vector();
     gaussianCount = 0;
     shellCount = 0;
     String[] tokens;
     int i0 = -1;
     int i1 = 0;
     int slaterPt = 0;
     int[] slater = null;
     String type = null;
     int atomIndex = 0;
     boolean dataAssigned = true;
     shells = new Vector();
     while (readLine() != null) {
       if (line.charAt(3) != ' ') {
       if (!dataAssigned)
         slaterPt = copySlaterData(atomIndex, slaterPt);
       atomIndex = parseInt(line.substring(0, 4));
       if (atomIndex-- < 0)
         break;
       dataAssigned = false;
       continue;
     }
     // basis block
     if (line.charAt(34) != ' ') {
       // shell;
       shellCount++;
       i0 = parseInt(line.substring(0, 30));
       i1 = parseInt(line.substring(32, 35)) - 1;
       if (i0-- < 0)
         i0 = i1;
       type = line.substring(36,38).trim();
       System.out.println(atomIndex + " " + i0 + " " + i1 + " " + type);
       dataAssigned = true;
       slaterPt = shells.size();
       slater = new int[4];
       shells.addElement(slater);
       slater[0] = atomIndex;
       slater[1] = JmolAdapter.getQuantumShellTagID(type);
       slater[2] = gaussianCount;
       //slater[3] = 0; // will be count of gaussians
         continue;
       }
       gdata.addElement(getTokens());
       gaussianCount++;
       slater[3]++;
     }
     gaussians = new float[gaussianCount][];
     for (int i = 0; i < gaussianCount; i++) {
       tokens = (String[]) gdata.get(i);
       gaussians[i] = new float[tokens.length];
       for (int j = 0; j < tokens.length; j++)
         gaussians[i][j] = parseFloat(tokens[j]);
     }    
  }

  private int copySlaterData(int atomIndex, int slaterPt) {
    int n = shells.size();
    int i;
    for (i = slaterPt; i < n; i++) {
      int[] slater = new int[4];
      int[] s0 = (int[]) shells.get(i);
      slater[0] = atomIndex;
      slater[1] = s0[1];
      slater[2] = s0[2];
      slater[3] = s0[3];
      shells.add(slater);
    }
    return i;
  }
  private void readMolecularOrbitals() throws Exception {
    // would need orbital coefficients here
    //addMOData(nThisLine, data, mos);
    //setMOData(false);
  }
  */


}
