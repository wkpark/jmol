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

import java.awt.Color;
import java.util.Hashtable;

public class Shade3D {

  public final static byte shadeAmbient = 0;
  public static final byte shadeNormal = 24;
  public static final byte shadeDarker = 16;
  public static final int shadeMax = 64;
  // the light source vector
  public static final float xLS = -1;
  public static final float yLS = -1;
  public static final float zLS = 1.5f;
  public static final float magnitudeLight =
    (float)Math.sqrt(xLS*xLS + yLS*yLS + zLS*zLS);
  // the light source vector normalized
  private static float xLight = xLS / magnitudeLight;
  private static float yLight = yLS / magnitudeLight;
  private static float zLight = zLS / magnitudeLight;

  // the viewer vector is always 0,0,1

  public static int exponentSpecular = 5;
  public static final float intensityDiffuseSource = 0.6f;
  public static final float intensitySpecularSource = 0.4f;
  public static final float intensityAmbient = 0;
  public final static float ambientFraction = 0.6f;
  public final static float ambientRange = 1 - ambientFraction;
  public final static float intenseFraction = 0.95f;

  Color color;
  int[] shades = new int[shadeMax];

  private Shade3D(Color color) {
    this.color = color;
    calcShades(color.getRGB());
  }

  private static Hashtable htCache = new Hashtable();

  public static Shade3D getShade(Color color) {
    Shade3D shade = (Shade3D) htCache.get(color);
    if (shade == null) {
      shade = new Shade3D(color);
      htCache.put(color, shade);
    }
    return shade;
  }

  public Shade3D getShade(int rgb) {
    return getShade(new Color(rgb));
  }

  public static int[] getShades(Color color) {
    Shade3D sp = (Shade3D) htCache.get(color);
    if (sp == null) {
      sp = new Shade3D(color);
      htCache.put(color, sp);
    }
    return sp.shades;
  }

  public static int[] getShades(int rgb) {
    return getShades(new Color(rgb));
  }

  private void calcShades(int rgb) {
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    shades[shadeNormal] = rgb(red, grn, blu);
    for (int i = 0; i < shadeNormal; ++i) {
      float fraction = ambientFraction + ambientRange*i/shadeNormal;
      shades[i] = rgb((int)(red*fraction + 0.5f),
                      (int)(grn*fraction + 0.5f),
                      (int)(blu*fraction + 0.5f));
    }

    int nSteps = shadeMax - shadeNormal - 1;
    float redRange = (255 - red) * intenseFraction;
    float grnRange = (255 - grn) * intenseFraction;
    float bluRange = (255 - blu) * intenseFraction;

    for (int i = 1; i <= nSteps; ++i) {
      shades[shadeNormal + i] = rgb(red + (int)(redRange * i / nSteps + 0.5f),
                                    grn + (int)(grnRange * i / nSteps + 0.5f),
                                    blu + (int)(bluRange * i / nSteps + 0.5f));
    }
  }

  private final static int rgb(int red, int grn, int blu) {
    return 0xFF000000 | (red << 16) | (grn << 8) | blu;
  }

  static String StringFromRgb(int rgb) {
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    return "[" + red + "," + grn + "," + blu + "]";
  }

  public String toString() {
    String str = "Shades for:" + StringFromRgb(shades[shadeNormal]) + "\n";
    for (int shade = 0; shade < shadeMax; ++shade)
      str += StringFromRgb(shades[shade]) + "\n";
    str += "\n";
    return str;
  }

  public static byte calcIntensity(float x, float y, float z) {
    float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
    return calcIntensityNormalized(x/magnitude, y/magnitude, z/magnitude);
  }

  public static byte calcIntensity(float x, float y, float z, float magnitude){
    return calcIntensityNormalized(x/magnitude, y/magnitude, z/magnitude);
  }

  public static byte calcIntensityNormalized(float x, float y, float z) {

    float cosTheta = x*xLight + y*yLight + z*zLight;
    float intensity = intensityAmbient; // ambient component
    if (cosTheta > 0) {
      intensity += cosTheta * intensityDiffuseSource; // diffuse component

      // this is the dot product of the reflection and the viewer
      // but the viewer only has a z component
      float dotProduct = z * 2 * cosTheta - zLight;
      if (dotProduct > 0) {
        for (int n = exponentSpecular; --n >= 0; )
          dotProduct *= dotProduct;
        intensity += dotProduct * intensitySpecularSource; // specular
      }
    }
    int shade = (int)(shadeMax * intensity + 0.5f);
    if (shade >= shadeMax) shade = shadeMax-1;
    return (byte)shade;
  }
}
