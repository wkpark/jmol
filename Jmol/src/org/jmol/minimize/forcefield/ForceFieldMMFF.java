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
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.util.Logger;

public class ForceFieldMMFF {

  private class AtomType {
    String smartsCode;
    int mmType;
    int elemNo;
    String descr;
    AtomType(int mmType, int elemNo, String descr, String smartsCode) {
      System.out.println("MMFF94\n" + mmType + "\n" + elemNo + "\n" + smartsCode);
      this.mmType = mmType;
      this.elemNo = elemNo;
      this.smartsCode = smartsCode;
      this.descr = descr;
    }
  }
  
  public ForceFieldMMFF() {
    getParameters();
  }
  private static List<AtomType> atomTypes;
  private static Map<Integer, Float> bciData;
  
  private void getParameters() {
    if (atomTypes != null)
      return;
    getAtomTypes("MMFF94-smarts.txt");
    Hashtable<Integer, Float> data = new Hashtable<Integer, Float>();
    getBciData("mmffpbci.par.txt", true, data);
    getBciData("mmffchg.par.txt", false, data);
    bciData = data;
  }

  private void getBciData(String fileName, boolean isPartial, Hashtable<Integer, Float> data) {    
    URL url = null;
    String line = null;
    try {
      if ((url = this.getClass().getResource(fileName)) == null) {
        System.err.println("Couldn't find file: " + fileName);
        throw new NullPointerException();
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      while ((line = br.readLine()) != null) {
        if (line.length() < 5 || line.startsWith("*"))
          continue;
        int a1 = Integer.valueOf(line.substring(3,5).trim()).intValue();
        int a2 = (isPartial ? 0 : Integer.valueOf(line.substring(8,10).trim()).intValue());
        Float value = Float.valueOf((isPartial ? line.substring(5,15) : line.substring(10,20)).trim());
        data.put(Integer.valueOf(a1 * 100 + a2), value);
        System.out.println(a1 + "\t" + a2 + "\t" + value);
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
      types.add(new AtomType(0, 0, "NOT FOUND", ""));
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.endsWith(".") || line.startsWith("#"))
          continue;
        types.add(new AtomType(Integer.valueOf(line.substring(0,2).trim()).intValue(),
            Integer.valueOf(line.substring(4,6).trim()).intValue(), 
            line.substring(6,30).trim(), line.substring(31).trim()));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName + " line=" + line);

    }
    Logger.info(types.size() + " force field parameters read");
    atomTypes = types;

  }

