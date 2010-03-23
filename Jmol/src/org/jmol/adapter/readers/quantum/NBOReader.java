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

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * NBO file 31 reader will pull in other files as necessary
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
  
  protected void initializeReader() throws Exception {
    String line1 = readLine();
    readLine();
    isOutputFile = (line.indexOf("***") >= 0);
    if (isOutputFile) {
      readFile31();
    } else {
      readData31(line1, line);
    }
    super.initializeReader();
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
  
  private void readData31(String line1, String line2) throws Exception {
    if (line1 == null)
      line1 = readLine();
    if (line2 == null)
      line2 = readLine();
    atomSetCollection.setAtomSetName(line1.trim());
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
  }

  private void readFile31() throws Exception {
    String fileName = (String) htParams.get("fullPathName");
    int pt = fileName.lastIndexOf(".");
    if (pt < 0)
      pt = fileName.length();
    fileName = fileName.substring(0, pt) + ".31";
    String data = viewer.getFileAsString(fileName);
    if (data.length() == 0) {
      Logger.error(" supplemental file " + fileName + " was not found");
      continuing = false;
      return;
    }
    BufferedReader readerSave = reader;
    reader = new BufferedReader(new StringReader(data));
    readData31(null, null);
    reader = readerSave;
  }

  protected boolean checkLine() throws Exception {
    return checkNboLine();
  }
  

}
