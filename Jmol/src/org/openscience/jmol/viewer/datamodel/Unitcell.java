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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;

public class Unitcell extends Shape {

  boolean hasUnitcell;
  float a,b,c,alpha,beta,gamma;
  Point3f[] vertices;

  final static Point3f pointOrigin = new Point3f();

  final static float toRadians = (float)Math.PI * 2 / 360;

  public void initShape() {
    float[] notionalUnitcell = frame.notionalUnitcell;
    Matrix3f crystalScaleMatrix = frame.crystalScaleMatrix;
    Vector3f crystalTranslateVector = frame.crystalTranslateVector;
    Matrix3f matrixUnitcellToOrthogonal = frame.matrixUnitcellToOrthogonal;
    dumpCellData(notionalUnitcell, crystalScaleMatrix,
                 crystalTranslateVector, matrixUnitcellToOrthogonal);
    hasUnitcell = notionalUnitcell != null;
    if (hasUnitcell) {
      float a = this.a = notionalUnitcell[0];
      float b = this.b = notionalUnitcell[1];
      float c = this.c = notionalUnitcell[2];
      float alpha = this.alpha = notionalUnitcell[3];
      float beta  = this.beta  = notionalUnitcell[4];
      float gamma = this.gamma = notionalUnitcell[5];

      /* some intermediate variables */
      float cosAlpha = (float)Math.cos(toRadians*alpha);
      float sinAlpha = (float)Math.sin(toRadians*alpha);
      float cosBeta  = (float)Math.cos(toRadians*beta);
      float sinBeta  = (float)Math.sin(toRadians*beta);
      float cosGamma = (float)Math.cos(toRadians*gamma);
      float sinGamma = (float)Math.sin(toRadians*gamma);
    

      // 1. align the a axis with x axis
      Point3f pointA = new Point3f(a, 0, 0);
      // 2. place the b is in xy plane making a angle gamma with a
      Point3f pointB = new Point3f(b * cosGamma, b * sinGamma, 0);
      // 3. now the c axis,
      // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
      float V = a * b * c *
        (float) Math.sqrt(1.0 - cosAlpha*cosAlpha -
                          cosBeta*cosBeta -
                          cosGamma*cosGamma +
                          2.0*cosAlpha*cosBeta*cosGamma);

      Point3f pointC = new Point3f(c * cosBeta,
                                   c * (cosAlpha - cosBeta*cosGamma)/sinGamma,
                                   V/(a * b * sinGamma));

      // 4. the other points

      Point3f pointAB = new Point3f();
      pointAB.add(pointA, pointB);
      Point3f pointAC = new Point3f();
      pointAC.add(pointA, pointC);
      Point3f pointBC = new Point3f();
      pointBC.add(pointB, pointC);
      Point3f pointABC = new Point3f();
      pointABC.add(pointA, pointBC);
      
      vertices = new Point3f[] {
        pointOrigin,
        pointA,
        pointB,
        pointAB,
        pointC,
        pointAC,
        pointBC,
        pointABC
      };
    }
  }

  void dumpCellData(float[] notionalUnitcell,
                    Matrix3f crystalScaleMatrix,
                    Vector3f crystalTranslateVector,
                    Matrix3f matrixUnitcellToOrthogonal) {
    if (notionalUnitcell == null) {
      System.out.println("notional unitcell is null");
      return;
    }
    System.out.print("unitcell:");
    for (int i = 0; i < 6; ++i)
      System.out.print(" " + notionalUnitcell[i]);
    System.out.println("");

    System.out.println("scale matrix:\n" + crystalScaleMatrix);
    System.out.println("translate vector:\n" + crystalTranslateVector);
    System.out.println("inverted matrix:\n" + matrixUnitcellToOrthogonal);
  }
}
