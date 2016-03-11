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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package org.jmol.symmetry;

import org.jmol.api.Interface;
import org.jmol.util.BoxInfo;
import org.jmol.util.Escape;
import javajs.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.SimpleUnitCell;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3i;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.T4;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/**
 * a class private to the org.jmol.symmetry package
 * to be accessed only through the SymmetryInterface API
 * 
 * adds vertices and offsets orientation, 
 * and a variety of additional calculations that in 
 * principle could be put in SimpleUnitCell
 * if desired, but for now are in this optional package.
 * 
 */

class UnitCell extends SimpleUnitCell {
  
  private P3[] vertices; // eight corners
  private final P3 cartesianOffset = new P3();
  private P3 fractionalOffset;
  private boolean allFractionalRelative; // specifically used for JmolData
  private P3 unitCellMultiplier;
  public Lst<String> moreInfo;
  public String name = "";
  
  private UnitCell() {
    super();  
  }
  
  /**
   * 
   * A special constructor for spacially defined unit cells.
   * Not used by readers. 
   * 
   * @param points [origin, a, b, c]
   * @param setRelative
   * @return new unit cell
   */
  static UnitCell newP(T3[] points, boolean setRelative) {
    UnitCell c = new UnitCell();
    float[] parameters = new float[] { -1, 0, 0, 0, 0, 0, points[1].x,
        points[1].y, points[1].z, points[2].x, points[2].y, points[2].z,
        points[3].x, points[3].y, points[3].z };
    c.init(parameters);
    c.allFractionalRelative = setRelative;
    c.initUnitcellVertices();
    c.setCartesianOffset(points[0]);
    return c;
  }
  
  public static UnitCell newA(float[] params, boolean setRelative) {
    UnitCell c = new UnitCell();
    c.init(params);
    c.initUnitcellVertices();
    c.allFractionalRelative = setRelative;
    return  c;
  }

  void initOrientation(M3 mat) {
    if (mat == null)
      return;
    M4 m = new M4();
    m.setToM3(mat);
    matrixFractionalToCartesian.mul2(m, matrixFractionalToCartesian);
    matrixCartesianToFractional.setM4(matrixFractionalToCartesian).invert();
    initUnitcellVertices();
  }

  /**
   * when offset is null, use the current cell, otherwise use the original unit cell
   * 
   * @param pt
   * @param offset
   */
  final void toUnitCell(P3 pt, P3 offset) {
    if (matrixCartesianToFractional == null)
      return;
    if (offset == null) {
      // used redefined unitcell 
      matrixCartesianToFractional.rotTrans(pt);
      unitize(pt);
      matrixFractionalToCartesian.rotTrans(pt);
    } else {
      // use original unit cell
      matrixCtoFANoOffset.rotTrans(pt);
      unitize(pt);
      pt.add(offset); 
      matrixFtoCNoOffset.rotTrans(pt);
    }
  }
  
  public void unitize(P3 pt) {
    switch (dimension) {
    case 3:
      pt.z = toFractionalX(pt.z);  
      //$FALL-THROUGH$
    case 2:
      pt.y = toFractionalX(pt.y);
      //$FALL-THROUGH$
    case 1:
      pt.x = toFractionalX(pt.x);
    }
  }

  public void reset() {
    unitCellMultiplier = null;
    setOffset(P3.new3(0, 0, 0));
  }
  
