/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 10:52:44 -0500 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.g3d;

import javax.vecmath.Vector3f;
import org.jmol.util.Int2IntHash;

  /**
   * Constructs a canonical geodesic sphere of unit radius.
   *<p>
   * The Normix3D code quantizes arbitrary vectors to the vectors
   * of this unit sphere. normix values are then used for
   * high performance surface normal lighting
   *<p>
   * The vertices of the geodesic sphere can be used for constructing
   * vanderWaals and Connolly dot surfaces.
   *<p>
   * One geodesic sphere is constructed. It is a unit sphere
   * with radius of 1.0
   *<p>
   * Many times a sphere is constructed with lines of latitude and
   * longitude. With this type of rendering, the atom has north and
   * south poles. And the faces are not regularly shaped ... at the
   * poles they are triangles but elsewhere they are quadrilaterals.
   *<p>
   * A geodesic sphere is more appropriate for this type
   * of application. The geodesic sphere does not have poles and 
   * looks the same in all orientations ... as a sphere should. All
   * faces are triangles. Not equilateral, but close. 
   *<p>
   * The geodesic sphere is constructed by starting with an icosohedron, 
   * a platonic solid with 12 vertices and 20 equilateral triangles
   * for faces. The internal call to the private
   * method <code>quadruple</code> will
   * split each triangular face into 4 faces by creating a new vertex
   * at the midpoint of each edge. These midpoints are still in the
   * plane, so they are then 'pushed out' to the surface of the
   * enclosing sphere by normalizing their length back to 1.0
   *<p>
   * The sequence of vertex counts is 12, 42, 162, 642, 2562.
   * These are identified by 'levels', that run from 0 through 4;
   * The vertices
   * are stored so that when spheres are small they can choose to display
   * only the first n bits where n is one of the above vertex counts.
   *<code>
   * Faces + Vertices = Edges + 2
   *   Faces: 20, 80, 320, 1280, 5120, 20480
   *     start with 20 faces ... at each level multiply by 4
   *   Edges: 30, 120, 480, 1920, 7680, 30720
   *     start with 30 edges ... also multipy by 4 ... strange, but true
   *   Vertices: 12, 42, 162, 642, 2562, 10242
   *     start with 12 vertices and 30 edges.
   *     when you subdivide, each edge contributes one vertex
   *     12 + 30 = 42 vertices at the next level
   *     80 faces + 42 vertices - 2 = 120 edges at the next level
   *</code>
   *<p>
   * The vertices of the 'one true canonical sphere' are rotated to the
   * current molecular rotation at the beginning of the repaint cycle.
   * That way,
   * individual atoms only need to scale the unit vector to the vdw
   * radius for that atom.
   *<p>
   *
   * Notes 27 Sep 2005 <br />
   * If I were to switch the representation to staring with
   * a tetrahedron instead of an icosohedron we would get:
   *<code>
   * Faces: 4, 16, 64, 256, 1024
   * Edges: 6, 24, 96, 384, 1536
   * Vertices: 4, 10, 34, 130, 514
   *</code>
   * If I switched to face-centered normixes then I could efficiently
   * Regardless, I think that face-centered normixes would also reduce
   * ambiguity and would speed up the normal to normix process.
   *
   * I could also start with an octahedron that placed one triangle
   * in each 3D cartesian octant. That would push to 512 faces instead
   * of 256 faces, leaving me with shorts. But, it would be easier to quantize
   * at the first level because it would be based upon sign. And perhaps
   * it would be easier to take advantage of symmetry in the process of
   * converting from normal to normix.
   *
   * Notes 11 Oct 2005 <br />
   * Using an octahedron, the counts come up as follows:
   * Faces:  8, 32, 128, 512
   * Edges: 12, 48, 192, 768
   * Vrtxs:  6, 18,  66, 258
   */

class Geodesic3D2 {
  
  Graphics3D g3d;

  private final static boolean DUMP = true;

  final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  final static float oneFifth = 2 * (float)Math.PI / 5;
  final static float oneTenth = oneFifth / 2;
  
  // miguel 2005 01 11
  // within the context of this code, the term 'vertex' is used
  // to refer to a short which is an index into the tables
  // of vertex information.
  final static short[] faceVertexesOctant = {
    0, 1, 4,
    0, 5, 1,
    0, 3, 5,
    0, 4, 3,

    2, 1, 5,
    2, 5, 3,
    2, 3, 4,
    2, 4, 1,
  };

  // every vertex has 6 neighbors ... except at the beginning of the world
  final static short[] neighborVertexesOctant = {
    1, 4, 3, 5,-1,-1, // 0
    0, 5, 2, 4,-1,-1, // 1
    1, 5, 3, 4,-1,-1, // 2
    0, 4, 2, 5,-1,-1, // 3
    0, 1, 2, 3,-1,-1, // 4
    0, 3, 2, 1,-1,-1, // 5
  };

