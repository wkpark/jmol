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

public class Cylinder25D {

  DisplayControl control;
  Graphics25D g25d;
  public Cylinder25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  void paintCylinderShape(int x1, int y1, int z1, int x2, int y2, int z2,
                          int diameter, Color color) {
    int x = (x1 + x2) / 2;
    int y = (y1 + y2) / 2;
    int z = (z2 + z2) / 2;
    int[] shades = Shade25D.getShades(color);
    int intensity = getIntensity(x1, y1, z1, x2, y2, z2);
    Color shaded = new Color(shades[intensity]);
    g25d.fillCircleCentered(shaded, shaded, x, y, z, diameter);
  }


  int getIntensity(int x1, int y1, int z1, int x2, int y2, int z2) {
    // Normalize the vectLightSource vector:
    double[] vectLightSource = new double[3];
    for (int i = 0; i < 3; ++ i)
      vectLightSource[i] = Sphere25D.lightSource[i];
    normalize(vectLightSource);

    double[] vectNormal = new double[3];
    double[] vectTemp = new double[3];
    double[] vectReflection = new double[3];
    double vectViewer[] = {0, 0, 1};

    vectNormal[0] = x2 - x1;
    vectNormal[1] = y2 - y1;
    vectNormal[2] = z2 - z1;
    normalize(vectNormal);
    double cosTheta = dotProduct(vectNormal, vectLightSource);
    double intensitySpecular = 0.0;
    double intensityDiffuse = 0.0;
    if (cosTheta > 0) {
      intensityDiffuse = cosTheta * Sphere25D.intensityDiffuseSource;
      scale(2 * cosTheta, vectNormal, vectTemp);
      sub(vectTemp, vectLightSource, vectReflection);
 
      double dpRV = dotProduct(vectReflection, vectViewer);
      if (dpRV < 0.0)
        dpRV = 0.0;
      // dpRV = Math.pow(dpRV, 25);
      for (int n = Sphere25D.exponentSpecular; --n >= 0; )
        dpRV *= dpRV;
      intensitySpecular = dpRV * Sphere25D.intensitySpecularSource;
    }
    double intensity =
      intensitySpecular + intensityDiffuse + Sphere25D.intensityAmbient;
    int shade = (int)(Sphere25D.intensityNormal * intensity);
    if (shade >= Sphere25D.intensityMax) shade = Sphere25D.intensityMax-1;
    return shade;
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
