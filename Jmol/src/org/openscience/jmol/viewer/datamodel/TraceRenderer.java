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
    if (trace == null || trace.radius == 0)
      return;
    Atom[] alphas = frame.pdbMolecule.getAlphaCarbons();
    for (int i = alphas.length; --i > 0; ) {
      int iPrev = i - 1;
      int iPrev2 = i - 2;
      if (iPrev2 < 0) iPrev2 = 0;
      int iNext = i + 1;
      if (iNext >= alphas.length) iNext = i;
      Atom atom0 = alphas[iPrev2];
      Atom atom1 = alphas[iPrev];
      Atom atom2 = alphas[i];
      Atom atom3 = alphas[iNext];
      g3d.fillHermite(atom1.colixAtom, atom2.colixAtom, atom1.diameter,
                      atom0.x, atom0.y, atom0.z, atom1.x, atom1.y, atom1.z,
                      atom2.x, atom2.y, atom2.z, atom3.x, atom3.y, atom3.z);
    }
  }
}

