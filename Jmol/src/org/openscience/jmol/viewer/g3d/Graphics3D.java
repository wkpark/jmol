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

import org.openscience.jmol.viewer.*;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import javax.vecmath.Point3i;

final public class Graphics3D {

  JmolViewer viewer;
  Platform3D platform;
  boolean forcePlatformAWT = false;
  Line3D line3d;
  Circle3D circle3d;
  Sphere3D sphere3d;
  Triangle3D triangle3d;
  Cylinder3D cylinder3d;
  Hermite3D hermite3d;

  boolean tFullSceneAntialiasing;
  boolean tPaintingInProgress;

  public int width,height;
  int slab;
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

  int argbCurrent;
  boolean jvm12orGreater;
  Font fontCurrent;
  FontMetrics fontmetricsCurrent;

  public Graphics3D(JmolViewer viewer) {
    this.viewer = viewer;
    jvm12orGreater = viewer.jvm12orGreater;
    if (jvm12orGreater && !forcePlatformAWT) {
      platform = allocateSwing3D();
    } else {
      platform = new Awt3D(viewer.getAwtComponent());
    }
    this.line3d = new Line3D(viewer, this);
    this.circle3d = new Circle3D(viewer, this);
    this.sphere3d = new Sphere3D(viewer, this);
    this.triangle3d = new Triangle3D(viewer, this);
    this.cylinder3d = new Cylinder3D(viewer, this);
    this.hermite3d = new Hermite3D(viewer, this);
  }

  private Platform3D allocateSwing3D() {
    // this method is necessary in order to prevent Swing-related
    // classes from getting touched on the MacOS9 platform
    // otherwise the Mac crashes *badly* when the classes are not found
    return new Swing3D();
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

    if (size1 == 0) {
      pbuf = pbuf1 = pbuf4 = null;
      zbuf = zbuf1 = zbuf4 = null;
      return;
    }

    platform.allocateBuffers(width, height);
    pbuf = pbuf1 = platform.pBuffer;
    zbuf = zbuf1 = platform.zBuffer;
    
    //    pbuf4 = new int[size4];
    //    zbuf4 = new short[size4];
  }

  public void validateRectClip(Rectangle rectClip) {
    // on Linux platform with Sun 1.4.2_02 I am getting a clipping rectangle
    // that is wider than the current window during window resize
    if (rectClip.x < 0)
      rectClip.x = 0;
    if (rectClip.y < 0)
      rectClip.y = 0;
    if (rectClip.x + rectClip.width > width)
      rectClip.width = width - rectClip.x;
    if (rectClip.y + rectClip.height > height)
      rectClip.height = height - rectClip.y;
  }

  public void setSlabValue(int slab) {
    this.slab = slab;
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
    return platform.imagePixelBuffer;
  }

  public void setColor(Color color) {
    argbCurrent = color.getRGB();
  }

  public void setArgb(int argb) {
    argbCurrent = argb;
  }

  public void setColix(short colix) {
    argbCurrent = getArgb(colix);
  }

