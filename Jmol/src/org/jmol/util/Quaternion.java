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
  // create a new object with the given components
  private Quaternion(float q0, float q1, float q2, float q3) {
    this.q0 = q0;
    this.q1 = q1;
    this.q2 = q2;
    this.q3 = q3;
    fixQ();
  }

  public Quaternion(Point4f pt) {
    float factor = pt.distance(new Point4f(0,0,0,0));
    if (factor == 0) {
      q0 = 1;
      return;
    }
    q0 = pt.w / factor;
    q1 = pt.x / factor;
    q2 = pt.y / factor;
    q3 = pt.z / factor;
    fixQ();
}

  public Quaternion(Tuple3f pt, float theta) {
    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      q0 = 1;
      return;
    }
    float fact = (float)(Math.sin(theta/2*Math.PI/180)/Math.sqrt(pt.x * pt.x + pt.y * pt.y + pt.z * pt.z));
    q0 = (float) (Math.cos(theta/2 * Math.PI/180));
    q1 = pt.x * fact;
    q2 = pt.y * fact;
    q3 = pt.z * fact;
    fixQ();
  }
  
  private Quaternion fixQ() {
    if (q0 < 0) {
      q0 = -q0;
      q1 = -q1;
      q2 = -q2;
      q3 = -q3;
    }
    return this;
  }

  public Quaternion(Matrix3f mat) {

    /*
     *  Originally from http://www.gamedev.net/community/forums/topic.asp?topic_id=448380
     *  The current algorithm is adapted from Visualizing Quaternions, by Andrew J. Hanson
     *   (Morgan Kaufmann, 2006), page 446
     *  
     *  HOWEVER, checking with AxisAngle4f and Quat4f equivalents, it was found that
     *  BOTH of these sources produce inverted quaternions. So here we do an inversion.
     *  
     *  This correction was made in 11.5.42  6/19/2008  -- Bob Hanson
     *  
     */

    float tr = mat.m00 + mat.m11 + mat.m22; /* Matrix trace */
    float s;
    float[] q = new float[4];
    if (tr > 0) {
      s = (float) Math.sqrt(tr + 1);
      q0 = 0.5f * s;
      s = 0.5f / s;
      q1 = (mat.m21 - mat.m12) * s;
      q2 = (mat.m02 - mat.m20) * s;
      q3 = (mat.m10 - mat.m01) * s;
    } else {
      float[][] m = new float[][] { new float[3], new float[3], new float[3] };
      mat.getRow(0, m[0]);
      mat.getRow(1, m[1]);
      mat.getRow(2, m[2]);

      /* Find out the biggest element along the diagonal */
      float max = Math.max(mat.m11, mat.m00);
      int i = (mat.m22 > max ? 2 : max == mat.m11 ? 1 : 0);
      int j = (i + 1) % 3;
      int k = (j + 1) % 3;
      s = -(float) Math.sqrt((m[i][i] - (m[j][j] + m[k][k])) + 1);
      q[i] = s * 0.5f;
      if (s != 0)
        s = 0.5f / s;
      q[j] = (m[i][j] + m[j][i]) * s;
      q[k] = (m[i][k] + m[k][i]) * s;
      q0 = (m[k][j] - m[j][k]) * s;
      q1 = q[0];  // x
      q2 = q[1];  // y
      q3 = q[2];  // z  
    }
    fixQ();
  }
  
  public static final Quaternion getQuaternionFrame(Vector3f vA, Vector3f vB, Vector3f vC) {
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
    // UNIT addition 
    return new Quaternion(getNormal(), getTheta() + x);
  }

  public Quaternion mul(float x) {
    // UNIT multiplication
    if (x == 1 || x == -1)
      return new Quaternion(q0 * x, q1 * x, q2 * x, q3 * x);
    return new Quaternion(getNormal(), getTheta() * x);
  }
  
  public Quaternion mul(Quaternion p) {
    return new Quaternion(
        q0 * p.q0 - q1 * p.q1 - q2 * p.q2 - q3 * p.q3,
        q0 * p.q1 + q1 * p.q0 + q2 * p.q3 - q3 * p.q2,
        q0 * p.q2 + q2 * p.q0 + q3 * p.q1 - q1 * p.q3,
        q0 * p.q3 + q3 * p.q0 + q1 * p.q2 - q2 * p.q1
        );
  }
  
  public Quaternion div(Quaternion p) {
    // unit quaternions assumed -- otherwise would scale by 1/p.dot(p)
    return mul(p.inv());
  }
  
  public Quaternion divLeft(Quaternion p) {
    // unit quaternions assumed -- otherwise would scale by 1/p.dot(p)
    return p.inv().mul(this);
  }
  
  public float dot(Quaternion q) {
    return this.q0*q.q0 + this.q1*q.q1 + this.q2*q.q2 + this.q3*q.q3;
  }
  
  public Quaternion inv() {
    return new Quaternion(q0, -q1, -q2, -q3);
  }
  
  public String toString() {
    fixQ();
    return "{" + q0 + " " + q1 + " " + q2 + " " + q3 + "}";
  }
  
  public Vector3f getVector(int i) {
    if (mat == null)
      setMatrix();
    Vector3f v = new Vector3f();
    mat.getColumn(i, v);
    return v;
  }
  
  public Vector3f getNormal() {
    fixQ();
    Vector3f v = new Vector3f(q1, q2, q3);
    v.normalize();
    return v;
  }
  
  public float getTheta() {
    fixQ();
    return (float) (Math.acos(q0) * 2 * 180/Math.PI);  
  }
  
  public void getThetaDirected(Point4f axisAngle) {
    //fills in .w;
    float theta = getTheta();
    Vector3f v = getNormal();
    if (axisAngle.x * q1 + axisAngle.y * q2 + axisAngle.z * q3 < 0) {
      v.scale(-1);
      theta = -theta;
    }
    axisAngle.set(v.x, v.y, v.z, theta);
  }
  
  public Point4f toPoint4f() {
    fixQ();
    return new Point4f(q1, q2, q3, q0);
  }
  
  public Point3f transform(Point3f pt) {
    if (mat == null)
      setMatrix();
    Point3f ptNew = new Point3f(pt);
    mat.transform(ptNew);
    return ptNew;
  }

  public Vector3f transform(Vector3f v) {
    if (mat == null)
      setMatrix();
    Vector3f vNew = new Vector3f(v);
    mat.transform(vNew);
    return vNew;
  }

}