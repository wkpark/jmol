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

public class VectorShape extends Shape {

  Point3d pointOrigin;
  Point3d pointVector;
  double headSizeAngstroms;
  int[] ax = new int[4];
  int[] ay = new int[4];
  int[] az = new int[4];

  public VectorShape(Point3d pointOrigin, Point3d pointVector, double scale) {
    this.pointOrigin = pointOrigin;
    this.pointVector = new Point3d(pointVector);
    this.pointVector.scaleAdd(scale, pointOrigin);
    headSizeAngstroms = pointOrigin.distance(this.pointVector) / 4;
  }

  public VectorShape(Point3d pointOrigin, Point3d pointVector) {
    this(pointOrigin, pointVector, 1.0);
  }

  public void render(Graphics25D g25d, DisplayControl control) {
    short colixVector = control.getColixVector();
    int xEnd = ax[0], yEnd = ay[0], zEnd = az[0];
    g25d.drawLine(colixVector, x, y, z, xEnd, yEnd, zEnd);

    int dx = xEnd - x, xHead = xEnd - (dx / 4);
    int dy = yEnd - y, yHead = yEnd - (dy / 4);
    int mag2d = (int)(Math.sqrt(dx*dx + dy*dy) + 0.5);
    int dz = zEnd - z, zHead = zEnd - (dz / 4);
    int headSizePixels = (int)(control.scaleToScreen(zHead, headSizeAngstroms) + 0.5);

    ax[2] = xEnd - dx/6; ay[2] = yEnd - dy/6; az[2] = zEnd - dz/6;

    int dxHead, dyHead;
    if (mag2d == 0) {
      dxHead = 0;
      dyHead = headSizePixels;
    } else {
      dxHead = headSizePixels * -dy / mag2d;
      dyHead = headSizePixels * dx / mag2d;
    }

    ax[1] = xHead - dxHead/2; ax[3] = ax[1] + dxHead;
    ay[1] = yHead - dyHead/2; ay[3] = ay[1] + dyHead;
    az[1] = zHead;            az[3] = zHead;
    g25d.fillPolygon4(colixVector, ax, ay, az);
  }


  public void transform(DisplayControl control) {
    Point3i screen = control.transformPoint(pointOrigin);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    screen = control.transformPoint(pointVector);
    ax[0] = screen.x;
    ay[0] = screen.y;
    az[0] = screen.z;
  }
}

