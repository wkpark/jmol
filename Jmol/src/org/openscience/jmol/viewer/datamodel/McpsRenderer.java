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
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

abstract class McpsRenderer extends ShapeRenderer {

  void render() {
    if (shape == null)
      return;
    Mcps mcps = (Mcps)shape;
    for (int m = mcps.getMcpsmodelCount(); --m >= 0; ) {
      Mcps.Mcpsmodel mcpsmodel = mcps.getMcpsmodel(m);
      if (displayModel > 0 && displayModel != mcpsmodel.modelNumber)
        continue;
      for (int c = mcpsmodel.getMcpschainCount(); --c >= 0; ) {
        Mcps.Mcpschain mcpschain = mcpsmodel.getMcpschain(c);
        if (mcpschain.polymerCount >= 2)
          renderMcpschain(mcpschain);
      }
    }
  }

  abstract void renderMcpschain(Mcps.Mcpschain mcpschain);

  ////////////////////////////////////////////////////////////////
  // some utilities
  final boolean[] calcIsSpecials(int polymerCount, Group[] polymerGroups) {
    boolean[] isSpecials = viewer.allocTempBooleans(polymerCount + 1);
    for (int i = polymerCount; --i >= 0; )
      isSpecials[i] = polymerGroups[i].isHelixOrSheet();
    isSpecials[polymerCount] = isSpecials[polymerCount - 1];
    return isSpecials;
  }


  final Point3i[] calcScreenLeadMidpoints(int polymerCount, Point3f[] leadMidpoints) {
    int count = polymerCount + 1;
    Point3i[] leadMidpointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(leadMidpoints[i], leadMidpointScreens[i]);
      //g3d.fillSphereCentered(Graphics3D.CYAN, 15, leadMidpointScreens[i]);
    }
    return leadMidpointScreens;
  }

  final void renderRopeSegment(short colix, short[] mads, int i,
                               int polymerCount, Group[] polymerGroups,
                               Point3i[] leadMidpointScreens, boolean[] isSpecials) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > polymerCount) iNext1 = polymerCount;
    int iNext2 = i + 2; if (iNext2 > polymerCount) iNext2 = polymerCount;
    
    int madThis, madBeg, madEnd;
    madThis = madBeg = madEnd = mads[i];
    if (isSpecials != null) {
      if (! isSpecials[iPrev1])
        madBeg = (mads[iPrev1] + madThis) / 2;
      if (! isSpecials[iNext1])
        madEnd = (mads[iNext1] + madThis) / 2;
    }
    int diameterBeg = viewer.scaleToScreen(leadMidpointScreens[i].z, madBeg);
    int diameterEnd = viewer.scaleToScreen(leadMidpointScreens[iNext1].z, madEnd);
    int diameterMid =
      viewer.scaleToScreen(polymerGroups[i].getLeadAtom().getScreenZ(),
                           madThis);
    g3d.fillHermite(colix, 4,
                    diameterBeg, diameterMid, diameterEnd,
                    leadMidpointScreens[iPrev1], leadMidpointScreens[i],
                    leadMidpointScreens[iNext1], leadMidpointScreens[iNext2]);
  }
}
