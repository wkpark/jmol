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

import org.openscience.jmol.DisplayControl;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;
import java.util.Hashtable;


final public class ShadedBondRenderer {

  DisplayControl control;
  Graphics25D g25d;

  public ShadedBondRenderer(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  int[] axCylinder, ayCylinder, azCylinder;

  void render(Color color, int[] ax, int[] ay, int[] az) {
    axCylinder = ax; ayCylinder = ay; azCylinder = az;
    boolean firstPass = true;
    Color[] shades = getBondShades(color, Color.black);
    int numPasses = calcNumShadeSteps();
    for (int i = numPasses; --i >= 0; ) {
      Color shade = shades[i * maxShade / numPasses];
      if (firstPass) {
        drawInside(shade, 2, ax, ay, az);
        firstPass = false;
      } else {
        stepPolygon();
      }
      g25d.fillPolygon4(shade, ax, ay, az);
    }
  }

  void drawInside(Color color, int width,
                  int[] ax, int[] ay, int[] az) {
    // mth dec 2002
    // I am not happy with this implementation, but for now it is the most
    // effective kludge I could come up with to deal with the fact that
    // the brush is offset to the lower right of the point when drawing
    if (color == null)
      return;
    g25d.setColor(color);
    int iNW = 0;
    int iNE = 1;
    int iSE = 2;
    int iSW = 3;
    int iT;
    boolean top = false;
    if (ax[iNE] < ax[iNW]) {
      iT = iNE; iNE = iNW; iNW = iT;
      iT = iSE; iSE = iSW; iSW = iT;
      top = !top;
    }
    drawInside1(top, ax[iNW], ay[iNW], az[iNW],
                ax[iNE], ay[iNE], az[iNE]);
    if (width > 1)
      drawInside1(!top, ax[iSW], ay[iSW], az[iSW],
                  ax[iSE], ay[iSE], az[iSE]);
  }

  private final static boolean applyDrawInsideCorrection = true;
  void drawInside1(boolean top,
                   int x1, int y1, int z1, int x2, int y2, int z2) {
    if (!applyDrawInsideCorrection) {
      drawLineInside(x1, y1, z1, x2, y2, z2);
      return;
    }
    int dx = x2 - x1, dy = y2 - y1;
    if (dy >= 0) {
      if (dy == 0) {
        if (top) {
          --x2;
        } else {
          --y1; --x2; --y2;
        }
      } else if (3*dy < dx) {
        if (top) {
          ++y1; --x2;
        } else {
          --x2; --y2;
        }
      } else if (dy < dx) {
        if (! top) {
          --x2; --y2;
        }
      } else if (dx == 0) {
        if (top) {
          --x1; --x2; --y2;
        } else {
          --y2;
        }
      } else if (3*dx < dy) {
        if (top) {
          --x1; --x2; --y2;
        } else {
          --y2;
        }
      } else if (dx == dy) {
        if (top) {
          ++y1; --x2;
          g25d.drawLine(x1, y1, z1, x2, y2, z2);
          --x1; --x2;
        } else {
          g25d.drawLine(x1+1, y1, z1, x2, y2-1, z2);
          --x2; --y2;
        }
      }
    } else {
      if (dx == 0) {
        if (top) {
          --y1;
        } else {
          --x1; --y1; --x2;
        }
      } else if (3*dx < -dy) {
        if (top) {
          --y1;
        } else {
          --x1; --y1; --x2;
        }
      } else if (dx > -dy*3) {
        if (top){
          --x2; ++y2;
        } else {
          --y1; --x2;
        }
      } else if (dx == -dy) {
        if (!top) {
          --x2; ++y2;
        }
      }
    }
    g25d.drawLine(x1, y1, z1, x2, y2, z2);
  }

  private final static boolean applyLineInsideCorrection = true;

  void drawLineInside(int x1, int y1, int z1, int x2, int y2, int z2) {
    if (applyLineInsideCorrection) {
      if (x2 < x1) {
        int xT = x1; x1 = x2; x2 = xT;
        int yT = y1; y1 = y2; y2 = yT;
        int zT = z1; z1 = z2; z2 = zT;
      }
      int dx = x2 - x1, dy = y2 - y1;
      if (dy >= 0) {
        if (dy <= dx)
          --x2;
        if (dx <= dy)
          --y2;
      } else {
        if (-dy <= dx)
          --x2;
        if (dx <= -dy)
          --y1;
      }
    }
    g25d.drawLine(x1, y1, z1, x2, y2, z2);
  }

  private final Hashtable htShades = new Hashtable();
  private final static int maxShade = 16;
  Color[] getBondShades(Color color, Color darker) {
    Color[] shades = (Color[])htShades.get(color);
    if (shades == null) {
      int colorR = color.getRed();
      int colorG = color.getGreen();
      int colorB = color.getBlue();
      colorR += colorR / 12; if (colorR > 255) colorR = 255;
      colorG += colorG / 12; if (colorG > 255) colorG = 255;
      colorB += colorB / 12; if (colorB > 255) colorB = 255;
      int darkerR = darker.getRed(),   rangeR = colorR - darkerR;
      int darkerG = darker.getGreen(), rangeG = colorG - darkerG;
      int darkerB = darker.getBlue(),  rangeB = colorB - darkerB;
      shades = new Color[maxShade];
      for (int i = 0; i < maxShade; ++i) {
        double distance = (float)i / (maxShade - 1);
        double percentage = Math.sqrt(1 - distance);
        int r = darkerR + (int)(percentage * rangeR);
        int g = darkerG + (int)(percentage * rangeG);
        int b = darkerB + (int)(percentage * rangeB);
        int rgb = 0xFF << 24 | r << 16 | g << 8 | b;
        shades[i] = new Color(rgb);
      }
      htShades.put(color, shades);
    }
    return shades;
  }

  int xL, yL, dxL, dyL;
  int dxLTop, dyLTop, dxLBot, dyLBot;
  int xR, yR, dxR, dyR;
  int dxRTop, dyRTop, dxRBot, dyRBot;
  int step, lenMax;

  int calcNumShadeSteps() {
    int dxSlope = axCylinder[1] - axCylinder[0];
    int dySlope = ayCylinder[1] - ayCylinder[0];
    calcLightPoint(dxSlope, dySlope);
    if (dxSlope < 0) dxSlope = -dxSlope;
    if (dySlope < 0) dySlope = -dySlope;

    xL = axCylinder[0]; yL = ayCylinder[0];
    dxL = axCylinder[3] - xL; dyL = ayCylinder[3] - yL;
    int lenL = (int)Math.sqrt(dxL*dxL + dyL*dyL);
    int lenLTop = lenL * pctLight / 100;
    int lenLBot = lenL - lenLTop;
    dxLTop = dxL * pctLight / 100;
    dxLBot = dxL - dxLTop;
    dyLTop = dyL * pctLight / 100;
    dyLBot = dyL - dyLTop;

    xR = axCylinder[1]; yR = ayCylinder[1];
    dxR = axCylinder[2] - xR; dyR = ayCylinder[2] - yR;
    int lenR = (int)Math.sqrt(dxR*dxR + dyR+dyR);
    int lenRTop = lenR * pctLight / 100;
    int lenRBot = lenR - lenRTop;
    dxRTop = dxR * pctLight / 100;
    dxRBot = dxR - dxRTop;
    dyRTop = dyR * pctLight / 100;
    dyRBot = dyR - dyRTop;

    step = 0;
    lenMax = Math.max(Math.max(lenLTop, lenLBot), Math.max(lenRTop, lenRBot));
    if (lenMax < 1)
      control.logError("BondRenderer calculation error #3465 :^)");
    return lenMax;
  }

  void stepPolygon() {
    ++step;
    int dxStepLTop = dxLTop * step / lenMax;
    int dyStepLTop = dyLTop * step / lenMax;
    int dxStepLBot = dxLBot * step / lenMax;
    int dyStepLBot = dyLBot * step / lenMax;

    int dxStepRTop = dxRTop * step / lenMax;
    int dyStepRTop = dyRTop * step / lenMax;
    int dxStepRBot = dxRBot * step / lenMax;
    int dyStepRBot = dyRBot * step / lenMax;

    axCylinder[0] = xL + dxStepLTop;
    ayCylinder[0] = yL + dyStepLTop;
    axCylinder[1] = xR + dxStepRTop;
    ayCylinder[1] = yR + dyStepRTop;
    axCylinder[2] = xR + dxR - dxStepRBot;
    ayCylinder[2] = yR + dyR - dyStepRBot;
    axCylinder[3] = xL + dxL - dxStepLBot;
    ayCylinder[3] = yL + dyL - dyStepLBot;
  }

  int pctLight = 50;

  void calcLightPoint(int dxSlope, int dySlope) {
    /*
      mth
      Well, I tried for a while and could not figure it out,
      maybe some other day ... or somebody else

      we need to calculate the factor as a percentage of where
      the brightest spot on the bond cylinder is located

      I guess that I also need to know the orientation in the
      z dimension to be able to calculate this ... maybe that
      is my problem
      
    double mag = Math.sqrt(dxSlope*dxSlope + dySlope*dySlope);
    double cos = dxSlope / mag;
    double angle = Math.acos(cos);
    angle += Math.PI / 4;
    factorTop = 50 + (int)(Math.cos(angle) * 40);
    System.out.println(" dxSlope="+dxSlope+
                       " dySlope="+dySlope+
                       " factorTop="+factorTop);
    */
  }


}

