/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.jmol.g3d.*;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

class RocketsRenderer extends MpsRenderer {

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Rockets cartoon;

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    Rockets.Cchain cchain = (Rockets.Cchain)mpspolymer;
    render1Chain(cchain.polymer, cchain.mads, cchain.colixes);
  }

  void render1Chain(Polymer polymer, short[] mads, short[] colixes) {
    if (! (polymer instanceof AminoPolymer))
      return;
    initializeChain((AminoPolymer)polymer);
    clearPending();
    for (int i = 0; i < monomerCount; ++i) {
      if (mads[i] == 0)
        continue;
      short colix = colixes[i];
      Monomer monomer = monomers[i];
      if (colix == 0)
        colix = monomer.getLeadAtom().colixAtom;
      if (monomer.isHelixOrSheet()) {
        //        System.out.println("renderSpecialSegment[" + i + "]");
        renderSpecialSegment(monomer, colix, mads[i]);
      } else {
        //        System.out.println("renderRopeSegment[" + i + "]");
        renderRopeSegment(colix, mads, i,
                          monomerCount, monomers,
                          screens, isSpecials);
      }
    }
    renderPending();
    viewer.freeTempScreens(screens);
    viewer.freeTempPoints(cordMidPoints);
    viewer.freeTempBooleans(isSpecials);
  }

  int monomerCount;
  Monomer[] monomers;
  Point3i[] screens;
  boolean[] isSpecials;
  Point3f[] cordMidPoints;

  void initializeChain(AminoPolymer aminopolymer) {
    monomers = aminopolymer.monomers;
    monomerCount = aminopolymer.count;
    isSpecials = calcIsSpecials(monomerCount, monomers);
    cordMidPoints = calcRopeMidPoints(aminopolymer);
    screens = getScreens();
  }

  Point3f[] calcRopeMidPoints(AminoPolymer aminopolymer) {
    int midPointCount = monomerCount + 1;
    Point3f[] cordMidPoints = viewer.allocTempPoints(midPointCount);
    Monomer residuePrev = null;
    ProteinStructure proteinstructurePrev = null;
    Point3f point;
    for (int i = 0; i < monomerCount; ++i) {
      point = cordMidPoints[i];
      Monomer residue = monomers[i];
      if (isSpecials[i]) {
        ProteinStructure proteinstructure = residue.getProteinStructure();
        point.set(i - 1 != proteinstructure.getMonomerIndex()
                  ? proteinstructure.getAxisStartPoint()
                  : proteinstructure.getAxisEndPoint());

        //        if (i != structure.getStartResidueIndex()) {
        //          point.add(structure.getAxisEndPoint());
        //          point.scale(0.5f);
        //        }
        residuePrev = residue;
        proteinstructurePrev = proteinstructure;
      } else {
        if (proteinstructurePrev != null)
          point.set(proteinstructurePrev.getAxisEndPoint());
        else
          aminopolymer.getLeadMidPoint(i, point);
        residuePrev = null;
        proteinstructurePrev = null;
      }
    }
    point = cordMidPoints[monomerCount];
    if (proteinstructurePrev != null)
      point.set(proteinstructurePrev.getAxisEndPoint());
    else
      aminopolymer.getLeadMidPoint(monomerCount, point);
    return cordMidPoints;
  }

  Point3i[] getScreens() {
    int count = monomerCount + 1;
    Point3i[] screens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(cordMidPoints[i], screens[i]);
      //      g3d.fillSphereCentered(Colix.CYAN, 15, screens[i]);
    }
    return screens;
  }

  final Point3i screenA = new Point3i();
  Point3i screenB = new Point3i();
  Point3i screenC = new Point3i();

  void renderSpecialSegment(Monomer monomer, short colix, short mad) {
    ProteinStructure proteinstructure = monomer.getProteinStructure();
    if (tPending) {
      if (proteinstructure == proteinstructurePending &&
          mad == madPending &&
          colix == colixPending &&
          proteinstructure.getIndex(monomer) == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPending();
    }
    proteinstructurePending = proteinstructure;
    startIndexPending = endIndexPending = proteinstructure.getIndex(monomer);
    colixPending = colix;
    madPending = mad;
    tPending = true;
  }

  boolean tPending;
  ProteinStructure proteinstructurePending;
  int startIndexPending;
  int endIndexPending;
  short madPending;
  short colixPending;
  int[] shadesPending;

  void clearPending() {
    tPending = false;
  }

  void renderPending() {
    if (tPending) {
      Point3f[] segments = proteinstructurePending.getSegments();
      boolean tEnd =
        (endIndexPending == proteinstructurePending.getMonomerCount() - 1);

      /*
      System.out.println("structurePending.getPolymerCount()=" +
                         structurePending.getPolymerCount());
      System.out.println("segments.length=" + segments.length);
      System.out.println(" startIndexPending=" + startIndexPending +
                         " endIndexPending=" + endIndexPending);
      System.out.println("tEnd=" + tEnd);
      */
      if (proteinstructurePending instanceof Helix)
        renderPendingHelix(segments[startIndexPending],
                           segments[endIndexPending],
                           segments[endIndexPending + 1],
                           tEnd);
      else
        renderPendingSheet(segments[startIndexPending],
                           segments[endIndexPending],
                           segments[endIndexPending + 1],
                           tEnd);
      tPending = false;
    }
  }

  void renderPendingHelix(Point3f pointStart, Point3f pointBeforeEnd,
                          Point3f pointEnd, boolean tEnd) {
    viewer.transformPoint(pointStart, screenA);
    viewer.transformPoint(pointEnd, screenB);
    if (tEnd) {
      viewer.transformPoint(pointBeforeEnd, screenC);
      int capDiameter =
        viewer.scaleToScreen(screenC.z, madPending + (madPending >> 2));
      g3d.fillCone(colixPending, Graphics3D.ENDCAPS_FLAT, capDiameter,
                   screenC, screenB);
      if (startIndexPending == endIndexPending)
        return;
      Point3i t = screenB;
      screenB = screenC;
      screenC = t;
    }
    int zMid = (screenA.z + screenB.z) / 2;
    int diameter = viewer.scaleToScreen(zMid, madPending);
    g3d.fillCylinder(colixPending, Graphics3D.ENDCAPS_FLAT, diameter,
                     screenA, screenB);
  }

  void renderPendingSheet(Point3f pointStart, Point3f pointBeforeEnd,
                          Point3f pointEnd, boolean tEnd) {
    shadesPending = g3d.getShades(colixPending);
    if (tEnd) {
      drawArrowHeadBox(pointBeforeEnd, pointEnd);
      drawBox(pointStart, pointBeforeEnd);
    } else {
      drawBox(pointStart, pointEnd);
    }
  }

  final Point3f pointTipOffset = new Point3f();
  final Point3f pointArrow2 = new Point3f();
  final Vector3f vectorNormal = new Vector3f();

  final Vector3f scaledWidthVector = new Vector3f();
  final Vector3f scaledHeightVector = new Vector3f();

  final static byte arrowHeadFaces[] =
  {0, 1, 3, 2,
   0, 4, 5, 2,
   1, 4, 5, 3};

  void drawArrowHeadBox(Point3f base, Point3f tip) {
    Sheet sheet = (Sheet)proteinstructurePending;
    float scale = madPending / 1000f;
    scaledWidthVector.set(sheet.getWidthUnitVector());
    scaledWidthVector.scale(scale * 1.25f);
    scaledHeightVector.set(sheet.getHeightUnitVector());
    scaledHeightVector.scale(scale / 3);
    pointCorner.add(scaledWidthVector, scaledHeightVector);
    pointCorner.scaleAdd(-0.5f, base);
    pointTipOffset.set(scaledHeightVector);
    pointTipOffset.scaleAdd(-0.5f, tip);
    buildArrowHeadBox(pointCorner, scaledWidthVector,
                      scaledHeightVector, pointTipOffset);
    int argb = calcSurfaceArgb(0, 1, 4);
    g3d.fillTriangle(colixPending,
                     screenCorners[0],
                     screenCorners[1],
                     screenCorners[4]);
    g3d.fillTriangle(colixPending,
                     screenCorners[2],
                     screenCorners[3],
                     screenCorners[5]);
    for (int i = 0; i < 12; i += 4) {
      int i0 = arrowHeadFaces[i];
      int i1 = arrowHeadFaces[i + 1];
      int i2 = arrowHeadFaces[i + 2];
      int i3 = arrowHeadFaces[i + 3];
      argb = calcSurfaceArgb(i0, i1, i3);
      g3d.fillQuadrilateral(colixPending,
                            screenCorners[i0],
                            screenCorners[i1],
                            screenCorners[i2],
                            screenCorners[i3]);
    }
    /*
    Sheet sheet = (Sheet)structurePending;
    Vector3f widthUnitVector = sheet.getWidthUnitVector();
    Vector3f heightUnitVector = sheet.getHeightUnitVector();
    float widthScale = (madPending + madPending >> 2) / 2 / 1000f;

    pointArrow1.set(widthUnitVector);
    pointArrow1.scaleAdd(-widthScale, base);
    viewer.transformPoint(pointArrow1, screenA);

    pointArrow2.set(widthUnitVector);
    pointArrow2.scaleAdd(widthScale, base);
    viewer.transformPoint(pointArrow2, screenB);

    viewer.transformPoint(tip, screenC);

    viewer.transformVector(heightUnitVector, vectorNormal);

    g3d.drawfillTriangle(colixPending, vectorNormal,
                         screenA, screenB, screenC);
    */
  }

  final Vector3f lengthVector = new Vector3f();
  final Point3f pointCorner = new Point3f();

  void drawBox(Point3f pointA, Point3f pointB) {
    Sheet sheet = (Sheet)proteinstructurePending;
    float scale = madPending / 1000f;
    scaledWidthVector.set(sheet.getWidthUnitVector());
    scaledWidthVector.scale(scale);
    scaledHeightVector.set(sheet.getHeightUnitVector());
    scaledHeightVector.scale(scale / 4);
    pointCorner.add(scaledWidthVector, scaledHeightVector);
    pointCorner.scaleAdd(-0.5f, pointA);
    lengthVector.sub(pointB, pointA);
    buildBox(pointCorner, scaledWidthVector,
             scaledHeightVector, lengthVector);
    for (int i = 0; i < 6; ++i) {
      int i0 = boxFaces[i * 4];
      int i1 = boxFaces[i * 4 + 1];
      int i2 = boxFaces[i * 4 + 2];
      int i3 = boxFaces[i * 4 + 3];
      int argb = calcSurfaceArgb(i0, i1, i3);
      g3d.fillQuadrilateral(colixPending,
                            screenCorners[i0],
                            screenCorners[i1],
                            screenCorners[i2],
                            screenCorners[i3]);
    }
  }

  int calcSurfaceArgb(int iA, int iB, int iC) {
    return shadesPending[viewer.calcSurfaceIntensity(corners[iA],
                                                     corners[iB],
                                                     corners[iC])];
  }

  final static byte[] boxFaces =
  {
    0, 1, 3, 2,
    0, 2, 6, 4,
    0, 4, 5, 1,
    7, 5, 4, 6,
    7, 6, 2, 3,
    7, 3, 1, 5 };

  final Point3f[] corners = new Point3f[8];
  final Point3f[] screenCorners = new Point3f[8];
  {
    for (int i = 8; --i >= 0; ) {
      screenCorners[i] = new Point3f();
      corners[i] = new Point3f();
    }
  }

  void buildBox(Point3f pointCorner, Vector3f scaledWidthVector,
                Vector3f scaledHeightVector, Vector3f lengthVector) {
    for (int i = 8; --i >= 0; ) {
      Point3f corner = corners[i];
      corner.set(pointCorner);
      if ((i & 1) != 0)
        corner.add(scaledWidthVector);
      if ((i & 2) != 0)
        corner.add(scaledHeightVector);
      if ((i & 4) != 0)
        corner.add(lengthVector);
      viewer.transformPoint(corner, screenCorners[i]);
    }
  }

  void buildArrowHeadBox(Point3f pointCorner, Vector3f scaledWidthVector,
                         Vector3f scaledHeightVector, Point3f pointTip) {
    for (int i = 4; --i >= 0; ) {
      Point3f corner = corners[i];
      corner.set(pointCorner);
      if ((i & 1) != 0)
        corner.add(scaledWidthVector);
      if ((i & 2) != 0)
        corner.add(scaledHeightVector);
      viewer.transformPoint(corner, screenCorners[i]);
    }
    corners[4].set(pointTip);
    viewer.transformPoint(pointTip, screenCorners[4]);
    corners[5].add(pointTip, scaledHeightVector);
    viewer.transformPoint(corners[5], screenCorners[5]);
  }
}
