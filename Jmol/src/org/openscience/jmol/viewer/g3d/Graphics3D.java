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

import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

final public class Graphics3D {

  JmolViewer viewer;
  Platform3D platform;
  Line3D line25d;
  Circle3D circle25d;
  Sphere3D sphere25d;
  Triangle3D triangle25d;
  Cylinder3D cylinder25d;
  Dots3D dots25d;
  Graphics g;

  boolean tOversample;
  boolean tPaintingInProgress;

  int width,height;
  int xLast, yLast;
  int[] pbuf;
  short[] zbuf;

  int width1, height1, size1;
  int xLast1, yLast1;
  int[] pbuf1;
  short[] zbuf1;

  int width4, height4, size4;
  int xLast4, yLast4;
  int[] pbuf4;
  short[] zbuf4;

  final static int zBackground = 32767;
  final static boolean forceAWT = false;

  int argbCurrent;
  Font fontCurrent;
  FontMetrics fontmetricsCurrent;

  public Graphics3D(JmolViewer viewer) {
    this.viewer = viewer;
    this.g = g;
    if (viewer.jvm12orGreater && ! forceAWT ) {
      platform = new Swing3D();
    } else {
      platform = new Awt3D(viewer.getAwtComponent());
    }
    platform = new Awt3D(viewer.getAwtComponent());
    this.line25d = new Line3D(viewer, this);
    this.circle25d = new Circle3D(viewer, this);
    this.sphere25d = new Sphere3D(viewer, this);
    this.triangle25d = new Triangle3D(viewer, this);
    this.cylinder25d = new Cylinder3D(viewer, this);
    this.dots25d = new Dots3D(viewer, this);
  }

  public void setSize(int width, int height) {
    width1 = this.width = width;
    xLast1 = xLast = width1 - 1;
    height1 = this.height = height;
    yLast1 = yLast = height - 1;
    size1 = width1 * height1;

    width4 = width + width;
    xLast4 = width4 - 1;
    height4 = height + height;
    yLast4 = height4 - 1;
    size4 = width4 * height4;

    if (g != null)
      g.dispose();
    if (size1 == 0) {
      g = null;
      pbuf = pbuf1 = pbuf4 = null;
      zbuf = zbuf1 = zbuf4 = null;
      return;
    }
    platform.allocateImage(width, height, true);
    g = platform.getGraphics();

    pbuf = pbuf1 = platform.getPbuf();
    zbuf = zbuf1 = new short[size1];
    
    pbuf4 = new int[size4];
    zbuf4 = new short[size4];
  }

  private void downSample() {
    int[] pbuf1 = this.pbuf1;
    int[] pbuf4 = this.pbuf4;
    int width4 = this.width4;
    int offset1 = 0;
    int offset4 = 0;
    for (int i = this.height1; --i >= 0; ) {
      for (int j = this.width1; --j >= 0; ) {
        int argb;
        argb  = (pbuf4[offset4         ] >> 2) & 0x3F3F3F3F;
        argb += (pbuf4[offset4 + width4] >> 2) & 0x3F3F3F3F;
        ++offset4;
        argb += (pbuf4[offset4         ] >> 2) & 0x3F3F3F3F;
        argb += (pbuf4[offset4 + width4] >> 2) & 0x3F3F3F3F;
        argb += (argb & 0xC0C0C0C0) >> 6;
        argb |= 0xFF000000;
        pbuf1[offset1] = argb;
        ++offset1;
        ++offset4;
      }
      offset4 += width4;
    }
  }

  public Image getScreenImage() {
    return platform.getImage();
  }

  public void setColor(Color color) {
    argbCurrent = color.getRGB();
  }

  public void setArgb(int argb) {
    argbCurrent = argb;
  }

  public void setColix(short colix) {
    argbCurrent = Colix.getArgb(colix);
  }

  int[] imageBuf = new int[256];

