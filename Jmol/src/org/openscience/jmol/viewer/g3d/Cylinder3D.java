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

  private short colixA, colixB;
  private int[] shadesA;
  private int[] shadesB;
  private int xA, yA, zA;
  private int dxB, dyB, dzB;
  private boolean tEvenDiameter;
  private int evenCorrection;
  private int diameter;
  private byte endcaps;
  private boolean tEndcapNone;
  private int xEndcap, yEndcap, zEndcap;
  private int argbEndcap;

  private float radius, radius2, cosTheta, cosPhi, sinPhi;

  int sampleCount;
  private float[] samples = new float[32];

  public void render(short colixA, short colixB, byte endcaps, int diameter,
                     int xA, int yA, int zA,
                     int dxB, int dyB, int dzB) {
    this.diameter = diameter;
    if (diameter <= 1) {
      if (diameter == 1)
        g3d.plotLineDelta(colixA, colixB, xA, yA, zA, dxB, dyB, dzB);
      return;
    }
    this.xA = xA; this.yA = yA; this.zA = zA;
    this.dxB = dxB; this.dyB = dyB; this.dzB = dzB;
    this.shadesA = Colix.getShades(this.colixA = colixA);
    this.shadesB = Colix.getShades(this.colixB = colixB);
    this.endcaps = endcaps;
    calcArgbEndcap(true);

    generateBaseEllipse();

    if (endcaps == Graphics3D.ENDCAPS_FLAT)
      renderFlatEndcap(true);
    for (int i = rasterCount; --i >= 0; )
      plotRaster(i);
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL)
      renderSphericalEndcaps();
  }

  void generateBaseEllipse() {
    tEvenDiameter = (diameter & 1) == 0;
    //    System.out.println("diameter=" + diameter);
    radius = diameter / 2.0f;
    radius2 = radius*radius;
    int mag2d2 = dxB*dxB + dyB*dyB;
    float mag2d = (float)Math.sqrt(mag2d2);
    float mag3d = (float)Math.sqrt(mag2d2 + dzB*dzB);
    this.cosTheta = dzB / mag3d;
    this.cosPhi = dxB / mag2d;
    this.sinPhi = dyB / mag2d;
    
    calcRotatedPoint(  0f, 0);
    calcRotatedPoint(0.5f, 1);
    calcRotatedPoint(  1f, 2);
    rasterCount = 3;
    interpolate(0, 1);
    interpolate(1, 2);
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
    if (tEndcapNone) {
      g3d.plotPixelClipped(argbEndcap, xEndcap+x,  yEndcap+y, zEndcap-z-1);
      g3d.plotPixelClipped(argbEndcap, xEndcap-x,  yEndcap-y, zEndcap+z-1);
    }
    g3d.plotLineDelta(shadesA[iUp], shadesB[iUp],
                      xA + x, yA + y, zA - z,
                      dxB, dyB, dzB);
    if (endcaps != Graphics3D.ENDCAPS_SPHERICAL || dzB >= 0) {
      g3d.plotLineDelta(shadesA[iDn], shadesB[iDn],
                        xA - x, yA - y, zA + z,
                        dxB, dyB, dzB);
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

  void renderFlatEndcap(boolean tCylinder) {
    if (dzB == 0 || (!tCylinder && dzB < 0))
      return;
    int xT = xA, yT = yA, zT = zA;
    if (dzB < 0) {
      xT += dxB; yT += dyB; zT += dzB;
    }

    findMinMaxY();
    for (int y = yMin; y <= yMax; ++y) {
      findMinMaxX(y);
      /*
        System.out.println("endcap y="+y+" xMin="+xMin+" xMax="+xMax);
      */
      int count = xMax - xMin;
      g3d.plotPixelsClipped(argbEndcap, count,
                            xT + xMin,  yT + y, zT - zXMin - 2,
                            zT - zXMax - 2);
    }
  }

  void renderSphericalEndcaps() {
    g3d.fillSphereCentered(colixA, diameter, xA, yA, zA + 1);
    g3d.fillSphereCentered(colixB, diameter, xA + dxB, yA + dyB, zA + dzB + 1);
  }

  int xTip, yTip, zTip;

  public void renderCone(short colix, byte endcap, int diameter,
                         int xA, int yA, int zA,
                         int xTip, int yTip, int zTip) {
    dxB = (this.xTip = xTip) - (this.xA = xA);
    dyB = (this.yTip = yTip) - (this.yA = yA);
    dzB = (this.zTip = zTip) - (this.zA = zA);
    shadesA = Colix.getShades(this.colixA = colix);
    this.diameter = diameter;
    if (diameter <= 1) {
      if (diameter == 1)
        g3d.plotLineDelta(colixA, xA, yA, zA, dxB, dyB, dzB);
      return;
    }
    this.endcaps = endcap;
    calcArgbEndcap(false);
    generateBaseEllipse();
    if (endcaps == Graphics3D.ENDCAPS_FLAT)
      renderFlatEndcap(false);
    for (int i = rasterCount; --i >= 0; )
      plotRasterCone(i);
  }

  void plotRasterCone(int i) {
    /*
    System.out.println("plotRaster " + i + " (" + xRaster[i] + "," +
                       yRaster[i] + "," + zRaster[i] + ")" +
                       " iUp=" + iUp + " iDn=" + iDn);
    */
    int x = xRaster[i];
    int y = yRaster[i];
    int z = zRaster[i];
    int xUp = xA + x, yUp = yA + y, zUp = zA - z;
    int xDn = xA - x, yDn = yA - y, zDn = zA + z;

    if (tEndcapNone) {
      g3d.plotPixelClipped(argbEndcap, xUp, yUp, zUp);
      g3d.plotPixelClipped(argbEndcap, xDn, yDn, zDn);
    }
    int iUp = intensityUp[i], iDn = intensityDn[i];
    g3d.plotLineDelta(shadesA[iUp], shadesB[iUp],
                      xUp, yUp, zUp, xTip - xUp, yTip - yUp, zTip - zUp);
    if (! (endcaps == Graphics3D.ENDCAPS_FLAT && dzB > 0))
      g3d.plotLineDelta(shadesA[iDn], shadesB[iDn],
                        xDn, yDn, zDn, xTip - xDn, yTip - yDn, zTip - zDn);
  }

  void calcArgbEndcap(boolean tCylinder) {
    tEndcapNone = false;
    if ((endcaps == Graphics3D.ENDCAPS_SPHERICAL) ||
        (dzB == 0) ||
        (!tCylinder && dzB < 0))
      return;
    xEndcap = xA; yEndcap = yA; zEndcap = zA;
    int intensityEndcap;
    int[] shadesEndcap;
    if (dzB >= 0) {
      intensityEndcap = Shade3D.calcIntensity(-dxB, -dyB, dzB);
      shadesEndcap = shadesA;
      System.out.println("endcap is A");
    } else {
      intensityEndcap = Shade3D.calcIntensity(dxB, dyB, -dzB);
      shadesEndcap = shadesB;
      xEndcap += dxB; yEndcap += dyB; zEndcap += dzB;
      System.out.println("endcap is B");
    }
    if (intensityEndcap > 44) // limit specular glare on endcap
      intensityEndcap = 44;
    argbEndcap = shadesEndcap[intensityEndcap];
    tEndcapNone = (endcaps == Graphics3D.ENDCAPS_NONE);
    System.out.println("tEndcapNone=" + tEndcapNone);
  }
}
