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

public class Cylinder25D {

  static int foo = 0;

  DisplayControl control;
  Graphics25D g25d;
  public Cylinder25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  void test(short colix1, short colix2,int diameter,
            int x, int y, int z, int dx, int dy, int dz) {
    CylinderShape cs = new CylinderShape(diameter,
                                         x, y, z,
                                         dx, dy, dz);
    cs.render(colix1, colix2, x, y, z, dx, dy, dz);
  }

  void renderClipped0(short colix, int diameter, int x, int y, int z,
            int dx, int dy, int dz) {
    CylinderShape cs = new CylinderShape(diameter,
                                         x, y, z,
                                         dx, dy, dz);
    cs.render(colix, colix, x, y, z, dx, dy, dz);
  }

  void renderClipped(short colix1, short colix2, int diameter,
                     int x1, int y1, int z1, int dx, int dy, int dz) {
    if (true)
      return;
    Sphere25D sphere25d = g25d.sphere25d;
    sphere25d.render(colix1, diameter, x1, y1, z1);
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1;
    int yIncrement = 1;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
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
        xCurrent += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        sphere25d.render(colix1, diameter, xCurrent, yCurrent, zCurrent);
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dy < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    do {
      yCurrent += yIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        xCurrent += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      sphere25d.render(colix1, diameter, xCurrent, yCurrent, zCurrent);
    } while (--n > 0);
  }

  class CylinderShape {
    int _2a;
    int _2b;
    int dxTheta;
    int dyTheta;
    int mag2d;

    boolean tLine;
    int x1, y1, x2, y2;
    int radius2; // radius squared
    int diameter;
    int dx, dy, dz;

    boolean tEven; // to apply correction

    short colix;
    int[] shades;

    long A, B, C, F;
    long twoA, twoB, twoC, _Adiv4, _Bdiv4;
    double scaleFactor;

    double radius3d2; // radius 3d squared

    int xN, yN, xNE, yNE, xE, yE, xSE, ySE, xS, yS;

    int ibRaster;
    byte[] raster; // this raster stores quads of <x,y,z,shade>

    int xOrigin, yOrigin, zOrigin;

    public CylinderShape(int diameter, int xOrigin, int yOrigin, int zOrigin,
                         int dx, int dy, int dz) {
      radius3d2 = (diameter*diameter) / 4.0;
      int mag2d2 = dx*dx + dy*dy;
      mag2d = (int)(Math.sqrt(mag2d2) + 0.5);
      int mag3d2 = mag2d2 + dz*dz;
      int mag3d = (int)(Math.sqrt(mag3d2) + 0.5);
      this._2a = this.diameter = diameter;
      tEven = (diameter & 1) == 0;
      this._2b = diameter - diameter * mag2d / mag3d;
      this.radius2 = ((diameter * diameter) + 2) / 4;

      this.dxTheta = this.dx = dx;
      this.dyTheta = -(this.dy = dy);
      this.dz = dz;

      this.xOrigin = xOrigin; this.yOrigin = yOrigin; this.zOrigin = zOrigin;

      /*
      System.out.println("origin="+xOrigin+","+yOrigin+","+zOrigin+
                         " diam=" + diameter);
      */
      if (_2b > 0) {
        calcEllipseFactors();
        calcCriticalPoints();
        // we need to check and see whether or not there is a tight
        // cluster of points. If so, then we will have problems, so
        // just draw a line
        if ((xE > xN) &&                   // not vertical
            !(xNE == xSE && yNE == ySE) && // not horizontal
            !(xNE == -xSE && yNE == -ySE)  // not horizontal
            ) {
          rasterizeEllipse();
          return;
        }
      }
      tLine = true;
      calcLineFactors();
    }

    void calcEllipseFactors() {
      int dx2 = dxTheta*dxTheta;
      int dy2 = dyTheta*dyTheta;
      int dx2_plus_dy2=dx2+dy2;

      long _4a2 = _2a*_2a;
      long _4b2 = _2b*_2b;

      long tmpA = (_4a2*dx2 + _4b2*dy2);
      long tmpB = (_4a2*dy2 + _4b2*dx2);
      long tmpC = -((_4a2 - _4b2)*(dxTheta*dyTheta));
      long tmpF = -(_4a2*_4b2) * dx2_plus_dy2;

      _Adiv4 = tmpA;
      A = tmpA * 4;
      twoA = A * 2;

      _Bdiv4 = tmpB;
      B = tmpB * 4;
      twoB = B * 2;

      C = tmpC * 4;
      twoC = C * 2;

      F = tmpF;

      scaleFactor = 4 * Math.sqrt(dx2_plus_dy2);

      /*
      System.out.println("ellipse0a");
      System.out.println(" _2a=" + _2a + " _2b=" + _2b);
      System.out.println("angle: " +
                         " A=" + A + " B=" + B + " C=" + C + " F=" + F +
                         " A/4=" + _Adiv4 + " B/4=" + _Bdiv4);
      */
    }

