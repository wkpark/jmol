
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

class PlainLine {

  private static Color vectorColor = Color.black;


  private double ctheta = 0.0;
  private double stheta = 0.0;

  private float x1;
  private float y1;
  private float x2;
  private float y2;
  private double magnitude;

  static int[] xpoints = new int[4];
  static int[] ypoints = new int[4];

  static void setVectorColor(Color c) {
    vectorColor = c;
  }

  public PlainLine(Graphics gc, float x1, float y1, float x2, float y2) {

    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;

    double dy = y2 - y1;
    double dx = x2 - x1;

    magnitude = Math.sqrt(dx * dx + dy * dy);

    if (Math.abs(magnitude - 0.0) < Double.MIN_VALUE) {
      return;
    }

    ctheta = dx / magnitude;
    stheta = dy / magnitude;

    paint(gc);
  }

  public void paint(Graphics gc) {

    gc.setColor(vectorColor);

    gc.drawLine((int) x1, (int) y1, (int) (x1 + magnitude * ctheta),
        (int) (y1 + magnitude * stheta));

  }
}

