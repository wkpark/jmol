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

package org.openscience.jmol.g25d;

import org.openscience.jmol.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Cylinder25D {

  static int foo = 0;

  DisplayControl control;
  Graphics25D g25d;
  public Cylinder25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  private int[] shades1;
  private int[] shades2;
  private int xOrigin, yOrigin, zOrigin;
  private int dx, dy, dz;
  
  public void render(short colix1, short colix2, int diameter,
                     int xOrigin, int yOrigin, int zOrigin,
                     int dx, int dy, int dz) {
    this.xOrigin = xOrigin; this.yOrigin = yOrigin; this.zOrigin = zOrigin;
    this.dx = dx; this.dy = dy; this.dz = dz;
    this.shades1 = Colix.getShades(colix1);
    this.shades2 = Colix.getShades(colix2);
    
    float radius = diameter / 2.0f;
    float radius2 = radius*radius;
    int mag2d2 = dx*dx + dy*dy;
    float mag2d = (float)Math.sqrt(mag2d2);
    float mag3d = (float)Math.sqrt(mag2d2 + dz*dz);
    float cosTheta = dz / mag3d;
    float cosPhi = dx / mag2d;
    float sinPhi = dy / mag2d;

    initRotatedPoints();
    for (float y = -radius + 0.25f; y < radius; y += .5) {
      float y2 = y * y;
      float h = (float)Math.sqrt(radius2 - y2);
      float x = h * cosTheta;
      float z = (float)Math.sqrt(radius2 - y2 - x*x);
      float xR = x * cosPhi - y * sinPhi;
      float yR = x * sinPhi + y * cosPhi;
      plotRotatedPoint(xR, yR, z);
    }
    plotLastRotatedPoint();
  }

  private int xLast, yLast, zLast, intensityLast, countLast;
  private boolean tPrev;
  private int xPrev, yPrev, zPrev, intensityPrev;

  private void initRotatedPoints() {
    xLast = Integer.MAX_VALUE;
    countLast = 0;
    tPrev = false;
  }

  private void plotRotatedPoint(float xF, float yF, float zF) {
    int x = (int)(xF + (xF < 0 ? -0.5f : 0.5f));
    int y = (int)(yF + (yF < 0 ? -0.5f : 0.5f));
    int z = (int)(zF + (zF < 0 ? -0.5f : 0.5f));
    int intensity = Shade25D.calcIntensity(xF, yF, zF);
    if (x == xLast && y == yLast) {
      zLast += z;
      intensityLast += intensity;
      ++countLast;
    } else {
      plotLastRotatedPoint();
      xLast = x;
      yLast = y;
      zLast = z;
      intensityLast = intensity;
      countLast = 1;
    }
  }
  
  void plotLastRotatedPoint() {
    if (countLast > 0) {
      if (countLast > 1) {
        zLast = (zLast + countLast/2) / countLast;
        intensityLast = (intensityLast + countLast/2) / countLast;
      }
      g25d.plotLineDelta(shades1[intensityLast], shades2[intensityLast],
                         xOrigin + xLast, yOrigin + yLast,
                         zOrigin - zLast,
                         dx, dy, dz);

      if (tPrev && xLast != xPrev && yLast != yPrev) {
        int zInterpolate = (zLast + zPrev) / 2;
        int intensityInterpolate = (intensityLast + intensityPrev) / 2;
        // FIXME ... current interpolation is not technically "correct"
        int xInterpolate, yInterpolate;
        xInterpolate = xLast;
        yInterpolate = yPrev;
        g25d.plotLineDelta(shades1[intensityInterpolate],
                           shades2[intensityInterpolate],
                           xOrigin + xInterpolate,
                           yOrigin + yInterpolate,
                           zOrigin - zInterpolate,
                           dx, dy, dz);
      }
      tPrev = true;
      xPrev = xLast;
      yPrev = yLast;
      zPrev = zLast;
      intensityPrev = intensityLast;
    }
  }
}
