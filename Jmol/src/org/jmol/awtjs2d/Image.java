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

package org.jmol.awtjs2d;

import org.jmol.api.ApiPlatform;
import org.jmol.util.JmolFont;
import org.jmol.viewer.Viewer;

/**
 * methods required by Jmol that access java.awt.Image
 * 
 * private to org.jmol.awt
 * 
 */
class Image {

  static Object createImage(Object data) {
    // getFileAsImage
    return null;
  }

  /**
   * @param display  
   * @param image 
   * @throws InterruptedException 
   */
  static void waitForDisplay(Object display, Object image) throws InterruptedException {
    // this is important primarily for retrieving images from 
    // files, as in set echo ID myimage "image.gif"
  }

  /**
   * @param canvas  
   * @return width
   */
  static int getWidth(Object canvas) {
    /**
     * @j2sNative
     * 
     * return canvas.width;
     */
    return 0;
  }

  /**
   * @param canvas  
   * @return width
   */
  static int getHeight(Object canvas) {
    /**
     * @j2sNative
     * 
     * return canvas.height;
     */
    return 0;
  }

  /**
   * @param apiPlatform 
   * @param viewer 
   * @param quality  
   * @param comment 
   * @return null
   */
  static Object getJpgImage(ApiPlatform apiPlatform, Viewer viewer, int quality, String comment) {
    return  null;
  }

  /**
   * @param context  
   * @param width 
   * @param height 
   * @return null
   */
  static int[] grabPixels(Object context, int width, int height) {
    /**
     * @j2sNative
     * 
     * return context.getImageData(0, 0, width, height);
     */
    return null;
  }

  static int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height,
                                 int bgcolor) {
    // this is a background image;
    return null;
  }

  public static int[] getTextPixels(String text, JmolFont font3d, Object context,
                                    Object context2, int width, int height,
                                    int ascent) {
    /**
     * @j2sNative
     * 
     * context.setColor("#000000");
     * context.fillRect(0, 0, width, height);
     * context.setColor("#FFFFFF")
     * context.fillText(text, 0, ascent);
     * return grabPixels(context, width, height);
     */
    {
      return null;
    }
  }

  static Object newBufferedImage(Object image, int w, int h) {
    /**
     * @j2sNative
     * 
     * return null; // would be for stereo
     */
    return null;
  }

  /**
   * @param windowWidth 
   * @param windowHeight 
   * @param pBuffer 
   * @param windowSize 
   * @param backgroundTransparent  
   * @param display TODO
   * @return   an Image
   */
  static Object allocateRgbImage(int windowWidth, int windowHeight,
                                       int[] pBuffer, int windowSize, boolean backgroundTransparent, Object display) {
    // the pixelBuffer itself is used for storage. 
    return pBuffer;
  }

  /**
   * @param image 
   * @param backgroundTransparent  
   * @return Graphics object
   */
  static Object getStaticGraphics(Object image, boolean backgroundTransparent) {
    // for text processing;
    return null;
    }

  /**
   * @param image  
   * @return 
   */
  static Object getGraphics(Object image) {
    /**
     * @j2sNative
     * 
     * return image.getContext();
     */
    return null;
  }

  /**
   * 
   * @param g
   * @param img
   * @param x
   * @param y
   * @param width  unused in Jmol proper
   * @param height unused in Jmol proper
   */
  static void drawImage(Object g, Object img, int x, int y, int width, int height) {
    //((Graphics)g).drawImage((java.awt.Image) img, x, y, null);
  }

  static void flush(Object image) {
    //((java.awt.Image) image).flush();
  }

  static void disposeGraphics(Object graphicForText) {
    //((java.awt.Graphics) graphicForText).dispose();
  }

}
