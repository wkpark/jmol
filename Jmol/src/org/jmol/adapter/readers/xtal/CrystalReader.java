/* $RCSfile$
 * $Author: hansonr $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 * Copyright (C) 2009  Piero Canepa, University of Kent, UK
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
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.util.Vector;

/**
 * 
 * http://www.crystal.unito.it/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
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
 *          to NOT load vibrations, use
 * 
 *          load "xxx.out" FILTER "novibrations"
 * 
 *          TODO: fix atom counter  when frequency are for fragment 
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  private boolean isPrimitive = true;
  private boolean isPolymer = false;
  private boolean isSlab = false;
  private boolean isMolecular = false;
  private boolean doReadAtoms = false;
  private boolean haveCharges = false;
  private boolean addVibrations = false;
  private int atomCount;
  private int[] atomFrag;

  protected void initializeReader() throws Exception {
    isPrimitive = (filter == null || filter.indexOf("conv") < 0);
    addVibrations = (filter == null || filter.indexOf("novib") < 0);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("unitCellType",
        (isPrimitive ? "primitive" : "conventional"));
    setFractionalCoordinates(readHeader());
  }

  boolean iHaveDesiredModel;
  protected boolean checkLine() throws Exception {
    // starting point for any calculation is the definition of the lattice
    // parameters similar to the "data" statement of a CIF file
    if (line.startsWith(" LATTICE PARAMETER")
        && (isPrimitive
            && (line.contains("- PRIMITIVE") || line.contains("- BOHR")) || !isPrimitive
            && line.contains("- CONVENTIONAL"))) {
      if (iHaveDesiredModel) {
        continuing = false;
        return false;
      }
      if (!isPrimitive || doGetModel(++modelNumber)) {
        readCellParams();
        iHaveDesiredModel = checkLastModel();
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
    if (line.startsWith(" TOTAL ENERGY")) {
      readEnergy();
      readLine();
      if (line.startsWith(" ********"))
        discardLinesUntilContains("SYMMETRY ALLOWED");
      if (line.startsWith(" TTTTTTTT"))
        discardLinesUntilContains("PREDICTED ENERGY CHANGE");
      return true;
    }

    if (line.startsWith(" TYPE OF CALCULATION")) {
      calculationType = line.substring(line.indexOf(":") + 1).trim();
      return true;
    }

    if (line.startsWith(" MULLIKEN POPULATION ANALYSIS")) {
      readPartialCharges();
      return true;
    }

    if (line.startsWith(" FREQUENCIES COMPUTED ON A FRAGMENT")) {
      readFragments();
      return true;
    }

    if (addVibrations
        && line
            .contains("* CALCULATION OF PHONON FREQUENCIES AT THE GAMMA POINT.")) {
      readFrequencies();
      return true;
    }

    if (line.startsWith(" ATOMIC SPINS SET")) {
      readSpins();
      return true;
    }

    if (line.startsWith(" TOTAL ATOMIC SPINS  :")) {
      readMagneticMoments();
      return true;
    }

    return true;
  }

  /*
  SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
  ATOMIC SPINS SET TO (ATOM, AT. N., SPIN)
    1  26-1   2   8 0   3   8 0   4   8 0   5  26 1   6  26 1   7   8 0   8   8 0
    9   8 0  10  26-1  11  26-1  12   8 0  13   8 0  14   8 0  15  26 1  16  26 1
   17   8 0  18   8 0  19   8 0  20  26-1  21  26-1  22   8 0  23   8 0  24   8 0
   25  26 1  26  26 1  27   8 0  28   8 0  29   8 0  30  26-1
  ALPHA-BETA ELECTRONS LOCKED TO   0 FOR  50 SCF CYCLES

      */
  private void readSpins() throws Exception {
    String[] spins = new String[atomCount];
    String data = "";
    while (readLine() != null && line.indexOf("ALPHA") < 0)
      data += line;
    data = TextFormat.simpleReplace(data, "-", " -");
    String[] tokens = getTokens(data);
    for (int i = 0, pt = 2; i < atomCount; i++, pt += 3)
      spins[i] = tokens[pt];
    data = TextFormat.join(spins, '\n', 0);
    atomSetCollection.setAtomSetAuxiliaryProperty("spin", data);
  }

  private void readMagneticMoments() throws Exception {
    String data = "";
    while (readLine() != null && line.indexOf("TTTTTT") < 0)
      data += line;
    data = TextFormat.join(getTokens(data), '\n', 0);
    atomSetCollection.setAtomSetAuxiliaryProperty("magneticMoment", data);
  }

  protected void finalizeReader() throws Exception {
    if (energy != null)
      setEnergy();
    super.finalizeReader();
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
    if (type.indexOf("EXTERNAL FILE") >= 0) {
      type = readLine().trim();
      isPolymer = (type.equals("1D - POLYMER"));
      isSlab = (type.equals("2D - SLAB"));
    } else {
      isPolymer = (type.equals("POLYMER CALCULATION"));
      isSlab = (type.equals("SLAB CALCULATION"));
    }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("symmetryType", type);

    if (type.indexOf("MOLECULAR") >= 0) {
      isMolecular = doReadAtoms = true;
      readLine();
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
          "molecularCalculationPointGroup", line.substring(
              line.indexOf(" OR ") + 4).trim());
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

  int atomIndexLast;

  /*
   * ATOMS IN THE ASYMMETRIC UNIT 30 - ATOMS IN THE UNIT CELL: 30 ATOM X/A Y/B
   * Z(ANGSTROM)
   * ***************************************************************** 1 T 26 FE
   * 3.332220233571E-01 1.664350001467E-01 5.975038441891E+00 2 T 8 O
   * -3.289334452690E-01 1.544678332212E-01 5.601153565811E+00
   */
  private void readFractionalCoords() throws Exception {
    if (isMolecular)
      newAtomSet();
    readLine();
    readLine();
    int i = atomIndexLast;
    atomIndexLast = atomSetCollection.getAtomCount();
    boolean doNormalizePrimitive = isPrimitive && !isMolecular && !isPolymer
        && !isSlab;
    while (readLine() != null && line.length() > 0) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      int atomicNumber = getAtomicNumber(tokens[2]);
      atom.atomName = getAtomName(tokens[3]);
      float x = parseFloat(tokens[4]);
      float y = parseFloat(tokens[5]);
      float z = parseFloat(tokens[6]);
      if (haveCharges)
        atom.partialCharge = atomSetCollection.getAtom(i++).partialCharge;
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
    atomCount = atomSetCollection.getAtomCount() - atomIndexLast;
  }

  private String getAtomName(String s) {
    String atomName = s;
    if (atomName.length() > 1 && Character.isLetter(atomName.charAt(1)))
      atomName = atomName.substring(0, 1)
          + Character.toLowerCase(atomName.charAt(1)) + atomName.substring(2);
    return atomName;
  }

  /*
   * Crystal adds 100 to the atomic number when the same atom will be described
   * with different basis sets. It also adds 200 when ECP are used:
   * 
   * 1 T 282 PB 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 2 T 16
   * S -5.000000000000E-01 -5.000000000000E-01 -5.000000000000E-01
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
   * ATOM AT. N. COORDINATES 1 12 0.000000000000E+00 0.000000000000E+00
   * 0.000000000000E+00 2 8 5.000000000000E-01 5.000000000000E-01
   * 5.000000000000E-01
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

  private Double energy;

  private void readEnergy() {
    line = TextFormat.simpleReplace(line, "( ", "(");
    String[] tokens = getTokens();
    energy = new Double(Double.parseDouble(tokens[2]));
    setEnergy();
  }

  private void setEnergy() {
    atomSetCollection.setAtomSetAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("Energy", energy);
    atomSetCollection.setAtomSetName("Energy = " + energy + " Hartree");
  }

  /*
   * MULLIKEN POPULATION ANALYSIS - NO. OF ELECTRONS 152.000000
   * 
   * ATOM Z CHARGE A.O. POPULATION
   * 
   * 1 FE 26 23.991 2.000 1.920 2.057 2.057 2.057 0.384 0.674 0.674
   */
  private void readPartialCharges() throws Exception {
    if (haveCharges)
      return;
    haveCharges = true;
    discardLines(3);
    Atom[] atoms = atomSetCollection.getAtoms();
    int i = atomSetCollection.getLastAtomSetAtomIndex();
    while (readLine() != null && line.length() > 3)
      if (line.charAt(3) != ' ')
        atoms[i++].partialCharge = parseFloat(line.substring(9, 11))
            - parseFloat(line.substring(12, 18));
  }

  
  private void readFragments() throws Exception{
    /*
     *   2 (   8 O )     3 (   8 O )     4 (   8 O )    85 (   8 O )    86 (   8 O ) 
     *  87(   8 O )    89(   6 C )    90(   8 O )    91(   8 O )    92(   1 H ) 
     *  93(   1 H )    94(   6 C )    95(   1 H )    96(   8 O )    97(   1 H ) 
     *  98(   8 O )    99(   6 C )   100(   8 O )   101(   8 O )   102(   1 H ) 
     * 103(   1 H )   104(   6 C )   105(   1 H )   106(   8 O )   107(   8 O ) 
     * 108(   1 H )   109(   6 C )   110(   1 H )   111(   8 O )   112(   8 O ) 
     * 113(   1 H )   114(   6 C )   115(   1 H )   116(   8 O )   117(   8 O ) 
     * 118(   1 H ) 
     *INFORMATION **** INPFREQ ****  ANALYSIS OF THE VIBRATIONAL MODES
     * 
     */
    int numAtomsFrag = parseInt(line.substring(39, 44));
    if (numAtomsFrag < 0)
      return;
    atomFrag = new int[numAtomsFrag];
    String Sfrag = "";
    while (readLine() != null && line.indexOf("INFORMATION **** INPFREQ") < 0)
      Sfrag += line;
    Sfrag = TextFormat.simpleReplace(Sfrag, "(", " (");
    String[] tokens = getTokens(Sfrag);
    for (int i = 0, pos = 0; i < numAtomsFrag; i++, pos += 5)
      atomFrag[i] = parseInt(tokens[pos]) - 1;
  }

  private float[] frequencies;
  private String[] data;

  private void readFrequencies() throws Exception {
    discardLinesUntilContains("MODES         EIGV");
    readLine();
    Vector vData = new Vector();
    int freqAtomCount = atomCount;
    while (readLine() != null && line.length() > 0) {
      int i0 = parseInt(line.substring(1, 5));
      int i1 = parseInt(line.substring(6, 10));
      String irrep = line.substring(49, 52).trim();
      String intens = line.substring(59, 69).replace(')', ' ').trim();

      // not all crystal calculations include intensities values
      // this feature is activated when the keyword INTENS is on the input
      if (intens == null)
        intens = "not available";

      String irActivity = line.substring(55, 58).trim();
      String ramanActivity = line.substring(71, 73).trim();

      String[] data = new String[] { irrep, intens, irActivity, ramanActivity };
      for (int i = i0; i <= i1; i++)
        vData.add(data);
    }

    discardLinesUntilContains("NORMAL MODES NORMALIZED TO CLASSICAL AMPLITUDES");
    readLine();
    int lastAtomCount = -1;
    while (readLine() != null && line.startsWith(" FREQ(CM**-1)")) {
      int frequencyCount = 0;
      String[] tokens = getTokens(line.substring(15));
      frequencies = new float[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        float frequency = parseFloat(tokens[i]);
        frequencies[frequencyCount] = frequency;
        frequencyCount++;
        if (Logger.debugging) {
          Logger.debug((vibrationNumber + 1) + " frequency=" + frequency);
        }
      }
      boolean[] ignore = new boolean[frequencyCount];
      int iAtom0 = 0;
      for (int i = 0; i < frequencyCount; i++) {
        data = (String[]) vData.get(vibrationNumber);
        ignore[i] = (!doGetVibration(++vibrationNumber) || data == null);
        if (ignore[i])
          continue;
        lastAtomCount = cloneLastAtomSet(atomCount);
        if (i == 0)
          iAtom0 = atomSetCollection.getLastAtomSetAtomIndex();
        setFreqValue(i);
      }
      readLine();
      fillFrequencyData(iAtom0, freqAtomCount, lastAtomCount, ignore, false,
          14, 10, atomFrag);
      readLine();
    }
  }

  private void setFreqValue(int i) {
    String activity = ", IR: " + data[2] + ", Ram. " + data[3];
    atomSetCollection.setAtomSetName(data[0] + " "
        + TextFormat.formatDecimal(frequencies[i], 2) + " cm-1 ("
        + TextFormat.formatDecimal(Float.parseFloat(data[1]), 0) + " km/Mole)"
        + activity);
    atomSetCollection.setAtomSetProperty("Frequency", frequencies[i] + " cm-1");
    atomSetCollection.setAtomSetProperty("IR Intensity", data[1] + " km/Mole");
    atomSetCollection.setAtomSetProperty("vibrationalSymmetry", data[0]);
    atomSetCollection.setAtomSetProperty("IR activity ", data[2]);
    atomSetCollection.setAtomSetProperty("Raman activity ", data[3]);
  }
  
}
