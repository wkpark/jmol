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
  private int evenCorrection;
  private int diameter;
  private byte endcaps;
  private int argbEndcap;

  private float radius, radius2, cosTheta, cosPhi, sinPhi;

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
    this.endcaps = endcaps;
    if (endcaps != Graphics3D.ENDCAPS_SPHERICAL) {
      int intensityEndcap = Shade3D.calcIntensity(-dxCyl, -dyCyl, dzCyl); 
      if (intensityEndcap > 44) // limit specular glare on endcap
        intensityEndcap = 44;
      argbEndcap = shades1[intensityEndcap];
    }
    this.tEvenDiameter = (diameter & 1) == 0;
    
    //    System.out.println("diameter=" + diameter);
    radius = diameter / 2.0f;
    this.radius2 = radius*radius;
    int mag2d2 = dxCyl*dxCyl + dyCyl*dyCyl;
    float mag2d = (float)Math.sqrt(mag2d2);
    float mag3d = (float)Math.sqrt(mag2d2 + dzCyl*dzCyl);
    this.cosTheta = dzCyl / mag3d;
    this.cosPhi = dxCyl / mag2d;
    this.sinPhi = dyCyl / mag2d;

    float x, y, z, h, xR, yR;
    float yMax = radius * (float)Math.sin(Math.PI/4);

    calcRotatedPoint(  0f, 0);
    calcRotatedPoint(0.5f, 1);
    calcRotatedPoint(  1f, 2);
    rasterCount = 3;
    interpolate(0, 1);
    interpolate(1, 2);
    if (endcaps == Graphics3D.ENDCAPS_FLAT)
      renderFlatEndcap();
    for (int i = rasterCount; --i >= 0; )
      plotRaster(i);
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
  }

  void interpolate(int iLower, int iUpper) {
    int dx = xRaster[iUpper] - xRaster[iLower];
    if (dx < 0)
      dx = -dx;
    int dy = yRaster[iUpper] - yRaster[iLower];
    if (dy < 0)
      dy = -dy;
    /*
    System.out.println("interpolate(" + iLower + "," + iUpper + ")" + " -> " +
                       "dx=" + dx + " dy=" + dy);
    */
    if ((dx + dy) <= 1)
      return;
    float tLower = tRaster[iLower];
    float tUpper = tRaster[iUpper];
    int iMid = allocRaster();
    for (int j = 4; --j >= 0; ) {
      float tMid = (tLower + tUpper) / 2;
      calcRotatedPoint(tMid, iMid);
      if ((xRaster[iMid] == xRaster[iLower]) &&
          (yRaster[iMid] == yRaster[iLower])) {
        intensityUp[iLower] = (intensityUp[iLower] + intensityUp[iMid]) / 2;
        intensityDn[iLower] = (intensityDn[iLower] + intensityDn[iMid]) / 2;
        tLower = tMid;
      } else if ((xRaster[iMid] == xRaster[iUpper]) &&
               (yRaster[iMid] == yRaster[iUpper])) {
        intensityUp[iUpper] = (intensityUp[iUpper] + intensityUp[iMid]) / 2;
        intensityDn[iUpper] = (intensityDn[iUpper] + intensityDn[iMid]) / 2;
        tUpper = tMid;
      } else {
        interpolate(iLower, iMid);
        interpolate(iMid, iUpper);
        return;
      }
    }
    xRaster[iMid] = xRaster[iLower];
    yRaster[iMid] = yRaster[iUpper];
  }

  void plotRaster(int i) {
    int iUp = intensityUp[i], iDn = intensityDn[i];
    /*
    System.out.println("plotRaster " + i + " (" + xRaster[i] + "," +
                       yRaster[i] + "," + zRaster[i] + ")" +
                       " iUp=" + iUp + " iDn=" + iDn);
    */
    int x = xRaster[i];
    int y = yRaster[i];
    int z = zRaster[i];
    if (endcaps == Graphics3D.ENDCAPS_NONE) {
      g3d.plotPixelClipped(argbEndcap, xOrigin + x,  yOrigin + y, zOrigin - z);
      g3d.plotPixelClipped(argbEndcap, xOrigin - x,  yOrigin - y, zOrigin + z);
    }
    g3d.plotLineDelta(shades1[iUp], shades2[iUp],
                      xOrigin + x, yOrigin + y, zOrigin - z,
                      dxCyl, dyCyl, dzCyl);
    if (endcaps != Graphics3D.ENDCAPS_SPHERICAL || dzCyl >= 0) {
      g3d.plotLineDelta(shades1[iDn], shades2[iDn],
                        xOrigin - x, yOrigin - y, zOrigin + z,
                        dxCyl, dyCyl, dzCyl);
    }
  }

  int[] realloc(int[] a) {
    int[] t;
    t = new int[a.length * 2];
    System.arraycopy(a, 0, t, 0, a.length);
    return t;
  }

  float[] realloc(float[] a) {
    float[] t;
    t = new float[a.length * 2];
    System.arraycopy(a, 0, t, 0, a.length);
    return t;
  }

  int allocRaster() {
    if (rasterCount == xRaster.length) {
      xRaster = realloc(xRaster);
      yRaster = realloc(yRaster);
      zRaster = realloc(zRaster);
      tRaster = realloc(tRaster);
      intensityUp = realloc(intensityUp);
      intensityDn = realloc(intensityDn);
    }
    return rasterCount++;
  }

  int rasterCount;

  float[] tRaster = new float[32];
  int[] xRaster = new int[32];
  int[] yRaster = new int[32];
  int[] zRaster = new int[32];
  int[] intensityUp = new int[32];
  int[] intensityDn = new int[32];

  void calcRotatedPoint(float t, int i) {
    tRaster[i] = t;
    double tPI = t * Math.PI;
    double xT = Math.sin(tPI) * cosTheta;
    double yT = Math.cos(tPI);
    double xR = radius * (xT * cosPhi - yT * sinPhi);
    double yR = radius * (xT * sinPhi + yT * cosPhi);
    double z2 = radius2 - (xR*xR + yR*yR);
    double zR = (z2 > 0 ? Math.sqrt(z2) : 0);

    xRaster[i] = (int)xR;
    yRaster[i] = (int)yR;
    zRaster[i] = (int)(zR + 0.5);

    intensityUp[i] = Shade3D.calcIntensity((float)xR, (float)yR, (float)zR);
    intensityDn[i] = Shade3D.calcIntensity((float)-xR, (float)-yR, (float)-zR);

    /*
    System.out.println("calcRotatedPoint(" + t + "," + i + ")" + " -> " +
    xRaster[i] + "," + yRaster[i] + "," + zRaster[i]);
    */
  }

  int yMin, yMax;
  int xMin, xMax;
  int zXMin, zXMax;

  void findMinMaxY() {
    yMin = yMax = yRaster[0];
    for (int i = rasterCount; --i > 0; ) {
      int y = yRaster[i];
      if (y < yMin)
        yMin = y;
      else if (y > yMax)
        yMax = y;
      else {
        y = -y;
        if (y < yMin)
          yMin = y;
        else if (y > yMax)
          yMax = y;
      }
    }
  }

  void findMinMaxX(int y) {
    xMin = Integer.MAX_VALUE;
    xMax = Integer.MIN_VALUE;
    for (int i = rasterCount; --i >= 0; ) {
      if (yRaster[i] == y) {
        int x = xRaster[i];
        if (x < xMin) {
          xMin = x;
          zXMin = zRaster[i];
        }
        if (x > xMax) {
          xMax = x;
          zXMax = zRaster[i];
        }
        if (y == 0) {
        }
      }
      if (yRaster[i] == -y) { // 0 will run through here too
        int x = -xRaster[i];
        if (x < xMin) {
          xMin = x;
          zXMin = -zRaster[i];
        }
        if (x > xMax) {
          xMax = x;
          zXMax = -zRaster[i];
        }
      }
    }
  }

  void renderFlatEndcap() {
    findMinMaxY();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      /*
      System.out.println("endcap y=" + y + " xMin=" + xMin + " xMax=" + xMax);
      */
      int count = xMax - xMin;
      g3d.plotPixelsClipped(argbEndcap, count,
                            xOrigin + xMin,  yOrigin + y, zOrigin - zXMin - 1,
                            zOrigin - zXMax - 1);
    }
  }

  void renderSphericalEndcaps() {
    g3d.fillSphereCentered(colix1, diameter, xOrigin, yOrigin, zOrigin + 1);
    g3d.fillSphereCentered(colix2, diameter,
                           xOrigin + dxCyl, yOrigin + dyCyl, zOrigin+dzCyl+1);
  }

  public void renderCone(short colix, byte endcap, int diameter,
                         int xBase, int yBase, int zBase,
                         int xTip, int yTip, int zTip) {
  }

}
