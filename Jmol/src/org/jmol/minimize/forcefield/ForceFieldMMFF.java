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
import org.jmol.util.BitSetUtil;
import org.jmol.util.JmolEdge;
import org.jmol.util.Logger;

public class ForceFieldMMFF {

  private class AtomType {
    int mmType;
    int elemNo;
    float formalCharge;
    float fcadj;
    boolean sbmb;
    boolean arom;
    String descr;
    String smartsCode;
    AtomType(int elemNo, int mmType, float formalCharge, String descr, String smartsCode) {
      this.mmType = mmType;
      this.elemNo = elemNo;
      this.smartsCode = smartsCode;
      this.descr = descr;
      this.formalCharge = formalCharge;
      setFlags();
    }
    private void setFlags() {
      switch (mmType) {
      
      // Note that these are NOT fractional charges based on
      // number of connected atoms. These are relatively arbitrary
      // fractions of the formal charge to be shared with other atoms.
      // That is, it is not significant that 0.5 is 1/2, and 0.25 is 1/4; 
      // they are just numbers.
      
      case 32:
      case 35:
      case 72:
        // 32  OXYGEN IN CARBOXYLATE ANION
        // 32  NITRATE ANION OXYGEN
        // 32  SINGLE TERMINAL OXYGEN ON TETRACOORD SULFUR
        // 32  TERMINAL O-S IN SULFONES AND SULFONAMIDES
        // 32  TERMINAL O IN SULFONATES
        // 35  OXIDE OXYGEN ON SP2 CARBON, NEGATIVELY CHARGED
        // 72  TERMINAL SULFUR BONDED TO PHOSPHORUS
        fcadj = 0.5f;
        break;
      case 62:
      case 76:
        // 62  DEPROTONATED SULFONAMIDE N-; FORMAL CHARGE=-1
        // 76  NEGATIVELY CHARGED N IN, E.G, TRI- OR TETRAZOLE ANION
        fcadj = 0.25f;
        break;
      }
      switch (mmType) {
      case 37:
      case 38:
      case 39:
      case 44:
      case 58:
      case 59:
      case 63:
      case 64:
      case 65:
      case 66:
      case 69:
      case 78:
      case 79:
      case 81:
      case 82:
        arom = true;
      }
      switch (mmType) {
      case 2:
      case 3:
      case 4:
      case 9:
      case 30:
      case 37:
      case 39:
      case 54:
      case 57:
      case 58:
      case 63:
      case 64:
      case 67:
      case 75:
      case 78:
      case 80:
      case 81:
        sbmb = true;
      }
    }
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
        int bondType = line.charAt(0) - '0';
        int a1 = Integer.valueOf(line.substring(3,5).trim()).intValue();
        int a2 = (isPartial ? 0 : Integer.valueOf(line.substring(8,10).trim()).intValue());
        Float value = Float.valueOf((isPartial ? line.substring(5,15) : line.substring(10,20)).trim());
        Integer key = Integer.valueOf((a1 * 100 + a2) * 10 + bondType);
        data.put(key, value);
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
      types.add(new AtomType(0, 0, Float.NaN, "NOT FOUND", ""));
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.startsWith("#"))
          continue;
        //0         1         2         3         4         5         6
        //0123456789012345678901234567890123456789012345678901234567890123456789
        //O   8 32  -4 NITRATE ANION OXYGEN      $([OD1][ND3]([OD1])[OD1])
        int elemNo = Integer.valueOf(line.substring(3,5).trim()).intValue();
        int mmType = Integer.valueOf(line.substring(6,8).trim()).intValue();
        float formalCharge = Float.valueOf(line.substring(9,12).trim()).floatValue()/12;
        String desc = line.substring(13,38).trim();
        String smarts = line.substring(39).trim();
        types.add(new AtomType(elemNo, mmType, formalCharge, desc, smarts));
      }
      br.close();
    } catch (Exception e) {
      System.err.println("Exception " + e.getMessage() + " in getResource "
          + fileName + " line=" + line);

    }
    Logger.info(types.size() + " SMARTS-based atom type read");
    atomTypes = types;

  }

  public static int[] getTypes(Atom[] atoms, BitSet bsAtoms, 
                               SmilesMatcherInterface smartsMatcher, 
                               List<BitSet> vAromatic56) {
    List<BitSet>bitSets = new ArrayList<BitSet>();
    String[] smarts = new String[atomTypes.size()];
    int[] types = new int[atoms.length];
    BitSet elements = new BitSet();
    BitSet bsConnected = BitSetUtil.copy(bsAtoms);
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Atom a = atoms[i];
      Bond[] bonds = a.getBonds();
      if (bonds != null)
        for (int j = bonds.length; --j >= 0;)
          if (bonds[j].isCovalent())
            bsConnected.set(j);
    }
    for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected.nextSetBit(i + 1))
      elements.set(atoms[i].getElementNumber());
    for (int i = atomTypes.size(); -- i >= 1;) {
      AtomType at = atomTypes.get(i);
      if (!elements.get(at.elemNo))
        continue;
      smarts[i] = at.smartsCode;      
    }
    smartsMatcher.getSubstructureSets(smarts, atoms, atoms.length, 
        JmolEdge.FLAG_AROMATIC_STRICT | JmolEdge.FLAG_AROMATIC_DOUBLE, 
        bsConnected, bitSets, vAromatic56);
    BitSet bsDone = new BitSet();
    for (int j = 0; j < bitSets.size(); j++) {
      BitSet bs = bitSets.get(j);
      if (bs == null)
        continue;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (bsDone.get(i))
          continue;
        types[i] = j;
        bsDone.set(i);
      }
    }
    if (Logger.debugging)
      for (int i = bsConnected.nextSetBit(0); i >= 0; i = bsConnected.nextSetBit(i + 1))
        Logger.info("atom " + atoms[i] + "\ttype " + atomTypes.get(types[i]).mmType + "\t" + atomTypes.get(types[i]).smartsCode + "\t"+ atomTypes.get(types[i]).descr);
    
    return types;
  }

  /**
   * assign partial charges ala MMFF94
   * 
   * @param bonds
   * @param atoms
   * @param types
   * @param bsAtoms
   * @param vAromatic56 
   * @return   full array of partial charges
   */
  public static float[] getPartialCharges(Bond[] bonds, Atom[] atoms,
                                          int[] types, BitSet bsAtoms, 
                                          List<BitSet> vAromatic56) {

    // start with formal charges specified by MMFF94 (not what is in file!)

    float[] partialCharges = new float[atoms.length];
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1))
      partialCharges[i] = atomTypes.get(types[i]).formalCharge;

    // run through all bonds, adjusting formal charges as necessary

    for (int i = bonds.length; --i >= 0;) {
      Atom a1 = bonds[i].getAtom1();
      Atom a2 = bonds[i].getAtom2();
      // It's possible that some of our atoms are not in the atom set,
      // but we don't want both of them to be out of the set.
      
      boolean ok1 = bsAtoms.get(a1.index);
      boolean ok2 = bsAtoms.get(a2.index); 
      if (!ok1 && !ok2)
        continue;
      AtomType at1 = atomTypes.get(types[a1.index]);
      AtomType at2 = atomTypes.get(types[a2.index]);
      int type1 = at1.mmType;
      int type2 = at2.mmType;
      
      // we are only interested in bonds that are between different atom types
      
//      if (type1 == type2)
  //      continue;
      
      // check for bond charge increment
      
      // The table is created using the key (100 * type1 + type2), 
      // where type1 < type2. In addition, we encode the partial bci values
      // with key (100 * type)
      
      float dq;  // the difference in charge to be added or subtracted from the formal charges
      try {
        int bondType = (isBondType1(at1, at2) && 
            bonds[i].getCovalentOrder() == 1 
            && !isAromaticBond(a1.index, a2.index, vAromatic56) ? 1 : 0);
        float bFactor = (type1 < type2 ? -1 : 1);
        Integer key = Integer.valueOf(((bFactor == 1 ? type2 * 100 + type1
            : type1 * 100 + type2) * 10) + bondType);
        Float bciValue = bciData.get(key);
        float bci;
        String msg = (Logger.debugging ? key + " " + a1 + "/" + a2 + " mmTypes=" + type1 + "/" + type2 + " formalCharges=" + at1.formalCharge + "/" + at2.formalCharge + " bci = " : null);
        if (bciValue == null) { 
          // no bci was found; we have to use partial bond charge increments
          // a failure here indicates we don't have information
          float pa = bciData.get(Integer.valueOf(type1 * 1000)).floatValue();
          float pb = bciData.get(Integer.valueOf(type2 * 1000)).floatValue();
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

  private static boolean isAromaticBond(int a1, int a2, List<BitSet> vAromatic56) {
    for (int i = vAromatic56.size(); --i >= 0;) {
      BitSet bsRing = vAromatic56.get(i);
      if (bsRing.get(a1) && bsRing.get(a2))
        return true;
    }
    return false;
  }

  private static boolean isBondType1(AtomType at1, AtomType at2) {
      return at1.sbmb && at2.sbmb || at1.arom && at2.arom; 
  }

  public static String[] getAtomTypeDescs(int[] types) {
    String[] stypes = new String[types.length];
    for (int i = types.length; --i >= 0;) {
      AtomType at = atomTypes.get(types[i]);
      stypes[i] = String.valueOf(at.mmType);
    }
    return stypes;
  }

  /*
   * chemkit -- Kyle Lutz 
   *  
  void MmffPartialChargeModel::setMolecule(const chemkit::Molecule *molecule)
  {
  
   ...
   
    // assign partial charges for each atom
    for(size_t i = 0; i < molecule->size(); i++){
        const chemkit::Atom *atom = molecule->atom(i);
        int atomType = typer->typeNumber(atom);

        const MmffAtomParameters *atomParameters = m_parameters->atomParameters(atomType);
        if(!atomParameters){
            continue;
        }

        chemkit::Real q0 = typer->formalCharge(atom);
        chemkit::Real M = atomParameters->crd;
        chemkit::Real V = m_parameters->partialChargeParameters(atomType)->fcadj;
        chemkit::Real formalChargeSum = 0;
        chemkit::Real partialChargeSum = 0;

        if(V == 0){
            foreach(const chemkit::Atom *neighbor, atom->neighbors()){
                chemkit::Real neighborFormalCharge = typer->formalCharge(neighbor);

                if(neighborFormalCharge < 0){
                    q0 += neighborFormalCharge / (2 * neighbor->neighborCount());
                }
            }
        }

        if(atomType == 62){
            foreach(const chemkit::Atom *neighbor, atom->neighbors()){
                chemkit::Real neighborFormalCharge = typer->formalCharge(neighbor);

                if(neighborFormalCharge > 0){
                    q0 -= neighborFormalCharge / 2;
                }
            }
        }

        foreach(const chemkit::Atom *neighbor, atom->neighbors()){
            int neighborType = typer->typeNumber(neighbor);

            const MmffChargeParameters *chargeParameters = m_parameters->chargeParameters(atom, atomType, neighbor, neighborType);
            if(chargeParameters){
                partialChargeSum += -chargeParameters->bci;
            }
            else{
                chargeParameters = m_parameters->chargeParameters(neighbor, neighborType, atom, atomType);
                if(chargeParameters){
                    partialChargeSum += chargeParameters->bci;
                }
                else{
                    const MmffPartialChargeParameters *partialChargeParameters = m_parameters->partialChargeParameters(atomType);
                    const MmffPartialChargeParameters *neighborPartialChargeParameters = m_parameters->partialChargeParameters(neighborType);
                    if(!partialChargeParameters || !neighborPartialChargeParameters){
                        continue;
                    }

                    partialChargeSum += (partialChargeParameters->pbci - neighborPartialChargeParameters->pbci);
                }
            }

            formalChargeSum += typer->formalCharge(neighbor);
        }

        // equation 15 (p. 662)
        m_partialCharges[i] = (1 - M * V) * q0 + V * formalChargeSum + partialChargeSum;
    }

    // cleanup typer object
    if(!m_typer){
        delete typer;
    }
  }

 */
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