  private static int[] getTypes(Atom[] atoms, BitSet bsAtoms, SmilesMatcherInterface smartsMatcher) {
    BitSet[] bitSets = new BitSet[atomTypes.size()];
    String[] smarts = new String[atomTypes.size()];
    int[] types = new int[atoms.length];
    BitSet elements = new BitSet();
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      elements.set(atoms[i].getElementNumber());
    for (int i = atomTypes.size(); -- i >= 1;) {
      AtomType at = atomTypes.get(i);
      if (!elements.get(at.elemNo))
        continue;
      smarts[i] = at.smartsCode;      
    }
    smartsMatcher.getSubstructureSets(smarts, atoms, atoms.length, bsAtoms, bitSets);
    BitSet bsDone = new BitSet();
    for (int j = 0; j < bitSets.length; j++) {
      BitSet bs = bitSets[j];
      if (bs == null)
        continue;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (bsDone.get(i))
          continue;
        types[i] = j;//atomTypes.get(j).mmType;
      }
    }
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      System.out.println("atom " + atoms[i] + "\ttype " + atomTypes.get(types[i]).mmType + "\t" + atomTypes.get(types[i]).smartsCode + "\t"+ atomTypes.get(types[i]).descr);
    }
    
    return types;
  }
  
  public static float[] getPartialCharges(Atom[] atoms, BitSet bsAtoms, 
                                SmilesMatcherInterface smartsMatcher) {
    int[] types = getTypes(atoms, bsAtoms, smartsMatcher);
    float[] charges = new float[bsAtoms.cardinality()];
    for (int i = bsAtoms.nextSetBit(0), pt = 0; i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      int type1 = atomTypes.get(types[i]).mmType;
      Atom atom = atoms[i];
      Bond[] bonds = atom.getBonds();
      float q0a = atom.getFormalCharge();
      float q0b = 0;
      float wab = 0;
      float pa = Float.MAX_VALUE;
      float factor = 0;
      switch (type1) {
      case 32:
        // 32  OXYGEN IN CARBOXYLATE ANION
        // 32  NITRATE ANION OXYGEN
        // 32  SINGLE TERMINAL OXYGEN ON TETRACOORD SULFUR
        // 32  TERMINAL O-S IN SULFONES AND SULFONAMIDES
        // 32  TERMINAL O IN SULFONATES
      case 35:
        // 35  OXIDE OXYGEN ON SP2 CARBON, NEGATIVELY CHARGED
      case 72:
        // 72  TERMINAL SULFUR BONDED TO PHOSPHORUS
        factor = 0.5f;
        break;
      case 62:
        // 62  DEPROTONATED SULFONAMIDE N-; FORMAL CHARGE=-1
      case 76:
        // 76  NEGATIVELY CHARGED N IN, E.G, TRI- OR TETRAZOLE ANION
        factor = 0.25f;
        break;
      }
      int nBonds = 0;
      for (int j = bonds.length; --j >= 0; ) {
        if (!bonds[j].isCovalent())
          continue;
        nBonds++;
        Atom atom2 = bonds[j].getOtherAtom(atom);
        q0b += atom2.getFormalCharge();
        int type2 = atomTypes.get(types[atom2.index]).mmType;
        Float value;
        float bFactor;
        if (type1 < type2) {
          bFactor = -1;
          value = bciData.get(Integer.valueOf(type1 * 100 + type2));
        } else {
          value = bciData.get(Integer.valueOf(type2 * 100 + type1));
          bFactor = 1;
        }
        if (value == null) {
          if (pa == Float.MAX_VALUE)
            pa = bciData.get(Integer.valueOf(type1 * 100)).floatValue();
          float pb = bciData.get(Integer.valueOf(type2 * 100)).floatValue();
          wab += pa - pb; 
        } else {
          wab += bFactor * value.floatValue();
        }
      }
      if (factor == 0)
        charges[pt++] = q0a + wab;
      else
        charges[pt++] = (1f - nBonds * factor) * q0a + factor * q0b + wab;
    }
    return charges;
  }
  
  /* forcefieldmmff94.cpp
   
  bool OBForceFieldMMFF94::SetPartialCharges()
  {
    vector<double> charges(_mol.NumAtoms()+1, 0);
    double M, Wab, factor, q0a, q0b, Pa, Pb;

    FOR_ATOMS_OF_MOL (atom, _mol) {
      int type = atoi(atom->GetType());

      switch (type) {
      case 32:
      case 35:
      case 72:
        factor = 0.5;
        break;
      case 62:
      case 76:
        factor = 0.25;
        break;
      default:
        factor = 0.0;
        break;
      }

      M = GetCrd(type);
      q0a = atom->GetPartialCharge();

      // charge sharing
      if (!factor)
        FOR_NBORS_OF_ATOM (nbr, &*atom)
          if (nbr->GetPartialCharge() < 0.0)
            q0a += nbr->GetPartialCharge() / (2.0 * (double)(nbr->GetValence()));

      // needed for SEYWUO, positive charge sharing?
      if (type == 62)
        FOR_NBORS_OF_ATOM (nbr, &*atom)
          if (nbr->GetPartialCharge() > 0.0)
            q0a -= nbr->GetPartialCharge() / 2.0;

      q0b = 0.0;
      Wab = 0.0;
      Pa = Pb = 0.0;
      FOR_NBORS_OF_ATOM (nbr, &*atom) {
        int nbr_type = atoi(nbr->GetType());

        q0b += nbr->GetPartialCharge();

        bool bci_found = false;
        for (unsigned int idx=0; idx < _ffchgparams.size(); idx++)
          if (GetBondType(&*atom, &*nbr) == _ffchgparams[idx]._ipar[0]) {
            if ((type == _ffchgparams[idx].a) && (nbr_type == _ffchgparams[idx].b)) {
              Wab += -_ffchgparams[idx]._dpar[0];
              bci_found = true;
            } else if  ((type == _ffchgparams[idx].b) && (nbr_type == _ffchgparams[idx].a)) {
              Wab += _ffchgparams[idx]._dpar[0];
              bci_found = true;
            }
          }

        if (!bci_found) {
          for (unsigned int idx=0; idx < _ffpbciparams.size(); idx++) {
            if (type == _ffpbciparams[idx].a)
              Pa = _ffpbciparams[idx]._dpar[0];
            if (nbr_type == _ffpbciparams[idx].a)
              Pb = _ffpbciparams[idx]._dpar[0];
          }
          Wab += Pa - Pb;
        }
      }
      if (factor)
        charges[atom->GetIdx()] = (1.0 - M * factor) * q0a + factor * q0b + Wab;
      else
        charges[atom->GetIdx()] = q0a + Wab;
    }

    FOR_ATOMS_OF_MOL (atom, _mol)
      atom->SetPartialCharge(charges[atom->GetIdx()]);

    PrintPartialCharges();

    return true;
  }

   */
  
  /*  cdk charges/MMFF94PartialCharges.java
   
  public IAtomContainer assignMMFF94PartialCharges(IAtomContainer ac) throws Exception {
    ForceFieldConfigurator ffc = new ForceFieldConfigurator();
    ffc.setForceFieldConfigurator("mmff94");
    ffc.assignAtomTyps((IMolecule)ac);
    Map<String,Object> parameterSet = ffc.getParameterSet();
    // for this calculation,
    // we need some values stored in the vector "data" in the
    // hashtable of these atomTypes:    
    double charge = 0;
    double formalCharge = 0;
    double formalChargeNeigh = 0;
    double theta = 0;
    double sumOfFormalCharges = 0;
    double sumOfBondIncrements = 0;
    org.openscience.cdk.interfaces.IAtom thisAtom = null;
    List<IAtom> neighboors;
    Object data = null;
    Object bondData = null;
    Object dataNeigh = null;
    java.util.Iterator<IAtom> atoms = ac.atoms().iterator();
    while(atoms.hasNext()) {
      //logger.debug("ATOM "+i+ " " +atoms[i].getSymbol());
      thisAtom = atoms.next();
      data = parameterSet.get("data"+thisAtom.getAtomTypeName());
      neighboors = ac.getConnectedAtomsList(thisAtom);
      formalCharge = thisAtom.getCharge();
      theta = (Double)((List)data).get(5);
      charge = formalCharge * (1 - (neighboors.size() * theta));
      sumOfFormalCharges = 0;
      sumOfBondIncrements = 0;
            for (IAtom neighboor : neighboors) {
                IAtom neighbour = (IAtom) neighboor;
                dataNeigh = parameterSet.get("data" + neighbour.getAtomTypeName());
                if (parameterSet.containsKey("bond" + thisAtom.getAtomTypeName() + ";" + neighbour.getAtomTypeName())) {
                    bondData = parameterSet.get("bond" + thisAtom.getAtomTypeName() + ";" + neighbour.getAtomTypeName());
                    sumOfBondIncrements -= (Double) ((List)bondData).get(4);
                } else
                if (parameterSet.containsKey("bond" + neighbour.getAtomTypeName() + ";" + thisAtom.getAtomTypeName())) {
                    bondData = parameterSet.get("bond" + neighbour.getAtomTypeName() + ";" + thisAtom.getAtomTypeName());
                    sumOfBondIncrements += (Double) ((List)bondData).get(4);
                } else {
                    // Maybe not all bonds have pbci in mmff94.prm, i.e. C-N
                    sumOfBondIncrements += (theta - (Double) ((List)dataNeigh).get(5));
                }


                dataNeigh = parameterSet.get("data" + neighbour.getID());
                formalChargeNeigh = neighbour.getCharge();
                sumOfFormalCharges += formalChargeNeigh;
            }
            charge += sumOfFormalCharges * theta;
      charge += sumOfBondIncrements;
      thisAtom.setProperty("MMFF94charge", charge);
      //logger.debug( "CHARGE :"+thisAtom.getProperty("MMFF94charge") );
    }
    return ac;
  }

   */
}
