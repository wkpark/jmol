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
import java.awt.Rectangle;

final public class Awt3D extends Platform3D {

  Component component;
  MemoryImageSource mis;

  public Awt3D(Component component) {
    this.component = component;
  }

  public void allocatePixelBuffer() {
    pBuffer = new int[size];
    mis = new MemoryImageSource(width, height, pBuffer, 0, width);
    mis.setAnimated(true);
    imagePixelBuffer = component.createImage(mis);
  }

  public void notifyEndOfRendering() {
    mis.newPixels();
  }

  Image allocateOffscreenImage(int width, int height) {
    return component.createImage(widthOffscreen, heightOffscreen);
  }
}
