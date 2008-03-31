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

import java.util.BitSet;
import java.util.Vector;

import org.jmol.api.MinimizerInterface;
import org.jmol.i18n.GT;
import org.jmol.minimize.forcefield.ForceField;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

public class Minimizer implements MinimizerInterface {

  public Viewer viewer;
  public Atom[] atoms;
  public MinAtom[] minAtoms;
  public MinBond[] minBonds;
  public BitSet bsMinFixed;
  private int atomCount;
  private int bondCount;
  private int atomCountFull;
  
  public int[][] angles;
  public int[][] torsions;
  public double[] partialCharges;
  
  private int steps = 50;
  private double crit = 1e-3;

  private static Vector atomTypes;
  private ForceField pFF;
  private String ff = "UFF";
  private BitSet bsTaint, bsSelected, bsAtoms;
  private BitSet bsFixed;
  
  public Minimizer() {
  }

  public void setProperty(String propertyName, Object value) {
    if (propertyName.equals("clear")) {
      clear();
      return;
    }
    if (propertyName.equals("cancel")) {
      stopMinimization(false);
      return;
    }

    if (propertyName.equals("stop")) {
      stopMinimization(true);
      return;
    }
  }

  public Object getProperty(String propertyName, int param) {
    if (propertyName.equals("log")) {
      return (pFF == null ? "" : pFF.getLogData());
    }
    return null;
  }
  
  private void clear() {
    setMinimizationOn(false);
    atomCount = 0;
    bondCount = 0;
    atoms = null;
    viewer = null;
    minAtoms = null;
    minBonds = null;
    angles = null;
    torsions = null;
    partialCharges = null;
    coordSaved = null;
    bsTaint = null;
    bsAtoms = null;
    bsFixed = null;
    bsMinFixed = null;
    bsSelected = null;
    pFF = null;
  }
  
  public boolean minimize(Viewer viewer, int steps, double crit,
                          BitSet bsSelected, BitSet bsFixed) {
    if (minimizationOn)
      return false;
    Object val;
    if (steps == Integer.MAX_VALUE) {
      val = viewer.getParameter("minimizationSteps");
      if (val != null && val instanceof Integer)
        steps = ((Integer) val).intValue();
    }
    this.steps = steps;

    if (crit <= 0) {
      val = viewer.getParameter("minimizationCriterion");
      if (val != null && val instanceof Float)
        crit = ((Float) val).floatValue();
    }
    this.crit = Math.max(crit, 0.0001);

    Logger.info("minimize: initializing (steps = " + steps + " criterion = "
        + crit + ") ...");

    getForceField();
    if (pFF == null) {
      Logger.error(GT._("Could not get class for force field {0}", ff));
      return false;
    }

    if (this.viewer == null) {
      this.viewer = viewer;
      viewer.setMinimizer(this);
      atomCountFull = viewer.getAtomCount();
      atoms = viewer.getModelSet().getAtoms();
      bsAtoms = BitSetUtil.copy(bsSelected);
      atomCount = BitSetUtil.cardinalityOf(bsAtoms);
      if (atomCount == 0) {
        Logger.error(GT._("No atoms selected -- nothing to do!"));
        return false;
      }
    }

    if (!BitSetUtil.compareBits(bsSelected, this.bsSelected) || !BitSetUtil
        .compareBits(bsFixed, this.bsFixed)) {
      if (!setupMinimization(bsFixed)) {
        clear();
        return false;
      }
    } else {
      setAtomPositions();
    }
    this.bsSelected = bsSelected;
    this.bsFixed = bsFixed;

    // minimize and store values

    if (steps > 0 && !viewer.useMinimizationThread())
      minimizeWithoutThread();
    else if (steps > 0)
      setMinimizationOn(true);
    else
      getEnergyOnly();
    return true;
  }

