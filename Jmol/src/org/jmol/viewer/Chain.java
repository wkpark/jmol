/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer;

import java.util.BitSet;

final class Chain {

  Frame frame;
  Model model;
  char chainID;
  int groupCount;
  int selectedGroupCount;
  BitSet bsSelectedGroups;
  private final static BitSet bsNull = new BitSet();
  Group[] groups = new Group[16];


  //  private Group[] mainchain;

  Chain(Frame frame, Model model, char chainID) {
    this.frame = frame;
    this.model = model;
    this.chainID = chainID;
  }

  void freeze() {
    groups = (Group[])Util.setLength(groups, groupCount);
  }
  
  void addGroup(Group group) {
    if (groupCount == groups.length)
      groups = (Group[])Util.doubleLength(groups);
    groups[groupCount++] = group;
  }
  
  Group getGroup(int groupIndex) {
    return groups[groupIndex];
  }
  
  int getGroupCount() {
    return groupCount;
  }

  void selectAtoms(BitSet bs) {
    Frame frame = model.mmset.frame;
    Atom[] atoms = frame.getAtoms();
    for (int i = frame.getAtomCount(); --i >= 0; ) {
      Atom atom = atoms[i];
      if (atom.getChain() == this)
        bs.set(i);
    }
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    selectedGroupCount = 0;
    if (bsSelectedGroups == null)
      bsSelectedGroups = new BitSet();
    else
      bsSelectedGroups.and(bsNull);
    for (int i = groupCount; --i >= 0; ) {
      if (groups[i].isSelected(bsSelected)) {
        ++selectedGroupCount;
        bsSelectedGroups.set(i);
      }
    }
  }

  int getSelectedGroupCount() {
    return selectedGroupCount;
  }

  int getSelectedGroupIndex(Group group) {
    int selectedGroupIndex = 0;
    for (int i = 0; i < groupCount; ++i) {
      if (bsSelectedGroups.get(i)) {
        if (groups[i] == group)
          return selectedGroupIndex;
        ++selectedGroupIndex;
      }
    }
    return -1;
  }
}
