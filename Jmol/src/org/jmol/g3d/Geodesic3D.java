/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.g3d;

import javax.vecmath.Vector3f;
import java.util.Hashtable;

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
   * faces are equilateral triangles.
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
   *<p>
   * The vertices of the 'one true canonical sphere' are rotated to the
   * current molecular rotation at the beginning of the repaint cycle.
   * That way,
   * individual atoms only need to scale the unit vector to the vdw
   * radius for that atom.
   */

class Geodesic3D {
  
  Graphics3D g3d;

  private final static boolean DUMP = false;

  final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  final static float oneFifth = 2 * (float)Math.PI / 5;
  final static float oneTenth = oneFifth / 2;
  
  // miguel 2005 01 11
  // within the context of this code, the term 'vertex' is used
  // to refer to a short which is an index into the tables
  // of vertex information.
  final static short[] faceVertexesIcosahedron = {
    0, 1, 2,
    0, 2, 3,
    0, 3, 4,
    0, 4, 5,
    0, 5, 1,

    1, 6, 2,
    2, 7, 3,
    3, 8, 4,
    4, 9, 5,
    5, 10, 1,

    6, 1, 10,
    7, 2, 6,
    8, 3, 7,
    9, 4, 8,
    10, 5, 9,

    11, 6, 10,
    11, 7, 6,
    11, 8, 7,
    11, 9, 8,
    11, 10, 9,
  };

  // every vertex has 6 neighbors ... except at the beginning of the world
  final static short[] neighborVertexesIcosahedron = {
    1, 2, 3, 4, 5,-1, // 0
    0, 5,10, 6, 2,-1, // 1
    0, 1, 6, 7, 3,-1, // 2
    0, 2, 7, 8, 4,-1, // 3

    0, 3, 8, 9, 5,-1, // 4
    0, 4, 9,10, 1,-1, // 5
    1,10,11, 7, 2,-1, // 6
    2, 6,11, 8, 3,-1, // 7

    3, 7,11, 9, 4,-1, // 8
    4, 8,11,10, 5,-1, // 9
    5, 9,11, 6, 1,-1, // 10
    6, 7, 8, 9,10,-1 // 11
  };

  /**
   * 5 levels, 0 through 4
   */
  final static int maxLevel = 4;
  static short[] vertexCounts;
  static short[][] neighborVertexesArrays;
  static short[][] faceVertexesArrays;
  static short[][] faceNormixesArrays;
  static Vector3f[] vertexVectors;

  Geodesic3D(Graphics3D g3d) {
    this.g3d = g3d;
    if (vertexCounts == null)
      initialize();
  }

