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

class TraceRenderer extends McpsRenderer {

  void render() {
    super.render();
    screens = null;
    alphas = null;
  }

  void renderMcpschain(Mcps.Mcpschain mcpschain) {
    Trace.Tchain tchain = (Trace.Tchain)mcpschain;
    render1Chain(tchain.polymerCount,
                 tchain.polymerGroups,
                 tchain.mads,
                 tchain.colixes);
  }

  Point3i[] screens;
  Atom[] alphas;
  
  void render1Chain(int count, PdbGroup[] groups,
                    short[] mads, short[] colixes) {
    if (count > 0) {
      calcMidPoints(count, groups);
      for (int i = count; --i >= 0; ) {
        if (mads[i] == 0)
          continue;
        short colix = colixes[i];
        if (colix == 0)
          colix = alphas[i].colixAtom;
        render1Segment(colix, mads, i, count);
      }
    }
  }

  void calcMidPoints(int count, PdbGroup[] groups) {
    screens = frameRenderer.getTempScreens(count + 1);
    alphas = frameRenderer.getTempAtoms(count);
    Atom atomPrev = alphas[0] = groups[0].getAlphaCarbonAtom();
    setScreen(atomPrev, screens[0]);
    for (int i = 1; i < count; ++i) {
        Atom atomThis = alphas[i] = groups[i].getAlphaCarbonAtom();
        calcAverageScreen(atomPrev, atomThis, screens[i]);
        atomPrev = atomThis;
    }
    setScreen(atomPrev, screens[count]);
  }

  void setScreen(Atom atom, Point3i dest) {
    dest.x = atom.getScreenX();
    dest.y = atom.getScreenY();
    dest.z = atom.getScreenZ();
  }

  void calcAverageScreen(Atom atomA, Atom atomB, Point3i dest) {
    dest.x = (atomA.getScreenX() + atomB.getScreenX()) / 2;
    dest.y = (atomA.getScreenY() + atomB.getScreenY()) / 2;
    dest.z = (atomA.getScreenZ() + atomB.getScreenZ()) / 2;
  }

  void render1Segment(short colix, short[] mads, int i, int count) {
    int iPrev1 = i - 1; if (iPrev1 < 0) iPrev1 = 0;
    int iNext1 = i + 1; if (iNext1 > count) iNext1 = count;
    int iNext2 = i + 2; if (iNext2 > count) iNext2 = count;
    
    int madThis = mads[i];
    int madBeg = (mads[iPrev1] + madThis) / 2;
    int diameterBeg = viewer.scaleToScreen(screens[i].z, madBeg);
    int madEnd = (mads[iNext1] + madThis) / 2;
    int diameterEnd = viewer.scaleToScreen(screens[iNext1].z, madEnd);
    int diameterMid = viewer.scaleToScreen(alphas[i].getScreenZ(), madThis);
    g3d.fillHermite(colix, 7, diameterBeg, diameterMid, diameterEnd,
                    screens[iPrev1], screens[i],
                    screens[iNext1], screens[iNext2]);
  }
}

