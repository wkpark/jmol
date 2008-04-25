/* $RCSfile$
 *  * $Author$
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
import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

import org.jmol.api.JmolExportInterface;
import org.jmol.api.JmolRendererInterface;
import org.jmol.shape.ShapeRenderer;
import org.jmol.util.Logger;

/**
 * Provides high-level graphics primitives for 3D visualization.
 *<p>
 * A pure software implementation of a 3D graphics engine.
 * No hardware required.
 * Depending upon what you are rendering ... some people say it
 * is <i>pretty fast</i>.
 *
 * @author Miguel, miguel@jmol.org
 * 
 * with additions by Bob Hanson hansonr@stolaf.edu
 * 
 * The above is an understatement to say the least.
 * 
 * This is a two-pass rendering system. In the first pass, all opaque
 * objects are rendered. In the second pass, all translucent objects
 * are rendered. 
 * 
 * If there are no translucent objects, then that is found in the 
 * first pass as follows: 
 * 
 * The renderers first try to set the color index of the object to be 
 * rendered using setColix(short colix), and that method returns false 
 * if we are in the wrong pass for that type of object. 
 * 
 * In addition, setColix records in the boolean haveTranslucentObjects 
 * whether a translucent object was seen in the first pass. 
 * 
 * The second pass is skipped if this flag is not set. This saves immensely 
 * on rendering time when there are no translucent objects.  
 * 
 * THUS, IT IS CRITICAL THAT ALL RENDERING OPTIONS CHECK THE COLIX USING
 * g3d.setColix(short colix) PRIOR TO RENDERING.
 * 
 * Translucency is rendered only approximately. We can't maintain a full
 * buffer of all translucent objects. Instead, we "cheat" by maintaining
 * one translucent z buffer. When a translucent pixel is to be written, its
 * z position is checked and...
 * 
 * ...if it is behind or at the z position of any pixel, it is ignored
 * ...if it is in front of a translucent pixel, it is added to the translucent buffer
 * ...if it is between an opaque and translucent pixel, the translucent pixel is
 *       turned opaque, and the new pixel is added to the translucent buffer
 * 
 * This guarantees accurate translucency when there are no more than two translucent
 * pixels between the user and an opaque pixel. It's a fudge, for sure. But it is 
 * pretty good, and certainly fine for "draft" work. 
 * 
 * Users needing more accurate translucencty are encouraged to use the POV-Ray export
 * facility for production-level work.
 * 
 * Antialiasing is accomplished as full scene antialiasing. This means that 
 * the width and height are doubled (both here and in TransformManager), the
 * scene is rendered, and then each set of four pixels is averaged (roughly)
 * as the final pixel in the width*height buffer. 
 * 
 * Antialiasing options allow for antialiasing of all objects:
 * 
 *    antialiasDisplay = true
 *    antialiasTranslucent = true
 * 
 * or just the opaque ones:
 * 
 *    antialiasDisplay = true
 *    antialiasTranslucent = false
 *    
 * or not at all:
 * 
 *    antialiasDisplay = false
 *
 * The difference will be speed and memory. Adding translucent objects
 * doubles the buffer requirement, and adding antialiasing quadruples
 * the buffer requirement. 
 * 
 * So we have:
 * 
 * Memory requirements are significant, in multiples of (width) * (height) 32-bit integers:
 *
 *                 antialias OFF       ON/opaque only   ON/all objects
 *
 *   no translucent     1p + 1z = 2      4p + 4z = 8      4p + 4z = 8
 *      objects
 *
 *   with translucent   2p + 2z = 4      5p + 5z = 10     8p + 8z = 16
 *      objects
 *
 * Note that no antialising at all is required for POV-Ray output. 
 * POV-Ray will do antialiasing on its own.
 * 
 * In principle we could save a bit in the case of antialiasing of 
 * just opaque objects and reuse the p and z buffers for the 
 * translucent buffer, but this hasn't been implemented because the 
 * savings isn't that great, and if you are going to the trouble of
 * having antialiasing, you probably what it all.
 * 
 */

final public class Graphics3D implements JmolRendererInterface {

  Platform3D platform;
  Line3D line3d;
  Circle3D circle3d;
  Sphere3D sphere3d;
  //Colix3D colix3d;
  Triangle3D triangle3d;
  Cylinder3D cylinder3d;
  Hermite3D hermite3d;
  Normix3D normix3d;
  boolean isFullSceneAntialiasingEnabled;
  private boolean antialiasThisFrame;
  private boolean antialias2; 
  private boolean antialiasEnabled;
    
  public void destroy() {
    releaseBuffers();
    platform = null;
    //System.out.println("g3d destroyed");
  }

  /**
   * is full scene / oversampling antialiasing GENERALLY in effect
   *
   * @return the answer
   */
  public boolean isDisplayAntialiased() {
    return antialiasEnabled;
  }

  /**
   * is full scene / oversampling antialiasing in effect
   *
   * @return the answer
   */
  public boolean isAntialiased() {
    return antialiasThisFrame;
  }

  boolean inGreyscaleMode;
  byte[] anaglyphChannelBytes;
  
  boolean twoPass = false;
  boolean isPass2;
  boolean addAllPixels;
  boolean haveTranslucentObjects;
  
  int windowWidth, windowHeight;
  int width, height;
  
  int displayMinX, displayMaxX, displayMinY, displayMaxY;
  int slab, depth;
  boolean zShade;
  int xLast, yLast;
  private int[] pbuf;
  private int[] pbufT;
  int[] zbuf;
  private int[] zbufT;
  int bufferSize;

  //int clipX;
  //int clipY;
  //int clipWidth;
  //int clipHeight;

  short colixCurrent;
  int[] shadesCurrent;
  int argbCurrent;
  boolean isTranslucent;
  boolean isScreened;
  int translucencyMask;
  int argbNoisyUp, argbNoisyDn;

  Font3D font3dCurrent;

  public final static byte ENDCAPS_NONE = 0;
  public final static byte ENDCAPS_OPEN = 1;
  public final static byte ENDCAPS_FLAT = 2;
  public final static byte ENDCAPS_SPHERICAL = 3;

  
  public final static byte shadeMax = Shade3D.shadeMax;
  public final static byte shadeLast = Shade3D.shadeMax - 1;
  public final static byte shadeNormal = Shade3D.shadeNormal;
  public final static byte intensitySpecularSurfaceLimit = Shade3D.intensitySpecularSurfaceLimit;

