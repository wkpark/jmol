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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinTorsion;
import org.jmol.minimize.Util;
import org.jmol.util.TextFormat;

/*
 * Java implementation by Bob Hanson 5/2012
 * based on chemKit code by Kyle Lutz.
 *    
 * Original work, as listed at http://towhee.sourceforge.net/forcefields/mmff94.html:
 * 
 *    T. A. Halgren; "Merck Molecular Force Field. I. Basis, Form, Scope, 
 *      Parameterization, and Performance of MMFF94", J. Comp. Chem. 5 & 6 490-519 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. II. MMFF94 van der Waals 
 *      and Electrostatic Parameters for Intermolecular Interactions", 
 *      J. Comp. Chem. 5 & 6 520-552 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. III. Molecular Geometries and 
 *      Vibrational Frequencies for MMFF94", J. Comp. Chem. 5 & 6 553-586 (1996).
 *    T. A. Halgren; R. B. Nachbar; "Merck Molecular Force Field. IV. 
 *      Conformational Energies and Geometries for MMFF94", J. Comp. Chem. 5 & 6 587-615 (1996).
 *    T. A. Halgren; "Merck Molecular Force Field. V. Extension of MMFF94 
 *      Using Experimental Data, Additional Computational Data, 
 *      and Empirical Rules", J. Comp. Chem. 5 & 6 616-641 (1996).
 *    T. A. Halgren; "MMFF VII. Characterization of MMFF94, MMFF94s, 
 *      and Other Widely Available Force Fields for Conformational Energies 
 *      and for Intermolecular-Interaction Energies and Geometries", 
 *      J. Comp. Chem. 7 730-748 (1999).
 * 
 */


// just a preliminary copy of UFF at this point

class CalculationsMMFF extends Calculations {

  public static final double fStretch = 143.9325 / 2;

  private Map<Integer, Object> ffParams;

  DistanceCalc bondCalc;
  AngleCalc angleCalc;
  TorsionCalc torsionCalc;
  OOPCalc oopCalc;
  VDWCalc vdwCalc;
  ESCalc esCalc;
  SBCalc sbCalc;
  
  CalculationsMMFF(ForceField ff, Map<Integer, Object> ffParams, 
      MinAtom[] minAtoms, MinBond[] minBonds, 
      MinAngle[] minAngles, MinTorsion[] minTorsions, double[] partialCharges,
      List<Object[]> constraints) {
    super(ff, minAtoms, minBonds, minAngles, minTorsions, partialCharges, constraints);    
    this.ffParams = ffParams;
    bondCalc = new DistanceCalc();
    angleCalc = new AngleCalc();
    sbCalc = new SBCalc();
    torsionCalc = new TorsionCalc();
    oopCalc = new OOPCalc();
    vdwCalc = new VDWCalc();
    esCalc = new ESCalc();
  }
  
  @Override
  String getUnit() {
    return "kcal/mol"; // Note that we SHOULD convert from kcal/mol internally
  }

  @Override
  boolean setupCalculations() {

    List<Object[]> calc;

    DistanceCalc distanceCalc = new DistanceCalc();
    calc = calculations[CALC_DISTANCE] = new ArrayList<Object[]>();
    for (int i = 0; i < bondCount; i++)
      distanceCalc.setData(calc, minBonds[i]);

    calc = calculations[CALC_ANGLE] = new ArrayList<Object[]>();
    AngleCalc angleCalc = new AngleCalc();
    for (int i = minAngles.length; --i >= 0;)
      angleCalc.setData(calc, minAngles[i].data);

    calc = calculations[CALC_STRETCH_BEND] = new ArrayList<Object[]>();
    SBCalc sbCalc = new SBCalc();
    for (int i = minAngles.length; --i >= 0;)
      sbCalc.setData(calc, minAngles[i].data);

    calc = calculations[CALC_TORSION] = new ArrayList<Object[]>();
    TorsionCalc torsionCalc = new TorsionCalc();
    for (int i = minTorsions.length; --i >= 0;)
      torsionCalc.setData(calc, minTorsions[i].data);

    calc = calculations[CALC_OOP] = new ArrayList<Object[]>();
    // set up the special atom arrays
    OOPCalc oopCalc = new OOPCalc();
    int elemNo;
    for (int i = 0; i < atomCount; i++) {
      MinAtom a = minAtoms[i];
      if (a.nBonds == 3 && isInvertible(elemNo = a.atom.getElementNumber()))
        oopCalc.setData(calc, i, elemNo);
    }

    pairSearch(calculations[CALC_VDW] = new ArrayList<Object[]>(), new VDWCalc(),
        calculations[CALC_ES] = new ArrayList<Object[]>(), new ESCalc());

    return true;
  }

