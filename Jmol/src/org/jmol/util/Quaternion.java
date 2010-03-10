/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-05 09:07:28 -0500 (Thu, 05 Apr 2007) $
 * $Revision: 7326 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

//import javax.vecmath.AxisAngle4f;
//import javax.vecmath.Point3f;
//import javax.vecmath.Quat4f;

/*
 * Standard UNIT quaternion math -- for rotation.
 * 
 * All rotations can be represented as two identical quaternions. 
 * This is because any rotation can be considered from either end of the
 * rotational axis -- either as a + rotation or a - rotation. This code
 * is designed to always maintain the quaternion with a rotation in the
 * [0, PI) range. 
 * 
 * This ensures that the reported theta is always positive, and the normal
 * reported is always associated with a positive theta.  
 * 
 * By Bob Hanson, hansonr@stolaf.edu 6/2008
 * 
 * 
 * 
 */

public class Quaternion {
  public float q0, q1, q2, q3;
  public Matrix3f mat;

  private final static Point4f qZero = new Point4f();
  private final static Quaternion qTemp = new Quaternion(0, 0, 0, 0);
  
  public Quaternion() {
    q0 = 1;
  }

  public Quaternion(Quaternion q) {
    set(q);
  }

  public Quaternion(Tuple3f pt, float theta) {
    set(pt, theta);
  }

  public Quaternion(Matrix3f mat) {
    set(mat);
  }

  public Quaternion(AxisAngle4f a) {
    set(a);
  }

  public Quaternion(Point4f pt) {
    set(pt);
  }

  // create a new object with the given components
  public Quaternion(float q0, float q1, float q2, float q3) {
    this.q0 = q0;
    this.q1 = q1;
    this.q2 = q2;
    this.q3 = q3;
  }

  public void set(Quaternion q) {
    q0 = q.q0;
    q1 = q.q1;
    q2 = q.q2;
    q3 = q.q3;
  }

  /**
   * {x y z w} --> {q1 q2 q3 q0} and factored
   * 
   * @param pt
   */
  private void set(Point4f pt) {
    float factor = (pt == null ? 0 : pt.distance(qZero));
    if (factor == 0) {
      q0 = 1;
      return;
    }
    q0 = pt.w / factor;
    q1 = pt.x / factor;
    q2 = pt.y / factor;
    q3 = pt.z / factor;
  }

