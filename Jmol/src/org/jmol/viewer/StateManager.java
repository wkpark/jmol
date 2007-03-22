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
import javax.vecmath.Tuple3f;
import javax.vecmath.Point4f;
import javax.vecmath.Matrix3f;

import java.util.Hashtable;
import java.util.BitSet;
import java.util.Enumeration;

import org.jmol.g3d.Graphics3D;
import org.jmol.util.CommandHistory;
import org.jmol.util.TextFormat;
import org.jmol.util.Parser;

import java.util.Arrays;

class StateManager {

  

  /* steps in adding a global variable:
 
  In Viewer:
  
  1. add a check in setIntProperty or setBooleanProperty or setFloat.. or setString...
  2. create new set/get methods
  
  In StateManager
  
  3. create the global.xxx varaible
  4. in registerParameter() register it so that it shows up as having a value in math
  
  */

  final static int OBJ_BACKGROUND = 0;
  final static int OBJ_AXIS1 = 1;
  final static int OBJ_AXIS2 = 2;
  final static int OBJ_AXIS3 = 3;
  final static int OBJ_BOUNDBOX = 4;
  final static int OBJ_UNITCELL = 5;
  final static int OBJ_FRANK = 6;
  final static int OBJ_MAX = 7;
  final static String objectNameList =
      "background axis1      axis2      axis3      boundbox   unitcell   frank      ";
  
  static int getObjectIdFromName(String name) {
    if (name == null)
      return -1;
    int objID = objectNameList.indexOf(name.toLowerCase());
    return (objID < 0 ? objID : objID / 11);
  }
  
  static String getObjectNameFromId(int objId) {
    if (objId < 0 || objId >= OBJ_MAX)
      return null;
   return objectNameList.substring(objId * 11,objId*11 + 11).trim();
  }
  
  Viewer viewer;
  Hashtable saved = new Hashtable();
  String lastOrientation = "";
  String lastConnections = "";
  String lastSelected = "";
  String lastState = "";

  StateManager(Viewer viewer) {
    this.viewer = viewer;
  }

  GlobalSettings getGlobalSettings() {
    GlobalSettings g = new GlobalSettings();
    g.registerAllValues();
    return g;  
  }
  
  void clear(GlobalSettings global) {
    global.clear();
    //other state clearing? -- place here
  }

  void setCrystallographicDefaults() {
    //axes on and mode unitCell; unitCell on; perspective depth off;
    viewer.setAxesModeUnitCell(true);
    viewer.setShowAxes(true);
    viewer.setShowUnitCell(true);
    viewer.setBooleanProperty("perspectiveDepth", false);
  }

