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


class SasurfaceRenderer extends ShapeRenderer {

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

    Sasurface sasurface = (Sasurface)shape;
    if (sasurface == null)
      return;
    int surfaceCount = sasurface.surfaceCount;
    if (surfaceCount == 0)
      return;
    Sasurface1[] surfaces = sasurface.surfaces;
    hideSaddles = viewer.getTestFlag1();
    hideCavities = viewer.getTestFlag2();
    hideConvex = viewer.getTestFlag3();
    for (int i = surfaceCount; --i >= 0; )
      renderSasurface1(surfaces[i]);
  }

  void renderSasurface1(Sasurface1 surface) {
    if (surface.hide)
      return;
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
          renderConvex(surface, atom, colixesConvex[i], vertexMap, faceMap);
      }
    }
    Sasurface1.Torus[] toruses = surface.toruses;
    for (int i = surface.torusCount; --i >= 0; ) {
      Sasurface1.Torus torus = toruses[i];
      /*
      renderToruX(torus,
                  atoms[ixI], convexVertexMaps[ixI],
                  atoms[ixJ], convexVertexMaps[ixJ],
                  colixesConvex,
                  renderingLevel);
      */
      renderTorus(torus, atoms, colixesConvex, convexVertexMaps);
    }
    Sasurface1.Cavity[] cavities = surface.cavities;
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
  private final static boolean CAVITY_DOTS = false;

  void renderConvex(Sasurface1 surface, Atom atom,
                    short colix, int[] vertexMap, int[] faceMap) {
    if (hideConvex)
      return;
    Point3i[] screens = screensCache.lookup(atom, vertexMap);
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    if (CONVEX_DOTS) {
      int[] edgeVertexes = surface.calcEdgeVertexes(vertexMap);
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
  void renderTorus(Sasurface1.Torus torus,
                   Atom[] atoms, short[] convexColixes,
                   int[][] convexVertexMaps) {
    //    if (convexVertexMaps[torus.ixA] != null)
    //    if (convexVertexMaps[torus.ixB] != null)
    //getColix(torus.colixB, colixes, atoms, torus.ixB));
    prepareTorusColixes(torus, convexColixes, atoms);
    renderTorus(torus, torusColixes);
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
  static final int INNER_TORUS_STEP_COUNT = Sasurface1.INNER_TORUS_STEP_COUNT;
  static final int OUTER_TORUS_STEP_COUNT = Sasurface1.OUTER_TORUS_STEP_COUNT;
  static final float INNER_TORUS_STEP_ANGLE=Sasurface1.INNER_TORUS_STEP_ANGLE;
  final static int MAX_SEGMENT_COUNT = Sasurface1.MAX_SEGMENT_COUNT;
  final Matrix3f matrixT = new Matrix3f();
  final Matrix3f matrixT1 = new Matrix3f();
  final Point3f pointT = new Point3f();
  final Point3f pointT1 = new Point3f();

  final Point3f[] torusPoints = (new Point3f[INNER_TORUS_STEP_COUNT *
                                             OUTER_TORUS_STEP_COUNT]);
  final Point3i[] torusScreens = (new Point3i[INNER_TORUS_STEP_COUNT *
                                              OUTER_TORUS_STEP_COUNT]);
  {
    for (int i = torusPoints.length; --i >= 0; ) {
      torusPoints[i] = new Point3f();
      torusScreens[i] = new Point3i();
    }
  }

  final short[] torusEdgeIndexes = new short[INNER_TORUS_STEP_COUNT];

  final byte[] torusSegmentStarts = new byte[MAX_SEGMENT_COUNT];

  void renderTorus(Sasurface1.Torus torus, short[] colixes) {
    torus.calcPoints(torusPoints);
    Point3i[] screens = torusScreens;
    torus.calcScreens(torusPoints, screens);
    short[] normixes = torus.normixes;
    int outerPointCount = torus.outerPointCount;
    int ixP = 0;
    int torusSegmentCount = torus.torusSegmentCount;
    for (int i = 0; i < torusSegmentCount; ++i) {
      int stepCount = torus.torusSegments[i].stepCount;
      int ixQ = ixP + outerPointCount;
      for (int j = stepCount; --j > 0; ) { // .GT.
        ++ixP;
        ++ixQ;
        for (int k = 1; k < outerPointCount; ++k) {
          g3d.fillQuadrilateral(screens[ixP-1], colixes[k-1], normixes[ixP-1],
                                screens[ixP  ], colixes[k  ], normixes[ixP  ],
                                screens[ixQ  ], colixes[k  ], normixes[ixQ  ],
                                screens[ixQ-1], colixes[k-1], normixes[ixQ-1]);
          ++ixP;
          ++ixQ;
        }
      }
      ixP = ixQ;
    }
    
    //    renderTorusEdges(torus);
    //    Point3i[] convexScreens = screensCache.lookup(atom, vertexMap);
    //    renderTorusAtomConnections(torus, null);
  }

  /*
  void renderTorusEdges(Sasurface1.Torus torus) {
    Point3i[] screens = torusScreens;
    // show torus edges
    int segmentCount = torus.countContiguousSegments(torusSegmentStarts);
    //    System.out.println("segmentCount=" + segmentCount);
    for (int m = segmentCount; --m >= 0; ) {
      int segmentStart = torusSegmentStarts[m];
      int countA = torus.extractAtomEdgeIndexes(segmentStart,
                                                false, torusEdgeIndexes);
      for (int n = countA; --n >= 0; )
        g3d.fillSphereCentered(g3d.MAGENTA,
                               5 + (3*m), screens[torusEdgeIndexes[n]]);
      
      int countB = torus.extractAtomEdgeIndexes(segmentStart,
                                                true, torusEdgeIndexes);
      for (int n = countB; --n >= 0; )
        g3d.fillSphereCentered(g3d.CYAN,
                               5 + (3*m), screens[torusEdgeIndexes[n]]);
    }
  }
  */

  void renderTorusAtomConnections(Sasurface1.Torus torus,
                                  Point3i screensConvex) {
    //    System.out.println("torus.connectAConvex=" +
    //                       torus.connectAConvex);
    short[] x = torus.connectAConvex;
    /*
    if (x != null) {
      for (int i = 0; i < x.length; ++i)
        System.out.print(" " + x[i]);
      System.out.println("");
    }
    */
    /*
    int segmentCount = torus.countCcontiguousSegments(torusSegmentStarts);
    for (int m = segmentCount; --m >= 0; ) {
      renderTorusAtomConnection(torus, torusSegmentStarts[m], 
    short[] connectAConvex = torus.connectA;
    if (connectAConvex == null) {
      System.out.println("connectA == null");
      return;
    }
    int countA = torus.extractAtomEdgeIndexes(segmentStart,
                                              false, torusEdgeIndexes);
    
      short[] torusEdgeIndexes = new short[INNER_TORUS_STEP_COUNT];

    for (int i = connectA.length; --i >= 0; )
      drawOneConnection(torusScreens[connectA[i]],
                        screensConvex[
    */
  }
    
  final short[] torusColixes = new short[OUTER_TORUS_STEP_COUNT];

  void prepareTorusColixes(Sasurface1.Torus torus, short[] convexColixes,
                           Atom[] atoms) {
    int ixA = torus.ixA;
    int ixB = torus.ixB;
    int outerPointCount = torus.outerPointCount;
    short colixB = torus.colixB;
    short colixA = Graphics3D.inheritColix(torus.colixA,
                                           convexColixes[ixA],
                                           atoms[ixA].colixAtom);
    colixB = Graphics3D.inheritColix(torus.colixB,
                                     convexColixes[ixB],
                                     atoms[ixB].colixAtom);
    if (colixA == colixB) {
      for (int i = outerPointCount; --i >= 0; )
        torusColixes[i] = colixA;
      return;
    }
    int halfRoundedUp = (outerPointCount + 1) / 2;
    // this will get overwritten if outerPointCount is even
    torusColixes[outerPointCount / 2] = colixA;
    for (int i = outerPointCount / 2; --i >= 0; ) {
      torusColixes[i] = colixA;
      torusColixes[i + halfRoundedUp] = colixB;
    }
  }

  ////////////////////////////////////////////////////////////////


  Vector3f vectorIJ = new Vector3f();
  Vector3f vectorJI = new Vector3f();

  boolean SHOW_TORUS_PROBE_CENTERS = false;
  boolean SHOW_TORUS_PROBE_CENTER_VERTEX_CONNECTIONS = false;
  boolean SHOW_TORUS_VERTEX_CONNECTIONS = false;

  boolean SHOW_TORUS_CAVITY_FOO = false;
  
  final Point3i[] cavityScreens = new Point3i[4];
  {
    for (int i = cavityScreens.length; --i >= 0; )
      cavityScreens[i] = new Point3i();
  };

  void renderCavity(Sasurface1.Cavity cavity, Atom[] atoms,
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
    viewer.transformPoint(cavity.pointBottom, cavityScreens[0]);
    viewer.transformPoint(cavity.pointPI, cavityScreens[1]);
    viewer.transformPoint(cavity.pointPJ, cavityScreens[2]);
    viewer.transformPoint(cavity.pointPK, cavityScreens[3]);
    short[] normixes = cavity.normixes;
    short colix = Graphics3D.YELLOW;
    renderCavitySegment(colix, cavityScreens, normixes, 0, 1, 2);
    renderCavitySegment(colix, cavityScreens, normixes, 0, 2, 3);
    renderCavitySegment(colix, cavityScreens, normixes, 0, 3, 1);
  }

  void renderCavitySegment(short colix, Point3i[] cavityScreens,
                           short[] normixes,
                           int iA, int iB, int iC) {
    g3d.fillTriangle(colix,
                     cavityScreens[iA], normixes[iA],
                     cavityScreens[iB], normixes[iB],
                     cavityScreens[iC], normixes[iC]);
  }

  void renderCavity2(Sasurface1.Cavity cavity, Atom[] atoms,
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

    g3d.fillTriangle(screensI[vertexI], colixI, vertexI,
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
