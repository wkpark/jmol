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

import java.awt.Rectangle;
import java.awt.Color;
import java.util.Hashtable;

public class BondRenderer {

  DisplayControl control;
  public BondRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics25D g25d;
  Rectangle clip;

  public void setGraphicsContext(Graphics25D g25d, Rectangle clip) {
    this.g25d = g25d;
    this.clip = clip;

    fastRendering = control.getFastRendering();
    showAtoms = control.getShowAtoms();
    colorSelection = control.getColorSelection();
    showMultipleBonds = control.getShowMultipleBonds();
    modeMultipleBond = control.getModeMultipleBond();
    showAxis = control.getDebugShowAxis();
  }

  boolean fastRendering;
  boolean showAtoms;
  Color colorSelection;
  boolean showMultipleBonds;
  byte modeMultipleBond;
  boolean showAxis;

  int x1, y1, z1;
  int x2, y2, z2;
  int dx, dy, dz;
  int dx2, dy2, dz2;
  int mag2d, mag2d2, halfMag2d;
  int mag3d, mag3d2;
  Color color1, color2;
  boolean sameColor;
  int radius1, diameter1;
  int radius2, diameter2;
  int width1, width2;
  Color outline1, outline2;
  byte styleAtom1, styleAtom2;
  int bondOrder;
  byte styleBond;
  short marBond;
  
  private void renderHalo() {
    int diameter = (width1 + width2 + 1) / 2;
    int x = (x1 + x2) / 2, y = (y1 + y2) / 2, z = (z1 + z2) / 2;
    int halowidth = diameter / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int halodiameter = diameter + 2 * halowidth;
    int haloradius = (halodiameter + 1) / 2;
    g25d.setColor(colorSelection);
    g25d.fillCircle(x - haloradius, y - haloradius, z, halodiameter);
  }

  public void render(AtomShape atomShape1, int index1,
                     AtomShape atomShape2, int index2,
                     int order) {
    styleAtom1 = atomShape1.styleAtom;
    styleAtom2 = atomShape2.styleAtom;
    styleBond = atomShape1.styleBonds[index1];
    marBond = atomShape1.marBonds[index1];
    x1 = atomShape1.x; y1 = atomShape1.y; z1 = atomShape1.z;
    x2 = atomShape2.x; y2 = atomShape2.y; z2 = atomShape2.z;
    width1 = atomShape1.bondWidths[index1];
    width2 = atomShape2.bondWidths[index2];
    if (width1 < 4 && width2 < 4) {
      // to smooth out narrow bonds
        width1 = width2 = (width1 + width2) / 2;
    }

    color1 = atomShape1.colorBonds[index1];
    if (color1 == null)
      color1 = atomShape1.colorAtom;
    color2 = atomShape2.colorBonds[index2];
    if (color2 == null)
      color2 = atomShape2.colorAtom;
    sameColor = color1.equals(color2);

    if (!showAtoms) {
      diameter1 = diameter2 = 0;
    } else {
      diameter1 =
        (styleAtom1 == DisplayControl.NONE) ? 0 : atomShape1.diameter;
      diameter2 =
        (styleAtom2 == DisplayControl.NONE) ? 0 : atomShape2.diameter;
    }

    bondOrder = getRenderBondOrder(order);

    if (control.hasSelectionHalo(atomShape1.atom, index1))
      renderHalo();
    if (styleBond != DisplayControl.NONE)
      renderBond();
  }

  int getRenderBondOrder(int order) {
    if (order == 1 ||
        !showMultipleBonds ||
        modeMultipleBond == DisplayControl.MB_NEVER ||
        (modeMultipleBond == DisplayControl.MB_SMALL &&
         marBond > DisplayControl.marMultipleBondSmallMaximum))
      return 1;
    return order;
  }

