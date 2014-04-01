/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-12 00:46:22 -0500 (Tue, 12 Sep 2006) $
 * $Revision: 5501 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.readers.quantum;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;

import java.io.IOException;

import javajs.util.AU;
import javajs.util.List;
import javajs.util.PT;
import javajs.util.V3;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolAdapter;

import org.jmol.util.Escape;
import org.jmol.util.Logger;

/**
 * Reader for Gaussian fchk files
 * 
 * @author hansonr  Bob Hanson hansonr@stolaf.edu
 *
 **/
public class GaussianFchkReader extends MOReader {
  
  private Map<String, Object> fileData;
  private int atomCount;
  
  @Override
  protected void initializeReader() throws Exception {
    super.initializeReader();
    energyUnits = "";
    fileData = new Hashtable<String, Object>();
    fileData.put("title",  rd().trim());
    calculationType = PT.rep(rd(), "  ", " ");
    asc.newAtomSet();
    asc.setAtomSetAuxiliaryInfo("fileData", fileData);
    readAllData();
    readAtoms();
    readBonds();
    readDipoleMoment();
    readPartialCharges();
    readBasis();
    readMOs();
    //readFrequencies();
    continuing = false;
  }

  private void readAllData() throws Exception {
    while ((line == null ? rd() : line) != null) {
      String name = PT.rep(line.substring(0, 40).trim(), " ","");
      char type = line.charAt(43);
      boolean isArray = (line.indexOf("N=") >= 0);
      String v = line.substring(50).trim();
      Logger.info(name + " = " + v + " " + isArray);
      Object o = null;
      if (isArray) {
        switch (type) {
        case 'I':
        case 'R':
          o = fillFloatArray(null, 0, new float[parseIntStr(v)]);
          line = null;
          break;
        default: // C H L
          v = rd().trim();
          while (rd() != null && line.indexOf("   N=   ") < 0)
            v += " " + line.trim();
          o = v;
          break;
        }
      } else {
        switch (type) {
        case 'I':
          o = Integer.valueOf(parseIntStr(v));
          break;
        case 'R':
          o = Double.valueOf(Double.parseDouble(v));
          break;
        case 'C':
        case 'L':
          o = v;
          break;
        }
        line = null;
      }
      if (o != null)
        fileData.put(name, o);
    }
  }
  
  private void readAtoms() throws Exception {
    float[] atomNumbers = (float[]) fileData.get("Atomicnumbers");
    float[] data = (float[]) fileData.get("Currentcartesiancoordinates");
    String e = "" + fileData.get("TotalEnergy"); 
    asc.setAtomSetEnergy(e, parseFloatStr(e));
    atomCount = atomNumbers.length;
    float f = ANGSTROMS_PER_BOHR;
    for(int i = 0, pt = 0; i < atomCount; i++) {
      Atom atom = asc.addNewAtom();
      atom.elementNumber = (short) atomNumbers[i];
      if (atom.elementNumber < 0)
        atom.elementNumber = 0; // dummy atoms have -1 -> 0
      setAtomCoordXYZ(atom, data[pt++] * f, data[pt++] * f, data[pt++] * f);
    }
  }

  /*
MxBond                                     I                3
NBond                                      I   N=          11
           3           3           2           3           3           3
           1           1           1           1           1
IBond                                      I   N=          33
           2           3           7           1           4           8
           1           6           0           2           5           9
           4           6          10           3           5          11
           1           0           0           2           0           0
           4           0           0           5           0           0
           6           0           0
RBond                                      R   N=          33
  1.50000000E+00  1.50000000E+00  1.00000000E+00  1.50000000E+00  1.50000000E+00
  1.00000000E+00  1.50000000E+00  1.50000000E+00  0.00000000E+00  1.50000000E+00
  1.50000000E+00  1.00000000E+00  1.50000000E+00  1.50000000E+00  1.00000000E+00
  1.50000000E+00  1.50000000E+00  1.00000000E+00  1.00000000E+00  0.00000000E+00
  0.00000000E+00  1.00000000E+00  0.00000000E+00  0.00000000E+00  1.00000000E+00
  0.00000000E+00  0.00000000E+00  1.00000000E+00  0.00000000E+00  0.00000000E+00
  1.00000000E+00  0.00000000E+00  0.00000000E+00
   */

  private void readBonds() {
    float[] iBond  = (float[]) fileData.get("IBond");
    if (iBond == null)
      return;
    float[] rBond  = (float[]) fileData.get("RBond");
    int mxBond = ((Integer) fileData.get("MxBond")).intValue();
    for (int ia = 0, pt = 0; ia < atomCount; ia++)
      for (int j = 0; j < mxBond; j++, pt++) {
        int ib = (int) iBond[pt] - 1;
        if (ib <= ia)
          continue;
        float order = rBond[pt];
        int iorder = (order == 1.5f ? JmolAdapter.ORDER_AROMATIC : (int) order);
        asc.addBond(new Bond(ia, ib, iorder));        
      }
  }
  
