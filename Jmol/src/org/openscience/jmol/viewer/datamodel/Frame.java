/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
import org.jmol.g3d.Graphics3D;
import org.openscience.jmol.viewer.*;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.awt.Rectangle;

final public class Frame {

  JmolViewer viewer;
  FrameRenderer frameRenderer;
  // NOTE: these strings are interned and are lower case
  // therefore, we can do == comparisions against string constants
  // if (modelTypeName == "xyz")
  String modelTypeName;
  Mmset mmset;
  Graphics3D g3d;
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  float maxBondingRadius = Integer.MIN_VALUE;
  float maxVanderwaalsRadius = Integer.MIN_VALUE;

  int atomCount;
  Atom[] atoms;
  Object[] clientAtomReferences;
  int bondCount;
  Bond[] bonds;
  private final static int growthIncrement = 250;
  boolean fileCoordinatesAreFractional;
  float[] notionalUnitcell;
  Matrix3f matrixNotional;
  Matrix3f pdbScaleMatrix;
  Matrix3f pdbScaleMatrixTranspose;
  Vector3f pdbTranslateVector;
  Matrix3f matrixEuclideanToFractional;
  Matrix3f matrixFractionalToEuclidean;

  boolean hasVibrationVectors;

  BitSet elementsPresent;
  BitSet groupsPresent;

  public Frame(JmolViewer viewer, String modelTypeName) {
    this.viewer = viewer;
    // NOTE: these strings are interned and are lower case
    // therefore, we can do == comparisions against string constants
    // if (modelTypeName == "xyz") { }
    this.modelTypeName = modelTypeName.toLowerCase().intern();
    mmset = new Mmset(this);
    this.frameRenderer = viewer.getFrameRenderer();
    this.g3d = viewer.g3d;

  }

  FrameExportModelAdapter exportModelAdapter;

  public ModelAdapter getExportModelAdapter() {
    if (exportModelAdapter == null)
      exportModelAdapter = new FrameExportModelAdapter(viewer, this);
    return exportModelAdapter;
  }

