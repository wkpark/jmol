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

package org.jmol.minimize;

import javajs.util.AU;
import javajs.util.List;
import java.util.Hashtable;

import java.util.Map;

import org.jmol.api.MinimizerInterface;
import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.minimize.forcefield.ForceField;
import org.jmol.minimize.forcefield.ForceFieldMMFF;
import org.jmol.minimize.forcefield.ForceFieldUFF;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.Logger;

import org.jmol.script.T;
import org.jmol.viewer.Viewer;

public class Minimizer implements MinimizerInterface {

  public Viewer viewer;
  public Atom[] atoms;
  public Bond[] bonds;
  public int rawBondCount;
  
  public MinAtom[] minAtoms;
  public MinBond[] minBonds;
  public MinAngle[] minAngles;
  public MinTorsion[] minTorsions;
  public MinPosition[] minPositions;
  
  public BS bsMinFixed;
  private int atomCount;
  private int bondCount;
  private int[] atomMap; 
 
  public double[] partialCharges;
  
  private int steps = 50;
  private double crit = 1e-3;

  public String units = "kJ/mol";
  
  private ForceField pFF;
  private String ff = "UFF";
  private BS bsTaint, bsSelected;
  public BS bsAtoms;
  private BS bsFixedDefault;
  private BS bsFixed;
  
  public List<Object[]> constraints;
  
  private boolean isSilent;
  
  public Minimizer() {
  }

  public void setProperty(String propertyName, Object value) {
    if (propertyName.equals("ff")) {
      // UFF or MMFF
      if (!ff.equals(value)) {
        setProperty("clear", null);
        ff = (String) value;
      }
      return;
    }
    if (propertyName.equals("cancel")) {
      stopMinimization(false);
      return;
    }
    if (propertyName.equals("clear")) {
      if (minAtoms != null) {
        stopMinimization(false);
        clear();
      }
      return;
    }
    if (propertyName.equals("constraint")) {
      addConstraint((Object[]) value);
      return;
    }
    if (propertyName.equals("fixed")) {
      bsFixedDefault = (BS) value;
      return;
    }
    if (propertyName.equals("stop")) {
      stopMinimization(true);
      return;
    }
    if (propertyName.equals("viewer")) {
      viewer = (Viewer) value;
      return;
    }
  }

  public Object getProperty(String propertyName, int param) {
    if (propertyName.equals("log")) {
      return (pFF == null ? "" : pFF.getLogData());
    }
    return null;
  }
  
  private Map<String, Object[]> constraintMap;
  private int elemnoMax;
  private void addConstraint(Object[] c) {
    if (c == null)
      return;
    int[] atoms = (int[]) c[0];
    int nAtoms = atoms[0];
    if (nAtoms == 0) {
      constraints = null;
      return;
    }
    if (constraints == null) {
      constraints = new  List<Object[]>();
      constraintMap = new Hashtable<String, Object[]>();
    }
    if (atoms[1] > atoms[nAtoms]) {
        AU.swapInt(atoms, 1, nAtoms);
        if (nAtoms == 4)
          AU.swapInt(atoms, 2, 3);
    }
    String id = Escape.eAI(atoms);
    Object[] c1 = constraintMap.get(id);
    if (c1 != null) {
      c1[2] = c[2]; // just set target value
      return;
    }
    constraintMap.put(id, c);
    constraints.addLast(c);
  }
    
  private void clear() {
    setMinimizationOn(false);
    atomCount = 0;
    bondCount = 0;
    atoms = null;
    bonds = null;
    rawBondCount = 0;
    minAtoms = null;
    minBonds = null;
    minAngles = null;
    minTorsions = null;
    partialCharges = null;
    coordSaved = null;
    atomMap = null;
    bsTaint = null;
    bsAtoms = null;
    bsFixed = null;
    bsFixedDefault = null;
    bsMinFixed = null;
    bsSelected = null;
    constraints = null;
    constraintMap = null;
    pFF = null;
  }
  
