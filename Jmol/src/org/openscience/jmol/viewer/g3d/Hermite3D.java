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
  final float[] sA = new float[16];
  final int[] xB = new int[16];
  final int[] yB = new int[16];
  final int[] zB = new int[16];
  final float[] sB = new float[16];
  int sp;

  public void render(short colix, int diameterBeg, int diameterMid, int diameterEnd,
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
    sB[0] = 1;
    xB[0] = x2; yB[0] = y2; zB[0] = z2;
    sp = 0;
    int dDiameterFirstHalf = 2 * (diameterMid - diameterBeg);
    int dDiameterSecondHalf = 2 * (diameterEnd - diameterMid);
    do {
      int dx = xB[sp] - xA[sp];
      int dy = yB[sp] - yA[sp];
      int dist2 = dx*dx + dy*dy;
      if (dist2 <= 2) {
        // mth 2003 10 13
        // I tried drawing short cylinder segments here,
        // but drawing spheres was faster
        float s = sA[sp];
        int d =(s < 0.5
                ? diameterBeg + (int)(dDiameterFirstHalf * s)
                : diameterMid + (int)(dDiameterSecondHalf * (s - 0.5)));
        g3d.fillSphereCentered(colix, d, xA[sp], yA[sp], zA[sp]);
        --sp;
      } else {
        double s = (sA[sp] + sB[sp]) / 2;
        double s2 = s * s;
        double s3 = s2 * s;
        double h1 = 2*s3 - 3*s2 + 1;
        double h2 = -2*s3 + 3*s2;
        double h3 = s3 - 2*s2 + s;
        double h4 = s3 - s2;
        int x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
        int y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
        int z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
        xB[sp+1] = xB[sp];
        yB[sp+1] = yB[sp];
        zB[sp+1] = zB[sp];
        sB[sp+1] = sB[sp];
        xB[sp] = x;
        yB[sp] = y;
        zB[sp] = z;
        sB[sp] = (float)s;
        ++sp;
        xA[sp] = x;
        yA[sp] = y;
        zA[sp] = z;
        sA[sp] = (float)s;
      }
    } while (sp >= 0);
  }
}
