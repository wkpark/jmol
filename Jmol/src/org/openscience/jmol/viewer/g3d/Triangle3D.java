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

import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Triangle3D {

  JmolViewer viewer;
  Graphics3D g3d;

  int ax[] = new int[4];
  int ay[] = new int[4];
  int az[] = new int[4];

  public Triangle3D(JmolViewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
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
                       "  minY="+ax[iMinY]+","+ay[iMinY]+","+az[iMinY]+"\n" +
                       "  midY="+ax[iMidY]+","+ay[iMidY]+","+az[iMidY]+"\n" +
                       "  maxY="+ax[iMaxY]+","+ay[iMaxY]+","+az[iMaxY]+"\n");
    */
    if (ay[iMinY] < ay[iMidY]) {
      // there is an upper triangle
      if (ay[iMidY] == ay[iMaxY]) {
        fillUpper(iMinY, iMidY, iMaxY);
        return;
      }
      int dxMaxMin = ax[iMaxY] - ax[iMinY];
      int dyMaxMin = ay[iMaxY] - ay[iMinY];
      int dzMaxMin = az[iMaxY] - az[iMinY];
      int dyMidMin = ay[iMidY] - ay[iMinY];
      int roundFactor;
      roundFactor = dyMaxMin / 2;
      if (dxMaxMin < 0) roundFactor = -roundFactor;
      ax[3] = ax[iMinY] + (dxMaxMin * dyMidMin + roundFactor) / dyMaxMin;
      ay[3] = ay[iMidY];
      roundFactor = dyMaxMin / 2;
      if (dzMaxMin < 0) roundFactor = -roundFactor;
      az[3] = az[iMinY] + (dzMaxMin * dyMidMin + roundFactor) / dyMaxMin;
      fillUpper(iMinY, iMidY, 3);
      iMinY = 3;
    }
    fillLower(iMinY, iMidY, iMaxY);
  }

  int[] axW = new int[32], azW = new int[32];
  int[] axE = new int[32], azE = new int[32];

  void reallocRasterArrays(int n) {
    n = (n + 31) & ~31;
    axW = new int[n];
    azW = new int[n];
    axE = new int[n];
    azE = new int[n];
  }

  void fillUpper(int iN, int iW, int iE) {
    if (ax[iW] > ax[iE]) { int t = iW; iW = iE; iE = t; }

    /*
    System.out.println("fillUpper\n" +
                       "N="+ax[iN]+"," + ay[iN] + "," + az[iN] + "\n" +
                       "W="+ax[iW]+"," + ay[iW] + "," + az[iW] + "\n" +
                       "E="+ax[iE]+"," + ay[iE] + "," + az[iE] + "\n");
    */
    int nLines = ay[iW] - ay[iN];
    if (nLines == 0)
      return;
    if (nLines > axW.length)
      reallocRasterArrays(nLines);
    generateRaster(nLines, iN, iW, axW, azW);
    generateRaster(nLines, iN, iE, axE, azE);
    fillRaster(ay[iN], nLines, false);
  }
  
  void fillLower(int iW, int iE, int iS) {
    if (ax[iW] > ax[iE]) { int t = iW; iW = iE; iE = t; }
    
    /*
    System.out.println("fillLower\n" +
                       "W="+ax[iW]+","+ay[iW]+","+az[iW]+"\n" +
                       "E="+ax[iE]+","+ay[iE]+","+az[iE]+"\n" +
                       "S="+ax[iS]+","+ay[iS]+","+az[iS]+"\n");
    */
    int nLines = ay[iS] - ay[iW];
    if (nLines == 0)
      return;
    if (nLines > axW.length)
      reallocRasterArrays(nLines);
    generateRaster(nLines, iW, iS, axW, azW);
    generateRaster(nLines, iE, iS, axE, azE);
    fillRaster(ay[iW], nLines, true);
  }

  void generateRaster(int dy, int iN, int iS,
                      int[] axRaster, int[] azRaster) {
    /*
    System.out.println("generateRaster\n" +
                       "N="+ax[iN]+","+ay[iN]+","+az[iN]+"\n" +
                       "S="+ax[iS]+","+ay[iS]+","+az[iS]+"\n");
    */
    int xN = ax[iN], zN = az[iN];
    int xS = ax[iS], zS = az[iS];
    int dx = xS - xN, dz = zS - zN;
    int xCurrent = xN;
    int xIncrement, width, errorTerm;
    if (dx >= 0) {
      xIncrement = 1;
      width = dx;
      errorTerm = 0;
    } else {
      xIncrement = -1;
      width = -dx;
      errorTerm = -dy + 1;
    }

    /*
    System.out.println("xN=" + xN + " xS=" + xS + " dy=" + dy + " dz=" + dz);
    */
    int zCurrentScaled = (zN << 10) + (1 << 9);
    int roundingFactor;
    roundingFactor = dy/2; if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled =((dz << 10) + roundingFactor) / dy;

    int xMajorIncrement;
    int xMajorError;
    if (dy >= width) {
      xMajorIncrement = 0;
      xMajorError = width;
    } else {
      xMajorIncrement = dx / dy;
      xMajorError = width % dy;
    }
    for (int y = 0; y < dy; ++y, zCurrentScaled += zIncrementScaled) {
      axRaster[y] = xCurrent;
      azRaster[y] = zCurrentScaled >> 10;
      //      System.out.println("z=" + azRaster[y]);
      xCurrent += xMajorIncrement;
      errorTerm += xMajorError;
      if (errorTerm > 0) {
        xCurrent += xIncrement;
        errorTerm -= dy;
      }
    }
  }

  void fillRaster(int y, int numLines, boolean paintFirstLine) {
    //    System.out.println("fillRaster("+y+","+numLines+","+paintFirstLine);
    int i = 0;
    if (y < 0) {
      numLines += y;
      i -= y;
      y = 0;
    } else if (! paintFirstLine) {
      --numLines;
      ++y;
      ++i;
    }
    if (y + numLines > g3d.height)
      numLines = g3d.height - y;
    for ( ; --numLines >= 0; ++y, ++i) {
      int xW = axW[i];
      g3d.plotPixelsClipped(g3d.argbCurrent,
                            axE[i] - xW, xW, y, azW[i], azE[i]);
    }
  }
}
