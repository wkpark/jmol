/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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

class SelectionManager {

  JmolViewer viewer;

  SelectionManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  private final BitSet bsNull = new BitSet();
  final BitSet bsSelection = new BitSet();
  // this is a tri-state. the value -1 means unknown
  final static int TRUE = 1;
  final static int FALSE = 0;
  final static int UNKNOWN = -1;
  int empty = TRUE;


  void addSelection(int atomIndex) {
    bsSelection.set(atomIndex);
    empty = FALSE;
  }

  void addSelection(BitSet set) {
    bsSelection.or(set);
    if (empty == TRUE)
      empty = UNKNOWN;
  }

  void toggleSelection(int atomIndex) {
    if (bsSelection.get(atomIndex))
      bsSelection.clear(atomIndex);
    else
      bsSelection.set(atomIndex);
    empty = (empty == TRUE) ? FALSE : UNKNOWN;
  }

  boolean isSelected(int atomIndex) {
    return bsSelection.get(atomIndex);
  }

  boolean isEmpty() {
    if (empty != UNKNOWN)
      return empty == TRUE;
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (bsSelection.get(i)) {
        empty = FALSE;
        return false;
      }
    empty = TRUE;
    return true;
  }

  void selectAll() {
    int count = viewer.getAtomCount();
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0; )
      bsSelection.set(i);
  }

  void clearSelection() {
    bsSelection.and(bsNull);
    empty = TRUE;
  }

  void delete(int iDeleted) {
    if (empty == TRUE)
      return;
    int numAfterDelete = viewer.getAtomCount() - 1;
    for (int i = iDeleted; i < numAfterDelete; ++i) {
      if (bsSelection.get(i + 1))
        bsSelection.set(i);
      else
        bsSelection.clear(i);
    }
    empty = UNKNOWN;
  }

  void setSelection(int atomIndex) {
    bsSelection.and(bsNull);
    bsSelection.set(atomIndex);
    empty = FALSE;
  }

  void setSelectionSet(BitSet set) {
    bsSelection.and(bsNull);
    bsSelection.or(set);
    empty = UNKNOWN;
  }

  void toggleSelectionSet(BitSet bs) {
    /*
      toggle each one independently
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (bs.get(i))
        toggleSelection(i);
    */
    int atomCount = viewer.getAtomCount();
    int i = atomCount;
    while (--i >= 0)
      if (bs.get(i) && !bsSelection.get(i))
        break;
    if (i < 0) { // all were selected
      for (i = atomCount; --i >= 0; )
        if (bs.get(i))
          bsSelection.clear(i);
      empty = UNKNOWN;
    } else { // at least one was not selected
      do {
        if (bs.get(i)) {
          bsSelection.set(i);
          empty = FALSE;
        }
      } while (--i >= 0);
    }
  }

  void invertSelection() {
    empty = TRUE;
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (bsSelection.get(i)) {
        bsSelection.clear(i);
      } else {
        bsSelection.set(i);
        empty = FALSE;
      }
  }

  void excludeSelectionSet(BitSet setExclude) {
    if (empty == TRUE)
      return;
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (setExclude.get(i))
        bsSelection.clear(i);
    empty = UNKNOWN;
  }

  int getSelectionCount() {
    // FIXME mth 2003 11 16
    // very inefficient ... but works for now
    // need to implement our own bitset that keeps track of the count
    // maybe one that takes 'model' into account as well
    if (empty == TRUE)
      return 0;
    int count = 0;
    empty = TRUE;
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (bsSelection.get(i))
     ++count;
    if (count > 0)
      empty = FALSE;
    return count;
  }
}
