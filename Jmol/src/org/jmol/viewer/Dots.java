/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.viewer;

import javax.vecmath.*;

import org.jmol.g3d.Graphics3D;

import java.util.Hashtable;
import java.util.BitSet;

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

class Dots extends Shape {

  DotsRenderer dotsRenderer;

  short mad; // this is really just a true/false flag ... 0 vs non-zero

  int dotsConvexMax; // the Max == the highest atomIndex with dots + 1
  int[][] dotsConvexMaps;
  short[] colixesConvex;
  Vector3f[] geodesicVertices;
  int geodesicCount;
  int[] geodesicMap;
  final static int[] mapNull = new int[0];

  int cavityCount;
  Cavity[] cavities;
  int torusCount;
  Torus[] tori;

  Hashtable htTori;

  boolean useVanderwaalsRadius;

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
    
  void initShape() {
    dotsRenderer = (DotsRenderer)frame.getRenderer(JmolConstants.SHAPE_DOTS);
    geodesicVertices = dotsRenderer.geodesic.vertices;
    geodesicCount = geodesicVertices.length;
    geodesicMap = allocateBitmap(geodesicCount);
  }

  void setSize(int size, BitSet bsSelected) {
    // miguel 9 Sep 2005
    // this is a hack ...
    // if mad == 0 then turn it off
    // if mad > 0 then use vdw radius
    // if mad < 0 then use ionic radius
    short mad = (short)size;
    this.mad = mad;
    boolean newUseVanderwaalsRadius = (mad > 0);
    
    if (radiusP != viewer.getCurrentSolventProbeRadius() ||
        (newUseVanderwaalsRadius ^ useVanderwaalsRadius)) {
      dotsConvexMax = 0;
      dotsConvexMaps = null;
      torusCount = 0;
      htTori = null;
      tori = null;
      cavityCount = 0;
      cavities = null;
      radiusP = viewer.getCurrentSolventProbeRadius();
      diameterP = 2 * radiusP;
    }
    int atomCount = frame.atomCount;
    boolean isVisible = (mad != 0);
    int bsLength = bsSelected.length();
    // always delete old surfaces for selected atoms
    if (dotsConvexMaps != null) {
      for (int i = bsLength; --i >= 0; )
        if (bsSelected.get(i))
          dotsConvexMaps[i] = null;
      deleteUnnecessaryTori();
      deleteUnnecessaryCavities();
    }
    // now, calculate surface for selected atoms
    if (mad != 0) {
      useVanderwaalsRadius = (mad > 0);
      if (dotsConvexMaps == null) {
        dotsConvexMaps = new int[atomCount][];
        colixesConvex = new short[atomCount];
      }
      for (int i = bsLength; --i >= 0; ) {
        if (bsSelected.get(i)) {
          setAtomI(i, isVisible);
          getNeighbors(bsSelected);
          calcConvexMap();
          calcTori();
          calcCavities();
        }
      }
    }
    if (dotsConvexMaps == null)
      dotsConvexMax = 0;
    else {
      // update this count to speed up dotsRenderer
      int i;
      for (i = atomCount; --i >= 0 && dotsConvexMaps[i] == null; )
        {}
      dotsConvexMax = i + 1;
    }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      System.out.println("Dots.setProperty('color')");
      setProperty("colorConvex", value, bs);
      setProperty("colorConcave", value, bs);
      setProperty("colorSaddle", value, bs);
    }
    // no translucency for dots
    if ("colorConvex" == propertyName) {
      if(colixesConvex == null) 
        return;
      System.out.println("Dots.setProperty('colorConvex')");
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          colixesConvex[i] =
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix : viewer.getColixAtomPalette(atoms[i], (String)value));
      return;
    }
    if ("colorSaddle" == propertyName) {
      short colix = Graphics3D.getColix(value);
      for (int i = torusCount; --i >= 0; ) {
        Torus torus = tori[i];
        if (bs.get(torus.ixI))
          torus.colixI =
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix
             : viewer.getColixAtomPalette(atoms[torus.ixI],(String)value));
        if (bs.get(torus.ixJ))
          torus.colixJ =
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix
             : viewer.getColixAtomPalette(atoms[torus.ixJ],(String)value));
      }
      return;
    }
    if ("colorConcave" == propertyName) {
      short colix = Graphics3D.getColix(value);
      for (int i = cavityCount; --i >= 0; ) {
        Cavity cavity = cavities[i];
        if (bs.get(cavity.ixI))
          cavity.colixI =
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix
             : viewer.getColixAtomPalette(atoms[cavity.ixI], (String)value));
        if (bs.get(cavity.ixJ))
          cavity.colixJ =
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix
             : viewer.getColixAtomPalette(atoms[cavity.ixJ], (String)value));
        if (bs.get(cavity.ixK))
          cavity.colixK = 
            ((colix != Graphics3D.UNRECOGNIZED)
             ? colix
             : viewer.getColixAtomPalette(atoms[cavity.ixK], (String)value));
      }
      return;
    }
  }

  float getAppropriateRadius(Atom atom) {
    return (useVanderwaalsRadius
            ? atom.getVanderwaalsRadiusFloat()
            : atom.getBondingRadiusFloat());
  }

  void setAtomI(int indexI, boolean isVisible) {
    this.indexI = indexI;
    atomI = frame.atoms[indexI];
    atomI.setShapeVisibility(myVisibilityFlag,isVisible);
    centerI = atomI.point3f;
    radiusI = getAppropriateRadius(atomI);
    radiiIP2 = radiusI + radiusP;
    radiiIP2 *= radiiIP2;
  }

  void setNeighborJ(int indexNeighbor) {
    indexJ = neighborIndices[indexNeighbor];
    atomJ = neighbors[indexNeighbor];
    radiusJ = getAppropriateRadius(atomJ);
    radiiJP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerJ = neighborCenters[indexNeighbor];
    distanceIJ2 = centerI.distanceSquared(centerJ);
  }

  void setNeighborK(int indexNeighbor) {
    indexK = neighborIndices[indexNeighbor];
    centerK = neighborCenters[indexNeighbor];
    atomK = neighbors[indexNeighbor];
    radiusK = getAppropriateRadius(atomK);
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
      map = new int[count];
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
  
  void getNeighbors(BitSet bsSelected) {
    /*
    System.out.println("Dots.getNeighbors radiusI=" + radiusI +
                       " diameterP=" + diameterP +
                       " maxVdw=" + frame.getMaxVanderwaalsRadius());
    */
    AtomIterator iter =
      frame.getWithinModelIterator(atomI, radiusI + diameterP +
                                   frame.getMaxVanderwaalsRadius());
    neighborCount = 0;
    while (iter.hasNext()) {
      Atom neighbor = iter.next();
      if (neighbor == atomI)
        continue;
      // only consider selected neighbors
      if (! bsSelected.get(neighbor.atomIndex))
        continue;
      float neighborRadius = getAppropriateRadius(neighbor);
      if (centerI.distance(neighbor.point3f) >
          radiusI + radiusP + radiusP + neighborRadius)
        continue;
      if (neighborCount == neighbors.length) {
        neighbors = (Atom[])Util.doubleLength(neighbors);
        neighborIndices = Util.doubleLength(neighborIndices);
        neighborCenters = (Point3f[])Util.doubleLength(neighborCenters);
        neighborPlusProbeRadii2 = Util.doubleLength(neighborPlusProbeRadii2);
      }
      neighbors[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor.point3f;
      neighborIndices[neighborCount] = neighbor.atomIndex;
      float neighborPlusProbeRadii = neighborRadius + radiusP;
      neighborPlusProbeRadii2[neighborCount] =
        neighborPlusProbeRadii * neighborPlusProbeRadii;
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
      htTori = new Hashtable();
    }
    for (int iJ = neighborCount; --iJ >= 0; ) {
      if (indexI >= neighborIndices[iJ])
        continue;
      setNeighborJ(iJ);
      torusIJ = getTorus(atomI, atomJ);
      if (torusIJ == null)
        continue;
      torusIJ.calcProbeMap();
      if (torusIJ.probeMap == 0)
        continue;
      if (torusCount == tori.length)
        tori = (Torus[])Util.doubleLength(tori);
      tori[torusCount++] = torusIJ;
    }
  }

  void deleteUnnecessaryTori() {
    boolean torusDeleted = false;
    for (int i = torusCount; --i >= 0; ) {
      Torus torus = tori[i];
      if (dotsConvexMaps[torus.ixI] == null &&
          dotsConvexMaps[torus.ixJ] == null) {
        torusDeleted = true;
        tori[i] = null;
      }
    }
    if (torusDeleted) {
      int iDestination = 0;
      for (int iSource = 0; iSource < torusCount; ++iSource) {
        if (tori[iSource] != null)
          tori[iDestination++] = tori[iSource];
      }
      for (int i = torusCount; --i >= iDestination; )
        tori[i] = null;
      torusCount = iDestination;
    }
  }

  final Matrix3f matrixT = new Matrix3f();
  final Matrix3f matrixT1 = new Matrix3f();
  final AxisAngle4f aaT = new AxisAngle4f();

  final Vector3f vectorT = new Vector3f();
  final Vector3f vectorT1 = new Vector3f();
  final Vector3f vectorZ = new Vector3f(0, 0, 1);
  final Vector3f vectorX = new Vector3f(1, 0, 0);

  final Point3f pointTorusP = new Point3f();
  final Vector3f vectorPI = new Vector3f();
  final Vector3f vectorPJ = new Vector3f();

  class Torus {
    int ixI, ixJ;
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
    short colixI, colixJ;

    Torus(Point3f centerA, int indexA, Point3f centerB, int indexB,
          Point3f center, float radius) {
      this.ixI = indexA;
      this.ixJ = indexB;
      this.center = center;
      this.radius = radius;

      axisVector = new Vector3f();
      axisVector.sub(centerB, centerA);

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

      pointTorusP.add(center, radialVector);

      vectorPI.sub(centerA, pointTorusP);
      vectorPI.normalize();
      vectorPI.scale(radiusP);

      vectorPJ.sub(centerB, pointTorusP);
      vectorPJ.normalize();
      vectorPJ.scale(radiusP);

      outerRadial = new Vector3f();
      outerRadial.add(vectorPI, vectorPJ);
      outerRadial.normalize();
      outerRadial.scale(radiusP);

      outerAngle = vectorPJ.angle(vectorPI) / 2;

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

    void calcProbeMap() {
      long probeMap = ~0;
      
      float stepAngle = 2 * (float)Math.PI / 64;
      aaT.set(axisVector, 0);
      int iLastNeighbor = 0;
      for (int a = 64; --a >= 0; ) {
        aaT.angle = a * stepAngle;
        matrixT.set(aaT);
        matrixT.transform(radialVector, pointT);
        pointT.add(center);
        int iStart = iLastNeighbor;
        do {
          if (neighbors[iLastNeighbor].atomIndex != ixJ) {
            if (pointT.distanceSquared(neighborCenters[iLastNeighbor])
                < neighborPlusProbeRadii2[iLastNeighbor]) {
              probeMap &= ~(1L << (63 - a));
              break;
            }
          }
          iLastNeighbor = (iLastNeighbor + 1) % neighborCount;
        } while (iLastNeighbor != iStart);
      }
      this.probeMap = probeMap;
    }
  }

  Torus getTorus(Atom atomI, Atom atomJ) {
    int indexI = atomI.atomIndex;
    int indexJ = atomJ.atomIndex;
    // indexI < indexJ is tested previously in calcTorus
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
      htTori.put(key, Boolean.FALSE);
      return null;
    }
    Point3f center = calcTorusCenter();
    Torus torus = new Torus(centerI, indexI, centerJ, indexJ, center, radius);
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

  void deleteUnnecessaryCavities() {
    boolean cavityDeleted = false;
    for (int i = cavityCount; --i >= 0; ) {
      Cavity cavity = cavities[i];
      if (dotsConvexMaps[cavity.ixI] == null &&
          dotsConvexMaps[cavity.ixJ] == null &&
          dotsConvexMaps[cavity.ixK] == null) {
        cavityDeleted = true;
        cavities[i] = null;
      }
    }
    if (cavityDeleted) {
      int iDestination = 0;
      for (int iSource = 0; iSource < cavityCount; ++iSource) {
        if (cavities[iSource] != null)
          cavities[iDestination++] = cavities[iSource];
      }
      for (int i = cavityCount; --i >= iDestination; )
        cavities[i] = null;
      cavityCount = iDestination;
    }
  }

  void getCavitiesIJK() {
    torusIJ = getTorus(atomI, atomJ);
    torusIK = getTorus(atomI, atomK);
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
    if (cavityCount == cavities.length)
      cavities = (Cavity[])Util.doubleLength(cavities);
    cavities[cavityCount++] = cavity;
  }

  final Vector3f uIJK = new Vector3f();
  final Vector3f v2v3 = new Vector3f();
  final Vector3f v3v1 = new Vector3f();
  final Vector3f v1v2 = new Vector3f();
  final Vector3f p1 = new Vector3f();
  final Vector3f p2 = new Vector3f();
  final Vector3f p3 = new Vector3f();

  // plus use vectorPI and vectorPJ from above;
  final Vector3f vectorPK = new Vector3f();

  final static byte[] gcSplits = {
    1, 2, 4,
    2, 3, 5,
    3, 1, 6,
    1, 4, 7,
    2, 4, 8,
    2, 5, 9,
    3, 5, 10,
    3, 6, 11,
    1, 6, 12
  };

  class Cavity {
    final int ixI, ixJ, ixK;
    final Point3f[] points;
    short colixI, colixJ, colixK;

    Cavity() {
      ixI = indexI; ixJ = indexJ; ixK = indexK;

      points = new Point3f[25];
      for (int i = 25; --i >= 0; )
        points[i] = new Point3f();

      vectorPI.sub(centerI, probeIJK);
      vectorPI.normalize();
      points[1].scaleAdd(radiusP, vectorPI, probeIJK);
      
      vectorPJ.sub(centerJ, probeIJK);
      vectorPJ.normalize();
      points[2].scaleAdd(radiusP, vectorPJ, probeIJK);
      
      vectorPK.sub(centerK, probeIJK);
      vectorPK.normalize();
      points[3].scaleAdd(radiusP, vectorPK, probeIJK);

      vectorT.add(vectorPI, vectorPJ);
      vectorT.add(vectorPK);
      vectorT.normalize();
      points[0].scaleAdd(radiusP, vectorT, probeIJK);

      for (int i = 0; i < gcSplits.length; i += 3)
        splitGreatCircle(gcSplits[i], gcSplits[i + 1], gcSplits[i + 2]);
      for (int i = 13; i < 25; ++i)
        splitGreatCircle(0, i - 12, i);
    }

    void splitGreatCircle(int indexA, int indexB, int indexMiddle) {
      vectorT.sub(points[indexA], probeIJK);
      vectorT1.sub(points[indexB], probeIJK);
      vectorT.add(vectorT1);
      vectorT.normalize();
      points[indexMiddle].scaleAdd(radiusP, vectorT, probeIJK);
    }
  }


  /*==============================================================*
   * All that it is trying to do is calculate the base point between
   * the two probes. This is the intersection of three planes:
   * the plane defined by atoms IJK, the bisecting plane of torusIJ,
   * and the bisecting plane of torusIK. <p>
   * I could not understand the algorithm that is described
   * in the Connolly article... seemed too complicated ... :-(
   * This algorithm takes finds the intersection of three planes,
   * where each plane is defined by a normal + a point on the plane
   *==============================================================*/
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
  void setModelVisibility() {
    if (dotsConvexMaps == null)
      return;
    Atom[] atoms = frame.atoms;
    for (int i = dotsConvexMax; --i >= 0; ) {
      int[] map = dotsConvexMaps[i];
      if (map != null && map != mapNull) {
        Atom atom = atoms[i];
        atom.clickabilityFlags |= JmolConstants.CLICKABLE_DOTS;
      }
    }
  }
}
