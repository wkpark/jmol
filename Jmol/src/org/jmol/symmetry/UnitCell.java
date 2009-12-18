/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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


package org.jmol.symmetry;

/*
 * Bob Hanson 9/2006
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 *
 */
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.BoxInfo;
import org.jmol.util.Quadric;
import org.jmol.viewer.JmolConstants;


//import org.jmol.util.Escape;

class UnitCell {
  
  final static float toRadians = (float) Math.PI * 2 / 360;
  float a, b, c, alpha, beta, gamma;
  boolean isPrimitive;
  float[] notionalUnitcell; //6 parameters + 16 matrix items
  private Matrix4f matrixCartesianToFractional;
  private Matrix4f matrixFractionalToCartesian;
  private Point3f[] vertices; // eight corners

  private Point3f cartesianOffset = new Point3f();
  private Point3f fractionalOffset = new Point3f();
  
  UnitCell(float[] notionalUnitcell) {
    setUnitCell(notionalUnitcell);
  }

  final void toCartesian(Point3f pt) {
    if (matrixFractionalToCartesian == null)
      return;
    matrixFractionalToCartesian.transform(pt);
  }
  
  final void toFractional(Point3f pt) {
    if (matrixCartesianToFractional == null)
      return;
    matrixCartesianToFractional.transform(pt);
  }
  
  private final void toFractionalUnitCell(Point3f pt) {
    if (matrixCartesianToFractional == null)
      return;
    matrixCartesianToFractional.transform(pt);
    pt.x = toFractional(pt.x);
    pt.y = toFractional(pt.y);
    pt.z = toFractional(pt.z);  
  }
  
  private static float toFractional(float x) {
    // introduced in Jmol 11.7.36
    x = (float) (x - Math.floor(x));
    if (x > 0.9999f || x < 0.0001f) 
      x = 0;
    return x;
  }
  
  final void toUnitCell(Point3f pt, Point3f offset) {
    if (matrixCartesianToFractional == null)
      return;
    toFractionalUnitCell(pt);
    // offset is added to regular offset
    if (offset != null)
      pt.add(offset);
    matrixFractionalToCartesian.transform(pt);
  }
  
  void setOffset(Point3f pt) {
    // from "unitcell {i j k}" via uccage
    fractionalOffset.set(pt);
    matrixCartesianToFractional.m03 = -pt.x;
    matrixCartesianToFractional.m13 = -pt.y;
    matrixCartesianToFractional.m23 = -pt.z;
    cartesianOffset.set(pt);
    matrixFractionalToCartesian.m03 = 0;
    matrixFractionalToCartesian.m13 = 0;
    matrixFractionalToCartesian.m23 = 0;
    matrixFractionalToCartesian.transform(cartesianOffset);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    
  }

  void setOffset(int nnn) {
    // from "unitcell ijk" via uccage
    setOffset(ijkToPoint3f(nnn));
  }

  static Point3f ijkToPoint3f(int nnn) {
    Point3f cell = new Point3f();
    cell.x = nnn / 100 - 5;
    cell.y = (nnn % 100) / 10 - 5;
    cell.z = (nnn % 10) - 5;
    return cell;
  }
  
  final String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  Point3f[] getVertices() {
    return vertices; // does not include offsets
  }
  
  Point3f getCartesianOffset() {
    return cartesianOffset;
  }
  
  Point3f getFractionalOffset() {
    return fractionalOffset;
  }
  
  float[] getNotionalUnitCell() {
    return notionalUnitcell;
  }
  
  float getInfo(int infoType) {
    switch (infoType) {
    case JmolConstants.INFO_A:
      return a;
    case JmolConstants.INFO_B:
      return b;
    case JmolConstants.INFO_C:
      return c;
    case JmolConstants.INFO_ALPHA:
      return alpha;
    case JmolConstants.INFO_BETA:
      return beta;
    case JmolConstants.INFO_GAMMA:
      return gamma;
    }
    return Float.NaN;
  }
  
  /// private methods
  
