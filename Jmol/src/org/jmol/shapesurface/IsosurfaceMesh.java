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
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.util.ArrayUtil;
import org.jmol.viewer.Viewer;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.readers.JvxlReader;

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
    jvxlData.vContours = null;
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
    return vertexIndexes != null
          && !(Float.isNaN(vertexValues[iA = vertexIndexes[0]])
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
  
  public static final int CONTOUR_NPOLYGONS = 0;
  public static final int CONTOUR_BITSET = 1;
  public static final int CONTOUR_VALUE = 2;
  public static final int CONTOUR_COLOR = 3;
  public static final int CONTOUR_FDATA = 4;
  public static final int CONTOUR_POINTS = 5;

  /**
   * create a set of contour data. Each contour is a
   * Vector containing:
   *   0 Integer number of polygons (length of BitSet) 
   *   1 BitSet of critical triangles
   *   2 Float value
   *   3 int[] [colorArgb]
   *   4 StringBuffer containing encoded data for each segment:
   *     char type ('3', '6', '5') indicating which two edges
   *       of the triangle are connected: 
   *         '3' 0x011 AB-BC
   *         '5' 0x101 AB-CA
   *         '6' 0x110 BC-CA
   *     char fraction along first edge (jvxlFractionToCharacter)
   *     char fraction along second edge (jvxlFractionToCharacter)
   *   5- stream of pairs of points for rendering
   * 
   * @return contour vector set
   */
  Vector[] getContours() {
    int n = jvxlData.nContours;
    if (n <= 0)
      return null;
    if (jvxlData.vContours != null)
      return jvxlData.vContours;
    Vector[] vContours = new Vector[n];
    for (int i = 0; i < n; i++)
      vContours[i] = new Vector();
    float dv = (jvxlData.valueMappedToBlue - jvxlData.valueMappedToRed)
        / (n + 1);
    // n + 1 because we want n lines between n + 1 slices
    for (int i = 0; i < n; i++) {
      float value = jvxlData.valueMappedToRed + (i + 1) * dv;
      get3dContour(vContours[i], value, jvxlData.contourColors[i]);
    }
    return jvxlData.vContours = vContours;
  }
  
  private void get3dContour(Vector v, float value, int color) {
    BitSet bsContour = new BitSet(polygonCount);
    StringBuffer fData = new StringBuffer();
    v.add(new Integer(polygonCount));
    v.add(bsContour);
    v.add(new Float(value));
    v.add(new int[] {color});
    v.add(fData);
    for (int i = polygonCount; --i >= 0;) {
      if (!setABC(i))
        continue;
      int type = 0;
      float f1, f2;
      f1 = checkPt(iA, iB, value);
      if (!Float.isNaN(f1)) {
        type |= 1;
        v.add(getContourPoint(iA, iB, f1));
      }
      f2 = checkPt(iB, iC, value);
        if (!Float.isNaN(f2)) {
          if (type == 0)
            f1 = f2;
          type |= 2;
          v.add(getContourPoint(iB, iC, f2));
        }
      switch(type){
      case 0:
      case 3:
        break;
      default:
        f2 = checkPt(iC, iA, value);
        type |= 4;
        v.add(getContourPoint(iC, iA, f2));
      }
      if (type == 0)
        continue;
      bsContour.set(i);
      fData.append(type);
      fData.append(JvxlReader.jvxlFractionAsCharacter(f1));
      fData.append(JvxlReader.jvxlFractionAsCharacter(f2));
    }
  }

  private float checkPt(int i, int j, float f) {
    float f1, f2;
    return (((f1 = vertexValues[i]) <= f) == (f < (f2 = vertexValues[j]))
        ? (f - f1) / (f2 - f1) : Float.NaN);
  }

  private Point3f getContourPoint(int i, int j, float f) {
    Point3f pt = new Point3f();
    pt.set(vertices[j]);
    pt.sub(vertices[i]);
    pt.scale(f);
    pt.add(vertices[i]);
    return pt;
  }
}
