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

import org.openscience.jmol.viewer.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Hermite3D {

  static int foo = 0;

  JmolViewer viewer;
  Graphics3D g3d;

  public Hermite3D(JmolViewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
  }

  final int[] xA = new int[16];
  final int[] yA = new int[16];
  final int[] zA = new int[16];
  final int[] dA = new int[16];
  final float[] sA = new float[16];
  final int[] xB = new int[16];
  final int[] yB = new int[16];
  final int[] zB = new int[16];
  final int[] dB = new int[16];
  final float[] sB = new float[16];
  int sp;

  public void render(short colix, int diameter1, int diameter2,
                     int x0, int y0, int z0, int x1, int y1, int z1,
                     int x2, int y2, int z2, int x3, int y3, int z3) {
    g3d.setColix(colix);
    int xT1 = (x2 - x0) * 7 / 8;
    int yT1 = (y2 - y0) * 7 / 8;
    int zT1 = (z2 - z0) * 7 / 8;
    int xT2 = (x3 - x1) * 7 / 8;
    int yT2 = (y3 - y1) * 7 / 8;
    int zT2 = (z3 - z1) * 7 / 8;

    sA[0] = 0;
    xA[0] = x1; yA[0] = y1; zA[0] = z1;
    g3d.fillSphereCentered(colix, diameter1, x1, y1, z1);
    sB[0] = 1;
    xB[0] = x2; yB[0] = y2; zB[0] = z2;
    g3d.fillSphereCentered(colix, diameter2, x2, y2, z2);
    int dDiameter = diameter2 - diameter1;
    sp = 0;
    do {
      int dx = xB[sp] - xA[sp];
      if (dx < 0) dx = -dx;
      int dy = yB[sp] - yA[sp];
      if (dy < 0) dy = -dy;
      if (dx < 2 && dy < 2) {
        --sp;
      } else {
        float s = (sA[sp] + sB[sp]) / 2;
        float s2 = s * s;
        float s3 = s2 * s;
        float h1 = 2*s3 - 3*s2 + 1;
        float h2 = -2*s3 + 3*s2;
        float h3 = s3 - 2*s2 + s;
        float h4 = s3 - s2;
        int x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
        int y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
        int z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
        int d = diameter1 + (int)(dDiameter * s);
        g3d.fillSphereCentered(colix, d, x, y, z);
        xB[sp+1] = xB[sp];
        yB[sp+1] = yB[sp];
        zB[sp+1] = zB[sp];
        sB[sp+1] = sB[sp];
        xB[sp] = x;
        yB[sp] = y;
        zB[sp] = z;
        sB[sp] = s;
        ++sp;
        xA[sp] = x;
        yA[sp] = y;
        zA[sp] = z;
        sA[sp] = s;
      }
    } while (sp >= 0);
  }
}
