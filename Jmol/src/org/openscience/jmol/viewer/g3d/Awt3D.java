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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.FontMetrics;
import java.awt.Font;

final public class Awt3D implements Platform3D {

  Component component;
  int width, height;
  Image image;
  MemoryImageSource mis;
  int[] pbuf;

  int widthOffscreen, heightOffscreen;
  Image imageOffscreen;
  Graphics gOffscreen;

  public Awt3D(Component component) {
    this.component = component;
  }

  public void allocateImage(int width, int height) {
    this.width = width;
    this.height = height;
    pbuf = new int[width * height];
    mis = new MemoryImageSource(width, height, pbuf, 0, width);
    mis.setAnimated(true);
    if (image != null)
      image.flush();
    image = component.createImage(mis);
  }

  public Image getImage() {
    return image;
  }

  public int[] getPbuf() {
    return pbuf;
  }

  public void notifyEndOfRendering() {
    mis.newPixels();
  }

  public FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }

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
    imageOffscreen = component.createImage(widthOffscreen, heightOffscreen);
    gOffscreen = imageOffscreen.getGraphics();
  }

  public Graphics getGraphicsOffscreen() {
    return gOffscreen;
  }

  public Image getImageOffscreen() {
    return imageOffscreen;
  }
}
