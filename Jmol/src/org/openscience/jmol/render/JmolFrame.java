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
import org.openscience.jmol.UseJavaSort;
import org.openscience.jmol.applet.NonJavaSort;
import org.openscience.jmol.g25d.Graphics25D;
import javax.vecmath.Point3d;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class JmolFrame {

  DisplayControl control;

  public JmolFrame(DisplayControl control) {
    this.control = control;
  }

  int atomShapeCount = 0;
  final static int growthIncrement = 128;
  int iaMax = growthIncrement;
  AtomShape[] atomShapes = new AtomShape[growthIncrement];

  public void addAtomShape(AtomShape atomShape) {
    if (atomShapeCount == iaMax) {
      AtomShape[] newAtomShapes = new AtomShape[iaMax + growthIncrement];
      System.arraycopy(atomShapes, 0, newAtomShapes, 0, iaMax);
      atomShapes = newAtomShapes;
      iaMax += growthIncrement;
    }
    atomShape.setAtomIndex(atomShapeCount);
    atomShapes[atomShapeCount++] = atomShape;
    if (htAtomMap != null)
      htAtomMap.put(atomShape.atom, atomShape);
  }

  private Hashtable htAtomMap = null;

  private void initAtomMap() {
    if (htAtomMap == null) {
      htAtomMap = new Hashtable();
      for (int i = atomShapeCount; --i >= 0; ) {
        AtomShape atomShape = atomShapes[i];
        htAtomMap.put(atomShape.atom, atomShape);
      }
    }
  }

  public void bondAtomShapes(JmolAtom atom1, JmolAtom atom2, int order) {
    if (htAtomMap == null)
      initAtomMap();
    AtomShape atomShape1 = (AtomShape)htAtomMap.get(atom1);
    if (atomShape1 == null)
      return;
    AtomShape atomShape2 = (AtomShape)htAtomMap.get(atom2);
    if (atomShape2 == null)
      return;
    atomShape1.bondMutually(atomShape2, order, control);
  }

  public void bondAtomShapes(AtomShape atomShape1, JmolAtom atom2, int order) {
    if (htAtomMap == null)
      initAtomMap();
    AtomShape atomShape2 = (AtomShape)htAtomMap.get(atom2);
    if (atomShape2 == null)
      return;
    atomShape1.bondMutually(atomShape2, order, control);
  }

  public void bondAtomShapes(AtomShape atomShape1, AtomShape atomShape2, int order) {
    atomShape1.bondMutually(atomShape2, order, control);
  }

  private Point3d centerBoundingBox;
  private Point3d cornerBoundingBox;
  private Point3d centerRotation;
  private double radiusBoundingBox;
  private double radiusRotation;
  private double minAtomVectorMagnitude;
  private double maxAtomVectorMagnitude;
  private double atomVectorRange;

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

  public double getMinAtomVectorMagnitude() {
    findBounds();
    return minAtomVectorMagnitude;
  }

  public double getMaxAtomVectorMagnitude() {
    findBounds();
    return maxAtomVectorMagnitude;
  }

  public double getAtomVectorRange() {
    findBounds();
    return maxAtomVectorMagnitude - minAtomVectorMagnitude;
  }

  public void clearBounds() {
    centerBoundingBox = centerRotation = null;
    radiusBoundingBox = radiusRotation =
      minAtomVectorMagnitude = maxAtomVectorMagnitude = atomVectorRange = 0f;
  }

  private void findBounds() {
    if ((centerBoundingBox != null) || (atomShapes == null) || (atomShapeCount <= 0))
      return;
    calcBoundingBox();
    centerRotation = centerBoundingBox;
    calculateAtomVectorMagnitudeRange();
    radiusBoundingBox = radiusRotation = calcRadius(centerBoundingBox);
  }

  void calculateAtomVectorMagnitudeRange() {
    /*
    minAtomVectorMagnitude = maxAtomVectorMagnitude = -1;
    for (int i = 0; i < atomShapeCount; ++i) {
      AtomShape atom = atomShapes[i];
      if (!atom.hasVector())
        continue;
      double magnitude=atom.getVectorMagnitude();
      if (magnitude > maxAtomVectorMagnitude) {
        maxAtomVectorMagnitude = magnitude;
      }
      if ((magnitude < minAtomVectorMagnitude) ||
          (minAtomVectorMagnitude == -1)) {
        minAtomVectorMagnitude = magnitude;
      }
    }
    atomVectorRange = maxAtomVectorMagnitude - minAtomVectorMagnitude;
    */
  }

  private void calcBoundingBox() {
    /**
     * Note that this method is overridden by CrystalFrame
     */
    // bounding box is defined as the center of the cartesian coordinates
    // as stored in the file
    // Note that this is not really the geometric center of the molecule
    // ... for this we would need to do a Minimal Enclosing Sphere calculation
    Point3d position = atomShapes[0].getPoint3d();
    double minX = position.x, maxX = minX;
    double minY = position.y, maxY = minY;
    double minZ = position.z, maxZ = minZ;

    for (int i = 1; i < atomShapeCount; ++i) {
      position = atomShapes[i].getPoint3d();
      double x = position.x;
      if (x < minX) { minX = x; }
      if (x > maxX) { maxX = x; }
      double y = position.y;
      if (y < minY) { minY = y; }
      if (y > maxY) { maxY = y; }
      double z = position.z;
      if (z < minZ) { minZ = z; }
      if (z > maxZ) { maxZ = z; }
    }
    centerBoundingBox = new Point3d((minX + maxX) / 2,
                                    (minY + maxY) / 2,
                                    (minZ + maxZ) / 2);
    cornerBoundingBox = new Point3d(maxX, maxY, maxZ);
    cornerBoundingBox.sub(centerBoundingBox);
  }

  private double calcRadius(Point3d center) {
    /**
     * Note that this method is overridden by CrystalFrame
     */
    // Now that we have defined the center, find the radius to the outermost
    // atom, including the radius of the atom itself. Note that this is
    // currently the vdw radius as scaled by the vdw display radius as set
    // in preferences. This is *not* recalculated if the user changes the
    // display scale ... perhaps it should be.
    // Atom Vectors should be included in this calculation so they don't get
    // clipped off the screen during rotations ... but I don't understand
    // them yet ... so they are not included. samples/cs2.xyz has atom vectors
    //
    // examples of crystal vectors samples/estron.cml samples/bulk_Si.in
    double radius = 0.0f;
    //    double atomSphereFactor = control.getPercentVdwAtom() / 100.0;
    for (int i = atomShapeCount; --i >= 0; ) {
      AtomShape atomShape = atomShapes[i];
      Point3d posAtom = atomShape.getPoint3d();
      double distAtom = center.distance(posAtom);
      double radiusVdw = atomShape.getVanderwaalsRadius();
      //      double distVdw = distAtom + (radiusVdw * atomSphereFactor);
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
    return radius;
  }

  public void render(Graphics25D g25d, DisplayControl control) {
    if (atomShapeCount <= 0)
      return;
    if (shapes == null || control.hasStructuralChange()) {
      AtomShape[] atomShapes = this.atomShapes;
      Shape[] vectorShapes = this.vectorShapes;
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
        atomShapeCount + vectorShapeCount + axisShapeCount + bboxShapeCount;
      shapes = new Shape[shapeCount];
      int i = 0;

      System.arraycopy(atomShapes, 0, shapes, i, atomShapeCount);
      i += atomShapeCount;

      if (vectorShapes != null) {
        System.arraycopy(vectorShapes, 0, shapes, i, vectorShapeCount);
        i += vectorShapeCount;
      }
      if (axisShapes != null) {
        System.arraycopy(axisShapes, 0, shapes, i, axisShapeCount);
        i += axisShapeCount;
      }
      if (bboxShapes != null) {
        System.arraycopy(bboxShapes, 0, shapes, i, bboxShapeCount);
        i += bboxShapeCount;
      }

      /*
      if (frame instanceof CrystalFrame) {
        CrystalFrame crystalFrame = (CrystalFrame) frame;
        double[][] rprimd = crystalFrame.getRprimd();
        
        // The three primitives vectors with arrows
        for (int i = 0; i < 3; i++) {
          VectorShape vector = new VectorShape(zeroPoint,
              new Point3d(rprimd[i][0], rprimd[i][1], rprimd[i][2]), false,
                true);
          shapesVector.addElement(vector);
        }
        
        // The full primitive cell
        if (true) {
          // Depends on the settings...TODO
          Vector boxEdges = crystalFrame.getBoxEdges();
          for (int i = 0; i < boxEdges.size(); i = i + 2) {
            LineShape line =
              new LineShape((Point3d) boxEdges.elementAt(i),
                            (Point3d) boxEdges.elementAt(i + 1));
            shapesVector.addElement(line);
          }
        }
      }
      */
    }

    control.calcViewTransformMatrix();
    for (int i = 0; i < shapes.length; ++i) {
      shapes[i].transform(control);
    }
    
    if (control.jvm12orGreater) {
      UseJavaSort.sortShapes(shapes);
    } else {
      NonJavaSort.sortShapes(shapes);
    }

    if (! control.getGraphics25DEnabled()) {
      boolean slabEnabled = control.getSlabEnabled();
      int slabValue = control.getSlabValue();
      for (int i = 0; i < shapes.length; ++i) {
        if (slabEnabled) {
          if (shapes[i].z > slabValue)
            continue;
        }
        shapes[i].render(g25d, control);
      }
    } else {
      for (int i = shapes.length; --i >= 0 ; ) {
        shapes[i].render(g25d, control);
      }
    }

    // measures probably should be dealt with just like other shapes
    // but just leave it for now
    if (control.getShowMeasurements())
      control.measureRenderer.render(g25d, control);
  }

  private Shape[] shapes = null;


  private final static int vectorGrowthIncrement = 16;
  private int vectorShapeCount = 0;
  private Shape[] vectorShapes = null;

  public void addVectorShape(Shape shape) {
    if (vectorShapes == null || vectorShapeCount == vectorShapes.length) {
      Shape[] newShapes = new Shape[vectorShapeCount + vectorGrowthIncrement];
      if (vectorShapes != null)
        System.arraycopy(vectorShapes, 0, newShapes, 0, vectorShapeCount);
      vectorShapes = newShapes;
    }
    vectorShapes[vectorShapeCount++] = shape;
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

}
