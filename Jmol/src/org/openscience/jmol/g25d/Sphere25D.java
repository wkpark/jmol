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
import java.util.Hashtable;

public class Sphere25D {

  DisplayControl control;
  Graphics25D g25d;
  public Sphere25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  void paintSphereShape(int x, int y, int z, int diameter, Color color) {
    int argb = color.getRGB();
    x -= (diameter + 1) / 2;
    y -= (diameter + 1) / 2;
    SphereShape ss = SphereShape.get(diameter);
    byte[] intensities = ss.intensities;
    byte[] heights = ss.heights;
    int[] shades = Shade25D.getShades(argb);
    int offset = 0;
    for (int i = 0; i < diameter; ++i) 
      for (int j = 0; j < diameter; ++j) {
        int intensity = intensities[offset];
        if (intensity >= 0)
          g25d.plotPixelClipped(shades[intensity],
                                x+i, y+j, z - heights[offset]);
        ++offset;
      }
  }
}

class SphereShape {
  int diameter;
  byte[] intensities;
  byte[] heights;

  static final double[] lightSource = {-1, -1, 1.5};
  static int exponentSpecular = 5;
  static double intensityDiffuseSource = .6;
  static double intensitySpecularSource = .8;
  static double intensityAmbient = .5;

  static final int intensityNormal = Shade25D.shadeNormal;
  static final int intensityMax = Shade25D.shadeMax;

  private SphereShape(int diameter) {
    this.diameter = diameter;
    initialize(diameter, intensityNormal, intensityMax, lightSource);
  }

  static int maxSphereCache = 64;
  static SphereShape[] sphereShapeCache = new SphereShape[maxSphereCache];

  static SphereShape get(int diameter) {
    SphereShape ss;
    if (diameter > maxSphereCache)
      diameter = maxSphereCache;
    --diameter;
    ss = sphereShapeCache[diameter];
    if (ss != null)
      return ss;
    ss = sphereShapeCache[diameter] = new SphereShape(diameter + 1);
    return ss;
  }

  void initialize(int diameter, int intensityNormal, int intensityMax,
                  double[] lightsource) {
    double vectNormal[] = new double[3];
    double vectReflection[] = new double[3];
    double vectTemp[] = new double[3];
    double vectViewer[] = {0, 0, 1};
    double radius = diameter / 2.0;
    int offset = 0;

    intensities = new byte[diameter*diameter];
    heights = new byte[diameter*diameter];

    // Normalize the vectLightSource vector:
    double[] vectLightSource = new double[3];
    for (int i = 0; i < 3; ++ i)
      vectLightSource[i] = lightsource[i];
    normalize(vectLightSource);
    double k1 = -radius + .5;
    for (int i = 0; i < diameter; ++i, ++k1) {
      double k2 = -radius + .5;
      for (int j = 0; j < diameter; ++j, ++k2) {
        vectNormal[0] = k2;
        vectNormal[1] = k1;
        double len1 = Math.sqrt(k2 * k2 + k1 * k1);
        if (len1 <= radius) {
          vectNormal[2] = radius * Math.cos(Math.asin(len1 / radius));
          int zheight = (int)(vectNormal[2] + .5);
          normalize(vectNormal);
          double cosTheta = dotProduct(vectNormal, vectLightSource);
          double intensitySpecular = 0.0;
          double intensityDiffuse = 0.0;
          if (cosTheta > 0) {
            intensityDiffuse = cosTheta * intensityDiffuseSource;

            scale(2 * cosTheta, vectNormal, vectTemp);
            sub(vectTemp, vectLightSource, vectReflection);

            double dpRV = dotProduct(vectReflection, vectViewer);
            if (dpRV < 0.0)
              dpRV = 0.0;
            // dpRV = Math.pow(dpRV, 25);
            for (int n = exponentSpecular; --n >= 0; )
              dpRV *= dpRV;
            intensitySpecular = dpRV * intensitySpecularSource;
          }
          double intensity =
            intensitySpecular + intensityDiffuse + intensityAmbient;
          int shade = (int)(intensityNormal * intensity);
          if (shade >= intensityMax) shade = intensityMax-1;
          intensities[offset] = (byte)shade;
          heights[offset] = (byte)zheight;
        } else {
          intensities[offset] = -1;
          heights[offset] = -1;
        }
        ++offset;
      }
    }
  }

  private void normalize(double v[]) {

    double mag = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (Math.abs(mag - 0.0) < java.lang.Double.MIN_VALUE) {
      v[0] = 0.0;
      v[1] = 0.0;
      v[2] = 0.0;
    } else {
      v[0] = v[0] / mag;
      v[1] = v[1] / mag;
      v[2] = v[2] / mag;
    }
  }

  private double dotProduct(double[] v1, double[] v2) {
    return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
  }

  private void scale(double a, double[] v, double[] vResult) {
    vResult[0] = a * v[0];
    vResult[1] = a * v[1];
    vResult[2] = a * v[2];
  }

  private void add(double[] v1, double[] v2, double[] vSum) {
    vSum[0] = v1[0] + v2[0];
    vSum[1] = v1[1] + v2[1];
    vSum[2] = v1[2] + v2[2];
  }

  private void sub(double[] v1, double[] v2, double[] vDiff) {
    vDiff[0] = v1[0] - v2[0];
    vDiff[1] = v1[1] - v2[1];
    vDiff[2] = v1[2] - v2[2];
  }

}
