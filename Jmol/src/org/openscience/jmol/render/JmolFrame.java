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

package org.openscience.jmol.render;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.ProteinProp;
import org.openscience.jmol.g25d.Graphics25D;
import javax.vecmath.Point3d;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class JmolFrame {

  public DisplayControl control;
  // the maximum CovalentRadius seen in this set of atoms
  // used in autobonding
  private double maxCovalentRadius = 0.0;
  // whether or not this frame has any protein properties
  public boolean hasProteinProps;

  Shape[] shapes;

  final static int growthIncrement = 128;
  int atomShapeCount = 0;
  AtomShape[] atomShapes;
  int bondShapeCount = 0;
  BondShape[] bondShapes;

  public JmolFrame(DisplayControl control, int atomCount) {
    this.control = control;
    atomShapes = new AtomShape[atomCount];
    bondShapes = new BondShape[atomCount * 2];
  }

  public JmolFrame(DisplayControl control) {
    this(control, growthIncrement);
  }

  public void finalize() {
    htAtomMap = null;
    if (bondShapeCount == 0 && control.getAutoBond())
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
    if (!hasProteinProps)
      hasProteinProps = (atomShape.getProteinProp() != null);
    return atomShape;
  }

  public int getAtomCount() {
    return atomShapeCount;
  }

  public AtomShape getAtomAt(int atomIndex) {
    return atomShapes[atomIndex];
  }

  private Hashtable htAtomMap = null;

  private void initAtomMap() {
    if (htAtomMap == null) {
      htAtomMap = new Hashtable();
      for (int i = atomShapeCount; --i >= 0; ) {
        AtomShape atomShape = atomShapes[i];
        htAtomMap.put(atomShape.clientAtom, atomShape);
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

  private Point3d centerBoundingBox;
  private Point3d cornerBoundingBox;
  private Point3d centerRotation;
  private double radiusBoundingBox;
  private double radiusRotation;

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

  public void render(Graphics25D g25d, DisplayControl control) {
    if (atomShapeCount <= 0)
      return;
    if (shapes == null || control.hasStructuralChange()) {
      AtomShape[] atomShapes = this.atomShapes;
      LineShape[] lineShapes = this.lineShapes;
      MeasurementShape[] measurementShapes = this.measurementShapes;
      Shape[] axisShapes = null;
      int axisShapeCount = 0;
      Shape[] bboxShapes = null;
      int bboxShapeCount = 0;
      if (control.getModeAxes() != DisplayControl.AXES_NONE) {
        axisShapes = control.getAxes().getShapes();
        axisShapeCount = axisShapes.length;
      }
      if (control.getShowBoundingBox()) {
        bboxShapes = control.getBoundingBox().getBboxShapes();
        bboxShapeCount = bboxShapes.length;
      }
      int shapeCount =
        atomShapeCount + lineShapeCount + crystalCellLineCount +
        measurementShapeCount +
        axisShapeCount + bboxShapeCount;
      shapes = new Shape[shapeCount];
      int i = 0;

      System.arraycopy(atomShapes, 0, shapes, i, atomShapeCount);
      i += atomShapeCount;

      if (lineShapes != null) {
        System.arraycopy(lineShapes, 0, shapes, i, lineShapeCount);
        i += lineShapeCount;
      }
      if (crystalCellLines != null) {
        System.arraycopy(crystalCellLines, 0, shapes, i, crystalCellLineCount);
        i += crystalCellLineCount;
      }
      if (measurementShapes != null) {
        System.arraycopy(measurementShapes, 0,
                         shapes, i, measurementShapeCount);
        i += measurementShapeCount;
      }
      if (axisShapes != null) {
        System.arraycopy(axisShapes, 0, shapes, i, axisShapeCount);
        i += axisShapeCount;
      }
      if (bboxShapes != null) {
        System.arraycopy(bboxShapes, 0, shapes, i, bboxShapeCount);
        i += bboxShapeCount;
      }
    }

    control.calcViewTransformMatrix();

    for (int i = 0; i < shapes.length; ++i)
      shapes[i].transform(control);
    
    /*
    if (control.jvm12orGreater) {
      UseJavaSort.sortShapes(shapes);
    } else {
      NonJavaSort.sortShapes(shapes);
    }
    */
    
    for (int i = shapes.length; --i >= 0 ; )
      shapes[i].render(g25d, control);
  }

  final static int lineGrowthIncrement = 16;
  private int lineShapeCount = 0;
  private LineShape[] lineShapes = null;

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

  private int crystalCellLineCount = 0;
  private LineShape[] crystalCellLines = null;

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
  private int measurementShapeCount = 0;
  private MeasurementShape[] measurementShapes = null;

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
  private final BitSet bsEmpty = new BitSet();
  private final BitSet bsFoundRectangle = new BitSet();
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
    return new SelectedCovalentBondIterator(bondType, bsSelected);
  }

  class SelectedCovalentBondIterator implements BondShapeIterator {
    BitSet bsSelected;
    int bondType;

    int iAtomShape;
    boolean tHaveAtom;
    int iBondShape;
    int iAtomShapeCurrent;
    AtomShape atomShapeCurrent;
    BondShape[] bondsCurrent;
    boolean bondSelectionModeOr;

    SelectedCovalentBondIterator(byte bondType, BitSet bsSelected) {
      this.bondType = bondType;
      this.bsSelected = bsSelected;
      iAtomShape = iBondShape = 0;
      bondSelectionModeOr = control.getBondSelectionModeOr();
    }

    public boolean hasNext() {
      while (true) {
        while (atomShapeCurrent == null ||
               bondsCurrent == null ||
               iBondShape == bondsCurrent.length) {
          if (iAtomShape == atomShapeCount)
            return false;
          iAtomShapeCurrent = iAtomShape++;
          if (bsSelected.get(iAtomShapeCurrent)) {
            atomShapeCurrent=atomShapes[iAtomShapeCurrent];
            bondsCurrent = atomShapeCurrent.bonds;
            iBondShape = 0;
          } else {
            atomShapeCurrent = null;
            bondsCurrent = null;
          }
        }
        for ( ; iBondShape < bondsCurrent.length; ++iBondShape) {
          BondShape bondShape = bondsCurrent[iBondShape];
          if ((bondShape.order & bondType) == 0)
            continue;
          AtomShape atomShapeOther =
            (bondShape.atomShape1 != atomShapeCurrent ?
             bondShape.atomShape1 : bondShape.atomShape2);
          boolean tOtherSelected = bsSelected.get(atomShapeOther.atomIndex);
          if (bondSelectionModeOr) {
            if (!tOtherSelected || bondShape.atomShape1 == atomShapeCurrent)
              return true;
          } else {
            if (tOtherSelected && bondShape.atomShape1 == atomShapeCurrent)
              return true;
          }
        }
      }
    }

    public BondShape next() {
      return bondsCurrent[iBondShape++];
    }
  }

  private Bspt bspt;

  private Bspt getBspt() {
    if (bspt == null) {
      bspt = new Bspt(3);
      for (int i = atomShapeCount; --i >= 0; ) {
        AtomShape atom = atomShapes[i];
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
    bondTolerance = control.getBondTolerance();
    minBondDistance = control.getMinBondDistance();
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
      if (hasProteinProps) {
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
      bondShapes[i].delete();
      bondShapes[i] = null;
    }
    bondShapeCount = 0;
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
        bond.delete();
        bondShapes[i] = null;
      }
    }
    bondShapeCount = indexNoncovalent;
  }
}
