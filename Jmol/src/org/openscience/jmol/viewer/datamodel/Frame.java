/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.viewer.datamodel;

import org.jmol.api.ModelAdapter;
import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.pdb.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

final public class Frame {

  public JmolViewer viewer;
  public FrameRenderer frameRenderer;
  Graphics3D g3d;
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  float maxBondingRadius = 0;
  float maxVanderwaalsRadius = 0;
  // whether or not this frame has any protein properties
  int modelType;
  boolean hasPdbRecords;
  PdbFile pdbFile;

  final static int growthIncrement = 128;
  public int modelCount;
  int lastModelNumber = -1;
  public short modelIDs[];
  int atomCount = 0;
  public Atom[] atoms;
  Object[] clientAtomReferences;
  int bondCount = 0;
  public Bond[] bonds;
  boolean fileCoordinatesAreFractional;
  public float[] notionalUnitcell;
  public Matrix3f matrixNotional;
  public Matrix3f pdbScaleMatrix;
  public Matrix3f pdbScaleMatrixTranspose;
  public Vector3f pdbTranslateVector;
  public Matrix3f matrixEuclideanToFractional;
  public Matrix3f matrixFractionalToEuclidean;

  public Frame(JmolViewer viewer, int atomCount,
                   int modelType, boolean hasPdbRecords) {
    this.viewer = viewer;
    this.modelType = modelType;
    this.hasPdbRecords = hasPdbRecords;
    if (hasPdbRecords)
      pdbFile = new PdbFile(this);
    modelIDs = new short[10];
    atoms = new Atom[atomCount];
    bonds = new Bond[atomCount * 2];
    this.frameRenderer = viewer.getFrameRenderer();
    this.g3d = viewer.g3d;

    checkShape(JmolConstants.SHAPE_BALLS);
    checkShape(JmolConstants.SHAPE_STICKS);
  }

  public Frame(JmolViewer viewer, int modelType, boolean hasPdbRecords) {
    this(viewer, growthIncrement, modelType, hasPdbRecords);
  }

  public Frame(JmolViewer viewer) {
    this(viewer, JmolConstants.MODEL_TYPE_OTHER, false);
  }

  FrameExportModelAdapter exportModelAdapter;

  public ModelAdapter getExportModelAdapter() {
    if (exportModelAdapter == null)
      exportModelAdapter = new FrameExportModelAdapter(viewer, this);
    return exportModelAdapter;
  }

  public void freeze() {
    htAtomMap = null;
    if (notionalUnitcell != null)
      doUnitcellStuff();
    if (viewer.getAutoBond()) {
      if ((bondCount == 0) ||
          (hasPdbRecords && (bondCount < (atomCount / 2))))
        rebond(false);
    }
    if (hasPdbRecords)
      pdbFile.freeze();
  }

  public Atom addAtom(int modelNumber, Object atomUid,
                      byte atomicNumber,
                      String atomName, 
                      int atomicCharge,
                      int occupancy,
                      float bfactor,
                      float x, float y, float z,
                      boolean isHetero, int atomSerial, char chainID,
                      String group3, int sequenceNumber, char insertionCode,
                      Object clientAtomReference) {
    if (modelNumber != lastModelNumber) {
      if (modelCount == modelIDs.length) {
      short[] newModelIDs = new short[atoms.length + 20];
      System.arraycopy(modelIDs, 0, newModelIDs, 0, modelIDs.length);
      modelIDs = newModelIDs;
      }
      lastModelNumber = modelIDs[modelCount++] = (short)modelNumber;
    }
    if (atomCount == atoms.length) {
      Atom[] newAtoms =
        new Atom[atoms.length + growthIncrement];
      System.arraycopy(atoms, 0, newAtoms, 0, atoms.length);
      atoms = newAtoms;
    }
    Atom atom = new Atom(this, atomCount,
                         modelNumber, 
                         atomicNumber,
                         atomName,
                         atomicCharge,
                         occupancy,
                         bfactor,
                         x, y, z,
                         isHetero, atomSerial, chainID,
                         group3, sequenceNumber, insertionCode,
                         pdbFile);
    atoms[atomCount] = atom;
    if (clientAtomReference != null) {
      if (clientAtomReferences == null)
        clientAtomReferences = new Object[atoms.length];
      else if (clientAtomReferences.length <= atomCount) {
        Object[] t = new Object[atoms.length];
        System.arraycopy(clientAtomReferences, 0, t, 0,
                         clientAtomReferences.length);
        clientAtomReferences = t;
      }
      clientAtomReferences[atomCount] = clientAtomReference;
    }
    ++atomCount;
    htAtomMap.put(atomUid, atom);
    if (bspf != null)
      bspf.addTuple(atom.getModelNumber(), atom);
    float bondingRadius = atom.getBondingRadiusFloat();
    if (bondingRadius > maxBondingRadius)
      maxBondingRadius = bondingRadius;
    float vdwRadius = atom.getVanderwaalsRadiusFloat();
    if (vdwRadius > maxVanderwaalsRadius)
      maxVanderwaalsRadius = vdwRadius;
    return atom;
  }

