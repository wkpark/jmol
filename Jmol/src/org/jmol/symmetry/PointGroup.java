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

  private final static int[] axesMaxProperN = new int[] { 
    0, // not used
    0, // not used 
    15,// C2 
    10,// C3 
    6, // C4
    6, // C5
    10, // C6,S6
    };

  private final static int[] axesMaxImproperN = new int[] { 
    9, // S2 planes
    0, // not used 
    0, // n/a
    0, // n/a
    1, // S4
    0, // C5
    10, // S6
    0, // not used
    1, // S8
    0, // not used
    6, // S10
    0, // not used 
    1  // S12
    };

  private static int maxProper = axesMaxProperN.length;   
  private static int maxImproper = axesMaxImproperN.length;
  
  private int[] nC;
  private Operation[][] axes;
  
  private String name = "C_1?";

  public String getName() {
    return name;
  }

  private final Vector3f vTemp = new Vector3f();
  private int centerAtomIndex = -1;
  private boolean haveInversionCenter;
  
  final private Point3f center = new Point3f();
  
  public PointGroup(Atom[] atomset, BitSet bsAtoms) {
    nC = new int[maxImproper];
    axes = new Operation[maxImproper][];
    Point3f[] atoms;
    if ((atoms = getCenter(atomset, bsAtoms)) == null)
      return;
    int[] elements = new int[atoms.length];
    int n = 0;
    for (int i = atomset.length; --i >= 0;)
      if (bsAtoms.get(i))
        elements[n++] = atomset[i].getElementNumber();
    getElementArrays(atoms, elements);

    haveInversionCenter = haveInversionCenter(atoms, elements);

    if (isLinear(atoms)) {
      if (haveInversionCenter) {
        name = "D_(infinity)h";
      } else {
        name = "C_(infinity)v";
      }
      return;
    }
    axes[0] = new Operation[9];
    int nPlanes = 0;
    //findAxes(atoms, elements, false);

    //    n = findAxesByElement(atoms, elements, haveInversionCenter);

    findCAxes(atoms, elements);
    nPlanes = findPlanes(atoms, elements);
    findAdditionalAxes(nPlanes, atoms, elements);

    /* flow chart contribution of Dean Johnston */

    n = getHighestOrder(nC);

    if (nC[3] > 1) {
      // must be Ix, Ox, or Tx
      if (nC[5] > 1) {
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
      if (n < 2) {
        if (nPlanes == 1) {
          // C_s, C_i, S_2, C_1
          name = "C_s";
          return;
        }
        if (haveInversionCenter) {
          name = "C_i";
          return;
        }
        name = "C_1";
      } else if ((n % 2) == 1 && nC[2] > 0 || (n % 2) == 0 && nC[2] > 1) {
        // here based on the presence of C2 axes in any odd-order group
        // and more than one C2 if even order (since the one will be part of the 
        // principal axis

        // Dnh, Dnd, Dn, Sn
        if (nPlanes == 0) {
          if (axes[n][0].type == OPERATION_IMPROPER_AXIS) {
            name = "S_" + n;
          } else {
            name = "D_" + n;
          }
        } else {
          if (axes[n][0].type == OPERATION_IMPROPER_AXIS)
            n /= 2;
          if (nPlanes == n) {
            name = "D_" + n + "d";
          } else {
            name = "D_" + n + "h";
          }
        }
      } else if (nPlanes == 0) {
        if (axes[n][0].type == OPERATION_IMPROPER_AXIS) {
          name = "S_" + n;
        } else {
          name = "C_" + n;
        }
      } else if (nPlanes == n) {
        name = "C_" + n + "v";
      } else {
        name = "C_" + n + "h";
      }
    }
    System.out.println(drawInfo());
  }

  private void findAdditionalAxes(int nPlanes, Point3f[] atoms, int[] elements) {
    
    Operation[] planes = axes[0];
    // first, cross pairs of plane normals. We don't need many.
    if (nPlanes > 1 && nPlanes < maxProper && nC[nPlanes] == 0) {
      vTemp.cross(planes[0].normalOrAxis, planes[1].normalOrAxis);
      if (!checkAxisOrder(atoms, elements, nPlanes, vTemp, center)
          && nPlanes > 2) {
        vTemp.cross(planes[1].normalOrAxis, planes[2].normalOrAxis);
        checkAxisOrder(atoms, elements, nPlanes - 1, vTemp, center);
      }
    }
    if (nC[2] == 0 && nPlanes > 2) {
      for (int i = 0; i < nPlanes - 1; i++) {
        for (int j = i + 1; j < nPlanes; j++) {
          vTemp.add(planes[1].normalOrAxis, planes[2].normalOrAxis);
          checkAxisOrder(atoms, elements, 2, vTemp, center);
        }
      }
    }
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
  
/*
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
        
        //define *pt* based on cross-products
        //this is for Dnh and Cnh, ferrocene-types
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
              checkAxisOrder(atoms, elements, o, v1, center);
          }
          continue;
        }
        if (nC[2] < axesMaxN[2]) {
          v3.set(pt);
          checkAxisOrder(atoms, elements, 2, v3, center);
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
          checkAxisOrder(atoms, elements, iOrder, v3, center);
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
          checkAxisOrder(atoms, elements, 3, v3, center);
        }
    return null;
  }
*/
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
        if (isParallel(v1, v2)){
          getAllAxes(v1, atoms, elements);
          continue;
        } 
        if (nC[2] < axesMaxProperN[2]) {
          v3.set(pt);
          getAllAxes(v3, atoms, elements);
        }
        
        // look for the axis perpendicular to the A -- 0 -- B plane

        float order = (float) (2 * Math.PI / v1.angle(v2));
        int iOrder = (int) (order + 0.01f);
        boolean isIntegerOrder =(order - iOrder <= 0.02f); 
        if (!isIntegerOrder)
          continue;
        if (nC[iOrder] < axesMaxProperN[iOrder]) {
          v3.cross(v1, v2);
          checkAxisOrder(atoms, elements, iOrder, v3, center);
        }
      }
    }
    
    // check all C2 axes for C3-related axes
    
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
          checkAxisOrder(atoms, elements, 3, v3, center);
        }

    // Check all pairs of atoms for C2 relationships
    
    
    // Now check for triples of elements that will define
    // axes using the element with the smallest
    // number of atoms n, with n >= 3 
    // cross all triples of vectors looking for standard
    // principal axes quantities.

    // Also check for vectors from {0 0 0} to
    // the midpoint of each triple of atoms

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
                checkAxisOrder(atoms, elements, 3, v3, center);
                pt.set(atoms[i]);
                pt.add(atoms[j]);
                pt.add(atoms[k]);
                v1.set(pt);
                v1.normalize();
                if (!isParallel(v1, v3))
                  getAllAxes(v1, atoms, elements);
                if (nC[5] == axesMaxProperN[5])
                  break out;
              }
    
    // get minimum element count > 2

    if (!haveInversionCenter) {

      //check for C2 by looking for axes along element-based geometric centers

      vs = new Vector3f[maxElement];
      for (int i = atoms.length; --i >= 0;) {
        int e1 = elements[i];
        if (vs[e1] == null)
          vs[e1] = new Vector3f();
        vs[e1].add(atoms[i]);
      }
      for (int i = 0; i < maxElement; i++)
        if (vs[i] != null)
          vs[i].scale(1f / eCounts[i]);

      // check for vectors from {0 0 0} to
      // the midpoint of each pair of atoms
      // within the same element

      for (int i = 0; i < maxElement; i++)
        if (vs[i] != null)
          for (int j = 0; j < maxElement; j++) {
            if (i == j || vs[j] == null)
              continue;
            v1.set(vs[i]);
            v1.sub(vs[j]);
            checkAxisOrder(atoms, elements, 2, v1, center);
          }
    }

    return getHighestOrder(nC);
  }

  private void getAllAxes(Vector3f v3, Point3f[] atoms, int[] elements) {
    for (int o = maxProper; --o >= 2;) 
      if (nC[o] < axesMaxProperN[o])
        checkAxisOrder(atoms, elements, o, v3, center);
  }

  private int getHighestOrder(int[] nC) {
    int n;
    for (n = maxImproper; --n > 1 && nC[n] == 0;) {
    }
    return n;
  }
