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
  Point3i[] screensTorusStripLast;
  Point3i[] screensTorusStrip;

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
    screensTorusStrip = viewer.allocTempScreens(Surface.segmentsPerFullCircle);
    screensTorusStripLast =
      viewer.allocTempScreens(Surface.segmentsPerFullCircle);
    geodesicFaceCount =
      g3d.getGeodesicFaceCount(surface.geodesicRenderingLevel);
    geodesicFaceVertexes =
      g3d.getGeodesicFaceVertexes(surface.geodesicRenderingLevel);
    geodesicFaceNormixes =
      g3d.getGeodesicFaceNormixes(surface.geodesicRenderingLevel);
    Atom[] atoms = frame.atoms;
    int[][] convexSurfaceMaps = surface.convexSurfaceMaps;
    short[] colixesConvex = surface.colixesConvex;
    int displayModelIndex = this.displayModelIndex;
    for (int i = surface.surfaceConvexMax; --i >= 0; ) {
      int[] map = convexSurfaceMaps[i];
      if (map != null && map != mapNull) {
        Atom atom = atoms[i];
        if (displayModelIndex < 0 || displayModelIndex == atom.modelIndex)
          renderConvex(atom, colixesConvex[i], map);
      }
    }
    Surface.Torus[] toruses = surface.toruses;
    for (int i = surface.torusCount; --i >= 0; ) {
    //for (int i = 1; --i >= 0; ) {
      Surface.Torus torus = toruses[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[torus.ixI].modelIndex)
        renderTorus(torus, atoms, colixesConvex, convexSurfaceMaps);
    }

    Surface.Cavity[] cavities = surface.cavities;
    for (int i = surface.cavityCount; --i >= 0; ) {
      Surface.Cavity cavity = cavities[i];
      if (displayModelIndex < 0 ||
          displayModelIndex == atoms[cavity.ixI].modelIndex)
        renderCavity(cavities[i], atoms, colixesConvex, convexSurfaceMaps);
    }
    viewer.freeTempScreens(screens);
    viewer.freeTempScreens(screensTorusStrip);
    viewer.freeTempScreens(screensTorusStripLast);
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
        //short normix = geodesicFaceNormixes[i];
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
  Matrix3f matrixT2 = new Matrix3f();
  Matrix3f matrixRot = new Matrix3f();
  AxisAngle4f aaT = new AxisAngle4f();
  AxisAngle4f aaT1 = new AxisAngle4f();

  static final float torusStepAngle = 2 * (float)Math.PI / 64;

  void renderTorus(Surface.Torus torus,
                   Atom[] atoms, short[] colixes, int[][] convexSurfaceMaps) {
    if (true) {
      renderTorus1(torus, atoms, colixes, convexSurfaceMaps);
      return;
    }
    if (false) {
      if (convexSurfaceMaps[torus.ixI] != null)
        renderTorusHalf(torus,
                        getColix(torus.colixI, colixes, atoms, torus.ixI),
                        false);
      if (convexSurfaceMaps[torus.ixJ] != null)
        renderTorusHalf(torus,
                        getColix(torus.colixJ, colixes, atoms, torus.ixJ),
                        true);
      return;
    }
    
    short colix = getColix(torus.colixI, colixes, atoms, torus.ixI);
    g3d.setColix(colix);
    int stripCount = torus.stripPointArrays.length;
    short[] normixesLast = null;
    for (int i = 0; i < stripCount; ++i) {
      Point3f[] strip = torus.stripPointArrays[i];
      short[] normixes = torus.stripNormixesArrays[i];
      int stripLength = strip.length;
      for (int j = stripLength; --j >= 0; )
        viewer.transformPoint(strip[j], screensTorusStrip[j]);
      if (i > 0) {
        for (int j = stripLength; --j > 0; ) { // .GT. not .GE.
          int k = j - 1;
          g3d.fillQuadrilateral(colix, false,
                                screensTorusStripLast[k],
                                normixesLast[k],
                                screensTorusStripLast[j],
                                normixesLast[j],
                                screensTorusStrip[j],
                                normixes[j],
                                screensTorusStrip[k],
                                normixes[k]);
        }
      }
      normixesLast = normixes;
      Point3i[] t = screensTorusStripLast;
      screensTorusStripLast = screensTorusStrip;
      screensTorusStrip = t;
    }
  }

  void renderTorus1(Surface.Torus torus,
                    Atom[] atoms, short[] colixes, int[][] convexSurfaceMaps) {
    if (false)
      if (torus.ixI != 0 || torus.ixJ != 2)
        return;
    if (torus.torusCavities == null) {
      renderFullTorus(torus, atoms, colixes, convexSurfaceMaps);
      return;
    }

    Point3i screen = viewer.transformPoint(torus.center);
    g3d.fillSphereCentered(Graphics3D.RED, 10, screen);

    for (int c = 0; c < (torus.torusCavityCount & ~1); c += 2) {
      boolean rightHanded = torus.rightHandeds[c];
      int d = c + 1;
      screen =
        viewer.transformPoint(torus.torusCavities[c].getPoint(torus.ixI));
      g3d.fillSphereCentered(Graphics3D.GREEN, 10, screen);

      screen =
        viewer.transformPoint(torus.torusCavities[c].getPoint(torus.ixJ));
      g3d.fillSphereCentered(Graphics3D.BLUE, 10, screen);

      screen = viewer.transformPoint(torus.torusCavities[c].probeCenter);
      g3d.fillSphereCentered(Graphics3D.YELLOW, 10, screen);

      screen = viewer.transformPoint(torus.torusCavities[d].probeCenter);
      g3d.fillSphereCentered(Graphics3D.HOTPINK, 15, screen);

      Vector3f vA = new Vector3f();
      vA.sub(torus.torusCavities[c].probeCenter, torus.center);
      Vector3f vB = new Vector3f();
      vB.sub(torus.torusCavities[d].probeCenter, torus.center);

      Vector3f outerVectorA = new Vector3f();
      outerVectorA.sub(torus.torusCavities[c].getPoint(torus.ixI),
                       torus.torusCavities[c].probeCenter);
      float innerAngle = getAngle(rightHanded, vA, vB, torus.axisVector);
      int innerSegmentCount =
        (int)((innerAngle < 0 ? -innerAngle : innerAngle)
              / Surface.radiansPerSegment);
      if (innerSegmentCount == 0)
        ++innerSegmentCount;
      
      float actualRadiansPerInnerSegment = innerAngle / innerSegmentCount;
      
      
      Vector3f vT = new Vector3f();
      Point3f pT = new Point3f();
      Point3f pTO = new Point3f();
      
      Vector3f vTO = new Vector3f();

      vTO.set(outerVectorA);
      //    matrixT.transform(outerVectorA, vTO);
      pT.add(torus.torusCavities[c].probeCenter, vTO);
      screen = viewer.transformPoint(pT);
      g3d.fillSphereCentered(Graphics3D.ORANGE, 5, screen);

    
      int outerSegmentCount =
        (int)(torus.outerRadians / Surface.radiansPerSegment);
      if (outerSegmentCount == 0)
        ++outerSegmentCount;
      float actualRadiansPerOuterSegment =
        torus.outerRadians / outerSegmentCount;
      
      aaT.set(torus.axisVector, 0);
      vTO.cross(torus.axisVector, vA);
      aaT1.set(vTO, 0);
      for (int i = 0; i <= innerSegmentCount; ++i) {
        matrixT.set(aaT);
        matrixT.transform(vA, vT);
        pT.add(torus.center, vT);
        screen = viewer.transformPoint(pT);
        g3d.fillSphereCentered(rightHanded ? g3d.PINK : g3d.LIME,
                               10, screen);
        
        matrixT.transform(outerVectorA, vTO);
        pTO.add(pT, vTO);
        screen = viewer.transformPoint(pTO);
        g3d.fillSphereCentered(Graphics3D.ORANGE, 5, screen);
        
        aaT1.angle = 0;
        for (int j = 0; j <= outerSegmentCount; ++j) {
          matrixT1.set(aaT1);
          matrixT1.transform(outerVectorA, vTO);
          matrixT.transform(vTO);
          pTO.add(pT, vTO);
          screen = viewer.transformPoint(pTO);
          g3d.fillSphereCentered(Graphics3D.MAGENTA, 5, screen);
          
          aaT1.angle += actualRadiansPerOuterSegment;
        }
        aaT.angle += actualRadiansPerInnerSegment;
      }
    }
  }
  
  void renderFullTorus(Surface.Torus torus, Atom[] atoms,
                       short[] colixes, int[][] convexSurfaceMaps) {
    final int innerSegmentCount = Surface.segmentsPerFullCircle;
    final float radiansPerSegment = Surface.radiansPerSegment;

    Vector3f vT = new Vector3f();
    Point3f pT = new Point3f();
    Point3f pTO = new Point3f();

    Vector3f vTO = new Vector3f();

    int outerSegmentCount =
      (int)(torus.outerRadians / Surface.radiansPerSegment);
    if (outerSegmentCount == 0)
      ++outerSegmentCount;
    float actualRadiansPerOuterSegment =
      torus.outerRadians / outerSegmentCount;

    aaT.set(torus.axisVector, 0);
    vTO.cross(torus.axisVector, torus.radialVector);
    aaT1.set(vTO, 0);
    for (int i = 0; i <= innerSegmentCount; ++i) {
      matrixT.set(aaT);
      matrixT.transform(torus.radialVector, vT);
      pT.add(torus.center, vT);
      Point3i screen;
      screen = viewer.transformPoint(pT);
      g3d.fillSphereCentered(Graphics3D.PINK, 10, screen);

      aaT1.angle = 0;
      for (int j = 0; j <= outerSegmentCount; ++j) {
        matrixT1.set(aaT1);
        matrixT1.transform(torus.outerVector, vTO);
        matrixT.transform(vTO);
        pTO.add(pT, vTO);
        screen = viewer.transformPoint(pTO);
        g3d.fillSphereCentered(j == 0 ? Graphics3D.ORANGE : Graphics3D.MAGENTA,
                               5, screen);

        aaT1.angle += actualRadiansPerOuterSegment;
      }
      aaT.angle += radiansPerSegment;
    }
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
    long probeMap = -1;

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

  void renderCavity(Surface.Cavity cavity,
                    Atom[] atoms, short[] colixes, int[][] convexSurfaceMaps) {
    Point3f[] points = cavity.points;
    short[] normixes = cavity.normixes;
    if (screens.length < points.length) {
      viewer.freeTempScreens(screens);
      screens = viewer.allocTempScreens(points.length);
    }
    for (int i = points.length; --i >= 0; )
      viewer.transformPoint(points[i], screens[i]);

    short colix1 = getColix(cavity.colixI, colixes, atoms, cavity.ixI);
    //short colix2 = getColix(cavity.colixJ, colixes, atoms, cavity.ixJ);
    //short colix3 = getColix(cavity.colixK, colixes, atoms, cavity.ixK);
    //short colixCenter = Graphics3D.RED;
                        
    Point3i screenCenter = screens[0];
    short normixCenter = normixes[0];

    for (int i = 1; i < points.length; ++i) {
      int j = i + 1;
      if (j == points.length)
        j = 1;
      short colix = colix1;
      g3d.fillTriangle(false,
                       screenCenter, Graphics3D.RED, normixCenter,
                       screens[i], colix, normixes[i],
                       screens[j], colix, normixes[j]);
    }
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
}

