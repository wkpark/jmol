/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
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

package org.jmol.minimize;

import java.util.Random;

import javax.vecmath.Vector3d;

public class Util {

  final public static double RAD_TO_DEG = (180.0 / Math.PI);
  final public static double DEG_TO_RAD = (Math.PI / 180.0);

  public static Vector3d newSub(Vector3d a, Vector3d b) {
    return new Vector3d(a.x - b.x, a.y - b.y, a.z - b.z);
  }

  public static Vector3d newSub(double[] a, double[] b) {
    return new Vector3d(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
  }

  public static void putCoord(Vector3d v, double[] c) {
    c[0] = v.x;
    c[1] = v.y;
    c[2] = v.z;
  }

  public static double distance2(double[] a, double[] b) {
    double dx = a[0] - b[0];
    double dy = a[1] - b[1];
    double dz = a[2] - b[2];
    return (dx * dx + dy * dy + dz * dz);
  }
  
  public static double distance2(Vector3d a, Vector3d b) {
    double dx = a.x - b.x;
    double dy = a.y - b.y;
    double dz = a.z - b.z;
    return (dx * dx + dy * dy + dz * dz);
  }

  public static double getAngleRadiansABC(double[] a, double[] b, double[] c) {
    // cos law:
    // (ac)^2 = (ab)^2 + (bc)^2 - 2(ab)(bc)cos_theta
    // 2(ab)(bc) cos_theta = (ab)^2 + (bc)^2 - (ac)^2
    // cos_theta = ((ab)^2 + (bc)^2 - (ac)^2) / 2 (ab)(bc)
    double ab2 = distance2(a, b);
    double bc2 = distance2(b, c);
    double ac2 = distance2(a, c);
    return (isNearZero(ab2, 1e-3) || isNearZero(bc2, 1e-3) ? 0 :
        Math.acos((ab2 + bc2 - ac2 ) / 2 / Math.sqrt(ab2 * bc2)));
  }
  
  public static boolean isApprox(Vector3d a, Vector3d b, double precision) {
    return (distance2(a, b) <= precision * precision
        * Math.min(a.lengthSquared(), b.lengthSquared()));
  }

  final static double max_squarable_double = 1e150;
  final static double min_squarable_double = 1e-150;

  public static boolean canBeSquared(double x) {
    if (x == 0)
      return true;
    return ((x = Math.abs(x)) < max_squarable_double && x > min_squarable_double);
  }

  public static boolean isNegligible(double a, double b) {
    return isNegligible(a, b, 1e-11);
  }

  public static boolean isFinite(double a) {
    return !Double.isInfinite(a) && !Double.isNaN(a);
  }

  public static boolean isNegligible(double a, double b, double precision) {
    return (Math.abs(a) <= precision * Math.abs(b));
  }

  public static boolean isNear(double a, double b) {
    return isNear(a, b, 2e-6);
  }

  public static boolean isNear(double a, double b, double epsilon) {
    return (Math.abs(a - b) < epsilon);
  }
  public static boolean isNearZero(double a) {
    return isNearZero(a, 2e-6);
  }

  public static boolean isNearZero(double a, double epsilon) {
    return (Math.abs(a) < epsilon);
  }

  public static boolean canBeNormalized(Vector3d a) {
    if (a.x == 0.0 && a.y == 0.0 && a.z == 0.0)
      return false;
    return (canBeSquared(a.x) && canBeSquared(a.y) && canBeSquared(a.z));
  }

  public static Vector3d newCross(Vector3d v1, Vector3d v2) {
    Vector3d vv = new Vector3d();
    vv.cross(v1, v2);
    return vv;
  }

  public static double vectorLengthDerivative(Vector3d a, Vector3d b) {
    Vector3d vab = newSub(a, b);
    double rab = vab.length();
    if (rab < 0.1) {// atoms are too close to each other
      randomizeUnitVector(vab);
      rab = 0.1;
    }
    vab.normalize();
    a.set(vab);
    a.scale(-1); // -drab/da
    b.set(vab); // -drab/db

    return rab;
  }

  private static void randomizeUnitVector(Vector3d v) {
    Random ptr = new Random();

    // obtain a random vector with 0.001 <= length^2 <= 1.0, normalize
    // the vector to obtain a random vector of length 1.0.
    double l;
    do {
      v
          .set(ptr.nextFloat() - 0.5, ptr.nextFloat() - 0.5,
              ptr.nextFloat() - 0.5);
      l = v.lengthSquared();
    } while ((l > 1.0) || (l < 1e-4));
    v.normalize();
  }

  public static double vectorAngleRadians(Vector3d v1, Vector3d v2) {
    double l1 = v1.length();
    double l2 = v2.length();
    return (isNearZero(l1) || isNearZero(l2) ? 0 :
      Math.acos(v1.dot(v2) / (l1 * l2)));
  }

  public static double vectorAngleDerivative(Vector3d i, Vector3d j, Vector3d k) {
    // This is adapted from http://scidok.sulb.uni-saarland.de/volltexte/2007/1325/pdf/Dissertation_1544_Moll_Andr_2007.pdf
    // Many thanks to Andreas Moll and the BALLView developers for this
    // via OpenBabel

    // Calculate the vector between atom1 and atom2,
    // test if the vector has length larger than 0 and normalize it
    Vector3d v1 = newSub(i, j);
    Vector3d v2 = newSub(k, j);

    double length1 = v1.length();
    double length2 = v2.length();

    // test if the vector has length larger than 0 and normalize it
    if (isNearZero(length1) || isNearZero(length2)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      return 0.0;
    }

    // Calculate the normalized bond vectors
    double inverse_length_v1 = 1.0 / length1;
    double inverse_length_v2 = 1.0 / length2;
    v1.scale(inverse_length_v1);
    v2.scale(inverse_length_v2);

    // Calculate the cross product of v1 and v2, test if it has length unequal 0,
    // and normalize it.
    Vector3d c1 = newCross(v1, v2);
    double length = c1.length();
    if (isNearZero(length)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      return 0.0;
    }

    c1.scale(1 / length);

    // Calculate the Math.cos of theta and then theta
    double costheta = v1.dot(v2);
    double theta;
    if (costheta > 1.0) {
      theta = 0.0;
      costheta = 1.0;
    } else if (costheta < -1.0) {
      theta = 180.0;
      costheta = -1.0;
    } else {
      theta = RAD_TO_DEG * Math.acos(costheta);
    }

    Vector3d t1 = newCross(v1, c1);
    t1.normalize();
    Vector3d t2 = newCross(v2, c1);
    t2.normalize();

    t1.scale(-inverse_length_v1);
    i.set(t1);
    t2.scale(inverse_length_v2);
    k.set(t2);
    t2.add(i);
    j.set(t2);
    j.scale(-1);
    return theta;
  }

  public static double vectorTorsionDerivativeRadians(Vector3d i, Vector3d j,
                                               Vector3d k, Vector3d l) {
    // This is adapted from http://scidok.sulb.uni-saarland.de/volltexte/2007/1325/pdf/Dissertation_1544_Moll_Andr_2007.pdf
    // Many thanks to Andreas Moll and the BALLView developers for this

    // Bond vectors of the three atoms
    Vector3d ij, jk, kl;
    // length of the three bonds
    double l_ij, l_jk, l_kl;
    // angle between ijk and jkl:
    double angle_ijk, angle_jkl;

    ij = newSub(j, i);
    jk = newSub(k, j);
    kl = newSub(l, k);

    l_ij = ij.length();
    l_jk = jk.length();
    l_kl = kl.length();

    if (isNearZero(l_ij) || isNearZero(l_jk) || isNearZero(l_kl)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return 0.0;
    }

    angle_ijk = vectorAngleRadians(ij, jk);
    angle_jkl = vectorAngleRadians(jk, kl);

    // normalize the bond vectors:
    ij.scale(1 / l_ij);
    jk.scale(1 / l_jk);
    k.scale(1 / l_kl);

    double sin_j = Math.sin(angle_ijk);
    double sin_k = Math.sin(angle_jkl);

    double rsj = l_ij * sin_j;
    double rsk = l_kl * sin_k;

    double rs2j = 1. / (rsj * sin_j);
    double rs2k = 1. / (rsk * sin_k);

    double rrj = l_ij / l_jk;
    double rrk = l_kl / l_jk;

    double rrcj = rrj * (-Math.cos(angle_ijk));
    double rrck = rrk * (-Math.cos(angle_jkl));

    Vector3d a = newCross(ij, jk);
    Vector3d b = newCross(jk, kl);
    Vector3d c = newCross(a, b);

    i.set(a);
    i.scale(-rs2j);
    l.set(b);
    b.scale(rs2k);

    j.set(i);
    j.scale(rrcj - 1.);
    k.set(l);
    k.scale(-rrck);
    j.add(k);
    k.set(i);
    k.add(j);
    k.add(l);
    k.scale(-1);

    return Math.atan2(c.dot(jk), a.dot(b));
  }

  /* Calculate the angle between point a and the plane determined by b,c,d */
  public static double pointPlaneAngleRadians(Vector3d a, Vector3d b, Vector3d c,
                                        Vector3d d) {

    Vector3d ac = newSub(a, c);
    Vector3d bc = newSub(b, c);
    Vector3d cd = newSub(c, d);

    Vector3d normal = newCross(bc, cd);
    return Math.PI / 2.0 - vectorAngleRadians(normal, ac);
  }
  
  public static double vectorPlaneDerivativeRadians(Vector3d i, Vector3d j, Vector3d k,
                                           Vector3d l) {
    // This is adapted from http://scidok.sulb.uni-saarland.de/volltexte/2007/1325/pdf/Dissertation_1544_Moll_Andr_2007.pdf
    // Many thanks to Andreas Moll and the BALLView developers for this

    // temp variables:
    double length;
    Vector3d delta;

    // normal vectors of the three planes:
    Vector3d an, bn, cn;

    // calculate normalized bond vectors from central atom to outer atoms:
    delta = newSub(i, j);
    length = delta.length();
    if (isNearZero(length)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return 0.0;
    }
    // normalize the bond vector:
    delta.scale(1 / length);
    // store the normalized bond vector from central atom to outer atoms:
    Vector3d ji = delta;
    // store length of this bond:
    double length_ji = length;

    delta = newSub(k, j);
    length = delta.length();
    if (isNearZero(length)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return 0.0;
    }
    // normalize the bond vector:
    delta.scale(1 / length);
    // store the normalized bond vector from central atom to outer atoms:
    Vector3d jk = delta;
    // store length of this bond:
    double length_jk = length;

    delta = newSub(l, j);
    length = delta.length();
    if (isNearZero(length)) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return 0.0;
    }
    // normalize the bond vector:
    delta.scale(1 / length);
    // store the normalized bond vector from central atom to outer atoms:
    Vector3d jl = delta;
    // store length of this bond:
    double length_jl = length;

    // the normal vectors of the three planes:
    an = newCross(ji, jk);
    bn = newCross(jk, jl);
    cn = newCross(jl, ji);

    // Bond angle ji to jk
    double cos_theta = ji.dot(jk);
    double theta = Math.acos(cos_theta);
    // If theta equals 180 degree or 0 degree
    if (isNearZero(theta) || isNearZero(Math.abs(theta - Math.PI))) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return 0.0;
    }

