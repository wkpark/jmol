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

  int distinguishingBits;

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
    distinguishingBits = 0;
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
    int specialAtomID = atom.specialAtomID;
    if ((specialAtomID > 0) &&
        (specialAtomID <  JmolConstants.ATOMID_DISTINGUISHING_ATOM_MAX))
      distinguishingBits |= 1 << specialAtomID;
  }


  void finishBuildingGroup() {
    if (! hasPendingGroup)
      return;
    Group group = distinguishAndAllocateGroup();
    propogateGroup(group, firstAtomIndex, lastAtomIndex);
    chain.addGroup(group);
    hasPendingGroup = false;
    chain = null;
  }
  
  Group distinguishAndAllocateGroup() {
    Atom[] atoms = frameBuilder.atoms;
    Group group = null;
    if ((distinguishingBits & JmolConstants.ATOMID_PROTEIN_MASK) ==
        JmolConstants.ATOMID_PROTEIN_MASK) {
      group = AminoMonomer.validateAndAllocate(chain, group3,
                                               groupSequenceNumber,
                                               groupInsertionCode,
                                               atoms, firstAtomIndex,
                                               lastAtomIndex);
    } else if ((distinguishingBits & JmolConstants.ATOMID_ALPHA_ONLY_MASK) ==
               JmolConstants.ATOMID_ALPHA_ONLY_MASK) {
      group = AlphaMonomer.validateAndAllocate(chain, group3,
                                               groupSequenceNumber,
                                               groupInsertionCode,
                                               atoms, firstAtomIndex,
                                               lastAtomIndex);
    } else if ((distinguishingBits & JmolConstants.ATOMID_NUCLEIC_MASK) ==
               JmolConstants.ATOMID_NUCLEIC_MASK) {
      group = NucleicMonomer.validateAndAllocate(chain, group3,
                                                 groupSequenceNumber,
                                                 groupInsertionCode,
                                                 atoms, firstAtomIndex,
                                                 lastAtomIndex);
    }
    if (group == null)
      group = new Group(chain, group3,
                        groupSequenceNumber, groupInsertionCode,
                        firstAtomIndex, lastAtomIndex);
    return group;
  }

  void propogateGroup(Group group, int firstAtomIndex, int lastAtomIndex) {
    Atom[] atoms = frameBuilder.atoms;
    for (int i = firstAtomIndex; i <= lastAtomIndex; ++i)
      atoms[i].setGroup(group);
  }
}
