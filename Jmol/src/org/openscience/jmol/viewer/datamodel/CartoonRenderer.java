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

class CartoonRenderer extends McpsRenderer {

  Point3i[] getTempScreens(int minLen) {
    Point3i[] screens = new Point3i[minLen];
    for (int i = minLen; --i >= 0; )
      screens[i] = new Point3i();
    return screens;
  }

  final Point3f pointT = new Point3f();

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                        short[] mads, float offsetFraction) {
    int count = centers.length;
    Point3i[] screens = getTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0; )
        viewer.transformPoint(centers[i], screens[i]);
    } else {
      offsetFraction /= 1000;
      for (int i = count; --i >= 0; ) {
        pointT.set(vectors[i]);
        short mad = isSpecials[i] || i == 0 ? mads[i] : mads[i - 1];
        float scale = mad * offsetFraction;
        pointT.scaleAdd(scale, centers[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  void calcScreenLeadMidpoints(Point3f[] leadMidpoints) {
    int count = polymerCount + 1;
    leadMidpointScreens = frameRenderer.getTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(leadMidpoints[i], leadMidpointScreens[i]);
      //g3d.fillSphereCentered(Graphics3D.CYAN, 15, leadMidpointScreens[i]);
    }
  }

  boolean isNucleotidePolymer;
  int polymerCount;
  Group[] polymerGroups;
  Point3i[] leadMidpointScreens;

  void renderMcpschain( Mcps.Mcpschain mcpsChain) {
    Cartoon.Cchain strandsChain = (Cartoon.Cchain)mcpsChain;
    if (strandsChain.wingVectors != null) {
      polymerCount = strandsChain.polymerCount;
      polymerGroups = strandsChain.polymerGroups;
      isSpecials = calcIsSpecials(polymerGroups, polymerCount);
      isNucleotidePolymer = strandsChain.polymer instanceof NucleotidePolymer;
      calcScreenLeadMidpoints(strandsChain.leadMidpoints);
      render1Chain(polymerCount,
                   polymerGroups,
                   strandsChain.leadMidpoints,
                   strandsChain.wingVectors,
                   strandsChain.mads,
                   strandsChain.colixes);
    }
  }


  boolean[] isSpecials;

  boolean[]  calcIsSpecials(Group[] polymerGroups, int polymerCount) {
    boolean[] isSpecials = frameRenderer.getTempBooleans(polymerCount);
    for (int i = polymerCount; --i >= 0; )
      isSpecials[i] = polymerGroups[i].isHelixOrSheet();
    return isSpecials;
  }


  void render1Chain(int polymerCount,
                    Group[] groups, Point3f[] centers,
                    Vector3f[] vectors, short[] mads, short[] colixes) {
    Point3i[] screensTop;
    Point3i[] screensBottom;

    screensTop = calcScreens(centers, vectors, mads,
                             isNucleotidePolymer ? 1f : 0.5f);
    screensBottom = calcScreens(centers, vectors, mads,
                                isNucleotidePolymer ? 0f : -0.5f);
    for (int i = polymerCount; --i >= 0; )
      if (mads[i] > 0) {
        Group group = groups[i];
        short colix = colixes[i];
        if (colix == 0)
          colix = group.getLeadAtom().colixAtom;
        if (isSpecials[i])
          render2StrandSegment(polymerCount,
                               group, colix, mads,
                               screensTop, screensBottom, i);
        else
          renderRopeSegment(colix, mads, i);
      }
  }
  
  void render2StrandSegment(int polymerCount, Group group, short colix,
                            short[] mads, Point3i[] screensTop,
                            Point3i[] screensBottom, int i) {
    int iLast = polymerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getLeadAtom().colixAtom;
    
    //change false -> true to fill in mesh
    g3d.drawHermite(true, colix, isNucleotidePolymer ? 4 : 7,
                    screensTop[iPrev], screensTop[i],
                    screensTop[iNext], screensTop[iNext2],
                    screensBottom[iPrev], screensBottom[i],
                    screensBottom[iNext], screensBottom[iNext2]
                    );
  }

  void renderRopeSegment(short colix, short[] mads, int i) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > polymerCount) iNext1 = polymerCount;
    int iNext2 = i + 2; if (iNext2 > polymerCount) iNext2 = polymerCount;
    
    int madThis, madBeg, madEnd;
    madThis = madBeg = madEnd = mads[i];
    if (! isSpecials[iPrev1])
      madBeg = (mads[iPrev1] + madThis) / 2;
    if (! isSpecials[iNext1])
      madEnd = (mads[iNext1] + madThis) / 2;
    int diameterBeg = viewer.scaleToScreen(leadMidpointScreens[i].z, madBeg);
    int diameterEnd = viewer.scaleToScreen(leadMidpointScreens[iNext1].z, madEnd);
    int diameterMid =
      viewer.scaleToScreen(polymerGroups[i].getLeadAtom().getScreenZ(),
                           madThis);
    g3d.fillHermite(colix, 4,
                    diameterBeg, diameterMid, diameterEnd,
                    leadMidpointScreens[iPrev1], leadMidpointScreens[i],
                    leadMidpointScreens[iNext1], leadMidpointScreens[iNext2]);
//    System.out.println("render1Segment: iPrev1=" + iPrev1 +
//                       " i=" + i + " iNext1=" + iNext1 + " iNext2=" + iNext2 +
//                       " leadMidpointScreens[i]=" + leadMidpointScreens[i] + " colix=" + colix +
//                       " mads[i]=" + mads[i]);
  }



  /*

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();
  int diameterBeg, diameterMid, diameterEnd;

  Cartoon cartoon;

  void renderMcpschain(Mcps.Mcpschain mcpschain) {
    Cartoon.Cchain cchain = (Cartoon.Cchain)mcpschain;
    render1Chain(cchain.polymer, cchain.mads, cchain.colixes);
  }

  void render1Chain(Polymer polymer, short[] mads, short[] colixes) {
    if (! (polymer instanceof AminoPolymer))
      return;
    initializeChain((AminoPolymer)polymer);
    clearPending();
    for (int i = 0; i < polymerCount; ++i) {
      if (mads[i] == 0)
        continue;
      short colix = colixes[i];
      if (colix == 0)
        colix = alphas[i].colixAtom;
      Group group = polymerGroups[i];
      if (group.isHelixOrSheet()) {
        //        System.out.println("renderSpecialSegment[" + i + "]");
        renderSpecialSegment(group, colix, mads[i]);
      } else {
        //        System.out.println("renderRopeSegment[" + i + "]");
        renderRopeSegment(colix, mads, i);
      }
    }
    renderPending();
  }

  int polymerCount;
  Group[] polymerGroups;
  Point3i[] leadMidpointScreens;
  Atom[] alphas;

  void initializeChain(AminoPolymer aminopolymer) {
    polymerGroups = aminopolymer.getGroups();
    polymerCount = aminopolymer.getCount();
    calcIsSpecials();
    calcScreenLeadMidpoints(aminopolymer.leadMidpoints);
    alphas = getAlphas();
  }

  void calcScreenLeadMidpoints(Point3f[] leadMidpoints) {
    int count = polymerCount + 1;
    leadMidpointScreens = frameRenderer.getTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(leadMidpoints[i], leadMidpointScreens[i]);
      g3d.fillSphereCentered(Graphics3D.CYAN, 15, leadMidpointScreens[i]);
    }
  }

  Atom[] getAlphas() {
    Atom[] alphas = frameRenderer.getTempAtoms(polymerCount);
    for (int i = polymerCount; --i >= 0; )
      alphas[i] = polymerGroups[i].getAlphaCarbonAtom();
    return alphas;
  }

  void renderRopeSegment(short colix, short[] mads, int i) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > polymerCount) iNext1 = polymerCount;
    int iNext2 = i + 2; if (iNext2 > polymerCount) iNext2 = polymerCount;
    
    int madThis, madBeg, madEnd;
    madThis = madBeg = madEnd = mads[i];
    if (! isSpecials[iPrev1])
      madBeg = (mads[iPrev1] + madThis) / 2;
    if (! isSpecials[iNext1])
      madEnd = (mads[iNext1] + madThis) / 2;
    int diameterBeg = viewer.scaleToScreen(leadMidpointScreens[i].z, madBeg);
    int diameterEnd = viewer.scaleToScreen(leadMidpointScreens[iNext1].z, madEnd);
    int diameterMid = viewer.scaleToScreen(alphas[i].getScreenZ(), madThis);
    g3d.fillHermite(colix, 3, diameterBeg, diameterMid, diameterEnd,
                    leadMidpointScreens[iPrev1], leadMidpointScreens[i],
                    leadMidpointScreens[iNext1], leadMidpointScreens[iNext2]);
//    System.out.println("render1Segment: iPrev1=" + iPrev1 +
//                       " i=" + i + " iNext1=" + iNext1 + " iNext2=" + iNext2 +
//                       " leadMidpointScreens[i]=" + leadMidpointScreens[i] + " colix=" + colix +
//                       " mads[i]=" + mads[i]);
  }

  final Point3i screenA = new Point3i();
  Point3i screenB = new Point3i();
  Point3i screenC = new Point3i();

  void renderSpecialSegment(Group group, short colix, short mad) {
    AminoStructure aminostructure = group.aminostructure;
    if (tPending) {
      if (aminostructure == aminostructurePending &&
          mad == madPending &&
          colix == colixPending &&
          aminostructure.getIndex(group) == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPending();
    }
    aminostructurePending = aminostructure;
    startIndexPending = endIndexPending = aminostructure.getIndex(group);
    colixPending = colix;
    madPending = mad;
    tPending = true;
  }

  boolean tPending;
  AminoStructure aminostructurePending;
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
      Point3f[] segments = aminostructurePending.getSegments();
      boolean tEnd =
        (endIndexPending == aminostructurePending.getPolymerCount() - 1);

//
//      System.out.println("structurePending.getPolymerCount()=" +
//                         structurePending.getPolymerCount());
//      System.out.println("segments.length=" + segments.length);
//      System.out.println(" startIndexPending=" + startIndexPending +
//                         " endIndexPending=" + endIndexPending);
//      System.out.println("tEnd=" + tEnd);
      if (aminostructurePending instanceof Helix)
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
    Sheet sheet = (Sheet)aminostructurePending;
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
//    Sheet sheet = (Sheet)structurePending;
//    Vector3f widthUnitVector = sheet.getWidthUnitVector();
//    Vector3f heightUnitVector = sheet.getHeightUnitVector();
//    float widthScale = (madPending + madPending >> 2) / 2 / 1000f;

//    pointArrow1.set(widthUnitVector);
//    pointArrow1.scaleAdd(-widthScale, base);
//    viewer.transformPoint(pointArrow1, screenA);

//    pointArrow2.set(widthUnitVector);
//    pointArrow2.scaleAdd(widthScale, base);
//    viewer.transformPoint(pointArrow2, screenB);

//    viewer.transformPoint(tip, screenC);

//    viewer.transformVector(heightUnitVector, vectorNormal);

//    g3d.drawfillTriangle(colixPending, vectorNormal,
//                         screenA, screenB, screenC);
  }

  final Vector3f lengthVector = new Vector3f();
  final Point3f pointCorner = new Point3f();

  void drawBox(Point3f pointA, Point3f pointB) {
    Sheet sheet = (Sheet)aminostructurePending;
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
  */
}
