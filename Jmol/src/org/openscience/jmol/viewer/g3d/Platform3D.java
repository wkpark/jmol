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

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Font;
import java.awt.FontMetrics;

abstract public class Platform3D {

  int width, height;
  Image imagePixelBuf;
  int[] pixelBuf;
  int widthOffscreen, heightOffscreen;
  Image imageOffscreen;
  Graphics gOffscreen;

  abstract void allocatePixelBuf();

  public int[] allocatePixelBuf(int width, int height) {
    this.width = width;
    this.height = height;
    if (imagePixelBuf != null)
      imagePixelBuf.flush();
    allocatePixelBuf();
    return pixelBuf;
  }

  public void notifyEndOfRendering() {
  }

  public FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }

  abstract Image allocateOffscreenImage(int width, int height);

  public void checkOffscreenSize(int width, int height) {
    if (width <= widthOffscreen && height <= heightOffscreen)
      return;
    if (imageOffscreen != null) {
      gOffscreen.dispose();
      imageOffscreen.flush();
    }
    if (width > widthOffscreen)
      widthOffscreen = (width + 63) & ~63;
    if (height > heightOffscreen)
      heightOffscreen = (width + 15) & ~15;
    imageOffscreen = allocateOffscreenImage(widthOffscreen, heightOffscreen);
    gOffscreen = imageOffscreen.getGraphics();
  }
}