  private void readDipoleMoment() throws Exception {
    float[] data = (float[]) fileData.get("DipoleMoment");
    if (data == null)
      return;
    V3 dipole = V3.new3(data[0], data[1], data[2]);
    Logger.info("Molecular dipole for model " + asc.atomSetCount
        + " = " + dipole);
    asc.setAtomSetAuxiliaryInfo("dipole", dipole);
  }

  private void readPartialCharges() throws Exception {
    float[] data = (float[]) fileData.get("Mulliken Charges");
    if (data == null)
      return;
    Atom[] atoms = asc.atoms;
    for (int i = 0; i < atomCount; ++i) {
      float c = data[i];
      atoms[i].partialCharge = c;
      if (Math.abs(c) > 0.8f)
        atoms[i].formalCharge = Math.round(c);
    }
    Logger.info("Mulliken charges found for Model " + asc.atomSetCount);
  }
  

  /* SAMPLE BASIS OUTPUT */
  /*
   * see also http://www.gaussian.com/g_ur/k_gen.htm  -- thank you, Rick Spinney

   Standard basis: VSTO-3G (5D, 7F)
   AO basis set:
   Atom O1       Shell     1 SP   3    bf    1 -     4          0.000000000000          0.000000000000          0.216790088607
   0.5033151319D+01 -0.9996722919D-01  0.1559162750D+00
   0.1169596125D+01  0.3995128261D+00  0.6076837186D+00
   0.3803889600D+00  0.7001154689D+00  0.3919573931D+00
   Atom H2       Shell     2 S   3     bf    5 -     5          0.000000000000          1.424913022638         -0.867160354429
   0.3425250914D+01  0.1543289673D+00
   0.6239137298D+00  0.5353281423D+00
   0.1688554040D+00  0.4446345422D+00
   Atom H3       Shell     3 S   3     bf    6 -     6          0.000000000000         -1.424913022638         -0.867160354429
   0.3425250914D+01  0.1543289673D+00
   0.6239137298D+00  0.5353281423D+00
   0.1688554040D+00  0.4446345422D+00
   There are     3 symmetry adapted basis functions of A1  symmetry.
   There are     0 symmetry adapted basis functions of A2  symmetry.
   There are     1 symmetry adapted basis functions of B1  symmetry.
   There are     2 symmetry adapted basis functions of B2  symmetry.
   
   or
   
    AO basis set in the form of general basis input (Overlap normalization):
      1 0
 S   3 1.00       0.000000000000
      0.1508000000D 01 -0.1754110411D 00
      0.5129000000D 00 -0.4465363900D 00
      0.1362000000D 00  0.1295841966D 01
 S   3 1.00       0.000000000000
      0.2565000000D 01 -0.1043105923D 01
      0.1508000000D 01  0.1331478902D 01
      0.5129000000D 00  0.5613064585D 00
 S   1 1.00       0.000000000000
      0.4170000000D-01  0.1000000000D 01
 P   3 1.00       0.000000000000
      0.4859000000D 01 -0.9457549473D-01
      0.1219000000D 01  0.7434797586D 00
      0.4413000000D 00  0.3668143796D 00
 P   2 1.00       0.000000000000
      0.5725000000D 00 -0.8808640317D-01
      0.8300000000D-01  0.1028397037D 01
 P   1 1.00       0.000000000000
      0.2500000000D-01  0.1000000000D 01
 D   3 1.00       0.000000000000
      0.4195000000D 01  0.4857290090D-01
      0.1377000000D 01  0.5105223094D 00
      0.4828000000D 00  0.5730028106D 00
 D   1 1.00       0.000000000000
      0.1501000000D 00  0.1000000000D 01
 ****
      2 0
...
   */

  //S,X,Y,Z,XX,YY,ZZ,XY,XZ,YZ,XXX,YYY,ZZZ,XYY,XXY,XXZ,XZZ,YZZ,YYZ,XYZ
  private static String[] AO_TYPES = {"F7", "D5", "L", "S", "P", "D", "F", "G", "H"};  
  private void readBasis() throws Exception {
    float[] types = (float[]) fileData.get("Shelltypes");
    gaussianCount = 0;
    shellCount = 0;    
    if (types == null)
      return;
    shellCount = types.length;    
    shells = new  List<int[]>();
    float[] pps = (float[]) fileData.get("Numberofprimitivespershell");
    float[] atomMap = (float[]) fileData.get("Shelltoatommap");
    float[] exps = (float[]) fileData.get("Primitiveexponents");
    float[] coefs = (float[]) fileData.get("Contractioncoefficients");
    float[] spcoefs = (float[]) fileData.get("P(S=P)Contractioncoefficients");
    gaussians = AU.newFloat2(exps.length);
    for (int i = 0; i < shellCount; i++) {
      String oType = AO_TYPES[(int) types[i] + 3];
      int nGaussians = (int) pps[i];
      int iatom = (int) atomMap[i];
      int[] slater = new int[4];
      slater[0] = iatom - 1;
      if (oType.equals("F7") || oType.equals("D5"))
        slater[1] = JmolAdapter.getQuantumShellTagIDSpherical(oType.substring(0, 1));
      else
        slater[1] = JmolAdapter.getQuantumShellTagID(oType);      
      slater[2] = gaussianCount;
      slater[3] = nGaussians;
      if (Logger.debugging)
        Logger.debug("Slater " + shells.size() + " " + Escape.eAI(slater));
      shells.addLast(slater);
      for (int j = 0; j < nGaussians; j++) {
        float[] g = gaussians[gaussianCount] = new float[3];
        g[0] = exps[gaussianCount]; 
        g[1] = coefs[gaussianCount]; 
        g[2] = spcoefs[gaussianCount]; 
        gaussianCount++;
      }
    }
    Logger.info(shellCount + " slater shells read");
    Logger.info(gaussianCount + " gaussian primitives read");
  }
  
