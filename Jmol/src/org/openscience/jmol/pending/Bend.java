/*
 * @(#)Bend.java    1.0 98/12/18
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

package jmol;

class Bend {

    /** Bend
     *  class to do bend utilities for a bend with the central 
     *  node at atom b.
     */

    AtomType ata, atb, atc; 

    Bend(AtomType ata, AtomType atb, AtomType atc) {
        this.ata = ata;
        this.atb = atb;
        this.atc = atc;
    }        

    double getAngle(double[] qa, double[] qb, double[] qc) {
        double dx = qa[0] - qb[0];
        double dy = qa[1] - qb[1];
        double dz = qa[2] - qb[2];
        double gx = qc[0] - qb[0];
        double gy = qc[1] - qb[1];
        double gz = qc[2] - qb[2];
        double dx2 = dx*dx;
        double dy2 = dy*dy;
        double dz2 = dz*dz;
        double gx2 = gx*gx;
        double gy2 = gy*gy;
        double gz2 = gz*gz;
        double rab2 = dx2+dy2+dz2;
        double rcb2 = gx2+gy2+gz2;
        double rabi2 = 1.0 / rab2;
        double rcbi2 = 1.0 / rcb2;
        // Take dot product of bond vectors to get cos(theta)
        double dot = dx*gx + dy*gy + dz*gz;
        double denom = Math.sqrt(rabi2*rcbi2);
        double cosang = dot*denom;
        cosang = Math.min(cosang, 1.0);
        cosang = Math.max(cosang, -1.0);
        double angle = Math.acos(cosang);
        return angle;
    }
    
    float getAngle(float[] qa, float[] qb, float[] qc) {
        double da = getAngle((double) qa, (double) qb, (double) qc);
        return (float) da;
    }
    
}
