/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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


/**
 * renders triangles
 *<p>
 * currently only renders flat triangles
 *<p>
 * will probably need performance tuning
 *
 * @author Miguel, miguel@jmol.org
 */
class Triangle3D {

  Graphics3D g3d;

  int[] ax = new int[3];
  int[] ay = new int[3];
  int[] az = new int[3];
  
  Rgb16[] rgb16sGouraud;

  Triangle3D(Graphics3D g3d) {
    this.g3d = g3d;
    rgb16sGouraud = new Rgb16[3];
    for (int i = 3; --i >= 0; )
      rgb16sGouraud[i] = new Rgb16();
  }

  /*===============================================================
   * 2004 05 12 - mth
   * I have been working hard to get the triangles to render
   * correctly when lines are drawn only once.
   * the rules were :
   * a pixel gets drawn when
   * 1. it is to the left of a line
   * 2. it is under a horizontal line
   *
   * this generally worked OK, but failed on small skinny triangles
   * careful reading of Michael Abrash's book
   * Graphics Programming Black Book
   * Chapter 38, The Polygon Primeval, page 714
   * it says:
   *   Narrow wedges and one-pixel-wide polygons will show up spottily
   * I do not understand why this is the case
   * so, the triangle drawing now paints overlapping edges by one pixel
   *
   *==============================================================*/
  

  void fillTriangle(boolean translucent, boolean useGouraud) {
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
    int yMin = ay[iMinY];
    int yMid = ay[iMidY];
    int yMax = ay[iMaxY];
    int nLines = yMax - yMin + 1;
    if (nLines > axW.length)
      reallocRasterArrays(nLines);

    Rgb16[] gouraudW = useGouraud ? rgb16sW : null;
    Rgb16[] gouraudE = useGouraud ? rgb16sE : null;
    
    int dyMidMin = yMid - yMin;
    if (dyMidMin == 0) {
      // flat top
      if (ax[iMidY] < ax[iMinY])
        { int t = iMidY; iMidY = iMinY; iMinY = t; }
      generateRaster(nLines, iMinY, iMaxY, axW, azW, 0, gouraudW);
      generateRaster(nLines, iMidY, iMaxY, axE, azE, 0, gouraudE);
    } else if (yMid == yMax) {
      // flat bottom
      if (ax[iMaxY] < ax[iMidY])
        { int t = iMidY; iMidY = iMaxY; iMaxY = t; }
      generateRaster(nLines, iMinY, iMidY, axW, azW, 0, gouraudW);
      generateRaster(nLines, iMinY, iMaxY, axE, azE, 0, gouraudE);
    } else {
      int dxMaxMin = ax[iMaxY] - ax[iMinY];
      //int dzMaxMin = az[iMaxY] - az[iMinY];
      int roundFactor;
      roundFactor = nLines / 2;
      if (dxMaxMin < 0) roundFactor = -roundFactor;
      int axSplit = ax[iMinY] + (dxMaxMin * dyMidMin + roundFactor) / nLines;
      if (axSplit < ax[iMidY]) {
        generateRaster(nLines, iMinY, iMaxY, axW, azW, 0, gouraudW);
        generateRaster(dyMidMin, iMinY, iMidY, axE, azE, 0, gouraudE);
        generateRaster(nLines-dyMidMin, iMidY, iMaxY, axE, azE, dyMidMin,
                       gouraudE);
      } else {
        generateRaster(dyMidMin, iMinY, iMidY, axW, azW, 0, gouraudW);
        generateRaster(nLines-dyMidMin, iMidY, iMaxY, axW, azW, dyMidMin,
                       gouraudW);
        generateRaster(nLines, iMinY, iMaxY, axE, azE, 0, gouraudE);
      }
    }
    fillRaster(yMin, nLines, translucent, useGouraud);
  }

  private final static int DEFAULT = 64;

