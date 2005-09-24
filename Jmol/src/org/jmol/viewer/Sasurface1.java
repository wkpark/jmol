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

  private int GEODESIC_CALC_LEVEL = Sasurface.MAX_GEODESIC_RENDERING_LEVEL;
  private int MAX_FULL_TORUS_STEP_COUNT = Sasurface.MAX_FULL_TORUS_STEP_COUNT;
  private int OUTER_TORUS_STEP_COUNT = Sasurface.OUTER_TORUS_STEP_COUNT;

  private float TARGET_INNER_TORUS_STEP_ANGLE =
    (float)(2 * Math.PI / (MAX_FULL_TORUS_STEP_COUNT - 1));

  int geodesicRenderingLevel = GEODESIC_CALC_LEVEL;

  int surfaceConvexMax; // the Max == the highest atomIndex with surface + 1
  int[][] convexVertexMaps;
  int[][] convexFaceMaps;
  short[] colixesConvex;
  int geodesicVertexCount;

  int cavityCount;
  SasCavity[] cavities;
  int torusCount;
  Torus[] toruses;

  IntInt2ObjHash htToruses;

  final int[] edgeVertexesT;

  SasGem gem;
  SasNeighborFinder neighborFinder;

  private final static boolean LOG = false;

  private final Point3f pointT = new Point3f();
  private final Point3f pointT1 = new Point3f();
  private final Point3f zeroPointT = new Point3f();
  private final Point3f centerPointT = new Point3f();
  private final Point3f centerPointAT = new Point3f();
  private final Point3f centerPointBT = new Point3f();

  private final static float PI = (float)Math.PI;

  final Vector3f torusCavityAngleVector = new Vector3f();

  Sasurface1(String surfaceID, Viewer viewer, Graphics3D g3d, short colix,
             BitSet bs) {
    this.surfaceID = surfaceID;
    this.viewer = viewer;
    this.g3d = g3d;
    this.colix = colix;

    frame = viewer.getFrame();
    gem = new SasGem(viewer, g3d, frame, GEODESIC_CALC_LEVEL);
    neighborFinder = new SasNeighborFinder(frame, this, g3d);
    geodesicVertexCount = g3d.getGeodesicVertexCount(GEODESIC_CALC_LEVEL);
    edgeVertexesT = Bmp.allocateBitmap(geodesicVertexCount);
    generate(bs);
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
    neighborFinder.setProbeRadius(radiusP);
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
        neighborFinder.findAbuttingNeighbors(i, bsSelected);
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

    }

    for (int i = torusCount; --i >= 0; ) {
      Torus torus = toruses[i];
      torus.stitchWithGeodesics();
    }

    for (int i = atomCount; --i >= 0; ) {
      int[] vertexMap = convexVertexMaps[i];
      if (vertexMap != null)
        convexFaceMaps[i] = gem.calcFaceBitmap(vertexMap);
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
          torus.colixA =
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[torus.ixA], (String)value);
        if (bs.get(torus.ixB))
          torus.colixB =
            (colix != Graphics3D.UNRECOGNIZED)
            ? colix
            : viewer.getColixAtomPalette(atoms[torus.ixB], (String)value);
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

  void calcVectors0and90(Point3f planeCenter, Vector3f axisVector,
                         Point3f planeZeroPoint,
                         Vector3f vector0, Vector3f vector90) {
    vector0.sub(planeZeroPoint, planeCenter);
    aaT.set(axisVector, PI / 2);
    matrixT.set(aaT);
    matrixT.transform(vector0, vector90);
  }

  ////////////////////////////////////////////////////////////////

  private final Vector3f centerVectorT = new Vector3f();
  private final Point3f vertexPointT = new Point3f();
  private final Vector3f vertexVectorT = new Vector3f();

  private int[] bmpNotClipped;

  private int countStitchesT;
  private short[] stitchesT = new short[64];
  private float[] segmentVertexAnglesT = new float[MAX_FULL_TORUS_STEP_COUNT];
  private short[] segmentVertexesT = new short[MAX_FULL_TORUS_STEP_COUNT];

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
      angle = 2*PI - angle;
    return angle;
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

  final Vector3f unitRadialVectorT = new Vector3f();
  // 90 degrees, although everything is in radians
  final Vector3f radialVector90T = new Vector3f();

  Point3f pointAtomI;
  Point3f pointAtomJ;

  final Vector3f vectorIP = new Vector3f();
  final Vector3f vectorJP = new Vector3f();

  float[] cavityAngles = new float[32];

  final Vector3f outerRadials[] = new Vector3f[OUTER_TORUS_STEP_COUNT];
  {
    for (int i = outerRadials.length; --i >= 0; )
      outerRadials[i] = new Vector3f();
  }

  Point3f[] convexEdgePoints;
  // what is the max size of this thing?
  short[] edgeVertexes;

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

  void addCavity(int indexI, int indexJ, int indexK, SasCavity cavity) {
    if (cavities == null)
      cavities = new SasCavity[32];
    else if (cavityCount == cavities.length)
      cavities = (SasCavity[])Util.doubleLength(cavities);
    cavities[cavityCount++] = cavity;

    allocateConvexVertexBitmap(indexI);
    allocateConvexVertexBitmap(indexJ);
    allocateConvexVertexBitmap(indexK);
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

    short[] geodesicStitchesA;
    short[] geodesicStitchesB;
    short[] seamA;
    short[] seamB;

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

      calcVectors0and90(center, axisUnitVector, referenceProbePoint,
                        radialVector, radialVector90T);

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
      else if (c == 0)
        c = 2;

      int t = 0;
      for (int i = torusSegmentCount; --i >= 0; )
        t += torusSegments[i].stepCount;
      //      System.out.println("segmentStripCount t=" + t);
      segmentStripCount = (byte)t;
      outerPointCount = (byte)c;
      totalPointCount = (short)(t * c);
      if (totalPointCount == 0) {
        System.out.println("?Que? why is this a torus?");
        System.out.println("calcPointCounts: " +
                           " outerAngle=" + outerAngle +
                           " segmentStripCount=" + segmentStripCount +
                           " outerPointCount=" + outerPointCount +
                           " totalPointCount=" + totalPointCount);
        for (int i = 0; i < torusSegmentCount; ++i) {
          TorusSegment ts = torusSegments[i];
          System.out.println("  torusSegment[" + i + "] : " +
                             " .startAngle=" + ts.startAngle +
                             " .stepAngle=" + ts.stepAngle +
                             " .stepCount=" + ts.stepCount);
                             
        }
        throw new NullPointerException();
      }
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

    void addCavity(SasCavity cavity, boolean rightHanded) {
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
        addTorusSegment(new TorusSegment());
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

    class TorusSegment {
      float startAngle;
      float stepAngle;
      int stepCount; // # of vertexes, which is 1 more than the # of strips

      TorusSegment() { // for a full torus
        this.startAngle = 0;
        this.stepAngle = TARGET_INNER_TORUS_STEP_ANGLE;
        this.stepCount = MAX_FULL_TORUS_STEP_COUNT;
        /*
        System.out.println("FullTorus\n" +
                           " startAngle=" + startAngle +
                           " stepAngle=" + stepAngle +
                           " stepCount=" + stepCount +
                           " totalSegmentAngle=" + (stepAngle*(stepCount-1)));
        */
      }
      
      TorusSegment(float startAngle, float endAngle) {
        this.startAngle = startAngle;
        float totalSegmentAngle = endAngle - startAngle;
        /*
        System.out.println(" startAngle=" + startAngle +
                           " endAngle=" + endAngle +
                           " totalSegmentAngle=" + totalSegmentAngle);
        */
        if (totalSegmentAngle < 0)
          totalSegmentAngle += 2 * PI;
        stepCount = (int)(totalSegmentAngle / TARGET_INNER_TORUS_STEP_ANGLE);
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

      void stitchWithSortedProjectedVertexes(int projectedCount,
                                             short[] projectedVertexes,
                                             float[] projectedAngles,
                                             float[] projectedDistances,
                                             boolean isEdgeA) {
        int minProjectedIndex = 0;
        while (minProjectedIndex < projectedCount &&
               projectedAngles[minProjectedIndex] < startAngle)
          ++minProjectedIndex;
        float endAngle = startAngle + (stepAngle * stepCount);
        int maxProjectedIndex = minProjectedIndex;
        while (maxProjectedIndex < projectedCount &&
               projectedAngles[maxProjectedIndex] <= endAngle)
          ++maxProjectedIndex;
        int vertexCount = maxProjectedIndex - minProjectedIndex;
        if (vertexCount == 0) {
          System.out.println("no vertexes for this torus segment");
          return;
        }
        /*
        System.out.println(" startAngle=" + startAngle +
                           " endAngle=" + endAngle +
                           " minProjectedIndex=" + minProjectedIndex +
                           " minAngle=" + projectedAngles[minProjectedIndex] +
                           " maxProjectedIndex=" + maxProjectedIndex +
                           " lastAngle=" +
                           projectedAngles[maxProjectedIndex - 1]);
        */
        fillSegmentVertexAngles(isEdgeA);
        stitchEm(stepCount, segmentVertexesT, segmentVertexAnglesT,
                 minProjectedIndex, maxProjectedIndex,
                 projectedVertexes,
                 projectedAngles, projectedDistances);
      }

      void stitchEm(int torusCount, short[] torusVertexes, float[] torusAngles,
                    int geodesicMin, int geodesicMax,
                    short[] geodesicVertexes, float[] geodesicAngles,
                    float[] geodesicDistances) {
        if (geodesicMin == geodesicMax)
          return;
        int tLast = torusCount - 1;
        int gLast = geodesicMax - 1;
        oneStitch(torusVertexes[0], geodesicVertexes[geodesicMin]);
        int t = 0;
        int g = geodesicMin;
        while (t < tLast && g < gLast) {
          float angleT = angleABC(torusAngles[t], 0,
                                  torusAngles[t + 1], 0,
                                  geodesicAngles[g], geodesicDistances[g]);
          float angleG = angleABC(torusAngles[t], 0,
                                  geodesicAngles[g+1], geodesicDistances[g+1],
                                  geodesicAngles[g], geodesicDistances[g]);
          if (angleT > angleG)
            ++t;
          else
            ++g;
          oneStitch(torusVertexes[t], geodesicVertexes[g]);
        }
        while (t < tLast || g < gLast) {
          if (t < tLast)
            ++t;
          else
            ++g;
          oneStitch(torusVertexes[t], geodesicVertexes[g]);
        }
      }

      void oneStitch(short torusVertex, short geodesicVertex) {
        if (countStitchesT + 1 >= stitchesT.length)
          stitchesT = Util.doubleLength(stitchesT);
        stitchesT[countStitchesT] = torusVertex;
        stitchesT[countStitchesT + 1] = geodesicVertex;
        countStitchesT += 2;
      }

      void fillSegmentVertexAngles(boolean isEdgeA) {
        short segmentVertex = getSegmentStartingVertex(isEdgeA);
        float angle = startAngle;
        for (int i = 0; i < stepCount; ++i) {
          segmentVertexesT[i] = segmentVertex;
          segmentVertex += outerPointCount;
          segmentVertexAnglesT[i] = angle;
          angle += stepAngle;
        }
      }
      
      short getSegmentStartingVertex(boolean isEdgeA) {
        int totalStepCount = 0;
        for (int i = 0; i < torusSegmentCount; ++i) {
          TorusSegment segment = torusSegments[i];
          if (segment == this) {
            int startingVertex = totalStepCount * outerPointCount;
            if (! isEdgeA) {
              /*
              System.out.println(" -------- I am not edge A! ---------");
              System.out.println("   startingVertex=" + startingVertex +
                                 "   outerPointCount=" + outerPointCount);
              */
              startingVertex += outerPointCount - 1;
              /*
              System.out.println("   after startingVertex=" + startingVertex);
              */
            }
            return (short)startingVertex;
          }
          totalStepCount += segment.stepCount;
        }
        System.out.println("torus segment not found in torus");
        throw new NullPointerException();
      }
    }

    void clipVertexMaps() {
      clipVertexMap(true);
      clipVertexMap(false);
    }

    void clipVertexMap(boolean isEdgeA) {
      int ix = isEdgeA ? ixA : ixB;
      Atom atom = frame.atoms[ix];
      calcZeroPoint(isEdgeA, zeroPointT);
      gem.clipGeodesic(isEdgeA, atom.point3f, atom.getVanderwaalsRadiusFloat(),
                       zeroPointT, axisUnitVector, convexVertexMaps[ix]);
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

    void calcZeroAndCenterPoints(boolean edgeA, Point3f atomCenter,
                                 Point3f zeroPoint, Point3f centerPoint) {
      calcZeroPoint(edgeA, zeroPoint);
      calcClippingPlaneCenter(atomCenter, axisUnitVector, zeroPoint,
                              centerPoint);
    }

    void calcClippingPlaneCenterPoints(Point3f centerPointA,
                                       Point3f centerPointB) {
      calcZeroPoint(true, zeroPointT);
      Point3f centerA = frame.atoms[ixA].point3f;
      calcClippingPlaneCenter(centerA, axisUnitVector, zeroPointT,
                              centerPointA);

      calcZeroPoint(false, zeroPointT);
      Point3f centerB = frame.atoms[ixB].point3f;
      calcClippingPlaneCenter(centerB, axisUnitVector, zeroPointT,
                              centerPointB);
    }

    void stitchWithGeodesics() {
      //      System.out.println("torus.stitchWithGeodesics()");
      stitchWithGeodesic(true);
      stitchWithGeodesic(false);
    }

    void stitchWithGeodesic(boolean isEdgeA) {
      int ix = isEdgeA ? ixA : ixB;
      Atom atom = frame.atoms[ix];
      float atomRadius = atom.getVanderwaalsRadiusFloat();
      Point3f atomCenter = atom.point3f;
      boolean dump = (ixA == 0 && (ixB == 1 || ixB == 3));
      int edgeCount =
        gem.findGeodesicEdge(convexVertexMaps[ix], edgeVertexesT);
      if (edgeCount > 0) {
        calcZeroAndCenterPoints(isEdgeA, atomCenter, zeroPointT, centerPointT);
        gem.projectAndSortGeodesicPoints(isEdgeA,
                                         atomCenter, atomRadius,
                                         centerPointT, axisUnitVector,
                                         zeroPointT, (torusCavities == null),
                                         edgeVertexesT, dump);
        stitchSegmentsWithSortedProjectedVertexes(gem.projectedCount,
                                                  gem.projectedVertexes,
                                                  gem.projectedAngles,
                                                  gem.projectedDistances,
                                                  isEdgeA);
      }
    }

    void stitchSegmentsWithSortedProjectedVertexes(int projectedCount,
                                                   short[] projectedVertexes,
                                                   float[] projectedAngles,
                                                   float[] projectedDistances,
                                                   boolean isEdgeA) {
      countStitchesT = 0;
      for (int i = torusSegmentCount; --i >= 0; )
        torusSegments[i].stitchWithSortedProjectedVertexes(projectedCount,
                                                           projectedVertexes,
                                                           projectedAngles,
                                                           projectedDistances,
                                                           isEdgeA);
      short[] geodesicStitches = new short[countStitchesT];
      for (int i = countStitchesT; --i >= 0; )
        geodesicStitches[i] = stitchesT[i];
      if (isEdgeA)
        geodesicStitchesA = geodesicStitches;
      else
        geodesicStitchesB = geodesicStitches;
      short[] seam = createSeam(countStitchesT, stitchesT);
      if (isEdgeA)
        seamA = seam;
      else
        seamB = seam;
    }

    void findClippedEdgeVertexes(int[] edgeVertexesA, int[] edgeVertexesB) {
      calcClippingPlaneCenterPoints(centerPointAT, centerPointBT);
      Atom atomA = frame.atoms[ixA];
      Atom atomB = frame.atoms[ixB];
      gem.findClippedGeodesicEdge(true, atomA.point3f,
                                  atomA.getVanderwaalsRadiusFloat(),
                                  centerPointAT, axisUnitVector,
                                  convexVertexMaps[ixA],
                                  edgeVertexesA);
      gem.findClippedGeodesicEdge(false, atomB.point3f,
                                  atomB.getVanderwaalsRadiusFloat(),
                                  centerPointBT, axisUnitVector,
                                  convexVertexMaps[ixA],
                                  edgeVertexesB);
    }

    void findEdgeVertexes(int[] edgeVertexesA, int[] edgeVertexesB) {
      gem.findGeodesicEdge(convexVertexMaps[ixA], edgeVertexesA);
      gem.findGeodesicEdge(convexVertexMaps[ixB], edgeVertexesB);
    }
  }


  class TorusCavity {
    final SasCavity cavity;
    final boolean rightHanded;
    float angle = 0;
    
    TorusCavity(SasCavity cavity, boolean rightHanded) {
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
}

