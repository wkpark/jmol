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

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * NBO file nn reader will pull in other files as necessary
 * 
 * 
 **/

/*
 * NBO output analysis is based on
 * 
 * ********************************** NBO 5.G
 * *********************************** N A T U R A L A T O M I C O R B I T A L A
 * N D N A T U R A L B O N D O R B I T A L A N A L Y S I S
 * ***********************
 * ******************************************************* (c) Copyright
 * 1996-2004 Board of Regents of the University of Wisconsin System on behalf of
 * the Theoretical Chemistry Institute. All Rights Reserved.
 * 
 * Cite this program as:
 * 
 * NBO 5.G. E. D. Glendening, J. K. Badenhoop, A. E. Reed, J. E. Carpenter, J.
 * A. Bohmann, C. M. Morales, and F. Weinhold (Theoretical Chemistry Institute,
 * University of Wisconsin, Madison, WI, 2001); http://www.chem.wisc.edu/~nbo5
 * 
 * /AONBO / : Print the AO to NBO transformation
 */
public class GenNBOReader extends MOReader {

  private boolean isOutputFile;
  private String moType = "";
  private int nOrbitals;

  /*
   * molname.31 AO molname.32 PNAO molname.33 NAO molname.34 PNHO molname.35 NHO
   * molname.36 PNBO molname.37 NBO molname.38 PNLMO molname.39 NLMO molname.40
   * MO molname.41 AO density matrix molname.46 Basis label file
   */
  protected void initializeReader() throws Exception {
    String line1 = readLine();
    readLine();
    isOutputFile = (line.indexOf("***") >= 0);
    boolean isOK;
    if (isOutputFile) {
      isOK = readFile31();
      super.initializeReader();
      // keep going -- we need to read the file using MOReader.
    } else if (line.contains("s in the AO basis:")) {
      moType = line.substring(1, line.indexOf("s"));
      isOK = readFile31();
    } else {
      moType = "AO";
      isOK = readData31(line1, line);
    }
    if (!isOK)
      Logger.error("Unimplemented shell type -- no orbitals avaliable");
    if (isOutputFile) 
      return;
    if (isOK) {
      readFile46();
      readOrbitalData(!moType.equals("AO"));
      setMOData(false);
      moData.put("isNormalized", Boolean.TRUE);
    }
    continuing = false;
  }

  protected boolean checkLine() throws Exception {
    // for .nbo only
    return checkNboLine();
  }

  private String getFileData(String ext) {
    String fileName = (String) htParams.get("fullPathName");
    int pt = fileName.lastIndexOf(".");
    if (pt < 0)
      pt = fileName.length();
    fileName = fileName.substring(0, pt) + ext;
    String data = viewer.getFileAsString(fileName);
    if (data.length() == 0) {
      Logger.error(" supplemental file " + fileName + " was not found");
      continuing = false;
      return null;
    }
    return data;
  }

  /*
   * 14_a Basis set information needed for plotting orbitals
   * ---------------------------------------------------------------------------
   * 36 90 162
   * ---------------------------------------------------------------------------
   * 6 -2.992884000 -1.750577000 1.960024000 6 -2.378528000 -1.339374000
   * 0.620578000
   */

  private boolean readFile31() throws Exception {
    String data = getFileData(".31");
    if (data == null)
      return false;
    BufferedReader readerSave = reader;
    reader = new BufferedReader(new StringReader(data));
    if (!readData31(null, null))
      return false;
    reader = readerSave;
    return true;
  }

  private void readFile46() throws Exception {
    String data = getFileData(".46");
    if (data == null)
      return;
    BufferedReader readerSave = reader;
    reader = new BufferedReader(new StringReader(data));
    readData46();
    reader = readerSave;
  }

