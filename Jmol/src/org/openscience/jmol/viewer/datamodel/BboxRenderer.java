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
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class BboxRenderer extends ShapeRenderer {

  void render() {
    Bbox bbox = (Bbox)shape;
    short mad = bbox.mad;
    if (mad == 0)
      return;
    render(viewer, g3d, mad, bbox.colix, bbox.vertices,
           frameRenderer.getTempScreens(8));
  }

  static void render(JmolViewer viewer, Graphics3D g3d,
                     short mad, short colix,
                     Point3f vertices[], Point3i screens[]) {
    int zSum = 0;
    for (int i = 8; --i >= 0; ) {
      viewer.transformPoint(vertices[i], screens[i]);
      zSum += screens[i].z;
    }
    int widthPixels = mad;
    if (mad >= 20) {
      widthPixels = viewer.scaleToScreen(zSum / 8, mad);
    }
    for (int i = 0; i < 24; i += 2) {
      if (mad < 0)
        g3d.drawDottedLine(colix,
                           screens[Bbox.edges[i]],
                           screens[Bbox.edges[i+1]]);
      else
        g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, widthPixels,
                         screens[Bbox.edges[i]],
                         screens[Bbox.edges[i+1]]);
    }
  }
}
