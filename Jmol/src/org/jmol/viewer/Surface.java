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

  private final static int GEODESIC_CALC_LEVEL = 3;
  int geodesicRenderingLevel = 3;

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

  private final static byte[] torusStepCounts = {20, 40, 60, 80};

  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();

  final static int segmentsPerFullCircle = 50;
  final static float radiansPerSegment =
    2*(float)Math.PI/segmentsPerFullCircle;

  final Point3f[] pointStripT = new Point3f[segmentsPerFullCircle];
  final Vector3f stripSurfaceVector = new Vector3f();
  final Vector3f outerSurfaceVector = new Vector3f();
  final Point3f outerCenterPoint = new Point3f();
  final Point3f outerSurfacePoint = new Point3f();
    
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
      htToruses = null;
      toruses = null;
      cavityCount = 0;
      cavities = null;
      radiusP = viewer.getCurrentSolventProbeRadius();
      diameterP = 2 * radiusP;
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

    // now, calculate surface for selected atoms
    if (mad != 0) {
      long timeBegin = System.currentTimeMillis();
      if (radiusP > 0 && htToruses == null)
        htToruses = new Hashtable();
      for (int i = 0; i < atomCount; ++i) // make this loop count up
        if (bsSelected.get(i)) {
          setAtomI(i);
          getNeighbors(bsSelected);
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
    //    System.out.println("setAtomI:" + indexI);
    this.indexI = indexI;
    atomI = frame.atoms[indexI];
    centerI = atomI.point3f;
    radiusI = atomI.getVanderwaalsRadiusFloat();
    radiiIP = radiusI + radiusP;
    radiiIP2 = radiiIP * radiiIP;
  }

  void setNeighborJ(int indexNeighbor) {
    indexJ = neighborIndexes[indexNeighbor];
    //    System.out.println(" setNeighborJ:" + indexJ);
    atomJ = neighbors[indexNeighbor];
    radiusJ = atomJ.getVanderwaalsRadiusFloat();
    radiiJP = neighborPlusProbeRadii[indexNeighbor];
    radiiJP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerJ = neighborCenters[indexNeighbor];
    distanceIJ2 = centerJ.distanceSquared(centerI);
    distanceIJ = (float)Math.sqrt(distanceIJ2);
  }

  void setNeighborK(int indexNeighbor) {
    indexK = neighborIndexes[indexNeighbor];
    //    System.out.println("  setNeighborK:" + indexK);
    atomK = neighbors[indexNeighbor];
    radiusK = atomK.getVanderwaalsRadiusFloat();
    radiiKP = neighborPlusProbeRadii[indexNeighbor];
    radiiKP2 = neighborPlusProbeRadii2[indexNeighbor];
    centerK = neighborCenters[indexNeighbor];
    distanceIK2 = centerK.distanceSquared(centerI);
    distanceIK = (float)Math.sqrt(distanceIK2);
    distanceJK2 = centerK.distanceSquared(centerJ);
    distanceJK = (float)Math.sqrt(distanceJK2);
  }

  void calcVertexBitmapI() {
    Bmp.setAllBits(tempVertexMap, geodesicVertexCount);
    if (atomI.getAtomNumber() == 131)
      System.out.println("I see it!");
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
    if (atomI.getAtomNumber() == 131) {
      System.out.println("Bmp.countBits(tempVertexMap)=" +
                         Bmp.countBits(tempVertexMap) +
                         "\nBmp.countBits(convexVertexMaps[indexI])=" +
                         Bmp.countBits(convexVertexMaps[indexI]));
      System.out.println("tempVertexMap.length=" + tempVertexMap.length +
                         "\nconvexVertexMaps[indexI].length=" +
                         convexVertexMaps[indexI].length);
    }
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
  Atom[] neighbors = new Atom[16];
  int[] neighborIndexes = new int[16];
  Point3f[] neighborCenters = new Point3f[16];
  float[] neighborPlusProbeRadii = new float[16];
  float[] neighborPlusProbeRadii2 = new float[16];
  
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
      if (neighborCount == neighbors.length) {
        neighbors = (Atom[])Util.doubleLength(neighbors);
        neighborIndexes = Util.doubleLength(neighborIndexes);
        neighborCenters = (Point3f[])Util.doubleLength(neighborCenters);
        neighborPlusProbeRadii = Util.doubleLength(neighborPlusProbeRadii);
        neighborPlusProbeRadii2 = Util.doubleLength(neighborPlusProbeRadii2);
      }
      neighbors[neighborCount] = neighbor;
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
      me.distance(neighbors[i].getPoint3f()));
      }
    */
  }

  void deleteUnusedToruses() {
    boolean torusDeleted = false;
    for (int i = torusCount; --i >= 0; ) {
      Torus torus = toruses[i];
      if (convexVertexMaps[torus.ixI] == null &&
          convexVertexMaps[torus.ixJ] == null) {
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

  final Vector3f vOrganizeA = new Vector3f();
  final Vector3f vOrganizeB = new Vector3f();

  final Vector3f axisVector = new Vector3f();
  final Vector3f unitRadialVector = new Vector3f();
  final Vector3f radialVector = new Vector3f();
  final Point3f pointTorusP = new Point3f();
  final Vector3f vectorTorusP = new Vector3f();
  final AxisAngle4f aaRotate = new AxisAngle4f();

  final Vector3f vectorIP = new Vector3f();
  final Vector3f vectorJP = new Vector3f();

  class Torus {
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
    short[] connections;
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

    Torus(int indexA, Point3f centerA, int indexB, Point3f centerB, 
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

    void addConnection(short vertexI, short vertexJ) {
      if (connections == null)
        connections = new short[32];
      for (int i = connectionCount; (i -= 2) >= 0; )
        if (connections[i] == vertexI &&
            connections[i+1] == vertexJ)
          return;
      if (connectionCount == connections.length)
        connections = Util.doubleLength(connections);
      connections[connectionCount++] = vertexI;
      connections[connectionCount++] = vertexJ;
      Bmp.setBit(convexVertexMaps[ixI], vertexI);
      Bmp.setBit(convexVertexMaps[ixJ], vertexJ);
    }

    void addCavity(Cavity cavity, boolean rightHanded) {
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

    void connect() {
      if (indexI != ixI)
        throw new NullPointerException();
      if (indexJ != ixJ)
        throw new NullPointerException();
      System.out.println("connect " + ixI + ":" + ixJ);

      Point3f pointI = frame.atoms[ixI].point3f;
      Point3f pointJ = frame.atoms[ixJ].point3f;
      axisVector.sub(pointJ, pointI);
                     
      if (axisVector.z == 0)
        unitRadialVector.set(vectorZ);
      else {
        unitRadialVector.set(-axisVector.y, axisVector.x, 0);
        unitRadialVector.normalize();
      }
      radialVector.scale(radius, unitRadialVector);
      
      aaRotate.set(axisVector, 0);

      int numSteps = torusStepCounts[geodesicRenderingLevel];
      pointspCount = 0;
      pointsp = new Point3f[numSteps];
      vertexesIP = new short[numSteps];
      vertexesJP = new short[numSteps];
      float stepRadians = 2 * (float)Math.PI / numSteps;
      for (int i = 0; i < numSteps; ++i) {
        aaRotate.angle = i * stepRadians;
        matrixT.set(aaRotate);
        matrixT.transform(radialVector, vectorTorusP);
        pointTorusP.add(center, vectorTorusP);
        if (! checkProbeNotIJ(pointTorusP))
          continue;
        pointsp[pointspCount] = new Point3f(pointTorusP);
        vectorIP.sub(pointTorusP, pointI);
        vectorJP.sub(pointTorusP, pointJ);
        short vertexIP = g3d.getNormix(vectorIP, geodesicRenderingLevel);
        vertexesIP[pointspCount] = vertexIP;
        Bmp.setBit(convexVertexMaps[ixI], vertexIP);
        short vertexJP = g3d.getNormix(vectorJP, geodesicRenderingLevel);
        vertexesJP[pointspCount] = vertexJP;
        Bmp.setBit(convexVertexMaps[ixJ], vertexJP);
        pointspCount++;
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
      if (neighborIndexes[iJ] <= indexI)
        continue;
      setNeighborJ(iJ);
      // deal with corrupt files that have duplicate atoms
      if (distanceIJ < 0.1)
        continue;
      vectorIJ.sub(centerJ, centerI);
      calcTorusCenter(centerI, radiiIP2, centerJ, radiiJP2, distanceIJ2,
                      torusCenterIJ);
      for (int iK = neighborCount; --iK >= 0; ) {
        if (neighborIndexes[iK] <= indexJ)
          continue;
        setNeighborK(iK);
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
        torusIJ.connect();
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
    //    System.out.println("getCavitiesIJK:" + indexI + "," + indexJ + "," +
    //                       indexK);
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
        Cavity cavity = new Cavity(cavityProbe);
        addCavity(cavity);
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

  class Cavity {
    final int ixI, ixJ, ixK;

    final Point3f pointPI = new Point3f();
    final Point3f pointPJ = new Point3f();
    final Point3f pointPK = new Point3f();
    final short vertexI, vertexJ, vertexK;
    short colixI, colixJ, colixK;

    Cavity(Point3f probeCenter) {
      ixI = indexI; ixJ = indexJ; ixK = indexK;

      vectorPI.sub(centerI, probeCenter);
      vectorPI.normalize();
      pointPI.scaleAdd(radiusP, vectorPI, probeCenter);

      vectorPJ.sub(centerJ, probeCenter);
      vectorPJ.normalize();
      pointPJ.scaleAdd(radiusP, vectorPJ, probeCenter);

      vectorPK.sub(centerK, probeCenter);
      vectorPK.normalize();
      pointPK.scaleAdd(radiusP, vectorPK, probeCenter);

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
