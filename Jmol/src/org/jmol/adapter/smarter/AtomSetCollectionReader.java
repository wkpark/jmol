/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

package org.jmol.adapter.smarter;


import java.io.BufferedReader;
import java.util.Map;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolDocument;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import javajs.util.M3;
import javajs.util.P3;
import org.jmol.util.Quaternion;

import javajs.util.OC;
import javajs.util.PT;
import javajs.util.V3;
import javajs.util.List;
import javajs.util.SB;

import org.jmol.viewer.Viewer;


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
 * 
 * At the very minimum, you need:
 * 
 *  setAtomCoord()
 * 
 * so that:
 *  (a) atom coordinates can be turned fractional by load parameters
 *  (b) symmetry can be applied once per model in the file
 *  
 *  If you know where the end of the atom+bond data are, then you can
 *  use applySymmetryAndSetTrajectory() once, just before exiting. Otherwise, use it
 *  twice -- it has a check to make sure it doesn't RUN twice -- once
 *  at the beginning and once at the end of the model.
 *  
 * htParams is used for passing information to the readers
 * and for returning information from the readers
 * 
 * It won't be null at this stage.
 * 
 * from Eval or Viewer:
 * 
 *  applySymmetryToBonds
 *  atomTypes (for Mol2Reader)
 *  bsModels
 *  filter
 *  firstLastStep
 *  firstLastSteps
 *  getHeader
 *  isTrajectory
 *  lattice
 *  manifest (for SmarterJmolAdapter)
 *  modelNumber
 *  spaceGroupIndex
 *  symmetryRange
 *  unitcell
 *  packed
 *  
 * from FileManager:
 * 
 *  fullPathName
 *  subFileList (for SmarterJmolAdapter)
 * 
 * from MdTopReader:
 *   
 *  isPeriodic
 *  templateAtomCount
 *  
 * from MdCrdReader:   
 * 
 *  trajectorySteps
 *  
 * from Resolver:
 * 
 *  filteredAtomCount
 *  ptFile
 *  readerName
 *  templateAtomCount
 *  
 *  
 * from AtomSetCollectionReader:
 *  
 *  bsFilter
 *  
 * 
 */

public abstract class AtomSetCollectionReader {

  public final static float ANGSTROMS_PER_BOHR = 0.5291772f; // used by SpartanArchive

  public boolean isBinary;

  public AtomSetCollection atomSetCollection;
  protected BufferedReader reader;
  protected JmolDocument binaryDoc;
  protected String readerName;
  public Map<String, Object> htParams;
  public List<P3[]> trajectorySteps;

  //protected String parameterData;

  // buffer
  public String line, prevline;
  protected int[] next = new int[1];
  protected int ptLine;

  // protected/public state variables
  public int[] latticeCells;
  public boolean doProcessLines;
  public boolean iHaveUnitCell;
  public boolean iHaveSymmetryOperators;
  public boolean continuing = true;
  
  public Viewer viewer; // used by GenNBOReader and by CifReader

  public boolean doApplySymmetry;
  protected boolean ignoreFileSymmetryOperators;
  protected boolean isTrajectory;
  public boolean applySymmetryToBonds;
  protected boolean doCheckUnitCell;
  protected boolean getHeader;
  protected boolean isSequential;
  protected int templateAtomCount;
  public int modelNumber;
  protected int vibrationNumber;
  public int desiredVibrationNumber = Integer.MIN_VALUE;
  protected BS bsModels;
  protected boolean havePartialChargeFilter;
  public String calculationType = "?";
  protected String spaceGroup;
  protected boolean ignoreFileUnitCell;
  protected boolean ignoreFileSpaceGroupName;
  public float[] notionalUnitCell; //0-5 a b c alpha beta gamma; 6-21 matrix c->f
  protected int desiredModelNumber = Integer.MIN_VALUE;
  public SymmetryInterface symmetry;
  protected OC out;
  protected boolean iHaveFractionalCoordinates;
  public boolean doPackUnitCell;
  protected String strSupercell;
  protected P3 ptSupercell;
  protected boolean mustFinalizeModelSet;
  protected boolean forcePacked;

  // private state variables

  private SB loadNote = new SB();
  boolean doConvertToFractional;
  boolean fileCoordinatesAreFractional;
  boolean merging;
  float symmetryRange;
  private int[] firstLastStep;
  private int lastModelNumber = Integer.MAX_VALUE;
  int desiredSpaceGroupIndex = -1;
  protected P3 fileScaling;
  protected P3 fileOffset;
  private P3 fileOffsetFractional;
  P3 unitCellOffset;
  private boolean unitCellOffsetFractional;

//    public void finalize() {
//      System.out.println(this + " finalized");
//    }

  protected String filePath;
  protected String fileName;

  protected int stateScriptVersionInt = Integer.MAX_VALUE; // for compatibility PDB reader Jmol 12.0.RC24 fix 
  // http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/org/jmol/adapter/readers/cifpdb/PdbReader.java?r1=13502&r2=13525

  protected void setup(String fullPath, Map<String, Object> htParams, Object reader) {
    setupASCR(fullPath, htParams, reader);
  }

  protected void setupASCR(String fullPath, Map<String, Object> htParams, Object reader) {
    if (fullPath == null)
      return;
    this.htParams = htParams;
    filePath = fullPath.replace('\\', '/');
    int i = filePath.lastIndexOf('/');
    fileName = filePath.substring(i + 1);
    if (reader instanceof BufferedReader)
      this.reader = (BufferedReader) reader;
    else if (reader instanceof JmolDocument)
      binaryDoc = (JmolDocument) reader;
  }

  Object readData() throws Exception {
    initialize();
    atomSetCollection = new AtomSetCollection(readerName, this, null, null);
    try {
      initializeReader();
      if (binaryDoc == null) {
        if (line == null && continuing)
          readLine();
        while (line != null && continuing)
          if (checkLine())
            readLine();
      } else {
        binaryDoc.setOutputChannel(out);
        processBinaryDocument();
      }
      finalizeReader(); // upstairs
    } catch (Throwable e) {
      Logger.info("Reader error: " + e);
      if (!viewer.isJS)
        e.printStackTrace();
      setError(e);
    }
    if (reader != null)
      reader.close();
    if (binaryDoc != null)
      binaryDoc.close();
    return finish();
  }

  private void fixBaseIndices() {
    try {
    int baseAtomIndex = ((Integer) htParams.get("baseAtomIndex")).intValue();
    int baseModelIndex = ((Integer) htParams.get("baseModelIndex")).intValue();
    baseAtomIndex += atomSetCollection.atomCount;
    baseModelIndex += atomSetCollection.atomSetCount;
    htParams.put("baseAtomIndex", Integer.valueOf(baseAtomIndex));
    htParams.put("baseModelIndex", Integer.valueOf(baseModelIndex));
    } catch (Exception e) {
      // ignore
    }
  }

