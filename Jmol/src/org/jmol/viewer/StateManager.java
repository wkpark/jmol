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


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.jmol.constant.EnumAxesMode;
import org.jmol.constant.EnumCallback;
import org.jmol.constant.EnumStructure;
import org.jmol.constant.EnumStereoMode;
import org.jmol.modelset.Bond;
import org.jmol.modelset.ModelSet;
import org.jmol.script.ScriptFunction;
import org.jmol.script.ScriptVariable;
import org.jmol.script.Token;
import org.jmol.util.Escape;

import org.jmol.util.BitSet;
import org.jmol.util.BitSetUtil;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.util.Matrix3f;
import org.jmol.util.Point3f;
import org.jmol.util.StringXBuilder;
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
  public final static int OBJ_MAX = 8;
  private final static String objectNameList = "background axis1      axis2      axis3      boundbox   unitcell   frank      ";

  public static String getVariableList(Map<String, ScriptVariable> htVariables, int nMax,
                                       boolean withSites, boolean definedOnly) {
    StringXBuilder sb = new StringXBuilder();
    // user variables only:
    int n = 0;

    String[] list = new String[htVariables.size()];
    for (Map.Entry<String, ScriptVariable> entry : htVariables.entrySet()) {
      String key = entry.getKey();
      ScriptVariable var = entry.getValue();
      if ((withSites || !key.startsWith("site_")) && (!definedOnly || key.charAt(0) == '@'))
        list[n++] = key
            + (key.charAt(0) == '@' ? " " + var.asString() : " = "
                + varClip(key, var.escape(), nMax));
    }
    Arrays.sort(list, 0, n);
    for (int i = 0; i < n; i++)
      if (list[i] != null)
        appendCmd(sb, list[i]);
    if (n == 0 && !definedOnly)
      sb.append("# --no global user variables defined--;\n");
    return sb.toString();
  }
  
  public static int getObjectIdFromName(String name) {
    if (name == null)
      return -1;
    int objID = objectNameList.indexOf(name.toLowerCase());
    return (objID < 0 ? objID : objID / 11);
  }

  static String getObjectNameFromId(int objId) {
    if (objId < 0 || objId >= OBJ_MAX)
      return null;
    return objectNameList.substring(objId * 11, objId * 11 + 11).trim();
  }

  Viewer viewer;
  Map<String, Object> saved = new Hashtable<String, Object>();
  
  String lastOrientation = "";
  String lastConnections = "";
  String lastSelected = "";
  String lastState = "";
  String lastShape = "";
  String lastCoordinates = "";

  StateManager(Viewer viewer) {
    this.viewer = viewer;
  }
  
  GlobalSettings getGlobalSettings(GlobalSettings gsOld, boolean clearUserVariables) {
    return new GlobalSettings(gsOld, clearUserVariables);
  }

  void clear(GlobalSettings global) {
    viewer.setShowAxes(false);
    viewer.setShowBbcage(false);
    viewer.setShowUnitCell(false);
    global.clear();
  }

  void setCrystallographicDefaults() {
    //axes on and mode unitCell; unitCell on; perspective depth off;
    viewer.setAxesModeUnitCell(true);
    viewer.setShowAxes(true);
    viewer.setShowUnitCell(true);
    viewer.setBooleanProperty("perspectiveDepth", false);
  }

  private void setCommonDefaults() {
    viewer.setBooleanProperty("perspectiveDepth", true);
    viewer.setFloatProperty("bondTolerance",
        JmolConstants.DEFAULT_BOND_TOLERANCE);
    viewer.setFloatProperty("minBondDistance",
        JmolConstants.DEFAULT_MIN_BOND_DISTANCE);
  }

  void setJmolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "Jmol");
    viewer.setBooleanProperty("axesOrientationRasmol", false);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", false);
    viewer.setIntProperty("percentVdwAtom",
        JmolConstants.DEFAULT_PERCENT_VDW_ATOM);
    viewer.setIntProperty("bondRadiusMilliAngstroms",
        JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS);
    viewer.setDefaultVdw("auto");
  }

  void setRasMolDefaults() {
    setCommonDefaults();
    viewer.setStringProperty("defaultColorScheme", "RasMol");
    viewer.setBooleanProperty("axesOrientationRasmol", true);
    viewer.setBooleanProperty("zeroBasedXyzRasmol", true);
    viewer.setIntProperty("percentVdwAtom", 0);
    viewer.setIntProperty("bondRadiusMilliAngstroms", 1);
    viewer.setDefaultVdw("Rasmol");
  }

  String listSavedStates() {
    String names = "";
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext())
      names += "\n" + e.next();
    return names;
  }

  private void deleteSavedType(String type) {
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext()) {
      String name = e.next();
      if (name.startsWith(type)) {
        e.remove();
        Logger.debug("deleted " + name);
      }
    }
  }

  void deleteSaved(String name) {
    saved.remove(name);
  }
  
  void saveSelection(String saveName, BitSet bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Selected_");
      return;
    }
    saveName = lastSelected = "Selected_" + saveName;
    saved.put(saveName, BitSetUtil.copy(bsSelected));
  }

  boolean restoreSelection(String saveName) {
    String name = (saveName.length() > 0 ? "Selected_" + saveName
        : lastSelected);
    BitSet bsSelected = (BitSet) saved.get(name);
    if (bsSelected == null) {
      viewer.select(new BitSet(), false, null, false);
      return false;
    }
    viewer.select(bsSelected, false, null, false);
    return true;
  }

  void saveState(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("State_");
      return;
    }
    saveName = lastState = "State_" + saveName;
    saved.put(saveName, viewer.getStateInfo());
  }

  String getSavedState(String saveName) {
    String name = (saveName.length() > 0 ? "State_" + saveName : lastState);
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
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Shape_");
      return;
    }
    saveName = lastShape = "Shape_" + saveName;
    saved.put(saveName, viewer.getStructureState());
  }

  String getSavedStructure(String saveName) {
    String name = (saveName.length() > 0 ? "Shape_" + saveName : lastShape);
    String script = (String) saved.get(name);
    return (script == null ? "" : script);
  }

  void saveCoordinates(String saveName, BitSet bsSelected) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Coordinates_");
      return;
    }
    saveName = lastCoordinates = "Coordinates_" + saveName;
    saved.put(saveName, viewer.getCoordinateState(bsSelected));
  }

  String getSavedCoordinates(String saveName) {
    String name = (saveName.length() > 0 ? "Coordinates_" + saveName
        : lastCoordinates);
    String script = (String) saved.get(name);
    return (script == null ? "" : script);
  }

  Orientation getOrientation() {
    return new Orientation(false);
  }

  String getSavedOrientationText(String saveName) {
    Orientation o;
    if (saveName != null) {
      o = getOrientation(saveName);
      return (o == null ? "" : o.getMoveToText(true));      
    } 
    StringXBuilder sb = new StringXBuilder();
    Iterator<String> e = saved.keySet().iterator();
    while (e.hasNext()) {
       String name = e.next();
       if (!name.startsWith("Orientation_")) {
         continue;
       }
       sb.append(((Orientation) saved.get(name)).getMoveToText(true));
    }
    return sb.toString(); 
  }


  void saveOrientation(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Orientation_");
      return;
    }
    Orientation o = new Orientation(saveName.equals("default"));
    o.saveName = lastOrientation = "Orientation_" + saveName;
    saved.put(o.saveName, o);
  }
  
  boolean restoreOrientation(String saveName, float timeSeconds, boolean isAll) {
    Orientation o = getOrientation(saveName);
    if (o == null)
      return false;
    o.restore(timeSeconds, isAll);
    //    Logger.info(listSavedStates());
    return true;
  }

  private Orientation getOrientation(String saveName) {
    String name = (saveName.length() > 0 ? "Orientation_" + saveName
        : lastOrientation);    
    return (Orientation) saved.get(name);
  }

  public class Orientation {

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
    //boolean navigateSurface;
    String moveToText;
    

    Orientation(boolean asDefault) {
      if (asDefault) {
        Matrix3f rotationMatrix = (Matrix3f) viewer
          .getModelSetAuxiliaryInfoValue("defaultOrientationMatrix");
        if (rotationMatrix == null)
          this.rotationMatrix.setIdentity();
        else
          this.rotationMatrix.setM(rotationMatrix);
      } else {
        viewer.getRotation(this.rotationMatrix);
      }
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
      zoom = viewer.getZoomSetting();
      center.setT(viewer.getRotationCenter());
      windowCenteredFlag = viewer.isWindowCentered();
      rotationRadius = viewer.getRotationRadius();
      navigationMode = viewer.getNavigationMode();
      //navigateSurface = viewer.getNavigateSurface();
      moveToText = viewer.getMoveToText(-1);
      if (navigationMode) {
        xNav = viewer.getNavigationOffsetPercent('X');
        yNav = viewer.getNavigationOffsetPercent('Y');
        navDepth = viewer.getNavigationDepthPercent();
        navCenter = Point3f.newP(viewer.getNavigationCenter());
      }
    }

    public String getMoveToText(boolean asCommand) {
      return (asCommand ? "  " + moveToText + "\n  save orientation \"" 
          + saveName.substring(12) + "\";\n" : moveToText);
    }
    
    public void restore(float timeSeconds, boolean isAll) {
      if (!isAll) {
        viewer.setRotationMatrix(rotationMatrix);
        return;
      }
      viewer.setBooleanProperty("windowCentered", windowCenteredFlag);
      viewer.setBooleanProperty("navigationMode", navigationMode);
      //viewer.setBooleanProperty("navigateSurface", navigateSurface);
      viewer.moveTo(viewer.eval, timeSeconds, center, null, Float.NaN, rotationMatrix, zoom, xTrans,
          yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    }
  }

  void saveBonds(String saveName) {
    if (saveName.equalsIgnoreCase("DELETE")) {
      deleteSavedType("Bonds_");
      return;
    }
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
        connections[i] = new Connection(b.getAtomIndex1(), b.getAtomIndex2(), b
            .getMad(), b.getColix(), b.order, b.getEnergy(), b.getShapeVisibilityFlags());
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
            modelSet.atoms[c.atomIndex2], c.order, c.mad, null, c.energy, false, true);
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
    int order;
    float energy;
    int shapeVisibilityFlags;

    Connection(int atom1, int atom2, short mad, short colix, int order, float energy,
        int shapeVisibilityFlags) {
      atomIndex1 = atom1;
      atomIndex2 = atom2;
      this.mad = mad;
      this.colix = colix;
      this.order = order;
      this.energy = energy;
      this.shapeVisibilityFlags = shapeVisibilityFlags;
    }
  }

  private final static Map<String, ScriptFunction> staticFunctions = new Hashtable<String, ScriptFunction>();
  private Map<String, ScriptFunction> localFunctions = new Hashtable<String, ScriptFunction>();

  Map<String, ScriptFunction> getFunctions(boolean isStatic) {
    return (isStatic ? staticFunctions : localFunctions);
  }

  String getFunctionCalls(String selectedFunction) {
    if (selectedFunction == null)
      selectedFunction = "";
    StringXBuilder s = new StringXBuilder();
    int pt = selectedFunction.indexOf("*");
    boolean isGeneric = (pt >= 0);
    boolean isStatic = (selectedFunction.indexOf("static_") == 0);
    boolean namesOnly = (selectedFunction.equalsIgnoreCase("names") || selectedFunction.equalsIgnoreCase("static_names"));
    if (namesOnly)
      selectedFunction = "";
    if (isGeneric)
      selectedFunction = selectedFunction.substring(0, pt);
    selectedFunction = selectedFunction.toLowerCase();
    Map<String, ScriptFunction> ht = getFunctions(isStatic);
    String[] names = new String[ht.size()];
    Iterator<String> e = ht.keySet().iterator();
    int n = 0;
    while (e.hasNext()) {
      String name = e.next();
      if (selectedFunction.length() == 0 && !name.startsWith("_")
          || name.equalsIgnoreCase(selectedFunction) || isGeneric
          && name.toLowerCase().indexOf(selectedFunction) == 0)
        names[n++] = name;
    }
    Arrays.sort(names, 0, n);
    for (int i = 0; i < n; i++) {
      ScriptFunction f = ht.get(names[i]);
      s.append(namesOnly ? f.getSignature() : f.toString());
      s.appendC('\n');
    }
    return s.toString();
  }

  public void clearFunctions() {
    staticFunctions.clear();
    localFunctions.clear();
  }

  private static boolean isStaticFunction(String name) {
    return name.startsWith("static_");  
  }
  
  boolean isFunction(String name) {
    return (isStaticFunction(name) ? staticFunctions : localFunctions).containsKey(name);
  }

  void addFunction(ScriptFunction function) {
    (isStaticFunction(function.name) ? staticFunctions
        : localFunctions).put(function.name, function);
  }
  
  void removeFunction(String name) {
    ScriptFunction function = getFunction(name);
    if (function == null)
      return;
    staticFunctions.remove(name);
    localFunctions.remove(name);
  }

  ScriptFunction getFunction(String name) {
    if (name == null)
      return null;
    ScriptFunction function = (isStaticFunction(name) ? staticFunctions
        : localFunctions).get(name);
    return (function == null || function.aatoken == null ? null : function);
  }
  
  protected final static String unreportedProperties =
    //these are handled individually in terms of reporting for the state
    //NOT EXCLUDING the load state settings, because although we
    //handle these specially for the CURRENT FILE, their current
    //settings won't be reflected in the load state, which is determined
    //earlier, when the file loads. 
    //
    //place any parameter here you do NOT want to have in the state
    //
    // _xxxxx variables are automatically exempt
    //
    (";ambientpercent;animationfps"
        + ";antialiasdisplay;antialiasimages;antialiastranslucent;appendnew;axescolor"
        + ";axesposition;axesmolecular;axesorientationrasmol;axesunitcell;axeswindow;axis1color;axis2color"
        + ";axis3color;backgroundcolor;backgroundmodel;bondsymmetryatoms;boundboxcolor;cameradepth"
        + ";debug;debugscript;defaultlatttice;defaults;defaultdropscript;diffusepercent;exportdrivers"
        + ";_filecaching;_filecache;fontcaching;fontscaling;forcefield;language"
        + ";legacyautobonding"
        + ";loglevel;logfile;loggestures;logcommands;measurestylechime"
        + ";loadformat;loadligandformat;smilesurlformat;pubchemformat;nihresolverformat;edsurlformat;edsurlcutoff;multiprocessor;navigationmode;"
        + ";pathforallfiles;perspectivedepth;phongexponent;perspectivemodel;preservestate;refreshing;repaintwaitms;rotationradius"
        + ";showaxes;showaxis1;showaxis2;showaxis3;showboundbox;showfrank;showtiming;showunitcell"
        + ";slabenabled;slab;slabrange;depth;zshade;zshadepower;specular;specularexponent;specularpercent;specularpower;stateversion"
        + ";statusreporting;stereo;stereostate;vibrationperiod"
        + ";unitcellcolor;visualrange;windowcentered;zerobasedxyzrasmol;zoomenabled;mousedragfactor;mousewheelfactor"
        //    saved in the hash table but not considered part of the state:
        + ";scriptqueue;scriptreportinglevel;syncscript;syncmouse;syncstereo;" 
        + ";defaultdirectory;currentlocalpath;defaultdirectorylocal"
        //    more settable Jmol variables    
        + ";ambient;bonds;colorrasmol;diffuse;frank;hetero;hidenotselected"
        + ";hoverlabel;hydrogen;languagetranslation;measurementunits;navigationdepth;navigationslab"
        + ";picking;pickingstyle;propertycolorschemeoverload;radius;rgbblue;rgbgreen;rgbred"
        + ";scaleangstromsperinch;selectionhalos;showscript;showselections;solvent;strandcount"
        + ";spinx;spiny;spinz;spinfps;navx;navy;navz;navfps;" + EnumCallback.getNameList()
        + ";undo;bondpicking;modelkitmode;allowgestures;allowkeystrokes;allowmultitouch;allowmodelkit"
        + ";").toLowerCase();

  protected static int getJmolVersionInt() {
    // 11.9.999 --> 1109999
    String s = JmolConstants.version;
    int version = -1;

    try {
      // Major number
      int i = s.indexOf(".");
      if (i < 0) {
        version = 100000 * Integer.parseInt(s);
        return version;
      }
      version = 100000 * Integer.parseInt(s.substring(0, i));

      // Minor number
      s = s.substring(i + 1);
      i = s.indexOf(".");
      if (i < 0) {
        version += 1000 * Integer.parseInt(s);
        return version;
      }
      version += 1000 * Integer.parseInt(s.substring(0, i));

      // Revision number
      s = s.substring(i + 1);
      i = s.indexOf("_");
      if (i >= 0)
        s = s.substring(0, i);
      i = s.indexOf(" ");
      if (i >= 0)
        s = s.substring(0, i);
      version += Integer.parseInt(s);
    } catch (NumberFormatException e) {
      // We simply keep the version currently found
    }

    return version;
  }

  public class GlobalSettings {

    Map<String, Object> htNonbooleanParameterValues;
    Map<String, Boolean> htBooleanParameterFlags;
    Map<String, Boolean> htPropertyFlagsRemoved;
    Map<String, ScriptVariable> htUserVariables = new Hashtable<String, ScriptVariable>();
    Map<String, String> databases;

    /*
     *  Mostly these are just saved and restored directly from Viewer.
     *  They are collected here for reference and to ensure that no 
     *  methods are written that bypass viewer's get/set methods.
     *  
     *  Because these are not Frame variables, they (mostly) should persist past
     *  a new file loading. There is some question in my mind whether all
     *  should be in this category.
     *  
     */

    GlobalSettings(GlobalSettings gsOld, boolean clearUserVariables) {
      registerAllValues(gsOld, clearUserVariables);
    }

    void clear() {
      Iterator<String> e = htUserVariables.keySet().iterator();
      while (e.hasNext()) {
        String key = e.next();
        if (key.charAt(0) == '@' || key.startsWith("site_"))
          e.remove();
      }

      // PER-zap settings made
      setPicked(-1);
      setParamI("_atomhovered", -1);
      setParamS("_pickinfo", "");
      setParamB("selectionhalos", false);
      setParamB("hidenotselected", false); // to synchronize with selectionManager
      setParamB("measurementlabels", measurementLabels = true);
      setParamB("drawHover", drawHover = false);
      

    }

    void registerAllValues(GlobalSettings g, boolean clearUserVariables) {
      htNonbooleanParameterValues = new Hashtable<String, Object>();
      htBooleanParameterFlags = new Hashtable<String, Boolean>();
      htPropertyFlagsRemoved = new Hashtable<String, Boolean>();
      if (g != null) {
        //persistent values not reset with the "initialize" command
        if (!clearUserVariables)
          htUserVariables = g.htUserVariables; // 12.3.7, 12.2.7
        debugScript = g.debugScript;
        disablePopupMenu = g.disablePopupMenu;
        messageStyleChime = g.messageStyleChime;
        defaultDirectory = g.defaultDirectory;
        allowGestures = g.allowGestures;
        allowModelkit = g.allowModelkit;
        allowMultiTouch = g.allowMultiTouch;
        allowKeyStrokes = g.allowKeyStrokes;
        legacyAutoBonding = g.legacyAutoBonding;
        useScriptQueue = g.useScriptQueue;
        useArcBall = g.useArcBall;
        databases = g.databases;
      }    
      if (databases == null) {
        databases = new Hashtable<String, String>();
        getDataBaseList(JmolConstants.databases);
        loadFormat = databases.get("pdb");
        loadLigandFormat = databases.get("ligand");
        nmrUrlFormat = databases.get("nmr");
        smilesUrlFormat = databases.get("nci") + "/file?format=sdf&get3d=True";
        nihResolverFormat = databases.get("nci");
        pubChemFormat = databases.get("pubchem");
        
        // beyond these six, they are just in the form load =xxx/id
        
        getDataBaseList(userDatabases);
      }

    for (EnumCallback item : EnumCallback.values())        
        resetValue(item.name() + "Callback", g);        

      setParamI("historyLevel", 0); //deprecated ? doesn't do anything

      // These next are just placeholders so that the math processor
      // knows they are Jmol variables. They are held by other managers.
      // This is NOT recommended, because it is easy to forget they are 
      // here and then not reset them properly. Basically it means that
      // the other manager must ensure that the value changed there is
      // updated here, AND when an initialization occurs, they remain in
      // sync. This is difficult to manage and should be changed.
      // The good news is that this manager is initialized FIRST, so 
      // we really just have to make sure that all these values are definitely
      // also initialized within the managers. 

      setParamI("depth", 0);                 // maintained by TransformManager
      setParamF("gestureSwipeFactor", ActionManager.DEFAULT_GESTURE_SWIPE_FACTOR);
      setParamB("hideNotSelected", false); //maintained by the selectionManager
      setParamS("hoverLabel", ""); // maintained by the Hover shape
      setParamB("isKiosk", viewer.isKiosk()); // maintained by Viewer
      setParamS("logFile", viewer.getLogFile()); // maintained by Viewer
      setParamI("logLevel", Logger.getLogLevel());
      setParamF("mouseWheelFactor", ActionManager.DEFAULT_MOUSE_WHEEL_FACTOR);
      setParamF("mouseDragFactor", ActionManager.DEFAULT_MOUSE_DRAG_FACTOR);
      setParamI("navFps", TransformManager.DEFAULT_NAV_FPS); 
      setParamI("navigationDepth", 0);   // maintained by TransformManager
      setParamI("navigationSlab", 0);    // maintained by TransformManager
      setParamI("navX", 0);              // maintained by TransformManager
      setParamI("navY", 0);              // maintained by TransformManager
      setParamI("navZ", 0);              // maintained by TransformManager
      setParamS("pathForAllFiles", "");
      setParamI("perspectiveModel", TransformManager.DEFAULT_PERSPECTIVE_MODEL);
      setParamS("picking", "identify");      // maintained by ActionManager
      setParamS("pickingStyle", "toggle");   // maintained by ActionManager
      setParamB("refreshing", true);         // maintained by Viewer
      setParamI("rotationRadius", 0);        // maintained by TransformManager
      setParamI("scaleAngstromsPerInch", 0); // maintained by TransformManager
      setParamI("scriptReportingLevel", 0);  // maintained by ScriptEvaluator
      setParamB("selectionHalos", false);    // maintained by ModelSet
      setParamB("showaxes", false);          // maintained by Axes
      setParamB("showboundbox", false);      // maintained by Bbcage
      setParamB("showfrank", false);         // maintained by Viewer
      setParamB("showUnitcell", false);      // maintained by Uccage
      setParamI("slab", 100);                // maintained by TransformManager
      setParamB("slabEnabled", false);       // maintained by TransformManager     
      setParamF("slabrange", 0f);            // maintained by TransformManager
      setParamI("spinX", 0);                 // maintained by TransformManager
      setParamI("spinY", TransformManager.DEFAULT_SPIN_Y);
      setParamI("spinZ", 0);                 // maintained by TransformManager
      setParamI("spinFps", TransformManager.DEFAULT_SPIN_FPS);
      setParamI("stereoDegrees", EnumStereoMode.DEFAULT_STEREO_DEGREES); 
      setParamI("stateversion", 0); // only set by a saved state being recalled
      setParamB("syncScript", viewer.getStatusManager().syncingScripts);
      setParamB("syncMouse", viewer.getStatusManager().syncingMouse);
      setParamB("syncStereo", viewer.getStatusManager().stereoSync);
      setParamB("windowCentered", true); // maintained by TransformManager
      setParamB("zoomEnabled", true);    // maintained by TransformManager
      setParamI("zDepth", 0);            // maintained by TransformManager
      setParamB("zShade", false);        // maintained by TransformManager
      setParamI("zSlab", 50);            // maintained by TransformManager
      

      // These next values have no other place than the global Hashtables.
      // This just means that a call to viewer.getXxxxProperty() is necessary.
      // Otherwise, it's the same as if they had a global variable. 
      // It's just an issue of speed of access. Generally, these should only be
      // accessed by the user. 
      
      setParamI("_version", getJmolVersionInt());

      setParamB("axesWindow", true);
      setParamB("axesMolecular", false);
      setParamB("axesPosition", false);
      setParamB("axesUnitcell", false);
      setParamI("backgroundModel", 0);
      setParamB("colorRasmol", false);
      setParamS("currentLocalPath", "");
      setParamS("defaultLattice", "{0 0 0}");
      setParamS("defaultColorScheme", "Jmol");
      setParamS("defaultDirectoryLocal", "");
      setParamS("defaults", "Jmol");
      setParamS("defaultVDW", "Jmol");
      setParamS("exportDrivers", JmolConstants.EXPORT_DRIVER_LIST);
      setParamI("propertyAtomNumberColumnCount", 0);
      setParamI("propertyAtomNumberField", 0);
      setParamI("propertyDataColumnCount", 0);
      setParamI("propertyDataField", 0);
      setParamB("undo", true);

      // OK, all of the rest of these are maintained here as global values (below)

      setParamB("allowEmbeddedScripts", allowEmbeddedScripts);
      setParamB("allowGestures", allowGestures);
      setParamB("allowKeyStrokes", allowKeyStrokes);
      setParamB("allowModelkit", allowModelkit);
      setParamB("allowMultiTouch", allowMultiTouch);
      setParamB("allowRotateSelected", allowRotateSelected);
      setParamB("allowMoveAtoms", allowMoveAtoms);
      setParamI("ambientPercent", ambientPercent);
      setParamI("animationFps", animationFps);
      setParamB("antialiasImages", antialiasImages);
      setParamB("antialiasDisplay", antialiasDisplay);
      setParamB("antialiasTranslucent", antialiasTranslucent);
      setParamB("appendNew", appendNew);
      setParamS("appletProxy", appletProxy);
      setParamB("applySymmetryToBonds", applySymmetryToBonds);
      setParamB("atomPicking", atomPicking);
      setParamS("atomTypes", atomTypes);
      setParamB("autoBond", autoBond);
      setParamB("autoFps", autoFps);
//      setParameterValue("autoLoadOrientation", autoLoadOrientation);
      setParamI("axesMode", axesMode.getCode());
      setParamF("axesScale", axesScale);
      setParamB("axesOrientationRasmol", axesOrientationRasmol);
      setParamB("bondModeOr", bondModeOr);
      setParamB("bondPicking", bondPicking);
      setParamI("bondRadiusMilliAngstroms", bondRadiusMilliAngstroms);
      setParamF("bondTolerance", bondTolerance);
      setParamF("cameraDepth", cameraDepth);
      setParamB("cartoonBaseEdges", cartoonBaseEdges);
      setParamB("cartoonFancy", cartoonFancy);
      setParamB("cartoonRockets", cartoonRockets);
      setParamB("chainCaseSensitive", chainCaseSensitive);
      setParamS("dataSeparator", dataSeparator);
      setParamB("debugScript", debugScript);
      setParamS("defaultAngleLabel", defaultAngleLabel);
      setParamF("defaultDrawArrowScale", defaultDrawArrowScale);
      setParamS("defaultDirectory", defaultDirectory);
      setParamS("defaultDistanceLabel", defaultDistanceLabel);
      setParamS("defaultDropScript", defaultDropScript);
      setParamS("defaultLabelPDB", defaultLabelPDB);
      setParamS("defaultLabelXYZ", defaultLabelXYZ);
      setParamS("defaultLoadFilter", defaultLoadFilter);
      setParamS("defaultLoadScript", defaultLoadScript);
      setParamB("defaultStructureDSSP", defaultStructureDSSP);
      setParamS("defaultTorsionLabel", defaultTorsionLabel);
      setParamF("defaultTranslucent", defaultTranslucent);
      setParamI("delayMaximumMs", delayMaximumMs);
      setParamI("diffusePercent", diffusePercent);
      setParamF("dipoleScale", dipoleScale);
      setParamB("disablePopupMenu", disablePopupMenu);
      setParamB("displayCellParameters", displayCellParameters);
      setParamI("dotDensity", dotDensity);
      setParamI("dotScale", dotScale);
      setParamB("dotsSelectedOnly", dotsSelectedOnly);
      setParamB("dotSurface", dotSurface);
      setParamB("dragSelected", dragSelected);
      setParamB("drawHover", drawHover);
      setParamB("drawPicking", drawPicking);
      setParamB("dsspCalculateHydrogenAlways", dsspCalcHydrogen);
      setParamB("dynamicMeasurements", dynamicMeasurements);
      setParamS("edsUrlFormat", edsUrlFormat);
      //setParameterValue("edsUrlOptions", edsUrlOptions);
      setParamS("edsUrlCutoff", edsUrlCutoff);
      setParamB("ellipsoidArcs", ellipsoidArcs);
      setParamB("ellipsoidAxes", ellipsoidAxes);
      setParamF("ellipsoidAxisDiameter", ellipsoidAxisDiameter);
      setParamB("ellipsoidBall", ellipsoidBall);
      setParamI("ellipsoidDotCount", ellipsoidDotCount);
      setParamB("ellipsoidDots", ellipsoidDots);
      setParamB("ellipsoidFill", ellipsoidFill);
      setParamS("energyUnits", energyUnits);
//      setParameterValue("_fileCaching", _fileCaching);
//      setParameterValue("_fileCache", _fileCache);
      setParamB("fontScaling", fontScaling);
      setParamB("fontCaching", fontCaching);
      setParamB("forceAutoBond", forceAutoBond);
      setParamS("forceField", forceField);
      setParamB("fractionalRelative", fractionalRelative);
      setParamB("greyscaleRendering", greyscaleRendering);
      setParamF("hbondsAngleMinimum", hbondsAngleMinimum);
      setParamF("hbondsDistanceMaximum", hbondsDistanceMaximum);
      setParamB("hbondsBackbone", hbondsBackbone);
      setParamB("hbondsRasmol", hbondsRasmol);
      setParamB("hbondsSolid", hbondsSolid);
      setParamI("helixStep", helixStep);
      setParamS("helpPath", helpPath);
      setParamI("hermiteLevel", hermiteLevel);
      setParamB("hideNameInPopup", hideNameInPopup);
      setParamB("hideNavigationPoint", hideNavigationPoint);
      setParamB("highResolution", highResolutionFlag);
      setParamF("hoverDelay", hoverDelayMs / 1000f);
      setParamB("imageState", imageState);
      setParamB("isosurfaceKey", isosurfaceKey);
      setParamB("isosurfacePropertySmoothing",
          isosurfacePropertySmoothing);
      setParamI("isosurfacePropertySmoothingPower",
          isosurfacePropertySmoothingPower);
      setParamB("justifyMeasurements", justifyMeasurements);
      setParamB("legacyAutoBonding", legacyAutoBonding);
      setParamF("loadAtomDataTolerance", loadAtomDataTolerance);
      setParamS("loadFormat", loadFormat);
      setParamS("loadLigandFormat", loadLigandFormat);
      setParamB("logCommands", logCommands);
      setParamB("logGestures", logGestures);
      setParamB("measureAllModels", measureAllModels);
      setParamB("measurementLabels", measurementLabels);
      setParamS("measurementUnits", measureDistanceUnits);
      setParamI("meshScale", meshScale);
      setParamB("messageStyleChime", messageStyleChime);
      setParamF("minBondDistance", minBondDistance);
      setParamI("minPixelSelRadius", minPixelSelRadius);
      setParamI("minimizationSteps", minimizationSteps);
      setParamB("minimizationRefresh", minimizationRefresh);
      setParamB("minimizationSilent", minimizationSilent);
      setParamF("minimizationCriterion", minimizationCriterion);
      setParamB("modelKitMode", modelKitMode);
      setParamB("monitorEnergy", monitorEnergy);
      setParamF("multipleBondRadiusFactor", multipleBondRadiusFactor);
      setParamF("multipleBondSpacing", multipleBondSpacing);
      setParamB("multiProcessor", multiProcessor && (Viewer.nProcessors > 1));
      setParamB("navigationMode", navigationMode);
      //setParamB("navigateSurface", navigateSurface);
      setParamB("navigationPeriodic", navigationPeriodic);
      setParamF("navigationSpeed", navigationSpeed);
      setParamS("nmrUrlFormat", nmrUrlFormat);
      setParamB("partialDots", partialDots);
      setParamB("pdbAddHydrogens", pdbAddHydrogens); // new 12.1.51
      setParamB("pdbGetHeader", pdbGetHeader); // new 11.5.39
      setParamB("pdbSequential", pdbSequential); // new 11.5.39
      setParamB("perspectiveDepth", perspectiveDepth);
      setParamI("percentVdwAtom", percentVdwAtom);
      setParamI("phongExponent", phongExponent);
      setParamI("pickingSpinRate", pickingSpinRate);
      setParamS("pickLabel", pickLabel);
      setParamF("pointGroupLinearTolerance", pointGroupLinearTolerance);
      setParamF("pointGroupDistanceTolerance", pointGroupDistanceTolerance);
      setParamB("preserveState", preserveState);
      setParamS("propertyColorScheme", propertyColorScheme);
      setParamS("quaternionFrame", quaternionFrame);
      setParamB("rangeSelected", rangeSelected);
      setParamI("repaintWaitMs", repaintWaitMs);
      setParamI("ribbonAspectRatio", ribbonAspectRatio);
      setParamB("ribbonBorder", ribbonBorder);
      setParamB("rocketBarrels", rocketBarrels);
      setParamB("saveProteinStructureState", saveProteinStructureState);
      setParamB("scriptqueue", useScriptQueue);
      setParamB("selectAllModels", selectAllModels);
      setParamB("selectHetero", rasmolHeteroSetting);
      setParamB("selectHydrogen", rasmolHydrogenSetting);
      setParamF("sheetSmoothing", sheetSmoothing);
      setParamB("showHiddenSelectionHalos", showHiddenSelectionHalos);
      setParamB("showHydrogens", showHydrogens);
      setParamB("showKeyStrokes", showKeyStrokes);
      setParamB("showMeasurements", showMeasurements);
      setParamB("showMultipleBonds", showMultipleBonds);
      setParamB("showNavigationPointAlways", showNavigationPointAlways);
      setParamI("showScript", scriptDelay);
      setParamB("showtiming", showTiming);
      setParamB("slabByMolecule", slabByMolecule);
      setParamB("slabByAtom", slabByAtom);
      setParamB("smartAromatic", smartAromatic);
      setParamI("smallMoleculeMaxAtoms", smallMoleculeMaxAtoms);
      setParamS("smilesUrlFormat", smilesUrlFormat);
      setParamS("nihResolverFormat", nihResolverFormat);
      setParamS("pubChemFormat", pubChemFormat);
      setParamB("solventProbe", solventOn);
      setParamF("solventProbeRadius", solventProbeRadius);
      setParamB("specular", specular);
      setParamI("specularExponent", specularExponent);
      setParamI("specularPercent", specularPercent);
      setParamI("specularPower", specularPower);
      setParamB("ssbondsBackbone", ssbondsBackbone);
      setParamB("statusReporting", statusReporting);
      setParamI("strandCount", strandCountForStrands);
      setParamI("strandCountForStrands", strandCountForStrands);
      setParamI("strandCountForMeshRibbon", strandCountForMeshRibbon);
      setParamF("strutDefaultRadius", strutDefaultRadius);
      setParamF("strutLengthMaximum", strutLengthMaximum);
      setParamI("strutSpacing", strutSpacing);
      setParamB("strutsMultiple", strutsMultiple);
      setParamB("testFlag1", testFlag1);
      setParamB("testFlag2", testFlag2);
      setParamB("testFlag3", testFlag3);
      setParamB("testFlag4", testFlag4);
      setParamB("traceAlpha", traceAlpha);
      setParamB("useArcBall", useArcBall);
      setParamB("useMinimizationThread", useMinimizationThread);
      setParamB("useNumberLocalization", useNumberLocalization);
      setParamF("vectorScale", vectorScale);
      setParamB("vectorSymmetry", vectorSymmetry);
      setParamF("vibrationPeriod", vibrationPeriod);
      setParamF("vibrationScale", vibrationScale);
      setParamF("visualRange", visualRange);
      setParamB("waitForMoveTo", waitForMoveTo);
      setParamB("wireframeRotation", wireframeRotation);
      setParamI("zDepth", zDepth);
      setParamB("zeroBasedXyzRasmol", zeroBasedXyzRasmol);
      setParamB("zoomLarge", zoomLarge);
      setParamI("zShadePower", zShadePower);
      setParamI("zSlab", zSlab);  
    }

    //lighting (see GData.Shade3D

    int ambientPercent = 45;
    int diffusePercent = 84;
    boolean specular = true;
    int specularExponent = 6;  // log2 of phongExponent
    int phongExponent = 64;    // 2^specularExponent
    int specularPercent = 22;
    int specularPower = 40;
    int zDepth = 0;
    int zShadePower = 3;  // increased to 3 from 1 for Jmol 12.1.49
    int zSlab = 50; // increased to 50 from 0 in Jmol 12.3.6 and Jmol 12.2.6
     
    boolean slabByMolecule = false;
    boolean slabByAtom = false;

    //file loading

    boolean allowEmbeddedScripts = true;
    boolean appendNew = true;
    String appletProxy = "";
    boolean applySymmetryToBonds = false; //new 11.1.29
    String atomTypes = "";
    boolean autoBond = true;
//    boolean autoLoadOrientation = false; // 11.7.30 for Spartan and Sygress/CAChe loading with or without rotation
       // starting with Jmol 12.0.RC10, this setting is ignored, and FILTER "NoOrient" is required if the file
       // is to be loaded without reference to the orientation saved in the file.
    boolean axesOrientationRasmol = false;
    short bondRadiusMilliAngstroms = JmolConstants.DEFAULT_BOND_MILLIANGSTROM_RADIUS;
    float bondTolerance = JmolConstants.DEFAULT_BOND_TOLERANCE;
    String defaultDirectory = "";
    boolean defaultStructureDSSP = true; // Jmol 12.1.15
    final Point3f ptDefaultLattice = new Point3f();
    String defaultLoadScript = "";
    String defaultLoadFilter = "";
    String defaultDropScript = "zap; load SYNC %FILE;if (%ALLOWCARTOONS && _loadScript == '' && defaultLoadScript == '' && _filetype == 'Pdb') {if ({(protein or nucleic)&*/1.1} && {*/1.1}[1].groupindex != {*/1.1}[0].groupindex){select protein or nucleic;cartoons only;}if ({visible}){color structure}else{wireframe -0.1};if (!{visible}){spacefill 23%};select *}";
//    boolean _fileCaching = false;
//    String _fileCache = "";
    boolean forceAutoBond = false;
    boolean fractionalRelative = false; // true: UNITCELL offset will change meaning of {1/2 1/2 1/2} 
    char inlineNewlineChar = '|'; //pseudo static
    String loadFormat, loadLigandFormat, nmrUrlFormat, smilesUrlFormat, nihResolverFormat, pubChemFormat;

    String edsUrlFormat = "http://eds.bmc.uu.se/eds/dfs/%LC13/%LCFILE/%LCFILE.omap";
    String edsUrlCutoff = "load('http://eds.bmc.uu.se/eds/dfs/%LC13/%LCFILE/%LCFILE.sfdat').lines.find('MAP_SIGMA').split(' ')[2]";
    String edsUrlOptions = "within 2.0 {*}";
    float minBondDistance = JmolConstants.DEFAULT_MIN_BOND_DISTANCE;
    int minPixelSelRadius = 6;
    boolean pdbAddHydrogens = false; // true to add hydrogen atoms
    boolean pdbGetHeader = false; // true to get PDB header in auxiliary info
    boolean pdbSequential = false; // true for no bonding check
    int percentVdwAtom = JmolConstants.DEFAULT_PERCENT_VDW_ATOM;
    int smallMoleculeMaxAtoms = 40000;
    boolean smartAromatic = true;
    boolean zeroBasedXyzRasmol = false;
    boolean legacyAutoBonding = false;

    /**
     *  these settings are determined when the file is loaded and are
     *  kept even though they might later change. So we list them here
     *  and ALSO let them be defined in the settings. 10.9.98 missed this. 
     * @param htParams 
     *  
     * @return script command
     */
    String getLoadState(Map<String, Object> htParams) {
      
      
      // some commands register flags so that they will be 
      // restored in a saved state definition, but will not execute
      // now so that there is no chance any embedded scripts or
      // default load scripts will run and slow things down.
      StringXBuilder str = new StringXBuilder();
      appendCmd(str, "set allowEmbeddedScripts false");
      if (allowEmbeddedScripts)
        setParamB("allowEmbeddedScripts", true);
      appendCmd(str, "set appendNew " + appendNew);
      appendCmd(str, "set appletProxy " + Escape.escapeStr(appletProxy));
      appendCmd(str, "set applySymmetryToBonds " + applySymmetryToBonds);
      if (atomTypes.length() > 0)
        appendCmd(str, "set atomTypes " + Escape.escapeStr(atomTypes));
      appendCmd(str, "set autoBond " + autoBond);
//      appendCmd(str, "set autoLoadOrientation " + autoLoadOrientation);
      if (axesOrientationRasmol)
        appendCmd(str, "set axesOrientationRasmol true");
      appendCmd(str, "set bondRadiusMilliAngstroms " + bondRadiusMilliAngstroms);
      appendCmd(str, "set bondTolerance " + bondTolerance);
      appendCmd(str, "set defaultLattice " + Escape.escapePt(ptDefaultLattice));
      appendCmd(str, "set defaultLoadFilter " + Escape.escapeStr(defaultLoadFilter)) ;
      appendCmd(str, "set defaultLoadScript \"\"");
      if (defaultLoadScript.length() > 0)
        setParamS("defaultLoadScript", defaultLoadScript);
      appendCmd(str, "set defaultStructureDssp " + defaultStructureDSSP);
      String sMode = viewer.getDefaultVdwTypeNameOrData(Integer.MIN_VALUE, null);
      appendCmd(str, "set defaultVDW " + sMode);
      if (sMode.equals("User"))
        appendCmd(str, viewer.getDefaultVdwTypeNameOrData(Integer.MAX_VALUE, null));
      appendCmd(str, "set forceAutoBond " + forceAutoBond);
      appendCmd(str, "#set defaultDirectory " + Escape.escapeStr(defaultDirectory));
      appendCmd(str, "#set loadFormat " + Escape.escapeStr(loadFormat));
      appendCmd(str, "#set loadLigandFormat " + Escape.escapeStr(loadLigandFormat));
      appendCmd(str, "#set smilesUrlFormat " + Escape.escapeStr(smilesUrlFormat));
      appendCmd(str, "#set nihResolverFormat " + Escape.escapeStr(nihResolverFormat));
      appendCmd(str, "#set pubChemFormat " + Escape.escapeStr(pubChemFormat));
      appendCmd(str, "#set edsUrlFormat " + Escape.escapeStr(edsUrlFormat));
      appendCmd(str, "#set edsUrlCutoff " + Escape.escapeStr(edsUrlCutoff));
//      if (autoLoadOrientation)
  //      appendCmd(str, "set autoLoadOrientation true");
      appendCmd(str, "set legacyAutoBonding " + legacyAutoBonding);
      appendCmd(str, "set minBondDistance " + minBondDistance);
      // these next two might be part of a 2D->3D operation
      appendCmd(str, "set minimizationCriterion  " + minimizationCriterion);
      appendCmd(str, "set minimizationSteps  " + minimizationSteps);
      appendCmd(str, "set pdbAddHydrogens " + (htParams != null && htParams.get("pdbNoHydrogens") == null ? pdbAddHydrogens : false));
      appendCmd(str, "set pdbGetHeader " + pdbGetHeader);
      appendCmd(str, "set pdbSequential " + pdbSequential);
      appendCmd(str, "set percentVdwAtom " + percentVdwAtom);
      appendCmd(str, "set smallMoleculeMaxAtoms " + smallMoleculeMaxAtoms);
      appendCmd(str, "set smartAromatic " + smartAromatic);
      if (zeroBasedXyzRasmol)
        appendCmd(str, "set zeroBasedXyzRasmol true");
      return str.toString();
    }

    void setDefaultLattice(Point3f ptLattice) {
      ptDefaultLattice.setT(ptLattice);
    }

    Point3f getDefaultLattice() {
      return ptDefaultLattice;
    }

    //centering and perspective

    boolean allowRotateSelected = false;
    boolean allowMoveAtoms = false;
    boolean perspectiveDepth = true;
    float visualRange = 5f;

    //solvent

    boolean solventOn = false;

    //measurements

    String defaultAngleLabel = "%VALUE %UNITS";
    String defaultDistanceLabel = "%VALUE %UNITS"; //also %_ and %a1 %a2 %m1 %m2, etc.
    String defaultTorsionLabel = "%VALUE %UNITS";
    boolean justifyMeasurements = false;
    boolean measureAllModels = false;

    // minimization  // 11.5.21 03/2008

    int minimizationSteps = 100;
    boolean minimizationRefresh = true;
    boolean minimizationSilent = false;
    float minimizationCriterion = 0.001f;

    //rendering

    boolean antialiasDisplay = false;
    boolean antialiasImages = true;
    boolean imageState = true;
    boolean antialiasTranslucent = true;
    boolean displayCellParameters = true;
    boolean dotsSelectedOnly = false;
    boolean dotSurface = true;
    int dotDensity = 3;
    int dotScale = 1;
    int meshScale = 1;
    boolean dynamicMeasurements = false;
    boolean greyscaleRendering = false;
    boolean isosurfaceKey = false;
    boolean isosurfacePropertySmoothing = true;
    int isosurfacePropertySmoothingPower = 7;
    int repaintWaitMs = 1000;
    boolean showHiddenSelectionHalos = false;
    boolean showKeyStrokes = true;
    boolean showMeasurements = true;
    boolean showTiming = false;
    boolean zoomLarge = true; //false would be like Chime
    String backgroundImageFileName;
    
    //atoms and bonds

    boolean partialDots = false;
    boolean bondModeOr = false;
    boolean hbondsBackbone = false;
    float hbondsAngleMinimum = 90f;
    float hbondsDistanceMaximum = 3.25f;
    boolean hbondsRasmol = true; // 12.0.RC3
    boolean hbondsSolid = false;
    byte modeMultipleBond = JmolConstants.MULTIBOND_NOTSMALL;
    boolean showHydrogens = true;
    boolean showMultipleBonds = true;
    boolean ssbondsBackbone = false;
    float multipleBondSpacing = -1;     // 0.35?
    float multipleBondRadiusFactor = 0; // 0.75?

    //secondary structure + Rasmol

    boolean cartoonBaseEdges = false;
    boolean cartoonRockets = false;
    boolean cartoonFancy = false;
    boolean chainCaseSensitive = false;
    int hermiteLevel = 0;
    boolean highResolutionFlag = false;
    boolean rangeSelected = false;
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting = true;
    int ribbonAspectRatio = 16;
    boolean ribbonBorder = false;
    boolean rocketBarrels = false;
    float sheetSmoothing = 1; // 0: traceAlpha on alphas for helix, 1 on midpoints
    boolean traceAlpha = true;

    //misc

    boolean allowGestures = false;
    boolean allowModelkit = true;
    boolean allowMultiTouch = true; // but you still need to set the parameter multiTouchSparshUI=true
    boolean allowKeyStrokes = false;
    int animationFps = 10;
    boolean atomPicking = true;
    boolean autoFps = false;
    EnumAxesMode axesMode = EnumAxesMode.BOUNDBOX;
    float axesScale = 2;
    boolean bondPicking = false;
    float cameraDepth = 3.0f;
    String dataSeparator = "~~~";
    boolean debugScript = false;
    float defaultDrawArrowScale = 0.5f;
    String defaultLabelXYZ = "%a";
    String defaultLabelPDB = "%m%r";
    float defaultTranslucent = 0.5f;
    int delayMaximumMs = 0;
    float dipoleScale = 1.0f;
    boolean disablePopupMenu = false;
    boolean dragSelected = false;
    boolean drawHover = false;
    boolean drawPicking = false;
    boolean dsspCalcHydrogen = true;
    String energyUnits = "kJ";
    String helpPath = JmolConstants.DEFAULT_HELP_PATH;
    boolean fontScaling = false;
    boolean fontCaching = true;
    String forceField = "MMFF";
    int helixStep = 1;
    boolean hideNameInPopup = false;
    int hoverDelayMs = 500;
    float loadAtomDataTolerance = 0.01f;
    boolean logCommands = false;
    boolean logGestures = false;
    String measureDistanceUnits = "nanometers";
    boolean measurementLabels = true;
    boolean messageStyleChime = false;
    boolean monitorEnergy = false;
    boolean multiProcessor = true;
    int pickingSpinRate = 10;
    String pickLabel = "";
    float pointGroupDistanceTolerance = 0.2f;
    float pointGroupLinearTolerance = 8.0f;
    boolean preserveState = true;
    String propertyColorScheme = "roygb";
    String quaternionFrame = "p"; // was c prior to Jmol 11.7.47
    boolean saveProteinStructureState = true;
    float solventProbeRadius = 1.2f;
    int scriptDelay = 0;
    boolean selectAllModels = true;
    boolean statusReporting = true;
    int strandCountForStrands = 5;
    int strandCountForMeshRibbon = 7;
    int strutSpacing = 6;
    float strutLengthMaximum = 7.0f;
    float strutDefaultRadius = JmolConstants.DEFAULT_STRUT_RADIUS;
    boolean strutsMultiple = false; //on a single position    
    boolean useArcBall = false;
    boolean useMinimizationThread = true;
    boolean useNumberLocalization = true;
    boolean useScriptQueue = true;
    boolean waitForMoveTo = true; // Jmol 11.9.24
    float vectorScale = 1f;
    boolean vectorSymmetry = false; // Jmol 12.3.2
    float vibrationPeriod = 1f;
    float vibrationScale = 1f;
    boolean wireframeRotation = false;

    // window

    boolean hideNavigationPoint = false;
    boolean navigationMode = false;
    //boolean navigateSurface = false;
    boolean navigationPeriodic = false;
    float navigationSpeed = 5;
    boolean showNavigationPointAlways = false;
    String stereoState = null;
    boolean modelKitMode = false;

    // special persistent object characteristics -- bbcage, uccage, axes:

    int[] objColors = new int[OBJ_MAX];
    boolean[] objStateOn = new boolean[OBJ_MAX];
    int[] objMad = new int[OBJ_MAX];

    boolean ellipsoidAxes = false;
    boolean ellipsoidDots = false;
    boolean ellipsoidArcs = false;
    boolean ellipsoidFill = false;
    boolean ellipsoidBall = true;

    int ellipsoidDotCount = 200;
    float ellipsoidAxisDiameter = 0.02f;

    String getWindowState(StringXBuilder sfunc, int width, int height) {
      StringXBuilder str = new StringXBuilder();
      if (sfunc != null) {
        sfunc
            .append("  initialize;\n  set refreshing false;\n  _setWindowState;\n");
        str.append("\nfunction _setWindowState() {\n");
      }
      if (width != 0)
        str.append("# preferredWidthHeight ").appendI(width).append(" ").appendI(height).append(";\n");
      str
      .append("# width ").appendI(width == 0 ? viewer.getScreenWidth() : width)
      .append(";\n# height ").appendI(height == 0 ? viewer.getScreenHeight() : height)
      .append(";\n");
      appendCmd(str, "stateVersion = " + getParameter("_version"));
      appendCmd(str, "background " + Escape.escapeColor(objColors[0]));
      for (int i = 1; i < OBJ_MAX; i++)
        if (objColors[i] != 0)
          appendCmd(str, getObjectNameFromId(i) + "Color = \""
              + Escape.escapeColor(objColors[i]) + '"');
      if (backgroundImageFileName != null)
        appendCmd(str, "background IMAGE /*file*/" + Escape.escapeStr(backgroundImageFileName));
      str.append(getSpecularState());
      appendCmd(str, "statusReporting  = " + statusReporting);
      if (sfunc != null)
        str.append("}\n\n");
      return str.toString();
    }

    String getSpecularState() {
      StringXBuilder str = new StringXBuilder();
      appendCmd(str, "set ambientPercent " + GData.getAmbientPercent());
      appendCmd(str, "set diffusePercent " + GData.getDiffusePercent());
      appendCmd(str, "set specular " + GData.getSpecular());
      appendCmd(str, "set specularPercent " + GData.getSpecularPercent());
      appendCmd(str, "set specularPower " + GData.getSpecularPower());
      int se = GData.getSpecularExponent();
      int pe = GData.getPhongExponent();
      if (Math.pow(2, se) == pe)
        appendCmd(str, "set specularExponent " + se);
      else
        appendCmd(str, "set phongExponent " + pe);        
      appendCmd(str, "set zShadePower " + zShadePower);
      return str.toString();
    }

    //testing

    boolean testFlag1 = false;
    boolean testFlag2 = false;
    boolean testFlag3 = false;
    boolean testFlag4 = false;

    //controlled access:

    void setUnits(String units) {
      String mu = measureDistanceUnits;
      String eu = energyUnits;
      if (units.equalsIgnoreCase("angstroms"))
        measureDistanceUnits = "angstroms";
      else if (units.equalsIgnoreCase("nanometers")
          || units.equalsIgnoreCase("nm"))
        measureDistanceUnits = "nanometers";
      else if (units.equalsIgnoreCase("picometers")
          || units.equalsIgnoreCase("pm"))
        measureDistanceUnits = "picometers";
      else if (units.equalsIgnoreCase("bohr") || units.equalsIgnoreCase("au"))
        measureDistanceUnits = "au";
      else if (units.equalsIgnoreCase("vanderwaals") || units.equalsIgnoreCase("vdw"))
        measureDistanceUnits = "vdw";
      else if (units.equalsIgnoreCase("kj"))
        energyUnits = "kJ";
      else if (units.equalsIgnoreCase("kcal"))
        energyUnits = "kcal";
      if (!mu.equalsIgnoreCase(measureDistanceUnits))
        setParamS("measurementUnits", measureDistanceUnits);
      else if (!eu.equalsIgnoreCase(energyUnits)) 
        setParamS("energyUnits", energyUnits);
    }

    boolean isJmolVariable(String key) {
      return key.charAt(0) == '_'
          || htNonbooleanParameterValues.containsKey(key = key.toLowerCase())
          || htBooleanParameterFlags.containsKey(key)
          || unreportedProperties.indexOf(";" + key + ";") >= 0;
    }

    private void resetValue(String name, GlobalSettings g) {
      setParamS(name, g == null ? "" : (String) g.getParameter(name));
    }
    
    public void setParamB(String name, boolean value) {
      name = name.toLowerCase();
      if (htNonbooleanParameterValues.containsKey(name))
        return; // don't allow setting boolean of a numeric
      htBooleanParameterFlags.put(name, value ? Boolean.TRUE : Boolean.FALSE);
    }

    void setParamI(String name, int value) {
      name = name.toLowerCase();
      if (htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htNonbooleanParameterValues.put(name, Integer.valueOf(value));
    }

    public void setParamF(String name, float value) {
      if (Float.isNaN(value))
        return;
      name = name.toLowerCase();
      if (htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting numeric of a boolean
      htNonbooleanParameterValues.put(name, new Float(value));
    }

    void setParamS(String name, String value) {
      name = name.toLowerCase();
      if (value == null || htBooleanParameterFlags.containsKey(name))
        return; // don't allow setting string of a boolean
      htNonbooleanParameterValues.put(name, value);
    }

    void removeParam(String key) {
      // used by resetError to remove _errorMessage
      // used by setSmilesString to remove _smilesString
      // used by setAxesModeMolecular to remove axesUnitCell
      //   and either axesWindow or axesMolecular
      // used by setAxesModeUnitCell to remove axesMolecular
      //   and either remove axesWindow or axesUnitCell

      key = key.toLowerCase();
      if (htBooleanParameterFlags.containsKey(key)) {
        htBooleanParameterFlags.remove(key);
        if (!htPropertyFlagsRemoved.containsKey(key))
          htPropertyFlagsRemoved.put(key, Boolean.FALSE);
        return;
      }
      if (htNonbooleanParameterValues.containsKey(key))
        htNonbooleanParameterValues.remove(key);
    }

    ScriptVariable setUserVariable(String key, ScriptVariable var) {
      if (var == null) 
        return null;
//      System.out.println("stateman setting user variable " + key );
      key = key.toLowerCase();
      htUserVariables.put(key, var.setName(key).setGlobal());
      return var;
    }

    void unsetUserVariable(String key) {
      if (key.equals("all") || key.equals("variables")) {
        htUserVariables.clear();
        Logger.info("all user-defined variables deleted");
      } else if (htUserVariables.containsKey(key)) {
        Logger.info("variable " + key + " deleted");
        htUserVariables.remove(key);
      }
    }

    void removeUserVariable(String key) {
      htUserVariables.remove(key);
    }

    ScriptVariable getUserVariable(String name) {
      if (name == null)
        return null;
      name = name.toLowerCase();
      return htUserVariables.get(name);
    }

    String getParameterEscaped(String name, int nMax) {
      name = name.toLowerCase();
      if (htNonbooleanParameterValues.containsKey(name)) {
        Object v = htNonbooleanParameterValues.get(name);
        return varClip(name, Escape.escape(v), nMax);
      }
      if (htBooleanParameterFlags.containsKey(name))
        return htBooleanParameterFlags.get(name).toString();
      if (htUserVariables.containsKey(name))
        return htUserVariables.get(name).escape();
      if (htPropertyFlagsRemoved.containsKey(name))
        return "false";
      return "<not defined>";
    }

    /**
     * 
     * strictly a getter -- returns "" if not found
     * 
     * @param name
     * @return      a Integer, Float, String, BitSet, or Variable
     */
    Object getParameter(String name) {
      Object v = getParam(name, false);
      return (v == null ? "" : v);
    }

    /**
     *  
     * 
     * @param name
     * @param doSet
     * @return     a new variable if possible, but null if "_xxx"
     * 
     */
    ScriptVariable getOrSetNewVariable(String name, boolean doSet) {
      if (name == null || name.length() == 0)
        name = "x";
      Object v = getParam(name, true);
      return (v == null && doSet && name.charAt(0) != '_' ?
        setUserVariable(name, ScriptVariable.newVariable(Token.string, ""))
         : ScriptVariable.getVariable(v));
    }

    Object getParam(String name, boolean asVariable) {
      name = name.toLowerCase();
      if (name.equals("_memory")) {
      	float bTotal = 0;
      	float bFree = 0;
      	try {
          Runtime runtime = Runtime.getRuntime();
          bTotal = runtime.totalMemory() / 1000000f;
          bFree = runtime.freeMemory() / 1000000f;
        } catch (Throwable e) {
      		// Runtime absent (JavaScript)
      	}
        String value = TextFormat.formatDecimal(bTotal - bFree, 1) + "/"
            + TextFormat.formatDecimal(bTotal, 1);
        htNonbooleanParameterValues.put("_memory", value);
      }
      if (htNonbooleanParameterValues.containsKey(name))
        return htNonbooleanParameterValues.get(name);
      if (htBooleanParameterFlags.containsKey(name))
        return htBooleanParameterFlags.get(name);
      if (htPropertyFlagsRemoved.containsKey(name))
        return Boolean.FALSE;
      if (htUserVariables.containsKey(name)) {
        ScriptVariable v = htUserVariables.get(name);
        return (asVariable ? v : ScriptVariable.oValue(v));
      }
      return null;
    }

    String getAllSettings(String prefix) {
      StringXBuilder commands = new StringXBuilder();
      Iterator<String> e;
      String key;
      String[] list = new String[htBooleanParameterFlags.size()
          + htNonbooleanParameterValues.size()+ htUserVariables.size()];
      //booleans
      int n = 0;
      String _prefix = "_" + prefix;
      e = htBooleanParameterFlags.keySet().iterator();
      while (e.hasNext()) {
        key = e.next();
        if (prefix == null || key.indexOf(prefix) == 0
            || key.indexOf(_prefix) == 0)
          list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
              + htBooleanParameterFlags.get(key);
      }
      //save as _xxxx if you don't want "set" to be there first
      e = htNonbooleanParameterValues.keySet().iterator();
      while (e.hasNext()) {
        key = e.next();
        if (key.charAt(0) != '@'
            && (prefix == null || key.indexOf(prefix) == 0 || key
                .indexOf(_prefix) == 0)) {
          Object value = htNonbooleanParameterValues.get(key);
          if (value instanceof String)
            value = chop(Escape.escapeStr((String) value));
          list[n++] = (key.indexOf("_") == 0 ? key + " = " : "set " + key + " ")
              + value;
        }
      }
      e = htUserVariables.keySet().iterator();
      while (e.hasNext()) {
        key = e.next();
        if (prefix == null || key.indexOf(prefix) == 0) {
          ScriptVariable value = htUserVariables.get(key);
          String s = value.asString();
          list[n++] = key + " " + (key.startsWith("@") ? "" : "= ") + (value.tok == Token.string ? chop(Escape.escapeStr(s)) : s);
        }
      }
      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);
      commands.append("\n");
      return commands.toString();
    }

    private String chop(String s) {
      int len = s.length();
      if (len < 512)
        return s;
      StringXBuilder sb = new StringXBuilder();
      String sep = "\"\\\n    + \"";
      int pt = 0;
      for (int i = 72; i < len; pt = i, i += 72) {
        while (s.charAt(i - 1) == '\\')
          i++;
        sb.append((pt == 0 ? "" : sep)).append(s.substring(pt, i));
      }
      sb.append(sep).append(s.substring(pt, len));
      return sb.toString();
    }

    String getState(StringXBuilder sfunc) {
      String[] list = new String[htBooleanParameterFlags.size()
          + htNonbooleanParameterValues.size()];
      StringXBuilder commands = new StringXBuilder();
      boolean isState = (sfunc != null);
      if (isState) {
        sfunc.append("  _setVariableState;\n");
        commands.append("function _setVariableState() {\n\n");
      }
      int n = 0;
      Iterator<String> e;
      String key;
      //booleans
      e = htBooleanParameterFlags.keySet().iterator();
      while (e.hasNext()) {
        key = e.next();       
        if (doReportProperty(key))
          list[n++] = "set " + key + " " + htBooleanParameterFlags.get(key);
      }
      e = htNonbooleanParameterValues.keySet().iterator();
      while (e.hasNext()) {
        key = e.next();
        if (doReportProperty(key)) {
          Object value = htNonbooleanParameterValues.get(key);
          if (key.charAt(0) == '=') {
            //save as =xxxx if you don't want "set" to be there first
            // (=color [element], =frame ...; set unitcell) -- see Viewer.java
            key = key.substring(1);
          } else {
            if (key.indexOf("default") == 0)
              key = " set " + key;
            else
              key = "set " + key;
            value = Escape.escape(value);
          }
          list[n++] = key + " " + value;
        }
      }
      switch (axesMode) {
      case UNITCELL:
        list[n++] = "set axes unitcell";
        break;
      case BOUNDBOX:
        list[n++] = "set axes window";
        break;
      default:
        list[n++] = "set axes molecular";
      }

      Arrays.sort(list, 0, n);
      for (int i = 0; i < n; i++)
        if (list[i] != null)
          appendCmd(commands, list[i]);

      String s = StateManager.getVariableList(htUserVariables, 0, false, true);
      if (s.length() > 0) {
        commands.append("\n#user-defined atom sets; \n");
        commands.append(s);
      }

      // label defaults

      viewer.loadShape(JmolConstants.SHAPE_LABELS);
      commands.append((String) viewer.getShapeProperty(JmolConstants.SHAPE_LABELS,
          "defaultState"));

      // structure defaults
      
      if (haveSetStructureList) {
        commands.append("struture HELIX set " + Escape.escape(structureList.get(EnumStructure.HELIX)));
        commands.append("struture SHEET set " + Escape.escape(structureList.get(EnumStructure.SHEET)));
        commands.append("struture TURN set " + Escape.escape(structureList.get(EnumStructure.TURN)));
      }
      if (sfunc != null)
        commands.append("\n}\n\n");
      return commands.toString();
    }

    private boolean doReportProperty(String name) {
      return (name.charAt(0) != '_' && unreportedProperties.indexOf(";" + name
          + ";") < 0);
    }

    String getVariableList() {
      return StateManager.getVariableList(htUserVariables, 0, true, false);
    }

    // static because we don't plan to be changing these
    private Map<EnumStructure, float[]> structureList = new Hashtable<EnumStructure, float[]>();
    
    {
      structureList.put(EnumStructure.TURN, 
          new float[] { // turn
              30, 90, -15, 95,
          });
      structureList.put(EnumStructure.SHEET, 
      new float[] { // sheet
          -180, -10,   70,  180, 
          -180, -45, -180, -130, 
           140, 180,   90, 180, 
        });
      structureList.put(EnumStructure.HELIX, 
      new float[] {  // helix
        -160, 0, -100, 45,
      });
    }
    
    private boolean haveSetStructureList;
    private String[] userDatabases;
    
    public void setStructureList(float[] list, EnumStructure type) {
      haveSetStructureList = true;
      structureList.put(type, list);
    }
    
    public Map<EnumStructure, float[]> getStructureList() {
      return structureList;
    }

    void setPicked(int atomIndex) {
      ScriptVariable pickedSet = null;
      if (atomIndex >= 0) {
        setParamI("_atompicked", atomIndex);
        pickedSet = (ScriptVariable) getParam("picked", true);
      }
      if (pickedSet == null || pickedSet.tok != Token.bitset) {
        pickedSet = ScriptVariable.newVariable(Token.bitset, new BitSet());
        setUserVariable("picked", pickedSet);
      }
      if (atomIndex >= 0)
        ScriptVariable.getBitSet(pickedSet, false).set(atomIndex);
    }

    public String resolveDataBase(String database, String id) {
      String format = databases.get(database.toLowerCase());
      if (format == null)
        return null;
      if (id.indexOf("/") < 0) {
        if (database.equals("pubchem"))
          id = "name/" + id;
        else if (database.equals("nci"))
          id += "/file?format=sdf&get3d=True";
      }
      return (format.indexOf("%FILE") < 0 ? format + id : TextFormat
          .formatStringS(format, "FILE", id));
    }

    private void getDataBaseList(String[] list) {
      if (list == null)
        return;
      for (int i = 0; i < list.length; i += 2)
        databases.put(list[i].toLowerCase(), list[i + 1]);
    }
  }

  ///////// state serialization 

  public static void setStateInfo(Map<String, BitSet> ht,
                                  int i1, int i2, String key) {
    BitSet bs;
    if (ht.containsKey(key)) {
      bs = ht.get(key);
    } else {
      bs = new BitSet();
      ht.put(key, bs);
    }
    bs.setBits(i1, i2 + 1);
  }

  public static String varClip(String name, String sv, int nMax) {
    if (nMax > 0 && sv.length() > nMax)
      sv = sv.substring(0, nMax) + " #...more (" + sv.length()
          + " bytes -- use SHOW " + name + " or MESSAGE @" + name
          + " to view)";
    return sv;
  }

  public static String getCommands(Map<String, BitSet> htDefine,
                                   Map<String, BitSet> htMore,
                                   String selectCmd) {
    StringXBuilder s = new StringXBuilder();
    String setPrev = getCommands2(htDefine, s, null, selectCmd);
    if (htMore != null)
      getCommands2(htMore, s, setPrev, "select");
    return s.toString();
  }

  private static String getCommands2(Map<String, BitSet> ht,
                                    StringXBuilder s,
                                    String setPrev, String selectCmd) {
    if (ht == null)
      return "";
    for (Map.Entry<String, BitSet> entry : ht.entrySet()) {
      String key = entry.getKey();
      String set = Escape.escape(entry.getValue());
      if (set.length() < 5) // nothing selected
        continue;
      set = selectCmd + " " + set;
      if (!set.equals(setPrev))
        appendCmd(s, set);
      setPrev = set;
      if (key.indexOf("-") != 0) // - for key means none required
        appendCmd(s, key);
    }
    return setPrev;
  }

  public static void appendCmd(StringXBuilder s, String cmd) {
    if (cmd.length() == 0)
      return;
    s.append("  ").append(cmd).append(";\n");
  }
}
