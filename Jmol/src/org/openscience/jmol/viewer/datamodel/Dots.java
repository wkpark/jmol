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

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.util.Hashtable;
import java.util.BitSet;
import java.awt.Rectangle;

/****************************************************************
 * The Dots and DotsRenderer classes implement vanderWaals and Connolly
 * dot surfaces. <p>
 * The vanderWaals surface is defined by the vanderWaals radius of each
 * atom. The surface of the atom is 'peppered' with dots. Each dot is
 * tested to see if it falls within the vanderWaals radius of any of
 * its neighbors. If so, then the dot is not displayed. <p>
 * See DotsRenderer.Geodesic for more discussion of the implementation. <p>
 * The Connolly surface is defined by rolling a probe sphere over the
 * surface of the molecule. In this way, a smooth surface is generated ...
 * one that does not have crevices between atoms. Three types of shapes
 * are generated: convex, saddle, and concave. <p>
 * The 'probe' is a sphere. A sphere of 1.2 angstroms representing HOH
 * is commonly used. <p>
 * Convex shapes are generated on the exterior surfaces of exposed atoms.
 * They are points on the sphere which are exposed. In these areas of
 * the molecule they look just like the vanderWaals dot surface. <p>
 * The saddles are generated between pairs of atoms. Imagine an O2
 * molecule. The probe sphere is rolled around the two oxygen spheres so
 * that it stays in contact with both spheres. The probe carves out a
 * torus (donut). The portion of the torus between the two points of
 * contact with the oxygen spheres is a saddle. <p>
 * The concave shapes are defined by triples of atoms. Imagine three
 * atom spheres in a close triangle. The probe sphere will sit (nicely)
 * in the little cavity formed by the three spheres. In fact, there are
 * two cavities, one on each side of the triangle. The probe sphere makes
 * one point of contact with each of the three atoms. The shape of the
 * cavity is the spherical triangle on the surface of the probe sphere
 * determined by these three contact points. <p>
 * For each of these three surface shapes, the dots are painted only
 * when the probe sphere does not interfere with any of the neighboring
 * atoms. <p>
 * See the following scripting commands:<br>
 * set solvent on/off (on defaults to 1.2 angstroms) <br>
 * set solvent 1.5 (choose another probe size) <br>
 * dots on/off <br>
 * color dots [color] <br>
 * color dotsConvex [color] <br>
 * color dotsSaddle [color] <br>
 * color dotsConcave [color] <br>
 *
 * The reference article for this implementation is: <br>
 * Analytical Molecular Surface Calculation, Michael L. Connolly,
 * Journal of Applied Crystalography, (1983) 15, 548-558 <p>
 *
 ****************************************************************/

public class Dots extends Graphic {

  DotsRenderer dotsRenderer;

  BitSet bsDotsOn;
  int dotsConvexCount;
  int[][] dotsConvexMaps;
  Vector3f[] geodesicVertices;
  int geodesicCount;
  int[] geodesicMap;
  final static int[] mapNull = new int[0];

  int cavityCount;
  Cavity[] cavities;
  int torusCount;
  Torus[] tori;

  BitSet bsToriCalculated;
  Hashtable htTori;

  int indexI, indexJ, indexK;
  Atom atomI, atomJ, atomK;
  Point3f centerI, centerJ, centerK;
  float radiusI, radiusJ, radiusK;
  float radiusP, diameterP;
  float radiiIP2, radiiJP2, radiiKP2;
  float distanceIJ2;
  Torus torusIJ, torusIK;
  final Point3f baseIJK = new Point3f();
  final Point3f probeIJK = new Point3f();
  float heightIJK;

  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();

    
  public void initGraphic() {
    dotsRenderer = (DotsRenderer)frame.getRenderer(JmolConstants.GRAPHIC_DOTS);
    bsDotsOn = new BitSet();
    geodesicVertices = dotsRenderer.geodesic.vertices;
    geodesicCount = geodesicVertices.length;
    geodesicMap = allocateBitmap(geodesicCount);
  }

