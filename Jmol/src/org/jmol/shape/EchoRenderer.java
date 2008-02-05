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

import java.util.Enumeration;
import javax.vecmath.Point3i;

public class EchoRenderer extends ShapeRenderer {

  float imageFontScaling;

  protected void render() {
    Echo echo = (Echo)shape;
    Point3i pt = new Point3i();
    Enumeration e = echo.texts.elements();
    float scalePixelsPerMicron = (viewer.getFontScaling() ? viewer.getScalePixelsPerAngstrom() * 10000 : 0);
    imageFontScaling = viewer.getImageFontScaling();
    while (e.hasMoreElements()) {
      Text t = (Text)e.nextElement();
      if (!t.visible)
        continue;
      if (t.valign == Text.VALIGN_XYZ) {
        viewer.transformPoint(t.xyz, pt);
        t.setXYZs(pt.x, pt.y, pt.z, pt.z);
      }
      t.render(g3d, scalePixelsPerMicron, imageFontScaling);
    }
    String frameTitle = viewer.getFrameTitle();
    if (frameTitle != null && frameTitle.length() > 0)
      renderFrameTitle(frameTitle);
  }
  
  private void renderFrameTitle(String frameTitle) {
    if (isGenerator || !g3d.setColix(viewer.getColixBackgroundContrast()))
      return;
    byte fid = g3d.getFontFid("Monospaced", 14 * imageFontScaling);
    g3d.setFont(fid);
    int y = (int) (viewer.getScreenHeight() - 10 * imageFontScaling);
    int x = (int) (5 * imageFontScaling);
    g3d.drawStringNoSlab(frameTitle, null, x, y, 0);
  }

  
}
