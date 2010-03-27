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
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolBioResolver;
import org.jmol.api.SymmetryInterface;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;

import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.util.TriangleData;
import org.jmol.viewer.JmolConstants;
import org.jmol.script.Token;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.StateManager.Orientation;

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
  protected void merge(ModelSet modelSet) {
    for (int i = 0; i < modelSet.modelCount; i++) {
      Model m = models[i] = modelSet.models[i];
      m.modelSet = (ModelSet) this;
      for (int j = 0; j < m.chainCount; j++)
        m.chains[j].setModelSet(m.modelSet);
      stateScripts = modelSet.stateScripts;
      proteinStructureTainted = modelSet.proteinStructureTainted;
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
    unitCells = null;
    withinModelIterator = null;
    withinAtomSetIterator = null;
    super.releaseModelSet();
  }

  
  protected BitSet bsSymmetry;
  
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

  SymmetryInterface[] unitCells;

  public SymmetryInterface[] getCellInfos() {
    return unitCells;
  }

  public SymmetryInterface getUnitCell(int modelIndex) {
    return (unitCells != null && modelIndex >= 0 && modelIndex < unitCells.length 
        ? unitCells[modelIndex] : null);
  }

  /**
   * 
   * @param type
   * @param plane
   * @param scale
   * @param modelIndex
   * @param flags
   *          1 -- edges only 2 -- triangles only 3 -- both
   * @return Vector
   */
  public Vector getPlaneIntersection(int type, Point4f plane, float scale,
                                     int flags, int modelIndex) {
    Point3f[] pts = null;
    switch (type) {
    case Token.unitcell:
      SymmetryInterface uc = getUnitCell(modelIndex);
      if (uc == null)
        return null;
      pts = uc.getCanonicalCopy(scale);
      break;
    case Token.boundbox:
      pts = boxInfo.getCanonicalCopy(scale);
      break;
    }
    Vector v = new Vector();
    v.add(pts);
    return TriangleData.intersectPlane(plane, v, flags);
  }

  protected int[] modelNumbers = new int[1];  // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  protected int[] modelFileNumbers = new int[1];  // file * 1000000 + modelInFile (1-based)
  protected String[] modelNumbersForAtomLabel = new String[1];
  protected String[] modelNames = new String[1];
  protected String[] frameTitles = new String[1];

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

  public void setFrameTitle(BitSet bsFrames, String title) {
    for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames.nextSetBit(i + 1)) 
      frameTitles[i] = title;
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

  private Properties modelSetProperties;
  private Hashtable modelSetAuxiliaryInfo;

  protected Group[] groups;
  protected int groupCount;
  protected int baseGroupIndex = 0;
  private int structureCount = 0;
  private Structure[] structures = new Structure[10];
  protected boolean haveBioClasses = true;
  protected JmolBioResolver jbr = null;
  
  protected void calculatePolymers(BitSet alreadyDefined) {
    if (jbr == null)
      return;
    if (alreadyDefined != null) {
      jbr.clearBioPolymers(groups, groupCount, alreadyDefined);
    }
    boolean checkPolymerConnections = !viewer.getPdbLoadInfo(1);
    for (int i = baseGroupIndex; i < groupCount; ++i) {
      Polymer bp = jbr.buildBioPolymer(groups[i], groups, i, checkPolymerConnections);
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
    polymer.bioPolymerIndexInModel = model.bioPolymerCount;
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
    return (unitCells == null || unitCells[0] == null ? null : unitCells[0]
        .getNotionalUnitCell());
  }

  //new way:

  protected boolean someModelsHaveSymmetry;
  protected boolean someModelsHaveAromaticBonds;
  protected boolean someModelsHaveFractionalCoordinates;

  public boolean setCrystallographicDefaults() {
    return !isPDB && someModelsHaveSymmetry && someModelsHaveFractionalCoordinates;
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
    boxInfo.setBbcage(1);
  }
  
  public Point3f getAverageAtomPoint() {
    return averageAtomPoint;
  }

  public Point3f getBoundBoxCenter(int modelIndex) {
    if (isJmolDataFrame(modelIndex))
      return new Point3f();
    return boxInfo.getBoundBoxCenter();
  }

  public Vector3f getBoundBoxCornerVector() {
    return boxInfo.getBoundBoxCornerVector();
  }

  public Point3fi[] getBboxVertices() {
    return boxInfo.getBboxVertices();
  }

  public Hashtable getBoundBoxInfo() {
    return boxInfo.getBoundBoxInfo();
  }

  public BitSet getBoundBoxModels() {
    return bboxModels;
  }
  
  public void setBoundBox(Point3f pt1, Point3f pt2, boolean byCorner, float scale) {
    if (pt1 != null && pt1.distance(pt2) == 0)
      return;
    isBbcageDefault = false;
    bboxModels = null;
    bboxAtoms = null;
    boxInfo.setBoundBox(pt1, pt2, byCorner, scale);
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

  public int getDefaultVdwType(int modelIndex) {
    return (!models[modelIndex].isPDB ? JmolConstants.VDW_AUTO_BABEL
        : models[modelIndex].hydrogenCount == 0 ? JmolConstants.VDW_AUTO_JMOL
        : JmolConstants.VDW_AUTO_BABEL); // RASMOL is too small
  }

  public boolean setRotationRadius(int modelIndex, float angstroms) {
    if (isJmolDataFrame(modelIndex)) {
      models[modelIndex].defaultRotationRadius = angstroms;
      return false;
    }
    return true;
  }

  public float calcRotationRadius(int modelIndex, Point3f center) {
    if (isJmolDataFrame(modelIndex)) {
      float r = models[modelIndex].defaultRotationRadius;
      return (r == 0 ? 10 : r);
    }
    float maxRadius = 0;
    for (int i = atomCount; --i >= 0;) {
      if (isJmolDataFrame(atoms[i])) {
        modelIndex = atoms[i].modelIndex;
        while (i >= 0 && atoms[i].modelIndex == modelIndex)
          i--;
        continue;
      }
        Atom atom = atoms[i];
        float distAtom = center.distance(atom);
        float outerVdw = distAtom + getRadiusVdwJmol(atom);
        if (outerVdw > maxRadius)
          maxRadius = outerVdw;     
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  public void calcBoundBoxDimensions(BitSet bs, float scale) {
    if (bs != null && bs.nextSetBit(0) < 0)
      bs = null;
    if (bs == null && isBbcageDefault || atomCount < 2)
      return;
    bboxModels = getModelBitSet(bboxAtoms = BitSetUtil.copy(bs), false);
    if (calcAtomsMinMax(bs, boxInfo) == atomCount)
      isBbcageDefault = true;
    if (bs == null) { // from modelLoader or reset
      averageAtomPoint.set(getAtomSetCenter(null));
      if (unitCells != null)
        calcUnitCellMinMax();
    }
    boxInfo.setBbcage(scale);
  }

  public BoxInfo getBoxInfo(BitSet bs, float scale) {
    if (bs == null)
      return boxInfo;
    BoxInfo bi = new BoxInfo();
    calcAtomsMinMax(bs, bi);
    bi.setBbcage(scale);
    return bi;
  }

  public int calcAtomsMinMax(BitSet bs, BoxInfo boxInfo) {
    boxInfo.reset();
    int nAtoms = 0;
    boolean isAll = (bs == null);
    int i0 = (isAll ? atomCount - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      nAtoms++;
      if (!isJmolDataFrame(atoms[i]))
        boxInfo.addBoundBoxPoint(atoms[i]);
    }
    return nAtoms;
  }

  private void calcUnitCellMinMax() {
    for (int i = 0; i < modelCount; i++) {
      if (!unitCells[i].getCoordinatesAreFractional())
        continue;
      Point3f[] vertices = unitCells[i].getUnitCellVertices();
      for (int j = 0; j < 8; j++)
        boxInfo.addBoundBoxPoint(vertices[j]);
    }
  }

  public float calcRotationRadius(BitSet bs) {
    // Eval getZoomFactor
    Point3f center = getAtomSetCenter(bs);
    float maxRadius = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  /**
   * 
   * @param vAtomSets
   * @param addCenters
   * @return             array of two lists of points, centers first if desired
   */

  public Point3f[][] getCenterAndPoints(Vector vAtomSets, boolean addCenters) {
    BitSet bsAtoms1, bsAtoms2;
    int n = (addCenters ? 1 : 0);
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      BitSet[] bss = (BitSet[]) vAtomSets.get(ii);
      bsAtoms1 = bss[0];
      bsAtoms2 = bss[1];
      n += Math.min(bsAtoms1.cardinality(), bsAtoms2.cardinality());
    }
    Point3f[][] points = new Point3f[2][n];
    if (addCenters) {
      points[0][0] = new Point3f();
      points[1][0] = new Point3f();
    }
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      BitSet[] bss = (BitSet[]) vAtomSets.get(ii);
      bsAtoms1 = bss[0];
      bsAtoms2 = bss[1];
      for (int i = bsAtoms1.nextSetBit(0), j = bsAtoms2.nextSetBit(0); i >= 0 && j >= 0; i = bsAtoms1
          .nextSetBit(i + 1), j = bsAtoms2.nextSetBit(j + 1)) {
        points[0][--n] = atoms[i];
        points[1][n] = atoms[j];
        if (addCenters) {
          points[0][0].add(atoms[i]);
          points[1][0].add(atoms[j]);
        }
      }
    }
    if (addCenters) {
      points[0][0].scale(1f/(points[0].length - 1));
      points[1][0].scale(1f/(points[1].length - 1));
    }
    return points;
  }

  public Point3f getAtomSetCenter(BitSet bs) {
    Point3f ptCenter = new Point3f(0, 0, 0);
    int nPoints = 0;
    if (bs != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (!isJmolDataFrame(atoms[i])) {
          nPoints++;
          ptCenter.add(atoms[i]);
        }
      }
    if (nPoints > 0)
      ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    super.setAtomProperty(bs, tok, iValue, fValue, sValue, values, list);
    if ((tok == Token.valence || tok == Token.formalcharge)
        && viewer.getSmartAromatic())
      assignAromaticBonds();
  }
  
  protected Vector stateScripts = new Vector();
  /*
   * stateScripts are connect commands that must be executed in sequence.
   * 
   * What I fear is that in deleting models we must delete these connections,
   * and in deleting atoms, the bitsets may not be retrieved properly. 
   * 
   * First, ZAP. We should be able to check these to see if the selected model
   * 
   */
  private int thisStateModel = 0;

  public StateScript addStateScript(String script1, BitSet bsBonds, BitSet bsAtoms1,
                             BitSet bsAtoms2, String script2,
                             boolean addFrameNumber, boolean postDefinitions) {
    if (addFrameNumber) {
      int iModel = viewer.getCurrentModelIndex();
      if (thisStateModel != iModel)
        script1 = "frame "
            + (iModel < 0 ? "" + iModel : getModelNumberDotted(iModel)) + ";\n  "
            + script1;
      thisStateModel = iModel;
    } else {
      thisStateModel = -1;
    }
    StateScript stateScript = new StateScript(thisStateModel, script1, bsBonds, bsAtoms1,
        bsAtoms2, script2, postDefinitions);
    if (stateScript.isValid())
      stateScripts.addElement(stateScript);
    return stateScript;
  }

  public static class StateScript {
    private int modelIndex;
    private BitSet bsBonds;
    private BitSet bsAtoms1;
    private BitSet bsAtoms2;
    private String script1;
    private String script2;
    boolean postDefinitions;
    
    StateScript(int modelIndex, String script1, BitSet bsBonds, BitSet bsAtoms1,
        BitSet bsAtoms2, String script2, boolean postDefinitions) {
      this.modelIndex = modelIndex;
      this.script1 = script1;
      this.bsBonds = bsBonds;
      this.bsAtoms1 = bsAtoms1;
      this.bsAtoms2 = bsAtoms2;
      this.script2 = script2;
      this.postDefinitions = postDefinitions;
    }
    
    public boolean isValid() {
      return script1 != null && script1.length() > 0
        && (bsBonds == null || bsBonds.nextSetBit(0) >= 0)
        && (bsAtoms1 == null || bsAtoms1.nextSetBit(0) >= 0)
        && (bsAtoms2 == null || bsAtoms2.nextSetBit(0) >= 0);
    }

    public String toString() {
      if (!isValid())
        return "";
      StringBuffer sb = new StringBuffer(script1);
      if (bsBonds != null)
        sb.append(" ").append(Escape.escape(bsBonds, false));
      if (bsAtoms1 != null)
        sb.append(" ").append(Escape.escape(bsAtoms1));
      if (bsAtoms2 != null)
        sb.append(" ").append(Escape.escape(bsAtoms2));
      if (script2 != null)
        sb.append(" ").append(script2);
      String s = sb.toString();
      if (!s.endsWith(";"))
        s += ";";
      return s;
    }

    public boolean isConnect() {
      return (script1.indexOf("connect") == 0);
    }
    
    public boolean deleteAtoms(int modelIndex, BitSet bsBonds, BitSet bsAtoms) {
      //false return means delete this script
      if (modelIndex == this.modelIndex) 
        return true;
      if (modelIndex < this.modelIndex) {
//        this.modelIndex--;
        return false;
      }
      BitSetUtil.deleteBits(this.bsBonds, bsBonds);
      BitSetUtil.deleteBits(this.bsAtoms1, bsAtoms);
      BitSetUtil.deleteBits(this.bsAtoms2, bsAtoms);
      return isValid();
    }

    public void setModelIndex(int index) {
      modelIndex = index; // for creating data frames 
    }
  }
  
  protected void defineStructure(int modelIndex, String structureType, 
                                 String structureID, int serialID, int strandCount, 
                                 char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID, int endSequenceNumber,
                       char endInsertionCode) {
    if (structureCount == structures.length)
      structures = (Structure[]) ArrayUtil.setLength(structures,
          structureCount + 10);
    structures[structureCount++] = new Structure(modelIndex, structureType,
        structureID, serialID, 
        strandCount, startChainID,
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
  protected void calculateStructuresAllExcept(BitSet alreadyDefined, boolean addFileData) {
    freezeModels();
    for (int i = modelCount; --i >= 0;)
      if (models[i].isPDB && !alreadyDefined.get(i))
        models[i].calculateStructures();
    setStructureIds();
     if (addFileData)
      propagateSecondaryStructure();
  }

  public void setProteinType(BitSet bs, byte iType) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    BitSet bsModels = new BitSet();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (iLast != i - 1)
        monomerIndexCurrent = -1;
      monomerIndexCurrent = atoms[i].group.setProteinStructureType(iType,
          monomerIndexCurrent);
      int modelIndex = atoms[i].modelIndex;
      bsModels.set(modelIndex);
      proteinStructureTainted = models[modelIndex].structureTainted = true;
      iLast = i = atoms[i].group.lastAtomIndex;
    }
    int[] lastStrucNo = new int[modelCount];
    for (int i = 0; i < atomCount; ) {
      int modelIndex = atoms[i].modelIndex; 
      if (!bsModels.get(modelIndex)) {
        i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount;
        continue;
      }
      iLast = atoms[i].getStrucNo();
      if (iLast < 1000 && iLast > lastStrucNo[modelIndex])
        lastStrucNo[modelIndex] = iLast;
      i = atoms[i].group.lastAtomIndex + 1;
    }
    for (int i = 0; i < atomCount; ) {
      int modelIndex = atoms[i].modelIndex; 
      if (!bsModels.get(modelIndex)) {
        i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount;
        continue;
      }
      if (atoms[i].getStrucNo() > 1000)
        atoms[i].group.setProteinStructureId(++lastStrucNo[modelIndex]);
      i = atoms[i].group.lastAtomIndex + 1;
    }
  }
  
  private void freezeModels() {
    for (int iModel = modelCount; --iModel >= 0;) {
      Model m = models[iModel];
      m.chains = (Chain[])ArrayUtil.setLength(m.chains, m.chainCount);
      m.groupCount = -1;
      m.getGroupCount();      
      for (int i = 0; i < m.chainCount; ++i)
        m.chains[i].groups = (Group[])ArrayUtil.setLength(m.chains[i].groups, m.chains[i].groupCount);
      m.bioPolymers = (Polymer[])ArrayUtil.setLength(m.bioPolymers, m.bioPolymerCount);
      for (int i = m.bioPolymerCount; --i >= 0; ) {
        m.bioPolymers[i].freeze();
      }
    }
  }
  
  public BitSet setConformation(BitSet bsAtoms) {
    BitSet bsModels = getModelBitSet(bsAtoms, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      models[i].setConformation(bsAtoms);
    return bsAtoms;
  }

  public BitSet getConformation(int modelIndex, int conformationIndex,
                                boolean doSet) {
    BitSet bs = new BitSet();
    for (int i = modelCount; --i >= 0;)
      if (i == modelIndex || modelIndex < 0) {
        int nAltLocs = getAltLocCountInModel(i);
        if (nAltLocs == 0 || conformationIndex >= nAltLocs)
          continue;
        String altLocs = getAltLocListInModel(i);
        BitSet bsConformation = getModelAtomBitSet(i, true);
        if (conformationIndex >= 0)
          for (int c = nAltLocs; --c >= 0;)
            if (c != conformationIndex)
              bsConformation.andNot(getAtomBits(
                  Token.spec_alternate, altLocs.substring(c, c + 1)));
        if (bsConformation.nextSetBit(0) >= 0) {
          bs.or(bsConformation);
          if (doSet)
            models[i].setConformation(bsConformation);
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

  protected void setModelSetProperties(Properties modelSetProperties) {
    this.modelSetProperties = modelSetProperties;
  }

  protected void setModelSetAuxiliaryInfo(Hashtable modelSetAuxiliaryInfo) {
    this.modelSetAuxiliaryInfo = modelSetAuxiliaryInfo;
  }

  public Properties getModelSetProperties() {
    return modelSetProperties;
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return modelSetAuxiliaryInfo;
  }

  public String getModelSetProperty(String propertyName) {
    // no longer used in Jmol
    return (modelSetProperties == null ? null : modelSetProperties
        .getProperty(propertyName));
  }

  public Object getModelSetAuxiliaryInfo(String keyName) {
    // the preferred method now
    return (modelSetAuxiliaryInfo == null ? null : modelSetAuxiliaryInfo
        .get(keyName));
  }

  protected boolean getModelSetAuxiliaryInfoBoolean(String keyName) {
    return (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName) && ((Boolean) modelSetAuxiliaryInfo
        .get(keyName)).booleanValue());
  }

/*
  int getModelSetAuxiliaryInfoInt(String keyName) {
    if (modelSetAuxiliaryInfo != null
        && modelSetAuxiliaryInfo.containsKey(keyName)) {
      return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }
*/

  protected Vector trajectorySteps;

  protected int getTrajectoryCount() {
    return (trajectorySteps == null ? 0 : trajectorySteps.size());
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
    int atomIndex;
    for (int i = 1; i <= count; i++)
      if ((atomIndex = countPlusIndices[i]) >= 0 
          && models[atoms[atomIndex].modelIndex].isTrajectory)
        return true;
    return false;
  }

  public BitSet getModelBitSet(BitSet atomList, boolean allTrajectories) {
    BitSet bs = new BitSet();
    int modelIndex = 0;
    boolean isAll = (atomList == null);
    int i0 = (isAll ? 0 : atomList.nextSetBit(0));
    for (int i = i0; i >= 0 && i < atomCount; i = (isAll ? i + 1 : atomList
        .nextSetBit(i + 1))) {
      bs.set(modelIndex = atoms[i].modelIndex);
      if (allTrajectories) {
        int iBase = models[modelIndex].trajectoryBaseIndex;
        for (int j = 0; j < modelCount; j++)
          if (models[j].trajectoryBaseIndex == iBase)
            bs.set(j);
      }
      i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount - 1;
    }
    return bs;
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

  public int getInsertionCountInModel(int modelIndex) {
    return models[modelIndex].nInsertions;
  }

  public String getModelFileType(int modelIndex) {
    return (String) getModelAuxiliaryInfo(modelIndex, "fileType");
  }
  
  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  

    int file = (int) (fDotM);
    int model = (int) ((fDotM - file + 0.00001) * 10000);
    while (model != 0 && model % 10 == 0)
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
          structure.structureID, structure.serialID, structure.strandCount,
          structure.startChainID, structure.startSeqcode, structure.endChainID,
          structure.endSeqcode);
    }
  }

  public int getChainCount(boolean addWater) {
    int chainCount = 0;
    for (int i = modelCount; --i >= 0;)
      chainCount += models[i].getChainCount(addWater);
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

  public int getChainCountInModel(int modelIndex, boolean countWater) {
    if (modelIndex < 0)
      return getChainCount(countWater);
    return models[modelIndex].getChainCount(countWater);
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

  public void calculateStraightness() {
    if (getHaveStraightness())
      return;
    char ctype = 'S';//(viewer.getTestFlag3() ? 's' : 'S');
    char qtype = viewer.getQuaternionFrame();
    int mStep = viewer.getHelixStep();
    // testflag3 ON  --> preliminary: Hanson's original normal-based straightness
    // testflag3 OFF --> final: Kohler's new quaternion-based straightness
    for (int i = modelCount; --i >= 0;) {
      Model model = models[i];
      int nPoly = model.getBioPolymerCount();
      for (int p = 0; p < nPoly; p++)
        model.bioPolymers[p].getPdbData(viewer, ctype, qtype, mStep, 2, false, null, null, null, null, false, new BitSet());
    }
    setHaveStraightness(true);
  }

  public Quaternion[] getAtomGroupQuaternions(BitSet bsAtoms, int nMax,
                                              char qtype) {
    // run through list, getting quaternions. For simple groups, 
    // go ahead and take first three atoms
    // for PDB files, do not include NON-protein groups.
    int n = 0;
    Vector v = new Vector();
    for (int i = bsAtoms.nextSetBit(0); i >= 0 && n < nMax; i = bsAtoms.nextSetBit(i + 1)) {
      Group g = atoms[i].group;
      Quaternion q = g.getQuaternion(qtype);
      if (q == null) {
        if (g.seqcode == Integer.MIN_VALUE)
          q = g.getQuaternionFrame(atoms); // non-PDB just use first three atoms
        if (q == null)
          continue;
      }
      v.add(q);
      i = g.lastAtomIndex;
    }
    Quaternion[] qs = new Quaternion[v.size()];
    for (int i = 0; i < qs.length; i++) 
      qs[i] = (Quaternion) v.get(i);
    return qs;
  }

  private static class Structure {
    String typeName;
    byte type;
    char startChainID;
    int startSeqcode;
    char endChainID;
    int endSeqcode;
    int modelIndex;
    String structureID;
    int serialID;
    int strandCount;

    Structure(int modelIndex, String typeName, 
        String structureID, int serialID, int strandCount, 
        char startChainID,
        int startSeqcode, char endChainID, int endSeqcode) {
      this.modelIndex = modelIndex;
      this.typeName = typeName;
      this.structureID = structureID;
      this.strandCount = strandCount; 
      this.serialID = serialID;
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

  /*
   * Vector getStructureInfo() { Vector info = new Vector(); for (int i = 0; i <
   * structureCount; i++) info.addElement(structures[i].toHashtable()); return
   * info; }
   */
  /*
   * ONLY from one model
   */

  public String getPdbAtomData(BitSet bs) {
    if (atomCount == 0)
      return "";
    int iModel = atoms[0].modelIndex;
    int iModelLast = -1;
    StringBuffer sb = new StringBuffer();
    boolean showModels = (iModel != atoms[atomCount - 1].modelIndex);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      if (showModels && a.modelIndex != iModelLast) {
        if (iModelLast != -1)
          sb.append("ENDMDL\n");
        iModelLast = a.modelIndex;
        sb.append("MODEL     " + (iModelLast + 1) + "\n");
      }
      if (!models[a.modelIndex].isPDB)
        sb
            .append(LabelToken
                .formatLabel(
                    viewer,
                    a,
                    "HETATM%5i %-4a%1AUNK %1c   1%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2[symbol]  \n"));
      else if (a.isHetero())
        sb
            .append(LabelToken
                .formatLabel(
                    viewer,
                    a,
                    "HETATM%5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2[symbol]  \n"));
      else
        sb
            .append(LabelToken
                .formatLabel(
                    viewer,
                    a,
                    "ATOM  %5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2[symbol]  \n"));
    }
    if (showModels)
      sb.append("ENDMDL\n");
    return sb.toString();
  }
  
  /* **********************
   * 
   * Jmol Data Frame methods
   * 
   *****************************/

  public String getPdbData(int modelIndex, String type, BitSet bsSelected, boolean addHeader) {
    if (isJmolDataFrame(modelIndex))
      modelIndex = getJmolDataSourceFrame(modelIndex);
    if (modelIndex < 0)
      return "";
    if (!models[modelIndex].isPDB)
      return null;
    Model model = models[modelIndex];
    char ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type
        .charAt(11) : 'R');
    char qtype = (ctype != 'R' ? 'r' 
        : type.length() > 13 && type.indexOf("ramachandran ") >= 0 ?
          type.charAt(13) : 'R');
    if (qtype == 'r')
      qtype = viewer.getQuaternionFrame();
    int mStep = viewer.getHelixStep();
    int derivType = (type.indexOf("diff") < 0 ? 0 : type.indexOf("2") < 0 ? 1 : 2);
    boolean isDraw = (type.indexOf("draw") >= 0); 
    BitSet bsAtoms = getModelAtomBitSet(modelIndex, false);
    int nPoly = model.getBioPolymerCount();
    StringBuffer pdbATOM = new StringBuffer();
    StringBuffer pdbCONECT = new StringBuffer();
    BitSet bsWritten = new BitSet();
    for (int p = 0; p < nPoly; p++)
        model.bioPolymers[p].getPdbData(viewer, ctype, qtype,mStep, derivType, isDraw,
            bsAtoms, pdbATOM, pdbCONECT, bsSelected, p == 0, bsWritten);
    pdbATOM.append(pdbCONECT);
    String s = pdbATOM.toString();
    if (isDraw || s.length() == 0)
      return s;
    String remark = "REMARK   6 Jmol PDB-encoded data: " + type + ";";
    if (ctype != 'R')
      remark += "  quaternionFrame = \"" + qtype + "\"";
    remark += "\nREMARK   6 Jmol Version " + Viewer.getJmolVersion();
    bsSelected.and(bsAtoms);
    remark += "\n\n" + getProteinStructureState(bsWritten, false, ctype == 'R', true);
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
    if (type.startsWith("quaternion") && type.indexOf("deriv") < 0) { //generic quaternion
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

  protected void clearDataFrameReference(int modelIndex) {
    for (int i = 0; i < modelCount; i++) { 
      Hashtable df = models[i].dataFrames;
      if (df == null)
        continue;
      Object key;
      Enumeration e = df.keys();
      while (e.hasMoreElements())
        if (((Integer)(df.get(key = e.nextElement()))).intValue() == modelIndex)
          df.remove(key);
    }  
  }
  
  public String getJmolFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? 
        models[modelIndex].jmolFrameType : "modelSet");
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? 
        models[modelIndex].dataSourceFrame : -1);
  }

  public void saveModelOrientation(int modelIndex, Orientation orientation) {
    models[modelIndex].orientation = orientation;
  }

  public Orientation getModelOrientation(int modelIndex) {
    return models[modelIndex].orientation;
  }

  /*
   final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
   "MODEL ", "SCALE",  "HETATM", "SEQRES",
   "DBREF ", };
   */

  private final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  private String getFullPDBHeader(int modelIndex) {
    if (modelIndex < 0)
      return "";
    String info = (String) getModelAuxiliaryInfo(modelIndex, "fileHeader");
    if (info != null)
      return info;
    info = viewer.getCurrentFileAsString();
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0 : info.indexOf("\n"
          + strRecord))) {
      case -1:
        continue;
      case 0:
        setModelAuxiliaryInfo(modelIndex, "fileHeader", "");
        return "";
      default:
        if (ichFound < ichMin)
          ichMin = ++ichFound;
      }
    }
    info = info.substring(0, ichMin);
    setModelAuxiliaryInfo(modelIndex, "fileHeader", info);
    return info;
  }

  public String getPDBHeader(int modelIndex) {
    return (isPDB ? getFullPDBHeader(modelIndex) : getFileHeader(modelIndex));
  }

  public String getFileHeader(int modelIndex) {
    if (modelIndex < 0)
      return "";
    if (isPDB)
      return getFullPDBHeader(modelIndex);
    String info = (String) getModelAuxiliaryInfo(modelIndex, "fileHeader");
    if (info == null)
      info = modelSetName;
    if (info != null)
      return info;
    return "no header information found";
  }

  public Hashtable getModelInfo(BitSet bsModels) {
    Hashtable info = new Hashtable();
    info.put("modelSetName", modelSetName);
    info.put("modelCount", new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", Boolean
        .valueOf(modelSetHasVibrationVectors()));
    if (modelSetProperties != null)
      info.put("modelSetProperties", modelSetProperties);
    info.put("modelCountSelected", new Integer(BitSetUtil.cardinalityOf(bsModels)));
    info.put("modelsSelected", bsModels);
    Vector vModels = new Vector();
    
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
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
      model.put("chainCount", new Integer(getChainCountInModel(i, true)));
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

  public int getModelSymmetryCount(int modelIndex) {
    String[] operations;
    if (unitCells == null || unitCells[modelIndex] == null ||
        (operations = unitCells[modelIndex].getSymmetryOperations()) == null)
      return models[modelIndex].biosymmetryCount;
    return operations.length;
  }
  
  public String getSymmetryOperation(int modelIndex, String spaceGroup, 
                                     int symOp, Point3f pt1, Point3f pt2, 
                                     String drawID) {
    Hashtable sginfo = getSpaceGroupInfo(modelIndex, spaceGroup, symOp, pt1, pt2, drawID); 
    if (sginfo == null)
      return "";
    Object[][] infolist = (Object[][]) sginfo.get("operations");
    if (infolist == null)
      return "";
    StringBuffer sb = new StringBuffer();
    symOp--;
    for (int i = 0; i < infolist.length; i++) {
      if (infolist[i] == null || symOp >= 0 && symOp != i)
        continue;
      if (drawID != null)
        return (String) infolist[i][3];
      if (sb.length() > 0)
        sb.append('\n');
      if (symOp < 0)
        sb.append(i + 1).append("\t");
      sb.append(infolist[i][0]).append("\t").append(infolist[i][2]);
    }
    if (sb.length() == 0 && drawID != null)
      sb.append ("draw " + drawID + "* delete");
    return sb.toString();
  }

  public int[] getModelCellRange(int modelIndex) {
    if (unitCells == null)
      return null;
    return unitCells[modelIndex].getCellRange();
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

  private String getSymmetryInfoAsString(int modelIndex) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    return (unitCell == null ? "no symmetry information" 
        : unitCell.getSymmetryInfoString());
  }

  public void toCartesian(int modelIndex, Point3f pt) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    if (unitCell != null)
      unitCell.toCartesian(pt);
  }

  public void toFractional(int modelIndex, Point3f pt) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    if (unitCell != null)
      unitCell.toFractional(pt);
  }

  public void toUnitCell(int modelIndex, Point3f pt, Point3f offset) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    if (unitCell != null)
      unitCells[modelIndex].toUnitCell(pt, offset);
  }

  public boolean setUnitCellOffset(int modelIndex, Point3f pt) {
    // from "unitcell {i j k}" via uccage
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    if (unitCell == null)
      return false;
    unitCell.setUnitCellOffset(pt);
    return true;
  }

  public boolean setUnitCellOffset(int modelIndex, int nnn) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
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
      if (bsTemp.length() > 0)
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

  public void rotateAtoms(Matrix3f mNew, Matrix3f matrixRotate,
                             BitSet bsAtoms, boolean fullMolecule,
                             Point3f center, boolean isInternal) {
    bspf = null;
    BitSet bs = (fullMolecule ? getMoleculeBitSet(bsAtoms) : BitSetUtil.copy(bsAtoms));
    if (mNew == null) {
      matTemp.set(matrixRotate);
    } else {
      matInv.set(matrixRotate);
      matInv.invert();
      ptTemp.set(0, 0, 0);
      matTemp.mul(mNew, matrixRotate);
      matTemp.mul(matInv, matTemp);
    }
    int n = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
        if (isInternal) {
          atoms[i].sub(center);
          matTemp.transform(atoms[i]);
          atoms[i].add(center);          
        } else {
          ptTemp.add(atoms[i]);
          matTemp.transform(atoms[i]);
          ptTemp.sub(atoms[i]);
        }
        taint(i, TAINT_COORD);
        n++;
      }
    if (n == 0)
      return;
    if (!isInternal) {
      ptTemp.scale(1f / n);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) 
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
    int i;
    while ((i = bsInitial.length()) > 0) {
      bsTemp = getMoleculeBitSet(i - 1);
      bsInitial.andNot(bsTemp);
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

  public void invertSelected(Point3f pt, Point4f plane, int iAtom,
                             BitSet invAtoms, BitSet bs) {
    bspf = null;
    if (pt != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        float x = (pt.x - atoms[i].x) * 2;
        float y = (pt.y - atoms[i].y) * 2;
        float z = (pt.z - atoms[i].z) * 2;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (plane != null) {
      // ax + by + cz + d = 0
      Vector3f norm = new Vector3f(plane.x, plane.y, plane.z);
      norm.normalize();
      float d = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y
          + plane.z * plane.z);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        float twoD = -Measure.distanceToPlane(plane, d, atoms[i]) * 2;
        float x = norm.x * twoD;
        float y = norm.y * twoD;
        float z = norm.z * twoD;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (iAtom >= 0) {
      Atom thisAtom = atoms[iAtom];
      // stereochemical inversion at iAtom
      Bond[] bonds = thisAtom.bonds;
      Point3f pt1 = null;
      Point3f pt2 = null;
      BitSet bsAtoms = new BitSet();
      for (int i = 0; i < bonds.length; i++) {
        Atom a = bonds[i].getOtherAtom(thisAtom);
        if (invAtoms.get(a.index)) {
          if (pt1 == null)
            pt1 = a;
          else if (pt2 == null)
            pt2 = a;
        } else {
          bsAtoms.or(getBranchBitSet(a.index, iAtom));
        }
      }
      if (pt1 == null || bsAtoms.cardinality() == 0)
        return;
      if (pt2 == null)
        pt2 = pt1;
      pt = new Point3f(pt1);
      pt.add(pt2);
      pt.scale(0.5f);
      Vector3f v = new Vector3f(thisAtom);
      v.sub(pt);
      Quaternion q = new Quaternion(v, 180);
      rotateAtoms(null, q.getMatrix(), bsAtoms, false, thisAtom, true);
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
    pos.sub(neg);
    Logger.warn("CalculateMolecularDipole: this is an approximate result -- needs checking");
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
    if (modelIndex < 0)
      return moleculeCount;
    for (int i = 0; i < modelCount; i++) {
      if (modelIndex == i)
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
      if (bsTemp.length() > 0) {
        selectedMolecules.set(i);
        selectedMoleculeCount++;
      }
    }
  }

  public Molecule[] getMolecules() {
    if (moleculeCount > 0)
      return molecules;
    if (molecules == null)
      molecules = new Molecule[4];
    moleculeCount = 0;
    BitSet atomlist = new BitSet(atomCount);
    BitSet bs = new BitSet(atomCount);
    int thisModelIndex = -1;
    int modelIndex = -1;
    int indexInModel = -1;
    int moleculeCount0 = -1;
    Model m = null;
    for (int i = 0; i < atomCount; i++)
      if (!atomlist.get(i) && !bs.get(i)) {
        modelIndex = atoms[i].modelIndex;
        if (modelIndex != thisModelIndex) {
          indexInModel = -1;
          m = models[modelIndex];
          m.firstMolecule = moleculeCount;
          moleculeCount0 = moleculeCount - 1;
          thisModelIndex = modelIndex;
        }
        indexInModel++;
        bs = getBranchBitSet(i, -1);
        atomlist.or(bs);
        if (moleculeCount == molecules.length)
          molecules = (Molecule[]) ArrayUtil.setLength(molecules,
              moleculeCount * 2);
        molecules[moleculeCount] = new Molecule((ModelSet) this, moleculeCount,
            i, bs, thisModelIndex, indexInModel);
        m.moleculeCount = moleculeCount++ - moleculeCount0;
      }
    return molecules;
  }

  public BitSet getBranchBitSet(int atomIndex, int atomIndexNot) {
    BitSet bs = new BitSet(atomCount);
    if (atomIndex < 0)
      return bs;
    BitSet bsToTest = getModelAtomBitSet(atoms[atomIndex].modelIndex, true);
    if (atomIndexNot >= 0)
      bsToTest.clear(atomIndexNot);
    getCovalentlyConnectedBitSet(atoms[atomIndex], bs, bsToTest);
    return bs;
  }

  private void getCovalentlyConnectedBitSet(Atom atom, BitSet bs,
                                            BitSet bsToTest) {
    int atomIndex = atom.index;
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
        getCovalentlyConnectedBitSet(bond.getOtherAtom(atom), bs, bsToTest);
    }
  }

  public boolean hasCalculatedHBonds(BitSet bs) {
    BitSet bsModels = getModelBitSet(bs, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (models[atoms[i].modelIndex].hasCalculatedHBonds)
        return true;
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
          || (bond.order & JmolConstants.BOND_H_CALC_MASK) == 0)
        continue;
      if (bsAtoms != null && !bsAtoms.get(bond.atom1.index)) {
        models[baseIndex].hasCalculatedHBonds = true;
        continue;
      }
      bsDelete.set(i);
      nDelete++;
    }        
    if (nDelete > 0)
      deleteBonds(bsDelete, false);
  }

  //////////// iterators //////////
  
  //private final static boolean MIX_BSPT_ORDER = false;

  protected void initializeBspf() {
    if (bspf == null) {
      if (showRebondTimes && Logger.debugging)
        Logger.startTimer();
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
      if (showRebondTimes && Logger.debugging) {
        Logger.checkTimer("Time to build bspf");
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
    int modelIndex = atoms[atomIndex].modelIndex;
    modelIndex = models[modelIndex].trajectoryBaseIndex;
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
    return (modelIndex < 0 ? bondCount : models[modelIndex].getBondCount());
  }

  ////////// atoms /////////

  public void setAtomCoordRelative(Tuple3f offset, BitSet bs) {
    setAtomCoordRelative(bs, offset.x, offset.y, offset.z);
    recalculatePositionDependentQuantities();
  }

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    super.setAtomCoord(bs, tokType, xyzValues);
    switch(tokType) {
    case Token.vibx:
    case Token.viby:
    case Token.vibz:
    case Token.vibxyz:
      break;
    default:
      recalculatePositionDependentQuantities();
    }
  }

  private void recalculatePositionDependentQuantities() {
    if (getHaveStraightness())
      calculateStraightness();
    recalculateLeadMidpointsAndWingVectors(-1);
  }

  /** see comments in org.jmol.modelsetbio.AlphaPolymer.java
   * 
   * Struts are calculated for atoms in bs1 connecting to atoms in bs2.
   * The two bitsets may overlap. 
   * 
   * @param bs1
   * @param bs2
   * @return     number of struts added
   */
  public int calculateStruts(BitSet bs1, BitSet bs2) {
    // select only ONE model
    makeConnections(0, Float.MAX_VALUE, JmolConstants.BOND_STRUT, JmolConstants.CONNECT_DELETE_BONDS, bs1, bs2, null, false);
    int iAtom = bs1.nextSetBit(0);
    if (iAtom < 0)
      return 0;
    int modelIndex = atoms[iAtom].modelIndex;
    Model model = models[modelIndex];
    if (!model.isPDB)
      return 0;

    // only check the atoms in THIS model
    Vector vCA = new Vector();
    Atom a1 = null;
    BitSet bsCheck;
    if (bs1.equals(bs2)) {
      bsCheck = bs1;
    } else {
      bsCheck = BitSetUtil.copy(bs1);
      bsCheck.or(bs2);
    }
    bsCheck.and(getModelAtomBitSet(modelIndex, false));
    for (int i = bsCheck.nextSetBit(0); i >= 0; i = bsCheck.nextSetBit(i + 1))
      if (atoms[i].isVisible(0)
          && atoms[i].getSpecialAtomID() == JmolConstants.ATOMID_ALPHA_CARBON
          && atoms[i].getGroupID() != JmolConstants.GROUPID_CYSTINE)
        vCA.add((a1 = atoms[i]));
    if (vCA.size() == 0)
      return 0;    
    float thresh = viewer.getStrutLengthMaximum();
    short mad = (short) (viewer.getStrutDefaultRadius() * 2000);
    int delta = viewer.getStrutSpacingMinimum();
    boolean strutsMultiple = viewer.getStrutsMultiple();
    Vector struts = model.getBioPolymer(a1.getPolymerIndexInModel())
        .calculateStruts((ModelSet) this, atoms, bs1, bs2, vCA, thresh, delta, strutsMultiple);
    for (int i = 0; i < struts.size(); i++) {
      Object[] o = (Object[]) struts.get(i);
      bondAtoms((Atom) o[0], (Atom) o[1], JmolConstants.BOND_STRUT, mad, null);
    }
    return struts.size();
  }

  public int getAtomCountInModel(int modelIndex) {
    return (modelIndex < 0 ? atomCount : models[modelIndex].atomCount);
  }
  
  protected BitSet bsAll;
  
  public BitSet getModelAtomBitSet(BitSet bsModels) {
    BitSet bs = new BitSet();
    if (bsModels == null)
      bs.or(bsAll);
    else
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
        bs.or(getModelAtomBitSet(i, false));
    return bs;
  }
  /**
   * 
   * @param modelIndex
   * @param asCopy     MUST BE TRUE IF THE BITSET IS GOING TO BE MODIFIED!
   * @return either the actual bitset or a copy
   */
  public BitSet getModelAtomBitSet(int modelIndex, boolean asCopy) {
    BitSet bs = (modelIndex < 0 ? bsAll : models[modelIndex].bsAtoms);
    if (bs == null)
      bs = bsAll = BitSetUtil.setAll(atomCount);
    return (asCopy ? BitSetUtil.copy(bs) : bs);
  }

  /**
   * general unqualified lookup of atom set type
   * 
   * @param tokType
   * @param specInfo
   * @return BitSet; or null if we mess up the type
   */
  public BitSet getAtomBits(int tokType, Object specInfo) {
    int[] info;
    BitSet bs;
    switch (tokType) {
    default:
      return super.getAtomBits(tokType, specInfo);
    case Token.molecule:
      return getMoleculeBitSet((BitSet) specInfo);
    case Token.boundbox:
      BoxInfo boxInfo = getBoxInfo((BitSet) specInfo, 1);
      bs = getAtomsWithin(boxInfo.getBoundBoxCornerVector().length() + 0.0001f,
          boxInfo.getBoundBoxCenter(), null, -1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (!boxInfo.isWithin(atoms[i]))
          bs.clear(i);
      return bs;
    case Token.spec_seqcode_range:
      info = (int[]) specInfo;
      int seqcodeA = info[0];
      int seqcodeB = info[1];
      char chainID = (char) info[2];
      bs = new BitSet();
      boolean caseSensitive = viewer.getChainCaseSensitive();
      if (!caseSensitive)
        chainID = Character.toUpperCase(chainID);
      for (int i = modelCount; --i >= 0;)
        models[i].selectSeqcodeRange(seqcodeA, seqcodeB, chainID, bs,
            caseSensitive);
      return bs;
    case Token.specialposition:
      bs = new BitSet(atomCount);
      int modelIndex = -1;
      int nOps = 0;
      for (int i = atomCount; --i >= 0;) {
        Atom atom = atoms[i];
        BitSet bsSym = atom.getAtomSymmetry();
        if (bsSym != null) {
          if (atom.modelIndex != modelIndex) {
            modelIndex = atom.modelIndex;
            if (getModelCellRange(modelIndex) == null)
              continue;
            nOps = getModelSymmetryCount(modelIndex);
          }
          // special positions are characterized by
          // multiple operator bits set in the first (overall)
          // block of nOpts bits.
          // only strictly true with load {nnn mmm 1}

          int n = 0;
          for (int j = nOps; --j >= 0;)
            if (bsSym.get(j))
              if (++n > 1) {
                bs.set(i);
                break;
              }
        }
      }
      return bs;
    case Token.symmetry:
      return BitSetUtil.copy(bsSymmetry == null ? bsSymmetry = new BitSet(
              atomCount)
              : bsSymmetry);
    case Token.unitcell:
      bs = new BitSet();
      SymmetryInterface unitcell = viewer.getCurrentUnitCell();
      if (unitcell == null)
        return bs;
      Point3f cell = new Point3f(1, 1, 1);
      for (int i = atomCount; --i >= 0;)
        if (isInLatticeCell(i, cell))
          bs.set(i);
      return bs;
    }
  }

  /**
   * Get atoms within a specific distance of any atom in a specific set of atoms
   * either within all models or within just the model(s) of those atoms
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
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        for (int iModel = modelCount; --iModel >= 0;) {
          if (!bsCheck.get(iModel))
            continue;
          if (distance < 0) {
            getAtomsWithin(-distance, atoms[i], bsResult, -1);
            getAtomsWithin(distance, atoms[i].getFractionalUnitCoord(true),
                bsResult, -1);
            continue;
          }
          AtomIndexIterator iterWithin = getWithinModelIterator(iModel,
              atoms[i], distance);
          while (iterWithin.hasNext())
            if ((iAtom = iterWithin.next()) >= 0
                && iterWithin.foundDistance2() <= d2)
              bsResult.set(iAtom);
        }
    } else {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (distance < 0) {
            getAtomsWithin(-distance, atoms[i], bsResult, atoms[i].modelIndex);
            getAtomsWithin(distance, atoms[i], bsResult, atoms[i].modelIndex);
            continue;
          }
          AtomIndexIterator iterWithin = getWithinModelIterator(atoms[i],
              distance);
          while (iterWithin.hasNext())
            if ((iAtom = iterWithin.next()) >= 0
                && iterWithin.foundDistance2() <= d2)
              bsResult.set(iAtom);
        }
    }
    return bsResult;
  }

  public BitSet getAtomsWithin(float distance, Point3f coord, BitSet bsResult,
                               int modelIndex) {

    if (bsResult == null)
      bsResult = new BitSet();

    if (distance < 0) { // just check all unitCell distances
      distance = -distance;
      final Point3f ptTemp1 = new Point3f();
      final Point3f ptTemp2 = new Point3f();
      for (int i = atomCount; --i >= 0;) {
        Atom atom = atoms[i];
        if (modelIndex >= 0 && atoms[i].modelIndex != modelIndex)
          continue;
        if (!bsResult.get(i)
            && atom.getFractionalUnitDistance(coord, ptTemp1, ptTemp2) <= distance)
          bsResult.set(atom.index);
      }
      return bsResult;
    }

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
    return bsResult;
  }
 
  public BitSet getSequenceBits(String specInfo, BitSet bs) {
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

  // ////////// Bonding methods ////////////

  public void deleteBonds(BitSet bsBonds, boolean isFullModel) {
    if (!isFullModel) {
      BitSet bsA = new BitSet();
      BitSet bsB = new BitSet();
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        bsA.clear();
        bsB.clear();
        bsA.set(bonds[i].getAtomIndex1());
        bsB.set(bonds[i].getAtomIndex2());
        addStateScript("connect ", null, bsA, bsB, "delete", false, true);
      }
    }
    super.deleteBonds(bsBonds, isFullModel);
  }

  protected int[] makeConnections(float minDistance, float maxDistance,
                                  int order, int connectOperation,
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
    boolean minDistanceIsFractionRadius = (minDistance < 0);
    boolean maxDistanceIsFractionRadius = (maxDistance < 0);
    if (minDistanceIsFractionRadius)
      minDistance = -minDistance;
    if (maxDistanceIsFractionRadius)
      maxDistance = -maxDistance;
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    Bond bondAB = null;
    int m = (isBonds ? 1 : atomCount);
    Atom atomA = null;
    Atom atomB = null;
    float dAB = 0;
    float dABcalc = 0;
    short newOrder = (short) (order | JmolConstants.BOND_NEW);
    for (int iA = bsA.nextSetBit(0); iA >= 0; iA = bsA.nextSetBit(iA + 1)) {
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
              && atomA.alternateLocationID != '\0'
              && atomB.alternateLocationID != '\0')
            continue;
          bondAB = atomA.getBond(atomB);
        }
        if (bondAB == null && (identifyOnly || modifyOnly) || bondAB != null
            && createOnly)
          continue;
        float distanceSquared = atomA.distanceSquared(atomB);
        if (minDistanceIsFractionRadius || maxDistanceIsFractionRadius) {
          dAB = atomA.distance(atomB);
          dABcalc = atomA.getBondingRadiusFloat() + atomB.getBondingRadiusFloat();
        }
        if ((minDistanceIsFractionRadius ? dAB < dABcalc * minDistance 
                 : distanceSquared < minDistanceSquared)
            || (maxDistanceIsFractionRadius ? dAB > dABcalc * maxDistance 
                 : distanceSquared > maxDistanceSquared))
          continue;       
        if (bondAB != null) {
          if (!identifyOnly && !matchAny) {
            bondAB.setOrder(order);
            bsAromatic.clear(bondAB.index);
          }
          if (!identifyOnly || matchAny 
              || order == bondAB.order || newOrder == bondAB.order  
              || matchHbond && bondAB.isHydrogen()) {
            bsBonds.set(bondAB.index);
            nModified++;
          }
        } else {
          bsBonds.set(
               bondAtoms(atomA, atomB, order, mad, bsBonds).index);
          nNew++;
        }
      }
    }
    if (autoAromatize)
      assignAromaticBonds(true, bsBonds);
    if (!identifyOnly)
      ((ModelSet)this).setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, null, bsBonds);
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
    if (showRebondTimes && Logger.debugging)
      Logger.startTimer();
    /*
     * miguel 2006 04 02 note that the way that these loops + iterators are
     * constructed, everything assumes that all possible pairs of atoms are
     * going to be looked at. for example, the hemisphere iterator will only
     * look at atom indexes that are >= (or <= ?) the specified atom. if we are
     * going to allow arbitrary sets bsA and bsB, then this will not work. so,
     * for now I will do it the ugly way. maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    boolean isAll = (bsA == null);
    BitSet bsCheck;
    int i0;
    if (isAll) {
      i0 = 0;
      bsCheck = null;
    } else {
      if (bsA.equals(bsB)) {
        bsCheck = bsA;
      } else {
        bsCheck = BitSetUtil.copy(bsA);
        bsCheck.or(bsB);
      }
      i0 = bsCheck.nextSetBit(0);
    }
    CubeIterator iter = null;
    for (int i = i0; i >= 0 && i < atomCount; i = (isAll ? i + 1 : bsCheck.nextSetBit(i + 1))) {
      boolean isAtomInSetA = (isAll || bsA.get(i));
      boolean isAtomInSetB = (isAll || bsB.get(i));
      Atom atom = atoms[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.modelIndex;
      // no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrame(modelIndex)) {
          i = models[modelIndex].firstAtomIndex + models[modelIndex].atomCount - 1;
          continue;
        }
        initializeBspt(modelIndex);
        iter = bspf.getCubeIterator(modelIndex);
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      boolean isFirstExcluded = (bsExclude != null && bsExclude.get(i));
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom || atomNear.isDeleted())
          continue;
        int atomIndexNear = atomNear.index;
        boolean isNearInSetA = (isAll || bsA.get(atomIndexNear));
        boolean isNearInSetB = (isAll || bsB.get(atomIndexNear));
        // BOTH must be excluded in order to ignore bonding
        if (!isNearInSetA && !isNearInSetB 
            || !(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA)
            || isFirstExcluded && bsExclude.get(atomIndexNear))
          continue;
        short order = getBondOrder(atom, myBondingRadius, atomNear, atomNear
            .getBondingRadiusFloat(), iter.foundDistance2(), minBondDistance2,
            bondTolerance);
        if (order > 0) {
          if (checkValencesAndBond(atom, atomNear, order, mad, bsBonds))
            nNew++;
        }
      }
      iter.release();
    }
    if (showRebondTimes && Logger.debugging)
      Logger.checkTimer("Time to autoBond");
    return nNew;
  }

  private int[] autoBond(int order, BitSet bsA, BitSet bsB, BitSet bsBonds,
                         boolean isBonds, boolean matchHbond) {
    if (isBonds) {
      BitSet bs = bsA;
      bsA = new BitSet();
      bsB = new BitSet();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        bsA.set(bonds[i].atom1.index);
        bsB.set(bonds[i].atom2.index);
      }
    }
    if (matchHbond) {
      initializeBspf();
      return new int[] { autoHbond(bsA, bsB, bsBonds, 0, 0), 0 };
    }
    return new int[] { autoBond(bsA, bsB, null, bsBonds), 0 };
  }
  
  private static float defaultHbondMax = 3.25f;
  private static float hbondMin = 2.5f;

  private static boolean checkMinAttachedAngle(Atom atom1, Atom atom2, float minAngle, Vector3f v1, Vector3f v2) {
    v1.sub(atom1, atom2);
    return (checkMinAttachedAngle(atom1, atom1.getBonds(), atom2, minAngle, v1, v2)
        && checkMinAttachedAngle(atom2, atom2.getBonds(), atom1, minAngle, v1, v2));
  }

  private static boolean checkMinAttachedAngle(Atom atom1, Bond[] bonds1, Atom atom2,
                                        float minAngle, Vector3f v1, Vector3f v2) {
    if (bonds1 != null)
      for (int i = bonds1.length; --i >= 0;)
        if (bonds1[i].isCovalent()) {
          v2.sub(atom1, bonds1[i].getOtherAtom(atom1));
          if (v2.angle(v1) < minAngle)
            return false;
        }
    v1.scale(-1); // set for second check
    return true;
  }

  protected int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds,
                          float maxXYDistance, float minAttachedAngle) {
    if (maxXYDistance <= 0)
      maxXYDistance = defaultHbondMax;
    float hbondMax2 = maxXYDistance * maxXYDistance;
    float hbondMin2 = hbondMin * hbondMin;
    int nNew = 0;
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    if (showRebondTimes && Logger.debugging)
      Logger.startTimer();
    int modelLast = -1;
    BitSet bsCO = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].getSpecialAtomID() == JmolConstants.ATOMID_CARBONYL_OXYGEN)
        bsCO.set(i);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      int elementNumber = atom.getElementNumber();
      if (elementNumber != 7 && elementNumber != 8)
        continue;      
      boolean firstIsCO = bsCO.get(i);
      //float searchRadius = hbondMax;
      if (atom.modelIndex != modelLast)
          initializeBspt(modelLast = atom.modelIndex);
      CubeIterator iter = bspf.getCubeIterator(atom.modelIndex);
      iter.initializeHemisphere(atom, maxXYDistance);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        int elementNumberNear = atomNear.getElementNumber();
        if (elementNumberNear != 7 && elementNumberNear != 8
            || atomNear == atom || iter.foundDistance2() < hbondMin2
            || iter.foundDistance2() > hbondMax2 || atom.isBonded(atomNear)
            || firstIsCO && bsCO.get(atomNear.index))
          continue;
        if (minAttachedAngle > 0
            && !checkMinAttachedAngle(atom, atomNear, minAttachedAngle, v1, v2))
          continue;
        getOrAddBond(atom, atomNear, JmolConstants.BOND_H_REGULAR, (short) 1,
            bsPseudoHBonds);
        nNew++;
      }
      iter.release();
    }
    ((ModelSet)this).setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, null, 
        bsPseudoHBonds);
    if (showRebondTimes && Logger.debugging)
      Logger.checkTimer("Time to hbond");
    return nNew;
  }


  //////////// state definition ///////////

  boolean proteinStructureTainted = false;
  
  void setStructureIds() {
    int id;
    int idnew = 0;
    int lastid = -1;
    int imodel = -1;
    int lastmodel = -1;
    for (int i = 0; i < atomCount; i++) {
      if ((imodel = atoms[i].modelIndex) != lastmodel) {
        idnew = 0;
        lastmodel = imodel;
        lastid = -1;
      }
      if ((id = atoms[i].getStrucNo()) != lastid 
               && id != 0) {
        atoms[i].getGroup().setProteinStructureId(++idnew);
        lastid = idnew;
      }
    }
  }
  

  public String getProteinStructureState(BitSet bsAtoms, boolean taintedOnly,
                                         boolean needPhiPsi, boolean pdbFormat) {
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
    String sid = "";
    String group1 = "";
    String group2 = "";
    String chain1 = "";
    String chain2 = "";
    int n = 0;
    int nHelix = 0;
    int nTurn = 0;
    int nSheet = 0;
    BitSet bsTainted = null;
    if (taintedOnly) {
      if (!proteinStructureTainted)
        return "";
      bsTainted = new BitSet();
      for (int i = 0; i < atomCount; i++)
        if (models[atoms[i].modelIndex].isStructureTainted())
          bsTainted.set(i);
      bsTainted.set(atomCount);
    }
    for (int i = 0; i <= atomCount; i++)
      if (i == atomCount || bsAtoms == null || bsAtoms.get(i)) {
        if (taintedOnly && !bsTainted.get(i))
          continue;
        id = 0;
        if (i == atomCount || (id = atoms[i].getStrucNo()) != lastId) {
          if (bs != null) {
            if (itype == JmolConstants.PROTEIN_STRUCTURE_HELIX
                || itype == JmolConstants.PROTEIN_STRUCTURE_TURN
                || itype == JmolConstants.PROTEIN_STRUCTURE_SHEET) {
              n++;
              if (bsAtoms == null) {
                int iModel = atoms[iLastAtom].modelIndex;
                cmd.append("  structure ").append(
                    JmolConstants.getProteinStructureName(itype)).append(" ")
                    .append(Escape.escape(bs)).append("    \t# model=").append(
                        getModelNumberDotted(iModel)).append(" & (").append(
                        res1).append(" - ").append(res2).append(");\n");
              } else {
                String str;
                int nx;
                StringBuffer sb;
                // NNN III GGG C RRRR GGG C RRRR
                // HELIX 99 99 LYS F 281 LEU F 293 1
                // NNN III 2 GGG CRRRR GGG CRRRR
                // SHEET 1 A 8 ILE A 43 ASP A 45 0
                // NNN III GGG CRRRR GGG CRRRR
                // TURN 1 T1 PRO A 41 TYR A 44
                switch (itype) {
                case JmolConstants.PROTEIN_STRUCTURE_HELIX:
                  nx = ++nHelix;
                  if (sid == null || pdbFormat)
                    sid = TextFormat.formatString("%3N %3N", "N", nx);
                  str = "HELIX  %ID %3GROUPA %1CA %4RESA  %3GROUPB %1CB %4RESB";
                  sb = sbHelix;
                  break;
                case JmolConstants.PROTEIN_STRUCTURE_SHEET:
                  nx = ++nSheet;
                  if (sid == null || pdbFormat) {
                    sid = TextFormat.formatString("%3N %3A 0", "N", nx);
                    sid = TextFormat.formatString(sid, "A", "S" + nx);
                  }
                  str = "SHEET  %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
                  sb = sbSheet;
                  break;
                case JmolConstants.PROTEIN_STRUCTURE_TURN:
                default:
                  nx = ++nTurn;
                  if (sid == null || pdbFormat)
                    sid = TextFormat.formatString("%3N %3N", "N", nx);
                  str = "TURN   %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
                  sb = sbTurn;
                  break;
                }
                str = TextFormat.formatString(str, "ID", sid);
                str = TextFormat.formatString(str, "GROUPA", group1);
                str = TextFormat.formatString(str, "CA", chain1);
                str = TextFormat.formatString(str, "RESA", res1);
                str = TextFormat.formatString(str, "GROUPB", group2);
                str = TextFormat.formatString(str, "CB", chain2);
                str = TextFormat.formatString(str, "RESB", res2);
                sb.append(str);
                if (!pdbFormat)
                  sb.append(" strucno= ").append(lastId);
                sb.append("\n");

                /*
                 * HELIX 1 H1 ILE 7 PRO 19 1 3/10 CONFORMATION RES 17,19 1CRN 55
                 * HELIX 2 H2 GLU 23 THR 30 1 DISTORTED 3/10 AT RES 30 1CRN 56
                 * SHEET 1 S1 2 THR 1 CYS 4 0 1CRNA 4 SHEET 2 S1 2 CYS 32 ILE 35
                 */
              }
            }
            bs = null;
          }
          if (id == 0
              || bsAtoms != null
              && needPhiPsi
              && (Float.isNaN(atoms[i].getGroupPhi()) || Float.isNaN(atoms[i]
                  .getGroupPsi())))
            continue;
        }
        char ch = atoms[i].getChainID();
        if (ch == 0)
          ch = ' ';
        if (bs == null) {
          bs = new BitSet();
          res1 = atoms[i].getResno();
          group1 = atoms[i].getGroup3(false);
          chain1 = "" + ch;
        }
        itype = atoms[i].getProteinStructureType();
        sid = atoms[i].getProteinStructureTag();
        bs.set(i);
        lastId = id;
        res2 = atoms[i].getResno();
        group2 = atoms[i].getGroup3(false);
        chain2 = "" + ch;
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
    boolean ishbond = (intType == JmolConstants.BOND_HYDROGEN_MASK);
    boolean isall = (intType == JmolConstants.BOND_ORDER_ANY);
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = bonds[ibond];
      if (isall || bond.is(intType) || ishbond && bond.isHydrogen()) {
        if (bs.get(bond.atom1.index)) {
          nBonded[i = bond.atom2.index]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.index)) {
          nBonded[i = bond.atom1.index]++;
          bsResult.set(i);
        }
      }
    }
    boolean nonbonded = (min == 0);
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

    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      atomMap[i] = ++nAtoms;
      getAtomRecordMOL(s, i);
    }
    for (int i = 0; i < bondCount; i++) {
      Bond bond = bonds[i];
      if (bs.get(bond.atom1.index) && bs.get(bond.atom2.index)) {
        if (!bond.isHydrogen()) {
          getBondRecordMOL(s, i, atomMap);
          nBonds++;
        }
      }
    }
    if (nAtoms > 999 || nBonds > 999) {
      Logger.error("ModelManager.java::getModel: ERROR atom/bond overflow");
      return "";
    }
    // 21 21 0 0 0
    TextFormat.rFill(mol, "   ", "" + nAtoms);
    TextFormat.rFill(mol, "   ", "" + nBonds);
    mol.append("  0  0  0\n");
    mol.append(s);
    return mol.toString();
  }
  
  private void getAtomRecordMOL(StringBuffer s, int i){
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomX(i),9));
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomY(i),9));
    TextFormat.rFill(s, "          " ,TextFormat.safeTruncate(getAtomZ(i),9));
    s.append(" ").append((getElementSymbol(i) + "  ").substring(0,2)).append("\n");
  }

  private void getBondRecordMOL(StringBuffer s, int i,int[] atomMap){
  //  1  2  1
    Bond b = bonds[i];
    TextFormat.rFill(s, "   ","" + atomMap[b.atom1.index]);
    TextFormat.rFill(s, "   ","" + atomMap[b.atom2.index]);
    int order = b.getValence();
    if (order > 3)
      order = 1;
    switch (b.order & ~JmolConstants.BOND_NEW) {
    case JmolAdapter.ORDER_AROMATIC:
      order = 4;
      break;
    case JmolAdapter.ORDER_PARTIAL12:
      order = 5;
      break;
    case JmolAdapter.ORDER_AROMATIC_SINGLE:
      order = 6;
      break;
    case JmolAdapter.ORDER_AROMATIC_DOUBLE:
      order = 7;
      break;
    }
    s.append("  ").append(order).append("\n"); 
  }
  
  public String getChimeInfo(int tok, BitSet bs) {
    if (tok != Token.info)
      return super.getChimeInfo(tok, bs);
    int n = 0;
    StringBuffer sb = new StringBuffer();
    int nHetero = 0;
    if (models[0].isPDB) {
      sb.append("\nMolecule name ....... "
              + getModelSetAuxiliaryInfo("COMPND"));
      sb.append("\nSecondary Structure . PDB Data Records");
      sb.append("\nBrookhaven Code ..... " + modelSetName);
      for (int i = modelCount; --i >= 0;)
        n += models[i].getChainCount(false);
      sb.append("\nNumber of Chains .... " + n);
      n = 0;
      for (int i = modelCount; --i >= 0;)
        n += models[i].getGroupCount(false);
      nHetero = 0;
      for (int i = modelCount; --i >= 0;)
        nHetero += models[i].getGroupCount(true);
      sb.append("\nNumber of Groups .... " + n);
      if (nHetero > 0)
        sb.append(" (" + nHetero + ")");
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isHetero())
          nHetero++;
    }
    sb.append("\nNumber of Atoms ..... " + (atomCount - nHetero));
    if (nHetero > 0)
      sb.append(" (" + nHetero + ")");
    sb.append("\nNumber of Bonds ..... " + bondCount);
    sb.append("\nNumber of Models ...... " + modelCount);
    if (models[0].isPDB) {
      int nH = 0;
      int nS = 0;
      int nT = 0;
      if (structures != null)
        for (int i = structureCount; --i >= 0;)
          switch (structures[i].type) {
          case JmolConstants.PROTEIN_STRUCTURE_HELIX:
            nH++;
            break;
          case JmolConstants.PROTEIN_STRUCTURE_SHEET:
            nS++;
            break;
          case JmolConstants.PROTEIN_STRUCTURE_TURN:
            nT++;
            break;
          }
      sb.append("\nNumber of Helices ... " + nH);
      sb.append("\nNumber of Strands ... " + nS);
      sb.append("\nNumber of Turns ..... " + nT);
    }
    return sb.append('\n').toString().substring(1);
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
  
  public Hashtable getAuxiliaryInfo(BitSet bsModels) {
    Hashtable info = getModelSetAuxiliaryInfo();
    if (info == null)
      return info;
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      if (bsModels != null && !bsModels.get(i))
        continue;
      Hashtable modelinfo = getModelAuxiliaryInfo(i);
      models.addElement(modelinfo);
    }
    info.put("models",models);
    return info;
  }

  public Vector getAllAtomInfo(BitSet bs) {
    Vector V = new Vector();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      V.addElement(getAtomInfoLong(i));
    return V;
  }

  public void getAtomIdentityInfo(int i, Hashtable info) {
    info.put("_ipt", new Integer(i));
    info.put("atomIndex", new Integer(i));
    info.put("atomno", new Integer(getAtomNumber(i)));
    info.put("info", getAtomInfo(i, null));
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
    info.put("visible", Boolean.valueOf(atoms[i].isVisible(0)));
    info.put("clickabilityFlags", new Integer(atom.clickabilityFlags));
    info.put("visibilityFlags", new Integer(atom.shapeVisibilityFlags));
    info.put("spacefill", new Float(atom.getRadius()));
    String strColor = Escape.escapeColor(viewer.getColorArgbOrGray(atom.colixAtom));
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
    if (models[atom.modelIndex].isPDB) {
      info.put("resname", atom.getGroup3(false));
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
      info.put("occupancy", new Integer(atom.getOccupancy100()));
      int temp = atom.getBfactor100();
      info.put("temp", new Integer(temp / 100));
    }
    return info;
  }  

  public Vector getAllBondInfo(BitSet bs) {
    Vector V = new Vector();
    int thisAtom = (bs.cardinality() == 1 ? bs.nextSetBit(0) : -1);
    for (int i = 0; i < bondCount; i++)
      if (thisAtom >= 0? (bonds[i].atom1.index == thisAtom || bonds[i].atom2.index == thisAtom) 
          : bs.get(bonds[i].atom1.index) && bs.get(bonds[i].atom2.index)) 
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
    getAtomIdentityInfo(atom1.index, infoA);
    Hashtable infoB = new Hashtable();
    getAtomIdentityInfo(atom2.index, infoB);
    info.put("atom1",infoA);
    info.put("atom2",infoB);
    info.put("order", new Float(JmolConstants.getBondOrderNumberFromOrder(bonds[i].order)));
    info.put("radius", new Float(bond.mad/2000.));
    info.put("length_Ang",new Float(atom1.distance(atom2)));
    info.put("visible", Boolean.valueOf(bond.shapeVisibilityFlags != 0));
    String strColor = Escape.escapeColor(viewer.getColorArgbOrGray(bond.colix));
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
    int nChains = model.getChainCount(true);
    Vector infoChains = new Vector();    
    for(int i = 0; i < nChains; i++) {
      Chain chain = model.getChain(i);
      Vector infoChain = new Vector();
      int nGroups = chain.getGroupCount();
      Hashtable arrayName = new Hashtable();
      for (int igroup = 0; igroup < nGroups; igroup++) {
        Group group = chain.getGroup(igroup);
        if (!bs.get(group.firstAtomIndex)) 
          continue;
        Hashtable infoGroup = new Hashtable();
        infoGroup.put("groupIndex", new Integer(igroup));
        infoGroup.put("groupID", new Short(group.getGroupID()));
        String s = group.getSeqcodeString();
        if (s != null)
          infoGroup.put("seqCode", s);
        infoGroup.put("_apt1", new Integer(group.firstAtomIndex));
        infoGroup.put("_apt2", new Integer(group.lastAtomIndex));
        infoGroup.put("atomInfo1", getAtomInfo(group.firstAtomIndex, null));
        infoGroup.put("atomInfo2", getAtomInfo(group.lastAtomIndex, null));
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
    if (unitCells == null)
      return "not applicable";
    return unitCells[modelIndex].getUnitCellInfo();
  }

  private SymmetryInterface symTemp;

  public Hashtable getSpaceGroupInfo(int modelIndex, String spaceGroup, 
                                     int symOp, Point3f pt1, Point3f pt2, String drawID) {
    String strOperations = null;
    Hashtable info = null;
    SymmetryInterface cellInfo = null;
    Object[][] infolist = null;
    if (spaceGroup == null) {
      if (modelIndex <= 0)
        modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        strOperations = "no single current model";
      else if (unitCells == null || unitCells[modelIndex] == null)
        strOperations = "not applicable";
      if (strOperations != null) {
        info = new Hashtable();
        info.put("spaceGroupInfo", strOperations);
        info.put("symmetryInfo", "");
        return info;
      }
      if (pt1 == null && drawID == null && symOp != 0)
        info = (Hashtable) getModelAuxiliaryInfo(modelIndex, "spaceGroupInfo");
      if (info != null)
        return info;
      info = new Hashtable();
      if (pt1 == null && drawID == null)
        setModelAuxiliaryInfo(modelIndex, "spaceGroupInfo", info);
      cellInfo = unitCells[modelIndex];
      spaceGroup = cellInfo.getSpaceGroupName();
      String[] list = unitCells[modelIndex].getSymmetryOperations();
      if (list == null) {
        strOperations = "\n no symmetry operations employed";
      } else {
        getSymTemp(true);
        symTemp.setSpaceGroup(false);
        strOperations = "\n" + list.length + " symmetry operations employed:";
        infolist = new Object[list.length][];
        for (int i = 0; i < list.length; i++) {
          int iSym = symTemp.addSpaceGroupOperation("=" + list[i], i + 1);
          if (iSym < 0)
            continue;
          infolist[i] = (symOp > 0 && symOp - 1 != iSym ? null 
              : symTemp.getSymmetryOperationDescription(iSym, cellInfo, pt1, pt2, drawID));
          if (infolist[i] != null)
            strOperations += "\n" + (i + 1) + "\t" + infolist[i][0] + "\t"
                + infolist[i][2];
        }
      }
    } else {
      info = new Hashtable();
    }
    info.put("spaceGroupName", spaceGroup);
    getSymTemp(true);
    String data = symTemp.getSpaceGroupInfo(spaceGroup, cellInfo);
    if (infolist != null) {
      info.put("operations", infolist);
      info.put("symmetryInfo", strOperations);
    }
    if (data == null)
      data = "could not identify space group from name: " + spaceGroup
          + "\nformat: show spacegroup \"2\" or \"P 2c\" "
          + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"";
    info.put("spaceGroupInfo", data);
    return info;
  }
  
  public Object getSymmetryInfo(BitSet bsAtoms, String xyz, int op, Point3f pt, 
                                Point3f pt2, String id, int type) {
    int iModel = -1;
    if (bsAtoms == null) {
      iModel = viewer.getCurrentModelIndex();
      if (iModel < 0)
        return "";
      bsAtoms = getModelAtomBitSet(iModel, false);
    }
    int iAtom = bsAtoms.nextSetBit(0);
    if (iAtom < 0)
      return "";
    iModel = atoms[iAtom].modelIndex;
    SymmetryInterface uc = getUnitCell(iModel);
    if (uc == null)
      return "";
    if (pt2 != null)
      return getSymmetryOperation(iModel, null, op, pt, pt2, (id == null ? "sym" : id));
    if (xyz == null) {
      String[] ops = uc.getSymmetryOperations();
      if (ops == null || op == 0 || Math.abs(op) > ops.length)
        return "";
      if (op > 0) {
        xyz = ops[op - 1 ];
      } else {
        xyz = ops[-1 - op];
      }
    } else {
      op = 0;
    }
    getSymTemp(false); 
    symTemp.setSpaceGroup(false);
    int iSym = symTemp.addSpaceGroupOperation((op < 0 ? "!" : "=") + xyz, Math.abs(op));
    if (iSym < 0)
      return "";
    symTemp.setUnitCell(uc.getNotionalUnitCell());
    Object[] info;
    pt = new Point3f(pt == null ? atoms[iAtom] : pt);
    if (type == Token.point) {
      uc.toFractional(pt);
      if (Float.isNaN(pt.x))
        return "";
      Point3f sympt = new Point3f();
      symTemp.newSpaceGroupPoint(iSym, pt, sympt, 0, 0, 0);
      symTemp.toCartesian(sympt);
      return sympt;
    }
    // null id means "array info only" but here we want the draw commands
    info = symTemp.getSymmetryOperationDescription(iSym, uc, pt, pt2,
        (id == null ? "sym" : id));
    int ang = ((Integer)info[9]).intValue();
    /*
     *  xyz (Jones-Faithful calculated from matrix)
     *  xyzOriginal (Provided by operation) 
     *  description ("C2 axis", for example) 
     *  translation vector (fractional)  
     *  translation vector (cartesian)
     *  inversion point 
     *  axis point 
     *  axis vector
     *  angle of rotation
     *  matrix representation
     */
    switch (type) {
    case Token.array:
      return info;
    case Token.list:
      String[] sinfo = new String[] {
          (String) info[0],
          (String) info[1],
          (String) info[2],
          // skipping DRAW commands here
          Escape.escape((Vector3f)info[4]),
          Escape.escape((Vector3f)info[5]),
          Escape.escape((Point3f)info[6]),
          Escape.escape((Point3f)info[7]),
          Escape.escape((Vector3f)info[8]),
          "" + info[9],
          "" + Escape.escape((Matrix4f)info[10])
        };
        return sinfo;
    case Token.info:
      return info[0];
    default:
    case Token.label:
      return info[2];
    case Token.draw:
      return info[3];
    case Token.translation: 
      // skipping fractional translation
      return info[5]; // cartesian translation
    case Token.center:
      return info[6];
    case Token.point:
      return info[7];
    case Token.axis:
    case Token.plane:
      return ((ang == 0) == (type == Token.plane)? (Vector3f) info[8] : null);
    case Token.angle:
      return info[9];
    case Token.matrix4f:
      return info[10];
    } 
  }

  private void getSymTemp(boolean forceNew) {
    if (symTemp == null || forceNew)
      symTemp = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
  }

  protected void deleteModel(int modelIndex, int firstAtomIndex, int nAtoms,
                          BitSet bsAtoms, BitSet bsBonds) {
    /*
     *   ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
     * ModelCollection.stateScripts ?????
     */
    if (modelIndex < 0) {
      //final deletions
      bspf = null;
      bsAll = null;
      molecules = null;
      withinModelIterator = null;
      withinAtomSetIterator = null;
      isBbcageDefault = false;
      calcBoundBoxDimensions(null, 1);
      return;
    }
    
    modelNumbers = (int[]) ArrayUtil
        .deleteElements(modelNumbers, modelIndex, 1);
    modelFileNumbers = (int[]) ArrayUtil.deleteElements(modelFileNumbers,
        modelIndex, 1);
    modelNumbersForAtomLabel = (String[]) ArrayUtil.deleteElements(
        modelNumbersForAtomLabel, modelIndex, 1);
    modelNames = (String[]) ArrayUtil.deleteElements(modelNames, modelIndex, 1);
    frameTitles = (String[]) ArrayUtil.deleteElements(frameTitles, modelIndex,
        1);
    thisStateModel = -1;
    int nDeleted = 0;
    for (int i = structureCount; --i >= 0;) {
      if (structures[i].modelIndex > modelIndex) {
        structures[i].modelIndex--;
      } else if (structures[i].modelIndex == modelIndex) {
        structures = (Structure[]) ArrayUtil.deleteElements(structures, i, 1);
        nDeleted++;
      } else {
        break;
      }
    }
    structureCount -= nDeleted;
    String[] group3Lists = (String[]) getModelSetAuxiliaryInfo("group3Lists");
    int[][] group3Counts = (int[][]) getModelSetAuxiliaryInfo("group3Counts");
    int ptm = modelIndex + 1;
    if (group3Lists != null && group3Lists[ptm] != null) {
      for (int i = group3Lists[ptm].length() / 6; --i >= 0;)
        if (group3Counts[ptm][i] > 0) {
          group3Counts[0][i] -= group3Counts[ptm][i];
          if (group3Counts[0][i] == 0)
            group3Lists[0] = group3Lists[0].substring(0, i * 6) + ",["
                + group3Lists[0].substring(i * 6 + 2);
        }
    }
    if (group3Lists != null) {
      modelSetAuxiliaryInfo.put("group3Lists", ArrayUtil.deleteElements(
          group3Lists, modelIndex, 1));
      modelSetAuxiliaryInfo.put("group3Counts", ArrayUtil.deleteElements(
          group3Counts, modelIndex, 1));
    }

    //fix cellInfos array
    if (unitCells != null) {
      for (int i = modelCount; --i > modelIndex;)
        unitCells[i].setModelIndex(unitCells[i].getModelIndex() - 1);
      unitCells = (SymmetryInterface[]) ArrayUtil.deleteElements(unitCells, modelIndex,
          1);
    }

    // correct stateScripts, particularly CONNECT scripts
    for (int i = stateScripts.size(); --i >= 0;)
      if (!((StateScript) stateScripts.get(i)).deleteAtoms(modelIndex, bsBonds,
          bsAtoms))
        stateScripts.removeElementAt(i);
    
    // set to recreate bounding box
    deleteModelAtoms(firstAtomIndex, nAtoms, bsAtoms);
    viewer.deleteModelAtoms(firstAtomIndex, nAtoms, bsAtoms);
  }

  public String getMoInfo(int modelIndex) {
    StringBuffer sb = new StringBuffer();
    for (int m = 0; m < modelCount; m++) {
      if (modelIndex >= 0 && m != modelIndex)
        continue;
      Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(m, "moData");
      if (moData == null)
        continue;
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        continue;
      for (int i = nOrb; --i >= 0; ) {
        Hashtable mo = (Hashtable) mos.get(i);
        String type = (String) mo.get("type");
        if (type == null)
          type = "";
        String units = (String) mo.get("energyUnits");
        if (units == null)
          units = "";
        Float occ = (Float) mo.get("occupancy");
        if (occ != null)
          type = "occupancy " + occ.floatValue() + " " + type;
        String sym = (String) mo.get("symmetry");
        if (sym != null) 
          type += sym;
        sb.append(TextFormat.sprintf(
            "model %-2s;  mo %-2i # energy %-8.3f %s %s\n", new Object[] {
                getModelNumberDotted(m), new Integer(i + 1),
                mo.get("energy"), units, type }));
      }
    }
    return sb.toString();
  }

}
