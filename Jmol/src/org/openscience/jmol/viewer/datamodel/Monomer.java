/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

abstract class Monomer extends Group {

  Polymer polymer;

  byte leadAtomOffset;
  byte wingAtomOffset = -1;

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

  int distinguishingBits;


  Monomer(Chain chain, String group3,
          int sequenceNumber, char insertionCode,
          int firstAtomIndex, int lastAtomIndex) {
    super(chain, group3, sequenceNumber, insertionCode,
          firstAtomIndex, lastAtomIndex);
  }

  void setPolymer(Polymer polymer) {
    this.polymer = polymer;
  }

  ////////////////////////////////////////////////////////////////

  boolean isAminoMonomer() { return false; }
  boolean isAlphaMonomer() { return false; }
  boolean isNucleicMonomer() { return false; }
  public boolean isDna() { return false; }
  public boolean isRna() { return false; }
  final public boolean isProtein() { return isAlphaMonomer(); }
  final public boolean isNucleic() { return isNucleicMonomer(); }

  ////////////////////////////////////////////////////////////////

  void setStructure(ProteinStructure proteinstructure) { };
  ProteinStructure getProteinStructure() { return null; };
  byte getProteinStructureType() { return 0; };
  boolean isHelix() { return false; }
  boolean isHelixOrSheet() { return false; }

  ////////////////////////////////////////////////////////////////

  final Atom getAtomFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)];
  }

  final Point3f getAtomPointFromOffset(byte offset) {
    if (offset == -1)
      return null;
    return chain.frame.atoms[firstAtomIndex + (offset & 0xFF)].point3f;
  }

  final int getLeadAtomIndex() {
    return firstAtomIndex + (leadAtomOffset & 0xFF);
  }

  final Atom getLeadAtom() {
    return getAtomFromOffset(leadAtomOffset);
  }

  final Point3f getLeadAtomPoint() {
    return getAtomPointFromOffset(leadAtomOffset);
  }

  final Atom getWingAtom() {
    if (wingAtomOffset == -1) {
      System.out.println("Monomer.getWingAtomIndex() called ... " +
                         "but I have no wing man");
      throw new NullPointerException();
    }
    return getAtomFromOffset(wingAtomOffset);
  }

  final Point3f getWingAtomPoint() {
    return getWingAtom().point3f;
  }

  ////////////////////////////////////////////////////////////////
  // static stuff for group ids
  ////////////////////////////////////////////////////////////////

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

  ////////////////////////////////////////////////////////////////
  // try to get rid of these 

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
            : chain.frame.getAtomAt(atomIndex));
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

  /*
  void dumpNucleotideIndices() {

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
  }
  */
  
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

  ////////////////////////////////////////////////////////////////

  /*
  public boolean isProtein() {
    return ((distinguishingBits & JmolConstants.ATOMID_PROTEIN_MASK) ==
            JmolConstants.ATOMID_PROTEIN_MASK);
  }

  public boolean isNucleic() { 
   return ((distinguishingBits & JmolConstants.ATOMID_NUCLEIC_MASK) ==
            JmolConstants.ATOMID_NUCLEIC_MASK);
  }

  public boolean isDna() {
    // this is a little tricky ... apply the RNA mask
    // but then check to make sure that the O2 bit is turned off
    return ((distinguishingBits & JmolConstants.ATOMID_RNA_MASK) ==
            JmolConstants.ATOMID_NUCLEIC_MASK);
  }

  public boolean isRna() {
    return ((distinguishingBits & JmolConstants.ATOMID_RNA_MASK) ==
            JmolConstants.ATOMID_RNA_MASK);
  }
  */
}
