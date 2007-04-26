/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shapebio;

import org.jmol.g3d.Graphics3D;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class RocketsRenderer extends MpsRenderer {

  final static float MIN_CONE_HEIGHT = 0.05f;

  void renderMpspolymer(Mps.MpsShape mpspolymer) {
    Rockets.Cchain cchain = (Rockets.Cchain)mpspolymer;
    if (!(cchain.polymer instanceof AminoPolymer))
      return;
    calcRopeMidPoints(false);    
    calcScreenControlPoints(cordMidPoints);
    controlPoints = cordMidPoints;
    render1();
    viewer.freeTempPoints(cordMidPoints);
  }

  Point3f[] cordMidPoints;

  void calcRopeMidPoints(boolean isNewStyle) {
    int midPointCount = monomerCount + 1;
    cordMidPoints = viewer.allocTempPoints(midPointCount);
    ProteinStructure proteinstructurePrev = null;
    Point3f point;
    for (int i = 0; i < monomerCount; ++i) {
      point = cordMidPoints[i];
      Monomer residue = monomers[i];
      if (isNewStyle) {
        point.set(controlPoints[i]);        
      } else if (isHelix(i) || isSheet(i)) {
        ProteinStructure proteinstructure = residue.getProteinStructure();
        point.set(i - 1 != proteinstructure.getMonomerIndex() ?
            proteinstructure.getAxisStartPoint() :
            proteinstructure.getAxisEndPoint());
        proteinstructurePrev = proteinstructure;
      } else {
        if (proteinstructurePrev != null)
          point.set(proteinstructurePrev.getAxisEndPoint());
        else {
          point.set(controlPoints[i]);        
        }
        proteinstructurePrev = null;
      }
    }
    point = cordMidPoints[monomerCount];
    if (proteinstructurePrev != null)
      point.set(proteinstructurePrev.getAxisEndPoint());
    else {
      point.set(controlPoints[monomerCount]);        
    }
  }

  void render1() {
    tPending = false;
    for (int i = 0; i < monomerCount; ++i)
      if (bsVisible.get(i)) {
        Monomer monomer = monomers[i];
        if (isHelix(i) || isSheet(i)) {
          renderSpecialSegment(monomer, getLeadColix(i), mads[i]);
        } else {
          renderHermiteConic(i, true);
        }
      }
    renderPending();
  }

  void renderSpecialSegment(Monomer monomer, short colix, short mad) {
    ProteinStructure proteinstructure = monomer.getProteinStructure();
    if (tPending) {
      if (proteinstructure == proteinstructurePending && mad == madPending
          && colix == colixPending
          && proteinstructure.getIndex(monomer) == endIndexPending + 1) {
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

  void renderPending() {
    if (!tPending)
      return;
    Point3f[] segments = proteinstructurePending.getSegments();
    boolean tEnd = (endIndexPending == proteinstructurePending
        .getMonomerCount() - 1);
    if (proteinstructurePending instanceof Helix)
      renderPendingRocketSegment(endIndexPending, segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1], tEnd);
    else if (proteinstructurePending instanceof Sheet)
      renderPendingSheet(segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1], tEnd);
    tPending = false;
  }

  Point3f screenA = new Point3f();
  Point3f screenB = new Point3f();
  Point3f screenC = new Point3f();

  void renderPendingRocketSegment(int i, Point3f pointStart,
                                  Point3f pointBeforeEnd, Point3f pointEnd,
                                  boolean tEnd) {
    viewer.transformPoint(pointStart, screenA);
    viewer.transformPoint(pointEnd, screenB);
    int zMid = (int) Math.floor((screenA.z + screenB.z) / 2f);
    int diameter = viewer.scaleToScreen(zMid, madPending);
    if (tEnd) {
      viewer.transformPoint(pointBeforeEnd, screenC);
      if (g3d.setColix(colixPending)) {
        if (pointBeforeEnd.distance(pointEnd) > MIN_CONE_HEIGHT)
          renderCone(i, pointBeforeEnd, pointEnd, screenC, screenB, madPending,
              colixPending);
        else
          g3d.fillCylinderBits(Graphics3D.ENDCAPS_FLAT, diameter, screenB,
              screenC);
      }
      if (startIndexPending == endIndexPending)
        return;
      Point3f t = screenB;
      screenB = screenC;
      screenC = t;
    }
    if (g3d.setColix(colixPending))
      g3d.fillCylinderBits(Graphics3D.ENDCAPS_FLAT, diameter, screenA, screenB);
  }

  void renderPendingSheet(Point3f pointStart, Point3f pointBeforeEnd,
                          Point3f pointEnd, boolean tEnd) {
    if (!g3d.setColix(colixPending))
      return;
    if (tEnd) {
      drawArrowHeadBox(pointBeforeEnd, pointEnd);
      drawBox(pointStart, pointBeforeEnd);
    } else {
      drawBox(pointStart, pointEnd);
    }
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

  final Point3f pointTipOffset = new Point3f();
  final Point3f pointArrow2 = new Point3f();
  final Vector3f vectorNormal = new Vector3f();

  final Vector3f scaledWidthVector = new Vector3f();
  final Vector3f scaledHeightVector = new Vector3f();

  final static byte arrowHeadFaces[] =
  {0, 1, 3, 2,
   0, 4, 5, 2,
   1, 4, 5, 3};

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
      if (g3d.setColix(colixPending))
        g3d.fillQuadrilateral(screenCorners[i0],
                              screenCorners[i1],
                              screenCorners[i2],
                              screenCorners[i3]);
    }
  }

  void drawArrowHeadBox(Point3f base, Point3f tip) {
    if (!g3d.setColix(colixPending))
      return;
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
    g3d.fillTriangle(screenCorners[0],
                     screenCorners[1],
                     screenCorners[4]);
    g3d.fillTriangle(screenCorners[2],
                     screenCorners[3],
                     screenCorners[5]);
    for (int i = 0; i < 12; i += 4) {
      int i0 = arrowHeadFaces[i];
      int i1 = arrowHeadFaces[i + 1];
      int i2 = arrowHeadFaces[i + 2];
      int i3 = arrowHeadFaces[i + 3];
      g3d.fillQuadrilateral(screenCorners[i0],
                              screenCorners[i1],
                              screenCorners[i2],
                              screenCorners[i3]);
    }
  }
}
