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
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

class StrandsRenderer extends McpsRenderer {

  Strands strands;
  Point3f pointT = new Point3f();

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                   short[] mads, float offsetFraction) {
    Point3i[] screens = frameRenderer.getTempScreens(centers.length);
    if (offsetFraction == 0) {
      for (int i = centers.length; --i >= 0; )
        viewer.transformPoint(centers[i], screens[i]);
    } else {
      offsetFraction /= 1000;
      for (int i = centers.length; --i >= 0; ) {
        pointT.set(vectors[i]);
        float scale = mads[i] * offsetFraction;
        pointT.scaleAdd(scale, centers[i]);
        viewer.transformPoint(pointT, screens[i]);
      }
    }
    return screens;
  }

  int polymerCount;
  int strandCount;
  float halfStrandCount;
  float strandSeparation;
  float baseOffset;

  void renderMcpschain(Mcps.Mcpschain mcpschain) {
    Strands.Schain schain = (Strands.Schain)mcpschain;
    polymerCount = schain.polymerCount;

    strandCount = viewer.getStrandsCount();
    strandSeparation = (strandCount <= 1 ) ? 0 : 1f / (strandCount - 1);
    baseOffset =
      ((strandCount & 1) == 0) ? strandSeparation / 2 : strandSeparation;
    
    render1Chain(schain.polymerCount,
                 schain.polymerGroups,
                 schain.centers,
                 schain.vectors,
                 schain.mads,
                 schain.colixes);
  }


  void render1Chain(int polymerCount,
                    PdbGroup[] groups, Point3f[] centers,
                    Vector3f[] vectors, short[] mads, short[] colixes) {
    Point3i[] screens;
    for (int i = strandCount >> 1; --i >= 0; ) {
      float f = (i * strandSeparation) + baseOffset;
      screens = calcScreens(centers, vectors, mads, f);
      render1Strand(polymerCount, groups, mads, colixes, screens);
      screens = calcScreens(centers, vectors, mads, -f);
      render1Strand(polymerCount, groups, mads, colixes, screens);
    }
    if ((strandCount & 1) != 0) {
      screens = calcScreens(centers, vectors, mads, 0f);
      render1Strand(polymerCount, groups, mads, colixes, screens);
    }
  }

  void render1Strand(int polymerCount, PdbGroup[] groups, short[] mads,
                     short[] colixes, Point3i[] screens) {
    for (int i = polymerCount; --i >= 0; )
      if (mads[i] > 0)
        render1StrandSegment(polymerCount,
                             groups[i], colixes[i], mads, screens, i);
  }


  void render1StrandSegment(int polymerCount, PdbGroup group, short colix,
                            short[] mads, Point3i[] screens, int i) {
    int iLast = polymerCount;
    int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
    int iNext = i + 1; if (iNext > iLast) iNext = iLast;
    int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
    if (colix == 0)
      colix = group.getAlphaCarbonAtom().colixAtom;
    g3d.drawHermite(colix, 7, screens[iPrev], screens[i],
                    screens[iNext], screens[iNext2]);
  }
}