/*  
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
            getAllAxes(v1, atoms, elements);
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
                getAllAxes(v3, atoms, elements);
                if (nC[5] > 2)
                  break out;
              }
    return getHighestOrder(nC);
  }
*/
  private boolean checkAxisOrder(Point3f[] atoms, int[] elements,
                                 int iOrder, Vector3f v, Point3f center) {
    switch (iOrder) {
    case 6:
    case 4:
      if (nC[5] > 0)
        return false;
      break;
    case 5:
      if (nC[4] > 0 || nC[6] > 0)
        return false;
      break;
    }
    
    v.normalize();
    if (haveAxis(iOrder, v))
      return false;
    Quaternion q = new Quaternion(v, 360 / (iOrder < 0 ? -iOrder : iOrder));
    if (!checkOperation(atoms, elements, q, center, iOrder < 0))
      return false;
    addAxis(iOrder, v);
    switch (iOrder) {
    case 2:
      checkAxisOrder(atoms, elements, -4, v, center);
      break;
    case 3:
      checkAxisOrder(atoms, elements, -6, v, center);
      break;
    case 4:
      addAxis(2, v);
      checkAxisOrder(atoms, elements, -8, v, center);
      break;
    case 5:
      checkAxisOrder(atoms, elements, -10, v, center);
      break;
    case 6:
      addAxis(2, v);
      addAxis(3, v);
      break;
    case 8:
      addAxis(2, v);
      addAxis(4, v);
      break;
    }
    return true;
  }

  private void addAxis(int iOrder, Vector3f v) {
    int aiOrder = Math.abs(iOrder);
    if (haveAxis(iOrder, v))
      return;
    if (axes[aiOrder] == null)
      axes[aiOrder] = new Operation[Math.max(
          (aiOrder < maxProper ? axesMaxProperN[aiOrder] : 0),
          axesMaxImproperN[aiOrder])];
    axes[aiOrder][nC[aiOrder]++] = new Operation(v, iOrder);
  }

  private boolean haveAxis(int iOrder, Vector3f v) {
    int aiOrder = Math.abs(iOrder);
    if (nC[aiOrder] == (iOrder > 0 ? axesMaxProperN[aiOrder] : axesMaxImproperN[aiOrder]))
      return true;
    if (nC[aiOrder] > 0)
      for (int i = nC[aiOrder]; --i >= 0;) {
        if (isParallel(v, axes[aiOrder][i].normalOrAxis))
          return true;
      }
    return false;
  }

  private int findPlanes(Point3f[] atoms, int[] elements) {
    Operation[] axesC2 = axes[2];
    int nC2 = nC[2];
    
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
        if (nPlanes == axesMaxImproperN[0])
          return nPlanes;
      }
    }
    
    // also look for planes normal to any C2 axis
    
    for (int i = 0; i < nC2; i++)
      nPlanes = getPlane(nPlanes, axesC2[i].normalOrAxis, atoms, elements, center);
    return nPlanes;
  }

  private int getPlane(int nPlanes, Vector3f v3, Point3f[] atoms, int[] elements, 
                       Point3f center2) {
    if (!haveAxis(0, v3)
        && checkOperation(atoms, elements, new Quaternion(v3, 180),
            center, true))
      axes[0][nC[0]++] = new Operation(v3);
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
      order = Math.abs(i);
      normalOrAxis = new Quaternion(v, 180).getNormal();
      Logger.info("new operation -- " + typeNames[type] + " order = " + i + " " + normalOrAxis);
    }

    Operation(Vector3f v) {
      type = OPERATION_PLANE;
      normalOrAxis = new Quaternion(v, 180).getNormal();
      Logger.info("new operation -- " + typeNames[type] + " " + normalOrAxis );
    }

    public String toString() {
      return type + " " + order + " " + normalOrAxis;
    }

    public String getLabel() {
      switch(type) {
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
    int nAxes = 0;
    for (int i = 1; i < maxImproper; i++)
      nAxes += nC[i];
    StringBuffer sb = new StringBuffer("# name=" + name + ", nPlanes=" + nC[0] + ", nAxes=" + nAxes + ";\n");
    sb.append("draw p0" + (haveInversionCenter ? "inv " : " ") + center
        + (haveInversionCenter ? "\"i\";\n" : ";\n"));
    for (int i = 2; i < maxImproper; i++) {
      for (int j = 0; j < nC[i]; j++) {
        op=axes[i][j];
        v.set(op.normalOrAxis);
        v.add(center);
        float scale = 4.0f + op.order / 4.0f;
        sb.append("draw va").append(i).append("_").append(j)
        .append(" width 0.05 scale -" + scale + " ").append(Escape.escape(v));
        v.scaleAdd(-2, op.normalOrAxis, v);
        sb.append(Escape.escape(v))
        .append("\""+op.getLabel()+"\"").append(op.type == OPERATION_IMPROPER_AXIS ? " color red" : "").append(";\n");
      }
    }
    for (int j = 0; j < nC[0]; j++) {
      op=axes[0][j];
      v.set(op.normalOrAxis);
      v.scale(0.01f);
      v.add(center);
      sb.append("draw vp").append(j).append("disk width 6.0 cylinder ")
      .append(Escape.escape(v));
      v.scaleAdd(-0.02f, op.normalOrAxis, v);
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
      sb.append(Escape.escape(v))
      .append("{0 360 0.5} color red;\n");

    
    }
    return sb.toString();
  }
}
