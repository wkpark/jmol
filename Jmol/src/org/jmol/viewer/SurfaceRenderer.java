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


class SurfaceRenderer extends ShapeRenderer {

  boolean perspectiveDepth;
  boolean hideSaddles;
  boolean hideCavities;
  boolean hideConvex;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;
  int geodesicVertexCount;
  int geodesicFaceCount;
  short[] geodesicFaceVertexes;
  short[] geodesicFaceNormixes;
  ScreensCache screensCache;
  float radiusP;

  Vector3f[] transformedProbeVertexes;
  Point3i[] probeScreens;

  void initRenderer() {
    screensCache = new ScreensCache(viewer, 6);
  }

  void render() {
    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int)viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();

    Surface surface = (Surface)shape;
    if (surface == null)
      return;
    hideSaddles = viewer.getTestFlag1();
    hideCavities = viewer.getTestFlag2();
    hideConvex = viewer.getTestFlag3();
    int renderingLevel = surface.geodesicRenderingLevel;
    radiusP = surface.radiusP;
    geodesicVertexCount = surface.geodesicVertexCount;
    screensCache.alloc(geodesicVertexCount);
    geodesicFaceCount =
      g3d.getGeodesicFaceCount(renderingLevel);
    geodesicFaceVertexes =
      g3d.getGeodesicFaceVertexes(renderingLevel);
    geodesicFaceNormixes =
      g3d.getGeodesicFaceNormixes(renderingLevel);
    if (transformedProbeVertexes == null ||
        transformedProbeVertexes.length < geodesicVertexCount)
      allocTransformedProbeVertexes();
    Atom[] atoms = frame.atoms;
    int[][] convexVertexMaps = surface.convexVertexMaps;
    int[][] convexFaceMaps = surface.convexFaceMaps;
    short[] colixesConvex = surface.colixesConvex;
    int displayModelIndex = this.displayModelIndex;
    for (int i = surface.surfaceConvexMax; --i >= 0; ) {
      int[] vertexMap = convexVertexMaps[i];
      if (vertexMap != null) {
        int[] faceMap = convexFaceMaps[i];
        Atom atom = atoms[i];
        if (displayModelIndex < 0 || displayModelIndex == atom.modelIndex)
          renderConvex(atom, colixesConvex[i], vertexMap, faceMap);
      }
    }
    Surface.Torus[] toruses = surface.toruses;
    for (int i = surface.torusCount; --i >= 0; ) {
      Surface.Torus torus = toruses[i];
      /*
      renderToruX(torus,
                  atoms[ixI], convexVertexMaps[ixI],
                  atoms[ixJ], convexVertexMaps[ixJ],
                  colixesConvex,
                  renderingLevel);
      */
      renderTorus(torus, atoms, colixesConvex, convexVertexMaps);
    }
    Surface.Cavity[] cavities = surface.cavities;
    for (int i = surface.cavityCount; --i >= 0; )
      renderCavity(cavities[i], atoms,
                   colixesConvex,
                   convexVertexMaps);
    screensCache.free();
  }
  
  void allocTransformedProbeVertexes() {
    transformedProbeVertexes = new Vector3f[geodesicVertexCount];
    probeScreens = new Point3i[geodesicVertexCount];
    Vector3f[] transformedGeodesicVertexes =
      viewer.g3d.getTransformedVertexVectors();
    for (int i = geodesicVertexCount; --i >= 0; ) {
      transformedProbeVertexes[i] = new Vector3f();
      transformedProbeVertexes[i].scale(radiusP,
                                        transformedGeodesicVertexes[i]);
      probeScreens[i] = new Point3i();
    }
  }

  private final static boolean CONVEX_DOTS = false;
  private final static boolean CAVITY_DOTS = true;

  void renderConvex(Atom atom, short colix, int[] vertexMap, int[] faceMap) {
    if (hideConvex)
      return;
    Point3i[] screens = screensCache.lookup(atom, vertexMap);
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    if (CONVEX_DOTS) {
      int[] edgeVertexes = ((Surface)shape).calcEdgeVertexes(vertexMap);
      for (int vertex = Bmp.getMaxMappedBit(vertexMap); --vertex >= 0; ) {
        if (Bmp.getBit(edgeVertexes, vertex)) {
          Point3i screen = screens[vertex];
          g3d.fillSphereCentered(colix, 6, screen);
        } else if (Bmp.getBit(vertexMap, vertex)) {
          Point3i screen = screens[vertex];
          g3d.fillSphereCentered(colix, 3, screen);
        }
      }
    }
    for (int i = Bmp.getMaxMappedBit(faceMap), j = 3*i - 1; --i >= 0; j -= 3) {
      if (Bmp.getBit(faceMap, i)) {
        short vA = geodesicFaceVertexes[j - 2];
        short vB = geodesicFaceVertexes[j - 1];
        short vC = geodesicFaceVertexes[j];
        g3d.fillTriangle(colix,
                         screens[vA], vA,
                         screens[vB], vB,
                         screens[vC], vC);
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // torus rendering
  void renderTorus(Surface.Torus torus,
                   Atom[] atoms, short[] colixes, int[][] convexVertexMaps) {
    if (convexVertexMaps[torus.ixA] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixA, colixes, atoms, torus.ixA),
                      false);
    if (convexVertexMaps[torus.ixB] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixB, colixes, atoms, torus.ixB),
                      true);
  }

  short getColix(short colix, short[] colixes, Atom[] atoms, int index) {
    return Graphics3D.inheritColix(colix, atoms[index].colixAtom);
  }

  int getTorusOuterDotCount() {
    return 32;
  }

  int getTorusIncrement() {
    return 1;
  }

  final AxisAngle4f aaT = new AxisAngle4f();
  final AxisAngle4f aaT1 = new AxisAngle4f();
  static final float INNER_TORUS_STEP_ANGLE = Surface.INNER_TORUS_STEP_ANGLE;
  final Matrix3f matrixT = new Matrix3f();
  final Matrix3f matrixT1 = new Matrix3f();
  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();

  final Point3f[][] torusPoints =
    new Point3f[Surface.INNER_TORUS_STEP_COUNT][];
  final Point3i[][] torusScreens =
    new Point3i[Surface.INNER_TORUS_STEP_COUNT][];
  {
    for (int i = torusPoints.length; --i >= 0; ) {
      Point3f[] outerPoints = new Point3f[Surface.OUTER_TORUS_STEP_COUNT];
      torusPoints[i] = outerPoints;
      Point3i[] outerScreens = new Point3i[Surface.OUTER_TORUS_STEP_COUNT];
      torusScreens[i] = outerScreens;
      for (int j = outerPoints.length; --j >= 0; ) {
        outerPoints[j] = new Point3f();
        outerScreens[j] = new Point3i();
      }
    }
  }

  void renderTorusHalf(Surface.Torus torus, short colix, boolean renderJHalf) {
    g3d.setColix(colix);
    torus.calcPoints(torusPoints, renderJHalf);
    torus.calcScreens(torusPoints, torusScreens);
    int outerPointCount = torus.outerPointCount;

    for (int i = 0, probeT = torus.probeMap; probeT != 0; ++i, probeT <<= 1) {
      if (probeT >= 0)
        continue;
      Point3i[] screens = torusScreens[i];
      for (int j = outerPointCount; --j >= 0; )
        g3d.drawPixel(screens[j]);
    }
  }

  ////////////////////////////////////////////////////////////////


  Vector3f vectorIJ = new Vector3f();
  Vector3f vectorJI = new Vector3f();

  boolean SHOW_TORUS_PROBE_CENTERS = false;
  boolean SHOW_TORUS_PROBE_CENTER_VERTEX_CONNECTIONS = false;
  boolean SHOW_TORUS_VERTEX_CONNECTIONS = false;

  boolean SHOW_TORUS_CAVITY_FOO = false;
  
  /*
  void renderToruX(Surface.Torus torus,
                   Atom atomI, int[] vertexMapI,
                   Atom atomJ, int[] vertexMapJ,
                   short[] colixesSaddle,
                   int renderingLevel) {
    if (hideSaddles)
      return;
    if (vertexMapI == null || vertexMapJ == null)
      return;
    short colixI = Graphics3D.inheritColix(colixesSaddle[torus.ixI],
                                           atomI.colixAtom);
    short colixJ = Graphics3D.inheritColix(colixesSaddle[torus.ixI],
                                           atomJ.colixAtom);

    Point3i[] screensI = screensCache.lookup(atomI, vertexMapI);
    Point3i[] screensJ = screensCache.lookup(atomJ, vertexMapJ);

    Point3i screenClosestI = screensI[torus.normixI];
    Point3i screenClosestJ = screensJ[torus.normixJ];

    if (SHOW_TORUS_CAVITY_FOO) {
      for (int i = torus.connectionCount; (i -= 2) >= 0; ) {
        short vertexA = torus.cavityConnections[i];
        short vertexB = torus.cavityConnections[i + 1];
        
        g3d.fillCylinder(Graphics3D.ORANGE, Graphics3D.ENDCAPS_FLAT, 4,
                         screensI[vertexA],
                         screensJ[vertexB]);
      }
    }

    for (int i = torus.pointspCount; --i >= 0; ) {
      Point3f torusPoint = torus.pointsp[i];
      if (torusPoint != null) {
        Point3i screenP = viewer.transformPoint(torusPoint);
        if (SHOW_TORUS_PROBE_CENTERS)
          g3d.fillSphereCentered(Graphics3D.GREEN, 3, screenP);
        
        if (SHOW_TORUS_PROBE_CENTER_VERTEX_CONNECTIONS && screenP != null) {
          g3d.fillCylinder(Graphics3D.YELLOW, Graphics3D.ENDCAPS_FLAT, 4,
                           screensI[torus.vertexesIP[i]], screenP);
          
          g3d.fillCylinder(Graphics3D.CYAN, Graphics3D.ENDCAPS_FLAT, 4,
                           screensJ[torus.vertexesJP[i]], screenP);
        }
      }
      if (SHOW_TORUS_VERTEX_CONNECTIONS) {
        g3d.fillCylinder(Graphics3D.MAGENTA, Graphics3D.ENDCAPS_FLAT, 4,
                         screensI[torus.vertexesIP[i]],
                         screensJ[torus.vertexesJP[i]]);
      }
    }

    for (int i = 0; i < torus.ijTriangleCount; i += 3) {
      short vertexI1 = torus.ijTriangles[i];
      short vertexJ1 = torus.ijTriangles[i + 1];
      short vertexI2 = torus.ijTriangles[i + 2];
      
      g3d.fillTriangle(false,
                       screensI[vertexI1], colixI, vertexI1,
                       screensJ[vertexJ1], colixJ, vertexJ1,
                       screensI[vertexI2], colixI, vertexI2);
    }

    for (int i = 0; i < torus.jiTriangleCount; i += 3) {
      short vertexJ1 = torus.jiTriangles[i];
      short vertexI1 = torus.jiTriangles[i + 1];
      short vertexJ2 = torus.jiTriangles[i + 2];
      
      g3d.fillTriangle(false,
                       screensJ[vertexJ1], colixJ, vertexJ1,
                       screensI[vertexI1], colixI, vertexI1,
                       screensJ[vertexJ2], colixJ, vertexJ2);
    }

  }
  */

  void renderCavity(Surface.Cavity cavity, Atom[] atoms,
                    short[] colixesCavity,
                    int[][] convexVertexMaps) {
    if (hideCavities)
      return;
    if (CAVITY_DOTS) {
      Point3i screen;
      screen = viewer.transformPoint(cavity.pointPI);
      g3d.fillSphereCentered(Graphics3D.RED, 4, screen);
      
      screen = viewer.transformPoint(cavity.pointPJ);
      g3d.fillSphereCentered(Graphics3D.GREEN, 4, screen);
      
      screen = viewer.transformPoint(cavity.pointPK);
      g3d.fillSphereCentered(Graphics3D.BLUE, 4, screen);
    }
    int[] faceMap = cavity.cavityFaceMap;
    if (faceMap == null)
      return;
    calcProbePoints(cavity.probeCenter, cavity.cavityVertexMap, probeScreens);
    short colix = Graphics3D.GREEN;
    for (int i = Bmp.getMaxMappedBit(faceMap), j = 3*i - 1; --i >= 0; j -= 3) {
      if (Bmp.getBit(faceMap, i)) {
        short vA = geodesicFaceVertexes[j - 2];
        short vB = geodesicFaceVertexes[j - 1];
        short vC = geodesicFaceVertexes[j];
        g3d.fillTriangle(colix,
                         probeScreens[vA], g3d.getInverseNormix(vA),
                         probeScreens[vB], g3d.getInverseNormix(vB),
                         probeScreens[vC], g3d.getInverseNormix(vC));
      }
    }
  }

  void renderCavity2(Surface.Cavity cavity, Atom[] atoms,
                     short[] colixesCavity,
                     int[][] convexVertexMaps) {
    if (hideCavities)
      return;
    short vertexI = cavity.vertexI; if (vertexI < 0) return;
    short vertexJ = cavity.vertexJ; if (vertexJ < 0) return;
    short vertexK = cavity.vertexK; if (vertexK < 0) return;
    int ixI = cavity.ixI;
    int ixJ = cavity.ixJ;
    int ixK = cavity.ixK;

    Atom atomI = atoms[ixI];
    Atom atomJ = atoms[ixJ];
    Atom atomK = atoms[ixK];
    
    short colixI = Graphics3D.inheritColix(colixesCavity[ixI],
                                           atomI.colixAtom);
    short colixJ = Graphics3D.inheritColix(colixesCavity[ixJ],
                                           atomJ.colixAtom);
    short colixK = Graphics3D.inheritColix(colixesCavity[ixK],
                                           atomK.colixAtom);

    if (CAVITY_DOTS) {
      Point3i screen;
      screen = viewer.transformPoint(cavity.pointPI);
      g3d.fillSphereCentered(Graphics3D.RED, 4, screen);
      
      screen = viewer.transformPoint(cavity.pointPJ);
      g3d.fillSphereCentered(Graphics3D.GREEN, 4, screen);
      
      screen = viewer.transformPoint(cavity.pointPK);
      g3d.fillSphereCentered(Graphics3D.BLUE, 4, screen);
    }
    Point3i[] screensI = screensCache.lookup(atomI,
                                             convexVertexMaps[ixI]);
    Point3i[] screensJ = screensCache.lookup(atomJ,
                                             convexVertexMaps[ixJ]);
    Point3i[] screensK = screensCache.lookup(atomK,
                                             convexVertexMaps[ixK]);
    if (CAVITY_DOTS) {
      g3d.fillSphereCentered(Graphics3D.RED, 8, screensI[vertexI]);
      g3d.fillSphereCentered(Graphics3D.GREEN, 8, screensJ[vertexJ]);
      g3d.fillSphereCentered(Graphics3D.BLUE, 8, screensK[vertexK]);
    }

    g3d.fillTriangle(false,
                     screensI[vertexI], colixI, vertexI,
                     screensJ[vertexJ], colixJ, vertexJ,
                     screensK[vertexK], colixK, vertexK);
  }

  final Point3f probePointT = new Point3f();

  void calcProbePoints(Point3f probeCenter, int[] vertexMap,
                       Point3i[] screens) {
    for (int i = Bmp.getMaxMappedBit(vertexMap); --i >= 0; ) {
      if (! Bmp.getBit(vertexMap, i))
        continue;
      probePointT.add(probeCenter, transformedProbeVertexes[i]);
      screens[i].set(viewer.transformPoint(probePointT));
    }
  }
}

