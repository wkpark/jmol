/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-22 07:48:05 -0500 (Tue, 22 May 2007) $
 * $Revision: 7806 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jmol.api.JmolStateCreator;
import org.jmol.modelset.Atom;
import org.jmol.shape.AtomShape;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
//import org.jmol.util.Eigen;
import org.jmol.util.Escape;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.SB;
import org.jmol.util.V3;


public class Ellipsoids extends AtomShape {

  public Map<String, Ellipsoid> htEllipsoids = new Hashtable<String, Ellipsoid>();
  public boolean haveEllipsoids;
  public short[][] colixset;
  byte[][] paletteIDset;
  public short[][] madset;

  @Override
  public int getIndexFromName(String thisID) {
    return ((ellipsoid = htEllipsoids.get(thisID)) == null ? -1 : 1);
  }

  Ellipsoid ellipsoid;
  private int iSelect;

  @Override
  protected void setSize(int size, BS bsSelected) {
    setSize2(size, bsSelected);
    checkSets();
    madset[iSelect] = mads;
    for (int i = bsSelected.nextSetBit(0); i >= 0; i = bsSelected
        .nextSetBit(i + 1)) {
      if (size != 0)
        scaleEllipsoid(atoms[i], size, iSelect);
      boolean isVisible = (madset[0] != null && madset[0].length > i
          && madset[0][i] > 0 || madset[1] != null && madset[1].length > i
          && madset[1][i] > 0 || madset[2] != null && madset[2].length > i
          && madset[2][i] > 0);
      bsSizeSet.setBitTo(i, isVisible);
      atoms[i].setShapeVisibility(myVisibilityFlag, isVisible);
    }
  }
  
  void scaleEllipsoid(Atom atom, int size, int iSelect) {
    Tensor[] tensors = atom.getTensors();
    Tensor t = (tensors != null && iSelect < tensors.length ? tensors[iSelect] : null);
    if (t != null)
      tensors[iSelect].setScale(t.forThermalEllipsoid ? getThermalRadius(size) : size < 1 ? 0 : size / 100.0f);
  }

  // from ORTEP manual ftp://ftp.ornl.gov/pub/ortep/man/pdf/chap6.pdf
  
  private static float[] crtval = new float[] {
    0.3389f, 0.4299f, 0.4951f, 0.5479f, 0.5932f, 0.6334f, 0.6699f, 0.7035f,
    0.7349f, 0.7644f, 0.7924f, 0.8192f, 0.8447f, 0.8694f, 0.8932f, 0.9162f,
    0.9386f, 0.9605f, 0.9818f, 1.0026f, 1.0230f, 1.0430f, 1.0627f, 1.0821f,
    1.1012f, 1.1200f, 1.1386f, 1.1570f, 1.1751f, 1.1932f, 1.2110f, 1.2288f,
    1.2464f, 1.2638f, 1.2812f, 1.2985f, 1.3158f, 1.3330f, 1.3501f, 1.3672f,
    1.3842f, 1.4013f, 1.4183f, 1.4354f, 1.4524f, 1.4695f, 1.4866f, 1.5037f,
    1.5209f, 1.5382f, 1.5555f, 1.5729f, 1.5904f, 1.6080f, 1.6257f, 1.6436f,
    1.6616f, 1.6797f, 1.6980f, 1.7164f, 1.7351f, 1.7540f, 1.7730f, 1.7924f,
    1.8119f, 1.8318f, 1.8519f, 1.8724f, 1.8932f, 1.9144f, 1.9360f, 1.9580f,
    1.9804f, 2.0034f, 2.0269f, 2.0510f, 2.0757f, 2.1012f, 2.1274f, 2.1544f,
    2.1824f, 2.2114f, 2.2416f, 2.2730f, 2.3059f, 2.3404f, 2.3767f, 2.4153f,
    2.4563f, 2.5003f, 2.5478f, 2.5997f, 2.6571f, 2.7216f, 2.7955f, 2.8829f,
    2.9912f, 3.1365f, 3.3682f 
  };
  
