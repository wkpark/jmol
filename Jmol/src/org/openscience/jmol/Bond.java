/*
 * @(#)Bond.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter & Michael Beachy All Rights
 * Reserved.
 *
 * J. Daniel Gezelter & Michael Beachy grant you ("Licensee") a 
 * non-exclusive, royalty free, license to use, modify and redistribute 
 * this software in source and binary code form, provided that the 
 * following conditions are met:
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
 * EXCLUDED.  J. DANIEL GEZELTER, MICHAEL BEACHY AND THEIR LICENSORS
 * SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A
 * RESULT OF USING, MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL J. DANIEL GEZELTER, MICHAEL BEACHY OR
 * THEIR LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR
 * FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
 * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE,
 * EVEN IF J. DANIEL GEZELTER OR MICHAEL BEACHY HAVE BEEN ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import java.util.*;
import java.awt.Graphics;
import java.awt.*;
import javax.swing.*;

public class Bond {

    private static JPanel jpanel;
    private static float screenScale;
    private static boolean bondsToAtomCenters = false;
    private static float Smoothness=0.7f;
    private static double bondwidth = 0.1;   /* static vars, once for each 
                                                class, not once per instance
                                                of class */   
    private AtomType at1, at2;
    private Color col1, col2;
    
    public static void setJPanel(JPanel jp) {
        jpanel = jp;
    }

    public static void setScreenScale(float ss) {
        screenScale = ss;
    }

    public static void setBondWidth(double bw) {
        bondwidth = bw;
    }

    public static double getBondWidth() {
        return bondwidth;
    }

    public static void toggleBondsToAtomCenters() {
        bondsToAtomCenters = !bondsToAtomCenters;
    }

    public static void setBondsToAtomCenters(boolean btac) {
        bondsToAtomCenters = btac;
    }

    public static boolean getBondsToAtomCenters() {
        return bondsToAtomCenters;
    }

    public Bond(AtomType at1, AtomType at2) {
        this.at1 = at1;
        this.at2 = at2;
        col1 = at1.getBaseAtomType().getColor();
        col2 = at2.getBaseAtomType().getColor();
    }        

    public void paint(Graphics gc, int x1, int y1, int z1, 
                      int x2, int y2, int z2) {
        paint(gc, x1, y1, z1, x2, y2, z2, false);
    }

    //useLine added by T.GREY- enforces line mode!
    public void paint(Graphics gc, int x1, int y1, int z1, 
                      int x2, int y2, int z2, boolean useLine) {
        
        int xmp = (x1+x2)/2;        
        int ymp = (y1+y2)/2;

        double r1, r2;
        int deltaX, deltaY;
	double tmp;

	double run = (double) (x2 - x1);
        double run2 = run*run;
	double rise  = (double) (y2 - y1);
        double rise2 = rise*rise;
        double zdiff = (double) (z2 - z1);
        double costheta = Math.sqrt(run2+rise2) /
            Math.sqrt(run2+rise2+zdiff*zdiff);

        //Added by T.GREY- fudges bonds to atom centres for quick draw mode
        if (bondsToAtomCenters || useLine) {
            r1 = 0.0;
            r2 = 0.0;
        } else {
            r1 = costheta*(double)at1.getCircleRadius(z1);
            r2 = costheta*(double)at2.getCircleRadius(z2);
        }

        double bl2 = run*run + rise*rise;

        double m = rise/run;
        
        if (bl2 <= (r1+r2)*(r1+r2)) return;

        double ddx1 = 0.0;
        double ddx2 = 0.0;
        
        int dx1 = 0;
        int dy1 = 0;
        int dx2 = 0;
        int dy2 = 0;
        
        if (x1 == x2) {
            if (y1 > y2) {
                dy1 = - (int) Math.round(r1);
                dy2 = (int) Math.round(r2);
            } else {
                dy1 = (int) Math.round(r1);
                dy2 = - (int) Math.round(r2);
            }            
        } else if (x1 >= x2) {
            ddx1 = - Math.sqrt(r1*r1/(1+m*m)); 
            ddx2 = - Math.sqrt(r2*r2/(1+m*m));
            dx1 = (int) Math.round(ddx1);
            dx2 = -(int) Math.round(ddx2);            
            dy1 = (int) Math.round(ddx1*m);
            dy2 = -(int) Math.round(ddx2*m);
        } else {
            ddx1 = Math.sqrt(r1*r1/(1+m*m)); 
            ddx2 = Math.sqrt(r2*r2/(1+m*m));
            dx1 = (int) Math.round(ddx1);
            dx2 = -(int) Math.round(ddx2);            
            dy1 = (int) Math.round(ddx1*m);
            dy2 = -(int) Math.round(ddx2*m);
        }

        // Duck out quickly if just line mode:
	if (DisplaySettings.getBondDrawMode() == DisplaySettings.LINE || useLine){
            gc.setColor(col1);
            gc.drawLine(x1+dx1, y1+dy1, xmp, ymp);
            gc.setColor(col2);
            gc.drawLine(x2+dx2, y2+dy2, xmp, ymp);
            return;
	}
        
        double halfbw = 0.5 * bondwidth * screenScale;

        if (halfbw >= 0.5) {
            
            int npoints = 4;
            int xpoints[] = new int[npoints];
            int ypoints[] = new int[npoints];
            
	    // slope of lines that make up the bond ends and midpoint line
	    double slope = -run/rise;

	    tmp=Math.sqrt(halfbw*halfbw/(1+slope*slope));
            	    
	    deltaX=(int) Math.round(tmp);
            deltaY= deltaX == 0 ? (int) Math.round(halfbw) : (int) Math.round(tmp*slope);

	    xpoints[0] = (x1+dx1) + deltaX;
	    ypoints[0] = (y1+dy1) + deltaY;
	    		 
	    xpoints[1] = (x1+dx1) - deltaX;
	    ypoints[1] = (y1+dy1) - deltaY;
	    		 
	    xpoints[2] = xmp - deltaX;
	    ypoints[2] = ymp - deltaY;
	    		 
	    xpoints[3] = xmp + deltaX;
	    ypoints[3] = ymp + deltaY;
            
            Polygon poly1 = new Polygon(xpoints, ypoints, 4);
                    
	    xpoints[0] = (x2+dx2) + deltaX;
	    ypoints[0] = (y2+dy2) + deltaY;
	    		 
	    xpoints[1] = (x2+dx2) - deltaX;
	    ypoints[1] = (y2+dy2) - deltaY;
	    
	    Polygon poly2 = new Polygon(xpoints, ypoints, 4);

            switch( DisplaySettings.getBondDrawMode() ) {
            case DisplaySettings.WIREFRAME:
                gc.setColor(col1);
                gc.drawPolygon(poly1);
                gc.setColor(col2);
                gc.drawPolygon(poly2);
                break;
            case DisplaySettings.SHADING:
                for (int i = (int)(2.0*halfbw); i > -1 ; i--) {
                    double len = i / (2.0*halfbw);
                    // System.out.println("len = " + len);
                    int R1 = (int) ((float)col1.getRed()   * (1.0f - len));
                    int G1 = (int) ((float)col1.getGreen() * (1.0f - len));
                    int B1 = (int) ((float)col1.getBlue()  * (1.0f - len));
                    // Bitwise masking to make color model:
                    int model1 = -16777216 | R1 << 16 | G1 << 8 | B1;

                    int R2 = (int) ((float)col2.getRed()   * (1.0f - len));
                    int G2 = (int) ((float)col2.getGreen() * (1.0f - len));
                    int B2 = (int) ((float)col2.getBlue()  * (1.0f - len));
                    // Bitwise masking to make color model:
                    int model2 = -16777216 | R2 << 16 | G2 << 8 | B2;

                    double dX= Math.round(tmp);            
                    double dY;
                    if ((int)dX == 0) {
                        dY = Math.round(halfbw);
                    } else {
                        dY = Math.round(tmp*slope);
                    }
                    int dXi = (int)(2.0*dX*len);
                    int dYi = (int)(2.0*dY*len);

                    gc.setColor(new Color(model1));

                    xpoints[0] = x1 + dx1 + dXi;
                    ypoints[0] = y1 + dy1 + dYi;
                    
                    xpoints[1] = x1 + dx1 - dXi;
                    ypoints[1] = y1 + dy1 - dYi;
                    
                    xpoints[2] = xmp - dXi;
                    ypoints[2] = ymp - dYi;
                    
                    xpoints[3] = xmp + dXi;
                    ypoints[3] = ymp + dYi;
                    
                    Polygon polya = new Polygon(xpoints, ypoints, 4);

                    gc.fillPolygon(polya);

                    //gc.drawLine(x1 + dx1 + dXi, y1 + dy1 + dYi,
                    //            xmp + dXi, ymp + dYi);
                    //gc.drawLine(xmp - dXi, ymp - dYi,
                    //            x1 + dx1 - dXi, y1 + dy1 - dYi);

                    gc.setColor(new Color(model2));

                    //gc.drawLine(x2 + dx2 + dXi, y2 + dy2 + dYi, 
                    //            xmp + dXi, ymp + dYi);
                    //gc.drawLine(xmp - dXi, ymp - dYi, 
                    //            x2 + dx2 - dXi, y2 + dy2 - dYi);

                    xpoints[0] = (x2+dx2) + dXi;
                    ypoints[0] = (y2+dy2) + dYi;
                    
                    xpoints[1] = (x2+dx2) - dXi;
                    ypoints[1] = (y2+dy2) - dYi;
                    
                    Polygon polyb = new Polygon(xpoints, ypoints, 4);
                    
                    gc.fillPolygon(polyb);

                }
                break;
            /* we already did the LINE mode above to save calculation time
             * so we don't need to do it here.
             */
            default:
                gc.setColor(col1);
                gc.fillPolygon(poly1);
                gc.setColor(DisplaySettings.getOutlineColor());
                gc.drawPolygon(poly1);
                gc.setColor(col2);
                gc.fillPolygon(poly2);
                gc.setColor(DisplaySettings.getOutlineColor());
                gc.drawPolygon(poly2);
                break;
            }

        } else {

            gc.setColor(col1);
            gc.drawLine(x1+dx1, y1+dy1, xmp, ymp);
            gc.setColor(col2);
            gc.drawLine(x2+dx2, y2+dy2, xmp, ymp);
        }                
    }    
}
