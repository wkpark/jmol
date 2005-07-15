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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;
import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Mesh {
  String meshID;
  boolean visible = true;
  short colix;
  Graphics3D g3d;
    
  int vertexCount;
  Point3f[] vertices;
  short[] normixes;
  int polygonCount;
  int[][] polygonIndexes;

  boolean showPoints = false;
  boolean drawTriangles = false;
  boolean fillTriangles = true;
    
  Mesh(String meshID, Graphics3D g3d, short colix) {
    this.meshID = meshID;
    this.g3d = g3d;
    this.colix = colix;
  }


  void clear() {
    vertexCount = polygonCount = 0;
    vertices = null;
    polygonIndexes = null;
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

  void sumVertexNormals(Vector3f[] vectorSums) {
    final Vector3f vNormalizedNormal = new Vector3f();

    for (int i = polygonCount; --i >= 0; ) {
      int[] pi = polygonIndexes[i];
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

  void setVertexCount(int vertexCount) {
    this.vertexCount = vertexCount;
    vertices = new Point3f[vertexCount];
  }

  void setPolygonCount(int polygonCount) {
    this.polygonCount = polygonCount;
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

}
