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

import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import java.util.BitSet;

public class Axes extends Graphic {

  final static Point3f[] unitAxisPoints = {
    new Point3f( 1, 0, 0),
    new Point3f( 0, 1, 0),
    new Point3f( 0, 0, 1),
    new Point3f(-1, 0, 0),
    new Point3f( 0,-1, 0),
    new Point3f( 0, 0,-1)
  };

  byte mode;
  final Point3f originPoint = new Point3f();
  final Point3f[] axisPoints = new Point3f[6];
  {
    for (int i = 6; --i >= 0; )
      axisPoints[i] = new Point3f();
  }

  public void setShow(boolean show) {
    this.show = show;
    this.mode = (show ? JmolConstants.AXES_BBOX : JmolConstants.AXES_NONE);
    if (mode == JmolConstants.AXES_NONE)
      return;
    System.out.println("setting originPoint!");
    originPoint.set(viewer.getBoundingBoxCenter());
    Point3f corner = viewer.getBoundingBoxCorner();
    for (int i = 6; --i >= 0; ) {
      Point3f axisPoint = axisPoints[i];
      axisPoint.set(unitAxisPoints[i]);
      if (mode == JmolConstants.AXES_BBOX) {
        // we have just set the axisPoint to be a unit on a single axis
        // therefore only one of these values (x, y, or z) will be nonzero
        // it will have value 1 or -1
        axisPoint.x *= corner.x;
        axisPoint.y *= corner.y;
        axisPoint.z *= corner.z;
      }
      axisPoint.add(originPoint);
    }
  }

  public void setMad(short mad, BitSet bsSelected) {
  }
  
  public void setColix(byte palette, short colix, BitSet bsSelected) {
  }
}
