/*
 * @(#)Angle.java    1.0 99/08/13
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
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

class Angle extends Measurement implements MeasurementInterface {

    private int[] Atoms = new int[3];
    private double angle;
    private boolean computed = false;
    private ChemFrame fcf;

    public Angle(int a1, int a2, int a3) {
        super();
        Atoms[0] = a1;
        Atoms[1] = a2;
        Atoms[2] = a3;
        compute();
    }
    
    public void paint(Graphics g, 
                      int x1, int y1, int z1, 
                      int x2, int y2, int z2, 
                      int x3, int y3, int z3) throws Exception {
        paintAngleLine(g, x1, y1, x2, y2, x3, y3);
        paintAngleString(g, x1, y1, z1, x2, y2, z2, x3, y3, z3);        
    }

    private void paintAngleLine(Graphics g, 
                                int x1, int y1, 
                                int x2, int y2, 
                                int x3, int y3) {        
        int xa = (x1+x2)/2;
        int ya = (y1+y2)/2;
        int xb = (x3+x2)/2;
        int yb = (y3+y2)/2;

        g.setColor(DisplaySettings.getAngleColor());
        String vers = System.getProperty("java.version");
        if (vers.compareTo("1.2") >= 0) {
            Graphics2D g2 = (Graphics2D) g;
            BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_ROUND, 
                                                 BasicStroke.JOIN_ROUND, 0, 
                                                 new float[]{3,3}, 0);
            g2.setStroke(dotted);
            g2.drawLine(xa, ya, xb, yb);
        } else {
            g.drawLine(xa, ya, xb, yb);
        }
    }



    private void paintAngleString(Graphics g, 
                                  int x1, int y1, int z1, 
                                  int x2, int y2, int z2,
                                  int x3, int y3, int z3) {
        
        Font font = new Font("Helvetica", Font.PLAIN, 
                             (int)(getAvgRadius(z1,z2,z3)));
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics(font);
        g.setColor(DisplaySettings.getTextColor());
        String s = (new Double(getAngle())).toString();
        if (s.length() > 5) 
            s = s.substring(0,5);

        s = s + "\u00b0";
        int j = fontMetrics.stringWidth(s);

        int xloc = (2*x2 + x1 + x3)/4;
        int yloc = (2*y2 + y1 + y3)/4;
        
        g.drawString(s, xloc, yloc);
    }   

    public float getAvgRadius(int z1, int z2, int z3) {
        if (cf == null) return 0.0f;
        
        AtomType a = cf.getAtomType(Atoms[0]);
        AtomType b = cf.getAtomType(Atoms[1]);
        AtomType c = cf.getAtomType(Atoms[2]);
        
        return (a.getCircleRadius(z1) + 
                b.getCircleRadius(z2) + 
                c.getCircleRadius(z3))/3.0f;
    }          

    public int[] getAtomList() {
        return Atoms;
    }

    public boolean sameAs(int a1, int a2, int a3) {
        /* ordering is important in an angle measurement.  We can rule out a 
           match based on a2, and then permute a1 and a3 */
        if (Atoms[1] == a2) {
            if (Atoms[0] == a1 && Atoms[2] == a3) {
                return true;
            } else {
                if (Atoms[0] == a3 && Atoms[2] == a1) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public String toString() {
        return ("[" + Atoms[0] + "," + Atoms[1] + "," + Atoms[2] + 
                " = " +  getAngle() + "]");
    }

    public double getAngle() {
        if (!computed || cf != fcf) compute();
        return angle;
    }

    public void compute() {

        if (cf == null) return;

        double[] c0 = cf.getVertCoords(Atoms[0]);
        double[] c1 = cf.getVertCoords(Atoms[1]);
        double[] c2 = cf.getVertCoords(Atoms[2]);


        double ax = c0[0]-c1[0];
        double ay = c0[1]-c1[1];
        double az = c0[2]-c1[2];

        double bx = c2[0]-c1[0];
        double by = c2[1]-c1[1];
        double bz = c2[2]-c1[2];

        double ax2 = ax*ax;
        double ay2 = ay*ay;
        double az2 = az*az;

        double bx2 = bx*bx;
        double by2 = by*by;
        double bz2 = bz*bz;

        double rij2 = ax2+ay2+az2;
        double rkj2 = bx2+by2+bz2;

        double riji2 = 1.0/rij2;
        double rkji2 = 1.0/rkj2;

        double dot = ax*bx + ay*by + az*bz;
        double denom = Math.sqrt(riji2*rkji2);
        double cosang = dot*denom;
        if (cosang > 1.0) cosang = 1.0;
        if (cosang < -1.0) cosang = -1.0;

        angle = toDegrees(Math.acos(cosang));
        fcf = cf;
        computed = true;
    }    
    public static double toDegrees(double angrad) {
        return angrad * 180.0 / Math.PI;
    }
}
