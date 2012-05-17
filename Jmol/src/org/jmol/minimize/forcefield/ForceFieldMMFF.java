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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.minimize.MinAngle;
import org.jmol.minimize.MinAtom;
import org.jmol.minimize.MinBond;
import org.jmol.minimize.MinObject;
import org.jmol.minimize.MinTorsion;
import org.jmol.minimize.Minimizer;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.JmolEdge;
import org.jmol.util.Logger;

/**
 * MMFF94 implementation 5/14/2012
 * 
 * - fully validated for atom types and charges
 * - reasonably well validated for energies (see below)
 * 
 * - TODO: add UFF for preliminary/backup calculation
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 *
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
 * Validation carried out using MMFF94_opti.log and MMFF94_dative.mol2 
 * (761 models) using checkmm.spt (checkAllEnergies)
 * 
 * All typical compounds validate. The following 12 rather esoteric 
 * structures do not validate to within 0.1 kcal/mol total energy;
 * 

1 COMKAQ   E=   -7.3250003   Eref=  -7.6177  diff=  0.2926998
 -- MMFF94 ignores 1 of 5-membered ring torsions for a 1-oxo-2-oxa-bicyclo[3.2.0]heptane
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

2 DUVHUX10   E=   64.759995  Eref=  64.082855  diff=  0.6771393
 -- MMFF94 ignores 5-membered ring issue for S-S-containing ring
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate
 
3 FORJIF   E=   35.978   Eref=  35.833878  diff=  0.14412308
 -- MMFF94 uses some sort of undocumented empirical rule used for 1 torsion not found in tables
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

4 JADLIJ   E=   25.104   Eref=  24.7038  diff=  0.4001999
 -- ignores 5-membered ring for S (note, however, this is not the case in BODKOU)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

5 KEPKIZ   E=   61.127   Eref=  61.816277  diff=  0.68927765
 -- MMFF94 requires empirical rule parameters
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

6 PHOSLA10   E=   111.232994   Eref=  112.07078  diff=  0.8377838
 -- MMFF94 ignores all 5-membered ring torsions in ring with P
 -- (note, however, this is not the case in CUVGAB)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

7 PHOSLB10   E=   -93.479004   Eref=  -92.64081  diff=  0.8381958
 -- MMFF94 ignores all 5-membered ring torsions in ring with P
 -- (note, however, this is not the case in CUVGAB)
 -- MMFF94_bmin.log: WARNING - Conformational Energies May Not Be Accurate

empirical-rule-requiring models: (all are nonaromatic heterocycles)

8   ERULE_01   E=   -22.582  Eref=  -21.515108   diff=  1.0668926
9   ERULE_02   E=   29.407999  Eref=  29.799572  diff=  0.39157295
10  ERULE_03   E=   -3.326   Eref=  -2.9351802   diff=  0.3908198
11  ERULE_04   E=   -2.572   Eref=  -2.31007   diff=  0.26193
13  ERULE_08   E=   33.734   Eref=  34.41382   diff=  0.6798172 
 
 
 *
 * 
 * 
 */


public class ForceFieldMMFF extends ForceField {

  private static final int A4_SB = 125;
  private static final int A4_SBDEF = 126;
  private static final int A4_VDW = 3;
  private static final int A4_CHRG = 4;
  private static final int KEY_SBDEF = 0;
  private static final int KEY_PBCI = 0;
  private static final int KEY_VDW = 0;
  private static final int KEY_OOP = 6;
  private static final int TYPE_PBCI = 0x1;
  private static final int TYPE_VDW = 0x11;
  private static final int TYPE_CHRG = 0x22;
  // the following are bit flags indicating in the 0xF range 
  // which atoms might have default parameter values
  private static final int TYPE_BOND = 0x3;    //    0011
  private static final int TYPE_ANGLE = 0x5;   //    0101
  private static final int TYPE_SB = 0x15;     // 01 0101
  private static final int TYPE_SBDEF = 0x25;  // 10 0101
  private static final int TYPE_TORSION = 0x9; // 00 1001
  private static final int TYPE_OOP = 0xD;     // 00 1101;
  

  private static List<AtomType> atomTypes;
  private static Map<Integer, Object> ffParams;

  private int[] rawAtomTypes;
  private int[] rawBondTypes;
  private float[] rawMMFF94Charges; // calculated here
  
  public String[] getAtomTypeDescriptions() {
    return getAtomTypeDescs(rawAtomTypes);
  }

  public float[] getPartialCharges() {
    return rawMMFF94Charges;
  }

