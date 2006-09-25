/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

class CartoonRenderer extends MpsRenderer {

  /*
   * This is now an unhappy marriage of cartoons and rockets.
   * I realize it's a hack. For now it will have to do.
   * 
   * Bob Hanson 8/2006
   * 
   */
  final Point3f pointT = new Point3f();
  final static float MIN_CONE_HEIGHT = 0.05f;
  boolean newRockets;

  void calc1Screen(Point3f center, Vector3f vector,
                   short mad, float offsetFraction, Point3i screen) {
    pointT.set(vector);
    float scale = mad * offsetFraction;
    pointT.scaleAdd(scale, center);
    viewer.transformPoint(pointT, screen);
  }

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                        short[] mads, float offsetFraction) {
    int count = centers.length;
    Point3i[] screens = viewer.allocTempScreens(count);
    if (offsetFraction == 0) {
      for (int i = count; --i >= 0; )
        viewer.transformPoint(centers[i], screens[i]);
    } else {
      for (int i = count; --i >= 0; ) {
        pointT.set(vectors[i]);
        short mad = mads[i];
        float scale = mad * offsetFraction;
        pointT.scaleAdd(scale, centers[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  boolean isNucleicPolymer;
  boolean isCarbohydratePolymer;
  boolean ribbonBorder = false;
  int monomerCount;
  Monomer[] monomers;
  Point3f[] leadMidpoints;
  Vector3f[] wingVectors;
  short[] mads;
  short[] colixes;

  boolean[] isSpecials;
  boolean[] isHelixes;
  Point3i[] leadMidpointScreens;
  Point3i[] ribbonTopScreens;
  Point3i[] ribbonBottomScreens;

  int myVisibilityFlag;
  boolean renderAsRockets;
  void renderMpspolymer( Mps.Mpspolymer mpspolymer, int myVisibilityFlag) {
    Cartoon.Cchain strandsChain = (Cartoon.Cchain)mpspolymer;
    this.myVisibilityFlag = myVisibilityFlag;
    newRockets = !viewer.getTestFlag1();
    renderAsRockets = viewer.getCartoonRocketFlag();
    
    if (strandsChain.wingVectors != null) {
      monomerCount = strandsChain.monomerCount;
      monomers = strandsChain.monomers;
      leadMidpoints = strandsChain.leadMidpoints;
      wingVectors = strandsChain.wingVectors;
      mads = strandsChain.mads;
      colixes = strandsChain.colixes;
      ribbonBorder = viewer.getRibbonBorder();
      if (!initializeChain(strandsChain.polymer))
        return;
      render1Chain();
    }
  }

  Point3i[] screens;
  Point3f[] screensf;
  Point3f[] cordMidPoints;

  boolean initializeChain(Polymer polymer) {
    isNucleicPolymer = polymer instanceof NucleicPolymer;
    isCarbohydratePolymer = polymer instanceof CarbohydratePolymer;
    if (isCarbohydratePolymer)
      return false;
    isSpecials = calcIsSpecials(monomerCount, monomers);
    isHelixes = calcIsHelix(monomerCount, monomers);
    leadMidpointScreens = calcScreenLeadMidpoints(monomerCount, leadMidpoints);
    ribbonTopScreens = calcScreens(leadMidpoints, wingVectors, mads,
        isNucleicPolymer ? 1f / 1000 : 0.5f / 1000);
    ribbonBottomScreens = calcScreens(leadMidpoints, wingVectors, mads,
        isNucleicPolymer ? 0f : -0.5f / 1000);
    if (!isNucleicPolymer)
      calcRopeMidPoints(polymer, newRockets);
    getScreens();
    clearPending();
    return true;
  }

  void calcRopeMidPoints(Polymer polymer, boolean isNewStyle) {
    int midPointCount = monomerCount + 1;
    cordMidPoints = viewer.allocTempPoints(midPointCount);
    ProteinStructure proteinstructurePrev = null;
    Point3f point;
    for (int i = 0; i < monomerCount; ++i) {
      point = cordMidPoints[i];
      Monomer residue = monomers[i];
      if (isNewStyle) {
        polymer.getLeadMidPoint(i, point);        
      } else if (isSpecials[i]) {
        ProteinStructure proteinstructure = residue.getProteinStructure();
        point.set(i - 1 != proteinstructure.getMonomerIndex() ?
            proteinstructure.getAxisStartPoint() :
            proteinstructure.getAxisEndPoint());
        proteinstructurePrev = proteinstructure;
      } else {
        if (proteinstructurePrev != null)
          point.set(proteinstructurePrev.getAxisEndPoint());
        else
          polymer.getLeadMidPoint(i, point);
        proteinstructurePrev = null;
      }
    }
    point = cordMidPoints[monomerCount];
    if (proteinstructurePrev != null)
      point.set(proteinstructurePrev.getAxisEndPoint());
    else
      polymer.getLeadMidPoint(monomerCount, point);
  }

  void getScreens() {
    int count = monomerCount + 1;
    screens = viewer.allocTempScreens(count);
    screensf = viewer.allocTempPoints(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(cordMidPoints[i], screensf[i]);
      screens[i].x = (int)Math.floor(screensf[i].x);
      screens[i].y = (int)Math.floor(screensf[i].y);
      screens[i].z = (int)Math.floor(screensf[i].z);
    }
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
  
  void render1Chain() {
    boolean lastWasSheet = false;
    boolean lastWasHelix = false;
    
    // this code is REALLY BAD, I admit. Key structures that must render properly
    // include 1crn and 7hvp
    
    for (int i = monomerCount; --i >= 0;) {
      // runs backwards, so it can render the heads first
      Monomer group = monomers[i];
      if ((group.shapeVisibilityFlags & myVisibilityFlag) != 0) {
        short colix = Graphics3D.inheritColix(colixes[i],
            group.getLeadAtom().colixAtom);
        boolean isHelix = isHelixes[i];
        boolean isSheet = isSpecials[i] && !isHelix;
        boolean isHelixRocket = (renderAsRockets ? isHelix : false);
        if (isHelixRocket) {
          // skip helixRockets in this pass
        } else if (isSheet || isHelix) {
          if (lastWasSheet && isSheet || lastWasHelix && isHelix) {
            render2StrandSegment(monomerCount, group, colix, mads, i);
          } else {
            render2StrandArrowhead(monomerCount, group, colix, mads, i);
          }
        } else {
          //turn
          if (lastWasHelix && !isHelix && !isSheet) {
            renderRopeSegment(colix, mads, i, monomerCount, monomers,
                screens, isSpecials);
          } else if (!renderAsRockets || i == 0 || !isHelixes[i - 1]) {
            renderRopeSegment(colix, mads, i, monomerCount, monomers,
               leadMidpointScreens, isSpecials);
          }
          if (isNucleicPolymer)
            renderNucleicBaseStep((NucleicMonomer) group, colix, mads[i],
                leadMidpointScreens[i + 1]);
        }
        lastWasSheet = isSheet;
        lastWasHelix = isHelix;
      } else {
        lastWasHelix = lastWasSheet = false;
      }
    }

    //doing the cylinders separately because we want to connect them if we can.
    
    if (renderAsRockets && !isNucleicPolymer && !isCarbohydratePolymer) {
      lastWasHelix = false;
      for (int i = 0; i < monomerCount; ++i) {
        if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0)
          continue;
        Monomer group = monomers[i];
        if ((group.shapeVisibilityFlags & myVisibilityFlag) != 0) {
          short colix = Graphics3D.inheritColix(colixes[i],
              group.getLeadAtom().colixAtom);
          boolean isHelix = isHelixes[i];
          if (isHelix) {
            renderHelixAsRocket(group, colix, mads[i], i > 0 && isHelixes[i-1]);
            if (newRockets &&  i > 0 && !isSpecials[i-1]) {
              renderRopeSegment2(colix, mads, i, i - 1, monomerCount,
                  monomers, screens, null);
            }
          } else if (isSpecials[i]) {
            // sheet done above
          } else if (lastWasHelix) {
            if (newRockets)
              renderRopeSegment2(colix, mads, i-1, i, monomerCount,
                  monomers, screens, null);
            renderRopeSegment(colix, mads, i, monomerCount,
                monomers, screens, isSpecials);
          }
          lastWasHelix = isHelix;
        }
      }
      renderPendingRocketSegment(true);
      viewer.freeTempScreens(screens);
      viewer.freeTempPoints(cordMidPoints);
      viewer.freeTempPoints(screensf);
    }
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
    viewer.freeTempScreens(leadMidpointScreens);
    viewer.freeTempBooleans(isSpecials);
    viewer.freeTempBooleans(isHelixes);
  }
  
  // this is a hack of the old rocket code that doesn't work properly for mulitple colors.
  
  int lastDiameter;

  void renderHelixAsRocket(Monomer monomer, short colix, short mad, boolean isEnd) {
    lastDiameter = Integer.MAX_VALUE;
    ProteinStructure proteinstructure = monomer.getProteinStructure();
    if (tPending) {
      if (proteinstructure == proteinstructurePending && mad == madPending
          && colix == colixPending
          && proteinstructure.getIndex(monomer) == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPendingRocketSegment(isEnd);
    }
    proteinstructurePending = proteinstructure;
    startIndexPending = endIndexPending = proteinstructure.getIndex(monomer);
    colixPending = colix;
    madPending = mad;
    tPending = true;
  }

  final Point3f screenA = new Point3f();
  Point3f screenB = new Point3f();
  Point3f screenC = new Point3f();

  void renderPendingRocketSegment(boolean isEnd) {
    if (!tPending)
      return;
    tPending = false;
    if (proteinstructurePending instanceof Helix) {
      Point3f[] segments = proteinstructurePending.getSegments();
      boolean tEnd = (endIndexPending == proteinstructurePending
          .getMonomerCount() - 1);
      renderPendingRocketSegment(segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1], tEnd, startIndexPending == 0);
      //System.out.println("renderP " + startIndexPending + " " + endIndexPending
        //  + " " + tEnd + " " + isEnd);
    }
  }

  void renderPendingRocketSegment(Point3f pointStart, Point3f pointBeforeEnd,
                          Point3f pointEnd, boolean tEnd, boolean isEnd) {
    viewer.transformPoint(pointStart, screenA);
    viewer.transformPoint(pointEnd, screenB);
    int zMid = (int)Math.floor((screenA.z + screenB.z) / 2f);
    int diameter = viewer.scaleToScreen(zMid, madPending);
    if (tEnd) {
      viewer.transformPoint(pointBeforeEnd, screenC);
      if (pointBeforeEnd.distance(pointEnd) > MIN_CONE_HEIGHT) {
        int coneDiameter = viewer.scaleToScreen((int)Math.floor(screenC.z), madPending
            + (madPending >> 2));
        g3d.fillCone(colixPending, Graphics3D.ENDCAPS_FLAT, coneDiameter,
            screenC, screenB);
        lastDiameter=coneDiameter;
      } else {
 //       System.out.println(screenB + " " + screenC + " " + diameter + " diam" );
        g3d.fillCylinderBits(colixPending, Graphics3D.ENDCAPS_FLAT, diameter,
            screenB, screenC);
        lastDiameter = Integer.MAX_VALUE;        
      }
      if (startIndexPending == endIndexPending)
        return;
      Point3f t = screenB;
      screenB = screenC;
      screenC = t;
    }
/*
 * well this was an idea that appears to be too complicated for now at least --
 * when drawing a cylinder draw just the beginning and the end rather than each 
 * segment. So.... back to the original for now.
 * 
 *     g3d.fillCylinderBits(colixPending, Graphics3D.ENDCAPS_FLAT, (diameter == lastDiameter ? -1 : diameter),
        screenA, screenB);
*/ 
    g3d.fillCylinderBits(colixPending, Graphics3D.ENDCAPS_FLAT, diameter,
        screenA, screenB);

    lastDiameter = diameter;
  }

  void render2StrandSegment(int monomerCount, Monomer group, short colix,
                            short[] mads, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1;
    if (iPrev < 0)
      iPrev = 0;
    int iNext = i + 1;
    if (iNext > iLast)
      iNext = iLast;
    int iNext2 = i + 2;
    if (iNext2 > iLast)
      iNext2 = iLast;

    //change false -> true to fill in mesh
    g3d.drawHermite(true, ribbonBorder, colix, isNucleicPolymer ? 4 : 7,
        ribbonTopScreens[iPrev], ribbonTopScreens[i], ribbonTopScreens[iNext],
        ribbonTopScreens[iNext2], ribbonBottomScreens[iPrev],
        ribbonBottomScreens[i], ribbonBottomScreens[iNext],
        ribbonBottomScreens[iNext2], renderAsRockets ? 8 : 0);
  }

  final Point3i screenArrowTop = new Point3i();
  final Point3i screenArrowTopPrev = new Point3i();
  final Point3i screenArrowBot = new Point3i();
  final Point3i screenArrowBotPrev = new Point3i();

  void render2StrandArrowhead(int monomerCount, Monomer group, short colix,
                              short[] mads, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1;
    if (iPrev < 0)
      iPrev = 0;
    int iNext = i + 1;
    if (iNext > iLast)
      iNext = iLast;
    int iNext2 = i + 2;
    if (iNext2 > iLast)
      iNext2 = iLast;
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i], .7f / 1000,
        screenArrowTop);
    calc1Screen(leadMidpoints[iPrev], wingVectors[iPrev], mads[iPrev],
        1.0f / 1000, screenArrowTopPrev);
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i], -.7f / 1000,
        screenArrowBot);
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i], -1.0f / 1000,
        screenArrowBotPrev);
    if (ribbonBorder)
      g3d.fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
          screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
          screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    g3d.drawHermite(true, ribbonBorder, colix, isNucleicPolymer ? 4 : 7,
        screenArrowTopPrev, screenArrowTop, leadMidpointScreens[iNext],
        leadMidpointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        leadMidpointScreens[iNext], leadMidpointScreens[iNext2],
        renderAsRockets ? 8 : 0);
  }

  final Point3f[] ring6Points = new Point3f[6];
  final Point3i[] ring6Screens = new Point3i[6];
  final Point3f[] ring5Points = new Point3f[5];
  final Point3i[] ring5Screens = new Point3i[5];

  {
    ring6Screens[5] = new Point3i();
    for (int i = 5; --i >= 0; ) {
      ring5Screens[i] = new Point3i();
      ring6Screens[i] = new Point3i();
    }
  }

  void renderNucleicBaseStep(NucleicMonomer nucleotide,
                             short colix, short mad, Point3i backboneScreen) {
    nucleotide.getBaseRing6Points(ring6Points);
    viewer.transformPoints(ring6Points, ring6Screens);
    renderRing6(colix);
    boolean hasRing5 = nucleotide.maybeGetBaseRing5Points(ring5Points);
    Point3i stepScreen;
    if (hasRing5) {
      viewer.transformPoints(ring5Points, ring5Screens);
      renderRing5();
      stepScreen = ring5Screens[2];
    } else {
      stepScreen = ring6Screens[1];
    }
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL,
                     viewer.scaleToScreen(backboneScreen.z,
                                          mad > 1 ? mad / 2 : mad),
                     backboneScreen, stepScreen);
    --ring6Screens[5].z;
    for (int i = 5; --i > 0; ) {
      --ring6Screens[i].z;
      if (hasRing5)
        --ring5Screens[i].z;
    }
    for (int i = 6; --i > 0; )
      g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
                       ring6Screens[i], ring6Screens[i - 1]);
    if (hasRing5) {
      for (int i = 5; --i > 0; )
        g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
                         ring5Screens[i], ring5Screens[i - 1]);
    } else {
      g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
                       ring6Screens[5], ring6Screens[0]);
    }
  }

  void renderRing6(short colix) {
    g3d.calcSurfaceShade(colix,
                         ring6Screens[0], ring6Screens[2], ring6Screens[4]);
    g3d.fillTriangle(ring6Screens[0], ring6Screens[2], ring6Screens[4]);
    g3d.fillTriangle(ring6Screens[0], ring6Screens[1], ring6Screens[2]);
    g3d.fillTriangle(ring6Screens[0], ring6Screens[4], ring6Screens[5]);
    g3d.fillTriangle(ring6Screens[2], ring6Screens[3], ring6Screens[4]);
  }

  void renderRing5() {
    // shade was calculated previously by renderRing6();
    g3d.fillTriangle(ring5Screens[0], ring5Screens[2], ring5Screens[3]);
    g3d.fillTriangle(ring5Screens[0], ring5Screens[1], ring5Screens[2]);
    g3d.fillTriangle(ring5Screens[0], ring5Screens[3], ring5Screens[4]);
  }  
}
