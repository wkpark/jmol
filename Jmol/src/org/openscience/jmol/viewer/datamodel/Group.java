/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3f;

final public class Group {

  Frame frame;
  Chain chain;
  Polymer polymer;
  int seqcode;
  short groupID;
  ProteinStructure proteinstructure;

  // FIXME - mth 2004 05 17
  // these two arrays of indices need to be merged
  // one is for amino acid residues
  // the other is for nucleotide bases
  int[] mainchainIndices;

  int[] nucleotideIndices;
  int atomIndexNucleotidePhosphorus = -1;
  int atomIndexNucleotideWing = -1;
  int atomIndexRnaO2Prime = -1;
  int nucleicCount = 0;


  short aminoBackboneHbondOffset = 0;


  Group(Frame frame, Chain chain,
                  int sequenceNumber, char insertionCode, String group3) {
    this.frame = frame;
    this.chain = chain;
    this.seqcode = getSeqcode(sequenceNumber, insertionCode);
    if (group3 == null)
      group3 = "";
    this.groupID = getGroupID(group3);
  }

  void setPolymer(Polymer polymer) {
    this.polymer = polymer;
  }

  void setStructure(ProteinStructure proteinstructure) {
    this.proteinstructure = proteinstructure;
  }

  byte getProteinStructureType() {
    if (proteinstructure != null)
      return proteinstructure.type;
    if (hasNucleotidePhosphorus()) {
      if (atomIndexRnaO2Prime != -1)
        return JmolConstants.PROTEIN_STRUCTURE_RNA;
      return JmolConstants.PROTEIN_STRUCTURE_DNA;
    }
    return 0;
  }
  
  public boolean isGroup3(String group3) {
    return group3Names[groupID].equalsIgnoreCase(group3);
  }

  String getGroup3() {
    return group3Names[groupID];
  }

  public static String getGroup3(short groupID) {
    return group3Names[groupID];
  }

  int getSeqcode() {
    return seqcode;
  }

  String getSeqcodeString() {
    return getSeqcodeString(seqcode);
  }

  short getGroupID() {
    return groupID;
  }

  boolean isGroup3Match(String strWildcard) {
    int cchWildcard = strWildcard.length();
    int ichWildcard = 0;
    String group3 = group3Names[groupID];
    int cchGroup3 = group3.length();
    if (cchWildcard < cchGroup3)
      return false;
    while (cchWildcard > cchGroup3) {
      // wildcard is too long
      // so strip '?' from the beginning and the end, if possible
      if (strWildcard.charAt(ichWildcard) == '?') {
        ++ichWildcard;
      } else if (strWildcard.charAt(ichWildcard + cchWildcard - 1) != '?') {
        return false;
      }
      --cchWildcard;
    }
    for (int i = cchGroup3; --i >= 0; ) {
      char charWild = strWildcard.charAt(ichWildcard + i);
      if (charWild == '?')
        continue;
      if (charWild != group3.charAt(i))
        return false;
    }
    return true;
  }

  char getChainID() {
    return chain.chainID;
  }

  boolean isHelix() {
    return proteinstructure != null &&
      proteinstructure.type == JmolConstants.PROTEIN_STRUCTURE_HELIX;
  }

  boolean isHelixOrSheet() {
    return proteinstructure != null &&
      proteinstructure.type >= JmolConstants.PROTEIN_STRUCTURE_SHEET;
  }

  /****************************************************************
   * static stuff for group ids
   ****************************************************************/

  private static Hashtable htGroup = new Hashtable();

  static String[] group3Names = new String[128];
  static short group3NameCount = 0;

  static {
    for (int i = 0; i < JmolConstants.predefinedGroup3Names.length; ++i)
      addGroup3Name(JmolConstants.predefinedGroup3Names[i]);
  }

  synchronized static short addGroup3Name(String group3) {
    if (group3NameCount == group3Names.length)
      group3Names = Util.doubleLength(group3Names);
    short groupID = group3NameCount++;
    group3Names[groupID] = group3;
    htGroup.put(group3, new Short(groupID));
    return groupID;
  }

  static short getGroupID(String group3) {
    if (group3 == null)
      return -1;
    short groupID = lookupGroupID(group3);
    return (groupID != -1) ? groupID : addGroup3Name(group3);
  }

  public static short lookupGroupID(String group3) {
    if (group3 != null) {
      Short boxedGroupID = (Short)htGroup.get(group3);
      if (boxedGroupID != null)
        return boxedGroupID.shortValue();
    }
    return -1;
  }

  /*
  void assignAtom(Atom atom) {
    PdbAtom pdbAtom = new PdbAtom(this, atom.atomIndex, pdbRecord);
    if (pdbAtom.atomID < JmolConstants.ATOMID_MAINCHAIN_MAX) {
      if (! registerMainchainAtomIndex(pdbAtom.atomID, atom.atomIndex))
        pdbAtom.atomID += JmolConstants.ATOMID_MAINCHAIN_IMPOSTERS;
    }
    return pdbAtom;
  }
  */

