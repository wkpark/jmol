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

package org.jmol.util;

import javajs.util.AU;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.V3;
import javajs.util.T3;



/**
 * general-purpose simple unit cell for calculations 
 * and as a super-class of unitcell, which is only part of Symmetry
 * 
 * allows one-dimensional (polymer) and two-dimensional (slab) 
 * periodicity
 * 
 */

public class SimpleUnitCell {

  protected float[] notionalUnitcell; //6 parameters + optional 16 matrix items
  public M4 matrixCartesianToFractional;
  public M4 matrixFractionalToCartesian;

  protected final static float toRadians = (float) Math.PI * 2 / 360;

  private int na, nb, nc;
  public boolean isSupercell() {
    return (na > 1 || nb > 1 || nc > 1);
  }

  protected float a, b, c, alpha, beta, gamma;
  protected double cosAlpha, sinAlpha;
  protected double cosBeta, sinBeta;
  protected double cosGamma, sinGamma;
  protected double volume;
  protected double cA_, cB_;
  protected double a_;
  protected double b_, c_;
  protected int dimension;

  public static boolean isValid(float[] parameters) {
    return (parameters != null && (parameters[0] > 0 || parameters.length > 14
        && !Float.isNaN(parameters[14])));
  }

  protected SimpleUnitCell() {
  }
  
  public static SimpleUnitCell newA(float[] parameters) {
    SimpleUnitCell c = new SimpleUnitCell();
    c.init(parameters);
    return c;
  }
  
