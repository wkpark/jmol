/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.Hashtable;

public class BondRenderer {

  DisplayControl control;
  public BondRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics g;
  Rectangle clip;

  public void setGraphicsContext(Graphics g, Rectangle clip) {
    this.g = g;
    this.clip = clip;

    fastRendering = control.getFastRendering();
    showAtoms = control.getShowAtoms();
  }

  boolean fastRendering;
  boolean showAtoms;

  int x1, y1, z1;
  int x2, y2, z2;
  int dx, dy, dz;
  int dx2, dy2, dz2;
  int sgndx, sgndy;
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
  
  public void render(AtomShape atomShape1, int index1,
                     AtomShape atomShape2, int index2,
                     int bondOrder) {
    x1 = atomShape1.x; y1 = atomShape1.y; z1 = atomShape1.z;
    x2 = atomShape2.x; y2 = atomShape2.y; z2 = atomShape2.z;
    dx = x2 - x1; dx2 = dx * dx; sgndx = (dx > 0) ? 1 : (dx < 0) ? -1 : 0;
    dy = y2 - y1; dy2 = dy * dy; sgndy = (dy > 0) ? 1 : (dy < 0) ? -1 : 0;
    dz = z2 - z1; dz2 = dz * dz;
    mag2d2 = dx2 + dy2;
    mag3d2 = mag2d2 + dz2;
    styleAtom1 = atomShape1.styleAtom;
    styleAtom2 = atomShape2.styleAtom;
    color1 = atomShape1.colorBonds[index1];
    if (color1 == null)
      color1 = atomShape1.colorAtom;
    color2 = atomShape2.colorBonds[index2];
    if (color2 == null)
      color2 = atomShape2.colorAtom;
    sameColor = color1.equals(color2);
    if (mag2d2 <= 2 || mag2d2 <= 49 && fastRendering)
      return; // also avoids divide by zero when magnitude == 0
    if (showAtoms && (mag2d2 <= 16))
      return; // the pixels from the atoms will nearly cover the bond
    styleBond = atomShape1.styleBonds[index1];
    if (!showAtoms && bondOrder == 1 &&
        (fastRendering || styleBond == control.WIREFRAME)) {
      g.setColor(color1);
      if (sameColor) {
        drawLineInside(g, x1, y1, x2, y2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        drawLineInside(g, x1, y1, xMid, yMid);
        g.setColor(color2);
        drawLineInside(g, xMid, yMid, x2, y2);
      }
      return;
    }
    if (!showAtoms) {
      diameter1 = diameter2 = 0;
    } else {
      diameter1 =
        (styleAtom1 == DisplayControl.NONE) ? 0 : atomShape1.diameter;
      diameter2 =
        (styleAtom2 == DisplayControl.NONE) ? 0 : atomShape2.diameter;
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

    width1 = atomShape1.bondWidths[index1];
    width2 = atomShape2.bondWidths[index2];

    if (width1 < 4 && width2 < 4) {
      // to smooth out narrow bonds
        width1 = width2 = (width1 + width2) / 2;
    }
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
      g.setColor(control.transparentGreen());
      g.drawLine(x1 + 5, y1, x1 - 5, y1);
      g.drawLine(x1, y1 + 5, x1, y1 - 5);
      g.drawOval(x1-5, y1-5, 10, 10);
      g.drawLine(x2 + 5, y2, x2 - 5, y2);
      g.drawLine(x2, y2 + 5, x2, y2 - 5);
      g.drawOval(x2-5, y2-5, 10, 10);
    }
  }

  private static final boolean showAxis = false;

  void initializeDebugColors() {
  }

  int[] axPoly = new int[4];
  int[] ayPoly = new int[4];
  int xExit, yExit;

  void lineBond() {
    calcMag2dLine();
    calcSurfaceIntersections();
    calcExitPoint();
    if (sameColor || distanceExit >= mag2dLine / 2 ) {
      if (distanceExit + distanceSurface2 >= mag2dLine)
        return;
      g.setColor(color2);
      drawLineInside(g, xExit, yExit, xSurface2, ySurface2);
      return;
    }
    int xMid = (xAxis1 + xAxis2) / 2;
    int yMid = (yAxis1 + yAxis2) / 2;
    g.setColor(color1);
    drawLineInside(g, xExit, yExit, xMid, yMid);
    g.setColor(color2);
    drawLineInside(g, xMid, yMid, xSurface2, ySurface2);
  }

  void polyBond(byte styleBond) {
    boolean bothColors = !sameColor;

    xAxis1 -= dxHalf1; yAxis1 -= dyHalf1;
    xAxis2 -= dxHalf2; yAxis2 -= dyHalf2;
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
    calcMag2dLine();
    calcSurfaceIntersections();
    calcExitPoint();
    int xExitBot = xExit, yExitBot = yExit;
    int xMidBot = (xAxis1 + xAxis2) / 2, yMidBot = (yAxis1 + yAxis2) / 2;
    int xSurfaceBot = xSurface2, ySurfaceBot = ySurface2;

    // now, restore the axis points to their proper position
    xAxis1 -= dxOtherHalf1; yAxis1 -= dyOtherHalf1;
    xAxis2 -= dxOtherHalf2; yAxis2 -= dyOtherHalf2;

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
    g.setColor(color);
    switch(styleBond) {
    case DisplayControl.BOX:
      g.drawPolygon(axPoly, ayPoly, 4);
      break;
    case DisplayControl.SHADING:
      if (width1 > 3) {
        boolean firstPass = true;
        int numPasses = calcNumShadeSteps();
        Color[] shades = getShades(color, Color.black);
        for (int i = numPasses; --i >= 0; ) {
          Color shade = shades[i * maxShade / numPasses];
          if (firstPass) {
            drawInside(g, shade, 2, axPoly, ayPoly);
            firstPass = false;
          }
          g.setColor(shade);
          g.fillPolygon(axPoly, ayPoly, 4);
          stepPolygon();
        }
        break;
      } // else fall into QUICKDRAW
    case DisplayControl.QUICKDRAW:
      g.fillPolygon(axPoly, ayPoly, 4);
      drawInside(g, outline, 2, axPoly, ayPoly);
      break;
    }
  }

  void drawEndCaps() {
    if (!showAtoms || (styleAtom1 == DisplayControl.NONE))
      drawEndCap(xAxis1, yAxis1, width1, color1, outline1);
    if (!showAtoms || (styleAtom2 == DisplayControl.NONE))
      drawEndCap(xAxis2, yAxis2, width2, color2, outline2);
  }

  private ShadedSphereRenderer shadedSphereRenderer;

  void drawEndCap(int x, int y, int diameter, Color color, Color outline) {
    int radiusCap, xUpperLeft, yUpperLeft;
    radiusCap = (diameter+1) / 2;
    xUpperLeft = x - radiusCap;
    yUpperLeft = y - radiusCap;
    switch (styleBond) {
    case DisplayControl.QUICKDRAW:
      --diameter; // don't forget that a drawn circle is one larger
      g.setColor(color);
      g.fillOval(xUpperLeft, yUpperLeft, diameter, diameter);
      g.setColor(outline);
      g.drawOval(xUpperLeft, yUpperLeft, diameter, diameter);
      break;
    case DisplayControl.SHADING:
      if (shadedSphereRenderer == null)
        shadedSphereRenderer = new ShadedSphereRenderer(control);
      shadedSphereRenderer.render(g, xUpperLeft, yUpperLeft, diameter,
                                  color, outline);
    }
  }

  int offset1, offset2, doffset;

  void drawInside(Graphics g, Color color, int width, int[] ax, int[] ay) {
    // mth dec 2002
    // I am not happy with this implementation, but for now it is the most
    // effective kludge I could come up with to deal with the fact that
    // the brush is offset to the lower right of the point when drawing
    if (color == null)
      return;
    g.setColor(color);
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
    drawInside1(g, top, ax[iNW], ay[iNW], ax[iNE], ay[iNE]);
    if (width > 1)
      drawInside1(g, !top, ax[iSW], ay[iSW], ax[iSE], ay[iSE]);
  }

  private final static boolean applyDrawInsideCorrection = true;
  void drawInside1(Graphics g, boolean top, int x1, int y1, int x2, int y2) {
    if (!applyDrawInsideCorrection) {
      drawLineInside(g, x1, y1, x2, y2);
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
          g.drawLine(x1, y1, x2, y2);
          --x1; --x2;
        } else {
          g.drawLine(x1+1, y1, x2, y2-1);
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
    g.drawLine(x1, y1, x2, y2);
  }

  private final static boolean applyLineInsideCorrection = true;

  void drawLineInside(Graphics g, int x1, int y1, int x2, int y2) {
    if (applyLineInsideCorrection) {
      if (x2 < x1) {
        int xT = x1; x1 = x2; x2 = xT;
        int yT = y1; y1 = y2; y2 = yT;
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
    g.drawLine(x1, y1, x2, y2);
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

  int xAxis1, yAxis1, xAxis2, yAxis2;
  int dxWidth1, dyWidth1, dxWidth2, dyWidth2;
  int dxHalf1, dyHalf1, dxHalf2, dyHalf2;
  int dxOtherHalf1, dyOtherHalf1, dxOtherHalf2, dyOtherHalf2;

  int space1, space2, step1, step2, dxStep1, dyStep1, dxStep2, dyStep2;
  int offsetAxis1, offsetAxis2;

  void resetAxisCoordinates(boolean lineBond) {
    if (width1 == 0)
      width1 = 1;
    if (width2 == 0)
      width2 = 1;
    space1 = width1 / 4 + 2; space2 = width2 / 4 + 2;
    step1 = width1 + space1;      step2 = width2 + space2;
    dxStep1 = step1 * dy / mag2d; dyStep1 = step1 * -dx / mag2d;
    dxStep2 = step2 * dy / mag2d; dyStep2 = step2 * -dx / mag2d;

    xAxis1 = x1; yAxis1 = y1;     xAxis2 = x2; yAxis2 = y2;

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
      g.setColor(control.transparentGrey());
      g.drawLine(x1 + dy, y1 - dx, x1 - dy, y1 + dx);
      g.drawLine(x2 + dy, y2 - dx, x2 - dy, y2 + dx);
    }
    if (lineBond)
      return;
    /*
    dxWidth1 = -(width1 *  dy + halfMag2d * sgndy) / mag2d;
    dyWidth1 =  (width1 *  dx + halfMag2d * sgndx) / mag2d;
    dxWidth2 =  (width2 * -dy - halfMag2d * sgndy) / mag2d;
    dyWidth2 = -(width2 * -dx - halfMag2d * sgndx) / mag2d;
    */
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
      g.setColor(Color.cyan);
      g.drawLine(xAxis1, yAxis1, xAxis2, yAxis2);
    }
  }

  int xSurface1, ySurface1, xSurface2, ySurface2;
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

    // ensure that we stay inside;
    int radius2Squared = radius2*radius2 - 1;
    int offset2Squared = offsetAxis2*offsetAxis2;
    distanceSurface2 = 0;
    if (offset2Squared < radius2Squared) {
      distanceSurface2 = (int)(Math.sqrt(radius2Squared-offset2Squared));
      distanceSurface2 = distanceSurface2 * mag2d / mag3d;
    }
    int dxSlice2 = distanceSurface2 * -dxLine / mag2dLine;
    int dySlice2 = distanceSurface2 * -dyLine / mag2dLine;
    xSurface2 = xAxis2 + dxSlice2;
    ySurface2 = yAxis2 + dySlice2;

    if (showAxis) {
      dot(xSurface1, ySurface1, control.transparentBlue());
      dot(xSurface2, ySurface2, control.transparentBlue());
    }
  }

  void dot(int x, int y, Color co) {
    g.setColor(co);
    g.fillRect(x-1, y-1, 2, 2);
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
      int dx = xExit - x1, dy = yExit - y1;
      distanceExit = (int)Math.sqrt(dx*dx + dy*dy);
      if (showAxis)
        dot(xExit, yExit, control.transparentBlue());
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

  
  int xL, yL, dxL, dyL, lenL;
  int xR, yR, dxR, dyR, lenR;
  int step, lenMax;

  int calcNumShadeSteps() {
    xL = axPoly[0]; yL = ayPoly[0];
    dxL = axPoly[3] - xL; dyL = ayPoly[3] - yL;
    lenL = (int)Math.sqrt(dxL*dxL + dyL*dyL);
    xR = axPoly[1]; yR = ayPoly[1];
    dxR = axPoly[2] - xR; dyR = ayPoly[2] - yR;
    lenR = (int)Math.sqrt(dxR*dxR + dyR+dyR);
    lenMax = lenL;
    if (lenR > lenMax)
      lenMax = lenR;
    step = 0;
    return lenMax / 2;
  }

  void stepPolygon() {
    ++step;
    int dxStepL = dxL * step / (lenMax - 1);
    int dyStepL = dyL * step / (lenMax - 1);
    int dxStepR = dxR * step / (lenMax - 1);
    int dyStepR = dyR * step / (lenMax - 1);

    axPoly[0] = xL + dxStepL;
    ayPoly[0] = yL + dyStepL;
    axPoly[1] = xR + dxStepR;
    ayPoly[1] = yR + dyStepR;
    axPoly[2] = xR + dxR - dxStepR;
    ayPoly[2] = yR + dyR - dyStepR;
    axPoly[3] = xL + dxL - dxStepL;
    ayPoly[3] = yL + dyL - dyStepL;
  }
}