  protected Object readDataObject(Object node) throws Exception {
    initialize();
    atomSetCollection = new AtomSetCollection(readerName, this, null, null);
    initializeReader();
    processDOM(node);
    return finish();
  }

  /**
   * 
   * @param DOMNode
   */
  protected void processDOM(Object DOMNode) {
    // XML readers only
  }

  /**
   * @throws Exception 
   */
  protected void processBinaryDocument() throws Exception {
    // Binary readers only
  }

  protected void initializeReader() throws Exception {
    // reader-dependent
  }

  /**
   * @return true if need to read new line
   * @throws Exception 
   * 
   */
  protected boolean checkLine() throws Exception {
    // reader-dependent
    return true;
  }

  /**
   * sets continuing and doProcessLines
   * 
   * @return TRUE if continuing, FALSE if not
   * 
   */
  public boolean checkLastModel() {
    if (isLastModel(modelNumber) && doProcessLines) {
      continuing = false;
      return false;
    }
    doProcessLines = false;
    return true;
  }

  /**
   * after reading a model, Q: Is this the last model?
   * 
   * @param modelNumber
   * @return  Yes/No
   */
  public boolean isLastModel(int modelNumber) {
    return (desiredModelNumber > 0 || modelNumber >= lastModelNumber);
  }

  public String appendLoadNote(String info) {
    loadNote.append(info).append("\n");
    Logger.info(info);
    return info;
  }

  @SuppressWarnings("unchecked")
  protected void initializeTrajectoryFile() {
    // add a dummy atom, just so not "no atoms found"
    atomSetCollection.addAtom(new Atom());
    trajectorySteps = (List<P3[]>) htParams.get("trajectorySteps");
    if (trajectorySteps == null)
      htParams.put("trajectorySteps", trajectorySteps = new  List<P3[]>());
  }

  /**
   * optional reader-specific method run first.  
   * @throws Exception
   */
  protected void finalizeReader() throws Exception {
    finalizeReaderASCR();
  }

  protected void finalizeReaderASCR() throws Exception {
    applySymmetryAndSetTrajectory();
    setLoadNote();
    atomSetCollection.finalizeStructures();
    if (doCentralize)
      atomSetCollection.centralize();
  }

  /////////////////////////////////////////////////////////////////////////////////////

  protected void setLoadNote() {
    if (loadNote.length() > 0)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("modelLoadNote", loadNote.toString());
  }

  public void setIsPDB() {
    atomSetCollection.setGlobalBoolean(AtomSetCollection.GLOBAL_ISPDB);
    atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
    if (htParams.get("pdbNoHydrogens") != null)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("pdbNoHydrogens",
          htParams.get("pdbNoHydrogens"));
    if (checkFilterKey("ADDHYDROGENS"))
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("pdbAddHydrogens",Boolean.TRUE);      
  }

  private Object finish() {
    String s = (String) htParams.get("loadState");
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("loadState",
        s == null ? "" : s);
    s = (String) htParams.get("smilesString");
    if (s != null)
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("smilesString", s);
    if (!htParams.containsKey("templateAtomCount"))
      htParams.put("templateAtomCount", Integer.valueOf(atomSetCollection
          .atomCount));
    if (htParams.containsKey("bsFilter"))
      htParams.put("filteredAtomCount", Integer.valueOf(BSUtil
          .cardinalityOf((BS) htParams.get("bsFilter"))));
    if (!calculationType.equals("?"))
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("calculationType",
          calculationType);

    String name = atomSetCollection.fileTypeName;
    String fileType = name;
    if (fileType.indexOf("(") >= 0)
      fileType = fileType.substring(0, fileType.indexOf("("));
    for (int i = atomSetCollection.atomSetCount; --i >= 0;) {
      atomSetCollection.setAtomSetAuxiliaryInfoForSet("fileName", filePath, i);
      atomSetCollection.setAtomSetAuxiliaryInfoForSet("fileType", fileType, i);
    }
    atomSetCollection.freeze(reverseModels);
    if (atomSetCollection.errorMessage != null)
      return atomSetCollection.errorMessage + "\nfor file " + filePath
          + "\ntype " + name;
    if ((atomSetCollection.bsAtoms == null ? atomSetCollection.atomCount
        : atomSetCollection.bsAtoms.cardinality()) == 0
        && fileType.indexOf("DataOnly") < 0 && atomSetCollection.getAtomSetCollectionAuxiliaryInfo("dataOnly") == null)
      return "No atoms found\nfor file " + filePath + "\ntype " + name;
    fixBaseIndices();
    return atomSetCollection;
  }

  /**
   * @param e  
   */
  private void setError(Throwable e) {
    String s;
    /**
     * @j2sNative
     * 
     * if (e.getMessage)
     *  s = e.getMessage()
     * else
     *  s = e.toString();
     */
    {
      s = e.getMessage();
    }
    if (line == null)
      atomSetCollection.errorMessage = "Error reading file at end of file \n" + s;
    else
      atomSetCollection.errorMessage = "Error reading file at line " + ptLine
          + ":\n" + line + "\n" + s;
    if (!viewer.isJS)
      e.printStackTrace();
  }