  void setOffset(T3 pt) {
    if (pt == null)
      return;
    T4 pt4 = (pt instanceof T4 ? (T4) pt : null);
    if (pt4 != null ? pt4.w <= 0 : pt.x >= 100 || pt.y >= 100) {
      // from "unitcell range {aaa bbb scale}"
      //   or "unitcell reset"
      unitCellMultiplier = (pt.z == 0 && pt.x == pt.y ? null : P3.newP(pt));
      if (pt4 == null || pt4.w == 0)
        return;
      // from reset, continuing 
    }
    // from "unitcell offset {i j k}"
    if (hasOffset() || pt.lengthSquared() > 0) {
      fractionalOffset = new P3();
      fractionalOffset.setT(pt);
    }
    matrixCartesianToFractional.m03 = -pt.x;
    matrixCartesianToFractional.m13 = -pt.y;
    matrixCartesianToFractional.m23 = -pt.z;
    cartesianOffset.setT(pt);
    matrixFractionalToCartesian.m03 = 0;
    matrixFractionalToCartesian.m13 = 0;
    matrixFractionalToCartesian.m23 = 0;
    matrixFractionalToCartesian.rotTrans(cartesianOffset);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    if (allFractionalRelative) {
      matrixCtoFANoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
  }

  private void setCartesianOffset(T3 origin) {
    cartesianOffset.setT(origin);
    matrixFractionalToCartesian.m03 = cartesianOffset.x;
    matrixFractionalToCartesian.m13 = cartesianOffset.y;
    matrixFractionalToCartesian.m23 = cartesianOffset.z;
    boolean wasOffset = hasOffset();
    fractionalOffset = new P3();
    fractionalOffset.setT(cartesianOffset);
    matrixCartesianToFractional.m03 = 0;
    matrixCartesianToFractional.m13 = 0;
    matrixCartesianToFractional.m23 = 0;
    matrixCartesianToFractional.rotTrans(fractionalOffset);
    matrixCartesianToFractional.m03 = -fractionalOffset.x;
    matrixCartesianToFractional.m13 = -fractionalOffset.y;
    matrixCartesianToFractional.m23 = -fractionalOffset.z;
    if (allFractionalRelative) {
      matrixCtoFANoOffset.setM4(matrixCartesianToFractional);
      matrixFtoCNoOffset.setM4(matrixFractionalToCartesian);
    }
    if (!wasOffset && fractionalOffset.lengthSquared() == 0)
      fractionalOffset = null;
  }

  void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ) {
    if (maxXYZ.x <= maxXYZ.y && maxXYZ.y >= 555) {
      //alternative format for indicating a range of cells:
      //{111 666}
      //555 --> {0 0 0}
      P3 pt = new P3();
      ijkToPoint3f(maxXYZ.x, pt, 0);
      minXYZ.x = (int) pt.x;
      minXYZ.y = (int) pt.y;
      minXYZ.z = (int) pt.z;
      ijkToPoint3f(maxXYZ.y, pt, 1);
      //555 --> {1 1 1}
      maxXYZ.x = (int) pt.x;
      maxXYZ.y = (int) pt.y;
      maxXYZ.z = (int) pt.z;
    }
    switch (dimension) {
    case 1: // polymer
      minXYZ.y = 0;
      maxXYZ.y = 1;
      //$FALL-THROUGH$
    case 2: // slab
      minXYZ.z = 0;
      maxXYZ.z = 1;
    }
  }

  final String dumpInfo(boolean isFull) {
    return "a=" + a + ", b=" + b + ", c=" + c + ", alpha=" + alpha + ", beta=" + beta + ", gamma=" + gamma
       + "\n" + Escape.eAP(getUnitCellVectors())
       + "\nvolume=" + volume
       + (isFull ? "\nfractional to cartesian: " + matrixFractionalToCartesian 
       + "\ncartesian to fractional: " + matrixCartesianToFractional : "");
  }

  P3[] getVertices() {
    return vertices; // does not include offsets
  }
  
  P3 getCartesianOffset() {
    // for slabbing isosurfaces and rendering the ucCage
    return cartesianOffset;
  }
  
  P3 getFractionalOffset() {
    return fractionalOffset;
  }
  
  final private static double twoP2 = 2 * Math.PI * Math.PI;

  private final static V3[] unitVectors = {
    JC.axisX, JC.axisY, JC.axisZ};
  
