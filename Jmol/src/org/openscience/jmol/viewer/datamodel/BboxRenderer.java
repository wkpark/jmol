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

  final Point3i[] bboxScreens = new Point3i[8];
  {
    for (int i = 8; --i >= 0; )
      bboxScreens[i] = new Point3i();
  }

  void render() {
    Bbox bbox = (Bbox)shape;
    short mad = bbox.mad;
    if (mad == 0)
      return;
    int zSum = 0;;
    for (int i = 8; --i >= 0; ) {
      viewer.transformPoint(bbox.bboxPoints[i], bboxScreens[i]);
      zSum += bboxScreens[i].z;
    }
    int widthPixels = 0;
    if (mad > 0)
      widthPixels = viewer.scaleToScreen(zSum / 8, mad);
    short colix = bbox.colix;
    for (int i = 0; i < 24; i += 2) {
      if (mad < 0)
        g3d.drawDottedLine(colix,
                           bboxScreens[bbox.edges[i]],
                           bboxScreens[bbox.edges[i+1]]);
      else
        g3d.fillCylinder(colix, Graphics3D.ENDCAPS_SPHERICAL, widthPixels,
                         bboxScreens[bbox.edges[i]],
                         bboxScreens[bbox.edges[i+1]]);
    }
  }
}
