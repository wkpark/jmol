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

import org.jmol.g3d.*;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

class CartoonRenderer extends MpsRenderer {

  final Point3f pointT = new Point3f();

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
        //boolean isSpecial = isSpecials[i];
        short mad = mads[i];
        /*
        if (isSpecial && !lastWasSpecial)
            mad *= 2;
        */
        /*
        short mad = isSpecial || i == 0 ? mads[i] : mads[i - 1];
        if (i + 1 < count && isSpecial) {
          if (isSpecial && ! isSpecials[i + 1])
            mad = mads[i];
        }
        */
        float scale = mad * offsetFraction;
        pointT.scaleAdd(scale, centers[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  boolean isNucleicPolymer;
  int monomerCount;
  Monomer[] monomers;
  Point3f[] leadMidpoints;
  Vector3f[] wingVectors;
  short[] mads;
  short[] colixes;

  boolean[] isSpecials;
  Point3i[] leadMidpointScreens;
  Point3i[] ribbonTopScreens;
  Point3i[] ribbonBottomScreens;

  void renderMpspolymer( Mps.Mpspolymer mpspolymer) {
    Cartoon.Cchain strandsChain = (Cartoon.Cchain)mpspolymer;
    if (strandsChain.wingVectors != null) {
      monomerCount = strandsChain.monomerCount;
      monomers = strandsChain.monomers;
      isNucleicPolymer = strandsChain.polymer instanceof NucleicPolymer;
      leadMidpoints = strandsChain.leadMidpoints;
      wingVectors = strandsChain.wingVectors;
      mads = strandsChain.mads;
      colixes = strandsChain.colixes;
      render1Chain();
    }
  }


  void render1Chain() {

    isSpecials = calcIsSpecials(monomerCount, monomers);
    leadMidpointScreens = calcScreenLeadMidpoints(monomerCount, leadMidpoints);
    ribbonTopScreens = calcScreens(leadMidpoints, wingVectors, mads,
                             isNucleicPolymer ? 1f / 1000 : 0.5f / 1000);
    ribbonBottomScreens = calcScreens(leadMidpoints, wingVectors, mads,
                                isNucleicPolymer ? 0f : -0.5f / 1000);
    boolean lastWasSpecial = false;
    for (int i = monomerCount; --i >= 0; )
      if (mads[i] > 0) {
        Monomer group = monomers[i];
        short colix = colixes[i];
        if (colix == 0)
          colix = group.getLeadAtom().colixAtom;
        boolean isSpecial = isSpecials[i];
        if (isSpecial) {
          if (lastWasSpecial)
            render2StrandSegment(monomerCount, group, colix, mads, i);
          else
            render2StrandArrowhead(monomerCount, group, colix, mads, i);
        } else {
          renderRopeSegment(colix, mads, i,
                            monomerCount, monomers,
                            leadMidpointScreens, isSpecials);
          if (isNucleicPolymer)
            renderNucleicBaseStep((NucleicMonomer)group, colix, mads[i],
                                  leadMidpointScreens[i + 1]);
        }
        lastWasSpecial = isSpecial;
      } else {
        // miguel 2004 12 13
        // bug [ 1014874 ] Beta pleated sheet arrowheads
        // if a segment is not being rendered then we need to
        // reset our flag so that the arrowhead will be drawn
        // for the next segment
        lastWasSpecial = false;
      }
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
    viewer.freeTempScreens(leadMidpointScreens);
    viewer.freeTempBooleans(isSpecials);
  }
  
  void render2StrandSegment(int monomerCount, Monomer group, short colix,
                            short[] mads, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getLeadAtom().colixAtom;
    
    //change false -> true to fill in mesh
    g3d.drawHermite(true, colix, isNucleicPolymer ? 4 : 7,
                    ribbonTopScreens[iPrev], ribbonTopScreens[i],
                    ribbonTopScreens[iNext], ribbonTopScreens[iNext2],
                    ribbonBottomScreens[iPrev], ribbonBottomScreens[i],
                    ribbonBottomScreens[iNext], ribbonBottomScreens[iNext2]
                    );
  }

  final Point3i screenArrowTop = new Point3i();
  final Point3i screenArrowTopPrev = new Point3i();
  final Point3i screenArrowBot = new Point3i();
  final Point3i screenArrowBotPrev = new Point3i();

  void render2StrandArrowhead(int monomerCount, Monomer group, short colix,
                              short[] mads, int i) {
    int iLast = monomerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getLeadAtom().colixAtom;
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i],
                .7f / 1000, screenArrowTop);
    calc1Screen(leadMidpoints[iPrev], wingVectors[iPrev], mads[iPrev],
                1.0f / 1000, screenArrowTopPrev);
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i],
                -.7f / 1000, screenArrowBot);
    calc1Screen(leadMidpoints[i], wingVectors[i], mads[i],
                -1.0f / 1000, screenArrowBotPrev);
    g3d.fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, 3,
                     screenArrowTop.x, screenArrowTop.y, screenArrowTop.z,
                     screenArrowBot.x, screenArrowBot.y, screenArrowBot.z);
    g3d.drawHermite(true, colix, isNucleicPolymer ? 4 : 7,
                    screenArrowTopPrev, screenArrowTop,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2],
                    screenArrowBotPrev, screenArrowBot,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2]
                    );
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
    //    System.out.println("render nucleic base step:" + nucleotide);
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
    //    g3d.fillSphereCentered(Graphics3D.YELLOW, 15, pScreen);
    g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL,
                     viewer.scaleToScreen(backboneScreen.z,
                                          mad > 1 ? mad / 2 : mad),
                     backboneScreen, stepScreen);
    ////////////////////////////////////////////////////////////////
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
    g3d.calcSurfaceShade(colix, false, 
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
