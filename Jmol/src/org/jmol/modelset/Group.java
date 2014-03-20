/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelset;


import javajs.J2SRequireImport;
import org.jmol.util.BSUtil;

import javajs.util.AU;
import javajs.util.List;
import javajs.util.Quat;

import org.jmol.util.Logger;
import javajs.util.P3;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.constant.EnumStructure;
import org.jmol.java.BS;
import org.jmol.script.T;

import java.util.Hashtable;

import java.util.Map;


@J2SRequireImport({java.lang.Short.class,org.jmol.viewer.JC.class})
public class Group {

  protected int groupIndex;
  
  public int getGroupIndex() {
    return groupIndex;
  }
  
  public void setGroupIndex(int groupIndex) {
    this.groupIndex = groupIndex;
  }

  public Chain chain;
  public int firstAtomIndex = -1;
  public int leadAtomIndex = -1;
  public int lastAtomIndex;

  public int seqcode;
  
  protected short groupID;
  protected boolean isProtein;
  
  int selectedIndex;
  private final static int SEQUENCE_NUMBER_FLAG = 0x80;
  private final static int INSERTION_CODE_MASK = 0x7F; //7-bit character codes, please!
  private final static int SEQUENCE_NUMBER_SHIFT = 8;
  
  public int shapeVisibilityFlags;
  
  public Group() {}
  
  public Group setGroup(Chain chain, String group3, int seqcode,
        int firstAtomIndex, int lastAtomIndex) {
    this.chain = chain;
    this.seqcode = seqcode;
    
    if (group3 == null)
      group3 = "";
    groupID = getGroupIdFor(group3);
    isProtein = (groupID >= 1 && groupID < JC.GROUPID_AMINO_MAX); 

    this.firstAtomIndex = firstAtomIndex;
    this.lastAtomIndex = lastAtomIndex;
    return this;
  }

  public void setModelSet(ModelSet modelSet) {
    chain.model.modelSet = modelSet;  
  }
  
