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

package org.openscience.jmol.viewer.g3d;

import org.openscience.jmol.viewer.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Cylinder3D {

  static int foo = 0;

  JmolViewer viewer;
  Graphics3D g3d;

  public Cylinder3D(JmolViewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
  }

  private short colix1, colix2;
  private int[] shades1;
  private int[] shades2;
  private int xOrigin, yOrigin, zOrigin;
  private int dxCyl, dyCyl, dzCyl;
  private boolean tEvenDiameter;
  private int diameter;

  private float radius2, cosTheta, cosPhi, sinPhi;

  int sampleCount;
  private float[] samples = new float[32];

  public void render(short colix1, short colix2, byte endcaps, int diameter,
                     int xOrigin, int yOrigin, int zOrigin,
                     int dxCyl, int dyCyl, int dzCyl) {
    this.diameter = diameter;
    if (this.diameter <= 1) {
      if (this.diameter == 1)
        g3d.plotLineDelta(colix1, colix2,
                          xOrigin, yOrigin, zOrigin, dxCyl, dyCyl, dzCyl);
      return;
    }
    this.xOrigin = xOrigin; this.yOrigin = yOrigin; this.zOrigin = zOrigin;
    this.dxCyl = dxCyl; this.dyCyl = dyCyl; this.dzCyl = dzCyl;
    this.shades1 = Colix.getShades(this.colix1 = colix1);
    this.shades2 = Colix.getShades(this.colix2 = colix2);
    this.tEvenDiameter = (diameter & 1) == 0;
    
    float radius = diameter / 2.0f;
    this.radius2 = radius*radius;
    int mag2d2 = dxCyl*dxCyl + dyCyl*dyCyl;
    float mag2d = (float)Math.sqrt(mag2d2);
    float mag3d = (float)Math.sqrt(mag2d2 + dzCyl*dzCyl);
    this.cosTheta = dzCyl / mag3d;
    this.cosPhi = dxCyl / mag2d;
    this.sinPhi = dyCyl / mag2d;

    float x, y, z, h, xR, yR;
    float yMax = radius * (float)Math.sin(Math.PI/4);

    for (sampleCount = 0, y = 1/6f; y < yMax ; y += 1/3f) {
      if (sampleCount == samples.length) {
        float[] t = new float[samples.length * 2];
        System.arraycopy(samples, 0, t, 0, samples.length);
        samples = t;
      }
      h = (float)Math.sqrt(radius2 - y*y);
      samples[sampleCount++] = h;
      samples[sampleCount++] = y;
    }

    initRotatedPoints();

    generateRotatedPoints(true);
    switch (endcaps) {
    case Graphics3D.ENDCAPS_SPHERICAL:
      reduceRecorded();
      plotRecorded(false);
      renderSphericalEndcaps();
      return;
    case Graphics3D.ENDCAPS_FLAT:
      generateRotatedPoints(false);
      reduceRecorded();
      renderFlatEndcap();
      plotRecorded(false);
      return;
    case Graphics3D.ENDCAPS_NONE:
      generateRotatedPoints(false);
      reduceRecorded();
      renderNoEndcap();
      plotRecorded(true);
      return;
    }
  }

  void generateRotatedPoints(boolean tUp) {
    int i = 0;
    float x, y;
    while (i < sampleCount) {
      y = -samples[i++];
      x = samples[i++] * cosTheta;
      rotateAndRecord(x, y, tUp);
    }
    while (i > 0) {
      y = -samples[--i];
      x = samples[--i] * cosTheta;
      rotateAndRecord(x, y, tUp);
    }
    while (i < sampleCount) {
      x = samples[i++] * cosTheta;
      y = samples[i++];
      rotateAndRecord(x, y, tUp);
    }
    while (i > 0) {
      x = samples[--i] * cosTheta;
      y = samples[--i];
      rotateAndRecord(x, y, tUp);
    }

  }

  void rotateAndRecord(float x, float y, boolean tUp) {
    float z = (float)Math.sqrt(radius2 - x*x - y*y);
    float xR = x * cosPhi - y * sinPhi;
    float yR = x * sinPhi + y * cosPhi;
    if (tUp)
      recordRotatedPoint(xR, yR, z);
    else
      recordRotatedPoint(-xR, -yR, -z);
  }

  int numRecorded;
  int[] xRecorded = new int[32];
  int[] yRecorded = new int[32];
  int[] zRecorded = new int[32];
  int[] intensityRecorded = new int[32];
  int[] countRecorded = new int[32];

  private void initRotatedPoints() {
    numRecorded = 0;
  }

  int[] doubleLength(int[] a) {
    int[] t = new int[a.length * 2];
    System.arraycopy(a, 0, t, 0, a.length);
    return t;
  }

  void doubleRecorded() {
    xRecorded = doubleLength(xRecorded);
    yRecorded = doubleLength(yRecorded);
    zRecorded = doubleLength(zRecorded);
    intensityRecorded = doubleLength(intensityRecorded);
    countRecorded = doubleLength(countRecorded);
  }

  private void recordRotatedPoint(float xF, float yF, float zF) {
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
    
    int iPrevious = numRecorded - 1;
    if (iPrevious >= 0 &&
        x == xRecorded[iPrevious] &&
        y == yRecorded[iPrevious]) {
      zRecorded[iPrevious] += z;
      intensityRecorded[iPrevious] += intensity;
      ++countRecorded[iPrevious];
    } else {
      if (numRecorded == xRecorded.length)
        doubleRecorded();
      xRecorded[numRecorded] = x;
      yRecorded[numRecorded] = y;
      zRecorded[numRecorded] = z;
      intensityRecorded[numRecorded] = intensity;
      countRecorded[numRecorded] = 1;
      ++numRecorded;
    }
  }

  void reduceRecorded() {
    // reduce any pixel that that had multiple hits
    for (int i = numRecorded; --i >= 0; ) {
      int count = countRecorded[i];
      if (count > 1) {
        int z = zRecorded[i];
        zRecorded[i] = (z + (z >= 0 ? count : -count)/2) / count;
        intensityRecorded[i] = (intensityRecorded[i] + count/2) / count;
      }
    }
  }

  void plotRecorded(boolean tPlotNegativeZ) {
    // FIXME mth - tPlotNegativeZ needs to be used for ENDCAPS_FLAT
    for (int i = 0; i < numRecorded; ++i) {
      int z = zRecorded[i];
      int x = xRecorded[i];
      int y = yRecorded[i];
      int intensity = intensityRecorded[i];
      g3d.plotLineDelta(shades1[intensity], shades2[intensity],
                         xOrigin + x, yOrigin + y,
                         zOrigin - z,
                         dxCyl, dyCyl, dzCyl);
      /*
      System.out.println("plot (" + (xOrigin + x) + "," + (yOrigin + y) + ","+
                         (zOrigin-z) + ") intensity=" + intensity);
      */
      if (i == 0)
        continue;
      int iPrev = i - 1;
      int dx = x - xRecorded[iPrev];
      int dy = y - yRecorded[iPrev];
      int dist2 = dx*dx + dy*dy;
      if (dist2 <= 1)
        continue;
      // if we jumped more than one pixel then interpolate
      int xInterpolate, yInterpolate;
      if (dx == 0 || dy == 0) {
        xInterpolate = x - dx/2;
        yInterpolate = y - dy/2;
      } else {
        xInterpolate = x;
        yInterpolate = y - dy;
      }
      int zInterpolate = (z + zRecorded[iPrev]) / 2;
      int intensityInterpolate = (intensity + intensityRecorded[iPrev]) / 2;
      /*
      System.out.println("interpolate between " + iPrev + " & " + i +
                         " (" + xInterpolate + "," + yInterpolate + "," +
                         zInterpolate + ") intensity:" + intensityInterpolate);
      */
      g3d.plotLineDelta(shades1[intensityInterpolate],
                        shades2[intensityInterpolate],
                        xOrigin + xInterpolate,
                        yOrigin + yInterpolate,
                        zOrigin - zInterpolate,
                        dxCyl, dyCyl, dzCyl);
      /*
      System.out.println("interpolate (" + (xOrigin + xInterpolate) + "," +
                         (yOrigin + yInterpolate) +  "," +
                         (zOrigin - zInterpolate) +
                         ") intensity=" + intensity);
      */
    }
  }

  void renderSphericalEndcaps() {
    g3d.fillSphereCentered(colix1, diameter, xOrigin, yOrigin, zOrigin);
    g3d.fillSphereCentered(colix2, diameter,
                            xOrigin+dxCyl, yOrigin+dyCyl, zOrigin+dzCyl);
  }

  int yMin, yMax;
  int xMin, xMax;
  int zXMin, zXMax;

  void findMinMaxY() {
    yMin = yMax = yRecorded[0];
    for (int i = numRecorded; --i > 0; ) {
      int y = yRecorded[i];
      if (y < yMin)
        yMin = y;
      else if (y > yMax)
        yMax = y;
    }
  }

  void findMinMaxX(int y) {
    xMin = Integer.MAX_VALUE;
    xMax = Integer.MIN_VALUE;
    for (int i = numRecorded; --i >= 0; ) {
      if (yRecorded[i] != y)
        continue;
      int x = xRecorded[i];
      if (x < xMin) {
        xMin = x;
        zXMin = zRecorded[i];
      }
      if (x > xMax) {
        xMax = x;
        zXMax = zRecorded[i];
      }
    }
  }

  int calcArgbEnd() {
    return shades1[Shade3D.calcIntensity(-dxCyl, -dyCyl, dzCyl)];
  }

  void renderNoEndcap() {
    // yes, when there is no endcap we do something special
    findMinMaxY();
    int argbEnd = calcArgbEnd();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      int count = xMax - xMin;
      g3d.plotPixelClipped(argbEnd, xOrigin + xMin,  yOrigin + y,
                           zOrigin - zXMin);
      if (xMax != xMin)
        g3d.plotPixelClipped(argbEnd, xOrigin + xMax,  yOrigin + y,
                             zOrigin - zXMax);
    }
  }

  void renderFlatEndcap() {
    findMinMaxY();
    int argbEnd = calcArgbEnd();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      int count = xMax - xMin;
      g3d.plotPixelsClipped(argbEnd, count, xOrigin + xMin,  yOrigin + y,
                            zOrigin - zXMin, zOrigin - zXMax);
    }
  }
}
