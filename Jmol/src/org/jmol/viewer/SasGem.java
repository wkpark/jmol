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

package org.jmol.viewer;

import org.jmol.util.Bmp;
import org.jmol.util.IntInt2ObjHash;
import org.jmol.g3d.Graphics3D;

import javax.vecmath.*;

/**
 * The SasGem is the Solvent Accessible Surface Geodesic Edge Machine.
 */

class SasGem {

  final Graphics3D g3d;
  final Viewer viewer;
  final Frame frame;
  final int geodesicLevel;

  int projectedCount = 0;
  short[] projectedVertexes = new short[64];
  float[] projectedAngles = new float[64];
  float[] projectedDistances = new float[64];

  final Vector3f[] geodesicVertexVectors;
  final int geodesicVertexCount;
  final int geodesicFaceCount;
  final short[] geodesicFaceVertexes;
  final short[] geodesicNeighborVertexes;

  final Vector3f centerVectorT = new Vector3f();
  final Point3f vertexPointT = new Point3f();
  final Vector3f vertexVectorT = new Vector3f();
  final Point3f projectedPointT = new Point3f();
  final Vector3f projectedVectorT = new Vector3f();

  final int[] bmpNotClipped;
  final int[] faceMapT;

  private final static float PI = (float)Math.PI;
  private final static int MAX_FULL_TORUS_STEP_COUNT =
    Sasurface.MAX_FULL_TORUS_STEP_COUNT;

  private final static int EXPOSED_EDGE_METHOD = 0;
  private int method = EXPOSED_EDGE_METHOD;

  SasGem(Viewer viewer, Graphics3D g3d, Frame frame, int geodesicLevel) {
    this.g3d = g3d;
    this.viewer = viewer;
    this.frame = frame;
    this.geodesicLevel = geodesicLevel;

    geodesicVertexVectors = g3d.getGeodesicVertexVectors();
    geodesicVertexCount = g3d.getGeodesicVertexCount(geodesicLevel);
    geodesicFaceCount = g3d.getGeodesicFaceCount(geodesicLevel);
    geodesicFaceVertexes = g3d.getGeodesicFaceVertexes(geodesicLevel);
    geodesicNeighborVertexes = g3d.getGeodesicNeighborVertexes(geodesicLevel);

    bmpNotClipped = Bmp.allocateBitmap(geodesicVertexCount);
    faceMapT = Bmp.allocateBitmap(geodesicFaceCount);
  }


