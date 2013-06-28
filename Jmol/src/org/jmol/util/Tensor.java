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

  private static float TEMPERATURE_FACTOR = (float) (Math.sqrt(0.5) / Math.PI);
  private static final float MAGNETIC_SUSCEPTIBILITY_FACTOR = 0.1f;
  private static final float BORN_EFFECTIVE_CHARGE_FACTOR = 1f;
  private static final float INTERACTION_FACTOR = 0.04f;
  
  public String id;
  public String type; // null == > 
  public V3[] eigenVectors; // possibly null (isotropic)
  public float[] eigenValues;
  public int modelIndex;
  public P3 center = P3.new3(0, 0, 0);
  public boolean forThermalEllipsoid = true;
  public float scale = 1;
  public float typeFactor = 1;
  public int eigenSignMask = 7;
  public float[] lengths; // depends upon type
  public double[] coef; // from equation
  public int atomIndex1 = -1;
  public int atomIndex2 = -1;

  public Tensor setThermal(double[] coef) {
    this.coef = coef;
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
    typeFactor = TEMPERATURE_FACTOR;
    type = "temp";
    setTypeFactor();
    eigenSignMask = 7;
    return this;
  }

  /**
   * 
   * @param eigenVectors
   *        may be null, in which case typeFactor = 1
   * @param eigenValues
   * @param type
   * @return this tensor
   */
  public Tensor setVectors(V3[] eigenVectors, float[] eigenValues, String type) {
    this.eigenVectors = eigenVectors;
    this.eigenValues = eigenValues;
    this.type = type;
    setTypeFactor();
    eigenSignMask = (eigenValues[0] >= 0 ? 1 : 0)
        + (eigenValues[1] >= 0 ? 2 : 0) + (eigenValues[2] >= 0 ? 4 : 0);
    return this;
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

  public void setAtomIndexes(int index1, int index2) {
    atomIndex1 = index1;
    atomIndex2 = index2;
  }

  /**
   * set first element to be thermal ellipsoid if TLS is present
   * 
   * @param list
   * @param atomIndex
   * @return somewhat ordered array
   */
  public static Tensor[] getStandardArray(JmolList<Tensor> list, int atomIndex) {
    int  n = list.size();
    for (int i = n; --i >= 0;)
      list.get(i).setAtomIndexes(atomIndex, -1);
    int pt = -1;
    boolean haveTLS = false;
    for (int i = n; --i >= 0;) {
      Tensor t = list.get(i);
      if (t.forThermalEllipsoid)
        pt = i;
      else if (t.type.equals("TLS-U"))
        haveTLS = true;
    }
    Tensor[] a = new Tensor[(pt >= 0 || !haveTLS ? 0 : 1) + n];
    if (pt >= 0) {
      a[0] = list.get(pt);
      if (list.size() == 1)
        return a;
    }
    // back-fills list for TLS:
    // 0 = temp, 1 = TLS-R, 2 = TLS-U
    if (haveTLS) {
      pt = 0;
      for (int i = n; --i >= 0;) {
        Tensor t = list.get(i);
        if (t.forThermalEllipsoid)
          continue;
        a[++pt] = t;
      }
    } else {
      for (int i = 0; i < n; i++)
        a[i] = list.get(i);
    }
    return a;
  }

  @Override
  public String toString() {
    setLengths();
    return (type + " " + eigenVectors == null ? "" + eigenValues[0]
        : eigenVectors[0] + "\t" + eigenValues[0] + "\t" + lengths[0] + "\n" 
        + eigenVectors[1] + "\t" + eigenValues[1] + "\t" + lengths[1] + "\n"
        + eigenVectors[2] + "\t" + eigenValues[2] + "\t" + lengths[2] + "\n");
  }


}
