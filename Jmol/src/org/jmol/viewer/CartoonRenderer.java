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

class CartoonRenderer extends RocketsRenderer {

  boolean newRockets = true;
  boolean renderAsRockets;
  
  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    Cartoon.Cchain schain = (Cartoon.Cchain) mpspolymer;
    if (schain.wingVectors == null || isCarbohydrate)
      return;
    renderAsRockets = viewer.getCartoonRocketFlag();
    aspectRatio = viewer.getRibbonAspectRatio();
    calcScreenControlPoints();
    ribbonTopScreens = calcScreens(isNucleic ? 1f : 0.5f);
    ribbonBottomScreens = calcScreens(isNucleic ? 0f : -0.5f);
    if (isNucleic) {
      renderNucleic();
    } else {
      calcRopeMidPoints(newRockets);
      render1();
      viewer.freeTempPoints(cordMidPoints);
    }
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
  }

  void renderNucleic() {
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i)) {
        renderRopeSegment(i, true);
        renderNucleicBaseStep((NucleicMonomer) monomers[i], getLeadColix(i), mads[i],
            controlPointScreens[i + 1]);
      }
  }

  void render1() {
    boolean lastWasSheet = false;
    boolean lastWasHelix = false;
    ProteinStructure previousStructure = null;
    ProteinStructure thisStructure;

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes monomerCount --> 0, because
    // we want to hit the heads first

    for (int i = monomerCount; --i >= 0;) {
      // runs backwards, so it can render the heads first
      thisStructure = monomers[i].getProteinStructure();
      if (thisStructure != previousStructure) {
        lastWasHelix = false;
        lastWasSheet = false;
      }
      previousStructure = thisStructure;
      boolean isHelix = isHelixes[i];
      boolean isSheet = isSpecials[i] && !isHelix;
      boolean isHelixRocket = (renderAsRockets ? isHelix : false);
      if (bsVisible.get(i)) {
        if (isHelixRocket) {
          //next pass
        } else if (isSheet || isHelix) {
          if (lastWasSheet && isSheet || lastWasHelix && isHelix)
            render2StrandSegment(true, i, aspectRatio);
          else
            render2StrandArrowhead(i);
        } else {
          renderRopeSegment(i, true);
        }
      }
      lastWasSheet = isSheet;
      lastWasHelix = isHelix;
    }

    if (renderAsRockets)
      renderRockets();
  }

  void renderRockets() {
    //doing the cylinders separately because we want to connect them if we can.

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes 0 --> monomerCount, because
    // the special segments routine takes care of heads
    tPending = false;
    for (int i = 0; i < monomerCount; ++i)
      if (bsVisible.get(i) && isHelixes[i])
        renderSpecialSegment(monomers[i], getLeadColix(i), mads[i]);
    renderPending();
  }
  
  //// cardinal hermite arrow head rendering

  final Point3i screenArrowTop = new Point3i();
  final Point3i screenArrowTopPrev = new Point3i();
  final Point3i screenArrowBot = new Point3i();
  final Point3i screenArrowBotPrev = new Point3i();

  void render2StrandArrowhead(int i) {
    short colix = getLeadColix(i);
    int iPrev = Math.max(i - 1, 0);
    int iNext = Math.min(i + 1, monomerCount);
    int iNext2 = Math.min(i + 2, monomerCount);
    calc1Screen(controlPoints[i], wingVectors[i], mads[i], .7f,
        screenArrowTop);
    calc1Screen(controlPoints[iPrev], wingVectors[iPrev], mads[iPrev],
        1.0f, screenArrowTopPrev);
    calc1Screen(controlPoints[i], wingVectors[i], mads[i], -.7f,
        screenArrowBot);
    calc1Screen(controlPoints[i], wingVectors[i], mads[i], -1.0f,
        screenArrowBotPrev);
    if (ribbonBorder)
      g3d.fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
          screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
          screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    g3d.drawHermite(true, ribbonBorder, colix, isNucleic ? 4 : 7,
        screenArrowTopPrev, screenArrowTop, controlPointScreens[iNext],
        controlPointScreens[iNext2], screenArrowBotPrev, screenArrowBot,
        controlPointScreens[iNext], controlPointScreens[iNext2],
        aspectRatio);
  }

  //// nucleic acid base rendering
  
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