  private boolean setupMinimization(BitSet bsFixed) {
    
    // not implemented
    bsMinFixed = null;
    if (bsFixed != null) {
      bsMinFixed = new BitSet();
      for (int i = 0, pt = 0; i < atomCountFull; i++)
        if (bsAtoms.get(i)) {
          if (bsFixed.get(i))
            bsMinFixed.set(pt);
          pt++;
        }
    }

    // add all atoms

    int[] atomMap = new int[atomCountFull];
    minAtoms = new MinAtom[atomCount];
    int elemnoMax = 0;
    BitSet bsElements = new BitSet();
    for (int i = 0, pt = 0; i < atomCountFull; i++) {
      Atom atom = atoms[i];
      if (bsAtoms.get(i)) {
        atomMap[i] = pt;
        int atomicNo = atoms[i].getElementNumber();
        elemnoMax = Math.max(elemnoMax, atomicNo);
        bsElements.set(atomicNo);
        int flags = 0;
        if (bsFixed != null && bsFixed.get(i))
          flags = MinAtom.ATOM_FIXED;
        minAtoms[pt] = new MinAtom(pt, atom, new double[] { atom.x, atom.y,
            atom.z }, flags, null);
        pt++;
      }
    }

    Logger.info(GT._("{0} atoms will be minimized.", "" + atomCount));
    Logger.info("minimize: creating bonds...");

    // add all bonds
    Vector bondInfo = new Vector();
    bondCount = 0;
    for (int i = 0; i < atomCountFull; i++)
      if (bsAtoms.get(i)) {
        Bond[] bonds = atoms[i].getBonds();
        if (bonds != null)
          for (int j = 0; j < bonds.length; j++) {
            int i2 = bonds[j].getOtherAtom(atoms[i]).getAtomIndex();
            if (i2 > i && bsAtoms.get(i2)) {
              int bondOrder = bonds[j].getOrder();
              switch (bondOrder) {
              case 1:
              case 2:
              case 3:
                break;
              case JmolConstants.BOND_AROMATIC:
                bondOrder = 5;
                break;
              default:
                bondOrder = 1;
              }
              bondCount++;
              bondInfo.addElement(new int[] { atomMap[i], atomMap[i2],
                  bondOrder });
            }
          }
      }
    int[] atomIndexes;

    minBonds = new MinBond[bondCount];
    for (int i = 0; i < bondCount; i++) {
      MinBond bond = minBonds[i] = new MinBond(atomIndexes = (int[]) bondInfo
          .elementAt(i), false, false);
      int atom1 = atomIndexes[0];
      int atom2 = atomIndexes[1];
      minAtoms[atom1].bonds.addElement(bond);
      minAtoms[atom2].bonds.addElement(bond);
      minAtoms[atom1].nBonds++;
      minAtoms[atom2].nBonds++;
    }

    for (int i = 0; i < atomCount; i++)
      atomIndexes = minAtoms[i].getBondedAtomIndexes();

    //set the atom types

    Logger.info("minimize: setting atom types...");

    if (atomTypes == null)
      atomTypes = getAtomTypes();
    if (atomTypes == null)
      return false;
    int nElements = atomTypes.size();
    bsElements.clear(0);
    for (int i = 0; i < nElements; i++) {
      String[] data = ((String[]) atomTypes.get(i));
      String smarts = data[0];
      if (smarts == null)
        continue;
      BitSet search = getSearch(smarts, elemnoMax, bsElements);
      //if the 0 bit in bsElements gets set, then the element is not present,
      //and there is no need to search for it;
      //if search is null, then we are done -- max elemno exceeded
      if (bsElements.get(0))
        bsElements.clear(0);
      else if (search == null)
        break;
      else
        for (int j = 0, pt = 0; j < atomCountFull; j++)
          if (bsAtoms.get(j)) {
            if (search.get(j)) {
              minAtoms[pt].type = data[1];
              //System.out.println("pt" +pt + data[1]);
            }
            pt++;
          }
    }

    // set the model

    Logger.info("minimize: getting angles...");
    getAngles();
    Logger.info("minimize: getting torsions...");
    getTorsions();

    pFF.setModel(this);

    if (!pFF.setup()) {
      Logger.error(GT._("could not setup force field {0}", ff));
      return false;
    }

    if (steps > 0) {
      bsTaint = BitSetUtil.copy(bsAtoms);
      if (bsFixed != null)
        BitSetUtil.andNot(bsTaint, bsFixed);
      viewer.setTaintedAtoms(bsTaint, AtomCollection.TAINT_COORD);
    }

    this.bsFixed = bsFixed;
    return true;

  }
  
