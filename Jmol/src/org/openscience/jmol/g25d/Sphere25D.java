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

import org.openscience.jmol.*;

import java.awt.Component;
import java.awt.image.MemoryImageSource;
import java.util.Hashtable;

public class Sphere25D {

  DisplayControl control;
  Graphics25D g25d;
  byte[] semicircles;
  int[] semicircleIndex;

  final static int numFoo = 512;
  final static int shiftDivFoo = 9;
  short[] heightsFoo;

  public Sphere25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;

    /*
    semicircles = new byte[127*256 + 128];
    semicircleIndex = new int[256];
    int offset = 0;
    for (int d = 1; d < 255; ++d) {
      semicircleIndex[d] = offset;
      float radius = d / 2.0f;
      float radius2 = radius*radius;
      for (int i = 0; i < d; ++i) {
        float len = i - radius;
        float len2 = len*len;
        float z = (float)Math.sqrt(radius2 - len2);
        semicircles[offset++] = (byte)(z + 0.5f);
      }
    }
    */
  }

  void render3(short colix, int diameter, int xC, int yC, int zC) {
    if (diameter >= maxSphereCache)
      diameter = maxSphereCache;
    int radius = (diameter + 1) / 2;
    float r = diameter / 2.0f;
    float radius2 = r * r;
    g25d.argbCurrent = Colix.getArgb(colix);
    int xUL = xC - radius;
    int yUL = yC - radius;
    int[] shades = Colix.getShades(colix);
    
    float yF = -radius + 0.5f;
    for (int i = 0; i < diameter; ++i, ++yF) {
      float y2 = yF * yF;
      int y = yUL + i;
      float xF = -radius + 0.5f;
      for (int j = 0; j < diameter; ++j, ++xF) {
        float z2 = radius2 - y2 - xF*xF;
        if (z2 >= 0) {
          float zF = (float)Math.sqrt(z2);
          int intensity = Shade25D.calcIntensity(xF, yF, zF);
          int x = xUL + j;
          int z = zC - (int)(zF + 0.5f);
          g25d.plotPixelClipped(shades[intensity],
                                x, y, z);
        }
      }
    }
  }

  void renderBigUnclipped(int[] shades, int diameter, int x, int y, int z) {
    int radius = diameter / 2;
    x -= radius;
    y -= radius;
    float r = diameter / 2.0f;
    float r2 = r * r;
    
    float yF = -r + 0.5f;
    int zMin = z - ((diameter + 1) >> 1);
    int width = g25d.width;
    int[] pbuf = g25d.pbuf;
    short[] zbuf = g25d.zbuf;
    int y0 = y;
    int offsetPbufBeginLine = width * y + x;
    for (int i = 0; i < diameter;
         ++i, ++y0, ++yF, offsetPbufBeginLine += width) {
      float y2 = yF * yF;
      float xF = -r + 0.5f;
      int x0 = x;
      int offsetPbuf = offsetPbufBeginLine;
      for (int j = 0; j < diameter;
           ++j, ++x0, ++xF, ++offsetPbuf) {
        if (zbuf[offsetPbuf] <= zMin)
          continue;
        float z2 = r2 - y2 - xF*xF;
        if (z2 >= 0) {
          float zF = (float)Math.sqrt(z2);
          int z0 = z - (int)(zF + 0.5f);
          if (zbuf[offsetPbuf] <= z0)
            continue;
          int intensity = Shade25D.calcIntensity(xF, yF, zF);
          pbuf[offsetPbuf] = shades[intensity];
          zbuf[offsetPbuf] = (short) z0;
        }
      }
    }
  }

  void renderBigClipped(int[] shades, int diameter, int x, int y, int z) {
    int radius = diameter / 2;
    x -= radius;
    y -= radius;
    // FIXME optimize me some
    float r = diameter / 2.0f;
    float r2 = r * r;

    float yF = -r + 0.5f;
    int y0 = y;
    for (int i = 0; i < diameter; ++i, ++y0, ++yF) {
      float y2 = yF * yF;
      float xF = -r + 0.5f;
      int x0 = x;
      for (int j = 0; j < diameter; ++j, ++x0, ++xF) {
        float z2 = r2 - y2 - xF*xF;
        if (z2 >= 0) {
          float zF = (float)Math.sqrt(z2);
          int intensity = Shade25D.calcIntensity(xF, yF, zF);
          int z0 = z - (int)(zF + 0.5f);
          g25d.plotPixelClipped(shades[intensity],
                                x0, y0, z0);
        }
      }
    }
  }

  /*
  void render1(short colix, int diameter, int x, int y, int z) {
    if (diameter >= maxSphereCache)
      diameter = maxSphereCache;
    int radius = (diameter + 1) / 2;
    x -= radius;
    y -= radius;
    g25d.argbCurrent = Colix.getArgb(colix);
    int offsetRows = semicircleIndex[diameter];
    for (int i = diameter; --i >= 0; ++y) {
      int n = semicircles[offsetRows++];
      int numToPlot = n+n;
      int offsetCols = semicircleIndex[numToPlot];
      int numToSkip = diameter - n;
      int xT = x + numToSkip;
      while(--numToPlot >= 0)
        g25d.plotPixelClipped(xT++, y, z - semicircles[offsetCols++]);
    }
  }
  */
  
  void render(short colix, int diameter, int x, int y, int z) {
    if (diameter <= 1) {
      if (diameter == 1)
        g25d.plotPixelClipped(colix, x, y, z);
      return;
    }
    int radius = (diameter + 1) >> 1;
    int[] shades = Colix.getShades(colix);
    if (diameter >= maxSphereCache) {
      if (x-radius < 0 || x+radius >= g25d.width ||
          y-radius < 0 || y+radius >= g25d.height)
        renderBigClipped(shades, diameter, x, y, z);
      else
        renderBigUnclipped(shades, diameter, x, y, z);
      return;
    } 
    int[] ss = getSphereShape(diameter);
    if (x-radius < 0 || x+radius >= g25d.width ||
        y-radius < 0 || y+radius >= g25d.height)
      renderShapeClipped(shades, ss, diameter, x, y, z);
    else
      renderShapeUnclipped(shades, ss, diameter, x, y, z);
  }


  void renderShapeUnclipped(int[] shades, int[] sphereShape,
                            int diameter, int x, int y, int z) {
    int[] pbuf = g25d.pbuf;
    short[] zbuf = g25d.zbuf;
    int offsetSphere = 0;
    int width = g25d.width;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    do {
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      do {
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (zPixel < zbuf[offsetSE]) {
          zbuf[offsetSE] = (short)zPixel;
          pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
        }
        if (zPixel < zbuf[offsetSW]) {
          zbuf[offsetSW] = (short)zPixel;
          pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
        }
        if (zPixel < zbuf[offsetNE]) {
          zbuf[offsetNE] = (short)zPixel;
          pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
        }
        if (zPixel < zbuf[offsetNW]) {
          zbuf[offsetNW] = (short)zPixel;
          pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
    } while (--nLines > 0);
  }

  void renderShapeClipped(int[] shades, int[] sphereShape,
                          int diameter, int x, int y, int z) {
    int[] pbuf = g25d.pbuf;
    short[] zbuf = g25d.zbuf;
    int offsetSphere = 0;
    int width = g25d.width;
    int height = g25d.height;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    int ySouth = y;
    int yNorth = y - evenSizeCorrection;
    do {
      boolean tSouthVisible = ySouth >= 0 && ySouth < height;
      boolean tNorthVisible = yNorth >= 0 && yNorth < height;
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int xEast = x;
      int xWest = x - evenSizeCorrection;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < width;
        boolean tEastVisible = xEast >= 0 && xEast < width;
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (tSouthVisible) {
          if (tEastVisible && zPixel < zbuf[offsetSE]) {
            zbuf[offsetSE] = (short)zPixel;
            pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
          }
          if (tWestVisible && zPixel < zbuf[offsetSW]) {
            zbuf[offsetSW] = (short)zPixel;
            pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
          }
        }
        if (tNorthVisible) {
          if (tEastVisible && zPixel < zbuf[offsetNE]) {
            zbuf[offsetNE] = (short)zPixel;
            pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
          }
          if (tWestVisible && zPixel < zbuf[offsetNW]) {
            zbuf[offsetNW] = (short)zPixel;
            pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
          }
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        ++xEast;
        --xWest;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
      ++ySouth;
      --yNorth;
    } while (--nLines > 0);
  }

  final static int maxSphereCache = 64;
  static int[][] sphereShapeCache = new int[maxSphereCache][];
  byte[] intensities = new byte[maxSphereCache * maxSphereCache];
  byte[] heights = new byte[maxSphereCache * maxSphereCache];

  int[] getSphereShape(int diameter) {
    int[] ss;
    if (diameter > maxSphereCache)
      diameter = maxSphereCache;
    ss = sphereShapeCache[diameter - 1];
    if (ss != null)
      return ss;
    ss = sphereShapeCache[diameter - 1] = createSphereShape(diameter);
    return ss;
  }

  int calcIntensity1(float x, float y, float radius2) {
    float z2 = radius2 - x*x - y*y;
    if (z2 < 0)
      return -1;
    return Shade25D.calcIntensity(x, y, (float)Math.sqrt(z2));
  }

  byte calcIntensity(float x, float y, float radius2) {
    int count = 0;
    int intensitySum = 0;
    for (float i = -1/6f; i < .17f; i += 1/3f)
      for (float j = -1/6f; j < .17f; j += 1/3f) {
        int intensity = calcIntensity1(x + i, y + j, radius2);
        if (intensity >= 0) {
          intensitySum += intensity;
          ++count;
        }
      }
    if (count == 0) {
      System.out.println("count is 0 ... why am I here?");
      return -1;
    }
    return (byte)((intensitySum + count/2) / count);
  }

  int[] createSphereShape(int diameter) {
    float radiusF = diameter / 2.0f;
    float radiusF2 = radiusF * radiusF;
    int offset = 0;
    
    int visibleCount = 0;
    int countSE = 0;
    int radius = diameter / 2;
    
    float y = -radiusF + 0.5f;
    for (int i = 0; i < diameter; ++i, ++y) {
      float y2 = y * y;
      float x = -radiusF + 0.5f;
      for (int j = 0; j < diameter; ++j, ++x) {
        float z2 = radiusF2 - y2 - x*x;
        if (z2 >= 0) {
          float z = (float)Math.sqrt(z2);
          intensities[offset] = calcIntensity(x, y, radiusF2);
          heights[offset] = (byte)(z + 0.5f);
          if (j >= radius && i >= radius)
            ++countSE;
        } else {
          intensities[offset] = -1;
          heights[offset] = -1;
        }
        ++offset;
      }
    }
    
    int[] sphereShape = new int[countSE];
    int offset2 = 0;
    int offsetCount;

    for(int i = radius; i < diameter; ++i) {
      int offsetRowSouth = i*diameter;
      int offsetRowNorth = (diameter - i - 1) * diameter;
      for (int j = radius;
           j < diameter && heights[offsetRowSouth + j] != -1; ++j) {
        int packed = heights[offsetRowSouth + j];
        packed |= (intensities[offsetRowSouth + j]) << 7;
        packed |= (intensities[offsetRowSouth + diameter - j - 1]) << 13;
        packed |= (intensities[offsetRowNorth + j]) << 19;
        packed |= (intensities[offsetRowNorth + diameter - j - 1]) << 25;
        sphereShape[offset2++] = packed;
      }
      sphereShape[offset2 - 1] |= 0x80000000;
    }
    return sphereShape;
  }
}