  /**
   * q = (cos(theta/2), sin(theta/2) * n)
   * 
   * @param pt
   * @param theta
   */
  public void set(Tuple3f pt, float theta) {
    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      q0 = 1;
      return;
    }
    double fact = (Math.sin(theta / 2 * Math.PI / 180) / Math.sqrt(pt.x
        * pt.x + pt.y * pt.y + pt.z * pt.z));
    q0 = (float) (Math.cos(theta / 2 * Math.PI / 180));
    q1 = (float) (pt.x * fact);
    q2 = (float) (pt.y * fact);
    q3 = (float) (pt.z * fact);
  }

  public void set(AxisAngle4f a) {
    AxisAngle4f aa = new AxisAngle4f(a);
    if (aa.angle == 0)
      aa.y = 1;
    Matrix3f m3 = new Matrix3f();
    m3.set(aa);
    set(m3);
  }

  public void set(Matrix3f mat) {

    /*
     * Changed 7/16/2008 to double precision for 11.5.48.
     * 
     * <quote>
     *  
     * RayTrace Software Package, release 3.0.  May 3, 2006.
     *
     * Mathematics Subpackage (VrMath)
     *
     * Author: Samuel R. Buss
     *
     * Software is "as-is" and carries no warranty.  It may be used without
     *   restriction, but if you modify it, please change the filenames to
     *   prevent confusion between different versions.  Please acknowledge
     *   all use of the software in any publications or products based on it.
     *
     * Bug reports: Sam Buss, sbuss@ucsd.edu.
     * Web page: http://math.ucsd.edu/~sbuss/MathCG
     
     // Use Shepperd's algorithm, which is stable, does not lose
     //    significant precision and uses only one sqrt.
     //   J. Guidance and Control, 1 (1978) 223-224.

     * </quote>
     * 
     * Except, that code has errors.
     * 
     * CORRECTIONS (as noted below) of Quaternion.cpp. I have reported the bug.
     *  
     * -- Bob Hanson
     * 
     *  theory:    
     *         cos(theta/2)^2 = (cos(theta) + 1)/2
     *  and      
     *         trace = (1-x^2)ct + (1-y^2)ct + (1-z^2)ct + 1 = 2cos(theta) + 1
     *  or
     *         cos(theta) = (trace - 1)/2 
     *         
     *  so in general,       
     *       
     *       w = cos(theta/2) 
     *         = sqrt((cos(theta)+1)/2) 
     *         = sqrt((trace-1)/4+1/2)
     *         = sqrt((trace+1)/4)
     *         = sqrt(trace+1)/2
     *     
     *  but there are precision issues, so we allow for other situations.
     *  note -- trace >= 0.5 when cos(theta) >= -0.25 (-104.48 <= theta <= 104.48).
     *  this code cleverly matches the precision in all four options.
     *
     */

    this.mat = mat;
    
    double trace = mat.m00 + mat.m11 + mat.m22;
    double temp;
    double w, x, y, z;
    if (trace >= 0.5) {
      w = Math.sqrt(1.0 + trace);
      x = (mat.m21 - mat.m12) / w;
      y = (mat.m02 - mat.m20) / w;
      z = (mat.m10 - mat.m01) / w;
    } else if ((temp = mat.m00 + mat.m00 - trace) >= 0.5) {
      x = Math.sqrt(1.0 + temp);
      w = (mat.m21 - mat.m12) / x;
      y = (mat.m10 + mat.m01) / x;
      z = (mat.m20 + mat.m02) / x;
    } else if ((temp = mat.m11 + mat.m11 - trace) >= 0.5 
        || mat.m11 > mat.m22) {
      y = Math.sqrt(1.0 + temp);
      w = (mat.m02 - mat.m20) / y;
      x = (mat.m10 + mat.m01) / y;
      z = (mat.m21 + mat.m12) / y;
    } else {
      z = Math.sqrt(1.0 + mat.m22 + mat.m22 - trace);
      w = (mat.m10 - mat.m01) / z;
      x = (mat.m20 + mat.m02) / z; // was -
      y = (mat.m21 + mat.m12) / z; // was -
    }

    q0 = (float) (w * 0.5);
    q1 = (float) (x * 0.5);
    q2 = (float) (y * 0.5);
    q3 = (float) (z * 0.5);

    /*
     *  Originally from http://www.gamedev.net/community/forums/topic.asp?topic_id=448380
     *  later algorithm was adapted from Visualizing Quaternions, by Andrew J. Hanson
     *   (Morgan Kaufmann, 2006), page 446
     *  
     *  HOWEVER, checking with AxisAngle4f and Quat4f equivalents, it was found that
     *  BOTH of these sources produce inverted quaternions. So here we do an inversion.
     *  
     *  This correction was made in 11.5.42  6/19/2008  -- Bob Hanson
     *
     *  former algorithm used:     
     * /
     
     double tr = mat.m00 + mat.m11 + mat.m22; //Matrix trace 
     double s;
     double[] q = new double[4];
     if (tr > 0) {
     s = Math.sqrt(tr + 1);
     q0 = (float) (0.5 * s);
     s = 0.5 / s; // = 1/q0
     q1 = (float) ((mat.m21 - mat.m12) * s);
     q2 = (float) ((mat.m02 - mat.m20) * s);
     q3 = (float) ((mat.m10 - mat.m01) * s);
     } else {
     float[][] m = new float[][] { new float[3], new float[3], new float[3] };
     mat.getRow(0, m[0]);
     mat.getRow(1, m[1]);
     mat.getRow(2, m[2]);

     //Find out the biggest element along the diagonal 
     float max = Math.max(mat.m11, mat.m00);
     int i = (mat.m22 > max ? 2 : max == mat.m11 ? 1 : 0);
     int j = (i + 1) % 3;
     int k = (j + 1) % 3;
     s = -Math.sqrt(1 + m[i][i] - m[j][j] - m[k][k]);
     // 0 = 1 + (1-x^2)ct + x^2 -(1-y^2)ct - y^2 - (1-z^2)ct - z^2
     // 0 = 1 - ct + (x^2 - y^2 - z^2) - (x^2 - y^2 - z^2)ct
     // 0 = 1 - ct + 2x^2 - 1 - (2x^2)ct + ct
     // 0 = 2x^2(1 - ct)
     // theta = 0 (but then trace = 1 + 1 + 1 = 3)
     // or x = 0. 
     q[i] = s * 0.5;
     if (s != 0)
     s = 0.5 / s; // = 1/q[i]
     q[j] = (m[i][j] + m[j][i]) * s;
     q[k] = (m[i][k] + m[k][i]) * s;
     q0 = (float) ((m[k][j] - m[j][k]) * s);
     q1 = (float) q[0]; // x
     q2 = (float) q[1]; // y
     q3 = (float) q[2]; // z 
     }

     */
  }

  /*
   * if qref is null, "fix" this quaternion
   * otherwise, return a quaternion that is CLOSEST to the given quaternion
   * that is, one that gives a positive dot product
   * 
   */
  public void setRef(Quaternion qref) {
    if (qref == null) {
      fixQ(this);
      return;
    }
    if (dot(qref) >= 0)
      return;
    q0 *= -1;
    q1 *= -1;
    q2 *= -1;
    q3 *= -1;
  }

  public static final Quaternion getQuaternionFrame(Point3f center, Point3f x, Point3f xy) {
    Vector3f vA = new Vector3f(x);
    vA.sub(center);
    Vector3f vB = new Vector3f(xy);
    vB.sub(center);
    return getQuaternionFrame(vA, vB, null);
  }
  
  public static final Quaternion getQuaternionFrame(Vector3f vA, Vector3f vB,
                                                    Vector3f vC) {
    if (vC == null) {
      vC = new Vector3f();
      vC.cross(vA, vB);
    }
    Vector3f vBprime = new Vector3f();
    vBprime.cross(vC, vA);
    vA.normalize();
    vBprime.normalize();
    vC.normalize();
    Matrix3f mat = new Matrix3f();
    mat.setColumn(0, vA);
    mat.setColumn(1, vBprime);
    mat.setColumn(2, vC);

    /*
     * 
     * Verification tests using Quat4f and AngleAxis4f:
     * 
     System.out.println("quaternion frame matrix: " + mat);
     
     Point3f pt2 = new Point3f();
     mat.transform(new Point3f(1, 0, 0), pt2);
     System.out.println("vA=" + vA + " M(100)=" + pt2);
     mat.transform(new Point3f(0, 1, 0), pt2);
     System.out.println("vB'=" + vBprime + " M(010)=" + pt2);
     mat.transform(new Point3f(0, 0, 1), pt2);
     System.out.println("vC=" + vC + " M(001)=" + pt2);
     Quat4f q4 = new Quat4f();
     q4.set(mat);
     System.out.println("----");
     System.out.println("Quat4f: {" + q4.w + " " + q4.x + " " + q4.y + " " + q4.z + "}");
     System.out.println("Quat4f: 2xy + 2wz = m10: " + (2 * q4.x * q4.y + 2 * q4.w * q4.z) + " = " + mat.m10);   
     
     */

    Quaternion q = new Quaternion(mat);

     /*
     System.out.println("Quaternion mat from q \n" + q.getMatrix());
     System.out.println("Quaternion: " + q.getNormal() + " " + q.getTheta());
     AxisAngle4f a = new AxisAngle4f();
     a.set(mat);
     Vector3f v = new Vector3f(a.x, a.y, a.z);
     v.normalize();
     System.out.println("angleAxis: " + v + " "+(a.angle/Math.PI * 180));
     */
     
    return q;
  }

  public Matrix3f getMatrix() {
    if (mat == null)
      setMatrix();
    return mat;
  }

  private void setMatrix() {
    mat = new Matrix3f();
    // q0 = w, q1 = x, q2 = y, q3 = z
    mat.m00 = q0 * q0 + q1 * q1 - q2 * q2 - q3 * q3;
    mat.m01 = 2 * q1 * q2 - 2 * q0 * q3;
    mat.m02 = 2 * q1 * q3 + 2 * q0 * q2;
    mat.m10 = 2 * q1 * q2 + 2 * q0 * q3;
    mat.m11 = q0 * q0 - q1 * q1 + q2 * q2 - q3 * q3;
    mat.m12 = 2 * q2 * q3 - 2 * q0 * q1;
    mat.m20 = 2 * q1 * q3 - 2 * q0 * q2;
    mat.m21 = 2 * q2 * q3 + 2 * q0 * q1;
    mat.m22 = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3;
  }

  public Quaternion add(float x) {
    // scalar theta addition (degrees) 
   return new Quaternion(getNormal(), getTheta() + x);
  }

  public Quaternion mul(float x) {
    // scalar theta multiplication
    return (x == 1 ? new Quaternion(q0, q1, q2, q3) : 
      new Quaternion(getNormal(), getTheta() * x));
  }

  public Quaternion mul(Quaternion p) {
    return new Quaternion(q0 * p.q0 - q1 * p.q1 - q2 * p.q2 - q3 * p.q3, q0
        * p.q1 + q1 * p.q0 + q2 * p.q3 - q3 * p.q2, q0 * p.q2 + q2 * p.q0 + q3
        * p.q1 - q1 * p.q3, q0 * p.q3 + q3 * p.q0 + q1 * p.q2 - q2 * p.q1);
  }

  public Quaternion div(Quaternion p) {
    // unit quaternions assumed -- otherwise would scale by 1/p.dot(p)
    return mul(p.inv());
  }

  public Quaternion divLeft(Quaternion p) {
    // unit quaternions assumed -- otherwise would scale by 1/p.dot(p)
    return this.inv().mul(p);
  }

  public float dot(Quaternion q) {
    return this.q0 * q.q0 + this.q1 * q.q1 + this.q2 * q.q2 + this.q3 * q.q3;
  }

  public Quaternion inv() {
    return new Quaternion(q0, -q1, -q2, -q3);
  }

  public Quaternion negate() {
    return new Quaternion(-q0, -q1, -q2, -q3);
  }

  /*
   * return a quaternion having:
   * 1) q0 > 0
   * or
   * 2) q0 = 0 and q1 > 0
   * or
   * 3) q0 = 0 and q1 = 0 and q2 > 0
   * or
   * 4) q0 = 0 and q1 = 0 and q2 = 0 and q3 > 0
   * 
   */
  private void fixQ(Quaternion qNew) {
    float f = (q0 < 0 || q0 == 0
        && (q1 < 0 || q1 == 0 && (q2 < 0 || q2 == 0 && q3 < 0)) ? -1 : 1);
    qNew.q0 = q0 * f;
    qNew.q1 = q1 * f;
    qNew.q2 = q2 * f;
    qNew.q3 = q3 * f;
  }

  public Vector3f getVector(int i) {
    return getVector(i, 1f);
  }

  private Vector3f getVector(int i, float scale) {
    if (i == -1) {
      fixQ(qTemp);
      return new Vector3f(qTemp.q1 * scale, qTemp.q2 * scale, qTemp.q3 * scale);
    }
    if (mat == null)
      setMatrix();
    Vector3f v = new Vector3f();
    mat.getColumn(i, v);
    if (scale != 1f)
      v.scale(scale);
    return v;
  }

  /**
   * 
   * @return  vector such that 0 <= angle <= 180
   */
  public Vector3f getNormal() {
    fixQ(qTemp);
    Vector3f v = new Vector3f(qTemp.q1, qTemp.q2, qTemp.q3);
    if (v.length() == 0)
      return new Vector3f(0, 0, 1);
    v.normalize();
    return v;
  }

  /**
   * 
   * @return 0 <= angle <= 180 in degrees
   */
  public float getTheta() {
    fixQ(qTemp);
    return (float) (Math.acos(qTemp.q0) * 2 * 180 / Math.PI);
  }

  public float getThetaRadians() {
    fixQ(qTemp);
    return (float) (Math.acos(qTemp.q0) * 2);
  }

  /**
   * 
   * @param v0
   * @return    vector option closest to v0
   * 
   */
  public Vector3f getNormalDirected(Vector3f v0) {
    Vector3f v = getNormal();
    if (v0.x * q1 + v0.y * q2 + v0.z * q3 < 0) {
      v.scale(-1);
    }
    return v;
  }

  public Vector3f get3dProjection(Vector3f v3d) {
    v3d.set(q1, q2, q3);
    return v3d;
  }
  
  /**
   * 
   * @param axisAngle
   * @return   fill in theta of axisAngle such that 
   */
  public Point4f getThetaDirected(Point4f axisAngle) {
    //fills in .w;
    float theta = getTheta();
    Vector3f v = getNormal();
    if (axisAngle.x * q1 + axisAngle.y * q2 + axisAngle.z * q3 < 0) {
      v.scale(-1);
      theta = -theta;
    }
    axisAngle.set(v.x, v.y, v.z, theta);
    return axisAngle;
  }

  public Point4f toPoint4f() {
    // NO q0 normalization here

    // note: for quaternions, we save them {q1, q2, q3, q0} 
    // While this may seem odd, it is so that for any point4 -- 
    // planes, axisangles, and quaternions -- we can use the 
    // first three coordinates to determine the relavent axis
    // the fourth then gives us offset to {0,0,0} (plane), 
    // rotation angle (axisangle), and cos(theta/2) (quaternion).
    
    return new Point4f(q1, q2, q3, q0);
  }

  public AxisAngle4f toAxisAngle4f() {
    fixQ(qTemp);
    double theta = 2 * Math.acos(qTemp.q0);
    double sinTheta2 = Math.sin(theta/2);
    Vector3f v = getNormal();
    if (sinTheta2 < 0) {
      v.scale(-1);
      theta = Math.PI - theta;
    }
    return new AxisAngle4f(v, (float) theta);
  }

  public Point3f transform(Point3f pt) {
    if (mat == null)
      setMatrix();
    Point3f ptNew = new Point3f(pt);
    mat.transform(ptNew);
    return ptNew;
  }

  public void transform(Point3f pt, Point3f ptNew) {
    if (mat == null)
      setMatrix();
    mat.transform(pt, ptNew);
  }

  public Vector3f transform(Vector3f v) {
    if (mat == null)
      setMatrix();
    Vector3f vNew = new Vector3f(v);
    mat.transform(vNew);
    return vNew;
  }

  public Quaternion leftDifference(Quaternion q2) {
    //dq = q.leftDifference(qnext);//q.inv().mul(qnext);
    Quaternion q2adjusted = (this.dot(q2) < 0 ? q2.negate() : q2);
    return inv().mul(q2adjusted);
  }

  public Quaternion rightDifference(Quaternion q2) {
    //dq = qnext.rightDifference(q);//qnext.mul(q.inv());
    Quaternion q2adjusted = (this.dot(q2) < 0 ? q2.negate() : q2);
    return mul(q2adjusted.inv());
  }

  public String getInfo() {
    AxisAngle4f axis = toAxisAngle4f();
    return TextFormat.sprintf("%10.6f%10.6f%10.6f%10.6f  %6.2f  %10.5f %10.5f %10.5f",
        new Object[] { new float[] { q0, q1, q2, q3, 
            (float) (axis.angle * 180 / Math.PI), axis.x, axis.y, axis.z } });
  }

  public String draw(String prefix, String id, Point3f ptCenter, 
                     float scale) {
    String strV = " VECTOR " + Escape.escape(ptCenter) + " ";
    if (scale == 0)
      scale = 1f;
    return "draw " + prefix + "x" + id + strV
        + Escape.escape(getVector(0, scale)) + " color red\n"
        + "draw " + prefix + "y" + id + strV
        + Escape.escape(getVector(1, scale)) + " color green\n"
        + "draw " + prefix + "z" + id + strV
        + Escape.escape(getVector(2, scale)) + " color blue\n";
  }

  /**
   * 
   *  Java axisAngle / plane / Point4f format
   *  all have the format {x y z w}
   *  so we go with that here as well
   *   
   * @return  "{q1 q2 q3 q0}"
   */
  public String toString() {
    return "{" + q1 + " " + q2 + " " + q3 + " " + q0 + "}";
  }

  public static Object sphereMean(Quaternion[] data, float[] retStddev, float criterion) {
    // would like this to be based on Buss and Fillmore 
    // "Spherical Averages and Applications to Spherical Splines and Interpolation"
    // currently just simple 3D average, though
    while (true) {
      if (data == null || data.length == 0)
        break;
      if (retStddev == null)
        retStddev = new float[1];
      if (data.length == 1) {
        retStddev[0] = 0;
        return new Quaternion(data[0]);
      }        
      float diff = Float.MAX_VALUE;
      float lastStddev = Float.MAX_VALUE;
      Quaternion[] ndata = sphereNormalize(data);
      Quaternion qMean = simpleAverage(ndata);
      int maxIter = 100;
      while (diff > criterion && --maxIter >= 0) {
        qMean = newMean(ndata, qMean); 
        retStddev[0] = stdDev(ndata, qMean);
        diff = Math.abs(retStddev[0] - lastStddev);
        lastStddev = retStddev[0];
        System.out.println("sphereMean " + lastStddev + " " + diff);
      }
      return qMean;
    }
    return "NaN";
  }

  private static Quaternion newMean(Quaternion[] ndata, Quaternion mean) {
    Vector3f sum = new Vector3f();
    Vector3f vMean = mean.getNormal();
    Vector3f v = new Vector3f();
    for (int i = ndata.length; --i >= 0;) {
      Quaternion q = ndata[i];
      Quaternion dq = q.div(mean);
      float dist_theta = dq.getThetaRadians();
      //System.out.println(" newmean dist_theta " + dq + " " + dist_theta * 180 / 3.1415926);
      v = dq.getNormal();
      //System.out.println(projections[i]);
      v.scale(dist_theta / 2);
      sum.add(v);
    }
    sum.scale(1f / ndata.length);
    if (sum.dot(vMean) < 0)
      sum.scale(-1);
    float theta = 2 * (float) (Math.asin(sum.length()) * 180 / Math.PI);
    Quaternion newMean = new Quaternion(sum, theta);
    System.out.println("newMean = " + newMean + " " + sum +  " " + theta);
    newMean = newMean.mul(mean);
    if (newMean.dot(mean) < 0)
      newMean = newMean.negate();
    return newMean;
  }

  private static Quaternion[] sphereNormalize(Quaternion[] data) {
    Quaternion[] ndata = new Quaternion[data.length];
    for (int i = data.length; --i >= 0;)
      ndata[i] = new Quaternion(data[i]);
    // Find the coordinate with the max. total absolute value.
    double xAbsSum=0, yAbsSum=0, zAbsSum=0, wAbsSum=0;
    for (int i = data.length; --i >= 0;) {
      xAbsSum += Math.abs(data[i].q0);
      yAbsSum += Math.abs(data[i].q1);
      zAbsSum += Math.abs(data[i].q2);
      wAbsSum += Math.abs(data[i].q3);
    }
    int index = 3;
    if (xAbsSum>yAbsSum) {
      if (xAbsSum>zAbsSum) {
        if (xAbsSum>wAbsSum)
          index = 0;
      } else if (zAbsSum>wAbsSum) {
        index = 2;
      }
    } else if (yAbsSum>zAbsSum) {
      if (yAbsSum>wAbsSum)
        index = 1;
    } else if (zAbsSum>wAbsSum) {
      index = 2;
    }
    for (int i = data.length; --i >= 0;) {
      float value;
      switch (index) {
      case 0:
        value = ndata[i].q0;
        break;
      case 1:
        value = ndata[i].q1;
        break;
      case 2:
        value = ndata[i].q2;
        break;
      default:
        value = ndata[i].q3;
        break;
      }
      if (value < 0)
        ndata[i] = ndata[i].negate();
    }
    return ndata;
  }

  /**
   * get average normal vector
   * scale normal by average projection of vectors onto it
   * create quaternion from this 3D projection
   * 
   * @param ndata
   * @return approximate average
   */
  private static Quaternion simpleAverage(Quaternion[] ndata) {
    Vector3f mean = new Vector3f();
    for (int i = ndata.length; --i >= 0;)
      mean.add(ndata[i].getNormal());
    float f = 0;
    mean.normalize();
    Vector3f v = new Vector3f();
    for (int i = ndata.length; --i >= 0;)
      f += ndata[i].get3dProjection(v).dot(mean);
    mean.scale(f / ndata.length);
    f = (float) Math.sqrt(1 - mean.lengthSquared());
    if (Float.isNaN(f))
      f = 0;
    return new Quaternion(new Point4f(mean.x, mean.y, mean.z, f));
  }

  private static float stdDev(Quaternion[] data, Quaternion mean) {
    double sum = 0;
    double sum2 = 0;
    int n = data.length;
    // the quaternion dot product gives theta/2 for dq
    // it is proportional to the distance on the 4D sphere
    // just as for two unit vectors, a dot b is the angle theta between them
    // as well as the distance on the 3D sphere
    for (int i = n; --i >= 0;) {
      float dist = data[i].dot(mean);
      sum += dist;
      sum2 += dist * dist;
    }
    return (float) Math.sqrt((sum2 - sum * sum / n) / (n - 1));
  }

}