  private void setAtomPositions() {
    for (int i = 0; i < atomCount; i++)
      minAtoms[i].set();
  }
  //////////////// atom type support //////////////////
  
  
  final static int TOKEN_ELEMENT_ONLY = 0;
  final static int TOKEN_ELEMENT_CHARGED = 1;
  final static int TOKEN_ELEMENT_CONNECTED = 2;
  final static int TOKEN_ELEMENT_AROMATIC = 3;
  final static int TOKEN_ELEMENT_SP = 4;
  final static int TOKEN_ELEMENT_SP2 = 5;
  
  /*
Token[keyword(0x80064) value="expressionBegin"]
Token[keyword(0x2880034) intValue=2621446(0x280006) value="="]
Token[integer(0x2) intValue=6(0x6) value="6"]
Token[keyword(0x880020) value="and"]
Token[keyword(0x108002a) value="connected"]
Token[keyword(0x880000) value="("]
Token[integer(0x2) intValue=3(0x3) value="3"]
Token[keyword(0x880001) value=")"]

   */
  final static int PT_ELEMENT = 2;
  final static int PT_CHARGE = 5;
  final static int PT_CONNECT = 6;
  
  final static Token[][] tokenTypes = new Token[][] {
         /*0*/  new Token[]{
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0), //2
       Token.tokenExpressionEnd},
         /*1*/  new Token[]{
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0), //2
       Token.tokenAnd, 
       new Token(Token.opEQ, Token.formalCharge),
       Token.intToken(0), //5
       Token.tokenExpressionEnd},
         /*2*/  new Token[]{
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0)  ,  // 2
       Token.tokenAnd, 
       new Token(Token.connected, "connected"),
       Token.tokenLeftParen,
       Token.intToken(0),   // 6
       Token.tokenRightParen,
       Token.tokenExpressionEnd},
         /*3*/  new Token[]{
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0), //2
       Token.tokenAnd, 
       new Token(Token.isaromatic, "isaromatic"),
       Token.tokenExpressionEnd},
         /*4*/  new Token[]{ //sp == connected(1,"triple") or connected(2, "double")
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0)  ,  // 2
       Token.tokenAnd, 
       Token.tokenLeftParen,
       new Token(Token.connected, "connected"),
       Token.tokenLeftParen,
       Token.intToken(1),
       Token.tokenComma,
       new Token(Token.string, "triple"),
       Token.tokenRightParen,
       Token.tokenOr,
       new Token(Token.connected, "connected"),
       Token.tokenLeftParen,
       Token.intToken(2),
       Token.tokenComma,
       new Token(Token.string, "double"),
       Token.tokenRightParen,
       Token.tokenRightParen,
       Token.tokenExpressionEnd},
         /*5*/  new Token[]{  // sp2 == connected(1, double)
       Token.tokenExpressionBegin,
       new Token(Token.opEQ, Token.elemno), 
       Token.intToken(0)  ,  // 2
       Token.tokenAnd, 
       new Token(Token.connected, "connected"),
       Token.tokenLeftParen,
       Token.intToken(1),
       Token.tokenComma,
       new Token(Token.string, "double"),
       Token.tokenRightParen,
       Token.tokenExpressionEnd},
  };
  
  /*
  Token[keyword(0x108002a) value="connected"]
        Token[keyword(0x880000) value="("]
        Token[integer(0x2) intValue=1(0x1) value="1"]
        Token[keyword(0x880008) value=","]
        Token[string(0x4) value="triple"]
        Token[keyword(0x880001) value=")"]
        Token[keyword(0x880018) value="or"]
        Token[keyword(0x108002a) value="connected"]
        Token[keyword(0x880000) value="("]
        Token[integer(0x2) intValue=2(0x2) value="2"]
        Token[keyword(0x880008) value=","]
        Token[string(0x4) value="double"]
        Token[keyword(0x880001) value=")"]
        Token[keyword(0x80065) value="expressionEnd"]
  */
  private BitSet getSearch(String smarts, int elemnoMax, BitSet bsElements) {
    /*
     * 
     * only a few possibilities --
     *
     * [#n] an element --> elemno=n
     * [XDn] element X with n connections
     * [X^n] element X with n+1 connections
     * [X+n] element X with formal charge +n
     * 
     */

    Token[] search = null;

    int len = smarts.length();
    search = tokenTypes[TOKEN_ELEMENT_ONLY];
    int n = smarts.charAt(len - 2) - '0';
    int elemNo = 0;
    if (n >= 10)
      n = 0;
    if (smarts.charAt(1) == '#') {
      elemNo = Parser.parseInt(smarts.substring(2, len - 1));
    } else {
      String s = smarts.substring(1, (n > 0 ? len - 3 : len - 1));
      if (s.equals(s.toLowerCase())) {
        s = s.toUpperCase();
        search = tokenTypes[TOKEN_ELEMENT_AROMATIC];
      }
      elemNo = JmolConstants.elementNumberFromSymbol(s);
    }
    if (elemNo > elemnoMax)
      return null;
    if (!bsElements.get(elemNo)) {
      bsElements.set(0);
      return null;
    }
    switch (smarts.charAt(len - 3)) {
    case 'D':
      search = tokenTypes[TOKEN_ELEMENT_CONNECTED];
      search[PT_CONNECT].intValue = n;
      break;
    case '^':
      search = tokenTypes[TOKEN_ELEMENT_SP + (n - 1)];
      break;
    case '+':
      search = tokenTypes[TOKEN_ELEMENT_CHARGED];
      search[PT_CHARGE].intValue = n;
      break;
    }
    search[PT_ELEMENT].intValue = elemNo;
    Object v = viewer.evaluateExpression(search);
    //System.out.println(smarts + " minimize atoms=" + v.toString());
    return (v instanceof BitSet ? (BitSet) v : null);
  }
  
  public void getAngles() {

    Vector vAngles = new Vector();

    for (int ib = 0; ib < atomCount; ib++) {
      MinAtom atomB = minAtoms[ib];
      int n = atomB.nBonds;
      if (n < 2)
        continue;
      //for all central atoms....
      int[] atomList = atomB.getBondedAtomIndexes();
      for (int ia = 0; ia < n - 1; ia++)
        for (int ic = ia + 1; ic < n; ic++) {
          // note! center in center; ia < ic  (not like OpenBabel)
          vAngles.addElement(new int[] { atomList[ia], ib, atomList[ic] });
/*          System.out.println (" " 
              + minAtoms[atomList[ia]].getIdentity() + " -- " 
              + minAtoms[ib].getIdentity() + " -- " 
              + minAtoms[atomList[ic]].getIdentity());
*/        }
    }
    
    angles = new int[vAngles.size()][];
    for (int i = vAngles.size(); --i >= 0; )
      angles[i] = (int[]) vAngles.elementAt(i);
    Logger.info(angles.length + " angles");
  }

  public void getTorsions() {

    Vector vTorsions = new Vector();

    // extend all angles a-b-c by one, but only
    // when when c > b -- other possibility will be found
    // starting with angle d-c-b
    for (int i = angles.length; --i >= 0;) {
      int[] angle = angles[i];
      int ia = angle[0];
      int ib = angle[1];
      int ic = angle[2];
      if (ic > ib && minAtoms[ic].nBonds != 1) {
        int[] atomList = minAtoms[ic].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          int id = atomList[j];
          if (id != ia && id != ib) {
            vTorsions.addElement(new int[] { ia, ib, ic, id });
/*            System.out.println(" " + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[id].getIdentity() + " ");
*/
          }
        }
      }
      if (ia > ib && minAtoms[ia].nBonds != 1) {
        int[] atomList = minAtoms[ia].getBondedAtomIndexes();
        for (int j = 0; j < atomList.length; j++) {
          int id = atomList[j];
          if (id != ic && id != ib) {
            vTorsions.addElement(new int[] { ic, ib, ia, id });
/*            System.out.println(" " + minAtoms[ic].getIdentity() + " -- "
                + minAtoms[ib].getIdentity() + " -- "
                + minAtoms[ia].getIdentity() + " -- "
                + minAtoms[id].getIdentity() + " ");
*/          }
        }
      }
      
    }

    torsions = new int[vTorsions.size()][];
    for (int i = vTorsions.size(); --i >= 0;)
      torsions[i] = (int[]) vTorsions.elementAt(i);

    Logger.info(torsions.length + " torsions");

  }

  
  
  
  ///////////////////////////// minimize //////////////////////
  
 
  public ForceField getForceField() {
    if (pFF == null) {
      try {
        String className = getClass().getName();
        className = className.substring(0, className.lastIndexOf(".")) 
        + ".forcefield.ForceField" + ff;
        Logger.info( "minimize: using " + className);
        pFF = (ForceField) Class.forName(className).newInstance();
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }
    System.out.println("minimize: forcefield = " + pFF);
    return pFF;
  }
  
  public Vector getAtomTypes() {
    getForceField();
    return (pFF == null ? null : pFF.getAtomTypes());
  }
  
  /* ***************************************************************
   * Minimization thead support
   ****************************************************************/

  boolean minimizationOn;

  private MinimizationThread minimizationThread;

  private void setMinimizationOn(boolean minimizationOn) {
    if (!minimizationOn) {
      if (minimizationThread != null) {
        minimizationThread.interrupt();
        minimizationThread = null;
      }
      this.minimizationOn = false;
      return;
    }
    if (minimizationThread == null) {
      minimizationThread = new MinimizationThread();
      minimizationThread.start();
    }
    this.minimizationOn = true;
  }

  private void getEnergyOnly() {
    if (pFF == null || viewer == null)
      return;
    pFF.steepestDescentInitialize(steps, crit);      
    viewer.setFloatProperty("_minimizationEnergyDiff", 0);
    viewer.setFloatProperty("_minimizationEnergy", (float) pFF.getEnergy());
    viewer.setStringProperty("_minimizationStatus", "calculate");
    viewer.notifyMinimizationStatus();
  }
  
  public void startMinimization() {
    if (pFF == null || viewer == null)
      return;
    viewer.setIntProperty("_minimizationStep", 0);
    viewer.setStringProperty("_minimizationStatus", "starting");
    viewer.notifyMinimizationStatus();
    viewer.saveCoordinates(null, bsTaint);
    pFF.steepestDescentInitialize(steps, crit);
    viewer.setFloatProperty("_minimizationEnergy", (float) pFF.getEnergy());
    viewer.setFloatProperty("_minimizationEnergyDiff", (float) pFF.getEnergyDiff());
    saveCoordinates();
  }

  boolean stepMinimization() {
    boolean doRefresh = viewer.getBooleanProperty("minimizationRefresh");
    viewer.setStringProperty("_minimizationStatus", "running");
    boolean going = pFF.steepestDescentTakeNSteps(1);
    int currentStep = pFF.getCurrentStep();
    viewer.setIntProperty("_minimizationStep", currentStep);
    viewer.setFloatProperty("_minimizationEnergy", (float) pFF.getEnergy());
    viewer.notifyMinimizationStatus();
    if (doRefresh) {
      updateAtomXYZ();
      viewer.refresh(0, "minimization step " + currentStep);
    }
    return going;
  }

  void endMinimization() {
    updateAtomXYZ();
    setMinimizationOn(false);
    boolean failed = pFF.DetectExplosion();
    if (failed)
      restoreCoordinates();
    viewer.setIntProperty("_minimizationStep", pFF.getCurrentStep());
    viewer.setFloatProperty("_minimizationEnergy", (float) pFF.getEnergy());
    viewer.setStringProperty("_minimizationStatus", (failed ? "failed" : "done"));
    viewer.notifyMinimizationStatus();
    viewer.refresh(0, "Minimizer:done" + (failed ? " EXPLODED" : "OK"));
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

  private void stopMinimization(boolean coordAreOK) {
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
    viewer.refreshMeasures();
  }

  private void minimizeWithoutThread() {
    //for batch operation
    startMinimization();
    do {
      if (!stepMinimization())
        endMinimization();
    } while (true);
  }
  
  class MinimizationThread extends Thread implements Runnable {
    public void run() {
      long startTime = System.currentTimeMillis();
      long lastRepaintTime = startTime;
      
      //should save the atom coordinates
      startMinimization();      
      
      try {
        do {
          long currentTime = System.currentTimeMillis();
          int elapsed = (int) (currentTime - lastRepaintTime);
          int sleepTime = 33 - elapsed;
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
          lastRepaintTime = currentTime = System.currentTimeMillis();
          if (!stepMinimization())
            endMinimization();            
          elapsed = (int) (currentTime - startTime);
        } while (!isInterrupted());
      } catch (Exception e) {
        if (minimizationOn)
          System.out.println(" minimization thread interrupted");
      }
    }
  }
}