  /**
   * 5 levels, 0 through 4
   */
  final static int maxLevel = 3;
  static short[] vertexCounts;
  static short[][] neighborVertexesArrays;
  static short[][] faceVertexesArrays;
  static Vector3f[][] faceVectorsArrays;
  static Vector3f[] vertexVectors = new Vector3f[] {
    new Vector3f( 0, 1, 0),
    new Vector3f( 0, 0, 1),
    new Vector3f( 0,-1, 0),
    new Vector3f( 0, 0,-1),
    new Vector3f( 1, 0, 0),
    new Vector3f(-1, 0, 0)
  };



  Geodesic3D2(Graphics3D g3d) {
    this.g3d = g3d;
    if (vertexCounts == null)
      initialize();
    if (DUMP) {
      System.out.println("vertexVectors.length=" + vertexVectors.length);
      for (int i = 0; i < vertexVectors.length; ++i)
        System.out.println("" + i + " : " + vertexVectors[i]);
    }
  }

  private synchronized void initialize() {
    if (vertexCounts != null)
      return;
    vertexCounts = new short[maxLevel];
    neighborVertexesArrays = new short[maxLevel][];
    faceVertexesArrays = new short[maxLevel][];
    faceVectorsArrays = new Vector3f[maxLevel][];
    
    faceVertexesArrays[0] = faceVertexesOctant;
    neighborVertexesArrays[0] = neighborVertexesOctant;
    vertexCounts[0] = 6;
    
    for (int i = 0; i < maxLevel - 1; ++i)
      quadruple(i);
    
    if (DUMP) {
      for (int i = 0; i < maxLevel; ++i) {
        System.out.println("geodesic level " + i +
                           " vertexCount= " + getVertexCount(i) +
                           " faceCount=" + getFaceCount(i) +
                           " edgeCount=" + getEdgeCount(i));
      }
    }
    for (int i = 0; i < maxLevel; ++i)
      faceVectorsArrays[i] =
        buildFaceVectors(faceVertexesArrays[i], vertexVectors);
  }
  
  static int getVertexCount(int level) {
    return vertexCounts[level];
  }

  static Vector3f[] getVertexVectors() {
    return vertexVectors;
  }

  static int getFaceCount(int level) {
    return faceVertexesArrays[level].length / 3;
  }

  static int getEdgeCount(int level) {
    return getVertexCount(level) + getFaceCount(level) - 2;
  }

  static short[] getNeighborVertexes(int level) {
    return neighborVertexesArrays[level];
  }

  static short[] getFaceVertexes(int level) {
    return faceVertexesArrays[level];
  }

  static Vector3f[] getFaceVectors(int level) {
    return faceVectorsArrays[level];
  }


  private static short vertexNext;
  private static Int2IntHash htVertex;
    
  private final static boolean VALIDATE = true;

  private static void quadruple(int level) {
    if (DUMP)
      System.out.println("quadruple(" + level + ")");
    htVertex = new Int2IntHash();
    int oldVertexCount = vertexVectors.length;
    short[] oldFaceVertexes = faceVertexesArrays[level];
    int oldFaceVertexesLength = oldFaceVertexes.length;
    int oldFaceCount = oldFaceVertexesLength / 3;
    int oldEdgesCount = oldVertexCount + oldFaceCount - 2;
    int newVertexCount = oldVertexCount + oldEdgesCount;
    int newFaceCount = 4 * oldFaceCount;
    Vector3f[] newVertexVectors = new Vector3f[newVertexCount];
    System.arraycopy(vertexVectors, 0, newVertexVectors, 0, oldVertexCount);
    vertexVectors = newVertexVectors;

    short[] newFacesVertexes = new short[3 * newFaceCount];
    faceVertexesArrays[level + 1] = newFacesVertexes;
    short[] neighborVertexes = new short[6 * newVertexCount];
    neighborVertexesArrays[level + 1] = neighborVertexes;
    for (int i = neighborVertexes.length; --i >= 0; )
      neighborVertexes[i] = -1;

    vertexCounts[level + 1] = (short)newVertexCount;

    if (DUMP)
      System.out.println("oldVertexCount=" + oldVertexCount +
                         " newVertexCount=" + newVertexCount +
                         " oldFaceCount=" + oldFaceCount +
                         " newFaceCount=" + newFaceCount);
    
    vertexNext = (short)oldVertexCount;

    int iFaceNew = 0;
    for (int i = 0; i < oldFaceVertexesLength; ) {
      short iA = oldFaceVertexes[i++];
      short iB = oldFaceVertexes[i++];
      short iC = oldFaceVertexes[i++];
      short iAB = getVertex(iA, iB);
      short iBC = getVertex(iB, iC);
      short iCA = getVertex(iC, iA);
        
      newFacesVertexes[iFaceNew++] = iA;
      newFacesVertexes[iFaceNew++] = iAB;
      newFacesVertexes[iFaceNew++] = iCA;

      newFacesVertexes[iFaceNew++] = iB;
      newFacesVertexes[iFaceNew++] = iBC;
      newFacesVertexes[iFaceNew++] = iAB;

      newFacesVertexes[iFaceNew++] = iC;
      newFacesVertexes[iFaceNew++] = iCA;
      newFacesVertexes[iFaceNew++] = iBC;

      newFacesVertexes[iFaceNew++] = iCA;
      newFacesVertexes[iFaceNew++] = iAB;
      newFacesVertexes[iFaceNew++] = iBC;

      addNeighboringVertexes(neighborVertexes, iAB, iA);
      addNeighboringVertexes(neighborVertexes, iAB, iCA);
      addNeighboringVertexes(neighborVertexes, iAB, iBC);
      addNeighboringVertexes(neighborVertexes, iAB, iB);

      addNeighboringVertexes(neighborVertexes, iBC, iB);
      addNeighboringVertexes(neighborVertexes, iBC, iCA);
      addNeighboringVertexes(neighborVertexes, iBC, iC);

      addNeighboringVertexes(neighborVertexes, iCA, iC);
      addNeighboringVertexes(neighborVertexes, iCA, iA);
    }
    if (VALIDATE) {
      int vertexCount = vertexVectors.length;
      if (iFaceNew != newFacesVertexes.length)
        throw new NullPointerException();
      if (vertexNext != newVertexCount)
        throw new NullPointerException();
      for (int i = 0; i < 6; ++i) {
        for (int j = 0; j < 4; ++j) {
          int neighbor = neighborVertexes[i * 6 + j];
          if (neighbor < 0)
            throw new NullPointerException();
          if (neighbor >= vertexCount)
            throw new NullPointerException();
        }
        if (neighborVertexes[i * 6 + 4] != -1)
          throw new NullPointerException();
        if (neighborVertexes[i * 6 + 5] != -1)
          throw new NullPointerException();
      }
      for (int i = 6 * 6; i < neighborVertexes.length; ++i) {
        int neighbor = neighborVertexes[i];
        if (neighbor < 0)
          throw new NullPointerException();
        if (neighbor >= vertexCount)
          throw new NullPointerException();
      }
      for (int i = 0; i < newVertexCount; ++i) {
        int neighborCount = 0;
        for (int j = neighborVertexes.length; --j >= 0; )
          if (neighborVertexes[j] == i)
            ++neighborCount;
        if (i < 6) {
          if (neighborCount != 4)
            throw new NullPointerException();
        } else {
          if (neighborCount != 6)
            throw new NullPointerException();
        }
        int faceCount = 0;
        for (int j = newFacesVertexes.length; --j >= 0; )
          if (newFacesVertexes[j] == i)
            ++faceCount;
        if (i < 6) {
          if (faceCount != 4)
            throw new NullPointerException();
        } else {
          if (faceCount != 6)
            throw new NullPointerException();
        }
      }
    }
    htVertex = null;
  }

