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

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;

import java.util.Hashtable;

import java.util.Map;


import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;

import javajs.util.P3;
import javajs.util.V3;

/*
 * Bob Hanson 7/2008
 * 
 * brute force -- preliminary from BCCE20 meeting 2008
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 * 
 *
 */

class PointGroup {

  private final static int[] axesMaxN = new int[] { 
     15, // used for plane count
      0, // n/a 
      0, // not used -- would be S2 (inversion)
      1, // S3
      3, // S4
      1, // S5
      10,// S6
      0, // n/a
      1, // S8
      0, // n/a
      6, // S10
      0, // n/a 
      1, // S12
      0, // n/a
      0, // n/a firstProper = 14
      0, // n/a 
      15,// C2 
      10,// C3 
      6, // C4
      6, // C5
      10,// C6
      0, // C7
      1, // C8
  };

  private final static int[] nUnique = new int[] { 
     1, // used for plane count
     0, // n/a 
     0, // not used -- would be S2 (inversion)
     2, // S3
     2, // S4
     4, // S5
     2, // S6
     0, // n/a
     4, // S8
     0, // n/a
     4, // S10
     0, // n/a 
     4, // S12
     0, // n/a
     0, // n/a firstProper = 14
     0, // n/a 
     1, // C2 
     2, // C3 
     2, // C4
     4, // C5
     2, // C6
     0, // C7
     4, // C8
 };

  private final static int s3 = 3;
  private final static int s4 = 4;
  private final static int s5 = 5;
  private final static int s6 = 6;
  private final static int s8 = 8;
  private final static int s10 = 10;
  private final static int s12 = 12;
  private final static int firstProper = 14;
  private final static int c2 = firstProper + 2;
  private final static int c3 = firstProper + 3;
  private final static int c4 = firstProper + 4;
  private final static int c5 = firstProper + 5;
  private final static int c6 = firstProper + 6;
  private final static int c8 = firstProper + 8;
  private final static int maxAxis = axesMaxN.length;

  String drawInfo;
  Map<String, Object> info;
  String textInfo;

  private String drawType = "";
  private int drawIndex;
  private float scale = Float.NaN;  
  private int[]  nAxes = new int[maxAxis];
  private Operation[][] axes = new Operation[maxAxis][];
  private int nAtoms;
  private float radius;
  private float distanceTolerance = 0.25f; // making this just a bit more generous
  private float linearTolerance = 8f;
  private float cosTolerance = 0.99f; // 8 degrees
  private String name = "C_1?";
  private Operation principalAxis;
  private Operation principalPlane;

  String getName() {
    return name;
  }

  private final V3 vTemp = new V3();
  private int centerAtomIndex = -1;
  private boolean haveInversionCenter;
  
  final private P3 center = new P3();

  private T3[] points;
  private int[] elements;

  private BS bsAtoms;

  private boolean haveVibration;

  private boolean localEnvOnly;


  /**
   * 
   * @param pgLast
   * @param atomset
   * @param bsAtoms
   * @param haveVibration
   * @param distanceTolerance
   * @param linearTolerance
   * @param localEnvOnly
   * @return a PointGroup
   */
  
  static PointGroup getPointGroup(PointGroup pgLast, T3[] atomset,
                                         BS bsAtoms, boolean haveVibration,
                                         float distanceTolerance,
                                         float linearTolerance, boolean localEnvOnly) {
    PointGroup pg = new PointGroup();
    pg.distanceTolerance = distanceTolerance;
    pg.linearTolerance = linearTolerance;
    pg.bsAtoms = (bsAtoms == null ? BSUtil.newBitSet2(0, atomset.length) : bsAtoms);
    pg.haveVibration = haveVibration;
    pg.localEnvOnly = localEnvOnly;
    return (pg.set(pgLast, atomset) ? pg : pgLast);
  }

  private PointGroup() {
  }
  
  private boolean isEqual(PointGroup pg) {
    if (pg == null)
      return false;
    if (linearTolerance != pg.linearTolerance 
        || distanceTolerance != pg.distanceTolerance
        || nAtoms != pg.nAtoms
        || localEnvOnly != pg.localEnvOnly
        || haveVibration != pg.haveVibration
        || bsAtoms ==  null ? pg.bsAtoms != null : !bsAtoms.equals(pg.bsAtoms))
      return false;
    for (int i = 0; i < nAtoms; i++) {
      // real floating == 0 here because they must be IDENTICAL POSITIONS
      if (elements[i] != pg.elements[i] || !points[i].equals(pg.points[i]))
        return false;
    }
    return true;
  }
  