  void findClippedGeodesicEdge(boolean isEdgeA,
                               Point3f geodesicCenter, float radius,
                               Point3f planeCenter, Vector3f unitNormal,
                               int[] visibleVertexMap,
                               int[] edgeVertexMap) {
    Bmp.clearBitmap(bmpNotClipped);
    Bmp.clearBitmap(edgeVertexMap);
    int unclippedCount = 0;
    for (int i = -1; (i = Bmp.nextSetBit(visibleVertexMap, i + 1)) >= 0; ) {
      //    for (int i = geodesicVertexCount; --i >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[i],
                            geodesicCenter);
      vertexVectorT.sub(vertexPointT, planeCenter);
      float dot = vertexVectorT.dot(unitNormal);
      if (isEdgeA)
        dot = -dot;
      if (dot >= 0) {
        ++unclippedCount;
        Bmp.setBit(bmpNotClipped, i);
      }
    }
    if (unclippedCount == 0)
      return; // everything is clipped ... inside another atom
    if (unclippedCount == geodesicVertexCount) {
      findClippedFaceVertexes(isEdgeA, geodesicCenter, radius,
                              planeCenter, unitNormal, edgeVertexMap);
      return;
    }
    for (int v = -1; (v = Bmp.nextSetBit(bmpNotClipped, v + 1)) >= 0; ) {
      int neighborsOffset = v * 6;
      for (int j = (v < 12) ? 5 : 6; --j >= 0; ) {
        int neighbor = geodesicNeighborVertexes[neighborsOffset + j];
        if (! Bmp.getBit(bmpNotClipped, neighbor)) {
          Bmp.setBit(edgeVertexMap, v);
          break;
        }
      }
    }
  }
  
  int findGeodesicEdge(int[] visibleVertexMap, int[] edgeVertexMap) {
    int edgeVertexCount = 0;
    Bmp.clearBitmap(edgeVertexMap);
    for (int v = -1; (v = Bmp.nextSetBit(visibleVertexMap, v + 1)) >= 0; ) {
      int neighborsOffset = v * 6;
      for (int j = (v < 12) ? 5 : 6; --j >= 0; ) {
        int neighbor = geodesicNeighborVertexes[neighborsOffset + j];
        if (! Bmp.getBit(visibleVertexMap, neighbor)) {
          Bmp.setBit(edgeVertexMap, v);
          ++edgeVertexCount;
          break;
        }
      }
    }
    return edgeVertexCount;
  }

  short findClosestVertex(short normix, int[] edgeVertexMap) {
    // note that there is some other code in Normix3D.java that
    // does the same thing. See which algorithm works better.
    int champion = -1;
    float championAngle = PI;
    Vector3f vector = geodesicVertexVectors[normix];
    for (int v = -1; (v = Bmp.nextSetBit(edgeVertexMap, v + 1)) >= 0; ) {
      float angle = vector.angle(geodesicVertexVectors[v]);
      if (angle < championAngle) {
        championAngle = angle;
        champion = v;
      }
    }
    return (short)champion;
  }

  /*
   * only a small piece of the sphere is clipped.
   * That is, none of the vertexes themselves are clipped,
   * but only a piece that is in within a face.
   * So, find the three vertexes that define the face in
   * which the clipped portion of the sphere exists.
   *
   * not working well ... cases are more pathological
   * just disable this.
   */
  void findClippedFaceVertexes(boolean isEdgeA,
                               Point3f geodesicCenter, float radius,
                               Point3f planeCenter, Vector3f unitNormal,
                               int[] edgeVertexMap) {
    return; // works poorly;
    /*
    // isEdgeA is not accounted for in this old code below
    float goldLength, silverLength, bronzeLength;
    goldLength = silverLength = bronzeLength = Float.MAX_VALUE;
    short goldVertex, silverVertex, bronzeVertex;
    goldVertex = silverVertex = bronzeVertex = -1;
    for (int i = geodesicVertexCount; --i >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[i],
                            geodesicCenter);
      vertexVectorT.sub(vertexPointT, planeCenter);
      float challengerLength = vertexVectorT.length();
      if (challengerLength < goldLength) {
        bronzeLength = challengerLength;
        bronzeVertex = (short)i;
      }
      if (challengerLength < silverLength) {
        bronzeLength = silverLength;
        bronzeVertex = silverVertex;
        silverLength = challengerLength;
        silverVertex = (short)i;
      }
      if (challengerLength < goldLength) {
        silverLength = goldLength;
        silverVertex = goldVertex;
        goldLength = challengerLength;
        goldVertex = (short)i;
      }
    }
    // now, confirm that the 3 closest vertexes are actually neighbors
    // that form a face;
    if (! g3d.isNeighborVertex(goldVertex, silverVertex,
                               geodesicLevel) ||
        ! g3d.isNeighborVertex(goldVertex, bronzeVertex,
                               geodesicLevel) ||
        ! g3d.isNeighborVertex(silverVertex, bronzeVertex,
                               geodesicLevel)) {
      System.out.println("Strange condition 0xFACE");
      return;
    }
    Bmp.setBit(edgeVertexMap, goldVertex);
    Bmp.setBit(edgeVertexMap, silverVertex);
    Bmp.setBit(edgeVertexMap, bronzeVertex);
    */
  }

  final AxisAngle4f aaT = new AxisAngle4f();
  final Matrix3f matrixT = new Matrix3f();
  final Vector3f vectorT = new Vector3f();

  void calcVectors0and90(Point3f planeCenter, Vector3f axisVector,
                         Point3f planeZeroPoint,
                         Vector3f vector0, Vector3f vector90) {
    vector0.sub(planeZeroPoint, planeCenter);
    aaT.set(axisVector, PI / 2);
    matrixT.set(aaT);
    matrixT.transform(vector0, vector90);
  }

  int countStitchesT;
  short[] stitchesT = new short[64];
  float[] segmentVertexAngles = new float[MAX_FULL_TORUS_STEP_COUNT];
  short[] segmentVertexes = new short[MAX_FULL_TORUS_STEP_COUNT];

  final Vector3f vector0T = new Vector3f();
  final Vector3f vector90T = new Vector3f();
  final Point3f planeCenterT = new Point3f();

  void projectAndSortGeodesicPoints(Point3f geodesicCenter, float radius,
                                    Point3f planeCenter,
                                    Vector3f axisUnitVector,
                                    Point3f planeZeroPoint,
                                    boolean fullTorus,
                                    int[] edgeVertexMap,
                                    boolean dump) {
    Vector3f vector0T = this.vector0T;
    Vector3f vector90T = this.vector90T;
    Point3f vertexPointT = this.vertexPointT;
    Vector3f vertexVectorT = this.vertexVectorT;
    Point3f projectedPointT = this.projectedPointT;
    Vector3f projectedVectorT = this.projectedVectorT;

    calcVectors0and90(planeCenter, axisUnitVector, planeZeroPoint,
                      vector0T, vector90T);

    float radiansPerAngstrom = PI / radius;
    
    projectedCount = 0;
    switch (method) {
    case EXPOSED_EDGE_METHOD:
      for (int v = -1; (v = Bmp.nextSetBit(edgeVertexMap, v + 1)) >= 0; ) {
        vertexPointT.scaleAdd(radius,geodesicVertexVectors[v],geodesicCenter);
        vertexVectorT.sub(vertexPointT, planeCenter);
        float distance = axisUnitVector.dot(vertexVectorT);
        projectedPointT.scaleAdd(-distance, axisUnitVector, vertexPointT);
        projectedVectorT.sub(projectedPointT, planeCenter);
        float angle =
          calcAngleInThePlane(vector0T, vector90T, projectedVectorT);
        addProjectedPoint((short) v, angle, distance * radiansPerAngstrom);
      }
      break;
    }
    
    sortProjectedVertexes();
    if (fullTorus)
      duplicateFirstProjectedGeodesicPoint();
  }

  void calcClippingPlaneCenter(Point3f axisPoint, Vector3f axisUnitVector,
                               Point3f planePoint, Point3f planeCenterPoint) {
    vectorT.sub(axisPoint, planePoint);
    float distance = axisUnitVector.dot(vectorT);
    planeCenterPoint.scaleAdd(-distance, axisUnitVector, axisPoint);
  }

  void addProjectedPoint(short vertex, float angle, float radians) {
    if (projectedCount == projectedVertexes.length) {
      projectedVertexes = Util.doubleLength(projectedVertexes);
      projectedAngles = Util.doubleLength(projectedAngles);
      projectedDistances = Util.doubleLength(projectedDistances);
    }
    projectedVertexes[projectedCount] = vertex;
    projectedAngles[projectedCount] = angle;
    projectedDistances[projectedCount] = radians;
    ++projectedCount;
  }

  static float calcAngleInThePlane(Vector3f radialVector0,
                                   Vector3f radialVector90,
                                   Vector3f vectorInQuestion) {
    float angle = radialVector0.angle(vectorInQuestion);
    float angle90 = radialVector90.angle(vectorInQuestion);
    if (angle90 > PI/2)
      angle = 2*PI - angle;
    return angle;
  }

  void sortProjectedVertexes() {
    int projectedCount = this.projectedCount;
    short[] projectedVertexes = this.projectedVertexes;
    float[] projectedAngles = this.projectedAngles;
    float[] projectedDistances = this.projectedDistances;
    for (int i = projectedCount; --i > 0; )
      for (int j = i; --j >= 0; )
        if (projectedAngles[j] > projectedAngles[i]) {
          Util.swap(projectedAngles, i, j);
          Util.swap(projectedDistances, i, j);
          Util.swap(projectedVertexes, i, j);
        }
  }

  void duplicateFirstProjectedGeodesicPoint() {
    System.out.println("duplicateFirstProjectedGeodesicPoint");
    addProjectedPoint(projectedVertexes[0],
                      projectedAngles[0] + 2*PI,
                      projectedDistances[0]);
  }

  float angleABC(float xA, float yA, float xB, float yB, float xC, float yC) {
    double vxAB = xA - xB;
    double vyAB = yA - yB;
    double vxBC = xC - xB;
    double vyBC = yC - yB;
    double dot = vxAB * vxBC + vyAB * vyBC;
    double lenAB = Math.sqrt(vxAB*vxAB + vyAB*vyAB);
    double lenBC = Math.sqrt(vxBC*vxBC + vyBC*vyBC);
    return (float)Math.acos(dot / (lenAB * lenBC));
  }

  float angleABCRight(float xA, float xB, float xC, float yC) {
    double vxAB = xA - xB;
    double vxBC = xC - xB;
    double vyBC = yC;
    double dot = vxAB * vxBC;
    double lenAB = Math.abs(vxAB);
    double lenBC = Math.sqrt(vxBC*vxBC + vyBC*vyBC);
    return (float)Math.acos(dot / (lenAB * lenBC));
  }

  float angleABCLeft(float xA, float xB, float yB, float xC, float yC) {
    double vxAB = xA - xB;
    double vyAB = 0 - yB;
    double vxBC = xC - xB;
    double vyBC = yC - yB;
    double dot = vxAB * vxBC + vyAB * vyBC;
    double lenAB = Math.sqrt(vxAB*vxAB + vyAB*vyAB);
    double lenBC = Math.sqrt(vxBC*vxBC + vyBC*vyBC);
    return (float)Math.acos(dot / (lenAB * lenBC));
  }

  int countSeamT;
  short[] seamT = new short[64];
  short[] createSeam(int stitchCount, short[] stitches) {
    countSeamT = 0;
    short lastTorusVertex = -1;
    short lastGeodesicVertex = -1;
    for (int i = 0; i < stitchCount; i += 2) {
      short torusVertex = stitches[i];
      short geodesicVertex = stitches[i + 1];
      if (torusVertex != lastTorusVertex) {
        if (geodesicVertex != lastGeodesicVertex) {
          if (countSeamT > 0)
            addToSeam(Short.MIN_VALUE);
          addToSeam(torusVertex);
          addToSeam((short)~geodesicVertex);
        } else {
          addToSeam(torusVertex);
        }
      }else {
        addToSeam((short)~geodesicVertex);
      }
      lastTorusVertex = torusVertex;
      lastGeodesicVertex = geodesicVertex;
    }
    short[] seam = new short[countSeamT];
    for (int i = seam.length; --i >= 0; )
      seam[i] = seamT[i];
    //    dumpSeam(stitchCount, stitches, seam);
    //    decodeSeam(seam);
    return seam;
  }

  void addToSeam(short vertex) {
    if (countSeamT == seamT.length)
      seamT = Util.doubleLength(seamT);
    seamT[countSeamT++] = vertex;
  }

  void dumpSeam(int stitchCount, short[] stitches, short[] seam) {
    System.out.println("dumpSeam:");
    for (int i = 0; i < stitchCount; i += 2)
      System.out.println("  " + stitches[i] + "->" + stitches[i+1]);
    System.out.println(" --");
    for (int i = 0; i < seam.length; ++i) {
      short v = seam[i];
      System.out.print("  " + v + " ");
      if (v == Short.MIN_VALUE)
        System.out.println(" -- break");
      else if (v < 0)
        System.out.println( "(" + ~v + ")");
      else
        System.out.println("");
    }
  }

  void decodeSeam(short[] seam) {
    System.out.println("-----\ndecodeSeam\n-----");
    boolean breakSeam = true;
    int lastTorusVertex = -1;
    int lastGeodesicVertex = -1;
    for (int i = 0; i < seam.length; ++i) {
      if (breakSeam) {
        lastTorusVertex = seam[i++];
        lastGeodesicVertex = ~seam[i];
        System.out.println("--break--");
        breakSeam = false;
        continue;
      }
      int v = seam[i];
      if (v > 0) {
        System.out.println(" " + lastTorusVertex + " -> " +
                           v + " -> " +
                           "(" + lastGeodesicVertex + ")");
        lastTorusVertex = v;
      } else {
        v = ~v;
        System.out.println(" " + lastTorusVertex + " -> " +
                           "(" + v + ") -> " +
                           "(" + lastGeodesicVertex + ")");
        lastGeodesicVertex = v;
      }
    }
  }

  void clipGeodesic(boolean isEdgeA, Point3f geodesicCenter, float radius,
                    Point3f planePoint, Vector3f axisUnitVector,
                    int[] geodesicVertexMap) {
    centerVectorT.sub(geodesicCenter, planePoint);
    float dotCenter = centerVectorT.dot(axisUnitVector);
    if (isEdgeA)
      dotCenter = -dotCenter;
    if (dotCenter >= radius) // all points are visible
      return;
    if (dotCenter < -radius) { // all points are clipped
      Bmp.clearBitmap(geodesicVertexMap);
      return;
    }
    for (int i = -1; (i = Bmp.nextSetBit(geodesicVertexMap, i + 1)) >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[i],
                            geodesicCenter);
      vertexVectorT.sub(vertexPointT, planePoint);
      float dot = vertexVectorT.dot(axisUnitVector);
      if (isEdgeA)
        dot = -dot;
      if (dot < 0)
        Bmp.clearBit(geodesicVertexMap, i);
    }
  }

  int[] calcFaceBitmap(int[] vertexMap) {
    Bmp.clearBitmap(faceMapT);
    for (int i = geodesicFaceCount, j = 3 * (i - 1); --i >= 0; j -= 3) {
      if (Bmp.getBit(vertexMap, geodesicFaceVertexes[j]) &&
          Bmp.getBit(vertexMap, geodesicFaceVertexes[j + 1]) &&
          Bmp.getBit(vertexMap, geodesicFaceVertexes[j + 2]))
        Bmp.setBit(faceMapT, i);
    }
    return Bmp.allocMinimalCopy(faceMapT);
  }
}