  public int getAtomIndexFromAtomNumber(int atomNumber) {
    for (int i = atomCount; --i >= 0; ) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
      }
    return -1;
  }

  public int getModelCount() {
    return modelCount;
  }

  public int getChainCount() {
    return (hasPdbRecords ? pdbFile.getChainCount() : 0);
  }

  public int getGroupCount() {
    return (hasPdbRecords ? pdbFile.getGroupCount() : 0);
  }

  public int getAtomCount() {
    return atomCount;
  }

  public Atom[] getAtoms() {
    return atoms;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public Point3f getAtomPoint3f(int atomIndex) {
    return atoms[atomIndex].point3f;
  }

  public PdbAtom getPdbAtom(int atomIndex) {
    return atoms[atomIndex].pdbAtom;
  }

  public int getBondCount() {
    return bondCount;
  }

  public Bond getBondAt(int bondIndex) {
    return bonds[bondIndex];
  }

  public boolean hasPdbRecords() {
    return hasPdbRecords;
  }

  private Hashtable htAtomMap = new Hashtable();

  public void addHydrogenBond(Atom atom1, Atom atom2) {
    addBond(atom1.bondMutually(atom2, JmolConstants.BOND_HYDROGEN));
  }

  private void addBond(Bond bond) {
    if (bond == null)
      return;
    if (bondCount == bonds.length) {
      Bond[] newBonds =
        new Bond[bonds.length + growthIncrement];
      System.arraycopy(bonds, 0, newBonds, 0, bonds.length);
      bonds = newBonds;
    }
    bonds[bondCount++] = bond;
  }

  public void bondAtoms(Object atomUid1, Object atomUid2,
                             int order) {
    Atom atom1 = (Atom)htAtomMap.get(atomUid1);
    if (atom1 == null) {
      System.out.println("Frame.bondAtoms cannot find atomUid1?");
      return;
    }
    Atom atom2 = (Atom)htAtomMap.get(atomUid2);
    if (atom2 == null) {
      System.out.println("Frame.bondAtoms cannot find atomUid2?");
      return;
    }
    addBond(atom1.bondMutually(atom2, order));
  }

  public void bondAtoms(Atom atom1, Object clientAtom2,
                             int order) {
    Atom atom2 = (Atom)htAtomMap.get(clientAtom2);
    if (atom2 == null)
      return;
    addBond(atom1.bondMutually(atom2, order));
  }

  public void bondAtoms(Atom atom1, Atom atom2,
                             int order) {
    addBond(atom1.bondMutually(atom2, order));
  }

  Shape allocateShape(int shapeType) {
    String classBase = JmolConstants.shapeClassBases[shapeType];
    String className = "org.openscience.jmol.viewer.datamodel." + classBase;

    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape)shapeClass.newInstance();
      shape.setViewerG3dFrame(viewer, g3d, this);
      return shape;
    } catch (Exception e) {
      System.out.println("Could not instantiate shape:" + classBase +
                         "\n" + e);
      e.printStackTrace();
    }
    return null;
  }

  final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];

  void checkShape(int shapeType) {
    if (shapes[shapeType] == null) {
      shapes[shapeType] = allocateShape(shapeType);
    }
  }
  
  public void setShapeSize(int shapeType, int size, BitSet bsSelected) {
    if (size != 0)
      checkShape(shapeType);
    if (shapes[shapeType] != null)
      shapes[shapeType].setSize(size, bsSelected);
  }

  public void setShapeProperty(int shapeType, String propertyName,
                               Object value, BitSet bsSelected) {
    /*
    System.out.println("Frame.setShapeProperty(" +
                       JmolConstants.shapeClassBases[shapeType] +
                       "," + propertyName + "," + value + ")");
    */
    if (shapes[shapeType] != null)
      shapes[shapeType].setProperty(propertyName, value, bsSelected);
  }

  public Object getShapeProperty(int shapeType,
                                 String propertyName, int index) {
    return (shapes[shapeType] == null
            ? null : shapes[shapeType].getProperty(propertyName, index));
  }

  Point3f averageAtomPoint;

  Point3f centerBoundingBox;
  Vector3f boundingBoxCornerVector;
  Point3f minBoundingBox;
  Point3f maxBoundingBox;

  Point3f centerUnitcell;

  //  float radiusBoundingBox;
  Point3f rotationCenter;
  float rotationRadius;
  Point3f rotationCenterDefault;
  float rotationRadiusDefault;

  public Point3f getBoundingBoxCenter() {
    findBounds();
    return centerBoundingBox;
  }

  public Vector3f getBoundingBoxCornerVector() {
    findBounds();
    return boundingBoxCornerVector;
  }

  public Point3f getRotationCenter() {
    findBounds();
    return rotationCenter;
  }

  public void increaseRotationRadius(float increaseInAngstroms) {
    rotationRadius += increaseInAngstroms;
  }

  public float getRotationRadius() {
    findBounds();
    return rotationRadius;
  }

  public void setRotationCenter(Point3f newCenterOfRotation) {
    if (newCenterOfRotation != null) {
      rotationCenter = newCenterOfRotation;
      rotationRadius = calcRotationRadius(rotationCenter);
    } else {
      rotationCenter = rotationCenterDefault;
      rotationRadius = rotationRadiusDefault;
    }
  }

  public void clearBounds() {
    rotationCenter = null;
    rotationRadius = 0;
  }

  private void findBounds() {
    if ((rotationCenter != null) || (atomCount <= 0))
      return;
    calcRotationSphere();
  }

  private void calcRotationSphere() {
    calcAverageAtomPoint();
    calcBoundingBoxDimensions();
    if (notionalUnitcell != null)
      calcUnitcellDimensions();
    rotationCenter = rotationCenterDefault =
      averageAtomPoint;
    rotationRadius = rotationRadiusDefault =
      calcRotationRadius(rotationCenterDefault);
  }

  private void calcAverageAtomPoint() {
    Point3f average = this.averageAtomPoint = new Point3f();
    for (int i = atomCount; --i >= 0; )
      average.add(atoms[i].point3f);
    average.scale(1f/atomCount);
  }

  final static Point3f[] unitBboxPoints = {
    new Point3f( 1, 1, 1),
    new Point3f( 1, 1,-1),
    new Point3f( 1,-1, 1),
    new Point3f( 1,-1,-1),
    new Point3f(-1, 1, 1),
    new Point3f(-1, 1,-1),
    new Point3f(-1,-1, 1),
    new Point3f(-1,-1,-1),
  };

  final Point3f[] bboxVertices = new Point3f[8];

  private void calcBoundingBoxDimensions() {
    minBoundingBox = new Point3f();
    maxBoundingBox = new Point3f();

    calcAtomsMinMax(minBoundingBox, maxBoundingBox);

    centerBoundingBox = new Point3f(minBoundingBox);
    centerBoundingBox.add(maxBoundingBox);
    centerBoundingBox.scale(0.5f);
    boundingBoxCornerVector = new Vector3f(maxBoundingBox);
    boundingBoxCornerVector.sub(centerBoundingBox);

    for (int i = 8; --i >= 0; ) {
      Point3f bbcagePoint = bboxVertices[i] = new Point3f(unitBboxPoints[i]);
      bbcagePoint.x *= boundingBoxCornerVector.x;
      bbcagePoint.y *= boundingBoxCornerVector.y;
      bbcagePoint.z *= boundingBoxCornerVector.z;
      bbcagePoint.add(centerBoundingBox);
    }
  }

  final static Point3f[] unitCubePoints = {
    new Point3f( 0, 0, 0),
    new Point3f( 0, 0, 1),
    new Point3f( 0, 1, 0),
    new Point3f( 0, 1, 1),
    new Point3f( 1, 0, 0),
    new Point3f( 1, 0, 1),
    new Point3f( 1, 1, 0),
    new Point3f( 1, 1, 1),
  };

  Point3f[] unitcellVertices;

  private void calcUnitcellDimensions() {
    unitcellVertices = new Point3f[8];
    for (int i = 8; --i >= 0; ) {
      Point3f vertex = unitcellVertices[i] = new Point3f();
      matrixFractionalToEuclidean.transform(unitCubePoints[i], vertex);
    }
    centerUnitcell = new Point3f(unitcellVertices[7]);
    centerUnitcell.scale(0.5f);
  }

  private float calcRotationRadius(Point3f center) {

    float maxRadius = 0;
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom.point3f);
      float radiusVdw = atom.getVanderwaalsRadiusFloat();
      float outerVdw = distAtom + radiusVdw;
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    
    return maxRadius;

    /*
    // check the 8 corners of the bounding box
    float maxRadius2 = center.distanceSquared(bboxVertices[7]);
    for (int i = 7; --i >= 0; ) {
      float radius2 = center.distanceSquared(bboxVertices[i]);
      if (radius2 > maxRadius2)
        maxRadius2 = radius2;
    }
    if (unitcellVertices != null) {
      for (int i = 8; --i >= 0; ) {
        float radius2 = center.distanceSquared(bboxVertices[i]);
        if (radius2 > maxRadius2)
          maxRadius2 = radius2;
      }
    }
    */
  }

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = null;

  /*
  public void addMeasurement(int count, int[] atomIndices) {
    // turn on the display of measures
    setShapeSize(JmolConstants.SHAPE_MEASURES, 1, null);

    Measurement measureNew = new Measurement(this, count, atomIndices);
    if (measurements == null ||
        measurementCount == measurements.length) {
      Measurement[] newShapes =
        new Measurement[measurementCount +
                             measurementGrowthIncrement];
      if (measurements != null)
        System.arraycopy(measurements, 0,
                         newShapes, 0, measurementCount);
      measurements = newShapes;
    }
    measurements[measurementCount++] = measureNew;

  }

  public void clearMeasurements() {
    measurementCount = 0;
    measurements = null;
  }

  public void deleteMeasurement(int imeasurement) {
    System.arraycopy(measurements, imeasurement+1,
                     measurements, imeasurement,
                     measurementCount - imeasurement - 1);
    --measurementCount;
    measurements[measurementCount] = null;
  }

  public int getMeasurementCount() {
    return measurementCount;
  }

  public Measurement[] getMeasurements() {
    return measurements;
  }
  */

  /****************************************************************
   * selection handling
   ****************************************************************/
  public boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    if (frankShape == null)
      return false;
    return frankShape.wasClicked(x, y);
  }

  final static int selectionPixelLeeway = 5;

  public int findNearestAtomIndex(int x, int y) {
    /*
     * FIXME
     * mth - this code has problems
     * 0. can select atoms which are not displayed!
     * 1. doesn't take radius of atom into account until too late
     * 2. doesn't take Z dimension into account, so it could select an atom
     *    which is behind the one the user wanted
     * 3. doesn't take into account the fact that hydrogens could be hidden
     *    you can select a region and get extra hydrogens
     */
    if (atomCount == 0)
      return -1;
    Atom atomNearest = null;
    int indexNearest = -1;
    int r2Nearest = Integer.MAX_VALUE;
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if ((atom.chargeAndFlags & Atom.VISIBLE_FLAG) == 0)
        continue;
      int dx = atom.x - x;
      int dx2 = dx * dx;
      if (dx2 > r2Nearest)
        continue;
      int dy = atom.y - y;
      int dy2 = dy * dy;
      if (dy2 + dx2 > r2Nearest)
        continue;
      atomNearest = atom; // will definitely happen the first time through
      r2Nearest = dx2 + dy2;
      indexNearest = i;
    }
    int rNearest = (int)Math.sqrt(r2Nearest);
    return (rNearest > atomNearest.diameter/2 + selectionPixelLeeway)
      ? -1
      : indexNearest;
  }
    
  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();
  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (rect.contains(atom.x, atom.y))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  public BondIterator getBondIterator(byte bondType, BitSet bsSelected) {
    return new SelectedBondIterator(bondType, bsSelected);
  }

  class SelectedBondIterator implements BondIterator {

    byte bondType;
    int iBond;
    BitSet bsSelected;
    boolean bondSelectionModeOr;

    SelectedBondIterator(byte bondType, BitSet bsSelected) {
      this.bondType = bondType;
      this.bsSelected = bsSelected;
      iBond = 0;
      bondSelectionModeOr = viewer.getBondSelectionModeOr();
    }

    public boolean hasNext() {
      for ( ; iBond < bondCount; ++iBond) {
        Bond bond = bonds[iBond];
        if ((bond.order & bondType) == 0)
          continue;
        boolean isSelected1 =
          bsSelected.get(bond.atom1.atomIndex);
        boolean isSelected2 =
          bsSelected.get(bond.atom2.atomIndex);
        if ((!bondSelectionModeOr & isSelected1 & isSelected2) ||
            (bondSelectionModeOr & (isSelected1 | isSelected2)))
          return true;
      }
      return false;
    }

    public Bond next() {
      return bonds[iBond++];
    }
  }

  private Bspf bspf;

  void initializeBspf() {
    if (bspf == null) {
      bspf = new Bspf(3);
      for (int i = atomCount; --i >= 0; ) {
        Atom atom = atoms[i];
        if (! atom.isDeleted())
          bspf.addTuple(atom.getModelNumber(), atom);
      }
    }
  }

  private void clearBspf() {
    bspf = null;
  }

  int getBsptCount() {
    if (bspf == null)
      initializeBspf();
    return bspf.getBsptCount();
  }

  private final WithinIterator withinAtomIterator = new WithinIterator();
  private final WithinIterator withinPointIterator = new WithinIterator();
  private final PointWrapper pointWrapper = new PointWrapper();

  public AtomIterator getWithinIterator(Atom atomCenter,
                                             float radius) {
    withinAtomIterator.initialize(atomCenter, radius);
    return withinAtomIterator;
  }

  public AtomIterator getWithinIterator(Point3f point, float radius) {
    pointWrapper.setPoint(point);
    withinPointIterator.initialize(pointWrapper, radius);
    return withinPointIterator;
  }

  class WithinIterator implements AtomIterator {

    Bspt.SphereIterator iterCurrent;
    Bspt.Tuple center;
    float radius;
    int bsptIndexLast;
    

    void initialize(Bspt.Tuple center, float radius) {
      this.center = center;
      this.radius = radius;
      iterCurrent = null;
      bsptIndexLast = getBsptCount();
    }

    public boolean hasNext() {
      while (true) {
        if (iterCurrent != null && iterCurrent.hasMoreElements())
          return true;
        if (bsptIndexLast == 0)
          return false;
        iterCurrent = bspf.getSphereIterator(--bsptIndexLast);
        if (iterCurrent != null)
          iterCurrent.initialize(center, radius);
      }
    }

    public Atom next() {
      return (Atom)iterCurrent.nextElement();
    }

    public void release() {
      iterCurrent = null;
      for (int i = getBsptCount(); --i >= 0; ) {
        Bspt.SphereIterator iter = bspf.getSphereIterator(i);
        if (iter != null)
          iter.release();
      }
    }
  }

  class PointWrapper implements Bspt.Tuple {
    Point3f point;
    
    void setPoint(Point3f point) {
      this.point.set(point);
    }
    
    public float getDimensionValue(int dim) {
      return (dim == 0
	      ? point.x
	      : (dim == 1 ? point.y : point.z));
    }
  }

  final static boolean showRebondTimes = true;

  private float bondTolerance;
  private float minBondDistance;
  private float minBondDistance2;

  public void rebond() {
    rebond(true);
  }

  public void rebond(boolean deleteFirst) {
    if (deleteFirst)
      deleteAllBonds();
    bondTolerance = viewer.getBondTolerance();
    minBondDistance = viewer.getMinBondDistance();
    minBondDistance2 = minBondDistance*minBondDistance;

    char chainLast = '?';
    int indexLastCA = -1;
    Atom atomLastCA = null;

    initializeBspf();

    long timeBegin, timeEnd;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      float searchRadius =
        myBondingRadius + maxBondingRadius + bondTolerance;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelNumber());
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        if (atomNear != atom) {
          int order = getBondOrder(atom, myBondingRadius,
                                   atomNear, atomNear.getBondingRadiusFloat(),
                                   iter.foundDistance2());
          if (order > 0)
            addBond(atom.bondMutually(atomNear, order));
        }
      }
      iter.release();
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to autoBond=" + (timeEnd - timeBegin));
    }
  }

  private int getBondOrder(Atom atomA, float bondingRadiusA,
                           Atom atomB, float bondingRadiusB,
                           float distance2) {
    //            System.out.println(" radiusA=" + bondingRadiusA +
    //                               " radiusB=" + bondingRadiusB +
    //                         " distance2=" + distance2 +
    //                         " tolerance=" + bondTolerance);
    float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
    float maxAcceptable2 = maxAcceptable * maxAcceptable;
    if (distance2 < minBondDistance2) {
      //System.out.println("less than minBondDistance");
      return 0;
    }
    if (distance2 <= maxAcceptable2) {
      //System.out.println("returning 1");
      return 1;
    }
    return 0;
  }

  float hbondMax = 3.25f;
  float hbondMin = 2.5f;
  float hbondMin2 = hbondMin * hbondMin;
  
  boolean hbondsCalculated;

  boolean useRasMolHbondsCalculation = true;

  public void calcHbonds() {
    if (hbondsCalculated)
      return;
    hbondsCalculated = true;
    if (useRasMolHbondsCalculation) {
      if (pdbFile != null)
        pdbFile.calcHydrogenBonds();
      return;
    }
    initializeBspf();
    long timeBegin, timeEnd;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      int elementNumber = atom.elementNumber;
      if (elementNumber != 7 && elementNumber != 8)
        continue;
      float searchRadius = hbondMax;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelNumber());
      iter.initializeHemisphere(atom, hbondMax);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        int elementNumberNear = atomNear.elementNumber;
        if (elementNumberNear != 7 && elementNumberNear != 8)
          continue;
        if (atomNear == atom)
          continue;
        if (iter.foundDistance2() < hbondMin2)
          continue;
        if (atom.isBonded(atomNear))
          continue;
        addBond(atom.bondMutually(atomNear, JmolConstants.BOND_HYDROGEN));
        System.out.println("adding an hbond between " + atom.atomIndex +
                           " & " + atomNear.atomIndex);
      }
      iter.release();
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to hbond=" + (timeEnd - timeBegin));
    }
  }


  public void deleteAllBonds() {
    for (int i = bondCount; --i >= 0; ) {
      bonds[i].deleteAtomReferences();
      bonds[i] = null;
    }
    bondCount = 0;
  }

  public void deleteBond(Bond bond) {
    // what a disaster ... I hate doing this
    for (int i = bondCount; --i >= 0; ) {
      if (bonds[i] == bond) {
        bonds[i].deleteAtomReferences();
        System.arraycopy(bonds, i+1, bonds, i,
                         bondCount - i - 1);
        --bondCount;
        bonds[bondCount] = null;
      }
    }
  }

  public void deleteCovalentBonds() {
    int indexNoncovalent = 0;
    for (int i = 0; i < bondCount; ++i) {
      Bond bond = bonds[i];
      if (! bond.isCovalent()) {
        if (i != indexNoncovalent) {
          bonds[indexNoncovalent++] = bond;
          bonds[i] = null;
        }
      } else {
        bond.deleteAtomReferences();
        bonds[i] = null;
      }
    }
    bondCount = indexNoncovalent;
  }

  public void deleteAtom(int atomIndex) {
    clearBspf();
    atoms[atomIndex].markDeleted();
  }

  public float getMaxVanderwaalsRadius() {
    return maxVanderwaalsRadius;
  }

  public void setNotionalUnitcell(float[] notionalUnitcell) {
    if (notionalUnitcell != null && notionalUnitcell.length != 6)
      System.out.println("notionalUnitcell length incorrect:" +
                         notionalUnitcell);
    else
      this.notionalUnitcell = notionalUnitcell;
  }

  public void setPdbScaleMatrix(float[] pdbScaleMatrixArray) {
    if (pdbScaleMatrixArray == null)
      return;
    if (pdbScaleMatrixArray.length != 9) {
      System.out.println("pdbScaleMatrix.length != 9 :" + 
                        pdbScaleMatrix);
      return;
    }
    pdbScaleMatrix = new Matrix3f(pdbScaleMatrixArray);
    pdbScaleMatrixTranspose = new Matrix3f();
    pdbScaleMatrixTranspose.transpose(pdbScaleMatrix);
  }

  public void setPdbScaleTranslate(float[] pdbScaleTranslate) {
    if (pdbScaleTranslate == null)
      return;
    if (pdbScaleTranslate.length != 3) {
      System.out.println("pdbScaleTranslate.length != 3 :" + 
                         pdbScaleTranslate);
      return;
    }
    this.pdbTranslateVector = new Vector3f(pdbScaleTranslate);
  }

  public ShapeRenderer getRenderer(int shapeType) {
    return frameRenderer.getRenderer(shapeType);
  }

  void doUnitcellStuff() {
    constructFractionalMatrices();
    if (fileCoordinatesAreFractional)
      convertFractionalToEuclidean();
    /*
      mth 2004 03 06
      We do not want to pack the unitcell automatically.
    putAtomsInsideUnitcell();
    */
  }

  void constructFractionalMatrices() {
    matrixNotional = new Matrix3f();
    calcNotionalMatrix(notionalUnitcell, matrixNotional);
    if (pdbScaleMatrix != null) {
      //      System.out.println("using PDB Scale matrix");
      matrixEuclideanToFractional = new Matrix3f();
      matrixEuclideanToFractional.transpose(pdbScaleMatrix);
      matrixFractionalToEuclidean = new Matrix3f();
      matrixFractionalToEuclidean.invert(matrixEuclideanToFractional);
    } else {
      //      System.out.println("using notional unit cell");
      matrixFractionalToEuclidean = matrixNotional;
      matrixEuclideanToFractional = new Matrix3f();
      matrixEuclideanToFractional.invert(matrixFractionalToEuclidean);
    }
    /*
    System.out.println("matrixNotional\n" +
                       matrixNotional +
                       "matrixFractionalToEuclidean\n" +
                       matrixFractionalToEuclidean + "\n" +
                       "matrixEuclideanToFractional\n" +
                       matrixEuclideanToFractional);
    */
  }

  final static float toRadians = (float)Math.PI * 2 / 360;

  void calcNotionalMatrix(float[] notionalUnitcell,
                          Matrix3f matrixNotional) {
    // note that these are oriented as columns, not as row
    // this is because we will later use the transform method,
    // which multiplies the matrix by the point, not the point by the matrix
    float gamma = notionalUnitcell[5];
    float beta  = notionalUnitcell[4];
    float alpha = notionalUnitcell[3];
    float c = notionalUnitcell[2];
    float b = notionalUnitcell[1];
    float a = notionalUnitcell[0];
    
    /* some intermediate variables */
    float cosAlpha = (float)Math.cos(toRadians * alpha);
    float sinAlpha = (float)Math.sin(toRadians * alpha);
    float cosBeta  = (float)Math.cos(toRadians * beta);
    float sinBeta  = (float)Math.sin(toRadians * beta);
    float cosGamma = (float)Math.cos(toRadians * gamma);
    float sinGamma = (float)Math.sin(toRadians * gamma);
    
    
    // 1. align the a axis with x axis
    matrixNotional.setColumn(0, a, 0, 0);
    // 2. place the b is in xy plane making a angle gamma with a
    matrixNotional.setColumn(1, b * cosGamma, b * sinGamma, 0);
    // 3. now the c axis,
    // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
    float V = a * b * c *
      (float) Math.sqrt(1.0 - cosAlpha*cosAlpha -
                        cosBeta*cosBeta -
                        cosGamma*cosGamma +
                        2.0*cosAlpha*cosBeta*cosGamma);
    matrixNotional.
      setColumn(2, c * cosBeta,
                c * (cosAlpha - cosBeta*cosGamma)/sinGamma,
                V/(a * b * sinGamma));
  }

  void putAtomsInsideUnitcell() {
    /****************************************************************
     * find connected-sets ... aka 'molecules'
     * convert to fractional coordinates
     * for each connected-set
     *   find its center
     *   if the center is outside the unitcell
     *     move the atoms
     * convert back to euclidean coordinates
     ****************************************************************/
    convertEuclideanToFractional();
    // but for now, just do one connected-set
    Point3f adjustment = findFractionalAdjustment();
    if (adjustment.x != 0 || adjustment.y != 0 || adjustment.z != 0)
      applyFractionalAdjustment(adjustment);
    convertFractionalToEuclidean();
  }

  void convertEuclideanToFractional() {
    for (int i = atomCount; --i >= 0; )
      matrixEuclideanToFractional.transform(atoms[i].point3f);
  }

  void convertFractionalToEuclidean() {
    for (int i = atomCount; --i >= 0; )
      matrixFractionalToEuclidean.transform(atoms[i].point3f);
  }

  Point3f findFractionalAdjustment() {
    Point3f pointMin = new Point3f();
    Point3f pointMax = new Point3f();
    calcAtomsMinMax(pointMin, pointMax);
    pointMin.add(pointMax);
    pointMin.scale(0.5f);

    Point3f fractionalCenter = pointMin;
    System.out.println("fractionalCenter=" + fractionalCenter);
    Point3f adjustment = pointMax;
    adjustment.set((float)Math.floor(fractionalCenter.x),
                   (float)Math.floor(fractionalCenter.y),
                   (float)Math.floor(fractionalCenter.z));
    return adjustment;
  }

  void applyFractionalAdjustment(Point3f adjustment) {
    System.out.println("applyFractionalAdjustment(" + adjustment + ")");
    for (int i = atomCount; --i >= 0; )
      atoms[i].point3f.sub(adjustment);
  }

  void calcAtomsMinMax(Point3f pointMin, Point3f pointMax) {
    float minX, minY, minZ, maxX, maxY, maxZ;
    Point3f pointT;
    pointT = atoms[0].point3f;
    minX = maxX = pointT.x;
    minY = maxY = pointT.y;
    minZ = maxZ = pointT.z;
    
    for (int i = atomCount; --i > 0; ) {
      // note that the 0 element was set above
      pointT = atoms[i].point3f;
      float t;
      t = pointT.x;
      if (t < minX) { minX = t; }
      else if (t > maxX) { maxX = t; }
      t = pointT.y;
      if (t < minY) { minY = t; }
      else if (t > maxY) { maxY = t; }
      t = pointT.z;
      if (t < minZ) { minZ = t; }
      else if (t > maxZ) { maxZ = t; }
    }
    pointMin.set(minX, minY, minZ);
    pointMax.set(maxX, maxY, maxZ);
  }

  void setLabel(String label, int atomIndex) {
  }
}
