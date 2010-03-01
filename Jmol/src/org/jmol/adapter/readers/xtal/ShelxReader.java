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

import org.jmol.adapter.smarter.*;

import org.jmol.util.ArrayUtil;

/**
 * A reader for SHELX output (RES) files. It does not read all information.
 * The list of fields that is read: TITL, REM, END, CELL, SPGR, SFAC
 * In addition atoms are read.
 *
 * <p>A reader for SHELX files. It currently supports SHELXL.
 *
 * <p>The SHELX format is described on the net:
 * <a href="http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html">
 * http://www.msg.ucsf.edu/local/programs/shelxl/ch_07.html</a>.
 *
 * modified by Bob Hanson 2006/04 to allow 
 * variant CrystalMaker .cmdf file reading.
 * 
 *  
 */

public class ShelxReader extends AtomSetCollectionReader {

  private String[] sfacElementSymbols;
  private boolean isCmdf = false;

  public void initializeReader() {
      setFractionalCoordinates(true);
  }
  
  protected boolean checkLine() throws Exception {

    int lineLength ;
    // '=' as last char of line means continue on next line
    while ((lineLength = (line = line.trim()).length()) > 0 
        && line.charAt(lineLength - 1) == '=') 
      line = line.substring(0, lineLength - 1) + readLine();
    if (line.startsWith("TITL")) {
      if (!doGetModel(++modelNumber))
        return checkLastModel();
      sfacElementSymbols = null;
      applySymmetryAndSetTrajectory();
      setFractionalCoordinates(true);
      isCmdf = false;
      atomSetCollection.newAtomSet();
      atomSetCollection.setAtomSetName(parseTrimmed(line, 4));
      iHaveAtoms = true;
      return true;
    }

    if (!iHaveAtoms || lineLength < 3)
      return true;

    String command = (line + " ").substring(0, 4).toUpperCase().trim();
    if (unsupportedRecordTypes.indexOf(command + ";") >= 0)
      return true;
    for (int i = supportedRecordTypes.length; --i >= 0;)
      if (command.equals(supportedRecordTypes[i])) {
        processSupportedRecord(i);
        return true;
      }
    if (!isCmdf)
      assumeAtomRecord();
    return true;
  }

  private final static String unsupportedRecordTypes = 
    "ZERR;DISP;UNIT;LAUE;REM;MORE;TIME;" +
    "HKLF;OMIT;SHEL;BASF;TWIN;EXTI;SWAT;HOPE;MERG;" +
    "SPEC;RESI;MOVE;ANIS;AFIX;HFIX;FRAG;FEND;EXYZ;" +
    "EXTI;EADP;EQIV;" +
    "CONN;PART;BIND;FREE;" +
    "DFIX;DANG;BUMP;SAME;SADI;CHIV;FLAT;DELU;SIMU;" +
    "DEFS;ISOR;NCSY;SUMP;" +
    "L.S.;CGLS;BLOC;DAMP;STIR;WGHT;FVAR;" +
    "BOND;CONF;MPLA;RTAB;HTAB;LIST;ACTA;SIZE;TEMP;" +
    "WPDB;" +
    "FMAP;GRID;PLAN;MOLE";
  
  final private static String[] supportedRecordTypes = { "TITL", "CELL", "SPGR",
      "SFAC", "LATT", "SYMM", "NOTE", "ATOM", "END" };

  private void processSupportedRecord(int recordIndex) throws Exception {
    //Logger.debug(recordIndex+" "+line);
    switch (recordIndex) {
    case 0: // TITL
    case 8: // END
      break;
    case 1: // CELL
      cell();
      setSymmetryOperator("x,y,z");
      break;
    case 2: // SPGR
      setSpaceGroupName(parseTrimmed(line, 4));
      break;
    case 3: // SFAC
      parseSfacRecord();
      break;
    case 4: // LATT
      parseLattRecord();
      break;
    case 5: // SYMM
      parseSymmRecord();
      break;
    case 6: // NOTE
      isCmdf = true;
      break;
    case 7: // ATOM
      if (isCmdf)
        processCmdfAtoms();
      break;
    }
  }

  private void parseLattRecord() throws Exception {
    parseToken(line);
    int latt = parseInt();
    atomSetCollection.setLatticeParameter(latt);
  }

