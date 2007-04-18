/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-03 14:51:57 -0600 (Sun, 03 Dec 2006) $
 * $Revision: 6372 $
 *
 * Copyright (C) 2005  Miguel, The Jmol Development Team
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sf.net
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

package org.jmol.viewer;

import java.util.BitSet;

import javax.vecmath.Point3f;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.ArrayUtil;

class IsosurfaceMesh extends Mesh {
  JvxlData jvxlData = new JvxlData();

  boolean hideBackground;
  int realVertexCount;
  int vertexIncrement = 1;
  int firstViewableVertex;
  boolean hasGridPoints;

  float[] vertexValues;  
  short[] vertexColixes;
  
  IsosurfaceMesh(String thisID, Graphics3D g3d, short colix) {
    super(thisID, g3d, colix);
  }

  void clear(String meshType, boolean iAddGridPoints, boolean showTriangles) {
    super.clear(meshType);  
    vertexColixes = null;
    vertexValues = null;
    isColorSolid = true;
    realVertexCount = 0;
    firstViewableVertex = 0;
    hasGridPoints = iAddGridPoints;
    showPoints = iAddGridPoints;
    this.showTriangles = showTriangles;
    jvxlData.jvxlSurfaceData = "";
    jvxlData.jvxlEdgeData = "";
    jvxlData.jvxlColorData = "";
    surfaceSet = null;
    nSets = 0;
  }
  
  void allocVertexColixes() {
    if (vertexColixes == null) {
      vertexColixes = new short[vertexCount];
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] = colix;
    }
    isColorSolid = false;
  }

  public void setColorSchemeSets() {
    allocVertexColixes();
    int n = 0;
    for (int i = 0; i < surfaceSet.length; i++)
      if (surfaceSet[i] != null) {
        int c = Graphics3D.getColorArgb(++n);
        //System.out.println(n + " " + Integer.toHexString(c));
        short colix = Graphics3D.getColix(c);
        for (int j = 0; j < vertexCount; j++)
          if (surfaceSet[i].get(j))
            vertexColixes[j] = colix; //not black
      }
  }

  int addVertexCopy(Point3f vertex, float value) {
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = (float[]) ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  }

  void setTranslucent(boolean isTranslucent, float iLevel) {
    super.setTranslucent(isTranslucent, iLevel);
    if (vertexColixes != null)
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] =
          Graphics3D.getColixTranslucent(vertexColixes[i], isTranslucent, iLevel);
  }

  void updateSurfaceData(char isNaN) {
    invalidateTriangles();
    char[] chars = jvxlData.jvxlEdgeData.toCharArray();
    for (int i = 0; i < realVertexCount; i+= vertexIncrement)
      if (Float.isNaN(vertexValues[i]))
          chars[i] = isNaN;
    jvxlData.jvxlEdgeData = String.copyValueOf(chars);
  }
  
  void invalidateTriangles() {
    for (int i = polygonCount; --i >= 0;) {
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (Float.isNaN(vertexValues[iA]) || Float.isNaN(vertexValues[iB])
          || Float.isNaN(vertexValues[iC]))
        polygonIndexes[i] = null;
    }
  }
  
  void addTriangleCheck(int vertexA, int vertexB, int vertexC, int check) {
    if (vertexValues != null && (Float.isNaN(vertexValues[vertexA])||Float.isNaN(vertexValues[vertexB])||Float.isNaN(vertexValues[vertexC])))
      return;
    if (Float.isNaN(vertices[vertexA].x)||Float.isNaN(vertices[vertexB].x)||Float.isNaN(vertices[vertexC].x))
      return;
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, check};
  }

  
  public BitSet[] surfaceSet;
  public int[] vertexSets;
  public int nSets = 0;
  
  private boolean setsSuccessful;

  public BitSet[] getSurfaceSet() {
    return (surfaceSet == null ? getSurfaceSet(0) : surfaceSet);
  }

  void invalidateSurfaceSet(int i) {
    for (int j = surfaceSet[i].length(); --j >= 0;)
      if (surfaceSet[i].get(j))
        vertexValues[j] = Float.NaN;
    surfaceSet[i] = null;
  }

  public BitSet[] getSurfaceSet(int level) {
    if (level == 0) {
      surfaceSet = new BitSet[100];
      nSets = 0;
    }
    setsSuccessful = true;
    for (int i = 0; i < polygonCount; i++) {
      int[] p = polygonIndexes[i];
      int pt0 = findSet(p[0]);
      int pt1 = findSet(p[1]);
      int pt2 = findSet(p[2]);
      if (pt0 < 0 && pt1 < 0 && pt2 < 0) {
        createSet(p[0], p[1], p[2]);
        continue;
      }
      if (pt0 == pt1 && pt1 == pt2)
        continue;
      if (pt0 >= 0) {
        surfaceSet[pt0].set(p[1]);
        surfaceSet[pt0].set(p[2]);
        if (pt1 >= 0 && pt1 != pt0)
          mergeSets(pt0, pt1);
        if (pt2 >= 0 && pt2 != pt0 && pt2 != pt1)
          mergeSets(pt0, pt2);
        continue;
      }
      if (pt1 >= 0) {
        surfaceSet[pt1].set(p[0]);
        surfaceSet[pt1].set(p[2]);
        if (pt2 >= 0 && pt2 != pt1)
          mergeSets(pt1, pt2);
        continue;
      }
      surfaceSet[pt2].set(p[0]);
      surfaceSet[pt2].set(p[1]);
    }
    int n = 0;
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null)
        n++;
    BitSet[] temp = new BitSet[n];
    n = 0;
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null)
        temp[n++] = surfaceSet[i];
    nSets = n;
    surfaceSet = temp;
    if (!setsSuccessful && level < 2)
      getSurfaceSet(++level);
    if (level == 0) {
      vertexSets = new int[vertexCount];
      for (int i = 0; i < nSets; i++)
        for (int j = 0; j < vertexCount; j++)
          if (surfaceSet[i].get(j))
            vertexSets[j] = i;
    }
    return surfaceSet;
  }

  private int findSet(int vertex) {
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null && surfaceSet[i].get(vertex))
        return i;
    return -1;
  }

  private void createSet(int v1, int v2, int v3) {
    int i;
    for (i = 0; i < nSets; i++)
      if (surfaceSet[i] == null)
        break;
    if (i >= 100) {
      setsSuccessful = false;
      return;
    }
    if (i == nSets)
      nSets = i + 1;
    surfaceSet[i] = new BitSet();
    surfaceSet[i].set(v1);
    surfaceSet[i].set(v2);
    surfaceSet[i].set(v3);
  }

  private void mergeSets(int a, int b) {
    surfaceSet[a].or(surfaceSet[b]);
    surfaceSet[b] = null;
  }

  
}
