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
import org.openscience.jmol.Java12;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

final public class Graphics25D {
  DisplayControl control;
  Platform25D platform;
  Image img;
  Graphics g;
  int width,height;
  int[] pbuf;
  short[] zbuf;

  boolean usePbuf;

  int argbCurrent;

  public Graphics25D(DisplayControl control) {
    this.control = control;
    this.g = g;
    if (control.jvm12orGreater) {
      usePbuf = true;
      platform = new Swing25D();
    } else {
      usePbuf = false;
      platform = new Awt25D(control.getAwtComponent());
    }
  }

  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    if (g != null)
      g.dispose();
    if (width == 0 || height == 0) {
      img = null;
      g = null;
      pbuf = null;
      zbuf = null;
      return;
    }
    img = platform.allocateImage(width, height);
    g = platform.getGraphics();
    pbuf = platform.getPbuf();
    zbuf = new short[width * height];
  }

  public Image getScreenImage() {
    return img;
  }

  public void setColor(Color color) {
    argbCurrent = color.getRGB();
    g.setColor(color);
  }

  public void drawPolygon(int[] ax, int[] ay, int numPoints) {
    g.drawPolygon(ax, ay, numPoints);
  }

  public void fillPolygon(int[] ax, int[] ay, int numPoints) {
    g.fillPolygon(ax, ay, numPoints);
  }

  public void drawImage(Image image, int x, int y, ImageObserver observer) {
    g.drawImage(image, x, y, observer);
  }

  public void drawImage(Image image, int x, int y, int width, int height,
                        ImageObserver observer) {
    g.drawImage(image, x, y, width, height, observer);
  }

  public void drawImage(Image image, int x, int y) {
    g.drawImage(image, x, y, null);
  }

  public void drawOval(int x, int y, int width, int height) {
    g.drawOval(x, y, width, height);
  }

  public void drawOval(int x, int y, int z, int width, int height) {
    g.drawOval(x, y, width, height);
  }

  public void fillOval(int x, int y, int z, int width, int height) {
    g.fillOval(x, y, width, height);
  }

  public void fillOval(int x, int y, int width, int height) {
    g.fillOval(x, y, width, height);
  }

  public void drawRect(int x, int y, int width, int height) {
    g.drawRect(x, y, width, height);
  }

  public void fillRect(int x, int y, int width, int height) {
    g.fillRect(x, y, width, height);
  }

  public void drawString(String str, int xBaseline, int yBaseline) {
    g.drawString(str, xBaseline, yBaseline);
  }

  public void enableAntialiasing(boolean enableAntialiasing) {
    control.java12.enableAntialiasing(g, enableAntialiasing);
  }

  public void dottedStroke() {
    control.java12.dottedStroke(g);
  }

  public void defaultStroke() {
    control.java12.defaultStroke(g);
  }

  public void setFont(Font font) {
    g.setFont(font);
  }

  public FontMetrics getFontMetrics(Font font) {
    return g.getFontMetrics(font);
  }

  public void setClip(Shape shape) {
    g.setClip(shape);
  }

  public void setClip(int x, int y, int width, int height) {
    g.setClip(x, y, width, height);
  }

  // 3D specific routines
  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    if (! usePbuf) {
      g.drawLine(x1, y1, x2, y2);
      return;
    }
    plotLineDelta(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
  }

  public void drawPixel(int x, int y, int z) {
    if (! usePbuf) {
      g.drawLine(x, y, x, y);
      return;
    }
    if (x >= 0 && x < width && y >= 0 && y < height) {
      pbuf[y*width + x] = argbCurrent;
    }
  }

  public void drawPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    g.drawPolygon(ax, ay, numPoints);
  }

  public void fillPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    g.fillPolygon(ax, ay, numPoints);
  }

  void plotPixel(int x, int y, int z) {
    if (x < 0 || x >= width ||
        y < 0 || y >= height
        //        || z < 0 || z >= 8192
        )
      return;
    int offset = y * width + x;
    pbuf[offset] = argbCurrent;
    /*
    if (zbuf[offset] < z) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
    */
  }

  void plotLineDelta(int x1, int y1, int z1, int dx, int dy, int dz) {
    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1, yIncrement = 1;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
    }
    int twoDx = dx + dx, twoDy = dy + dy;
    plotPixel(xCurrent, yCurrent, z1);
    if (dx == 0 && dy == 0)
      return;
    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      do {
        xCurrent += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        plotPixel(xCurrent, yCurrent, zCurrentScaled >> 10);
      } while (--dx > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    do {
      yCurrent += yIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        xCurrent += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      plotPixel(xCurrent, yCurrent, zCurrentScaled >> 10);
    } while (--dy > 0);
  }

}
