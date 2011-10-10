/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmolandroid.api;

import java.net.URL;

import org.jmol.api.ApiPlatform;
import org.jmol.g3d.Font3D;
import org.jmol.util.JpegEncoder;
import org.jmol.viewer.Viewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

  Bitmap bitmap;
  Canvas canvas;

  static Object createImage(Object data) {
    // can be ignored
    return null;
  }

  static void waitForDisplay(Object display, Object image) throws InterruptedException {
    // can be ignored
  }

  static int getWidth(Object image) {
    return ((Image) image).bitmap.getWidth();
  }

  static int getHeight(Object image) {
    return ((Image) image).bitmap.getHeight();
  }

  static Object getJpgImage(ApiPlatform apiPlatform, Viewer viewer, int quality, String comment) {
    // can be ignored
    return null;
  }

  static void grabPixels(Object imageobj, int imageWidth,
                                int imageHeight, int[] values) {
    // can be ignored
  }

  static int[] grabPixels(Object imageobj, int x, int y, int width,
                                 int height) {
    int[] pixels = new int[width * height];
    ((Image) imageobj).bitmap.getPixels(pixels, 0, width, x, y, width, height);
    return pixels;
  }

  static int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                Object imageobj, int width, int height, int bgcolor) {
    Bitmap bitmap = ((Image) imageOffscreen).bitmap;
    Canvas canvas = (Canvas) gOffscreen;
    int width0 = bitmap.getWidth();
    int height0 = bitmap.getHeight();
    if (g instanceof Graphics2D) {
      ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f));
      g.setColor(bgcolor == 0 ? new Color(0, 0, 0, 0) : new Color(bgcolor));
      g.fillRect(0, 0, width, height);
      ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
      g.drawImage(image, 0, 0, width, height, 0, 0, width0, height0, null);
    } else {
      g.clearRect(0, 0, width, height);
      g.drawImage(image, 0, 0, width, height, 0, 0, width0, height0, null);
    }
    return org.jmolImage.grabPixels(imageOffscreen,
        0, 0, width, height);
  }

  static void renderOffScreen(String text, Font3D font3d, Object gObj,
                                     int mapWidth, int height, int ascent) {
    Graphics g = (Graphics) gObj;
    g.setColor(Color.black);
    g.fillRect(0, 0, mapWidth, height);
    g.setColor(Color.white);
    g.setFont((Font) font3d.font);
    g.drawString(text, 0, ascent);
  }

  static Object newBufferedImage(Object image, int w, int h) {
    return new BufferedImage(w, h, ((BufferedImage) image).getType());
  }

  static Object newBufferedImage(int w, int h) {
    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  /**
   * @param windowWidth 
   * @param windowHeight 
   * @param pBuffer 
   * @param windowSize 
   * @param backgroundTransparent  
   * @return   an Image
   */
  static Object allocateRgbImage(int windowWidth, int windowHeight,
                                       int[] pBuffer, int windowSize, boolean backgroundTransparent) {
    
    //backgroundTransparent not working with antialiasDisplay. I have no idea why. BH 9/24/08
    /* DEAD CODE   if (false && backgroundTransparent)
          return new BufferedImage(
              rgbColorModelT,
              Raster.createWritableRaster(
                  new SinglePixelPackedSampleModel(
                      DataBuffer.TYPE_INT,
                      windowWidth,
                      windowHeight,
                      sampleModelBitMasksT), 
                  new DataBufferInt(pBuffer, windowSize),
                  null),
              false, 
              null);
    */
    return new BufferedImage(
        rgbColorModel,
        Raster.createWritableRaster(
            new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT,
                windowWidth,
                windowHeight,
                sampleModelBitMasks), 
            new DataBufferInt(pBuffer, windowSize),
            null),
        false, 
        null);
  }

  /**
   * @param image 
   * @param backgroundTransparent  
   * @return Graphics object
   */
  static Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    // ignore transparent;
      return getGraphics(image); 
  }

  static Object getGraphics(Object image) {
    Canvas canvas = ((Image) image).canvas;
    if (canvas == null)
      ((Image) image).canvas = new Canvas(((Image) image).bitmap);
    return canvas;
  }

  static void drawImage(Object g, Object img, int x, int y) {
    ((Canvas) g).drawBitmap(((Image) img).bitmap, x, y, null);
  }

  static void flush(Object image) {
    // unnecessary?
  }

  static void disposeGraphics(Object g) {
    // unnecessary?
  }

  /*
   * exporter only
  public void setPixel(int x, int y, int argb) {
    bitmap.setPixel(x, y, argb);
  }
  public void compress(Format format, int quality, OutputStream stream) {
    bitmap.compress(format == Format.JPEG ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG, quality, stream);
  }
*/

}
