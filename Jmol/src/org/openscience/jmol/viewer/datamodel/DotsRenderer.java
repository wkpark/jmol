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
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

public class DotsRenderer {

  JmolViewer viewer;
  Graphics3D g3d;
  JmolFrame frame;
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

  public void setGraphicsContext(Graphics3D g3d, Rectangle rectClip,
                                 JmolFrame frame) {
    this.g3d = g3d;
    this.frame = frame;
    perspectiveDepth = viewer.getPerspectiveDepth();
    colixDots = viewer.getColixDots();
  }

  Icosohedron icosohedron;

  void transform() {
    icosohedron.transform();
  }

  public void render() {
    Dots dots = frame.dots;
    if (dots == null)
      return;
    AtomShape[] atomShapes = frame.atomShapes;
    int[][] dotsConvexMaps = dots.dotsConvexMaps;
    for (int i = dots.dotsConvexCount; --i >= 0; ) {
      int[] map = dotsConvexMaps[i];
      if (map != null)
        render(atomShapes[i], map);
    }
    Dots.Torus[] tori = dots.tori;
    if (tori == null)
      return;
    Dots.Torus torus;
    for (int i = 0; i < tori.length && (torus = tori[i]) != null; ++i)
      renderTorus(torus);
    if (true)
      return;
    AtomShape[] triples = dots.triples;
    if (triples == null)
      return;
    int i = 0;
    AtomShape atomI, atomJ, atomK;
    g3d.setColix(viewer.getColixDistance());
    while ((atomI = triples[i++]) != null) {
      atomJ = triples[i++];
      atomK = triples[i++];
      g3d.plotPoint(viewer.transformPoint(dots.getTorus(atomI, atomJ).center));
      g3d.plotPoint(viewer.transformPoint(dots.getTorus(atomI, atomK).center));
      g3d.plotPoint(viewer.transformPoint(dots.getTorus(atomJ, atomK).center));
    }
  }

  public void render(AtomShape atomShape, int[] visibilityMap) {
    render(colixDots == 0 ? atomShape.colixAtom : colixDots,
           atomShape.getVanderwaalsRadius(), visibilityMap,
           atomShape.x, atomShape.y, atomShape.z);
  }

  void render(short colix, float vdwRadius, int[] visibilityMap,
              int x, int y, int z) {
    System.out.println("rendering convex");
    icosohedron.calcScreenPoints(visibilityMap, vdwRadius, x, y, z);
    if (icosohedron.screenCoordinateCount > 0)
      g3d.plotPoints(
                     colix,
                     //                     icosohedron.intensities,
                     icosohedron.screenCoordinateCount,
                     icosohedron.screenCoordinates);
  }

  // I have no idea what this number should be
  /*
  int neighborCount;
  AtomShape[] neighbors = new AtomShape[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborRadii2 = new float[16];
  
  void getNeighbors(AtomShape atom, float vdwRadius, float probeRadius) {
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
          Point3f[] centersNew = new Point3f[2 * neighborCount];
          System.arraycopy(neighborCenters, 0, centersNew, 0, neighborCount);
          neighborCenters = centersNew;
          float[] radiiNew = new float[2 * neighborCount];
          System.arraycopy(neighborRadii2, 0, radiiNew, 0, neighborCount);
          neighborRadii2 = radiiNew;
        }
        neighbors[neighborCount] = neighbor;
        neighborCenters[neighborCount] = neighbor.point3f;
        float effectiveRadius = (neighbor.getVanderwaalsRadius() +
                                  probeRadius);
        neighborRadii2[neighborCount] = effectiveRadius * effectiveRadius;
        ++neighborCount;
      }
    }
  */
    /*
      System.out.println("neighborsFound=" + neighborCount);
      System.out.println("myVdwRadius=" + myVdwRadius +
      " maxVdwRadius=" + maxVdwRadius +
      " distMax=" + (myVdwRadius + maxVdwRadius));
      Point3f me = atom.getPoint3f();
      for (int i = 0; i < neighborCount; ++i) {
      System.out.println(" dist=" +
      me.distance(neighbors[i].getPoint3f()));
      }
    */
  /*
  }
  */

