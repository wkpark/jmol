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
import javax.vecmath.Matrix3f;
import java.util.Random;

/**
 * Provides quantization of normalized vectors so that shading for
 * lighting calculations can be handled by a simple index lookup
 *<p>
 * A 'normix' is a normal index, represented as a short
 *
 * @author Miguel, miguel@jmol.org
 */
class Normix3D {

  final Graphics3D g3d;
  final Geodesic3D geodesic3d;
  final Vector3f[] vertexVectors;
  final Vector3f[] transformedVectors;
  final byte[] intensities;
  final short[] neighborVertexes;
  final int normixCount;
  final Object root;

  private final static boolean GEODESIC_DUMP = false;

  private final static boolean TIMINGS = false;

  private final static int level = 3;

  private final Matrix3f rotationMatrix = new Matrix3f();

  Normix3D(Graphics3D g3d) {
    // 12, 42, 162, 642, 2562
    this.g3d = g3d;
    geodesic3d = g3d.geodesic3d;
    vertexVectors = Geodesic3D.vertexVectors;
    neighborVertexes = Geodesic3D.getNeighborVertexes(level);
    normixCount = Geodesic3D.getVertexCount(level);
    intensities = new byte[normixCount];
    transformedVectors = new Vector3f[normixCount];
    for (int i = normixCount; --i >= 0; )
      transformedVectors[i] = new Vector3f();

    root = buildTree();

    if (GEODESIC_DUMP)
      geodesicDump();

    /*
    for (int i = 0; i < normixCount; ++i)
      System.out.println("vertexVectors[" + i + "]=" + vertexVectors[i]);
    */

    //    dumpTree(root, 0);

    if (TIMINGS) {
      for (int i = 0; i < normixCount; ++i) {
        short normix = getNormixNormalized(vertexVectors[i]);
        if (normix != i)
          System.out.println("" + i + " -> " + normix);
      }
      
      Random rand = new Random();
      Vector3f vFoo = new Vector3f();
      Vector3f vBar = new Vector3f();
      Vector3f vSum = new Vector3f();
      
      int runCount = 1000000;
      long timeBegin, runTime;
      
      timeBegin = System.currentTimeMillis();
      for (int i = 0; i < runCount; ++i) {
        short foo = (short)(rand.nextDouble() * normixCount);
        int offsetNeighbor;
        short bar;
        do {
          offsetNeighbor = foo * 6 + (int)(rand.nextDouble() * 6);
          bar = neighborVertexes[offsetNeighbor];
        } while (bar == -1);
        vFoo.set(vertexVectors[foo]);
        vFoo.scale(rand.nextFloat());
        vBar.set(vertexVectors[bar]);
        vBar.scale(rand.nextFloat());
        vSum.add(vFoo, vBar);
        vSum.normalize();
        short sum = getNormixNormalized(vSum);
        if (sum != foo && sum != bar) {
          System.out.println("foo:" + foo + " -> " +
                             vertexVectors[foo] + "\n" +
                             "bar:" + bar + " -> " +
                             vertexVectors[bar] + "\n" +
                             "sum:" + sum + " -> " +
                             vertexVectors[sum] + "\n" +
                             "foo.dist="+distance(vSum,
                                                  vertexVectors[foo])+"\n"+
                             "bar.dist="+distance(vSum,
                                                  vertexVectors[bar])+"\n"+
                             "sum.dist="+distance(vSum,
                                                  vertexVectors[sum])+"\n"+
                             "\nvSum:" + vSum + "\n");
          dumpTree(root, 0);
          throw new NullPointerException();
        }
      }
      runTime = System.currentTimeMillis() - timeBegin;
      System.out.println("foo runtime for " + runCount + " -> " +
                         runTime + " ms");
    }

  }

  float distance(Vector3f v1, Vector3f v2) {
    float dx = v1.x - v2.x;
    float dy = v1.y - v2.y;
    float dz = v1.z - v2.z;
    return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
  }
    

  void dumpTree(Object element, int level) {
    if (element instanceof Integer) {
      for (int i = 4 * level; --i >= 0; )
        System.out.print(" ");
      int n = ((Integer)element).intValue();
      System.out.println("" + n + " -> " + vertexVectors[n]);
    } else {
      Node node = (Node)element;
      dumpTree(node.elementLeft, level + 1);
      for (int i = 4 * level; --i >= 0; )
        System.out.print(" ");
      System.out.println("" + node.dim + " - " + node.maxLeft +
                         " <-> " + node.minRight);
      dumpTree(node.elementRight, level + 1);
    }
  }

