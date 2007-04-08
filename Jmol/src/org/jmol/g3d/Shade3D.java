/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.g3d;

//import org.jmol.util.Logger;

/**
 *<p>
 * Implements the shading of RGB values to support shadow and lighting
 * highlights.
 *</p>
 *<p>
 * Each RGB value has 64 shades. shade[0] represents ambient lighting.
 * shade[63] is white ... a full specular highlight.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final class Shade3D {

  // there are 64 shades of a given color
  // 0 = ambient
  // 63 = brightest ... white
  static final int shadeMax = 64;
  static final int shadeLast = shadeMax - 1;

  static byte shadeNormal = 52;

  // the light source vector
  static final float xLightsource = -1;
  static final float yLightsource = -1;
  static final float zLightsource = 2.5f;
  static final float magnitudeLight =
    (float)Math.sqrt(xLightsource * xLightsource +
                     yLightsource * yLightsource +
                     zLightsource * zLightsource);
  // the light source vector normalized
  static final float xLight = xLightsource / magnitudeLight;
  static final float yLight = yLightsource / magnitudeLight;
  static final float zLight = zLightsource / magnitudeLight;

  // the viewer vector is always 0,0,1

  // the following six settings are set in StateManager
  // and saved in g3d.lighting
  
  static int SPECULAR_ON = 0; // set specular on|off
  static int SPECULAR_PERCENT = 1;
  static int SPECULAR_EXPONENT = 2;
  static int SPECULAR_POWER = 3;
  static int DIFFUSE_PERCENT = 4;
  static int AMBIENT_PERCENT = 5;
  static int INTENSITY_SPECULAR = 6;
  static int INTENSE_FRACTION = 7;
  static int INTENSITY_DIFFUSE = 8;
  static int AMBIENT_FRACTION = 9;

  static int[] getShades(int rgb, boolean greyScale, float[] lighting) {
    int[] shades = new int[shadeMax];
    if (rgb == 0)
      return shades;
    
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;
    float ambientFraction = lighting[AMBIENT_FRACTION];
    float ambientRange = 1 - ambientFraction;
    float intenseFraction = lighting[INTENSE_FRACTION];
    
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
    if (greyScale)
      for (int i = shadeMax; --i >= 0; )
        shades[i] = Graphics3D.calcGreyscaleRgbFromRgb(shades[i]);
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

  final static byte intensitySpecularSurfaceLimit = (byte)(shadeNormal + 4);

  static byte calcIntensity(float x, float y, float z, float[] lighting) {
    // from Cylinder3D.calcArgbEndcap and renderCone
    // from Graphics3D.calcIntensity and calcIntensityScreen
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (byte)(calcFloatIntensityNormalized((float)(x/magnitude),
                                               (float)(y/magnitude),
                                               (float)(z/magnitude), lighting)
                  * shadeLast + 0.5f);
  }

  static byte calcIntensityNormalized(float x, float y, float z, float[] lighting) {
    //from Normix3D.setRotationMatrix
    return (byte)(calcFloatIntensityNormalized(x, y, z, lighting)
                  * shadeLast + 0.5f);
  }

  static int calcFp8Intensity(float x, float y, float z, float[] lighting) {
    //from calcDitheredNoisyIntensity (not utilized)
    //and Cylinder.calcRotatedPoint
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (int)(calcFloatIntensityNormalized((float)(x/magnitude),
                                              (float)(y/magnitude),
                                              (float)(z/magnitude), lighting)
                 * shadeLast * (1 << 8));
  }
/*
  static float calcFloatIntensity(float x, float y, float z, float[] lighting) {
    //not utilized
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return calcFloatIntensityNormalized((float)(x/magnitude),
                                        (float)(y/magnitude),
                                        (float)(z/magnitude), lighting);
  }
*/
  
  private static float calcFloatIntensityNormalized(float x, float y, float z, float[] lighting) {
    float cosTheta = x*xLight + y*yLight + z*zLight;
    float intensity = 0; // ambient component
    if (cosTheta > 0) {
      intensity += cosTheta * lighting[INTENSITY_DIFFUSE]; // diffuse component
      
      if (lighting[SPECULAR_ON] != 0) {
        // this is the dot product of the reflection and the viewer
        // but the viewer only has a z component
        float dotProduct = z * 2 * cosTheta - zLight;
        if (dotProduct > 0) {
          for (int n = (int)lighting[SPECULAR_EXPONENT]; --n >= 0 && dotProduct > .0001f; )
            dotProduct *= dotProduct;
          // specular component
          intensity += dotProduct * lighting[INTENSITY_SPECULAR];
        }
      }
    }
    if (intensity > 1)
      return 1;
    return intensity;
  }

  /*
   static byte calcDitheredNoisyIntensity(float x, float y, float z, float[] lighting) {
   //not utilized
   // add some randomness to prevent banding
   int fp8Intensity = calcFp8Intensity(x, y, z, lighting);
   int intensity = fp8Intensity >> 8;
   // this cannot overflow because the if the float intensity is 1.0
   // then intensity will be == shadeLast
   // but there will be no fractional component, so the next test will fail
   if ((fp8Intensity & 0xFF) > nextRandom8Bit())
   ++intensity;
   int random16bit = seed & 0xFFFF;
   if (random16bit < 65536 / 3 && intensity > 0)
   --intensity;
   else if (random16bit > 65536 * 2 / 3 && intensity < shadeLast)
   ++intensity;
   return (byte)intensity;
   }
   */

  static byte calcDitheredNoisyIntensity(float x, float y, float z, float r,
                                         float[] lighting) {
    // from Sphere3D only
    // add some randomness to prevent banding
    int fp8Intensity = (int) (calcFloatIntensityNormalized(x / r, y / r, z / r,
        lighting)
        * shadeLast * (1 << 8));
    int intensity = fp8Intensity >> 8;
    // this cannot overflow because the if the float intensity is 1.0
    // then intensity will be == shadeLast
    // but there will be no fractional component, so the next test will fail
    if ((fp8Intensity & 0xFF) > nextRandom8Bit())
      ++intensity;
    int random16bit = seed & 0xFFFF;
    if (random16bit < 65536 / 3 && intensity > 0)
      --intensity;
    else if (random16bit > 65536 * 2 / 3 && intensity < shadeLast)
      ++intensity;
    return (byte) intensity;
  }

  /*
    This is a linear congruential pseudorandom number generator,
    as defined by D. H. Lehmer and described by Donald E. Knuth in
    The Art of Computer Programming,
    Volume 2: Seminumerical Algorithms, section 3.2.1.

  static long seed = 1;
  static int nextRandom8Bit() {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    //    return (int)(seed >>> (48 - bits));
    return (int)(seed >>> 40);
  }
  */

  // this doesn't really need to be synchronized
  // no serious harm done if two threads write seed at the same time
  private static int seed = 0x12345679; // turn lo bit on
  /**
   *<p>
   * Implements RANDU algorithm for random noise in lighting/shading.
   *</p>
   *<p>
   * RANDU is the classic example of a poor random number generator.
   * But it is very cheap to calculate and is good enough for our purposes.
   *</p>
   *
   * @return Next random
   */
  static int nextRandom8Bit() {
    int t = seed;
    seed = t = ((t << 16) + (t << 1) + t) & 0x7FFFFFFF;
    return t >> 23;
  }

  /*
  static void setLightsourceZ(float z) {
    zLightsource = z;
    magnitudeLight =
      (float)Math.sqrt(xLightsource * xLightsource +
                       yLightsource * yLightsource +
                       zLightsource * zLightsource);
    //dump();
  }
*/
  
}
