/*
 * @(#)Dihedral.java    1.0 99/08/13
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

class Dihedral extends Measurement implements MeasurementInterface {

    private int[] Atoms = new int[4];
    private double dihedral;
    private boolean computed = false;
    private ChemFrame fcf;

    public Dihedral(int a1, int a2, int a3, int a4) {
        super();
        Atoms[0] = a1;
        Atoms[1] = a2;
        Atoms[2] = a3;
        Atoms[3] = a4;
        compute();
    }
    
    public void paint(Graphics g, DisplaySettings settings, 
                      int x1, int y1, int z1, 
                      int x2, int y2, int z2, 
                      int x3, int y3, int z3,
                      int x4, int y4, int z4) throws Exception {
        paintDihedralLine(g, settings, x1, y1, x2, y2, x3, y3, x4, y4);
        paintDihedralString(g, settings, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
    }

    private void paintDihedralLine(Graphics g, DisplaySettings settings, 
                                int x1, int y1, 
                                int x2, int y2, 
                                int x3, int y3,
                                int x4, int y4) {
        int xa = (x1+x2)/2;
        int ya = (y1+y2)/2;
        int xb = (x3+x4)/2;
        int yb = (y3+y4)/2;

        g.setColor(settings.getDihedralColor());
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
    
    private void paintDihedralString(Graphics g, DisplaySettings settings, 
                                  int x1, int y1, int z1, 
                                  int x2, int y2, int z2,
                                  int x3, int y3, int z3,
                                  int x4, int y4, int z4) {

        Font font = new Font("Helvetica", Font.PLAIN, 
                             (int)(getAvgRadius(z1,z2,z3,z4)));
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics(font);
        g.setColor(settings.getTextColor());
        String s = (new Double(getDihedral())).toString();
        if (s.length() > 5) 
            s = s.substring(0,5);
        s = s + "\u00b0";
        int j = fontMetrics.stringWidth(s);

        int xloc = (x1 + x2 + x3 + x4)/4;
        int yloc = (y1 + y2 + y3 + y4)/4;
        
        g.drawString(s, xloc, yloc);        
    }
    
    private float getAvgRadius(int z1, int z2, int z3, int z4) {
        if (cf == null) return 0.0f;
        
        AtomType a = cf.getAtomType(Atoms[0]);
        AtomType b = cf.getAtomType(Atoms[1]);
        AtomType c = cf.getAtomType(Atoms[2]);
        AtomType d = cf.getAtomType(Atoms[3]);
        
        return (a.getCircleRadius(z1) + 
                b.getCircleRadius(z2) + 
                c.getCircleRadius(z3) +
                d.getCircleRadius(z4))/4.0f;
    }          

    public int[] getAtomList() {
        return Atoms;
    }

    public boolean sameAs(int a1, int a2, int a3, int a4) {
        /* Lazy way out. Just make sure that we're one of the 2
           ordered permutations. */
        if (Atoms[0]==a1 && Atoms[1]==a2 && Atoms[2]==a3 && Atoms[3]==a4) {
            return true;
        } else {
            if (Atoms[0]==a4 && Atoms[1]==a3 && Atoms[2]==a2 && Atoms[3]==a1) {
                return true;
            } 
        }
        return false;
    }

    public String toString() {
        return ("[" + Atoms[0] + "," + Atoms[1] + 
                "," + Atoms[2] + "," + Atoms[3] +
                " = " +  getDihedral() + "]");
    }

    public double getDihedral() {
        if (!computed || cf != fcf) compute();
        return dihedral;
    }
    
    public void compute() {

        if (cf == null) return;

        double[] c0 = cf.getVertCoords(Atoms[0]);
        double[] c1 = cf.getVertCoords(Atoms[1]);
        double[] c2 = cf.getVertCoords(Atoms[2]);
        double[] c3 = cf.getVertCoords(Atoms[3]);


        double ijx = c0[0]-c1[0];
        double ijy = c0[1]-c1[1];
        double ijz = c0[2]-c1[2];

        double kjx = c2[0]-c1[0];
        double kjy = c2[1]-c1[1];
        double kjz = c2[2]-c1[2];

        double klx = c2[0]-c3[0];
        double kly = c2[1]-c3[1];
        double klz = c2[2]-c3[2];

        double ax = ijy*kjz - ijz*kjy;
        double ay = ijz*kjx - ijx*kjz;
        double az = ijx*kjy - ijy*kjx;
        double cx = kjy*klz - kjz*kly;
        double cy = kjz*klx - kjx*klz;
        double cz = kjx*kly - kjy*klx;
       
        double ai2 = 1.0/(ax*ax + ay*ay + az*az);
        double ci2 = 1.0/(cx*cx + cy*cy + cz*cz);
        
        double ai = Math.sqrt(ai2);
        double ci = Math.sqrt(ci2);
        double denom = ai*ci;
        double cross = ax*cx + ay*cy + az*cz;
        double cosang = cross*denom;
        if (cosang > 1.0) cosang = 1.0;
        if (cosang < -1.0) cosang = -1.0;

        dihedral = toDegrees(Math.acos(cosang));        
        computed = true;
    }    
    public static double toDegrees(double angrad) {
        return angrad * 180.0 / Math.PI;
    }
}
