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

import java.util.Arrays;
import java.util.Comparator;

public class Tensor {

  private static float TEMPERATURE_FACTOR = (float) (Math.sqrt(0.5) / Math.PI);
  private static final float MAGNETIC_SUSCEPTIBILITY_FACTOR = 0.01f;
  private static final float BORN_EFFECTIVE_CHARGE_FACTOR = 1f;
  private static final float INTERACTION_FACTOR = 0.04f;

  private static TensorSort tSort;

  private double[][] asymmetricTensor;
    

  public String type; // iso temp ms isc charge TLS-R TLS-U...
  public String altType; // "0" "1" "2"
  public float typeFactor = 1;

  public int modelIndex;
  public int atomIndex1 = -1;
  public int atomIndex2 = -1;

  public V3[] eigenVectors; // possibly null (isotropic)
  public float[] eigenValues;

  public boolean forThermalEllipsoid = true;
  public int eigenSignMask = 7;
  public boolean isIsotropic;

  public static Tensor copyTensor(Tensor t0) {
    Tensor t = new Tensor();
    t.eigenValues = t0.eigenValues;
    t.eigenVectors = t0.eigenVectors;
    t.asymmetricTensor = t0.asymmetricTensor;
    t.type = t0.type;
    t.typeFactor = t0.typeFactor;
    return t;
  }


  public double[][] getAsymmetricTensor() {
    return asymmetricTensor;
  }

  /**
   * all instantiation must go through one of the static getTensor... methods
   * 
   */
  private Tensor() {}
  
  /**
   * 
   * @param asymmetricTensor
   * @param type
   * @return Tensor
   */
  public static Tensor getTensorFromAsymmetricTensor(double[][] asymmetricTensor, String type) {
    double[][] a = new double[3][3];    
    for (int i = 3; --i >= 0;)
      for (int j = 3; --j >= 0;)
        a[i][j] = asymmetricTensor[i][j];
    
    // symmetrize matrix
    if (a [0][1] != a[1][0]) {
      a[0][1] = a[1][0] = (a[0][1] + a[1][0])/2;
    }
    if (a[1][2] != a[2][1]) {
      a[1][2] = a[2][1] = (a[1][2] + a[2][1])/2;
    }
    if (a[0][2] != a[2][0]) {
      a[0][2] = a[2][0] = (a[0][2] + a[2][0])/2;
    }
    Eigen eigen = new Eigen(3);
    eigen.calc(a);
    Matrix3f m = new Matrix3f();
    float[] mm = new float[9];
    for (int i = 0, p = 0; i < 3; i++)
      for (int j = 0; j < 3; j++)
        mm[p++] = (float) a[i][j];
    m.setA(mm);

    V3[] evec = eigen.getEigenVectors3();
    V3 n = new V3();
    V3 cross = new V3();
    for (int i = 0; i < 3; i++) {
      n.setT(evec[i]);
      m.transform(n);
      cross.cross(n, evec[i]);
      //Logger.info("v[i], n, n x v[i]"+ evec[i] + " " + n + " "  + cross);
      n.setT(evec[i]);
       n.normalize();
      cross.cross(evec[i], evec[(i + 1) % 3]);
      //Logger.info("draw id eigv" + i + " " + Escape.eP(evec[i]) + " color " + (i ==  0 ? "red": i == 1 ? "green" : "blue") + " # " + n + " " + cross);
    }
//    Logger.info("eigVal+vec (" + eigen.d[0] + " + " + eigen.e[0]
//        + ")\n             (" + eigen.d[1] + " + " + eigen.e[1]
//        + ")\n             (" + eigen.d[2] + " + " + eigen.e[2] + ")");

    V3[] vectors = new V3[3];
    float[] values = new float[3];
    eigen.fillArrays(vectors, values);
    Tensor t = newTensorType(vectors, values, type);
    t.asymmetricTensor = asymmetricTensor;
    return t;
  }

  public static Tensor getTensorFromEigenVectors(V3[] eigenVectors,
                                            float[] eigenValues, String type) {
    float[] values = new float[3];
    V3[] vectors = new V3[3];
    for (int i = 0; i < 3; i++) {
      vectors[i] = V3.newV(eigenVectors[i]);
      values[i] = eigenValues[i];
    }    
    Tensor t = newTensorType(vectors, values, type);
    t.isIsotropic = "iso".equals(type);
    return t;
  }

  public static Tensor getTensorFromAxes(V3[] axes) {
    Tensor t = new Tensor();
    t.eigenValues = new float[3];
    t.eigenVectors = new V3[3];
    for (int i = 0; i < 3; i++) {
      t.eigenVectors[i] = V3.newV(axes[i]);
      t.eigenValues[i] = axes[i].length();
      if (t.eigenValues[i] == 0)
        return null;
      t.eigenVectors[i].normalize();
    }
    if (Math.abs(t.eigenVectors[0].dot(t.eigenVectors[1])) > 0.0001f
        || Math.abs(t.eigenVectors[1].dot(t.eigenVectors[2])) > 0.0001f 
        || Math.abs(t.eigenVectors[2].dot(t.eigenVectors[0])) > 0.0001f)
      return null;
    sort(t.eigenVectors, t.eigenValues);
    return t;
  }

