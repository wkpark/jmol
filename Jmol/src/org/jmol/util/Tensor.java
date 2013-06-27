/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-04-26 01:52:03 -0500 (Sat, 26 Apr 2008) $
 * $Revision: 9314 $
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

public class Tensor {

  public float[] eigenValues;
  public V3[] eigenVectors;
  public boolean forThermalEllipsoid = true;
  public float scale = 1;
  public float typeFactor = 1;
  public int eigenSignMask = 7;
  private float[] lengths;
  
  private static float ONE_OVER_ROOT2_PI = (float) (Math.sqrt(0.5) / Math.PI);
  
  public Tensor setThermal(double[] coef) {
    forThermalEllipsoid = true;
    eigenValues = new float[3];
    eigenVectors = new V3[3];
    // assumes an ellipsoid centered on 0,0,0
    // called by UnitCell for the initial creation of Object[] ellipsoid    
    double[][] mat = new double[3][3];
    mat[0][0] = coef[0]; //XX
    mat[1][1] = coef[1]; //YY
    mat[2][2] = coef[2]; //ZZ
    mat[0][1] = mat[1][0] = coef[3] / 2; //XY
    mat[0][2] = mat[2][0] = coef[4] / 2; //XZ
    mat[1][2] = mat[2][1] = coef[5] / 2; //YZ
    Eigen.getUnitVectors(mat, eigenVectors, eigenValues);
    typeFactor = ONE_OVER_ROOT2_PI;
    eigenSignMask = 7;
    return this;
  }

  /**
   * 
   * @param eigenVectors  may be null, in which case typeFactor = 1
   * @param eigenValues
   * @param forThermalEllipsoid
   * @param typeFactor
   * @return this tensor
   */
  public Tensor setVectors(V3[] eigenVectors, float[] eigenValues, boolean forThermalEllipsoid, float typeFactor) {
   this.eigenVectors = eigenVectors;
   this.eigenValues = eigenValues;
   this.forThermalEllipsoid = forThermalEllipsoid;
   this.typeFactor = typeFactor;
   eigenSignMask = (eigenValues[0] >= 0 ? 1 : 0) + (eigenValues[1] >= 0 ? 2 : 0) + (eigenValues[2] >= 0 ? 4 : 0);
   return this;
 }
 
  public void setTypeFactor(float f) {
    typeFactor = f;
    lengths = null;
  }
  
  public void setScale(float scale) {
    this.scale = scale;
    lengths = null;
  }

  public float getLength(int i) {
    if (lengths == null)
      setLengths();
    return lengths[i];
  }

  public void setLengths() {
    if (lengths == null)
      lengths = new float[3];
    if (forThermalEllipsoid)
      for (int i = 0; i < lengths.length; i++)
        lengths[i] = (float) (Math.sqrt(Math.abs(eigenValues[i])) * typeFactor * scale);
    else
      for (int i = 0; i < lengths.length; i++)
        lengths[i] = (Math.abs(eigenValues[i]) * typeFactor * scale);
  }
  
  @Override
  public String toString() {
    return (eigenVectors == null ? "" + eigenValues[0] : 
      eigenVectors[0] + "\t" + eigenValues[0] + "\n"
      + eigenVectors[1] + "\t" + eigenValues[1] + "\n"
      + eigenVectors[2] + "\t" + eigenValues[2] + "\n");
  }
  
  //////////  Ellipsoid Code ///////////
  //
  // Bob Hanson, 4/2008
  //
  // several useful methods designed for Jmol
  //
  // but of potentially many uses
  //
  //////////////////////////////////////

  public void rotate(Matrix4f mat) {
    if (eigenVectors != null)
    for (int i = 0; i < 3; i++)
      mat.transformV(eigenVectors[i]);
  }
  
  public static int getOctant(P3 pt) {
    int i = 0;
    if (pt.x < 0)
      i += 1;
    if (pt.y < 0)
      i += 2;
    if (pt.z < 0)
      i += 4;
    return i;
  }

}
