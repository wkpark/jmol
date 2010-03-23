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
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Hashtable;

/**
 * NBO file nn reader will pull in other files as necessary
 *
 *
 **/

 /* NBO output analysis is based on
  * 
 *********************************** NBO 5.G ***********************************
             N A T U R A L   A T O M I C   O R B I T A L   A N D
          N A T U R A L   B O N D   O R B I T A L   A N A L Y S I S
 *******************************************************************************
  (c) Copyright 1996-2004 Board of Regents of the University of Wisconsin System
      on behalf of the Theoretical Chemistry Institute.  All Rights Reserved.

          Cite this program as:

          NBO 5.G.  E. D. Glendening, J. K. Badenhoop, A. E. Reed,
          J. E. Carpenter, J. A. Bohmann, C. M. Morales, and F. Weinhold
          (Theoretical Chemistry Institute, University of Wisconsin,
          Madison, WI, 2001); http://www.chem.wisc.edu/~nbo5

       /AONBO  / : Print the AO to NBO transformation
  * 
  */
public class NBOReader extends MOReader {
    
  private boolean isOutputFile;
  private String moType = "";
  private int nCoef;
  private int nOrbitals;
  
  protected void initializeReader() throws Exception {
    String line1 = readLine();
    readLine();
    isOutputFile = (line.indexOf("***") >= 0);
    if (isOutputFile) {
      readFile31();
      // keep going -- we need to read the file using MOReader.
    } else if (line.contains("s in the AO basis:")){
      moType = line.substring(0, line.indexOf("s") + 1);
      readFile31();
      readFile46();
      readOrbitalData();
      continuing = false;
    } else {
      readData31(line1, line);
      continuing = false;
    }
    super.initializeReader();
  }

  protected boolean checkLine() throws Exception {
    // for .nbo only
    return checkNboLine();
  }
  
  
  /*
  14_a                                                                          
 Basis set information needed for plotting orbitals
 ---------------------------------------------------------------------------
     36    90   162
 ---------------------------------------------------------------------------
    6  -2.992884000  -1.750577000   1.960024000
    6  -2.378528000  -1.339374000   0.620578000
   */
  
  private void readFile31() throws Exception {
    String data = getFileData(".31");
    if (data == null)
      return;
    BufferedReader readerSave = reader;
    reader = new BufferedReader(new StringReader(data));
    readData31(null, null);
    reader = readerSave;
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

  private void readData46() throws Exception {
    String[] tokens = getTokens(readLine()); 
    nCoef = parseInt(tokens[1]);
    discardLinesUntilContains("-");
    StringBuffer sb = new StringBuffer();
    do {
      sb.append(line);
    } while (readLine() != null);
    String data = sb.toString();
    int n = data.length();
    sb = new StringBuffer();
    for (int i = 0; i < n; i++) {
      char c = data.charAt(i);
      switch (c) {
      case '-':
        if (data.charAt(i + 1) == ' ')
          i++;
        break;
      case ' ':
        if (Character.isDigit(data.charAt(i + 1)))
          continue;
        break;        
      }
      sb.append(c);
    }
    tokens = getTokens(sb.toString());
    for (int i = 0; i < tokens.length; i++) {
      Hashtable mo = new Hashtable();
      String type = tokens[i];
      mo.put("type", type);
      //TODO: does not account for SOMO
      mo.put("occupancy", new Float(type.indexOf("*") >= 0 ? 0 : 2));
      orbitals.add(mo);
    }
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

  private void readData31(String line1, String line2) throws Exception {
    if (line1 == null)
      line1 = readLine();
    if (line2 == null)
      line2 = readLine();
    atomSetCollection.setAtomSetName(line1.trim() + moType);
    readLine();  // ----------
    String[] tokens = getTokens(readLine());
    int atomCount = parseInt(tokens[0]);
    readLine();  // ----------
    atomSetCollection.newAtomSet();
    for (int i = 0; i < atomCount; i++) {
      tokens = getTokens(readLine());
      Atom atom = atomSetCollection.addNewAtom();
      atom.elementNumber = (short) parseInt(tokens[0]);
      atom.set(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
    }    
    readBasisFunctions();
  }

  private void readBasisFunctions() {
    // TODO
    
  }

  private void readOrbitalData() throws Exception {
    readLine();
    for (int i = 0; i < nOrbitals; i++) {
      Hashtable mo = (Hashtable) orbitals.get(i);
      float[] coefs = new float[nCoef];
      mo.put("ccoefficients", coefs);
      for (int j = 0; j < nCoef;) {
        String[] data = getTokens(readLine());
        for (int k = 0; k < data.length; k++, j++)
          coefs[j] = parseFloat(data[k]);
      }
      orbitals.add(mo);
    }
  }

}