  private void setUnitCell(float[] notionalUnitcell) {
    if (notionalUnitcell == null || notionalUnitcell[0] == 0)
      return;
    this.notionalUnitcell = notionalUnitcell;

    a = notionalUnitcell[JmolConstants.INFO_A];
    b = notionalUnitcell[JmolConstants.INFO_B];
    c = notionalUnitcell[JmolConstants.INFO_C];
    alpha = notionalUnitcell[JmolConstants.INFO_ALPHA];
    beta = notionalUnitcell[JmolConstants.INFO_BETA];
    gamma = notionalUnitcell[JmolConstants.INFO_GAMMA];
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

    final static double twoP2 = 2 * Math.PI * Math.PI;
    
    Object[] getEllipsoid(float[] parBorU) {
      /*
       * 
       * returns {Vector3f[3] unitVectors, float[3] lengths}
       * from J.W. Jeffery, Methods in X-Ray Crystallography, Appendix VI,
       * Academic Press, 1971
       * 
       * comparing with Fischer and Tillmanns, Acta Cryst C44 775-776, 1988,
       * these are really BETA values. Note that

          T = exp(-2 pi^2 (a*b* U11h^2 + b*b* U22k^2 + c*c* U33l^2 
              + 2 a*b* U12hk + 2 a*c* U13hl + 2 b*c* U23kl))

       * (ORTEP type 8) is the same as

          T = exp{-2 pi^2^ sum~i~[sum~j~(U~ij~ h~i~ h~j~ a*~i~ a*~j~)]}

       * http://ndbserver.rutgers.edu/mmcif/dictionaries/html/cif_mm.dic/Items/_atom_site.aniso_u[1][2].html
       * 
       * Ortep: http://www.ornl.gov/sci/ortep/man_pdf.html
       * 
Anisotropic temperature factor Types 0, 1, 2, 3, and 10 use the following formula for the
complete temperature factor.

Base^(-D(b11h2 + b22k2 + b33l2 + cb12hk + cb13hl + cb23kl))

The coefficients bij (i,j = 1,2,3) of the various types are defined with the following constant settings.

Type 0: Base = e, c = 2, D = 1
Type 1: Base = e, c = 1, D = l
Type 2: Base = 2, c = 2, D = l
Type 3: Base = 2, c = 1, D = l

Anisotropic temperature factor Types 4, 5, 8, and 9 use the following formula for the
complete temperature factor, in which a1* , a2*, a3* are reciprocal cell dimensions.

exp[ -D(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + C a1*a2*U12hk + C a1*a3 * U13hl + C a2*a3 * U23kl)]

The coefficients Uij (i,j = 1,2,3) of the various types are defined with the following constant settings.

Type 4: C = 2, D = 1/4
Type 5: C = 1, D = 1/4
Type 8: C = 2, D = 2pi2
Type 9: C = 1, D = 2pi2


For beta, we use definitions at http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html

that betaij = 2pi^2ai*aj* Uij

So if Type 8 is 

exp[ -2pi^2(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + 2a1*a2*U12hk + 2a1*a3 * U13hl + 2a2*a3 * U23kl)]

then we have

exp[ -pi^2(beta11hh + beta22kk + beta33ll + 2beta12hk + 2beta13hl + 2beta23kl)]

and the betaij should be entered as Type 0. 

       */
      
      float[] lengths = new float[6]; // last three are for factored lengths
      if (parBorU[0] == 0) { // this is iso
        lengths[1] = (float) Math.sqrt(parBorU[7]);
        return new Object[] { null, lengths };
      }

      int ortepType = (int) parBorU[6];
      boolean isFractional = (ortepType == 4 || ortepType == 5
          || ortepType == 8 || ortepType == 9);
      double cc = 2 - (ortepType % 2);
      double dd = (ortepType == 8 || ortepType == 9 || ortepType == 10 ? twoP2
          : ortepType == 4 || ortepType == 5 ? 0.25 
          : ortepType == 2 || ortepType == 3 ? Math.log(2)
          : 1 );
      // types 6 and 7 not supported

      //System.out.println("ortep type " + ortepType + " isFractional=" + isFractional + " D = " + dd + " C=" + cc);
      double B11 = parBorU[0] * dd * (isFractional ? a_ * a_ : 1);
      double B22 = parBorU[1] * dd * (isFractional ? b_ * b_ : 1);
      double B33 = parBorU[2] * dd * (isFractional ? c_ * c_ : 1);
      double B12 = parBorU[3] * dd * (isFractional ? a_ * b_ : 1) * cc;
      double B13 = parBorU[4] * dd * (isFractional ? a_ * c_ : 1) * cc;
      double B23 = parBorU[5] * dd * (isFractional ? b_ * c_ : 1) * cc;

      // set bFactor = (U11*U22*U33)
      parBorU[7] = (float) Math.pow(B11 / twoP2 / a_ / a_ * B22 / twoP2
          / b_ / b_ * B33 / twoP2 / c_ / c_, 0.3333);

      double[] Bcart = new double[6];

      Bcart[0] = a * a * B11 + b * b * cosGamma * cosGamma * B22 + c * c
          * cosBeta * cosBeta * B33 + a * b * cosGamma * B12 + b * c * cosGamma
          * cosBeta * B23 + a * c * cosBeta * B13;
      Bcart[1] = b * b * sinGamma * sinGamma * B22 + c * c * cA_ * cA_ * B33
          + b * c * cA_ * sinGamma * B23;
      Bcart[2] = c * c * cB_ * cB_ * B33;
      Bcart[3] = 2 * b * b * cosGamma * sinGamma * B22 + 2 * c * c * cA_
          * cosBeta * B33 + a * b * sinGamma * B12 + b * c
          * (cA_ * cosGamma + sinGamma * cosBeta) * B23 + a * c * cA_ * B13;
      Bcart[4] = 2 * c * c * cB_ * cosBeta * B33 + b * c * cosGamma * B23 + a
          * c * cB_ * B13;
      Bcart[5] = 2 * c * c * cA_ * cB_ * B33 + b * c * cB_ * sinGamma * B23;

      //System.out.println("UnitCell Bcart="+Bcart[0] + " " + Bcart[1] + " " +  Bcart[2] + " " + Bcart[3] + " " + Bcart[4] + " " + Bcart[5]);
      Vector3f unitVectors[] = new Vector3f[3];
      for (int i = 0; i < 3; i++)
        unitVectors[i] = new Vector3f();
        Quadric.getAxesForEllipsoid(Bcart, unitVectors, lengths);

        // note -- this is the ellipsoid in INVERSE CARTESIAN SPACE!

        double factor = Math.sqrt(0.5) / Math.PI;
        for (int i = 0; i < 3; i++)
          lengths[i] = (float) (factor / lengths[i]);
        return new Object[] { unitVectors, lengths };
    }
    
  }
  
