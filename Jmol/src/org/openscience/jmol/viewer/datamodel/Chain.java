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
import javax.vecmath.Point3f;
import java.util.BitSet;

final public class Chain {

  public Model model;
  public char chainID;
  int groupCount;
  Group[] groups = new Group[16];

  private Group[] mainchain;
  private Polymer polymer;

  public Chain(Model model, char chainID) {
    this.model = model;
    this.chainID = chainID;
  }

  void freeze() {
    groups = (Group[])Util.setLength(groups, groupCount);
    polymer = Polymer.allocatePolymer(this);
  }
  
  Group allocateGroup(Frame frame, String group3,
                      int sequenceNumber, char insertionCode) {
    Group group =
      new Group(frame, this, sequenceNumber, insertionCode, group3);

    if (groupCount == groups.length)
      groups = (Group[])Util.doubleLength(groups);
    return groups[groupCount++] = group;
  }

  public Group getGroup(int groupIndex) {
    return groups[groupIndex];
  }
  
  public int getGroupCount() {
    return groupCount;
  }

  public Point3f getGroupAlphaCarbonPoint(int groupIndex) {
    return groups[groupIndex].getAlphaCarbonAtom().point3f;
  }

  // to get something other than the alpha carbon atom
  public Point3f getGroupPoint(int groupIndex, int mainchainIndex) {
    return getGroup(groupIndex).getMainchainAtom(mainchainIndex).point3f;
  }

  int mainchainHelper(boolean addGroups) {
    int mainchainCount = 0;
    outer:
    for (int i = groupCount; --i >= 0; ) {
      Group group = groups[i];
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

  public Group[] getMainchain() {
    if (mainchain == null) {
      int mainchainCount = mainchainHelper(false);
      if (mainchainCount == groupCount) {
        mainchain = groups;
      } else {
        mainchain = new Group[mainchainCount];
        if (mainchainCount > 0)
          mainchainHelper(true);
      }
    }
    return mainchain;
  }

  public Polymer getPolymer() {
    return polymer;
  }

  void addSecondaryStructure(byte type,
                             int startSeqcode, int endSeqcode) {
    if (polymer != null)
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
      midPoint.set(groups[groupIndex].aminostructure.
                   getStructureMidPoint(groupIndex));
      /*
      System.out.println("" + groupIndex + "isHelixOrSheet" +
                         midPoint.x + "," + midPoint.y + "," + midPoint.z);
      */
    } else if (groupIndex > 0 &&
               groups[groupIndex - 1].isHelixOrSheet()) {
      midPoint.set(groups[groupIndex - 1].aminostructure.
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

  public void selectAtoms(BitSet bs) {
    Frame frame = model.mmset.frame;
    Atom[] atoms = frame.getAtoms();
    for (int i = frame.getAtomCount(); --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom.getChain() == this)
        bs.set(i);
    }
  }

  public void calcHydrogenBonds() {
    if (polymer != null)
      polymer.calcHydrogenBonds();
  }
}
