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

final class Line3D {

  Graphics3D g3d;

  Line3D(JmolViewer viewer, Graphics3D g3d) {
    this.g3d = g3d;
  }

  void drawLine(int argb, int x1, int y1, int z1, int x2, int y2, int z2,
                boolean tDotted) {
    int cc1 = clipCode(x1, y1);
    int cc2 = clipCode(x2, y2);
    int xLast = g3d.xLast;
    int yLast = g3d.yLast;
    while ((cc1 | cc2) != 0) {
      if ((cc1 & cc2) != 0)
        return;
      int dx = x2 - x1;
      int dy = y2 - y1;
      int dz = z2 - z1;


      if (cc1 != 0) { //cohen-sutherland line clipping
        if      ((cc1 & xLT) != 0)
          { y1 +=      (-x1 * dy)/dx; z1 +=      (-x1 * dz)/dx; x1 = 0; }
        else if ((cc1 & xGT) != 0)
          { y1 += ((xLast-x1)*dy)/dx; z1 += ((xLast-x1)*dz)/dx; x1 = xLast; }
        else if ((cc1 & yLT) != 0)
          { x1 +=      (-y1 * dx)/dy; z1 +=      (-y1 * dz)/dy; y1 = 0; }
        else
          { x1 += ((yLast-y1)*dx)/dy; z1 += ((yLast-y1)*dz)/dy; y1 = yLast; }
        cc1 = clipCode(x1, y1);
      } else {
        if      ((cc2 & xLT) != 0)
          { y2 +=      (-x2 * dy)/dx; z2 +=      (-x2 * dz)/dx; x2 = 0; }
        else if ((cc2 & xGT) != 0)
          { y2 += ((xLast-x2)*dy)/dx; z2 += ((xLast-x2)*dz)/dx; x2 = xLast; }
        else if ((cc2 & yLT) != 0)
          { x2 +=      (-y2 * dx)/dy; z2 +=      (-y2 * dz)/dy; y2 = 0; }
        else
          { x2 += ((yLast-y2)*dx)/dy; z2 += ((yLast-y2)*dz)/dy; y2 = yLast; }
        cc2 = clipCode(x2, y2);
      }
    }
    if (tDotted)
      plotDottedLineDeltaUnclipped(argb, x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
    else
      plotLineDeltaUnclipped(argb, x1, y1, z1, x2 - x1, y2 - y1, z2 - z1);
  }

  final static int xLT = 8;
  final static int xGT = 4;
  final static int yLT = 2;
  final static int yGT = 1;

  private final int clipCode(int x, int y) {
    int code = 0;
    if (x < 0)
      code |= 8;
    else if (x >= g3d.width)
      code |= 4;

    if (y < 0)
      code |= 2;
    else if (y >= g3d.height)
      code |= 1;
    return code;
  }

  void plotLineDeltaUnclipped(int argb,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx;
      do {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = argb;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    do {
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] = argb;
      }
    } while (--n > 0);
  }

  void plotDottedLineDeltaUnclipped(int argb,
                                    int x1, int y1, int z1, int dx, int dy, int dz) {
    boolean flipFlop = true;
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx;
      do {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (flipFlop && zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = argb;
        }
        flipFlop = !flipFlop;
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    do {
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (flipFlop && zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] = argb;
      }
      flipFlop = !flipFlop;
    } while (--n > 0);
  }

  void plotLineDeltaUnclippedGradient(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;

    int r1 = (argb1 >> 16) & 0xFF;
    int g1 = (argb1 >> 8) & 0xFF;
    int b1 = argb1 & 0xFF;
    int r2 = (argb2 >> 16) & 0xFF;
    int g2 = (argb2 >> 8) & 0xFF;
    int b2 = argb2 & 0xFF;
    int dr = r2 - r1;
    int dg = g2 - g1;
    int db = b2 - b1;
    int rScaled = r1 << 10;
    int gScaled = g1 << 10;
    int bScaled = b1 << 10;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx;
      int nTransition = n >> 2;
      int nColor2 = (n - nTransition) / 2;
      int nColor1 = n - nColor2;
      if (nTransition <= 0)
        nTransition = 1;
      int drScaled = (dr << 10) / nTransition;
      int dgScaled = (dg << 10) / nTransition;
      int dbScaled = (db << 10) / nTransition;
      do {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] =
            (n > nColor1) ? argb1 :
            (n <= nColor2) ? argb2 :
            0xFF000000 | 
            ((rScaled << 6) & 0x00FF0000) |
            ((gScaled >> 2) & 0x0000FF00) |
            (bScaled >> 10);
        }
        if (n <= nColor1) {
          rScaled += drScaled;
          gScaled += dgScaled;
          bScaled += dbScaled;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    int nTransition = n >> 2;
    int nColor2 = (n - nTransition) / 2;
    int nColor1 = n - nColor2;
    if (nTransition <= 0)
      nTransition = 1;
    int drScaled = (dr << 10) / nTransition;
    int dgScaled = (dg << 10) / nTransition;
    int dbScaled = (db << 10) / nTransition;
    do {
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] =
          (n > nColor1) ? argb1 :
          (n <= nColor2) ? argb2 :
          0xFF000000 | 
          ((rScaled << 6) & 0x00FF0000) |
          ((gScaled >> 2) & 0x0000FF00) |
          (bScaled >> 10);
      }
      if (n <= nColor1) {
        rScaled += drScaled;
        gScaled += dgScaled;
        bScaled += dbScaled;
      }
    } while (--n > 0);
  }

  void plotLineDeltaUnclipped(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx, nMid = n / 2;
      do {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy, nMid = n / 2;
    do {
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] = n > nMid ? argb1 : argb2;
      }
    } while (--n > 0);
  }

  void plotLineDeltaClipped(int argb1, int argb2,
                            int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int height = g3d.height;
    int offset = y1 * width + x1;
    if (x1 >= 0 && x1 < width &&
        y1 >= 0 && y1 < height) {
      if (z1 < zbuf[offset]) {
        zbuf[offset] = (short)z1;
        pbuf[offset] = argb1;
      }
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1;
    int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx, nMid = n / 2;
      do {
        xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy, nMid = n / 2;
    do {
      yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      if (xCurrent >= 0 && xCurrent < width &&
          yCurrent >= 0 && yCurrent < height) {
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
      }
    } while (--n > 0);
  }
}
