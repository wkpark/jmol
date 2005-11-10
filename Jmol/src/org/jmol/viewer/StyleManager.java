/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import java.text.DecimalFormat;

class StyleManager {

  Viewer viewer;

  StyleManager(Viewer viewer) {
    this.viewer = viewer;
  }

  int percentVdwAtom = 20;
  void setPercentVdwAtom(int percentVdwAtom) {
    this.percentVdwAtom = percentVdwAtom;
  }

  short marBond = 100;
  void setMarBond(short marBond) {
    this.marBond = marBond;
  }

  byte modeMultipleBond = JmolConstants.MULTIBOND_SMALL;
  void setModeMultipleBond(byte modeMultipleBond) {
    this.modeMultipleBond = modeMultipleBond;
  }

  boolean showMultipleBonds = true;
  void setShowMultipleBonds(boolean showMultipleBonds) {
    this.showMultipleBonds = showMultipleBonds;
  }

  boolean showAtoms = true;
  void setShowAtoms(boolean showAtoms) {
    this.showAtoms = showAtoms;
  }

  boolean showBonds = true;
  void setShowBonds(boolean showBonds) {
    this.showBonds = showBonds;
  }

  boolean showHydrogens = true;
  void setShowHydrogens(boolean showHydrogens) {
    this.showHydrogens = showHydrogens;
  }

  boolean showVectors = true;
  void setShowVectors(boolean showVectors) {
    this.showVectors = showVectors;
  }

  boolean showMeasurements = true;
  void setShowMeasurements(boolean showMeasurements) {
    this.showMeasurements = showMeasurements;
  }

  boolean showMeasurementLabels = true;
  void setShowMeasurementLabels(boolean showMeasurementLabels) {
    this.showMeasurementLabels = showMeasurementLabels;
  }

  String measureDistanceUnits = "nanometers";
  boolean setMeasureDistanceUnits(String units) {
    if (units.equalsIgnoreCase("angstroms"))
      measureDistanceUnits = "angstroms";
    else if (units.equalsIgnoreCase("nanometers"))
      measureDistanceUnits = "nanometers";
    else if (units.equalsIgnoreCase("picometers"))
      measureDistanceUnits = "picometers";
    else
      return false;
    return true;
  }

  String propertyStyleString = "";
  void setPropertyStyleString(String s) {
    propertyStyleString = s;
  }

  boolean wireframeRotation = false;
  void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
  }

  boolean zeroBasedXyzRasmol = false;
  void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
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

  void setJmolDefaults() {
    setCommonDefaults();
    viewer.setDefaultColors("jmol");
    viewer.setAxesOrientationRasmol(false);
    setZeroBasedXyzRasmol(false);
  }

  void setRasmolDefaults() {
    setCommonDefaults();
    viewer.setDefaultColors("rasmol");
    viewer.setAxesOrientationRasmol(true);
    setZeroBasedXyzRasmol(true);
    viewer.setPercentVdwAtom(0);
    viewer.setMarBond((short)1);
  }

  boolean frankOn;
  void setFrankOn(boolean frankOn) {
    this.frankOn = frankOn;
  }

  boolean ssbondsBackbone;
  void setSsbondsBackbone(boolean ssbondsBackbone) {
    this.ssbondsBackbone = ssbondsBackbone;
  }

  boolean hbondsBackbone;
  void setHbondsBackbone(boolean hbondsBackbone) {
    this.hbondsBackbone = hbondsBackbone;
  }

  boolean hbondsSolid;
  void setHbondsSolid(boolean hbondsSolid) {
    this.hbondsSolid = hbondsSolid;
  }

  /****************************************************************
   * label related
   ****************************************************************/

  int pointsLabelFontSize = JmolConstants.LABEL_DEFAULT_FONTSIZE;
  void setLabelFontSize(int points) {
    this.pointsLabelFontSize = points <= 0 ? JmolConstants.LABEL_DEFAULT_FONTSIZE : points;
  }

  int labelOffsetX = JmolConstants.LABEL_DEFAULT_X_OFFSET;
  int labelOffsetY = JmolConstants.LABEL_DEFAULT_Y_OFFSET;
  void setLabelOffset(int offsetX, int offsetY) {
    labelOffsetX = offsetX;
    labelOffsetY = offsetY;
  }

  static String[] formattingStrings = {
    "0", "0.0", "0.00", "0.000", "0.0000", "0.00000",
    "0.000000", "0.0000000", "0.00000000", "0.000000000"
  };

  DecimalFormat[] formatters;

  String formatDecimal(float value, int decimalDigits) {
    if (decimalDigits < 0)
      return "" + value;
    if (formatters == null)
      formatters = new DecimalFormat[formattingStrings.length];
    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    DecimalFormat formatter = formatters[decimalDigits];
    if (formatter == null)
      formatter = formatters[decimalDigits] =
        new DecimalFormat(formattingStrings[decimalDigits]);
    return formatter.format(value);
  }
}
