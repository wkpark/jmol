/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public abstract class PdbStructure {

  PdbPolymer polymer;
  byte type;
  int polymerIndex;
  int polymerCount;
  Point3f center;
  Point3f axisA, axisB;
  Vector3f axisUnitVector;
  Point3f[] segments;

  PdbStructure(PdbPolymer polymer, byte type,
               int polymerIndex, int polymerCount) {
    this.polymer = polymer;
    this.type = type;
    this.polymerIndex = polymerIndex;
    this.polymerCount = polymerCount;
  }

  void calcAxis() {
  }

  void calcSegments() {
    if (segments != null)
      return;
    calcAxis();
    /*
    System.out.println("axisA=" + axisA.x + "," + axisA.y + "," + axisA.z);
    System.out.println("axisB=" + axisB.x + "," + axisB.y + "," + axisB.z);
    */
    segments = new Point3f[polymerCount + 1];
    segments[polymerCount] = axisB;
    segments[0] = axisA;
    for (int i = polymerCount; --i > 0; ) {
      Point3f point = segments[i] = new Point3f();
      polymer.getAlphaCarbonMidPoint(polymerIndex + i, point);
      projectOntoAxis(point);
    }
    for (int i = 0; i < segments.length; ++i) {
      Point3f point = segments[i];
      /*
      System.out.println("segment[" + i + "]=" +
                         point.x + "," + point.y + "," + point.z);
      */
    }
  }

  boolean lowerNeighborIsHelixOrSheet() {
    if (polymerIndex == 0)
      return false;
    return polymer.groups[polymerIndex - 1].isHelixOrSheet();
  }

  boolean upperNeighborIsHelixOrSheet() {
    int upperNeighborIndex = polymerIndex + polymerCount;
    if (upperNeighborIndex == polymer.count)
      return false;
    return polymer.groups[upperNeighborIndex].isHelixOrSheet();
  }

  final Vector3f vectorProjection = new Vector3f();

  void projectOntoAxis(Point3f point) {
    // assumes axisA, axisB, and axisUnitVector are set;
    vectorProjection.sub(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.set(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
  }

  public int getPolymerCount() {
    return polymerCount;
  }

  public int getPolymerIndex() {
    return polymerIndex;
  }

  public int getIndex(PdbGroup group) {
    PdbGroup[] groups = polymer.groups;
    int i;
    for (i = polymerCount; --i >= 0; )
      if (groups[polymerIndex + i] == group)
        break;
    return i;
  }

  public Point3f[] getSegments() {
    if (segments == null)
      calcSegments();
    return segments;
  }

  public Point3f getAxisStartPoint() {
    calcAxis();
    return axisA;
  }

  public Point3f getAxisEndPoint() {
    calcAxis();
    return axisB;
  }

  public Point3f getStructureMidPoint(int index) {
    if (segments == null)
      calcSegments();
    /*
    Point3f point = segments[residueIndex - startResidueIndex];
    System.out.println("PdbStructure.getStructureMidpoint(" +
                       residueIndex + ") -> " +
                       point.x + "," + point.y + "," + point.z);
    */
    return segments[index];
  }
}