  Tensor getTensor(Viewer vwr, float[] parBorU) {
    /*
     * 
     * returns {Vector3f[3] unitVectors, float[3] lengths} from J.W. Jeffery,
     * Methods in X-Ray Crystallography, Appendix VI, Academic Press, 1971
     * 
     * comparing with Fischer and Tillmanns, Acta Cryst C44 775-776, 1988, these
     * are really BETA values. Note that
     * 
     * T = exp(-2 pi^2 (a*b* U11h^2 + b*b* U22k^2 + c*c* U33l^2 + 2 a*b* U12hk +
     * 2 a*c* U13hl + 2 b*c* U23kl))
     * 
     * (ORTEP type 8) is the same as
     * 
     * T = exp{-2 pi^2^ sum~i~[sum~j~(U~ij~ h~i~ h~j~ a*~i~ a*~j~)]}
     * 
     * http://ndbserver.rutgers.edu/mmcif/dictionaries/html/cif_mm.dic/Items/
     * _atom_site.aniso_u[1][2].html
     * 
     * Ortep: http://www.ornl.gov/sci/ortep/man_pdf.html
     * 
     * Anisotropic temperature factor Types 0, 1, 2, 3, and 10 use the following
     * formula for the complete temperature factor.
     * 
     * Base^(-D(b11h2 + b22k2 + b33l2 + cb12hk + cb13hl + cb23kl))
     * 
     * The coefficients bij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 0: Base = e, c = 2, D = 1 
     * Type 1: Base = e, c = 1, D = l 
     * Type 2: Base = 2, c = 2, D = l 
     * Type 3: Base = 2, c = 1, D = l
     * 
     * Anisotropic temperature factor Types 4, 5, 8, and 9 use the following
     * formula for the complete temperature factor, in which a1* , a2*, a3* are
     * reciprocal cell dimensions.
     * 
     * exp[ -D(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + C a1*a2*U12hk + C a1*a3
     * * U13hl + C a2*a3 * U23kl)]
     * 
     * The coefficients Uij (i,j = 1,2,3) of the various types are defined with
     * the following constant settings.
     * 
     * Type 4: C = 2, D = 1/4 
     * Type 5: C = 1, D = 1/4 
     * Type 8: C = 2, D = 2pi2
     * Type 9: C = 1, D = 2pi2
     * 
     * 
     * For beta, we use definitions at
     * http://www.iucr.org/iucr-top/comm/cnom/adp/finrepone/finrepone.html
     * 
     * that betaij = 2pi^2ai*aj* Uij
     * 
     * So if Type 8 is
     * 
     * exp[ -2pi^2(a1*a1*U11hh + a2*a2*U22kk + a3*a3*U33ll + 2a1*a2*U12hk +
     * 2a1*a3 * U13hl + 2a2*a3 * U23kl)]
     * 
     * then we have
     * 
     * exp[ -pi^2(beta11hh + beta22kk + beta33ll + 2beta12hk + 2beta13hl +
     * 2beta23kl)]
     * 
     * and the betaij should be entered as Type 0.
     */

    Tensor t = ((Tensor) Interface.getUtil("Tensor", vwr, "file"));
    if (parBorU[0] == 0 && parBorU[1] == 0 && parBorU[2] == 0) { // this is iso
      float f = parBorU[7];
      float[] eigenValues = new float[] {f, f, f};
      // sqrt will be taken when converted to lengths later
      // no factor of 0.5 pi^2       
      return t.setFromEigenVectors(unitVectors, eigenValues, "iso", "Uiso=" + f, null);
    }
    t.parBorU = parBorU;
    
    double[] Bcart = new double[6];

    int ortepType = (int) parBorU[6];

    if (ortepType == 12) {
      // macromolecular Cartesian

      Bcart[0] = parBorU[0] * twoP2;
      Bcart[1] = parBorU[1] * twoP2;
      Bcart[2] = parBorU[2] * twoP2;
      Bcart[3] = parBorU[3] * twoP2 * 2;
      Bcart[4] = parBorU[4] * twoP2 * 2;
      Bcart[5] = parBorU[5] * twoP2 * 2;

      parBorU[7] = (parBorU[0] + parBorU[1] + parBorU[3]) / 3;

    } else {

      boolean isFractional = (ortepType == 4 || ortepType == 5
          || ortepType == 8 || ortepType == 9);
      double cc = 2 - (ortepType % 2);
      double dd = (ortepType == 8 || ortepType == 9 || ortepType == 10 ? twoP2
          : ortepType == 4 || ortepType == 5 ? 0.25 
          : ortepType == 2 || ortepType == 3 ? Math.log(2) 
          : 1);
      // types 6 and 7 not supported

      //System.out.println("ortep type " + ortepType + " isFractional=" +
      // isFractional + " D = " + dd + " C=" + cc);
      double B11 = parBorU[0] * dd * (isFractional ? a_ * a_ : 1);
      double B22 = parBorU[1] * dd * (isFractional ? b_ * b_ : 1);
      double B33 = parBorU[2] * dd * (isFractional ? c_ * c_ : 1);
      double B12 = parBorU[3] * dd * (isFractional ? a_ * b_ : 1) * cc;
      double B13 = parBorU[4] * dd * (isFractional ? a_ * c_ : 1) * cc;
      double B23 = parBorU[5] * dd * (isFractional ? b_ * c_ : 1) * cc;

      // set bFactor = (U11*U22*U33)
      parBorU[7] = (float) Math.pow(B11 / twoP2 / a_ / a_ * B22 / twoP2 / b_
          / b_ * B33 / twoP2 / c_ / c_, 0.3333);

      Bcart[0] = a * a * B11 + b * b * cosGamma * cosGamma * B22 + c * c
          * cosBeta * cosBeta * B33 + a * b * cosGamma * B12 + b * c * cosGamma
          * cosBeta * B23 + a * c * cosBeta * B13;
      Bcart[1] = b * b * sinGamma * sinGamma * B22 + c * c * cA_ * cA_ * B33
          + b * c * cA_ * sinGamma * B23;
      Bcart[2] = c * c * cB_ * cB_ * B33;
      Bcart[3] = 2 * b * b * cosGamma * sinGamma * B22 + 2 * c * c * cA_
          * cosBeta * B33 + a * b * sinGamma * B12 + b * c
          * (cA_ * cosGamma + sinGamma * cosBeta) * B23 + a * c * cA_ * B13;
      Bcart[4] = 2 * c * c * cB_ * cosBeta * B33 + b * c * cosGamma * B23 + a
          * c * cB_ * B13;
      Bcart[5] = 2 * c * c * cA_ * cB_ * B33 + b * c * cB_ * sinGamma * B23;

    }

    //System.out.println("UnitCell Bcart=" + Bcart[0] + " " + Bcart[1] + " "
      //  + Bcart[2] + " " + Bcart[3] + " " + Bcart[4] + " " + Bcart[5]);

    return t.setFromThermalEquation(Bcart, Escape.eAF(parBorU));
  }
  