  private void readMOs() throws Exception {
    if (shells == null)
      return;
    int nElec = ((Integer) fileData.get("Numberofelectrons")).intValue();
    int nAlpha = ((Integer) fileData.get("Numberofalphaelectrons")).intValue();
    int nBeta = ((Integer) fileData.get("Numberofbetaelectrons")).intValue();
    //int mult = ((Integer) fileData.get("Multiplicity")).intValue();
    float[] aenergies = (float[]) fileData.get("AlphaOrbitalEnergies");
    float[] benergies = (float[]) fileData.get("BetaOrbitalEnergies");
    float[] acoefs = (float[]) fileData.get("AlphaMOcoefficients");
    float[] bcoefs = (float[]) fileData.get("BetaMOcoefficients");
    if (acoefs == null)
      return;
    int occ = (bcoefs == null ? 2 : 1);
    int n = (bcoefs == null ? nElec : nAlpha);
    getOrbitals(aenergies, acoefs, occ, n);
    if (bcoefs != null)
      getOrbitals(benergies, bcoefs, occ, nBeta);
    setMOData(false); 
  }
  
  private void getOrbitals(float[] e, float[] c, int occ, int nElec) {
    int nOrb = e.length;
    int nCoef = c.length;
    nCoef /= nOrb;
    alphaBeta = (occ == 2 ? "" : alphaBeta.equals("alpha") ? "beta" : "alpha");
    int pt = 0;
    int n = 0;
    for (int i = 0; i < nOrb; i++) {
      float[] coefs = new float[nCoef];
      for (int j = 0; j < nCoef; j++)
        coefs[j] = c[pt++];
      Map<String, Object> mo = new Hashtable<String, Object>();
      mo.put("coefficients", coefs);
      mo.put("occupancy", Float.valueOf(occ));
      n += occ;
      if (n >= nElec)
        occ = 0;
      mo.put("energy", Float.valueOf(e[i]));
      mo.put("type", alphaBeta);
      setMO(mo);
    }
  }

  private void readFrequencies() throws Exception, IOException {
//    discardLinesUntilContains(":");
//    if (line == null)
//      throw (new Exception("No frequencies encountered"));
//    while ((line= rd()) != null && line.length() > 15) {
//      // we now have the line with the vibration numbers in them, but don't need it
//      String[] symmetries = getTokensStr(rd());
//      String[] frequencies = getTokensAt(
//          discardLinesUntilStartsWith(" Frequencies"), 15);
//      String[] red_masses = getTokensAt(
//          discardLinesUntilStartsWith(" Red. masses"), 15);
//      String[] frc_consts = getTokensAt(
//          discardLinesUntilStartsWith(" Frc consts"), 15);
//      String[] intensities = getTokensAt(
//          discardLinesUntilStartsWith(" IR Inten"), 15);
//      int iAtom0 = asc.ac;
//      int ac = asc.getLastAtomSetAtomCount();
//      int frequencyCount = frequencies.length;
//      boolean[] ignore = new boolean[frequencyCount];
//      for (int i = 0; i < frequencyCount; ++i) {
//        ignore[i] = !doGetVibration(++vibrationNumber);
//        if (ignore[i])
//          continue;  
//        asc.cloneLastAtomSet();
//        // set the properties
//        //String name = asc.setAtomSetFrequency("Calculation " + calculationNumber, symmetries[i], frequencies[i], null);
//        //appendLoadNote("model " + asc.atomSetCount + ": " + name);
//        //namedSets.set(asc.currentAtomSetIndex);
//        asc.setAtomSetModelProperty("ReducedMass",
//            red_masses[i]+" AMU");
//        asc.setAtomSetModelProperty("ForceConstant",
//            frc_consts[i]+" mDyne/A");
//        asc.setAtomSetModelProperty("IRIntensity",
//            intensities[i]+" KM/Mole");
//      }
//      discardLinesUntilContains(" AN ");
//      fillFrequencyData(iAtom0, ac, ac, ignore, true, 0, 0, null, 0);
//    }
  }
  
}
