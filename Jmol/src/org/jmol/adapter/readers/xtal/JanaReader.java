  /* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.xtal;

import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.MSInterface;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;

import javajs.util.A4;
import javajs.util.P3;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.Lst;
import javajs.util.Matrix;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Modulation;

/**
 * A reader for Jana M50+M40 file pairs.  *
 * @author Bob Hanson hansonr@stolaf.edu 8/7/2013  
 */

public class JanaReader extends AtomSetCollectionReader {

  private Lst<float[]> lattvecs;
  private int thisSub;
  private boolean modAverage;
  private String modAxes;
  private int modDim;
  
  @Override
  public void initializeReader() throws Exception {
    modAverage = checkFilterKey("MODAVE");
    modAxes = getFilter("MODAXES=");
    setFractionalCoordinates(true);
    asc.newAtomSet();
  }
  
  final static String records = "tit  cell ndim qi   lat  sym  spg  end  wma";
  //                             0    5    10   15   20   25   30   35   40
  final static int TITLE = 0;
  final static int CELL  = 5;
  final static int NDIM  = 10;
  final static int QI    = 15;
  final static int LATT  = 20;
  final static int SYM   = 25;
  final static int SPG   = 30;
  final static int END   = 35;
  final static int WMATRIX = 40;
  
//  Version Jana2006
//  title
//  cell 8.987 15.503 12.258 90 90 90
//  esdcell 0.002 0.002 0.002 0 0 0
//  ndim 4 ncomp 1
//  qi 0 0 0.413
//  qr 0 0 0
//  spgroup Pmcn(00g)s00 62 3
//  lattice P
//  symmetry x1 x2 x3 x4
//  symmetry -x1+1/2 -x2+1/2 x3+1/2 x4+1/2
//  symmetry -x1 x2+1/2 -x3+1/2 -x4
//  symmetry x1+1/2 -x2 -x3 -x4+1/2
//  symmetry -x1 -x2 -x3 -x4
//  symmetry x1+1/2 x2+1/2 -x3+1/2 -x4+1/2
//  symmetry x1 -x2+1/2 x3+1/2 x4
//  symmetry -x1+1/2 x2 x3 x4+1/2

  @Override
  protected boolean checkLine() throws Exception {
    if (line.length() < 3)
      return true;
    Logger.info(line);
    parseTokenStr(line);
    switch (records.indexOf(line.substring(0, 3))) {
      case TITLE:
        asc.setAtomSetName(line.substring(5).trim());
        break;
      case CELL:
        cell();
        setSymmetryOperator("x,y,z");
        break;
      case NDIM:
        ndim();
        break;
      case LATT:
        if (lattvecs == null)
          lattvecs = new Lst<float[]>();
        if (!ms.addLatticeVector(lattvecs, line.substring(8)))
          appendLoadNote(line + " not supported");
        break;
      case SPG:
        setSpaceGroupName(getTokens()[1]);
        break;
      case SYM:
        symmetry();        
        break;
      case QI:
        if (!modAverage)
          qi();
        break;
      case END:
        continuing = false;
        break;
      case WMATRIX:
        int n = 3 + modDim;
        Matrix m;
        if (thisSub++ == 0) {
          m = Matrix.identity(n, n);
          ms.addSubsystem("" + thisSub++, m);
        }
        m = new Matrix(null, n, n);
        double[][] a = m.getArray();
        float[] data = new float[n * n];
        fillFloatArray(null, 0, data);
        for (int i = 0, pt = 0; i < n; i++)
          for (int j = 0; j < n; j++, pt++)
             a[i][j] = data[pt];
        ms.addSubsystem("" + thisSub, m);
    }
    return true;
  }

  @Override
  public void finalizeReader() throws Exception {
    readM40Data();
    if (lattvecs != null && lattvecs.size() > 0)
      asc.getSymmetry().addLatticeVectors(lattvecs);
    applySymmetryAndSetTrajectory();
    adjustM40Occupancies();
    if (ms != null) {
      ms.setModulation(true);
      ms.finalizeModulation();
    }
    finalizeReaderASCR();
  }
  