  final public static float getThermalRadius(int prob) {
    return crtval[prob < 1 ? 0 : prob > 99 ? 98 : prob - 1];
  }

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {
    if (propertyName == "thisID") {
      ellipsoid = (value == null ? null : (Ellipsoid) htEllipsoids.get(value));
      if (value == null)
        return;
      if (ellipsoid == null) {
        String id = (String) value;
        ellipsoid = new Ellipsoid(id, viewer.getCurrentModelIndex());
        htEllipsoids.put(id, ellipsoid);
        haveEllipsoids = true;
      }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
      while (e.hasNext()) {
        Ellipsoid ellipsoid = e.next();
        if (ellipsoid.modelIndex > modelIndex)
          ellipsoid.modelIndex--;
        else if (ellipsoid.modelIndex == modelIndex)
          e.remove();
      }
      haveEllipsoids = !htEllipsoids.isEmpty();
      ellipsoid = null;
      return;
    }
    if (ellipsoid != null) {
      if ("delete" == propertyName) {
        htEllipsoids.remove(ellipsoid.id);
        haveEllipsoids = !htEllipsoids.isEmpty();
        return;
      }
      if ("modelindex" == propertyName) {
        ellipsoid.modelIndex = ((Integer) value).intValue();
        return;
      }
      if ("on" == propertyName) {
        ellipsoid.isOn = ((Boolean) value).booleanValue();
        return;
      }
      if ("atoms" == propertyName) {
        //setPoints(viewer.modelSet.atoms, (BS) value);
        return;
      }
      if ("points" == propertyName) {
        //Object[] o = (Object[]) value;
        //setPoints((P3[]) o[1], (BS) o[2]);
        return;
      }
      if ("axes" == propertyName) {
        ellipsoid.setAxes((V3[]) value);
        return;
      }
      if ("equation" == propertyName) {
        ellipsoid.setEquation((double[]) value);
        return;
      }
      if ("center" == propertyName) {
        ellipsoid.setCenter((P3) value);
        return;
      }
      if ("scale" == propertyName) {
        ellipsoid.setScale(((Float) value).floatValue());
        return;
      }
      if ("color" == propertyName) {
        ellipsoid.colix = C.getColixO(value);
        return;
      }
      if ("translucentLevel" == propertyName) {
        setPropAS(propertyName, value, bs);
        return;
      }
      if ("translucency" == propertyName) {
        boolean isTranslucent = (value.equals("translucent"));
        ellipsoid.colix = C.getColixTranslucent3(ellipsoid.colix,
            isTranslucent, translucentLevel);
        return;
      }
    }

    if ("select" == propertyName) {
      iSelect = ((Integer) value).intValue() - 1;
      checkSets();
      colixes = colixset[iSelect];
      paletteIDs = paletteIDset[iSelect];
      mads = madset[iSelect];
      return;
    }

    if ("params" == propertyName) {
      Object[] data = (Object[]) value;
      data[2] = null;// Jmol does not allow setting sizes this way from PyMOL yet
      iSelect = 0;
      setSize(50, bs);
      // onward...
    }

    setPropAS(propertyName, value, bs);
    if (colixset != null) {
      if ("color" == propertyName || "translucency" == propertyName
          || "deleteModelAtoms" == propertyName) {
        colixset[iSelect] = colixes;
        paletteIDset[iSelect] = paletteIDs;
        madset[iSelect] = mads;
      }
    }
  }

//  private void setPoints(P3[] points, BS bs) {
//    return;
    // doesn't really work. Just something I was playing with.
//    if (points == null)
//      return;
//    int n = bs.cardinality();
//    if (n < 3)
//      return;
//    P3 ptCenter = new P3();
//    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
//      ptCenter.add(points[i]);
//    ptCenter.scale(1.0f/n);
//    double Sxx = 0, Syy = 0, Szz = 0, Sxy = 0, Sxz = 0, Syz = 0;
//    P3 pt = new P3();
//    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
//      pt.setT(points[i]);
//      pt.sub(ptCenter);
//      Sxx += (double) pt.x * (double) pt.x;
//      Sxy += (double) pt.x * (double) pt.y;
//      Sxz += (double) pt.x * (double) pt.z;
//      Syy += (double) pt.y * (double) pt.y;
//      Szz += (double) pt.z * (double) pt.z;
//      Syz += (double) pt.y * (double) pt.z;      
//    }
//    double[][] N = new double[3][3];
//    N[0][0] = Syy + Szz;
//    N[1][1] = Sxx + Szz;
//    N[2][2] = Sxx + Syy;
//    Eigen eigen = Eigen.newM(N);
//    ellipsoid.setEigen(ptCenter, eigen, 1f / n / 3);
//  }

