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
package org.openscience.jmol.render;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.g25d.Graphics25D;

import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class BoundingBox {

  DisplayControl control;

  Point3d pointOrigin;
  Point3d pointCorner;
  BboxShape[] bboxShapes;


  Point3d[] unitBboxPoints = {
    new Point3d( 1, 1, 1),
    new Point3d( 1, 1,-1),
    new Point3d( 1,-1, 1),
    new Point3d( 1,-1,-1),
    new Point3d(-1, 1, 1),
    new Point3d(-1, 1,-1),
    new Point3d(-1,-1, 1),
    new Point3d(-1,-1,-1),
  };

  public BoundingBox(DisplayControl control) {
    this.control = control;

    bboxShapes = new BboxShape[8];
    for (int i = 0; i < 8; ++i)
      bboxShapes[i] =
        new BboxShape(new Point3d(unitBboxPoints[i]), i);
  }

  public Shape[] getBboxShapes() {
    return bboxShapes;
  }

  public void recalc() {
    Point3d pointOrigin = control.getBoundingBoxCenter();
    Point3d pointCorner = control.getBoundingBoxCorner();
    for (int i = 0; i < 8; ++i) {
      Point3d pointBbox = bboxShapes[i].getPoint();
      pointBbox.set(unitBboxPoints[i]);
      pointBbox.x *= pointCorner.x;
      pointBbox.y *= pointCorner.y;
      pointBbox.z *= pointCorner.z;
      pointBbox.add(pointOrigin);
    }
  }

  class BboxShape extends Shape {
    Point3d point;
    int myIndex;

    BboxShape(Point3d point, int myIndex) {
      this.point = point;
      this.myIndex = myIndex;
    }

    Point3d getPoint() {
      return point;
    }

    public void transform(DisplayControl control) {
      Point3i screen = control.transformPoint(point);
      x = screen.x;
      y = screen.y;
      z = screen.z;
    }
  
    public void render(Graphics25D g25d, DisplayControl control) {
      // the points I am connected with are at the indices obtained
      // by XORing each of the three bits of my index
      short colix = control.getColixAxes();
      for (int i = 1; i <= 4; i <<= 1) {
        int indexOther = myIndex ^ i;
        BboxShape bboxOther = bboxShapes[indexOther];
        if (z > bboxOther.z || (z == bboxOther.z && myIndex > indexOther))
          g25d.drawLine(colix, x, y, z, bboxOther.x, bboxOther.y, bboxOther.z);
      }
    }
  }
}