  protected void init(float[] parameters) {
    if (parameters == null)
      parameters = new float[] {1, 1, 1, 90, 90, 90};
    if (!isValid(parameters))
      return;
    notionalUnitcell = AU.arrayCopyF(parameters, parameters.length);

    a = parameters[0];
    b = parameters[1];
    c = parameters[2];
    alpha = parameters[3];
    beta = parameters[4];
    gamma = parameters[5];
    
    // (int) Float.NaN == 0 (but not in JavaScript!)
    na = Math.max(1, parameters.length >= 25 && !Float.isNaN(parameters[22]) ? (int) parameters[22] : 1);
    nb = Math.max(1, parameters.length >= 25 && !Float.isNaN(parameters[23]) ? (int) parameters[23] : 1);
    nc = Math.max(1, parameters.length >= 25 && !Float.isNaN(parameters[24]) ? (int) parameters[24] : 1);

    if (a <= 0) {
      // must calculate a, b, c alpha beta gamma from vectors;
      V3 va = V3.new3(parameters[6], parameters[7], parameters[8]);
      V3 vb = V3.new3(parameters[9], parameters[10], parameters[11]);
      V3 vc = V3.new3(parameters[12], parameters[13], parameters[14]);
      a = va.length();
      b = vb.length();
      c = vc.length();
      if (a == 0)
        return;
      if (b == 0)
        b = c = -1; //polymer
      else if (c == 0)
        c = -1; //slab
      alpha = (b < 0 || c < 0 ? 90 : vb.angle(vc) / toRadians);
      beta = (c < 0 ? 90 : va.angle(vc) / toRadians);
      gamma = (b < 0 ? 90 : va.angle(vb) / toRadians);
      if (c < 0) {
        float[] n = AU.arrayCopyF(parameters, -1);
        if (b < 0) {
          vb.set(0, 0, 1);
          vb.cross(vb, va);
          if (vb.length() < 0.001f)
            vb.set(0, 1, 0);
          vb.normalize();
          n[9] = vb.x;
          n[10] = vb.y;
          n[11] = vb.z;
        }
        if (c < 0) {
          vc.cross(va, vb);
          vc.normalize();
          n[12] = vc.x;
          n[13] = vc.y;
          n[14] = vc.z;
        }
        parameters = n;
      }
    }
    
    a *= na;
    if (b <= 0) {
      b = c = 1;
      dimension = 1;
    } else if (c <= 0) {
      c = 1;
      b *= nb;
      dimension = 2;
    } else {
      b *= nb;
      c *= nc;
      dimension = 3;
    }

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

    if (parameters.length > 21 && !Float.isNaN(parameters[21])) {
      // parameters with a 4x4 matrix
      // [a b c alpha beta gamma m00 m01 m02 m03 m10 m11.... m20...]
      // this is for PDB and CIF reader
      float[] scaleMatrix = new float[16];
      for (int i = 0; i < 16; i++) {
        float f;
        switch (i % 4) {
        case 0:
          f = na;
          break;
        case 1:
          f = nb;
          break;
        case 2:
          f = nc;
          break;
        default:
          f = 1;
          break;
        }
        scaleMatrix[i] = parameters[6 + i] * f;
      }
      
      matrixCartesianToFractional = M4.newA16(scaleMatrix);
      matrixFractionalToCartesian = new M4();
      matrixFractionalToCartesian.invertM(matrixCartesianToFractional);
    } else if (parameters.length > 14 && !Float.isNaN(parameters[14])) {
      // parameters with a 3 vectors
      // [a b c alpha beta gamma ax ay az bx by bz cx cy cz...]
      M4 m = matrixFractionalToCartesian = new M4();
      m.setColumn4(0, parameters[6] * na, parameters[7] * na, parameters[8] * na, 0);
      m.setColumn4(1, parameters[9] * nb, parameters[10] * nb, parameters[11] * nb, 0);
      m.setColumn4(2, parameters[12] * nc, parameters[13] * nc, parameters[14] * nc, 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractional = new M4();
      matrixCartesianToFractional.invertM(matrixFractionalToCartesian);
    } else {
      M4 m = matrixFractionalToCartesian = new M4();
      // 1. align the a axis with x axis
      m.setColumn4(0, a, 0, 0, 0);
      // 2. place the b is in xy plane making a angle gamma with a
      m.setColumn4(1, (float) (b * cosGamma), (float) (b * sinGamma), 0, 0);
      // 3. now the c axis,
      // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
      m.setColumn4(2, (float) (c * cosBeta), (float) (c
          * (cosAlpha - cosBeta * cosGamma) / sinGamma), (float) (volume / (a
          * b * sinGamma)), 0);
      m.setColumn4(3, 0, 0, 0, 1);
      matrixCartesianToFractional = new M4();
      matrixCartesianToFractional.invertM(matrixFractionalToCartesian);
    }
    matrixCtoFANoOffset = matrixCartesianToFractional;
    matrixFtoCNoOffset = matrixFractionalToCartesian;
  }

  protected M4 matrixCtoFANoOffset;
  protected M4 matrixFtoCNoOffset;
  public final static int INFO_DIMENSIONS = 6;
  public final static int INFO_GAMMA = 5;
  public final static int INFO_BETA = 4;
  public final static int INFO_ALPHA = 3;
  public final static int INFO_C = 2;
  public final static int INFO_B = 1;
  public final static int INFO_A = 0;

  /**
   * convenience return only after changing fpt
   * 
   * @param fpt
   * @return adjusted fpt
   */
  public P3 toSupercell(P3 fpt) {
    fpt.x /= na;
    fpt.y /= nb;
    fpt.z /= nc;
    return fpt;
  }

  public final void toCartesian(T3 pt, boolean ignoreOffset) {
    if (matrixFractionalToCartesian != null)
      (ignoreOffset ? matrixFtoCNoOffset : matrixFractionalToCartesian)
          .rotTrans(pt);
  }

  public final void toFractional(T3 pt, boolean isAbsolute) {
    if (matrixCartesianToFractional == null)
      return;
    (isAbsolute ? matrixCtoFANoOffset : matrixCartesianToFractional)
        .rotTrans(pt);
  }

  public boolean isPolymer() {
    return (dimension == 1);
  }

  public boolean isSlab() {
    return (dimension == 2);
  }

  public final float[] getNotionalUnitCell() {
    return notionalUnitcell;
  }

  public final float[] getUnitCellAsArray(boolean vectorsOnly) {
    M4 m = matrixFractionalToCartesian;
    return (vectorsOnly ? new float[] { 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
      } 
      : new float[] { 
        a, b, c, alpha, beta, gamma, 
        m.m00, m.m10, m.m20, // Va
        m.m01, m.m11, m.m21, // Vb
        m.m02, m.m12, m.m22, // Vc
        dimension, (float) volume,
      } 
    );
  }

  public final float getInfo(int infoType) {
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
    case INFO_DIMENSIONS:
      return dimension;
    }
    return Float.NaN;
  }

  /**
   * Expanded cell notation:
   * 
   *   111 - 1000  --> center 5,5,5; range 0 to 9 or -5 to +4
   *    
   *   1000000 - 1999999 --> center 50,50,50; range 0 to 99 or -50 to +49
   *   1000000000 - 1999999999 --> center 500, 500, 500; range 0 to 999 or -500 to +499
   *   
   * @param nnn
   * @param cell
   * @param offset  0 or 1 typically; < 0 means "apply no offset"
   */
  public static void ijkToPoint3f(int nnn, P3 cell, int offset) {
    int f = (nnn > 1000000000 ? 1000 : nnn > 1000000 ? 100 : 10);
    int f2 = f * f;
    offset -= (offset >= 0 ? 5 * f / 10 : offset);
    cell.x = ((nnn / f2) % f) + offset;
    cell.y = (nnn % f2) / f + offset;
    cell.z = (nnn % f) + offset;
  }

}
