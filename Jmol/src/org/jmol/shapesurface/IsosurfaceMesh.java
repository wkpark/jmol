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

package org.jmol.shapesurface;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.util.ArrayUtil;
import org.jmol.viewer.Viewer;
import org.jmol.jvxl.data.JvxlData;

import org.jmol.jvxl.calc.MarchingSquares;
import org.jmol.shape.Mesh;

public class IsosurfaceMesh extends Mesh {
  JvxlData jvxlData = new JvxlData();
  public boolean hideBackground;
  public int realVertexCount;
  public int vertexIncrement = 1;
  public int firstRealVertex = -1;
  public boolean hasGridPoints;
  float calculatedArea = Float.NaN;
  float calculatedVolume = Float.NaN;

  public float[] vertexValues;  
  public short[] vertexColixes;
  
  IsosurfaceMesh(String thisID, Graphics3D g3d, short colix) {
    super(thisID, g3d, colix);
    haveCheckByte = true;
    jvxlData.version = Viewer.getJmolVersion();
  }

  void clear(String meshType, boolean iAddGridPoints, boolean showTriangles) {
    super.clear(meshType);  
    vertexColixes = null;
    vertexValues = null;
    assocGridPointMap = null;
    assocGridPointNormals = null;
    vertexSets = null;
    isColorSolid = true;
    firstRealVertex = -1;
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
    int n = 2; //skipping the first two
    for (int i = 0; i < surfaceSet.length; i++)
      if (surfaceSet[i] != null) {
        int c = Graphics3D.getColorArgb(n++);
        //System.out.println(n + " " + Integer.toHexString(c));
        short colix = Graphics3D.getColix(c);
        for (int j = 0; j < vertexCount; j++)
          if (surfaceSet[i].get(j))
            vertexColixes[j] = colix; //not black
      }
  }

  Hashtable assocGridPointMap ;
  Hashtable assocGridPointNormals;

  int addVertexCopy(Point3f vertex, float value, int assocVertex, boolean associateNormals) {
    //if (vertexCount == 1619 || vertexCount == 320)
      //System.out.println(vertex);
    int vPt = addVertexCopy(vertex, value);
    switch (assocVertex) {
    case MarchingSquares.CONTOUR_POINT:
      if (firstRealVertex < 0)
        firstRealVertex = vPt;
      break;
    case MarchingSquares.VERTEX_POINT:
      hasGridPoints = true;
      break;
    case MarchingSquares.EDGE_POINT:
      vertexIncrement = 3;
      break;
    default:
      if (firstRealVertex < 0)
        firstRealVertex = vPt;
      if (associateNormals) {
        if (assocGridPointMap == null) {
          assocGridPointMap = new Hashtable();
          assocGridPointNormals = new Hashtable();
        }
        Integer key = new Integer(assocVertex);
        assocGridPointMap.put(new Integer(vPt), key);
        if (!assocGridPointNormals.containsKey(key))
          assocGridPointNormals.put(key, new Vector3f(0, 0, 0));
      }
    }
    return vPt;
  }

  int addVertexCopy(Point3f vertex, float value) {
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = (float[]) ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  }

  public void setTranslucent(boolean isTranslucent, float iLevel) {
    super.setTranslucent(isTranslucent, iLevel);
    if (vertexColixes != null)
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] =
          Graphics3D.getColixTranslucent(vertexColixes[i], isTranslucent, iLevel);
  }

  void addTriangleCheck(int vertexA, int vertexB, int vertexC, int check) {
    if (vertexValues != null && (Float.isNaN(vertexValues[vertexA])||Float.isNaN(vertexValues[vertexB])||Float.isNaN(vertexValues[vertexC])))
      return;
    if (Float.isNaN(vertices[vertexA].x)||Float.isNaN(vertices[vertexB].x)||Float.isNaN(vertices[vertexC].x))
      return;
    //System.out.println("adding triangle " + vertexA + vertices[vertexA] + " " + vertexB +  vertices[vertexB] + " " + vertexC + vertices[vertexC]);
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, check};
  }
  
  void invalidateTriangles() {
    for (int i = polygonCount; --i >= 0;)
      if (!setABC(i))
        polygonIndexes[i] = null;
  }
  
  private int iA, iB, iC;
  
  private boolean setABC(int i) {
    int[] vertexIndexes = polygonIndexes[i];
    return vertexIndexes != null && !(Float.isNaN(vertexValues[iA = vertexIndexes[0]]) 
        || Float.isNaN(vertexValues[iB = vertexIndexes[1]])
        || Float.isNaN(vertexValues[iC = vertexIndexes[2]]));
  }
  
  float calculateArea() {
    if (!Float.isNaN(calculatedArea))
      return calculatedArea;
    double v = 0;
    for (int i = polygonCount; --i >= 0;) {
      if (!setABC(i)) 
        continue;
      vAB.sub(vertices[iB], vertices[iA]);
      vAC.sub(vertices[iC], vertices[iA]);
      vTemp.cross(vAB, vAC);
      v += vTemp.length();
    }
    return calculatedArea = (float) (v / 2);
  }

  float calculateVolume() {
    if (!Float.isNaN(calculatedVolume))
      return calculatedVolume;
    double v = 0;
    for (int i = polygonCount; --i >= 0;) {
      if (!setABC(i)) 
        continue;
      vAB.set(vertices[iB]);
      vAC.set(vertices[iC]);
      vTemp.cross(vAB, vAC);
      vAC.set(vertices[iA]);
      v += vAC.dot(vTemp);
    }
    return calculatedVolume = (float) (v / 6);
  }

  public BitSet[] surfaceSet;
  public int[] vertexSets;
  public int nSets = 0;
  
  public void sumVertexNormals(Vector3f[] vectorSums) {
    super.sumVertexNormals(vectorSums);
    /* 
     * OK, so if there is an associated grid point (because the 
     * point is so close to one), we now declare that associated
     * point to be used for the vectorSum instead of a new, 
     * independent one for the point itself.
     *  
     *  Bob Hanson, 05/2006
     *  
     *  having 2-sided normixes is INCOMPATIBLE with this when not a plane 
     *  
     */
    if (assocGridPointMap != null) {
      Enumeration e = assocGridPointMap.keys();
      while (e.hasMoreElements()) {
        Integer I = (Integer) e.nextElement();
        ((Vector3f) assocGridPointNormals.get(assocGridPointMap.get(I)))
            .add(vectorSums[I.intValue()]);
      }
      e = assocGridPointMap.keys();
      while (e.hasMoreElements()) {
        Integer I = (Integer) e.nextElement();
        vectorSums[I.intValue()] = ((Vector3f) assocGridPointNormals
            .get(assocGridPointMap.get(I)));
      }
    }
  }
}
