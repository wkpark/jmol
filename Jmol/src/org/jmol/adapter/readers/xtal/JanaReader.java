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

import org.jmol.util.Logger;

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
    if (ms != null)
      ms.setModulation(false);
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
      if (nGroups != 0 && i > 0)
        throw new Exception(
            "Jmol cannot read rigid body M40 files for composites");
      if (newSub != null)
        newSub.set(n = n + nFree);
    }
    iSub = (newSub == null ? 0 : 1);
    int nAtoms = -1;
    boolean allowAltLoc = (iSub == 0);
    Lst<Atom> molAtoms = null;
    Lst<P3> freePositions = null;
    Hashtable<String, T3> pts = null;
    String molName = null;
    String refAtomName = null;
    int refType = 0;
    P3 pt0 = null;
    if (nGroups > 0) {
      Logger.info("JanaReader found " + nFree + " free atoms and " + nGroups
          + " groups");
      molAtoms = new Lst<Atom>();
      pts = new Hashtable<String, T3>();
      freePositions = new Lst<P3>();
      ms.setGroupPoints(pts);
      if (allowAltLoc)
        asc.setAtomSetAuxiliaryInfo("altLocsAreBondSets", Boolean.TRUE);
    }
    while (readM40Floats(r) != null) {
      while (line != null
          && (line.length() == 0 || line.charAt(0) == ' ' || line.charAt(0) == '-')) {
        // skip entry for a filtered atom
        readM40Floats(r);
      }
      if (line == null)
        break;
      nAtoms++;
      Atom atom = new Atom();
      Logger.info(line);
      atom.atomName = name = line.substring(0, 9).trim();
      if (!filterAtom(atom, 0))
        continue;
      if (iSub > 0) {
        if (newSub.get(nAtoms))
          iSub++;
        atom.altLoc = ("" + iSub).charAt(0);
        //mr.addSubsystem("" + iSub, null, atom.atomName);
      }
      atom.foccupancy = floats[2];
      setAtomCoordXYZ(atom, floats[3], floats[4], floats[5]);
      if (Float.isNaN(floats[2])) {
        // new "molecule" group
        refType = getInt(10, 11);
        // IR The type of the reference point 
        // (0=explicit, 1=gravity centre, 2=geometry centre)
        //if (refType != 0)
         // throw new Exception(
          //    "Jmol can only read rigid body groups with explicit references (not point groups)");
        refAtomName = null;
        if (Float.isNaN(floats[4]))
          refAtomName = line.substring(28, 37).trim();
        else
          pt0 = P3.newP(atom);
        molName = name;
        molAtoms.clear();
        freePositions.clear();
        continue;
      }
      if (modDim == 0) {
        asc.addAtom(atom);
        continue;
      }
      String posName = null;
      if (name.equals(refAtomName))
        pt0 = P3.newP(atom);
      else if (name.startsWith("pos#"))
        posName = name;
      if (posName == null) {
        readModulation(r, atom);
        asc.addAtom(atom);
        if (molAtoms != null) {
          molAtoms.addLast(atom);
          freePositions.addLast(P3.newP(atom));
        }
      } else {
        if (molAtoms.size() == 0 || !allowAltLoc)
          continue;
        processPosition(r, molName, posName, atom, molAtoms, freePositions,
            pt0, pts, isAxial);
      }
    }
    r.close();
  }

  private void processPosition(BufferedReader r, String molName,
                               String posName, Atom atom, Lst<Atom> molAtoms, Lst<P3> freePositions,
                               T3 ptRef, Hashtable<String, T3> pts, boolean isAxial)
      throws Exception {
    char altLoc = (posName.length() == 5 ? posName.charAt(4)
        : (char) (55 + parseIntStr(posName.substring(4))));
    // this does not seem to be quite right
    // because some files may use "pos#1" for two 
    // atoms that have different modulations but 
    // are really different true molecules, not just different groups
    // no obvious fix for this
    boolean isImproper = (getInt(10, 11) == -1); // "sign" of rotation
    int systType = getInt(13, 14); 
    // Type of the local coordinate system. 
    // 0 if the basic crystallographic setting is used. 
    // 1 if the local system for the model molecule is defined 
    //   explicitly
    // 2 if an explicit choice is used also for the actual position.  
    if (systType != 0)
      throw new Exception(
          "Jmol can only read rigid body groups with explicit references (not point groups)");
    readModulation(r, atom);
    String name = atom.atomName;
    int n = molAtoms.size();
    Logger.info(name + " Molecule " + molName + " has " + n + " atoms");
    String script = "";
    String ext = "_" + name.substring(4);
    //atom.anisoBorU are the rotation/translation terms.   
    //  isAxial: Z Y X (Z first)
    // notAxial: Z X Z
    Quat phi = Quat.newAA(A4.newVA(V3.new3(0, 0, 1), (float)(atom.anisoBorU[0] / 180* Math.PI)));
    Quat chi = Quat.newAA(A4.newVA(isAxial ? V3.new3(0, 1, 0) : V3.new3(1, 0, 0), (float)(atom.anisoBorU[1] / 180* Math.PI)));
    Quat psi = Quat.newAA(A4.newVA(isAxial ? V3.new3(1, 0, 0) : V3.new3(0, 0, 1), (float)(atom.anisoBorU[2] / 180* Math.PI)));
    V3 vTrans = V3.new3(atom.anisoBorU[3], atom.anisoBorU[4], atom.anisoBorU[5]);
    Quat qR =phi.mulQ(chi).mulQ(psi);
    System.out.println(qR);
    P3 ptMod = P3.newP(ptRef);
    ptMod.add(vTrans);
    //getModelPosition(ptRef, qR, vTrans, isImproper, ptMod);
    for (int i = 0; i < n; i++) {
      // process molecule atoms
      Atom a = molAtoms.get(i);
      String newName = a.atomName;
      if (a.altLoc == '\0') {
        newName += ext;
      } else {
        a = asc.newCloneAtom(a);
        a.setT(freePositions.get(i));
        newName = newName.substring(0, newName.lastIndexOf("_")) + ext;
      }
      pts.put(a.atomName = newName, ptMod);
      System.out.println(a.atomName + " 1 " + a);
      getModelPosition(ptRef, qR, vTrans, isImproper, a);
      System.out.println(a.atomName + " 2 " + a);
      a.altLoc = altLoc;
      script += ", " + newName;
      ms.copyModulations(null, ";" + posName, ";" + newName);
    }
    script = "@" + molName + ext + script.substring(1);
    addJmolScript(script);
    appendLoadNote(script);
  }

  private void getModelPosition(T3 ptRef, Quat qR, V3 vTrans,
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

  private void readModulation(BufferedReader r, Atom atom) throws Exception {
    String label = ";" + atom.atomName;
    boolean haveSpecialOcc = (getInt(60, 61) > 0);
    boolean haveSpecialDisp = (getInt(61, 62) > 0);
    boolean haveSpecialUij = (getInt(62, 63) > 0);
    int nOcc = getInt(65, 68);
    int nDisp = getInt(68, 71);
    int nUij = getInt(71, 74);
    // read anisotropies
    readM40Floats(r);
    boolean extended = false;
    if (Float.isNaN(floats[0])) {
      extended = true;
      readM40Floats(r); // second atom line
    }
    System.out.println(line);
    boolean isIso = true;
    for (int j = 1; j < 6; j++)
      if (floats[j] != 0) {
        isIso = false;
        break;
      }
    if (isIso) {
      if (floats[0] != 0)
        setU(atom, 7, floats[0]);
    } else {
      for (int j = 0; j < 6; j++)
        setU(atom, j, floats[j]);
    }

    if (extended) {
      r.readLine();
      r.readLine(); //???
    }

    // read occupancy parameters
    double[] pt;
    float o_0 = (nOcc > 0 && !haveSpecialOcc ? parseFloatStr(r.readLine())
        : 1);
    // we add a pt that save the original (unadjusted) o_0 and o_site
    // will implement 
    //
    //  O = o_site (o_0 + SUM)
    //
    // However, first we need to adjust o_0 because the value given in m40 is 
    // divided by the number of operators giving this site.

    if (o_0 != 1)
      ms.addModulation(null, "J_O#0" + label,
          new double[] { atom.foccupancy, o_0, 0 }, -1);
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
        a2 = floats[0]; // sin (first on line)
        a1 = floats[1]; // cos (second on line)
      }
      pt = new double[] { a1, a2, 0 };
      if (a1 != 0 || a2 != 0)
        ms.addModulation(null, "O_" + wv + "#0" + label, pt, -1);
    }

    // read displacement data
    for (int j = 0; j < nDisp; j++) {
      if (haveSpecialDisp) {
        readM40Floats(r);
        float c = floats[3];
        float w = floats[4];
        for (int k = 0; k < 3; k++)
          if (floats[k] != 0)
            ms.addModulation(null, "D_S#" + LABELS.charAt(k) + label,
                new double[] { c, w, floats[k] }, -1);
      } else {
        // Fourier
        addSinCos(j, "D_", label, r);
      }
    }
    // finally read Uij sines and cosines
    for (int j = 0; j < nUij; j++) {
      checkFourier(j);
      if (isIso) {
        // fourier?
        addSinCos(j, "U_", label, r);
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

  private void addSinCos(int j, String key, String label, BufferedReader r) throws Exception {
    checkFourier(j);
    readM40Floats(r);
    for (int k = 0; k < 3; ++k) {
      float ccos = floats[k + 3];
      float csin = floats[k];
      if (csin == 0 && ccos == 0)
        continue;
      String axis = "" + LABELS.charAt(k % 3);
      if (modAxes != null && modAxes.indexOf(axis.toUpperCase()) < 0)
        continue;
      String id = key + (j + 1) + "#" + axis + label;
      ms.addModulation(null, id, new double[] { ccos, csin, 0 }, -1);
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
   * @return
   */
  private int getInt(int col1, int col2) {
    int n = line.length();
    return (n > col1 ? parseIntStr(line.substring(col1, Math.min(n, col2))) : 0);
  }

  private float[] floats = new float[6];
  
  private String readM40Floats(BufferedReader r) throws Exception {
    if ((line = r.readLine()) == null || line.indexOf("-------") >= 0) 
      return (line = null);
    if (Logger.debugging)
      Logger.debug(line);
    int ptLast = line.length() - 9;
    for (int i = 0, pt = 0; i < 6 && pt <= ptLast; i++, pt += 9)
      floats[i] = parseFloatStr(line.substring(pt, pt + 9));
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
  public void doPreSymmetry() {
    if (ms != null)
      ms.setModulation(false);
  }


}
