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
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.RenderingHints;
import java.util.Enumeration;

public class ShadedSphereRenderer {

  DisplayControl control;
  public ShadedSphereRenderer(DisplayControl control) {
    this.control = control;
  }

  private static final int minCachedSize = 4;
  private static final int maxCachedSize = 50;
  private static final int scalableSize = 47;
  private static final int maxSmoothedSize = 200;
  // I am getting severe graphical artifacts around the edges when
  // rendering hints are turned on. Therefore, I am adding a margin
  // to shaded rendering in order to cut down on edge effects
  private static final int artifactMargin = 4;
  private static final int minShadingBufferSize = maxCachedSize;
  private static final int maxShadingBufferSize =
    maxSmoothedSize + artifactMargin*2;
    
  
  public void render(Graphics g, int xUpperLeft, int yUpperLeft,
                      int diameter, Color color, Color outline) {
    if (diameter < minCachedSize) {
      if (diameter <= 2) {
        if (diameter > 0) {
          g.setColor(outline);
          g.fillRect(xUpperLeft, yUpperLeft, diameter, diameter);
        }
      } else {
        int diamT = diameter-1;
        g.setColor(color);
        g.fillOval(xUpperLeft, yUpperLeft, diamT, diamT);
        g.setColor(outline);
        g.drawOval(xUpperLeft, yUpperLeft, diamT, diamT);
      }
      return;
    }
    Image[] shadedImages = (Image[]) control.imageCache.get(color);
    if (shadedImages == null)
      shadedImages = loadShadedSphereCache(control, color);

    if (diameter < maxCachedSize) {
      // images in the cache have a clear margin of 1
      g.drawImage(shadedImages[diameter],
                   xUpperLeft - 1, yUpperLeft - 1, null);
      return;
    }
    Image imgSphere = shadedImages[0];
    if (! control.getUseGraphics2D()) {
      g.drawImage(imgSphere, xUpperLeft, yUpperLeft,
                  diameter, diameter, null);
      return;
    }
    if (diameter < maxSmoothedSize) {
      drawScaledShadedAtom((Graphics2D) g, imgSphere,
                           xUpperLeft - artifactMargin,
                           yUpperLeft - artifactMargin,
                           diameter, artifactMargin);
    }
    // too big ... just forget the smoothing
    // but we *can* clip it to eliminate fat pixels
    Ellipse2D circle =
      new Ellipse2D.Double(xUpperLeft, yUpperLeft, diameter, diameter);
    g.setClip(circle);
    g.drawImage(imgSphere, xUpperLeft, yUpperLeft, diameter, diameter, null);
    g.setClip(null);
  }

  private static final double[] lightSource = { -1.0f, -1.0f, 2.0f };
  private Image[] loadShadedSphereCache(DisplayControl control, Color color) {
    Image shadedImages[] = new Image[maxCachedSize];
    Component component = control.getAwtComponent();
    control.imageCache.put(color, shadedImages);
    shadedImages[0] = sphereSetup(component, color, scalableSize, lightSource);
    if (! control.getUseGraphics2D()) {
      for (int d = minCachedSize; d < maxCachedSize; ++d) {
        shadedImages[d] = sphereSetup(component, color, d+2, lightSource);
      }
    } else {
      for (int d = minCachedSize; d < maxCachedSize; ++d) {
        BufferedImage bi = new BufferedImage(d+2, d+2,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        drawScaledShadedAtom(g2, shadedImages[0], 0, 0, d, 1);
        shadedImages[d] = bi;
      }
    }
    return shadedImages;
  }
  
  private static byte[] mapRGBA;
  private static IndexColorModel cmMask;
  private static int sizeMask = 0;
  private static BufferedImage biMask = null;
  private static Graphics2D g2Mask;
  private static WritableRaster rasterMask;
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
      rasterMask = biMask.getRaster();
      biAlphaMask = new BufferedImage(cmMask, rasterMask, false, null);
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

  private static int sizeShadingBuffer = 0;
  private static BufferedImage biShadingBuffer = null;
  private static Graphics2D g2ShadingBuffer = null;

  void drawScaledShadedAtom(Graphics2D g2, Image image,
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
    g2.setClip(xUpperLeft, yUpperLeft, size, size);
    g2.drawImage(biShadingBuffer, xUpperLeft, yUpperLeft, null);
    g2.setClip(null);
  }

  /**
   * Creates a shaded atom image.
   */
  private static Image sphereSetup(Component component, Color ballColor,
                                   int diameter, double[] lightSource) {

    double v1[] = new double[3];
    double v2[] = new double[3];
    int radius = (diameter + 1) / 2; // round it up
    int j = -1;

    // Create our own version of an IndexColorModel:
    int model[] = new int[diameter*diameter];

    // Normalize the lightsource vector:
    double[] lightsource = new double[3];
    for (int i = 0; i < 3; ++ i)
      lightsource[i] = lightSource[i];
    normalize(lightsource);
    for (int k1 = -(diameter - radius); k1 < radius; k1++) {
      for (int k2 = -(diameter - radius); k2 < radius; k2++) {
        j++;
        v1[0] = k2;
        v1[1] = k1;
        double len1 = Math.sqrt(k2 * k2 + k1 * k1);
        if (len1 <= radius) {
          int red2 = 0;
          int green2 = 0;
          int blue2 = 0;
          v1[2] = radius * Math.cos(Math.asin(len1 / radius));
          normalize(v1);
          double len2 = Math.abs((v1[0] * lightsource[0]
                         + v1[1] * lightsource[1] + v1[2] * lightsource[2]));
          if (len2 < 0.995f) {
            red2 = (int) (ballColor.getRed() * len2);
            green2 = (int) (ballColor.getGreen() * len2);
            blue2 = (int) (ballColor.getBlue() * len2);
          } else {
            v2[0] = lightsource[0] + 0.0f;
            v2[1] = lightsource[1] + 0.0f;
            v2[2] = lightsource[2] + 1.0f;
            normalize(v2);
            double len3 = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
            double len4 = 8.0f * len3 * len3 - 7.0f;
            double len5 = 100.0f * len4;
            len5 = Math.max(len5, 0.0f);
            red2 = (int) (ballColor.getRed() * 155 * len2 + 100.0 + len5);
            green2 = (int) (ballColor.getGreen() * 155 * len2 + 100.0 + len5);
            blue2 = (int) (ballColor.getBlue() * 155 * len2 + 100.0 + len5);
          }
          red2 = Math.min(red2 + 32, 255);
          green2 = Math.min(green2 + 32, 255);
          blue2 = Math.min(blue2 + 32, 255);


          // Bitwise masking to make model:
          model[j] = 0xff000000 | red2 << 16 | green2 << 8 | blue2;
        } else {
          model[j] = 0x00000000;
        }
      }
    }
    return component.createImage(new MemoryImageSource(diameter, diameter,
                                                       model, 0, diameter));
  }

  /**
   * normalizes the double[3] vector in place
   */
  private static void normalize(double v[]) {

    double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (Math.abs(len - 0.0) < java.lang.Double.MIN_VALUE) {
      v[0] = 0.0;
      v[1] = 0.0;
      v[2] = 0.0;
    } else {
      v[0] = v[0] / len;
      v[1] = v[1] / len;
      v[2] = v[2] / len;
    }
  }
}