  private void cell() throws Exception {
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseFloat());
  }

  private void ndim() throws Exception {
    ms = (MSInterface) Interface
        .getOption("adapter.readers.cif.MSReader");
    modDim = ms.initialize(this, (parseIntStr(getTokens()[1]) - 3));
  }

  private int qicount;

  private void qi() {
    double[] pt = new double[modDim];
    pt[qicount] = 1;
    ms.addModulation(null, "W_" + (++qicount), new double[] {parseFloat(), parseFloat(), parseFloat()}, -1);
    ms.addModulation(null, "F_" + qicount + "_coefs_", pt, -1);
  }

  private void symmetry() throws Exception {
    setSymmetryOperator(PT.rep(line.substring(9).trim()," ", ","));
  }

  private final String LABELS = "xyz";

  
  //  12    0    0    1
  //  1.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000                                          00
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //
  //                                                         SPECIAL   COUNTS
  //                                                             ---  -------
  //                             x        y        z             ODU  O  D  U
  // Zn        5  1     0.500000 0.250000 0.406400 0.244000      000  0  2  0
  //
  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  //
  //  0.048000 0.000000 0.000000 0.000000 0.000000 0.000000      0000000000
  //  0.015300 0.000000 0.000000-0.010100 0.000000 0.000000      000000
  //  0.000000 0.000200-0.000100 0.000000 0.000500-0.000400      000000
  //  0.000000                                                   0
  // ...
  //                   [occ_site]
  // Na1       1  2     0.500000 0.500000 0.000000 0.000000      000  1  1  1
  //  0.034155-0.007468 0.002638 0.000000-0.005723 0.000000      0000111010
  // [occ_0 for Fourier or width for crenel] 
  //  0.848047                                                   1
  // [center for Crenel; ]
  //  0.000000 0.312670                                          01
  //  0.029441 0.000000 0.003581 0.000000 0.000000 0.000000      101000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  // -0.051170 0.000624-0.008585 0.000000 0.014781 0.000000      111010
  //  0.000000
  //

  private String molName;
  private Lst<Atom> molAtoms;
  private Lst<Integer> molTtypes;
  private Lst<P3> freePositions;
  private boolean molHasTLS;
  private Quat qR;


  /**
   * read the M40 file
   * 
   * @throws Exception
   */
  private void readM40Data() throws Exception {
    String name = filePath;
    int ipt = name.lastIndexOf(".");
    if (ipt < 0)
      return;
    name = name.substring(0, ipt + 2) + "40";
    String id = name.substring(0, ipt);
    ipt = id.lastIndexOf("/");
    id = id.substring(ipt + 1);
    BufferedReader r = Rdr.getBR((String) vwr.getLigandModel(id, name, "_file",
        "----"));
    if (readM40Floats(r).startsWith("command"))
      readM40WaveVectors(r);

    // ref: manual98.pdf
    // Jana98: The Crystallographic Computing System
    // Vaclav Petricek and Michal Dusek,Dec. 2000
    //    p. 98
    //    Header numbers (This is part of table in page 98)
    //    Nat1 Nmol1 Nat21 Nmol2 Nat32 Nmol3 Itemp Irot
    //    Natm1 Npos1
    //    Natm2 Npos2 Nmol1 lines for the 1st composite subsystem
    //    ......
    //    The header of m40 contains number of atoms in atomic and molecular parts, number
    //    of molecules and molecular positions. In the case of a composite these numbers are
    //    listed repeatedly for each composite part.
    //    The number of composite parts is given in m50 (see the key ncomp, Table 9, page 80)
    //    and can be defined with PRELIM user interface (see § 2.2.2, page 68). The numbers
    //    for non-existing composite parts are omitted.
    //    Meaning of parameters
    //    Nat1 Number of atoms in the 1st composite part.
    //    Nmol1 Number of molecules3 in the 1st composite part
    //    Nat2 Number of atoms in the 2nd composite part.
    //    Nmol2 Number of molecules in the 2nd composite part
    //    Nat3 Number of atoms in the 3rd composite part.
    //    Nmol3 Number of molecules in the 3rd composite part
    //    Itemp Type of temperature parameters (0 for U, 1 for beta)
    //    Irot Key of molecular rotation (0 for Eulerian, 1 for axial). See page 143
    //    for more information.
    //    Natm1 Number of atoms in the 1st molecule of the 1st composite part
    //    Npos1 Number of positions of the 1st molecule of the 1st composite part
    //    Natm2 Number of atoms in the 2nd molecule of the 1st composite part
    //    Npos2 Number of positions of the 2nd molecule of the 1st composite part

    // except Jana2006 may have changed this:

    int nFree = 0, nGroups = 0;
    boolean isAxial = false;
    BS newSub = (thisSub == 0 ? null : new BS());
    int iSub = (thisSub == 0 ? 1 : thisSub);
    for (int i = 0, n = 0, pt = 0; i < iSub; i++, pt += 10) {
      nFree = getInt(pt, pt + 5);
      nGroups = getInt(pt + 5, pt + 10);
      isAxial = (getInt(pt + 15, pt + 20) == 1);
      if (nGroups != 0 && i > 0) {
        throw new Exception(
            "Jmol cannot read rigid body M40 files for composites");
      }
      if (newSub != null)
        newSub.set(n = n + nFree);
    }
    iSub = (newSub == null ? 0 : 1);
    int nAtoms = -1;
    //Hashtable<String, T3> pts = null;
    String refAtomName = null;
    int refType = 0;
    P3 pt0 = null;
    if (nGroups > 0) {
      Logger.info("JanaReader found " + nFree + " free atoms and " + nGroups
          + " groups");
      molName = null;
      molAtoms = new Lst<Atom>();
      molTtypes = new Lst<Integer>();
      //pts = new Hashtable<String, T3>();
      // ms.setGroupPoints(pts);
      if (thisSub == 0)
        asc.setAtomSetAuxiliaryInfo("altLocsAreBondSets", Boolean.TRUE);
    }
    
    // note that we are skipping scale, overall isotropic temperature factor, and extinction parameters
    
    while (skipToNextAtom(r) != null) {
      nAtoms++;
      Atom atom = new Atom();
      Logger.info(line);
      atom.atomName = name = line.substring(0, 9).trim();
      boolean isRefAtom = name.equals(refAtomName);
      atom.foccupancy = floats[2];
      boolean isJanaMolecule = Float.isNaN(atom.foccupancy);
      if (isJanaMolecule) {
        // new "molecule" group
        refType = getInt(10, 11);
        // IR The type of the reference point 
        // (0=explicit, 1=gravity centre, 2=geometry centre)
        String pointGroup = getStr(12, 18);

        // see http://en.wikipedia.org/wiki/Crystallographic_point_group
        if (pointGroup.length() > 0 && !pointGroup.equals("1")) {
          throw new Exception("Jmol cannot process M40 files with molecule positions based on point-group symmetry.");
        }
        refAtomName = null;
        if (Float.isNaN(floats[4]))
          refAtomName = getStr(28, 37);
        else
          pt0 = P3.new3(floats[3], floats[4], floats[5]);
        molName = name;
        molAtoms.clear();
        molTtypes.clear();
        molHasTLS = false;
        continue;
      }
      boolean isExcluded = false;
      String posName = (name.startsWith("pos#") ? name : null);
      if (posName == null) {
        if (!filterAtom(atom, 0)) {
          if (!isRefAtom)
            continue;
          isExcluded = true;
        }
        setAtomCoordXYZ(atom, floats[3], floats[4], floats[5]);
        if (isRefAtom) {
          pt0 = P3.newP(atom);
          if (isExcluded)
            continue;
        }
        asc.addAtom(atom);
        if (iSub > 0) {
          if (newSub.get(nAtoms))
            iSub++;
          atom.altLoc = ("" + iSub).charAt(0);
        }
        readModulation(r, atom, null, null, false);
        if (molAtoms != null)
          molAtoms.addLast(atom);
      } else {
        if (molAtoms.size() == 0)
          continue;
        processPosition(r, posName, atom, pt0, isAxial);
      }
    }
  }

  private String skipToNextAtom(BufferedReader r) throws Exception {
    while (readM40Floats(r) != null
        && (line.length() == 0 || line.charAt(0) == ' ' || line.charAt(0) == '-')) {
    }
    return line;
  }

  /**
   * We process the Pos#n record here. This involves cloning the free atoms,
   * translating and rotating them to the proper locations, and copying the
   * modulations. Jmol uses the alternative location PDB option (%1, %2,...) to
   * specify the group, enabling the Jmol command DISPLAY configuration=1, for
   * example. We also set a flag to prevent autobonding between alt-loc sets.
   * 
   * At this point we only support systType=1 (basic coordinates)
   * 
   * @param r
   * @param posName
   * @param atom
   * @param ptRef
   * @param isAxial
   * @throws Exception
   */
  private void processPosition(BufferedReader r, String posName, Atom atom,
                               T3 ptRef, boolean isAxial) throws Exception {

    // read the Pos# line.

    atom.atomName = molName + "_" + posName;
    char altLoc = (posName.length() == 5 ? posName.charAt(4)
        : (char) (55 + parseIntStr(posName.substring(4))));
    // this does not seem to be quite right
    // because some files may use "pos#1" for two 
    // atoms that have different modulations but 
    // are really different true molecules, not just different groups
    // no obvious fix for this
    boolean isImproper = (getInt(9, 11) == -1); // "sign" of rotation
    int systType = getInt(13, 14);
    P3 rm = (systType == 0 ? null : new P3());
    P3 rp = (systType == 0 ? null : new P3());

    // Type of the local coordinate system. 
    // 0 if the basic crystallographic setting is used. 
    // 1 if the local system for the model molecule is defined 
    //   explicitly
    // 2 if an explicit choice is used also for the actual position.  
    if (systType != 0) {
      throw new Exception(
          "Jmol can only read rigid body groups with basic crystallographic settings.");
    }

    // read the modulation --  atom.anisoBorU will be the rotation/translation terms.
    float[] rotData = readModulation(r, atom, rm, rp, true);

    // generate R and t in r' = R(r - rho) + rho + t
    // where rho is the model reference position.

    String name = atom.atomName;
    int n = molAtoms.size();
    Logger.info(name + " Molecule " + molName + " has " + n + " atoms");
    String script = "";
    String ext = "_" + posName.substring(4);
    //  isAxial: X Y Z (X first)
    // notAxial: Z X Z
    Quat phi = Quat.newAA(A4.newVA(V3.new3(0, 0, 1),
        (float) (atom.anisoBorU[0] / 180 * Math.PI)));
    Quat chi = Quat.newAA(A4.newVA(
        isAxial ? V3.new3(0, 1, 0) : V3.new3(1, 0, 0),
        (float) (atom.anisoBorU[1] / 180 * Math.PI)));
    Quat psi = Quat.newAA(A4.newVA(
        isAxial ? V3.new3(1, 0, 0) : V3.new3(0, 0, 1),
        (float) (atom.anisoBorU[2] / 180 * Math.PI)));
    V3 vTrans = V3
        .new3(atom.anisoBorU[3], atom.anisoBorU[4], atom.anisoBorU[5]);
    qR = phi.mulQ(chi).mulQ(psi);

    // generate the modulation point for this group

    // process atoms
    P3 ptRel = new P3();
    for (int i = 0; i < n; i++) {
      Atom a = molAtoms.get(i);
      String newName = a.atomName;
      if (a.altLoc == '\0' && a.insertionCode == '\0') {
        newName += ext;
        if (i == 0)
          freePositions = new Lst<P3>();
        freePositions.addLast(P3.newP(a));
      } else {
        a = asc.newCloneAtom(a);
        a.setT(freePositions.get(i));
        newName = newName.substring(0, newName.lastIndexOf("_")) + ext;
      }
      a.atomName = newName;
      getAtomPosition(ptRef, qR, vTrans, isImproper, a);
      if (thisSub == 0)
        a.altLoc = altLoc;
      else
        a.insertionCode = altLoc;
      script += ", " + newName;
      // we define dFrac as a - pt
      ptRel.sub2(a, ptRef);
      copyModulations(";" + atom.atomName, ";" + newName, ptRel);
      if (rotData != null)
        setRigidBodyRotations(";" + newName, ptRel, rotData);
    }

    // generate a script that will define the model name as an atom selection

    script = "@" + molName + ext + script.substring(1);
    addJmolScript(script);
    appendLoadNote(script);
  }

  /**
   * The model position is calculated as
   * 
   *  r' = (+/-)R(r - rho) + rho + t
   * 
   * @param ptRef      rho
   * @param qR         R
   * @param vTrans     t
   * @param isImproper 
   * @param pt         return value
   */
  private void getAtomPosition(T3 ptRef, Quat qR, V3 vTrans,
                                boolean isImproper, P3 pt) {
    P3 rho = P3.newP(ptRef);
    getSymmetry().toCartesian(rho, true);
    symmetry.toCartesian(pt, true);
    pt.sub(rho);
    qR.transformP2(pt, pt);
    if (isImproper)
      pt.scale(-1);
    pt.add(rho);
    symmetry.toFractional(pt, true);
    pt.add(vTrans);
  }

  private float[] readModulation(BufferedReader r, Atom atom, P3 rm, P3 rp,
                                  boolean isPos) throws Exception {
    String label = ";" + atom.atomName;
    int tType = (isPos ? -1 : getInt(13, 14));
    if (!isPos && molTtypes != null)
      molTtypes.addLast(Integer.valueOf(tType));
    boolean haveSpecialOcc = getFlag(60);
    boolean haveSpecialDisp = getFlag(61);
    boolean haveSpecialUij = getFlag(62);
    int nOcc = getInt(65, 68); // could be -1
    int nDisp = getInt(68, 71);
    int nUij = getInt(71, 74);
    if (rm != null) {
      readM40Floats(r);
      rm.set(floats[0], floats[1], floats[2]);
      rp.set(floats[3], floats[4], floats[5]);
    }
    if (tType > 2)
      readM40Floats(r);
    // read anisotropies (or Pos#n rotation/translations)
    readM40Floats(r);
    switch (tType) {
    case 6:
    case 5:
    case 4:
    case 3:
      skipLines(r, tType - 1);
      appendLoadNote("Skipping temperature factors with order > 2");
      //$FALL-THROUGH$
    case 2:
    case -1:
      for (int j = 0; j < 6; j++)
        setU(atom, j, floats[j]);
      break;
    case 1:
      if (floats[0] != 0)
        setU(atom, 7, floats[0]);
      break;
    case 0:
      molHasTLS = true;
      appendLoadNote("Jmol cannot process molecular TLS parameters");
      break;
    }
    if (modDim == 0)
      return null; // return for nonmodulated Pos#n

    if (isPos && molHasTLS)
      skipLines(r, 4);

    // read occupancy modulation

    double[] pt;
    float o_0 = (nOcc > 0 && !haveSpecialOcc ? parseFloatStr(r.readLine()) : 1);
    // we add a pt that saves the original (unadjusted) o_0 and o_site
    // will implement 
    //
    //  O = o_site (o_0 + SUM)
    //
    // However, first we need to adjust o_0 because the value given in m40 is 
    // divided by the number of operators giving this site.

    if (o_0 != 1)
      ms.addModulation(null, "J_O#0" + label, new double[] { atom.foccupancy,
          o_0, 0 }, -1);
    atom.foccupancy *= o_0;
    int wv = 0;
    float a1, a2;
    for (int j = 0; j < nOcc; j++) {
      if (haveSpecialOcc) {
        float[][] data = readM40FloatLines(2, 1, r);
        a2 = data[0][0]; // width (first line)
        a1 = data[1][0]; // center (second line)
      } else {
        wv = j + 1;
        readM40Floats(r);
        a1 = floats[0]; // sin (first on line)
        a2 = floats[1]; // cos (second on line)
      }
      pt = new double[] { a1, a2, 0 };
      if (a1 != 0 || a2 != 0)
        ms.addModulation(null, "O_" + wv + "#0" + label, pt, -1);
    }

    // read displacement modulation

    for (int j = 0; j < nDisp; j++) {
      if (haveSpecialDisp) {
        readM40Floats(r);
        float c = floats[3]; // center
        float w = floats[4]; // width
        for (int k = 0; k < 3; k++)
          if (floats[k] != 0)
            ms.addModulation(null, "D_S#" + LABELS.charAt(k) + label,
                new double[] { c, w, floats[k] }, -1);
      } else {
        // Fourier displacements
        addSinCos(j, "D_", label, r, isPos);
      }
    }

    float[] rotData = null;
    if (isPos && nDisp > 0) {
      int n = nDisp * 6;
      rotData = new float[n];
      for (int p = 0, j = 0; p < n; p++, j++) {
        if (p % 6 == 0) {
          j = 0;
          readM40Floats(r);
        }
        rotData[p] = floats[j];
      }
    }

    // finally read Uij sines and cosines

    if (!isPos) // No TLS here
      for (int j = 0; j < nUij; j++) {
        checkFourier(j);
        if (tType == 1) {
          // fourier?
          addSinCos(j, "U_", label, r, false);
        } else {
          if (haveSpecialUij) {
            //TODO
            Logger.error("JanaReader -- not interpreting SpecialUij flag: "
                + line);
          } else {
            float[][] data = readM40FloatLines(2, 6, r);
            for (int k = 0, p = 0; k < 6; k++, p += 3)
              ms.addModulation(null,
                  "U_" + (j + 1) + "#" + U_LIST.substring(p, p + 3) + label,
                  new double[] { data[1][k], data[0][k], 0 }, -1);
          }
        }
      }
    // higher order temperature factor modulation ignored

    // phason ignored
    return rotData;
  }

  private void skipLines(BufferedReader r, int n) throws Exception {
    for (int i = 1; i < n; i++)
      r.readLine();
  }

  private boolean getFlag(int i) {
    return (getInt(i, i + 1) > 0);
  }

  public final static String U_LIST = "U11U22U33U12U13U23UISO";
  
  private void readM40WaveVectors(BufferedReader r) throws Exception {
    while (!readM40Floats(r).contains("end"))
      if (line.startsWith("wave")) {
        String[] tokens = getTokens();
        double[] pt = new double[modDim];
        for (int i = 0; i < modDim; i++)
          pt[i] = parseFloatStr(tokens[i + 2]);
        ms.addModulation(null, "F_" + parseIntStr(tokens[1]) + "_coefs_", pt, -1);
      }
    readM40Floats(r);
  }

  /**
   * Add x, y, and z modulations as [ csin, ccos, 0]
   * 
   * @param j
   * @param key
   * @param label
   * @param r
   * @param isPos
   * @throws Exception
   */
  private void addSinCos(int j, String key, String label, BufferedReader r, boolean isPos)
      throws Exception {
    checkFourier(j);
    readM40Floats(r);
    for (int k = 0; k < 3; ++k) {
      float csin = floats[k];
      float ccos = floats[k + 3];
      if (csin == 0 && ccos == 0 && !isPos)
        continue;
      String axis = "" + LABELS.charAt(k % 3);
      if (modAxes != null && modAxes.indexOf(axis.toUpperCase()) < 0)
        continue;
      String id = key + (j + 1) + "#" + axis + label;
      ms.addModulation(null, id, new double[] { csin, ccos, 0 }, -1);
    }
  }

  /**
   * presumption here is that there is only one model 
   * (that atModel is "@0") and that there is just one dimension?
   * 
   * @param j
   */
  private void checkFourier(int j) {
    double[] pt;
    if (j > 0 && ms.getMod("F_" + (++j) + "_coefs_") == null && (pt = ms.getMod("F_1_coefs_")) != null) {
      double[] p = new double[modDim];
      for (int i = modDim; --i >= 0;)
        p[i] = pt[i] * j;
      ms.addModulation(null, "F_" + j + "_coefs_", p, -1);
    }
  }

  /**
   * safe int parsing of line.substring(col1, col2);
   * 
   * @param col1
   * @param col2
   * @return value or 0
   */
  private int getInt(int col1, int col2) {
    int n = line.length();
    return (n > col1 ? parseIntStr(getStr(col1, col2)) : 0);
  }
  
  /**
   * safe string parsing of line.substring(col1, col2);
   * 
   * @param col1
   * @param col2
   * @return value or ""
   */
  private String getStr(int col1, int col2) {
    int n = line.length();
    return (n > col1 ? line.substring(col1, Math.min(n, col2)).trim(): "");
  }

  private float[] floats = new float[6];
  
  private String readM40Floats(BufferedReader r) throws Exception {
    if ((line = r.readLine()) == null || line.indexOf("-------") >= 0) 
      return (line = null);
    if (Logger.debugging)
      Logger.debug(line);
    int ptLast = line.length() - 9;
    for (int i = 0, pt = 0; i < 6; i++, pt += 9) {
      floats[i] = (pt <= ptLast ? parseFloatStr(line.substring(pt, pt + 9)) : Float.NaN);
    }
    return line;
  }

  private float[][] readM40FloatLines(int nLines, int nFloats, BufferedReader r) throws Exception {
    float[][] data = new float[nLines][nFloats];
    for (int i = 0; i < nLines; i++) {
      readM40Floats(r);
      for (int j = 0; j < nFloats; j++)
        data[i][j] = floats[j];
    }
    return data;
  }

  /**
   * M40 occupancies are divided by the site multiplicity; 
   * here we factor that back in.
   * 
   */
  private void adjustM40Occupancies() {
    Map<String, Integer> htSiteMult = new Hashtable<String, Integer>();    
    Atom[] atoms = asc.atoms;
    SymmetryInterface symmetry = asc.getSymmetry();
    for (int i = asc.ac; --i >= 0;) {
      Atom a = atoms[i];
      Integer ii = htSiteMult.get(a.atomName);
      if (ii == null)
        htSiteMult.put(a.atomName, ii = Integer.valueOf(symmetry.getSiteMultiplicity(a)));
      a.foccupancy *= ii.intValue();
    }
  }

  @Override
  public void doPreSymmetry() throws Exception {
    if (ms != null)
      ms.setModulation(false);
  }

  private void copyModulations(String label, String newLabel, P3 ptRel) {
    Map<String, double[]> mapTemp = new Hashtable<String, double[]>();
    for (Entry<String, double[]> e : ms.getModulationMap().entrySet()) {
      String key = e.getKey();
      if (!key.contains(label))
        continue;
      key = PT.rep(key, label, newLabel);
      double[] val = e.getValue();
      switch (key.charAt(0)) {
      case 'O':
        setRigidBodyPhase(key, e.getValue(), ptRel);
        break;
      case 'D':
        // we will phase at the time of rotation
        break;
      case 'U':
        // not implemented
        continue;
      }
      mapTemp.put(key, val);
    }

    for (Entry<String, double[]> e : mapTemp.entrySet())
      ms.addModulation(null, e.getKey(), e.getValue(), -1);
  }

  /**
   * Transform unphased Fourier x,y,z cos/sin coefficients in a 
   * rigid body system based on distance from center.
   * @param label ";atomName"
   * @param ptRel vector to atom relative from reference point
   * @param data block of nDisp*6 rotational parameters
   * 
   */
  private void setRigidBodyRotations(String label, T3 ptRel, float[] data) {
    int n = data.length / 6;
    V3 vCart = V3.newV(ptRel);
    symmetry.toCartesian(vCart, true);
    //System.out.println(label + " " + Escape.eAF(data));
    Quat qrrev = qR.inv();
    for (int i = 0, p = 0; i < n; i++, p+= 6) {
      checkFourier(i);
      String key = "D_"+ (i + 1);
      V3 vsin = V3.new3(data[p], data[p + 1], data[p + 2]);
      V3 vcos = V3.new3(data[p + 3], data[p + 4], data[p + 5]);
      
      
      symmetry.toCartesian(vcos,  true);
      vcos.cross(vcos,  vCart);
      symmetry.toFractional(vcos,  true);

      symmetry.toCartesian(vsin,  true);
      vsin.cross(vsin,  vCart);
      symmetry.toFractional(vsin,  true);
      
      addRotMod(key + "#x" + label, vcos.x, vsin.x, ptRel);
      addRotMod(key + "#y" + label, vcos.y, vsin.y, ptRel);
      addRotMod(key + "#z" + label, vcos.z, vsin.z, ptRel);      
    }
  }

  private void addRotMod(String key, float ccos, float csin, T3 pt) {
    double[] v = ms.getMod(key);
    //System.out.println(" addrotmod " + v[0]/csin + " " + v[1]/ccos);
    
    double[] v2 = setRigidBodyPhase(key, new double[] {v[0] + csin,  v[1] + ccos, 0}, pt);
    ms.addModulation(null, key, v2, -1);
  }

  /**
   * 
   * Adjust phases to match the difference between the atom's position and the
   * rigid molecular fragment's reference point.
   * @param key 
   * @param v 
   * @param pt 
   * @return phase-adjusted parameters
   * 
   */
  private double[] setRigidBodyPhase(String key, double[] v, T3 pt) {
    boolean isCenter = false;
    switch (ms.getModType(key)) {
    case Modulation.TYPE_OCC_FOURIER:
    case Modulation.TYPE_DISP_FOURIER:
    case Modulation.TYPE_U_FOURIER:
      break;
    case Modulation.TYPE_OCC_CRENEL:
    case Modulation.TYPE_DISP_SAWTOOTH:
      isCenter = true;
      break;
    }
    double nqDotD = 0;
    double n = -1;
    double[] qcoefs = ms.getQCoefs(key);
    for (int i = modDim; --i >= 0;) {
      if (qcoefs[i] != 0) {
        n = qcoefs[i];
        // n in sin(2 pi n q.ptRel), where pt is (<final atom position> - <reference point>)
        double[] q = ms.getMod("W_" + (i + 1));
        nqDotD = n * (q[0] * pt.x + q[1] * pt.y + q[2] * pt.z);
        break;
      }
    }
    //Logger.info ("unphased coefficients: " + key + " " + n + " " +  v[0] + " " + v[1]);
    if (isCenter) {
      v[0] += nqDotD; // move center of sawtooth or crenel to match this atom
    } else {
      double cA = v[0]; // A sin...
      double cB = v[1]; // B cos....
      double sin = Math.sin(2 * Math.PI * nqDotD);
      double cos = Math.cos(2 * Math.PI * nqDotD);
      v[0] = cA * cos + cB * sin;   // A sin
      v[1] = -cA * sin + cB * cos;  // B cos
    }
    //Logger.info ("phased coefficients: " + key+ " "  + n + " " +  v[0] + " " + v[1]);
    //Logger.info ("");
    return v;
  }


}