  @SuppressWarnings("unchecked")
  private void initialize() {
    Object o = htParams.get("supercell");
    if (o instanceof String)
      strSupercell = (String) o;
    else
      ptSupercell = (P3) o;
    initializeSymmetry();
    viewer = (Viewer) htParams.remove("viewer"); // don't pass this on to user
    if (htParams.containsKey("stateScriptVersionInt"))
      stateScriptVersionInt = ((Integer) htParams.get("stateScriptVersionInt"))
          .intValue();
    merging = htParams.containsKey("merging");
    getHeader = htParams.containsKey("getHeader");
    isSequential = htParams.containsKey("isSequential");
    readerName = (String) htParams.get("readerName");
    if (htParams.containsKey("outputChannel"))
      out = (OC) htParams.get("outputChannel");
    //parameterData = (String) htParams.get("parameterData");
    if (htParams.containsKey("vibrationNumber"))
      desiredVibrationNumber = ((Integer) htParams.get("vibrationNumber"))
          .intValue();
    else if (htParams.containsKey("modelNumber"))
      desiredModelNumber = ((Integer) htParams.get("modelNumber")).intValue();
    applySymmetryToBonds = htParams.containsKey("applySymmetryToBonds");
    bsFilter = (BS) htParams.get("bsFilter");
    setFilter(null);
    // ptFile < 0 indicates just one file being read
    // ptFile >= 0 indicates multiple files are being loaded
    // if the file is not the first read in the LOAD command, then
    // we look to see if it was loaded using LOAD ... "..." COORD ....
    int ptFile = (htParams.containsKey("ptFile") ? ((Integer) htParams
        .get("ptFile")).intValue() : -1);
    isTrajectory = htParams.containsKey("isTrajectory");
    if (ptFile > 0 && htParams.containsKey("firstLastSteps")) {
      Object val = ((List<Object>) htParams.get("firstLastSteps"))
          .get(ptFile - 1);
      if (val instanceof BS) {
        bsModels = (BS) val;
      } else {
        firstLastStep = (int[]) val;
      }
    } else if (htParams.containsKey("firstLastStep")) {
      firstLastStep = (int[]) htParams.get("firstLastStep");
    } else if (htParams.containsKey("bsModels")) {
      bsModels = (BS) htParams.get("bsModels");
    }
    if (htParams.containsKey("templateAtomCount"))
      templateAtomCount = ((Integer) htParams.get("templateAtomCount"))
          .intValue();
    if (bsModels != null || firstLastStep != null)
      desiredModelNumber = Integer.MIN_VALUE;
    if (bsModels == null && firstLastStep != null) {
      if (firstLastStep[0] < 0)
        firstLastStep[0] = 0;
      if (firstLastStep[2] == 0 || firstLastStep[1] < firstLastStep[0])
        firstLastStep[1] = -1;
      if (firstLastStep[2] < 1)
        firstLastStep[2] = 1;
      bsModels = BSUtil.newAndSetBit(firstLastStep[0]);
      if (firstLastStep[1] > firstLastStep[0]) {
        for (int i = firstLastStep[0]; i <= firstLastStep[1]; i += firstLastStep[2])
          bsModels.set(i);
      }
    }
    if (bsModels != null && (firstLastStep == null || firstLastStep[1] != -1))
      lastModelNumber = bsModels.length();

    symmetryRange = (htParams.containsKey("symmetryRange") ? ((Float) htParams
        .get("symmetryRange")).floatValue() : 0);
    initializeSymmetryOptions();
    //this flag FORCES symmetry -- generally if coordinates are not fractional,
    //we may note the unit cell, but we do not apply symmetry
    //with this flag, we convert any nonfractional coordinates to fractional
    //if a unit cell is available.

    if (htParams.containsKey("spaceGroupIndex")) {
      // three options include:
      // = -1: normal -- use operators if present or name if not
      // = -2: user is supplying operators or name
      // >=0: spacegroup fully determined
      // = -999: ignore -- just the operators

      desiredSpaceGroupIndex = ((Integer) htParams.get("spaceGroupIndex"))
          .intValue();
      if (desiredSpaceGroupIndex == -2)
        spaceGroup = (String) htParams.get("spaceGroupName");
      ignoreFileSpaceGroupName = (desiredSpaceGroupIndex == -2 || desiredSpaceGroupIndex >= 0);
      ignoreFileSymmetryOperators = (desiredSpaceGroupIndex != -1);
    }
    if (htParams.containsKey("unitCellOffset")) {
      fileScaling = P3.new3(1, 1, 1);
      fileOffset = (P3) htParams.get("unitCellOffset");
      fileOffsetFractional = P3.newP(fileOffset);
      unitCellOffsetFractional = htParams
          .containsKey("unitCellOffsetFractional");
    }
    if (htParams.containsKey("unitcell")) {
      float[] fParams = (float[]) htParams.get("unitcell");
      if (merging)
        setFractionalCoordinates(true);
      if (fParams.length == 9) {
        // these are vectors
        addPrimitiveLatticeVector(0, fParams, 0);
        addPrimitiveLatticeVector(1, fParams, 3);
        addPrimitiveLatticeVector(2, fParams, 6);
      } else {
        setUnitCell(fParams[0], fParams[1], fParams[2], fParams[3], fParams[4],
            fParams[5]);
      }
      ignoreFileUnitCell = iHaveUnitCell;
      if (merging && !iHaveUnitCell)
        setFractionalCoordinates(false);
      // with appendNew == false and UNITCELL parameter, we assume fractional coordinates
    }
  }

  protected void initializeSymmetryOptions() {
    latticeCells = new int[3];
    doApplySymmetry = false;
    P3 pt = (P3) htParams.get("lattice");
    if (pt == null) {
      if (!forcePacked)
        return;
      pt = P3.new3(1, 1, 1);
    }
    latticeCells[0] = (int) pt.x;
    latticeCells[1] = (int) pt.y;
    latticeCells[2] = (int) pt.z;
    doCentroidUnitCell = (htParams.containsKey("centroid"));
    if (doCentroidUnitCell && (latticeCells[2] == -1 || latticeCells[2] == 0))
      latticeCells[2] = 1;
    boolean isPacked = forcePacked || htParams.containsKey("packed");
    centroidPacked = doCentroidUnitCell && isPacked;
    doPackUnitCell = !doCentroidUnitCell && (isPacked || latticeCells[2] < 0);
    doApplySymmetry = (latticeCells[0] > 0 && latticeCells[1] > 0);
    //allows for {1 1 1} or {1 1 -1} or {555 555 0|1|-1} (-1  being "packed")
    if (!doApplySymmetry)
      latticeCells = new int[3];
  }

  public boolean haveModel;

  public boolean doGetModel(int modelNumber, String title) {
    if (title != null && nameRequired != null && nameRequired.length() > 0 
        && title.toUpperCase().indexOf(nameRequired) < 0)
          return false;
    // modelNumber is 1-based, but firstLastStep is 0-based
    boolean isOK = (bsModels == null ? desiredModelNumber < 1
        || modelNumber == desiredModelNumber
        : modelNumber > lastModelNumber ? false : modelNumber > 0
            && bsModels.get(modelNumber - 1)
            || haveModel
            && firstLastStep != null
            && firstLastStep[1] < 0
            && (firstLastStep[2] < 2 || (modelNumber - 1 - firstLastStep[0])
                % firstLastStep[2] == 0));
    if (isOK && desiredModelNumber == 0)
      atomSetCollection.discardPreviousAtoms();
    haveModel |= isOK;
    if (isOK)
      doProcessLines = true;
    return isOK;
  }

  private String previousSpaceGroup;
  private float[] previousUnitCell;

  protected void initializeSymmetry() {
    previousSpaceGroup = spaceGroup;
    previousUnitCell = notionalUnitCell;
    iHaveUnitCell = ignoreFileUnitCell;
    if (!ignoreFileUnitCell) {
      notionalUnitCell = new float[25];
      //0-5 a b c alpha beta gamma
      //6-21 m00 m01... m33 cartesian-->fractional
      //22-24 supercell.x supercell.y supercell.z
      for (int i = 25; --i >= 0;)
        notionalUnitCell[i] = Float.NaN;
      if (ptSupercell != null) {
        notionalUnitCell[22] = Math.max(1, (int) ptSupercell.x);
        notionalUnitCell[23] = Math.max(1, (int) ptSupercell.y);
        notionalUnitCell[24] = Math.max(1, (int) ptSupercell.z);
      }
      symmetry = null;
    }
    if (!ignoreFileSpaceGroupName)
      spaceGroup = "unspecified!";
    doCheckUnitCell = false;
  }

