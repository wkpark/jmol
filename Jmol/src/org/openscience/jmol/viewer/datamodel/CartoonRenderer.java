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

  boolean isNucleotidePolymer;
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
      isNucleotidePolymer = strandsChain.polymer instanceof NucleotidePolymer;
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
                             isNucleotidePolymer ? 1f / 1000 : 0.5f / 1000);
    ribbonBottomScreens = calcScreens(leadMidpoints, wingVectors, mads,
                                isNucleotidePolymer ? 0f : -0.5f / 1000);
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
        }
        else
          renderRopeSegment(colix, mads, i,
                            monomerCount, monomers,
                            leadMidpointScreens, isSpecials);
        lastWasSpecial = isSpecial;
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
    g3d.drawHermite(true, colix, isNucleotidePolymer ? 4 : 7,
                    screenArrowTopPrev, screenArrowTop,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2],
                    screenArrowBotPrev, screenArrowBot,
                    leadMidpointScreens[iNext], leadMidpointScreens[iNext2]
                    );
  }

}
