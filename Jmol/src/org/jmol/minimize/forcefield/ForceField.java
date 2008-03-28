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
 *  Lesser General Public License for more details.
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
import org.jmol.minimize.Util;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Viewer;

abstract public class ForceField {

  // same flags as for openBabel:
  
  // terms
  final static int ENERGY = (1 << 0); //!< all terms
  final static int EBOND = (1 << 1); //!< bond term
  final static int EANGLE = (1 << 2); //!< angle term
  final static int ESTRBND = (1 << 3); //!< strbnd term
  final static int ETORSION = (1 << 4); //!< torsion term
  final static int EOOP = (1 << 5); //!< oop term
  final static int EVDW = (1 << 6); //!< vdw term
  final static int EELECTROSTATIC = (1 << 7); //!< electrostatic term

  Calculations calc;
  
  private String getUnits() {
    return calc.getUnit();
  }

  public abstract Vector getAtomTypes();

  protected abstract Hashtable getFFParameters();

  private double criterion, e0; 
  private int stepCurrent, stepMax; 
  private double[][] coordSaved;  

  int atomCount; 
  int bondCount;

  Viewer viewer;
  MinAtom[] atoms;
  MinBond[] bonds;
  int[][] angles;
  int[][] torsions;
  double[] partialCharges;
  
  public ForceField() {}
  
  public void setModel(Viewer viewer, MinAtom[] atoms, MinBond[] bonds, int[][] angles, 
      int[][] torsions, double[] partialCharges) {
  
    this.viewer = viewer;
    this.atoms = atoms;
    this.bonds = bonds;
    this.angles = angles;
    this.torsions = torsions;
    atomCount = atoms.length;
    bondCount = bonds.length;
  }
    
  public boolean setup() {
    if (!calc.haveParams()) {
      Hashtable temp = getFFParameters();
      if (temp == null)
        return false;
      calc.setParams(temp);
    }
    return calc.setupCalculations();
  }


  //////////////////////////////////////////////////////////////////////////////////
  //
  // Energy Minimization
  //
  //////////////////////////////////////////////////////////////////////////////////

  ////////////// calculation /////////////////
  
  void steepestDescent(int steps, double econv) {
    steepestDescentInitialize(steps, econv);
    steepestDescentTakeNSteps(steps);
  }

  public void steepestDescentInitialize(int stepMax, double criterion) {
    this.stepMax = stepMax;//1000
    this.criterion = criterion; //1e-3
    stepCurrent = 0;
    clearGradients();

    if (Logger.debugging)
      calc.dumpAtomList("S T E E P E S T   D E S C E N T");
    
    e0 = energyFull(false, false);

  }

  private void clearGradients() {
    for (int i = 0; i < atomCount; i++)
      atoms[i].gradient[0] = atoms[i].gradient[1] = atoms[i].gradient[2] = 0; 
  }
  
  Vector3d dir = new Vector3d();
  public boolean steepestDescentTakeNSteps(int n) {
    if (stepMax == 0)
      return false;

    for (int iStep = 1; iStep <= n; iStep++) {
      stepCurrent++;
      calc.setSilent(true);
      for (int i = 0; i < atomCount; i++)
        setGradientsUsingNumericalDerivative(atoms[i], ENERGY);
      linearSearch();
      calc.setSilent(false);

      if (Logger.debugging)
        calc.dumpAtomList("S T E P    " + stepCurrent);

      double e1 = energyFull(false, false);

      boolean done = Util.isNear(e1, e0, criterion);
      if (done || stepCurrent % 10 == 0)
        Logger.info(TextFormat.sprintf(" Step %-4i E = %10.6f    dE = %8.6f   criterion %10.8f", null,
            new float[] { (float) e1, (float) (e1 - e0), (float) criterion },
            new int[] { stepCurrent }));
      if (done) {
        Logger.info(TextFormat.formatString(
            "   STEEPEST DESCENT HAS CONVERGED: E = %8.5f " + getUnits() + " after " + stepCurrent + " steps", "f",
            (float) e1));
        return false; //done
      }

      if (stepMax <= stepCurrent) {
        return false; //done
      }

      e0 = e1;
    }
    return true; // continue
  }

  private double getEnergy(int terms, boolean gradients) {
    if ((terms & ENERGY) != 0)
      return energyFull(gradients, true);
    double e = 0.0;
    if ((terms & EBOND) != 0)
      e += energyBond(gradients);
    if ((terms & EANGLE) != 0)
      e += energyAngle(gradients);
    if ((terms & ESTRBND) != 0)
      e += energyStrBnd(gradients);
    if ((terms & ETORSION) != 0)
     e += energyTorsion(gradients);
    if ((terms & EOOP) != 0)
      e += energyOOP(gradients);
    if ((terms & EVDW) != 0)
      e += energyVDW(gradients);
    if ((terms & EELECTROSTATIC) != 0)
      e += energyES(gradients);
    return e;
  }

  //  
  //         f(x + delta) - f(x)
  // f'(x) = ------------------- 
  //                delta
  //
  private void setGradientsUsingNumericalDerivative(MinAtom atom, int terms) {
    double delta = 1.0e-5;
    double e0 = getEnergy(terms, false);
    atom.gradient[0] = getDx(atom, terms, 0, e0, delta);
    atom.gradient[1] = getDx(atom, terms, 1, e0, delta);
    atom.gradient[2] = getDx(atom, terms, 2, e0, delta);
    //System.out.println(" atom + " + atom.atom.getAtomIndex() + " " + atom.gradient[0] + " " + atom.gradient[1] + " " + atom.gradient[2] );
    return;
  }

