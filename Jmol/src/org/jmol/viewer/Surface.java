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

import javax.vecmath.*;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Enumeration;

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

class Surface extends Shape {

  short mad; // this is really just a true/false flag ... 0 vs non-zero

  private final static int GEODESIC_CALC_LEVEL = 3;
  int geodesicRenderingLevel = 1;

  int surfaceConvexMax; // the Max == the highest atomIndex with surface + 1
  int[][] surfaceConvexMaps;
  short[] colixesConvex;
  Vector3f[] geodesicVertexVectors;
  int geodesicVertexCount;
  int[] geodesicMap;
  final static int[] mapNull = new int[0];

  int cavityCount;
  Cavity[] cavities;
  int torusCount;
  Torus[] toruses;

  Hashtable htToruses;

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
    geodesicVertexVectors = g3d.getGeodesicVertexVectors();
    geodesicVertexCount = g3d.getGeodesicVertexCount(GEODESIC_CALC_LEVEL);
    geodesicMap = allocateBitmap(geodesicVertexCount);
  }

  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    this.mad = mad;
    if (radiusP != viewer.getCurrentSolventProbeRadius()) {
      surfaceConvexMax = 0;
      surfaceConvexMaps = null;
      torusCount = 0;
      htToruses = null;
      toruses = null;
      cavityCount = 0;
      cavities = null;
      radiusP = viewer.getCurrentSolventProbeRadius();
      diameterP = 2 * radiusP;
    }
    int atomCount = frame.atomCount;
    // always delete old surfaces for selected atoms
    if (surfaceConvexMaps != null) {
      for (int i = atomCount; --i >= 0; )
        if (bsSelected.get(i))
          surfaceConvexMaps[i] = null;
      deleteUnusedToruses();
      deleteUnusedCavities();
    }
    // now, calculate surface for selected atoms
    if (mad != 0) {
      if (surfaceConvexMaps == null) {
        surfaceConvexMaps = new int[atomCount][];
        colixesConvex = new short[atomCount];
      }
      if (radiusP > 0 && htToruses == null)
        htToruses = new Hashtable();
      for (int i = atomCount; --i >= 0; )
        if (bsSelected.get(i)) {
          setAtomI(i);
          getNeighbors(bsSelected);
          calcConvexMap();
          calcTorusesI();
          calcAndSaveCavitiesI();
        }
      saveToruses();
    }
    if (surfaceConvexMaps == null)
      surfaceConvexMax = 0;
    else {
      // update this count to speed up surfaceRenderer
      int i;
      for (i = atomCount; --i >= 0 && surfaceConvexMaps[i] == null; )
        {}
      surfaceConvexMax = i + 1;
    }
  }

  void setProperty(String propertyName, Object value, BitSet bs) {
    int atomCount = frame.atomCount;
    Atom[] atoms = frame.atoms;
    if ("color" == propertyName) {
      System.out.println("Surface.setProperty('color')");
      setProperty("colorConvex", value, bs);
      setProperty("colorConcave", value, bs);
      setProperty("colorSaddle", value, bs);
    }
    if ("colorConvex" == propertyName) {
      System.out.println("Surface.setProperty('colorConvex')");
      short colix = g3d.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          colixesConvex[i] = colix;
      return;
    }
    if ("colorSaddle" == propertyName) {
      short colix = g3d.getColix(value);
      for (int i = torusCount; --i >= 0; ) {
        Torus torus = toruses[i];
        if (bs.get(torus.indexII))
          torus.colixI = colix;
        if (bs.get(torus.indexJJ))
          torus.colixJ = colix;
      }
      return;
    }
    if ("colorConcave" == propertyName) {
      short colix = g3d.getColix(value);
      for (int i = cavityCount; --i >= 0; ) {
        Cavity cavity = cavities[i];
        if (bs.get(cavity.ixI))
          cavity.colixI = colix;
        if (bs.get(cavity.ixJ))
          cavity.colixJ = colix;
        if (bs.get(cavity.ixK))
          cavity.colixK = colix;
      }
      return;
    }
    if ("colorScheme" == propertyName) {
      if (value != null) {
        byte palette = viewer.getPalette((String)value);
        for (int i = atomCount; --i >= 0; ) {
          if (bs.get(i)) {
            Atom atom = atoms[i];
            colixesConvex[i] = viewer.getColixAtomPalette(atom, palette);
          }
        }
        for (int i = torusCount; --i >= 0; ) {
          Torus torus = toruses[i];
          if (bs.get(torus.indexII))
            torus.colixI = viewer.getColixAtomPalette(atoms[torus.indexII],
                                                      palette);
          if (bs.get(torus.indexJJ))
            torus.colixJ = viewer.getColixAtomPalette(atoms[torus.indexJJ],
                                                      palette);
        }
        for (int i = cavityCount; --i >= 0; ) {
          Cavity cavity = cavities[i];
          if (bs.get(cavity.ixI))
            cavity.colixI = viewer.getColixAtomPalette(atoms[cavity.ixI],
                                                       palette);
          if (bs.get(cavity.ixJ))
            cavity.colixJ = viewer.getColixAtomPalette(atoms[cavity.ixJ],
                                                       palette);
          if (bs.get(cavity.ixK))
            cavity.colixK = viewer.getColixAtomPalette(atoms[cavity.ixK],
                                                       palette);
        }
        return;
      }
      return;
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
    indexJ = neighborIndexes[indexNeighbor];
    atomJ = neighbors[indexNeighbor];
    radiusJ = atomJ.getVanderwaalsRadiusFloat();
    radiiJP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerJ = neighborCenters[indexNeighbor];
    distanceIJ2 = centerI.distanceSquared(centerJ);
  }

  void setNeighborK(int indexNeighbor) {
    indexK = neighborIndexes[indexNeighbor];
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
      map = new int[count];
      System.arraycopy(geodesicMap, 0, map, 0, count);
    }
    surfaceConvexMaps[indexI] = map;
  }

  void calcConvexBits() {
    setAllBits(geodesicMap, geodesicVertexCount);
    if (neighborCount == 0)
      return;
    float combinedRadii = radiusI + radiusP;
    int iLastUsed = 0;
    for (int iDot = geodesicVertexCount; --iDot >= 0; ) {
      pointT.set(geodesicVertexVectors[iDot]);
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
  int[] neighborIndexes = new int[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  
  void getNeighbors(BitSet bsSelected) {
    /*
    System.out.println("Surface.getNeighbors radiusI=" + radiusI +
                       " diameterP=" + diameterP +
                       " maxVdw=" + frame.getMaxVanderwaalsRadius());
    */
    AtomIterator iter =
      frame.getWithinIterator(atomI, radiusI + diameterP +
                              frame.getMaxVanderwaalsRadius());
    neighborCount = 0;
    while (iter.hasNext()) {
      Atom neighbor = iter.next();
      if (neighbor == atomI)
        continue;
      // only consider selected neighbors
      if (! bsSelected.get(neighbor.atomIndex))
        continue;
      float neighborRadius = neighbor.getVanderwaalsRadiusFloat();
      if (centerI.distance(neighbor.point3f) >
          radiusI + radiusP + radiusP + neighborRadius)
        continue;
      if (neighborCount == neighbors.length) {
        neighbors = (Atom[])Util.doubleLength(neighbors);
        neighborIndexes = Util.doubleLength(neighborIndexes);
        neighborCenters = (Point3f[])Util.doubleLength(neighborCenters);
        neighborPlusProbeRadii2 = Util.doubleLength(neighborPlusProbeRadii2);
      }
      neighbors[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor.point3f;
      neighborIndexes[neighborCount] = neighbor.atomIndex;
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

  void calcTorusesI() {
    if (radiusP == 0)
      return;
    for (int iJ = neighborCount; --iJ >= 0; ) {
      if (indexI >= neighborIndexes[iJ])
        continue;
      setNeighborJ(iJ);
      calcTorusIJ();
    }
  }

  void deleteUnusedToruses() {
    boolean torusDeleted = false;
    for (int i = torusCount; --i >= 0; ) {
      Torus torus = toruses[i];
      if (surfaceConvexMaps[torus.indexII] == null &&
          surfaceConvexMaps[torus.indexJJ] == null) {
        torusDeleted = true;
        toruses[i] = null;
      }
    }
    if (torusDeleted) {
      int iDestination = 0;
      for (int iSource = 0; iSource < torusCount; ++iSource) {
        if (toruses[iSource] != null)
          toruses[iDestination++] = toruses[iSource];
      }
      for (int i = torusCount; --i >= iDestination; )
        toruses[i] = null;
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
    int indexII, indexJJ;
    Point3f center;
    float radius;
    Vector3f axisVector;
    Vector3f radialVector;
    Vector3f unitRadialVector;
    Vector3f tangentVector;
    Vector3f outerRadial;
    float outerAngle;
    AxisAngle4f aaRotate;
    short colixI, colixJ;
    int cavityCount;
    Cavity[] cavities;

    Torus(Point3f centerI, int indexI, Point3f centerJ, int indexJ,
          Point3f center, float radius) {
      this.indexII = indexI;
      this.indexJJ = indexJ;
      this.center = center;
      this.radius = radius;

      axisVector = new Vector3f();
      axisVector.sub(centerJ, centerI);

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

      vectorPI.sub(centerI, pointTorusP);
      vectorPI.normalize();
      vectorPI.scale(radiusP);

      vectorPJ.sub(centerJ, pointTorusP);
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

    void addCavity(Cavity cavity) {
      if (cavities == null)
        cavities = new Cavity[4];
      else if (cavityCount == cavities.length)
        cavities = (Cavity[])Util.doubleLength(cavities);
      cavities[cavityCount++] = cavity;
    }
  }

  void calcTorusIJ() {
    // indexI < indexJ is tested previously in calcTorus
    if (indexI >= indexJ)
      throw new NullPointerException();
    Long key = new Long(((long)indexI << 32) + indexJ);
    Object value = htToruses.get(key);
    if (value != null)
      return;
    float radius = calcTorusRadius();
    if (radius == 0) {
      htToruses.put(key, Boolean.FALSE);
      return;
    }
    Point3f center = calcTorusCenter();

    float holeRadius = radius - radiusP;
    for (int n = neighborCount; --n >= 0; ) {
      int k = neighborIndexes[n];
      if (k == indexJ)
        continue;
      // see whether or not the probe would fit
      // at the farthest point from k
      //
      // 1. find the closest point on the torus plane to center K
      // 2. calculate vector from that point to center torus
      // 3. normalize
      // 4. scale by torus radius
      // 5. take distanceSquared to center K
      // 6. see if that distanceSquared > neighborPlusProbeRadii2
    }
    

    Torus torus = new Torus(centerI, indexI, centerJ, indexJ, center, radius);
    htToruses.put(key, torus);
  }

  void saveToruses() {
    if (radiusP == 0)
      return;
    if (toruses == null)
      toruses = new Torus[128];
    for (Enumeration e = htToruses.elements(); e.hasMoreElements(); ) {
      Object value = e.nextElement();
      if (value instanceof Torus) {
        Torus torus = (Torus)value;
        if (torusCount == toruses.length)
          toruses = (Torus[])Util.doubleLength(toruses);
        toruses[torusCount++] = torus;
      }
    }
  }

  Torus getTorus(int atomIndexA, int atomIndexB) {
    if (atomIndexA >= atomIndexB)
      throw new NullPointerException();
    Long key = new Long(((long)atomIndexA << 32) + atomIndexB);
    Object value = htToruses.get(key);
    return (value instanceof Torus) ? (Torus)value : null;
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


  void calcAndSaveCavitiesI() {
    if (radiusP == 0)
      return;
    if (cavities == null) {
      cavities = new Cavity[16];
      cavityCount = 0;
    }
    for (int iJ = neighborCount; --iJ >= 0; ) {
      if (indexI >= neighborIndexes[iJ])
        continue;
      setNeighborJ(iJ);
      for (int iK = neighborCount; --iK >= 0; ) {
        if (indexJ >= neighborIndexes[iK])
          continue;
        setNeighborK(iK);
        float distanceJK2 = centerJ.distanceSquared(centerK);
        if (distanceJK2 >= radiiJP2 + radiiKP2)
          continue;
        getCavitiesIJK();
      }
    }
  }

  void deleteUnusedCavities() {
    boolean cavityDeleted = false;
    for (int i = cavityCount; --i >= 0; ) {
      Cavity cavity = cavities[i];
      if (surfaceConvexMaps[cavity.ixI] == null &&
          surfaceConvexMaps[cavity.ixJ] == null &&
          surfaceConvexMaps[cavity.ixK] == null) {
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
    torusIJ = getTorus(indexI, indexJ);
    torusIK = getTorus(indexI, indexK);
    if (torusIJ == null || torusIK == null) {
      System.out.println("null torus found?");
      return;
    }
    uIJK.cross(torusIJ.axisVector, torusIK.axisVector);
    if (Float.isNaN(uIJK.x)) // linear
      return;
    uIJK.normalize();
    if (! calcBaseIJK() || ! calcHeightIJK())
      return;
    for (int i = -1; i <= 1; i += 2) {
      probeIJK.scaleAdd(i * heightIJK, uIJK, baseIJK);
      Cavity cavity = null;
      if (checkProbeIJK()) {
        cavity = new Cavity();
        addCavity(cavity);
        torusIJ.addCavity(cavity);
        torusIK.addCavity(cavity);
      }
    }
  }

  boolean checkProbeIJK() {
    for (int i = neighborCount; --i >= 0; ) {
      int neighborIndex = neighborIndexes[i];
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

  private final static float radiansPerSegment = 15 * 2*(float)Math.PI/360;

  class Cavity {
    final int ixI, ixJ, ixK;
    final Point3f[] points;
    final short[] normixes;
    short colixI, colixJ, colixK;

    Cavity() {
      ixI = indexI; ixJ = indexJ; ixK = indexK;

      vectorPI.sub(centerI, probeIJK);
      vectorPI.normalize();
      vectorPJ.sub(centerJ, probeIJK);
      vectorPJ.normalize();
      vectorPK.sub(centerK, probeIJK);
      vectorPK.normalize();
      float radiansIJ = vectorPI.angle(vectorPJ);
      int segmentsIJ = (int)(radiansIJ / radiansPerSegment);
      if (segmentsIJ == 0)
        ++segmentsIJ;
      float radiansJK = vectorPJ.angle(vectorPK);
      int segmentsJK = (int)(radiansJK / radiansPerSegment);
      if (segmentsJK == 0)
        ++segmentsJK;
      float radiansKI = vectorPK.angle(vectorPI);
      int segmentsKI = (int)(radiansKI / radiansPerSegment);
      if (segmentsKI == 0)
        ++segmentsKI;
      
      int pointCount = 1 + segmentsIJ + segmentsJK + segmentsKI;
      normixes = new short[pointCount];
      points = new Point3f[pointCount];
      for (int i = pointCount; --i >= 0; )
        points[i] = new Point3f();
      
      vectorT.add(vectorPI, vectorPJ);
      vectorT.add(vectorPK);
      vectorT.normalize();
      points[0].scaleAdd(radiusP, vectorT, probeIJK);
      normixes[0] = g3d.getNormix(vectorT);

      addSegments(radiansIJ, segmentsIJ, vectorPI, vectorPJ,
                  points, normixes, 1);
      addSegments(radiansJK, segmentsJK, vectorPJ, vectorPK,
                  points, normixes, 1 + segmentsIJ);
      addSegments(radiansKI, segmentsKI, vectorPK, vectorPI,
                  points, normixes, 1 + segmentsIJ + segmentsJK);
      for (int i = 0; i < points.length; ++i) {
        System.out.println("points[" + i + "]=" + points[i]);
      }
    }

    void addSegments(float radians, float segments, Vector3f v1, Vector3f v2,
                     Point3f[] points, short[] normixes, int index) {
      points[index].scaleAdd(radiusP, v1, probeIJK);
      normixes[index] = g3d.getNormix(v1);
      if (segments == 1)
        return;
      vectorT.cross(v1, v2);
      aaT.set(vectorT, 0);
      float radiansPerSegment = radians / segments;
      for (int i = 0, j = index + i; i < segments; ++i, ++j) {
        aaT.angle =  i * radiansPerSegment;
        matrixT.set(aaT);
        vectorT.set(v1);
        matrixT.transform(vectorT, vectorT1);
        points[j].scaleAdd(radiusP, vectorT1, probeIJK);
        normixes[j] = g3d.getNormix(vectorT1);
      }
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
    if (Float.isNaN(v2v3.x))
      return false;
    v3v1.cross(v3, v1);
    if (Float.isNaN(v3v1.x))
      return false;
    v1v2.cross(v1, v2);
    if (Float.isNaN(v1v2.x))
      return false;
    float denominator = v1.dot(v2v3);
    if (denominator == 0)
      return false;
    baseIJK.scale(v1.dot(p1), v2v3);
    baseIJK.scaleAdd(v2.dot(p2), v3v1, baseIJK);
    baseIJK.scaleAdd(v3.dot(p3), v1v2, baseIJK);
    baseIJK.scale(1 / denominator);
    if (Float.isNaN(baseIJK.x))
      return false;
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
    System.out.println("heightIJK=" + heightIJK);
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
