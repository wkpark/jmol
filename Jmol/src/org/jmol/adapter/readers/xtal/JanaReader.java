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


import org.jmol.adapter.readers.cif.ModulationReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.io.JmolBinary;
import org.jmol.java.BS;

import javajs.util.List;
import org.jmol.util.Logger;

import javajs.util.M4;
import javajs.util.P3;

/**
 * A reader for Jana M50+M40 file pairs.  *
 * @author Bob Hanson hansonr@stolaf.edu 8/7/2013  
 */

public class JanaReader extends ModulationReader {

  private List<float[]> lattvecs;
  private int thisSub;
  
  @Override
  public void initializeReader() throws Exception {
      setFractionalCoordinates(true);
      initializeModulation();
      atomSetCollection.newAtomSet();
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
        atomSetCollection.setAtomSetName(line.substring(5).trim());
        break;
      case CELL:
        cell();
        setSymmetryOperator("x,y,z");
        break;
      case NDIM:
        ndim();
        break;
      case LATT:
        lattvec(line.substring(8));
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
        M4 m = new M4();
        if (thisSub++ == 0) {
          m.setIdentity();
          addSubsystem("1", m, null);
          thisSub++;
          m = new M4();
        }
        float[] data = new float[16];
        fillFloatArray(null, 0, data);
        m.setA(data, 0);
        addSubsystem("" + thisSub, m, null);
    }
    return true;
  }

  @Override
  public void finalizeReader() throws Exception {
    readM40Data();
    if (lattvecs != null)
      atomSetCollection.getSymmetry().addLatticeVectors(lattvecs);
    applySymmetryAndSetTrajectory();
    adjustM40Occupancies();
    setModulation();
    finalizeModulation();
    finalizeReaderASCR();
  }
  
  private void cell() throws Exception {
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseFloat());
  }

  private void ndim() {
    setModDim(parseIntStr(getTokens()[1]) - 3);
  }

  private int qicount;

  private void qi() {
    P3 pt = P3.new3(parseFloat(), parseFloat(), parseFloat());
    addModulation(null, "W_" + (++qicount), pt, -1);
    pt = new P3();
    switch (qicount) {
    case 1:
      pt.x = 1;
      break;
    case 2:
      pt.y = 1;
      break;
    case 3:
      pt.z = 1;
      break;
    }
    addModulation(null, "F_" + qicount + "_q_", pt, -1);
  }
   private void lattvec(String data) throws Exception {
    float[] a;
    char c = data.charAt(0);
    switch(c) {
    case 'P':
    case 'X':
      return;
    case 'A':
    case 'B':
    case 'C':
    case 'I':
      a = new float[] {0.5f, 0.5f, 0.5f};
      if (c != 'I')
        a[c - 'A'] = 0;
      break; 
    case 'F':
      lattvec("A");
      lattvec("B");
      lattvec("C");
      return;
    case '0': // X explicit
      if (data.indexOf(".") < 0)
        return; // lattvec 0 0 0 unnecessary
      a = getTokensFloat(data, null, modDim + 3);
      break;
    default:
      appendLoadNote(line + " not supported");
      return;
    }
    if (lattvecs == null)
      lattvecs = new List<float[]>();
    lattvecs.addLast(a);
  }

  private void symmetry() throws Exception {
    setSymmetryOperator(javajs.util.PT.simpleReplace(line.substring(9).trim()," ", ","));
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
    BufferedReader r = JmolBinary.getBufferedReaderForString((String) viewer
        .getLigandModel(id, name, "_file", "----"));
    if (readM40Floats(r).startsWith("command"))
      readM40WaveVectors(r);
    BS newSub = getSubSystemList();
    int iSub = (newSub == null ? 0 : 1);
    int nAtoms = -1;
    while (readM40Floats(r) != null) {
      while (line != null && (line.length() == 0 || line.charAt(0) == ' '
          || line.charAt(0) == '-')) {
        readM40Floats(r);
      }
      if (line == null)
        break;
      nAtoms++;
      Atom atom = new Atom();
      atom.atomName = line.substring(0, 9).trim();
      Logger.info(line);
      if (!filterAtom(atom, 0))
        continue;
      if (iSub > 0) {
        if (newSub.get(nAtoms))
          iSub++;
        addSubsystem("" + iSub, null, atom.atomName);
      }
      float o_site = atom.foccupancy = floats[2];
      setAtomCoordXYZ(atom, floats[3], floats[4], floats[5]);
      atomSetCollection.addAtom(atom);
      if (!incommensurate)
        continue;
      String label = ";" + atom.atomName;
      boolean haveSpecialOcc = (getInt(60, 61) > 0);
      boolean haveSpecialDisp = (getInt(61, 62) > 0);
      boolean haveSpecialUij = (getInt(62, 63) > 0);
      int nOcc = getInt(65, 68);
      int nDisp = getInt(68, 71);
      int nUij = getInt(71, 74);
      // read anisotropies
      readM40Floats(r);
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

      // read occupancy parameters
      P3 pt;
      float o_0 = (nOcc > 0 && !haveSpecialOcc ? parseFloatStr(r.readLine()) : 1);
      // we add a pt that save the original (unadjusted) o_0 and o_site
      // will implement 
      //
      //  O = o_site (o_0 + SUM)
      //
      // However, first we need to adjust o_0 because the value given in m40 is 
      // divided by the number of operators giving this site.
      if (o_0 != 1) {
        addModulation(null, "J_O#0;" + atom.atomName, P3.new3(o_site, o_0, 0), -1);
      }
      atom.foccupancy = o_0 * o_site;
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
          a2 = floats[0];  // sin (first line)
          a1 = floats[1];  // cos (second line)
        }
        id = "O_" + wv + "#0" + label;
        pt = P3.new3(a1, a2, 0);
        if (a1 != 0 || a2 != 0)
          addModulation(null, id, pt, -1);
      }

      // read displacement data
      for (int j = 0; j < nDisp; j++) {
        if (haveSpecialDisp) {
          readM40Floats(r);
          float c = floats[3];
          float w = floats[4];
          for (int k = 0; k < 3; k++)
            if (floats[k] != 0)
              addModulation(null, "D_S#" + LABELS.charAt(k) + label, P3.new3(c,
                  w, floats[k]), -1);
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
              addModulation(null, "U_" + (j + 1) + "#"
                  + U_LIST.substring(p, p + 3) + label, P3.new3(data[1][k],
                  data[0][k], 0), -1);
          }
        }
      }
    }
    r.close();
  }

  private BS getSubSystemList() {
    if (htSubsystems == null)
      return null;
    BS bs = new BS();
    String[] tokens = getTokens();
    for (int i = 0, n = 0; i < tokens.length; i+= 2) {
      int nAtoms = parseIntStr(tokens[i]);
      if (nAtoms == 0)
        break;
      bs.set(n = n + nAtoms);
    }
    return bs;
  }

  private void readM40WaveVectors(BufferedReader r) throws Exception {
    while (!readM40Floats(r).contains("end"))
      if (line.startsWith("wave")) {
        String[] tokens = getTokens();
        P3 pt = new P3();
        switch (modDim) {
        case 3:
          pt.z = parseFloatStr(tokens[4]);
          //$FALL-THROUGH$
        case 2:
          pt.y = parseFloatStr(tokens[3]);
          //$FALL-THROUGH$
        case 1:
          pt.x = parseFloatStr(tokens[2]);
        }
        addModulation(null, "F_" + parseIntStr(tokens[1]) + "_q_", pt, -1);
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
      P3 pt = P3.new3(ccos, csin, 0);
      addModulation(null, id, pt, -1);
    }
  }

  private void checkFourier(int j) {
    P3 pt;
    if (j > 0 && getModulationVector("F_" + (j + 1) + "_q_") == null && (pt = getModulationVector("F_1_q_")) != null) {
      pt = P3.newP(pt);
      pt.scale(j + 1);
      addModulation(null, "F_" + (j + 1) + "_q_", pt, -1);
    }
  }

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
    int ptLast = line.length() - 10;
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
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = atomSetCollection.getAtomCount(); --i >= 0;) {
      Atom a = atoms[i];
      Integer ii = htSiteMult.get(a.atomName);
      if (ii == null) {
        htSiteMult.put(a.atomName, ii = Integer.valueOf(atomSetCollection.getSymmetry().getSiteMultiplicity(a)));
        //System.out.println(a.atomName + " " + ii + " " + a.bsSymmetry + " " + a.foccupancy);
      }
      a.foccupancy *= ii.intValue();
    }
  }
}