    void calcCriticalPoints() {
      //    g.setColor(transBlue);
      //    int yN = (int)Math.sqrt(A*F/(C*C-A*B));
      //    int xN = (int)(-C/A*yN);
      double sqrtA = Math.sqrt(A);
      double dblxN = -C/sqrtA;
      double dblyN = sqrtA;
      dblxN /= scaleFactor;
      dblyN /= scaleFactor;
      xN = (int)(dblxN + (dblxN < 0 ? -0.5 : 0.5));
      yN = (int)(dblyN + (dblyN < 0 ? -0.5 : 0.5));
      //System.out.println("N: " + dblxN + "," + dblyN + " " + xN + "," + yN);

      double sqrtAplusBminus2C = Math.sqrt(A + B - 2*C);
      double dblxNE = (B - C)/sqrtAplusBminus2C;
      double dblyNE = (A - C)/sqrtAplusBminus2C;
      dblxNE /= scaleFactor;
      dblyNE /= scaleFactor;
      xNE = (int)(dblxNE + (dblxNE < 0 ? -0.5 : 0.5));
      yNE = (int)(dblyNE + (dblyNE < 0 ? -0.5 : 0.5));
      //System.out.println("NE:" + dblxNE +","+ dblyNE + " " + xNE+"," + yNE);

      double sqrtB = Math.sqrt(B);
      double dblxE = sqrtB;
      dblxE /= scaleFactor;
      double dblyE = -C/sqrtB;
      dblyE /= scaleFactor;
      xE = (int)(dblxE + (dblxE < 0 ? -0.5 : 0.5));
      yE = (int)(dblyE + (dblyE < 0 ? -0.5 : 0.5));
      //System.out.println("E: " + dblxE + "," + dblyE + " " + xE + "," + yE);

      double sqrtAplusBplus2C = Math.sqrt(A + B + 2*C);
      double dblxSE = (B + C)/sqrtAplusBplus2C;
      dblxSE /= scaleFactor;
      double dblySE = -(A + C)/sqrtAplusBplus2C;
      dblySE /= scaleFactor;
      xSE = (int)(dblxSE + (dblxSE < 0 ? -0.5 : 0.5));
      ySE = (int)(dblySE + (dblySE < 0 ? -0.5 : 0.5));
      //System.out.println("SE:" + dblxSE +","+ dblySE + " " + xSE+"," + ySE);

      yS = -yN;
      xS = -xN;

      /*
      System.out.println("  N=" + xN  + "," + yN  +
                       " NE=" + xNE + "," + yNE +
                       "  E=" + xE  + "," + yE  +
                       " SE=" + xSE + "," + ySE +
                       "  S=" + xS  + "," + yS);
      */
    }

    long eval(int x, int y) {
      long n = A*x*x + B*y*y+2*C*x*y + F;
      System.out.println("eval " +x+"," +y + " -> " + n);
      return n;
    }

    long evalNextMidpointNNE(int x, int y) {
      long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y - 1)+ F +
        (twoA-C)*x + (twoC-B)*y + _Bdiv4;
      /*
      assert
        n == A*x*x + B*y*y + 2*C*x*y+ F + (2*A-C)*x + (2*C-B)*y + A - C + B/4;
      */
      return n;
    }

