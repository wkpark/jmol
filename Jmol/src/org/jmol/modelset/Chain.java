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

import java.util.BitSet;

import org.jmol.util.BitSetUtil;

public final class Chain {

  ModelSet modelSet;
  Model model;
  char chainID;
  int groupCount;
  int selectedGroupCount;
  private boolean isDna, isRna;
  BitSet bsSelectedGroups;
  Group[] groups = new Group[16];


  //  private Group[] mainchain;

  public Chain(ModelSet modelSet, Model model, char chainID) {
    this.modelSet = modelSet;
    this.model = model;
    this.chainID = chainID;
  }

  public char getChainID() {
    return chainID;
  }
  
  public ModelSet getModelSet() {
    return modelSet;
  }
  
  public boolean isDna() { return isDna; }
  public boolean isRna() { return isRna; }

  public void setIsDna(boolean TF) {isDna = TF;}
  public void setIsRna(boolean TF) {isRna = TF;}

  public Group getGroup(int groupIndex) {
    return groups[groupIndex];
  }
  
  public int getGroupCount() {
    return groupCount;
  }

  public Atom getAtom(int index) {
    return modelSet.atoms[index];
  }
  
  /**
   * prior to coloring by group, we need the chain count per chain
   * that is selected
   * 
   * @param bsSelected
   */
  public void calcSelectedGroupsCount(BitSet bsSelected) {
    selectedGroupCount = 0;
    if (bsSelectedGroups == null)
      bsSelectedGroups = new BitSet();
    BitSetUtil.clear(bsSelectedGroups);
    for (int i = 0; i < groupCount; i++) {
      if (groups[i].isSelected(bsSelected)) {
        groups[i].selectedIndex = selectedGroupCount++;
        bsSelectedGroups.set(i);
      } else {
        groups[i].selectedIndex = -1;
      }
    }
  }

  public void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
    int seqcode, indexA, indexB, minDiff;
    for (indexA = 0; indexA < groupCount && groups[indexA].seqcode != seqcodeA; indexA++) {
    }
    if (indexA == groupCount) {
      // didn't find A exactly -- go find the nearest that is GREATER than this value
      minDiff = Integer.MAX_VALUE;
      for (int i = groupCount; --i >= 0;)
        if ((seqcode = groups[i].seqcode) > seqcodeA
            && (seqcode - seqcodeA) < minDiff) {
          indexA = i;
          minDiff = seqcode - seqcodeA;
        }
      if (minDiff == Integer.MAX_VALUE)
        return;
    }
    if (seqcodeB == Integer.MAX_VALUE)
      seqcodeB = groups[groupCount - 1].seqcode;
    for (indexB = indexA; indexB < groupCount && groups[indexB].seqcode != seqcodeB; indexB++) {
    }
    if (indexB == groupCount) {
      // didn't find B exactly -- get the nearest that is LESS than this value
      minDiff = Integer.MAX_VALUE;
      for (int i = indexA; i < groupCount; i++)
        if ((seqcode = groups[i].seqcode) < seqcodeB
            && (seqcodeB - seqcode) < minDiff) {
          indexB = i;
          minDiff = seqcodeB - seqcode;
        }
      if (minDiff == Integer.MAX_VALUE)
        return;
    }
    for (int i = indexA; i <= indexB; ++i)
      groups[i].selectAtoms(bs);
  }
  
  int getSelectedGroupCount() {
    return selectedGroupCount;
  }

  public final void updateOffsetsForAlternativeLocations(BitSet bsSelected,
                                                  int nAltLocInModel,
                                                  byte[] offsets,
                                                  int firstAtomIndex,
                                                  int lastAtomIndex) {

    String[] atomNames = modelSet.getAtomNames();
    for (int offsetIndex = offsets.length; --offsetIndex >= 0;) {
      int offset = offsets[offsetIndex] & 0xFF;
      if (offset == 255)
        continue;
      int iThis = firstAtomIndex + offset;
      Atom atom = getAtom(iThis);
      if (atom.getAlternateLocationID() == 0)
        continue;
      // scan entire group list to ensure including all of
      // this atom's alternate conformation locations.
      // (PDB order may be AAAAABBBBB, not ABABABABAB)
      int nScan = lastAtomIndex - firstAtomIndex;
      for (int i = 1; i <= nScan; i++) {
        int iNew = iThis + i;
        if (iNew > lastAtomIndex)
          iNew -= nScan + 1;
        int offsetNew = iNew - firstAtomIndex;
        if (offsetNew < 0 || offsetNew > 255 || iNew == iThis
            || atomNames[iNew] != atomNames[iThis]
            || !bsSelected.get(iNew))
          continue;
        offsets[offsetIndex] = (byte) offsetNew;
        /*
         Logger.debug(iNew + " " + offsetNew + " old:" + offset + " "
         + chain.frame.atoms[iThis].getIdentity() + " " + " new:"
         + offsetNew + " " + chain.frame.atoms[iNew].getIdentity());
         */
        break;
      }
    }

  }

  public void fixIndices(int atomsDeleted) {
    for (int i = 0; i < groupCount; i++) {
      groups[i].firstAtomIndex -= atomsDeleted;
      groups[i].lastAtomIndex -= atomsDeleted;
    }
  }
}