  void registerAtom(Atom atom) {
    /*
    System.out.println("Group.registerAtom(atom) atom.atomID="+atom.atomID+
                       " atom.atomName=" + atom.atomName);
    */
    byte specialAtomID = atom.getSpecialAtomID();
    if (specialAtomID <= 0)
      return;
    if (specialAtomID < JmolConstants.ATOMID_DEFINING_PROTEIN_MAX) {
      if (! registerMainchainAtomIndex(specialAtomID, atom.atomIndex))
        atom.demoteSpecialAtomImposter();
      return;
    }
    /*
    if (specialAtomID < JmolConstants.ATOMID_DEFINING_NUCLEOTIDE_MAX) {
      registerNucleicAtomIndex(specialAtomID, atom.atomIndex);
      return;
    }
    */
  }

  boolean registerMainchainAtomIndex(short atomid, int atomIndex) {
    if (mainchainIndices == null) {
      mainchainIndices = new int[4];
      for (int i = 4; --i >= 0; )
        mainchainIndices[i] = -1;
    }
    if (mainchainIndices[atomid - 1] != -1) {
      // my residue already has a mainchain atom with this atomid
      // I must be an imposter
      return false;
    }
    mainchainIndices[atomid - 1] = atomIndex;
    return true;
  }

  Atom getLeadAtom() {
    if (hasNucleotidePhosphorus())
      return getNucleotidePhosphorusAtom();
    return getAlphaCarbonAtom();
    
  }

  int getLeadAtomIndex() {
    if (hasNucleotidePhosphorus())
      return atomIndexNucleotidePhosphorus;
    return getAlphaCarbonIndex();
  }

  Atom getWingAtom() {
    if (hasNucleotidePhosphorus())
      return getNucleotideWingAtom();
    return getCarbonylOxygenAtom();
  }

  int getWingAtomIndex() {
    if (hasNucleotidePhosphorus())
      return atomIndexNucleotideWing;
    return getAlphaCarbonIndex();
  }

  int getAlphaCarbonIndex() {
    if (mainchainIndices == null)
      return -1;
    return mainchainIndices[1];
  }

  int getCarbonylOxygenIndex() {
    if (mainchainIndices == null)
      return -1;
    return mainchainIndices[3];
  }

  Atom getMainchainAtom(int i) {
    int j;
    if (mainchainIndices == null || (j = mainchainIndices[i]) == -1) {
      System.out.println("I am a mainchain atom returning null because " +
                         " mainchainIndices==null? " +
                         (mainchainIndices == null));
      System.out.println("chain '" + chain.chainID + "'");
      System.out.println("group3=" + getGroup3());
      System.out.println("sequence=" + getSeqcodeString());
      return null;
    }
    return getAtomIndex(j);
  }

  Atom getNitrogenAtom() {
    return getMainchainAtom(0);
  }

  Atom getAlphaCarbonAtom() {
    return getMainchainAtom(1);
  }

  Atom getCarbonylCarbonAtom() {
    return getMainchainAtom(2);
  }

  Atom getCarbonylOxygenAtom() {
    return getMainchainAtom(3);
  }

  Point3f getAlphaCarbonPoint() {
    return getMainchainAtom(1).point3f;
  }

  boolean hasAlphaCarbon() {
    return (mainchainIndices != null) && (mainchainIndices[1] != -1);
  }

  boolean hasFullMainchain() {
    if (mainchainIndices == null)
      return false;
    for (int i = 4; --i >= 0; )
      if (mainchainIndices[i] == -1)
        return false;
    return true;
  }

  public static int getSeqcode(int sequenceNumber, char insertionCode) {
    if (sequenceNumber == Integer.MIN_VALUE)
      return sequenceNumber;
    if (insertionCode >= 'a' && insertionCode <= 'z')
      insertionCode -= 'a' - 'A';
    else if (insertionCode < 'A' || insertionCode > 'Z')
      insertionCode = '\0';
    return (sequenceNumber << 8) + insertionCode;
  }

  public static String getSeqcodeString(int seqcode) {
    if (seqcode == Integer.MIN_VALUE)
      return null;
    return (seqcode & 0xFF) == 0
      ? "" + (seqcode >> 8)
      : "" + (seqcode >> 8) + '^' + (char)(seqcode & 0xFF);
  }

  boolean isProline() {
    // this is the index into JmolConstants.predefinedGroup3Names
    return groupID == JmolConstants.GROUPID_PROLINE;
  }

