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
package org.openscience.jmol.viewer.protein;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public abstract class PdbStructure {

  PdbChain chain;
  byte type;
  int residueStart;
  int residueEnd;
  int count;
  Point3f center;
  Point3f axisA, axisB;
  Vector3f axisUnitVector;
  Point3f[] segments;

  PdbStructure(PdbChain chain, byte type, int residueStart, int residueEnd) {
    this.chain = chain;
    this.type = type;
    this.residueStart = residueStart;
    this.residueEnd = residueEnd;
    this.count = residueEnd - residueStart + 1;
  }

  void calcAxis() {
  }

  void calcSegments() {
    if (segments != null)
      return;
    calcAxis();
    segments = new Point3f[count + 1];
    segments[count] = axisB;
    segments[0] = axisA;
    for (int i = 1; i < count; ++i) {
      Point3f point = segments[i] = new Point3f();
      chain.getAlphaCarbonMidPoint(residueStart + i, point);
      projectOntoAxis(point);
    }
  }

  final Vector3f vectorProjection = new Vector3f();

  void projectOntoAxis(Point3f point) {
    // assumes axisA, axisB, and axisUnitVector are set;
    vectorProjection.sub(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.set(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
  }

  public int getResidueCount() {
    return count;
  }

  public int getResidueNumberStart() {
    return residueStart;
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

}
