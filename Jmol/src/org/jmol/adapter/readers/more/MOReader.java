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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;
import org.jmol.api.JmolAdapter;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;


import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * General methods for reading molecular orbital data,
 * including embedded output from the NBO program.
 * In particular, when the AONBO keyword is included.
 *
 *
 * requires the following sort of construct:
 * 
  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    readAtomSetCollection(reader, "some type");
  }
  
  protected boolean checkLine() {
    if (line.indexOf(...)) {
      doThis();
      return true/false;
    } 
    if (line.indexOf(...)) {
      doThat();
      return true/false;
    } 
    return checkNboLine();
  }
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
abstract class MOReader extends AtomSetCollectionReader {
    
  protected int shellCount = 0;
  protected int gaussianCount = 0;
  protected Hashtable moData = new Hashtable();
  protected Vector shells;
  protected float[][] gaussians;

  protected Vector orbitals = new Vector();
  protected String energyUnits = "";
  
  protected Vector moTypes;
  private boolean getNBOs;
  private boolean getNBOCharges;
  protected boolean haveNboCharges;

  private String[] filterTokens;
  private boolean filterIsNot; 

  protected boolean iHaveAtoms = false;
  protected boolean continuing = true;
  protected boolean ignoreMOs = false;
  protected String alphaBeta = "";

  final protected int HEADER_GAMESS_UK_MO = 3;
  final protected int HEADER_GAMESS_OCCUPANCIES = 2;
  final protected int HEADER_GAMESS_ORIGINAL = 1;
  final protected int HEADER_NONE = 0;
  

  abstract public void readAtomSetCollection(BufferedReader reader); 

  /**
   * @return true if need to read new line
   * @throws Exception 
   * 
   */
  abstract protected boolean checkLine() throws Exception;
  
  public void readAtomSetCollection(BufferedReader reader, String type) {
    initializeMoReader(reader, type);
    try {
      readLine();
      iHaveAtoms = false;
      while (line != null && continuing)
        if (checkLine())
          readLine();
      finalizeMoReader();
    } catch (Exception e) {
      setError(e);
    }
  }
  
  protected void finalizeMoReader() {
    // see subclasses
  }