  public void setShow(boolean show) {
    this.show = show;
    BitSet bsSelected = viewer.getBitSetSelection();
    if (radiusP != viewer.getSolventProbeRadius()) {
      dotsConvexCount = 0;
      dotsConvexMaps = null;
      bsToriCalculated = null;
      htTori = null;
      torusCount = 0;
      cavities = null;
      radiusP = viewer.getSolventProbeRadius();
      diameterP = 2 * radiusP;
    }
    int atomCount = frame.atomCount;
    dotsConvexCount = 0;
    if (show) {
      bsDotsOn.or(bsSelected);
      if (dotsConvexMaps == null)
        dotsConvexMaps = new int[atomCount][];
      else if (dotsConvexMaps.length < atomCount) {
        int[][] t = new int[atomCount][];
        System.arraycopy(dotsConvexMaps, 0, t, 0,
                         dotsConvexMaps.length);
        dotsConvexMaps = t;
      }
      for (int i = atomCount; --i >= 0; )
        if (bsDotsOn.get(i)) {
          if (i >= dotsConvexCount)
            dotsConvexCount = i + 1;
          if (dotsConvexMaps[i] != null)
            continue;
          setAtomI(i);
          getNeighbors();
          calcConvexMap();
          calcTori();
          calcCavities();
        }
    } else {
      // turn off the selected dots
      // 1.1 jvm does not have BitSet.andNot()
      for (int i = atomCount; --i >= 0; )
        if (bsSelected.get(i))
          bsDotsOn.clear(i);
      int i;
      for (i = atomCount; --i >= 0 && !bsDotsOn.get(i); )
        {}
      dotsConvexCount = i+1;
    }
  }

  void setAtomI(int indexI) {
    this.indexI = indexI;
    atomI = frame.atoms[indexI];
    centerI = atomI.point3f;
    radiusI = atomI.getVanderwaalsRadiusFloat();
    radiiIP2 = radiusI + radiusP;
    radiiIP2 *= radiiIP2;
  }

  void setNeighborJ(int indexNeighbor) {
    indexJ = neighborIndices[indexNeighbor];
    atomJ = neighbors[indexNeighbor];
    radiusJ = atomJ.getVanderwaalsRadiusFloat();
    radiiJP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerJ = neighborCenters[indexNeighbor];
    distanceIJ2 = centerI.distanceSquared(centerJ);
  }

  void setNeighborK(int indexNeighbor) {
    indexK = neighborIndices[indexNeighbor];
    centerK = neighborCenters[indexNeighbor];
    atomK = neighbors[indexNeighbor];
    radiusK = atomK.getVanderwaalsRadiusFloat();
    radiiKP2 = neighborPlusProbeRadii2[indexNeighbor];
  }

  void calcConvexMap() {
    calcConvexBits();
    int indexLast;
    for (indexLast = geodesicMap.length;
         --indexLast >= 0 && geodesicMap[indexLast] == 0; )
      {}
    int[] map = mapNull;
    if (indexLast >= 0) {
      int count = indexLast + 1;
      map = new int[indexLast + 1];
      System.arraycopy(geodesicMap, 0, map, 0, count);
    }
    dotsConvexMaps[indexI] = map;
  }

  void calcConvexBits() {
    setAllBits(geodesicMap, geodesicCount);
    if (neighborCount == 0)
      return;
    float combinedRadii = radiusI + radiusP;
    int iLastUsed = 0;
    for (int iDot = geodesicCount; --iDot >= 0; ) {
      pointT.set(geodesicVertices[iDot]);
      pointT.scaleAdd(combinedRadii, centerI);
      int iStart = iLastUsed;
      do {
        if (pointT.distanceSquared(neighborCenters[iLastUsed])
	    < neighborPlusProbeRadii2[iLastUsed]) {
          clearBit(geodesicMap, iDot);
          break;
        }
        iLastUsed = (iLastUsed + 1) % neighborCount;
      } while (iLastUsed != iStart);
    }
  }

