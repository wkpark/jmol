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

public class PdbPolymer {

  public PdbChain chain;
  int count;
  PdbGroup[] groups;
  int[] atomIndices;

  public PdbPolymer(PdbChain chain) {
    this.chain = chain;

    PdbGroup[] chainGroups = chain.groups;
    int firstNonMainchain = 0;
    for (int i = 0; i < chain.groupCount; ++i ) {
      PdbGroup group = chainGroups[i];
      if (group == null || ! group.hasFullMainchain())
        continue;
      ++count;
      if (firstNonMainchain == i)
        ++firstNonMainchain;
    }
    if (count < 2) {
      count = 0;
    } else if (count == firstNonMainchain) {
      // either a complete match or the polymer is at the front of the chain
      groups = chainGroups;
    } else {
      groups = new PdbGroup[count];
      for (int i = 0, j = 0; i < chain.groupCount; ++i) {
        PdbGroup group = chainGroups[i];
        if (group == null || !group.hasFullMainchain())
          continue;
        groups[j++] = group;
      }
    }
  }

  public int getCount() {
    return count;
  }

  public PdbGroup[] getGroups() {
    return groups;
  }

  public int[] getAtomIndices() {
    if (atomIndices == null) {
      atomIndices = new int[count];
      for (int i = count; --i >= 0; )
        atomIndices[i] = groups[i].getAlphaCarbonIndex();
    }
    return atomIndices;
  }

  public Point3f getResidueAlphaCarbonPoint(int groupIndex) {
    return groups[groupIndex].getAlphaCarbonAtom().point3f;
  }

  // to get something other than the alpha carbon atom
  public Point3f getResiduePoint(int groupIndex, int mainchainIndex) {
    return groups[groupIndex].getMainchainAtom(mainchainIndex).point3f;
  }

  public void getAlphaCarbonMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == count) {
      groupIndex = count - 1;
    } else if (groupIndex > 0) {
      midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
      midPoint.add(groups[groupIndex-1].getAlphaCarbonPoint());
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(groups[groupIndex].getAlphaCarbonPoint());
  }

  public void getStructureMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex < count &&
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

  public int getIndex(short groupSequence) {
    for (int i = count; --i >= 0; )
      if (groups[i].groupSequence == groupSequence)
        return i;
    return -1;
  }

  void addSecondaryStructure(byte type,
                             short startResidueID, short endResidueID) {
    //    System.out.println("PdbPolymer.addSecondaryStructure(" +
    //                       type + "," + startResidueID + "," +
    // endResidueID +")");
    int polymerIndexStart, polymerIndexEnd;
    if ((polymerIndexStart = getIndex(startResidueID)) == -1 ||
        (polymerIndexEnd = getIndex(endResidueID)) == -1)
      return;
    int structureCount = polymerIndexEnd - polymerIndexStart + 1;
    if (structureCount < 1) {
      System.out.println("structure definition error");
      return;
    }
    PdbStructure structure;
    switch(type) {
    case JmolConstants.SECONDARY_STRUCTURE_HELIX:
      structure = new Helix(this, polymerIndexStart, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_SHEET:
      structure = new Sheet(this, polymerIndexStart, structureCount);
      break;
    case JmolConstants.SECONDARY_STRUCTURE_TURN:
      structure = new Turn(this, polymerIndexStart, structureCount);
      break;
    default:
      System.out.println("unrecognized secondary structure type");
      return;
    }
    for (int i = polymerIndexStart; i <= polymerIndexEnd; ++i)
      groups[i].setStructure(structure);
  }
}
