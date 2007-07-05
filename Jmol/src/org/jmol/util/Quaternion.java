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
import javax.vecmath.Vector3f;

public class Quaternion {
  public float q0, q1, q2, q3;

  // create a new object with the given components
  public Quaternion(float q0, float q1, float q2, float q3) {
    this.q0 = q0;
    this.q1 = q1;
    this.q2 = q2;
    this.q3 = q3;
  }

  public Quaternion(Matrix3f mat) {

    //from http://www.gamedev.net/community/forums/topic.asp?topic_id=448380
    float q0, q1, q2, q3;

    float tr = mat.m00 + mat.m11 + mat.m22; /* Matrix trace */
    float s;

    if (tr > 0) {
      s = (float) Math.sqrt(tr + 1);
      q0 = 0.5f * s;
      s = 0.5f / s;
      q1 = (mat.m12 - mat.m21) * s;
      q2 = (mat.m20 - mat.m02) * s;
      q3 = (mat.m01 - mat.m10) * s;
    } else {
      /* Find out the bigger element from diagonal */
      float max = mat.m00;
      max = (mat.m11 > max ? mat.m11 : max);
      max = (mat.m22 > max ? mat.m22 : max);
      if (mat.m00 == max) {
        /* Column 0 */
        s = (float) Math.sqrt(mat.m00 - mat.m11 - mat.m22 + 1);
        q1 = 0.5f * s;
        if (s != 0)
          s = 0.5f / s;
        q0 = (mat.m12 - mat.m21) * s;
        q2 = (mat.m01 + mat.m10) * s;
        q3 = (mat.m02 + mat.m20) * s;
      } else if (mat.m11 == max) {
        /* Column 1 */
        s = (float) Math.sqrt(mat.m11 - mat.m00 - mat.m22 + 1);
        q2 = 0.5f * s;
        if (s != 0)
          s = 0.5f / s;
        q0 = (mat.m20 - mat.m02) * s;
        q1 = (mat.m01 + mat.m10) * s;
        q3 = (mat.m12 + mat.m21) * s;
      } else {
        /* Column 2 */
        s = (float) Math.sqrt(mat.m22 - mat.m00 - mat.m11 + 1);
        q3 = 0.5f * s;
        if (s != 0)
          s = 0.5f / s;
        q0 = (mat.m01 - mat.m10) * s;
        q1 = (mat.m02 + mat.m20) * s;
        q2 = (mat.m12 + mat.m21) * s;
      }
    }
    this.q0 = q0;
    this.q1 = q1;
    this.q2 = q2;
    this.q3 = q3;
  }
  
  public Quaternion mul(float x) {
    return new Quaternion(q0 * x, q1 * x, q2 * x, q3 * x);
  }
  
  public Quaternion mul(Quaternion p) {
    return new Quaternion(
        p.q0 * q0 - p.q1 * q1 - p.q2 * q2 - p.q3 * q3,
        p.q0 * q1 + p.q1 * q0 + p.q2 * q3 - p.q3 * q2,
        p.q0 * q2 + p.q2 * q0 + p.q3 * q1 - p.q1 * q3,
        p.q0 * q3 + p.q3 * q0 + p.q1 * q2 - p.q2 * q1
        );
  }
  
  public float dot(Quaternion q) {
    return this.q0*q.q0 + this.q1*q.q1 + this.q2*q.q2 + this.q3*q.q3;
  }
  
  public Quaternion inv() {
    return new Quaternion(q0, -q1, -q2, -q3);
  }
  
  public String toString() {
    return "{" + q0 + " " + q1 + " " + q2 + " " + q3 + "}";
  }
  
  public static final Quaternion getQuaternionFrame(Vector3f vA, Vector3f vB) {
    Vector3f vC = new Vector3f();
    vC.cross(vA, vB);
    Vector3f vBprime = new Vector3f();
    vBprime.cross(vC, vA);
    vA.normalize();
    vBprime.normalize();
    vC.normalize();
    Matrix3f mat = new Matrix3f();
    mat.setColumn(0, vA);
    mat.setColumn(1, vBprime);
    mat.setColumn(2, vC);
    return new Quaternion(mat);
  }
}
