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
import java.awt.image.PixelGrabber;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

final public class Graphics25D {

  DisplayControl control;
  Platform25D platform;
  Image img;
  Graphics g;
  int width,height,size;
  int[] pbuf;
  short[] zbuf;

  final static int zBackground = 32767;

  public boolean enabled = false;
  boolean capable = false;
  boolean usePbuf;

  int argbDraw, argbFill;

  public Graphics25D(DisplayControl control) {
    this.control = control;
    this.g = g;
    this.capable = control.jvm12orGreater;
    if (capable) {
      platform = new Swing25D();
    } else {
      platform = new Awt25D(control.getAwtComponent());
    }
  }

  public void setEnabled(boolean value) {
    System.out.println("Graphics25D.setEnabled(" + value +")");
    if (enabled != value) {
      enabled = value;
      setSize(width, height);
    }
  }

  public void setSize(int width, int height) {
    System.out.println("Graphics25D.setSize(" + width + "," + height + ")");
    this.width = width;
    this.height = height;
    this.size = width * height;
    if (g != null)
      g.dispose();
    if (size == 0) {
      img = null;
      g = null;
      pbuf = null;
      zbuf = null;
      return;
    }
    img = platform.allocateImage(width, height, !enabled);
    g = platform.getGraphics();
    usePbuf = capable & enabled;
    if (usePbuf) {
      System.out.println("using pbuf");
      pbuf = platform.getPbuf();
      zbuf = new short[size];
    }
  }

  public Image getScreenImage() {
    return img;
  }

  public void setColor(Color color) {
    argbDraw = color.getRGB();
    g.setColor(color);
  }

  int[] imageBuf = new int[256];