  /*
   * from SMARTS search when calculating partial charges:
   * 
   * vRings[0] list of 3-membered rings
   * vRings[1] list of 4-membered rings
   * vRings[2] list of all 5-membered rings
   * vRings[3] list of aromatic 5-membered and 6-membered rings
   */
  private List<BitSet>[] vRings;
  
  public ForceFieldMMFF(Minimizer m) {
    this.minimizer = m;
    getChargeParameters();
  }
  
  @Override
  public void clear() {
    // not same atoms?
    // TODO
    
  }

  @Override
  public boolean setModel(BitSet bsElements, int elemnoMax) {
    getMinimizationParameters();
    Minimizer m = minimizer;
    setArrays(m.atoms, m.bsAtoms, m.bonds, m.rawBondCount, false);  
    setModelFields();
    fixTypes();
    calc = new CalculationsMMFF(this, ffParams, minAtoms, minBonds, 
        minAngles, minTorsions, minimizer.constraints);
    calc.setLoggingEnabled(true);
    return calc.setupCalculations();
  }

  public void setArrays(Atom[] atoms, BitSet bsAtoms, Bond[] bonds,
                        int rawBondCount, boolean doRound) {
    Minimizer m = minimizer;
    // these are original atom-index-based, not minAtom-index based. 

    vRings = ArrayUtil.createArrayOfArrayList(4);
    rawAtomTypes = setAtomTypes(atoms, bsAtoms, m.viewer.getSmilesMatcher(),
        vRings);
    rawBondTypes = setBondTypes(bonds, rawBondCount, bsAtoms, rawAtomTypes,
        vRings[R56]);
    rawMMFF94Charges = getPartialCharges(bonds, rawBondTypes, atoms,
        rawAtomTypes, bsAtoms, doRound);
  }
  
  private void getChargeParameters() {
    if (ffParams != null)
      return;
    getAtomTypes("mmff/MMFF94-smarts.txt");
    Hashtable<Integer, Object> data = new Hashtable<Integer, Object>();
    getMmffParameters("mmff/mmffpbci.par.txt", data, TYPE_PBCI);
    getMmffParameters("mmff/mmffchg.par.txt", data, TYPE_CHRG);
    ffParams = data;
  }

  private void getMinimizationParameters() {
    // presumes charge parameters have been loaded
    if (ffParams.containsKey(Integer.valueOf(-1)))
      return;
    getMmffParameters("mmff/mmffvdw.par.txt",  ffParams, TYPE_VDW);
    getMmffParameters("mmff/mmffbond.par.txt", ffParams, TYPE_BOND);
    getMmffParameters("mmff/mmffang.par.txt",  ffParams, TYPE_ANGLE);
    getMmffParameters("mmff/mmffstbn.par.txt", ffParams, TYPE_SB);
    getMmffParameters("mmff/mmffdfsb.par.txt", ffParams, TYPE_SBDEF);
    getMmffParameters("mmff/mmfftor.par.txt",  ffParams, TYPE_TORSION);
    getMmffParameters("mmff/mmffoop.par.txt",  ffParams, TYPE_OOP);
    ffParams.put(Integer.valueOf(-1), Boolean.TRUE);
  }

