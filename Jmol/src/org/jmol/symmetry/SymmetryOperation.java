/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.api.SymmetryInterface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.viewer.Token;

/*
 * Bob Hanson 4/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 *
 */

class SymmetryOperation extends Matrix4f {
  String xyzOriginal;
  String xyz;
  boolean doNormalize = true;
  boolean isFinalized;

  SymmetryOperation() {
  }

  SymmetryOperation(boolean doNormalize) {
    this.doNormalize = doNormalize;
  }

  SymmetryOperation(SymmetryOperation op, Point3f[] atoms,
                           int atomIndex, int count, boolean doNormalize) {
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    this.doNormalize = doNormalize;
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    set(op); // sets the underlying Matrix4f
    doFinalize();
    if (doNormalize)
      setOffset(atoms, atomIndex, count);
  }

  void doFinalize() {
    m03 /= 12f;
    m13 /= 12f;
    m23 /= 12f;
    isFinalized = true;
  }
  
  String getXyz(boolean normalized) {
    return (normalized || xyzOriginal == null ? xyz : xyzOriginal);
  }

  private Point4f temp = new Point4f();
  void newPoint(Point3f atom1, Point3f atom2,
                       int transX, int transY, int transZ) {
    temp.set(atom1.x, atom1.y, atom1.z, 1);
    transform(temp, temp);
    atom2.set(temp.x + transX, temp.y + transY, temp.z + transZ);
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n"
        + ((Matrix4f) this).toString();
  }

  final static String dumpSeitz(Matrix4f s) {
    return (new StringBuffer("{\t")).append((int) s.m00).append("\t").append((int) s.m01)
        .append("\t").append((int) s.m02).append("\t").append(twelfthsOf(s.m03)).append("\t}\n")
        .append("{\t").append((int) s.m10).append("\t").append((int) s.m11).append("\t").append((int) s.m12)
        .append("\t").append(twelfthsOf(s.m13)).append("\t}\n")
        .append("{\t").append((int) s.m20).append("\t").append((int) s.m21).append("\t").append((int) s.m22)
        .append("\t").append(twelfthsOf(s.m23)).append("\t}\n").append("{\t0\t0\t0\t1\t}\n").toString();
  }
  
  final static String dumpCanonicalSeitz(Matrix4f s) {
    return (new StringBuffer()).append("{\t").append((int) s.m00).append("\t").append((int) s.m01)
        .append("\t").append((int) s.m02).append("\t").append(twelfthsOf(s.m03+12)).append("\t}\n")
        .append("{\t").append((int) s.m10).append("\t").append((int) s.m11).append("\t").append((int) s.m12)
        .append("\t").append(twelfthsOf(s.m13+12)).append("\t}\n").append("{\t").append((int) s.m20)
        .append("\t").append((int) s.m21).append("\t")
        .append((int) s.m22).append("\t").append(twelfthsOf(s.m23+12)).append("\t}\n")
        .append("{\t0\t0\t0\t1\t}\n").toString();
  }
  
