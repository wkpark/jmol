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

import org.openscience.jmol.viewer.JmolConstants;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Helix extends PdbStructure {

  Helix(PdbChain chain, int startResidueIndex, int residueCount) {
    super(chain, JmolConstants.SECONDARY_STRUCTURE_HELIX,
          startResidueIndex, residueCount);
  }

  // copied from sheet -- not correct
  void calcAxis() {
    if (axisA != null)
      return;
    axisA = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex + 1, axisA);
    axisB = new Point3f();
    chain.getAlphaCarbonMidPoint(endResidueIndex, axisB);

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex, tempA);
    projectOntoAxis(tempA);
    Point3f tempB = new Point3f();
    chain.getAlphaCarbonMidPoint(startResidueIndex + residueCount, tempB);
    projectOntoAxis(tempB);
    axisA = tempA;
    axisB = tempB;
  }


  /*
  void calcAxisFoo() {
    if (axisA != null)
      return;
    calcCenter();
    Point3f[] points = new Point3f[count];
    float[] lengths = new float[count];
    for (int i = count; --i >= 0; ) {
      Point3f point = new Point3f(chain.getResiduePoint(i + startResidueID));
      point.sub(center);
      points[i] = point;
      lengths[i] = length(point);
    }
    calcSums(count, points, lengths);
    calcDirectionCosines();
    Point3f point;
    float length;
    point = points[0];
    length = lengths[0];
    //    axisA = new Point3f(length * cosineX, length * cosineY, length * cosineZ);
    axisA = center;
    point = points[count - 1];
    length = lengths[count - 1];
    axisB = new Point3f(length * cosineX, length * cosineY, length * cosineZ);
    axisB.add(center);
  }
  */

  /****************************************************************
   * see:
   * Defining the Axis of a Helix
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 185-189, 1989
   *
   * Simple Methods for Computing the Least Squares Line
   * in Three Dimensions
   * Peter C Kahn
   * Computers Chem. Vol 13, No 3, pp 191-195, 1989
   ****************************************************************/

  void calcCenter() {
    if (center == null) {
      center = new Point3f(chain.getResidueAlphaCarbonPoint(endResidueIndex));
      for (int i = startResidueIndex; i < endResidueIndex; ++i)
        center.add(chain.getResidueAlphaCarbonPoint(i));
      center.scale(1f/residueCount);
      System.out.println("structure center is at :" + center);
    }
  }

  static float length(Point3f point) {
    return
      (float)Math.sqrt(point.x*point.x + point.y*point.y + point.z*point.z);
  }

  float sumXiLi, sumYiLi, sumZiLi;
  void calcSums(int count, Point3f[] points, float[] lengths) {
    sumXiLi = sumYiLi = sumZiLi = 0;
    for (int i = count; --i >= 0; ) {
      Point3f point = points[i];
      float length = lengths[i];
      sumXiLi += point.x * length;
      sumYiLi += point.y * length;
      sumZiLi += point.z * length;
    }
  }

  float cosineX, cosineY, cosineZ;
  void calcDirectionCosines() {
    float denominator =
      (float)Math.sqrt(sumXiLi*sumXiLi + sumYiLi*sumYiLi + sumZiLi*sumZiLi);
    cosineX = sumXiLi / denominator;
    cosineY = sumYiLi / denominator;
    cosineZ = sumZiLi / denominator;
  }

}
