/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-15 07:52:29 -0600 (Wed, 15 Mar 2006) $
 * $Revision: 4614 $
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

package org.jmol.adapter.readers.more;

import org.jmol.adapter.smarter.*;

import java.io.BufferedReader;

import org.jmol.api.JmolAdapter;

/**
 * A minimal multi-file reader for TRIPOS SYBYL mol2 files.
 *<p>
 * <a href='http://www.tripos.com/data/support/mol2.pdf '>
 * http://www.tripos.com/data/support/mol2.pdf 
 * </a>
 * 
 * PDB note:
 * 
 * Note that mol2 format of PDB files is quite minimal. All we
 * get is the PDB atom name, coordinates, residue number, and residue name
 * No chain terminator, not chain designator, no element symbol.
 * 
 * Chains based on numbering reset just labeled A B C D .... Z a b c d .... z
 * Element symbols based on reasoned guess and properties of hetero groups
 * 
 * So this is just a hack -- trying to guess at all of these.
 * 
 * 
 *<p>
 */

public class Mol2Reader extends AtomSetCollectionReader {

  private int nAtoms = 0;
  private int atomCount = 0;
  private boolean isPDB = false;

  public AtomSetCollection readAtomSetCollection(BufferedReader reader) {
    this.reader = reader;
    atomSetCollection = new AtomSetCollection("mol2");
    try {
      setFractionalCoordinates(false);
      readLine();
      modelNumber = 0;
      while (line != null) {
        if (line.equals("@<TRIPOS>MOLECULE")) {
          if (doGetModel(++modelNumber)) {
            processMolecule();
            if (isLastModel(modelNumber))
              break;
            continue;
          }
        }
        readLine();
      }
    } catch (Exception e) {
      return setError(e);
    }
    return atomSetCollection;
  }

  private void processMolecule() throws Exception {
    /* 4-6 lines:
     ZINC02211856
     55    58     0     0     0
     SMALL
     USER_CHARGES
     2-diethylamino-1-[2-(2-naphthyl)-4-quinolyl]-ethanol

     mol_name
     num_atoms [num_bonds [num_subst [num_feat [num_sets]]]]
     mol_type
     charge_type
     [status_bits
     [mol_comment]]

     */

    isPDB = false;
    String thisDataSetName = readLineTrimmed();
    lastSequenceNumber = Integer.MAX_VALUE;
    chainID = 'A' - 1;
    readLine();
    line += " 0 0 0 0 0 0";
    atomCount = parseInt(line);
    int bondCount = parseInt();
    int resCount = parseInt();
    readLine();//mol_type
    readLine();//charge_type
    boolean iHaveCharges = (line.indexOf("NO_CHARGES") != 0);
    //optional SYBYL status
    if (readLine() != null && (line.length() == 0 || line.charAt(0) != '@')) {
      //optional comment -- but present if comment is present
      if (readLine() != null && line.length() != 0 && line.charAt(0) != '@') {
        thisDataSetName += ": " + line.trim();
      }
    }
    newAtomSet(thisDataSetName);
    while (line != null && !line.equals("@<TRIPOS>MOLECULE")) {
      if (line.equals("@<TRIPOS>ATOM")) {
        readAtoms(atomCount, iHaveCharges);
        atomSetCollection.setAtomSetName(thisDataSetName);
      } else if (line.equals("@<TRIPOS>BOND")) {
        readBonds(bondCount);
      } else if (line.equals("@<TRIPOS>SUBSTRUCTURE")) {
        readResInfo(resCount);
      } else if (line.equals("@<TRIPOS>CRYSIN")) {
        readCrystalInfo();
      }
      readLine();
    }
    nAtoms += atomCount;
    if (isPDB) {
      atomSetCollection
          .setAtomSetCollectionAuxiliaryInfo("isPDB", Boolean.TRUE);
      atomSetCollection.setAtomSetAuxiliaryInfo("isPDB", Boolean.TRUE);
    }
    applySymmetry();
  }

