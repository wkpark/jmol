/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol.render;

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

//import java.awt.Graphics;
import java.awt.Rectangle;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

class LineShape extends Shape {

  Point3d pointOrigin;
  Point3d pointEnd;
  int xEnd, yEnd, zEnd;

  LineShape(Point3d pointOrigin, Point3d pointEnd) {
    this.pointOrigin = pointOrigin;
    this.pointEnd = pointEnd;
  }

  public String toString() {
    return "Primitive line shape";
  }

  public void transform(DisplayControl control) {
    Point3i screen = control.transformPoint(pointOrigin);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    screen = control.transformPoint(pointEnd);
    xEnd = screen.x;
    yEnd = screen.y;
    zEnd = screen.z;
    // z = (z + zEnd) / 2;
    if (zEnd > z)
      z = zEnd;
  }
  
  public void render(Graphics25D g25d, DisplayControl control) {
    g25d.drawLine(control.getColixVector(), x, y, z, xEnd, yEnd, zEnd);
  }
}