  private boolean isInvertible(int n) {
    switch (n) {
    case 6: // C
    case 7: // N
    case 8: // O
    case 15: // P
    case 33: // As
    case 51: // Sb
    case 83: // Bi
      return true;
    default: 
      return false;// no inversion term for this element
    }
  }

  static double calculateR0(double ri, double rj, double chiI, double chiJ,
                            double bondorder) {
    // precompute the equilibrium geometry
    // From equation 3
    double rbo = -0.1332 * (ri + rj) * Math.log(bondorder);
    // From equation 4
    
    double dchi = Math.sqrt(chiI) - Math.sqrt(chiJ);
    double ren = ri * rj * dchi * dchi / (chiI * ri + chiJ * rj);
    // From equation 2
    // NOTE: See http://towhee.sourceforge.net/forcefields/uff.html
    // There is a typo in the published paper
    return (ri + rj + rbo - ren);
  }

  @Override
  double compute(int iType, Object[] dataIn) {

    switch (iType) {
    case CALC_DISTANCE:
      return bondCalc.compute(dataIn);
    case CALC_ANGLE:
      return angleCalc.compute(dataIn);
    case CALC_STRETCH_BEND:
      return sbCalc.compute(dataIn);
    case CALC_TORSION:
      return torsionCalc.compute(dataIn);
    case CALC_OOP:
      return oopCalc.compute(dataIn);
    case CALC_VDW:
      return vdwCalc.compute(dataIn);
    case CALC_ES:
      return esCalc.compute(dataIn);
    }
    return 0.0;
  }

  Object getParameter(Object a) {
    return ffParams.get(a);
  }

  double r0, kb;
  final static double cs = -2.0;
  final static double cs2 = ((7.0/12.0)*(cs * cs));
  double delta2;

  class DistanceCalc extends Calculation {

    void setData(List<Object[]> calc, MinBond bond) {
      ia = bond.data[0];
      ib = bond.data[1];
      
      double[] data = (double[]) getParameter(bond.key);
/*      

      kb = KCAL332 * parA.dVal[PAR_Z] * parB.dVal[PAR_Z] / (r0 * r0 * r0);
*/
      calc.add(new Object[] { new int[] { ia, ib },  data });
    }

    @Override
    double compute(Object[] dataIn) {
      getPointers(dataIn);
      kb = dData[0];
      r0 = dData[1];
      ia = iData[0];
      ib = iData[1];
      
      if (gradients) {
        da.set(minAtoms[ia].coord);
        db.set(minAtoms[ib].coord);
        rab = Util.restorativeForceAndDistance(da, db, dc);
      } else {
        rab = Math.sqrt(Util.distance2(minAtoms[ia].coord, minAtoms[ib].coord));
      }

      // Er = 0.5 k (r - r0)^2
      
      delta = rab - r0;     // we pre-compute the r0 below
      delta2 = delta * delta;
      energy = kb * delta * delta; // 0.5 factor was precalculated
      energy = fStretch * kb * (delta2) * (1 + cs * delta + cs2  * (delta2));

      if (gradients) {
        dE = 2.0 * kb * delta; // TODO
        addForce(da, ia, dE);
        addForce(db, ib, dE);
      }
      
      if (logging)
        appendLogData(getDebugLine(CALC_DISTANCE, this));
      
      return energy;
    }
  }

  
  final static double KCAL644 = 644.12 * KCAL_TO_KJ;
  
  class AngleCalc extends Calculation {
  
    void setData(List<Object[]> calc, int[] angle) {
    }

    @Override
    double compute(Object[] dataIn) {
      return energy;
    }
  }

  class SBCalc extends Calculation {
    
    // TODO 
    void setData(List<Object[]> calc, int[] angle) {
    }

    @Override
    double compute(Object[] dataIn) {
      return energy;
    }
  }

  class TorsionCalc extends Calculation {

    void setData(List<Object[]> calc, int[] t) {
    }
    
    @Override
    double compute(Object[] dataIn) {
      return energy;
    }
  }
  