  public final static short INHERIT_ALL = 0;
  public final static short USE_PALETTE = 2;
  public final static short BLACK = 4;
  public final static short ORANGE = 5;
  public final static short PINK = 6;
  public final static short BLUE = 7;
  public final static short WHITE = 8;
  public final static short CYAN = 9;
  public final static short RED = 10;
  public final static short GREEN = 11;
  public final static short GRAY = 12;
  public final static short SILVER = 13;
  public final static short LIME = 14;
  public final static short MAROON = 15;
  public final static short NAVY = 16;
  public final static short OLIVE = 17;
  public final static short PURPLE = 18;
  public final static short TEAL = 19;
  public final static short MAGENTA = 20;
  public final static short YELLOW = 21;
  public final static short HOTPINK = 22;
  public final static short GOLD = 23;


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
    //this.colix3d = new Colix3D();
    this.triangle3d = new Triangle3D(this);
    this.cylinder3d = new Cylinder3D(this);
    this.hermite3d = new Hermite3D(this);
    this.normix3d = new Normix3D();
    //    setFontOfSize(13);
  }
  
  public void setg3dExporter(Graphics3D g3d, JmolExportInterface exporter) {
  }
  
  public JmolExportInterface getExporter() {
    return null;
  }
  
  public void setRenderer(ShapeRenderer shapeRenderer) {
  }
  
  int newWindowWidth, newWindowHeight;
  boolean newAntialiasing;

  public boolean currentlyRendering() {
    return currentlyRendering;
  }
  
  public void setWindowParameters(int width, int height, boolean antialias) {
    newWindowWidth = width;
    newWindowHeight = height;
    newAntialiasing = antialias;
    if (currentlyRendering)
      endRendering();
  }
  
  private void setWidthHeight(boolean isAntialiased) {
    width = windowWidth;
    height = windowHeight;
    if (isAntialiased) {
      width <<= 1;
      height <<= 1;
    }
/*    
    System.out.println("Graphics3D setWidthHeight width=" + width + " height=" + height 
    + " isAntialiased=" + isAntialiased
    + " window width,height: " + windowWidth + "," + windowHeight);
*/
    xLast = width - 1;
    yLast = height - 1;
    displayMinX = -(width >> 1);
    displayMaxX = width - displayMinX;
    displayMinY = -(height >> 1);
    displayMaxY = height - displayMinY;
    bufferSize = width * height;
  }
  
  public boolean checkTranslucent(boolean isAlphaTranslucent) {
    if (isAlphaTranslucent)
      haveTranslucentObjects = true;
    return (!twoPass || twoPass && (isPass2 == isAlphaTranslucent));
  }
  
  public void beginRendering(Matrix3f rotationMatrix) {
    if (currentlyRendering)
      endRendering();
    if (windowWidth != newWindowWidth || windowHeight != newWindowHeight
        || newAntialiasing != isFullSceneAntialiasingEnabled) {
      windowWidth = newWindowWidth;
      windowHeight = newWindowHeight;
      isFullSceneAntialiasingEnabled = newAntialiasing;
      releaseBuffers();
    }
    normix3d.setRotationMatrix(rotationMatrix);
    antialiasEnabled = antialiasThisFrame = newAntialiasing;
    currentlyRendering = true;
    twoPass = true; //only for testing -- set false to disallow second pass
    isPass2 = false;
    //System.out.println("Graphics3D beginRendering width=" + width + " height=" + height 
    //    + " window width,height: " + windowWidth + "," + windowHeight);
    //System.out.println("pass1 antialiasEnabled=" + antialiasEnabled);
    colixCurrent = 0;
    haveTranslucentObjects = false;
    addAllPixels = true;
    if (pbuf == null) {
      platform.allocateBuffers(windowWidth, windowHeight,
                              antialiasThisFrame);
      pbuf = platform.pBuffer;
      zbuf = platform.zBuffer;
    }
    setWidthHeight(antialiasThisFrame);
    //setRectClip(clipX, clipY, clipWidth, clipHeight);
    platform.obtainScreenBuffer();
  }

  private void releaseBuffers() {
    pbuf = null;
    zbuf = null;
    pbufT = null;
    zbufT = null;
    platform.releaseBuffers();
  }
  
  public boolean setPass2(boolean antialiasTranslucent) {
    if (!haveTranslucentObjects || !currentlyRendering)
      return false;
    isPass2 = true;
    //System.out.println("pass2");
    colixCurrent = 0;
    addAllPixels = true;
    if (pbufT == null || antialias2 != antialiasTranslucent) {
      platform.allocateTBuffers(antialiasTranslucent);
      pbufT = platform.pBufferT;
      zbufT = platform.zBufferT;
    }    
    antialias2 = antialiasTranslucent;
    if (antialiasThisFrame && !antialias2)
      downsampleFullSceneAntialiasing(true);
    //System.out.println("Graphics3D setPass2 width=" + width + " height=" + height + " antialiasTranslucent=" + antialiasTranslucent);
    platform.clearTBuffer();
    return true;
  }
  
  
  public void endRendering() {
    if (!currentlyRendering)
      return;
    if (pbuf != null) {
      if (isPass2)
        mergeOpaqueAndTranslucentBuffers();
      if (antialiasThisFrame)
        downsampleFullSceneAntialiasing(false);
    }
    platform.notifyEndOfRendering();
    //setWidthHeight(antialiasEnabled);
    currentlyRendering = false;
  }

  int anaglyphLength;
  public void snapshotAnaglyphChannelBytes() {
    if (currentlyRendering)
      throw new NullPointerException();
    anaglyphLength = windowWidth * windowHeight;
    if (anaglyphChannelBytes == null ||
  anaglyphChannelBytes.length != anaglyphLength)
      anaglyphChannelBytes = new byte[anaglyphLength];
    for (int i = anaglyphLength; --i >= 0; )
      anaglyphChannelBytes[i] = (byte)pbuf[i];
  }

  public void applyCustomAnaglyph(int[] stereoColors) {
    //best if complementary, but they do not have to be0 
    int color1 = stereoColors[0];
    int color2 = stereoColors[1] & 0x00FFFFFF;
    for (int i = anaglyphLength; --i >= 0;) {
      int a = anaglyphChannelBytes[i] & 0x000000FF;
      a = (a | ((a | (a << 8)) << 8)) & color2;
      pbuf[i] = (pbuf[i] & color1) | a;
    }
  }

  public void applyGreenAnaglyph() {
    for (int i = anaglyphLength; --i >= 0; ) {
      int green = (anaglyphChannelBytes[i] & 0x000000FF) << 8;
      pbuf[i] = (pbuf[i] & 0xFFFF0000) | green;
    }
  }

  public void applyBlueAnaglyph() {
    for (int i = anaglyphLength; --i >= 0; ) {
      int blue = anaglyphChannelBytes[i] & 0x000000FF;
      pbuf[i] = (pbuf[i] & 0xFFFF0000) | blue;
    }
  }

  public void applyCyanAnaglyph() {
    for (int i = anaglyphLength; --i >= 0; ) {
      int blue = anaglyphChannelBytes[i] & 0x000000FF;
      int cyan = (blue << 8) | blue;
      pbuf[i] = pbuf[i] & 0xFFFF0000 | cyan;
    }
  }
  
  public Image getScreenImage() {
    return platform.imagePixelBuffer;
  }

  public void releaseScreenImage() {
    platform.clearScreenBufferThreaded();
  }

  public boolean haveTranslucentObjects() {
    return haveTranslucentObjects;
  }
  
  /**
   * gets g3d width
   *
   * @return width pixel count;
   */
  public int getRenderWidth() {
    return width;
  }

  /**
   * gets g3d height
   *
   * @return height pixel count
   */
  public int getRenderHeight() {
    return height;
  }

  /**
   * gets g3d slab
   *
   * @return slab
   */
  public int getSlab() {
    return slab;
  }

  /**
   * gets g3d depth
   *
   * @return depth
   */
  public int getDepth() {
    return depth;
  }

  private int backgroundArgb;
  
  /**
   * sets background color to the specified argb value
   *
   * @param argb an argb value with alpha channel
   */
  public void setBackgroundArgb(int argb) {
    // clear alpha channel and make distinct
    backgroundArgb = argb + ((argb & 0xFF) == 0xFF ? -1 : 1); 
    platform.setBackground(backgroundArgb);
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
   * @param zShade whether to shade along z front to back
   */
  public void setSlabAndDepthValues(int slabValue, int depthValue,
                                    boolean zShade) {
    slab = slabValue < 0 ? 0 : slabValue;
    depth = depthValue < 0 ? 0 : depthValue;
    this.zShade = zShade;
  }

  int getZShift(int z) {
    return (zShade ? (z - slab) * 5 / (depth - slab): 0);
  }
  
  private void downsampleFullSceneAntialiasing(boolean downsampleZBuffer) {
    int width4 = width;
    int offset1 = 0;
    int offset4 = 0;
    //System.out.println("downsample " + downsampleZBuffer);
    for (int i = windowHeight; --i >= 0; offset4 += width4)
      for (int j = windowWidth; --j >= 0; ++offset1) {
        
        /* more precise, but of no benefit:

        int a = pbuf[offset4];
        int b = pbuf[offset4++ + width4];
        int c = pbuf[offset4];
        int d = pbuf[offset4++ + width4];
        int argb = ((((a & 0x0f0f0f) + (b & 0x0f0f0f)
           + (c & 0x0f0f0f) + (d & 0x0f0f0f)) >> 2) & 0x0f0f0f)
           + ( ((a & 0xF0F0F0) + (b & 0xF0F0F0) 
           +   (c & 0xF0F0F0) + (d & 0xF0F0F0)
                ) >> 2);
        */
        
        int argb = (pbuf[offset4] >> 2) & 0x3F3F3F3F;
        argb += (pbuf[offset4++ + width4] >> 2) & 0x3F3F3F3F;
        argb += (pbuf[offset4] >> 2) & 0x3F3F3F3F;
        argb += (pbuf[offset4++ + width4] >> 2) & 0x3F3F3F3F;
        argb += (argb & 0xC0C0C0C0) >> 6;
        pbuf[offset1] = argb & 0x00FFFFFF;
      }
    if (downsampleZBuffer) {
      //we will add the alpha mask later
      offset1 = offset4 = 0;
      for (int i = windowHeight; --i >= 0; offset4 += width4)
        for (int j = windowWidth; --j >= 0; ++offset1, ++offset4) {
          int z = Math.min(zbuf[offset4], zbuf[offset4 + width4]);
          z = Math.min(z, zbuf[++offset4]);
          z = Math.min(z, zbuf[offset4 + width4]);
          if (z != Integer.MAX_VALUE)
            z >>= 1;
          zbuf[offset1] = (pbuf[offset1] == backgroundArgb ? Integer.MAX_VALUE
              : z);
        }
      setWidthHeight(antialiasThisFrame = false);
    }
    
  }

  void mergeOpaqueAndTranslucentBuffers() {
    for (int offset = 0; offset < bufferSize; offset++)
      mergeBufferPixel(pbuf, pbufT[offset], offset);

  }
  
  static void averageBufferPixel(int[] pIn, int[] pOut, int pt, int dp) {
    int argbA = pIn[pt - dp];
    int argbB = pIn[pt + dp];
    if (argbA == 0 || argbB == 0)
      return;
    pOut[pt] = ((((argbA & 0xFF000000)>>1) + ((argbB & 0xFF000000)>>1))<< 1)
        | (((argbA & 0x00FF00FF) + (argbB & 0x00FF00FF)) >> 1) & 0x00FF00FF
        | (((argbA & 0x0000FF00) + (argbB & 0x0000FF00)) >> 1) & 0x0000FF00;
  }
  
  static void mergeBufferPixel(int[] pbuf, int argbB, int pt) {
    if (argbB == 0)
      return;
    int argbA = pbuf[pt];
    if (argbA == argbB)
      return;

    int rbA = (argbA & 0x00FF00FF);
    int gA = (argbA & 0x0000FF00);
    int rbB = (argbB & 0x00FF00FF);
    int gB = (argbB & 0x0000FF00);
    int logAlpha = (argbB >> 24) & 7;
    //just for now:
    //0 or 1=100% opacity, 2=87.5%, 3=75%, 4=50%, 5=50%, 6 = 25%, 7 = 12.5% opacity.
    //4 is reserved because it is the default-Jmol 10.2
    switch (logAlpha) {
    // 0.0 to 1.0 ==> MORE translucent   
    //                1/8  1/4 3/8 1/2 5/8 3/4 7/8
    //     t           32  64  96  128 160 192 224
    //     t >> 5       1   2   3   4   5   6   7

    case 1: // 7:1
      rbA = (((rbB << 2) + (rbB << 1) + rbB  + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 2) + + (gB << 1) + gB + gA) >> 3) & 0x0000FF00;
      break;
    case 2: // 3:1
      rbA = (((rbB << 1) + rbB + rbA) >> 2) & 0x00FF00FF;
      gA = (((gB << 1) + gB + gA) >> 2) & 0x0000FF00;
      break;
    case 3: // 5:3
      rbA = (((rbB << 2) + rbB + (rbA << 1) + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 2) + gB  + (gA << 1) + gA) >> 3) & 0x0000FF00;
      break;
    case 4: // 1:1
      rbA = ((rbA + rbB) >> 1) & 0x00FF00FF;
      gA = ((gA + gB) >> 1) & 0x0000FF00;
      break;
    case 5: // 3:5
      rbA = (((rbB << 1) + rbB + (rbA << 2) + rbA) >> 3) & 0x00FF00FF;
      gA = (((gB << 1) + gB  + (gA << 2) + gA) >> 3) & 0x0000FF00;
      break;
    case 6: // 1:3
      rbA = (((rbA << 1) + rbA + rbB) >> 2) & 0x00FF00FF;
      gA = (((gA << 1) + gA + gB) >> 2) & 0x0000FF00;
      break;
    case 7: // 1:7
      rbA = (((rbA << 2) + (rbA << 1) + rbA + rbB) >> 3) & 0x00FF00FF;
      gA = (((gA << 2) + (gA << 1) + gA + gB) >> 3) & 0x0000FF00;
      break;
    }
    pbuf[pt] = 0xFF000000 | rbA | gA;    
  }
  
  public boolean hasContent() {
    return platform.hasContent();
  }

  private int currentIntensity;
  
  private void setColixAndIntensity(short colix, int intensity) {
    if (colix == colixCurrent && currentIntensity == intensity)
      return;
    currentIntensity = -1;
    setColix(colix);
    setColorNoisy(intensity);
  }

  
  /**
   * sets current color from colix color index
   * @param colix the color index
   * @return true or false if this is the right pass
   */
  public boolean setColix(short colix) {
    if (colix == colixCurrent && currentIntensity == -1)
      return true;
    int mask = colix & TRANSLUCENT_MASK;
    if (mask == TRANSPARENT)
      return false;
    isTranslucent = mask != 0;
    isScreened = isTranslucent && mask == TRANSLUCENT_SCREENED;
    if (!checkTranslucent(isTranslucent && !isScreened))
      return false;
    addAllPixels = isPass2 || !isTranslucent;
    if (isPass2)
      translucencyMask = (mask << ALPHA_SHIFT) | 0xFFFFFF;
    colixCurrent = colix;
    shadesCurrent = getShades(colix);
    currentIntensity = -1; 
    argbCurrent = argbNoisyUp = argbNoisyDn = getColixArgb(colix);
    return true;
  }

  void setColorNoisy(int intensity) {
    //if (isPass2)
      //return;
    currentIntensity = intensity;
    argbCurrent = shadesCurrent[intensity];
    argbNoisyUp = shadesCurrent[intensity < shadeLast ? intensity + 1 : shadeLast];
    argbNoisyDn = shadesCurrent[intensity > 0 ? intensity - 1 : 0];
  }
  
  int zMargin;
  
  void setZMargin(int dz) {
    zMargin = dz;
  }
  
  void addPixel(int offset, int z, int p) {
    addPixelT(offset, z, p, zbuf, pbuf, zbufT, pbufT, translucencyMask, isPass2, zMargin);
  }
  
  final static void addPixelT(int offset, int z, int p, int[] zbuf, int[] pbuf, int[] zbufT, int[] pbufT, int translucencyMask, boolean isPass2, int zMargin) {
    if (!isPass2) {
      zbuf[offset] = z;
      pbuf[offset] = p;
      return;
    }
    int zT = zbufT[offset]; 
    if (z < zT) {
      //new in front -- merge old translucent with opaque
      //if (zT != Integer.MAX_VALUE)
      int argb = pbufT[offset];
      if (argb != 0 && zT - z > zMargin)
        mergeBufferPixel(pbuf, argb, offset);
      zbufT[offset] = z;
      pbufT[offset] = p & translucencyMask;
    } else if (z == zT) {
    } else {
      //oops-out of order
      if (z - zT > zMargin)
        mergeBufferPixel(pbuf, p & translucencyMask, offset);
    }
  }

  /**
   * draws a screened circle ... every other dot is turned on
   *
   * @param colix the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   * @param doFill fill or not
   */
  public void drawCircleCentered(short colix, int diameter, int x, int y, int z, boolean doFill) {
    // draw circle
    if (isClippedZ(z))
      return;
    int r = (diameter + 1) / 2;
    boolean isClipped = x < r || x + r >= width || y < r || y + r >= height;
      if (!isClipped)
        circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
      else if (!isClippedXY(diameter, x, y))
        circle3d.plotCircleCenteredClipped(x, y, z, diameter);
    if (!doFill)
      return;
    if (!isClipped)
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    else if (!isClippedXY(diameter, x, y))
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter);    
  }

  public void fillScreenedCircleCentered(short colixFill, int diameter, int x, int y, int z) {
    // halo only -- simple Z/window clip
    if (isClippedZ(z))
      return;
    int r = (diameter + 1) / 2;
    boolean isClipped = x < r || x + r >= width || y < r || y + r >= height;
    if (setColix(getColixTranslucent(colixFill, false, 0))) {
      if (!isClipped)
        circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
      else if (!isClippedXY(diameter, x, y))
        circle3d.plotCircleCenteredClipped(x, y, z, diameter);
    }
    if (!setColix(getColixTranslucent(colixFill, true, 0.5f)))
      return;
    if (!isClipped)
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter);
    else if (!isClippedXY(diameter, x, y))
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter);
  }

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public void fillSphereCentered(int diameter, int x, int y, int z) {
    switch(diameter) {
    case 1:
      plotPixelClipped(argbCurrent, x, y, z);
    case 0:
      return;
    }
    //System.out.println("Graphics3D fillSphereCentered diameter x y z" + diameter + " "+x+" "+y+" "+z);
    if (diameter <= (antialiasThisFrame ? Sphere3D.maxSphereDiameter2 : Sphere3D.maxSphereDiameter))
      sphere3d.render(shadesCurrent, !addAllPixels, diameter, x, y, z, null);
  }

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center javax.vecmath.Point3i defining the center
   */

  public void fillSphereCentered(int diameter, Point3i center) {
    fillSphereCentered(diameter, center.x, center.y, center.z);
  }

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center a javax.vecmath.Point3f ... floats are casted to ints
   */
  public void fillSphereCentered(int diameter, Point3f center) {
    fillSphereCentered(diameter, (int)center.x, (int)center.y, (int)center.z);
  }

  public void renderEllipsoid(int x, int y, int z, Object[] ellipsoid, int diameter) {
    switch (diameter) {
    case 1:
      plotPixelClipped(argbCurrent, x, y, z);
    case 0:
      return;
    }
    //System.out.println("Graphics3D fillSphereCentered diameter x y z" + diameter + " "+x+" "+y+" "+z);
    if (diameter <= (antialiasThisFrame ? Sphere3D.maxSphereDiameter2 : Sphere3D.maxSphereDiameter))
      sphere3d.render(shadesCurrent, !addAllPixels, diameter, x, y, z, (Matrix4f) ellipsoid[3]);
  }

  /**
   * draws a rectangle
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab z for slab check (for set labelsFront)
   * @param rWidth pixel count
   * @param rHeight pixel count
   */
  public void drawRect(int x, int y, int z, int zSlab, int rWidth, int rHeight) {
    // labels (and rubberband, not implemented) and navigation cursor
    if (zSlab != 0 && isClippedZ(zSlab))
      return;
    int w = rWidth - 1;
    int h = rHeight - 1;
    int xRight = x + w;
    int yBottom = y + h;
    if (y >= 0 && y < height)
      drawHLine(x, y, z, w);
    if (yBottom >= 0 && yBottom < height)
      drawHLine(x, yBottom, z, w);
    if (x >= 0 && x < width)
      drawVLine(x, y, z, h);
    if (xRight >= 0 && xRight < width)
      drawVLine(xRight, y, z, h);
  }

  private void drawHLine(int x, int y, int z, int w) {
    // hover, labels only
    if (w < 0) {
      x += w;
      w = -w;
    }
    if (x < 0) {
      w += x;
      x = 0;
    }
    if (x + w >= width)
      w = width - 1 - x;
    int offset = x + width * y;
    if (addAllPixels) {
      for (int i = 0; i <= w; i++) {
        if (z < zbuf[offset])
          addPixel(offset, z, argbCurrent);
        offset++;
      }
      return;
    }
    boolean flipflop = ((x ^ y) & 1) != 0;
    for (int i = 0; i <= w; i++) {
      if ((flipflop = !flipflop) && z < zbuf[offset])
        addPixel(offset, z, argbCurrent);
      offset++;
    }
  }

  private void drawVLine(int x, int y, int z, int h) {
    // hover, labels only
    if (h < 0) {
      y += h;
      h = -h;
    }
    if (y < 0) {
      h += y;
      y = 0;
    }
    if (y + h >= height) {
      h = height - 1 - y;
    }
    int offset = x + width * y;
    if (addAllPixels) {
      for (int i = 0; i <= h; i++) {
        if (z < zbuf[offset])
          addPixel(offset, z, argbCurrent);
        offset += width;
      }
      return;
    }
    boolean flipflop = ((x ^ y) & 1) != 0;
    for (int i = 0; i <= h; i++) {
      if ((flipflop = !flipflop) && z < zbuf[offset])
        addPixel(offset, z, argbCurrent);
      offset += width;
    }
  }


  /**
   * fills background rectangle for label
   *<p>
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab  z value for slabbing
   * @param widthFill pixel count
   * @param heightFill pixel count
   */
  public void fillRect(int x, int y, int z, int zSlab, int widthFill, int heightFill) {
    // hover and labels only -- slab at atom or front -- simple Z/window clip
    if (isClippedZ(zSlab))
      return;
    if (x < 0) {
      widthFill += x;
      if (widthFill <= 0)
        return;
      x = 0;
    }
    if (x + widthFill > width) {
      widthFill = width - x;
      if (widthFill <= 0)
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
  
  /**
   * draws the specified string in the current font.
   * no line wrapping -- axis, labels, measures
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   * @param zSlab z for slab calculation
   */
  
  public void drawString(String str, Font3D font3d,
                         int xBaseline, int yBaseline, int z, int zSlab) {
    //axis, labels, measures    
    if (str == null)
      return;
    if (isClippedZ(zSlab))
      return;
    //System.out.println("drawString " + str + " "+ xBaseline + " " + yBaseline);
    drawStringNoSlab(str, font3d, xBaseline, yBaseline, z); 
  }

  /**
   * draws the specified string in the current font.
   * no line wrapping -- echo, frank, hover, molecularOrbital, uccage
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */
  
  public void drawStringNoSlab(String str, Font3D font3d, 
                               int xBaseline, int yBaseline,
                               int z) {
    // echo, frank, hover, molecularOrbital, uccage
    if (str == null)
      return;
    if(font3d != null)
      font3dCurrent = font3d;
    plotText(xBaseline, yBaseline, z, argbCurrent, str, font3dCurrent, null);
  }
  
  public void plotText(int x, int y, int z, int argb,
                String text, Font3D font3d, JmolRendererInterface jmolRenderer) {
    Text3D.plot(x, y, z, argb, text, font3d, this, jmolRenderer, 
        antialiasThisFrame);    
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

  boolean currentlyRendering;

  /*
  private void setRectClip(int x, int y, int width, int height) {
    // not implemented
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
  */

  //mostly public drawing methods -- add "public" if you need to

  /* ***************************************************************
   * points
   * ***************************************************************/

  
  public void drawPixel(int x, int y, int z) {
    // measures - render angle
    plotPixelClipped(x, y, z);
  }

  public void drawPoints(int count, int[] coordinates) {
    // for dots only
    plotPoints(count, coordinates);
  }

  /* ***************************************************************
   * lines and cylinders
   * ***************************************************************/

  public void drawDashedLine(int run, int rise, Point3i pointA, Point3i pointB) {
    // measures only
    line3d.plotDashedLine(argbCurrent, !addAllPixels, run, rise, 
        pointA.x, pointA.y, pointA.z,
        pointB.x, pointB.y, pointB.z, false);
  }

  public void drawDottedLine(Point3i pointA, Point3i pointB) {
     //axes, bbcage only
    line3d.plotDashedLine(argbCurrent, !addAllPixels, 2, 1,
                          pointA.x, pointA.y, pointA.z,
                          pointB.x, pointB.y, pointB.z, false);
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    // stars
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
                    x1, y1, z1, x2, y2, z2, false);
  }

  public void drawLine(short colixA, short colixB,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    // backbone and sticks
    if (!setColix(colixA))
      colixA = 0;
    boolean isScreenedA = !addAllPixels;
    int argbA = argbCurrent;
    if (!setColix(colixB))
      colixB = 0;
    if (colixA == 0 && colixB == 0)
      return;
    line3d.plotLine(argbA, isScreenedA, argbCurrent, !addAllPixels,
                    x1, y1, z1, x2, y2, z2, false);
  }
  
  public void drawLine(Point3i pointA, Point3i pointB) {
    // draw quadrilateral and hermite
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z, false);
  }
  
  public void fillCylinder(short colixA, short colixB, byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    //Backbone, Mps, Sticks
    if (!setColix(colixA))
      colixA = 0;
    boolean isScreenedA = !addAllPixels;
    if (!setColix(colixB))
      colixB = 0;
    if (colixA == 0 && colixB == 0)
      return;
    cylinder3d.render(colixA, colixB, isScreenedA, !addAllPixels, endcaps, diameter,
                      xA, yA, zA, xB, yB, zB);
  }

  public void fillCylinder(byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    //measures, vectors
    cylinder3d.render(colixCurrent, colixCurrent, !addAllPixels, !addAllPixels, endcaps, diameter,
                      xA, yA, zA, xB, yB, zB);
  }

  public void fillCylinder(byte endcaps, int diameter,
                           Point3i screenA, Point3i screenB) {
    //axes, bbcage, uccage, cartoon, dipoles, mesh
    cylinder3d.render(colixCurrent, colixCurrent, !addAllPixels, !addAllPixels, endcaps, diameter,
                      screenA.x, screenA.y, screenA.z,
                      screenB.x, screenB.y, screenB.z);
  }

  public void fillCylinderBits(byte endcaps, int diameter,
                               Point3f screenA, Point3f screenB) {
   // dipole cross, cartoonRockets
   cylinder3d.renderBits(colixCurrent, colixCurrent, !addAllPixels, !addAllPixels, endcaps, diameter,
       screenA.x, screenA.y, screenA.z,
       screenB.x, screenB.y, screenB.z);
 }

  public void fillCone(byte endcap, int diameter,
                       Point3i screenBase, Point3i screenTip) {
    // dipoles, mesh, vectors
    cylinder3d.renderCone(colixCurrent, !addAllPixels, endcap, diameter,
                          screenBase.x, screenBase.y, screenBase.z,
                          screenTip.x, screenTip.y, screenTip.z, false);
  }

  public void fillCone(byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    // cartoons, rockets
    cylinder3d.renderCone(colixCurrent, !addAllPixels, endcap, diameter,
                          screenBase.x, screenBase.y, screenBase.z,
                          screenTip.x, screenTip.y, screenTip.z, true);
  }

  public void drawHermite(int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(false, tension, 0, 0, 0, s0, s1, s2, s3);
  }

  public void drawHermite(boolean fill, boolean border,
                          int tension, Point3i s0, Point3i s1, Point3i s2,
                          Point3i s3, Point3i s4, Point3i s5, Point3i s6,
                          Point3i s7, int aspectRatio) {
    hermite3d.render2(fill, border, tension, s0, s1, s2, s3, s4, s5, s6,
        s7, aspectRatio);
  }

  public void fillHermite(int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(true, tension,
                     diameterBeg, diameterMid, diameterEnd,
                     s0, s1, s2, s3);
  }
  
  public static void getHermiteList(int tension, Tuple3f s0, Tuple3f s1, Tuple3f s2, Tuple3f s3, Tuple3f s4, Tuple3f[] list, int index0, int n) {
    Hermite3D.getHermiteList(tension, s0, s1, s2, s3, s4, list, index0, n);
  }

  /* ***************************************************************
   * triangles
   * ***************************************************************/

  public void drawTriangle(Point3i screenA, short colixA, Point3i screenB,
                           short colixB, Point3i screenC, short colixC, int check) {
    // primary method for mapped Mesh
    int xA = screenA.x;
    int yA = screenA.y;
    int zA = screenA.z;
    int xB = screenB.x;
    int yB = screenB.y;
    int zB = screenB.z;
    int xC = screenC.x;
    int yC = screenC.y;
    int zC = screenC.z;
    if ((check & 1) == 1)
      drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    if ((check & 2) == 2)
      drawLine(colixB, colixC, xB, yB, zB, xC, yC, zC);
    if ((check & 4) == 4)
      drawLine(colixA, colixC, xA, yA, zA, xC, yC, zC);
  }

  public void drawTriangle(Point3i screenA, Point3i screenB,
                           Point3i screenC, int check) {
    // primary method for unmapped monochromatic Mesh
    int xA = screenA.x;
    int yA = screenA.y;
    int zA = screenA.z;
    int xB = screenB.x;
    int yB = screenB.y;
    int zB = screenB.z;
    int xC = screenC.x;
    int yC = screenC.y;
    int zC = screenC.z;
    if ((check & 1) == 1)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xA, yA, zA, xB, yB, zB, false);
    if ((check & 2) == 2)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xB, yB, zB, xC, yC, zC, false);
    if ((check & 4) == 4)
      line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels,
          xA, yA, zA, xC, yC, zC, false);
  }

  public void drawCylinderTriangle(int xA, int yA, int zA, int xB,
                                   int yB, int zB, int xC, int yC, int zC,
                                   int diameter) {
    // polyhedra
    fillCylinder(ENDCAPS_SPHERICAL, diameter, xA, yA,
        zA, xB, yB, zB);
    fillCylinder(ENDCAPS_SPHERICAL, diameter, xA, yA,
        zA, xC, yC, zC);
    fillCylinder(ENDCAPS_SPHERICAL, diameter, xB, yB,
        zB, xC, yC, zC);
  }

  public void drawfillTriangle(int xA, int yA, int zA, int xB,
                               int yB, int zB, int xC, int yC, int zC) {
    // sticks -- sterochemical wedge notation -- not implemented?
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xA,
        yA, zA, xB, yB, zB, false);
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xA,
        yA, zA, xC, yC, zC, false);
    line3d.plotLine(argbCurrent, !addAllPixels, argbCurrent, !addAllPixels, xB,
        yB, zB, xC, yC, zC, false);
    triangle3d.fillTriangle(xA, yA, zA, xB, yB, zB, xC, yC, zC, false);
  }
  
  public void fillTriangle(Point3i screenA, int intensityA,
                           Point3i screenB, int intensityB,
                           Point3i screenC, int intensityC) {
    triangle3d.setGouraud(intensityA, intensityB, intensityC);
    triangle3d.fillTriangle(screenA, screenB, screenC, true);
  }
  
  public void fillTriangle(Point3i screenA, short colixA, short normixA,
                           Point3i screenB, short colixB, short normixB,
                           Point3i screenC, short colixC, short normixC) {
    // mesh, isosurface
    boolean useGouraud;
    if (normixA == normixB && normixA == normixC &&
        colixA == colixB && colixA == colixC) {
      setColixAndIntensity(colixA, normix3d.getIntensity(normixA));
      useGouraud = false;
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
      useGouraud = true;
    }
    triangle3d.fillTriangle(screenA, screenB, screenC, useGouraud);
  }

  public void fillTriangle(short normix,
                           int xScreenA, int yScreenA, int zScreenA,
                           int xScreenB, int yScreenB, int zScreenB,
                           int xScreenC, int yScreenC, int zScreenC) {
    // polyhedra
    setColorNoisy(normix3d.getIntensity(normix));
    triangle3d.fillTriangle( xScreenA, yScreenA, zScreenA,
        xScreenB, yScreenB, zScreenB,
        xScreenC, yScreenC, zScreenC, false);
  }

  public void fillTriangle(Point3f screenA, Point3f screenB, Point3f screenC) {
    // rockets
    setColorNoisy(calcIntensityScreen(screenA, screenB, screenC));
    triangle3d.fillTriangle(screenA, screenB, screenC, false);
  }

  public void fillTriangle(Point3i screenA, Point3i screenB, Point3i screenC) {
    // cartoon, hermite
    triangle3d.fillTriangle(screenA, screenB, screenC, false);
  }

  public void fillTriangle(Point3i screenA, short colixA,
                                   short normixA, Point3i screenB,
                                   short colixB, short normixB,
                                   Point3i screenC, short colixC,
                                   short normixC, float factor) {
    // isosurface test showing triangles
    boolean useGouraud;
    if (normixA == normixB && normixA == normixC && colixA == colixB
        && colixA == colixC) {
      setColixAndIntensity(colixA, normix3d.getIntensity(normixA));
      useGouraud = false;
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
      useGouraud = true;
    }
    triangle3d.fillTriangle(screenA, screenB, screenC, factor,
        useGouraud);
  }

  /* ***************************************************************
   * quadrilaterals
   * ***************************************************************/
  
  public void drawQuadrilateral(short colix, Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    //mesh only -- translucency has been checked
    setColix(colix);
    drawLine(screenA, screenB);
    drawLine(screenB, screenC);
    drawLine(screenC, screenD);
    drawLine(screenD, screenA);
  }

  public void fillQuadrilateral(Point3f screenA, Point3f screenB,
                                Point3f screenC, Point3f screenD) {
    // hermite, rockets, cartoons
    setColorNoisy(calcIntensityScreen(screenA, screenB, screenC));
    triangle3d.fillTriangle(screenA, screenB, screenC, false);
    triangle3d.fillTriangle(screenA, screenC, screenD, false);
  }

  public void fillQuadrilateral(Point3i screenA, short colixA, short normixA,
                                Point3i screenB, short colixB, short normixB,
                                Point3i screenC, short colixC, short normixC,
                                Point3i screenD, short colixD, short normixD) {
    // mesh
    fillTriangle(screenA, colixA, normixA,
                 screenB, colixB, normixB,
                 screenC, colixC, normixC);
    fillTriangle(screenA, colixA, normixA,
                 screenC, colixC, normixC,
                 screenD, colixD, normixD);
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, Vector3f[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int faceVertexMax) {
    // generator only
  }
  
  /* ***************************************************************
   * lower-level plotting routines
   * ***************************************************************/

  public boolean isClipped(int x, int y, int z) {
    // this is the one that could be augmented with slabPlane
    return (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth);
  }
  
  private boolean isClipped(int x, int y) {
    return (x < 0 || x >= width || y < 0 || y >= height);
  }

  public boolean isInDisplayRange(int x, int y) {
    return (x >= displayMinX && x < displayMaxX && y >= displayMinY && y < displayMaxY);
  }
  
  private boolean isClippedXY(int diameter, int x, int y) {
    int r = (diameter + 1) >> 1;
    return (x < -r || x >= width + r || y < -r || y >= height + r);
  }
    
  public boolean isClippedZ(int z) {
    return (z != Integer.MIN_VALUE  && (z < slab || z > depth));
  }
  
  void plotPixelClipped(int x, int y, int z) {
    //circle3D, drawPixel, plotPixelClipped(point3)
    if (isClipped(x, y, z))
      return;
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argbCurrent);
  }

  public void plotPixelClipped(Point3i screen) {
    // hermite only
    plotPixelClipped(screen.x, screen.y, screen.z);
  }

  void plotPixelClipped(int argb, int x, int y, int z) {
    // cylinder3d plotRaster
    if (isClipped(x, y, z))
      return;
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argb);
  }

  public void plotPixelClippedNoSlab(int argb, int x, int y, int z) {
    // drawString via text3d.plotClipped
    if (isClipped(x, y))
      return;
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argb);
  }

  void plotPixelClipped(int argb, boolean isScreened, int x, int y, int z) {
    if (isClipped(x, y, z))
      return;
    if (isScreened && ((x ^ y) & 1) != 0)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argb);
  }

  void plotPixelUnclipped(int x, int y, int z) {
    // circle (halo)
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argbCurrent);
  }
  
  void plotPixelUnclipped(int argb, int x, int y, int z) {
    // cylinder plotRaster
    int offset = y * width + x;
    if (z < zbuf[offset])
      addPixel(offset, z, argb);
  }
  
  void plotPixelsClipped(int count, int x, int y, int z) {
    // for circle only; i.e. halo 
    // simple Z/window clip
    if (y < 0 || y >= height || x >= width)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    if (count <= 0)
      return;
    int offsetPbuf = y * width + x;
    int offsetMax = offsetPbuf + count;
    int step = 1;
    if (!addAllPixels) {
      step = 2;
      if (((x ^ y) & 1) != 0)
        ++offsetPbuf;
    }
    while (offsetPbuf < offsetMax) {
      if (z < zbuf[offsetPbuf])
        addPixel(offsetPbuf, z, argbCurrent);
      offsetPbuf += step;
    }
  }

  void plotPixelsClipped(int count, int x, int y, int zAtLeft, int zPastRight,
                         Rgb16 rgb16Left, Rgb16 rgb16Right) {
    // cylinder3d.renderFlatEndcap, triangle3d.fillRaster
    if (count <= 0 || y < 0 || y >= height || x >= width
        || (zAtLeft < slab && zPastRight < slab)
        || (zAtLeft > depth && zPastRight > depth))
      return;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled = ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))
        / count;
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
        if (addAllPixels || (flipflop = !flipflop) == true) {
          int z = zScaled >> 10;
          if (z >= slab && z <= depth && z < zbuf[offsetPbuf]) {
            seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
            int bits = (seed >> 16) & 0x07;
            addPixel(offsetPbuf, z, bits == 0 ? argbNoisyDn
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
        if (addAllPixels || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z >= slab && z <= depth && z < zbuf[offsetPbuf])
            addPixel(offsetPbuf, z, 0xFF000000 | (rScaled & 0xFF0000)
                | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  /*
   final static boolean ENABLE_GOURAUD_STATS = false;
   static int totalGouraud;
   static int shortCircuitGouraud;

   void plotPixelsUnclipped(int count, int x, int y, int zAtLeft,
   int zPastRight, Rgb16 rgb16Left, Rgb16 rgb16Right) {
   // for Triangle3D.fillRaster
   if (count <= 0)
   return;
   int seed = (x << 16) + (y << 1) ^ 0x33333333;
   // scale the z coordinates;
   int zScaled = (zAtLeft << 10) + (1 << 9);
   int dz = zPastRight - zAtLeft;
   int roundFactor = count / 2;
   int zIncrementScaled = ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))
   / count;
   int offsetPbuf = y * width + x;
   if (rgb16Left == null) {
   if (!isTranslucent) {
   while (--count >= 0) {
   int z = zScaled >> 10;
   if (z < zbuf[offsetPbuf]) {
   zbuf[offsetPbuf] = z;
   seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
   int bits = (seed >> 16) & 0x07;
   pbuf[offsetPbuf] = (bits == 0 ? argbNoisyDn
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
   zbuf[offsetPbuf] = z;
   seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
   int bits = (seed >> 16) & 0x07;
   pbuf[offsetPbuf] = (bits == 0 ? argbNoisyDn
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

   if (!isTranslucent) {
   for (; zbuf[j] < zMin; ++j)
   if (--i == 0) {
   if ((++shortCircuitGouraud % 100000) == 0)
   Logger.debug("totalGouraud=" + totalGouraud
   + " shortCircuitGouraud=" + shortCircuitGouraud + " %="
   + (100.0 * shortCircuitGouraud / totalGouraud));
   return;
   }
   } else {
   if (flipflop) {
   ++j;
   if (--i == 0)
   return;
   }
   for (; zbuf[j] < zMin; j += 2) {
   i -= 2;
   if (i <= 0) {
   if ((++shortCircuitGouraud % 100000) == 0)
   Logger.debug("totalGouraud=" + totalGouraud
   + " shortCircuitGouraud=" + shortCircuitGouraud + " %="
   + (100.0 * shortCircuitGouraud / totalGouraud));
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
   if (!isTranslucent || (flipflop = !flipflop)) {
   int z = zScaled >> 10;
   if (z < zbuf[offsetPbuf]) {
   zbuf[offsetPbuf] = z;
   pbuf[offsetPbuf] = (0xFF000000 | (rScaled & 0xFF0000)
   | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
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
   */
  ///////////////////////////////////
  void plotPixelsUnclipped(int count, int x, int y, int zAtLeft,
                           int zPastRight, Rgb16 rgb16Left, Rgb16 rgb16Right) {
    // for isosurface Triangle3D.fillRaster
    if (count <= 0)
      return;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    boolean flipflop = ((x ^ y) & 1) != 0;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled = ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))
        / count;
    int offsetPbuf = y * width + x;
    if (rgb16Left == null) {
      while (--count >= 0) {
        if (addAllPixels || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z < zbuf[offsetPbuf]) {
            seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
            int bits = (seed >> 16) & 0x07;
            addPixel(offsetPbuf, z, bits == 0 ? argbNoisyDn
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
        if (addAllPixels || (flipflop = !flipflop)) {
          int z = zScaled >> 10;
          if (z < zbuf[offsetPbuf])
            addPixel(offsetPbuf, z, 0xFF000000 | (rScaled & 0xFF0000)
                | (gScaled & 0xFF00) | ((bScaled >> 8) & 0xFF));
        }
        ++offsetPbuf;
        zScaled += zIncrementScaled;
        rScaled += rIncrement;
        gScaled += gIncrement;
        bScaled += bIncrement;
      }
    }
  }

  ///////////////////////////////
  void plotPixelsUnclipped(int count, int x, int y, int z) {
    
    // for Cirle3D.plot8Filled and fillRect
    
    int offsetPbuf = y * width + x;
    if (addAllPixels) {
      while (--count >= 0) {
        if (z < zbuf[offsetPbuf])
          addPixel(offsetPbuf, z, argbCurrent);
        ++offsetPbuf;
      }
    } else {
      int offsetMax = offsetPbuf + count;
      if (((x ^ y) & 1) != 0)
        if (++offsetPbuf == offsetMax)
          return;
      do {
        if (z < zbuf[offsetPbuf])
          addPixel(offsetPbuf, z, argbCurrent);
        offsetPbuf += 2;
      } while (offsetPbuf < offsetMax);
    }
  }

  private void plotPoints(int count, int[] coordinates) {
    for (int i = count * 3; i > 0; ) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (isClipped(x, y, z))
        continue;
      int offset = y * width + x++;
      if (z < zbuf[offset])
        addPixel(offset, z, argbCurrent);
      if (antialiasThisFrame) {
        offset = y * width + x;
        if (!isClipped(x, y, z) && z < zbuf[offset])
          addPixel(offset, z, argbCurrent);
        offset = (++y)* width + x;
        if (!isClipped(x, y, z) && z < zbuf[offset])
          addPixel(offset, z, argbCurrent);
        offset = y * width + (--x);
        if (!isClipped(x, y, z) && z < zbuf[offset])
          addPixel(offset, z, argbCurrent);
      }

    }
  }

  
  /* ***************************************************************
   * color indexes -- colix
   * ***************************************************************/

  /* entries 0 and 1 are reserved and are special inheritance
     0 INHERIT_ALL inherits both color and translucency
     1 INHERIT_COLOR is used to inherit just the color
     
     
     0x8000 changeable flag (elements and isotopes, about 200; negative)
     0x7800 translucent flag set

     NEW:
     0x0000 translucent level 0  (opaque)
     0x0800 translucent level 1
     0x1000 translucent level 2
     0x1800 translucent level 3
     0x2000 translucent level 4
     0x2800 translucent level 5
     0x3000 translucent level 6
     0x3800 translucent level 7
     0x4000 translucent level 8 (invisible)

     0x0000 inherit color and translucency
     0x0001 inherit color; translucency determined by mask     
     0x0002 special palette ("group", "structure", etc.); translucency by mask

     Note that inherited colors and special palettes are not handled here. 
     They could be anything, including totally variable quantities such as 
     distance to an object. So there are two stages of argb color determination
     from a colix. The special palette flag is only used transiently - just to
     indicate that the color selected isn't a known color. The actual palette-based
     colix is saved here, and and the atom or shape's byte paletteID is set as well.
     
     Shapes/ColorManager: responsible for assigning argb colors based on 
     color palettes. These argb colors are then used directly.
     
     Graphics3D: responsible for "system" colors and caching of user-defined rgbs.
     
     
     
     0x0004 black...
       ....
     0x0017  ...gold
     0x00?? additional colors used from JavaScript list or specified by user

     Bob Hanson 3/2007
     
  */
  private final static short CHANGEABLE_MASK       = (short)0x8000; // negative
  private final static short UNMASK_CHANGEABLE_TRANSLUCENT =0x07FF;
  private final static int   TRANSLUCENT_SHIFT        = 11;
  private final static int   ALPHA_SHIFT              = 24 - TRANSLUCENT_SHIFT;
  private final static int   TRANSLUCENT_MASK         = 0xF << TRANSLUCENT_SHIFT; //0x7800
  private final static int   TRANSLUCENT_SCREENED     = TRANSLUCENT_MASK;
  private final static int   TRANSPARENT              =  8 << TRANSLUCENT_SHIFT;  //0x4000
  final static int           TRANSLUCENT_50           =  4 << TRANSLUCENT_SHIFT;  //0x2000
  public final static short  OPAQUE_MASK              = ~TRANSLUCENT_MASK;


  private final static short INHERIT_COLOR       = 1;
  final static short         UNUSED_OPTION3      = 3;
  final static short         SPECIAL_COLIX_MAX   = 4;

  /**
   * Return a greyscale rgb value 0-FF using NTSC color luminance algorithm
   *<p>
   * the alpha component is set to 0xFF. If you want a value in the
   * range 0-255 then & the result with 0xFF;
   *
   * @param rgb the rgb value
   * @return a grayscale value in the range 0 - 255 decimal
   */
  public static int calcGreyscaleRgbFromRgb(int rgb) {
    int grey = ((2989 * ((rgb >> 16) & 0xFF)) +
                (5870 * ((rgb >> 8) & 0xFF)) +
                (1140 * (rgb & 0xFF)) + 5000) / 10000;
    int greyRgb = (grey << 16) | (grey << 8) | grey | 0xFF000000;
    return greyRgb;
  }

  public static short getColix(int argb) {
    return Colix3D.getColix(argb); 
  }

  public final static Point3f colorPointFromInt(int color, Point3f pt) {
    pt.z = color & 0xFF;
    pt.y = (color >> 8) & 0xFF;
    pt.x = (color >> 16) & 0xFF;
    return pt;
  }

  public final static Point3f colorPointFromString(String colorName, Point3f pt) {
    return colorPointFromInt(getArgbFromString(colorName), pt);
  }

  public static short getColix(String colorName) {
    int argb = getArgbFromString(colorName);
    if (argb != 0)
      return Colix3D.getColix(argb);
    if ("none".equalsIgnoreCase(colorName))
      return INHERIT_ALL;
    if ("opaque".equalsIgnoreCase(colorName))
      return INHERIT_COLOR;
    return USE_PALETTE;
  }

  
  private final static short applyColorTranslucencyLevel(short colix,
                                                         float translucentLevel) {
    // 0.0 to 1.0 ==> MORE translucent   
    //                 1/8  1/4 3/8 1/2 5/8 3/4 7/8 8/8
    //     t            32  64  96  128 160 192 224 255 or 256
    //     t >> 5        1   2   3   4   5   6   7   8
    //     (t >> 5) + 1  2   3   4   5   6   7   8   9 
    // 15 is reserved for screened, so 9-14 just map to 9, "invisible"

    if (translucentLevel == 0) //opaque
      return (short) (colix & ~TRANSLUCENT_MASK);
    if (translucentLevel < 0) //screened
      return (short) (colix | TRANSLUCENT_MASK);
    if (translucentLevel >= 255 || translucentLevel == 1.0)
      return (short) (colix | TRANSPARENT);
    int iLevel = (int) (translucentLevel < 1 ? translucentLevel * 256
            : translucentLevel <= 9 ? ((int) (translucentLevel-1)) << 5
               : translucentLevel < 15 ? 8 << 5 : translucentLevel);
    iLevel = (iLevel >> 5) % 16;
    return (short) (colix & ~TRANSLUCENT_MASK | (iLevel << TRANSLUCENT_SHIFT));
  }

  public final static int getColixTranslucencyLevel(short colix) {
    int logAlpha = (colix >> TRANSLUCENT_SHIFT) & 15;
    switch (logAlpha) {
    case 1:
    case 2:
    case 3:
    case 4:
    case 5:
    case 6:
    case 7:
      return logAlpha << 5;
    case 15:
      return -1;
    default:
      return 255;
    }
  }
  
  public static short getColix(Object obj) {
    if (obj == null)
      return INHERIT_ALL;
    if (obj instanceof Byte)
      return (((Byte) obj).byteValue() == 0 ? INHERIT_ALL
          : USE_PALETTE);
    if (obj instanceof Integer)
      return Colix3D.getColix(((Integer) obj).intValue());
    if (obj instanceof String)
      return getColix((String) obj);
    if (Logger.debugging) {
      Logger.debug("?? getColix(" + obj + ")");
    }
    return HOTPINK;
  }

  public final static short getColixTranslucent(short colix, boolean isTranslucent, float translucentLevel) {
    if (colix == INHERIT_ALL)
      colix = INHERIT_COLOR;
    colix &= ~TRANSLUCENT_MASK;
    if (!isTranslucent)
      return colix;
    return applyColorTranslucencyLevel(colix, translucentLevel);
  }

  public int getColixArgb(short colix) {
    if (colix < 0)
      colix = changeableColixMap[colix & UNMASK_CHANGEABLE_TRANSLUCENT];
    if (! inGreyscaleMode)
      return Colix3D.getArgb(colix);
    return Colix3D.getArgbGreyscale(colix);
  }

  int[] getShades(short colix) {
    if (colix < 0)
      colix = changeableColixMap[colix & UNMASK_CHANGEABLE_TRANSLUCENT];
    if (! inGreyscaleMode)
      return Colix3D.getShades(colix);
    return Colix3D.getShadesGreyscale(colix);
  }

  public final static short getChangeableColixIndex(short colix) {
    if (colix >= 0)
      return -1;
    return (short)(colix & UNMASK_CHANGEABLE_TRANSLUCENT);
  }

  public final static boolean isColixTranslucent(short colix) {
    return ((colix & TRANSLUCENT_MASK) != 0);
  }

  public final static short getColixInherited(short myColix, short parentColix) {
    switch (myColix) {
    case INHERIT_ALL:
      return parentColix;
    case INHERIT_COLOR:
      return (short) (parentColix & OPAQUE_MASK);
    default:
      //check this colix irrespective of translucency, and if inherit, then
      //it must be inherit color but not translucent level; 
      return ((myColix & OPAQUE_MASK) == INHERIT_COLOR ? (short) (parentColix
          & OPAQUE_MASK | myColix & TRANSLUCENT_MASK) : myColix);
    }
  }

  public final static boolean isColixColorInherited(short colix) {
    switch (colix) {
    case INHERIT_ALL:
    case INHERIT_COLOR:
      return true;
    default: //could be translucent of some sort
      return (colix & OPAQUE_MASK) == INHERIT_COLOR; 
    }
  }
  
  //no references:

  /*
  
  public final static short getColixInherited(short myColix, short parentColix,
                                              short grandParentColix) {
    if ((myColix & OPAQUE_MASK) >= SPECIAL_COLIX_MAX)
      return myColix;
    return getColixInherited(myColix, getColixInherited(parentColix,
        grandParentColix));
  }

  public final short getColixMix(short colixA, short colixB) {
    return Colix.getColixMix(colixA >= 0 ? colixA :
                             changeableColixMap[colixA &
                                               UNMASK_CHANGEABLE_TRANSLUCENT],
                             colixB >= 0 ? colixB :
                             changeableColixMap[colixB &
                                               UNMASK_CHANGEABLE_TRANSLUCENT]);
  }
 */
  
  public String getHexColorFromIndex(short colix) {
    int argb = getColixArgb(colix);
    return getHexColorFromRGB(argb);
  }
  
  public static String getHexColorFromRGB(int argb) {
    if (argb == 0)
      return null;
    String r  = "00" + Integer.toHexString((argb >> 16) & 0xFF);
    r = r.substring(r.length() - 2);
    String g  = "00" + Integer.toHexString((argb >> 8) & 0xFF);
    g = g.substring(g.length() - 2);
    String b  = "00" + Integer.toHexString(argb & 0xFF);
    b = b.substring(b.length() - 2);
    return r + g + b;
  }

  /****************************************************************
   * changeable colixes
   * give me a short ID and a color, and I will give you a colix
   * later, you can reassign the color if you want
   * Used only for colorManager coloring of elements
   ****************************************************************/

  private short[] changeableColixMap = new short[16];

  public short getChangeableColix(short id, int argb) {
    if (id >= changeableColixMap.length) {
      short[] t = new short[id + 16];
      System.arraycopy(changeableColixMap, 0, t, 0, changeableColixMap.length);
      changeableColixMap = t;
    }
    if (changeableColixMap[id] == 0)
      changeableColixMap[id] = Colix3D.getColix(argb);
    return (short)(id | CHANGEABLE_MASK);
  }

  public void changeColixArgb(short id, int argb) {
    if (id < changeableColixMap.length && changeableColixMap[id] != 0)
      changeableColixMap[id] = Colix3D.getColix(argb);
  }

  /* ***************************************************************
   * shading and lighting
   * ***************************************************************/

  public static void flushShadesAndSphereCaches() { 
    Colix3D.flushShades();
    Sphere3D.flushSphereCache();
  }

  final static float[] lighting = Shade3D.lighting;
  
  public synchronized static void setSpecular(boolean specular) {
    lighting[Shade3D.SPECULAR_ON] = (specular ? 1f : 0f);
  }

  public static boolean getSpecular() {
    return (lighting[Shade3D.SPECULAR_ON] != 0);
  }

  public synchronized static void setSpecularPower(int specularPower) {
    lighting[Shade3D.SPECULAR_POWER] = specularPower;
    lighting[Shade3D.INTENSE_FRACTION] = specularPower / 100f;
  }
  
  public static int getSpecularPower() {
    return (int) lighting[Shade3D.SPECULAR_POWER];
  }
  
  public synchronized static void setSpecularPercent(int specularPercent) {
    lighting[Shade3D.SPECULAR_PERCENT]= specularPercent;
    lighting[Shade3D.INTENSITY_SPECULAR] = specularPercent / 100f;
  }

  public static int getSpecularPercent() {
    return (int) lighting[Shade3D.SPECULAR_PERCENT];
  }

  public synchronized static void setSpecularExponent(int specularExponent) {
    lighting[Shade3D.SPECULAR_EXPONENT] = specularExponent;
  }
  
  public static int getSpecularExponent() {
    return (int) lighting[Shade3D.SPECULAR_EXPONENT];
  }
  
  public synchronized static void setDiffusePercent(int diffusePercent) {
    lighting[Shade3D.DIFFUSE_PERCENT]= diffusePercent;
    lighting[Shade3D.INTENSITY_DIFFUSE]= diffusePercent / 100f;
  }

  public static int getDiffusePercent() {
    return (int) lighting[Shade3D.DIFFUSE_PERCENT];
  }
  
  public synchronized static void setAmbientPercent(int ambientPercent) {
    lighting[Shade3D.AMBIENT_PERCENT] = ambientPercent;
    lighting[Shade3D.AMBIENT_FRACTION] = ambientPercent / 100f;
  }

  public static int getAmbientPercent() {
    return (int) (lighting[Shade3D.AMBIENT_PERCENT]);
  }
  
  public static Point3f getLightSource() {
    return new Point3f(Shade3D.xLight, Shade3D.yLight, Shade3D.zLight);
  }
  
  /*
  public void setLightsourceZ(float dist) {
    Shade3D.setLightsourceZ(dist);
  }
  */
  
  private final Vector3f vectorAB = new Vector3f();
  private final Vector3f vectorAC = new Vector3f();
  private final Vector3f vectorNormal = new Vector3f();
  // these points are in screen coordinates even though 3f

  public int calcSurfaceShade(Point3i screenA, Point3i screenB, Point3i screenC) {
    // or center and point, as for an ellipse
    vectorAB.set(screenB.x - screenA.x, screenB.y - screenA.y, screenB.z
        - screenA.z);
    int intensity;
    if (screenC == null) {
      intensity = Shade3D.calcIntensity(-vectorAB.x, -vectorAB.y, vectorAB.z);
    } else {
      vectorAC.set(screenC.x - screenA.x, screenC.y - screenA.y, screenC.z
          - screenA.z);
      vectorAB.cross(vectorAB, vectorAC);
      intensity = vectorAB.z >= 0 ? Shade3D.calcIntensity(-vectorAB.x,
          -vectorAB.y, vectorAB.z) : Shade3D.calcIntensity(vectorAB.x,
          vectorAB.y, -vectorAB.z);
    }
    if (intensity > intensitySpecularSurfaceLimit)
      intensity = intensitySpecularSurfaceLimit;
    setColorNoisy(intensity);
    return argbCurrent;
  }

  private int calcIntensityScreen(Point3f screenA,
                                 Point3f screenB, Point3f screenC) {
    // for fillTriangle and fillQuad.
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

  /* ***************************************************************
   * fontID stuff
   * a fontID is a byte that contains the size + the face + the style
   * ***************************************************************/

  public Font3D getFont3D(float fontSize) {
    return Font3D.getFont3D(Font3D.FONT_FACE_SANS,
                            Font3D.FONT_STYLE_PLAIN, fontSize, fontSize, platform);
  }

  public Font3D getFont3D(String fontFace, float fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.FONT_STYLE_PLAIN, fontSize, fontSize, platform);
  }
    
  // {"Plain", "Bold", "Italic", "BoldItalic"};
  public Font3D getFont3D(String fontFace, String fontStyle, float fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.getFontStyleID(fontStyle), fontSize, fontSize, platform);
  }

  public Font3D getFont3DScaled(Font3D font, float scale) {
    float newScale = font.fontSizeNominal * scale;
    return (newScale == font.fontSize ? font : Font3D.getFont3D(font.idFontFace,
        font.idFontStyle, newScale, font.fontSizeNominal, platform));
  }

  public byte getFontFid(float fontSize) {
    return getFont3D(fontSize).fid;
  }

  public byte getFontFid(String fontFace, float fontSize) {
    return getFont3D(fontFace, fontSize).fid;
  }

  /* ***************************************************************
   * known JavaScript colors
   * ***************************************************************/

  // 140 JavaScript color names
  // includes 16 official HTML 4.0 color names & values
  // plus a few extra rasmol names

  private final static String[] colorNames = {
    "black",                // 000000
    "pewhite",              // ffffff
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

    "aliceblue",            // F0F8FF
    "antiquewhite",         // FAEBD7
    "aqua",                 // 00FFFF
    "aquamarine",           // 7FFFD4
    "azure",                // F0FFFF
    "beige",                // F5F5DC
    "bisque",               // FFE4C4
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
  };

  public static int getColorArgb(int i) {
    return colorArgbs[i % colorArgbs.length];
  }

  private final static int[] colorArgbs = {
    0xFF000000, // black
    // plus the PE chain colors
    0xFFffffff, // pewhite
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
    // standard JavaScript
    0xFFF0F8FF, // aliceblue
    0xFFFAEBD7, // antiquewhite
    0xFF00FFFF, // aqua
    0xFF7FFFD4, // aquamarine
    0xFFF0FFFF, // azure
    0xFFF5F5DC, // beige
    0xFFFFE4C4, // bisque
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
  };

  private static final Hashtable mapJavaScriptColors = new Hashtable();
  static {
    for (int i = colorNames.length; --i >= 0; )
      mapJavaScriptColors.put(colorNames[i], new Integer(colorArgbs[i]));
  }

  /**
   * accepts [xRRGGBB] or [0xRRGGBB] or [0xFFRRGGBB] or #RRGGBB
   * or a valid JavaScript color
   * 
   * @param strColor
   * @return 0 if invalid or integer color 
   */
  public static int getArgbFromString(String strColor) {
    int len = 0;
    if (strColor == null || (len = strColor.length()) == 0)
      return 0;
    if (strColor.charAt(0) == '[' && strColor.charAt(len - 1) == ']') {
      String check;
      switch (len) {
      case 9:
        check = "x";
        break;
      case 10:
        check = "0x";
        break;
      default:
        return 0;
      }
      if (strColor.indexOf(check) != 1)
        return 0;
      strColor = "#" + strColor.substring(len - 7, len - 1);
      len = 7;
    }
    if (len == 7 && strColor.charAt(0) == '#') {
      try {
        int red = Integer.parseInt(strColor.substring(1, 3), 16);
        int grn = Integer.parseInt(strColor.substring(3, 5), 16);
        int blu = Integer.parseInt(strColor.substring(5, 7), 16);
        return (0xFF000000 | (red & 0xFF) << 16 | (grn & 0xFF) << 8 | (blu & 0xFF));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    Integer boxedArgb = 
        (Integer) mapJavaScriptColors.get(strColor.toLowerCase());
    return (boxedArgb == null ? 0 : boxedArgb.intValue());
  }

  /* ***************************************************************
   * normals and normal indexes -- normix
   * ***************************************************************/

  public static float distanceToPlane(Point4f plane, Point3f pt) {
    return (plane == null ? Float.NaN 
        : (plane.x * pt.x + plane.y * pt.y + plane.z * pt.z + plane.w)
        / (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z
            * plane.z));
  }

  public static float distanceToPlane(Point4f plane, float d, Point3f pt) {
    return (plane == null ? Float.NaN : (plane.x * pt.x + plane.y
        * pt.y + plane.z * pt.z + plane.w) / d);
  }

  public static float distanceToPlane(Vector3f norm, float w, Point3f pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }

  public static void calcNormalizedNormal(Point3f pointA, Point3f pointB,
         Point3f pointC, Vector3f vNormNorm, Vector3f vAB, Vector3f vAC) {
    vAB.sub(pointB, pointA);
    vAC.sub(pointC, pointA);
    vNormNorm.cross(vAB, vAC);
    vNormNorm.normalize();
  }

  public static float getDirectedNormalThroughPoints(Point3f pointA, 
         Point3f pointB, Point3f pointC, Point3f ptRef, Vector3f vNorm, 
         Vector3f vAB, Vector3f vAC) {
    // for x = plane({atomno=1}, {atomno=2}, {atomno=3}, {atomno=4})
    float nd = getNormalThroughPoints(pointA, pointB, pointC, vNorm, vAB, vAC);
    if (ptRef != null) {
      Point3f pt0 = new Point3f(pointA);
      pt0.add(vNorm);
      float d = pt0.distance(ptRef);
      pt0.set(pointA);
      pt0.sub(vNorm);
      if (d > pt0.distance(ptRef)) {
        vNorm.scale(-1);
        nd = -nd;
      }
    }
    return nd;
  }
  
  public static float getNormalThroughPoints(Point3f pointA, Point3f pointB,
                                   Point3f pointC, Vector3f vNorm, Vector3f vAB, Vector3f vAC) {
    // for Polyhedra
    calcNormalizedNormal(pointA, pointB, pointC, vNorm, vAB, vAC);
    // ax + by + cz + d = 0
    // so if a point is in the plane, then N dot X = -d
    vAB.set(pointA);
    float d = -vAB.dot(vNorm);
    return d;
  }

  public static void getNormalFromCenter(Point3f ptCenter, Point3f ptA, Point3f ptB,
                            Point3f ptC, boolean isOutward, Vector3f normal) {
    // for Polyhedra
    Point3f ptT = new Point3f();
    Point3f ptT2 = new Point3f();
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    calcNormalizedNormal(ptA, ptB, ptC, normal, vAB, vAC);
    //but which way is it? add N to A and see who is closer to Center, A or N. 
    ptT.set(ptA);
    ptT.add(ptB);
    ptT.add(ptC);
    ptT.scale(1/3f);
    ptT2.set(normal);
    ptT2.scale(0.1f);
    ptT2.add(ptT);
    //              A      C         Bob Hanson 2006
    //                \   /
    //                 \ / 
    //                  x pT is center of ABC; ptT2 is offset a bit from that
    //                  |    either closer to x (ok if not opaque) or further
    //                  |    from x (ok if opaque)
    //                  B
    // in the case of facet ABx, the "center" is really the OTHER point, C.
    boolean doReverse = (isOutward && ptCenter.distance(ptT2) < ptCenter.distance(ptT)
        || !isOutward && ptCenter.distance(ptT) < ptCenter.distance(ptT2));
    if (doReverse)
      normal.scale(-1f);
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
  
  public static void projectOntoAxis (Point3f point, Point3f axisA, Vector3f axisUnitVector, Vector3f vectorProjection) {
    vectorProjection.sub(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.set(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
    vectorProjection.sub(point, axisA);
  }
  
  public static void calcBestAxisThroughPoints(Point3f[] points, Point3f axisA,
                                               Vector3f axisUnitVector,
                                               Vector3f vectorProjection,
                                               int nTriesMax) {
    // just a crude starting point.

    int nPoints = points.length;
    axisA.set(points[0]);
    axisUnitVector.sub(points[nPoints - 1], axisA);
    axisUnitVector.normalize();

    /*
     * We now calculate the least-squares 3D axis
     * through the helix alpha carbons starting with Vo
     * as a first approximation.
     * 
     * This uses the simple 0-centered least squares fit:
     * 
     * Y = M cross Xi
     * 
     * minimizing R^2 = SUM(|Y - Yi|^2) 
     * 
     * where Yi is the vector PERPENDICULAR of the point onto axis Vo
     * and Xi is the vector PROJECTION of the point onto axis Vo
     * and M is a vector adjustment 
     * 
     * M = SUM_(Xi cross Yi) / sum(|Xi|^2)
     * 
     * from which we arrive at:
     * 
     * V = Vo + (M cross Vo)
     * 
     * Basically, this is just a 3D version of a 
     * standard 2D least squares fit to a line, where we would say:
     * 
     * y = m xi + b
     * 
     * D = n (sum xi^2) - (sum xi)^2
     * 
     * m = [(n sum xiyi) - (sum xi)(sum yi)] / D
     * b = [(sum yi) (sum xi^2) - (sum xi)(sum xiyi)] / D
     * 
     * but here we demand that the line go through the center, so we
     * require (sum xi) = (sum yi) = 0, so b = 0 and
     * 
     * m = (sum xiyi) / (sum xi^2)
     * 
     * In 3D we do the same but 
     * instead of x we have Vo,
     * instead of multiplication we use cross products
     * 
     * A bit of iteration is necessary.
     * 
     * Bob Hanson 11/2006
     * 
     */

    calcAveragePointN(points, nPoints, axisA);

    int nTries = 0;
    while (nTries++ < nTriesMax
        && findAxis(points, nPoints, axisA, axisUnitVector, vectorProjection) > 0.001) {
    }

    /*
     * Iteration here gets the job done.
     * We now find the projections of the endpoints onto the axis
     * 
     */

    Point3f tempA = new Point3f(points[0]);
    Graphics3D.projectOntoAxis(tempA, axisA, axisUnitVector, vectorProjection);
    axisA.set(tempA);
  }

  static float findAxis(Point3f[] points, int nPoints, Point3f axisA,
                        Vector3f axisUnitVector, Vector3f vectorProjection) {
    Vector3f sumXiYi = new Vector3f();
    Vector3f vTemp = new Vector3f();
    Point3f pt = new Point3f();
    Point3f ptProj = new Point3f();
    Vector3f a = new Vector3f(axisUnitVector);

    float sum_Xi2 = 0;
    float sum_Yi2 = 0;
    for (int i = nPoints; --i >= 0;) {
      pt.set(points[i]);
      ptProj.set(pt);
      Graphics3D.projectOntoAxis(ptProj, axisA, axisUnitVector,
          vectorProjection);
      vTemp.sub(pt, ptProj);
      sum_Yi2 += vTemp.lengthSquared();
      vTemp.cross(vectorProjection, vTemp);
      sumXiYi.add(vTemp);
      sum_Xi2 += vectorProjection.lengthSquared();
    }
    Vector3f m = new Vector3f(sumXiYi);
    m.scale(1 / sum_Xi2);
    vTemp.cross(m, axisUnitVector);
    axisUnitVector.add(vTemp);
    axisUnitVector.normalize();
    //check for change in direction by measuring vector difference length
    vTemp.set(axisUnitVector);
    vTemp.sub(a);
    return vTemp.length();
  }
  
  
  public static void calcAveragePoint(Point3f pointA, Point3f pointB,
                                      Point3f pointC) {
    pointC.set((pointA.x + pointB.x) / 2, (pointA.y + pointB.y) / 2,
        (pointA.z + pointB.z) / 2);
  }
  
  public static void calcAveragePointN(Point3f[] points, int nPoints,
                                Point3f averagePoint) {
    averagePoint.set(points[0]);
    for (int i = 1; i < nPoints; i++)
      averagePoint.add(points[i]);
    averagePoint.scale(1f / nPoints);
  }

  public short getNormix(Vector3f vector) {
    return normix3d.getNormix(vector.x, vector.y, vector.z,
                              Normix3D.NORMIX_GEODESIC_LEVEL);
  }

  public short getInverseNormix(short normix) {
    if (normix3d.inverseNormixes == null)
      normix3d.calculateInverseNormixes();
    return normix3d.inverseNormixes[normix];
  }

  public short get2SidedNormix(Vector3f vector) {
    return (short)~normix3d.getNormix(vector.x, vector.y, vector.z,
                                      Normix3D.NORMIX_GEODESIC_LEVEL);
  }

  public boolean isDirectedTowardsCamera(short normix) {
    //polyhedra
    return normix3d.isDirectedTowardsCamera(normix);
  }

  public Vector3f[] getTransformedVertexVectors() {
    return normix3d.getTransformedVectors();
  }

  public Vector3f getNormixVector(short normix) {
    return normix3d.getVector(normix);
  }

}
