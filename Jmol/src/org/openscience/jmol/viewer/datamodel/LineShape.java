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
package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;

import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class LineShape extends Shape {

  Point3f pointOrigin;
  Point3f pointEnd;
  int xEnd, yEnd, zEnd;

  public LineShape() {
  }

  public LineShape(Point3f pointOrigin, Point3f pointEnd) {
    this.pointOrigin = pointOrigin;
    this.pointEnd = pointEnd;
  }

  public Point3f getPoint1() {
    return pointOrigin;
  }

  public Point3f getPoint2() {
    return pointEnd;
  }

  public String toString() {
    return "Primitive line shape";
  }

  public void transform(JmolViewer viewer) {
    Point3i screen = viewer.transformPoint(pointOrigin);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    screen = viewer.transformPoint(pointEnd);
    xEnd = screen.x;
    yEnd = screen.y;
    zEnd = screen.z;
    // z = (z + zEnd) / 2;
    if (zEnd > z)
      z = zEnd;
  }
  
  public void render(Graphics3D g3d, JmolViewer viewer) {
    g3d.drawLine(viewer.getColixVector(), x, y, z, xEnd, yEnd, zEnd);
  }
}

