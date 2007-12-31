/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.api.JmolBioResolver;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.g3d.Graphics3D;
import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;

abstract public class ModelCollection extends BondCollection {

  /**
   * initial transfer of model data from old to new model set.
   * Note that all new models are added later, AFTER the old ones. This is 
   * very important, because all of the old atom numbers must map onto the
   * same numbers in the new model set, or the state script will not run
   * properly, among other problems.
   * 
   * We subclass these just for sanity sake.
   *  
   * @param modelSet
   */
  void merge(ModelSet modelSet) {
    for (int i = 0; i < modelSet.modelCount; i++) {
      models[i] = modelSet.models[i];
      models[i].bsAtoms = null;
      stateScripts = modelSet.stateScripts;
      thisStateModel = -1;
    }
    super.merge(modelSet);
  }

  protected void releaseModelSet() {
    /*
     * Probably unnecessary, but here for general accounting.
     * 
     * I added this when I was trying to track down a memory bug.
     * I know that you don't have to do this, but I was concerned
     * that somewhere in this mess was a reference to modelSet. 
     * As it turns out, it was in models[i] (which was not actually
     * nulled but, rather, transferred to the new model set anyway).
     * Quite amazing that that worked at all, really. Some models were
     * referencing the old modelset, some the new. Yeiks!
     * 
     * Bob Hanson 11/7/07
     * 
     */
    models = null;
    bsSymmetry = null;
    bsAll = null;
    cellInfos = null;
    withinModelIterator = null;
    withinAtomSetIterator = null;
    super.releaseModelSet();
  }

  protected String modelSetName;

  public String getModelSetName() {
    return modelSetName;
  }

  protected Model[] models = new Model[1];

  public Model[] getModels() {
    return models;
  }

  protected int modelCount;

  public int getModelCount() {
    return modelCount;
  }

  CellInfo[] cellInfos;

  public CellInfo[] getCellInfos() {
    return cellInfos;
  }
  
  public UnitCell getUnitCell(int modelIndex) {
    if (modelIndex < 0)
      return null;
    return (cellInfos == null ? null : cellInfos[modelIndex].getUnitCell());
  }

  int[] modelNumbers = new int[1];  // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  int[] modelFileNumbers = new int[1];  // file * 1000000 + modelInFile (1-based)
  String[] modelNumbersForAtomLabel = new String[1];
  String[] modelNames = new String[1];
  String[] frameTitles = new String[1];

  public String getModelName(int modelIndex) {
    return modelCount < 1 ? "" 
        : modelIndex >= 0 ? modelNames[modelIndex]
        : modelNumbersForAtomLabel[-1 - modelIndex];
  }

  public String getModelTitle(int modelIndex) {
    return (String) getModelAuxiliaryInfo(modelIndex, "title");
  }

  public String getModelFileName(int modelIndex) {
    return (String) getModelAuxiliaryInfo(modelIndex, "fileName");
  }

  public void setFrameTitle(int modelIndex, String title) {
    if (modelIndex >= 0 && modelIndex < modelCount)
      frameTitles[modelIndex] = title;
  }
  
