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
package org.jmol.viewer.managers;

import org.jmol.viewer.*;
import org.jmol.viewer.datamodel.Atom;
import org.jmol.viewer.datamodel.Frame;
import javax.vecmath.Point3f;
import java.util.BitSet;

public class PickingManager {

  JmolViewer viewer;

  int pickingMode = JmolConstants.PICKING_IDENT;

  int queuedAtomCount = 0;
  int[] queuedAtomIndexes = new int[4];

  int[] countPlusIndexes = new int[5];

  public PickingManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public void atomPicked(int atomIndex, boolean shiftKey) {
    if (atomIndex == -1)
      return;
    Frame frame = viewer.getFrame();
    switch(pickingMode) {
    case JmolConstants.PICKING_OFF:
      break;
    case JmolConstants.PICKING_IDENT:
      viewer.notifyAtomPicked(atomIndex);
      break;
    case JmolConstants.PICKING_DISTANCE:
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2)
        break;
      float distance = frame.getDistance(queuedAtomIndexes[0],
                                         atomIndex);
      viewer.scriptStatus("Distance " +
                          viewer.getAtomInfo(queuedAtomIndexes[0]) +
                          " - " +
                          viewer.getAtomInfo(queuedAtomIndexes[1]) +
                          " : " + distance);
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
      viewer.scriptStatus("Angle " +
                          viewer.getAtomInfo(queuedAtomIndexes[0]) +
                          " - " +
                          viewer.getAtomInfo(queuedAtomIndexes[1]) +
                          " - " +
                          viewer.getAtomInfo(queuedAtomIndexes[2]) +
                          " : " + angle);
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
      viewer.scriptStatus("Torsion " +
                          viewer.getAtomInfo(queuedAtomIndexes[0]) +
                          " - " +
                          viewer.getAtomInfo(queuedAtomIndexes[1]) +
                          " - " +
                          viewer.getAtomInfo(queuedAtomIndexes[2]) +
                          " - " + 
                          viewer.getAtomInfo(queuedAtomIndexes[3]) +
                          " : " + torsion);
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
      viewer.setCenter(frame.getAtomPoint3f(atomIndex));
      break;
    case JmolConstants.PICKING_SELECT_ATOM:
      if (shiftKey)
        viewer.toggleSelection(atomIndex);
      else
        viewer.setSelection(atomIndex);
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      BitSet bsGroup = frame.getGroupBitSet(atomIndex);
      if (shiftKey)
        viewer.toggleSelectionSet(bsGroup);
      else
        viewer.setSelectionSet(bsGroup);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      BitSet bsChain = frame.getChainBitSet(atomIndex);
      if (shiftKey)
        viewer.toggleSelectionSet(bsChain);
      else
        viewer.setSelectionSet(bsChain);
      viewer.clearClickCount();
      break;
    }
  }

  public void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    queuedAtomCount = 0;
    System.out.println("setPickingMode(" +
                       pickingMode + ":" +
                       JmolConstants.pickingModeNames[pickingMode] + ")");
  }

  void queueAtom(int atomIndex) {
    queuedAtomIndexes[queuedAtomCount++] = atomIndex;
    viewer.scriptStatus("Atom #" + queuedAtomCount + ":" +
                        viewer.getAtomInfo(atomIndex));
  }
}