  private void checkSets() {
    if (colixset == null) {
      colixset = ArrayUtil.newShort2(3);
      paletteIDset = ArrayUtil.newByte2(3);
      madset = ArrayUtil.newShort2(3);
    }
  }

  public static void getEquationForQuadricWithCenter(float x, float y, float z,
                                                     Matrix3f mToElliptical,
                                                     V3 vTemp, Matrix3f mTemp,
                                                     double[] coef,
                                                     Matrix4f mDeriv) {
    /* Starting with a center point and a matrix that converts cartesian 
     * or screen coordinates to ellipsoidal coordinates, 
     * this method fills a float[10] with the terms for the 
     * equation for the ellipsoid:
     * 
     * c0 x^2 + c1 y^2 + c2 z^2 + c3 xy + c4 xz + c5 yz + c6 x + c7 y + c8 z - 1 = 0 
     * 
     * I made this up; I haven't seen it in print. -- Bob Hanson, 4/2008
     * 
     */

    vTemp.set(x, y, z);
    mToElliptical.transform(vTemp);
    double f = 1 - vTemp.dot(vTemp); // J
    mTemp.transposeM(mToElliptical);
    mTemp.transform(vTemp);
    mTemp.mul(mToElliptical);
    coef[0] = mTemp.m00 / f; // A = aXX
    coef[1] = mTemp.m11 / f; // B = aYY
    coef[2] = mTemp.m22 / f; // C = aZZ
    coef[3] = mTemp.m01 * 2 / f; // D = aXY
    coef[4] = mTemp.m02 * 2 / f; // E = aXZ
    coef[5] = mTemp.m12 * 2 / f; // F = aYZ
    coef[6] = -2 * vTemp.x / f; // G = aX
    coef[7] = -2 * vTemp.y / f; // H = aY
    coef[8] = -2 * vTemp.z / f; // I = aZ
    coef[9] = -1; // J = -1

    /*
     * f = Ax^2 + By^2 + Cz^2 + Dxy + Exz + Fyz + Gx + Hy + Iz + J
     * df/dx = 2Ax +  Dy +  Ez + G
     * df/dy =  Dx + 2By +  Fz + H
     * df/dz =  Ex +  Fy + 2Cz + I
     */

    if (mDeriv == null)
      return;
    mDeriv.setIdentity();
    mDeriv.m00 = (float) (2 * coef[0]);
    mDeriv.m11 = (float) (2 * coef[1]);
    mDeriv.m22 = (float) (2 * coef[2]);

    mDeriv.m01 = mDeriv.m10 = (float) coef[3];
    mDeriv.m02 = mDeriv.m20 = (float) coef[4];
    mDeriv.m12 = mDeriv.m21 = (float) coef[5];

    mDeriv.m03 = (float) coef[6];
    mDeriv.m13 = (float) coef[7];
    mDeriv.m23 = (float) coef[8];
  }

  @Override
  public String getShapeState() {
    SB sb = new SB();
    getStateID(sb);
    getStateAtoms(sb);
    return sb.toString();
  }

