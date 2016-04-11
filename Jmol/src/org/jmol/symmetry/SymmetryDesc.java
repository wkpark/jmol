/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import org.jmol.util.Logger;

/**
 * A class to handle requests for information; removed from coresym.js because
 * it is not often used
 * 
 */
public class SymmetryDesc {

  public SymmetryDesc() {
    // for reflection
  }

  private final static String[] keys = { "xyz", "xyzOriginal", "label",
      null /*draw*/, "fractionalTranslation", "cartesianTranslation",
      "inversionCenter", null /*point*/, "axisVector", "rotationAngle", "matrix",
      "unitTranslation", "centeringVector", "timeReversal", "plane" };

  /**
   * 
   * @param op
   * @param uc
   * @param pta00
   *        optional initial atom point
   * @param ptTarget
   *        optional target atom point
   * @param id
   * @param modelSet 
   * @param scaleFactor
   *        scale for rotation vector only
   * @return Object[] containing:
   * 
   *         [0] xyz (Jones-Faithful calculated from matrix)
   * 
   *         [1] xyzOriginal (Provided by calling method)
   * 
   *         [2] info ("C2 axis", for example)
   * 
   *         [3] draw commands
   * 
   *         [4] translation vector (fractional)
   * 
   *         [5] translation vector (Cartesian)
   * 
   *         [6] inversion point
   * 
   *         [7] axis point
   * 
   *         [8] axis vector (defines plane if angle = 0
   * 
   *         [9] angle of rotation
   * 
   *         [10] matrix representation
   * 
   *         [11] lattice translation 
   * 
   *         [12] centering
   * 
   *         [13] time reversal
   *         
   *         [14] plane
   * 
   */