  protected void newAtomSet(String name) {
    if (atomSetCollection.currentAtomSetIndex >= 0) {
      atomSetCollection.newAtomSet();
      atomSetCollection.setCollectionName("<collection of "
          + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
    } else {
      atomSetCollection.setCollectionName(name);
    }
    atomSetCollection.setAtomSetAuxiliaryInfoForSet("name", name, Math.max(0, atomSetCollection.currentAtomSetIndex));
  }

  protected int cloneLastAtomSet(int atomCount, P3[] pts) throws Exception {
    int lastAtomCount = atomSetCollection.getLastAtomSetAtomCount();
    atomSetCollection.cloneLastAtomSetFromPoints(atomCount, pts);
    if (atomSetCollection.haveUnitCell) {
      iHaveUnitCell = true;
      doCheckUnitCell = true;
      spaceGroup = previousSpaceGroup;
      notionalUnitCell = previousUnitCell;
    }
    return lastAtomCount;
  }

  public void setSpaceGroupName(String name) {
    if (ignoreFileSpaceGroupName)
      return;
    spaceGroup = name.trim();
    Logger.info("Setting space group name to " + spaceGroup);
  }

  public int setSymmetryOperator(String xyz) {
    if (ignoreFileSymmetryOperators)
      return -1;
    int isym = atomSetCollection.getXSymmetry().addSpaceGroupOperation(this, xyz);
    if (isym < 0)
      Logger.warn("Skipping symmetry operation " + xyz);
    iHaveSymmetryOperators = true;
    return isym;
  }

  private int nMatrixElements = 0;

  private void initializeCartesianToFractional() {
    for (int i = 0; i < 16; i++)
      if (!Float.isNaN(notionalUnitCell[6 + i]))
        return; //just do this once
    for (int i = 0; i < 16; i++)
      notionalUnitCell[6 + i] = ((i % 5 == 0 ? 1 : 0));
    nMatrixElements = 0;
  }

  public void clearUnitCell() {
    if (ignoreFileUnitCell)
      return;
    for (int i = 6; i < 22; i++)
      notionalUnitCell[i] = Float.NaN;
    checkUnitCell(6);
  }

  public void setUnitCellItem(int i, float x) {
    if (ignoreFileUnitCell)
      return;
    if (i == 0 && x == 1 || i == 3 && x == 0)
      return;
    if (!Float.isNaN(x) && i >= 6 && Float.isNaN(notionalUnitCell[6]))
      initializeCartesianToFractional();
    notionalUnitCell[i] = x;
    if (Logger.debugging) {
      Logger.debug("setunitcellitem " + i + " " + x);
    }
    if (i < 6 || Float.isNaN(x))
      iHaveUnitCell = checkUnitCell(6);
    else if (++nMatrixElements == 12)
      checkUnitCell(22);
  }

  protected M3 matUnitCellOrientation;

  public void setUnitCell(float a, float b, float c, float alpha, float beta,
                          float gamma) {
    if (ignoreFileUnitCell)
      return;
    clearUnitCell();
    notionalUnitCell[0] = a;
    notionalUnitCell[1] = b;
    notionalUnitCell[2] = c;
    if (alpha != 0)
      notionalUnitCell[3] = alpha;
    if (beta != 0)
      notionalUnitCell[4] = beta;
    if (gamma != 0)
      notionalUnitCell[5] = gamma;
    iHaveUnitCell = checkUnitCell(6);
  }

  public void addPrimitiveLatticeVector(int i, float[] xyz, int i0) {
    if (ignoreFileUnitCell)
      return;
    if (i == 0)
      for (int j = 0; j < 6; j++)
        notionalUnitCell[j] = 0;
    i = 6 + i * 3;
    notionalUnitCell[i++] = xyz[i0++];
    notionalUnitCell[i++] = xyz[i0++];
    notionalUnitCell[i] = xyz[i0];
    if (Float.isNaN(notionalUnitCell[0])) {
      for (i = 0; i < 6; i++)
        notionalUnitCell[i] = -1;
    }
    iHaveUnitCell = checkUnitCell(15);
  }

  private boolean checkUnitCell(int n) {
    for (int i = 0; i < n; i++)
      if (Float.isNaN(notionalUnitCell[i]))
        return false;
    getNewSymmetry().setUnitCell(notionalUnitCell);
    if (doApplySymmetry)
      doConvertToFractional = !fileCoordinatesAreFractional;
    //if (but not only if) applying symmetry do we force conversion
    checkUnitCellOffset();
    return true;
  }

  private void checkUnitCellOffset() {
    if (symmetry == null || fileOffsetFractional == null)
      return;
    fileOffset.setT(fileOffsetFractional);
    if (unitCellOffsetFractional != fileCoordinatesAreFractional) {
      if (unitCellOffsetFractional)
        symmetry.toCartesian(fileOffset, false);
      else
        symmetry.toFractional(fileOffset, false);
    }
  }

  protected SymmetryInterface getNewSymmetry() {
    symmetry = (SymmetryInterface) Interface
        .getOptionInterface("symmetry.Symmetry");
    return symmetry;
  }

  public void setFractionalCoordinates(boolean TF) {
    iHaveFractionalCoordinates = fileCoordinatesAreFractional = TF;
    checkUnitCellOffset();
  }

  /////////// FILTER /////////////////

  protected BS bsFilter;
  public String filter;
  private boolean haveAtomFilter;
  private boolean filterAltLoc;
  private boolean filterGroup3;
  private boolean filterChain;
  private boolean filterAtomName;
  private boolean filterAtomType;
  private String filterAtomTypeStr;
  private String filterAtomNameTerminator = ";";  
  private boolean filterElement;
  protected boolean filterHetero;
  private boolean filterEveryNth;
  private int filterN;
  private int nFiltered;
  private boolean doSetOrientation;
  protected boolean doCentralize;
  protected boolean addVibrations;
  protected boolean useAltNames;
  public boolean doReadMolecularOrbitals;
  protected boolean reverseModels;
  private String nameRequired;
  boolean doCentroidUnitCell;
  boolean centroidPacked;


  // ALL:  "CENTER" "REVERSEMODELS"
  // MANY: "NOVIB" "NOMO"
  // CASTEP: "CHARGE=HIRSH q={i,j,k};"
  // CIF: "ASSEMBLY n"
  // CRYSTAL: "CONV" (conventional), "INPUT"
  // CSF, SPARTAN: "NOORIENT"
  // GAMESS-US:  "CHARGE=LOW"
  // JME, MOL: "NOMIN"
  // MOL:  "2D"
  // Molden: "INPUT" "GEOM" "NOGEOM"
  // MopacArchive: "NOCENTER"
  // MOReaders: "NBOCHARGES"
  // P2N: "ALTNAME"
  // PDB: "BIOMOLECULE n;" "NOSYMMETRY"  "CONF n"
  // Spartan: "INPUT", "ESPCHARGES"
  // 

  protected void setFilterAtomTypeStr(String s) {
    // PDB reader TYPE=...
    filterAtomTypeStr = s;
    filterAtomNameTerminator = "\0";
  }
  
  protected void setFilter(String filter0) {
    if (filter0 == null) {
      filter0 = (String) htParams.get("filter");
    } else {
      bsFilter = null;
    }
    if (filter0 != null)
      filter0 = filter0.toUpperCase();
    filter = filter0;
    doSetOrientation = !checkFilterKey("NOORIENT");
    doCentralize = (!checkFilterKey("NOCENTER") && checkFilterKey("CENTER"));
    addVibrations = !checkFilterKey("NOVIB");
    doReadMolecularOrbitals = !checkFilterKey("NOMO");
    useAltNames = checkFilterKey("ALTNAME");
    reverseModels = checkFilterKey("REVERSEMODELS");
    if (checkFilterKey("NAME=")) {
      nameRequired = filter.substring(filter.indexOf("NAME=") + 5);
      if (nameRequired.startsWith("'"))
        nameRequired = PT.split(nameRequired, "'")[1]; 
      else if (nameRequired.startsWith("\""))
        nameRequired = PT.split(nameRequired, "\"")[1]; 
      filter0 = filter = PT.rep(filter, nameRequired,"");
      filter0 = filter = PT.rep(filter, "NAME=","");
    }
    if (filter == null)
      return;
    filterAtomName = checkFilterKey("*.") || checkFilterKey("!.");
    filterElement = checkFilterKey("_");
    filterHetero = checkFilterKey("HETATM"); // PDB
    filterGroup3 = checkFilterKey("[");
    filterChain = checkFilterKey(":");
    filterAltLoc = checkFilterKey("%");
    filterEveryNth = checkFilterKey("/=");
    if (filterEveryNth)
      filterN = parseIntStr(filter.substring(filter.indexOf("/=") + 2));
    else
      filterAtomType = checkFilterKey("=");
    if (filterN == Integer.MIN_VALUE)
      filterEveryNth = false;
    haveAtomFilter = filterAtomName || filterAtomType || filterElement || filterGroup3 || filterChain
        || filterAltLoc || filterHetero || filterEveryNth || checkFilterKey("/=");
    if (bsFilter == null) {
      // bsFilter is usually null, but from MDTOP it gets set to indicate
      // which atoms were selected by the filter. This then
      // gets used by COORD files to load just those coordinates
      // and it returns the bitset of filtered atoms
      bsFilter = new BS();
      htParams.put("bsFilter", bsFilter);
      filter = (";" + filter + ";").replace(',', ';');
      Logger.info("filtering with " + filter);
      if (haveAtomFilter) {
        int ipt;
        filter1 = filter;
        if ((ipt = filter.indexOf("|")) >= 0) {
          filter1 = filter.substring(0, ipt).trim() + ";";
          filter2 = ";" + filter.substring(ipt).trim();
        }
      }
    }
  }

  private String filter1, filter2;

  public String getFilter(String key) {
    int pt = (filter == null ? -1 : filter.indexOf(key));
    return (pt < 0 ? null : filter.substring(pt + key.length(), filter.indexOf(";", pt)));
  }

  public boolean checkFilterKey(String key) {
    return (filter != null && filter.indexOf(key) >= 0);
  }

  /**
   * @param atom
   * @param iAtom
   * @return        true if we want this atom
   */
  protected boolean filterAtom(Atom atom, int iAtom) {
    if (!haveAtomFilter)
      return true;
    // cif, mdtop, pdb, gromacs, pqr
    boolean isOK = checkFilter(atom, filter1);
    if (filter2 != null)
      isOK |= checkFilter(atom, filter2);
    if (isOK && filterEveryNth)
      isOK = (((nFiltered++) % filterN) == 0);
    bsFilter.setBitTo(iAtom >= 0 ? iAtom : atomSetCollection.atomCount, isOK);
    return isOK;
  }

  /**
   * 
   * @param atom
   * @param f
   * @return  true if a filter is found
   */
  private boolean checkFilter(Atom atom, String f) {
    return (!filterGroup3 || atom.group3 == null || !filterReject(f, "[",
        atom.group3.toUpperCase() + "]"))
        && (!filterAtomName || allowAtomName(atom.atomName, f))
        && (filterAtomTypeStr == null || atom.atomName == null 
            || atom.atomName.toUpperCase().indexOf("\0" + filterAtomTypeStr) >= 0)
        && (!filterElement || atom.elementSymbol == null || !filterReject(f, "_",
            atom.elementSymbol.toUpperCase() + ";"))
        && (!filterChain || atom.chainID == 0 || !filterReject(f, ":", ""
            + viewer.getChainIDStr(atom.chainID)))
        && (!filterAltLoc || atom.altLoc == '\0' || !filterReject(
            f, "%", "" + atom.altLoc))
        && (!filterHetero || !filterReject(f, "HETATM",
            atom.isHetero ? "HETATM" : "ATOM"));
  }

  public boolean rejectAtomName(String name) {
    return filterAtomName && !allowAtomName(name, filter);
  }

  private boolean allowAtomName(String atomName, String f) {
    return (atomName == null || !filterReject(f, ".",
        atomName.toUpperCase() + filterAtomNameTerminator));
  }

  protected boolean filterReject(String f, String code, String atomCode) {
    return (f.indexOf(code) >= 0 && (f.indexOf("!" + code) >= 0 ? f
        .indexOf(code + atomCode) >= 0 : f.indexOf(code + atomCode) < 0));
  }

  protected void set2D() {
    // MOL and JME
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("is2D", Boolean.TRUE);
    if (!checkFilterKey("NOMIN"))
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("doMinimize",
          Boolean.TRUE);
  }

  public boolean doGetVibration(int vibrationNumber) {
    // vibrationNumber is 1-based
    return addVibrations
        && (desiredVibrationNumber <= 0 || vibrationNumber == desiredVibrationNumber);
  }

  private M3 matrixRotate;

  public MSInterface ms;

  public void setTransform(float x1, float y1, float z1, float x2, float y2,
                           float z2, float x3, float y3, float z3) {
    if (matrixRotate != null || !doSetOrientation)
      return;
    matrixRotate = new M3();
    V3 v = V3.new3(x1, y1, z1);
    // rows in Sygress/CAChe and Spartan become columns here
    v.normalize();
    matrixRotate.setColumnV(0, v);
    v.set(x2, y2, z2);
    v.normalize();
    matrixRotate.setColumnV(1, v);
    v.set(x3, y3, z3);
    v.normalize();
    matrixRotate.setColumnV(2, v);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
        "defaultOrientationMatrix", M3.newM3(matrixRotate));
    // first two matrix column vectors define quaternion X and XY plane
    Quaternion q = Quaternion.newM(matrixRotate);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo(
        "defaultOrientationQuaternion", q);
    Logger.info("defaultOrientationMatrix = " + matrixRotate);

  }