  private int lastSequenceNumber = Integer.MAX_VALUE;
  private char chainID = 'A' - 1;
  //private final static String sybylTypes = " Any C.1 C.2 C.3 C.ar C.cat Co.oh Cr.oh Cr.th Cu Du Du.C Fe H.spc H.t3p Hal Het Hev LP N.1 N.2 N.3 N.4 N.am N.ar N.pl3 O.2 O.3 O.co2 O.spc O.t3p P.3 S.2 S.3 S.O S.O2 ";
  // see http://www.chem.cmu.edu/courses/09-560/docs/msi/ffbsim/B_AtomTypes.html last accessed 10/30/2008
  // x, X, and Xx symbols will always be themselves and so are not included in the lists below.
  // xn... Xn... or Xxn... where n is not a letter will likewise be handled without the list 
  // some duplication below...
  private final static String ffTypes = 
      /* AMBER   */ " CA CB CC CD CE CF CG CH CI CJ CK CM CN CP CQ CR CT CV CW HA HP HC HO HS HW LP NA NB NC NT OH OS OW SH AH BH HT HY AC BC CS OA OB OE OT "
    + /* CFF     */ " dw hc hi hn ho hp hs hw hscp htip ca cg ci cn co coh cp cr cs ct c3h c3m c4h c4m na nb nh nho nh+ ni nn np npc nr nt nz oc oe oh op oscp otip sc sh sp br cl ca+ ar si lp nu sz oz az pz ga ge tioc titd li+ na+ rb+ cs+ mg2+ ca2+ ba2+ cu2+ cl- br- so4 sy oy ay ayt nac+ mg2c fe2c mn4c mn3c co2c ni2c lic+ pd2+ ti4c sr2c ca2c cly- hocl py vy nh4+ so4y lioh naoh koh foh cloh beoh al "
    + /* CHARMM  */ " CE1 CF1 CF2 CF3 CG CD2 CH1E CH2E CH3E CM CP3 CPH1 CPH2 CQ66 CR55 CR56 CR66 CS66 CT CT3 CT4 CUA1 CUA2 CUA3 CUY1 CUY2 HA HC HMU HO HT LP NC NC2 NO2 NP NR1 NR2 NR3 NR55 NR56 NR66 NT NX OA OAC OC OE OH2 OK OM OS OSH OSI OT OW PO3 PO4 PT PUA1 PUY1 SE SH1E SK SO1 SO2 SO3 SO4 ST ST2 ST2 "
    + /* COMPASS */ " br br- br1 cl cl- cl1 cl12 cl13 cl14 cl1p ca+ cu+2 fe+2 mg+2 zn+2 cs+ li+ na+ rb+ al4z si si4 si4c si4z ar he kr ne xe "
    + /* ESFF    */ " dw hi hw ca cg ci co coh cp cr cs ct ct3 na nb nh nho ni no np nt nt2 nz oa oc oh op os ot sp bt cl' si4l si5l si5t si6 si6o si' "
    + /* GAFF    */ " br ca cc cd ce cf cl cp cq cu cv cx cy ha hc hn ho hp hs na nb nc nd nh oh os pb pc pd pe pf px py sh ss sx sy "
    + /* PCFF    */ " hn2 ho2 cz oo oz si sio hsi osi ";
  private final static String twoChar = " LP ca+ br cl ar si lp nu br- br1 cl- cl1 cl12 cl13 cl14 cl1p cu+2 fe+2 mg+2 zn+2 cs+ li+ na+ rb+ al4z si si4 si4c si4z he kr ne xe ga ge tioc titd li+ na+ rb+ cs+ mg2+ ca2+ ba2+ cu2+ nac+ mg2c fe2c mn4c mn3c co2c ni2c lic+ pd2+ ti4c sr2c ca2c cly- lioh naoh cloh beoh al Be+ Be+2 Li+ cl' Mg+ Mg+2 Na+ si4l si5l si5t si6 si6o si' sio ";  
  private final static String specialTypes = " sz az sy ay ayt ";
  private final static String secondCharOnly = " AH BH AC BC ";

