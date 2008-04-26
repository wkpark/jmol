/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 */
//import javax.vecmath.AxisAngle4f;
//import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.util.Quadric;


//import org.jmol.util.Escape;

public class UnitCell {
  
  final static float toRadians = (float) Math.PI * 2 / 360;
  final static Point3f[] unitCubePoints = { new Point3f(0, 0, 0),
      new Point3f(0, 0, 1), new Point3f(0, 1, 0), new Point3f(0, 1, 1),
      new Point3f(1, 0, 0), new Point3f(1, 0, 1), new Point3f(1, 1, 0),
      new Point3f(1, 1, 1), };

  public final static int INFO_A = 0;
  public final static int INFO_B = 1;
  public final static int INFO_C = 2;
  public final static int INFO_ALPHA = 3;
  public final static int INFO_BETA = 4;
  public final static int INFO_GAMMA = 5;
  
  float a, b, c, alpha, beta, gamma;
  float[] notionalUnitcell; //6 parameters + 16 matrix items
  Matrix4f matrixNotional;
  Matrix4f matrixCartesianToFractional;
  Matrix4f matrixFractionalToCartesian;
  Point3f[] vertices; // eight corners

  Point3f cartesianOffset = new Point3f();
  Point3f fractionalOffset = new Point3f();
  
  public UnitCell(float[] notionalUnitcell) {
    setUnitCell(notionalUnitcell);
  }

  public final void toCartesian(Point3f pt) {
    if (matrixFractionalToCartesian == null)
      return;
    matrixFractionalToCartesian.transform(pt);
  }
  
  public final void toFractional(Point3f pt) {
    if (matrixCartesianToFractional == null)
      return;
    matrixCartesianToFractional.transform(pt);
  }
  
  private final void toFractionalUnitCell(Point3f pt) {
    if (matrixCartesianToFractional == null)
      return;
    matrixCartesianToFractional.transform(pt);
    pt.x = (float)(pt.x - Math.floor(pt.x));
    pt.y = (float)(pt.y - Math.floor(pt.y));
    pt.z = (float)(pt.z - Math.floor(pt.z));    
  }
  
  public final void toUnitCell(Point3f pt, Point3f offset) {
    if (matrixCartesianToFractional == null)
      return;
    toFractionalUnitCell(pt);
    if (offset == null)
      offset = fractionalOffset;
    pt.add(offset);
    matrixFractionalToCartesian.transform(pt);
  }
  
  public void setOffset(Point3f pt) {
    // from "unitcell {i j k}" via uccage
    fractionalOffset.set(pt);
    cartesianOffset.set(pt);
    matrixFractionalToCartesian.transform(cartesianOffset);
  }

  public void setOffset(int nnn) {
    // from "unitcell ijk" via uccage
    setOffset(ijkToPoint3f(nnn));
  }

  public static Point3f ijkToPoint3f(int nnn) {
    Point3f cell = new Point3f();
    cell.x = nnn / 100 - 5;
    cell.y = (nnn % 100) / 10 - 5;
    cell.z = (nnn % 10) - 5;
    return cell;
  }
  
  public final String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  public Point3f[] getVertices() {
    return vertices;
  }
  
  public Point3f getCartesianOffset() {
    return cartesianOffset;
  }
  
  public Point3f getFractionalOffset() {
    return fractionalOffset;
  }
  
  public float[] getNotionalUnitCell() {
    return notionalUnitcell;
  }
  
  public float getInfo(int infoType) {
    switch (infoType) {
    case INFO_A:
      return a;
    case INFO_B:
      return b;
    case INFO_C:
      return c;
    case INFO_ALPHA:
      return alpha;
    case INFO_BETA:
      return beta;
    case INFO_GAMMA:
      return gamma;
    }
    return Float.NaN;
  }
  
  /// private methods
  
  private void setUnitCell(float[] notionalUnitcell) {
    if (notionalUnitcell == null || notionalUnitcell[0] == 0)
      return;
    this.notionalUnitcell = notionalUnitcell;

    a = notionalUnitcell[INFO_A];
    b = notionalUnitcell[INFO_B];
    c = notionalUnitcell[INFO_C];
    alpha = notionalUnitcell[INFO_ALPHA];
    beta = notionalUnitcell[INFO_BETA];
    gamma = notionalUnitcell[INFO_GAMMA];
    calcNotionalMatrix();
    constructFractionalMatrices();
    calcUnitcellVertices();
  }

  private Data data;
  
  private class Data {
    double cosAlpha, sinAlpha;
    double cosBeta, sinBeta;
    double cosGamma, sinGamma;
    double volume;
    double cA_, cB_, a_, b_, c_;
    
    Data() {
      cosAlpha = Math.cos(toRadians * alpha);
      sinAlpha = Math.sin(toRadians * alpha);
      cosBeta = Math.cos(toRadians * beta);
      sinBeta = Math.sin(toRadians * beta);
      cosGamma = Math.cos(toRadians * gamma);
      sinGamma = Math.sin(toRadians * gamma);
      double unitVolume = Math.sqrt(sinAlpha * sinAlpha + sinBeta * sinBeta
          + sinGamma * sinGamma + 2.0 * cosAlpha * cosBeta * cosGamma - 2);
      volume = a * b * c * unitVolume;
      // these next few are for the B' calculation
      cA_ = (cosAlpha - cosBeta * cosGamma) / sinGamma;
      cB_ = unitVolume / sinGamma;
      a_ = b * c * sinAlpha / volume;
      b_ = a * c * sinBeta / volume;
      c_ = a * b * sinGamma / volume;
    }

