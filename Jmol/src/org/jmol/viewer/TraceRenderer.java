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

import org.jmol.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class TraceRenderer extends MpsRenderer {

  boolean isNucleicPolymer;
  int myVisibilityFlag;
  
  void renderMpspolymer(Mps.Mpspolymer mpspolymer, int myVisibilityFlag) {
    this.myVisibilityFlag = myVisibilityFlag;
    Trace.Tchain tchain = (Trace.Tchain)mpspolymer;
    isNucleicPolymer = tchain.polymer instanceof NucleicPolymer;
    monomerCount = tchain.monomerCount;
    if (monomerCount == 0)
      return;
    monomers = tchain.monomers;
    leadMidpoints = tchain.leadMidpoints;
    leadMidpointScreens = calcScreenLeadMidpoints(monomerCount, leadMidpoints);
    render1Chain(tchain.mads,
                 tchain.colixes);
    viewer.freeTempScreens(leadMidpointScreens);
  }

  int monomerCount;

  Monomer[] monomers;
  Point3i[] leadMidpointScreens;
  Point3f[] leadMidpoints;

  void render1Chain(short[] mads, short[] colixes) {
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || frame.bsHidden.get(monomers[i].getLeadAtomIndex()))
        continue;
      short colix = Graphics3D.inheritColix(colixes[i], monomers[i]
          .getLeadAtom().colixAtom);
      renderRopeSegment(colix, mads, i, monomerCount, monomers,
          leadMidpointScreens, null);
    }
  }

}

