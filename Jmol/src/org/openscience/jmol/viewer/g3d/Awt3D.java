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

final public class Awt3D {

  Component component;
  int width, height;
  boolean enabledPbuf = false;
  Image image;
  MemoryImageSource mis;
  Graphics g;
  Image imageFontOps; // this image & graphics are used for font operations;
  int[] pbuf;

  public Awt3D(Component component) {
    this.component = component;
  }

  public void allocateImage(int width, int height, boolean enabledPbuf) {
    this.width = width;
    this.height = height;
    this.enabledPbuf = enabledPbuf;
    if (enabledPbuf) {
      pbuf = new int[width * height];
      mis = new MemoryImageSource(width, height, pbuf, 0, width);
      mis.setAnimated(true);
      image = component.createImage(mis);
      if (g != null) g.dispose();
      imageFontOps = component.createImage(10, 10);
      g = imageFontOps.getGraphics();
    } else {
      pbuf = null;
      mis = null;
      imageFontOps = null;
      image = component.createImage(width, height);
      if (g != null) g.dispose();
      g = image.getGraphics();
    }
  }

  public Image getImage() {
    return image;
  }

  public Graphics getGraphics() {
    return g;
  }

  public int[] getPbuf() {
    return pbuf;
  }

  public void notifyEndOfRendering() {
    if (enabledPbuf)
      mis.newPixels();
  }
}
