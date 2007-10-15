/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.adapter.smarter;

import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JmolConstants;

import java.io.BufferedReader;

import java.util.Enumeration;
import java.util.Hashtable;

/*
 * Notes 9/2006 Bob Hanson
 * 
 * all parsing functions now moved to org.jmol.util.Parser
 * 
 * to add symmetry capability to any reader, some or all of the following 
 * methods need to be there:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetry()
 * 
 * At the very minimum, you need:
 * 
 *  setAtomCoord()
 *  applySymmetry()
 * 
 * so that:
 *  (a) atom coordinates can be turned fractional by load parameters
 *  (b) symmetry can be applied once per model in the file
 *  
 *  If you know where the end of the atom+bond data are, then you can
 *  use applySymmetry() once, just before exiting. Otherwise, use it
 *  twice -- it has a check to make sure it doesn't RUN twice -- once
 *  at the beginning and once at the end of the model.
 *  
 *  LOAD PARAMETERS:
 *  
 *  load parameters are in the form of an integer array of varying length.
 *  Some of these must be implemented in individual readers
 *  
 *  0:       desired model number
 *  1 - 3:   {i j k} lattice parameters for applying symmetry
 *  4:       desired space group number
 *  5 - 10:  unit cell parameters a b c alpha beta gamma
 * 
 */

public abstract class AtomSetCollectionReader {
  public AtomSetCollection atomSetCollection;
  public BufferedReader reader;
  public String line, prevline;
  
  protected long ptLine;
  

  public final static float ANGSTROMS_PER_BOHR = 0.5291772f;

  public int desiredModelNumber;
  public int modelNumber;
  public boolean iHaveDesiredModel;

  public String spaceGroup;
  public UnitCell unitcell;
  public float[] notionalUnitCell; //0-5 a b c alpha beta gamma; 6-21 matrix c->f
  public int[] latticeCells = new int[3];
  public int desiredSpaceGroupIndex;

  public int[] next = new int[1];
  
  // parser functions are static, so they need notstatic counterparts
   
  
  protected String[] getTokens() {
    return Parser.getTokens(line);  
  }
  
  protected static void getTokensFloat(String s, float[] f, int n) {
    Parser.parseFloatArray(getTokens(s), f, n);
  }
  
  protected static String[] getTokens(String s) {
    return Parser.getTokens(s);  
  }
  
  protected static String[] getTokens(String s, int iStart) {
    return Parser.getTokens(s, iStart);  
  }
  
