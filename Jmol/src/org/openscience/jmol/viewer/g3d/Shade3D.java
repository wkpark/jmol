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

import javax.vecmath.Vector3f;

public final class Shade3D {

  // there are 64 shades of a given color
  // 0 = ambient
  // 63 = brightest ... white
  public static final int shadeMax = 64;

  private static byte shadeNormal = 48;

  // the light source vector
  static final float xLightsource = -1;
  static final float yLightsource = -1;
  static float zLightsource = 1.5f;
  static float magnitudeLight =
    (float)Math.sqrt(xLightsource * xLightsource +
                     yLightsource * yLightsource +
                     zLightsource * zLightsource);
  // the light source vector normalized
  static float xLight = xLightsource / magnitudeLight;
  static float yLight = yLightsource / magnitudeLight;
  static float zLight = zLightsource / magnitudeLight;

  // the viewer vector is always 0,0,1

  static boolean specularOn = true;
  static int specularExponent = 6;
  static float intenseFraction = 0.4f;
  static float intensitySpecular = 0.4f;
  static float intensityDiffuse = 0.6f;
  static float ambientFraction = 0.4f;

  public static int[] getShades(int rgb) {
    int[] shades = new int[shadeMax];

    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    float ambientRange = 1 - ambientFraction;

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
    return shades;
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

  /*
  public static byte calcIntensity(float x, float y, float z) {
    float magnitude = (float)Math.sqrt(x*x + y*y + z*z);
    return calcIntensityNormalized(x/magnitude, y/magnitude, z/magnitude);
  }

  public static byte calcIntensity(float x, float y, float z, float magnitude) {
    return calcIntensityNormalized(x/magnitude, y/magnitude, z/magnitude);
  }
  */

  public final static int intensitySpecularSurfaceLimit = 48;

  public static int calcIntensityNormalized(float x, float y, float z) {
    float cosTheta = x*xLight + y*yLight + z*zLight;
    float intensity = 0; // ambient component
    if (cosTheta > 0) {
      intensity += cosTheta * intensityDiffuse; // diffuse component
      
      if (specularOn) {
        // this is the dot product of the reflection and the viewer
        // but the viewer only has a z component
        float dotProduct = z * 2 * cosTheta - zLight;
        if (dotProduct > 0) {
          for (int n = specularExponent; --n >= 0; )
            dotProduct *= dotProduct;
          intensity += dotProduct * intensitySpecular; // specular
        }
      }
    }
    int shade = (int)(shadeMax * intensity + 0.5f);
    if (shade >= shadeMax) shade = shadeMax-1;
    return (byte)shade;
  }

  public static void setSpecular(boolean specular) {
    specularOn = specular;
    dump();
  }

  public static boolean getSpecular() {
    return specularOn;
  }

  public static void setLightsourceZ(float z) {
    zLightsource = z;
    magnitudeLight =
      (float)Math.sqrt(xLightsource * xLightsource +
                       yLightsource * yLightsource +
                       zLightsource * zLightsource);
    dump();
  }

  public static void setSpecularPower(int specularPower) {
    if (specularPower >= 0)
      intenseFraction = specularPower / 100f;
    else
      specularExponent = -specularPower;
    dump();
  }

  public static void setAmbientPercent(int ambientPercent) {
    ambientFraction = ambientPercent / 100f;
    dump();
  }

  public static void setDiffusePercent(int diffusePercent) {
    intensityDiffuse = diffusePercent / 100f;
    dump();
  }

  public static void setSpecularPercent(int specularPercent) {
    intensitySpecular = specularPercent / 100f;
    dump();
  }

  static void dump() {
    System.out.println("\n ambientPercent=" + ambientFraction +
                       "\n diffusePercent=" + intensityDiffuse +
                       "\n specularOn=" + specularOn +
                       "\n specularPercent=" + intensitySpecular +
                       "\n specularPower=" + intenseFraction +
                       "\n specularExponent=" + specularExponent +
                       "\n zLightsource=" + zLightsource +
                       "\n shadeNormal=" + shadeNormal);
  }
}
