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
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class Frame {

  public JmolViewer viewer;
  public FrameRenderer frameRenderer;
  // the maximum CovalentRadius seen in this set of atoms
  // used in autobonding
  float maxCovalentRadius = 0;
  float maxVanderwaalsRadius = 0;
  // whether or not this frame has any protein properties
  int modelType;
  boolean hasPdbRecords;
  PdbMolecule pdbMolecule;

  final static int growthIncrement = 128;
  int atomCount = 0;
  public Atom[] atoms;
  int bondCount = 0;
  public Bond[] bonds;

  public Frame(JmolViewer viewer, int atomCount,
                   int modelType, boolean hasPdbRecords) {
    this.viewer = viewer;
    this.modelType = modelType;
    this.hasPdbRecords = hasPdbRecords;
    if (hasPdbRecords)
      pdbMolecule = new PdbMolecule(this);
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
    if (viewer.getAutoBond()) {
      if ((bondCount == 0) ||
          (hasPdbRecords && (bondCount < (atomCount / 2))))
        rebond(false);
    }
    if (hasPdbRecords)
      pdbMolecule.freeze();
  }

  public Atom addAtom(Object clientAtom) {
    if (atomCount == atoms.length) {
      Atom[] newAtoms =
        new Atom[atoms.length + growthIncrement];
      System.arraycopy(atoms, 0, newAtoms, 0, atoms.length);
      atoms = newAtoms;
    }
    Atom atom = new Atom(this, atomCount, pdbMolecule, clientAtom);
    atoms[atomCount++] = atom;
    if (htAtomMap != null)
      htAtomMap.put(clientAtom, atom);
    if (bspf != null)
      bspf.addTuple(atom.getModelNumber(), atom);
    float covalentRadius = atom.getCovalentRadius();
    if (covalentRadius > maxCovalentRadius)
      maxCovalentRadius = covalentRadius;
    float vdwRadius = atom.getVanderwaalsRadius();
    if (vdwRadius > maxVanderwaalsRadius)
      maxVanderwaalsRadius = vdwRadius;
    return atom;
  }

  public int getAtomCount() {
    return atomCount;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public Point3f getAtomPoint3f(int atomIndex) {
    return atoms[atomIndex].getPoint3f();
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

  Dots dots;

  public void setDotsOn(boolean dotsOn, BitSet bsSelected) {
    if (dotsOn && dots == null)
      dots = new Dots(viewer, this, frameRenderer.getDotsRenderer());
    if (dots != null)
      dots.setDotsOn(dotsOn, bsSelected);
  }

  Ribbons ribbons;

  public void setRibbons(int ribbonType, BitSet bsSelected) {
    if (ribbonType != 0 && ribbons == null)
      ribbons = new Ribbons(viewer, this);
    if (ribbons != null)
      ribbons.setRibbons(ribbonType, bsSelected);
  }

  Trace trace;

  public void setTraceMad(short mad, BitSet bsSelected) {
    if (mad != 0 && trace == null)
      trace = new Trace(viewer, this);
    if (trace != null)
      trace.setMad(mad, bsSelected);
  }
  
  public void setTraceColix(byte palette, short colix, BitSet bsSelected) {
    if ((palette != JmolConstants.PALETTE_CPK || colix != 0) && trace == null)
      trace = new Trace(viewer, this);
    if (trace != null)
      trace.setColix(palette, colix, bsSelected);
  }
  
  Cartoon cartoon;

  public void setCartoonMad(short mad, BitSet bsSelected) {
    if (mad != 0 && cartoon == null)
      cartoon = new Cartoon(viewer, this);
    if (cartoon != null)
      cartoon.setMad(mad, bsSelected);
  }
  
  public void setCartoonColix(byte palette, short colix, BitSet bsSelected) {
    if ((palette != JmolConstants.PALETTE_CPK || colix != 0) && cartoon == null)
      cartoon = new Cartoon(viewer, this);
    if (cartoon != null)
      cartoon.setColix(palette, colix, bsSelected);
  }
  
  Strands strands;

  public void setStrandsMad(short mad, BitSet bsSelected) {
    if (mad != 0 && strands == null)
      strands = new Strands(viewer, this);
    if (strands != null)
      strands.setMad(mad, bsSelected);
  }
  
  public void setStrandsColix(byte palette, short colix, BitSet bsSelected) {
    if ((palette != JmolConstants.PALETTE_CPK || colix != 0) &&
        strands == null)
      strands = new Strands(viewer, this);
    if (strands != null)
      strands.setColix(palette, colix, bsSelected);
  }
  
  Axes axes;
  public void setModeAxes(byte modeAxes) {
    if (modeAxes != JmolConstants.AXES_NONE && axes == null)
      axes = new Axes(viewer);
    if (axes != null)
      axes.setMode(modeAxes);
  }

  Bbox bbox;
  public void setShowBoundingBox(boolean showBoundingBox) {
    if (showBoundingBox && bbox == null)
      bbox = new Bbox(viewer);
    if (bbox != null)
      bbox.setShowBoundingBox(showBoundingBox);
  }

  Point3f centerBoundingBox;
  Point3f cornerBoundingBox;
  Point3f centerRotation;
  float radiusBoundingBox;
  float radiusRotation;

  public float getGeometricRadius() {
    findBounds();
    return radiusBoundingBox;
  }

  public Point3f getBoundingBoxCenter() {
    findBounds();
    return centerBoundingBox;
  }

  public Point3f getBoundingBoxCorner() {
    findBounds();
    return cornerBoundingBox;
  }

  public Point3f getRotationCenter() {
    findBounds();
    return centerRotation;
  }

  public float getRotationRadius() {
    findBounds();
    return radiusRotation;
  }

  public void setRotationCenter(Point3f newCenterOfRotation) {
    if (newCenterOfRotation != null) {
      centerRotation = newCenterOfRotation;
      radiusRotation = calcRadius(centerRotation);
    } else {
      centerRotation = centerBoundingBox;
      radiusRotation = radiusBoundingBox;
    }
  }

  public void clearBounds() {
    centerBoundingBox = centerRotation = null;
    radiusBoundingBox = radiusRotation = 0;
  }

  private void findBounds() {
    if ((centerBoundingBox != null) || (atomCount <= 0))
      return;
    calcBoundingBox();
    centerRotation = centerBoundingBox;
    radiusBoundingBox = radiusRotation = calcRadius(centerBoundingBox);
  }

  private void calcBoundingBox() {
    // bounding box is defined as the center of the cartesian coordinates
    // as stored in the file
    // Note that this is not really the geometric center of the molecule
    // ... for this we would need to do a Minimal Enclosing Sphere calculation
    float minX, minY, minZ, maxX, maxY, maxZ;
    Point3f point;
    if (cellLineCount == 0) { // non-crystal, so find extremes of atoms
      point = atoms[0].getPoint3f();
      minX = maxX = point.x;
      minY = maxY = point.y;
      minZ = maxZ = point.z;
      
      for (int i = atomCount; --i > 0; ) {
        // note that the 0 element was set above
        point = atoms[i].getPoint3f();
        float t;
        t = point.x;
        if (t < minX) { minX = t; }
        else if (t > maxX) { maxX = t; }
        t = point.y;
        if (t < minY) { minY = t; }
        else if (t > maxY) { maxY = t; }
        t = point.z;
        if (t < minZ) { minZ = t; }
        else if (t > maxZ) { maxZ = t; }
      }
    } else { // a crystal cell, so use center of crystal cell box
      point = cellLines[0].pointOrigin;
      minX = maxX = point.x;
      minY = maxY = point.y;
      minZ = maxZ = point.z;
      for (int i = cellLineCount; --i >= 0; ) {
        point = cellLines[i].pointOrigin;
        int j = 0;
        do {
          float t;
          t = point.x;
          if (t < minX) { minX = t; }
          else if (t > maxX) { maxX = t; }
          t = point.y;
          if (t < minY) { minY = t; }
          else if (t > maxY) { maxY = t; }
          t = point.z;
          if (t < minZ) { minZ = t; }
          else if (t > maxZ) { maxZ = t; }
          point = cellLines[i].pointEnd;
        } while (j++ == 0);
      }
    }
      
    centerBoundingBox = new Point3f((minX + maxX) / 2,
                                    (minY + maxY) / 2,
                                    (minZ + maxZ) / 2);
    cornerBoundingBox = new Point3f(maxX, maxY, maxZ);
    cornerBoundingBox.sub(centerBoundingBox);
  }

  private float calcRadius(Point3f center) {
    float radius = 0;
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      Point3f posAtom = atom.getPoint3f();
      float distAtom = center.distance(posAtom);
      float radiusVdw = atom.getVanderwaalsRadius();
      float distVdw = distAtom + radiusVdw;
      
      if (distVdw > radius)
        radius = distVdw;
      /*
      if (atom.hasVector()) {
        // mth 2002 nov
        // this calculation isn't right, but I can't get it to work with
        // samples/cs2.syz when I try to use
        // float distVector = center.distance(atom.getScaledVector());
        // So I am over-estimating and giving up for the day. 
        float distVector = distAtom + atom.getVectorMagnitude();
        if (distVector > radius)
          radius = distVector;
      }
      */
    }
    for (int i = lineCount; --i >= 0; ) {
      Line line = lines[i];
      float distLineEnd;
      distLineEnd = center.distance(line.pointOrigin);
      if (distLineEnd > radius)
        radius = distLineEnd;
      distLineEnd = center.distance(line.pointEnd);
      if (distLineEnd > radius)
        radius = distLineEnd;
    }
    for (int i = cellLineCount; --i >= 0; ) {
      Line cellLine = cellLines[i];
      float distLineEnd;
      distLineEnd = center.distance(cellLine.pointOrigin);
      if (distLineEnd > radius)
        radius = distLineEnd;
      distLineEnd = center.distance(cellLine.pointEnd);
      if (distLineEnd > radius)
        radius = distLineEnd;
    }
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

  int cellLineCount = 0;
  Line[] cellLines = null;

  public void addCrystalCellLine(Line line) {
    if (cellLines == null ||
        cellLineCount == cellLines.length) {
      Line[] newLines = new Line[cellLineCount + lineGrowthIncrement];
      if (cellLines != null)
        System.arraycopy(cellLines, 0, newLines, 0,
                         cellLineCount);
      cellLines = newLines;
    }
    cellLines[cellLineCount++] = line;
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
        if (atom.styleAtom >= JmolConstants.STYLE_NONE) // not deleted atoms
          bspf.addTuple(atom.getModelNumber(), atom);
      }
    }
  }

  private void clearBspf() {
    bspf = null;
  }

  private WithinIterator withinAtomIterator = new WithinIterator();
  private WithinIterator withinPointIterator = new WithinIterator();
  private PointWrapper pointWrapper = new PointWrapper();

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
      int bsptIndexLast = bspf.getBsptCount();
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
      for (int i = bspf.getBsptCount(); --i >= 0; ) {
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
      float myCovalentRadius = atom.getCovalentRadius();
      float searchRadius =
        myCovalentRadius + maxCovalentRadius + bondTolerance;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelNumber());
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        if (atomNear != atom) {
          int order = getBondOrder(atom, myCovalentRadius,
                                   atomNear, atomNear.getCovalentRadius(),
                                   iter.foundDistance2());
          if (order > 0)
            addBond(atom.bondMutually(atomNear, order));
        }
      }
      iter.release();

      // FIXME mth 2003 11 30
      // get this out of here and implement it more like like trace
      // alpha carbon mechanims is not consistent

      // Protein backbone bonds
      if (hasPdbRecords) {
        PdbAtom pdbatom = atom.getPdbAtom();
        if (pdbatom != null) {
          char chainThis = pdbatom.getChainID();
          if (chainThis == chainLast) {
            if (pdbatom.getName().equals("CA")) {
              if (atomLastCA != null) {
		  bondAtoms(atom, atomLastCA,
				 Bond.BACKBONE);
              }
              atomLastCA = atom;
            }
          } else {
            chainLast = chainThis;
            atomLastCA = null;
          }
        } else {
          chainLast = '?';
        }
      }
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to autoBond=" + (timeEnd - timeBegin));
    }
  }

  private int getBondOrder(Atom atomA, float covalentRadiusA,
                           Atom atomB, float covalentRadiusB,
                           float distance2) {
    //            System.out.println(" radiusA=" + covalentRadiusA +
    //                               " radiusB=" + covalentRadiusB +
    //                         " distance2=" + distance2 +
    //                         " tolerance=" + bondTolerance);
    float maxAcceptable = covalentRadiusA + covalentRadiusB + bondTolerance;
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

  public void calcHbonds() {
    if (hbondsCalculated)
      return;
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
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.getModelNumber());
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
        addBond(atom.bondMutually(atomNear, Bond.HYDROGEN));
        System.out.println("adding an hbond between " + atom.atomIndex +
                           " & " + atomNear.atomIndex);
      }
      iter.release();
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to hbond=" + (timeEnd - timeBegin));
    }
    hbondsCalculated = true;
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
}
