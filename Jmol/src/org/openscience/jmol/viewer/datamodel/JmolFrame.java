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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.protein.ProteinProp;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3d;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class JmolFrame {

  public JmolViewer viewer;
    public FrameRenderer frameRenderer;
  // the maximum CovalentRadius seen in this set of atoms
  // used in autobonding
  double maxCovalentRadius = 0.0;
  double maxVanderwaalsRadius = 0.0;
  // whether or not this frame has any protein properties
  boolean hasPdbRecords;

  final static int growthIncrement = 128;
  int atomShapeCount = 0;
  public AtomShape[] atomShapes;
  int bondShapeCount = 0;
  public BondShape[] bondShapes;

  public JmolFrame(JmolViewer viewer, int atomCount,
                   boolean hasPdbRecords) {
    this.viewer = viewer;
    this.hasPdbRecords = hasPdbRecords;
    atomShapes = new AtomShape[atomCount];
    bondShapes = new BondShape[atomCount * 2];
    this.frameRenderer = viewer.getFrameRenderer();
  }

  public JmolFrame(JmolViewer viewer, boolean hasPdbRecords) {
    this(viewer, growthIncrement, false);
  }

  public JmolFrame(JmolViewer viewer) {
    this(viewer, false);
  }

  public void finalize() {
    htAtomMap = null;
    if (bondShapeCount == 0 && viewer.getAutoBond())
      rebond();
  }

  public AtomShape addAtom(Object clientAtom) {
    if (atomShapeCount == atomShapes.length) {
      AtomShape[] newAtomShapes =
        new AtomShape[atomShapes.length + growthIncrement];
      System.arraycopy(atomShapes, 0, newAtomShapes, 0, atomShapes.length);
      atomShapes = newAtomShapes;
    }
    AtomShape atomShape = new AtomShape(this, atomShapeCount, clientAtom);
    atomShapes[atomShapeCount++] = atomShape;
    if (htAtomMap != null)
      htAtomMap.put(clientAtom, atomShape);
    if (bspt != null)
      bspt.addTuple(atomShape);
    double covalentRadius = atomShape.getCovalentRadius();
    if (covalentRadius > maxCovalentRadius)
      maxCovalentRadius = covalentRadius;
    double vdwRadius = atomShape.getVanderwaalsRadius();
    if (vdwRadius > maxVanderwaalsRadius)
      maxVanderwaalsRadius = vdwRadius;
    return atomShape;
  }

  public int getAtomCount() {
    return atomShapeCount;
  }

  public AtomShape getAtomAt(int atomIndex) {
    return atomShapes[atomIndex];
  }

  public int getBondCount() {
    return bondShapeCount;
  }

  public BondShape getBondAt(int bondIndex) {
    return bondShapes[bondIndex];
  }

  public boolean hasPdbRecords() {
    return hasPdbRecords;
  }

  private Hashtable htAtomMap = null;

  private void initAtomMap() {
    if (htAtomMap == null) {
      htAtomMap = new Hashtable();
      for (int i = atomShapeCount; --i >= 0; ) {
        AtomShape atomShape = atomShapes[i];
        htAtomMap.put(atomShape.getClientAtom(), atomShape);
      }
    }
  }

  private void addBondShape(BondShape bondShape) {
    if (bondShape == null)
      return;
    if (bondShapeCount == bondShapes.length) {
      BondShape[] newBondShapes =
        new BondShape[bondShapes.length + growthIncrement];
      System.arraycopy(bondShapes, 0, newBondShapes, 0, bondShapes.length);
      bondShapes = newBondShapes;
    }
    bondShapes[bondShapeCount++] = bondShape;
  }

  public void bondAtomShapes(Object clientAtom1, Object clientAtom2,
                             int order) {
    if (htAtomMap == null)
      initAtomMap();
    AtomShape atomShape1 = (AtomShape)htAtomMap.get(clientAtom1);
    if (atomShape1 == null)
      return;
    AtomShape atomShape2 = (AtomShape)htAtomMap.get(clientAtom2);
    if (atomShape2 == null)
      return;
    addBondShape(atomShape1.bondMutually(atomShape2, order));
  }

  public void bondAtomShapes(AtomShape atomShape1, Object clientAtom2,
                             int order) {
    if (htAtomMap == null)
      initAtomMap();
    AtomShape atomShape2 = (AtomShape)htAtomMap.get(clientAtom2);
    if (atomShape2 == null)
      return;
    addBondShape(atomShape1.bondMutually(atomShape2, order));
  }

  public void bondAtomShapes(AtomShape atomShape1, AtomShape atomShape2,
                             int order) {
    addBondShape(atomShape1.bondMutually(atomShape2, order));
  }

  Point3d centerBoundingBox;
  Point3d cornerBoundingBox;
  Point3d centerRotation;
  double radiusBoundingBox;
  double radiusRotation;

  public double getGeometricRadius() {
    findBounds();
    return radiusBoundingBox;
  }

  public Point3d getBoundingBoxCenter() {
    findBounds();
    return centerBoundingBox;
  }

  public Point3d getBoundingBoxCorner() {
    findBounds();
    return cornerBoundingBox;
  }

  public Point3d getRotationCenter() {
    findBounds();
    return centerRotation;
  }

  public double getRotationRadius() {
    findBounds();
    return radiusRotation;
  }

  public void setRotationCenter(Point3d newCenterOfRotation) {
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
    if ((centerBoundingBox != null) || (atomShapeCount <= 0))
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
    double minX, minY, minZ, maxX, maxY, maxZ;
    Point3d point;
    if (crystalCellLineCount == 0) { // non-crystal, so find extremes of atoms
      point = atomShapes[0].getPoint3d();
      minX = maxX = point.x;
      minY = maxY = point.y;
      minZ = maxZ = point.z;
      
      for (int i = atomShapeCount; --i > 0; ) {
        // note that the 0 element was set above
        point = atomShapes[i].getPoint3d();
        double t;
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
      point = crystalCellLines[0].getPoint1();
      minX = maxX = point.x;
      minY = maxY = point.y;
      minZ = maxZ = point.z;
      for (int i = crystalCellLineCount; --i >= 0; ) {
        point = crystalCellLines[i].getPoint1();
        int j = 0;
        do {
          double t;
          t = point.x;
          if (t < minX) { minX = t; }
          else if (t > maxX) { maxX = t; }
          t = point.y;
          if (t < minY) { minY = t; }
          else if (t > maxY) { maxY = t; }
          t = point.z;
          if (t < minZ) { minZ = t; }
          else if (t > maxZ) { maxZ = t; }
          point = crystalCellLines[i].getPoint2();
        } while (j++ == 0);
      }
    }
      
    centerBoundingBox = new Point3d((minX + maxX) / 2,
                                    (minY + maxY) / 2,
                                    (minZ + maxZ) / 2);
    cornerBoundingBox = new Point3d(maxX, maxY, maxZ);
    cornerBoundingBox.sub(centerBoundingBox);
  }

  private double calcRadius(Point3d center) {
    double radius = 0.0f;
    for (int i = atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      Point3d posAtom = atomShape.getPoint3d();
      double distAtom = center.distance(posAtom);
      double radiusVdw = atomShape.getVanderwaalsRadius();
      double distVdw = distAtom + radiusVdw;
      
      if (distVdw > radius)
        radius = distVdw;
      /*
      if (atom.hasVector()) {
        // mth 2002 nov
        // this calculation isn't right, but I can't get it to work with
        // samples/cs2.syz when I try to use
        // double distVector = center.distance(atom.getScaledVector());
        // So I am over-estimating and giving up for the day. 
        double distVector = distAtom + atom.getVectorMagnitude();
        if (distVector > radius)
          radius = distVector;
      }
      */
    }
    for (int i = lineShapeCount; --i >= 0; ) {
      LineShape ls = lineShapes[i];
      double distLineEnd;
      distLineEnd = center.distance(ls.getPoint1());
      if (distLineEnd > radius)
        radius = distLineEnd;
      distLineEnd = center.distance(ls.getPoint2());
      if (distLineEnd > radius)
        radius = distLineEnd;
    }
    for (int i = crystalCellLineCount; --i >= 0; ) {
      LineShape ls = crystalCellLines[i];
      double distLineEnd;
      distLineEnd = center.distance(ls.getPoint1());
      if (distLineEnd > radius)
        radius = distLineEnd;
      distLineEnd = center.distance(ls.getPoint2());
      if (distLineEnd > radius)
        radius = distLineEnd;
    }
    return radius;
  }

  final static int lineGrowthIncrement = 16;
  int lineShapeCount = 0;
  LineShape[] lineShapes = null;

  public void addLineShape(LineShape shape) {
    if (lineShapes == null || lineShapeCount == lineShapes.length) {
      LineShape[] newShapes =
        new LineShape[lineShapeCount + lineGrowthIncrement];
      if (lineShapes != null)
        System.arraycopy(lineShapes, 0, newShapes, 0, lineShapeCount);
      lineShapes = newShapes;
    }
    lineShapes[lineShapeCount++] = shape;
  }

  int crystalCellLineCount = 0;
  LineShape[] crystalCellLines = null;

  public void addCrystalCellLine(LineShape line) {
    if (crystalCellLines == null ||
        crystalCellLineCount == crystalCellLines.length) {
      LineShape[] newShapes =
        new LineShape[crystalCellLineCount + lineGrowthIncrement];
      if (crystalCellLines != null)
        System.arraycopy(crystalCellLines, 0, newShapes, 0,
                         crystalCellLineCount);
      crystalCellLines = newShapes;
    }
    crystalCellLines[crystalCellLineCount++] = line;
  }

  final static int measurementGrowthIncrement = 16;
  int measurementShapeCount = 0;
  MeasurementShape[] measurementShapes = null;

  public void addMeasurementShape(MeasurementShape shape) {
    if (measurementShapes == null ||
        measurementShapeCount == measurementShapes.length) {
      MeasurementShape[] newShapes =
        new MeasurementShape[measurementShapeCount +
                             measurementGrowthIncrement];
      if (measurementShapes != null)
        System.arraycopy(measurementShapes, 0,
                         newShapes, 0, measurementShapeCount);
      measurementShapes = newShapes;
    }
    measurementShapes[measurementShapeCount++] = shape;
  }

  public void clearMeasurementShapes() {
    measurementShapeCount = 0;
    measurementShapes = null;
  }

  public void deleteMeasurementShape(int imeasurement) {
    System.arraycopy(measurementShapes, imeasurement+1,
                     measurementShapes, imeasurement,
                     measurementShapeCount - imeasurement - 1);
    --measurementShapeCount;
    measurementShapes[measurementShapeCount] = null;
  }

  public int getMeasurementShapeCount() {
    return measurementShapeCount;
  }

  public MeasurementShape[] getMeasurementShapes() {
    return measurementShapes;
  }

  /****************************************************************
   * selection handling
   ****************************************************************/
  final static int selectionPixelLeeway = 5;

  public int findNearestAtomIndex(int x, int y) {
    /*
     * FIXME
     * mth - this code has problems
     * 1. doesn't take radius of atom into account until too late
     * 2. doesn't take Z dimension into account, so it could select an atom
     *    which is behind the one the user wanted
     * 3. doesn't take into account the fact that hydrogens could be hidden
     *    you can select a region and get extra hydrogens
     */
    if (atomShapeCount == 0)
      return -1;
    AtomShape atomShapeNearest = null;
    int indexNearest = -1;
    int r2Nearest = Integer.MAX_VALUE;
    for (int i = atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      int dx = atomShape.x - x;
      int dx2 = dx * dx;
      if (dx2 > r2Nearest)
        continue;
      int dy = atomShape.y - y;
      int dy2 = dy * dy;
      if (dy2 + dx2 > r2Nearest)
        continue;
      atomShapeNearest = atomShape; // will definitely happen the first time through
      r2Nearest = dx2 + dy2;
      indexNearest = i;
    }
    int rNearest = (int)Math.sqrt(r2Nearest);
    return (rNearest > atomShapeNearest.diameter/2 + selectionPixelLeeway)
      ? -1
      : indexNearest;
  }
    
  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();
  public BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      if (rect.contains(atomShape.x, atomShape.y))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  public AtomShapeIterator getAtomIterator(BitSet bsSelected) {
    return new SelectedAtomShapeIterator(bsSelected);
  }

  class SelectedAtomShapeIterator implements AtomShapeIterator {

    int iAtomShape;
    BitSet bsSelected;

    SelectedAtomShapeIterator(BitSet bsSelected) {
      this.bsSelected = bsSelected;
      iAtomShape = 0;
    }

    public boolean hasNext() {
      for ( ; iAtomShape < atomShapeCount; ++iAtomShape)
        if (bsSelected.get(iAtomShape))
          return true;
      return false;
    }

    public AtomShape next() {
      return atomShapes[iAtomShape++];
    }
  }

  public BondShapeIterator getBondIterator(byte bondType, BitSet bsSelected) {
    return new SelectedBondShapeIterator(bondType, bsSelected);
  }

  class SelectedBondShapeIterator implements BondShapeIterator {

    byte bondType;
    int iBondShape;
    BitSet bsSelected;
    boolean bondSelectionModeOr;

    SelectedBondShapeIterator(byte bondType, BitSet bsSelected) {
      this.bondType = bondType;
      this.bsSelected = bsSelected;
      iBondShape = 0;
      bondSelectionModeOr = viewer.getBondSelectionModeOr();
    }

    public boolean hasNext() {
      for ( ; iBondShape < bondShapeCount; ++iBondShape) {
        BondShape bondShape = bondShapes[iBondShape];
        if ((bondShape.order & bondType) == 0)
          continue;
        boolean isSelected1 =
          bsSelected.get(bondShape.atomShape1.getAtomIndex());
        boolean isSelected2 =
          bsSelected.get(bondShape.atomShape2.getAtomIndex());
        if ((!bondSelectionModeOr & isSelected1 & isSelected2) ||
            (bondSelectionModeOr & (isSelected1 | isSelected2)))
          return true;
      }
      return false;
    }

    public BondShape next() {
      return bondShapes[iBondShape++];
    }
  }

  private Bspt bspt;

  private Bspt getBspt() {
    if (bspt == null) {
      bspt = new Bspt(3);
      for (int i = atomShapeCount; --i >= 0; ) {
        AtomShape atom = atomShapes[i];
        if (atom.styleAtom >= JmolViewer.NONE) // don't add deleted atoms
          bspt.addTuple(atom);
      }
    }
    return bspt;
  }

  private void clearBspt() {
    bspt = null;
  }

  public AtomShapeIterator getWithinIterator(AtomShape atomCenter,
                                             double distance) {
    return new WithinAtomShapeIterator(atomCenter, distance);
  }

  class WithinAtomShapeIterator implements AtomShapeIterator {

    Bspt.EnumerateSphere enum;

    WithinAtomShapeIterator(AtomShape atomCenter, double distance) {
      enum = getBspt().enumSphere(atomCenter, distance);
    }

    public boolean hasNext() {
      return enum.hasMoreElements();
    }

    public AtomShape next() {
      return (AtomShape)enum.nextElement();
    }
  }

  final static boolean showRebondTimes = true;

  private double bondTolerance;
  private double minBondDistance;
  private double minBondDistance2;

  public void rebond() {
    deleteAllBonds();
    bondTolerance = viewer.getBondTolerance();
    minBondDistance = viewer.getMinBondDistance();
    minBondDistance2 = minBondDistance*minBondDistance;

    char chainLast = '?';
    int indexLastCA = -1;
    AtomShape atomLastCA = null;

    Bspt bspt = getBspt();
    long timeBegin, timeEnd;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomShapeCount; --i >= 0; ) {
      AtomShape atom = atomShapes[i];
      // Covalent bonds
      double myCovalentRadius = atom.getCovalentRadius();
      double searchRadius =
        myCovalentRadius + maxCovalentRadius + bondTolerance;
      for (Bspt.EnumerateSphere e = bspt.enumHemiSphere(atom, searchRadius);
           e.hasMoreElements(); ) {
        AtomShape atomNear = (AtomShape)e.nextElement();
        if (atomNear != atom) {
          int order = getBondOrder(atom, myCovalentRadius,
                                   atomNear, atomNear.getCovalentRadius(),
                                   e.foundDistance2());
          if (order > 0)
            addBondShape(atom.bondMutually(atomNear, order));
        }
      }

      // Protein backbone bonds
      if (hasPdbRecords) {
        ProteinProp pprop = atom.getProteinProp();
        if (pprop != null) {
          char chainThis = pprop.getChain();
          if (chainThis == chainLast) {
            if (pprop.getName().equals("CA")) {
              if (atomLastCA != null) {
              bondAtomShapes(atom, atomLastCA,
                             BondShape.BACKBONE);
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

  private int getBondOrder(AtomShape atomA, double covalentRadiusA,
                           AtomShape atomB, double covalentRadiusB,
                           double distance2) {
    //            System.out.println(" radiusA=" + covalentRadiusA +
    //                               " radiusB=" + covalentRadiusB +
    //                         " distance2=" + distance2 +
    //                         " tolerance=" + bondTolerance);
    double maxAcceptable = covalentRadiusA + covalentRadiusB + bondTolerance;
    double maxAcceptable2 = maxAcceptable * maxAcceptable;
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

  public void deleteAllBonds() {
    for (int i = bondShapeCount; --i >= 0; ) {
      bondShapes[i].deleteAtomReferences();
      bondShapes[i] = null;
    }
    bondShapeCount = 0;
  }

  public void deleteBond(BondShape bond) {
    // what a disaster ... I hate doing this
    for (int i = bondShapeCount; --i >= 0; ) {
      if (bondShapes[i] == bond) {
        bondShapes[i].deleteAtomReferences();
        System.arraycopy(bondShapes, i+1, bondShapes, i,
                         bondShapeCount - i - 1);
        --bondShapeCount;
        bondShapes[bondShapeCount] = null;
      }
    }
  }

  public void deleteCovalentBonds() {
    int indexNoncovalent = 0;
    for (int i = 0; i < bondShapeCount; ++i) {
      BondShape bond = bondShapes[i];
      if (! bond.isCovalent()) {
        if (i != indexNoncovalent) {
          bondShapes[indexNoncovalent++] = bond;
          bondShapes[i] = null;
        }
      } else {
        bond.deleteAtomReferences();
        bondShapes[i] = null;
      }
    }
    bondShapeCount = indexNoncovalent;
  }

  public Object deleteAtom(int atomIndex) { // returns the clientAtom
    clearBspt();
    return atomShapes[atomIndex].markDeleted();
  }

  public double getMaxVanderwaalsRadius() {
    return maxVanderwaalsRadius;
  }
}
