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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;

class BackboneRenderer extends MpsRenderer {

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    renderTraceChain((Backbone.Bbpolymer)mpspolymer);
  }
  
  void renderTraceChain(Backbone.Bbpolymer bbpolymer) {
    render1Chain(bbpolymer.monomerCount,
                 bbpolymer.polymer.getLeadAtomIndices(),
                 bbpolymer.mads, bbpolymer.colixes);
  }

  void render1Chain(int monomerCount, int[] atomIndices,
                    short[] mads, short[] colixes) {
    for (int i = monomerCount - 1; --i >= 0; ) {
      if (mads[i] == 0)
        continue;
      Atom atomA = frame.getAtomAt(atomIndices[i]);
      int xA = atomA.getScreenX(), yA = atomA.getScreenY(),
        zA = atomA.getScreenZ();
      Atom atomB = frame.getAtomAt(atomIndices[i + 1]);
      int xB = atomB.getScreenX(), yB = atomB.getScreenY(),
        zB = atomB.getScreenZ(); 
      short colixA = colixes[i];
      if (colixA == 0)
        colixA = atomA.colixAtom;
      short colixB = colixes[i + 1];
      if (colixB == 0)
        colixB = atomB.colixAtom;
      if (mads[i] < 0) {
        g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
      } else {
        int width = viewer.scaleToScreen((zA + zB)/2, mads[i]);
        g3d.fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_SPHERICAL,
                         width, xA, yA, zA, xB, yB, zB);
      }
    }
  }
}