  public void drawImage(Image image, int x, int y, int z) {
    if (! usePbuf) {
      g.drawImage(image, x, y, null);
      return;
    }
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    int imageSize = imageWidth * imageHeight;
    if (imageSize > imageBuf.length)
      imageBuf = new int[imageSize];
    PixelGrabber pg = new PixelGrabber(image, 0, 0, imageWidth, imageHeight,
                                       imageBuf, 0, imageWidth);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
      System.out.println("pg.grabPixels Interrupted");
    }
    int offsetSrc = 0;
    if (x >= 0 && y >= 0 && x+imageWidth < width && y+imageHeight < height) {
      do {
        plotPixelsUnclipped(imageBuf, offsetSrc, imageWidth, x, y, z);
        offsetSrc += imageWidth;
        ++y;
      } while (--imageHeight > 0);
    } else {
      do {
        plotPixelsClipped(imageBuf, offsetSrc, imageWidth, x, y, z);
        offsetSrc += imageWidth;
        ++y;
      } while (--imageHeight > 0);
    }
  }

  public void drawImage(Image image, int x, int y, int z,
                        int width, int height) {
    g.drawImage(image, x, y, width, height, null);
  }

  public void drawCircle(Color color, int x, int y, int z, int diameter) {
    if (! usePbuf) {
      g.setColor(color);
      g.drawOval(x, y, diameter-1, diameter-1);
      return;
    }
    argbDraw = color.getRGB();
    int r = (diameter + 1) / 2;
    if (x >= r && x + r < width && y >= r && y + r < height) {
      plotCircleCenteredUnclipped(x + r, y + r, z, r);
    } else {
      plotCircleCenteredClipped(x + r, y + r, z, r);
    }
  }

  public void fillCircle(Color colorFill,
                         int x, int y, int z, int diameter) {
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillOval(x, y, diameter, diameter);
      return;
    }
    argbDraw = colorFill.getRGB();
    int r = (diameter + 1) / 2;
    if (x >= r && x + r < width && y >= r && y + r < width) {
      plotFilledCircleCenteredUnclipped(x + r, y + r, z, r);
    } else {
      plotFilledCircleCenteredClipped(x + r, y + r, z, r);
    }
  }

  public void fillCircle(Color colorOutline, Color colorFill,
                         int x, int y, int z, int diameter) {
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillOval(x, y, diameter, diameter);
      --diameter;
      g.setColor(colorOutline);
      g.drawOval(x, y, diameter, diameter);
      return;
    }
    argbDraw = colorOutline.getRGB();
    int r = (diameter + 1) / 2;
    if (x >= r && x + r < width && y >= r && y + r < width) {
      plotCircleCenteredUnclipped(x + r, y + r, z, r);
      argbDraw = colorFill.getRGB();
      plotFilledCircleCenteredUnclipped(x + r, y + r, z, r);
    } else {
      plotCircleCenteredClipped(x + r, y + r, z, r);
      argbDraw = colorFill.getRGB();
      plotFilledCircleCenteredClipped(x + r, y + r, z, r);
    }
  }

  public void fillSquare2(int x, int y, int z) {
    if (! usePbuf) {
      g.drawLine(x, y, x+1, y);
      ++y;
      g.drawLine(x, y, x+1, y);
      return;
    }
    if (x >= 0 && y >= 0 && x+1 < width && y+1 < height) {
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argbDraw;
      }
      ++offset;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argbDraw;
      }
      offset += width;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argbDraw;
      }
      --offset;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argbDraw;
      }
    } else {
      plotPixelsClipped(2, x, y, z);
      plotPixelsClipped(2, x, y+1, z);
    }
  }

  public void drawRect(int x, int y, int width, int height) {
    g.drawRect(x, y, width, height);
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
  public void clearScreenBuffer(Color colorBackground,int xClip, int yClip,
                                int widthClip, int heightClip) {
    if (! usePbuf) {
      g.setColor(colorBackground);
      g.fillRect(xClip, yClip, widthClip, heightClip);
      return;
    }
    if (heightClip <= 0 || widthClip <= 0)
      return;
    int argbBackground = colorBackground.getRGB();
    int offsetSrc = yClip * width + xClip;
    int offsetT = offsetSrc;
    int count = widthClip;
    do {
      zbuf[offsetT] = zBackground;
      pbuf[offsetT++] = argbBackground;
    } while (--count > 0);
    int offsetDst = offsetSrc;
    while (--heightClip > 0) {
      offsetDst += width;
      System.arraycopy(pbuf, offsetSrc, pbuf, offsetDst, widthClip);
      System.arraycopy(zbuf, offsetSrc, zbuf, offsetDst, widthClip);
    }
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    if (! usePbuf) {
      g.drawLine(x1, y1, x2, y2);
      return;
    }
    if (x1 >= 0 && x1 < width && y1 >= 0 && y1 <= height &&
        x2 >= 0 && x2 < width && y2 >= 0 && y2 <= height) {
      plotLineDeltaUnclipped(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
    } else {
      plotLineDeltaClipped(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
    }
  }

  public void drawPixel(int x, int y, int z) {
    if (! usePbuf) {
      g.drawLine(x, y, x, y);
      return;
    }
    if (x >= 0 && x < width && y >= 0 && y < height) {
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argbDraw;
      }
    }
  }

  public void drawPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    int i = numPoints - 1;
    drawLine(ax[0], ay[0], az[0], ax[i], ay[i], az[i]);
    while (--i >= 0)
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillPolygon(int[] ax, int[] ay, int[] az, int numPoints) {
    if (! usePbuf) {
      g.fillPolygon(ax, ay, numPoints);
      return;
    }
    drawPolygon(ax, ay, az, numPoints);
  }

  public void fillRect(int x, int y, int z, int widthFill, int heightFill) {
    if (! usePbuf) {
      g.drawRect(x, y, width, height);
      return;
    }
    if (x < 0) {
      widthFill += x;
      if (widthFill <= 0)
        return;
      x = 0;
    }
    if (x + widthFill > width) {
      widthFill = width - x;
      if (widthFill == 0)
        return;
    }
    if (y < 0) {
      heightFill += y;
      if (heightFill <= 0)
        return;
      y = 0;
    }
    if (y + heightFill > height)
      heightFill = height - y;
    while (--height >= 0)
      plotPixelsUnclipped(width, x, y++, z);
  }

  /****************************************************************
   * the plotting routines
   ****************************************************************/


  void plotPixelClipped(int x, int y, int z) {
    if (x < 0 || x >= width ||
        y < 0 || y >= height
        //        || z < 0 || z >= 8192
        )
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbDraw;
    }
  }

  void plotPixelUnclipped(int x, int y, int z) {
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbDraw;
    }
  }

  void plotPixelsClipped(int count, int x, int y, int z) {
    if (y < 0 || y >= height || x >= width)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      if (count < 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argbDraw;
      }
      ++offsetPbuf;
    }
  }

  void plotPixelsUnclipped(int count, int x, int y, int z) {
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argbDraw;
      }
      ++offsetPbuf;
    }
  }

  void plotPixelsClipped(int[] pixels, int offset, int count,
                         int x, int y, int z) {
    if (y < 0 || y >= height || x >= width)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      if (count < 0)
        return;
      offset -= x; // and this is adding -x
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int pixel = pixels[offset++];
      int alpha = pixel & 0xFF000000;
      if (alpha >= 0x80000000) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = pixel;
          }
      }
      ++offsetPbuf;
    }
  }

  void plotPixelsUnclipped(int[] pixels, int offset, int count,
                           int x, int y, int z) {
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int pixel = pixels[offset++];
      int alpha = pixel & 0xFF000000;
      if (alpha >= 0x80000000) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = pixel;
        }
      }
      ++offsetPbuf;
    }
  }
  
  void plotLineDeltaClipped(int x1, int y1, int z1, int dx, int dy, int dz) {
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
    plotPixelClipped(xCurrent, yCurrent, z1);
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
      int n = dx;
      do {
        xCurrent += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        plotPixelClipped(xCurrent, yCurrent, zCurrentScaled >> 10);
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    do {
      yCurrent += yIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        xCurrent += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      plotPixelClipped(xCurrent, yCurrent, zCurrentScaled >> 10);
    } while (--n > 0);
  }

  void plotLineDeltaUnclipped(int x1, int y1, int z1, int dx, int dy, int dz) {
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argbDraw;
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1, yIncrement = 1, yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx;
      do {
        xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = argbDraw;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    do {
      yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] = argbDraw;
      }
    } while (--n > 0);
  }

  int xCenter, yCenter, zCenter;

  void plotCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                 int r) {
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8CircleCenteredClipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                   int r) {
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8CircleCenteredUnclipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plot8CircleCenteredClipped(int dx, int dy) {
    plotPixelClipped(xCenter + dx, yCenter + dy, zCenter);
    plotPixelClipped(xCenter + dx, yCenter - dy, zCenter);
    plotPixelClipped(xCenter - dx, yCenter + dy, zCenter);
    plotPixelClipped(xCenter - dx, yCenter - dy, zCenter);

    plotPixelClipped(xCenter + dy, yCenter + dx, zCenter);
    plotPixelClipped(xCenter + dy, yCenter - dx, zCenter);
    plotPixelClipped(xCenter - dy, yCenter + dx, zCenter);
    plotPixelClipped(xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8CircleCenteredUnclipped(int dx, int dy) {
    plotPixelUnclipped(xCenter + dx, yCenter + dy, zCenter);
    plotPixelUnclipped(xCenter + dx, yCenter - dy, zCenter);
    plotPixelUnclipped(xCenter - dx, yCenter + dy, zCenter);
    plotPixelUnclipped(xCenter - dx, yCenter - dy, zCenter);

    plotPixelUnclipped(xCenter + dy, yCenter + dx, zCenter);
    plotPixelUnclipped(xCenter + dy, yCenter - dx, zCenter);
    plotPixelUnclipped(xCenter - dy, yCenter + dx, zCenter);
    plotPixelUnclipped(xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8FilledCircleCenteredClipped(int dx, int dy) {
    plotPixelsClipped(2*dx + 1, xCenter - dx, yCenter + dy, zCenter);
    plotPixelsClipped(2*dx + 1, xCenter - dx, yCenter - dy, zCenter);
    plotPixelsClipped(2*dy + 1, xCenter - dy, yCenter + dx, zCenter);
    plotPixelsClipped(2*dy + 1, xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8FilledCircleCenteredUnclipped(int dx, int dy) {
    plotPixelsUnclipped(2*dx + 1, xCenter - dx, yCenter + dy, zCenter);
    plotPixelsUnclipped(2*dx + 1, xCenter - dx, yCenter - dy, zCenter);
    plotPixelsUnclipped(2*dy + 1, xCenter - dy, yCenter + dx, zCenter);
    plotPixelsUnclipped(2*dy + 1, xCenter - dy, yCenter - dx, zCenter);
  }

  void plotFilledCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                       int r) {
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8FilledCircleCenteredClipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

  void plotFilledCircleCenteredUnclipped(int xCenter, int yCenter, int zCenter,
                                       int r) {
    this.xCenter = xCenter;
    this.yCenter = yCenter;
    this.zCenter = zCenter;
    int x = r;
    int y = 0;
    int xChange = 1 - 2*r;
    int yChange = 1;
    int radiusError = 0;
    while (x >= y) {
      plot8FilledCircleCenteredUnclipped(x, y);
      ++y;
      radiusError += yChange;
      yChange += 2;
      if (2*radiusError + xChange > 0) {
        --x;
        radiusError += xChange;
        xChange += 2;
      }
    }
  }

}
