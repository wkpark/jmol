/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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

final class GroupBuilder {

  final JmolViewer viewer;
  final FrameBuilder frameBuilder;

  boolean hasPendingGroup;

  GroupBuilder(JmolViewer viewer, FrameBuilder frameBuilder) {
    this.viewer = viewer;
    this.frameBuilder = frameBuilder;
  }

  void initializeBuild() {
    hasPendingGroup = false;
  }

  void finalizeBuild() {
    hasPendingGroup = false;
    chain = null;
  }

  Chain chain;
  String group3;
  int groupSequenceNumber;
  char groupInsertionCode;
  int firstAtomIndex, lastAtomIndex;

  void startBuildingGroup(Chain chain, String group3,
                          int groupSequenceNumber, char groupInsertionCode) {
    if (hasPendingGroup)
      throw new NullPointerException();
    hasPendingGroup = true;
    this.chain = chain;
    this.group3 = group3;
    this.groupSequenceNumber = groupSequenceNumber;
    this.groupInsertionCode = groupInsertionCode;
    firstAtomIndex = -1;
  }


  void registerAtom(Atom atom) {
    if (! hasPendingGroup)
      throw new NullPointerException();
    int atomIndex = atom.atomIndex;
    if (firstAtomIndex < 0)
      firstAtomIndex = lastAtomIndex = atomIndex;
    else if (++lastAtomIndex != atomIndex) {
      System.out.println("unexpected atom index while building group\n" +
                         " expected:" + lastAtomIndex +
                         " received:" + atomIndex);
      throw new NullPointerException();
    }
  }



  void finishBuildingGroup() {
    if (! hasPendingGroup)
      return;
    Group group = new Group(chain, group3,
                            groupSequenceNumber, groupInsertionCode,
                            firstAtomIndex, lastAtomIndex);
    propogateGroup(group, firstAtomIndex, lastAtomIndex);
    chain.addGroup(group);
    hasPendingGroup = false;
    chain = null;
  }

  void propogateGroup(Group group, int firstAtomIndex, int lastAtomIndex) {
    Atom[] atoms = frameBuilder.atoms;
    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
      atoms[i].setGroup(group);
  }
}
