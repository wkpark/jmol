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
package org.openscience.jmol.viewer.pdb;

import org.openscience.jmol.viewer.JmolConstants;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class Helix extends PdbStructure {

  Helix(PdbPolymer polymer, int polymerIndex, int polymerCount) {
    super(polymer, JmolConstants.SECONDARY_STRUCTURE_HELIX,
          polymerIndex, polymerCount);
  }

  // copied from sheet -- not correct
  void calcAxis() {
    if (axisA != null)
      return;
    axisA = new Point3f();
    polymer.getAlphaCarbonMidPoint(polymerIndex + 1, axisA);
    axisB = new Point3f();
    polymer.getAlphaCarbonMidPoint(polymerIndex + polymerCount - 1, axisB);

    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    Point3f tempA = new Point3f();
    polymer.getAlphaCarbonMidPoint(polymerIndex, tempA);
    projectOntoAxis(tempA);
    Point3f tempB = new Point3f();
    polymer.getAlphaCarbonMidPoint(polymerIndex + polymerCount, tempB);
    projectOntoAxis(tempB);
    axisA = tempA;
    axisB = tempB;
  }


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
      int i = polymerIndex + polymerCount - 1;
      center = new Point3f(polymer.getResidueAlphaCarbonPoint(i));
      while (--i >= polymerIndex)
        center.add(polymer.getResidueAlphaCarbonPoint(i));
      center.scale(1f/polymerCount);
      //      System.out.println("structure center is at :" + center);
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
