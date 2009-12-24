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
  private static final float xLightsource = -1;
  private static final float yLightsource = -1;
  private static final float zLightsource = 2.5f;
  private static final float magnitudeLight =
    (float)Math.sqrt(xLightsource * xLightsource +
                     yLightsource * yLightsource +
                     zLightsource * zLightsource);
  // the light source vector normalized
  static final float xLight = xLightsource / magnitudeLight;
  static final float yLight = yLightsource / magnitudeLight;
  static final float zLight = zLightsource / magnitudeLight;
  
  // the viewer vector is always 0,0,1

  /*  
  static boolean specularOn = true;
  // set specular 0-100
  static float intensitySpecular = 0.22f;
  // set specpower -6
  static int specularExponent = 6;
  // set specpower 0-100
  static float intenseFraction = 0.4f;
  // set diffuse 0-100
  static float intensityDiffuse = 0.84f;
  // set ambient 0-100
  static float ambientFraction = 0.45f;
  
  */

  //ones user sets:
  static final int SPECULAR_ON = 0; // set specular on|off
  static final int SPECULAR_PERCENT = 1;  // divide by 100 to get INTENSITY_SPECULAR
  static final int SPECULAR_POWER = 2;    // divide by 100 to get INTESITY_FRACTION
  static final int DIFFUSE_PERCENT = 3;  
  static final int AMBIENT_PERCENT = 4;
  //ones we actually use here:
  static final int SPECULAR_EXPONENT = 5; // log_2(PHONG_EXPONENT)
  static final int PHONG_EXPONENT = 6;
  static final int SPECULAR_FRACTION = 7; // <-- specularPercent
  static final int INTENSE_FRACTION = 8;  // <-- specularPower
  static final int DIFFUSE_FRACTION = 9; 
  static final int AMBIENT_FRACTION = 10;
  static final int USE_PHONG = 11;

  final static float[] lighting = new float[] {
      //user set:
      1f,       // specularON
      22f,      // specularPercent
      40f,      // specularPower
      84f,      // diffusePercent
      45f,      // ambientPercent
      //
      6f,       // specularExponent
      64f,      // phongExponent
      //derived:
      0.22f,    // intensitySpecular
      0.4f,     // intense fraction
      0.84f,    // intensity diffuse
      0.45f,    // ambient fraction
      0,        // use phong
      }; 
  
  static int[] getShades(int rgb, boolean greyScale) {
    int[] shades = new int[shadeMax];
    if (rgb == 0)
      return shades;
    shades[shadeNormal] = rgb;    
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;
    
    float ambientFraction = lighting[AMBIENT_FRACTION];
    float ambientRange = 1 - ambientFraction;
    float intenseFraction = lighting[INTENSE_FRACTION];
    
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

  final static int rgb(int red, int grn, int blu) {
    return 0xFF000000 | (red << 16) | (grn << 8) | blu;
  }

  /*
  static String StringFromRgb(int rgb) {
    // [red,green,blue]
    return "[" + ((rgb >> 16) & 0xFF) + "," + ((rgb >> 8) & 0xFF) + ","
        + (rgb & 0xFF) + "]";
  }
  */

  final static byte intensitySpecularSurfaceLimit = (byte)(shadeNormal + 4);

  static byte calcIntensity(float x, float y, float z) {
    // from Cylinder3D.calcArgbEndcap and renderCone
    // from Graphics3D.calcIntensity and calcIntensityScreen
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (byte)(calcFloatIntensityNormalized((float)(x/magnitude),
                                               (float)(y/magnitude),
                                               (float)(z/magnitude))
                  * shadeLast + 0.5f);
  }

  static byte calcIntensityNormalized(float x, float y, float z) {
    //from Normix3D.setRotationMatrix
    return (byte)(calcFloatIntensityNormalized(x, y, z)
                  * shadeLast + 0.5f);
  }

  static int calcFp8Intensity(float x, float y, float z) {
    //from calcDitheredNoisyIntensity (not utilized)
    //and Cylinder.calcRotatedPoint
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (int)(calcFloatIntensityNormalized((float)(x/magnitude),
                                              (float)(y/magnitude),
                                              (float)(z/magnitude))
                 * shadeLast * (1 << 8));
  }

  /*
   * static float calcFloatIntensity(float x, float y, float z) { //not utilized
   * double magnitude = Math.sqrt(x*x + y*y + z*z); return
   * calcFloatIntensityNormalized((float)(x/magnitude), (float)(y/magnitude),
   * (float)(z/magnitude)); }
   */

  private static float calcFloatIntensityNormalized(float x, float y, float z) {
    float NdotL = x * xLight + y * yLight + z * zLight;
    if (NdotL <= 0)
      return 0;
    // I = k_diffuse * f_diffuse + k_specular * f_specular
    // where
    // k_diffuse = (N dot L)
    // k_specular = {[(2(N dot L)N - L] dot V}^p
    //
    // and in our case V = {0 0 1} so the z component of that is:
    // 
    // k_specular = ( 2 * NdotL * z - zLight )^p
    // 
    // HOWEVER -- Jmol's "specularExponent is 2^phongExponent
    //
    // "specularExponent" phong_exponent
    // 0 1
    // 1 2
    // 2 4
    // 3 8
    // 4 16
    // 5 32
    // 5.322 40
    // 6 64
    // 7 128
    // 8 256
    // 9 512
    // 10 1024
    float intensity = NdotL * lighting[DIFFUSE_FRACTION];
    if (lighting[SPECULAR_ON] != 0) {
      float k_specular = 2 * NdotL * z - zLight;
      if (k_specular > 0) {
        if (lighting[USE_PHONG] != 0) {
          k_specular = (float) Math.pow(k_specular, lighting[PHONG_EXPONENT]);
        } else {
          for (int n = (int) lighting[SPECULAR_EXPONENT]; --n >= 0
              && k_specular > .0001f;)
            k_specular *= k_specular;
        }
        intensity += k_specular * lighting[SPECULAR_FRACTION];
      }
    }
    if (intensity > 1)
      return 1;
    return intensity;
  }

  /*
   static byte calcDitheredNoisyIntensity(float x, float y, float z) {
   //not utilized
   // add some randomness to prevent banding
   int fp8Intensity = calcFp8Intensity(x, y, z);
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

  static byte calcDitheredNoisyIntensity(float x, float y, float z, float r) {
    // from Sphere3D only
    // add some randomness to prevent banding
    int fp8Intensity = (int) (calcFloatIntensityNormalized(x / r, y / r, z / r)
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

  
  ////////////////////////////////////////////////////////////////
  // Sphere shading cache for Large spheres
  ////////////////////////////////////////////////////////////////

  static boolean sphereShadingCalculated = false;
  final static byte[] sphereIntensities = new byte[256 * 256];

  synchronized static void calcSphereShading() {
    //if (!sphereShadingCalculated) { //unnecessary -- but be careful!
    float xF = -127.5f;
    for (int i = 0; i < 256; ++xF, ++i) {
      float yF = -127.5f;
      for (int j = 0; j < 256; ++yF, ++j) {
        byte intensity = 0;
        float z2 = 130 * 130 - xF * xF - yF * yF;
        if (z2 > 0) {
          float z = (float) Math.sqrt(z2);
          intensity = calcDitheredNoisyIntensity(xF, yF, z, 130);
        }
        sphereIntensities[(j << 8) + i] = intensity;
      }
    }
    sphereShadingCalculated = true;
  }
  
  /*
  static byte calcSphereIntensity(int x, int y, int r) {
    int d = 2*r + 1;
    x += r;
    if (x < 0)
      x = 0;
    int x8 = (x << 8) / d;
    if (x8 > 0xFF)
      x8 = 0xFF;
    y += r;
    if (y < 0)
      y = 0;
    int y8 = (y << 8) / d;
    if (y8 > 0xFF)
      y8 = 0xFF;
    return sphereIntensities[(y8 << 8) + x8];
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
