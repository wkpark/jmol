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
  ShadedSphereRenderer shadedSphereRenderer;
  ShadedBondRenderer shadedBondRenderer;
  Sphere25D sphere25d;
  Triangle25D triangle25d;
  Image img;
  Graphics g;
  int width,height,size;
  int xLast, yLast;
  int[] pbuf;
  short[] zbuf;

  final static int zBackground = 32767;
  final static boolean forceAWT = true;

  public boolean enabled = false;
  boolean usePbuf;

  int argbCurrent;
  Font fontCurrent;

  public Graphics25D(DisplayControl control) {
    this.control = control;
    this.g = g;
    if (control.jvm12orGreater && ! forceAWT ) {
      platform = new Swing25D();
    } else {
      platform = new Awt25D(control.getAwtComponent());
    }
      platform = new Awt25D(control.getAwtComponent());
    this.shadedSphereRenderer = new ShadedSphereRenderer(control, this);
    this.shadedBondRenderer = new ShadedBondRenderer(control, this);
    this.sphere25d = new Sphere25D(control, this);
    this.triangle25d = new Triangle25D(control, this);
  }

  public void setEnabled(boolean value) {
    if (enabled != value) {
      enabled = value;
      setSize(width, height);
    }
  }

  public void setSize(int width, int height) {
    this.width = width;
    xLast = width - 1;
    this.height = height;
    yLast = height - 1;
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
    img = platform.allocateImage(width, height, enabled);
    g = platform.getGraphics();
    usePbuf = enabled;
    if (usePbuf) {
      pbuf = platform.getPbuf();
      zbuf = new short[size];
    }
  }

  public Image getScreenImage() {
    platform.notifyEndOfRendering();
    return img;
  }

  public void setColor(Color color) {
    if (! usePbuf) {
      g.setColor(color);
      return;
    }
    argbCurrent = color.getRGB();
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
    if (x >= 0 && y >= 0 && x+imageWidth <= width && y+imageHeight <= height) {
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
    if (! usePbuf) {
      g.drawImage(image, x, y, width, height, null);
      return;
    }
    System.out.println("drawImage(... width,height) not implemented");
  }

  public void drawCircleCentered(Color color, int x, int y, int z,
                                 int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! usePbuf) {
      g.setColor(color);
      if (diameter <= 2) {
        if (diameter == 1) {
          g.drawLine(x, y, x, y);
        } else {
          g.drawLine(x-1, y, x, y);
          --y;
          g.drawLine(x-1, y, x, y);
        }
      } else {
        g.drawOval(x - r, y - r, diameter-1, diameter-1);
      }
      return;
    }
    argbCurrent = color.getRGB();
    if (x >= r && x + r < width && y >= r && y + r < height) {
      if (diameter <= 2) {
        if (diameter == 1) {
          plotPixelUnclipped(x, y, z);
        } else {
          plotPixelUnclipped(x, y, z);
          plotPixelUnclipped(x-1, y, z);
          --y;
          plotPixelUnclipped(x, y, z);
          plotPixelUnclipped(x-1, y, z);
        }
      } else {
        plotCircleCenteredUnclipped(x, y, z, diameter);
      }
    } else {
      if (diameter <= 2) {
        if (diameter == 1) {
          plotPixelClipped(x, y, z);
        } else {
          plotPixelClipped(x, y, z);
          plotPixelClipped(x-1, y, z);
          --y;
          plotPixelClipped(x, y, z);
          plotPixelClipped(x-1, y, z);
        }
      } else {
        plotCircleCenteredClipped(x + r, y + r, z, r);
      }
    }
  }

  public void fillCircleCentered(Color colorFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillOval(x - r, y - r, diameter, diameter);
      return;
    }
    argbCurrent = colorFill.getRGB();
    if (x >= r && x + r < width && y >= r && y + r < width) {
      plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      plotFilledCircleCenteredClipped(x, y, z, diameter);
    }
  }

  public void fillCircleCentered(Color colorOutline, Color colorFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillOval(x -= r, y -= r, diameter, diameter);
      --diameter;
      g.setColor(colorOutline);
      g.drawOval(x, y, diameter, diameter);
      return;
    }
    argbCurrent = colorOutline.getRGB();
    if (x >= r && x + r < width && y >= r && y + r < height) {
      if (diameter <= 2) {
        if (diameter == 1) {
          plotPixelUnclipped(x, y, z);
        } else {
          plotPixelUnclipped(x, y, z);
          plotPixelUnclipped(x-1, y, z);
          --y;
          plotPixelUnclipped(x, y, z);
          plotPixelUnclipped(x-1, y, z);
        }
      } else {
        plotCircleCenteredUnclipped(x, y, z, diameter);
        argbCurrent = colorFill.getRGB();
        plotFilledCircleCenteredUnclipped(x, y, z, diameter);
      }
    } else {
      if (diameter <= 2) {
        if (diameter == 1) {
          plotPixelClipped(x, y, z);
        } else {
          plotPixelClipped(x, y, z);
          plotPixelClipped(x-1, y, z);
          --y;
          plotPixelClipped(x, y, z);
          plotPixelClipped(x-1, y, z);
        }
      } else {
        plotCircleCenteredClipped(x, y, z, diameter);
        argbCurrent = colorFill.getRGB();
        plotFilledCircleCenteredClipped(x, y, z, diameter);
      }
    }
  }

  public void fillSphereCentered(Color colorOutline, Color colorFill,
                                 int x, int y, int z, int diameter) {
    int r = (diameter + 1) / 2;
    if (! usePbuf) 
      shadedSphereRenderer.render(x - r, y - r, z,
                                  diameter, colorFill, colorOutline);
    else
      sphere25d.paintSphereShape(x, y, z, diameter, colorFill);
  }

  public void drawRect(int x, int y, int width, int height) {
    if (! usePbuf) {
      g.drawRect(x, y, width, height);
      return;
    }
    int xRight = x + width;
    drawLine(x, y, 0, xRight, y, 0);
    int yBottom = y + height;
    drawLine(x, y, 0, x, yBottom, 0);
    drawLine(x, yBottom, 0, xRight, yBottom, 0);
    drawLine(xRight, y, 0, xRight, yBottom, 0);
  }

  public void drawString(String str, int xBaseline, int yBaseline) {
    if (! usePbuf) {
      g.drawString(str, xBaseline, yBaseline);
      return;
    }
    Text25D.plot(xBaseline, yBaseline, 100, argbCurrent,
                 str, fontCurrent, this, control.getAwtComponent());
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
    if (! usePbuf)
      g.setFont(font);
    else
      fontCurrent = font;
  }

  public FontMetrics getFontMetrics(Font font) {
    if (! usePbuf)
      return g.getFontMetrics(font);
    System.out.println("usePbuf ... getFontMetrics is a problem");
    return null;
  }

  public void setClip(Shape shape) {
    if (! usePbuf) {
      g.setClip(shape);
      return;
    }
    System.out.println("setClip(shape) not implemented for pbuf");
  }

  public void setClip(int x, int y, int width, int height) {
    if (! usePbuf) {
      g.setClip(x, y, width, height);
      return;
    }
    System.out.println("setClip(x,y,width,height) not implemented for pbuf");
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
    int cc1 = clipCode(x1, y1);
    int cc2 = clipCode(x2, y2);
    while ((cc1 | cc2) != 0) {
      if ((cc1 & cc2) != 0)
        return;
      int dx = x2 - x1;
      int dy = y2 - y1;
      if (cc1 != 0) { //cohen-sutherland line clipping
        if      ((cc1 & xLT) != 0) { y1 +=      (-x1 * dy) / dx; x1 = 0; }
        else if ((cc1 & xGT) != 0) { y1 += ((xLast-x1)*dy) / dx; x1 = xLast; }
        else if ((cc1 & yLT) != 0) { x1 +=      (-y1 * dx) / dy; y1 = 0; }
        else                       { x1 += ((yLast-y1)*dx) / dy; y1 = yLast; }
        cc1 = clipCode(x1, y1);
      } else {
        if      ((cc2 & xLT) != 0) { y2 +=      (-x2 * dy) / dx; x2 = 0; }
        else if ((cc2 & xGT) != 0) { y2 += ((xLast-x2)*dy) / dx; x2 = xLast; }
        else if ((cc2 & yLT) != 0) { x2 +=      (-y2 * dx) / dy; y2 = 0; }
        else                       { x2 += ((yLast-y2)*dx) / dy; y2 = yLast; }
        cc2 = clipCode(x2, y2);
      }
    }
    plotLineDeltaUnclipped(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
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
        pbuf[offset] = argbCurrent;
      }
    }
  }

  public void drawPolygon4(Color colorOutline, int[] ax, int[] ay, int[] az) {
    setColor(colorOutline);
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillPolygon4(Color colorFill,
                           int[] ax, int[] ay, int[] az) {
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillPolygon(ax, ay, 4);
      return;
    }
    argbCurrent = colorFill.getRGB();
    System.arraycopy(ax, 0, triangle25d.ax, 0, 3);
    System.arraycopy(ay, 0, triangle25d.ay, 0, 3);
    System.arraycopy(az, 0, triangle25d.az, 0, 3);
    triangle25d.fillTriangle();
    triangle25d.ax[1] = ax[3];
    triangle25d.ay[1] = ay[3];
    triangle25d.az[1] = az[3];
    triangle25d.fillTriangle();
  }

  public void fillPolygon4(Color colorOutline, Color colorFill,
                           int[] ax, int[] ay, int[] az) {
    if (! usePbuf) {
      g.setColor(colorFill);
      g.fillPolygon(ax, ay, 4);
      g.setColor(colorOutline);
      g.drawPolygon(ax, ay, 4);
      return;
    }
    drawPolygon4(colorOutline, ax, ay, az);
    fillPolygon4(colorFill, ax, ay, az);
  }

  public void fillCylinder4(Color color, int ax[], int ay[], int az[]) {
    if (! usePbuf) {
      shadedBondRenderer.render(color, ax, ay, az);
      return;
    }
    drawPolygon4(color, ax, ay, az);
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


  final static int xLT = 8;
  final static int xGT = 4;
  final static int yLT = 2;
  final static int yGT = 1;

  private final int clipCode(int x, int y) {
    int code = 0;
    if (x < 0)
      code |= 8;
    else if (x >= width)
      code |= 4;
    if (y < 0)
      code |= 2;
    else if (y >= height)
      code |= 1;
    return code;
  }

  void plotPixelClipped(int x, int y, int z) {
    if (x < 0 || x >= width ||
        y < 0 || y >= height
        //        || z < 0 || z >= 8192
        )
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(int argb, int x, int y, int z) {
    if (x < 0 || x >= width ||
        y < 0 || y >= height
        //        || z < 0 || z >= 8192
        )
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void forcePixel(Color co, int x, int y) {
    if (x < 0 || x >= width ||
        y < 0 || y >= height
        //        || z < 0 || z >= 8192
        )
      return;
    int offset = y * width + x;
    zbuf[offset] = 0;
    pbuf[offset] = co.getRGB();
  }

  void plotPixelUnclipped(int x, int y, int z) {
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
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
        pbuf[offsetPbuf] = argbCurrent;
      }
      ++offsetPbuf;
    }
  }

  void plotPixelsUnclipped(int count, int x, int y, int z) {
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argbCurrent;
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
      if ((alpha & 0x80000000) != 0) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = pixel;
        }
      }
      ++offsetPbuf;
    }
  }
  
  void plotLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    plotLineDeltaUnclipped(x1, y1, z1, x2-x1, y2-y1, z2-z1);
  }

  void plotLineDeltaUnclipped(int x1, int y1, int z1, int dx, int dy, int dz) {
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argbCurrent;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
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
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = argbCurrent;
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
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] = argbCurrent;
      }
    } while (--n > 0);
  }

  int xCenter, yCenter, zCenter;
  int sizeCorrection;

  void plotCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                 int diameter) {
    int r = diameter / 2;
    this.sizeCorrection = 1 - (diameter & 1);
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
                                   int diameter) {
    int r = diameter / 2;
    this. sizeCorrection = 1 - (diameter & 1);
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
    plotPixelClipped(xCenter + dx - sizeCorrection,
                     yCenter + dy - sizeCorrection, zCenter);
    plotPixelClipped(xCenter + dx - sizeCorrection, yCenter - dy, zCenter);
    plotPixelClipped(xCenter - dx, yCenter + dy - sizeCorrection, zCenter);
    plotPixelClipped(xCenter - dx, yCenter - dy, zCenter);

    plotPixelClipped(xCenter + dy - sizeCorrection,
                     yCenter + dx - sizeCorrection, zCenter);
    plotPixelClipped(xCenter + dy - sizeCorrection, yCenter - dx, zCenter);
    plotPixelClipped(xCenter - dy, yCenter + dx - sizeCorrection, zCenter);
    plotPixelClipped(xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8CircleCenteredUnclipped(int dx, int dy) {
    plotPixelUnclipped(xCenter + dx - sizeCorrection,
                       yCenter + dy - sizeCorrection, zCenter);
    plotPixelUnclipped(xCenter + dx - sizeCorrection, yCenter - dy, zCenter);
    plotPixelUnclipped(xCenter - dx, yCenter + dy - sizeCorrection, zCenter);
    plotPixelUnclipped(xCenter - dx, yCenter - dy, zCenter);

    plotPixelUnclipped(xCenter + dy-sizeCorrection,
                       yCenter + dx-sizeCorrection, zCenter);
    plotPixelUnclipped(xCenter + dy - sizeCorrection, yCenter - dx, zCenter);
    plotPixelUnclipped(xCenter - dy, yCenter + dx - sizeCorrection, zCenter);
    plotPixelUnclipped(xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8FilledCircleCenteredClipped(int dx, int dy) {
    plotPixelsClipped(2*dx + 1 - sizeCorrection,
                      xCenter - dx, yCenter + dy - sizeCorrection, zCenter);
    plotPixelsClipped(2*dx + 1 - sizeCorrection,
                      xCenter - dx, yCenter - dy, zCenter);
    plotPixelsClipped(2*dy + 1 - sizeCorrection,
                      xCenter - dy, yCenter + dx - sizeCorrection, zCenter);
    plotPixelsClipped(2*dy + 1 - sizeCorrection,
                      xCenter - dy, yCenter - dx, zCenter);
  }

  void plot8FilledCircleCenteredUnclipped(int dx, int dy) {
    plotPixelsUnclipped(2*dx + 1 - sizeCorrection,
                        xCenter - dx, yCenter + dy - sizeCorrection, zCenter);
    plotPixelsUnclipped(2*dx + 1 - sizeCorrection,
                        xCenter - dx, yCenter - dy, zCenter);
    plotPixelsUnclipped(2*dy + 1 - sizeCorrection,
                        xCenter - dy, yCenter + dx - sizeCorrection, zCenter);
    plotPixelsUnclipped(2*dy + 1 - sizeCorrection,
                        xCenter - dy, yCenter - dx, zCenter);
  }

  void plotFilledCircleCenteredClipped(int xCenter, int yCenter, int zCenter,
                                       int diameter) {
    int r = diameter / 2;
    this. sizeCorrection = 1 - (diameter & 1);
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
                                       int d) {
    int r = d / 2;
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

