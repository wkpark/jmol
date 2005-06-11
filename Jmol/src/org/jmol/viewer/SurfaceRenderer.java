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
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;
  int geodesicVertexCount;
  int geodesicFaceCount;
  short[] geodesicFaceVertexes;
  short[] geodesicFaceNormixes;
  ScreensCache screensCache;

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
    int renderingLevel = surface.geodesicRenderingLevel;
    geodesicVertexCount = surface.geodesicVertexCount;
    screensCache.alloc(geodesicVertexCount);
    geodesicFaceCount =
      g3d.getGeodesicFaceCount(renderingLevel);
    geodesicFaceVertexes =
      g3d.getGeodesicFaceVertexes(renderingLevel);
    geodesicFaceNormixes =
      g3d.getGeodesicFaceNormixes(renderingLevel);
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
      int ixI = torus.ixI;
      int ixJ = torus.ixJ;
      renderTorus(torus,
                  atoms[ixI], convexVertexMaps[ixI],
                  atoms[ixJ], convexVertexMaps[ixJ], renderingLevel);
    }
    Surface.Cavity[] cavities = surface.cavities;
    for (int i = surface.cavityCount; --i >= 0; )
      renderCavity(cavities[i], atoms, convexVertexMaps);
    screensCache.free();
  }

  private final static boolean CONVEX_DOTS = false;
  private final static boolean CAVITY_DOTS = false;

  void renderConvex(Atom atom, short colix, int[] vertexMap, int[] faceMap) {
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

  Vector3f vectorIJ = new Vector3f();
  Vector3f vectorJI = new Vector3f();

  void renderTorus(Surface.Torus torus,
                   Atom atomI, int[] vertexMapI,
                   Atom atomJ, int[] vertexMapJ, int renderingLevel) {
    if (vertexMapI == null || vertexMapJ == null)
      return;
    Point3i[] screensI = screensCache.lookup(atomI, vertexMapI);
    Point3i[] screensJ = screensCache.lookup(atomJ, vertexMapJ);

    Point3i screenClosestI = screensI[torus.normixI];
    Point3i screenClosestJ = screensJ[torus.normixJ];
    /*
    g3d.fillCylinder(Graphics3D.PINK, Graphics3D.ENDCAPS_FLAT, 4,
                     screensI[torus.normixI],
                     screensJ[torus.normixJ]);
    */
    for (int i = torus.connectionCount; (i -= 2) >= 0; ) {
      short vertexA = torus.connections[i];
      short vertexB = torus.connections[i + 1];

      g3d.fillCylinder(Graphics3D.ORANGE, Graphics3D.ENDCAPS_FLAT, 4,
                       screensI[vertexA],
                       screensJ[vertexB]);
    }

    for (int i = torus.pointspCount; --i >= 0; ) {
      Point3i screenP = viewer.transformPoint(torus.pointsp[i]);
      g3d.fillSphereCentered(Graphics3D.GREEN, 3, screenP);

      boolean CONNECT_WITH_DOT = true;
      if (CONNECT_WITH_DOT) {
        g3d.fillCylinder(Graphics3D.YELLOW, Graphics3D.ENDCAPS_FLAT, 4,
                         screensI[torus.vertexesIP[i]], screenP);
        
        g3d.fillCylinder(Graphics3D.CYAN, Graphics3D.ENDCAPS_FLAT, 4,
                         screensJ[torus.vertexesJP[i]], screenP);
      } else {
        g3d.fillCylinder(Graphics3D.MAGENTA, Graphics3D.ENDCAPS_FLAT, 4,
                         screensI[torus.vertexesIP[i]],
                         screensJ[torus.vertexesJP[i]]);
      }
    }
  }

  void renderCavity(Surface.Cavity cavity, Atom[] atoms,
                    int[][] convexVertexMaps) {
    short vertexI = cavity.vertexI; if (vertexI < 0) return;
    short vertexJ = cavity.vertexJ; if (vertexJ < 0) return;
    short vertexK = cavity.vertexK; if (vertexK < 0) return;

    Atom atomI = atoms[cavity.ixI];
    Atom atomJ = atoms[cavity.ixJ];
    Atom atomK = atoms[cavity.ixK];
    
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
                                             convexVertexMaps[cavity.ixI]);
    Point3i[] screensJ = screensCache.lookup(atomJ,
                                             convexVertexMaps[cavity.ixJ]);
    Point3i[] screensK = screensCache.lookup(atomK,
                                             convexVertexMaps[cavity.ixK]);
    if (CAVITY_DOTS) {
      g3d.fillSphereCentered(Graphics3D.RED, 8, screensI[vertexI]);
      g3d.fillSphereCentered(Graphics3D.GREEN, 8, screensJ[vertexJ]);
      g3d.fillSphereCentered(Graphics3D.BLUE, 8, screensK[vertexK]);
    }

    g3d.fillTriangle(false,
                     screensI[vertexI], atomI.colixAtom, vertexI,
                     screensJ[vertexJ], atomJ.colixAtom, vertexJ,
                     screensK[vertexK], atomK.colixAtom, vertexK);
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
