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

package org.openscience.jmol.g3d;

import org.openscience.jmol.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Triangle3D {

  DisplayControl control;
  Graphics3D g3d;

  int ax[] = new int[4];
  int ay[] = new int[4];
  int az[] = new int[4];

  public Triangle3D(DisplayControl control, Graphics3D g3d) {
    this.control = control;
    this.g3d = g3d;
  }

  /****************************************************************
   * FIXME mth 2003 08 01
   * this needs to be reworked
   * lots of problems with errors caused by integer arithmetic
   * for now, all shapes will be drawn with lines as well as being
   * filled. that will get the edges right. 
   ****************************************************************/
  

  void fillTriangle() {
    int iMinY = 0;
    if (ay[1] < ay[0]) iMinY = 1;
    if (ay[2] < ay[iMinY]) iMinY = 2;
    int iMidY = (iMinY + 1) % 3;
    int iMaxY = (iMinY + 2) % 3;
    if (ay[iMidY] > ay[iMaxY]) { int t = iMidY; iMidY = iMaxY; iMaxY = t; }
    /*
    System.out.println("----fillTriangle\n" +
                       " iMinY=" + iMinY + " iMidY=" + iMidY +
                       " iMaxY=" + iMaxY + "\n" +
                       "  minY=" + ax[iMinY] + "," + ay[iMinY] + "\n" +
                       "  midY=" + ax[iMidY] + "," + ay[iMidY] + "\n" +
                       "  maxY=" + ax[iMaxY] + "," + ay[iMaxY] + "\n");
    */
    if (ay[iMinY] < ay[iMidY]) {
      // there is an upper triangle
      if (ay[iMidY] == ay[iMaxY]) {
        fillUpper(iMinY, iMidY, iMaxY);
        return;
      }
      int dxMax = ax[iMaxY] - ax[iMinY];
      int dyMax = ay[iMaxY] - ay[iMinY];
      int dzMax = az[iMaxY] - az[iMinY];
      int dyMid = ay[iMidY] - ay[iMinY];
      int roundX = (dxMax < 0) ? -dyMax/2 : dyMax/2;
      int roundZ = (dzMax < 0) ? -dyMax/2 : dyMax/2;
      ax[3] = ax[iMinY] + (dxMax * dyMid + roundX) / dyMax;
      ay[3] = ay[iMidY];
      az[3] = az[iMinY] + (dzMax * dyMid + roundZ) / dyMax;
      fillUpper(iMinY, iMidY, 3);
      iMinY = 3;
    }
    fillLower(iMinY, iMidY, iMaxY);
  }

  int[] axLeft = new int[32], azLeft = new int[32];
  int[] axRight = new int[32], azRight = new int[32];

  void reallocRasterArrays(int n) {
    n = (n + 31) & ~31;
    axLeft = new int[n];
    azLeft = new int[n];
    axRight = new int[n];
    azRight = new int[n];
  }

  void fillUpper(int iTop, int iLeft, int iRight) {
    if (ax[iLeft] > ax[iRight]) { int t = iLeft; iLeft = iRight; iRight = t; }
    /*
    System.out.println("fillUpper\n" +
                       "   top=" + ax[iTop] + "," + ay[iTop] + "\n" +
                       "  left=" + ax[iLeft] + "," + ay[iLeft] + "\n" +
                       " right=" + ax[iRight] + "," + ay[iRight] + "\n");
    */
    int nLines = ay[iLeft] - ay[iTop] + 1;
    if (nLines > axLeft.length)
      reallocRasterArrays(nLines);
    generateRasterPoints(nLines, iTop, iLeft, axLeft, azLeft);
    generateRasterPoints(nLines, iTop, iRight, axRight, azRight);
    fillRaster(ay[iTop], nLines);
  }

  void fillLower(int iLeft, int iRight, int iBottom) {
    if (ax[iLeft] > ax[iRight]) { int t = iLeft; iLeft = iRight; iRight = t; }
    /*
    System.out.println("fillLower\n" +
                       "  left=" + ax[iLeft] + "," + ay[iLeft] + "\n" +
                       " right=" + ax[iRight] + "," + ay[iRight] + "\n" +
                       "bottom=" + ax[iBottom] + "," + ay[iBottom] + "\n");
    */
    int nLines = ay[iBottom] - ay[iLeft] + 1;
    if (nLines > axLeft.length)
      reallocRasterArrays(nLines);
    generateRasterPoints(nLines, iLeft, iBottom, axLeft, azLeft);
    generateRasterPoints(nLines, iRight, iBottom, axRight, azRight);
    fillRaster(ay[iLeft], nLines);
  }

  void generateRasterPoints(int dy, int iTop, int iBot,
                            int[] axRaster, int[] azRaster) {
    int xTop = ax[iTop], zTop = az[iTop];
    int xBot = ax[iBot], zBot = az[iBot];
    int dx = xBot - xTop, dz = zBot - zTop;
    /*
    System.out.println("xTop=" + xTop +
                       " xBot=" + xBot + " dy=" + dy
                       );
    */
    axRaster[0] = xTop;
    azRaster[0] = zTop;

    int dyMinus1 = dy - 1;
    if (dyMinus1 == 0)
      return;
    int zCurrentScaled = zTop << 10;
    int roundingFactor = dyMinus1; if (dz < 0) roundingFactor = -roundingFactor;

    int zIncrementScaled =((dz << 10) + roundingFactor) / dyMinus1;

    roundingFactor = dyMinus1 / 2; if (dx < 0) roundingFactor = -roundingFactor;

    for (int y = 1; y < dy; ++y) {
      int x = xTop + ((dx * y) + roundingFactor) / dyMinus1;
      axRaster[y] = x;
      zCurrentScaled += zIncrementScaled;
      azRaster[y] = zCurrentScaled >> 10;
    }
  }

  void fillRaster(int y, int dy) {
    int i = 0;
    if (y < 0) {
      dy += y;
      i -= y;
      y = 0;
    }
    while (--dy >= 0) {
      if (y >= g3d.height)
        return;
      int xLeft = axLeft[i];
      if (xLeft < 0)
        xLeft = 0;
      int xRight = axRight[i];
      if (xRight >= g3d.width)
        xRight = g3d.width - 1;
      int nPix = xRight - xLeft;
      if (nPix > 0) {
        // FIXME mth 2003 06 09
        // z is not correct
        // needs to change from azLeft[i] to azRight[i]
        g3d.plotPixelsUnclipped(nPix, xLeft, y, azLeft[i]);
      }
      ++y;
      ++i;
    }
  }
}