  int[] imageBuf = new int[0];

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
    if (z < slab)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colix);
    if ((x >= r && x + r < width) && (y >= r && y + r < height)) {
      switch (diameter) {
      case 2:
        plotPixelUnclipped(  x, y-1, z);
        plotPixelUnclipped(x-1, y-1, z);
        plotPixelUnclipped(x-1,   y, z);
      case 1:
        plotPixelUnclipped(x, y, z);
      case 0:
        break;
      default:
        circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
      }
    } else {
      switch (diameter) {
      case 2:
        plotPixelClipped(  x, y-1, z);
        plotPixelClipped(x-1, y-1, z);
        plotPixelClipped(x-1,   y, z);
      case 1:
        plotPixelClipped(x, y, z);
      case 0:
        break;
      default:
        circle3d.plotCircleCenteredClipped(x, y, z, diameter);
      }
    }
  }

  public void fillScreenedCircleCentered(short colixFill, int diameter, 
                                         int x, int y, int z) {
    if (diameter == 0 || z < slab)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter, true);
      circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter, true);
      circle3d.plotCircleCenteredClipped(x, y, z, diameter);
    }
  }

  public void fillCircleCentered(short colixFill, int diameter, 
                                 int x, int y, int z) {
    if (diameter == 0 || z < slab)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter, false);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter, false);
    }
  }

  public void fillSphereCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (z < slab)
      return;
    if (diameter <= 1) {
      plotPixelClipped(colix, x, y, z);
    } else {
      sphere3d.render(colix, diameter, x, y, z);
    }
  }

  public void fillSphereCentered(short colix, int diameter, Point3i screen) {
    fillSphereCentered(colix, diameter, screen.x, screen.y, screen.z);
  }

  public void drawRect(short colix, int x, int y, int width, int height) {
    argbCurrent = getArgb(colix);
    int xRight = x + width;
    drawLine(x, y, 0, xRight, y, 0);
    int yBottom = y + height;
    drawLine(x, y, 0, x, yBottom, 0);
    drawLine(x, yBottom, 0, xRight, yBottom, 0);
    drawLine(xRight, y, 0, xRight, yBottom, 0);
  }

  public void drawString(String str, short colix,
                         int xBaseline, int yBaseline, int z) {
    if (z < slab)
      return;
    argbCurrent = getArgb(colix);
    Text3D.plot(xBaseline, yBaseline - fontmetricsCurrent.getAscent(), z,
                argbCurrent, str, fontCurrent, this);
  }

  public void setFont(Font font) {
    if (fontCurrent != font) {
      fontCurrent = font;
      fontmetricsCurrent = platform.getFontMetrics(font);
    }
  }

  public FontMetrics getFontMetrics(Font font) {
    if (font == fontCurrent)
      return fontmetricsCurrent;
    else
      return platform.getFontMetrics(font);
  }

  // 3D specific routines
  public void beginRendering(boolean tFullSceneAntialiasing) {
    if (tFullSceneAntialiasing && zbuf4 != null) {
      width = width4;
      height = height4;
      xLast = xLast4;
      yLast = yLast4;
      pbuf = pbuf4;
      zbuf = zbuf4;
    } else {
      width = width1;
      height = height1;
      xLast = xLast1;
      yLast = yLast1;
      pbuf = pbuf1;
      zbuf = zbuf1;
    }
    this.tFullSceneAntialiasing = tFullSceneAntialiasing;
  }

  public void endRendering() {
    if (tFullSceneAntialiasing)
      downSample();
    platform.notifyEndOfRendering();
  }

  public void clearScreenBuffer(int argbBackground, Rectangle rectClip) {
    // mth 2003 08 07
    // this may be an easy place to take advantage of multiple processors
    // have a thread whose responsibility it is to keep a buffer clean and
    // ready to go.
    platform.clearScreenBuffer(argbBackground, rectClip);
  }

  public void drawDashedLine(short colix, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, run, rise, x1, y1, z1, x2, y2, z2);
  }

  public void drawDottedLine(short colix,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, 2, 1, x1, y1, z1, x2, y2, z2);
  }

  public void drawDashedLine(short colix1, short colix2, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
        
    line3d.drawDashedLine(getArgb(colix1), getArgb(colix2),
                          run, rise, x1, y1, z1, x2, y2, z2);
  }
  

  public void drawLine(Point3i pointA, Point3i pointB) {
    line3d.drawLine(argbCurrent, argbCurrent,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawLine(short colix, Point3i pointA, Point3i pointB) {
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawDottedLine(short colix, Point3i pointA, Point3i pointB) {
    drawDashedLine(colix, 2, 1, pointA, pointB);
  }

  public void drawDashedLine(short colix, int run, int rise,
                             Point3i pointA, Point3i pointB) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, run, rise,
                          pointA.x, pointA.y, pointA.z,
                          pointB.x, pointB.y, pointB.z);
  }

  public void drawDashedLine(int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawDashedLine(argbCurrent, argbCurrent, run, rise,
                          x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(argbCurrent, argbCurrent, x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb, x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix1, short colix2,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(getArgb(colix1), getArgb(colix2),
                    x1, y1, z1, x2, y2, z2);
  }
  
  public void drawPolygon4(int[] ax, int[] ay, int[] az) {
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void drawfillPolygon4(int[] ax, int[] ay, int[] az) {
    drawPolygon4(ax, ay, az);
    System.arraycopy(ax, 0, triangle3d.ax, 0, 3);
    System.arraycopy(ay, 0, triangle3d.ay, 0, 3);
    System.arraycopy(az, 0, triangle3d.az, 0, 3);
    triangle3d.fillTriangle();
    triangle3d.ax[1] = ax[3];
    triangle3d.ay[1] = ay[3];
    triangle3d.az[1] = az[3];
    triangle3d.fillTriangle();
  }

  public void fillQuadrilateral(short colix,
                                Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    fillTriangle(colix, screenA, screenB, screenC);
    fillTriangle(colix, screenA, screenC, screenD);
 }

  public void fillTriangle(short colix, Point3i screenA,
                           Point3i screenB, Point3i screenC) {
    calcSurfaceShade(colix, screenA, screenB, screenC);
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangle();
  }

  public void fillQuadrilateral(int argb,
                                Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    fillTriangle(argb, screenA, screenB, screenC);
    fillTriangle(argb, screenA, screenC, screenD);
  }
  
  public void fillTriangle(int argb, Point3i screenA,
                           Point3i screenB, Point3i screenC) {
    argbCurrent = argb;
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangle();
  }

  int intensity = 0;
  
  void calcSurfaceShade(short colix, Point3i screenA,
                        Point3i screenB, Point3i screenC) {
    argbCurrent = getShades(colix)[intensity];
    intensity = (intensity + 10) & 63;
  }


  public void drawfillTriangle(short colix, int xA, int yA, int zA, int xB,
                               int yB, int zB, int xC, int yC, int zC) {
    int argb = argbCurrent = getArgb(colix);
    line3d.drawLine(argb, argb, xA, yA, zA, xB, yB, zB);
    line3d.drawLine(argb, argb, xA, yA, zA, xC, yC, zC);
    line3d.drawLine(argb, argb, xB, yB, zB, xC, yC, zC);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangle();
  }

  public void fillTriangle(short colix, int xA, int yA, int zA,
                           int xB, int yB, int zB, int xC, int yC, int zC) {
    /*
    System.out.println("fillTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    argbCurrent = getArgb(colix);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangle();
  }

  public void drawTriangle(short colix, int xA, int yA, int zA,
                           int xB, int yB, int zB, int xC, int yC, int zC) {
    /*
    System.out.println("drawTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb, xA, yA, zA, xB, yB, zB);
    line3d.drawLine(argb, argb, xA, yA, zA, xC, yC, zC);
    line3d.drawLine(argb, argb, xB, yB, zB, xC, yC, zC);
  }

  public final static byte ENDCAPS_NONE = 0;
  public final static byte ENDCAPS_OPEN = 1;
  public final static byte ENDCAPS_FLAT = 2;
  public final static byte ENDCAPS_SPHERICAL = 3;

  public void fillCylinder(short colixA, short colixB, byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    cylinder3d.render(colixA, colixB, endcaps, diameter,
                      xA, yA, zA, xB - xA, yB - yA, zB - zA);
  }

  public void fillCylinder(short colix, byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    cylinder3d.render(colix, colix, endcaps, diameter,
                      xA, yA, zA, xB - xA, yB - yA, zB - zA);
  }

  public void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3i screenA, Point3i screenB) {
    cylinder3d.render(colix, colix, endcaps, diameter,
                      screenA.x, screenA.y, screenA.z,
                      screenB.x - screenA.x, screenB.y - screenA.y,
                      screenB.z - screenA.z);
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       int xBase, int yBase, int zBase,
                       int xTip, int yTip, int zTip) {
    cylinder3d.renderCone(colix, endcap, diameter,
                      xBase, yBase, zBase, xTip, yTip, zTip);

  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3i screenBase, Point3i screenTip) {
    cylinder3d.renderCone(colix, endcap, diameter,
                          screenBase.x, screenBase.y, screenBase.z,
                          screenTip.x, screenTip.y, screenTip.z);

  }

  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(true, colix, tension, diameterBeg, diameterMid, diameterEnd,
                     s0, s1, s2, s3);
  }
  
  public void drawHermite(short colix, int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(false, colix, tension, 0, 0, 0, s0, s1, s2, s3);
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
      plotPixelsUnclipped(width, x, y++, z, false);
  }

  public void drawPixel(Point3i point) {
    plotPixelClipped(point);
  }

  public void drawPixel(int x, int y, int z) {
    plotPixelClipped(x, y, z);
  }

  /****************************************************************
   * the plotting routines
   ****************************************************************/


  void plotPixelClipped(int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(Point3i screen) {
    int x = screen.x; if (x < 0 || x >= width) return;
    int y = screen.y; if (y < 0 || y >= height) return;
    int z = screen.z; if (z < slab) return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(int argb, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelClipped(short colix, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = getArgb(colix);
    }
  }

  void forcePixel(Color co, int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height)
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

  void plotPixelsClipped(int count, int x, int y, int z, boolean tScreened) {
    if (y < 0 || y >= height || x >= width || z < slab)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    int offsetMax = offsetPbuf + count;
    int step = 1;
    if (tScreened) {
      step = 2;
      if (((x ^ y) & 1) != 0)
        if (++offsetPbuf == offsetMax)
          return;
    }
    do {
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argbCurrent;
      }
      offsetPbuf += step;
    } while (offsetPbuf < offsetMax);
  }

  void plotPixelsClipped(int argb, int count, int x, int y,
                         int zAtLeft, int zPastRight) {
    //    System.out.print("plotPixelsClipped z values:");
    /*
    System.out.println("plotPixelsClipped count=" + count + "x,y,z=" +
                       x + "," + y + "," + zAtLeft + " -> " + zPastRight);
    */
    if (count <= 0 || y < 0 || y >= height || x >= width ||
        (zAtLeft < slab && zPastRight < slab))
      return;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled =
      ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))/count;
    if (x < 0) {
      x = -x;
      zScaled += zIncrementScaled * x;
      count -= x;
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int z = zScaled >> 10;
      //      System.out.print(" " + z);
      if (z < zbuf[offsetPbuf] && z >= slab) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argb;
      }
      ++offsetPbuf;
      zScaled += zIncrementScaled;
    }
    //    System.out.println("");
  }

  void plotPixelsUnclipped(int count, int x, int y, int z, boolean tScreened) {
    int offsetPbuf = y * width + x;
    if (! tScreened) {
      while (--count >= 0) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = argbCurrent;
        }
        ++offsetPbuf;
      }
    } else {
      int offsetMax = offsetPbuf + count;
      if (((x ^ y) & 1) != 0)
        if (++offsetPbuf == offsetMax)
          return;
      do {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = argbCurrent;
        }
        offsetPbuf += 2;
      } while (offsetPbuf < offsetMax);
    }
  }

  void plotPixelsClipped(int[] pixels, int offset, int count,
                         int x, int y, int z) {
    if (y < 0 || y >= height || x >= width || z < slab)
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
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab)
      line3d.plotLineDeltaClipped(argb1, argb2, x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(argb1, argb2, x, y, z, dx, dy, dz);
  }

  public void plotPoints(short colix, int count, int[] coordinates) {
    int argb = argbCurrent = getArgb(colix);
    for (int i = count * 3; i > 0; ) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab)
        continue;
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argb;
      }
    }
  }

  public void plotPoints(int count,
                         short colix, byte[] intensities, int[] coordinates) {
    int[] shades = getShades(colix);
    for (int i = count * 3, j = count-1; i > 0; --j) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab)
        continue;
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        //        pbuf[offset] = getArgb(colix);
        pbuf[offset] = shades[intensities[j]];
      }
    }
  }

  void averageOffsetArgb(int offset, int argb) {
    pbuf[offset] =((((pbuf[offset] >> 1) & 0x007F7F7F) +
                    ((argb >> 1) & 0xFF7F7F7F)) |
                   (argb & 0xFF010101));
  }

  public final static short NULL = 0;
  public final static short BLACK = 1;
  public final static short ORANGE = 2;
  public final static short PINK = 3;
  public final static short BLUE = 4;
  public final static short WHITE = 5;
  public final static short AQUA = 6;
  public final static short CYAN = 6;
  public final static short RED = 7;
  public final static short GREEN = 8;
  public final static short GRAY = 9;
  public final static short SILVER = 10;
  public final static short LIGHTGRAY = 10;
  public final static short LIME = 11;
  public final static short MAROON = 12;
  public final static short NAVY = 13;
  public final static short OLIVE = 14;
  public final static short PURPLE = 15;
  public final static short TEAL = 16;
  public final static short MAGENTA = 17;
  public final static short FUCHSIA = 17;
  public final static short YELLOW = 18;

  public int getArgb(short colix) {
    return Colix.getArgb(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  public int[] getShades(short colix) {
    return Colix.getShades(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  public short getColix(int argb) {
    return Colix.getColix(argb);
  }

  public short getColix(Color color) {
    return Colix.getColix(color);
  }

  public Color getColor(short colix) {
    return Colix.getColor(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  /****************************************************************
   * changable colixes
   * give me a short ID and a color, and I will give you a colix
   * later, you can reassign the color if you want
   ****************************************************************/

  short[] changableColixMap = new short[16];

  public short getChangableColix(short id, int argb) {
    ++id; // to deal with 0;
    if (id >= changableColixMap.length) {
      short[] t = new short[id + 16];
      System.arraycopy(changableColixMap, 0, t, 0, changableColixMap.length);
      changableColixMap = t;
    }
    if (changableColixMap[id] == 0)
      changableColixMap[id] = getColix(argb);
    return (short)-id;
  }

  public void changeColixArgb(short id, int argb) {
    ++id;
    if (id < changableColixMap.length && changableColixMap[id] != 0)
      changableColixMap[id] = getColix(argb);
  }

  public void flushShadesAndImageCaches() {
    Colix.flushShades();
    Sphere3D.flushImageCache();
  }

  public final static byte shadeMax = Shade3D.shadeMax;
  public final static byte shadeNormal = Shade3D.shadeNormal;
  public final static byte intensitySpecularSurfaceLimit =
    Shade3D.intensitySpecularSurfaceLimit;

  public void setSpecular(boolean specular) {
    Shade3D.setSpecular(specular);
  }

  public boolean getSpecular() {
    return Shade3D.getSpecular();
  }

  public void setSpecularPower(int specularPower) {
    Shade3D.setSpecularPower(specularPower);
  }

  public void setAmbientPercent(int ambientPercent) {
    Shade3D.setAmbientPercent(ambientPercent);
  }

  public void setDiffusePercent(int diffusePercent) {
    Shade3D.setDiffusePercent(diffusePercent);
  }

  public void setSpecularPercent(int specularPercent) {
    Shade3D.setSpecularPercent(specularPercent);
  }

  public void setLightsourceZ(float dist) {
    Shade3D.setLightsourceZ(dist);
  }

  public int calcIntensityNormalized(float x, float y, float z) {
    return Shade3D.calcIntensityNormalized(x, y, z);
  }
}