  /*
  int[] bitmap;
  Point3f pointT = new Point3f();

  void calcBits(Point3f myCenter, float vdwRadius, float probeRadius) {
    Vector3f[] vertices = icosohedron.vertices;
    int dotCount = vertices.length;
    setAllBits(bitmap, dotCount);
    if (neighborCount == 0)
      return;
    int iNeighborLast = 0;
    float fullRadius = vdwRadius + probeRadius;
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
  */

  /*
  AtomShape[] calcTriples(JmolFrame frame) {
    float probeRadius = viewer.getSolventProbeRadius();
    if (probeRadius == 0)
      return null;
    float probeDiameter = 2 * probeRadius;
    AtomShape[] triples = new AtomShape[300];
    int iTriples = 0;
    AtomShape[] atomShapes = frame.atomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; ) {
      AtomShape atomI = atomShapes[i];
      float vdwRadiusI = atomI.getVanderwaalsRadius();
      getNeighbors(atomI, vdwRadiusI, probeRadius);
      for (int iJ = neighborCount; --iJ >= 0; ) {
        AtomShape atomJ = neighbors[iJ];
        int j = atomJ.getAtomIndex();
        if (i >= j)
          continue;
        float vdwRadiusJ = atomJ.getVanderwaalsRadius();
        float distIJ = atomI.point3f.distance(atomJ.point3f);
        if (distIJ >= (vdwRadiusI + probeDiameter + vdwRadiusJ))
          continue;
        for (int iK = iJ; --iK >= 0; ) {
          AtomShape atomK = neighbors[iK];
          if (j >= atomK.getAtomIndex())
            continue;
          float vdwRadiusK = atomK.getVanderwaalsRadius();
          float distIK = atomI.point3f.distance(atomK.point3f);
          if (distIK >= (vdwRadiusI + probeDiameter + vdwRadiusK))
            continue;
          float distJK = atomJ.point3f.distance(atomK.point3f);
          if (distJK >= (vdwRadiusJ + probeDiameter + vdwRadiusK))
            continue;
          if (iTriples == triples.length) {
            AtomShape[] t = new AtomShape[3 * iTriples];
            System.arraycopy(triples, 0, t, 0, iTriples);
            triples = t;
          }
          triples[iTriples++] = atomI;
          triples[iTriples++] = atomJ;
          triples[iTriples++] = atomK;
        }
      }
    }
    return triples;
  }
  */

  /*
  Torus[] calcTori(JmolFrame frame) {
    Dots dots = frame.dots;
    System.out.println("DotsRendereer.calcTori()");
    float probeRadius = viewer.getSolventProbeRadius();
    if (probeRadius == 0)
      return null;
    float probeDiameter = 2 * probeRadius;
    Torus[] tori = new Torus[32];
    int iTorus = 0;
    AtomShape[] atomShapes = frame.atomShapes;
    for (int i = frame.atomShapeCount; --i >= 0; ) {
      AtomShape atomI = atomShapes[i];
      float vdwRadiusI = atomI.getVanderwaalsRadius();
      getNeighbors(atomI, vdwRadiusI, probeRadius);
      for (int iJ = neighborCount; --iJ >= 0; ) {
        AtomShape atomJ = neighbors[iJ];
        int j = atomJ.getAtomIndex();
        if (i >= j) {
          continue;
        }
        Torus torusIJ = getTorus(dots, atomI, atomJ);
        if (torusIJ == null) {
          continue;
        }
        if (iTorus == tori.length) {
          Torus[] t = new Torus[iTorus * 2];
          System.arraycopy(tori, 0, t, 0, iTorus);
          tori = t;
        }
        tori[iTorus++] = torusIJ;
      }
    }
    return tori;
  }
  */

  /*
  int[] calcVisibilityMap(AtomShape atom) {
    float vdwRadius = atom.getVanderwaalsRadius();
    float probeRadius = viewer.getSolventProbeRadius();
    getNeighbors(atom, vdwRadius, probeRadius);
    calcBits(atom.getPoint3f(), vdwRadius, probeRadius);
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
  */

