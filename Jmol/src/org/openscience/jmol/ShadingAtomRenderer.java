
/*
 * Copyright 2002 The Jmol Development Team
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
package org.openscience.jmol;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

/**
 * Draws atoms as shaded circles colored by the type of atom.
 * The shading gives a pseudo-3D look to the atom.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ShadingAtomRenderer implements AtomRenderer {

  /**
   * The Component where all atoms will be drawn. This reference will be used for
   * creating and displaying the images for shaded atom spheres.
   */
  private static Component imageComponent;

  /**
   * Pool of atom images for shaded renderings.
   */
  private static Hashtable ballImages = new Hashtable();

  /**
   * Draws an atom on a particular graphics context.
   *
   * @param gc the Graphics context
   * @param atom the atom to be drawn
   * @param picked whether this atom is picked
   * @param settings the display settings
   */
  public void paint(Graphics gc, Atom atom, boolean picked,
      DisplaySettings settings) {
    
    ColorProfile colorProfile;
    if (settings.getAtomColorProfile() == DisplaySettings.ATOMCHARGE) {
        colorProfile = new ChargeColorProfile();
    } else {
        colorProfile = new DefaultColorProfile();
    }
    Color atomColor = colorProfile.getColor(atom);

    int x = atom.screenX;
    int y = atom.screenY;
    int z = atom.screenZ;
    int diameter =
      (int) (2.0f
        * settings.getCircleRadius(z, atom.getType().getVdwRadius()));
    int radius = diameter >> 1;

    if (picked) {
      int halo = radius + 5;
      int halo2 = 2 * halo;
      gc.setColor(settings.getPickedColor());
      gc.fillOval(x - halo, y - halo, halo2, halo2);
    }
    
    Image shadedImage = null;
    if (ballImages.containsKey(atomColor)) {
      shadedImage = (Image) ballImages.get(atomColor);
    } else {
      shadedImage = sphereSetup(atomColor, settings);
      ballImages.put(atomColor, shadedImage);
    }
    gc.drawImage(shadedImage, x - radius, y - radius, diameter, diameter,
        imageComponent);
  }
  
  /**
   * Sets the Component where all atoms will be drawn.
   *
   * @param c the Component
   */
  public static void setImageComponent(Component c) {
    imageComponent = c;
  }

  /**
   * Creates a shaded atom image.
   *
   * @param ballColor color for the sphere.
   * @param settings the display settings used for the light source.
   * @return an image of a shaded sphere.
   */
  private static Image sphereSetup(Color ballColor,
      DisplaySettings settings) {

    float v1[] = new float[3];
    float v2[] = new float[3];
    byte b = 40;
    int i = 2 * b + 1;
    int j = -1;

    // Create our own version of an IndexColorModel:
    int model[] = new int[i * i];

    // Normalize the lightsource vector:
    float[] lightsource = normalize(settings.getLightSourceVector());
    for (int k1 = -b; k1 <= b; k1++) {
      for (int k2 = -b; k2 <= b; k2++) {
        j++;
        v1[0] = k2;
        v1[1] = k1;
        float len1 = (float) Math.sqrt(k2 * k2 + k1 * k1);
        if (len1 <= b) {
          int red2 = 0;
          int green2 = 0;
          int blue2 = 0;
          v1[2] = b * (float) Math.cos(Math.asin(len1 / b));
          v1 = normalize(v1);
          float len2 = (float) Math.abs((double) (v1[0] * lightsource[0]
                         + v1[1] * lightsource[1] + v1[2] * lightsource[2]));
          if (len2 < 0.995f) {
            red2 = (int) ((float) ballColor.getRed() * len2);
            green2 = (int) ((float) ballColor.getGreen() * len2);
            blue2 = (int) ((float) ballColor.getBlue() * len2);
          } else {
            v2[0] = lightsource[0] + 0.0f;
            v2[1] = lightsource[1] + 0.0f;
            v2[2] = lightsource[2] + 1.0f;
            v2 = normalize(v2);
            float len3 = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
            float len4 = 8.0f * len3 * len3 - 7.0f;
            float len5 = 100.0f * len4;
            len5 = Math.max(len5, 0.0f);
            red2 = (int) ((float) (ballColor.getRed() * 155) * len2 + 100.0
                + len5);
            green2 = (int) ((float) (ballColor.getGreen() * 155) * len2
                + 100.0 + len5);
            blue2 = (int) ((float) (ballColor.getBlue() * 155) * len2 + 100.0
                + len5);
            red2 = Math.min(red2, 255);
            green2 = Math.min(green2, 255);
            blue2 = Math.min(blue2, 255);
          }

          // Bitwise masking to make model:
          model[j] = 0xff000000 | red2 << 16 | green2 << 8 | blue2;
        } else {
          model[j] = 0x00000000;
        }
      }
    }
    Image result = imageComponent.createImage(new MemoryImageSource(i, i, model, 0,
        i));
    return result;
  }

  /**
                                                                                                                                                                                                                                                                   * Returns a normalized vector for the float[3] given.
                                                                                                                                                                                                                                                                   */
  private static float[] normalize(float v[]) {

    float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    float v2[] = new float[3];
    if (Math.abs(len - 0.0) < Double.MIN_VALUE) {
      v2[0] = 0.0f;
      v2[1] = 0.0f;
      v2[2] = 0.0f;
    } else {
      v2[0] = v[0] / len;
      v2[1] = v[1] / len;
      v2[2] = v[2] / len;
    }
    return v2;
  }
}