  private void parseSymmRecord() throws Exception {
    setSymmetryOperator(parseTrimmed(line, 4));
  }

  private void cell() throws Exception {
    /* example:
     * CELL   wavelngth    a        b         c       alpha   beta   gamma
     * CELL   1.54184   7.11174  21.71704  30.95857  90.000  90.000  90.000
     * 
     * or CrystalMaker file:
     * 
     * CELL       a        b         c       alpha   beta   gamma
     * CELL   7.11174  21.71704  30.95857  90.000  90.000  90.000
     */

    String[] tokens = getTokens();
    int ioff = 1;
    if (isCmdf) {
      ioff = 0;
    } else {
      float wavelength = parseFloat(tokens[0]);
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("wavelength",
          new Float(wavelength));
    }
    for (int ipt = 0; ipt < 6; ipt++)
      setUnitCellItem(ipt, parseFloat(tokens[ipt + ioff + 1]));
  }

  private void parseSfacRecord() {
    // an SFAC record is one of two cases
    // a simple SFAC record contains element names
    // a general SFAC record contains coefficients for a single element
    String[] sfacTokens = getTokens(line, 4);
    boolean allElementSymbols = true;
    for (int i = sfacTokens.length; allElementSymbols && --i >= 0;) {
      String token = sfacTokens[i];
      allElementSymbols = Atom.isValidElementSymbolNoCaseSecondChar(token);
    }
    if (allElementSymbols)
      parseSfacElementSymbols(sfacTokens);
    else
      parseSfacCoefficients(sfacTokens);
  }

  private void parseSfacElementSymbols(String[] sfacTokens) {
    if (sfacElementSymbols == null) {
      sfacElementSymbols = sfacTokens;
    } else {
      int oldCount = sfacElementSymbols.length;
      int tokenCount = sfacTokens.length;
      sfacElementSymbols = ArrayUtil.setLength(sfacElementSymbols, oldCount + tokenCount);
      for (int i = tokenCount; --i >= 0;)
        sfacElementSymbols[oldCount + i] = sfacTokens[i];
    }
  }
  
  private void parseSfacCoefficients(String[] sfacTokens) {
    float a1 = parseFloat(sfacTokens[1]);
    float a2 = parseFloat(sfacTokens[3]);
    float a3 = parseFloat(sfacTokens[5]);
    float a4 = parseFloat(sfacTokens[7]);
    float c = parseFloat(sfacTokens[9]);
    // element # is these floats rounded to nearest int
    int z = (int) (a1 + a2 + a3 + a4 + c + 0.5f);
    String elementSymbol = getElementSymbol(z);
    int oldCount = 0;
    if (sfacElementSymbols == null) {
      sfacElementSymbols = new String[1];
    } else {
      oldCount = sfacElementSymbols.length;
      sfacElementSymbols = ArrayUtil.setLength(sfacElementSymbols, oldCount + 1);
      sfacElementSymbols[oldCount] = elementSymbol;
    }
    sfacElementSymbols[oldCount] = elementSymbol;
  }

  private void assumeAtomRecord() throws Exception {
    // this line gives an atom, because any line not starting with
    // a SHELX command is an atom
    String atomName = parseToken(line);
    int scatterFactor = parseInt();
    float x = parseFloat();
    float y = parseFloat();
    float z = parseFloat();
    // skip the rest

    Atom atom = atomSetCollection.addNewAtom();
    atom.atomName = atomName;
    if (sfacElementSymbols != null) {
      int elementIndex = scatterFactor - 1;
      if (elementIndex >= 0 && elementIndex < sfacElementSymbols.length)
        atom.elementSymbol = sfacElementSymbols[elementIndex];
    }
    setAtomCoord(atom, x, y, z);
  }

  private void processCmdfAtoms() throws Exception {
    while (readLine() != null && line.length() > 10) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens();
      atom.elementSymbol = getSymbol(tokens[0]);
      setAtomCoord(atom, parseFloat(tokens[2]), parseFloat(tokens[3]),
          parseFloat(tokens[4]));
    }
  }

  private String getSymbol(String sym) {
    if (sym == null)
      return "Xx";
    int len = sym.length();
    if (len < 2)
      return sym;
    char ch1 = sym.charAt(1);
    if (ch1 >= 'a' && ch1 <= 'z')
      return sym.substring(0, 2);
    return "" + sym.charAt(0);
  }

}