  // I have no idea what this number should be
  int neighborCount;
  Atom[] neighbors = new Atom[16];
  int[] neighborIndices = new int[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  
  void getNeighbors() {
    AtomIterator iter =
      frame.getWithinIterator(atomI, radiusI + diameterP +
                              frame.getMaxVanderwaalsRadius());
    neighborCount = 0;
    while (iter.hasNext()) {
      Atom neighbor = iter.next();
      if (neighbor == atomI)
        continue;
      float neighborRadius = neighbor.getVanderwaalsRadiusFloat();
      float neighborRadii2 = neighborRadius + radiusP;
      neighborRadii2 *= neighborRadii2;
      if (radiiIP2 + neighborRadii2 <=
          centerI.distanceSquared(neighbor.point3f))
        continue;
      if (neighborCount == neighbors.length) {
        Atom[] neighborsNew = new Atom[2 * neighborCount];
        System.arraycopy(neighbors, 0, neighborsNew, 0, neighborCount);
        neighbors = neighborsNew;
        int[] indicesNew = new int[2 * neighborCount];
        System.arraycopy(neighborIndices, 0, indicesNew, 0, neighborCount);
        neighborIndices = indicesNew;
        Point3f[] centersNew = new Point3f[2 * neighborCount];
        System.arraycopy(neighborCenters, 0, centersNew, 0, neighborCount);
        neighborCenters = centersNew;
        float[] radiiNew = new float[2 * neighborCount];
        System.arraycopy(neighborPlusProbeRadii2, 0, radiiNew, 0,
                         neighborCount);
        neighborPlusProbeRadii2 = radiiNew;
      }
      neighbors[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor.point3f;
      neighborIndices[neighborCount] = neighbor.atomIndex;
      neighborPlusProbeRadii2[neighborCount] = neighborRadii2;
      ++neighborCount;
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

  void calcTori() {
    if (radiusP == 0)
      return;
    if (htTori == null) {
      torusCount = 0;
      tori = new Torus[32];
      bsToriCalculated = new BitSet();
      htTori = new Hashtable();
    }
    if (bsToriCalculated.get(indexI))
      return;
    for (int iJ = neighborCount; --iJ >= 0; ) {
      if (indexI >= neighborIndices[iJ])
        continue;
      setNeighborJ(iJ);
      torusIJ = getTorus(atomI, indexI, atomJ, indexJ);
      if (torusIJ == null)
        continue;
      calcTorusProbeMap(torusIJ);
      if (torusIJ.probeMap == 0)
        continue;
      if (torusCount == tori.length) {
        Torus[] t = new Torus[torusCount * 2];
        System.arraycopy(tori, 0, t, 0, torusCount);
        tori = t;
      }
      tori[torusCount++] = torusIJ;
    }
  }

  final Matrix3f matrixT = new Matrix3f();
  final Matrix3f matrixT1 = new Matrix3f();
  final AxisAngle4f aaT = new AxisAngle4f();

  void calcTorusProbeMap(Torus torus) {
    long probeMap = ~0;

    float stepAngle = 2 * (float)Math.PI / 64;
    aaT.set(torus.axisVector, 0);
    int iLastNeighbor = 0;
    for (int a = 64; --a >= 0; ) {
      aaT.angle = a * stepAngle;
      matrixT.set(aaT);
      matrixT.transform(torus.radialVector, pointT);
      pointT.add(torus.center);
      int iStart = iLastNeighbor;
      do {
        if (neighbors[iLastNeighbor].atomIndex != torus.indexJ) {
          if (pointT.distanceSquared(neighborCenters[iLastNeighbor])
              < neighborPlusProbeRadii2[iLastNeighbor]) {
            probeMap &= ~(1L << (63 - a));
            break;
          }
        }
        iLastNeighbor = (iLastNeighbor + 1) % neighborCount;
      } while (iLastNeighbor != iStart);
    }
    torus.probeMap = probeMap;
  }

  final Vector3f vectorT = new Vector3f();
  final Vector3f vectorZ = new Vector3f(0, 0, 1);
  final Vector3f vectorX = new Vector3f(1, 0, 0);

  class Torus {
    int indexI, indexJ;
    Point3f center;
    float radius;
    Vector3f axisVector;
    Vector3f radialVector;
    Vector3f unitRadialVector;
    Vector3f tangentVector;
    Vector3f outerRadial;
    float outerAngle;
    long probeMap;
    AxisAngle4f aaRotate;

    Torus(Point3f centerI, int indexI, Point3f centerJ, int indexJ,
          Point3f center, float radius, Vector3f axisVector) {
      this.indexI = indexI;
      this.indexJ = indexJ;
      this.center = center;
      this.radius = radius;
      this.axisVector = axisVector;

      if (axisVector.x == 0)
        unitRadialVector = new Vector3f(1, 0, 0);
      else if (axisVector.y == 0)
        unitRadialVector = new Vector3f(0, 1, 0);
      else if (axisVector.z == 0)
        unitRadialVector = new Vector3f(0, 0, 1);
      else {
        unitRadialVector = new Vector3f(-axisVector.y, axisVector.x, 0);
        unitRadialVector.normalize();
      }
      radialVector = new Vector3f(unitRadialVector);
      radialVector.scale(radius);

      tangentVector = new Vector3f();
      tangentVector.cross(radialVector, axisVector);
      tangentVector.normalize();

      pointT.set(center);
      pointT.add(radialVector);

      outerRadial = new Vector3f(centerI);
      outerRadial.sub(pointT);
      outerRadial.normalize();
      outerRadial.scale(radiusP);

      vectorT.sub(centerJ, pointT);
      outerAngle = vectorT.angle(outerRadial);
      
      float angle = vectorZ.angle(axisVector);
      if (angle == 0) {
        matrixT.setIdentity();
      } else {
        vectorT.cross(vectorZ, axisVector);
        aaT.set(vectorT, angle);
        matrixT.set(aaT);
      }

      matrixT.transform(unitRadialVector, vectorT);
      angle = vectorX.angle(vectorT);
      if (angle != 0) {
        vectorT.cross(vectorX, vectorT);
        aaT.set(vectorT, angle);
        matrixT1.set(aaT);
        matrixT.mul(matrixT1);
      }

      aaRotate = new AxisAngle4f();
      aaRotate.set(matrixT);
    }
  }

  final static Boolean boxedFalse = new Boolean(false);

  Torus getTorus(Atom atomI, int indexI, Atom atomJ, int indexJ) {
    if (indexI >= indexJ)
      throw new NullPointerException();
    Long key = new Long(((long)indexI << 32) + indexJ);
    Object value = htTori.get(key);
    if (value != null) {
      if (value instanceof Torus) {
        Torus torus = (Torus)value;
        return torus;
      }
      return null;
    }
    float radius = calcTorusRadius();
    if (radius == 0) {
      htTori.put(key, boxedFalse);
      return null;
    }
    Point3f center = calcTorusCenter();
    Vector3f axisVector = new Vector3f();
    axisVector.sub(centerI, centerJ);
    Torus torus = new Torus(centerI, indexI, centerJ, indexJ,
                            center, radius, axisVector);
    htTori.put(key, torus);
    return torus;
  }

  Point3f calcTorusCenter() {
    Point3f torusCenter = new Point3f();
    torusCenter.sub(centerJ, centerI);
    torusCenter.scale((radiiIP2-radiiJP2) / distanceIJ2);
    torusCenter.add(centerI);
    torusCenter.add(centerJ);
    torusCenter.scale(0.5f);
    /*
      System.out.println("calcTorusCenter i=" + atomI.point3f.x + "," +
      atomI.point3f.y + "," + atomI.point3f.z + "  j=" +
      atomJ.point3f.x + "," + atomJ.point3f.y + "," +
      atomJ.point3f.z + "  center=" +
      torusCenter.x + "," + torusCenter.y + "," +
      torusCenter.z);
    */
    return torusCenter;
  }

  float calcTorusRadius() {
    float t1 = radiusI + radiusJ + diameterP;
    float t2 = t1*t1 - distanceIJ2;
    float diff = radiusI - radiusJ;
    float t3 = distanceIJ2 - diff*diff;
    if (t2 <= 0 || t3 <= 0 || distanceIJ2 == 0)
      return 0;
    return (float)(0.5*Math.sqrt(t2)*Math.sqrt(t3)/Math.sqrt(distanceIJ2));
  }


  void calcCavities() {
    if (radiusP == 0)
      return;
    if (cavities == null) {
      cavities = new Cavity[16];
      cavityCount = 0;
    }
    for (int iJ = neighborCount; --iJ >= 0; ) {
      if (indexI >= neighborIndices[iJ])
        continue;
      setNeighborJ(iJ);
      for (int iK = neighborCount; --iK >= 0; ) {
        if (indexJ >= neighborIndices[iK])
          continue;
        setNeighborK(iK);
        float distanceJK2 = centerJ.distanceSquared(centerK);
        if (distanceJK2 >= radiiJP2 + radiiKP2)
          continue;
        getCavitiesIJK();
      }
    }
  }

  void getCavitiesIJK() {
    torusIJ = getTorus(atomI, indexI, atomJ, indexJ);
    torusIK = getTorus(atomI, indexI, atomK, indexK);
    if (torusIJ == null || torusIK == null) {
      System.out.println("null torus found?");
      return;
    }
    uIJK.cross(torusIJ.axisVector, torusIK.axisVector);
    uIJK.normalize();
    if (! calcBaseIJK() || ! calcHeightIJK())
      return;
    probeIJK.scaleAdd(heightIJK, uIJK, baseIJK);
    if (checkProbeIJK())
      addCavity(new Cavity());
    probeIJK.scaleAdd(-heightIJK, uIJK, baseIJK);
    if (checkProbeIJK())
      addCavity(new Cavity());
  }

  boolean checkProbeIJK() {
    for (int i = neighborCount; --i >= 0; ) {
      int neighborIndex = neighborIndices[i];
      if (neighborIndex == indexI ||
          neighborIndex == indexJ ||
          neighborIndex == indexK)
        continue;
      if (probeIJK.distanceSquared(neighborCenters[i]) <
          neighborPlusProbeRadii2[i])
        return false;
    }
    return true;
  }

  void addCavity(Cavity cavity) {
    if (cavityCount == cavities.length) {
      Cavity[] t = new Cavity[2 * cavityCount];
      System.arraycopy(cavities, 0, t, 0, cavityCount);
      cavities = t;
    }
    cavities[cavityCount++] = cavity;
  }

  final Vector3f uIJK = new Vector3f();
  final Vector3f v2v3 = new Vector3f();
  final Vector3f v3v1 = new Vector3f();
  final Vector3f v1v2 = new Vector3f();
  final Vector3f p1 = new Vector3f();
  final Vector3f p2 = new Vector3f();
  final Vector3f p3 = new Vector3f();

  class Cavity {
    int ixI, ixJ, ixK;
    Point3f pointIP, pointJP, pointKP;

    Cavity() {
      ixI = indexI; ixJ = indexJ; ixK = indexK;

      vectorT.sub(centerI, probeIJK);
      vectorT.normalize();
      pointIP = new Point3f();
      pointIP.scaleAdd(radiusP, vectorT, probeIJK);
      
      vectorT.sub(centerJ, probeIJK);
      vectorT.normalize();
      pointJP = new Point3f();
      pointJP.scaleAdd(radiusP, vectorT, probeIJK);
      
      vectorT.sub(centerK, probeIJK);
      vectorT.normalize();
      pointKP = new Point3f();
      pointKP.scaleAdd(radiusP, vectorT, probeIJK);
    }
  }

  /****************************************************************
   * I could not understand the algorithm in the Connolly article :-(
   * All that it is trying to do is calculate the base point between
   * the two probes. This is the intersection of three planes:
   * the plane defined by atoms IJK, the bisecting plane of torusIJ,
   * and the bisecting plane of torusIK. <p>
   * This algorithm takes finds the intersection of three planes,
   * where each plane is defined by a normal + a point on the plane
   ****************************************************************/
  boolean calcBaseIJK() {
    Vector3f v1 = torusIJ.axisVector;
    p1.set(torusIJ.center);
    Vector3f v2 = torusIK.axisVector;
    p2.set(torusIK.center);
    Vector3f v3 = uIJK;
    p3.set(centerI);
    v2v3.cross(v2, v3);
    v3v1.cross(v3, v1);
    v1v2.cross(v1, v2);
    float denominator = v1.dot(v2v3);
    if (denominator == 0)
      return false;
    baseIJK.scale(v1.dot(p1), v2v3);
    baseIJK.scaleAdd(v2.dot(p2), v3v1, baseIJK);
    baseIJK.scaleAdd(v3.dot(p3), v1v2, baseIJK);
    baseIJK.scale(1 / denominator);
    return true;
  }
  
  boolean calcHeightIJK() {
    float hypotenuse2 = radiiIP2;
    vectorT.sub(baseIJK, centerI);
    float baseLength2 = vectorT.lengthSquared();
    float height2 = hypotenuse2 - baseLength2;
    if (height2 <= 0)
      return false;
    heightIJK = (float)Math.sqrt(height2);
    return true;
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
