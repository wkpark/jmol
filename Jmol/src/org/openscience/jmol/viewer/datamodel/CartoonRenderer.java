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
        boolean isSpecial = isSpecials[i];
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

  void calcScreenLeadMidpoints() {
    int count = polymerCount + 1;
    leadMidpointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(leadMidpoints[i], leadMidpointScreens[i]);
      //g3d.fillSphereCentered(Graphics3D.CYAN, 15, leadMidpointScreens[i]);
    }
  }

  boolean isNucleotidePolymer;
  int polymerCount;
  Group[] polymerGroups;
  Point3f[] leadMidpoints;
  Vector3f[] wingVectors;
  short[] mads;
  short[] colixes;

  Point3i[] leadMidpointScreens;
  Point3i[] ribbonTopScreens;
  Point3i[] ribbonBottomScreens;

  void renderMcpschain( Mcps.Mcpschain mcpsChain) {
    Cartoon.Cchain strandsChain = (Cartoon.Cchain)mcpsChain;
    if (strandsChain.wingVectors != null) {
      polymerCount = strandsChain.polymerCount;
      polymerGroups = strandsChain.polymerGroups;
      calcIsSpecials();
      isNucleotidePolymer = strandsChain.polymer instanceof NucleotidePolymer;
      leadMidpoints = strandsChain.leadMidpoints;
      wingVectors = strandsChain.wingVectors;
      mads = strandsChain.mads;
      colixes = strandsChain.colixes;
      render1Chain();
    }
  }


  boolean[] isSpecials;

  void calcIsSpecials() {
    isSpecials = frameRenderer.getTempBooleans(polymerCount);
    for (int i = polymerCount; --i >= 0; )
      isSpecials[i] = polymerGroups[i].isHelixOrSheet();
  }


  void render1Chain() {

    calcScreenLeadMidpoints();
    ribbonTopScreens = calcScreens(leadMidpoints, wingVectors, mads,
                             isNucleotidePolymer ? 1f / 1000 : 0.5f / 1000);
    ribbonBottomScreens = calcScreens(leadMidpoints, wingVectors, mads,
                                isNucleotidePolymer ? 0f : -0.5f / 1000);
    boolean lastWasSpecial = false;
    for (int i = polymerCount; --i >= 0; )
      if (mads[i] > 0) {
        Group group = polymerGroups[i];
        short colix = colixes[i];
        if (colix == 0)
          colix = group.getLeadAtom().colixAtom;
        boolean isSpecial = isSpecials[i];
        if (isSpecial) {
          if (lastWasSpecial)
            render2StrandSegment(polymerCount, group, colix, mads, i);
          else
            render2StrandArrowhead(polymerCount, group, colix, mads, i);
        }
        else
          renderRopeSegment(colix, mads, i);
        lastWasSpecial = isSpecial;
      }
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
    viewer.freeTempScreens(leadMidpointScreens);
  }
  
  void render2StrandSegment(int polymerCount, Group group, short colix,
                            short[] mads, int i) {
    int iLast = polymerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getLeadAtom().colixAtom;
    
    //change false -> true to fill in mesh
    g3d.drawHermite(true, colix, isNucleotidePolymer ? 4 : 7,
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

  void render2StrandArrowhead(int polymerCount, Group group, short colix,
                              short[] mads, int i) {
    int iLast = polymerCount;
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
    g3d.drawHermite(true, colix, isNucleotidePolymer ? 4 : 7,
                    screenArrowTopPrev, screenArrowTop,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2],
                    screenArrowBotPrev, screenArrowBot,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2]
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


}