  private void initializeMoReader(BufferedReader reader, String type) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("type");
    line = "\nNBOs in the AO basis:";
    getNBOs = filterMO();
    line = "\nNBOcharges";
    getNBOCharges = (filter != null && filterMO());
    if (filter == null)
      return;
    filter = TextFormat.simpleReplace(filter, "nbocharges","");
    if (filter.length() < 3)
      filter = null;
  }
  
  protected boolean filterMO() {
    if (filter == null)
      return true;
    boolean isOK = true;
    int nOK = 0;
    line = line.toLowerCase() + " " + alphaBeta;
    if (filterTokens == null) {
      filterIsNot = (filter.indexOf("!") >= 0);
      filterTokens = getTokens(filter.replace('!', ' ').replace(',', ' ')
          .replace(';', ' ').toLowerCase());
    }
    for (int i = 0; i < filterTokens.length; i++)
      if (line.indexOf(filterTokens[i]) >= 0) {
        if (!filterIsNot) {
          nOK = filterTokens.length;
          break;
        }
      } else if (filterIsNot) {
        nOK++;
      }
    isOK = (nOK == filterTokens.length);
    if (line.indexOf('\n') != 0)
      Logger.info("filter MOs: " + isOK + " for \"" + line + "\"");
    return isOK;
  }

  /**
   * 
   * @return true if need to read line
   * @throws Exception
   */
  protected boolean checkNboLine() throws Exception {

    // these output lines are being passed on by NBO 5.0 to whatever program is
    // using it (GAMESS, GAUSSIAN)

    if (getNBOs) {
      if (line.indexOf("(Occupancy)   Bond orbital/ Coefficients/ Hybrids") >= 0) {
        getNboTypes();
        return false;
      }
      if (line.indexOf("NBOs in the AO basis:") >= 0) {
        readMolecularOrbitals(HEADER_NONE);
        return false;
      }
    }
    if (getNBOCharges && line.indexOf("Summary of Natural Population Analysis:") >= 0) {
      getNboCharges();
      return true;
    }
    return true;
  }
  
  /*
 Summary of Natural Population Analysis:

                                     Natural Population                 Natural
             Natural    ---------------------------------------------    Spin
  Atom No    Charge        Core      Valence    Rydberg      Total      Density
 ------------------------------------------------------------------------------
    O  1   -0.25759      1.99984     6.23673    0.02102     8.25759     0.28689
    N  2    0.51518      1.99975     4.42031    0.06476     6.48482     0.42622
    O  3   -0.25759      1.99984     6.23673    0.02102     8.25759     0.28689
 ===============================================================================

   */
  
  private void getNboCharges() throws Exception {
    if (haveNboCharges)
      return; // don't use alpha/beta spin charges
    discardLinesUntilContains("----");
    discardLinesUntilContains("----");
    haveNboCharges = true;
    int atomCount = atomSetCollection.getAtomCount();
    int i0 = atomSetCollection.getLastAtomSetAtomIndex();
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = i0; i < atomCount; ++i) {
      // first skip over the dummy atoms
      while (atoms[i].elementNumber == 0)
        ++i;
      // assign the partial charge
      String[] tokens = getTokens(readLine());
      float charge;
      if (tokens == null || tokens.length < 3 || Float.isNaN(charge = parseFloat(tokens[2]))) {
        Logger.info("Error reading NBO charges: " + line);
        return;
      }
      atoms[i].partialCharge = charge;      
      if (Logger.debugging)
        Logger.debug("Atom " + i + " using NBOcharge: " + charge);
    }
    Logger.info("Using NBO charges for Model " + atomSetCollection.getAtomSetCount());
  }
  
  
  /*
   * 
     (Occupancy)   Bond orbital/ Coefficients/ Hybrids
 -------------------------------------------------------------------------------
   1. (1.98839) BD ( 1) C  1- C  2       
               ( 51.09%)   0.7148* C  1 s( 26.14%)p 2.82( 73.68%)d 0.01(  0.18%)
                                        -0.0003 -0.5112 -0.0085  0.0025  0.8344
                                         0.0173 -0.1824 -0.0055 -0.0841  0.0006
                                         0.0133  0.0063 -0.0009 -0.0349  0.0180
               ( 48.91%)   0.6994* C  2 s( 38.95%)p 1.56( 60.89%)d 0.00(  0.16%)
                                         0.0002 -0.6233  0.0310 -0.0067 -0.7603
                                         0.0081  0.1752 -0.0039 -0.0028  0.0014
                                         0.0235  0.0042  0.0002 -0.0285  0.0140

   */
  protected void getNboTypes() throws Exception {
    moTypes = new Vector();
    readLine();
    readLine();
    int n = 0;
    while (line != null && line.indexOf(".") == 4) {
      if (parseInt(line.substring(0, 4)) != n + 1)
        break;
      moTypes.add(n++, line.substring(5, 34).trim());
      while (readLine() != null && line.startsWith("     ")) {
      }
    }
    Logger.info(n + " natural bond orbitals read");
  }

  /*
   * 
   * GAMESS:
   * 
   ------------------
   MOLECULAR ORBITALS
   ------------------

          ------------
          EIGENVECTORS
          ------------

                      1          2          3          4          5
                  -79.9156   -20.4669   -20.4579   -20.4496   -20.4419
                     A          A          A          A          A   
    1  C  1  S   -0.000003  -0.000029  -0.000004   0.000011   0.000016
    2  C  1  S   -0.000009   0.000140   0.000001   0.000057   0.000065
    3  C  1  X    0.000007  -0.000241  -0.000022  -0.000010  -0.000061
    4  C  1  Y   -0.000008   0.000017  -0.000027  -0.000010   0.000024
    5  C  1  Z    0.000007   0.000313   0.000009  -0.000002  -0.000001
    6  C  1  S    0.000049   0.000875  -0.000164  -0.000521  -0.000440
    7  C  1  X   -0.000066   0.000161   0.000125   0.000034   0.000406
    8  C  1  Y    0.000042   0.000195  -0.000165  -0.000254  -0.000573
    9  C  1  Z    0.000003   0.000045   0.000052   0.000112  -0.000129
   10  C  1 XX   -0.000010   0.000010  -0.000040   0.000019   0.000045
   11  C  1 YY   -0.000010  -0.000031   0.000000  -0.000003   0.000019
...

                      6          7          8          9         10
                  -20.4354   -20.4324   -20.3459   -20.3360   -11.2242
                     A          A          A          A          A   
    1  C  1  S    0.000000  -0.000001   0.000001   0.000000   0.008876
    2  C  1  S   -0.000003   0.000002   0.000003   0.000002   0.000370

...
 TOTAL NUMBER OF BASIS SET SHELLS             =  101
   *
   * NBO: --- note, "-" can be in column with " " causing tokenization failure
   * 
          AO         1       2       3       4       5       6       7       8
      ---------- ------- ------- ------- ------- ------- ------- ------- -------
   1.  C 1 (s)    0.0364  0.0407 -0.0424  0.0428  0.0056 -0.0009  0.0052  0.0018
   2.  C 1 (s)   -0.1978 -0.1875  0.1959 -0.1992 -0.0159  0.0054 -0.0130  0.0084

   */
  protected void readMolecularOrbitals(int headerType) throws Exception {
    if (ignoreMOs) {
      // but for now can override this with FILTER=" LOCALIZED ORBITALS"
      //should read alpha and beta
      readLine();
      return;
    }
    Hashtable[] mos = null;
    Vector[] data = null;
    Vector coeffLabels = null;
    int ptOffset = -1;
    int fieldSize = 0;
    int nThisLine = 0;
    readLine();
    int moCount = 0;
    int nSkip = -1;
    if (line.indexOf("---") >= 0)
      readLine();
    while (readLine() != null) {
      String[] tokens = getTokens();
      if (Logger.debugging) {
        Logger.debug(tokens.length + " --- " + line);
      }
      if (line.indexOf("end") >= 0)
        break;
      if (line.indexOf(" ALPHA SET ") >= 0) {
        alphaBeta = "alpha";
        if (readLine() == null)
          break;
      } else if (line.indexOf(" BETA SET ") >= 0) {
        alphaBeta = "beta";
        if (readLine() == null)
          break;
      }
        //not everyone has followed the conventions for ending a section of output
      if (line.length() == 0 || line.indexOf("--") >= 0 || line.indexOf(".....") >=0 
           || line.indexOf("NBO BASIS") >= 0 // reading NBOs
           || line.indexOf("CI EIGENVECTORS WILL BE LABELED") >=0 //this happens when doing MCSCF optimizations
           || line.indexOf("   THIS LOCALIZATION HAD") >=0) { //this happens with certain localization methods
        for (int iMo = 0; iMo < nThisLine; iMo++) {
          float[] coefs = new float[data[iMo].size()];
          int iCoeff = 0;
          while (iCoeff < coefs.length) {
            // Reorder F coeffs; leave the rest untouched
            if (((String) coeffLabels.get(iCoeff)).equals("XXX")) {
              Hashtable fCoeffs = new Hashtable();
              for (int ifc = 0; ifc < 10; ifc++) {
                fCoeffs.put(coeffLabels.get(iCoeff+ifc), data[iMo].get(iCoeff+ifc));
              }
              for (int ifc = 0; ifc < 10; ifc++) {
                String orderLabel = JmolAdapter.getQuantumSubshellTag(JmolAdapter.SHELL_F_CARTESIAN, ifc);
                coefs[iCoeff++] = parseFloat((String) fCoeffs.get(orderLabel));
              }
            } else {
              coefs[iCoeff] = parseFloat((String) data[iMo].get(iCoeff));
              iCoeff++;
            }
          }
          mos[iMo].put("coefficients", coefs);
          if (alphaBeta.length() > 0)
            mos[iMo].put("type", alphaBeta);
          else if (moTypes != null && moCount < moTypes.size())
            mos[iMo].put("type", moTypes.get(moCount++));
          orbitals.addElement(mos[iMo]);
        }
        nThisLine = 0;
        if (line.length() == 0)
          continue;
        break;
      }
      //read the data line:
      if (nThisLine == 0) {
        nThisLine = tokens.length;
        if (tokens[0].equals("AO")) {
          //01234567890123456789
          // 480. Li31 (s)   -7.3005  1.8135 -9.4655 -0.5137 -5.1614-23.4537-20.3894-37.6613
          nThisLine--;
          ptOffset = 16;
          fieldSize = 8;
          nSkip = 3;
            // NBOs
        }
        if (mos == null || nThisLine > mos.length) {
           mos = new Hashtable[nThisLine];
           data = new Vector[nThisLine];
        }
        for (int i = 0; i < nThisLine; i++) {
          mos[i] = new Hashtable();
          data[i] = new Vector();
        }
        getMOHeader(headerType, tokens, mos, nThisLine);
        coeffLabels = new Vector();
        continue;
      }
      if (ptOffset < 0) {
        nSkip = tokens.length - nThisLine;
        for (int i = 0; i < nThisLine; i++)
          data[i].addElement(tokens[i + nSkip]);
      } else {
        int pt = ptOffset;
        for (int i = 0; i < nThisLine; i++, pt += fieldSize)
          data[i].addElement(line.substring(pt, pt + fieldSize).trim());
      }
      coeffLabels.addElement(JmolAdapter.canonicalizeQuantumSubshellTag(tokens[nSkip - 1].toUpperCase()));
      
      line = "";
    }
    energyUnits = "a.u.";
    setMOData(!alphaBeta.equals("alpha"));    
  }

  protected void getMOHeader(int headerType, String[] tokens, Hashtable[] mos, int nThisLine)
      throws Exception {
    readLine();
    switch (headerType) {
    default:
    case HEADER_NONE:
      // this means there are no energies, occupancies or symmetries
      for (int i = 0; i < nThisLine; i++) {
        mos[i].put("energy", "");
      }
      return;
    case HEADER_GAMESS_UK_MO:
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("energy", new Float(tokens[i]));
      discardLines(5);
      return;
    case HEADER_GAMESS_ORIGINAL:
      // this is the original functionality
      tokens = getTokens();
      if (tokens.length == 0)
        tokens = getTokens(readLine());
      for (int i = 0; i < nThisLine; i++) {
        mos[i].put("energy", new Float(tokens[i]));
      }
      readLine();
      break;
    case HEADER_GAMESS_OCCUPANCIES:
      // MCSCF NATURAL ORBITALS only have occupancy
      boolean haveSymmetry = (line.length() > 0 || readLine() != null);
      tokens = getTokens();
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("occupancy", new Float(tokens[i].charAt(0) == '-' ? 2.0f
            : parseFloat(tokens[i])));
      readLine(); // blank or symmetry
      if (!haveSymmetry)
        return;
      // MCSCF NATURAL ORBITALS (from GUGA) using CSF configurations have
      // occupancy and symmetry
    }
    if (line.length() > 0) {
      tokens = getTokens();
      for (int i = 0; i < nThisLine; i++)
        mos[i].put("symmetry", tokens[i]);
    }
  }

  protected void addMOData(int nColumns, Vector[] data, Hashtable[] mos) {
    for (int i = 0; i < nColumns; i++) {
      float[] coefs = new float[data[i].size()];
      for (int j = coefs.length; --j >= 0;)
        coefs[j] = parseFloat((String) data[i].get(j));
      mos[i].put("coefficients", coefs);
      orbitals.addElement(mos[i]);
    }
  }

  protected void setMOData(boolean clearOrbitals) {
    moData.put("calculationType", calculationType);
    moData.put("energyUnits", energyUnits);
    moData.put("shells", shells);
    moData.put("gaussians", gaussians);
    moData.put("mos", orbitals);
    setMOData(moData);
    if (clearOrbitals) {
      orbitals = new Vector();
      moData = new Hashtable();
      alphaBeta = "";
    }
  }
  
}
