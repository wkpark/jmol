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

package org.openscience.jmol.render;

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;

class SphereG2D {

  Image imageSphereG2D(Image imgTemplate, int diameter) {
    BufferedImage bi = new BufferedImage(diameter+2, diameter+2,
                                         BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = bi.createGraphics();
    drawSphereG2D(g2, imgTemplate, 0, 0, diameter, 1);
    g2.dispose();
    return bi;
  }

  void drawClippedSphereG2D(Graphics25D g25d,
                            Image imgSphere, int x, int y, int d) {
    // too big ... just forget the smoothing
    // but we *can* clip it to eliminate fat pixels
    Ellipse2D circle = new Ellipse2D.Double(x, y, d, d);
    g25d.setClip(circle);
    g25d.drawImage(imgSphere, x, y, 0, d, d);
    g25d.setClip(null);
  }
  
  private static byte[] mapRGBA;
  private static IndexColorModel cmMask;
  private static int sizeMask = 0;
  private static BufferedImage biMask = null;
  private static Graphics2D g2Mask;
  private static BufferedImage biAlphaMask;

  private void applyCircleMask(Graphics2D g, int diameter, int margin) {
    // mth 2002 nov 12
    // a 4 bit greyscale mask would/should be sufficient here, but there
    // was a bug in my JVM (or a bug in my head) which prevented it
    // from working
    if (mapRGBA == null) {
      mapRGBA = new byte[256];
      for (int i = 0; i < 256; ++ i) {
        mapRGBA[i] = (byte) i;
      }
      cmMask = new IndexColorModel(8, 256, mapRGBA, mapRGBA, mapRGBA, mapRGBA);
    }
    int size = diameter + 2*margin;
    if (size > sizeMask) {
      sizeMask = size * 2;
      if (sizeMask < minShadingBufferSize)
        sizeMask = minShadingBufferSize;
      if (sizeMask > maxShadingBufferSize)
        sizeMask = maxShadingBufferSize;
      biMask = new BufferedImage(sizeMask, sizeMask,
                                 BufferedImage.TYPE_BYTE_GRAY);
      g2Mask = biMask.createGraphics();
      g2Mask.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      biAlphaMask = new BufferedImage(cmMask, biMask.getRaster(), false, null);
    }
    g2Mask.setColor(Color.black);
    g2Mask.fillRect(0, 0, size, size);
    g2Mask.setColor(Color.white);
    g2Mask.fillOval(margin, margin, diameter, diameter);

    Composite foo = g.getComposite();
    g.setComposite(AlphaComposite.DstIn);
    g.drawImage(biAlphaMask, 0, 0, null);
    g.setComposite(foo);
  }

  private static final int minShadingBufferSize =
    ShadedSphereRenderer.maxCachedSize;
  private static final int maxShadingBufferSize =
    ShadedSphereRenderer.maxSmoothedSize +
    ShadedSphereRenderer.artifactMargin * 2;
    
  private static int sizeShadingBuffer = 0;
  private static BufferedImage biShadingBuffer = null;
  private static Graphics2D g2ShadingBuffer = null;

  void drawSphereG2D(Graphics g, Image image, int xUpperLeft, int yUpperLeft,
                     int diameter, int margin) {
    Graphics2D g2 = (Graphics2D)g;
    final int size = diameter + 2*margin;
    if (size > sizeShadingBuffer) {
      sizeShadingBuffer = size * 2; // leave some room to grow
      if (sizeShadingBuffer < minShadingBufferSize)
        sizeShadingBuffer = minShadingBufferSize;
      if (sizeShadingBuffer > maxShadingBufferSize)
        sizeShadingBuffer = maxShadingBufferSize;
      biShadingBuffer = new BufferedImage(sizeShadingBuffer, sizeShadingBuffer,
                                          BufferedImage.TYPE_INT_ARGB);
      g2ShadingBuffer = biShadingBuffer.createGraphics();
      g2ShadingBuffer.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    g2ShadingBuffer.drawImage(image, 0, 0, size, size, null);
    applyCircleMask(g2ShadingBuffer, diameter, margin);
    g2.setClip(xUpperLeft, yUpperLeft, size, size);
    g2.drawImage(biShadingBuffer, xUpperLeft, yUpperLeft, null);
    g2.setClip(null);
  }

  void drawSphereG2D(Graphics25D g25d, Image image,
                     int xUpperLeft, int yUpperLeft,
                     int diameter, int margin) {
    final int size = diameter + 2*margin;
    if (size > sizeShadingBuffer) {
      sizeShadingBuffer = size * 2; // leave some room to grow
      if (sizeShadingBuffer < minShadingBufferSize)
        sizeShadingBuffer = minShadingBufferSize;
      if (sizeShadingBuffer > maxShadingBufferSize)
        sizeShadingBuffer = maxShadingBufferSize;
      biShadingBuffer = new BufferedImage(sizeShadingBuffer, sizeShadingBuffer,
                                          BufferedImage.TYPE_INT_ARGB);
      g2ShadingBuffer = biShadingBuffer.createGraphics();
      g2ShadingBuffer.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                              RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    g2ShadingBuffer.drawImage(image, 0, 0, size, size, null);
    applyCircleMask(g2ShadingBuffer, diameter, margin);
    g25d.setClip(xUpperLeft, yUpperLeft, size, size);
    g25d.drawImage(biShadingBuffer, xUpperLeft, yUpperLeft, 0);
    g25d.setClip(null);
  }
  
}