  private boolean set(PointGroup pgLast, T3[] atomset) {
    cosTolerance = (float) (Math.cos(linearTolerance / 180 * Math.PI));
    if (!getPointsAndElements(atomset)) {
      Logger.error("Too many atoms for point group calculation");
      name = "point group not determined -- ac > " + ATOM_COUNT_MAX
          + " -- select fewer atoms and try again.";
      return true;
    }
    getElementCounts();
    if (haveVibration) {
      P3[] atomVibs = new P3[points.length];
      for (int i = points.length; --i >= 0;) {
        atomVibs[i] = P3.newP(points[i]);
        V3 v = ((Atom) points[i]).getVibrationVector();
        if (v != null)
          atomVibs[i].add(v);
      }
      points = atomVibs;
    }
    if (isEqual(pgLast))
      return false;
    findInversionCenter();

    if (isLinear(points)) {
      if (haveInversionCenter) {
        name = "D(infinity)h";
      } else {
        name = "C(infinity)v";
      }
      vTemp.sub2(points[1], points[0]);
      addAxis(c2, vTemp);
      principalAxis = axes[c2][0];
      if (haveInversionCenter) {
        axes[0] = new Operation[1];
        principalPlane = axes[0][nAxes[0]++] = new Operation(vTemp);
      }
      return true;
    }
    axes[0] = new Operation[15];
    int nPlanes = 0;
    findCAxes();
    nPlanes = findPlanes();
    findAdditionalAxes(nPlanes);

    /* flow chart contribution of Dean Johnston */

    int n = getHighestOrder();
    if (nAxes[c3] > 1) {
      // must be Ix, Ox, or Tx
      if (nAxes[c5] > 1) {
        if (haveInversionCenter) {
          name = "Ih";
        } else {
          name = "I";
        }
      } else if (nAxes[c4] > 1) {
        if (haveInversionCenter) {
          name = "Oh";
        } else {
          name = "O";
        }
      } else {
        if (nPlanes > 0) {
          if (haveInversionCenter) {
            name = "Th";
          } else {
            name = "Td";
          }
        } else {
          name = "T";
        }
      }
    } else {
      // Cs, Ci, C1
      if (n < 2) {
        if (nPlanes == 1) {
          name = "Cs";
          return true;
        }
        if (haveInversionCenter) {
          name = "Ci";
          return true;
        }
        name = "C1";
      } else if ((n % 2) == 1 && nAxes[c2] > 0 || (n % 2) == 0 && nAxes[c2] > 1) {
        // Dnh, Dnd, Dn, S4

        // here based on the presence of C2 axes in any odd-order group
        // and more than one C2 if even order (since the one will be part of the 
        // principal axis

        principalAxis = setPrincipalAxis(n, nPlanes);
        if (nPlanes == 0) {
          if (n < firstProper) {
            name = "S" + n;
          } else {
            name = "D" + (n - firstProper);
          }
        } else {
          // highest axis may be S8, but this is really D4h/D4d
          if (n < firstProper)
            n = n / 2;
          else
            n -= firstProper;
          if (nPlanes == n) {
            name = "D" + n + "d";
          } else {
            name = "D" + n + "h";
          }
        }
      } else if (nPlanes == 0) {
        // Cn, S3, S6 
        principalAxis = axes[n][0];
        if (n < firstProper) {
          name = "S" + n;
        } else {
          name = "C" + (n - firstProper);
        }
      } else if (nPlanes == n - firstProper) {
        principalAxis = axes[n][0];
        name = "C" + nPlanes + "v";
      } else {
        principalAxis = axes[n < firstProper ? n + firstProper : n][0];
        principalPlane = axes[0][0];
        if (n < firstProper)
          n /= 2;
        else
          n -= firstProper;
        name = "C" + n + "h";
      }
    }
    return true;
  }