  void setCommonDefaults() {
    viewer.setBooleanProperty("perspectiveDepth", true);
    viewer.setIntProperty("percentVdwAtom", JmolConstants.DEFAULT_PERCENT_VDW_ATOM);
    viewer.setFloatProperty("bondTolerance", JmolConstants.DEFAULT_BOND_TOLERANCE);
    viewer.setFloatProperty("minBondDistance", JmolConstants.DEFAULT_MIN_BOND_DISTANCE);
    viewer.setIntProperty("bondRadiusMilliAngstroms", JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
  }

  void setJmolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "Jmol");
    viewer.setBooleanProperty("axesOrientationRasmol", false);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", false);
  }

  void setRasMolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "RasMol");
    viewer.setBooleanProperty("axesOrientationRasmol", true);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", true);
    viewer.setIntProperty("percentVdwAtom", 0);
    viewer.setIntProperty("bondRadiusMilliAngstroms", 1);
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

  void saveState(String saveName) {
    saveName = lastState = "State_" + saveName;
    saved.put(saveName, viewer.getStateInfo());
  }

  String getSavedState(String saveName) {
    String name = (saveName.length() > 0 ? "State_" + saveName
        : lastState);
    String script = (String) saved.get(name);
    return (script == null ? "" : script); 
  }
  
  boolean restoreState(String saveName) {
    //not used -- more efficient just to run the script 
    String name = (saveName.length() > 0 ? "State_" + saveName
        : lastState);
    String script = (String) saved.get(name);
    if (script == null)
      return false;
    viewer.script(script + CommandHistory.NOHISTORYATALL_FLAG);
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
    Point3f navCenter = new Point3f();
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;
    boolean windowCenteredFlag;
    boolean navigationMode;

    Orientation() {
      viewer.getRotation(rotationMatrix);
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
      zoom = viewer.getZoomPercentFloat();
      center.set(viewer.getRotationCenter());
      windowCenteredFlag = viewer.isWindowCentered();
      rotationRadius = viewer.getRotationRadius();
      navigationMode = viewer.getNavigationMode(); 
      if (navigationMode) {
        navCenter = viewer.getNavigationOffset();
        xNav = viewer.getNavigationOffsetPercent('X');
        yNav = viewer.getNavigationOffsetPercent('Y');
        navDepth = viewer.getNavigationDepthPercent();
        navCenter = viewer.getNavigationCenter();
      }     
    }

    void restore(float timeSeconds) {
      viewer.setBooleanProperty("windowCentered", windowCenteredFlag);
      viewer.setBooleanProperty("navigationMode", navigationMode);
      viewer.moveTo(timeSeconds, rotationMatrix, center, zoom, xTrans, yTrans,
          rotationRadius, navCenter, xNav, yNav, navDepth);
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
            frame.atoms[c.atomIndex2], c.order, c.mad, null);
        b.colix = c.colix;
        b.shapeVisibilityFlags = c.shapeVisibilityFlags;
      }
      for (int i = bondCount; --i >= 0;)
        frame.bonds[i].index = i;
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reportAll", null);
    }
  }

  static boolean isMeasurementUnit(String units) {
    String u = ";" + units.toLowerCase() + ";";
    return (";angstroms;au;bohr;nanometers;nm;picometers;pm;".indexOf(u) >= 0);
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

    //lighting (see Graphics3D.Shade3D
    
    boolean specular     = true;
    int specularPercent  = 22;
    int specularExponent = 6;
    int specularPower    = 40;
    int diffusePercent   = 84;
    int ambientPercent   = 45;

    //file loading

    char inlineNewlineChar     = '|';    //pseudo static
    boolean appendNew          = true;
    boolean zeroBasedXyzRasmol = false;
    boolean forceAutoBond      = false;
    boolean autoBond           = true;
    boolean allowEmbeddedScripts = true;
    int percentVdwAtom    = JmolConstants.DEFAULT_PERCENT_VDW_ATOM;
    short bondRadiusMilliAngstroms         = JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
    float bondTolerance   = JmolConstants.DEFAULT_BOND_TOLERANCE;
    float minBondDistance = JmolConstants.DEFAULT_MIN_BOND_DISTANCE;
    String defaultLoadScript   = "";
    String defaultDirectory    = null;
    String loadFormat          = "http://www.rcsb.org/pdb/files/%FILE.pdb";

    /**
     *  these settings are determined when the file is loaded and are
     *  kept even though they might later change. So we list them here
     *  and ALSO let them be defined in the settings. 10.9.98 missed this. 
     *  
     * @return script command
     */
    String getLoadState() {
      // some commands register flags so that they will be 
      // restored in a saved state definition, but will not execute
      // now so that there is no chance any embedded scripts or
      // default load scripts will run and slow things down.
      StringBuffer str = new StringBuffer();
      appendCmd(str, "allowEmbeddedScripts = false");
      if (allowEmbeddedScripts)
        setParameterValue("allowEmbeddedScripts", true);
      appendCmd(str, "autoBond = " + autoBond);
      appendCmd(str, "appendNew = " + appendNew);
      if (viewer.getAxesOrientationRasmol())
        appendCmd(str, "axesOrientationRasmol = true");
      appendCmd(str, "bondRadiusMilliAngstroms = " + bondRadiusMilliAngstroms);
      appendCmd(str, "bondTolerance = " + bondTolerance);
      if (defaultDirectory != null)
        appendCmd(str, "defaultDirectory = " + escape(defaultDirectory));
      appendCmd(str, "defaultLattice = " + escape(ptDefaultLattice));
      appendCmd(str, "defaultLoadScript = \"\"");
      if (defaultLoadScript.length() > 0)
        setParameterValue("defaultLoadScript", defaultLoadScript);
      appendCmd(str, "loadFormat = " + escape(loadFormat));

      appendCmd(str, "forceAutoBond = " + forceAutoBond);
      appendCmd(str, "minBondDistance = " + minBondDistance);
      appendCmd(str, "percentVdwAtom = " + percentVdwAtom);
      if (zeroBasedXyzRasmol)
        appendCmd(str, "zeroBasedXyzRasmol = true");
      str.append("\n");
      return str.toString();
    }

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

      viewer.setShowAxes(false);
      viewer.setShowBbcage(false);
      viewer.setShowUnitCell(false);
      clearVolatileProperties();
    }

    //centering and perspective

    boolean allowRotateSelected = false;

    //solvent

    boolean solventOn = false;

    //measurements

    boolean measureAllModels    = false;
    boolean justifyMeasurements = false;
    String defaultDistanceLabel = "%VALUE %UNITS"; //also %_ and %a1 %a2 %m1 %m2, etc.
    String defaultAngleLabel    = "%VALUE %UNITS";
    String defaultTorsionLabel  = "%VALUE %UNITS";

    //rendering

    boolean enableFullSceneAntialiasing = false;
    boolean greyscaleRendering          = false;
    boolean zoomLarge                   = true; //false would be like Chime
    boolean dotsSelectedOnly            = false;
    boolean dotSurface              = true;
    boolean displayCellParameters       = true;
    boolean showHiddenSelectionHalos    = false;
    boolean showMeasurements            = true;
    boolean zShade                      = false;
    boolean dynamicMeasurements         = false;
    
    //atoms and bonds

    boolean bondModeOr = false;
    boolean showMultipleBonds   = true;
    boolean showHydrogens       = true;
    boolean ssbondsBackbone     = false;
    boolean hbondsBackbone      = false;
    boolean hbondsSolid         = false;

    byte modeMultipleBond = JmolConstants.MULTIBOND_NOTSMALL;

    //secondary structure + Rasmol

    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting   = true;
    boolean cartoonRockets        = false;
    boolean ribbonBorder          = false;
    boolean chainCaseSensitive    = false;
    boolean rangeSelected         = false;

    boolean traceAlpha            = true;
    boolean highResolutionFlag    = false;
    int ribbonAspectRatio         = 16;
    int strandCount               = 5;
    int hermiteLevel              = 0;
    float sheetSmoothing          = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints

    //misc

    boolean hideNameInPopup      = false;
    boolean disablePopupMenu     = false;
    float vibrationScale         = 1f;
    float vibrationPeriod        = 1f;
    float vectorScale            = 1f;
    float cameraDepth            = 3.0f;
    float solventProbeRadius     = 1.2f;
    int scriptDelay              = 0;
    int hoverDelayMs             = 500;
    boolean hideNavigationPoint  = false;
    boolean showNavigationPointAlways = false;
    String propertyColorScheme   = "roygb";
    boolean useNumberLocalization = true;
    float defaultTranslucent      = 0.5f;
    boolean autoFps               = false;
    
    
    
    // window
    
    int[] objColors            = new int[OBJ_MAX];
    boolean[] objStateOn       = new boolean[OBJ_MAX];
    short[] objMad             = new short[OBJ_MAX];
    String stereoState         = null;
    boolean navigationMode     = false;
    boolean navigationPeriodic = false;
    boolean navigationCentered = false;
    float navigationSpeed      = 5;

    String getWindowState() {
      StringBuffer str = new StringBuffer("# window state;\n# height "
          + viewer.getScreenHeight() + ";\n# width " + viewer.getScreenWidth()
          + ";\n");
      appendCmd(str, "initialize");
      appendCmd(str, "stateVersion = " + getParameter("_version"));
      appendCmd(str, "refreshing = false");
      for (int i = 0; i < OBJ_MAX; i++)
        if (objColors[i] != 0)
          appendCmd(str, getObjectNameFromId(i) + "Color = \""
              + escapeColor(objColors[i]) + '"');
      str.append(getSpecularState());
      if (stereoState != null)
        appendCmd(str, "stereo" + stereoState);
      str.append("\n");
      return str.toString();
    }

    String getSpecularState() {
      StringBuffer str = new StringBuffer("");
      appendCmd(str, "ambientPercent = " + ambientPercent);
      appendCmd(str, "diffusePercent = " + diffusePercent);
      appendCmd(str, "specular = " + specular);
      appendCmd(str, "specularPercent = " + specularPercent);
      appendCmd(str, "specularPower = " + specularPower);
      appendCmd(str, "specularExponent = " + specularExponent);
      return str.toString();
    }
    
    int axesMode               = JmolConstants.AXES_MODE_BOUNDBOX;
    float axesScale            = 1;        
    int pickingSpinRate        = 10;
    
    String helpPath            = null;
    String defaultHelpPath     = JmolConstants.DEFAULT_HELP_PATH;

    //testing

    boolean debugScript = false;
    boolean testFlag1 = false;
    boolean testFlag2 = false;
    boolean testFlag3 = false;
    boolean testFlag4 = false;

    // measurements

    //controlled access:

    private String measureDistanceUnits = "nanometers";
    void setMeasureDistanceUnits(String units) {
      if (units.equalsIgnoreCase("angstroms"))
        measureDistanceUnits = "angstroms";
      else if (units.equalsIgnoreCase("nanometers")
          || units.equalsIgnoreCase("nm"))
        measureDistanceUnits = "nanometers";
      else if (units.equalsIgnoreCase("picometers")
          || units.equalsIgnoreCase("pm"))
        measureDistanceUnits = "picometers";
      else if (units.equalsIgnoreCase("bohr")
          || units.equalsIgnoreCase("au"))
        measureDistanceUnits = "au";
    }
    
    String getMeasureDistanceUnits() {
      return measureDistanceUnits;
    }
    
    Hashtable htParameterValues;
    Hashtable htPropertyFlags;
    
    final static String volatileProperties = 
      //indicate all properties here in lower case
      //surrounded by ";" that should be reset upon file load
      //frame properties and such:
        ";selectionhalos;";
    
    final static String unnecessaryProperties = 
      //these are handled individually
      //NOT EXCLUDING the load state settings, because although we
      //handle these specially for the CURRENT FILE, their current
      //settings won't be reflected in the load state, which is determined
      //earlier, when the file loads. 
        ";refreshing;defaults;backgroundmodel;stereo;"
      + ";appendnew;backgroundcolor;axescolor;axis1color;axis2color;axis3color;boundboxcolor;unitcellcolor;"
      + ";ambientpercent;diffusepercent;specular;specularexponent;specularpower;specularpercent;"
      + ";debugscript;showfrank;showaxes;showaxis1;showaxis2;showaxis3;showunitcell;showboundbox;"
      + ";slabEnabled;zoomEnabled;axeswindow;axesunitcell;axesmolecular;windowcentered;"
      + ";cameradepth;navigationmode;rotationradius;"
      + ";zerobasedxyzrasmol;axesorientationrasmol;"
      + ";_modelnumber;_modelname;_currentmodelnumberinfile;_currentfilenumber;_version;_memory;"
      + "";

    void clearVolatileProperties() {
      Enumeration e;
      e = htPropertyFlags.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        if (volatileProperties.indexOf(";" + key + ";") >= 0 || key.charAt(0) == '@') 
          htPropertyFlags.remove(key);
      }
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        if (volatileProperties.indexOf(";" + key + ";") >= 0 || key.charAt(0) == '@') 
          htParameterValues.remove(key);
      }
    }

    void removeParameter(String key) {
      if (htPropertyFlags.containsKey(key)) {
        htPropertyFlags.remove(key);
        return;
      }
      if (htParameterValues.containsKey(key))
          htParameterValues.remove(key);
    }

    void setParameterValue(String name, boolean value) {
      name = name.toLowerCase();
      if (htParameterValues.containsKey(name))
        return; // don't allow setting boolean of a numeric
      htPropertyFlags.put(name, value ? Boolean.TRUE : Boolean.FALSE);  
    }

    void setParameterValue(String name, int value) {
      name = name.toLowerCase();
      if (htPropertyFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htParameterValues.put(name, new Integer(value));
    }
    
    void setParameterValue(String name, float value) {
      name = name.toLowerCase();
      if (Float.isNaN(value)) {
        htParameterValues.remove(name);
        htPropertyFlags.remove(name);
        return;
      }
      if (htPropertyFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htParameterValues.put(name, new Float(value));
    }
    
    void setParameterValue(String name, String value) {
      if (value == null || htPropertyFlags.containsKey(name))
        return; // don't allow setting string of a boolean
      name = name.toLowerCase();
      htParameterValues.put(name, value);
    }
    
    boolean doRegister(String name) {
      return (unnecessaryProperties.indexOf(";" + name + ";") < 0);
    }

    String getParameterEscaped(String name) {
      return getParameterEscaped(name, 0);
    }

    String getParameterEscaped(String name, int nMax) {
      name = name.toLowerCase();
      if (htParameterValues.containsKey(name)) {
        String sv = StateManager.escape(htParameterValues.get(name));
        if (nMax > 0 && sv.length() > nMax)
          sv = sv.substring(0, nMax) + "\n#...(" + sv.length()
              + " bytes -- use SHOW " + name + " or MESSAGE @" + name
              + " to view)";
        return sv;
      }
      if (htPropertyFlags.containsKey(name))
        return htPropertyFlags.get(name).toString();
      return "<not set>";
    }
    
    Object getParameter(String name) {
      name = name.toLowerCase();
      if (name.equals("_memory")) {
        Runtime runtime = Runtime.getRuntime();
        float bTotal = runtime.totalMemory() / 1000000f;
        float bFree = runtime.freeMemory() / 1000000f;
        return TextFormat.formatDecimal(bTotal - bFree, 1) + "/"
            + TextFormat.formatDecimal(bTotal, 1);
      }
      if (htParameterValues.containsKey(name))
        return htParameterValues.get(name);
      if (htPropertyFlags.containsKey(name))
        return htPropertyFlags.get(name);
      return "";
    }
    
    String getAllSettings(int nMax) {
      StringBuffer commands = new StringBuffer("");
      Enumeration e;
      String key;
      String[] list = new String[htPropertyFlags.size() + htParameterValues.size()];
      //booleans
      int n = 0;
      e = htPropertyFlags.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        list[n++] = key + " = " + htPropertyFlags.get(key);
      }
      //save as _xxxx if you don't want "set" to be there first
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (key.charAt(0) != '@' && key.charAt(0) != '_') {
          Object value = htParameterValues.get(key);
            if (value instanceof String)
              value = escape((String) value);
            list[n++] = key + " = " + value;
        }
      }
      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);
      commands.append("\n");
      return commands.toString();
    }
    
    String getState() {
      int n = 0;
      String[] list = new String[htPropertyFlags.size() + htParameterValues.size()];
      StringBuffer commands = new StringBuffer("# settings;\n");
      appendCmd(commands, "refreshing = false");
      Enumeration e;
      String key;
      //booleans
      e = htPropertyFlags.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (doRegister(key))
          list[n++] = key + " = " + htPropertyFlags.get(key);
      }
      //save as _xxxx if you don't want "set" to be there first
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (key.charAt(0) != '@' && doRegister(key)) {
          Object value = htParameterValues.get(key);
          if (key.charAt(0) == '_') {
            key = key.substring(1) + " ";
          } else {
            if (key.indexOf("default") == 0)
              key = " " + key;
            key += " = ";
            if (value instanceof String)
              value = escape((String) value);
          }
          list[n++] = key +  value;
        }
      }
      switch (axesMode) {
      case JmolConstants.AXES_MODE_UNITCELL:
        list[n++] = "axes = unitcell";
        break;
      case JmolConstants.AXES_MODE_BOUNDBOX:
        list[n++] = "axes = window";
        break;
      default:
        list[n++] =  "axes = molecular";
      }
      //variables only:
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (key.charAt(0) == '@')
          list[n++] = key + " " + htParameterValues.get(key);
      }
      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);
      commands.append("\n");
      return commands.toString();
    }
    
    void registerAllValues() {
      htParameterValues = new Hashtable();
      htPropertyFlags = new Hashtable();
      setParameterValue("showaxes",false);
      setParameterValue("showunitcell",false);
      setParameterValue("showboundbox",false);
      setParameterValue("showfrank",false);

      setParameterValue("allowEmbeddedScripts",allowEmbeddedScripts);
      setParameterValue("allowRotateSelected",allowRotateSelected);
      setParameterValue("ambientPercent",ambientPercent);
//      setParameterValue("argbBackground",argbBackground);
      setParameterValue("appendNew",appendNew);
      setParameterValue("autoBond",autoBond);
      setParameterValue("autoFps",autoFps);
      setParameterValue("axesMode",axesMode);
      setParameterValue("axesScale",axesScale);
      setParameterValue("bondRadiusMilliAngstroms",bondRadiusMilliAngstroms);
      setParameterValue("bondModeOr",bondModeOr);
      setParameterValue("bondTolerance",bondTolerance);
      setParameterValue("cameraDepth",cameraDepth);
      setParameterValue("cartoonRockets",cartoonRockets);
      setParameterValue("chainCaseSensitive",chainCaseSensitive);
      setParameterValue("debugScript",debugScript);
      setParameterValue("defaultAngleLabel",defaultAngleLabel);
      setParameterValue("defaultDirectory",defaultDirectory);
      setParameterValue("defaultDistanceLabel",defaultDistanceLabel);
      setParameterValue("defaultTranslucent",defaultTranslucent);
      setParameterValue("helpPath",defaultHelpPath);
      setParameterValue("defaultLoadScript",defaultLoadScript);
      setParameterValue("defaultTorsionLabel",defaultTorsionLabel);
      setParameterValue("vectorScale",vectorScale);
      setParameterValue("vibrationPeriod",vibrationPeriod);
      setParameterValue("vibrationScale",vibrationScale);
      setParameterValue("diffusePercent",diffusePercent);
      setParameterValue("disablePopupMenu",disablePopupMenu);
      setParameterValue("displayCellParameters",displayCellParameters);
      setParameterValue("dotsSelectedOnly",dotsSelectedOnly);
      setParameterValue("dotSurface",dotSurface);
      setParameterValue("dynamicMeasurements",dynamicMeasurements);
//nah      setParameterValue("enableFullSceneAntialiasing",enableFullSceneAntialiasing);
      setParameterValue("forceAutoBond",forceAutoBond);
      setParameterValue("greyscaleRendering",greyscaleRendering);
      setParameterValue("hbondsBackbone",hbondsBackbone);
      setParameterValue("hbondsSolid",hbondsSolid);
      setParameterValue("helpPath",helpPath);
      setParameterValue("hermiteLevel",hermiteLevel);
      setParameterValue("hideNameInPopup",hideNameInPopup);
      setParameterValue("hideNavigationPoint",hideNavigationPoint);
      setParameterValue("highResolutionFlag",highResolutionFlag);
      setParameterValue("hoverDelay",hoverDelayMs/1000f);
      setParameterValue("justifyMeasurements",justifyMeasurements);
      setParameterValue("loadFormat",loadFormat);
      setParameterValue("measureAllModels",measureAllModels);
      setParameterValue("minBondDistance",minBondDistance);
      setParameterValue("navigationCentered",navigationCentered);
      setParameterValue("navigationMode",navigationMode);
      setParameterValue("navigationPeriodic",navigationPeriodic);
      setParameterValue("navigationSpeed",navigationSpeed);
      setParameterValue("percentVdwAtom",percentVdwAtom);
      setParameterValue("pickingSpinRate",pickingSpinRate);
      setParameterValue("propertyColorScheme",propertyColorScheme);
      setParameterValue("rangeSelected",rangeSelected);
      setParameterValue("selectHetero",rasmolHeteroSetting);
      setParameterValue("selectHydrogen",rasmolHydrogenSetting);
      setParameterValue("ribbonAspectRatio",ribbonAspectRatio);
      setParameterValue("ribbonBorder",ribbonBorder);
      setParameterValue("scriptDelay",scriptDelay);
      setParameterValue("sheetSmoothing",sheetSmoothing);
      setParameterValue("showHiddenSelectionHalos",showHiddenSelectionHalos);
      setParameterValue("showHydrogens",showHydrogens);
      setParameterValue("showMeasurements",showMeasurements);
      setParameterValue("showMultipleBonds",showMultipleBonds);
      setParameterValue("showNavigationPointAlways",showNavigationPointAlways);
      setParameterValue("solvent",solventOn);
      setParameterValue("solventProbeRadius",solventProbeRadius);
      setParameterValue("specular",specular);
      setParameterValue("specularExponent",specularExponent);
      setParameterValue("specularPercent",specularPercent);
      setParameterValue("specularPower",specularPower);
      setParameterValue("ssbondsBackbone",ssbondsBackbone);
      setParameterValue("stereoState",stereoState);
      setParameterValue("strandCount",strandCount);
      setParameterValue("testFlag1",testFlag1);
      setParameterValue("testFlag2",testFlag2);
      setParameterValue("testFlag3",testFlag3);
      setParameterValue("testFlag4",testFlag4);
      setParameterValue("traceAlpha",traceAlpha);
      setParameterValue("useNumberLocalization",useNumberLocalization);
//nah    setParameterValue("zeroBasedXyzRasmol",zeroBasedXyzRasmol);
      setParameterValue("zoomLarge",zoomLarge);
      setParameterValue("zShade",zShade);
    }
  }

  ///////// state serialization 

  static String escape(Object x) {
    if (x instanceof String)
      return escape("" + x);
    return x.toString();
  }
  
  static String escape(BitSet bs) {
    return escape(bs, true);
  }
    
  static String escape(BitSet bs, boolean isAtoms) {
    char chOpen = (isAtoms ? '(' : '[');   
    char chClose = (isAtoms ? ')' : ']');   
    if (bs == null)
      return chOpen + "{}" + chClose;
    StringBuffer s = new StringBuffer(chOpen + "{");
    int imax = bs.size();
    int iLast = -1;
    int iFirst = -2;
    int i = -1;
    while (++i <= imax) {
      boolean isSet = bs.get(i);
      if (i == imax || iLast >= 0 && !isSet) {
        if (iLast >= 0 && iFirst != iLast)
          s.append((iFirst == iLast - 1 ? " " : ":") + iLast);
        if (i == imax)
          break;
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
    s.append("}").append(chClose);
    return s.toString();
  }
 
  static BitSet unescapeBitset(String strBitset) {
    if (strBitset == "{null}")
      return null;
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
  
  static Object unescapePoint(String strPoint) {
    if (strPoint == null || strPoint.length() == 0 
        || strPoint.charAt(0) != '{' || strPoint.charAt(strPoint.length() - 1) != '}')
      return strPoint;
    float[] points = new float[5];
    int nPoints = 0;
    String str = strPoint.substring(1, strPoint.length() - 1);
    int[] next = new int[1];
    for (; nPoints < 5;nPoints++) {
      points[nPoints] = Parser.parseFloat(str, next);
      if (Float.isNaN(points[nPoints]))
        break;
    }
    if (nPoints == 3)
      return new Point3f(points[0], points[1], points[2]);
    if (nPoints == 4)
      return new Point4f(points[0], points[1], points[2], points[3]);
    return strPoint;    
  }
  
  static String escape(String str) {
    if (str == null)
      return "\"\"";
    int pt = -2;
    while ((pt = str.indexOf("\"", pt + 2)) >= 0)
      str = str.substring(0, pt) + '\\' + str.substring(pt);
    str = TextFormat.simpleReplace(str, "\n", "\1");
    str = TextFormat.simpleReplace(str, "\1", "\\n");
    for (int i = str.length(); --i >= 0;)
      if (str.charAt(i) > 0x7F)
        str = str.substring(0, i) + "\\u" + Integer.toHexString(str.charAt(i))
            + str.substring(i + 1);
    return "\"" + str + "\"";
  }
  
  static String escape(float[] f) {
   StringBuffer sb = new StringBuffer();
   for (int i = 0; i < f.length; i++) {
     if (i > 0)
       sb.append('\n');
     sb.append(""+f[i]);
   }
   return sb.toString();
  }
  
  static String escape(Tuple3f xyz) {
    return "{" + xyz.x + " " + xyz.y + " " + xyz.z +"}";
  }
  
  static String escape(Point4f xyzw) {
    return "{" + xyzw.x + " " + xyzw.y + " " + xyzw.z + " " + xyzw.w + "}";
  }
  
  static String escapeColor(int argb) {
    return "[x" + Graphics3D.getHexColorFromRGB(argb) + "]";
  }
  
  static void setStateInfo(Hashtable ht, int i1, int i2, String key) {
    BitSet bs;
    if (ht.containsKey(key)) {
      bs = (BitSet) ht.get(key);
    } else {
      bs = new BitSet();
      ht.put(key, bs);
    }
    for (int i = i1; i <= i2; i++)
      bs.set(i);
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

  static String getCommands(Hashtable ht, StringBuffer s, String setPrev,
                            int nAll, String selectCmd) {
    if (ht == null)
      return "";
    String strAll = "({0:" + (nAll - 1) + "})";
    Enumeration e = ht.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String set = escape((BitSet) ht.get(key));
      if (set.length() < 5) // nothing selected
        continue;
      if (!set.equals(setPrev))
        appendCmd(s, selectCmd + " " + (set.equals(strAll) ? "*" : set));
      setPrev = set;
      if (key.indexOf("-") != 0) // - for key means none required
        appendCmd(s, key);
    }
    return setPrev;
  } 

  static void appendCmd(StringBuffer s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append(cmd).append(";\n");
  }
}