  private boolean readData31(String line1, String line2) throws Exception {
    if (line1 == null)
      line1 = readLine();
    if (line2 == null)
      line2 = readLine();
    atomSetCollection.setAtomSetName(line1.trim() + moType);

    // read atomCount, shellCount, and gaussianCount
    readLine(); // ----------
    String[] tokens = getTokens(readLine());
    int atomCount = parseInt(tokens[0]);
    shellCount = parseInt(tokens[1]);
    gaussianCount = parseInt(tokens[2]);

    // read atom types and positions
    readLine(); // ----------
    atomSetCollection.newAtomSet();
    for (int i = 0; i < atomCount; i++) {
      tokens = getTokens(readLine());
      int z = parseInt(tokens[0]);
      if (z < 0) // dummy atom
        continue;
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (short) z;
      atom.set(parseFloat(tokens[1]), parseFloat(tokens[2]),
          parseFloat(tokens[3]));
    }

    // read basis functions
    shells = new Vector();
    gaussians = new float[gaussianCount][];
    for (int i = 0; i < gaussianCount; i++)
      gaussians[i] = new float[5];
    readLine(); // ----------
    nOrbitals = 0;
    for (int i = 0; i < shellCount; i++) {
      tokens = getTokens(readLine());
      readLine(); // skip second line?
      int[] slater = new int[4];
      slater[0] = parseInt(tokens[0]) - 1; // atom pointer; 1-based
      int n = parseInt(tokens[1]);
      nOrbitals += n;
      switch (n) {
      case 1:
        slater[1] = JmolAdapter.SHELL_S;
        break;
      case 3:
        slater[1] = JmolAdapter.SHELL_P;
        break;
      case 4:
        slater[1] = JmolAdapter.SHELL_SP;
        break;
      case 5:
        // TODO order?
        // GenNBO is 251 252 253 254 255 for Dxy Dxz Dyz Dx2-y2 D2z2-x2-y2
        // org.jmol.quantum.MOCalculation expects d2z^2-x2-y2, dxz, dyz, dx^2-y^2, dxy
        slater[1] = JmolAdapter.SHELL_D_SPHERICAL;
        return false;
      case 6:
        // TODO order?
        // GenNBO is 201 202 203 204 205 206 for Dxx Dxy Dxz Dyy Dyz Dzz
        // org.jmol.quantum.MOCalculation expects Dxx Dyy Dzz Dxy Dxz Dyz
        slater[1] = JmolAdapter.SHELL_D_CARTESIAN;
        return false;
      case 7:
        // TODO order?
        // GenNBO is 351 352 353 354 355 356 357
        //        as 2z3-3x2z-3y2z
        //               4xz2-x3-xy2
        //                   4yz2-x2y-y3
        //                           x2z-y2z
        //                               xyz+x3-3xy2
        //                                   3x2y-y3
        slater[1] = JmolAdapter.SHELL_F_SPHERICAL;
        return false;
      case 10:
        // TODO order?
        // GenNBO is 301 302 303 304 305 306 307 308 309 310
        //       for xxx xxy xxz xyy xyz xzz yyy yyz yzz zzz
        // org.jmol.quantum.MOCalculation expects
        //           xxx yyy zzz xyy xxy xxz xzz yzz yyz xyz
        slater[1] = JmolAdapter.SHELL_F_CARTESIAN;
        return false;
      }
      // 0 = S, 1 = P, 2 = SP, 3 = D, 4 = F
      slater[2] = parseInt(tokens[2]) - 1; // gaussian list pointer
      slater[3] = parseInt(tokens[3]);     // number of gaussians
      shells.addElement(slater);
    }

    for (int i = 0; i < nOrbitals; i++) {
      Hashtable mo = new Hashtable();
      orbitals.add(mo);
    }

    // get alphas and exponents

    for (int j = 0; j < 5; j++) {
      readLine();
      float[] temp = new float[gaussianCount];
      fillFloatArray(temp);
      for (int i = 0; i < gaussianCount; i++)
        gaussians[i][j] = temp[i];
    }
    if (Logger.debugging) {
      Logger.debug(shells.size() + " slater shells read");
      Logger.debug(gaussians.length + " gaussian primitives read");
    }
    return true;
  }

  private boolean readData46() throws Exception {
    String[] tokens = getTokens(readLine());
    if (parseInt(tokens[1]) != nOrbitals) {
      Logger.error("file 46 number of orbitals does not match nOrbitals: " + nOrbitals);
      return false;
    }
    String ntype = null;
    if (moType.indexOf("NHO") >= 0)
      ntype = "NHO";
    else if (moType.indexOf("NBO") >= 0)
      ntype = "NBO";
    else if (moType.indexOf("NAO") >= 0)
      ntype = "NAO";
    else if (moType.equals("AO"))
      ntype = "AO";
    if (ntype == null) {
      Logger.error("uninterpretable type " + moType);
      return false;
    }
    if (!ntype.equals("AO"))
      discardLinesUntilContains(ntype);
    StringBuffer sb = new StringBuffer();
    while (readLine() != null && line.indexOf("O    ") < 0)
      sb.append(line);
    sb.append(' ');
    String data = sb.toString();
    int n = data.length() - 1;
    sb = new StringBuffer();
    for (int i = 0; i < n; i++) {
      char c = data.charAt(i);
      switch (c) {
      case '(':
      case '-':
        if (data.charAt(i + 1) == ' ')
          i++;
        break;
      case ' ':
        if (Character.isDigit(data.charAt(i + 1)) || data.charAt(i + 1) == '(')
          continue;
        break;
      }
      sb.append(c);
    }
    Logger.info(sb.toString());
    tokens = getTokens(sb.toString());
    for (int i = 0; i < nOrbitals; i++) {
      Hashtable mo = (Hashtable) orbitals.get(i);
      String type = tokens[i];
      mo.put("type", moType + " " + type);
      // TODO: does not account for SOMO
      mo.put("occupancy", new Float(type.indexOf("*") >= 0 ? 0 : 2));
    }
    return true;
  }

  private void readOrbitalData(boolean isMO) throws Exception {
    if (isMO)
      readLine();
    for (int i = 0; i < nOrbitals; i++) {
      Hashtable mo = (Hashtable) orbitals.get(i);
      float[] coefs = new float[nOrbitals];
      mo.put("coefficients", coefs);
      if (isMO)
        for (int j = 0; j < nOrbitals;) {
          String[] data = getTokens(readLine());
          for (int k = 0; k < data.length; k++, j++)
            coefs[j] = parseFloat(data[k]);
        }
      else
        coefs[i] = 1;
    }
  }

}
