
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol;

import java.awt.Color;
import java.awt.Graphics;

class ArrowLine {

  private static Color vectorColor = Color.black;

  private boolean arrowStart = false;
  private boolean arrowEnd = true;

  private double length = 0.0;
  private double scaling = 1.0;
  private double ctheta = 0.0;
  private double stheta = 0.0;

  private float x1;
  private float y1;
  private static float arrowHeadSize = 10.0f;
  private static float arrowHeadRadius = 1.0f;
  
  static int[] xpoints = new int[4];
  static int[] ypoints = new int[4];

  static void setVectorColor(Color c) {
    vectorColor = c;
  }

  static void setLengthScale(float ls) {
    arrowHeadSize = 10.0f * ls;
  }

  static float getLengthScale() {
    return arrowHeadSize / 10.0f;
  }

  static void setRadiusScale(float rs) {
    arrowHeadRadius = rs;
  }

  static float getRadiusScale() {
    return arrowHeadRadius;
  }

  public ArrowLine(Graphics gc, float x1, float y1, float x2, float y2,
          boolean arrowStart, boolean arrowEnd) {
    this(gc, x1, y1, x2, y2, arrowStart, arrowEnd,
      Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)), 1.0);
  }

   public ArrowLine(Graphics gc, float x1, float y1, float x2, float y2,
          boolean arrowStart, boolean arrowEnd, double length, double scaling) {

    this.length = length;
    this.scaling = scaling;
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
    this.x1 = x1;
    this.y1 = y1;

    double dy = y2 - y1;
    double dx = x2 - x1;
    
    double magnitude = Math.sqrt(dx*dx + dy*dy);
    
    if (Math.abs(magnitude - 0.0) < Double.MIN_VALUE) {
      return;
    }
    
    ctheta = dx / magnitude;
    stheta = dy / magnitude;

    paint(gc);
  }

  public void paint(Graphics gc) {
    gc.setColor(vectorColor);
    
    gc.drawLine((int) x1, (int) y1, (int) (x1 + length*ctheta), (int) (y1 + length*stheta));
    
    if (Math.abs(length - 0.0) < Double.MIN_VALUE) {
      return;
    }
    if (arrowStart) {
      paintArrowHead(gc, 0.0, false);
    }
    if (arrowEnd) {
      paintArrowHead(gc, length, true);
    }
  }

  public void paintArrowHead(Graphics gc, double lengthOffset, boolean forwardArrow) {
    double directionSign = 1.0;
    if (forwardArrow) {
      directionSign = -1.0;
    }
    double px = lengthOffset;
    double py = 0.0;
    double ly = scaling * arrowHeadSize * arrowHeadRadius;
    double lx = lengthOffset + directionSign * 2.0*scaling*arrowHeadSize;
    double rx = lx;
    double ry = -ly;
    double mx = lengthOffset + directionSign * 1.5*scaling*arrowHeadSize;
    double my = 0.0;

    xpoints[0] = (int) (x1 + px * ctheta - py * stheta);
    ypoints[0] = (int) (y1 + px * stheta + py * ctheta);

    xpoints[1] = (int) (x1 + lx * ctheta - ly * stheta);
    ypoints[1] = (int) (y1 + lx * stheta + ly * ctheta);

    xpoints[2] = (int) (x1 + mx * ctheta - my * stheta);
    ypoints[2] = (int) (y1 + mx * stheta + my * ctheta);

    xpoints[3] = (int) (x1 + rx * ctheta - ry * stheta);
    ypoints[3] = (int) (y1 + rx * stheta + ry * ctheta);
    gc.fillPolygon(xpoints, ypoints, 4);
  }

}

