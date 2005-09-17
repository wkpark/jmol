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

class Sasurface1 {

  String surfaceID;
  Graphics3D g3d;
  Viewer viewer;
  short colix;
  Frame frame;

  short mad; // this is really just a true/false flag ... 0 vs non-zero

  boolean hide;

  private final static int GEODESIC_CALC_LEVEL =
    Sasurface.MAX_GEODESIC_RENDERING_LEVEL;
  int geodesicRenderingLevel = GEODESIC_CALC_LEVEL;

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

  IntInt2ObjHash htToruses;

  final private static boolean LOG = false;

  private final static short[] torusStepCounts = {20,40,40,80};

  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();
  final Point3f zeroPointT = new Point3f();

  private final static float PI = (float)Math.PI;

  final static int segmentsPerFullCircle = 50;
  final static float radiansPerSegment = 2 * PI / segmentsPerFullCircle;

  final Point3f[] pointStripT = new Point3f[segmentsPerFullCircle];
  final Vector3f stripSurfaceVector = new Vector3f();
  final Vector3f outerSurfaceVector = new Vector3f();
  final Point3f outerCenterPoint = new Point3f();
  final Point3f outerSurfacePoint = new Point3f();

  final Vector3f torusCavityAngleVector = new Vector3f();

  Vector3f[] probeVertexVectors;
    
  Sasurface1(String surfaceID, Viewer viewer, Graphics3D g3d, short colix,
             BitSet bs) {
    this.surfaceID = surfaceID;
    this.viewer = viewer;
    this.g3d = g3d;
    this.colix = colix;

    frame = viewer.getFrame();
    initShape();
    generate(bs);
  }

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

  void clearAll() {
    surfaceConvexMax = 0;
    convexVertexMaps = null;
    convexFaceMaps = null;
    torusCount = 0;
    toruses = null;
    cavityCount = 0;
    cavities = null;
    htToruses = null;
    radiusP = viewer.getCurrentSolventProbeRadius();
    diameterP = 2 * radiusP;
    calcProbeVectors();
  }

  void generate(BitSet bsSelected) {
    viewer.setSolventOn(true);
    clearAll();
    int atomCount = frame.atomCount;
    convexVertexMaps = new int[atomCount][];
    convexFaceMaps = new int[atomCount][];
    colixesConvex = new short[atomCount];

    htToruses = new IntInt2ObjHash();
    // now, calculate surface for selected atoms
    long timeBegin = System.currentTimeMillis();
    int surfaceAtomCount = 0;
    for (int i = 0; i < atomCount; ++i) { // make this loop count up
      if (bsSelected.get(i)) {
        ++surfaceAtomCount;
        setAtomI(i);
        getNeighbors(bsSelected);
        sortNeighborIndexes();
        calcCavitiesI();
      }
    }
    
    for (int i = torusCount; --i >= 0; ) {
      Torus torus = toruses[i];
      torus.checkCavityCorrectness0();
      torus.checkCavityCorrectness1();
      torus.electReferenceCavity();
      torus.calcVectors();
      torus.calcCavityAnglesAndSort();
      torus.checkCavityCorrectness2();
      torus.buildTorusSegments();
      torus.calcPointCounts();
      torus.calcNormixes();

      torus.clipVertexMaps();

      // torus.checkTorusSegments();
      torus.connectWithGeodesics();
    }

    for (int i = atomCount; --i >= 0; ) {
      int[] vertexMap = convexVertexMaps[i];
      if (vertexMap != null)
        convexFaceMaps[i] = calcFaceBitmap(vertexMap);
    }

    long timeElapsed = System.currentTimeMillis() - timeBegin;
    System.out.println("surface atom count=" + surfaceAtomCount);
    System.out.println("Surface construction time = " + timeElapsed + " ms");
    htToruses = null;
    // update this count to slightly speed up surfaceRenderer
    int i;
    for (i = atomCount; --i >= 0 && convexVertexMaps[i] == null; )
      {}
    surfaceConvexMax = i + 1;
  }

