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

import java.awt.Graphics;
import javax.vecmath.Point3d;

public class VectorShape extends LineShape {

  // mth 2003 05 08
  // This code only renders the "arrowhead" part of a vector.
  // That is, it no longer draws the "shaft" of the arrow.
  // As far as I know, it has only been tested with the
  // the end of the vector, not the beginning

  boolean arrowStart;
  boolean arrowEnd;

  VectorShape(Point3d pointOrigin, Point3d pointEnd,
              boolean arrowStart, boolean arrowEnd) {
    super(pointOrigin, pointEnd);
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
  }

  public String toString() {
    return "Primitive vector shape";
  }

  public void render(Graphics g, DisplayControl control) {
    double scaling = 1.0;
    ArrowLine al = new ArrowLine(g, control, x, y, xEnd, yEnd,
                                 false,
                                 arrowStart, arrowEnd, scaling);

  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(pointOrigin);
    x = (int)screen.x;
    y = (int)screen.y;
    z = (int)screen.z;
    screen = control.transformPoint(pointEnd);
    xEnd = (int)screen.x;
    yEnd = (int)screen.y;
    zEnd = (int)screen.z;

    if (arrowEnd)
      z = zEnd;
  }
}

