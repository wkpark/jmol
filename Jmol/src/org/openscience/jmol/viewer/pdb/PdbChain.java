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
import javax.vecmath.Point3f;
import java.util.BitSet;

public class PdbChain {

  public PdbModel model;
  public char chainID;
  int groupCount;
  PdbGroup[] groups = new PdbGroup[16];
  PdbGroup[] mainchain;
  PdbPolymer polymer;
  BitSet atomSet;

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
    polymer = new PdbPolymer(this);
  }
  
  PdbGroup allocateGroup(int sequence, String group3) {

    PdbGroup group = new PdbGroup(this, sequence, group3);

    if (groupCount == groups.length) {
      PdbGroup[] t = new PdbGroup[groupCount * 2];
      System.arraycopy(groups, 0, t, 0, groupCount);
      groups = t;
    }
    return groups[groupCount++] = group;
  }

  public PdbGroup getResidue(int groupIndex) {
    return groups[groupIndex];
  }
  
  public int getResidueCount() {
    return groupCount;
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

  public PdbPolymer getPolymer() {
    return polymer;
  }

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
    if (polymer == null)
      polymer = new PdbPolymer(this);
    polymer.addSecondaryStructure(type, startSeqcode, endSeqcode);
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

  public BitSet getAtomSet() {
    if (atomSet == null) {
      BitSet bs = atomSet = new BitSet();
      Frame frame = model.file.frame;
      Atom[] atoms = frame.getAtoms();
      for (int i = frame.getAtomCount(); --i >= 0; ) {
        Atom atom = atoms[i];
        if (atom.getPdbChain() == this)
          bs.set(i);
      }
    }
    return atomSet;
  }

  public void calcHydrogenBonds() {
    if (polymer != null)
      polymer.calcHydrogenBonds();
  }
}
