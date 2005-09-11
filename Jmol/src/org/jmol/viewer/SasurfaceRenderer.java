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
    for (int i = -1; (i = Bmp.nextSetBit(faceMap, i + 1)) >= 0; ) {
      int j = 3 * i;
      short vA = geodesicFaceVertexes[j];
      short vB = geodesicFaceVertexes[j + 1];
      short vC = geodesicFaceVertexes[j + 2];
      g3d.fillTriangle(colix,
                       screens[vA], vA,
                       screens[vB], vB,
                       screens[vC], vC);
    }
    /*
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
    */
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
    Sasurface1.Torus.TorusCavity[] torusCavities = torus.torusCavities;
    int torusCavityIndex = 0;
    int ixP = 0;
    int torusSegmentCount = torus.torusSegmentCount;
    for (int i = 0; i < torusSegmentCount; ++i) {
      if (torusCavities != null)
        renderTorusCavityTriangle(screens, normixes, ixP, outerPointCount,
                                  colixes,
                                  torusCavities[torusCavityIndex++]);
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
      if (torusCavities != null)
        renderTorusCavityTriangle(screens, normixes, ixP, outerPointCount,
                                  colixes,
                                  torusCavities[torusCavityIndex++]);
      ixP = ixQ;
    }
    
  }

  final Point3i screenCavityBottom = new Point3i();

  void renderTorusCavityTriangle(Point3i[] torusScreens,
                                 short[] torusNormixes,
                                 int torusIndex,
                                 int torusPointCount,
                                 short[] colixes,
                                 Sasurface1.Torus.TorusCavity torusCavity) {
        Sasurface1.Cavity cavity = torusCavity.cavity;
    viewer.transformPoint(cavity.pointBottom, screenCavityBottom);
    short normixCavityBottom = cavity.normixBottom;
    Point3i torusScreenLast = torusScreens[torusIndex];
    short torusNormixLast = torusNormixes[torusIndex];
    short colixLast = colixes[0];
    ++torusIndex;
    for (int i = 1; i < torusPointCount; ++i) {
      Point3i torusScreen = torusScreens[torusIndex];
      short torusNormix = torusNormixes[torusIndex];
      ++torusIndex;
      short colix = colixes[i];
      short colixBottom = colix;
      if (colix != colixLast)
        colixBottom = g3d.getColixMix(colix, colixLast);
      g3d.fillTriangle(torusScreenLast, colixLast, torusNormixLast,
                       torusScreen, colix, torusNormix, 
                       screenCavityBottom, colixBottom, normixCavityBottom);
      torusScreenLast = torusScreen;
      torusNormixLast = torusNormix;
      colixLast = colix;
    }
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
