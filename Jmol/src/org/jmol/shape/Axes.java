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
package org.jmol.shape;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.symmetry.UnitCell;
import org.jmol.viewer.JmolConstants;

public class Axes extends FontLineShape {

  
  private final static Point3f[] unitAxisPoints = {
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

  private final static float MIN_AXIS_LEN = 1.5f;
  
  public void initShape() {
    super.initShape();
    myType = "axes";
    font3d = g3d.getFont3D(JmolConstants.AXES_DEFAULT_FONTSIZE);
    int axesMode = viewer.getAxesMode();
    if (axesMode == JmolConstants.AXES_MODE_UNITCELL && modelSet.getCellInfos() != null) {
      UnitCell unitcell = viewer.getCurrentUnitCell();
      if (unitcell == null)
        return;
      Point3f[] vectors = unitcell.getVertices();
      Point3f offset = unitcell.getCartesianOffset();
      originPoint.set(offset);
      float scale = viewer.getAxesScale() / 2;
      // We must divide by 2 because that is the default for ALL axis types.
      // Not great, but it will have to do. 
      axisPoints[0].scaleAdd(scale, vectors[4], offset);
      axisPoints[1].scaleAdd(scale, vectors[2], offset);
      axisPoints[2].scaleAdd(scale, vectors[1], offset);
      //axisPoints[0].add(offset, vectors[4]);
      //axisPoints[1].add(offset, vectors[2]);
      //axisPoints[2].add(offset, vectors[1]);
      return;
    } else if (axesMode == JmolConstants.AXES_MODE_MOLECULAR) {
      originPoint.set(0, 0, 0);
    } else {
      originPoint.set(viewer.getBoundBoxCenter());
    }
    setScale(viewer.getAxesScale());
  }
  
  public Object getProperty(String property, int index) {
    if (property.equals("axisPoints"))
      return axisPoints;
    return null;
  }

  void setScale(float scale) {
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
  
 public String getShapeState() {
    return super.getShapeState() + "  axisScale = " + viewer.getAxesScale() + ";\n";
  }

}
