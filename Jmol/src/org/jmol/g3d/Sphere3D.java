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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.g3d;

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
 *
 * @author Miguel, miguel@jmol.org
 */
class Sphere3D {

  Graphics3D g3d;
  
  Sphere3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  private int zShift;
  
  private final static int maxSphereCache = 128;
  private final static int maxOddSizeSphere = 49;
  private final static int maxSphereDiameter = 1000;
  private final static int[][] sphereShapeCache = new int[maxSphereCache][];

  static synchronized void flushSphereCache() {
    for (int i =  maxSphereCache; --i >= 0;)
      sphereShapeCache[i] = null;
  }

  private static int[] getSphereShape(int diameter) {
    int[] ss;
    return ((ss = sphereShapeCache[diameter - 1]) == null ? createSphereShape(diameter): ss);
  }

  private synchronized static int[] createSphereShape(int diameter) {
    int countSE = 0;
    boolean oddDiameter = (diameter & 1) != 0;
    float radiusF = diameter / 2.0f;
    float radiusF2 = radiusF * radiusF;
    int radius = (diameter + 1) / 2;

    float y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0)
          ++countSE;
      }
    }
    
    int[] sphereShape = new int[countSE];
    int offset = 0;

    y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0) {
          float z = (float)Math.sqrt(z2);
          int height = (int)z;
          int intensitySE = Shade3D.calcDitheredNoisyIntensity( x,  y, z, radiusF);
          int intensitySW = Shade3D.calcDitheredNoisyIntensity(-x,  y, z, radiusF);
          int intensityNE = Shade3D.calcDitheredNoisyIntensity( x, -y, z, radiusF);
          int intensityNW = Shade3D.calcDitheredNoisyIntensity(-x, -y, z, radiusF);
          int packed = (height |
                        (intensitySE << 7) |
                        (intensitySW << 13) |
                        (intensityNE << 19) |
                        (intensityNW << 25));
          sphereShape[offset++] = packed;
        }
      }
      sphereShape[offset - 1] |= 0x80000000;
    }
    return sphereShapeCache[diameter - 1] = sphereShape;
  }

  void render(int[] shades, boolean tScreened, int diameter,
              int x, int y, int z) {
    if (z == 1)
      return;
    if (diameter > maxOddSizeSphere)
      diameter &= ~1;
    int radius = (diameter + 1) >> 1;
    int minX = x - radius, maxX = x + radius;
    if (maxX < 0 || minX >= g3d.width)
      return;
    int minY = y - radius, maxY = y + radius;
    if (maxY < 0 || minY >= g3d.height)
      return;
    int minZ = z - radius, maxZ = z + radius;
    if (maxZ < g3d.slab || minZ > g3d.depth)
      return;
    if (diameter > maxSphereCache) {
      if (diameter <= maxSphereDiameter)
        renderLargeSphere(shades, tScreened, diameter, x, y, z);
      return;
    }
    zShift = g3d.getZShift(z);
    int[] ss = getSphereShape(diameter);
    if (minX < 0 || maxX >= g3d.width ||
        minY < 0 || maxY >= g3d.height ||
        minZ < g3d.slab || z > g3d.depth)
      renderShapeClipped(shades, tScreened, ss, diameter, x, y, z);
    else
      renderShapeUnclipped(shades, tScreened, ss, diameter, x, y, z);
  }
  
  private void renderShapeUnclipped(int[] shades, boolean tScreened,
                                    int[] sphereShape, int diameter, int x,
                                    int y, int z) {
    int[] zbuf = g3d.zbuf;
    int offsetSphere = 0;

    int width = g3d.width;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    if (!tScreened) {
      do {
        int offsetSE = offsetSouthCenter;
        int offsetSW = offsetSouthCenter - evenSizeCorrection;
        int offsetNE = offsetNorthCenter;
        int offsetNW = offsetNorthCenter - evenSizeCorrection;
        int packed;
        do {
          packed = sphereShape[offsetSphere++];
          int zPixel = z - (packed & 0x7F);
          if (zPixel < zbuf[offsetSE])
            g3d.addPixel(offsetSE, zPixel,
                shades[((packed >> 7) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetSW])
            g3d.addPixel(offsetSW, zPixel,
                shades[((packed >> 13) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetNE])
            g3d.addPixel(offsetNE, zPixel,
                shades[((packed >> 19) & 0x3F) >> zShift]);
          if (zPixel < zbuf[offsetNW])
            g3d.addPixel(offsetNW, zPixel,
                shades[((packed >> 25) & 0x3F) >> zShift]);
          ++offsetSE;
          --offsetSW;
          ++offsetNE;
          --offsetNW;
        } while (packed >= 0);
        offsetSouthCenter += width;
        offsetNorthCenter -= width;
      } while (--nLines > 0);
      return;
    }
    int flipflopSouthCenter = (x ^ y) & 1;
    int flipflopNorthCenter = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopSE = flipflopSouthCenter;
    int flipflopSW = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopNE = flipflopNorthCenter;
    int flipflopNW = flipflopNorthCenter ^ evenSizeCorrection;
    int flipflopsCenter = flipflopSE | (flipflopSW << 1) | (flipflopNE << 2)
        | (flipflopNW << 3);
    do {
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int flipflops = (flipflopsCenter = ~flipflopsCenter);
      do {
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if ((flipflops & 1) != 0 && zPixel < zbuf[offsetSE])
            g3d.addPixel(offsetSE, zPixel,
                shades[((packed >> 7) & 0x3F) >> zShift]);
        if ((flipflops & 2) != 0 && zPixel < zbuf[offsetSW])        
          g3d.addPixel(offsetSW, zPixel,
              shades[((packed >> 13) & 0x3F) >> zShift]);
        if ((flipflops & 4) != 0 && zPixel < zbuf[offsetNE])        
          g3d.addPixel(offsetNE, zPixel,
              shades[((packed >> 19) & 0x3F) >> zShift]);
        if ((flipflops & 8) != 0 && zPixel < zbuf[offsetNW])
          g3d.addPixel(offsetNW, zPixel,
              shades[((packed >> 25) & 0x3F) >> zShift]);
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        flipflops = ~flipflops;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
    } while (--nLines > 0);
  }

  private final static int SHADE_SLAB_CLIPPED = Shade3D.shadeNormal - 5;

  private void renderShapeClipped(int[] shades, boolean tScreened,
                                  int[] sphereShape, int diameter, int x,
                                  int y, int z) {
    int[] zbuf = g3d.zbuf;
    boolean addAllPixels = g3d.addAllPixels;
    int offsetSphere = 0;
    int width = g3d.width, height = g3d.height;
    int slab = g3d.slab, depth = g3d.depth;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    int ySouth = y;
    int yNorth = y - evenSizeCorrection;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    int flipflopSouthCenter = (x ^ y) & 1;
    int flipflopNorthCenter = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopSE = flipflopSouthCenter;
    int flipflopSW = flipflopSouthCenter ^ evenSizeCorrection;
    int flipflopNE = flipflopNorthCenter;
    int flipflopNW = flipflopNorthCenter ^ evenSizeCorrection;
    int flipflopsCenter = flipflopSE | (flipflopSW << 1) | (flipflopNE << 2)
        | (flipflopNW << 3);
    do {
      boolean tSouthVisible = ySouth >= 0 && ySouth < height;
      boolean tNorthVisible = yNorth >= 0 && yNorth < height;
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int flipflops = (flipflopsCenter = ~flipflopsCenter);
      int xEast = x;
      int xWest = x - evenSizeCorrection;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < width;
        boolean tEastVisible = xEast >= 0 && xEast < width;
        packed = sphereShape[offsetSphere++];
        boolean isCore;
        int zOffset = packed & 0x7F;
        int zPixel;
        if (z < slab) {
          // center in front of plane -- possibly show back half
          zPixel = z + zOffset;
          isCore = (zPixel >= slab);
        } else {
          // center is behind, show front, possibly as solid core
          zPixel = z - zOffset;
          isCore = (zPixel < slab);
        }
        if (isCore)
          zPixel = slab;
        if (zPixel >= slab && zPixel <= depth) {
          if (tSouthVisible) {
            if (tEastVisible && (addAllPixels || (flipflops & 1) != 0)
                && zPixel < zbuf[offsetSE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 7) & 0x07)
                  : (packed >> 7) & 0x3F);
              g3d.addPixel(offsetSE, zPixel, shades[i >> zShift]);
            }
            if (tWestVisible && (addAllPixels || (flipflops & 2) != 0)
                && zPixel < zbuf[offsetSW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 13) & 0x07)
                  : (packed >> 13) & 0x3F);
              g3d.addPixel(offsetSW, zPixel, shades[i >> zShift]);
            }
          }
          if (tNorthVisible) {
            if (tEastVisible && (!tScreened || (flipflops & 4) != 0)
                && zPixel < zbuf[offsetNE]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 19) & 0x07)
                  : (packed >> 19) & 0x3F);
              g3d.addPixel(offsetNE, zPixel, shades[i >> zShift]);
            }
            if (tWestVisible && (!tScreened || (flipflops & 8) != 0)
                && zPixel < zbuf[offsetNW]) {
              int i = (isCore ? SHADE_SLAB_CLIPPED - 3 + ((randu >> 25) & 0x07)
                  : (packed >> 25) & 0x3F);
              g3d.addPixel(offsetNW, zPixel, shades[i >> zShift]);
            }
          }
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        ++xEast;
        --xWest;
        flipflops = ~flipflops;
        if (isCore)
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
      ++ySouth;
      --yNorth;
    } while (--nLines > 0);
  }

  private void renderLargeSphere(int[] shades, boolean tScreened, int diameter,
                         int x, int y, int z) {
    if (!Shade3D.sphereShadingCalculated)
      Shade3D.calcSphereShading();
    int radius = diameter / 2;
    renderQuadrant(shades, tScreened, radius, x, y, z, -1, -1);
    renderQuadrant(shades, tScreened, radius, x, y, z, -1,  1);
    renderQuadrant(shades, tScreened, radius, x, y, z,  1, -1);
    renderQuadrant(shades, tScreened, radius, x, y, z,  1,  1);
  }

  private void renderQuadrant(int[] shades, boolean tScreened, int radius, int x,
                      int y, int z, int xSign, int ySign) {
    int t = x + radius * xSign;
    int xStatus = (x < 0 ? -1 : x < g3d.width ? 0 : 1)
        + (t < 0 ? -2 : t < g3d.width ? 0 : 2);
    if (xStatus == -3 || xStatus == 3)
      return;

    t = y + radius * ySign;
    int yStatus = (y < 0 ? -1 : y < g3d.height ? 0 : 1)
        + (t < 0 ? -2 : t < g3d.height ? 0 : 2);
    if (yStatus == -3 || yStatus == 3)
      return;

    boolean zStatus = (z - radius >= g3d.slab  && z <= g3d.depth);
    
    if (xStatus == 0 && yStatus == 0 && zStatus)
      renderQuadrantUnclipped(shades, tScreened, radius, x, y, z, xSign, ySign);
    else
      renderQuadrantClipped(shades, tScreened, radius, x, y, z, xSign, ySign);
  }

  private void renderQuadrantUnclipped(int[] shades, boolean tScreened, int radius,
                               int x, int y, int z,
                               int xSign, int ySign) {
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;

    int[] zbuf = g3d.zbuf;
    boolean addAllPixels = g3d.addAllPixels;
    int width = g3d.width;
    // it will get flipped twice before use
    // so initialize it to true if it is at an even coordinate
    boolean flipflopBeginLine = ((x ^ y) & 1) == 0;
    int offsetPbufBeginLine = width * y + x;
    if (ySign < 0)
      width = -width;
    offsetPbufBeginLine -= width;
    for (int i = 0, i2 = 0; i2 <= r2; i2 += i + i + 1, ++i) {
      int offsetPbuf = (offsetPbufBeginLine += width) - xSign;
      boolean flipflop = (flipflopBeginLine = !flipflopBeginLine);
      int s2 = r2 - i2;
      int z0 = z - radius;
      int y8 = ((i * ySign + radius) << 8) / dDivisor;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + j + 1, ++j) {
        offsetPbuf += xSign;
        if (addAllPixels || (flipflop = !flipflop)) {
          if (zbuf[offsetPbuf] <= z0)
            continue;
          int k = (int)Math.sqrt(s2 - j2);
          z0 = z - k;
          if (zbuf[offsetPbuf] <= z0)
            continue;
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          g3d.addPixel(offsetPbuf,z0, shades[Shade3D.sphereIntensities[((y8 << 8) + x8) >> zShift]]);
        }
      }
    }
  }

  private void renderQuadrantClipped(int[] shades, boolean tScreened, int radius,
                             int x, int y, int z,
                             int xSign, int ySign) {
    int r2 = radius * radius;
    int dDivisor = radius * 2 + 1;
    int[] zbuf = g3d.zbuf;
    int slab = g3d.slab;
    int depth = g3d.depth;
    int height = g3d.height;
    int width = g3d.width;
    int offsetPbufBeginLine = width * y + x;
    int lineIncrement = width;
    int randu = (x << 16) + (y << 1) ^ 0x33333333;
    if (ySign < 0)
      lineIncrement = -width;
    int yCurrent = y - ySign;
    for (int i = 0, i2 = 0;
         i2 <= r2;
         i2 += i + i + 1, ++i, offsetPbufBeginLine += lineIncrement) {
      yCurrent += ySign;
      if (yCurrent < 0) {
        if (ySign < 0)
          return;
        continue;
      }
      if (yCurrent >= height) {
        if (ySign > 0)
          return;
        continue;
      }
      int offsetPbuf = offsetPbufBeginLine;
      int s2 = r2 - i2;
      int xCurrent = x - xSign;
      int y8 = ((i * ySign + radius) << 8) / dDivisor;
      randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + j + 1, ++j, offsetPbuf += xSign) {
        xCurrent += xSign;
        if (xCurrent < 0) {
          if (xSign < 0)
            break;
          continue;
        }
        if (xCurrent >= width) {
          if (xSign > 0)
            break;
          continue;
        }
        if (tScreened && (((xCurrent ^ yCurrent) & 1) != 0))
          continue;
        int zOffset = (int)Math.sqrt(s2 - j2);
        int zPixel;
        boolean isCore;
        if (z < slab) {
          // center in front of plane -- possibly show back half
          zPixel = z + zOffset;
          isCore = (zPixel >= slab);
        } else {
          // center is behind, show front, possibly as solid core
          zPixel = z - zOffset;
          isCore = (zPixel < slab);
        }
        if (isCore)
          zPixel = slab;
        if (zPixel < slab || zPixel > depth || zbuf[offsetPbuf] <= zPixel)
          continue;
        int s;
        if (isCore) {
          s = SHADE_SLAB_CLIPPED - 3 + ((randu >> 8) & 0x07);
          randu = ((randu << 16) + (randu << 1) + randu) & 0x7FFFFFFF;
        } else {
          int x8 = ((j * xSign + radius) << 8) / dDivisor;
          s = Shade3D.sphereIntensities[(y8 << 8) + x8];
        }
        g3d.addPixel(offsetPbuf, zPixel, shades[s >> zShift]);
      }
      // randu is failing me and generating moire patterns :-(
      // so throw in a little more salt
      randu = ((randu + xCurrent + yCurrent) | 1) & 0x7FFFFFFF;
    }
  }
}
