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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

final public class Awt25D implements Platform25D {

  Component component;
  int width, height;
  Image image;
  Graphics g;
  int[] pbuf;

  public Awt25D(Component component) {
    this.component = component;
  }

  public Image allocateImage(int width, int height, boolean useAlphaChannel) {
    this.width = width;
    this.height = height;
    image = component.createImage(width, height);
    g = (image == null) ? null : image.getGraphics();
    return image;
  }

  public Graphics getGraphics() {
    return g;
  }

  public int[] getPbuf() {
    return pbuf;
  }
}
