/*
 * (c) Copyright 1993, 1994, Silicon Graphics, Inc.
 * ALL RIGHTS RESERVED
 * Permission to use, copy, modify, and distribute this software for
 * any purpose and without fee is hereby granted, provided that the above
 * copyright notice appear in all copies and that both the copyright notice
 * and this permission notice appear in supporting documentation, and that
 * the name of Silicon Graphics, Inc. not be used in advertising
 * or publicity pertaining to distribution of the software without specific,
 * written prior permission.
 *
 * THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU "AS-IS"
 * AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR OTHERWISE,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL SILICON
 * GRAPHICS, INC.  BE LIABLE TO YOU OR ANYONE ELSE FOR ANY DIRECT,
 * SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY
 * KIND, OR ANY DAMAGES WHATSOEVER, INCLUDING WITHOUT LIMITATION,
 * LOSS OF PROFIT, LOSS OF USE, SAVINGS OR REVENUE, OR THE CLAIMS OF
 * THIRD PARTIES, WHETHER OR NOT SILICON GRAPHICS, INC.  HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS, HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE
 * POSSESSION, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 * US Government Users Restricted Rights
 * Use, duplication, or disclosure by the Government is subject to
 * restrictions set forth in FAR 52.227.19(c)(2) or subparagraph
 * (c)(1)(ii) of the Rights in Technical Data and Computer Software
 * clause at DFARS 252.227-7013 and/or in similar or successor
 * clauses in the FAR or the DOD or NASA FAR Supplement.
 * Unpublished-- rights reserved under the copyright laws of the
 * United States.  Contractor/manufacturer is Silicon Graphics,
 * Inc., 2011 N.  Shoreline Blvd., Mountain View, CA 94039-7311.
 *
 * OpenGL(TM) is a trademark of Silicon Graphics, Inc.
 */
/*
 * Trackball code:
 *
 * Implementation of a virtual trackball.
 * Implemented by Gavin Bell, lots of ideas from Thant Tessman and
 *   the August '88 issue of Siggraph's "Computer Graphics," pp. 121-129.
 *
 * Converted to Java by:
 * Dan Gezelter (gezelter@chem.columbia.edu)
 *
 * Vector manip code:
 *
 * Original code from:
 * David M. Ciemiewicz, Mark Grossman, Henry Moreton, and Paul Haeberli
 *
 * Much mucking with by:
 * Gavin Bell
 * 
 * Converted to Java by:
 * Dan Gezelter (gezelter@chem.columbia.edu)
 */

package jmol;

class Trackball {
    /*
     * This size should really be based on the distance from the center of
     * rotation to the point on the object underneath the mouse.  That
     * point would then track the mouse as closely as possible.  This is a
     * simple example, though, so that is left as an Exercise for the
     * Programmer.
     */
    private static final float TRACKBALLSIZE = 0.8f;
    private static final int RENORMCOUNT = 97;
    private static int count = 0;

    private void vzero(float[] v) {
        v[0] = 0.0f;
        v[1] = 0.0f;
        v[2] = 0.0f;
    }
    
    private void vset(float[] v, float x, float y, float z) {
        v[0] = x;
        v[1] = y;
        v[2] = z;
    }

    private void vsub(float[] src1, float[] src2, float[] dst) {
        dst[0] = src1[0] - src2[0];
        dst[1] = src1[1] - src2[1];
        dst[2] = src1[2] - src2[2];
    }

    private void vcopy(float[] v1, float[] v2) {
        for (int i = 0 ; i < 3 ; i++)
            v2[i] = v1[i];
    }
    
    private void vcross(float[] v1, float[] v2, float[] cross) {
        float temp[] = new float[3];        
        temp[0] = (v1[1] * v2[2]) - (v1[2] * v2[1]);
        temp[1] = (v1[2] * v2[0]) - (v1[0] * v2[2]);
        temp[2] = (v1[0] * v2[1]) - (v1[1] * v2[0]);
        System.arraycopy(temp, 0, cross, 0, temp.length);
    }

    private float vlength(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }
    
    private void vscale(float[] v, float div) {
        v[0] *= div;
        v[1] *= div;
        v[2] *= div;
    }

    private void vnormal(float[] v) {
        vscale(v,1.0f/vlength(v));
    }
    
