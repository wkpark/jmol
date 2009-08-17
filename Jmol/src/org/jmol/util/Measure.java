/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.viewer.Token;

final public class Measure {

  public final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  public static float computeTorsion(Tuple3f p1, Tuple3f p2, Tuple3f p3, Tuple3f p4, boolean asDegrees) {
  
    float ijx = p1.x - p2.x;
    float ijy = p1.y - p2.y;
    float ijz = p1.z - p2.z;
  
    float kjx = p3.x - p2.x;
    float kjy = p3.y - p2.y;
    float kjz = p3.z - p2.z;
  
    float klx = p3.x - p4.x;
    float kly = p3.y - p4.y;
    float klz = p3.z - p4.z;
  
    float ax = ijy * kjz - ijz * kjy;
    float ay = ijz * kjx - ijx * kjz;
    float az = ijx * kjy - ijy * kjx;
    float cx = kjy * klz - kjz * kly;
    float cy = kjz * klx - kjx * klz;
    float cz = kjx * kly - kjy * klx;
  
    float ai2 = 1f / (ax * ax + ay * ay + az * az);
    float ci2 = 1f / (cx * cx + cy * cy + cz * cz);
  
    float ai = (float) Math.sqrt(ai2);
    float ci = (float) Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }
  
    float torsion = (float) Math.acos(cosang);
    float dot = ijx * cx + ijy * cy + ijz * cz;
    float absDot = Math.abs(dot);
    torsion = (dot / absDot > 0) ? torsion : -torsion;
    return (asDegrees ? torsion / radiansPerDegree : torsion);
  }

  public static float computeAngle(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, Vector3f vectorBA, Vector3f vectorBC, boolean asDegrees) {
    vectorBA.sub(pointA, pointB);
    vectorBC.sub(pointC, pointB);
    float angle = vectorBA.angle(vectorBC);
    return (asDegrees ? angle / radiansPerDegree : angle);
  }

  public static float computeAngle(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, boolean asDegrees) {
    Vector3f vectorBA = new Vector3f();
    Vector3f vectorBC = new Vector3f();        
    return Measure.computeAngle(pointA, pointB, pointC, vectorBA, vectorBC, asDegrees);
  }

  public static Object computeHelicalAxis(String id, int tokType, Point3f a, Point3f b,
                                    Quaternion dq) {
    /*
                b
           |   /|
           |  / |
           | /  |
           |/   c
         b'+   / \
           |  /   \      Vcb = Vab . n
         n | /     \d    Vda = (Vcb - Vab) / 2
           |/theta  \
         a'+---------a
                r 
    */
    
    Vector3f vab = new Vector3f();
    vab.sub(b, a);
    /*
     * testing here to see if directing the normal makes any difference -- oddly
     * enough, it does not. When n = -n and theta = -theta vab.n is reversed,
     * and that magnitude is multiplied by n in generating the A'-B' vector.
     * 
     * a negative angle implies a left-handed axis (sheets)
     */
    Point4f aa = new Point4f();
    aa.x = vab.x;
    aa.y = vab.y;
    aa.z = vab.z;
    boolean asdirected = false;
    float theta = (asdirected ? dq.getThetaDirected(aa).w : dq.getTheta());// .Directed(aa).w;
    Vector3f n = (asdirected ? dq.getNormalDirected(vab) : dq.getNormal());// Directed(vab);
    aa.x = vab.x;
    aa.y = vab.y;
    aa.z = vab.z;
    float v_dot_n = vab.dot(n);
    if (Math.abs(v_dot_n) < 0.0001f)
      v_dot_n = 0;
    if (tokType == Token.axis) {
      if (v_dot_n != 0)
        n.scale(v_dot_n);
      return n;
    }
    if (v_dot_n == 0)
      v_dot_n = Float.MIN_VALUE; // allow for perpendicular axis to vab
    Vector3f vcb = new Vector3f(n);
    vcb.scale(v_dot_n);
    Vector3f va_prime_d = new Vector3f();
    va_prime_d.cross(vab, n);
    va_prime_d.normalize();
    Vector3f vda = new Vector3f();
    vda.sub(vcb, vab);
    vda.scale(0.5f);
    va_prime_d.scale(theta == 0 ? 0 : (float) (vda.length() / Math
        .tan(theta / 2 / 180 * Math.PI)));
    Vector3f r = new Vector3f(va_prime_d);
    if (theta != 0)
      r.add(vda);
    if (tokType == Token.radius)
      return r;
    Point3f pt_a_prime = new Point3f(a);
    pt_a_prime.sub(r);
    if (tokType == Token.point) {
      return pt_a_prime;
    }
    if (v_dot_n != Float.MIN_VALUE)
      n.scale(v_dot_n);
    // must calculate directed angle:
    Point3f pt_b_prime = new Point3f(pt_a_prime);
    pt_b_prime.add(n);
    theta = computeTorsion(a, pt_a_prime, pt_b_prime, b, true);
    if (Float.isNaN(theta))
      theta = (float) (dq.getTheta() / 180 * Math.PI); // allow for r = 0
    if (tokType == Token.angle)
      return new Float(theta);
    if (tokType == Token.draw) {
      String s = "draw ID " + id + " VECTOR " + Escape.escape(pt_a_prime)
          + " " + Escape.escape(n) + " color "
          + (theta < 0 ? "{255.0 200.0 0.0}" : "{255.0 0.0 128.0}");
      if (Logger.debugging)
          s +=";measure " + Escape.escape(a) + " $" + id 
            + "[1] " + " $" + id + "[2] " + Escape.escape(b);
      return s;
    }
    // for now... array:
    float residuesPerTurn = (theta == 0 ? 0 : 360f / theta);
    float pitch = Math.abs(v_dot_n == Float.MIN_VALUE ? 0 : n.length() * theta / 360f);
    Object[] ret = new Object[] {pt_a_prime, n, r, new Point3f(theta, pitch, residuesPerTurn)};
    if (tokType == Token.array)
      return ret;
    else if (tokType == Token.list)
      return new String[] { 
          Escape.escape(pt_a_prime), // a' 
          Escape.escape(n), // n
          Escape.escape(r), // r
          Escape.escape(new Point3f(theta /*(degrees)*/,pitch, residuesPerTurn))
          };
    return null;
  }

}
