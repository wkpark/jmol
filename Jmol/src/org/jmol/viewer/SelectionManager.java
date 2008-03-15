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

import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;

import org.jmol.api.JmolSelectionListener;
import org.jmol.i18n.GT;
import org.jmol.modelset.ModelSet;

import java.util.BitSet;
import java.util.Hashtable;

class SelectionManager {

  private Viewer viewer;

  private JmolSelectionListener[] listeners = new JmolSelectionListener[4];

  SelectionManager(Viewer viewer) {
    this.viewer = viewer;
  }

  final BitSet bsSelection = new BitSet();
  
  BitSet bsSubset; // set in Eval and only pointed to here
  // this is a tri-state. the value -1 means unknown
  private final static int TRUE = 1;
  private final static int FALSE = 0;
  private final static int UNKNOWN = -1;
  private int empty = TRUE;

  private boolean hideNotSelected;
  private final BitSet bsHidden = new BitSet();
 
  void clear() {
    clearSelection(true);
    hide(null, true);
    setSelectionSubset(null);
  }
  
  void hide(BitSet bs, boolean isQuiet) {
    BitSetUtil.clear(bsHidden);
    if (bs != null)
      bsHidden.or(bs);
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet != null)
      modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      viewer.reportSelection(GT._(
          "{0} atoms hidden",
          "" + BitSetUtil.cardinalityOf(bsHidden)));
  }

  void display(BitSet bsAll, BitSet bs, boolean isQuiet) {
    if (bs == null) {
      BitSetUtil.clear(bsHidden);
    } else {
      bsHidden.or(bsAll);
      BitSetUtil.andNot(bsHidden, bs);
    }
    ModelSet modelSet = viewer.getModelSet();
    if (modelSet != null)
      modelSet.setBsHidden(bsHidden);
    if (!isQuiet)
      viewer.reportSelection(GT._(
          "{0} atoms hidden",
          "" + BitSetUtil.cardinalityOf(bsHidden)));
  }

  BitSet getHiddenSet() {
    return bsHidden;
  }

  boolean getHideNotSelected() {
    return hideNotSelected;    
  }
  
  void setHideNotSelected(boolean TF) {
    hideNotSelected = TF;
    if (TF)
      selectionChanged(false);
  }
  
  boolean isSelected(int atomIndex) {
    return bsSelection.get(atomIndex);
  }

  void select(BitSet bs, boolean isQuiet) {
    if (bs == null) {
      selectAll(true);
      if (!viewer.getRasmolHydrogenSetting())
        excludeSelectionSet(viewer.getAtomBits(Token.hydrogen));
      if (!viewer.getRasmolHeteroSetting())
        excludeSelectionSet(viewer.getAtomBits(Token.hetero));
      selectionChanged(false);
    } else {
      setSelectionSet(bs);
    }
    if (!isQuiet)
      viewer.reportSelection(GT._("{0} atoms selected",
          "" + getSelectionCount()));
  }

  void selectAll(boolean isQuiet) {
    int count = viewer.getAtomCount();
    empty = (count == 0) ? TRUE : FALSE;
    for (int i = count; --i >= 0; )
      bsSelection.set(i);
    selectionChanged(isQuiet);
  }

  void clearSelection(boolean isQuiet) {
    hideNotSelected = false;
    BitSetUtil.clear(bsSelection);
    empty = TRUE;
    selectionChanged(isQuiet);
  }

  void setSelectionSet(BitSet set) {
    BitSetUtil.clear(bsSelection);
    if (set != null)
      bsSelection.or(set);
    empty = UNKNOWN;
    selectionChanged(false);
  }

  void setSelectionSubset(BitSet bs) {
    
    //for informational purposes only
    //the real copy is in Eval so that eval operations
    //can all use it directly, and so that all these
    //operations still work properly on the full set of atoms
    
    bsSubset = bs;
  }

  boolean isInSelectionSubset(int atomIndex) {
    return (atomIndex < 0 || bsSubset == null || bsSubset.get(atomIndex));
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
    selectionChanged(false);
  }

  private void excludeSelectionSet(BitSet setExclude) {
    if (setExclude == null || empty == TRUE)
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
      if (bsSelection.get(i) && (bsSubset == null || bsSubset.get(i)))
     ++count;
    if (count > 0)
      empty = FALSE;
    return count;
  }

  void addListener(JmolSelectionListener listener) {
    for (int i = listeners.length; --i >= 0; )
      if (listeners[i] == listener) {
        listeners[i] = null;
        break;
      }
    int len = listeners.length;
    for (int i = len; --i >= 0; )
      if (listeners[i] == null) {
        listeners[i] = listener;
        return;
      }
    listeners = (JmolSelectionListener[])ArrayUtil.doubleLength(listeners);
    listeners[len] = listener;
  }

  private void selectionChanged(boolean isQuiet) {
    if (hideNotSelected)
      hide(BitSetUtil.copyInvert(bsSelection, viewer.getAtomCount()), false);
    if (isQuiet)
      return;
    for (int i = listeners.length; --i >= 0;) {
      JmolSelectionListener listener = listeners[i];
      if (listener != null)
        listeners[i].selectionChanged(bsSelection);
    }
  }
    
  String getState(StringBuffer sfunc) {
    StringBuffer commands = new StringBuffer();
    if (sfunc != null) {
      sfunc.append("  _setSelectionState;\n");
      commands.append("function _setSelectionState();\n");
    }
    StateManager.appendCmd(commands, viewer.getTrajectoryInfo());
    String cmd = null;
    Hashtable temp = new Hashtable();
    if (BitSetUtil.firstSetBit(bsHidden) >= 0)
      temp.put("hide selected", bsHidden);
    if (BitSetUtil.firstSetBit(bsSubset) >= 0)
      temp.put("subset selected", bsSubset);
    cmd = StateManager.getCommands(temp);
    if (cmd != null)
      commands.append(cmd);
    temp = new Hashtable();
    temp.put("-", bsSelection);
    cmd = StateManager.getCommands(temp, null, viewer.getAtomCount());
    if (cmd == null)
      StateManager.appendCmd(commands, "select none");
    else
      commands.append(cmd);
    StateManager.appendCmd(commands, "set hideNotSelected " + viewer.getBooleanProperty("hideNotSelected"));
    commands.append(viewer.getShapeProperty(JmolConstants.SHAPE_STICKS, "selectionState"));
    if (viewer.getSelectionHaloEnabled())
      StateManager.appendCmd(commands, "SelectionHalos ON");
    if (sfunc != null) 
      commands.append("end function;\n\n");
    return commands.toString();
  }

  /*
  void removeSelection(int atomIndex) {
    bsSelection.clear(atomIndex);
    if (empty != TRUE)
        empty = UNKNOWN;
    selectionChanged();
  }

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

  void setSelection(int atomIndex) {
    BitSetUtil.clear(bsSelection);
    bsSelection.set(atomIndex);
    empty = FALSE;
    selectionChanged();
  }

  void toggleSelectionSet(BitSet bs) {
    //
    //  //toggle each one independently
    //for (int i = viewer.getAtomCount(); --i >= 0; )
    //  if (bs.get(i))
    //    toggleSelection(i);
    
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

  void invertSelection(int atomCount) {
    empty = TRUE;
    for (int i = atomCount; --i >= 0; )
      if (bsSelection.get(i)) {
        bsSelection.clear(i);
      } else {
        bsSelection.set(i);
        empty = FALSE;
      }
    selectionChanged();
  }


*/
  

}
