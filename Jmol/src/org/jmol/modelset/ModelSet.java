/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

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

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomIndexIterator;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.SphereIterator;
import org.jmol.shape.Closest;
import org.jmol.shape.Dipoles;
import org.jmol.shape.Labels;
import org.jmol.shape.Shape;
import org.jmol.symmetry.UnitCell;

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.awt.Rectangle;


/*
 * An abstract class always created using new ModelLoader(...)
 * 
 * ModelLoader simply pulls out all private classes that are
 * necessary only for file loading (and structure recalculation).
 * 
 * What is left here are all the methods that are 
 * necessary AFTER a model is loaded, when it is being 
 * accessed by ModelManager or other classes.
 * 
 * Please:
 * 
 * 1) designate any methods accessed only by ModelManager as default
 * 2) designate any methods accessed only by ModelLoader as protected
 * 3) designate any methods used only here as private
 * 
 * methods needing access outside this package, of course, are designated public
 * 
 * Bob Hanson, 5/2007
 * 
 */
abstract public class ModelSet {

  Viewer viewer;
  Mmset mmset;
  Graphics3D g3d;
  
  protected String modelSetTypeName;
  protected boolean isXYZ;
  protected boolean isPDB;
  protected Vector trajectories;

  boolean isPDB() {
    return isPDB;
  }
  
  protected boolean isZeroBased;

  void setZeroBased() {
    isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
  }

  CellInfo[] cellInfos;
  
  public CellInfo[] getCellInfos() {
    return cellInfos;
  }
  
  public Atom[] atoms;
  int atomCount;
  String[] atomNames;

  
  Atom[] getAtoms() {
    return atoms;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public int getAtomCount() {
    return atomCount;
  }
  
  public String[] getAtomNames() {
    return atomNames;
  }
  
  private final AtomIteratorWithinModel withinModelIterator = new AtomIteratorWithinModel();

  AtomIterator getWithinModelIterator(Atom atomCenter, float radius) {
    //Polyhedra
    initializeBspf();
    withinModelIterator.initialize(bspf, atomCenter.modelIndex, atomCenter, radius);
    return withinModelIterator;
  }

  private final AtomIteratorWithinSet withinAtomSetIterator = new AtomIteratorWithinSet();

  AtomIndexIterator getWithinAtomSetIterator(int atomIndex, float distance, BitSet bsSelected, boolean isGreaterOnly, boolean modelZeroBased) {
    //EnvelopeCalculation, IsoSolventReader
    initializeBspf();
    withinAtomSetIterator.initialize(this, bspf, atoms[atomIndex].modelIndex, atomIndex, distance, bsSelected, isGreaterOnly, modelZeroBased);
    return withinAtomSetIterator;
  }
  
  Bond[] bonds;
  int bondCount;
  
  public Bond[] getBonds() {
    return bonds;
  }

  public Bond getBondAt(int bondIndex) {
    return bonds[bondIndex];
  }

  /**
   * not necessarily the REAL bond count; this is an ARRAY MAXIMUM
   * 
   * @return  SIZE OF BOND ARRAY
   */
  public int getBondCount() {
    return bondCount;
  }
  
  public BondIterator getBondIterator(short bondType, BitSet bsSelected) {
    //Dipoles, Sticks
    return new BondIteratorSelected(this, bondType, bsSelected,   viewer.getBondSelectionModeOr());
  }

  public BondIterator getBondIterator(BitSet bsSelected) {
    //Sticks
    return new BondIteratorSelected(this, bsSelected);
  }

  //note: Molecules is set up to only be calculated WHEN NEEDED
  protected Molecule[] molecules = new Molecule[4];
  protected int moleculeCount;

  int getMoleculeCount() {
    return moleculeCount;
  }

  protected int modelCount;
  
  public int getModelCount() {
    return modelCount;
  }

  protected final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];
  
