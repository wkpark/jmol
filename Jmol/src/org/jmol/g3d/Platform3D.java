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
package org.jmol.g3d;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;

abstract class Platform3D {

  int width, height;
  int size;
  Image imagePixelBuffer;
  int[] pBuffer;
  short[] zBuffer;
  int widthOffscreen, heightOffscreen;
  Image imageOffscreen;
  Graphics gOffscreen;

  final static short ZBUFFER_BACKGROUND = 32767;

  abstract void allocatePixelBuffer();

  void allocateBuffers(int width, int height) {
    this.width = width;
    this.height = height;
    size = width * height;
    zBuffer = new short[size];
    if (imagePixelBuffer != null)
      imagePixelBuffer.flush();
    allocatePixelBuffer();
  }

  void clearScreenBuffer(int argbBackground, Rectangle rectClip) {
    int offsetSrc = rectClip.y * width + rectClip.x;
    int countPerLine = rectClip.width;
    int offsetT = offsetSrc;
    for (int i = countPerLine; --i >= 0; ) {
      zBuffer[offsetT] = ZBUFFER_BACKGROUND;
      pBuffer[offsetT++] = argbBackground;
    }
    int offsetDst = offsetSrc + width;
    for (int nLines = rectClip.height-1; --nLines >= 0; offsetDst += width) {
      if (offsetDst + countPerLine > pBuffer.length) {
        System.out.println("\nPlatform3D.clearScreenBuffer dst out of bounds!" +
                           "\npBuffer.length=" + pBuffer.length + 
                           "\noffsetDst=" + offsetDst +
                           "\ncountPerLine=" + countPerLine +
                           "\nrectClip.x=" + rectClip.x +
                           "\nrectClip.y=" + rectClip.y +
                           "\nrectClip.width=" + rectClip.width +
                           "\nrectClip.height=" + rectClip.height +
                           "\noffsetSrc=" + offsetSrc +
                           "\nwidth=" + width +
                           "\nheight=" + height +
                           "\nsize=" + size +
                           "zBuffer.length=" + zBuffer.length);
      }
      System.arraycopy(pBuffer, offsetSrc, pBuffer, offsetDst, countPerLine);
      System.arraycopy(zBuffer, offsetSrc, zBuffer, offsetDst, countPerLine);
    }
  }

  void notifyEndOfRendering() {
  }

  FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }

  abstract Image allocateOffscreenImage(int width, int height);

  void checkOffscreenSize(int width, int height) {
    if (width <= widthOffscreen && height <= heightOffscreen)
      return;
    if (imageOffscreen != null) {
      gOffscreen.dispose();
      imageOffscreen.flush();
    }
    if (width > widthOffscreen)
      widthOffscreen = (width + 63) & ~63;
    if (height > heightOffscreen)
      heightOffscreen = (height + 15) & ~15;
    imageOffscreen = allocateOffscreenImage(widthOffscreen, heightOffscreen);
    gOffscreen = imageOffscreen.getGraphics();
  }
}