    double sin_theta = Math.sin(theta);
    double sin_dl = an.dot(jl) / sin_theta;

    // the wilson angle:
    double dl = Math.asin(sin_dl);

    // In case: wilson angle equals 0 or 180 degree: do nothing
    if (isNearZero(dl) || isNearZero(Math.abs(dl - Math.PI))) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return dl;
    }

    double cos_dl = Math.cos(dl);

    // if wilson angle equal 90 degree: abort
    if (cos_dl < 0.0001) {
      i.set(0, 0, 0);
      j.set(0, 0, 0);
      k.set(0, 0, 0);
      l.set(0, 0, 0);
      return dl;
    }

    //    l = (an / sin_theta - jl * sin_dl) / length_jl;

    an.scale(1 / length_jl / sin_theta);
    jl.scale(sin_dl / length_jl);
    l.set(an);
    l.sub(jl);

    k.set(jk); //temp storage

    //    i = ((bn + (((-ji + jk * cos_theta) * sin_dl) / sin_theta)) / length_ji) / sin_theta;

    jk.scale(cos_theta);
    jk.sub(ji);
    jk.scale(sin_dl / sin_theta);
    jk.add(bn);
    jk.scale(1 / length_ji / sin_theta);
    i.set(jk);

    //    k = ((cn + (((-jk + ji * cos_theta) * sin_dl) / sin_theta)) / length_jk) / sin_theta;

    ji.scale(cos_theta);
    ji.sub(k); //original jk value
    ji.scale(sin_dl / sin_theta);
    ji.add(cn);
    ji.scale(1 / length_jk / sin_theta);
    k.set(ji);

    //    j = -(i + k + l);

    j.set(i);
    j.add(k);
    j.add(l);
    j.scale(-1);

    return dl;
  }
  
  
}
