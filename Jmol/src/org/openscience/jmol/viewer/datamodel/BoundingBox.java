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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;

import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class BoundingBox {

  JmolViewer viewer;

  Point3f pointOrigin;
  Point3f pointCorner;
  BboxShape[] bboxShapes;


  Point3f[] unitBboxPoints = {
    new Point3f( 1, 1, 1),
    new Point3f( 1, 1,-1),
    new Point3f( 1,-1, 1),
    new Point3f( 1,-1,-1),
    new Point3f(-1, 1, 1),
    new Point3f(-1, 1,-1),
    new Point3f(-1,-1, 1),
    new Point3f(-1,-1,-1),
  };

  public BoundingBox(JmolViewer viewer) {
    this.viewer = viewer;

    bboxShapes = new BboxShape[8];
    for (int i = 0; i < 8; ++i)
      bboxShapes[i] =
        new BboxShape(new Point3f(unitBboxPoints[i]), i);
  }

  public Shape[] getBboxShapes() {
    return bboxShapes;
  }

  public void recalc() {
    Point3f pointOrigin = viewer.getBoundingBoxCenter();
    Point3f pointCorner = viewer.getBoundingBoxCorner();
    for (int i = 0; i < 8; ++i) {
      Point3f pointBbox = bboxShapes[i].getPoint();
      pointBbox.set(unitBboxPoints[i]);
      pointBbox.x *= pointCorner.x;
      pointBbox.y *= pointCorner.y;
      pointBbox.z *= pointCorner.z;
      pointBbox.add(pointOrigin);
    }
  }

  class BboxShape extends Shape {
    Point3f point;
    int myIndex;

    BboxShape(Point3f point, int myIndex) {
      this.point = point;
      this.myIndex = myIndex;
    }

    Point3f getPoint() {
      return point;
    }

    public void transform(JmolViewer viewer) {
      Point3i screen = viewer.transformPoint(point);
      x = screen.x;
      y = screen.y;
      z = screen.z;
    }
  
    public void render(Graphics3D g3d, JmolViewer viewer) {
      // the points I am connected with are at the indices obtained
      // by XORing each of the three bits of my index
      short colix = viewer.getColixAxes();
      for (int i = 1; i <= 4; i <<= 1) {
        int indexOther = myIndex ^ i;
        BboxShape bboxOther = bboxShapes[indexOther];
        if (z > bboxOther.z || (z == bboxOther.z && myIndex > indexOther))
          g3d.drawLine(colix, x, y, z, bboxOther.x, bboxOther.y, bboxOther.z);
      }
    }
  }
}