    private float vdot(float[] v1, float[] v2) {
        return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2];
    }
    
    private void vadd(float[] src1, float[] src2, float[] dst) {
        dst[0] = src1[0] + src2[0];
        dst[1] = src1[1] + src2[1];
        dst[2] = src1[2] + src2[2];
    }
    
    Trackball(float[] q, float p1x, float p1y, float p2x, float p2y) {
        float a[] = new float[3]; /* Axis of rotation */
        float phi;  /* how much to rotate about axis */
        float p1[] = new float[3];
        float p2[] = new float[3];
        float d[] = new float[3];
        float t;
       
        if (p1x == p2x && p1y == p2y) {
            /* Zero rotation */
            vzero(q);
            q[3] = 1.0f;
            return;
        }

        /*
         * First, figure out z-coordinates for projection of P1 and P2 to
         * deformed sphere
         */
        vset(p1,p1x,p1y,tb_project_to_sphere(TRACKBALLSIZE,p1x,p1y));
        vset(p2,p2x,p2y,tb_project_to_sphere(TRACKBALLSIZE,p2x,p2y));

        /*
         *  Now, we want the cross product of P1 and P2
         */
        vcross(p2,p1,a);
        
        /*
         *  Figure out how much to rotate around that axis.
         */
        vsub(p1,p2,d);
        t = vlength(d) / (2.0f*TRACKBALLSIZE);
        
        /*
         * Avoid problems with out-of-control values...
         */
        if (t > 1.0f) t = 1.0f;
        if (t < -1.0f) t = -1.0f;
        phi = (float) (2.0 * Math.asin(t));
        
        axis_to_quat(a,phi,q);
    }

    private void axis_to_quat(float[] a, float phi, float[] q) {
        vnormal(a);
        vcopy(a,q);
        vscale(q,(float) Math.sin(phi/2.0));
        q[3] = (float) Math.cos(phi/2.0);
    }

    /*
     * Project an x,y pair onto a sphere of radius r OR a hyperbolic sheet
     * if we are away from the center of the sphere.
     */

    static float tb_project_to_sphere(float r, float x, float y) {
        float d, t, z;
        
        d = (float) Math.sqrt(x*x + y*y);
        if (d < r * 0.70710678118654752440f) {    /* Inside sphere */
            z = (float) Math.sqrt(r*r - d*d);
        } else {           /* On hyperbola */
            t = r / 1.41421356237309504880f;
            z = t*t / d;
        }
        return z;
    }
        
    /*
     * Given two rotations, e1 and e2, expressed as quaternion rotations,
     * figure out the equivalent single rotation and stuff it into dest.
     *
     * This routine also normalizes the result every RENORMCOUNT times it is
     * called, to keep error from creeping in.
     *
     * NOTE: This routine is written so that q1 or q2 may be the same
     * as dest (or each other).
     */
    
    public void add_quats (float[] q1, float[] q2, float[] d) {

        count = 0;
        float t1[] = new float[4];
        float t2[] = new float[4];
        float t3[] = new float[4];
        float t4[] = new float[4];
        float tf[] = new float[4];
        
        vcopy(q1, t1);
        vscale(t1,q2[3]);
        
        vcopy(q2,t2);
        vscale(t2,q1[3]);
        
        vcross(q2,q1,t3);
        vadd(t1,t2,tf);
        vadd(t3,tf,tf);
        tf[3] = q1[3] * q2[3] - vdot(q1,q2);
        
        d[0] = tf[0];
        d[1] = tf[1];
        d[2] = tf[2];
        d[3] = tf[3];
        
        if (++count > RENORMCOUNT) {
            count = 0;
            normalize_quat(d);
        }
    }

    /*
     * Quaternions always obey:  a^2 + b^2 + c^2 + d^2 = 1.0
     * If they don't add up to 1.0, dividing by their magnitued will
     * renormalize them.
     *
     * Note: See the following for more information on quaternions:
     *
     * - Shoemake, K., Animating rotation with quaternion curves, Computer
     *   Graphics 19, No 3 (Proc. SIGGRAPH'85), 245-254, 1985.
     * - Pletinckx, D., Quaternion calculus as a basic tool in computer
     *   graphics, The Visual Computer 5, 2-13, 1989.
     */
    static void normalize_quat(float[] q) {        
        float mag = (q[0]*q[0] + q[1]*q[1] + q[2]*q[2] + q[3]*q[3]);
        for (int i = 0; i < 4; i++) q[i] /= mag;
    }
    
    /*
     * Build a rotation matrix, given a quaternion rotation.
     *
     */
    void build_rotmatrix(Matrix3D m, float[] q) {
        
        m.xx = 1.0f - 2.0f * (q[1] * q[1] + q[2] * q[2]);
        m.xy = 2.0f * (q[0] * q[1] - q[2] * q[3]);
        m.xz = 2.0f * (q[2] * q[0] + q[1] * q[3]);
        m.xo = 0.0f;
        
        m.yx = 2.0f * (q[0] * q[1] + q[2] * q[3]);
        m.yy= 1.0f - 2.0f * (q[2] * q[2] + q[0] * q[0]);
        m.yz = 2.0f * (q[1] * q[2] - q[0] * q[3]);
        m.yo = 0.0f;
        
        m.zx = 2.0f * (q[2] * q[0] - q[1] * q[3]);
        m.zy = 2.0f * (q[1] * q[2] + q[0] * q[3]);
        m.zz = 1.0f - 2.0f * (q[1] * q[1] + q[0] * q[0]);
        m.zo = 0.0f;
        
    }

}   
