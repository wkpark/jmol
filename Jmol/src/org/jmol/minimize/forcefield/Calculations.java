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

import java.util.List;

import javax.vecmath.Vector3d;

import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinTorsion;
import org.jmol.minimize.Util;
import org.jmol.util.ArrayUtil;
import org.jmol.util.TextFormat;

abstract class Calculations {

  final public static double RAD_TO_DEG = (180.0 / Math.PI);
  final public static double DEG_TO_RAD = (Math.PI / 180.0);

  final static double KCAL_TO_KJ = 4.1868;

  final static int CALC_DISTANCE = 0; 
  final static int CALC_ANGLE = 1; 
  final static int CALC_STRETCH_BEND = 2;
  final static int CALC_TORSION = 3;  // first 4 are calculated for constraint energies
  final static int CALC_OOP = 4;
  final static int CALC_VDW = 5;
  final static int CALC_ES = 6;
  final static int CALC_MAX = 7;

  ForceField ff;
  List<Object[]>[] calculations = ArrayUtil.createArrayOfArrayList(CALC_MAX);

  int atomCount;
  int bondCount;
  MinAtom[] minAtoms;
  MinBond[] minBonds;
  MinAngle[] minAngles;
  MinTorsion[] minTorsions;
  double[] partialCharges;
  boolean havePartialCharges;
  List<Object[]> constraints;
  boolean isPreliminary;

  public void setConstraints(List<Object[]> constraints) {
    this.constraints = constraints;
  }

  Calculations(ForceField ff, 
      MinAtom[] minAtoms, MinBond[] minBonds,
      MinAngle[] minAngles, MinTorsion[] minTorsions, 
      double[] partialCharges,
      List<Object[]> constraints) {
    this.ff = ff;
    this.minAtoms = minAtoms;
    this.minBonds = minBonds;
    this.minAngles = minAngles;
    this.minTorsions = minTorsions;
    this.constraints = constraints;
    atomCount = minAtoms.length;
    bondCount = minBonds.length;
    if (partialCharges != null && partialCharges.length == atomCount)
      for (int i = atomCount; --i >= 0;)
        if (partialCharges[i] != 0) {
          havePartialCharges = true;
          break;
        }
    if (!havePartialCharges)
      partialCharges = null;
    this.partialCharges = partialCharges;
  }

  abstract boolean setupCalculations();

  abstract String getAtomList(String title);

  abstract String getDebugHeader(int iType);

  abstract String getDebugFooter(int iType, double energy);

  abstract String getUnit();

  abstract double compute(int iType, Object[] dataIn);

  void addForce(Vector3d v, int i, double dE) {
    minAtoms[i].force[0] += v.x * dE;
    minAtoms[i].force[1] += v.y * dE;
    minAtoms[i].force[2] += v.z * dE;
  }

  boolean gradients;

  boolean silent;

  public void setSilent(boolean TF) {
    silent = TF;
  }

  StringBuffer logData = new StringBuffer();

  public String getLogData() {
    return logData.toString();
  }

  void appendLogData(String s) {
    logData.append(s).append("\n");
  }

  boolean logging;
  boolean loggingEnabled;

  void setLoggingEnabled(boolean TF) {
    loggingEnabled = TF;
    if (loggingEnabled)
      logData = new StringBuffer();
  }

  void setPreliminary(boolean TF) {
    isPreliminary = TF;
  }

  abstract class PairCalc extends Calculation {
    
    abstract void setData(List<Object[]> calc, int ia, int ib);

  }
  
