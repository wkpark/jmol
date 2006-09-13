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

import org.jmol.util.Logger;

class StyleManager {

  Viewer viewer;

  StyleManager(Viewer viewer) {
    this.viewer = viewer;
  }

  final static String DEFAULT_HELP_PATH = "http://www.stolaf.edu/academics/chemapps/jmol/docs/index.htm?search=";
  String getDefaultHelpPath() {
    return DEFAULT_HELP_PATH;
  }
  
  void setCommonDefaults() {
    viewer.zoomToPercent(100);
    viewer.setPercentVdwAtom(DEFAULT_PERCENT_VDW_ATOM);
    viewer.setPerspectiveDepth(true);
    viewer.setBondTolerance(DEFAULT_BOND_TOLERANCE);
    viewer.setMinBondDistance(DEFAULT_MIN_BOND_DISTANCE);
    viewer.setMarBond((short)(DEFAULT_BOND_RADIUS * 1000));
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

  final static int DEFAULT_PERCENT_VDW_ATOM = 20;
  int percentVdwAtom = DEFAULT_PERCENT_VDW_ATOM;
  
  final static float DEFAULT_BOND_RADIUS = 0.15f;
  short marBond = (short) (DEFAULT_BOND_RADIUS * 1000);

  //angstroms of slop ... from OpenBabel ... mth 2003 05 26
  final static float DEFAULT_BOND_TOLERANCE = 0.45f;
  float bondTolerance = DEFAULT_BOND_TOLERANCE;

  // minimum acceptable bonding distance ... from OpenBabel ... mth 2003 05 26
  final static float DEFAULT_MIN_BOND_DISTANCE = 0.4f;
  float minBondDistance = DEFAULT_MIN_BOND_DISTANCE;

  void setPercentVdwAtom(int percentVdwAtom) {
    if (percentVdwAtom != this.percentVdwAtom) 
      Logger.info("default percent Van der Waal radius set to " + percentVdwAtom);
    this.percentVdwAtom = percentVdwAtom;
  }

  void setMarBond(short marBond) {
    if (marBond != this.marBond) 
      Logger.info("default bond radius set to " + (marBond/1000f));
    this.marBond = marBond;
  }

  void setBondTolerance(float bondTolerance) {
    if (bondTolerance != this.bondTolerance) 
      Logger.info("default bond tolerance set to " + bondTolerance);
    this.bondTolerance = bondTolerance;
  }
  
  void setMinBondDistance(float minBondDistance) {
    if (minBondDistance != this.minBondDistance) 
      Logger.info("default minimum bond distance set to " + minBondDistance);
    this.minBondDistance = minBondDistance;
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

  int defaultVectorMad = 0;
  void setDefaultVectorMad(int mad) {
    this.defaultVectorMad = mad;
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
    else if (units.equalsIgnoreCase("nanometers") ||
        units.equalsIgnoreCase("nm"))
      measureDistanceUnits = "nanometers";
    else if (units.equalsIgnoreCase("picometers") ||
        units.equalsIgnoreCase("pm"))
      measureDistanceUnits = "picometers";
    else
      return false;
    return true;
  }

  String propertyStyleString = "";
  void setPropertyStyleString(String s) {
    propertyStyleString = s;
  }

  boolean zeroBasedXyzRasmol = false;
  void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    this.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
  }

  float getBondTolerance() {
    return bondTolerance;
  }

  float getMinBondDistance() {
    return minBondDistance;
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
  
  String getStandardLabelFormat() {
    // from the RasMol 2.6b2 manual: RasMol uses the label
    // "%n%r:%c.%a" if the molecule contains more than one chain:
    // "%e%i" if the molecule has only a single residue (a small molecule) and
    // "%n%r.%a" otherwise.
    String strLabel;
    int modelCount = viewer.getModelCount();
    if (viewer.getChainCount() > modelCount)
      strLabel = "[%n]%r:%c.%a";
    else if (viewer.getGroupCount() <= modelCount)
      strLabel = "%e%i";
    else
      strLabel = "[%n]%r.%a";
    if (viewer.getModelCount() > 1)
      strLabel += "/%M";
    return strLabel;
  }
  
  void setCrystallographicDefaults() { 
    //axes on and mode unitCell; unitCell on; perspective depth off;
    viewer.setShapeSize(JmolConstants.SHAPE_AXES, 200);
    viewer.setShapeSize(JmolConstants.SHAPE_UCCAGE, 1);
    viewer.setAxesModeUnitCell(true);
    viewer.setPerspectiveDepth(false);
  }

}
