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

package org.openscience.jmol.g3d;

import org.openscience.jmol.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Cylinder3D {

  static int foo = 0;

  DisplayControl control;
  Graphics3D g3d;

  public Cylinder3D(DisplayControl control, Graphics3D g3d) {
    this.control = control;
    this.g3d = g3d;
  }

  private short colix1, colix2;
  private int[] shades1;
  private int[] shades2;
  private int xOrigin, yOrigin, zOrigin;
  private int dx, dy, dz;
  private boolean tEvenDiameter;
  private int diameter;

  private float radius2, cosTheta, cosPhi, sinPhi;
  
  private float[] samples = new float[32];

  public void render(short colix1, short colix2, int diameter,
                     int xOrigin, int yOrigin, int zOrigin,
                     int dx, int dy, int dz) {
    this.diameter = diameter;
    if (this.diameter <= 1) {
      if (this.diameter == 1)
        g3d.plotLineDelta(colix1, colix2, xOrigin, yOrigin, zOrigin, dx, dy, dz);
      return;
    }
    this.xOrigin = xOrigin; this.yOrigin = yOrigin; this.zOrigin = zOrigin;
    this.dx = dx; this.dy = dy; this.dz = dz;
    this.shades1 = Colix.getShades(this.colix1 = colix1);
    this.shades2 = Colix.getShades(this.colix2 = colix2);
    this.tEvenDiameter = (diameter & 1) == 0;
    
    float radius = diameter / 2.0f;
    this.radius2 = radius*radius;
    int mag2d2 = dx*dx + dy*dy;
    float mag2d = (float)Math.sqrt(mag2d2);
    float mag3d = (float)Math.sqrt(mag2d2 + dz*dz);
    this.cosTheta = dz / mag3d;
    this.cosPhi = dx / mag2d;
    this.sinPhi = dy / mag2d;

    int n;
    float x, y, z, h, xR, yR;
    float yMax = radius * 0.7f;

    for (n = 0, y = 1/6f; y < yMax ; y += 1/3f) {
      if (n == samples.length) {
        float[] t = new float[samples.length * 2];
        System.arraycopy(samples, 0, t, 0, samples.length);
        samples = t;
      }
      h = (float)Math.sqrt(radius2 - y*y);
      samples[n++] = h;
      samples[n++] = y;
    }

    initRotatedPoints();

    int i = 0;
    while (i < n) {
      y = -samples[i++];
      x = samples[i++] * cosTheta;
      rotateAndPlot(x, y);
    }
    while (i > 0) {
      y = -samples[--i];
      x = samples[--i] * cosTheta;
      rotateAndPlot(x, y);
    }
    while (i < n) {
      x = samples[i++] * cosTheta;
      y = samples[i++];
      rotateAndPlot(x, y);
    }
    while (i > 0) {
      x = samples[--i] * cosTheta;
      y = samples[--i];
      rotateAndPlot(x, y);
    }

    /*
    for (float y = -radius + 1/4f; y < radius; y += 1/2f) {
      float y2 = y * y;
      float h = (float)Math.sqrt(radius2 - y2);
      float x = h * cosTheta;
      float z = (float)Math.sqrt(radius2 - y2 - x*x);
      float xR = x * cosPhi - y * sinPhi;
      float yR = x * sinPhi + y * cosPhi;
      plotRotatedPoint(xR, yR, z);
    }
    */

    plotLastRotatedPoint();
    renderEndcaps();
  }

  void rotateAndPlot(float x, float y) {
    float z = (float)Math.sqrt(radius2 - x*x - y*y);
    float xR = x * cosPhi - y * sinPhi;
    float yR = x * sinPhi + y * cosPhi;
    plotRotatedPoint(xR, yR, z);
  }

  private int xLast, yLast, zLast, intensityLast, countLast;
  private boolean tBeforeLast;
  private int xBeforeLast, yBeforeLast, zBeforeLast, intensityBeforeLast;

  private void initRotatedPoints() {
    xLast = Integer.MAX_VALUE;
    countLast = 0;
    tBeforeLast = false;
  }

  private void plotRotatedPoint(float xF, float yF, float zF) {
    int x, y, z;
    if (tEvenDiameter) {
      x = (int)(xF + (xF < 0 ? -1 : 0));
      y = (int)(yF + (yF < 0 ? -1 : 0));
    } else {
      x = (int)(xF + (xF < 0 ? -0.5f : 0.5f));
      y = (int)(yF + (yF < 0 ? -0.5f : 0.5f));
    }
    z = (int)(zF + 0.5f);
    int intensity = Shade3D.calcIntensity(xF, yF, zF);
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
      //      System.out.println("plot " + xLast + "," + yLast);
      g3d.plotLineDelta(shades1[intensityLast], shades2[intensityLast],
                         xOrigin + xLast, yOrigin + yLast,
                         zOrigin - zLast,
                         dx, dy, dz);
      int xDiff = xLast - xBeforeLast;
      if (xDiff < 0)
        xDiff = -xDiff;
      int yDiff = yLast - yBeforeLast;
      if (yDiff < 0)
        yDiff = -yDiff;
      if (tBeforeLast &&
          ((xDiff > 1) || (yDiff > 1) || (xDiff != 0) && (yDiff != 0))) {
        int xInterpolate, yInterpolate;
        if (xDiff > 1 || yDiff > 1) {
          xInterpolate = (xLast + xBeforeLast) / 2;
          yInterpolate = (yLast + yBeforeLast) / 2;
        } else {
          xInterpolate = xLast;
          yInterpolate = yBeforeLast;
        }
        int zInterpolate = (zLast + zBeforeLast) / 2;
        int intensityInterpolate = (intensityLast + intensityBeforeLast) / 2;
        /*
        System.out.println(" before=" + xBeforeLast + "," + yBeforeLast +
                           " last=" + xLast + "," + yLast + " -> " +
                           xInterpolate + "," + yInterpolate);
        */
        g3d.plotLineDelta(shades1[intensityInterpolate],
                           shades2[intensityInterpolate],
                           xOrigin + xInterpolate,
                           yOrigin + yInterpolate,
                           zOrigin - zInterpolate,
                           dx, dy, dz);
      }
      tBeforeLast = true;
      xBeforeLast = xLast;
      yBeforeLast = yLast;
      zBeforeLast = zLast;
      intensityBeforeLast = intensityLast;
    }
  }

  void renderEndcaps() {
    g3d.fillSphereCentered(colix1, diameter, xOrigin, yOrigin, zOrigin);
    g3d.fillSphereCentered(colix2, diameter,
                            xOrigin+dx, yOrigin+dy, zOrigin+dz);
  }
}
