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

import java.util.BitSet;
import java.util.Vector;

import javax.vecmath.Point3f;

/**
 * 
 * http://www.crystal.unito.it/
 * 
 * @author Pieremanuele Canepa, Room 104, FM Group School of Physical Sciences,
 *         Ingram Building, University of Kent, Canterbury, Kent, CT2 7NH United
 *         Kingdom, pc229@kent.ac.uk
 * 
 * @version 1.4
 * 
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
 *          to load just the input deck exactly as indicated, use
 * 
 *          load "xxx.out" FILTER "input"
 *          
 *    now allows reading of frequencies and atomic values with conventional
 *    as long as this is not an optimization. 
 *    
 *    Piero-- 
 *    
 *    Q: Is there a way to determine the conventional cell from the primitive
 *    cell if the original primitive cell parameters are known and the cell
 *    changes? Can we just scale it up? Can the symmetry change during a 
 *    calculation? -- Bob
 *          
 * 
 */

public class CrystalReader extends AtomSetCollectionReader {

  private boolean isPrimitive;
  private boolean isPolymer;
  private boolean isSlab;
  private boolean isMolecular;
  private boolean haveCharges;
  private boolean addVibrations;
  private boolean isFreqCalc;
  private boolean inputOnly;
  private int atomCount;
  private int[] atomFrag;

  protected void initializeReader() throws Exception {
    doProcessLines = false;
    if (filter != null)
      filter = filter.toLowerCase();
    inputOnly = (filter != null && filter.indexOf("input") >= 0);
    addVibrations = !inputOnly && (filter == null || filter.indexOf("novib") < 0);
    isPrimitive = !inputOnly && (filter == null || filter.indexOf("conv") < 0);
    setFractionalCoordinates(readHeader());
  }

