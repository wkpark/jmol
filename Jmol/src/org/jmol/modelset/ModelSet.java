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
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomIndexIterator;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.SphereIterator;
import org.jmol.shape.Closest;
import org.jmol.shape.MeshCollection;
import org.jmol.shape.Shape;
import org.jmol.shapespecial.Dipoles;
import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.UnitCell;

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.awt.Rectangle;


/*
 * An abstract class always created using new ModelLoader(...)
 * 
 * Merged with methods in Mmset and ModelManager 10/2007  Jmol 11.3.32
 * 
 * ModelLoader simply pulls out all private classes that are
 * necessary only for file loading (and structure recalculation).
 * 
 * What is left here are all the methods that are 
 * necessary AFTER a model is loaded, when it is being 
 * accessed by Viewer, primarily.
 * 
 * Please:
 * 
 * 1) designate any methods used only here as private
 * 2) designate any methods accessed only by ModelLoader as protected
 * 3) designate any methods accessed within modelset as nothing
 * 4) designate any methods accessed only by Viewer as public
 * 
 * Bob Hanson, 5/2007, 10/2007
 * 
 */
abstract public class ModelSet {

  Viewer viewer;
  Graphics3D g3d;
  
  protected String modelSetTypeName;
  protected boolean isXYZ;
  protected boolean isPDB;
  protected Vector trajectories;

  boolean isPDB() {
    return isPDB;
  }
  
  public boolean isPDB(int modelIndex) {
    return getModel(modelIndex).isPDB;
  }

  protected boolean isZeroBased;

  public void setZeroBased() {
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

  public AtomIterator getWithinModelIterator(Atom atomCenter, float radius) {
    //Polyhedra, within()
    initializeBspf();
    withinModelIterator.initialize(bspf, atomCenter.modelIndex, atomCenter, radius);
    return withinModelIterator;
  }

  private final AtomIteratorWithinSet withinAtomSetIterator = new AtomIteratorWithinSet();

  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex, float distance, BitSet bsSelected, boolean isGreaterOnly, boolean modelZeroBased) {
    //EnvelopeCalculation, IsoSolventReader, within 
    initializeBspf();
    withinAtomSetIterator.initialize(this, bspf, atoms[atomIndex].modelIndex, atomIndex, distance, bsSelected, isGreaterOnly, modelZeroBased);
    return withinAtomSetIterator;
  }
  
