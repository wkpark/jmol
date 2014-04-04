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
package org.jmol.render;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Text;
import org.jmol.script.T;
import org.jmol.shape.Echo;
import org.jmol.util.C;
import org.jmol.viewer.JC;

public class EchoRenderer extends LabelsRenderer {

  @Override
  protected boolean render() {
    if (vwr.isPreviewOnly())
      return false;
    Echo echo = (Echo) shape;
    float scalePixelsPerMicron = (vwr.getBoolean(T.fontscaling) ? vwr
        .getScalePixelsPerAngstrom(true) * 10000 : 0);
    imageFontScaling = vwr.getImageFontScaling();
    boolean haveTranslucent = false;
    for (Text t: echo.objects.values()) {
      if (!t.visible || t.hidden) {
        continue;
      }
      if (t.pointerPt instanceof Atom) {
        if (!((Atom) t.pointerPt).checkVisible())
          continue;
      }
      if (t.valign == JC.VALIGN_XYZ) {
        tm.transformPtScr(t.xyz, pt0i);
        t.setXYZs(pt0i.x, pt0i.y, pt0i.z, pt0i.z);
      } else if (t.movableZPercent != Integer.MAX_VALUE) {
        int z = vwr.zValueFromPercent(t.movableZPercent);
        t.setZs(z, z);
      }
      if (t.pointerPt == null) {
        t.pointer = JC.POINTER_NONE;
      } else {
        t.pointer = JC.POINTER_ON;
        tm.transformPtScr(t.pointerPt, pt0i);
        t.atomX = pt0i.x;
        t.atomY = pt0i.y;
        t.atomZ = pt0i.z;
        if (t.zSlab == Integer.MIN_VALUE)
          t.zSlab = 1;
      }
      TextRenderer.render(t, vwr, g3d, scalePixelsPerMicron, imageFontScaling,
          false, null, xy);
     if (C.isColixTranslucent(t.bgcolix) || C.isColixTranslucent(t.colix))
       haveTranslucent = true;
    }
    if (!isExport) {
      String frameTitle = vwr.getFrameTitle();
      if (frameTitle != null && frameTitle.length() > 0) {
        if (g3d.setC(vwr.getColixBackgroundContrast())) {
          if (frameTitle.indexOf("%{") >= 0 || frameTitle.indexOf("@{") >= 0)
            frameTitle = vwr.formatText(frameTitle);
          renderFrameTitle(frameTitle);
        }
      }
    }
    return haveTranslucent;
  }
  
  private void renderFrameTitle(String frameTitle) {
    byte fid = g3d.getFontFidFS("Serif", 14 * imageFontScaling);
    g3d.setFontFid(fid);
    int y = (int) Math.floor(vwr.getScreenHeight() * (g3d.isAntialiased() ? 2 : 1) - 10 * imageFontScaling);
    int x = (int) Math.floor(5 * imageFontScaling);
    g3d.drawStringNoSlab(frameTitle, null, x, y, 0, (short) 0);
  }
}
