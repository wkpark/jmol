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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.datamodel.AtomShape;

import java.awt.Color;
import java.awt.Font;

public class StyleManager {

  JmolViewer viewer;

  public StyleManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public byte styleAtom = JmolViewer.SHADED;
  public void setStyleAtom(byte styleAtom) {
    this.styleAtom = styleAtom;
  }

  public int percentVdwAtom = 20;
  public void setPercentVdwAtom(int percentVdwAtom) {
    this.percentVdwAtom = percentVdwAtom;
  }

  public byte styleBond = JmolViewer.SHADED;
  public void setStyleBond(byte styleBond) {
    this.styleBond = styleBond;
  }

  public short marBond = 100;
  public void setMarBond(short marBond) {
    this.marBond =marBond;
  }

  public byte modeMultipleBond = JmolViewer.MB_SMALL;
  public void setModeMultipleBond(byte modeMultipleBond) {
    this.modeMultipleBond = modeMultipleBond;
  }

  public boolean showMultipleBonds = true;
  public void setShowMultipleBonds(boolean showMultipleBonds) {
    this.showMultipleBonds = showMultipleBonds;
  }

  public boolean showAtoms = true;
  public void setShowAtoms(boolean showAtoms) {
    this.showAtoms = showAtoms;
  }

  public boolean showBonds = true;
  public void setShowBonds(boolean showBonds) {
    this.showBonds = showBonds;
  }

  public boolean showHydrogens = true;
  public void setShowHydrogens(boolean showHydrogens) {
    this.showHydrogens = showHydrogens;
  }

  public boolean showVectors = true;
  public void setShowVectors(boolean showVectors) {
    this.showVectors = showVectors;
  }

  public boolean showMeasurements = true;
  public void setShowMeasurements(boolean showMeasurements) {
    this.showMeasurements = showMeasurements;
  }

  public boolean showMeasurementLabels = true;
  public void setShowMeasurementLabels(boolean showMeasurementLabels) {
    this.showMeasurementLabels = showMeasurementLabels;
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
  }

  public String propertyStyleString = "";
  public void setPropertyStyleString(String s) {
    propertyStyleString = s;
  }

  public boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
  }

  void setCommonDefaults() {
    viewer.zoomToPercent(100);
    viewer.setPercentVdwAtom(20);
    viewer.setWireframeRotation(false);
    viewer.setPerspectiveDepth(true);
    viewer.setBondTolerance(0.45f);
    viewer.setMinBondDistance(0.40f);
    viewer.setMarBond((short)100);
  }

  void setTextColors(Color color) {
    viewer.setColorLabel(color);
    viewer.setColorDistance(color);
    viewer.setColorAngle(color);
    viewer.setColorDihedral(color);
  }

  public void setJmolDefaults() {
    setCommonDefaults();
    viewer.setStyleBond(JmolViewer.SHADED);
    viewer.setStyleAtom(JmolViewer.SHADED);
    viewer.setColorBackground(Color.white);
    setTextColors(Color.black);
  }

  public void setRasmolDefaults() {
    setCommonDefaults();
    viewer.setStyleBond(JmolViewer.WIREFRAME);
    viewer.setStyleAtom(JmolViewer.NONE);
    viewer.setColorBackground(Color.black);
    setTextColors(Color.white);
  }
}
