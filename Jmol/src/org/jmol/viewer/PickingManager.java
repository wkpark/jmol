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

import java.util.BitSet;
import org.jmol.i18n.GT;

class PickingManager {

  Viewer viewer;

  int pickingMode = JmolConstants.PICKING_IDENT;

  boolean chimeStylePicking = true;

  int queuedAtomCount = 0;
  int[] queuedAtomIndexes = new int[4];

  int[] countPlusIndexes = new int[5];

  PickingManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void atomPicked(int atomIndex, boolean shiftKey) {
    String value;
    if (atomIndex == -1)
      return;
    Frame frame = viewer.getFrame();
    switch(pickingMode) {
    case JmolConstants.PICKING_OFF:
      break;
    case JmolConstants.PICKING_IDENT:
      viewer.setStatusAtomPicked(atomIndex, viewer.getAtomInfo(atomIndex));
      break;
    case JmolConstants.PICKING_DISTANCE:
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2)
        break;
      float distance = frame.getDistance(queuedAtomIndexes[0],
                                         atomIndex);
      value = "Distance "
      + viewer.getAtomInfo(queuedAtomIndexes[0])
      +  " - "
      + viewer.getAtomInfo(queuedAtomIndexes[1])
      + " : " + distance;
    viewer.setStatusNewPickingModeMeasurement(2,value);
      break;
    case JmolConstants.PICKING_ANGLE:
      if (queuedAtomCount >= 3)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 3)
        break;
      float angle = frame.getAngle(queuedAtomIndexes[0],
                                   queuedAtomIndexes[1],
                                   atomIndex);
      value = "Angle " +
      viewer.getAtomInfo(queuedAtomIndexes[0]) +
      " - " +
      viewer.getAtomInfo(queuedAtomIndexes[1]) +
      " - " +
      viewer.getAtomInfo(queuedAtomIndexes[2]) +
      " : " + angle;
      viewer.setStatusNewPickingModeMeasurement(3,value);
      break;
    case JmolConstants.PICKING_TORSION:
      if (queuedAtomCount >= 4)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 4)
        break;
      float torsion = frame.getTorsion(queuedAtomIndexes[0],
                                       queuedAtomIndexes[1],
                                       queuedAtomIndexes[2],
                                       atomIndex);
      value = "Torsion " +
      viewer.getAtomInfo(queuedAtomIndexes[0]) +
      " - " +
      viewer.getAtomInfo(queuedAtomIndexes[1]) +
      " - " +
      viewer.getAtomInfo(queuedAtomIndexes[2]) +
      " - " + 
      viewer.getAtomInfo(queuedAtomIndexes[3]) +
      " : " + torsion;
      viewer.setStatusNewPickingModeMeasurement(4, value);
      break;
    case JmolConstants.PICKING_MONITOR:
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2)
        break;
      countPlusIndexes[0] = 2;
      countPlusIndexes[1] = queuedAtomIndexes[0];
      countPlusIndexes[2] = queuedAtomIndexes[1];
      viewer.toggleMeasurement(countPlusIndexes);
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
      if (shiftKey | chimeStylePicking)
        viewer.toggleSelection(atomIndex);
      else
        viewer.setSelection(atomIndex);
      reportSelection();
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      BitSet bsGroup = frame.getGroupBitSet(atomIndex);
      if (shiftKey | chimeStylePicking)
        viewer.toggleSelectionSet(bsGroup);
      else
        viewer.setSelectionSet(bsGroup);
      viewer.clearClickCount();
      reportSelection();
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      BitSet bsChain = frame.getChainBitSet(atomIndex);
      if (shiftKey | chimeStylePicking)
        viewer.toggleSelectionSet(bsChain);
      else
        viewer.setSelectionSet(bsChain);
      viewer.clearClickCount();
      reportSelection();
      break;
    case JmolConstants.PICKING_SPIN:
      if (viewer.getSpinOn()) {
        viewer.setSpinOn(false);
        break;
      }
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2) {
        viewer.setSpinOn(false);
        viewer.scriptStatus(GT._("pick " 
            + (queuedAtomCount == 1 ? "one more" : "two ") 
            + " atoms in order to spin the model around an axis"));
        break;
      }
      viewer.startSpinningAxis(queuedAtomIndexes[0], atomIndex, false/*isClockwise*/);
    }
  }

  void reportSelection() {
    viewer.scriptStatus("" + viewer.getSelectionCount() + " " + GT._("atoms selected"));
  }

  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    queuedAtomCount = 0;
    System.out.println("setPickingMode(" +
                       pickingMode + ":" +
                       JmolConstants.pickingModeNames[pickingMode] + ")");
  }

  void queueAtom(int atomIndex) {
    queuedAtomIndexes[queuedAtomCount++] = atomIndex;
    viewer.setStatusAtomPicked(atomIndex, "Atom #" + queuedAtomCount + ":" +
                        viewer.getAtomInfo(atomIndex));
  }
}
