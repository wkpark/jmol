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

final public class Swing3D extends Platform3D {

  public void allocatePixelBuf() {
    BufferedImage bi =
      new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    WritableRaster wr = bi.getRaster();
    DataBuffer db = wr.getDataBuffer();
    DataBufferInt dbi = (DataBufferInt) db;
    pixelBuf = dbi.getData();
    imagePixelBuf = bi;
  }

  Image allocateOffscreenImage(int width, int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  }

}