  Object[] getEllipsoid(float[] parBorU){
    //returns {Vector3f[3] unitVectors, float[3] lengths}
    if (parBorU == null)
      return null;
    if (data == null)
      data = new Data();
    return data.getEllipsoid(parBorU);
  }

  private void constructFractionalMatrices() {
    if (notionalUnitcell.length > 6 && !Float.isNaN(notionalUnitcell[21])) {
      float[] scaleMatrix = new float[16];
      for (int i = 0; i < 16; i++)
        scaleMatrix[i] = notionalUnitcell[6 + i];
      matrixCartesianToFractional = new Matrix4f(scaleMatrix);
      matrixFractionalToCartesian = new Matrix4f();
      matrixFractionalToCartesian.invert(matrixCartesianToFractional);
    } else if (notionalUnitcell.length > 6 && !Float.isNaN(notionalUnitcell[14])) {
      isPrimitive = true;
      Matrix4f m = matrixFractionalToCartesian = new Matrix4f();
      float[] n = notionalUnitcell;
      if (data == null)
        data = new Data();
      m.setColumn(0, n[6], n[7], n[8], 0);
      m.setColumn(1, n[9], n[10], n[11], 0);
      m.setColumn(2, n[12], n[13], n[14], 0);
      m.setColumn(3, 0, 0, 0, 1);
      matrixCartesianToFractional = new Matrix4f();
      matrixCartesianToFractional.invert(matrixFractionalToCartesian);
    } else {
      Matrix4f m = matrixFractionalToCartesian = new Matrix4f();
      if (data == null)
        data = new Data();
      // 1. align the a axis with x axis
      m.setColumn(0, a, 0, 0, 0);
      // 2. place the b is in xy plane making a angle gamma with a
      m.setColumn(1, (float) (b * data.cosGamma), 
          (float) (b * data.sinGamma), 0, 0);
      // 3. now the c axis,
      // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
      m.setColumn(2, (float) (c * data.cosBeta), 
          (float) (c * (data.cosAlpha - data.cosBeta * data.cosGamma) / data.sinGamma), 
          (float) (data.volume / (a * b * data.sinGamma)), 0);
      m.setColumn(3, 0, 0, 0, 1);
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
      matrixFractionalToCartesian.transform(BoxInfo.unitCubePoints[i], vertices[i]);
    }
  }  
}
