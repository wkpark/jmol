/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-16 18:06:32 -0500 (Mon, 16 Apr 2007) $
 * $Revision: 7418 $
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

package org.jmol.shapespecial;

import java.util.BitSet;
import java.io.BufferedReader;
import javax.vecmath.Point3f;

import org.jmol.shape.MeshFileCollection;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;

public class Pmesh extends MeshFileCollection {

  /* 
   * Example:
   * 

100
3.0000 3.0000 1.0000
2.3333 3.0000 1.0000
...(98 more like this)
81
5
0
10
11
1
0
...(80 more sets like this)

    * The first line defines the number of grid points 
    *   defining the surface (integer, n)
    * The next n lines define the Cartesian coordinates 
    *   of each of the grid points (n lines of x, y, z floating point data points)
    * The next line specifies the number of polygons, m, to be drawn (81 in this case).
    * The next m sets of numbers, one number per line, 
    *   define the polygons. In each set, the first number, p, specifies 
    *   the number of points in each set. Currently this number must be either 
    *   4 (for triangles) or 5 (for quadrilaterals). The next p numbers specify 
    *   indexes into the list of data points (starting with 0). 
    *   The first and last of these numbers must be identical in order to 
    *   "close" the polygon.
    * 
   */
  
  private boolean isOnePerLine;
  private int modelIndex;
  private String pmeshError;

  public void initShape() {
    super.initShape();
    myType = "pmesh";
  }

  public void setProperty(String propertyName, Object value, BitSet bs) {
    //Logger.debug(propertyName + " "+ value);

    if ("init" == propertyName) {
      pmeshError = null;
      isFixed = false;
      modelIndex = -1;
      isOnePerLine = false;
      script = (String) value;
      super.setProperty("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
      //fall through to MeshCollection "init"
    }

    if ("modelIndex" == propertyName) {
      modelIndex = ((Integer) value).intValue();
      return;
    }

    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      setModelIndex(-1, modelIndex = -1);
      return;
    }

    if ("bufferedReaderOnePerLine" == propertyName) {
      isOnePerLine = true;
      propertyName = "bufferedReader";
    }

    if ("bufferedReader" == propertyName) {
      BufferedReader br = (BufferedReader) value;
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.clear("pmesh");
      currentMesh.isValid = readPmesh(br);
      if (currentMesh.isValid) {
        currentMesh.initialize(JmolConstants.FULLYLIT);
        currentMesh.visible = true;
        currentMesh.title = title;
      } else {
        Logger.error(pmeshError);
      }
      setModelIndex(-1, modelIndex);
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

  public Object getProperty(String property, int index) {
    if (property.equals("pmeshError"))
      return pmeshError;
    return super.getProperty(property, index);
  }

  private boolean readPmesh(BufferedReader br) {
    try {
      if (!readVertexCount(br))
        return false;
      Logger.debug("vertexCount=" + currentMesh.vertexCount);
      if (!readVertices(br))
        return false;
      Logger.debug("vertices read");
      if (!readPolygonCount(br))
        return false;
      Logger.debug("polygonCount=" + currentMesh.polygonCount);
      if (!readPolygonIndexes(br))
        return false;
      Logger.debug("polygonIndexes read");
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = "pmesh ERROR: read exception: " + e;
      return false;
    }
    return true;
  }

  private boolean readVertexCount(BufferedReader br) throws Exception {
    pmeshError = "pmesh ERROR: vertex count must be positive";
    currentMesh.vertexCount = 0;
    currentMesh.vertices = new Point3f[0];
    int n = parseInt(br.readLine());
    if (n <= 0) {
      pmeshError += " (" + n + ")";
      return false;
    }
    currentMesh.vertices = new Point3f[n];
    currentMesh.vertexCount = n;
    pmeshError = null;
    return true;
  }

  private boolean readVertices(BufferedReader br) throws Exception {
    pmeshError = "pmesh ERROR: invalid vertex list";
    if (isOnePerLine) {
      for (int i = 0; i < currentMesh.vertexCount; ++i) {
        float x = parseFloat(br.readLine());
        float y = parseFloat(br.readLine());
        float z = parseFloat(br.readLine());
        currentMesh.vertices[i] = new Point3f(x, y, z);
      }
    } else {
      for (int i = 0; i < currentMesh.vertexCount; ++i) {
        line = br.readLine();
        float x = parseFloat(line);
        float y = parseFloat();
        float z = parseFloat();
        currentMesh.vertices[i] = new Point3f(x, y, z);
      }
    }
    pmeshError = null;
    return true;
  }

  private boolean readPolygonCount(BufferedReader br) throws Exception {
    int n = parseInt(br.readLine());
    if (n > 0)
      currentMesh.setPolygonCount(n);
    else
      pmeshError = "pmesh ERROR: polygon count must be > 0 (" + n + ")";
    return (n > 0);
  }

  private boolean readPolygonIndexes(BufferedReader br) throws Exception {
    for (int i = 0; i < currentMesh.polygonCount; ++i)
      if ((currentMesh.polygonIndexes[i] = readPolygon(i, br)) == null)
        return false;
    return true;
  }

  private int[] readPolygon(int iPoly, BufferedReader br) throws Exception {
    int vertexIndexCount = parseInt(br.readLine());
    if (vertexIndexCount < 2) {
      pmeshError = "pmesh ERROR: each polygon must have at least two verticies indicated -- polygon "
          + (iPoly + 1);
      return null;
    }
    int vertexCount = vertexIndexCount - 1;
    int nVertex = (vertexCount < 3 ? 3 : vertexCount);
    int[] vertices = new int[nVertex];
    for (int i = 0; i < vertexCount; ++i)
      if ((vertices[i] = parseInt(br.readLine())) < 0
          || vertices[i] >= currentMesh.vertexCount) {
        pmeshError = "pmesh ERROR: invalid vertex index: " + vertices[i];
        return null;
      }
    for (int i = vertexCount; i < nVertex; ++i)
      vertices[i] = vertices[i - 1];
    int extraVertex = parseInt(br.readLine());
    if (extraVertex != vertices[0]) {
      pmeshError = "pmesh ERROR: last polygon point reference (" + extraVertex
          + ") is not the same as the first (" + vertices[0] + ") for polygon "
          + (iPoly + 1);
      return null;
    }
    return vertices;
  }

}
