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
import org.jmol.g3d.Graphics3D;

import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class AxesRenderer extends ShapeRenderer {

  String[] axisLabels = { "+X", "+Y", "+Z",
                          null, null, null };

  final Point3i[] axisScreens = new Point3i[6];
  {
    for (int i = 6; --i >= 0; )
      axisScreens[i] = new Point3i();
  }
  final Point3i originScreen = new Point3i();

  void render() {
    Axes axes = (Axes)shape;
    short mad = axes.mad;
    if (mad == 0)
      return;

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
        frameRenderer.renderStringOutside(label, colix, axes.font3d,
                                          axisScreens[i], g3d);
    }
  }
}
