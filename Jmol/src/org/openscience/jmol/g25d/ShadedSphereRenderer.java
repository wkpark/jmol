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

import org.openscience.jmol.*;

import java.awt.Component;
//import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.awt.Color;

public class ShadedSphereRenderer {

  DisplayControl control;
  Graphics25D g25d;
  SphereG2D sphereG2D;
  public ShadedSphereRenderer(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
    if (control.jvm12orGreater)
      sphereG2D = new SphereG2D();
  }

  static final int minCachedSize = 2;
  static final int maxCachedSize = 100;
  static final int scalableSize = 47;
  static final int maxSmoothedSize = 200;
  // I am getting severe graphical artifacts around the edges when
  // rendering hints are turned on. Therefore, I am adding a margin
  // to shaded rendering in order to cut down on edge effects
  static final int artifactMargin = 4;
  
  public void render(int xUpperLeft, int yUpperLeft, int z,
                      int diameter, Color color, Color outline) {
    if (diameter < minCachedSize) {
      if (diameter == 1) {
        g25d.setColor(outline);
        g25d.drawPixel(xUpperLeft, yUpperLeft, z);
      }
      return;
    }
    Image[] shadedImages = (Image[]) control.imageCache.get(color);
    if (shadedImages == null)
      shadedImages = initializeShadedSphereCache(control, color);

    if (diameter < maxCachedSize) {
      Image sphere = shadedImages[diameter];
      if (sphere == null)
        sphere = loadShadedSphereCache(control, color, shadedImages, diameter);
      int margin = control.getUseGraphics2D() ? 1 : 0;
      g25d.drawImage(sphere, xUpperLeft - margin, yUpperLeft - margin, z);
      return;
    }
    Image imgSphere = shadedImages[0];
    if (! control.getUseGraphics2D()) {
      g25d.drawImage(imgSphere, xUpperLeft, yUpperLeft, z,
                  diameter, diameter);
    } else if (diameter < maxSmoothedSize) {
      sphereG2D.drawSphereG2D(g25d, imgSphere,
                              xUpperLeft - artifactMargin,
                              yUpperLeft - artifactMargin,
                              diameter, artifactMargin);
    } else {
      sphereG2D.drawClippedSphereG2D(g25d, imgSphere,
                                     xUpperLeft, yUpperLeft, diameter);
    }
  }

  private static final double[] lightSource = { -1.0f, -1.0f, 2.0f };
  private Image[] initializeShadedSphereCache(DisplayControl control,
                                              Color color) {
    Image shadedImages[] = new Image[maxCachedSize];
    Component component = control.getAwtComponent();
    control.imageCache.put(color, shadedImages);
    shadedImages[0] = sphereSetup(component, color, scalableSize, lightSource);
    return shadedImages;
  }
  
  private Image loadShadedSphereCache(DisplayControl control, Color color,
                                      Image[] shadedImages, int diameter) {
    return shadedImages[diameter] =
      control.getUseGraphics2D()
      ? sphereG2D.imageSphereG2D(shadedImages[0], diameter)
      : sphereSetup(control.getAwtComponent(), color, diameter, lightSource);
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
