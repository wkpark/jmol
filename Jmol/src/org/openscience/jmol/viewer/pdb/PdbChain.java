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

public class PdbChain {

  public PdbModel model;
  public char chainID;
  short firstGroupSequence;
  int groupCount;
  PdbGroup[] groups = new PdbGroup[16];
  PdbGroup[] mainchain;

  public PdbChain(PdbModel model, char chainID) {
    this.model = model;
    this.chainID = chainID;
  }

  void freeze() {
    if (groupCount != groups.length) {
      PdbGroup[] t = new PdbGroup[groupCount];
      System.arraycopy(groups, 0, t, 0, groupCount);
      groups = t;
    }
  }
  
  PdbGroup allocateGroup(short groupSequence, String group3) {

    PdbGroup group = new PdbGroup(this, groupSequence, group3);
    
    if (groupCount == 0)
      firstGroupSequence = groupSequence;
    int groupIndex = groupSequence - firstGroupSequence;
    if (groupIndex < 0) {
      System.out.println("residue out of sequence?");
      return group;
    }
    if (groupIndex >= groups.length) {
      PdbGroup[] t = new PdbGroup[groupIndex * 2];
      System.arraycopy(groups, 0, t, 0, groupCount);
      groups = t;
    }
    groups[groupIndex] = group;
    if (groupIndex >= groupCount)
      groupCount = groupIndex + 1;
    return group;
  }

  public PdbGroup getResidue(int groupIndex) {
    return groups[groupIndex];
  }
  
  public int getResidueCount() {
    return groupCount;
  }

  public short getFirstResidueID() {
    return firstGroupSequence;
  }

  public Point3f getResidueAlphaCarbonPoint(int groupIndex) {
    return groups[groupIndex].getAlphaCarbonAtom().point3f;
  }

  // to get something other than the alpha carbon atom
  public Point3f getResiduePoint(int groupIndex, int mainchainIndex) {
    return getResidue(groupIndex).getMainchainAtom(mainchainIndex).point3f;
  }

  int mainchainHelper(boolean addGroups) {
    int mainchainCount = 0;
    outer:
    for (int i = groupCount; --i >= 0; ) {
      PdbGroup group = groups[i];
      int[] mainchainIndices = group.mainchainIndices;
      if (mainchainIndices == null) {
        System.out.println("group.mainchainIndices == null :-(");
        continue;
      }
      for (int j = 4; --j >=0; )
        if (mainchainIndices[j] == -1)
          continue outer;
      if (addGroups)
        mainchain[mainchainCount] = group;
      ++mainchainCount;
    }
    return mainchainCount;
  }

  public PdbGroup[] getMainchain() {
    if (mainchain == null) {
      int mainchainCount = mainchainHelper(false);
      if (mainchainCount == groupCount) {
        mainchain = groups;
      } else {
        mainchain = new PdbGroup[mainchainCount];
        if (mainchainCount > 0)
          mainchainHelper(true);
      }
    }
    return mainchain;
  }

  void addSecondaryStructure(byte type,
                             short startResidueID, short endResidueID) {
    int structureIndex = startResidueID - firstGroupSequence;
    int structureCount = endResidueID - startResidueID + 1;
    if (structureCount < 1 ||
        structureIndex < 0 ||
        endResidueID > (firstGroupSequence + groupCount)) {
      System.out.println("structure definition error");
      return;
    }
    PdbStructure structure;
    switch(type) {
    case JmolConstants.SECONDARY_STRUCTURE_HELIX:
      structure = new Helix(this, structureIndex, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_SHEET:
      structure = new Sheet(this, structureIndex, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_TURN:
      structure = new Turn(this, structureIndex, structureCount);
      break;
    default:
      System.out.println("unrecognized secondary structure type");
      return;
    }
    for (int i = structureIndex + structureCount; --i >= structureIndex; )
      groups[i].setStructure(structure);
  }

  public void getAlphaCarbonMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == groupCount) {
      groupIndex = groupCount - 1;
    } else if (groupIndex > 0) {
      midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
      midPoint.add(groups[groupIndex-1].getAlphaCarbonPoint());
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
  }

  public void getStructureMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex < groupCount &&
        groups[groupIndex].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex].structure.
                   getStructureMidPoint(groupIndex));
      /*
      System.out.println("" + groupIndex + "isHelixOrSheet" +
                         midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else if (groupIndex > 0 &&
               groups[groupIndex - 1].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex - 1].structure.
                   getStructureMidPoint(groupIndex));
      /*
      System.out.println("" + groupIndex + "previous isHelixOrSheet" +
                         midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else {
      getAlphaCarbonMidPoint(groupIndex, midPoint);
      /*
      System.out.println("" + groupIndex + "the alpha carbon midpoint" +
                         midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    }
  }
}