  protected void pairSearch(List<Object[]> calc1, PairCalc pc1, 
                            List<Object[]> calc2, PairCalc pc2) {
    /*A:*/ for (int i = 0; i < atomCount - 1; i++) { // one atom...
      MinAtom atomA = minAtoms[i];
      int[] atomList1 = atomA.getBondedAtomIndexes();
      B: for (int j = i + 1; j < atomCount; j++) { // another atom...
        MinAtom atomB = minAtoms[j];
        /*nbrA:*/ for (int k = atomList1.length; --k >= 0;) { // check bonded A-B
          MinAtom nbrA = minAtoms[atomList1[k]];
          if (nbrA == atomB)
            continue B; // pick another B
          if (nbrA.nBonds == 1)
            continue;
          int[] atomList2 = nbrA.getBondedAtomIndexes(); // check A-X-B
          /*nbrAA:*/ for (int l = atomList2.length; --l >= 0;) {
            MinAtom nbrAA = minAtoms[atomList2[l]];
            if (nbrAA == atomB)
              continue B; // pick another B
            
            //this next would exclude A-X-X-B, but Rappe does not do that, he says
            
/*            if (nbrAA.nBonds == 1)
              continue;
            int[] atomList3 = nbrAA.getBondedAtomIndexes(); // check A-X-X-B
            nbrAAA: for (int m = atomList3.length; --m >= 0;) {
              MinAtom nbrAAA = atoms[atomList3[m]];
              if (nbrAAA == atomB)
                continue B; // pick another B           
            }
*/            
            
          }
        }
        pc1.setData(calc1, i, j);
        if (pc2 != null)
          pc2.setData(calc2, i, j);
      }
    }
  }

  private double calc(int iType, boolean gradients) {
    logging = loggingEnabled && !silent;
    this.gradients = gradients;
    List<Object[]> calc = calculations[iType];
    int nCalc;
    double energy = 0;
    if (calc == null || (nCalc = calc.size()) == 0)
      return 0;
    if (logging)
      appendLogData(getDebugHeader(iType));
    for (int ii = 0; ii < nCalc; ii++)
      energy += compute(iType, calculations[iType].get(ii));
    if (logging)
      appendLogData(getDebugFooter(iType, energy));
    if (constraints != null && iType <= CALC_TORSION)
      energy += constraintEnergy(iType);
    return energy;
  }

  double energyStrBnd(@SuppressWarnings("unused") boolean gradients) {
    return 0.0f;
  }

  double energyBond(boolean gradients) {
    return calc(CALC_DISTANCE, gradients);
  }

  double energyAngle(boolean gradients) {
    return calc(CALC_ANGLE, gradients);
  }

  double energyTorsion(boolean gradients) {
    return calc(CALC_TORSION, gradients);
  }

