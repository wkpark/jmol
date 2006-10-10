/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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


import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.symmetry.UnitCell;

class Axes extends SelectionIndependentShape {

  
  void setProperty(String propertyName, Object value, BitSet bs) {
    if ("scale" == propertyName) {
      setScale(((Float)value).floatValue());
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  final static Point3f[] unitAxisPoints = {
    new Point3f( 1, 0, 0),
    new Point3f( 0, 1, 0),
    new Point3f( 0, 0, 1),
    new Point3f(-1, 0, 0),
    new Point3f( 0,-1, 0),
    new Point3f( 0, 0,-1)
  };

  float scale = 1f;

  final Point3f originPoint = new Point3f();
  final Point3f[] axisPoints = new Point3f[6];
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new Point3f();
  }

  final static float MIN_AXIS_LEN = 1.5f;
  void initShape() {
    font3d = g3d.getFont3D(JmolConstants.AXES_DEFAULT_FONTSIZE);
    int axesMode = viewer.getAxesMode();
    if (axesMode == JmolConstants.AXES_MODE_UNITCELL && frame.cellInfos != null) {
      UnitCell unitcell = viewer.getCurrentUnitCell();
      if (unitcell == null)
        return;
      Point3f[] vectors = unitcell.getVertices();
      Point3f offset = unitcell.getCartesianOffset();
      originPoint.set(offset);
      axisPoints[0].add(offset, vectors[4]);
      axisPoints[1].add(offset, vectors[2]);
      axisPoints[2].add(offset, vectors[1]);
      return;
    } else if (axesMode == JmolConstants.AXES_MODE_MOLECULAR) {
      originPoint.set(0, 0, 0);
    } else {
      originPoint.set(viewer.getBoundBoxCenter());
    }
    Vector3f corner = viewer.getBoundBoxCornerVector();
    for (int i = 6; --i >= 0;) {
      Point3f axisPoint = axisPoints[i];
      axisPoint.set(unitAxisPoints[i]);
      // we have just set the axisPoint to be a unit on a single axis
      // therefore only one of these values (x, y, or z) will be nonzero
      // it will have value 1 or -1
      if (corner.x < MIN_AXIS_LEN)
        corner.x = MIN_AXIS_LEN;
      if (corner.y < MIN_AXIS_LEN)
        corner.y = MIN_AXIS_LEN;
      if (corner.z < MIN_AXIS_LEN)
        corner.z = MIN_AXIS_LEN;

      axisPoint.x *= corner.x * scale;
      axisPoint.y *= corner.y * scale;
      axisPoint.z *= corner.z * scale;
      axisPoint.add(originPoint);
    }
  }
  
  void setScale(float angstroms) {
    scale = angstroms;
    initShape();
  }
}