  private Shape allocateShape(int shapeID) {
    if (shapeID == JmolConstants.SHAPE_HSTICKS || shapeID == JmolConstants.SHAPE_SSSTICKS)
      return null;
    String className = JmolConstants.getShapeClassName(shapeID);
    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape) shapeClass.newInstance();
      shape.initializeShape(viewer, g3d, this, shapeID);
      return shape;
    } catch (Exception e) {
      Logger.error("Could not instantiate shape:" + className, e);
    }
    return null;
  }

  public Shape getShape(int i) {
    //FrameRenderer
    return shapes[i];
  }

  protected final Point3f averageAtomPoint = new Point3f();

  Point3f getAverageAtomPoint() {
    return averageAtomPoint;
  }

  protected final Point3f centerBoundBox = new Point3f();

  Point3f getBoundBoxCenter() {
    return centerBoundBox;
  }

  protected final Vector3f boundBoxCornerVector = new Vector3f();

  Vector3f getBoundBoxCornerVector() {
    return boundBoxCornerVector;
  }

  protected final Point3f[] bboxVertices = new Point3f[8];

  public Point3f[] getBboxVertices() {
    return bboxVertices;
  }

  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  int[] atomSerials;
  byte[] specialAtomIDs;
  Object[] clientAtomReferences;
  Vector3f[] vibrationVectors;
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  
  float[] getPartialCharges() {
    return partialCharges;
  }

  protected int[] surfaceDistance100s;

  private BitSet bsHidden = new BitSet();

  public void setBsHidden(BitSet bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  ////////////////////////////////////////////////////////////////

  /**
   * deprecated due to multimodel issues, 
   * but required by an interface -- do NOT remove.
   * 
   * @return      just the first unit cell
   * 
   */
  public float[] getNotionalUnitcell() {
    return (cellInfos == null || cellInfos[0] == null ? null : cellInfos[0].getNotionalUnitCell());
  }

  //new way:
  
  protected boolean someModelsHaveSymmetry;
  
  boolean haveSymmetry() {
    return someModelsHaveSymmetry;
  }
  
  public boolean modelSetHasVibrationVectors(){
    return (vibrationVectors != null);
  }
  
  //variables that will be reset when a new frame is instantiated

  private boolean selectionHaloEnabled = false;
  private boolean echoShapeActive = false;

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
    }
  }

  boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  boolean getEchoStateActive() {
    return echoShapeActive;
  }

  public void setEchoStateActive(boolean TF) {
    echoShapeActive = TF;
  }


  protected BitSet[] elementsPresent;

  ////  atom coordinate changing  //////////
  
  private BitSet tainted;  // not final -- can be set to null

  BitSet getTaintedAtoms() {
    return tainted;
  }
  
  private void taint(int atomIndex) {
    if (tainted == null)
      tainted = new BitSet(atomCount);
    tainted.set(atomIndex);
  }

  void setTaintedAtoms(BitSet bs) {
    if (bs == null) {
      tainted = null;
      return;
    }
    if (tainted == null)
      tainted = new BitSet(atomCount);
    BitSetUtil.copy(bs, tainted);
  }

  Bspf bspf;

  void loadCoordinates(String data) {
    bspf = null;
    int[] lines = Parser.markLines(data, ';');
    try {
      int nData = Parser.parseInt(data.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(data.substring(
            lines[i], lines[i + 1])));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        float x = Parser.parseFloat(tokens[3]);
        float y = Parser.parseFloat(tokens[4]);
        float z = Parser.parseFloat(tokens[5]);
        setAtomCoord(atomIndex, x, y, z);
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }

  void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x = x;
    atoms[atomIndex].y = y;
    atoms[atomIndex].z = z;
    taint(atomIndex);
  }

  void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x += x;
    atoms[atomIndex].y += y;
    atoms[atomIndex].z += z;
    taint(atomIndex);
  }

  void setAtomCoordRelative(BitSet atomSet, float x, float y, float z) {
    bspf = null;
    for (int i = atomCount; --i >= 0;)
      if (atomSet.get(i))
        setAtomCoordRelative(i, x, y, z);
  }

  private final Matrix3f matTemp = new Matrix3f();
  private final Matrix3f matInv = new Matrix3f();
  private final Point3f ptTemp = new Point3f();

  void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate, BitSet bsInput,
                      boolean fullMolecule, boolean isInternal) {
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
        taint(i);
        n++;
      }
    if (isInternal)
      return;
    ptTemp.scale(1f / n);
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i))
        atoms[i].add(ptTemp);
  }

  BitSet getMoleculeBitSet(BitSet bs) {
    // returns cumulative sum of all atoms in molecules containing these atoms
    if (moleculeCount == 0)
      getMolecules();
    BitSet bsResult = (BitSet) bs.clone();
    BitSet bsInitial = (BitSet) bs.clone();
    int iLastBit;
    while ((iLastBit = BitSetUtil.length(bsInitial)) > 0) {
      bsTemp = getMoleculeBitSet(iLastBit - 1);
      BitSetUtil.andNot(bsInitial, bsTemp);
      bsResult.or(bsTemp);
    }
    return bsResult;
  }

  BitSet getMoleculeBitSet(int atomIndex) {
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++)
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].atomList;
    return null;
  }

  void invertSelected(Point3f pt, Point4f plane, BitSet bs) {
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

  //////////////  overall model set methods ////////////////
    
  public Mmset getMmset() {
    return mmset;
  }
  
  String getModelSetTypeName() {
    return modelSetTypeName;
  }

  Properties getModelSetProperties() {
    return mmset.getModelSetProperties();
  }

  String getModelSetProperty(String propertyName) {
    return mmset.getModelSetProperty(propertyName);
  }

  Hashtable getModelSetAuxiliaryInfo() {
    return mmset.getModelSetAuxiliaryInfo();
  }

  Object getModelSetAuxiliaryInfo(String keyName) {
    return mmset.getModelSetAuxiliaryInfo(keyName);
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    mmset.calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    mmset.calcSelectedMonomersCount(bsSelected);
  }

  void setShapeSize(int shapeID, int size, BitSet bsSelected) {
    if (size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setSize(size, bsSelected);
  }

  void loadShape(int shapeID) {
    if (shapes[shapeID] == null) {
      shapes[shapeID] = allocateShape(shapeID);
    }
  }

  void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bsSelected) {
    if (shapes[shapeID] != null)
      shapes[shapeID].setProperty(propertyName, value, bsSelected);
  }

  Object getShapeProperty(int shapeID, String propertyName, int index) {
    return (shapes[shapeID] == null ? null : shapes[shapeID].getProperty(
        propertyName, index));
  }

  void setModelVisibility() {
    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BitSet bs = viewer.getVisibleFramesBitSet();
    //NOT balls (yet)
    for (int i = 1; i < JmolConstants.SHAPE_MAX; i++)
      if (shapes[i] != null)
        shapes[i].setVisibilityFlags(bs);
    //s(bs);
    //
    // BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
    shapes[JmolConstants.SHAPE_BALLS].setVisibilityFlags(bs);

    //set clickability -- this enables measures and such
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null)
        shape.setModelClickability();
    }
  }

  /**
   * allows rebuilding of PDB structures;
   * also accessed by ModelManager from Eval
   * 
   * @param alreadyDefined    set to skip calculation
   *  
   */
  void calculateStructuresAllExcept(BitSet alreadyDefined) {
    mmset.calculateStructuresAllExcept(alreadyDefined);
    mmset.freeze();
  }

  BitSet setConformation(int modelIndex, int conformationIndex) {
    BitSet bs = new BitSet();
    String altLocs = getAltLocListInModel(modelIndex);
    if (altLocs.length() > 0) {
      BitSet bsConformation = getModelAtomBitSet(modelIndex);
      if (conformationIndex >= 0)
        for (int c = getAltLocCountInModel(modelIndex); --c >= 0;)
          if (c != conformationIndex)
            BitSetUtil.andNot(bsConformation,
                getSpecAlternate(altLocs.substring(c, c + 1)));
      if (BitSetUtil.length(bsConformation) > 0) {
        setConformation(modelIndex, bsConformation);
        bs.or(bsConformation);
      }
    }
    return bs;
  }

  void setConformation(int modelIndex, BitSet bsConformation) {
    mmset.setConformation(modelIndex, bsConformation);
  }

  void setTrajectory(int iTraj) {
    if (trajectories == null || iTraj < 0 || iTraj >= trajectories.size())
      return;
    Point3f[] trajectory = (Point3f[]) trajectories.get(iTraj);
      for (int i = atomCount; --i >= 0;)
        atoms[i].set(trajectory[i]);
  }

  int getTrajectoryCount() {
    return (trajectories == null ? 1 : trajectories.size());
  }
  
  /*
  private Hashtable userProperties;

  void putUserProperty(String name, Object property) {
    if (userProperties == null)
      userProperties = new Hashtable();
    if (property == null)
      userProperties.remove(name);
    else
      userProperties.put(name, property);
  }
*/  

  //////////////  individual models ////////////////
  
  Model getModel(int modelIndex) {
    return mmset.getModel(modelIndex);
  }

  int getModelNumberIndex(int modelNumber, boolean useModelNumber) {
    return mmset.getModelNumberIndex(modelNumber, useModelNumber);
  }
  
  int getModelNumber(int modelIndex) {
    return mmset.getModelNumber(modelIndex);
  }

  int getModelFileNumber(int modelIndex) {
    return mmset.getModelFileNumber(modelIndex);
  }

  String getModelName(int modelIndex) {
    return mmset.getModelName(modelIndex);
  }

  String getModelTitle(int modelIndex) {
    return mmset.getModelTitle(modelIndex);
  }

  String getModelFile(int modelIndex) {
    return mmset.getModelFile(modelIndex);
  }

  Properties getModelProperties(int modelIndex) {
    return mmset.getModelProperties(modelIndex);
  }

  String getModelProperty(int modelIndex, String propertyName) {
    return mmset.getModelProperty(modelIndex, propertyName);
  }

  Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return mmset.getModelAuxiliaryInfo(modelIndex);
  }

  Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return mmset.getModelAuxiliaryInfo(modelIndex, keyName);
  }

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

  String getAltLocListInModel(int modelIndex) {
    String str = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
    return (str == null ? "" : str);
  }

  String getInsertionListInModel(int modelIndex) {
    String str = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
    return (str == null ? "" : str);
  }

  String getModelSymmetryList(int modelIndex) {
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
    return (cellInfos == null || cellInfos[modelIndex] == null ?
        0 : cellInfos[modelIndex].symmetryOperations.length);
  }
  
  public int getAltLocCountInModel(int modelIndex) {
    return mmset.getNAltLocs(modelIndex);
  }

  public int getInsertionCountInModel(int modelIndex) {
    return mmset.getNInsertions(modelIndex);
  }

  public int[] getModelCellRange(int modelIndex) {
    if (cellInfos == null)
      return null;
    return cellInfos[modelIndex].getCellRange();
  }
  
  boolean modelHasVibrationVectors(int modelIndex) {
    if (vibrationVectors != null)
      for (int i = atomCount; --i >= 0;)
        if ((modelIndex < 0 || atoms[i].modelIndex == modelIndex)
            && vibrationVectors[i] != null && vibrationVectors[i].length() > 0)
          return true;
    return false;
  }

  BitSet getElementsPresentBitSet(int modelIndex) {
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

  void toCartesian(int modelIndex, Point3f pt) {
    if (modelIndex < 0)
      modelIndex = 0;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    String str = "Frame convertFractional " + pt + "--->";
    cellInfos[modelIndex].toCartesian(pt);
    Logger.info(str + pt);
  }

  void toUnitCell(int modelIndex, Point3f pt, Point3f offset) {
    if (modelIndex < 0)
      return;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    cellInfos[modelIndex].toUnitCell(pt, offset);
  }
  
  void toFractional(int modelIndex, Point3f pt) {
    if (modelIndex < 0)
      return;
    if (cellInfos == null || modelIndex >= cellInfos.length
        || cellInfos[modelIndex] == null)
      return;
    cellInfos[modelIndex].toFractional(pt);
  }
  
  //////////// atoms //////////////
  
  int getAtomCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return atomCount;
    int n = 0;
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].modelIndex == modelIndex)
        n++;
    return n;
  }
  
  int getFirstAtomIndexInModel(int modelIndex) {
    return mmset.getFirstAtomIndex(modelIndex);
  }
  
  int getAtomIndexFromAtomNumber(int atomNumber) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
    }
    return -1;
  }

  private boolean reportFormalCharges = false;

  void setFormalCharges(BitSet bs, int formalCharge) {
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i))
        atoms[i].setFormalCharge(formalCharge);
    reportFormalCharges = true;
  }
  
  void setProteinType(BitSet bs, byte iType) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        if (iLast != i - 1)
          monomerIndexCurrent = -1;
        iLast = i;
        monomerIndexCurrent = atoms[i].setProteinStructureType(iType,
            monomerIndexCurrent);
      }
  }
  
  float calcRotationRadius(Point3f center) {
    float maxRadius = 0;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom);
      float radiusVdw = atom.getVanderwaalsRadiusFloat();
      float outerVdw = distAtom + radiusVdw;
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  float calcRotationRadius(BitSet bs) {
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

  Point3f getAtomSetCenter(BitSet bs) {
    Point3f ptCenter = new Point3f(0, 0, 0);
    int nPoints = BitSetUtil.cardinalityOf(bs);
    if (nPoints == 0)
      return ptCenter;
    for (int i = atomCount; --i >= 0;) {
      if (bs.get(i))
        ptCenter.add(atoms[i]);
    }
    ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  private float maxBondingRadius = Float.MIN_VALUE;
  private float maxVanderwaalsRadius = Float.MIN_VALUE;
  private final static boolean showRebondTimes = true;

  public float getMaxVanderwaalsRadius() {
    //Dots
    if (maxVanderwaalsRadius == Float.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  private void findMaxRadii() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat();
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  Point3f getAveragePosition(int atomIndex1, int atomIndex2) {
    Atom atom1 = atoms[atomIndex1];
    Atom atom2 = atoms[atomIndex2];
    return new Point3f((atom1.x + atom2.x) / 2, (atom1.y + atom2.y) / 2,
        (atom1.z + atom2.z) / 2);
  }

  Vector3f getAtomVector(int atomIndex1, int atomIndex2) {
    Vector3f V = new Vector3f(atoms[atomIndex1]);
    V.sub(atoms[atomIndex2]);
    return V;
  }

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  void clearBfactorRange() {
    hasBfactorRange = false;
  }

  private void calcBfactorRange(BitSet bs) {
    if (!hasBfactorRange) {
      bfactor100Lo = Integer.MAX_VALUE;
      bfactor100Hi = Integer.MIN_VALUE;
      for (int i = atomCount; --i > 0;)
        if (bs == null || bs.get(i)) {
          int bf = atoms[i].getBfactor100();
          if (bf < bfactor100Lo)
            bfactor100Lo = bf;
          else if (bf > bfactor100Hi)
            bfactor100Hi = bf;
        }
      hasBfactorRange = true;
    }
  }

  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (viewer.isRangeSelected()) {
        calcBfactorRange(viewer.getSelectionSet());
      } else {
        calcBfactorRange(null);
      }
    }
    return bfactor100Lo;
  }

  public int getBfactor100Hi() {
    //ColorManager
    getBfactor100Lo();
    return bfactor100Hi;
  }

  private int surfaceDistanceMax;

  public int getSurfaceDistanceMax() {
    //ColorManager, Eval
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistanceMax;
  }

  private BitSet bsSurface;
  private int nSurfaceAtoms;

  int getSurfaceDistance100(int atomIndex) {
    //atom
    if (nSurfaceAtoms == 0)
      return -1;
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistance100s[atomIndex];
  }

  private void calcSurfaceDistances() {
    calculateSurface(null, null, -1);
  }
  
  Point3f[] calculateSurface(BitSet bsSelected, BitSet bsIgnore,
                             float envelopeRadius) {
    if (envelopeRadius < 0)
      envelopeRadius = EnvelopeCalculation.SURFACE_DISTANCE_FOR_CALCULATION;
    EnvelopeCalculation ec = new EnvelopeCalculation(viewer, atomCount, null);
    ec.calculate(Float.MAX_VALUE, envelopeRadius, 1, Float.MAX_VALUE, 
        bsSelected, bsIgnore, false, false, false, false, true);
    Point3f[] points = ec.getPoints();
    surfaceDistanceMax = 0;
    bsSurface = ec.getBsSurfaceClone();
    surfaceDistance100s = new int[atomCount];
    nSurfaceAtoms = BitSetUtil.cardinalityOf(bsSurface);
    if (nSurfaceAtoms == 0 || points == null || points.length == 0)
      return points;
    for (int i = 0; i < atomCount; i++) {
      //surfaceDistance100s[i] = Integer.MIN_VALUE;
      if (bsSurface.get(i)) {
        surfaceDistance100s[i] = 0;
      } else {
        float dMin = Float.MAX_VALUE;
        Atom atom = atoms[i];
        for (int j = points.length; --j >= 0;) {
          float d = points[j].distance(atom) - envelopeRadius;
          if (d < 0 && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
            Logger.debug("draw d" + j + " " + Escape.escape(points[j])
                + " \"" + d + " ? " + atom.getIdentity() + "\"");
          dMin = Math.min(d, dMin);
        }
        int d = surfaceDistance100s[i] = (int) (dMin * 100);
        surfaceDistanceMax = Math.max(surfaceDistanceMax, d);
      }
    }
    return points;
  }

  public float getMeasurement(int[] countPlusIndices) {
    float value = Float.NaN;
    if (countPlusIndices == null)
      return value;
    int count = countPlusIndices[0];
    if (count < 2)
      return value;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] < 0) {
        return value;
      }
    switch (count) {
    case 2:
      value = getDistance(countPlusIndices[1], countPlusIndices[2]);
      break;
    case 3:
      value = getAngle(countPlusIndices[1], countPlusIndices[2],
          countPlusIndices[3]);
      break;
    case 4:
      value = getTorsion(countPlusIndices[1], countPlusIndices[2],
          countPlusIndices[3], countPlusIndices[4]);
      break;
    default:
      Logger.error("Invalid count in measurement calculation:" + count);
      throw new IndexOutOfBoundsException();
    }

    return value;
  }

  public float getDistance(int atomIndexA, int atomIndexB) {
    return atoms[atomIndexA].distance(atoms[atomIndexB]);
  }

  public float getAngle(int atomIndexA, int atomIndexB, int atomIndexC) {
    Point3f pointA = atoms[atomIndexA];
    Point3f pointB = atoms[atomIndexB];
    Point3f pointC = atoms[atomIndexC];
    return Measure.computeAngle(pointA, pointB, pointC, true);
  }

  public float getTorsion(int atomIndexA, int atomIndexB, int atomIndexC,
                   int atomIndexD) {
    return Measure.computeTorsion(atoms[atomIndexA], atoms[atomIndexB],
        atoms[atomIndexC], atoms[atomIndexD], true);
  }

  ///////////// bonds ////////////////////////
    
  /**
   * for general use
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  int getBondCountInModel(int modelIndex) {
    int n = 0;
    for (int i = bondCount; --i >= 0;)
      if (modelIndex < 0 || bonds[i].atom1.modelIndex == modelIndex)
        n++;
    return n;
  }

  BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
    BitSet bs = new BitSet();
    boolean bondSelectionModeOr = viewer.getBondSelectionModeOr();
    for (int iBond = 0; iBond < bondCount; ++iBond) {
      Bond bond = bonds[iBond];
      boolean isSelected1 = bsAtoms.get(bond.atom1.atomIndex);
      boolean isSelected2 = bsAtoms.get(bond.atom2.atomIndex);
      if ((!bondSelectionModeOr & isSelected1 & isSelected2)
          || (bondSelectionModeOr & (isSelected1 | isSelected2)))
        bs.set(iBond);
    }
    return bs;
  }

  public Bond bondAtoms(Atom atom1, Atom atom2, short order, short mad, BitSet bsBonds) {
    return getOrAddBond(atom1, atom2, order, mad, bsBonds);
  }

  private final static int bondGrowthIncrement = 250;

  private Bond getOrAddBond(Atom atom, Atom atomOther, short order, short mad,
                            BitSet bsBonds) {
    int i;
    if (atom.isBonded(atomOther)) {
      i = atom.getBond(atomOther).index;
    } else {
      if (bondCount == bonds.length)
        bonds = (Bond[]) ArrayUtil
            .setLength(bonds, bondCount + bondGrowthIncrement);
      if (order < 0)
        order = 1;
      i = setBond(bondCount++, bondMutually(atom, atomOther, order, mad)).index;
    }
    if (bsBonds != null)
      bsBonds.set(i);
    return bonds[i];
  }

  Bond setBond(int index, Bond bond) {
    bond.index = index;
    return bonds[index] = bond;
  }

  protected Bond bondMutually(Atom atom, Atom atomOther, short order, short mad) {
    Bond bond = new Bond(atom, atomOther, order, mad, (short) 0);
    addBondToAtom(atom, bond);
    addBondToAtom(atomOther, bond);
    return bond;
  }

  private void addBondToAtom(Atom atom, Bond bond) {
    if (atom.bonds == null) {
      atom.bonds = new Bond[1];
      atom.bonds[0] = bond;
    } else {
      atom.bonds = addToBonds(bond, atom.bonds);
    }
  }

  final static int MAX_BONDS_LENGTH_TO_CACHE = 5;
  final static int MAX_NUM_TO_CACHE = 200;
  int[] numCached = new int[MAX_BONDS_LENGTH_TO_CACHE];
  Bond[][][] freeBonds = new Bond[MAX_BONDS_LENGTH_TO_CACHE][][];
  {
    for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;)
      // .GT. 0
      freeBonds[i] = new Bond[MAX_NUM_TO_CACHE][];
  }

  private Bond[] addToBonds(Bond newBond, Bond[] oldBonds) {
    Bond[] newBonds;
    if (oldBonds == null) {
      if (numCached[1] > 0)
        newBonds = freeBonds[1][--numCached[1]];
      else
        newBonds = new Bond[1];
      newBonds[0] = newBond;
    } else {
      int oldLength = oldBonds.length;
      int newLength = oldLength + 1;
      if (newLength < MAX_BONDS_LENGTH_TO_CACHE && numCached[newLength] > 0)
        newBonds = freeBonds[newLength][--numCached[newLength]];
      else
        newBonds = new Bond[newLength];
      newBonds[oldLength] = newBond;
      for (int i = oldLength; --i >= 0;)
        newBonds[i] = oldBonds[i];
      if (oldLength < MAX_BONDS_LENGTH_TO_CACHE
          && numCached[oldLength] < MAX_NUM_TO_CACHE)
        freeBonds[oldLength][numCached[oldLength]++] = oldBonds;
    }
    return newBonds;
  }

  Vector3f getModelDipole(int modelIndex) {
    Vector3f dipole = (Vector3f) mmset.getModelAuxiliaryInfo(modelIndex, "dipole");
    if (dipole == null)
      dipole = (Vector3f) mmset.getModelAuxiliaryInfo(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

  Vector3f calculateMolecularDipole(int modelIndex) {
    if (partialCharges == null)
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
    System.out.print("draw ptn {" + neg.x + " " + neg.y + " " + neg.z + "};draw ptp {" + pos.x + " " + pos.y + " " + pos.z + " }#" + cNeg + " " + cPos+" ");
    pos.sub(neg);
    System.out.println(pos.length() + " " + cPos);
    System.out.println (" this is an untested result -- needs checking");
    pos.scale(cPos * 4.8f); //1e-10f * 1.6e-19f/ 3.336e-30f;
    
    // SUM_Q[(SUM_pos Q_iRi) / SUM_Q   -  (SUM_neg Q_iRi) / (-SUM_Q) ]    
    // this is really just SUM_i (Q_iR_i). Don't know why that would not work. 
    
    
    //http://www.chemistry.mcmaster.ca/esam/Chapter_7/section_3.html
    // 1 Debye = 3.336e-30 Coulomb-meter; C_e = 1.6022e-19 C
    return pos;
  }
  
  void getBondDipoles() {
    if (partialCharges == null)
      return;
    loadShape(JmolConstants.SHAPE_DIPOLES);
    Dipoles dipoles = (Dipoles) shapes[JmolConstants.SHAPE_DIPOLES];
    dipoles.clear(true);
    for (int i = bondCount; --i >= 0;) {
      if (!bonds[i].isCovalent())
        continue;
      Atom atom1 = bonds[i].atom1;
      Atom atom2 = bonds[i].atom2;
      float c1 = partialCharges[atom1.atomIndex];
      float c2 = partialCharges[atom2.atomIndex];
      if (c1 != c2) 
        dipoles.setDipole(atom1, atom2, c1, c2);
    }
  } 

  private BitSet bsPseudoHBonds;

  /**
   * These are not actual hydrogen bonds. They are N-O bonds in proteins and nucleic acids
   * The method is called by AminoPolymer and NucleicPolymer methods,
   * which are indirectly called by this.autoHbond
   *  
   * @param atom1
   * @param atom2
   * @param order
   * @param bsA
   * @param bsB
   */
  void addHydrogenBond(Atom atom1, Atom atom2, short order, BitSet bsA,
                       BitSet bsB) {
    if (atom1 == null || atom2 == null)
      return;
    boolean atom1InSetA = bsA == null || bsA.get(atom1.atomIndex);
    boolean atom1InSetB = bsB == null || bsB.get(atom1.atomIndex);
    boolean atom2InSetA = bsA == null || bsA.get(atom2.atomIndex);
    boolean atom2InSetB = bsB == null || bsB.get(atom2.atomIndex);
    if (atom1InSetA && atom2InSetB || atom1InSetB && atom2InSetA)
      getOrAddBond(atom1, atom2, order, (short) 1, bsPseudoHBonds);
  }
 
  void rebond() {
    // from eval "connect" or from app preferences panel
    stateScripts.addElement("connect;");
    deleteAllBonds();
    autoBond(null, null, null);
  }

  protected int autoBond(BitSet bsA, BitSet bsB, BitSet bsBonds) {
    if (atomCount == 0)
      return 0;
    // null values for bitsets means "all"
    if (maxBondingRadius == Float.MIN_VALUE)
      findMaxRadii();
    float bondTolerance = viewer.getBondTolerance();
    float minBondDistance = viewer.getMinBondDistance();
    float minBondDistance2 = minBondDistance * minBondDistance;
    short mad = viewer.getMadBond();
    //char chainLast = '?';
    //int indexLastCA = -1;
    //Atom atomLastCA = null;
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
    for (int i = atomCount; --i >= 0;) {
      boolean isAtomInSetA = (bsA == null || bsA.get(i));
      boolean isAtomInSetB = (bsB == null || bsB.get(i));
      if (!isAtomInSetA && !isAtomInSetB)
        continue;
      Atom atom = atoms[i];
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom)
          continue;
        int atomIndexNear = atomNear.atomIndex;
        boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
        boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
        if (!isNearInSetA && !isNearInSetB)
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
    if (showRebondTimes && Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      long timeEnd = System.currentTimeMillis();
      Logger.debug("Time to autoBond=" + (timeEnd - timeBegin));
    }

    return nNew;
  }

  private short getBondOrder(Atom atomA, float bondingRadiusA, Atom atomB,
                             float bondingRadiusB, float distance2,
                             float minBondDistance2, float bondTolerance) {
    if (bondingRadiusA == 0 || bondingRadiusB == 0)
      return 0;
    float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
    float maxAcceptable2 = maxAcceptable * maxAcceptable;
    if (distance2 < minBondDistance2) {
      return 0;
    }
    if (distance2 <= maxAcceptable2) {
      return 1;
    }
    return 0;
  }

  private boolean haveWarned = false;

  void checkValencesAndBond(Atom atomA, Atom atomB, short order, short mad,
                            BitSet bsBonds) {
    if (atomA.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT
        || atomB.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT) {
      if (!haveWarned)
        Logger.warn("maximum auto bond count reached");
      haveWarned = true;
      return;
    }
    int formalChargeA = atomA.getFormalCharge();
    if (formalChargeA != 0) {
      int formalChargeB = atomB.getFormalCharge();
      if ((formalChargeA < 0 && formalChargeB < 0)
          || (formalChargeA > 0 && formalChargeB > 0))
        return;
    }
    if (atomA.alternateLocationID != atomB.alternateLocationID
        && atomA.alternateLocationID != 0 && atomB.alternateLocationID != 0)
      return;
    getOrAddBond(atomA, atomB, order, mad, bsBonds);
  }

  public void deleteAllBonds() {
    //StateManager
    stateScripts.clear();
    viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reset", null);
    for (int i = bondCount; --i >= 0;) {
      bonds[i].deleteAtomReferences();
      bonds[i] = null;
    }
    bondCount = 0;
  }

  protected short defaultCovalentMad;

  int makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB,
                      BitSet bsBonds, boolean isBonds) {
    if (connectOperation != JmolConstants.CONNECT_IDENTIFY_ONLY) {
      String stateScript = "connect " + minDistance + " " + maxDistance + " ";
      if (isBonds)
        stateScript += Escape.escape(bsA, false) + " ";
      else
        stateScript += Escape.escape(bsA) + " "
            + Escape.escape(bsB) + " ";
      stateScript += JmolConstants.getBondOrderNameFromOrder(order) + " "
          + JmolConstants.connectOperationName(connectOperation);
      stateScript += ";";
      stateScripts.addElement(stateScript);
    }
    if (connectOperation == JmolConstants.CONNECT_DELETE_BONDS)
      return deleteConnections(minDistance, maxDistance, order, bsA, bsB,
          isBonds);
    if (connectOperation == JmolConstants.CONNECT_AUTO_BOND)
      return autoBond(order, bsA, bsB, bsBonds, isBonds);
    if (order == JmolConstants.BOND_ORDER_NULL)
      order = JmolConstants.BOND_COVALENT_SINGLE; // default 
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    defaultCovalentMad = viewer.getMadBond();
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    boolean identifyOnly = (connectOperation == JmolConstants.CONNECT_IDENTIFY_ONLY);
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
        if (bondAB == null
            && (identifyOnly || JmolConstants.CONNECT_MODIFY_ONLY == connectOperation)
            || bondAB != null
            && JmolConstants.CONNECT_CREATE_ONLY == connectOperation)
          continue;
        float distanceSquared = atomA.distanceSquared(atomB);
        if (distanceSquared < minDistanceSquared
            || distanceSquared > maxDistanceSquared)
          continue;
        if (bondAB != null) {
          if (order >= 0 && !identifyOnly)
            bondAB.setOrder(order);
          if (!identifyOnly || order == bondAB.order
              || order == JmolConstants.BOND_ORDER_ANY
              || order == JmolConstants.BOND_H_REGULAR && bondAB.isHydrogen()) {
            bsBonds.set(bondAB.index);
            nModified++;
          }
        } else {
          bondAtoms(atomA, atomB, order, mad, bsBonds);
          nNew++;
        }
      }
    }
    Logger.info(nNew + " new bonds; " + nModified + " modified");
    return nNew + nModified;
  }

  /**
   * When creating a new bond, determine bond diameter from order 
   * @param order
   * @return if hydrogen bond, default to 1; otherwise 0 (general default) 
   */
  protected short getDefaultMadFromOrder(short order) {
    return (short) ((order & JmolConstants.BOND_HYDROGEN_MASK) > 0 ? 1
        : defaultCovalentMad);
  }

  private int autoBond(short order, BitSet bsA, BitSet bsB, BitSet bsBonds,
                       boolean isBonds) {
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
    if (order == JmolConstants.BOND_ORDER_NULL)
      return autoBond(bsA, bsB, bsBonds);
    else if (order == JmolConstants.BOND_H_REGULAR)
      return autoHbond(bsA, bsB, bsBonds);
    else
      Logger.warn("autoBond() unknown order: " + order);
    return 0;
  }

  private int deleteConnections(float minDistance, float maxDistance, short order,
                        BitSet bsA, BitSet bsB, boolean isBonds) {
    BitSet bsDelete = new BitSet();
    float minDistanceSquared = minDistance * minDistance;
    float maxDistanceSquared = maxDistance * maxDistance;
    if (order != JmolConstants.BOND_ORDER_NULL
        && (order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      order = JmolConstants.BOND_HYDROGEN_MASK;
    int nDeleted = 0;
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      Atom atom1 = bond.atom1;
      Atom atom2 = bond.atom2;
      if (!isBonds
          && (bsA.get(atom1.atomIndex) && bsB.get(atom2.atomIndex) || bsA
              .get(atom2.atomIndex)
              && bsB.get(atom1.atomIndex)) || isBonds && bsA.get(i)) {
        if (bond.atom1.isBonded(bond.atom2)) {
          float distanceSquared = atom1.distanceSquared(atom2);
          if (distanceSquared >= minDistanceSquared
              && distanceSquared <= maxDistanceSquared)
            if (order == JmolConstants.BOND_ORDER_NULL
                || order == (bond.order & ~JmolConstants.BOND_SULFUR_MASK)
                || (order & bond.order & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
              bsDelete.set(i);
              nDeleted++;
            }
        }
      }
    }
    deleteBonds(bsDelete);
    Logger.info(nDeleted + " bonds deleted");
    return nDeleted;
  }

  private void deleteBonds(BitSet bs) {
    int iDst = 0;
    for (int iSrc = 0; iSrc < bondCount; ++iSrc) {
      Bond bond = bonds[iSrc];
      if (!bs.get(iSrc))
        setBond(iDst++, bond);
      else
        bond.deleteAtomReferences();
    }
    for (int i = bondCount; --i >= iDst;)
      bonds[i] = null;
    bondCount = iDst;
  }
/*
  void deleteCovalentBonds() {
    int indexNoncovalent = 0;
    for (int i = 0; i < bondCount; ++i) {
      Bond bond = bonds[i];
      if (bond == null)
        continue;
      if (!bond.isCovalent()) {
        if (i != indexNoncovalent) {
          setBond(indexNoncovalent++, bond);
          bonds[i] = null;
        }
      } else {
        bond.deleteAtomReferences();
        bonds[i] = null;
      }
    }
    bondCount = indexNoncovalent;
  }
*/

  private float hbondMax = 3.25f;
  private float hbondMin = 2.5f;
  private float hbondMin2 = hbondMin * hbondMin;

  private final static boolean useRasMolHbondsCalculation = true;

  int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds) {
    bsPseudoHBonds = new BitSet();
    if (useRasMolHbondsCalculation && bondCount > 0) {
      if (mmset != null)
        mmset.calcHydrogenBonds(bsA, bsB);
      bsBonds = bsPseudoHBonds;
      return BitSetUtil.cardinalityOf(bsBonds);
    }
    // this method is not enabled and is probably error-prone.
    // it does not take into account anything but distance, 
    // and as such is not really practical. 

    int nNew = 0;
    initializeBspf();
    long timeBegin = 0;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      int elementNumber = atom.getElementNumber();
      if (elementNumber != 7 && elementNumber != 8)
        continue;
      //float searchRadius = hbondMax;
      SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
      iter.initializeHemisphere(atom, hbondMax);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        int elementNumberNear = atomNear.getElementNumber();
        if (elementNumberNear != 7 && elementNumberNear != 8)
          continue;
        if (atomNear == atom)
          continue;
        if (iter.foundDistance2() < hbondMin2)
          continue;
        if (atom.isBonded(atomNear))
          continue;
        getOrAddBond(atom, atomNear, JmolConstants.BOND_H_REGULAR, (short) 1,
            bsPseudoHBonds);
        nNew++;
      }
      iter.release();
    }
    if (showRebondTimes && Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      long timeEnd = System.currentTimeMillis();
      Logger.debug("Time to hbond=" + (timeEnd - timeBegin));
    }
    return nNew;
  }

  private final static boolean MIX_BSPT_ORDER = false;

  void initializeBspf() {
    if (bspf == null) {
      long timeBegin = 0;
      if (showRebondTimes)
        timeBegin = System.currentTimeMillis();
      bspf = new Bspf(3);
      if (MIX_BSPT_ORDER) {
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
        Logger.debug("sequential bspt order");
        for (int i = atomCount; --i >= 0;) {
          Atom atom = atoms[i];
          bspf.addTuple(atom.modelIndex, atom);
        }
      }
      if (showRebondTimes) {
        long timeEnd = System.currentTimeMillis();
        Logger.debug("time to build bspf=" + (timeEnd - timeBegin) + " ms");
        bspf.stats();
        //        bspf.dump();
      }
    }
  }

  /*
  int getBsptCount() {
    if (bspf == null)
      initializeBspf();
    return bspf.getBsptCount();
  }
*/
  

  //////////////// groups ///////////////////

  int getChainCount() {
    return mmset.getChainCount();
  }

  int getGroupCount() {
    return mmset.getGroupCount();
  }

  int getBioPolymerCount() {
    return mmset.getBioPolymerCount();
  }

  int getChainCountInModel(int modelIndex) {
    return mmset.getChainCountInModel(modelIndex);
  }

  int getBioPolymerCountInModel(int modelIndex) {
    return mmset.getBioPolymerCountInModel(modelIndex);
  }

  int getGroupCountInModel(int modelIndex) {
    return mmset.getGroupCountInModel(modelIndex);
  }

  ////////// molecules /////////////
  
  private void getMolecules() {
    if (moleculeCount > 0)
      return;
    if (molecules == null)
      molecules = new Molecule[4];
    moleculeCount = 0;
    int atomCount = getAtomCount();
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
          mmset.getModel(modelIndex).firstMolecule = moleculeCount;
          moleculeCount0 = moleculeCount - 1;
          thisModelIndex = modelIndex;
        }
        indexInModel++;
        bs = getConnectedBitSet(i);
        atomlist.or(bs);
        if (moleculeCount == molecules.length)
          molecules = (Molecule[]) ArrayUtil.setLength(molecules,
              moleculeCount * 2);
        molecules[moleculeCount] = new Molecule(this, moleculeCount, bs,
            thisModelIndex, indexInModel);
        mmset.getModel(thisModelIndex).moleculeCount = moleculeCount
            - moleculeCount0;
        moleculeCount++;
      }
  }

  private BitSet getConnectedBitSet(int atomIndex) {
    int atomCount = getAtomCount();
    BitSet bs = new BitSet(atomCount);
    BitSet bsToTest = getModelAtomBitSet(atoms[atomIndex].modelIndex);
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

  
  private BitSet bsTemp = new BitSet();

  Vector getMoleculeInfo(BitSet bsAtoms) {
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
    return mmset.getModel(modelIndex).firstMolecule;
  }
*/
  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < modelCount; i++) {
      if (modelIndex == i || modelIndex < 0)
        n += mmset.getModel(i).moleculeCount;
    }
    return n;
  }

  private BitSet selectedMolecules = new BitSet();
  private int selectedMoleculeCount;

  void calcSelectedMoleculesCount(BitSet bsSelected) {
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

  ///////// atom and shape selecting /////////
  
  boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    if (frankShape == null)
      return false;
    return frankShape.wasClicked(x, y);
  }

  private final Closest closest = new Closest();

  int findNearestAtomIndex(int x, int y) {
    if (atomCount == 0)
      return -1;
    closest.atom = null;
    findNearestAtomIndex(x, y, closest);

    for (int i = 0; i < shapes.length && closest.atom == null; ++i)
      if (shapes[i] != null)
        shapes[i].findNearestAtomIndex(x, y, closest);
    int closestIndex = (closest.atom == null ? -1 : closest.atom.atomIndex);
    closest.atom = null;
    return closestIndex;
  }

  private final static int minimumPixelSelectionRadius = 6;

  /*
   * generalized; not just balls
   * 
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  private void findNearestAtomIndex(int x, int y, Closest closest) {
    Atom champion = null;
    //int championIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      Atom contender = atoms[i];
      if (contender.isClickable()
          && isCursorOnTopOf(contender, x, y, minimumPixelSelectionRadius,
              champion))
        champion = contender;
    }
    closest.atom = champion;
  }

  /**
   * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
   * @param contender
   * @param x
   * @param y
   * @param radius
   * @param champion
   * @return true if user is pointing to this atom
   */
  boolean isCursorOnTopOf(Atom contender, int x, int y, int radius,
                          Atom champion) {
    return contender.screenZ > 1 && !g3d.isClippedZ(contender.screenZ)
        && g3d.isInDisplayRange(contender.screenX, contender.screenY)
        && contender.isCursorOnTopOf(x, y, radius, champion);
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();

  BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (rect.contains(atom.screenX, atom.screenY))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  ////////////////// atomData filling ////////////

  void fillAtomData(AtomData atomData, int mode) {
    if (mode == AtomData.MODE_GET_ATTACHED_HYDROGENS) {
      int[] nH = new int[1];
      atomData.hAtomRadius = JmolConstants.vanderwaalsMars[1] / 1000f;
      atomData.hAtoms = getAdditionalHydrogens(atomData.bsSelected, nH);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if(atomData.modelIndex < 0)
      atomData.firstAtomIndex = Math.max(0, BitSetUtil.firstSetBit(atomData.bsSelected));
    else
      atomData.firstAtomIndex = getFirstAtomIndexInModel(atomData.modelIndex);
    atomData.lastModelIndex = atomData.firstModelIndex = (atomCount == 0 ? 0 : atoms[atomData.firstAtomIndex].modelIndex);
    atomData.modelName = getModelName(-1 - atomData.modelIndex);
    atomData.atomXyz = atoms;
    atomData.atomCount = atomCount;
    atomData.atomicNumber = new int[atomCount];
    boolean includeRadii = (mode == AtomData.MODE_FILL_COORDS_AND_RADII);
    if (includeRadii)
      atomData.atomRadius = new float[atomCount];
    for (int i = 0; i < atomCount; i++) {
      if (atomData.modelIndex >= 0 && atoms[i].modelIndex != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BitSet();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atoms[i].getElementNumber();
      atomData.lastModelIndex = atoms[i].modelIndex;
      if (includeRadii)
        atomData.atomRadius[i] = (atomData.useIonic ? atoms[i]
            .getBondingRadiusFloat() : atoms[i].getVanderwaalsRadiusFloat());
    }
  }

  private Point3f[][] getAdditionalHydrogens(BitSet atomSet, int[] nTotal) {
    Vector3f z = new Vector3f();
    Vector3f x = new Vector3f();
    Point3f[][] hAtoms = new Point3f[atomCount][];
    Point3f pt;
    int nH = 0;
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    for (int i = 0; i < atomCount; i++) {
      if (atomSet.get(i) && atoms[i].getElementNumber() == 6) {

        int n = 0;
        Atom atom = atoms[i];
        int nBonds = (atom.getCovalentHydrogenCount() > 0 ? 0 : atom
            .getCovalentBondCount());
        if (nBonds == 3 || nBonds == 2) { //could be XA3 sp2 or XA2 sp
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization == null || hybridization.equals("sp"))
            nBonds = 0;
        }
        if (nBonds > 0 && nBonds <= 4)
          n += 4 - nBonds;
        hAtoms[i] = new Point3f[n];
        nH += n;
        n = 0;
        switch (nBonds) {
        case 1:
          getHybridizationAndAxes(i, z, x, "sp3a", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3b", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3c", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          break;
        case 2:
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization != null && !hybridization.equals("sp")) {
            getHybridizationAndAxes(i, z, x, "lpa", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
            getHybridizationAndAxes(i, z, x, "lpb", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
          break;
        case 3:
          if (getHybridizationAndAxes(i, z, x, "sp3", true) != null) {
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
        default:
        }

      }
    }
    nTotal[0] = nH;
    return hAtoms;
  }

  ////// special method for lcaoCartoons
  
  String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                           String lcaoTypeRaw, boolean hybridizationCompatible) {
    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);
    Atom atom = atoms[atomIndex];
    String hybridization = "";
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    Atom atom1 = atom;
    Atom atom2 = atom;
    int nBonds = 0;
    float _180 = (float) Math.PI * 0.95f;
    Vector3f n = new Vector3f();
    Vector3f x2 = new Vector3f();
    Vector3f x3 = new Vector3f(3.14159f, 2.71828f, 1.41421f);
    Vector3f x4 = new Vector3f();
    Vector3f y1 = new Vector3f();
    Vector3f y2 = new Vector3f();
    if (atom.bonds != null)
      for (int i = atom.bonds.length; --i >= 0;)
        if (atom.bonds[i].isCovalent()) {
          ++nBonds;
          atom1 = atom.bonds[i].getOtherAtom(atom);
          n.sub(atom, atom1);
          n.normalize();
          z.add(n);
          switch (nBonds) {
          case 1:
            x.set(n);
            atom2 = atom1;
            break;
          case 2:
            x2.set(n);
            break;
          case 3:
            x3.set(n);
            x4.set(-z.x, -z.y, -z.z);
            break;
          case 4:
            x4.set(n);
            break;
          default:
            i = -1;
          }
        }
    switch (nBonds) {
    case 0:
      z.set(0, 0, 1);
      x.set(1, 0, 0);
      break;
    case 1:
      if (lcaoType.indexOf("sp3") == 0) { // align z as sp3 orbital
        hybridization = "sp3";
        x.cross(x3, z);
        y1.cross(z, x);
        x.normalize();
        y1.normalize();
        y2.set(x);
        z.normalize();
        x.scaleAdd(2.828f, x, z); // 2*sqrt(2)
        if (!lcaoType.equals("sp3a") && !lcaoType.equals("sp3")) {
          x.normalize();
          AxisAngle4f a = new AxisAngle4f(z.x, z.y, z.z, (lcaoType
              .equals("sp3b") ? 1 : -1) * 2.09439507f); // PI*2/3
          Matrix3f m = new Matrix3f();
          m.setIdentity();
          m.set(a);
          m.transform(x);
        }
        z.set(x);
        x.cross(y1, z);
        break;
      }
      hybridization = "sp";
      if (atom1.getCovalentBondCount() == 3) {
        //special case, for example R2C=O oxygen
        getHybridizationAndAxes(atom1.atomIndex, z, x3, lcaoType, false);
        x3.set(x);
        if (lcaoType.indexOf("sp2") == 0) { // align z as sp2 orbital
          hybridization = "sp2";
          z.scale(-1);
        }
      }
      x.cross(x3, z);
      break;
    case 2:
      if (z.length() < 0.1) {
        // linear A--X--B
        hybridization = "sp";
        if (!lcaoType.equals("pz")) {
          if (atom1.getCovalentBondCount() != 3)
            atom1 = atom2;
          if (atom1.getCovalentBondCount() == 3) {
            //special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(atom1.atomIndex, x, z, "pz", false);
            if (lcaoType.equals("px"))
              x.scale(-1);
            z.set(x2);
            break;
          }
        }
        z.set(x);
        x.cross(x3, z);
        break;
      }
      // bent A--X--B
      hybridization = (lcaoType.indexOf("sp3") == 0 ? "sp3" : "sp2");
      x3.cross(z, x);
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        if (lcaoType.equals("sp2a") || lcaoType.equals("sp2b")) {
          z.set(lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
        }
        x.cross(z, x3);
        break;
      }
      if (lcaoType.indexOf("lp") == 0) { // align z as lone pair
        hybridization = "lp"; //any is OK
        x3.normalize();
        z.normalize();
        y1.scaleAdd(1.2f, x3, z);
        y2.scaleAdd(-1.2f, x3, z);
        if (!lcaoType.equals("lp"))
          z.set(lcaoType.indexOf("b") >= 0 ? y2 : y1);
        x.cross(z, x3);
        break;
      }
      hybridization = lcaoType;
      // align z as p orbital
      x.cross(z, x3);
      z.set(x3);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
      break;
    default:
      //3 or 4 bonds
      if (x.angle(x2) < _180)
        y1.cross(x, x2);
      else
        y1.cross(x, x3);
      y1.normalize();
      if (x2.angle(x3) < _180)
        y2.cross(x2, x3);
      else
        y2.cross(x, x3);
      y2.normalize();
      if (Math.abs(y2.dot(y1)) < 0.95f) {
        hybridization = "sp3";
        if (lcaoType.indexOf("sp") == 0) { // align z as sp3 orbital
          z.set(lcaoType.equalsIgnoreCase("sp3")
                  || lcaoType.indexOf("d") >= 0 ? x4
                  : lcaoType.indexOf("c") >= 0 ? x3
                      : lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
          x.set(y1);
        } else { //needs testing here
          if (lcaoType.indexOf("lp") == 0 && nBonds == 3) { // align z as lone pair            
            hybridization = "lp"; //any is OK
          }
          x.cross(z, x);
        }
        break;
      }
      hybridization = "sp2";
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        z.set(lcaoType.equalsIgnoreCase("sp3") || lcaoType.indexOf("d") >= 0 ? x4
                : lcaoType.indexOf("c") >= 0 ? x3
                    : lcaoType.indexOf("b") >= 0 ? x2 : x);
        z.scale(-1);
        x.set(y1);
        break;
      }
      // align z as p orbital
      z.set(y1);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
    }

    x.normalize();
    z.normalize();

    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug(atom.getIdentity() + " nBonds=" + nBonds + " " + hybridization);
    }
    if (hybridizationCompatible) {
      if (hybridization == "")
        return null;
      if (lcaoType.indexOf("p") == 0) {
        if (hybridization == "sp3")
          return null;
      } else {
        if (lcaoType.indexOf(hybridization) < 0)
          return null;
      }
    }
    return hybridization;
  }
  
  /* ******************************************************
   * 
   * These next methods are used by Eval to select for 
   * specific atom sets. They all eturn a BitSet
   * 
   ********************************************************/
 
  BitSet getModelBitSet(BitSet atomList) {
    BitSet bs = new BitSet();
    for (int i = 0; i < atomCount; i++)
      if (atomList.get(i))
        bs.set(atoms[i].modelIndex);
    return bs;
  }

  /**
   * general unqualified lookup of atom set type
   * @param setType
   * @return BitSet; or null if we mess up the type
   */
  BitSet getAtomBits(String setType) {
    if (setType.equals("specialposition"))
      return getSpecialPosition();
    if (setType.equals("symmetry"))
      return getSymmetrySet();
    if (setType.equals("unitcell"))
      return getUnitCellSet();
    if (setType.equals("hetero"))
      return getHeteroSet();
    if (setType.equals("hydrogen"))
      return getHydrogenSet();
    if (setType.equals("protein"))
      return getProteinSet();
    if (setType.equals("carbohydrate"))
      return getCarbohydrateSet();
    if (setType.equals("nucleic"))
      return getNucleicSet();
    if (setType.equals("dna"))
      return getDnaSet();
    if (setType.equals("rna"))
      return getRnaSet();
    if (setType.equals("purine"))
      return getPurineSet();
    if (setType.equals("pyrimidine"))
      return getPyrimidineSet();
    return null;
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

  private BitSet getHeteroSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isHetero())
        bs.set(i);
    return bs;
  }

  private BitSet getHydrogenSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getElementNumber() == 1)
        bs.set(i);
    }
    return bs;
  }

  private BitSet getProteinSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isProtein())
        bs.set(i);
    return bs;
  }

  private BitSet getCarbohydrateSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isCarbohydrate())
        bs.set(i);
    return bs;
  }

  private BitSet getNucleicSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isNucleic())
        bs.set(i);
    return bs;
  }

  private BitSet getDnaSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isDna())
        bs.set(i);
    return bs;
  }

  private BitSet getRnaSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isRna())
        bs.set(i);
    return bs;
  }

  private BitSet getPurineSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isPurine())
        bs.set(i);
    return bs;
  }

  private BitSet getPyrimidineSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isPyrimidine())
        bs.set(i);
    return bs;
  }

  /**
   * general lookup for String type
   * @param setType
   * @param specInfo
   * @return BitSet or null in certain cases
   */
  BitSet getAtomBits(String setType, String specInfo) {
    if (setType.equals("IdentifierOrNull"))
      return getIdentifierOrNull(specInfo);
    if (setType.equals("SpecAtom"))
      return getSpecAtom(specInfo);
    if (setType.equals("SpecName"))
      return getSpecName(specInfo);
    if (setType.equals("SpecAlternate"))
      return getSpecAlternate(specInfo);
    return null;
  }

  /**
   * overhauled by RMH Nov 1, 2006.
   * 
   * @param identifier
   * @return null or bs
   */
  private BitSet getIdentifierOrNull(String identifier) {
    //a primitive lookup scheme when [ ] are not used
    //nam
    //na?
    //nam45
    //nam45C
    //nam45^
    //nam45^A
    //nam45^AC -- note, no colon here -- if present, handled separately
    //nam4? does NOT match anything for PDB files, but might for others
    //atom specifiers:
    //H?
    //H32
    //H3?

    //in the case of a ?, we take the whole thing

    BitSet bs = getSpecNameOrNull(identifier);
    if (bs != null || identifier.indexOf("?") > 0)
      return bs;

    int pt = identifier.indexOf("*");
    if (pt > 0)
      return getSpecNameOrNull(identifier.substring(0, pt) + "??????????"
          + identifier.substring(pt + 1));
    int len = identifier.length();
    pt = 0;
    while (pt < len && Character.isLetter(identifier.charAt(pt)))
      ++pt;
    bs = getSpecNameOrNull(identifier.substring(0, pt));
    if (pt == len)
      return bs;
    if (bs == null)
      bs = new BitSet();
    //
    // look for a sequence number or sequence number ^ insertion code
    //
    int pt0 = pt;
    while (pt < len && Character.isDigit(identifier.charAt(pt)))
      ++pt;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
    } catch (NumberFormatException nfe) {
      return null;
    }
    char insertionCode = ' ';
    if (pt < len && identifier.charAt(pt) == '^')
      if (++pt < len)
        insertionCode = identifier.charAt(pt);
    int seqcode = Group.getSeqcode(seqNumber, insertionCode);
    BitSet bsInsert = getSpecSeqcode(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = getSpecSeqcode(Character.toUpperCase(identifier.charAt(pt)),
            false);
      if (bsInsert == null)
        return null;
      pt++;
    }
    bs.and(bsInsert);
    if (pt >= len)
      return bs;
    //
    // look for a chain spec -- no colon
    //
    char chainID = identifier.charAt(pt++);
    bs.and(getSpecChain(chainID));
    if (pt == len)
      return bs;
    //
    // not applicable
    //
    return null;
  }

  private BitSet getSpecAtom(String atomSpec) {
    BitSet bs = new BitSet();
    atomSpec = atomSpec.toUpperCase();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].isAtomNameMatch(atomSpec)) {
        bs.set(i);
      }
    }
    return bs;
  }

  private BitSet getSpecName(String name) {
    BitSet bs = getSpecNameOrNull(name);
    if (bs != null)
      return bs;
    int pt = name.indexOf("*");
    if (pt > 0) {
      bs = getSpecNameOrNull(name.substring(0, pt) + "??????????"
          + name.substring(pt + 1));
    }
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet getSpecNameOrNull(String name) {
    BitSet bs = null;
    name = name.toUpperCase();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isGroup3OrNameMatch(name)) {
        if (bs == null)
          bs = new BitSet(i + 1);
        bs.set(i);
      }
    return bs;
  }

  private BitSet getSpecAlternate(String alternateSpec) {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].isAlternateLocationMatch(alternateSpec))
        bs.set(i);
    }
    return bs;
  }

  /**
   * general lookup for integer type -- from Eval
   * @param setType   
   * @param specInfo  
   * @return bitset; null only if we mess up with name
   */
  BitSet getAtomBits(String setType, int specInfo) {
    if (setType.equals("SpecResid"))
      return getSpecResid(specInfo);
    if (setType.equals("SpecSeqcode"))
      return getSpecSeqcode(specInfo, true);
    if (setType.equals("SpecChain"))
      return getSpecChain((char) specInfo);
    if (setType.equals("atomno"))
      return getSpecAtomNumber(specInfo);
    if (setType.equals("SpecModel"))
      return getSpecModel(specInfo);
    return null;
  }

  private BitSet getSpecResid(int resid) {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getGroupID() == resid)
        bs.set(i);
    }
    return bs;
  }

  private BitSet getSpecSeqcode(int seqcode, boolean returnEmpty) {
    BitSet bs = new BitSet();
    int seqNum = (seqcode >> 8);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCode(seqcode);
    switch (insCode) {
    case '?':
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if ((seqNum == 0 || seqNum == (atomSeqcode >> 8))
            && (atomSeqcode & 0xFF) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (seqcode == atomSeqcode || seqNum == 0
            && seqcode == (atomSeqcode & 0xFF) || insCode == '*'
            && seqNum == (atomSeqcode >> 8)) {
          bs.set(i);
          isEmpty = false;
        }
      }
    }
    return (!isEmpty || returnEmpty ? bs : null);
  }

  private BitSet getSpecChain(char chain) {
    boolean caseSensitive = viewer.getChainCaseSensitive();
    if (!caseSensitive)
      chain = Character.toUpperCase(chain);
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      char ch = atoms[i].getChainID();
      if (!caseSensitive)
        ch = Character.toUpperCase(ch);
      if (chain == ch)
        bs.set(i);
    }
    return bs;
  }

  private BitSet getSpecAtomNumber(int atomno) {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getAtomNumber() == atomno)
        bs.set(i);
    }
    return bs;
  }

  private BitSet getSpecModel(int modelNumber) {
    return getModelAtomBitSet(getModelNumberIndex(modelNumber, true));
  }

  /**
   * general lookup involving a range
   * @param setType
   * @param specInfo
   * @return BitSet; or null if mess up with type
   */
  BitSet getAtomBits(String setType, int[] specInfo) {
    if (setType.equals("SpecSeqcodeRange"))
      return getSpecSeqcodeRange(specInfo[0], specInfo[1]);
    if (setType.equals("Cell"))
      return getCellSet(specInfo[0], specInfo[1], specInfo[2]);
    return null;
  }

  private BitSet getSpecSeqcodeRange(int seqcodeA, int seqcodeB) {
    BitSet bs = new BitSet();
    mmset.selectSeqcodeRange(seqcodeA, seqcodeB, bs);
    return bs;
  }

  private BitSet getCellSet(int ix, int jy, int kz) {
    BitSet bs = new BitSet();
    Point3f cell = new Point3f(ix / 1000f, jy / 1000f, kz / 1000f);
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isInLatticeCell(cell))
        bs.set(i);
    return bs;
  }

  BitSet getModelAtomBitSet(int modelIndex) {
    BitSet bs = new BitSet();
    for (int i = 0; i < atomCount; i++)
      if (modelIndex < 0 || atoms[i].modelIndex == modelIndex)
        bs.set(i);
    return bs;
  }

  BitSet getAtomsWithin(float distance, Point4f plane) {
    BitSet bsResult = new BitSet();
    for (int i = getAtomCount(); --i >= 0;) {
      Atom atom = getAtomAt(i);
      float d = Graphics3D.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }

  BitSet getVisibleSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible())
        bs.set(i);
    return bs;
  }

  BitSet getClickableSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isClickable())
        bs.set(i);
    return bs;
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/
 
  private Vector stateScripts = new Vector();
  private int thisStateModel = 0;

  void addStateScript(String script) {
    int iModel = viewer.getCurrentModelIndex();
    if (thisStateModel != iModel) {
      thisStateModel = iModel;
      script = "frame " + (iModel < 0 ? "" + iModel : getModelName(-1 - iModel))
          + ";\n" + script;
    }
    stateScripts.addElement(script);
  }

  String getState(boolean isAll) {
    StringBuffer commands = new StringBuffer();
    String cmd;
    if (isAll && reportFormalCharges) {
      commands.append("\n# charges;\n");
      Hashtable ht = new Hashtable();
      for (int i = 0; i < atomCount; i++)
        StateManager.setStateInfo(ht, i, i, "formalCharge = "
            + atoms[i].getFormalCharge());
      commands.append(StateManager.getCommands(ht));
    }

    // positions

    if (isAll && tainted != null) {
      commands.append("\n# positions;\n");
      StringBuffer s = new StringBuffer();
      int n = 0;
      for (int i = 0; i < atomCount; i++)
        if (tainted.get(i)) {
          s.append(i + 1).append(" ").append(atoms[i].getElementSymbol())
              .append(" ").append(
                  TextFormat.simpleReplace(atoms[i].getIdentity(), " ", "_"))
              .append(" ").append(atoms[i].x).append(" ").append(atoms[i].y)
              .append(" ").append(atoms[i].z).append(" ;\n");
          ++n;
        }
      commands.append("DATA \"coord set\"\n").append(n).append(
          " ;\nJmol Coordinate Data Format 1 -- Jmol ").append(
          Viewer.getJmolVersion()).append(";\n");
      commands.append(s);
      commands.append("end \"coord set\";\n");
    }

    // connections

    if (isAll) {
      commands.append("\n# connections;\n");
      Vector fs = stateScripts;
      int len = fs.size();
      for (int i = 0; i < len; i++)
        commands.append(fs.get(i)).append("\n");

      // labels

      viewer.loadShape(JmolConstants.SHAPE_LABELS);
      ((Labels) shapes[JmolConstants.SHAPE_LABELS]).getDefaultState(commands);

      commands.append("\n# model state;\n");
      // shape construction

    }

    setModelVisibility();

    commands.append(getProteinStructureState());
    commands.append("\n");
    
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null && (isAll || JmolConstants.isShapeSecondary(i)) 
          && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
        commands.append(cmd);
    }
    return commands.toString();
  }

  private String getProteinStructureState() {
    BitSet bs = null;
    StringBuffer cmd = new StringBuffer();
    int itype = 0;
    int id = 0;
    int iLastAtom = 0;
    int lastId = -1;
    int res1 = 0;
    int res2 = 0;
    for (int i = 0; i <= atomCount; i++) {
      id = Integer.MIN_VALUE;
      if (i == atomCount
          || (id = atoms[i].getProteinStructureID()) != lastId) {
        if (bs != null) {
          cmd.append("structure ")
              .append(JmolConstants.getProteinStructureName(itype))
              .append(" ").append(Escape.escape(bs))
              .append("    \t# model=").append(getModelName(-1 - atoms[iLastAtom].modelIndex))
              .append(" & (").append(res1).append(" - ").append(res2).append(");\n");
          bs = null;
        }
        if (id == Integer.MIN_VALUE)
          continue;
        res1 = atoms[i].getResno();
      }
      if (bs == null) {
        bs = new BitSet();
      }
      itype = atoms[i].getProteinStructureType();
      bs.set(i);
      lastId = id;
      res2 = atoms[i].getResno();
      iLastAtom = i;
    }
    return cmd.toString();
  }

  String getPdbData(String type, char ctype, BitSet bsAtoms) {
    boolean isDerivative = (type.indexOf(" deriv") >= 0);
    return mmset.getPdbData(type, ctype, bsAtoms, isDerivative);
  }
 
}