  public final void setShapeVisibility(int visFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= visFlag;        
    } else {
      shapeVisibilityFlags &=~visFlag;
    }
  }

  public final String getGroup3() {
    return group3Names[groupID];
  }

  public static String getGroup3For(short groupID) {
    return group3Names[groupID];
  }

  public final char getGroup1() {
    if (groupID >= JC.predefinedGroup1Names.length)
      return '?';
    return JC.predefinedGroup1Names[groupID];
  }

  public final short getGroupID() {
    return groupID;
  }

  public final int getChainID() {
    return chain.chainID;
  }

  public int getBioPolymerLength() {
    return 0;
  }

  public int getMonomerIndex() {
    return -1;
  }

  public Group[] getGroups() {
    return null;
  }

  public Object getStructure() {
    return null;
  }

  public int getStrucNo() {
    return 0;
  }
  
  public EnumStructure getProteinStructureType() {
    return EnumStructure.NOT;
  }

  public EnumStructure getProteinStructureSubType() {
    return getProteinStructureType();
  }


  /**
   * 
   * @param type
   * @param monomerIndexCurrent
   * @return type
   */
  public int setProteinStructureType(EnumStructure type, int monomerIndexCurrent) {
    return -1;
  }

  public boolean isProtein() { 
    return isProtein; 
  }
  
  public boolean isNucleic() { 
    return (groupID >= JC.GROUPID_AMINO_MAX 
        && groupID < JC.GROUPID_NUCLEIC_MAX); 
  }
  public boolean isDna() { return false; }
  public boolean isRna() { return false; }
  public boolean isPurine() { return false; }
  public boolean isPyrimidine() { return false; }
  public boolean isCarbohydrate() { return false; }

  ////////////////////////////////////////////////////////////////
  // static stuff for group ids
  ////////////////////////////////////////////////////////////////

  private static Map<String, Short> htGroup = new Hashtable<String, Short>();

  private static String[] group3Names = new String[128];
  private static short group3NameCount;
  
  static {
    // this is acceptable for J2S compilation SPECIFICALLY 
    // because even though this class is not final, 
    // group3Names is a private field.
    for (int i = 0; i < JC.predefinedGroup3Names.length; ++i) {
      addGroup3Name(JC.predefinedGroup3Names[i]);
    }
  }
  
  private synchronized static short addGroup3Name(String group3) {
    if (group3NameCount == group3Names.length)
      group3Names = AU.doubleLengthS(group3Names);
    short groupID = group3NameCount++;
    group3Names[groupID] = group3;
    htGroup.put(group3, Short.valueOf(groupID));
    return groupID;
  }

  private static short getGroupIdFor(String group3) {
    if (group3 == null)
      return -1;
    short groupID = lookupGroupID(group3);
    return (groupID == -1 ? addGroup3Name(group3) : groupID);
  }

  public static short lookupGroupID(String group3) {
    if (group3 != null) {
      Short boxedGroupID = htGroup.get(group3);
      if (boxedGroupID != null)
        return boxedGroupID.shortValue();
    }
    return -1;
  }

  ////////////////////////////////////////////////////////////////
  // seqcode stuff
  ////////////////////////////////////////////////////////////////

  public final int getResno() {
    return (seqcode == Integer.MIN_VALUE ? 0 : seqcode >> SEQUENCE_NUMBER_SHIFT); 
  }

  public final static int getSeqNumberFor(int seqcode) {
    return (haveSequenceNumber(seqcode)? seqcode >> SEQUENCE_NUMBER_SHIFT 
        : Integer.MAX_VALUE);
  }
  
  public final static boolean haveSequenceNumber(int seqcode) {
    return ((seqcode & SEQUENCE_NUMBER_FLAG) != 0);
  }

  public final String getSeqcodeString() {
    return getSeqcodeStringFor(seqcode);
  }

  public static int getSeqcodeFor(int seqNo, char insCode) {
    if (seqNo == Integer.MIN_VALUE)
      return seqNo;
    if (! ((insCode >= 'A' && insCode <= 'Z') ||
           (insCode >= 'a' && insCode <= 'z') ||
           (insCode >= '0' && insCode <= '9') ||
           insCode == '?' || insCode == '*')) {
      if (insCode != ' ' && insCode != '\0')
        Logger.warn("unrecognized insertionCode:" + insCode);
      insCode = '\0';
    }
    return ((seqNo == Integer.MAX_VALUE ? 0 
        : (seqNo << SEQUENCE_NUMBER_SHIFT) | SEQUENCE_NUMBER_FLAG))
        + insCode;
  }

  public static String getSeqcodeStringFor(int seqcode) {
    if (seqcode == Integer.MIN_VALUE)
      return null;
    return (seqcode & INSERTION_CODE_MASK) == 0
      ? "" + (seqcode >> SEQUENCE_NUMBER_SHIFT)
      : "" + (seqcode >> SEQUENCE_NUMBER_SHIFT) 
           + '^' + (char)(seqcode & INSERTION_CODE_MASK);
  }

  public char getInsertionCode() {
    if (seqcode == Integer.MIN_VALUE)
      return '\0';
    return (char)(seqcode & INSERTION_CODE_MASK);
  }
  
  public static int getInsertionCodeFor(int seqcode) {
    return (seqcode & INSERTION_CODE_MASK);
  }
  
  public static char getInsertionCodeChar(int seqcode) {
    return (seqcode == Integer.MIN_VALUE ? '\0' : (char)(seqcode & INSERTION_CODE_MASK));
  }
  
  private BS bsAdded;
  
  public boolean isAdded(int atomIndex) {
    return bsAdded != null && bsAdded.get(atomIndex);
  }
  
  public void addAtoms(int atomIndex) {
    if (bsAdded == null)
      bsAdded = new BS();
    bsAdded.set(atomIndex);
  }
  
  public int selectAtoms(BS bs) {
    bs.setBits(firstAtomIndex, lastAtomIndex + 1);
    if (bsAdded != null)
      bs.or(bsAdded);
    return lastAtomIndex;
  }

  public boolean isSelected(BS bs) {
    int pt = bs.nextSetBit(firstAtomIndex);
    return (pt >= 0 && pt <= lastAtomIndex 
        || bsAdded != null &&  bsAdded.intersects(bs));
  }

  boolean isHetero() {
    // just look at the first atom of the group
    return chain.getAtom(firstAtomIndex).isHetero();
  }
  
  @Override
  public String toString() {
    return "[" + getGroup3() + "-" + getSeqcodeString() + "]";
  }

  protected float scaleToScreen(int Z, int mar) {
    return chain.model.modelSet.vwr.scaleToScreen(Z, mar);
  }
  
  protected boolean isCursorOnTopOf(Atom atom, int x, int y, int radius, Atom champ) {
    return chain.model.modelSet.isCursorOnTopOf(atom , x, y, radius, champ);
  }
  
  protected boolean isAtomHidden(int atomIndex) {
    return chain.model.modelSet.isAtomHidden(atomIndex);
  }

  /**
   * BE CAREFUL: FAILURE TO NULL REFERENCES TO model WILL PREVENT FINALIZATION
   * AND CREATE A MEMORY LEAK.
   * 
   * @return associated Model
   */
  public Model getModel() {
    return chain.model;
  }
  
  public int getModelIndex() {
    return chain.model.modelIndex;
  }
  
  public int getSelectedMonomerCount() {
    return 0;
  }

  public int getSelectedMonomerIndex() {
    return -1;
  }
  
  public int getSelectedGroupIndex() {
    return selectedIndex;
  }
  
  /**
   * 
   * @param atomIndex
   * @return T/F
   */
  public boolean isLeadAtom(int atomIndex) {
    return false;
  }
  
  public Atom getLeadAtomOr(Atom atom) { //for sticks
    Atom a = getLeadAtom();
    return (a == null ? atom : a);
  }
  
  public Atom getLeadAtom() {
    return null; // but see Monomer class
  }

  /**
   * 
   * @param qType
   * @return quaternion
   */
  public Quat getQuaternion(char qType) {
    return null;
  }
  
  public Quat getQuaternionFrame(Atom[] atoms) {
    if (lastAtomIndex - firstAtomIndex < 3)
      return null;
    int pt = firstAtomIndex;
    return Quat.getQuaternionFrame(atoms[pt], atoms[++pt], atoms[++pt]);
  }

  /**
   * 
   * @param i
   */
  public void setStrucNo(int i) {
  }

  /**
   * 
   * @param tokType
   * @param qType
   * @param mStep
   * @return helix data of some sort
   */
  public Object getHelixData(int tokType, char qType, int mStep) {
        switch (tokType) {
        case T.point:
          return new P3();
        case T.axis:
        case T.radius:
          return new V3();
        case T.angle:
          return Float.valueOf(Float.NaN);
        case T.array:
        case T.list:
          return new String[] {};
        }
    return "";
  }

  /**
   * 
   * @param type
   * @return T/F
   */
  public boolean isWithinStructure(EnumStructure type) {
    return false;
  }

  public String getProteinStructureTag() {
    return null;
  }

  public String getStructureId() {
    return "";
  }

  public int getBioPolymerIndexInModel() {
    return -1;
  }

  /**
   * 
   * @param g
   * @return T/F
   */
  public boolean isCrossLinked(Group g) {
    return false;
  }

  /**
   * 
   * @param vReturn
   * @return T/F
   */
  public boolean getCrossLinkLead(List<Integer> vReturn) {
    return false;
  }

  public boolean isConnectedPrevious() {
    return false;
  }

  public Atom getNitrogenAtom() {
    return null;
  }

  public Atom getCarbonylOxygenAtom() {
    return null;
  }

  public void fixIndices(int atomsDeleted, BS bsDeleted) {
    firstAtomIndex -= atomsDeleted;
    leadAtomIndex -= atomsDeleted;
    lastAtomIndex -= atomsDeleted;
    if (bsAdded != null)
      BSUtil.deleteBits(bsAdded, bsDeleted);
  }

  public Map<String, Object> getGroupInfo(int igroup) {
    Map<String, Object> infoGroup = new Hashtable<String, Object>();
    infoGroup.put("groupIndex", Integer.valueOf(igroup));
    infoGroup.put("groupID", Short.valueOf(groupID));
    String s = getSeqcodeString();
    if (s != null)
      infoGroup.put("seqCode", s);
    infoGroup.put("_apt1", Integer.valueOf(firstAtomIndex));
    infoGroup.put("_apt2", Integer.valueOf(lastAtomIndex));
    if (bsAdded != null)
    infoGroup.put("addedAtoms", bsAdded);
    infoGroup.put("atomInfo1", chain.model.modelSet.getAtomInfo(firstAtomIndex, null));
    infoGroup.put("atomInfo2", chain.model.modelSet.getAtomInfo(lastAtomIndex, null));
    infoGroup.put("visibilityFlags", Integer.valueOf(shapeVisibilityFlags));
    return infoGroup;
  }

  public void getMinZ(Atom[] atoms, int[] minZ) {
      minZ[0] = Integer.MAX_VALUE;
      for (int i = firstAtomIndex; i <= lastAtomIndex; i++)
        checkMinZ(atoms[i], minZ);
      if (bsAdded != null)
        for (int i = bsAdded.nextSetBit(0); i >= 0; i = bsAdded.nextSetBit(i + 1))
          checkMinZ(atoms[i], minZ);
  }

  private void checkMinZ(Atom atom, int[] minZ) {
      int z = atom.sZ - atom.sD / 2 - 2;
      if (z < minZ[0])
        minZ[0] = Math.max(1, z);
  }

  /**
   * Monomers only
   * 
   * @param tok  
   * @return NaN 
   */
  public float getGroupParameter(int tok) {
    return Float.NaN;
  }

  /**
   * @param name 
   * @param offset  
   * @return index of atom based on offset
   */
  public int getAtomIndex(String name, int offset) {
    return -1;
  }


}
