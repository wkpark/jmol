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

import java.io.InputStream;

import org.jmol.api.ApiPlatform;
import org.jmol.g3d.Font3D;
import org.jmol.viewer.Viewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

  Bitmap bitmap;
  Canvas canvas;
  Config type;

  Image(int width, int height, Config type) {
    this.type = (type == null ? Bitmap.Config.ARGB_8888 : type);
    bitmap = Bitmap.createBitmap(width, height, this.type);
  }

  Image(InputStream stream) {
    bitmap = BitmapFactory.decodeStream(stream);
    type = bitmap.getConfig();
  }

  
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

  static int[] grabPixels(Object imageobj, int width, int height) {
    int[] pixels = new int[width * height];
    ((Image) imageobj).bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    return pixels;
  }

  static int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                Object imageobj, int width, int height, int bgcolor) {
    // goffscreen is not necessary. imageOffscreen will have its own canvas object
    return null;
    // for now we can ignore this, as it is only for image objects being created
    // for background images and text images. 
    /*
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
    return grabPixels(imageOffscreen, width, height);
        */
  }

  /**
   * 
   * @param text
   * @param font3d
   * @param gObj     UNUSED
   * @param imgObj
   * @param width
   * @param height
   * @param ascent
   * @return
   */
  public static int[] getTextPixels(String text, Font3D font3d, Object gObj,
                                    Object imgObj, int width, int height,
                                    int ascent) {
    
    Image image = (Image) imgObj;
    image.canvas.clipRect(new Rect(0, 0, width, height));
    image.canvas.drawColor(Color.BLACK);
    Paint paint = (Paint) (font3d.font);
    paint.setColor(Color.WHITE);
    image.canvas.drawText(text, 0, 0, paint);
    /*  ascent not needed here?
    g.setFont((Font) font3d.font);
    g.drawString(text, 0, ascent);
    */
    return grabPixels(image, width, height);
  }

  static Object getJpgImage(ApiPlatform apiPlatform, Viewer viewer, int quality, String comment) {
    // can be ignored
    return null;
  }

  static Object newBufferedImage(Object image, int w, int h) {
    return new Image(w, h, ((Image) image).type);
  }

  static Object newBufferedImage(int w, int h) {
    return new Image(w, h, null);
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
    // in standard Jmol we use an image as a buffer, writing directly to its image buffer.
    // so for Android we do the same thing, only here we just save the pBuffer.
    // no treatment of transparent background.
    return pBuffer;
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

  static void drawImage(Object graphic, Object imgInts, int x, int y, int width, int height) {
    Canvas canvas = ((Canvas) graphic);
    canvas.drawBitmap((int[]) imgInts, 0, canvas.getWidth(), x, y, width, height, true, null);
  }

  static void flush(Object image) {
    // unnecessary?
  }

  static void disposeGraphics(Object graphicForText) {
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