  private void getMmffParameters(String fileName, Map<Integer, Object> data, int dataType) {    
    URL url = null;
    String line = null;
    
    // parameters are keyed by a 32-bit Integer 
    // that is composed of four 7-bit atom types and one 4-bit parameter type
    // in some cases, the last 7-bit atom type (a4) is used for additional parameter typing
    
    int a1, a2 = 127, a3 = 127, a4 = 127;
    Object value = null;
    if (Logger.debugging)
      Logger.info("reading data from " + fileName);
    try {
      if ((url = this.getClass().getResource(fileName)) == null) {
        System.err.println("Couldn't find file: " + fileName);
        throw new NullPointerException();
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      while ((line = br.readLine()) != null && line.length() < 5 || !line.startsWith("*"))
        continue; // skip header
      while ((line = br.readLine()) != null) {
        if (line.length() < 5 || line.startsWith("*"))
          continue;
        int type = line.charAt(0) - '0';
        switch (dataType) {
        case TYPE_OOP: // oop (tor max type is 5)
          type = KEY_OOP;
          break;
        case TYPE_SBDEF: // default stretch bend (by row; identified by a4 = 126, not 127)
          a4 = A4_SBDEF;
          type = KEY_SBDEF;
          break;
        case TYPE_SB: // stretch bend identified by a4 = 125, not 127
          a4 = A4_SB;
          break;
        case TYPE_CHRG: // chrg (bci, identified by a4 = 4, not 127)
          a4 = A4_CHRG;
          if (type == 4)
            continue; // I have no idea what type=4 here would mean. It's supposed to be a bond type
          break;
        case TYPE_VDW:  // vdw identified by a4 = 3, not 127
          if (line.charAt(5) != ' ')
            continue; // header stuff
          a4 = A4_VDW;
          type = KEY_VDW;
          break;
        case TYPE_PBCI:  // pbci
          type = KEY_PBCI;
          break;
        case TYPE_TORSION:  // tor
        case TYPE_ANGLE:  // angle
        case TYPE_BOND:  // bond
          break;          
        }
        switch (dataType) {
        case TYPE_TORSION: 
        case TYPE_OOP:
          a4 = Integer.valueOf(line.substring(18,20).trim()).intValue();
          // fall through
        case TYPE_ANGLE:
        case TYPE_SB:
        case TYPE_SBDEF:
          a3 = Integer.valueOf(line.substring(13,15).trim()).intValue();
          // fall through
        case TYPE_CHRG:
        case TYPE_BOND:
          a2 = Integer.valueOf(line.substring(8,10).trim()).intValue();
          break;
        case TYPE_VDW:
        case TYPE_PBCI:
          break;
        }
        a1 = Integer.valueOf(line.substring(3,5).trim()).intValue();
        switch (dataType) {
        case TYPE_PBCI:
          value = Float.valueOf(line.substring(5,15).trim());
          break;
        case TYPE_VDW: // vdw alpha-i, N-i, A-i, G-i, DA
          value = new double[] {
              Double.valueOf(line.substring(10,15).trim()).doubleValue(),
              Double.valueOf(line.substring(20,25).trim()).doubleValue(),
              Double.valueOf(line.substring(30,35).trim()).doubleValue(),
              Double.valueOf(line.substring(40,45).trim()).doubleValue(),
              line.charAt(46) // '-', 'A', 'D'
              };
          break;
        case TYPE_BOND: // bond stretch: kb, r0 
          value = new double[] {
              Double.valueOf(line.substring(14,20).trim()).doubleValue(),
              Double.valueOf(line.substring(25,31).trim()).doubleValue() };
          break;
        case TYPE_CHRG: // bond chrg
          value = Float.valueOf(line.substring(10,20).trim());
          break;
        case TYPE_ANGLE:   // angles: ka, theta0
        case TYPE_SB:  // stretch-bend: kbaIJK, kbaKJI
          value = new double[] {
              Double.valueOf(line.substring(19,25).trim()).doubleValue(),
              Double.valueOf(line.substring(28,35).trim()).doubleValue() };
          break;
        case TYPE_SBDEF: // default stretch-bend: F(I_J,K),F(K_J,I)  
          double v1 = Double.valueOf(line.substring(19,25).trim()).doubleValue();
          double v2 = Double.valueOf(line.substring(28,35).trim()).doubleValue();
          value = new double[] { v1, v2 };
          Integer key = MinObject.getKey(type, a1, a2, a3, a4);
          data.put(key, value);
          value = new double[] { v2, v1 };
          int a = a1;
          a1 = a3;
          a3 = a;
          break;
        case TYPE_TORSION: // tor: v1, v2, v3
          value = new double[] {
              Double.valueOf(line.substring(22,28).trim()).doubleValue(),
              Double.valueOf(line.substring(30,36).trim()).doubleValue(),
              Double.valueOf(line.substring(38,44).trim()).doubleValue()
              };
          break;
        case TYPE_OOP: // oop: koop  
          value = Double.valueOf(line.substring(24,30).trim());
          break;
        }        
        Integer key = MinObject.getKey(type, a1, a2, a3, a4);
        data.put(key, value);
        if (Logger.debugging)
          Logger.info(MinObject.decodeKey(key) + " " + Escape.escape(value));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName + " line=" + line);
    }
  }

  private void getAtomTypes(String fileName) {
    List<AtomType> types = new ArrayList<AtomType>();
    URL url = null;
    String line = null;
    try {
      if ((url = this.getClass().getResource(fileName)) == null) {
        System.err.println("Couldn't find file: " + fileName);
        throw new NullPointerException();
      }

      //turns out from the Jar file
      // it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      // and within Eclipse it's a BufferedInputStream

      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      types.add(new AtomType(0, 0, 0, 0, "H or NOT FOUND", ""));
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#"))
          continue;
        //0         1         2         3         4         5         6
        //0123456789012345678901234567890123456789012345678901234567890123456789
        //O   8 32  0  -4 NITRATE ANION OXYGEN      $([OD1][ND3]([OD1])[OD1])
        int elemNo = Integer.valueOf(line.substring(3,5).trim()).intValue();
        int mmType = Integer.valueOf(line.substring(6,8).trim()).intValue();
        int hType = Integer.valueOf(line.substring(9,11).trim()).intValue();
        float formalCharge = Float.valueOf(line.substring(12,15).trim()).floatValue()/12;
        String desc = line.substring(16,41).trim();
        String smarts = line.substring(42).trim();
        types.add(new AtomType(elemNo, mmType, hType, formalCharge, desc, smarts));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName + " line=" + line);

    }
    Logger.info((types.size()-1) + " SMARTS-based atom types read");
    atomTypes = types;

  }

