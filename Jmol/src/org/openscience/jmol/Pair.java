/*
 * @(#)Pair.java    1.0 98/12/18
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

public class Pair {

    private AtomType ata, atb; 
    
    /** 
     * class to do pairwise-interaction utilities for two atoms
     * that are not necessarily (but possibly are) bonded.  
     */
    public Pair(AtomType ata, AtomType atb) {
        this.ata = ata;
        this.atb = atb;
    }        

    /** 
     * Returns the distance in molecule-space between two atoms
     * 
     * @param qax the x coordinate of atom A
     * @param qay the y coordinate of atom A
     * @param qaz the z coordinate of atom A
     * @param qbx the x coordinate of atom B
     * @param qby the y coordinate of atom B
     * @param qbz the z coordinate of atom B
     */   
    public float getDistance(float qax, float qay, float qaz, 
                             float qbx, float qby, float qbz) {
        float dx = qax - qbx;
        float dy = qay - qby;
        float dz = qaz - qbz;
        float dx2 = dx*dx;
        float dy2 = dy*dy;
        float dz2 = dz*dz;
        float rab2 = dx2+dy2+dz2;
        float dist = (float)Math.sqrt(rab2);
        return dist;
    }

    /**
     * Returns the average of the two screen radii in the pair
     * 
     * @param z1 the screen-transformed z coordinate of atom 1
     * @param z2 the screen-transformed z coordinate of atom 2
     */
    public float getAvgRadius(DisplaySettings settings, int z1, int z2) {
        return (settings.getCircleRadius(z1, ata.getBaseAtomType().getVdwRadius()) + settings.getCircleRadius(z2, atb.getBaseAtomType().getVdwRadius()))/2.0f;
    }          
}
