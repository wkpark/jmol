/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Quaternion;

/*
 * Bob Hanson 7/2008
 * 
 * brute force -- preliminary from BCCE20 meeting 2008
 * 
 */

public class PointGroup {

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

  private int[] nAxes;
  private Operation[][] axes;

  private String name = "C_1?";

  public String getName() {
    return name;
  }

  private final Vector3f vTemp = new Vector3f();
  private int centerAtomIndex = -1;
  private boolean haveInversionCenter;

  final private Point3f center = new Point3f();

  public PointGroup(Atom[] atomset, BitSet bsAtoms, boolean haveVibration) {
    nAxes = new int[maxAxis];
    axes = new Operation[maxAxis][];
    Point3f[] atoms;
    if ((atoms = getCenter(atomset, bsAtoms)) == null) {
      Logger.error("Too many atoms for point group calculation");
      name = "point group not determined -- atomCount > " + ATOM_COUNT_MAX + " -- select fewer atoms and try again.";
      return;
    }
    int[] elements = new int[atoms.length];
    int n = 0;
    for (int i = atomset.length; --i >= 0;)
      if (bsAtoms.get(i)) {
        int bondIndex = 1 + Math.max(3, atomset[i].getCovalentBondCount());
        elements[n++] = atomset[i].getElementNumber() * bondIndex;
      }
    getElementArrays(atoms, elements);
    if (haveVibration) {
      Point3f[] atomVibs = new Point3f[atoms.length];
      for (int i = atoms.length; --i >= 0;) {
        atomVibs[i] = new Point3f(atoms[i]);
        Vector3f v = ((Atom)atoms[i]).getVibrationVector();
        if (v != null)
          atomVibs[i].add(v);
      }
      atoms = atomVibs;
    }
    findInversionCenter(atoms, elements);

    if (isLinear(atoms)) {
      if (haveInversionCenter) {
        name = "D(infinity)h";
      } else {
        name = "C(infinity)v";
      }
      return;
    }
    axes[0] = new Operation[15];
    int nPlanes = 0;
    findCAxes(atoms, elements);
    nPlanes = findPlanes(atoms, elements);
    findAdditionalAxes(nPlanes, atoms, elements);

    /* flow chart contribution of Dean Johnston */

    n = getHighestOrder();
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
          return;
        }
        if (haveInversionCenter) {
          name = "Ci";
          return;
        }
        name = "C1";
      } else if ((n % 2) == 1 && nAxes[c2] > 0 || (n % 2) == 0 && nAxes[c2] > 1) {
        // Dnh, Dnd, Dn, S4

        // here based on the presence of C2 axes in any odd-order group
        // and more than one C2 if even order (since the one will be part of the 
        // principal axis

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
        if (n < firstProper) {
          name = "S" + n;
        } else {
          name = "C" + (n - firstProper);
        }
      } else if (nPlanes == n - firstProper) {
        name = "C" + nPlanes + "v";
      } else {
        if (n < firstProper)
          n /= 2;
        else
          n -= firstProper;
        name = "C" + n + "h";
      }
    }
    //System.out.println(drawInfo());
  }

  private final static int ATOM_COUNT_MAX = 100;
  private Atom[] getCenter(Atom[] atomset, BitSet bsAtoms) {
    int atomCount = BitSetUtil.cardinalityOf(bsAtoms);
    if (atomCount > ATOM_COUNT_MAX)
      return null;
    Atom[] atoms = new Atom[atomCount];
    if (atomCount == 0)
      return atoms;
    int nAtoms = 0;
    for (int i = BitSetUtil.length(bsAtoms); --i >= 0;)
      if (bsAtoms.get(i)) {
        atoms[nAtoms] = atomset[i];
        center.add(atoms[nAtoms++]);
      }
    center.scale(1f / nAtoms);
    for (int i = nAtoms; --i >= 0;)
      if (atoms[i].distance(center) < 0.1) {
        centerAtomIndex = i;
        break;
      }
    return atoms;
  }

  private void findInversionCenter(Point3f[] atoms, int[] elements) {
    haveInversionCenter = checkOperation(atoms, elements, null, center, -1);
    if (haveInversionCenter) {
      axes[1] = new Operation[1];
      axes[1][0] = new Operation();
    }
  }

  private final static float DISTANCE_TOLERANCE = 0.2f;
  private boolean checkOperation(Point3f[] atoms, int[] elements, Quaternion q,
                                 Point3f center, int iOrder) {
    Point3f pt = new Point3f();
    int nFound = 0;
    boolean isInversion = (iOrder < firstProper);
    out: for (int i = atoms.length; --i >= 0 && nFound < atoms.length;)
      if (i == centerAtomIndex) {
        nFound++;
      } else {
        Point3f a1 = atoms[i];
        int e1 = elements[i];
        if (q != null) {
          pt.set(a1);
          pt.sub(center);
          q.transform(pt, pt);
          pt.add(center);
        } else {
          pt.set(a1);
        }
        if (isInversion) {
          // A trick here: rather than 
          // actually doing a rotation/reflection
          // we do a rotation INVERSION. This works
          // due to the symmetry of S2, S4, and S8
          // For S3 and S6, we play the trick of 
          // rotating as C6 and C3, respectively, 
          // THEN doing the rotation/inversion. 
          vTemp.sub(center, pt);
          pt.scaleAdd(2, vTemp, pt);
        }
        if ((q != null || isInversion) && pt.distance(a1) < DISTANCE_TOLERANCE) {
          nFound++;
          continue;
        }
        for (int j = atoms.length; --j >= 0;) {
          if (j == i || elements[j] != e1)
            continue;
          Point3f a2 = atoms[j];
          //  System.out.println(i + " " + j + " " + a1 + " " + a2 + " " + pt 
          //      + nFound + " " + pt.distance(a2));
          if (pt.distance(a2) < DISTANCE_TOLERANCE) {
            nFound++;
            continue out;
          }
        }
      }
    return nFound == atoms.length;
  }

  private boolean isLinear(Point3f[] atoms) {
    Vector3f v1 = null;
    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      if (v1 == null) {
        v1 = new Vector3f();
        v1.sub(atoms[i], center);
        v1.normalize();
        vTemp.set(v1);
        continue;
      }
      vTemp.sub(atoms[i], center);
      vTemp.normalize();
      if (!isParallel(v1, vTemp))
        return false;
    }
    return true;
  }

  private final static float LINEAR_DOT_MINIMUM = 0.99f; // 8 degrees

  private static boolean isParallel(Vector3f v1, Vector3f v2) {
    return (Math.abs(v1.dot(v2)) >= LINEAR_DOT_MINIMUM);
  }

  int maxElement = 0;
  int[] eCounts;

  private void getElementArrays(Point3f[] atoms, int[] elements) {
    for (int i = atoms.length; --i >= 0;) {
      int e1 = elements[i];
      if (e1 > maxElement)
        maxElement = e1;
    }
    eCounts = new int[++maxElement];
    for (int i = atoms.length; --i >= 0;)
      eCounts[elements[i]]++;
  }

  private int findCAxes(Point3f[] atoms, int[] elements) {
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector3f v3 = new Vector3f();
    Point3f pt = new Point3f();

    // look for the proper and improper axes relating pairs of atoms

    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      Point3f a1 = atoms[i];
      int e1 = elements[i];
      for (int j = atoms.length; --j > i;) {
        Point3f a2 = atoms[j];
        if (elements[j] != e1)
          continue;

        // look for all axes to average position of A and B
        // or including A and B if A - 0 - B is linear

        pt.add(a1, a2);
        pt.scale(0.5f);
        v1.sub(a1, center);
        v2.sub(a2, center);
        v1.normalize();
        v2.normalize();
        if (isParallel(v1, v2)) {
          getAllAxes(v1, atoms, elements);
          continue;
        }
        if (nAxes[c2] < axesMaxN[c2]) {
          v3.set(pt);
          getAllAxes(v3, atoms, elements);
        }

        // look for the axis perpendicular to the A -- 0 -- B plane

        float order = (float) (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) (order + 0.01f);
        boolean isIntegerOrder = (order - iOrder <= 0.02f);
        if (!isIntegerOrder || (iOrder = iOrder + firstProper) >= maxAxis)
          continue;
        if (nAxes[iOrder] < axesMaxN[iOrder]) {
          v3.cross(v1, v2);
          checkAxisOrder(atoms, elements, iOrder, v3, center);
        }
      }
    }

    // check all C2 axes for C3-related axes

    Vector3f[] vs = new Vector3f[nAxes[c2] * 2];
    for (int i = 0; i < vs.length; i++)
      vs[i] = new Vector3f();
    int n = 0;
    for (int i = 0; i < nAxes[c2]; i++) {
      vs[n++].set(axes[c2][i].normalOrAxis);
      vs[n].set(axes[c2][i].normalOrAxis);
      vs[n++].scale(-1);
    }
    for (int i = vs.length; --i >= 2;)
      for (int j = i; --j >= 1;)
        for (int k = j; --k >= 0;) {
          v3.set(vs[i]);
          v3.add(vs[j]);
          v3.add(vs[k]);
          if (v3.length() < 1.0)
            continue;
          checkAxisOrder(atoms, elements, c3, v3, center);
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

    out: for (int i = 0; i < atoms.length - 2; i++)
      if (elements[i] == iMin)
        for (int j = i + 1; j < atoms.length - 1; j++)
          if (elements[j] == iMin)
            for (int k = j + 1; k < atoms.length; k++)
              if (elements[k] == iMin) {
                v1.sub(atoms[i], atoms[j]);
                v2.sub(atoms[i], atoms[k]);
                v1.normalize();
                v2.normalize();
                v3.cross(v1, v2);
                getAllAxes(v3, atoms, elements);
//                checkAxisOrder(atoms, elements, 3, v3, center);
                pt.set(atoms[i]);
                pt.add(atoms[j]);
                pt.add(atoms[k]);
                v1.set(pt);
                v1.normalize();
                if (!isParallel(v1, v3))
                  getAllAxes(v1, atoms, elements);
                if (nAxes[c5] == axesMaxN[c5])
                  break out;
              }

    //check for C2 by looking for axes along element-based geometric centers

    vs = new Vector3f[maxElement];
    for (int i = atoms.length; --i >= 0;) {
      int e1 = elements[i];
      if (vs[e1] == null)
        vs[e1] = new Vector3f();
      else if (haveInversionCenter)
        continue;
      vs[e1].add(atoms[i]);
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
          if (haveInversionCenter) {
           v1.cross(vs[i], vs[j]);
          } else {
            v1.set(vs[i]);
            v1.sub(vs[j]);
          }
          checkAxisOrder(atoms, elements, c2, v1, center);
          
        }

    return getHighestOrder();
  }

  private void getAllAxes(Vector3f v3, Point3f[] atoms, int[] elements) {
    for (int o = c2; o < maxAxis; o++)
      if (nAxes[o] < axesMaxN[o])
        checkAxisOrder(atoms, elements, o, v3, center);
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

  private boolean checkAxisOrder(Point3f[] atoms, int[] elements, int iOrder,
                                 Vector3f v, Point3f center) {
    switch (iOrder) {
    case c8:
      if (nAxes[c3] > 0)
        return false;
      // fall through;
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
    Quaternion q = new Quaternion(v, (iOrder < firstProper ? 180 : 0) + 360 / (iOrder % firstProper));
    if (!checkOperation(atoms, elements, q, center, iOrder))
      return false;
    addAxis(iOrder, v);
    // check for Sn:
    switch (iOrder) {
    case c2:
      checkAxisOrder(atoms, elements, s4, v, center);//D2d, D4h, D6d
      break;
    case c3:
      checkAxisOrder(atoms, elements, s3, v, center);//C3h, D3h
      if (haveInversionCenter)
        addAxis(s6, v);
      break;
    case c4:
      addAxis(c2, v);
      checkAxisOrder(atoms, elements, s4, v, center);//D2d, D4h, D6d
      checkAxisOrder(atoms, elements, s8, v, center);//D4d
      break;
    case c5:
      checkAxisOrder(atoms, elements, s5, v, center); //C5h, D5h
      if (haveInversionCenter)
        addAxis(s10, v);
      break;
    case c6:
      addAxis(c2, v);
      addAxis(c3, v);
      checkAxisOrder(atoms, elements, s3, v, center);//C6h, D6h
      checkAxisOrder(atoms, elements, s6, v, center);//C6h, D6h
      checkAxisOrder(atoms, elements, s12, v, center);//D6d
      break;
    case c8:
      //note -- D8d would have a S16 axis. This will not be found.
      addAxis(c2, v);
      addAxis(c4, v);
      break;
    }
    return true;
  }

  private void addAxis(int iOrder, Vector3f v) {
    if (haveAxis(iOrder, v))
      return;
    if (axes[iOrder] == null)
      axes[iOrder] = new Operation[axesMaxN[iOrder]];
    axes[iOrder][nAxes[iOrder]++] = new Operation(v, iOrder);
  }

  private boolean haveAxis(int iOrder, Vector3f v) {
    if (nAxes[iOrder] == axesMaxN[iOrder]) {
      //System.out.println(iOrder + " " + (iOrder-firstProper) + " is maxed at " + axesMaxN[iOrder]);
      return true;
    }
    if (nAxes[iOrder] > 0)
      for (int i = nAxes[iOrder]; --i >= 0;) {
        if (isParallel(v, axes[iOrder][i].normalOrAxis))
          return true;
      }
    return false;
  }

  private int findPlanes(Point3f[] atoms, int[] elements) {
    Operation[] axesC2 = axes[c2];
    int nC2 = nAxes[c2];

    Point3f pt = new Point3f();
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector3f v3 = new Vector3f();
    int nPlanes = 0;
    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      Point3f a1 = atoms[i];
      int e1 = elements[i];
      for (int j = atoms.length; --j > i;) {
        if (elements[j] != e1)
          continue;

        // plane are treated as S2 axes here

        // first, check planes through two atoms and the center
        // or perpendicular to a linear A -- 0 -- B set

        Point3f a2 = atoms[j];
        pt.add(a1, a2);
        pt.scale(0.5f);
        v1.sub(a1, center);
        v2.sub(a2, center);
        if (!isParallel(v1, v2)) {
          v3.cross(v1, v2);
          v3.normalize();
          nPlanes = getPlane(nPlanes, v3, atoms, elements, center);
        }

        // second, look for planes perpendicular to the A -- B line

        v3.set(a2);
        v3.sub(a1);
        v3.normalize();
        nPlanes = getPlane(nPlanes, v3, atoms, elements, center);
        if (nPlanes == axesMaxN[0])
          return nPlanes;
      }
    }

    // also look for planes normal to any C2 axis

    for (int i = 0; i < nC2; i++)
      nPlanes = getPlane(nPlanes, axesC2[i].normalOrAxis, atoms, elements,
          center);
    return nPlanes;
  }

  private int getPlane(int nPlanes, Vector3f v3, Point3f[] atoms,
                       int[] elements, Point3f center2) {
    if (!haveAxis(0, v3)
        && checkOperation(atoms, elements, new Quaternion(v3, 180), center,
            -1))
      axes[0][nAxes[0]++] = new Operation(v3);
    return nAxes[0];
  }

  private void findAdditionalAxes(int nPlanes, Point3f[] atoms, int[] elements) {

    Operation[] planes = axes[0];
    int Cn = 0;
    if (nPlanes > 1
        && ((Cn = nPlanes + firstProper) < maxAxis) 
        && nAxes[Cn] == 0) {
      // cross pairs of plane normals. We don't need many.
      vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
      if (!checkAxisOrder(atoms, elements, Cn, vTemp, center)
          && nPlanes > 2) {
        vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
        checkAxisOrder(atoms, elements, Cn - 1, vTemp, center);
      }
    }
    if (nAxes[c2] == 0 && nPlanes > 2) {
      // check for C2 axis relating 
      for (int i = 0; i < nPlanes - 1; i++) {
        for (int j = i + 1; j < nPlanes; j++) {
          vTemp.add(planes[1].normalOrAxis, planes[2].normalOrAxis);
          if (checkAxisOrder(atoms, elements, c2, vTemp, center))
            System.out.println("found a C2 axis by adding plane normals");
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

  public class Operation {

    int type;
    int order;
    Vector3f normalOrAxis;

    Operation() {
      type = OPERATION_INVERSION_CENTER;
      //if (Logger.debugging)
        Logger.info("new operation -- " + typeNames[type]);
      order = 1;
    }

    Operation(Vector3f v, int i) {
      type = (i < firstProper ? OPERATION_IMPROPER_AXIS : OPERATION_PROPER_AXIS);
      order = i % firstProper;
      normalOrAxis = new Quaternion(v, 180).getNormal();
      //if (Logger.debugging)
        Logger.info("new operation -- " + (order == i ? "S" : "C") + order + " "
            + normalOrAxis);
    }

    Operation(Vector3f v) {
      type = OPERATION_PLANE;
      normalOrAxis = new Quaternion(v, 180).getNormal();
      //if (Logger.debugging)
        Logger.info("new operation -- plane " + normalOrAxis);
    }

    public String toString() {
      return type + " " + order + " " + normalOrAxis;
    }

    public String getLabel() {
      switch (type) {
      case OPERATION_PLANE:
        return "";
      case OPERATION_IMPROPER_AXIS:
        return "S" + order;
      default:
        return "C" + order;
      }
    }
  }

  public String drawInfo() {
    Vector3f v = new Vector3f();
    Operation op;
    int n = 0;
    int[] nType = new int[4];
    for (int i = 1; i < maxAxis; i++) {
      n += nAxes[i];
      for (int j = nAxes[i]; --j >= 0;)
        nType[axes[i][j].type]++; 
    }
    StringBuffer sb = new StringBuffer("set perspectivedepth off;\n");
    sb.append("draw p0" + (haveInversionCenter ? "inv " : " ") + Escape.escape(center)
        + (haveInversionCenter ? "\"i\";\n" : ";\n"));
    for (int i = 2; i < maxAxis; i++) {
      for (int j = 0; j < nAxes[i]; j++) {
        op = axes[i][j];
        v.set(op.normalOrAxis);
        v.add(center);
        float scale = (4.0f + op.order / 4.0f) * (op.type == OPERATION_IMPROPER_AXIS ? -1 : 1);
        sb.append("draw va").append(op.getLabel()).append("_").append(j).append(
            " width 0.05 scale " + scale + " ").append(Escape.escape(v));
        v.scaleAdd(-2, op.normalOrAxis, v);
        sb.append(Escape.escape(v)).append("\"" + op.getLabel() + "\"").append(
            op.type == OPERATION_IMPROPER_AXIS ? " color red" : "").append(
            ";\n");
      }
    }
    for (int j = 0; j < nAxes[0]; j++) {
      op = axes[0][j];
      v.set(op.normalOrAxis);
      v.scale(0.025f);
      v.add(center);
      sb.append("draw vp").append(j).append("disk width 6.0 cylinder ").append(
          Escape.escape(v));
      v.scaleAdd(-0.05f, op.normalOrAxis, v);
      sb.append(Escape.escape(v)).append(" color translucent;\n");

      v.set(op.normalOrAxis);
      v.add(center);
      sb.append("draw vp").append(j).append("ring width 0.05 scale 3.0 arc ")
          .append(Escape.escape(v));
      v.scaleAdd(-2, op.normalOrAxis, v);
      sb.append(Escape.escape(v));
      v.x += 0.011;
      v.y += 0.012;
      v.z += 0.013;
      sb.append(Escape.escape(v)).append("{0 360 0.5} color red;\n");
    }
    sb.append("# name=" + name);
    sb.append(", nCi=" + (haveInversionCenter ? 1 : 0));
    sb.append(", nCs=" + nAxes[0]);
    sb.append(", nCn=" + nType[OPERATION_PROPER_AXIS]);
    sb.append(", nSn=" + nType[OPERATION_IMPROPER_AXIS]);
    sb.append(": ");
    for (int i = maxAxis; --i >= 2; )
      if (nAxes[i] > 0)
        sb.append(" n" + (i < firstProper ? "S" : "C") + (i % firstProper) + "=" + nAxes[i]);
    sb.append(";\n");
    return sb.toString();
  }
}
