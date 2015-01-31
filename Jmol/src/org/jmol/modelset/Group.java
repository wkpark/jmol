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

import javajs.util.Lst;
import javajs.util.Quat;

import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import javajs.util.P3;
import org.jmol.c.STR;
import org.jmol.java.BS;

import java.util.Hashtable;

import java.util.Map;


/**
 * The essential container for every atom. Possibly a Monomer, but not necessarily.
 * Always a member of a chain; sometimes a member of a BioPolymer.
 * 
 * Groups need not be contiguous. firstAtomIndex is always fine to use, but
 * lastAtomIndex is only the end of the INITIAL set of atoms in the file and
 * must be carefully used. 
 * 
 */
@J2SRequireImport({java.lang.Short.class,org.jmol.viewer.JC.class})
public class Group {

  public static String standardGroupList; // will be populated by org.jmol.biomodelset.Resolver
  public static String[] group3Names = new String[128];
  public static String[] specialAtomNames; // filled by Resolver
  

  /**
   * required
   */
  public Chain chain;

  public int groupIndex;
  public char group1; // set by modelLoader or possibly DSSRParser

  public int firstAtomIndex = -1;
  public int leadAtomIndex = -1;
  public int lastAtomIndex;
  private BS bsAdded;

  public int seqcode;
  
  public short groupID;
  
  /**
   * for coloring by group
   */
  public int selectedIndex;
  
  private final static int SEQUENCE_NUMBER_FLAG = 0x80;
  private final static int INSERTION_CODE_MASK = 0x7F; //7-bit character codes, please!
  private final static int SEQUENCE_NUMBER_SHIFT = 8;
  
  public int shapeVisibilityFlags;
  
  /**
   * @j2sIngore
   */
  public Group() {}
  
  public Group setGroup(Chain chain, String group3, int seqcode,
        int firstAtomIndex, int lastAtomIndex) {
    this.chain = chain;
    this.seqcode = seqcode;
    this.firstAtomIndex = firstAtomIndex;
    this.lastAtomIndex = lastAtomIndex;
    if (group3 != null && group3.length() > 0)
      setGroupID(group3);
    return this;
  }

  /**
   * @param group3  
   */
  protected void setGroupID(String group3) {
    // monomer only
  }

  public boolean isAdded(int atomIndex) {
    return bsAdded != null && bsAdded.get(atomIndex);
  }
  
  public void addAtoms(int atomIndex) {
    if (bsAdded == null)
      bsAdded = new BS();
    bsAdded.set(atomIndex);
  }
  
  /**
   * note that we may pick up additional bits here
   * that were added later
   * 
   * @param bs
   * @return last of the contiguous atoms
   */
  public int setAtomBits(BS bs) {
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
  
  public final void setShapeVisibility(int visFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= visFlag;        
    } else {
      shapeVisibilityFlags &=~visFlag;
    }
  }

  public String getGroup3() {
    return (groupID < 1 ? "" : group3Names[groupID]);
  }


  public char getGroup1() {
    return '?';
  }
  
  public int getBioPolymerLength() {
    return 0;
  }

  public int getMonomerIndex() {
    return -1;
  }

  public Object getStructure() {
    return null;
  }

  public int getStrucNo() {
    return 0;
  }
  
  public STR getProteinStructureType() {
    return STR.NOT;
  }

  public STR getProteinStructureSubType() {
    return getProteinStructureType();
  }


  /**
   * 
   * @param type
   * @param monomerIndexCurrent
   * @return type
   */
  public int setProteinStructureType(STR type, int monomerIndexCurrent) {
    return -1;
  }

  public boolean isProtein() { 
    return false; 
  }
  
  public boolean isNucleic() {
    return false;
    //return (groupID >= JC.GROUPID_AMINO_MAX 
      //  && groupID < JC.GROUPID_NUCLEIC_MAX); 
  }
  public boolean isDna() { return false; }
  public boolean isRna() { return false; }
  public boolean isPurine() { return false; }
  public boolean isPyrimidine() { return false; }
  public boolean isCarbohydrate() { return false; }


  ////////////////////////////////////////////////////////////////
  // seqcode stuff
  ////////////////////////////////////////////////////////////////

  public final int getResno() {
    return (seqcode == Integer.MIN_VALUE ? 0 : seqcode >> SEQUENCE_NUMBER_SHIFT); 
  }

  public void setResno(int i) {
    seqcode = getSeqcodeFor(i, getInsertionCode());
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
    return (seqcode == Integer.MIN_VALUE ? '\0' :(char)(seqcode & INSERTION_CODE_MASK));
  }
  
  public static int getInsertionCodeFor(int seqcode) {
    return (seqcode & INSERTION_CODE_MASK);
  }
  
  public static char getInsertionCodeChar(int seqcode) {
    return (seqcode == Integer.MIN_VALUE ? '\0' : (char)(seqcode & INSERTION_CODE_MASK));
  }
  
  protected float scaleToScreen(int Z, int mar) {
    return chain.model.ms.vwr.tm.scaleToScreen(Z, mar);
  }
  
  protected boolean isCursorOnTopOf(Atom atom, int x, int y, int radius, Atom champ) {
    return chain.model.ms.isCursorOnTopOf(atom , x, y, radius, champ);
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
  
  public int getSelectedMonomerCount() {
    return 0;
  }

  public int getSelectedMonomerIndex() {
    return -1;
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
    return Escape.escapeHelical(null, tokType, null, null, null);
  }

  /**
   * 
   * @param type
   * @return T/F
   */
  public boolean isWithinStructure(STR type) {
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
  public boolean getCrossLinkLead(Lst<Integer> vReturn) {
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

  public Map<String, Object> getGroupInfo(int igroup, P3 ptTemp) {
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
    infoGroup.put("atomInfo1", chain.model.ms.getAtomInfo(firstAtomIndex, null, ptTemp));
    infoGroup.put("atomInfo2", chain.model.ms.getAtomInfo(lastAtomIndex, null, ptTemp));
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

  public BS getBSSideChain() {
    // for now, AlphaMonomer only
    return new BS();
  }

  @Override
  public String toString() {
    return "[" + getGroup3() + "-" + getSeqcodeString() + "]";
  }

}