  private Object[] getDescription(SymmetryOperation op, SymmetryInterface uc,
                                  P3 pta00, P3 ptTarget, String id,
                                  ModelSet modelSet, float scaleFactor) {

    if (!op.isFinalized)
      op.doFinalize();
    if (scaleFactor == 0)
      scaleFactor = 1f;
    V3 vtemp = new V3();
    P3 ptemp = new P3();
    P3 ptemp2 = new P3();
    P3 pta01 = new P3();
    P3 pta02 = new P3();
    V3 ftrans = new V3();
    V3 vtrans = new V3();
    P4 plane = null;

    if (pta00 == null || Float.isNaN(pta00.x))
      pta00 = new P3();
    if (ptTarget != null) {

      // Check to see that this is the correct operator

      setFractional(uc, pta00, pta01, ptemp);
      op.rotTrans(pta01);
      uc.toCartesian(pta01, false);
      uc.toUnitCell(pta01, ptemp);
      pta02.setT(ptTarget);
      uc.toUnitCell(pta02, ptemp);
      if (pta01.distance(pta02) > 0.1f)
        return null;

      // Check to see if the two points only differ by
      // a translation after transformation.
      // If so, add that difference to the matrix transformation

      setFractional(uc, pta00, pta01, null);
      op.rotTrans(pta01);
      setFractional(uc, ptTarget, pta02, null);
      vtrans.sub2(pta02, pta01);
    }

    // get the frame vectors and points

    pta01.set(1, 0, 0);
    pta02.set(0, 1, 0);
    P3 pta03 = P3.new3(0, 0, 1);
    pta01.add(pta00);
    pta02.add(pta00);
    pta03.add(pta00);

    // target point, rotated, inverted, and translated

    P3 pt0 = rotTransCart(op, uc, pta00, vtrans);
    P3 pt1 = rotTransCart(op, uc, pta01, vtrans);
    P3 pt2 = rotTransCart(op, uc, pta02, vtrans);
    P3 pt3 = rotTransCart(op, uc, pta03, vtrans);

    V3 vt1 = V3.newVsub(pt1, pt0);
    V3 vt2 = V3.newVsub(pt2, pt0);
    V3 vt3 = V3.newVsub(pt3, pt0);

    approx(vtrans);

    // check for inversion

    vtemp.cross(vt1, vt2);
    boolean haveInversion = (vtemp.dot(vt3) < 0);

    // The first trick is to check cross products to see if we still have a
    // right-hand axis.

    if (haveInversion) {

      // undo inversion for quaternion analysis (requires proper rotations only)

      pt1.sub2(pt0, vt1);
      pt2.sub2(pt0, vt2);
      pt3.sub2(pt0, vt3);

    }

    // The second trick is to use quaternions. Each of the three faces of the
    // frame (xy, yz, and zx)
    // is checked. The helix() function will return data about the local helical
    // axis, and the
    // symop(sym,{0 0 0}) function will return the overall translation.

    T3[] info = Measure.computeHelicalAxis(
        pta00,
        pt0,
        Quat.getQuaternionFrame(pt0, pt1, pt2).div(
            Quat.getQuaternionFrame(pta00, pta01, pta02)));
    // new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn), pt_b_prime };
    P3 pa1 = (P3) info[0];
    V3 ax1 = (V3) info[1];
    int ang1 = (int) Math.abs(PT.approx(((P3) info[3]).x, 1));
    float pitch1 = SymmetryOperation.approxF(((P3) info[3]).y);

    if (haveInversion) {

      // redo inversion

      pt1.add2(pt0, vt1);
      pt2.add2(pt0, vt2);
      pt3.add2(pt0, vt3);

    }

    V3 trans = V3.newVsub(pt0, pta00);
    if (trans.length() < 0.1f)
      trans = null;

    // ////////// determination of type of operation from first principles

    P3 ptinv = null; // inverted point for translucent frame
    P3 ipt = null; // inversion center
    P3 ptref = null; // reflection center

    boolean isTranslation = (ang1 == 0);
    boolean isRotation = !isTranslation;
    boolean isInversionOnly = false;
    boolean isMirrorPlane = false;

    if (isRotation || haveInversion) {
      // the translation will be taken care of other ways
      trans = null;
    }

    // handle inversion

    if (haveInversion && isTranslation) {

      // simple inversion operation

      ipt = P3.newP(pta00);
      ipt.add(pt0);
      ipt.scale(0.5f);
      ptinv = pt0;
      isInversionOnly = true;

    } else if (haveInversion) {

      /*
       * 
       * We must convert simple rotations to rotation-inversions; 2-screws to
       * planes and glide planes.
       * 
       * The idea here is that there is a relationship between the axis for a
       * simple or screw rotation of an inverted frame and one for a
       * rotation-inversion. The relationship involves two adjacent equilateral
       * triangles:
       * 
       * 
       *       o 
       *      / \
       *     /   \    i'
       *    /     \ 
       *   /   i   \
       * A/_________\A' 
       *  \         / 
       *   \   j   / 
       *    \     / 
       *     \   / 
       *      \ / 
       *       x
       *      
       * Points i and j are at the centers of the triangles. Points A and A' are
       * the frame centers; an operation at point i, j, x, or o is taking A to
       * A'. Point i is 2/3 of the way from x to o. In addition, point j is half
       * way between i and x.
       * 
       * The question is this: Say you have an rotation/inversion taking A to
       * A'. The relationships are:
       * 
       * 6-fold screw x for inverted frame corresponds to 6-bar at i for actual
       * frame 3-fold screw i for inverted frame corresponds to 3-bar at x for
       * actual frame
       * 
       * The proof of this follows. Consider point x. Point x can transform A to
       * A' as a clockwise 6-fold screw axis. So, say we get that result for the
       * inverted frame. What we need for the real frame is a 6-bar axis
       * instead. Remember, though, that we inverted the frame at A to get this
       * result. The real frame isn't inverted. The 6-bar must do that inversion
       * AND also get the frame to point A' with the same (clockwise) rotation.
       * The key is to see that there is another axis -- at point i -- that does
       * the trick.
       * 
       * Take a look at the angles and distances that arise when you project A
       * through point i. The result is a frame at i'. Since the distance i-i'
       * is the same as i-A (and thus i-A') and the angle i'-i-A' is 60 degrees,
       * point i is also a 6-bar axis transforming A to A'.
       * 
       * Note that both the 6-fold screw axis at x and the 6-bar axis at i are
       * both clockwise.
       * 
       * Similar analysis shows that the 3-fold screw i corresponds to the 3-bar
       * axis at x.
       * 
       * So in each case we just calculate the vector i-j or x-o and then factor
       * appropriately.
       * 
       * The 4-fold case is simpler -- just a parallelogram.
       */

      V3 d = (pitch1 == 0 ? new V3() : ax1);
      float f = 0;
      switch (ang1) {
      case 60: // 6_1 at x to 6-bar at i
        f = 2f / 3f;
        break;
      case 120: // 3_1 at i to 3-bar at x
        f = 2;
        break;
      case 90: // 4_1 to 4-bar at opposite corner
        f = 1;
        break;
      case 180: // 2_1 to mirror plane
        // C2 with inversion is a mirror plane -- but could have a glide
        // component.
        ptref = P3.newP(pta00);
        ptref.add(d);
        pa1.scaleAdd2(0.5f, d, pta00);
        if (ptref.distance(pt0) > 0.1f) {
          trans = V3.newVsub(pt0, ptref);
          setFractional(uc, trans, ptemp, null);
          ftrans.setT(ptemp);
        } else {
          trans = null;
        }
        isRotation = false;
        haveInversion = false;
        isMirrorPlane = true;
        break;
      default:
        haveInversion = false;
        break;
      }
      if (f != 0) {
        vtemp.sub2(pta00, pa1);
        vtemp.add(pt0);
        vtemp.sub(pa1);
        vtemp.sub(d);
        vtemp.scale(f);
        pa1.add(vtemp);
        ipt = new P3();
        ipt.scaleAdd2(0.5f, d, pa1);
        ptinv = new P3();
        ptinv.scaleAdd2(-2, ipt, pta00);
        ptinv.scale(-1);
      }

    } else if (trans != null) {

      // get rid of unnecessary translations added to keep most operations
      // within cell 555

      ptemp.setT(trans);
      uc.toFractional(ptemp, false);
      //      if (SymmetryOperation.approxF(ptemp.x) == 1)
      //        ptemp.x = 0;
      //      if (SymmetryOperation.approxF(ptemp.y) == 1)
      //        ptemp.y = 0;
      //      if (SymmetryOperation.approxF(ptemp.z) == 1)
      //        ptemp.z = 0;

      ftrans.setT(ptemp);
      uc.toCartesian(ptemp, false);
      trans.setT(ptemp);
    }

    // fix angle based on direction of axis

    int ang = ang1;
    approx0(ax1);

    if (isRotation) {

      P3 ptr = new P3();

      vtemp.setT(ax1);

      // draw the lines associated with a rotation

      int ang2 = ang1;
      if (haveInversion) {
        ptr.add2(pa1, vtemp);
        ang2 = Math.round(Measure.computeTorsion(ptinv, pa1, ptr, pt0, true));
      } else if (pitch1 == 0) {
        ptr.setT(pa1);
        ptemp.scaleAdd2(1, ptr, vtemp);
        ang2 = Math.round(Measure.computeTorsion(pta00, pa1, ptemp, pt0, true));
      } else {
        ptemp.add2(pa1, vtemp);
        ptr.scaleAdd2(0.5f, vtemp, pa1);
        ang2 = Math.round(Measure.computeTorsion(pta00, pa1, ptemp, pt0, true));
      }

      if (ang2 != 0)
        ang1 = ang2;

      if (!haveInversion
          && pitch1 == 0
          && (ax1.z < 0 || ax1.z == 0 && (ax1.y < 0 || ax1.y == 0 && ax1.x < 0))) {
        ax1.scale(-1);
        ang1 = -ang1;
      }

    }

    // time to get the description

    String info1 = "identity";

    if (isInversionOnly) {
      ptemp.setT(ipt);
      uc.toFractional(ptemp, false);
      info1 = "Ci: " + strCoord(ptemp, op.isBio);
    } else if (isRotation) {
      if (haveInversion) {
        info1 = "" + (360 / ang) + "-bar axis";
      } else if (pitch1 != 0) {
        info1 = "" + (360 / ang) + "-fold screw axis";
        ptemp.setT(ax1);
        uc.toFractional(ptemp, false);
        info1 += "|translation: " + strCoord(ptemp, op.isBio);
      } else {
        info1 = "C" + (360 / ang) + " axis";
      }
    } else if (trans != null) {
      String s = " " + strCoord(ftrans, op.isBio);
      if (isTranslation) {
        info1 = "translation:" + s;
      } else if (isMirrorPlane) {
        float fx = SymmetryOperation.approxF(ftrans.x);
        float fy = SymmetryOperation.approxF(ftrans.y);
        float fz = SymmetryOperation.approxF(ftrans.z);
        s = " " + strCoord(ftrans, op.isBio);
        if (fx != 0 && fy != 0 && fz != 0) {
          if (Math.abs(fx) == Math.abs(fy) && Math.abs(fy) == Math.abs(fz))
            info1 = "d-";
          else
            info1 = "g-"; // see group 167
        } else if (fx != 0 && fy != 0 || fy != 0 && fz != 0 || fz != 0
            && fx != 0)
          info1 = "n-";
        else if (fx != 0)
          info1 = "a-";
        else if (fy != 0)
          info1 = "b-";
        else
          info1 = "c-";
        info1 += "glide plane|translation:" + s;
      }
    } else if (isMirrorPlane) {
      info1 = "mirror plane";
    }

    if (haveInversion && !isInversionOnly) {
      ptemp.setT(ipt);
      uc.toFractional(ptemp, false);
      info1 += "|inversion center at " + strCoord(ptemp, op.isBio);
    }

    if (op.timeReversal == -1)
      info1 += "|time-reversed";

    String cmds = null;
    String xyzNew = (op.isBio ? op.xyzOriginal : SymmetryOperation
        .getXYZFromMatrix(op, false, false, false));

    // check for drawing

    if (id != null) {

      String drawid;
      String opType = null;
      drawid = "\ndraw ID " + id + "_";

      // delete previous elements of this user-settable ID

      SB draw1 = new SB();

      draw1.append(drawid).append("* delete");
      //    .append(
      //    ("print " + PT.esc(
      //        id + " " + (op.index + 1) + " " + op.fixMagneticXYZ(op, op.xyzOriginal, false) + "|"
      //            + op.fixMagneticXYZ(op, xyzNew, true) + "|" + info1).replace(
      //        '\n', ' '))).append("\n")

      // draw the initial frame

      drawLine(draw1, drawid + "frame1X", 0.15f, pta00, pta01, "red");
      drawLine(draw1, drawid + "frame1Y", 0.15f, pta00, pta02, "green");
      drawLine(draw1, drawid + "frame1Z", 0.15f, pta00, pta03, "blue");

      String color;

      if (isRotation) {

        P3 ptr = new P3();

        color = "red";

        ang = ang1;
        float scale = 1f;
        vtemp.setT(ax1);

        // draw the lines associated with a rotation

        if (haveInversion) {
          opType = drawid + "rotinv";
          ptr.add2(pa1, vtemp);
          if (pitch1 == 0) {
            ptr.setT(ipt);
            vtemp.scale(3 * scaleFactor);
            ptemp.scaleAdd2(-1, vtemp, pa1);
            draw1.append(drawid).append("rotVector2 diameter 0.1 ")
                .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
                .append(" color red");
          }
          scale = pt0.distance(ptr);
          draw1.append(drawid).append("rotLine1 ").append(Escape.eP(ptr))
              .append(Escape.eP(ptinv)).append(" color red");
          draw1.append(drawid).append("rotLine2 ").append(Escape.eP(ptr))
              .append(Escape.eP(pt0)).append(" color red");
        } else if (pitch1 == 0) {
          opType = drawid + "rot";
          boolean isSpecial = (pta00.distance(pt0) < 0.2f);
          if (!isSpecial) {
            draw1.append(drawid).append("rotLine1 ").append(Escape.eP(pta00))
                .append(Escape.eP(pa1)).append(" color red");
            draw1.append(drawid).append("rotLine2 ").append(Escape.eP(pt0))
                .append(Escape.eP(pa1)).append(" color red");
          }
          vtemp.scale(3 * scaleFactor);
          ptemp.scaleAdd2(-1, vtemp, pa1);
          draw1.append(drawid).append("rotVector2 diameter 0.1 ")
              .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
              .append(" color red");
          ptr.setT(pa1);
          if (pitch1 == 0 && pta00.distance(pt0) < 0.2)
            ptr.scaleAdd2(0.5f, ptr, vtemp);
        } else {
          opType = drawid + "screw";
          color = "orange";
          draw1.append(drawid).append("rotLine1 ").append(Escape.eP(pta00))
              .append(Escape.eP(pa1)).append(" color red");
          ptemp.add2(pa1, vtemp);
          draw1.append(drawid).append("rotLine2 ").append(Escape.eP(pt0))
              .append(Escape.eP(ptemp)).append(" color red");
          ptr.scaleAdd2(0.5f, vtemp, pa1);
        }

        // draw arc arrow

        ptemp.add2(ptr, vtemp);
        if (haveInversion && pitch1 != 0) {
          draw1.append(drawid).append("rotRotLine1").append(Escape.eP(ptr))
              .append(Escape.eP(ptinv)).append(" color red");
          draw1.append(drawid).append("rotRotLine2").append(Escape.eP(ptr))
              .append(Escape.eP(pt0)).append(" color red");
        }
        draw1
            .append(drawid)
            .append(
                "rotRotArrow arrow width 0.1 scale " + PT.escF(scale) + " arc ")
            .append(Escape.eP(ptr)).append(Escape.eP(ptemp));
        ptemp.setT(haveInversion ? ptinv : pta00);
        if (ptemp.distance(pt0) < 0.1f)
          ptemp.set((float) Math.random(), (float) Math.random(),
              (float) Math.random());
        draw1.append(Escape.eP(ptemp));
        ptemp.set(0, ang, 0);
        draw1.append(Escape.eP(ptemp)).append(" color red");

        // draw the main vector

        draw1.append(drawid).append("rotVector1 vector diameter 0.1 ")
            .append(Escape.eP(pa1)).append(Escape.eP(vtemp)).append("color ")
            .append(color);

      } else if (isMirrorPlane) {

        // lavender arrow across plane from pt00 to pt0

        if (pta00.distance(ptref) > 0.2)
          draw1.append(drawid).append("planeVector width 0.1 arrow ")
              .append(Escape.eP(pta00)).append(Escape.eP(ptref))
              .append(" color lavender");

        // faint inverted frame if mirror trans is not null

        opType = drawid + "plane";
        if (trans != null) {
          opType = drawid + "glide";
          drawFrameLine("X", ptref, vt1, 0.15f, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptref, vt2, 0.15f, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptref, vt3, 0.15f, ptemp, draw1, opType, "blue");
        }

        color = (trans == null ? "green" : "blue");

        // ok, now HERE's a good trick. We use the Marching Cubes
        // algorithm to find the intersection points of a plane and the unit
        // cell.
        // We expand the unit cell by 5% in all directions just so we are
        // guaranteed to get cutoffs.

        vtemp.setT(ax1);
        vtemp.normalize();
        // ax + by + cz + d = 0
        // so if a point is in the plane, then N dot X = -d
        float w = -vtemp.x * pa1.x - vtemp.y * pa1.y - vtemp.z * pa1.z;
        plane = P4.new4(vtemp.x, vtemp.y, vtemp.z, w);
        Lst<Object> v = new Lst<Object>();
        float margin = (Math.abs(w) < 0.01f && vtemp.x * vtemp.y > 0.4 ? 1.30f
            : 1.05f);
        v.addLast(uc.getCanonicalCopy(margin, false));
        modelSet.intersectPlane(plane, v, 3);

        // returns triangles and lines
        for (int i = v.size(); --i >= 0;) {
          P3[] pts = (P3[]) v.get(i);
          draw1.append(drawid).append("planep").appendI(i).append(" ")
              .append(Escape.eP(pts[0])).append(Escape.eP(pts[1]));
          if (pts.length == 3)
            draw1.append(Escape.eP(pts[2]));
          draw1.append(" color translucent ").append(color);
        }

        // and JUST in case that does not work, at least draw a circle

        if (v.size() == 0) {
          ptemp.add2(pa1, ax1);
          draw1.append(drawid).append("planeCircle scale 2.0 circle ")
              .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
              .append(" color translucent ").append(color).append(" mesh fill");
        }
      }

      if (haveInversion) {
        opType = drawid + "inv";
        draw1.append(drawid).append("invPoint diameter 0.4 ")
            .append(Escape.eP(ipt));
        draw1.append(drawid).append("invArrow width 0.1 arrow ")
            .append(Escape.eP(pta00)).append(Escape.eP(ptinv))
            .append(" color lavender");
        if (!isInversionOnly) {
          // n-bar: draw a faint frame showing the inversion
          drawFrameLine("X", ptinv, vt1, 0.15f, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptinv, vt2, 0.15f, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptinv, vt3, 0.15f, ptemp, draw1, opType, "blue");
        }
      }

      // and display translation if still not {0 0 0}

      if (trans != null) {
        if (ptref == null)
          ptref = P3.newP(pta00);
        draw1.append(drawid).append("transVector width 0.1 vector ")
            .append(Escape.eP(ptref)).append(Escape.eP(trans));
      }

      // draw the final frame just a bit fatter and shorter, in case they
      // overlap

      ptemp2.setT(pt0);
      ptemp.sub2(pt1, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2X", 0.2f, ptemp2, ptemp, "red");
      ptemp.sub2(pt2, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Y", 0.2f, ptemp2, ptemp, "green");
      ptemp.sub2(pt3, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Z", 0.2f, ptemp2, ptemp, "purple");

      // color the targeted atoms opaque and add another frame if necessary

      draw1.append("\nvar pt00 = " + Escape.eP(pta00));
      draw1.append("\nsym_point = pt00");
      draw1.append("\nvar p0 = " + Escape.eP(ptemp2));
      draw1
          .append("\nvar set2 = within(0.2,p0);if(!set2){set2 = within(0.2,p0.uxyz.xyz)}");
      draw1.append("\nsym_target = set2;if (set2) {");
      //      if (haveCentering)
      //      draw1.append(drawid).append(
      //        "cellOffsetVector arrow @p0 @set2 color grey");
      draw1.append(drawid)
          .append("offsetFrameX diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt1)).append("*0.9} color red");
      draw1.append(drawid)
          .append("offsetFrameY diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt2)).append("*0.9} color green");
      draw1.append(drawid)
          .append("offsetFrameZ diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt3)).append("*0.9} color purple");
      draw1.append("\n}\n");
      cmds = draw1.toString();
      if (Logger.debugging)
        Logger.info(cmds);
      draw1 = null;
      drawid = null;
    }
    if (trans == null)
      ftrans = null;
    if (isRotation) {
      if (haveInversion) {
      } else if (pitch1 == 0) {
      } else {
        // screw
        trans = V3.newV(ax1);
        ptemp.setT(trans);
        uc.toFractional(ptemp, false);
        ftrans = V3.newV(ptemp);
      }
    }
    if (isMirrorPlane) {
      ang1 = 0;
    }
    if (haveInversion) {
      if (isInversionOnly) {
        pa1 = null;
        ax1 = null;
        trans = null;
        ftrans = null;
      }
    } else if (isTranslation) {
      pa1 = null;
      ax1 = null;
    }

    // and display translation if still not {0 0 0}
    if (ax1 != null)
      ax1.normalize();
    M4 m2 = null;
    m2 = M4.newM4(op);
    if (vtrans.length() != 0) {
      m2.m03 += vtrans.x;
      m2.m13 += vtrans.y;
      m2.m23 += vtrans.z;
    }
    // TODO: here we need magnetic
    xyzNew = (op.isBio ? m2.toString() : op.modDim > 0 ? op.xyzOriginal
        : SymmetryOperation.getXYZFromMatrix(m2, false, false, false));
    if (op.timeReversal != 0)
      xyzNew = op.fixMagneticXYZ(m2, xyzNew, true);
    return new Object[] { 
        xyzNew, op.xyzOriginal, info1, cmds, 
        approx0(ftrans), approx0(trans), approx0(ipt), approx0(pa1), 
        plane == null ? approx0(ax1) : null, 
        ang1 != 0 ? Integer.valueOf(ang1) : null, m2, vtrans.lengthSquared() > 0 ? vtrans : null, op.getCentering(),
        Integer.valueOf(op.timeReversal), plane };
  }

  /**
   * Set pt01 to pt00, possibly adding offset into unit cell
   * 
   * @param uc
   * @param pt00
   * @param pt01
   * @param offset
   */
  private static void setFractional(SymmetryInterface uc, T3 pt00, P3 pt01,
                                    P3 offset) {
    pt01.setT(pt00);
    if (offset != null)
      uc.toUnitCell(pt01, offset);
    uc.toFractional(pt01, false);
  }

  private static void drawFrameLine(String xyz, P3 pt, V3 v, float width,
                                    P3 ptemp, SB draw1, String key, String color) {
    ptemp.setT(pt);
    ptemp.add(v);
    drawLine(draw1, key + "Pt" + xyz, width, pt, ptemp, "translucent " + color);
  }

  private static P3 rotTransCart(SymmetryOperation op, SymmetryInterface uc,
                                 P3 pt00, V3 vtrans) {
    P3 p0 = P3.newP(pt00);
    uc.toFractional(p0, false);
    op.rotTrans(p0);
    p0.add(vtrans);
    uc.toCartesian(p0, false);
    return p0;
  }

  private static String strCoord(T3 p, boolean isBio) {
    approx0(p);
    return (isBio ? p.x + " " + p.y + " " + p.z : SymmetryOperation.fcoord(p));
  }

  private static void drawLine(SB s, String id, float diameter, P3 pt0, P3 pt1,
                               String color) {
    s.append(id).append(" diameter ").appendF(diameter).append(Escape.eP(pt0))
        .append(Escape.eP(pt1)).append(" color ").append(color);
  }

  private static T3 approx0(T3 pt) {
    if (pt != null) {
      if (Math.abs(pt.x) < 0.0001f)
        pt.x = 0;
      if (Math.abs(pt.y) < 0.0001f)
        pt.y = 0;
      if (Math.abs(pt.z) < 0.0001f)
        pt.z = 0;
    }
    return pt;
  }

  private static T3 approx(T3 pt) {
    if (pt != null) {
      pt.x = SymmetryOperation.approxF(pt.x);
      pt.y = SymmetryOperation.approxF(pt.y);
      pt.z = SymmetryOperation.approxF(pt.z);
    }
    return pt;
  }

  private Object getSymmetryInfo(Symmetry sym, int iModel, int iAtom,
                                 Symmetry uc, String xyz, int op, P3 pt,
                                 P3 pt2, String id, int type,
                                 ModelSet modelSet, float scaleFactor, int nth) {
    if (type == T.lattice)
      return uc.getLatticeType();
    int iop = op;
    if (pt2 == null) {
      if (xyz == null) {
        SymmetryOperation[] ops = (SymmetryOperation[]) uc
            .getSymmetryOperations();
        if (ops == null || op == 0 || Math.abs(op) > ops.length)
          return (type == T.draw ? "draw ID sym_* delete" : "");
        iop = Math.abs(op) - 1;
        xyz = ops[iop].xyz;
      } else {
        iop = op = 0;
      }
    } else if (type == T.draw) {
      return getSymmetryInfoObject(sym, iModel, op, pt, pt2,
          (id == null ? "sym" : id), "all", modelSet, scaleFactor, nth, true);
    }
    Object[] info = null;

    if (pt2 == null) {
      SymmetryInterface symTemp = modelSet.getSymTemp(true);
      symTemp.setSpaceGroup(false);
      boolean isBio = uc.isBio();
      int i = (isBio ? symTemp.addBioMoleculeOperation(
          uc.spaceGroup.finalOperations[iop], op < 0) : symTemp
          .addSpaceGroupOperation((op < 0 ? "!" : "=") + xyz, Math.abs(op)));

      if (i < 0)
        return "";
      SymmetryOperation opTemp = (SymmetryOperation) symTemp
          .getSpaceGroupOperation(i);
      opTemp.index = op - 1;
      if (!isBio)
        opTemp.getCentering();
      if (pt != null || iAtom >= 0)
        pt = P3.newP(pt == null ? modelSet.at[iAtom] : pt);
      if (type == T.point) {
        if (isBio)
          return "";
        symTemp.setUnitCell(uc.getUnitCellParams(), false);
        uc.toFractional(pt, false);
        if (Float.isNaN(pt.x))
          return "";
        P3 sympt = new P3();
        symTemp.newSpaceGroupPoint(i, pt, sympt, 0, 0, 0);
        symTemp.toCartesian(sympt, false);
        return sympt;
      }
      info = getDescription(opTemp, uc, pt, pt2, (id == null ? "sym" : id),
          modelSet, scaleFactor);
    } else {
      boolean isList = (type == T.list);
      nth = (nth != 0 ? nth : isList ? -1 : 1);
      Object ret = getSymmetryInfoObject(sym, iModel, op, pt, pt2, null,
          isList ? null : "info", modelSet, scaleFactor, nth, isList);
      if (isList)
        return ret;
      info = (Object[]) ret;
    }
    if (info == null)
      return "";
    /*
     *  xyz (Jones-Faithful calculated from matrix)
     *  xyzOriginal (Provided by operation) 
     *  description ("C2 axis", for example) 
     *  translation vector (fractional)  
     *  translation vector (cartesian)
     *  inversion point 
     *  axis point 
     *  axis vector
     *  angle of rotation
     *  matrix representation
     *  time reversal
     */
    switch (type) {
    case T.array:
      Map<String, Object> lst = new Hashtable<String, Object>();
      for (int j = 0, n = info.length; j < n; j++)
        if (keys[j] != null && info[j] != null)
          lst.put(keys[j], info[j]);
      return lst;
      //    case T.list:
      //      String[] sinfo = new String[] {
      //          (String) info[0],
      //          (String) info[1],
      //          (String) info[2],
      //          // skipping DRAW commands here
      //          Escape.eP((V3) info[4]), Escape.eP((V3) info[5]),
      //          Escape.eP((P3) info[6]), Escape.eP((P3) info[7]),
      //          Escape.eP((V3) info[8]), "" + info[9], "" + Escape.e(info[10]),
      //          "" + info[13] };
      //      return sinfo;
    case T.angle:
      return info[9];
    case T.axes:
      return info[8];
    case T.center:
      return info[6];
    case T.draw:
      return info[3] + "\nprint " + PT.esc(info[0] + " " + info[2]);
    case T.full:
      return info[0] + "  \t" + info[2];
    case T.info:
      return info; // internal only
    default:
    case T.label:
      return info[2];
    case T.matrix4f:
      return info[10];
    case T.point:
      return info[7];
      // 11: vTrans
      // 12: centering
    case T.times:
      return info[13];
    case T.plane:
      return info[14];
    case T.translation:
      return info[5]; // cartesian translation
    case T.unitcell:
      return info[11];
    case T.xyz:
      return info[0];
    }
  }

  Object getSymmetryInfoObject(Symmetry sym, int modelIndex, int symOp, P3 pt1,
                               P3 pt2, String drawID, String type,
                               ModelSet modelSet, float scaleFactor, int nth,
                               boolean asString) {
    String strOperations = "";
    Object[][] infolist;
    Object ret = (asString ? "" : null);
    if (type != null && type.indexOf("y") >= 0) {
      // SHOW SYMOP "x,y,...."
      sym = (Symmetry) modelSet.getSymTemp(true);
      sym.setSpaceGroup(false);
      String xyz = type;
      int pt = xyz.indexOf(":");
      if (pt >= 0) {
        type = xyz.substring(pt + 1);
        xyz = xyz.substring(0, pt);
      }
      symOp = sym.addSpaceGroupOperation("=" + xyz, -1) + 1;
      if (symOp <= 0)
        return ret;
      //      SymmetryInterface cellInfo = modelSet.am[modelIndex].biosymmetry;
      //      if (cellInfo == null)
      //        cellInfo = modelSet.getUnitCell(modelIndex);
      //      if (cellInfo == null)
      //        return "";
      //      infolist = new Object[1][1];
      //      infolist[0] = getDescription(sym.spaceGroup.operations[i], cellInfo,
      //          null, null, null, null, 1, 0);

    }

    Map<String, Object> sginfo = getSpaceGroupInfo(sym, modelIndex, null,
        symOp, pt1, pt2, drawID, modelSet, scaleFactor, nth);
    if (sginfo == null)
      return ret;
    strOperations = (String) sginfo.get("symmetryInfo");
    infolist = (Object[][]) sginfo.get("operations");
    if (infolist == null)
      return ret;
    SB sb = new SB();
    symOp--;
    boolean labelOnly = "label".equals(type);
    boolean prettyMat = "fmatrix".equals(type);
    for (int i = 0; i < infolist.length; i++) {
      if (infolist[i] == null || symOp >= 0 && symOp != i)
        continue;
      if (!asString)
        return infolist[i];
      if (drawID != null)
        return ((String) infolist[i][3]) + "\nprint " + PT.esc(strOperations);
      if (sb.length() > 0)
        sb.appendC('\n');
      if (prettyMat) {
        SymmetryOperation.getPrettyMatrix(sb, (M4) infolist[i][10]);
        sb.appendC('\t');
      } else if (!labelOnly) {
        if (symOp < 0)
          sb.appendI(i + 1).appendC('\t');
        sb.append((String) infolist[i][0]).appendC('\t'); //xyz
      }
      sb.append((String) infolist[i][2]); //desc
    }
    if (sb.length() == 0)
      return (drawID != null ? "draw " + drawID + "* delete" : ret);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> getSpaceGroupInfo(Symmetry sym, int modelIndex,
                                        String sgName, int symOp, P3 pt1,
                                        P3 pt2, String drawID,
                                        ModelSet modelSet, float scaleFactor,
                                        int nth) {
    String strOperations = null;
    Map<String, Object> info = null;
    SymmetryInterface cellInfo = null;
    Object[][] infolist = null;
    boolean isStandard = true;
    if (sgName == null) {
      if (modelIndex <= 0)
        modelIndex = (pt1 instanceof Atom ? ((Atom) pt1).mi
            : modelSet.vwr.am.cmi);
      boolean isBio = false;
      if (modelIndex < 0)
        strOperations = "no single current model";
      else if (!(isBio = (cellInfo = modelSet.am[modelIndex].biosymmetry) != null)
          && (cellInfo = modelSet.getUnitCell(modelIndex)) == null)
        strOperations = "not applicable";
      if (strOperations != null) {
        info = new Hashtable<String, Object>();
        info.put("spaceGroupInfo", strOperations);
        info.put("symmetryInfo", "");
      } else if (pt1 == null && drawID == null && symOp != 0) {
        info = (Map<String, Object>) modelSet.getInfo(modelIndex,
            "spaceGroupInfo");
      }
      if (info != null)
        return info;
      info = new Hashtable<String, Object>();
      if (pt1 == null && drawID == null && symOp == 0)
        modelSet.setInfo(modelIndex, "spaceGroupInfo", info);
      sgName = cellInfo.getSpaceGroupName();
      SymmetryOperation[] ops = (SymmetryOperation[]) cellInfo
          .getSymmetryOperations();
      SpaceGroup sg = (isBio ? ((Symmetry) cellInfo).spaceGroup : null);
      String jf = "";
      int opCount = 0;
      if (ops != null) {
        isStandard = !isBio;
        if (isBio)
          sym.spaceGroup = SpaceGroup.getNull(false, false, false);
        else
          sym.setSpaceGroup(false);
        // check to make sure that new group has been created magnetic or not
        if (ops[0].timeReversal != 0)
          ((SymmetryOperation) sym.getSpaceGroupOperation(0)).timeReversal = 1;
        infolist = new Object[ops.length][];
        strOperations = "";
        for (int i = 0, nop = 0; i < ops.length && nop != nth; i++) {
          SymmetryOperation op = ops[i];
          int iop = (sym.getSpaceGroupOperation(i) != null ? i : isBio ? sym
              .addBioMoleculeOperation(sg.finalOperations[i], false) : sym
              .addSpaceGroupOperation("=" + op.xyz, i + 1));
          if (iop < 0)
            continue;
          op = (SymmetryOperation) sym.getSpaceGroupOperation(i);
          if (op.timeReversal != 0 || op.modDim > 0)
            isStandard = false;
          jf += ";" + op.xyz;
          Object[] ret = (symOp > 0 && symOp - 1 != iop ? null
              : getDescription(op, cellInfo, pt1, pt2, drawID, modelSet,
                  scaleFactor));
          if (ret != null) {
            if (nth > 0 && ++nop != nth)
              continue;
            infolist[i] = ret;
            strOperations += "\n" + (i + 1) + "\t" + ret[0] + "\t" + ret[2];
            opCount++;
          }
        }
      }
      if (opCount == 0)
        strOperations = "\n no symmetry operations";
      else
        strOperations = (nth < 0 && symOp <= 0 ? "\n" + opCount
            + " symmetry operation" + (opCount == 1 ? ":" : "s:") : "")
            + strOperations;
      jf = jf.substring(jf.indexOf(";") + 1);
      if (sgName.indexOf("[--]") >= 0)
        sgName = jf;
    } else {
      info = new Hashtable<String, Object>();
    }
    info.put("spaceGroupName", sgName);
    info.put("symmetryInfo", (infolist == null ? "" : strOperations));
    if (infolist != null)
      info.put("operations", infolist);
    String data;
    if (isStandard) {
      data = sym.getSpaceGroupInfoStr(sgName, cellInfo);
      if (data == null || data.equals("?"))
        data = "could not identify space group from name: " + sgName
            + "\nformat: show spacegroup \"2\" or \"P 2c\" "
            + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"";
    } else {
      data = sgName;
    }
    info.put("spaceGroupInfo", data);
    return info;
  }

  Object getSymmetryInfoAtom(BS bsAtoms, String xyz, int op, P3 pt, P3 pt2,
                             String id, int type, ModelSet modelSet,
                             float scaleFactor, int nth) {
    if (type == 0) {
      type = T.xyz;
      if (id.equalsIgnoreCase("matrix")) {
        type = T.matrix4f;
      } else if (id.equalsIgnoreCase("description")) {
        type = T.label;
      } else if (id.equalsIgnoreCase("axispoint")) {
        type = T.point;
      } else if (id.equalsIgnoreCase("time")) {
        type = T.times;
      } else if (id.equalsIgnoreCase("info")) {
        type = T.array;
      } else {
        // center, draw, plane, axis, translation, angle, array, list
        type = T.getTokFromName(id);
        if (type == 0)
          type = T.label;
      }
    }
    int iModel = -1;
    if (bsAtoms == null) {
      iModel = modelSet.vwr.am.cmi;
      if (iModel < 0)
        return "";
      bsAtoms = modelSet.vwr.getModelUndeletedAtomsBitSet(iModel);
    }
    int iAtom = bsAtoms.nextSetBit(0);
    if (iAtom < 0)
      return "";
    iModel = modelSet.at[iAtom].mi;
    SymmetryInterface uc = modelSet.am[iModel].biosymmetry;
    if (uc == null && (uc = modelSet.getUnitCell(iModel)) == null)
      return "";
    if (type != T.draw || op != Integer.MAX_VALUE)
      return getSymmetryInfo((Symmetry) uc, iModel, iAtom, (Symmetry) uc, xyz,
          op, pt, pt2, id, type, modelSet, scaleFactor, nth);
    String s = "";
    M4[] ops = uc.getSymmetryOperations();
    if (ops != null) {
      if (id == null)
        id = "sg";
      String desc = "";
      int n = ops.length;
      int nop = 0;
      for (op = 0; op < n; op++) { // no need to do op 1
        if (nth > 0 && nth != ++nop)
          continue;
        String cmds = (String) getSymmetryInfo((Symmetry) uc, iModel, iAtom,
            (Symmetry) uc, xyz, op, pt, pt2, id + op, type, modelSet,
            scaleFactor, nth);
        int p = cmds.indexOf("\n\n") + 1;
        desc += cmds.substring(0, p);
        s += cmds.substring(p);
      }
      s = desc + s;
      if (Logger.debugging)
        Logger.info(s);
    }
    return s;
  }

}
