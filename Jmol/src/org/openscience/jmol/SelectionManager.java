/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol;

import java.util.BitSet;

public class SelectionManager {

  private final BitSet bsNull = new BitSet();
  public final BitSet bsSelection = new BitSet();

  public void addSelection(Atom atom) {
    bsSelection.set(atom.getAtomNumber());
  }

  public void removeSelection(Atom atom) {
    bsSelection.clear(atom.getAtomNumber());
  }

  public void toggleSelection(Atom atom) {
    int atomNum = atom.getAtomNumber();
    if (bsSelection.get(atomNum))
      bsSelection.clear(atomNum);
    else
      bsSelection.set(atomNum);
  }

  public void addSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      bsSelection.set(atoms[i].getAtomNumber());
  }

  public void removeSelection(Atom[] atoms) {
    for (int i = 0; i < atoms.length; ++i)
      bsSelection.clear(atoms[i].getAtomNumber());
  }

  public void clearSelection() {
    bsSelection.and(bsNull);
  }

  public int countSelection() {
    int count = 0;
    for (int i = 0, size = bsSelection.size(); i < size; ++i)
      if (bsSelection.get(i))
        ++count;
    return count;
  }

  public boolean isSelected(Atom atom) {
    return bsSelection.get(atom.getAtomNumber());
  }

  public void setSelectionSet(BitSet set) {
    bsSelection.and(bsNull);
    bsSelection.or(set);
  }
}