  private double getDx(MinAtom atom, int terms, int i,
                       double e0, double delta) {
    atom.coord[i] += delta;
    double e = getEnergy(terms, false);
    atom.coord[i] -= delta;
    return (e - e0) / delta;
  }

/*  
  //  
  //          f(x + 2delta) - 2f(x + delta) + f(x)
  // f''(x) = ------------------------------------
  //                        (delta)^2        
  //
  void getNumericalSecondDerivative(MinAtom atom, int terms, Vector3d dir) {
    double delta = 1.0e-5;
    double e0 = getEnergy(terms, false);
    double dx = getDx2(atom, terms, 0, e0, delta);
    double dy = getDx2(atom, terms, 1, e0, delta);
    double dz = getDx2(atom, terms, 2, e0, delta);
    dir.set(dx, dy, dz);
  }

  private double getDx2(MinAtom atom, int terms, int i,
                                     double e0, double delta) {
    // calculate f(1)    
    atom.coord[i] += delta;
    double e1 = getEnergy(terms, false);
    // calculate f(2)
    atom.coord[i] += delta;
    double e2 = getEnergy(terms, false);
    atom.coord[i] -= 2 * delta;
    return (e2 - 2 * e1 + e0) / (delta * delta);
  }

*/  
  public double energyFull(boolean gradients, boolean isSilent) {
    double energy;

    if (!isSilent && Logger.debugging)
      Logger.info("\nE N E R G Y\n");

    if (gradients)
      clearGradients();

    energy = energyBond(gradients)
       + energyAngle(gradients)
       + energyTorsion(gradients)
       + energyOOP(gradients)
       + energyVDW(gradients)
       + energyES(gradients);

    if (!isSilent && Logger.debugging)
      Logger.info(TextFormat.sprintf("\nTOTAL ENERGY = %8.3f " + getUnits() + "\n", 
          null, new float[] { (float) energy }));
    return energy;
  }

  double energyStrBnd(boolean gradients) {
    return 0.0f;
  }

  double energyBond(boolean gradients) {
    return calc.energyBond(gradients); 
  }
  
  double energyAngle(boolean gradients) {
    return calc.energyAngle(gradients); 
  }

  double energyTorsion(boolean gradients) {
    return calc.energyTorsion(gradients); 
  }

  double energyOOP(boolean gradients) {
    return calc.energyOOP(gradients); 
  }

  double energyVDW(boolean gradients) {
    return calc.energyVDW(gradients);
  }

  double energyES(boolean gradients) {
    return calc.energyES(gradients);
  }
  
  // linearSearch 
  //
  // atom: coordinates of atom at iteration k (x_k)
  // direction: search direction ( d = -grad(x_0) )
  //
  // ALGORITHM:
  // 
  // step = 1
  // for (i = 1 to 100) {                max steps = 100
  //   e_k = energy(x_k)                 energy of current iteration
  //   x_k = x_k + step * d              update coordinates
  //   e_k+1 = energy(x_k+1)             energy of next iteration
  //   
  //   if (e_k+1 < e_k)
  //     step = step * 1.2               increase step size
  //   if (e_k+1 > e_k) {
  //     x_k = x_k - step * d            reset coordinates to previous iteration
  //     step = step * 0.5               reduce step size
  //   }
  //   if (e_k+1 == e_k)
  //     end                             convergence criteria reached, stop
  // }

  private void linearSearch() {

    double alpha = 0.0; // Scale factor along direction vector
    double step = 0.2;
    double trustRadius = 0.3; // don't move further than 0.3 Angstroms

    double e1 = energyFull(false, true);

    for (int iStep = 0; iStep < 10; iStep++) {
      saveCoordinates();
      for (int i = 0; i < atomCount; ++i) {
        double[] grad = atoms[i].gradient;
        double[] coord = atoms[i].coord;
        for (int j = 0; j < 3; ++j) {
          if (Util.isFinite(grad[j])) {
            double tempStep = -grad[j] * step;
            if (tempStep > trustRadius)
              coord[j] += trustRadius;
            else if (tempStep < -1.0 * trustRadius)
              coord[j] -= trustRadius;
            else
              coord[j] += tempStep;
          }
        }
      }

      double e2 = energyFull(false, true);

      //System.out.println("linearSearch e2=" + e2 + " e1=" + e1 + " step=" + step);

      if (Util.isNear(e2, e1, 1.0e-3))
        break;
      if (e2 > e1) {
        step *= 0.1;
        restoreCoordinates();
      } else if (e2 < e1) {
        e1 = e2;
        alpha += step;
        step *= 2.15;
        if (step > 1.0)
          step = 1.0;
      }
    }
    
  }

  private void saveCoordinates() {
    if (coordSaved == null)
      coordSaved = new double[atomCount][3];
    for (int i = 0; i < atomCount; i++) 
      for (int j = 0; j < 3; j++)
        coordSaved[i][j] = atoms[i].coord[j];
  }
  
  private void restoreCoordinates() {
    for (int i = 0; i < atomCount; i++) 
      for (int j = 0; j < 3; j++)
        atoms[i].coord[j] = coordSaved[i][j];
  }
  
  public boolean DetectExplosion() {
    for (int i = 0; i < atomCount; i++) {
      MinAtom atom = atoms[i];
      for (int j = 0; j < 3; j++)
        if (!Util.isFinite(atom.coord[j]))
          return true;
    }
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = bonds[i];
      if (Util.distance2(atoms[bond.atomIndexes[0]].coord,
          atoms[bond.atomIndexes[1]].coord) > 900.0)
        return true;
    }
    return false;
  }

  public int getStepCurrent() {
    return stepCurrent;
  }

  public void dumpAtomList(String title) {
    calc.dumpAtomList(title);
  }
  
}
