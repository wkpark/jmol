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

  JmolViewer viewer;

  boolean hasPendingGroup;
  Group currentGroup;

  GroupBuilder(JmolViewer viewer) {
    this.viewer = viewer;
  }

  void initializeBuild() {
    hasPendingGroup = false;
  }

  void finalizeBuild() {
    hasPendingGroup = false;
    currentGroup = null;
  }

  void startBuildingGroup(Chain currentChain, String group3,
                          int groupSequenceNumber, char groupInsertionCode) {
    if (hasPendingGroup)
      throw new NullPointerException();
    hasPendingGroup = true;
    currentGroup =
      currentChain.allocateGroup(group3,
                                 groupSequenceNumber, groupInsertionCode);
  }


  void registerAtom(Atom atom) {
    if (! hasPendingGroup)
      throw new NullPointerException();
    atom.setGroup(currentGroup);
    currentGroup.registerAtom(atom);
  }



  void finishBuildingGroup() {
    if (hasPendingGroup) {
      hasPendingGroup = false;
    }
  }
}
