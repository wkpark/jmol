/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class AxesRenderer extends ShapeRenderer {

  final static int axisFontsize = 14;

  String[] axisLabels = { "+X", "+Y", "+Z",
                          null, null, null };

  
  void render() {
    Axes axes = (Axes)shape;
    short mad = axes.mad;
    if (mad == 0)
      return;
    final Point3i[] axisScreens = frameRenderer.getTempScreens(7);
    final Point3i originScreen = axisScreens[6];

    viewer.transformPoint(axes.originPoint, originScreen);
    for (int i = 6; --i >= 0; )
      viewer.transformPoint(axes.axisPoints[i], axisScreens[i]);

    int widthPixels = mad;
    if (mad >= 20)
      widthPixels = viewer.scaleToScreen(originScreen.z, mad);
    short colix = axes.colix;
    for (int i = 6; --i >= 0; ) {
      if (mad < 0)
        g3d.drawDottedLine(colix, originScreen, axisScreens[i]);
      else
        g3d.fillCylinder(colix, Graphics3D.ENDCAPS_FLAT,
                         widthPixels, originScreen, axisScreens[i]);
      String label = axisLabels[i];
      if (label != null)
        frameRenderer.renderStringOutside(label, colix,
                                          axisFontsize, axisScreens[i], g3d);
    }
  }
}