  public void drawImage(Image image, int x, int y, int z) {
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

  public void drawCircleCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = Colix.getArgb(colix);
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
        circle25d.plotCircleCenteredUnclipped(x, y, z, diameter);
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
        circle25d.plotCircleCenteredClipped(x , y , z, diameter);
      }
    }
  }

  public void fillCircleCentered(short colixFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = Colix.getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle25d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      circle25d.plotFilledCircleCenteredClipped(x, y, z, diameter);
    }
  }

  public void fillCircleCentered(short colixOutline, short colixFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = Colix.getArgb(colixOutline);
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
        circle25d.plotCircleCenteredUnclipped(x, y, z, diameter);
        argbCurrent = Colix.getArgb(colixFill);
        circle25d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
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
        circle25d.plotCircleCenteredClipped(x, y, z, diameter);
        argbCurrent = Colix.getArgb(colixFill);
        circle25d.plotFilledCircleCenteredClipped(x, y, z, diameter);
      }
    }
  }

  public void drawDotsCentered(short colixDots, 
                               int x, int y, int z, int diameterDots) {
    dots25d.render(colixDots, diameterDots, x, y, z);
  }

  public void fillSphereCentered(short colix, int diameter,
                                 int x, int y, int z) {
    sphere25d.render(colix, diameter, x, y, z);
  }

  public void drawRect(short colix, int x, int y, int width, int height) {
    argbCurrent = Colix.getArgb(colix);
    int xRight = x + width;
    line25d.drawLine(x, y, 0, xRight, y, 0);
    int yBottom = y + height;
    line25d.drawLine(x, y, 0, x, yBottom, 0);
    line25d.drawLine(x, yBottom, 0, xRight, yBottom, 0);
    line25d.drawLine(xRight, y, 0, xRight, yBottom, 0);
  }

  public void drawString(String str, short colix,
                         int xBaseline, int yBaseline, int z) {
    argbCurrent = Colix.getArgb(colix);
    Text3D.plot(xBaseline, yBaseline - fontmetricsCurrent.getAscent(),
                 z, argbCurrent,
                 str, fontCurrent, this, viewer.getAwtComponent());
  }

  public void enableAntialiasing(boolean enableAntialiasing) {
    //    viewer.java12.enableAntialiasing(g, enableAntialiasing);
  }

  public void dottedStroke() {
    //    viewer.java12.dottedStroke(g);
  }

  public void defaultStroke() {
    //    viewer.java12.defaultStroke(g);
  }


  public void setFont(Font font) {
    if (fontCurrent != font) {
      fontCurrent = font;
      fontmetricsCurrent = g.getFontMetrics(font);
    }
  }

  public FontMetrics getFontMetrics(Font font) {
    if (font == fontCurrent)
      return fontmetricsCurrent;
    else
      return g.getFontMetrics(font);
  }

  // 3D specific routines
  public void beginRendering(boolean tOversample) {
    if (tOversample) {
      width = width4;
      height = height4;
      xLast = xLast4;
      yLast = yLast4;
      pbuf = pbuf4;
      zbuf = zbuf4;
      System.out.println("beginRendering(true)");
    } else {
      width = width1;
      height = height1;
      xLast = xLast1;
      yLast = yLast1;
      pbuf = pbuf1;
      zbuf = zbuf1;
    }
    this.tOversample = tOversample;
  }

  public void endRendering() {
    if (tOversample)
      downSample();
    platform.notifyEndOfRendering();
  }

  public void clearScreenBuffer(Color colorBackground,int xClip, int yClip,
                                int widthClip, int heightClip) {
    // mth 2003 08 07
    // this is the easiest and probably best place to take advantage
    // of multiple processors
    // have a thread whose repsonsibility it is to keep a buffer clean and
    // ready to go.
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

  public void drawLine(short colix,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    argbCurrent = Colix.getArgb(colix);
    line25d.drawLine(x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix1, short colix2,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb1 = Colix.getArgb(colix1);
    int argb2 = Colix.getArgb(colix2);
    if (argb1 == argb2) {
      argbCurrent = argb1;
      line25d.drawLine(x1, y1, z1, x2, y2, z2);
      return;
    }
    if (x1 < 0 || x1 >= width  || x2 < 0 || x2 >= width ||
        y1 < 0 || y1 >= height || y2 < 0 || y2 >= height) {
      int xMid = (x1 + x2) / 2;
      int yMid = (y1 + y2) / 2;
      int zMid = (z1 + z2) / 2;
      argbCurrent = argb1;
      line25d.drawLine(x1, y1, z1, xMid, yMid, zMid);
      argbCurrent = argb2;
      line25d.drawLine(xMid, yMid, zMid, x2, y2, z2);
    } else {
      line25d.plotLineDeltaUnclipped(argb1, argb2,
                                     x1, y1, z1, x2-x1, y2-y1, z2-z1);
    }
  }

  public void drawPolygon4(short colixOutline, int[] ax, int[] ay, int[] az) {
    setColix(colixOutline);
    line25d.drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      line25d.drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void drawPolygon4(Color colorOutline, int[] ax, int[] ay, int[] az) {
    setColor(colorOutline);
    line25d.drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      line25d.drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillPolygon4(short colixFill,
                           int[] ax, int[] ay, int[] az) {
    // draw and then fill
    // make up for some deficiencies in the fill code
    drawPolygon4(colixFill, ax, ay, az);
    argbCurrent = Colix.getArgb(colixFill);
    System.arraycopy(ax, 0, triangle25d.ax, 0, 3);
    System.arraycopy(ay, 0, triangle25d.ay, 0, 3);
    System.arraycopy(az, 0, triangle25d.az, 0, 3);
    triangle25d.fillTriangle();
    triangle25d.ax[1] = ax[3];
    triangle25d.ay[1] = ay[3];
    triangle25d.az[1] = az[3];
    triangle25d.fillTriangle();
  }

  public void fillPolygon4(Color colorFill,
                           int[] ax, int[] ay, int[] az) {
    drawPolygon4(colorFill, ax, ay, az);
    System.arraycopy(ax, 0, triangle25d.ax, 0, 3);
    System.arraycopy(ay, 0, triangle25d.ay, 0, 3);
    System.arraycopy(az, 0, triangle25d.az, 0, 3);
    triangle25d.fillTriangle();
    triangle25d.ax[1] = ax[3];
    triangle25d.ay[1] = ay[3];
    triangle25d.az[1] = az[3];
    triangle25d.fillTriangle();
  }

  public void fillPolygon4(short colixOutline, short colixFill,
                           int[] ax, int[] ay, int[] az) {
    drawPolygon4(colixOutline, ax, ay, az);
    fillPolygon4(colixFill, ax, ay, az);
  }

  public void fillTriangle(short colix, int x0, int y0, int z0,
                           int x1, int y1, int z1, int x2, int y2, int z2) {
    int[] t;
    t = triangle25d.ax;
    t[0] = x0; t[1] = x1; t[2] = x2;
    t = triangle25d.ay;
    t[0] = y0; t[1] = y1; t[2] = y2;
    t = triangle25d.az;
    t[0] = z0; t[1] = z1; t[2] = z2;

    triangle25d.fillTriangle();
  }

  public void fillCylinder(short colix1, short colix2, int w,
                           int x1, int y1, int z1, int x2, int y2, int z2) {
    cylinder25d.render(colix1, colix2, w,
                       x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
  }

  public void fillRect(int x, int y, int z, int widthFill, int heightFill) {
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

  void plotPixelUnclipped(int argb, int x, int y, int z) {
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
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

  void plotPixelsClipped(int argb, int count, int x, int y, int zAtLeft, int zAtRight) {
    if (y < 0 || y >= height || x >= width)
      return;
    // scale the z coordinates;
    int zScaled = zAtLeft << 10;
    int zIncrement = ((zAtRight << 10) - zScaled + (count + 1) / 2)/count;
    // all Z values are positive, although zIncrement can be negative
    // add 1/2 so that z rounds properly when truncated
    zScaled += 1 << 9;
    if (x < 0) {
      x = -x;
      zScaled += (zIncrement * x) / count;
      count += x;      if (count < 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int z = zScaled >> 10;
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argb;
      }
      ++offsetPbuf;
      zScaled += zIncrement;
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
  
  void plotLineDelta(int argb, int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height)
      line25d.plotLineDeltaClipped(argb, argb, x, y, z, dx, dy, dz);
    else
      line25d.plotLineDeltaUnclipped(argb, x, y, z, dx, dy, dz);
  }

  void plotLineDelta(int argb1, int argb2,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height)
      line25d.plotLineDeltaClipped(argb1, argb2, x, y, z, dx, dy, dz);
    else if (argb1 == argb2)
      line25d.plotLineDeltaUnclipped(argb1, x, y, z, dx, dy, dz);
    else 
      line25d.plotLineDeltaUnclipped(argb1, argb2, x, y, z, dx, dy, dz);
  }
}