  AtomIndexIterator getWithinAtomSetIterator(int modelIndex, int atomIndex, float distance) {
    //EnvelopeCalculation, IsoSolventReader, within 
    initializeBspf();
    withinAtomSetIterator.initialize(this, bspf, modelIndex, atomIndex, distance, null, false, false);
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

  public Point3f getAverageAtomPoint() {
    return averageAtomPoint;
  }

  protected final Point3f centerBoundBox = new Point3f();

  public Point3f getBoundBoxCenter() {
    return centerBoundBox;
  }

  protected final Vector3f boundBoxCornerVector = new Vector3f();

  public Vector3f getBoundBoxCornerVector() {
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
  
  public float[] getPartialCharges() {
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
  protected boolean someModelsHaveAromaticBonds;
  
  public boolean haveSymmetry() {
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

  public boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  public boolean getEchoStateActive() {
    return echoShapeActive;
  }

  public void setEchoStateActive(boolean TF) {
    echoShapeActive = TF;
  }


  protected BitSet[] elementsPresent;

  ////  atom coordinate and property changing  //////////
  
  final public static byte TAINT_COORD = 0;
  final private static byte TAINT_FORMALCHARGE = 1;
  final private static byte TAINT_OCCUPANCY = 2;
  final private static byte TAINT_PARTIALCHARGE = 3;
  final private static byte TAINT_TEMPERATURE = 4;
  final private static byte TAINT_VALENCE = 5;
  final private static byte TAINT_VIBRATION = 6;
  final private static byte TAINT_MAX = 7;
  
  
  private BitSet[] tainted;  // not final -- can be set to null

  public BitSet getTaintedAtoms(byte type) {
    return tainted == null ? null : tainted[type];
  }
  
  private void taint(int atomIndex, byte type) {
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    tainted[type].set(atomIndex);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    if (bs == null) {
      if (tainted == null)
        return;
      tainted[type] = null;
      return;
    }
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    BitSetUtil.copy(bs, tainted[type]);
  }

  Bspf bspf;

  public void loadData(String dataType, String dataString) {
    if (dataType.equalsIgnoreCase("coord")) {
      loadCoordinates(dataString, false);
      return;
    } else if (dataType.equalsIgnoreCase("vibrationvector")) {
      loadCoordinates(dataString, true);
      return;
    }
    byte type = 0;
    if (dataType.equalsIgnoreCase("formalcharge"))
      type = TAINT_FORMALCHARGE;
    else if (dataType.equalsIgnoreCase("occupancy"))
      type = TAINT_OCCUPANCY;
    else if (dataType.equalsIgnoreCase("partialcharge"))
      type = TAINT_PARTIALCHARGE;
    else if (dataType.equalsIgnoreCase("temperature"))
      type = TAINT_TEMPERATURE;
    else if (dataType.equalsIgnoreCase("valence"))
      type = TAINT_VALENCE;
    else
      return;
    
    int[] lines = Parser.markLines(dataString, ';');
    try {
      int nData = Parser.parseInt(dataString.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(dataString.substring(
            lines[i], lines[i + 1])));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        float x = Parser.parseFloat(tokens[tokens.length - 1]);
        switch (type) {
        case TAINT_FORMALCHARGE:
          atoms[atomIndex].setFormalCharge((int)x);          
          break;
        case TAINT_PARTIALCHARGE:
          atoms[atomIndex].setPartialCharge(this, x);          
          break;
        case TAINT_TEMPERATURE:
          atoms[atomIndex].setBFactor(this, x);          
          break;
        case TAINT_VALENCE:
          atoms[atomIndex].setValency((int)x);          
          break;
        }
        taint(atomIndex, type);
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }    
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors) {
    if (!isVibrationVectors)
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
        if (isVibrationVectors) {
          setAtomVibrationVector(atomIndex, x, y, z);
        } else {
          setAtomCoord(atomIndex, x, y, z);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }

  public void setAtomVibrationVector(int atomIndex, float x, float y, float z) {
    atoms[atomIndex].setVibrationVector(this, x, y, z);  
    taint(atomIndex, TAINT_VIBRATION);
  }
  
  public void setAtomCoordFractional(int atomIndex, Point3f pt) {
    atoms[atomIndex].setFractionalCoord(pt);
    taint(atomIndex, TAINT_COORD);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x = x;
    atoms[atomIndex].y = y;
    atoms[atomIndex].z = z;
    taint(atomIndex, TAINT_COORD);
  }

  public void setAtomCoordRelative(Point3f offset, BitSet bs) {
    setAtomCoordRelative(bs, offset.x, offset.y, offset.z);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x += x;
    atoms[atomIndex].y += y;
    atoms[atomIndex].z += z;
    taint(atomIndex, TAINT_COORD);
  }

  private void setAtomCoordRelative(BitSet atomSet, float x, float y, float z) {
    bspf = null;
    for (int i = atomCount; --i >= 0;)
      if (atomSet.get(i))
        setAtomCoordRelative(i, x, y, z);
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue) {
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Atom atom = atoms[i];
      switch (tok) {
      case Token.atomX:
        setAtomCoord(i, fValue, atom.y, atom.z);
        break;
      case Token.atomY:
        setAtomCoord(i, atom.x, fValue, atom.z);
        break;
      case Token.atomZ:
        setAtomCoord(i, atom.x, atom.y, fValue);
        break;
      case Token.fracX:
      case Token.fracY:
      case Token.fracZ:
        atom.setFractionalCoord(tok, fValue);
        taint(i, TAINT_COORD);
        break;
      case Token.formalCharge:
        atom.setFormalCharge(iValue);
        taint(i, TAINT_FORMALCHARGE);
        break;
      case Token.occupancy:
        atom.setOccupancy(this, iValue);
        taint(i, TAINT_OCCUPANCY);
        break;
      case Token.partialCharge:
        atom.setPartialCharge(this, fValue);
        taint(i, TAINT_PARTIALCHARGE);
        break;
      case Token.temperature:
        atom.setBFactor(this, fValue);
        taint(i, TAINT_TEMPERATURE);
        break;
      case Token.valence:
        atom.setValency(iValue);
        taint(i, TAINT_VALENCE);
        break;
      case Token.vibX:
      case Token.vibY:
      case Token.vibZ:
        atom.setVibrationVector(this, tok, fValue);
        taint(i, TAINT_VIBRATION);
        break;
      }
    }
    if ((tok == Token.valence || tok == Token.formalCharge)
        && viewer.getSmartAromatic())
      assignAromaticBonds();
  }
 
  private final Matrix3f matTemp = new Matrix3f();
  private final Matrix3f matInv = new Matrix3f();
  private final Point3f ptTemp = new Point3f();

  public void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate, BitSet bsInput,
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
        taint(i, TAINT_COORD);
        n++;
      }
    if (isInternal)
      return;
    ptTemp.scale(1f / n);
    for (int i = atomCount; --i >= 0;)
      if (bs.get(i))
        atoms[i].add(ptTemp);
  }

  public BitSet getMoleculeBitSet(BitSet bs) {
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

  //////////////  overall model set methods ////////////////
    
  public String getModelSetTypeName() {
    return modelSetTypeName;
  }

  public void setShapeSize(int shapeID, int size, BitSet bsSelected) {
    if (size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setSize(size, bsSelected);
  }

  public void loadShape(int shapeID) {
    if (shapes[shapeID] == null) {
      shapes[shapeID] = allocateShape(shapeID);
    }
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bsSelected) {
    if (shapes[shapeID] != null)
      shapes[shapeID].setProperty(propertyName.intern(), value, bsSelected);
  }

  public Object getShapeProperty(int shapeID, String propertyName, int index) {
    return (shapes[shapeID] == null ? null : shapes[shapeID].getProperty(
        propertyName, index));
  }

  public void setModelVisibility() {
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

  public BitSet setConformation(int modelIndex, int conformationIndex) {
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

  public void setTrajectory(int iTraj) {
    if (trajectories == null || iTraj < 0 || iTraj >= trajectories.size())
      return;
    Point3f[] trajectory = (Point3f[]) trajectories.get(iTraj);
      for (int i = atomCount; --i >= 0;)
        atoms[i].set(trajectory[i]);
  }

  public int getTrajectoryCount() {
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

  private String getModelSymmetryList(int modelIndex) {
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
  
  //////////// atoms //////////////
  
  public String getAtomInfo(int i) {
    return atoms[i].getInfo();
  }

  public String getAtomInfoXYZ(int i, boolean withScreens) {
    return atoms[i].getInfoXYZ(withScreens);
  }

  public String getElementSymbol(int i) {
    return atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return atoms[i].getElementNumber();
  }

  String getElementName(int i) {
      return JmolConstants.elementNameFromNumber(atoms[i]
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return atoms[i].getAtomNumber();
  }

  public float getAtomX(int i) {
    return atoms[i].x;
  }

  public float getAtomY(int i) {
    return atoms[i].y;
  }

  public float getAtomZ(int i) {
    return atoms[i].z;
  }

  public Point3f getAtomPoint3f(int i) {
    return atoms[i];
  }

  public float getAtomRadius(int i) {
    return atoms[i].getRadius();
  }

  public float getAtomVdwRadius(int i) {
    return atoms[i].getVanderwaalsRadiusFloat();
  }

  public short getAtomColix(int i) {
    return atoms[i].getColix();
  }

  public String getAtomChain(int i) {
    return "" + atoms[i].getChainID();
  }

  public String getAtomSequenceCode(int i) {
    return atoms[i].getSeqcodeString();
  }

  public int getAtomModelIndex(int i) {
    return atoms[i].getModelIndex();
  }
  
  public int getAtomCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return atomCount;
    int n = 0;
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].modelIndex == modelIndex)
        n++;
    return n;
  }
  
  public int getAtomIndexFromAtomNumber(int atomNumber) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BitSet bs, int formalCharge) {
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        atoms[i].setFormalCharge(formalCharge);
        taint(i, TAINT_FORMALCHARGE);
      }
  }
  
  public void setProteinType(BitSet bs, byte iType) {
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
  
  public float calcRotationRadius(Point3f center) {
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

  public float calcRotationRadius(BitSet bs) {
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

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  public void clearBfactorRange() {
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
  
  public Point3f[] calculateSurface(BitSet bsSelected, BitSet bsIgnore,
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
    
  public Atom getBondAtom1(int i) {
    return bonds[i].atom1;
  }

  public Atom getBondAtom2(int i) {
    return bonds[i].atom2;
  }

  public float getBondRadius(int i) {
    return bonds[i].getRadius();
  }

  public short getBondOrder(int i) {
    return bonds[i].getOrder();
  }

  public short getBondColix1(int i) {
    return bonds[i].getColix1();
  }

  public short getBondColix2(int i) {
    return bonds[i].getColix2();
  }
  
  public int getBondModelIndex(int i) {
    Atom atom = bonds[i].getAtom1();
    if (atom != null) {
      return atom.getModelIndex();
    }
    atom = bonds[i].getAtom2();
    if (atom != null) {
      return atom.getModelIndex();
    }
    return 0;
  }


  
  /**
   * for general use
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  public int getBondCountInModel(int modelIndex) {
    int n = 0;
    for (int i = bondCount; --i >= 0;)
      if (modelIndex < 0 || bonds[i].atom1.modelIndex == modelIndex)
        n++;
    return n;
  }

  public BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
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

  protected Bond setBond(int index, Bond bond) {
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

  protected final static int MAX_BONDS_LENGTH_TO_CACHE = 5;
  protected final static int MAX_NUM_TO_CACHE = 200;
  protected int[] numCached = new int[MAX_BONDS_LENGTH_TO_CACHE];
  protected Bond[][][] freeBonds = new Bond[MAX_BONDS_LENGTH_TO_CACHE][][];
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

  public Vector3f getModelDipole(int modelIndex) {
    if (modelIndex < 0)
      return null;
    Vector3f dipole = (Vector3f) getModelAuxiliaryInfo(modelIndex, "dipole");
    if (dipole == null)
      dipole = (Vector3f) getModelAuxiliaryInfo(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

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
  
  public void getBondDipoles() {
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
 
  public void rebond() {
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

  public int makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB,
                      BitSet bsBonds, boolean isBonds) {
    if (connectOperation != JmolConstants.CONNECT_IDENTIFY_ONLY) {
      String stateScript = "connect ";
      if (minDistance != JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
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
          if (order >= 0 && !identifyOnly) {
            bondAB.order = order;
            bsAromatic.clear(bondAB.index); 
          }
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

  private float hbondMax = 3.25f;
  private float hbondMin = 2.5f;
  private float hbondMin2 = hbondMin * hbondMin;

  private final static boolean useRasMolHbondsCalculation = true;

  public int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds) {
    bsPseudoHBonds = new BitSet();
    if (useRasMolHbondsCalculation && bondCount > 0) {
      //if (mmset != null)  ????????????????
      calcHydrogenBonds(bsA, bsB);
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
          getModel(modelIndex).firstMolecule = moleculeCount;
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
        getModel(thisModelIndex).moleculeCount = moleculeCount
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
  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < modelCount; i++) {
      if (modelIndex == i || modelIndex < 0)
        n += getModel(i).moleculeCount;
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

  ///////// atom and shape selecting /////////
  
  
  public boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    return (frankShape != null && frankShape.wasClicked(x, y));
  }

  /* ***************************************************************
   * shape support
   ****************************************************************/

  public int getShapeIdFromObjectName(String objectName) {
    for (int i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; i < JmolConstants.SHAPE_MAX_MESH_COLLECTION; ++i) {
      MeshCollection shape = (MeshCollection) shapes[i];
      if (shape != null && shape.getIndexFromName(objectName) >= 0)
        return i;
    }
    Dipoles dipoles = (Dipoles) shapes[JmolConstants.SHAPE_DIPOLES];
    if (dipoles != null && dipoles.getIndexFromName(objectName) >= 0)
      return JmolConstants.SHAPE_DIPOLES;
    return -1;
  }

  private final Closest closest = new Closest();

  public int findNearestAtomIndex(int x, int y) {
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

  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (rect.contains(atom.screenX, atom.screenY))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  ////////////////// atomData filling ////////////

  public void fillAtomData(AtomData atomData, int mode) {
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
  
  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
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
 
  public BitSet getModelBitSet(BitSet atomList) {
    BitSet bs = new BitSet();
    for (int i = 0; i < atomCount; i++)
      if (atomList.get(i))
        bs.set(atoms[i].modelIndex);
    return bs;
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
    case Token.hetero:
      return getHeteroSet();
    case Token.hydrogen:
      return getHydrogenSet();
    case Token.protein:
      return getProteinSet();
    case Token.carbohydrate:
      return getCarbohydrateSet();
    case Token.nucleic:
      return getNucleicSet();
    case Token.dna:
      return getDnaSet();
    case Token.rna:
      return getRnaSet();
    case Token.purine:
      return getPurineSet();
    case Token.pyrimidine:
      return getPyrimidineSet();
    case Token.isaromatic:
      return getAromaticSet();
    }
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

  private BitSet getAromaticSet() {
    BitSet bs = new BitSet();
    for (int i = bondCount; --i >= 0;)
      if (bonds[i].isAromatic()) {
        bs.set(bonds[i].getAtomIndex1());
        bs.set(bonds[i].getAtomIndex2());
      }
    return bs;
  }

  /**
   * general lookup for String type
   * @param tokType
   * @param specInfo
   * @return BitSet or null in certain cases
   */
  public BitSet getAtomBits(int tokType, String specInfo) {
    switch (tokType) {
    case Token.identifier:
      return getIdentifierOrNull(specInfo);
    case Token.spec_atom:
      return getSpecAtom(specInfo);
    case Token.spec_name_pattern:
      return getSpecName(specInfo);
    case Token.spec_alternate:
      return getSpecAlternate(specInfo);
    }
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
   * @param tokType   
   * @param specInfo  
   * @return bitset; null only if we mess up with name
   */
  public BitSet getAtomBits(int tokType, int specInfo) {
    switch (tokType) {
    case Token.spec_resid:
      return getSpecResid(specInfo);
    case Token.spec_chain:
      return getSpecChain((char) specInfo);
    case Token.spec_seqcode:
      return getSpecSeqcode(specInfo, true);
    case Token.spec_model:
      return getSpecModel(specInfo);
    case Token.atomno:
      return getSpecAtomNumber(specInfo);
    }
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
    int seqNum = Group.getSequenceNumber(seqcode);
    boolean haveSeqNumber = (seqNum != Integer.MAX_VALUE);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCode(seqcode);
    switch (insCode) {
    case '?':
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (!haveSeqNumber 
            || seqNum == Group.getSequenceNumber(atomSeqcode)
            && Group.getInsertionCodeValue(atomSeqcode) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (seqcode == atomSeqcode || 
            !haveSeqNumber && seqcode == Group.getInsertionCodeValue(atomSeqcode) 
            || insCode == '*' && seqNum == Group.getSequenceNumber(atomSeqcode)) {
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

  private BitSet getSpecModel(int modelNumber) {
    return getModelAtomBitSet(getModelNumberIndex(modelNumber, true));
  }

  private BitSet getSpecAtomNumber(int atomno) {
    //for Measures
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      if (atoms[i].getAtomNumber() == atomno)
        bs.set(i);
    }
    return bs;
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

  private BitSet getCellSet(int ix, int jy, int kz) {
    BitSet bs = new BitSet();
    Point3f cell = new Point3f(ix / 1000f, jy / 1000f, kz / 1000f);
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isInLatticeCell(cell))
        bs.set(i);
    return bs;
  }

  public BitSet getModelAtomBitSet(int modelIndex) {
    BitSet bs = new BitSet();
    for (int i = 0; i < atomCount; i++)
      if (modelIndex < 0 || atoms[i].modelIndex == modelIndex)
        bs.set(i);
    return bs;
  }

  public BitSet getAtomsWithin(float distance, Point4f plane) {
    BitSet bsResult = new BitSet();
    for (int i = getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      float d = Graphics3D.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }

  public BitSet getVisibleSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible())
        bs.set(i);
    return bs;
  }

  public BitSet getClickableSet() {
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

  public void addStateScript(String script) {
    int iModel = viewer.getCurrentModelIndex();
    if (thisStateModel != iModel) {
      thisStateModel = iModel;
      script = "frame " + (iModel < 0 ? "" + iModel : getModelName(-1 - iModel))
          + ";\n" + script;
    }
    stateScripts.addElement(script);
  }

  public String getState(StringBuffer sfunc, boolean isAll) {
    StringBuffer commands = new StringBuffer();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState();\n");
    }
    String cmd;

    // properties

    if (isAll) {
      for (byte i = 0; i < TAINT_MAX; i++)
        if(getTaintedAtoms(i) != null) 
          getTaintedState(commands, i);
    }

    // connections

    if (isAll) {
      Vector fs = stateScripts;
      int len = fs.size();
      if (len > 0) {
        commands.append("\n");
        for (int i = 0; i < len; i++)
          commands.append("  ").append(fs.get(i)).append("\n");
      }

      commands.append("\n");
      // shape construction

    }

    setModelVisibility();

    commands.append(getProteinStructureState());
    
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null && (isAll || JmolConstants.isShapeSecondary(i)) 
          && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
        commands.append(cmd);
    }
    if (sfunc != null)
      commands.append("\nend function;\n\n");
    return commands.toString();
  }

  private void getTaintedState(StringBuffer commands, byte type) {
    BitSet t = getTaintedAtoms(type);
    commands.append("\n");
    StringBuffer s = new StringBuffer();
    int n = 0;
    String dataLabel = "";
    for (int i = 0; i < atomCount; i++)
      if (t.get(i)) {
        s.append(i + 1).append(" ").append(atoms[i].getElementSymbol())
        .append(" ").append(
            TextFormat.simpleReplace(atoms[i].getIdentity(), " ", "_"))
            .append(" ");
        switch (type) {
        case TAINT_COORD:
          dataLabel = "coord set";
              s.append(" ").append(atoms[i].x).append(" ").append(atoms[i].y)
              .append(" ").append(atoms[i].z);
          break;
        case TAINT_FORMALCHARGE:
          dataLabel = "formalcharge set";
          s.append(atoms[i].getFormalCharge());
          break;
        case TAINT_PARTIALCHARGE:
          dataLabel = "partialcharge set";
          s.append(atoms[i].getPartialCharge());
          break;
        case TAINT_TEMPERATURE:
          dataLabel = "temperature set";
          s.append(atoms[i].getBfactor100() / 100f);
          break;
        case TAINT_VALENCE:
          dataLabel = "valence set";
          s.append(atoms[i].getValence());
          break;
        case TAINT_VIBRATION:
          dataLabel = "vibrationvector set";
          Vector3f v = atoms[i].getVibrationVector();
          if (v == null)
            v = new Vector3f();
          s.append(" ").append(v.x).append(" ").append(v.y).append(" ").append(v.z);
        }
        s.append(" ;\n");
        ++n;
      }
    commands.append("  DATA \"" + dataLabel + "\"\n").append(n).append(
        " ;\nJmol Property Data Format 1 -- Jmol ").append(
        Viewer.getJmolVersion()).append(";\n");
    commands.append(s);
    commands.append("  end \"" + dataLabel + "\";\n");
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
    int n = 0;
    for (int i = 0; i <= atomCount; i++) {
      id = Integer.MIN_VALUE;
      if (i == atomCount
          || (id = atoms[i].getProteinStructureID()) != lastId) {
        if (bs != null) {
          n++;
          cmd.append("  structure ")
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
    if (n > 0)
      cmd.append("\n");
    return cmd.toString();
  }

  String getPdbData(String type, char ctype, BitSet bsAtoms) {
    boolean isDerivative = (type.indexOf(" deriv") >= 0);
    return getPdbData(type, ctype, bsAtoms, isDerivative);
  }
  
  /*
   * aromatic single/double bond assignment 
   * by Bob Hanson, hansonr@stolaf.edu, Oct. 2007
   * Jmol 11.3.29.
   * 
   * This algorithm assigns alternating single/double bonds to all 
   * sets of bonds of type AROMATIC in a system. Any bonds already
   * assigned AROMATICSINGLE or AROMATICDOUBLE by the user are preserved.
   * 
   * In this way the user can assign ONE bond, and Jmol will take it from
   * there.
   * 
   * The algorithm is highly recursive.
   * 
   * We track two bond bitsets: bsAromaticSingle and bsAromaticDouble.
   *  
   * Loop through all aromatic bonds. 
   *   If unassigned, assignAromaticDouble(Bond bond).
   *   If unsuccessful, assignAromaticSingle(Bond bond).
   * 
   * assignAromaticDouble(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticDouble.set(thisBond)
   *   
   *   For each aromatic bond connected to each atom that is not
   *   already assigned AROMATICSINGLE or AROMATICDOUBLE:
   *   
   *     assignAromaticSingle(Bond bond)
   *     
   *   If unsuccessful, bsAromaticDouble.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   * 
   * assignAromaticSingle(Bond bond):
   * 
   *   Each of the two atoms must have exactly one double bond.
   *   
   *   bsAromaticSingle.set(thisBond)
   *   
   *   For each aromatic bond connected to this atom that is not
   *   already assigned:
   *   
   *     for one: assignAromaticDouble(Bond bond) 
   *     the rest: assignAromaticSingle(Bond bond)
   *     
   *   If two AROMATICDOUBLE bonds to the same atom are found
   *   or unsuccessful in assigning AROMATICDOUBLE or AROMATICSINGLE, 
   *   bsAromaticSingle.clear(thisBond) and 
   *   return FALSE, otherwise return TRUE.
   *   
   * The process continues until all bonds are processed. It is quite
   * possible that the first assignment will fail either because somewhere
   * down the line the user has assigned an incompatible AROMATICDOUBLE or
   * AROMATICSINGLE bond. 
   * 
   * This is no problem though, because the assignment is self-correcting, 
   * and in the second pass the process will be opposite, and success will
   * be achieved.
   * 
   * It is possible that no correct assignment is possible because the structure
   * has no valid closed-shell Lewis structure. In that case, AROMATICSINGLE 
   * bonds will be assigned to problematic areas.  
   * 
   * Bob Hanson -- 10/2007
   * 
   */

  private BitSet bsAromaticSingle;
  private BitSet bsAromaticDouble;
  private BitSet bsAromatic = new BitSet();

  public void resetAromatic() {
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (bond.isAromatic())
        bond.order = JmolConstants.BOND_AROMATIC;
    }
  }
  
  public void assignAromaticBonds() {
    assignAromaticBonds(true);
  }

  /**
   * algorithm discussed above.
   * 
   * @param isUserCalculation   if set, don't reset the base aromatic bitset
   *                            and do report changes to STICKS as though this
   *                            were a bondOrder command.  
   */
  protected void assignAromaticBonds(boolean isUserCalculation) {
    // bsAromatic tracks what was originally in the file, but
    // individual bonds are cleared if the connect command has been used.
    // in this way, users can override the file designations.
    if (!isUserCalculation)
      bsAromatic = new BitSet();
    
    //set up the two temporary bitsets and reset bonds.
    
    bsAromaticSingle = new BitSet();
    bsAromaticDouble = new BitSet();
    for (int i = bondCount; --i >= 0;) {
      Bond bond = bonds[i];
      if (bsAromatic.get(i))
        bond.order = JmolConstants.BOND_AROMATIC;
      switch (bond.order) {
      case JmolConstants.BOND_AROMATIC:
        bsAromatic.set(i);
        break;
      case JmolConstants.BOND_AROMATIC_SINGLE:
        bsAromaticSingle.set(i);
        break;
      case JmolConstants.BOND_AROMATIC_DOUBLE:
        bsAromaticDouble.set(i);
        break;
      }
    }
    // main recursive loop
    Bond bond;
    for (int i = bondCount; --i >= 0;) {
      bond = bonds[i];
      if (bond.order != JmolConstants.BOND_AROMATIC 
          || bsAromaticDouble.get(i) || bsAromaticSingle.get(i))
        continue;
      if (!assignAromaticDouble(bond))
        assignAromaticSingle(bond);
    }
    // all done: do the actual assignments and clear arrays.
    for (int i = bondCount; --i >= 0;) {
      bond = bonds[i];      
      if (bsAromaticDouble.get(i)) {
        if(bond.order != JmolConstants.BOND_AROMATIC_DOUBLE) {
          bsAromatic.set(i);
          bond.order = JmolConstants.BOND_AROMATIC_DOUBLE;
        }
      } else if (bsAromaticSingle.get(i) || bond.isAromatic()) {
        if(bond.order != JmolConstants.BOND_AROMATIC_SINGLE) {
          bsAromatic.set(i);
          bond.order = JmolConstants.BOND_AROMATIC_SINGLE;
        }
      }
    }

    assignAromaticNandO();
    
    bsAromaticSingle = null;
    bsAromaticDouble = null;

    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.
    
    if (isUserCalculation)
      setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, bsAromatic);

}

  /**
   * try to assign AROMATICDOUBLE to this bond. Each atom needs to be
   * have all single bonds except for this one.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticDouble(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticSingle.get(bondIndex))
      return false;
    if (bsAromaticDouble.get(bondIndex))
      return true;
    bsAromaticDouble.set(bondIndex);
    if (!assignAromaticSingle(bond.atom1, bondIndex)
        || !assignAromaticSingle(bond.atom2, bondIndex)) {
      bsAromaticDouble.clear(bondIndex);
      return false;
    }
    return true;
  }
  
  /**
   * try to assign AROMATICSINGLE to this bond. Each atom needs to be
   * able to have one aromatic double bond attached.  
   * 
   * @param bond
   * @return      true if successful; false otherwise
   */
  private boolean assignAromaticSingle(Bond bond) {
    int bondIndex = bond.index;
    if (bsAromaticDouble.get(bondIndex))
      return false;
    if (bsAromaticSingle.get(bondIndex))
      return true;
    bsAromaticSingle.set(bondIndex);
    if (!assignAromaticDouble(bond.atom1) || !assignAromaticDouble(bond.atom2)) {
      bsAromaticSingle.clear(bondIndex);
      return false;
    }
    return true;
  }

  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms needs all single bonds, 
   * because the bond leading up to it is double.
   * 
   * @param atom
   * @param notBondIndex  that index of the bond leading to this atom --- to be ignored
   * @return      true if successful, false if not
   */
  private boolean assignAromaticSingle(Atom atom, int notBondIndex) {
    Bond[] bonds = atom.bonds;
    if (assignAromaticSingleHetero(atom))
      return false;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (bondIndex == notBondIndex || !bond.isAromatic()
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (bsAromaticDouble.get(bondIndex) || !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return true;
  }
 
  /**
   * N atoms with 3 bonds cannot also have a double bond; 
   * other atoms need one and only one double bond;
   * the rest must be single bonds.
   * 
   * @param atom
   * @return      true if successful, false if not
   */
  private boolean assignAromaticDouble(Atom atom) {
    Bond[] bonds = atom.bonds;
    boolean haveDouble = assignAromaticSingleHetero(atom);
    int lastBond = -1;
    for (int i = bonds.length; --i >= 0;) {
      if (bsAromaticDouble.get(bonds[i].index))
        haveDouble = true;
      if (bonds[i].isAromatic())
        lastBond = i;
    }
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      int bondIndex = bond.index;
      if (!bond.isAromatic() || bsAromaticDouble.get(bondIndex)
          || bsAromaticSingle.get(bondIndex))
        continue;
      if (!haveDouble && assignAromaticDouble(bond))
        haveDouble = true;
      else if ((haveDouble || i < lastBond) && !assignAromaticSingle(bond)) {
        return false;
      }
    }
    return haveDouble;
  } 
  
  private boolean assignAromaticSingleHetero(Atom atom) {
    // only C N O S may be a problematic:
    int n = atom.getElementNumber();
    switch (n) {
    case 6: // C
    case 7: // N
    case 8: // O
    case 16: // S
      break;
    default:
      return true;
    }
    int nAtoms = atom.getValence();
    switch (n) {
    case 6: // C
      return (nAtoms == 4);
    case 7: // N
    case 8: // O
      return (nAtoms == 10 - n && atom.getFormalCharge() < 1);
    case 16: // S
      return (nAtoms == 18 - n && atom.getFormalCharge() < 1);
    }
    return false;
  }
  
  private void assignAromaticNandO() {
    Bond bond;
    for (int i = bondCount; --i >= 0;) {
      bond = bonds[i];
      if (bond.order != JmolConstants.BOND_AROMATIC_SINGLE) 
        continue;
      Atom atom1;
      Atom atom2 = bond.atom2;
      int n1;
      int n2 = atom2.getElementNumber();
      if (n2 == 7 || n2 == 8) {
        n1 = n2;
        atom1 = atom2;
        atom2 = bond.atom1;
        n2 = atom2.getElementNumber();
      } else {
        atom1 = bond.atom1;
        n1 = atom1.getElementNumber();
      }
      if (n1 != 7 && n1 != 8)
        continue;
      int valence = atom1.getValence();
      int bondorder = atom1.getCovalentBondCount();
      int charge = atom1.getFormalCharge();
      switch (n1) {
      case 7:
        //trivalent nonpositive N with lone pair in p orbital
        //next to trivalent C --> N=C
        if (valence == 3 && bondorder == 3 
            && charge < 1 
            && n2 == 6 && atom2.getValence() == 3)
          bond.order = JmolConstants.BOND_AROMATIC_DOUBLE; 
        break;
      case 8:
        //monovalent nonnegative O next to P or S
        if (valence == 1 && charge == 0 
            && (n2 == 14 || n2 == 16))
          bond.order = JmolConstants.BOND_AROMATIC_DOUBLE; 
        break;
      }
    }
  }

  /////////////old mmset methods ///////////////
  
  Properties modelSetProperties;
  Hashtable modelSetAuxiliaryInfo;

  private Properties[] modelProperties = new Properties[1];
  private Hashtable[] modelAuxiliaryInfo = new Hashtable[1];
  private Model[] models = new Model[1];

  private int structureCount = 0;
  private Structure[] structures = new Structure[10];

  void merge(ModelSet modelSet) {
    for (int i = 0; i < modelSet.modelCount; i++) {
      models[i] = modelSet.models[i];
      modelProperties[i] = modelSet.getModelProperties(i);
      modelAuxiliaryInfo[i] = modelSet.getModelAuxiliaryInfo(i);
    }    
  }
  
  void defineStructure(int modelIndex, String structureType, char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID, int endSequenceNumber,
                       char endInsertionCode) {
    if (structureCount == structures.length)
      structures = (Structure[]) ArrayUtil
          .setLength(structures, structureCount + 10);
    structures[structureCount++] = new Structure(modelIndex, structureType, startChainID,
        Group.getSeqcode(startSequenceNumber, startInsertionCode), endChainID,
        Group.getSeqcode(endSequenceNumber, endInsertionCode));
  }

  void clearStructures(BitSet alreadyDefined) {
    for (int i = modelCount; --i >= 0;)
      if (models[i].isPDB && !alreadyDefined.get(i))
        models[i].clearStructures();
  }

  /**
   * allows rebuilding of PDB structures;
   * also accessed by ModelManager from Eval
   * 
   * @param alreadyDefined    set to skip calculation
   *  
   */
  void calculateStructuresAllExcept(BitSet alreadyDefined) {
    for (int i = modelCount; --i >= 0;)
      if (models[i].isPDB && !alreadyDefined.get(i))
        models[i].calculateStructures();
    for (int i = modelCount; --i >= 0;)
      models[i].freeze();
    propogateSecondaryStructure();
  }

  public BitSet setConformation(int modelIndex, BitSet bsConformation) {
    for (int i = modelCount; --i >= 0;)
      if (i == modelIndex || modelIndex < 0)
      models[i].setConformation(bsConformation);
    return bsConformation;
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

  void setModelCount() {
    models = (Model[]) ArrayUtil.setLength(models, modelCount);
    modelProperties = (Properties[]) ArrayUtil
        .setLength(modelProperties, modelCount);
    modelAuxiliaryInfo = (Hashtable[]) ArrayUtil.setLength(modelAuxiliaryInfo,
        modelCount);
  }

  public String getModelTitle(int modelIndex) {
    return models[modelIndex].modelTitle;
  }
  
  public String getModelFile(int modelIndex) {
    return models[modelIndex].modelFile;
  }
  
  int getFirstAtomIndexInModel(int modelIndex) {
    return models[modelIndex].firstAtomIndex;  
  }
  
  void setFirstAtomIndex(int modelIndex, int atomIndex) {
    models[modelIndex].firstAtomIndex = atomIndex;  
  }
  
  public String getModelName(int modelIndex) {
    if (modelIndex < 0)
      return getModelNumberDotted(-1 - modelIndex);
    return models[modelIndex].modelTag;
  }

  String getModelNumberDotted(int modelIndex) {
    if (modelCount < 1)
      return "";
    return models[modelIndex].modelNumberDotted;
  }
  
  public int getModelNumberIndex(int modelNumber, boolean useModelNumber) {
    if (useModelNumber) {
      for (int i = 0; i < modelCount; i++)
        if (models[i].modelNumber == modelNumber)
          return i;
      return -1;
    }
    //new decimal format:   frame 1.2 1.3 1.4
    for (int i = 0; i < modelCount; i++)
      if (models[i].modelFileNumber == modelNumber)
        return i;
    return -1;
  }

  public int getModelNumber(int modelIndex) {
    return models[modelIndex].modelNumber;
  }

  public int getModelFileNumber(int modelIndex) {
    return models[modelIndex].modelFileNumber;
  }

  public Properties getModelProperties(int modelIndex) {
    return modelProperties[modelIndex];
  }

  public String getModelProperty(int modelIndex, String property) {
    Properties props = modelProperties[modelIndex];
    return props == null ? null : props.getProperty(property);
  }

  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : modelAuxiliaryInfo[modelIndex]);
  }

  public void setModelAuxiliaryInfo(int modelIndex, Object key, Object value) {
    modelAuxiliaryInfo[modelIndex].put(key, value);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String key) {
    if (modelIndex < 0)
      return null;
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    return info == null ? null : info.get(key);
  }

  protected boolean getModelAuxiliaryInfoBoolean(int modelIndex, String keyName) {
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  protected int getModelAuxiliaryInfoInt(int modelIndex, String keyName) {
    Hashtable info = modelAuxiliaryInfo[modelIndex];
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  Model getModel(int modelIndex) {
    return models[modelIndex];
  }

  int getNAltLocs(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }
    
  public int getInsertionCountInModel(int modelIndex) {
    return models[modelIndex].nInsertions;
  }
    
  boolean setModelNameNumberProperties(int modelIndex, String modelName,
                                       int modelNumber,
                                       Properties modelProperties,
                                       Hashtable modelAuxiliaryInfo,
                                       boolean isPDB, String jmolData) {

    this.modelProperties[modelIndex] = modelProperties;
    if (modelAuxiliaryInfo == null)
      modelAuxiliaryInfo = new Hashtable();
    this.modelAuxiliaryInfo[modelIndex] = modelAuxiliaryInfo;
    String modelTitle = (String) getModelAuxiliaryInfo( modelIndex, "title");
    if (jmolData != null) {
      modelAuxiliaryInfo.put("jmolData", jmolData);
      modelTitle = jmolData;
    }
    String modelFile = (String) getModelAuxiliaryInfo( modelIndex, "fileName");
    if (modelNumber != Integer.MAX_VALUE)
      models[modelIndex] = new Model(this, modelIndex, modelNumber, modelName,
          modelTitle, modelFile, jmolData);
    String codes = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
    models[modelIndex].setNAltLocs(codes == null ? 0 : codes.length());
    codes = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
    models[modelIndex].setNInsertions(codes == null ? 0 : codes.length());
    return models[modelIndex].isPDB = getModelAuxiliaryInfoBoolean(modelIndex, "isPDB");
  }
  
  /**
   * Model numbers are considerably more complicated in Jmol 11.
   * 
   * int modelNumber
   *  
   *   The adapter gives us a modelNumber, but that is not necessarily
   *   what the user accesses. If a single files is loaded this is:
   *   
   *   a) single file context:
   *   
   *     1) the sequential number of the model in the file , or
   *     2) if a PDB file and "MODEL" record is present, that model number
   *     
   *   b) multifile context:
   *   
   *     always 1000000 * (fileIndex + 1) + (modelIndexInFile + 1)
   *   
   *   
   * int fileIndex
   * 
   *   The 0-based reference to the file containing this model. Used
   *   when doing   "select model=3.2" in a multifile context
   *   
   * int modelFileNumber
   * 
   *   An integer coding both the file and the model:
   *   
   *     file * 1000000 + modelInFile (1-based)
   *     
   *   Used all over the place. Note that if there is only one file,
   *   then modelFileNumber < 1000000.
   * 
   * String modelNumberDotted
   *   
   *   A number the user can use "1.3"
   *   
   * @param baseModelCount
   *    
   */
  void finalizeModelNumbers(int baseModelCount) {
    if (modelCount == baseModelCount)
      return;
    String sNum;
    int modelnumber = 0;
    
    int lastfilenumber = -1;
    if (baseModelCount > 0) {
      if (models[0].modelNumber < 1000000) {
        for (int i = 0; i < baseModelCount; i++) {
          models[i].modelNumber = 1000000 + i + 1;
          models[i].modelNumberDotted = "1." + (i + 1);
          if (models[i].modelTag.length() == 0)
            models[i].modelTag = "" + models[i].modelNumber;
        }
      }
      modelnumber = models[baseModelCount - 1].modelNumber;
      modelnumber -= modelnumber % 1000000;
      if (models[baseModelCount].modelNumber < 1000000)
        modelnumber += 1000000;
      for (int i = baseModelCount; i < modelCount; i++) {
        models[i].modelNumber += modelnumber;
        models[i].modelNumberDotted = (modelnumber / 1000000) + "." + (modelnumber % 1000000);
        if (models[i].modelTag.length() == 0)
          models[i].modelTag = "" + models[i].modelNumber;
      }
    }
    for (int i = baseModelCount; i < modelCount; ++i) {
      int filenumber = models[i].modelNumber / 1000000;
      if (filenumber != lastfilenumber) {
        modelnumber = 0;
        lastfilenumber = filenumber;
      }
      modelnumber++;
      if (filenumber == 0) {
        // only one file -- take the PDB number or sequential number as given by adapter
        sNum = "" + getModelNumber(i);
        filenumber = 1;
      } else {
//        //if only one file, just return the integer file number
  //      if (modelnumber == 1
    //        && (i + 1 == modelCount || models[i + 1].modelNumber / 1000000 != filenumber))
      //    sNum = filenumber + "";
       // else
          sNum = filenumber + "." + modelnumber;
      }
      models[i].modelNumberDotted = sNum;
      models[i].fileIndex = filenumber - 1;
      models[i].modelInFileIndex = modelnumber - 1;
      models[i].modelFileNumber = filenumber * 1000000 + modelnumber;
    }
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  
    
    int file = (int)(fDotM);
    int model = (int) ((fDotM - file +0.00001) * 10000);
    while (model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }
  
  public int getAltLocCountInModel(int modelIndex) {
    return models[modelIndex].nAltLocs;
  }
  

  private void propogateSecondaryStructure() {
    // issue arises with multiple file loading and multi-_data  mmCIF files
    // that structural information may be model-specific
    for (int i = structureCount; --i >= 0;) {
      Structure structure = structures[i];
      for (int j = modelCount; --j >= 0;)
        if (structure.modelIndex == j || structure.modelIndex == -1) {
          models[j].addSecondaryStructure(structure.type,
              structure.startChainID, structure.startSeqcode,
              structure.endChainID, structure.endSeqcode);
        }
    }
  }

  public Model[] getModels() {
    return models;
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
      polymerCount += models[i].getBioPolymerCount();
    return polymerCount;
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getBioPolymerCount();
    return models[modelIndex].getBioPolymerCount();
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
    for (int i = modelCount; --i >= 0; )
      models[i].calcHydrogenBonds(bsA, bsB);
  }

  static class Structure {
    String typeName;
    byte type;
    char startChainID;
    int startSeqcode;
    char endChainID;
    int endSeqcode;
    int modelIndex;

    Structure(int modelIndex, String typeName, char startChainID, int startSeqcode,
        char endChainID, int endSeqcode) {
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
  private String getPdbData(String type, char ctype, BitSet bsAtoms,
                    boolean isDerivative) {
    StringBuffer pdbATOM = new StringBuffer();
    StringBuffer pdbCONECT = new StringBuffer();
    int firstAtom = BitSetUtil.firstSetBit(bsAtoms);
    if (firstAtom < 0)
      return null;
    int modelIndex = atoms[firstAtom].modelIndex;
    if (isJmolDataFrame(modelIndex))
      return null;
    int nPoly = models[modelIndex].getBioPolymerCount();
    for (int p = 0; p < nPoly; p++)
          models[modelIndex]
              .getPdbData(ctype, isDerivative, bsAtoms, pdbATOM, pdbCONECT);
    pdbATOM.append(pdbCONECT);
    return pdbATOM.toString();
  }
  
  public boolean isJmolDataFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount 
        && models[modelIndex].isJmolDataFrame());
  }

  protected Hashtable htJmolData = new Hashtable();
  public void setPtJmolDataFrame(String type, int modelIndex) {
    htJmolData.put(type, new Integer(modelIndex));
  }
  
  public int getPtJmolDataFrame(String type) {
    Integer iModel = (Integer) htJmolData.get(type); 
    if (iModel == null)
      return -1;
    return iModel.intValue();
  }

  public String getJmolDataFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < modelCount ? 
        models[modelIndex].getJmolDataFrameType() : "modelSet");
  }

  /////////// from modelManager
  
  public boolean checkObjectHovered(int x, int y) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectHovered(x, y))
      return true;
    shape = shapes[JmolConstants.SHAPE_DRAW];
    if (shape == null || !viewer.getDrawHover())
      return false;
    return shape.checkObjectHovered(x, y);
  }
 
  public String getPdbData(String type, BitSet bsAtoms) {
    char ctype = (type.length() > 11 && type.indexOf("quaternion ") >= 0 ? type.charAt(11) : 'r');
    String s = getPdbData(type, ctype, bsAtoms);
    if (s == null)
      return null;
    String remark = "REMARK 999 Jmol PDB-encoded data: " + type
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
    return remark + getPDBStructureInfo() + s;
  }

  public UnitCell getUnitCell(int modelIndex) {
    if (modelIndex < 0)
      return null;
    return (cellInfos == null ? null : cellInfos[modelIndex].getUnitCell());
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
  
  public void calculateStructures(int modelIndex) {
    BitSet bsDefined = new BitSet(modelCount);
    for (int i = 0; i < modelCount; i++)
      if (modelIndex >= 0 && i != modelIndex)
        bsDefined.set(i);
    calculateStructuresAllExcept(bsDefined);
  }
  
  public String getModelInfoAsString() {
    String str =  "model count = " + modelCount +
                 "\nmodelSetHasVibrationVectors:" +
                 modelSetHasVibrationVectors();
    Properties props = getModelSetProperties();
    str = str.concat(listProperties(props));
    for (int i = 0; i < modelCount; ++i) {
      str = str.concat("\n" + i + ":" + getModelName(-1 -i) +
                 ":" + getModelTitle(i) +
                 "\nmodelHasVibrationVectors:" +
                 modelHasVibrationVectors(i));
      //str = str.concat(listProperties(getModelProperties(i)));
    }
    return str;
  }
  
  public String getSymmetryInfoAsString() {
    String str = "Symmetry Information:";
    for (int i = 0; i < modelCount; ++i) {
      str += "\nmodel #" + getModelName(-1 - i) + "; name=" + getModelName(i) + "\n"
          + getSymmetryInfoAsString(i);
    }
    return str;
  }

  public BitSet getAtomsWithin(int tokType, String specInfo, BitSet bs) {
    if (tokType == Token.sequence)
      return withinSequence(specInfo, bs);
    return null;
  }
  
  public BitSet getAtomsWithin(int tokType, BitSet bs) {
    switch (tokType) {
    case Token.group:
      return withinGroup(bs);
    case Token.chain:
      return withinChain(bs);
    case Token.molecule:
      return getMoleculeBitSet(bs);
    case Token.model:
      return withinModel(bs);
    case Token.element:
      return withinElement(bs);
    case Token.site:
      return withinSite(bs);
    }
    return null;
  }

  private BitSet withinGroup(BitSet bs) {
    //Logger.debug("withinGroup");
    Group groupLast = null;
    BitSet bsResult = new BitSet();
    for (int i = getAtomCount(); --i >= 0;) {
      if (!bs.get(i))
        continue;
      Atom atom = atoms[i];
      Group group = atom.getGroup();
      if (group != groupLast) {
        group.selectAtoms(bsResult);
        groupLast = group;
      }
    }
    return bsResult;
  }

  private BitSet withinChain(BitSet bs) {
    Chain chainLast = null;
    BitSet bsResult = new BitSet();
    int atomCount = getAtomCount();
    for (int i = atomCount; --i >= 0;) {
      if (!bs.get(i))
        continue;
      Chain chain = atoms[i].getChain();
      if (chain != chainLast) {
        for (int j = atomCount; --j >= 0;)
          if (atoms[j].getChain() == chain)
            bsResult.set(j);
        chainLast = chain;
      }
    }
    return bsResult;
  }

  private BitSet withinModel(BitSet bs) {
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(atoms[i].modelIndex);
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(atoms[i].modelIndex))
        bsResult.set(i);
    return bsResult;
  }

  private BitSet withinSite(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(atoms[i].atomSite);
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(atoms[i].atomSite))
        bsResult.set(i);
    return bsResult;
  }

  private BitSet withinElement(BitSet bs) {
    //Logger.debug("withinGroup");
    BitSet bsResult = new BitSet();
    BitSet bsThis = new BitSet();
    for (int i = getAtomCount(); --i >= 0;)
      if (bs.get(i))
        bsThis.set(getElementNumber(i));
    for (int i = getAtomCount(); --i >= 0;)
      if (bsThis.get(getElementNumber(i)))
        bsResult.set(i);
    return bsResult;
  }

  private BitSet withinSequence(String specInfo, BitSet bs) {
    //Logger.debug("withinSequence");
    String sequence = "";
    int lenInfo = specInfo.length();
    BitSet bsResult = new BitSet();
    if (lenInfo == 0)
      return bsResult;
    int modelCount = viewer.getModelCount();
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

  public BitSet getAtomsWithin(float distance, BitSet bs,
                               boolean withinAllModels) {
    BitSet bsResult = new BitSet();
    if (withinAllModels) {
      bsResult.or(bs);
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i))
          for (int model = modelCount; --model >= 0;) {
            AtomIndexIterator iterWithin = getWithinAtomSetIterator(
                model, i, distance);
            while (iterWithin.hasNext())
              bsResult.set(iterWithin.next());
          }
    } else {
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          Atom atom = atoms[i];
          AtomIterator iterWithin = getWithinModelIterator(atom, distance);
          while (iterWithin.hasNext())
            bsResult.set(iterWithin.next().getAtomIndex());
        }
    }
    return bsResult;
  }

  public BitSet getAtomsWithin(float distance, Point3f coord) {
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (atom.distance(coord) <= distance)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }
 
  public BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    BitSet bsResult = new BitSet();
    int atomCount = getAtomCount();
    int[] nBonded = new int[atomCount];
    int bondCount = getBondCount();
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
    int atomCount = getAtomCount();
    int bondCount = getBondCount();
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
      if (bs.get(bonds[i].getAtom1().atomIndex) 
          && bs.get(bonds[i].getAtom2().atomIndex)) {
        int order = getBondOrder(i);
        if (order >= 1 && order < 3) {
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
    rFill(mol, "   ",""+nAtoms);
    rFill(mol, "   ",""+nBonds);
    mol.append("  0  0  0\n");
    mol.append(s);
    return mol.toString();
  }
  
  void getAtomRecordMOL(StringBuffer s, int i){
    //   -0.9920    3.2030    9.1570 Cl  0  0  0  0  0
    //    3.4920    4.0920    5.8700 Cl  0  0  0  0  0
    //012345678901234567890123456789012
    rFill(s, "          " ,safeTruncate(getAtomX(i),9));
    rFill(s, "          " ,safeTruncate(getAtomY(i),9));
    rFill(s, "          " ,safeTruncate(getAtomZ(i),9));
    s.append(" ").append((getElementSymbol(i) + "  ").substring(0,2)).append("\n");
  }

  void getBondRecordMOL(StringBuffer s, int i,int[] atomMap){
  //  1  2  1
    Bond b = bonds[i];
    rFill(s, "   ","" + atomMap[b.getAtom1().atomIndex]);
    rFill(s, "   ","" + atomMap[b.getAtom2().atomIndex]);
    s.append("  ").append(getBondOrder(i)).append("\n"); 
  }
  
  private void rFill(StringBuffer s, String s1, String s2) {
    s.append(s1.substring(0, s1.length() - s2.length()));
    s.append(s2);
  }
  
  private String safeTruncate(float f, int n) {
    if (f > -0.001 && f < 0.001)
      f = 0;
    return (f + "         ").substring(0,n);
  }

  protected String modelSetName;

  public String getModelSetName() {
    return modelSetName;
  }

  public Hashtable getModelInfo() {
    Hashtable info = new Hashtable();
    int modelCount = getModelCount();
    info.put("modelSetName",getModelSetName());
    info.put("modelCount",new Integer(modelCount));
    info.put("modelSetHasVibrationVectors", 
        Boolean.valueOf(modelSetHasVibrationVectors()));
    Properties props = viewer.getModelSetProperties();
    if(props != null)
      info.put("modelSetProperties",props);
    Vector models = new Vector();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable model = new Hashtable();
      model.put("_ipt",new Integer(i));
      model.put("num",new Integer(getModelNumber(i)));
      model.put("file_model",getModelName(-1 - i));
      model.put("name", getModelName(i));
      String s = getModelTitle(i);
      if (s != null)
        model.put("title", s);
      model.put("file", getModelFile(i));
      model.put("vibrationVectors", Boolean.valueOf(modelHasVibrationVectors(i)));
      model.put("atomCount",new Integer(getAtomCountInModel(i)));
      model.put("bondCount",new Integer(getBondCountInModel(i)));
      model.put("groupCount",new Integer(getGroupCountInModel(i)));
      model.put("polymerCount",new Integer(getBioPolymerCountInModel(i)));
      model.put("chainCount",new Integer(getChainCountInModel(i)));      
      props = getModelProperties(i);
      if (props != null)
        model.put("modelProperties", props);
      models.addElement(model);
    }
    info.put("models",models);
    return info;
  }

  public String getModelFileInfo(BitSet frames) {
    String str = "";
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      if (!frames.get(i))
        continue;
      String file_model = getModelName(-1 - i);
      str += "\n\nfile[\"" + file_model + "\"] = " 
          + Escape.escape(getModelFile(i)) 
          + "\ntitle[\"" + file_model + "\"] = " 
          + Escape.escape(getModelTitle(i))
          + "\nname[\"" + file_model + "\"] = "
          + Escape.escape(getModelName(i));
    }
    return str;
  }
  
  public Hashtable getAuxiliaryInfo() {
    Hashtable info = getModelSetAuxiliaryInfo();
    if (info == null)
      return info;
    Vector models = new Vector();
    int modelCount = viewer.getModelCount();
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
    if (vibrationVectors != null && vibrationVectors[i] != null) {
      info.put("vibVector", new Vector3f(vibrationVectors[i]));
    }
    info.put("bondCount", new Integer(atom.getCovalentBondCount()));
    info.put("radius", new Float((atom.getRasMolRadius() / 120.0)));
    info.put("model", new Integer(atom.getModelNumberDotted()));
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
    if (isPDB(atom.modelIndex)) {
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
    int bondCount = getBondCount();
    for (int i = 0; i < bondCount; i++)
      if (bs.get(bonds[i].getAtom1().atomIndex) 
          && bs.get(bonds[i].getAtom2().atomIndex)) 
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
    info.put("order", new Integer(getBondOrder(i)));
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
  
  private String listProperties(Properties props) {
    String str = "";
    if (props == null) {
      str = str.concat("\nProperties: null");
    } else {
      Enumeration e = props.propertyNames();
      str = str.concat("\nProperties:");
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        str = str.concat("\n " + propertyName + "=" +
                   props.getProperty(propertyName));
      }
    }
    return str;
  }

  public Hashtable getAllChainInfo(BitSet bs) {
    Hashtable finalInfo = new Hashtable();
    Vector modelVector = new Vector();
    int modelCount = getModelCount();
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

  public void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    int modelCount = viewer.getModelCount();
    boolean isTraceAlpha = viewer.getTraceAlpha();
    float sheetSmoothing = viewer.getSheetSmoothing();
    int last = Integer.MAX_VALUE - 1;
    for (int i = 0; i < modelCount; ++i) {
      int polymerCount = getBioPolymerCountInModel(i);
      for (int ip = 0; ip < polymerCount; ip++)
        last = models[i].getBioPolymer(ip)
            .getPolymerPointsAndVectors(last, bs, vList, isTraceAlpha, sheetSmoothing);
    }
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
    int modelCount = getModelCount();
    for (int i = 0; i < modelCount; ++i) {
      Hashtable modelInfo = new Hashtable();
      Vector info = new Vector();
      int polymerCount = getBioPolymerCountInModel(i);
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

  private String pdbHeader;
  /*
  final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
    "MODEL ", "SCALE",  "HETATM", "SEQRES",
    "DBREF ", };
*/

  final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  private String getFullPDBHeader() {
    String info = (pdbHeader == null ? (pdbHeader = viewer.getCurrentFileAsString()) : pdbHeader);
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0 : 
        info.indexOf("\n" + strRecord))) {
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

  public String getFileHeader() {
    if (isPDB()) 
      return getFullPDBHeader();
    String info = getModelSetProperty("fileHeader");
    if (info == null)
      info = getModelSetName();
    if (info != null) return info;
    return "no header information found";
  }

  public String getPDBHeader() {
    return (isPDB() ? getFullPDBHeader() : getFileHeader());
  }
  
  public String getPDBStructureInfo() {
    if (isPDB())
      return "";
    getFullPDBHeader();
    int lines[] = Parser.markLines(pdbHeader, '\n');
    StringBuffer str = new StringBuffer();
    int n = lines.length - 1;
    for (int i = 0; i < n; i++) {
      String line = pdbHeader.substring(lines[i], lines[i + 1]);
      if (
          line.indexOf("HEADER") == 0 || 
          line.indexOf("COMPND") == 0 || 
          line.indexOf("SOURCE") == 0 || 
          line.indexOf("HELIX") == 0 || 
          line.indexOf("SHEET") == 0 || 
          line.indexOf("TURN") == 0)
        str.append(line);
    }
    return str.toString();
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
    } else {
      sg = SpaceGroup.determineSpaceGroup(spaceGroup);
      if (sg == null)
        sg = SpaceGroup.createSpaceGroup(spaceGroup, false);
    }
    if (sg == null)
      return "could not identify space group from name: " + spaceGroup;
    return sg.dumpInfo() + strOperations;
  }


  public Point3f[] getPolymerLeadMidPoints(int iModel, int iPolymer) {
    return models[iModel].getBioPolymer(iPolymer).getLeadMidpoints();
  }

  public Hashtable getBoundBoxInfo() {
    Hashtable info = new Hashtable();
    info.put("center", getBoundBoxCenter());
    info.put("edge", getBoundBoxCornerVector());
    return info;
  }
  
  public boolean checkObjectClicked(int x, int y, int modifiers) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectClicked(x, y, modifiers))
      return true;
    return ((shape = shapes[JmolConstants.SHAPE_DRAW]) != null
        && shape.checkObjectClicked(x, y, modifiers));
  }
 
  public void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null
          && shape.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers))
        break;
    }
  }

  public Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    StringBuffer commands = new StringBuffer();
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null) {
        String shapeType = JmolConstants.shapeClassBases[i];
        if ("Draw,Dipoles,Isosurface,LcaoOrbital,MolecularOrbital".indexOf(shapeType) >= 0) {
          Hashtable shapeinfo = new Hashtable();
          shapeinfo.put("obj", shape.getShapeDetail());
          info.put(shapeType, shapeinfo);
        }
      }
    }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }
  
  public boolean hbondsAreVisible(int modelIndex) {
    for (int i = bondCount; --i >= 0;)
      if (modelIndex < 0 || modelIndex == bonds[i].atom1.modelIndex)
        if (bonds[i].isHydrogen() && bonds[i].mad > 0)
          return true;
    return false;
  }

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    Point3f xyz = (xyzValues instanceof Point3f ? (Point3f) xyzValues : null);
    String[] values = (xyzValues instanceof String[] ? (String[]) xyzValues : null);
    int nValues = (values == null ? Integer.MAX_VALUE : values.length);
    if (xyz == null && values == null || nValues == 0)
      return;
    int n = 0;
    int atomCount = getAtomCount();
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i) && n < nValues) {
        if (values != null) {
          Object o = Escape.unescapePoint(values[n++]);
          if (!(o instanceof Point3f))
            break;
          xyz = (Point3f) o;
        }
        switch (tokType) {
        case Token.xyz:
          setAtomCoord(i, xyz.x, xyz.y, xyz.z);
          break;
        case Token.fracXyz:
          setAtomCoordFractional(i, xyz);
          break;
        case Token.vibXyz:
          setAtomVibrationVector(i, xyz.x, xyz.y, xyz.z);
          break;
        }
      }
  }


}

