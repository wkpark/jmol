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
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.protein.*;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

class CartoonRenderer extends Renderer {

  CartoonRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    this.viewer = viewer;
    this.frameRenderer = frameRenderer;
  }

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Cartoon cartoon;

  void render() {
    this.cartoon = frame.cartoon;

    if (cartoon == null || !cartoon.initialized)
      return;
    PdbMolecule pdbMolecule = cartoon.pdbMolecule;
    short[][] madsChains = cartoon.madsChains;
    short[][] colixesChains = cartoon.colixesChains;
    for (int i = cartoon.chainCount; --i >= 0; )
      render1Chain(pdbMolecule.getChain(i),
                   madsChains[i], colixesChains[i]);
  }
  
  int residueCount;

  void render1Chain(PdbChain pdbchain,
                    short[] mads, short[] colixes) {
    residueCount = pdbchain.getResidueCount();
    Point3i[] screens = getScreens(pdbchain);
    Atom[] alphas = getAlphas(pdbchain);
    for (int i = pdbchain.getResidueCount(); --i >= 0; ) {
      short colix = colixes[i];
      if (colix == 0)
        colix = alphas[i].colixAtom;
      render1Segment(screens, alphas, colix, mads, i);
    }
  }

  Point3f[] calcStructureMidPoints(PdbChain pdbchain) {
    int count = residueCount + 1;
    Point3f[] structureMidPoints = frameRenderer.getTempPoints(count);
    for (int i = count; --i >= 0; )
      pdbchain.getStructureMidPoint(i, structureMidPoints[i]);
    return structureMidPoints;
  }

  Point3i[] getScreens(PdbChain pdbchain) {
    int count = residueCount + 1;
    Point3f[] structureMidPoints = calcStructureMidPoints(pdbchain);
    Point3i[] screens = frameRenderer.getTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(structureMidPoints[i], screens[i]);
      g3d.fillSphereCentered(Colix.CYAN, 15, screens[i]);
    }
    return screens;
  }

  Atom[] getAlphas(PdbChain pdbchain) {
    Atom[] alphas = frameRenderer.getTempAtoms(residueCount);
    PdbResidue[] residues = pdbchain.getMainchain();
    for (int i = residueCount; --i >= 0; )
      alphas[i] = residues[i].getAlphaCarbonAtom();
    return alphas;
  }

  void render1Segment(Point3i[] screens, Atom[] alphas,
                      short colix, short[] mads, int i) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > residueCount) iNext1 = residueCount;
    int iNext2 = i + 2; if (iNext2 > residueCount) iNext2 = residueCount;
    
    int madThis = mads[i];
    int madBeg = (mads[iPrev1] + madThis) / 2;
    int diameterBeg = viewer.scaleToScreen(screens[i].z, madBeg);
    int madEnd = (mads[iNext1] + madThis) / 2;
    int diameterEnd = viewer.scaleToScreen(screens[iNext1].z, madEnd);
    int diameterMid = viewer.scaleToScreen(alphas[i].z, madThis);
    g3d.fillHermite(colix, diameterBeg, diameterMid, diameterEnd,
                    screens[iPrev1], screens[i],
                    screens[iNext1], screens[iNext2]);
    /*
    System.out.println("render1Segment: iPrev1=" + iPrev1 +
                       " i=" + i + " iNext1=" + iNext1 + " iNext2=" + iNext2 +
                       " screens[i]=" + screens[i] + " colix=" + colix +
                       " mads[i]=" + mads[i]);
    */
  }

  int mainchainLast;

  void render1ChainX(PdbResidue[] mainchain, short[] mads, short[] colixes) {
    mainchainLast = mainchain.length - 1;
    for (int i = 0; i < mainchain.length; ++i) {
      PdbResidue residue = mainchain[i];
      short colix = colixes[i];
      if (colix == 0)
        colix = residue.getAlphaCarbonAtom().colixAtom;
      if (residue.isHelixOrSheet()) {
        renderSpecialSegment(residue, i, colix, mads[i]);
      } else {
        calcSegmentPoints(mainchain, i, mads);
        render1Segment(colix);
      }
    }
    renderPending();
  }

  void calcSegmentPoints(PdbResidue[] mainchain, int i, short[] mads) {
    int iPrev1 = i - 1, iPrev2 = i - 2, iNext1 = i + 1, iNext2 = i + 2;
    if (iPrev1 < 0)
      iPrev1 = 0;
    if (iPrev2 < 0)
      iPrev2 = 0;
    if (iNext1 > mainchainLast)
      iNext1 = mainchainLast;
    if (iNext2 > mainchainLast)
      iNext2 = mainchainLast;

    if (mainchain[iPrev2].isHelixOrSheet()) {
      viewer.transformPoint(mainchain[iPrev2].structure.getAxisEndPoint(), s0);
      System.out.println("iPrev2 isHelixOrSheet");
    }
    else
      calcAverage(mainchain, iPrev2, iPrev1, s0);

    if (mainchain[iPrev1].isHelixOrSheet()) {
      viewer.transformPoint(mainchain[iPrev1].structure.getAxisEndPoint(), s1);
      System.out.println("iPrev1 isHelixOrSheet");
    }
    else
      calcAverage(mainchain, iPrev1, i, s1);

    if (mainchain[iNext1].isHelixOrSheet()) {
      viewer.transformPoint(mainchain[iNext1].structure.getAxisStartPoint(),
                            s2);
      System.out.println("iNext1 isHelixOrSheet");
    }
    else
      calcAverage(mainchain, i, iNext1, s2);

    if (mainchain[iNext2].isHelixOrSheet()) {
      System.out.println("iNext2 isHelixOrSheet");
      Point3f point = mainchain[iNext2].structure.getAxisStartPoint();
      System.out.println("point=" + point);
      viewer.transformPoint(point,
                            s3);
    }
    else
      calcAverage(mainchain, iNext1, iNext2, s3);

    int madBeg, madMid, madEnd;
    madBeg = madMid = madEnd = mads[i];
    if (! mainchain[iPrev1].isHelixOrSheet())
      madBeg = (mads[iPrev1] + madBeg) / 2;
    if (! mainchain[iNext1].isHelixOrSheet())
      madEnd = (mads[iNext1] + madEnd) / 2;
    diameterBeg = viewer.scaleToScreen(s1.z, madBeg);
    diameterMid = viewer.scaleToScreen(mainchain[i].getAlphaCarbonAtom().z,
                                       madMid);
    diameterEnd = viewer.scaleToScreen(s2.z, madEnd);
  }

  void calcAverage(PdbResidue[] mainchain, int iA, int iB, Point3i dest) {
    Atom atomA = mainchain[iA].getAlphaCarbonAtom();
    Atom atomB = mainchain[iB].getAlphaCarbonAtom();
    dest.x = (atomA.x + atomB.x) / 2;
    dest.y = (atomA.y + atomB.y) / 2;
    dest.z = (atomA.z + atomB.z) / 2;
  }

  void render1Segment(short colix) {
    g3d.fillHermite(colix, diameterBeg, diameterMid, diameterEnd,
                    s0, s1, s2, s3);
  }

  final Point3i screenA = new Point3i();
  Point3i screenB = new Point3i();
  Point3i screenC = new Point3i();

  void renderSpecialSegment(PdbResidue residue, int residueIndex,
                            short colix, short mad) {
    PdbStructure structure = residue.structure;
    if (tPending) {
      if (structure == structurePending &&
          mad == madPending &&
          colix == colixPending &&
          residueIndex == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPending();
    }
    structurePending = structure;
    startIndexPending = endIndexPending = residueIndex;
    colixPending = colix;
    madPending = mad;
    tPending = true;
  }

  boolean tPending;
  PdbStructure structurePending;
  int startIndexPending;
  int endIndexPending;
  short madPending;
  short colixPending;

  void renderPending() {
    if (tPending) {
      Point3f[] segments = structurePending.getSegments();
      int indexStructureStart = structurePending.getStartResidueIndex();
      int indexStart = startIndexPending - indexStructureStart;
      int indexEnd = endIndexPending - indexStructureStart;
      boolean tEnd = (indexEnd == structurePending.getResidueCount() - 1);
      if (structurePending instanceof Helix)
        renderPendingHelix(segments[indexStart],
                           segments[indexEnd - 1],
                           segments[indexEnd],
                           tEnd);
      else
        renderPendingSheet(segments[indexStart],
                           segments[indexEnd - 1],
                           segments[indexEnd],
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
    Sheet sheet = (Sheet)structurePending;
    float scale = madPending / 1000f;
    scaledWidthVector.set(sheet.getWidthUnitVector());
    scaledWidthVector.scale(scale * 1.25f);
    scaledHeightVector.set(sheet.getHeightUnitVector());
    scaledHeightVector.scale(scale / 4);
    pointCorner.add(scaledWidthVector, scaledHeightVector);
    pointCorner.scaleAdd(-0.5f, base);
    pointTipOffset.set(scaledHeightVector);
    pointTipOffset.scaleAdd(-0.5f, tip);
    buildArrowHeadBox(pointCorner, scaledWidthVector,
                      scaledHeightVector, pointTipOffset);
    g3d.fillTriangle(colixPending,
                     screenCorners[0],
                     screenCorners[1],
                     screenCorners[4]);
    g3d.fillTriangle(colixPending,
                     screenCorners[2],
                     screenCorners[3],
                     screenCorners[5]);
    for (int i = 0; i < 12; i += 4)
      g3d.fillQuadrilateral(colixPending,
                            screenCorners[arrowHeadFaces[i]],
                            screenCorners[arrowHeadFaces[i + 1]],
                            screenCorners[arrowHeadFaces[i + 2]],
                            screenCorners[arrowHeadFaces[i + 3]]);
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
    Sheet sheet = (Sheet)structurePending;
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
    for (int i = 0; i < 6; ++i)
      g3d.fillQuadrilateral(colixPending,
                            screenCorners[boxFaces[i * 4]],
                            screenCorners[boxFaces[i * 4 + 1]],
                            screenCorners[boxFaces[i * 4 + 2]],
                            screenCorners[boxFaces[i * 4 + 3]]);
  }

  final static byte[] boxFaces =
  {
    0, 1, 3, 2,
    0, 2, 6, 4,
    0, 4, 5, 1,
    7, 5, 4, 6,
    7, 6, 2, 3,
    7, 3, 2, 6 };

  final Point3f[] corners = new Point3f[8];
  final Point3i[] screenCorners = new Point3i[8];
  {
    for (int i = 8; --i >= 0; ) {
      screenCorners[i] = new Point3i();
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
