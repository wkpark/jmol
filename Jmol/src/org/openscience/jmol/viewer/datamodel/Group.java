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

public class Group {

  Chain chain;
  int seqcode;
  short groupID;
  int firstAtomIndex = -1;
  int lastAtomIndex;


  Group(Chain chain, String group3, int sequenceNumber, char insertionCode,
        int firstAtomIndex, int lastAtomIndex) {
    this.chain = chain;
    this.seqcode = getSeqcode(sequenceNumber, insertionCode);
    if (group3 == null)
      group3 = "";
    this.groupID = getGroupID(group3);
    this.firstAtomIndex = firstAtomIndex;
    this.lastAtomIndex = lastAtomIndex;
  }

  void freeze() {
  }

  public final boolean isGroup3(String group3) {
    return group3Names[groupID].equalsIgnoreCase(group3);
  }

  final String getGroup3() {
    return group3Names[groupID];
  }

  public static String getGroup3(short groupID) {
    return group3Names[groupID];
  }

  final int getSeqcode() {
    return seqcode;
  }

  final String getSeqcodeString() {
    return getSeqcodeString(seqcode);
  }

  final short getGroupID() {
    return groupID;
  }

  final boolean isGroup3Match(String strWildcard) {
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

  final char getChainID() {
    return chain.chainID;
  }

  byte getProteinStructureType() {
    return JmolConstants.PROTEIN_STRUCTURE_NONE;
  }

  public boolean isProtein() { return false; }
  public boolean isNucleic() { return false; }
  public boolean isDna() { return false; }
  public boolean isRna() { return false; }

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
  // seqcode stuff
  ////////////////////////////////////////////////////////////////

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

  public final void selectAtoms(BitSet bs) {
    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
      bs.set(i);
  }
  
  public String toString() {
    return "[" + getGroup3() + "-" + getSeqcodeString() + "]";
  }

}
