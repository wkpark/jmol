/*
 * @(#)ArrowLine.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import java.awt.*;
import javax.swing.*;

class ArrowLine {

    private static JPanel jpanel;
    private static float screenScale;
    private static Color vectorColor = Color.black;

    private static float RadiusScale = 1.0f;
    private static float LengthScale = 1.0f;

    private boolean aStart = false;
    private boolean aEnd = true;    

    private double length = -1;
    private double ctheta = 0;
    private double stheta = 0;
    
    private int x1, y1, x2, y2, r, s;
        
    static int[] xpoints = new int[4];
    static int[] ypoints = new int[4];

    
    static void setJPanel(JPanel jp) {
        jpanel = jp;
    }

    static void setVectorColor(Color c) {
        vectorColor = c;
    }

    static void setLengthScale(float ls) {
        LengthScale = ls;
    }

    static float getLengthScale() {
        return LengthScale;
    }

    static void setRadiusScale(float rs) {
        RadiusScale = rs;
    }

    static float getRadiusScale() {
        return RadiusScale;
    }

    public ArrowLine(Graphics gc, int x1, int y1, int x2, int y2,
                     boolean arrowStart, boolean arrowEnd, 
                     int radius, int size) {
        
        this.r = (int)(radius*RadiusScale);
        this.s = (int)(size*LengthScale);
        this.aStart = arrowStart;
        this.aEnd = arrowEnd;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        
        precalc();
        gc.setColor(vectorColor);
        paintLine(gc);
        paintArrows(gc);
        
    }


    private void precalc() {
        if (length<0) {
            double dy = (double)y2 - (double)y1;
            double dx = (double)x2 - (double)x1;
        
            length = Math.sqrt(Math.pow(dx,2.0) +
                               Math.pow(dy,2.0));

            if (length==0) return;
        
            ctheta = dx/length;
            stheta = dy/length; 
        }
    }

    public void paintLine(Graphics gc) {
        gc.drawLine(x1, y1, x2, y2);
    }
  
    public void paintStartArrowHead(Graphics gc) {
        double ax = x1 + 0.5;
        double ay = y1 + 0.5;
        double px = r + 1;
        double py = 0;
        double ly = s;
        double lx = r+s+s;
        double rx = lx;
        double ry = -ly;
        double mx = r+s+s/2;
        double my = 0;
        double tx; double ty;
        tx = px*ctheta-py*stheta;
        ty = px*stheta+py*ctheta;
        px = tx; py = ty;
        tx = lx*ctheta-ly*stheta;
        ty = lx*stheta+ly*ctheta;
        lx = tx; ly = ty;
        tx = rx*ctheta-ry*stheta;
        ty = rx*stheta+ry*ctheta;
        rx = tx; ry = ty;
        tx = mx*ctheta-my*stheta;
        ty = mx*stheta+my*ctheta;
        mx = tx; my = ty;
        px+=ax; py+=ay;
        mx+=ax; my+=ay;
        lx+=ax; ly+=ay;
        rx+=ax; ry+=ay;
        xpoints[0]=(int)px;
        xpoints[1]=(int)lx;
        xpoints[2]=(int)mx;
        xpoints[3]=(int)rx;
        ypoints[0]=(int)py;
        ypoints[1]=(int)ly;
        ypoints[2]=(int)my;
        ypoints[3]=(int)ry;
        gc.fillPolygon(xpoints,ypoints,4);    
    }

    public void paintEndArrowHead(Graphics gc) {
        double ax = x1+0.5;
        double ay = y1+0.5;
        double px = length-(r+1);
        double py = 0;
        double ly = s;
        double lx = length-(r+s+s);
        double rx = lx;
        double ry = -ly;
        double mx = length-(r+s+s/2);
        double my = 0;
        double tx; double ty;
        tx = px*ctheta-py*stheta;
        ty = px*stheta+py*ctheta;
        px = tx; py = ty;
        tx = lx*ctheta-ly*stheta;
        ty = lx*stheta+ly*ctheta;
        lx = tx; ly = ty;
        tx = rx*ctheta-ry*stheta;
        ty = rx*stheta+ry*ctheta;
        rx = tx; ry = ty;
        tx = mx*ctheta-my*stheta;
        ty = mx*stheta+my*ctheta;
        mx = tx; my = ty;
        px+=ax; py+=ay;
        mx+=ax; my+=ay;
        lx+=ax; ly+=ay;
        rx+=ax; ry+=ay;
        xpoints[0]=(int)px;
        xpoints[1]=(int)lx;
        xpoints[2]=(int)mx;
        xpoints[3]=(int)rx;
        ypoints[0]=(int)py;
        ypoints[1]=(int)ly;
        ypoints[2]=(int)my;
        ypoints[3]=(int)ry;
        gc.fillPolygon(xpoints,ypoints,4);    
    }
    
    public void paintArrows(Graphics gc) {
        if (length==0) return;
        if (aStart) {
            paintStartArrowHead(gc);
        }
        if (aEnd) {
            paintEndArrowHead(gc);
        }
    }  
}
