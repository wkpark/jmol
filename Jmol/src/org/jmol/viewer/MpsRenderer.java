/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

abstract class MpsRenderer extends ShapeRenderer {

  Point3f[] leadMidpoints;
  Vector3f[] wingVectors;

  void render() {
    if (shape == null)
      return;
    Mps mcps = (Mps)shape;
    for (int m = mcps.getMpsmodelCount(); --m >= 0; ) {
      Mps.Mpsmodel mcpsmodel = mcps.getMpsmodel(m);
      if ((mcpsmodel.modelVisibilityFlags & shape.myVisibilityFlag) == 0)
        continue;
      for (int c = mcpsmodel.getMpspolymerCount(); --c >= 0; ) {
        Mps.Mpspolymer mpspolymer = mcpsmodel.getMpspolymer(c);
        if (mpspolymer.monomerCount >= 2)
          renderMpspolymer(mpspolymer, shape.myVisibilityFlag);
      }
    }
  }

  abstract void renderMpspolymer(Mps.Mpspolymer mpspolymer, int myVisibilityFlag);

  ////////////////////////////////////////////////////////////////
  // some utilities
  final boolean[] calcIsSpecials(int monomerCount, Monomer[] monomers) {
    boolean[] isSpecials = viewer.allocTempBooleans(monomerCount + 1);
    for (int i = monomerCount; --i >= 0; ) {
      isSpecials[i] = monomers[i].isHelixOrSheet();
    }
    isSpecials[monomerCount] = isSpecials[monomerCount - 1];
    return isSpecials;
  }

  final boolean[] calcIsHelix(int monomerCount, Monomer[] monomers) {
    boolean[] isHelix = viewer.allocTempBooleans(monomerCount + 1);
    for (int i = monomerCount; --i >= 0; ) {
      isHelix[i] = monomers[i].isHelix();
    }
    isHelix[monomerCount] = isHelix[monomerCount - 1];
    return isHelix;
  }


  final Point3i[] calcScreenLeadMidpoints(int monomerCount, Point3f[] leadMidpoints) {
    int count = monomerCount + 1;
    Point3i[] leadMidpointScreens = viewer.allocTempScreens(count);
    for (int i = count; --i >= 0; ) {
      viewer.transformPoint(leadMidpoints[i], leadMidpointScreens[i]);
    }
    return leadMidpointScreens;
  }

  final void renderRopeSegment(short colix, short[] mads, int i,
                               int monomerCount, Monomer[] monomers,
                               Point3i[] leadMidpointScreens, boolean[] isSpecials) {
    renderRopeSegment2(colix, mads, i, i, monomerCount, monomers, leadMidpointScreens, isSpecials);    
  }

  final void renderRopeSegment2(short colix, short[] mads, int i, int imad,
                               int monomerCount, Monomer[] monomers,
                               Point3i[] leadMidpointScreens, boolean[] isSpecials) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > monomerCount) iNext1 = monomerCount;
    int iNext2 = i + 2; if (iNext2 > monomerCount) iNext2 = monomerCount;
    
    int madThis, madBeg, madEnd;
    madThis = madBeg = madEnd = mads[imad];
    if (isSpecials != null) {
      if (! isSpecials[iPrev1])
        madBeg = (mads[iPrev1] + madThis) / 2;
      if (! isSpecials[iNext1])
        madEnd = (mads[iNext1] + madThis) / 2;
    }
    int diameterBeg = 0;
    try{
      diameterBeg =
      viewer.scaleToScreen(leadMidpointScreens[i].z, madBeg);
    }catch (Exception e) {
      System.out.println(i);
    }
    int diameterEnd =
      viewer.scaleToScreen(leadMidpointScreens[iNext1].z, madEnd);
    int diameterMid =
      viewer.scaleToScreen(monomers[i].getLeadAtom().getScreenZ(),
                           madThis);
    g3d.fillHermite(colix, monomers[i].isNucleic() ? 4 : 7,
                    diameterBeg, diameterMid, diameterEnd,
                    leadMidpointScreens[iPrev1], leadMidpointScreens[i],
                    leadMidpointScreens[iNext1], leadMidpointScreens[iNext2]);
  }
}