  boolean setMatrixFromXYZ(String xyz) {
    /*
     * sets symmetry based on an operator string "x,-y,z+1/2", for example
     * 
     */
    if (xyz == null)
      return false;
    xyzOriginal = xyz;
    xyz = xyz.toLowerCase();
    float[] temp = new float[16];
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    char ch;
    int x = 0;
    int y = 0;
    int z = 0;
    float iValue = 0;
    String strOut = "";
    String strT;
    int rowPt = -1;
    temp[15] = 1;
    float decimalMultiplier = 1f;
    if (xyz.indexOf("xyz matrix: ") == 0) {
      /* note: these terms must in unit cell fractional coordinates!
       * CML matrix is in fractional coordinates, but do not take into accout
       * hexagonal systems. Thus, in wurtzite.cml, for P 6c 2'c:
       *
       * xyz matrix: 
       * 
       * -5.000000000000e-1  8.660254037844e-1  0.000000000000e0   0.000000000000e0 
       * -8.660254037844e-1 -5.000000000000e-1  0.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   1.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   0.000000000000e0   1.000000000000e0
       *
       * I see that these are "fractional coordinates" but they do not take into 
       * account the real xy transform, which is something like y,-x,z here.
       * 
       * I think it's a bug in the CML, actually. -- Bob Hanson 9/2008
       * 
       */
      this.xyz = xyz;
      Parser.parseFloatArray(xyz, null, temp);
      for (int i = 0; i < 16; i++) {
        if (Float.isNaN(temp[i]))
          return false;
        float v = temp[i];
        if (Math.abs(v) < 0.00001f)
          v = 0;
        if (i % 4 == 3)
          v = normalizeTwelfths((v < 0 ? -1 : 1) * (int)(Math.abs(v) * 12.001f));
        temp[i] = v;
      }
      return true;
    }
    
    xyz += ",";
    //Logger.debug(xyz.length() + " " + xyz);
    for (int i = 0; i < xyz.length(); i++) {
      ch = xyz.charAt(i);
      //Logger.debug("char = " + ch + isDecimal);
      switch (ch) {
      case '\'':
      case ' ':
      case '{':
      case '}':
      case '!':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        isDenominator = true;
        continue;
      case 'X':
      case 'x':
        x = (isNegative ? -1 : 1);
        break;
      case 'Y':
      case 'y':
        y = (isNegative ? -1 : 1);
        break;
      case 'Z':
      case 'z':
        z = (isNegative ? -1 : 1);
        break;
      case ',':
        if (++rowPt > 2) {
          Logger.warn("Symmetry Operation? " + xyz);
          return false;
        }
        int tpt = rowPt * 4;
        // put translation into 12ths
        iValue = normalizeTwelfths(iValue);
        temp[tpt++] = x;
        temp[tpt++] = y;
        temp[tpt++] = z;
        temp[tpt] = iValue;
        strT = "";
        strT += (x == 0 ? "" : x < 0 ? "-x" : strT.length() == 0 ? "x" : "+x");
        strT += (y == 0 ? "" : y < 0 ? "-y" : strT.length() == 0 ? "y" : "+y");
        strT += (z == 0 ? "" : z < 0 ? "-z" : strT.length() == 0 ? "z" : "+z");
        strT += xyzFraction(iValue, false);
        strOut += (strOut == "" ? "" : ",") + strT;
        //note: when ptLatt[3] = -1, ptLatt[rowPt] MUST be 0.
        if (rowPt == 2) {
          set(temp);
          this.xyz = strOut;
          if (Logger.debugging)
            Logger.debug("" + (Matrix4f)this);
          rowPt = 0;
          return true;
        }
        x = y = z = 0;
        iValue = 0;
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal)
          continue;
      //allow to pass through
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (isDecimal && ich >= 0 && ich <= 9) {
          decimalMultiplier /= 10f;
          if (iValue < 0)
            isNegative = true;
          iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
          continue;
        }
        if (ich >= 1 && ich <= 9) {
          if (isDenominator) {
            iValue /= ich;
          } else {
            iValue = (isNegative ? -1f : 1f) * ich;
          }
        } else {
          Logger.warn("symmetry character?" + ch);
        }
      }
      isDecimal = isDenominator = isNegative = false;
    }
    return false;
  }

  private float normalizeTwelfths(float iValue) {
    iValue *= 12f;
    if (doNormalize) {
      while (iValue > 6)
        iValue -= 12;
      while (iValue <= -6)
        iValue += 12;
    }
    return iValue;
  }

  final static String getXYZFromMatrix(Matrix4f mat, boolean allPositive) {
    String str = "";
    float[] row = new float[4];
    for (int i = 0; i < 3; i++) {
      mat.getRow(i, row);
      String term = "";
      if (row[0] != 0)
        term += (row[0] < 0 ? "-" : "+") + "x";
      if (row[1] != 0)
        term += (row[1] < 0 ? "-" : "+") + "y";
      if (row[2] != 0)
        term += (row[2] < 0 ? "-" : "+") + "z";
      term += xyzFraction(row[3], allPositive);
      if (term.length() > 0 && term.charAt(0) == '+')
        term = term.substring(1);
      str += "," + term;
    }
    return str.substring(1);
  }

  private final static String twelfthsOf(float n12ths) {
    String str = "";
    if (n12ths < 0) {
      str = "-";
      n12ths = -n12ths;
    }
    return str + twelfths[((int) n12ths) % 12];  
  }
  
  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
      "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  private final static String xyzFraction(float n12ths, boolean allPositive) {
    if (allPositive) {
      while (n12ths < 0)
        n12ths += 12f;
    } else if (n12ths > 6f) {
      n12ths -= 12f;
    }
    String s = twelfthsOf(n12ths);
    return (s.charAt(0) == '0' ? "" : n12ths > 0 ? "+" + s : s);
  }

  
  private static float approx(float f) {
    return approx(f, 100);
  }

  private static float approx(float f, float n) {
    return ((int) (f * n + 0.5f * (f < 0 ? -1 : 1)) / n);
  }

  public Object[] getDescription(SymmetryInterface uc, Point3f pt00, String id) {

    if (!isFinalized)
      doFinalize();
    Vector3f vtemp = new Vector3f();
    Point3f ptemp = new Point3f();
    Point3f ftrans = new Point3f();

    boolean typeOnly = (pt00 == null);
    if (typeOnly)
      pt00 = new Point3f();

    Point3f pt01 = new Point3f(1, 0, 0);
    Point3f pt02 = new Point3f(0, 1, 0);
    Point3f pt03 = new Point3f(0, 0, 1);
    pt01.add(pt00);
    pt02.add(pt00);
    pt03.add(pt00);

    Point3f p0 = new Point3f(pt00);
    Point3f p1 = new Point3f(pt01);
    Point3f p2 = new Point3f(pt02);
    Point3f p3 = new Point3f(pt03);

    uc.toFractional(p0);
    uc.toFractional(p1);
    uc.toFractional(p2);
    uc.toFractional(p3);
    transform(p0, p0);
    transform(p1, p1);
    transform(p2, p2);
    transform(p3, p3);
    uc.toCartesian(p0);
    uc.toCartesian(p1);
    uc.toCartesian(p2);
    uc.toCartesian(p3);

    Vector3f v01 = new Vector3f();
    v01.sub(p1, p0);
    Vector3f v02 = new Vector3f();
    v02.sub(p2, p0);
    Vector3f v03 = new Vector3f();
    v03.sub(p3, p0);

    vtemp.cross(v01, v02);
    boolean haveinversion = (vtemp.dot(v03) < 0);

    // The first trick is to check cross products to see if we still have a
    // right-hand axis.

    if (haveinversion) {

      // undo inversion for quaternion analysis (requires proper rotations only)

      p1.scaleAdd(-2, v01, p1);
      p2.scaleAdd(-2, v02, p2);
      p3.scaleAdd(-2, v03, p3);

    }

    // The second trick is to use quaternions. Each of the three faces of the
    // frame (xy, yz, and zx)
    // is checked. The helix() function will return data about the local helical
    // axis, and the
    // symop(sym,{0 0 0}) function will return the overall translation.

    Object[] info;
    info = (Object[]) Measure.computeHelicalAxis(null, Token.array, pt00, p0,
        Quaternion.getQuaternionFrame(p0, p1, p2).div(
            Quaternion.getQuaternionFrame(pt00, pt01, pt02)));
    Point3f pa1 = (Point3f) info[0];
    Vector3f ax1 = (Vector3f) info[1];
    int ang1 = (int) Math.abs(approx(((Point3f) info[3]).x, 1));
    float pitch1 = approx(((Point3f) info[3]).y);
    info = (Object[]) Measure.computeHelicalAxis(null, Token.array, pt00, p0,
        Quaternion.getQuaternionFrame(p0, p2, p3).div(
            Quaternion.getQuaternionFrame(pt00, pt02, pt03)));
    Vector3f ax2 = (Vector3f) info[1];
    int ang2 = (int) Math.abs(approx(((Point3f) info[3]).x, 1));

    info = (Object[]) Measure.computeHelicalAxis(null, Token.array, pt00, p0,
        Quaternion.getQuaternionFrame(p0, p3, p1).div(
            Quaternion.getQuaternionFrame(pt00, pt03, pt01)));
    Vector3f ax3 = (Vector3f) info[1];
    int ang3 = (int) Math.abs(approx(((Point3f) info[3]).x, 1));

    if (haveinversion) {

      // redo inversion

      p1.scaleAdd(2, v01, p1);
      p2.scaleAdd(2, v02, p2);
      p3.scaleAdd(2, v03, p3);

    }

    Vector3f trans = new Vector3f(p0);
    trans.sub(pt00);
    if (trans.length() < 0.1f)
      trans = null;

    // ////////// determination of type of operation from first principles

    boolean isinversion = false;
    boolean ismirrorplane = false;
    Point3f ptinv = null; // inverted point for translucent frame
    Point3f ipt = null; // inversion center
    Point3f pt0 = new Point3f(pt00); // reflection center

    boolean istranslation = (ang1 == 0 && ang2 == 0 && ang3 == 0);

    // unit axes

    Vector3f n1 = new Vector3f(ax1);
    Vector3f n2 = new Vector3f(ax2);
    Vector3f n3 = new Vector3f(ax3);
    n1.normalize();
    n2.normalize();
    n3.normalize();

    boolean isrotation = !istranslation && approx(Math.abs(n1.dot(n2))) == 1
        && approx(Math.abs(n2.dot(n3))) == 1
        && approx(Math.abs(n3.dot(n1))) == 1;

    if (isrotation || haveinversion)
      trans = null;

    // handle inversion

    if (haveinversion && istranslation) {

      // simple inversion operation

      ipt = new Point3f(pt00);
      ipt.add(p0);
      ipt.scale(0.5f);
      ptinv = p0;
      isinversion = true;

    } else if (haveinversion) {

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
       * o / \ / \ i' / \ / i \ A/_________\A' \ / \ j / \ / \ / \ / x
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

      Vector3f d = (pitch1 == 0 ? new Vector3f() : ax1);
      float f = 0;
      if (ang1 == 60) { // 6_1 at x to 6-bar at i
        f = 3f / 3f;
      } else if (ang1 == 120) { // 3_1 at i to 3-bar at x
        f = 2;
      } else if (ang1 == 90) { // 4_1 to 4-bar at opposite corner
        f = 1;
      } else if (ang1 == 180) { // 2_1 to mirror plane
        // C2 with inversion is a mirror plane -- but could have a glide
        // component.
        pt0 = new Point3f();
        pt0.set(pt00);
        pt0.add(d);
        pa1.scaleAdd(0.5f, d, pt00);
        if (pt0.distance(p0) > 0.1f) {
          trans = new Vector3f(p0);
          trans.sub(pt0);
          ftrans.set(trans);
          uc.toFractional(ftrans);
        } else {
          trans = null;
        }
        isrotation = false;
        haveinversion = false;
        ismirrorplane = true;
      }
      if (f != 0) {
        // pa1 = pa1 + ((pt00 - pa1) + (p0 - (pa1 + d))) * f

        vtemp.set(pt00);
        vtemp.sub(pa1);
        vtemp.add(p0);
        vtemp.sub(pa1);
        vtemp.sub(d);
        vtemp.scale(f);
        pa1.add(vtemp);
        ipt = new Point3f();
        ipt.scaleAdd(0.5f, d, pa1);
        ptinv = new Point3f();
        ptinv.scaleAdd(-2, ipt, pt00);
        ptinv.scale(-1);
      }

    } else if (trans != null) {

      // get rid of unnecessary translations added to keep most operations
      // within cell 555

      ftrans.set(trans);
      uc.toFractional(ftrans);
      if (approx(ftrans.x) == 1)
        ftrans.x = 0;
      if (approx(ftrans.y) == 1)
        ftrans.y = 0;
      if (approx(ftrans.z) == 1)
        ftrans.z = 0;
    }

    // time to get the description

    String info1 = "";

    if (isinversion) {
      uc.toFractional(ipt);
      info1 = "inversion center|" + fcoord(ipt);
    } else if (isrotation) {
      if (haveinversion) {
        info1 = "" + (360 / ang1) + "-bar axis";
      } else if (pitch1 != 0) {
        info1 = "" + (360 / ang1) + "-fold screw axis";
      } else {
        info1 = "C" + (360 / ang1) + " axis";
      }
    } else if (trans != null) {
      String s = " " + fcoord(ftrans);
      if (istranslation) {
        info1 = "translation:" + s;
      } else if (ismirrorplane) {
        float fx = approx(ftrans.x);
        float fy = approx(ftrans.y);
        float fz = approx(ftrans.z);
        s = " " + fcoord(ftrans);
        if (fx != 0 && fy != 0 && fz != 0)
          info1 = "d-";
        else if (fx != 0 && fy != 0 || fy != 0 && fz != 0 || fz != 0 && fx != 0)
          info1 = "n-";
        else if (fx != 0)
          info1 = "a-";
        else if (fy != 0)
          info1 = "b-";
        else
          info1 = "c-";

        info1 += "glide plane |translation:" + s;
      }
    } else if (ismirrorplane) {
      info1 = "mirror plane";
    }

    if (haveinversion && !isinversion) {
      uc.toFractional(ipt);
      info1 += "|inversion center at " + fcoord(ipt);
    }

    if (typeOnly)
      return new Object[] { xyz, xyzOriginal, info1, ftrans, ipt, pa1, ax1,
          new Integer(ang1), null };

    if (id == null)
      id = "sym_";

    StringBuffer draw1 = new StringBuffer();
    String drawid = "\ndraw ID " + id + "_";
    draw1.append(drawid).append("* delete");
    draw1.append(drawid).append("frame1X diameter 0.15 ").append(
        Escape.escape(pt00)).append(Escape.escape(pt01)).append("color red");
    draw1.append(drawid).append("frame1Y diameter 0.15 ").append(
        Escape.escape(pt00)).append(Escape.escape(pt02)).append("color green");
    draw1.append(drawid).append("frame1Z diameter 0.15 ").append(
        Escape.escape(pt00)).append(Escape.escape(pt03)).append("color blue");
    ptemp.set(p1);
    ptemp.sub(p0);
    ptemp.scaleAdd(0.9f, ptemp, p0);
    draw1.append(drawid).append("frame2X diameter 0.20").append(
        Escape.escape(p0)).append(Escape.escape(ptemp)).append("color red");
    ptemp.set(p2);
    ptemp.sub(p0);
    ptemp.scaleAdd(0.9f, ptemp, p0);
    draw1.append(drawid).append("frame2Y diameter 0.20").append(
        Escape.escape(p0)).append(Escape.escape(ptemp)).append("color green");
    ptemp.set(p3);
    ptemp.sub(p0);
    ptemp.scaleAdd(0.9f, ptemp, p0);
    draw1.append(drawid).append("frame2Z diameter 0.20").append(
        Escape.escape(p0)).append(Escape.escape(ptemp)).append("color purple");

    // draw the lines associated with a rotation

    if (isrotation) {
      String col = "red";
      if (haveinversion) {
        draw1.append(drawid).append("invRotation arrow ").append(
            Escape.escape(ptinv)).append(Escape.escape(p0));
        draw1.append(drawid).append("invLine ").append(Escape.escape(ipt))
            .append(Escape.escape(p0));
      } else if (pitch1 == 0) {
        draw1.append(drawid).append("rotRotation arrow ").append(
            Escape.escape(pt00)).append(Escape.escape(pt0));
        draw1.append(drawid).append("rotLine1 ").append(Escape.escape(pt00))
            .append(Escape.escape(pa1));
        draw1.append(drawid).append("rotLine2 ").append(Escape.escape(p0))
            .append(Escape.escape(pa1));
        ax1.scale(3);
        col = "orange";
        ptemp.set(ax1);
        ptemp.scale(-1);
        draw1.append(drawid).append("rotVector2 vector diameter 0.1 ").append(
            Escape.escape(pa1)).append(Escape.escape(ptemp)).append(" color ")
            .append(col);
      } else {
        draw1.append(drawid).append("rotLine1 ").append(Escape.escape(pt00))
            .append(Escape.escape(pa1));
        ptemp.set(pa1);
        ptemp.add(ax1);
        draw1.append(drawid).append("rotLine2 ").append(Escape.escape(p0))
            .append(Escape.escape(ptemp));
      }
      draw1.append(drawid).append("rotVector1 vector diameter 0.1 ").append(
          Escape.escape(pa1)).append(Escape.escape(ax1)).append("color ")
          .append(col);
    }

    // draw the mirror plane

    if (ismirrorplane) {
      if (pt00.distance(pt0) > 0.2)
        draw1.append(drawid).append("planeVector arrow ").append(
            Escape.escape(pt00)).append(Escape.escape(pt0)).append(
            " color indigo");
      ptemp.set(pa1);
      ptemp.add(ax1);
      draw1.append(drawid).append("planeCircle scale 2.0 circle ").append(
          Escape.escape(pa1)).append(Escape.escape(ptemp)).append(
          " color translucent ").append(trans == null ? "green" : "blue")
          .append(" mesh fill");
      if (trans != null) {
        ptemp.set(pt0);
        ptemp.scaleAdd(-1, p0, p1);
        draw1.append(drawid).append("planeFrameX diameter 0.15 ").append(
            Escape.escape(pt0)).append(Escape.escape(ptemp)).append(
            " color translucent red");
        ptemp.set(pt0);
        ptemp.scaleAdd(-1, p0, p2);
        draw1.append(drawid).append("planeFrameY diameter 0.15 ").append(
            Escape.escape(pt0)).append(Escape.escape(ptemp)).append(
            " color translucent green");
        ptemp.set(pt0);
        ptemp.scaleAdd(-1, p0, p3);
        draw1.append(drawid).append("planeFrameZ diameter 0.15 ").append(
            Escape.escape(pt0)).append(Escape.escape(ptemp)).append(
            " color translucent blue");
      }
    }

    // draw a faint frame showing the inversion

    if (haveinversion) {
      draw1.append(drawid).append("invPoint diameter 0.4 ").append(
          Escape.escape(ipt));
      draw1.append(drawid).append("invArrow arrow ")
          .append(Escape.escape(pt00)).append(Escape.escape(ptinv)).append(
              " color indigo");
      if (!isinversion) {
        ptemp.set(ptinv);
        ptemp.scaleAdd(-1, pt01, pt00);
        draw1.append(drawid).append("invFrameX diameter 0.15 ").append(
            Escape.escape(ptinv)).append(Escape.escape(ptemp)).append(
            " color translucent red");
        ptemp.set(ptinv);
        ptemp.scaleAdd(-1, pt02, pt00);
        draw1.append(drawid).append("invFrameY diameter 0.15 ").append(
            Escape.escape(ptinv)).append(Escape.escape(ptemp)).append(
            " color translucent green");
        ptemp.set(ptinv);
        ptemp.scaleAdd(-1, pt03, pt00);
        draw1.append(drawid).append("invFrameZ diameter 0.15 ").append(
            Escape.escape(ptinv)).append(Escape.escape(ptemp)).append(
            " color translucent blue");
      }
    }

    // and display translation if still not {0 0 0}

    if (trans != null) {
      draw1.append(drawid).append("transVector vector ").append(
          Escape.escape(pt0)).append(Escape.escape(trans));
    }

    // color the targeted atoms opaque and add another frame if necessary

    draw1.append("\nvar pt00 = " + Escape.escape(pt00));
    draw1.append("\nvar p0 = " + Escape.escape(p0));
    draw1.append("\nif (within(0.2,p0).length == 0) {");
    draw1.append("\nvar set2 = within(0.2,p0.uxyz.xyz)");
    draw1.append("\nif (set2) {");
    draw1.append(drawid).append("cellOffsetVector arrow @p0 @set2 color grey");
    draw1.append(drawid).append(
        "offsetFrameX diameter 0.20 @{set2.xyz} @{set2.xyz + ").append(
        Escape.escape(v01)).append("*0.9} color red");
    draw1.append(drawid).append(
        "offsetFrameY diameter 0.20 @{set2.xyz} @{set2.xyz + ").append(
        Escape.escape(v02)).append("*0.9} color green");
    draw1.append(drawid).append(
        "offsetFrameZ diameter 0.20 @{set2.xyz} @{set2.xyz + ").append(
        Escape.escape(v03)).append("*0.9} color purple");
    draw1.append("\n}}\n");

    return new Object[] { xyz, xyzOriginal, info1, ftrans, ipt, pa1, ax1,
        new Integer(ang1), draw1.toString() };
  }

  private String fcoord(Tuple3f p) {
    return fc(p.x) + " " + fc(p.y) + " " + fc(p.z);
  }

  private String fc(float x) {
    String m = (x < 0 ? "-" : "");
    if (x < 0) x = -x;
    int x24 = (int) approx(x * 24);
    if (x24 == 0)return "0";
    if (x24 == 24)return m + "1";
    if (x24 == 12)return m + "1/2";
    if (x24%8 == 0)return m + (x24/8) + "/3";
    if (x24%6 == 0)return m + (x24/6) + "/4";
    if (x24%4 == 0)return m + (x24/4) + "/6";
    if (x24%3 == 0)return m + (x24/3) + "/8";
    return m + x;
  }

  Point3f atomTest = new Point3f();

  private void setOffset(Point3f[] atoms, int atomIndex, int count) {
    /*
     * the center of mass of the full set of atoms is moved into the cell with this
     *  
     */
    int i1 = atomIndex;
    int i2 = i1 + count;
    float x = 0;
    float y = 0;
    float z = 0;
    for (int i = i1; i < i2; i++) {
      newPoint(atoms[i], atomTest, 0, 0, 0);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    
    while (x < -0.001 || x >= count + 0.001) {
      m03 += (x < 0 ? 1 : -1);
      x += (x < 0 ? count : -count);
    }
    while (y < -0.001 || y >= count + 0.001) {
      m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? count : -count);
    }
    while (z < -0.001 || z >= count + 0.001) {
      m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? count : -count);
    }
  }

  private void transformCartesian(UnitCell unitcell, Point3f pt) {
    unitcell.toFractional(pt);
    transform(pt);
    unitcell.toCartesian(pt);

  }
  
  Vector3f[] rotateEllipsoid(Point3f cartCenter, Vector3f[] vectors,
                                    UnitCell unitcell, Point3f ptTemp1, Point3f ptTemp2) {
    Vector3f[] vRot = new Vector3f[3];
    ptTemp2.set(cartCenter);
    transformCartesian(unitcell, ptTemp2);
    for (int i = vectors.length; --i >= 0;) {
      ptTemp1.set(cartCenter);
      ptTemp1.add(vectors[i]);
      transformCartesian(unitcell, ptTemp1);
      vRot[i] = new Vector3f(ptTemp1);
      vRot[i].sub(ptTemp2);
    }
    return vRot;
  }
}