  public static Tensor getTensorFromThermalEquation(double[] coef) {
    Tensor t = new Tensor();
    t.eigenValues = new float[3];
    t.eigenVectors = new V3[3];
    // assumes an ellipsoid centered on 0,0,0
    // called by UnitCell for the initial creation of Object[] ellipsoid    
    double[][] mat = new double[3][3];
    mat[0][0] = coef[0]; //XX
    mat[1][1] = coef[1]; //YY
    mat[2][2] = coef[2]; //ZZ
    mat[0][1] = mat[1][0] = coef[3] / 2; //XY
    mat[0][2] = mat[2][0] = coef[4] / 2; //XZ
    mat[1][2] = mat[2][1] = coef[5] / 2; //YZ
    Eigen.getUnitVectors(mat, t.eigenVectors, t.eigenValues);
    sort(t.eigenVectors, t.eigenValues);
    t.typeFactor = TEMPERATURE_FACTOR;
    t.type = "temp";
    t.setTypeFactor();
    return t;
  }

  private static Tensor newTensorType(V3[] vectors, float[] values, String type) {
    Tensor t = new Tensor();
    t.eigenValues = values;
    t.eigenVectors = vectors;
    for (int i = 0; i < 3; i++)
      t.eigenVectors[i].normalize();
    sort(t.eigenVectors, t.eigenValues);
    t.setType(type);
    t.eigenSignMask = (t.eigenValues[0] >= 0 ? 1 : 0)
        + (t.eigenValues[1] >= 0 ? 2 : 0) + (t.eigenValues[2] >= 0 ? 4 : 0);
    return t;
  }

  private void setTypeFactor() {
    forThermalEllipsoid = false;
    switch ("iso  temp ms   isc  chargeTLS-UTLS-R".indexOf(type)) {
    //       0    5    10   15   20    25   30
    default: // TLS, other
      typeFactor = 1;
      break;
    case 0: // iso
      forThermalEllipsoid = true;
      typeFactor = 1;
      break;
    case 5: // temp
      forThermalEllipsoid = true;
      typeFactor = TEMPERATURE_FACTOR;
      break;
    case 10: // ms
      typeFactor = MAGNETIC_SUSCEPTIBILITY_FACTOR;
      break;
    case 15: // isc
      typeFactor = INTERACTION_FACTOR;
      break;
    case 20: // charge
      typeFactor = BORN_EFFECTIVE_CHARGE_FACTOR;
      break;
    }
  }

  public Tensor setType(String type) {
    if (this.type == null || type == null)
      this.type = type;
    setTypeFactor();
    return this;
  }

  public float getFactoredValue(int i) {
    float f = Math.abs(eigenValues[i]);
    return (forThermalEllipsoid ? (float) Math.sqrt(f) : f) * typeFactor;
  }

  public void setAtomIndexes(int index1, int index2) {
    atomIndex1 = index1;
    atomIndex2 = index2;
  }

  private static int[] sortOrder = { 1, 0, 2 };

  /**
   * sorts EigenVectors by
   * 
   * and normalize the eigenVectors
   * 
   * @param eigenVectors
   * @param eigenValues
   */
  private static void sort(V3[] eigenVectors, float[] eigenValues) {
    // |sigma_3 - sigma_iso| >= |sigma_1 - sigma_iso| >= |sigma_2 - sigma_iso|
    // first sorted 3 2 1, then 1 and 2 are switched using the sortOrder above.
    Object[][] o = new Object[][] {
        new Object[] { eigenVectors[0], Float.valueOf(eigenValues[0]) },
        new Object[] { eigenVectors[1], Float.valueOf(eigenValues[1]) },
        new Object[] { eigenVectors[2], Float.valueOf(eigenValues[2]) } };
    float sigmaIso = (eigenValues[0] + eigenValues[1] + eigenValues[2]) / 3f;
    Arrays.sort(o, getTensorSort(sigmaIso));
    for (int i = 0; i < 3; i++) {
      eigenValues[i] = ((Float) o[sortOrder[i]][1]).floatValue();
      eigenVectors[i] = (V3) o[sortOrder[i]][0];
      eigenVectors[i].normalize();
    }
  }

  private static Comparator<? super Object[]> getTensorSort(float sigmaIso) {
    if (tSort == null)
      tSort = new TensorSort();
    tSort.sigmaIso = sigmaIso;
    return tSort;
  }

  @Override
  public String toString() {
    return (type + "\n" + (eigenVectors == null ? ""
        + eigenValues[0] : eigenVectors[0] + "\t" + eigenValues[0] + "\t"
        + "\n" + eigenVectors[1] + "\t" + eigenValues[1] + "\t" + "\n"
        + eigenVectors[2] + "\t" + eigenValues[2] + "\t" + "\n"));
  }

}
