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

//import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class Axes {

  DisplayControl control;

  OriginShape originShape;
  AxisShape[] axisShapes;

  final Point3d pointOrigin = new Point3d();
  Point3d[] unitAxisPoints = {
    new Point3d( 1, 0, 0),
    new Point3d( 0, 1, 0),
    new Point3d( 0, 0, 1),
    new Point3d(-1, 0, 0),
    new Point3d( 0,-1, 0),
    new Point3d( 0, 0,-1)
  };

  String[] axisLabels = { "+X", "+Y", "+Z",
                          null, null, null

  };

  public Axes(DisplayControl control) {
    this.control = control;

    originShape = new OriginShape();
    axisShapes = new AxisShape[6];
    for (int i = 0; i < 6; ++i)
      axisShapes[i] =
        new AxisShape(new Point3d(unitAxisPoints[i]), axisLabels[i]);
  }

  public Shape getOriginShape() {
    return originShape;
  }

  public Shape[] getAxisShapes() {
    return axisShapes;
  }

  public void recalc(byte modeAxes) {
    if (modeAxes == DisplayControl.AXES_NONE)
      return;
    pointOrigin.set(control.getBoundingBoxCenter());
    Point3d corner = control.getBoundingBoxCorner();
    for (int i = 0; i < 6; ++i) {
      Point3d axisPoint = axisShapes[i].getPoint();
      axisPoint.set(unitAxisPoints[i]);
      if (modeAxes == DisplayControl.AXES_BBOX) {
        // we have just set the axisPoint to be a unit on a single axis
        // therefor only one of these values (x, y, or z) will be nonzero
        // it will have value 1 or -1
        axisPoint.x *= corner.x;
        axisPoint.y *= corner.y;
        axisPoint.z *= corner.z;
      }
      axisPoint.add(pointOrigin);
    }
  }

  final static int xOffsetLabel = 5;
  final static int yOffsetLabel = -5;
  final static int axisFontsize = 14;

  class OriginShape extends Shape {
    public void transform(DisplayControl control) {
      Point3i screen = control.transformPoint(pointOrigin);
      x = screen.x;
      y = screen.y;
      z = screen.z;
    }

    public void render(Graphics25D g25d, DisplayControl control) {
      boolean colorSet = false;
      for (int i = 0; i < 6; ++i) {
        AxisShape axis = axisShapes[i];
        if (axis.z <= z) {
          if (!colorSet)
            g25d.setColor(control.getColorAxes());
          g25d.drawLine(x, y, z, axis.x, axis.y, axis.z);
        }
      }
    }
  }

  class AxisShape extends Shape {
    Point3d pointAxisEnd;
    String label;
    AxisShape(Point3d pointAxisEnd, String label) {
      this.pointAxisEnd = pointAxisEnd;
      this.label = label;
    }

    Point3d getPoint() {
      return pointAxisEnd;
    }

    public void transform(DisplayControl control) {
      Point3i screen = control.transformPoint(pointAxisEnd);
      x = screen.x;
      y = screen.y;
      z = screen.z;
    }
  
    public void render(Graphics25D g25d, DisplayControl control) {
      if (z > originShape.z) {
        g25d.setColor(control.getColorAxes());
        g25d.drawLine(x, y, z,
                      originShape.x, originShape.y, originShape.z);
      }
      if (label != null)
        control.renderStringOutside(label, control.getColorAxes(),
                                    axisFontsize, x, y, z);
    }

    /*
    public void render(Graphics g, DisplayControl control) {
      if (z > originShape.z) {
        g.setColor(control.getColorAxes());
        g.drawLine(x, y, originShape.x, originShape.y);
      }
      if (label != null) {
        int xLabel;
        int yLabel;
        int dx = x - originShape.x;
        int dy = y - originShape.y;
        if (dx == 0 && dy == 0) {
          xLabel = x;
          yLabel = y;
        } else {
          int dist = (int) Math.sqrt(dx*dx + dy*dy);
          xLabel = originShape.x + ((dist + axisFontsize) * dx / dist);
          yLabel = originShape.y + ((dist + 2 + axisFontsize / 2) * dy / dist);
        }
        control.renderStringOffset(label, Color.green,
                                   axisFontsize,
                                   xLabel, yLabel, 0, 0);
      }
    }
    */
  }
}
