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
  Cylinder25D cylinder25d;
  Dots25D dots25d;
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

  public boolean tEnabled = true;

  int argbCurrent;
  Font fontCurrent;
  FontMetrics fontmetricsCurrent;

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
    this.cylinder25d = new Cylinder25D(control, this);
    this.dots25d = new Dots25D(control, this);
  }

  public void setEnabled(boolean tEnable) {
    if (this.tEnabled == tEnable)
      return;
    this.tEnabled = tEnable;
    setSize(width, height);
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
    platform.allocateImage(width, height, tEnabled);
    g = platform.getGraphics();
    pbuf = pbuf1 = pbuf4 = null;
    zbuf = zbuf1 = zbuf4 = null;
    if (tEnabled) {
      pbuf = pbuf1 = platform.getPbuf();
      zbuf = zbuf1 = new short[size1];

      pbuf4 = new int[size4];
      zbuf4 = new short[size4];
    }
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
    if (! tEnabled) {
      g.setColor(color);
      return;
    }
    argbCurrent = color.getRGB();
  }

  public void setArgb(int argb) {
    if (! tEnabled) {
      g.setColor(new Color(argb));
      return;
    }
    argbCurrent = argb;
  }

  public void setColix(short colix) {
    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
      return;
    }
    argbCurrent = Colix.getArgb(colix);
  }

  int[] imageBuf = new int[256];

  public void drawImage(Image image, int x, int y, int z) {
    if (! tEnabled) {
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
    if (! tEnabled) {
      g.drawImage(image, x, y, width, height, null);
      return;
    }
    System.out.println("drawImage(... width,height) not implemented");
  }

  public void drawCircleCentered(short colix, int x, int y, int z,
                                 int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
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

  public void fillCircleCentered(short colixFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! tEnabled) {
      g.setColor(Colix.getColor(colixFill));
      g.fillOval(x - r, y - r, diameter, diameter);
      return;
    }
    argbCurrent = Colix.getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < width) {
      plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      plotFilledCircleCenteredClipped(x, y, z, diameter);
    }
  }

  public void fillCircleCentered(short colixOutline, short colixFill,
                                 int x, int y, int z, int diameter) {
    if (diameter == 0)
      return;
    int r = (diameter + 1) / 2;
    if (! tEnabled) {
      g.setColor(Colix.getColor(colixFill));
      g.fillOval(x -= r, y -= r, diameter, diameter);
      --diameter;
      g.setColor(Colix.getColor(colixOutline));
      g.drawOval(x, y, diameter, diameter);
      return;
    }
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
        plotCircleCenteredUnclipped(x, y, z, diameter);
        argbCurrent = Colix.getArgb(colixFill);
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
        argbCurrent = Colix.getArgb(colixFill);
        plotFilledCircleCenteredClipped(x, y, z, diameter);
      }
    }
  }

  public void drawDotsCentered(short colixDots, 
                               int x, int y, int z, int diameterDots) {
    if (! tEnabled)
      return;
    dots25d.render(colixDots, diameterDots, x, y, z);
  }

  public void fillSphereCentered(short colixOutline, short colixFill,
                                 int x, int y, int z, int diameter) {
    int r = (diameter + 1) / 2;
    if (! tEnabled)
      shadedSphereRenderer.render(x - r, y - r, z,
                                  diameter,
                                  Colix.getColor(colixFill),
                                  Colix.getColor(colixOutline));
    else
      sphere25d.render(colixFill, diameter, x, y, z);
  }

  public void drawRect(short colix, int x, int y, int width, int height) {
    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
      g.drawRect(x, y, width, height);
      return;
    }
    argbCurrent = Colix.getArgb(colix);
    int xRight = x + width;
    drawLine(x, y, 0, xRight, y, 0);
    int yBottom = y + height;
    drawLine(x, y, 0, x, yBottom, 0);
    drawLine(x, yBottom, 0, xRight, yBottom, 0);
    drawLine(xRight, y, 0, xRight, yBottom, 0);
  }

  public void drawString(String str, short colix,
                         int xBaseline, int yBaseline, int z) {
    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
      g.drawString(str, xBaseline, yBaseline);
      return;
    }
    argbCurrent = Colix.getArgb(colix);
    Text25D.plot(xBaseline, yBaseline - fontmetricsCurrent.getAscent(),
                 z, argbCurrent,
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
    if (! tEnabled)
      g.setFont(font);
    else {
      if (fontCurrent != font) {
        fontCurrent = font;
        fontmetricsCurrent = g.getFontMetrics(font);
      }
    }
  }

  public FontMetrics getFontMetrics(Font font) {
    if (! tEnabled)
      return g.getFontMetrics(font);
    if (font == fontCurrent)
      return fontmetricsCurrent;
    else
      return g.getFontMetrics(font);
  }

  public void setClip(Shape shape) {
    if (! tEnabled) {
      g.setClip(shape);
      return;
    }
    System.out.println("setClip(shape) not implemented for pbuf");
  }

  public void setClip(int x, int y, int width, int height) {
    if (! tEnabled) {
      g.setClip(x, y, width, height);
      return;
    }
    System.out.println("setClip(x,y,width,height) not implemented for pbuf");
  }

  // 3D specific routines
  public void beginRendering(boolean tOversample) {
    if (! tEnabled)
      return;
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
    if (! tEnabled)
      return;
    if (tOversample)
      downSample();
    platform.notifyEndOfRendering();
  }

  public void clearScreenBuffer(Color colorBackground,int xClip, int yClip,
                                int widthClip, int heightClip) {
    if (! tEnabled) {
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

  private final static boolean applyLineInsideCorrection = true;

  public void drawLineInside(int x1, int y1, int z1, int x2, int y2, int z2) {
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
    drawLine(x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
      g.drawLine(x1, y1, x2, y2);
      return;
    }
    argbCurrent = Colix.getArgb(colix);
    drawLine(x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix1, short colix2,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    if (colix1 == colix2) {
      drawLine(colix1, x1, y1, z1, x2, y2, z2);
      return;
    }
    if (! tEnabled ||
        x1 < 0 || x1 >= width  || x2 < 0 || x2 >= width ||
        y1 < 0 || y1 >= height || y2 < 0 || y2 >= height) {
      int xMid = (x1 + x2) / 2;
      int yMid = (y1 + y2) / 2;
      int zMid = (z1 + z2) / 2;
      drawLine(colix1, x1, y1, z1, xMid, yMid, zMid);
      drawLine(colix2, xMid, yMid, zMid, x2, y2, z2);
    } else {
      plotLineDeltaUnclipped(Colix.getArgb(colix1), Colix.getArgb(colix2),
                             x1, y1, z1, x2-x1, y2-y1, z2-z1);
    }
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    if (! tEnabled) {
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
      int dz = z2 - z1;


      if (cc1 != 0) { //cohen-sutherland line clipping
        if      ((cc1 & xLT) != 0)
          { y1 +=      (-x1 * dy)/dx; z1 +=      (-x1 * dz)/dx; x1 = 0; }
        else if ((cc1 & xGT) != 0)
          { y1 += ((xLast-x1)*dy)/dx; z1 += ((xLast-x1)*dz)/dx; x1 = xLast; }
        else if ((cc1 & yLT) != 0)
          { x1 +=      (-y1 * dx)/dy; z1 +=      (-y1 * dz)/dy; y1 = 0; }
        else
          { x1 += ((yLast-y1)*dx)/dy; z1 += ((yLast-y1)*dz)/dy; y1 = yLast; }
        cc1 = clipCode(x1, y1);
      } else {
        if      ((cc2 & xLT) != 0)
          { y2 +=      (-x2 * dy)/dx; z2 +=      (-x2 * dz)/dx; x2 = 0; }
        else if ((cc2 & xGT) != 0)
          { y2 += ((xLast-x2)*dy)/dx; z2 += ((xLast-x2)*dz)/dx; x2 = xLast; }
        else if ((cc2 & yLT) != 0)
          { x2 +=      (-y2 * dx)/dy; z2 +=      (-y2 * dz)/dy; y2 = 0; }
        else
          { x2 += ((yLast-y2)*dx)/dy; z2 += ((yLast-y2)*dz)/dy; y2 = yLast; }
        cc2 = clipCode(x2, y2);
      }
    }
    plotLineDeltaUnclipped(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
  }

  /*
  public void drawLineDelta(int x1, int y1, int z1, int dx, int dy, int dz) {
    drawLine(x1, y1, z1, x1 + dx, y1 + dy, z1 + dz);
  }
  */

  public void drawPixel(int x, int y, int z) {
    if (! tEnabled) {
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

  public void drawPolygon4(short colixOutline, int[] ax, int[] ay, int[] az) {
    setColix(colixOutline);
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void drawPolygon4(Color colorOutline, int[] ax, int[] ay, int[] az) {
    setColor(colorOutline);
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillPolygon4(short colixFill,
                           int[] ax, int[] ay, int[] az) {
    if (! tEnabled) {
      g.setColor(Colix.getColor(colixFill));
      g.fillPolygon(ax, ay, 4);
      return;
    }
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
    if (! tEnabled) {
      g.setColor(colorFill);
      g.fillPolygon(ax, ay, 4);
      return;
    }
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
    if (! tEnabled) {
      g.setColor(Colix.getColor(colixFill));
      g.fillPolygon(ax, ay, 4);
      g.setColor(Colix.getColor(colixOutline));
      g.drawPolygon(ax, ay, 4);
      return;
    }
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

    if (! tEnabled) {
      g.setColor(Colix.getColor(colix));
      g.fillPolygon(triangle25d.ax, triangle25d.ay, 3);
      return;
    }
    triangle25d.fillTriangle();
  }

  public void fillCylinder(short colix1, short colix2, int w,
                           int x1, int y1, int z1, int x2, int y2, int z2) {
    cylinder25d.render(colix1, colix2, w,
                       x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
  }

  public void fillShadedPolygon4(short colix, int ax[], int ay[], int az[]) {
    if (! tEnabled) {
      shadedBondRenderer.render(Colix.getColor(colix), ax, ay, az);
      return;
    }
    System.out.println("fillShadedPolygon4 with pbuf ?que?");
  }

  public void fillRect(int x, int y, int z, int widthFill, int heightFill) {
    if (! tEnabled) {
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
  
  void plotLineDelta(int argb1, int argb2,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (argb1 == argb2) {
      argbCurrent = argb1;
      if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
          y < 0 || y >= height || y + dy < 0 || y + dy >= height)
        drawLine(x, y, z, x+dx, y+dy, z+dz);
      else
        plotLineDeltaUnclipped(x, y, z, dx, dy, dz);
      return;
    }
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height)
      plotLineDeltaClipped(argb1, argb2, x, y, z, dx, dy, dz);
    else
      plotLineDeltaUnclipped(argb1, argb2, x, y, z, dx, dy, dz);
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

  void plotLineDeltaUnclippedGradient(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int r1 = (argb1 >> 16) & 0xFF;
    int g1 = (argb1 >> 8) & 0xFF;
    int b1 = argb1 & 0xFF;
    int r2 = (argb2 >> 16) & 0xFF;
    int g2 = (argb2 >> 8) & 0xFF;
    int b2 = argb2 & 0xFF;
    int dr = r2 - r1;
    int dg = g2 - g1;
    int db = b2 - b1;
    int rScaled = r1 << 10;
    int gScaled = g1 << 10;
    int bScaled = b1 << 10;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
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
      int nTransition = n >> 2;
      int nColor2 = (n - nTransition) / 2;
      int nColor1 = n - nColor2;
      if (nTransition <= 0)
        nTransition = 1;
      int drScaled = (dr << 10) / nTransition;
      int dgScaled = (dg << 10) / nTransition;
      int dbScaled = (db << 10) / nTransition;
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
          pbuf[offset] =
            (n > nColor1) ? argb1 :
            (n <= nColor2) ? argb2 :
            0xFF000000 | 
            ((rScaled << 6) & 0x00FF0000) |
            ((gScaled >> 2) & 0x0000FF00) |
            (bScaled >> 10);
        }
        if (n <= nColor1) {
          rScaled += drScaled;
          gScaled += dgScaled;
          bScaled += dbScaled;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    int nTransition = n >> 2;
    int nColor2 = (n - nTransition) / 2;
    int nColor1 = n - nColor2;
    if (nTransition <= 0)
      nTransition = 1;
    int drScaled = (dr << 10) / nTransition;
    int dgScaled = (dg << 10) / nTransition;
    int dbScaled = (db << 10) / nTransition;
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
        pbuf[offset] =
          (n > nColor1) ? argb1 :
          (n <= nColor2) ? argb2 :
          0xFF000000 | 
          ((rScaled << 6) & 0x00FF0000) |
          ((gScaled >> 2) & 0x0000FF00) |
          (bScaled >> 10);
      }
      if (n <= nColor1) {
        rScaled += drScaled;
        gScaled += dgScaled;
        bScaled += dbScaled;
      }
    } while (--n > 0);
  }

  void plotLineDeltaUnclipped(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
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
      int n = dx, nMid = n / 2;
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
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy, nMid = n / 2;
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
        pbuf[offset] = n > nMid ? argb1 : argb2;
      }
    } while (--n > 0);
  }

  void plotLineDeltaClipped(int argb1, int argb2,
                            int x1, int y1, int z1, int dx, int dy, int dz) {
    int width = this.width;
    int height = this.height;
    int offset = y1 * width + x1;
    if (x1 >= 0 && x1 < width &&
        y1 >= 0 && y1 < height) {
      if (z1 < zbuf[offset]) {
        zbuf[offset] = (short)z1;
        pbuf[offset] = argb1;
      }
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1;
    int yIncrement = 1;
    int yOffsetIncrement = width;

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
      int n = dx, nMid = n / 2;
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
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy, nMid = n / 2;
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
      if (xCurrent >= 0 && xCurrent < width &&
          yCurrent >= 0 && yCurrent < height) {
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
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

