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
import java.util.Hashtable;

import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Mesh {
  Viewer viewer;

  String meshID;
  boolean visible = true;
  short colix;
  short[] vertexColixes;
  Graphics3D g3d;
    
  int vertexCount;
  Point3f[] vertices;
  short[] normixes;
  int polygonCount;
  int[][] polygonIndexes = null;
  
  int drawVertexCount;
  float scale = 1;
  Point3f ptCenter = new Point3f(0,0,0);
  Point3f ptCenters[];
  Vector3f axis = new Vector3f(1,0,0);
  Vector3f axes[];
  String meshType = null;
  String drawType = null;
  
  int[] visibilityFlags = null;
  
  boolean showPoints = false;
  boolean drawTriangles = false;
  boolean fillTriangles = true;
    
  Mesh(Viewer viewer, String meshID, Graphics3D g3d, short colix) {
    this.viewer = viewer;
    this.meshID = meshID;
    this.g3d = g3d;
    this.colix = colix;
  }

  void clear(String meshType) {
    vertexCount = polygonCount = 0;
    scale = 1;
    vertices = null;
    polygonIndexes = null;
    this.meshType = meshType;
  }

  void initialize() {
    Vector3f[] vectorSums = new Vector3f[vertexCount];
    for (int i = vertexCount; --i >= 0; )
      vectorSums[i] = new Vector3f();
    sumVertexNormals(vectorSums);
    normixes = new short[vertexCount];
    for (int i = vertexCount; --i >= 0; ) {
      normixes[i] = g3d.get2SidedNormix(vectorSums[i]);
      /*
        System.out.println("vectorSums[" + i + "]=" + vectorSums[i] +
        " -> normix:" + normixes[i]);
      */
    }
  }

  void allocVertexColixes() {
    if (vertexColixes == null) {
      vertexColixes = new short[vertexCount];
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] = colix;
    }
  }

  void setTranslucent(boolean isTranslucent) {
    colix = Graphics3D.setTranslucent(colix, isTranslucent);
    if (vertexColixes != null)
      for (int i = vertexCount; --i >= 0; )
        vertexColixes[i] =
          Graphics3D.setTranslucent(vertexColixes[i], isTranslucent);
  }

  void sumVertexNormals(Vector3f[] vectorSums) {
    final Vector3f vNormalizedNormal = new Vector3f();

    //FIXME uncaught exception! null pi;

    for (int i = polygonCount; --i >= 0; ) {
      int[] pi = polygonIndexes[i];
      if(pi != null) {
        g3d.calcNormalizedNormal(vertices[pi[0]],
                                 vertices[pi[1]],
                                 vertices[pi[2]],
                                 vNormalizedNormal);
        for (int j = pi.length; --j >= 0; ) {
          int k = pi[j];
          vectorSums[k].add(vNormalizedNormal);
        }
      }
    }
  }

  void setVertexCount(int vertexCount) {
    this.vertexCount = vertexCount;
    vertices = new Point3f[vertexCount];
  }

  void setPolygonCount(int polygonCount) {
    this.polygonCount = polygonCount;
    if (polygonIndexes == null || polygonCount > polygonIndexes.length)
      polygonIndexes = new int[polygonCount][];
  }

  int addVertexCopy(Point3f vertex) {
    if (vertexCount == 0)
      vertices = new Point3f[256];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) Util.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    return vertexCount++;
  }

  void addTriangle(int vertexA, int vertexB, int vertexC) {
    if (polygonCount == 0)
      polygonIndexes = new int[256][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) Util.doubleLength(polygonIndexes);
    int[] polygon = new int[] {vertexA, vertexB, vertexC};
    polygonIndexes[polygonCount++] = polygon;
  }

  void setColix(short colix) {
    this.colix = colix;
  }

  void checkForDuplicatePoints(float cutoff) {
    float cutoff2 = cutoff * cutoff;
    for (int i = vertexCount; --i >= 0; )
      for (int j = i; --j >= 0; ) {
        float dist2 = vertices[i].distanceSquared(vertices[j]);
        if (dist2 < cutoff2) {
          System.out.println("Mesh.checkForDuplicates " +
                             vertices[i] + "<->" + vertices[j] +
                             " : " + Math.sqrt(dist2));
        }
      }
  }
  
  Hashtable getShapeDetail() {
    return null;
  }
  
 final  boolean isPolygonDisplayable(int index) {
    return (visibilityFlags == null ||
        visibilityFlags[index] != 0); 
  }
  
  final int setPolygon(Point3f[] ptList, int nVertices, int nPoly) {
    /*
     * designed for Draw 
     * 
     * for now, just add all new vertices. It's simpler this way
     * though a bit redundant. We could reuse the fixed ones -- no matter
     * 
     */

    if (nVertices > 4)
      nVertices = 4;    // for now

    switch (nVertices) {
    case 1:
      drawType = "Point";
      break;
    case 2:
      drawType = "Line";
      break;
    default:
      drawType = "Plane";
    }
    drawVertexCount = nVertices;
    
    if (nVertices == 0) return nPoly;
    int nVertices0 = vertexCount;
    
    for (int i = 0; i < nVertices; i++) {
      addVertexCopy(ptList[i]);
    }
    int npoints = (nVertices < 3 ? 3 : nVertices);
    setPolygonCount(nPoly + 1);
    polygonIndexes[nPoly] = new int[npoints];
    for (int i = 0; i < npoints; i++) {
      polygonIndexes[nPoly][i] = nVertices0 + (i < nVertices ? i : nVertices - 1);
    }
    return nPoly + 1;
  }
  
  final void scaleDrawing(float newScale) {
    /*
     * allows for Draw to scale object
     * have to watch out for double-listed vertices
     * 
     */
    if (newScale == 0 || vertexCount == 0 || scale == newScale)
      return;
    Vector3f diff = new Vector3f();
    float f = newScale / scale;
    scale = newScale;
    int iptlast = -1;
    int ipt = 0;
    for (int i = polygonCount; --i >= 0;) {
      Point3f center = (ptCenters == null ? ptCenter : ptCenters[i]);
      iptlast = -1;
      for (int iV = polygonIndexes[i].length; --iV >= 0;) {
        ipt = polygonIndexes[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        diff.sub(vertices[ipt], center);
        diff.scale(f);
        diff.add(center);
        vertices[ipt].set(diff);
      }
    }
  }
  
  final void setCenter(int iModel) {
    Point3f center = new Point3f(0, 0, 0);
    int iptlast = -1;
    int ipt = 0;
    int n = 0;
    for (int i = polygonCount; --i >= 0;) {
      if (iModel >=0 && i != iModel)
        continue;
      iptlast = -1;
      for (int iV = polygonIndexes[i].length; --iV >= 0;) {
        ipt = polygonIndexes[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        center.add(vertices[ipt]);
        n++;
      }
      if (i == iModel || i == 0) {
        center.scale(1.0f / n);
        break;
      }
    }
    if (iModel < 0){
      ptCenter = center;
    } else {
      ptCenters[iModel] = center;
    }
  }

  final Point3f getSpinCenter(int modelIndex) {
    if (vertices == null)
      return null;
    return (ptCenters == null || modelIndex < 0 ? ptCenter : ptCenters[modelIndex]);
  }
  
  final Vector3f getSpinAxis(int modelIndex) {
    if (vertices == null)
      return null;
    return (ptCenters == null || modelIndex < 0 ? axis : axes[modelIndex]);
  }
  
  final void setAxes() {
    axis = new Vector3f(0, 0, 0);
    axes = new Vector3f[polygonCount];
    if (vertices == null)
      return;
    for (int i = polygonCount; --i >= 0;) {
      axes[i] = new Vector3f();
      if(drawVertexCount == 2) {
        axes[i].sub(vertices[polygonIndexes[i][0]], vertices[polygonIndexes[i][1]]);
      } else {      
        g3d.calcNormalizedNormal(vertices[polygonIndexes[i][0]],
                                 vertices[polygonIndexes[i][1]],
                                 vertices[polygonIndexes[i][2]],
                                 axes[i]);
      }
      axis.add(axes[i]);
    }
  }

}