  private Operation setPrincipalAxis(int n, int nPlanes) {
    Operation principalPlane = setPrincipalPlane(n, nPlanes);
    if (nPlanes == 0 && n < firstProper || nAxes[n] == 1) {
      if (nPlanes > 0 && n < firstProper)
        n = firstProper + n / 2;
        return axes[n][0];
    }
    // D2, D2d, D2h -- which c2 axis is it?
    if (principalPlane == null)
      return null;
    for (int i = 0; i < nAxes[c2]; i++)
      if (isParallel(principalPlane.normalOrAxis, axes[c2][i].normalOrAxis)) {
        if (i != 0) {
          Operation o = axes[c2][0];
          axes[c2][0] = axes[c2][i];
          axes[c2][i] = o;
        }
        return axes[c2][0];
      }
    return null;
  }

  private Operation setPrincipalPlane(int n, int nPlanes) {
    // principal plane is perpendicular to more than two other planes
    if (nPlanes == 1)
      return principalPlane = axes[0][0];
    if (nPlanes == 0 || nPlanes == n - firstProper)
      return null;
    for (int i = 0; i < nPlanes; i++)
      for (int j = 0, nPerp = 0; j < nPlanes; j++)
        if (isPerpendicular(axes[0][i].normalOrAxis, axes[0][j].normalOrAxis) && ++nPerp > 2) {
          if (i != 0) {
            Operation o = axes[0][0];
            axes[0][0] = axes[0][i];
            axes[0][i] = o;
          }
          return principalPlane = axes[0][0];
        }
    return null;
  }

  private final static int ATOM_COUNT_MAX = 100;