    Object[] getEllipsoid(float[] U) {
      //returns {Vector3f[3] unitVectors, float[3] lengths}
      //from J.W. Jeffery, Methods in X-Ray Crystallography, Appendix VI,
      // Academic Press, 1971
      double twoP2 = 2 * Math.PI * Math.PI;
      double B11 = twoP2 * U[0] * a_ * a_;
      double B22 = twoP2 * U[1] * b_ * b_;
      double B33 = twoP2 * U[2] * c_ * c_;
      double B12 = 2 * twoP2 * U[3] * a_ * b_;
      double B13 = 2 * twoP2 * U[4] * a_ * c_;
      double B23 = 2 * twoP2 * U[5] * b_ * c_;
      
      double[] Bcart = new double[6];
      
      Bcart[0] = a * a * B11 
          + b * b * cosGamma * cosGamma * B22
          + c * c * cosBeta * cosBeta * B33
          + a * b * cosGamma * B12
          + b * c * cosGamma * cosBeta * B23
          + a * c * cosBeta * B13;
      Bcart[1] = b * b * sinGamma * sinGamma * B22
          + c * c * cA_ * cA_ * B33
          + b * c * cA_ * sinGamma * B23;
      Bcart[2] = c * c * cB_ * cB_ * B33;
      Bcart[3] = 2 * b * b * cosGamma * sinGamma * B22 
          + 2 * c * c * cA_ * cosBeta * B33 
          + a * b * sinGamma * B12
          + b * c * (cA_ * cosGamma + sinGamma * cosBeta) * B23
          + a * c * cA_ * B13;
      Bcart[4] = 2 * c * c * cB_ * cosBeta * B33 
          + b * c * cosGamma * B23
          + a * c * cB_ * B13;
      Bcart[5] = 2 * c * c * cA_ * cB_ * B33
          + b * c * cB_ * sinGamma * B23;

      Vector3f unitVectors[] = new Vector3f[3];
      for (int i = 0; i < 3; i++)
        unitVectors[i] = new Vector3f();      
      float[] lengths = new float[3];
      Quadric.getAxesForEllipsoid(Bcart, unitVectors, lengths);

      // note -- this is the ellipsoid in INVERSE CARTESIAN SPACE!
      
      double factor = Math.sqrt(8) / Math.PI;
      for (int i = 0; i < 3; i++)
        lengths[i] = (float) (factor / lengths[i]);
      return new Object[] {unitVectors, lengths};
    }
    
  }
  
  public Object[] getEllipsoid(float[] U){
    //returns {Vector3f[3] unitVectors, float[3] lengths}
    if (U == null)
      return null;
    if (data == null)
      data = new Data();
    return data.getEllipsoid(U);
  }

  private void calcNotionalMatrix() {
    // note that these are oriented as columns, not as row
    // this is because we will later use the transform method,
    // which operates M * P, where P is a column vector
    matrixNotional = new Matrix4f();

    if (data == null)
      data = new Data();

    // 1. align the a axis with x axis
    matrixNotional.setColumn(0, a, 0, 0, 0);
    // 2. place the b is in xy plane making a angle gamma with a
    matrixNotional.setColumn(1, (float) (b * data.cosGamma), 
        (float) (b * data.sinGamma), 0, 0);
    // 3. now the c axis,
    // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
    matrixNotional.setColumn(2, (float) (c * data.cosBeta), 
        (float) (c * (data.cosAlpha - data.cosBeta * data.cosGamma) / data.sinGamma), 
        (float) (data.volume / (a * b * data.sinGamma)), 0);
    matrixNotional.setColumn(3, 0, 0, 0, 1);
  }

  private void constructFractionalMatrices() {
    if (notionalUnitcell.length > 6 && !Float.isNaN(notionalUnitcell[6])) {
        float[] scaleMatrix = new float[16];
      for (int i = 0; i < 16; i++)
        scaleMatrix[i] = notionalUnitcell[6 + i];
      matrixCartesianToFractional = new Matrix4f(scaleMatrix);
      matrixFractionalToCartesian = new Matrix4f();
      matrixFractionalToCartesian.invert(matrixCartesianToFractional);
    } else {
      //System.out.println("notional: "+matrixNotional);
      matrixFractionalToCartesian = matrixNotional;
      matrixCartesianToFractional = new Matrix4f();
      matrixCartesianToFractional.invert(matrixFractionalToCartesian);
    }
    
    /* 
    Point3f v = new Point3f(1,2,3);
    toFractional(v);
    System.out.println("fractionaltocart:" + matrixFractionalToCartesian);
    System.out.println("testing mat.transform [1 2 3]" + matrixCartesianToFractional+v);
    */
  }

  private void calcUnitcellVertices() {
    vertices = new Point3f[8];
    for (int i = 8; --i >= 0;) {
      vertices[i] = new Point3f();
      matrixFractionalToCartesian.transform(unitCubePoints[i], vertices[i]);
    }
  }  
}
