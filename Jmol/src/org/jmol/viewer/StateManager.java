/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;

import java.util.Hashtable;
import java.util.BitSet;
import java.util.Enumeration;
import java.text.DecimalFormat;

//import org.jmol.util.Logger;

class StateManager {
  GlobalSettings globalSettings;
  Viewer viewer;
  Hashtable saved = new Hashtable();
  String lastOrientation = "";
  String lastConnections = "";
  String lastSelected = "";

  StateManager(Viewer viewer) {
    globalSettings = new GlobalSettings();
    this.viewer = viewer;
  }

  void clear(GlobalSettings global) {
    global.clear();
    //other state clearing? -- place here
  }

  void setCrystallographicDefaults() {
    //axes on and mode unitCell; unitCell on; perspective depth off;
    viewer.setShapeSize(JmolConstants.SHAPE_AXES, 200);
    viewer.setShapeSize(JmolConstants.SHAPE_UCCAGE, -1);
    viewer.setAxesModeUnitCell(true);
    viewer.setPerspectiveDepth(false);
  }

  void setCommonDefaults() {
    viewer.zoomToPercent(100);
    viewer.setPerspectiveDepth(true);
    viewer.setPercentVdwAtom(JmolConstants.DEFAULT_PERCENT_VDW_ATOM);
    viewer.setBondTolerance(JmolConstants.DEFAULT_BOND_TOLERANCE);
    viewer.setMinBondDistance(JmolConstants.DEFAULT_MIN_BOND_DISTANCE);
    viewer.setMarBond(JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
  }

  void setJmolDefaults() {
    setCommonDefaults();
    viewer.setDefaultColors("jmol");
    viewer.setAxesOrientationRasmol(false);
    viewer.setZeroBasedXyzRasmol(false);
  }

  void setRasmolDefaults() {
    setCommonDefaults();
    viewer.setDefaultColors("rasmol");
    viewer.setAxesOrientationRasmol(true);
    viewer.setZeroBasedXyzRasmol(true);
    viewer.setPercentVdwAtom(0);
    viewer.setMarBond((short) 1);
  }

  private DecimalFormat[] formatters;

  private static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
    "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };

  String formatDecimal(float value, int decimalDigits) {
    if (decimalDigits < 0)
      return "" + value;
    if (formatters == null)
      formatters = new DecimalFormat[formattingStrings.length];
    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    DecimalFormat formatter = formatters[decimalDigits];
    if (formatter == null)
      formatter = formatters[decimalDigits] = new DecimalFormat(
          formattingStrings[decimalDigits]);
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

  String listSavedStates() {
    String names = "";
    Enumeration e = saved.keys();
    while (e.hasMoreElements())
      names += "\n" + e.nextElement();
    return names;
  }

  void saveSelection(String saveName, BitSet bsSelected) {
    saveName = lastSelected = "Selected_" + saveName;
    BitSet bs = (BitSet) bsSelected.clone();
    saved.put(saveName, bs);
  }

  boolean restoreSelection(String saveName) {
    String name = (saveName.length() > 0 ? "Selected_" + saveName
        : lastSelected);
    BitSet bsSelected = (BitSet) saved.get(name);
    if (bsSelected == null) {
      viewer.select(new BitSet(), false);
      return false;
    }
    viewer.select(bsSelected, false);
    return true;
  }

  void saveOrientation(String saveName) {
    Orientation o = new Orientation();
    o.saveName = lastOrientation = "Orientation_" + saveName;
    saved.put(o.saveName, o);
  }

  boolean restoreOrientation(String saveName, float timeSeconds) {
    String name = (saveName.length() > 0 ? "Orientation_" + saveName
        : lastOrientation);
    Orientation o = (Orientation) saved.get(name);
    if (o == null)
      return false;
    o.restore(timeSeconds);
    //    Logger.info(listSavedStates());
    return true;
  }

  class Orientation {

    String saveName;

    Matrix3f rotationMatrix = new Matrix3f();
    float xTrans, yTrans;
    float zoom, rotationRadius;
    Point3f center = new Point3f();
    boolean windowCenteredFlag;

    Orientation() {
      viewer.getRotation(rotationMatrix);
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
      zoom = viewer.getZoomPercentFloat();
      center.set(viewer.getRotationCenter());
      windowCenteredFlag = viewer.isWindowCentered();
      rotationRadius = viewer.getRotationRadius();
    }

    void restore(float timeSeconds) {
      viewer.setWindowCentered(windowCenteredFlag);
      viewer.moveTo(timeSeconds, rotationMatrix, center, zoom, xTrans, yTrans,
          rotationRadius);
    }
  }

  void saveBonds(String saveName) {
    Connections b = new Connections();
    b.saveName = lastConnections = "Bonds_" + saveName;
    saved.put(b.saveName, b);
  }

  boolean restoreBonds(String saveName) {
    String name = (saveName.length() > 0 ? "Bonds_" + saveName
        : lastConnections);
    Connections c = (Connections) saved.get(name);
    if (c == null)
      return false;
    c.restore();
    //    Logger.info(listSavedStates());
    return true;
  }

  class Connections {

    String saveName;
    int bondCount;
    Connection[] connections;

    Connections() {
      Frame frame = viewer.getFrame();
      if (frame == null)
        return;
      bondCount = frame.bondCount;
      connections = new Connection[bondCount + 1];
      Bond[] bonds = frame.bonds;
      for (int i = bondCount; --i >= 0;) {
        Bond b = bonds[i];
        connections[i] = new Connection(b.atom1.atomIndex, b.atom2.atomIndex,
            b.mad, b.colix, b.order, b.shapeVisibilityFlags);
      }
    }

    class Connection {
      int atomIndex1;
      int atomIndex2;
      short mad;
      short colix;
      short order;
      int shapeVisibilityFlags;

      Connection(int atom1, int atom2, short mad, short colix, short order,
          int shapeVisibilityFlags) {
        atomIndex1 = atom1;
        atomIndex2 = atom2;
        this.mad = mad;
        this.colix = colix;
        this.order = order;
        this.shapeVisibilityFlags = shapeVisibilityFlags;
      }
    }

    void restore() {
      Frame frame = viewer.getFrame();
      if (frame == null)
        return;
      frame.deleteAllBonds();
      for (int i = bondCount; --i >= 0;) {
        Connection c = connections[i];
        if (c.atomIndex1 >= frame.atomCount || c.atomIndex2 >= frame.atomCount)
          continue;
        Bond b = frame.bondAtoms(frame.atoms[c.atomIndex1],
            frame.atoms[c.atomIndex2], c.order, c.mad);
        b.colix = c.colix;
        b.shapeVisibilityFlags = c.shapeVisibilityFlags;
      }
    }
  }

  class GlobalSettings {

    /*
     *  Mostly these are just saved and restored directly from Viewer.
     *  They are collected here for reference and to ensure that no 
     *  methods are written that bypass viewer's get/set methods.
     *  
     *  Because these are not Frame variables, they should persist past
     *  a new file loading. There is some question in my mind whether all
     *  should be in this category.
     *  
     */

    GlobalSettings() {
      //
    }

    //file loading

    boolean zeroBasedXyzRasmol = false;
    boolean forceAutoBond      = false;

    char inlineNewlineChar     = '|';
    String defaultLoadScript   = "";
    private final Point3f ptDefaultLattice = new Point3f();

    void setDefaultLattice(Point3f ptLattice) {
      ptDefaultLattice.set(ptLattice);
    }
    
    Point3f getDefaultLatticePoint() {
      return ptDefaultLattice;
    }

    int[] getDefaultLatticeArray() {
      int[] A = new int[4];
      A[1] = (int) ptDefaultLattice.x;
      A[2] = (int) ptDefaultLattice.y;
      A[3] = (int) ptDefaultLattice.z;
      return A;
    }

    void clear() {

      // OK, here is where we would put any 
      // "global" settings that
      // need to be reset whenever a file is loaded

    }

    //centering and perspective

    boolean allowCameraMoveFlag = true;
    boolean adjustCameraFlag = true;

    //solvent

    boolean solventOn = false;
    float solventProbeRadius = 1.2f;

    //measurements

    boolean measureAllModels;

    //rendering

    boolean enableFullSceneAntialiasing = false;
    boolean greyscaleRendering          = false;
    boolean zoomLarge                   = true; //false would be like Chime
    boolean dotsSelectedOnlyFlag        = false;
    boolean dotSurfaceFlag              = true;
    boolean displayCellParameters       = true;
    boolean showHiddenSelectionHalos    = false;
    boolean showMeasurements            = true;
    boolean frankOn                     = false;
    
    //atoms and bonds

    boolean bondSelectionModeOr = false;
    boolean showMultipleBonds   = true;
    boolean showHydrogens       = true;
    boolean ssbondsBackbone     = false;
    boolean hbondsBackbone      = false;
    boolean hbondsSolid         = false;

    int percentVdwAtom    = JmolConstants.DEFAULT_PERCENT_VDW_ATOM;
    short marBond         = JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
    float bondTolerance   = JmolConstants.DEFAULT_BOND_TOLERANCE;
    float minBondDistance = JmolConstants.DEFAULT_MIN_BOND_DISTANCE;
    byte modeMultipleBond = JmolConstants.MULTIBOND_NOTSMALL;
    int defaultVectorMad  = 0;

    //labels

    boolean labelsGroupFlag        = false;
    boolean labelsFrontFlag        = false;
    boolean labelPointerBackground = true;
    boolean labelPointerBox        = true;
    boolean labelPointerNoBox      = false;
    
    int labelOffsetX        = JmolConstants.LABEL_DEFAULT_X_OFFSET;
    int labelOffsetY        = JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    int pointsLabelFontSize = JmolConstants.LABEL_DEFAULT_FONTSIZE;

    //secondary structure + Rasmol

    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting   = true;
    boolean cartoonRocketFlag     = false;
    boolean ribbonBorder          = false;
    boolean chainCaseSensitive    = false;
    boolean rangeSelected         = false;

    boolean traceAlpha            = true;
    boolean highResolutionFlag    = false;
    int ribbonAspectRatio         = 16;
    int hermiteLevel              = 0;
    float sheetSmoothing          = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints

    //misc

    boolean hideNameInPopup    = false;
    boolean disablePopupMenu   = false;

    int axesMode               = JmolConstants.AXES_MODE_BOUNDBOX;
    int pickingSpinRate        = 10;
    
    String helpPath            = null;
    String defaultHelpPath     = JmolConstants.DEFAULT_HELP_PATH;
    String propertyStyleString = "";


    //testing

    boolean debugScript = false;
    boolean testFlag1 = false;
    boolean testFlag2 = false;
    boolean testFlag3 = false;
    boolean testFlag4 = false;

    // measurements

    //controlled access:
    private String measureDistanceUnits = "nanometers";
    boolean setMeasureDistanceUnits(String units) {
      if (units.equalsIgnoreCase("angstroms"))
        measureDistanceUnits = "angstroms";
      else if (units.equalsIgnoreCase("nanometers")
          || units.equalsIgnoreCase("nm"))
        measureDistanceUnits = "nanometers";
      else if (units.equalsIgnoreCase("picometers")
          || units.equalsIgnoreCase("pm"))
        measureDistanceUnits = "picometers";
      else
        return false;
      return true;
    }
    
    String getMeasureDistanceUnits() {
      return measureDistanceUnits;
    }
  }

  ///////// state serialization 

  static String encodeBitset(BitSet bs) {
    if (bs == null)
      return "({})";
    StringBuffer s = new StringBuffer("({");
    int imax = bs.size();
    int iLast = -1;
    int iFirst = -2;
    int i = -1;
    while (++i <= imax) {
      boolean isSet = bs.get(i);
      if (i == imax || iLast >= 0 && !isSet) {
        if (iLast >= 0 && iFirst != iLast)
          s.append((iFirst == iLast - 1 ? " " : ":") + iLast);
        if (i == imax) {
          s.append("})");
          return s.toString();
        }
        iLast = -1;
      }
      if (bs.get(i)) {
        if (iLast < 0) {
          s.append((iFirst == -2 ? "" : " ") + i);
          iFirst = i;
        }
        iLast = i;
      }
    }
    return "({})"; // impossible return
  }
 
  static BitSet decodeBitset (String strBitset) {
    BitSet bs = new BitSet();
    int len = strBitset.length();
    int iPrev = -1;
    int iThis = -2;
    char ch;
    if (len < 3)
      return bs;
    for (int i = 0; i < len; i++) {
      switch (ch = strBitset.charAt(i)) {
      case '}':
      case '{':
      case ' ':
        if (iThis < 0)
          break;
        if (iPrev < 0) 
          iPrev = iThis;
        for (int j = iPrev; j<= iThis; j++)
          bs.set(j);
        iPrev = -1;
        iThis = -2;
        break;
      case ':':
        iPrev = iThis;
        iThis = -2;
        break;
      default:
        if (Character.isDigit(ch)) {
          if (iThis < 0)
            iThis = 0;
          iThis = (iThis << 3) + (iThis << 1) + (ch - '0');
        }
      }
    }
    return bs;
  }
  
  static String escape(String str) {
    int pt = -2;
    while ((pt = str.indexOf("\"", pt + 2)) >= 0)
      str = str.substring(0, pt) + '\\' + str.substring(pt);
    return "\"" + str + "\"";
  }
  
  static String encloseCoord(Point3f xyz) {
    return "{" + xyz.x + " " + xyz.y + " " + xyz.z +"}";
  }
  
  static String getCommands(Hashtable ht) {
    return getCommands(ht, null, -1, "select");
  }

  static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll) {
    return getCommands(htDefine, htMore, nAll, "select");
  }

  static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll, String selectCmd) {
    StringBuffer s = new StringBuffer();
    String setPrev = getCommands(htDefine, s, null, nAll, selectCmd);
    if (htMore != null)
      getCommands(htMore, s, setPrev, nAll, selectCmd);
    return s.toString();
  }

  static String getCommands(Hashtable ht, StringBuffer s, String setPrev, int nAll, String selectCmd) {
    if (ht == null)
      return "";
    String strAll = "({0:"+ (nAll - 1)+ "})";
   Enumeration e = ht.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String set = encodeBitset((BitSet) ht.get(key));
      if (set.length() == 4)
        continue;
      if (!set.equals(setPrev)) {
        if (set.equals(strAll)) {
          s.append(selectCmd);
          s.append(" *;");
        } else {
        s.append(selectCmd);
        s.append(" ");
        s.append(set);
        s.append(";\n");
        }
      }
      setPrev = set;
      if (key.indexOf("-") < 0) // - for key means none required
      s.append(key + ";");
      s.append("\n");
    }
    return setPrev;
  }
}
