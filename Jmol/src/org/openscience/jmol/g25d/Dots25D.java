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

import org.openscience.jmol.DisplayControl;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.awt.Color;
import java.util.Hashtable;

public class Dots25D {

  DisplayControl control;
  Graphics25D g25d;
  public Dots25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  final static int numLatitude = 16;
  final static int numLongitude = 16;

  float[] afDots;

  void init() {
    afDots = new float[(numLatitude + 1) * (numLongitude + 1 + 1)];
    int i = 0;
    for (double phi = 0.0; phi <= Math.PI; phi += Math.PI/numLatitude) {
      afDots[i++] = (float) (Math.cos(phi - Math.PI/2));
      System.out.println("phi=" + afDots[i - 1]);
      double r = Math.sin(phi);
      for (double theta = 0.0; theta <= Math.PI; theta+=Math.PI/numLongitude) {
        afDots[i++] = (float) (Math.cos(theta) * r / 2 + 0.5);
      }
    }
  }

  void render(Color color, int diameter, int x, int y, int z) {
    int argb = color.getRGB();
    double radius = diameter / 2.0;
    for (double phi = 0.0; phi <= Math.PI/2; phi += Math.PI/12) {
      double r = radius * Math.cos(phi);
      double yD = radius * Math.sin(phi);
      int yT = (int)(yD + (yD >= 0 ? 0.5 : -0.5));
      for (double theta = 0.0; theta <= Math.PI/2; theta+=Math.PI/12) {
        double xD = r * Math.sin(theta);
        int xT = (int)(xD + (xD >= 0 ? 0.5 : -0.5));
        int h = (int)(r * Math.cos(theta));
        g25d.plotPixelClipped(argb, x + xT, y + yT, z - h);
        g25d.plotPixelClipped(argb, x + xT, y - yT, z - h);
        g25d.plotPixelClipped(argb, x - xT, y + yT, z - h);
        g25d.plotPixelClipped(argb, x - xT, y - yT, z - h);
      }
    }
  }
}
