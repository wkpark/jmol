/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.shape;
import org.jmol.g3d.Graphics3D;

public class FrankRenderer extends FontLineShapeRenderer {

  //we render Frank last just for the touch that if there are translucent
  //objects, then it becomes translucent. Just for fun.
  
  // no FrankGenerator
  
  protected void render() {
    Frank frank = (Frank) shape;
    if (isGenerator || !viewer.getShowFrank()
        || !g3d.setColix(Graphics3D.getColixTranslucent(Frank.defaultFontColix,
            g3d.haveTranslucentObjects(), 0.5f)))
      return;
    frank.calcMetrics();
    int dx = frank.frankWidth + Frank.frankMargin;
    int dy = frank.frankDescent;
    if (g3d.isAntialiased()) {
      dx <<= 1;
      dy <<= 1;
    }
    g3d.drawStringNoSlab(Frank.frankString, frank.font3d, (short) 0, 
        g3d.getRenderWidth() - dx, g3d.getRenderHeight() - dy, 0);
  }
}
