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
import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import java.util.Vector;

/****************************************************************
 * for some useful background info about hermite curves check out
 * http://www.cubic.org/~submissive/sourcerer/hermite.htm
 ****************************************************************/

class Hermite3D {

  Graphics3D g3d;

  Hermite3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  final Point3i[] pLeft = new Point3i[16];
  final Point3i[] pRight = new Point3i[16];

  final Point3f[] pTopLeft = new Point3f[16];
  final Point3f[] pTopRight = new Point3f[16];
  final Point3f[] pBotLeft = new Point3f[16];
  final Point3f[] pBotRight = new Point3f[16];
  final float[] sLeft = new float[16];
  final float[] sRight = new float[16];
  int sp;
  {
    for (int i = 16; --i >= 0; ) {
      pLeft[i] = new Point3i();
      pRight[i] = new Point3i();

      pTopLeft[i] = new Point3f();
      pTopRight[i] = new Point3f();
      pBotLeft[i] = new Point3f();
      pBotRight[i] = new Point3f();
    }
  }

  void render(boolean tFill, short colix, int tension,
                     int diameterBeg, int diameterMid, int diameterEnd,
                     Point3i p0, Point3i p1, Point3i p2, Point3i p3) {
    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ((x2 - p0.x) * tension) / 8;
    int yT1 = ((y2 - p0.y) * tension) / 8;
    int zT1 = ((z2 - p0.z) * tension) / 8;
    int xT2 = ((p3.x - x1) * tension) / 8;
    int yT2 = ((p3.y - y1) * tension) / 8;
    int zT2 = ((p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(p1);
    sRight[0] = 1;
    pRight[0].set(p2);
    sp = 0;
    int dDiameterFirstHalf = 0;
    int dDiameterSecondHalf = 0;
    if (tFill) {
      dDiameterFirstHalf = 2 * (diameterMid - diameterBeg);
      dDiameterSecondHalf = 2 * (diameterEnd - diameterMid);
    } else {
      g3d.setColix(colix);
    }
    do {
      Point3i a = pLeft[sp];
      Point3i b = pRight[sp];
      int dx = b.x - a.x;
      int dy = b.y - a.y;
      int dist2 = dx*dx + dy*dy;
      if (dist2 <= 2) {
        // mth 2003 10 13
        // I tried drawing short cylinder segments here,
        // but drawing spheres was faster
        float s = sLeft[sp];
        if (tFill) {
          int d =(s < 0.5f
                  ? diameterBeg + (int)(dDiameterFirstHalf * s)
                  : diameterMid + (int)(dDiameterSecondHalf * (s - 0.5f)));
          g3d.fillSphereCentered(colix, d, a);
        } else {
          g3d.plotPixelClipped(a);
        }
        --sp;
      } else {
        double s = (sLeft[sp] + sRight[sp]) / 2;
        double s2 = s * s;
        double s3 = s2 * s;
        double h1 = 2*s3 - 3*s2 + 1;
        double h2 = -2*s3 + 3*s2;
        double h3 = s3 - 2*s2 + s;
        double h4 = s3 - s2;
        Point3i pMid = pRight[sp+1];
        pMid.x = (int) (h1*x1 + h2*x2 + h3*xT1 + h4*xT2);
        pMid.y = (int) (h1*y1 + h2*y2 + h3*yT1 + h4*yT2);
        pMid.z = (int) (h1*z1 + h2*z2 + h3*zT1 + h4*zT2);
        pRight[sp+1] = pRight[sp];
        sRight[sp+1] = sRight[sp];
        pRight[sp] = pMid;
        sRight[sp] = (float)s;
        ++sp;
        pLeft[sp].set(pMid);
        sLeft[sp] = (float)s;
      }
    } while (sp >= 0);
  }

  public void render2x(boolean fill, short colix, int tension,
                      Point3i p0, Point3i p1, Point3i p2, Point3i p3,//top strand segment
                      Point3i p4, Point3i p5, Point3i p6, Point3i p7) {//bottom strand segment
    
    Point3i[] endPoints = {p2, p1, p6, p5};
    Vector points = new Vector(10); // stores all points for top+bottom strands of 1 segment
    int whichPoint = 0;

    int numTopStrandPoints = 2; //first and last points automatically included
    float numPointsPerSegment = 5.0f;//use 5 for mesh

    if (fill)
      numPointsPerSegment = 10.0f; // could make it so you can set this from script command


    float interval = (1.0f / numPointsPerSegment);
    float currentInt = 0.0f;

    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ( (x2 - p0.x) * tension) / 8;
    int yT1 = ( (y2 - p0.y) * tension) / 8;
    int zT1 = ( (z2 - p0.z) * tension) / 8;
    int xT2 = ( (p3.x - x1) * tension) / 8;
    int yT2 = ( (p3.y - y1) * tension) / 8;
    int zT2 = ( (p3.z - z1) * tension) / 8;
    sLeft[0] = 0;
    pLeft[0].set(p1);
    sRight[0] = 1;
    pRight[0].set(p2);
    sp = 0;
    //g3d.setColix(colix);

     for (int strands = 2; strands > 0; strands--) {
       if (strands == 1) {
         x1 = p5.x; y1 = p5.y; z1 = p5.z;
         x2 = p6.x; y2 = p6.y; z2 = p6.z;
         xT1 = ( (x2 - p4.x) * tension) / 8;
         yT1 = ( (y2 - p4.y) * tension) / 8;
         zT1 = ( (z2 - p4.z) * tension) / 8;
         xT2 = ( (p7.x - x1) * tension) / 8;
         yT2 = ( (p7.y - y1) * tension) / 8;
         zT2 = ( (p7.z - z1) * tension) / 8;
         sLeft[0] = 0;
         pLeft[0].set(p5);
         sRight[0] = 1;
         pRight[0].set(p6);
         sp = 0;
       }

       points.add(endPoints[whichPoint++]);
       currentInt = interval;
       do {
         Point3i a = pLeft[sp];
         Point3i b = pRight[sp];
         int dx = b.x - a.x;
         int dy = b.y - a.y;
         int dist2 = dx * dx + dy * dy;
         if (dist2 <= 2) {
           // mth 2003 10 13
           // I tried drawing short cylinder segments here,
           // but drawing spheres was faster
           float s = sLeft[sp];

           g3d.fillSphereCentered(colix, 3, a);
           //draw outside edges of mesh

           if (s < 1.0f - currentInt) { //if first point over the interval
             Point3i temp = new Point3i();
             temp.set(a);
             points.add(temp); //store it
             currentInt += interval; // increase to next interval
             if (strands == 2) {
               numTopStrandPoints++;
             }
           }
           --sp;
         }
         else {
           double s = (sLeft[sp] + sRight[sp]) / 2;
           double s2 = s * s;
           double s3 = s2 * s;
           double h1 = 2 * s3 - 3 * s2 + 1;
           double h2 = -2 * s3 + 3 * s2;
           double h3 = s3 - 2 * s2 + s;
           double h4 = s3 - s2;
           Point3i pMid = pRight[sp + 1];
           pMid.x = (int) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
           pMid.y = (int) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
           pMid.z = (int) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
           pRight[sp + 1] = pRight[sp];
           sRight[sp + 1] = sRight[sp];
           pRight[sp] = pMid;
           sRight[sp] = (float) s;
           ++sp;
           pLeft[sp].set(pMid);
           sLeft[sp] = (float) s;
         }
       } while (sp >= 0);
       points.add(endPoints[whichPoint++]);
     } //end of for loop - processed top and bottom strands
     int size = points.size();
     if (fill) {//RIBBONS
       Point3i t1 = null;
       Point3i b1 = null;
       Point3i t2 = null;
       Point3i b2 = null;
       int top = 1;

       for (;top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++) {
         t1 = (Point3i) points.elementAt(top - 1);
         b1 = (Point3i) points.elementAt(numTopStrandPoints + (top - 1));
         t2 = (Point3i) points.elementAt(top);
         b2 = (Point3i) points.elementAt(numTopStrandPoints + top);

         g3d.fillTriangle(colix, t1, b1, t2);
         g3d.fillTriangle(colix, b2, t2, b1);
       }
       if((numTopStrandPoints*2) != size){//BUG(DC09_MAY_2004): not sure why but
         //sometimes misses triangle at very start of segment
         //temp fix - will inestigate furture
         g3d.fillTriangle(colix, p1, p5, t2);
         g3d.fillTriangle(colix, b2, t2, p5);
       }
     }
     else {//MESH
       for (int top = 0;
            top < numTopStrandPoints && (top + numTopStrandPoints) < size; top++) {
       g3d.drawLine(colix, (Point3i) points.elementAt(top), (Point3i) points.elementAt(top + numTopStrandPoints));
     }}

  }

  static void set(Point3f p3f, Point3i p3i) {
    p3f.x = p3i.x;
    p3f.y = p3i.y;
    p3f.z = p3i.z;
  }

  public void render2(boolean fill, short colix, int tension,
                      Point3i p0, Point3i p1, Point3i p2, Point3i p3,//top strand segment
                      Point3i p4, Point3i p5, Point3i p6, Point3i p7) {//bottom strand segment
    
    int x1 = p1.x, y1 = p1.y, z1 = p1.z;
    int x2 = p2.x, y2 = p2.y, z2 = p2.z;
    int xT1 = ( (x2 - p0.x) * tension) / 8;
    int yT1 = ( (y2 - p0.y) * tension) / 8;
    int zT1 = ( (z2 - p0.z) * tension) / 8;
    int xT2 = ( (p3.x - x1) * tension) / 8;
    int yT2 = ( (p3.y - y1) * tension) / 8;
    int zT2 = ( (p3.z - z1) * tension) / 8;
    set(pTopLeft[0], p1);
    set(pTopRight[0], p2);

    int x5 = p5.x, y5 = p5.y, z5 = p5.z;
    int x6 = p6.x, y6 = p6.y, z6 = p6.z;
    int xT5 = ( (x6 - p4.x) * tension) / 8;
    int yT5 = ( (y6 - p4.y) * tension) / 8;
    int zT5 = ( (z6 - p4.z) * tension) / 8;
    int xT6 = ( (p7.x - x5) * tension) / 8;
    int yT6 = ( (p7.y - y5) * tension) / 8;
    int zT6 = ( (p7.z - z5) * tension) / 8;
    set(pBotLeft[0], p5);
    set(pBotRight[0], p6);

    sLeft[0] = 0;
    sRight[0] = 1;
    sp = 0;

    do {
      Point3f a = pTopLeft[sp];
      Point3f b = pTopRight[sp];
      double dxTop = b.x - a.x;
      if (dxTop >= -1 && dxTop <= 1) {
        double dyTop = b.y - a.y;
        if (dyTop >= -1 && dyTop <= 1) {
          Point3f c = pBotLeft[sp];
          Point3f d = pBotRight[sp];
          float dxBot = d.x - c.x;
          if (dxBot >= -1 && dxBot <= 1) {
            float dyBot = d.y - c.y;
            if (dyBot >= -1 && dyBot <= 1) {
              
              g3d.fillSphereCentered(colix, 3, a);
              g3d.fillSphereCentered(colix, 3, c);
              
              g3d.fillTriangle(colix, a, b, c);
              g3d.fillTriangle(colix, b, c, d);
              // render triangles here
              
              --sp;
              continue;
            }
          }
        }
      }
      double s = (sLeft[sp] + sRight[sp]) / 2;
      double s2 = s * s;
      double s3 = s2 * s;
      double h1 = 2 * s3 - 3 * s2 + 1;
      double h2 = -2 * s3 + 3 * s2;
      double h3 = s3 - 2 * s2 + s;
      double h4 = s3 - s2;

      Point3f pMidTop = pTopRight[sp + 1];
      pMidTop.x = (int) (h1 * x1 + h2 * x2 + h3 * xT1 + h4 * xT2);
      pMidTop.y = (int) (h1 * y1 + h2 * y2 + h3 * yT1 + h4 * yT2);
      pMidTop.z = (int) (h1 * z1 + h2 * z2 + h3 * zT1 + h4 * zT2);
      Point3f pMidBot = pBotRight[sp + 1];
      pMidBot.x = (float) (h1 * x5 + h2 * x6 + h3 * xT5 + h4 * xT6);
      pMidBot.y = (float) (h1 * y5 + h2 * y6 + h3 * yT5 + h4 * yT6);
      pMidBot.z = (float) (h1 * z5 + h2 * z6 + h3 * zT5 + h4 * zT6);
      
      pTopRight[sp + 1] = pTopRight[sp];
      pTopRight[sp] = pMidTop;
      pBotRight[sp + 1] = pBotRight[sp];
      pBotRight[sp] = pMidBot;
      
      sRight[sp + 1] = sRight[sp];
      sRight[sp] = (float) s;
      ++sp;
      pTopLeft[sp].set(pMidTop);
      pBotLeft[sp].set(pMidBot);
      sLeft[sp] = (float) s;
    } while (sp >= 0);
  }
}