  protected boolean checkLine() throws Exception {
    if (line.contains("FRQFRQ")) {
      isFreqCalc = true;
      return true;
    }
    if (!isPrimitive) {
      if (line.startsWith(" SHIFT OF THE ORIGIN")) {
        readShift();
        return true;
      }
      if (line.startsWith(" INPUT COORDINATES")) {
        readInputCoords();
        if (inputOnly)
          continuing = false;
        return true;
      }
    }

    if (line.startsWith(" LATTICE PARAMETER")) {
      boolean isConvLattice = line.contains("- CONVENTIONAL");
      if (isConvLattice) {
        // skip if we want primitive and this is the conventional lattice
        if (isPrimitive)
          return true;
        readCellParams(); 
      } else if (!isPrimitive && !havePrimitiveMapping) {
        readPrimitiveMapping(); // just for properties
        return true;
      }
      if (!doGetModel(++modelNumber))
        return checkLastModel();
      if (isPrimitive) {
        readCellParams();
      } else if (modelNumber == 1) {
        // done here
      } else {
        if (!isFreqCalc) {
          // conventional cell and now the lattice has changed.
          // ignore? Can we convert a new primitive cell to conventional cell?
          continuing = false;
          Logger.error("Ignoring structure " + modelNumber + " due to FILTER \"conventional\"");
        }
      }
      return true;
    }
    if (!doProcessLines)
      return true;

    if (line.startsWith(" ATOMS IN THE ASYMMETRIC UNIT")) {
      if (isPrimitive)
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

    if (isPrimitive && line.contains("VOLUME=") && line.contains("- DENSITY")) {
      readVolumePrimCell();
      return true;
    }

    if (line.startsWith(" MULLIKEN POPULATION ANALYSIS")) {
      readPartialCharges();
      return true;
    }

    if (line.startsWith(" TOTAL ATOMIC CHARGES")) {
      readTotalAtomicCharges();
      return true;
    }

    if (line.startsWith(" FREQUENCIES COMPUTED ON A FRAGMENT")) {
      readFragments();
      return true;
    }

    if (addVibrations
        && line
            .contains("* CALCULATION OF PHONON FREQUENCIES AT THE GAMMA POINT.")) {
      if (vInputCoords != null)
        processInputCoords();
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

  private Point3f ptOriginShift = new Point3f();
  private void readShift() {
    //  SHIFT OF THE ORIGIN                  :    3/4    1/4      0
    String[] tokens = getTokens();
    int pt = tokens.length - 3;
    ptOriginShift.set(fraction(tokens[pt++]), fraction(tokens[pt++]), fraction(tokens[pt]));
  }

  private float fraction(String f) {
    String[] ab = TextFormat.split(f, '/');
    return (ab.length == 2 ? parseFloat(ab[0]) / parseFloat(ab[1]) : 0);
  }

  private void readVolumePrimCell() {
    // line looks like:  PRIMITIVE CELL - CENTRING CODE 1/0 VOLUME=   113.054442 - DENSITY 2.642 g/cm^3
    String[] tokens = getTokens(line);
    String volumePrim = tokens[7];
    // this is to avoid misreading 
    //PRIMITIVE CELL - CENTRING CODE 5/0 VOLUME=    30.176529 - DENSITY11.444 g/cm^3
    if (tokens[9].length() > 7) {
      line = TextFormat.simpleReplace(line, "DENSITY", "DENSITY ");
    }
    String densityPrim = tokens[10];
    atomSetCollection.setAtomSetAuxiliaryProperty("volumePrimitive", TextFormat
        .formatDecimal(parseFloat(volumePrim), 3));
    atomSetCollection.setAtomSetAuxiliaryProperty("densityPrimitive",
        TextFormat.formatDecimal(parseFloat(densityPrim), 3));
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
    String data = "";
    while (readLine() != null && line.indexOf("ALPHA") < 0)
      data += line;
    data = TextFormat.simpleReplace(data, "-", " -");
    setData("spin", data, 2, 3);
  }

  private void readMagneticMoments() throws Exception {
    String data = "";
    while (readLine() != null && line.indexOf("TTTTTT") < 0)
      data += line;
    setData("magneticMoment", data, 0, 1);
  }

  private void setData(String name, String data, int pt, int dp) {
    String[] s = new String[atomCount];
    String[] tokens = getTokens(data);
    for (int i = 0, j = 0; i < atomCount; i++, pt += dp) {
      int iConv = getAtomIndexFromPrimitiveIndex(i);
      if (iConv >= 0)
        s[j++] = tokens[pt];
    }
    data = TextFormat.join(s, '\n', 0);
    atomSetCollection.setAtomSetAuxiliaryProperty(name, data);
  }

  protected void finalizeReader() throws Exception {
    if (vInputCoords != null)
      processInputCoords();
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
    if ((isPolymer || isSlab) && !isPrimitive) {
      Logger.error("Cannot use FILTER \"conventional\" with POLYMER or SLAB");
      isPrimitive = true;
    }
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("unitCellType",
        (isPrimitive ? "primitive" : "conventional"));


    if (type.indexOf("MOLECULAR") >= 0) {
      isMolecular = doProcessLines = true;
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
    } else {
      discardLinesUntilContains("GAMMA");
      String[] tokens = getTokens(readLine());
      if (isSlab) {
        if (isPrimitive)
          setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1,
              parseFloat(tokens[3]), parseFloat(tokens[4]),
              parseFloat(tokens[5]));
        else
          setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]), -1, 90, 90,
              parseFloat(tokens[2]));
      } else if (isPolymer) {
        setUnitCell(parseFloat(tokens[0]), -1, -1, parseFloat(tokens[3]),
            parseFloat(tokens[4]), parseFloat(tokens[5]));
      } else {
        setUnitCell(parseFloat(tokens[0]), parseFloat(tokens[1]),
            parseFloat(tokens[2]), parseFloat(tokens[3]),
            parseFloat(tokens[4]), parseFloat(tokens[5]));
      }
    }
  }
  
  private boolean havePrimitiveMapping;
  private int[] primitiveToIndex;
  
  /**
   * create arrays that map primitive atoms to conventional atoms
   * in a 1:1 fashion. Creates:
   * 
   *  int[] primitiveToIndex -- points to model-based atomIndex
   * 
   * @throws Exception
   */
  private void readPrimitiveMapping() throws Exception {
    havePrimitiveMapping = true;
    BitSet bsInputAtomsIgnore = new BitSet();
    int n = vInputCoords.size();    
    int[] indexToPrimitive = new int[n];
    primitiveToIndex = new int[n];
    for (int i = 0; i < n; i++)
      indexToPrimitive[i] = -1;
    
    discardLines(3);
    while (readLine() != null && line.contains(" NOT IRREDUCIBLE")) {
      // example HA_BULK_PBE_FREQ.OUT
      // we remove unnecessary atoms. This is important, because
      // these won't get properties, and we don't know exactly which
      // other atom to associate with them.
      
      bsInputAtomsIgnore.set(parseInt(line.substring(21, 25)) - 1);
      readLine();
    }
    
    // COORDINATES OF THE EQUIVALENT ATOMS
    discardLines(3);
    int iPrim = 0;
    int nPrim = 0;
    while (readLine() != null && line.indexOf("NUMBER") < 0) {
      if (line.length() == 0)
        continue;
      nPrim++;
      int iAtom = parseInt(line.substring(4, 8)) - 1;
      if (indexToPrimitive[iAtom] < 0) {
        // no other primitive atom is mapped to a given conventional atom.
        indexToPrimitive[iAtom] = iPrim++; 
      }
    }
    
    if (bsInputAtomsIgnore.nextSetBit(0) >= 0) 
      for (int i = n; --i >= 0;) {
        if (bsInputAtomsIgnore.get(i))
          vInputCoords.remove(i);
      }
    atomCount = vInputCoords.size();

    // now reset primitiveToIndex
    primitiveToIndex = new int[nPrim];
    for (int i = 0; i < nPrim; i++)
      primitiveToIndex[i] = -1;

    for (int i = vInputCoords.size(); --i >= 0;) {
      line = (String) vInputCoords.get(i);
      int iConv = parseInt(line.substring(0, 4)) - 1;
      iPrim = indexToPrimitive[iConv];
      if (iPrim >= 0)
        primitiveToIndex[iPrim] = i;
    }
    
    System.out.println(nPrim + " primitive atoms " + vInputCoords.size() + " conventionalAtoms");
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

  private Vector vInputCoords;

  /*
   * INPUT COORDINATES
   * 
   * ATOM AT. N. COORDINATES 
   * 1 12 0.000000000000E+00 0.000000000000E+00 0.000000000000E+00 
   * 2  8 5.000000000000E-01 5.000000000000E-01 5.000000000000E-01
   */
  private void readInputCoords() throws Exception {
    // we only store them, because we may want to delete some
    readLine();
    readLine();
    vInputCoords = new Vector();
    while (readLine() != null && line.length() > 0)
      vInputCoords.add(line);
  }
  
  private void processInputCoords() throws Exception {
    // here we may have deleted unnecessary input coordinates
    int atomCount = vInputCoords.size();
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens((String) vInputCoords.get(i));
      int atomicNumber = getAtomicNumber(tokens[1]);
      float x = parseFloat(tokens[2]) + ptOriginShift.x;
      float y = parseFloat(tokens[3]) + ptOriginShift.y;
      float z = parseFloat(tokens[4]) + ptOriginShift.z;
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
    vInputCoords = null;
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
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    int iPrim = 0;
    while (readLine() != null && line.length() > 3)
      if (line.charAt(3) != ' ') {
        int iConv = getAtomIndexFromPrimitiveIndex(iPrim);
        if (iConv >= 0)
          atoms[i0 + iConv].partialCharge = parseFloat(line.substring(9, 11))
              - parseFloat(line.substring(12, 18));
        iPrim++;
      }
  }

  private float[] nuclearCharges;
  private void readTotalAtomicCharges() throws Exception {
    StringBuffer data = new StringBuffer();
    while (readLine() != null && !line.contains("T")) // TTTTT or SUMMED SPIN DENSITY
      data.append(line);
    String[] tokens = getTokens(data.toString());
    float[] charges = new float[tokens.length];
    if (nuclearCharges == null)
      nuclearCharges = charges;
    Atom[] atoms = atomSetCollection.getAtoms();
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    for (int i = 0; i < charges.length; i++) {
      int iConv = getAtomIndexFromPrimitiveIndex(i);
      if (iConv >= 0) {
        charges[i] = parseFloat(tokens[i]);
        atoms[i0 + iConv].partialCharge = nuclearCharges[i] - charges[i];
      }
    }
  }
  
  private int getAtomIndexFromPrimitiveIndex(int iPrim) {
    return (primitiveToIndex == null ? iPrim : primitiveToIndex[iPrim]);
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
      atomFrag[i] = getAtomIndexFromPrimitiveIndex(parseInt(tokens[pos]) - 1);

    // note: atomFrag[i] will be -1 if this atom is being ignored due to FILTER "conventional"

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
    atomSetCollection.setAtomSetProperty("IRintensity", data[1] + " km/Mole");
    atomSetCollection.setAtomSetProperty("vibrationalSymmetry", data[0]);
    atomSetCollection.setAtomSetProperty("IRactivity ", data[2]);
    atomSetCollection.setAtomSetProperty("Ramanactivity ", data[3]);
  }
  
}
