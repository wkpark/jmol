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

  private String name = "C_1";

  public String getName() {
    return name;
  }

  private int nOperations = 0;

  public int getOperationCount() {
    return nOperations;
  }

  private Operation[] operations;

  public Operation getOperation(int i) {
    if (i >= nOperations)
      return null;
    return operations[i];
  }

  private final Vector3f vTemp = new Vector3f();
  private int centerAtomIndex = -1;

  final private Point3f center = new Point3f();

  public PointGroup(Atom[] atomset, BitSet bsAtoms) {
    Point3f[] atoms;
    if ((atoms = getCenter(atomset, bsAtoms)) == null)
      return;
    int[] elements = new int[atoms.length];
    int n = 0;
    for (int i = atomset.length; --i >= 0;)
      if (bsAtoms.get(i))
        elements[n++] = atomset[i].getElementNumber();
    boolean haveInversionCenter = haveInversionCenter(atoms, elements);
    if (isLinear(atoms)) {
      if (haveInversionCenter) {
        name = "D_(infinity)h";
        nOperations = 2;
        operations = new Operation[2];
        operations[0] = new Operation(vTemp, 0);
        operations[1] = new Operation(vTemp);
      } else {
        name = "C_(infinity)v";
        nOperations = 1;
        operations = new Operation[2];
        operations[0] = new Operation(vTemp, 0);
      }
      return;
    }
    int[] nC = new int[9];
    Operation[][] axes = new Operation[9][];
    Operation[] planes = null;
    int nPlanes = 0;
    findAxes(atoms, elements, nC, axes, false);
    for (n = 9; --n > 1 && nC[n] == 0;) {
    }
    n = findAxesByElement(atoms, elements, nC, axes, haveInversionCenter);
    if (nC[3] > 1) {
      // must be Ix, Ox, or Tx
      if (nC[5] > 1) {
        planes = new Operation[20];
        nPlanes = findPlanes(atoms, elements, nC, planes, axes[2], nC[2]);
        if (haveInversionCenter) {
          name = "I_h";
        } else {
          name = "I";
        }
      } else if (nC[4] > 1) {
        if (haveInversionCenter) {
          name = "O_h";
        } else {
          name = "O";
        }
      } else {
        planes = new Operation[6];
        nPlanes = findPlanes(atoms, elements, nC, planes, axes[2], nC[2]);
        if (nPlanes > 0) {
          if (haveInversionCenter) {
            name = "T_h";
          } else {
            name = "T_d";
          }
        } else {
          name = "T";
        }
      }
    } else {
      // options Dnh, Dnd, Dn, Cnh, Cnv, Cn, Sn, Ci, Cs
      planes = new Operation[9];
      nPlanes = findPlanes(atoms, elements, nC, planes, axes[2], nC[2]);
      if (nPlanes > 1 && nPlanes <= 8 && nC[nPlanes] == 0) {
        vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
        if (!checkAxisOrder(atoms, elements, nC, nPlanes, vTemp, axes, center,
            false)
            && nPlanes > 2) {
          vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
          checkAxisOrder(atoms, elements, nC, nPlanes - 1, vTemp, axes, center,
              false);
        }
      }
      for (n = 9; --n > 1 && nC[n] == 0;) {
      }
      if (nC[2] == 0 && nPlanes > 0) {
        for (int i = 0; i < nPlanes - 1; i++) {
          for (int j = i + 1; j < nPlanes; j++) {
            vTemp.add(planes[1].normalOrAxis, planes[2].normalOrAxis);
            checkAxisOrder(atoms, elements, nC, 2, vTemp, axes, center, false);
          }
        }

      }
      if (n < 2) {
        if (nPlanes == 1) {
          // C_s, C_i, S_n, C_1
          name = "C_s";
          nOperations = 1;
          operations = new Operation[1];
          operations[0] = planes[0];
          return;
        }
        if (haveInversionCenter) {
          name = "C_i";
          nOperations = 1;
          operations = new Operation[1];
          operations[0] = new Operation();
          return;
        }
        name = "C_1?";
      } else if ((n % 2) == 1 && nC[2] > 0 || (n % 2) == 0 && nC[2] > 1) {
        // Dnh, Dnd, Dn
        switch (nPlanes - n) {
        case 1:
          name = "D_" + n + "h";
          break;
        case 0:
          name = "D_" + n + "d";
          break;
        default:
          if (axes[n][0].type == OPERATION_IMPROPER_AXIS) {
            name = "S_" + n;
          } else {
            name = "D_" + n;
          }
        }
      } else if (nPlanes == 1) {
        name = "C_" + n + "h";
      } else if (nPlanes == n) {
        name = "C_" + n + "v";
      } else {
        if (axes[n][0].type == OPERATION_IMPROPER_AXIS) {
          name = "S_" + n;
        } else {
          name = "C_" + n;
        }
      }
    }
    dumpAxes(nC, axes);
    dumpPlanes(nPlanes, planes);
  }

  private Atom[] getCenter(Atom[] atomset, BitSet bsAtoms) {
    int atomCount = BitSetUtil.cardinalityOf(bsAtoms);
    if (atomCount > 100) {
      Logger.error("Too many atoms for point group calculation");
      return null;
    }
    Atom[] atoms = new Atom[atomCount];
    int nAtoms = 0;
    for (int i = BitSetUtil.length(bsAtoms); --i >= 0;)
      if (bsAtoms.get(i)) {
        atoms[nAtoms] = atomset[i];
        center.add(atoms[nAtoms++]);
      }
    if (nAtoms < 2)
      return null;
    center.scale(1f / nAtoms);
    for (int i = nAtoms; --i >= 0;)
      if (atoms[i].distance(center) < 0.1) {
        centerAtomIndex = i;
        break;
      }
    return atoms;
  }

  private boolean haveInversionCenter(Point3f[] atoms, int[] elements) {
    return checkOperation(atoms, elements, null, center, true);
  }

  private boolean checkOperation(Point3f[] atoms, int[] elements, Quaternion q,
                                 Point3f center, boolean isInversion) {
    Point3f pt = new Point3f();
    int nFound = 0;
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
          vTemp.sub(center, pt);
          pt.scaleAdd(2, vTemp, pt);
        }
        if ((q != null || isInversion) && pt.distance(a1) < 0.1) {
          nFound++;
          continue;
        }
        for (int j = atoms.length; --j >= 0;) {
          if (j == i || elements[j] != e1)
            continue;
          Point3f a2 = atoms[j];
          if (pt.distance(a2) < 0.1) {
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

  private final static float LINEAR_DOT_MINIMUM = 0.9999f;

  private static boolean isParallel(Vector3f v1, Vector3f v2) {
    return (Math.abs(v1.dot(v2)) >= LINEAR_DOT_MINIMUM);
  }

  final static int[] axesMaxN = new int[] { 0, 0, 15, 10, 6, 6, 1, 1, 1 };

  private Operation findAxes(Point3f[] atoms, int[] elements, int[] nC,
                             Operation[][] axes, boolean isImproper) {
    Point3f pt = new Point3f();
    Point3f pt1 = new Point3f();
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector3f v3 = new Vector3f();
    for (int i = atoms.length; --i >= 0;) {
      if (i == centerAtomIndex)
        continue;
      Point3f a1 = atoms[i];
      int e1 = elements[i];
      for (int j = atoms.length; --j > i;) {
        Point3f a2 = atoms[j];
        if (elements[j] != e1)
          continue;
        if (isImproper) {
          v1.sub(center, a1);
          pt1.scaleAdd(2, v1, a1);
        } else {
          pt1.set(a1);
        }
        pt.add(pt1, a2);
        pt.scale(0.5f);
        v1.sub(a1, center);
        v2.sub(a2, center);
        v1.normalize();
        v2.normalize();
        if (isParallel(v1, v2)) {
          if (!isImproper) {
            for (int o = 2; o <= 8; o++)
              checkAxisOrder(atoms, elements, nC, o, v1, axes, center, false);
          }
          continue;
        }
        if (nC[2] < axesMaxN[2]) {
          v3.set(pt);
          if (checkAxisOrder(atoms, elements, nC, 2, v3, axes, center,
              isImproper) && !isImproper)
            checkAxisOrder(atoms, elements, nC, -4, v3, axes, center,
              true);
        }
        float order = (float) (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) (order + 0.01f);
        boolean isIntegerOrder =(order - iOrder <= 0.02f); 
        if (!isIntegerOrder)
          continue; // not an integer order
        if (isImproper) {
          if (iOrder != 4 && iOrder != 6)
            continue;
        }
        if (iOrder <= 8 && nC[iOrder] < axesMaxN[iOrder]) {
          // not a valid order, or plenty of these already
          v3.cross(v1, v2);
          checkAxisOrder(atoms, elements, nC, iOrder, v3, axes, center,
              isImproper);
        }
      }
    }
    if (isImproper)
      return null;
    Vector3f[] vs = new Vector3f[nC[2] * 2];
    for (int i = 0; i < vs.length; i++)
      vs[i] = new Vector3f();
    int n = 0;
    for (int i = 0; i < nC[2]; i++) {
      vs[n++].set(axes[2][i].normalOrAxis);
      vs[n].set(axes[2][i].normalOrAxis);
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
          checkAxisOrder(atoms, elements, nC, 3, v3, axes, center, false);
        }
    return null;
  }

  private int findAxesByElement(Point3f[] atoms, int[] elements, int[] nC,
                                Operation[][] axes, boolean isInversion) {
    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector3f v3 = new Vector3f();
    Vector3f[] vs = new Vector3f[120];
    int[] eCounts = new int[120];
    int maxElement = 0;
    for (int i = atoms.length; --i >= 0;) {
      Point3f a1 = atoms[i];
      int e1 = elements[i];
      if (vs[e1] == null)
        vs[e1] = new Vector3f();
      vs[e1].add(a1);
      eCounts[e1]++;
      if (e1 > maxElement)
        maxElement = e1;
    }
    int nMin = Integer.MAX_VALUE;
    int iMin = -1;
    for (int i = 0; i <= maxElement; i++)
      if (vs[i] != null) {
        if (eCounts[i] < nMin && eCounts[i] > 1) {
          nMin = eCounts[i];
          iMin = i;
        }
        vs[i].scale(1f / eCounts[i]);
      }
    if (!isInversion) {
      for (int i = 0; i <= maxElement; i++)
        if (vs[i] != null)
          for (int j = 0; j <= maxElement; j++) {
            if (i == j || vs[j] == null)
              continue;
            v1.set(vs[i]);
            v1.sub(vs[j]);
            //if (isInversion)v1.set(0,0,1);
            //System.out.println("draw " + Escape.escape(center) + Escape.escape(v1));
            for (int o = 8; o >= 2; o--) { 
              checkAxisOrder(atoms, elements, nC, o, v1, axes, center,
                  isInversion);
//              if (nC[o] > 0)
  //              return o;
            }
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
                //System.out.println(v3 + " " + (v1.dot(v2)/Math.PI * 180));
                for (int o = 8; o >= 2; o--) {
                  if (!checkAxisOrder(atoms, elements, nC, o, v3, axes, center,
                      isInversion))
                    checkAxisOrder(atoms, elements, nC, o, v3, axes, center,
                        !isInversion);
                }
                if (nC[5] > 2)
                  break out;
              }
    int n;
    for (n = 9; --n > 1 && nC[n] == 0;) {
    }
    return n;
  }

  private boolean checkAxisOrder(Point3f[] atoms, int[] elements, int[] nC,
                                 int iOrder, Vector3f v, Operation[][] axes,
                                 Point3f center, boolean doInversion) {
    v.normalize();
    int aiOrder = Math.abs(iOrder);
    if (haveAxis(nC, iOrder, v, axes[aiOrder]))
      return false;
    Quaternion q = new Quaternion(v, 360 / (iOrder < 0 ? -iOrder : iOrder));
    if (!checkOperation(atoms, elements, q, center, doInversion))
      return false;
    addAxis(nC, (iOrder < 0 ? iOrder : doInversion ? -iOrder : iOrder), v, axes);
    switch (iOrder) {
    case 4:
      addAxis(nC, 2, v, axes);
      break;
    case 6:
      addAxis(nC, 2, v, axes);
      addAxis(nC, 3, v, axes);
      break;
    case 8:
      addAxis(nC, 2, v, axes);
      addAxis(nC, 4, v, axes);
      break;
    }
    return true;
  }

  private void addAxis(int[] nC, int iOrder, Vector3f v, Operation[][] axes) {
    int aiOrder = Math.abs(iOrder);
    if (haveAxis(nC, iOrder, v, axes[aiOrder]))
      return;
    Operation[] axesSet = axes[aiOrder];
    if (axesSet == null)
      axesSet = axes[aiOrder] = new Operation[axesMaxN[aiOrder]];
    axesSet[nC[aiOrder]++] = new Operation(v, iOrder);
  }

  private boolean haveAxis(int[] nC, int iOrder, Vector3f v, Operation[] axes) {
    iOrder = Math.abs(iOrder);
    if (nC[iOrder] > 0)
      for (int i = nC[iOrder]; --i >= 0;) {
        if (isParallel(v, axes[i].normalOrAxis))
          return true;
      }
    return false;
  }

  private int findPlanes(Point3f[] atoms, int[] elements, int[] nC,
                         Operation[] planes, Operation[] axesC2, int nC2) {
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
        Point3f a2 = atoms[j];
        pt.add(a1, a2);
        pt.scale(0.5f);
        v1.sub(a1, center);
        v2.sub(a2, center);
        if (isParallel(v1, v2)) {
          v3.set(v1);
        } else {
          v3.cross(v1, v2);
        }
        v3.normalize();
        if (!haveAxis(nC, 0, v3, planes)
            && checkOperation(atoms, elements, new Quaternion(v3, 180), center,
                true))
          nPlanes = addPlane(nC, v3, planes);
        v3.set(a2);
        v3.sub(a1);
        v3.normalize();
        if (!haveAxis(nC, 0, v3, planes)
            && checkOperation(atoms, elements, new Quaternion(v3, 180), pt,
                true))
          nPlanes = addPlane(nC, v3, planes);
        if (nPlanes == planes.length)
          return nPlanes;
      }
    }
    for (int i = 0; i < nC2; i++) {
      v3 = axesC2[i].normalOrAxis;
      if (!haveAxis(nC, 0, v3, planes)
          && checkOperation(atoms, elements, new Quaternion(v3, 180), center,
              true))
        nPlanes = addPlane(nC, v3, planes);
    }
    return nPlanes;
  }

  private int addPlane(int[] nC, Vector3f v3, Operation[] planes) {
    planes[nC[0]++] = new Operation(v3);
    return nC[0];
  }

  final static int OPERATION_AXIS = 0;
  final static int OPERATION_PLANE = 1;
  final static int OPERATION_IMPROPER_AXIS = 2;
  final static int OPERATION_INVERSION_CENTER = 3;

  final static String[] typeNames = { "axis", "plane", "improper axis", "center of inversion" };

  public class Operation {

    int type;
    int order;
    Vector3f normalOrAxis;

    Operation() {
      type = OPERATION_INVERSION_CENTER;
      Logger.info("new operation -- " + typeNames[type]);
      order = 1;
    }

    Operation(Vector3f v, int i) {
      type = (i < 0 ? OPERATION_IMPROPER_AXIS : OPERATION_AXIS);
      order = i;
      normalOrAxis = new Vector3f(v);
      normalOrAxis.normalize();
      Logger.info("new operation -- " + typeNames[type] + " order = " + i + " " + normalOrAxis);
    }

    Operation(Vector3f v) {
      type = OPERATION_PLANE;
      normalOrAxis = new Vector3f(v);
      normalOrAxis.normalize();
      Logger.info("new operation -- " + typeNames[type] + " " + normalOrAxis );
    }

    public String toString() {
      return type + " " + order + " " + normalOrAxis;
    }
  }

  private void dumpAxes(int[] nC, Operation[][] axes) {
    Vector3f v = new Vector3f();
    for (int i = 2; i <= 8; i++) {
      for (int j = 0; j < nC[i]; j++) {
        v.set(axes[i][j].normalOrAxis);
        String s = "draw va" + i + "_" + j + " scale 5.0 " + Escape.escape(v);
        v.scale(-1);
        s += Escape.escape(v);
        System.out.println(s);
      }
    }
    System.out.println("#NOTE: THIS IS AN INCOMPLETE LIST");
  }

  private void dumpPlanes(int n, Operation[] planes) {
    Vector3f v = new Vector3f();
    for (int j = 0; j < n; j++) {
      v.set(planes[j].normalOrAxis);
      v.set(planes[j].normalOrAxis);
      String s = "draw vp" + j + " scale 3.0 plane perp " + Escape.escape(v);
      v.scale(-1);
      s += Escape.escape(v);
      System.out.println(s);
    }
    System.out.println("#NOTE: THIS IS AN INCOMPLETE LIST");
  }

}
