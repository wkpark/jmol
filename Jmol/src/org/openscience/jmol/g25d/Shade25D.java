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

import java.awt.Color;
import java.util.Hashtable;

public class Shade25D {

  public final static byte shadeAmbient = 0;
  public static final byte shadeNormal = 24;
  public static final byte shadeDarker = 16;
  public static final int shadeMax = 64;
  public static final float[] vectLightSource = {-1, -1, 1.5f};
  public static final float[] vectViewer = {0, 0, 1};
  public static int exponentSpecular = 5;
  public static final float intensityDiffuseSource = 0.6f;
  public static final float intensitySpecularSource = 0.4f;
  public static final float intensityAmbient = 0;
  public final static float ambientFraction = 0.6f;
  public final static float ambientRange = 1 - ambientFraction;
  public final static float intenseFraction = 0.95f;
  static boolean tInitialized;
  
  static synchronized void initialize() {
    if (tInitialized)
      return;
    normalize(vectLightSource);
    initializeCache();
    tInitialized = true;
  }

  static final int minNormalCache = -5;
  static final int maxNormalCache = 5;
  static final int numCache = maxNormalCache - minNormalCache + 1;
  // z is positive only
  // x & y are over full range
  static final int numCache2 = numCache * numCache;
  static final byte[] shadeCache = new byte[(maxNormalCache + 1) * numCache2];

  static void initializeCache() {
    for (int z = 0; z <= maxNormalCache; ++z)
      for (int y = minNormalCache; y <= maxNormalCache; ++y)
        for (int x = minNormalCache; x <= maxNormalCache; ++x) {
          int offsetCache =
            z * numCache2 +
            (y-minNormalCache) * numCache +
            (x-minNormalCache);
          shadeCache[offsetCache] = calcIntensity(x, y, z);
        }
  }

  Color color;
  int[] shades = new int[shadeMax];

  private Shade25D(Color color) {
    this.color = color;
    calcShades(color.getRGB());
  }

  private static Hashtable htCache = new Hashtable();

  public static Shade25D getShade(Color color) {
    Shade25D shade = (Shade25D) htCache.get(color);
    if (shade == null) {
      shade = new Shade25D(color);
      htCache.put(color, shade);
    }
    return shade;
  }

  public Shade25D getShade(int rgb) {
    return getShade(new Color(rgb));
  }

  public static int[] getShades(Color color) {
    Shade25D sp = (Shade25D) htCache.get(color);
    if (sp == null) {
      sp = new Shade25D(color);
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

  public static byte getIntensity(int x1,int y1,int z1,int x2,int y2,int z2) {
    return getIntensity(x2 - x1, y2 - y1, z2 - z1);
  }


  public static byte getIntensity(int x, int y, int z) {
    if (z < 0)
      return shadeAmbient; // this isn't really true, but we never see back surfaces
    if (x <= maxNormalCache && x >= minNormalCache &&
        y <= maxNormalCache && y >= minNormalCache &&
        z <= maxNormalCache && z >= minNormalCache) {
      int offsetCache =
        z * numCache2 +
        (y-minNormalCache) * numCache +
        (x-minNormalCache);
      return shadeCache[offsetCache];
    }
    return calcIntensity(x, y, z);
  }

  static final float[] vectNormal = new float[3];
  static final float[] vectTemp = new float[3];
  static final float[] vectReflection = new float[3];

  public static synchronized byte calcIntensity(float x, float y, float z) {

    vectNormal[0] = x;
    vectNormal[1] = y;
    vectNormal[2] = z;
    normalize(vectNormal);
    float cosTheta = dotProduct(vectNormal, vectLightSource);
    float intensitySpecular = 0;
    float intensityDiffuse = 0;
    if (cosTheta > 0) {
      intensityDiffuse = cosTheta * intensityDiffuseSource;
      scale(2 * cosTheta, vectNormal, vectTemp);
      sub(vectTemp, vectLightSource, vectReflection);
 
      float dpRV = dotProduct(vectReflection, vectViewer);
      if (dpRV < 0)
        dpRV = 0;
      for (int n = exponentSpecular; --n >= 0; )
        dpRV *= dpRV;
      intensitySpecular = dpRV * intensitySpecularSource;
    }
    float intensity =
      intensitySpecular + intensityDiffuse + intensityAmbient;
    int shade = (int)(shadeMax * intensity + 0.5f);
    if (shade >= shadeMax) shade = shadeMax-1;
    return (byte)shade;
  }


  private static void normalize(float v[]) {

    float mag = (float)Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (Math.abs(mag - 0) <= java.lang.Float.MIN_VALUE) {
      v[0] = 0;
      v[1] = 0;
      v[2] = 0;
    } else {
      v[0] = v[0] / mag;
      v[1] = v[1] / mag;
      v[2] = v[2] / mag;
    }
  }

  private static float dotProduct(float[] v1, float[] v2) {
    return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
  }

  private static void scale(float a, float[] v, float[] vResult) {
    vResult[0] = a * v[0];
    vResult[1] = a * v[1];
    vResult[2] = a * v[2];
  }

  private static void add(float[] v1, float[] v2, float[] vSum) {
    vSum[0] = v1[0] + v2[0];
    vSum[1] = v1[1] + v2[1];
    vSum[2] = v1[2] + v2[2];
  }

  private static void sub(float[] v1, float[] v2, float[] vDiff) {
    vDiff[0] = v1[0] - v2[0];
    vDiff[1] = v1[1] - v2[1];
    vDiff[2] = v1[2] - v2[2];
  }
}
