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
  SasCache sasCache;
  //  ScreensCache screensCache;
  float radiusP;

  Vector3f[] transformedProbeVertexes;
  Point3i[] probeScreens;

  void initRenderer() {
    int maxVertexCount =
      g3d.getGeodesicVertexCount(Sasurface.MAX_GEODESIC_RENDERING_LEVEL);
    sasCache =
      new SasCache(viewer, 6, maxVertexCount);
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
    sasCache.clear();
    for (int i = surfaceCount; --i >= 0; )
      renderSasurface1(surfaces[i]);
  }

  void renderSasurface1(Sasurface1 surface) {
    if (surface.hide)
      return;
    int renderingLevel = surface.geodesicRenderingLevel;
    radiusP = surface.radiusP;
    geodesicVertexCount = surface.geodesicVertexCount;
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
    Point3i[] screens = sasCache.lookupAtomScreens(atom, vertexMap);
    colix = Graphics3D.inheritColix(colix, atom.colixAtom);
    /*
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
    */
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

  final short[] torusEdgeIndexes = new short[INNER_TORUS_STEP_COUNT];

  final byte[] torusSegmentStarts = new byte[MAX_SEGMENT_COUNT];

  void renderTorus(Sasurface1.Torus torus, short[] colixes) {
    Point3i[] screens = sasCache.lookupTorusScreens(torus);
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
}
