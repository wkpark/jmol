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


class RibbonsRenderer extends McpsRenderer { // not current for Mcp class

  Ribbons strands;

  Point3i[] getTempScreens(int minLen) {
    Point3i[] screensT = new Point3i[0];//DC: made local
    if (screensT.length < minLen) {
      Point3i[] t = new Point3i[minLen];
      System.arraycopy(screensT, 0, t, 0, screensT.length);
      for (int i = screensT.length; i < t.length; ++i)
        t[i] = new Point3i();
      screensT = t;
    }
    return screensT;
  }

  Point3i[] calcScreens(Point3f[] centers, Vector3f[] vectors,
                   short[] mads, float offsetFraction) {

  Point3f pointT = new Point3f();  //DC: made local
  Point3i[] screens = getTempScreens(centers.length);
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

  void renderMcpschain( Mcps.Mcpschain mcpsChain) {
    Ribbons.Schain strandsChain = (Ribbons.Schain)mcpsChain;
    polymerCount = strandsChain.polymerCount;

    strandCount = viewer.getStrandsCount();
    strandSeparation = (strandCount <= 1 ) ? 0 : 1f / (strandCount - 1);
    baseOffset =
      ((strandCount & 1) == 0) ? strandSeparation / 2 : strandSeparation;

    render1Chain(strandsChain.polymerCount,
                 strandsChain.polymerGroups,
                 strandsChain.centers,
                 strandsChain.vectors,
                 strandsChain.mads,
                 strandsChain.colixes);
  }


  void render1Chain(int polymerCount,
                    PdbGroup[] groups, Point3f[] centers,
                    Vector3f[] vectors, short[] mads, short[] colixes) {
    Point3i[] screensTop;
    Point3i[] screensBottom;

    int j = strandCount >> 1;
    float offset = (j * strandSeparation) + baseOffset;
      screensTop = calcScreens(centers, vectors, mads, offset);
      screensBottom = calcScreens(centers, vectors, mads, -offset);
      render2Strand(polymerCount, groups, mads, colixes, screensTop, screensBottom);
  }

  void render2Strand(int polymerCount, PdbGroup[] groups, short[] mads,
                     short[] colixes, Point3i[] screensTop, Point3i[] screensBottom) {
    for (int i = polymerCount; --i >= 0; ){
      if (mads[i] > 0){
        render2StrandSegment(polymerCount,
                             groups[i], colixes[i], mads, screensTop, screensBottom, i);}}
  }

  void render2StrandSegment(int polymerCount, PdbGroup group, short colix,
                          short[] mads, Point3i[] screensTop, Point3i[] screensBottom, int i) {
  int iLast = polymerCount;
  int iPrev = i - 1; if (iPrev < 0) iPrev = 0;
  int iNext = i + 1; if (iNext > iLast) iNext = iLast;
  int iNext2 = i + 2; if (iNext2 > iLast) iNext2 = iLast;
  if (colix == 0)
    colix = group.getAlphaCarbonAtom().colixAtom;

    //change false -> true to fill in mesh
  g3d.drawHermite(true, colix, 7, screensTop[iPrev], screensTop[i],
                  screensTop[iNext], screensTop[iNext2],
                  screensBottom[iPrev], screensBottom[i],
                  screensBottom[iNext], screensBottom[iNext2]
                  );
}

}
