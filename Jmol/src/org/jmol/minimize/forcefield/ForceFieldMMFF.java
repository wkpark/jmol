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

public class ForceFieldMMFF extends ForceField {

  private static final int A4_SB_CHRG = 1;
  private static final int A4_SBDEF = 2;
  private static final int A4_VDW = 1;

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
    setArrays(m.atoms, m.bsAtoms, m.bonds, m.rawBondCount);  
    setModelFields();
    fixTypes();
    return false;
  }

  public void setArrays(Atom[] atoms, BitSet bsAtoms, Bond[] bonds, int rawBondCount) {
    Minimizer m = minimizer;
    // these are original atom-index-based, not minAtom-index based. 
    
    vRings = ArrayUtil.createArrayOfArrayList(4);
    rawAtomTypes = setAtomTypes(atoms, bsAtoms, m.viewer
        .getSmilesMatcher(), vRings);
    rawBondTypes = setBondTypes(bonds, rawBondCount,
        bsAtoms, rawAtomTypes, vRings[R56]);
    rawMMFF94Charges = getPartialCharges(bonds, rawBondTypes,
        atoms, rawAtomTypes, bsAtoms);
  }
  
  private void getChargeParameters() {
    if (ffParams != null)
      return;
    getAtomTypes("mmff/MMFF94-smarts.txt");
    Hashtable<Integer, Object> data = new Hashtable<Integer, Object>();
    getMmffParameters("mmff/mmffpbci.par.txt", data, 1);
    getMmffParameters("mmff/mmffchg.par.txt", data, 22);
    ffParams = data;
  }

  private void getMinimizationParameters() {
    // presumes charge parameters have been loaded
    if (ffParams.containsKey(Integer.valueOf(-1)))
      return;
    getMmffParameters("mmff/mmffvdw.par.txt",  ffParams, 11);
    getMmffParameters("mmff/mmffbond.par.txt", ffParams, 2);
    getMmffParameters("mmff/mmffang.par.txt",  ffParams, 3);
    getMmffParameters("mmff/mmffstbn.par.txt", ffParams, 33);
    getMmffParameters("mmff/mmffdfsb.par.txt", ffParams, 333);
    getMmffParameters("mmff/mmfftor.par.txt",  ffParams, 4);
    getMmffParameters("mmff/mmffoop.par.txt",  ffParams, 44);
    ffParams.put(Integer.valueOf(-1), Boolean.TRUE);
  }

  private void getMmffParameters(String fileName, Map<Integer, Object> data, int dataType) {    
    URL url = null;
    String line = null;
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
        case 44: // oop (tor max type is 5)
          type = 6;
          break;
        case 333: // default stretch bend (by row; identified by a4 = 2, not 127)
          a4 = A4_SBDEF;
          type = 0;
          break;
        case 33: // stretch bend identified by a4 = 1, not 127
        case 22: // chrg (bci, identified by a4 = 1, not 127)
          a4 = A4_SB_CHRG;
          if (type == 4)
            continue; // I have no idea what type=4 here would mean. It's supposed to be a bond type
          break;
        case 11:  // vdw identified by a4 = 1, not 127
          if (line.charAt(5) != ' ')
            continue;
          a4 = A4_VDW;
          type = 0;
          break;
        case 4:  // tor
        case 3:  // angle
        case 2:  // bond
        case 1:  // pbci
          break;          
        }
        switch (dataType) {
        case 44: 
        case 4:
          a4 = Integer.valueOf(line.substring(18,20).trim()).intValue();
          // fall through
        case 333:
        case 33:
        case 3:
          a3 = Integer.valueOf(line.substring(13,15).trim()).intValue();
          // fall through
        case 22:
        case 2:
          a2 = Integer.valueOf(line.substring(8,10).trim()).intValue();
          break;
        case 11:
        case 1:
          break;
        }
        a1 = Integer.valueOf(line.substring(3,5).trim()).intValue();
        switch (dataType) {
        case 1: // atom pbci
          value = Float.valueOf(line.substring(5,15).trim());
          break;
        case 11: // vdw alpha-i, N-i, A-i, G-i, DA
          value = new double[] {
              Double.valueOf(line.substring(10,15).trim()).doubleValue(),
              Double.valueOf(line.substring(20,25).trim()).doubleValue(),
              Double.valueOf(line.substring(30,35).trim()).doubleValue(),
              Double.valueOf(line.substring(40,45).trim()).doubleValue(),
              line.charAt(46) // '-', 'A', 'D'
              };
          break;
        case 2: // bond stretch: kb, r0 
          value = new double[] {
              Double.valueOf(line.substring(14,20).trim()).doubleValue(),
              Double.valueOf(line.substring(25,31).trim()).doubleValue() };
          break;
        case 22: // bond chrg
          value = Float.valueOf(line.substring(10,20).trim());
          break;
        case 3:   // angles: ka, theta0
        case 33:  // stretch-bend: kbaIJK, kbaKJI
        case 333: // default stretch-bend: F(I_J,K),F(K_J,I)  
          value = new double[] {
              Double.valueOf(line.substring(19,25).trim()).doubleValue(),
              Double.valueOf(line.substring(28,35).trim()).doubleValue() };
          break;
        case 4: // tor: v1, v2, v3
          value = new double[] {
              Double.valueOf(line.substring(22,28).trim()).doubleValue(),
              Double.valueOf(line.substring(30,36).trim()).doubleValue(),
              Double.valueOf(line.substring(38,44).trim()).doubleValue()
              };
          break;
        case 44: // oop: koop  
          value = Float.valueOf(line.substring(24,30).trim());
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
   * @return   full array of partial charges
   */
  public static float[] getPartialCharges(Bond[] bonds, int[] bTypes, Atom[] atoms,
                                          int[] aTypes, BitSet bsAtoms) {

    // start with formal charges specified by MMFF94 (not what is in file!)

    float[] partialCharges = new float[atoms.length];
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      partialCharges[i] = atomTypes.get(Math.max(0, aTypes[i])).formalCharge;

    // run through all bonds, adjusting formal charges as necessary

    for (int i = bTypes.length; --i >= 0;) {
      Atom a1 = bonds[i].getAtom1();
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
        Integer key = MinObject.getKey(bondType, bFactor == 1 ? type2 : type1, bFactor == 1 ? type1 : type2, 127, A4_SB_CHRG);
        Float bciValue = (Float) ffParams.get(key);
        float bci;
        String msg = (Logger.debugging ? a1 + "/" + a2 + " mmTypes=" + type1 + "/" + type2 + " formalCharges=" + at1.formalCharge + "/" + at2.formalCharge + " bci = " : null);
        if (bciValue == null) { 
          // no bci was found; we have to use partial bond charge increments
          // a failure here indicates we don't have information
          float pa = ((Float) ffParams.get(MinObject.getKey(0, type1, 127, 127, 127))).floatValue();
          float pb = ((Float) ffParams.get(MinObject.getKey(0, type2, 127, 127, 127))).floatValue();
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
    
    for (int i = partialCharges.length; --i >= 0;)
      partialCharges[i] = ((int) (partialCharges[i] * 1000)) / 1000f;
    
    // that's all thre is to it!
    
    return partialCharges;
  }

  private static boolean isBondType1(AtomType at1, AtomType at2) {
    return at1.sbmb && at2.sbmb || at1.arom && at2.arom; 
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
            bsConnected.set(j);
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
        //System.out.println(atoms[i] + " " + bonds[0].getOtherAtom(atoms[i]) + " " +atomTypes.get(types[bonds[0].getOtherAtom(atoms[i]).index]).mmType 
          //    + " " + atomTypes.get(types[bonds[0].getOtherAtom(atoms[i]).index]).hType);
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
      int it = rawAtomTypes[minAtoms[i].atom.index];
      minAtoms[i].ffAtomType = atomTypes.get(Math.max(0, it));
      minAtoms[i].ffType = (it < 0 ? -it : atomTypes.get(it).mmType);      
    }
    
    // fix order in bonds and set type and key
    System.out.println("-- bonds --");
    for (int i = minBondCount; --i >= 0;) {
      MinBond bond = minBonds[i];
      fixOrder(bond.data, 0, 1);
      bond.type = rawBondTypes[bond.rawIndex];
      bond.key = MinObject.getKey(bond.type, 
          minAtoms[bond.data[0]].ffType, 
          minAtoms[bond.data[1]].ffType, 
          127, 127);
      System.out.println(bond 
          + " " + minAtoms[bond.data[0]]
          + " " + minAtoms[bond.data[1]]
          + "\t" + Escape.escape(getFFParams(bond.key)));
    }
    
    // fix order in angles and set type
    System.out.println("-- angles --");

    for (int i = minAngles.length; --i >= 0;) {
     MinAngle angle = minAngles[i];
      if (fixOrder(angle.data, 0, 2) == -1)
        swap(angle.data, ABI_IJ, ABI_JK);
      setAngleType(angle);
      angle.key = MinObject.getKey(angle.type, 
          minAtoms[angle.data[0]].ffType, 
          minAtoms[angle.data[1]].ffType, 
          minAtoms[angle.data[2]].ffType, 
          127);
      System.out.println(angle
          + " " + minAtoms[angle.data[0]]
          + " " + minAtoms[angle.data[1]]
          + " " + minAtoms[angle.data[2]]
          + "\t" + Escape.escape(getFFParams(angle.key))
      );
      int typeA = minAtoms[angle.data[0]].ffType;
      int typeB = minAtoms[angle.data[1]].ffType;
      int typeC = minAtoms[angle.data[2]].ffType;
      angle.sbKey = MinObject.getKey(angle.sbType, typeA, typeB, typeC, A4_SB_CHRG);
      if (getFFParams(angle.sbKey) == null) {
        int r1 = getRowFor(angle.data[0]);
        int r2 = getRowFor(angle.data[1]);
        int r3 = getRowFor(angle.data[2]);
        angle.sbKey = MinObject.getKey(angle.sbType, r1, r2, r3, A4_SBDEF);
      }
      System.out.println(angle
          + " " + minAtoms[angle.data[0]]
          + " " + minAtoms[angle.data[1]]
          + " " + minAtoms[angle.data[2]]
          + "\tSB " + Escape.escape(getFFParams(angle.sbKey))
      );
    }
    
    // fix order in torsions and set type
   System.out.println("-- torsions --");
    for (int i = minTorsions.length; --i >= 0;) {
      MinTorsion t = minTorsions[i];
      switch (fixOrder(t.data, 1, 2)) {
      case 1:
        break;
      case -1:
        swap(t.data, 0, 3);
        swap(t.data, TBI_AB, TBI_CD);
        break;
      case 0:
        if(fixOrder(t.data, 0, 3) == -1)
          swap(t.data, TBI_AB, TBI_CD);
        break;
      }
      setTorsionType(t);
      int typeA = minAtoms[t.data[0]].ffType;
      int typeB = minAtoms[t.data[1]].ffType;
      int typeC = minAtoms[t.data[2]].ffType;
      int typeD = minAtoms[t.data[3]].ffType;
      t.key = MinObject.getKey(t.type, typeA, typeB, typeC, typeD);
      if (getFFParams(t.key) == null) {
        t.key = MinObject.getKey(t.type, getEquivalentType(typeA, 0), typeB, typeC, getEquivalentType(typeD, 2));
        if (getFFParams(t.key) == null) {
          t.key = MinObject.getKey(t.type, getEquivalentType(typeA, 2), typeB, typeC, getEquivalentType(typeD, 0));
          if (getFFParams(t.key) == null) {
            t.key = MinObject.getKey(t.type, getEquivalentType(typeA, 2), typeB, typeC, getEquivalentType(typeD, 2));
            if (getFFParams(t.key) == null && t.type != 0) {
              t.key = MinObject.getKey(0, getEquivalentType(typeA, 2), typeB, typeC, getEquivalentType(typeD, 2));
            }
          }
        }
      }
      System.out.println(t 
          + " " + minAtoms[t.data[0]]
          + " " + minAtoms[t.data[1]]
          + " " + minAtoms[t.data[2]]
          + " " + minAtoms[t.data[3]]
          + "\t" + Escape.escape(getFFParams(t.key)));
    }
    System.out.println("done in ForceFieldMMFF");
  }

  private int getRowFor(int i) {
    int elemno = minAtoms[i].atom.getElementNumber();
    return (elemno < 3 ? 1 : elemno < 11 ? 2 : elemno < 19 ? 3 : 4);
  }

  private static Object getFFParams(Integer key) {
    return (key == null ? null : ffParams.get(key));
  }

  private final static int[] sbMap = {0, 1, 3, 5, 4, 6, 8, 9, 11};

  private void setAngleType(MinAngle angle) {
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
  }
  
  private void setTorsionType(MinTorsion t) {
    // from chemkit MmffAtomTyper::torsionInteractionType
    if (checkRings(vRings[R4], t.data, 3)) { 
      t.type = 4; // in 4-membered ring
      return;
    }
    if (checkRings(vRings[R5], t.data, 4)) { 
      t.type = 5; // in 5-membered ring
      return;
    }
    t.type = (minBonds[t.data[TBI_BC]].type == 1 ? 1 
        : minBonds[t.data[TBI_AB]].type == 0 && minBonds[t.data[TBI_CD]].type == 0 ? 0 : 2);
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

  /**
   * 
   * @param a
   * @param i
   * @param j
   * @return  1 if in order, 0 if same, -1 if reversed
   */
  private int fixOrder(int[] a, int i, int j) {
    int test = minAtoms[a[j]].ffType - minAtoms[a[i]].ffType; 
    if (test < 0)
      swap(a, i, j);
    return (test < 0 ? -1 : test > 0 ? 1 : 0);
  }

  private static void swap(int[] a, int i, int j) {
    int t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

  /*
   * atype aspec crd val  pilp mltb arom lin sbmb
     1    6    4    4    0    0    0    0    0
     2    6    3    4    0    2    0    0    1
     3    6    3    4    0    2    0    0    1
     4    6    2    4    0    3    0    1    1
     5    1    1    1    0    0    0    0    0
     6    8    2    2    1    0    0    0    0
     7    8    1    2    0    2    0    0    0
     8    7    3    3    1    0    0    0    0
     9    7    2    3    0    2    0    0    1
    10    7    3    3    1    1    0    0    0
    11    9    1    1    1    0    0    0    0
    12   17    1    1    1    0    0    0    0
    13   35    1    1    1    0    0    0    0
    14   53    1    1    1    0    0    0    0
    15   16    2    2    1    0    0    0    0
    16   16    1    2    0    2    0    0    0
    17   16    3    4    0    2    0    0    0
    18   16    4    4    0    0    0    0    0
    19   14    4    4    0    0    0    0    0
    20    6    4    4    0    0    0    0    0
    21    1    1    1    0    0    0    0    0
    22    6    4    4    0    0    0    0    0
    23    1    1    1    0    0    0    0    0
    24    1    1    1    0    0    0    0    0
    25   15    4    4    0    0    0    0    0
    26   15    3    3    1    0    0    0    0
    27    1    1    1    0    0    0    0    0
    28    1    1    1    0    0    0    0    0
    29    1    1    1    0    0    0    0    0
    30    6    3    4    0    2    0    0    1
    31    1    1    1    0    0    0    0    0
    32    8    1   12    1    1    0    0    0
    33    1    1    1    0    0    0    0    0
    34    7    4    4    0    0    0    0    0
    35    8    1    1    1    1    0    0    0
    36    1    1    1    0    0    0    0    0
    37    6    3    4    0    2    1    0    1
    38    7    2    3    0    2    1    0    0
    39    7    3    3    1    1    1    0    1
    40    7    3    3    1    0    0    0    0
    41    6    3    4    0    1    0    0    0
    42    7    1    3    0    3    0    0    0
    43    7    3    3    1    0    0    0    0
    44   16    2    2    1    1    1    0    0
    45    7    3    4    0    2    0    0    0
    46    7    2    3    0    2    0    0    0
    47    7    1    2    0    2    0    0    0
    48    7    2    2    0    0    0    0    0
    49    8    3    3    0    0    0    0    0
    50    1    1    1    0    0    0    0    0
    51    8    2    3    0    2    0    0    0
    52    1    1    1    0    0    0    0    0
    53    7    2    4    0    2    0    1    0
    54    7    3    4    0    2    0    0    1
    55    7    3   34    0    1    0    0    0
    56    7    3   34    0    1    0    0    0
    57    6    3    4    0    2    0    0    1
    58    7    3    4    0    1    1    0    1
    59    8    2    2    1    1    1    0    0
    60    6    1    3    0    3    0    0    0
    61    7    2    4    0    3    0    1    0
    62    7    2    2    1    0    0    0    0
    63    6    3    4    0    2    1    0    1
    64    6    3    4    0    2    1    0    1
    65    7    2    3    0    2    1    0    0
    66    7    2    3    0    2    1    0    0
    67    7    3    4    0    2    0    0    1
    68    7    4    4    0    0    0    0    0
    69    7    3    4    0    1    1    0    0
    70    8    2    2    1    0    0    0    0
    71    1    1    1    0    0    0    0    0
    72   16    1    1    1    1    0    0    0
    73   16    3    3    0    0    0    0    0
    74   16    2    4    0    2    0    0    0
    75   15    2    3    0    2    0    0    1
    76    7    2    2    1    0    0    0    0
    77   17    4    4    0    0    0    0    0
    78    6    3    4    0    2    1    0    1
    79    7    2    3    0    2    1    0    0
    80    6    3    4    0    2    0    0    1
    81    7    3    4    0    1    1    0    1
    82    7    3    4    0    1    1    0    0
    87   26    0    0    0    0    0    0    0
    88   26    0    0    0    0    0    0    0
    89    9    0    0    0    0    0    0    0
    90   17    0    0    0    0    0    0    0
    91   35    0    0    0    0    0    0    0
    92    3    0    0    0    0    0    0    0
    93   11    0    0    0    0    0    0    0
    94   19    0    0    0    0    0    0    0
    95   30    0    0    0    0    0    0    0
    96   20    0    0    0    0    0    0    0
    97   29    0    0    0    0    0    0    0
    98   29    0    0    0    0    0    0    0
    99   12    0    0    0    0    0    0    0

    */

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
    return (type == 70 || type > 82 ? type : level == 2 ? 0 : 
      equivalentTypes[((type - 1) << 1) + level]);
  }
}
