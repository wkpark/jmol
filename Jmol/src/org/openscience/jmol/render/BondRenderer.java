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
  Graphics g;
  Rectangle clip;
  DisplayControl control;

  public void setContext(Graphics g, Rectangle clip, DisplayControl control) {
    this.g = g;
    this.clip = clip;
    this.control = control;
  }

  public void render(AtomShape atomShape1, AtomShape atomShape2,
                     int bondOrder) {
    int x1 = atomShape1.x, y1 = atomShape1.y;
    int x2 = atomShape2.x, y2 = atomShape2.y;
    if (!isInsideClip(clip, x1, y1, x2, y2))
      return;
    int dx = x2 - x1, dx2 = dx * dx;
    int dy = y2 - y1, dy2 = dy * dy;
    int magnitude2 = dx2 + dy2;
    Color color1 = getAtomColor(control, atomShape1.atom);
    Color color2 = getAtomColor(control, atomShape2.atom);
    if ((magnitude2 <= 2) || (control.fastRendering && magnitude2 <= 49))
      return; // also avoid divide by zero when magnitude == 0
    if (control.showAtoms && (magnitude2 <= 16))
      return; // the pixels from the atoms will nearly cover the bond
    if (!control.showAtoms &&
        (control.fastRendering || control.bondDrawMode==control.LINE)) {
      // the trivial case of no atoms and only single lines
      // in this case double & triple bonds are not shown 
      if (color1.equals(color2)) {
        drawLineInside(g, color1, x1, y1, x2, y2);
      } else {
        int xMid = (x1 + x2) / 2;
        int yMid = (y1 + y2) / 2;
        drawLineInside(g, color1, x1, y1, xMid, yMid);
        drawLineInside(g, color2, xMid, yMid, x2, y2);
      }
      return;
    }
    int diameter1, radius1, diameter2, radius2;
    if (!control.showAtoms) {
      diameter1 = radius1 = diameter2 = radius2 = 0;
    } else {
      diameter1 = atomShape1.diameter;
      radius1 = diameter1 >> 1;
      diameter2 = atomShape2.diameter;
      radius2 = diameter2 >> 1;
    }

    int z1 = atomShape1.z;
    int z2 = atomShape2.z;
    int dz = z2 - z1;
    int dz2 = dz * dz;
    int magnitude = (int) Math.sqrt(magnitude2);
    double cosine = magnitude / Math.sqrt(magnitude2 + dz2);
    int radius1Bond = (int)(radius1 * cosine);
    int radius2Bond = (int)(radius2 * cosine);
    if (magnitude < radius1 + radius2Bond) {
      // the front atom (radius1) has completely obscured the bond
      return;
    }

    // FIXME -- kludge until I calculate accurate positions for
    // bond intersections
    // single line is correct without arcFactor
    int arcFactor = bondOrder - 1;
    int x1Bond = x1 + ((radius1Bond - arcFactor) * dx) / magnitude;
    int y1Bond = y1 + ((radius1Bond - arcFactor) * dy) / magnitude;
    int x2Bond = x2 - ((radius2Bond - arcFactor) * dx) / magnitude;
    int y2Bond = y2 - ((radius2Bond - arcFactor) * dy) / magnitude;
    int x1Edge = x1 + ((radius1 - arcFactor) * dx) / magnitude;
    int y1Edge = y1 + ((radius1 - arcFactor) * dy) / magnitude;
    // technically, this bond width is not correct in that it is
    // not in perspective. the width at z1 should be wider than the
    // width at z2. but ...
    // just take the average of the two z's and don't worry about it
    int avgZ = (z1 + z2) / 2;
    double halfBondWidth = control.scaleToScreen(avgZ, control.bondWidth/2);

    if (control.fastRendering || control.bondDrawMode==control.LINE) {
      drawLineBond(g, control,
                   x1Bond, y1Bond, color1,
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2,
                   dx, dy, magnitude, bondOrder, halfBondWidth);
      return;
    }
    if (control.bondDrawMode != control.SHADING) {
      Color outline1 = getOutline(control, color1);
      Color outline2 = getOutline(control, color2);
      if (halfBondWidth < .75) {
        drawLineBond(g, control,
                     x1Bond, y1Bond, (control.bondDrawMode == control.WIREFRAME
                                      ? color1 : outline1),
                     x1Edge, y1Edge,
                     x2Bond, y2Bond, (control.bondDrawMode == control.WIREFRAME
                                      ? color2 : outline2),
                     dx, dy, magnitude, bondOrder, halfBondWidth);
        return;
      }
      drawRectBond(g, control,
                   x1Bond, y1Bond, color1, outline1,
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2, outline2,
                   control.bondDrawMode != control.WIREFRAME,
                   dx, dy, magnitude, bondOrder, halfBondWidth, halfBondWidth);
      return;
    }
    // drawing shaded bonds
    if (control.mouseDragged || (int)halfBondWidth < 2) {
      drawRectBond(g, control,
                   x1Bond, y1Bond, color1, getDarker(color1),
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, color2, getDarker(color2),
                   true,
                   dx, dy, magnitude, bondOrder, halfBondWidth, halfBondWidth);
      return;
    }
    Color darker1 = getDarker(color1), outline1 = darker1;
    Color bright1 = color1;
    Color darker2 = getDarker(color2), outline2 = darker2;
    Color bright2 = color2;

    int atom1R = darker1.getRed(),   range1R = bright1.getRed() - atom1R;
    int atom1G = darker1.getGreen(), range1G = bright1.getGreen() - atom1G;
    int atom1B = darker1.getBlue(),  range1B = bright1.getBlue() - atom1B;
    int atom2R = darker2.getRed(),   range2R = bright2.getRed() - atom2R;
    int atom2G = darker2.getGreen(), range2G = bright2.getGreen() - atom2G;
    int atom2B = darker2.getBlue(),  range2B = bright2.getBlue() - atom2B;

    int numPasses = (int)halfBondWidth;
    double widthT = halfBondWidth;
    for (int i = 0; i < numPasses; ++i, widthT -= 1.0) {
      // numPasses must be > 1 because of test above
      double pct = (double) i / (numPasses - 1);
      int r1 = atom1R + (int)(pct * range1R);
      int g1 = atom1G + (int)(pct * range1G);
      int b1 = atom1B + (int)(pct * range1B);
      int r2 = atom2R + (int)(pct * range2R);
      int g2 = atom2G + (int)(pct * range2G);
      int b2 = atom2B + (int)(pct * range2B);

      // Bitwise masking to make color model:
      int model1 = 0xFF << 24 | r1 << 16 | g1 << 8 | b1;
      Color co1 = new Color(model1);
      int model2 = 0xFF << 24 | r2 << 16 | g2 << 8 | b2;
      Color co2 = new Color(model2);

      drawRectBond(g, control,
                   x1Bond, y1Bond, co1, outline1,
                   x1Edge, y1Edge,
                   x2Bond, y2Bond, co2, outline2,
                   true, dx, dy, magnitude, bondOrder,
                   widthT, halfBondWidth);
      // only draw the outline the first time around
      outline1 = outline2 = null;
    }
  }

  private boolean isVisible(int x1, int y1, int xEdge, int yEdge,
                            int x2, int y2) {
    int dxEdge = xEdge - x1, dyEdge = yEdge - y1;
    int dx2 = x2 - x1, dy2 = y2 - y1;
    return (dx2*dx2 + dy2*dy2) > (dxEdge*dxEdge + dyEdge*dyEdge);
  }

  private static final int separationIncrement = 4;

  private void drawLineBond(final Graphics g, DisplayControl control,
                            int x1, int y1, final Color color1,
                            int xEdge, int yEdge,
                            int x2, int y2, final Color color2,
                            final int dx, final int dy, final int magnitude,
                            int bondOrder, final double halfBondWidth) {
    if (! isVisible(x1, y1, xEdge, yEdge, x2, y2))
      return;
    int sepUp = (int) (separationIncrement * halfBondWidth);
    int sepDn = sepUp - (int)((separationIncrement * 2) * halfBondWidth);
    int xOffset = (sepDn * dy) / magnitude;
    int yOffset = (sepUp * dx) / magnitude;
    if (bondOrder == 2) {
      int xHalfOffset = xOffset/2, yHalfOffset = yOffset/2;
      x1 -= xHalfOffset; y1 -= yHalfOffset;
      x2 -= xHalfOffset; y2 -= yHalfOffset;
      xEdge -= xHalfOffset; yEdge -= yHalfOffset;
    } else if (bondOrder == 3) {
      x1 -= xOffset; y1 -= yOffset;
      x2 -= xOffset; y2 -= yOffset;
      xEdge -= xOffset; yEdge -= yOffset;
    } else if (bondOrder > 3) {
      bondOrder = 3; // just for protection against a wild parameter value
    }
    while (true) {
      int xTemp, yTemp;
      int xMid = (x1 + x2) / 2;
      int yMid = (y1 + y2) / 2;
      if (color1.equals(color2) || 
          !isVisible(x1, y1, xEdge, yEdge, xMid, yMid)) {
        xTemp = xEdge; yTemp = yEdge;
      } else {
        drawLineInside(g, color1, xEdge, yEdge, xMid, yMid);
        xTemp = xMid; yTemp = yMid;
      }
      drawLineInside(g, color2, xTemp, yTemp, x2, y2);      
      if (--bondOrder <= 0) // also catch initial parameter values <= 0
        return;
      x1 += xOffset; y1 += yOffset;
      x2 += xOffset; y2 += yOffset;
      xEdge += xOffset; yEdge += yOffset;
    }
  }

  private static final int[] xBondRectPoints = new int[4];
  private static final int[] yBondRectPoints = new int[4];

  private void drawRectBond(final Graphics g, DisplayControl control,
                            final int x1, final int y1,
                            Color color1, Color color1Outline,
                            final int xEdge, final int yEdge,
                            final int x2, final int y2,
                            Color color2, Color color2Outline,
                            final boolean boolFill,
                            final int dx, final int dy, final int magnitude,
                            int bondOrder, final double halfBondWidth,
                            final double separationWidth) {
    if (! isVisible(x1, y1, xEdge, yEdge, x2, y2))
      return;
    // offsets for the width of the bond rectangle
    int width = (int)(2*halfBondWidth);
    int xHalfWidth = (int)(halfBondWidth * dy / magnitude);
    int yHalfWidth = (int)(halfBondWidth * dx / magnitude);
    int xFullWidth = (int)(halfBondWidth * 2 * dy / magnitude);
    int yFullWidth = (int)(halfBondWidth * 2 * dx / magnitude);

    int x1Top = x1 + xHalfWidth, x1Bot = x1Top - xFullWidth;
    int y1Top = y1 - yHalfWidth, y1Bot = y1Top + yFullWidth;
    int x2Top = x2 + xHalfWidth, x2Bot = x2Top - xFullWidth;
    int y2Top = y2 - yHalfWidth, y2Bot = y2Top + yFullWidth;
    int xEdgeTop = xEdge + xHalfWidth, xEdgeBot = xEdgeTop - xFullWidth;
    int yEdgeTop = yEdge - yHalfWidth, yEdgeBot = yEdgeTop + yFullWidth;
    int xMidTop = (x1Top + x2Top) / 2, yMidTop = (y1Top + y2Top + 1) / 2;
    int xMidBot = (x1Bot + x2Bot) / 2, yMidBot = (y1Bot + y2Bot + 1) / 2;

    int sepUp = (int) (separationIncrement * separationWidth);
    int sepDn = sepUp - (int)((separationIncrement * 2) * separationWidth);
    int xOffset = (sepDn * dy) / magnitude;
    int yOffset = (sepUp * dx) / magnitude;
    if (bondOrder == 2) {
      int xHalfOffset = xOffset/2;
      x1Top -=    xHalfOffset; x1Bot -=    xHalfOffset;
      x2Top -=    xHalfOffset; x2Bot -=    xHalfOffset;
      xEdgeTop -= xHalfOffset; xEdgeBot -= xHalfOffset;
      xMidTop -=  xHalfOffset; xMidBot -=  xHalfOffset;
      int yHalfOffset = yOffset/2;
      y1Top -=    yHalfOffset; y1Bot -=    yHalfOffset;
      y2Top -=    yHalfOffset; y2Bot -=    yHalfOffset;
      yEdgeTop -= yHalfOffset; yEdgeBot -= yHalfOffset;
      yMidTop -=  yHalfOffset; yMidBot -=  yHalfOffset;
    } else if (bondOrder == 3) {
      x1Top -=    xOffset; x1Bot -=    xOffset;
      x2Top -=    xOffset; x2Bot -=    xOffset;
      xEdgeTop -= xOffset; xEdgeBot -= xOffset;
      xMidTop -=  xOffset; xMidBot -=  xOffset;

      y1Top -=    yOffset; y1Bot -=    yOffset;
      y2Top -=    yOffset; y2Bot -=    yOffset;
      yEdgeTop -= yOffset; yEdgeBot -= yOffset;
      yMidTop -=  yOffset; yMidBot -=  yOffset;
    } else if (bondOrder > 3) {
      bondOrder = 3; // just in case
    }


    while (true) {
      xBondRectPoints[0] = xEdgeTop; yBondRectPoints[0] = yEdgeTop;
      xBondRectPoints[3] = xEdgeBot; yBondRectPoints[3] = yEdgeBot;
      if (!color1.equals(color2) &&
          isVisible(x1Top, y1Top, xEdgeTop, yEdgeTop, xMidTop, yMidTop)) {
        // two different bond colors
        xBondRectPoints[1] = xMidTop; yBondRectPoints[1] = yMidTop;
        xBondRectPoints[2] = xMidBot; yBondRectPoints[2] = yMidBot;
        g.setColor(color1);
        if (boolFill) {
          g.fillPolygon(xBondRectPoints, yBondRectPoints, 4);
          drawInside(g, color1Outline, width,xBondRectPoints, yBondRectPoints);
        } else {
          g.drawPolygon(xBondRectPoints, yBondRectPoints, 4);
        }
        xBondRectPoints[0] = xMidTop; yBondRectPoints[0] = yMidTop;
        xBondRectPoints[3] = xMidBot; yBondRectPoints[3] = yMidBot;
      }
      xBondRectPoints[1] = x2Top; yBondRectPoints[1] = y2Top;
      xBondRectPoints[2] = x2Bot; yBondRectPoints[2] = y2Bot; 
      g.setColor(color2);
      if (boolFill) {
        g.fillPolygon(xBondRectPoints, yBondRectPoints, 4);
        drawInside(g, color2Outline, width, xBondRectPoints, yBondRectPoints);
      } else {
        g.drawPolygon(xBondRectPoints, yBondRectPoints, 4);
      }

      /*
      // don't draw outlines if we did not fill
      if (boolFill && color1Outline != null) {
        int xOutlineTop, yOutlineTop, xOutlineBot, yOutlineBot;
        if (color1Outline.equals(color2Outline) ||
            !isVisible(x1Top, y1Top, xEdgeTop, yEdgeTop, xMidTop, yMidTop)) {
          xOutlineTop = xEdgeTop; yOutlineTop = yEdgeTop;
          xOutlineBot = xEdgeBot; yOutlineBot = yEdgeBot;
        } else {
          g.setColor(color1Outline);
          drawInside(g, (int)halfBondWidth*2,
                     xEdgeTop, yEdgeTop, xMidTop, yMidTop,
                     xEdgeBot, yEdgeBot, xMidBot, yMidBot);
          xOutlineTop = xMidTop; yOutlineTop = yMidTop;
          xOutlineBot = xMidBot; yOutlineBot = yMidBot;
        }
        g.setColor(color2Outline);
        drawInside(g,  (int)halfBondWidth*2,
                   xOutlineTop, yOutlineTop, x2Top, y2Top,
                   xOutlineBot, yOutlineBot, x2Bot, y2Bot);
      }
      */
      if (--bondOrder <= 0) // also catch a crazy parameter value
        return;
      x1Top +=    xOffset; x1Bot +=    xOffset;
      x2Top +=    xOffset; x2Bot +=    xOffset;
      xEdgeTop += xOffset; xEdgeBot += xOffset;
      xMidTop +=  xOffset; xMidBot +=  xOffset;

      y1Top +=    yOffset; y1Bot +=    yOffset;
      y2Top +=    yOffset; y2Bot +=    yOffset;
      yEdgeTop += yOffset; yEdgeBot += yOffset;
      yMidTop +=  yOffset; yMidBot +=  yOffset;
    }
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

  static Hashtable htDarker = new Hashtable();
  private Color getDarker(Color color) {
    Color darker = (Color) htDarker.get(color);
    if (darker == null) {
      darker = color.darker();
      htDarker.put(color, darker);
    }
    return darker;
  }

  private Color getAtomColor(DisplayControl control, Atom atom) {
    org.openscience.cdk.renderer.color.AtomColorer colorProfile;
    if (control.getAtomColorProfile() == DisplayControl.ATOMCHARGE)
        colorProfile =
          new org.openscience.cdk.renderer.color.PartialAtomicChargeColors();
    else
        colorProfile = AtomColors.getInstance();
    return colorProfile.getAtomColor((org.openscience.cdk.Atom)atom);
  }

  private Color getOutline(DisplayControl control, Color color) {
    return control.showDarkerOutline ? getDarker(color) : control.outlineColor;
  }

  private static Rectangle rectTemp = new Rectangle();

  private boolean isInsideClip(Rectangle clip,
                               int x1, int y1, int x2, int y2) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (x1 < x2) {
      xMin = x1;
      width = x2 - x1;
    } else if (x2 < x1) {
      xMin = x2;
      width = x1 - x2;
    } else {
      xMin = x1;
      width = 1;
    }
    if (y1 < y2) {
      yMin = y1;
      height = y2 - y1;
    } else if (y2 < y1) {
      yMin = y2;
      height = y1 - y2;
    } else {
      yMin = y1;
      height = 1;
    }
    // there are some problems with this quick&dirty implementation
    // so I am going to throw in some slop
    xMin -= 5;
    yMin -= 5;
    width += 10;
    height += 10;
    rectTemp.setRect(xMin, yMin, width, height);
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("bond " + x + "," + y + "->" + x2 + "," + y2 +
                       " & " + clip.x + "," + clip.y +
                       " W " + clip.width + " H " + clip.height +
                       "->" + visible);
    */
    return visible;
  }

}