  /*
  Point3f calcTorusCenter(AtomShape atomI, AtomShape atomJ) {
    float rI = atomI.getVanderwaalsRadius();
    float rJ = atomJ.getVanderwaalsRadius();
    float rP = viewer.getSolventProbeRadius();
    float rIrP2 = rI + rP;
    rIrP2 *= rIrP2;
    float rJrP2 = rJ + rP;
    rJrP2 *= rJrP2;
    float distIJ2 = atomI.point3f.distance(atomJ.point3f);
    distIJ2 *= distIJ2;
    float t = (rIrP2 - rJrP2) / distIJ2;

    Point3f torusCenter = new Point3f(atomJ.point3f);
    torusCenter.sub(atomI.point3f);
    torusCenter.scale(t);
    torusCenter.add(atomI.point3f);
    torusCenter.add(atomJ.point3f);
    torusCenter.scale(0.5f);
    System.out.println("calcTorusCenter i=" + atomI.point3f.x + "," +
                       atomI.point3f.y + "," + atomI.point3f.z + "  j=" +
                       atomJ.point3f.x + "," + atomJ.point3f.y + "," +
                       atomJ.point3f.z + "  center=" +
                       torusCenter.x + "," + torusCenter.y + "," +
                       torusCenter.z);
    return torusCenter;
  }

  float calcTorusRadius(AtomShape atomI, AtomShape atomJ) {
    float rI = atomI.getVanderwaalsRadius();
    float rJ = atomJ.getVanderwaalsRadius();
    float rP = viewer.getSolventProbeRadius();
    float distIJ = atomI.point3f.distance(atomJ.point3f);
    float distIJ2 = distIJ*distIJ;
    float t1 = rI + rJ + 2*rP;
    float t2 = t1*t1 - distIJ2;
    float diff = rI - rJ;
    float t3 = distIJ2 - diff*diff;
    if (t2 <= 0 || t3 <= 0 || distIJ == 0)
      return 0;
    return (float)(0.5 * Math.sqrt(t2) * Math.sqrt(t3) / distIJ);
  }

  Boolean boxedFalse = new Boolean(false);

  Torus getTorus(Dots dots, AtomShape atomI, AtomShape atomJ) {
    int i = atomI.getAtomIndex();
    int j = atomJ.getAtomIndex();
    if (i >= j)
      throw new NullPointerException();
    Long key = new Long(((long)i << 32) + j);
    Object value = dots.htTori.get(key);
    if (value != null) {
      if (value instanceof Torus) {
        Torus torus = (Torus)value;
        return torus;
      }
      return null;
    }
    float radius = calcTorusRadius(atomI, atomJ);
    if (radius == 0) {
      dots.htTori.put(key, boxedFalse);
      return null;
    }
    Point3f center = calcTorusCenter(atomI, atomJ);
    Vector3f axisVector = new Vector3f(atomI.point3f);
    axisVector.sub(center);
    Torus torus = new Torus(i, j, center, radius, axisVector);
    dots.htTori.put(key, torus);
    return torus;
  }

  class Torus {
    int i, j;
    Point3f center;
    float radius;
    Vector3f axisVector;

    Torus(int i, int j, Point3f center, float radius, Vector3f axisVector) {
      this.i = i;
      this.j = j;
      this.center = center;
      this.radius = radius;
      this.axisVector = axisVector;
    }

    void render() {
      Point3i screenCenter = viewer.transformPoint(center);
      int screenDiameter =
        (int)(viewer.scaleToScreen(screenCenter.z, radius) * 2 + 0.5f);
      g3d.drawCircleCentered(viewer.getColixDistance(),
                             20,
                             screenCenter.x, screenCenter.y, screenCenter.z);
      int dotCount = 64;
      float stepAngle = 2 * (float)Math.PI / dotCount;
      Matrix3f matrixT = new Matrix3f();
      AxisAngle4f aaT = new AxisAngle4f(axisVector.x, axisVector.y,
                                        axisVector.z, 0);
      Vector3f radial = calcRadial(radius, axisVector);
      for (int i = dotCount; --i >= 0; ) {
        aaT.angle = i * stepAngle;
        matrixT.set(aaT);
        matrixT.transform(radial, pointT);
        pointT.add(center);
        Point3i screenPoint = viewer.transformPoint(pointT);
        g3d.plotPoint(screenPoint);
      }
    }
  }
  */

