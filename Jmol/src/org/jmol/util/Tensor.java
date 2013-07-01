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

/**
 * @author Bob Hanson hansonr@stolaf.edu 6/30/2013
 * 
 */
public class Tensor {

  // factors that give reasonable first views of ellipsoids.
  
  private static final float ADP_FACTOR = (float) (Math.sqrt(0.5) / Math.PI);
  private static final float MAGNETIC_SUSCEPTIBILITY_FACTOR = 0.01f;
  private static final float ELECTRIC_FIELD_GRADIENT_FACTOR = 1f;
  private static final float BORN_EFFECTIVE_CHARGE_FACTOR = 1f;
  private static final float INTERACTION_FACTOR = 0.04f;
  
  private static TensorSort tSort; // used for sorting eigenvector/values

  // base data:
  
  public String type;
  public int iType = TYPE_OTHER;
  
  // type is an identifier that the reader/creator delivers:
  //
  // adp    -- crystallographic displacement parameters 
  //           - "erature factors"; t.forThermalEllipsoid = true
  //           - either anisotropic (ADP) or isotropic (IDP)
  // iso      -- isotropic displacement parameters; from org.jmol.symmetry.UnitCell 
  //           - changed to "adp" after setting t.isIsotropic = true
  // ms       -- magnetic susceptibility
  // isc      -- NMR interaction tensors
  //           - will have both atomIndex1 and atomIndex2 defined when
  //           - incorporated into a model
  // charge   -- Born Effective Charge tensor
  // TLS-U    -- Translation/Libration/Skew tensor (anisotropic)
  // TLS-R    -- Translation/Libration/Skew tensor (residual)
  
  private static final String KNOWN_TYPES = ";iso....;adp....;tls-u..;tls-r..;ms.....;efg....;isc....;charge.;";
  private static int getType(String type) {
    int pt = KNOWN_TYPES.indexOf(";" + type.toLowerCase() + ".");
    return (pt < 0 ? TYPE_OTHER : pt / 8); 
  }

  // these may be augmented, but the order should be kept the same within this list 
  // no types  < -1, because these are used in Ellipsoids.getAtomState() as bs.get(iType + 1)
  
  public static final int TYPE_OTHER  = -1;
  public static final int TYPE_ISO    = 0;
  public static final int TYPE_ADP   = 1;
  public static final int TYPE_TLS_U  = 2;
  public static final int TYPE_TLS_R  = 3;
  public static final int TYPE_MS     = 4;
  public static final int TYPE_EFG    = 5;
  public static final int TYPE_ISC    = 6;
  public static final int TYPE_CHARGE = 7;

  public double[][] asymTensor;
  public double[][] symTensor;    
  public V3[] eigenVectors;
  public float[] eigenValues;

  // derived type-based information, Jmol-centric, for rendering:
  
  public String altType; // "0" "1" "2"

  // altType is somewhat of a legacy - just lets you use 
  
  //  ellipsoid SET 1
  //  ellipsoid SET 2
  //   etc...
  
  public boolean isIsotropic; // just rendered as balls, not special features
  public boolean forThermalEllipsoid;
  public int eigenSignMask = 7; // signs of eigenvalues; bits 2,1,0 set to 1 if > 0
  private float typeFactor = 1; // an ellipsoid scaling factor depending upon type

  // added only after passing
  // the tensor to ModelLoader:
  
  public int modelIndex;
  public int atomIndex1 = -1;
  public int atomIndex2 = -1;