  P3[] getCanonicalCopy(float scale, boolean withOffset) {
    P3[] pts  = new P3[8];
    P3 cell0 = null;
    P3 cell1 = null;
    if (withOffset && unitCellMultiplier != null) {
      cell0 = new P3();
      cell1 = new P3();
      ijkToPoint3f((int) unitCellMultiplier.x, cell0, 0);
      ijkToPoint3f((int) unitCellMultiplier.y, cell1, 0);
      cell1.sub(cell0);
    }
    for (int i = 0; i < 8; i++) {
      P3 pt = pts[i] = P3.newP(BoxInfo.unitCubePoints[i]);
      if (cell0 != null) {
        scale *= (unitCellMultiplier.z == 0 ? 1 : unitCellMultiplier.z);
        pts[i].add3(cell0.x + cell1.x * pt.x, 
            cell0.y + cell1.y * pt.y,
            cell0.z + cell1.z * pt.z);
      }
      matrixFractionalToCartesian.rotTrans(pt);
      if (!withOffset)
        pt.sub(cartesianOffset);
    }
    return BoxInfo.getCanonicalCopy(pts, scale);
  }

  /// private methods
  
  
  private static float toFractionalX(float x) {
    // introduced in Jmol 11.7.36
    x = (float) (x - Math.floor(x));
    if (x > 0.9999f || x < 0.0001f) 
      x = 0;
    return x;
  }
  
  private void initUnitcellVertices() {
    if (matrixFractionalToCartesian == null)
      return;
    matrixCtoFANoOffset = M4.newM4(matrixCartesianToFractional);
    matrixFtoCNoOffset = M4.newM4(matrixFractionalToCartesian);
    vertices = new P3[8];
    for (int i = 8; --i >= 0;)
      vertices[i] = (P3) matrixFractionalToCartesian.rotTrans2(BoxInfo.unitCubePoints[i], new P3());
  }