  private synchronized void initialize() {
    if (vertexCounts != null)
      return;
    vertexCounts = new short[maxLevel];
    neighborVertexesArrays = new short[maxLevel][];
    faceVertexesArrays = new short[maxLevel][];
    faceNormixesArrays = new short[maxLevel][];

    vertexVectors = new Vector3f[12];
    vertexVectors[0] = new Vector3f(0, 0, halfRoot5);
    for (int i = 0; i < 5; ++i) {
      vertexVectors[i+1] =
        new Vector3f((float)Math.cos(i * oneFifth),
                     (float)Math.sin(i * oneFifth),
                     0.5f);
      vertexVectors[i+6] =
        new Vector3f((float)Math.cos(i * oneFifth + oneTenth),
                     (float)Math.sin(i * oneFifth + oneTenth),
                     -0.5f);
    }
    vertexVectors[11] = new Vector3f(0, 0, -halfRoot5);
    for (int i = 12; --i >= 0; )
      vertexVectors[i].normalize();
    faceVertexesArrays[0] = faceVertexesIcosahedron;
    neighborVertexesArrays[0] = neighborVertexesIcosahedron;
    vertexCounts[0] = 12;
    
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

  short[] getFaceNormixes(int level) {
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    return calcFaceNormixes(level);
  }

  private synchronized short[] calcFaceNormixes(int level) {
    System.out.println("calcFaceNormixes(" + level + ")");
    short[] faceNormixes = faceNormixesArrays[level];
    if (faceNormixes != null)
      return faceNormixes;
    Vector3f t = new Vector3f();
    short[] faceVertexes = faceVertexesArrays[level];
    int j = faceVertexes.length;
    int faceCount = j / 3;
    faceNormixes = new short[faceCount];
    for (int i = faceCount; --i >= 0; ) {
      Vector3f vA = vertexVectors[faceVertexes[--j]];
      Vector3f vB = vertexVectors[faceVertexes[--j]];
      Vector3f vC = vertexVectors[faceVertexes[--j]];
      float dAB, dBC, dAC;
      dAB = g3d.normix3d.distance(vA, vB);
      dBC = g3d.normix3d.distance(vB, vC);
      dAC = g3d.normix3d.distance(vA, vC);
      t.add(vA, vB);
      t.add(vC);
      short normix = g3d.normix3d.getNormix(t);
      faceNormixes[i] = normix;
      System.out.println("" + t + " = " + vA + " + " + vB + " + " + vC);
      Vector3f faceNormal = vertexVectors[normix];
      System.out.println(" --> " + faceNormal);
      System.out.println("dAB=" + dAB + " dBC=" + dBC + " dAC=" + dAC);

      float champD = g3d.normix3d.distance(vertexVectors[normix], t);
      int champ = normix;
      int vertexCount = getVertexCount(g3d.normix3d.level);
      for (int k = vertexCount; --k >= 0; ) {
        float d = g3d.normix3d.distance(vertexVectors[k], t);
        if (d < champD) {
          champ = k;
          champD = d;
        }
      }
      if (champ != normix)
        System.out.println("sequential search says " + champ + " ? is == " +
                           normix);
    }
    faceNormixesArrays[level] = faceNormixes;
    return faceNormixes;
  }
    

  private static short vertexNext;
  private static Hashtable htVertex;
    
  private final static boolean VALIDATE = true;

  private static void quadruple(int level) {
    if (DUMP)
      System.out.println("quadruple(" + level + ")");
    htVertex = new Hashtable();
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
      for (int i = 0; i < 12; ++i) {
        for (int j = 0; j < 5; ++j) {
          int neighbor = neighborVertexes[i * 6 + j];
          if (neighbor < 0)
            throw new NullPointerException();
          if (neighbor >= vertexCount)
            throw new NullPointerException();
        if (neighborVertexes[i * 6 + 5] != -1)
          throw new NullPointerException();
        }
      }
      for (int i = 12 * 6; i < neighborVertexes.length; ++i) {
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
        if ((i < 12 && neighborCount != 5) ||
            (i >= 12 && neighborCount != 6))
          throw new NullPointerException();
        int faceCount = 0;
        for (int j = newFacesVertexes.length; --j >= 0; )
          if (newFacesVertexes[j] == i)
            ++faceCount;
        if ((i < 12 && faceCount != 5) ||
            (i >= 12 && faceCount != 6))
          throw new NullPointerException();
      }
    }
    htVertex = null;
  }

  private static void addNeighboringVertexes(short[] neighborVertexes,
                                             short v1, short v2) {
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

  short getNeighborVertex(int level, short vertex, int neighborIndex) {
    short[] neighborVertexes = neighborVertexesArrays[level];
    int offset = vertex * 6 + neighborIndex;
    return neighborVertexes[offset];
  }
    
  private static short getVertex(short v1, short v2) {
    if (v1 > v2) {
      short t = v1;
      v1 = v2;
      v2 = t;
    }
    Integer hashKey = new Integer((v1 << 16) + v2);
    Short iv = (Short)htVertex.get(hashKey);
    if (iv != null)
      return iv.shortValue();
    Vector3f newVertexVector = new Vector3f(vertexVectors[v1]);
    vertexVectors[vertexNext] = newVertexVector;
    newVertexVector.add(vertexVectors[v2]);
    newVertexVector.scale(0.5f);
    newVertexVector.normalize();
    htVertex.put(hashKey, new Short(vertexNext));
    return vertexNext++;
  }
}