  final static double KCAL6 = 6.0 * KCAL_TO_KJ;
  final static double KCAL22 = 22.0 * KCAL_TO_KJ;
  final static double KCAL44 = 44.0 * KCAL_TO_KJ;
  
  class OOPCalc extends Calculation {

    void setData(List<Object[]> calc, int ib, int elemNo) {
    }

    @Override
    double compute(Object[] dataIn) {
      return energy;
    }
  }
  
  class VDWCalc extends PairCalc {
    
    @Override
    void setData(List<Object[]> calc, int ia, int ib) {
    }

    @Override
    double compute(Object[] dataIn) {
      return energy;
    } 
  }
  
  final static double KCAL332 = KCAL_TO_KJ * 332.0637;
  
  class ESCalc extends PairCalc {

    @Override
    void setData(List<Object[]> calc, int ia, int ib) {
    }

    @Override
    double compute(Object[] dataIn) {
      return energy;
    }
  }

  
  ///////// REPORTING /////////////
  
  @Override
  String getAtomList(String title) {
    String trailer =
          "----------------------------------------"
          + "-------------------------------------------------------\n";  
    StringBuffer sb = new StringBuffer();
    sb.append("\n" + title + "\n\n"
        + " ATOM    X        Y        Z    TYPE     GRADX    GRADY    GRADZ  "
        + "---------BONDED ATOMS--------\n"
        + trailer);
    for (int i = 0; i < atomCount; i++) {
      MinAtom atom = minAtoms[i];
      int[] others = atom.getBondedAtomIndexes();
      int[] iVal = new int[others.length + 1];
      iVal[0] = atom.atom.getAtomNumber();
      String s = "   ";
      for (int j = 0; j < others.length; j++) {
        s += " %3d";
        iVal[j + 1] = minAtoms[others[j]].atom.getAtomNumber();
      }
      sb.append(TextFormat.sprintf("%3d %8.3f %8.3f %8.3f  %-5s %8.3f %8.3f %8.3f" + s + "\n", 
          new Object[] { atom.sType,
          new float[] { (float) atom.coord[0], (float) atom.coord[1],
            (float) atom.coord[2], (float) atom.force[0], (float) atom.force[1],
            (float) atom.force[2] }, iVal}));
    }
    sb.append(trailer + "\n\n");
    return sb.toString();
  }

  @Override
  String getDebugHeader(int iType) {
    switch (iType){
    case -1:
      return  "Universal Force Field -- " +
          "Rappe, A. K., et. al.; J. Am. Chem. Soc. (1992) 114(25) p. 10024-10035\n";
    case CALC_DISTANCE:
      return
           "\nB O N D   S T R E T C H I N G (" + bondCount + " bonds)\n\n"
          +"  ATOMS  ATOM TYPES   BOND    BOND       IDEAL      FORCE\n"
          +"  I   J   I     J     TYPE   LENGTH     LENGTH    CONSTANT      DELTA     ENERGY\n"
          +"--------------------------------------------------------------------------------";
    case CALC_ANGLE:
      return 
           "\nA N G L E   B E N D I N G (" + minAngles.length + " angles)\n\n"
          +"    ATOMS      ATOM TYPES        VALENCE    IDEAL        FORCE\n"
          +"  I   J   K   I     J     K       ANGLE     ANGLE      CONSTANT     ENERGY\n"
          +"--------------------------------------------------------------------------";
    case CALC_TORSION:
      return 
           "\nT O R S I O N A L (" + minTorsions.length + " torsions)\n\n"
          +"      ATOMS           ATOM TYPES            n    COS          FORCE      TORSION\n"
          +"  I   J   K   L   I     J     K     L          (n phi0)      CONSTANT     ANGLE        ENERGY\n"
          +"---------------------------------------------------------------------------------------------";
    case CALC_OOP:
      return 
           "\nO U T - O F - P L A N E   B E N D I N G\n\n"
          +"      ATOMS           ATOM TYPES             OOP        FORCE \n"
          +"  I   J   K   L   I     J     K     L       ANGLE     CONSTANT      ENERGY\n"
          +"--------------------------------------------------------------------------";
    case CALC_VDW:
      return 
           "\nV A N   D E R   W A A L S\n\n"
          +"  ATOMS  ATOM TYPES\n"
          +"  I   J   I     J      Rij       kij     ENERGY\n"
          +"-----------------------------------------------";
    case CALC_ES:
      return 
          "\nE L E C T R O S T A T I C   I N T E R A C T I O N S\n\n"
          +"  ATOMS  ATOM TYPES            QiQj\n"
          +"  I   J   I     J      Rij    *332.17    ENERGY\n"
          +"-----------------------------------------------";
    }
    return "";
  }

