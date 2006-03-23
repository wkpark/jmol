/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development
 *
 * Contact: miguel@jmol.org, jmol-developers@lists.sf.net
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.api.JmolSelectionListener;
import java.util.BitSet;
import java.util.Vector;

class SelectionManager {

  Viewer viewer;

  JmolSelectionListener[] listeners = new JmolSelectionListener[4];

  SelectionManager(Viewer viewer) {
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
    if (! bsSelection.get(atomIndex)) {
      bsSelection.set(atomIndex);
      empty = FALSE;
      selectionChanged();
    }
  }

  void addSelection(BitSet set) {
    bsSelection.or(set);
    if (empty == TRUE)
      empty = UNKNOWN;
    selectionChanged();
  }

  void toggleSelection(int atomIndex) {
    if (bsSelection.get(atomIndex))
      bsSelection.clear(atomIndex);
    else
      bsSelection.set(atomIndex);
    empty = (empty == TRUE) ? FALSE : UNKNOWN;
    selectionChanged();
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

  void select(BitSet bs) {    
    if (bs == null) {
      if (!viewer.getRasmolHydrogenSetting())
        excludeSelectionSet(viewer.getAtomBits("hydrogen"));
      if (!viewer.getRasmolHeteroSetting())
        excludeSelectionSet(viewer.getAtomBits("hetero"));

    } else {
      setSelectionSet(bs);
    }
  }
  
  void selectAll() {
    int count = viewer.getAtomCount();
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0; )
      bsSelection.set(i);
    selectionChanged();
  }

  void clearSelection() {
    bsSelection.and(bsNull);
    empty = TRUE;
    selectionChanged();
  }

  void setSelection(int atomIndex) {
    bsSelection.and(bsNull);
    bsSelection.set(atomIndex);
    empty = FALSE;
    selectionChanged();
  }

  void setSelectionSet(BitSet set) {
    bsSelection.and(bsNull);
    bsSelection.or(set);
    empty = UNKNOWN;
    selectionChanged();
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
    selectionChanged();
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
    selectionChanged();
  }

  void excludeSelectionSet(BitSet setExclude) {
    if (empty == TRUE)
      return;
    for (int i = viewer.getAtomCount(); --i >= 0; )
      if (setExclude.get(i))
        bsSelection.clear(i);
    empty = UNKNOWN;
    selectionChanged();
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

  void addListener(JmolSelectionListener listener) {
    removeListener(listener);
    int len = listeners.length;
    for (int i = len; --i >= 0; ) {
      if (listeners[i] == null) {
        listeners[i] = listener;
        return;
      }
    }
    listeners = (JmolSelectionListener[])Util.doubleLength(listeners);
    listeners[len] = listener;
  }

  void removeListener(JmolSelectionListener listener) {
    for (int i = listeners.length; --i >= 0; )
      if (listeners[i] == listener) {
        listeners[i] = null;
        return;
      }
  }

  private void selectionChanged() {
    for (int i = listeners.length; --i >= 0; ) {
      JmolSelectionListener listener = listeners[i];
      if (listener != null)
        listeners[i].selectionChanged(bsSelection);
    }
  }
  
  BitSet getAtomBitSet(String atomExpression) {
    Eval e = new Eval(viewer);
    
    BitSet bs = new BitSet();
    try {
      bs = e.getAtomBitSet(atomExpression);
    } catch (Exception ex) {
      System.out.println("getAtomBitSet " + atomExpression);
      System.out.println(ex);
      return bs;
    }
    return bs;  
  }

  Vector getAtomBitSetVector(String atomExpression) {
    Vector V = new Vector();
    BitSet bs = getAtomBitSet(atomExpression);
    int atomCount = viewer.getAtomCount();
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) V.add(new Integer(i));
    return V;
  }
}
