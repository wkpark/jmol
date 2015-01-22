/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.g3d;

class PixelatorShaded extends Pixelator {


  public int zSlab, zDepth, zShadeR, zShadeG, zShadeB;
  public int zShadePower = 3;

  /**
   * @param g 
   * @param p0 
   */
  PixelatorShaded(Graphics3D g, Pixelator p0) {
    super(g);
      zShadeR = g.bgcolor & 0xFF;
      zShadeG = (g.bgcolor & 0xFF00) >> 8;
      zShadeB = (g.bgcolor & 0xFF0000) >> 16;
      this.zSlab = g.zSlab < 0 ? 0 : g.zSlab;
      this.zDepth = g.zDepth < 0 ? 0 : g.zDepth;
      this.zShadePower = g.zShadePower;
      this.p0 = p0;
  }

  @Override
  void addPixel(int offset, int z, int p) {
    if (z > zDepth)
      return;
    if (z <= zDepth && z >= zSlab) {
      int pR = p & 0xFF;
      int pG = (p & 0xFF00) >> 8;
      int pB = (p & 0xFF0000) >> 16;
      int pA = (p & 0xFF000000);
      float f = (float)(zDepth - z) / (zDepth - zSlab);
      if (zShadePower > 1) {
        for (int i = 0; i < zShadePower; i++)
          f *= f;
      }
      pR = zShadeR + (int) (f * (pR - zShadeR));
      pG = zShadeG + (int) (f * (pG - zShadeG));
      pB = zShadeB + (int) (f * (pB - zShadeB));        
      p = (pB << 16) | (pG << 8) | pR | pA;
    }
    // important not to go directly to addPixel here for JavaScript avoidance of Java2Script SAEM method
    p0.addPixel(offset, z, p);
  }
}