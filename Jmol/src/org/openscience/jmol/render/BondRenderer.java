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
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.Kernel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Polygon;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;
import java.util.Enumeration;
import java.util.Hashtable;

import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;

import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;

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
  }

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
  int bondOrder;

  public void render(AtomShape atomShape1, AtomShape atomShape2,
                     byte styleBond, int bondOrder) {
    x1 = atomShape1.x; y1 = atomShape1.y; z1 = atomShape1.z;
    x2 = atomShape2.x; y2 = atomShape2.y; z2 = atomShape2.z;
    dx = x2 - x1; dx2 = dx * dx; sgndx = (dx > 0) ? 1 : (dx < 0) ? -1 : 0;
    dy = y2 - y1; dy2 = dy * dy; sgndy = (dy > 0) ? 1 : (dy < 0) ? -1 : 0;
    dz = z2 - z1; dz2 = dz * dz;
    mag2d2 = dx2 + dy2;
    mag3d2 = mag2d2 + dz2;
    color1 = control.getColorAtom(atomShape1.atom);
    color2 = control.getColorAtom(atomShape2.atom);
    sameColor = color1.equals(color2);
    if (mag2d2 <= 2 || mag2d2 <= 49 && control.getFastRendering())
      return; // also avoids divide by zero when magnitude == 0
    if (control.getShowAtoms() && (mag2d2 <= 16))
      return; // the pixels from the atoms will nearly cover the bond
    if (!control.getShowAtoms() && bondOrder == 1 &&
        (control.getFastRendering() ||
         styleBond == control.WIREFRAME)) {
      if (sameColor) {
        drawLineInside(g, color1, x1, y1, x2, y2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        drawLineInside(g, color1, x1, y1, xMid, yMid);
        drawLineInside(g, color2, xMid, yMid, x2, y2);
      }
      return;
    }
    if (!control.getShowAtoms()) {
      diameter1 = radius1 = diameter2 = radius2 = 0;
    } else {
      diameter1 = atomShape1.diameter;
      radius1 = diameter1 >> 1;
      diameter2 = atomShape2.diameter;
      radius2 = diameter2 >> 1;
    }
    mag2d = (int)Math.sqrt(mag2d2);
    // if the front atom (radius1) has completely obscured the bond, stop
    if (radius1 >= mag2d)
      return;
    halfMag2d = mag2d / 2;
    mag3d = (int)Math.sqrt(mag3d2);
    int radius1Bond = radius1 * mag2d / mag3d;
    int radius2Bond = radius2 * mag2d / mag3d;

    outline1 = control.getColorAtomOutline(color1);
    outline2 = control.getColorAtomOutline(color2);

    width1 = atomShape1.bondWidth;
    width2 = atomShape2.bondWidth;

    if (width1 < 4 && width2 < 4) {
      // to smooth out narrow bonds
        width1 = width2 = (width1 + width2) / 2;
    }
    this.bondOrder = bondOrder;

    boolean lineBond =
      styleBond == control.WIREFRAME ||
      control.getFastRendering();
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
        return;
      stepAxisCoordinates();
    }
  }

  int[] axPoly = new int[4];
  int[] ayPoly = new int[4];
  int xExit, yExit;

  void lineBond() {
    calcSurfaceIntersections();
    calcExitPoint();
    if (sameColor || distanceExit >= mag2dLine / 2 ) {
      if (distanceExit < (mag2dLine - distanceSurface2))
        drawLineInside(g, color2, xExit, yExit, xSurface2, ySurface2);
      return;
    }
    int xMid = (xAxis1 + xAxis2) / 2;
    int yMid = (yAxis1 + yAxis2) / 2;
    drawLineInside(g, color1, xExit, yExit, xMid, yMid);
    drawLineInside(g, color2, xMid, yMid, xSurface2, ySurface2);
  }

  void polyBond(byte styleBond) {
    boolean bothColors = !sameColor;

    calcSurfaceIntersections();
    calcExitPoint();
    int xExitTop = xExit, yExitTop = yExit;
    int xMidTop = (xAxis1 + xAxis2) / 2, yMidTop = (yAxis1 + yAxis2) / 2;
    int xSurfaceTop = xSurface2, ySurfaceTop = ySurface2;
    if (distanceExit >= mag2dLine / 2) {
      bothColors = false;
      if (distanceExit >= (mag2dLine - distanceSurface2 + 1))
        return;
    }

    stepAxisCoordinates();
    calcSurfaceIntersections();
    calcExitPoint();
    int xExitBot = xExit, yExitBot = yExit;
    int xMidBot = (xAxis1 + xAxis2) / 2, yMidBot = (yAxis1 + yAxis2) / 2;
    int xSurfaceBot = xSurface2, ySurfaceBot = ySurface2;
    if (distanceExit >= mag2dLine / 2) {
      bothColors = false;
      if (distanceExit > (mag2dLine - distanceSurface2 + 1))
        return;
    }

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
      if (width1 > 5) {
        int numPasses = calcNumShadeSteps();
        Color[] shades = getShades(color, Color.black);
        for (int i = numPasses; --i >= 0; ) {
          g.setColor(shades[i * maxShade / numPasses]);
          g.fillPolygon(axPoly, ayPoly, 4);
          stepPolygon();
        }
        break;
      }
    case DisplayControl.QUICKDRAW:
      g.fillPolygon(axPoly, ayPoly, 4);
      drawInside(g, outline, 2, axPoly, ayPoly);
      break;
    }
  }

  int offset1, offset2, doffset;
  void lineBond1(int offset1, int offset2) {
    this.offset1 = offset1;
    this.offset2 = offset2;
    doffset = offset2 - offset1;
    calcAxisCoordinates();
    calcSurfaceIntersections();
    calcExitPoint();
    if (sameColor || distanceExit >= mag2dLine / 2 ) {
      if (distanceExit < mag2dLine)
        drawLineInside(g, color2, xExit, yExit, xSurface2, ySurface2);
      return;
    }
    int xMid = (xAxis1 + xAxis2) / 2;
    int yMid = (yAxis1 + yAxis2) / 2;
    drawLineInside(g, color1, xExit, yExit, xMid, yMid);
    drawLineInside(g, color2, xMid, yMid, xSurface2, ySurface2);
  }

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
    boolean top = true;
    if (ax[iNE] < ax[iNW]) {
      iT = iNE; iNE = iNW; iNW = iT;
      iT = iSE; iSE = iSW; iSW = iT;
      top = !top;
    }
    drawInside1(g, top, ax[iNW], ay[iNW], ax[iNE], ay[iNE]);
    if (width > 1)
      drawInside1(g, !top, ax[iSW], ay[iSW], ax[iSE], ay[iSE]);
  }

  void drawInside1(Graphics g, boolean top, int x1, int y1, int x2, int y2) {
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

  void drawLineInside(Graphics g, Color co, int x1, int y1, int x2, int y2) {
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
    g.setColor(co);
    g.drawLine(x1, y1, x2, y2);
  }

  private final Hashtable htShades = new Hashtable();
  final static int maxShade = 16;
  Color[] getShades(Color color, Color darker) {
    Color[] shades = (Color[])htShades.get(color);
    if (shades == null) {
      int darkerR = darker.getRed(),   rangeR = color.getRed() - darkerR;
      int darkerG = darker.getGreen(), rangeG = color.getGreen() - darkerG;
      int darkerB = darker.getBlue(),  rangeB = color.getBlue() - darkerB;
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

  // u stands for unscaled - unscaled by the magnitude
  int dxLine, dyLine, mag2dLine;

  int uxAxis1, uyAxis1;
  int uxAxis2, uyAxis2;

  int xAxis1, yAxis1;
  int xAxis2, yAxis2;

  void calcAxisCoordinates() {
    uxAxis1 = -offset1 * dy;
    xAxis1 = x1 + uxAxis1 / mag2d;
    uyAxis1 = offset1 * dx;
    yAxis1 = y1 + uyAxis1 / mag2d;
    uxAxis2 = offset2 * -dy;
    xAxis2 = x2 + uxAxis2 / mag2d;
    uyAxis2 = -offset2 * -dx;
    yAxis2 = y2 + uyAxis2 / mag2d;

    dxLine = xAxis2 - xAxis1;
    dyLine = yAxis2 - yAxis1;
    mag2dLine = (int)Math.sqrt(dxLine*dxLine + dyLine*dyLine);
  }

  // I currently have an 'accurate method' and a 'regular method'
  // accurate rounds each point to the nearest pixel
  // regular makes regular steps along the axis
  // this propogates errors, but the distances between the lines are fixed
  static final boolean accurateMethod = false;
  boolean showAxis = false;

  int lines, steps, halfSteps, currentStep;
  int dxWidth1, dyWidth1, dxWidth2, dyWidth2;

  void resetAxisCoordinates(boolean lineBond) {
    lines = bondOrder;
    if (! lineBond) {
      lines *= 2;
    } else {
      if (width1 == 0)
        width1 = 1;
      width1 *= 2;
      if (width2 == 0)
        width2 = 1;
      width2 *= 2;
    }
    steps = lines-1;
    halfSteps = steps / 2;

    if (accurateMethod) {
      currentStep = 0;
    } else {
      dxWidth1 = -(width1 *  dy + halfMag2d * sgndy) / mag2d;
      dyWidth1 =  (width1 *  dx + halfMag2d * sgndx) / mag2d;
      dxWidth2 =  (width2 * -dy - halfMag2d * sgndy) / mag2d;
      dyWidth2 = -(width2 * -dx - halfMag2d * sgndx) / mag2d;

      xAxis1 = x1 - dxWidth1*halfSteps;
      yAxis1 = y1 - dyWidth1*halfSteps;
      xAxis2 = x2 - dxWidth2*halfSteps;
      yAxis2 = y2 - dyWidth2*halfSteps;
      if (steps % 2 == 1) {
        xAxis1 -= dxWidth1/2;
        yAxis1 -= dyWidth2/2;
        xAxis2 -= dxWidth2/2;
        yAxis2 -= dyWidth2/2;
      }
    }
    calcLineSlope();
  }

  void stepAxisCoordinates() {
    if (accurateMethod) {
      ++currentStep;
    } else {
      xAxis1 += dxWidth1;
      yAxis1 += dyWidth1;
      xAxis2 += dxWidth2;
      yAxis2 += dyWidth2;
    }
    calcLineSlope();
  }

  void calcLineSlope() {
    if (accurateMethod) {
      offset1 = (currentStep - halfSteps) * width1;
      offset2 = (currentStep - halfSteps) * width2;
      if (steps % 2 == 1) {
        offset1 -= width1 / 2;
        offset2 -= width2 / 2;
      }
      xAxis1 = x1 - (offset1 * dy + sgndy * halfMag2d) / mag2d;
      yAxis1 = y1 + (offset1 * dx + sgndx * halfMag2d) / mag2d;
      xAxis2 = x2 - (offset2 * dy + sgndy * halfMag2d) / mag2d;
      yAxis2 = y2 + (offset2 * dx + sgndx * halfMag2d) / mag2d;
    }
    dxLine = xAxis2 - xAxis1;
    dyLine = yAxis2 - yAxis1;
    mag2dLine = (int)Math.sqrt(dxLine*dxLine + dyLine*dyLine);
    if (!accurateMethod) {
      dxLine = xAxis2 - xAxis1;
      dyLine = yAxis2 - yAxis1;
      mag2dLine = (int)Math.sqrt(dxLine*dxLine + dyLine*dyLine);
      int dxOffset1 = xAxis1 - x1;
      int dyOffset1 = yAxis1 - y1;
      offset1 = (int)Math.sqrt(dxOffset1*dxOffset1 + dyOffset1*dyOffset1);
      int dxOffset2 = xAxis2 - x2;
      int dyOffset2 = yAxis2 - y2;
      offset2 = (int)Math.sqrt(dxOffset2*dxOffset2 + dyOffset2*dyOffset2);
    }
    if (showAxis) {
      g.setColor(Color.lightGray);
      g.drawLine(x1 + dy, y1 - dx, x1 - dy, y1 + dx);
      g.drawLine(x2 + dy, y2 - dx, x2 - dy, y2 + dx);
      g.setColor(Color.cyan);
      g.drawLine(xAxis1, yAxis1, xAxis2, yAxis2);
    }
  }

  int xSurface1, ySurface1, xSurface2, ySurface2;
  int distanceSurface2;

  int radius1Squared;

  public static final boolean calcSurface1 = false;
  void calcSurfaceIntersections() {
    if (calcSurface1) {
      radius1Squared = radius1*radius1;
      int offset1Squared = offset1*offset1;
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
    int offset2Squared = offset2*offset2;
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
      dot(xSurface1, ySurface1, Color.red);
      dot(xSurface2, ySurface2, Color.red);
    }
  }

  Color colorGreenTrans = new Color(0x4000FF00, true);
  Color colorBlueTrans = new Color(0x400000FF, true);
  Color colorRedTrans = new Color(0x40FF0000, true);
  Color colorGreyTrans = new Color(0x40808080, true);

  void dot(int x, int y, Color co) {
    g.setColor(co);
    g.fillRect(x-1, y-1, 2, 2);
  }

  double[] intersectionCoords = new double[4];
  int distanceExit;
  void calcExitPoint() {
    int count = intersectCircleLine(x1, y1, diameter1,
                                    xAxis1, yAxis1, xAxis2, yAxis2,
                                    intersectionCoords);
    if (count == 0) {
      xExit = xAxis1;
      yExit = yAxis1;
    } else {
      // as currently implemented, the "interesting" point is the first
      // point returned. However, if you make changes you need to be careful
      // or you will end up breaking that. If the bonds are drawn from the
      // opposite side of the atom then that is the problem.
      // you need to either 1) be more careful about the signs on the
      // things that you did, or 2) put in a test here to determine which
      // of the two points should be assigned to Exit
      //
      if (lines == 1) {
        xExit = (int)(intersectionCoords[0] + 0.5);
        yExit = (int)(intersectionCoords[1] + 0.5);
      } else {
        // unfortunately, we have to do this calculation to ensure that
        // the end points are exactly on the axis lines
        // otherwise the results look terrible when things get small
        // because 'parallel' lines are not parallel
        double dx = intersectionCoords[0] - xAxis1;
        double dy = intersectionCoords[1] - yAxis1;
        distanceExit = (int)Math.sqrt(dx*dx + dy*dy);
        xExit = xAxis1 + distanceExit * dxLine / mag2dLine;
        yExit = yAxis1 + distanceExit * dyLine / mag2dLine;
      }
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