  Object buildTree() {
    short[] sortedVertexes = new short[normixCount];
    for (int i = normixCount; --i >= 0; )
      sortedVertexes[i] = (short)i;
    return buildTree(sortedVertexes, 0, normixCount);
  }

  Object buildTree(short[] sortedVertexes, int min, int max) {
    //    System.out.println("buildTree(" + min + "," + max + ")");
    if (max - min <= 0)
      throw new NullPointerException();
    if (max - min == 1)
      return new Integer(sortedVertexes[min]);
    float maxLeft, minRight;
    int mid;
    int dim = 2;
    do {
      sort(sortedVertexes, min, max, dim);
      mid = (min + max + 1) / 2;
      //      System.out.println("proposed mid=" + mid);
      minRight = getDimensionValue(dim, sortedVertexes[mid]);
      maxLeft = getDimensionValue(dim, sortedVertexes[mid - 1]);
      if (maxLeft < minRight)
        break;
    } while (--dim >= 0);
    if (dim < 0) {
      dim = 0;
      while ((maxLeft = getDimensionValue(dim, sortedVertexes[mid - 1])) ==
             minRight)
        if (--mid == min)
          throw new NullPointerException();
    }
    Object elementLeft = buildTree(sortedVertexes, min, mid);
    Object elementRight = buildTree(sortedVertexes, mid, max);
    return new Node(dim, maxLeft, elementLeft, minRight, elementRight);
  }

  void sort(short[] sortedVertexes, int min, int max, int dim) {
    for (int i = max; --i > min; ) { // this is > not >=
      short champion = sortedVertexes[i];
      float championValue = getDimensionValue(dim, champion);
      for (int j = i; --j >= min; ) {
        short challenger = sortedVertexes[j];
        float challengerValue = getDimensionValue(dim, challenger);
        if (championValue < challengerValue) {
          sortedVertexes[i] = challenger;
          sortedVertexes[j] = champion;
          champion = challenger;
          championValue = challengerValue;
        }
      }
    }
    float last = getDimensionValue(dim, sortedVertexes[min]);
    for (int i = min + 1; i < max; ++i) {
      float current = getDimensionValue(dim, sortedVertexes[i]);
      if (current < last)
        throw new NullPointerException();
      last = current;
    }
  }

  float getDimensionValue(int dim, int i1) {
    Vector3f v1 = vertexVectors[i1];
    switch (dim) {
    case 0:
      return v1.x;
    case 1:
      return v1.y;
    case 2:
      return v1.z;
    default:
      System.out.println("invalid dimension:" + dim);
      throw new NullPointerException();
    }
  }

  void geodesicDump() {
    for (int level = 0; level <= Graphics3D.HIGHEST_GEODESIC_LEVEL; ++level) {
      int vertexCount = Geodesic3D.getVertexCount(level);
      System.out.println("level=" + level +
                         " vertexCount=" + vertexCount +
                         " faceCount=" + Geodesic3D.getFaceCount(level));
      short[] neighborVertexes = Geodesic3D.getNeighborVertexes(level);
      short[] faceVertexes = Geodesic3D.getFaceVertexes(level);
      System.out.println("neighborVertexes.length=" +
                         neighborVertexes.length +
                         " faceVertexes.length=" +
                         faceVertexes.length);
      for (short i = 0; i < vertexCount; ++i) {
        System.out.print("level:" + level + " vertex:" + i + " ->");
        for (int j = 0; j < 6; ++j)
          System.out.print(" " + neighborVertexes[i * 6 + j]);
        System.out.println("");
      }
      /*
      for (short i = 0; i < vertexCount; ++i) {
        System.out.print("level:" + level + " vertex:" + i + " ->");
        for (int j = 6; --j >= 0; )
          System.out.print(" " + geodesic3d.getNeighborVertex(0, i, j));
        System.out.println("");
      }
      */
      System.out.println("-----------------");
    }
  }

  short getNormix(Vector3f v) {
    float magnitude = v.length();
    return getNormixNormalized(v.x / magnitude,
                               v.y / magnitude,
                               v.z / magnitude);
  }

  short getNormixNormalized(Vector3f v) {
    return getNormixNormalized(v.x, v.y, v.z);
  }