  private boolean getPointsAndElements(T3[] atomset) {
    int ac = BSUtil.cardinalityOf(bsAtoms);
    if (ac > ATOM_COUNT_MAX)
      return false;
    points = new P3[ac];
    elements = new int[ac];
    if (ac == 0)
      return true;
    nAtoms = 0;
    // we optionally include bonding information
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1), nAtoms++) {
      T3 p = points[nAtoms] = atomset[i];
      if (p instanceof Node) {
        int bondIndex = (localEnvOnly ? 1 : 1 + Math.max(3,
            ((Node) p).getCovalentBondCount()));
        elements[nAtoms] = ((Node) p).getElementNumber() * bondIndex;
      }
      center.add(points[nAtoms]);
    }
    center.scale(1f / nAtoms);
    for (int i = nAtoms; --i >= 0;) {
      float r = center.distance(points[i]);
      if (r < distanceTolerance)
        centerAtomIndex = i;
      radius = Math.max(radius, r);
    }
    return true;
  }

  private void findInversionCenter() {
    haveInversionCenter = checkOperation(null, center, -1);
    if (haveInversionCenter) {
      axes[1] = new Operation[1];
      axes[1][0] = new Operation();
    }
  }

  private boolean checkOperation(Quat q, P3 center, int iOrder) {
    P3 pt = new P3();
    int nFound = 0;
    boolean isInversion = (iOrder < firstProper);
    out: for (int i = points.length; --i >= 0 && nFound < points.length;)
      if (i == centerAtomIndex) {
        nFound++;
      } else {
        T3 a1 = points[i];
        int e1 = elements[i];
        if (q != null) {
          pt.sub2(a1, center);
          q.transform2(pt, pt).add(center);
        } else {
          pt.setT(a1);
        }
        if (isInversion) {
          // A trick here: rather than 
          // actually doing a rotation/reflection
          // we do a rotation INVERSION. This works
          // due to the symmetry of S2, S4, and S8
          // For S3 and S6, we play the trick of b
          // rotating as C6 and C3, respectively, 
          // THEN doing the rotation/inversion. 
          vTemp.sub2(center, pt);
          pt.scaleAdd2(2, vTemp, pt);
        }
        if ((q != null || isInversion) && pt.distance(a1) < distanceTolerance) {
          nFound++;
          continue;
        }
        for (int j = points.length; --j >= 0;) {
          if (j == i || j == centerAtomIndex || elements[j] != e1)
            continue;
          T3 a2 = points[j];
          if (pt.distance(a2) < distanceTolerance) {
            nFound++;
            continue out;
          }
        }
      }
    return nFound == points.length;
  }

  private boolean isLinear(T3[] atoms) {
    V3 v1 = null;
    if (atoms.length < 2)
      return false;
    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      if (v1 == null) {
        v1 = new V3();
        v1.sub2(atoms[i], center);
        v1.normalize();
        vTemp.setT(v1);
        continue;
      }
      vTemp.sub2(atoms[i], center);
      vTemp.normalize();
      if (!isParallel(v1, vTemp))
        return false;
    }
    return true;
  }

  private boolean isParallel(V3 v1, V3 v2) {
    // note -- these MUST be unit vectors
    return (Math.abs(v1.dot(v2)) >= cosTolerance);
  }

  private boolean isPerpendicular(V3 v1, V3 v2) {
    // note -- these MUST be unit vectors
    return (Math.abs(v1.dot(v2)) <= 1 - cosTolerance);
  }

  int maxElement = 0;
  int[] eCounts;

  private void getElementCounts() {
    for (int i = points.length; --i >= 0;) {
      int e1 = elements[i];
      if (e1 > maxElement)
        maxElement = e1;
    }
    eCounts = new int[++maxElement];
    for (int i = points.length; --i >= 0;)
      eCounts[elements[i]]++; 
  }

  private int findCAxes() {
    V3 v1 = new V3();
    V3 v2 = new V3();
    V3 v3 = new V3();

    // look for the proper and improper axes relating pairs of atoms

    for (int i = points.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      T3 a1 = points[i];
      int e1 = elements[i];
      for (int j = points.length; --j > i;) {
        T3 a2 = points[j];
        if (elements[j] != e1)
          continue;

        // check if A - 0 - B is linear

        v1.sub2(a1, center);
        v2.sub2(a2, center);
        v1.normalize();
        v2.normalize();
        if (isParallel(v1, v2)) {
          getAllAxes(v1);
          continue;
        }

        // look for all axes to average position of A and B

        if (nAxes[c2] < axesMaxN[c2]) {
          v3.ave(a1, a2);
          v3.sub(center);
          getAllAxes(v3);
        }

        // look for the axis perpendicular to the A -- 0 -- B plane

        float order = (float) (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) Math.floor(order + 0.01f);
        boolean isIntegerOrder = (order - iOrder <= 0.02f);
        if (!isIntegerOrder || (iOrder = iOrder + firstProper) >= maxAxis)
          continue;
        if (nAxes[iOrder] < axesMaxN[iOrder]) {
          v3.cross(v1, v2);
          checkAxisOrder(iOrder, v3, center);
        }
      }
    }

    // check all C2 axes for C3-related axes

    V3[] vs = new V3[nAxes[c2] * 2];
    for (int i = 0; i < vs.length; i++)
      vs[i] = new V3();
    int n = 0;
    for (int i = 0; i < nAxes[c2]; i++) {
      vs[n++].setT(axes[c2][i].normalOrAxis);
      vs[n].setT(axes[c2][i].normalOrAxis);
      vs[n++].scale(-1);
    }
    for (int i = vs.length; --i >= 2;)
      for (int j = i; --j >= 1;)
        for (int k = j; --k >= 0;) {
          v3.add2(vs[i], vs[j]);
          v3.add(vs[k]);
          if (v3.length() < 1.0)
            continue;
          checkAxisOrder(c3, v3, center);
        }

    // Now check for triples of elements that will define
    // axes using the element with the smallest
    // number of atoms n, with n >= 3 
    // cross all triples of vectors looking for standard
    // principal axes quantities.

    // Also check for vectors from {0 0 0} to
    // the midpoint of each triple of atoms

    // get minimum element count > 2

    int nMin = Integer.MAX_VALUE;
    int iMin = -1;
    for (int i = 0; i < maxElement; i++) {
      if (eCounts[i] < nMin && eCounts[i] > 2) {
        nMin = eCounts[i];
        iMin = i;
      }
    }

    out: for (int i = 0; i < points.length - 2; i++)
      if (elements[i] == iMin)
        for (int j = i + 1; j < points.length - 1; j++)
          if (elements[j] == iMin)
            for (int k = j + 1; k < points.length; k++)
              if (elements[k] == iMin) {
                v1.sub2(points[i], points[j]);
                v2.sub2(points[i], points[k]);
                v1.normalize();
                v2.normalize();
                v3.cross(v1, v2);
                getAllAxes(v3);
//                checkAxisOrder(3, v3, center);
                v1.add2(points[i], points[j]);
                v1.add(points[k]);
                v1.normalize();
                if (!isParallel(v1, v3))
                  getAllAxes(v1);
                if (nAxes[c5] == axesMaxN[c5])
                  break out;
              }

    //check for C2 by looking for axes along element-based geometric centers

    vs = new V3[maxElement];
    for (int i = points.length; --i >= 0;) {
      int e1 = elements[i];
      if (vs[e1] == null)
        vs[e1] = new V3();
      else if (haveInversionCenter)
        continue;
      vs[e1].add(points[i]);
    }
    if (!haveInversionCenter)
      for (int i = 0; i < maxElement; i++)
        if (vs[i] != null)
          vs[i].scale(1f / eCounts[i]);

    // check for vectors from {0 0 0} to
    // the midpoint of each pair of atoms
    // within the same element if there is no inversion center,
    // otherwise, check for cross-product axis

    for (int i = 0; i < maxElement; i++)
      if (vs[i] != null)
        for (int j = 0; j < maxElement; j++) {
          if (i == j || vs[j] == null)
            continue;
          if (haveInversionCenter)
           v1.cross(vs[i], vs[j]);
          else
            v1.sub2(vs[i], vs[j]);
          checkAxisOrder(c2, v1, center);
          
        }

    return getHighestOrder();
  }

  private void getAllAxes(V3 v3) {
    for (int o = c2; o < maxAxis; o++)
      if (nAxes[o] < axesMaxN[o])
        checkAxisOrder(o, v3, center);
  }

  private int getHighestOrder() {
    int n = 0;
    // highest S
    for (n = firstProper; --n > 1 && nAxes[n] == 0;) {
    }
    // or highest C
    if (n > 1)
      return (n + firstProper < maxAxis && nAxes[n + firstProper] > 0 ? n + firstProper : n);
    for (n = maxAxis; --n > 1 && nAxes[n] == 0;) {
    }
    return n;
  }

  private boolean checkAxisOrder(int iOrder, V3 v, P3 center) {
    switch (iOrder) {
    case c8:
      if (nAxes[c3] > 0)
        return false;
      //$FALL-THROUGH$;
    case c6:
    case c4:
      if (nAxes[c5] > 0)
        return false;
      break;
    case c3:
      if (nAxes[c8] > 0)
        return false;
      break;
    case c5:
      if (nAxes[c4] > 0 || nAxes[c6] > 0 || nAxes[c8] > 0) 
        return false;
      break;
    }

    v.normalize();
    if (haveAxis(iOrder, v))
      return false;
    Quat q = Quat.newVA(v, (iOrder < firstProper ? 180 : 0) + 360 / (iOrder % firstProper));
    if (!checkOperation(q, center, iOrder))
      return false;
    addAxis(iOrder, v);
    // check for Sn:
    switch (iOrder) {
    case c2:
      checkAxisOrder(s4, v, center);//D2d, D4h, D6d
      break;
    case c3:
      checkAxisOrder(s3, v, center);//C3h, D3h
      if (haveInversionCenter)
        addAxis(s6, v);
      break;
    case c4:
      addAxis(c2, v);
      checkAxisOrder(s4, v, center);//D2d, D4h, D6d
      checkAxisOrder(s8, v, center);//D4d
      break;
    case c5:
      checkAxisOrder(s5, v, center); //C5h, D5h
      if (haveInversionCenter)
        addAxis(s10, v);
      break;
    case c6:
      addAxis(c2, v);
      addAxis(c3, v);
      checkAxisOrder(s3, v, center);//C6h, D6h
      checkAxisOrder(s6, v, center);//C6h, D6h
      checkAxisOrder(s12, v, center);//D6d
      break;
    case c8:
      //note -- D8d would have a S16 axis. This will not be found.
      addAxis(c2, v);
      addAxis(c4, v);
      break;
    }
    return true;
  }

  private void addAxis(int iOrder, V3 v) {
    if (haveAxis(iOrder, v))
      return;
    if (axes[iOrder] == null)
      axes[iOrder] = new Operation[axesMaxN[iOrder]];
    axes[iOrder][nAxes[iOrder]++] = new Operation(v, iOrder);
  }

  private boolean haveAxis(int iOrder, V3 v) {
    if (nAxes[iOrder] == axesMaxN[iOrder]) {
      return true;
    }
    if (nAxes[iOrder] > 0)
      for (int i = nAxes[iOrder]; --i >= 0;) {
        if (isParallel(v, axes[iOrder][i].normalOrAxis))
          return true;
      }
    return false;
  }

  private int findPlanes() {
    P3 pt = new P3();
    V3 v1 = new V3();
    V3 v2 = new V3();
    V3 v3 = new V3();
    int nPlanes = 0;
    boolean haveAxes = (getHighestOrder() > 1);
    for (int i = points.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      T3 a1 = points[i];
      int e1 = elements[i];
      for (int j = points.length; --j > i;) {
        if (haveAxes && elements[j] != e1)
          continue;

        // plane are treated as S2 axes here

        // first, check planes through two atoms and the center
        // or perpendicular to a linear A -- 0 -- B set

        T3 a2 = points[j];
        pt.add2(a1, a2);
        pt.scale(0.5f);
        v1.sub2(a1, center);
        v2.sub2(a2, center);
        if (!isParallel(v1, v2)) {
          v3.cross(v1, v2);
          v3.normalize();
          nPlanes = getPlane(v3);
        }

        // second, look for planes perpendicular to the A -- B line

        v3.sub2(a2, a1);
        v3.normalize();
        nPlanes = getPlane(v3);
        if (nPlanes == axesMaxN[0])
          return nPlanes;
      }
    }

    // also look for planes normal to any C axis
    if (haveAxes)
      for (int i = c2; i < maxAxis; i++)
        for (int j = 0; j < nAxes[i]; j++)
          nPlanes = getPlane(axes[i][j].normalOrAxis);
    return nPlanes;
  }

  private int getPlane(V3 v3) {
    if (!haveAxis(0, v3)
        && checkOperation(Quat.newVA(v3, 180), center,
            -1))
      axes[0][nAxes[0]++] = new Operation(v3);
    return nAxes[0];
  }

  private void findAdditionalAxes(int nPlanes) {

    Operation[] planes = axes[0];
    int Cn = 0;
    if (nPlanes > 1
        && ((Cn = nPlanes + firstProper) < maxAxis) 
        && nAxes[Cn] == 0) {
      // cross pairs of plane normals. We don't need many.
      vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
      if (!checkAxisOrder(Cn, vTemp, center)
          && nPlanes > 2) {
        vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
        checkAxisOrder(Cn - 1, vTemp, center);
      }
    }
    if (nAxes[c2] == 0 && nPlanes > 2) {
      // check for C2 axis relating 
      for (int i = 0; i < nPlanes - 1; i++) {
        for (int j = i + 1; j < nPlanes; j++) {
          vTemp.add2(planes[1].normalOrAxis, planes[2].normalOrAxis);
          //if (
          checkAxisOrder(c2, vTemp, center);
          //)
            //Logger.error("found a C2 axis by adding plane normals");
        }
      }
    }
  }

  final static int OPERATION_PLANE = 0;
  final static int OPERATION_PROPER_AXIS = 1;
  final static int OPERATION_IMPROPER_AXIS = 2;
  final static int OPERATION_INVERSION_CENTER = 3;

  final static String[] typeNames = { "plane", "proper axis", "improper axis",
      "center of inversion" };

  int nOps = 0;
  private class Operation {
    int type;
    int order;
    int index;
    V3 normalOrAxis;

    Operation() {
      index = ++nOps;
      type = OPERATION_INVERSION_CENTER;
      order = 1;
      if (Logger.debugging)
        Logger.debug("new operation -- " + typeNames[type]);
    }

    Operation(V3 v, int i) {
      index = ++nOps;
      type = (i < firstProper ? OPERATION_IMPROPER_AXIS : OPERATION_PROPER_AXIS);
      order = i % firstProper;
      normalOrAxis = Quat.newVA(v, 180).getNormal();
      if (Logger.debugging)
        Logger.debug("new operation -- " + (order == i ? "S" : "C") + order + " "
            + normalOrAxis);
    }

    Operation(V3 v) {
      if (v == null)
        return;
      index = ++nOps;
      type = OPERATION_PLANE;
      normalOrAxis = Quat.newVA(v, 180).getNormal();
      if (Logger.debugging)
        Logger.debug("new operation -- plane " + normalOrAxis);
    }

    String getLabel() {
      switch (type) {
      case OPERATION_PLANE:
        return "Cs";
      case OPERATION_IMPROPER_AXIS:
        return "S" + order;
      default:
        return "C" + order;
      }
    }
  }

  Object getInfo(int modelIndex, boolean asDraw, boolean asInfo, String type,
                 int index, float scaleFactor) {
    info = (asInfo ? new Hashtable<String, Object>() : null);
    V3 v = new V3();
    Operation op;
    if (scaleFactor == 0)
      scaleFactor = 1;
    scale = scaleFactor;
    int[][] nType = new int[4][2];
    for (int i = 1; i < maxAxis; i++)
      for (int j = nAxes[i]; --j >= 0;)
        nType[axes[i][j].type][0]++;
    SB sb = new SB()
      .append("# ").appendI(nAtoms).append(" atoms\n");
    if (asDraw) {
      boolean haveType = (type != null && type.length() > 0);
      drawType = type = (haveType ? type : "");
      drawIndex = index;
      boolean anyProperAxis = (type.equalsIgnoreCase("Cn"));
      boolean anyImproperAxis = (type.equalsIgnoreCase("Sn"));
      sb.append("set perspectivedepth off;\n");
      String m = "_" + modelIndex + "_";
      if (!haveType)
        sb.append("draw pg0").append(m).append("* delete;draw pgva").append(m
           ).append("* delete;draw pgvp").append(m).append("* delete;");
      if (!haveType || type.equalsIgnoreCase("Ci"))
        sb.append("draw pg0").append(m).append(
            haveInversionCenter ? "inv " : " ").append(
            Escape.eP(center)).append(haveInversionCenter ? "\"i\";\n" : ";\n");
      float offset = 0.1f;
      for (int i = 2; i < maxAxis; i++) {
        if (i == firstProper)
          offset = 0.1f;
        if (nAxes[i] == 0)
          continue;
        String label = axes[i][0].getLabel();
        offset += 0.25f;
        float scale = scaleFactor * radius + offset;
        if (!haveType || type.equalsIgnoreCase(label) || anyProperAxis
            && i >= firstProper || anyImproperAxis && i < firstProper)
          for (int j = 0; j < nAxes[i]; j++) {
            if (index > 0 && j + 1 != index)
              continue;
            op = axes[i][j];
            v.add2(op.normalOrAxis, center);
            if (op.type == OPERATION_IMPROPER_AXIS)
              scale = -scale;
            sb.append("draw pgva").append(m).append(label).append("_").appendI(
                j + 1).append(" width 0.05 scale ").appendF(scale).append(" ").append(
                Escape.eP(v));
            v.scaleAdd2(-2, op.normalOrAxis, v);
            boolean isPA = (principalAxis != null && op.index == principalAxis.index);
            sb.append(Escape.eP(v)).append(
                "\"").append(label).append(isPA ? "*" : "").append("\" color ").append(
                isPA ? "red" : op.type == OPERATION_IMPROPER_AXIS ? "blue"
                    : "yellow").append(";\n");
          }
      }
      if (!haveType || type.equalsIgnoreCase("Cs"))
        for (int j = 0; j < nAxes[0]; j++) {
          if (index > 0 && j + 1 != index)
            continue;
          op = axes[0][j];
          sb.append("draw pgvp").append(m).appendI(j + 1).append(
              "disk scale ").appendF(scaleFactor * radius * 2).append(" CIRCLE PLANE ")
              .append(Escape.eP(center));
          v.add2(op.normalOrAxis, center);
          sb.append(Escape.eP(v)).append(" color translucent yellow;\n");
          v.add2(op.normalOrAxis, center);
          sb.append("draw pgvp").append(m).appendI(j + 1).append(
              "ring width 0.05 scale ").appendF(scaleFactor * radius * 2).append(" arc ")
              .append(Escape.eP(v));
          v.scaleAdd2(-2, op.normalOrAxis, v);
          sb.append(Escape.eP(v));
          v.add3(0.011f,  0.012f,  0.013f);
          sb.append(Escape.eP(v))
              .append("{0 360 0.5} color ")
              .append(
                  principalPlane != null && op.index == principalPlane.index ? "red"
                      : "blue").append(";\n");
        }
      sb.append("# name=").append(name);
      sb.append(", nCi=").appendI(haveInversionCenter ? 1 : 0);
      sb.append(", nCs=").appendI(nAxes[0]);
      sb.append(", nCn=").appendI(nType[OPERATION_PROPER_AXIS][0]);
      sb.append(", nSn=").appendI(nType[OPERATION_IMPROPER_AXIS][0]);
      sb.append(": ");
      for (int i = maxAxis; --i >= 2;)
        if (nAxes[i] > 0) {
          sb.append(" n").append(i < firstProper ? "S" : "C").appendI(i % firstProper);
          sb.append("=").appendI(nAxes[i]);
        }
      sb.append(";\n");
      drawInfo = sb.toString();
      return drawInfo;
    }
    int n = 0;
    int nTotal = 1;
    String ctype = (haveInversionCenter ? "Ci" : "center");
    if (haveInversionCenter)
      nTotal++;
    if (info == null)
      sb.append("\n\n").append(name).append("\t").append(ctype).append("\t").append(Escape.eP(center));
    else
      info.put(ctype, center);
    for (int i = maxAxis; --i >= 0;) {
      if (nAxes[i] > 0) {
        n = nUnique[i];
        String label = axes[i][0].getLabel();
        if (info == null)
          sb.append("\n\n").append(name).append("\tn").append(label).append("\t").appendI(nAxes[i]).append("\t").appendI(n);
        else
          info.put("n" + label, Integer.valueOf(nAxes[i]));
        n *= nAxes[i];
        nTotal += n;
        nType[axes[i][0].type][1] += n;
        Lst<V3> vinfo = (info == null ? null : new  Lst<V3>());
        for (int j = 0; j < nAxes[i]; j++) {
          //axes[i][j].typeIndex = j + 1;
          if (vinfo == null)
            sb.append("\n").append(name).append("\t").append(label).append("_").appendI(j + 1).append("\t"
               ).appendO(axes[i][j].normalOrAxis);
          else
            vinfo.addLast(axes[i][j].normalOrAxis);
        }
        if (info != null)
          info.put(label, vinfo);
      }
    }
    
    if (info == null) {
      sb.append("\n");
      sb.append("\n").append(name).append("\ttype\tnType\tnUnique");
      sb.append("\n").append(name).append("\tE\t  1\t  1");

      n = (haveInversionCenter ? 1 : 0);
      sb.append("\n").append(name).append("\tCi\t  ").appendI(n).append("\t  ").appendI(n);

      sb.append("\n").append(name).append("\tCs\t");
      PT.rightJustify(sb, "    ", nAxes[0] + "\t");
      PT.rightJustify(sb, "    ", nAxes[0] + "\n");

      sb.append(name).append("\tCn\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][0] + "\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_PROPER_AXIS][1] + "\n");

      sb.append(name).append("\tSn\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][0] + "\t");
      PT.rightJustify(sb, "    ", nType[OPERATION_IMPROPER_AXIS][1] + "\n");

      sb.append(name).append("\t\tTOTAL\t");
      PT.rightJustify(sb, "    ", nTotal + "\n");
      textInfo = sb.toString();
      return textInfo;
    }
    info.put("name", name);
    info.put("nAtoms", Integer.valueOf(nAtoms));
    info.put("nTotal", Integer.valueOf(nTotal));
    info.put("nCi", Integer.valueOf(haveInversionCenter ? 1 : 0));
    info.put("nCs", Integer.valueOf(nAxes[0]));
    info.put("nCn", Integer.valueOf(nType[OPERATION_PROPER_AXIS][0]));
    info.put("nSn", Integer.valueOf(nType[OPERATION_IMPROPER_AXIS][0]));
    info.put("distanceTolerance", Float.valueOf(distanceTolerance));
    info.put("linearTolerance", Float.valueOf(linearTolerance));
    info.put("detail", sb.toString().replace('\n', ';'));
    if (principalAxis != null && principalAxis.index > 0)
      info.put("principalAxis", principalAxis.normalOrAxis);
    if (principalPlane != null && principalPlane.index > 0)
      info.put("principalPlane", principalPlane.normalOrAxis);
    return info;
  }

  boolean isDrawType(String type, int index, float scale) {
    return (drawInfo != null && drawType.equals(type == null ? "" : type) 
        && drawIndex == index && this.scale  == scale);
  }
  
}
