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

  Frame frame;
  Model model;
  char chainID;
  int groupCount;
  Group[] groups = new Group[16];

  //  private Group[] mainchain;

  Chain(Frame frame, Model model, char chainID) {
    this.frame = frame;
    this.model = model;
    this.chainID = chainID;
  }

  void freeze() {
    groups = (Group[])Util.setLength(groups, groupCount);
    for (int i = groupCount; --i >= 0; )
      groups[i].freeze();
  }
  
  Group allocateGroup(String group3,
                      int sequenceNumber, char insertionCode) {
    Group group =
      new Group(this, sequenceNumber, insertionCode, group3);

    if (groupCount == groups.length)
      groups = (Group[])Util.doubleLength(groups);
    return groups[groupCount++] = group;
  }

  Group getGroup(int groupIndex) {
    return groups[groupIndex];
  }
  
  int getGroupCount() {
    return groupCount;
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
}
