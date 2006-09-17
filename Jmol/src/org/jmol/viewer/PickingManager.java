/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.util.Logger;

import java.util.BitSet;
import org.jmol.i18n.GT;

class PickingManager {

  Viewer viewer;

  int pickingMode = JmolConstants.PICKING_IDENT;
  int pickingStyle = JmolConstants.PICKINGSTYLE_CHIME;

  int queuedAtomCount = 0;
  int[] queuedAtomIndexes = new int[4];

  int[] countPlusIndexes = new int[5];

  PickingManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void atomPicked(int atomIndex, int modifiers) {
    boolean shiftKey = ((modifiers & MouseManager.SHIFT) != 0);
    boolean alternateKey = ((modifiers & MouseManager.ALT) != 0);
    if (atomIndex == -1) {
      if (pickingStyle == JmolConstants.PICKINGSTYLE_PFAAT 
          && !shiftKey && !alternateKey) {
        viewer.clearSelection();
        reportSelection();
      }
      if (pickingMode == JmolConstants.PICKING_MONITOR
          || pickingStyle == JmolConstants.PICKINGSTYLE_MEASURE)
        queuedAtomCount = 0;
      return;
    }

    String value;
    Frame frame = viewer.getFrame();
    switch (pickingMode) {
    case JmolConstants.PICKING_OFF:
      break;
    case JmolConstants.PICKING_IDENT:
      viewer.setStatusAtomPicked(atomIndex, viewer.getAtomInfoXYZ(atomIndex));
      break;
    case JmolConstants.PICKING_MONITOR:
    case JmolConstants.PICKING_DISTANCE:
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2)
        break;
      float distance = frame.getDistance(queuedAtomIndexes[0], atomIndex);
      value = "Distance " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[1]) + " : " + distance;
      viewer.setStatusNewPickingModeMeasurement(2, value);
      if (pickingMode == JmolConstants.PICKING_MONITOR
          || pickingStyle == JmolConstants.PICKINGSTYLE_MEASURE)
        toggleMeasurement(2);
      break;
    case JmolConstants.PICKING_ANGLE:
      if (queuedAtomCount >= 3)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 3)
        break;
      float angle = frame.getAngle(queuedAtomIndexes[0], queuedAtomIndexes[1],
          atomIndex);
      value = "Angle " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[1]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[2]) + " : " + angle;
      viewer.setStatusNewPickingModeMeasurement(3, value);
      if (pickingStyle == JmolConstants.PICKINGSTYLE_MEASURE)
        toggleMeasurement(3);
      break;
    case JmolConstants.PICKING_TORSION:
      if (queuedAtomCount >= 4)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 4)
        break;
      float torsion = frame.getTorsion(queuedAtomIndexes[0],
          queuedAtomIndexes[1], queuedAtomIndexes[2], atomIndex);
      value = "Torsion " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[1]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[2]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[3]) + " : " + torsion;
      viewer.setStatusNewPickingModeMeasurement(4, value);
      if (pickingStyle == JmolConstants.PICKINGSTYLE_MEASURE)
        toggleMeasurement(4);
      break;
    case JmolConstants.PICKING_LABEL:
      viewer.togglePickingLabel(atomIndex);
      break;
    case JmolConstants.PICKING_CENTER:
      BitSet bs = new BitSet();
      bs.set(atomIndex);
      viewer.setCenterBitSet(bs, false);
      break;
    case JmolConstants.PICKING_SELECT_ATOM:
      applyMouseStyle(atomIndex, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      BitSet bsGroup = frame.getGroupBitSet(atomIndex);
      applyMouseStyle(bsGroup, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      BitSet bsChain = frame.getChainBitSet(atomIndex);
      applyMouseStyle(bsChain, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_MOLECULE:
      BitSet bsMolecule = frame.getMoleculeBitSet(atomIndex);
      applyMouseStyle(bsMolecule, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_SITE:
      BitSet bsSite = frame.getVisibleSiteBitSet(atomIndex);
      applyMouseStyle(bsSite, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_ELEMENT:
      BitSet bsElement = frame.getVisibleElementBitSet(atomIndex);
      applyMouseStyle(bsElement, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SPIN:
      if (viewer.getSpinOn()) {
        viewer.setSpinOn(false);
        break;
      }
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      if (queuedAtomCount == 1 && queuedAtomIndexes[0] == atomIndex)
        break;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2) {
        viewer.setSpinOn(false);
        viewer.scriptStatus(GT._("pick "
            + (queuedAtomCount == 1 ? "one more" : "two ")
            + " atoms in order to spin the model around an axis"));
        break;
      }
      viewer
          .startSpinningAxis(queuedAtomIndexes[0], atomIndex, false/*isClockwise*/);
    }
  }

  void toggleMeasurement(int nAtoms) {
    countPlusIndexes[0] = nAtoms;
    for (int i = 0; i < nAtoms; i++)
      countPlusIndexes[i + 1] = queuedAtomIndexes[i];
    viewer.toggleMeasurement(countPlusIndexes);  
  }
  
  void applyMouseStyle(int atomIndex, boolean shiftKey, boolean alternateKey) {
    if (pickingStyle == JmolConstants.PICKINGSTYLE_PFAAT) {
      if (shiftKey && alternateKey)
        viewer.removeSelection(atomIndex);
      else if (shiftKey)
        viewer.toggleSelection(atomIndex);
      else if (alternateKey)
        viewer.addSelection(atomIndex);
      else
        viewer.setSelection(atomIndex);
    } else {
      if (shiftKey | pickingStyle == JmolConstants.PICKINGSTYLE_CHIME)
        viewer.toggleSelection(atomIndex);
      else
        viewer.setSelection(atomIndex);
    }
    reportSelection();
  }
  
  void applyMouseStyle(BitSet bs, boolean shiftKey, boolean alternateKey) {
    if (pickingStyle == JmolConstants.PICKINGSTYLE_PFAAT) {
      if (shiftKey && alternateKey)
        viewer.removeSelection(bs);
      else if (shiftKey)
        viewer.toggleSelectionSet(bs);
      else if (alternateKey)
        viewer.addSelection(bs);
      else
        viewer.setSelectionSet(bs);
    } else {
      if (shiftKey | pickingStyle == JmolConstants.PICKINGSTYLE_CHIME)
        viewer.toggleSelectionSet(bs);
      else
        viewer.setSelectionSet(bs);
    }
    reportSelection();
  }

    void reportSelection() {
    viewer.reportSelection("" + viewer.getSelectionCount() + " " + GT._("atoms selected"));
  }

  int getPickingMode() {
    return pickingMode;
  }
    
  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    queuedAtomCount = 0;
    Logger.debug("setPickingMode(" +
                       pickingMode + ":" +
                       JmolConstants.pickingModeNames[pickingMode] + ")");
  }

  boolean setPickingStyle(String style) {
    int pickingStyle = -1; 
    for (int i = JmolConstants.pickingStyleNames.length; --i >=0; )
      if (style.equalsIgnoreCase(JmolConstants.pickingStyleNames[i]))
        pickingStyle = i;
    if (pickingStyle < 0)
      return false;
    this.pickingStyle = pickingStyle;
    queuedAtomCount = 0;
    Logger.debug("setPickingStyle(" +
            pickingStyle + ":" +
            JmolConstants.pickingStyleNames[pickingStyle] + ")");
    return true;
  }

  void queueAtom(int atomIndex) {
    queuedAtomIndexes[queuedAtomCount++] = atomIndex;
    viewer.setStatusAtomPicked(atomIndex, "Atom #" + queuedAtomCount + ":" +
                        viewer.getAtomInfo(atomIndex));
  }
}
