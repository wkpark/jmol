/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import org.jmol.shape.Shape;
import org.jmol.symmetry.UnitCell;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.AtomIndexIterator;
import org.jmol.modelset.BoxInfo;
import org.jmol.modelset.ModelSet;

import org.jmol.api.*;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.g3d.*;
import org.jmol.util.BitSetUtil;
import org.jmol.util.CommandHistory;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Base64;
import org.jmol.util.JpegEncoder;
import org.jmol.util.Measure;
import org.jmol.util.TempArray;
import org.jmol.util.TextFormat;
import org.jmol.util.Parser;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Properties;
import java.util.Vector;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.Reader;

/*
 * 
 * ****************************************************************
 * The JmolViewer can be used to render client molecules. Clients implement the
 * JmolAdapter. JmolViewer uses this interface to extract information from the
 * client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.1 virtual machines. The 3d graphics rendering
 * package is a software implementation of a z-buffer. It does not use Java3D
 * and does not use Graphics2D from Java 1.2. Therefore, it is well suited to
 * building web browser applets that will run on a wide variety of system
 * configurations.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea
 * being that applet.getProperty("jmolViewer") returns this Viewer object,
 * allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 * ****************************************************************
 */

public class Viewer extends JmolViewer implements AtomDataServer {

  protected void finalize() throws Throwable {
    Logger.debug("viewer finalize " + this);
    super.finalize();
  }

  // these are all private now so we are certain they are not 
  // being accesed by any other classes

  
  private Component display;
  private Graphics3D g3d;
  private JmolAdapter modelAdapter;

  private CommandHistory commandHistory = new CommandHistory();
  private ColorManager colorManager;
  private Compiler compiler;
  Compiler getCompiler() {
    return compiler;
  }

  private MinimizerInterface minimizer;
  public void setMinimizer(MinimizerInterface minimizer) { 
    this.minimizer = minimizer;
    if (minimizer != null)
      minimizer.setProperty("viewer", this);
  }
  
  public MinimizerInterface getMinimizer() {
    if (minimizer != null)
      minimizer.setProperty("viewer", this);
    return minimizer;
  }
  
  private Eval eval;
  private DataManager dataManager;
  private FileManager fileManager;
  private ModelManager modelManager;
  private ModelSet modelSet;
  public MouseManager mouseManager;
  private PickingManager pickingManager;
  private RepaintManager repaintManager;
  private ScriptManager scriptManager;
  private SelectionManager selectionManager;
  private StateManager stateManager;
  private StateManager.GlobalSettings global;
  StateManager.GlobalSettings getGlobalSettings() {
    return global;
  }
  
  private StatusManager statusManager;
  private TempArray tempManager;
  private TransformManager transformManager;

  private String strJavaVendor;
  private String strJavaVersion;
  private String strOSName;
  private String htmlName = "";
  private String fullName = "";
  private String syncId = "";
  private String appletDocumentBase = "";
  private String appletCodeBase = "";

  private boolean jvm11orGreater = false;
  private boolean jvm12orGreater = false;
  private boolean jvm14orGreater = false;

  private Hashtable evalVariables;
  Hashtable getEvalVariables() {
    return evalVariables;
  }
  