  private void getStateID(SB sb) {
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    V3 v1 = new V3();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      if (ellipsoid.axes == null || ellipsoid.lengths == null)
        continue;
      sb.append("  Ellipsoid ID ").append(ellipsoid.id).append(" modelIndex ")
          .appendI(ellipsoid.modelIndex).append(" center ").append(
              Escape.eP(ellipsoid.center)).append(" axes");
      for (int i = 0; i < 3; i++) {
        v1.setT(ellipsoid.axes[i]);
        v1.scale(ellipsoid.lengths[i]);
        sb.append(" ").append(Escape.eP(v1));
      }
      sb.append(" " + getColorCommandUnk("", ellipsoid.colix, translucentAllowed));
      if (!ellipsoid.isOn)
        sb.append(" off");
      sb.append(";\n");
    }
  }

  private void getStateAtoms(SB sb) {
    if (madset == null)
      return;
    JmolStateCreator sc = viewer.getStateCreator();
    if (sc == null)
      return;
    for (int ii = 0; ii < 3; ii++) {
      if (madset[ii] == null)
        continue;
      appendCmd(sb, "Ellipsoids set " + (ii + 1) + "\n");
      Map<String, BS> temp = new Hashtable<String, BS>();
      Map<String, BS> temp2 = new Hashtable<String, BS>();
      if (bsSizeSet != null)
        for (int i = bsSizeSet.nextSetBit(0); i >= 0; i = bsSizeSet
            .nextSetBit(i + 1))
          BSUtil.setMapBitSet(temp, i, i, "Ellipsoids " + madset[ii][i]);
      if (bsColixSet != null && colixset[ii] != null)
        for (int i = bsColixSet.nextSetBit(0); i >= 0; i = bsColixSet
            .nextSetBit(i + 1))
          BSUtil.setMapBitSet(temp2, i, i, getColorCommand("Ellipsoids",
              paletteIDset[ii][i], colixset[ii][i], translucentAllowed));
      sb.append(sc.getCommands(temp, temp2, "select"));
    }
  }

  @Override
  public void setVisibilityFlags(BS bs) {
    /*
     * set all fixed objects visible; others based on model being displayed
     *      
     */
    if (!haveEllipsoids)
      return;
    Iterator<Ellipsoid> e = htEllipsoids.values().iterator();
    while (e.hasNext()) {
      Ellipsoid ellipsoid = e.next();
      ellipsoid.visible = ellipsoid.isOn
          && (ellipsoid.modelIndex < 0 || bs.get(ellipsoid.modelIndex));
    }
  }

  /*
   * Just a shape, nothing more.
   * 
   */
  public static class Ellipsoid {

    String id;
    public V3[] axes;
    public float[] lengths;
    public P3 center = P3.new3(0, 0, 0);
    double[] coef;
    public short colix = C.GOLD;
    int modelIndex;
    float scale = 1;
    public boolean visible;
    public boolean isValid;
    boolean isOn = true;

    protected Ellipsoid(String id, int modelIndex) {
      this.id = id;
      this.modelIndex = modelIndex;
    }

    public void setCenter(P3 center) {
      this.center = center;
      updateEquation();
    }

    public void setScale(float scale) {
      if (scale <= 0 || lengths == null) {
        isValid = false;
      } else {
        for (int i = 0; i < 3; i++)
          lengths[i] *= scale / scale;
        this.scale = scale;
        updateEquation();
      }
    }

//    public void setEigen(P3 ptCenter, Eigen eigen, float factor) {
//      center = ptCenter;
//      axes = eigen.getEigenVectors3();
//      double[] v = eigen.getEigenvalues();
//      lengths = new float[3];
//      for (int i = 0; i < 3; i++)
//        lengths[i] = (float) (v[i] * factor);
//      scale = 1;
//      updateEquation();
//    }

    protected void setEquation(double[] coef) {
      Tensor t = new Tensor().setThermal(this.coef = coef);
      axes = t.eigenVectors;
      lengths = new float[] { t.getLength(0), t.getLength(1), t.getLength(2) };
    }

    protected void setAxes(V3[] axes) {
      isValid = false;
      this.axes = axes;
      lengths = new float[3];
      scale = 1;
      for (int i = 0; i < 2; i++) {
        if (axes[i].length() > axes[i + 1].length()) {
          V3 v = axes[i];
          axes[i] = axes[i + 1];
          axes[i + 1] = v;
          if (i == 1)
            i = -1;
        }
      }
      for (int i = 0; i < 3; i++) {
        lengths[i] = axes[i].length();
        if (lengths[i] == 0)
          return;
        axes[i].normalize();
      }
      if (Math.abs(axes[0].dot(axes[1])) > 0.0001f
          || Math.abs(axes[0].dot(axes[1])) > 0.0001f
          || Math.abs(axes[0].dot(axes[1])) > 0.0001f)
        return;
      updateEquation();
    }

    private void updateEquation() {
      if (axes == null || lengths == null)
        return;
      Matrix3f mat = new Matrix3f();
      Matrix3f mTemp = new Matrix3f();
      V3 v1 = new V3();
      coef = new double[10];
      getEquationForQuadricWithCenter(center.x, center.y, center.z, mat, v1,
          mTemp, coef, null);
      isValid = true;
    }

  }

}