  void freeze() {
    ////////////////////////////////////////////////////////////////
    // convert fractional coordinates to cartesian
    if (notionalUnitcell != null)
      doUnitcellStuff();

    ////////////////////////////////////////////////////////////////
    // perform bonding if necessary
    if (viewer.getAutoBond()) {
      if ((bondCount == 0) ||
          (modelTypeName == "pdb" && (bondCount < (atomCount / 2))))
        rebond(false);
    }

    ////////////////////////////////////////////////////////////////
    // resize arrays
    if (atomCount < atoms.length)
      atoms = (Atom[])Util.setLength(atoms, atomCount);
    if (clientAtomReferences != null &&
        atomCount < clientAtomReferences.length)
      clientAtomReferences =
        (Object[])Util.setLength(clientAtomReferences, atomCount);
    if (bondCount < bonds.length)
      bonds = (Bond[])Util.setLength(bonds, bondCount);

    ////////////////////////////////////////////////////////////////
    // see if there are any vectors
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].vibrationVector != null) {
        hasVibrationVectors = true;
      }

    ////////////////////////////////////////////////////////////////
    //
    hackAtomSerialNumbersForAnimations();

    ////////////////////////////////////////////////////////////////
    // find things for the popup menus
    findElementsPresent();
    findGroupsPresent();
    mmset.freeze();

    checkShape(JmolConstants.SHAPE_BALLS);
    checkShape(JmolConstants.SHAPE_STICKS);
  }

  void hackAtomSerialNumbersForAnimations() {
    // first, validate that all atomSerials are NaN
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].atomSerial != Integer.MIN_VALUE)
        return;
    // now, we'll assign 1-based atom numbers within each model
    int lastModelIndex = Integer.MAX_VALUE;
    int modelAtomIndex = 0;
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atoms[i];
      if (atom.modelIndex != lastModelIndex) {
        lastModelIndex = atom.modelIndex;
        modelAtomIndex = 1;
      }
      atom.atomSerial = modelAtomIndex++;
    }
  }

  public int getAtomIndexFromAtomNumber(int atomNumber) {
    for (int i = atomCount; --i >= 0; ) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
      }
    return -1;
  }

  public int getModelCount() {
    return mmset.getModelCount();
  }

  public int getModelIndex(int modelID) {
    return mmset.getModelIndex(modelID);
  }

  public int getChainCount() {
    return mmset.getChainCount();
  }

  public int getGroupCount() {
    return mmset.getGroupCount();
  }

  public int getAtomCount() {
    return atomCount;
  }

  Atom[] getAtoms() {
    return atoms;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public Point3f getAtomPoint3f(int atomIndex) {
    return atoms[atomIndex].point3f;
  }

  public int getBondCount() {
    return bondCount;
  }

  public Bond getBondAt(int bondIndex) {
    return bonds[bondIndex];
  }

  private void addBond(Bond bond) {
    if (bond == null)
      return;
    if (bondCount == bonds.length)
      bonds = (Bond[])Util.setLength(bonds, bondCount + growthIncrement);
    bonds[bondCount++] = bond;
  }

  void bondAtoms(Atom atom1, Atom atom2,
                             int order) {
    addBond(atom1.bondMutually(atom2, order));
  }

  public boolean hasVibrationVectors() {
    return hasVibrationVectors;
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

  void clearBounds() {
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
  }

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = null;

  /****************************************************************
   * selection handling
   ****************************************************************/
  public boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    if (frankShape == null)
      return false;
    return frankShape.wasClicked(x, y);
  }

  final static int minimumPixelSelectionRadius = 4;

  final Closest closest = new Closest();

  public int findNearestAtomIndex(int x, int y) {
    closest.atom = null;
    for (int i = 0; i < shapes.length; ++i) {
      Shape shape = shapes[i];
      if (shape != null) {
        shapes[i].findNearestAtomIndex(x, y, closest);
        if (closest.atom != null)
          break;
      }
    }
    int closestIndex = (closest.atom == null ? -1 : closest.atom.atomIndex);
    closest.atom = null;
    return closestIndex;
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();
  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (rect.contains(atom.getScreenX(), atom.getScreenY()))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  public BondIterator getBondIterator(short bondType, BitSet bsSelected) {
    return new SelectedBondIterator(bondType, bsSelected);
  }

  class SelectedBondIterator implements BondIterator {

    short bondType;
    int iBond;
    BitSet bsSelected;
    boolean bondSelectionModeOr;

    SelectedBondIterator(short bondType, BitSet bsSelected) {
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
          bspf.addTuple(atom.modelIndex, atom);
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

  void rebond(boolean deleteFirst) {
    if (deleteFirst)
      deleteAllBonds();
    if (maxBondingRadius == Integer.MIN_VALUE)
      findMaxRadii();
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
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
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

  void calcHbonds() {
    if (hbondsCalculated)
      return;
    hbondsCalculated = true;
    if (useRasMolHbondsCalculation) {
      if (mmset != null)
        mmset.calcHydrogenBonds();
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
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
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
        addBond(atom.bondMutually(atomNear, JmolConstants.BOND_H_REGULAR));
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


  void deleteAllBonds() {
    for (int i = bondCount; --i >= 0; ) {
      bonds[i].deleteAtomReferences();
      bonds[i] = null;
    }
    bondCount = 0;
  }

  void deleteBond(Bond bond) {
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

  void deleteCovalentBonds() {
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

  void deleteAtom(int atomIndex) {
    clearBspf();
    atoms[atomIndex].markDeleted();
  }

  float getMaxVanderwaalsRadius() {
    if (maxVanderwaalsRadius == Integer.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  void setNotionalUnitcell(float[] notionalUnitcell) {
    if (notionalUnitcell != null && notionalUnitcell.length != 6)
      System.out.println("notionalUnitcell length incorrect:" +
                         notionalUnitcell);
    else
      this.notionalUnitcell = notionalUnitcell;
  }

  void setPdbScaleMatrix(float[] pdbScaleMatrixArray) {
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

  void setPdbScaleTranslate(float[] pdbScaleTranslate) {
    if (pdbScaleTranslate == null)
      return;
    if (pdbScaleTranslate.length != 3) {
      System.out.println("pdbScaleTranslate.length != 3 :" + 
                         pdbScaleTranslate);
      return;
    }
    this.pdbTranslateVector = new Vector3f(pdbScaleTranslate);
  }

  // FIXME mth 2004 05 04 - do NOT pass a null in here
  // figure out what to do about the g3d when allocating a shape renderer
  ShapeRenderer getRenderer(int shapeType) {
    return frameRenderer.getRenderer(shapeType, null);
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
    /*
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

  void findElementsPresent() {
    elementsPresent = new BitSet();
    for (int i = atomCount; --i >= 0; )
      elementsPresent.set(atoms[i].elementNumber);
  }

  public BitSet getElementsPresentBitSet() {
    return elementsPresent;
  }

  void findGroupsPresent() {
    Group groupLast = null;
    groupsPresent = new BitSet();
    for (int i = atomCount; --i >= 0; ) {
      if (groupLast != atoms[i].group) {
        groupLast = atoms[i].group;
        groupsPresent.set(groupLast.getGroupID());
      }
    }
  }

  void findMaxRadii() {
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat();
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  public BitSet getGroupsPresentBitSet() {
    return groupsPresent;
  }
}
