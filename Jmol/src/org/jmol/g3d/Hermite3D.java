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

package org.jmol.g3d;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;
import javax.vecmath.Point3i;

class Hermite3D {

  Graphics3D g3d;

  Hermite3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  final Point3i[] pA = new Point3i[16];
  final Point3i[] pB = new Point3i[16];
  final float[] sA = new float[16];
  final float[] sB = new float[16];
  int sp;
  {
    for (int i = 16; --i >= 0; ) {
      pA[i] = new Point3i();
      pB[i] = new Point3i();
    }
  }

  void render(boolean tFill, short colix, int tension,
                     int diameterBeg, int diameterMid, int diameterEnd,
                     Point3i p0, Point3i p1, Point3i p2, Point3i p3) {
    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ((x2 - p0.x) * tension) / 8;
    int yT1 = ((y2 - p0.y) * tension) / 8;
    int zT1 = ((z2 - p0.z) * tension) / 8;
    int xT2 = ((p3.x - x1) * tension) / 8;
    int yT2 = ((p3.y - y1) * tension) / 8;
    int zT2 = ((p3.z - z1) * tension) / 8;
    sA[0] = 0;
    pA[0].set(p1);
    sB[0] = 1;
    pB[0].set(p2);
    sp = 0;
    int dDiameterFirstHalf = 0;
    int dDiameterSecondHalf = 0;
    if (tFill) {
      dDiameterFirstHalf = 2 * (diameterMid - diameterBeg);
      dDiameterSecondHalf = 2 * (diameterEnd - diameterMid);
    } else {
      g3d.setColix(colix);
    }
    do {
      Point3i a = pA[sp];
      Point3i b = pB[sp];
      int dx = b.x - a.x;
      int dy = b.y - a.y;
      int dist2 = dx*dx + dy*dy;
      if (dist2 <= 2) {
        // mth 2003 10 13
        // I tried drawing short cylinder segments here,
        // but drawing spheres was faster
        float s = sA[sp];
        if (tFill) {
          int d =(s < 0.5f
                  ? diameterBeg + (int)(dDiameterFirstHalf * s)
                  : diameterMid + (int)(dDiameterSecondHalf * (s - 0.5f)));
          g3d.fillSphereCentered(colix, d, a);
        } else {
          g3d.plotPixelClipped(a);
        }
        --sp;
      } else {
        double s = (sA[sp] + sB[sp]) / 2;
        double s2 = s * s;
        double s3 = s2 * s;
        double h1 = 2*s3 - 3*s2 + 1;
        double h2 = -2*s3 + 3*s2;
        double h3 = s3 - 2*s2 + s;
        double h4 = s3 - s2;
        Point3i pMid = pB[sp+1];
        pMid.x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
        pMid.y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
        pMid.z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
        pB[sp+1] = pB[sp];
        sB[sp+1] = sB[sp];
        pB[sp] = pMid;
        sB[sp] = (float)s;
        ++sp;
        pA[sp].set(pMid);
        sA[sp] = (float)s;
      }
    } while (sp >= 0);
  }
}
