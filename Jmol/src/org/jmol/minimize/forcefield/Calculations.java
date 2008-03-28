/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-11-23 12:49:25 -0600 (Fri, 23 Nov 2007) $
 * $Revision: 8655 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.minimize.forcefield;

import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Vector3d;

import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.util.Logger;

abstract class Calculations {

  final public static double RAD_TO_DEG = (180.0 / Math.PI);
  final public static double DEG_TO_RAD = (Math.PI / 180.0);

  final static double KCAL_TO_KJ = 4.1868;

  final static int CALC_BOND = 0;
  final static int CALC_ANGLE = 1;
  final static int CALC_TORSION = 2;
  final static int CALC_OOP = 3;
  final static int CALC_VDW = 4;
  final static int CALC_ES = 5;
  final static int CALC_MAX = 6;

  int atomCount;
  int bondCount;

  MinAtom[] atoms;
  MinBond[] bonds;
  int[][] angles;
  int[][] torsions;
  double[] partialCharges;

  boolean havePartialCharges;

  Vector[] calculations = new Vector[CALC_MAX];
  public Hashtable ffParams;

  Calculations(MinAtom[] atoms, MinBond[] bonds, int[][] angles,
      int[][] torsions, double[] partialCharges) {
    this.atoms = atoms;
    this.bonds = bonds;
    this.angles = angles;
    this.torsions = torsions;
    atomCount = atoms.length;
    bondCount = bonds.length;
    if (partialCharges != null && partialCharges.length == atomCount)
      for (int i = atomCount; --i >= 0;)
        if (partialCharges[i] != 0) {
          havePartialCharges = true;
          break;
        }
  }

  boolean haveParams() {
    return (ffParams != null);
  }

  void setParams(Hashtable temp) {
    ffParams = temp;
  }

  static FFParam getParameter(String a, Hashtable ffParams) {
    return (FFParam) ffParams.get(a);
  }

  abstract boolean setupCalculations();

  abstract void dumpAtomList(String title);

  abstract boolean setupElectrostatics();

  abstract String getDebugHeader(int iType);

  abstract String getDebugFooter(int iType, double energy);

  abstract String getUnit();

  abstract double compute(int iType, boolean debugging, Object[] dataIn);

  void addGradient(Vector3d v, int i, double dE) {
    atoms[i].gradient[0] += v.x * dE;
    atoms[i].gradient[1] += v.y * dE;
    atoms[i].gradient[2] += v.z * dE;
  }

  boolean gradients;

  boolean silent;
  
  public void setSilent(boolean TF) {
    silent = TF;
  }
  
  private double calc(int iType, boolean gradients) {
    this.gradients = gradients;
    Vector calc = calculations[iType];
    int nCalc;
    double energy = 0;
    if (calc == null || (nCalc = calc.size()) == 0)
      return 0;
    boolean debugHigh = !silent && (Logger.isActiveLevel(Logger.LEVEL_DEBUGHIGH));
    if (debugHigh)
      Logger.info(getDebugHeader(iType));

    for (int ii = 0; ii < nCalc; ii++)
      energy += compute(iType, debugHigh, (Object[]) calculations[iType]
          .get(ii));

    if (debugHigh)
      Logger.info(getDebugFooter(iType, energy));

    return energy;
  }

  double energyStrBnd(boolean gradients) {
    return 0.0f;
  }

  double energyBond(boolean gradients) {
    return calc(CALC_BOND, gradients);
  }

  double energyAngle(boolean gradients) {
    return calc(CALC_ANGLE, gradients);
  }

  double energyTorsion(boolean gradients) {
    return calc(CALC_TORSION, gradients);
  }

  double energyOOP(boolean gradients) {
    return calc(CALC_OOP, gradients);
  }

  double energyVDW(boolean gradients) {
    return calc(CALC_VDW, gradients);
  }

  double energyES(boolean gradients) {
    return calc(CALC_ES, gradients);
  }
}
