/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Helix extends ProteinStructure {

  Helix(AlphaPolymer apolymer, int monomerIndex, int monomerCount) {
    super(apolymer, JmolConstants.PROTEIN_STRUCTURE_HELIX,
          monomerIndex, monomerCount);
  }

  void calcAxis() {
    if (axisA != null)
      return;

    // just a crude starting point.
    
    axisA = new Point3f();
    axisB = new Point3f();
    apolymer.getLeadMidPoint(monomerIndex, axisA);
    apolymer.getLeadMidPoint(monomerIndex + monomerCount, axisB);
    axisUnitVector = new Vector3f();
    axisUnitVector.sub(axisB, axisA);
    axisUnitVector.normalize();

    /*
     * We now calculate the least-squares 3D axis
     * through the helix alpha carbons starting with Vo
     * as a first approximation.
     * 
     * This uses the simple 0-centered least squares fit:
     * 
     * Y = M cross Xi
     * 
     * minimizing R^2 = SUM(|Y - Yi|^2) 
     * 
     * where Yi is the vector PERPENDICULAR of the point onto axis Vo
     * and Xi is the vector PROJECTION of the point onto axis Vo
     * and M is a vector adjustment 
     * 
     * M = SUM_(Xi cross Yi) / sum(|Xi|^2)
     * 
     * from which we arrive at:
     * 
     * V = Vo + (Vo cross M)
     * 
     * Basically, this is just 3D version of a 
     * standard 2D least squares fit to a line, where we would say:
     * 
     * y = m xi + b
     * 
     * D = n (sum xi^2) - (sum xi)^2
     * 
     * m = [(n sum xiyi) - (sum xi)(sum yi)] / D
     * b = [(sum yi) (sum xi^2) - (sum xi)(sum xiyi)] / D
     * 
     * but here we demand that the line go through the center, so we
     * require (sum xi) = (sum yi) = 0, so b = 0 and
     * 
     * m = (sum xiyi) / (sum xi^2)
     * 
     * In 3D we do the same but 
     * instead of x we have Vo,
     * instead of multiplication we use cross products
     * 
     * Bob Hanson 11/2006
     * 
     */
    
    calcCenter();

    axisA.set(center);

    float sum_Xi2 = 0;
    Vector3f sumXiYi = new Vector3f();
    Vector3f vTemp = new Vector3f();
    Point3f pt = new Point3f();
    Point3f ptProj = new Point3f();

    for (int i = monomerIndex + monomerCount; --i >= monomerIndex;) {
      pt.set(apolymer.getLeadPoint(i));
      ptProj.set(pt);
      projectOntoAxis(ptProj);
      vTemp.sub(pt, ptProj);
      vTemp.cross(vectorProjection, vTemp);
      sumXiYi.add(vTemp);
      sum_Xi2 += vectorProjection.lengthSquared();
    }
    Vector3f m = new Vector3f(sumXiYi);
    m.scale(1/sum_Xi2);
    vTemp.cross(m, axisUnitVector);
    axisUnitVector.add(vTemp);
    axisUnitVector.normalize();

    /*
     * done. Only problem is, we can't guarantee the direction of the vector. 
     * So, we now find the projections of the endpoints onto the axis and
     * redefine the axis from those two points.
     * 
     */
    
    Point3f tempA = new Point3f();
    apolymer.getLeadMidPoint(monomerIndex, tempA);
    projectOntoAxis(tempA);
    axisA = tempA;
    
    Point3f tempB = new Point3f();
    apolymer.getLeadMidPoint(monomerIndex + monomerCount, tempB);
    projectOntoAxis(tempB);
    axisB.set(tempB);
    
    //axisUnitVector.sub(axisB, axisA);
    //axisUnitVector.normalize();
  }

  void calcCenter() {
    if (center != null)
      return;
    center = new Point3f();
    for (int i = monomerIndex + monomerCount; --i >= monomerIndex;)
      center.add(apolymer.getLeadPoint(i));
    center.scale(1f / monomerCount);
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

  /* not implemented -- I never got around to reading this -- BH 
   * 

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
  
  */
}
