/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

  final static int[] mapNull = Surface.mapNull;

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
    int[][] surfaceConvexMaps = surface.surfaceConvexMaps;
    short[] colixesConvex = surface.colixesConvex;
    int displayModelIndex = this.displayModelIndex;
    for (int i = surface.surfaceConvexMax; --i >= 0; ) {
      int[] map = surfaceConvexMaps[i];
      if (map != null && map != mapNull) {
        Atom atom = atoms[i];
        if (displayModelIndex < 0 || displayModelIndex == atom.modelIndex)
          renderConvex(atom, colixesConvex[i], map);
      }
    }
    Surface.Torus[] tori = surface.tori;
    for (int i = surface.torusCount; --i >= 0; ) {
      Surface.Torus torus = tori[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[torus.indexII].modelIndex)
        renderTorus(torus, atoms, colixesConvex, surfaceConvexMaps);
    }
    Surface.Cavity[] cavities = surface.cavities;
    if (false) {
      System.out.println("concave surface rendering currently disabled");
      return;
    }
    for (int i = surface.cavityCount; --i >= 0; ) {
      Surface.Cavity cavity = cavities[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[cavity.ixI].modelIndex)
        renderCavity(cavities[i], atoms, colixesConvex, surfaceConvexMaps);
    }
    viewer.freeTempScreens(screens);
    screens = null;
  }

  private final static boolean CONVEX_DOTS = false;

  void renderConvex(Atom atom, short colix, int[] visibilityMap) {
    calcScreenPoints(visibilityMap,
                     atom.getVanderwaalsRadiusFloat(),
                     atom.getScreenX(), atom.getScreenY(),
                     atom.getScreenZ());
    if (colix == 0)
      colix = atom.colixAtom;
    int maxMappedVertex = getMaxMappedVertex(visibilityMap);
    if (CONVEX_DOTS) {
      // for the dots code
      g3d.setColix(colix);
      for (int vertex = maxMappedVertex; --vertex >= 0; ) {
        if (! getBit(visibilityMap, vertex))
          continue;
        g3d.drawPixel(screens[vertex], vertex);
      }
    } else {
      for (int i = geodesicFaceCount, j = 0; --i >= 0; ) {
        short vA = geodesicFaceVertexes[j++];
        short vB = geodesicFaceVertexes[j++];
        short vC = geodesicFaceVertexes[j++];
        if (vA >= maxMappedVertex ||
            vB >= maxMappedVertex ||
            vC >= maxMappedVertex ||
            !getBit(visibilityMap, vA) ||
            !getBit(visibilityMap, vB) ||
            !getBit(visibilityMap, vC))
          continue;
        // miguel 2005 01 14
        // for some reason it does not work when trying to
        // use the normix for the face. Something is wrong somewhere
        // but I give up.
        // just use gouraud shading, in spite of performance penalty
        short normix = geodesicFaceNormixes[i];
        g3d.fillTriangle(colix, false,
                         screens[vA], vA,
                         screens[vB], vB,
                         screens[vC], vC);
      }
    }
  }

  Point3f pointT = new Point3f();
  Point3f pointT1 = new Point3f();
  Matrix3f matrixT = new Matrix3f();
  Matrix3f matrixT1 = new Matrix3f();
  Matrix3f matrixRot = new Matrix3f();
  AxisAngle4f aaT = new AxisAngle4f();
  AxisAngle4f aaT1 = new AxisAngle4f();

  static final float torusStepAngle = 2 * (float)Math.PI / 64;

  void renderTorus(Surface.Torus torus,
                   Atom[] atoms, short[] colixes, int[][] surfaceConvexMaps) {
    if (true)
      return;
    if (surfaceConvexMaps[torus.indexII] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixI, colixes, atoms, torus.indexII),
                      false);
    if (surfaceConvexMaps[torus.indexJJ] != null)
      renderTorusHalf(torus,
                      getColix(torus.colixJ, colixes, atoms, torus.indexJJ),
                      true);
  }

  short getColix(short colix, short[] colixes, Atom[] atoms, int index) {
    if (colix != 0)
      return colix;
    if (colixes[index] != 0)
      return colixes[index];
    return atoms[index].colixAtom;
  }

  void renderTorusHalf(Surface.Torus torus, short colix, boolean renderJHalf) {
    g3d.setColix(colix);
    long probeMap = torus.probeMap;

    int torusDotCount1 =
      (int)(getTorusOuterDotCount() * torus.outerAngle / (2 * Math.PI));
    float stepAngle1 = torus.outerAngle / torusDotCount1;
    if (renderJHalf)
      stepAngle1 = -stepAngle1;
    aaT1.set(torus.tangentVector, 0);

    aaT.set(torus.axisVector, 0);
    int step = getTorusIncrement();
    for (int i = 0; probeMap != 0; i += step, probeMap <<= step) {
      if (probeMap >= 0)
        continue;
      aaT.angle = i * torusStepAngle;
      matrixT.set(aaT);
      matrixT.transform(torus.radialVector, pointT);
      pointT.add(torus.center);

      for (int j = torusDotCount1; --j >= 0; ) {
        aaT1.angle = j * stepAngle1;
        matrixT1.set(aaT1);
        matrixT1.transform(torus.outerRadial, pointT1);
        matrixT.transform(pointT1);
        pointT1.add(pointT);
        g3d.drawPixel(viewer.transformPoint(pointT1));
      }
    }
  }

  void calcScreenPoints(int[] visibilityMap, float radius,
			  int atomX, int atomY, int atomZ) {
    float scaledRadius = viewer.scaleToScreen(atomZ, radius);
    for (int vertex = getMaxMappedVertex(visibilityMap); --vertex >= 0; ) {
      if (! getBit(visibilityMap, vertex))
        continue;
      Vector3f tv = transformedVectors[vertex];
      Point3i screen = screens[vertex];
      screen.x = atomX + (int)(scaledRadius * tv.x);
      screen.y = atomY - (int)(scaledRadius * tv.y); // y inverted on screen!
      screen.z = atomZ - (int)(scaledRadius * tv.z); // smaller z comes to me
    }
  }

  int getTorusIncrement() {
    if (scalePixelsPerAngstrom <= 5)
      return 16;
    if (scalePixelsPerAngstrom <= 10)
      return 8;
    if (scalePixelsPerAngstrom <= 20)
      return 4;
    if (scalePixelsPerAngstrom <= 40)
      return 2;
    return 1;
  }

  int getTorusOuterDotCount() {
    int dotCount = 8;
    if (scalePixelsPerAngstrom > 5) {
      dotCount = 16;
      if (scalePixelsPerAngstrom > 10) {
        dotCount = 32;
        if (scalePixelsPerAngstrom > 20) {
          dotCount = 64;
        }
      }
    }
    return dotCount;
  }

  /**
   * So, I need some help with this.
   * I cannot think of a good way to render this cavity.
   * The shapes are spherical triangle, but are very irregular.
   * In the center of aromatic rings there are 2-4 ... which looks ugly
   * So, if you have an idea how to render this, please let me know.
   */

  final static byte nearI = (byte)(1 << 0);
  final static byte nearJ = (byte)(1 << 1);
  final static byte nearK = (byte)(1 << 2);

  final static byte[] nearAssociations = {
    nearI | nearJ | nearK,

    nearI, nearJ, nearK,
    nearI | nearJ, nearJ | nearK, nearK | nearI,
    nearI, nearJ, nearJ, nearK, nearK, nearI,
    // index 13 starts here
    nearI, nearJ, nearK,
    nearI | nearJ, nearJ | nearK, nearK | nearI,
    nearI, nearJ, nearJ, nearK, nearK, nearI,
  };

  void renderCavity(Surface.Cavity cavity,
                    Atom[] atoms, short[] colixes, int[][] surfaceConvexMaps) {
    Point3f[] points = cavity.points;
    short[] normixes = cavity.normixes;
    for (int i = 4; --i >= 0; )
      viewer.transformPoint(points[i], screens[i]);
    
    short colix1 = getColix(cavity.colixI, colixes, atoms, cavity.ixI);
    short colix2 = getColix(cavity.colixJ, colixes, atoms, cavity.ixJ);
    short colix3 = getColix(cavity.colixK, colixes, atoms, cavity.ixK);
    short colix0 = Graphics3D.YELLOW;
                        

    g3d.fillTriangle(false,
                     screens[0], colix0, normixes[0],
                     screens[1], colix1, normixes[1],
                     screens[2], colix2, normixes[2]);
    g3d.fillTriangle(false,
                     screens[0], colix0, normixes[0],
                     screens[2], colix2, normixes[2],
                     screens[3], colix3, normixes[3]);
    g3d.fillTriangle(false,
                     screens[0], colix0, normixes[0],
                     screens[1], colix1, normixes[1],
                     screens[3], colix3, normixes[3]);
  }
  
  final static boolean getBit(int[] bitmap, int i) {
    return (bitmap[(i >> 5)] << (i & 31)) < 0;
  }

  int getMaxMappedVertex(int[] bitmap) {
    if (bitmap == null)
      return 0;
    int maxMapped = bitmap.length << 5;
    return maxMapped < geodesicVertexCount ? maxMapped : geodesicVertexCount;
  }
}

