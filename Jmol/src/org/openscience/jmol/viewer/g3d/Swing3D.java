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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.FontMetrics;
import java.awt.Font;

final public class Swing3D implements Platform3D{

  int width, height;
  BufferedImage bi;
  WritableRaster wr;
  DataBuffer db;
  DataBufferInt dbi;
  int[] pbuf;

  Graphics2D gOffscreen;
  BufferedImage biOffscreen;
  int widthOffscreen;
  int heightOffscreen;

  public void allocateImage(int width, int height) {
    this.width = width;
    this.height = height;
    if (bi != null)
      bi.flush();
    bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    wr = bi.getRaster();
    db = wr.getDataBuffer();
    dbi = (DataBufferInt) db;
    pbuf = dbi.getData();
  }

  public Image getImage() {
    return bi;
  }

  public int[] getPbuf() {
    return pbuf;
  }

  public void notifyEndOfRendering() {
  }

  public FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }

  public void checkOffscreenSize(int width, int height) {
    if (width <= widthOffscreen && height <= heightOffscreen)
      return;
    if (biOffscreen != null) {
      gOffscreen.dispose();
      biOffscreen.flush();
    }
    if (width > widthOffscreen)
      widthOffscreen = (width + 63) & ~63;
    if (height > heightOffscreen)
      heightOffscreen = (width + 15) & ~15;
    biOffscreen = new BufferedImage(widthOffscreen, heightOffscreen,
                                    BufferedImage.TYPE_INT_RGB);
    gOffscreen = biOffscreen.createGraphics();
  }

  public Graphics getGraphicsOffscreen() {
    return gOffscreen;
  }

  public Image getImageOffscreen() {
    return biOffscreen;
  }
}