  /**
   * assign partial charges ala MMFF94
   * 
   * @param bonds
   * @param bTypes 
   * @param bondCount 
   * @param atoms
   * @param aTypes
   * @param bsAtoms
   * @param doRound 
   * @return   full array of partial charges
   */
  public static float[] getPartialCharges(Bond[] bonds, int[] bTypes, Atom[] atoms,
                                          int[] aTypes, BitSet bsAtoms, boolean doRound) {

    // start with formal charges specified by MMFF94 (not what is in file!)

    float[] partialCharges = new float[atoms.length];
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      partialCharges[i] = atomTypes.get(Math.max(0, aTypes[i])).formalCharge;
    // run through all bonds, adjusting formal charges as necessary
    Atom a1 = null;
    for (int i = bTypes.length; --i >= 0;) {
      a1 = bonds[i].getAtom1();
      Atom a2 = bonds[i].getAtom2();
      // It's possible that some of our atoms are not in the atom set,
      // but we don't want both of them to be out of the set.
      
      boolean ok1 = bsAtoms.get(a1.index);
      boolean ok2 = bsAtoms.get(a2.index); 
      if (!ok1 && !ok2)
        continue;
      int it = aTypes[a1.index];
      AtomType at1 = atomTypes.get(Math.max(0, it));
      int type1 = (it < 0 ? -it : at1.mmType);
      it = aTypes[a2.index];
      AtomType at2 = atomTypes.get(Math.max(0, it));
      int type2 = (it < 0 ? -it : at2.mmType);
      
      // we are only interested in bonds that are between different atom types
      
//      if (type1 == type2)
  //      continue;
      
      // check for bond charge increment
      
      // The table is created using the key (100 * type1 + type2), 
      // where type1 < type2. In addition, we encode the partial bci values
      // with key (100 * type)
      
      float dq;  // the difference in charge to be added or subtracted from the formal charges
      try {
        int bondType = bTypes[i];
        float bFactor = (type1 < type2 ? -1 : 1);
        Integer key = MinObject.getKey(bondType, bFactor == 1 ? type2 : type1, bFactor == 1 ? type1 : type2, 127, A4_CHRG);
        Float bciValue = (Float) ffParams.get(key);
        float bci;
        String msg = (Logger.debugging ? a1 + "/" + a2 + " mmTypes=" + type1 + "/" + type2 + " formalCharges=" + at1.formalCharge + "/" + at2.formalCharge + " bci = " : null);
        if (bciValue == null) { 
          // no bci was found; we have to use partial bond charge increments
          // a failure here indicates we don't have information
          float pa = ((Float) ffParams.get(MinObject.getKey(KEY_PBCI, type1, 127, 127, 127))).floatValue();
          float pb = ((Float) ffParams.get(MinObject.getKey(KEY_PBCI, type2, 127, 127, 127))).floatValue();
          bci = pa - pb;
          if (Logger.debugging)
            msg += pa + " - " + pb + " = ";
        } else {
          bci = bFactor * bciValue.floatValue();
        }
        if (Logger.debugging) {
          msg += bci;
          Logger.debug(msg);
        }
        // Here's the way to do this:
        //
        // 1) The formal charge on each atom is adjusted both by
        // taking on an (arbitrary?) fraction of the formal charge on its partner
        // and by giving up an (arbitrary?) fraction of its own formal charge
        // Note that the formal charge is the one specified in the MMFF94 parameters,
        // NOT the model file. The compounds in MMFF94_dative.mol2, for example, do 
        // not indicate formal charges. The only used fractions are 0, 0.25, and 0.5. 
        //
        // 2) Then the bond charge increment is added.
        //
        // Note that this value I call "dq" is added to one atom and subtracted from its partner
        
        dq = at2.fcadj * at2.formalCharge - at1.fcadj * at1.formalCharge + bci;
      } catch (Exception e) {
        dq = Float.NaN;
      }
      if (ok1)
        partialCharges[a1.index] += dq;
      if (ok2)
        partialCharges[a2.index] -= dq;
    }
    
    // just rounding to 0.001 here:
    
    if (doRound) {
      float abscharge = 0;
      for (int i = partialCharges.length; --i >= 0;) {
        partialCharges[i] = ((int) (partialCharges[i] * 1000)) / 1000f;
        abscharge += Math.abs(partialCharges[i]);
      }
      if (abscharge == 0 && a1 != null) {
        partialCharges[a1.index]= -0.0f;
      }
    }    
    return partialCharges;
  }

