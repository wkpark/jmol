/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.g3d.Shade3D;
import java.awt.Rectangle;

import java.util.Hashtable;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class DotsRenderer {

  JmolViewer viewer;
  Graphics3D g3d;
  boolean perspectiveDepth;
  short colixDots;

  public DotsRenderer(JmolViewer viewer) {
    this.viewer = viewer;
    this.icosohedron = new Icosohedron(); // 12 vertices
    icosohedron.quadruple(); // 12 * 4 - 6 = 42 vertices
    icosohedron.quadruple(); // 42 * 4 - 6 = 162 vertices
    icosohedron.quadruple(); // 162 * 4 - 6 = 642 vertices
    //    icosohedron.quadruple(); // 642 * 4 - 6 = 2562 vertices
  }

  public void setGraphicsContext(Graphics3D g3d, Rectangle rectClip) {
    this.g3d = g3d;
    perspectiveDepth = viewer.getPerspectiveDepth();
    colixDots = viewer.getColixDots();
  }

  Icosohedron icosohedron;

  void transform() {
    icosohedron.transform();
  }

  public void render(AtomShape atomShape, int[] visibilityMap) {
    render(colixDots == 0 ? atomShape.colixAtom : colixDots,
           atomShape.getVanderwaalsRadius(), visibilityMap,
           atomShape.x, atomShape.y, atomShape.z);
  }

  void render(short colix, double vdwRadius, int[] visibilityMap,
              int x, int y, int z) {
    icosohedron.calcScreenPoints(visibilityMap, vdwRadius, x, y, z);
    if (icosohedron.screenCoordinateCount > 0)
      g3d.plotPoints(
                     colix,
                     //                     icosohedron.intensities,
                     icosohedron.screenCoordinateCount,
                     icosohedron.screenCoordinates);
  }

  // I have no idea what this number should be
  int neighborCount;
  AtomShape[] neighbors = new AtomShape[16];
  Point3d[] neighborCenters = new Point3d[16];
  double[] neighborRadii2 = new double[16];
  
  void getNeighbors(AtomShape atom, double vdwRadius, double probeRadius) {
    AtomShapeIterator iter =
      atom.frame.getWithinIterator(atom, vdwRadius + probeRadius*2 +
                                   atom.frame.getMaxVanderwaalsRadius());
    neighborCount = 0;
    while (iter.hasNext()) {
      AtomShape neighbor = iter.next();
      if (neighbor != atom) {
        if (neighborCount == neighbors.length) {
          AtomShape[] neighborsNew = new AtomShape[2 * neighborCount];
          System.arraycopy(neighbors, 0, neighborsNew, 0, neighborCount);
          neighbors = neighborsNew;
          Point3d[] centersNew = new Point3d[2 * neighborCount];
          System.arraycopy(neighborCenters, 0, centersNew, 0, neighborCount);
          neighborCenters = centersNew;
          double[] radiiNew = new double[2 * neighborCount];
          System.arraycopy(neighborRadii2, 0, radiiNew, 0, neighborCount);
          neighborRadii2 = radiiNew;
        }
        neighbors[neighborCount] = neighbor;
        neighborCenters[neighborCount] = neighbor.point3d;
        double effectiveRadius = (neighbor.getVanderwaalsRadius() +
                                  probeRadius);
        neighborRadii2[neighborCount] = effectiveRadius * effectiveRadius;
        ++neighborCount;
      }
    }
    /*
      System.out.println("neighborsFound=" + neighborCount);
      System.out.println("myVdwRadius=" + myVdwRadius +
      " maxVdwRadius=" + maxVdwRadius +
      " distMax=" + (myVdwRadius + maxVdwRadius));
      Point3d me = atom.getPoint3d();
      for (int i = 0; i < neighborCount; ++i) {
      System.out.println(" dist=" +
      me.distance(neighbors[i].getPoint3d()));
      }
    */
  }

  int[] bitmap;
  Point3d pointT = new Point3d();

  void calcBits(Point3d myCenter, double vdwRadius, double probeRadius) {
    Vector3d[] vertices = icosohedron.vertices;
    int dotCount = vertices.length;
    setAllBits(bitmap, dotCount);
    if (neighborCount == 0)
      return;
    int iNeighborLast = 0;
    double fullRadius = vdwRadius + probeRadius;
    for (int iDot = dotCount; --iDot >= 0; ) {
      pointT.set(vertices[iDot]);
      pointT.scaleAdd(fullRadius, myCenter);
      int iStart = iNeighborLast;
      do {
        if (pointT.distanceSquared(neighborCenters[iNeighborLast]) <
            neighborRadii2[iNeighborLast]) {
          clearBit(bitmap, iDot);
          break;
        }
        iNeighborLast = (iNeighborLast + 1) % neighborCount;
      } while (iNeighborLast != iStart);
    }
  }

  int[] calcVisibilityMap(AtomShape atom) {
    double vdwRadius = atom.getVanderwaalsRadius();
    double probeRadius = viewer.getSolventProbeRadius();
    getNeighbors(atom, vdwRadius, probeRadius);
    calcBits(atom.getPoint3d(), vdwRadius, probeRadius);
    int indexLast;
    for (indexLast = bitmap.length;
         --indexLast >= 0 && bitmap[indexLast] == 0; )
      {}
    if (indexLast == -1)
      return null;
    int count = indexLast + 1;
    int[] visibilityMap = new int[indexLast + 1];
    System.arraycopy(bitmap, 0, visibilityMap, 0, count);
    return visibilityMap;
  }

  final static double halfRoot5 = 0.5 * Math.sqrt(5);
  final static double oneFifth = 2 * Math.PI / 5;
  final static double oneTenth = oneFifth / 2;
  
  final static short[] faceIndicesInitial = {
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

  class Icosohedron {

    Vector3d[] vertices;
    Vector3d[] verticesTransformed;
    //    byte[] intensitiesTransformed;
    int screenCoordinateCount;
    int[] screenCoordinates;
    //    byte[] intensities;
    short[] faceIndices;

    Icosohedron() {
      vertices = new Vector3d[12];
      vertices[0] = new Vector3d(0, 0, halfRoot5);
      for (int i = 0; i < 5; ++i) {
        vertices[i+1] = new Vector3d(Math.cos(i * oneFifth),
                                     Math.sin(i * oneFifth),
                                     0.5);
        vertices[i+6] = new Vector3d(Math.cos(i * oneFifth + oneTenth),
                                     Math.sin(i * oneFifth + oneTenth),
                                     -0.5);
      }
      vertices[11] = new Vector3d(0, 0, -halfRoot5);
      for (int i = 12; --i >= 0; )
        vertices[i].normalize();
      faceIndices = faceIndicesInitial;
      verticesTransformed = new Vector3d[12];
      for (int i = 12; --i >= 0; )
        verticesTransformed[i] = new Vector3d();
      screenCoordinates = new int[3 * 12];
      //      intensities = new byte[12];
      //      intensitiesTransformed = new byte[12];
    }

    void transform() {
      for (int i = vertices.length; --i >= 0; ) {
        Vector3d t = verticesTransformed[i];
        viewer.transformVector(vertices[i], t);
        //        intensitiesTransformed[i] =
        //          Shade3D.calcIntensity((float)t.x, (float)t.y, (float)t.z);
      }
    }

    void calcScreenPoints(int[] visibilityMap, double radius,
			  int x, int y, int z) {
      int pixelsPerAngstrom = (int)viewer.scaleToScreen(0, 1.0);
      int dotCount = 12;
      if (pixelsPerAngstrom > 4) {
        dotCount = 42;
        if (pixelsPerAngstrom > 8) {
          dotCount = 162;
          if (pixelsPerAngstrom > 16) {
            dotCount = 642;
            //		  if (pixelsPerAngstrom > 32)
            //		      dotCount = 2562;
          }
        }
      }

      double scaledRadius = viewer.scaleToPerspective(z, radius);
      int icoordinates = 0;
      //      int iintensities = 0;
      int iDot = visibilityMap.length << 5;
      screenCoordinateCount = 0;
      if (iDot > dotCount)
        iDot = dotCount;
      while (--iDot >= 0) {
        if (! getBit(visibilityMap, iDot))
          continue;
        //        intensities[iintensities++] = intensitiesTransformed[iDot];
        Vector3d vertex = verticesTransformed[iDot];
        screenCoordinates[icoordinates++] = x
          + (int)((scaledRadius*vertex.x) + (vertex.x < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = y
          + (int)((scaledRadius*vertex.y) + (vertex.y < 0 ? -0.5 : 0.5));
        screenCoordinates[icoordinates++] = z
          + (int)((scaledRadius*vertex.z) + (vertex.z < 0 ? -0.5 : 0.5));
        ++screenCoordinateCount;
      }
    }

    short iVertexNew;
    Hashtable htVertex;
    
    void quadruple() {
      htVertex = new Hashtable();
      int nVerticesOld = vertices.length;
      short[] faceIndicesOld = faceIndices;
      int nFaceIndicesOld = faceIndicesOld.length;
      int nEdgesOld = nVerticesOld + nFaceIndicesOld/3 - 2;
      int nVerticesNew = nVerticesOld + nEdgesOld;
      Vector3d[] verticesNew = new Vector3d[nVerticesNew];
      System.arraycopy(vertices, 0, verticesNew, 0, nVerticesOld);
      vertices = verticesNew;
      verticesTransformed = new Vector3d[nVerticesNew];
      for (int i = nVerticesNew; --i >= 0; )
        verticesTransformed[i] = new Vector3d();
      screenCoordinates = new int[3 * nVerticesNew];
      //      intensitiesTransformed = new byte[nVerticesNew];
      //      intensities

      short[] faceIndicesNew = new short[4 * nFaceIndicesOld];
      faceIndices = faceIndicesNew;
      iVertexNew = (short)nVerticesOld;
      
      int iFaceNew = 0;
      for (int i = 0; i < nFaceIndicesOld; ) {
        short iA = faceIndicesOld[i++];
        short iB = faceIndicesOld[i++];
        short iC = faceIndicesOld[i++];
        short iAB = getVertex(iA, iB);
        short iBC = getVertex(iB, iC);
        short iCA = getVertex(iC, iA);
        
        faceIndicesNew[iFaceNew++] = iA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iCA;

        faceIndicesNew[iFaceNew++] = iB;
        faceIndicesNew[iFaceNew++] = iBC;
        faceIndicesNew[iFaceNew++] = iAB;

        faceIndicesNew[iFaceNew++] = iC;
        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iBC;

        faceIndicesNew[iFaceNew++] = iCA;
        faceIndicesNew[iFaceNew++] = iAB;
        faceIndicesNew[iFaceNew++] = iBC;
      }
      if (iFaceNew != faceIndicesNew.length) {
        System.out.println("que?");
        throw new NullPointerException();
      }
      if (iVertexNew != nVerticesNew) {
        System.out.println("huh? " + " iVertexNew=" + iVertexNew +
                           "nVerticesNew=" + nVerticesNew);
        throw new NullPointerException();
      }
      htVertex = null;
      bitmap = allocateBitmap(nVerticesNew);
    }
    
    private short getVertex(short i1, short i2) {
      if (i1 > i2) {
        short t = i1;
        i1 = i2;
        i2 = t;
      }
      Integer hashKey = new Integer((i1 << 16) + i2);
      Short iv = (Short)htVertex.get(hashKey);
      if (iv != null)
        return iv.shortValue();
      Vector3d vertexNew = new Vector3d(vertices[i1]);
      vertexNew.add(vertices[i2]);
      vertexNew.scale(0.5);
      vertexNew.normalize();
      htVertex.put(hashKey, new Short(iVertexNew));
      vertices[iVertexNew] = vertexNew;
      return iVertexNew++;
    }
  }

  final static int[] allocateBitmap(int count) {
    return new int[(count + 31) >> 5];
  }

  final static void setBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] |= 1 << (~i & 31);
  }

  final static void clearBit(int[] bitmap, int i) {
    bitmap[(i >> 5)] &= ~(1 << (~i & 31));
  }

  final static boolean getBit(int[] bitmap, int i) {
    return (bitmap[(i >> 5)] << (i & 31)) < 0;
  }

  final static void setAllBits(int[] bitmap, int count) {
    int i = count >> 5;
    if ((count & 31) != 0)
      bitmap[i] = 0x80000000 >> (count - 1);
    while (--i >= 0)
      bitmap[i] = -1;
  }
  
  final static void clearBitmap(int[] bitmap) {
    for (int i = bitmap.length; --i >= 0; )
      bitmap[i] = 0;
  }
    
}