  private void readAtoms(int atomCount, boolean iHaveCharges) throws Exception {
    //     1 Cs       0.0000   4.1230   0.0000   Cs        1 RES1   0.0000
    //  1 C1          7.0053   11.3096   -1.5429 C.3       1 <0>        -0.1912
    // free format, but no blank lines
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atomSetCollection.addNewAtom();
      String[] tokens = getTokens(readLine());
      //Logger.debug(tokens.length + " -" + tokens[5] + "- " + line);
      String atomType = tokens[5];
      atom.atomName = tokens[1] + '\0' + atomType;
      setAtomCoord(atom, parseFloat(tokens[2]), parseFloat(tokens[3]),
          parseFloat(tokens[4]));
      String elementSymbol = atomType;
      int nChar = elementSymbol.length();
      boolean deduceSymbol = (nChar > 1);
      if (deduceSymbol) {
        char ch0 = elementSymbol.charAt(0);
        char ch1 = elementSymbol.charAt(1);
        String check = " " + elementSymbol + " ";
        if (specialTypes.indexOf(check) >= 0) {
          elementSymbol = (ch0 == 's' ? "Si" : "Al");
        } else if (nChar == 2 && Character.isUpperCase(ch0) 
            && Character.isLowerCase(ch1)) {
          // Generic Xx
          deduceSymbol = false;
        } else if (Character.isLetter(ch0) && !Character.isLetter(ch1)) {
          // Xn... or xn...
          elementSymbol = "" + Character.toUpperCase(ch0);
          deduceSymbol = false;
        } else if (nChar > 2 && Character.isUpperCase(ch0)
            && Character.isLowerCase(ch1)
            && !Character.isLetter(elementSymbol.charAt(2))) {
          // Xxn.... (but not XXn... or xxn....)
          elementSymbol = "" + ch0 + ch1;
          deduceSymbol = false;
        } else {
          // must check list
          ch0 = Character.toUpperCase(ch0);
          deduceSymbol = (ffTypes.indexOf(check) < 0);
          if (deduceSymbol) {
            // not on the list
            elementSymbol = "" + ch0 + Character.toLowerCase(ch1);
          } else if (secondCharOnly.indexOf(check) >= 0) {
            // AH BH AC BC 
            elementSymbol = "" + ch1;
          } else if (twoChar.indexOf(check) >= 0) {
            // LP lp al cl12 etc.
            elementSymbol = "" + ch0 + ch1;
          } else {
            // all others 
            elementSymbol = "" + ch0;
          }
        }
      } else {
        // any single character
        elementSymbol = elementSymbol.toUpperCase();
      }
      atom.elementSymbol = elementSymbol;
      // apparently "NO_CHARGES" is not strictly enforced
      //      if (iHaveCharges)
      if (tokens.length > 6) {
        atom.sequenceNumber = parseInt(tokens[6]);
        if (atom.sequenceNumber < lastSequenceNumber) {
          if (chainID == 'Z')
            chainID = 'a' - 1;
          chainID++;
        }
        lastSequenceNumber = atom.sequenceNumber;
        atom.chainID = chainID;
      }
      if (tokens.length > 7) {
        atom.group3 = tokens[7];
        atom.isHetero = JmolAdapter.isHetero(atom.group3);
        if (!isPDB && atom.group3.length() <= 3
            && JmolAdapter.lookupGroupID(atom.group3) >= 0) {
          isPDB = true;
        }
        if (isPDB && deduceSymbol)
          atom.elementSymbol = deduceElementSymbol(atom.isHetero, atomType,
              atom.group3);
        //System.out.print(atom.atomName + "/" + atom.elementSymbol + " " );
      }
      if (tokens.length > 8)
        atom.partialCharge = parseFloat(tokens[8]);
    }
  }

  private void readBonds(int bondCount) throws Exception {
    //     6     1    42    1
    // free format, but no blank lines
    for (int i = 0; i < bondCount; ++i) {
      String[] tokens = getTokens(readLine());
      int atomIndex1 = parseInt(tokens[1]);
      int atomIndex2 = parseInt(tokens[2]);
      int order = parseInt(tokens[3]);
      if (order == Integer.MIN_VALUE)
        order = (tokens[3].equals("ar") ? JmolAdapter.ORDER_AROMATIC
            : JmolAdapter.ORDER_UNSPECIFIED);
      atomSetCollection.addBond(new Bond(nAtoms + atomIndex1 - 1, nAtoms
          + atomIndex2 - 1, order));
    }
  }

  private void readResInfo(int resCount) throws Exception {
    // free format, but no blank lines
    for (int i = 0; i < resCount; ++i) {
      readLine();
      //to be determined -- not implemented
    }
  }

  private void readCrystalInfo() throws Exception {
    //    4.1230    4.1230    4.1230   90.0000   90.0000   90.0000   221     1
    readLine();
    String[] tokens = getTokens();
    if (tokens.length < 6)
      return;
    for (int i = 0; i < 6; i++)
      setUnitCellItem(i, parseFloat(tokens[i]));
    String name = "";
    for (int i = 6; i < tokens.length; i++)
      name += " " + tokens[i];
    if (name == "")
      name = " P1";
    else
      name += " *";
    name = name.substring(1);
    setSpaceGroupName(name);
    Atom[] atoms = atomSetCollection.getAtoms();
    for (int i = 0; i < atomCount; ++i)
      setAtomCoord(atoms[nAtoms + i]);
  }
}
