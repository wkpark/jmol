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
import org.jmol.util.Logger;
import org.jmol.util.P3;
import org.jmol.util.TextFormat;

/**
 * A reader for Jana M50+M40 file pairs.  *
 * @author Bob Hanson hansonr@stolaf.edu 8/7/2013  
 */

public class JanaReader extends ModulationReader {

  private String m40Data;
  private String[] tokens;
  @Override
  public void initializeReader() throws Exception {
      setFractionalCoordinates(true);
      initializeMod();
      atomSetCollection.newAtomSet();
      String name = filePath;
      int pt = name.lastIndexOf(".");
      if (pt < 0)
        return;
      name = name.substring(0, pt + 2) + "40";
      String id = name.substring(0, pt);
      pt = id.lastIndexOf("/");
      id = id.substring(pt + 1);
      m40Data = (String) viewer.getLigandModel(id, name, "_file");
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
        lattvec();
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
    finalizeIncommensurate();
    applySymmetryAndSetTrajectory();
    setModulation();
    finalizeReaderASCR();
  }
  
  private final String labels = "xyz";

  //  12    0    0    1
  //  1.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000                                          00
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  //  0.000000 0.000000 0.000000 0.000000 0.000000 0.000000      000000
  // Zn        5  1     0.500000 0.250000 0.406400 0.244000      000  0  2  0
  //  0.048000 0.000000 0.000000 0.000000 0.000000 0.000000      0000000000
  //  0.015300 0.000000 0.000000-0.010100 0.000000 0.000000      000000
  //  0.000000 0.000200-0.000100 0.000000 0.000500-0.000400      000000
  //  0.000000                                                   0

  private void readM40Data() throws Exception {
    if (m40Data == null)
      return;
    BufferedReader r = JmolBinary.getBufferedReaderForString(m40Data);
    readM40Line(r);
    int nAtoms = parseIntStr(tokens[0]);
    for (int i = 0; i < nAtoms; i++) {
      while (readM40Line(r).length() == 0 || line.charAt(0) == ' ') {
      }
      Atom atom = atomSetCollection.addNewAtom();
      atom.atomName = tokens[0];
      setAtomCoordXYZ(atom, parseFloatStr(tokens[4]), parseFloatStr(tokens[5]), parseFloatStr(tokens[6]));
      int nq = (incommensurate && tokens.length > 9 ? parseIntStr(tokens[9]) : 0);
      r.readLine();
      for (int j = 0; j < nq; j++) {
        P3 pt;
        if (j > 0 && getModulationVector("F_" + (j + 1)) == null) {
          pt = P3.newP(getModulationVector("F_1"));
          pt.scale(j + 1);
          addModulation(null, "F_" + (j + 1), pt);
        }
        readM40Line(r);
        System.out.println(line);
        for (int k = 0; k < 3; ++k) {
          float ccos = parseFloatStr(tokens[k]);
          float csin = parseFloatStr(tokens[k + 3]);
          if (csin == 0 && ccos == 0)
            continue;
          String axis = "" + labels.charAt(k % 3);
          if (modAxes != null
              && modAxes.indexOf(axis.toUpperCase()) < 0)
            continue;
          String id = "D_" + (j + 1) + axis + ";" + atom.atomName;
          pt = P3.new3(csin, ccos, 0);
          addModulation(null, id, pt);
        }
      }
    }
    r.close();
  }

  private String readM40Line(BufferedReader r) throws Exception {
    line = r.readLine();
    line = TextFormat.simpleReplace(line, "-", " -");
    tokens = getTokens();
    return line;
  }

  private int qicount;

  private void qi() {
    P3 pt = P3.new3(parseFloat(), parseFloat(), parseFloat());
    if (qicount == 0)
      addModulation(null, "W_1", pt);
    addModulation(null, "F_" + (++qicount), pt);
  }
 
  private void ndim() {
    setModDim(line.substring(line.length() - 1));
  }

  private void lattvec() throws Exception {
    addLatticeVector(line.substring(8));
  }

  private void symmetry() throws Exception {
    setSymmetryOperator(TextFormat.simpleReplace(line.substring(9).trim()," ", ","));
  }

  private void cell() throws Exception {
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseFloat());
  }


}
