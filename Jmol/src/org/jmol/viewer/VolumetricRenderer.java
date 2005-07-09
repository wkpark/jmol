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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;


class VolumetricRenderer extends ShapeRenderer {

  final Point3i screenA = new Point3i();
  final Point3i screenB = new Point3i();

  void render() {
    System.out.println("VolumetricRenderer");
    Volumetric volumetric = (Volumetric)shape;
    int pointCount = volumetric.pointCount;
    Point3f[] points = volumetric.points;

    g3d.setColix(Graphics3D.YELLOW);
    System.out.println("pointCount=" + pointCount);
    for (int i = 0; i < pointCount; i += 2) {
      viewer.transformPoint(points[i], screenA);
      viewer.transformPoint(points[i + 1], screenB);
      g3d.drawLine(screenA, screenB);
    }
  }
}
