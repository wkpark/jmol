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

package org.openscience.jmol.g25d;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

final public class Swing25D implements Platform25D{

  int width, height;
  BufferedImage bi;
  WritableRaster wr;
  DataBuffer db;
  DataBufferInt dbi;
  int[] pbuf;
  Graphics2D g2;

  public Image allocateImage(int width, int height, boolean enabled) {
    this.width = width;
    this.height = height;
    bi = new BufferedImage(width, height,
                           enabled
                           ? BufferedImage.TYPE_INT_RGB
                           : BufferedImage.TYPE_INT_ARGB);
    wr = bi.getRaster();
    db = wr.getDataBuffer();
    dbi = (DataBufferInt) db;
    pbuf = dbi.getData();
    g2 = bi.createGraphics();
    return bi;
  }

  public Graphics getGraphics() {
    return g2;
  }

  public int[] getPbuf() {
    return pbuf;
  }

  public void notifyEndOfRendering() {
  }
}