  /**
   * returns an object of the specified type, including 
   * "eigenvalues", "eigenvectors", "asymmetric", 
   * "symmetric", "trace", "indices", and "type"
   * 
   * @param infoType
   * @return Object or null
   */
  public Object getInfo(String infoType) {
    if (infoType.charAt(0) != ';')
      infoType = ";" + infoType + ".";
    switch ((";............."
           + ";eigenvalues.."
           + ";eigenvectors."
           + ";asymmetric..."
           + ";symmetric...."
           + ";trace........"
           + ";haeberlen...."
           + ";eulerzyz....."
           + ";eulerzxz....."
           + ";indices......"
           + ";type........."
           ).indexOf(infoType) / 14) {
    case 1:
      return eigenValues;
    case 2:
      P3[] list = new P3[3];
      for (int i = 0; i < 3; i++)
        list[i] = P3.newP(eigenVectors[i]);
      return list;
    case 3:
      if (asymTensor == null)
        return null;
      float[][] a = new float[3][3];
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
          a[i][j] = (float) asymTensor[i][j];
      return a;
    case 4:
      if (symTensor == null)
        return null;
      float[][] b = new float[3][3];
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
          b[i][j] = (float) symTensor[i][j];
      return b;
    case 5: // trace
      return Float.valueOf(eigenValues[0] + eigenValues[1] + eigenValues[2]);
    case 6: // haeberlen
      float[] haeb = new float[3];
      haeb[0] = ((eigenValues[0] + eigenValues[1] + eigenValues[2])/3.0f);
      haeb[1] = (eigenValues[2] - (eigenValues[0]+eigenValues[1])/2.0f);
      if (haeb[1] != 0.0f)
        haeb[2] = ((eigenValues[1]-eigenValues[0])/(eigenValues[2]-haeb[0]));
      else
        haeb[2] = 0.0f;
      return haeb;
    case 7: // eulerzyz
      // TODO
      return null;
    case 8: // eulerzxz
      // TODO
      return null;
    case 9: // 
      return new int[] {modelIndex, atomIndex1, atomIndex2};
    case 10:
      return type;
    default:
      return null; 
    }
  }

  public static Tensor copyTensor(Tensor t0) {
    Tensor t = new Tensor();
    t.setType(t0.type);
    t.eigenValues = t0.eigenValues;
    t.eigenVectors = t0.eigenVectors;
    t.asymTensor = t0.asymTensor;
    t.symTensor = t0.symTensor;
    t.eigenSignMask = t0.eigenSignMask;
    t.modelIndex = t0.modelIndex;
    t.atomIndex1 = t0.atomIndex1;
    t.atomIndex2 = t0.atomIndex2;
    return t;
  }

  /**
   * private constructor so that all instantiation must go through one 
   * of the static getTensor... methods to set fields properly.
   * 
   */
  private Tensor() {}
  
  /**
   * Standard constructor for QM tensors
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
    t.asymTensor = asymmetricTensor;
    t.symTensor = a;
    return t;
  }

  /**
   * Standard constructor for charge and iso 
   * 
   * @param eigenVectors
   * @param eigenValues
   * @param type
   * @return Tensor
   */
  public static Tensor getTensorFromEigenVectors(V3[] eigenVectors,
                                            float[] eigenValues, String type) {
    float[] values = new float[3];
    V3[] vectors = new V3[3];
    for (int i = 0; i < 3; i++) {
      vectors[i] = V3.newV(eigenVectors[i]);
      values[i] = eigenValues[i];
    }    
    return newTensorType(vectors, values, type);
  }

