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

/**
 *<p>
 * Implements high performance rendering of shaded spheres.
 *</p>
 *<p>
 * Drawing spheres quickly is critically important to Jmol.
 * These routines implement high performance rendering of
 * spheres in 3D.
 *</p>
 *<p>
 * If you can think of a faster way to implement this, please
 * let us know.
 *</p>
 *<p>
 * There is a lot of bit-twiddling going on here, which may
 * make the code difficult to understand for non-systems programmers.
 *</p>
 */
class Sphere3D {

  Graphics3D g3d;
  byte[] semicircles;
  int[] semicircleIndex;

  final static int numFoo = 512;
  final static int shiftDivFoo = 9;
  short[] heightsFoo;

  Sphere3D(Graphics3D g3d) {
    this.g3d = g3d;

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

  /*
  void render3(short colix, int diameter, int xC, int yC, int zC) {
    if (diameter >= maxSphereCache)
      diameter = maxSphereCache;
    int radius = (diameter + 1) / 2;
    float r = diameter / 2.0f;
    float radius2 = r * r;
    g3d.argbCurrent = g3d.getArgb(colix);
    int xUL = xC - radius;
    int yUL = yC - radius;
    int[] shades = g3d.getShades(colix);
    
    float yF = -radius + 0.5f;
    for (int i = 0; i < diameter; ++i, ++yF) {
      float y2 = yF * yF;
      int y = yUL + i;
      float xF = -radius + 0.5f;
      for (int j = 0; j < diameter; ++j, ++xF) {
        float z2 = radius2 - y2 - xF*xF;
        if (z2 >= 0) {
          float zF = (float)Math.sqrt(z2);
          int intensity = calcIntensity(xF, yF, zF);
          int x = xUL + j;
          int z = zC - (int)(zF + 0.5f);
          g3d.plotPixelClipped(shades[intensity], x, y, z);
        }
      }
    }
  }
  */

  void renderBigUnclipped(int[] shades, int diameter, int x, int y, int z) {
    int radius = diameter / 2;
    x -= radius;
    y -= radius;
    float r = diameter / 2.0f;
    float r2 = r * r;
    
    float yF = -r + 0.5f;
    int zMin = z - ((diameter + 1) >> 1);
    int width = g3d.width;
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
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
          int intensity = Shade3D.calcDitheredNoisyIntensity(xF, yF, zF);
          pbuf[offsetPbuf] = shades[intensity];
          zbuf[offsetPbuf] = (short) z0;
        }
      }
    }
  }

  // thanks to nico for eliminating the function call to put up the pixels
  void renderBigClipped(int[] shades, int diameter, int x, int y, int z) {
    int radius = diameter / 2;
    x -= radius;
    y -= radius;
    float r = diameter / 2.0f;
    float r2 = r * r;
    
    float yF = -r + 0.5f;
    int zMin = z - ((diameter + 1) >> 1);
    int width = g3d.width;
    int height = g3d.height;
    int slab = g3d.slab;
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int y0 = y;
    int offsetPbufBeginLine = width * y + x;
    for (int i = 0; i < diameter;
         ++i, ++y0, ++yF, offsetPbufBeginLine += width) {
      if ((y0 < 0) || (y0 >= height))
      	continue;
      float y2 = yF * yF;
      float xF = -r + 0.5f;
      int x0 = x;
      int offsetPbuf = offsetPbufBeginLine;
      for (int j = 0; j < diameter;
           ++j, ++x0, ++xF, ++offsetPbuf) {
      	if ((x0 < 0) || (x0 >= g3d.width))
      	  continue;
        if (zbuf[offsetPbuf] <= zMin)
          continue;
        float z2 = r2 - y2 - xF*xF;
        if (z2 >= 0) {
          float zF = (float)Math.sqrt(z2);
          int z0 = z - (int)(zF + 0.5f);
          if (z0 >= slab && z0 < zbuf[offsetPbuf]) {
            int intensity = Shade3D.calcDitheredNoisyIntensity(xF, yF, zF);
            pbuf[offsetPbuf] = shades[intensity];
            zbuf[offsetPbuf] = (short) z0;
          }
        }
      }
    }
  }

  /*
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
          int intensity = Shade3D.calcDitheredNoisyIntensity(xF, yF, zF);
          int z0 = z - (int)(zF + 0.5f);
          g3d.plotPixelClipped(shades[intensity], x0, y0, z0);
        }
      }
    }
  }
  */

  void render(short colix, int diameter, int x, int y, int z) {
    int[] shades = g3d.getShades(colix);
    int radius = (diameter + 1) >> 1;
    int minX = x - radius, maxX = x + radius;
    int minY = y - radius, maxY = y + radius;
    if (maxX < 0 || minX >= g3d.width ||
        maxY < 0 || minY >= g3d.height ||
        z < g3d.slab)
      return;
    int minZ = z - radius;
    boolean clipped = (minX < 0 || maxX >= g3d.width ||
                       minY < 0 || maxY >= g3d.height ||
                       minZ < g3d.slab);
    if (diameter >= maxSphereCache) {
      if (clipped)
        renderBigClipped(shades, diameter, x, y, z);
      else
        renderBigUnclipped(shades, diameter, x, y, z);
      return;
    } 
    int[] ss = getSphereShape(diameter);
    if (clipped)
      renderShapeClipped(shades, ss, diameter, x, y, z);
    else
      renderShapeUnclipped(shades, ss, diameter, x, y, z);
  }
  

  void renderShapeUnclipped(int[] shades, int[] sphereShape,
                            int diameter, int x, int y, int z) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int offsetSphere = 0;
    int width = g3d.width;
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
        if (zPixel <= zbuf[offsetSE]) {
          if (zPixel < zbuf[offsetSE]) {
            zbuf[offsetSE] = (short)zPixel;
            pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
          } else {
            g3d.averageOffsetArgb(offsetSE, shades[(packed >> 7) & 0x3F]);
          }
        }
        if (zPixel <= zbuf[offsetSW]) {
          if (zPixel < zbuf[offsetSW]) {
            zbuf[offsetSW] = (short)zPixel;
            pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
          } else {
            g3d.averageOffsetArgb(offsetSW, shades[(packed >> 13) & 0x3F]);
          }
        }
        if (zPixel <= zbuf[offsetNE]) {
          if (zPixel < zbuf[offsetNE]) {
            zbuf[offsetNE] = (short)zPixel;
            pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
          } else {
            g3d.averageOffsetArgb(offsetNE, shades[(packed >> 19) & 0x3F]);
          }
        }
        if (zPixel < zbuf[offsetNW]) {
          if (zPixel < zbuf[offsetNW]) {
            zbuf[offsetNW] = (short)zPixel;
            pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
          } else {
            g3d.averageOffsetArgb(offsetNW, shades[(packed >> 25) & 0x3F]);
          }
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
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int offsetSphere = 0;
    int width = g3d.width;
    int height = g3d.height;
    int slab = g3d.slab;
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
        if (zPixel >= slab) {
          if (tSouthVisible) {
            if (tEastVisible && zPixel <= zbuf[offsetSE]) {
              if (zPixel < zbuf[offsetSE]) {
                zbuf[offsetSE] = (short)zPixel;
                pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
              } else {
                g3d.averageOffsetArgb(offsetSE, shades[(packed >> 7) & 0x3F]);
              }
            }
            if (tWestVisible && zPixel <= zbuf[offsetSW]) {
              if (zPixel < zbuf[offsetSW]) {
                zbuf[offsetSW] = (short)zPixel;
                pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
              } else {
                g3d.averageOffsetArgb(offsetSW, shades[(packed >> 13) & 0x3F]);
              }
            }
          }
          if (tNorthVisible) {
            if (tEastVisible && zPixel <= zbuf[offsetNE]) {
              if (zPixel < zbuf[offsetNE]) {
                zbuf[offsetNE] = (short)zPixel;
                pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
              } else {
                g3d.averageOffsetArgb(offsetNE, shades[(packed >> 19) & 0x3F]);
              }
            }
            if (tWestVisible && zPixel <= zbuf[offsetNW]) {
              if (zPixel < zbuf[offsetNW]) {
                zbuf[offsetNW] = (short)zPixel;
                pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
              } else {
                g3d.averageOffsetArgb(offsetNW, shades[(packed >> 25) & 0x3F]);
              }
            }
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

  final static int maxSphereCache = 128;
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

  static void flushImageCache() {
    sphereShapeCache = new int[maxSphereCache][];
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
          intensities[offset] = Shade3D.calcDitheredNoisyIntensity(x, y, z);
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
