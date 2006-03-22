/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.g3d;

import java.awt.Component;
import java.awt.Image;
import java.awt.FontMetrics;
import java.util.Hashtable;
import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

/**
 * Provides high-level graphics primitives for 3D visualization.
 *<p>
 * A pure software implementation of a 3D graphics engine.
 * No hardware required.
 * Depending upon what you are rendering ... some people say it
 * is <i>pretty fast</i>.
 *
 * @author Miguel, miguel@jmol.org
 */

final public class Graphics3D {

  Platform3D platform;
  Line3D line3d;
  Circle3D circle3d;
  Sphere3D sphere3d;
  Triangle3D triangle3d;
  Cylinder3D cylinder3d;
  Hermite3D hermite3d;
  Geodesic3D geodesic3d;
  Normix3D normix3d;

  public final static int HIGHEST_GEODESIC_LEVEL = 3;

  boolean isFullSceneAntialiasingEnabled;
  boolean antialiasThisFrame;

  boolean inGreyscaleMode;
  byte[] anaglyphChannelBytes;

  boolean tPaintingInProgress;

  int windowWidth, windowHeight;
  int width, height;
  int slab, depth;
  int xLast, yLast;
  int[] pbuf;
  short[] zbuf;

  short colixBackground = BLACK;
  short colixBackgroundContrast = WHITE;

  int clipX;
  int clipY;
  int clipWidth;
  int clipHeight;

  short colixCurrent;
  int[] shadesCurrent;
  int argbCurrent;
  boolean isTranslucent;
  int argbNoisyUp, argbNoisyDn;

  Font3D font3dCurrent;

  final static int ZBUFFER_BACKGROUND = Platform3D.ZBUFFER_BACKGROUND;

  /**
   * Allocates a g3d object
   *
   * @param awtComponent the java.awt.Component where the image will be drawn
   */
  public Graphics3D(Component awtComponent) {
    platform = Platform3D.createInstance(awtComponent);
    //    Font3D.initialize(platform);
    this.line3d = new Line3D(this);
    this.circle3d = new Circle3D(this);
    this.sphere3d = new Sphere3D(this);
    this.triangle3d = new Triangle3D(this);
    this.cylinder3d = new Cylinder3D(this);
    this.hermite3d = new Hermite3D(this);
    this.geodesic3d = new Geodesic3D(this);
    this.normix3d = new Normix3D(this);
    //    setFontOfSize(13);
  }
  
  /**
   * Sets the window size. This will be smaller than the
   * rendering size if FullSceneAntialiasing is enabled
   *
   * @param windowWidth Window width
   * @param windowHeight Window height
   * @param enableFullSceneAntialiasing currently not in production
   */
  public void setWindowSize(int windowWidth, int windowHeight,
                            boolean enableFullSceneAntialiasing) {
    if (this.windowWidth == windowWidth &&
        this.windowHeight == windowHeight &&
        enableFullSceneAntialiasing == isFullSceneAntialiasingEnabled)
      return;
    this.windowWidth = windowWidth;
    this.windowHeight = windowHeight;
    isFullSceneAntialiasingEnabled = enableFullSceneAntialiasing;
    width = -1; height = -1;
    pbuf = null;
    zbuf = null;
    platform.releaseBuffers();
  }

  /**
   * is full scene / oversampling antialiasing in effect
   *
   * @return the answer
   */
  public boolean fullSceneAntialiasRendering() {
    return false;
  }

  /**
   * gets g3d width
   *
   * @return width pixel count;
   */
  public int getWindowWidth() {
    return width;
  }

  /**
   * gets g3d height
   *
   * @return height pixel count
   */
  public int getWindowHeight() {
    return height;
  }

  public int getRenderWidth() {
    return width;
  }

  public int getRenderHeight() {
    return height;
  }

  /**
   * sets background color using colix color index
   *
   * @param colix a color index represented as a short
   * @see Colix
   */
  public void setBackground(short colix) {
    colixBackground = colix;
    int argbBackground = getColixArgb(colix);
    platform.setBackground(argbBackground);

    colixBackgroundContrast =
      (calcGreyscaleRgbFromRgb(argbBackground) & 0xFF) < 128 ? WHITE : BLACK;
  }

  /**
   * black or white, whichever contrasts more with the current background
   *
   * @return black or white colix value
   */
  public short getColixBackgroundContrast() {
    return colixBackgroundContrast;
  }

  /**
   * Return a greyscale rgb value 0-FF using NTSC color luminance algorithm
   *<p>
   * the alpha component is set to 0xFF. If you want a value in the
   * range 0-255 then & the result with 0xFF;
   *
   * @param rgb the rgb value
   * @return a grayscale value in the range 0 - 255 decimal
   */
  static int calcGreyscaleRgbFromRgb(int rgb) {
    int grey = ((2989 * ((rgb >> 16) & 0xFF)) +
                (5870 * ((rgb >> 8) & 0xFF)) +
                (1140 * (rgb & 0xFF)) + 5000) / 10000;
    int greyRgb = (grey << 16) | (grey << 8) | grey | 0xFF000000;
    return greyRgb;
  }

  /**
   * controls greyscale rendering
   * @param greyscaleMode Flag for greyscale rendering
   */
  public void setGreyscaleMode(boolean greyscaleMode) {
    this.inGreyscaleMode = greyscaleMode;
  }

  /**
   * clipping from the front and the back
   *<p>
   * the plane is defined as a percentage from the back of the image
   * to the front
   *<p>
   * For slab values:
   * <ul>
   *  <li>100 means 100% is shown
   *  <li>75 means the back 75% is shown
   *  <li>50 means the back half is shown
   *  <li>0 means that nothing is shown
   * </ul>
   *<p>
   * for depth values:
   * <ul>
   *  <li>0 means 100% is shown
   *  <li>25 means the back 25% is <i>not</i> shown
   *  <li>50 means the back half is <i>not</i> shown
   *  <li>100 means that nothing is shown
   * </ul>
   *<p>
   * @param slabValue front clipping percentage [0,100]
   * @param depthValue rear clipping percentage [0,100]
   */
  public void setSlabAndDepthValues(int slabValue, int depthValue) {
    slab =
      slabValue < 0 ? 0
      : slabValue > ZBUFFER_BACKGROUND ? ZBUFFER_BACKGROUND : slabValue;
    depth =
      depthValue < 0 ? 0
      : depthValue > ZBUFFER_BACKGROUND ? ZBUFFER_BACKGROUND : depthValue;
  }

