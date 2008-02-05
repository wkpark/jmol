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

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.TextFormat;

import java.util.Arrays;

public class StateManager {

  

  /* steps in adding a global variable:
 
  In Viewer:
  
  1. add a check in setIntProperty or setBooleanProperty or setFloat.. or setString...
  2. create new set/get methods
  
  In StateManager
  
  3. create the global.xxx variable
  4. in registerParameter() register it so that it shows up as having a value in math
  
  */

  public final static int OBJ_BACKGROUND = 0;
  public final static int OBJ_AXIS1 = 1;
  public final static int OBJ_AXIS2 = 2;
  public final static int OBJ_AXIS3 = 3;
  public final static int OBJ_BOUNDBOX = 4;
  public final static int OBJ_UNITCELL = 5;
  public final static int OBJ_FRANK = 6;
  public final static int OBJ_MAX = 7;
  private final static String objectNameList =
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
  String lastShape = "";

  StateManager(Viewer viewer) {
    this.viewer = viewer;
  }

  GlobalSettings getGlobalSettings() {
    GlobalSettings g = new GlobalSettings();
    g.registerAllValues();
    return g;  
  }
  
  void clear() {
    viewer.setShowAxes(false);
    viewer.setShowBbcage(false);
    viewer.setShowUnitCell(false);
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
    saved.put(saveName, BitSetUtil.copy(bsSelected));
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
/*  
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
*/
  void saveStructure(String saveName) {
    saveName = lastShape = "Shape_" + saveName;
    saved.put(saveName, viewer.getStructureState());
  }

  String getSavedStructure(String saveName) {
    String name = (saveName.length() > 0 ? "Shape_" + saveName
        : lastShape);
    String script = (String) saved.get(name);
    return (script == null ? "" : script); 
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
      ModelSet modelSet = viewer.getModelSet();
      if (modelSet == null)
        return;
      bondCount = modelSet.getBondCount();
      connections = new Connection[bondCount + 1];
      Bond[] bonds = modelSet.getBonds();
      for (int i = bondCount; --i >= 0;) {
        Bond b = bonds[i];
        connections[i] = new Connection(b.getAtomIndex1(), b.getAtomIndex2(),
            b.getMad(), b.getColix(), b.getOrder(), b.getShapeVisibilityFlags());
      }
    }

    void restore() {
      ModelSet modelSet = viewer.getModelSet();
      if (modelSet == null)
        return;
      modelSet.deleteAllBonds();
      for (int i = bondCount; --i >= 0;) {
        Connection c = connections[i];
        int atomCount = modelSet.getAtomCount();
        if (c.atomIndex1 >= atomCount || c.atomIndex2 >= atomCount)
          continue;
        Bond b = modelSet.bondAtoms(modelSet.atoms[c.atomIndex1],
            modelSet.atoms[c.atomIndex2], c.order, c.mad, null);
        b.setColix(c.colix);
        b.setShapeVisibilityFlags(c.shapeVisibilityFlags);
      }
      for (int i = bondCount; --i >= 0;)
        modelSet.getBondAt(i).setIndex(i);
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "reportAll", null);
    }
  }

  static class Connection {
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

  static boolean isMeasurementUnit(String units) {
    return Parser.isOneOf(units.toLowerCase(),"angstroms;au;bohr;nanometers;nm;picometers;pm");
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

    Hashtable listVariables = new Hashtable();
    
    void clear() {
      Enumeration e = htUserVariables.keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        if (key.charAt(0) == '@'
          || key.startsWith("site_")) 
          htUserVariables.remove(key);
      }

      setParameterValue("_atompicked", -1);
      setParameterValue("_atomhovered", -1);
      setParameterValue("_pickinfo", "");
      setParameterValue("selectionhalos", false);
      setParameterValue("hidenotselected", false);
      setParameterValue("measurementlabels", measurementLabels = true);
    }

    void setListVariable(String name, Token value) {
      name = name.toLowerCase();
      if (value == null)
        listVariables.remove(name);
      else
        listVariables.put(name, value);
    }

    Object getListVariable(String name, Object value) {
      if (name == null)
        return value;
      name = name.toLowerCase();
      return (listVariables.containsKey(name) ? listVariables.get(name) : value);
    }
    
    //lighting (see Graphics3D.Shade3D
    
    int ambientPercent   = 45;
    int diffusePercent   = 84;
    boolean specular     = true;
    int specularExponent = 6;
    int specularPercent  = 22;
    int specularPower    = 40;

    //file loading

    boolean allowEmbeddedScripts     = true;
    boolean appendNew                = true;
    String  appletProxy              = "";
    boolean applySymmetryToBonds     = false; //new 11.1.29
    boolean autoBond                 = true;
    short   bondRadiusMilliAngstroms = JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
    float   bondTolerance            = JmolConstants.DEFAULT_BOND_TOLERANCE;
    String  defaultLoadScript        = "";
    String  defaultDirectory         = "";
    boolean forceAutoBond            = false;
    char    inlineNewlineChar        = '|';    //pseudo static
    String  loadFormat               = "http://www.rcsb.org/pdb/files/%FILE.pdb";
    float   minBondDistance          = JmolConstants.DEFAULT_MIN_BOND_DISTANCE;
    int     percentVdwAtom           = JmolConstants.DEFAULT_PERCENT_VDW_ATOM;
    boolean smartAromatic            = true;
    boolean zeroBasedXyzRasmol       = false;

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
      appendCmd(str, "set autoBond " + autoBond);
      appendCmd(str, "set appendNew " + appendNew);
      appendCmd(str, "set appletProxy " + Escape.escape(appletProxy));
      appendCmd(str, "set applySymmetryToBonds " + applySymmetryToBonds);
      if (viewer.getAxesOrientationRasmol())
        appendCmd(str, "set axesOrientationRasmol true");
      appendCmd(str, "set bondRadiusMilliAngstroms " + bondRadiusMilliAngstroms);
      appendCmd(str, "set bondTolerance " + bondTolerance);
      appendCmd(str, "set defaultDirectory " + Escape.escape(defaultDirectory));
      appendCmd(str, "set defaultLattice " + Escape.escape(ptDefaultLattice));
      appendCmd(str, "set defaultLoadScript \"\"");
      if (defaultLoadScript.length() > 0)
        setParameterValue("defaultLoadScript", defaultLoadScript);
      appendCmd(str, "set loadFormat " + Escape.escape(loadFormat));

      appendCmd(str, "set forceAutoBond " + forceAutoBond);
      appendCmd(str, "set minBondDistance " + minBondDistance);
      appendCmd(str, "set percentVdwAtom " + percentVdwAtom);
      appendCmd(str, "set smartAromatic " + smartAromatic);
      if (zeroBasedXyzRasmol)
        appendCmd(str, "set zeroBasedXyzRasmol true");
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

    //centering and perspective

    boolean allowRotateSelected = false;
    boolean perspectiveDepth    = true;
    int     stereoDegrees       = 5;
    float   visualRange         = 5f;

    //solvent

    boolean solventOn = false;

    //measurements

    String defaultAngleLabel    = "%VALUE %UNITS";
    String defaultDistanceLabel = "%VALUE %UNITS"; //also %_ and %a1 %a2 %m1 %m2, etc.
    String defaultTorsionLabel  = "%VALUE %UNITS";
    boolean justifyMeasurements = false;
    boolean measureAllModels    = false;

    //rendering

    boolean antialiasDisplay            = false;
    boolean antialiasImages             = true;
    boolean antialiasTranslucent        = true;
    boolean displayCellParameters       = true;
    boolean dotsSelectedOnly            = false;
    boolean dotSurface                  = true;
    boolean dynamicMeasurements         = false;
    boolean greyscaleRendering          = false;
    boolean isosurfacePropertySmoothing = true;
    boolean showHiddenSelectionHalos    = false;
    boolean showMeasurements            = true;
    boolean zoomLarge                   = true; //false would be like Chime
    boolean zShade                      = false;
    
    //atoms and bonds

    boolean bondModeOr          = false;
    boolean hbondsBackbone      = false;
    boolean hbondsSolid         = false;
    byte    modeMultipleBond    = JmolConstants.MULTIBOND_NOTSMALL;
    boolean showHydrogens       = true;
    boolean showMultipleBonds   = true;
    boolean ssbondsBackbone     = false;

    //secondary structure + Rasmol

    boolean cartoonRockets        = false;
    boolean chainCaseSensitive    = false;
    int     hermiteLevel          = 0;
    boolean highResolutionFlag    = false;
    boolean rangeSelected         = false;
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting   = true;
    int     ribbonAspectRatio     = 16;
    boolean ribbonBorder          = false;
    boolean rocketBarrels         = false;
    float   sheetSmoothing        = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints
    boolean traceAlpha            = true;

    //misc

    int     animationFps              = 10;
    boolean autoFps                   = false;
    int     axesMode                  = JmolConstants.AXES_MODE_BOUNDBOX;
    float   axesScale                 = 2;        
    float   cameraDepth               = 3.0f;
    String  dataSeparator             = "~~~";
    boolean debugScript               = false;
    float   defaultDrawArrowScale     = 0.5f;
    float   defaultTranslucent        = 0.5f;
    int     delayMaximumMs            = 0;
    float   dipoleScale               = 1.0f;
    boolean disablePopupMenu          = false;
    boolean drawPicking               = false;
    String  helpPath                  = JmolConstants.DEFAULT_HELP_PATH;
    boolean fontScaling               = false;
    boolean hideNameInPopup           = false;
    int     hoverDelayMs              = 500;
    boolean measurementLabels         = true;
    int     pickingSpinRate           = 10;
    String  propertyColorScheme       = "roygb";
    float   solventProbeRadius        = 1.2f;
    int     scriptDelay               = 0;
    boolean statusReporting           = true;
    int     strandCountForStrands     = 5;
    int     strandCountForMeshRibbon  = 7;
    boolean useNumberLocalization     = true;
    float   vectorScale               = 1f;
    float   vibrationPeriod           = 1f;
    float   vibrationScale            = 1f;
    boolean wireframeRotation         = false;
    
    // window
    
    boolean   hideNavigationPoint       = false;
    boolean   navigationMode            = false;
    boolean   navigationPeriodic        = false;
    float     navigationSpeed           = 5;
    boolean   showNavigationPointAlways = false;
    String    stereoState               = null;

    // special persistent object characteristics -- bbcage, uccage, axes:
    
    int[]     objColors                 = new int[OBJ_MAX];
    boolean[] objStateOn                = new boolean[OBJ_MAX];
    short[]   objMad                    = new short[OBJ_MAX];

    String getWindowState(StringBuffer sfunc) {
      StringBuffer str = new StringBuffer();
      if (sfunc != null) {
        sfunc.append("  initialize;\n  set refreshing false;\n  _setWindowState;\n");
        str.append("\nfunction _setWindowState();\n");
      }
      str.append("# height "
          + viewer.getScreenHeight() + ";\n# width " + viewer.getScreenWidth()
          + ";\n");
      appendCmd(str, "stateVersion = " + getParameter("_version"));
      for (int i = 0; i < OBJ_MAX; i++)
        if (objColors[i] != 0)
          appendCmd(str, getObjectNameFromId(i) + "Color = \""
              + Escape.escapeColor(objColors[i]) + '"');
      str.append(getSpecularState());
      if (stereoState != null)
        appendCmd(str, "stereo" + stereoState);
      appendCmd(str, "statusReporting  = " + statusReporting);
      if (sfunc != null)
        str.append("end function;\n\n");
      return str.toString();
    }

    String getSpecularState() {
      StringBuffer str = new StringBuffer("");
      appendCmd(str, "ambientPercent = " + Graphics3D.getAmbientPercent());
      appendCmd(str, "diffusePercent = " + Graphics3D.getDiffusePercent());
      appendCmd(str, "specular = " + Graphics3D.getSpecular());
      appendCmd(str, "specularPercent = " + Graphics3D.getSpecularPercent());
      appendCmd(str, "specularPower = " + Graphics3D.getSpecularPower());
      appendCmd(str, "specularExponent = " + Graphics3D.getSpecularExponent());
      return str.toString();
    }    

    //testing

    boolean testFlag1 = false;
    boolean testFlag2 = false;
    boolean testFlag3 = false;
    boolean testFlag4 = false;

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
      setParameterValue("measurementUnits", measureDistanceUnits);
    }
    
    String getMeasureDistanceUnits() {
      return measureDistanceUnits;
    }
    
    Hashtable htParameterValues;
    Hashtable htPropertyFlags;
    Hashtable htPropertyFlagsRemoved;

    final static String unreportedProperties = 
      //these are handled individually in terms of reporting for the state
      //NOT EXCLUDING the load state settings, because although we
      //handle these specially for the CURRENT FILE, their current
      //settings won't be reflected in the load state, which is determined
      //earlier, when the file loads. 
      //
      //place any parameter here you do NOT want to have in the state
      //
      //this is a final static String. MAKE SURE ALL ENTRIES ARE LOWERCASE!
      //
      ";_animating;_atomhovered;_atompicked;_pickinfo" +
      ";_currentfilenumber;_currentmodelnumberinfile" +
      ";_height;_memory;_modelfile;_modelname;_modelnumber;_modeltitle;_spinning;_version;_width" +
      ";_firstframe;_lastframe;_trajectory;_slabplane;_depthplane" +
      ";ambientpercent;animationfps" +
      ";antialiasdisplay;antialiasimages;antialiastranslucent;appendnew;axescolor" +
      ";axesmolecular;axesorientationrasmol;axesunitcell;axeswindow;axis1color;axis2color" +
      ";axis3color;backgroundcolor;backgroundmodel;bondsymmetryatoms;boundboxcolor;cameradepth" +
      ";debugscript;defaultlatttice;defaults;diffusepercent;exportdrivers" +
      ";fontscaling;language;loglevel;navigationmode" +
      ";perspectivedepth;visualrange;perspectivemodel;refreshing;rotationradius" +
      ";showaxes;showaxis1;showaxis2;showaxis3;showboundbox;showfrank;showunitcell" +
      ";slabenabled;specular;specularexponent;specularpercent;specularpower;stateversion" +
      ";statusreporting;stereo;stereostate" +
      ";unitcellcolor;windowcentered;zerobasedxyzrasmol;zoomenabled;" +
      //    saved in the hash table but not considered part of the state:
      ";scriptqueue;scriptreportinglevel;syncscript;syncmouse" +
      //    more settable Jmol variables    
      ";ambient;bonds;colorrasmol;diffuse;drawhover;frank;hetero;hidenotselected" +
      ";hoverlabel;hydrogen;languagetranslation;measurementunits;navigationdepth;navigationslab" +
      ";picking;pickingstyle;propertycolorschemeoverload;radius;rgbblue;rgbgreen;rgbred" +
      ";scaleangstromsperinch;selectionhalos;showscript;showselections;solvent;strandcount" +
      ";spinx;spiny;spinz;spinfps" +
      ";animframecallback;loadstructcallback;messagecallback;hovercallback;resizecallback;pickcallback" +
      ";";
    
    boolean isJmolVariable(String key) {
      return htParameterValues.containsKey(key = key.toLowerCase()) 
      || htPropertyFlags.containsKey(key)
      || unreportedProperties.indexOf(";" + key + ";") >= 0;
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
      name = name.toLowerCase();
      if (value == null || htPropertyFlags.containsKey(name))
        return; // don't allow setting string of a boolean
      htParameterValues.put(name, value);
    }
    
    void removeJmolParameter(String key) {
      if (htPropertyFlags.containsKey(key)) {
        htPropertyFlags.remove(key);
        if (!htPropertyFlagsRemoved.containsKey(key))
          htPropertyFlagsRemoved.put(key, Boolean.FALSE);
        return;
      }
      if (htParameterValues.containsKey(key))
          htParameterValues.remove(key);
    }

    Hashtable htUserVariables = new Hashtable();
    void setUserVariable(String key, Token value) {
      key = key.toLowerCase();
      if (value == null) {
        if (key.equals("all")) {
          htUserVariables.clear();
          Logger.info("all user-defined variables deleted");
        } else if (htUserVariables.containsKey(key)) {
          Logger.info("variable " + key + " deleted");
          htUserVariables.remove(key);
        }
        return;
      }
      htUserVariables.put(key, value);
    }
    
    void removeUserVariable(String key) {
      htUserVariables.remove(key);  
    }
    
    Object getUserParameterValue(String key) {
      return htUserVariables.get(key);  
    }
    
    boolean doRegister(String name) {
      return (unreportedProperties.indexOf(";" + name + ";") < 0);
    }

    String getParameterEscaped(String name, int nMax) {
      name = name.toLowerCase();
      if (htParameterValues.containsKey(name)) {
        Object v = htParameterValues.get(name);
        String sv = escapeVariable(name, v);
        if (nMax > 0 && sv.length() > nMax)
          sv = sv.substring(0, nMax) + "\n#...(" + sv.length()
              + " bytes -- use SHOW " + name + " or MESSAGE @" + name
              + " to view)";
        return sv;
      }
      if (htPropertyFlags.containsKey(name))
        return htPropertyFlags.get(name).toString();
      if (htUserVariables.containsKey(name))
        return escapeUserVariable(name);
      if (htPropertyFlagsRemoved.containsKey(name))
        return "false";
      return "<not set>";
    }

    private String escapeUserVariable(String name) {
      Token token = (Token) htUserVariables.get(name);
      // previously known to contain 
      switch (token.tok) {
      case Token.on:
        return "true";
      case Token.off:
        return "false";
      case Token.integer:
        return "" + token.intValue;
      default:
        return escapeVariable(name, token.value);
      }        
    }
    
    Object getParameter(String name) {
      name = name.toLowerCase();
      if (name.equals("_memory")) {
        Runtime runtime = Runtime.getRuntime();
        float bTotal = runtime.totalMemory() / 1000000f;
        float bFree = runtime.freeMemory() / 1000000f;
        String value = TextFormat.formatDecimal(bTotal - bFree, 1) + "/"
            + TextFormat.formatDecimal(bTotal, 1);
        htParameterValues.put("_memory", value);
      }
      if (htParameterValues.containsKey(name))
        return htParameterValues.get(name);
      if (htPropertyFlags.containsKey(name))
        return htPropertyFlags.get(name);
      if (htPropertyFlagsRemoved.containsKey(name))
        return Boolean.FALSE;
      if (htUserVariables.containsKey(name)) {
        return Token.oValue((Token) htUserVariables.get(name));
      }
      return "";
    }

    String getAllSettings(String prefix) {
      StringBuffer commands = new StringBuffer("");
      Enumeration e;
      String key;
      String[] list = new String[htPropertyFlags.size()
          + htParameterValues.size()];
      //booleans
      int n = 0;
      String _prefix = "_" + prefix;
      e = htPropertyFlags.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (prefix == null 
            || key.indexOf(prefix) == 0 || key.indexOf(_prefix) == 0)
          list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
              + htPropertyFlags.get(key);
      }
      //save as _xxxx if you don't want "set" to be there first
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (key.charAt(0) != '@'
            && (prefix == null 
                || key.indexOf(prefix) == 0 || key.indexOf(_prefix) == 0)) {
          Object value = htParameterValues.get(key);
          if (value instanceof String)
            value = Escape.escape((String) value);
          list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
              + value;
        }
      }
      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);
      commands.append("\n");
      return commands.toString();
    }
    
    String getState(StringBuffer sfunc) {
      int n = 0;
      String[] list = new String[htPropertyFlags.size()
          + htParameterValues.size()];
      StringBuffer commands = new StringBuffer();
      if (sfunc != null) {
        sfunc.append("  _setVariableState;\n");
        commands.append("function _setVariableState();\n\n");
      }
      Enumeration e;
      String key;
      //booleans
      e = htPropertyFlags.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (doRegister(key))
          list[n++] = "set " + key + " " + htPropertyFlags.get(key);
      }
      //save as _xxxx if you don't want "set" to be there first
      e = htParameterValues.keys();
      while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        String name = key;
        if (key.charAt(0) != '@' && doRegister(key)) {
          Object value = htParameterValues.get(key);
          if (key.charAt(0) == '_') { //unitcell offset, for one _frame xx; set unitcell ...
            key = key.substring(1);
          } else {
            if (key.indexOf("default") == 0)
              key = " set " + key;
            else
              key = "set " + key;
            value = escapeVariable(name, value);
          }
          list[n++] = key + " " + value;
        }
      }
      switch (axesMode) {
      case JmolConstants.AXES_MODE_UNITCELL:
        list[n++] = "set axes unitcell";
        break;
      case JmolConstants.AXES_MODE_BOUNDBOX:
        list[n++] = "set axes window";
        break;
      default:
        list[n++] = "set axes molecular";
      }

      //nonboolean variables:
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

      //user variables only:
      commands.append("\n#user-defined variables; \n");

      e = htUserVariables.keys();
      n = 0;
      list = new String[htUserVariables.size()];
      while (e.hasMoreElements())
        list[n++] = (key = (String) e.nextElement())
            + (key.charAt(0) == '@' 
                ? " " + Token.sValue((Token) htUserVariables.get(key))
                : " = " + escapeUserVariable(key));

      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);
      if (n == 0)
        commands.append("# --none--;\n");
      
      // label defaults

      viewer.loadShape(JmolConstants.SHAPE_LABELS);
      commands.append(viewer.getShapeProperty(JmolConstants.SHAPE_LABELS, "defaultState"));

      if (sfunc != null)
        commands.append("\nend function;\n\n");
      return commands.toString();
    }
    
    private String escapeVariable(String name, Object value) {
      if (!(value instanceof String))
        return Escape.escape(value);
      Token var = (Token) getListVariable(name, (Token) null);
      if (var == null)
        return Escape.escape(value);
      return Escape.escape((String[]) var.value);
    }
        
    void registerAllValues() {
      htParameterValues = new Hashtable();
      htPropertyFlags = new Hashtable();
      htPropertyFlagsRemoved = new Hashtable();

      // some of these are just placeholders so that the math processor
      // knows they are Jmol variables. They are held by other managers

      setParameterValue("hoverLabel","");
      setParameterValue("rotationRadius",0);
      setParameterValue("scriptqueue",true);

      setParameterValue("_version",0);
      setParameterValue("stateversion",0);
      setParameterValue("animFrameCallback", "");
      setParameterValue("loadStructCallback", "");
      setParameterValue("messageCallback", "");
      setParameterValue("hoverCallback", "");
      setParameterValue("resizeCallback", "");
      setParameterValue("pickCallback", "");
      setParameterValue("allowEmbeddedScripts",allowEmbeddedScripts);
      setParameterValue("allowRotateSelected",allowRotateSelected);
      setParameterValue("ambientPercent",ambientPercent);
      setParameterValue("animationFps",animationFps);
      setParameterValue("antialiasImages",antialiasImages);
      setParameterValue("antialiasDisplay",antialiasDisplay);
      setParameterValue("antialiasTranslucent",antialiasTranslucent);
      setParameterValue("appendNew",appendNew);
      setParameterValue("appletProxy",appletProxy);
      setParameterValue("applySymmetryToBonds",applySymmetryToBonds);
      setParameterValue("autoBond",autoBond);
      setParameterValue("autoFps",autoFps);
      setParameterValue("axesMode",axesMode);
      setParameterValue("axesScale",axesScale);
      setParameterValue("axesWindow", true);
      setParameterValue("axesMolecular", false);
      setParameterValue("axesUnitcell", false);
      setParameterValue("axesOrientationRasmol", false);
      setParameterValue("backgroundModel", 0);
      setParameterValue("bondModeOr",bondModeOr);
      setParameterValue("bondRadiusMilliAngstroms",bondRadiusMilliAngstroms);
      setParameterValue("bondTolerance",bondTolerance);
      setParameterValue("cameraDepth",cameraDepth);
      setParameterValue("cartoonRockets",cartoonRockets);
      setParameterValue("chainCaseSensitive",chainCaseSensitive);
      setParameterValue("colorRasmol",false);
      setParameterValue("dataSeparator",dataSeparator);
      setParameterValue("debugScript",debugScript);
      setParameterValue("defaultLattice","{0 0 0}");
      setParameterValue("defaultAngleLabel",defaultAngleLabel);
      setParameterValue("defaultColorScheme","Jmol");
      setParameterValue("defaultDrawArrowScale",defaultDrawArrowScale);
      setParameterValue("defaultDirectory",defaultDirectory);
      setParameterValue("defaultDistanceLabel",defaultDistanceLabel);
      setParameterValue("defaultLoadScript",defaultLoadScript);
      setParameterValue("defaults", "Jmol");
      setParameterValue("defaultTorsionLabel",defaultTorsionLabel);
      setParameterValue("defaultTranslucent",defaultTranslucent);
      setParameterValue("delayMaximumMs", delayMaximumMs);
      setParameterValue("diffusePercent",diffusePercent);
      setParameterValue("dipoleScale", dipoleScale);
      setParameterValue("disablePopupMenu",disablePopupMenu);
      setParameterValue("displayCellParameters",displayCellParameters);
      setParameterValue("dotsSelectedOnly",dotsSelectedOnly);
      setParameterValue("dotSurface",dotSurface);
      setParameterValue("drawHover",false);
      setParameterValue("drawPicking",drawPicking);
      setParameterValue("dynamicMeasurements",dynamicMeasurements);
      setParameterValue("exportDrivers", JmolConstants.EXPORT_DRIVER_LIST);
      setParameterValue("fontScaling",fontScaling);
      setParameterValue("forceAutoBond",forceAutoBond);
      setParameterValue("greyscaleRendering",greyscaleRendering);
      setParameterValue("hbondsBackbone",hbondsBackbone);
      setParameterValue("hbondsSolid",hbondsSolid);
      setParameterValue("helpPath",helpPath);
      setParameterValue("hermiteLevel",hermiteLevel);
      setParameterValue("hideNameInPopup",hideNameInPopup);
      setParameterValue("hideNavigationPoint",hideNavigationPoint);
      setParameterValue("hideNotSelected",false); // saved in selectionManager
      setParameterValue("highResolution",highResolutionFlag);
      setParameterValue("historyLevel", 0);
      setParameterValue("hoverDelay",hoverDelayMs/1000f);
      setParameterValue("isosurfacePropertySmoothing", isosurfacePropertySmoothing);
      setParameterValue("justifyMeasurements",justifyMeasurements);
      setParameterValue("loadFormat",loadFormat);
      setParameterValue("measureAllModels",measureAllModels);
      setParameterValue("measurementLabels",measurementLabels=true);
      setParameterValue("measurementUnits", measureDistanceUnits);
      setParameterValue("minBondDistance",minBondDistance);
      setParameterValue("navigationMode",navigationMode);
      setParameterValue("navigationPeriodic",navigationPeriodic);
      setParameterValue("navigationDepth",0);
      setParameterValue("navigationSlab",0);
      setParameterValue("navigationSpeed",navigationSpeed);
      setParameterValue("perspectiveModel",11);
      setParameterValue("percentVdwAtom",percentVdwAtom);
      setParameterValue("picking","ident");
      setParameterValue("pickingSpinRate",pickingSpinRate);
      setParameterValue("pickingStyle","toggle");
      setParameterValue("propertyColorScheme",propertyColorScheme);
      setParameterValue("propertyDataField",0);
      setParameterValue("propertyAtomNumberField",0);
      setParameterValue("rangeSelected",rangeSelected);
      setParameterValue("refreshing",true);
      setParameterValue("ribbonAspectRatio",ribbonAspectRatio);
      setParameterValue("ribbonBorder",ribbonBorder);
      setParameterValue("rocketBarrels",rocketBarrels);
      setParameterValue("scaleAngstromsPerInch",0);
      setParameterValue("scriptReportingLevel",0);
      setParameterValue("selectionHalos",false);
      setParameterValue("selectHetero",rasmolHeteroSetting);
      setParameterValue("selectHydrogen",rasmolHydrogenSetting);
      setParameterValue("sheetSmoothing",sheetSmoothing);
      setParameterValue("showaxes",false);
      setParameterValue("showboundbox",false);
      setParameterValue("showfrank",false);
      setParameterValue("showHiddenSelectionHalos",showHiddenSelectionHalos);
      setParameterValue("showHydrogens",showHydrogens);
      setParameterValue("showMeasurements",showMeasurements);
      setParameterValue("showMultipleBonds",showMultipleBonds);
      setParameterValue("showNavigationPointAlways",showNavigationPointAlways);
      setParameterValue("showScript",scriptDelay);
      setParameterValue("showUnitcell",false);
      setParameterValue("slabEnabled",false);
      setParameterValue("smartAromatic",smartAromatic);
      setParameterValue("solventProbe",solventOn);
      setParameterValue("solventProbeRadius",solventProbeRadius);
      setParameterValue("specular",specular);
      setParameterValue("specularExponent",specularExponent);
      setParameterValue("specularPercent",specularPercent);
      setParameterValue("specularPower",specularPower);
      setParameterValue("spinX", 0);
      setParameterValue("spinY", 30);
      setParameterValue("spinZ", 0);
      setParameterValue("spinFps", 30);
      setParameterValue("ssbondsBackbone",ssbondsBackbone);
      setParameterValue("stereoDegrees",stereoDegrees);
      setParameterValue("statusReporting",statusReporting);
      setParameterValue("strandCount",strandCountForStrands);
      setParameterValue("strandCountForStrands",strandCountForStrands);
      setParameterValue("strandCountForMeshRibbon",strandCountForMeshRibbon);
      setParameterValue("syncMouse",false);
      setParameterValue("syncScript",false);
      setParameterValue("testFlag1",testFlag1);
      setParameterValue("testFlag2",testFlag2);
      setParameterValue("testFlag3",testFlag3);
      setParameterValue("testFlag4",testFlag4);
      setParameterValue("traceAlpha",traceAlpha);
      setParameterValue("useNumberLocalization",useNumberLocalization);
      setParameterValue("vectorScale",vectorScale);
      setParameterValue("vibrationPeriod",vibrationPeriod);
      setParameterValue("vibrationScale",vibrationScale);
      setParameterValue("visualRange",visualRange);
      setParameterValue("windowCentered",true);
      setParameterValue("wireframeRotation",wireframeRotation);
      setParameterValue("zoomEnabled",true);
      setParameterValue("zoomLarge",zoomLarge);
      setParameterValue("zShade",zShade);
      setParameterValue("zeroBasedXyzRasmol",zeroBasedXyzRasmol);
    }
  }

  ///////// state serialization 

  public static void setStateInfo(Hashtable ht, int i1, int i2, String key) {
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

  public static String getCommands(Hashtable ht) {
    return getCommands(ht, null, -1, "select");
  }

  public static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll) {
    return getCommands(htDefine, htMore, nAll, "select");
  }

  public static String getCommands(Hashtable htDefine, Hashtable htMore, int nAll, String selectCmd) {
    StringBuffer s = new StringBuffer();
    String setPrev = getCommands(htDefine, s, null, nAll, selectCmd);
    if (htMore != null)
      getCommands(htMore, s, setPrev, nAll, "select");
    return s.toString();
  }

  public static String getCommands(Hashtable ht, StringBuffer s, String setPrev,
                            int nAll, String selectCmd) {
    if (ht == null)
      return "";
    String strAll = "({0:" + (nAll - 1) + "})";
    Enumeration e = ht.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String set = Escape.escape((BitSet) ht.get(key));
      if (set.length() < 5) // nothing selected
        continue;
      set = selectCmd + " " + (set.equals(strAll) && false? "*" : set);
      if (!set.equals(setPrev))
        appendCmd(s, set);
      setPrev = set;
      if (key.indexOf("-") != 0) // - for key means none required
        appendCmd(s, key);
    }
    return setPrev;
  } 

  public static void appendCmd(StringBuffer s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append("  ").append(cmd).append(";\n");
  }
  

}