  private void renderBond() {
    dx = x2 - x1; dx2 = dx * dx;
    dy = y2 - y1; dy2 = dy * dy;
    dz = z2 - z1; dz2 = dz * dz;
    mag2d2 = dx2 + dy2;
    mag3d2 = mag2d2 + dz2;
    if (mag2d2 <= 2 || mag2d2 <= 49 && fastRendering)
      return; // also avoids divide by zero when magnitude == 0
    if (showAtoms && (mag2d2 <= 16))
      return; // the pixels from the atoms will nearly cover the bond
    if (!showAtoms && bondOrder == 1 &&
        (fastRendering || styleBond == control.WIREFRAME)) {
      g25d.setColor(color1);
      if (sameColor) {
        drawLineInside(g25d, x1, y1, z1, x2, y2, z2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        int zMid = (z1 + z2) / 2;
        drawLineInside(g25d, x1, y1, z1, xMid, yMid, zMid);
        g25d.setColor(color2);
        drawLineInside(g25d, xMid, yMid, zMid, x2, y2, z2);
      }
      return;
    }
    radius1 = diameter1 >> 1;
    radius2 = diameter2 >> 1;
    mag2d = (int)Math.sqrt(mag2d2);
    // if the front atom (radius1) has completely obscured the bond, stop
    if (radius1 >= mag2d)
      return;
    halfMag2d = mag2d / 2;
    mag3d = (int)Math.sqrt(mag3d2);
    int radius1Bond = radius1 * mag2d / mag3d;
    int radius2Bond = radius2 * mag2d / mag3d;

    outline1 = control.getColorAtomOutline(styleBond, color1);
    outline2 = control.getColorAtomOutline(styleBond, color2);

    this.bondOrder = bondOrder;

    boolean lineBond =
      styleBond == control.WIREFRAME || fastRendering;
    if (!lineBond && width1 < 2) {
      // if the bonds are narrow ...
      // just draw lines that are the color of the outline
      color1 = outline1;
      color2 = outline2;
      lineBond = true;
    }
    resetAxisCoordinates(lineBond);
    while (true) {
      if (lineBond)
        lineBond();
      else
        polyBond(styleBond);
      if (--bondOrder == 0)
        break;
      stepAxisCoordinates();
    }
    if (showAxis) {
      g25d.setColor(control.transparentGreen());
      g25d.drawLine(x1 + 5, y1, z1, x1 - 5, y1, z1);
      g25d.drawLine(x1, y1 + 5, z1, x1, y1 - 5, z1);
      g25d.drawCircle(x1-5, y1-5, z1, 10);
      g25d.drawLine(x2 + 5, y2, z2, x2 - 5, y2, z2);
      g25d.drawLine(x2, y2 + 5, z2, x2, y2 - 5, z2);
      g25d.drawCircle(x2-5, y2-5, z2, 10);
    }
  }

  void initializeDebugColors() {
  }

  int[] axPoly = new int[4];
  int[] ayPoly = new int[4];
  int[] azPoly = new int[4];
  int xExit, yExit, zExit;

  void lineBond() {
    calcMag2dLine();
    calcSurfaceIntersections();
    calcExitPoint();
    if (sameColor || distanceExit >= mag2dLine / 2 ) {
      if (distanceExit + distanceSurface2 >= mag2dLine)
        return;
      g25d.setColor(color2);
      drawLineInside(g25d, xExit, yExit, zExit,
                     xSurface2, ySurface2, zSurface2);
      return;
    }
    int xMid = (xAxis1 + xAxis2) / 2;
    int yMid = (yAxis1 + yAxis2) / 2;
    int zMid = (zAxis1 + zAxis2) / 2;
    g25d.setColor(color1);
    drawLineInside(g25d, xExit, yExit, zExit, xMid, yMid, zMid);
    g25d.setColor(color2);
    drawLineInside(g25d, xMid, yMid, zMid, xSurface2, ySurface2, zSurface2);
  }


  int serial = 0;

  void polyBond(byte styleBond) {
    boolean bothColors = !sameColor;

    xAxis1 -= dxHalf1; yAxis1 -= dyHalf1;
    xAxis2 -= dxHalf2; yAxis2 -= dyHalf2;
    offsetAxis2 -= half2;
    
    calcMag2dLine();
    calcSurfaceIntersections();
    calcExitPoint();
    int xExitTop = xExit, yExitTop = yExit;
    int xMidTop = (xAxis1 + xAxis2) / 2, yMidTop = (yAxis1 + yAxis2) / 2;
    int xSurfaceTop = xSurface2, ySurfaceTop = ySurface2;
    if (distanceExit >= mag2dLine / 2) {
      bothColors = false;
      if (distanceExit + distanceSurface2 >= mag2dLine)
        return;
    }

    
    xAxis1 += dxWidth1; yAxis1 += dyWidth1;
    xAxis2 += dxWidth2; yAxis2 += dyWidth2;
    offsetAxis2 += width2;

    calcMag2dLine();
    calcSurfaceIntersections();
    calcExitPoint();
    int xExitBot = xExit, yExitBot = yExit;
    int xMidBot = (xAxis1 + xAxis2) / 2, yMidBot = (yAxis1 + yAxis2) / 2;
    int xSurfaceBot = xSurface2, ySurfaceBot = ySurface2;

    // now, restore the axis points to their proper position
    xAxis1 -= dxOtherHalf1; yAxis1 -= dyOtherHalf1;
    xAxis2 -= dxOtherHalf2; yAxis2 -= dyOtherHalf2;
    offsetAxis2 -= otherHalf2;

    if (distanceExit >= mag2dLine / 2) {
      bothColors = false;
      if (distanceExit + distanceSurface2 >= mag2dLine)
        return;
    }

    // draw endcaps here, centered at axis1, axis2
    drawEndCaps();

    if (! bothColors) {
      if (distanceExit < mag2dLine) {
        axPoly[0] = xExitTop; ayPoly[0] = yExitTop;
        axPoly[1] = xSurfaceTop; ayPoly[1] = ySurfaceTop;
        axPoly[2] = xSurfaceBot; ayPoly[2] = ySurfaceBot;
        axPoly[3] = xExitBot; ayPoly[3] = yExitBot;
        polyBond1(styleBond, color2, outline2);
      }
    } else {
      axPoly[0] = xExitTop; ayPoly[0] = yExitTop;
      axPoly[1] = xMidTop; ayPoly[1] = yMidTop;
      axPoly[2] = xMidBot; ayPoly[2] = yMidBot;
      axPoly[3] = xExitBot; ayPoly[3] = yExitBot;
      polyBond1(styleBond, color1, outline1);

      axPoly[0] = xMidTop; ayPoly[0] = yMidTop;
      axPoly[1] = xSurfaceTop; ayPoly[1] = ySurfaceTop;
      axPoly[2] = xSurfaceBot; ayPoly[2] = ySurfaceBot;
      axPoly[3] = xMidBot; ayPoly[3] = yMidBot;
      polyBond1(styleBond, color2, outline2);
    }
  }

  void polyBond1(byte styleBond, Color color, Color outline) {
    g25d.setColor(color);
    switch(styleBond) {
    case DisplayControl.BOX:
      g25d.drawPolygon(axPoly, ayPoly, azPoly, 4);
      break;
    case DisplayControl.SHADING:
      if (width1 > 4) {
        boolean firstPass = true;
        Color[] shades = getShades(color, Color.black);
        int numPasses = calcNumShadeSteps();
        for (int i = numPasses; --i >= 0; ) {
          Color shade = shades[i * maxShade / numPasses];
          if (firstPass) {
            drawInside(g25d, shade, 2, axPoly, ayPoly, azPoly);
            firstPass = false;
          } else {
            stepPolygon();
            g25d.setColor(shade);
          }
          g25d.fillPolygon(axPoly, ayPoly, azPoly, 4);
        }
        break;
      } // else fall into QUICKDRAW
    case DisplayControl.QUICKDRAW:
      g25d.fillPolygon(axPoly, ayPoly, azPoly, 4);
      drawInside(g25d, outline, 2, axPoly, ayPoly, azPoly);
      break;
    }
  }

  void drawEndCaps() {
    if (!showAtoms || (styleAtom1 == DisplayControl.NONE))
      drawEndCap(xAxis1, yAxis1, zAxis1, width1, color1, outline1);
    if (!showAtoms || (styleAtom2 == DisplayControl.NONE))
      drawEndCap(xAxis2, yAxis2, zAxis2, width2, color2, outline2);
  }

  private ShadedSphereRenderer shadedSphereRenderer;

  void drawEndCap(int x, int y, int z, int diameter,
                  Color color, Color outline) {
    int radiusCap, xUpperLeft, yUpperLeft;
    radiusCap = (diameter+1) / 2;
    xUpperLeft = x - radiusCap;
    yUpperLeft = y - radiusCap;
    switch (styleBond) {
    case DisplayControl.QUICKDRAW:
      g25d.setColor(color);
      g25d.fillCircle(xUpperLeft, yUpperLeft, z, diameter);
      g25d.setColor(outline);
      g25d.drawCircle(xUpperLeft, yUpperLeft, z, diameter);
      break;
    case DisplayControl.SHADING:
      if (shadedSphereRenderer == null)
        shadedSphereRenderer = new ShadedSphereRenderer(control);
      shadedSphereRenderer.render(g25d, xUpperLeft, yUpperLeft, z,
                                  diameter, color, outline);
    }
  }

  int offset1, offset2, doffset;

  void drawInside(Graphics25D g25d, Color color, int width,
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
    drawInside1(g25d, top, ax[iNW], ay[iNW], az[iNW],
                ax[iNE], ay[iNE], az[iNE]);
    if (width > 1)
      drawInside1(g25d, !top, ax[iSW], ay[iSW], az[iSW],
                  ax[iSE], ay[iSE], az[iSE]);
  }

  private final static boolean applyDrawInsideCorrection = true;
  void drawInside1(Graphics25D g25d, boolean top,
                   int x1, int y1, int z1, int x2, int y2, int z2) {
    if (!applyDrawInsideCorrection) {
      drawLineInside(g25d, x1, y1, z1, x2, y2, z2);
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

  void drawLineInside(Graphics25D g25d,
                      int x1, int y1, int z1, int x2, int y2, int z2) {
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
  Color[] getShades(Color color, Color darker) {
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

  int xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2;
  int dxWidth1, dyWidth1, dxWidth2, dyWidth2;
  int dxHalf1, dyHalf1, dxHalf2, dyHalf2;
  int half2, otherHalf2;
  int dxOtherHalf1, dyOtherHalf1, dxOtherHalf2, dyOtherHalf2;

  int space1, space2, step1, step2, dxStep1, dyStep1, dxStep2, dyStep2;
  int offsetAxis1, offsetAxis2;

  void resetAxisCoordinates(boolean lineBond) {
    if (width1 == 0)
      width1 = 1;
    if (width2 == 0)
      width2 = 1;
    space1 = width1 / 8 + 3; space2 = width2 / 8 + 3;
    step1 = width1 + space1;      step2 = width2 + space2;
    dxStep1 = step1 * dy / mag2d; dyStep1 = step1 * -dx / mag2d;
    dxStep2 = step2 * dy / mag2d; dyStep2 = step2 * -dx / mag2d;

    xAxis1 = x1; yAxis1 = y1; zAxis1 = z1;
    xAxis2 = x2; yAxis2 = y2; zAxis2 = z2;
    offsetAxis1 = offsetAxis2 = 0;

    if (bondOrder == 2) {
      offsetAxis1 = -step1 / 2; offsetAxis2 = -step2 / 2;
      xAxis1 -= dxStep1 / 2; yAxis1 -= dyStep1 / 2;
      xAxis2 -= dxStep2 / 2; yAxis2 -= dyStep2 / 2;
    } else if (bondOrder == 3) {
      offsetAxis1 = -step1; offsetAxis2 = -step2;
      xAxis1 -= dxStep1; yAxis1 -= dyStep1;
      xAxis2 -= dxStep2; yAxis2 -= dyStep2;
    }
    if (showAxis) {
      g25d.setColor(control.transparentGrey());
      g25d.drawLine(x1 + dy, y1 - dx, z1, x1 - dy, y1 + dx, z1);
      g25d.drawLine(x2 + dy, y2 - dx, z2, x2 - dy, y2 + dx, z2);
    }
    if (lineBond)
      return;
    dxWidth1 = width1 * dy / mag2d;
    dyWidth1 = width1 * -dx / mag2d;
    dxWidth2 = width2 * dy / mag2d;
    dyWidth2 = width2 * -dx / mag2d;

    dxHalf1 = (dxWidth1 + ((dy >= 0) ? 1 : 0)) / 2;
    dyHalf1 = (dyWidth1 + ((dx <  0) ? 1 : 0)) / 2;
    dxHalf2 = (dxWidth2 + ((dy >= 0) ? 1 : 0)) / 2;
    dyHalf2 = (dyWidth2 + ((dx <  0) ? 1 : 0)) / 2;
    dxOtherHalf1 = dxWidth1 - dxHalf1; dyOtherHalf1 = dyWidth1 - dyHalf1;
    dxOtherHalf2 = dxWidth2 - dxHalf2; dyOtherHalf2 = dyWidth2 - dyHalf2;

    half2 = width2 / 2;
    otherHalf2 = width2 - half2;
  }

  void stepAxisCoordinates() {
    offsetAxis1 += step1; offsetAxis2 += step2;
    xAxis1 += dxStep1; yAxis1 += dyStep1;
    xAxis2 += dxStep2; yAxis2 += dyStep2;
  }

  int dxLine, dyLine, mag2dLineSquared, mag2dLine;
  void calcMag2dLine() {
    dxLine = xAxis2 - xAxis1;
    dyLine = yAxis2 - yAxis1;
    mag2dLineSquared = dxLine*dxLine + dyLine*dyLine;
    mag2dLine = (int)Math.sqrt(mag2dLineSquared);
    if (showAxis) {
      g25d.setColor(Color.cyan);
      g25d.drawLine(xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2);
    }
  }

  int xSurface1, ySurface1, zSurface1, xSurface2, ySurface2, zSurface2;
  int distanceSurface2;

  private static final boolean calcSurface1 = false;
  void calcSurfaceIntersections() {
    if (calcSurface1) {
      int radius1Squared = radius1*radius1;
      int offset1Squared = offsetAxis1*offsetAxis1;
      int radius1Slice = 0;
      if (offset1Squared < radius1Squared) {
        radius1Slice = (int)(Math.sqrt(radius1Squared-offset1Squared));
        radius1Slice = radius1Slice * mag2d / mag3d;
      }
      int dxSlice1 = radius1Slice * dxLine / mag2dLine;
      int dySlice1 = radius1Slice * dyLine / mag2dLine;
      xSurface1 = xAxis1 + dxSlice1;
      ySurface1 = yAxis1 + dySlice1;
    }

    // subtract 1 to ensure that we stay inside;
    int radius2Squared = radius2*radius2 - 1;
    int offset2Squared = offsetAxis2*offsetAxis2;
    distanceSurface2 = 0;
    if (offset2Squared < radius2Squared) {
      distanceSurface2 = (int)(Math.sqrt(radius2Squared-offset2Squared));
      distanceSurface2 = distanceSurface2 * mag2d / mag3d;
    }
    int dxSlice2 = distanceSurface2 * dxLine / mag2dLine;
    int dySlice2 = distanceSurface2 * dyLine / mag2dLine;
    xSurface2 = xAxis2 - dxSlice2;
    ySurface2 = yAxis2 - dySlice2;
    // FIXME mth 2003 06 02
    // need to calc zSurface2 correctly
    zSurface2 = z2;
    

    if (showAxis) {
      dot(xSurface1, ySurface1, zSurface1, control.transparentBlue());
      dot(xSurface2, ySurface2, zSurface2, control.transparentBlue());
    }
  }

  void dot(int x, int y, int z, Color co) {
    g25d.setColor(co);
    g25d.fillRect(x-1, y-1, z, 2, 2);
  }

  double[] intersectionCoords = new double[4];
  int distanceExit;

  void calcExitPoint() {
    int count = intersectCircleLine(x1, y1, diameter1-1,
                                    xAxis1, yAxis1, xAxis2, yAxis2,
                                    intersectionCoords);
    if (count == 0) {
      xExit = xAxis1;
      yExit = yAxis1;
      distanceExit = 0;
    } else {
      // as currently implemented, the "interesting" point is the first
      // point returned. However, if you make changes you need to be careful
      // or you will end up breaking that. If the bonds are drawn from the
      // opposite side of the atom then that is the problem.
      // you need to either 1) be more careful about the signs on the
      // things that you did, or 2) put in a test here to determine which
      // of the two points should be assigned to Exit
      //
      xExit = (int)(intersectionCoords[0]);
      yExit = (int)(intersectionCoords[1]);
      // FIXME mth 2003 06 02 - this zExit is not correct
      // with a good z-buffer implementation,
      // perhaps we don't need to calculate this
      zExit = z1;
      int dx = xExit - x1, dy = yExit - y1;
      distanceExit = (int)Math.sqrt(dx*dx + dy*dy);
      if (showAxis)
        dot(xExit, yExit, zExit, control.transparentBlue());
    }
  }
  
  int intersectCircleLine(int x, int y, int d, int xA, int yA, int xB, int yB,
                          double[] coords) {
    int dxA = xA - x, dxA2 = dxA * dxA;
    int dyA = yA - y, dyA2 = dyA * dyA;
    int dxB = xB - x, dxB2 = dxB * dxB;
    int dyB = yB - y, dyB2 = dyB * dyB;
    int dxAdxB = dxA * dxB;
    int dyAdyB = dyA * dyB;
    // gamma can get pretty large, so turn it into a double before multiplying
    int gamma = dxA2 + dyA2 + dxB2 + dyB2 - 2*dxAdxB - 2*dyAdyB;
    boolean tangent = gamma == 0;
    int delta = 2*dxAdxB + 2*dyAdyB - 2*dxA2 - 2*dyA2;
    double lambda0 = (d*d)/4.0 - (dxA2 + dyA2);
    double lambda1, lambda2;
    if (tangent) {
      if (delta == 0) {
        return 0;
      }
      lambda1 = lambda0 / delta;
      lambda2 = 0;
    } else {
      // delta stays small, so don't worry. gamma gets big, so worry
      lambda0 = lambda0 / gamma + (delta*delta)/(4.0*gamma*gamma);
      if (lambda0 < 0) {
        return 0;
      }
      lambda1 = Math.sqrt(lambda0);
      lambda2 = delta / (2.0 * gamma);
    }
    double lambda = lambda1 - lambda2;
    coords[0] = x + (1 - lambda) * dxA + lambda * dxB;
    coords[1] = y + (1 - lambda) * dyA + lambda * dyB;
    if (tangent)
      return 1;
    lambda = -lambda1 - lambda2;
    coords[2] = x + (1 - lambda) * dxA + lambda * dxB;
    coords[3] = y + (1 - lambda) * dyA + lambda * dyB;
    return 2;
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

  int xL, yL, dxL, dyL;
  int dxLTop, dyLTop, dxLBot, dyLBot;
  int xR, yR, dxR, dyR;
  int dxRTop, dyRTop, dxRBot, dyRBot;
  int step, lenMax;

  int calcNumShadeSteps() {
    int dxSlope = axPoly[1] - axPoly[0];
    int dySlope = ayPoly[1] - ayPoly[0];
    calcLightPoint(dxSlope, dySlope);
    if (dxSlope < 0) dxSlope = -dxSlope;
    if (dySlope < 0) dySlope = -dySlope;

    xL = axPoly[0]; yL = ayPoly[0];
    dxL = axPoly[3] - xL; dyL = ayPoly[3] - yL;
    int lenL = (int)Math.sqrt(dxL*dxL + dyL*dyL);
    int lenLTop = lenL * pctLight / 100;
    int lenLBot = lenL - lenLTop;
    dxLTop = dxL * pctLight / 100;
    dxLBot = dxL - dxLTop;
    dyLTop = dyL * pctLight / 100;
    dyLBot = dyL - dyLTop;

    xR = axPoly[1]; yR = ayPoly[1];
    dxR = axPoly[2] - xR; dyR = ayPoly[2] - yR;
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

    axPoly[0] = xL + dxStepLTop;
    ayPoly[0] = yL + dyStepLTop;
    axPoly[1] = xR + dxStepRTop;
    ayPoly[1] = yR + dyStepRTop;
    axPoly[2] = xR + dxR - dxStepRBot;
    ayPoly[2] = yR + dyR - dyStepRBot;
    axPoly[3] = xL + dxL - dxStepLBot;
    ayPoly[3] = yL + dyL - dyStepLBot;
  }
}

