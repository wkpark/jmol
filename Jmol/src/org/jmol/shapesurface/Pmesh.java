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

package org.jmol.shapesurface;

import java.util.BitSet;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;

import javax.vecmath.Point3f;

import org.jmol.shape.Mesh;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BinaryDocument;
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
  private boolean isBinary;
  private boolean iHaveModelIndex;
  String pmeshError;
  
  private final static String PMESH_BINARY_MAGIC_NUMBER = "PM" + '\1' + '\0';

  public void initShape() {
    super.initShape();
    myType = "pmesh";
  }

  public void setProperty(String propertyName, Object value, BitSet bs) {
    //Logger.debug(propertyName + " "+ value);

    if ("init" == propertyName) {
      br = null;
      doc = null;
      pmeshError = null;
      isFixed = false;
      isBinary = false;
      isOnePerLine = false;
      script = (String) value;
      getModelIndex(script);
      super.setProperty("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
      //fall through to MeshCollection "init"
    }

    if ("modelIndex" == propertyName) {
      if (!iHaveModelIndex)
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

    if ("fileData" == propertyName) {
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.clear("pmesh");
      if (value instanceof BufferedReader) {
        br = (BufferedReader) value;
      } else {
        doc = new PmeshBinaryDocument(new BufferedInputStream(
            (InputStream) value));
        if (!(isBinary = doc.readHeader()))
          return;
      }
      currentMesh.isValid = readPmesh();
      closeReaders();
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

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = meshCount; --i >= 0;) {
        if (meshes[i] == null)
          continue;
        if (meshes[i].modelIndex == modelIndex) {
           meshCount--;
            if (meshes[i] == currentMesh) 
              currentMesh = null;
            meshes = (Mesh[]) ArrayUtil.deleteElements(meshes, i, 1);
        } else if (meshes[i].modelIndex > modelIndex) {
          meshes[i].modelIndex--;
        }
      }
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
    if (property.startsWith("checkMagicNumber:"))
      return (property.indexOf(PMESH_BINARY_MAGIC_NUMBER) >= 0 ? Boolean.TRUE : Boolean.FALSE);
    return super.getProperty(property, index);
  }

  BufferedReader br;
  PmeshBinaryDocument doc;

  private boolean readPmesh() {
    try {
      if (!readVertexCount())
        return false;
      Logger.debug("vertexCount=" + currentMesh.vertexCount);
      if (!readVertices())
        return false;
      Logger.debug("vertices read");
      if (!readPolygonCount())
        return false;
      Logger.debug("polygonCount=" + currentMesh.polygonCount);
      if (!readPolygonIndexes())
        return false;
      Logger.debug("polygonIndexes read");
      if (currentMesh.polygonCount == 0) {
        currentMesh.setPolygonCount(1);
        currentMesh.polygonIndexes[0] = new int[] {0, 0, 0};
      }
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = "pmesh ERROR: read exception: " + e;
      return false;
    }
    return true;
  }

  private void closeReaders() {
    try {
      if (doc != null)
        doc.close();
      if (br != null)
        br.close();
    } catch (Exception e) {
    }
    doc = null;
    br = null;
  }
  
  private int getInt() throws Exception {
    return (isBinary ? doc.readInt() : parseInt(br.readLine()));
  }
  
  private boolean readVertexCount() throws Exception {
    pmeshError = "pmesh ERROR: vertex count must be positive";
    currentMesh.vertexCount = 0;
    currentMesh.vertices = new Point3f[0];
    int n = (isBinary ? doc.getVertexCount() : getInt());
    if (n <= 0) {
      pmeshError += " (" + n + ")";
      return false;
    }
    currentMesh.vertexCount = n;
    currentMesh.vertices = new Point3f[n];
    pmeshError = null;
    return true;
  }

  private boolean readVertices() throws Exception {
    pmeshError = "pmesh ERROR: invalid vertex list";
    if (isBinary) {
      doc.readPoint3fArray(currentMesh.vertices);
    } else if (isOnePerLine) {
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

  private boolean readPolygonCount() throws Exception {
    int n = (isBinary ? doc.getPolygonCount() : parseInt(br.readLine()));
    if (n >= 0) {
      currentMesh.setPolygonCount(n);
    }
    else
      pmeshError = "pmesh ERROR: polygon count must be >= 0 (" + n + ")";
    return (n >= 0);
  }

  private boolean readPolygonIndexes() throws Exception {
    for (int i = 0; i < currentMesh.polygonCount; ++i)
      if ((currentMesh.polygonIndexes[i] = readPolygon(i)) == null)
        return false;
    return true;
  }

  private int[] readPolygon(int iPoly) throws Exception {
    int vertexIndexCount = getInt();
    int vertexCount = (isBinary ? vertexIndexCount : vertexIndexCount - 1);
    if (vertexCount < 1 || vertexCount > 4) {
      pmeshError = "pmesh ERROR: bad polygon (must have 1-4 vertices) at #"
          + (iPoly + 1);
      return null;
    }
    int nVertex = (vertexCount < 3 ? 3 : vertexCount);
    int[] vertices = new int[nVertex];
    for (int i = 0; i < vertexCount; ++i)
      if ((vertices[i] = getInt()) < 0
          || vertices[i] >= currentMesh.vertexCount) {
        pmeshError = "pmesh ERROR: invalid vertex index: " + vertices[i];
        return null;
      }
    for (int i = vertexCount; i < nVertex; ++i)
      vertices[i] = vertices[i - 1];
    int extraVertex;
    if (isBinary || (extraVertex = getInt()) == vertices[0])
      return vertices;
    pmeshError = "pmesh ERROR: last polygon point reference (" + extraVertex
        + ") is not the same as the first (" + vertices[0] + ") for polygon "
        + (iPoly + 1);
    return null;
  }

  class PmeshBinaryDocument extends BinaryDocument {
    
    private int vertexCount;
    private int polygonCount;
    
    PmeshBinaryDocument(BufferedInputStream bis) {
      isRandom = false;
      stream = new DataInputStream(bis);
    }
    
    boolean readHeader() {
      /*
       *  4 bytes: P M \1 \0 
       *  1 byte: \0 for bigEndian
       *  3 bytes: reserved
       *  4 bytes: (int) vertexCount
       *  4 bytes: (int) polygonCount
       * 64 bytes: reserved
       *  ------------------------------
       *  float[vertexCount*3]vertices {x,y,z}
       *  [polygonCount] polygons 
       *  --each polygon--
       *    4 bytes: (int)nVertices (1,2,3, or 4)
       *    [4 bytes * nVertices] int[nVertices]
       *    
       * note that there is NO redundant extra vertex in this format 
       *
       */
      
      pmeshError = "could not read binary Pmesh file header";
      byte[] magicNumber = new byte[8];     
      try {
        readByteArray(magicNumber, 0, 8);
        if (!(new String(magicNumber)).startsWith(PMESH_BINARY_MAGIC_NUMBER))
          return false;
        isBigEndian = (magicNumber[5] == 0);
        vertexCount = readInt();
        polygonCount = readInt();
        byte[] reserved = new byte[64];
        readByteArray(reserved, 0, 64);
      } catch (Exception e) {
        pmeshError += " " + e.getMessage();
        close();
        return false;
      }
      pmeshError = null;
      return true;
    }

    void close() {
      try {
        stream.close();
      } catch (Exception e) {
      }
    }
    
    int getPolygonCount() {
      return polygonCount;
    }
    
    int getVertexCount() {
      return vertexCount;
    }
  }
}