  public void selectAtoms(BitSet bs) {
    Frame frame = chain.model.mmset.frame;
    Atom[] atoms = frame.getAtoms();
    for (int i = frame.getAtomCount(); --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom.getGroup() == this)
        bs.set(i);
    }
  }

  /*
  void registerNucleicAtomIndex(short atomid, int atomIndex) {
    if (atomid == JmolConstants.ATOMID_NUCLEOTIDE_PHOSPHORUS) {
      if (atomIndexNucleotidePhosphorus < 0) {
        ++nucleicCount;
        atomIndexNucleotidePhosphorus = atomIndex;
      }
      return;
    }
    if (atomid == JmolConstants.ATOMID_NUCLEOTIDE_WING) {
      if (atomIndexNucleotideWing < 0) {
        ++nucleicCount;
        atomIndexNucleotideWing = atomIndex;
      }
      return;
    }
    if (atomid == JmolConstants.ATOMID_RNA_O2PRIME)
      atomIndexRnaO2Prime = atomIndex;
    if (atomid >= JmolConstants.ATOMID_NUCLEOTIDE_MIN &&
        atomid < JmolConstants.ATOMID_NUCLEOTIDE_MAX) {
      if (nucleotideIndices == null)
        allocateNucleotideIndices();
      nucleotideIndices[atomid -
                        JmolConstants.ATOMID_NUCLEOTIDE_MIN] =
        atomIndex;
    }
    ++nucleicCount;
  }


  void allocateNucleotideIndices() {
    nucleotideIndices =
      new int[JmolConstants.ATOMID_NUCLEOTIDE_MAX -
              JmolConstants.ATOMID_NUCLEOTIDE_MIN];
    for (int i = nucleotideIndices.length; --i >= 0; )
      nucleotideIndices[i] = -1;
  }
  */

  Atom getNucleotidePhosphorusAtom() {
    return getAtomIndex(atomIndexNucleotidePhosphorus);
  }

  Atom getNucleotideWingAtom() {
    if (atomIndexNucleotidePhosphorus < 0)
      return null;
    return getAtomIndex(atomIndexNucleotideWing);
  }

  boolean hasNucleotidePhosphorus() {
    return (atomIndexNucleotidePhosphorus >= 0 &&
            atomIndexNucleotideWing >= 0 &&
            nucleicCount > 5);
  }

  Atom getNucleotideAtomID(int atomid) {
    return null;
    /*
    if (atomid < JmolConstants.ATOMID_NUCLEOTIDE_MIN ||
        atomid >= JmolConstants.ATOMID_NUCLEOTIDE_MAX) {
      System.out.println("getNucleotideAtomID out of bounds");
      return null;
    }
    int index = atomid - JmolConstants.ATOMID_NUCLEOTIDE_MIN;
    Atom atom = getAtomIndex(nucleotideIndices[index]);
    if (atom == null)
      System.out.println("getNucleotideAtomID(" + atomid + ") -> null ?" +
                         " nucleotideIndices[" + index + "]=" +
                         nucleotideIndices[index]);
    return atom;
    */
  }

  Atom getAtomIndex(int atomIndex) {
    return (atomIndex < 0
            ? null
            : chain.model.mmset.frame.getAtomAt(atomIndex));
  }

  Atom getPurineN1() {
    Atom n1 = ((groupID >= JmolConstants.GROUPID_PURINE_MIN &&
                groupID <= JmolConstants.GROUPID_PURINE_LAST)
               ? getNucleotideAtomID(JmolConstants.ATOMID_N1)
               : null);
    return n1;
  }

  Atom getPyrimidineN3() {
    return ((groupID >= JmolConstants.GROUPID_PYRIMIDINE_MIN &&
             groupID <= JmolConstants.GROUPID_PYRIMIDINE_LAST)
            ? getNucleotideAtomID(JmolConstants.ATOMID_N3)
            : null);
  }
            
  boolean isGuanine() {
    //    "@g _g=25,_g=26,_g>=39 & _g<=45,_g>=54 & _g<=56",
    return (groupID == JmolConstants.GROUPID_GUANINE ||
            groupID == JmolConstants.GROUPID_PLUS_GUANINE ||
            (groupID >= JmolConstants.GROUPID_GUANINE_1_MIN &&
             groupID <= JmolConstants.GROUPID_GUANINE_1_LAST) ||
            (groupID >= JmolConstants.GROUPID_GUANINE_2_MIN &&
             groupID <= JmolConstants.GROUPID_GUANINE_2_LAST));
  }

  void dumpNucleotideIndices() {
    /*
    System.out.println("dumpNucleotideIndices(" + this + "," + groupID + ")");
    if (nucleotideIndices == null) {
      System.out.println("  nucleotideIndices=null");
    } else {
      System.out.println("  ");
      for (int i = 0; i < nucleotideIndices.length; ++i)
        System.out.print(
                         JmolConstants.specialAtomNames[JmolConstants.ATOMID_NUCLEOTIDE_MIN + i] + ":" + nucleotideIndices[i] + " ");
      System.out.println("\n");
    }
    */
  }

  public String toString() {
    return "[" + getGroup3() + "-" + getSeqcodeString() + "]";
  }

  ////////////////////////////////////////////////////////////////

  void setAminoBackboneHbondOffset(int aminoBackboneHbondOffset) {
    this.aminoBackboneHbondOffset = (short)aminoBackboneHbondOffset;
  }
  
  int getAminoBackboneHbondOffset() {
    return aminoBackboneHbondOffset;
  }
}