  void setSize(int size, BitSet bsSelected) {
    System.out.println("Who is calling me?");
    throw new NullPointerException();
    /*
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

    htToruses = new IntInt2ObjHash();
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
    */
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
      System.out.println("I am surfaceID:" + surfaceID +
                         " Surface.setProperty(color," + value + ")");
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
        if (bs.get(torus.ixA))
          torus.colixA = colix;
        if (bs.get(torus.ixB))
          torus.colixB = colix;
      }
      return;
    }
    if ("translucencySaddle" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      for (int i = torusCount; --i >= 0; ) {
        Torus torus = toruses[i];
        if (bs.get(torus.ixA))
          torus.colixA = Graphics3D.setTranslucent(torus.colixA,
                                                   isTranslucent);
        if (bs.get(torus.ixB))
          torus.colixB = Graphics3D.setTranslucent(torus.colixB,
                                                   isTranslucent);
      }
      return;
    }
    if ("colorConcave" == propertyName) {
      short colix = Graphics3D.getColix(value);
      /*
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
      */
      return;
    }
    if ("translucencyConcave" == propertyName) {
      boolean isTranslucent = ("translucent" == value);
      /*
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
      */
      return;
    }

    if ("off" == propertyName) {
      hide = true;
      return;
    }

    if ("on" == propertyName) {
      hide = false;
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

  /*
  void calcVertexBitmapI() {
    int[] vertexMap = convexVertexMaps[indexI];
    Bmp.setAllBits(vertexMap, geodesicVertexCount);
    if (neighborCount > 0) {
      int iLastUsed = 0;
      for (int iDot = geodesicVertexCount; --iDot >= 0; ) {
        pointT.scaleAdd(radiiIP, geodesicVertexVectors[iDot], centerI);
        pointT1.scaleAdd(radiusI, geodesicVertexVectors[iDot], centerI);
        if (centerI.x == 0)
          System.out.println(" --->" +
                             " vertex[" + iDot + "]" + " radius:" + radiusI +
                             " pointT1:" + pointT1);
        int iStart = iLastUsed;
        do {
          if (pointT.distanceSquared(neighborCenters[iLastUsed])
              < neighborPlusProbeRadii2[iLastUsed]) {
            Bmp.clearBit(vertexMap, iDot);
            break;
          }
          if (++iLastUsed == neighborCount)
            iLastUsed = 0;
        } while (iLastUsed != iStart);
      }
    }
  }
  */

  final Vector3f axisUnitNormalT = new Vector3f();
  final Vector3f centerVectorT = new Vector3f();
  final Point3f vertexPointT = new Point3f();
  final Vector3f vertexVectorT = new Vector3f();
  final Point3f projectedPointT = new Point3f();
  final Vector3f projectedVectorT = new Vector3f();

  void clipGeodesic(Point3f geodesicCenter, float radius,
                    Point3f planePoint, Vector3f axisUnitVector,
                    int[] vertexMap) {
    centerVectorT.sub(geodesicCenter, planePoint);
    float dotCenter = centerVectorT.dot(axisUnitVector);
    if (dotCenter >= radius) // all points are visible
      return;
    if (dotCenter < -radius) { // all points are clipped
      Bmp.clearBitmap(vertexMap);
      return;
    }

    calcClippingPlaneCenter(geodesicCenter, axisUnitVector,
                            planePoint, planeCenterT);

    for (int i = -1; (i = Bmp.nextSetBit(vertexMap, i + 1)) >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[i],
                            geodesicCenter);
      vertexVectorT.sub(vertexPointT, planePoint);
      float dot = vertexVectorT.dot(axisUnitVector);
      if (dot < 0)
        Bmp.clearBit(vertexMap, i);
    }
  }

  int[] bmpNotClipped;

  void findClippedGeodesicEdge(Point3f geodesicCenter, float radius,
                               Point3f planePoint, Vector3f axisUnitVector,
                               int[] edgeVertexMap) {
    if (bmpNotClipped == null)
      bmpNotClipped = Bmp.allocateBitmap(geodesicVertexCount);
    Bmp.clearBitmap(bmpNotClipped);
    axisUnitNormalT.normalize(axisUnitVector);
    for (int i = geodesicVertexCount; --i >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[i],
                            geodesicCenter);
      vertexVectorT.sub(vertexPointT, planePoint);
      if (vertexVectorT.dot(axisUnitNormalT) >= 0)
        Bmp.setBit(bmpNotClipped, i);
    }
    Bmp.clearBitmap(edgeVertexMap);
    for (int v = -1; (v = Bmp.nextSetBit(bmpNotClipped, v + 1)) >= 0; ) {
      int neighborsOffset = v * 6;
      for (int j = (v < 12) ? 5 : 6; --j >= 0; ) {
        int neighbor = geodesicNeighborVertexes[neighborsOffset + j];
        if (! Bmp.getBit(bmpNotClipped, neighbor))
          Bmp.setBit(edgeVertexMap, v);
      }
    }
  }

  int projectedCount;
  short[] projectedVertexes = new short[64];
  float[] projectedAngles = new float[64];
  float[] projectedDistances = new float[64];
  final Vector3f vector0T = new Vector3f();
  final Vector3f vector90T = new Vector3f();
  final Point3f planeCenterT = new Point3f();

  void projectGeodesicPoints(Point3f geodesicCenter, float radius,
                             Point3f planeZeroPoint, Vector3f axisUnitVector,
                             int[] edgeVertexMap) {
    Point3f planeCenterT = this.planeCenterT;
    Vector3f vector0T = this.vector0T;
    Vector3f vector90T = this.vector90T;
    Point3f vertexPointT = this.vertexPointT;
    Vector3f vertexVectorT = this.vertexVectorT;
    Point3f projectedPointT = this.projectedPointT;
    Vector3f projectedVectorT = this.projectedVectorT;

    calcClippingPlaneCenter(geodesicCenter, axisUnitVector, planeZeroPoint,
                            planeCenterT);

    vector0T.sub(planeZeroPoint, planeCenterT);
    aaRotate.set(axisUnitVector, PI / 2);
    matrixT.set(aaRotate);
    matrixT.transform(vector0T, vector90T);

    for (int v = -1; (v = Bmp.nextSetBit(edgeVertexMap, v + 1)) >= 0; ) {
      vertexPointT.scaleAdd(radius, geodesicVertexVectors[v], geodesicCenter);
      vertexVectorT.sub(vertexPointT, planeCenterT);
      float distance = axisUnitVector.dot(vertexVectorT);
      projectedPointT.scaleAdd(-distance, axisUnitVector, vertexPointT);
      projectedVectorT.sub(projectedPointT, planeCenterT);
      float angle = calcAngleInThePlane(vector0T, vector90T, projectedVectorT);
      if (projectedCount == projectedVertexes.length) {
        projectedVertexes = Util.doubleLength(projectedVertexes);
        projectedAngles = Util.doubleLength(projectedAngles);
        projectedDistances = Util.doubleLength(projectedDistances);
      }
      projectedVertexes[projectedCount] = (short) v;
      projectedAngles[projectedCount] = angle;
      projectedDistances[projectedCount] = distance;
      ++projectedCount;
    }
  }

  void calcClippingPlaneCenter(Point3f axisPoint, Vector3f axisUnitVector,
                               Point3f planePoint, Point3f planeCenterPoint) {
    vectorT.sub(axisPoint, planePoint);
    float distance = axisUnitVector.dot(vectorT);
    planeCenterPoint.scaleAdd(-distance, axisUnitVector, axisPoint);
  }

  static float calcAngleInThePlane(Vector3f radialVector0,
                                   Vector3f radialVector90,
                                   Vector3f vectorInQuestion) {
    float angle = radialVector0.angle(vectorInQuestion);
    float angle90 = radialVector90.angle(vectorInQuestion);
    if (angle90 > PI/2)
      angle += PI;
    return angle;
  }

  void sortProjectedVertices() {
    for (int i = projectedCount; --i > 0; )
      for (int j = i - 1; --j >= 0; )
        if (projectedAngles[j] > projectedAngles[i]) {
          Util.swap(projectedAngles, i, j);
          Util.swap(projectedDistances, i, j);
          Util.swap(projectedVertexes, i, j);
        }
  }

  short[] calcTorusGeodesicStitchStrip(int rightCount,
                                       float[] rightXs,
                                       short[] rightIndexes,
                                       int leftCount,
                                       float[] leftXs,
                                       float[] leftYs,
                                       short[] leftIndexes,
                                       float sweepDistance) {
    /*
      This algorithm is somewhat like a ball pivoting algorithm.
      However, it is really just for stitching.
      working in 2d. assume that you are standing on the +y axis, slightly
      above the origin, looking down the x axes. There are points regularly
      placed along the x axis ... on the right bank.
      The left bank is jagged, with points having +X and +Y coordinates.
      We want to fully triangulate these points, where one point always
      stays glued to the x axis ... the right bank.
      the sweepDistance is the maximum distance that you will consider
      when looking at points on the left bank.
      sweepDistance is calculated from your current anchor on the
      right bank.
    */
    return null;
    /*

    if (rightCount == 0 || leftCount == 0)
      return null;
    int rightAnchorIndex = 0;
    int rightCandidateIndex;
    int leftAnchorIndex = 0;
    int leftCandidateBase;
    while ((rightCandidateIndex = rightAnchorIndex + 1) < rightCount &&
           (leftCandidateBase = leftAnchorIndex + 1) < leftCount) {
      float rightAnchorX = rightXs[rightAnchorIndex];
      float candidateMaxX = rightAnchorX + sweepDistance;
      if (candidateMaxX > rightXs[rightCandidateIndex])
        candidateMaxX = rightXs[rightCandidateIndex];
      int leftCandidateMax;
      for (leftCandidateMax = leftCandidateBase;
           (leftCandidateMax < leftCount &&
            leftXs[leftCandidateMax] < candidateMaxX);
           ++leftCandidateMax)
        {}
      if (leftCandidateMax == leftCandidateBase) {
        System.out.println("abend 12345");
        return null;
      }
      float leftAnchorX = leftXs[leftAnchorIndex];
      float leftAnchorY = leftYs[leftAnchorIndex];
      int leftChampion = leftCandidateBase;
      float championAngle = 0;
      for (int challenger = leftCandidateBase; challenger < leftCandidateMax;
           ++challenger) {
        float
      int leftChampion = leftCandidateBase;
      int leftChampionAngle = calcTheAngle(rightAnchorX,
                                           leftAnchorX, leftAnchorY,
                                           leftXs[leftCandidateBase],
                                           leftYs[leftCandidateBase]);
      for (leftWinner=leftCandidateMax; --leftWinner > leftCandidateBase; ) {
        

             
           (leftAnchorIndex + leftCandidateCount < leftCount) &&
             leftXs[leftAnchorIndex + leftCandidate
    }
    */
  }

  ////////////////////////////////////////////////////////////////

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

  //  final Vector3f axisVector = new Vector3f();
  final Vector3f negativeAxisUnitVectorT = new Vector3f();
  final Vector3f unitRadialVectorT = new Vector3f();
  //  final Vector3f radialVector = new Vector3f();
  // 90 degrees, although everything is in radians
  final Vector3f radialVector90T = new Vector3f();

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

  final static int ALL_PROBE_BITS_ON = ~((1<<(32-INNER_TORUS_STEP_COUNT))-1);
  final static int FIRST_AND_LAST_BITS =
    (0x8000 | (1 << (32 - INNER_TORUS_STEP_COUNT)));
  final static int MAX_SEGMENT_COUNT = INNER_TORUS_STEP_COUNT / 2;

  final Point3f[] torusPoints = new Point3f[INNER_TORUS_STEP_COUNT *
                                            OUTER_TORUS_STEP_COUNT];
  {
    for (int i = torusPoints.length; --i >= 0; )
      torusPoints[i] = new Point3f();
  }

  final Point3f[] torusEdgePointsT = new Point3f[INNER_TORUS_STEP_COUNT + 1];
  {
    for (int i = torusEdgePointsT.length; --i >= 0; )
      torusEdgePointsT[i] = new Point3f();
  }

  Point3f[] convexEdgePoints;
  // what is the max size of this thing?
  short[] edgeVertexes;

  class Torus {
    final int ixA, ixB;
    final Point3f center;
    final float radius;
    final boolean fullTorus;

    final Vector3f radialVector = new Vector3f();
    final Vector3f axisUnitVector = new Vector3f();
    final Vector3f tangentVector = new Vector3f();
    final Vector3f outerRadial = new Vector3f();
    float outerAngle;
    short colixA, colixB;
    byte outerPointCount;
    byte segmentStripCount;
    short totalPointCount;
    short[] normixes;

    short[] connectAConvex;

    Torus(int indexA, int indexB, Point3f center, float radius,
          boolean fullTorus) {
      this.ixA = indexA;
      this.ixB = indexB;
      this.center = new Point3f(center);
      this.radius = radius;
      this.fullTorus = fullTorus;
    }

    void electReferenceCavity() {
      if (torusCavities == null)
        return;
      if (torusCavities[0].rightHanded)
        return;
      for (int i = torusCavityCount; --i > 0; ) {
        TorusCavity torusCavity = torusCavities[i];
        if (torusCavity.rightHanded) {
          torusCavities[i] = torusCavities[0];
          torusCavities[0] = torusCavity;
          break;
        }
      }
      if (! torusCavities[0].rightHanded)
        throw new NullPointerException();
    }

    void calcVectors() {
      Point3f centerA = frame.atoms[ixA].point3f;
      Point3f centerB = frame.atoms[ixB].point3f;
      axisUnitVector.sub(centerB, centerA);
      axisUnitVector.normalize();

      Point3f referenceProbePoint = null;
      if (torusCavities != null) {
        referenceProbePoint = torusCavities[0].cavity.probeCenter;
      } else {
        // it is a full torus, so it does not really matter where
        // we put it;
        if (axisUnitVector.x == 0)
          unitRadialVectorT.set(vectorX);
        else if (axisUnitVector.y == 0)
          unitRadialVectorT.set(vectorY);
        else if (axisUnitVector.z == 0)
          unitRadialVectorT.set(vectorZ);
        else {
          unitRadialVectorT.set(-axisUnitVector.y, axisUnitVector.x, 0);
          unitRadialVectorT.normalize();
        }
        referenceProbePoint = pointT;
        pointT.scaleAdd(radius, unitRadialVectorT, center);
      }

      radialVector.sub(referenceProbePoint, center);

      aaRotate.set(axisUnitVector, PI / 2);
      matrixT.set(aaRotate);
      matrixT.transform(radialVector, radialVector90T);

      tangentVector.cross(axisUnitVector, radialVector);
      tangentVector.normalize();

      outerRadial.sub(centerA, referenceProbePoint);
      outerRadial.normalize();
      outerRadial.scale(radiusP);

      vectorT.sub(centerB, referenceProbePoint);
      outerAngle = outerRadial.angle(vectorT);
    }

    void calcPointCounts() {
      int c = (int)(OUTER_TORUS_STEP_COUNT * outerAngle / Math.PI);
      c = (c + 1) & 0xFE;
      if (c > OUTER_TORUS_STEP_COUNT)
        c = OUTER_TORUS_STEP_COUNT;

      int t = 0;
      for (int i = torusSegmentCount; --i >= 0; )
        t += torusSegments[i].stepCount;
      //      System.out.println("segmentStripCount t=" + t);
      segmentStripCount = (byte)t;
      outerPointCount = (byte)c;
      totalPointCount = (short)(t * c);
      //      System.out.println("calcPointCounts: " +
      //                 " segmentStripCount=" + segmentStripCount +
      //                 " outerPointCount=" + outerPointCount +
      //                 " totalPointCount=" + totalPointCount);
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

    int torusCavityCount;
    TorusCavity[] torusCavities;

    void addCavity(Cavity cavity, boolean rightHanded) {
      if (torusCavities == null)
        torusCavities = new TorusCavity[4];
      else if (torusCavityCount == torusCavities.length)
        torusCavities = (TorusCavity[])Util.doubleLength(torusCavities);
      torusCavities[torusCavityCount] =
        new TorusCavity(cavity, rightHanded);
      ++torusCavityCount;
    }

    void checkCavityCorrectness0() {
      if (fullTorus ^ (torusCavityCount == 0))
        throw new NullPointerException();
    }

    void checkCavityCorrectness1() {
      if ((torusCavityCount & 1) != 0)
        throw new NullPointerException();
      int rightCount = 0;
      for (int i = torusCavityCount; --i >= 0; )
        if (torusCavities[i].rightHanded)
          ++rightCount;
      if (rightCount != torusCavityCount / 2)
        throw new NullPointerException();
    }

    void calcCavityAnglesAndSort() {
      if (torusCavities == null) // full torus
        return;
      // because of previous election, torusCavities[0] has angle 0;
      for (int i = torusCavityCount; --i > 0; )
        torusCavities[i].calcAngle(center, radialVector, radialVector90T);
      sortCavitiesByAngle();
    }

    void sortCavitiesByAngle() {
      // no need to sort entry #0, whose angle (by definition) is zero
      for (int i = torusCavityCount; --i >= 2; ) {
        TorusCavity champion = torusCavities[i];
        for (int j = i; --j > 0; ) {
          TorusCavity challenger = torusCavities[j];
          if (challenger.angle > champion.angle) {
            torusCavities[j] = champion;
            torusCavities[i] = champion = challenger;
          }
        }
      }
    }
      
    void checkCavityCorrectness2() {
      if (torusCavities == null)
        return; // full torus
      if ((torusCavityCount & 1) != 0) // ensure even number
        throw new NullPointerException();
      if (torusCavities[0].angle != 0)
        throw new NullPointerException();
      for (int i = torusCavityCount; --i > 0; ) {
        if (torusCavities[i].angle <= torusCavities[i-1].angle &&
            i != torusCavityCount - 1) {
          //System.out.println("oops! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
          //for (int j = 0; j < torusCavityCount; ++j) {
          //System.out.println("cavity:" + j + " " +
          //                   torusCavities[j].angle + " " +
          //                   torusCavities[j].rightHanded);
          //}
          throw new NullPointerException();
        }
        if (((i & 1) == 0) ^ torusCavities[i].rightHanded)
          throw new NullPointerException();
      }
    }

    void buildTorusSegments() {
      if (torusCavityCount == 0) {
        addTorusSegment(new TorusSegment(0, 2 * PI));
      } else {
        for (int i = 0; i < torusCavityCount; i += 2)
          addTorusSegment(new TorusSegment(torusCavities[i].angle,
                                           torusCavities[i+1].angle));
      }
    }

    int torusSegmentCount;
    TorusSegment[] torusSegments;

    void addTorusSegment(TorusSegment torusSegment) {
      if (torusSegments == null)
        torusSegments = new TorusSegment[4];
      if (torusSegmentCount == torusSegments.length)
        torusSegments = (TorusSegment[])Util.doubleLength(torusSegments);
      torusSegments[torusSegmentCount++] = torusSegment;
    }

    void calcNormixes() {
      transformOuterRadials();
      short[] normixes = this.normixes = new short[totalPointCount];
      int ix = 0;
      for (int i = 0; i < torusSegmentCount; ++i)
        ix = torusSegments[i].calcNormixes(normixes, ix);
    }
    
    void calcPoints(Point3f[] points) {
      //System.out.println("Sasurface1.Torus.calcPoints " +
      //                 " torusSegmentCount=" + torusSegmentCount);
      int indexStart = 0;
      transformOuterRadials();
      for (int i = 0; i < torusSegmentCount; ++i)
        indexStart = torusSegments[i].calcPoints(points, indexStart);
    }
    
    void calcScreens(Point3f[] points, Point3i[] screens) {
      for (int i = totalPointCount; --i >= 0; )
        viewer.transformPoint(points[i], screens[i]);
    }

    class TorusCavity {
      final Cavity cavity;
      final boolean rightHanded;
      float angle = 0;
      
      TorusCavity(Cavity cavity, boolean rightHanded) {
        this.cavity = cavity;
        this.rightHanded = rightHanded;
      }

      void calcAngle(Point3f center, Vector3f radialVector,
                     Vector3f radialVector90) {
        torusCavityAngleVector.sub(cavity.probeCenter, center);
        angle = torusCavityAngleVector.angle(radialVector);
        float angleCavity90 = torusCavityAngleVector.angle(radialVector90);
        if (angleCavity90 <= PI / 2)
          return;
        angle = (2 * PI) - angle;
      }
    }

    class TorusSegment {
      float startAngle;
      float stepAngle;
      int stepCount;
      short[] geodesicConnections;

      TorusSegment(float startAngle, float endAngle) {
        this.startAngle = startAngle;
        float totalSegmentAngle = endAngle - startAngle;
        //System.out.println(" startAngle=" + startAngle +
        //                   " endAngle=" + endAngle +
        //                   " totalSegmentAngle=" + totalSegmentAngle);
        if (totalSegmentAngle < 0)
          totalSegmentAngle += 2 * PI;
        stepCount = (int)(totalSegmentAngle / INNER_TORUS_STEP_ANGLE);
        stepAngle = totalSegmentAngle / stepCount;
        ++stepCount; // one more strip than pieces of the segment
      }

      int calcPoints(Point3f[] points, int ixPoint) {
        aaT.set(axisUnitVector, startAngle);
        for (int i = stepCount; --i >= 0; aaT.angle += stepAngle) {
          matrixT.set(aaT);
          matrixT.transform(radialVector, pointT);
          pointT.add(center);
          for (int j = 0; j < outerPointCount; ++j, ++ixPoint) {
            matrixT.transform(outerRadials[j], vectorT);
            points[ixPoint].add(pointT, vectorT);
            //System.out.println("  calcPoints[" + ixPoint + "]=" +
            //                 points[ixPoint]);
          }
        }
        return ixPoint;
      }

      int calcNormixes(short[] normixes, int ix) {
        aaT.set(axisUnitVector, startAngle);
        for (int i = stepCount; --i >= 0; aaT.angle += stepAngle) {
          matrixT.set(aaT);
          for (int j = 0; j < outerPointCount; ++j, ++ix) {
            matrixT.transform(outerRadials[j], vectorT);
            normixes[ix] = g3d.get2SidedNormix(vectorT);
          }
        }
        return ix;
      }

      void calcEdgePoints(Point3f[] edgePoints, boolean edgeA) {
        int outerRadialIndex;
        if (edgeA) {
          transformOuterRadials();
          outerRadialIndex = 0;
        } else {
          outerRadialIndex = outerPointCount - 1;
        }
        aaT.set(axisUnitVector, startAngle);
        for (int i = 0; i < stepCount; aaT.angle += stepAngle, ++i) {
          matrixT.set(aaT);
          matrixT.transform(radialVector, pointT);
          pointT.add(center);
          matrixT.transform(outerRadials[outerRadialIndex], vectorT);
          edgePoints[i].add(pointT, vectorT);
        }
      }

      void connectWithGeodesic(boolean edgeA) {
        int connectionIndex;
        if (edgeA) {
          geodesicConnections = new short[2 * stepCount];
          connectionIndex = 0;
        } else {
          connectionIndex = stepCount;
        }
        calcEdgePoints(torusEdgePointsT, edgeA);
        Point3f atomCenter = frame.atoms[edgeA ? ixA : ixB].point3f;
        for (int i = 0; i < stepCount; ++i) {
          Point3f edgePoint = torusEdgePointsT[i];
          vectorT.sub(edgePoint, atomCenter);
          short normix = g3d.getNormix(vectorT, GEODESIC_CALC_LEVEL);
          //System.out.println("connected!");
          geodesicConnections[connectionIndex++] = normix;
        }
      }
    }

    void clipVertexMaps() {
      Atom atomA = frame.atoms[ixA];
      Atom atomB = frame.atoms[ixB];
      negativeAxisUnitVectorT.scale(-1, axisUnitVector);
      calcZeroPoint(true, zeroPointT);
      clipGeodesic(atomA.point3f, atomA.getVanderwaalsRadiusFloat(),
                   zeroPointT, negativeAxisUnitVectorT,
                   convexVertexMaps[ixA]);

      calcZeroPoint(false, zeroPointT);
      clipGeodesic(atomB.point3f, atomB.getVanderwaalsRadiusFloat(),
                   zeroPointT, axisUnitVector, 
                   convexVertexMaps[ixB]);
    }

    void calcZeroPoint(boolean edgeA, Point3f zeroPoint) {
      Vector3f t;
      if (edgeA) {
        t = outerRadial;
      } else {
        aaT1.set(tangentVector, outerAngle);
        matrixT1.set(aaT1);
        matrixT1.transform(outerRadial, vectorT1);
        t = vectorT1;
      }
      zeroPoint.add(center, radialVector);
      zeroPoint.add(t);
    }

    void calcClippingPlaneCenterPoints(Point3f centerPointA,
                                       Point3f centerPointB) {
      calcZeroPoint(true, zeroPointT);
      Point3f centerA = frame.atoms[ixA].point3f;
      calcClippingPlaneCenter(centerA, axisUnitVector, zeroPointT,
                              centerPointA);

      negativeAxisUnitVectorT.scale(-1, axisUnitVector);
      calcZeroPoint(false, zeroPointT);
      Point3f centerB = frame.atoms[ixB].point3f;
      calcClippingPlaneCenter(centerB, negativeAxisUnitVectorT, zeroPointT,
                              centerPointB);
    }
    
    void connectWithGeodesics() {
      transformOuterRadials();
      for (int i = 0; i < torusSegmentCount; ++i)
        torusSegments[i].connectWithGeodesic(true);
    }
  }

  void allocateConvexVertexBitmap(int atomIndex) {
    if (convexVertexMaps[atomIndex] == null)
      convexVertexMaps[atomIndex] =
        Bmp.allocateSetAllBits(geodesicVertexCount);
  }

  Torus createTorus(int indexI, int indexJ, Point3f torusCenterIJ,
                    float torusRadius, boolean fullTorus) {
    if (indexI >= indexJ)
      throw new NullPointerException();
    if (htToruses.get(indexI, indexJ) != null)
      throw new NullPointerException();
    allocateConvexVertexBitmap(indexI);
    allocateConvexVertexBitmap(indexJ);
    Torus torus = new Torus(indexI, indexJ, torusCenterIJ,
                            torusRadius, fullTorus);
    htToruses.put(indexI, indexJ, torus);
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
    return (Torus)htToruses.get(atomIndexA, atomIndexB);
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
      if (distanceIJ < 0.2)
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
    }
    // check for an isolated atom with no neighbors
    if (neighborCount == 0)
      allocateConvexVertexBitmap(indexI);
  }

  // check for a full torus with no cavities between I & J
  void checkFullTorusIJ() {
    if (getTorus(indexI, indexJ) == null) {
      if (vectorIJ.z == 0)
        unitRadialVectorT.set(vectorZ);
      else {
        unitRadialVectorT.set(-vectorIJ.y, vectorIJ.x, 0);
        unitRadialVectorT.normalize();
      }
      float torusRadiusIJ = calcTorusRadius(radiusI, radiusJ, distanceIJ2);
      pointT.scaleAdd(torusRadiusIJ, unitRadialVectorT, torusCenterIJ);
      if (checkProbeNotIJ(pointT))
        createTorus(indexI, indexJ, torusCenterIJ, torusRadiusIJ, true);
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
          torusIJ = createTorus(indexI, indexJ, torusCenterIJ,
                                calcTorusRadius(radiusI, radiusJ, distanceIJ2),
                                false);
        torusIJ.addCavity(cavity, rightHanded);
        
        if (torusIK == null && (torusIK = getTorus(indexI, indexK)) == null)
          torusIK = createTorus(indexI, indexK, torusCenterIK,
                                calcTorusRadius(radiusI, radiusK, distanceIK2),
                                false);
        torusIK.addCavity(cavity, !rightHanded);

        if (torusJK == null && (torusJK = getTorus(indexJ, indexK)) == null) {
          calcTorusCenter(centerJ, radiiJP2, centerK, radiiKP2, distanceJK2,
                          torusCenterJK);
          torusJK = createTorus(indexJ, indexK, torusCenterJK,
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

    final Point3f probeCenter;
    final Point3f pointBottom = new Point3f();
    final short normixBottom;
    
    // probeCenter is the center of the probe
    // probeBase is the midpoint between this cavity
    // and its mirror image on the other side
    Cavity(Point3f probeCenter, Point3f probeBase) {
      this.probeCenter = new Point3f(probeCenter);

      vectorPI.sub(centerI, probeCenter);
      vectorPI.normalize();
      vectorPI.scale(radiusP);

      vectorPJ.sub(centerJ, probeCenter);
      vectorPJ.normalize();
      vectorPJ.scale(radiusP);

      vectorPK.sub(centerK, probeCenter);
      vectorPK.normalize();
      vectorPK.scale(radiusP);

      // the bottomPoint;

      vectorT.add(vectorPI, vectorPJ);
      vectorT.add(vectorPK);
      vectorT.normalize();
      pointBottom.scaleAdd(radiusP, vectorT, probeCenter);

      normixBottom = g3d.getInverseNormix(vectorT);
    }
  }

}

