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

import org.jmol.g3d.Graphics3D;

class SurfaceRenderer extends ShapeRenderer {

  boolean perspectiveDepth;
  int scalePixelsPerAngstrom;
  boolean bondSelectionModeOr;
  int geodesicVertexCount;
  Vector3f[] transformedVectors;
  int geodesicFaceCount;
  short[] geodesicFaceVertexes;
  short[] geodesicFaceNormixes;
  Point3i[] screens;

  void initRenderer() {
  }

  void render() {
    perspectiveDepth = viewer.getPerspectiveDepth();
    scalePixelsPerAngstrom = (int)viewer.getScalePixelsPerAngstrom();
    bondSelectionModeOr = viewer.getBondSelectionModeOr();

    Surface surface = (Surface)shape;
    if (surface == null)
      return;
    transformedVectors = g3d.getTransformedVertexVectors();
    geodesicVertexCount = surface.geodesicVertexCount;
    screens = viewer.allocTempScreens(geodesicVertexCount);
    geodesicFaceCount =
      g3d.getGeodesicFaceCount(surface.geodesicRenderingLevel);
    geodesicFaceVertexes =
      g3d.getGeodesicFaceVertexes(surface.geodesicRenderingLevel);
    geodesicFaceNormixes =
      g3d.getGeodesicFaceNormixes(surface.geodesicRenderingLevel);
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

    viewer.freeTempScreens(screens);
    screens = null;
  }

  private final static boolean CONVEX_DOTS = true;

  void renderConvex(Atom atom, short colix, int[] vertexMap, int[] faceMap) {
    calcScreenPoints(vertexMap,
                     atom.getVanderwaalsRadiusFloat(),
                     atom.getScreenX(), atom.getScreenY(),
                     atom.getScreenZ());
    if (colix == 0)
      colix = atom.colixAtom;
    if (CONVEX_DOTS) {
      int[] edgeVertexes = ((Surface)shape).calcEdgeVertexes(vertexMap);
      for (int vertex = Surface.getMaxMappedBit(vertexMap); --vertex >= 0; ) {
        if (Surface.getBit(edgeVertexes, vertex)) {
          Point3i screen = screens[vertex];
          g3d.fillSphereCentered(colix, 6, screen);
        } else if (Surface.getBit(vertexMap, vertex)) {
          Point3i screen = screens[vertex];
          g3d.fillSphereCentered(colix, 3, screen);
        }
      }
    }
    for (int i = Surface.getMaxMappedBit(faceMap), j = 3*i - 1; --i >= 0; j -= 3) {
      if (Surface.getBit(faceMap, i)) {
        short vA = geodesicFaceVertexes[j - 2];
        short vB = geodesicFaceVertexes[j - 1];
        short vC = geodesicFaceVertexes[j];
        g3d.fillTriangle(colix, false,
                         screens[vA], vA,
                         screens[vB], vB,
                         screens[vC], vC);
      }
    }
  }

  void calcScreenPoints(int[] vertexMap, float radius,
                        int atomX, int atomY, int atomZ) {
    float scaledRadius = viewer.scaleToScreen(atomZ, radius);
    for (int vertex = Surface.getMaxMappedBit(vertexMap); --vertex >= 0; ) {
      if (! Surface.getBit(vertexMap, vertex))
        continue;
      Vector3f tv = transformedVectors[vertex];
      Point3i screen = screens[vertex];
      screen.x = atomX + (int)(scaledRadius * tv.x);
      screen.y = atomY - (int)(scaledRadius * tv.y); // y inverted on screen!
      screen.z = atomZ - (int)(scaledRadius * tv.z); // smaller z comes to me
    }
  }
}


