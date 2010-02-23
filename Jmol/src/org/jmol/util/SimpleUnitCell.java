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

/*
 * Bob Hanson 9/2006
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 *
 */
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.jmol.viewer.JmolConstants;


public class SimpleUnitCell {
  
  protected final static float toRadians = (float) Math.PI * 2 / 360;
  protected float a, b, c, alpha, beta, gamma;
  protected boolean isPrimitive;
  protected float[] notionalUnitcell; //6 parameters + 16 matrix items
  protected Matrix4f matrixCartesianToFractional;
  protected Matrix4f matrixFractionalToCartesian;
  protected boolean isPolymer;
  protected boolean isSlab;
  
  public boolean isPolymer() {
    return isPolymer;
  }
  
  public boolean isSlab() {
    return isSlab;
  }
  
  public SimpleUnitCell(float[] notionalUnitcell) {
    setUnitCell(notionalUnitcell);
  }

  public SimpleUnitCell(float a, float b, float c, 
                        float alpha, float beta, float gamma) {
    setUnitCell(new float[] {a, b, c, alpha, beta, gamma });
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
  
  public final float[] getNotionalUnitCell() {
    return notionalUnitcell;
  }
  
  public final float getInfo(int infoType) {
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
    case JmolConstants.INFO_DIMENSIONS:
      return (isPolymer ? 1 : isSlab ? 2 : 3);
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
    if (b == -1) {
      b = c = 1;
      isPolymer = true;
    } else if (c == -1) {
      c = 1;
      isSlab = true;
    }
    alpha = notionalUnitcell[JmolConstants.INFO_ALPHA];
    beta = notionalUnitcell[JmolConstants.INFO_BETA];
    gamma = notionalUnitcell[JmolConstants.INFO_GAMMA];
    constructFractionalMatrices();
  }

  protected Data data;
  
  protected class Data {
    public double cosAlpha, sinAlpha;
    public double cosBeta, sinBeta;
    public double cosGamma, sinGamma;
    public double volume;
    public double cA_, cB_;
    public double a_;
    public double b_, c_;
    
    public Data() {
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
  }
}