  public String getFrameTitle(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ?
        frameTitles[modelIndex] : "");
  }
  
  public String getModelNumberForAtomLabel(int modelIndex) {
    return modelNumbersForAtomLabel[modelIndex];
  }

  
  
  protected BitSet[] elementsPresent;

  protected boolean isXYZ;
  protected boolean isPDB;

  Properties modelSetProperties;
  Hashtable modelSetAuxiliaryInfo;

  protected Group[] groups;
  protected int groupCount;
  protected int baseGroupIndex = 0;
  private int structureCount = 0;
  private Structure[] structures = new Structure[10];
  protected boolean haveBioClasses = true;
  protected JmolBioResolver jbr = null;
  protected BitSet structuresDefinedInFile = new BitSet();
  
  protected void calculatePolymers(BitSet alreadyDefined) {
    if (jbr == null)
      return;
    if (alreadyDefined != null) {
      jbr.clearBioPolymers(groups, groupCount, alreadyDefined);
    }
    for (int i = baseGroupIndex; i < groupCount; ++i) {
      Polymer bp = jbr.buildBioPolymer(groups[i], groups, i);
      if (bp != null)
        addBioPolymerToModel(bp, groups[i].getModel());
    }
  }

  protected void addBioPolymerToModel(Polymer polymer, Model model) {
    if (model.bioPolymers.length == 0 || polymer == null)
      model.bioPolymers = new Polymer[8];
    if (polymer == null) {
      model.bioPolymerCount = 0;
      return;
    }
    if (model.bioPolymerCount == model.bioPolymers.length)
      model.bioPolymers = (Polymer[])ArrayUtil.doubleLength(model.bioPolymers);
    model.bioPolymers[model.bioPolymerCount++] = polymer;
  }

  /**
   * deprecated due to multimodel issues, 
   * but required by an interface -- do NOT remove.
   * 
   * @return      just the first unit cell
   * 
   */
  public float[] getNotionalUnitcell() {
    return (cellInfos == null || cellInfos[0] == null ? null : cellInfos[0]
        .getNotionalUnitCell());
  }

  //new way:

  protected boolean someModelsHaveSymmetry;
  protected boolean someModelsHaveAromaticBonds;

  public boolean haveSymmetry() {
    return someModelsHaveSymmetry;
  }

  //note: Molecules is set up to only be calculated WHEN NEEDED
  protected Molecule[] molecules = new Molecule[4];
  protected int moleculeCount;

  private final Matrix3f matTemp = new Matrix3f();
  private final Matrix3f matInv = new Matrix3f();
  private final Point3f ptTemp = new Point3f();

  ////////////////////////////////////////////

  private final Point3f averageAtomPoint = new Point3f();
  private boolean isBbcageDefault;
  private BitSet bboxModels;
  private BitSet bboxAtoms;
  private final BoxInfo boxInfo = new BoxInfo();
  {
    boxInfo.setBbcage();
  }
  
  public Point3f getAverageAtomPoint() {
    return averageAtomPoint;
  }

  public Point3f getBoundBoxCenter() {
    return boxInfo.getBoundBoxCenter();
  }

  public Vector3f getBoundBoxCornerVector() {
    return boxInfo.getBoundBoxCornerVector();
  }

  public Point3f[] getBboxVertices() {
    return boxInfo.getBboxVertices();
  }

  public Hashtable getBoundBoxInfo() {
    return boxInfo.getBoundBoxInfo();
  }

  public BitSet getBoundBoxModels() {
    return bboxModels;
  }
  
  public void setBoundBox(Point3f pt1, Point3f pt2, boolean byCorner) {
    if (pt1.distance(pt2) == 0)
      return;
    isBbcageDefault = false;
    bboxModels = null;
    bboxAtoms = null;
    boxInfo.setBoundBox(pt1, pt2, byCorner);
  }

  public String getBoundBoxCommand(boolean withOptions) {
    if (!withOptions && bboxAtoms != null)
      return "boundbox " + Escape.escape(bboxAtoms);
    ptTemp.set(boxInfo.getBoundBoxCenter());
    Vector3f bbVector = boxInfo.getBoundBoxCornerVector();
    String s = (withOptions ? "boundbox " + Escape.escape(ptTemp) + " "
        + Escape.escape(bbVector) + "\n#or\n" : "");
    ptTemp.sub(bbVector);
    s += "boundbox corners " + Escape.escape(ptTemp) + " ";
    ptTemp.scaleAdd(2, bbVector, ptTemp);
    float v = Math.abs(8 * bbVector.x * bbVector.y * bbVector.z);
    s += Escape.escape(ptTemp) + " # volume = " + v;
    return s;
  }

  public void calcBoundBoxDimensions(BitSet bs) {
    if (BitSetUtil.firstSetBit(bs) < 0)
      bs = null;
    if (bs == null && isBbcageDefault || atomCount < 2)
      return;
    bboxModels = getModelBitSet(bboxAtoms = BitSetUtil.copy(bs));
    if (calcAtomsMinMax(bs, boxInfo) == atomCount)
      isBbcageDefault = true;
    if (bs == null) { // from modelLoader or reset
      averageAtomPoint.set(getAtomSetCenter(null));
      if (cellInfos != null)
        calcUnitCellMinMax();
    }
    boxInfo.setBbcage();
  }

  public BoxInfo getBoxInfo(BitSet bs) {
    if (bs == null)
      return boxInfo;
    BoxInfo bi = new BoxInfo();
    calcAtomsMinMax(bs, bi);
    bi.setBbcage();
    return bi;
  }

  private int calcAtomsMinMax(BitSet bs, BoxInfo boxInfo) {
    boxInfo.reset();
    int nAtoms = 0;
    for (int i = atomCount; --i >= 0;)
      if (bs == null || bs.get(i)) {
        nAtoms++;
        if (!isJmolDataFrame(atoms[i]))
          boxInfo.addBoundBoxPoint(atoms[i]);
      }
    return nAtoms;
  }

  private void calcUnitCellMinMax() {
    for (int i = 0; i < modelCount; i++) {
      if (!cellInfos[i].coordinatesAreFractional)
        continue;
      Point3f[] vertices = cellInfos[i].getUnitCell().getVertices();
      for (int j = 0; j < 8; j++)
        boxInfo.addBoundBoxPoint(vertices[j]);
    }
  }

  public float calcRotationRadius(BitSet bs) {
    //Eval getZoomFactor
    Point3f center = getAtomSetCenter(bs);
    float maxRadius = 0;
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i)) {
        Atom atom = atoms[i];
        float distAtom = center.distance(atom);
        float radiusVdw = atom.getVanderwaalsRadiusFloat();
        float outerVdw = distAtom + radiusVdw;
        if (outerVdw > maxRadius)
          maxRadius = outerVdw;
      }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  public Point3f getAtomSetCenter(BitSet bs) {
    Point3f ptCenter = new Point3f(0, 0, 0);
    if (bs == null || BitSetUtil.firstSetBit(bs) < 0)
      return ptCenter;
    int nPoints = 0;
    for (int i = atomCount; --i >= 0;) {
      if (bs == null || bs.get(i))
        if (!isJmolDataFrame(atoms[i])) {
          nPoints++;
          ptCenter.add(atoms[i]);
        }
    }
    ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue, float[] values) {
    super.setAtomProperty(bs, tok, iValue, fValue, values);
    if ((tok == Token.valence || tok == Token.formalCharge)
        && viewer.getSmartAromatic())
      assignAromaticBonds();
  }

  protected Vector stateScripts = new Vector();
  private int thisStateModel = 0;

  public void addStateScript(String script, boolean addFrameNumber) {
    if (addFrameNumber) {
      int iModel = viewer.getCurrentModelIndex();
      if (thisStateModel != iModel)
        script = "  frame "
            + (iModel < 0 ? "" + iModel : getModelNumberDotted(iModel)) + ";\n"
            + script;
      thisStateModel = iModel;
    } else {
      thisStateModel = -1;
    }
    stateScripts.addElement(script);
  }

  protected void defineStructure(int modelIndex, String structureType, char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID, int endSequenceNumber,
                       char endInsertionCode) {
    if (structureCount == structures.length)
      structures = (Structure[]) ArrayUtil.setLength(structures,
          structureCount + 10);
    structures[structureCount++] = new Structure(modelIndex, structureType,
        startChainID,
        Group.getSeqcode(startSequenceNumber, startInsertionCode), endChainID,
        Group.getSeqcode(endSequenceNumber, endInsertionCode));
  }

  /**
   * allows rebuilding of PDB structures;
   * also accessed by ModelManager from Eval
   * 
   * @param alreadyDefined    set to skip calculation
   * @param addFileData       in the case of loading, we add the PDB data
   *  
   */
  void calculateStructuresAllExcept(BitSet alreadyDefined, boolean addFileData) {
    freezeModels();
    for (int i = modelCount; --i >= 0;)
      if (models[i].isPDB && !alreadyDefined.get(i))
        models[i].calculateStructures();
     if (addFileData)
      propagateSecondaryStructure();
  }

  private void freezeModels() {
    for (int iModel = modelCount; --iModel >= 0;) {
      Model m = models[iModel];
      m.chains = (Chain[])ArrayUtil.setLength(m.chains, m.chainCount);
      m.groupCount = -1;
      m.getGroupCount();      
      for (int i = 0; i < m.chainCount; ++i)
        m.chains[i].groups = (Group[])ArrayUtil.setLength(m.chains[i].groups, groupCount);
      m.bioPolymers = (Polymer[])ArrayUtil.setLength(m.bioPolymers, m.bioPolymerCount);
      for (int i = m.bioPolymerCount; --i >= 0; ) {
        m.bioPolymers[i].freeze();
      }
    }
  }
  
  public BitSet setConformation(int modelIndex, BitSet bsConformation) {
    for (int i = modelCount; --i >= 0;)
      if (i == modelIndex || modelIndex < 0)
        models[i].setConformation(bsConformation);
    return bsConformation;
  }

  public BitSet setConformation(int modelIndex, int conformationIndex) {
    BitSet bs = new BitSet();
    String altLocs = getAltLocListInModel(modelIndex);
    if (altLocs.length() > 0) {
      BitSet bsConformation = getModelAtomBitSet(modelIndex, true);
      if (conformationIndex >= 0)
        for (int c = models[modelIndex].nAltLocs; --c >= 0;)
          if (c != conformationIndex)
            BitSetUtil.andNot(bsConformation, getSpecAlternate(altLocs
                .substring(c, c + 1)));
      if (BitSetUtil.length(bsConformation) > 0) {
        setConformation(modelIndex, bsConformation);
        bs.or(bsConformation);
      }
    }
    return bs;
  }

  public Hashtable getHeteroList(int modelIndex) {
    Hashtable htFull = new Hashtable();
    boolean ok = false;
    for (int i = modelCount; --i >= 0;)
      if (modelIndex < 0 || i == modelIndex) {
        Hashtable ht = (Hashtable) getModelAuxiliaryInfo(i, "hetNames");
        if (ht == null)
          continue;
        ok = true;
        Enumeration e = ht.keys();
        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          htFull.put(key, ht.get(key));
        }
      }
    return (ok ? htFull : (Hashtable) getModelSetAuxiliaryInfo("hetNames"));
  }

  void setModelSetProperties(Properties modelSetProperties) {
    this.modelSetProperties = modelSetProperties;
  }

  void setModelSetAuxiliaryInfo(Hashtable modelSetAuxiliaryInfo) {
    this.modelSetAuxiliaryInfo = modelSetAuxiliaryInfo;
  }

  public Properties getModelSetProperties() {
    return modelSetProperties;
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return modelSetAuxiliaryInfo;
  }

  public String getModelSetProperty(String propertyName) {
    return (modelSetProperties == null ? null : modelSetProperties
        .getProperty(propertyName));
  }

  public Object getModelSetAuxiliaryInfo(String keyName) {
    return (modelSetAuxiliaryInfo == null ? null : modelSetAuxiliaryInfo
        .get(keyName));
  }

  boolean getModelSetAuxiliaryInfoBoolean(String keyName) {
    return (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName) && ((Boolean) modelSetAuxiliaryInfo
        .get(keyName)).booleanValue());
  }

  int getModelSetAuxiliaryInfoInt(String keyName) {
    if (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName)) {
      return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  protected Vector trajectories;

  protected int getTrajectoryCount() {
    return (trajectories == null ? 0 : trajectories.size());
  }

  public int getTrajectoryIndex(int modelIndex) {
    return models[modelIndex].trajectoryBaseIndex;
  }
  
  public boolean isTrajectory(int modelIndex) {
    return models[modelIndex].isTrajectory;
  }
  
  public boolean isTrajectory(int[] countPlusIndices) {
    if (countPlusIndices == null)
      return false;
    int count = countPlusIndices[0];
    for (int i = count; --i >= 0;) {
      if (countPlusIndices[i + 1] < 0)
        return false;
      if (models[atoms[countPlusIndices[i + 1]].modelIndex].isTrajectory)
        return true;
      }
    return false;
  }

  /** 
   * only some models can be iterated through.
   * models for which trajectoryBaseIndexes[i] != i are trajectories only
   * 
   * @param allowJmolData
   * @return  bitset of models
   */
  public BitSet getIterativeModels(boolean allowJmolData) {
    BitSet bs = new BitSet();
    for (int i = 0; i < modelCount; i++) {
      if (!allowJmolData && isJmolDataFrame(i))
        continue;
      if (models[i].trajectoryBaseIndex == i)
        bs.set(i);      
    }
    return bs;
  }
  
  public void selectDisplayedTrajectories(BitSet bs) {
    //when a trajectory is selected, the atom's modelIndex is
    //switched to that of the selected trajectory
    //even though the underlying model itself is not changed.
    for (int i = 0; i < modelCount; i++) {
      if (models[i].isTrajectory && atoms[models[i].firstAtomIndex].modelIndex != i)
        bs.clear(i);
    }
  }
  
  public String getModelNumberDotted(int modelIndex) {
    return (modelCount < 1 || modelIndex < 0 ? "" : 
      Escape.escapeModelFileNumber(modelFileNumbers[modelIndex]));
  }

  public int getModelNumber(int modelIndex) {
    return modelNumbers[modelIndex];
  }

  public int getModelFileNumber(int modelIndex) {
    return modelFileNumbers[modelIndex];
  }

  public Properties getModelProperties(int modelIndex) {
    return models[modelIndex].properties;
  }

  public String getModelProperty(int modelIndex, String property) {
    Properties props = models[modelIndex].properties;
    return props == null ? null : props.getProperty(property);
  }

  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : models[modelIndex].auxiliaryInfo);
  }

  public void setModelAuxiliaryInfo(int modelIndex, Object key, Object value) {
    models[modelIndex].auxiliaryInfo.put(key, value);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String key) {
    if (modelIndex < 0)
      return null;
    return models[modelIndex].auxiliaryInfo.get(key);
  }

  protected boolean getModelAuxiliaryInfoBoolean(int modelIndex, String keyName) {
    Hashtable info = models[modelIndex].auxiliaryInfo;
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  protected int getModelAuxiliaryInfoInt(int modelIndex, String keyName) {
    Hashtable info = models[modelIndex].auxiliaryInfo;
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  Model getModel(int modelIndex) {
    return models[modelIndex];
  }

  public int getInsertionCountInModel(int modelIndex) {
    return models[modelIndex].nInsertions;
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  

    int file = (int) (fDotM);
    int model = (int) ((fDotM - file + 0.00001) * 10000);
    while (model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }

  public int getAltLocCountInModel(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }

  private void propagateSecondaryStructure() {
    // issue arises with multiple file loading and multi-_data  mmCIF files
    // that structural information may be model-specific
    // but for PDB files it is not. So we don't assign -1 for 
    // structure modelIndex anymore in PDB files.
    for (int i = structureCount; --i >= 0;) {
      Structure structure = structures[i];
      models[structure.modelIndex].addSecondaryStructure(structure.type,
          structure.startChainID, structure.startSeqcode, structure.endChainID,
          structure.endSeqcode);
    }
  }

  public int getChainCount() {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0;)
      chainCount += models[i].getChainCount();
    return chainCount;
  }

  public int getBioPolymerCount() {
    int polymerCount = 0;
    for (int i = modelCount; --i >= 0;)
      if (!models[i].isTrajectory || models[i].trajectoryBaseIndex == i)
        polymerCount += models[i].getBioPolymerCount();
    return polymerCount;
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getBioPolymerCount();
    if (models[modelIndex].isTrajectory && models[modelIndex].trajectoryBaseIndex != modelIndex)
      return 0;
    return models[modelIndex].getBioPolymerCount();
  }

  public void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    boolean isTraceAlpha = viewer.getTraceAlpha();
    float sheetSmoothing = viewer.getSheetSmoothing();
    int last = Integer.MAX_VALUE - 1;
    for (int i = 0; i < modelCount; ++i) {
      int polymerCount = models[i].getBioPolymerCount();
      for (int ip = 0; ip < polymerCount; ip++)
        last = models[i].getBioPolymer(ip)
            .getPolymerPointsAndVectors(last, bs, vList, isTraceAlpha, sheetSmoothing);
    }
  }
  
  public void recalculateLeadMidpointsAndWingVectors(int modelIndex) {
    if (modelIndex < 0) {
      for (int i = 0; i < modelCount; i++)
        recalculateLeadMidpointsAndWingVectors(i);
      return;
    }
    int polymerCount = models[modelIndex].getBioPolymerCount();
    for (int ip = 0; ip < polymerCount; ip++)
      models[modelIndex].getBioPolymer(ip)
          .recalculateLeadMidpointsAndWingVectors();
  }
  
  public Point3f[] getPolymerLeadMidPoints(int iModel, int iPolymer) {
    return models[iModel].getBioPolymer(iPolymer).getLeadMidpoints();
  }

  public int getChainCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getChainCount();
    return models[modelIndex].getChainCount();
  }

  public int getGroupCount() {
    int groupCount = 0;
    for (int i = modelCount; --i >= 0;)
      groupCount += models[i].getGroupCount();
    return groupCount;
  }

  public int getGroupCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getGroupCount();
    return models[modelIndex].getGroupCount();
  }

  public void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedGroupsCount(bsSelected);
  }

  public void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = modelCount; --i >= 0;)
      models[i].calcSelectedMonomersCount(bsSelected);
  }

  public void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    //bsA and bsB are always the same. If that changes, we must
    //pass on bsB to clearCalculatedHydrogenBonds as well;
    for (int i = modelCount; --i >= 0;)
      if (models[i].trajectoryBaseIndex == i) {
        clearCalculatedHydrogenBonds(i, bsA);
        models[i].calcHydrogenBonds(bsA, bsB);
      }
  }

  static class Structure {
    String typeName;
    byte type;
    char startChainID;
    int startSeqcode;
    char endChainID;
    int endSeqcode;
    int modelIndex;

    Structure(int modelIndex, String typeName, char startChainID,
        int startSeqcode, char endChainID, int endSeqcode) {
      this.modelIndex = modelIndex;
      this.typeName = typeName;
      this.startChainID = startChainID;
      this.startSeqcode = startSeqcode;
      this.endChainID = endChainID;
      this.endSeqcode = endSeqcode;
      if ("helix".equals(typeName))
        type = JmolConstants.PROTEIN_STRUCTURE_HELIX;
      else if ("sheet".equals(typeName))
        type = JmolConstants.PROTEIN_STRUCTURE_SHEET;
      else if ("turn".equals(typeName))
        type = JmolConstants.PROTEIN_STRUCTURE_TURN;
      else
        type = JmolConstants.PROTEIN_STRUCTURE_NONE;
    }

    /*    Hashtable toHashtable() {
     Hashtable info = new Hashtable();
     info.put("type", typeName);
     info.put("startChainID", startChainID + "");
     info.put("startSeqcode", new Integer(startSeqcode));
     info.put("endChainID", endChainID + "");
     info.put("endSeqcode", new Integer(endSeqcode));
     return info;
     }
     */
  }

  /*  Vector getStructureInfo() {
   Vector info = new Vector();
   for (int i = 0; i < structureCount; i++)
   info.addElement(structures[i].toHashtable());
   return info;
   }
   */
  /* ONLY from one model 
   * 
   */
  /* ONLY from one model 
   * 
   */
  private String getPdbData(String type, char ctype, int modelIndex,
                            boolean isDerivative) {
    StringBuffer pdbCONECT = new StringBuffer();
    if (isJmolDataFrame(modelIndex))
      modelIndex = getJmolDataSourceFrame(modelIndex);
    if (modelIndex < 0)
      return "";
    int nPoly = models[modelIndex].getBioPolymerCount();
    StringBuffer pdbATOM = new StringBuffer();
    BitSet bsAtoms = getModelAtomBitSet(modelIndex, false);
    for (int p = 0; p < nPoly; p++) {
      Model model = models[modelIndex];
      for (int mp = 0; mp < model.bioPolymerCount; mp++)
        model.bioPolymers[mp].getPdbData(ctype, isDerivative, bsAtoms, pdbATOM,
            pdbCONECT);
    }
    pdbATOM.append(pdbCONECT);
    return getProteinStructureState(bsAtoms, ctype == 'r') + pdbATOM.toString();
  }

  /* **********************
   * 
   * Jmol Data Frame methods
   * 
   *****************************/

  public String getPdbData(int modelIndex, String type) {
    if (!models[modelIndex].isPDB)
      return null;
    char ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type
        .charAt(11) : 'r');
    String s = getPdbData(type, ctype, modelIndex, (type.indexOf(" deriv") >= 0));
    if (s.length() == 0)
      return "";
    String remark = "REMARK   6 Jmol PDB-encoded data: " + type
        + " data(x,y,z,charge)=";
    switch (ctype) {
    case 'w':
      remark += "(x,y,z,w)";
      break;
    case 'x':
      remark += "(y,z,w,x)";
      break;
    case 'y':
      remark += "(z,w,x,y)";
      break;
    case 'z':
      remark += "(w,x,y,z)";
      break;
    case 'r':
      remark += "(phi,psi,omega,partialCharge)";
    }
    remark += "\n";
    return remark + s;
  }

  public boolean isJmolDataFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount && models[modelIndex].jmolData != null);
  }
  
  private boolean isJmolDataFrame(Atom atom) {
    return (models[atom.modelIndex].jmolData != null);
  }

  public void setJmolDataFrame(String type, int modelIndex, int modelDataIndex) {
    Model model = models[type == null ? models[modelDataIndex].dataSourceFrame
        : modelIndex];
    if (type == null) {
      //leaving a data frame -- just set generic to this one if quaternion
      type = models[modelDataIndex].jmolFrameType;
    }
    if (modelIndex >= 0) {
      if (model.dataFrames == null)
        model.dataFrames = new Hashtable();
      models[modelDataIndex].dataSourceFrame = modelIndex;
      models[modelDataIndex].jmolFrameType = type;
      model.dataFrames.put(type, new Integer(modelDataIndex));
    }
    if (type.indexOf(" ") > 0) { //generic quaternion
      type = type.substring(0, type.indexOf(" "));
      model.dataFrames.put(type, new Integer(modelDataIndex));
    }
  }

  public int getJmolDataFrameIndex(int modelIndex, String type) {
    if (models[modelIndex].dataFrames == null)
      return -1;
    Integer index = (Integer) models[modelIndex].dataFrames.get(type);
    return (index == null ? -1 : index.intValue());
  }

  public String getJmolFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? 
        models[modelIndex].jmolFrameType : "modelSet");
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? 
        models[modelIndex].dataSourceFrame : -1);
  }
  
  private String pdbHeader;
  /*
   final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
   "MODEL ", "SCALE",  "HETATM", "SEQRES",
   "DBREF ", };
   */

  final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  private String getFullPDBHeader() {
    String info = (pdbHeader == null ? (pdbHeader = viewer
        .getCurrentFileAsString()) : pdbHeader);
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0 : info.indexOf("\n"
          + strRecord))) {
      case -1:
        continue;
      case 0:
        return "";
      default:
        if (ichFound < ichMin)
          ichMin = ++ichFound;
      }
    }
    return info.substring(0, ichMin);
  }

  public String getPDBHeader() {
    return (isPDB ? getFullPDBHeader() : getFileHeader());
  }

  public String getFileHeader() {
    if (isPDB)
      return getFullPDBHeader();
    String info = getModelSetProperty("fileHeader");
    if (info == null)
      info = modelSetName;
    if (info != null)
      return info;
    return "no header information found";
  }

  public Hashtable getModelInfo() {
    Hashtable info = new Hashtable();
    info.put("modelSetName", modelSetName);
    info.put("modelCount", new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", Boolean
        .valueOf(modelSetHasVibrationVectors()));
    if (modelSetProperties != null)
      info.put("modelSetProperties", modelSetProperties);
    Vector vModels = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt", new Integer(i));
      model.put("num", new Integer(getModelNumber(i)));
      model.put("file_model", getModelNumberDotted(i));
      model.put("name", getModelName(i));
      String s = getModelTitle(i);
      if (s != null)
        model.put("title", s);
      s = getModelFileName(i);
      if (s != null)
        model.put("file", s);
      model.put("vibrationVectors", Boolean
          .valueOf(modelHasVibrationVectors(i)));
      model.put("atomCount", new Integer(getAtomCountInModel(i)));
      model.put("bondCount", new Integer(getBondCountInModel(i)));
      model.put("groupCount", new Integer(getGroupCountInModel(i)));
      model.put("polymerCount", new Integer(models[i].getBioPolymerCount()));
      model.put("chainCount", new Integer(getChainCountInModel(i)));
      if (models[i].properties != null)
        model.put("modelProperties", models[i].properties);
      vModels.addElement(model);
    }
    info.put("models", vModels);
    return info;
  }

  //////////////  individual models ////////////////

  public int getAltLocIndexInModel(int modelIndex, char alternateLocationID) {
    if (alternateLocationID == '\0')
      return 0;
    String altLocList = getAltLocListInModel(modelIndex);
    if (altLocList.length() == 0)
      return 0;
    return altLocList.indexOf(alternateLocationID) + 1;
  }

  public int getInsertionCodeIndexInModel(int modelIndex, char insertionCode) {
    if (insertionCode == '\0')
      return 0;
    String codeList = getInsertionListInModel(modelIndex);
    if (codeList.length() == 0)
      return 0;
    return codeList.indexOf(insertionCode) + 1;
  }

  public String getAltLocListInModel(int modelIndex) {
    if (modelIndex < 0)
      return "";
    String str = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
    return (str == null ? "" : str);
  }

  private String getInsertionListInModel(int modelIndex) {
    String str = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
    return (str == null ? "" : str);
  }

  protected String getModelSymmetryList(int modelIndex) {
    if (cellInfos == null || cellInfos[modelIndex] == null)
      return "";
    String[] list = cellInfos[modelIndex].symmetryOperations;
    String str = "";
    if (list != null)
      for (int i = 0; i < list.length; i++)
        str += "\n" + list[i];
    return str;
  }

  public int getModelSymmetryCount(int modelIndex) {
    return (cellInfos == null || cellInfos[modelIndex] == null ? 0
        : cellInfos[modelIndex].symmetryOperations.length);
  }

  public int[] getModelCellRange(int modelIndex) {
    if (cellInfos == null)
      return null;
    return cellInfos[modelIndex].getCellRange();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    if (vibrationVectors != null)
      for (int i = atomCount; --i >= 0;)
        if ((modelIndex < 0 || atoms[i].modelIndex == modelIndex)
            && vibrationVectors[i] != null && vibrationVectors[i].length() > 0)
          return true;
    return false;
  }

  public BitSet getElementsPresentBitSet(int modelIndex) {
    if (modelIndex >= 0)
      return elementsPresent[modelIndex];
    BitSet bs = new BitSet();
    for (int i = 0; i < modelCount; i++)
      bs.or(elementsPresent[i]);
    return bs;
  }

  String getSymmetryInfoAsString(int modelIndex) {
    if (cellInfos == null)
      return "no symmetry information";
    return cellInfos[modelIndex].symmetryInfoString;
  }

  public void toCartesian(int modelIndex, Point3f pt) {
    if (modelIndex < 0)
      modelIndex = 0;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    cellInfos[modelIndex].toCartesian(pt);
    //String str = "Frame convertFractional " + pt + "--->";
    //Logger.info(str + pt);
  }

  public void toUnitCell(int modelIndex, Point3f pt, Point3f offset) {
    if (modelIndex < 0)
      return;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    cellInfos[modelIndex].toUnitCell(pt, offset);
  }

  public void toFractional(int modelIndex, Point3f pt) {
    if (modelIndex < 0)
      return;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    cellInfos[modelIndex].toFractional(pt);
  }

  public Point3f getUnitCellOffset(int modelIndex) {
    // from "unitcell {i j k}" via uccage
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return null;
    return unitCell.getCartesianOffset();
  }

  public boolean setUnitCellOffset(int modelIndex, Point3f pt) {
    // from "unitcell {i j k}" via uccage
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return false;
    unitCell.setOffset(pt);
    return true;
  }

  public boolean setUnitCellOffset(int modelIndex, int nnn) {
    UnitCell unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return false;
    unitCell.setOffset(nnn);
    return true;
  }
  
  ///////// molecules /////////

  private BitSet bsTemp = new BitSet();

  public Vector getMoleculeInfo(BitSet bsAtoms) {
    if (moleculeCount == 0)
      getMolecules();
    Vector V = new Vector();
    for (int i = 0; i < moleculeCount; i++) {
      bsTemp = BitSetUtil.copy(bsAtoms);
      bsTemp.and(molecules[i].atomList);
      if (BitSetUtil.length(bsTemp) > 0)
        V.addElement(molecules[i].getInfo());
    }
    return V;
  }

  public int getMoleculeIndex(int atomIndex) {
    //ColorManager
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++) {
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].indexInModel;
    }
    return 0;
  }
  /*
   int getFirstMoleculeIndexInModel(int modelIndex) {
   if (moleculeCount == 0)
   getMolecules();
   return getModel(modelIndex).firstMolecule;
   }
   */

  public void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate,
                             BitSet bsInput, boolean fullMolecule,
                             boolean isInternal) {
    bspf = null;
    BitSet bs = (fullMolecule ? getMoleculeBitSet(bsInput) : bsInput);
    matInv.set(matrixRotate);
    matInv.invert();
    ptTemp.set(0, 0, 0);
    matTemp.mul(mNew, matrixRotate);
    matTemp.mul(matInv, matTemp);
    int n = 0;
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i)) {
        ptTemp.add(atoms[i]);
        matTemp.transform(atoms[i]);
        ptTemp.sub(atoms[i]);
        taint(i, TAINT_COORD);
        n++;
      }
    if (!isInternal) {
      ptTemp.scale(1f / n);
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i))
          atoms[i].add(ptTemp);
    }
    recalculateLeadMidpointsAndWingVectors(-1);
  }

  public BitSet getMoleculeBitSet(BitSet bs) {
    // returns cumulative sum of all atoms in molecules containing these atoms
    if (moleculeCount == 0)
      getMolecules();
    BitSet bsResult = BitSetUtil.copy(bs);
    BitSet bsInitial = BitSetUtil.copy(bs);
    int iLastBit;
    while ((iLastBit = BitSetUtil.length(bsInitial)) > 0) {
      bsTemp = getMoleculeBitSet(iLastBit - 1);
      BitSetUtil.andNot(bsInitial, bsTemp);
      bsResult.or(bsTemp);
    }
    return bsResult;
  }

  public BitSet getMoleculeBitSet(int atomIndex) {
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++)
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].atomList;
    return null;
  }

  public void invertSelected(Point3f pt, Point4f plane, BitSet bs) {
    bspf = null;
    if (pt != null) {
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          float x = (pt.x - atoms[i].x) * 2;
          float y = (pt.y - atoms[i].y) * 2;
          float z = (pt.z - atoms[i].z) * 2;
          setAtomCoordRelative(i, x, y, z);
        }
      return;
    }
    // ax + by + cz + d = 0
    Vector3f norm = new Vector3f(plane.x, plane.y, plane.z);
    norm.normalize();
    float d = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z
        * plane.z);
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i)) {
        float twoD = -Graphics3D.distanceToPlane(plane, d, atoms[i]) * 2;
        float x = norm.x * twoD;
        float y = norm.y * twoD;
        float z = norm.z * twoD;
        setAtomCoordRelative(i, x, y, z);
      }
  }

  public Vector3f getModelDipole(int modelIndex) {
    if (modelIndex < 0)
      return null;
    Vector3f dipole = (Vector3f) getModelAuxiliaryInfo(modelIndex, "dipole");
    if (dipole == null)
      dipole = (Vector3f) getModelAuxiliaryInfo(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

  ////////// molecules /////////////

  public Vector3f calculateMolecularDipole(int modelIndex) {
    if (partialCharges == null || modelIndex < 0)
      return null;
    int nPos = 0;
    int nNeg = 0;
    float cPos = 0;
    float cNeg = 0;
    Vector3f pos = new Vector3f();
    Vector3f neg = new Vector3f();
    for (int i = 0; i < atomCount; i++) {
      if (atoms[i].modelIndex != modelIndex)
        continue;
      float c = partialCharges[i];
      if (c < 0) {
        nNeg++;
        cNeg += c;
        neg.scaleAdd(c, atoms[i], neg);
      } else if (c > 0) {
        nPos++;
        cPos += c;
        pos.scaleAdd(c, atoms[i], pos);
      }
    }
    if (nNeg == 0 || nPos == 0)
      return null;
    pos.scale(1f/cPos);
    neg.scale(1f/cNeg);
    //System.out.print("draw ptn {" + neg.x + " " + neg.y + " " + neg.z + "};draw ptp {" + pos.x + " " + pos.y + " " + pos.z + " }#" + cNeg + " " + cPos+" ");
    pos.sub(neg);
    //System.out.println(pos.length() + " " + cPos);
    Logger.warn(" this is an untested result -- needs checking");
    pos.scale(cPos * 4.8f); //1e-10f * 1.6e-19f/ 3.336e-30f;
    
    // SUM_Q[(SUM_pos Q_iRi) / SUM_Q   -  (SUM_neg Q_iRi) / (-SUM_Q) ]    
    // this is really just SUM_i (Q_iR_i). Don't know why that would not work. 
    
    
    //http://www.chemistry.mcmaster.ca/esam/Chapter_7/section_3.html
    // 1 Debye = 3.336e-30 Coulomb-meter; C_e = 1.6022e-19 C
    return pos;
  }
  
  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < modelCount; i++) {
      if (modelIndex == i || modelIndex < 0)
        n += models[i].moleculeCount;
    }
    return n;
  }

  private BitSet selectedMolecules = new BitSet();
  private int selectedMoleculeCount;

  public void calcSelectedMoleculesCount(BitSet bsSelected) {
    if (moleculeCount == 0)
      getMolecules();
    selectedMolecules.xor(selectedMolecules);
    selectedMoleculeCount = 0;
    for (int i = 0; i < moleculeCount; i++) {
      BitSetUtil.copy(bsSelected, bsTemp);
      bsTemp.and(molecules[i].atomList);
      if (BitSetUtil.length(bsTemp) > 0) {
        selectedMolecules.set(i);
        selectedMoleculeCount++;
      }
    }
  }


  private void getMolecules() {
    if (moleculeCount > 0)
      return;
    if (molecules == null)
      molecules = new Molecule[4];
    moleculeCount = 0;
    BitSet atomlist = new BitSet(atomCount);
    BitSet bs = new BitSet(atomCount);
    int thisModelIndex = -1;
    int modelIndex = -1;
    int indexInModel = -1;
    int moleculeCount0 = -1;
    for (int i = 0; i < atomCount; i++)
      if (!atomlist.get(i) && !bs.get(i)) {
        modelIndex = atoms[i].modelIndex;
        if (modelIndex != thisModelIndex) {
          indexInModel = -1;
          models[modelIndex].firstMolecule = moleculeCount;
          moleculeCount0 = moleculeCount - 1;
          thisModelIndex = modelIndex;
        }
        indexInModel++;
        bs = getConnectedBitSet(i);
        atomlist.or(bs);
        if (moleculeCount == molecules.length)
          molecules = (Molecule[]) ArrayUtil.setLength(molecules,
              moleculeCount * 2);
        molecules[moleculeCount] = new Molecule((ModelSet) this, moleculeCount,
            bs, thisModelIndex, indexInModel);
        getModel(thisModelIndex).moleculeCount = moleculeCount - moleculeCount0;
        moleculeCount++;
      }
  }

  private BitSet getConnectedBitSet(int atomIndex) {
    BitSet bs = new BitSet(atomCount);
    BitSet bsToTest = getModelAtomBitSet(atoms[atomIndex].modelIndex, true);
    getCovalentlyConnectedBitSet(atoms[atomIndex], bs, bsToTest);
    return bs;
  }

  private void getCovalentlyConnectedBitSet(Atom atom, BitSet bs,
                                            BitSet bsToTest) {
    int atomIndex = atom.atomIndex;
    if (!bsToTest.get(atomIndex))
      return;
    bsToTest.clear(atomIndex);
    bs.set(atomIndex);
    if (atom.bonds == null)
      return;
    for (int i = atom.bonds.length; --i >= 0;) {
      Bond bond = atom.bonds[i];
      if ((bond.order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
        continue;
      if (bond.atom1 == atom) {
        getCovalentlyConnectedBitSet(bond.atom2, bs, bsToTest);
      } else {
        getCovalentlyConnectedBitSet(bond.atom1, bs, bsToTest);
      }
    }
  }

  public boolean hasCalculatedHBonds(BitSet bsAtoms) {
    for (int i = atomCount; --i >= 0;) {
      if (bsAtoms.get(i) && models[atoms[i].modelIndex].hasCalculatedHBonds)
        return true;
      i = models[atoms[i].modelIndex].firstAtomIndex;
    }
    return false;
  }

  public void clearCalculatedHydrogenBonds(int baseIndex, BitSet bsAtoms) {
    BitSet bsDelete = new BitSet();
    int nDelete = 0;
    models[baseIndex].hasCalculatedHBonds = false;
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (baseIndex >= 0 
           && models[bond.atom1.modelIndex].trajectoryBaseIndex != baseIndex
          || (bond.order & JmolConstants.BOND_HBOND_CALC) == 0)
        continue;
      if (bsAtoms != null && !bsAtoms.get(bond.atom1.atomIndex)) {
        models[baseIndex].hasCalculatedHBonds = true;
        continue;
      }
      bsDelete.set(i);
      nDelete++;
    }        
    if (nDelete > 0)
      deleteBonds(bsDelete);
  }


  //////////// iterators //////////
  
  //private final static boolean MIX_BSPT_ORDER = false;

  protected void initializeBspf() {
    if (bspf == null) {
      long timeBegin = 0;
      if (showRebondTimes)
        timeBegin = System.currentTimeMillis();
      bspf = new Bspf(3);
      /*      if (MIX_BSPT_ORDER) {
       Logger.debug("mixing bspt order");
       int stride = 3;
       int step = (atomCount + stride - 1) / stride;
       for (int i = 0; i < step; ++i)
       for (int j = 0; j < stride; ++j) {
       int k = i * stride + j;
       if (k >= atomCount)
       continue;
       Atom atom = atoms[k];
       bspf.addTuple(atom.modelIndex, atom);
       }
       } else {
       */
      Logger.debug("sequential bspt order");
      for (int i = atomCount; --i >= 0;) {
        // important that we go backward here, because we are going to 
        // use System.arrayCopy to expand the array ONCE only
        Atom atom = atoms[i];
        bspf.addTuple(models[atom.modelIndex].trajectoryBaseIndex, atom);
      }
      //      }
      if (showRebondTimes) {
        long timeEnd = System.currentTimeMillis();
        Logger.debug("time to build bspf=" + (timeEnd - timeBegin) + " ms");
        bspf.stats();
        //        bspf.dump();
      }
    }
  }

  protected void initializeBspt(int modelIndex) {
    if (bspf.isInitialized(modelIndex))
      return;
    bspf.initialize(modelIndex, atoms, getModelAtomBitSet(modelIndex, false));
  }
 
  private AtomIteratorWithinModel withinModelIterator;
  private AtomIteratorWithinSet withinAtomSetIterator;

  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased) {
    //EnvelopeCalculation, IsoSolventReader
    // This iterator returns only atoms OTHER than the atom specified
    // and with the specified restrictions. 
    // Model zero-based means the index returned is within the model, 
    // not the full atom set.
    
    initializeBspf();
    int modelIndex = models[atoms[atomIndex].modelIndex].trajectoryBaseIndex;
    initializeBspt(modelIndex);
    if (withinAtomSetIterator == null)
      withinAtomSetIterator = new AtomIteratorWithinSet();
    withinAtomSetIterator.initialize(bspf, modelIndex, atomIndex, 
        atoms[atomIndex], distance, bsSelected, isGreaterOnly, 
       (modelZeroBased ? models[modelIndex].firstAtomIndex : 0));
    return withinAtomSetIterator;
  }
  
  public AtomIndexIterator getWithinModelIterator(Atom atomCenter, float radius) {
    //Polyhedra, within(distance, atom)
    return getWithinModelIterator(atomCenter.modelIndex, atomCenter, radius);
  }

  private AtomIndexIterator getWithinModelIterator(int modelIndex, Point3f center, float radius) {
    //polyhedra, within(distance, atom), within(distance, point)
    initializeBspf();
    modelIndex = models[modelIndex].trajectoryBaseIndex;
    initializeBspt(modelIndex);
    if (withinModelIterator == null)
      withinModelIterator = new AtomIteratorWithinModel();
    withinModelIterator.initialize(bspf, modelIndex, center, radius);
    return withinModelIterator;
  }

  ////////// bonds /////////

  public int getBondCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return bondCount;
    int n = models[modelIndex].bondCount;
    if (n >= 0)
      return n;
    return (models[modelIndex].bondCount = super.getBondCountInModel(modelIndex)); 
  }

  ////////// atoms /////////

  public void setAtomCoordRelative(Point3f offset, BitSet bs) {
    setAtomCoordRelative(bs, offset.x, offset.y, offset.z);
    recalculateLeadMidpointsAndWingVectors(-1);
  }

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    super.setAtomCoord(bs, tokType, xyzValues);
    recalculateLeadMidpointsAndWingVectors(-1);
  }

  public int getAtomCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return atomCount;
    int n = models[modelIndex].atomCount;
    if (n >= 0)
      return n;
    return (models[modelIndex].atomCount = super.getAtomCountInModel(modelIndex)); 
  }
  
  private BitSet bsAll;
  
  /**
   * 
   * @param modelIndex
   * @param asCopy     MUST BE TRUE IF THE BITSET IS GOING TO BE MODIFIED!
   * @return either the actual bitset or a copy
   */
  public BitSet getModelAtomBitSet(int modelIndex, boolean asCopy) {
    BitSet bs = (modelIndex < 0 ? bsAll : models[modelIndex].bsAtoms);
    if (bs == null) {
      if (modelIndex < 0) {
        bs = bsAll = BitSetUtil.setAll(atomCount);
      } else {
        bs = new BitSet();
        for (int i = 0; i < atomCount; i++)
          if (atoms[i].modelIndex == modelIndex)
            bs.set(i);
        models[modelIndex].bsAtoms = bs;
      }
    }
    return (asCopy ? BitSetUtil.copy(bs) : bs);
  }

  /**
   * general unqualified lookup of atom set type
   * @param tokType
   * @return BitSet; or null if we mess up the type
   */
  public BitSet getAtomBits(int tokType) {
    switch (tokType) {
    case Token.specialposition:
      return getSpecialPosition();
    case Token.symmetry:
      return getSymmetrySet();
    case Token.unitcell:
      return getUnitCellSet();
    }
    return super.getAtomBits(tokType);
  }

  private BitSet getSpecialPosition() {
    BitSet bs = new BitSet(atomCount);
    int modelIndex = -1;
    int nOps = 0;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      BitSet bsSym = atom.getAtomSymmetry();
      if (bsSym != null) {
        if (atom.modelIndex!=modelIndex) {
          modelIndex = atom.modelIndex;
          nOps = getModelSymmetryCount(modelIndex);
        }
        //special positions are characterized by
        //multiple operator bits set in the first (overall)
        //block of nOpts bits.
        //only strictly true with load {nnn mmm 1}
        
        int n = 0;
        for (int j = nOps; --j>= 0; )
          if (bsSym.get(j))
            if (++n > 1) {
              bs.set(i);
              break;
            }
      }
    }
    return bs;
  }

  private BitSet getUnitCellSet() {
    BitSet bsCell = new BitSet();
    UnitCell unitcell = viewer.getCurrentUnitCell();
    if (unitcell == null)
      return bsCell;
    Point3f cell = new Point3f(unitcell.getFractionalOffset());
    cell.x += 1;
    cell.y += 1;
    cell.z += 1;
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isInLatticeCell(cell))
        bsCell.set(i);
    return bsCell;
  }
  
  protected BitSet bsSymmetry;
  
  protected BitSet getSymmetrySet() {
    return BitSetUtil.copy(bsSymmetry == null ? bsSymmetry = new BitSet(atomCount) : bsSymmetry);
  }

  /**
   * general lookup involving a range
   * @param tokType
   * @param specInfo
   * @return BitSet; or null if mess up with type
   */
  public BitSet getAtomBits(int tokType, int[] specInfo) {
    switch (tokType) {
    case Token.spec_seqcode_range:
      return getSpecSeqcodeRange(specInfo[0], specInfo[1]);
    case Token.cell:
      return getCellSet(specInfo[0], specInfo[1], specInfo[2]);
    }
    return null;
  }

  private BitSet getSpecSeqcodeRange(int seqcodeA, int seqcodeB) {
    BitSet bs = new BitSet();
    for (int i = modelCount; --i >= 0;)
      models[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
    return bs;
  }

  /**
   * Get atoms within a specific distance of any atom in a specific set of
   * atoms either within all models or within just the model(s) of those atoms
   * 
   * @param distance
   * @param bs
   * @param withinAllModels 
   * @return the set of atoms
   */
  public BitSet getAtomsWithin(float distance, BitSet bs,
                               boolean withinAllModels) {
    BitSet bsResult = new BitSet();
    BitSet bsCheck = getIterativeModels(false);
    float d2 = distance * distance;
    int iAtom = 0;
    if (withinAllModels) {
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i))
          for (int iModel = modelCount; --iModel >= 0;) {
            if (!bsCheck.get(iModel))
              continue;
            AtomIndexIterator iterWithin = getWithinModelIterator(iModel, atoms[i],
                distance);
            while (iterWithin.hasNext())
              if ((iAtom = iterWithin.next()) >= 0
                  && iterWithin.foundDistance2() <= d2)
                bsResult.set(iAtom);
          }
    } else {
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          AtomIndexIterator iterWithin = getWithinModelIterator(atoms[i], distance);
          while (iterWithin.hasNext())
            if ((iAtom = iterWithin.next()) >= 0
                && iterWithin.foundDistance2() <= d2)
              bsResult.set(iAtom);
        }
    }
    return bsResult;
  }

  public BitSet getAtomsWithin(float distance, Point3f coord) {

    BitSet bsResult = new BitSet();
    
/*    
    //this test showed it was highly worth the effort to do this.
    
    long timeBegin = 0;
    timeBegin = System.currentTimeMillis();

    if (viewer.getTestFlag1()) {
      for (int i = atomCount; --i >= 0;) {
        Atom atom = atoms[i];
        if (atom.distance(coord) <= distance)
          bsResult.set(atom.atomIndex);
      }
      //System.out.println("modelCollection simple distance " + (System.currentTimeMillis() - timeBegin));
      return bsResult;
    }
*/
    BitSet bsCheck = getIterativeModels(false);
    float d2 = distance * distance;
    for (int iModel = modelCount; --iModel >= 0;) {
      if (!bsCheck.get(iModel))
        continue;
      AtomIndexIterator iterWithin = getWithinModelIterator(iModel, coord,
          distance);
      int iAtom;
      while (iterWithin.hasNext())
        if ((iAtom = iterWithin.next()) >= 0
            && iterWithin.foundDistance2() <= d2)
          bsResult.set(iAtom);
    }
    //System.out.println("modelCollection iter distance " + (System.currentTimeMillis() - timeBegin));
    return bsResult;
  }
 
  public BitSet getAtomsWithin(int tokType, BitSet bs) {
    switch (tokType) {
    case Token.molecule:
      return getMoleculeBitSet(bs);
    case Token.boundbox:
      return getAtomsWithinBox(getBoxInfo(bs));
    }
    return super.getAtomsWithin(tokType, bs);
  }

  private BitSet getAtomsWithinBox(BoxInfo boxInfo) {
    BitSet bs = getAtomsWithin(boxInfo.getBoundBoxCornerVector().length() + 0.0001f,
        boxInfo.getBoundBoxCenter());
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i) && !boxInfo.isWithin(atoms[i]))
        bs.clear(i);
    return bs;
  }

  public BitSet getAtomsWithin(int tokType, String specInfo, BitSet bs) {
    if (tokType == Token.sequence)
      return withinSequence(specInfo, bs);
    return null;
  }
  
  private BitSet withinSequence(String specInfo, BitSet bs) {
    //Logger.debug("withinSequence");
    String sequence = "";
    int lenInfo = specInfo.length();
    BitSet bsResult = new BitSet();
    if (lenInfo == 0)
      return bsResult;
    for (int i = 0; i < modelCount; ++i) {
      int polymerCount = getBioPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++) {
        sequence = models[i].getBioPolymer(ip).getSequence();
        int j = -1;
        while ((j = sequence.indexOf(specInfo, ++j)) >=0)
          models[i].getBioPolymer(ip)
          .getPolymerSequenceAtoms(i, ip, j, lenInfo, bs, bsResult);
      }
    }
    return bsResult;
  }

  //////////// Bonding methods ////////////
  

  protected int[] makeConnections(float minDistance, float maxDistance,
                                  short order, int connectOperation,
                                  BitSet bsA, BitSet bsB, BitSet bsBonds,
                                  boolean isBonds) {
    boolean matchAny = (order == JmolConstants.BOND_ORDER_ANY);
    boolean matchHbond = (order == JmolConstants.BOND_H_REGULAR);
    boolean matchNull = (order == JmolConstants.BOND_ORDER_NULL);
    boolean identifyOnly = false;
    boolean modifyOnly = false;
    boolean createOnly = false;
    boolean autoAromatize = false;
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    switch (connectOperation) {
    case JmolConstants.CONNECT_DELETE_BONDS:
      return deleteConnections(minDistance, maxDistance, order, bsA, bsB,
          isBonds, matchNull, minDistanceSquared, maxDistanceSquared);
    case JmolConstants.CONNECT_AUTO_BOND:
      if (order != JmolConstants.BOND_AROMATIC)
        return autoBond(order, bsA, bsB, bsBonds, isBonds, matchHbond);
      modifyOnly = true;
      autoAromatize = true;
      break;
    case JmolConstants.CONNECT_IDENTIFY_ONLY:
      identifyOnly = true;
      break;
    case JmolConstants.CONNECT_MODIFY_ONLY:
      modifyOnly = true;
      break;
    case JmolConstants.CONNECT_CREATE_ONLY:
      createOnly = true;
      break;
    }
    if (matchNull)
      order = JmolConstants.BOND_COVALENT_SINGLE; //default for setting
    defaultCovalentMad = viewer.getMadBond();
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    Bond bondAB = null;
    int n = (isBonds ? bondCount : atomCount);
    int m = (isBonds ? 1 : atomCount);
    Atom atomA = null;
    Atom atomB = null;
    for (int iA = n; --iA >= 0;) {
      if (!bsA.get(iA))
        continue;
      if (isBonds) {
        bondAB = bonds[iA];
        atomA = bondAB.atom1;
        atomB = bondAB.atom2;
      } else {
        atomA = atoms[iA];
      }
      for (int iB = m; --iB >= 0;) {
        if (!isBonds) {
          if (iB == iA)
            continue;
          if (!bsB.get(iB))
            continue;
          atomB = atoms[iB];
          if (atomA.modelIndex != atomB.modelIndex)
            continue;
          if (atomA.alternateLocationID != atomB.alternateLocationID
              && atomA.alternateLocationID != 0
              && atomB.alternateLocationID != 0)
            continue;
          bondAB = atomA.getBond(atomB);
        }
        if (bondAB == null && (identifyOnly || modifyOnly) || bondAB != null
            && createOnly)
          continue;
        float distanceSquared = atomA.distanceSquared(atomB);
        if (distanceSquared < minDistanceSquared
            || distanceSquared > maxDistanceSquared)
          continue;
        if (bondAB != null) {
          if (!identifyOnly && !matchAny) {
            bondAB.order = order;
            bsAromatic.clear(bondAB.index);
          }
          if (!identifyOnly || matchAny || order == bondAB.order || matchHbond
              && bondAB.isHydrogen()) {
            bsBonds.set(bondAB.index);
            nModified++;
          }
        } else {
          bondAtoms(atomA, atomB, order, mad, bsBonds);
          nNew++;
        }
      }
    }
    if (autoAromatize)
      assignAromaticBonds(true, bsBonds);
    return new int[] { nNew, nModified };
  }

  public int autoBond(BitSet bsA, BitSet bsB, BitSet bsExclude, BitSet bsBonds) {
    if (atomCount == 0)
      return 0;
    // null values for bitsets means "all"
    if (maxBondingRadius == Float.MIN_VALUE)
      findMaxRadii();
    float bondTolerance = viewer.getBondTolerance();
    float minBondDistance = viewer.getMinBondDistance();
    float minBondDistance2 = minBondDistance * minBondDistance;
    short mad = viewer.getMadBond();
    int nNew = 0;
    initializeBspf();

    long timeBegin = 0;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    /*
     * miguel 2006 04 02
     * note that the way that these loops + iterators are constructed,
     * everything assumes that all possible pairs of atoms are going to
     * be looked at.
     * for example, the hemisphere iterator will only look at atom indexes
     * that are >= (or <= ?) the specified atom.
     * if we are going to allow arbitrary sets bsA and bsB, then this will
     * not work.
     * so, for now I will do it the ugly way.
     * maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      boolean isAtomInSetA = (bsA == null || bsA.get(i));
      boolean isAtomInSetB = (bsB == null || bsB.get(i));
      if (!isAtomInSetA && !isAtomInSetB || bsExclude != null
          && bsExclude.get(i))
        continue;
      Atom atom = atoms[i];
      int modelIndex = atom.modelIndex;
      //no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrame(modelIndex)) {
          for (; --i >= 0;)
            if (atoms[i].modelIndex != modelIndex)
              break;
          i++;
          continue;
        }
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      CubeIterator iter = bspf.getCubeIterator(modelIndex);
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom)
          continue;
        int atomIndexNear = atomNear.atomIndex;
        boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
        boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
        if (!isNearInSetA && !isNearInSetB || bsExclude != null
            && bsExclude.get(atomIndexNear))
          continue;
        if (!(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA))
          continue;
        short order = getBondOrder(atom, myBondingRadius, atomNear, atomNear
            .getBondingRadiusFloat(), iter.foundDistance2(), minBondDistance2,
            bondTolerance);
        if (order > 0) {
          checkValencesAndBond(atom, atomNear, order, mad, bsBonds);
          nNew++;
        }
      }
      iter.release();
    }
    if (showRebondTimes && Logger.debugging) {
      long timeEnd = System.currentTimeMillis();
      Logger.debug("Time to autoBond=" + (timeEnd - timeBegin));
    }

    return nNew;
  }

  private int[] autoBond(short order, BitSet bsA, BitSet bsB, BitSet bsBonds,
                         boolean isBonds, boolean matchHbond) {
      if (isBonds) {
        BitSet bs = bsA;
        bsA = new BitSet();
        bsB = new BitSet();
        for (int i = bondCount; --i >= 0;)
          if (bs.get(i)) {
            bsA.set(bonds[i].atom1.atomIndex);
            bsB.set(bonds[i].atom2.atomIndex);
          }
      }
      if (matchHbond) {
        initializeBspf();
        return new int[] {autoHbond(bsA, bsB, bsBonds), 0};
      }
      return new int[] {autoBond(bsA, bsB, null, bsBonds), 0};
    }
  
  //////////// state definition ///////////

  protected String getProteinStructureState(BitSet bsAtoms, boolean needPhiPsi) {
    BitSet bs = null;
    StringBuffer cmd = new StringBuffer();
    StringBuffer sbTurn = new StringBuffer();
    StringBuffer sbHelix = new StringBuffer();
    StringBuffer sbSheet = new StringBuffer();
    int itype = 0;
    int id = 0;
    int iLastAtom = 0;
    int lastId = -1;
    int res1 = 0;
    int res2 = 0;
    String group1 = "";
    String group2 = "";
    String chain1 = "";
    String chain2 = "";
    int n = 0;
    int nHelix = 0;
    int nTurn = 0;
    int nSheet = 0;
    for (int i = 0; i <= atomCount; i++)
      if (bsAtoms == null || bsAtoms.get(i)) {
        id = Integer.MIN_VALUE;
        if (i == atomCount || (id = atoms[i].getProteinStructureID()) != lastId) {
          if (bs != null) {
            if (itype == JmolConstants.PROTEIN_STRUCTURE_HELIX
                || itype == JmolConstants.PROTEIN_STRUCTURE_TURN
                || itype == JmolConstants.PROTEIN_STRUCTURE_SHEET) {
              n++;
              if (bsAtoms == null) {
                int iModel = atoms[iLastAtom].modelIndex;
                cmd.append("  structure ").append(
                    JmolConstants.getProteinStructureName(itype)).append(" ")
                    .append(Escape.escape(bs))
                    .append("    \t# model=")
                    .append(getModelNumberDotted(iModel))
                    .append(" & (").append(res1).append(" - ").append(res2)
                    .append(");\n");
              } else {
                String str;
                int nx;
                String sid;
                StringBuffer sb;
                //       NNN III GGG C RRRR  GGG C RRRR
                //HELIX   99  99 LYS F  281  LEU F  293  1 
                //       NNN III 2 GGG CRRRR  GGG CRRRR
                //SHEET    1   A 8 ILE A  43  ASP A  45  0 
                //       NNN III GGG CRRRR  GGG CRRRR                                    
                //TURN     1  T1 PRO A  41  TYR A  44
                switch (itype) {
                case JmolConstants.PROTEIN_STRUCTURE_HELIX:
                  nx = ++nHelix;
                  sid = "H" + nx;
                  str = "HELIX  %3N %3ID %3GROUPA %1CA %4RESA  %3GROUPB %1CB %4RESB\n";
                  sb = sbHelix;
                  break;
                case JmolConstants.PROTEIN_STRUCTURE_SHEET:
                  nx = ++nSheet;
                  sid = "S" + nx;
                  str = "SHEET  %3N %3ID 2 %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB\n";
                  sb = sbSheet;
                  break;
                case JmolConstants.PROTEIN_STRUCTURE_TURN:
                default:
                  nx = ++nTurn;
                  sid = "T" + nx;
                  str = "TURN   %3N %3ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB\n";
                  sb = sbTurn;
                  break;
                }
                str = TextFormat.formatString(str, "N", nx);
                str = TextFormat.formatString(str, "ID", sid);
                str = TextFormat.formatString(str, "GROUPA", group1);
                str = TextFormat.formatString(str, "CA", chain1);
                str = TextFormat.formatString(str, "RESA", res1);
                str = TextFormat.formatString(str, "GROUPB", group2);
                str = TextFormat.formatString(str, "CB", chain2);
                str = TextFormat.formatString(str, "RESB", res2);
                sb.append(str);

                /*
                 HELIX    1  H1 ILE      7  PRO     19  1 3/10 CONFORMATION RES 17,19    1CRN  55
                 HELIX    2  H2 GLU     23  THR     30  1 DISTORTED 3/10 AT RES 30       1CRN  56
                 SHEET    1  S1 2 THR     1  CYS     4  0                                1CRNA  4
                 SHEET    2  S1 2 CYS    32  ILE    35 -1                                1CRN  58
                 TURN     1  T1 PRO    41  TYR    44                                     1CRN  59
                 */
              }
            }
            bs = null;
          }
          if (id == Integer.MIN_VALUE
              || bsAtoms != null
              && needPhiPsi
              && (Float.isNaN(atoms[i].getGroupPhi()) || Float.isNaN(atoms[i]
                  .getGroupPsi())))
            continue;
        }
        if (bs == null) {
          bs = new BitSet();
          res1 = atoms[i].getResno();
          group1 = atoms[i].getGroup3();
          chain1 = "" + atoms[i].getChainID();
        }
        itype = atoms[i].getProteinStructureType();
        bs.set(i);
        lastId = id;
        res2 = atoms[i].getResno();
        group2 = atoms[i].getGroup3();
        chain2 = "" + atoms[i].getChainID();
        iLastAtom = i;
      }
    if (n > 0)
      cmd.append("\n");
    return (bsAtoms == null ? cmd.toString() : sbHelix.append(sbSheet).append(
        sbTurn).append(cmd).toString());
  }
  
  public String getModelInfoAsString() {
    StringBuffer sb = new StringBuffer("model count = ");
    sb.append(modelCount).append("\nmodelSetHasVibrationVectors:")
        .append(modelSetHasVibrationVectors());
    if (modelSetProperties == null) {
      sb.append("\nProperties: null");
    } else {
      Enumeration e = modelSetProperties.propertyNames();
      sb.append("\nProperties:");
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        sb.append("\n ").append(propertyName).append("=")
            .append(modelSetProperties.getProperty(propertyName));
      }
    }
    for (int i = 0; i < modelCount; ++i)
      sb.append("\n").append(i)
          .append(":").append(getModelNumberDotted(i))
          .append(":").append(getModelName(i))
          .append(":").append(getModelTitle(i))
          .append("\nmodelHasVibrationVectors:")
          .append(modelHasVibrationVectors(i));
    return sb.toString();
  }
  
  public String getSymmetryInfoAsString() {
    StringBuffer sb = new StringBuffer("Symmetry Information:");
    for (int i = 0; i < modelCount; ++i)
      sb.append("\nmodel #").append(getModelNumberDotted(i))
          .append("; name=").append(getModelName(i)).append("\n")
          .append(getSymmetryInfoAsString(i));
    return sb.toString();
  }

  public BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    BitSet bsResult = new BitSet();
    int[] nBonded = new int[atomCount];
    int i;
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = bonds[ibond];
      if (intType == JmolConstants.BOND_ORDER_ANY || bond.order == intType) {
        if (bs.get(bond.atom1.atomIndex)) {
          nBonded[i = bond.atom2.atomIndex]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.atomIndex)) {
          nBonded[i = bond.atom1.atomIndex]++;
          bsResult.set(i);
        }
      }
    }
    boolean nonbonded = (min == 0 && max == 0);
    for (i = atomCount; --i >= 0;) {
      int n = nBonded[i];
      if (n < min || n > max)
        bsResult.clear(i);
      else if (nonbonded && n == 0)
        bsResult.set(i);
    }
    return bsResult;
  }

  public String getModelExtract(BitSet bs) {
    int nAtoms = 0;
    int nBonds = 0;
    int[] atomMap = new int[atomCount];
    StringBuffer mol = new StringBuffer();
    StringBuffer s = new StringBuffer();
    
    for (int i = 0; i < atomCount; i++) {
      if (bs.get(i)) {
        atomMap[i] = ++nAtoms;
        getAtomRecordMOL(s, i);
      }
    }
    for (int i = 0; i < bondCount; i++) {
      Bond bond = bonds[i];
      if (bs.get(bond.atom1.atomIndex) 
          && bs.get(bond.atom2.atomIndex)) {
        if (bond.order >= 1 && bond.order < 3) {
          getBondRecordMOL(s, i,atomMap);
          nBonds++;
        }
      }
    }
    if(nAtoms > 999 || nBonds > 999) {
      Logger.error("ModelManager.java::getModel: ERROR atom/bond overflow");
      return "";
    }
    // 21 21  0  0  0
    TextFormat.rFill(mol, "   ",""+nAtoms);
    TextFormat.rFill(mol, "   ",""+nBonds);
    mol.append("  0  0  0\n");
    mol.append(s);
    return mol.toString();
  }
  
  void getAtomRecordMOL(StringBuffer s, int i){
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomX(i),9));
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomY(i),9));
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomZ(i),9));
    s.append(" ").append((getElementSymbol(i) + "  ").substring(0,2)).append("\n");
  }

  void getBondRecordMOL(StringBuffer s, int i,int[] atomMap){
  //  1  2  1
    Bond b = bonds[i];
    TextFormat.rFill(s, "   ","" + atomMap[b.atom1.atomIndex]);
    TextFormat.rFill(s, "   ","" + atomMap[b.atom2.atomIndex]);
    s.append("  ").append(b.order).append("\n"); 
  }
  
  public String getModelFileInfo(BitSet frames) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < modelCount; ++i) {
      if (frames != null && !frames.get(i))
        continue;
      String file_model = getModelNumberDotted(i);
      sb.append("\n\nfile[\"").append(file_model)
          .append("\"] = ").append(Escape.escape(getModelFileName(i)))
          .append("\ntitle[\"").append(file_model)
          .append("\"] = ").append(Escape.escape(getModelTitle(i)))
          .append("\nname[\"").append(file_model)
          .append("\"] = ").append(Escape.escape(getModelName(i)));
    }
    return sb.toString();
  }
  
  public Hashtable getAuxiliaryInfo() {
    Hashtable info = getModelSetAuxiliaryInfo();
    if (info == null)
      return info;
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelinfo = getModelAuxiliaryInfo(i);
      models.addElement(modelinfo);
    }
    info.put("models",models);
    return info;
  }

  public Vector getAllAtomInfo(BitSet bs) {
    Vector V = new Vector();
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++) 
      if (bs.get(i))
        V.addElement(getAtomInfoLong(i));
    return V;
  }

  public void getAtomIdentityInfo(int i, Hashtable info) {
    info.put("_ipt", new Integer(i));
    info.put("atomIndex", new Integer(i));
    info.put("atomno", new Integer(getAtomNumber(i)));
    info.put("info", getAtomInfo(i));
    info.put("sym", getElementSymbol(i));
  }
  
  private Hashtable getAtomInfoLong(int i) {
    Atom atom = atoms[i];
    Hashtable info = new Hashtable();
    getAtomIdentityInfo(i, info);
    info.put("element", getElementName(i));
    info.put("elemno", new Integer(getElementNumber(i)));
    info.put("x", new Float(getAtomX(i)));
    info.put("y", new Float(getAtomY(i)));
    info.put("z", new Float(getAtomZ(i)));
    info.put("coord", new Point3f(atom));
    if (vibrationVectors != null && vibrationVectors[i] != null) {
      info.put("vibVector", new Vector3f(vibrationVectors[i]));
    }
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius() / 120.0)));
    info.put("model", atom.getModelNumberForLabel());
    info.put("visible", Boolean.valueOf(atoms[i].isVisible()));
    info.put("clickabilityFlags", new Integer(atom.clickabilityFlags));
    info.put("visibilityFlags", new Integer(atom.shapeVisibilityFlags));
    info.put("spacefill", new Float(atom.getRadius()));
    String strColor = viewer.getHexColorFromIndex(atom.colixAtom);
    if (strColor != null)
      info.put("color", strColor);
    info.put("colix", new Integer(atom.colixAtom));
    boolean isTranslucent = atom.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
    info.put("formalCharge", new Integer(atom.getFormalCharge()));
    info.put("partialCharge", new Float(atom.getPartialCharge()));
    float d = atom.getSurfaceDistance100() / 100f;
    if (d >= 0)
      info.put("surfaceDistance", new Float(d));
    if (getModel(atom.modelIndex).isPDB) {
      info.put("resname", atom.getGroup3());
      int seqNum = atom.getSeqNumber();
      char insCode = atom.getInsertionCode();
      if (seqNum > 0)
        info.put("resno", new Integer(seqNum));
      if (insCode != 0)
        info.put("insertionCode", "" + insCode);
      char chainID = atom.getChainID();
      info.put("name", getAtomName(i));
      info.put("chain", (chainID == '\0' ? "" : "" + chainID));
      info.put("atomID", new Integer(atom.getSpecialAtomID()));
      info.put("groupID", new Integer(atom.getGroupID()));
      if (atom.alternateLocationID != '\0')
        info.put("altLocation", "" + atom.alternateLocationID);
      info.put("structure", new Integer(atom.getProteinStructureType()));
      info.put("polymerLength", new Integer(atom.getPolymerLength()));
      info.put("occupancy", new Integer(atom.getOccupancy()));
      int temp = atom.getBfactor100();
      info.put("temp", new Integer((temp < 0 ? 0 : temp / 100)));
    }
    return info;
  }  

  public Vector getAllBondInfo(BitSet bs) {
    Vector V = new Vector();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(bonds[i].atom1.atomIndex) 
          && bs.get(bonds[i].atom2.atomIndex)) 
        V.addElement(getBondInfo(i));
    return V;
  }

  private Hashtable getBondInfo(int i) {
    Bond bond = bonds[i];
    Atom atom1 = bond.atom1;
    Atom atom2 = bond.atom2;
    Hashtable info = new Hashtable();
    info.put("_bpt", new Integer(i));
    Hashtable infoA = new Hashtable();
    getAtomIdentityInfo(atom1.atomIndex, infoA);
    Hashtable infoB = new Hashtable();
    getAtomIdentityInfo(atom2.atomIndex, infoB);
    info.put("atom1",infoA);
    info.put("atom2",infoB);
    info.put("order", new Integer(bonds[i].order));
    info.put("radius", new Float(bond.mad/2000.));
    info.put("length_Ang",new Float(atom1.distance(atom2)));
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = viewer.getHexColorFromIndex(bond.colix);
    if (strColor != null) 
      info.put("color", strColor);
    info.put("colix", new Integer(bond.colix));
    boolean isTranslucent = bond.isTranslucent();
    if (isTranslucent)
      info.put("translucent", Boolean.valueOf(isTranslucent));
   return info;
  }  
  
  public Hashtable getAllChainInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = getChainInfo(i, bs);
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("chains",info);
        modelVector.addElement(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  private Vector getChainInfo(int modelIndex, BitSet bs) {
    Model model = models[modelIndex];
    int nChains = model.getChainCount();
    Vector infoChains = new Vector();    
    for(int i = 0; i < nChains; i++) {
      Chain chain = model.getChain(i);
      Vector infoChain = new Vector();
      int nGroups = chain.getGroupCount();
      Hashtable arrayName = new Hashtable();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.getGroup(igroup);
        if (! bs.get(group.firstAtomIndex)) 
          continue;
        Hashtable infoGroup = new Hashtable();
        infoGroup.put("groupIndex", new Integer(igroup));
        infoGroup.put("groupID", new Short(group.getGroupID()));
        infoGroup.put("seqCode", group.getSeqcodeString());
        infoGroup.put("_apt1", new Integer(group.firstAtomIndex));
        infoGroup.put("_apt2", new Integer(group.lastAtomIndex));
        infoGroup.put("atomInfo1", getAtomInfo(group.firstAtomIndex));
        infoGroup.put("atomInfo2", getAtomInfo(group.lastAtomIndex));
        infoGroup.put("visibilityFlags", new Integer(group.shapeVisibilityFlags));
        infoChain.addElement(infoGroup);
      }
      if (! infoChain.isEmpty()) { 
        arrayName.put("residues",infoChain);
        infoChains.addElement(arrayName);
      }
    }
    return infoChains;
  }  
  
  public Hashtable getAllPolymerInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = new Vector();
      int polymerCount = models[i].getBioPolymerCount();
      for (int ip = 0; ip < polymerCount; ip++) {
        Hashtable polyInfo = models[i].getBioPolymer(ip).getPolymerInfo(bs); 
        if (! polyInfo.isEmpty())
          info.addElement(polyInfo);
      }
      if (info.size() > 0) {
        modelInfo.put("modelIndex",new Integer(i));
        modelInfo.put("polymers",info);
        modelVector.addElement(modelInfo);
      }
    }
    finalInfo.put("models",modelVector);
    return finalInfo;
  }

  public String getUnitCellInfoText() {
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      return "no single current model";
    if (cellInfos == null)
      return "not applicable";
    return cellInfos[modelIndex].getUnitCellInfo();
  }

  public String getSpaceGroupInfoText(String spaceGroup) {
    SpaceGroup sg;
    String strOperations = "";
    int modelIndex = viewer.getCurrentModelIndex();
    if (spaceGroup == null) {
      if (modelIndex < 0)
        return "no single current model";
      if (cellInfos == null)
        return "not applicable";
      CellInfo cellInfo = cellInfos[modelIndex];
      spaceGroup = cellInfo.spaceGroup;
      if (spaceGroup.indexOf("[") >= 0)
        spaceGroup = spaceGroup.substring(0, spaceGroup.indexOf("[")).trim();
      if (spaceGroup == "spacegroup unspecified")
        return "no space group identified in file";
      sg = SpaceGroup.determineSpaceGroup(spaceGroup, cellInfo
          .getNotionalUnitCell());
      strOperations = "\nSymmetry operations employed:"
          + getModelSymmetryList(modelIndex);
    } else if (spaceGroup.equalsIgnoreCase("ALL")) {
      return SpaceGroup.dumpAll();
    } else if (spaceGroup.equalsIgnoreCase("ALLSEITZ")) {
      return SpaceGroup.dumpAllSeitz();
    } else {
      sg = SpaceGroup.determineSpaceGroup(spaceGroup);
      if (sg == null) {
        sg = SpaceGroup.createSpaceGroup(spaceGroup, false);
      } else {
        StringBuffer sb = new StringBuffer();
        while (sg != null) {
          sb.append(sg.dumpInfo());
          sg = SpaceGroup.determineSpaceGroup(spaceGroup, sg);
        }
        return sb.toString();
      }
    }
    if (sg == null)
      return "could not identify space group from name: " + spaceGroup 
      + "\nformat: show spacegroup \"2\" or \"P 2c\" " +
          "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"";
    return sg.dumpInfo() + strOperations;
  }
}
