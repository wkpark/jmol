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

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class VectorShape extends LineShape {

  // mth 2003 05 08
  // This code only renders the "arrowhead" part of a vector.
  // That is, it no longer draws the "shaft" of the arrow.
  // As far as I know, it has only been tested with the
  // the end of the vector, not the beginning

  boolean arrowStart;
  boolean arrowEnd;

  public VectorShape(Point3d pointOrigin, Point3d pointEnd) {
    super(pointOrigin, pointEnd);
    this.arrowStart = false;
    this.arrowEnd = true;
  }

  /*
  VectorShape(Point3d pointOrigin, Point3d pointEnd,
              boolean arrowStart, boolean arrowEnd) {
    super(pointOrigin, pointEnd);
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
  }
  */

  public String toString() {
    return "Primitive vector shape";
  }

  public void render(Graphics25D g25d, DisplayControl control) {
    double scaling = 1.0;
    ArrowLine al = new ArrowLine(g25d, control, x, y, z, xEnd, yEnd, zEnd,
                                 false, arrowStart, arrowEnd, scaling);

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

    if (arrowEnd)
      z = zEnd;
  }
}

