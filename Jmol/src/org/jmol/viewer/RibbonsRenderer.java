/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

class RibbonsRenderer extends MpsRenderer {

  void renderMpspolymer(Mps.Mpspolymer mpspolymer) {
    if (wingVectors == null)
      return;
    render1(isNucleic ? 1f : 0.5f, isNucleic ? 0f : 0.5f);
  }

  void render1(float offsetTop, float offsetBottom) {
    render2Strand(true, offsetTop, offsetBottom);
  }
  
  void render2Strand(boolean doFill, float offsetTop, float offsetBottom) {
    ribbonTopScreens = calcScreens(offsetTop);
    ribbonBottomScreens = calcScreens(-offsetBottom);
    int aspectRatio = viewer.getRibbonAspectRatio();
    for (int i = monomerCount; --i >= 0;)
      if (bsVisible.get(i))
        render2StrandSegment(doFill, i, aspectRatio);
    viewer.freeTempScreens(ribbonTopScreens);
    viewer.freeTempScreens(ribbonBottomScreens);
  }

}
