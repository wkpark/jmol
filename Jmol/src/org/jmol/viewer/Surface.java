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
import org.jmol.g3d.Graphics3D;

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
 * surface of the molecule. In this way, a smooth surface is generated ...n
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

  private final static int GEODESIC_CALC_LEVEL = 2;
  int geodesicRenderingLevel = 2;

  int surfaceConvexMax; // the Max == the highest atomIndex with surface + 1
  int[][] convexVertexMaps;
  int[][] convexFaceMaps;
  short[] colixesConvex;
  Vector3f[] geodesicVertexVectors;
  int geodesicVertexCount;
  int geodesicFaceCount;
  short[] geodesicFaceVertexes;
  short[] geodesicNeighborVertexes;
  int[] tempVertexMap;
  int[] tempFaceMap;

  int cavityCount;
  Cavity[] cavities;
  int torusCount;
  Torus[] toruses;

  Hashtable htToruses;

  final private static boolean LOG = false;

  private final static short[] torusStepCounts = {20,40,40,80};

  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();

  private final static float PI = (float)Math.PI;

  final static int segmentsPerFullCircle = 50;
  final static float radiansPerSegment = 2 * PI / segmentsPerFullCircle;

  final Point3f[] pointStripT = new Point3f[segmentsPerFullCircle];
  final Vector3f stripSurfaceVector = new Vector3f();
  final Vector3f outerSurfaceVector = new Vector3f();
  final Point3f outerCenterPoint = new Point3f();
  final Point3f outerSurfacePoint = new Point3f();

  Vector3f[] probeVertexVectors;
    
  void initShape() {
    geodesicVertexVectors = g3d.getGeodesicVertexVectors();
    geodesicVertexCount = g3d.getGeodesicVertexCount(GEODESIC_CALC_LEVEL);
    tempVertexMap = Bmp.allocateBitmap(geodesicVertexCount);
    geodesicFaceCount = g3d.getGeodesicFaceCount(geodesicRenderingLevel);
    tempFaceMap = Bmp.allocateBitmap(geodesicFaceCount);
    geodesicFaceVertexes =
      g3d.getGeodesicFaceVertexes(geodesicRenderingLevel);
    geodesicNeighborVertexes =
      g3d.getGeodesicNeighborVertexes(geodesicRenderingLevel);
  }

  void setSize(int size, BitSet bsSelected) {
    short mad = (short)size;
    this.mad = mad;
    viewer.setSolventOn(true);
    if (radiusP != viewer.getCurrentSolventProbeRadius()) {
      surfaceConvexMax = 0;
      convexVertexMaps = null;
      convexFaceMaps = null;
      torusCount = 0;
      toruses = null;
      cavityCount = 0;
      cavities = null;
      radiusP = viewer.getCurrentSolventProbeRadius();
      diameterP = 2 * radiusP;
      calcProbeVectors();
    }
    int atomCount = frame.atomCount;
    if (convexVertexMaps == null) {
      convexVertexMaps = new int[atomCount][];
      convexFaceMaps = new int[atomCount][];
      colixesConvex = new short[atomCount];
    }
    // always delete old surfaces for selected atoms
    for (int i = atomCount; --i >= 0; )
      if (bsSelected.get(i)) {
        convexVertexMaps[i] = null;
        convexFaceMaps[i] = null;
      }
    deleteUnusedToruses();
    deleteUnusedCavities();

    htToruses = new Hashtable();
    // now, calculate surface for selected atoms
    if (mad != 0) {
      long timeBegin = System.currentTimeMillis();
      for (int i = 0; i < atomCount; ++i) // make this loop count up
        if (bsSelected.get(i)) {
          setAtomI(i);
          getNeighbors(bsSelected);
          sortNeighborIndexes();
          calcCavitiesI();
          if (convexVertexMaps[i] != null)
            calcVertexBitmapI();
        }
      for (int i = atomCount; --i >= 0; ) {
        int[] vertexMap = convexVertexMaps[i];
        if (vertexMap != null)
            convexFaceMaps[i] = calcFaceBitmap(vertexMap);
      }

      long timeElapsed = System.currentTimeMillis() - timeBegin;
      System.out.println("atomCount=" + atomCount);
      System.out.println("Surface construction time = " + timeElapsed + " ms");
    }
    htToruses = null;
    if (convexVertexMaps == null)
      surfaceConvexMax = 0;
    else {
      // update this count to speed up surfaceRenderer
      int i;
      for (i = atomCount; --i >= 0 && convexVertexMaps[i] == null; )
        {}
      surfaceConvexMax = i + 1;
    }
  }

  void calcProbeVectors() {
    // calculate a canonical probe that is the geodesic
    // vectors scaled to the probe radius
    probeVertexVectors = new Vector3f[geodesicVertexCount];
    for (int i = geodesicVertexCount; --i >= 0; ) {
      probeVertexVectors[i] = new Vector3f();
      probeVertexVectors[i].scale(radiusP, geodesicVertexVectors[i]);
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
    if ("translucency" == propertyName) {
      setProperty("translucencyConvex", value, bs);
      setProperty("translucencyConcave", value, bs);
      setProperty("translucencySaddle", value, bs);
    }
    if ("colorConvex" == propertyName) {
      System.out.println("Surface.setProperty('colorConvex')");
      short colix = Graphics3D.getColix(value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          colixesConvex[i] =
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[i], (String)value);
      return;
    }
    if ("translucencyConvex" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int i = atomCount; --i >= 0; )
        if (bs.get(i))
          colixesConvex[i] = Graphics3D.setTranslucent(colixesConvex[i],
                                                       isTranslucent);
      return;
    }
    if ("colorSaddle" == propertyName) {
      short colix = Graphics3D.getColix(value);
      for (int i = torusCount; --i >= 0; ) {
        Torus torus = toruses[i];
        colix = colix; // try to eliminate eclipse warning
        /*
        if (bs.get(torus.ixI))
          torus.colixI = colix;
        if (bs.get(torus.ixJ))
          torus.colixJ = colix;
        */
      }
      return;
    }
    if ("translucencySaddle" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int i = torusCount; --i >= 0; ) {
        Torus torus = toruses[i];
        torus = torus;
        // something
      }
      return;
    }
    if ("colorConcave" == propertyName) {
      short colix = Graphics3D.getColix(value);
      for (int i = cavityCount; --i >= 0; ) {
        Cavity cavity = cavities[i];
        if (bs.get(cavity.ixI))
          cavity.colixI = 
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[cavity.ixI], (String)value);
        if (bs.get(cavity.ixJ))
          cavity.colixJ = 
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[cavity.ixJ], (String)value);
        if (bs.get(cavity.ixK))
          cavity.colixK = 
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[cavity.ixK], (String)value);
      }
      return;
    }
    if ("translucencyConcave" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int i = cavityCount; --i >= 0; ) {
        Cavity cavity = cavities[i];
        if (bs.get(cavity.ixI))
          cavity.colixI = Graphics3D.setTranslucent(cavity.colixI,
                                                    isTranslucent);
        if (bs.get(cavity.ixJ))
          cavity.colixJ = Graphics3D.setTranslucent(cavity.colixJ,
                                                    isTranslucent);
        if (bs.get(cavity.ixK))
          cavity.colixK = Graphics3D.setTranslucent(cavity.colixK,
                                                    isTranslucent);
      }
      return;
    }
  }

  /*
   * radius and diameter of the probe. 0 == no probe
   */
  float radiusP, diameterP;

  /*
   * these state variables are set by the routines below
   */
  int indexI, indexJ, indexK;
  Atom atomI, atomJ, atomK;
  Point3f centerI, centerJ, centerK;
  private float radiusI, radiusJ, radiusK;
  private float radiiIP, radiiJP, radiiKP;
  private float radiiIP2, radiiJP2, radiiKP2;
  private float distanceIJ, distanceIK, distanceJK;
  private float distanceIJ2, distanceIK2, distanceJK2;

  void setAtomI(int indexI) {
    if (LOG)
      System.out.println("setAtomI:" + indexI);
    this.indexI = indexI;
    atomI = frame.atoms[indexI];
    centerI = atomI.point3f;
    radiusI = atomI.getVanderwaalsRadiusFloat();
    radiiIP = radiusI + radiusP;
    radiiIP2 = radiiIP * radiiIP;
  }

  void setNeighborJ(int sortedNeighborIndex) {
    indexJ = neighborIndexes[sortedNeighborIndex];
    if (LOG)
      System.out.println(" setNeighborJ:" + indexJ);
    atomJ = neighborAtoms[sortedNeighborIndex];
    radiusJ = atomJ.getVanderwaalsRadiusFloat();
    radiiJP = neighborPlusProbeRadii[sortedNeighborIndex];
    radiiJP2 = neighborPlusProbeRadii2[sortedNeighborIndex];
    centerJ = neighborCenters[sortedNeighborIndex];
    distanceIJ2 = centerJ.distanceSquared(centerI);
    distanceIJ = (float)Math.sqrt(distanceIJ2);
  }

  void setNeighborK(int sortedNeighborIndex) {
    indexK = neighborIndexes[sortedNeighborIndex];
    if (LOG)
      System.out.println("  setNeighborK:" + indexK);
    atomK = neighborAtoms[sortedNeighborIndex];
    radiusK = atomK.getVanderwaalsRadiusFloat();
    radiiKP = neighborPlusProbeRadii[sortedNeighborIndex];
    radiiKP2 = neighborPlusProbeRadii2[sortedNeighborIndex];
    centerK = neighborCenters[sortedNeighborIndex];
    distanceIK2 = centerK.distanceSquared(centerI);
    distanceIK = (float)Math.sqrt(distanceIK2);
    distanceJK2 = centerK.distanceSquared(centerJ);
    distanceJK = (float)Math.sqrt(distanceJK2);
  }

  void calcVertexBitmapI() {
    Bmp.setAllBits(tempVertexMap, geodesicVertexCount);
    if (neighborCount > 0) {
      int iLastUsed = 0;
      for (int iDot = geodesicVertexCount; --iDot >= 0; ) {
        pointT.set(geodesicVertexVectors[iDot]);
        pointT.scaleAdd(radiiIP, centerI);
        int iStart = iLastUsed;
        do {
          if (pointT.distanceSquared(neighborCenters[iLastUsed])
              < neighborPlusProbeRadii2[iLastUsed]) {
            Bmp.clearBit(tempVertexMap, iDot);
            break;
          }
          if (++iLastUsed == neighborCount)
            iLastUsed = 0;
        } while (iLastUsed != iStart);
      }
    }
    Bmp.orInto(convexVertexMaps[indexI], tempVertexMap);
  }

  int[] calcFaceBitmap(int[] vertexMap) {
    Bmp.clearBitmap(tempFaceMap);
    for (int i = geodesicFaceCount, j = 3 * (i - 1); --i >= 0; j -= 3) {
      if (Bmp.getBit(vertexMap, geodesicFaceVertexes[j]) &&
          Bmp.getBit(vertexMap, geodesicFaceVertexes[j + 1]) &&
          Bmp.getBit(vertexMap, geodesicFaceVertexes[j + 2]))
        Bmp.setBit(tempFaceMap, i);
    }
    return Bmp.copyMinimalBitmap(tempFaceMap);
  }

  // I have no idea what this number should be
  int neighborCount;
  Atom[] neighborAtoms = new Atom[16];
  int[] neighborIndexes = new int[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii = new float[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  int[] sortedNeighborIndexes = new int[16];
  
  void getNeighbors(BitSet bsSelected) {
    /*
    System.out.println("Surface.getNeighbors radiusI=" + radiusI +
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
      float neighborRadius = neighbor.getVanderwaalsRadiusFloat();
      if (centerI.distance(neighbor.point3f) >
          radiusI + radiusP + radiusP + neighborRadius)
        continue;
      if (neighborCount == neighborAtoms.length) {
        neighborAtoms = (Atom[])Util.doubleLength(neighborAtoms);
        neighborIndexes = Util.doubleLength(neighborIndexes);
        neighborCenters = (Point3f[])Util.doubleLength(neighborCenters);
        neighborPlusProbeRadii = Util.doubleLength(neighborPlusProbeRadii);
        neighborPlusProbeRadii2 = Util.doubleLength(neighborPlusProbeRadii2);
      }
      neighborAtoms[neighborCount] = neighbor;
      neighborCenters[neighborCount] = neighbor.point3f;
      neighborIndexes[neighborCount] = neighbor.atomIndex;
      float radii = neighborRadius + radiusP;
      neighborPlusProbeRadii[neighborCount] = radii;
      neighborPlusProbeRadii2[neighborCount] = radii * radii;
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
      me.distance(neighborAtoms[i].getPoint3f()));
      }
    */
  }

  void sortNeighborIndexes() {
    sortedNeighborIndexes =
      Util.ensureLength(sortedNeighborIndexes, neighborCount);
    for (int i = neighborCount; --i >= 0; )
      sortedNeighborIndexes[i] = i;
    for (int i = neighborCount; --i >= 0; )
      for (int j = i; --j >= 0; )
        if (neighborIndexes[sortedNeighborIndexes[i]] >
            neighborIndexes[sortedNeighborIndexes[j]]) {
          int t = sortedNeighborIndexes[i];
          sortedNeighborIndexes[i] = sortedNeighborIndexes[j];
          sortedNeighborIndexes[j] = t;
        }
  }

  void deleteUnusedToruses() {
    boolean torusDeleted = false;
    for (int i = torusCount; --i >= 0; ) {
      Torus torus = toruses[i];
      if (convexVertexMaps[torus.ixA] == null &&
          convexVertexMaps[torus.ixB] == null) {
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
  final AxisAngle4f aaT1 = new AxisAngle4f();

  final AxisAngle4f aaAxis = new AxisAngle4f();
  final Matrix3f matrixAxis = new Matrix3f();
  final AxisAngle4f aaOuterTangent = new AxisAngle4f();
  final Matrix3f matrixOuterTangent = new Matrix3f();

  static final Vector3f vectorNull = new Vector3f();
  static final Vector3f vectorX = new Vector3f(1, 0, 0);
  static final Vector3f vectorY = new Vector3f(0, 1, 0);
  static final Vector3f vectorZ = new Vector3f(0, 0, 1);

  final Vector3f vectorT = new Vector3f();
  final Vector3f vectorT1 = new Vector3f();

  final Vector3f vectorTorusT = new Vector3f();
  final Vector3f vectorTorusTangentT = new Vector3f();

  final Vector3f vectorPI = new Vector3f();
  final Vector3f vectorPJ = new Vector3f();

  final Vector3f vectorPA = new Vector3f();
  final Vector3f vectorPB = new Vector3f();

  final Vector3f vOrganizeA = new Vector3f();
  final Vector3f vOrganizeB = new Vector3f();

  final Vector3f axisVector = new Vector3f();
  final Vector3f unitRadialVector = new Vector3f();
  final Vector3f radialVector = new Vector3f();
  // 90 degrees, although everything is in radians
  final Vector3f radialVector90 = new Vector3f();
  // 270 degrees
  final Vector3f radialVector270 = new Vector3f();
  final Vector3f cavityProbeVector = new Vector3f();
  final Point3f pointTorusP = new Point3f();
  final Vector3f vectorTorusP = new Vector3f();
  final AxisAngle4f aaRotate = new AxisAngle4f();
  Point3f pointAtomI;
  Point3f pointAtomJ;

  final Vector3f vectorIP = new Vector3f();
  final Vector3f vectorJP = new Vector3f();

  float[] cavityAngles = new float[32];

  final static int INNER_TORUS_STEP_COUNT = 12;
  final static float INNER_TORUS_STEP_ANGLE =
    (float)(2 * Math.PI / INNER_TORUS_STEP_COUNT);
  // note that this is the number of steps in 180 degrees, not 360
  final static int OUTER_TORUS_STEP_COUNT = 11;
  final Vector3f outerRadials[] = new Vector3f[OUTER_TORUS_STEP_COUNT];
  {
    for (int i = outerRadials.length; --i >= 0; )
      outerRadials[i] = new Vector3f();
  }

  class Torus {
    final int ixA, ixB;
    final Point3f center;
    final float radius;
    final Vector3f axisVector;
    final Vector3f radialVector;
    final Vector3f unitRadialVector;
    final Vector3f tangentVector;
    final Vector3f outerRadial;
    final float outerAngle;
    int probeMap;
    short colixA, colixB;
    byte outerPointCount;
    byte probeCount;
    short totalPointCount;
    short[] normixes;

    Torus(int indexA, Point3f centerA, int indexB, Point3f centerB, 
          Point3f center, float radius, boolean fullTorus) {
      this.ixA = indexA;
      this.ixB = indexB;
      this.center = new Point3f(center);
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
      tangentVector.cross(axisVector, radialVector);
      tangentVector.normalize();

      pointTorusP.add(center, radialVector);

      vectorPA.sub(centerA, pointTorusP);
      vectorPA.normalize();
      vectorPA.scale(radiusP);

      vectorPB.sub(centerB, pointTorusP);
      vectorPB.normalize();
      vectorPB.scale(radiusP);

      outerRadial = new Vector3f(vectorPA);
      outerAngle = vectorPA.angle(vectorPB);

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

      aaRotate.set(matrixT);
    }

    void calcEverything() {
      calcProbeMap();
      calcPointCounts();
      calcNormixes();
    }

    void calcProbeMap() {
      // note that this probe map puts step 0 in the sign bit
      // this means that as we shift out the bits we can easily
      // test based upon the sign && we can easily determine
      // when there are no bits left;
      int probeMap = (~0 << (32 - INNER_TORUS_STEP_COUNT));
      
      aaT.set(axisVector, 0);
      int iLastNeighbor = 0;
      for (int step = INNER_TORUS_STEP_COUNT; --step >= 0; ) {
        aaT.angle = step * INNER_TORUS_STEP_ANGLE;
        matrixT.set(aaT);
        matrixT.transform(radialVector, pointT);
        pointT.add(center);
        int iStart = iLastNeighbor;
        do {
          if (neighborAtoms[iLastNeighbor].atomIndex != ixB) {
            if (pointT.distanceSquared(neighborCenters[iLastNeighbor])
                < neighborPlusProbeRadii2[iLastNeighbor]) {
              probeMap &= ~(1 << (31 - step));
              break;
            }
          }
          iLastNeighbor = (iLastNeighbor + 1) % neighborCount;
        } while (iLastNeighbor != iStart);
      }
      this.probeMap = probeMap;
    }

    void calcPointCounts() {
      int c = (int)(OUTER_TORUS_STEP_COUNT * outerAngle / Math.PI);
      outerPointCount = (byte)c;

      // count how many bits are in the probeMap;
      int t = probeMap;
      t = ((t & 0x55555555) + ((t >> 1) & 0x55555555));
      t = ((t & 0x33333333) + ((t >> 2) & 0x33333333));
      t = ((t & 0x0F0F0F0F) + ((t >> 4) & 0x0F0F0F0F));
      t = ((t & 0x00FF00FF) + ((t >> 8) & 0x00FF00FF));
      t =  (t & 0x0000FFFF) +  (t >>16);
      probeCount = (byte)t;
      totalPointCount = (short)(t * c);
    }

    void transformOuterRadials() {
      float stepAngle1 =
        (outerPointCount <= 1) ? 0 : outerAngle / (outerPointCount - 1);
      aaT1.set(tangentVector, stepAngle1 * outerPointCount);
      for (int i = outerPointCount; --i > 0; ) {
        aaT1.angle -= stepAngle1;
        matrixT1.set(aaT1);
        matrixT1.transform(outerRadial, outerRadials[i]);
      }
      outerRadials[0].set(outerRadial);
    }

    void calcNormixes() {
      if (probeCount == 0)
        return;
      short[] normixes = this.normixes = new short[totalPointCount];
      transformOuterRadials();
      aaT.set(axisVector, 0);
      int outerPointCount = this.outerPointCount;
      int ixP = 0;
      for (int i = 0, probeT = probeMap; probeT != 0; ++i, probeT <<= 1) {
        if (probeT >= 0)
          continue;
        aaT.angle = i * INNER_TORUS_STEP_ANGLE;
        matrixT.set(aaT);
        for (int j = 0; j < outerPointCount; ++j, ++ixP) {
          matrixT.transform(outerRadials[j], vectorT);
          normixes[ixP] = g3d.get2SidedNormix(vectorT);
        }
      }
    }

    void connect() {
      if (indexI != ixA)
        throw new NullPointerException();
      if (indexJ != ixB)
        throw new NullPointerException();
      if (LOG)
        System.out.println("connect " + ixA + ":" + ixB);
    }

    void addCavity(Cavity cavity, boolean rightHanded) {
    }

    void calcPoints(Point3f[] points) {
      if (probeCount == 0)
        return;
      transformOuterRadials();
      aaT.set(axisVector, 0);
      int outerPointCount = this.outerPointCount;
      int ixP = 0;
      for (int i = 0, probeT = probeMap; probeT != 0; ++i, probeT <<= 1) {
        if (probeT >= 0)
          continue;
        aaT.angle = i * INNER_TORUS_STEP_ANGLE;
        matrixT.set(aaT);
        matrixT.transform(radialVector, pointT);
        pointT.add(center);
        
        for (int j = 0; j < outerPointCount; ++j, ++ixP) {
          matrixT.transform(outerRadials[j], vectorT);
          points[ixP].add(pointT, vectorT);
        }
      }
    }

    void calcScreens(Point3f[] points, Point3i[] screens) {
      for (int i = totalPointCount; --i >= 0; )
        viewer.transformPoint(points[i], screens[i]);
    }
  }

  class TorusX {
    int ixI, ixJ;
    Point3f center;
    float radius;
    short normixI, normixJ; // the closest points
    //    Vector3f axisVector;
    //    Vector3f radialVector;
    //    Vector3f unitRadialVector;
    //    Vector3f tangentVector;
    //    Vector3f outerRadial;
    //    float outerAngle;
    //    AxisAngle4f aaRotate;
    //    short colixI, colixJ;
    boolean fullTorus;
    short[] cavityConnections;
    int connectionCount;
    //    Vector3f outerVector;
    //    float outerRadians;
    //    Point3f[][] stripPointArrays;
    //    short[][] stripNormixesArrays;

    int pointspCount;
    Point3f[] pointsp;
    short[] vertexesIP;
    short[] vertexesJP;

    int ijTriangleCount;
    short[] ijTriangles;

    int jiTriangleCount;
    short[] jiTriangles;

    int torusCavityCount;
    Cavity[] torusCavities = new Cavity[8];
    boolean[] torusCavityOpens = new boolean[8];

    TorusX(int indexA, Point3f centerA, int indexB, Point3f centerB, 
          Point3f center, float radius, boolean fullTorus) {
      this.ixI = indexA;
      this.ixJ = indexB;
      this.center = new Point3f(center);
      this.radius = radius;
      this.fullTorus = fullTorus;

      /*
      axisVector = new Vector3f();
      axisVector.sub(centerB, centerA);

      if (axisVector.z == 0)
        unitRadialVector = vectorZ;
      else {
        unitRadialVector = new Vector3f(-axisVector.y, axisVector.x, 0);
        unitRadialVector.normalize();
      }
      radialVector = new Vector3f(unitRadialVector);
      radialVector.scale(radius);

      tangentVector = new Vector3f();
      tangentVector.cross(axisVector, radialVector);
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

      outerVector = new Vector3f(vectorPI);
      outerVector.normalize();
      outerVector.scale(radiusP);

      outerRadians = outerVector.angle(vectorPJ);
      */

    }

    void addCavityConnection(short vertexI, short vertexJ) {
      for (int i = connectionCount; (i -= 2) >= 0; )
        if (cavityConnections[i] == vertexI &&
            cavityConnections[i+1] == vertexJ)
          return;
      if (cavityConnections == null ||
          connectionCount == cavityConnections.length)
        cavityConnections = Util.doubleLength(cavityConnections);
      cavityConnections[connectionCount++] = vertexI;
      cavityConnections[connectionCount++] = vertexJ;
      Bmp.setBit(convexVertexMaps[ixI], vertexI);
      Bmp.setBit(convexVertexMaps[ixJ], vertexJ);
    }

    void addCavity(Cavity cavity, boolean rightHanded) {
      if (LOG)
        System.out.println("torus " + ixI + ":" + ixJ +
                           " addCavity");
      if (torusCavityCount == torusCavities.length) {
        torusCavities = (Cavity[])Util.doubleLength(torusCavities);
        torusCavityOpens = Util.doubleLength(torusCavityOpens);
      }
      torusCavities[torusCavityCount] = cavity;
      torusCavityOpens[torusCavityCount] = rightHanded;
      ++torusCavityCount;

      if (cavity.ixI == ixI) {
        if (cavity.ixJ == ixJ)
          addCavityConnection(cavity.vertexI, cavity.vertexJ);
        else if (cavity.ixK == ixJ)
          addCavityConnection(cavity.vertexI, cavity.vertexK);
        else
          throw new NullPointerException();
      } else if (cavity.ixJ == ixI) {
        if (cavity.ixK == ixJ)
          addCavityConnection(cavity.vertexJ, cavity.vertexK);
        else
          throw new NullPointerException();
      } else {
        throw new NullPointerException();
      }
    }

    void calcReferenceVectors() {
      pointAtomI = frame.atoms[ixI].point3f;
      pointAtomJ = frame.atoms[ixJ].point3f;
      axisVector.sub(pointAtomJ, pointAtomI);
      
      if (axisVector.z == 0)
        unitRadialVector.set(vectorZ);
      else {
        unitRadialVector.set(-axisVector.y, axisVector.x, 0);
        unitRadialVector.normalize();
      }
      radialVector.scale(radius, unitRadialVector);
      
      aaRotate.set(axisVector, PI / 2);
      matrixT.set(aaRotate);
      matrixT.transform(radialVector, radialVector90);

      aaRotate.angle = 3 * PI / 2;
      matrixT.set(aaRotate);
      matrixT.transform(radialVector, radialVector270);

    }

    void calcCavityAngles() {
      cavityAngles = Util.ensureLength(cavityAngles, torusCavityCount);
      for (int i = torusCavityCount; --i >= 0; )
        cavityAngles[i] = calcCavityAngle(torusCavities[i]);
    }

    float calcCavityAngle(Cavity cavity) {
      cavityProbeVector.sub(cavity.probeCenter, center);
      float angleCavity0 = cavityProbeVector.angle(radialVector);
      float angleCavity90 = cavityProbeVector.angle(radialVector90);
      float angleCavity270 = cavityProbeVector.angle(radialVector270);
      if (angleCavity90 <= angleCavity270)
        return angleCavity0;
      else
        return PI + angleCavity0;
    }

    void sortCavitiesByAngle() {
      if (LOG)
        System.out.println("sortCavitiesByAngle " +
                           ixI + ":" + ixJ +
                           " torusCavityCount=" + torusCavityCount);
      for (int i = torusCavityCount; --i >= 0; )
        for (int j = i; --j >= 0; )
          if (cavityAngles[i] < cavityAngles[j]) {
            float t = cavityAngles[i];
            cavityAngles[i] = cavityAngles[j];
            cavityAngles[j] = t;
            
            Cavity x = torusCavities[i];
            torusCavities[i] = torusCavities[j];
            torusCavities[j] = x;

            boolean b = torusCavityOpens[i];
            torusCavityOpens[i] = torusCavityOpens[j];
            torusCavityOpens[j] = b;
          }
      for (int i = torusCavityCount; --i >= 0; ) {
        System.out.println("i=" + i + torusCavityOpens[i]);
      }
    }
    
    void connect() {
      if (indexI != ixI)
        throw new NullPointerException();
      if (indexJ != ixJ)
        throw new NullPointerException();
      if (LOG)
        System.out.println("connect " + ixI + ":" + ixJ);

      calcReferenceVectors();
      calcCavityAngles();
      sortCavitiesByAngle();
      int sortedCavityIndex = 0;
      
      int numSteps = torusStepCounts[geodesicRenderingLevel];
      pointspCount = 0;
      pointsp = new Point3f[numSteps];
      float stepRadians = 2 * PI / numSteps;
      for (int i = 0; i < numSteps; ++i) {
        float angle = i * stepRadians;
        // first pick up any cavities that may be in this step
        for ( ;
              (sortedCavityIndex < torusCavityCount &&
               cavityAngles[sortedCavityIndex] <= angle);
              ++sortedCavityIndex) {
          if (LOG)
            System.out.println("cavity angle=" +
                               cavityAngles[sortedCavityIndex]);
          addCavityConnection(torusCavities[sortedCavityIndex]);
        }
        if (LOG)
          System.out.println("angle=" + angle);
        aaRotate.angle = angle;
        matrixT.set(aaRotate);
        matrixT.transform(radialVector, vectorTorusP);
        pointTorusP.add(center, vectorTorusP);
        if (! checkProbeNotIJ(pointTorusP))
          continue;
        pointsp[pointspCount] = new Point3f(pointTorusP);
        vectorIP.sub(pointTorusP, pointAtomI);
        vectorJP.sub(pointTorusP, pointAtomJ);
        short vertexIP = g3d.getNormix(vectorIP, geodesicRenderingLevel);
        short vertexJP = g3d.getNormix(vectorJP, geodesicRenderingLevel);
        addConnection(vertexIP, vertexJP);
      }
      
      for (int m = 0; m < pointspCount; ++m) {
        int n = m + 1;
        if (n == pointspCount)
          n = 0;
        if (vertexesJP[m] != vertexesJP[n])
          continue;
        if (! g3d.isNeighborVertex(vertexesIP[m], vertexesIP[n],
                                   geodesicRenderingLevel))
          continue;
        if (ijTriangles == null ||
            ijTriangleCount + 3 >= ijTriangles.length)
          ijTriangles = Util.doubleLength(ijTriangles);
        ijTriangles[ijTriangleCount++] = vertexesIP[m];
        ijTriangles[ijTriangleCount++] = vertexesJP[m];
        ijTriangles[ijTriangleCount++] = vertexesIP[n];
      }

      for (int m = 0; m < pointspCount; ++m) {
        int n = m + 1;
        if (n == pointspCount)
          n = 0;
        if (vertexesIP[m] != vertexesIP[n])
          continue;
        if (! g3d.isNeighborVertex(vertexesJP[m], vertexesJP[n],
                                   geodesicRenderingLevel))
          continue;
        if (jiTriangles == null ||
            jiTriangleCount + 3 >= jiTriangles.length)
          jiTriangles = Util.doubleLength(jiTriangles);
        jiTriangles[jiTriangleCount++] = vertexesJP[m];
        jiTriangles[jiTriangleCount++] = vertexesIP[m];
        jiTriangles[jiTriangleCount++] = vertexesJP[n];
      }

      for (int m = 0; m < pointspCount; ++m) {
        int n = m + 1;
        if (n == pointspCount)
          n = 0;
        if (vertexesIP[m] == vertexesIP[n] ||
            vertexesJP[m] == vertexesJP[n])
          continue;
        if (! g3d.isNeighborVertex(vertexesIP[m], vertexesIP[n],
                                   geodesicRenderingLevel) ||
            ! g3d.isNeighborVertex(vertexesJP[m], vertexesJP[n],
                                   geodesicRenderingLevel))
          continue;

        if (ijTriangles == null ||
            ijTriangleCount + 3 >= ijTriangles.length)
          ijTriangles = Util.doubleLength(ijTriangles);
        ijTriangles[ijTriangleCount++] = vertexesIP[m];
        ijTriangles[ijTriangleCount++] = vertexesJP[m];
        ijTriangles[ijTriangleCount++] = vertexesIP[n];

        if (jiTriangles == null ||
            jiTriangleCount + 3 >= jiTriangles.length)
          jiTriangles = Util.doubleLength(jiTriangles);
        jiTriangles[jiTriangleCount++] = vertexesJP[m];
        jiTriangles[jiTriangleCount++] = vertexesIP[n];
        jiTriangles[jiTriangleCount++] = vertexesJP[n];
      }

      torusCavities = null;
      cavityConnections = null;
    }

    void addCavityConnection(Cavity cavity) {
      if (cavity.ixI == ixI) {
        if (cavity.ixJ == ixJ)
          addConnection(cavity.vertexI, cavity.vertexJ);
        else if (cavity.ixK == ixJ)
          addConnection(cavity.vertexI, cavity.vertexK);
        else
          throw new NullPointerException();
      } else if (cavity.ixJ == ixI) {
        if (cavity.ixK == ixJ)
          addConnection(cavity.vertexJ, cavity.vertexK);
        else
          throw new NullPointerException();
      } else {
        throw new NullPointerException();
      }
    }

    void addConnection(short vertexIP, short vertexJP) {
      if (pointspCount > 0 &&
          vertexesIP[pointspCount - 1] == vertexIP &&
          vertexesJP[pointspCount - 1] == vertexJP)
        return;

      if (vertexesIP == null || pointspCount == vertexesIP.length)
        vertexesIP = Util.doubleLength(vertexesIP);
      vertexesIP[pointspCount] = vertexIP;
      Bmp.setBit(convexVertexMaps[ixI], vertexIP);

      if (vertexesJP == null || pointspCount == vertexesJP.length)
        vertexesJP = Util.doubleLength(vertexesJP);
      vertexesJP[pointspCount] = vertexJP;
      Bmp.setBit(convexVertexMaps[ixJ], vertexJP);
      ++pointspCount;
    }

    boolean isNeighborVertex(int v1, int v2) {
      return v1 != v2;
    }

    /*
    void calcCloseNormixes() {
      Point3f centerI = frame.atoms[ixI].point3f;
      Point3f centerJ = frame.atoms[ixJ].point3f;
      axisVectorT.sub(centerI, centerJ);
      if (axisVectorT.z == 0)
        unitRadialVectorT.set(vectorZ);
      else {
        unitRadialVectorT.set(-axisVectorT.y, axisVectorT.x, 0);
        unitRadialVectorT.normalize();
      }
      radialVectorT.set(unitRadialVectorT);
      radialVectorT.scale(radius);
      pointTorusP.add(center, radialVectorT);
      vectorIP.sub(pointTorusP, centerI);
      normixI = g3d.getNormix(vectorIP, geodesicRenderingLevel);
      vectorJP.sub(pointTorusP, centerJ);
      normixJ = g3d.getNormix(vectorJP, geodesicRenderingLevel);
      
      convexVertexMaps[ixI] = Bmp.setBitGrow(convexVertexMaps[ixI], normixI);
      convexVertexMaps[ixJ] = Bmp.setBitGrow(convexVertexMaps[ixJ], normixJ);
      }
    */
      
  }

  void allocateConvexVertexBitmap(int atomIndex) {
    if (convexVertexMaps[atomIndex] == null)
      convexVertexMaps[atomIndex] = Bmp.allocateBitmap(geodesicVertexCount);
  }

  Torus createTorus(int indexI, Point3f centerI, int indexJ, Point3f centerJ,
                    Point3f torusCenterIJ, float torusRadius,
                    boolean fullTorus) {
    if (indexI >= indexJ)
      throw new NullPointerException();
    Long key = new Long(((long)indexI << 32) + indexJ);
    if (htToruses.get(key) != null)
      throw new NullPointerException();
    allocateConvexVertexBitmap(indexI);
    allocateConvexVertexBitmap(indexJ);
    Torus torus = new Torus(indexI, centerI, indexJ, centerJ,
                            torusCenterIJ, torusRadius, fullTorus);
    htToruses.put(key, torus);
    saveTorus(torus);
    return torus;
  }

  void saveTorus(Torus torus) {
    if (toruses == null)
      toruses = new Torus[128];
    else if (torusCount == toruses.length)
      toruses = (Torus[])Util.doubleLength(toruses);
    toruses[torusCount++] = torus;
  }

  Torus getTorus(int atomIndexA, int atomIndexB) {
    if (atomIndexA >= atomIndexB)
      throw new NullPointerException();
    Long key = new Long(((long)atomIndexA << 32) + atomIndexB);
    return (Torus)htToruses.get(key);
    //    Object value = htToruses.get(key);
    //    return (value instanceof Torus) ? (Torus)value : null;
  }

  float calcTorusRadius(float radiusA, float radiusB, float distanceAB2) {
    float t1 = radiusA + radiusB + diameterP;
    float t2 = t1*t1 - distanceAB2;
    float diff = radiusA - radiusB;
    float t3 = distanceAB2 - diff*diff;
    if (t2 <= 0 || t3 <= 0 || distanceAB2 == 0) {
      System.out.println("calcTorusRadius\n" +
                         " radiusA=" + radiusA + " radiusB=" + radiusB +
                         " distanceAB2=" + distanceAB2);
      System.out.println("distanceAB=" + Math.sqrt(distanceAB2) +
                         " t1=" + t1 + " t2=" + t2 +
                         " diff=" + diff + " t3=" + t3);
      throw new NullPointerException();
    }
    return (float)(0.5*Math.sqrt(t2)*Math.sqrt(t3)/Math.sqrt(distanceAB2));
  }

  void calcCavitiesI() {
    if (radiusP == 0)
      return;
    if (cavities == null) {
      cavities = new Cavity[32];
      cavityCount = 0;
    }
    for (int iJ = neighborCount; --iJ >= 0; ) {
      int sortedIndexJ = sortedNeighborIndexes[iJ];
      if (neighborIndexes[sortedIndexJ] <= indexI)
        continue;
      setNeighborJ(sortedIndexJ);
      // deal with corrupt files that have duplicate atoms
      if (distanceIJ < 0.1)
        continue;
      vectorIJ.sub(centerJ, centerI);
      calcTorusCenter(centerI, radiiIP2, centerJ, radiiJP2, distanceIJ2,
                      torusCenterIJ);
      for (int iK = neighborCount; --iK >= 0; ) {
        int sortedIndexK = sortedNeighborIndexes[iK];
        if (neighborIndexes[sortedIndexK] <= indexJ)
          continue;
        setNeighborK(sortedIndexK);
        // deal with corrupt files that have duplicate atoms
        if (distanceIK < 0.1 || distanceJK < 0.1)
          continue;
        if (distanceJK >= radiiJP + radiiKP)
          continue;
        getCavitiesIJK();
      }
      checkFullTorusIJ();
      Torus torusIJ = getTorus(indexI, indexJ);
      if (torusIJ != null)
        torusIJ.calcEverything();
    }
    // check for an isolated atom with no neighbors
    if (neighborCount == 0)
      allocateConvexVertexBitmap(indexI);
  }

  // check for a full torus with no cavities between I & J
  void checkFullTorusIJ() {
    if (getTorus(indexI, indexJ) == null) {
      if (vectorIJ.z == 0)
        unitRadialVector.set(vectorZ);
      else {
        unitRadialVector.set(-vectorIJ.y, vectorIJ.x, 0);
        unitRadialVector.normalize();
      }
      float torusRadiusIJ = calcTorusRadius(radiusI, radiusJ, distanceIJ2);
      pointTorusP.scaleAdd(torusRadiusIJ, unitRadialVector, torusCenterIJ);
      if (checkProbeNotIJ(pointTorusP))
        createTorus(indexI, centerI, indexJ, centerJ,
                    torusCenterIJ, torusRadiusIJ, true);
    }
  }

  void deleteUnusedCavities() {
    boolean cavityDeleted = false;
    for (int i = cavityCount; --i >= 0; ) {
      Cavity cavity = cavities[i];
      if (convexVertexMaps[cavity.ixI] == null &&
          convexVertexMaps[cavity.ixJ] == null &&
          convexVertexMaps[cavity.ixK] == null) {
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

  private final Vector3f vectorIJ = new Vector3f();
  private final Vector3f vectorIK = new Vector3f();
  private final Vector3f normalIJK = new Vector3f();
  private final Point3f torusCenterIJ = new Point3f();
  private final Point3f torusCenterIK = new Point3f();
  private final Point3f torusCenterJK = new Point3f();
  private final Point3f probeBaseIJK = new Point3f();
  private final Point3f cavityProbe = new Point3f();

  void getCavitiesIJK() {
    if (LOG)
      System.out.println("getCavitiesIJK:" + indexI + "," + indexJ + "," +
                         indexK);
    vectorIK.sub(centerK, centerI);
    normalIJK.cross(vectorIJ, vectorIK);
    if (Float.isNaN(normalIJK.x))
      return;
    normalIJK.normalize();
    calcTorusCenter(centerI, radiiIP2, centerK, radiiKP2, distanceIK2,
                    torusCenterIK);
    if (! intersectPlanes(vectorIJ, torusCenterIJ,
                          vectorIK, torusCenterIK,
                          normalIJK, centerI,
                          probeBaseIJK))
      return;
    float probeHeight = calcProbeHeightIJK(probeBaseIJK);
    if (probeHeight <= 0)
      return;
    Torus torusIJ = null, torusIK = null, torusJK = null;
    for (int i = -1; i <= 1; i += 2) {
      cavityProbe.scaleAdd(i * probeHeight, normalIJK, probeBaseIJK);
      if (checkProbeAgainstNeighborsIJK(cavityProbe)) {
        boolean rightHanded = (i == 1);
        allocateConvexVertexBitmap(indexI);
        allocateConvexVertexBitmap(indexJ);
        allocateConvexVertexBitmap(indexK);
        Cavity cavity = new Cavity(cavityProbe, probeBaseIJK);
        addCavity(cavity);
        if (LOG)
          System.out.println(" indexI=" + indexI +
                             " indexJ=" + indexJ +
                             " indexK=" + indexK);
        if (torusIJ == null && (torusIJ = getTorus(indexI, indexJ)) == null)
          torusIJ = createTorus(indexI, centerI, indexJ, centerJ,
                                torusCenterIJ,
                                calcTorusRadius(radiusI, radiusJ, distanceIJ2),
                                false);
        torusIJ.addCavity(cavity, rightHanded);
        
        if (torusIK == null && (torusIK = getTorus(indexI, indexK)) == null)
          torusIK = createTorus(indexI, centerI, indexK, centerK,
                                torusCenterIK,
                                calcTorusRadius(radiusI, radiusK, distanceIK2),
                                false);
        torusIK.addCavity(cavity, !rightHanded);

        if (torusJK == null && (torusJK = getTorus(indexJ, indexK)) == null) {
          calcTorusCenter(centerJ, radiiJP2, centerK, radiiKP2, distanceJK2,
                          torusCenterJK);
          torusJK = createTorus(indexJ, centerJ, indexK, centerK,
                                torusCenterJK,
                                calcTorusRadius(radiusJ, radiusK, distanceJK2),
                                false);
        }
        torusJK.addCavity(cavity, rightHanded);
      }
    }
  }


  void calcTorusCenter(Point3f centerA, float radiiAP2,
                       Point3f centerB, float radiiBP2, float distanceAB2,
                       Point3f torusCenter) {
    /*
    System.out.println("calcTorusCenter(" + centerA + "," + radiiAP2 + "," +
                       centerB + "," + radiiBP2 + "," +
                       distanceAB2 + "," + ")");
    */
    torusCenter.sub(centerB, centerA);
    torusCenter.scale((radiiAP2-radiiBP2) / distanceAB2);
    torusCenter.add(centerA);
    torusCenter.add(centerB);
    torusCenter.scale(0.5f);
    /*
    System.out.println("torusCenter=" + torusCenter);
    */
  }

  boolean checkProbeNotIJ(Point3f probeCenter) {
    for (int i = neighborCount; --i >= 0; ) {
      int neighborIndex = neighborIndexes[i];
      if (neighborIndex == indexI ||
          neighborIndex == indexJ)
        continue;
      if (probeCenter.distanceSquared(neighborCenters[i]) <
          neighborPlusProbeRadii2[i])
        return false;
    }
    return true;
  }

  boolean checkProbeAgainstNeighborsIJK(Point3f cavityProbe) {
    for (int i = neighborCount; --i >= 0; ) {
      int neighborIndex = neighborIndexes[i];
      if (neighborIndex == indexI ||
          neighborIndex == indexJ ||
          neighborIndex == indexK)
        continue;
      if (cavityProbe.distanceSquared(neighborCenters[i]) <
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
  final Vector3f p1 = new Vector3f();
  final Vector3f p2 = new Vector3f();
  final Vector3f p3 = new Vector3f();

  // plus use vectorPI and vectorPJ from above;
  final Vector3f vectorPK = new Vector3f();
  final Vector3f vectorCrossIJ = new Vector3f();
  final Vector3f vectorCrossIK = new Vector3f();
  final Vector3f vectorCrossJK = new Vector3f();

  class Cavity {
    final int ixI, ixJ, ixK;

    final Point3f pointPI = new Point3f();
    final Point3f pointPJ = new Point3f();
    final Point3f pointPK = new Point3f();
    final short vertexI, vertexJ, vertexK;
    short colixI, colixJ, colixK;
    final Point3f probeCenter;
    int[] cavityVertexMap;
    int[] cavityFaceMap;

    // probeCenter is the center of the probe
    // probeBase is the midpoint between this cavity
    // and its mirror image on the other side
    Cavity(Point3f probeCenter, Point3f probeBase) {
      this.probeCenter = new Point3f(probeCenter);
      ixI = indexI; ixJ = indexJ; ixK = indexK;

      vectorPI.sub(centerI, probeCenter);
      vectorPI.normalize();
      vectorPI.scale(radiusP);
      pointPI.add(vectorPI, probeCenter);

      vectorPJ.sub(centerJ, probeCenter);
      vectorPJ.normalize();
      vectorPJ.scale(radiusP);
      pointPJ.add(vectorPJ, probeCenter);

      vectorPK.sub(centerK, probeCenter);
      vectorPK.normalize();
      vectorPK.scale(radiusP);
      pointPK.add(vectorPK, probeCenter);

      // calc nearest geodesic vertexes 
      vectorT.sub(probeCenter, frame.atoms[ixI].point3f);
      vertexI = g3d.getNormix(vectorT, geodesicRenderingLevel);
      vectorT.sub(probeCenter, frame.atoms[ixJ].point3f);
      vertexJ = g3d.getNormix(vectorT, geodesicRenderingLevel);
      vectorT.sub(probeCenter, frame.atoms[ixK].point3f);
      vertexK = g3d.getNormix(vectorT, geodesicRenderingLevel);

      Bmp.setBitGrow(convexVertexMaps[ixI], vertexI);
      Bmp.setBitGrow(convexVertexMaps[ixJ], vertexJ);
      Bmp.setBitGrow(convexVertexMaps[ixK], vertexK);

      /*
      System.out.println(" vertexI=" + vertexI +
                         " vertexJ=" + vertexJ +
                         " vertexK=" + vertexK);

      */

      ////////////////////////////////////////////////////////////////
      // separately, let's find the geodesics that lie in the cavity
      ////////////////////////////////////////////////////////////////
      calcVertexBitmapCavity(vectorPI, vectorPJ, vectorPK);
    }

    void calcVertexBitmapCavity(Vector3f vectorPI, Vector3f vectorPJ,
                                Vector3f vectorPK) {
      int visibleVertexCount = 0;
      Bmp.clearBitmap(tempVertexMap);
      vectorCrossIJ.cross(vectorPI, vectorPJ);
      vectorCrossIK.cross(vectorPI, vectorPK);
      vectorCrossJK.cross(vectorPJ, vectorPK);
      for (int i = geodesicVertexCount; --i >= 0; ) {
        Vector3f probeVertex = probeVertexVectors[i];
        if (sameSide(vectorCrossIJ, vectorPK, probeVertex) &&
            sameSide(vectorCrossIK, vectorPJ, probeVertex) &&
            sameSide(vectorCrossJK, vectorPI, probeVertex)) {
          ++visibleVertexCount;
          Bmp.setBit(tempVertexMap, i);
        }
      }
      
      cavityVertexMap = Bmp.copyMinimalBitmap(tempVertexMap);
      if (cavityVertexMap != null)
        cavityFaceMap = calcFaceBitmap(cavityVertexMap);
      System.out.println("visibleVertexCount=" + visibleVertexCount);
    }

    boolean sameSide(Vector3f normal, Vector3f pointA, Vector3f pointB) {
      boolean positivePointA = normal.dot(pointA) >= 0;
      boolean positivePointB = normal.dot(pointB) >= 0;
      return ! (positivePointA ^ positivePointB);
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
  /*
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
  */
  
  final Vector3f v2v3 = new Vector3f();
  final Vector3f v3v1 = new Vector3f();
  final Vector3f v1v2 = new Vector3f();

  boolean intersectPlanes(Vector3f v1, Point3f p1,
                          Vector3f v2, Point3f p2,
                          Vector3f v3, Point3f p3,
                          Point3f intersection) {
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
    vectorT.set(p1);
    intersection.scale(v1.dot(vectorT), v2v3);
    vectorT.set(p2);
    intersection.scaleAdd(v2.dot(vectorT), v3v1, intersection);
    vectorT.set(p3);
    intersection.scaleAdd(v3.dot(vectorT), v1v2, intersection);
    intersection.scale(1 / denominator);
    if (Float.isNaN(intersection.x))
      return false;
    return true;
  }
  
  float calcProbeHeightIJK(Point3f probeBaseIJK) {
    float hypotenuse2 = radiiIP2;
    vectorT.sub(probeBaseIJK, centerI);
    float baseLength2 = vectorT.lengthSquared();
    float height2 = hypotenuse2 - baseLength2;
    if (height2 <= 0)
      return 0;
    return (float)Math.sqrt(height2);
  }

  final Vector3f vectorRightT = new Vector3f();
  final Matrix3f matrixRightT = new Matrix3f();
  final AxisAngle4f aaRightT = new AxisAngle4f();

  final float getAngle(boolean rightHanded,
                       Vector3f vA, Vector3f vB, Vector3f axis) {
    float angle = vA.angle(vB);
    float longerAngle = (float)(2 * Math.PI) - angle;
    if (! rightHanded) {
      angle = -angle;
      longerAngle = -longerAngle;
    }
    aaRightT.set(axis, angle);
    matrixRightT.set(aaRightT);
    matrixRightT.transform(vA, vectorRightT);
    float dotAngle = vectorRightT.dot(vB);
    aaRightT.angle = longerAngle;
    matrixRightT.set(aaRightT);
    matrixRightT.transform(vA, vectorRightT);
    float dotLongerAngle = vectorRightT.dot(vB);
    if (dotLongerAngle > dotAngle)
      angle = longerAngle;
    return angle;
  }

  int[] calcEdgeVertexes(int[] visibilityBitmap) {
    if (visibilityBitmap == null)
      return null;
    int[] edgeVertexes = new int[visibilityBitmap.length];
    for (int vertex = Bmp.getMaxMappedBit(visibilityBitmap),
           neighborIndex = 6 * (vertex - 1);
         --vertex >= 0;
         neighborIndex -= 6) {
      if (! Bmp.getBit(visibilityBitmap, vertex))
        continue;
      int i;
      for (i = (vertex < 12) ? 5 : 6; --i >= 0; ) {
        int neighbor = geodesicNeighborVertexes[neighborIndex + i];
        if (! Bmp.getBit(visibilityBitmap, neighbor))
          break;
      }
      if (i >= 0)
        Bmp.setBit(edgeVertexes, vertex);
    }
    return edgeVertexes;
  }
}
