/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer.datamodel;

import org.jmol.viewer.*;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

import java.util.BitSet;

public class Axes extends SelectionIndependentShape {

  final static Point3f[] unitAxisPoints = {
    new Point3f( 1, 0, 0),
    new Point3f( 0, 1, 0),
    new Point3f( 0, 0, 1),
    new Point3f(-1, 0, 0),
    new Point3f( 0,-1, 0),
    new Point3f( 0, 0,-1)
  };

  final Point3f originPoint = new Point3f();
  final Point3f[] axisPoints = new Point3f[6];
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new Point3f();
  }

  void initShape() {
    originPoint.set(viewer.getBoundingBoxCenter());
    Vector3f corner = viewer.getBoundingBoxCornerVector();
    for (int i = 6; --i >= 0; ) {
      Point3f axisPoint = axisPoints[i];
      axisPoint.set(unitAxisPoints[i]);
      // we have just set the axisPoint to be a unit on a single axis
      // therefore only one of these values (x, y, or z) will be nonzero
      // it will have value 1 or -1
      axisPoint.x *= corner.x;
      axisPoint.y *= corner.y;
      axisPoint.z *= corner.z;
      axisPoint.add(originPoint);

      colix = viewer.getColixAxes();

      font3d = g3d.getFont3D(JmolConstants.AXES_DEFAULT_FONTSIZE);
    }
  }
}
