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

class MeshRibbonRenderer extends StrandsRenderer {

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    if (wingVectors == null)
      return;
    setStrandCount(7);
    float offset = ((strandCount >> 1) * strandSeparation) + baseOffset;
    render2Strand(false, offset, offset);
    render1();
  }

  void render2Strand(boolean doFill, float offsetTop, float offsetBottom) {
    ribbonTopScreens = calcScreens(offsetTop);
    ribbonBottomScreens = calcScreens(-offsetBottom);
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i))
        renderHermiteRibbon(doFill, i, false);
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
  }
}