  /**
   * used internally when oversampling is enabled
   */
  private void downSampleFullSceneAntialiasing() {
    int[] pbuf1 = pbuf;
    int[] pbuf4 = pbuf;
    int width4 = width;
    int offset1 = 0;
    int offset4 = 0;
    for (int i = windowHeight; --i >= 0; ) {
      for (int j = windowWidth; --j >= 0; ) {
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

  public boolean hasContent() {
    return platform.hasContent();
  }

  /**
   * sets current color from colix color index
   * @param colix the color index
   */
  public void setColix(short colix) {
    colixCurrent = colix;
    shadesCurrent = getShades(colix);
    argbCurrent = argbNoisyUp = argbNoisyDn = getColixArgb(colix);
    isTranslucent = (colix & TRANSLUCENT_MASK) != 0;
  }

  public void setColixIntensity(short colix, int intensity) {
    colixCurrent = colix;
    shadesCurrent = getShades(colix);
    argbCurrent = argbNoisyUp = argbNoisyDn = shadesCurrent[intensity];
    isTranslucent = (colix & TRANSLUCENT_MASK) != 0;
  }

  public void setIntensity(int intensity) {
    // only adjusting intensity, but colix & isTranslucent stay the same
    argbCurrent = argbNoisyUp = argbNoisyDn = shadesCurrent[intensity];
  }

  void setColorNoisy(short colix, int intensity) {
    colixCurrent = colix;
    int[] shades = getShades(colix);
    argbCurrent = shades[intensity];
    argbNoisyUp = shades[intensity < shadeLast ? intensity + 1 : shadeLast];
    argbNoisyDn = shades[intensity > 0 ? intensity - 1 : 0];
    isTranslucent = (colix & TRANSLUCENT_MASK) != 0;
  }

  int[] imageBuf = new int[0];

  /**
   * draws a circle of the specified color at the specified location
   *
   * @param colix the color index
   * @param xyzd a long holding x, y, z, and d
   */
  public void drawCircleCentered(short colix, long xyzd) {
    drawCircleCentered(colix, Xyzd.getD(xyzd),
                       Xyzd.getX(xyzd), Xyzd.getY(xyzd), Xyzd.getZ(xyzd));
  }

  /**
   * draws a circle of the specified color at the specified location
   *
   * @param colix the color index
   * @param diameter pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void drawCircleCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    setColix(colix);
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

  /**
   * draws a screened circle ... every other dot is turned on
   *
   * @param colixFill the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void fillScreenedCircleCentered(short colixFill, int diameter, 
                                         int x, int y, int z) {
    if (diameter == 0 || z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    setColix(colixFill);
    isTranslucent = true;
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
      isTranslucent = false;
      circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter);
      isTranslucent = false;
      circle3d.plotCircleCenteredClipped(x, y, z, diameter);
    }
  }

  /**
   * fills a solid circle
   *
   * @param colixFill the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void fillCircleCentered(short colixFill, int diameter, 
                                 int x, int y, int z) {
    if (diameter == 0 || z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    setColix(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter);
    }
  }


  /**
   * fills a solid sphere
   *
   * @param colix the color index
   * @param xyzd x,y,z,diameter
   */
  public void fillSphereCentered(short colix, long xyzd) {
    int diameter = Xyzd.getD(xyzd);
    int x = Xyzd.getX(xyzd);
    int y = Xyzd.getY(xyzd);
    int z = Xyzd.getZ(xyzd);
    if (diameter <= 1) {
      plotPixelClipped(colix, x, y, z);
    } else {
      sphere3d.render(getShades(colix), ((colix & TRANSLUCENT_MASK) != 0),
                      diameter, x, y, z);
    }
  }

  /**
   * fills a solid sphere
   *
   * @param colix the color index
   * @param diameter pixel count
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void fillSphereCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (diameter <= 1) {
      plotPixelClipped(colix, x, y, z);
    } else {
      sphere3d.render(getShades(colix), ((colix & TRANSLUCENT_MASK) != 0),
                      diameter, x, y, z);
    }
  }

  /**
   * fills a solid sphere
   *
   * @param colix the color index
   * @param diameter pixel count
   * @param center javax.vecmath.Point3i defining the center
   */
  public void fillSphereCentered(short colix, int diameter, Point3i center) {
    fillSphereCentered(colix, diameter, center.x, center.y, center.z);
  }

  /**
   * fills a solid sphere
   *
   * @param colix the color index
   * @param diameter pixel count
   * @param center a javax.vecmath.Point3f ... floats are casted to ints
   */
  public void fillSphereCentered(short colix, int diameter, Point3f center) {
    fillSphereCentered(colix, diameter,
                       (int)center.x, (int)center.y, (int)center.z);
  }

  /**
   * draws a rectangle
   *
   * @param colix the color index
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param width pixel count
   * @param height pixel count
   */
  public void drawRect(short colix, int x, int y, int z,
                       int width, int height) {
    setColix(colix);
    int xRight = x + width - 1;
    line3d.drawHLine(argbCurrent, isTranslucent, x, y, z, width -1, true);
    int yBottom = y + height - 1;
    line3d.drawVLine(argbCurrent, isTranslucent, x, y, z, height - 1, true);
    line3d.drawVLine(argbCurrent, isTranslucent,
                     xRight, y, z, height - 1, true);
    line3d.drawHLine(argbCurrent, isTranslucent, x, yBottom, z, width, true);
  }

  /**
   * draws a rectangle while ignoring slab/depth clipping
   *<p>
   * could be useful for UI work
   *
   * @param colix the color index
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param width pixel count
   * @param height pixel count
   */
  public void drawRectNoSlab(short colix, int x, int y, int z,
                       int width, int height) {
    setColix(colix);
    int xRight = x + width - 1;
    line3d.drawHLine(argbCurrent, isTranslucent, x, y, z, width -1, false);
    int yBottom = y + height - 1;
    line3d.drawVLine(argbCurrent, isTranslucent, x, y, z, height - 1, false);
    line3d.drawVLine(argbCurrent, isTranslucent,
                     xRight, y, z, height - 1, false);
    line3d.drawHLine(argbCurrent, isTranslucent, x, yBottom, z, width, false);
  }

  /**
   * draws the specified string in the current font.
   * no line wrapping
   *
   * @param str the string
   * @param colix the color index
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */
  public void drawString(String str, short colix,
                         int xBaseline, int yBaseline, int z) {
    drawString(str, font3dCurrent, colix, (short)0, xBaseline, yBaseline, z);
  }
  
  /**
   * draws the specified string in the current font.
   * no line wrapping
   *
   * @param str the String
   * @param font3d the Font3D
   * @param colix the color index
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */
  public void drawString(String str, Font3D font3d, short colix,
                         int xBaseline, int yBaseline, int z) {
    drawString(str, font3d, colix, (short)0, xBaseline, yBaseline, z);
  }
  
  /**
   * draws the specified string in the current font.
   * no line wrapping
   *
   * @param str the String
   * @param font3d the Font3D
   * @param colix the color index
   * @param bgcolix the color index of the background
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */
  public void drawString(String str, Font3D font3d, short colix, short bgcolix,
                         int xBaseline, int yBaseline, int z) {
    //    System.out.println("Graphics3D.drawString(" + str + "," + font3d +
    //                       ", ...)");

    font3dCurrent = font3d;
    setColix(colix);
    if (z < slab || z > depth)
      return;
    //    System.out.println("ready to call");
    Text3D.plot(xBaseline, yBaseline - font3d.fontMetrics.getAscent(),
                z, argbCurrent, getColixArgb(bgcolix), str, font3dCurrent, this);
    //    System.out.println("done");
  }

  public void drawStringNoSlab(String str, Font3D font3d, short colix,
                               short bgcolix, int xBaseline, int yBaseline,
                               int z) {
    font3dCurrent = font3d;
    setColix(colix);
    Text3D.plot(xBaseline, yBaseline - font3d.fontMetrics.getAscent(),
                z, argbCurrent, getColixArgb(bgcolix), str, font3dCurrent, this);
  }
  
  public void setFontOfSize(int fontsize) {
    font3dCurrent = getFont3D(fontsize);
  }

  public void setFont(byte fid) {
    font3dCurrent = Font3D.getFont3D(fid);
  }
  
  public void setFont(Font3D font3d) {
    font3dCurrent = font3d;
  }
  
  public Font3D getFont3DCurrent() {
    return font3dCurrent;
  }

  public byte getFontFidCurrent() {
    return font3dCurrent.fid;
  }
  
  public FontMetrics getFontMetrics() {
    return font3dCurrent.fontMetrics;
  }

  boolean currentlyRendering;

  private void setRectClip(int x, int y, int width, int height) {
    if (x < 0)
      x = 0;
    if (y < 0)
      y = 0;
    if (x + width > windowWidth)
      width = windowWidth - x;
    if (y + height > windowHeight)
      height = windowHeight - y;
    clipX = x;
    clipY = y;
    clipWidth = width;
    clipHeight = height;
    if (antialiasThisFrame) {
      clipX *= 2;
      clipY *= 2;
      clipWidth *= 2;
      clipHeight *= 2;
    }
  }


  // 3D specific routines
  public void beginRendering(int clipX, int clipY,
                             int clipWidth, int clipHeight,
                             Matrix3f rotationMatrix,
                             boolean antialiasThisFrame) {
    if (currentlyRendering)
      endRendering();
    normix3d.setRotationMatrix(rotationMatrix);
    antialiasThisFrame &= isFullSceneAntialiasingEnabled;
    this.antialiasThisFrame = antialiasThisFrame;
    currentlyRendering = true;
    if (pbuf == null) {
      platform.allocateBuffers(windowWidth, windowHeight,
                               isFullSceneAntialiasingEnabled);
      pbuf = platform.pBuffer;
      zbuf = platform.zBuffer;
      width = windowWidth;
      xLast = width - 1;
      height = windowHeight;
      yLast = height - 1;
    }
    width = windowWidth;
    height = windowHeight;
    if (antialiasThisFrame) {
      width *= 2;
      height *= 2;
    }
    xLast = width - 1;
    yLast = height - 1;
    setRectClip(clipX, clipY, clipWidth, clipHeight);
    platform.obtainScreenBuffer();
  }

  public void endRendering() {
    if (currentlyRendering) {
      if (antialiasThisFrame)
        downSampleFullSceneAntialiasing();
      platform.notifyEndOfRendering();
      currentlyRendering = false;
    }
  }

  public void snapshotAnaglyphChannelBytes() {
    if (currentlyRendering)
      throw new NullPointerException();
    if (anaglyphChannelBytes == null ||
	anaglyphChannelBytes.length != pbuf.length)
      anaglyphChannelBytes = new byte[pbuf.length];
    for (int i = pbuf.length; --i >= 0; )
      anaglyphChannelBytes[i] = (byte)pbuf[i];
  }

  public void applyBlueOrGreenAnaglyph(boolean blueChannel) {
    int shiftCount = blueChannel ? 0 : 8;
    for (int i = pbuf.length; --i >= 0; )
      pbuf[i] = ((pbuf[i] & 0xFFFF0000) |
		 ((anaglyphChannelBytes[i] & 0x000000FF) << shiftCount));
  }

  public void applyCyanAnaglyph() {
    for (int i = pbuf.length; --i >= 0; ) {
      int blueAndGreen = anaglyphChannelBytes[i] & 0x000000FF;
      int cyan = (blueAndGreen << 8) | blueAndGreen;
      pbuf[i] = pbuf[i] & 0xFFFF0000 | cyan;
    }
  }
  
  public Image getScreenImage() {
    return platform.imagePixelBuffer;
  }

  public void releaseScreenImage() {
    platform.clearScreenBufferThreaded();
  }

  public void drawDashedLine(short colix, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getColixArgb(colix);
    line3d.drawDashedLine(argb, isTranslucent, argb, isTranslucent,
                          run, rise, x1, y1, z1, x2, y2, z2);
  }

  public void drawDottedLine(short colix,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getColixArgb(colix);
    line3d.drawDashedLine(argb, isTranslucent, argb, isTranslucent,
                          2, 1, x1, y1, z1, x2, y2, z2);
  }

  public void drawDashedLine(short colix1, short colix2, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
        
    line3d.drawDashedLine(getColixArgb(colix1), isColixTranslucent(colix1),
                          getColixArgb(colix2), isColixTranslucent(colix2),
                          run, rise, x1, y1, z1, x2, y2, z2);
  }
  

  public void drawLine(Point3i pointA, Point3i pointB) {
    line3d.drawLine(argbCurrent, isTranslucent, argbCurrent, isTranslucent,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawLine(short colix, Point3i pointA, Point3i pointB) {
    setColix(colix);
    line3d.drawLine(argbCurrent, isTranslucent, argbCurrent, isTranslucent,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawLine(short colix, long xyzdA, long xyzdB) {
    setColix(colix);
    line3d.drawLine(argbCurrent, isTranslucent, argbCurrent, isTranslucent,
                    Xyzd.getX(xyzdA), Xyzd.getY(xyzdA), Xyzd.getZ(xyzdA),
                    Xyzd.getX(xyzdB), Xyzd.getY(xyzdB), Xyzd.getZ(xyzdB));
  }

  public void drawDottedLine(short colix, Point3i pointA, Point3i pointB) {
    drawDashedLine(colix, 2, 1, pointA, pointB);
  }

  public void drawDashedLine(short colix, int run, int rise,
                             Point3i pointA, Point3i pointB) {
    setColix(colix);
    line3d.drawDashedLine(argbCurrent, isTranslucent,
                          argbCurrent, isTranslucent,
                          run, rise,
                          pointA.x, pointA.y, pointA.z,
                          pointB.x, pointB.y, pointB.z);
  }

  public void drawDashedLine(int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawDashedLine(argbCurrent, isTranslucent,
                          argbCurrent, isTranslucent,
                          run, rise, x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(argbCurrent, isTranslucent, argbCurrent, isTranslucent,
                    x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    setColix(colix);
    line3d.drawLine(argbCurrent, isTranslucent, argbCurrent, isTranslucent,
                    x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix1, short colix2,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(getColixArgb(colix1), isColixTranslucent(colix1),
                    getColixArgb(colix2), isColixTranslucent(colix2),
                    x1, y1, z1, x2, y2, z2);
  }
  
  public void drawPolygon4(int[] ax, int[] ay, int[] az) {
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillQuadrilateral(short colix,
                                Point3f screenA, Point3f screenB,
                                Point3f screenC, Point3f screenD) {
    /*
    System.out.println("fillQuad----------------");
    System.out.println("screenA="+ screenA +
                       "\nscreenB=" + screenB +
                       "\nscreenC=" + screenC +
                       "\nscreenD=" + screenD);
    */
    setColorNoisy(colix, calcIntensityScreen(screenA, screenB, screenC));
    fillTriangle(screenA, screenB, screenC);
    fillTriangle(screenA, screenC, screenD);
  }

  public void fillTriangle(short colix,
                           Point3i screenA, short normixA,
                           Point3i screenB, short normixB,
                           Point3i screenC, short normixC) {
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    if (normixA == normixB && normixA == normixC) {
      setColorNoisy(colix, normix3d.getIntensity(normixA));
      triangle3d.fillTriangle(false);
    } else {
      setColix(colix);
      triangle3d.setGouraud(shadesCurrent[normix3d.getIntensity(normixA)],
                            shadesCurrent[normix3d.getIntensity(normixB)],
                            shadesCurrent[normix3d.getIntensity(normixC)]);
      triangle3d.fillTriangle(true);
    }
  }

  public void fillTriangle(Point3i screenA, short colixA, short normixA,
                           Point3i screenB, short colixB, short normixB,
                           Point3i screenC, short colixC, short normixC) {
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    if (normixA == normixB && normixA == normixC &&
        colixA == colixB && colixA == colixC) {
      setColorNoisy(colixA, normix3d.getIntensity(normixA));
      triangle3d.fillTriangle(false);
    } else {
      triangle3d.setGouraud(getShades(colixA)[normix3d.getIntensity(normixA)],
                            getShades(colixB)[normix3d.getIntensity(normixB)],
                            getShades(colixC)[normix3d.getIntensity(normixC)]);
      int translucentCount = 0;
      if (isColixTranslucent(colixA))
        ++translucentCount;
      if (isColixTranslucent(colixB))
        ++translucentCount;
      if (isColixTranslucent(colixC))
        ++translucentCount;
      isTranslucent = translucentCount >= 2;
      triangle3d.fillTriangle(true);
    }
  }

  public void fillTriangle(short colix,
                           Point3i screenA, Point3i screenB, Point3i screenC) {
    calcSurfaceShade(colix, screenA, screenB, screenC);
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangle(false);
  }

  public void fillTriangle(short colix, long xyzdA, long xyzdB, long xyzdC) {
    calcSurfaceShade(colix, xyzdA, xyzdB, xyzdC);
    int[] t;
    t = triangle3d.ax;
    t[0] = Xyzd.getX(xyzdA); t[1] = Xyzd.getX(xyzdB); t[2] = Xyzd.getX(xyzdC);
    t = triangle3d.ay;
    t[0] = Xyzd.getY(xyzdA); t[1] = Xyzd.getY(xyzdB); t[2] = Xyzd.getY(xyzdC);
    t = triangle3d.az;
    t[0] = Xyzd.getZ(xyzdA); t[1] = Xyzd.getZ(xyzdB); t[2] = Xyzd.getZ(xyzdC);

    triangle3d.fillTriangle(false);
  }

  public void fillTriangle(short colix, short normix,
                           long xyzdA, long xyzdB, long xyzdC) {
    setColorNoisy(colix, normix3d.getIntensity(normix));
    int[] t;
    t = triangle3d.ax;
    t[0] = Xyzd.getX(xyzdA); t[1] = Xyzd.getX(xyzdB); t[2] = Xyzd.getX(xyzdC);
    t = triangle3d.ay;
    t[0] = Xyzd.getY(xyzdA); t[1] = Xyzd.getY(xyzdB); t[2] = Xyzd.getY(xyzdC);
    t = triangle3d.az;
    t[0] = Xyzd.getZ(xyzdA); t[1] = Xyzd.getZ(xyzdB); t[2] = Xyzd.getZ(xyzdC);

    triangle3d.fillTriangle(false);
  }

  public void fillTriangle(short colix, Point3f screenA,
                           Point3f screenB, Point3f screenC) {
    setColorNoisy(colix, calcIntensityScreen(screenA, screenB, screenC));
    fillTriangle(screenA, screenB, screenC);
  }

  public void fillQuadrilateral(short colix,
                                Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    fillTriangle(colix, screenA, screenB, screenC);
    fillTriangle(colix, screenA, screenC, screenD);
  }

  public void fillQuadrilateral(short colix,
                                Point3i screenA, short normixA,
                                Point3i screenB, short normixB,
                                Point3i screenC, short normixC,
                                Point3i screenD, short normixD) {
    fillTriangle(colix,
                 screenA, normixA,
                 screenB, normixB,
                 screenC, normixC);
    fillTriangle(colix,
                 screenA, normixA,
                 screenC, normixC,
                 screenD, normixD);
  }

  public void fillQuadrilateral(Point3i screenA, short colixA, short normixA,
                                Point3i screenB, short colixB, short normixB,
                                Point3i screenC, short colixC, short normixC,
                                Point3i screenD, short colixD, short normixD) {
    fillTriangle(screenA, colixA, normixA,
                 screenB, colixB, normixB,
                 screenC, colixC, normixC);
    fillTriangle(screenA, colixA, normixA,
                 screenC, colixC, normixC,
                 screenD, colixD, normixD);
  }

  public void fillTriangle(Point3i screenA, Point3i screenB, Point3i screenC) {
    
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangle(false);
  }

  public void fillTriangle(Point3f screenA, Point3f screenB, Point3f screenC) {
    int[] t;
    t = triangle3d.ax;
    t[0] = (int)screenA.x; t[1] = (int)screenB.x; t[2] = (int)screenC.x;
    t = triangle3d.ay;
    t[0] = (int)screenA.y; t[1] = (int)screenB.y; t[2] = (int)screenC.y;
    t = triangle3d.az;
    t[0] = (int)screenA.z; t[1] = (int)screenB.z; t[2] = (int)screenC.z;

    triangle3d.fillTriangle(false);
  }

  int intensity = 0;
  
  void diff(Vector3f v, Point3i s1, Point3i s2) {
    v.x = s1.x - s2.x;
    v.y = s1.y - s2.y;
    v.z = s1.z - s2.z;
  }

  final static Point3i tmpScreenA = new Point3i();
  final static Point3i tmpScreenB = new Point3i();
  final static Point3i tmpScreenC = new Point3i();

  public void calcSurfaceShade(short colix,
                               long xyzdA, long xyzdB, long xyzdC) {
    Xyzd.setPoint3i(xyzdA, tmpScreenA);
    Xyzd.setPoint3i(xyzdB, tmpScreenB);
    Xyzd.setPoint3i(xyzdC, tmpScreenC);
    calcSurfaceShade(colix, tmpScreenA, tmpScreenB, tmpScreenC);
  }

  public void calcSurfaceShade(short colix, Point3i screenA,
                               Point3i screenB, Point3i screenC) {
    diff(vectorAB, screenB, screenA);
    diff(vectorAC, screenC, screenA);
    vectorNormal.cross(vectorAB, vectorAC);
    int intensity =
      vectorNormal.z >= 0
      ? calcIntensity(-vectorNormal.x, -vectorNormal.y, vectorNormal.z)
      : calcIntensity(vectorNormal.x, vectorNormal.y, -vectorNormal.z);
    if (intensity > intensitySpecularSurfaceLimit)
      intensity = intensitySpecularSurfaceLimit;
    setColorNoisy(colix, intensity);
  }

  public void drawfillTriangle(short colix, int xA, int yA, int zA, int xB,
                               int yB, int zB, int xC, int yC, int zC) {
    setColix(colix);
    int argb = argbCurrent;
    line3d.drawLine(argb, isTranslucent, argb, isTranslucent,
                    xA, yA, zA, xB, yB, zB);
    line3d.drawLine(argb, isTranslucent, argb, isTranslucent,
                    xA, yA, zA, xC, yC, zC);
    line3d.drawLine(argb, isTranslucent, argb, isTranslucent,
                    xB, yB, zB, xC, yC, zC);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangle(false);
  }

  public void fillTriangle(short colix, boolean translucent,
                           int xA, int yA, int zA,
                           int xB, int yB, int zB, int xC, int yC, int zC) {
    /*
    System.out.println("fillTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    setColix(colix);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangle(false);
  }

  /*
  final Point3i warrenA = new Point3i();
  final Point3i warrenB = new Point3i();
  final Point3i warrenC = new Point3i();

  public void fillTriangleWarren(int argb,
                                 int xA, int yA, int zA,
                                 int xB, int yB, int zB,
                                 int xC, int yC, int zC) {
    warrenA.x = xA; warrenA.y = yA; warrenA.z = zA;
    warrenB.x = xB; warrenB.y = yB; warrenB.z = zB;
    warrenC.x = xC; warrenC.y = yC; warrenC.z = zC;

    short colix = getColix(argb);

    calcSurfaceShade(colix, false, warrenA, warrenB, warrenC);

    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangle(false, false);
  }
  */

  public void drawTriangle(short colix,
                           int xA, int yA, int zA,
                           int xB, int yB, int zB,
                           int xC, int yC, int zC) {
    /*
    System.out.println("drawTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    setColix(colix);
    drawLine(xA, yA, zA, xB, yB, zB);
    drawLine(xA, yA, zA, xC, yC, zC);
    drawLine(xB, yB, zB, xC, yC, zC);
  }

  public void drawCylinderTriangle(short colix,
                           int xA, int yA, int zA,
                           int xB, int yB, int zB,
                           int xC, int yC, int zC, int diameter) {   
    fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, diameter,
        xA, yA, zA, xB, yB, zB);
    fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, diameter,
        xA, yA, zA, xC, yC, zC);
    fillCylinder(colix, colix, Graphics3D.ENDCAPS_SPHERICAL, diameter,
        xB, yB, zB, xC, yC, zC);
  }

  public void drawTriangle(short colix, long xyzdA, long xyzdB, long xyzdC) {
    drawTriangle(colix,
                 Xyzd.getX(xyzdA), Xyzd.getY(xyzdA), Xyzd.getZ(xyzdA),
                 Xyzd.getX(xyzdB), Xyzd.getY(xyzdB), Xyzd.getZ(xyzdB),
                 Xyzd.getX(xyzdC), Xyzd.getY(xyzdC), Xyzd.getZ(xyzdC));
  }

  public void drawCylinderTriangle(short colix, long xyzdA, long xyzdB,
                                   long xyzdC, int diameter) {
    drawCylinderTriangle(colix, 
        Xyzd.getX(xyzdA), Xyzd.getY(xyzdA), Xyzd.getZ(xyzdA),
        Xyzd.getX(xyzdB), Xyzd.getY(xyzdB), Xyzd.getZ(xyzdB),
        Xyzd.getX(xyzdC), Xyzd.getY(xyzdC), Xyzd.getZ(xyzdC), diameter);
  }

  public void drawTriangle(short colix, Point3i screenA,
                           Point3i screenB, Point3i screenC) {
    drawTriangle(colix,
                 screenA.x, screenA.y, screenA.z,
                 screenB.x, screenB.y, screenB.z,
                 screenC.x, screenC.y, screenC.z);
  }

  public void drawQuadrilateral(short colix,
                                Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    setColix(colix);
    drawLine(screenA, screenB);
    drawLine(screenB, screenC);
    drawLine(screenC, screenD);
    drawLine(screenD, screenA);
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

  public void drawHermite(boolean fill, boolean border,
                          short colix, int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3,
                          Point3i s4, Point3i s5, Point3i s6, Point3i s7) {
    hermite3d.render2(fill, border, colix, tension,
                      s0, s1, s2, s3, s4, s5, s6, s7);
  }
  
  public void fillRect(short colix,
                       int x, int y, int z, int widthFill, int heightFill) {
    setColix(colix);
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
    while (--heightFill >= 0)
      plotPixelsUnclipped(widthFill, x, y++, z);
  }

  public void drawPixel(Point3i point) {
    plotPixelClipped(point);
  }

  public void drawPixel(int x, int y, int z) {
    plotPixelClipped(x, y, z);
  }

  public void drawPixel(Point3i point, int normix) {
    plotPixelClipped(shadesCurrent[normix3d.intensities[normix]],
                     point.x, point.y, point.z);
  }

  /* ***************************************************************
   * the plotting routines
   * ***************************************************************/


  void plotPixelClipped(int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
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
    int z = screen.z; if (z < slab || z > depth) return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(int argb, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelClippedNoSlab(int argb, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelClipped(int argb, boolean isTranslucent, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    if (isTranslucent && ((x ^ y) & 1) != 0)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelClipped(short colix, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = getColixArgb(colix);
    }
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
    if (y < 0 || y >= height || x >= width || z < slab || z > depth)
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
    if (isTranslucent) {
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

  void plotPixelsClipped(int count, int x, int y,
                         int zAtLeft, int zPastRight,
                         Rgb16 rgb16Left, Rgb16 rgb16Right) {
    //    System.out.print("plotPixelsClipped z values:");
    /*
    System.out.println("plotPixelsClipped count=" + count + "x,y,z=" +
                       x + "," + y + "," + zAtLeft + " -> " + zPastRight);
    */
    if (count <= 0 || y < 0 || y >= height || x >= width ||
        (zAtLeft < slab && zPastRight < slab) ||
        (zAtLeft > depth && zPastRight > depth))
      return;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
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
    // when screening 0,0 should be turned ON
    // the first time through this will get flipped to true
    boolean flipflop = ((x ^ y) & 1) != 0;
    int offsetPbuf = y * width + x;
    if (rgb16Left == null) {
      while (--count >= 0) {
        if (! isTranslucent || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z >= slab && z <= depth && z < zbuf[offsetPbuf]) {
            zbuf[offsetPbuf] = (short)z;
            seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
            int bits = (seed >> 16) & 0x07;
            pbuf[offsetPbuf] = (bits == 0
                                ? argbNoisyDn
                                : (bits == 1 ? argbNoisyUp : argbCurrent));
          }
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
      }
    } else {
      int rScaled = rgb16Left.rScaled << 8;
      int rIncrement = ((rgb16Right.rScaled - rgb16Left.rScaled) << 8) / count;
      int gScaled = rgb16Left.gScaled;
      int gIncrement = (rgb16Right.gScaled - gScaled) / count;
      int bScaled = rgb16Left.bScaled;
      int bIncrement = (rgb16Right.bScaled - bScaled) / count;
      while (--count >= 0) {
        if (! isTranslucent || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z >= slab && z <= depth && z < zbuf[offsetPbuf]) {
            zbuf[offsetPbuf] = (short)z;
            pbuf[offsetPbuf] = (0xFF000000 |
                                (rScaled & 0xFF0000) |
                                (gScaled & 0xFF00) |
                                ((bScaled >> 8) & 0xFF));
          }
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  final static boolean ENABLE_GOURAUD_STATS = true;
  static int totalGouraud;
  static int shortCircuitGouraud;

  void plotPixelsUnclipped(int count, int x, int y,
                           int zAtLeft, int zPastRight,
                           Rgb16 rgb16Left, Rgb16 rgb16Right) {
    if (count <= 0)
      return;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled =
      ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))/count;
    int offsetPbuf = y * width + x;
    if (rgb16Left == null) {
      if (! isTranslucent) {
        while (--count >= 0) {
          int z = zScaled >> 10;
          if (z < zbuf[offsetPbuf]) {
            zbuf[offsetPbuf] = (short)z;
            seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
            int bits = (seed >> 16) & 0x07;
            pbuf[offsetPbuf] = (bits == 0
                                ? argbNoisyDn
                                : (bits == 1 ? argbNoisyUp : argbCurrent));
          }
          ++offsetPbuf;
          zScaled += zIncrementScaled;
        }
      } else {
        boolean flipflop = ((x ^ y) & 1) != 0;
        while (--count >= 0) {
          flipflop = !flipflop;
          if (flipflop) {
            int z = zScaled >> 10;
            if (z < zbuf[offsetPbuf]) {
              zbuf[offsetPbuf] = (short)z;
              seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
              int bits = (seed >> 16) & 0x07;
              pbuf[offsetPbuf] = (bits == 0
                                  ? argbNoisyDn
                                  : (bits == 1 ? argbNoisyUp : argbCurrent));
            }
          }
          ++offsetPbuf;
          zScaled += zIncrementScaled;
        }
      }
    } else {
      boolean flipflop = ((x ^ y) & 1) != 0;
      if (ENABLE_GOURAUD_STATS) {
        ++totalGouraud;
        int i = count;
        int j = offsetPbuf;
        int zMin = zAtLeft < zPastRight ? zAtLeft : zPastRight;
        
        if (! isTranslucent) {
          for ( ; zbuf[j] < zMin; ++j)
            if (--i == 0) {
              if ((++shortCircuitGouraud % 100000) == 0)
                System.out.println("totalGouraud=" + totalGouraud +
                                   " shortCircuitGouraud=" +
                                   shortCircuitGouraud
                                   + " %=" +
                                   (100.0 * shortCircuitGouraud /
                                    totalGouraud));
              return;
            }
        } else {
          if (flipflop) {
            ++j;
            if (--i == 0)
              return;
          }
          for ( ; zbuf[j] < zMin; j += 2) {
            i -= 2;
            if (i <= 0) {
              if ((++shortCircuitGouraud % 100000) == 0)
                System.out.println("totalGouraud=" + totalGouraud +
                                   " shortCircuitGouraud=" +
                                   shortCircuitGouraud
                                   + " %=" +
                                   (100.0 * shortCircuitGouraud /
                                    totalGouraud));
              return;
            }
          }
        }
      }

      int rScaled = rgb16Left.rScaled << 8;
      int rIncrement = ((rgb16Right.rScaled - rgb16Left.rScaled) << 8) / count;
      int gScaled = rgb16Left.gScaled;
      int gIncrement = (rgb16Right.gScaled - gScaled) / count;
      int bScaled = rgb16Left.bScaled;
      int bIncrement = (rgb16Right.bScaled - bScaled) / count;
      while (--count >= 0) {
        if (! isTranslucent || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z < zbuf[offsetPbuf]) {
            zbuf[offsetPbuf] = (short)z;
            pbuf[offsetPbuf] = (0xFF000000 |
                                (rScaled & 0xFF0000) |
                                (gScaled & 0xFF00) |
                                ((bScaled >> 8) & 0xFF));
          }
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  void plotPixelsUnclipped(int count, int x, int y, int z) {
    int offsetPbuf = y * width + x;
    if (! isTranslucent) {
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
    if (y < 0 || y >= height || x >= width || z < slab || z > depth)
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
  
  void plotLineDelta(int[] shades1, boolean isTranslucent1,
                     int[] shades2, boolean isTranslucent2,
                     int fp8Intensity,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab || z > depth || z + dz > depth)
      line3d.plotLineDeltaClipped(shades1, isTranslucent1,
                                  shades2, isTranslucent2,
                                  fp8Intensity,
                                  x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(shades1, isTranslucent1,
                                    shades2, isTranslucent2,
                                    fp8Intensity,
                                    x, y, z, dx, dy, dz);
  }

  void plotLineDelta(short colixA, short colixB,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab || z > depth || z + dz > depth)
      line3d.plotLineDeltaClipped(getColixArgb(colixA),
                                  isColixTranslucent(colixA),
                                  getColixArgb(colixB),
                                  isColixTranslucent(colixB),
                                  x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(getColixArgb(colixA),
                                    isColixTranslucent(colixA),
                                    getColixArgb(colixB),
                                    isColixTranslucent(colixB),
                                    x, y, z, dx, dy, dz);
  }

  void plotLineDelta(int argb1, boolean isTranslucent1,
                     int argb2, boolean isTranslucent2,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab || z > depth || z + dz > depth)
      line3d.plotLineDeltaClipped(argb1, isTranslucent1,
                                  argb2, isTranslucent2,
                                  x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(argb1, isTranslucent1,
                                    argb2, isTranslucent2,
                                    x, y, z, dx, dy, dz);
  }

  public void plotPoints(short colix, int count, int[] coordinates) {
    setColix(colix);
    int argb = argbCurrent;
    for (int i = count * 3; i > 0; ) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
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
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
        continue;
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        //        pbuf[offset] = getColixArgb(colix);
        pbuf[offset] = shades[intensities[j]];
      }
    }
  }

  void averageOffsetArgb(int offset, int argb) {
    pbuf[offset] =((((pbuf[offset] >> 1) & 0x007F7F7F) +
                    ((argb >> 1) & 0xFF7F7F7F)) |
                   (argb & 0xFF010101));
  }

  /* entries 0 through 3 are reserved and are special
     TRANSLUCENT and OPAQUE are used to inherit
     the underlying color, but change the translucency

     Note that colors are not actually translucent. Rather,
     they are 'screened' where every-other pixel is turned
     on. 
  */
  final static short TRANSLUCENT_MASK = 0x4000;
  final static short OPAQUE_MASK  = ~TRANSLUCENT_MASK;
  final static short CHANGABLE_MASK = (short)0x8000; // negative
  final static short UNMASK_CHANGABLE_TRANSLUCENT = 0x3FFF;

  public final static short NULL_COLIX   = 0;
  public final static short TRANSLUCENT  = 1;
  public final static short OPAQUE       = 2;
  public final static short UNRECOGNIZED = 3;
  public final static short SPECIAL_COLIX_MAX = 4;

  public final static short BLACK       = 4;
  public final static short ORANGE      = 5;
  public final static short PINK        = 6;
  public final static short BLUE        = 7;
  public final static short WHITE       = 8;
  public final static short CYAN        = 9;
  public final static short RED         = 10;
  public final static short GREEN       = 11;
  public final static short GRAY        = 12;
  public final static short SILVER      = 13;
  public final static short LIME        = 14;
  public final static short MAROON      = 15;
  public final static short NAVY        = 16;
  public final static short OLIVE       = 17;
  public final static short PURPLE      = 18;
  public final static short TEAL        = 19;
  public final static short MAGENTA     = 20;
  public final static short YELLOW      = 21;
  public final static short HOTPINK     = 22;
  public final static short GOLD        = 23;

  static int[] predefinedArgbs = {
    0xFF000000, // black
    0xFFFFA500, // orange
    0xFFFFC0CB, // pink
    0xFF0000FF, // blue
    0xFFFFFFFF, // white
    0xFF00FFFF, // cyan
    0xFFFF0000, // red
    0xFF008000, // green
    0xFF808080, // gray
    0xFFC0C0C0, // silver
    0xFF00FF00, // lime
    0xFF800000, // maroon
    0xFF000080, // navy
    0xFF808000, // olive
    0xFF800080, // purple
    0xFF008080, // teal
    0xFFFF00FF, // magenta
    0xFFFFFF00, // yellow
    0xFFFF69B4, // hotpink
    0xFFFFD700, // gold
  };

  static {
    for (int i = 0; i < predefinedArgbs.length; ++i)
      if (Colix.getColix(predefinedArgbs[i]) != i + SPECIAL_COLIX_MAX)
        throw new NullPointerException();
  }

  public int getColixArgb(short colix) {
    if (colix < 0)
      colix = changableColixMap[colix & UNMASK_CHANGABLE_TRANSLUCENT];
    if (! inGreyscaleMode)
      return Colix.getArgb(colix);
    return Colix.getArgbGreyscale(colix);
  }

  public int[] getShades(short colix) {
    if (colix < 0)
      colix = changableColixMap[colix & UNMASK_CHANGABLE_TRANSLUCENT];
    if (! inGreyscaleMode)
      return Colix.getShades(colix);
    return Colix.getShadesGreyscale(colix);
  }

  public final static short getChangableColixIndex(short colix) {
    if (colix >= 0)
      return -1;
    return (short)(colix & UNMASK_CHANGABLE_TRANSLUCENT);
  }

  public final static boolean isColixTranslucent(short colix) {
    return (colix & TRANSLUCENT_MASK) != 0;
  }

  public final static short getTranslucentColix(short colix,
                                                boolean translucent) {
    return (short)(translucent ?
                   (colix | TRANSLUCENT_MASK) :
                   (colix & OPAQUE_MASK));
  }

  public final static short getTranslucentColix(short colix) {
    return (short)(colix | TRANSLUCENT_MASK);
  }

  public final static short getOpaqueColix(short colix) {
    return (short)(colix & OPAQUE_MASK);
  }

  public final static short getColix(int argb) {
    return Colix.getColix(argb);
  }

  public final short getColixMix(short colixA, short colixB) {
    return Colix.getColixMix(colixA >= 0 ? colixA :
                             changableColixMap[colixA &
                                               UNMASK_CHANGABLE_TRANSLUCENT],
                             colixB >= 0 ? colixB :
                             changableColixMap[colixB &
                                               UNMASK_CHANGABLE_TRANSLUCENT]);
  }

  public final static short setTranslucent(short colix,
                                           boolean isTranslucent) {
    if (isTranslucent) {
      if (colix >= 0 && colix < SPECIAL_COLIX_MAX)
        return TRANSLUCENT;
      return (short)(colix | TRANSLUCENT_MASK);
    }
    if (colix >= 0 && colix < SPECIAL_COLIX_MAX)
      return OPAQUE;
    return (short)(colix & OPAQUE_MASK);
  }

  public final static short getColix(String colorName) {
    int argb = getArgbFromString(colorName);
    if (argb != 0)
      return getColix(argb);
    if ("none".equalsIgnoreCase(colorName))
      return 0;
    if ("translucent".equalsIgnoreCase(colorName))
      return TRANSLUCENT;
    if ("opaque".equalsIgnoreCase(colorName))
      return OPAQUE;
    return UNRECOGNIZED;
  }

  public final static short getColix(Object obj) {
    if (obj == null)
      return 0;
    if (obj instanceof Integer)
      return getColix(((Integer)obj).intValue());
    if (obj instanceof String)
      return getColix((String)obj);
    System.out.println("?? getColix(" + obj + ")");
    return HOTPINK;
  }

  public final static short inheritColix(short myColix, short parentColix) {
    switch (myColix) {
    case 0:
      return parentColix;
    case TRANSLUCENT:
      return (short)(parentColix | TRANSLUCENT_MASK);
    case OPAQUE:
      return (short)(parentColix & OPAQUE_MASK);
    default:
      return myColix;
    }
  }

  public final static short inheritColix(short myColix,
                                         short parentColix,
                                         short grandParentColix) {
    if (myColix >= SPECIAL_COLIX_MAX)
      return myColix;
    parentColix = inheritColix(parentColix, grandParentColix);
    if (myColix == 0)
      return parentColix;
    return inheritColix(myColix, parentColix);
  }

  public String getHexColorFromIndex(short colix) {
    int argb = getColixArgb(colix);
    if (argb == 0)
      return null;
    String r  = Integer.toHexString((argb >> 16) & 0xFF);
    String g  = Integer.toHexString((argb >> 8) & 0xFF);
    String b  = Integer.toHexString(argb & 0xFF);
    return "#" + r + g + b;
  }

  /****************************************************************
   * changable colixes
   * give me a short ID and a color, and I will give you a colix
   * later, you can reassign the color if you want
   ****************************************************************/

  short[] changableColixMap = new short[16];

  public short getChangableColix(short id, int argb) {
    if (id >= changableColixMap.length) {
      short[] t = new short[id + 16];
      System.arraycopy(changableColixMap, 0, t, 0, changableColixMap.length);
      changableColixMap = t;
    }
    if (changableColixMap[id] == 0)
      changableColixMap[id] = getColix(argb);
    return (short)(id | CHANGABLE_MASK);
  }

  public void changeColixArgb(short id, int argb) {
    if (id < changableColixMap.length && changableColixMap[id] != 0)
      changableColixMap[id] = getColix(argb);
  }

  public void flushShadesAndImageCaches() {
    Colix.flushShades();
    Sphere3D.flushImageCache();
  }

  public final static byte shadeMax = Shade3D.shadeMax;
  public final static byte shadeLast = Shade3D.shadeMax - 1;
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

  private final Vector3f vectorAB = new Vector3f();
  private final Vector3f vectorAC = new Vector3f();
  private final Vector3f vectorNormal = new Vector3f();

  // these points are in screen coordinates
  public int calcIntensityScreen(Point3f screenA,
                                 Point3f screenB, Point3f screenC) {
    vectorAB.sub(screenB, screenA);
    vectorAC.sub(screenC, screenA);
    vectorNormal.cross(vectorAB, vectorAC);
    return
      (vectorNormal.z >= 0
            ? Shade3D.calcIntensity(-vectorNormal.x, -vectorNormal.y,
                                    vectorNormal.z)
            : Shade3D.calcIntensity(vectorNormal.x, vectorNormal.y,
                                    -vectorNormal.z));
  }


  static public int calcIntensity(float x, float y, float z) {
    return Shade3D.calcIntensity(x, y, z);
  }

  /* ***************************************************************
   * fontID stuff
   * a fontID is a byte that contains the size + the face + the style
   * ***************************************************************/

  public Font3D getFont3D(int fontSize) {
    return Font3D.getFont3D(Font3D.FONT_FACE_SANS,
                            Font3D.FONT_STYLE_PLAIN, fontSize, platform);
  }

  public Font3D getFont3D(String fontFace, int fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.FONT_STYLE_PLAIN, fontSize, platform);
  }
    
  // {"Plain", "Bold", "Italic", "BoldItalic"};
  public Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.getFontStyleID(fontStyle), fontSize, platform);
  }

  public byte getFontFid(int fontSize) {
    return getFont3D(fontSize).fid;
  }

  public byte getFontFid(String fontFace, int fontSize) {
    return getFont3D(fontFace, fontSize).fid;
  }

  public byte getFontFid(String fontFace, String fontStyle, int fontSize) {
    return getFont3D(fontFace, fontStyle, fontSize).fid;
  }

  // 140 JavaScript color names
  // includes 16 official HTML 4.0 color names & values
  // plus a few extra rasmol names

  public final static String[] colorNames = {
    "aliceblue",            // F0F8FF
    "antiquewhite",         // FAEBD7
    "aqua",                 // 00FFFF
    "aquamarine",           // 7FFFD4
    "azure",                // F0FFFF
    "beige",                // F5F5DC
    "bisque",               // FFE4C4
    "black",                // 000000
    "blanchedalmond",       // FFEBCD
    "blue",                 // 0000FF
    "blueviolet",           // 8A2BE2
    "brown",                // A52A2A
    "burlywood",            // DEB887
    "cadetblue",            // 5F9EA0
    "chartreuse",           // 7FFF00
    "chocolate",            // D2691E
    "coral",                // FF7F50
    "cornflowerblue",       // 6495ED
    "cornsilk",             // FFF8DC
    "crimson",              // DC143C
    "cyan",                 // 00FFFF
    "darkblue",             // 00008B
    "darkcyan",             // 008B8B
    "darkgoldenrod",        // B8860B
    "darkgray",             // A9A9A9
    "darkgreen",            // 006400
    "darkkhaki",            // BDB76B
    "darkmagenta",          // 8B008B
    "darkolivegreen",       // 556B2F
    "darkorange",           // FF8C00
    "darkorchid",           // 9932CC
    "darkred",              // 8B0000
    "darksalmon",           // E9967A
    "darkseagreen",         // 8FBC8F
    "darkslateblue",        // 483D8B
    "darkslategray",        // 2F4F4F
    "darkturquoise",        // 00CED1
    "darkviolet",           // 9400D3
    "deeppink",             // FF1493
    "deepskyblue",          // 00BFFF
    "dimgray",              // 696969
    "dodgerblue",           // 1E90FF
    "firebrick",            // B22222
    "floralwhite",          // FFFAF0 16775920
    "forestgreen",          // 228B22
    "fuchsia",              // FF00FF
    "gainsboro",            // DCDCDC
    "ghostwhite",           // F8F8FF
    "gold",                 // FFD700
    "goldenrod",            // DAA520
    "gray",                 // 808080
    "green",                // 008000
    "greenyellow",          // ADFF2F
    "honeydew",             // F0FFF0
    "hotpink",              // FF69B4
    "indianred",            // CD5C5C
    "indigo",               // 4B0082
    "ivory",                // FFFFF0
    "khaki",                // F0E68C
    "lavender",             // E6E6FA
    "lavenderblush",        // FFF0F5
    "lawngreen",            // 7CFC00
    "lemonchiffon",         // FFFACD
    "lightblue",            // ADD8E6
    "lightcoral",           // F08080
    "lightcyan",            // E0FFFF
    "lightgoldenrodyellow", // FAFAD2
    "lightgreen",           // 90EE90
    "lightgrey",            // D3D3D3
    "lightpink",            // FFB6C1
    "lightsalmon",          // FFA07A
    "lightseagreen",        // 20B2AA
    "lightskyblue",         // 87CEFA
    "lightslategray",       // 778899
    "lightsteelblue",       // B0C4DE
    "lightyellow",          // FFFFE0
    "lime",                 // 00FF00
    "limegreen",            // 32CD32
    "linen",                // FAF0E6
    "magenta",              // FF00FF
    "maroon",               // 800000
    "mediumaquamarine",     // 66CDAA
    "mediumblue",           // 0000CD
    "mediumorchid",         // BA55D3
    "mediumpurple",         // 9370DB
    "mediumseagreen",       // 3CB371
    "mediumslateblue",      // 7B68EE
    "mediumspringgreen",    // 00FA9A
    "mediumturquoise",      // 48D1CC
    "mediumvioletred",      // C71585
    "midnightblue",         // 191970
    "mintcream",            // F5FFFA
    "mistyrose",            // FFE4E1
    "moccasin",             // FFE4B5
    "navajowhite",          // FFDEAD
    "navy",                 // 000080
    "oldlace",              // FDF5E6
    "olive",                // 808000
    "olivedrab",            // 6B8E23
    "orange",               // FFA500
    "orangered",            // FF4500
    "orchid",               // DA70D6
    "palegoldenrod",        // EEE8AA
    "palegreen",            // 98FB98
    "paleturquoise",        // AFEEEE
    "palevioletred",        // DB7093
    "papayawhip",           // FFEFD5
    "peachpuff",            // FFDAB9
    "peru",                 // CD853F
    "pink",                 // FFC0CB
    "plum",                 // DDA0DD
    "powderblue",           // B0E0E6
    "purple",               // 800080
    "red",                  // FF0000
    "rosybrown",            // BC8F8F
    "royalblue",            // 4169E1
    "saddlebrown",          // 8B4513
    "salmon",               // FA8072
    "sandybrown",           // F4A460
    "seagreen",             // 2E8B57
    "seashell",             // FFF5EE
    "sienna",               // A0522D
    "silver",               // C0C0C0
    "skyblue",              // 87CEEB
    "slateblue",            // 6A5ACD
    "slategray",            // 708090
    "snow",                 // FFFAFA 16775930
    "springgreen",          // 00FF7F
    "steelblue",            // 4682B4
    "tan",                  // D2B48C
    "teal",                 // 008080
    "thistle",              // D8BFD8
    "tomato",               // FF6347
    "turquoise",            // 40E0D0
    "violet",               // EE82EE
    "wheat",                // F5DEB3
    "white",                // FFFFFF 16777215
    "whitesmoke",           // F5F5F5
    "yellow",               // FFFF00
    "yellowgreen",          // 9ACD32
    // plus a few rasmol names/values
    "bluetint",             // AFD7FF
    "greenblue",            // 2E8B57
    "greentint",            // 98FFB3
    "grey",                 // 808080
    "pinktint",             // FFABBB
    "redorange",            // FF4500
    "yellowtint",           // F6F675
    "pecyan",               // 00ffff
    "pepurple",             // d020ff
    "pegreen",              // 00ff00
    "peblue",               // 6060ff
    "peviolet",             // ff80c0
    "pebrown",              // a42028
    "pepink",               // ffd8d8
    "peyellow",             // ffff00
    "pedarkgreen",          // 00c000
    "peorange",             // ffb000
    "pelightblue",          // b0b0ff
    "pedarkcyan",           // 00a0a0
    "pedarkgray",           // 606060
    "pewhite",              // ffffff
  };

  public final static int[] colorArgbs = {
    0xFFF0F8FF, // aliceblue
    0xFFFAEBD7, // antiquewhite
    0xFF00FFFF, // aqua
    0xFF7FFFD4, // aquamarine
    0xFFF0FFFF, // azure
    0xFFF5F5DC, // beige
    0xFFFFE4C4, // bisque
    0xFF000000, // black
    0xFFFFEBCD, // blanchedalmond
    0xFF0000FF, // blue
    0xFF8A2BE2, // blueviolet
    0xFFA52A2A, // brown
    0xFFDEB887, // burlywood
    0xFF5F9EA0, // cadetblue
    0xFF7FFF00, // chartreuse
    0xFFD2691E, // chocolate
    0xFFFF7F50, // coral
    0xFF6495ED, // cornflowerblue
    0xFFFFF8DC, // cornsilk
    0xFFDC143C, // crimson
    0xFF00FFFF, // cyan
    0xFF00008B, // darkblue
    0xFF008B8B, // darkcyan
    0xFFB8860B, // darkgoldenrod
    0xFFA9A9A9, // darkgray
    0xFF006400, // darkgreen

    0xFFBDB76B, // darkkhaki
    0xFF8B008B, // darkmagenta
    0xFF556B2F, // darkolivegreen
    0xFFFF8C00, // darkorange
    0xFF9932CC, // darkorchid
    0xFF8B0000, // darkred
    0xFFE9967A, // darksalmon
    0xFF8FBC8F, // darkseagreen
    0xFF483D8B, // darkslateblue
    0xFF2F4F4F, // darkslategray
    0xFF00CED1, // darkturquoise
    0xFF9400D3, // darkviolet
    0xFFFF1493, // deeppink
    0xFF00BFFF, // deepskyblue
    0xFF696969, // dimgray
    0xFF1E90FF, // dodgerblue
    0xFFB22222, // firebrick
    0xFFFFFAF0, // floralwhite
    0xFF228B22, // forestgreen
    0xFFFF00FF, // fuchsia
    0xFFDCDCDC, // gainsboro
    0xFFF8F8FF, // ghostwhite
    0xFFFFD700, // gold
    0xFFDAA520, // goldenrod
    0xFF808080, // gray
    0xFF008000, // green
    0xFFADFF2F, // greenyellow
    0xFFF0FFF0, // honeydew
    0xFFFF69B4, // hotpink
    0xFFCD5C5C, // indianred
    0xFF4B0082, // indigo
    0xFFFFFFF0, // ivory
    0xFFF0E68C, // khaki
    0xFFE6E6FA, // lavender
    0xFFFFF0F5, // lavenderblush
    0xFF7CFC00, // lawngreen
    0xFFFFFACD, // lemonchiffon
    0xFFADD8E6, // lightblue
    0xFFF08080, // lightcoral
    0xFFE0FFFF, // lightcyan
    0xFFFAFAD2, // lightgoldenrodyellow
    0xFF90EE90, // lightgreen
    0xFFD3D3D3, // lightgrey
    0xFFFFB6C1, // lightpink
    0xFFFFA07A, // lightsalmon
    0xFF20B2AA, // lightseagreen
    0xFF87CEFA, // lightskyblue
    0xFF778899, // lightslategray
    0xFFB0C4DE, // lightsteelblue
    0xFFFFFFE0, // lightyellow
    0xFF00FF00, // lime
    0xFF32CD32, // limegreen
    0xFFFAF0E6, // linen
    0xFFFF00FF, // magenta
    0xFF800000, // maroon
    0xFF66CDAA, // mediumaquamarine
    0xFF0000CD, // mediumblue
    0xFFBA55D3, // mediumorchid
    0xFF9370DB, // mediumpurple
    0xFF3CB371, // mediumseagreen
    0xFF7B68EE, // mediumslateblue
    0xFF00FA9A, // mediumspringgreen
    0xFF48D1CC, // mediumturquoise
    0xFFC71585, // mediumvioletred
    0xFF191970, // midnightblue
    0xFFF5FFFA, // mintcream
    0xFFFFE4E1, // mistyrose
    0xFFFFE4B5, // moccasin
    0xFFFFDEAD, // navajowhite
    0xFF000080, // navy
    0xFFFDF5E6, // oldlace
    0xFF808000, // olive
    0xFF6B8E23, // olivedrab
    0xFFFFA500, // orange
    0xFFFF4500, // orangered
    0xFFDA70D6, // orchid
    0xFFEEE8AA, // palegoldenrod
    0xFF98FB98, // palegreen
    0xFFAFEEEE, // paleturquoise
    0xFFDB7093, // palevioletred
    0xFFFFEFD5, // papayawhip
    0xFFFFDAB9, // peachpuff
    0xFFCD853F, // peru
    0xFFFFC0CB, // pink
    0xFFDDA0DD, // plum
    0xFFB0E0E6, // powderblue
    0xFF800080, // purple
    0xFFFF0000, // red
    0xFFBC8F8F, // rosybrown
    0xFF4169E1, // royalblue
    0xFF8B4513, // saddlebrown
    0xFFFA8072, // salmon
    0xFFF4A460, // sandybrown
    0xFF2E8B57, // seagreen
    0xFFFFF5EE, // seashell
    0xFFA0522D, // sienna
    0xFFC0C0C0, // silver
    0xFF87CEEB, // skyblue
    0xFF6A5ACD, // slateblue
    0xFF708090, // slategray
    0xFFFFFAFA, // snow
    0xFF00FF7F, // springgreen
    0xFF4682B4, // steelblue
    0xFFD2B48C, // tan
    0xFF008080, // teal
    0xFFD8BFD8, // thistle
    0xFFFF6347, // tomato
    0xFF40E0D0, // turquoise
    0xFFEE82EE, // violet
    0xFFF5DEB3, // wheat
    0xFFFFFFFF, // white
    0xFFF5F5F5, // whitesmoke
    0xFFFFFF00, // yellow
    0xFF9ACD32, // yellowgreen
    // plus a few rasmol names/values
    0xFFAFD7FF, // bluetint
    0xFF2E8B57, // greenblue
    0xFF98FFB3, // greentint
    0xFF808080, // grey
    0xFFFFABBB, // pinktint
    0xFFFF4500, // redorange
    0xFFF6F675, // yellowtint
    // plus the PE chain colors
    0xFF00ffff, // pecyan
    0xFFd020ff, // pepurple
    0xFF00ff00, // pegreen
    0xFF6060ff, // peblue
    0xFFff80c0, // peviolet
    0xFFa42028, // pebrown
    0xFFffd8d8, // pepink
    0xFFffff00, // peyellow
    0xFF00c000, // pedarkgreen
    0xFFffb000, // peorange
    0xFFb0b0ff, // pelightblue
    0xFF00a0a0, // pedarkcyan
    0xFF606060, // pedarkgray
    0xFFffffff, // pewhite
  };

  private static final Hashtable mapJavaScriptColors = new Hashtable();
  static {
    for (int i = colorNames.length; --i >= 0; )
      mapJavaScriptColors.put(colorNames[i], new Integer(colorArgbs[i]));
  }

  public static int getArgbFromString(String strColor) {
    /*
    System.out.println("ColorManager.getArgb!FromString(" + strColor + ")");
    */
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return (0xFF000000 |
                  (red & 0xFF) << 16 |
                  (grn & 0xFF) << 8  |
                  (blu & 0xFF));
        } catch (NumberFormatException e) {
        }
      } else {
        Integer boxedArgb =
          (Integer)mapJavaScriptColors.get(strColor.toLowerCase());
        if (boxedArgb != null)
          return boxedArgb.intValue();
      }
    }
    return 0;
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();

  public void calcNormalizedNormal(Point3f pointA, Point3f pointB,
                                   Point3f pointC, Vector3f vNormNorm) {
    vAB.sub(pointB, pointA);
    vAC.sub(pointC, pointA);
    vNormNorm.cross(vAB, vAC);
    vNormNorm.normalize();
  }

  public void calcXYNormalToLine(Point3f pointA, Point3f pointB,
                                   Vector3f vNormNorm) {
    // vector in xy plane perpendicular to a line between two points RMH
    Vector3f axis = new Vector3f(pointA);
    axis.sub(pointB);
    float phi = axis.angle(new Vector3f(0, 1, 0));
    if (phi == 0) {
      vNormNorm.set(1, 0, 0);
    } else {
      vNormNorm.cross(axis, new Vector3f(0, 1, 0));
      vNormNorm.normalize();
    }
  }
  
  public void calcAveragePoint(Point3f pointA, Point3f pointB,
                                   Point3f pointC) {
   Vector3f v = new Vector3f(pointB);
   v.sub(pointA);
   v.scale(1/2f);
   pointC.set(pointA);
   pointC.add(v);
  }
  
  public short getNormix(Vector3f vector) {
    return normix3d.getNormix(vector.x, vector.y, vector.z,
                              Normix3D.NORMIX_GEODESIC_LEVEL);
  }

  public short getNormix(Vector3f vector, int geodesicLevel) {
    return normix3d.getNormix(vector.x, vector.y, vector.z, geodesicLevel);
  }

  public short getInverseNormix(Vector3f vector) {
    return normix3d.getNormix(-vector.x, -vector.y, -vector.z,
                              Normix3D.NORMIX_GEODESIC_LEVEL);
  }

  public short getInverseNormix(short normix) {
    if (normix3d.inverseNormixes != null)
      return normix3d.inverseNormixes[normix];
    normix3d.calculateInverseNormixes();
    return normix3d.inverseNormixes[normix];
  }

  public short get2SidedNormix(Vector3f vector) {
    return (short)~normix3d.getNormix(vector.x, vector.y, vector.z,
                                      Normix3D.NORMIX_GEODESIC_LEVEL);
  }

  public boolean isDirectedTowardsCamera(short normix) {
    return normix3d.isDirectedTowardsCamera(normix);
  }

  public short getClosestVisibleGeodesicVertexIndex(Vector3f vector,
                                                    int[] visibilityBitmap,
                                                    int level) {
    return normix3d.getVisibleNormix(vector.x, vector.y, vector.z,
                                     visibilityBitmap, level);
  }

  public boolean isNeighborVertex(short vertex1, short vertex2, int level) {
    return Geodesic3D.isNeighborVertex(vertex1, vertex2, level);
  }

  public Vector3f[] getGeodesicVertexVectors() {
    return Geodesic3D.getVertexVectors();
  }

  public int getGeodesicVertexCount(int level) {
    return Geodesic3D.getVertexCount(level);
  }

  public Vector3f[] getTransformedVertexVectors() {
    return normix3d.getTransformedVectors();
  }

  public Vector3f getNormixVector(short normix) {
    return normix3d.getVector(normix);
  }

  public int getGeodesicFaceCount(int level) {
    return Geodesic3D.getFaceCount(level);
  }

  public short[] getGeodesicFaceVertexes(int level) {
    return Geodesic3D.getFaceVertexes(level);
  }

  public short[] getGeodesicFaceNormixes(int level) {
    return normix3d.getFaceNormixes(level);
  }

  public final static int GEODESIC_START_VERTEX_COUNT = 12;
  public final static int GEODESIC_START_NEIGHBOR_COUNT = 5;

  public short[] getGeodesicNeighborVertexes(int level) {
    return Geodesic3D.getNeighborVertexes(level);
  }
}