    long evalNextMidpointENE(int x, int y) {
      long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y - 1)+ F +
        (A-twoC)*x + (C-twoB)*y + _Adiv4;
      /*
      assert
        n == A*x*x + B*y*y + 2*C*x*y+ F + (A-2*C)*x + (C-2*B)*y + B - C + A/4;
      */
      return n;
    }

    long evalNextMidpointESE(int x, int y) {
      long n = A*(x*x) + B*(y*y + 1) + C*(2*x*y + 1)+ F +
        -(A+twoC)*x - (twoB+C)*y + _Adiv4;
      /*
      long m =
        A*x*x + B*y*y + 2*C*x*y+ F - (A+2*C)*x - (2*B+C)*y + B + C + A/4;
      assert m==n;
      */
      return n;
    }

    long evalNextMidpointSSE(int x, int y) {
      long n = A*(x*x + 1) + B*(y*y) + C*(2*x*y + 1)+ F +
        -(twoA+C)*x - (B+twoC)*y + _Bdiv4;
      /*
      assert
        n == A*x*x + B*y*y + 2*C*x*y+ F - (2*A+C)*x - (B+2*C)*y + A + C + B/4;
      */
      return n;
    }

    void allocRaster() {
      ibRaster = 0;
      raster = new byte[_2a * 4 * 2];
    }

    void reallocRaster() {
      byte[] t = new byte[2 * raster.length];
      System.arraycopy(raster, 0, t, 0, ibRaster);
      raster = t;
    }

    void rasterizeEllipse() {
      allocRaster();
      int xCurrent = xN;
      int yCurrent = yN;
      recordCoordinate(xCurrent, yCurrent);

      // NNE octant
      //assert xCurrent == xN && yCurrent == yN;
      if (xCurrent != xNE || yCurrent != yNE) {
        long discriminator = evalNextMidpointNNE(xN, yN);
        int xStop = xNE - 1;
        while (xCurrent < xStop && yCurrent > yNE) {
          ++xCurrent;
          if (discriminator < 0) {
            discriminator += A*(2*xCurrent + 1) + C*(2*yCurrent - 1);
          } else {
            recordCoordinate(xCurrent, yCurrent);
            --yCurrent;
            discriminator += (A-C)*(2*xCurrent + 1) + (C-B)*2*yCurrent;
          }
          //assert discriminator == evalNextMidpointNNE(xCurrent, yCurrent);
          recordCoordinate(xCurrent, yCurrent);
        }
        while (yCurrent > yNE || xCurrent < xNE) {
          if (yCurrent > yNE)
            --yCurrent;
          else if (xCurrent < xNE)
            ++xCurrent;
          recordCoordinate(xCurrent, yCurrent);
        }
      }


      // ENE octant
      //assert xCurrent == xNE && yCurrent == yNE;
      if (xCurrent != xE || yCurrent != yE) {
        long discriminator = evalNextMidpointENE(xNE, yNE);
        int yStop = yE + 1;
        while (yCurrent > yStop && xCurrent < xE) {
          --yCurrent;
          if (discriminator < 0) {
            recordCoordinate(xCurrent, yCurrent);
            ++xCurrent;
            discriminator += (A-C)*2*xCurrent + (C-B)*(2*yCurrent - 1);
          } else {
            discriminator += -( C*(2*xCurrent + 1) + B*(2*yCurrent - 1) );
          }
          //assert discriminator == evalNextMidpointENE(xCurrent, yCurrent);
          recordCoordinate(xCurrent, yCurrent);
        }
        while (yCurrent > yE || xCurrent < xE) {
          if (yCurrent > yE)
            --yCurrent;
          else if (xCurrent < xE)
            ++xCurrent;
          recordCoordinate(xCurrent, yCurrent);
        }
      }


      // ESE octant
      //assert xCurrent == xE && yCurrent == yE;
      if (xCurrent != xSE || yCurrent != ySE) {
        long discriminator = evalNextMidpointESE(xE, yE);
        int yStop = ySE + 1;
        while (yCurrent > yStop && xCurrent > xSE) {
          --yCurrent;
          if (discriminator < 0) {
            discriminator += -( C*(2*xCurrent - 1) + B*(2*yCurrent - 1) );
          } else {
            recordCoordinate(xCurrent, yCurrent);
            --xCurrent;
            discriminator += -( (A+C)*2*xCurrent + (B+C)*(2*yCurrent - 1) );
          }
          //assert discriminator == evalNextMidpointESE(xCurrent, yCurrent);
          recordCoordinate(xCurrent, yCurrent);
        }
        while (yCurrent > ySE || xCurrent > xSE) {
          if (yCurrent > ySE)
            --yCurrent;
          else if (xCurrent > xSE)
            --xCurrent;
          recordCoordinate(xCurrent, yCurrent);
        }
      }

      // SSE octant
      //assert xCurrent == xSE && yCurrent == ySE;
      if (xCurrent != xS || yCurrent != yS) {
        long discriminator = evalNextMidpointSSE(xSE, ySE);
        int xStop = xS + 1;
        while (xCurrent > xStop && yCurrent > yS) {
          --xCurrent;
          if (discriminator < 0) {
            recordCoordinate(xCurrent, yCurrent);
            --yCurrent;
            discriminator += -( (A+C)*(2*xCurrent - 1) + 2*(B+C)*yCurrent );
          } else {
            discriminator += -( A*(2*xCurrent - 1) + C*(2*yCurrent - 1) );
          }
          //assert discriminator == evalNextMidpointSSE(xCurrent, yCurrent);
          recordCoordinate(xCurrent, yCurrent);
        }
        while (xCurrent > xS || yCurrent > yS) {
          if (xCurrent > xS)
            --xCurrent;
          else if (yCurrent > yS)
            --yCurrent;
          recordCoordinate(xCurrent, yCurrent);
        }
      }

      //assert xCurrent == xS && yCurrent == yS;
    }

    void render(short colix1, short colix2,
                int x, int y, int z, int dx, int dy, int dz) {
      System.out.println("cylinder (" + x + "," + y + "," + z + ") -> ("
                         + dx + "," + dy + "," + dz + " diameter=" + diameter);
      int[] shades1 = Colix.getShades(colix1);
      int[] shades2 = Colix.getShades(colix2);
      if (tLine) {
        plotEdgewiseCylinder(shades1, shades2, x1, y1, x2, y2);
        return;
      }
      for (int i = 0; i < ibRaster; i += 4) {
        int x0 = raster[i];
        int y0 = raster[i+1];
        int z0 = raster[i+2];
        int intensity = raster[i+3];
        g25d.plotLineDelta(shades1[intensity], shades2[intensity],
                           //                           x + ((x0 > 0 && tEven) ? x0 - 1 : x0),
                           //                           y + ((y0 > 0 && tEven) ? y0 - 1 : y0),
                           x + x0,
                           y + y0,
                           z + z0,
                           dx, dy, dz);
        g25d.plotLineDelta(shades1[intensity], shades2[intensity],
                           //                           x - ((x0 < 0 && tEven) ? x0 + 1 : x0),
                           //                           y - ((y0 < 0 && tEven) ? y0 + 1 : y0),
                           x - x0,
                           y - y0,
                           z - z0,
                           dx, dy, dz);
      }
    }

    void recordCoordinate(int x, int y) {
      int z = 0;
      double t = radius3d2 - (x*x + y*y);
      if (t > 0)
        z = (int)(Math.sqrt(t) + 0.5);
      byte shade = Shade25D.getIntensity(x, y, z);
      //      System.out.println("record " + x + "," + y + "," + z);
      if (ibRaster == raster.length)
        reallocRaster();
      raster[ibRaster++] = (byte)x;
      raster[ibRaster++] = (byte)y;
      raster[ibRaster++] = (byte)z;
      raster[ibRaster++] = shade;
    }

    /****************************************************************
     * When the cylinder is laying in the plane, then the profile
     * is a line instead of an ellipse
     ****************************************************************/

    void calcLineFactors() {
      int radius = diameter / 2;
      /*
      System.out.println("dy=" + dy + " dx=" + dx + " mag2d=" + mag2d +
                         " diameter=" + diameter);
      */
      x1 = -(radius * -dy + -dy/2) / mag2d;
      y1 = -(radius * dx + dx/2) / mag2d;
      x2 = x1 + (diameter * -dy + -dy/2) / mag2d;
      y2 = y1 + (diameter * dx + dx/2) / mag2d;
    }

    int[] shades1;
    int[] shades2;

    void plotEdgewiseCylinder(int[] shades1, int[] shades2,
                              int x1, int y1, int x2, int y2) {
      System.out.println("plotEdgewiseCylinder (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2
                         + ")");
      this.shades1 = shades1;
      this.shades2 = shades2;

      int dx = x2 - x1;
      int dy = y2 - y1;

      if (dx == 0 && dy == 0)
        return;

      int xCurrent = x1;
      int yCurrent = y1;
      int xIncrement = 1;
      int yIncrement = 1;

      if (dx < 0) {
        dx = -dx;
        xIncrement = -1;
      }
      if (dy < 0) {
        dy = -dy;
        yIncrement = -1;
      }
      int twoDx = dx << 1, twoDy = dy << 1;

      if (dy <= dx) {
        int twoDxAccumulatedYError = 0;
        int n = dx;
        do {
          xCurrent += xIncrement;
          twoDxAccumulatedYError += twoDy;
          if (twoDxAccumulatedYError > dx) {
            plotEdgewise1(xCurrent, yCurrent);
            yCurrent += yIncrement;
            twoDxAccumulatedYError -= twoDx;
          }
          plotEdgewise1(xCurrent, yCurrent);
        } while (--n > 0);
        return;
      }
      int twoDyAccumulatedXError = 0;
      int n = dy;
      do {
        yCurrent += yIncrement;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          plotEdgewise1(xCurrent, yCurrent);
          xCurrent += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        plotEdgewise1(xCurrent, yCurrent);
      } while (--n > 0);
    }

    void plotEdgewise1(int x, int y) {
      int z2 = radius2 - (x*x + y*y);
      int z = (z2 <= 0) ? 0 : (int)(Math.sqrt(z2) + 0.5);

      byte intensity = Shade25D.getIntensity(x, y, z);
      g25d.plotLineDelta(shades1[intensity], shades2[intensity],
                         xOrigin + x, yOrigin + y, zOrigin - z, dx, dy, dz);
    }
  }

  int onTheSameSide(int xA, int yA, int xB, int yB, int x1, int y1, int x2, int y2) {
    int dyAB = yA - yB;
    int dxAB = xA - xB;
    int n1 = (x1 - xB)*dyAB - (y1 - yB)*dxAB;
    if (n1 == 0)
      return 0;
    int n2 = (x2 - xB)*dyAB - (y2 - yB)*dxAB;
    if (n2 == 0)
      return 0;
    return ((n1 > 0) ^ (n2 > 0)) ? 1 : -1;
  }
}