  private static boolean isBondType1(AtomType at1, AtomType at2) {
    return at1.sbmb && at2.sbmb || at1.arom && at2.arom; 
    // but what about at1.sbmb && at2.arom?
  }

  private static int getBondType(Bond bond, AtomType at1, AtomType at2,
                               int index1, int index2, List<BitSet> vAromatic56) {
  return (isBondType1(at1, at2) && 
      bond.getCovalentOrder() == 1 
      && !isAromaticBond(index1, index2, vAromatic56) ? 1 : 0);  
 }

  private static boolean isAromaticBond(int a1, int a2, List<BitSet> vAromatic56) {
    for (int i = vAromatic56.size(); --i >= 0;) {
      BitSet bsRing = vAromatic56.get(i);
      if (bsRing.get(a1) && bsRing.get(a2))
        return true;
    }
    return false;
  }

  public static String[] getAtomTypeDescs(int[] types) {
    String[] stypes = new String[types.length];
    for (int i = types.length; --i >= 0;) {
      stypes[i] = String.valueOf(types[i] < 0 ? -types[i] : atomTypes.get(types[i]).mmType);
    }
    return stypes;
  }

  ///////////// MMFF94 object typing //////////////
  
  /**
   * The file MMFF94-smarts.txt is derived from MMFF94-smarts.xlsx.
   * This file contains records for unique atom type/formal charge sharing/H atom type.
   * For example, the MMFF94 type 6 is distributed over eight AtomTypes, 
   * each with a different SMARTS match.
   * 
   * H atom types are given in the file as properties of other atom types, not
   * as their own individual SMARTS searches. H atom types are determined based
   * on their attached atom's atom type.
   * 
   * @param atoms
   * @param bsAtoms
   * @param smartsMatcher
   * @param vRings
   * @return  array of indexes into AtomTypes or, for H, negative of mmType
   */
  private static int[] setAtomTypes(Atom[] atoms, BitSet bsAtoms, 
                               SmilesMatcherInterface smartsMatcher, 
                               List<BitSet>[] vRings) {
    List<BitSet>bitSets = new ArrayList<BitSet>();
    String[] smarts = new String[atomTypes.size()];
    int[] types = new int[atoms.length];
    BitSet bsElements = new BitSet();
    BitSet bsHydrogen = new BitSet();
    BitSet bsConnected = BitSetUtil.copy(bsAtoms);
    
    // It may be important to include all attached atoms
    
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      Bond[] bonds = a.getBonds();
      if (bonds != null)
        for (int j = bonds.length; --j >= 0;)
          if (bonds[j].isCovalent())
            bsConnected.set(bonds[j].getOtherAtom(a).index);
    }
    
    // we need to identify H atoms and also make a BitSet of all the elements
    
