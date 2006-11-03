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

class TraceRenderer extends MpsRenderer {

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    calcScreenControlPoints();
    if (viewer.getTestFlag1())
      renderMpspolymerMesh(mpspolymer);
    else
      render1();
  }

  void render1() {
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i))
        renderHermiteConic(i, false);
  }

  void renderMpspolymerMesh(Mps.Mpspolymer mpspolymer) {
    Trace.Tchain chain = (Trace.Tchain) mpspolymer;
    if (mpspolymer.meshes == null)
      chain.createMeshes(controlPoints);
    renderMeshes(chain);
  }

  void renderMeshes(Trace.Tchain chain) {
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i)) {
        if (!chain.meshReady[i])
          chain.createMesh(i, controlPoints);
        chain.meshes[i].colix = getLeadColix(i);
        render1(chain.meshes[i]);
      }
  }
}