  public Viewer(Component display, JmolAdapter modelAdapter) {
    if (Logger.debugging) {
      Logger.debug("Viewer constructor " + this);
    }
    this.display = display;
    this.modelAdapter = modelAdapter;
    strJavaVendor = System.getProperty("java.vendor");
    strOSName = System.getProperty("os.name");
    strJavaVersion = System.getProperty("java.version");
    // Netscape on MacOS does not implement 1.1 event model
    jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 && !(strJavaVendor
        .startsWith("Netscape")
        && strJavaVersion.compareTo("1.1.5") <= 0 && "Mac OS".equals(strOSName)));
    jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);
    stateManager = new StateManager(this);
    g3d = new Graphics3D(display);
    colorManager = new ColorManager(this, g3d);
    //initialize();
    statusManager = new StatusManager(this);
    scriptManager = new ScriptManager(this);
    transformManager = new TransformManager11(this);
    //transformManager = new TransformManager10(this);
    selectionManager = new SelectionManager(this);
    pickingManager = new PickingManager(this);
    if (jvm14orGreater)
      mouseManager = MouseWrapper14.alloc(display, this);
    else if (jvm11orGreater)
      mouseManager = MouseWrapper11.alloc(display, this);
    else
      mouseManager = new MouseManager10(display, this);
    modelManager = new ModelManager(this);
    tempManager = new TempArray();
    dataManager = new DataManager();
    repaintManager = new RepaintManager(this);
    initialize();
    fileManager = new FileManager(this, modelAdapter);
    compiler = new Compiler(this);
    evalVariables = new Hashtable();
    eval = new Eval(this);
  }
  
  /**
   * NOTE: for APPLICATION AND APPLET call
   * 
   *   setModeMouse(JmolConstants.MOUSE_NONE);
   * 
   * before setting viewer=null
   * 
   * in order to remove references to display window in listeners and hoverWatcher
   * 
   *  This is the main access point for creating an application
   *  or applet viewer. After allocation it is MANDATORY that one of 
   *  the next commands is either 
   *  
   *      viewer.evalString("ZAP");
   *    
   *    or at least:
   *    
   *      viewer.setAppletContext("",null,null,"")
   *    
   *    One or the other of these is necessary to establish the 
   *    first modelset, which might be required by one or more
   *    later evaluated commands or file loadings.
   *    
   * @param display       either DisplayPanel or WrappedApplet
   * @param modelAdapter  the model reader
   * @return              a viewer instance 
   */
  public static JmolViewer allocateViewer(Component display,
                                          JmolAdapter modelAdapter) {
    return new Viewer(display, modelAdapter);
  }

  private boolean isSilent = false;
  private boolean isApplet = false;
  private boolean autoExit = false;
  private String writeInfo;
  private boolean haveDisplay = true;
  private boolean mustRender = true;
  private boolean checkScriptOnly = false;
  private boolean listCommands = false;
  private boolean fileOpenCheck = true;
  private boolean useCommandThread = false;
  private boolean isSignedApplet = false;

  public boolean isApplet() {
    return (htmlName.length() > 0);
  }

  public void setAppletContext(String fullName, URL documentBase, URL codeBase,
                               String appletProxyOrCommandOptions) {
    this.fullName = fullName = (fullName == null ? "" : fullName);
    this.appletDocumentBase = (documentBase == null ? "" : documentBase
        .toString());
    this.appletCodeBase = (codeBase == null ? "" : codeBase.toString());
    int i = fullName.lastIndexOf("[");
    this.htmlName = (i < 0 ? fullName : fullName.substring(0, i));
    this.syncId = (i < 0 ? "" : fullName
        .substring(i + 1, fullName.length() - 1));
    isApplet = (documentBase != null);
    String str = appletProxyOrCommandOptions;
    if (str == null)
      str = "";
    String mem = (String) getParameter("_memory");

    String appletProxy = null;
    if (isApplet && (i = str.indexOf("-appletProxy ")) >= 0) {
      appletProxy = str.substring(i + 13);
      str = str.substring(0, i);
    }

    useCommandThread = (str.indexOf("-t") >= 0);
    if (useCommandThread)
      scriptManager.startCommandWatcher(true);
    isSignedApplet = (isApplet && str.indexOf("-signed") >= 0);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_useCommandThread", useCommandThread);
    if (!isApplet) {
      // not an applet -- used to pass along command line options
      if (str.indexOf("-i") >= 0) {
        Logger.setLogLevel(Logger.LEVEL_WARN); //no info, but warnings and errors
        isSilent = true;
      }
      if (str.indexOf("-c") >= 0) {
        checkScriptOnly = true;
        fileOpenCheck = true;
      }
      if (str.indexOf("-C") >= 0) {
        checkScriptOnly = true;
        fileOpenCheck = false;
      }
      if (str.indexOf("-l") >= 0) {
        listCommands = true;
      }
      if (str.indexOf("-x") >= 0) {
        autoExit = true;
      }
      if (display == null || str.indexOf("-n") >= 0) {
        haveDisplay = false;
        display = null;
      }
      writeInfo = null;
      if (str.indexOf("-w") >= 0) {
        i = str.indexOf("\1");
        int j = str.lastIndexOf("\1");
        writeInfo = str.substring(i + 1, j);
      }
      mustRender = (haveDisplay || writeInfo != null);
    }

    /*
     Logger.info("jvm11orGreater=" + jvm11orGreater
     + "\njvm12orGreater=" + jvm12orGreater + "\njvm14orGreater="
     + jvm14orGreater);
     */
    if (!isSilent) {
      Logger.info(JmolConstants.copyright
          + "\nJmol Version "
          + getJmolVersion()
          + "\njava.vendor:"
          + strJavaVendor
          + "\njava.version:"
          + strJavaVersion
          + "\nos.name:"
          + strOSName
          + "\nmemory:"
          + mem
          + "\nuseCommandThread: "
          + useCommandThread
          + (!isApplet ? "" : "\nappletId:" + htmlName
              + (isSignedApplet ? " (signed)" : "")));
    }

    if (isApplet)
      fileManager.setAppletContext(documentBase, codeBase, appletProxy);
    zap(false, false); //here to allow echos
    global.setParameterValue("language", GT.getLanguage());
  }

  public static String getJmolVersion() {
    return JmolConstants.version + "  " + JmolConstants.date;
  }

  public String getExportDriverList() {
    return (String) global.getParameter("exportDrivers");
  }

  private static int getJmolVersionInt() {
    //11.9.999 --> 1109999
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

  String getHtmlName() {
    return htmlName;
  }

  boolean mustRenderFlag() {
    return mustRender && refreshing;
  }

  static int getLogLevel() {
    for (int i = 0; i < Logger.LEVEL_MAX; i++)
      if (Logger.isActiveLevel(i))
        return Logger.LEVEL_MAX - i;
    return 0;
  }

  public Component getAwtComponent() {
    return display;
  }

  public boolean handleOldJvm10Event(Event e) {
    return mouseManager.handleOldJvm10Event(e);
  }

  void reset() {
    //Eval.reset()
    //initializeModel
    modelSet.calcBoundBoxDimensions(null);
    axesAreTainted = true;
    transformManager.homePosition();
    if (modelSet.setCrystallographicDefaults())
      stateManager.setCrystallographicDefaults();//modelSet.someModelsHavePeriodicOrigin);
    else
      setAxesModeMolecular(false);

    refresh(1, "Viewer:homePosition()");
  }

  public void homePosition() {
    evalString("reset");
  }

  /*
   final Hashtable imageCache = new Hashtable();

   void flushCachedImages() {
   imageCache.clear();
   colorManager.flushCachedColors();
   }
   */

  Hashtable getAppletInfo() {
    Hashtable info = new Hashtable();
    info.put("htmlName", htmlName);
    info.put("syncId", syncId);
    info.put("fullName", fullName);
    if (isApplet) {
      info.put("documentBase", appletDocumentBase);
      info.put("codeBase", appletCodeBase);
      info.put("registry", statusManager.getRegistryInfo());
    }
    info.put("version", JmolConstants.version);
    info.put("date", JmolConstants.date);
    info.put("javaVendor", strJavaVendor);
    info.put("javaVersion", strJavaVersion);
    info.put("operatingSystem", strOSName);
    return info;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

  void initialize() {
    boolean haveGlobal = (global != null);
    boolean debugScript = (haveGlobal ? global.debugScript : false);
    boolean messageStyleChime = (haveGlobal ? global.messageStyleChime : false);
    global = stateManager.getGlobalSettings();
    if (haveGlobal) {
      setBooleanProperty("debugScript", debugScript);
      setBooleanProperty("messageStyleChime", messageStyleChime);
    }
    setIntProperty("_version", getJmolVersionInt(), true);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_useCommandThread", useCommandThread);
    colorManager.resetElementColors();
    setObjectColor("background", "black");
    setObjectColor("axis1", "red");
    setObjectColor("axis2", "green");
    setObjectColor("axis3", "blue");
    
    //transfer default global settings to managers and g3d
    
    setZShade(global.zShade);
    setAmbientPercent(global.ambientPercent);
    setDiffusePercent(global.diffusePercent);
    setSpecular(global.specular);
    setSpecularPercent(global.specularPercent);
    setSpecularExponent(global.specularExponent);
    setSpecularPower(global.specularPower);
    
    repaintManager.setAnimationFps(global.animationFps);

    statusManager.setAllowStatusReporting(global.statusReporting);

    setTransformManagerDefaults();
    
  }

  String listSavedStates() {
    return stateManager.listSavedStates();
  }

  void saveOrientation(String saveName) {
    //from Eval
    stateManager.saveOrientation(saveName);
  }

  boolean restoreOrientation(String saveName, float timeSeconds) {
    //from Eval
    return stateManager.restoreOrientation(saveName, timeSeconds);
  }

  void saveBonds(String saveName) {
    //from Eval
    stateManager.saveBonds(saveName);
  }

  boolean restoreBonds(String saveName) {
    //from Eval
    clearMinimization();
    return stateManager.restoreBonds(saveName);
  }

  void saveState(String saveName) {
    //from Eval
    stateManager.saveState(saveName);
  }

  public String getSavedState(String saveName) {
    return stateManager.getSavedState(saveName);
  }

  void saveStructure(String saveName) {
    //from Eval
    stateManager.saveStructure(saveName);
  }

  String getSavedStructure(String saveName) {
    return stateManager.getSavedStructure(saveName);
  }

  public void saveCoordinates(String saveName, BitSet bsSelected) {
    //from Eval
    stateManager.saveCoordinates(saveName, bsSelected);
  }

  String getSavedCoordinates(String saveName) {
    return stateManager.getSavedCoordinates(saveName);
  }

  void saveSelection(String saveName) {
    //from Eval
    stateManager.saveSelection(saveName, selectionManager.bsSelection);
    stateManager.restoreSelection(saveName); //just to register the # of selected atoms
  }

  boolean restoreSelection(String saveName) {
    //from Eval
    return stateManager.restoreSelection(saveName);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  public Matrix4f getMatrixtransform() {
    return transformManager.getMatrixtransform();
  }
  
  public float getRotationRadius() {
    return transformManager.getRotationRadius();
  }

  public void setRotationRadius(float angstroms) {
    transformManager.setRotationRadius(angstroms);
  }

  public Point3f getRotationCenter() {
    return transformManager.getRotationCenter();
  }

  void setCenterAt(String relativeTo, Point3f pt) {
    //Eval centerAt boundbox|absolute|average {pt}
    transformManager.setCenterAt(relativeTo, pt);
    refresh(0, "Viewer:setCenter(" + relativeTo + ")");
  }

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    //Eval 
    //setCenterSelected

    Point3f center = (bsCenter != null
        && BitSetUtil.cardinalityOf(bsCenter) > 0 ? getAtomSetCenter(bsCenter)
        : null);
    transformManager.setNewRotationCenter(center, doScale);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  void setNewRotationCenter(Point3f center) {
    // eval ???
    transformManager.setNewRotationCenter(center, true);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  Point3f getNavigationCenter() {
    return transformManager.getNavigationCenter();
  }

  public boolean getNavigationCentered() {
    return transformManager.isNavigationCentered();
  }

  float getNavigationDepthPercent() {
    return transformManager.getNavigationDepthPercent();
  }

  void navigate(int keyWhere, int modifiers) {
    transformManager.navigate(keyWhere, modifiers);
    if (!transformManager.vibrationOn)
      refresh(1, "Viewer:navigate()");
  }

  public Point3f getNavigationOffset() {
    return transformManager.getNavigationOffset();
  }

  float getNavigationOffsetPercent(char XorY) {
    return transformManager.getNavigationOffsetPercent(XorY);
  }

  public boolean getNavigating() {
    return transformManager.getNavigating();
  }

  boolean isInPosition(Point3f pt, float degrees) {
    return transformManager.isInPosition(pt, degrees);
  }

  void move(Vector3f dRot, float dZoom, Vector3f dTrans, float dSlab,
            float floatSecondsTotal, int fps) {
    //from Eval
    transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  void moveTo(float floatSecondsTotal, Point3f center, Point3f pt,
              float degrees, float zoom, float xTrans, float yTrans,
              float rotationRadius, Point3f navCenter, float xNav, float yNav,
              float navDepth) {
    //from Eval
    transformManager.moveTo(floatSecondsTotal, center, pt, degrees, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  void moveTo(float floatSecondsTotal, Matrix3f rotationMatrix, Point3f center,
              float zoom, float xTrans, float yTrans, float rotationRadius,
              Point3f navCenter, float xNav, float yNav, float navDepth) {
    //from StateManager -- -1 for time --> no repaint
    transformManager.moveTo(floatSecondsTotal, rotationMatrix, center, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  String getMoveToText(float timespan) {
    return transformManager.getMoveToText(timespan, true);
  }

  void navigate(float timeSeconds, Point3f[] path, float[] theta,
                int indexStart, int indexEnd) {
    transformManager.navigate(timeSeconds, path, theta, indexStart, indexEnd);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Point3f center) {
    transformManager.navigate(timeSeconds, center);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Point3f[][] pathGuide) {
    transformManager.navigate(timeSeconds, pathGuide);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Vector3f rotAxis, float degrees) {
    transformManager.navigate(timeSeconds, rotAxis, degrees);
    refresh(1, "navigate");
  }

  void navTranslate(float timeSeconds, Point3f center) {
    transformManager.navTranslate(timeSeconds, center);
    refresh(1, "navigate");
  }

  void navTranslatePercent(float timeSeconds, float x, float y) {
    transformManager.navTranslatePercent(timeSeconds, x, y);
    refresh(1, "navigate");
  }

  // refresh(2 indicates this is a mouse motion -- not going through Eval script
  // so we bypass Eval and mainline on the other viewer!
  
  void zoomBy(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.zoomBy(pixels);
    refresh(2, syncingMouse ? "Mouse: zoomBy " + pixels : "");
  }

  void zoomByFactor(float factor) {
    //MouseManager.mouseWheel
    transformManager.zoomByFactor(factor);
    refresh(2, syncingMouse ? "Mouse: zoomByFactor " + factor : "");
  }

  void rotateXYBy(int xDelta, int yDelta) {
    //mouseSinglePressDrag
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh(2, syncingMouse ? "Mouse: rotateXYBy " + xDelta + " " + yDelta : "");
  }

  void rotateZBy(int zDelta) {
    //mouseSinglePressDrag
    transformManager.rotateZBy(zDelta);
    refresh(2, syncingMouse ? "Mouse: rotateZBy " + zDelta : "");
  }

  void rotateMolecule(int deltaX, int deltaY) {
    setRotateSelected(true);
    setRotateMolecule(true);
    transformManager.rotateXYBy(deltaX, deltaY);
    setRotateMolecule(false);
    setRotateSelected(false);
    refresh(2, syncingMouse ? "Mouse: rotateMolecule " + deltaX + " " + deltaY : "");
  }

  void translateXYBy(int xDelta, int yDelta) {
    //mouseDoublePressDrag, mouseSinglePressDrag
    transformManager.translateXYBy(xDelta, yDelta);
    refresh(2, syncingMouse ? "Mouse: translateXYBy " + xDelta + " " + yDelta : "");
  }

  public void rotateFront() {
    //deprecated
    transformManager.rotateFront();
    refresh(1, "Viewer:rotateFront()");
  }

  public void rotateToX(float angleRadians) {
    //deprecated
    transformManager.rotateToX(angleRadians);
    refresh(1, "Viewer:rotateToX()");
  }

  public void rotateToY(float angleRadians) {
    //deprecated
    transformManager.rotateToY(angleRadians);
    refresh(1, "Viewer:rotateToY()");
  }

  public void rotateToZ(float angleRadians) {
    //deprecated
    transformManager.rotateToZ(angleRadians);
    refresh(1, "Viewer:rotateToZ()");
  }

  public void rotateToX(int angleDegrees) {
    //deprecated
    rotateToX(angleDegrees * Measure.radiansPerDegree);
  }

  public void rotateToY(int angleDegrees) {
    //deprecated
    rotateToY(angleDegrees * Measure.radiansPerDegree);
  }

  void translateToXPercent(float percent) {
    //Eval.translate()
    transformManager.translateToXPercent(percent);
    refresh(1, "Viewer:translateToXPercent()");
  }

  void translateToYPercent(float percent) {
    //Eval.translate()
    transformManager.translateToYPercent(percent);
    refresh(1, "Viewer:translateToYPercent()");
  }

  void translateToZPercent(float percent) {
    //Eval.translate()
    transformManager.translateToZPercent(percent);
    refresh(1, "Viewer:translateToZPercent()");
  }

  float getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  float getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  float getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
  }

  String getTranslationScript() {
    return transformManager.getTranslationScript();
  }

  public int getZoomPercent() {
    //deprecated 
    return (int) getZoomPercentFloat();
  }

  public float getZoomPercentFloat() {
    return transformManager.getZoomPercentFloat();
  }

  float getMaxZoomPercent() {
    return TransformManager.MAXIMUM_ZOOM_PERCENTAGE;
  }

  private void setZoomEnabled(boolean zoomEnabled) {
    transformManager.setZoomEnabled(zoomEnabled);
    refresh(1, "Viewer:setZoomEnabled()");
  }

  void slabReset() {
    transformManager.slabReset();
  }

  boolean getZoomEnabled() {
    return transformManager.zoomEnabled;
  }

  public boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  void slabByPixels(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.slabByPercentagePoints(pixels);
    refresh(0, "Viewer:slabByPixels()");
  }

  void depthByPixels(int pixels) {
    //MouseManager.mouseDoublePressDrag
    transformManager.depthByPercentagePoints(pixels);
    refresh(0, "Viewer:depthByPixels()");
  }

  void slabDepthByPixels(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.slabDepthByPercentagePoints(pixels);
    refresh(0, "Viewer:slabDepthByPixels()");
  }

  void slabToPercent(int percentSlab) {
    //Eval.slab
    transformManager.slabToPercent(percentSlab);
    refresh(0, "Viewer:slabToPercent()");
  }

  void slabInternal(Point4f plane, boolean isDepth) {
    transformManager.slabInternal(plane, isDepth);
  }

  void depthToPercent(int percentDepth) {
    //Eval.depth
    transformManager.depthToPercent(percentDepth);
    refresh(0, "Viewer:depthToPercent()");
  }

  private void setSlabEnabled(boolean slabEnabled) {
    //Eval.slab
    transformManager.setSlabEnabled(slabEnabled);
    refresh(0, "Viewer:setSlabEnabled()");
  }

  void setSlabDepthInternal(boolean isDepth) {
    transformManager.setSlabDepthInternal(isDepth);
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void finalizeTransformParameters() {
    //FrameRenderer
    //InitializeModel
    
    transformManager.finalizeTransformParameters();
    g3d.setSlabAndDepthValues(transformManager.slabValue,
        transformManager.depthValue, global.zShade);
  }

  public Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
  }

  public void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPointNoClip(Point3f pointAngstroms, Point3i point3i) {
    transformManager.transformPointNoClip(pointAngstroms, point3i);
  }

  public void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPoints(Point3f[] pointsAngstroms, Point3i[] pointsScreens) {
    transformManager.transformPoints(pointsAngstroms.length, pointsAngstroms,
        pointsScreens);
  }

  public void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  public void unTransformPoint(Point3f pointScreen, Point3f pointAngstroms) {
    //called by Draw.move2D
    transformManager.unTransformPoint(pointScreen, pointAngstroms);
  }

  public float getScalePixelsPerAngstrom() {
    return transformManager.scalePixelsPerAngstrom;
  }

  public short scaleToScreen(int z, int milliAngstroms) {
    //all shapes    
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  public float scaleToPerspective(int z, float sizeAngstroms) {
    //DotsRenderer
    return transformManager.scaleToPerspective(z, sizeAngstroms);
  }

  private void setScaleAngstromsPerInch(float angstromsPerInch) {
    //Eval.setScale3d
    transformManager.setScaleAngstromsPerInch(angstromsPerInch);
  }

  void setSpin(String key, int value) {
    //Eval
    if (!Parser.isOneOf(key, "x;y;z;fps"))
      return;
    switch ("x;y;z;fps".indexOf(key)) {
    case 0:
      transformManager.setSpinX(value);
      break;
    case 2:
      transformManager.setSpinY(value);
      break;
    case 4:
      transformManager.setSpinZ(value);
      break;
    case 6:
    default:
      transformManager.setSpinFps(value);
      break;
    }
    global.setParameterValue("spin" + key, value);
  }

  String getSpinState() {
    return transformManager.getSpinState(false);
  }

  float getSpinX() {
    return transformManager.spinX;
  }

  float getSpinY() {
    return transformManager.spinY;
  }

  float getSpinZ() {
    return transformManager.spinZ;
  }

  float getSpinFps() {
    return transformManager.spinFps;
  }

  void setSpinOn(boolean spinOn) {
    //Eval
    //startSpinningAxis
    transformManager.setSpinOn(spinOn);
  }

  boolean getSpinOn() {
    return transformManager.getSpinOn();
  }

  String getOrientationText(boolean isAll) {
    return transformManager.getOrientationText(isAll);
  }

  Hashtable getOrientationInfo() {
    return transformManager.getOrientationInfo();
  }

  Matrix3f getMatrixRotate() {
    return transformManager.getMatrixRotate();
  }

  void getAxisAngle(AxisAngle4f axisAngle) {
    transformManager.getAxisAngle(axisAngle);
  }

  String getTransformText() {
    return transformManager.getTransformText();
  }

  void getRotation(Matrix3f matrixRotation) {
    transformManager.getRotation(matrixRotation);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  private void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
    global.setParameterValue("colorRasmol", (colorScheme.equals("rasmol")));
  }

  private void setDefaultTranslucent(float value) {
    global.defaultTranslucent = value;
  }

  float getDefaultTranslucent() {
    return global.defaultTranslucent;
  }

  public int getColixArgb(short colix) {
    return g3d.getColixArgb(colix);
  }

  void setRubberbandArgb(int argb) {
    //Eval
    colorManager.setRubberbandArgb(argb);
  }

  public short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  void setElementArgb(int elementNumber, int argb) {
    //Eval
    global.setParameterValue("=color "
        + JmolConstants.elementNameFromNumber(elementNumber), Escape
        .escapeColor(argb));
    colorManager.setElementArgb(elementNumber, argb);
  }

  public float getVectorScale() {
    return global.vectorScale;
  }

  public void setVectorScale(float scale) {
    global.setParameterValue("vectorScale", scale);
    global.vectorScale = scale;
    refresh(0, "set vectorScale");
  }

  public float getDefaultDrawArrowScale() {
    return global.defaultDrawArrowScale;
  }

  public void setDefaultDrawArrowScale(float scale) {
    global.setParameterValue("defaultDrawArrowScale", scale);
    global.defaultDrawArrowScale = scale;
    refresh(0, "set defaultDrawArrowScale");
  }

  float getVibrationScale() {
    return global.vibrationScale;
  }

  float getVibrationPeriod() {
    return global.vibrationPeriod;
  }

  public void setVibrationScale(float scale) {
    //Eval

    transformManager.setVibrationScale(scale);
    global.vibrationScale = scale;
    //because this is public:
    global.setParameterValue("vibrationScale", scale);
  }

  void setVibrationOff() {
    transformManager.setVibrationPeriod(0);
  }

  public void setVibrationPeriod(float period) {
    //Eval
    transformManager.setVibrationPeriod(period);
    period = Math.abs(period);
    global.vibrationPeriod = period;
    //because this is public:
    global.setParameterValue("vibrationPeriod", period);
  }

  void setObjectColor(String name, String colorName) {
    if (colorName == null || colorName.length() == 0)
      return;
    setObjectArgb(name, Graphics3D.getArgbFromString(colorName));
  }

  void setObjectArgb(String name, int argb) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId < 0)
      return;
    global.objColors[objId] = argb;
    switch (objId) {
    case StateManager.OBJ_BACKGROUND:
      g3d.setBackgroundArgb(argb);
      colorManager.setColixBackgroundContrast(argb);
      break;
    }
    global.setParameterValue(name + "Color", Escape.escapeColor(argb));
  }

  int getObjectArgb(int objId) {
    return global.objColors[objId];
  }

  public short getObjectColix(int objId) {
    int argb = getObjectArgb(objId);
    if (argb == 0)
      return getColixBackgroundContrast();
    return Graphics3D.getColix(argb);
  }

  public String getObjectState(String name) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return "";
    int mad = getObjectMad(objId);
    StringBuffer s = new StringBuffer("\n");
    Shape.appendCmd(s, name
        + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted"
            : mad < 20 ? " " + mad : " " + (mad / 2000f)));
    return s.toString();
  }

  //for historical reasons, leave these two:

  public void setColorBackground(String colorName) {
    setObjectColor("background", colorName);
  }

  public int getBackgroundArgb() {
    return getObjectArgb(StateManager.OBJ_BACKGROUND);
  }

  void setObjectMad(int iShape, String name, int mad) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return;
    if (mad == -2 || mad == -4) { //turn on if not set "showAxes = true"
      int m = mad + 3;
      mad = getObjectMad(objId);
      if (mad == 0)
        mad = m;
    }
    global.setParameterValue("show" + name, mad != 0);
    global.objStateOn[objId] = (mad != 0);
    if (mad == 0)
      return;
    global.objMad[objId] = mad;
    setShapeSize(iShape, mad); //just loads it
  }

  public int getObjectMad(int objId) {
    return (global.objStateOn[objId] ? global.objMad[objId] : 0);
  }

  public void setPropertyColorScheme(String scheme, boolean isOverloaded) {
    global.propertyColorScheme = scheme;
    colorManager.setColorScheme(scheme, isOverloaded);
  }

  public String getPropertyColorScheme() {
    return global.propertyColorScheme;
  }

  public short getColixBackgroundContrast() {
    return colorManager.colixBackgroundContrast;
  }

  String getSpecularState() {
    return global.getSpecularState();
  }

  private static void setSpecular(boolean specular) {
    //Eval
    ColorManager.setSpecular(specular);
    //global.specular = specular;
  }

  boolean getSpecular() {
    return ColorManager.getSpecular();
  }

  private static void setSpecularPower(int specularPower) {
    //Eval
    ColorManager.setSpecularPower(Math.abs(specularPower));
    //global.specularPower = specularPower;
  }

  private static void setSpecularExponent(int specularExponent) {
    //Eval
    ColorManager.setSpecularPower(-Math.abs(specularExponent));
    //global.specularExponent = specularExponent;
  }

  private static void setAmbientPercent(int ambientPercent) {
    //Eval
    ColorManager.setAmbientPercent(ambientPercent);
    //global.ambientPercent = ambientPercent;
  }

  static int getAmbientPercent() {
    return ColorManager.getAmbientPercent();
  }

  private static void setDiffusePercent(int diffusePercent) {
    //Eval
    ColorManager.setDiffusePercent(diffusePercent);
    //global.diffusePercent = diffusePercent;
  }

  static int getDiffusePercent() {
    return ColorManager.getDiffusePercent();
  }

  private static void setSpecularPercent(int specularPercent) {
    //Eval
    ColorManager.setSpecularPercent(specularPercent);
    //global.specularPercent = specularPercent;
  }

  static int getSpecularPercent() {
    return ColorManager.getSpecularPercent();
  }

  private void setZShade(boolean TF) {
    global.zShade = TF;
  }

  boolean getZShade() {
    return global.zShade;
  }

  public short getColixAtomPalette(Atom atom, byte pid) {
    return colorManager.getColixAtomPalette(atom, pid);
  }

  public int[] getColorSchemeArray(String colorScheme) {
    return colorManager.getColorSchemeArray(colorScheme);
  }
  
  public String getColorSchemeList(String colorScheme, boolean ifDefault) {
    return colorManager.getColorSchemeList(colorScheme, ifDefault);
  }

  public static void setUserScale(int[] scale) {
    ColorManager.setUserScale(scale);
  }

  public short getColixForPropertyValue(float val) {
    //isosurface
    return colorManager.getColixForPropertyValue(val);
  }

  Point3f getColorPointForPropertyValue(float val) {
    //x = {atomno=3}.partialcharge.color
    short colix = colorManager.getColixForPropertyValue(val);
    Point3f pt = new Point3f();
    return Graphics3D.colorPointFromInt(g3d.getColixArgb(colix), pt);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  void select(BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.select(bs, isQuiet);
    modelSet.setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MAX_VALUE,
        null);
  }

  void selectBonds(BitSet bs) {
    modelSet.setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MAX_VALUE, bs);
  }

  void hide(BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.hide(bs, isQuiet);
  }

  void display(BitSet bsAll, BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.display(bsAll, bs, isQuiet);
  }

  BitSet getHiddenSet() {
    return selectionManager.getHiddenSet();
  }

  public boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean isInSelectionSubset(int atomIndex) {
    return selectionManager.isInSelectionSubset(atomIndex);
  }

  void reportSelection(String msg) {
    if (modelSet.getSelectionHaloEnabled())
      setTainted(true);
    scriptStatus(msg);
  }

  public Point3f getAtomSetCenter(BitSet bs) {
    return modelSet.getAtomSetCenter(bs);
  }

  public void selectAll() {
    //initializeModel
    selectionManager.selectAll(false);
    refresh(0, "Viewer:selectAll()");
  }
  
  private boolean noneSelected;
  void setNoneSelected(boolean noneSelected) {
    this.noneSelected = noneSelected;
  }

  Boolean getNoneSelected() {
    return (noneSelected ? Boolean.TRUE : Boolean.FALSE);
  }
  
  public void clearSelection() {
    //not used in this project; in jmolViewer interface, though
    selectionManager.clearSelection(false);
    global.setParameterValue("hideNotSelected", false);
    refresh(0, "Viewer:clearSelection()");
  }

  public void setSelectionSet(BitSet set) {
    //not used in this project; in jmolViewer interface, though
    selectionManager.setSelectionSet(set);
    refresh(0, "Viewer:setSelectionSet()");
  }

  void setSelectionSubset(BitSet subset) {
    selectionManager.setSelectionSubset(subset);
  }

  public BitSet getSelectionSubset() {
    return selectionManager.bsSubset;
  }

  private void setHideNotSelected(boolean TF) {
    selectionManager.setHideNotSelected(TF);
  }

  void invertSelection() {
    //Eval
    selectionManager.invertSelection();
    // only used from a script, so I do not think a refresh() is necessary
  }

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  public int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  void setFormalCharges(int formalCharge) {
    modelSet.setFormalCharges(selectionManager.bsSelection, formalCharge);
  }

  public void addSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  public void removeSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  BitSet getAtomBitSet(Object atomExpression) {
    // typically a string such as "(atomno < 3)"
    return Eval.getAtomBitSet(eval, this, atomExpression);
  }
  
  Vector getAtomBitSetVector(Object atomExpression) {
    return Eval.getAtomBitSetVector(eval, this, atomExpression);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to MouseManager
  // ///////////////////////////////////////////////////////////////

  public void setModeMouse(int modeMouse) {
    //call before setting viewer=null
    mouseManager.setModeMouse(modeMouse);
    if (modeMouse == JmolConstants.MOUSE_NONE) {
      //applet is being destroyed
      clearScriptQueue();
      haltScriptExecution();
      scriptManager.startCommandWatcher(false);
      g3d.destroy();
    }
  }

  Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  public int getCursorX() {
    return mouseManager.xCurrent;
  }

  public int getCursorY() {
    return mouseManager.yCurrent;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to FileManager
  // ///////////////////////////////////////////////////////////////

  private void setAppletProxy(String appletProxy) {
    //Eval
    fileManager.setAppletProxy(appletProxy);
  }

  private void setDefaultDirectory(String dir) {
    global.defaultDirectory = (dir == null ? "" : dir.replace('\\', '/'));
  }

  String getDefaultDirectory() {
    return global.defaultDirectory;
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary) {
    return fileManager.getBufferedReaderOrErrorMessageFromName(name,
        fullPathNameReturn, isBinary);
  }

  Object getBufferedReaderForString(String string) {
    return fileManager.getBufferedReaderForString(string);
  }

  void addLoadScript(String script) {
    fileManager.addLoadScript(script);  
  }

  public void openFile(String name) {
    //Jmol app file dropper, main, OpenUrlAction, RecentFilesAction
    name = name.replace('\\', '/');
    String type = fileManager.getFileTypeName(name);
    checkHalt("exit");
    // assumes a Jmol script file if no other file type
    evalString((type == null ? "script " : "load ") + Escape.escape(name));
  }

  void openFile(String name, Hashtable htParams, String loadScript,
                boolean isAppend) {
    //Eval
    if (name == null)
      return;
    if (name.equalsIgnoreCase("string")) {
      openStringInline(fileManager.getInlineData(-1), htParams, isAppend);
      return;
    }
    if (name.equalsIgnoreCase("string[]")) {
      // no reloading of string[] data -- just too complicated
      // openStringsInline(fileManager.getInlineDataArray(), htParams, false);
      return;
    }
    if (!isAppend)
      zap(false, false);
    Logger.startTimer();
    fileManager.openFile(name, htParams, loadScript, isAppend);
    Logger.checkTimer("openFile(" + name + ")");
    setStatusFileLoaded(1, name, "", getModelSetName(), null);
  }

  public void openFiles(String modelName, String[] names) {
    openFiles(modelName, names, null, false);
  }

  void openFiles(String modelName, String[] names, String loadScript,
                 boolean isAppend) {
    //Eval -- names will be loaded with full path names
    if (!isAppend)
      zap(false, false);
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names, loadScript, isAppend);
    long ms = System.currentTimeMillis() - timeBegin;
    for (int i = 0; i < names.length; i++) {
      setStatusFileLoaded(1, names[i], "", getModelSetName(), null);
    }
    Logger.info("openFiles(" + names.length + ") " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    //Jmol app file dropper
    openStringInline(strModel, null, false);
  }

  private boolean openStringInline(String strModel, Hashtable htParams,
                                   boolean isAppend) {
    //loadInline, openFile, openStringInline
    if (!isAppend)
      clear();
    fileManager.openStringInline(strModel, htParams, isAppend);
    String errorMsg = getOpenFileError(isAppend);
    if (errorMsg == null)
      setStatusFileLoaded(1, "string", "", getModelSetName(), null);
    return (errorMsg == null);
  }

  private void openStringsInline(String[] arrayModels, Hashtable htParams,
                                 boolean isAppend) {
    //loadInline, openFile, openStringInline
    if (!isAppend)
      clear();
    fileManager.openStringsInline(arrayModels, htParams, isAppend);
    String errorMsg = getOpenFileError(isAppend);
    if (errorMsg == null)
      setStatusFileLoaded(1, "string[]", "", getModelSetName(), null);
  }

  public char getInlineChar() {
    return global.inlineNewlineChar;
  }

  public void loadInline(String strModel) {
    //loadInline PARAMETER for APPLET ONLY
    loadInline(strModel, global.inlineNewlineChar);
  }

  public void loadInline(String strModel, boolean isAppend) {
    //applet Console, loadInline functions, app PasteClipboard
    loadInline(strModel, (char) 0, isAppend);
  }

  public void loadInline(String strModel, char newLine) {
    loadInline(strModel, newLine, false);
  }

  boolean loadInline(String strModel, char newLine, boolean isAppend) {
    if (strModel == null)
      return false;
    int i;
    int[] params = global.getDefaultLatticeArray();
    Hashtable A = new Hashtable();
    A.put("params", params);
    if (global.applySymmetryToBonds)
      A.put("applySymmetryToBonds", Boolean.TRUE);

    Logger.debug(strModel);
    if (newLine != 0 && newLine != '\n') {
      int len = strModel.length();
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      strModel = TextFormat.simpleReplace(strModel, "" + newLine, "\n");
    }
    String datasep = getDataSeparator();
    if (datasep != null && datasep != ""
        && (i = strModel.indexOf(datasep)) >= 0) {
      int n = 2;
      while ((i = strModel.indexOf(datasep, i + 1)) >= 0)
        n++;
      String[] strModels = new String[n];
      int pt = 0, pt0 = 0;
      for (i = 0; i < n; i++) {
        pt = strModel.indexOf(datasep, pt0);
        if (pt < 0)
          pt = strModel.length();
        strModels[i] = strModel.substring(pt0, pt);
        pt0 = pt + datasep.length();
      }
      openStringsInline(strModels, A, isAppend);
      return true;
    }
    return openStringInline(strModel, A, isAppend);
  }

  String getDataSeparator() {
    return (String) global.getParameter("dataseparator");  
  }
  
  public void loadInline(String[] arrayModels) {
    loadInline(arrayModels, false);
  }

  public void loadInline(String[] arrayModels, boolean isAppend) {
    //Eval data
    //loadInline
    if (arrayModels == null || arrayModels.length == 0)
      return;
    int[] params = global.getDefaultLatticeArray();
    Hashtable A = new Hashtable();
    A.put("params", params);
    if (global.applySymmetryToBonds)
      A.put("applySymmetryToBonds", Boolean.TRUE);
    openStringsInline(arrayModels, A, isAppend);
  }

  public boolean getApplySymmetryToBonds() {
    return global.applySymmetryToBonds;
  }

  void setApplySymmetryToBonds(boolean TF) {
    global.applySymmetryToBonds = TF;
  }

  void loadData(int type, String name, String coordinateData) {
    modelSet.loadData(type, name, coordinateData);
  }

  public void openDOM(Object DOMNode) {
    //applet.loadDOMNode
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    Logger.info("openDOM " + ms + " ms");
    setStatusFileLoaded(1, "JSNode", "", getModelSetName(), 
        getOpenFileError());
  }

  /**
   * Opens the file, given the reader.
   * 
   * name is a text name of the file ... to be displayed in the window no need
   * to pass a BufferedReader ... ... the FileManager will wrap a buffer around
   * it
   * 
   * DO NOT USE IN JMOL -- THIS METHOD IS ONLY HERE BECAUSE IT IS
   * PART OF THE LEGACY INTERFACE
   * IF USED BY ANOTHER APPLICATION, YOU ARE RESPONSIBLE FOR CLOSING THE READER
   * 
   * @param fullPathName
   * @param name
   * @param reader
   */
  public void openReader(String fullPathName, String name, Reader reader) {
    clear();
    fileManager.openReader(fullPathName, name, reader);
    getOpenFileError();
    System.gc();
  }

  /**
   * misnamed -- really this opens the file, gets the data, and returns error or null
   * 
   * @return errorMsg
   */
  public String getOpenFileError() {
    return getOpenFileError(false);
  }

  /**
   * the opener for Jmol
   * 
   * @param isAppend
   * @return errorMsg
   */
  String getOpenFileError(boolean isAppend) {
    String fullPathName = getFullPathName();
    String fileName = getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      setStatusFileNotLoaded(fullPathName, errorMsg);
      if (errorMsg != null && !isAppend)
        zap(errorMsg);
      return errorMsg;
    }
    if (isAppend) {
      modelSet = modelManager.merge(modelAdapter, clientFile);
      if (eval != null)
        eval.clearDefinitionsAndLoadPredefined();
      selectAll();
      setTainted(true);
    } else {
      openClientFile(fullPathName, fileName, clientFile);
    }
    clientFile = null;
    System.gc();
    return null;
  }

  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    setStatusFileLoaded(2, fullPathName, fileName, 
      null, null);
    pushHoldRepaint();
    modelSet = modelManager
        .setClientFile(fullPathName, fileName, modelAdapter, clientFile);
    initializeModel();
    popHoldRepaint();
    setStatusFileLoaded(3, fullPathName, fileName, 
        getModelSetName(), null);
  }


  public Object getCurrentFileAsBytes() {
    String filename = getFullPathName(); 
    if (filename == "string" || filename == "string[]" || filename == "JSNode")
      return getCurrentFileAsString();
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return "";
    return fileManager.getFileAsBytes(pathName);
  }

  public String getCurrentFileAsString() {
    String filename = getFullPathName(); 
    if (filename == "string") {
      return fileManager.getInlineData(-1);
    }
    if (filename == "string[]") {
      int modelIndex = getDisplayModelIndex();
      if (modelIndex < 0)
        return "";
      return fileManager.getInlineData(modelIndex);
    }
    if (filename == "JSNode") {
      return "<DOM NODE>";
    }
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return null;
    return fileManager.getFileAsString(pathName);
  }

  public String getFileAsString(String pathName) {
    return (pathName == null ? getCurrentFileAsString() : fileManager
        .getFileAsString(pathName));
  }

  public String getFullPathName() {
    return fileManager.getFullPathName();
  }

  public String getFileName() {
    return fileManager.getFileName();
  }

  String[] getFileInfo() {
    return fileManager.getFileInfo();
  }

  void setFileInfo(String[] fileInfo) {
    fileManager.setFileInfo(fileInfo);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

  public Point3f[] calculateSurface(BitSet bsSelected, float envelopeRadius) {
    if (bsSelected == null) bsSelected = selectionManager.bsSelection;
    addStateScript("calculate surfaceDistance "
        + (envelopeRadius == Float.MAX_VALUE ? "FROM" : "WITHIN"), null,
        bsSelected, null, "", false);
    return modelSet.calculateSurface(bsSelected, envelopeRadius);
  }

  public AtomIndexIterator getWithinModelIterator(Atom atom, float distance) {
    return modelSet.getWithinModelIterator(atom, distance);
  }

  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased) {
    return modelSet.getWithinAtomSetIterator(atomIndex, distance,
        bsSelected, isGreaterOnly, modelZeroBased);
  }

  public void fillAtomData(AtomData atomData, int mode) {
    atomData.programInfo = "Jmol Version " + getJmolVersion(); 
    atomData.fileName = getFileName();
    modelSet.fillAtomData(atomData, mode);
  }

  void addStateScript(String script, boolean addFrameNumber) {
    addStateScript(script, null, null, null, null, addFrameNumber);
  }

  void addStateScript(String script1, BitSet bsBonds, BitSet bsAtoms1,
                      BitSet bsAtoms2, String script2, boolean addFrameNumber) {
    modelSet.addStateScript(script1, bsBonds, bsAtoms1, bsAtoms2, script2,
        addFrameNumber);
  }

  public boolean getEchoStateActive() {
    return modelSet.getEchoStateActive();
  }

  void setEchoStateActive(boolean TF) {
    modelSet.setEchoStateActive(TF);
  }

  public void zap(boolean notify, boolean resetUndo) {
    //Eval
    //setAppletContext
    clear();
    modelSet = modelManager.zap();
    initializeModel();
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long bTotal = runtime.totalMemory();
    long bFree = runtime.freeMemory();
    long bMax = 0;
    try {
      bMax = runtime.maxMemory();
    } catch (Exception e) {
    }
    Logger.debug("ZAP memory inuse, total, free, max: " + (bTotal - bFree)
        + " " + bTotal + " " + bFree + " " + bMax);
    if (notify)
      setStatusFileLoaded(0, null, (resetUndo ? "resetUndo" : null), null, null);
  }

  private void zap(String msg) {
    zap(true, false);
    echoMessage(msg);
  }

  void echoMessage(String msg) {
    int iShape = JmolConstants.SHAPE_ECHO;
    loadShape(iShape);
    setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 9));
    setShapeProperty(iShape, "target", "error");
    setShapeProperty(iShape, "text", msg);
  }

  private void clear() {
    if (modelSet == null)
      return;
    clearMinimization();
    fileManager.clear();
    repaintManager.clear();
    transformManager.clear();
    pickingManager.clear();
    selectionManager.clear();
    clearAllMeasurements();
    modelSet = modelManager.clear();
    mouseManager.clear();
    statusManager.clear();
    stateManager.clear();
    global.clear();
    tempManager.clear();
    colorManager.clear();
    evalVariables.clear();
    //setRefreshing(true);
    refresh(0, "Viewer:clear()");
    dataManager.clear();
    System.gc();
  }

  private void clearMinimization() {
    if (minimizer == null)
      return;
    minimizer.setProperty("clear", null);
    minimizer = null;    
  }
  
  public void notifyMinimizationStatus() {
    String s = statusManager.getCallbackScript("minimizationcallback");
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.notifyMinimizationStatus();
  }
  
  public String getMinimizationInfo() {
    return (minimizer == null ? "" 
        : (String) minimizer.getProperty("log", 0));
  }
  
  public boolean useMinimizationThread() {
    return !autoExit;
  }
  
  private void initializeModel() {
    reset();
    selectAll();
    noneSelected = false;
    transformManager.setCenter();
    if (eval != null)
      eval.clearDefinitionsAndLoadPredefined();
    // there probably needs to be a better startup mechanism for shapes

    repaintManager.initializePointers(1);
    setCurrentModelIndex(0);
    setBackgroundModelIndex(-1);
    setFrankOn(getShowFrank());
    mouseManager.startHoverWatcher(true);
    setTainted(true);
    finalizeTransformParameters();
  }

  public String getModelSetName() {
    if (modelSet == null)
      return null;
    return modelSet.getModelSetName();
  }

  public String getModelSetFileName() {
    return modelManager.getModelSetFileName();
  }

  public String getUnitCellInfoText() {
    return modelSet.getUnitCellInfoText();
  }

  public String getSpaceGroupInfoText(String spaceGroup) {
    return modelSet.getSpaceGroupInfoText(spaceGroup);
  }

  void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    modelSet.getPolymerPointsAndVectors(bs, vList);
  }

  public String getModelSetProperty(String strProp) {
    return modelSet.getModelSetProperty(strProp);
  }

  public Object getModelSetAuxiliaryInfo(String strKey) {
    return modelSet.getModelSetAuxiliaryInfo(strKey);
  }

  public String getModelSetPathName() {
    return modelManager.getModelSetPathName();
  }

  public String getModelSetTypeName() {
    return modelSet.getModelSetTypeName();
  }

  public boolean haveFrame() {
    return haveModelSet();
  }

  boolean haveModelSet() {
    return modelSet != null;
  }

  int getSurfaceDistanceMax() {
    return modelSet.getSurfaceDistanceMax();
  }
  
  void calculateStructures(BitSet bsAtoms) {
    //Eval
    modelSet.calculateStructures(bsAtoms);
  }

  void clearBfactorRange() {
    //Eval
    modelSet.clearBfactorRange();
  }

  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                                        String lcaoType,
                                        boolean hybridizationCompatible) {
    return modelSet.getHybridizationAndAxes(atomIndex, z, x, lcaoType,
        hybridizationCompatible);
  }

  public BitSet getModelAtomBitSet(int modelIndex, boolean asCopy) {
    return modelSet.getModelAtomBitSet(modelIndex, asCopy);
  }

  public BitSet getModelBitSet(BitSet atomList) {
    return modelSet.getModelBitSet(atomList);
  }

  Object getClientFile() {
    // DEPRECATED - use getExportJmolAdapter()
    return null;
  }

  // this is a problem. SmarterJmolAdapter doesn't implement this;
  // it can only return null. 

  public String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
    if (modelAdapter == null || propertyName == null
        || propertyName.length() == 0)
      return null;
    return modelAdapter.getClientAtomStringProperty(clientAtom, propertyName);
  }

  /*****************************************************************************
   * This is the method that should be used to extract the model data from Jmol.
   * Note that the API provided by JmolAdapter is used to import data into Jmol
   * and to export data out of Jmol.
   * 
   * When exporting, a few of the methods in JmolAdapter do not make sense.
   * openBufferedReader(...) Others may be implemented in the future, but are
   * not currently all pdb specific things Just pass in null for the methods
   * that want a clientFile. The main methods to use are getFrameCount(null) ->
   * currently always returns 1 getAtomCount(null, 0) getAtomIterator(null, 0)
   * getBondIterator(null, 0)
   * 
   * The AtomIterator and BondIterator return Objects as unique IDs to identify
   * the atoms. atomIterator.getAtomUid() bondIterator.getAtomUid1() &
   * bondIterator.getAtomUid2() The ExportJmolAdapter will return the 0-based
   * atom index as a boxed Integer. That means that you can cast the results to
   * get a zero-based atom index int atomIndex =
   * ((Integer)atomIterator.getAtomUid()).intValue(); ... int bondedAtom1 =
   * ((Integer)bondIterator.getAtomUid1()).intValue(); int bondedAtom2 =
   * ((Integer)bondIterator.getAtomUid2()).intValue();
   * 
   * post questions to jmol-developers@lists.sf.net
   * 
   * @return A JmolAdapter
   ****************************************************************************/

  JmolAdapter getExportJmolAdapter() {
    /*  
     * 
     return new FrameExportJmolAdapter(this, modelSet);

     */
    return null;
  }

  public ModelSet getModelSet() {
    return modelSet;
  }

  public String getBoundBoxCommand(boolean withOptions) {
    return modelSet.getBoundBoxCommand(withOptions);
  }
  
  void setBoundBox(Point3f pt1, Point3f pt2, boolean byCorner) {
    modelSet.setBoundBox(pt1, pt2, byCorner);  
  }
  
  public Point3f getBoundBoxCenter() {
    return modelSet.getBoundBoxCenter();
  }

  Point3f getAverageAtomPoint() {
    return modelSet.getAverageAtomPoint();
  }

  void calcBoundBoxDimensions(BitSet bs) {
    modelSet.calcBoundBoxDimensions(bs);
    axesAreTainted = true;
    refresh(0, "set calcBoundBoxDimensions");
  }
  
  BoxInfo getBoxInfo(BitSet bs) {
    return modelSet.getBoxInfo(bs);
  }
  
  float calcRotationRadius(Point3f center) {
    return modelSet.calcRotationRadius(center);
  }

  public float calcRotationRadius(BitSet bs) {
    return modelSet.calcRotationRadius(bs);
  }

  public Vector3f getBoundBoxCornerVector() {
    return modelSet.getBoundBoxCornerVector();
  }

  Hashtable getBoundBoxInfo() {
    return modelSet.getBoundBoxInfo();
  }

  public BitSet getBoundBoxModels() {
    return modelSet.getBoundBoxModels();
  }

  public int getBoundBoxCenterX() {
    // used by axes renderer
    return dimScreen.width / 2;
  }

  public int getBoundBoxCenterY() {
    return dimScreen.height / 2;
  }

  public int getModelCount() {
    return modelSet.getModelCount();
  }

  String getModelInfoAsString() {
    return modelSet.getModelInfoAsString();
  }

  String getSymmetryInfoAsString() {
    return modelSet.getSymmetryInfoAsString();
  }

  public Properties getModelSetProperties() {
    return modelSet.getModelSetProperties();
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return modelSet.getModelSetAuxiliaryInfo();
  }

  public int getModelNumber(int modelIndex) {
    if (modelIndex < 0)
      return modelIndex;
    return modelSet.getModelNumber(modelIndex);
  }

  public int getModelFileNumber(int modelIndex) {
    if (modelIndex < 0)
      return 0;
    return modelSet.getModelFileNumber(modelIndex);
  }

  public String getModelNumberDotted(int modelIndex) {
    return modelIndex < 0 ? "0" : modelSet == null ? null 
        : modelSet.getModelNumberDotted(modelIndex);
  }

  public String getModelName(int modelIndex) {
    return modelSet == null ? null : modelSet.getModelName(modelIndex);
  }

  public Properties getModelProperties(int modelIndex) {
    return modelSet.getModelProperties(modelIndex);
  }

  public String getModelProperty(int modelIndex, String propertyName) {
    return modelSet.getModelProperty(modelIndex, propertyName);
  }

  public String getModelFileInfo() {
    return modelSet.getModelFileInfo(getVisibleFramesBitSet());
  }

  String getModelFileInfoAll() {
    return modelSet.getModelFileInfo(null);    
  }
  
  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return modelSet.getModelAuxiliaryInfo(modelIndex);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return modelSet.getModelAuxiliaryInfo(modelIndex, keyName);
  }

  int getModelNumberIndex(int modelNumber, boolean useModelNumber,
                          boolean doSetTrajectory) {
    return modelSet.getModelNumberIndex(modelNumber, useModelNumber,
        doSetTrajectory);
  }

  boolean modelSetHasVibrationVectors() {
    return modelSet.modelSetHasVibrationVectors();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return modelSetHasVibrationVectors()
        && modelSet.modelHasVibrationVectors(modelIndex);
  }

  public int getChainCount() {
    return modelSet.getChainCount(true);
  }

  public int getChainCountInModel(int modelIndex) {
    //revised to NOT include water chain (for menu)
    return modelSet.getChainCountInModel(modelIndex, false);
  }

  public int getChainCountInModel(int modelIndex, boolean countWater) {
    return modelSet.getChainCountInModel(modelIndex, countWater);
  }

  public int getGroupCount() {
    return modelSet.getGroupCount();
  }

  public int getGroupCountInModel(int modelIndex) {
    return modelSet.getGroupCountInModel(modelIndex);
  }

  public int getPolymerCount() {
    return modelSet.getBioPolymerCount();
  }

  public int getPolymerCountInModel(int modelIndex) {
    return modelSet.getBioPolymerCountInModel(modelIndex);
  }

  public int getAtomCount() {
    return modelSet.getAtomCount();
  }

  public int getAtomCountInModel(int modelIndex) {
    return modelSet.getAtomCountInModel(modelIndex);
  }

  /**
   * For use in setting a for() construct max value
   * @return used size of the bonds array;
   */
  public int getBondCount() {
    return modelSet.getBondCount();
  }

  /**
   * from JmolPopup.udateModelSetComputedMenu
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  public int getBondCountInModel(int modelIndex) {
    return modelSet.getBondCountInModel(modelIndex);
  }

  BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
    //eval
    return modelSet.getBondsForSelectedAtoms(bsAtoms);
  }

  boolean frankClicked(int x, int y) {
    return frankOn && modelSet.frankClicked(x, y);
  }

  int findNearestAtomIndex(int x, int y) {
    return (modelSet == null ? -1 : modelSet.findNearestAtomIndex(x, y));
  }

  BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelSet.findAtomsInRectangle(rectRubberBand);
  }

  void toCartesian(Point3f pt) {
    int modelIndex = repaintManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toCartesian(modelIndex, pt);
  }

  void toUnitCell(Point3f pt, Point3f offset) {
    int modelIndex = repaintManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toUnitCell(modelIndex, pt, offset);
  }

  void toFractional(Point3f pt) {
    int modelIndex = repaintManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toFractional(modelIndex, pt);
  }

  public void setCenterSelected() {
    //depricated
    setCenterBitSet(selectionManager.bsSelection, true);
  }
  
  public void setBondTolerance(float bondTolerance) {
    global.setParameterValue("bondTolerance", bondTolerance);
    global.bondTolerance = bondTolerance;
  }

  public float getBondTolerance() {
    return global.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    //PreferencesDialog
    global.setParameterValue("minBondDistance", minBondDistance);
    global.minBondDistance = minBondDistance;
  }

  public float getMinBondDistance() {
    return global.minBondDistance;
  }

  int[] getAtomIndices(BitSet bs) {
    return modelSet.getAtomIndices(bs);
  }

  BitSet getAtomBits(int tokType) {
    return modelSet.getAtomBits(tokType);
  }

  BitSet getAtomBits(int tokType, String specInfo) {
    return modelSet.getAtomBits(tokType, specInfo);
  }

  public BitSet getAtomBits(int tokType, int specInfo) {
    return modelSet.getAtomBits(tokType, specInfo);
  }

  BitSet getAtomBits(int tokType, int[] specInfo) {
    return modelSet.getAtomBits(tokType, specInfo);
  }

  BitSet getAtomsWithin(int tokType, BitSet bs) {
    return modelSet.getAtomsWithin(tokType, bs);
  }

  BitSet getAtomsWithin(float distance, Point3f coord) {
    return modelSet.getAtomsWithin(distance, coord);
  }

  BitSet getAtomsWithin(float distance, Point4f plane) {
    return modelSet.getAtomsWithin(distance, plane);
  }

  BitSet getAtomsWithin(int tokType, String specInfo, BitSet bs) {
    return modelSet.getAtomsWithin(tokType, specInfo, bs);
  }

  BitSet getAtomsWithin(float distance, BitSet bs, boolean isWithinModelSet) {
    return modelSet.getAtomsWithin(distance, bs, isWithinModelSet);
  }

  BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    return modelSet.getAtomsConnected(min, max, intType, bs);
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelSet.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet(int modelIndex) {
    return modelSet.getElementsPresentBitSet(modelIndex);
  }

  public Hashtable getHeteroList(int modelIndex) {
    return modelSet.getHeteroList(modelIndex);
  }

  BitSet getVisibleSet() {
    return modelSet.getVisibleSet();
  }

  BitSet getClickableSet() {
    return modelSet.getClickableSet();
  }

  void calcSelectedGroupsCount() {
    modelSet.calcSelectedGroupsCount(selectionManager.bsSelection);
  }

  void calcSelectedMonomersCount() {
    modelSet.calcSelectedMonomersCount(selectionManager.bsSelection);
  }

  void calcSelectedMoleculesCount() {
    modelSet.calcSelectedMoleculesCount(selectionManager.bsSelection);
  }

  String getFileHeader() {
    return modelSet.getFileHeader(repaintManager.currentModelIndex);
  }

  String getPDBHeader() {
    return modelSet.getPDBHeader(repaintManager.currentModelIndex);
  }

  public Hashtable getModelInfo() {
    return modelSet.getModelInfo();
  }

  public Hashtable getAuxiliaryInfo() {
    return modelSet.getAuxiliaryInfo();
  }

  public Hashtable getShapeInfo() {
    return modelSet.getShapeInfo();
  }

  int getShapeIdFromObjectName(String objectName) {
    return modelSet.getShapeIdFromObjectName(objectName);
  }

  Vector getAllAtomInfo(Object atomExpression) {
    return modelSet.getAllAtomInfo(getAtomBitSet(atomExpression));
  }

  Vector getAllBondInfo(Object atomExpression) {
    return modelSet.getAllBondInfo(getAtomBitSet(atomExpression));
  }

  Vector getMoleculeInfo(Object atomExpression) {
    return modelSet.getMoleculeInfo(getAtomBitSet(atomExpression));
  }

  public String getChimeInfo(int tok) {
    return modelSet.getChimeInfo(tok, selectionManager.bsSelection);
  }

  public Hashtable getAllChainInfo(Object atomExpression) {
    return modelSet.getAllChainInfo(getAtomBitSet(atomExpression));
  }

  public Hashtable getAllPolymerInfo(Object atomExpression) {
    return modelSet.getAllPolymerInfo(getAtomBitSet(atomExpression));
  }
  
  public String getStateInfo() {
    return getStateInfo(null);
  }

  final static String STATE_VERSION_STAMP = "# Jmol state version ";
  
  public String getStateInfo(String type) {
    boolean isAll = (type == null || type.equalsIgnoreCase("all"));
    StringBuffer s = new StringBuffer("");
    StringBuffer sfunc = (isAll ? new StringBuffer("function _setState();\n")
        : null);
    if (isAll)
      s.append(STATE_VERSION_STAMP + getJmolVersion() + ";\n");
    if (isApplet && isAll) {
      StateManager.appendCmd(s, "# fullName = " + Escape.escape(fullName));
      StateManager.appendCmd(s, "# documentBase = "
          + Escape.escape(appletDocumentBase));
      StateManager
          .appendCmd(s, "# codeBase = " + Escape.escape(appletCodeBase));
      s.append("\n");
    }
    //  window state
    if (isAll || type.equalsIgnoreCase("windowState"))
      s.append(global.getWindowState(sfunc));
    //  file state
    if (isAll || type.equalsIgnoreCase("fileState"))
      s.append(fileManager.getState(sfunc));
    //  numerical values
    if (isAll || type.equalsIgnoreCase("variableState"))
      s.append(global.getState(sfunc));
    if (isAll || type.equalsIgnoreCase("dataState"))
      dataManager.getDataState(s, sfunc, modelSet.atoms, 
          getAtomCount(), modelSet.getAtomicPropertyState(-1, null));

    //  definitions, connections, atoms, bonds, labels, echos, shapes
    if (isAll || type.equalsIgnoreCase("modelState"))
      s.append(modelSet.getState(sfunc, true));
    //  color scheme
    if (isAll || type.equalsIgnoreCase("colorState"))
      s.append(ColorManager.getState(sfunc));
    //  frame information
    if (isAll || type.equalsIgnoreCase("frameState"))
      s.append(repaintManager.getState(sfunc));
    //  orientation and slabbing
    if (isAll || type.equalsIgnoreCase("perspectiveState"))
      s.append(transformManager.getState(sfunc));
    //  display and selections
    if (isAll || type.equalsIgnoreCase("selectionState"))
      s.append(selectionManager.getState(sfunc));
    if (sfunc != null) {
      StateManager.appendCmd(sfunc, "set refreshing true");
      StateManager.appendCmd(sfunc, "set antialiasDisplay " + global.antialiasDisplay);
      StateManager.appendCmd(sfunc, "set antialiasTranslucent " + global.antialiasTranslucent);
      StateManager.appendCmd(sfunc, "set antialiasImages " + global.antialiasImages);
      if (getSpinOn())
        StateManager.appendCmd(sfunc, "spin on");
      sfunc.append("end function;\n\n_setState;\n");
    }
    if (isAll)
      s.append(sfunc);
    return s.toString();
  }

  public String getStructureState() {
    return modelSet.getState(null, false);
  }

  public String getCoordinateState(BitSet bsSelected) {
    return modelSet.getAtomicPropertyState(AtomCollection.TAINT_COORD, bsSelected);
  }
  
  void setCurrentColorRange(String label) {
    float[] data = getDataFloat(label);
    BitSet bs = (data == null ? null 
        : (BitSet) ((Object[]) dataManager.getData(label))[2]);
    setCurrentColorRange(data, bs);
  }

  void setCurrentColorRange(float[] data, BitSet bs) {
    colorManager.setCurrentColorRange(data, bs, global.propertyColorScheme);
  }

  public void setCurrentColorRange(float min, float max) {
    colorManager.setCurrentColorRange(min, max);
  }

  public float[] getCurrentColorRange() {
    return colorManager.getCurrentColorRange();
  }
  
  public void setData(String type, Object[] data, int atomCount, int matchField,
               int field) {
    dataManager.setData(type, data, atomCount, matchField, field);
  }
  
  public static Object testData; // for isosurface  
  public static Object testData2; // for isosurface 

  Object[] getData(String type) {
    return dataManager.getData(type);  
  }
  
  float[] getDataFloat(String label) {
    return dataManager.getDataFloat(label);
  }

  float[][] getDataFloat2D(String label) {
    return dataManager.getDataFloat2D(label);  
  }
  
  public float getDataFloat(String label, int atomIndex) {
    return dataManager.getDataFloat(label, atomIndex);
  }

  public String getAltLocListInModel(int modelIndex) {
    return modelSet.getAltLocListInModel(modelIndex);
  }

  public BitSet setConformation() {
    // user has selected some atoms, now this sets that as a conformation
    // with the effect of rewriting the cartoons to match

    return modelSet.setConformation(-1, selectionManager.bsSelection);
  }

  // AKA "configuration"
  public BitSet setConformation(int conformationIndex) {
    return modelSet.setConformation(repaintManager.currentModelIndex,
        conformationIndex);
  }

  int autoHbond(BitSet bsBonds) {
    //Eval
 //   modelSet.addStateScript("select", null, selectionManager.bsSelection, null,
   //     "; calculate hbonds;", false);
    return autoHbond(selectionManager.bsSelection,
        selectionManager.bsSelection, bsBonds);
  }

  int autoHbond(BitSet bsFrom, BitSet bsTo, BitSet bsBonds) {
    //Eval
    return modelSet.autoHbond(bsFrom, bsTo, bsBonds);
  }

  public boolean hasCalculatedHBonds(BitSet bsAtoms) {
    return modelSet.hasCalculatedHBonds(bsAtoms);
  }

  public boolean havePartialCharges() {
    return modelSet.getPartialCharges() != null;
  }

  public UnitCell getCurrentUnitCell() {
    return modelSet.getUnitCell(getDisplayModelIndex());
  }

  Point3f getCurrentUnitCellOffset() {
    return modelSet.getUnitCellOffset(getDisplayModelIndex());
  }

  void setCurrentUnitCellOffset(int offset) {
    int modelIndex = repaintManager.currentModelIndex;
    if (modelSet.setUnitCellOffset(modelIndex, offset))
      global.setParameterValue("=frame " + getModelNumberDotted(modelIndex)
          + "; set unitcell ", offset);
  }

  void setCurrentUnitCellOffset(Point3f pt) {
    int modelIndex = repaintManager.currentModelIndex;
    if (modelSet.setUnitCellOffset(modelIndex, pt))
      global.setParameterValue("=frame " + getModelNumberDotted(modelIndex)
          + "; set unitcell ", Escape.escape(pt));
  }

  /* ****************************************************************************
   * delegated to MeasurementManager
   ****************************************************************************/

  public String getDefaultMeasurementLabel(int nPoints) {
    switch (nPoints) {
    case 2:
      return global.defaultDistanceLabel;
    case 3:
      return global.defaultAngleLabel;
    default:
      return global.defaultTorsionLabel;
    }
  }

  private void setDefaultMeasurementLabel(int nPoints, String format) {
    switch (nPoints) {
    case 2:
      global.defaultDistanceLabel = format;
    case 3:
      global.defaultAngleLabel = format;
    case 4:
      global.defaultTorsionLabel = format;
    }
  }

  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JmolConstants.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  public String getMeasurementStringValue(int i) {
    String str = ""
        + getShapeProperty(JmolConstants.SHAPE_MEASURES, "stringValue", i);
    return str;
  }

  Vector getMeasurementInfo() {
    return (Vector) getShapeProperty(JmolConstants.SHAPE_MEASURES, "info");
  }

  public String getMeasurementInfoAsString() {
    return (String) getShapeProperty(JmolConstants.SHAPE_MEASURES, "infostring");
  }

  public int[] getMeasurementCountPlusIndices(int i) {
    int[] List = (int[]) getShapeProperty(JmolConstants.SHAPE_MEASURES,
        "countPlusIndices", i);
    return List;
  }

  void setPendingMeasurement(int[] atomCountPlusIndices, Point3f ptClicked) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending",
        new Object[] {atomCountPlusIndices, ptClicked});
  }

  void clearAllMeasurements() {
    //Eval only
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
    refresh(0, "Viewer:clearAllMeasurements()");
  }

  public void clearMeasurements() {
    //depricated but in the API -- use "script" directly
    //see clearAllMeasurements()
    evalString("measures delete");
  }

  private void setJustifyMeasurements(boolean TF) {
    global.justifyMeasurements = TF;
  }

  public boolean getJustifyMeasurements() {
    return global.justifyMeasurements;
  }

  void setMeasurementFormats(String strFormat) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax,
                         boolean isDelete, boolean isAllConnected,
                         boolean isShowHide, boolean isHidden, String strFormat) {
    //Eval.monitor()
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected", Boolean
        .valueOf(isAllConnected));
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormat", strFormat);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isDelete ? "deleteVector"
        : isShowHide ? (isHidden ? "hideVector" : "showVector")
            : "defineVector", monitorExpressions);
    setStatusMeasuring("scripted", 1, "?");
  }

  public void deleteMeasurement(int i) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  void deleteMeasurement(int[] atomCountPlusIndices) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
        atomCountPlusIndices);
  }

  public void showMeasurement(int[] atomCountPlusIndices, boolean isON) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isON ? "show" : "hide",
        atomCountPlusIndices);
    refresh(0, "Viewer:showMeasurements()");
  }

  void hideMeasurements(boolean isOFF) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "hideAll", Boolean
        .valueOf(isOFF));
    refresh(0, "hideMeasurements()");
  }

  void toggleMeasurement(int[] atomCountPlusIndices, String strFormat) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES,
        (strFormat == null ? "toggle" : "toggleOn"), atomCountPlusIndices);
    if (strFormat != null)
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  // ///////////////////////////////////////////////////////////////

  void repaint() {
    //from RepaintManager
    if (display == null)  
      return;
    display.repaint();
  }

  void setAnimationDirection(int direction) {// 1 or -1
    //Eval
    repaintManager.setAnimationDirection(direction);
  }

  void reverseAnimation() {
    //Eval
    repaintManager.reverseAnimation();
  }

  int getAnimationDirection() {
    return repaintManager.animationDirection;
  }

  Hashtable getAnimationInfo() {
    return repaintManager.getAnimationInfo();
  }

  public void setAnimationFps(int fps) {
    if (fps < 1)
      fps = 1;
    if (fps > 50)
      fps = 50;
    global.setParameterValue("animationFps", fps);
    //Eval
    //app AtomSetChooser
    repaintManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return repaintManager.animationFps;
  }

  void setAnimationReplayMode(int replay, float firstFrameDelay,
                              float lastFrameDelay) {
    //Eval

    // 0 means once
    // 1 means loop
    // 2 means palindrome
    repaintManager.setAnimationReplayMode(replay, firstFrameDelay,
        lastFrameDelay);
  }

  int getAnimationReplayMode() {
    return repaintManager.animationReplayMode;
  }

  void setAnimationOn(boolean animationOn) {
    //Eval
    boolean wasAnimating = repaintManager.animationOn;
    if (animationOn == wasAnimating)
      return;
    repaintManager.setAnimationOn(animationOn);
  }

  void resumeAnimation() {
    //Eval
    if (repaintManager.animationOn) {
      Logger.debug("animation is ON in resumeAnimation");
      return;
    }
    repaintManager.resumeAnimation();
    refresh(0, "Viewer:resumeAnimation()");
  }

  void pauseAnimation() {
    //Eval
    if (!repaintManager.animationOn || repaintManager.animationPaused) {
      return;
    }
    repaintManager.pauseAnimation();
    refresh(0, "Viewer:pauseAnimation()");
  }

  void setAnimationRange(int modelIndex1, int modelIndex2) {
    repaintManager.setAnimationRange(modelIndex1, modelIndex2);
  }

  public BitSet getVisibleFramesBitSet() {
    BitSet bs = BitSetUtil.copy(repaintManager.getVisibleFramesBitSet());
    modelSet.selectDisplayedTrajectories(bs);
    return bs;
  }

  boolean isAnimationOn() {
    return repaintManager.animationOn;
  }

  void setAnimationNext() {
    //Eval
    if (repaintManager.setAnimationNext())
      refresh(0, "Viewer:setAnimationNext()");
  }

  void setAnimationPrevious() {
    //Eval
    if (repaintManager.setAnimationPrevious())
      refresh(0, "Viewer:setAnimationPrevious()");
  }

  void setAnimationLast() {
    //Eval
    repaintManager.setAnimationLast();
    refresh(0, "Viewer:setAnimationLast()");
  }

  void rewindAnimation() {
    //Eval
    repaintManager.rewindAnimation();
    refresh(0, "Viewer:rewindAnimation()");
  }

  void setCurrentModelIndex(int modelIndex) {
    //Eval
    //initializeModel
    repaintManager.setCurrentModelIndex(modelIndex);
  }

  void setTrajectory(int modelIndex) {
    modelSet.setTrajectory(modelIndex);
  }
  
  public void setTrajectory(BitSet bsModels) {
    modelSet.setTrajectory(bsModels);
  }

  boolean isTrajectory(int modelIndex) {
    return modelSet.isTrajectory(modelIndex);
  }
  
  public BitSet getBitSetTrajectories() {
    return modelSet.getBitSetTrajectories();
  }

  String getTrajectoryInfo() {
    return modelSet.getTrajectoryInfo();
  }

  public void setCurrentModelIndex(int modelIndex, boolean clearBackground) {
    //Eval
    //initializeModel
    repaintManager.setCurrentModelIndex(modelIndex, clearBackground);
  }

  public int getCurrentModelIndex() {
    return repaintManager.currentModelIndex;
  }

  public int getDisplayModelIndex() {
    // modified to indicate if there is also a background model index
    int modelIndex = repaintManager.currentModelIndex;
    int backgroundIndex = getBackgroundModelIndex();
    return (backgroundIndex >= 0 ? -2 - modelIndex : modelIndex);
  }

  boolean haveFileSet() {
    return (getModelCount() > 1 && getModelNumber(0) > 1000000);
  }

  void setBackgroundModelIndex(int modelIndex) {
    //initializeModel
    repaintManager.setBackgroundModelIndex(modelIndex);
    global.setParameterValue("backgroundModel", modelSet.getModelNumberDotted(modelIndex));
  }

  public int getBackgroundModelIndex() {
    return repaintManager.backgroundModelIndex;
  }

  public FrameRenderer getFrameRenderer() {
    return repaintManager.frameRenderer;
  }

  void setFrameVariables(int firstModelIndex, int lastModelIndex) {
    global.setParameterValue("_firstFrame", getModelNumberDotted(firstModelIndex));
    global.setParameterValue("_lastFrame", getModelNumberDotted(lastModelIndex));
  }

  boolean wasInMotion = false;
  int motionEventNumber;

  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  void setInMotion(boolean inMotion) {
    //MouseManager, TransformManager
    // Logger.debug("viewer.setInMotion("+inMotion+")");
    if (wasInMotion ^ inMotion) {
      repaintManager.setInMotion(inMotion);
      //resizeImage(0, 0, false, false, true);
      if (inMotion) {
        ++motionEventNumber;
      } else {
        repaintManager.refresh();
      }
      wasInMotion = inMotion;
    }
  }

  public boolean getInMotion() {
    //mps
    return repaintManager.inMotion;
  }

  public void pushHoldRepaint() {
    repaintManager.pushHoldRepaint();
  }

  public void popHoldRepaint() {
    repaintManager.popHoldRepaint();
  }

  private boolean refreshing = true;

  void setRefreshing(boolean TF) {
    //also set by Eval error to TRUE
    refreshing = TF;
  }

  boolean getRefreshing() {
    return refreshing;
  }

  /**
   * initiate a repaint/update sequence if it has not already been requested.
   * invoked whenever any operation causes changes that require new rendering. 
   * 
   * The repaint/update sequence will only be invoked if (a) no repaint is already
   * pending and (b) there is no hold flag set in repaintManager.
   * 
   * Sequence is as follows:
   * 
   * 1) RepaintManager.refresh() checks flags and then calls Viewer.repaint()
   * 2) Viewer.repaint() invokes display.repaint(), provided display is not null (headless)
   * 3) The system responds with an invocation of Jmol.update(Graphics g), which we are
   *    routing through Jmol.paint(Graphics g). 
   * 4) Jmol.update invokes Viewer.setScreenDimension(size), which makes the
   *    necessary changes in parameters for any new window size.
   * 5) Jmol.update invokes Viewer.renderScreenImage(g, size, rectClip)
   * 6) Viewer.renderScreenImage checks object visibility, invokes render1 to
   *    do the actual creation of the image pixel map and send it to the screen, 
   *    and then invokes repaintView() 
   * 7) Viewer.repaintView() invokes RepaintManager.repaintDone(), to clear the flags and
   *    then use notify() to release any threads holding on wait(). 
   * 
   * @param isOrientationChange
   * @param strWhy
   * 
   */
  public void refresh(int isOrientationChange, String strWhy) {
    if (isOrientationChange >= 0)
      repaintManager.refresh();
    else if (isOrientationChange < 0)
      isOrientationChange = -isOrientationChange;
    if (isOrientationChange != 0)
      statusManager.setStatusViewerRefreshed(isOrientationChange, strWhy, 
          syncingMouse, !syncingScripts);
  }

  void requestRepaintAndWait() {
    if (!haveDisplay)
      return;
    repaintManager.requestRepaintAndWait();
    statusManager.setStatusViewerRefreshed(1, "move/navigate/refresh",
        syncingMouse, !syncingScripts);

  }

  public void repaintView() {
    repaintManager.repaintDone();
  }

  private boolean axesAreTainted = false;

  public boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  ////////////// screen/image methods ///////////////

  final Dimension dimScreen = new Dimension();

  //final Rectangle rectClip = new Rectangle();

  public void setScreenDimension(Dimension dim) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    int height = dim.height;
    int width = dim.width;
    if (getStereoMode() == JmolConstants.STEREO_DOUBLE)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    resizeImage(width, height, false, false, true);
  }

  private void resizeImage(int width, int height, 
                           boolean isImageWrite, 
                           boolean isGenerator,
                           boolean isReset) {
    if (width > 0) {
      if (isImageWrite && !isReset)
        setImageFontScaling(width, height);
      dimScreen.width = width;
      dimScreen.height = height;
    }
    
    antialiasDisplay = false;
    if (isReset) {
      imageFontScaling = 1;
      antialiasDisplay = global.antialiasDisplay;//&& !getInMotion();
    } else if (isImageWrite && !isGenerator) {
      antialiasDisplay = global.antialiasImages;
    }
    if (antialiasDisplay)
      imageFontScaling *= 2;
    if (width > 0 && !isImageWrite) {
      global.setParameterValue("_width", width);
      global.setParameterValue("_height", height);
      setStatusResized(width, height);
    }
    if (width <= 0) {
      width = dimScreen.width;
      height = dimScreen.height;
    }
    transformManager.setScreenParameters(width, height,
        isImageWrite || isReset? global.zoomLarge : false,
            antialiasDisplay, false, false);
    g3d.setWindowParameters(width, height, antialiasDisplay); 
  }

  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }
  
  public int getScreenDim() {
    return  (global.zoomLarge == (dimScreen.height > dimScreen.width) ? dimScreen.height : dimScreen.width);
  }

  /* not implemented
   private void setRectClip(Rectangle clip) {
   if (clip == null) {
   rectClip.x = rectClip.y = 0;
   rectClip.setSize(dimScreen);
   } else {
   rectClip.setBounds(clip);
   // on Linux platform with Sun 1.4.2_02 I am getting a clipping rectangle
   // that is wider than the current window during window resize
   if (rectClip.x < 0)
   rectClip.x = 0;
   if (rectClip.y < 0)
   rectClip.y = 0;
   if (rectClip.x + rectClip.width > dimScreen.width)
   rectClip.width = dimScreen.width - rectClip.x;
   if (rectClip.y + rectClip.height > dimScreen.height)
   rectClip.height = dimScreen.height - rectClip.y;
   }
   }
   */

  public String generateOutput(String type, String fileName, int width, int height) {
    saveState("_Export");
    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    resizeImage(width, height, true, true, false);
    setModelVisibility();
    String data = repaintManager.generateOutput(type, g3d, modelSet, fileName);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized.
    resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    //System.out.println("renderScreenImage");
    if (isTainted || getSlabEnabled())
      setModelVisibility();
    isTainted = false;
    if (size != null)
      setScreenDimension(size);
    int stereoMode = getStereoMode();
    switch (stereoMode) {
    case JmolConstants.STEREO_DOUBLE:
      render1(g, getImage(true), dimScreen.width, 0);
    case JmolConstants.STEREO_NONE:
      render1(g, getImage(false), 0, 0);
      break;
    case JmolConstants.STEREO_REDCYAN:
    case JmolConstants.STEREO_REDBLUE:
    case JmolConstants.STEREO_REDGREEN:
    case JmolConstants.STEREO_CUSTOM:
      render1(g, getStereoImage(stereoMode), 0, 0);
      break;
    }
    repaintView();
  }

  private Image getImage(boolean isDouble) {
    g3d.beginRendering(transformManager.getStereoRotationMatrix(isDouble));
    render();
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  private boolean antialiasDisplay;

  private void render() {
    boolean antialias2 = antialiasDisplay && global.antialiasTranslucent;
    repaintManager.render(g3d, modelSet);
    if (g3d.setPass2(antialias2)) {
      transformManager.setAntialias(antialias2);
      repaintManager.render(g3d, modelSet);
    }  
  }
  
  private Image getStereoImage(int stereoMode) {
    g3d.beginRendering(transformManager.getStereoRotationMatrix(true));
    render();
    g3d.endRendering();
    g3d.snapshotAnaglyphChannelBytes();
    g3d.beginRendering(transformManager.getStereoRotationMatrix(false));
    render();
    g3d.endRendering();
    switch (stereoMode) {
    case JmolConstants.STEREO_REDCYAN:
      g3d.applyCyanAnaglyph();
      break;
    case JmolConstants.STEREO_CUSTOM:
      g3d.applyCustomAnaglyph(transformManager.stereoColors);
      break;
    case JmolConstants.STEREO_REDBLUE:
      g3d.applyBlueAnaglyph();
      break;
    default:
      g3d.applyGreenAnaglyph();
    }
    return g3d.getScreenImage();
  }

  private void render1(Graphics g, Image img, int x, int y) {
    if (g != null) {
      try {
        g.drawImage(img, x, y, null);
      } catch (NullPointerException npe) {
        Logger.error("Sun!! ... fix graphics your bugs!");
      }
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    boolean isStereo = false;
    //setRectClip(null);
    int stereoMode = getStereoMode();
    switch (stereoMode) {
    case JmolConstants.STEREO_DOUBLE:
      // this allows for getting both eye views in two images
      // because you can adjust using "stereo -2.5", then "stereo +2.5"
      isStereo = true;
      break;
    case JmolConstants.STEREO_REDCYAN:
    case JmolConstants.STEREO_REDBLUE:
    case JmolConstants.STEREO_REDGREEN:
    case JmolConstants.STEREO_CUSTOM:
      return getStereoImage(stereoMode);
    }
    return getImage(isStereo);
  }

  /**
   * @param quality
   * @return base64-encoded version of the image
   */
  public String getJpegBase64(int quality) {
    Image eImage = getScreenImage();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    JpegEncoder jc = new JpegEncoder(eImage, quality, os);
    jc.Compress();
    byte[] jpeg = os.toByteArray();
    releaseScreenImage();
    return Base64.getBase64(jpeg).toString();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  private void setAllowEmbeddedScripts(boolean TF) {
    global.allowEmbeddedScripts = TF;
  }

  boolean getAllowEmbeddedScripts() {
    return global.allowEmbeddedScripts;
  }

  public String evalFile(String strFilename) {
    //app -s flag
    int ptWait = strFilename.indexOf(" -nowait");
    if (ptWait >= 0) {
      return (String) evalStringWaitStatus("String", strFilename.substring(0,
          ptWait), "", true, false);
    }
    return scriptManager.addScript(strFilename, true, false);
  }

  public String script(String strScript) {
    return evalString(strScript);
  }

  String interruptScript = "";

  String getInterruptScript() {
    String s = interruptScript;
    interruptScript = "";
    if (Logger.debugging && s != "")
      Logger.debug("interrupt: " + s);
    return s;
  }

  public String evalString(String strScript) {
    return evalStringQuiet(strScript, false, true);
  }

  public String evalStringQuiet(String strScript) {
    return evalStringQuiet(strScript, true, true);
  }
  
  private String evalStringQuiet(String strScript, boolean isQuiet, boolean allowSyncScript) {
    // central point for all incoming script processing
    // all menu items, all mouse movement -- everything goes through this method
    // by setting syncScriptTarget = ">" the user can direct that all scripts 
    // initiated WITHIN this applet (not sent to it) 
    // we append #NOSYNC; here so that the receiving applet does not attempt
    // to pass it back to us or any other applet.
    //System.out.println(getHtmlName() + " evalstringquiet " + strScript);
    if (allowSyncScript && syncingScripts && strScript.indexOf("#NOSYNC;") < 0  )
      syncScript(strScript + " #NOSYNC;", null);
    boolean isInterrupt = (strScript.length() > 0 && strScript.charAt(0) == '!');
    if (isInterrupt)
      strScript = strScript.substring(1);
    if (checkResume(strScript))
      return "script processing resumed";
    if (checkHalt(strScript))
      return "script execution halted";
    if (isScriptExecuting() && (isInterrupt || eval.isExecutionPaused())) {
      interruptScript = strScript;
      if (strScript.indexOf("moveto ") == 0)
        scriptManager.flushQueue("moveto ");
      return "!" + strScript;
    }
    interruptScript = "";
    return scriptManager.addScript(strScript, false, isQuiet && !getMessageStyleChime());
  }

  private void setScriptQueue(boolean value) {
    scriptManager.setQueue(value);
  }

  boolean usingScriptQueue() {
    return scriptManager.useQueue;
  }

  public void clearScriptQueue() {
    //Eval
    //checkHalt **
    scriptManager.clearQueue();
  }

  public boolean checkResume(String strScript) {
    if (strScript.equalsIgnoreCase("resume")) {
      resumeScriptExecution();
      return true;
    }
    return false;
  }

  public boolean checkHalt(String strScript) {
    String str = strScript.toLowerCase();
    if (str.equals("pause")) {
      pauseScriptExecution();
      return true;
    }
    if (str.startsWith("exit")) {
      haltScriptExecution();
      clearScriptQueue();
      if (checkScriptOnly)
        Logger.info("exit -- stops script checking");
      checkScriptOnly = false;
      return str.equals("exit");
    }
    if (str.startsWith("quit")) {
      haltScriptExecution();
      if (checkScriptOnly)
        Logger.info("quit -- stops script checking");
      checkScriptOnly = false;
      return str.equals("quit");
    }
    return false;
  }

  /// direct no-queue use:

  public String scriptWait(String strScript) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    String str = (String) evalStringWaitStatus("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated", false,
        false);
    GT.setDoTranslate(doTranslateTemp);
    return str;
  }

  public Object scriptWaitStatus(String strScript, String statusList) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    Object ret = evalStringWaitStatus("object", strScript, statusList, false,
        false);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
  }

  public Object evalStringWaitStatus(String returnType, String strScript,
                                     String statusList) {
    scriptManager.waitForQueue();
    return evalStringWaitStatus(returnType, strScript, statusList, false, false);
  }

  int scriptIndex;

  synchronized Object evalStringWaitStatus(String returnType, String strScript,
                                           String statusList,
                                           boolean isScriptFile, boolean isQuiet) {
    // from the scriptManager only!
    if (checkResume(strScript))
      return "script processing resumed"; //be very odd if this fired
    if (checkHalt(strScript))
      return "script execution halted";
    if (strScript == null)
      return null;

    //typically request: "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
    //set up first with applet.jmolGetProperty("jmolStatus",statusList)
    //flush list
    String oldStatusList = statusManager.statusList;
    getProperty("String", "jmolStatus", statusList);
    if (checkScriptOnly)
      Logger.info("--checking script:\n" + eval.getScript() + "\n----\n");
    boolean historyDisabled = (strScript.indexOf(")") == 0);
    if (historyDisabled)
      strScript = strScript.substring(1);
    boolean isOK = (isScriptFile ? eval.loadScriptFile(strScript, isQuiet)
        : eval.loadScriptString(strScript, isQuiet));
    String strErrorMessage = eval.getErrorMessage();
    if (isOK) {
      statusManager.setStatusScriptStarted(++scriptIndex, strScript);
      eval.runEval(checkScriptOnly, !checkScriptOnly || fileOpenCheck,
          historyDisabled, listCommands);
      int msWalltime = eval.getExecutionWalltime();
      strErrorMessage = eval.getErrorMessage();
      statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
/*      if (getMessageStyleChime())
        scriptStatus("script <exiting>");
*/
      if (isScriptFile && writeInfo != null)
        createImage(writeInfo);
    } else {
      scriptStatus(strErrorMessage);
    }
    if (checkScriptOnly) {
      if (strErrorMessage == null)
        Logger.info("--script check ok");
      else
        Logger.error("--script check error\n" + strErrorMessage);
    }
    if (isScriptFile && autoExit) {
      System.out.flush();
      System.exit(0);
    } else if (checkScriptOnly)
      Logger.info("(use 'exit' to stop checking)");
    if (returnType.equalsIgnoreCase("String"))
      return eval.getErrorMessage();
    // get  Vector of Vectors of Vectors info
    Object info = getProperty(returnType, "jmolStatus", statusList);
    // reset to previous status list
    getProperty("object", "jmolStatus", oldStatusList);
    return info;
  }

  boolean checking;

  void  setStatusScriptTermination(String strErrorMessage, int msWalltime) {
    statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
  }

  public String scriptCheck(String strScript) {
    // from ConsoleTextPane.checkCommand() and applet Jmol.scriptProcessor()
    if (strScript == null || checking)
      return null;
    checking = true;
    if (strScript.indexOf(")") == 0) // history disabled
      strScript = strScript.substring(1);
    Object obj = eval.checkScriptSilent(strScript);
    checking = false;
    if (obj instanceof String)
      return (String) obj;
    return null;
  }

  public boolean isScriptExecuting() {
    return eval.isScriptExecuting();
  }

  public void haltScriptExecution() {
    eval.haltExecution();
  }

  public void resumeScriptExecution() {
    eval.resumePausedExecution();
  }

  public void pauseScriptExecution() {
    refresh(0, "pauseScriptExecution");
    eval.pauseExecution();
  }

  private void setDefaultLoadScript(String script) {
    //Eval
    global.defaultLoadScript = script;
  }

  String getDefaultLoadScript() {
    return global.defaultLoadScript;
  }

  private void setLanguage(String language) {
    statusManager.setCallbackFunction("language", language);
    global.setParameterValue("language", GT.getLanguage());
  }

  private void setLoadFormat(String format) {
    //Eval
    global.loadFormat = format;
  }

  String getLoadFormat() {
    return global.loadFormat;
  }

  public String getStandardLabelFormat() {
    return stateManager.getStandardLabelFormat();
  }

  public int getRibbonAspectRatio() {
    //mps
    return global.ribbonAspectRatio;
  }

  private void setRibbonAspectRatio(int ratio) {
    //Eval
    global.ribbonAspectRatio = ratio;
  }

  public float getSheetSmoothing() {
    //mps
    return global.sheetSmoothing;
  }

  private void setSheetSmoothing(float factor0To1) {
    //Eval
    global.sheetSmoothing = factor0To1;
  }

  public boolean getSsbondsBackbone() {
    return global.ssbondsBackbone;
  }

  private void setHbondsBackbone(boolean TF) {
    //Eval
    global.hbondsBackbone = TF;
  }

  public boolean getHbondsBackbone() {
    return global.hbondsBackbone;
  }

  private void setHbondsSolid(boolean TF) {
    //Eval
    global.hbondsSolid = TF;
  }

  public boolean getHbondsSolid() {
    return global.hbondsSolid;
  }

  public void setMarBond(short marBond) {
    global.bondRadiusMilliAngstroms = marBond;
    global.setParameterValue("bondRadiusMilliAngstroms", marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2, BitSetUtil.setAll(getAtomCount()));
  }

  int hoverAtomIndex = -1;
  String hoverText;

  public void hoverOn(int atomIndex) {
    if (eval != null && isScriptExecuting() || atomIndex == hoverAtomIndex
        || global.hoverDelayMs == 0)
      return;
    if (!isInSelectionSubset(atomIndex))
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target",
        new Integer(atomIndex));
    hoverText = null;
    hoverAtomIndex = atomIndex;
  }

  int getHoverDelay() {
    return global.hoverDelayMs;
  }

  private void setHoverDelay(int milliSec) {
    global.hoverDelayMs = milliSec;
  }

  public void hoverOn(int x, int y, String text) {
    // from draw for drawhover on
    if (eval != null && isScriptExecuting())
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "xy", new Point3i(x, y, 0));
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", text);
    hoverAtomIndex = -1;
    hoverText = text;
  }

  void hoverOff() {
    if (hoverAtomIndex >= 0) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
      hoverAtomIndex = -1;
    }
    if (hoverText != null) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
      hoverText = null;
    }
  }

  private void setAtomHoverLabel(String text) {
    setShapeProperty(JmolConstants.SHAPE_HOVER, "atomLabel", text);
  }

  void setLabel(String strLabel) {
    //Eval
    if (strLabel != null) { // force the class to load and display
      loadShape(JmolConstants.SHAPE_LABELS);
      setShapeSize(JmolConstants.SHAPE_LABELS, 0);
    }
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  int n;

  void togglePickingLabel(BitSet bs) {
    //eval label toggle (atomset) and pickingManager
    if (bs == null)
      bs = selectionManager.bsSelection;
    loadShape(JmolConstants.SHAPE_LABELS);
    setShapeSize(JmolConstants.SHAPE_LABELS, 0, null);
    modelSet.setShapeProperty(JmolConstants.SHAPE_LABELS, "toggleLabel",
        null, bs);
    refresh(0, "Viewer:togglePickingLabel()");
  }

  BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  public void loadShape(int shapeID) {
    modelSet.loadShape(shapeID);
  }

  void setShapeSize(int shapeID, int size) {
    //Eval - many
    //stateManager.setCrystallographicDefaults
    //Viewer - many
    setShapeSize(shapeID, size, selectionManager.bsSelection);
  }

  public void setShapeSize(int shapeID, int size, BitSet bsAtoms) {
    //above,
    //Eval.configuration
    modelSet.setShapeSize(shapeID, size, bsAtoms);
    refresh(0, "Viewer:setShapeSize(" + shapeID + "," + size + ")");
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value) {
    //Eval
    //many local

    /*
     * Logger.debug("JmolViewer.setShapeProperty("+
     * JmolConstants.shapeClassBases[shapeID]+ "," + propertyName + "," + value +
     * ")");
     */
    if (shapeID < 0)
      return; //not applicable
    modelSet.setShapeProperty(shapeID, propertyName, value,
        selectionManager.bsSelection);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bs) {
    //Eval color
    if (shapeID < 0)
      return; //not applicable
    modelSet.setShapeProperty(shapeID, propertyName, value, bs);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
    //Eval
    setShapeProperty(shapeID, propertyName, argb == 0 ? null : new Integer(
        argb | 0xFF000000));
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return modelSet.getShapeProperty(shapeType, propertyName,
        Integer.MIN_VALUE);
  }

  Object getShapeProperty(int shapeType, String propertyName, int index) {
    return modelSet.getShapeProperty(shapeType, propertyName, index);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  short getColix(Object object) {
    return Graphics3D.getColix(object);
  }

  private void setRasmolHydrogenSetting(boolean b) {
    //Eval
    global.rasmolHydrogenSetting = b;
  }

  boolean getRasmolHydrogenSetting() {
    return global.rasmolHydrogenSetting;
  }

  private void setRasmolHeteroSetting(boolean b) {
    //Eval
    global.rasmolHeteroSetting = b;
  }

  boolean getRasmolHeteroSetting() {
    return global.rasmolHeteroSetting;
  }

  boolean getDebugScript() {
    return global.debugScript;
  }

  public void setDebugScript(boolean debugScript) {
    global.debugScript = debugScript;
    global.setParameterValue("debugScript", debugScript);
  }

  void atomPicked(int atomIndex, int modifiers) {
    if (!isInSelectionSubset(atomIndex))
      return;
    pickingManager.atomPicked(atomIndex, modifiers);
  }

  void clearClickCount() {
    //MouseManager.clearclickCount()
    mouseManager.clearClickCount();
    setTainted(true);
  }

  public final static int CURSOR_DEFAULT = 0;
  public final static int CURSOR_HAND = 1;
  public final static int CURSOR_CROSSHAIR = 2;
  public final static int CURSOR_MOVE = 3;
  public final static int CURSOR_WAIT = 4;

  private int currentCursor = CURSOR_DEFAULT;

  public void setCursor(int cursor) {
    if (currentCursor == cursor || display == null)
      return;
    int c;
    switch (currentCursor = cursor) {
    case CURSOR_HAND:
      c = Cursor.HAND_CURSOR;
      break;
    case CURSOR_MOVE:
      c = Cursor.MOVE_CURSOR;
      break;
    case CURSOR_CROSSHAIR:
      c = Cursor.CROSSHAIR_CURSOR;
      break;
    case CURSOR_WAIT:
      c = Cursor.WAIT_CURSOR;
      break;
    default:
      display.setCursor(Cursor.getDefaultCursor());
      return;
    }
    display.setCursor(Cursor.getPredefinedCursor(c));
  }

  private void setPickingMode(String mode) {
    int pickingMode = JmolConstants.getPickingMode(mode);
    if (pickingMode < 0)
      pickingMode = JmolConstants.PICKING_IDENT;
    pickingManager.setPickingMode(pickingMode);
    global.setParameterValue("picking", 
        JmolConstants.getPickingModeName(pickingManager.getPickingMode()));
  }

  public int getPickingMode() {
    return pickingManager.getPickingMode();
  }

  private void setDrawPicking(boolean TF) {
    global.drawPicking = TF;
  }
  
  public boolean getDrawPicking() {
    return global.drawPicking;
  }
  
  private void setPickingStyle(String style) {
    int pickingStyle = JmolConstants.getPickingStyle(style);
    if (pickingStyle < 0)
      pickingStyle = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
    pickingManager.setPickingStyle(pickingStyle);
    global.setParameterValue("pickingStyle", 
        JmolConstants.getPickingStyleName(pickingManager.getPickingStyle()));
  }

  void setDrawHover(boolean TF) {
    pickingManager.setDrawHover(TF);
  }

  public boolean getDrawHover() {
    return pickingManager.getDrawHover();
  }

  public String getAtomInfo(int atomIndex) {
    return modelSet.getAtomInfo(atomIndex);
  }

  public String getAtomInfoXYZ(int atomIndex, boolean useChimeFormat) {
    return modelSet.getAtomInfoXYZ(atomIndex, useChimeFormat);
  }

  // //////////////status manager dispatch//////////////

  public Hashtable getMessageQueue() {
    return statusManager.messageQueue;
  }

  private void setCallbackFunction(String callbackType, String callbackFunction) {
    //Eval
    if (callbackFunction.equalsIgnoreCase("none"))
      callbackFunction = null;
    statusManager.setCallbackFunction(callbackType, callbackFunction);
  }

  /*
   no local version of MessageCallback
   */

  public void setStatusAtomPicked(int atomIndex, String info) {
    String s = statusManager.getCallbackScript("pickcallback");
    global.setParameterValue("_atompicked", atomIndex);
    global.setParameterValue("_pickinfo", info);
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.setStatusAtomPicked(atomIndex, info);
  }

  public void setStatusAtomHovered(int atomIndex, String info) {
    String s = statusManager.getCallbackScript("hovercallback");
    global.setParameterValue("_atomhovered", atomIndex);
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.setStatusAtomHovered(atomIndex, info);
  }

  public void setStatusMeasurePicked(int iatom, String strMeasure) {
    //for pending measurements or "set picking measure"
    statusManager.setStatusMeasurePicked(iatom, strMeasure);
  }

  public void setStatusMeasuring(String status, int count,
                                                 String strMeasure) {
    //measurement completed
    statusManager.setStatusMeasuring(status, count, strMeasure);
  }

  public void setStatusResized(int width, int height) {
    String s = statusManager.getCallbackScript("resizecallback");
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.setStatusResized(width, height);
  }

  void setStatusScriptStarted(int iscript, String script) {
    statusManager.setStatusScriptStarted(iscript, script);
  }

  Vector getStatusChanged(String statusNameList) {
    return statusManager.getStatusChanged(statusNameList);
  }

  void popupMenu(int x, int y) {
    if (global.disablePopupMenu)
      return;
    setFrankOn(true);
    statusManager.popupMenu(x, y);
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    statusManager.setJmolStatusListener(jmolStatusListener);
  }

  void setStatusFrameChanged(int frameNo) {
    transformManager.setVibrationPeriod(Float.NaN);
    int modelIndex = repaintManager.currentModelIndex;
    int firstIndex = repaintManager.firstModelIndex;
    int lastIndex = repaintManager.lastModelIndex;
    
    if (firstIndex == lastIndex)
      modelIndex = firstIndex;
    int frameID = getModelFileNumber(modelIndex);
    int fileNo = frameID;
    int modelNo = frameID % 1000000;
    int firstNo = getModelFileNumber(firstIndex);
    int lastNo = getModelFileNumber(lastIndex);
    String s;
    if (fileNo == 0) {
      s = getModelNumberDotted(firstIndex);
      if (firstIndex != lastIndex)
        s+= " - " + getModelNumberDotted(lastIndex);
      if (firstNo / 1000000 == lastNo / 1000000)
        fileNo = firstNo;
    } else {
      s = getModelNumberDotted(modelIndex);
    }
    if (fileNo != 0)
      fileNo = (fileNo < 1000000 ? 1 : fileNo / 1000000);

    global.setParameterValue("_currentFileNumber", fileNo);
    global.setParameterValue("_currentModelNumberInFile", modelNo);
    global.setParameterValue("_frameID", frameID);
    global.setParameterValue("_modelNumber", s);
    global.setParameterValue("_modelName", (modelIndex < 0 ? ""
        : getModelName(modelIndex)));
    global.setParameterValue("_modelTitle", (modelIndex < 0 ? "" 
        : getModelTitle(modelIndex)));
    global.setParameterValue("_modelFile", (modelIndex < 0 ? "" : getModelFileName(modelIndex)));

    s = statusManager.getCallbackScript("animframecallback");
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.setStatusFrameChanged(frameNo, fileNo, modelNo,
          (repaintManager.animationDirection < 0 ? -firstNo : firstNo),
          (repaintManager.currentDirection < 0 ? -lastNo : lastNo));
  }

  private String getModelTitle(int modelIndex) {
    //necessary for status manager frame change?
    return modelSet == null ? null : modelSet.getModelTitle(modelIndex);
  }

  private String getModelFileName(int modelIndex) {
    //necessary for status manager frame change?
    return modelSet == null ? null : modelSet.getModelFileName(modelIndex);
  }
  
  private void setStatusFileLoaded(int ptLoad, String fullPathName,
                                   String fileName, String modelName,
                                   String strError) {
    String s = statusManager.getCallbackScript("loadstructcallback");
    if (s != null)
      evalStringQuiet(s, true, false);
    else
      statusManager.setStatusFileLoaded(fullPathName, fileName, modelName, strError, ptLoad);
  }

  private void setStatusFileNotLoaded(String fullPathName, String errorMsg) {
    setStatusFileLoaded(-1, fullPathName, null, null, errorMsg);
  }

  public void scriptEcho(String strEcho) {
    statusManager.setScriptEcho(strEcho);
    if (listCommands && strEcho != null && strEcho.indexOf("$[") == 0)
      Logger.info(strEcho);
  }
  
  private void scriptError(String msg) {
    Logger.error(msg);
    scriptEcho(msg);
  }

  void scriptStatus(String strStatus) {
    statusManager.setScriptStatus(strStatus);
  }

  private void setScriptDelay(int milliSec) {
    global.scriptDelay = milliSec;    
  }

  int getScriptDelay() {
    return global.scriptDelay;
  }

  public void showUrl(String urlString) {
    //applet.Jmol
    //app Jmol
    //StatusManager
    if (urlString == null)
      return;
    if (urlString.indexOf(":") < 0) {
      String base = fileManager.getAppletDocumentBase();
      if (base == "")
        base = fileManager.getFullPathName();
      if (base.indexOf("/") >= 0) {
        base = base.substring(0, base.lastIndexOf("/") + 1);
      } else if (base.indexOf("\\") >= 0) {
        base = base.substring(0, base.lastIndexOf("\\") + 1);
      }
      urlString = base + urlString;
    }
    Logger.info("showUrl:" + urlString);
    statusManager.showUrl(urlString);
  }

  void showConsole(boolean showConsole) {
    //Eval
    statusManager.showConsole(showConsole);
  }

  void clearConsole() {
    //Eval
    statusManager.clearConsole();
  }

  Object getParameterEscaped(String key) {
    return global.getParameterEscaped(key, 0);
  }

  public Object getParameter(String key) {
    return global.getParameter(key);
  }

  void unsetProperty(String name) {
    global.setUserVariable(name, null);
  }

  public boolean getBooleanProperty(String key) {
    return getBooleanProperty(key, true);
  }

  /*****************************************************************************
   * @param key
   * @param doICare  true if you want an error message if it doesn't exist 
   * @return the boolean property mth 2005 06 24 and/or these property names
   *         should be interned strings so that we can just do == comparisions
   *         between strings
   ****************************************************************************/
  
  public boolean getBooleanProperty(String key, boolean doICare) {
    //JmolPopup
    key = key.toLowerCase();
    if (global.htPropertyFlags.containsKey(key)) {
      return ((Boolean) global.htPropertyFlags.get(key)).booleanValue();
    }
    // special cases
    if (key.equalsIgnoreCase("haveBFactors"))
      return (modelSet.getBFactors() != null);
    if (key.equalsIgnoreCase("colorRasmol"))
      return colorManager.getDefaultColorRasmol();
    if (key.equalsIgnoreCase("frank"))
      return getShowFrank();
    if (key.equalsIgnoreCase("showSelections"))
      return getSelectionHaloEnabled();
    if (global.htUserVariables.containsKey(key)) {
      Token t = (Token) global.getUserParameterValue(key);
      if (t.tok == Token.on)
        return true;
      if (t.tok == Token.off)
        return false;
    }
    if (doICare)
      Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setStringProperty(String key, String value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    //Eval
    boolean notFound = false;
    while (true) {
      
      ///11.5.39//
      if (key.equalsIgnoreCase("quaternionFrame")) {
        setQuaternionFrame(value);
        break;
      }
      
      ///11.5.11//
      if (key.equalsIgnoreCase("defaultVDW")) {
        setDefaultVdw(value);
        return;
      }
      
      ///11.1.30//
      if (key.equalsIgnoreCase("language")) {
        setLanguage(value); //fr cs en none, etc.
        return;
      }

      ///11.1.22//

      if (key.equalsIgnoreCase("loadFormat")) {
        setLoadFormat(value);
        break;
      }

      ///11.1///

      if (key.equalsIgnoreCase("backgroundColor")) {
        setObjectColor("background", value);
        return;
      }

      if (key.equalsIgnoreCase("axesColor")) {
        setObjectColor("axis1", value);
        setObjectColor("axis2", value);
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("axis1Color")) {
        setObjectColor("axis1", value);
        return;
      }

      if (key.equalsIgnoreCase("axis2Color")) {
        setObjectColor("axis2", value);
        return;
      }

      if (key.equalsIgnoreCase("axis3Color")) {
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("boundBoxColor")) {
        setObjectColor("boundbox", value);
        return;
      }

      if (key.equalsIgnoreCase("unitCellColor")) {
        setObjectColor("unitcell", value);
        return;
      }

      if (key.equalsIgnoreCase("propertyColorScheme")) {
        setPropertyColorScheme(value, false);
        return;
      }

      if (key.equalsIgnoreCase("propertyColorSchemeOverload")) {
        setPropertyColorScheme(value, true);
        return;
      }

      if (key.equalsIgnoreCase("hoverLabel")) {
        setAtomHoverLabel(value);
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("defaultDistanceLabel")) {
        setDefaultMeasurementLabel(2, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultAngleLabel")) {
        setDefaultMeasurementLabel(3, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultTorsionLabel")) {
        setDefaultMeasurementLabel(4, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultLoadScript")) {
        setDefaultLoadScript(value);
        break;
      }
      if (key.equalsIgnoreCase("appletProxy")) {
        setAppletProxy(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultDirectory")) {
        setDefaultDirectory(value);
        break;
      }
      if (key.equalsIgnoreCase("helpPath")) {
        setHelpPath(value);
        break;
      }
      if (key.equalsIgnoreCase("defaults")) {
        setDefaults(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultColorScheme")) {
        setDefaultColors(value);
        break;
      }
      if (key.equalsIgnoreCase("picking")) {
        setPickingMode(value);
        return;
      }
      if (key.equalsIgnoreCase("pickingStyle")) {
        setPickingStyle(value);
        return;
      }
      if (key.equalsIgnoreCase("dataSeparator")) {
        //just saving this
        break;
      }
      if (key.toLowerCase().indexOf("callback") >= 0) {
        setCallbackFunction(key, value);
        break;
      }
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound && key.charAt(0) != '@') {
      //not found -- @ is a silent mode indicator
        if (global.htPropertyFlags.containsKey(key) || global.htPropertyFlagsRemoved.containsKey(key)) {
          scriptError(GT._("ERROR: cannot set boolean flag to string value: {0}", key));
          return;
        }
        //Logger.warn(key + " -- string variable defined (" + value.length()
          //  + " bytes)");
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, new Token(Token.string, value));
  }

  void removeUserVariable(String key) {
    global.removeUserVariable(key);
    if (key.indexOf("callback") >= 0)
      statusManager.setCallbackFunction(key, null);
  }
  
  boolean isJmolVariable(String key) {
    return global.isJmolVariable(key);
  }
  
  public void setFloatProperty(String key, float value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    setFloatProperty(key, value, false);
  }

  private boolean setFloatProperty(String key, float value, boolean isInt) {
    //Eval
    boolean notFound = false;
    while (true) {
      //11.5.30//
      if (key.equalsIgnoreCase("ellipsoidAxisDiameter")) {
        if (isInt)
          value = value / 1000;
        // ellipsoidAxisDiameter" just handled as getParameter()
        break;
      }

      ///11.3.52//
      if (key.equalsIgnoreCase("spinX")) {
        setSpin("x", (int)value);
        break;
      }
      if (key.equalsIgnoreCase("spinY")) {
        setSpin("y", (int)value);
        break;
      }
      if (key.equalsIgnoreCase("spinZ")) {
        setSpin("z", (int)value);
        break;
      }
      if (key.equalsIgnoreCase("spinFPS")) {
        setSpin("fps", (int)value);
        break;
      }
      
      ///11.3.17//
      
      if (key.equalsIgnoreCase("defaultDrawArrowScale")) {
        setDefaultDrawArrowScale(value);
        break;
      }
      
      ///11.1///
      if (key.equalsIgnoreCase("defaultTranslucent")) {
        setDefaultTranslucent(value);
        break;
      }

      if (key.equalsIgnoreCase("axesScale")) {
        setAxesScale(value);
        break;
      }
      if (key.equalsIgnoreCase("visualRange")) {
        setVisualRange(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationDepth")) {
        setNavigationDepthPercent(0, value);
        break;
      }
      if (key.equalsIgnoreCase("navigationSpeed")) {
        setNavigationSpeed(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationSlab")) {
        setNavigationSlabOffset(value);
        break;
      }
      if (key.equalsIgnoreCase("cameraDepth")) {
        setCameraDepth(value);
        break;
      }
      if (key.equalsIgnoreCase("rotationRadius")) {
        setRotationRadius(value);
        break;
      }
      if (key.equalsIgnoreCase("hoverDelay")) {
        setHoverDelay((int) (value * 1000));
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("sheetSmoothing")) {
        setSheetSmoothing(value);
        break;
      }
      if (key.equalsIgnoreCase("dipoleScale")) {
        setDipoleScale(value);
        break;
      }
      if (key.equalsIgnoreCase("stereoDegrees")) {
        setStereoDegrees(value);
        return true;
      }
      if (key.equalsIgnoreCase("vectorScale")) {
        //public -- no need to set
        setVectorScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationPeriod")) {
        //public -- no need to set
        setVibrationPeriod(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationScale")) {
        //public -- no need to set
        setVibrationScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("bondTolerance")) {
        setBondTolerance(value);
        return true;
      }
      if (key.equalsIgnoreCase("minBondDistance")) {
        setMinBondDistance(value);
        return true;
      }
      if (key.equalsIgnoreCase("scaleAngstromsPerInch")) {
        setScaleAngstromsPerInch(value);
        break;
      }
      if (key.equalsIgnoreCase("solventProbeRadius")) {
        setSolventProbeRadius(value);
        break;
      }
      if (key.equalsIgnoreCase("radius")) { //deprecated
        setFloatProperty("solventProbeRadius", value);
        return true;
      }      
      // not found
      if (isInt)
        return false;
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound) {
        if (global.htPropertyFlags.containsKey(key)) {
          scriptError(GT._("ERROR: cannot set boolean flag to numeric value: {0}", key));
          return true;
        }
        //Logger.warn("viewer.setFloatProperty(" + key + "," + value
          //  + ") - float variable defined");
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, new Token(Token.decimal, new Float(value)));
    return true;
  }

  public void setIntProperty(String key, int value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }

    //Eval
    setIntProperty(key, value, true);
  }

  private void setIntProperty(String key, int value, boolean defineNew) {
    boolean notFound = false;    
    while (true) {
      //11.5.30//
      // ellipsoidDotCount" just handled as getParameter()

      //11.5.4//
      if (key.equalsIgnoreCase("delayMaximumMs")) {
        setDelayMaximum(value);
        break;
      }
      
      ///11.3.52//
      
      if (key.equalsIgnoreCase("logLevel")) {
        Logger.setLogLevel(value);
        Logger.info("logging level set to " + value);
        global.setParameterValue("logLevel", value);
        return;
      }
      
      if (key.equalsIgnoreCase("axesMode")) {
        setAxesMode(value);
        return;
      }
      ///11.1.31//

      if (key.equalsIgnoreCase("propertyDataField")) {
        break;
      }

      ///11.1///

      if (key.equalsIgnoreCase("strandCount")) {
        setStrandCount(0, value);
        return;
      }
      if (key.equalsIgnoreCase("strandCountForStrands")) {
        setStrandCount(JmolConstants.SHAPE_STRANDS, value);
        return;
      }
      if (key.equalsIgnoreCase("strandCountForMeshRibbon")) {
        setStrandCount(JmolConstants.SHAPE_MESHRIBBON, value);
        return;
      }
      if (key.equalsIgnoreCase("perspectiveModel")) {
        setPerspectiveModel(value);
        break;
      }
      if (key.equalsIgnoreCase("showScript")) {
        setScriptDelay(value);
        break;
      }
      if (key.equalsIgnoreCase("specularPower")) {
        setSpecularPower(value);
        break;
      }
      if (key.equalsIgnoreCase("specularExponent")) {
        setSpecularExponent(value);
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setIntProperty("specularPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("diffuse")) {
        setIntProperty("diffusePercent", value);
        return;
      }
      if (key.equalsIgnoreCase("ambient")) {
        setIntProperty("ambientPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("specularPercent")) {
        setSpecularPercent(value);
        break;
      }
      if (key.equalsIgnoreCase("diffusePercent")) {
        setDiffusePercent(value);
        break;
      }
      if (key.equalsIgnoreCase("ambientPercent")) {
        setAmbientPercent(value);
        break;
      }

      if (key.equalsIgnoreCase("ribbonAspectRatio")) {
        setRibbonAspectRatio(value);
        break;
      }
      if (key.equalsIgnoreCase("pickingSpinRate")) {
        setPickingSpinRate(value);
        break;
      }
      if (key.equalsIgnoreCase("animationFps")) {
        setAnimationFps(value);
        break;
      }
      if (key.equalsIgnoreCase("percentVdwAtom")) {
        setPercentVdwAtom(value);
        break;
      }
      if (key.equalsIgnoreCase("bondRadiusMilliAngstroms")) {
        setMarBond((short) value);
        //public method -- no need to set
        return;
      }
      if (key.equalsIgnoreCase("hermiteLevel")) {
        setHermiteLevel(value);
        break;
      }
      //not found
      if ((value != 0 && value != 1)
          || !setBooleanProperty(key, value == 1, false)) {
        if (setFloatProperty(key, value, true))
          return;
      }
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound) {
      if (global.htPropertyFlags.containsKey(key)) {
        scriptError(GT._("ERROR: cannot set boolean flag to numeric value: {0}", key));
        return;
      }
      //Logger.info("viewer.setIntProperty(" + key + "," + value
        //  + ") - integer variable defined");      
    }
    if (!defineNew)
      return;
    if (isJmol) {
      global.setParameterValue(key, value);      
    } else {
      global.setUserVariable(key, Token.intToken(value));
    }
  }

  private void setDelayMaximum(int ms) {
    global.delayMaximumMs = ms;
  }
  
  int getDelayMaximum() {
    return global.delayMaximumMs;
  }

  public void setBooleanProperty(String key, boolean value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    setBooleanProperty(key, value, true);
  }

  boolean setBooleanProperty(String key, boolean value, boolean defineNew) {
    boolean notFound = false;
    boolean doRepaint = true;
    while (true) {
      //11.5.39
      if (key.equalsIgnoreCase("messageStyleChime")) {
        setMessageStyleChime(value);
        break;
      }
      if (key.equalsIgnoreCase("pdbSequential")) {
        setPdbLoadInfo(value, 1);
        break;
      }
      if (key.equalsIgnoreCase("pdbGetHeader")) {
        setPdbLoadInfo(value, 2);
        break;
      }
      
      //11.5.30
      // ellipsoidAxes just handled as getBooleanProperty()
      // ellipsoidArcs just handled as getBooleanProperty()
      // ellipsoidDots just handled as getBooleanProperty()
      // ellipsoidBall just handled as getBooleanProperty()
      //11.5.4
      if (key.equalsIgnoreCase("fontScaling")) {
        setFontScaling(value);
        break;
      }
      //11.3.56      
      if (key.equalsIgnoreCase("syncMouse")) {
        setSyncTarget(0, value);
        break;
      }
      if (key.equalsIgnoreCase("syncScript")) {
        setSyncTarget(1, value);
        break;
      }

      // 11.3.55
      
      if (key.equalsIgnoreCase("wireframeRotation")) {
        setWireframeRotation(value);
        break;
      }

      //11.3.46
      
      if (key.equalsIgnoreCase("isosurfacePropertySmoothing")) {
        setIsosurfacePropertySmoothing(value);
        break;
      }

      //11.3.43
      
      if (key.equalsIgnoreCase("drawPicking")) {
        setDrawPicking(value);
        break;
      }

      //11.3.36
      
      if (key.equalsIgnoreCase("antialiasDisplay")) {
        setAntialias(0, value);
        break;
      }

      if (key.equalsIgnoreCase("antialiasTranslucent")) {
        setAntialias(1, value);
        break;
      }

      if (key.equalsIgnoreCase("antialiasImages")) {
        setAntialias(2, value);
        break;
      }

      //11.3.29
      
      if (key.equalsIgnoreCase("smartAromatic")) {
        setSmartAromatic(value);
        break;
      }

      //11.1.29

      if (key.equalsIgnoreCase("applySymmetryToBonds")) {
        setApplySymmetryToBonds(value);
        break;
      }

      //11.1.22

      if (key.equalsIgnoreCase("appendNew")) {
        setAppendNew(value);
        break;
      }

      if (key.equalsIgnoreCase("autoFPS")) {
        setAutoFps(value);
        break;
      }

      //11.1.21

      if (key.equalsIgnoreCase("useNumberLocalization")) {
        setUseNumberLocalization(value);
        break;
      }

      //11.1.20

      if (key.equalsIgnoreCase("showFrank")) {
        setFrankOn(value);
        break;
      }

      /////

      if (key.equalsIgnoreCase("solventProbe")) {
        setSolventOn(value);
        break;
      }

      if (key.equalsIgnoreCase("dynamicMeasurements")) {
        setDynamicMeasurements(value);
        break;
      }

      //11.1.14

      if (key.equalsIgnoreCase("allowRotateSelected")) {
        setAllowRotateSelected(value);
        break;
      }

      ///11.1.13///

      if (key.equalsIgnoreCase("showScript")) {
        setIntProperty("showScript", value ? 1 : 0);
        return true;
      }
      ///11.1///
      if (key.equalsIgnoreCase("allowEmbeddedScripts")) {
        setAllowEmbeddedScripts(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationPeriodic")) {
        setNavigationPeriodic(value);
        break;
      }
      if (key.equalsIgnoreCase("zShade")) {
        setZShade(value);
        break;
      }
      if (key.equalsIgnoreCase("drawHover")) {
        setDrawHover(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationMode")) {
        setNavigationMode(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNavigationPoint")) {
        setHideNavigationPoint(value);
        break;  
      }
      if (key.equalsIgnoreCase("showNavigationPointAlways")) {
        setShowNavigationPointAlways(value);
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("refreshing")) {
        setRefreshing(value);
        break;
      }
      if (key.equalsIgnoreCase("justifyMeasurements")) {
        setJustifyMeasurements(value);
        break;
      }
      if (key.equalsIgnoreCase("ssBondsBackbone")) {
        setSsbondsBackbone(value);
        break;
      }
      if (key.equalsIgnoreCase("hbondsBackbone")) {
        setHbondsBackbone(value);
        break;
      }
      if (key.equalsIgnoreCase("hbondsSolid")) {
        setHbondsSolid(value);
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setSpecular(value);
        break;
      }
      if (key.equalsIgnoreCase("slabEnabled")) {
        setSlabEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("zoomEnabled")) {
        setZoomEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("solventProbe")) {
        setSolventOn(value);
        break;
      }
      if (key.equalsIgnoreCase("highResolution")) {
        setHighResolution(value);
        break;
      }
      if (key.equalsIgnoreCase("traceAlpha")) {
        setTraceAlpha(value);
        break;
      }
      if (key.equalsIgnoreCase("zoomLarge")) {
        setZoomLarge(value);
        break;
      }
      if (key.equalsIgnoreCase("languageTranslation")) {
        GT.setDoTranslate(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNotSelected")) {
        setHideNotSelected(value);
        break;
      }
      if (key.equalsIgnoreCase("colorRasmol")) {
        setDefaultColors(value ? "rasmol" : "jmol");
        break;
      }
      if (key.equalsIgnoreCase("scriptQueue")) {
        setScriptQueue(value);
        break;
      }
      if (key.equalsIgnoreCase("dotSurface")) {
        setDotSurfaceFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("dotsSelectedOnly")) {
        setDotsSelectedOnly(value);
        break;
      }
      if (key.equalsIgnoreCase("selectionHalos")) {
        setSelectionHalos(value); //volatile
        break;
      }
      if (key.equalsIgnoreCase("selectHydrogen")) {
        setRasmolHydrogenSetting(value);
        break;
      }
      if (key.equalsIgnoreCase("selectHetero")) {
        setRasmolHeteroSetting(value);
        break;
      }
      if (key.equalsIgnoreCase("showMultipleBonds")) {
        setShowMultipleBonds(value);
        break;
      }
      if (key.equalsIgnoreCase("showHiddenSelectionHalos")) {
        setShowHiddenSelectionHalos(value);
        break;
      }
      if (key.equalsIgnoreCase("windowCentered")) {
        setWindowCentered(value);
        break;
      }
      if (key.equalsIgnoreCase("displayCellParameters")) {
        setDisplayCellParameters(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag1")) {
        setTestFlag1(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag2")) {
        setTestFlag2(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag3")) {
        setTestFlag3(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag4")) {
        setTestFlag4(value);
        break;
      }
      if (key.equalsIgnoreCase("ribbonBorder")) {
        setRibbonBorder(value);
        break;
      }
      if (key.equalsIgnoreCase("cartoonRockets")) {
        setCartoonRocketFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("rocketBarrels")) {
        setRocketBarrelFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("greyscaleRendering")) {
        setGreyscaleRendering(value);
        break;
      }
      if (key.equalsIgnoreCase("measurementLabels")) {
        setShowMeasurementLabels(value);
        break;
      }
      
      // these next three remove parameters, so don't set htParameter key here
      
      if (key.equalsIgnoreCase("axesWindow")) {
        setAxesModeMolecular(!value);
        return true;
      }
      if (key.equalsIgnoreCase("axesMolecular")) {
        setAxesModeMolecular(value);
        return true;
      }
      if (key.equalsIgnoreCase("axesUnitCell")) {
        setAxesModeUnitCell(value);
        return true;
      }
      //public; no need to set here
      if (key.equalsIgnoreCase("axesOrientationRasmol")) {
        setAxesOrientationRasmol(value);
        return true;
      }
      if (key.equalsIgnoreCase("debugScript")) {
        setDebugScript(value);
        return true;
      }
      if (key.equalsIgnoreCase("perspectiveDepth")) {
        setPerspectiveDepth(value);
        return true;
      }
      if (key.equalsIgnoreCase("showAxes")) {
        setShowAxes(value);
        return true;
      }
      if (key.equalsIgnoreCase("showBoundBox")) {
        setShowBbcage(value);
        return true;
      }
      if (key.equalsIgnoreCase("showHydrogens")) {
        setShowHydrogens(value);
        return true;
      }
      if (key.equalsIgnoreCase("showMeasurements")) {
        setShowMeasurements(value);
        return true;
      }
      if (key.equalsIgnoreCase("showUnitcell")) {
        setShowUnitCell(value);
        return true;
      }
      //these next are deprecated because they don't 
      //give much indication what they really do:
      if (key.equalsIgnoreCase("frank"))
        return setBooleanProperty("showFrank", value, true);
      if (key.equalsIgnoreCase("solvent"))
        return setBooleanProperty("solventProbe", value, true);
      if (key.equalsIgnoreCase("bonds"))
        return setBooleanProperty("showMultipleBonds", value, true);
      if (key.equalsIgnoreCase("hydrogen")) //deprecated
        return setBooleanProperty("selectHydrogen", value, true);
      if (key.equalsIgnoreCase("hetero")) //deprecated
        return setBooleanProperty("selectHetero", value, true);
      if (key.equalsIgnoreCase("showSelections")) //deprecated -- see "selectionHalos"
        return setBooleanProperty("selectionHalos", value, true);
      // these next return, because there is no need to repaint
      while (true) {
        doRepaint = false;
        if (key.equalsIgnoreCase("bondModeOr")) {
          setBondSelectionModeOr(value);
          break;
        }
        if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
          setZeroBasedXyzRasmol(value);
          break;
        }
        if (key.equalsIgnoreCase("rangeSelected")) {
          setRangeSelected(value);
          break;
        }
        if (key.equalsIgnoreCase("measureAllModels")) {
          setMeasureAllModels(value);
          break;
        }
        if (key.equalsIgnoreCase("statusReporting")) {
          setAllowStatusReporting(value);
          break;
        }
        if (key.equalsIgnoreCase("chainCaseSensitive")) {
          setChainCaseSensitive(value);
          break;
        }
        if (key.equalsIgnoreCase("hideNameInPopup")) {
          setHideNameInPopup(value);
          break;
        }
        if (key.equalsIgnoreCase("disablePopupMenu")) {
          setDisablePopupMenu(value);
          break;
        }
        if (key.equalsIgnoreCase("forceAutoBond")) {
          setForceAutoBond(value);
          break;
        }
        //public - no need to set
        if (key.equalsIgnoreCase("autobond")) {
          setAutoBond(value);
          return true;
        }
        notFound = true;
        break;
      }
      if (!defineNew)
        return !notFound;
      notFound = true;
      break;
    } 
    if (!defineNew)
      return !notFound;
    key = key.toLowerCase();
    boolean isJmol = global.htPropertyFlags.containsKey(key);
    if (!isJmol && notFound) {
      if (global.htParameterValues.containsKey(key)) {
        scriptError(
            GT._("ERROR: Cannot set value of this variable to a boolean: {0}", key));
        return true;
      }
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, value ? Token.tokenOn : Token.tokenOff);
    if (notFound)
      return false;
    if (doRepaint) {
      setTainted(true);
      refresh(0, "viewer.setBooleanProperty");
    }
    return true;
  }

  private void setPdbLoadInfo(boolean value, int type) {
    switch(type) {
    case 1:
      global.pdbSequential = value;
      return;
    case 2:
      global.pdbGetHeader = value;
      return;
    }
  }

  public boolean getPdbLoadInfo(int type) {
    switch(type) {
    case 1:
      return global.pdbSequential;
    case 2:
      return global.pdbGetHeader;
    }
    return false;
  }
  
  private void setMessageStyleChime(boolean value) {
    global.messageStyleChime = value;
  }

  boolean getMessageStyleChime() {
    return global.messageStyleChime;
  }
  
  private void setFontScaling(boolean value) {
    global.fontScaling = value;    
  }
  
  public boolean getFontScaling() {
    return global.fontScaling;
  }

  void showParameter(String key, boolean ifNotSet, int nMax) {
    String sv = "" + global.getParameterEscaped(key, nMax);
    if (ifNotSet || sv.indexOf("<not set>") < 0)
      showString(key + " = " + sv);
  }

  void showString(String str) {
    Logger.warn(str);
    scriptEcho(str);
  }

  String getAllSettings(String prefix) {
    return global.getAllSettings(prefix);
  }

  ////////  flags and settings ////////

  public boolean getDotSurfaceFlag() {
    return global.dotSurface;
  }

  private void setDotSurfaceFlag(boolean TF) {
    global.dotSurface = TF;
  }

  public boolean getDotsSelectedOnlyFlag() {
    return global.dotsSelectedOnly;
  }

  private void setDotsSelectedOnly(boolean TF) {
    global.dotsSelectedOnly = TF;
  }

  public boolean isRangeSelected() {
    return global.rangeSelected;
  }

  private void setRangeSelected(boolean TF) {
    global.rangeSelected = TF;
  }

  private void setIsosurfacePropertySmoothing(boolean TF) {
    global.isosurfacePropertySmoothing = TF; 
  }
  
  boolean getIsosurfacePropertySmoothing() {
    //Eval
    return global.isosurfacePropertySmoothing;
  }
  
  private void setWireframeRotation(boolean TF) {
    global.wireframeRotation = TF; 
  }
  
  public boolean getWireframeRotation() {
    return global.wireframeRotation;
  }
  
  boolean isWindowCentered() {
    return transformManager.isWindowCentered();
  }

  private void setWindowCentered(boolean TF) {
    //setBooleanProperty
    transformManager.setWindowCentered(TF);
  }

  private void setCameraDepth(float percent) {
    transformManager.setCameraDepthPercent(percent);
    refresh(1, "set cameraDepth");
  }

  void setNavigationDepthPercent(float timeSec, float percent) {
    transformManager.setNavigationDepthPercent(timeSec, percent);
    refresh(1, "set navigationDepth");
  }

  private void setNavigationSpeed(float value) {
    global.navigationSpeed = value;
  }

  float getNavigationSpeed() {
    return global.navigationSpeed;
  }

  private void setShowNavigationPointAlways(boolean TF) {
    global.showNavigationPointAlways = TF;
  }

  private void setHideNavigationPoint(boolean TF) {
    global.hideNavigationPoint = TF;
  }

  public boolean getShowNavigationPoint() {
    if (!global.navigationMode || !transformManager.canNavigate())
      return false;
    return (getNavigating() && !global.hideNavigationPoint
        || global.showNavigationPointAlways || getInMotion());
  }

  public void setVisualRange(float angstroms) {
    transformManager.setVisualRange(angstroms);
    refresh(1, "set visualRange");
  }

  private void setSolventProbeRadius(float radius) {
    //Eval
    global.solventProbeRadius = radius;
  }

  float getSolventProbeRadius() {
    return global.solventProbeRadius;
  }

  public float getCurrentSolventProbeRadius() {
    return global.solventOn ? global.solventProbeRadius : 0;
  }

  private void setSolventOn(boolean isOn) {
    //Eval
    global.solventOn = isOn;
  }

  boolean getSolventOn() {
    return global.solventOn;
  }

  private void setAllowStatusReporting(boolean TF) {
    //not part of the state
    statusManager.setAllowStatusReporting(TF);
  }

  private void setTestFlag1(boolean value) {
    global.testFlag1 = value;
  }

  public boolean getTestFlag1() {
    return global.testFlag1;
  }

  public boolean getTestFlag2() {
    return global.testFlag2;
  }

  private void setTestFlag2(boolean value) {
    global.testFlag2 = value;
  }

  public boolean getTestFlag3() {
    return global.testFlag3;
  }

  private void setTestFlag3(boolean value) {
    global.testFlag3 = value;
  }

  public boolean getTestFlag4() {
    return global.testFlag4;
  }

  private void setTestFlag4(boolean value) {
    global.testFlag4 = value;
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    //setBooleanProperty                      
    //stateManager.setCrystallographicDefaults
    //app preferences dialog   
    global.setParameterValue("perspectiveDepth", perspectiveDepth);
    transformManager.setPerspectiveDepth(perspectiveDepth);
    refresh(0, "Viewer:setPerspectiveDepth()");
  }

  public void setAxesOrientationRasmol(boolean TF) {
    //app PreferencesDialog
    //stateManager
    //setBooleanproperty
    global.setParameterValue("axesOrientationRasmol", TF);
    transformManager.setAxesOrientationRasmol(TF);
    refresh(0, "Viewer:setAxesOrientationRasmol()");
  }

  public boolean getAxesOrientationRasmol() {
    return transformManager.axesOrientationRasmol;
  }

  void setAxesScale(float scale) {
    global.axesScale = scale;
    axesAreTainted = true;
    refresh(0, "set axesScale");
  }

  public Point3f[] getAxisPoints() {
    return (getObjectMad(StateManager.OBJ_AXIS1) == 0
        || getAxesMode() != JmolConstants.AXES_MODE_UNITCELL ? null
        : (Point3f[]) getShapeProperty(JmolConstants.SHAPE_AXES, "axisPoints"));
  }

  public float getAxesScale() {
    return global.axesScale;
  }

  private void setAxesMode(int mode) {
    switch (mode) {
    case JmolConstants.AXES_MODE_MOLECULAR:
      setAxesModeMolecular(true);
      return;
    case JmolConstants.AXES_MODE_BOUNDBOX:
      setAxesModeMolecular(false);
      return;
    case JmolConstants.AXES_MODE_UNITCELL:
      setAxesModeUnitCell(true);
      return;
    }
  }
  private void setAxesModeMolecular(boolean TF) {
    global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeJmolParameter("axesunitcell");
    global.removeJmolParameter(TF ? "axeswindow" : "axesmolecular");
    global.setParameterValue("axesMode",global.axesMode);
    global.setParameterValue(TF ? "axesMolecular" : "axesWindow" ,true);
    
  }

  void setAxesModeUnitCell(boolean TF) {
    //stateManager
    //setBooleanproperty
    global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeJmolParameter("axesmolecular");
    global.removeJmolParameter(TF ? "axeswindow" : "axesunitcell");
    global.setParameterValue(TF ? "axesUnitcell" : "axesWindow" ,true);
    global.setParameterValue("axesMode",global.axesMode);
  }

  public int getAxesMode() {
    return global.axesMode;
  }

  private void setDisplayCellParameters(boolean displayCellParameters) {
    global.displayCellParameters = displayCellParameters;
  }

  public boolean getDisplayCellParameters() {
    return global.displayCellParameters;
  }

  public boolean getPerspectiveDepth() {
    return transformManager.getPerspectiveDepth();
  }

  public void setSelectionHalos(boolean TF) {
    // display panel can hit this without a frame, apparently
    if (modelSet == null || TF == getSelectionHaloEnabled())
      return;
    global.setParameterValue("selectionHalos", TF);
    loadShape(JmolConstants.SHAPE_HALOS);
    //a frame property, so it is automatically reset
    modelSet.setSelectionHaloEnabled(TF);
  }

  public boolean getSelectionHaloEnabled() {
    return modelSet.getSelectionHaloEnabled();
  }

  private void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    //Eval
    global.bondModeOr = bondSelectionModeOr;
    refresh(0, "Viewer:setBondSelectionModeOr()");
  }

  public boolean getBondSelectionModeOr() {
    return global.bondModeOr;
  }

  public boolean getChainCaseSensitive() {
    return global.chainCaseSensitive;
  }

  private void setChainCaseSensitive(boolean chainCaseSensitive) {
    global.chainCaseSensitive = chainCaseSensitive;
  }

  public boolean getRibbonBorder() {
    //mps
    return global.ribbonBorder;
  }

  private void setRibbonBorder(boolean borderOn) {
    global.ribbonBorder = borderOn;
  }

  public boolean getCartoonRocketFlag() {
    return global.cartoonRockets;
  }

  public boolean getRocketBarrelFlag() {
    return global.rocketBarrels;
  }

  private void setCartoonRocketFlag(boolean TF) {
    global.cartoonRockets = TF;
  }

  private void setRocketBarrelFlag(boolean TF) {
    global.rocketBarrels = TF;
  }

  private void setStrandCount(int type, int value) {
    switch(type) {
    case JmolConstants.SHAPE_STRANDS:
      global.strandCountForStrands = value;
      break;
    case JmolConstants.SHAPE_MESHRIBBON:
      global.strandCountForMeshRibbon = value;
      break;
    default:
      global.strandCountForStrands = value;
      global.strandCountForMeshRibbon = value;
      break;
    }
    global.setParameterValue("strandCount",value);
    global.setParameterValue("strandCountForStrands",global.strandCountForStrands);
    global.setParameterValue("strandCountForMeshRibbon",global.strandCountForMeshRibbon);
  }
  
  public int getStrandCount(int type) {
    return (type == JmolConstants.SHAPE_STRANDS ? 
        global.strandCountForStrands : global.strandCountForMeshRibbon);
  }
  
  boolean getHideNameInPopup() {
    return global.hideNameInPopup;
  }

  private void setHideNameInPopup(boolean hideNameInPopup) {
    global.hideNameInPopup = hideNameInPopup;
  }

  private void setNavigationPeriodic(boolean TF) {
    global.navigationPeriodic = TF;
  }

  boolean getNavigationPeriodic() {
    return global.navigationPeriodic;
  }

  private void setNavigationMode(boolean TF) {
    global.navigationMode = TF;
    if (TF && !transformManager.canNavigate()) {
      setVibrationOff();
      transformManager = transformManager.getNavigationManager(this,
          dimScreen.width, dimScreen.height);
      transformManager.homePosition();
    }
    transformManager.setNavigationMode(TF);
    //refresh(1,"set navigationMode");
  }

  boolean getNavigationMode() {
    return global.navigationMode;
  }

  private void setNavigationSlabOffset(float percent) {
    transformManager.setNavigationSlabOffsetPercent(percent);
  }

  private void setPerspectiveModel(int mode) {
    setVibrationOff();
    switch (mode) {
    case 10:
      transformManager = new TransformManager10(this, dimScreen.width,
          dimScreen.height);
      break;
    default:
      transformManager = transformManager.getNavigationManager(this,
          dimScreen.width, dimScreen.height);
    }    
    setTransformManagerDefaults();
    transformManager.homePosition();
  }

  private void setTransformManagerDefaults() {
    transformManager.setCameraDepthPercent(global.cameraDepth);
    transformManager.setPerspectiveDepth(global.perspectiveDepth);
    transformManager.setStereoDegrees(global.stereoDegrees);
    transformManager.setVisualRange(global.visualRange);
  }

  private void setZoomLarge(boolean TF) {
    global.zoomLarge = TF;
    transformManager.scaleFitToScreen(false, global.zoomLarge, false, true);
  }

  boolean getZoomLarge() {
    return global.zoomLarge;
  }

  private void setTraceAlpha(boolean TF) {
    global.traceAlpha = TF;
  }

  public boolean getTraceAlpha() {
    //mps
    return global.traceAlpha;
  }

  public int getHermiteLevel() {
    //mps
    return global.hermiteLevel;
  }

  private void setHermiteLevel(int level) {
    global.hermiteLevel = level;
  }

  public boolean getHighResolution() {
    //mps
    return global.highResolutionFlag;
  }

  private void setHighResolution(boolean TF) {
    global.highResolutionFlag = TF;
  }

  private void setSsbondsBackbone(boolean TF) {
    //Eval
    global.ssbondsBackbone = TF;
  }

  String getLoadState() {
    return global.getLoadState();
  }

  public void setAutoBond(boolean TF) {
    //setBooleanProperties
    global.setParameterValue("autobond", TF);
    global.autoBond = TF;
  }

  public boolean getAutoBond() {
    return global.autoBond;
  }

  int[] makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB,
                      BitSet bsBonds, boolean isBonds) {
    //eval
    clearMinimization();
    clearAllMeasurements(); // necessary for serialization
    return modelSet.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds);
  }

  public void rebond() {
    //Eval, PreferencesDialog
    clearMinimization();
    modelSet.deleteAllBonds();
    modelSet.autoBond(null, null, null, null);
    addStateScript("connect;", false);
    refresh(0, "Viewer:rebond()");
  }

  void setPdbConectBonding(boolean isAuto) {
    // from eval
    clearMinimization();
    modelSet.deleteAllBonds();
    BitSet bsExclude = new BitSet();
    modelSet.setPdbConectBonding(0, 0, bsExclude);
    if (isAuto) {
      modelSet.autoBond(null, null, bsExclude, null);
      addStateScript("connect PDB AUTO;", false);
      return;
    }
    addStateScript("connect PDB;", false);
    refresh(0, "Viewer:setPdbConnectBonding()");
  }
    
  // //////////////////////////////////////////////////////////////
  // Graphics3D
  // //////////////////////////////////////////////////////////////

  private void setGreyscaleRendering(boolean greyscaleRendering) {
    global.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh(0, "Viewer:setGreyscaleRendering()");
  }

  boolean getGreyscaleRendering() {
    return global.greyscaleRendering;
  }

  private void setDisablePopupMenu(boolean disablePopupMenu) {
    global.disablePopupMenu = disablePopupMenu;
  }

  boolean getDisablePopupMenu() {
    return global.disablePopupMenu;
  }

  private void setForceAutoBond(boolean forceAutoBond) {
    global.forceAutoBond = forceAutoBond;
  }

  public boolean getForceAutoBond() {
    return global.forceAutoBond;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  public void setPercentVdwAtom(int percentVdwAtom) {
    global.setParameterValue("percentVdwAtom", percentVdwAtom);
    global.percentVdwAtom = percentVdwAtom;
    setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom);
  }

  public int getPercentVdwAtom() {
    return global.percentVdwAtom;
  }

  public short getDefaultMadAtom() {
    return (short) (-2000 - global.percentVdwAtom);
  }

  public short getMadBond() {
    return (short) (global.bondRadiusMilliAngstroms * 2);
  }

  public short getMarBond() {
    return global.bondRadiusMilliAngstroms;
  }

  /*
   void setModeMultipleBond(byte modeMultipleBond) {
   //not implemented
   global.modeMultipleBond = modeMultipleBond;
   refresh(0, "Viewer:setModeMultipleBond()");
   }
   */

  public byte getModeMultipleBond() {
    //sticksRenderer
    return global.modeMultipleBond;
  }

  private void setShowMultipleBonds(boolean TF) {
    //Eval.setBonds
    //stateManager
    global.showMultipleBonds = TF;
    refresh(0, "Viewer:setShowMultipleBonds()");
  }

  public boolean getShowMultipleBonds() {
    return global.showMultipleBonds;
  }

  public void setShowHydrogens(boolean TF) {
    //PreferencesDialog
    //setBooleanProperty
    global.setParameterValue("showHydrogens", TF);
    global.showHydrogens = TF;
    refresh(0, "Viewer:setShowHydrogens()");
  }

  public boolean getShowHydrogens() {
    return global.showHydrogens;
  }

  private void setShowHiddenSelectionHalos(boolean TF) {
    //setBooleanProperty
    global.showHiddenSelectionHalos = TF;
    refresh(0, "Viewer:setShowHiddenSelectionHalos()");
  }

  public boolean getShowHiddenSelectionHalos() {
    return global.showHiddenSelectionHalos;
  }

  public void setShowBbcage(boolean value) {
    setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", (short) (value ? -4
        : 0));
    global.setParameterValue("showBoundBox", value);
  }

  public boolean getShowBbcage() {
    return getObjectMad(StateManager.OBJ_BOUNDBOX) != 0;
  }

  public void setShowUnitCell(boolean value) {
    setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitcell", (short) (value ? -4
        : 0));
    global.setParameterValue("showUnitCell", value);
  }

  public boolean getShowUnitCell() {
    return getObjectMad(StateManager.OBJ_UNITCELL) != 0;
  }

  public void setShowAxes(boolean value) {
    setObjectMad(JmolConstants.SHAPE_AXES, "axes", (short) (value ? -2 : 0));
    global.setParameterValue("showAxes", value);
  }

  public boolean getShowAxes() {
    return getObjectMad(StateManager.OBJ_AXIS1) != 0;
  }

  boolean frankOn = true;
  public void setFrankOn(boolean TF) {
    frankOn = TF;
    setObjectMad(JmolConstants.SHAPE_FRANK, "frank", (short) (TF ? 1 : 0));
  }

  public boolean getShowFrank() {
    return getObjectMad(StateManager.OBJ_FRANK) != 0;
  }

  public void setShowMeasurements(boolean TF) {
    //setbooleanProperty
    global.setParameterValue("showMeasurements", TF);
    global.showMeasurements = TF;
    refresh(0, "setShowMeasurements()");
  }

  public boolean getShowMeasurements() {
    return global.showMeasurements;
  }

  private void setShowMeasurementLabels(boolean TF) {
    global.measurementLabels = TF;
  }
  
  public boolean getShowMeasurementLabels() {
    return global.measurementLabels;
  }

  private void setMeasureAllModels(boolean TF) {
    global.measureAllModels = TF;
  }

  public boolean getMeasureAllModelsFlag() {
    return global.measureAllModels;
  }

  void setMeasureDistanceUnits(String units) {
    //stateManager
    //Eval
    global.setMeasureDistanceUnits(units);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
  }

  public String getMeasureDistanceUnits() {
    return global.getMeasureDistanceUnits();
  }

  private void setUseNumberLocalization(boolean TF) {
    global.useNumberLocalization = TF;
    TextFormat.setUseNumberLocalization(TF);
  }

  public boolean getUseNumberLocalization() {
    return global.useNumberLocalization;
  }

  void setAppendNew(boolean TF) {
    //Eval dataFrame
    global.appendNew = TF;
  }

  public boolean getAppendNew() {
    return global.appendNew;
  }

  private void setAutoFps(boolean TF) {
    global.autoFps = TF;
  }

  boolean getAutoFps() {
    return global.autoFps;
  }

  public void setRasmolDefaults() {
    setDefaults("RasMol");
  }

  public void setJmolDefaults() {
    setDefaults("Jmol");
  }

  private void setDefaults(String type) {
    if (type.equalsIgnoreCase("RasMol")) {
      stateManager.setRasMolDefaults();
      return;
    }
    stateManager.setJmolDefaults();
  }

  private void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    //stateManager
    //setBooleanProperty
    global.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
    modelSet.setZeroBased();
  }

  public boolean getZeroBasedXyzRasmol() {
    return global.zeroBasedXyzRasmol;
  }

  private void setAntialias(int mode, boolean TF) {
    switch (mode) {
    case 0: //display
      global.antialiasDisplay = TF;
      break;
    case 1: // translucent
      global.antialiasTranslucent = TF;
      break;
    case 2: // images
      global.antialiasImages = TF;
      return;
    } 
    resizeImage(0, 0, false, false, true);
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

  public Point3f[] allocTempPoints(int size) {
    //rockets renderer
    return tempManager.allocTempPoints(size);
  }

  public void freeTempPoints(Point3f[] tempPoints) {
    tempManager.freeTempPoints(tempPoints);
  }

  public Point3i[] allocTempScreens(int size) {
    //mesh and mps
    return tempManager.allocTempScreens(size);
  }

  public void freeTempScreens(Point3i[] tempScreens) {
    tempManager.freeTempScreens(tempScreens);
  }

  /*
   public boolean[] allocTempBooleans(int size) {
   return tempManager.allocTempBooleans(size);
   }

   public void freeTempBooleans(boolean[] tempBooleans) {
   tempManager.freeTempBooleans(tempBooleans);
   }
   */

  public byte[] allocTempBytes(int size) {
    //mps renderer
    return tempManager.allocTempBytes(size);
  }

  public void freeTempBytes(byte[] tempBytes) {
    tempManager.freeTempBytes(tempBytes);
  }

  // //////////////////////////////////////////////////////////////
  // font stuff
  // //////////////////////////////////////////////////////////////
  Font3D getFont3D(String fontFace, String fontStyle, float fontSize) {
    return g3d.getFont3D(fontFace, fontStyle, fontSize);
  }

  public String formatText(String text0) {
    int i;
    if ((i = text0.indexOf("@{")) < 0 && (i = text0.indexOf("%{")) < 0)
      return text0;
    
    // old style %{ now @{
    
    String text = TextFormat.simpleReplace(text0, "%{", "@{");
    String name;
    while ((i = text.indexOf("@{")) >= 0) {
      i++;
      int i0 = i + 1;
      if (text.indexOf("}", i0) < 0)
        return text;
      int nBrace = 1;
      int len = text.length();
      while (nBrace > 0 && ++i < len) {
        switch(text.charAt(i)) {
        case '{':
          nBrace++;
          break;
        case '}':
          nBrace--;
          break;
        }
      }
      if (nBrace != 0)
        return text;
      name = text.substring(i0, i);
      if (name.length() == 0)
        return text;
      Object v = evaluateExpression(name);
      if (v instanceof Point3f)
        v = Escape.escape((Point3f) v);
      text = text.substring(0, i0 - 2) + v.toString() + text.substring(i + 1);
    }
    return text;
  }

  // //////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  // //////////////////////////////////////////////////////////////

  String getElementSymbol(int i) {
    return modelSet.getElementSymbol(i);
  }

  int getElementNumber(int i) {
    return modelSet.getElementNumber(i);
  }

  public String getAtomName(int i) {
    return modelSet.getAtomName(i);
  }

  public int getAtomNumber(int i) {
    return modelSet.getAtomNumber(i);
  }

  float getAtomX(int i) {
    return modelSet.getAtomX(i);
  }

  float getAtomY(int i) {
    return modelSet.getAtomY(i);
  }

  float getAtomZ(int i) {
    return modelSet.getAtomZ(i);
  }

  public Point3f getAtomPoint3f(int i) {
    return modelSet.getAtomAt(i);
  }

  public float getAtomRadius(int i) {
    return modelSet.getAtomRadius(i);
  }

  public float getAtomVdwRadius(int i) {
    return modelSet.getAtomVdwRadius(i);
  }

  public int getAtomArgb(int i) {
    return g3d.getColixArgb(modelSet.getAtomColix(i));
  }

  String getAtomChain(int i) {
    return modelSet.getAtomChain(i);
  }

  public int getAtomModelIndex(int i) {
    return modelSet.getAtomModelIndex(i);
  }

  String getAtomSequenceCode(int i) {
    return modelSet.getAtomSequenceCode(i);
  }

  public float getBondRadius(int i) {
    return modelSet.getBondRadius(i);
  }

  public short getBondOrder(int i) {
    return modelSet.getBondOrder(i);
  }
  
  void assignAromaticBonds() {
    modelSet.assignAromaticBonds();
  }
  
  public boolean getSmartAromatic() {
    return global.smartAromatic;
  }
  
  private void setSmartAromatic(boolean TF) {
    global.smartAromatic = TF;
  }

  void resetAromatic() {
    modelSet.resetAromatic();
  }
  
  public int getBondArgb1(int i) {
    return g3d.getColixArgb(modelSet.getBondColix1(i));
  }

  public int getBondModelIndex(int i) {
    //legacy
    return modelSet.getBondModelIndex(i);
  }

  public int getBondArgb2(int i) {
    return g3d.getColixArgb(modelSet.getBondColix2(i));
  }

  public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
    return modelSet.getPolymerLeadMidPoints(modelIndex, polymerIndex);
  }

  // //////////////////////////////////////////////////////////////
  // stereo support
  // //////////////////////////////////////////////////////////////

  void setStereoMode(int stereoMode, String state) {
    //Eval -- ok; this is set specially
    global.stereoState = state;
    transformManager.setStereoMode(stereoMode);
    setBooleanProperty("greyscaleRendering",
        stereoMode > JmolConstants.STEREO_DOUBLE);
  }

  void setStereoMode(int[] twoColors, String state) {
    //Eval -- also set specially
    global.stereoState = state;
    transformManager.setStereoMode(twoColors);
    setBooleanProperty("greyscaleRendering", true);
  }

  int getStereoMode() {
    return transformManager.stereoMode;
  }

  float getStereoDegrees() {
    return transformManager.stereoDegrees;
  }

  private void setStereoDegrees(float degrees) {
    //Eval
    transformManager.setStereoDegrees(degrees);
  }

  // //////////////////////////////////////////////////////////////
  //
  // //////////////////////////////////////////////////////////////

  public boolean isJvm12orGreater() {
    return jvm12orGreater;
  }

  public String getOperatingSystemName() {
    return strOSName;
  }

  public String getJavaVendor() {
    return strJavaVendor;
  }

  public String getJavaVersion() {
    return strJavaVersion;
  }

  public Graphics3D getGraphics3D() {
    return g3d;
  }

  public boolean showModelSetDownload() {
    return true; //deprecated
  }

  // /////////////// getProperty /////////////

  public Object getProperty(String returnType, String infoType, String paramInfo) {
    return getProperty(returnType, infoType, (Object) paramInfo);
  }

  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    // accepts a BitSet paramInfo
    // return types include "JSON", "string", "readable", and anything else returns the Java object.
    return PropertyManager.getProperty(this, returnType, infoType, paramInfo);
  }

  String getModelExtract(Object atomExpression) {
    return fileManager.getFullPathName() + "\nJmol version " + getJmolVersion()
        + "\nEXTRACT: " + atomExpression + "\n"
        + modelSet.getModelExtract(getAtomBitSet(atomExpression));
  }

  public String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

  //////////////////////////////////////////////////

  void setModelVisibility() {
    //Eval -- ok - handled specially
    if (modelSet == null) //necessary for file chooser
      return;    
    modelSet.setModelVisibility();
  }

  void setFrameTitle(int modelIndex, String title) {
    modelSet.setFrameTitle(modelIndex, title);
  }
  
  String getFrameTitle(int modelIndex) {
    return modelSet.getFrameTitle(modelIndex);
  }
  
  boolean isTainted = true;

  void setTainted(boolean TF) {
    isTainted = TF && refreshing;
    axesAreTainted = TF && refreshing;
  }

  Point3f checkObjectClicked(int x, int y, int modifiers) {
    return modelSet.checkObjectClicked(x, y, modifiers);
  }

  boolean checkObjectHovered(int x, int y) {
    if (modelSet == null)
      return false;
    return modelSet.checkObjectHovered(x, y);
  }

  void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    modelSet.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers);
  }

  void rotateAxisAngleAtCenter(Point3f rotCenter, Vector3f rotAxis,
                               float degrees, float endDegrees, boolean isSpin,
                               boolean isSelected) {
    // Eval: rotate FIXED
    transformManager.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees,
        endDegrees, isSpin, isSelected);
    refresh(-1, "rotateAxisAngleAtCenter");
  }

  void rotateAboutPointsInternal(Point3f point1, Point3f point2,
                                 float nDegrees, float endDegrees,
                                 boolean isSpin, boolean isSelected) {
    // Eval: rotate INTERNAL
    transformManager.rotateAboutPointsInternal(point1, point2, nDegrees,
        endDegrees, false, isSpin, isSelected);
    refresh(-1, "rotateAxisAboutPointsInternal");
  }

  private void setPickingSpinRate(int rate) {
    //Eval
    if (rate < 1)
      rate = 1;
    global.pickingSpinRate = rate;
  }

  int getPickingSpinRate() {
    //PickingManager
    return global.pickingSpinRate;
  }

  public void startSpinningAxis(int atomIndex1, int atomIndex2,
                                boolean isClockwise) {
    // PickingManager.setAtomPicked  "set picking SPIN"
    Point3f pt1 = modelSet.getAtomAt(atomIndex1);
    Point3f pt2 = modelSet.getAtomAt(atomIndex2);
    startSpinningAxis(pt1, pt2, isClockwise);
  }

  public void startSpinningAxis(Point3f pt1, Point3f pt2, boolean isClockwise) {
    //Draw.checkObjectClicked  ** could be difficult
    //from draw object click
    if (getSpinOn()) {
      setSpinOn(false);
      return;
    }
    transformManager.rotateAboutPointsInternal(pt1, pt2,
        global.pickingSpinRate, Float.MAX_VALUE, isClockwise, true, false);
  }

  public Vector3f getModelDipole() {
    return modelSet.getModelDipole(getDisplayModelIndex());
  }

  public Vector3f calculateMolecularDipole() {
    return modelSet.calculateMolecularDipole(getDisplayModelIndex());
  }

  private void setDipoleScale(float scale) {
    //Eval
    global.dipoleScale = scale;
  }

  public float getDipoleScale() {
    return global.dipoleScale;
  }
  
  public void getAtomIdentityInfo(int atomIndex, Hashtable info) {
    modelSet.getAtomIdentityInfo(atomIndex, info);
  }

  void setDefaultLattice(Point3f ptLattice) {
    //Eval -- handled separately
    global.setDefaultLattice(ptLattice);
    global.setParameterValue("defaultLattice", Escape.escape(ptLattice));
  }

  Point3f getDefaultLattice() {
    return global.getDefaultLatticePoint();
  }

  BitSet getTaintedAtoms(byte type) {
    return modelSet.getTaintedAtoms(type);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    modelSet.setTaintedAtoms(bs, type);
  }

  public String getData(String atomExpression, String type) {
    String exp = "";
    if (type.toLowerCase().indexOf("property_") == 0)
      exp = "{selected}.label(\"%{" + type + "}\")";
    else if (type.equalsIgnoreCase("PDB"))
      //old crude
      exp = "{selected and not hetero}.label(\"ATOM  %5i %-4a%1A%3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
          + "+{selected and hetero}.label(\"HETATM%5i %-4a%1A%3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines";
    else if (type.equalsIgnoreCase("MOL"))
      exp = "\"line1\nline2\nline3\n\"+(\"\"+{selected}.size)%-3+(\"\"+{selected}.bonds.size)%-3+\"  0  0  0\n\""
          + "+{selected}.labels(\"%10.4x%10.4y%10.4z %-2e  0  0  0  0  0\").lines"
          + "+{selected}.bonds.labels(\"%3D1%3D2%3ORDER  0  0  0\").lines";
    else
      //if(type.equals("XYZ"))
      exp = "\"\" + {selected}.size + \"\n\n\"+{selected}.label(\"%-2e %10.5x %10.5y %10.5z\").lines";
    if (!atomExpression.equals("selected"))
      exp = TextFormat.simpleReplace(exp, "selected", atomExpression);
    return (String) evaluateExpression(exp);
  }

  public synchronized Object evaluateExpression(Object stringOrTokens) {
    return Eval.evaluateExpression(this, stringOrTokens);
  }
  
  public String getPdbData(BitSet bs) {
    if (bs == null)
      bs = getSelectionSet();
    return modelSet.getPdbAtomData(bs);
  }


  String getPdbData(int modelIndex, String type) {
    return modelSet.getPdbData(modelIndex, type);
  }

  public boolean isJmolDataFrame(int modelIndex) {
    return modelSet.isJmolDataFrame(modelIndex);
  }
  
  public boolean isJmolDataFrame() {
    return modelSet.isJmolDataFrame(repaintManager.currentModelIndex);
  }
  
  int getJmolDataFrameIndex(int modelIndex, String type) {
    return modelSet.getJmolDataFrameIndex(modelIndex, type);
  }

  void setJmolDataFrame(String type, int modelIndex, int dataIndex) {
    modelSet.setJmolDataFrame(type, modelIndex, dataIndex);  
  }
  
  void setFrameTitle(String title) {
    loadShape(JmolConstants.SHAPE_ECHO);
    modelSet.setFrameTitle(repaintManager.currentModelIndex, title);  
  }
  
  public String getFrameTitle() {
    return modelSet.getFrameTitle(repaintManager.currentModelIndex);
  }
  
  String getJmolFrameType(int modelIndex) {
    return modelSet.getJmolFrameType(modelIndex);
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return modelSet.getJmolDataSourceFrame(modelIndex);
  }
  
  void setAtomProperty(BitSet bs, int tok, int iValue, float fValue, float[] values) {
    modelSet.setAtomProperty(bs, tok, iValue, fValue, values);
    switch(tok) {
    case Token.atomX:
    case Token.atomY:
    case Token.atomZ:
    case Token.fracX:
    case Token.fracY:
    case Token.fracZ:
      refreshMeasures();
    }    
  }
 
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    //Frame equivalent used in DATA "coord set"
    modelSet.setAtomCoord(atomIndex, x, y, z);
    // no measure refresh here -- because it may involve hundreds of calls
  }

  void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    modelSet.setAtomCoord(bs, tokType, xyzValues);
    refreshMeasures();
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    modelSet.setAtomCoordRelative(atomIndex, x, y, z);
    // no measure refresh here -- because it may involve hundreds of calls
  }

  void setAtomCoordRelative(Point3f offset) {
    //Eval
    modelSet.setAtomCoordRelative(offset, selectionManager.bsSelection);
    refreshMeasures();
  }

  void setRotateSelected(boolean TF) {
    transformManager.setRotateSelected(TF);
    refreshMeasures();
  }

  void setRotateMolecule(boolean TF) {
    transformManager.setRotateMolecule(TF);
    refreshMeasures();
  }

  void setAllowRotateSelected(boolean TF) {
    global.allowRotateSelected = TF;
  }

  boolean allowRotateSelected() {
    return global.allowRotateSelected;
  }

  void invertSelected(Point3f pt, BitSet bs) {
    //Eval
    modelSet.invertSelected(pt, null, bs);
    refreshMeasures();
  }

  void invertSelected(Point3f pt, Point4f plane) {
    //Eval
    modelSet.invertSelected(pt, plane, selectionManager.bsSelection);
    refreshMeasures();
  }

  void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate,
                      boolean fullMolecule, Point3f center, boolean isInternal) {
    modelSet.rotateSelected(mNew, matrixRotate,
        selectionManager.bsSelection, fullMolecule, center, isInternal);
    refreshMeasures();
  }

  public void refreshMeasures() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "refresh", null);
  }
  
  void setDynamicMeasurements(boolean TF) { //deprecated; unnecessary
    global.dynamicMeasurements = TF;
  }

  public boolean getDynamicMeasurements() {
    return global.dynamicMeasurements;
  }

  /**
   *  fills an array with data -- if nX < 0 and this would involve JavaScript, then this reads a full 
   *  set of Double[][] in one function call. Otherwise it reads the values using
   *  individual function calls, which each return Double. 
   *  
   *  If the functionName begins with "file:" then data are read from 
   *  a file specified after the colon. The sign of nX is not relevant in that case.
   *  The file may contain mixed numeric and non-numeric values;
   *  the non-numeric values will be skipped by Parser.parseFloatArray 
   *   
   * @param functionName
   * @param nX
   * @param nY
   * @return    nX by nY array of floating values
   */
  public float[][] functionXY(String functionName, int nX, int nY) {
    if (functionName.indexOf("file:") < 0)
      return statusManager.functionXY(functionName, nX, nY);
    String data = getFileAsString(functionName.substring(5));
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    float[] f = new float[nX * nY];
    Parser.parseFloatArray(data, null, f);
    float[][] fdata = new float[nX][nY];
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        fdata[i][j] = f[n++];
    return fdata;
  }

  String eval(String strEval) {
    return statusManager.eval(strEval);
  }

  private void setHelpPath(String url) {
    //Eval 
    global.helpPath = url;
  }

  void getHelp(String what) {
    if (what.length() > 0 && what.indexOf("?") != 0
        && global.helpPath.indexOf("?") < 0)
      what = "?search=" + what;
    showUrl(global.helpPath + what);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  /*
   * Moved from the consoles to viewer, since this could be of general
   * interest, it's more a property of Eval/Viewer, and the consoles 
   * are really just a mechanism for getting user input and sending 
   * results, not saving a history of it all. Ultimately I hope to integrate
   * the mouse picking and possibly periodic updates of position 
   * into this history to get a full history. We'll see!  BH 9/2006
   * 
   */

  /**
   * Adds one or more commands to the command history
   * @param command  the command to add
   */
  void addCommand(String command) {
    commandHistory.addCommand(command);
  }

  /**
   * Removes one command from the command history
   * @return command removed
   */
  String removeCommand() {
    return commandHistory.removeCommand();
  }

  /**
   * Options include:
   * ;  all                             n == Integer.MAX_VALUE
   * ;  n prev                          n >= 1
   * ;  next                            n == -1
   * ;  set max to -2 - n               n <= -3
   * ;  just clear                      n == -2
   * ;  clear and turn off; return ""   n == 0
   * ;  clear and turn on; return ""    n == Integer.MIN_VALUE;
   *     
   * @param howFarBack  number of lines (-1 for next line)
   * @return one or more lines of command history
   */
  public String getSetHistory(int howFarBack) {
    return commandHistory.getSetHistory(howFarBack);
  }

  // ///////////////////////////////////////////////////////////////
  // image export
  // ///////////////////////////////////////////////////////////////

  private void createImage(String type_name) { // or script now
    int quality, width, height;
    if (type_name == null)
      return;
    if (type_name.length() == 0)
      type_name = "JPG:jmol.jpg";
    if (type_name.indexOf(":") < 0)
      type_name += ":jmol.jpg";
    int i = type_name.indexOf(":");
    String type = type_name.substring(0, i);
    String file = type_name.substring(i + 1);
    String swidth = "-1";
    String sheight = "-1";
    String squality = "75";
    i = file.indexOf('\t');
    if (i > 0) {
      swidth = file.substring(i + 1);
      file = file.substring(0, i);
    }
    i = swidth.indexOf('\t');
    if (i > 0) {
      sheight = swidth.substring(i + 1);
      swidth = swidth.substring(0, i);
    }
    i = sheight.indexOf('\t');
    if (i > 0) {
      squality = sheight.substring(i + 1);
      sheight = sheight.substring(0, i);
    }
    try {
      width = Integer.parseInt(swidth);
      height = Integer.parseInt(sheight);
      quality = Integer.parseInt(squality);
      createImage(file, type, quality, width, height);
    } catch (Exception e) {
      System.out.println("error processing write request: " + type_name);
    }
  }

  private float imageFontScaling = 1;
  public float getImageFontScaling() {
    return imageFontScaling;
  }
  
  public void createImage(String file, Object type_or_text_or_bytes, int quality,
                          int width, int height) {
    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    if (quality != Integer.MIN_VALUE) {
      resizeImage(width, height, true, false, false);
      setModelVisibility();
    }
    try {
      statusManager.createImage(file, type_or_text_or_bytes, quality);
    } catch (Exception e) {
      Logger.error("Error creating image: " + e.getMessage());
    }
    if (quality != Integer.MIN_VALUE) {
      resizeImage(saveWidth, saveHeight, true, false, true);
    }
  }

  private void setImageFontScaling(int width, int height) {
    float screenDimNew = (global.zoomLarge == (height > width) ? height : width);
    imageFontScaling = screenDimNew / getScreenDim();
  }
  
  private boolean syncingScripts = false;
  private boolean syncingMouse = false;
  
  private void setSyncTarget(int mode, boolean TF) {
    switch (mode) {
    case 0:
      syncingMouse = TF;
      break;
    case 1:
      syncingScripts = TF;
      break;
    }
    // if turning both off, sync the orientation now
    if (!syncingScripts && !syncingMouse)
      refresh(-1, "set sync");
  }
  
  public void syncScript(String script, String applet) {
    boolean isAll = ("*".equals(applet));
    boolean allButMe = (">".equals(applet));
    boolean disableSend = ("~".equals(applet));
    boolean justMe = disableSend || (".".equals(applet));
    //System.out.println(getHtmlName() + " syncscript " + script + " --- applet " + applet);
    //null same as ">" -- "all others"
    if (!justMe) {
      statusManager.syncSend(script, (isAll || allButMe ? null : applet));
      if (!isAll)
        return;
    }
    if (script.equalsIgnoreCase("on")) {
      statusManager.setSyncDriver(StatusManager.SYNC_DRIVER);
      return;
    }
    if (script.equalsIgnoreCase("off")) {
      statusManager.setSyncDriver(StatusManager.SYNC_OFF);
      return;
    }
    if (script.equalsIgnoreCase("slave")) {
      statusManager.setSyncDriver(StatusManager.SYNC_SLAVE);
      return;
    }
    int syncMode = statusManager.getSyncMode();
    if (syncMode == StatusManager.SYNC_OFF)
      return;
    if (syncMode != StatusManager.SYNC_DRIVER)
      disableSend = false;
    if (Logger.debugging)
      Logger.debug(htmlName + " syncing with script: " + script);
    //driver is being positioned by another driver -- don't pass on the change
    //driver is being positioned by a mouse movement
    //format is from above refresh(2, xxx) calls
    //Mouse: [CommandName] [value1] [value2]
    if (disableSend)
      statusManager.setSyncDriver(StatusManager.SYNC_DISABLE);
    if (script.indexOf("Mouse: ") != 0) {
      evalStringQuiet(script, true, false);
      return;
    }
    String[] tokens = Parser.getTokens(script);
    String key = tokens[1];
    switch (tokens.length) {
    case 3:
      if (key.equals("zoomByFactor"))
        zoomByFactor(Parser.parseFloat(tokens[2]));
      else if (key.equals("zoomBy"))
        zoomBy(Parser.parseInt(tokens[2]));
      else if (key.equals("rotateZBy"))
        rotateZBy(Parser.parseInt(tokens[2]));
      break;
    case 4:
      if (key.equals("rotateXYBy"))
        rotateXYBy(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      else if (key.equals("translateXYBy"))
        translateXYBy(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      else if (key.equals("rotateMolecule"))
        rotateMolecule(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      break;
    }
    if (disableSend)
      setSyncDriver(StatusManager.SYNC_ENABLE);
  }

  void setSyncDriver(int mode) {
    statusManager.setSyncDriver(mode);
  }

  public float[] getPartialCharges() {
    return modelSet.getPartialCharges();
  }

  void setProteinType(byte iType, BitSet bs) {
    modelSet.setProteinType(bs == null ? selectionManager.bsSelection : bs,
        iType);
  }

  /*  
   void debugStack(String msg) {
   //what's the right way to do this?
   try {
   Logger.error(msg);
   String t = null;
   t.substring(3);
   } catch (Exception e) {
   e.printStackTrace();
   }
   }
   */

  void setMenu(String fileOrText, boolean isFile) {
    if (isFile)
      Logger.info("Setting menu " + (fileOrText.length() == 0 ? "to Jmol defaults" : "from file " + fileOrText));
    if (fileOrText.length() == 0)
      fileOrText = null;
    else if (isFile)
      fileOrText = fileManager.getFileAsString(fileOrText);
    statusManager.setCallbackFunction("menu", fileOrText);
  }
  
  String getMenu() {
    return statusManager.eval("_GET_MENU");
  }

  void setListVariable(String name, Token value) {
    global.setListVariable(name, value);
  }

  Object getListVariable(String name, Object value) {
    return global.getListVariable(name, value);
  }
  
  
  public Point3f getBondPoint3f1(int i) {
    //legacy -- no calls
    return modelSet.getBondAtom1(i);
  }

  public Point3f getBondPoint3f2(int i) {
    //legacy -- no calls
    return modelSet.getBondAtom2(i);
  }

  float getVibrationCoord(int atomIndex, char c) {
    return modelSet.getVibrationCoord(atomIndex, c);
  }

  public Vector3f getVibrationVector(int atomIndex) {
    return modelSet.getVibrationVector(atomIndex);
  }

  public int getVanderwaalsMar(int i) {
    return (dataManager.defaultVdw == JmolConstants.VDW_USER ?
        dataManager.userVdwMars[i] 
        : JmolConstants.getVanderwaalsMar(i, dataManager.defaultVdw));
  }
  
  public int getVanderwaalsMar(int i, int iMode) {
    if (iMode == JmolConstants.VDW_USER && dataManager.bsUserVdws == null)
      iMode = dataManager.defaultVdw;
    return (iMode == JmolConstants.VDW_USER ?
        dataManager.userVdwMars[i] : JmolConstants.getVanderwaalsMar(i, iMode));
  }
  
  void setDefaultVdw(String mode) {
    dataManager.setDefaultVdw(mode);
    global.setParameterValue("defaultVDW", getDefaultVdw(Integer.MIN_VALUE));
  }
  
  String getDefaultVdw(int iMode) {
    return dataManager.getDefaultVdw(iMode, null);
  }

  public int deleteAtoms(BitSet bs, boolean fullModels) {
    clearMinimization();
    if (!fullModels)
      return selectionManager.deleteAtoms(bs);
    fileManager.addLoadScript("zap " + Escape.escape(bs));
    setCurrentModelIndex(0, false);
    repaintManager.setAnimationOn(false);
    BitSet bsDeleted = modelSet.deleteAtoms(bs, true);
    setAnimationRange(0, 0);
    eval.deleteAtomsInVariables(bsDeleted);
    repaintManager.clear();
    repaintManager.initializePointers(1);
    if (getModelCount() > 1)
      setCurrentModelIndex(-1, true);
    hoverAtomIndex = -1;
    setStatusFileLoaded(0, null, null, null, null);
    refreshMeasures();
    return BitSetUtil.cardinalityOf(bsDeleted);
  }
  
  public BitSet getDeletedAtoms() {
    return selectionManager.bsDeleted;
  }

  public char getQuaternionFrame() {
    return global.quaternionFrame.charAt(0);
  }
  
  void setQuaternionFrame(String qType) {
    global.quaternionFrame = "" + (qType.toLowerCase()+"c").charAt(0);
  }

}
