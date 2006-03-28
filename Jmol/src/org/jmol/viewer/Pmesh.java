/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

package org.jmol.viewer;

import java.util.BitSet;
import java.io.BufferedReader;
import javax.vecmath.Point3f;

class Pmesh extends MeshCollection {

  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("bufferedreader" == propertyName) {
      BufferedReader br = (BufferedReader)value;
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.clear();
      readPmesh(br);
      currentMesh.initialize();
      currentMesh.visible = true;
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  /*
   * vertexCount
   * x.xx y.yy z.zz {vertices}
   * polygonCount
   *
   */

  void readPmesh(BufferedReader br) {
    //    System.out.println("Pmesh.readPmesh(" + br + ")");
    try {
      readVertexCount(br);
      //      System.out.println("vertexCount=" + currentMesh.vertexCount);
      readVertices(br);
      //      System.out.println("vertices read");
      readPolygonCount(br);
      //      System.out.println("polygonCount=" + currentMesh.polygonCount);
      readPolygonIndexes(br);
      //      System.out.println("polygonIndexes read");
    } catch (Exception e) {
      System.out.println("Pmesh.readPmesh exception:" + e);
    }
  }

  void readVertexCount(BufferedReader br) throws Exception {
    currentMesh.setVertexCount(parseInt(br.readLine()));
  }

  void readVertices(BufferedReader br) throws Exception {
    if (currentMesh.vertexCount > 0) {
      for (int i = 0; i < currentMesh.vertexCount; ++i) {
        String line = br.readLine();
        float x = parseFloat(line);
        float y = parseFloat(line, ichNextParse);
        float z = parseFloat(line, ichNextParse);
        currentMesh.vertices[i] = new Point3f(x, y, z);
      }
    }
  }

  void readPolygonCount(BufferedReader br) throws Exception {
    currentMesh.setPolygonCount(parseInt(br.readLine()));
  }

  void readPolygonIndexes(BufferedReader br) throws Exception {
    if (currentMesh.polygonCount > 0) {
      for (int i = 0; i < currentMesh.polygonCount; ++i)
        currentMesh.polygonIndexes[i] = readPolygon(br);
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
      System.out.println("?Que? polygon is not complete");
      throw new NullPointerException();
    }
    return vertices;
  }
}
