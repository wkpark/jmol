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
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.g3d.Shade3D;
import java.awt.Rectangle;

class TraceRenderer extends Renderer {

  TraceRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    Trace trace = frame.trace;
    if (trace == null || !trace.initialized)
      return;
    Atom[][] chains = trace.chains;
    short[][] marsChains = trace.marsChains;
    short[][] colixesChains = trace.colixesChains;
    for (int i = chains.length; --i >= 0; ) {
      Atom[] alphas = chains[i];
      short[] mars = marsChains[i];
      short[] colixes = colixesChains[i];
      for (int j = alphas.length; --j > 0; ) {
        int jPrev = j - 1;
        int jPrev2 = j - 2;
        if (jPrev2 < 0) jPrev2 = 0;
        int jNext = j + 1;
        if (jNext >= alphas.length) jNext = j;
        Atom atom0 = alphas[jPrev2];
        Atom atom1 = alphas[jPrev];
        Atom atom2 = alphas[j];
        Atom atom3 = alphas[jNext];
        short colix = colixes[jPrev];
        if (colix == 0)
          colix = atom1.colixAtom;
        int mad = mars[jPrev] * 2;
        int diameter1 = viewer.scaleToScreen(atom1.z, mad);
        int diameter2 = viewer.scaleToScreen(atom2.z, mad);
        g3d.fillHermite(colix, diameter1, diameter2,
                        atom0.x, atom0.y, atom0.z, atom1.x, atom1.y, atom1.z,
                        atom2.x, atom2.y, atom2.z, atom3.x, atom3.y, atom3.z);
      }
    }
  }
}