  public boolean minimize(int steps, double crit, BS bsSelected,
                          BS bsFixed, boolean haveFixed, boolean forceSilent, 
                          String ff) {
    isSilent = (forceSilent || viewer.getBooleanProperty("minimizationSilent"));
    Object val;
    setEnergyUnits();
    if (steps == Integer.MAX_VALUE) {
      val = viewer.getParameter("minimizationSteps");
      if (val != null && val instanceof Integer)
        steps = ((Integer) val).intValue();
    }
    this.steps = steps;

    // if the user indicated minimize ... FIX ... or we don't have any defualt,
    // use the bsFixed coming in here, which is set to "nearby and in frame" in that case. 
    // and if something is fixed, then AND it with "nearby and in frame" as well.
    if (!haveFixed && bsFixedDefault != null)
      bsFixed.and(bsFixedDefault);
    if (crit <= 0) {
      val = viewer.getParameter("minimizationCriterion");
      if (val != null && val instanceof Float)
        crit = ((Float) val).floatValue();
    }
    this.crit = Math.max(crit, 0.0001);

    if (minimizationOn)
      return false;
    ForceField pFF0 = pFF;
    getForceField(ff);
    if (pFF == null) {
      Logger.error(GT._("Could not get class for force field {0}", ff));
      return false;
    }
    Logger.info("minimize: initializing " + pFF.name + " (steps = " + steps + " criterion = "
        + crit + ") ...");
    if (bsSelected.cardinality() == 0) {
      Logger.error(GT._("No atoms selected -- nothing to do!"));
      return false;
    }
    atoms = viewer.getModelSet().atoms;
    bsAtoms = BSUtil.copy(bsSelected);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      if (atoms[i].getElementNumber() == 0)
        bsAtoms.clear(i);
    if (bsFixed != null)
      bsAtoms.or(bsFixed);
    atomCount = bsAtoms.cardinality();

    boolean sameAtoms = BSUtil.areEqual(bsSelected, this.bsSelected);
    this.bsSelected = bsSelected;
    if (pFF0 != null && pFF != pFF0)
      sameAtoms = false;
    if (!sameAtoms)
      pFF.clear();
    if ((!sameAtoms || !BSUtil.areEqual(bsFixed, this.bsFixed))
        && !setupMinimization()) {
      clear();
      return false;
    }
    if (steps > 0) {
      bsTaint = BSUtil.copy(bsAtoms);
      BSUtil.andNot(bsTaint, bsFixed);
      viewer.setTaintedAtoms(bsTaint, AtomCollection.TAINT_COORD);
    }
    if (bsFixed != null)
      this.bsFixed = bsFixed;
    setAtomPositions();

    if (constraints != null) {
      for (int i = constraints.size(); --i >= 0;) {
        Object[] constraint = constraints.get(i);
        int[] aList = (int[]) constraint[0];
        int[] minList = (int[]) constraint[1];
        int nAtoms = aList[0] = Math.abs(aList[0]);
        for (int j = 1; j <= nAtoms; j++) {
          if (steps <= 0 || !bsAtoms.get(aList[j])) {
            aList[0] = -nAtoms; // disable
            break;
          }
          minList[j - 1] = atomMap[aList[j]];
        }
      }
    }

    pFF.setConstraints(this);

    // minimize and store values

    if (steps <= 0)
      getEnergyOnly();
    else if (isSilent || !viewer.useMinimizationThread())
      minimizeWithoutThread();
    else
      setMinimizationOn(true);
    return true;
  }

  private void setEnergyUnits() {
    String s = viewer.getEnergyUnits();
    units = (s.equalsIgnoreCase("kcal") ? "kcal" : "kJ");
  }

  private boolean setupMinimization() {

    coordSaved = null;
    atomMap = new int[atoms.length];
    minAtoms = new MinAtom[atomCount];
    elemnoMax = 0;
    BS bsElements = new BS();
    for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms
        .nextSetBit(i + 1), pt++) {
      Atom atom = atoms[i];
      atomMap[i] = pt;
      int atomicNo = atoms[i].getElementNumber();
      elemnoMax = Math.max(elemnoMax, atomicNo);
      bsElements.set(atomicNo);
      minAtoms[pt] = new MinAtom(pt, atom, new double[] { atom.x, atom.y,
          atom.z }, atomCount);
      minAtoms[pt].sType = atom.getAtomName();
    }

