/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.viewer;
import java.lang.reflect.Array;

final public class Util {

  public static Object ensureLength(Object array, int minimumLength) {
    if (Array.getLength(array) >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static String[] ensureLength(String[] array, int minimumLength) {
    if (array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static short[] ensureLength(short[] array, int minimumLength) {
    if (array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static byte[] ensureLength(byte[] array, int minimumLength) {
    if (array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static Object doubleLength(Object array) {
    return setLength(array, 2 * Array.getLength(array));
  }

  public static String[] doubleLength(String[] array) {
    return setLength(array, 2 * array.length);
  }

  public static float[] doubleLength(float[] array) {
    return setLength(array, 2 * array.length);
  }

  public static int[] doubleLength(int[] array) {
    return setLength(array, 2 * array.length);
  }

  public static short[] doubleLength(short[] array) {
    return setLength(array, 2 * array.length);
  }

  public static byte[] doubleLength(byte[] array) {
    return setLength(array, 2 * array.length);
  }

  public static Object setLength(Object array, int newLength) {
    Object t =
      Array.newInstance(array.getClass().getComponentType(), newLength);
    int oldLength = Array.getLength(array);
    System.arraycopy(array, 0, t, 0,
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }

  public static String[] setLength(String[] array, int newLength) {
    int oldLength = array.length;
    String[] t = (String[])Array.newInstance(Float.TYPE, newLength);
    System.arraycopy(array, 0, t, 0, 
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }
  
  public static float[] setLength(float[] array, int newLength) {
    int oldLength = array.length;
    float[] t = (float[])Array.newInstance(Float.TYPE, newLength);
    System.arraycopy(array, 0, t, 0, 
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }
  
  public static int[] setLength(int[] array, int newLength) {
    int oldLength = array.length;
    int[] t = (int[])Array.newInstance(Integer.TYPE, newLength);
    System.arraycopy(array, 0, t, 0, 
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }
  
  public static short[] setLength(short[] array, int newLength) {
    int oldLength = array.length;
    short[] t = (short[])Array.newInstance(Short.TYPE, newLength);
    System.arraycopy(array, 0, t, 0, 
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }

  public static byte[] setLength(byte[] array, int newLength) {
    int oldLength = array.length;
    byte[] t = (byte[])Array.newInstance(Byte.TYPE, newLength);
    System.arraycopy(array, 0, t, 0, 
                     oldLength < newLength ? oldLength : newLength);
    return t;
  }
}