class ScreensCache {

  final Viewer viewer;
  final int cacheSize;
  int screensLength;
  Point3i[][] cacheScreens;
  int[] cacheAtomIndexes;
  int lruClock;
  int[] cacheLrus;

  Vector3f[] transformedVectors;

  ScreensCache(Viewer viewer, int cacheSize) {
    this.viewer = viewer;
    this.cacheSize = cacheSize;
    cacheScreens = new Point3i[cacheSize][];
    cacheAtomIndexes = new int[cacheSize];
    cacheLrus = new int[cacheSize];
  }

  void alloc(int screensLength) {
    this.screensLength = screensLength;
    for (int i = cacheSize; --i >= 0; ) {
      cacheScreens[i] = viewer.allocTempScreens(screensLength);
      cacheAtomIndexes[i] = -1;
      cacheLrus[i] = -1;
    }
    lruClock = 0;
    transformedVectors = viewer.g3d.getTransformedVertexVectors();
  }

  Point3i[] lookup(Atom atom, int[] vertexMap) {
    int atomIndex = atom.atomIndex;
    for (int i = cacheSize; --i >= 0; ) {
      if (cacheAtomIndexes[i] == atomIndex) {
        cacheLrus[i] = lruClock++;
        return cacheScreens[i];
      }
    }
    int iOldest = 0;
    int lruOldest = cacheLrus[0];
    for (int i = cacheSize; --i > 0; ) { // only > 0
      if (cacheLrus[i] < lruOldest) {
        lruOldest = cacheLrus[i];
        iOldest = i;
      }
    }
    Point3i[] screens = cacheScreens[iOldest];
    calcScreenPoints(atom, vertexMap, screens);
    cacheAtomIndexes[iOldest] = atomIndex;
    cacheLrus[iOldest] = lruClock++;
    return screens;
  }