  protected float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  public float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }

  protected float parseFloat(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseFloat(s, iEnd, next);
  }
  
  protected int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  public int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  protected int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }
  
  protected int parseInt(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseInt(s, iEnd, next);
  }

  protected String parseToken() {
    return Parser.parseToken(line, next);
  }
  
  protected String parseToken(String s) {
    next[0] = 0;
    return Parser.parseToken(s, next);
  }
  
  protected String parseTokenNext(String s) {
    return Parser.parseToken(s, next);
  }

  protected String parseToken(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseToken(s, iEnd, next);
  }
  
  protected static String parseTrimmed(String s, int iStart) {
    return Parser.parseTrimmed(s, iStart);
  }
  
  protected static String parseTrimmed(String s, int iStart, int iEnd) {
    return Parser.parseTrimmed(s, iStart, iEnd);
  }
  
  // load options:

  protected boolean doApplySymmetry;
  boolean doConvertToFractional;
  boolean fileCoordinatesAreFractional;
  boolean ignoreFileUnitCell;
  protected boolean ignoreFileSymmetryOperators;
  boolean ignoreFileSpaceGroupName;
  boolean isTrajectory;
  protected boolean applySymmetryToBonds;
  float symmetryRange;  

  // state variables
  public boolean iHaveUnitCell;
  //boolean iHaveCartesianToFractionalMatrix;
  private boolean iHaveFractionalCoordinates;
  private boolean iHaveSymmetryOperators;
  public boolean needToApplySymmetry;

  public abstract AtomSetCollection readAtomSetCollection(BufferedReader reader);

  public AtomSetCollection readAtomSetCollectionFromDOM(Object DOMNode) {
    return null;
  }

  public AtomSetCollection setError(Exception e) {
    e.printStackTrace();
    if (line == null)
      atomSetCollection.errorMessage = "Unexpected end of file after line "
          + --ptLine + ":\n" + prevline;
    else
      atomSetCollection.errorMessage = "Error reading file at line " + ptLine
          + ":\n" + line + "\n" + e.getMessage();
    return atomSetCollection;
  }
  
  public void initialize() {
    // called by the resolver
    modelNumber = 0;
    desiredModelNumber = -1;
    iHaveDesiredModel = false;

    latticeCells[0] = latticeCells[1] = latticeCells[2] = 0;

    desiredSpaceGroupIndex = -1;

    isTrajectory = false;

    ignoreFileUnitCell = false;
    ignoreFileSpaceGroupName = false;
    ignoreFileSymmetryOperators = false;
    doConvertToFractional = false;
    doApplySymmetry = false;
    applySymmetryToBonds = false;

    fileCoordinatesAreFractional = false;

    iHaveUnitCell = false;
    //iHaveCartesianToFractionalMatrix = false;
    iHaveFractionalCoordinates = false;
    iHaveSymmetryOperators = false;

    initializeSymmetry();
  }

  public void initialize(Hashtable htParams) {
    initialize();
    int[] params = null;
    if (htParams != null) {
      params = (int[]) htParams.get("params");
      applySymmetryToBonds = htParams.containsKey("applySymmetryToBonds");
    }
    if (params == null)
      return;
    Float distance = (Float) htParams.get("symmetryRange");
    symmetryRange = (distance == null ? 0 : distance.floatValue());

    // params is of variable length: 4, 5, or 11
    // [desiredModelNumber, i, j, k, 
    //  desiredSpaceGroupIndex,
    //  a*10000, b*10000, c*10000, alpha*10000, beta*10000, gamma*10000]

    isTrajectory = params[0] == -1;
    if (!isTrajectory)
      desiredModelNumber = params[0];
    latticeCells[0] = params[1];
    latticeCells[1] = params[2];
    latticeCells[2] = params[3];
    doApplySymmetry = (latticeCells[0] > 0 && latticeCells[1] > 0
        && latticeCells[2] > 0 || latticeCells[0] > 9 && latticeCells[1] > 9);
    //allows for {1 1 1} or {555 555 0|1}
    if (!doApplySymmetry) {
      latticeCells[0] = 0;
      latticeCells[1] = 0;
      latticeCells[2] = 0;
    }

    //this flag FORCES symmetry -- generally if coordinates are not fractional,
    //we may note the unit cell, but we do not apply symmetry
    //with this flag, we convert any nonfractional coordinates to fractional
    //if a unit cell is available.

    if (params.length >= 5) {
      // three options include:
      // = -1: normal -- use operators if present or name if not
      // >=0: spacegroup fully determined
      // = -999: ignore just the operators

      desiredSpaceGroupIndex = params[4];
      ignoreFileSpaceGroupName = (desiredSpaceGroupIndex >= 0);
      ignoreFileSymmetryOperators = (desiredSpaceGroupIndex != -1);
    }
    if (params.length >= 11) {
      setUnitCell(params[5] / 10000f, params[6] / 10000f, params[7] / 10000f,
          params[8] / 10000f, params[9] / 10000f, params[10] / 10000f);
      ignoreFileUnitCell = iHaveUnitCell;
    }
  }

  private void initializeSymmetry() {
    iHaveUnitCell = ignoreFileUnitCell;
    if (!ignoreFileUnitCell) {
      notionalUnitCell = new float[22];
      //0-5 a b c alpha beta gamma; 6-21 m00 m01... m33 cartesian-->fractional
      for (int i = 22; --i >= 0;)
        notionalUnitCell[i] = Float.NaN;
      unitcell = null;
    }
    if (!ignoreFileSpaceGroupName)
      spaceGroup = "unspecified *";

    needToApplySymmetry = false;
  }

  public void newAtomSet(String name) {
    if (atomSetCollection.currentAtomSetIndex >= 0) {
      atomSetCollection.newAtomSet();
      atomSetCollection.setCollectionName("<collection of "
          + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
    } else {
      atomSetCollection.setCollectionName(name);
    }
    Logger.debug(name);
  }

  public void setSpaceGroupName(String name) {
    if (ignoreFileSpaceGroupName)
      return;
    spaceGroup = name.trim();
  }

  public void setSymmetryOperator(String jonesFaithful) {
    if (ignoreFileSymmetryOperators)
      return;
    atomSetCollection.setLatticeCells(latticeCells, applySymmetryToBonds);
    if (!atomSetCollection.addSymmetry(jonesFaithful))
      Logger.warn("Skipping symmetry operation " + jonesFaithful);
    iHaveSymmetryOperators = true;
  }

  private int nMatrixElements = 0;
  public void initializeCartesianToFractional() {
    for (int i = 0; i < 16; i++)
      if (!Float.isNaN(notionalUnitCell[6 + i]))
        return; //just do this once
    for (int i = 0; i < 16; i++)
      notionalUnitCell[6 + i] = ((i % 5 == 0 ? 1 : 0));
    nMatrixElements = 0;
  }

  public void setUnitCellItem(int i, float x) {
    if (ignoreFileUnitCell)
      return;
    if (i >= 6 && Float.isNaN(notionalUnitCell[6]))
      initializeCartesianToFractional();
    notionalUnitCell[i] = x;
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("setunitcellitem " + i + " " + x);
    }
    if (i < 6)
      iHaveUnitCell = checkUnitCell(6);
    else if(++nMatrixElements == 12)
      checkUnitCell(22);
  }

  public void setUnitCell(float a, float b, float c, float alpha, float beta,
                   float gamma) {
    if (ignoreFileUnitCell)
      return;
    notionalUnitCell[UnitCell.INFO_A] = a;
    notionalUnitCell[UnitCell.INFO_B] = b;
    notionalUnitCell[UnitCell.INFO_C] = c;
    notionalUnitCell[UnitCell.INFO_ALPHA] = alpha;
    notionalUnitCell[UnitCell.INFO_BETA] = beta;
    notionalUnitCell[UnitCell.INFO_GAMMA] = gamma;
    iHaveUnitCell = checkUnitCell(6);
  }

  private boolean checkUnitCell(int n) {
    for (int i = 0; i < n; i++)
      if (Float.isNaN(notionalUnitCell[i]))
        return false;
    unitcell = new UnitCell(notionalUnitCell);
    if (doApplySymmetry)
      doConvertToFractional = !fileCoordinatesAreFractional;
    //if (but not only if) applying symmetry do we force conversion
    return true;
  }

  public void setFractionalCoordinates(boolean TF) {
    iHaveFractionalCoordinates = fileCoordinatesAreFractional = TF;
  }

  public void setAtomCoord(Atom atom, float x, float y, float z) {
    atom.x = x;
    atom.y = y;
    atom.z = z;
    setAtomCoord(atom);
  }

  public void setAtomCoord(Atom atom) {
    if (doConvertToFractional && !fileCoordinatesAreFractional
        && unitcell != null) {
      unitcell.toFractional(atom);
      iHaveFractionalCoordinates = true;
    }
    //if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
    //Logger.debug(" atom "+atom.atomName + " " + atom.x + " " + atom.y+" "+atom.z);
    needToApplySymmetry = true;
  }

  protected void addSites(Hashtable htSites) {
    atomSetCollection.setAtomSetAuxiliaryInfo("pdbSites", htSites);
    Enumeration e = htSites.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Hashtable htSite = (Hashtable) htSites.get(name);
      String seqNum = (String) htSite.get("seqNum");
      String groups = (String) htSite.get("groups");
      addJmolScript("@site_" + name + " " + groups);
      addJmolScript("@" + seqNum + " " + groups);
      addJmolScript("site_" + name + " = \"" + groups + "\".split(\",\")");
    }
  }

  public void applySymmetry() throws Exception {
    if (isTrajectory)
      atomSetCollection.setTrajectory();
    if (!needToApplySymmetry || !iHaveUnitCell) {
      initializeSymmetry();
      return;
    }
    atomSetCollection.setCoordinatesAreFractional(iHaveFractionalCoordinates);
    atomSetCollection.setNotionalUnitCell(notionalUnitCell);
    atomSetCollection.setAtomSetSpaceGroupName(spaceGroup);
    atomSetCollection.setSymmetryRange(symmetryRange);
    if (doConvertToFractional || fileCoordinatesAreFractional) {
      atomSetCollection.setLatticeCells(latticeCells, applySymmetryToBonds);
      if (ignoreFileSpaceGroupName || !iHaveSymmetryOperators) {
        SpaceGroup sg = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex,
            (spaceGroup.indexOf("*")>=0 ? "P1" : spaceGroup), notionalUnitCell, atomSetCollection.doNormalize);
        if (sg != null) {
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
            Logger.debug("using generated space group " + sg.dumpInfo());
          atomSetCollection.setAtomSetSpaceGroupName((spaceGroup.indexOf("*")>=0 ? spaceGroup : sg.getName()));
          atomSetCollection.applySymmetry(sg);
        }
      } else {
        atomSetCollection.applySymmetry();
      }
    }
    initializeSymmetry();
  }

  public static String getElementSymbol(int elementNumber) {
    return JmolConstants.elementSymbolFromNumber(elementNumber);
  }
  
  protected void fillDataBlock(String[][] data) throws Exception {
    int nLines = data.length;
    for (int i = 0; i < nLines; i++)
      data[i] = getTokens(discardLinesUntilNonBlank());
  }

  protected void discardLines(int nLines) throws Exception {
    for (int i = nLines; --i >= 0;)
      readLine();
  }

  protected String discardLinesUntilStartsWith(String startsWith) throws Exception {
    while (readLine() != null && !line.startsWith(startsWith)) {
    }
    return line;
  }

  protected String discardLinesUntilContains(String containsMatch) throws Exception {
    while (readLine() != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  protected void discardLinesUntilBlank() throws Exception {
    while (readLine() != null && line.trim().length() != 0) {
    }
  }

  protected String discardLinesUntilNonBlank() throws Exception {
    while (readLine() != null && line.trim().length() == 0) {
    }
    return line;
  }

  protected void checkLineForScript(String line) {
    this.line = line;
    checkLineForScript();
  }
  
  public void checkLineForScript() {
    if (line.indexOf("Jmol PDB-encoded data") >= 0) 
       atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolData", "" + line);
    if (line.endsWith("#noautobond")) {
      line = line.substring(0, line.lastIndexOf('#')).trim();
      atomSetCollection.setAtomSetCollectionProperty("noautobond", "true");
    }
    int pt = line.indexOf("jmolscript:");
    if (pt >= 0) {
      String script = line.substring(pt + 11, line.length());
      if (script.indexOf("#") >= 0) {
        script = script.substring(0, script.indexOf("#"));
      }
      addJmolScript(script);
      line = line.substring(0, pt).trim();
    }
  }

  protected void addJmolScript(String script) {
    String previousScript = atomSetCollection
        .getAtomSetCollectionProperty("jmolscript");
    if (previousScript == null)
      previousScript = "";
    else
      previousScript += ";";
    Logger.info("#jmolScript: " + script);
    atomSetCollection.setAtomSetCollectionProperty("jmolscript", previousScript
        + script);
  }

  public String readLine() throws Exception {
    prevline = line;
    line = reader.readLine();
    ptLine++;
    //System.out.println("readLine " + ptLine + " " + line);
    return line;
  }

  protected String readLineTrimmed() throws Exception {
    readLine();
    if (line == null)
      line = "";
    return line = line.trim();
  }
  
  protected String[] getStrings(String sinfo, int nFields, int width) {
    String[] fields = new String[nFields];
    int pt = 0;
    for (int i = 0; i < nFields; i++)
      fields[i] = sinfo.substring(pt, width);
    return fields;
  }
    
}
