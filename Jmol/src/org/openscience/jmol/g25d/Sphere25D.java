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
    if (diameter == 0)
      return;
    int radius = diameter >> 1;
    x -= radius;
    y -= radius;
    int[] shades = Colix.getShades(colix);
    if (diameter >= maxSphereCache) {
      if (x < 0 || x+diameter >= g25d.width ||
          y < 0 || y+diameter >= g25d.height)
        renderBigClipped(shades, diameter, x, y, z);
      else
        renderBigUnclipped(shades, diameter, x, y, z);
      return;
    } 
    int[] ss = getSphereShape(diameter);
    if (x < 0 || x+diameter >= g25d.width ||
        y < 0 || y+diameter >= g25d.height)
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
    int offsetNorthBeginLine = width * y + x;
    int offsetSouthBeginLine = offsetNorthBeginLine + (diameter - 1) * width;
    int halfDiameter = (diameter + 1) / 2;
    int i = halfDiameter;
    do {
      int plotCount = sphereShape[offsetSphere++];
      int skipCount = halfDiameter - plotCount;
      int offsetNW = offsetNorthBeginLine + skipCount;
      int offsetNE = offsetNorthBeginLine + diameter - skipCount - 1;
      int offsetSW = offsetSouthBeginLine + skipCount;
      int offsetSE = offsetSouthBeginLine + diameter - skipCount - 1;
      do {
        int packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (zPixel < zbuf[offsetNW]) {
          zbuf[offsetNW] = (short)zPixel;
          pbuf[offsetNW] = shades[(packed >> 7) & 0x3F];
        }
        if (zPixel < zbuf[offsetNE]) {
          zbuf[offsetNE] = (short)zPixel;
          pbuf[offsetNE] = shades[(packed >> 13) & 0x3F];
        }
        if (zPixel < zbuf[offsetSW]) {
          zbuf[offsetSW] = (short)zPixel;
          pbuf[offsetSW] = shades[(packed >> 19) & 0x3F];
        }
        if (zPixel < zbuf[offsetSE]) {
          zbuf[offsetSE] = (short)zPixel;
          pbuf[offsetSE] = shades[(packed >> 25) & 0x3F];
        }
        ++offsetNW;
        --offsetNE;
        ++offsetSW;
        --offsetSE;
        } while (--plotCount > 0);
      offsetNorthBeginLine += width;
      offsetSouthBeginLine -= width;
    } while (--i > 0);
  }

  void renderShapeClipped(int[] shades, int[] sphereShape,
                            int diameter, int x, int y, int z) {
    int[] pbuf = g25d.pbuf;
    short[] zbuf = g25d.zbuf;
    int offsetSphere = 0;
    int width = g25d.width;
    int height = g25d.height;
    int diameterMinus1 = diameter - 1;
    int offsetNorthBeginLine = width * y + x;
    int offsetSouthBeginLine = offsetNorthBeginLine + diameterMinus1 * width;
    int halfDiameter = (diameter + 1) / 2;
    int i = halfDiameter;
    int yNorth = y;
    int ySouth = y + diameterMinus1;
    do {
      boolean tNorthVisible = yNorth >= 0 && yNorth < height;
      boolean tSouthVisible = ySouth >= 0 && ySouth < height;
      int plotCount = sphereShape[offsetSphere++];
      int skipCount = halfDiameter - plotCount;
      int offsetNW = offsetNorthBeginLine + skipCount;
      int offsetNE = offsetNorthBeginLine + diameterMinus1 - skipCount;
      int offsetSW = offsetSouthBeginLine + skipCount;
      int offsetSE = offsetSouthBeginLine + diameterMinus1 - skipCount;
      int xWest = x + skipCount;
      int xEast = x + diameterMinus1 - skipCount;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < width;
        boolean tEastVisible = xEast >= 0 && xEast < width;
        int packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (tNorthVisible) {
          if (tWestVisible && zPixel < zbuf[offsetNW]) {
            zbuf[offsetNW] = (short)zPixel;
            pbuf[offsetNW] = shades[(packed >> 7) & 0x3F];
          }
          if (tEastVisible && zPixel < zbuf[offsetNE]) {
            zbuf[offsetNE] = (short)zPixel;
            pbuf[offsetNE] = shades[(packed >> 13) & 0x3F];
          }
        }
        if (tSouthVisible) {
          if (tWestVisible && zPixel < zbuf[offsetSW]) {
            zbuf[offsetSW] = (short)zPixel;
            pbuf[offsetSW] = shades[(packed >> 19) & 0x3F];
          }
          if (tEastVisible && zPixel < zbuf[offsetSE]) {
            zbuf[offsetSE] = (short)zPixel;
            pbuf[offsetSE] = shades[(packed >> 25) & 0x3F];
          }
        }

        ++xWest;
        --xEast;
        ++offsetNW;
        --offsetNE;
        ++offsetSW;
        --offsetSE;
      } while (--plotCount > 0);
      ++yNorth;
      --ySouth;
      offsetNorthBeginLine += width;
      offsetSouthBeginLine -= width;
    } while (--i > 0);
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

  int[] createSphereShape(int diameter) {
    float radius = diameter / 2.0f;
    float radius2 = radius * radius;
    int offset = 0;
    
    int visibleCount = 0;
    int countNW = 0;
    int halfDiameter = (diameter + 1) / 2;
    
    float y = -radius + 0.5f;
    for (int i = 0; i < diameter; ++i, ++y) {
      float y2 = y * y;
      float x = -radius + 0.5f;
      for (int j = 0; j < diameter; ++j, ++x) {
        float z2 = radius2 - y2 - x*x;
        if (z2 >= 0) {
          float z = (float)Math.sqrt(z2);
          intensities[offset] = Shade25D.calcIntensity(x, y, z);
          heights[offset] = (byte)(z + 0.5f);
          if (x < halfDiameter && y < halfDiameter)
            ++countNW;
        } else {
          intensities[offset] = -1;
          heights[offset] = -1;
        }
        ++offset;
      }
    }
    
    int[] sphereShape = new int[diameter + countNW];
    
    int offset2 = 0;
    int offsetCount;
    int r = diameter / 2;
    for(int i = 0; i < halfDiameter; ++i) {
      int offsetNorth = i*diameter;
      int offsetSouth = (diameter - i - 1) * diameter;
      int j = 0;
      while (j < halfDiameter && heights[offsetNorth + j] == -1)
        ++j;
      int plotCount = halfDiameter - j;
      sphereShape[offset2++] = plotCount;
      while (j < halfDiameter) {
        int packed = heights[offsetNorth + j];
        packed |= (intensities[offsetNorth + j]) << 7;
        packed |= (intensities[offsetNorth + diameter - j - 1]) << 13;
        packed |= (intensities[offsetSouth + j]) << 19;
        packed |= (intensities[offsetSouth + diameter - j - 1]) << 25;
        sphereShape[offset2++] = packed;
        ++j;
      }
    }
    return sphereShape;
  }
}