  String getDebugLine(int iType, Calculation c) {
    switch (iType) {
    case CALC_DISTANCE:
      return TextFormat.sprintf(
          "%3d %3d  %-5s %-5s  %4.2f%8.3f   %8.3f     %8.3f   %8.3f   %8.3f",
          new Object[] { minAtoms[c.ia].toString(), minAtoms[c.ib].toString(), 
          new float[] { 0, (float)c.rab, 
              (float)c.dData[1], (float)c.dData[0], 
              (float)c.delta, (float)c.energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber() }});
    case CALC_ANGLE:
      return TextFormat.sprintf(
          "%3d %3d %3d  %-5s %-5s %-5s  %8.3f  %8.3f     %8.3f   %8.3f", 
          new Object[] { minAtoms[c.ia].sType, minAtoms[c.ib].sType, 
              minAtoms[c.ic].sType,
          new float[] { (float)(c.theta * RAD_TO_DEG), (float)c.dData[4] /*THETA0*/, 
              (float)c.dData[0]/*Kijk*/, (float) c.energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber(),
              minAtoms[c.ic].atom.getAtomNumber()} });
      case CALC_TORSION:
      return TextFormat.sprintf(
          "%3d %3d %3d %3d  %-5s %-5s %-5s %-5s  %3d %8.3f     %8.3f     %8.3f     %8.3f", 
          new Object[] { minAtoms[c.ia].sType, minAtoms[c.ib].sType, 
              minAtoms[c.ic].sType, minAtoms[c.id].sType, 
          new float[] { (float) c.dData[1]/*cosNphi0*/, (float) c.dData[0]/*V*/, 
              (float) (c.theta * RAD_TO_DEG), (float) c.energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber(),
              minAtoms[c.ic].atom.getAtomNumber(), minAtoms[c.id].atom.getAtomNumber(), c.iData[4] } });
    case CALC_OOP:
      return TextFormat.sprintf("" +
          "%3d %3d %3d %3d  %-5s %-5s %-5s %-5s  %8.3f   %8.3f     %8.3f",
          new Object[] { minAtoms[c.ia].sType, minAtoms[c.ib].sType, 
              minAtoms[c.ic].sType, minAtoms[c.id].sType,
          new float[] { (float)(c.theta * RAD_TO_DEG), 
              (float)c.dData[0]/*koop*/, (float) c.energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber(),
              minAtoms[c.ic].atom.getAtomNumber(), minAtoms[c.id].atom.getAtomNumber() } });
    case CALC_VDW:
      return TextFormat.sprintf("%3d %3d  %-5s %-5s %6.3f  %8.3f  %8.3f", 
          new Object[] { minAtoms[c.iData[0]].sType, minAtoms[c.iData[1]].sType,
          new float[] { (float)c.rab, (float)c.dData[0]/*kab*/, (float)c.energy},
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber() } });
    case CALC_ES:
      return TextFormat.sprintf("%3d %3d  %-5s %-5s %6.3f  %8.3f  %8.3f", 
          new Object[] { minAtoms[c.iData[0]].sType, minAtoms[c.iData[1]].sType,
          new float[] { (float)c.rab, (float)c.dData[0]/*qq*/, (float)c.energy },
          new int[] { minAtoms[c.ia].atom.getAtomNumber(), minAtoms[c.ib].atom.getAtomNumber() } });
    }
    return "";
  }

  @Override
  String getDebugFooter(int iType, double energy) {
    String s = "";
    switch (iType){
    case CALC_DISTANCE:
      s = "BOND STRETCHING";
      break;
    case CALC_ANGLE:
      s = "ANGLE BENDING";
      break;
    case CALC_TORSION:
      s = "TORSIONAL";
      break;
    case CALC_OOP:
      s = "OUT-OF-PLANE BENDING";
      break;
    case CALC_VDW:
      s = "VAN DER WAALS";
      break;
    case CALC_ES:
      s = "ELECTROSTATIC ENERGY";
      break;
    }
    return TextFormat.sprintf("\n     TOTAL %s ENERGY = %8.3f %s\n", 
        new Object[] { s, getUnit(), Float.valueOf((float) energy) });
  }

}

