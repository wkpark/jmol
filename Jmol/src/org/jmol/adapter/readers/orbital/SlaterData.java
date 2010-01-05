/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-05-13 19:17:06 -0500 (Sat, 13 May 2006) $
 * $Revision: 5114 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.adapter.readers.orbital;

/*
 * -- cartesian bases only
 * 
 * Sincere thanks to Miroslav Kohout (DGRID) for helping me get this right
 *   -- Bob Hanson, 1/5/2010
 */

public class SlaterData {

  final static void scaleSlater(int[] idata, float[] fdata) {
    int ex = idata[1];
    int ey = idata[2];
    int ez = idata[3];
    int er = idata[4];
    int el = ex + ey + ez;
    int n = el + er + 1;
    float zeta = fdata[0];
    switch (el) {
    case 0: //S
      fdata[1] *= getSlaterConstS(n, zeta);
      break;
    case 1: //P
      fdata[1] *= getSlaterConstP(n, zeta);
      break;
    case 2: //D
    case 3: //F
      fdata[1] *= getSlaterConstCartesian(n, zeta, el, ex, ey, ez);
      break;
    }
  }

  private final static double _1_4pi = 0.25 / Math.PI;

  // (2n)! 
  private final static double[] fact1 = new double[] {
    1.0, 2.0, 24.0, 720.0, 40320.0, 362880.0, 87178291200.0 };
 
  //                                               x   0  1  2  3   4
  // (2x - 1)!!                                           s  p  d   f
  private final static double[] fact2 = new double[] { 1, 1, 3, 15, 105 };

  protected static double fact(double f, float zeta, int n) {
    return Math.pow(2 * zeta, n + 0.5) * Math.sqrt(f * _1_4pi / fact1[n]);
  }

  // generally valid
  private final static float getSlaterConstCartesian(int n, float zeta, int el, int x, int y, int z) {
    return (float) fact(fact2[el + 1] / fact2[x] / fact2[y] / fact2[z], zeta, n);
  }

  // shortcuts
  protected final static float getSlaterConstS(int n, float zeta) {
    return (float) fact(1, zeta, n);
  }

  protected final static float getSlaterConstP(int n, float zeta) {
    return (float) fact(3, zeta, n);
  }
  
}