    for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected.nextSetBit(i + 1)) {
      int n = atoms[i].getElementNumber();
      switch (n) {
      case 1:
        bsHydrogen.set(i);
        break;
      default:
        bsElements.set(n);
      }
    }
    
    // generate a list of SMART codes 
    
    int nUsed = 0;
    for (int i = 1; i < atomTypes.size(); i++) {
      AtomType at = atomTypes.get(i);
      if (!bsElements.get(at.elemNo))
        continue;
      smarts[i] = at.smartsCode;
      nUsed++;
    }
    Logger.info(nUsed + " SMARTS matches used");
    
    // The SMARTS list is organized from least general to most general
    // for each atom. So the FIRST occurrence of an atom in the list
    // identifies that atom's MMFF94 type.
    
    smartsMatcher.getSubstructureSets(smarts, atoms, atoms.length, 
        JmolEdge.FLAG_AROMATIC_STRICT | JmolEdge.FLAG_AROMATIC_DOUBLE, 
        bsConnected, bitSets, vRings);
    BitSet bsDone = new BitSet();
    for (int j = 0; j < bitSets.size(); j++) {
      BitSet bs = bitSets.get(j);
      if (bs == null)
        continue;
      // This is a one-pass system. We first exclude
      // all atoms that are already identified...
      bs.andNot(bsDone);
      // then we set the type of what is remaining...
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        types[i] = j;
      // then we include these atoms in the set of atoms already identified
      bsDone.or(bs);
    }

    // now we add in the H atom types as the negative of their MMFF94 type
    // rather than as an index into AtomTypes. 
    
    for (int i = bsHydrogen.nextSetBit(0); i >= 0; i = bsHydrogen.nextSetBit(i + 1)) {
      Bond[] bonds = atoms[i].getBonds();
      if (bonds != null) {
        types[i] = -atomTypes.get(types[bonds[0].getOtherAtom(atoms[i]).index]).hType;
      }
    }
    if (Logger.debugging)
      for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected.nextSetBit(i + 1))
        Logger.info("atom " + atoms[i] + "\ttype " + (types[i] < 0 ? "" + -types[i] : (atomTypes.get(types[i]).mmType + "\t" + atomTypes.get(types[i]).smartsCode + "\t"+ atomTypes.get(types[i]).descr)));
    
    return types;
  }

  private static int[] setBondTypes(Bond[] bonds, int bondCount, BitSet bsAtoms,
                                    int[] aTypes, List<BitSet> vAromatic56) {
     int[] bTypes = new int[bondCount];
     for (int i = bondCount; --i >= 0;) {
       Atom a1 = bonds[i].getAtom1();
       Atom a2 = bonds[i].getAtom2();
       boolean ok1 = bsAtoms.get(a1.index);
       boolean ok2 = bsAtoms.get(a2.index);
       if (!ok1 && !ok2)
         continue;
       int it = aTypes[a1.index];
       AtomType at1 = atomTypes.get(Math.max(0, it));
       it = aTypes[a2.index];
       AtomType at2 = atomTypes.get(Math.max(0, it));
       bTypes[i] = getBondType(bonds[i], at1, at2, a1.index, a2.index,
           vAromatic56);
     }
     return bTypes;
   }

  private void fixTypes() {
    // set atom types in minAtoms
    for (int i = minAtomCount; --i >= 0;) {
      MinAtom a = minAtoms[i];
      int rawIndex = a.atom.index;
      int it = rawAtomTypes[rawIndex];
      a.ffAtomType = atomTypes.get(Math.max(0, it));
      int type = (it < 0 ? -it : atomTypes.get(it).mmType);
      a.ffType = type;
      a.vdwKey = MinObject.getKey(KEY_VDW, type, 127, 127, A4_VDW);
      a.partialCharge = rawMMFF94Charges[rawIndex];
    }
    
    for (int i = minBonds.length; --i >= 0;) {
      MinBond bond = minBonds[i];
      bond.type = rawBondTypes[bond.rawIndex];
      bond.key = getKey(bond, bond.type, TYPE_BOND);
    }
    
    for (int i = minAngles.length; --i >= 0;) {
      MinAngle angle = minAngles[i];
      angle.key = getKey(angle, angle.type, TYPE_ANGLE);
      angle.sbKey = getKey(angle, angle.sbType, TYPE_SB);
    }
    
    for (int i = minTorsions.length; --i >= 0;) {
      MinTorsion torsion = minTorsions[i];
      torsion.key = getKey(torsion, torsion.type, TYPE_TORSION);
    }
  }

  private final static int[] sbMap = {0, 1, 3, 5, 4, 6, 8, 9, 11};

  private int setAngleType(MinAngle angle) {
    /*
    0      The angle <i>i-j-k</i> is a "normal" bond angle
    1        Either bond <i>i-j</i> or bond <i>j-k</i> has a bond type of 1
    2      Bonds<i> i-j</i> and <i>j-k</i> each have bond types of 1; the sum is 2.
    3      The angle occurs in a three-membered ring
    4      The angle occurs in a four-membered ring
    5      Is in a three-membered ring and the sum of the bond types is 1
    6      Is in a three-membered ring and the sum of the bond types is 2
    7      Is in a four-membered ring and the sum of the bond types is 1
    8      Is in a four-membered ring and the sum of the bond types is 2
    */
    angle.type = minBonds[angle.data[ABI_IJ]].type + minBonds[angle.data[ABI_JK]].type;
    if (checkRings(vRings[R3], angle.data, 3)) {
      angle.type += (angle.type == 0 ? 3 : 4);
    } else if (checkRings(vRings[R4], angle.data, 3)) {
      angle.type += (angle.type == 0 ? 4 : 6);
    }
    
    /*
    SBT   AT BT[IJ] BT[JK]
    -------------------------------------------------------------
    0     0    0    0
    1     1    1    0
    2     1    0    1  [error in table]
    3     2    1    1  [error in table]
    4     4    0    0
    5     3    0    0
    6     5    1    0
    7     5    0    1
    8     6    1    1
    9     7    1    0
    10     7    0    1
    11     8    1    1
     */
    
    angle.sbType = sbMap[angle.type];
    switch (angle.type) {
    case 1:
    case 5:
    case 7:
      angle.sbType += minBonds[angle.data[ABI_JK]].type;
      break;
    }
    return angle.type;
  }
  
  private int setTorsionType(MinTorsion t) {
    if (checkRings(vRings[R4], t.data, 4)) 
      return (t.type = 4); // in 4-membered ring
    t.type = (minBonds[t.data[TBI_BC]].type == 1 ? 1 
        : minBonds[t.data[TBI_AB]].type == 0 && minBonds[t.data[TBI_CD]].type == 0 ? 0 : 2);
    if (t.type == 0 && checkRings(vRings[R5], t.data, 4)) {
      t.type = 5; // in 5-membered ring
    }
    return t.type;
  }

  private int typeOf(int iAtom) {
    return minAtoms[iAtom].ffType;
  }
  
  private boolean checkRings(List<BitSet> v, int[] minlist, int n) {
    if (v != null)
      for (int i = v.size(); --i >= 0;) {
        BitSet bs = v.get(i);
        if (bs.get(minAtoms[minlist[0]].atom.index)
            && bs.get(minAtoms[minlist[1]].atom.index)
            && (n < 3 || bs.get(minAtoms[minlist[2]].atom.index))
            && (n < 4 || bs.get(minAtoms[minlist[3]].atom.index)))
          return true;
      }
    return false; 
  }

  private Integer getKey(Object obj, int type, int ktype) {
    MinObject o = (obj instanceof MinObject ? (MinObject) obj : null);
    int[] data = (o == null ? (int[]) obj : o.data);
    int n = 4;
    switch (ktype) {
    case TYPE_BOND:
      fixOrder(data, 0, 1);
      n = 2;
      break;
    case TYPE_ANGLE:
      if (fixOrder(data, 0, 2) == -1)
        swap(data, ABI_IJ, ABI_JK);
      type = setAngleType((MinAngle) o);
      n = 3;
      break;
    case TYPE_SB:
      n = 3;
      break;
    case TYPE_TORSION:
      switch (fixOrder(data, 1, 2)) {
      case 1:
        break;
      case -1:
        swap(data, 0, 3);
        swap(data, TBI_AB, TBI_CD);
        break;
      case 0:
        if (fixOrder(data, 0, 3) == -1)
          swap(data, TBI_AB, TBI_CD);
        break;
      }
      type = setTorsionType((MinTorsion) o);
    }
    Integer key = null;
    for (int i = 0; i < 4; i++)
      typeData[i] = (i < n ? typeOf(data[i]) : 127);
    switch (ktype) {
    case TYPE_SB:
      typeData[3] = A4_SB;
      break;
    case TYPE_OOP:
      sortOop(typeData);
      break;
    }
    key = MinObject.getKey(type, typeData[0], typeData[1], typeData[2],
        typeData[3]);
    if (ffParams.containsKey(key))
      return key;
    // default typing
    switch (ktype) {
    case TYPE_BOND:
      key = applyEmpiricalRules(o, ktype);
      if (key != null)
        return key;
      break;
    case TYPE_TORSION:
      if (ffParams.containsKey(key = getTorsionKey(type, 0, 2)))
        return key;
      if (ffParams.containsKey(key = getTorsionKey(type, 2, 0)))
        return key;
      if (ffParams.containsKey(key = getTorsionKey(type, 2, 2)))
        return key;
      // set type = 0 if necessary...
      return getTorsionKey(0, 2, 2);
    case TYPE_SB:
      // use periodic row info
      int r1 = getRowFor(data[0]);
      int r2 = getRowFor(data[1]);
      int r3 = getRowFor(data[2]);
      return MinObject.getKey(KEY_SBDEF, r1, r2, r3, A4_SBDEF);
    }
    // run through equivalent types, really just 3
    boolean isSwapped = false;
    boolean haveKey = false;
    for (int i = 0; i < 3 && !haveKey; i++) {
      for (int j = 0, bit = 1; j < n; j++, bit <<= 1)
        if ((ktype & bit) == bit)
          typeData[j] = getEquivalentType(typeOf(data[j]), i);
      switch (ktype) {
      case TYPE_BOND:
        isSwapped = (fixTypeOrder(typeData, 0, 1));
        break;
      case TYPE_ANGLE:
        isSwapped = (fixTypeOrder(typeData, 0, 2));
        break;
      case TYPE_OOP:
        sortOop(typeData);
        break;
      }
      key = MinObject.getKey(type, typeData[0], typeData[1], typeData[2],
          typeData[3]);
      haveKey = ffParams.containsKey(key);
    }
    if (haveKey) {
      if (isSwapped)
        switch (ktype) {
        case TYPE_ANGLE:
          swap(data, 0, 2);
          swap(data, ABI_IJ, ABI_JK);
          setAngleType((MinAngle) o);
          break;
        }
    } else if (type != 0 && ktype == TYPE_ANGLE) {
      key = Integer.valueOf(key.intValue() ^ 0xFF);
    }
    return key;
  }

  private Integer getTorsionKey(int type, int i, int j) {
    return MinObject.getKey(type, getEquivalentType(typeData[0], i),
        typeData[1], typeData[2], getEquivalentType(typeData[3], j));  
  }

  private Integer applyEmpiricalRules(MinObject o, int ktype) {
    // TODO
    return null;
  }

  private int getRowFor(int i) {
    int elemno = minAtoms[i].atom.getElementNumber();
    return (elemno < 3 ? 0 : elemno < 11 ? 1 : elemno < 19 ? 2 : elemno < 37 ? 3 : 4);
  }

  private int[] typeData = new int[4];
  
  double getOutOfPlaneParameter(int[] data) {
    Integer key = getKey(data, KEY_OOP, TYPE_OOP);
    Double params = (Double) ffParams.get(key);    
    return (params == null ? 0 : params.doubleValue());
  }

  private static void sortOop(int[] typeData) {
    fixTypeOrder(typeData, 0, 2);
    fixTypeOrder(typeData, 0, 3);
    fixTypeOrder(typeData, 2, 3);
  }

  /**
   * 
   * @param a
   * @param i
   * @param j
   * @return true if swapped; false if not
   */
  private static boolean fixTypeOrder(int[] a, int i, int j) {
    if (a[i] > a[j]) {
      swap(a, i, j);
      return true;
    }
    return false;
  }

  /**
   * 
   * @param a
   * @param i
   * @param j
   * @return  1 if in order, 0 if same, -1 if reversed
   */
  private int fixOrder(int[] a, int i, int j) {
    int test = typeOf(a[j]) - typeOf(a[i]); 
    if (test < 0)
      swap(a, i, j);
    return (test < 0 ? -1 : test > 0 ? 1 : 0);
  }

  private static void swap(int[] a, int i, int j) {
    int t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

  private final static int[] equivalentTypes = {
    1,  1, //  1
    2,  1, //  2
    3,  1, //  3
    4,  1, //  4
    5,  5, //  5
    6,  6, //  6
    7,  6, //  7
    8,  8, //  8
    9,  8, //  9
   10,  8, // 10
   11, 11, // 11
   12, 12, // 12
   13, 13, // 13
   14, 14, // 14
   15, 15, // 15
   16, 15, // 16
   17, 15, // 17
   18, 15, // 18
   19, 19, // 19
    1,  1, // 20
   21,  5, // 21
   22,  1, // 22
   23,  5, // 23
   24,  5, // 24
   25, 25, // 25
   26, 25, // 26
   28,  5, // 27
   28,  5, // 28
   29,  5, // 29
    2,  1, // 30
   31, 31, // 31
    7,  6, // 32
   21,  5, // 33
    8,  8, // 34
    6,  6, // 35
   36,  5, // 36
    2,  1, // 37
    9,  8, // 38
   10,  8, // 39
   10,  8, // 40
    3,  1, // 41
   42,  8, // 42
   10,  8, // 43
   16, 15, // 44
   10,  8, // 45
    9,  8, // 46
   42,  8, // 47
    9,  8, // 48
    6,  6, // 49
   21,  5, // 50
    7,  6, // 51
   21,  5, // 52
   42,  8, // 53
    9,  8, // 54
   10,  8, // 55
   10,  8, // 56
    2,  1, // 57
   10,  8, // 58
    6,  6, // 59
    4,  1, // 60
   42,  8, // 61
   10,  8, // 62
    2,  1, // 63
    2,  1, // 64
    9,  8, // 65
    9,  8, // 66
    9,  8, // 67
    8,  8, // 68
    9,  8, // 69
   70, 70, // 70
    5,  5, // 71
   16, 15, // 72
   18, 15, // 73
   17, 15, // 74
   26, 25, // 75
    9,  8, // 76
   12, 12, // 77
    2,  1, // 78
    9,  8, // 79
    2,  1, // 80
   10,  8, // 81
    9,  8, // 82
  };

  /**
   * equivalent types for OOP and torsions
   * 
   * @param type  mmFF94 atom type 
   * @param level  0, 1, or 2.
   * @return equivalent type or 0
   * 
   */
  private static int getEquivalentType(int type, int level) {
    return (type == 0 ? 0 : type == 70 || type > 82 ? type : level == 2 ? 0 : 
      equivalentTypes[((type - 1) << 1) + level]);
  }
}
