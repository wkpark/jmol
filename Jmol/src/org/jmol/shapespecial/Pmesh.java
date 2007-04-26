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
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;

public class Pmesh extends MeshFileCollection {

  boolean isOnePerLine;
  int modelIndex;

 public void initShape() {
    super.initShape();
    myType = "pmesh";
  }
    
 public void setProperty(String propertyName, Object value, BitSet bs) {
    //Logger.debug(propertyName + " "+ value);
    
    if ("init" == propertyName) {
      isFixed = false;
      modelIndex = -1;
      isOnePerLine = false;
      script = (String)value;    
      super.setProperty("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
      //fall through to MeshCollection "init"
    }
    
    if ("modelIndex" == propertyName) {
      modelIndex = ((Integer)value).intValue();
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
      BufferedReader br = (BufferedReader)value;
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.clear("pmesh");
      currentMesh.isValid = readPmesh(br);
      if(currentMesh.isValid) {
        currentMesh.initialize(JmolConstants.FULLYLIT);
        currentMesh.visible = true;
        currentMesh.title = title;
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

  boolean readPmesh(BufferedReader br) {
    //Logger.debug("Pmesh.readPmesh(" + br + ")");
    try {
      readVertexCount(br);
      //Logger.debug("vertexCount=" + currentMesh.vertexCount);
      readVertices(br);
      //Logger.debug("vertices read");
      readPolygonCount(br);
      //Logger.debug("polygonCount=" + currentMesh.polygonCount);
      readPolygonIndexes(br);
      //Logger.debug("polygonIndexes read");
    } catch (Exception e) {
//Logger.debug("Pmesh.readPmesh exception:" + e);
      Logger.error("pmesh ERROR: read exception: " + e);
      return false;
    }
    return true;
  }

  void readVertexCount(BufferedReader br) throws Exception {
    currentMesh.vertexCount = 0;
    currentMesh.vertices = new Point3f[0];
    currentMesh.vertexCount = (currentMesh.vertices = 
      new Point3f[parseInt(br.readLine())]).length;
  }

  void readVertices(BufferedReader br) throws Exception {
    if (currentMesh.vertexCount <= 0)
      return;
    if (isOnePerLine) {
      for (int i = 0; i < currentMesh.vertexCount; ++i) {
        float x = parseFloat(br.readLine());
        float y = parseFloat(br.readLine());
        float z = parseFloat(br.readLine());
        currentMesh.vertices[i] = new Point3f(x, y, z);
      }
    } else {
      for (int i = 0; i < currentMesh.vertexCount; ++i) {
        String line = br.readLine();
        float x = parseFloat(line);
        float y = parseFloat();
        float z = parseFloat();
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
    if (vertexIndexCount < 2) {
      Logger.error("pmesh ERROR: each polygon must have at least two verticies indicated");
      currentMesh.isValid = false;
      return null;
    }
    int vertexCount = vertexIndexCount - 1;
    int nVertex = (vertexCount < 3 ? 3 : vertexCount);
    int[] vertices = new int[nVertex];
    for (int i = 0; i < vertexCount; ++i)
      vertices[i] = parseInt(br.readLine());
    for (int i = vertexCount; i < nVertex; ++i)
      vertices[i] = vertices[i - 1];
    int extraVertex = parseInt(br.readLine());
    if (extraVertex != vertices[0]) {
//Logger.debug("?Que? polygon is not complete");
      Logger.error("pmesh ERROR: last polygon point reference (" + extraVertex + ") is not the same as the first (" + vertices[0] + ")");
      currentMesh.isValid = false;
      throw new NullPointerException();
    }
    return vertices;
  }
  
}
