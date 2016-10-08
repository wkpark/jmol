/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.g3d;


//import org.jmol.util.Logger;
import org.jmol.api.JmolRendererInterface;
import org.jmol.util.GData;
import javajs.util.P3;
import javajs.util.P3i;

import org.jmol.util.Rgb16;

/**
 * renders triangles
 *<p>
 * currently only renders flat triangles
 *<p>
 * will probably need performance tuning
 *
 * @author Miguel, miguel@jmol.org
 */
public class TriangleRenderer extends PrecisionRenderer implements G3DRenderer {

  private Graphics3D g3d;

  private final static int DEFAULT = 64;
  private int[] ax = new int[3], ay = new int[3], az = new int[3];
  private float[] aa = new float[DEFAULT], bb = new float[DEFAULT];
  private int[] axW = new int[DEFAULT], azW = new int[DEFAULT];
  private int[] axE = new int[DEFAULT], azE = new int[DEFAULT];

  private Rgb16[] rgb16sW, rgb16sE;
  private Rgb16[] rgb16sGouraud;
  
  //private final static boolean VERIFY = false;

  public TriangleRenderer() {    
  }
  
  @Override
  public G3DRenderer set(JmolRendererInterface g3d, GData gdata) {
    try {
      rgb16sW = new Rgb16[DEFAULT];
      rgb16sE = new Rgb16[DEFAULT];
      for (int i = DEFAULT; --i >= 0;) {
        rgb16sW[i] = new Rgb16();
        rgb16sE[i] = new Rgb16();
      }
      this.g3d = (Graphics3D) g3d;
      rgb16sGouraud = new Rgb16[3];
      for (int i = 3; --i >= 0;)
        rgb16sGouraud[i] = new Rgb16();
    } catch (Exception e) {
      // must be export; not a problem
    }

    return this;
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

  private P3[] abc = new P3[3];

  void setGouraud(int rgbA, int rgbB, int rgbC) {
    rgb16sGouraud[0].setInt(rgbA);
    rgb16sGouraud[1].setInt(rgbB);
    rgb16sGouraud[2].setInt(rgbC);
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
   * note added 10/20/2012 -- Bob Hanson
   * 
   *   These problems were addressed and fixed in 
   *   revision (to Triangle3D) 7086 and 7148 back in 2007.
   *   http://jmol.svn.sourceforge.net/viewvc/jmol/trunk/Jmol/src/org/jmol/g3d/Triangle3D.java?r1=7084&r2=7086&pathrev=17476
   *
   *==============================================================*/

  void fillTriangleXYZ(int xScreenA, int yScreenA, int zScreenA, int xScreenB,
                    int yScreenB, int zScreenB, int xScreenC, int yScreenC,
                    int zScreenC, boolean useGouraud) {
    ax[0] = xScreenA;
    ax[1] = xScreenB;
    ax[2] = xScreenC;
    ay[0] = yScreenA;
    ay[1] = yScreenB;
    ay[2] = yScreenC;
    az[0] = zScreenA;
    az[1] = zScreenB;
    az[2] = zScreenC;
    abc[0] = null;
    fillTriangleB(useGouraud);
  }

  void fillTriangleP3i(P3i screenA, P3i screenB, P3i screenC,
                    boolean useGouraud) {
    ax[0] = screenA.x;
    ax[1] = screenB.x;
    ax[2] = screenC.x;
    ay[0] = screenA.y;
    ay[1] = screenB.y;
    ay[2] = screenC.y;
    az[0] = screenA.z;
    az[1] = screenB.z;
    az[2] = screenC.z;
    abc[0] = null;
    fillTriangleB(useGouraud);
  }

  void fillTriangleP3f(P3 screenA, P3 screenB, P3 screenC, boolean useGouraud,
                       boolean isPrecise) {
    
    //System.out.println("draw @{point(" + screenA + ",false)} @{point(" + screenB + ",false)} @{point(" + screenC + ",false)}");
    ax[0] = Math.round(screenA.x);
    ax[1] = Math.round(screenB.x);
    ax[2] = Math.round(screenC.x);
    ay[0] = Math.round(screenA.y);
    ay[1] = Math.round(screenB.y);
    ay[2] = Math.round(screenC.y);
    az[0] = Math.round(screenA.z);
    az[1] = Math.round(screenB.z);
    az[2] = Math.round(screenC.z);
    if (isPrecise) {
      abc[0] = screenA;
      abc[1] = screenB;
      abc[2] = screenC;
    } else {
      abc[0] = null;
    }
    fillTriangleB(useGouraud);
  }

//  void fillTriangleP3if(P3i screenA, P3i screenB, P3i screenC,
//                    float factor, boolean useGouraud) {
//    ax[0] = screenA.x;
//    ax[1] = screenB.x;
//    ax[2] = screenC.x;
//    ay[0] = screenA.y;
//    ay[1] = screenB.y;
//    ay[2] = screenC.y;
//    az[0] = screenA.z;
//    az[1] = screenB.z;
//    az[2] = screenC.z;
//    adjustVertex(ax, factor);
//    adjustVertex(ay, factor);
//    adjustVertex(az, factor);
//    fillTriangleB(useGouraud);
//  }
//
//  private static void adjustVertex(int[] t, float factor) {
//    float av = (t[0] + t[1] + t[2]) / 3f;
//    for (int i = 0; i < 3; i++)
//      t[i] += factor * (av - t[i]);
//  }

  private void fillTriangleB(boolean useGouraud) {
    if (az[0] <= 1 || az[1] <= 1 || az[2] <= 1)
      return;
    int cc0 = g3d.clipCode3(ax[0], ay[0], az[0]);
    int cc1 = g3d.clipCode3(ax[1], ay[1], az[1]);
    int cc2 = g3d.clipCode3(ax[2], ay[2], az[2]);
//    System.out.println("tri " + ax[0]  + " " + ay[0]  + " " + az[0]);
//    System.out.println("tri " + ax[1]  + " " + ay[1]  + " " + az[1]);
//    System.out.println("tri " + ax[2]  + " " + ay[2]  + " " + az[2]);

    int c = (cc0 | cc1 | cc2);
    boolean isClipped = (c != 0);
    if (isClipped) {
      if (c == -1 || (cc0 & cc1 & cc2) != 0) {
        // all three corners are being clipped on the same dimension
        return;
      }
    }
    boolean isPrecise = (abc[0] != null);
    int iMinY = 0;
    if (ay[1] < ay[iMinY])
      iMinY = 1;
    if (ay[2] < ay[iMinY])
      iMinY = 2;
    int iMidY = (iMinY + 1) % 3;
    int iMaxY = (iMinY + 2) % 3;
    if (ay[iMidY] > ay[iMaxY]) {
      int t = iMidY;
      iMidY = iMaxY;
      iMaxY = t;
    }
    int yMin = ay[iMinY];
    int yMid = ay[iMidY];
    int yMax = ay[iMaxY];
    int nLines = yMax - yMin + 1;
    if (nLines > g3d.height * 3)
      return;
    if (nLines > axW.length) {
      int n = (nLines + 31) & ~31;
      axW = new int[n];
      azW = new int[n];
      axE = new int[n];
      azE = new int[n];
      if (isPrecise) {
        aa = new float[n];
        bb = new float[n];
      }
      rgb16sW = reallocRgb16s(rgb16sW, n);
      rgb16sE = reallocRgb16s(rgb16sE, n);
    }
    Rgb16[] gouraudW, gouraudE;
    if (useGouraud) {
      gouraudW = rgb16sW;
      gouraudE = rgb16sE;
    } else {
      gouraudW = gouraudE = null;
    }
    int dyMidMin = yMid - yMin;
    if (dyMidMin == 0) {
      // flat top
      if (ax[iMidY] < ax[iMinY]) {
        int t = iMidY;
        iMidY = iMinY;
        iMinY = t;
      }
      /*  
         min ------- mid
           \         /
          A \       / B
           \ \     / /
              \   /
               max
      */
      generateRaster(nLines, iMinY, iMaxY, axW, azW, aa, isPrecise, 0, gouraudW);
      generateRaster(nLines, iMidY, iMaxY, axE, azE, bb, isPrecise, 0, gouraudE);
    } else if (yMid == yMax) {
      // flat bottom
      if (ax[iMaxY] < ax[iMidY]) {
        int t = iMidY;
        iMidY = iMaxY;
        iMaxY = t;
      }
      /*
       *       min
       *      /   \
       *   A /     \ B
       *  / /       \ \
       *   /         \
       *  mid ------ max
       */
      generateRaster(nLines, iMinY, iMidY, axW, azW, aa, isPrecise, 0, gouraudW);
      generateRaster(nLines, iMinY, iMaxY, axE, azE, bb, isPrecise, 0, gouraudE);
    } else {
      int dxMaxMin = ax[iMaxY] - ax[iMinY];
      int roundFactor;
      roundFactor = GData.roundInt(nLines / 2);
      if (dxMaxMin < 0)
        roundFactor = -roundFactor;
      int axSplit = ax[iMinY] + (dxMaxMin * dyMidMin + roundFactor) / nLines;
      if (axSplit < ax[iMidY]) {
        /*
         *       min
         *      /   \ B
         *   A /     \ \  
         *  / /      mid 
         *   /     C
         *  max   / 
         */

        // Trick is that we need to overlap so as to generate the IDENTICAL
        // raster on each segment, but then we always throw out the FIRST raster
        generateRaster(nLines, iMinY, iMaxY, axW, azW, aa, isPrecise, 0, gouraudW);
        generateRaster(dyMidMin + 1, iMinY, iMidY, axE, azE, bb, isPrecise, 0, gouraudE);
        generateRaster(nLines - dyMidMin, iMidY, iMaxY, axE, azE, bb, isPrecise, dyMidMin,
            gouraudE);

      } else {

        /*
         *       min
         *      /   \ C
         *   A /     \ \  
         *  / /       \ 
         *   /         \
         *  mid         \ 
         *       B->    max
         */

        generateRaster(dyMidMin + 1, iMinY, iMidY, axW, azW, aa, isPrecise, 0, gouraudW);
        generateRaster(nLines - dyMidMin, iMidY, iMaxY, axW, azW, aa, isPrecise, dyMidMin,
            gouraudW);
        generateRaster(nLines, iMinY, iMaxY, axE, azE, bb, isPrecise, 0, gouraudE);
      }
    }
    g3d.setZMargin(5);
    int pass2Row = g3d.pass2Flag01;
    int pass2Off = 1 - pass2Row;
    int xW;
    int i = 0;    
    if (yMin < 0) {
      nLines += yMin;
      i -= yMin;
      yMin = 0;
    }
    if (yMin + nLines > g3d.height)
      nLines = g3d.height - yMin;
    if (useGouraud) {
      // so far no precision here
      if (isClipped) {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pixelCount > 0)
            g3d.plotPixelsClippedRaster(pixelCount, xW, yMin, azW[i], azE[i], rgb16sW[i], rgb16sE[i]);
        }
      } else {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pass2Row == 1 && pixelCount < 0) {
            /*
             * The issue here is that some very long, narrow triangles can be skipped
             * altogether because axE < axW.
             * 
             */

            pixelCount = 1;
            xW--;
          }
          if (pixelCount > 0)
            g3d.plotPixelsUnclippedRaster(pixelCount, xW, yMin, azW[i], azE[i], rgb16sW[i], rgb16sE[i]);
        }
      }
    } else if (isPrecise) {
      if (isClipped) {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pixelCount > 0)
            g3d.plotPixelsClippedRasterBits(pixelCount, xW, yMin, azW[i], azE[i], null, null, aa[i], bb[i]);
        }
      } else {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pass2Row == 1 && pixelCount < 0) {
            /*
             * The issue here is that some very long, narrow triangles can be skipped
             * altogether because axE < axW.
             * 
             */

            pixelCount = 1;
            xW--;
          }
          if (pixelCount > 0)
            g3d.plotPixelsUnclippedRasterBits(pixelCount, xW, yMin, null, null, aa[i], bb[i]);
        }
      }
    } else {
      if (isClipped) {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pixelCount > 0)
            g3d.plotPixelsClippedRaster(pixelCount, xW, yMin, azW[i], azE[i], null, null);
        }
      } else {
        for (; --nLines >= pass2Row; ++yMin, ++i) {
          int pixelCount = axE[i] - (xW = axW[i]) + pass2Off;
          if (pass2Row == 1 && pixelCount < 0) {
            /*
             * The issue here is that some very long, narrow triangles can be skipped
             * altogether because axE < axW.
             * 
             */

            pixelCount = 1;
            xW--;
          }
          if (pixelCount > 0)
            g3d.plotPixelsUnclippedRaster(pixelCount, xW, yMin, azW[i], azE[i], null, null);
        }
      }
    }
    g3d.setZMargin(0);
  }
 
  private void generateRaster(int dy, int iN, int iS, int[] axRaster,
                              int[] azRaster, float[] ab, boolean isPrecise,
                              int iRaster, Rgb16[] gouraud) {
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
      errorTerm = 1 - dy;
    }
    int xMajorIncrement;
    int xMajorError;
    if (width <= dy) {
      // high-slope
      xMajorIncrement = 0;
      xMajorError = width;
    } else {
      // low-slope
      xMajorIncrement = GData.roundInt(dx / dy);
      xMajorError = width % dy;
    }
    if (isPrecise) {
      boolean isEast = (ab == bb);
//      if (isEast && ax[0] == 211 && ax[1] ==  139 && ax[2] ==  138)
//        System.out.println("trianglerenderer");

      setRastAB(abc[iN].y, abc[iN].z, abc[iS].y, abc[iS].z);
      float a0 = a;
      float b0 = b;
      for (int y = 0, zy = ay[iN], len = ab.length, lastY = dy - 1, i = iRaster; y <= lastY; ++i, ++y, ++zy) {
        if (i == 0 || i > iRaster) { // must always skip first on second time around
          if (i >= len)
            System.out.println("triangle rend errror");
          axRaster[i] = (y == lastY ? ax[iS] : xCurrent);
          azRaster[i] = (int) (ab[i] = getZCurrent(a0, b0, zy));
//          if ((ab[i] != Math.round(abc[iN].z)) && (ab[i] != Math.round(abc[iS].z)) && 
//              ((ab[i] < Math.round(abc[iN].z)) != (ab[i] > Math.round(abc[iS].z))))
//          System.out.println(Math.round(abc[iN].z) + "  "+ ab[i] + " " + Math.round(abc[iS].z));
          if (isEast) {
            //System.out.println(i + " axw " + axW[i] + " aa " + aa[i] + " x "
            //  + xCurrent + " ab " + ab[i]);

            //aa[i] and bb[i] are derived z values now
            setRastAB(axW[i], aa[i], axRaster[i], ab[i]);
            
//            if (ab[i] != getZCurrent(a, b, xCurrent))
//              System.out.println(a + " " + b + " " + axW[i] + " " + aa[i] +  " " + ab[i] + " " + getZCurrent(a, b, axRaster[i]));
            aa[i] = a;
            bb[i] = b;
          }
        }
        xCurrent += xMajorIncrement;
        errorTerm += xMajorError;
        if (errorTerm > 0) {
          xCurrent += xIncrement;
          errorTerm -= dy;
        }
      }
    } else {
      int zCurrentScaled = (zN << 10) + (1 << 9);
      int roundingFactor = GData.roundInt(dy / 2);
      if (dz < 0)
        roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;

      for (int y = 0, i = iRaster; y < dy; zCurrentScaled += zIncrementScaled, ++i, ++y) {
        axRaster[i] = xCurrent;
        azRaster[i] = zCurrentScaled >> 10;
        xCurrent += xMajorIncrement;
        errorTerm += xMajorError;
        if (errorTerm > 0) {
          xCurrent += xIncrement;
          errorTerm -= dy;
        }
      }
    }
    if (gouraud != null) {
      Rgb16 rgb16Base = rgb16t1;
      rgb16Base.setRgb(rgb16sGouraud[iN]);
      Rgb16 rgb16Increment = rgb16t2;
      rgb16Increment.diffDiv(rgb16sGouraud[iS], rgb16Base, dy);
      for (int i = iRaster, iMax = iRaster + dy; i < iMax; ++i)
        gouraud[i].setAndIncrement(rgb16Base, rgb16Increment);
      //      if (VERIFY) {
      //        Rgb16 north = rgb16sGouraud[iN];
      //        Rgb16 generated = gouraud[iRaster];
      //        if (north.getArgb() != generated.getArgb()) {
      //          if (Logger.debugging) {
      //            Logger.debug("north=" + north + "\ngenerated=" + generated);
      //          }
      //          throw new NullPointerException();
      //        }
      //      }
    }
  }

}
