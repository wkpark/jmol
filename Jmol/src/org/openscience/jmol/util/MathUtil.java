/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.util;


import java.lang.reflect.Array;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Matrix3d;

public class MathUtil {

  public static Point3f arrayToPoint3f(float[] array) {
    return new Point3f(array[0], array[1], array[2]);
  }

  public static Vector3f arrayToVector3f(float[] array) {
    return new Vector3f(array[0], array[1], array[2]);
  }

  public static Point3d arrayToPoint3d(double[] array) {
    return new Point3d(array[0], array[1], array[2]);
  }

  public static Vector3d arrayToVector3d(double[] array) {
    return new Vector3d(array[0], array[1], array[2]);
  }

  
  /**
   * Multiply the matrix "mat" by the vector "vec".
   * The result is a vector.
   */
  public static double[] mulVec(Matrix3d mat, double[] vec) {

    double[] result = new double[3];
    result[0] = mat.m00 * vec[0] + mat.m01 * vec[1] + mat.m02 * vec[2];
    result[1] = mat.m10 * vec[0] + mat.m11 * vec[1] + mat.m12 * vec[2];
    result[2] = mat.m20 * vec[0] + mat.m21 * vec[1] + mat.m22 * vec[2];
    return result;
  }


  /**
   * Given a <code>double</code> f, return the closest superior integer
   *
   */
  public static int intSup(double f) {
    if (f <= 0) {
      return (int) f;
    } else {
      return (int) f + 1;
    }

  }

  /**
   * Given a <code>double</code> f, return the closest inferior integer
   *
   */
  public static int intInf(double f) {
    if (f < 0) {
      return (int) f - 1;
    } else {
      return (int) f;
    }
  }

  /**
   * Convert a <code>Matrix3d</code> to a <code>double[3][3]</code>.
   *
   */
  public static double[][] matrix3fToArray(Matrix3d matrix3f) {

    double[][] array = new double[3][3];

    array[0][0] = matrix3f.m00;
    array[0][1] = matrix3f.m01;
    array[0][2] = matrix3f.m02;

    array[1][0] = matrix3f.m10;
    array[1][1] = matrix3f.m11;
    array[1][2] = matrix3f.m12;

    array[2][0] = matrix3f.m20;
    array[2][1] = matrix3f.m21;
    array[2][2] = matrix3f.m22;

    return array;
  }

  /**
   * Convert a <code>double[3][3]</code> to a <code>Matrix3d</code>.
   *
   */
  public static Matrix3d arrayToMatrix3d(double[][] array) {

    Matrix3d matrix3f = new Matrix3d();

    matrix3f.m00 = array[0][0];
    matrix3f.m01 = array[0][1];
    matrix3f.m02 = array[0][2];

    matrix3f.m10 = array[1][0];
    matrix3f.m11 = array[1][1];
    matrix3f.m12 = array[1][2];

    matrix3f.m20 = array[2][0];
    matrix3f.m21 = array[2][1];
    matrix3f.m22 = array[2][2];

    return matrix3f;
  }

}