  /**
   * Standard constructor for ellipsoids based on axes 
   * 
   * @param axes
   * @return Tensor
   */
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
    sortAndNormalize(t.eigenVectors, t.eigenValues);
    return t.setType("other");
  }

  /**
   * standard constructor for thermal ellipsoids convention beta
   * (see http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html)
   * 
   * @param coefs
   * @return Tensor
   */
  public static Tensor getTensorFromThermalEquation(double[] coefs) {
    Tensor t = new Tensor();
    t.eigenValues = new float[3];
    t.eigenVectors = new V3[3];
    // assumes an ellipsoid centered on 0,0,0
    // called by UnitCell for the initial creation from PDB/CIF ADP data    
    double[][] mat = new double[3][3];
    mat[0][0] = coefs[0]; //XX
    mat[1][1] = coefs[1]; //YY
    mat[2][2] = coefs[2]; //ZZ
    mat[0][1] = mat[1][0] = coefs[3] / 2; //XY
    mat[0][2] = mat[2][0] = coefs[4] / 2; //XZ
    mat[1][2] = mat[2][1] = coefs[5] / 2; //YZ
    Eigen.getUnitVectors(mat, t.eigenVectors, t.eigenValues);
    sortAndNormalize(t.eigenVectors, t.eigenValues);
    t.typeFactor = ADP_FACTOR;
    return t.setType("adp");
  }

  /**
   * Note that type may be null here to skip type initialization
   * and allow later setting of type; this should be used with care.
   * 
   * @param type
   * @return "this" for convenience only
   */
  public Tensor setType(String type) {
    if (this.type == null || type == null)
      this.type = type;
    if (type != null)
      processType();
    return this;
  }

  /**
   * Returns a factored eigenvalue; thermal ellipsoids use sqrt(abs(eigenvalue)) for
   * ellipsoid axes; others use just use abs(eigenvalue); all cases get factored by
   * typeFactor
   * 
   * @param i
   * @return factored eigenvalue
   */
  public float getFactoredValue(int i) {
    float f = Math.abs(eigenValues[i]);
    return (forThermalEllipsoid ? (float) Math.sqrt(f) : f) * typeFactor;
  }

  public void setAtomIndexes(int index1, int index2) {
    atomIndex1 = index1;
    atomIndex2 = index2;
  }

  public boolean isSelected(BS bsSelected) {
    return bsSelected.get(atomIndex1) && (atomIndex2 < 0 || bsSelected.get(atomIndex2));
  }

  /**
   * common processing of eigenvectors.
   * 
   * @param vectors
   * @param values
   * @param type
   * @return Tensor
   */
  private static Tensor newTensorType(V3[] vectors, float[] values, String type) {
    Tensor t = new Tensor();
    t.eigenValues = values;
    t.eigenVectors = vectors;
    for (int i = 0; i < 3; i++)
      t.eigenVectors[i].normalize();
    sortAndNormalize(t.eigenVectors, t.eigenValues);
    t.setType(type);
    t.eigenSignMask = (t.eigenValues[0] >= 0 ? 1 : 0)
        + (t.eigenValues[1] >= 0 ? 2 : 0) + (t.eigenValues[2] >= 0 ? 4 : 0);
    return t;
  }

  /**
   * Sets typeFactor, altType, isIsotropic, forThermalEllipsoid;
   * type "iso" changed to "" here.
   * 
   */
  private void processType() {
    
    forThermalEllipsoid = false;
    isIsotropic = false;
    altType = null;
    typeFactor = 1;
    
    switch (iType = getType(type)) {
    case TYPE_ISO:
      forThermalEllipsoid = true;
      isIsotropic = true;
      altType = "1";
      type = "adp";
      break;
    case TYPE_ADP:
      forThermalEllipsoid = true;
      typeFactor = ADP_FACTOR;
      altType = "1";
      break;
    case TYPE_MS:
      typeFactor = MAGNETIC_SUSCEPTIBILITY_FACTOR;
      break;
    case TYPE_EFG:
      typeFactor = ELECTRIC_FIELD_GRADIENT_FACTOR;
      break;
    case TYPE_ISC:
      typeFactor = INTERACTION_FACTOR;
      break;
    case TYPE_CHARGE:
      typeFactor = BORN_EFFECTIVE_CHARGE_FACTOR;
      break;
    case TYPE_TLS_R:
      altType = "2";
      break;
    case TYPE_TLS_U:
      altType = "3";
      break;
    }
  }

  private static int[] sortOrder = { 1, 0, 2 };

  /**
   * sorts EigenVectors by 
   * 
   * |sigma_3 - sigma_iso| >= |sigma_1 - sigma_iso| >= |sigma_2 - sigma_iso|
   * 
   * 
   * @param eigenVectors
   * @param eigenValues
   */
  private static void sortAndNormalize(V3[] eigenVectors, float[] eigenValues) {
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
