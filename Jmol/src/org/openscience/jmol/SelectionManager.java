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

  DisplayControl control;

  public SelectionManager(DisplayControl control) {
    this.control = control;
  }

  private final BitSet bsNull = new BitSet();
  public final BitSet bsSelection = new BitSet();
  // this is a tri-state. the value -1 means unknown
  final static int TRUE = 1;
  final static int FALSE = 0;
  final static int UNKNOWN = -1;
  int empty = TRUE;


  public void addSelection(int atomIndex) {
    bsSelection.set(atomIndex);
    empty = FALSE;
  }

  public void addSelection(BitSet set) {
    bsSelection.or(set);
    if (empty == TRUE)
      empty = UNKNOWN;
  }

  public void toggleSelection(int atomIndex) {
    if (bsSelection.get(atomIndex))
      bsSelection.clear(atomIndex);
    else
      bsSelection.set(atomIndex);
    empty = (empty == TRUE) ? FALSE : UNKNOWN;
  }

  public boolean isSelected(int atomIndex) {
    return bsSelection.get(atomIndex);
  }

  public boolean isEmpty() {
    if (empty != UNKNOWN)
      return empty == TRUE;
    for (int i = control.numberOfAtoms(); --i >= 0; )
      if (bsSelection.get(i)) {
        empty = FALSE;
        return false;
      }
    empty = TRUE;
    return true;
  }

  public void selectAll() {
    int count = control.numberOfAtoms();
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0; )
      bsSelection.set(i);
  }

  public void clearSelection() {
    bsSelection.and(bsNull);
    empty = TRUE;
  }

  public void delete(int iDeleted) {
    if (empty == TRUE)
      return;
    int numAfterDelete = control.numberOfAtoms() - 1;
    for (int i = iDeleted; i < numAfterDelete; ++i) {
      if (bsSelection.get(i + 1))
        bsSelection.set(i);
      else
        bsSelection.clear(i);
    }
    empty = UNKNOWN;
  }

  public void setSelectionSet(BitSet set) {
    bsSelection.and(bsNull);
    bsSelection.or(set);
    empty = UNKNOWN;
  }

  public void invertSelection() {
    empty = TRUE;
    for (int i = control.numberOfAtoms(); --i >= 0; )
      if (bsSelection.get(i)) {
        bsSelection.clear(i);
      } else {
        bsSelection.set(i);
        empty = FALSE;
      }
  }

  public void excludeSelectionSet(BitSet setExclude) {
    if (empty == TRUE)
      return;
    for (int i = control.numberOfAtoms(); --i >= 0; )
      if (setExclude.get(i))
        bsSelection.clear(i);
    empty = UNKNOWN;
  }
}