  /**
   * 
   * @param f1
   * @param f2
   * @param distance
   * @param dx
   * @param iRange
   * @param jRange
   * @param kRange
   * @param ptOffset TODO
   * @return       TRUE if pt has been set.
   */
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                              int iRange, int jRange, int kRange, P3 ptOffset) {
    P3 p1 = P3.newP(f1);
    toCartesian(p1, true);
    for (int i = -iRange; i <= iRange; i++)
      for (int j = -jRange; j <= jRange; j++)
        for (int k = -kRange; k <= kRange; k++) {
          ptOffset.set(f2.x + i, f2.y + j, f2.z + k);
          toCartesian(ptOffset, true);
          float d = p1.distance(ptOffset);
          if (dx > 0 ? Math.abs(d - distance) <= dx : d <= distance && d > 0.1f) {
            ptOffset.set(i, j, k);
            return true;
          }
        }
    return false;
  }

  public P3 getUnitCellMultiplier() {
    return unitCellMultiplier;
  }

  public P3[] getUnitCellVectors() {
    M4 m = matrixFractionalToCartesian;
    return new P3[] { 
        P3.newP(cartesianOffset),
        P3.new3(fix(m.m00), fix(m.m10), fix(m.m20)), 
        P3.new3(fix(m.m01), fix(m.m11), fix(m.m21)), 
        P3.new3(fix(m.m02), fix(m.m12), fix(m.m22)) };
  }

  private float fix(float x) {
    return (Math.abs(x) < 0.001f ? 0 : x);
  }

  public boolean isSameAs(UnitCell uc) {
    if (uc.unitCellParams.length != unitCellParams.length)
      return false;
    for (int i = unitCellParams.length; --i >= 0;)
      if (unitCellParams[i] != uc.unitCellParams[i]
          && !(Float.isNaN(unitCellParams[i]) && Float
              .isNaN(uc.unitCellParams[i])))
        return false;
    return (fractionalOffset == null ? !uc.hasOffset()
        : uc.fractionalOffset == null ? !hasOffset() 
        : fractionalOffset.distanceSquared(uc.fractionalOffset) == 0);
  }

  public boolean hasOffset() {
    return (fractionalOffset != null && fractionalOffset.lengthSquared() != 0);
  }

  public String getState() {
    String s = "";
    // unitcell offset {1 1 1}
    if (fractionalOffset != null && fractionalOffset.lengthSquared() != 0)
      s += "  unitcell offset " + Escape.eP(fractionalOffset) + ";\n";
    // unitcell range {444 555 1}
    if (unitCellMultiplier != null)
      s += "  unitcell range " + Escape.eP(unitCellMultiplier) + ";\n";
    return s;
  }

  /**
   * Returns a quaternion that will take the standard frame
   * to a view down a particular axis, expressed as its counterparts.
   * 
   * @param abc  ab  bc  ca
   * @return quaternion
   */
  public Quat getQuaternionRotation(String abc) {
    T3 a = V3.newVsub(vertices[4], vertices[0]);
    T3 b = V3.newVsub(vertices[2], vertices[0]);
    T3 c = V3.newVsub(vertices[1], vertices[0]);
    T3 x = new V3();
    T3 v = new V3();
  
//  qab = !quaternion({0 0 0}, cross(cxb,c), cxb);
//  qbc = !quaternion({0 0 0}, cross(axc,a), axc)
//  qca = !quaternion({0 0 0}, cross(bxa,b), bxa);
//      
      
  switch ("abc".indexOf(abc)) {
  case 0: // bc
    x.cross(a, c);
    v.cross(x, a);
    break;
  case 1: // ca
    x.cross(b, a);
    v.cross(x, b);
    break;
  case 2: // ab
    x.cross(c, b);
    v.cross(x, c);
    break;
  default:
    return null;
  }
  return Quat.getQuaternionFrame(null, v, x).inv();
  }

  public T3[] getV0abc(Object def) {
    M4 m;
    boolean isRev = false;
    V3[] pts = new V3[4];
    V3 pt = pts[0] = V3.new3(0, 0, 0);
    pts[1] = V3.new3(1, 0, 0);
    pts[2] = V3.new3(0, 1, 0);
    pts[3] = V3.new3(0, 0, 1);
    M3 m3 = new M3();
    if (def instanceof String) {
      String sdef = (String) def;
      // a,b,c;0,0,0
      if (sdef.indexOf(";") < 0)
        sdef += ";0,0,0";
      isRev = sdef.startsWith("!");
      if (isRev)
        sdef = sdef.substring(1);
      Symmetry symTemp = new Symmetry();
      symTemp.setSpaceGroup(false);
      int i = symTemp.addSpaceGroupOperation("=" + sdef, 0);
      if (i < 0)
        return null;
      m = symTemp.getSpaceGroupOperation(i);
      ((SymmetryOperation) m).doFinalize();
    } else if (def instanceof M3) {
      m = M4.newMV((M3) def, new P3());
    } else if (def instanceof M4) {
      m = (M4) def;
    } else {
      // direct 4x4 Cartesian transform
      m = (M4) ((Object[]) def)[0];
      m.getRotationScale(m3);
      toCartesian(pt, false);
      m.rotTrans(pt);
      for (int i = 1; i < 4; i++) {
        toCartesian(pts[i], true);
        m3.rotate(pts[i]);
      }
      return pts;
    }   
    
    // We have an operator that may need reversing.
    // Note that translations are limited to 1/2, 1/3, 1/4, 1/6, 1/8.

    m.getRotationScale(m3);
    m.getTranslation(pt);
    if (isRev) {
      m3.invert();
      m3.transpose();
      m3.rotate(pt);
      pt.scale(-1);
    } else {
      m3.transpose();
    }

    // Note that only the origin is translated;
    // the others are vectors from the origin.

    // this is a point, so we do not ignore offset
    toCartesian(pt, false);
    for (int i = 1; i < 4; i++) {
      m3.rotate(pts[i]);
      // these are vectors, so we ignore offset
      toCartesian(pts[i], true);
    }
    return pts;
  }

}