  /////////////////////////////

  public void setAtomCoordXYZ(Atom atom, float x, float y, float z) {
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  public Atom setAtomCoordScaled(Atom atom, String[] tokens, int i, float f) {
    if (atom == null)
      atom = atomSetCollection.addNewAtom();
    setAtomCoordXYZ(atom, parseFloatStr(tokens[i]) * f,
        parseFloatStr(tokens[i + 1]) * f, parseFloatStr(tokens[i + 2]) * f);
    return atom;
  }

  protected void setAtomCoordTokens(Atom atom, String[] tokens, int i) {
    setAtomCoordXYZ(atom, parseFloatStr(tokens[i]), parseFloatStr(tokens[i + 1]),
          parseFloatStr(tokens[i + 2]));
  }

  public Atom addAtomXYZSymName(String[] tokens, int i, String sym, String name) {
    Atom atom = atomSetCollection.addNewAtom();
    if (sym != null)
      atom.elementSymbol = sym;
    if (name != null)
      atom.atomName = name;
    setAtomCoordTokens(atom, tokens, i);
    return atom;
  }

  public void setAtomCoord(Atom atom) {
    // fileScaling is used by the PLOT command to 
    // put data into PDB format, preserving name/residue information,
    // and still get any xyz data into the allotted column space.
    if (fileScaling != null) {
      atom.x = atom.x * fileScaling.x + fileOffset.x;
      atom.y = atom.y * fileScaling.y + fileOffset.y;
      atom.z = atom.z * fileScaling.z + fileOffset.z;
    }
    if (doConvertToFractional && !fileCoordinatesAreFractional
        && symmetry != null) {
      if (!symmetry.haveUnitCell())
        symmetry.setUnitCell(notionalUnitCell);
      symmetry.toFractional(atom, false);
      iHaveFractionalCoordinates = true;
    }
    doCheckUnitCell = true;
  }

  public void addSites(Map<String, Map<String, Object>> htSites) {
    atomSetCollection.setAtomSetAuxiliaryInfo("pdbSites", htSites);
    String sites = "";
    for (Map.Entry<String, Map<String, Object>> entry : htSites.entrySet()) {
      String name = entry.getKey();
      Map<String, Object> htSite = entry.getValue();
      char ch;
      for (int i = name.length(); --i >= 0;)
        if (!Character.isLetterOrDigit(ch = name.charAt(i)) && ch != '\'')
          name = name.substring(0, i) + "_" + name.substring(i + 1);
      //String seqNum = (String) htSite.get("seqNum");
      String groups = (String) htSite.get("groups");
      if (groups.length() == 0)
        continue;
      addSiteScript("@site_" + name + " " + groups);
      //addJmolScript("@" + seqNum + " " + groups);
      addSiteScript("site_" + name + " = \"" + groups + "\".split(\",\")");
      sites += (sites == "" ? "" : ",") + "site_" + name;
    }
    addSiteScript("site_list = \"" + sites + "\".split(\",\")");
  }

  public void applySymmetryAndSetTrajectory() throws Exception {
    // overridden in many readers
    applySymTrajASCR();
  }
  
  public SymmetryInterface applySymTrajASCR() throws Exception {
    if (forcePacked)
      initializeSymmetryOptions();
    SymmetryInterface sym = (iHaveUnitCell && doCheckUnitCell ? atomSetCollection
        .getXSymmetry().applySymmetryFromReader(this, symmetry) : null);
    if (isTrajectory)
      atomSetCollection.setTrajectory();
    initializeSymmetry();
    return sym;
  }

  protected void doPreSymmetry() {
  }

  @SuppressWarnings("unchecked")
  public void finalizeMOData(Map<String, Object> moData) {
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
    if (moData == null)
      return;
    List<Map<String, Object>> orbitals = (List<Map<String, Object>>) moData
        .get("mos");
    if (orbitals != null)
      Logger.info(orbitals.size() + " molecular orbitals read in model "
          + atomSetCollection.atomSetCount);
  }

  public static String getElementSymbol(int elementNumber) {
    return JmolAdapter.getElementSymbol(elementNumber);
  }

  /**
   * fills an array with a predefined number of lines of data that is 
   * arranged in fixed FORTRAN-like column format
   *   
   * @param data
   * @param col0
   * @param colWidth
   * @param minLineLen TODO
   * @throws Exception
   */
  protected void fillDataBlockFixed(String[][] data, int col0, int colWidth, int minLineLen)
      throws Exception {
    if (colWidth == 0) {
      fillDataBlock(data, minLineLen);
      return;
    }
    int nLines = data.length;
    for (int i = 0; i < nLines; i++) {
      discardLinesUntilNonBlank();
      int nFields = (line.length() - col0 + 1) / colWidth; // Dmol reader is one short
      data[i] = new String[nFields];
      for (int j = 0, start = col0; j < nFields; j++, start += colWidth)
        data[i][j] = line.substring(start, Math.min(line.length(), start + colWidth));
    }
  }

  /**
   * fills an array with a pre-defined number of lines of token data,
   * skipping blank lines in the process
   * 
   * @param data
   * @param minLineLen TODO
   * @throws Exception
   */
  protected void fillDataBlock(String[][] data, int minLineLen) throws Exception {
    int nLines = data.length;
    for (int i = 0; i < nLines; i++) { 
      data[i] = getTokensStr(discardLinesUntilNonBlank());
      if (data[i].length < minLineLen)
        --i;
    }
      
  }

  /**
   * fills a float array with string data from a file
   * @param s     string data containing floats
   * @param width column width or 0 to read tokens
   * @param data  result data to be filled
   * @return      data
   * @throws Exception
   */
  protected float[] fillFloatArray(String s, int width, float[] data)
      throws Exception {
    String[] tokens = new String[0];
    int pt = 0;
    for (int i = 0; i < data.length; i++) {
      while (tokens != null && pt >= tokens.length) {
        if (s == null)
          s = readLine();
        if (width == 0) {
          tokens = getTokensStr(s);
        } else {
          tokens = new String[s.length() / width];
          for (int j = 0; j < tokens.length; j++)
            tokens[j] = s.substring(j * width, (j + 1) * width);
        }
        s = null;
        pt = 0;
      }
      if (tokens == null)
        break;
      data[i] = parseFloatStr(tokens[pt++]);
    }
    return data;
  }

  /**
   * Extracts a block of frequency data from a file. This block may be of two
   * types -- either X Y Z across a row or each of X Y Z on a separate line.
   * Data is presumed to be in fixed FORTRAN-like column format, not
   * space-separated columns.
   * 
   * @param iAtom0
   *          the first atom to be assigned a frequency
   * @param atomCount
   *          the number of atoms to be assigned
   * @param modelAtomCount
   *          the number of atoms in each model
   * @param ignore
   *          the frequencies to ignore because the user has selected only
   *          certain vibrations to be read or for whatever reason; length
   *          serves to set the number of frequencies to be read
   * @param isWide
   *          when TRUE, this is a table that has X Y Z for each mode within the
   *          same row; when FALSE, this is a table that has X Y Z for each mode
   *          on a separate line.
   * @param col0
   *          the column in which data starts
   * @param colWidth
   *          the width of the data columns
   * @param atomIndexes
   *          an array either null or indicating exactly which atoms get the
   *          frequencies (used by CrystalReader)
   * @param minLineLen TODO
   * @throws Exception
   */
  protected void fillFrequencyData(int iAtom0, int atomCount,
                                   int modelAtomCount, boolean[] ignore,
                                   boolean isWide, int col0, int colWidth,
                                   int[] atomIndexes, int minLineLen) throws Exception {
    boolean withSymmetry = (modelAtomCount != atomCount);
    if (atomIndexes != null)
      atomCount = atomIndexes.length;
    int nLines = (isWide ? atomCount : atomCount * 3);
    int nFreq = ignore.length;
    String[][] data = new String[nLines][];
    fillDataBlockFixed(data, col0, colWidth, minLineLen);
    for (int i = 0, atomPt = 0; i < nLines; i++, atomPt++) {
      String[] values = data[i];
      String[] valuesY = (isWide ? null : data[++i]);
      String[] valuesZ = (isWide ? null : data[++i]);
      int dataPt = values.length - (isWide ? nFreq * 3 : nFreq) - 1;
      for (int j = 0, jj = 0; jj < nFreq; jj++) {
        ++dataPt;
        String x = values[dataPt];
        if (x.charAt(0) == ')') // AMPAC reader!
          x = x.substring(1);
        float vx = parseFloatStr(x);
        float vy = parseFloatStr(isWide ? values[++dataPt] : valuesY[dataPt]);
        float vz = parseFloatStr(isWide ? values[++dataPt] : valuesZ[dataPt]);
        if (ignore[jj])
          continue;
        int iAtom = (atomIndexes == null ? atomPt : atomIndexes[atomPt]);
        if (iAtom < 0)
          continue;
        if (Logger.debugging)
          Logger.debug("atom " + iAtom + " vib" + j + ": " + vx + " " + vy + " "
              + vz);
        atomSetCollection.addVibrationVectorWithSymmetry(iAtom0 + modelAtomCount * j++
            + iAtom, vx, vy, vz, withSymmetry);
      }
    }
  }

  protected String readLines(int nLines) throws Exception {
    for (int i = nLines; --i >= 0;)
      readLine();
    return line;
  }

  public String discardLinesUntilStartsWith(String startsWith)
      throws Exception {
    while (readLine() != null && !line.startsWith(startsWith)) {
    }
    return line;
  }

  public String discardLinesUntilContains(String containsMatch)
      throws Exception {
    while (readLine() != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  public String discardLinesUntilContains2(String s1, String s2)
      throws Exception {
    while (readLine() != null && line.indexOf(s1) < 0 && line.indexOf(s2) < 0) {
    }
    return line;
  }

  public String discardLinesUntilBlank() throws Exception {
    while (readLine() != null && line.trim().length() != 0) {
    }
    return line;
  }

  public String discardLinesUntilNonBlank() throws Exception {
    while (readLine() != null && line.trim().length() == 0) {
    }
    return line;
  }

  protected void checkLineForScript(String line) {
    this.line = line;
    checkCurrentLineForScript();
  }

  public void checkCurrentLineForScript() {
    if (line.indexOf("Jmol") >= 0) {
      if (line.indexOf("Jmol PDB-encoded data") >= 0) {
        atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolData", line);
        if (!line.endsWith("#noautobond"))
          line += "#noautobond";
      }
      if (line.indexOf("Jmol data min") >= 0) {
        Logger.info(line);
        // The idea here is to use a line such as the following:
        //
        // REMARK   6 Jmol data min = {-1 -1 -1} max = {1 1 1} 
        //                      unScaledXyz = xyz / {10 10 10} + {0 0 0} 
        //                      plotScale = {100 100 100}
        //
        // to pass on to Jmol how to graph non-molecular data. 
        // The format allows for the actual data to be linearly transformed
        // so that it fits into the PDB format for x, y, and z coordinates.
        // This adapter will then unscale the data and also pass on to
        // Jmol the unit cell equivalent that takes the actual data (which
        // will be considered the fractional coordinates) to Jmol coordinates,
        // which will be a cube centered at {0 0 0} and ranging from {-100 -100 -100}
        // to {100 100 100}.
        //
        // Jmol 12.0.RC23 uses this to pass through the adapter a quaternion,
        // ramachandran, or other sort of plot.

        float[] data = new float[15];
        parseStringInfestedFloatArray(line.substring(10).replace('=', ' ')
            .replace('{', ' ').replace('}', ' '), data);
        P3 minXYZ = P3.new3(data[0], data[1], data[2]);
        P3 maxXYZ = P3.new3(data[3], data[4], data[5]);
        fileScaling = P3.new3(data[6], data[7], data[8]);
        fileOffset = P3.new3(data[9], data[10], data[11]);
        P3 plotScale = P3.new3(data[12], data[13], data[14]);
        if (plotScale.x <= 0)
          plotScale.x = 100;
        if (plotScale.y <= 0)
          plotScale.y = 100;
        if (plotScale.z <= 0)
          plotScale.z = 100;
        if (fileScaling.y == 0)
          fileScaling.y = 1;
        if (fileScaling.z == 0)
          fileScaling.z = 1;
        setFractionalCoordinates(true);
        latticeCells = new int[3];
        atomSetCollection.xtalSymmetry = null;
        setUnitCell(plotScale.x * 2 / (maxXYZ.x - minXYZ.x), plotScale.y * 2
            / (maxXYZ.y - minXYZ.y), plotScale.z * 2
            / (maxXYZ.z == minXYZ.z ? 1 : maxXYZ.z - minXYZ.z), 90, 90, 90);
        unitCellOffset = P3.newP(plotScale);
        unitCellOffset.scale(-1);
        symmetry.toFractional(unitCellOffset, false);
        unitCellOffset.scaleAdd2(-1f, minXYZ, unitCellOffset);
        symmetry.setOffsetPt(unitCellOffset);
        atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolDataScaling",
            new P3[] { minXYZ, maxXYZ, plotScale });
      }
    }
    if (line.endsWith("#noautobond")) {
      line = line.substring(0, line.lastIndexOf('#')).trim();
      atomSetCollection.setNoAutoBond();
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

  private String previousScript;

  public void addJmolScript(String script) {
    Logger.info("#jmolScript: " + script);
    if (previousScript == null)
      previousScript = "";
    else if (!previousScript.endsWith(";"))
      previousScript += ";";
    previousScript += script;
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolscript",
        previousScript);
  }

  private String siteScript;

  protected void addSiteScript(String script) {
    if (siteScript == null)
      siteScript = "";
    else if (!siteScript.endsWith(";"))
      siteScript += ";";
    siteScript += script;
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("sitescript",
        siteScript);  // checked in ScriptEvaluator.load()
  }

  public String readLine() throws Exception {
    return RL();
  }
  
  public String RL() throws Exception {
    prevline = line;
    line = reader.readLine();
    if (out != null && line != null) {
      byte[] b = line.getBytes();
      out.write(b, 0, b.length);
      out.writeByteAsInt(0x0A);
    }
    ptLine++;
    if (Logger.debugging)
      Logger.debug(line);
    return line;
  }

  final static protected String[] getStrings(String sinfo, int nFields,
                                             int width) {
    String[] fields = new String[nFields];
    for (int i = 0, pt = 0; i < nFields; i++, pt += width)
      fields[i] = sinfo.substring(pt, pt + width);
    return fields;
  }

  // parser functions are static, so they need notstatic counterparts

  protected String[] getTokens() {
    return PT.getTokens(line);
  }

  protected void parseStringInfestedFloatArray(String s, float[] data) {
    Parser.parseStringInfestedFloatArray(s, null, data);
  }

  protected static float[] getTokensFloat(String s, float[] f, int n) {
    if (f == null)
      f = new float[n];
    PT.parseFloatArrayDataN(getTokensStr(s), f, n);
    return f;
  }

  public static String[] getTokensStr(String s) {
    return PT.getTokens(s);
  }

  protected static String[] getTokensAt(String s, int iStart) {
    return PT.getTokensAt(s, iStart);
  }

  protected float parseFloat() {
    return PT.parseFloatNext(line, next);
  }

  public float parseFloatStr(String s) {
    next[0] = 0;
    return PT.parseFloatNext(s, next);
  }

  protected float parseFloatRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseFloatRange(s, iEnd, next);
  }

  protected int parseInt() {
    return PT.parseIntNext(line, next);
  }

  public int parseIntStr(String s) {
    next[0] = 0;
    return PT.parseIntNext(s, next);
  }

  protected int parseIntAt(String s, int iStart) {
    next[0] = iStart;
    return PT.parseIntNext(s, next);
  }

  protected int parseIntRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseIntRange(s, iEnd, next);
  }

  protected String parseToken() {
    return PT.parseTokenNext(line, next);
  }

  protected String parseTokenStr(String s) {
    next[0] = 0;
    return PT.parseTokenNext(s, next);
  }

  protected String parseTokenNext(String s) {
    return PT.parseTokenNext(s, next);
  }

  protected String parseTokenRange(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return PT.parseTokenRange(s, iEnd, next);
  }

  protected static String parseTrimmedAt(String s, int iStart) {
    return PT.parseTrimmedAt(s, iStart);
  }

  protected static String parseTrimmedRange(String s, int iStart, int iEnd) {
    return PT.parseTrimmedRange(s, iStart, iEnd);
  }

  /**
   * get all integers after letters
   * negative entries are spaces (1Xn)
   * 
   * @param s
   * @return Vector of integers
   */
  protected static List<Integer> getFortranFormatLengths(String s) {
    List<Integer> vdata = new  List<Integer>();
    int n = 0;
    int c = 0;
    int factor = 1;
    boolean inN = false;
    boolean inCount = true;
    s += ",";
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      switch (ch) {
      case '.':
        inN = false;
        continue;
      case ',':
        for (int j = 0; j < c; j++)
          vdata.addLast(Integer.valueOf(n * factor));
        inN = false;
        inCount = true;
        c = 0;
        continue;
      case 'X':
        n = c;
        c = 1;
        factor = -1;
        continue;

      }
      boolean isDigit = Character.isDigit(ch);
      if (isDigit) {
        if (inN)
          n = n * 10 + ch - '0';
        else if (inCount)
          c = c * 10 + ch - '0';
      } else if (Character.isLetter(ch)) {
        n = 0;
        inN = true;
        inCount = false;
        factor = 1;
      } else {
        inN = false;
      }
    }
    return vdata;
  }

