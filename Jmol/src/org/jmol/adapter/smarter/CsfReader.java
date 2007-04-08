/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.adapter.smarter;


import java.io.BufferedReader;
import java.util.Hashtable;

import org.jmol.quantum.MopacData;
import org.jmol.util.Logger;

/**
 * CSF file reader based on CIF idea -- fluid property fields.
 *
 * note that, like CIF, the order of fields is totally unpredictable
 * in addition, ID numbers are not sequential, requiring atomNames
 * 
 * first crack at this 2006/04/13
 * 
 * @author hansonr <hansonr@stolaf.edu>
 */
class CsfReader extends MopacDataReader {

  int nAtoms = 0;
  String atomicNumbers = "";
  
  AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("csf");
    try {
      readLine();
      while (line != null) {
        if (line.startsWith("object_class")) {
          //System.out.println(line);
          processObjectClass();
          // there is already an unprocessed line in the firing chamber
          continue; 
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }
  private void processObjectClass() throws Exception {
    if (line.equals("object_class connector")) {
      processConnectorObject();
      return;
    }
    if (line.equals("object_class atom")) {
      processAtomObject();
      return;
    }
    if (line.equals("object_class bond")) {
      processBondObject();
      return;
    }
    if (line.equals("object_class vibrational_level")) {
      processVibrationObject();
      return;
    }
    if (line.equals("object_class mol_orbital")) {
      processMolecularOrbitalObject();
      return;
    }
    if (line.equals("object_class sto_basis_fxn")) {
      processSlaterBasisObject();
      return;
    }
    
    readLine();
  }
  
  private Hashtable propertyItemCounts = new Hashtable();
  int[] fieldTypes = new int[100]; // should be enough
  int nFields = 0;
  
  int getPropertyCount(String what) {
    Integer count = (Integer)(propertyItemCounts.get(what));
    return (what.equals("ID") ? 1 : count == null ? 0 : count.intValue());
  }
  
  private int parseLineParameters(String[] fields,
                          byte[] fieldMap) throws Exception {
    
    String[] tokens = new String[0];
    //property xyz_coordinates Linus angstrom 6 3 FLOAT

    while (line != null) {
      tokens = getTokens();
      if (line.indexOf("property ") == 0)
        propertyItemCounts.put(tokens[1], new Integer((tokens[6].equals("STRING") ? 1 : parseInt(tokens[5]))));
      else if (line.indexOf("ID") == 0)
        break;
      readLine();
    }
    // ID line:
    String field;
    int fieldCount = -1;
    for (int i = 0; i < nFields; i++)
      fieldTypes[i] = 0;
    for (int ipt = 0, fpt = 0; ipt < tokens.length; ipt++ ) {
      field = tokens[ipt];
      for (int i = fields.length; --i >= 0; )
        if (field.equals(fields[i])) {
          fieldTypes[fpt] = fieldMap[i];
          //System.out.println(fpt + " " + fields[i] + " " + fieldMap[i] + " " + getPropertyCount(fields[i]));
          fieldCount = ipt + 1;
          break;
        }
      fpt += getPropertyCount(field);
      nFields = fpt;
    }
    return fieldCount;
  }

  ////////////////////////////////////////////////////////////////
  // connector data
  ////////////////////////////////////////////////////////////////

  final static byte conID   = 1;
  final static byte objCls1 = 2;
  final static byte objID1  = 3;
  final static byte objCls2 = 4;
  final static byte objID2  = 5;
  
  final static byte CONNECTOR_PROPERTY_MAX      = 6;

  final static String[] connectorFields = {
    "ID", "objCls1", "objID1", "objCls2", "objID2"
  };

  final static byte[] connectorFieldMap = {
    conID, objCls1, objID1, objCls2, objID2
  };
  
  Hashtable connectors = new Hashtable();
  
  void processConnectorObject() throws Exception {
    int fieldCount = parseLineParameters(connectorFields, connectorFieldMap);
    out: for (; readLine() != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String thisAtomID = null;
      String thisBondID = null;
      String tokens[] = getTokens();
      String field2 = "";
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
        case conID:
          break;
        case objCls1:
          if (!field.equals("atom"))
            continue out;
          break;
        case objCls2:
          field2 = field;
          if (field.equals("sto_basis_fxn"))
            nOrbitals++;
          else if (!field.equals("bond") && !field.equals("gto_basis_fxn")) 
            continue out;
          break;
        case objID1:
          thisAtomID = "atom"+field;
          break;
        case objID2:
          thisBondID = field2+field;
          break;
        default:
        }
      }
      if (thisAtomID != null && thisBondID != null) {
        if (connectors.containsKey(thisBondID)) {
          String[] connect = (String[])connectors.get(thisBondID);
          connect[1] = thisAtomID;
          //connectors.put(thisBondID, connect);
        } else {
          String[] connect = new String[2];
          connect[0] = thisAtomID;
          connectors.put(thisBondID, connect);
        }
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // atom data
  ////////////////////////////////////////////////////////////////

  final static byte NONE           = 0;
  final static byte atomID         = 1;
  final static byte sym            = 2;
  final static byte anum           = 3;
  final static byte chrg           = 4;
  final static byte xyz_coordinates = 5;
  final static byte pchrg           = 6;
  final static byte ATOM_PROPERTY_MAX = 7;
  

  final static String[] atomFields = {
    "ID",
    "sym", "anum",
    "chrg", "xyz_coordinates", "pchrg"
  };

  final static byte[] atomFieldMap = {
    atomID, sym, anum, chrg, xyz_coordinates, pchrg
  };

  void processAtomObject() throws Exception {
    nAtoms = 0;
    int fieldCount = parseLineParameters(atomFields, atomFieldMap);
    for (; readLine() != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens();
      Atom atom = new Atom();
      for (int i = 0; i < fieldCount; i++) {
        String field = tokens[i];
        if (field == null)
          Logger.warn("field == null in " + line);
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case atomID:
          atom.atomName = "atom"+field;
          break;
        case sym:
          atom.elementSymbol = field;
          break;
        case anum:
          atomicNumbers += field + " "; // for MO slater basis calc
          break;
        case chrg:
          atom.formalCharge = parseInt(field);
          break;
        case pchrg:
          atom.partialCharge = parseFloat(field);
          break;
        case xyz_coordinates:
          atom.x = parseFloat(field);
          atom.y = parseFloat(tokens[i + 1]);
          atom.z = parseFloat(tokens[i + 2]);
          break;
        }
      }
      if (Float.isNaN(atom.x) || Float.isNaN(atom.y) || Float.isNaN(atom.z)) {
        Logger.warn("atom " + atom.atomName + " has invalid/unknown coordinates");
      } else {
        nAtoms++;
        atomSetCollection.addAtomWithMappedName(atom);
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // bond order data
  ////////////////////////////////////////////////////////////////

  final static byte bondID = 1;
  final static byte bondType = 2;
  final static byte BOND_PROPERTY_MAX      = 3;

  final static String[] bondFields  = {
    "ID", "type"
  };

  final static byte[] bondFieldMap = {
    bondID, bondType
  };

  int nBonds = 0;
  
  void processBondObject() throws Exception {
    int fieldCount = parseLineParameters(bondFields,
                                         bondFieldMap);
    for (; readLine() != null; ) {
      if (line.startsWith("property_flags:"))
        break;
      String thisBondID = null;
      String tokens[] = getTokens();
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case bondID:
          thisBondID = "bond"+field;
          break;
        case bondType:
          int order = 1;
          if (field.equals("single"))
            order = 1;
          else if (field.equals("double"))
            order = 2;
          else if (field.equals("triple"))
            order = 3;
          else
            Logger.warn("unknown CSF bond order: " + field);
          String[] connect = (String[]) connectors.get(thisBondID);
          Bond bond = new Bond();
          bond.atomIndex1 = atomSetCollection.getAtomNameIndex(connect[0]);
          bond.atomIndex2 = atomSetCollection.getAtomNameIndex(connect[1]);
          bond.order=order;
          atomSetCollection.addBond(bond);
          nBonds++;
          break;
        }
      }
    }
  }

  
  final static byte vibID            = 1;
  final static byte normalMode       = 2;
  final static byte vibEnergy        = 3;
  final static byte transitionDipole = 4;
  final static byte lineWidth        = 5;
  final static byte VIB_PROPERTY_MAX = 6;

  final static String[] vibFields  = {
    "ID", "normalMode", "Energy", "transitionDipole"
  };

  final static byte[] vibFieldMap = {
    vibID, normalMode, vibEnergy, transitionDipole
  };

  void processVibrationObject() throws Exception {
    discardLinesUntilStartsWith("ID normalMode"); //a bit risky -- could miss it
    int thisvibID = -1;
    float[] vibXYZ = new float[3];
    int iatom = atomSetCollection.getFirstAtomSetAtomCount();
    int xyzpt = 0;
    Atom[] atoms = atomSetCollection.atoms;
    for (; readLine() != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens();
      if (parseInt(tokens[0]) != thisvibID) {
        thisvibID = parseInt(tokens[0]);
        atomSetCollection.cloneFirstAtomSetWithBonds(nBonds);
      }
      for (int i = 1; i < tokens.length; ++i) {
        vibXYZ[xyzpt++] = parseFloat(tokens[i]);
        if (xyzpt == 3) {
          atoms[iatom].addVibrationVector(vibXYZ[0], vibXYZ[1], vibXYZ[2]);
          iatom++;
          xyzpt = 0;
        }
      }
    }
    int fieldCount = parseLineParameters(vibFields, vibFieldMap);
    for (; readLine() != null;) {
      if (line.startsWith("property_flags:"))
        break;
      String tokens[] = getTokens();
      int thisvib = -1;
      for (int i = 0; i < fieldCount; ++i) {
        String field = tokens[i];
        switch (fieldTypes[i]) {
        case NONE:
          break;
        case vibID:
          thisvib = parseInt(field);
          break;
        case vibEnergy:
          atomSetCollection.setAtomSetName(field + " cm^-1", thisvib);
          atomSetCollection.setAtomSetProperty(SmarterJmolAdapter.PATH_KEY,
              "Frequencies");
          break;
        }
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // Molecular Orbitals
  ////////////////////////////////////////////////////////////////

  final static byte moID   = 1;
  final static byte eig_val = 2;
  final static byte mo_occ  = 3;
  final static byte eig_vec = 4;
  final static byte eig_vec_compressed = 5;
  final static byte coef_indices  = 6;
  final static byte bfxn_ang  = 7;
  final static byte sto_exp  = 8;
  final static byte contractions  = 9;
  final static byte MO_PROPERTY_MAX    = 10;

  final static String[] moFields = {
    "ID", "eig_val", "mo_occ", "eig_vec",
      "eig_vec_compressed", "coef_indices", "bfxn_ang", "sto_exp",
      "contractions"
  };

  final static byte[] moFieldMap = {
    moID, eig_val, mo_occ, eig_vec, eig_vec_compressed, coef_indices, bfxn_ang, sto_exp, contractions
  };
   
  void processMolecularOrbitalObject() throws Exception {
    if (nOrbitals == 0) {
      readLine();
      return; // no slaters;
    }
    Logger.info("Reading data for " + nOrbitals + " molecular orbitals");
    /* we read the following blocks in ANY order:
 
    ID dflag eig_val    mo_occ
     1   0x0 -36.825790 2.00000000
     2   0x0 -17.580715 2.00000000
     3   0x0 -14.523387 2.00000000
     4   0x0 -12.316568 2.00000000
     5   0x0   4.060438 0.00000000
     6   0x0   5.331586 0.00000000

    ID eig_vec_compressed                                          nom_coef 
     1 -0.845963 -0.179125 -0.179118 -0.067970  0.049666  0.000000        5
     2 -0.517325 -0.474120  0.474119 -0.377978  0.000000  0.000000        4
     3 -0.638505  0.466520  0.400882 -0.255125 -0.255118  0.000000        5
     4 -0.999990  0.000000  0.000000  0.000000  0.000000  0.000000        1
     5  0.582228  0.582125 -0.529468 -0.521559  0.386787 -0.004860        6
     6 -0.906355  0.906280 -0.753041 -0.550159  0.000000  0.000000        4
    
    ID coef_indices
     1  2 1 6 4 3 0
     2  3 6 1 4 0 0
     3  4 3 2 6 1 0
     4  5 0 0 0 0 0
     5  6 1 4 2 3 5
     6  1 6 3 4 0 0
    
    ID eig_vec
     1 -0.245163 -0.011925  0.000554  0.000542 -0.236038 -0.002974  0.006251
     1 -0.000460 -0.231155  0.003499  0.009902  0.000555 -0.236059  0.006221
     1  0.002910  0.001090 -0.245083  0.008801 -0.006892  0.004063 -0.264182
     1 -0.001313 -0.005736 -0.004526 -0.166087 -0.008065 -0.001462 -0.002563
     1 -0.166219  0.005699 -0.006460 -0.000149 -0.021764 -0.016402 -0.019220
     1 -0.014385 -0.022278 -0.016332 -0.019246 -0.021743 -0.023016 -0.018217
     1 -0.013078 -0.016269 -0.012006 -0.016322 -0.013100 -0.011989
     2  0.289501 -0.029400  0.007611 -0.002158  0.222093 -0.028271 -0.003105
     2 -0.004796 -0.000433 -0.025248  0.009182 -0.004274 -0.222016 -0.019921
     2  0.020485 -0.003535 -0.289379 -0.026658  0.012619 -0.007483  0.000107
     2 -0.046398  0.016755 -0.008013  0.351901  0.007737  0.007248 -0.001799
     2 -0.351606  0.000505 -0.010223  0.003301  0.024485  0.031084  0.019300
     2  0.000034 -0.000129 -0.031187 -0.019043 -0.024563  0.000012 -0.000014
     2  0.028784  0.031377  0.035556 -0.031461 -0.028669 -0.035517

     */

    float[] energy = new float[nOrbitals];
    int[] occupancy = new int[nOrbitals];
    float[][] list = new float[nOrbitals][nOrbitals];
    float[][] listCompressed = null;
    int[][] coefIndices = null;
    int ipt = 0;
    int coefPt = 0;
    int indexPt = 0;
    readLine();
    while (line != null) {
      if (line.startsWith("property_flags:"))
        readLine();
      if (line.startsWith("object_class"))
        break;
      int fieldCount = parseLineParameters(moFields, moFieldMap);
      int cPt = 0;
      while (readLine() != null) {
        if (line.startsWith("property_flags:"))
          break;
        String tokens[] = getTokens();
        for (int i = 0; i < fieldCount; ++i) {
          switch (fieldTypes[i]) {
          case moID:
            int id = parseInt(tokens[i]);
            if (id != ipt + 1) {
              cPt = indexPt = coefPt = 0;
              ipt = id - 1;
            }
            break;
          case eig_val:
            energy[ipt] = parseFloat(tokens[i]);
            break;
          case mo_occ:
            occupancy[ipt] = parseInt(tokens[i]);
            break;
          case eig_vec:
            for (int j = i; j < tokens.length; j++, cPt++)
              list[ipt][cPt] = parseFloat(tokens[j]);
            break;
          case eig_vec_compressed:
            if (listCompressed == null)
              listCompressed = new float[nOrbitals][nOrbitals];
            for (int j = i; j < tokens.length; j++, coefPt++)
              listCompressed[ipt][coefPt] = parseFloat(tokens[j]);
            break;
          case coef_indices:
            if (coefIndices == null)
              coefIndices = new int[nOrbitals][nOrbitals];
            for (int j = i; j < tokens.length; j++, indexPt++)
              coefIndices[ipt][indexPt] = parseInt(tokens[j]);
            break;
          }
        }
      }
    }
    //put it all together
    for (int iMo = 0; iMo < nOrbitals; iMo++) {
      if (indexPt > 0) { // must uncompress
        for (int i = 0; i < coefIndices[iMo].length; i++) {
          int pt = coefIndices[iMo][i] - 1;
          if (pt < 0)
            break;
          list[iMo][pt] = listCompressed[iMo][i];
        }
      }
      Hashtable mo = new Hashtable();
      mo.put("energy", new Float(energy[iMo]));
      mo.put("occupancy", new Integer(occupancy[iMo]));
      mo.put("coefficients", list[iMo]);
/*      
      System.out.print("MO " + iMo + " : ");
      for (int i = 0; i < nOrbitals; i++)
        System.out.print(" " + list[iMo][i]);
      System.out.println();
*/      
      orbitals.add(mo);
    }
    setMOs("eV");
  }
  
  void processSlaterBasisObject() throws Exception {
    String[] atomNos = getTokens(atomicNumbers);

    /*
     ID dflag bfxn_ang contr_len Nquant sto_exp  shell
     1   0x0        S         6      1 0.967807     1
     2   0x0        S         6      2 3.796544     2
     3   0x0       Px         6      2 2.389402     3
     4   0x0       Py         6      2 2.389402     3
     5   0x0       Pz         6      2 2.389402     3
     6   0x0        S         6      1 0.967807     4
     */

    readLine();
    while (line != null) {
      if (line.startsWith("property_flags:"))
        readLine();
      if (line.startsWith("object_class"))
        break;
      int fieldCount = parseLineParameters(moFields, moFieldMap);
      int iAtom = -1;
      String type = "";
      int nZetas = getPropertyCount("sto_exp");
      float[] zetas = new float[nZetas];
      float[] contractionCoefs = null;
      int atomicNumber = 0;

      while (readLine() != null) {
        if (line.startsWith("property_flags:"))
          break;
        String tokens[] = getTokens();
        for (int i = 0; i < fieldCount; ++i) {
          String field = tokens[i];
          switch (fieldTypes[i]) {
          case moID:
            iAtom = atomSetCollection.getAtomNameIndex(((String[]) (connectors
                .get("sto_basis_fxn" + field)))[0]);
            atomicNumber = parseInt(atomNos[iAtom]);
            break;
          case bfxn_ang:
            type = field;
            break;
          case sto_exp:
            for (int j = 0; j < nZetas; j++)
              zetas[j] = parseFloat(tokens[i + j]);
            break;
          case contractions:
            if (contractionCoefs == null)
              contractionCoefs = new float[nZetas];
            for (int j = 0; j < nZetas; j++)
              contractionCoefs[j] = parseFloat(tokens[i + j]);
          }
        }
        //System.out.println("orbitals for atom " + iAtom + " at.no. " + atomicNumber);
        for (int i = 0; i < nZetas; i++) {
          if (zetas[i] == 0)
            break;
          createSlaterByType(iAtom, atomicNumber, type, zetas[i]
              * (i == 0 ? 1 : -1), contractionCoefs == null ? 1
              : contractionCoefs[i]);
        }
      }
    }
    setSlaters();
  }  
  void createSlaterByType(int iAtom, int atomicNumber, String type, float zeta,
                          float coef) {
    int pt = "S Px Py Pz Dx2-y2 Dxz Dz2 Dyz Dxy".indexOf(type);
    //        0 2  5  8  11     18  22  26  30
    float absZeta = Math.abs(zeta);
    switch (pt) {
    case 0: //s
      addSlater(iAtom, 0, 0, 0, MopacData.getNPQs(atomicNumber) - 1, zeta,
          MopacData.getMopacConstS(atomicNumber, absZeta) * coef);
      return;
    case 2: //Px
    case 5: //Py
    case 8: //Pz
      addSlater(iAtom, pt == 2 ? 1 : 0, pt == 5 ? 1 : 0, pt == 8 ? 1 : 0,
          MopacData.getNPQp(atomicNumber) - 2, zeta, MopacData.getMopacConstP(
              atomicNumber, absZeta)
              * coef);
      return;
    case 11: //Dx2-y2
    case 18: //Dxz
    case 22: //Dz2
    case 26: //Dyz
    case 30: //Dxy
      int dPt = (pt == 11 ? 0 : pt == 18 ? 1 : pt == 22 ? 2 : pt == 26 ? 3 : 4);
      int dPt3 = dPt * 3;
      addSlater(iAtom, dValues[dPt3++], dValues[dPt3++], dValues[dPt3++],
          MopacData.getNPQd(atomicNumber) - 3, zeta, MopacData.getMopacConstD(
              atomicNumber, absZeta)
              * MopacData.getFactorD(dPt) * coef);
      return;
    }
  }
}
