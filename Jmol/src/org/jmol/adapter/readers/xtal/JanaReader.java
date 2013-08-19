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


import org.jmol.adapter.readers.cif.ModulationReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.io.JmolBinary;
import org.jmol.util.JmolList;
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.TextFormat;

/**
 * A reader for Jana M50+M40 file pairs.  *
 * @author Bob Hanson hansonr@stolaf.edu 8/7/2013  
 */

public class JanaReader extends ModulationReader {

  private JmolList<float[]> lattvecs;
  
  @Override
  public void initializeReader() throws Exception {
      setFractionalCoordinates(true);
      initializeMod();
      atomSetCollection.newAtomSet();
  }
  
  final static String records = "tit  cell ndim qi   lat  sym  spg  end";
  //                             0    5    10   15   20   25   30   35
  final static int TITLE = 0;
  final static int CELL  = 5;
  final static int NDIM  = 10;
  final static int QI    = 15;
  final static int LATT  = 20;
  final static int SYM   = 25;
  final static int SPG   = 30;
  final static int END   = 35;
  
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
    }
    return true;
  }

  @Override
  public void finalizeReader() throws Exception {
    readM40Data();
    if (lattvecs != null)
      atomSetCollection.getSymmetry().addLatticeVectors(lattvecs);
    applySymmetryAndSetTrajectory();
    setModulation();
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
      lattvecs = new JmolList<float[]>();
    lattvecs.addLast(a);
  }

  private void symmetry() throws Exception {
    setSymmetryOperator(TextFormat.simpleReplace(line.substring(9).trim()," ", ","));
  }

  private final String LABELS = "xyz";

  
  //  12    0    0    1
  //  1.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000                                          00
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //
  //                             x        y        z             CS?  O  D  U
  // Zn        5  1     0.500000 0.250000 0.406400 0.244000      000  0  2  0
  //
  // 0         1         2         3         4         5         6         7
  // 01234567890123456789012345678901234567890123456789012345678901234567890
  //
  //  0.048000 0.000000 0.000000 0.000000 0.000000 0.000000      0000000000
  //  0.015300 0.000000 0.000000-0.010100 0.000000 0.000000      000000
  //  0.000000 0.000200-0.000100 0.000000 0.000500-0.000400      000000
  //  0.000000                                                   0

  private void readM40Data() throws Exception {
    String name = filePath;
    int ipt = name.lastIndexOf(".");
    if (ipt < 0)
      return;
    name = name.substring(0, ipt + 2) + "40";
    String id = name.substring(0, ipt);
    ipt = id.lastIndexOf("/");
    id = id.substring(ipt + 1);
    BufferedReader r = JmolBinary.getBufferedReaderForString((String) viewer.getLigandModel(id, name, "_file", "----"));
    if (readM40Floats(r).startsWith("command"))
      readM40WaveVectors(r);
    int nAtoms = (int) floats[0];
    for (int i = 0; i < nAtoms; i++) {
      while (readM40Floats(r).length() == 0 || line.charAt(0) == ' '
          || line.charAt(0) == '-') {
      }
      
      
      Atom atom = new Atom();
      atom.atomName = line.substring(0, 9).trim();
      if (!filterAtom(atom, 0))
        continue;
      setAtomCoordXYZ(atom, floats[3], floats[4], floats[5]);
      atomSetCollection.addAtom(atom);
      if (!incommensurate)
        continue;
      String label = ";" + atom.atomName;
      boolean haveCrenel = (getInt(60, 61) > 0);
      boolean haveSawTooth = (getInt(61, 62) > 0);
      boolean haveSomething = (getInt(62, 63) > 0);
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
      if (nOcc > 0 && !haveCrenel)
        r.readLine(); //"1.00000"
      int wv = 0;
      float a1, a2;
      for (int j = 0; j < nOcc; j++) {
        if (haveCrenel) {
          float[][] data = readM40FloatLines(2, 1, r);
          a1 = data[1][0];
          a2 = data[0][0];
        } else {
          wv = j + 1;
          readM40Floats(r);
          a1 = floats[1];
          a2 = floats[0];          
        }
        id = "O_" + wv + "#0" + label;        
        pt = P3.new3(a1, a2, 0);
        if (a1 != 0 || a2 != 0)
          addModulation(null, id, pt, -1);
      }
      
      // read displacement data
      for (int j = 0; j < nDisp; j++) {
        if (haveSawTooth) {
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
          float[][] data = readM40FloatLines(2, 6, r);
          for (int k = 0, p = 0; k < 6; k++, p+=3)
            addModulation(null, "U_" + (j + 1) + "#"
                + U_LIST.substring(p, p + 3) + label, P3.new3(data[1][k],
                data[0][k], 0), -1);
        }
      }
    }
    r.close();
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
    line = r.readLine();
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

}