  /**
   * read three vectors, as for unit cube definitions
   * allows for non-numeric data preceding the number block
   * 
   * @param isBohr 
   * @return three vectors
   * @throws Exception 
   * 
   */
  protected V3[] read3Vectors(boolean isBohr) throws Exception {
    V3[] vectors = new V3[3];   
    float[] f = new float[3];
    for (int i = 0; i < 3; i++) {
      if (i > 0 || Float.isNaN(parseFloatStr(line))) {
        readLine();
        if (i == 0 && line != null) {
          i = -1;
          continue;
        }
      }
      fillFloatArray(line, 0, f);
      vectors[i] = new V3();
      vectors[i].setA(f);
      if (isBohr)
        vectors[i].scale(ANGSTROMS_PER_BOHR);
    }
    return vectors;
  }

  /**
   * allow 13C, 15N, 2H, etc. for isotopes
   *  
   * @param atom
   * @param str
   */
  protected void setElementAndIsotope(Atom atom, String str) {
    int isotope = parseIntStr(str);
    if (isotope == Integer.MIN_VALUE) {
      atom.elementSymbol = str;
    } else {
      str = str.substring(("" + isotope).length());
      atom.elementNumber = (short) (str.length() == 0 ? isotope
          : ((isotope << 7) + JmolAdapter.getElementNumber(str)));
    }
  }

  public void finalizeModelSet() {
    // PyMOL reader only
  }

  public void setChainID(Atom atom, char ch) {
    atom.chainID = viewer.getChainID("" + ch);    
  }

  public void setU(Atom atom, int i, float val) {
    // Ortep Type 8: D = 2pi^2, C = 2, a*b*
    float[] data = atomSetCollection.getAnisoBorU(atom);
    if (data == null)
      atomSetCollection.setAnisoBorU(atom, data = new float[8], 8);
    data[i] = val;
  }

}