  short getNormix(float x, float y, float z) {
    float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
    return getNormixNormalized(x / magnitude, y / magnitude, z / magnitude);
  }

  private final static int STATS_COUNT = 0;
  private final static boolean PRINT_STATS = STATS_COUNT > 0;
  private int normalizedCount;
  private int newChampionCount;
  private int[] playoffCounts;

  short getNormixNormalized(float x, float y, float z) {
    if (PRINT_STATS) {
      if (playoffCounts == null)
        playoffCounts = new int[30];
      if ((++normalizedCount % STATS_COUNT) == 0) {
        System.out.println("normalizedCount=" + normalizedCount +
                           " newChampionCount=" + newChampionCount);
        for (int i = 0; i < playoffCounts.length; ++i) {
          int playoffCount = playoffCounts[i];
          if (i > 0 && playoffCount == 0)
            break;
          float percentage = (float)playoffCount / normalizedCount;
          percentage = (int)(percentage * 1000)/10.f;
          System.out.println(" playoffCounts[" + i + "]=" + playoffCount +
                             " " + percentage);
        }
      }
    }
    Object element = root;
    while (element instanceof Node) {
      Node node = (Node)element;
      int dim = node.dim;
      float value = dim == 0 ? x : dim == 1 ? y : z;
      float distanceLeft = value - node.maxLeft;
      if (distanceLeft < 0)
        distanceLeft = -distanceLeft;
      float distanceRight = value - node.minRight;
      if (distanceRight < 0)
        distanceRight = -distanceRight;
      element = (distanceLeft < distanceRight
                 ? node.elementLeft : node.elementRight);

    }
    short champion = (short)((Integer)element).intValue();
    Vector3f v = vertexVectors[champion];
    float dx = x - v.x;
    float dy = y - v.y;
    float dz = z - v.z;
    float championDist2 = dx*dx + dy*dy + dz*dz;
    if (championDist2 == 0) {
      //      System.out.println("KO:" + champion);
      if (PRINT_STATS)
        ++playoffCounts[0];
      return champion;
    }
    boolean newChampion;
    int playoffLevel = 0;
    do {
      int offsetNeighborMin = champion * 6;
      ++playoffLevel;
      newChampion = false;
      for (int offsetNeighbor = offsetNeighborMin + 6;
           --offsetNeighbor >= offsetNeighborMin; ) {
        short challenger = neighborVertexes[offsetNeighbor];
        if (challenger == -1)
          continue;
        v = vertexVectors[challenger];
        dx = x - v.x;
        float challengerDist2 = dx*dx;
        if (challengerDist2 >= championDist2)
          continue;
        dy = y - v.y;
        challengerDist2 += dy*dy;
        if (challengerDist2 >= championDist2)
          continue;
        dz = z - v.z;
        challengerDist2 += dz*dz;
        if (challengerDist2 >= championDist2)
          continue;
        /*
        System.out.println("champion:" + champion +
                           " defeated by challenger:" + challenger);
        */
        champion = challenger;
        championDist2 = challengerDist2;
        newChampion = true;
        if (PRINT_STATS)
          ++newChampionCount;
      }
    } while (newChampion);
    if (PRINT_STATS)
      ++playoffCounts[playoffLevel];
    return champion;
  }

  byte getIntensity(short normix) {
    return intensities[normix];
  }

  void setRotationMatrix(Matrix3f rotationMatrix) {
    this.rotationMatrix.set(rotationMatrix);
    for (int i = normixCount; --i >= 0; ) {
      Vector3f tv = transformedVectors[i];
      rotationMatrix.transform(vertexVectors[i], tv);
      intensities[i] = (tv.z >= 0
                        ? Shade3D.calcIntensity(tv.x, -tv.y, tv.z)
                        : Shade3D.calcIntensity(-tv.x, tv.y, -tv.z));
    }
  }

  Vector3f[] getTransformedVectors() {
    return transformedVectors;
  }
}

class Node {
  int dim;
  float maxLeft;
  Object elementLeft;
  float minRight;
  Object elementRight;
  
  Node(int dim,
       float maxLeft, Object elementLeft,
       float minRight, Object elementRight) {
    this.dim = dim;
    this.maxLeft = maxLeft;
    this.elementLeft = elementLeft;
    this.minRight = minRight;
    this.elementRight = elementRight;
  }

}

