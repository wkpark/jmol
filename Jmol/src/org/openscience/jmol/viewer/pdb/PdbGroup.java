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
import java.util.Hashtable;
import javax.vecmath.Point3f;

public class PdbGroup {

  public PdbStructure structure;
  public PdbChain chain;
  public int sequence;
  public short groupID;
  int[] mainchainIndices;

  public PdbGroup(PdbChain chain, int sequence, String group3) {
    this.chain = chain;
    this.sequence = sequence;
    this.groupID = getGroupID(group3);
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

  public int getSequence() {
    return sequence;
  }

  public String getSequenceString() {
    return getSequenceString(sequence);
  }

  public short getGroupID() {
    return groupID;
  }

  public boolean isGroup3Match(String strWildcard) {
    if (strWildcard.length() != 3) {
      System.err.println("group wildcard length != 3");
      return false;
    }
    String group3 = group3Names[groupID];
    for (int i = 0; i < 3; ++i) {
      char charWild = strWildcard.charAt(i);
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
    Short boxedGroupID = (Short)htGroup.get(group3);
    if (boxedGroupID != null)
      return boxedGroupID.shortValue();
    return -1;
  }

  public PdbAtom allocatePdbAtom(int atomIndex, String pdbRecord) {
    PdbAtom pdbAtom = new PdbAtom(this, atomIndex, pdbRecord);
    if (pdbAtom.atomID < JmolConstants.ATOMID_MAINCHAIN_MAX) {
      if (! registerMainchainAtomIndex(pdbAtom.atomID, atomIndex))
        pdbAtom.atomID += JmolConstants.ATOMID_MAINCHAIN_IMPOSTERS;
    }
    return pdbAtom;
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
      System.out.println("sequence=" + getSequenceString());
      return null;
    }
    return chain.model.file.frame.getAtomAt(j);
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

  public static int getSequence(int sequenceNumber, char insertionCode) {
    return (sequenceNumber << 8) + ((insertionCode - ' ') & 0xFF);
  }

  public static String getSequenceString(int sequence) {
    return (sequence & 0xFF) == 0
      ? "" + (sequence >> 8)
      : "" + (sequence >> 8) + (char)(' ' + (sequence & 0xFF));
  }
}
