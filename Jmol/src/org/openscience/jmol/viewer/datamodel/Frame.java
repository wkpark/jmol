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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.pdb.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class Frame {

  public JmolViewer viewer;
  public FrameRenderer frameRenderer;
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  float maxBondingRadius = 0;
  float maxVanderwaalsRadius = 0;
  // whether or not this frame has any protein properties
  int modelType;
  boolean hasPdbRecords;
  PdbFile pdbFile;

  final static int growthIncrement = 128;
  int atomCount = 0;
  public Atom[] atoms;
  int bondCount = 0;
  public Bond[] bonds;
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
    atoms = new Atom[atomCount];
    bonds = new Bond[atomCount * 2];
    this.frameRenderer = viewer.getFrameRenderer();
  }

  public Frame(JmolViewer viewer, int modelType, boolean hasPdbRecords) {
    this(viewer, growthIncrement, modelType, hasPdbRecords);
  }

  public Frame(JmolViewer viewer) {
    this(viewer, JmolConstants.MODEL_TYPE_OTHER, false);
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

  public Atom addAtom(Object clientAtom) {
    if (atomCount == atoms.length) {
      Atom[] newAtoms =
        new Atom[atoms.length + growthIncrement];
      System.arraycopy(atoms, 0, newAtoms, 0, atoms.length);
      atoms = newAtoms;
    }
    Atom atom = new Atom(this, atomCount, pdbFile, clientAtom);
    atoms[atomCount++] = atom;
    if (htAtomMap != null)
      htAtomMap.put(clientAtom, atom);
    if (bspf != null)
      bspf.addTuple(atom.getModelID(), atom);
    float bondingRadius = atom.getBondingRadiusFloat();
    if (bondingRadius > maxBondingRadius)
      maxBondingRadius = bondingRadius;
    float vdwRadius = atom.getVanderwaalsRadiusFloat();
    if (vdwRadius > maxVanderwaalsRadius)
      maxVanderwaalsRadius = vdwRadius;
    return atom;
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

  private Hashtable htAtomMap = null;

  private void initAtomMap() {
    if (htAtomMap == null) {
      htAtomMap = new Hashtable();
      for (int i = atomCount; --i >= 0; ) {
        Atom atom = atoms[i];
        htAtomMap.put(atom.getClientAtom(), atom);
      }
    }
  }

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

  public void bondAtoms(Object clientAtom1, Object clientAtom2,
                             int order) {
    if (htAtomMap == null)
      initAtomMap();
    Atom atom1 = (Atom)htAtomMap.get(clientAtom1);
    if (atom1 == null)
      return;
    Atom atom2 = (Atom)htAtomMap.get(clientAtom2);
    if (atom2 == null)
      return;
    addBond(atom1.bondMutually(atom2, order));
  }

  public void bondAtoms(Atom atom1, Object clientAtom2,
                             int order) {
    if (htAtomMap == null)
      initAtomMap();
    Atom atom2 = (Atom)htAtomMap.get(clientAtom2);
    if (atom2 == null)
      return;
    addBond(atom1.bondMutually(atom2, order));
  }

  public void bondAtoms(Atom atom1, Atom atom2,
                             int order) {
    addBond(atom1.bondMutually(atom2, order));
  }

  Shape allocateShape(int refShape) {
    String classBase = JmolConstants.shapeClassBases[refShape];
    String className = "org.openscience.jmol.viewer.datamodel." + classBase;

    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape)shapeClass.newInstance();
      shape.setViewerFrame(viewer, this);
      return shape;
    } catch (Exception e) {
      System.out.println("Could not instantiate shape:" + classBase +
                         "\n" + e);
      e.printStackTrace();
    }
    return null;
  }

  final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];
  final short[] shapeMads = new short[JmolConstants.SHAPE_MAX];

  void checkShape(int refShape) {
    if (shapes[refShape] == null) {
      shapes[refShape] = allocateShape(refShape);
    }
  }
  
  public void setShapeMad(int refShape, short mad, BitSet bsSelected) {
    shapeMads[refShape] = mad;
    if (mad != 0)
      checkShape(refShape);
    if (shapes[refShape] != null)
      shapes[refShape].setMad(mad, bsSelected);
  }

  public short getShapeMad(int refShape) {
    return shapeMads[refShape];
  }

  public void setShapeColix(int refShape, byte palette,
                              short colix, BitSet bsSelected) {
    if (palette != JmolConstants.PALETTE_CPK || colix != 0)
      checkShape(refShape);
    if (shapes[refShape] != null)
      shapes[refShape].setColix(palette, colix, bsSelected);
  }

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
    calcBoundingBoxDimensions();
    rotationCenterDefault = centerBoundingBox;
    if (notionalUnitcell != null) {
      calcUnitcellDimensions();
      rotationCenterDefault = centerUnitcell;
    }
    rotationCenter = rotationCenterDefault;
    rotationRadius = rotationRadiusDefault =
      calcRotationRadius(rotationCenterDefault);
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
    float radius = (float)Math.sqrt(maxRadius2);
    return radius;
  }

  final static int lineGrowthIncrement = 16;
  int lineCount = 0;
  Line[] lines = null;

  public void addLineShape(Line line) {
    if (lines == null || lineCount == lines.length) {
      Line[] newLines =
        new Line[lineCount + lineGrowthIncrement];
      if (lines != null)
        System.arraycopy(lines, 0, newLines, 0, lineCount);
      lines = newLines;
    }
    lines[lineCount++] = line;
  }

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = null;

  public void addMeasurement(int count, int[] atomIndices) {
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

  /****************************************************************
   * selection handling
   ****************************************************************/
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

  public AtomIterator getAtomIterator(BitSet bsSelected) {
    return new SelectedAtomIterator(bsSelected);
  }

  class SelectedAtomIterator implements AtomIterator {

    int iAtom;
    BitSet bsSelected;

    SelectedAtomIterator(BitSet bsSelected) {
      this.bsSelected = bsSelected;
      iAtom = 0;
    }

    public boolean hasNext() {
      for ( ; iAtom < atomCount; ++iAtom)
        if (bsSelected.get(iAtom))
          return true;
      return false;
    }

    public Atom next() {
      return atoms[iAtom++];
    }

    public void release() {
    }
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
        if (atom.marAtom != JmolConstants.MAR_DELETED) // not deleted atoms
          bspf.addTuple(atom.getModelID(), atom);
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
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelID());
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
      int atomicNumber = atom.atomicNumber;
      if (atomicNumber != 7 && atomicNumber != 8)
        continue;
      float searchRadius = hbondMax;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelID());
      iter.initializeHemisphere(atom, hbondMax);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        int atomicNumberNear = atomNear.atomicNumber;
        if (atomicNumberNear != 7 && atomicNumberNear != 8)
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

  public Object deleteAtom(int atomIndex) { // returns the clientAtom
    clearBspf();
    return atoms[atomIndex].markDeleted();
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

  public ShapeRenderer getRenderer(int refShape) {
    return frameRenderer.getRenderer(refShape);
  }

  void doUnitcellStuff() {
    constructFractionalMatrices();
    putAtomsInsideUnitcell();
  }

  void constructFractionalMatrices() {
    System.out.println("constructFractionalMatrices()");
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
    float gamma = notionalUnitcell[5];
    float beta  = notionalUnitcell[4];
    float alpha = notionalUnitcell[3];
    float c = notionalUnitcell[2];
    float b = notionalUnitcell[1];
    float a = notionalUnitcell[0];
    
    /* some intermediate variablesn */
    float cosAlpha = (float)Math.cos(toRadians * alpha);
    float sinAlpha = (float)Math.sin(toRadians * alpha);
    float cosBeta  = (float)Math.cos(toRadians * beta);
    float sinBeta  = (float)Math.sin(toRadians * beta);
    float cosGamma = (float)Math.cos(toRadians * gamma);
    float sinGamma = (float)Math.sin(toRadians * gamma);
    
    
    // 1. align the a axis with x axis
    matrixNotional.setRow(0, a, 0, 0);
    // 2. place the b is in xy plane making a angle gamma with a
    matrixNotional.setRow(1, b * cosGamma, b * sinGamma, 0);
    // 3. now the c axis,
    // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
    float V = a * b * c *
      (float) Math.sqrt(1.0 - cosAlpha*cosAlpha -
                        cosBeta*cosBeta -
                        cosGamma*cosGamma +
                        2.0*cosAlpha*cosBeta*cosGamma);
    matrixNotional.
      setRow(2, c * cosBeta,
             c * (cosAlpha - cosBeta*cosGamma)/sinGamma, V/(a * b * sinGamma));
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
}