  void touch(Point3i[] screens) {
    for (int i = cacheSize; --i >= 0; ) {
      if (screens == cacheScreens[i]) {
        cacheLrus[i] = lruClock++;
        return;
      }
    }
    throw new NullPointerException();
  }

  void free() {
    for (int i = cacheSize; --i >= 0; ) {
      cacheScreens[i] = null;
      cacheAtomIndexes[i] = -1;
      cacheLrus[i] = -1;
    }
  }

  void calcScreenPoints(Atom atom, int[] vertexMap, Point3i[] screens) {
    float radius = atom.getVanderwaalsRadiusFloat();
    int atomX = atom.getScreenX();
    int atomY = atom.getScreenY();
    int atomZ = atom.getScreenZ();
    float scaledRadius = viewer.scaleToScreen(atomZ, radius);
    for (int vertex = Bmp.getMaxMappedBit(vertexMap); --vertex >= 0; ) {
      if (! Bmp.getBit(vertexMap, vertex))
        continue;
      Vector3f tv = transformedVectors[vertex];
      Point3i screen = screens[vertex];
      screen.x = atomX + (int)(scaledRadius * tv.x);
      screen.y = atomY - (int)(scaledRadius * tv.y); // y inverted on screen!
      screen.z = atomZ - (int)(scaledRadius * tv.z); // smaller z comes to me
    }
  }
}