  int[] axW = new int[DEFAULT], azW = new int[DEFAULT];
  int[] axE = new int[DEFAULT], azE = new int[DEFAULT];
  Rgb16[] rgb16sW, rgb16sE;
  {
    rgb16sW = new Rgb16[DEFAULT];
    rgb16sE = new Rgb16[DEFAULT];
    for (int i = DEFAULT; --i >= 0; ) {
      rgb16sW[i] = new Rgb16();
      rgb16sE[i] = new Rgb16();
    }
  };

  void reallocRasterArrays(int n) {
    n = (n + 31) & ~31;
    axW = new int[n];
    azW = new int[n];
    axE = new int[n];
    azE = new int[n];
    rgb16sW = reallocRgb16s(rgb16sW, n);
    rgb16sE = reallocRgb16s(rgb16sE, n);
  }

  Rgb16[] reallocRgb16s(Rgb16[] rgb16s, int n) {
    Rgb16[] t = new Rgb16[n];
    System.arraycopy(rgb16s, 0, t, 0, rgb16s.length);
    for (int i = rgb16s.length; i < n; ++i)
      t[i] = new Rgb16();
    return t;
  }

  final Rgb16 rgb16t1 = new Rgb16();
  final Rgb16 rgb16t2 = new Rgb16();
  
  private final static boolean VERIFY = true;

  void generateRaster(int dy, int iN, int iS,
                      int[] axRaster, int[] azRaster, int iRaster,
                      Rgb16[] gouraud) {
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
    for (int y = 0, i = iRaster;
         y < dy;
         zCurrentScaled += zIncrementScaled, ++i, ++y) {
      axRaster[i] = xCurrent;
      azRaster[i] = zCurrentScaled >> 10;
      //      System.out.println("z=" + azRaster[y]);
      xCurrent += xMajorIncrement;
      errorTerm += xMajorError;
      if (errorTerm > 0) {
        xCurrent += xIncrement;
        errorTerm -= dy;
      }
    }
    if (gouraud != null) {
      Rgb16 rgb16Base = rgb16t1;
      rgb16Base.set(rgb16sGouraud[iN]);
      Rgb16 rgb16Increment = rgb16t2;
      rgb16Increment.diffDiv(rgb16sGouraud[iS], rgb16Base, dy);
      for (int i = iRaster, iMax = iRaster + dy; i < iMax; ++i)
        gouraud[i].setAndIncrement(rgb16Base, rgb16Increment);
      Rgb16 north = rgb16sGouraud[iN];
      Rgb16 generated = gouraud[iRaster];
      if (VERIFY) {
        if (north.getArgb() != generated.getArgb()) {
          System.out.println("north=" + north +
                             "\ngenerated=" + generated);
          throw new NullPointerException();
        }
        /*
        if (rgb16Base.getArgb() != rgb16sGouraud[iS].getArgb())
          throw new NullPointerException();
        */
      }
    }
  }

  void fillRaster(int y, int numLines, boolean tScreened, boolean useGouraud) {
    //    System.out.println("fillRaster("+y+","+numLines+","+paintFirstLine);
    int i = 0;
    if (y < 0) {
      numLines += y;
      i -= y;
      y = 0;
    }
    if (y + numLines > g3d.height)
      numLines = g3d.height - y;
    if (! useGouraud) {
      for ( ; --numLines >= 0; ++y, ++i) {
        int xW = axW[i];
        g3d.plotNoisyPixelsClipped(axE[i] - xW + 1, xW, y, azW[i], azE[i],
                                   tScreened);
      }
    } else {
      for ( ; --numLines >= 0; ++y, ++i) {
        int xW = axW[i];
        g3d.plotGouraudPixelsClipped(axE[i] - xW + 1, xW, y, azW[i], azE[i],
                                     rgb16sW[i], rgb16sE[i], tScreened);
      }
    }
  }

  void setGouraud(int rgbA, int rgbB, int rgbC) {
    rgb16sGouraud[0].set(rgbA);
    rgb16sGouraud[1].set(rgbB);
    rgb16sGouraud[2].set(rgbC);
  }
}