    Logger.info(GT._("{0} atoms will be minimized.", "" + atomCount));
    Logger.info("minimize: getting bonds...");
    bonds = viewer.modelSet.bonds;
    rawBondCount = viewer.modelSet.bondCount;
    getBonds();
    Logger.info("minimize: getting angles...");
    getAngles();
    Logger.info("minimize: getting torsions...");
    getTorsions();
    return setModel(bsElements);
  }
  
  private boolean setModel(BS bsElements) {
    if (!pFF.setModel(bsElements, elemnoMax)) {
      //pFF.log("could not setup force field " + ff);
      Logger.error(GT._("could not setup force field {0}", ff));
      if (ff.equals("MMFF")) {
        getForceField("UFF");
        //pFF.log("could not setup force field " + ff);
        return setModel(bsElements);        
      }
      return false;
    }
    return true;
  }

  private void setAtomPositions() {
    for (int i = 0; i < atomCount; i++)
      minAtoms[i].set();
    bsMinFixed = null;
    if (bsFixed != null) {
      bsMinFixed = new BS();
      for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms
          .nextSetBit(i + 1), pt++)
        if (bsFixed.get(i))
          bsMinFixed.set(pt);
    }
  }

  private void getBonds() {
    List<MinBond> bondInfo = new  List<MinBond>();
    bondCount = 0;
    int i1, i2;
    for (int i = 0; i < rawBondCount; i++) {
      Bond bond = bonds[i];
      if (!bsAtoms.get(i1 = bond.getAtomIndex1())
          || !bsAtoms.get(i2 = bond.getAtomIndex2()))
        continue;
      if (i2 < i1) {
        int ii = i1;
        i1 = i2;
        i2 = ii;
      }
      int bondOrder = bond.getCovalentOrder();
      switch (bondOrder) {
      case 1:
      case 2:
      case 3:
        break;
      case JmolEdge.BOND_AROMATIC:
        bondOrder = 5;
        break;
      default:
        bondOrder = 1;
      }
      bondInfo.addLast(new MinBond(i, bondCount++, atomMap[i1], atomMap[i2], bondOrder, 0, null));
    }
    minBonds = new MinBond[bondCount];
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i] = bondInfo.get(i);
      int atom1 = bond.data[0];
      int atom2 = bond.data[1];
      minAtoms[atom1].addBond(bond, atom2);
      minAtoms[atom2].addBond(bond, atom1);
    }
    for (int i = 0; i < atomCount; i++)
      minAtoms[i].getBondedAtomIndexes();
  }

  public void getAngles() {
    List<MinAngle> vAngles = new  List<MinAngle>();
    int[] atomList;
    int ic;
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i];
      int ia = bond.data[0];
      int ib = bond.data[1];
      if (minAtoms[ib].nBonds > 1) {
        atomList = minAtoms[ib].getBondedAtomIndexes();
        for (int j = atomList.length; --j >= 0;)
          if ((ic = atomList[j]) > ia) {
            vAngles.addLast(new MinAngle(new int[] { ia, ib, ic, i,
                minAtoms[ib].getBondIndex(j)}));
            minAtoms[ia].bsVdw.clear(ic);
/*            System.out.println (" " 
                + minAtoms[ia].getIdentity() + " -- " 
                + minAtoms[ib].getIdentity() + " -- " 
                + minAtoms[ic].getIdentity());
*/
          }
      }
      if (minAtoms[ia].nBonds > 1) {
        atomList = minAtoms[ia].getBondedAtomIndexes();
        for (int j = atomList.length; --j >= 0;)
          if ((ic = atomList[j]) < ib && ic > ia) {
            vAngles
                .addLast(new MinAngle(new int[] { ic, ia, ib, minAtoms[ia].getBondIndex(j),
                    i}));
            minAtoms[ic].bsVdw.clear(ib);
/*
            System.out.println ("a " 
                + minAtoms[ic].getIdentity() + " -- " 
                + minAtoms[ia].getIdentity() + " -- " 
                + minAtoms[ib].getIdentity());
*/            
          }
      }
    }
    minAngles = vAngles.toArray(new MinAngle[vAngles.size()]);
    Logger.info(minAngles.length + " angles");
  }

  public void getTorsions() {
    List<MinTorsion> vTorsions = new  List<MinTorsion>();
    int id;
    // extend all angles a-b-c by one, but only
    // when when c > b or a > b
    for (int i = minAngles.length; --i >= 0;) {
      int[] angle = minAngles[i].data;
      int ia = angle[0];
      int ib = angle[1];
      int ic = angle[2];
      int[] atomList;
      if (ic > ib && minAtoms[ic].nBonds > 1) {
        atomList = minAtoms[ic].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          id = atomList[j];
          if (id != ia && id != ib) {
            vTorsions.addLast(new MinTorsion(new int[] { ia, ib, ic, id, 
                angle[ForceField.ABI_IJ], angle[ForceField.ABI_JK],
                minAtoms[ic].getBondIndex(j) }));
              minAtoms[Math.min(ia, id)].bs14.set(Math.max(ia, id));
/*            System.out.println("t " + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[id].getIdentity());
*/
          }
        }
      }
      if (ia > ib && minAtoms[ia].nBonds != 1) {
        atomList = minAtoms[ia].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          id = atomList[j];
          if (id != ic && id != ib) {
            vTorsions.addLast(new MinTorsion(new int[] { ic, ib, ia, id, 
                angle[ForceField.ABI_JK], angle[ForceField.ABI_IJ],
                minAtoms[ia].getBondIndex(j) }));
            minAtoms[Math.min(ic, id)].bs14.set(Math.max(ic, id));
/*            System.out.println("t " + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[id].getIdentity());
*/
          }
        }
      }
    }
    minTorsions = vTorsions.toArray(new MinTorsion[vTorsions.size()]);
    Logger.info(minTorsions.length + " torsions");
  }
  
  ///////////////////////////// minimize //////////////////////

  public ForceField getForceField(String ff) {
    if (ff.startsWith("MMFF"))
      ff = "MMFF";
    if (pFF == null || !ff.equals(this.ff)) {
      if (ff.equals("UFF")) {
        pFF = new ForceFieldUFF(this);
      } else if (ff.equals("MMFF")) {
        pFF = new ForceFieldMMFF(this);
      } else {
        // default to UFF
        pFF = new ForceFieldUFF(this);
        ff = "UFF";
      }
      this.ff = ff;
      viewer.setStringProperty("_minimizationForceField", ff);
    }
    //Logger.info("minimize: forcefield = " + pFF);
    return pFF;
  }
  
  /* ***************************************************************
   * Minimization thead support
   ****************************************************************/

  private boolean minimizationOn;
  public boolean minimizationOn() {
    return minimizationOn;
  }

  private MinimizationThread minimizationThread;

  private void setMinimizationOn(boolean minimizationOn) {
    //TODO -- shouldn't we allow run() here?
    //System.out.println("Minimizer setMinimizationOn "+ minimizationOn + " " + minimizationThread + " " + this.minimizationOn);
    this.minimizationOn = minimizationOn;
    if (!minimizationOn) {
      if (minimizationThread != null) {
        //minimizationThread.interrupt(); // did not seem to work with applet
        minimizationThread = null;
      }
      return;
    }
    if (minimizationThread == null) {
      minimizationThread = new MinimizationThread();
      minimizationThread.setManager(this, viewer, null);
      minimizationThread.start();
    }
  }

  private void getEnergyOnly() {
    if (pFF == null || viewer == null)
      return;
    pFF.steepestDescentInitialize(steps, crit);      
    viewer.setFloatProperty("_minimizationEnergyDiff", 0);
    reportEnergy();
    viewer.setStringProperty("_minimizationStatus", "calculate");
    viewer.notifyMinimizationStatus();
  }
  
  private void reportEnergy() {
    viewer.setFloatProperty("_minimizationEnergy", pFF.toUserUnits(pFF.getEnergy()));
  }

  public boolean startMinimization() {
   try {
      Logger.info("minimizer: startMinimization");
      viewer.setIntProperty("_minimizationStep", 0);
      viewer.setStringProperty("_minimizationStatus", "starting");
      viewer.setFloatProperty("_minimizationEnergy", 0);
      viewer.setFloatProperty("_minimizationEnergyDiff", 0);
      viewer.notifyMinimizationStatus();
      viewer.saveCoordinates("minimize", bsTaint);
      pFF.steepestDescentInitialize(steps, crit);
      reportEnergy();
      saveCoordinates();
    } catch (Exception e) {
      Logger.error("minimization error viewer=" + viewer + " pFF = " + pFF);
      return false;
    }
    minimizationOn = true;
    return true;
  }

  public boolean stepMinimization() {
    if (!minimizationOn)
      return false;
    boolean doRefresh = (!isSilent && viewer.getBooleanProperty("minimizationRefresh"));
    viewer.setStringProperty("_minimizationStatus", "running");
    boolean going = pFF.steepestDescentTakeNSteps(1);
    int currentStep = pFF.getCurrentStep();
    viewer.setIntProperty("_minimizationStep", currentStep);
    reportEnergy();
    viewer.setFloatProperty("_minimizationEnergyDiff", pFF.toUserUnits(pFF.getEnergyDiff()));
    viewer.notifyMinimizationStatus();
    if (doRefresh) {
      updateAtomXYZ();
      viewer.refresh(3, "minimization step " + currentStep);
    }
    return going;
  }

  public void endMinimization() {
    updateAtomXYZ();
    setMinimizationOn(false);
    boolean failed = pFF.detectExplosion();
    if (failed)
      restoreCoordinates();
    viewer.setIntProperty("_minimizationStep", pFF.getCurrentStep());
    reportEnergy();
    viewer.setStringProperty("_minimizationStatus", (failed ? "failed" : "done"));
    viewer.notifyMinimizationStatus();
    viewer.refresh(3, "Minimizer:done" + (failed ? " EXPLODED" : "OK"));
    Logger.info("minimizer: endMinimization");
}

  double[][] coordSaved;
  
  private void saveCoordinates() {
    if (coordSaved == null)
      coordSaved = new double[atomCount][3];
    for (int i = 0; i < atomCount; i++) 
      for (int j = 0; j < 3; j++)
        coordSaved[i][j] = minAtoms[i].coord[j];
  }
  
  private void restoreCoordinates() {
    if (coordSaved == null)
      return;
    for (int i = 0; i < atomCount; i++) 
      for (int j = 0; j < 3; j++)
        minAtoms[i].coord[j] = coordSaved[i][j];
    updateAtomXYZ();
  }

  public void stopMinimization(boolean coordAreOK) {
    if (!minimizationOn)
      return;
    setMinimizationOn(false);
    if (coordAreOK)
      endMinimization();
    else
      restoreCoordinates();
  }
  
  void updateAtomXYZ() {
    if (steps <= 0)
      return;
    for (int i = 0; i < atomCount; i++) {
      MinAtom minAtom = minAtoms[i];
      Atom atom = minAtom.atom;
      atom.x = (float) minAtom.coord[0];
      atom.y = (float) minAtom.coord[1];
      atom.z = (float) minAtom.coord[2];
    }
    viewer.refreshMeasures(false);
  }

  private void minimizeWithoutThread() {
    //for batch operation
    if (!startMinimization())
      return;
    while (stepMinimization()) {
    }
    endMinimization();
  }
  
  public void report(String msg, boolean isEcho) {
    if (isSilent)
      Logger.info(msg);
    else if (isEcho)
      viewer.showString(msg, false);
    else
      viewer.scriptEcho(msg);    
  }

  public void calculatePartialCharges(Bond[] bonds, int bondCount,
                                      Atom[] atoms, BS bsAtoms) {
    //TODO -- combine SMILES and MINIMIZER in same JAR file
    ForceFieldMMFF ff = new ForceFieldMMFF(this);
    ff.setArrays(atoms, bsAtoms, bonds, bondCount, true, true);
    viewer.setAtomProperty(bsAtoms, T.atomtype, 0, 0, null, null,
        ff.getAtomTypeDescriptions());
    viewer.setAtomProperty(bsAtoms, T.partialcharge, 0, 0, null,
        ff.getPartialCharges(), null);
  }

}
