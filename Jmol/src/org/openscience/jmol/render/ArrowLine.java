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

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

import java.awt.Color;
//import java.awt.Graphics;

public class ArrowLine {

  private boolean arrowStart = false;
  private boolean arrowEnd = true;
  private boolean drawLine = false;

  private double scaling = 1.0;
  private double ctheta = 0.0;
  private double stheta = 0.0;

  private double x1, y1, z1;
  private double x2, y2, z2;
  private double magnitude;

  static int[] xpoints = new int[4];
  static int[] ypoints = new int[4];

  /*
  public ArrowLine(Graphics gc, double x1, double y1, double x2, double y2,
      boolean arrowStart, boolean arrowEnd) {
    this(gc, x1, y1, x2, y2, arrowStart, arrowEnd, 1.0);
  }
  */

  public ArrowLine(Graphics25D g25d, DisplayControl control,
                   double x1, double y1, double z1,
                   double x2, double y2, double z2,
                   boolean drawLine,
                   boolean arrowStart, boolean arrowEnd, double scaling) {
    this.scaling = scaling;
    this.drawLine = drawLine;
    this.arrowStart = arrowStart;
    this.arrowEnd = arrowEnd;
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    this.x2 = x2;
    this.y2 = y2;
    this.z2 = z2;

    double dy = y2 - y1;
    double dx = x2 - x1;

    magnitude = Math.sqrt(dx * dx + dy * dy);

    if (Math.abs(magnitude - 0.0) < Double.MIN_VALUE) {
      return;
    }

    ctheta = dx / magnitude;
    stheta = dy / magnitude;

    paint(g25d, control);
  }

  public void paint(Graphics25D g25d, DisplayControl control) {

    g25d.setColor(control.getColorVector());

    double arrowLengthScale = control.getArrowLengthScale();
    double arrowHeadRadius = control.getArrowHeadRadius();
    double arrowHeadSize = control.getArrowHeadSize10();
    double offset = arrowHeadSize;
    if (arrowLengthScale < 0.0) {
      offset = -offset;
    }
    if (drawLine) {
      // FIXME mth 2003 06 02
      // the z2 dimension here is wrong
      // figure out what is going on here, get rid of the doubles, and fix it
      g25d.drawLine((int) x1, (int) y1, (int) z1,
                    (int)(x1 + (offset + magnitude*arrowLengthScale)*ctheta),
                    (int)(y1 + (offset + magnitude*arrowLengthScale)*stheta),
                    (int) z2
                    );
    }
    if (arrowStart) {
      paintArrowHead(g25d, 0.0, false, arrowHeadSize, arrowHeadRadius);
    }
    if (arrowEnd) {
      paintArrowHead(g25d, offset + magnitude * arrowLengthScale, true,
                     arrowHeadSize, arrowHeadRadius);
    }
  }

  public void paintArrowHead(Graphics25D g25d, double lengthOffset,
                             boolean forwardArrow,
                             double arrowHeadSize, double arrowHeadRadius) {

    double directionSign = 1.0;
    if (forwardArrow) {
      directionSign = -1.0;
    }
    if (lengthOffset < 0.0) {
      directionSign = -directionSign;
    }
    double px = lengthOffset;
    double py = 0.0;
    double ly = scaling * arrowHeadSize * arrowHeadRadius;
    double lx = lengthOffset + directionSign * 2.0 * scaling * arrowHeadSize;
    double rx = lx;
    double ry = -ly;
    double mx = lengthOffset + directionSign * 1.5 * scaling * arrowHeadSize;
    double my = 0.0;

    xpoints[0] = (int) (x1 + px * ctheta - py * stheta);
    ypoints[0] = (int) (y1 + px * stheta + py * ctheta);

    xpoints[1] = (int) (x1 + lx * ctheta - ly * stheta);
    ypoints[1] = (int) (y1 + lx * stheta + ly * ctheta);

    xpoints[2] = (int) (x1 + mx * ctheta - my * stheta);
    ypoints[2] = (int) (y1 + mx * stheta + my * ctheta);

    xpoints[3] = (int) (x1 + rx * ctheta - ry * stheta);
    ypoints[3] = (int) (y1 + rx * stheta + ry * ctheta);
    g25d.fillPolygon(xpoints, ypoints, 4);
  }
}

