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
import org.openscience.jmol.viewer.protein.ProteinProp;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

public class Dots {

  JmolViewer viewer;
  JmolFrame frame;
  DotsRenderer dotsRenderer;

  int dotsConvexCount;
  int[][] dotsConvexMaps;

  AtomShape[] triples;
  Torus[] tori;
  Hashtable htTori = new Hashtable();

  Dots(JmolViewer viewer, JmolFrame frame, DotsRenderer dotsRenderer) {
    this.viewer = viewer;
    this.frame = frame;
    this.dotsRenderer = dotsRenderer;
  }

  public void setDotsOn(boolean dotsOn, BitSet bsSelected) {
    int atomShapeCount = frame.atomShapeCount;
    if (dotsOn) {
      AtomShape[] atomShapes = frame.atomShapes;
      if (dotsConvexMaps == null)
        dotsConvexMaps = new int[atomShapeCount][];
      else if (dotsConvexMaps.length < atomShapeCount) {
        int[][] t = new int[atomShapeCount][];
        System.arraycopy(dotsConvexMaps, 0, t, 0,
                         dotsConvexMaps.length);
        dotsConvexMaps = t;
      }
      System.out.println("calculating visibility maps");
      for (int i = atomShapeCount; --i >= 0; )
        if (bsSelected.get(i) && dotsConvexMaps[i] == null)
          dotsConvexMaps[i] = calcVisibilityMap(atomShapes[i]);
      tori = calcTori(frame);
      triples = calcTriples(frame);
    } else {
      for (int i = atomShapeCount; --i >= 0; )
        if (bsSelected.get(i))
          dotsConvexMaps[i] = null;
      triples = null;
      tori = null;
    }
    int iLast = dotsConvexMaps.length;
    while (--iLast > 0 && dotsConvexMaps[iLast] == null)
      {}
    dotsConvexCount = iLast + 1;
  }

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

  int[] bitmap;
  Point3f pointT = new Point3f();

  void calcBits(Point3f myCenter, float vdwRadius, float probeRadius) {
    Vector3f[] vertices = dotsRenderer.icosohedron.vertices;
    int dotCount = vertices.length;
    if (bitmap == null)
      bitmap = allocateBitmap(dotCount);
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

  // I have no idea what this number should be
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
  }

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
        Torus torusIJ = getTorus(atomI, atomJ);
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

  }

  static Boolean boxedFalse = new Boolean(false);

  Torus getTorus(AtomShape atomI, AtomShape atomJ) {
    int i = atomI.getAtomIndex();
    int j = atomJ.getAtomIndex();
    if (i >= j)
      throw new NullPointerException();
    Long key = new Long(((long)i << 32) + j);
    Object value = htTori.get(key);
    if (value != null) {
      if (value instanceof Torus) {
        Torus torus = (Torus)value;
        return torus;
      }
      return null;
    }
    float radius = calcTorusRadius(atomI, atomJ);
    if (radius == 0) {
      htTori.put(key, boxedFalse);
      return null;
    }
    Point3f center = calcTorusCenter(atomI, atomJ);
    Vector3f axisVector = new Vector3f(atomI.point3f);
    axisVector.sub(center);
    Torus torus = new Torus(i, j, center, radius, axisVector);
    htTori.put(key, torus);
    return torus;
  }

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