  double energyStretchBend(boolean gradients) {
    return calc(CALC_STRETCH_BEND, gradients);
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

  final Vector3d da = new Vector3d();
  final Vector3d db = new Vector3d();
  final Vector3d dc = new Vector3d();
  final Vector3d dd = new Vector3d();
  int ia, ib, ic, id;

  final Vector3d v1 = new Vector3d();
  final Vector3d v2 = new Vector3d();
  final Vector3d v3 = new Vector3d();

  private final static double PI_OVER_2 = Math.PI / 2;
  private final static double TWO_PI = Math.PI * 2;

  private double constraintEnergy(int iType) {

    double value = 0;
    double k = 0;
    double energy = 0;

    for (int i = constraints.size(); --i >= 0;) {
      Object[] c = constraints.get(i);
      int nAtoms = ((int[]) c[0])[0];
      if (nAtoms != iType + 2)
        continue;
      int[] minList = (int[]) c[1];
      double targetValue = ((Float) c[2]).doubleValue();

      switch (iType) {
      case CALC_TORSION:
        id = minList[3];
        if (gradients)
          dd.set(minAtoms[id].coord);
        //fall through
      case CALC_ANGLE:
        ic = minList[2];
        if (gradients)
          dc.set(minAtoms[ic].coord);
        //fall through
      case CALC_DISTANCE:
        ib = minList[1];
        ia = minList[0];
        if (gradients) {
          db.set(minAtoms[ib].coord);
          da.set(minAtoms[ia].coord);
        }
      }

      k = 10000.0;

      switch (iType) {
      case CALC_TORSION:
        targetValue *= DEG_TO_RAD;
        value = (gradients ? Util.restorativeForceAndTorsionAngleRadians(da,
            db, dc, dd) : Util.getTorsionAngleRadians(minAtoms[ia].coord,
            minAtoms[ib].coord, minAtoms[ic].coord, minAtoms[id].coord, v1, v2, v3));
        if (value < 0 && targetValue >= PI_OVER_2)
          value += TWO_PI;
        else if (value > 0 && targetValue <= -PI_OVER_2)
          targetValue += TWO_PI;
        break;
      case CALC_ANGLE:
        targetValue *= DEG_TO_RAD;
        value = (gradients ? Util.restorativeForceAndAngleRadians(da, db, dc)
            : Util.getAngleRadiansABC(minAtoms[ia].coord, minAtoms[ib].coord,
                minAtoms[ic].coord));
        break;
      case CALC_DISTANCE:
        value = (gradients ? Util.restorativeForceAndDistance(da, db, dc)
            : Math.sqrt(Util.distance2(minAtoms[ia].coord, minAtoms[ib].coord)));
        break;
      }
      energy += constrainQuadratic(value, targetValue, k, iType);
    }
    return energy;
  }

  private double constrainQuadratic(double value, double targetValue, double k,
                                    int iType) {

    if (!Util.isFinite(value))
      return 0;

    double delta = value - targetValue;

    if (gradients) {
      double dE = 2.0 * k * delta;
      switch (iType) {
      case CALC_TORSION:
        addForce(dd, id, dE);
        //fall through
      case CALC_ANGLE:
        addForce(dc, ic, dE);
        //fall through
      case CALC_DISTANCE:
        addForce(db, ib, dE);
        addForce(da, ia, dE);
      }
    }
    return k * delta * delta;
  }

  void getConstraintList() {
    if (constraints == null || constraints.size() == 0)
      return;
    appendLogData("C O N S T R A I N T S\n---------------------");
    for (int i = constraints.size(); --i >= 0;) {
      Object[] c = constraints.get(i);
      int[] indexes = (int[]) c[0];
      int[] minList = (int[]) c[1];
      double targetValue = ((Float) c[2]).doubleValue();
      int iType = indexes[0] - 2;
      switch (iType) {
      case CALC_TORSION:
        id = minList[3];
        //fall through
      case CALC_ANGLE:
        ic = minList[2];
        //fall through
      case CALC_DISTANCE:
        ib = minList[1];
        ia = minList[0];
      }
      switch (iType) {
      case CALC_DISTANCE:
        appendLogData(TextFormat.sprintf("%3d %3d  %-5s %-5s  %12.6f",
            new Object[] {
                minAtoms[ia].atom.getAtomName(),
                minAtoms[ib].atom.getAtomName(),
                new float[] { (float) targetValue },
                new int[] { minAtoms[ia].atom.getAtomNumber(),
                    minAtoms[ib].atom.getAtomNumber(), } }));
        break;
      case CALC_ANGLE:
        appendLogData(TextFormat.sprintf("%3d %3d %3d  %-5s %-5s %-5s  %12.6f",
            new Object[] {
                minAtoms[ia].atom.getAtomName(),
                minAtoms[ib].atom.getAtomName(),
                minAtoms[ic].atom.getAtomName(),
                new float[] { (float) targetValue },
                new int[] { minAtoms[ia].atom.getAtomNumber(),
                    minAtoms[ib].atom.getAtomNumber(),
                    minAtoms[ic].atom.getAtomNumber(), } }));
        break;
      case CALC_TORSION:
        appendLogData(TextFormat
            .sprintf(
                "%3d %3d %3d %3d  %-5s %-5s %-5s %-5s  %3d %8.3f     %8.3f     %8.3f     %8.3f",
                new Object[] {
                    minAtoms[ia].atom.getAtomName(),
                    minAtoms[ib].atom.getAtomName(),
                    minAtoms[ic].atom.getAtomName(),
                    minAtoms[id].atom.getAtomName(),
                    new float[] { (float) targetValue },
                    new int[] { minAtoms[ia].atom.getAtomNumber(),
                        minAtoms[ib].atom.getAtomNumber(),
                        minAtoms[ic].atom.getAtomNumber(),
                        minAtoms[id].atom.getAtomNumber() } }));
        break;
      }
    }
    appendLogData("---------------------\n");
  }

}