  private static void addNeighboringVertexes(short[] neighborVertexes,
                                             short v1, short v2) {
    //System.out.println("addNeighboringVertexes(...," + v1 + "," + v2 + ")");
    for (int i = v1 * 6, iMax = i + 6; i < iMax; ++i) {
      if (neighborVertexes[i] == v2)
        return;
      if (neighborVertexes[i] < 0) {
        neighborVertexes[i] = v2;
        for (int j = v2 * 6, jMax = j + 6; j < jMax; ++j) {
          if (neighborVertexes[j] == v1)
            return;
          if (neighborVertexes[j] < 0) {
            neighborVertexes[j] = v1;
            return;
          }
        }
      }
    }
    throw new NullPointerException();
  }

  /*
  short getNeighborVertex(int level, short vertex, int neighborIndex) {
    short[] neighborVertexes = neighborVertexesArrays[level];
    int offset = vertex * 6 + neighborIndex;
    return neighborVertexes[offset];
  }
  */
    
  private static short getVertex(short v1, short v2) {
    if (v1 > v2) {
      short t = v1;
      v1 = v2;
      v2 = t;
    }
    int hashKey = (v1 << 16) + v2;
    int vertex = htVertex.get(hashKey);
    if (vertex != Integer.MIN_VALUE)
      return (short)vertex;
    Vector3f newVertexVector = vertexVectors[vertexNext] = new Vector3f();
    newVertexVector.add(vertexVectors[v1], vertexVectors[v2]);
    newVertexVector.normalize();
    htVertex.put(hashKey, vertexNext);
    return vertexNext++;
  }

  static boolean isNeighborVertex(short vertex1, short vertex2, int level) {
    short[] neighborVertexes = neighborVertexesArrays[level];
    int offset1 = vertex1 * 6;
    for (int i = offset1 + (vertex1 < 6 ? 4 : 6); --i >= offset1; )
      if (neighborVertexes[i] == vertex2)
        return true;
    return false;
  }

  private static Vector3f[] buildFaceVectors(short[] faceVertexes,
                                      Vector3f[] vertexVectors) {
    int faceCount = faceVertexes.length / 3;
    Vector3f faceVectors[] = new Vector3f[faceCount];
    for (int i = 0, j = 0; i < faceCount; ++i, j += 3) {
      Vector3f v = faceVectors[i] = new Vector3f();
      v.add(vertexVectors[faceVertexes[j]],
            vertexVectors[faceVertexes[j + 1]]);
      v.add(vertexVectors[faceVertexes[j + 2]]);
      v.normalize();
    }
    return faceVectors;
  }
}