  Point3f pointT = new Point3f();

  void renderTorus(Dots.Torus torus) {
    Point3i screenCenter = viewer.transformPoint(torus.center);
    int screenDiameter =
      (int)(viewer.scaleToScreen(screenCenter.z, torus.radius) * 2 + 0.5f);
    g3d.drawCircleCentered(viewer.getColixDistance(),
                           20,
                           screenCenter.x, screenCenter.y, screenCenter.z);
    int dotCount = 64;
    float stepAngle = 2 * (float)Math.PI / dotCount;
    Matrix3f matrixT = new Matrix3f();
    AxisAngle4f aaT = new AxisAngle4f(torus.axisVector.x,
                                      torus.axisVector.y,
                                      torus.axisVector.z, 0);
    Vector3f radial = calcRadial(torus.radius, torus.axisVector);
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      matrixT.transform(radial, pointT);
      pointT.add(torus.center);
      Point3i screenPoint = viewer.transformPoint(pointT);
      g3d.plotPoint(screenPoint);
    }
  }
  
  Vector3f calcRadial(float radius, Vector3f axis) {
    if (axis.x == 0)
      return new Vector3f(radius, 0, 0);
    if (axis.y == 0)
      return new Vector3f(0, radius, 0);
    if (axis.z == 0)
      return new Vector3f(0, 0, radius);
    Vector3f radial = new Vector3f(-axis.y, axis.x, 0);
    radial.normalize();
    radial.scale(radius);
    return radial;
  }

  final static float halfRoot5 = (float)(0.5 * Math.sqrt(5));
  final static float oneFifth = 2 * (float)Math.PI / 5;
  final static float oneTenth = oneFifth / 2;
  
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

    Vector3f[] vertices;
    Vector3f[] verticesTransformed;
    //    byte[] intensitiesTransformed;
    int screenCoordinateCount;
    int[] screenCoordinates;
    //    byte[] intensities;
    short[] faceIndices;

    Icosohedron() {
      vertices = new Vector3f[12];
      vertices[0] = new Vector3f(0, 0, halfRoot5);
      for (int i = 0; i < 5; ++i) {
        vertices[i+1] = new Vector3f((float)Math.cos(i * oneFifth),
                                     (float)Math.sin(i * oneFifth),
                                     0.5f);
        vertices[i+6] = new Vector3f((float)Math.cos(i * oneFifth + oneTenth),
                                     (float)Math.sin(i * oneFifth + oneTenth),
                                     -0.5f);
      }
      vertices[11] = new Vector3f(0, 0, -halfRoot5);
      for (int i = 12; --i >= 0; )
        vertices[i].normalize();
      faceIndices = faceIndicesInitial;
      verticesTransformed = new Vector3f[12];
      for (int i = 12; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
      screenCoordinates = new int[3 * 12];
      //      intensities = new byte[12];
      //      intensitiesTransformed = new byte[12];
    }

    void transform() {
      for (int i = vertices.length; --i >= 0; ) {
        Vector3f t = verticesTransformed[i];
        viewer.transformVector(vertices[i], t);
        //        intensitiesTransformed[i] =
        //          Shade3D.calcIntensity((float)t.x, (float)t.y, (float)t.z);
      }
    }

    void calcScreenPoints(int[] visibilityMap, float radius,
			  int x, int y, int z) {
      int pixelsPerAngstrom = (int)viewer.scaleToScreen(0, 1f);
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

      float scaledRadius = viewer.scaleToPerspective(z, radius);
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
        Vector3f vertex = verticesTransformed[iDot];
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
      Vector3f[] verticesNew = new Vector3f[nVerticesNew];
      System.arraycopy(vertices, 0, verticesNew, 0, nVerticesOld);
      vertices = verticesNew;
      verticesTransformed = new Vector3f[nVerticesNew];
      for (int i = nVerticesNew; --i >= 0; )
        verticesTransformed[i] = new Vector3f();
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
      //      bitmap = allocateBitmap(nVerticesNew);
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
      Vector3f vertexNew = new Vector3f(vertices[i1]);
      vertexNew.add(vertices[i2]);
      vertexNew.scale(0.5f);
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

