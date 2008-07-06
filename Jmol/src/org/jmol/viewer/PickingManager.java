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
import org.jmol.util.Measure;

import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.ModelSet;

class PickingManager {

  private Viewer viewer;

  private int pickingMode = JmolConstants.PICKING_IDENT;
  private int pickingStyleSelect = JmolConstants.PICKINGSTYLE_SELECT_CHIME;
  private int pickingStyleMeasure = JmolConstants.PICKINGSTYLE_MEASURE_OFF;

  private boolean drawHover;
  private int pickingStyle;
    
  private int queuedAtomCount;
  private int[] queuedAtomIndexes = new int[4];
  private int[] countPlusIndices = new int[5];

  PickingManager(Viewer viewer) {
    this.viewer = viewer;
    clear();
  }

  void clear() {
    pickingMode = JmolConstants.PICKING_IDENT;
    drawHover = false;
  }
  
  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    queuedAtomCount = 0;
  }

  int getPickingMode() {
    return pickingMode;
  }
    
  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (Logger.debugging) {
      Logger.debug(
          " setPickingStyle " + pickingStyle+": " +
          JmolConstants.getPickingStyleName(pickingStyle));
    }
    if (pickingStyle >= JmolConstants.PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      queuedAtomCount = 0;
    } else {
      pickingStyleSelect = pickingStyle;
    }
  }
  
  int getPickingStyle() {
    return pickingStyle;
  }

  void setDrawHover(boolean TF) {
    drawHover = TF;
  }
  
  boolean getDrawHover() {
    return drawHover;
  }

  void atomPicked(int atomIndex, int modifiers) {
    // atomIndex < 0 is possible here.
    boolean shiftKey = ((modifiers & MouseManager.SHIFT) != 0);
    boolean alternateKey = ((modifiers & MouseManager.ALT) != 0);
    if (atomIndex < 0) {
      if (pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_PFAAT 
          && !shiftKey && !alternateKey)
        viewer.script("select none");
      //if (pickingMode == JmolConstants.PICKING_MEASURE
      //    || pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON)
      queuedAtomCount = 0;
      if (pickingMode != JmolConstants.PICKING_SPIN)
        return;
    }

    String value;
    Atom[] atoms;
    ModelSet modelSet = viewer.getModelSet();
    switch (pickingMode) {
    case JmolConstants.PICKING_OFF:
      break;
    case JmolConstants.PICKING_IDENT:
      viewer.setStatusAtomPicked(atomIndex, null);
      break;
    case JmolConstants.PICKING_MEASURE:
    case JmolConstants.PICKING_MEASURE_DISTANCE:
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 2)
        break;
      atoms = modelSet.getAtoms();
      float distance = atoms[queuedAtomIndexes[0]].distance(atoms[atomIndex]);
      value = "Distance " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(atomIndex) + " : " + distance;
      viewer.setStatusMeasurePicked(2, value);
      if (pickingMode == JmolConstants.PICKING_MEASURE
          || pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON)
        toggleMeasurement(2);
      break;
    case JmolConstants.PICKING_MEASURE_ANGLE:
      if (queuedAtomCount >= 3)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 3)
        break;
      atoms = modelSet.getAtoms();
      float angle = Measure.computeAngle(atoms[queuedAtomIndexes[0]], 
          atoms[queuedAtomIndexes[1]],
          atoms[atomIndex], true);
      value = "Angle " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[1]) + " - "
          + viewer.getAtomInfo(atomIndex) + " : " + angle;
      viewer.setStatusMeasurePicked(3, value);
      if (pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON)
        toggleMeasurement(3);
      break;
    case JmolConstants.PICKING_MEASURE_TORSION:
      if (queuedAtomCount >= 4)
        queuedAtomCount = 0;
      queueAtom(atomIndex);
      if (queuedAtomCount < 4)
        break;
      atoms = modelSet.getAtoms();
      float torsion = Measure.computeTorsion(atoms[queuedAtomIndexes[0]], 
          atoms[queuedAtomIndexes[1]],
          atoms[queuedAtomIndexes[2]],
          atoms[atomIndex], true);
      value = "Torsion " + viewer.getAtomInfo(queuedAtomIndexes[0]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[1]) + " - "
          + viewer.getAtomInfo(queuedAtomIndexes[2]) + " - "
          + viewer.getAtomInfo(atomIndex) + " : " + torsion;
      viewer.setStatusMeasurePicked(4, value);
      if (pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON)
        toggleMeasurement(4);
      break;
    case JmolConstants.PICKING_LABEL:
      viewer.script("set labeltoggle {atomindex="+atomIndex+"}");
      break;
    case JmolConstants.PICKING_CENTER:
      viewer.script("zoomTo (atomindex=" + atomIndex+")");
      break;
    case JmolConstants.PICKING_SELECT_ATOM:
      applyMouseStyle("atomIndex="+atomIndex, shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_GROUP:
      applyMouseStyle("within(group, atomIndex=" + atomIndex+")", shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_CHAIN:
      applyMouseStyle("within(chain, atomIndex=" + atomIndex+")", shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_MOLECULE:
      applyMouseStyle("visible and within(molecule, atomIndex=" + atomIndex+")", shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_SITE:
      applyMouseStyle("visible and within(site, atomIndex=" + atomIndex+")", shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SELECT_ELEMENT:
      applyMouseStyle("visible and within(element, atomIndex=" + atomIndex+")", shiftKey, alternateKey);
      viewer.clearClickCount();
      break;
    case JmolConstants.PICKING_SPIN:
      if (viewer.getSpinOn()) {
        viewer.script("spin off");
        break;
      }
      if (queuedAtomCount >= 2)
        queuedAtomCount = 0;
      if (queuedAtomCount == 1 && queuedAtomIndexes[0] == atomIndex)
        break;
      if (atomIndex >= 0)
        queueAtom(atomIndex);
      if (queuedAtomCount < 2) {
        viewer.scriptStatus(queuedAtomCount == 1 ?
            GT._("pick one more atom in order to spin the model around an axis") :
            GT._("pick two atoms in order to spin the model around an axis"));
        break;
      }
      viewer.script("spin (atomindex="+queuedAtomIndexes[0]+") (atomIndex="+atomIndex+") "+viewer.getPickingSpinRate());
    }
  }

  private void queueAtom(int atomIndex) {
    queuedAtomIndexes[queuedAtomCount++] = atomIndex;
    viewer.setStatusAtomPicked(atomIndex, "Atom #" + queuedAtomCount + ":" +
                        viewer.getAtomInfo(atomIndex));
  }

  private void toggleMeasurement(int nAtoms) {
    countPlusIndices[0] = nAtoms;
    int iLast = -1;
    int iThis;
    for (int i = 0; i < nAtoms; i++) {
      if (iLast == (iThis = queuedAtomIndexes[i])) {
        queuedAtomCount = i;
        return;
      }
      iLast = countPlusIndices[i + 1] = iThis;
    }
    viewer.script(Measurement.getMeasurementScript(countPlusIndices));
  }
  
  private void applyMouseStyle(String item, boolean shiftKey, boolean alternateKey) {
    item = "(" + item + ")";
    if (pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_PFAAT) {
      if (shiftKey && alternateKey)
        viewer.script("select selected and not " + item);
      else if (shiftKey)
        viewer.script("select selected tog " + item); //toggle
      else if (alternateKey)
        viewer.script("select selected or " + item);
      else
        viewer.script("select " + item);
    } else {
      if (shiftKey | pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_CHIME)
        viewer.script("select selected tog " + item); //toggle
      else
        viewer.script("select " + item);
    }
  }  
}
