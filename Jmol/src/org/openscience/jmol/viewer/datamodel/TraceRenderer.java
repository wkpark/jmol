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

class TraceRenderer extends MpsRenderer {

  boolean isNucleicPolymer;

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
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
    for (int i = monomerCount; --i >= 0; ) {
      if (mads[i] == 0)
        continue;
      short colix = colixes[i];
      if (colix == 0)
        colix = monomers[i].getLeadAtom().colixAtom;
      renderRopeSegment(colix, mads, i,
                        monomerCount, monomers,
                        leadMidpointScreens, null);
    }
  }

}

