/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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

package org.jmol.viewer;
import org.jmol.g3d.*;

import java.util.BitSet;
import java.io.BufferedReader;
import java.io.IOException;
import javax.vecmath.Point3f;

class Pmesh extends SelectionIndependentShape {

  int vertexCount;
  Point3f[] vertices;
  int polygonCount;
  int[][] polygonIndexes;

  void initShape() {
    colix = Graphics3D.RED;
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("bufferedreader" == propertyName) {
      BufferedReader br = (BufferedReader)value;
      readPmesh(br);
      return;
    }
  }

  void readPmesh(BufferedReader br) {
    System.out.println("Pmesh.readPmesh(" + br + ")");
    try {
      readVertexCount(br);
      System.out.println("vertexCount=" + vertexCount);
      readVertices(br);
      System.out.println("vertices read");
      readPolygonCount(br);
      System.out.println("polygonCount=" + polygonCount);
      readPolygonIndexes(br);
      System.out.println("polygonIndexes read");
    } catch (Exception e) {
      System.out.println("Pmesh.readPmesh exception:" + e);
    }
  }

  void readVertexCount(BufferedReader br) throws Exception {
    vertexCount = parseInt(br.readLine());
  }

  void readVertices(BufferedReader br) throws Exception {
    if (vertexCount > 0) {
      vertices = new Point3f[vertexCount];
      for (int i = 0; i < vertexCount; ++i) {
        String line = br.readLine();
        float x = parseFloat(line);
        float y = parseFloat(line, ichNextParse);
        float z = parseFloat(line, ichNextParse);
        vertices[i] = new Point3f(x, y, z);
      }
    }
  }

  void readPolygonCount(BufferedReader br) throws Exception {
    polygonCount = parseInt(br.readLine());
  }

  void readPolygonIndexes(BufferedReader br) throws Exception {
    if (polygonCount > 0) {
      polygonIndexes = new int[polygonCount][];
      for (int i = 0; i < polygonCount; ++i)
        polygonIndexes[i] = readPolygon(br);
    }
  }

  int[] readPolygon(BufferedReader br) throws Exception {
    int vertexIndexCount = parseInt(br.readLine());
    if (vertexIndexCount < 4)
      return null;
    int vertexCount = vertexIndexCount - 1;
    int[] vertices = new int[vertexCount];
    for (int i = 0; i < vertexCount; ++i)
      vertices[i] = parseInt(br.readLine());
    int extraVertex = parseInt(br.readLine());
    if (extraVertex != vertices[0]) {
      System.out.println("?Que?");
      return null;
    }
    return vertices;
  }

  int ichNextParse;
  
  float parseFloat(String str) {
    return parseFloatChecked(str, 0, str.length());
  }

  float parseFloat(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Float.NaN;
    return parseFloatChecked(str, ich, cch);
  }

  float parseFloat(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichStart, ichMax);
  }

  final static float[] decimalScale =
  {0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f};
  final static float[] tensScale =
  {10, 100, 1000, 10000, 100000, 1000000};

  float parseFloatChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    float value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      ++ich;
      negative = true;
    }
    ch = 0;
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      ++ich;
      digitSeen = true;
    }
    if (ch == '.') {
      int iscale = 0;
      while (++ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
        if (iscale < decimalScale.length)
          value += (ch - '0') * decimalScale[iscale];
        ++iscale;
        digitSeen = true;
      }
    }
    if (! digitSeen)
      value = Float.NaN;
    else if (negative)
      value = -value;
    if (ich < ichMax && (ch == 'E' || ch == 'e')) {
      if (++ich >= ichMax)
        return Float.NaN;
      ch = str.charAt(ich);
      if ((ch == '+') && (++ich >= ichMax))
        return Float.NaN;
      int exponent = parseIntChecked(str, ich, ichMax);
      if (exponent == Integer.MIN_VALUE)
        return Float.NaN;
      if (exponent > 0)
        value *= ((exponent < tensScale.length)
                  ? tensScale[exponent - 1]
                  : Math.pow(10, exponent));
      else if (exponent < 0)
        value *= ((-exponent < decimalScale.length)
                  ? decimalScale[-exponent - 1]
                  : Math.pow(10, exponent));
    } else {
      ichNextParse = ich; // the exponent code finds its own ichNextParse
    }
    //    System.out.println("parseFloat(" + str + "," + ichStart + "," +
    //                       ichMax + ") -> " + value);
    return value;
  }
  
  int parseInt(String str) {
    return parseIntChecked(str, 0, str.length());
  }

  int parseInt(String str, int ich) {
    int cch = str.length();
    if (ich >= cch)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ich, cch);
  }

  int parseInt(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (ichStart >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichStart, ichMax);
  }

  int parseIntChecked(String str, int ichStart, int ichMax) {
    boolean digitSeen = false;
    int value = 0;
    int ich = ichStart;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      negative = true;
      ++ich;
    }
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      digitSeen = true;
      ++ich;
    }
    if (! digitSeen)
      value = Integer.MIN_VALUE;
    else if (negative)
      value = -value;
    //    System.out.println("parseInt(" + str + "," + ichStart + "," +
    //                       ichMax + ") -> " + value);
    ichNextParse = ich;
    return value;
  }

}
