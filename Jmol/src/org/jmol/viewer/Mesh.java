/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

import org.jmol.util.ArrayUtil;
import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Mesh {
  
  final static String PREVIOUS_MESH_ID = "+PREVIOUS_MESH+";
  final static int FRONTLIT = 0;
  final static int BACKLIT = 1;
  final static int FULLYLIT = 2;

  Graphics3D g3d;
  
  String[] title = null;
  String thisID;
  boolean isValid = true;
  String scriptCommand;
 
  boolean visible = true;
  short colix;
  int vertexCount;
  Point3f[] vertices;
  short[] normixes;
  int polygonCount;  // negative number indicates hermite curve
  int[][] polygonIndexes = null;
  
  float scale = 1;
  int diameter;
  Point3f ptCenter = new Point3f(0,0,0);
  String meshType = null;
  
  int atomIndex = -1;
  int modelIndex = -1;  // for Isosurface and Draw
  int visibilityFlags;
  int[] modelFlags = null; //one per POLYGON for DRAW
  
  boolean showPoints = false;
  boolean drawTriangles = false;
  boolean fillTriangles = true;
  boolean showTriangles = false; //as distinct entitities
  boolean frontOnly = false;
  boolean isTwoSided = true;
  boolean isColorSolid = true;
  
  static int SEED_COUNT = 25; //optimized for cartoon mesh hermites
  
  Mesh() {}
  
  Mesh(String thisID, Graphics3D g3d, short colix) {
    if (PREVIOUS_MESH_ID.equals(thisID))
      thisID = null;
    this.thisID = thisID;
    this.g3d = g3d;
    this.colix = colix;
  }

  void clear(String meshType) {
    vertexCount = polygonCount = 0;
    scale = 1;
    vertices = null;
    polygonIndexes = null;
    showPoints = false;
    drawTriangles = false;
    fillTriangles = true;
    showTriangles = false; //as distinct entitities
    frontOnly = false;
    this.meshType = meshType;
  }

  int lighting = Mesh.FRONTLIT;
  
  void initialize(int lighting) {
    Vector3f[] vectorSums = new Vector3f[vertexCount];
    for (int i = vertexCount; --i >= 0;)
      vectorSums[i] = new Vector3f();
    sumVertexNormals(vectorSums, false);
    normixes = new short[vertexCount];
    initializeNormixes(lighting, vectorSums);
  }

  void initializeNormixes(int lighting, Vector3f[] vectorSums) {
    isTwoSided = (lighting == Mesh.FULLYLIT);
    normixes = new short[vertexCount];
    for (int i = vertexCount; --i >= 0;)
      normixes[i] = g3d.getNormix(vectorSums[i]);
    this.lighting = Mesh.FRONTLIT;
    setLighting(lighting);  
  }
  
  void setLighting(int lighting) {
     if (lighting == this.lighting)
      return;
    switch (lighting < 0 ? this.lighting : lighting) {
    case BACKLIT:
      if (this.lighting == FULLYLIT)
        setLighting(-1);
      for (int i = vertexCount; --i >= 0;)
        normixes[i] = g3d.getInverseNormix(normixes[i]);
      break;
    case FULLYLIT:
      if (lighting == BACKLIT)
        setLighting(-1); //reverses previous
      for (int i = vertexCount; --i >= 0;)
        normixes[i] = (short)~normixes[i];
      break;
    case FRONTLIT:
      setLighting(-1); //reverses previous
      break;
    }
    this.lighting = lighting;
  }
  
  void setTranslucent(boolean isTranslucent, float iLevel) {
    colix = Graphics3D.getColixTranslucent(colix, isTranslucent, iLevel);
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();

  void sumVertexNormals(Vector3f[] vectorSums, boolean haveCheckByte) {
    final Vector3f vNormalizedNormal = new Vector3f();
    int adjustment = (haveCheckByte ? 1 : 0);

    for (int i = polygonCount; --i >= 0;) {
      int[] pi = polygonIndexes[i];
      try {
        if (pi != null) {
          Graphics3D.calcNormalizedNormal(vertices[pi[0]], vertices[pi[1]],
              vertices[pi[2]], vNormalizedNormal, vAB, vAC);
          // general 10.? error here was not watching out for 
          // occurrances of intersection AT a corner, leading to
          // two points of triangle being identical
          float l = vNormalizedNormal.length();
          if( l > 0.9 && l < 1.1) //test for not infinity or -infinity or isNaN
          for (int j = pi.length - adjustment; --j >= 0;) {
            int k = pi[j];
            vectorSums[k].add(vNormalizedNormal);
          }
        }
      } catch (Exception e) {
      }
    }
  }

  void setPolygonCount(int polygonCount) {
    //Logger.debug("Mesh setPolygonCount" + polygonCount);
    this.polygonCount = polygonCount;
    if (polygonCount < 0)
      return;
    if (polygonIndexes == null || polygonCount > polygonIndexes.length)
      polygonIndexes = new int[polygonCount][];
  }

  int addVertexCopy(Point3f vertex) {
    if (vertexCount == 0)
      vertices = new Point3f[SEED_COUNT];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) ArrayUtil.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    //Logger.debug("mesh.addVertexCopy " + vertexCount + vertex +vertices[vertexCount]);
    return vertexCount++;
  }

  void addTriangle(int vertexA, int vertexB, int vertexC) {
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC};
  }

  void addQuad(int vertexA, int vertexB, int vertexC, int vertexD) {
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, vertexD};
  }

  void setColix(short colix) {
    this.colix = colix;
  }

  /*
   void checkForDuplicatePoints(float cutoff) {
   //not implemented
   float cutoff2 = cutoff * cutoff;
   for (int i = vertexCount; --i >= 0; )
   for (int j = i; --j >= 0; ) {
   float dist2 = vertices[i].distanceSquared(vertices[j]);
   if ((dist2 < cutoff2) && Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
   Logger.debug("Mesh.checkForDuplicates " +
   vertices[i] + "<->" + vertices[j] +
   " : " + Math.sqrt(dist2));
   }
   }
   }
   Hashtable getShapeDetail() {
   return null;
   }
   */

  String getState(String type) {
    StringBuffer s = new StringBuffer(type);
    s.append(fillTriangles ? " fill" : " noFill");
    s.append(drawTriangles ? " mesh" : " noMesh");
    s.append(showPoints ? " dots" : " noDots");
    s.append(frontOnly ? " frontOnly" : " notFrontOnly");
    if (showTriangles)
      s.append(" triangles");
    s.append(lighting == BACKLIT ? " backlit"
        : lighting == FULLYLIT ? " fullylit" : " frontlit");
    if (!visible)
      s.append(" off");
    return s.toString();
  }
}
