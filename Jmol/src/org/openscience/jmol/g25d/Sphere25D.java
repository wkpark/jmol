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
import java.awt.image.MemoryImageSource;
import java.awt.Color;

public class Sphere25D {

  DisplayControl control;
  Graphics25D g25d;
  public Sphere25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  public void render(int x, int y, int z, int diameter, Color color) {
    int[] sphere = getSphere(color, diameter);
    int width = g25d.width;
    int height = g25d.height;
    int offsetSrc = 0;
    int imageHeight = diameter;
    if (x >= 0 && x + diameter <= width && y >= 0 && y + diameter <= height) {
      do {
        g25d.plotPixelsUnclipped(sphere, offsetSrc, diameter, x, y, z);
        offsetSrc += diameter;
        ++y;
      } while (--imageHeight > 0);
    } else {
      do {
        g25d.plotPixelsClipped(sphere, offsetSrc, diameter, x, y, z);
        offsetSrc += diameter;
        ++y;
      } while (--imageHeight > 0);
    }
  }

  private static final double[] lightSource = { -1.0, -1.0, 2.0 };

  public int[] getSphere(Color color, int diameter) {
    return sphereSetup(color, diameter, lightSource);
  }

  private static int[] sphereSetup(Color colorSphere,
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
          int grn2 = 0;
          int blu2 = 0;
          v1[2] = radius * Math.cos(Math.asin(len1 / radius));
          normalize(v1);
          double len2 = Math.abs((v1[0] * lightsource[0]
                         + v1[1] * lightsource[1] + v1[2] * lightsource[2]));
          if (len2 < 0.995f) {
            red2 = (int) (colorSphere.getRed() * len2);
            grn2 = (int) (colorSphere.getGreen() * len2);
            blu2 = (int) (colorSphere.getBlue() * len2);
          } else {
            v2[0] = lightsource[0] + 0.0f;
            v2[1] = lightsource[1] + 0.0f;
            v2[2] = lightsource[2] + 1.0f;
            normalize(v2);
            double len3 = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
            double len4 = 8.0f * len3 * len3 - 7.0f;
            double len5 = 100.0f * len4;
            len5 = Math.max(len5, 0.0f);
            red2 = (int) (colorSphere.getRed()  * 155 * len2 + 100.0 + len5);
            grn2 = (int) (colorSphere.getGreen()* 155 * len2 + 100.0 + len5);
            blu2 = (int) (colorSphere.getBlue() * 155 * len2 + 100.0 + len5);
          }
          red2 = Math.min(red2 + 32, 255);
          grn2 = Math.min(grn2 + 32, 255);
          blu2 = Math.min(blu2 + 32, 255);


          // Bitwise masking to make model:
          model[j] = 0xff000000 | red2 << 16 | grn2 << 8 | blu2;
        } else {
          model[j] = 0x00000000;
        }
      }
    }
    return model;
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
