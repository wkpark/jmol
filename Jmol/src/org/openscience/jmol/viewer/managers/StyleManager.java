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

import org.openscience.jmol.viewer.*;

import java.awt.Color;
import java.awt.Font;

public class StyleManager {

  JmolViewer viewer;

  public StyleManager(JmolViewer viewer) {
    this.viewer = viewer;
  }

  public int percentVdwAtom = 20;
  public void setPercentVdwAtom(int percentVdwAtom) {
    this.percentVdwAtom = percentVdwAtom;
  }

  public short marBond = 100;
  public void setMarBond(short marBond) {
    this.marBond = marBond;
  }

  public byte modeMultipleBond = JmolConstants.MULTIBOND_SMALL;
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

  public short measurementMad = -1;
  public void setMeasurementMad(short measurementMad) {
    System.out.println("measurementMad=" + measurementMad);
    this.measurementMad = measurementMad;
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

  public boolean zeroBasedXyzRasmol = false;
  public void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    this.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
  }

  void setCommonDefaults() {
    viewer.zoomToPercent(100);
    viewer.setPercentVdwAtom(20);
    viewer.setWireframeRotation(false);
    viewer.setPerspectiveDepth(true);
    viewer.setBondTolerance(0.45f);
    viewer.setMinBondDistance(0.40f);
    viewer.setMarBond((short)150);
  }

  public void setJmolDefaults() {
    setCommonDefaults();
    viewer.setColorScheme("jmol");
    viewer.setAxesOrientationRasmol(false);
    setZeroBasedXyzRasmol(false);
  }

  public void setRasmolDefaults() {
    setCommonDefaults();
    viewer.setColorScheme("rasmol");
    viewer.setAxesOrientationRasmol(true);
    setZeroBasedXyzRasmol(true);
    viewer.setPercentVdwAtom(0);
    viewer.setMarBond((short)1);
  }

  public boolean showFrank;
  public void setShowFrank(boolean showFrank) {
    this.showFrank = showFrank;
  }

  public boolean ssbondsBackbone;
  public void setSsbondsBackbone(boolean ssbondsBackbone) {
    this.ssbondsBackbone = ssbondsBackbone;
  }

  public boolean hbondsBackbone;
  public void setHbondsBackbone(boolean hbondsBackbone) {
    this.hbondsBackbone = hbondsBackbone;
  }

  /****************************************************************
   * label related
   ****************************************************************/
  public String strFontFace = JmolConstants.LABEL_DEFAULT_FONTFACE;
  public void setFontFace(String strFontFace) {
    this.strFontFace = strFontFace;
  }

  Font[] fonts = new Font[JmolConstants.LABEL_MAXIMUM_FONTSIZE -
                          JmolConstants.LABEL_MINIMUM_FONTSIZE + 1];

  public Font getFontOfSize(int points) {
    if (points < JmolConstants.LABEL_MINIMUM_FONTSIZE)
      points = JmolConstants.LABEL_MINIMUM_FONTSIZE;
    else if (points > JmolConstants.LABEL_MAXIMUM_FONTSIZE)
      points = JmolConstants.LABEL_MAXIMUM_FONTSIZE;
    int index = points - JmolConstants.LABEL_MINIMUM_FONTSIZE;
    Font font = fonts[index];
    if (font == null)
      font = fonts[index] = new Font(strFontFace, Font.PLAIN, points);
    return font;
  }

  public Font getLabelFont() {
    return getFontOfSize(pointsLabelFontSize);
  }

  public Font getLabelFont(int diameter) {
    return getFontOfSize(pointsLabelFontSize);
  }

  public int pointsLabelFontSize = JmolConstants.LABEL_DEFAULT_FONTSIZE;
  public void setLabelFontSize(int points) {
    this.pointsLabelFontSize = points <= 0 ? JmolConstants.LABEL_DEFAULT_FONTSIZE : points;
  }

  public int labelOffsetX = JmolConstants.LABEL_DEFAULT_X_OFFSET;
  public int labelOffsetY = JmolConstants.LABEL_DEFAULT_Y_OFFSET;
  public void setLabelOffset(int offsetX, int offsetY) {
    labelOffsetX = offsetX;
    labelOffsetY = offsetY;
  }

}
