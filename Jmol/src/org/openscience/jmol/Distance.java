/*
 * @(#)Distance.java    2.0 99/08/13
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

class Distance extends Measurement implements MeasurementInterface {

    private int[] Atoms = new int[2];
    private double distance;
    private boolean computed = false;
    private ChemFrame fcf;
    
    public Distance(int a1, int a2) {
        super();        
        Atoms[0] = a1;
        Atoms[1] = a2;
        compute();
    }
    
    public void paint(Graphics g, DisplaySettings settings, int x1, int y1, int z1, 
                      int x2, int y2, int z2) throws Exception {
        paintDistLine(g, settings, x1, y1, x2, y2);
        paintDistString(g, settings, x1, y1, z1, x2, y2, z2);       
    }

    private void paintDistLine(Graphics g, DisplaySettings settings, int x1, int y1, int x2, int y2) {
        g.setColor(settings.getDistanceColor());
        String vers = System.getProperty("java.version");
        if (vers.compareTo("1.2") >= 0) {
            Graphics2D g2 = (Graphics2D) g;
            BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_ROUND, 
                                                 BasicStroke.JOIN_ROUND, 0, 
                                                 new float[]{3,3}, 0);
            g2.setStroke(dotted);
            g2.drawLine(x1, y1, x2, y2);
        } else {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void paintDistString(Graphics g, DisplaySettings settings,
                                 int x1, int y1, int z1, 
                                 int x2, int y2, int z2) {
        
        double run = (double) (x2 - x1);
        double rise  = (double) (y2 - y1);
        double m = rise/run;
        Font font = new Font("Helvetica", Font.PLAIN, 
                             (int)(getAvgRadius(z1,z2)));
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics(font);
        g.setColor(settings.getTextColor());
        String s = (new Double(getDistance())).toString();
        if (s.length() > 5) 
            s = s.substring(0,5);
        s = s + " \u00c5";
        int j = fontMetrics.stringWidth(s);
        
        if (x2 == x1) {
            g.drawString(s, x1 + 1, ((y1+y2)/2) + 1 );
        } else {            
            g.drawString(s, (x1+x2)/2 - j - 1, (y1+y2)/2 - 1);
        }
    }   

    public float getAvgRadius(int z1, int z2) {
        if (cf == null) return 0.0f;
        
        AtomType a = cf.getAtomType(Atoms[0]);
        AtomType b = cf.getAtomType(Atoms[1]);
        
        return (a.getCircleRadius(z1) + b.getCircleRadius(z2))/2.0f;
    }          

    
    public int[] getAtomList() {
        return Atoms;
    }

    public boolean sameAs(int a1, int a2) {
        if (Atoms[0] == a1 && Atoms[1] == a2) {
            return true;
        } else {
            if (Atoms[0] == a2 && Atoms[1] == a1) {
                return true;
            } else {
                return false;
            }
        }
    }

    public String toString() {
        return ("[" + Atoms[0] + "," + Atoms[1] +  
                " = " +  getDistance() + "]");
    }
    
    public double getDistance() {
        if (!computed || cf != fcf) compute();
        return distance;
    }

    public void compute() {

        if (cf == null) return;

        double[] c0 = cf.getVertCoords(Atoms[0]);
        double[] c1 = cf.getVertCoords(Atoms[1]);

        double ax = c0[0]-c1[0];
        double ay = c0[1]-c1[1];
        double az = c0[2]-c1[2];

        double ax2 = ax*ax;
        double ay2 = ay*ay;
        double az2 = az*az;

        double rij2 = ax2+ay2+az2;
        
        distance = Math.sqrt(rij2);
        fcf = cf;
        computed = true;        
    }    
}
