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
package org.openscience.jmol.viewer.pdb;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.datamodel.Atom;
import org.openscience.jmol.viewer.datamodel.Frame;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3f;

final public class PdbGroup {

  public PdbChain chain;
  public PdbPolymer polymer;
  public int seqcode;
  public short groupID;
  public PdbStructure structure;
  public int[] mainchainIndices;

  public PdbGroup(PdbChain chain,
                  int sequenceNumber, char insertionCode, String group3) {
    this.chain = chain;
    this.seqcode = getSeqcode(sequenceNumber, insertionCode);
    this.groupID = getGroupID(group3);
  }

  public void setPolymer(PdbPolymer polymer) {
    this.polymer = polymer;
  }

  public void setStructure(PdbStructure structure) {
    this.structure = structure;
  }

  public byte getStructureType() {
    return structure == null ? 0 : structure.type;
  }
  
  public boolean isGroup3(String group3) {
    return group3Names[groupID].equalsIgnoreCase(group3);
  }

  public String getGroup3() {
    return group3Names[groupID];
  }

  public static String getGroup3(short groupID) {
    return group3Names[groupID];
  }

  public int getSeqcode() {
    return seqcode;
  }

  public String getSeqcodeString() {
    return getSeqcodeString(seqcode);
  }

  public short getGroupID() {
    return groupID;
  }

  public boolean isGroup3Match(String strWildcard) {
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

  public char getChainID() {
    return chain.chainID;
  }

  public boolean isHelix() {
    return structure != null &&
      structure.type == JmolConstants.SECONDARY_STRUCTURE_HELIX;
  }

  public boolean isHelixOrSheet() {
    return structure != null &&
      structure.type >= JmolConstants.SECONDARY_STRUCTURE_SHEET;
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
    if (group3NameCount == group3Names.length) {
      String[] t;
      t = new String[group3NameCount * 2];
      System.arraycopy(group3Names, 0, t, 0, group3NameCount);
      group3Names = t;
    }
    short groupID = group3NameCount++;
    group3Names[groupID] = group3;
    htGroup.put(group3, new Short(groupID));
    return groupID;
  }

  static short getGroupID(String group3) {
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
    System.out.println("PdbGroup.registerAtom(atom) atom.atomID="+atom.atomID+
                       " atom.atomName=" + atom.atomName);
    */
    if (atom.atomID < JmolConstants.ATOMID_MAINCHAIN_MAX) {
      if (! registerMainchainAtomIndex(atom.atomID, atom.atomIndex))
        atom.atomID += JmolConstants.ATOMID_MAINCHAIN_IMPOSTERS;
    }
  }

  boolean registerMainchainAtomIndex(short atomid, int atomIndex) {
    if (mainchainIndices == null) {
      mainchainIndices = new int[4];
      for (int i = 4; --i >= 0; )
        mainchainIndices[i] = -1;
    }
    if (mainchainIndices[atomid] != -1) {
      // my residue already has a mainchain atom with this atomid
      // I must be an imposter
      return false;
    }
    mainchainIndices[atomid] = atomIndex;
    return true;
  }

  public int getAlphaCarbonIndex() {
    if (mainchainIndices == null)
      return -1;
    return mainchainIndices[1];
  }

  public Atom getMainchainAtom(int i) {
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
    return chain.pdbmodel.pdbfile.frame.getAtomAt(j);
  }

  public Atom getNitrogenAtom() {
    return getMainchainAtom(0);
  }

  public Atom getAlphaCarbonAtom() {
    return getMainchainAtom(1);
  }

  public Atom getCarbonylCarbonAtom() {
    return getMainchainAtom(2);
  }

  public Atom getCarbonylOxygenAtom() {
    return getMainchainAtom(3);
  }

  public Point3f getAlphaCarbonPoint() {
    return getMainchainAtom(1).point3f;
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
    if (insertionCode >= 'a' && insertionCode <= 'z')
      insertionCode -= 'a' - 'A';
    return (sequenceNumber << 8) + ((insertionCode - ' ') & 0xFF);
  }

  public static String getSeqcodeString(int seqcode) {
    return (seqcode & 0xFF) == 0
      ? "" + (seqcode >> 8)
      : "" + (seqcode >> 8) + '^' + (char)(' ' + (seqcode & 0xFF));
  }

  public boolean isProline() {
    // this is the index into JmolConstants.predefinedGroup3Names
    return groupID == 14;
  }

  public void selectAtoms(BitSet bs) {
    Frame frame = chain.pdbmodel.pdbfile.frame;
    Atom[] atoms = frame.getAtoms();
    for (int i = frame.getAtomCount(); --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom.getPdbGroup() == this)
        bs.set(i);
    }
  }
}
