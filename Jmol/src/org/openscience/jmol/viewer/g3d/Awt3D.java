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
import java.awt.image.ImageProducer;
import java.awt.image.ImageConsumer;
import java.awt.image.ColorModel;
import java.awt.Rectangle;

final class Awt3D extends Platform3D implements ImageProducer {

  Component component;

  ColorModel colorModelRGB;
  ImageConsumer ic;

  Awt3D(Component component) {
    this.component = component;
    colorModelRGB = ColorModel.getRGBdefault();
  }

  void allocatePixelBuffer() {
    pBuffer = new int[size];
    imagePixelBuffer = component.createImage(this);
  }

  void notifyEndOfRendering() {
    if (this.ic != null)
      startProduction(ic);
  }

  Image allocateOffscreenImage(int width, int height) {
    return component.createImage(widthOffscreen, heightOffscreen);
  }

  public synchronized void addConsumer(ImageConsumer ic) {
    startProduction(ic);
  }

  public boolean isConsumer(ImageConsumer ic) {
    return (this.ic == ic);
  }

  public void removeConsumer(ImageConsumer ic) {
    if (this.ic == ic)
      this.ic = null;
  }

  public void requestTopDownLeftRightResend(ImageConsumer ic) {
  }

  public void startProduction(ImageConsumer ic) {
    if (this.ic != ic) {
      this.ic = ic;
      ic.setDimensions(width, height);
      ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT |
                  ImageConsumer.COMPLETESCANLINES |
                  ImageConsumer.SINGLEPASS);
    }
    ic.setPixels(0, 0, width, height, colorModelRGB, pBuffer, 0, width);
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
  }
}
