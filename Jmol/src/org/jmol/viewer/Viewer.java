/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-02-02 22:24:37 -0600 (Sun, 02 Feb 2014) $
 * $Revision: 19253 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javajs.J2SIgnoreImport;
import javajs.api.GenericCifDataParser;
import javajs.api.GenericMenuInterface;
import javajs.api.GenericMouseInterface;
import javajs.api.GenericPlatform;
import javajs.api.GenericZipTools;
import javajs.api.PlatformViewer;
import javajs.awt.Dimension;
import javajs.awt.Font;
import javajs.util.CU;
import javajs.util.DF;
import javajs.util.JSJSONParser;
import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.Rdr;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAnnotationParser;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolBioResolver;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolDataManager;
import org.jmol.api.JmolJSpecView;
import org.jmol.api.JmolNMRInterface;
import org.jmol.api.JmolParallelProcessor;
import org.jmol.api.JmolPropertyManager;
import org.jmol.api.JmolRendererInterface;
import org.jmol.api.JmolRepaintManager;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolScriptEvaluator;
import org.jmol.api.JmolScriptFunction;
import org.jmol.api.JmolScriptManager;
import org.jmol.api.JmolSelectionListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.api.MinimizerInterface;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.FIL;
import org.jmol.c.STER;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementData;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Orientation;
import org.jmol.modelset.StateScript;
import org.jmol.modelset.TickInfo;
import org.jmol.script.SV;
import org.jmol.script.ScriptContext;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.shape.Measures;
import org.jmol.shape.Shape;
import org.jmol.thread.TimeoutThread;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.CommandHistory;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.GData;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Rectangle;
import org.jmol.util.TempArray;
import org.jmol.viewer.binding.Binding;

/*
 * 
 * ****************************************************************
 * The JmolViewer can be used to render client molecules. Clients implement the
 * JmolAdapter. JmolViewer uses this interface to extract information from the
 * client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.5+ virtual machines. The 3d graphics rendering
 * package is a software implementation of a z-buffer. It does not use Java3D
 * and does not use Graphics2D from Java 1.2.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea
 * being that applet.getProperty("jmolViewer") returns this Viewer object,
 * allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 * 
 * This vwr can also be used with JmolData.jar, which is a 
 * frameless version of Jmol that can be used to batch-process
 * scripts from the command line. No shapes, no labels, no export
 * to JPG -- just raw data checking and output. 
 * 
 * 
 * NOSCRIPTING option: 2/2013
 * 
 * This option provides a smaller load footprint for JavaScript JSmol 
 * and disallows:
 * 
 *   scripting
 *   modelKitMode
 *   slabbing of read JVXL files
 *   calculate hydrogens
 *   
 * 
 * ****************************************************************
 */

@J2SIgnoreImport({ Runtime.class })
public class Viewer extends JmolViewer implements AtomDataServer,
    PlatformViewer {

  public boolean testAsync;// = true; // testing only

  static {
    /**
     * allows customization of Viewer -- not implemented in JSmol.
     * 
     * @j2sNative
     * 
     *            self.Jmol && Jmol.extend && Jmol.extend("vwr",
     *            org.jmol.viewer.Viewer.prototype);
     * 
     */
    {
    }
  }

  @Override
  protected void finalize() throws Throwable {
    if (Logger.debugging)
      Logger.debug("vwr finalize " + this);
    super.finalize();
  }

  // these are all private now so we are certain they are not
  // being accesed by any other classes

  public boolean autoExit = false;
  public boolean haveDisplay = false;

  public boolean isJS, isWebGL;
  public boolean isSingleThreaded;
  public boolean queueOnHold = false;

  public String fullName = "";
  public static String appletDocumentBase = "";
  public static String appletCodeBase = "";
  public static String appletIdiomaBase;

  public static String jsDocumentBase = "";

  public enum ACCESS {
    NONE, READSPT, ALL
  }

  public Object compiler;
  public Map<String, Object> definedAtomSets;
  public ModelSet ms;
  public FileManager fm;

  public boolean isApplet, isJNLP;

  public boolean isSyntaxAndFileCheck = false;
  public boolean isSyntaxCheck = false;
  public boolean listCommands = false;
  boolean mustRender = false;

  public String htmlName = "";
  public String appletName = "";

  public int tryPt;

  private String insertedCommand = "";

  public void setInsertedCommand(String strScript) {
    insertedCommand = strScript;
  }

  public GData gdata;
  public Object html5Applet; // j2s only

  public ActionManager acm;
  public AnimationManager am;
  public ColorManager cm;
  JmolDataManager dm;
  public ShapeManager shm;
  public SelectionManager slm;
  JmolRepaintManager rm;
  public GlobalSettings g;
  public StatusManager sm;
  public TransformManager tm;

  public static String strJavaVendor = "Java: "
      + System.getProperty("java.vendor", "j2s");
  public static String strOSName = System.getProperty("os.name", "");
  public static String strJavaVersion = "Java "
      + System.getProperty("java.version", "");

  String syncId = "";
  String logFilePath = "";

  private boolean allowScripting;
  public boolean isPrintOnly;
  public boolean isSignedApplet = false;
  private boolean isSignedAppletLocal = false;
  private boolean isSilent;
  private boolean multiTouch;
  public boolean noGraphicsAllowed;
  private boolean useCommandThread = false;

  private String commandOptions;
  public Map<String, Object> vwrOptions;
  public Object display;
  private JmolAdapter modelAdapter;
  private ACCESS access;
  private CommandHistory commandHistory;

  public ModelManager mm;
  public StateManager stm;
  private JmolScriptManager scm;
  public JmolScriptEvaluator eval;
  private TempArray tempArray;

  public boolean allowArrayDotNotation;
  public boolean async;

  private static String version_date;

  public static String getJmolVersion() {
    return (version_date == null ? version_date = JC.version + "  " + JC.date
        : version_date);
  }

  /**
   * old way...
   * 
   * @param display
   * @param modelAdapter
   * @param fullName
   * @param documentBase
   * @param codeBase
   * @param commandOptions
   * @param statusListener
   * @param implementedPlatform
   * @return JmolViewer object
   */
  protected static JmolViewer allocateViewer(Object display,
                                             JmolAdapter modelAdapter,
                                             String fullName, URL documentBase,
                                             URL codeBase,
                                             String commandOptions,
                                             JmolStatusListener statusListener,
                                             GenericPlatform implementedPlatform) {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("display", display);
    info.put("adapter", modelAdapter);
    info.put("statusListener", statusListener);
    info.put("platform", implementedPlatform);
    info.put("options", commandOptions);
    info.put("fullName", fullName);
    info.put("documentBase", documentBase);
    info.put("codeBase", codeBase);
    return new Viewer(info);
  }

  final Dimension dimScreen;
  final Lst<String> actionStates;
  final Lst<String> actionStatesRedo;
  VDW defaultVdw;

  public RadiusData rd;
  public Map<Object, Object> chainMap;
  private Lst<String> chainList;
  private String errorMessage;
  private String errorMessageUntranslated;
  private double privateKey;
  private boolean dataOnly;

  /**
   * new way...
   * 
   * @param info
   *        "display" "adapter" "statusListener" "platform" "options" "fullName"
   *        "documentBase" "codeBase" "multiTouch" [options] "noGraphics"
   *        "printOnly" "previewOnly" "debug" "applet" "signedApplet"
   *        "appletProxy" "useCommandThread" "platform" [option]
   *        "backgroundTransparent" "exit" "listCommands" "check" "checkLoad"
   *        "silent" "access:READSPT" "access:NONE" "menuFile"
   *        "headlessMaxTimeMs" "headlessImage" "isDataOnly" "async"
   **/

  public Viewer(Map<String, Object> info) {
    commandHistory = new CommandHistory();
    dimScreen = new Dimension(0, 0);
    rd = new RadiusData(null, 0, null, null);
    defaultVdw = VDW.JMOL;
    localFunctions = new Hashtable<String, JmolScriptFunction>();
    privateKey = Math.random();
    actionStates = new Lst<String>();
    actionStatesRedo = new Lst<String>();
    chainMap = new Hashtable<Object, Object>();
    chainList = new Lst<String>();
    setOptions(info);
  }

  public boolean haveAccess(ACCESS a) {
    // disables WRITE, LOAD file:/, set logFile 
    // command line -g and -w options ARE available for final writing of image
    return access == a;
  }

  @Override
  public JmolAdapter getModelAdapter() {
    return (modelAdapter == null ? modelAdapter = new SmarterJmolAdapter()
        : modelAdapter);
  }

  @Override
  public BS getSmartsMatch(String smarts, BS bsSelected) throws Exception {
    if (bsSelected == null)
      bsSelected = bsA();
    return getSmilesMatcher().getSubstructureSet(smarts, ms.at, ms.ac,
        bsSelected, JC.SMILES_TYPE_SMARTS);
  }

  @SuppressWarnings({ "unchecked", "null", "unused" })
  public void setOptions(Map<String, Object> info) {
    // can be deferred
    vwrOptions = info;
    // could be a Component, or could be a JavaScript class
    // use allocateViewer
    if (Logger.debugging) {
      Logger.debug("Viewer constructor " + this);
    }
    modelAdapter = (JmolAdapter) info.get("adapter");
    JmolStatusListener statusListener = (JmolStatusListener) info
        .get("statusListener");
    fullName = (String) info.get("fullName");
    if (fullName == null)
      fullName = "";
    Object o = info.get("codePath");
    if (o == null)
      o = "../java/";
    appletCodeBase = o.toString();
    appletIdiomaBase = appletCodeBase.substring(0,
        appletCodeBase.lastIndexOf("/", appletCodeBase.length() - 2) + 1)
        + "idioma";
    o = info.get("documentBase");
    appletDocumentBase = (o == null ? "" : o.toString());
    o = info.get("options");
    commandOptions = (o == null ? "" : o.toString());

    if (info.containsKey("debug") || commandOptions.indexOf("-debug") >= 0)
      Logger.setLogLevel(Logger.LEVEL_DEBUG);
    if (isApplet && info.containsKey("maximumSize"))
      setMaximumSize(((Integer) info.get("maximumSize")).intValue());

    isJNLP = checkOption2("isJNLP", "-jnlp");
    if (isJNLP)
      Logger.info("setting JNLP mode TRUE");

    isSignedApplet = isJNLP || checkOption2("signedApplet", "-signed");
    isApplet = isSignedApplet || checkOption2("applet", "-applet");
    allowScripting = !checkOption2("noscripting", "-noscripting");
    int i = fullName.indexOf("__");
    htmlName = (i < 0 ? fullName : fullName.substring(0, i));
    appletName = PT.split(htmlName + "_", "_")[0];
    syncId = (i < 0 ? "" : fullName.substring(i + 2, fullName.length() - 2));
    access = (checkOption2("access:READSPT", "-r") ? ACCESS.READSPT
        : checkOption2("access:NONE", "-R") ? ACCESS.NONE : ACCESS.ALL);
    isPreviewOnly = info.containsKey("previewOnly");
    if (isPreviewOnly)
      info.remove("previewOnly"); // see FilePreviewPanel
    isPrintOnly = checkOption2("printOnly", "-p");
    dataOnly = checkOption2("isDataOnly", "\0");
    autoExit = checkOption2("exit", "-x");
    o = info.get("platform");
    String platform = "unknown";
    if (o == null) {
      o = (commandOptions.contains("platform=") ? commandOptions
          .substring(commandOptions.indexOf("platform=") + 9)
          : "org.jmol.awt.Platform");
      // note that this must be the last option if give in commandOptions
    }
    if (o instanceof String) {
      platform = (String) o;
      isWebGL = (platform.indexOf(".awtjs.") >= 0);
      isJS = isWebGL || (platform.indexOf(".awtjs2d.") >= 0);
      async = !dataOnly && !autoExit
          && (testAsync || isJS && info.containsKey("async"));
      Object applet = null;
      String javaver = "?";
      /**
       * @j2sNative
       * 
       *            if(self.Jmol) { applet =
       *            Jmol._applets[this.htmlName.split("_object")[0]]; javaver =
       *            Jmol._version; }
       * 
       * 
       */
      {
        javaver = null;
      }
      if (javaver != null) {
        html5Applet = applet;
        strJavaVersion = javaver;
        strJavaVendor = "Java2Script " + (this.isWebGL ? "(WebGL)" : "(HTML5)");
      }
      o = Interface.getInterface(platform, this, "setOptions");
    }
    apiPlatform = (GenericPlatform) o;
    display = info.get("display");
    isSingleThreaded = apiPlatform.isSingleThreaded();
    noGraphicsAllowed = checkOption2("noDisplay", "-n");
    //System.out.println("nographics " + noGraphicsAllowed);
    headless = apiPlatform.isHeadless();
    haveDisplay = (isWebGL || display != null && !noGraphicsAllowed
        && !headless && !dataOnly);
    noGraphicsAllowed &= (display == null);
    //System.out.println("nographics " + noGraphicsAllowed);
    headless |= noGraphicsAllowed;
    //System.out.println("headless " + headless + commandOptions);
    if (haveDisplay) {
      mustRender = true;
      multiTouch = checkOption2("multiTouch", "-multitouch");
      /**
       * @j2sNative
       * 
       *            if (!this.isWebGL) this.display =
       *            document.getElementById(this.display);
       */
      {
      }
    } else {
      display = null;
    }
    apiPlatform.setViewer(this, display);
    o = info.get("graphicsAdapter");
    if (o == null && !isWebGL)
      o = Interface.getOption("g3d.Graphics3D", this, "setOptions");
    gdata = (o == null && (isWebGL || !isJS) ? new GData() : (GData) o);
    // intentionally throw an error here to restart the JavaScript async process
    gdata.initialize(this, apiPlatform);

    stm = new StateManager(this);
    cm = new ColorManager(this, gdata);
    sm = new StatusManager(this);
    boolean is4D = info.containsKey("4DMouse");
    tm = TransformManager.getTransformManager(this, Integer.MAX_VALUE, 0, is4D);
    slm = new SelectionManager(this);
    if (haveDisplay) {
      // must have language by now, as ActionManager uses GT._()
      acm = (multiTouch ? (ActionManager) Interface.getOption(
          "multitouch.ActionManagerMT", null, null) : new ActionManager());
      acm.setViewer(this,
          commandOptions + "-multitouch-" + info.get("multiTouch"));
      mouse = apiPlatform.getMouseManager(privateKey, display);
      if (multiTouch && !checkOption2("-simulated", "-simulated"))
        apiPlatform.setTransparentCursor(display);
    }
    mm = new ModelManager(this);
    shm = new ShapeManager(this);
    tempArray = new TempArray();
    am = new AnimationManager(this);
    o = info.get("repaintManager");
    if (o == null)
      o = Interface.getOption("render.RepaintManager", this, "setOptions");
    if (isJS || o != null && !o.equals(""))
      (rm = (JmolRepaintManager) o).set(this, shm);
    // again we through a JS error if in async mode
    ms = new ModelSet(this, null);
    initialize(true);
    fm = new FileManager(this);
    definedAtomSets = new Hashtable<String, Object>();
    setJmolStatusListener(statusListener);
    if (isApplet) {
      Logger.info("vwrOptions: \n" + Escape.escapeMap(vwrOptions));
      // Java only, because Signed applet can't find correct path when local.
      String path = (String) vwrOptions.get("documentLocation");
      if (!isJS && path != null && path.startsWith("file:/")) {
        path = path.substring(0, path.substring(0, (path + "?").indexOf("?"))
            .lastIndexOf("/"));
        Logger.info("setting current directory to " + path);
        cd(path);
      }
      path = appletDocumentBase;
      i = path.indexOf("#");
      if (i >= 0)
        path = path.substring(0, i);
      i = path.lastIndexOf("?");
      if (i >= 0)
        path = path.substring(0, i);
      i = path.lastIndexOf("/");
      if (i >= 0)
        path = path.substring(0, i);
      jsDocumentBase = path;
      fm.setAppletContext(appletDocumentBase);
      String appletProxy = (String) info.get("appletProxy");
      if (appletProxy != null)
        setStringProperty("appletProxy", appletProxy);
      if (isSignedApplet) {
        logFilePath = PT.rep(appletCodeBase, "file://", "");
        logFilePath = PT.rep(logFilePath, "file:/", "");
        if (logFilePath.indexOf("//") >= 0)
          logFilePath = null;
        else
          isSignedAppletLocal = true;
      } else if (!isJS) {
        logFilePath = null;
      }
      new GT(this, (String) info.get("language"));
      // deferred here so that language is set
      if (isJS)
        acm.createActions();
    } else {
      // not an applet -- used to pass along command line options
      gdata
          .setBackgroundTransparent(checkOption2("backgroundTransparent", "-b"));
      isSilent = checkOption2("silent", "-i");
      if (isSilent)
        Logger.setLogLevel(Logger.LEVEL_WARN); // no info, but warnings and
      if (headless && !isSilent)
        Logger.info("Operating headless display=" + display
            + " nographicsallowed=" + noGraphicsAllowed);
      // errors
      isSyntaxAndFileCheck = checkOption2("checkLoad", "-C");
      isSyntaxCheck = isSyntaxAndFileCheck || checkOption2("check", "-c");
      listCommands = checkOption2("listCommands", "-l");
      cd(".");
      if (headless) {
        headlessImageParams = (Map<String, Object>) info.get("headlessImage");
        o = info.get("headlistMaxTimeMs");
        if (o == null)
          o = Integer.valueOf(60000);
        setTimeout("" + Math.random(), ((Integer) o).intValue(), "exitJmol");
      }
    }
    useCommandThread = !headless
        && checkOption2("useCommandThread", "-threaded");
    setStartupBooleans();
    setIntProperty("_nProcessors", nProcessors);
    /*
     * Logger.info("jvm11orGreater=" + jvm11orGreater + "\njvm12orGreater=" +
     * jvm12orGreater + "\njvm14orGreater=" + jvm14orGreater);
     */
    if (!isSilent) {
      Logger.info(JC.copyright
          + "\nJmol Version: "
          + getJmolVersion()
          + "\njava.vendor: "
          + strJavaVendor
          + "\njava.version: "
          + strJavaVersion
          + "\nos.name: "
          + strOSName
          + "\nAccess: "
          + access
          + "\nmemory: "
          + getP("_memory")
          + "\nprocessors available: "
          + nProcessors
          + "\nuseCommandThread: "
          + useCommandThread
          + (!isApplet ? "" : "\nappletId:" + htmlName
              + (isSignedApplet ? " (signed)" : "")));
    }
    if (allowScripting)
      getScriptManager();
    zap(false, true, false); // here to allow echos
    g.setO("language", GT.getLanguage());
    g.setO("_hoverLabel", hoverLabel);
    stm.setJmolDefaults();
    // this code will be shared between Jmol 14.0 and 14.1
    Elements.covalentVersion = Elements.RAD_COV_BODR_2014_02_22;
    allowArrayDotNotation = true;
  }

  public void setDisplay(Object canvas) {
    // used by JSmol/HTML5 when a canvas is resized
    display = canvas;
    apiPlatform.setViewer(this, canvas);
  }

  public MeasurementData newMeasurementData(String id, Lst<Object> points) {
    return ((MeasurementData) Interface.getInterface(
        "org.jmol.modelset.MeasurementData", this, "script")).init(id, this,
        points);
  }

  private JmolDataManager getDataManager() {
    return (dm == null ? (dm = ((JmolDataManager) Interface.getInterface(
        "org.jmol.viewer.DataManager", this, "script")).set(this)) : dm);
  }

  private JmolScriptManager getScriptManager() {
    if (allowScripting && scm == null) {
      scm = (JmolScriptManager) Interface.getInterface(
          "org.jmol.script.ScriptManager", this, "setOptions");
      if (isJS && scm == null)
        throw new NullPointerException();
      if (scm == null) {
        allowScripting = false;
        return null;
      }
      eval = scm.setViewer(this);
      if (useCommandThread)
        scm.startCommandWatcher(true);
    }
    return scm;
  }

  private boolean checkOption2(String key1, String key2) {
    return (vwrOptions.containsKey(key1) || commandOptions.indexOf(key2) >= 0);
  }

  public boolean isPreviewOnly;

  /**
   * determined by GraphicsEnvironment.isHeadless() from java
   * -Djava.awt.headless=true
   * 
   * disables command threading
   * 
   * disables DELAY, TIMEOUT, PAUSE, LOOP, GOTO, SPIN <rate>, ANIMATION ON
   * 
   * turns SPIN <rate> <end> into just ROTATE <end>
   */

  public boolean headless;
  
  private void setStartupBooleans() {
    setBooleanProperty("_applet", isApplet);
    setBooleanProperty("_JSpecView".toLowerCase(), false);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_headless", headless);
    setStringProperty("_restrict", "\"" + access + "\"");
    setBooleanProperty("_useCommandThread", useCommandThread);
  }

  public String getExportDriverList() {
    return (haveAccess(ACCESS.ALL) ? (String) g.getParameter("exportDrivers",
        true) : "");
  }

  /**
   * end of life for this viewer
   */
  @Override
  public void dispose() {
    gRight = null;
    if (mouse != null) {
      acm.dispose();
      mouse.dispose();
      mouse = null;
    }
    clearScriptQueue();
    clearThreads();
    haltScriptExecution();
    if (scm != null)
      scm.clear(true);
    gdata.destroy();
    if (jmolpopup != null)
      jmolpopup.jpiDispose();
    if (modelkitPopup != null)
      modelkitPopup.jpiDispose();
    try {
      if (appConsole != null) {
        appConsole.dispose();
        appConsole = null;
      }
      if (scriptEditor != null) {
        scriptEditor.dispose();
        scriptEditor = null;
      }
    } catch (Exception e) {
      // ignore -- Disposal was interrupted only in Eclipse
    }
  }

  public void reset(boolean includingSpin) {
    // Eval.reset()
    // initializeModel
    ms.calcBoundBoxDimensions(null, 1);
    axesAreTainted = true;
    tm.homePosition(includingSpin);
    if (ms.setCrystallographicDefaults())
      stm.setCrystallographicDefaults();
    else
      setAxesMode(T.axeswindow);
    prevFrame = Integer.MIN_VALUE;
    if (!tm.spinOn)
      setSync();
  }

  @Override
  public void homePosition() {
    evalString("reset spin");
  }

  /*
   * final Hashtable imageCache = new Hashtable();
   * 
   * void flushCachedImages() { imageCache.clear();
   * GData.flushCachedColors(); }
   */

  // ///////////////////////////////////////////////////////////////
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

  public void initialize(boolean clearUserVariables) {
    g = new GlobalSettings(this, g, clearUserVariables);
    setStartupBooleans();
    setWidthHeightVar();
    if (haveDisplay) {
      g.setB("_is2D", isJS && !isWebGL);
      g.setB("_multiTouchClient", acm.isMTClient());
      g.setB("_multiTouchServer", acm.isMTServer());
    }
    cm.setDefaultColors(false);
    setObjectColor("background", "black");
    setObjectColor("axis1", "red");
    setObjectColor("axis2", "green");
    setObjectColor("axis3", "blue");

    // transfer default global settings to managers and g3d

    am.setAnimationOn(false);
    am.setAnimationFps(g.animationFps);
    sm.allowStatusReporting = g.statusReporting;
    setBooleanProperty("antialiasDisplay", g.antialiasDisplay);
    stm.resetLighting();
    tm.setDefaultPerspective();
  }

  public void setWidthHeightVar() {
    g.setI("_width", dimScreen.width);
    g.setI("_height", dimScreen.height);
  }

  void saveModelOrientation() {
    ms.saveModelOrientation(am.cmi, stm.getOrientation());
  }

  void restoreModelOrientation(int modelIndex) {
    Orientation o = ms.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, true);
  }

  void restoreModelRotation(int modelIndex) {
    Orientation o = ms.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, false);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  /**
   * This method is only called by JmolGLmol applet._refresh();
   * 
   * @return enough data to update a WebGL view
   * 
   */
  @SuppressWarnings("unused")
  public Object getGLmolView() {
    TransformManager tm = this.tm;
    T3 center = tm.fixedRotationCenter;
    Quat q = tm.getRotationQ();
    float xtrans = tm.xTranslationFraction;
    float ytrans = tm.yTranslationFraction;
    float scale = tm.scalePixelsPerAngstrom;
    float zoom = tm.zmPctSet;
    float cd = tm.cameraDistance;
    float pc = tm.screenPixelCount;
    boolean pd = tm.perspectiveDepth;
    int width = tm.width;
    int height = tm.height;
    
    /**
     * @j2sNative
     * 
    *             return {
    *              center:center,
    *              quaternion:q,
    *              xtrans:xtrans,
    *              ytrans:ytrans,
    *              scale:scale,
    *              zoom:zoom,
    *              cameraDistance:cd,
    *              pixelCount:pc,
    *              perspective:pd,
    *              width:width,
    *              height:height                          
    *             };
     */
    {
      return null;
    }
  }

  public void setRotationRadius(float angstroms, boolean doAll) {
    if (doAll)
      angstroms = tm.setRotationRadius(angstroms, false);
    // only set the rotationRadius if this is NOT a dataframe
    if (ms.setRotationRadius(am.cmi, angstroms))
      g.setF("rotationRadius", angstroms);
  }

  public void setCenterBitSet(BS bsCenter, boolean doScale) {
    // Eval
    // setCenterSelected
    if (isJmolDataFrame())
      return;
    tm.setNewRotationCenter(
        (BSUtil.cardinalityOf(bsCenter) > 0 ? ms.getAtomSetCenter(bsCenter)
            : null), doScale);
  }

  public void setNewRotationCenter(P3 center) {
    // eval CENTER command
    if (!isJmolDataFrame())
      tm.setNewRotationCenter(center, true);
  }

  void navigate(int keyWhere, int modifiers) {
    if (isJmolDataFrame())
      return;
    tm.navigateKey(keyWhere, modifiers);
    if (!tm.vibrationOn && keyWhere != 0)
      refresh(1, "Viewer:navigate()");
  }

  public void move(JmolScriptEvaluator eval, V3 dRot, float dZoom, V3 dTrans,
                   float dSlab, float floatSecondsTotal, int fps) {
    // from Eval
    tm.move(eval, dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    moveUpdate(floatSecondsTotal);
  }

  public void moveTo(JmolScriptEvaluator eval, float floatSecondsTotal,
                     P3 center, V3 rotAxis, float degrees, M3 rotationMatrix,
                     float zoom, float xTrans, float yTrans,
                     float rotationRadius, P3 navCenter, float xNav,
                     float yNav, float navDepth, float cameraDepth,
                     float cameraX, float cameraY) {
    // from StateManager -- -1 for time --> no repaint
    if (!haveDisplay)
      floatSecondsTotal = 0;
    setTainted(true);
    tm.moveTo(eval, floatSecondsTotal, center, rotAxis, degrees,
        rotationMatrix, zoom, xTrans, yTrans, rotationRadius, navCenter, xNav,
        yNav, navDepth, cameraDepth, cameraX, cameraY);
  }

  public void moveUpdate(float floatSecondsTotal) {
    if (floatSecondsTotal > 0)
      requestRepaintAndWait("moveUpdate");
    else if (floatSecondsTotal == 0)
      setSync();
  }

  public void navigatePt(P3 center) {
    // isosurface setHeading
    tm.setNavigatePt(center);
    setSync();
  }

  public void navigateAxis(V3 rotAxis, float degrees) {
    // isosurface setHeading
    tm.navigateAxis(rotAxis, degrees);
    setSync();
  }

  public void navTranslatePercent(float x, float y) {
    if (isJmolDataFrame())
      return;
    tm.navTranslatePercentOrTo(0, x, y);
    setSync();
  }

  void zoomBy(int pixels) {
    // MouseManager.mouseSinglePressDrag
    //if (mouseEnabled)
    tm.zoomBy(pixels);
    refresh(2, sm.syncingMouse ? "Mouse: zoomBy " + pixels : "");
  }

  void zoomByFactor(float factor, int x, int y) {
    // MouseManager.mouseWheel
    //if (mouseEnabled)
    tm.zoomByFactor(factor, x, y);
    refresh(2, !sm.syncingMouse ? "" : "Mouse: zoomByFactor " + factor
        + (x == Integer.MAX_VALUE ? "" : " " + x + " " + y));
  }

  void rotateXYBy(float degX, float degY) {
    // mouseSinglePressDrag
    //if (mouseEnabled)
    tm.rotateXYBy(degX, degY, null);
    refresh(2, sm.syncingMouse ? "Mouse: rotateXYBy " + degX + " " + degY : "");
  }

  public void spinXYBy(int xDelta, int yDelta, float speed) {
    //if (mouseEnabled)
    tm.spinXYBy(xDelta, yDelta, speed);
    if (xDelta == 0 && yDelta == 0)
      return;
    refresh(2, sm.syncingMouse ? "Mouse: spinXYBy " + xDelta + " " + yDelta
        + " " + speed : "");
  }

  public void rotateZBy(int zDelta, int x, int y) {
    // mouseSinglePressDrag
    //if (mouseEnabled)
    tm.rotateZBy(zDelta, x, y);
    refresh(2, sm.syncingMouse ? "Mouse: rotateZBy " + zDelta
        + (x == Integer.MAX_VALUE ? "" : " " + x + " " + y) : "");
  }

  void rotateSelected(float deltaX, float deltaY, BS bsSelected) {
    // bsSelected null comes from sync. 
    if (isJmolDataFrame())
      return;
    //if (mouseEnabled) {
    // "true" in setMovableBitSet call is necessary to implement set allowMoveAtoms
    tm.rotateXYBy(deltaX, deltaY, setMovableBitSet(bsSelected, true));
    refreshMeasures(true);
    //}
    //TODO: note that sync may not work with set allowRotateSelectedAtoms
    refresh(2, sm.syncingMouse ? "Mouse: rotateMolecule " + deltaX + " "
        + deltaY : "");
  }

  public BS movableBitSet;

  private BS setMovableBitSet(BS bsSelected, boolean checkMolecule) {
    if (bsSelected == null)
      bsSelected = bsA();
    bsSelected = BSUtil.copy(bsSelected);
    BSUtil.andNot(bsSelected, getMotionFixedAtoms());
    if (checkMolecule && !g.allowMoveAtoms)
      bsSelected = ms.getMoleculeBitSet(bsSelected);
    return movableBitSet = bsSelected;
  }

  public void translateXYBy(int xDelta, int yDelta) {
    // mouseDoublePressDrag, mouseSinglePressDrag
    //if (mouseEnabled)
    tm.translateXYBy(xDelta, yDelta);
    refresh(2, sm.syncingMouse ? "Mouse: translateXYBy " + xDelta + " "
        + yDelta : "");
  }

  @Override
  public void rotateFront() {
    // deprecated
    tm.resetRotation();
    refresh(1, "Viewer:rotateFront()");
  }

  public void translate(char xyz, float x, char type, BS bsAtoms) {
    int xy = (type == '\0' ? (int) x : type == '%' ? tm.percentToPixels(xyz, x)
        : tm.angstromsToPixels(x * (type == 'n' ? 10f : 1f)));
    if (bsAtoms != null) {
      if (xy == 0)
        return;
      tm.setSelectedTranslation(bsAtoms, xyz, xy);
    } else {
      switch (xyz) {
      case 'X':
      case 'x':
        if (type == '\0')
          tm.translateToPercent('x', x);
        else
          tm.translateXYBy(xy, 0);
        break;
      case 'Y':
      case 'y':
        if (type == '\0')
          tm.translateToPercent('y', x);
        else
          tm.translateXYBy(0, xy);
        break;
      case 'Z':
      case 'z':
        if (type == '\0')
          tm.translateToPercent('z', x);
        else
          tm.translateZBy(xy);
        break;
      }
    }
    refresh(1, "Viewer:translate()");
  }

  void slabByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    tm.slabByPercentagePoints(pixels);
    refresh(3, "slabByPixels");
  }

  void depthByPixels(int pixels) {
    // MouseManager.mouseDoublePressDrag
    tm.depthByPercentagePoints(pixels);
    refresh(3, "depthByPixels");

  }

  void slabDepthByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    tm.slabDepthByPercentagePoints(pixels);
    refresh(3, "slabDepthByPixels");
  }

  //  @Override
  //  public M4 getUnscaledTransformMatrix() {
  //    // unused
  //    return tm.getUnscaledTransformMatrix();
  //  }

  public void finalizeTransformParameters() {
    // FrameRenderer
    // InitializeModel

    tm.finalizeTransformParameters();
    gdata.setSlabAndZShade(tm.slabValue, tm.depthValue,
        (tm.zShadeEnabled ? tm.zSlabValue : Integer.MAX_VALUE), tm.zDepthValue,
        g.zShadePower);
  }

  public float getScalePixelsPerAngstrom(boolean asAntialiased) {
    return tm.scalePixelsPerAngstrom
        * (asAntialiased || !antialiased ? 1f : 0.5f);
  }

  public void setSpin(String key, int value) {
    // Eval
    if (!PT.isOneOf(key, ";x;y;z;fps;X;Y;Z;FPS;"))
      return;
    int i = "x;y;z;fps;X;Y;Z;FPS".indexOf(key);
    switch (i) {
    case 0:
      tm.setSpinXYZ(value, Float.NaN, Float.NaN);
      break;
    case 2:
      tm.setSpinXYZ(Float.NaN, value, Float.NaN);
      break;
    case 4:
      tm.setSpinXYZ(Float.NaN, Float.NaN, value);
      break;
    case 6:
    default:
      tm.setSpinFps(value);
      break;
    case 10:
      tm.setNavXYZ(value, Float.NaN, Float.NaN);
      break;
    case 12:
      tm.setNavXYZ(Float.NaN, value, Float.NaN);
      break;
    case 14:
      tm.setNavXYZ(Float.NaN, Float.NaN, value);
      break;
    case 16:
      tm.setNavFps(value);
      break;
    }
    g.setI((i < 10 ? "spin" : "nav") + key, value);
  }

  public String getSpinState() {
    return getStateCreator().getSpinState(false);
  }

  public String getOrientationText(int type, String name) {
    switch (type) {
    case T.volume:
    case T.best:
    case T.x:
    case T.y:
    case T.z:
    case T.quaternion:
      return ms.getBoundBoxOrientation(type, bsA());
    case T.name:
      return stm.getSavedOrientationText(name);
    default:
      return tm.getOrientationText(type);
    }
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  public float[] getCurrentColorRange() {
    return cm.getPropertyColorRange();
  }

  private void setDefaultColors(boolean isRasmol) {
    cm.setDefaultColors(isRasmol);
    g.setB("colorRasmol", isRasmol);
    g.setO("defaultColorScheme", (isRasmol ? "rasmol" : "jmol"));
  }

  public void setElementArgb(int elementNumber, int argb) {
    // Eval
    g.setO("=color " + Elements.elementNameFromNumber(elementNumber),
        Escape.escapeColor(argb));
    cm.setElementArgb(elementNumber, argb);
  }

  @Override
  public void setVectorScale(float scale) {
    g.setF("vectorScale", scale);
    g.vectorScale = scale;
  }

  @Override
  public void setVibrationScale(float scale) {
    // Eval
    // public legacy in JmolViewer
    tm.setVibrationScale(scale);
    g.vibrationScale = scale;
    // because this is public:
    g.setF("vibrationScale", scale);
  }

  @Override
  public void setVibrationPeriod(float period) {
    // Eval
    tm.setVibrationPeriod(period);
    period = Math.abs(period);
    g.vibrationPeriod = period;
    // because this is public:
    g.setF("vibrationPeriod", period);
  }

  void setObjectColor(String name, String colorName) {
    if (colorName == null || colorName.length() == 0)
      return;
    setObjectArgb(name, CU.getArgbFromString(colorName));
  }

  public void setObjectVisibility(String name, boolean b) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId >= 0) {
      setShapeProperty(objId, "display", b ? Boolean.TRUE : Boolean.FALSE);
    }

  }

  public void setObjectArgb(String name, int argb) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId < 0) {
      if (name.equalsIgnoreCase("axes")) {
        setObjectArgb("axis1", argb);
        setObjectArgb("axis2", argb);
        setObjectArgb("axis3", argb);
      }
      return;
    }
    g.objColors[objId] = argb;
    switch (objId) {
    case StateManager.OBJ_BACKGROUND:
      gdata.setBackgroundArgb(argb);
      cm.setColixBackgroundContrast(argb);
      break;
    }
    g.setO(name + "Color", Escape.escapeColor(argb));
  }

  public void setBackgroundImage(String fileName, Object image) {
    g.backgroundImageFileName = fileName;
    gdata.setBackgroundImage(image);
  }

  public short getObjectColix(int objId) {
    int argb = g.objColors[objId];
    return (argb == 0 ? cm.colixBackgroundContrast : C.getColix(argb));
  }

  public String getFontState(String myType, Font font3d) {
    return getStateCreator().getFontState(myType, font3d);
  }

  // for historical reasons, leave these two:

  @Override
  public void setColorBackground(String colorName) {
    setObjectColor("background", colorName);
  }

  @Override
  public int getBackgroundArgb() {
    return g.objColors[(StateManager.OBJ_BACKGROUND)];
  }

  public void setObjectMad(int iShape, String name, int mad) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return;
    if (mad == -2 || mad == -4) { // turn on if not set "showAxes = true"
      int m = mad + 3;
      mad = getObjectMad(objId);
      if (mad == 0)
        mad = m;
    }
    g.setB("show" + name, mad != 0);
    g.objStateOn[objId] = (mad != 0);
    if (mad == 0)
      return;
    g.objMad[objId] = mad;
    setShapeSize(iShape, mad, null); // just loads it
  }

  public int getObjectMad(int objId) {
    return (g.objStateOn[objId] ? g.objMad[objId] : 0);
  }

  public void setPropertyColorScheme(String scheme, boolean isTranslucent,
                                     boolean isOverloaded) {
    g.propertyColorScheme = scheme;
    if (scheme.startsWith("translucent ")) {
      isTranslucent = true;
      scheme = scheme.substring(12).trim();
    }
    cm.setPropertyColorScheme(scheme, isTranslucent, isOverloaded);
  }

  public String getLightingState() {
    return getStateCreator().getLightingState(true);
  }

  public P3 getColorPointForPropertyValue(float val) {
    // x = {atomno=3}.partialcharge.color
    return CU.colorPtFromInt(
        gdata.getColorArgbOrGray(cm.ce.getColorIndex(val)), null);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  public void select(BS bs, boolean isGroup, int addRemove, boolean isQuiet) {
    // Eval, ActionManager
    if (isGroup)
      bs = getUndeletedGroupAtomBits(bs);
    slm.select(bs, addRemove, isQuiet);
    shm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MAX_VALUE, null, null);
  }

  @Override
  public void setSelectionSet(BS set) {
    // JmolViewer API only -- not used in Jmol 
    select(set, false, 0, true);
  }

  public void selectBonds(BS bs) {
    shm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MAX_VALUE, null, bs);
  }

  public void displayAtoms(BS bs, boolean isDisplay, boolean isGroup,
                           int addRemove, boolean isQuiet) {
    // Eval
    if (isGroup)
      bs = getUndeletedGroupAtomBits(bs);
    if (isDisplay)
      slm.display(ms, bs, addRemove, isQuiet);
    else
      slm.hide(ms, bs, addRemove, isQuiet);
  }

  private BS getUndeletedGroupAtomBits(BS bs) {
    bs = ms.getAtoms(T.group, bs);
    BSUtil.andNot(bs, slm.bsDeleted);
    return bs;
  }

  void reportSelection(String msg) {
    if (selectionHalosEnabled)
      setTainted(true);
    if (isScriptQueued() || g.debugScript)
      scriptStatus(msg);
  }

  private void clearAtomSets() {
    slm.setSelectionSubset(null);
    definedAtomSets.clear();
    if (haveDisplay)
      acm.exitMeasurementMode("clearAtomSets");
  }

  public BS getDefinedAtomSet(String name) {
    Object o = definedAtomSets.get(name.toLowerCase());
    return (o instanceof BS ? (BS) o : new BS());
  }

  @Override
  public void selectAll() {
    // initializeModel
    slm.selectAll(false);
  }

  @Override
  public void clearSelection() {
    // not used in this project; in jmolViewer interface, though
    slm.clearSelection(true);
    g.setB("hideNotSelected", false);
  }

  public BS bsA() {
    return slm.getSelectedAtoms();
  }

  @Override
  public void addSelectionListener(JmolSelectionListener listener) {
    slm.addListener(listener);
  }

  @Override
  public void removeSelectionListener(JmolSelectionListener listener) {
    slm.addListener(listener);
  }

  BS getAtomBitSetEval(JmolScriptEvaluator eval, Object atomExpression) {
    return (allowScripting ? getScriptManager().getAtomBitSetEval(eval,
        atomExpression) : new BS());
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to Mouse (part of the apiPlatform system), 
  // ///////////////////////////////////////////////////////////////

  /**
   * either org.jmol.awt.Mouse or org.jmol.awtjs2d.Mouse
   */
  private GenericMouseInterface mouse;

  public void processTwoPointGesture(float[][][] touches) {
    mouse.processTwoPointGesture(touches);
  }

  public boolean processMouseEvent(int id, int x, int y, int modifiers,
                                   long time) {
    // also used for JavaScript from jQuery
    return mouse.processEvent(id, x, y, modifiers, time);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ActionManager
  // ///////////////////////////////////////////////////////////////

  public Rectangle getRubberBandSelection() {
    return (haveDisplay ? acm.getRubberBand() : null);
  }

  public boolean isBound(int mouseAction, int jmolAction) {
    return (haveDisplay && acm.bnd(mouseAction, jmolAction));

  }

  public int getCursorX() {
    return (haveDisplay ? acm.getCurrentX() : 0);
  }

  public int getCursorY() {
    return (haveDisplay ? acm.getCurrentY() : 0);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to FileManager
  // ///////////////////////////////////////////////////////////////

  public String getDefaultDirectory() {
    return g.defaultDirectory;
  }

  public String getLocalUrl(String fileName) {
    return apiPlatform.getLocalUrl(fileName);
  }

  @Override
  public BufferedInputStream getBufferedInputStream(String fullPathName) {
    // used by some JVXL readers, also OutputManager.writeZipFile and ScriptManager.openFileAsync
    return fm.getBufferedInputStream(fullPathName);
  }

  /*
    public void addLoadScript(String script) {
      System.out.println("VIEWER addLoadSCript " + script);
      // fileManager.addLoadScript(script);
    }
  */
  private Map<String, Object> setLoadParameters(Map<String, Object> htParams,
                                                boolean isAppend) {
    if (htParams == null)
      htParams = new Hashtable<String, Object>();
    htParams.put("vwr", this);
    if (g.atomTypes.length() > 0)
      htParams.put("atomTypes", g.atomTypes);
    if (!htParams.containsKey("lattice"))
      htParams.put("lattice", g.ptDefaultLattice);
    if (g.applySymmetryToBonds)
      htParams.put("applySymmetryToBonds", Boolean.TRUE);
    if (g.pdbGetHeader)
      htParams.put("getHeader", Boolean.TRUE);
    if (g.pdbSequential)
      htParams.put("isSequential", Boolean.TRUE);
    if (g.legacyJavaFloat)
      htParams.put("legacyJavaFloat", Boolean.TRUE);
    htParams.put("stateScriptVersionInt",
        Integer.valueOf(stateScriptVersionInt));
    if (!htParams.containsKey("filter")) {
      String filter = g.defaultLoadFilter;
      if (filter.length() > 0)
        htParams.put("filter", filter);
    }
    boolean merging = (isAppend && !g.appendNew && ms.ac > 0);
    htParams.put("baseAtomIndex", Integer.valueOf(isAppend ? ms.ac : 0));
    htParams.put("baseModelIndex",
        Integer.valueOf(ms.ac == 0 ? 0 : ms.mc + (merging ? -1 : 0)));
    if (merging)
      htParams.put("merging", Boolean.TRUE);
    return htParams;
  }

  // //////////////// methods that open a file to create a model set ///////////

  //  *indicates when a refresh is made (external apps and applets only)
  //  
  //  external apps only 
  //    via loadInline(List)*
  //      createModelSetAndReturnError
  //  
  //  openDOM, openReader, openFile, openFiles
  //    via loadModelFromFileRepaint*
  //      createModelSetAndReturnError
  //  
  //  loadInLine(String) via loadInLineScriptRepaint*
  //  FileDropper (string drop) via openStringInline*
  //    via openStringInlineParamsAppend
  //      createModelSetAndReturnError
  //
  //  external apps, applet only, via loadInline(String[])*
  //    via openStringsInlineParamsAppend
  //      createModelSetAndReturnError
  //
  //  script LOAD
  //    via loadModelFromFile
  //      createModelSetAndReturnError
  //      
  //  script CALCULATE HYDROGENS, PLOT, ZAP (modelkit)
  //    via openStringInlineParamsAppend
  //      createModelSetAndReturnError
  //  
  //  script LOAD DATA via loadFileFull and loadInlineScript
  //    openStringsInlineParamsAppend
  //      createModelSetAndReturnError

  /**
   * opens a file as a model, a script, or a surface via the creation of a
   * script that is queued \t at the beginning disallows script option - used by
   * JmolFileDropper and JmolPanel file-open actions - sets up a script to load
   * the file.
   * 
   * Called from (JSmolCore.js)Jmol.$appEvent(,,"drop").reader.onloadend()
   * 
   * @param fileName
   * @param flags
   *        1=pdbCartoons, 2=no scripting, 4=append, 8=fileOpen
   * 
   */
  @Override
  public void openFileAsyncSpecial(String fileName, int flags) {
    getScriptManager().openFileAsync(fileName, flags);
  }

  /**
   * 
   * for JmolSimpleViewer -- external applications only (and no-script
   * JavaScript)
   * 
   * @param fileName
   * @return null or error
   */
  @Override
  public String openFile(String fileName) {
    zap(true, true, false);
    return loadModelFromFileRepaint(null, fileName, null, null);
  }

  /**
   * for JmolSimpleViewer -- external applications only
   * 
   * @param fileNames
   * @return null or error
   */
  @Override
  public String openFiles(String[] fileNames) {
    zap(true, true, false);
    return loadModelFromFileRepaint(null, null, fileNames, null);
  }

  /**
   * Opens the file, given an already-created reader.
   * 
   * @param fullPathName
   * @param fileName
   * @param reader
   * @return null or error message
   */
  @Override
  public String openReader(String fullPathName, String fileName, Reader reader) {
    zap(true, true, false);
    return loadModelFromFileRepaint(fullPathName, fileName, null, reader);
  }

  /**
   * applet DOM method -- does not preserve state
   * 
   * @param DOMNode
   * @return null or error
   * 
   */
  @Override
  public String openDOM(Object DOMNode) {
    // applet.loadDOMNode
    zap(true, true, false);
    return loadModelFromFileRepaint("?", "?", null, DOMNode);
  }

  /**
   * 
   * for JmolSimpleViewer -- external applications only (and no-script
   * JavaScript)
   * 
   * @param fullPathName
   * @param fileName
   * @param fileNames
   * @param reader
   * @return error message or null
   * 
   */
  private String loadModelFromFileRepaint(String fullPathName, String fileName,
                                          String[] fileNames, Object reader) {
    String ret = loadModelFromFile(fullPathName, fileName, fileNames, reader,
        false, null, null, null, 0, false);
    refresh(1, "loadModelFromFileRepaint");
    return ret;
  }

  /**
   * Used by the ScriptEvaluator LOAD command to open one or more files. Now
   * necessary for EVERY load of a file, as loadScript must be passed to the
   * ModelLoader.
   * 
   * @param fullPathName
   *        TODO
   * @param fileName
   * @param fileNames
   * @param reader
   *        TODO
   * @param isAppend
   * @param htParams
   * @param loadScript
   * @param sOptions
   * @param tokType
   * @param isConcat
   * @return null or error
   */
  public String loadModelFromFile(String fullPathName, String fileName,
                                  String[] fileNames, Object reader,
                                  boolean isAppend,
                                  Map<String, Object> htParams, SB loadScript,
                                  SB sOptions, int tokType, boolean isConcat) {
    if (htParams == null)
      htParams = setLoadParameters(null, isAppend);
    if (isConcat)
      htParams.put("concatenate", Boolean.TRUE);
    Object atomSetCollection;
    String[] saveInfo = fm.getFileInfo();
    
    if (fileNames != null) {

      // 1) a set of file names

      if (loadScript == null) {
        loadScript = new SB().append("load files");
        for (int i = 0; i < fileNames.length; i++)
          loadScript.append(i == 0 || !isConcat ? " " : "+").append(
              "/*file*/$FILENAME" + (i + 1) + "$");
      }
      if (sOptions.length() > 0)
        loadScript.append(" /*options*/ ").append(sOptions.toString());
      long timeBegin = System.currentTimeMillis();

      atomSetCollection = fm.createAtomSetCollectionFromFiles(fileNames,
          setLoadParameters(htParams, isAppend), isAppend);
      long ms = System.currentTimeMillis() - timeBegin;
      Logger.info("openFiles(" + fileNames.length + ") " + ms + " ms");
      fileNames = (String[]) htParams.get("fullPathNames");
      String[] fileTypes = (String[]) htParams.get("fileTypes");
      String s = loadScript.toString();
      for (int i = 0; i < fileNames.length; i++) {
        String fname = fileNames[i];
        if (fileTypes != null && fileTypes[i] != null)
          fname = fileTypes[i] + "::" + fname;
        s = PT.rep(s, "$FILENAME" + (i + 1) + "$",
            PT.esc(fname.replace('\\', '/')));
      }

      loadScript = new SB().append(s);

    } else if (reader == null) {

      // 2) a standard, single file 

      if (loadScript == null)
        loadScript = new SB().append("load /*file*/$FILENAME$");

      atomSetCollection = openFileFull(fileName, isAppend, htParams, loadScript);

    } else if (reader instanceof Reader) {

      // 3) a file reader (not used by Jmol) 

      atomSetCollection = fm.createAtomSetCollectionFromReader(fullPathName,
          fileName, reader, htParams);

    } else {

      // 4) a DOM reader (could be used by Jmol) 

      atomSetCollection = fm.createAtomSetCollectionFromDOM(reader, htParams);
    }

    // OK, the file has been read and is now closed.

    if (tokType != 0) { // all we are doing is reading atom data
      fm.setFileInfo(saveInfo);
      return loadAtomDataAndReturnError(atomSetCollection, tokType);
    }

    if (htParams.containsKey("isData"))
      return (String) atomSetCollection;

    // now we fix the load script (possibly) with the full path name
    if (loadScript != null && !(atomSetCollection instanceof String)) {
      String fname = (String) htParams.get("fullPathName");
      if (fname == null)
        fname = "";
      // may have been modified.
      if (htParams.containsKey("loadScript"))
        loadScript = (SB) htParams.get("loadScript");
      htParams.put(
          "loadScript",
          loadScript = new SB().append(javajs.util.PT.rep(
              loadScript.toString(), "$FILENAME$",
              PT.esc(fname.replace('\\', '/')))));
    }

    // and finally to create the model set...

    return createModelSetAndReturnError(atomSetCollection, isAppend,
        loadScript, htParams);
  }

  Map<String, Object> ligandModels;
  Map<String, Boolean> ligandModelSet;

  public void setLigandModel(String key, String data) {
    if (ligandModels == null)
      ligandModels = new Hashtable<String, Object>();
    ligandModels.put(key, data);
  }

  /**
   * obtain CIF data for a ligand for purposes of adding hydrogens
   * or for any other purpose in terms of saving a data set for a file
   * in a state
   * 
   * @param id
   *        unique key; if null, clear "bad" entries from the set.
   * @param prefix
   * @param suffix or fileName
   * @param terminator
   *        Only save to this if not null
   * @return a ligand model or a string if just file data or null
   */
  public Object getLigandModel(String id, String prefix, String suffix,
                               String terminator) {
    // getLigandModel(id, m40File, "_file", "----"));
    // getLigandModel(group3, "ligand_", "_data", null);
    if (id == null) {
      if (ligandModelSet != null) {
        Iterator<Map.Entry<String, Object>> e = ligandModels.entrySet()
            .iterator();
        while (e.hasNext()) {
          Entry<String, Object> entry = e.next();
          if (entry.getValue() instanceof Boolean)
            e.remove();
        }
      }
      return null;
    }
    id = id.replace('\\', '/');
    boolean isLigand = prefix.equals("ligand_");
    id = (isLigand ? id.toUpperCase() : id.substring(id.lastIndexOf("/") + 1));
    if (ligandModelSet == null)
      ligandModelSet = new Hashtable<String, Boolean>();
    ligandModelSet.put(id, Boolean.TRUE);
    if (ligandModels == null)
      ligandModels = new Hashtable<String, Object>();
    int pngPt = id.indexOf("|");
    if (pngPt >= 0)
      id = id.substring(id.indexOf("|") + 1);
    Object model = (terminator == null ? ligandModels.get(id) : null);
    String data;
    String fname = null;
    if (model instanceof Boolean)
      return null;
    if (model == null && (terminator == null || pngPt >= 0))
      model = ligandModels.get(id + suffix);
    boolean isError = false;
    boolean isNew = (model == null);
    if (isNew) {
      String s;
      if (isLigand) {
        fname = (String) setLoadFormat("#" + id, '#', false);
        if (fname.length() == 0)
          return null;
        scriptEcho("fetching " + fname);
        s = getFileAsString3(fname, false, null);
      } else {
        scriptEcho("fetching " + prefix);
        s = getFileAsString3(prefix, false, null);
        int pt = (terminator == null ? -1 : s.indexOf(terminator));
        if (pt >= 0)
          s = s.substring(0, pt);
      }
      isError = (s.indexOf("java.") == 0);
      model = s;
      if (!isError)
        ligandModels.put(id + suffix, model);
    }
    if (!isLigand) {
      if (!isNew)
        scriptEcho(prefix + " loaded from cache");
      return model;
    }
    // process ligand business
    
    if (!isError && model instanceof String) {
      data = (String) model;
      // TODO: check for errors in reading file
      if (data.length() != 0) {
        Map<String, Object> htParams = new Hashtable<String, Object>();
        htParams.put("modelOnly", Boolean.TRUE);
        model = getModelAdapter().getAtomSetCollectionReader("ligand", null,
            Rdr.getBR(data), htParams);
        isError = (model instanceof String);
        if (!isError) {
          model = getModelAdapter().getAtomSetCollection(model);
          isError = (model instanceof String);
          if (fname != null && !isError)
            scriptEcho((String) getModelAdapter()
                .getAtomSetCollectionAuxiliaryInfo(model).get("modelLoadNote"));
        }
      }
    }
    if (isError) {
      scriptEcho(model.toString());
      ligandModels.put(id, Boolean.FALSE);
      return null;
    }
    return model;
  }

  /**
   * 
   * does NOT repaint
   * 
   * @param fileName
   * @param isAppend
   * @param htParams
   * @param loadScript
   *        only necessary for string reading
   * @return an AtomSetCollection or a String (error)
   */
  private Object openFileFull(String fileName, boolean isAppend,
                              Map<String, Object> htParams, SB loadScript) {
    if (fileName == null)
      return null;
    if (fileName.equals("String[]")) {
      // no reloading of string[] or file[] data -- just too complicated
      return null;
    }
    Object atomSetCollection;
    String msg = "openFile(" + fileName + ")";
    Logger.startTimer(msg);
    htParams = setLoadParameters(htParams, isAppend);
    boolean isLoadVariable = fileName.startsWith("@");
    boolean haveFileData = (htParams.containsKey("fileData"));
    if (fileName.indexOf('$') == 0)
      htParams.put("smilesString", fileName.substring(1));
    boolean isString = (fileName.equals("string") || fileName
        .equals(JC.MODELKIT_ZAP_TITLE));
    String strModel = null;
    if (haveFileData) {
      strModel = (String) htParams.get("fileData");
      if (htParams.containsKey("isData")) {
        Object o = loadInlineScript(strModel, '\0', isAppend, htParams);
        lastData = (g.preserveState ? getDataManager().createFileData(strModel) : null);
        return o;
      }
    } else if (isString) {
      strModel = ms.getInlineData(-1);
      if (strModel == null)
        if (g.modelKitMode)
          strModel = JC.MODELKIT_ZAP_STRING;
        else
          return "cannot find string data";
      if (loadScript != null)
        htParams.put(
            "loadScript",
            loadScript = new SB().append(PT.rep(loadScript.toString(),
                "$FILENAME$", "data \"model inline\"\n" + strModel
                    + "end \"model inline\"")));
    }
    if (strModel != null) {
      if (!isAppend)
        zap(true, false/*true*/, false);
      if (!isLoadVariable && (!haveFileData || isString))
        getStateCreator().getInlineData(loadScript, strModel, isAppend,
            g.defaultLoadFilter);
      atomSetCollection = fm.createAtomSetCollectionFromString(strModel,
          htParams, isAppend);
    } else {

      // if the filename has a "?" at the beginning, we don't zap, 
      // because the user might cancel the operation.

      atomSetCollection = fm.createAtomSetCollectionFromFile(fileName,
          htParams, isAppend);
    }
    Logger.checkTimer(msg, false);
    return atomSetCollection;
  }

  /**
   * only used by file dropper.
   */

  @Override
  public String openStringInline(String strModel) {
    // JmolSimpleViewer; JmolFileDropper inline string event
    String ret = openStringInlineParamsAppend(strModel, null, false);
    refresh(1, "openStringInline");
    return ret;
  }

  /**
   * from Applet and external applications only
   */

  @Override
  public String loadInline(String strModel) {
    // jmolViewer interface
    return loadInlineScriptRepaint(strModel, g.inlineNewlineChar, false);
  }

  /**
   * external apps only
   * 
   */

  @Override
  public String loadInline(String strModel, char newLine) {
    // JmolViewer interface
    return loadInlineScriptRepaint(strModel, newLine, false);
  }

  /**
   * used by applet and console
   */

  @Override
  public String loadInlineAppend(String strModel, boolean isAppend) {
    // JmolViewer interface
    return loadInlineScriptRepaint(strModel, '\0', isAppend);
  }

  private String loadInlineScriptRepaint(String strModel, char newLine,
                                         boolean isAppend) {
    String ret = loadInlineScript(strModel, newLine, isAppend, null);
    refresh(1, "loadInlineScript");
    return ret;
  }

  /**
   * external apps only
   * 
   */

  @Override
  public String loadInline(String[] arrayModels) {
    // JmolViewer interface
    return loadInline(arrayModels, false);
  }

  /**
   * external apps and applet only
   * 
   */
  @Override
  public String loadInline(String[] arrayModels, boolean isAppend) {
    // JmolViewer interface
    // Eval data
    // loadInline
    if (arrayModels == null || arrayModels.length == 0)
      return null;
    String ret = openStringsInlineParamsAppend(arrayModels, new Hashtable<String, Object>(), isAppend);
    refresh(1, "loadInline String[]");
    return ret;
  }

  /**
   * External applications only; does not preserve state -- intentionally!
   * 
   * @param arrayData
   * @param isAppend
   * @return null or error string
   * 
   */
  @Override
  public String loadInline(java.util.List<Object> arrayData, boolean isAppend) {
    // NO STATE SCRIPT -- HERE WE ARE TRYING TO CONSERVE SPACE

    // loadInline
    if (arrayData == null || arrayData.size() == 0)
      return null;
    if (!isAppend)
      zap(true, false/*true*/, false);
    Lst<Object> list = new Lst<Object>();
    for (int i = 0; i < arrayData.size(); i++)
      list.addLast(arrayData.get(i));
    Object atomSetCollection = fm.createAtomSeCollectionFromArrayData(list,
        setLoadParameters(null, isAppend), isAppend);
    String ret = createModelSetAndReturnError(atomSetCollection, isAppend,
        null, new Hashtable<String, Object>());
    refresh(1, "loadInline");
    return ret;
  }

  /**
   * used by loadInline and openFileFull
   * 
   * @param strModel
   * @param newLine
   * @param isAppend
   * @param htParams
   * @return null or error message
   */
  private String loadInlineScript(String strModel, char newLine,
                                  boolean isAppend, Map<String, Object> htParams) {
    if (strModel == null || strModel.length() == 0)
      return null;
    strModel = fixInlineString(strModel, newLine);
    if (newLine != 0)
      Logger.info("loading model inline, " + strModel.length()
          + " bytes, with newLine character " + (int) newLine + " isAppend="
          + isAppend);
    if (Logger.debugging)
      Logger.debug(strModel);
    String datasep = getDataSeparator();
    int i;
    if (datasep != null && datasep != ""
        && (i = strModel.indexOf(datasep)) >= 0
        && strModel.indexOf("# Jmol state") < 0) {
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
      return openStringsInlineParamsAppend(strModels, htParams, isAppend);
    }
    return openStringInlineParamsAppend(strModel, htParams, isAppend);
  }

  public static String fixInlineString(String strModel, char newLine) {
    // only if first character is "|" do we consider "|" to be new line
    int i;
    if (strModel.indexOf("\\/n") >= 0) {
      // the problem is that when this string is passed to Jmol
      // by the web page <embed> mechanism, browsers differ
      // in how they handle CR and LF. Some will pass it,
      // some will not.
      strModel = PT.rep(strModel, "\n", "");
      strModel = PT.rep(strModel, "\\/n", "\n");
      newLine = 0;
    }
    if (newLine != 0 && newLine != '\n') {
      boolean repEmpty = (strModel.indexOf('\n') >= 0);
      int len = strModel.length();
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      if (repEmpty)
        strModel = PT.rep(strModel, "" + newLine, "");
      else
        strModel = strModel.replace(newLine, '\n');
    }
    return strModel;
  }

  /**
   * Only used for adding hydrogen atoms and adding the model kit methane model;
   * not part of the public interface.
   * 
   * @param strModel
   * @param htParams
   * @param isAppend
   * @return null or error string
   * 
   */
  public String openStringInlineParamsAppend(String strModel,
                                             Map<String, Object> htParams,
                                             boolean isAppend) {
    // loadInline, openStringInline

    BufferedReader br = Rdr.getBR(strModel);
    String type = getModelAdapter().getFileTypeName(br);
    if (type == null)
      return "unknown file type";
    if (type.equals("spt")) {
      return "cannot open script inline";
    }

    htParams = setLoadParameters(htParams, isAppend);
    SB loadScript = (SB) htParams.get("loadScript");
    boolean isLoadCommand = htParams.containsKey("isData");
    if (loadScript == null)
      loadScript = new SB();
    if (!isAppend)
      zap(true, false/*true*/, false);
    if (!isLoadCommand)
      getStateCreator().getInlineData(loadScript, strModel, isAppend,
          g.defaultLoadFilter);
    Object atomSetCollection = fm.createAtomSetCollectionFromString(strModel,
        htParams, isAppend);
    return createModelSetAndReturnError(atomSetCollection, isAppend,
        loadScript, htParams);
  }

  /**
   * opens multiple files inline; does NOT repaint
   * 
   * @param arrayModels
   * @param htParams
   * @param isAppend
   * @return null or error message
   */
  private String openStringsInlineParamsAppend(String[] arrayModels,
                                               Map<String, Object> htParams,
                                               boolean isAppend) {
    // loadInline
    SB loadScript = new SB();
    if (!isAppend)
      zap(true, false/*true*/, false);
    Object atomSetCollection = fm.createAtomSeCollectionFromStrings(
        arrayModels, loadScript, setLoadParameters(htParams, isAppend),
        isAppend);
    return createModelSetAndReturnError(atomSetCollection, isAppend,
        loadScript, htParams);
  }

  public char getInlineChar() {
    // used by the ScriptEvaluator DATA command
    return g.inlineNewlineChar;
  }

  String getDataSeparator() {
    // used to separate data files within a single DATA command
    return (String) g.getParameter("dataseparator", true);
  }

  ////////// create the model set ////////////

  /**
   * finally(!) we are ready to create the "model set" from the
   * "atom set collection" - does NOT repaint
   * 
   * @param atomSetCollection
   * @param isAppend
   * @param loadScript
   *        if null, then some special method like DOM; turn of preserveState
   * @param htParams
   * @return errMsg
   */
  private String createModelSetAndReturnError(Object atomSetCollection,
                                              boolean isAppend, SB loadScript,
                                              Map<String, Object> htParams) {
    String fullPathName = fm.getFullPathName(false);
    String fileName = fm.getFileName();
    String errMsg;
    if (loadScript == null) {
      setBooleanProperty("preserveState", false);
      loadScript = new SB().append("load \"???\"");
    }
    if (atomSetCollection instanceof String) {
      errMsg = (String) atomSetCollection;
      setFileLoadStatus(FIL.NOT_LOADED, fullPathName, null, null, errMsg, null);
      if (displayLoadErrors && !isAppend && !errMsg.equals("#CANCELED#")
          && !errMsg.startsWith(JC.READER_NOT_FOUND))
        zapMsg(errMsg);
      return errMsg;
    }
    if (isAppend)
      clearAtomSets();
    else if (g.modelKitMode && !fileName.equals("Jmol Model Kit"))
      setModelKitMode(false);
    setFileLoadStatus(FIL.CREATING_MODELSET, fullPathName, fileName, null,
        null, null);

    // null fullPathName implies we are doing a merge
    pushHoldRepaintWhy("createModelSet");
    setErrorMessage(null, null);
    try {
      BS bsNew = new BS();
      mm.createModelSet(fullPathName, fileName, loadScript, atomSetCollection,
          bsNew, isAppend);
      if (bsNew.cardinality() > 0) {
        // is a 2D dataset, as from JME
        String jmolScript = (String) ms.getInfoM("jmolscript");
        if (ms.getMSInfoB("doMinimize"))
          try {
            JmolScriptEvaluator eval = (JmolScriptEvaluator) htParams.get("eval");
            minimize(eval,
                Integer.MAX_VALUE, 0, bsNew, null, 0, true, true, true, true);
          } catch (Exception e) {
            // TODO
          }
        else
          addHydrogens(bsNew, false, true);
        // no longer necessary? -- this is the JME/SMILES data:
        if (jmolScript != null)
          ms.msInfo.put("jmolscript", jmolScript);
      }
      initializeModel(isAppend);
      // if (global.modelkitMode &&
      // (modelSet.modelCount > 1 || modelSet.models[0].isPDB()))
      // setBooleanProperty("modelkitmode", false);

    } catch (Error er) {
      handleError(er, true);
      errMsg = getShapeErrorState();
      errMsg = ("ERROR creating model: " + er + (errMsg.length() == 0 ? ""
          : "|" + errMsg));
      zapMsg(errMsg);
      setErrorMessage(errMsg, null);
    }
    popHoldRepaint("createModelSet " + JC.REPAINT_IGNORE);
    errMsg = getErrorMessage();

    setFileLoadStatus(FIL.CREATED, fullPathName, fileName, ms.modelSetName,
        errMsg, (Boolean) htParams.get("async"));
    if (isAppend) {
      selectAll();
      setTainted(true);
      axesAreTainted = true;
    }
    atomSetCollection = null;
    System.gc();
    return errMsg;
  }

  /**
   * 
   * or just apply the data to the current model set
   * 
   * @param atomSetCollection
   * @param tokType
   * @return error or null
   */
  private String loadAtomDataAndReturnError(Object atomSetCollection,
                                            int tokType) {
    if (atomSetCollection instanceof String)
      return (String) atomSetCollection;
    setErrorMessage(null, null);
    try {
      String script = mm.createAtomDataSet(atomSetCollection, tokType);
      switch (tokType) {
      case T.xyz:
        if (script != null)
          runScript(script);
        break;
      case T.vibration:
        setStatusFrameChanged(true, false);
        break;
      case T.vanderwaals:
        shm.deleteVdwDependentShapes(null);
        break;
      }
    } catch (Error er) {
      handleError(er, true);
      String errMsg = getShapeErrorState();
      errMsg = ("ERROR adding atom data: " + er + (errMsg.length() == 0 ? ""
          : "|" + errMsg));
      zapMsg(errMsg);
      setErrorMessage(errMsg, null);
      setParallel(false);
    }
    return getErrorMessage();
  }

  ////////// File-related methods ////////////

  public String getCurrentFileAsString(String state) {
    String filename = fm.getFullPathName(false);
    if (filename.equals("string") || filename.equals(JC.MODELKIT_ZAP_TITLE))
      return ms.getInlineData(am.cmi);
    if (filename.equals("String[]"))
      return filename;
    if (filename == "JSNode")
      return "<DOM NODE>";
    //    filename = mm.getModelSetPathName();
    //    if (filename == null)
    //      return null;
    return getFileAsString4(filename, -1, true, false, false, state);
  }

  /**
   * 
   * @param filename
   * @return String[2] where [0] is fullpathname and [1] is error message or
   *         null
   */
  public String[] getFullPathNameOrError(String filename) {
    String[] data = new String[2];
    fm.getFullPathNameOrError(filename, false, data);
    return data;
  }

  public String getFileAsString3(String name, boolean checkProtected,
                                 String state) {
    return getFileAsString4(name, -1, false, false, checkProtected, state);
  }

  public String getFileAsString4(String name, int nBytesMax,
                                 boolean doSpecialLoad, boolean allowBinary,
                                 boolean checkProtected, String state) {
    if (name == null)
      return getCurrentFileAsString(state);
    String[] data = new String[2];
    data[0] = name;
    // ignore error completely
    fm.getFileDataAsString(data, nBytesMax, doSpecialLoad, allowBinary,
        checkProtected);
    return data[1];
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

  public void autoCalculate(int tokProperty) {
    switch (tokProperty) {
    case T.surfacedistance:
      ms.getSurfaceDistanceMax();
      break;
    case T.straightness:
      ms.calculateStraightnessAll();
      break;
    }
  }

  // This was just the sum of the atomic volumes, not considering overlap
  // It was never documented.
  // Removed in Jmol 13.0.RC4

  //  public float getVolume(BitSet bs, String type) {
  //    // Eval.calculate(), math function volume({atomExpression},"type")
  //    if (bs == null)
  //      bs = getSelectionSet();
  //    EnumVdw vType = EnumVdw.getVdwType(type);
  //    if (vType == null)
  //      vType = EnumVdw.AUTO;
  //    return modelSet.calculateVolume(bs, vType);
  //  }

  public void calculateStraightness() {
    ms.haveStraightness = false;
    ms.calculateStraightnessAll();
  }

  public P3[] calculateSurface(BS bsSelected, float envelopeRadius) {
    if (bsSelected == null)
      bsSelected = bsA();
    if (envelopeRadius == Float.MAX_VALUE || envelopeRadius == -1)
      ms.addStateScript("calculate surfaceDistance "
          + (envelopeRadius == Float.MAX_VALUE ? "FROM" : "WITHIN"), null,
          bsSelected, null, "", false, true);
    return ms.calculateSurface(bsSelected, envelopeRadius);
  }

  public Map<STR, float[]> getStructureList() {
    return g.getStructureList();
  }

  public void setStructureList(float[] list, STR type) {
    // none, turn, sheet, helix
    g.setStructureList(list, type);
    ms.setStructureList(getStructureList());
  }

  public String calculateStructures(BS bsAtoms, boolean asDSSP,
                                    boolean setStructure) {
    // Eval
    if (bsAtoms == null)
      bsAtoms = bsA();
    return ms.calculateStructures(bsAtoms, asDSSP, !am.animationOn,
        g.dsspCalcHydrogen, setStructure);
  }

  private JmolAnnotationParser annotationParser, dssrParser;

  public JmolAnnotationParser getAnnotationParser(boolean isDSSR) {
    return (isDSSR ? (dssrParser == null ? (dssrParser = (JmolAnnotationParser) Interface
        .getOption("dssx.DSSR1", this, "script")) : dssrParser)
        : (annotationParser == null ? (annotationParser = (JmolAnnotationParser) Interface
            .getOption("dssx.AnnotationParser", this, "script"))
            : annotationParser));
  }

  @Override
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                   boolean isGreaterOnly,
                                                   boolean modelZeroBased,
                                                   boolean isMultiModel) {
    return ms.getSelectedAtomIterator(bsSelected, isGreaterOnly,
        modelZeroBased, false, isMultiModel);
  }

  @Override
  public void setIteratorForAtom(AtomIndexIterator iterator, int atomIndex,
                                 float distance) {
    ms.setIteratorForAtom(iterator, -1, atomIndex, distance, null);
  }

  @Override
  public void setIteratorForPoint(AtomIndexIterator iterator, int modelIndex,
                                  T3 pt, float distance) {
    ms.setIteratorForPoint(iterator, modelIndex, pt, distance);
  }

  @Override
  public void fillAtomData(AtomData atomData, int mode) {
    atomData.programInfo = "Jmol Version " + getJmolVersion();
    atomData.fileName = fm.getFileName();
    ms.fillAtomData(atomData, mode);
  }

  public StateScript addStateScript(String script, boolean addFrameNumber,
                                    boolean postDefinitions) {
    // calculate
    // configuration
    // plot
    // rebond
    // setPdbConectBonding
    return ms.addStateScript(script, null, null, null, null, addFrameNumber,
        postDefinitions);
  }

  private MinimizerInterface minimizer;
  private SmilesMatcherInterface smilesMatcher;

  public MinimizerInterface getMinimizer(boolean createNew) {
    return (minimizer == null && createNew ? (minimizer = (MinimizerInterface) Interface
        .getInterface("org.jmol.minimize.Minimizer", this, "script"))
        .setProperty("vwr", this) : minimizer);
  }

  public SmilesMatcherInterface getSmilesMatcher() {
    return (smilesMatcher == null ? (smilesMatcher = (SmilesMatcherInterface) Interface
        .getInterface("org.jmol.smiles.SmilesMatcher", this, "script"))
        : smilesMatcher);
  }

  public void clearModelDependentObjects() {
    setFrameOffsets(null, false);
    stopMinimization();
    minimizer = null;
    smilesMatcher = null;
  }

  public void zap(boolean notify, boolean resetUndo, boolean zapModelKit) {
    clearThreads();
    if (mm.modelSet == null) {
      mm.zap();
    } else {
      //setBooleanProperty("appendNew", true);
      ligandModelSet = null;
      clearModelDependentObjects();
      fm.clear();
      clearRepaintManager(-1);
      am.clear();
      tm.clear();
      slm.clear();
      clearAllMeasurements();
      clearMinimization();
      gdata.clear();
      mm.zap();
      if (scm != null)
        scm.clear(false);
      if (nmrCalculation != null)
        getNMRCalculation().setChemicalShiftReference(null, 0);

      if (haveDisplay) {
        mouse.clear();
        clearTimeouts();
        acm.clear();
      }
      stm.clear(g);
      tempArray.clear();
      chainMap.clear();
      chainList.clear();
      chainCaseSpecified = false;
      //cm.clear();
      definedAtomSets.clear();
      lastData = null;
      if (dm != null)
        dm.clear();
      setBooleanProperty("legacyjavafloat", false);
      if (resetUndo) {
        if (zapModelKit && g.modelKitMode) {
          openStringInlineParamsAppend(JC.MODELKIT_ZAP_STRING, null, true);
          setRotationRadius(5.0f, true);
          setStringProperty("picking", "assignAtom_C");
          setStringProperty("picking", "assignBond_p");
        }
        undoClear();
      }
      System.gc();
    }
    initializeModel(false);
    if (notify) {
      setFileLoadStatus(FIL.ZAPPED, null, (resetUndo ? "resetUndo"
          : getZapName()), null, null, null);
    }
    if (Logger.debugging)
      Logger.checkMemory();
  }

  private void zapMsg(String msg) {
    zap(true, true, false);
    echoMessage(msg);
  }

  void echoMessage(String msg) {
    int iShape = JC.SHAPE_ECHO;
    shm.loadShape(iShape);
    setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 9));
    setShapeProperty(iShape, "target", "error");
    setShapeProperty(iShape, "text", msg);
  }

  private void initializeModel(boolean isAppend) {
    clearThreads();
    if (isAppend) {
      am.initializePointers(1);
      return;
    }
    reset(true);
    selectAll();
    rotatePrev1 = rotateBondIndex = -1;
    movingSelected = false;
    slm.noneSelected = Boolean.FALSE;
    setHoverEnabled(true);
    setSelectionHalosEnabled(false);
    tm.setCenter();
    am.initializePointers(1);
    setBooleanProperty("multipleBondBananas", false);
    if (!ms.getMSInfoB("isPyMOL")) {
      clearAtomSets();
      setCurrentModelIndex(0);
    }
    setBackgroundModelIndex(-1);
    setFrankOn(getShowFrank());
    startHoverWatcher(true);
    setTainted(true);
    finalizeTransformParameters();
  }

  public void startHoverWatcher(boolean tf) {
    if (tf && inMotion || !haveDisplay || tf
        && (!hoverEnabled && !sm.haveHoverCallback() || am.animationOn))
      return;
    acm.startHoverWatcher(tf);
  }

  @Override
  public String getModelSetPathName() {
    return mm.modelSetPathName;
  }

  @Override
  public String getModelSetFileName() {
    return (mm.fileName == null ? getZapName() : mm.fileName);
  }

  public String getUnitCellInfoText() {
    SymmetryInterface c = getCurrentUnitCell();
    return (c == null ? "not applicable" : c.getUnitCellInfo());
  }

  public float getUnitCellInfo(int infoType) {
    SymmetryInterface symmetry = getCurrentUnitCell();
    return (symmetry == null ? Float.NaN : symmetry
        .getUnitCellInfoType(infoType));
  }

  /**
   * 
   * convert string abc;offset or M3 or M4 to origin and three vectors -- a, b,
   * c. The string can be preceded by ! for "reverse of". For example,
   * "!a-b,-5a-5b,-c;7/8,0,1/8" offset is optional, but it still needs a
   * semicolon: "a/2,b/2,c;"
   * 
   * @param def
   *        a string or an M3 or M4
   * @return vectors [origin a b c]
   */
  public T3[] getV0abc(Object def) {
    SymmetryInterface uc = getCurrentUnitCell();
    return (uc == null ? null : uc.getV0abc(def));
  }

  public void getPolymerPointsAndVectors(BS bs, Lst<P3[]> vList) {
    ms.getPolymerPointsAndVectors(bs, vList, g.traceAlpha, g.sheetSmoothing);
  }

  public String getHybridizationAndAxes(int atomIndex, V3 z, V3 x,
                                        String lcaoType) {
    return ms.getHybridizationAndAxes(atomIndex, 0, z, x, lcaoType, true, true);
  }

  public BS getAllAtoms() {
    return getModelUndeletedAtomsBitSet(-1);
  }

  public BS getModelUndeletedAtomsBitSet(int modelIndex) {
    return slm.excludeAtoms(
        ms.getModelAtomBitSetIncludingDeleted(modelIndex, true), false);
  }

  public BS getModelUndeletedAtomsBitSetBs(BS bsModels) {
    return slm.excludeAtoms(ms.getModelAtomBitSetIncludingDeletedBs(bsModels),
        false);
  }

  @Override
  public P3 getBoundBoxCenter() {
    return ms.getBoundBoxCenter(am.cmi);
  }

  public void calcBoundBoxDimensions(BS bs, float scale) {
    ms.calcBoundBoxDimensions(bs, scale);
    axesAreTainted = true;
  }

  @Override
  public V3 getBoundBoxCornerVector() {
    return ms.getBoundBoxCornerVector();
  }

  public int getBoundBoxCenterX() {
    // used by axes renderer
    return dimScreen.width / 2;
  }

  public int getBoundBoxCenterY() {
    return dimScreen.height / 2;
  }

  @Override
  public Properties getModelSetProperties() {
    return ms.modelSetProperties;
  }

  @Override
  public Properties getModelProperties(int modelIndex) {
    return ms.am[modelIndex].properties;
  }

  @Override
  public Map<String, Object> getModelSetAuxiliaryInfo() {
    return ms.getAuxiliaryInfo(null);
  }

  @Override
  public int getModelNumber(int modelIndex) {
    return (modelIndex < 0 ? modelIndex : ms.getModelNumber(modelIndex));
  }

  public int getModelFileNumber(int modelIndex) {
    return (modelIndex < 0 ? 0 : ms.modelFileNumbers[modelIndex]);
  }

  @Override
  public String getModelNumberDotted(int modelIndex) {
    // must not return "all" for -1, because this could be within a frame RANGE
    return modelIndex < 0 ? "0" : ms.getModelNumberDotted(modelIndex);
  }

  @Override
  public String getModelName(int modelIndex) {
    return ms.getModelName(modelIndex);
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return (ms.getLastVibrationVector(modelIndex, T.vibration) >= 0);
  }

  public BS getBondsForSelectedAtoms(BS bsAtoms) {
    // eval
    return ms.getBondsForSelectedAtoms(bsAtoms,
        g.bondModeOr || BSUtil.cardinalityOf(bsAtoms) == 1);
  }

  public boolean frankClicked(int x, int y) {
    // bottom right Jmol logo
    return !g.disablePopupMenu && getShowFrank() && shm.checkFrankclicked(x, y);
  }

  public boolean frankClickedModelKit(int x, int y) {
    // top left indicator
    return !g.disablePopupMenu && g.modelKitMode && x >= 0 && y >= 0 && x < 40
        && y < 80;
  }

  @Override
  public int findNearestAtomIndex(int x, int y) {
    return findNearestAtomIndexMovable(x, y, false);
  }

  public int findNearestAtomIndexMovable(int x, int y, boolean mustBeMovable) {
    return (!g.atomPicking ? -1 : ms.findNearestAtomIndex(x, y,
        mustBeMovable ? slm.getMotionFixedAtoms() : null, g.minPixelSelRadius));
  }

  /**
   * absolute or relative to origin of UNITCELL {x y z}
   * 
   * @param pt
   * @param ignoreOffset
   *        TODO
   */
  public void toCartesian(T3 pt, boolean ignoreOffset) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell != null) {
      unitCell.toCartesian(pt, ignoreOffset);
      if (!g.legacyJavaFloat)
        PT.fixPtFloats(pt, PT.CARTESIAN_PRECISION);
    }
  }

  /**
   * absolute or relative to origin of UNITCELL {x y z}
   * 
   * @param pt
   * @param asAbsolute
   *        TODO
   */
  public void toFractional(T3 pt, boolean asAbsolute) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell != null) {
      unitCell.toFractional(pt, asAbsolute);
      if (!g.legacyJavaFloat)
        PT.fixPtFloats(pt, PT.FRACTIONAL_PRECISION);
    }

  }

  /**
   * relative to origin without regard to UNITCELL {x y z}
   * 
   * @param pt
   * @param offset
   */
  public void toUnitCell(P3 pt, P3 offset) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell != null)
      unitCell.toUnitCell(pt, offset);
  }

  public void setCurrentCage(String isosurfaceId) {
    Object[] data = new Object[] { isosurfaceId, null };
    shm.getShapePropertyData(JC.SHAPE_ISOSURFACE, "unitCell", data);
    ms.setModelCage(am.cmi, (SymmetryInterface) data[1]);
  }

  public void addUnitCellOffset(P3 pt) {
    SymmetryInterface unitCell = getCurrentUnitCell();
    if (unitCell == null)
      return;
    pt.add(unitCell.getCartesianOffset());
  }

  public void setAtomData(int type, String name, String coordinateData,
                          boolean isDefault) {
    // DATA "xxxx"
    // atom coordinates may be moved here 
    //  but this is not included as an atomMovedCallback
    ms.setAtomData(type, name, coordinateData, isDefault);
    if (type == AtomCollection.TAINT_COORD)
      checkCoordinatesChanged();
    refreshMeasures(true);
  }

  @Override
  public void setCenterSelected() {
    // depricated
    setCenterBitSet(bsA(), true);
  }

  void setApplySymmetryToBonds(boolean TF) {
    g.applySymmetryToBonds = TF;
  }

  @Override
  public void setBondTolerance(float bondTolerance) {
    g.setF("bondTolerance", bondTolerance);
    g.bondTolerance = bondTolerance;
  }

  @Override
  public void setMinBondDistance(float minBondDistance) {
    // PreferencesDialog
    g.setF("minBondDistance", minBondDistance);
    g.minBondDistance = minBondDistance;
  }

  public BS getAtomsNearPt(float distance, P3 coord) {
    BS bs = new BS();
    ms.getAtomsWithin(distance, coord, bs, -1);
    return bs;
  }

  public BS getBranchBitSet(int atomIndex, int atomIndexNot, boolean allowCyclic) {
    if (atomIndex < 0 || atomIndex >= ms.ac)
      return new BS();
    return JmolMolecule.getBranchBitSet(ms.at, atomIndex,
        getModelUndeletedAtomsBitSet(ms.at[atomIndex].mi), null, atomIndexNot,
        allowCyclic, true);
  }

  @Override
  public BS getElementsPresentBitSet(int modelIndex) {
    return ms.getElementsPresentBitSet(modelIndex);
  }

  String getFileHeader() {
    return ms.getFileHeader(am.cmi);
  }

  Object getFileData() {
    return ms.getFileData(am.cmi);
  }

  public Map<String, Object> getCifData(int modelIndex) {
    String data = getFileAsString3(ms.getModelFileName(modelIndex), false, null);
    return (data == null ? null : Rdr.readCifData(
        (GenericCifDataParser) Interface.getInterface(
            "javajs.util.CifDataParser", this, "script"), Rdr.getBR(data)));
  }

  JmolStateCreator jsc;

  public JmolStateCreator getStateCreator() {
    if (jsc == null)
      (jsc = (JmolStateCreator) Interface.getInterface(
          "org.jmol.viewer.StateCreator", this, "script")).setViewer(this);
    return jsc;
  }

  public String getWrappedStateScript() {
    return (String) getOutputManager().getWrappedState(null, null, null, null);
  }

  @Override
  public String getStateInfo() {
    return getStateInfo3(null, 0, 0);
  }

  public String getStateInfo3(String type, int width, int height) {
    return (g.preserveState ? getStateCreator().getStateScript(type, width,
        height) : "");
  }

  public String getStructureState() {
    return getStateCreator().getModelState(null, false, true);
  }

  public String getCoordinateState(BS bsSelected) {
    return getStateCreator().getAtomicPropertyState(AtomCollection.TAINT_COORD,
        bsSelected);
  }

  public void setCurrentColorRange(String label) {
    float[] data = (float[]) getDataObj(label, null, JmolDataManager.DATA_TYPE_AF);
    BS bs = (data == null ? null : (BS) ((Object[])getDataObj(label, null, JmolDataManager.DATA_TYPE_UNKNOWN))[2]);
    if (bs != null && g.rangeSelected)
      bs.and(bsA());
    cm.setPropertyColorRangeData(data, bs);
  }

  private Object[] lastData;

  /**
   * A general-purpose data storage method. Note that matchFieldCount and
   * dataFieldCount should both be positive or both be negative.
   * 
   * @param key
   * 
   *        a simple key name for the data, starting with "property_" if user-defined
   * 
   * @param data
   * 
   *        data[0] -- label
   * 
   *        data[1] -- string or float[] or float[][] or float[][][]
   * 
   *        data[2] -- selection bitset or int[] atomMap when field > 0
   * 
   *        data[3] -- arrayDepth 0(String),1(float[]),2(float[][]),3(float[][][]) or -1
   *        to indidate that it is set by data type
   * 
   *        data[4] -- Boolean.TRUE == saveInState
   * 
   * @param dataType
   * 
   *        see JmolDataManager interface
   * 
   * @param matchField
   * 
   *        if positive, data must match atomNo in this column
   * 
   *        if 0, no match column
   * 
   * @param matchFieldColumnCount
   *        if positive, this number of characters in match column if 0,
   *        reference is to tokens, not characters
   * 
   * @param dataField
   * 
   *        if positive, column containing the data
   * 
   *        if 0, values are a simple list; clear the data
   * 
   *        if Integer.MAX_VALUE, values are a simple list; don't clear the data
   * 
   *        if Integer.MIN_VALUE, have one SINGLE data value for all selected
   *        atoms
   * 
   * @param dataFieldColumnCount
   * 
   *        if positive, this number of characters in data column
   * 
   *        if 0, reference is to tokens, not characters
   */
  public void setData(String key, Object[] data, int dataType,
                      int matchField, int matchFieldColumnCount, int dataField,
                      int dataFieldColumnCount) {
    getDataManager().setData(key, lastData = data, dataType, ms.ac,
        matchField, matchFieldColumnCount, dataField, dataFieldColumnCount);
  }

  /**
   * Retrieve a data object
   * 
   * @param key
   * 
   * @param bsSelected
   * 
   *        selected atoms; for DATA_AF only
   * 
   * @param dataType
   * 
   *        see JmolDataManager interface
   * 
   * @return data object
   * 
   *         data[0] -- label (same as key)
   * 
   *         data[1] -- string or float[] or float[][] or float[][][]
   * 
   *         data[2] -- selection bitset or int[] atomMap when field > 0
   * 
   *         data[3] -- arrayDepth
   *         0(String),1(float[]),2(float[][]),3(float[][][]) or -1 to indicate
   *         that it is set by data type
   * 
   *         data[4] -- Boolean.TRUE == saveInState
   */
  public Object getDataObj(String key, BS bsSelected, int dataType) {
    return (key == null && dataType == JmolDataManager.DATA_TYPE_LAST ? lastData
        : getDataManager().getData(key, bsSelected, dataType));
  }

//  public float getDataFloatAt(String label, int atomIndex) {
//    return getDataManager().getDataFloatAt(label, atomIndex);
//  }

  // boolean autoLoadOrientation() {
  // return true;//global.autoLoadOrientation; 12.0.RC10
  // }

  public int autoHbond(BS bsFrom, BS bsTo, boolean onlyIfHaveCalculated) {
    if (bsFrom == null)
      bsFrom = bsTo = bsA();
    // bsTo null --> use DSSP method further developed 
    // here to give the "defining" Hbond set only
    return ms.autoHbond(bsFrom, bsTo, onlyIfHaveCalculated);
  }

  public SymmetryInterface getCurrentUnitCell() {
    if (am.cai >= 0)
      return ms.getUnitCellForAtom(am.cai);
    int m = am.cmi;
    if (m >= 0)
      return ms.getUnitCell(m);
    BS models = getVisibleFramesBitSet();
    SymmetryInterface ucLast = null;
    for (int i = models.nextSetBit(0); i >= 0; i = models.nextSetBit(i + 1)) {
      SymmetryInterface uc = ms.getUnitCell(i);
      if (uc == null)
        continue;
      if (ucLast == null) {
        ucLast = uc;
        continue;
      }
      if (!ucLast.unitCellEquals(uc))
        return null;
    }
    return ucLast;
  }

  /*
   * ****************************************************************************
   * delegated to MeasurementManager
   * **************************************************************************
   */

  public String getDefaultMeasurementLabel(int nPoints) {
    switch (nPoints) {
    case 2:
      return g.defaultDistanceLabel;
    case 3:
      return g.defaultAngleLabel;
    default:
      return g.defaultTorsionLabel;
    }
  }

  @Override
  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JC.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  @Override
  public String getMeasurementStringValue(int i) {
    return "" + shm.getShapePropertyIndex(JC.SHAPE_MEASURES, "stringValue", i);
  }

  public String getMeasurementInfoAsString() {
    return (String) getShapeProperty(JC.SHAPE_MEASURES, "infostring");
  }

  @Override
  public int[] getMeasurementCountPlusIndices(int i) {
    return (int[]) shm.getShapePropertyIndex(JC.SHAPE_MEASURES,
        "countPlusIndices", i);
  }

  void setPendingMeasurement(MeasurementPending mp) {
    // from MouseManager
    shm.loadShape(JC.SHAPE_MEASURES);
    setShapeProperty(JC.SHAPE_MEASURES, "pending", mp);
  }

  MeasurementPending getPendingMeasurement() {
    return (MeasurementPending) getShapeProperty(JC.SHAPE_MEASURES, "pending");
  }

  public void clearAllMeasurements() {
    // Eval only
    setShapeProperty(JC.SHAPE_MEASURES, "clear", null);
  }

  @Override
  public void clearMeasurements() {
    // depricated but in the API -- use "script" directly
    // see clearAllMeasurements()
    evalString("measures delete");
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to AnimationManager
  // ///////////////////////////////////////////////////////////////

  public void setAnimation(int tok) {
    switch (tok) {
    case T.playrev:
      am.reverseAnimation();
      //$FALL-THROUGH$
    case T.play:
    case T.resume:
      if (!am.animationOn)
        am.resumeAnimation();
      return;
    case T.pause:
      if (am.animationOn && !am.animationPaused)
        am.pauseAnimation();
      return;
    case T.next:
      am.setAnimationNext();
      return;
    case T.prev:
      am.setAnimationPrevious();
      return;
    case T.first:
    case T.rewind:
      am.rewindAnimation();
      return;
    case T.last:
      am.setAnimationLast();
      return;
    }
  }

  @Override
  public void setAnimationFps(int fps) {
    am.setAnimationFps(fps);
  }

  private void setAnimationMode(String mode) {
    if (mode.equalsIgnoreCase("once")) {
      am.setAnimationReplayMode(T.once, 0, 0);
    } else if (mode.equalsIgnoreCase("loop")) {
      am.setAnimationReplayMode(T.loop, 1, 1);
    } else if (mode.startsWith("pal")) {
      am.setAnimationReplayMode(T.palindrome, 1, 1);
    }
  }

  public void setAnimationOn(boolean animationOn) {
    // Eval
    boolean wasAnimating = am.animationOn;
    if (animationOn == wasAnimating)
      return;
    am.setAnimationOn(animationOn);
  }

  public void setAnimationRange(int modelIndex1, int modelIndex2) {
    am.setAnimationRange(modelIndex1, modelIndex2);
  }

  @Override
  public BS getVisibleFramesBitSet() {
    BS bs = BSUtil.copy(am.bsVisibleModels);
    if (ms.trajectory != null)
      ms.trajectory.selectDisplayed(bs);
    return bs;

  }

  public BS getFrameAtoms() {
    return getModelUndeletedAtomsBitSetBs(getVisibleFramesBitSet());
  }

  public void defineAtomSets(Map<String, Object> info) {
    definedAtomSets.putAll(info);
  }

  public void setAnimDisplay(BS bs) {
    am.setDisplay(bs);
    if (!am.animationOn)
      am.morph(am.currentMorphModel + 1);
  }

  public void setCurrentModelIndex(int modelIndex) {
    // Eval
    // initializeModel
    if (modelIndex == Integer.MIN_VALUE) {
      // just forcing popup menu update
      prevFrame = Integer.MIN_VALUE;
      setCurrentModelIndexClear(am.cmi, true);
      return;
    }
    am.setModel(modelIndex, true);
  }

  public String getTrajectoryState() {
    return (ms.trajectory == null ? "" : ms.trajectory.getState());
  }

  public void setFrameOffsets(BS bsAtoms, boolean isFull) {
    tm.bsFrameOffsets = null;
    if (isFull)
      clearModelDependentObjects();
    else
      tm.bsFrameOffsets = bsAtoms;
    tm.frameOffsets = ms.getFrameOffsets(bsAtoms, isFull);
  }

  public void setCurrentModelIndexClear(int modelIndex, boolean clearBackground) {
    // Eval
    // initializeModel
    am.setModel(modelIndex, clearBackground);
  }

  public boolean haveFileSet() {
    return (ms.mc > 1 && getModelNumber(Integer.MAX_VALUE) > 2000000);
  }

  public void setBackgroundModelIndex(int modelIndex) {
    am.setBackgroundModelIndex(modelIndex);
    g.setO("backgroundModel", ms.getModelNumberDotted(modelIndex));
  }

  void setFrameVariables() {
    g.setO("animationMode", T.nameOf(am.animationReplayMode));
    g.setI("animationFps", am.animationFps);
    g.setO("_firstFrame", am.getModelSpecial(-1));
    g.setO("_lastFrame", am.getModelSpecial(1));
    g.setF("_animTimeSec", am.getAnimRunTimeSeconds());
    g.setB("_animMovie", am.isMovie);
  }

  private int motionEventNumber;
  private boolean inMotion;

  public boolean getInMotion(boolean includeAnim) {
    return (inMotion || includeAnim && am.animationOn);
  }

  @Override
  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  @Override
  public void setInMotion(boolean inMotion) {
    if (this.inMotion ^ inMotion) {
      this.inMotion = inMotion;
      resizeImage(0, 0, false, false, true); // for antialiasdisplay
      if (inMotion) {
        startHoverWatcher(false);
        ++motionEventNumber;
      } else {
        startHoverWatcher(true);
        refresh(3, "vwr setInMotion " + inMotion);
      }
    }
  }

  private boolean refreshing = true;

  private void setRefreshing(boolean TF) {
    refreshing = TF;
  }

  public boolean getRefreshing() {
    return refreshing;
  }

  @Override
  public void pushHoldRepaint() {
    pushHoldRepaintWhy(null);
  }

  /**
   * 
   * @param why
   */
  public void pushHoldRepaintWhy(String why) {
    if (rm != null)
      rm.pushHoldRepaint(why);
  }

  @Override
  public void popHoldRepaint(String why) {
    //System.out.println("vwr popHoldRepaint " + why);
    if (rm != null) {
      rm.popHoldRepaint(why.indexOf(JC.REPAINT_IGNORE) < 0, why);
      //System.out.println("vwr popHoldRepaint " + why + " " + ((org.jmol.render.RepaintManager)repaintManager).holdRepaint);
    }
  }

  /**
   * initiate a repaint/update sequence if it has not already been requested.
   * invoked whenever any operation causes changes that require new rendering.
   * 
   * The repaint/update sequence will only be invoked if (a) no repaint is
   * already pending and (b) there is no hold flag set in repaintManager.
   * 
   * Sequence is as follows:
   * 
   * 1) RepaintManager.refresh() checks flags and then calls Viewer.repaint() 2)
   * Viewer.repaint() invokes display.repaint(), provided display is not null
   * (headless) 3) The system responds with an invocation of
   * Jmol.update(Graphics g), which we are routing through Jmol.paint(Graphics
   * g). 4) Jmol.update invokes Viewer.setScreenDimensions(size), which makes
   * the necessary changes in parameters for any new window size. 5) Jmol.update
   * invokes Viewer.renderScreenImage(g, size, rectClip) 6)
   * Viewer.renderScreenImage checks object visibility, invokes render1 to do
   * the actual creation of the image pixel map and send it to the screen, and
   * then invokes repaintView() 7) Viewer.repaintView() invokes
   * RepaintManager.repaintDone(), to clear the flags and then use notify() to
   * release any threads holding on wait().
   * 
   * @param mode
   * @param strWhy
   * 
   */
  @Override
  public void refresh(int mode, String strWhy) {
    // refresh(1) is used by operations to ONLY do a repaint -- no syncing
    // refresh(2) indicates this is a mouse motion requiring synchronization
    //            -- not going through Eval so we bypass Eval and mainline on the other vwr!
    // refresh(3) same as 1, but not WebGL
    // refresh(6) is used to do no refresh if in motion
    // refresh(7) is used to send JavaScript a "new orientation" command
    //            for example, at the end of a script
    if (rm == null || !refreshing)
      return;
    if (mode == 6 && getInMotion(true))
      return;
    if (isWebGL) {
      if (mode == 1 || mode == 2 || mode == 7) {
        tm.finalizeTransformParameters();

        /**
         * @j2sNative
         * 
         *            if (!this.html5Applet) return;
         *            this.html5Applet._refresh();
         */
        {
        }
      }
    } else {
      if (mode > 0 && mode != 7)
        rm.repaintIfReady("refresh " + mode + " " + strWhy);
    }
    if (mode != 7 && mode % 3 != 0 && sm.doSync())
      sm.setSync(mode == 2 ? strWhy : null);
  }

  public void requestRepaintAndWait(String why) {
    // called by moveUpdate from move, moveTo, navigate,
    // navTranslate
    // called by ScriptEvaluator "refresh" command
    // called by AnimationThread run()
    // called by TransformationManager move and moveTo
    // called by TransformationManager11 navigate, navigateTo
    if (rm == null)
      return;
    if (!haveDisplay) {
      setModelVisibility();
      shm.finalizeAtoms(false, true);
      return;
    }
    rm.requestRepaintAndWait(why);
    setSync();
  }

  public void clearShapeRenderers() {
    clearRepaintManager(-1);
  }

  public boolean isRepaintPending() {
    return (rm == null ? false : rm.isRepaintPending());
  }

  @Override
  public void notifyViewerRepaintDone() {
    if (rm != null)
      rm.repaintDone();
    am.repaintDone();
  }

  private boolean axesAreTainted = false;

  public boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  // //////////// screen/image methods ///////////////

  // final Rectangle rectClip = new Rectangle();

  private int maximumSize = Integer.MAX_VALUE;

  private void setMaximumSize(int x) {
    maximumSize = Math.max(x, 100);
  }

  @Override
  public void setScreenDimension(int width, int height) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    height = Math.min(height, maximumSize);
    width = Math.min(width, maximumSize);
    if (tm.stereoDoubleFull)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    resizeImage(width, height, false, false, true);
  }

  /**
   * A graphics from a "slave" stereo display that has been synchronized with
   * this this applet.
   */
  private Object gRight;

  /**
   * A flag to indicate that THIS is the right-side panel of a pair of synced
   * applets running a left-right stereo display (that would be piped into a
   * dual-image polarized projector system such as GeoWall).
   * 
   */
  private boolean isStereoSlave;

  public void setStereo(boolean isStereoSlave, Object gRight) {
    this.isStereoSlave = isStereoSlave;
    this.gRight = gRight;
  }

  public float imageFontScaling = 1;

  void resizeImage(int width, int height, boolean isImageWrite,
                   boolean isExport, boolean isReset) {
    if (!isImageWrite && creatingImage)
      return;
    boolean wasAntialiased = antialiased;
    antialiased = (isReset ? g.antialiasDisplay
        && checkMotionRendering(T.antialiasdisplay)
        : isImageWrite && !isExport ? g.antialiasImages : false);
    if (!isExport && !isImageWrite && (width > 0 || wasAntialiased != antialiased))
      setShapeProperty(JC.SHAPE_LABELS, "clearBoxes", null);
    imageFontScaling = (antialiased ? 2f : 1f) * (isReset || width <= 0 ? 1
        : (g.zoomLarge == (height > width) ? height : width) / getScreenDim());
    if (width > 0) {
      dimScreen.width = width;
      dimScreen.height = height;
      if (!isImageWrite) {
        g.setI("_width", width);
        g.setI("_height", height);
        //        setStatusResized(width, height);
      }
    } else {
      width = (dimScreen.width == 0 ? dimScreen.width = 500 : dimScreen.width);
      height = (dimScreen.height == 0 ? dimScreen.height = 500
          : dimScreen.height);
    }
    tm.setScreenParameters(width, height, isImageWrite || isReset ? g.zoomLarge
        : false, antialiased, false, false);
    gdata.setWindowParameters(width, height, antialiased);
    if (width > 0 && !isImageWrite)
      setStatusResized(width, height);
  }

  @Override
  public int getScreenWidth() {
    return dimScreen.width;
  }

  @Override
  public int getScreenHeight() {
    return dimScreen.height;
  }

  public int getScreenDim() {
    return (g.zoomLarge == (dimScreen.height > dimScreen.width) ? dimScreen.height
        : dimScreen.width);
  }

  @Override
  public String generateOutputForExport(Map<String, Object> params) {
    return (noGraphicsAllowed || rm == null ? null : getOutputManager()
        .getOutputFromExport(params));
  }

  private void clearRepaintManager(int iShape) {
    if (rm != null)
      rm.clear(iShape);
  }

  public void renderScreenImageStereo(Object gLeft, boolean checkStereoSlave,
                                      int width, int height) {
    // from paint/update event
    // gRight is for second stereo applet
    // when this is the stereoSlave, no rendering occurs through this applet
    // directly, only from the other applet.
    // this is for relatively specialized geoWall-type installations

    //      Jmol repaint/update system for the application:
    //      
    //      threads invoke vwr.refresh()
    //       
    //        --> repaintManager.refresh()
    //        --> vwr.repaint() 
    //        --> display.repaint()
    //        --> OS event queue calls Applet.paint() 
    //        --> vwr.renderScreenImage() 
    //        --> vwr.notifyViewerRepaintDone() 
    //        --> repaintManager.repaintDone()
    //        --> which sets repaintPending false and does notify();

    //System.out.println(Thread.currentThread() + "render Screen Image " +
    // creatingImage);
    //System.out.println("viewer render");
    if (updateWindow(width, height)) {
      if (!checkStereoSlave || gRight == null) {
        getScreenImageBuffer(gLeft, false);
      } else {
        drawImage(gRight, getImage(true, false), 0, 0, tm.stereoDoubleDTI);
        drawImage(gLeft, getImage(false, false), 0, 0, tm.stereoDoubleDTI);
      }
      //System.out.println(Thread.currentThread() +
      // "notifying repaintManager repaint is done");
    }
    if (captureParams != null
        && Boolean.FALSE != captureParams.get("captureEnabled")) {
      //showString(transformManager.matrixRotate.toString(), false);
      long t = ((Long) captureParams.get("endTime")).longValue();
      if (t > 0 && System.currentTimeMillis() + 50 > t)
        captureParams.put("captureMode", "end");
      processWriteOrCapture(captureParams);
    }
    notifyViewerRepaintDone();
  }

  public Map<String, Object> captureParams;
  private Map<String, Object> jsParams;

  /**
   * for JavaScript only
   * 
   */
  public void updateJS() {
    if (isWebGL) {
      if (jsParams == null) {
        jsParams = new Hashtable<String, Object>();
        jsParams.put("type", "JS");
      }
      if (updateWindow(0, 0))
        render();
      notifyViewerRepaintDone();
    } else {
      if (isStereoSlave)
        return;
      // getGraphics returns a canvas context2d
      renderScreenImageStereo(apiPlatform.getGraphics(null), true, 0, 0);
    }
  }

  /**
   * File has been loaded or model has been changed or atom picked. This is a
   * call to Jmol.View for view sets (new in Jmol 14.1.8)
   * 
   * @param imodel
   * @param iatom
   * 
   */
  private void updateJSView(int imodel, int iatom) {
    @SuppressWarnings("unused")
    Object applet = this.html5Applet;
    /**
     * @j2sNative
     * 
     *            applet && applet._viewSet != null &&
     *            applet._atomPickedCallback(imodel, iatom);
     * 
     */
    {
    }
  }

  private boolean updateWindow(int width, int height) {
    //System.out.println("Viewer updateWindow " + width + " " + height);
    if (!refreshing || creatingImage)
      return (refreshing ? false : !isJS);
    if (isTainted || tm.slabEnabled)
      setModelVisibility();
    isTainted = false;
    if (rm != null) {
      if (width != 0)
        setScreenDimension(width, height);
    }
    return true;
  }

  /**
   * JmolViewer interface uses this, but that is all
   */
  @Override
  public void renderScreenImage(Object g, int width, int height) {
    renderScreenImageStereo(g, false, width, height);
  }

  /**
   * 
   * @param isDouble
   * @param isImageWrite
   *        TODO
   * @return a java.awt.Image in the case of standard Jmol; an int[] in the case
   *         of Jmol-Android a canvas in the case of JSmol
   */
  private Object getImage(boolean isDouble, boolean isImageWrite) {
    Object image = null;
    try {
      beginRendering(isDouble, isImageWrite);
      render();
      gdata.endRendering();
      image = gdata.getScreenImage(isImageWrite);
    } catch (Error er) {
      gdata.getScreenImage(isImageWrite);
      handleError(er, false);
      setErrorMessage("Error during rendering: " + er, null);
    } catch (Exception e) {
      System.out.println("render error" + e);
      if (!isJS)
        e.printStackTrace();
    }
    return image;
  }

  private void beginRendering(boolean isDouble, boolean isImageWrite) {
    gdata.beginRendering(tm.getStereoRotationMatrix(isDouble), g.translucent,
        isImageWrite, !checkMotionRendering(T.translucent));
  }

  public boolean antialiased;

  private void render() {
    if (mm.modelSet == null || !mustRender || !refreshing && !creatingImage
        || rm == null)
      return;
    boolean antialias2 = antialiased && g.antialiasTranslucent;
    int[] navMinMax = shm.finalizeAtoms(true, true);
    if (isWebGL) {
      rm.renderExport(gdata, ms, jsParams);
      notifyViewerRepaintDone();
      return;
    }
    rm.render(gdata, ms, true, navMinMax);
    if (gdata.setPass2(antialias2)) {
      tm.setAntialias(antialias2);
      rm.render(gdata, ms, false, null);
      tm.setAntialias(antialiased);
    }
  }

  /**
   * 
   * @param graphic
   *        In JavaScript/HTML5, a Canvas.Context2d
   * @param img
   * @param x
   * @param y
   * @param isDTI DTI format -- scrunch width by factor of two
   */
  private void drawImage(Object graphic, Object img, int x, int y, boolean isDTI) {
    if (graphic != null && img != null) {
      apiPlatform.drawImage(graphic, img, x, y, dimScreen.width,
          dimScreen.height, isDTI);
    }
    gdata.releaseScreenImage();
  }

  /**
   * Image.getJpgImage, ImageCreator.clipImage, getImageBytes,
   * Viewer.renderScreenImageStereo
   */
  @Override
  public Object getScreenImageBuffer(Object graphic, boolean isImageWrite) {
    if (isWebGL)
      return (isImageWrite ? apiPlatform.allocateRgbImage(0, 0, null, 0, false, true) : null);
    boolean isDouble = tm.stereoDoubleFull || tm.stereoDoubleDTI;
    boolean mergeImages = (graphic == null && isDouble);
    Object imageBuffer;
    if (tm.stereoMode.isBiColor()) {
      beginRendering(true, isImageWrite);
      render();
      gdata.endRendering();
      gdata.snapshotAnaglyphChannelBytes();
      beginRendering(false, isImageWrite);
      render();
      gdata.endRendering();
      gdata.applyAnaglygh(tm.stereoMode, tm.stereoColors);
      imageBuffer = gdata.getScreenImage(isImageWrite);
    } else {
      imageBuffer = getImage(isDouble, isImageWrite);
    }
    Object imageBuffer2 = null;
    if (mergeImages) {
      imageBuffer2 = apiPlatform.newBufferedImage(imageBuffer,
          (tm.stereoDoubleDTI ? dimScreen.width : dimScreen.width << 1), dimScreen.height);
      graphic = apiPlatform.getGraphics(imageBuffer2);
    }
    if (graphic != null) {
      if (isDouble) {
        if (tm.stereoMode == STER.DTI) {
          drawImage(graphic, imageBuffer, dimScreen.width >> 1, 0, true);
          imageBuffer = getImage(false, false);
          drawImage(graphic, imageBuffer, 0, 0, true);        
          graphic = null;
        } else {
        drawImage(graphic, imageBuffer, dimScreen.width, 0, false);
        imageBuffer = getImage(false, false);
        }
      }
      if (graphic != null)
        drawImage(graphic, imageBuffer, 0, 0, false);        
    }
    return (mergeImages ? imageBuffer2 : imageBuffer);
  }

  /**
   * @return byte[] image, or null and an error message
   */
  @Override
  public byte[] getImageAsBytes(String type, int width, int height,
                                int quality, String[] errMsg) {
    return getOutputManager().getImageAsBytes(type, width, height, quality, errMsg);
  }

  @Override
  public void releaseScreenImage() {
    gdata.releaseScreenImage();
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  @Override
  public String evalFile(String strFilename) {
    // from JmolApp and test suite only
    return (allowScripting && getScriptManager() != null ? scm
        .evalFile(strFilename) : null);
  }

  public String getInsertedCommand() {
    String s = insertedCommand;
    insertedCommand = "";
    if (Logger.debugging && s != "")
      Logger.debug("inserting: " + s);
    return s;
  }

  @Override
  public String script(String strScript) {
    // JmolViewer -- just an alias for evalString
    return evalStringQuietSync(strScript, false, true);
  }

  @Override
  public String evalString(String strScript) {
    // JmolSimpleViewer
    return evalStringQuietSync(strScript, false, true);
  }

  @Override
  public String evalStringQuiet(String strScript) {
    // JmolViewer 
    return evalStringQuietSync(strScript, true, true);
  }

  public String evalStringQuietSync(String strScript, boolean isQuiet,
                                    boolean allowSyncScript) {
    return (getScriptManager() == null ? null : scm.evalStringQuietSync(
        strScript, isQuiet, allowSyncScript));
  }

  public void clearScriptQueue() {
    if (scm != null)
      scm.clearQueue();
  }

  private void setScriptQueue(boolean TF) {
    g.useScriptQueue = TF;
    if (!TF)
      clearScriptQueue();
  }

  @Override
  public boolean checkHalt(String str, boolean isInsert) {
    return (scm != null && scm.checkHalt(str, isInsert));
  }

  // / direct no-queue use:

  @Override
  public String scriptWait(String strScript) {
    return (String) evalWait("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated");
  }

  @Override
  public Object scriptWaitStatus(String strScript, String statusList) {
    // null statusList will return a String 
    //  -- output from PRINT/MESSAGE/ECHO commands or an error message
    // otherwise, specific status messages will be created as a Java object
    return evalWait("object", strScript, statusList);
  }

  private Object evalWait(String returnType, String strScript, String statusList) {
    //can't do waitForQueue in JavaScript and then wait for the queue:
    if (getScriptManager() == null)
      return null;
    scm.waitForQueue();
    boolean doTranslateTemp = GT.setDoTranslate(false);
    Object ret = evalStringWaitStatusQueued(returnType, strScript, statusList,
        false, false);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
  }

  public synchronized Object evalStringWaitStatusQueued(String returnType,
                                                        String strScript,
                                                        String statusList,
                                                        boolean isQuiet,
                                                        boolean isQueued) {
    /**
     * @j2sNative
     * 
     *            if (strScript.indexOf("JSCONSOLE") == 0) {
     *            this.html5Applet._showInfo(strScript.indexOf("CLOSE")<0); if
     *            (strScript.indexOf("CLEAR") >= 0)
     *            this.html5Applet._clearConsole(); return null; }
     */
    {
    }
    return (getScriptManager() == null ? null : scm.evalStringWaitStatusQueued(
        returnType, strScript, statusList, isQuiet, isQueued));
  }

  public void exitJmol() {
    if (isApplet && !isJNLP)
      return;
    if (headlessImageParams != null) {
      try {
        if (headless)
          outputToFile(headlessImageParams);
      } catch (Exception e) {
        //
      }
    }

    if (Logger.debugging)
      Logger.debug("exitJmol -- exiting");
    System.out.flush();
    System.exit(0);
  }

  private Object scriptCheckRet(String strScript, boolean returnContext) {
    return (getScriptManager() == null ? null : scm.scriptCheckRet(strScript, returnContext));
  }

  @Override
  public synchronized Object scriptCheck(String strScript) {
    return (getScriptManager() == null ? null : scriptCheckRet(strScript, false));
  }

  @Override
  public boolean isScriptExecuting() {
    return (eval != null && eval.isExecuting());
  }

  @Override
  public void haltScriptExecution() {
    if (eval != null) {
      eval.haltExecution();
      eval.stopScriptThreads();
    }
    setStringPropertyTok("pathForAllFiles", T.pathforallfiles, "");
    clearTimeouts();
  }

  public void pauseScriptExecution() {
    if (eval != null)
      eval.pauseExecution(true);
  }

  String resolveDatabaseFormat(String fileName) {
    if (hasDatabasePrefix(fileName))
      fileName = (String) setLoadFormat(fileName, fileName.charAt(0), false);
    return fileName;
  }

  public static boolean hasDatabasePrefix(String fileName) {
    return (fileName.length() != 0 && isDatabaseCode(fileName.charAt(0)));
  }

  public static boolean isDatabaseCode(char ch) {
    return (ch == '*' // PDBE
        || ch == '$' // NCI resolver
        || ch == '=' // RCSB model or ligand
    || ch == ':' // PubChem
    );
  }

  /**
   * Jmol will either specify a type or look for it in the first character,
   * making sure it is found using isDatabaseCode() first. Starting with Jmol
   * 13.1.13, we allow a generalized search using =xxx= where xxx is a known or
   * user-specified database designation.
   * 
   * @param name
   * @param type
   * @param withPrefix
   * @return String or String[]
   */
  public Object setLoadFormat(String name, char type, boolean withPrefix) {
    String format = null;
    String f = name.substring(1);
    switch (type) {
    case '=':
      if (name.startsWith("==")) {
        f = f.substring(1);
        type = '#';
      } else if (f.indexOf("/") > 0) {
        // =xxxx/....
        try {
          int pt = f.indexOf("/");
          String database = f.substring(0, pt);
          f = g.resolveDataBase(database, f.substring(pt + 1), null);
          if (f != null && f.startsWith("'"))
            f = evaluateExpression(f).toString();
          return (f == null || f.length() == 0 ? name : f);
        } catch (Exception e) {
          return name;
        }
      } else {
        format = ( 
            // following is temporary, until issues are resolved for AJAX asych
            isJS && g.loadFormat.equals(g.pdbLoadFormat) ? g.pdbLoadFormat0 
                : g.loadFormat);
      }
      //$FALL-THROUGH$
    case '#': // ligand
      if (format == null)
        format = g.pdbLoadLigandFormat;
      if (f.indexOf(".") >= 0 && format.equals(g.pdbLoadFormat))
          format = g.pdbLoadFormat0; // older version for =1crn.cif or  =1crn.pdb
      return g.resolveDataBase(null, f, format);
    case '*':
      // European Bioinformatics Institute
      int pt = name.lastIndexOf("/");
      if (name.startsWith("*dom/")) {
        //  *dom/.../.../.../xxxx
        f = name.substring(pt + 1);
        format = (pt > 4 ? name.substring(5) : "mappings");
        return PT.rep(g.resolveDataBase("map", f, null), "%TYPE", format);
      } else if (name.startsWith("*val/")) {
        //  *val/.../.../.../xxxx
        f = name.substring(pt + 1);
        format = (pt > 4 ? name.substring(5) : "validation/outliers/all");
        return PT.rep(g.resolveDataBase("map", f, null), "%TYPE", format);
      } else if (name.startsWith("*rna3d/")) {
        //  *rna3d/.../.../.../xxxx
        f = name.substring(pt + 1);
        format = (pt > 6 ? name.substring(6) : "loops");
        return PT.rep(g.resolveDataBase("rna3d", f, null), "%TYPE", format);
      } else if (name.startsWith("*dssr/")) {
        f = name.substring(pt + 1);
        return g.resolveDataBase("dssr", f, null);
      } else if (name.startsWith("*dssr1/")) {
        f = name.substring(pt + 1);
        return g.resolveDataBase("dssr1", f, null);
      }
      // these are processed in SmarterJmolAdapter
      String pdbe = "pdbe";
      if (f.length() == 5 && f.charAt(4) == '*') {
        pdbe = "pdbe2";
        f = f.substring(0, 4);
      }
      return g.resolveDataBase(pdbe, f, null);
    case ':': // PubChem
      format = g.pubChemFormat;
      if (f.equals("")) {
        try {
          f = "smiles:" + getSmiles(bsA());
        } catch (Exception e) {
          // oh well.
        }
      }
      String fl = f.toLowerCase();
      int fi = Integer.MIN_VALUE;
      try {
        fi = Integer.parseInt(f);
      } catch (Exception e) {
        //
      }
      if (fi != Integer.MIN_VALUE) {
        f = "cid/" + fi;
      } else {
        if (fl.startsWith("smiles:")) {
          format += "?POST?smiles=" + f.substring(7);
          f = "smiles";
        } else if (f.startsWith("cid:") || f.startsWith("inchikey:")
            || f.startsWith("cas:")) {
          f = f.replace(':', '/');
        } else {
          if (fl.startsWith("name:"))
            f = f.substring(5);
          f = "name/" + PT.escapeUrl(f);
        }
      }
      return PT.formatStringS(format, "FILE", f);
    case '$':
      if (name.startsWith("$$")) {
        // 2D version
        f = f.substring(1);

        //http://cactus.nci.nih.gov/chemical/structure/C%28O%29CCC/file?format=sdf
        format = PT.rep(g.smilesUrlFormat, "&get3d=True", "");
        return PT.formatStringS(format, "FILE", PT.escapeUrl(f));
      }
      if (name.equals("$"))
        try {
          f = getSmiles(bsA());
        } catch (Exception e) {
          // oh well...
        }
      //$FALL-THROUGH$
    case 'N':
    case '2':
    case 'I':
    case 'K':
    case 'S':
    case 'T':
    case '/':
      f = PT.escapeUrl(f);
      switch (type) {
      case 'N':
        format = g.nihResolverFormat + "/names";
        break;
      case '2':
        format = g.nihResolverFormat + "/image";
        break;
      case 'I':
        format = g.nihResolverFormat + "/stdinchi";
        break;
      case 'K':
        format = g.nihResolverFormat + "/inchikey";
        break;
      case 'S':
        format = g.nihResolverFormat + "/stdinchikey";
        break;
      case 'T':
        format = g.nihResolverFormat + "/stdinchi";
        break;
      case '/':
        format = g.nihResolverFormat + "/";
        break;
      default:
        format = g.smilesUrlFormat;
        break;
      }
      return (withPrefix ? "MOL3D::" : "")
          + PT.formatStringS(format, "FILE", f);
    case '_': // isosurface "=...", but we code that type as '_'
      boolean isDiff = f.startsWith("=");
      if (isDiff)
        f = f.substring(1);
      String server = FileManager.fixFileNameVariables(
          isDiff ? g.edsUrlFormatDiff : g.edsUrlFormat, f);
      String strCutoff = (isDiff ? "" : FileManager.fixFileNameVariables(
          g.edsUrlCutoff, f));
      return new String[] { server, strCutoff, isDiff ? "diff" : null };
    }
    return f;
  }

  public String getStandardLabelFormat(int type) {
    switch (type) {
    default:
    case 0: // standard
      return LabelToken.STANDARD_LABEL;
    case 1:
      return g.defaultLabelXYZ;
    case 2:
      return g.defaultLabelPDB;
    }
  }

  public P3[] getAdditionalHydrogens(BS bsAtoms, boolean doAll,
                                     boolean justCarbon, Lst<Atom> vConnections) {
    if (bsAtoms == null)
      bsAtoms = bsA();
    int[] nTotal = new int[1];
    P3[][] pts = ms.calculateHydrogens(bsAtoms, nTotal, doAll, justCarbon,
        vConnections);
    P3[] points = new P3[nTotal[0]];
    for (int i = 0, pt = 0; i < pts.length; i++)
      if (pts[i] != null)
        for (int j = 0; j < pts[i].length; j++)
          points[pt++] = pts[i][j];
    return points;
  }

  @Override
  public void setMarBond(short marBond) {
    g.bondRadiusMilliAngstroms = marBond;
    g.setI("bondRadiusMilliAngstroms", marBond);
    setShapeSize(JC.SHAPE_STICKS, marBond * 2, BSUtil.setAll(ms.ac));
  }

  private int hoverAtomIndex = -1;
  private String hoverText, hoverLabel = "%U";
  private boolean hoverEnabled = true;

  public void setHoverLabel(String strLabel) {
    shm.loadShape(JC.SHAPE_HOVER);
    setShapeProperty(JC.SHAPE_HOVER, "label", strLabel);
    setHoverEnabled(strLabel != null);
    g.setO("_hoverLabel", hoverLabel = strLabel);
    if (!hoverEnabled && !sm.haveHoverCallback())
      startHoverWatcher(false);
  }
  
  private void setHoverEnabled(boolean tf) {
    hoverEnabled = tf;
    g.setB("_hoverEnabled", tf);
  }

  /*
   * hoverCallback reports information about the atom being hovered over.
   * 
   * jmolSetCallback("hoverCallback", "myHoverCallback") function
   * myHoverCallback(strInfo, iAtom) {}
   * 
   * strInfo == the atom's identity, including x, y, and z coordinates iAtom ==
   * the index of the atom being hovered over
   * 
   * Viewer.setStatusAtomHovered Hover.setProperty("target") Viewer.hoverOff
   * Viewer.hoverOn
   */
  void hoverOn(int atomIndex, boolean isLabel) {
    g.removeParam("_objecthovered");
    g.setI("_atomhovered", atomIndex);
    g.setO("_hoverLabel", hoverLabel);
    g.setUserVariable("hovered", SV.getVariable(BSUtil.newAndSetBit(atomIndex)));
    if (sm.haveHoverCallback())
      sm.setStatusAtomHovered(atomIndex, getAtomInfoXYZ(atomIndex, false));
    if (!hoverEnabled)
      return;
    if (g.modelKitMode) {
      if (ms.isAtomAssignable(atomIndex))
        highlight(BSUtil.newAndSetBit(atomIndex));
      refresh(3, "hover on atom");
      return;
    }
    if (eval != null && isScriptExecuting() || atomIndex == hoverAtomIndex
        || g.hoverDelayMs == 0)
      return;
    if (!slm.isInSelectionSubset(atomIndex))
      return;
    shm.loadShape(JC.SHAPE_HOVER);
    if (isLabel && ms.at[atomIndex].isVisible(JC.VIS_LABEL_FLAG)) {
      setShapeProperty(JC.SHAPE_HOVER, "specialLabel",
          GT._("Drag to move label"));
    }
    setShapeProperty(JC.SHAPE_HOVER, "text", null);
    setShapeProperty(JC.SHAPE_HOVER, "target", Integer.valueOf(atomIndex));
    hoverText = null;
    hoverAtomIndex = atomIndex;
    refresh(3, "hover on atom");
  }

  public void hoverOnPt(int x, int y, String text, String id, T3 pt) {
    // from draw for drawhover on
    if (eval != null && isScriptExecuting())
      return;
    g.setO("_hoverLabel", hoverLabel);
    if (id != null && pt != null) {
      g.setO("_objecthovered", id);
      g.setI("_atomhovered", -1);
      g.setUserVariable("hovered", SV.getVariable(pt));
      if (sm.haveHoverCallback())
        sm.setStatusObjectHovered(id, text, pt);
    }
    if (!hoverEnabled)
      return;
    shm.loadShape(JC.SHAPE_HOVER);
    setShapeProperty(JC.SHAPE_HOVER, "xy", P3i.new3(x, y, 0));
    setShapeProperty(JC.SHAPE_HOVER, "target", null);
    setShapeProperty(JC.SHAPE_HOVER, "specialLabel", null);
    setShapeProperty(JC.SHAPE_HOVER, "text", text);
    hoverAtomIndex = -1;
    hoverText = text;
    refresh(3, "hover on point");
  }

  void hoverOff() {
    try {
      if (g.modelKitMode)
        highlight(null);
      if (!hoverEnabled)
        return;
      boolean isHover = (hoverText != null || hoverAtomIndex >= 0);
      if (hoverAtomIndex >= 0) {
        setShapeProperty(JC.SHAPE_HOVER, "target", null);
        hoverAtomIndex = -1;
      }
      if (hoverText != null) {
        setShapeProperty(JC.SHAPE_HOVER, "text", null);
        hoverText = null;
      }
      setShapeProperty(JC.SHAPE_HOVER, "specialLabel", null);
      if (isHover)
        refresh(3, "hover off");
    } catch (Exception e) {
      // ignore
    }
  }

  @Override
  public void setDebugScript(boolean debugScript) {
    g.debugScript = debugScript;
    g.setB("debugScript", debugScript);
    if (eval != null)
      eval.setDebugging();
  }

  void clearClickCount() {
    setTainted(true);
  }

  int currentCursor = GenericPlatform.CURSOR_DEFAULT;

  public void setCursor(int cursor) {
    if (isKiosk || currentCursor == cursor || multiTouch || !haveDisplay)
      return;
    apiPlatform.setCursor(currentCursor = cursor, display);
  }

  void setPickingMode(String strMode, int pickingMode) {
    if (!haveDisplay)
      return;
    showSelected = false;
    String option = null;
    if (strMode != null) {
      int pt = strMode.indexOf("_");
      if (pt >= 0) {
        option = strMode.substring(pt + 1);
        strMode = strMode.substring(0, pt);
      }
      pickingMode = ActionManager.getPickingMode(strMode);
    }
    if (pickingMode < 0)
      pickingMode = ActionManager.PICKING_IDENTIFY;
    acm.setPickingMode(pickingMode);
    g.setO("picking",
        ActionManager.getPickingModeName(acm.getAtomPickingMode()));
    if (option == null || option.length() == 0)
      return;
    option = Character.toUpperCase(option.charAt(0))
        + (option.length() == 1 ? "" : option.substring(1, 2));
    switch (pickingMode) {
    case ActionManager.PICKING_ASSIGN_ATOM:
      setAtomPickingOption(option);
      break;
    case ActionManager.PICKING_ASSIGN_BOND:
      setBondPickingOption(option);
      break;
    default:
      Logger.error("Bad picking mode: " + strMode + "_" + option);
    }
  }

  public int getPickingMode() {
    return (haveDisplay ? acm.getAtomPickingMode() : 0);
  }

  void setPickingStyle(String style, int pickingStyle) {
    if (!haveDisplay)
      return;
    if (style != null)
      pickingStyle = ActionManager.getPickingStyleIndex(style);
    if (pickingStyle < 0)
      pickingStyle = ActionManager.PICKINGSTYLE_SELECT_JMOL;
    acm.setPickingStyle(pickingStyle);
    g.setO("pickingStyle",
        ActionManager.getPickingStyleName(acm.getPickingStyle()));
  }

  public boolean getDrawHover() {
    return haveDisplay && g.drawHover;
  }

  private P3 ptTemp;

  public String getAtomInfo(int atomOrPointIndex) {
    if (ptTemp == null)
      ptTemp = new P3();
    // only for MeasurementTable and actionManager
    return (atomOrPointIndex >= 0 ? ms.getAtomInfo(atomOrPointIndex, null,
        ptTemp) : (String) shm.getShapePropertyIndex(JC.SHAPE_MEASURES,
        "pointInfo", -atomOrPointIndex));
  }

  private String getAtomInfoXYZ(int atomIndex, boolean useChimeFormat) {
    Atom atom = ms.at[atomIndex];
    if (useChimeFormat)
      return getChimeMessenger().getInfoXYZ(atom);
    if (ptTemp == null)
      ptTemp = new P3();
    return atom.getIdentityXYZ(true, ptTemp);
  }

  // //////////////status manager dispatch//////////////

  private void setSync() {
    if (sm.doSync())
      sm.setSync(null);
  }

  @Override
  public void setJmolCallbackListener(JmolCallbackListener listener) {
    sm.cbl = listener;
  }

  @Override
  public void setJmolStatusListener(JmolStatusListener listener) {
    sm.cbl = sm.jsl = listener;
  }

  public Lst<Lst<Lst<Object>>> getStatusChanged(String statusNameList) {
    return (statusNameList == null ? null : sm.getStatusChanged(statusNameList));
  }

  public boolean menuEnabled() {
    return (!g.disablePopupMenu && getPopupMenu() != null);
  }

  void popupMenu(int x, int y, char type) {
    if (!haveDisplay || !refreshing || isPreviewOnly || g.disablePopupMenu)
      return;
    switch (type) {
    case 'j':
      try {
        getPopupMenu();
        // can throw error if not present; that's ok
        jmolpopup.jpiShow(x, y);
      } catch (Throwable e) {
        // no Swing -- tough luck!
        Logger.info(e.toString());
        g.disablePopupMenu = true;
      }
      break;
    case 'a':
    case 'b':
    case 'm':
      // atom, bond, or main -- ignored      
      if (modelkitPopup == null
          && (modelkitPopup = apiPlatform.getMenuPopup(null, type)) == null)
        return;
      modelkitPopup.jpiShow(x, y);
      break;
    }
  }

  public String getMenu(String type) {
    getPopupMenu();
    if (type.equals("\0")) {
      popupMenu(dimScreen.width - 120, 0, 'j');
      return "OK";
    }
    return (jmolpopup == null ? "" : jmolpopup
        .jpiGetMenuAsString("Jmol version " + getJmolVersion() + "|_GET_MENU|"
            + type));
  }

  private Object getPopupMenu() {
    if (g.disablePopupMenu)
      return null;
    if (jmolpopup == null) {
      jmolpopup = (allowScripting ? apiPlatform
          .getMenuPopup(menuStructure, 'j') : null);
      if (jmolpopup == null && !async) {
        g.disablePopupMenu = true;
        return null;
      }
    }
    return jmolpopup.jpiGetMenuAsObject();
  }

  @Override
  public void setMenu(String fileOrText, boolean isFile) {
    if (isFile)
      Logger.info("Setting menu "
          + (fileOrText.length() == 0 ? "to Jmol defaults" : "from file "
              + fileOrText));
    if (fileOrText.length() == 0)
      fileOrText = null;
    else if (isFile)
      fileOrText = getFileAsString3(fileOrText, false, null);
    getProperty("DATA_API", "setMenu", fileOrText);
    sm.setCallbackFunction("menu", fileOrText);
  }

  // // JavaScript callback methods for the applet

  /*
   * 
   * animFrameCallback echoCallback (defaults to messageCallback) errorCallback
   * evalCallback hoverCallback loadStructCallback measureCallback (defaults to
   * messageCallback) messageCallback (no local version) minimizationCallback
   * pickCallback resizeCallback scriptCallback (defaults to messageCallback)
   * syncCallback
   */

  /*
   * aniframeCallback is called:
   * 
   * -- each time a frame is changed -- whenever the animation state is changed
   * -- whenever the visible frame range is changed
   * 
   * jmolSetCallback("animFrameCallback", "myAnimFrameCallback") function
   * myAnimFrameCallback(frameNo, fileNo, modelNo, firstNo, lastNo) {}
   * 
   * frameNo == the current frame in fileNo == the current file number, starting
   * at 1 modelNo == the current model number in the current file, starting at 1
   * firstNo == flag1 * (the first frame of the set, in file * 1000000 + model
   * notation) lastNo == flag2 * (the last frame of the set, in file * 1000000 +
   * model notation)
   * 
   * where flag1 = 1 if animationDirection > 1 or -1 otherwise where flag2 = 1
   * if currentDirection > 1 or -1 otherwise
   * 
   * RepaintManager.setStatusFrameChanged RepaintManager.setAnimationOff
   * RepaintManager.setCurrentModelIndex RepaintManager.clearAnimation
   * RepaintManager.rewindAnimation RepaintManager.setAnimationLast
   * RepaintManager.setAnimationRelative RepaintManager.setFrameRangeVisible
   * Viewer.setCurrentModelIndex Eval.file Eval.frame Eval.load
   * Viewer.createImage (when creating movie frames with the WRITE FRAMES
   * command) Viewer.initializeModel
   */

  private int prevFrame = Integer.MIN_VALUE;
  private float prevMorphModel;

  /**
   * @param isVib
   * @param doNotify
   *        ignored; not implemented
   */
  void setStatusFrameChanged(boolean isVib, boolean doNotify) {
    if (isVib) {
      // force reset (reading vibrations)
      prevFrame = Integer.MIN_VALUE;
    }
    tm.setVibrationPeriod(Float.NaN);
    int firstIndex = am.firstFrameIndex;
    int lastIndex = am.lastFrameIndex;

    boolean isMovie = am.isMovie;
    int modelIndex = am.cmi;
    if (firstIndex == lastIndex && !isMovie)
      modelIndex = firstIndex;
    int frameID = getModelFileNumber(modelIndex);
    int currentFrame = am.cmi;
    int fileNo = frameID;
    int modelNo = frameID % 1000000;
    int firstNo = (isMovie ? firstIndex : getModelFileNumber(firstIndex));
    int lastNo = (isMovie ? lastIndex : getModelFileNumber(lastIndex));

    String strModelNo;
    if (isMovie) {
      strModelNo = "" + (currentFrame + 1);
    } else if (fileNo == 0) {
      strModelNo = getModelNumberDotted(firstIndex);
      if (firstIndex != lastIndex)
        strModelNo += " - " + getModelNumberDotted(lastIndex);
      if (firstNo / 1000000 == lastNo / 1000000)
        fileNo = firstNo;
    } else {
      strModelNo = getModelNumberDotted(modelIndex);
    }
    if (fileNo != 0)
      fileNo = (fileNo < 1000000 ? 1 : fileNo / 1000000);

    if (!isMovie) {
      g.setI("_currentFileNumber", fileNo);
      g.setI("_currentModelNumberInFile", modelNo);
    }
    float currentMorphModel = am.currentMorphModel;
    g.setI("_currentFrame", currentFrame);
    g.setI("_morphCount", am.morphCount);
    g.setF("_currentMorphFrame", currentMorphModel);
    g.setI("_frameID", frameID);
    g.setI("_modelIndex", modelIndex);
    g.setO("_modelNumber", strModelNo);
    g.setO("_modelName", (modelIndex < 0 ? "" : getModelName(modelIndex)));
    String title = (modelIndex < 0 ? "" : ms.getModelTitle(modelIndex));
    g.setO("_modelTitle", title == null ? "" : title);
    g.setO("_modelFile",
        (modelIndex < 0 ? "" : ms.getModelFileName(modelIndex)));
    g.setO("_modelType",
        (modelIndex < 0 ? "" : ms.getModelFileType(modelIndex)));

    if (currentFrame == prevFrame && currentMorphModel == prevMorphModel)
      return;
    prevFrame = currentFrame;
    prevMorphModel = currentMorphModel;

    String entryName = getModelName(currentFrame);
    if (isMovie) {
      entryName = "" + (entryName == "" ? currentFrame + 1 : am.caf + 1) + ": " + entryName;
    } else {
      String script = "" + getModelNumberDotted(currentFrame);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
    }
    // there was a point where I thought frameNo and currentFrame
    // might be different. 
    sm.setStatusFrameChanged(fileNo, modelNo,
        (am.animationDirection < 0 ? -firstNo : firstNo),
        (am.currentDirection < 0 ? -lastNo : lastNo), currentFrame,
        currentMorphModel, entryName);
    if (doHaveJDX())
      getJSV().setModel(modelIndex);
    if (isJS)
      updateJSView(modelIndex, -1);
  }

  // interaction with JSpecView

  private boolean haveJDX;
  private JmolJSpecView jsv;

  private boolean doHaveJDX() {
    // once-on, never off
    return (haveJDX || (haveJDX = getBooleanProperty("_JSpecView".toLowerCase())));
  }

  JmolJSpecView getJSV() {
    if (jsv == null) {
      jsv = (JmolJSpecView) Interface
          .getOption("jsv.JSpecView", this, "script");
      jsv.setViewer(this);
    }
    return jsv;
  }

  /**
   * get the model designated as "baseModel" in a JCamp-MOL file for example,
   * the model used for bonding for an XYZVIB file or the model used as the base
   * model for a mass spec file. This might then allow pointing off a peak in
   * JSpecView to switch to the model that is involved in HNMR or CNMR
   * 
   * @param modelIndex
   * 
   * @return modelIndex
   */

  public int getJDXBaseModelIndex(int modelIndex) {
    if (!doHaveJDX())
      return modelIndex;
    return getJSV().getBaseModelIndex(modelIndex);
  }

  public Object getJspecViewProperties(Object myParam) {
    // from getProperty("JSpecView...")
    Object o = sm.getJspecViewProperties("" + myParam);
    if (o != null)
      haveJDX = true;
    return o;
  }

  /*
   * echoCallback is one of the two main status reporting mechanisms. Along with
   * scriptCallback, it outputs to the console. Unlike scriptCallback, it does
   * not output to the status bar of the application or applet. If
   * messageCallback is enabled but not echoCallback, these messages go to the
   * messageCallback function instead.
   * 
   * jmolSetCallback("echoCallback", "myEchoCallback") function
   * myEchoCallback(app, message, queueState) {}
   * 
   * queueState = 1 -- queued queueState = 0 -- not queued
   * 
   * serves:
   * 
   * Eval.instructionDispatchLoop when app has -l flag
   * ForceField.steepestDescenTakeNSteps for minimization done
   * Viewer.setPropertyError Viewer.setBooleanProperty error
   * Viewer.setFloatProperty error Viewer.setIntProperty error
   * Viewer.setStringProperty error Viewer.showString adds a Logger.warn()
   * message Eval.showString calculate, cd, dataFrame, echo, error, getProperty,
   * history, isosurface, listIsosurface, pointGroup, print, set, show, write
   * ForceField.steepestDescentInitialize for initial energy
   * ForceField.steepestDescentTakeNSteps for minimization update
   * Viewer.showParameter
   */

  public void scriptEcho(String strEcho) {
    if (!Logger.isActiveLevel(Logger.LEVEL_INFO))
      return;
    /**
     * @j2sNative
     * 
     *            System.out.println(strEcho);
     * 
     */
    {
    }
    sm.setScriptEcho(strEcho, isScriptQueued());
    if (listCommands && strEcho != null && strEcho.indexOf("$[") == 0)
      Logger.info(strEcho);
  }

  private boolean isScriptQueued() {
    return scm != null && scm.isScriptQueued();
  }

  /*
   * errorCallback is a special callback that can be used to identify errors
   * during scripting and file i/o, and also indicate out of memory conditions
   * 
   * jmolSetCallback("errorCallback", "myErrorCallback") function
   * myErrorCallback(app, errType, errMsg, objectInfo, errMsgUntranslated) {}
   * 
   * errType == "Error" or "ScriptException" errMsg == error message, possibly
   * translated, with added information objectInfo == which object (such as an
   * isosurface) was involved errMsgUntranslated == just the basic message
   * 
   * Viewer.notifyError Eval.runEval on Error and file loading Exceptions
   * Viewer.handleError Eval.runEval on OOM Error Viewer.createModelSet on OOM
   * model initialization Error Viewer.getImage on OOM rendering Error
   */
  public void notifyError(String errType, String errMsg,
                          String errMsgUntranslated) {
    g.setO("_errormessage", errMsgUntranslated);
    sm.notifyError(errType, errMsg, errMsgUntranslated);
  }

  /*
   * evalCallback is a special callback that evaluates expressions in JavaScript
   * rather than in Jmol.
   * 
   * Viewer.jsEval Eval.loadScriptFileInternal Eval.Rpn.evaluateScript
   * Eval.script
   */

  public String jsEval(String strEval) {
    return sm.jsEval(strEval);
  }

  /*
   * loadStructCallback indicates file load status.
   * 
   * jmolSetCallback("loadStructCallback", "myLoadStructCallback") function
   * myLoadStructCallback(fullPathName, fileName, modelName, errorMsg, ptLoad)
   * {}
   * 
   * ptLoad == JmolConstants.FILE_STATUS_NOT_LOADED == -1 ptLoad == JmolConstants.FILE_STATUS_ZAPPED == 0
   * ptLoad == JmolConstants.FILE_STATUS_CREATING_MODELSET == 2 ptLoad ==
   * JmolConstants.FILE_STATUS_MODELSET_CREATED == 3 ptLoad == JmolConstants.FILE_STATUS_MODELS_DELETED == 5
   * 
   * Only -1 (error loading), 0 (zapped), and 3 (model set created) messages are
   * passed on to the callback function. The others can be detected using
   * 
   * set loadStructCallback "jmolscript:someFunctionName"
   * 
   * At the time of calling of that method, the jmolVariable _loadPoint gives
   * the value of ptLoad. These load points are also recorded in the status
   * queue under types "fileLoaded" and "fileLoadError".
   * 
   * Viewer.setFileLoadStatus Viewer.createModelSet (2, 3)
   * Viewer.createModelSetAndReturnError (-1, 1, 4) Viewer.deleteAtoms (5)
   * Viewer.zap (0)
   */
  private void setFileLoadStatus(FIL ptLoad, String fullPathName,
                                 String fileName, String modelName,
                                 String strError, Boolean isAsync) {
    setErrorMessage(strError, null);
    g.setI("_loadPoint", ptLoad.getCode());
    boolean doCallback = (ptLoad != FIL.CREATING_MODELSET);
    if (doCallback)
      setStatusFrameChanged(false, false);
    sm.setFileLoadStatus(fullPathName, fileName, modelName, strError,
        ptLoad.getCode(), doCallback, isAsync);
    if (doCallback) {
//       setStatusFrameChanged(false, true); // ensures proper title in JmolFrame but then we miss the file name
      if (doHaveJDX())
        getJSV().setModel(am.cmi);
      if (isJS)
        updateJSView(am.cmi, -2);
    }

  }

  public String getZapName() {
    return (g.modelKitMode ? JC.MODELKIT_ZAP_TITLE : JC.ZAP_TITLE);
  }

  /*
   * measureCallback reports completed or pending measurements. Pending
   * measurements are measurements that the user has started but has not
   * completed -- this call comes when the user hesitates with the mouse over an
   * atom and the "rubber band" is showing
   * 
   * jmolSetCallback("measureCallback", "myMeasureCallback") function
   * myMeasureCallback(strMeasure, intInfo, status) {}
   * 
   * intInfo == (see below) status == "measurePicked" (intInfo == the number of
   * atoms in the measurement) "measureComplete" (intInfo == the current number
   * measurements) "measureDeleted" (intInfo == the index of the measurement
   * deleted or -1 for all) "measurePending" (intInfo == number of atoms picked
   * so far)
   * 
   * strMeasure:
   * 
   * For "set picking MEASURE ..." each time the user clicks an atom, a message
   * is sent to the pickCallback function (see below), and if the picking is set
   * to measure distance, angle, or torsion, then after the requisite number of
   * atoms is picked and the pick callback message is sent, a call is also made
   * to measureCallback with a string that indicates the measurement, such as:
   * 
   * Angle O #9 - Si #7 - O #2 : 110.51877
   * 
   * Under default conditions, when picking is not set to MEASURE, then
   * measurement reports are sent when the measure is completed, deleted, or
   * pending. These reports are in a psuedo array form that can be parsed more
   * easily, involving the atoms and measurement with units, for example:
   * 
   * [Si #3, O #8, Si #7, 60.1 <degrees mark>]
   * 
   * Viewer.setStatusMeasuring Measures.clear Measures.define
   * Measures.deleteMeasurement Measures.pending actionManager.atomPicked
   */

  public void setStatusMeasuring(String status, int intInfo, String strMeasure,
                                 float value) {

    // status           intInfo 

    // measureCompleted index
    // measurePicked    atom count
    // measurePending   atom count
    // measureDeleted   -1 (all) or index
    // measureSequence  -2
    sm.setStatusMeasuring(status, intInfo, strMeasure, value);
  }

  /*
   * minimizationCallback reports the status of a currently running
   * minimization.
   * 
   * jmolSetCallback("minimizationCallback", "myMinimizationCallback") function
   * myMinimizationCallback(app, minStatus, minSteps, minEnergy, minEnergyDiff)
   * {}
   * 
   * minStatus is one of "starting", "calculate", "running", "failed", or "done"
   * 
   * Viewer.notifyMinimizationStatus Minimizer.endMinimization
   * Minimizer.getEnergyonly Minimizer.startMinimization
   * Minimizer.stepMinimization
   */

  public void notifyMinimizationStatus() {
    Object step = getP("_minimizationStep");
    String ff = (String) getP("_minimizationForceField");
    sm.notifyMinimizationStatus((String) getP("_minimizationStatus"),
        step instanceof String ? Integer.valueOf(0) : (Integer) step,
        (Float) getP("_minimizationEnergy"),
        (step.toString().equals("0") ? Float.valueOf(0)
            : (Float) getP("_minimizationEnergyDiff")), ff);
  }

  /*
   * pickCallback returns information about an atom, bond, or DRAW object that
   * has been picked by the user.
   * 
   * jmolSetCallback("pickCallback", "myPickCallback") function
   * myPickCallback(strInfo, iAtom, map) {}
   * 
   * iAtom == the index of the atom picked or -2 for a draw object or -3 for a
   * bond
   * 
   * strInfo depends upon the type of object picked:
   * 
   * atom (iAtom>=0): a string determinied by the PICKLABEL parameter, which if "" delivers
   * the atom identity along with its coordinates
   * 
   * bond (iAtom==-3): ["bond", bondIdentityString (quoted), x, y, z] where the coordinates
   * are of the midpoint of the bond
   * 
   * draw (iAtom==-2): ["draw", ID(quoted), pickedModel, pickedVertex, x, y, z,
   * title(quoted)]
   * 
   * isosurface (iAtom==-4): ["isosurface", ID(quoted), pickedModel, pickedVertex, x, y, z,
   * title(quoted)]
   * 
   * map:
   * 
   * atom: null
   * 
   * bond: {pt, index, modelIndex, modelNumberDotted, type, strInfo}
   * 
   * Draw, isosurface: {pt, modelIndex, modelNumberDotted, id, vertex, type}
   * 
   * Viewer.setStatusAtomPicked Draw.checkObjectClicked (set picking DRAW)
   * Sticks.checkObjectClicked (set bondPicking TRUE; set picking IDENTIFY)
   * actionManager.atomPicked (set atomPicking TRUE; set picking IDENTIFY)
   * actionManager.queueAtom (during measurements)
   */

  public void setStatusAtomPicked(int atomIndex, String info,
                                  Map<String, Object> map) {
    if (info == null) {
      info = g.pickLabel;
      info = (info.length() == 0 ? getAtomInfoXYZ(atomIndex,
          g.messageStyleChime) : ms.getAtomInfo(atomIndex, info, ptTemp));
    }
    setPicked(atomIndex);
    g.setO("_pickinfo", info);
    sm.setStatusAtomPicked(atomIndex, info, map);
    if (atomIndex < 0)
      return;
    int syncMode = sm.getSyncMode();
    if (syncMode == StatusManager.SYNC_DRIVER && doHaveJDX())
      getJSV().atomPicked(atomIndex);
    if (isJS)
      updateJSView(ms.at[atomIndex].mi, atomIndex);
  }

  public boolean setStatusDragDropped(int mode, int x, int y, String fileName) {
    if (mode == 0) {
      g.setO("_fileDropped", fileName);
      g.setUserVariable("doDrop", SV.vT);
    }
    boolean handled = sm.setStatusDragDropped(mode, x, y, fileName);
    return (!handled || getP("doDrop").toString().equals("true"));
  }

  /*
   * resizeCallback is called whenever the applet gets a resize notification
   * from the browser
   * 
   * jmolSetCallback("resizeCallback", "myResizeCallback") function
   * myResizeCallback(width, height) {}
   */

  public void setStatusResized(int width, int height) {
    sm.setStatusResized(width, height);
  }

  /*
   * scriptCallback is the primary way to monitor script status. In addition, it
   * serves to for passing information to the user over the status line of the
   * browser as well as to the console. Note that console messages are also sent
   * by echoCallback. If messageCallback is enabled but not scriptCallback,
   * these messages go to the messageCallback function instead.
   * 
   * jmolSetCallback("scriptCallback", "myScriptCallback") function
   * myScriptCallback(app, status, message, intStatus, errorMessageUntranslated)
   * {}
   * 
   * intStatus == -2 script start -- message is the script itself intStatus == 0
   * general messages during script execution; translated error message may be
   * present intStatus >= 1 script termination message; translated and
   * untranslated message may be present value is time for execution in
   * milliseconds
   * 
   * Eval.defineAtomSet -- compilation bug indicates problem in JmolConstants
   * array Eval.instructionDispatchLoop -- debugScript messages
   * Eval.logDebugScript -- debugScript messages Eval.pause -- script execution
   * paused message Eval.runEval -- "Script completed" message Eval.script --
   * Chime "script <exiting>" message Eval.scriptStatusOrBuffer -- various
   * messages for Eval.checkContinue (error message) Eval.connect Eval.delete
   * Eval.hbond Eval.load (logMessages message) Eval.message Eval.runEval (error
   * message) Eval.write (error reading file) Eval.zap (error message)
   * FileManager.createAtomSetCollectionFromFile "requesting..." for Chime-like
   * compatibility actionManager.atomPicked
   * "pick one more atom in order to spin..." for example
   * Viewer.evalStringWaitStatus -- see above -2, 0 only if error, >=1 at
   * termination Viewer.reportSelection "xxx atoms selected"
   */

  public void scriptStatus(String strStatus) {
    setScriptStatus(strStatus, "", 0, null);
  }

  public void scriptStatusMsg(String strStatus, String statusMessage) {
    setScriptStatus(strStatus, statusMessage, 0, null);
  }

  public void setScriptStatus(String strStatus, String statusMessage,
                              int msWalltime, String strErrorMessageUntranslated) {
    sm.setScriptStatus(strStatus, statusMessage, msWalltime,
        strErrorMessageUntranslated);
  }

  /*
   * syncCallback traps script synchronization messages and allows for
   * cancellation (by returning "") or modification
   * 
   * jmolSetCallback("syncCallback", "mySyncCallback") function
   * mySyncCallback(app, script, appletName) { ...[modify script here]... return
   * newScript }
   * 
   * StatusManager.syncSend Viewer.setSyncTarget Viewer.syncScript
   */

  @Override
  public void showUrl(String urlString) {
    // applet.Jmol
    // app Jmol
    // StatusManager
    if (urlString == null)
      return;
    if (urlString.indexOf(":") < 0) {
      String base = fm.getAppletDocumentBase();
      if (base == "")
        base = fm.getFullPathName(false);
      if (base.indexOf("/") >= 0) {
        base = base.substring(0, base.lastIndexOf("/") + 1);
      } else if (base.indexOf("\\") >= 0) {
        base = base.substring(0, base.lastIndexOf("\\") + 1);
      }
      urlString = base + urlString;
    }
    Logger.info("showUrl:" + urlString);
    sm.showUrl(urlString);
  }

  /**
   * an external applet or app with class that extends org.jmol.jvxl.MeshCreator
   * might execute:
   * 
   * org.jmol.viewer.Viewer vwr = applet.getViewer(); vwr.setMeshCreator(this);
   * 
   * then that class's updateMesh(String id) method will be called whenever a
   * mesh is rendered.
   * 
   * @param meshCreator
   */
  public void setMeshCreator(Object meshCreator) {
    shm.loadShape(JC.SHAPE_ISOSURFACE);
    setShapeProperty(JC.SHAPE_ISOSURFACE, "meshCreator", meshCreator);
  }

  public void showConsole(boolean showConsole) {
    if (!haveDisplay)
      return;
    // Eval
    try {
      if (appConsole == null && showConsole)
        getConsole();
      appConsole.setVisible(true);
    } catch (Throwable e) {
      // no console for this client... maybe no Swing
    }
  }

  public JmolAppConsoleInterface getConsole() {
   getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
   return appConsole;
  }
  
  @Override
  public Object getParameter(String key) {
    return getP(key);
  }

  public Object getP(String key) {
    return g.getParameter(key, true);
  }

  public Object getPOrNull(String key) {
    return g.getParameter(key, false);
  }

  public void unsetProperty(String key) {
    key = key.toLowerCase();
    if (key.equals("all") || key.equals("variables"))
      fm.setPathForAllFiles("");
    g.unsetUserVariable(key);
  }

  @Override
  public void notifyStatusReady(boolean isReady) {
    System.out.println("Jmol applet " + fullName
        + (isReady ? " ready" : " destroyed"));
    if (!isReady)
      dispose();
    sm.setStatusAppletReady(fullName, isReady);
  }

  @Override
  public boolean getBooleanProperty(String key) {
    key = key.toLowerCase();
    if (g.htBooleanParameterFlags.containsKey(key))
      return g.htBooleanParameterFlags.get(key).booleanValue();
    // special cases
    if (key.endsWith("p!")) {
      if (acm == null)
        return false;
      String s = acm.getPickingState().toLowerCase();
      key = key.substring(0, key.length() - 2) + ";";
      return (s.indexOf(key) >= 0);
    }
    if (key.equalsIgnoreCase("executionPaused"))
      return (eval != null && eval.isPaused());
    if (key.equalsIgnoreCase("executionStepping"))
      return (eval != null && eval.isStepping());
    if (key.equalsIgnoreCase("haveBFactors"))
      return (ms.getBFactors() != null);
    if (key.equalsIgnoreCase("colorRasmol"))
      return cm.isDefaultColorRasmol;
    if (key.equalsIgnoreCase("frank"))
      return getShowFrank();
    if (key.equalsIgnoreCase("spinOn"))
      return tm.spinOn;
    if (key.equalsIgnoreCase("isNavigating"))
      return tm.isNavigating();
    if (key.equalsIgnoreCase("showSelections"))
      return selectionHalosEnabled;
    if (g.htUserVariables.containsKey(key)) {
      SV t = g.getUserVariable(key);
      if (t.tok == T.on)
        return true;
      if (t.tok == T.off)
        return false;
    }
    Logger.error("vwr.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  @Override
  public int getInt(int tok) {
    switch (tok) {
    case T.animationfps:
      return am.animationFps;
    case T.dotdensity:
      return g.dotDensity;
    case T.dotscale:
      return g.dotScale;
    case T.helixstep:
      return g.helixStep;
    case T.meshscale:
      return g.meshScale;
    case T.minpixelselradius:
      return g.minPixelSelRadius;
    case T.percentvdwatom:
      return g.percentVdwAtom;
    case T.pickingspinrate:
      return g.pickingSpinRate;
    case T.ribbonaspectratio:
      return g.ribbonAspectRatio;
    case T.showscript:
      return g.scriptDelay;
    case T.smallmoleculemaxatoms:
      return g.smallMoleculeMaxAtoms;
    case T.strutspacing:
      return g.strutSpacing;
    }
    Logger.error("viewer.getInt(" + T.nameOf(tok) + ") - not listed");
    return 0;
  }

  // special cases:

  public int getDelayMaximumMs() {
    return (haveDisplay ? g.delayMaximumMs : 1);
  }

  public int getHermiteLevel() {
    return (tm.spinOn ? 0 : g.hermiteLevel);
  }

  public int getHoverDelay() {
    return (g.modelKitMode ? 20 : g.hoverDelayMs);

  }

  @Override
  public boolean getBoolean(int tok) {
    switch (tok) {
    case T.pdb:
      return ms.getMSInfoB("isPDB");
    case T.allowgestures:
      return g.allowGestures;
    case T.allowmultitouch:
      return g.allowMultiTouch;
    case T.allowrotateselected:
      return g.allowRotateSelected;
    case T.appendnew:
      return g.appendNew;
    case T.applysymmetrytobonds:
      return g.applySymmetryToBonds;
    case T.atompicking:
      return g.atomPicking;
    case T.autobond:
      return g.autoBond;
    case T.autofps:
      return g.autoFps;
    case T.axesorientationrasmol:
      return g.axesOrientationRasmol;
    case T.backbonesteps:
      return g.backboneSteps;
    case T.bondmodeor:
      return g.bondModeOr;
    case T.cartoonbaseedges:
      return g.cartoonBaseEdges;
    case T.cartoonsfancy:
      return g.cartoonFancy;
    case T.cartoonladders:
      return g.cartoonLadders;
    case T.cartoonribose:
      return g.cartoonRibose;
    case T.cartoonrockets:
      return g.cartoonRockets;
    case T.chaincasesensitive:
      return g.chainCaseSensitive || chainCaseSpecified;
    case T.debugscript:
      return g.debugScript;
    case T.defaultstructuredssp:
      return g.defaultStructureDSSP;
    case T.disablepopupmenu:
      return g.disablePopupMenu;
    case T.displaycellparameters:
      return g.displayCellParameters;
    case T.dotsurface:
      return g.dotSurface;
    case T.dotsselectedonly:
      return g.dotsSelectedOnly;
    case T.drawpicking:
      return g.drawPicking;
    case T.fontcaching:
      return g.fontCaching;
    case T.fontscaling:
      return g.fontScaling;
    case T.forceautobond:
      return g.forceAutoBond;
    case T.fractionalrelative:
      return false;//g.fractionalRelative;
    case T.greyscalerendering:
      return g.greyscaleRendering;
    case T.hbondsbackbone:
      return g.hbondsBackbone;
    case T.hbondsrasmol:
      return g.hbondsRasmol;
    case T.hbondssolid:
      return g.hbondsSolid;
    case T.hetero:
      return g.rasmolHeteroSetting;
    case T.hidenameinpopup:
      return g.hideNameInPopup;
    case T.highresolution:
      return g.highResolutionFlag;
    case T.hydrogen:
      return g.rasmolHydrogenSetting;
    case T.isosurfacekey:
      return g.isosurfaceKey;
    case T.justifymeasurements:
      return g.justifyMeasurements;
    case T.legacyautobonding:
      // aargh -- BitSet efficiencies in Jmol 11.9.24, 2/3/2010, meant that
      // state files created before that that use select BONDS will select the
      // wrong bonds. 
      // reset after a state script is read
      return g.legacyAutoBonding;
    case T.legacyhaddition:
      // aargh -- Some atoms missed before Jmol 13.1.17
      return g.legacyHAddition;
    case T.loggestures:
      return g.logGestures;
    case T.measureallmodels:
      return g.measureAllModels;
    case T.measurementlabels:
      return g.measurementLabels;
    case T.messagestylechime:
      return g.messageStyleChime;
    case T.modelkitmode:
      return g.modelKitMode;
    case T.multiplebondbananas:
      return g.multipleBondBananas;
    case T.navigationmode:
      return g.navigationMode;
    case T.navigationperiodic:
      return g.navigationPeriodic;
    case T.partialdots:
      return g.partialDots;
    case T.pdbsequential:
      return g.pdbSequential;
    case T.preservestate:
      return g.preserveState;
    case T.ribbonborder:
      return g.ribbonBorder;
    case T.rocketbarrels:
      return g.rocketBarrels;
    case T.selectallmodels:
      return g.selectAllModels;
    case T.showhiddenselectionhalos:
      return g.showHiddenSelectionHalos;
    case T.showhydrogens:
      return g.showHydrogens;
    case T.showmeasurements:
      return g.showMeasurements;
    case T.showmodvecs:
      return g.showModVecs;
    case T.showmultiplebonds:
      return g.showMultipleBonds;
    case T.showtiming:
      return g.showTiming;
    case T.showunitcelldetails:
      return g.showUnitCellDetails;
    case T.slabbyatom:
      return g.slabByAtom;
    case T.slabbymolecule:
      return g.slabByMolecule;
    case T.smartaromatic:
      return g.smartAromatic;
    case T.solvent:
      return g.solventOn;
    case T.ssbondsbackbone:
      return g.ssbondsBackbone;
    case T.strutsmultiple:
      return g.strutsMultiple;
    case T.tracealpha:
      return g.traceAlpha;
    case T.translucent:
      return g.translucent;
    case T.twistedsheets:
      return g.twistedSheets;
    case T.vectorscentered:
      return g.vectorsCentered;
    case T.vectorsymmetry:
      return g.vectorSymmetry;
    case T.waitformoveto:
      return g.waitForMoveTo;
    case T.zerobasedxyzrasmol:
      return g.zeroBasedXyzRasmol;
    }
    Logger.error("viewer.getBoolean(" + T.nameOf(tok) + ") - not listed");
    return false;
  }

  // special cases:

  public boolean allowEmbeddedScripts() {
    return (g.allowEmbeddedScripts && !isPreviewOnly);
  }

  boolean getDragSelected() {
    return (g.dragSelected && !g.modelKitMode);
  }

  boolean getBondPicking() {
    return (g.bondPicking || g.modelKitMode);
  }

  public boolean useMinimizationThread() {
    return (g.useMinimizationThread && !autoExit);
  }

  @Override
  public float getFloat(int tok) {
    switch (tok) {
    case T.atoms:
      return g.particleRadius;
    case T.axesscale:
      return g.axesScale;
    case T.bondtolerance:
      return g.bondTolerance;
    case T.defaulttranslucent:
      return g.defaultTranslucent;
    case T.defaultdrawarrowscale:
      return g.defaultDrawArrowScale;
    case T.dipolescale:
      return g.dipoleScale;
    case T.drawfontsize:
      return g.drawFontSize;
    case T.exportscale:
      return g.exportScale;
    case T.hbondsangleminimum:
      return g.hbondsAngleMinimum;
    case T.hbondsdistancemaximum:
      return g.hbondsDistanceMaximum;
    case T.loadatomdatatolerance:
      return g.loadAtomDataTolerance;
    case T.minbonddistance:
      return g.minBondDistance;
    case T.modulation:
      return g.modulationScale;
    case T.multiplebondspacing:
      return g.multipleBondSpacing;
    case T.multiplebondradiusfactor:
      return g.multipleBondRadiusFactor;
    case T.navigationspeed:
      return g.navigationSpeed;
    case T.pointgroupdistancetolerance:
      return g.pointGroupDistanceTolerance;
    case T.pointgrouplineartolerance:
      return g.pointGroupLinearTolerance;
    case T.rotationradius:
      return tm.modelRadius;
    case T.sheetsmoothing:
      return g.sheetSmoothing;
    case T.solventproberadius:
      return g.solventProbeRadius;
    case T.starwidth:
      return g.starWidth;
    case T.strutdefaultradius:
      return g.strutDefaultRadius;
    case T.strutlengthmaximum:
      return g.strutLengthMaximum;
    case T.vectorscale:
      return g.vectorScale;
    case T.vibrationperiod:
      return g.vibrationPeriod;
    }
    Logger.error("viewer.getFloat(" + T.nameOf(tok) + ") - not listed");
    return 0;
  }

  @Override
  public void setStringProperty(String key, String value) {
    if (value == null || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setO(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, SV.newV(T.string, value).asBoolean());
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, SV.newV(T.string, value).asInt());
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, PT.parseFloat(value));
      break;
    default:
      setStringPropertyTok(key, tok, value);
    }
  }

  private void setStringPropertyTok(String key, int tok, String value) {
    switch (tok) {
    // 14.3.10 (forgot to add these earlier)
    case T.edsurlcutoff:
      g.edsUrlCutoff = value;
      break;
    case T.edsurlformat:
      g.edsUrlFormat = value;
      break;
    // 14.3.10 new
    case T.edsurlformatdiff:
      g.edsUrlFormatDiff = value;
      break;
    // 13.3.6
    case T.animationmode:
      setAnimationMode(value);
      return;
    case T.nmrpredictformat:
      // 13.3.4
      g.nmrPredictFormat = value;
      break;
    case T.defaultdropscript:
      // 13.1.2
      // for File|Open and Drag/drop
      g.defaultDropScript = value;
      break;

    case T.pathforallfiles:
      // 12.3.29
      value = fm.setPathForAllFiles(value);
      break;
    case T.energyunits:
      // 12.3.26
      setUnits(value, false);
      return;
    case T.forcefield:
      // 12.3.25
      g.forceField = value = ("UFF".equalsIgnoreCase(value) ? "UFF" : "MMFF");
      minimizer = null;
      break;
    case T.nmrurlformat:
      // 12.3.3
      g.nmrUrlFormat = value;
      break;
    case T.measurementunits:
      setUnits(value, true);
      return;
    case T.loadligandformat:
      // /12.1.51//
      g.pdbLoadLigandFormat = value;
      break;
    // 12.1.50
    case T.defaultlabelpdb:
      g.defaultLabelPDB = value;
      break;
    case T.defaultlabelxyz:
      g.defaultLabelXYZ = value;
      break;
    case T.defaultloadfilter:
      // 12.0.RC10
      g.defaultLoadFilter = value;
      break;
    case T.logfile:
      value = getOutputManager().setLogFile(value);
      if (value == null)
        return;
      break;
    case T.filecachedirectory:
      // 11.9.21
      // not implemented -- application only -- CANNOT BE SET BY STATE
      // global.fileCacheDirectory = value;
      break;
    case T.atomtypes:
      // 11.7.7
      g.atomTypes = value;
      break;
    case T.currentlocalpath:
      // /11.6.RC15
      break;
    case T.picklabel:
      // /11.5.42
      g.pickLabel = value;
      break;
    case T.quaternionframe:
      // /11.5.39//
      if (value.length() == 2 && value.startsWith("R"))
        // C, P -- straightness from Ramachandran angles
        g.quaternionFrame = value.substring(0, 2);
      else
        g.quaternionFrame = "" + (value.toLowerCase() + "p").charAt(0);
      if (!PT.isOneOf(g.quaternionFrame, JC.allowedQuaternionFrames))
        g.quaternionFrame = "p";
      ms.haveStraightness = false;
      break;
    case T.defaultvdw:
      // /11.5.11//
      setVdwStr(value);
      return;
    case T.language:
      // /11.1.30//
      // fr cs en none, etc.
      // also serves to change language for callbacks and menu
      new GT(this, value);
      String language = GT.getLanguage();
      modelkitPopup = null;
      if (jmolpopup != null) {
        jmolpopup.jpiDispose();
        jmolpopup = null;
        getPopupMenu();
      }
      sm.setCallbackFunction("language", language);
      value = GT.getLanguage();
      break;
    case T.loadformat:
      // /11.1.22//
      g.loadFormat = value;
      break;
    case T.backgroundcolor:
      // /11.1///
      setObjectColor("background", value);
      return;
    case T.axis1color:
      setObjectColor("axis1", value);
      return;
    case T.axis2color:
      setObjectColor("axis2", value);
      return;
    case T.axis3color:
      setObjectColor("axis3", value);
      return;
    case T.boundboxcolor:
      setObjectColor("boundbox", value);
      return;
    case T.unitcellcolor:
      setObjectColor("unitcell", value);
      return;
    case T.propertycolorscheme:
      setPropertyColorScheme(value, false, false);
      break;
    case T.hoverlabel:
      // a special label for selected atoms
      shm.loadShape(JC.SHAPE_HOVER);
      setShapeProperty(JC.SHAPE_HOVER, "atomLabel", value);
      break;
    case T.defaultdistancelabel:
      // /11.0///
      g.defaultDistanceLabel = value;
      break;
    case T.defaultanglelabel:
      g.defaultAngleLabel = value;
      break;
    case T.defaulttorsionlabel:
      g.defaultTorsionLabel = value;
      break;
    case T.defaultloadscript:
      g.defaultLoadScript = value;
      break;
    case T.appletproxy:
      fm.setAppletProxy(value);
      break;
    case T.defaultdirectory:
      if (value == null)
        value = "";
      value = value.replace('\\', '/');
      g.defaultDirectory = value;
      break;
    case T.helppath:
      g.helpPath = value;
      break;
    case T.defaults:
      if (!value.equalsIgnoreCase("RasMol") && !value.equalsIgnoreCase("PyMOL"))
        value = "Jmol";
      setDefaultsType(value);
      break;
    case T.defaultcolorscheme:
      // only two are possible: "jmol" and "rasmol"
      setDefaultColors(value.equalsIgnoreCase("rasmol"));
      return;
    case T.picking:
      setPickingMode(value, 0);
      return;
    case T.pickingstyle:
      setPickingStyle(value, 0);
      return;
    case T.dataseparator:
      // just saving this
      break;
    default:
      if (key.toLowerCase().endsWith("callback")) {
        sm.setCallbackFunction(key,
            (value.length() == 0 || value.equalsIgnoreCase("none") ? null
                : value));
        break;
      }
      if (!g.htNonbooleanParameterValues.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.newV(T.string, value));
        return;
      }
      // a few String parameters may not be tokenized. Save them anyway.
      // for example, defaultDirectoryLocal
      break;
    }
    g.setO(key, value);
  }

  @Override
  public void setFloatProperty(String key, float value) {
    if (Float.isNaN(value) || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setF(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "" + value);
      break;
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, value != 0);
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, (int) value);
      break;
    default:
      setFloatPropertyTok(key, tok, value);
    }
  }

  private void setFloatPropertyTok(String key, int tok, float value) {
    switch (tok) {
    case T.modulationscale:
      // 14.0.1
      ms.setModulation(null, false, null, false);
      g.modulationScale = value = Math.max(0.1f, value);
      ms.setModulation(null, true, null, false);
      break;
    case T.particleradius:
      // 13.3.9
      g.particleRadius = Math.abs(value);
      break;
    case T.drawfontsize:
      // 13.3.6
      g.drawFontSize = value;
      break;
    case T.exportscale:
      // 13.1.19
      g.exportScale = value;
      break;
    case T.starwidth:
      // 13.1.15
      g.starWidth = value;
      break;
    case T.multiplebondradiusfactor:
      // 12.1.11
      g.multipleBondRadiusFactor = value;
      break;
    case T.multiplebondspacing:
      // 12.1.11
      g.multipleBondSpacing = value;
      break;
    case T.slabrange:
      tm.setSlabRange(value);
      break;
    case T.minimizationcriterion:
      g.minimizationCriterion = value;
      break;
    case T.gestureswipefactor:
      if (haveDisplay)
        acm.setGestureSwipeFactor(value);
      break;
    case T.mousedragfactor:
      if (haveDisplay)
        acm.setMouseDragFactor(value);
      break;
    case T.mousewheelfactor:
      if (haveDisplay)
        acm.setMouseWheelFactor(value);
      break;
    case T.strutlengthmaximum:
      // 11.9.21
      g.strutLengthMaximum = value;
      break;
    case T.strutdefaultradius:
      g.strutDefaultRadius = value;
      break;
    case T.navx:
      // 11.7.47
      setSpin("X", (int) value);
      break;
    case T.navy:
      setSpin("Y", (int) value);
      break;
    case T.navz:
      setSpin("Z", (int) value);
      break;
    case T.navfps:
      if (Float.isNaN(value))
        return;
      setSpin("FPS", (int) value);
      break;
    case T.loadatomdatatolerance:
      g.loadAtomDataTolerance = value;
      break;
    case T.hbondsangleminimum:
      // 11.7.9
      g.hbondsAngleMinimum = value;
      break;
    case T.hbondsdistancemaximum:
      // 11.7.9
      g.hbondsDistanceMaximum = value;
      break;
    case T.pointgroupdistancetolerance:
      // 11.6.RC2//
      g.pointGroupDistanceTolerance = value;
      break;
    case T.pointgrouplineartolerance:
      g.pointGroupLinearTolerance = value;
      break;
    case T.ellipsoidaxisdiameter:
      g.ellipsoidAxisDiameter = value;
      break;
    case T.spinx:
      // /11.3.52//
      setSpin("x", (int) value);
      break;
    case T.spiny:
      setSpin("y", (int) value);
      break;
    case T.spinz:
      setSpin("z", (int) value);
      break;
    case T.spinfps:
      setSpin("fps", (int) value);
      break;
    case T.defaultdrawarrowscale:
      // /11.3.17//
      g.defaultDrawArrowScale = value;
      break;
    case T.defaulttranslucent:
      // /11.1///
      g.defaultTranslucent = value;
      break;
    case T.axesscale:
      setAxesScale(value);
      break;
    case T.visualrange:
      tm.visualRangeAngstroms = value;
      refresh(1, "set visualRange");
      break;
    case T.navigationdepth:
      setNavigationDepthPercent(value);
      break;
    case T.navigationspeed:
      g.navigationSpeed = value;
      break;
    case T.navigationslab:
      tm.setNavigationSlabOffsetPercent(value);
      break;
    case T.cameradepth:
      tm.setCameraDepthPercent(value, false);
      refresh(1, "set cameraDepth");
      // transformManager will set global value for us;
      return;
    case T.rotationradius:
      setRotationRadius(value, true);
      return;
    case T.hoverdelay:
      g.hoverDelayMs = (int) (value * 1000);
      break;
    case T.sheetsmoothing:
      // /11.0///
      g.sheetSmoothing = value;
      break;
    case T.dipolescale:
      value = checkFloatRange(value, -10, 10);
      g.dipoleScale = value;
      break;
    case T.stereodegrees:
      tm.setStereoDegrees(value);
      break;
    case T.vectorscale:
      // public -- no need to set
      setVectorScale(value);
      return;
    case T.vibrationperiod:
      // public -- no need to set
      setVibrationPeriod(value);
      return;
    case T.vibrationscale:
      // public -- no need to set
      setVibrationScale(value);
      return;
    case T.bondtolerance:
      setBondTolerance(value);
      return;
    case T.minbonddistance:
      setMinBondDistance(value);
      return;
    case T.scaleangstromsperinch:
      tm.setScaleAngstromsPerInch(value);
      break;
    case T.solventproberadius:
      value = checkFloatRange(value, 0, 10);
      g.solventProbeRadius = value;
      break;
    default:
      if (!g.htNonbooleanParameterValues.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.newV(T.decimal, Float.valueOf(value)));
        return;
      }
    }
    g.setF(key, value);
  }

  @Override
  public void setIntProperty(String key, int value) {
    if (value == Integer.MIN_VALUE || key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setI(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "" + value);
      break;
    case T.booleanparam:
      setBooleanPropertyTok(key, tok, value != 0);
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, value);
      break;
    default:
      setIntPropertyTok(key, tok, value);
    }
  }

  private void setIntPropertyTok(String key, int tok, int value) {
    switch (tok) {
    case T.contextdepthmax:
    case T.historylevel:
    case T.scriptreportinglevel:
      value = eval.setStatic(tok, value);
      break;
    case T.bondingversion:
      // 14.1.11
      value = (value == 0 ? Elements.RAD_COV_IONIC_OB1_100_1
          : Elements.RAD_COV_BODR_2014_02_22);
      g.bondingVersion = Elements.bondingVersion = value;
      break;
    case T.celshadingpower:
      // 13.3.9
      gdata.setCelPower(value);
      break;
    case T.ambientocclusion:
      // 13.3.9
      gdata.setAmbientOcclusion(value);
      break;
    case T.platformspeed:
      // 13.3.4
      g.platformSpeed = Math.min(Math.max(value, 0), 10); // 0 could mean "adjust as needed"
      break;
    case T.meshscale:
      // 12.3.29
      g.meshScale = value;
      break;
    case T.minpixelselradius:
      // 12.2.RC6
      g.minPixelSelRadius = value;
      break;
    case T.isosurfacepropertysmoothingpower:
      // 12.1.11
      g.isosurfacePropertySmoothingPower = value;
      break;
    case T.repaintwaitms:
      // 12.0.RC4
      g.repaintWaitMs = value;
      break;
    case T.smallmoleculemaxatoms:
      // 12.0.RC3
      g.smallMoleculeMaxAtoms = value;
      break;
    case T.minimizationsteps:
      g.minimizationSteps = value;
      break;
    case T.strutspacing:
      // 11.9.21
      g.strutSpacing = value;
      break;
    case T.phongexponent:
      // 11.9.13
      value = checkIntRange(value, 0, 1000);
      gdata.setPhongExponent(value);
      break;
    case T.helixstep:
      // 11.8.RC3
      g.helixStep = value;
      ms.haveStraightness = false;
      break;
    case T.dotscale:
      // 12.0.RC25
      g.dotScale = value;
      break;
    case T.dotdensity:
      // 11.6.RC2//
      g.dotDensity = value;
      break;
    case T.delaymaximumms:
      // 11.5.4//
      g.delayMaximumMs = value;
      break;
    case T.loglevel:
      // /11.3.52//
      Logger.setLogLevel(value);
      Logger.info("logging level set to " + value);
      g.setI("logLevel", value);
      if (eval != null)
        eval.setDebugging();
      return;
    case T.axesmode:
      setAxesMode(value == 2 ? T.axesunitcell : value == 1 ? T.axesmolecular
          : T.axeswindow);
      return;
    case T.strandcount:
      // /11.1///
      setStrandCount(0, value);
      return;
    case T.strandcountforstrands:
      setStrandCount(JC.SHAPE_STRANDS, value);
      return;
    case T.strandcountformeshribbon:
      setStrandCount(JC.SHAPE_MESHRIBBON, value);
      return;
    case T.perspectivemodel:
      // abandoned in 13.1.10
      //setPerspectiveModel(value);
      return;
    case T.showscript:
      g.scriptDelay = value;
      break;
    case T.specularpower:
      if (value < 0)
        value = checkIntRange(value, -10, -1);
      else
        value = checkIntRange(value, 0, 100);
      gdata.setSpecularPower(value);
      break;
    case T.specularexponent:
      value = checkIntRange(-value, -10, -1);
      gdata.setSpecularPower(value);
      break;
    case T.bondradiusmilliangstroms:
      setMarBond((short) value);
      // public method -- no need to set
      return;
    case T.specular:
      setBooleanPropertyTok(key, tok, value == 1);
      return;
    case T.specularpercent:
      value = checkIntRange(value, 0, 100);
      gdata.setSpecularPercent(value);
      break;
    case T.diffusepercent:
      value = checkIntRange(value, 0, 100);
      gdata.setDiffusePercent(value);
      break;
    case T.ambientpercent:
      value = checkIntRange(value, 0, 100);
      gdata.setAmbientPercent(value);
      break;
    case T.zdepth:
      tm.zDepthToPercent(value);
      break;
    case T.zslab:
      tm.zSlabToPercent(value);
      break;
    case T.depth:
      tm.depthToPercent(value);
      break;
    case T.slab:
      tm.slabToPercent(value);
      break;
    case T.zshadepower:
      g.zShadePower = value = Math.max(value, 0);
      break;
    case T.ribbonaspectratio:
      g.ribbonAspectRatio = value;
      break;
    case T.pickingspinrate:
      g.pickingSpinRate = (value < 1 ? 1 : value);
      break;
    case T.animationfps:
      setAnimationFps(value);
      return;
    case T.percentvdwatom:
      setPercentVdwAtom(value);
      break;
    case T.hermitelevel:
      g.hermiteLevel = value;
      break;
    case T.ellipsoiddotcount: // 11.5.30
    case T.propertyatomnumbercolumncount:
    case T.propertyatomnumberfield: // 11.6.RC16
    case T.propertydatacolumncount:
    case T.propertydatafield: // 11.1.31
      // just save in the hashtable, not in global
      break;
    default:
      // stateversion is not tokenized
      if (!g.htNonbooleanParameterValues.containsKey(key)) {
        g.setUserVariable(key, SV.newI(value));
        return;
      }
    }
    g.setI(key, value);
  }

  private static int checkIntRange(int value, int min, int max) {
    return (value < min ? min : value > max ? max : value);
  }

  private static float checkFloatRange(float value, float min, float max) {
    return (value < min ? min : value > max ? max : value);
  }

  @Override
  public void setBooleanProperty(String key, boolean value) {
    if (key == null || key.length() == 0)
      return;
    if (key.charAt(0) == '_') {
      g.setB(key, value);
      return;
    }
    int tok = T.getTokFromName(key);
    switch (T.getParamType(tok)) {
    case T.strparam:
      setStringPropertyTok(key, tok, "");
      break;
    case T.intparam:
      setIntPropertyTok(key, tok, value ? 1 : 0);
      break;
    case T.floatparam:
      setFloatPropertyTok(key, tok, value ? 1 : 0);
      break;
    default:
      setBooleanPropertyTok(key, tok, value);
    }
  }

  private void setBooleanPropertyTok(String key, int tok, boolean value) {
    boolean doRepaint = true;
    switch (tok) {
    case T.multiplebondbananas:
      // 14.3.15
      g.multipleBondBananas = value;
      break;
    case T.modulateoccupancy:
      // 12.0.RC6
      g.modulateOccupancy = value;
      break;
    case T.legacyjavafloat:
      // 14.3.5
      g.legacyJavaFloat = value;
      break;
    case T.showmodvecs:
      // 14.3.5
      g.showModVecs = value;
      break;
    case T.showunitcelldetails:
      // 14.1.16
      g.showUnitCellDetails = value;
      break;
    case T.fractionalrelative:
      // REMOVED in 14.1.16
      // an odd quantity -- relates specifically to scripts commands
      // using fx, fy, fz, fxyz, ux, uy, uz, uxyz, and cell=
      // It should never have been set in state, as it has nothing to do with the state 
      // Its use with cell= was never documented.
      // It makes no sense to change the unit cell and not change the 
      // meaning of fx, fy, fz, fxyz, ux, uy, uz, uxyz, and cell=
      // Its presence caused unitcell [{origin} {a} {b} {c}] to fail.

      //g.fractionalRelative = value;
      doRepaint = false;
      break;
    case T.vectorscentered:
      // 14.1.15
      g.vectorsCentered = value;
      break;
    case T.backbonesteps:
      // 14.1.14
      g.backboneSteps = value;
      break;
    case T.cartoonribose:
      // 14.1.8
      g.cartoonRibose = value;
      if (value && getBoolean(T.cartoonbaseedges))
        setBooleanPropertyTok("cartoonBaseEdges", T.cartoonbaseedges, false);
      break;
    case T.ellipsoidarrows:
      // 13.1.17 TRUE for little points on ellipsoids showing sign of 
      // eigenvalues (in --> negative; out --> positive)
      g.ellipsoidArrows = value;
      break;
    case T.translucent:
      // 13.1.17 false -> translucent objects are opaque among themselves (Pymol transparency_mode 2)
      g.translucent = value;
      break;
    case T.cartoonladders:
      // 13.1.15
      g.cartoonLadders = value;
      break;
    case T.twistedsheets:
      boolean b = g.twistedSheets;
      g.twistedSheets = value;
      if (b != value)
        checkCoordinatesChanged();
      break;
    case T.celshading:
      // 13.1.13
      gdata.setCel(value);
      break;
    case T.cartoonsfancy:
      // 12.3.7
      g.cartoonFancy = value;
      break;
    case T.showtiming:
      // 12.3.6
      g.showTiming = value;
      break;
    case T.vectorsymmetry:
      // 12.3.2
      g.vectorSymmetry = value;
      break;
    case T.isosurfacekey:
      // 12.2.RC5
      g.isosurfaceKey = value;
      break;
    case T.partialdots:
      // Jmol 12.1.46
      g.partialDots = value;
      break;
    case T.legacyautobonding:
      g.legacyAutoBonding = value;
      break;
    case T.defaultstructuredssp:
      g.defaultStructureDSSP = value;
      break;
    case T.dsspcalchydrogen:
      g.dsspCalcHydrogen = value;
      break;
    case T.allowmodelkit:
      // 11.12.RC15
      g.allowModelkit = value;
      if (!value)
        setModelKitMode(false);
      break;
    case T.modelkitmode:
      setModelKitMode(value);
      break;
    case T.multiprocessor:
      // 12.0.RC6
      g.multiProcessor = value && (nProcessors > 1);
      break;
    case T.monitorenergy:
      // 12.0.RC6
      g.monitorEnergy = value;
      break;
    case T.hbondsrasmol:
      // 12.0.RC3
      g.hbondsRasmol = value;
      break;
    case T.minimizationrefresh:
      g.minimizationRefresh = value;
      break;
    case T.minimizationsilent:
      // 12.0.RC5
      g.minimizationSilent = value;
      break;
    //case T.usearcball:
    //g.useArcBall = value;
    //break;
    case T.iskiosk:
      // 11.9.29
      // 12.2.9, 12.3.9: no false here, because it's a one-time setting
      if (value) {
        isKiosk = true;
        g.disablePopupMenu = true;
        if (display != null)
          apiPlatform.setTransparentCursor(display);
      }
      break;
    // 11.9.28
    case T.waitformoveto:
      g.waitForMoveTo = value;
      break;
    case T.logcommands:
      g.logCommands = true;
      break;
    case T.loggestures:
      g.logGestures = true;
      break;
    case T.allowmultitouch:
      // 11.9.24
      g.allowMultiTouch = value;
      break;
    case T.preservestate:
      // 11.9.23
      g.preserveState = value;
      ms.setPreserveState(value);
      undoClear();
      break;
    case T.strutsmultiple:
      // 11.9.23
      g.strutsMultiple = value;
      break;
    case T.filecaching:
      // 11.9.21
      // not implemented -- application only -- CANNOT BE SET BY STATE
      break;
    case T.slabbyatom:
      // 11.9.19
      g.slabByAtom = value;
      break;
    case T.slabbymolecule:
      // 11.9.18
      g.slabByMolecule = value;
      break;
    case T.saveproteinstructurestate:
      // 11.9.15
      g.saveProteinStructureState = value;
      break;
    case T.allowgestures:
      g.allowGestures = value;
      break;
    case T.imagestate:
      // 11.8.RC6
      g.imageState = value;
      break;
    case T.useminimizationthread:
      // 11.7.40
      g.useMinimizationThread = value;
      break;
    // case Token.autoloadorientation:
    // // 11.7.30; removed in 12.0.RC10 -- use FILTER "NoOrient"
    // global.autoLoadOrientation = value;
    // break;
    case T.allowkeystrokes:
      // 11.7.24
      if (g.disablePopupMenu)
        value = false;
      g.allowKeyStrokes = value;
      break;
    case T.dragselected:
      // 11.7.24
      g.dragSelected = value;
      showSelected = false;
      break;
    case T.showkeystrokes:
      g.showKeyStrokes = value;
      break;
    case T.fontcaching:
      // 11.7.10
      g.fontCaching = value;
      break;
    case T.atompicking:
      // 11.6.RC13
      g.atomPicking = value;
      break;
    case T.bondpicking:
      // 11.6.RC13
      highlight(null);
      g.bondPicking = value;
      break;
    case T.selectallmodels:
      // 11.5.52
      g.selectAllModels = value;
      if (value)
        slm.setSelectionSubset(null);
      else
        am.setSelectAllSubset(false);
      break;
    case T.messagestylechime:
      // 11.5.39
      g.messageStyleChime = value;
      break;
    case T.pdbsequential:
      g.pdbSequential = value;
      break;
    case T.pdbaddhydrogens:
      g.pdbAddHydrogens = value;
      break;
    case T.pdbgetheader:
      g.pdbGetHeader = value;
      break;
    case T.ellipsoidaxes:
      g.ellipsoidAxes = value;
      break;
    case T.ellipsoidarcs:
      g.ellipsoidArcs = value;
      break;
    case T.ellipsoidball:
      g.ellipsoidBall = value;
      break;
    case T.ellipsoiddots:
      g.ellipsoidDots = value;
      break;
    case T.ellipsoidfill:
      g.ellipsoidFill = value;
      break;
    case T.fontscaling:
      // 11.5.4
      g.fontScaling = value;
      break;
    case T.syncmouse:
      // 11.3.56
      setSyncTarget(0, value);
      break;
    case T.syncscript:
      setSyncTarget(1, value);
      break;
    case T.wireframerotation:
      // 11.3.55
      g.wireframeRotation = value;
      break;
    case T.isosurfacepropertysmoothing:
      // 11.3.46
      g.isosurfacePropertySmoothing = value;
      break;
    case T.drawpicking:
      // 11.3.43
      g.drawPicking = value;
      break;
    case T.antialiasdisplay:
      // 11.3.36
    case T.antialiastranslucent:
    case T.antialiasimages:
      setAntialias(tok, value);
      break;
    case T.smartaromatic:
      // 11.3.29
      g.smartAromatic = value;
      break;
    case T.applysymmetrytobonds:
      // 11.1.29
      setApplySymmetryToBonds(value);
      break;
    case T.appendnew:
      // 11.1.22
      g.appendNew = value;
      break;
    case T.autofps:
      g.autoFps = value;
      break;
    case T.usenumberlocalization:
      // 11.1.21
      DF.setUseNumberLocalization(g.useNumberLocalization = value);
      break;
    case T.showfrank:
    case T.frank:
      key = "showFrank";
      setFrankOn(value);
      // 11.1.20
      break;
    case T.solvent:
      key = "solventProbe";
      g.solventOn = value;
      break;
    case T.solventprobe:
      g.solventOn = value;
      break;
    case T.allowrotateselected:
      // 11.1.14
      g.allowRotateSelected = value;
      break;
    case T.allowmoveatoms:
      // 12.1.21
      //setBooleanProperty("allowRotateSelected", value);
      //setBooleanProperty("dragSelected", value);
      g.allowMoveAtoms = value;
      showSelected = false;
      break;
    case T.showscript:
      // /11.1.13///
      setIntPropertyTok("showScript", tok, value ? 1 : 0);
      return;
    case T.allowembeddedscripts:
      // /11.1///
      g.allowEmbeddedScripts = value;
      break;
    case T.navigationperiodic:
      g.navigationPeriodic = value;
      break;
    case T.zshade:
      tm.setZShadeEnabled(value);
      return;
    case T.drawhover:
      if (haveDisplay)
        g.drawHover = value;
      break;
    case T.navigationmode:
      setNavigationMode(value);
      break;
    case T.navigatesurface:
      // was experimental; abandoned in 13.1.10
      return;//global.navigateSurface = value;
      //break;
    case T.hidenavigationpoint:
      g.hideNavigationPoint = value;
      break;
    case T.shownavigationpointalways:
      g.showNavigationPointAlways = value;
      break;
    case T.refreshing:
      // /11.0///
      setRefreshing(value);
      break;
    case T.justifymeasurements:
      g.justifyMeasurements = value;
      break;
    case T.ssbondsbackbone:
      g.ssbondsBackbone = value;
      break;
    case T.hbondsbackbone:
      g.hbondsBackbone = value;
      break;
    case T.hbondssolid:
      g.hbondsSolid = value;
      break;
    case T.specular:
      gdata.setSpecular(value);
      break;
    case T.slabenabled:
      // Eval.slab
      tm.setSlabEnabled(value); // refresh?
      return;
    case T.zoomenabled:
      tm.setZoomEnabled(value);
      return;
    case T.highresolution:
      g.highResolutionFlag = value;
      break;
    case T.tracealpha:
      g.traceAlpha = value;
      break;
    case T.zoomlarge:
      g.zoomLarge = value;
      tm.setZoomHeight(g.zoomHeight, value);
      break;
    case T.zoomheight:
      g.zoomHeight = value;
      tm.setZoomHeight(value, g.zoomLarge);
      break;
    case T.languagetranslation:
      GT.setDoTranslate(value);
      break;
    case T.hidenotselected:
      slm.setHideNotSelected(value);
      break;
    case T.scriptqueue:
      setScriptQueue(value);
      break;
    case T.dotsurface:
      g.dotSurface = value;
      break;
    case T.dotsselectedonly:
      g.dotsSelectedOnly = value;
      break;
    case T.selectionhalos:
      setSelectionHalosEnabled(value);
      break;
    case T.selecthydrogen:
      g.rasmolHydrogenSetting = value;
      break;
    case T.selecthetero:
      g.rasmolHeteroSetting = value;
      break;
    case T.showmultiplebonds:
      g.showMultipleBonds = value;
      break;
    case T.showhiddenselectionhalos:
      g.showHiddenSelectionHalos = value;
      break;
    case T.windowcentered:
      tm.setWindowCentered(value);
      break;
    case T.displaycellparameters:
      g.displayCellParameters = value;
      break;
    case T.testflag1:
      g.testFlag1 = value;
      break;
    case T.testflag2:
      g.testFlag2 = value;
      break;
    case T.testflag3:
      g.testFlag3 = value;
      break;
    case T.testflag4:
      jmolTest();
      g.testFlag4 = value;
      break;
    case T.ribbonborder:
      g.ribbonBorder = value;
      break;
    case T.cartoonbaseedges:
      g.cartoonBaseEdges = value;
      if (value && getBoolean(T.cartoonribose))
        setBooleanPropertyTok("cartoonRibose", T.cartoonribose, false);
      break;
    case T.cartoonrockets:
      g.cartoonRockets = value;
      break;
    case T.rocketbarrels:
      g.rocketBarrels = value;
      break;
    case T.greyscalerendering:
      gdata.setGreyscaleMode(g.greyscaleRendering = value);
      break;
    case T.measurementlabels:
      g.measurementLabels = value;
      break;
    case T.axeswindow:
    case T.axesmolecular:
    case T.axesunitcell:
      setAxesMode(tok);
      return;
    case T.axesorientationrasmol:
      // public; no need to set here
      setAxesOrientationRasmol(value);
      return;
    case T.colorrasmol:
      setStringPropertyTok("defaultcolorscheme", T.defaultcolorscheme,
          value ? "rasmol" : "jmol");
      return;
    case T.debugscript:
      setDebugScript(value);
      return;
    case T.perspectivedepth:
      setPerspectiveDepth(value);
      return;
    case T.autobond:
      // public - no need to set
      setAutoBond(value);
      return;
    case T.showaxes:
      setShowAxes(value);
      return;
    case T.showboundbox:
      setShowBbcage(value);
      return;
    case T.showhydrogens:
      setShowHydrogens(value);
      return;
    case T.showmeasurements:
      setShowMeasurements(value);
      return;
    case T.showunitcell:
      setShowUnitCell(value);
      return;
    case T.bondmodeor:
      doRepaint = false;
      g.bondModeOr = value;
      break;
    case T.zerobasedxyzrasmol:
      doRepaint = false;
      g.zeroBasedXyzRasmol = value;
      reset(true);
      break;
    case T.rangeselected:
      doRepaint = false;
      g.rangeSelected = value;
      break;
    case T.measureallmodels:
      doRepaint = false;
      g.measureAllModels = value;
      break;
    case T.statusreporting:
      doRepaint = false;
      // not part of the state
      sm.allowStatusReporting = value;
      break;
    case T.chaincasesensitive:
      doRepaint = false;
      g.chainCaseSensitive = value;
      break;
    case T.hidenameinpopup:
      doRepaint = false;
      g.hideNameInPopup = value;
      break;
    case T.disablepopupmenu:
      doRepaint = false;
      g.disablePopupMenu = value;
      break;
    case T.forceautobond:
      doRepaint = false;
      g.forceAutoBond = value;
      break;
    default:
      if (!g.htBooleanParameterFlags.containsKey(key.toLowerCase())) {
        g.setUserVariable(key, SV.getBoolean(value));
        return;
      }
    }
    g.setB(key, value);
    if (doRepaint)
      setTainted(true);
  }

  /*
   * public void setFileCacheDirectory(String fileOrDir) { if (fileOrDir ==
   * null) fileOrDir = ""; global._fileCache = fileOrDir; }
   * 
   * String getFileCacheDirectory() { if (!global._fileCaching) return null;
   * return global._fileCache; }
   */

  private void setModelKitMode(boolean value) {
    if (acm == null || !allowScripting)
      return;
    if (value || g.modelKitMode) {
      setPickingMode(null, value ? ActionManager.PICKING_ASSIGN_BOND
          : ActionManager.PICKING_IDENTIFY);
      setPickingMode(null, value ? ActionManager.PICKING_ASSIGN_ATOM
          : ActionManager.PICKING_IDENTIFY);
    }
    boolean isChange = (g.modelKitMode != value);
    g.modelKitMode = value;
    highlight(null);
    if (value) {
      setNavigationMode(false);
      selectAll();
      // setShapeProperty(JmolConstants.SHAPE_LABELS, "color", "RED");
      setAtomPickingOption("C");
      setBondPickingOption("p");
      if (!isApplet)
        popupMenu(0, 0, 'm');
      if (isChange)
        sm.setCallbackFunction("modelkit", "ON");
      g.modelKitMode = true;
      if (ms.ac == 0)
        zap(false, true, true);
    } else {
      acm.setPickingMode(-1);
      setStringProperty("pickingStyle", "toggle");
      setBooleanProperty("bondPicking", false);
      if (isChange)
        sm.setCallbackFunction("modelkit", "OFF");
    }
  }

  public void setSmilesString(String s) {
    if (s == null)
      g.removeParam("_smilesString");
    else
      g.setO("_smilesString", s);
  }

  public void removeUserVariable(String key) {
    g.removeUserVariable(key);
    if (key.endsWith("callback"))
      sm.setCallbackFunction(key, null);
  }

  private void jmolTest() {
    /*
     * Vector v = new Vector(); Vector m = new Vector(); v.add(m);
     * m.add("MODEL     2");m.add(
     * "HETATM    1 H1   UNK     1       2.457   0.000   0.000  1.00  0.00           H  "
     * );m.add(
     * "HETATM    2 C1   UNK     1       1.385   0.000   0.000  1.00  0.00           C  "
     * );m.add(
     * "HETATM    3 C2   UNK     1      -1.385  -0.000   0.000  1.00  0.00           C  "
     * ); v.add(new String[] { "MODEL     2",
     * "HETATM    1 H1   UNK     1       2.457   0.000   0.000  1.00  0.00           H  "
     * ,
     * "HETATM    2 C1   UNK     1       1.385   0.000   0.000  1.00  0.00           C  "
     * ,
     * "HETATM    3 C2   UNK     1      -1.385  -0.000   0.000  1.00  0.00           C  "
     * , }); v.add(new String[] {"3","testing","C 0 0 0","O 0 1 0","N 0 0 1"} );
     * v.add("3\ntesting\nC 0 0 0\nO 0 1 0\nN 0 0 1\n"); loadInline(v, false);
     */
  }

  public void showParameter(String key, boolean ifNotSet, int nMax) {
    String sv = "" + g.getParameterEscaped(key, nMax);
    if (ifNotSet || sv.indexOf("<not defined>") < 0)
      showString(key + " = " + sv, false);
  }

  public void showString(String str, boolean isPrint) {
    if (!isJS && isScriptQueued() && (!isSilent || isPrint) && !"\0".equals(str)) {
      Logger.warn(str); // warn here because we still want to be be able to turn this off
    }
    scriptEcho(str);
  }

  public String getAllSettings(String prefix) {
    return getStateCreator().getAllSettings(prefix);
  }

  public String getBindingInfo(String qualifiers) {
    return (haveDisplay ? acm.getBindingInfo(qualifiers) : "");
  }

  // ////// flags and settings ////////

  public int getIsosurfacePropertySmoothing(boolean asPower) {
    // Eval
    return (asPower ? g.isosurfacePropertySmoothingPower
        : g.isosurfacePropertySmoothing ? 1 : 0);
  }

  public void setNavigationDepthPercent(float percent) {
    tm.setNavigationDepthPercent(percent);
    refresh(1, "set navigationDepth");
  }

  public boolean getShowNavigationPoint() {
    if (!g.navigationMode/* || !tm.canNavigate()*/)
      return false;
    return (tm.isNavigating() && !g.hideNavigationPoint
        || g.showNavigationPointAlways || getInMotion(true));
  }

  public float getCurrentSolventProbeRadius() {
    return g.solventOn ? g.solventProbeRadius : 0;
  }

  public boolean getTestFlag(int i) {
    switch (i) {
    case 1:
      // no PNGJ caching
      return g.testFlag1;
    case 2:
      // passed to MOCalcuation, but not used
      // nciCalculation special params.testFlag = 2 "absolute" calc.
      // GIF reducedColors
      return g.testFlag2;
    case 3:
      // isosurface numbers
      // polyhedra numbers
      return g.testFlag3;
    case 4:
      // isosurface normals
      // contact -- true: do not edit Cp list
      return g.testFlag4;
    }
    return false;
  }

  @Override
  public void setPerspectiveDepth(boolean perspectiveDepth) {
    // setBooleanProperty
    // stateManager.setCrystallographicDefaults
    // app preferences dialog
    tm.setPerspectiveDepth(perspectiveDepth);
  }

  @Override
  public void setAxesOrientationRasmol(boolean TF) {
    // app PreferencesDialog
    // stateManager
    // setBooleanproperty
    /*
     * *************************************************************** RasMol
     * has the +Y axis pointing down And rotations about the y axis are
     * left-handed setting this flag makes Jmol mimic this behavior
     * 
     * All versions of Jmol prior to 11.5.51 incompletely implement this flag.
     * All versions of Jmol between 11.5.51 and 12.2.4 incorrectly implement this flag.
     * Really all it is just a flag to tell Eval to flip the sign of the Z
     * rotation when specified specifically as "rotate/spin Z 30".
     * 
     * In principal, we could display the axis opposite as well, but that is
     * only aesthetic and not at all justified if the axis is molecular.
     * **************************************************************
     */
    g.setB("axesOrientationRasmol", TF);
    g.axesOrientationRasmol = TF;
    reset(true);
  }

  void setAxesScale(float scale) {
    scale = checkFloatRange(scale, -100, 100);
    g.axesScale = scale;
    axesAreTainted = true;
  }

  public P3[] getAxisPoints() {
    // for uccage renderer
    shm.loadShape(JC.SHAPE_AXES);
    return (getObjectMad(StateManager.OBJ_AXIS1) == 0
        || g.axesMode != T.axesunitcell
        || ((Boolean) getShapeProperty(JC.SHAPE_AXES, "axesTypeXY"))
            .booleanValue()
        || getShapeProperty(JC.SHAPE_AXES, "origin") != null ? null
        : (P3[]) getShapeProperty(JC.SHAPE_AXES, "axisPoints"));
  }

  void setAxesMode(int mode) {
    g.axesMode = mode;
    axesAreTainted = true;
    switch (mode) {
    case T.axesunitcell:
      // stateManager
      // setBooleanproperty
      g.removeParam("axesmolecular");
      g.removeParam("axeswindow");
      g.setB("axesUnitcell", true);
      mode = 2;
      break;
    case T.axesmolecular:
      g.removeParam("axesunitcell");
      g.removeParam("axeswindow");
      g.setB("axesMolecular", true);
      mode = 1;
      break;
    case T.axeswindow:
      g.removeParam("axesunitcell");
      g.removeParam("axesmolecular");
      g.setB("axesWindow", true);
      mode = 0;
    }
    g.setI("axesMode", mode);
  }

  private boolean selectionHalosEnabled = false;

  public boolean getSelectionHalosEnabled() {
    return selectionHalosEnabled;
  }

  public void setSelectionHalosEnabled(boolean TF) {
    if (selectionHalosEnabled == TF)
      return;
    g.setB("selectionHalos", TF);
    shm.loadShape(JC.SHAPE_HALOS);
    selectionHalosEnabled = TF;
  }

  public boolean getShowSelectedOnce() {
    boolean flag = showSelected;
    showSelected = false;
    return flag;
  }

  private void setStrandCount(int type, int value) {
    value = checkIntRange(value, 0, 20);
    switch (type) {
    case JC.SHAPE_STRANDS:
      g.strandCountForStrands = value;
      break;
    case JC.SHAPE_MESHRIBBON:
      g.strandCountForMeshRibbon = value;
      break;
    default:
      g.strandCountForStrands = value;
      g.strandCountForMeshRibbon = value;
      break;
    }
    g.setI("strandCount", value);
    g.setI("strandCountForStrands", g.strandCountForStrands);
    g.setI("strandCountForMeshRibbon", g.strandCountForMeshRibbon);
  }

  public int getStrandCount(int type) {
    return (type == JC.SHAPE_STRANDS ? g.strandCountForStrands
        : g.strandCountForMeshRibbon);
  }

  public void setNavigationMode(boolean TF) {
    g.navigationMode = TF;
    tm.setNavigationMode(TF);
  }

  @Override
  public void setAutoBond(boolean TF) {
    // setBooleanProperties
    g.setB("autobond", TF);
    g.autoBond = TF;
  }

  public int[] makeConnections(float minDistance, float maxDistance, int order,
                               int connectOperation, BS bsA, BS bsB,
                               BS bsBonds, boolean isBonds, boolean addGroup,
                               float energy) {
    // eval
    clearModelDependentObjects();
    // removed in 12.3.2 and 12.2.1; cannot remember why this was important
    // we aren't removing atoms, just bonds. So who cares in terms of measurements?
    // clearAllMeasurements(); // necessary for serialization (??)
    clearMinimization();
    return ms.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds, addGroup, energy);
  }

  @Override
  public void rebond() {
    // PreferencesDialog
    rebondState(false);
  }

  public void rebondState(boolean isStateScript) {
    // Eval CONNECT
    clearModelDependentObjects();
    ms.deleteAllBonds();
    boolean isLegacy = isStateScript && g.legacyAutoBonding;
    ms.autoBondBs4(null, null, null, null, getMadBond(), isLegacy);
    addStateScript(
        (isLegacy ? "set legacyAutoBonding TRUE;connect;set legacyAutoBonding FALSE;"
            : "connect;"), false, true);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  @Override
  public void setPercentVdwAtom(int value) {
    g.setI("percentVdwAtom", value);
    g.percentVdwAtom = value;
    rd.value = value / 100f;
    rd.factorType = EnumType.FACTOR;
    rd.vdwType = VDW.AUTO;
    shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, rd, null);
  }

  @Override
  public short getMadBond() {
    return (short) (g.bondRadiusMilliAngstroms * 2);
  }

  @Override
  public void setShowHydrogens(boolean TF) {
    // PreferencesDialog
    // setBooleanProperty
    g.setB("showHydrogens", TF);
    g.showHydrogens = TF;
  }

  public void setShowBbcage(boolean value) {
    setObjectMad(JC.SHAPE_BBCAGE, "boundbox", (short) (value ? -4 : 0));
    g.setB("showBoundBox", value);
  }

  public boolean getShowBbcage() {
    return getObjectMad(StateManager.OBJ_BOUNDBOX) != 0;
  }

  public void setShowUnitCell(boolean value) {
    setObjectMad(JC.SHAPE_UCCAGE, "unitcell", (short) (value ? -2 : 0));
    g.setB("showUnitCell", value);
  }

  public boolean getShowUnitCell() {
    return getObjectMad(StateManager.OBJ_UNITCELL) != 0;
  }

  public void setShowAxes(boolean value) {
    setObjectMad(JC.SHAPE_AXES, "axes", (short) (value ? -2 : 0));
    g.setB("showAxes", value);
  }

  public boolean getShowAxes() {
    return getObjectMad(StateManager.OBJ_AXIS1) != 0;
  }

  public boolean frankOn = true;
  public boolean noFrankEcho = true; // set when Echo bottom right renders

  @Override
  public void setFrankOn(boolean TF) {
    if (isPreviewOnly)
      TF = false;
    frankOn = TF;
    setObjectMad(JC.SHAPE_FRANK, "frank", (short) (TF ? 1 : 0));
  }

  public boolean getShowFrank() {
    if (isPreviewOnly || isApplet && creatingImage)
      return false;
    // Java remote signed applet only?
    return (isSignedApplet && !isSignedAppletLocal && !isJS || frankOn);
  }

  @Override
  public void setShowMeasurements(boolean TF) {
    // setbooleanProperty
    g.setB("showMeasurements", TF);
    g.showMeasurements = TF;
  }

  public void setUnits(String units, boolean isDistance) {
    // stateManager
    // Eval
    g.setUnits(units);
    if (isDistance) {
      g.setUnits(units);
      setShapeProperty(JC.SHAPE_MEASURES, "reformatDistances", null);
    } else {

    }
  }

  @Override
  public void setRasmolDefaults() {
    setDefaultsType("RasMol");
  }

  @Override
  public void setJmolDefaults() {
    setDefaultsType("Jmol");
  }

  private void setDefaultsType(String type) {
    if (type.equalsIgnoreCase("RasMol")) {
      stm.setRasMolDefaults();
      return;
    }
    if (type.equalsIgnoreCase("PyMOL")) {
      stm.setPyMOLDefaults();
      return;
    }
    stm.setJmolDefaults();
    setIntProperty("bondingVersion", Elements.RAD_COV_IONIC_OB1_100_1);
    shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, rd, getAllAtoms());
  }

  private void setAntialias(int tok, boolean TF) {

    switch (tok) {
    case T.antialiasdisplay:
      g.antialiasDisplay = TF;
      break;
    case T.antialiastranslucent:
      g.antialiasTranslucent = TF;
      break;
    case T.antialiasimages:
      g.antialiasImages = TF;
      return;
    }
    resizeImage(0, 0, false, false, true);
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

  public P3[] allocTempPoints(int size) {
    // rockets cartoons renderer only
    return tempArray.allocTempPoints(size);
  }

  public void freeTempPoints(P3[] tempPoints) {
    // rockets, cartoons render only
    tempArray.freeTempPoints(tempPoints);
  }

  public P3i[] allocTempScreens(int size) {
    // mesh and mps
    return tempArray.allocTempScreens(size);
  }

  public void freeTempScreens(P3i[] tempScreens) {
    tempArray.freeTempScreens(tempScreens);
  }

  public STR[] allocTempEnum(int size) {
    // mps renderer
    return tempArray.allocTempEnum(size);
  }

  public void freeTempEnum(STR[] temp) {
    tempArray.freeTempEnum(temp);
  }

  // //////////////////////////////////////////////////////////////
  // font stuff
  // //////////////////////////////////////////////////////////////
  public Font getFont3D(String fontFace, String fontStyle, float fontSize) {
    return gdata.getFont3DFSS(fontFace, fontStyle, fontSize);
  }

  // //////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  // //////////////////////////////////////////////////////////////

  public Quat[] getAtomGroupQuaternions(BS bsAtoms, int nMax) {
    return ms.getAtomGroupQuaternions(bsAtoms, nMax, getQuaternionFrame());
  }

  // //////////////////////////////////////////////////////////////
  // stereo support
  // //////////////////////////////////////////////////////////////

  public void setStereoMode(int[] twoColors, STER stereoMode, float degrees) {
    setFloatProperty("stereoDegrees", degrees);
    setBooleanProperty("greyscaleRendering", stereoMode.isBiColor());
    if (twoColors != null)
      tm.setStereoMode2(twoColors);
    else
      tm.setStereoMode(stereoMode);
  }

  // //////////////////////////////////////////////////////////////
  //
  // //////////////////////////////////////////////////////////////

  // /////////////// getProperty /////////////

  public boolean scriptEditorVisible;

  public JmolAppConsoleInterface appConsole;
  JmolScriptEditorInterface scriptEditor;
  GenericMenuInterface jmolpopup;
  private GenericMenuInterface modelkitPopup;
  private Map<String, Object> headlessImageParams;

  public String getChimeInfo(int tok) {
    return getPropertyManager().getChimeInfo(tok, bsA());
  }

  public String getModelFileInfo() {
    return getPropertyManager().getModelFileInfo(getVisibleFramesBitSet());
  }

  public String getModelFileInfoAll() {
    return getPropertyManager().getModelFileInfo(null);
  }

  @Override
  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    // accepts a BitSet paramInfo
    // return types include "JSON", "String", "readable", and anything else
    // returns the Java object.
    // Jmol 11.7.45 also uses this method as a general API
    // for getting and returning script data from the console and editor

    if (!"DATA_API".equals(returnType))
      return getPropertyManager().getProperty(returnType, infoType, paramInfo);

    switch (("scriptCheck........." // 0
        + "consoleText........." // 20
        + "scriptEditor........" // 40
        + "scriptEditorState..." // 60
        + "getAppConsole......." // 80
        + "getScriptEditor....." // 100
        + "setMenu............." // 120
        + "spaceGroupInfo......" // 140
        + "disablePopupMenu...." // 160
        + "defaultDirectory...." // 180
        + "getPopupMenu........" // 200
        + "shapeManager........" // 220
        + "getPreference......." // 240
    ).indexOf(infoType)) {

    case 0:
      return scriptCheckRet((String) paramInfo, true);
    case 20:
      return (appConsole == null ? "" : appConsole.getText());
    case 40:
      showEditor((String[]) paramInfo);
      return null;
    case 60:
      scriptEditorVisible = ((Boolean) paramInfo).booleanValue();
      return null;
    case 80:
      if (isKiosk) {
        appConsole = null;
      } else if (paramInfo instanceof JmolAppConsoleInterface) {
        appConsole = (JmolAppConsoleInterface) paramInfo;
      } else if (paramInfo != null && !((Boolean) paramInfo).booleanValue()) {
        appConsole = null;
      } else if (appConsole == null && paramInfo != null
          && ((Boolean) paramInfo).booleanValue()) {
        if (isJS) {
          appConsole = (JmolAppConsoleInterface) Interface.getOption(
              "consolejs.AppletConsole", this, "script");
        }
        /**
         * @j2sNative
         * 
         * 
         */
        {
          for (int i = 0; i < 4 && appConsole == null; i++) {
            appConsole = (isApplet ? (JmolAppConsoleInterface) Interface
                .getOption("console.AppletConsole", null, null)
                : (JmolAppConsoleInterface) Interface.getInterface(
                    "org.openscience.jmol.app.jmolpanel.console.AppConsole",
                    null, null));
            if (appConsole == null)
              try {
                System.out.println("Viewer can't start appConsole");
                Thread.currentThread().wait(100);
              } catch (InterruptedException e) {
                //
              }
          }
        }
        if (appConsole != null)
          appConsole.start(this);
      }
      scriptEditor = (isJS || appConsole == null ? null : appConsole
          .getScriptEditor());
      return appConsole;
    case 100:
      if (appConsole == null && paramInfo != null
          && ((Boolean) paramInfo).booleanValue()) {
        getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
        scriptEditor = (appConsole == null ? null : appConsole
            .getScriptEditor());
      }
      return scriptEditor;
    case 120:
      if (jmolpopup != null)
        jmolpopup.jpiDispose();
      jmolpopup = null;
      return menuStructure = (String) paramInfo;
    case 140:
      return ms.getSymTemp(true).getSpaceGroupInfo(ms, null);
    case 160:
      g.disablePopupMenu = true; // no false here, because it's a
      // one-time setting
      return null;
    case 180:
      return g.defaultDirectory;
    case 200:
      if (paramInfo instanceof String)
        return getMenu((String) paramInfo);
      return getPopupMenu();
    case 220:
      return shm.getProperty(paramInfo);
    case 240:
      return sm.syncSend("getPreference", paramInfo, 1);
    }
    Logger.error("ERROR in getProperty DATA_API: " + infoType);
    return null;
  }

  public void showEditor(String[] file_text) {
    JmolScriptEditorInterface scriptEditor = (JmolScriptEditorInterface) getProperty(
        "DATA_API", "getScriptEditor", Boolean.TRUE);
    if (scriptEditor == null)
      return;
    scriptEditor.show(file_text);
  }

  JmolPropertyManager pm;

  private JmolPropertyManager getPropertyManager() {
    if (pm == null)
      (pm = (JmolPropertyManager) Interface.getInterface(
          "org.jmol.viewer.PropertyManager", this, "prop")).setViewer(this);
    return pm;
  }

  // ////////////////////////////////////////////////

  boolean isTainted = true;

  public void setTainted(boolean TF) {
    isTainted = axesAreTainted = (TF && (refreshing || creatingImage));
  }

  public int notifyMouseClicked(int x, int y, int action, int mode) {
    // change y to 0 at bottom
    int modifiers = Binding.getButtonMods(action);
    int clickCount = Binding.getClickCount(action);
    g.setI("_mouseX", x);
    g.setI("_mouseY", dimScreen.height - y);
    g.setI("_mouseAction", action);
    g.setI("_mouseModifiers", modifiers);
    g.setI("_clickCount", clickCount);
    return sm.setStatusClicked(x, dimScreen.height - y, action, clickCount,
        mode);
  }

  Map<String, Object> checkObjectClicked(int x, int y, int modifiers) {
    return shm.checkObjectClicked(x, y, modifiers, getVisibleFramesBitSet(),
        g.drawPicking);
  }

  public boolean checkObjectHovered(int x, int y) {
    return (x >= 0 && shm != null && shm.checkObjectHovered(x, y,
        getVisibleFramesBitSet(), getBondPicking()));
  }

  void checkObjectDragged(int prevX, int prevY, int x, int y, int action) {
    int iShape = 0;
    switch (getPickingMode()) {
    case ActionManager.PICKING_LABEL:
      iShape = JC.SHAPE_LABELS;
      break;
    case ActionManager.PICKING_DRAW:
      iShape = JC.SHAPE_DRAW;
      break;
    }
    if (shm.checkObjectDragged(prevX, prevY, x, y, action,
        getVisibleFramesBitSet(), iShape)) {
      refresh(1, "checkObjectDragged");
      if (iShape == JC.SHAPE_DRAW)
        scriptEcho((String) getShapeProperty(JC.SHAPE_DRAW, "command"));
    }
    // TODO: refresh 1 or 2?
  }

  public boolean rotateAxisAngleAtCenter(JmolScriptEvaluator eval,
                                         P3 rotCenter, V3 rotAxis,
                                         float degreesPerSecond,
                                         float endDegrees, boolean isSpin,
                                         BS bsSelected) {
    // Eval: rotate FIXED
    boolean isOK = tm.rotateAxisAngleAtCenter(eval, rotCenter, rotAxis,
        degreesPerSecond, endDegrees, isSpin, bsSelected);
    if (isOK)
      setSync();
    return isOK;
  }

  public boolean rotateAboutPointsInternal(JmolScriptEvaluator eval, P3 point1,
                                           P3 point2, float degreesPerSecond,
                                           float endDegrees, boolean isSpin,
                                           BS bsSelected, V3 translation,
                                           Lst<P3> finalPoints,
                                           float[] dihedralList, M4 m4) {
    // Eval: rotate INTERNAL

    if (headless) {
      if (isSpin && endDegrees == Float.MAX_VALUE)
        return false;
      isSpin = false;
    }

    boolean isOK = tm.rotateAboutPointsInternal(eval, point1, point2,
        degreesPerSecond, endDegrees, false, isSpin, bsSelected, false,
        translation, finalPoints, dihedralList, m4);
    if (isOK)
      setSync();
    return isOK;
  }

  public void startSpinningAxis(T3 pt1, T3 pt2, boolean isClockwise) {
    // Draw.checkObjectClicked ** could be difficult
    // from draw object click
    if (tm.spinOn || tm.navOn) {
      tm.setSpinOff();
      tm.setNavOn(false);
      return;
    }
    tm.rotateAboutPointsInternal(null, pt1, pt2, g.pickingSpinRate,
        Float.MAX_VALUE, isClockwise, true, null, false, null, null, null, null);
  }

  public V3 getModelDipole() {
    return ms.getModelDipole(am.cmi);
  }

  public V3 calculateMolecularDipole(BS bsAtoms) {
    return ms.calculateMolecularDipole(am.cmi, bsAtoms);
  }

  public void setDefaultLattice(P3 p) {
    // Eval -- handled separately
    if (!Float.isNaN(p.x + p.y + p.z))
      g.ptDefaultLattice.setT(p);
    g.setO("defaultLattice", Escape.eP(p));
  }

  public P3 getDefaultLattice() {
    return g.ptDefaultLattice;
  }

  /**
   * 
   * V3000, SDF, JSON, CD, XYZ, XYZVIB, XYZRN, CML, PDB, PQR
   * 
   * @param atomExpression
   * @param doTransform
   * @param isModelKit
   * @param type
   * @return full file data
   * 
   */
  public String getModelExtract(Object atomExpression, boolean doTransform,
                                boolean isModelKit, String type) {
    return getPropertyManager().getModelExtract(getAtomBitSet(atomExpression),
        doTransform, isModelKit, type, false);
  }

  @Override
  public String getData(String atomExpression, String type) {
    // from GaussianDialog
    return getModelFileData(atomExpression, type, true);
  }

  /**
   * @param atomExpression -- will be wrapped in { } and evaluated
   * @param type -- lower case means "atom data only; UPPERCASE returns full file data
   * @param allTrajectories 
   * @return full or atom-only data formatted as specified
   */
  public String getModelFileData(String atomExpression, String type, boolean allTrajectories) {
    return getPropertyManager().getAtomData(atomExpression, type, allTrajectories);
  }

  public String getModelCml(BS bs, int nAtomsMax, boolean addBonds, boolean doTransform) {
    return getPropertyManager().getModelCml(bs, nAtomsMax, addBonds, doTransform, false);
  }

  public String getPdbAtomData(BS bs, OC out, boolean asPQR, boolean doTransform) {
    return getPropertyManager().getPdbAtomData(bs == null ? bsA() : bs, out, asPQR, doTransform, false);
  }

  public boolean isJmolDataFrame() {
    return ms.isJmolDataFrameForModel(am.cmi);
  }

  public void setFrameTitle(int modelIndex, String title) {
    ms.setFrameTitle(BSUtil.newAndSetBit(modelIndex), title);
  }

  public void setFrameTitleObj(Object title) {
    shm.loadShape(JC.SHAPE_ECHO);
    ms.setFrameTitle(getVisibleFramesBitSet(), title);
  }

  public String getFrameTitle() {
    return ms.getFrameTitle(am.cmi);
  }

  public void setAtomProperty(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    if (tok == T.vanderwaals)
      shm.deleteVdwDependentShapes(bs);
    clearMinimization();
    ms.setAtomProperty(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case T.atomx:
    case T.atomy:
    case T.atomz:
    case T.fracx:
    case T.fracy:
    case T.fracz:
    case T.unitx:
    case T.unity:
    case T.unitz:
    case T.element:
      refreshMeasures(true);
    }
  }

  public void checkCoordinatesChanged() {
    // note -- use of save/restore coordinates cannot 
    // track connected objects
    ms.recalculatePositionDependentQuantities(null, null);
    refreshMeasures(true);
  }

  public void setAtomCoords(BS bs, int tokType, Object xyzValues) {
    if (bs.isEmpty())
      return;
    ms.setAtomCoords(bs, tokType, xyzValues);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
  }

  public void setAtomCoordsRelative(T3 offset, BS bs) {
    // Eval
    if (bs == null)
      bs = bsA();
    if (bs.isEmpty())
      return;
    ms.setAtomCoordsRelative(offset, bs);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
  }

  public void invertAtomCoordPt(P3 pt, BS bs) {
    // Eval
    ms.invertSelected(pt, null, -1, null, bs);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
  }

  public void invertAtomCoordPlane(P4 plane, BS bs) {
    ms.invertSelected(null, plane, -1, null, bs);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
  }

  public void invertSelected(P3 pt, P4 plane, int iAtom, BS invAtoms) {
    // Eval
    BS bs = bsA();
    if (bs.isEmpty())
      return;
    ms.invertSelected(pt, plane, iAtom, invAtoms, bs);
    checkMinimization();
    sm.setStatusAtomMoved(bs);
  }

  public void moveAtoms(M4 m4, M3 mNew, M3 rotation, V3 translation, P3 center,
                        boolean isInternal, BS bsAtoms, boolean translationOnly) {
    // from TransformManager exclusively
    if (bsAtoms.isEmpty())
      return;
    ms.moveAtoms(m4, mNew, rotation, translation, bsAtoms, center, isInternal,
        translationOnly);
    checkMinimization();
    sm.setStatusAtomMoved(bsAtoms);
  }

  private boolean movingSelected;
  private boolean showSelected;

  public void moveSelected(int deltaX, int deltaY, int deltaZ, int x, int y,
                           BS bsSelected, boolean isTranslation, boolean asAtoms) {
    // called by actionManager
    // cannot synchronize this -- it's from the mouse and the event queue
    if (deltaZ == 0)
      return;
    if (x == Integer.MIN_VALUE)
      rotateBondIndex = -1;
    if (isJmolDataFrame())
      return;
    if (deltaX == Integer.MIN_VALUE) {
      showSelected = true;
      movableBitSet = setMovableBitSet(null, !asAtoms);
      shm.loadShape(JC.SHAPE_HALOS);
      refresh(6, "moveSelected");
      return;
    }
    if (deltaX == Integer.MAX_VALUE) {
      if (!showSelected)
        return;
      showSelected = false;
      movableBitSet = null;
      refresh(6, "moveSelected");
      return;
    }
    if (movingSelected)
      return;
    movingSelected = true;
    stopMinimization();
    // note this does not sync with applets
    if (rotateBondIndex >= 0 && x != Integer.MIN_VALUE) {
      actionRotateBond(deltaX, deltaY, x, y);
    } else {
      bsSelected = setMovableBitSet(bsSelected, !asAtoms);
      if (!bsSelected.isEmpty()) {
        if (isTranslation) {
          P3 ptCenter = ms.getAtomSetCenter(bsSelected);
          tm.finalizeTransformParameters();
          float f = (g.antialiasDisplay ? 2 : 1);
          P3i ptScreen = tm.transformPt(ptCenter);
          P3 ptScreenNew;
          if (deltaZ != Integer.MIN_VALUE)
            ptScreenNew = P3.new3(ptScreen.x, ptScreen.y, ptScreen.z + deltaZ
                + 0.5f);
          else
            ptScreenNew = P3.new3(ptScreen.x + deltaX * f + 0.5f, ptScreen.y
                + deltaY * f + 0.5f, ptScreen.z);
          P3 ptNew = new P3();
          tm.unTransformPoint(ptScreenNew, ptNew);
          // script("draw ID 'pt" + Math.random() + "' " + Escape.escape(ptNew));
          ptNew.sub(ptCenter);
          setAtomCoordsRelative(ptNew, bsSelected);
        } else {
          tm.rotateXYBy(deltaX, deltaY, bsSelected);
        }
      }
    }
    refresh(2, ""); // should be syncing here
    movingSelected = false;
  }

  public void highlightBond(int index, boolean isHover) {
    if (isHover && !hoverEnabled)
      return;
    BS bs = null;
    if (index >= 0) {
      Bond b = ms.bo[index];
      int i = b.atom2.i;
      if (!ms.isAtomAssignable(i))
        return;
      bs = BSUtil.newAndSetBit(i);
      bs.set(b.atom1.i);
    }
    highlight(bs);
    refresh(3, "highlightBond");
  }

  public void highlight(BS bs) {
    if (bs != null)
      shm.loadShape(JC.SHAPE_HALOS);
    setShapeProperty(JC.SHAPE_HALOS, "highlight", bs);
  }

  private int rotateBondIndex = -1;

  public void setRotateBondIndex(int index) {
    boolean haveBond = (rotateBondIndex >= 0);
    if (!haveBond && index < 0)
      return;
    rotatePrev1 = -1;
    bsRotateBranch = null;
    if (index == Integer.MIN_VALUE)
      return;
    rotateBondIndex = index;
    highlightBond(index, false);

  }

  int getRotateBondIndex() {
    return rotateBondIndex;
  }

  private int rotatePrev1 = -1;
  private int rotatePrev2 = -1;
  private BS bsRotateBranch;

  void actionRotateBond(int deltaX, int deltaY, int x, int y) {
    // called by actionManager
    if (rotateBondIndex < 0)
      return;
    BS bsBranch = bsRotateBranch;
    Atom atom1, atom2;
    if (bsBranch == null) {
      Bond b = ms.bo[rotateBondIndex];
      atom1 = b.atom1;
      atom2 = b.atom2;
      undoMoveActionClear(atom1.i, AtomCollection.TAINT_COORD, true);
      P3 pt = P3.new3(x, y, (atom1.sZ + atom2.sZ) / 2);
      tm.unTransformPoint(pt, pt);
      if (atom2.getCovalentBondCount() == 1
          || pt.distance(atom1) < pt.distance(atom2)
          && atom1.getCovalentBondCount() != 1) {
        Atom a = atom1;
        atom1 = atom2;
        atom2 = a;
      }
      if (Measure.computeAngleABC(pt, atom1, atom2, true) > 90
          || Measure.computeAngleABC(pt, atom2, atom1, true) > 90) {
        bsBranch = getBranchBitSet(atom2.i, atom1.i, true);
      }
      if (bsBranch != null)
        for (int n = 0, i = atom1.bonds.length; --i >= 0;) {
          if (bsBranch.get(atom1.getBondedAtomIndex(i)) && ++n == 2) {
            bsBranch = null;
            break;
          }
        }
      if (bsBranch == null) {
        bsBranch = ms.getMoleculeBitSetForAtom(atom1.i);
      }
      bsRotateBranch = bsBranch;
      rotatePrev1 = atom1.i;
      rotatePrev2 = atom2.i;
    } else {
      atom1 = ms.at[rotatePrev1];
      atom2 = ms.at[rotatePrev2];
    }
    V3 v1 = V3.new3(atom2.sX - atom1.sX, atom2.sY - atom1.sY, 0);
    V3 v2 = V3.new3(deltaX, deltaY, 0);
    v1.cross(v1, v2);
    float degrees = (v1.z > 0 ? 1 : -1) * v2.length();

    BS bs = BSUtil.copy(bsBranch);
    bs.andNot(slm.getMotionFixedAtoms());

    rotateAboutPointsInternal(eval, atom1, atom2, 0, degrees, false, bs, null,
        null, null, null);
  }

  public void refreshMeasures(boolean andStopMinimization) {
    setShapeProperty(JC.SHAPE_MEASURES, "refresh", null);
    if (andStopMinimization)
      stopMinimization();
  }

  /**
   * fills an array with data -- if nX < 0 and this would involve JavaScript,
   * then this reads a full set of Double[][] in one function call. Otherwise it
   * reads the values using individual function calls, which each return Double.
   * 
   * If the functionName begins with "file:" then data are read from a file
   * specified after the colon. The sign of nX is not relevant in that case. The
   * file may contain mixed numeric and non-numeric values; the non-numeric
   * values will be skipped by Parser.parseFloatArray
   * 
   * @param functionName
   * @param nX
   * @param nY
   * @return nX by nY array of floating values
   */
  public float[][] functionXY(String functionName, int nX, int nY) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString3(functionName.substring(5), false, null);
    else if (functionName.indexOf("data2d_") != 0)
      return sm.functionXY(functionName, nX, nY);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    float[][] fdata;
    if (data == null) {
      fdata = (float[][]) getDataObj(functionName, null, JmolDataManager.DATA_TYPE_AFF);
      if (fdata != null)
        return fdata;
      data = "";
    }
    fdata = new float[nX][nY];
    float[] f = new float[nX * nY];
    Parser.parseStringInfestedFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        fdata[i][j] = f[n++];
    return fdata;
  }

  public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString3(functionName.substring(5), false, null);
    else if (functionName.indexOf("data3d_") != 0)
      return sm.functionXYZ(functionName, nX, nY, nZ);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    nZ = Math.abs(nZ);
    float[][][] xyzdata;
    if (data == null) {
      xyzdata = (float[][][]) getDataObj(functionName, null, JmolDataManager.DATA_TYPE_AFF);
      if (xyzdata != null)
        return xyzdata;
      data = "";
    }
    xyzdata = new float[nX][nY][nZ];
    float[] f = new float[nX * nY * nZ];
    Parser.parseStringInfestedFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        for (int k = 0; k < nZ; k++)
          xyzdata[i][j][k] = f[n++];
    return xyzdata;
  }

  @Override
  public String extractMolData(String what) {
    if (what == null) {
      int i = am.cmi;
      if (i < 0)
        return null;
      what = getModelNumberDotted(i);
    }
    return getModelExtract(what, true, false, "V2000");
  }

  public String getNMRPredict(boolean openURL) {
    String molFile = getModelExtract("selected", true, false, "V2000");
    int pt = molFile.indexOf("\n");
    molFile = "Jmol " + version_date + molFile.substring(pt);
    if (openURL) {
      if (isApplet) {
        //TODO -- can do this if connected
        showUrl(g.nmrUrlFormat + molFile);
      } else {
        syncScript("true", "*", 0);
        syncScript("H1Simulate:", ".", 0);
      }
      return null;
    }
    String url = g.nmrPredictFormat + molFile;
    return getFileAsString3(url, false, null);
  }

  public void getHelp(String what) {
    if (g.helpPath.indexOf("?") < 0) {
      if (what.length() > 0 && what.indexOf("?") != 0)
        what = "?search=" + PT.rep(what, " ", "%20");
      what += (what.length() == 0 ? "?ver=" : "&ver=") + JC.version;
    } else {
      what = "&" + what;
    }
    showUrl(g.helpPath + what);
  }

  public String getChemicalInfo(String smiles, T t) {
    String info = null;
    char type = '/';
    switch ((t == null ? T.name : t.tok)) {
    case T.inchi:
      type = 'I';
      break;
    case T.inchikey:
      type = 'K';
      break;
    case T.stdinchi:
      type = 'T';
      break;
    case T.stdinchikey:
      type = 'S';
      break;
    case T.name:
      type = 'N';
      break;
    case T.image:
      type = '2';
      break;
    default:
      info = SV.sValue(t);
      if (info.equalsIgnoreCase("drawing") || info.equalsIgnoreCase("image"))
        type = '2';
      // the following should not be necessary
      // but an NCI server fault on 3/29/2014 required this
      //if (info.equals("sdf"))
      //info = "file?format=sdf";
    }
    String s = (String) setLoadFormat("_" + smiles, type, false);
    if (type == '2') { 
      fm.loadImage(s, "\1" + smiles, false);
      return  s;
    }
    if (type == '/')
      s += PT.rep(info, " ", "%20");
    return getFileAsString4(s, -1, false, false, false, "file");
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  /*
   * Moved from the consoles to vwr, since this could be of general interest,
   * it's more a property of Eval/Viewer, and the consoles are really just a
   * mechanism for getting user input and sending results, not saving a history
   * of it all. Ultimately I hope to integrate the mouse picking and possibly
   * periodic updates of position into this history to get a full history. We'll
   * see! BH 9/2006
   */

  /**
   * Adds one or more commands to the command history
   * 
   * @param command
   *        the command to add
   */
  public void addCommand(String command) {
    if (autoExit || !haveDisplay || !getPreserveState())
      return;
    commandHistory.addCommand(PT.replaceAllCharacters(command, "\r\n\t", " "));
  }

  /**
   * Removes one command from the command history
   * 
   * @return command removed
   */
  public String removeCommand() {
    return commandHistory.removeCommand();
  }

  /**
   * Options include: ; all n == Integer.MAX_VALUE ; n prev n >= 1 ; next n ==
   * -1 ; set max to -2 - n n <= -3 ; just clear n == -2 ; clear and turn off;
   * return "" n == 0 ; clear and turn on; return "" n == Integer.MIN_VALUE;
   * 
   * @param howFarBack
   *        number of lines (-1 for next line)
   * @return one or more lines of command history
   */
  @Override
  public String getSetHistory(int howFarBack) {
    return commandHistory.getSetHistory(howFarBack);
  }

  public String historyFind(String cmd, int dir) {
    return commandHistory.find(cmd, dir);
  }

  public void setHistory(String fileName) {
    commandHistory.getSetHistory(Integer.MIN_VALUE);
    commandHistory.addCommand(getFileAsString4(fileName, -1, false, false,
        true, null));
  }

  // ///////////////////////////////////////////////////////////////
  // image and file export
  // ///////////////////////////////////////////////////////////////

  public OC getOutputChannel(String localName, String[] fullPath) {
    return getOutputManager().getOutputChannel(localName, fullPath);
  }

  @Override
  public void writeTextFile(String fileName, String data) {
    Map<String, Object> params = new Hashtable<String, Object>();
    params.put("fileName", fileName);
    params.put("type", "txt");
    params.put("text", data);
    outputToFile(params);
  }

  /**
   * 
   * @param text
   *        null here clips image; String pastes text
   * 
   * @return "OK image to clipboard: [width] * [height] or "OK text to
   *         clipboard: [length]
   */
  @Override
  public String clipImageOrPasteText(String text) {
    if (!haveAccess(ACCESS.ALL))
      return "no";
    return getOutputManager().clipImageOrPasteText(text);
  }

  @Override
  public String getClipboardText() {
    if (!haveAccess(ACCESS.ALL))
      return "no";
    try {
      return getOutputManager().getClipboardText();
    } catch (Error er) {
      // unsigned applet will not have this interface
      return GT._("clipboard is not accessible -- use signed applet");
    }
  }

  public boolean creatingImage;

  /**
   * 
   * from eval write command only includes option to write set of files
   * 
   * @param params
   * @return message starting with "OK" or an error message
   */
  public String processWriteOrCapture(Map<String, Object> params) {
    return getOutputManager().processWriteOrCapture(params);
  }

  public Object createZip(String fileName, String type, Map<String, Object> params) {
    Object data = params.get("data");
    params.put("fileName", fileName);
    params.put("type", type);
    params.put("text", getStateInfo());
    if (data instanceof String[])
      params.put("scripts", data);
    else if (data instanceof Lst)
      params.put("imageData", data);
    return getOutputManager().outputToFile(params);
  }

  @Override
  public String outputToFile(Map<String, Object> params) {
    return getOutputManager().outputToFile(params);
  }

  private OutputManager outputManager;

  private OutputManager getOutputManager() {
    if (outputManager != null)
      return outputManager;
    return (outputManager = (OutputManager) Interface.getInterface(
        "org.jmol.viewer.OutputManager" + (isJS ? "JS" : "Awt"), this, "file"))
        .setViewer(this, privateKey);
  }

  private void setSyncTarget(int mode, boolean TF) {
    switch (mode) {
    case 0:
      sm.syncingMouse = TF;
      break;
    case 1:
      sm.syncingScripts = TF;
      break;
    case 2:
      sm.syncSend(TF ? SYNC_GRAPHICS_MESSAGE : SYNC_NO_GRAPHICS_MESSAGE, "*", 0);
      if (Float.isNaN(tm.stereoDegrees))
        setFloatProperty("stereoDegrees",
            TransformManager.DEFAULT_STEREO_DEGREES);
      if (TF) {
        setBooleanProperty("_syncMouse", false);
        setBooleanProperty("_syncScript", false);
      }
      return;
    }
    // if turning both off, sync the orientation now
    if (!sm.syncingScripts && !sm.syncingMouse)
      setSync();
  }

  public final static String SYNC_GRAPHICS_MESSAGE = "GET_GRAPHICS";
  public final static String SYNC_NO_GRAPHICS_MESSAGE = "SET_GRAPHICS_OFF";

  @Override
  public void syncScript(String script, String applet, int port) {
    getStateCreator().syncScript(script, applet, port);
  }

  @Override
  public int getModelIndexFromId(String id) {
    // from JSpecView peak pick and model "ID"
    return ms.getModelIndexFromId(id);
  }

  public void setSyncDriver(int mode) {
    sm.setSyncDriver(mode);
  }

  public float[] getPartialCharges() {
    return ms.getPartialCharges();
  }

  public void setProteinType(STR type, BS bs) {
    ms.setProteinType(bs == null ? bsA() : bs, type);
  }

  /*
   * void debugStack(String msg) { //what's the right way to do this? try {
   * Logger.error(msg); String t = null; t.substring(3); } catch (Exception e) {
   * System.out.println(e.toString()); } }
   */

  public int getVanderwaalsMar(int i) {
    return (defaultVdw == VDW.USER ? userVdwMars[i] : Elements
        .getVanderwaalsMar(i, defaultVdw));
  }

  public int getVanderwaalsMarType(int atomicAndIsotopeNumber, VDW type) {
    if (type == null)
      type = defaultVdw;
    else
      switch (type) {
      case AUTO:
      case AUTO_BABEL:
      case AUTO_JMOL:
      case AUTO_RASMOL:
        if (defaultVdw != VDW.AUTO)
          type = defaultVdw;
        break;
      default:
        break;
      }
    if (type == VDW.USER && bsUserVdws == null)
      type = VDW.JMOL;
    return (type == VDW.USER ? userVdwMars[atomicAndIsotopeNumber & 127]
        : Elements.getVanderwaalsMar(atomicAndIsotopeNumber, type));
  }

  void setVdwStr(String name) {
    VDW type = VDW.getVdwType(name);
    if (type == null)
      type = VDW.AUTO;
    // only allowed types here are VDW_JMOL, VDW_BABEL, VDW_RASMOL, VDW_USER, VDW_AUTO
    switch (type) {
    case JMOL:
    case BABEL:
    case RASMOL:
    case AUTO:
    case USER:
      break;
    default:
      type = VDW.JMOL;
    }
    if (type != defaultVdw && type == VDW.USER && bsUserVdws == null)
      setUserVdw(defaultVdw);
    defaultVdw = type;
    g.setO("defaultVDW", type.getVdwLabel());
  }

  BS bsUserVdws;
  float[] userVdws;
  int[] userVdwMars;

  void setUserVdw(VDW mode) {
    userVdwMars = new int[Elements.elementNumberMax];
    userVdws = new float[Elements.elementNumberMax];
    bsUserVdws = new BS();
    if (mode == VDW.USER)
      mode = VDW.JMOL;
    for (int i = 1; i < Elements.elementNumberMax; i++) {
      userVdwMars[i] = Elements.getVanderwaalsMar(i, mode);
      userVdws[i] = userVdwMars[i] / 1000f;
    }
  }

  public String getDefaultVdwNameOrData(int mode, VDW type, BS bs) {
    // called by getDataState and via Viewer: Eval.calculate,
    // Eval.show, StateManager.getLoadState, Viewer.setDefaultVdw
    switch (mode) {
    case Integer.MIN_VALUE:
      // iMode Integer.MIN_VALUE -- just the name
      return defaultVdw.getVdwLabel();
    case Integer.MAX_VALUE:
      // iMode = Integer.MAX_VALUE -- user, only selected
      if ((bs = bsUserVdws) == null)
        return "";
      type = VDW.USER;
      break;
    }
    if (type == null || type == VDW.AUTO)
      type = defaultVdw;
    if (type == VDW.USER && bsUserVdws == null)
      setUserVdw(defaultVdw);

    return getDataManager().getDefaultVdwNameOrData(type, bs);
  }

  public int deleteAtoms(BS bsAtoms, boolean fullModels) {
    int atomIndex = (bsAtoms == null ? -1 : bsAtoms.nextSetBit(0));
    if (atomIndex < 0)
      return 0;
    clearModelDependentObjects();
    if (!fullModels) {
      sm.modifySend(atomIndex, ms.at[atomIndex].mi, 4, "deleting atom "
          + ms.at[atomIndex].getAtomName());
      ms.deleteAtoms(bsAtoms);
      int n = slm.deleteAtoms(bsAtoms);
      setTainted(true);
      sm.modifySend(atomIndex, ms.at[atomIndex].mi, -4, "OK");
      return n;
    }
    return deleteModels(ms.at[atomIndex].mi, bsAtoms);
  }

  /**
   * called by ZAP {atomExpression} when atoms are present or the command is
   * specific for a model, such as ZAP 2.1
   * 
   * @param modelIndex
   * @param bsAtoms
   * @return number of atoms deleted
   */
  public int deleteModels(int modelIndex, BS bsAtoms) {
    clearModelDependentObjects();
    // fileManager.addLoadScript("zap " + Escape.escape(bs));
    sm.modifySend(-1, modelIndex, 5, "deleting model "
        + getModelNumberDotted(modelIndex));
    setCurrentModelIndexClear(0, false);
    am.setAnimationOn(false);
    BS bsD0 = BSUtil.copy(slm.bsDeleted);
    BS bsModels = (bsAtoms == null ? BSUtil.newAndSetBit(modelIndex) : ms
        .getModelBS(bsAtoms, false));
    BS bsDeleted = ms.deleteModels(bsModels);
    slm.processDeletedModelAtoms(bsDeleted);
    if (eval != null)
      eval.deleteAtomsInVariables(bsDeleted);
    setAnimationRange(0, 0);
    clearRepaintManager(-1);
    am.clear();
    am.initializePointers(1);
    setCurrentModelIndexClear(ms.mc > 1 ? -1 : 0, ms.mc > 1);
    hoverAtomIndex = -1;
    setFileLoadStatus(FIL.DELETED, null, null, null, null, null);
    refreshMeasures(true);
    if (bsD0 != null)
      bsDeleted.andNot(bsD0);
    sm.modifySend(-1, modelIndex, -5, "OK");
    return BSUtil.cardinalityOf(bsDeleted);
  }

  public void deleteBonds(BS bsDeleted) {
    int modelIndex = ms.bo[bsDeleted.nextSetBit(0)].atom1.mi;
    sm.modifySend(-1, modelIndex, 2, "delete bonds " + Escape.eBond(bsDeleted));
    ms.deleteBonds(bsDeleted, false);
    sm.modifySend(-1, modelIndex, -2, "OK");
  }

  public void deleteModelAtoms(int modelIndex, int firstAtomIndex, int nAtoms,
                               BS bsModelAtoms) {
    // called from ModelCollection.deleteModel
    sm.modifySend(-1, modelIndex, 1, "delete atoms " + Escape.eBS(bsModelAtoms));
    BSUtil.deleteBits(tm.bsFrameOffsets, bsModelAtoms);
    getDataManager().deleteModelAtoms(firstAtomIndex, nAtoms, bsModelAtoms);
    sm.modifySend(-1, modelIndex, -1, "OK");
  }

  public char getQuaternionFrame() {
    return g.quaternionFrame.charAt(g.quaternionFrame.length() == 2 ? 1 : 0);
  }

  /**
   * 
   * NOTE: This method is called from within a j2sNative block in
   * awtjs2d.Platform.java as well as from FileManager.loadImage
   * 
   * @param image
   *        could be a byte array
   * @param nameOrError
   * @param echoName
   *        if this is an echo rather than the background
   * @param sc
   *        delivered in JavaScript from Platform.java
   * @return false
   */
  public boolean loadImageData(Object image, String nameOrError, String echoName,
                     ScriptContext sc) {
    if (image == null && nameOrError != null)
      scriptEcho(nameOrError);
    if (echoName == null) {
      setBackgroundImage((image == null ? null : nameOrError), image);
    } else if (echoName.startsWith("\1")) {
      sm.showImage(echoName, image);
    } else {
      shm.loadShape(JC.SHAPE_ECHO);
      setShapeProperty(JC.SHAPE_ECHO, "text", nameOrError);
      if (image != null)
        setShapeProperty(JC.SHAPE_ECHO, "image", image);
    }
    if (isJS && sc != null) {
      sc.mustResumeEval = true;
      eval.resumeEval(sc);
    }
    return false;
  }

  public String cd(String dir) {
    if (dir == null) {
      dir = ".";
    } else if (dir.length() == 0) {
      setStringProperty("defaultDirectory", "");
      dir = ".";
    }
    dir = fm.getDefaultDirectory(dir
        + (dir.equals("=") ? "" : dir.endsWith("/") ? "X.spt" : "/X.spt"));
    if (dir.length() > 0)
      setStringProperty("defaultDirectory", dir);
    String path = fm.getFilePath(dir + "/", true, false);
    if (path.startsWith("file:/"))
      FileManager.setLocalPath(this, dir, false);
    return dir;
  }

  // //// Error handling

  public String setErrorMessage(String errMsg, String errMsgUntranslated) {
    errorMessageUntranslated = errMsgUntranslated;
    if (errMsg != null)
      eval.stopScriptThreads();
    return (errorMessage = errMsg);
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public String getErrorMessageUn() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private int currentShapeID = -1;
  private String currentShapeState;

  public void setShapeErrorState(int shapeID, String state) {
    currentShapeID = shapeID;
    currentShapeState = state;
  }

  public String getShapeErrorState() {
    if (currentShapeID < 0)
      return "";
    shm.releaseShape(currentShapeID);
    clearRepaintManager(currentShapeID);
    return JC.getShapeClassName(currentShapeID, false) + " "
        + currentShapeState;
  }

  public void handleError(Error er, boolean doClear) {
    // almost certainly out of memory; could be missing Jar file
    try {
      if (doClear)
        zapMsg("" + er); // get some breathing room
      undoClear();
      if (Logger.getLogLevel() == 0)
        Logger.setLogLevel(Logger.LEVEL_INFO);
      setCursor(GenericPlatform.CURSOR_DEFAULT);
      setBooleanProperty("refreshing", true);
      fm.setPathForAllFiles("");
      Logger.error("vwr handling error condition: " + er + "  ");
      if (!isJS)
        er.printStackTrace();
      notifyError("Error", "doClear=" + doClear + "; " + er, "" + er);
    } catch (Throwable e1) {
      try {
        Logger.error("Could not notify error " + er + ": due to " + e1);
      } catch (Throwable er2) {
        // tough luck.
      }
    }
  }

  // / User-defined functions

  final static Map<String, JmolScriptFunction> staticFunctions = new Hashtable<String, JmolScriptFunction>();
  Map<String, JmolScriptFunction> localFunctions;

  public Map<String, JmolScriptFunction> getFunctions(boolean isStatic) {
    return (isStatic ? staticFunctions : localFunctions);
  }

  public void removeFunction(String name) {
    name = name.toLowerCase();
    JmolScriptFunction function = getFunction(name);
    if (function == null)
      return;
    staticFunctions.remove(name);
    localFunctions.remove(name);
  }

  public JmolScriptFunction getFunction(String name) {
    if (name == null)
      return null;
    JmolScriptFunction function = (isStaticFunction(name) ? staticFunctions
        : localFunctions).get(name);
    return (function == null || function.geTokens() == null ? null : function);
  }

  private static boolean isStaticFunction(String name) {
    return name.startsWith("static_");
  }

  public boolean isFunction(String name) {
    return (isStaticFunction(name) ? staticFunctions : localFunctions)
        .containsKey(name);
  }

  public void clearFunctions() {
    staticFunctions.clear();
    localFunctions.clear();
  }

  public void addFunction(JmolScriptFunction function) {
    String name = function.getName();
    (isStaticFunction(name) ? staticFunctions : localFunctions).put(name,
        function);
  }

  public String getFunctionCalls(String selectedFunction) {
    return getStateCreator().getFunctionCalls(selectedFunction);
  }

  /**
   * Simple method to ensure that the image creator (which writes files) was in
   * fact opened by this vwr and not by some manipulation of the applet. When
   * the image creator is used it requires both a vwr object and that vwr's
   * private key. But the private key is private, so it is not possible to
   * create a useable image creator without working through a vwr's own methods.
   * Bob Hanson, 9/20/2009
   * 
   * @param privateKey
   * @return true if privateKey matches
   * 
   */

  public boolean checkPrivateKey(double privateKey) {
    return privateKey == this.privateKey;
  }

  public void bindAction(String desc, String name) {
    if (haveDisplay)
      acm.bind(desc, name);
  }

  public void unBindAction(String desc, String name) {
    if (haveDisplay)
      acm.unbindAction(desc, name);
  }

  public Lst<Object> getPlaneIntersection(int type, P4 plane, float scale,
                                          int flags) {
    return ms.getPlaneIntersection(type, plane, scale, flags,
        type == T.unitcell ? getCurrentUnitCell() : null);
  }

  public int calculateStruts(BS bs1, BS bs2) {
    return ms.calculateStruts(bs1 == null ? bsA() : bs1, bs2 == null ? bsA()
        : bs2);
  }

  /**
   * This flag if set FALSE:
   * 
   * 1) turns UNDO off for the application 2) turns history off 3) prevents
   * saving of inlinedata for later LOAD "" commands 4) turns off the saving of
   * changed atom properties 5) does not guarantee accurate state representation
   * 6) disallows generation of the state
   * 
   * It is useful in situations such as web sites where memory is an issue and
   * there is no need for such.
   * 
   * 
   * @return TRUE or FALSE
   */
  public boolean getPreserveState() {
    return (g.preserveState && scm != null);
  }

  boolean isKiosk;

  boolean isKiosk() {
    return isKiosk;
  }

  public boolean hasFocus() {
    return (haveDisplay && (isKiosk || apiPlatform.hasFocus(display)));
  }

  public void setFocus() {
    if (haveDisplay && !apiPlatform.hasFocus(display))
      apiPlatform.requestFocusInWindow(display);
  }

  void stopMinimization() {
    if (minimizer != null) {
      minimizer.setProperty("stop", null);
    }
  }

  void clearMinimization() {
    if (minimizer != null)
      minimizer.setProperty("clear", null);
  }

  public String getMinimizationInfo() {
    return (minimizer == null ? "" : (String) minimizer.getProperty("log", 0));
  }

  private void checkMinimization() {
    refreshMeasures(true);
    if (!g.monitorEnergy)
      return;
    try {
      minimize(null, 0, 0, getAllAtoms(), null, 0, false, false, true, false);
    } catch (Exception e) {
      // TODO
    }
    echoMessage(getP("_minimizationForceField") + " Energy = "
        + getP("_minimizationEnergy"));
  }

  /**
   * 
   * @param eval
   * @param steps
   *        Integer.MAX_VALUE --> use defaults
   * @param crit
   *        -1 --> use defaults
   * @param bsSelected
   * @param bsFixed
   * @param rangeFixed
   * @param addHydrogen
   * @param isOnly
   * @param isSilent
   * @param isLoad2D
   * @throws Exception
   */
  public void minimize(JmolScriptEvaluator eval, int steps, float crit,
                       BS bsSelected, BS bsFixed, float rangeFixed,
                       boolean addHydrogen, boolean isOnly, boolean isSilent,
                       boolean isLoad2D) throws Exception {

    // We only work on atoms that are in frame

    String ff = g.forceField;
    BS bsInFrame = getFrameAtoms();

    if (bsSelected == null)
      bsSelected = getModelUndeletedAtomsBitSet(getVisibleFramesBitSet()
          .length() - 1);
    else
      bsSelected.and(bsInFrame);

    if (rangeFixed <= 0)
      rangeFixed = JC.MINIMIZE_FIXED_RANGE;

    // we allow for a set of atoms to be fixed, 
    // but that is only used by default

    BS bsMotionFixed = BSUtil.copy(bsFixed == null ? slm.getMotionFixedAtoms()
        : bsFixed);
    boolean haveFixed = (bsMotionFixed.cardinality() > 0);
    if (haveFixed)
      bsSelected.andNot(bsMotionFixed);

    // We always fix any atoms that
    // are in the visible frame set and are within 5 angstroms
    // and are not already selected

    BS bsNearby = (isOnly ? new BS() : ms.getAtomsWithinRadius(rangeFixed,
        bsSelected, true, null));
    bsNearby.andNot(bsSelected);
    if (haveFixed) {
      bsMotionFixed.and(bsNearby);
    } else {
      bsMotionFixed = bsNearby;
    }
    bsMotionFixed.and(bsInFrame);

    if (addHydrogen)
      bsSelected.or(addHydrogens(bsSelected, isLoad2D, isSilent));

    if (bsSelected.cardinality() > JC.MINIMIZATION_ATOM_MAX) {
      Logger.error("Too many atoms for minimization (>"
          + JC.MINIMIZATION_ATOM_MAX + ")");
      return;
    }
    try {
      if (!isSilent)
        Logger.info("Minimizing " + bsSelected.cardinality() + " atoms");
      getMinimizer(true).minimize(steps, crit, bsSelected, bsMotionFixed,
          haveFixed, isSilent, ff);
    } catch (JmolAsyncException e) {
      if (eval != null)
        eval.loadFileResourceAsync(e.getFileName());
    } catch (Exception e) {
      Logger.error("Minimization error: " + e.toString());
      if (!isJS)
        e.printStackTrace();
    }
  }

  public void setMotionFixedAtoms(BS bs) {
    slm.setMotionFixedAtoms(bs);
  }

  public BS getMotionFixedAtoms() {
    return slm.getMotionFixedAtoms();
  }

  //  void rotateArcBall(int x, int y, float factor) {
  //    tm.rotateArcBall(x, y, factor);
  //    refresh(2, sm.syncingMouse ? "Mouse: rotateArcBall " + x + " "
  //        + y + " " + factor : "");
  //  }

  void getAtomicPropertyState(SB commands, byte type, BS bs, String name,
                              float[] data) {
    getStateCreator().getAtomicPropertyStateBuffer(commands, type, bs, name,
        data);
  }

  public P3[][] getCenterAndPoints(Lst<Object[]> atomSets, boolean addCenter) {
    return ms.getCenterAndPoints(atomSets, addCenter);
  }

  public String writeFileData(String fileName, String type, int modelIndex,
                              Object[] parameters) {
    return getOutputManager().writeFileData(fileName, type, modelIndex,
        parameters);
  }

  public String getPdbData(int modelIndex, String type, BS bsAtoms,
                           Object[] parameters, OC oc, boolean getStructure) {
    // plot command
    return getPropertyManager().getPdbData(modelIndex, type,
        bsAtoms == null ? bsA() : bsAtoms, parameters, oc, getStructure);
  }

  public BS getGroupsWithin(int nResidues, BS bs) {
    return ms.getGroupsWithin(nResidues, bs);
  }

  // parallel processing

  private Object executor;
  public static int nProcessors = 1;
  static {
    /**
     * @j2sIgnore
     * 
     */
    {
      nProcessors = Runtime.getRuntime().availableProcessors();
    }

  }

  public Object getExecutor() {
    // a Java 1.5 function
    if (executor != null || nProcessors < 2)
      return executor; // note -- a Java 1.5 function
    try {
      executor = ((JmolParallelProcessor) Interface.getInterface(
          "org.jmol.script.ScriptParallelProcessor", null, null)).getExecutor();
    } catch (Exception e) {
      executor = null;
    } catch (Error er) {
      executor = null;
    }
    if (executor == null)
      Logger.error("parallel processing is not available");
    return executor;
  }

  public boolean displayLoadErrors = true;

  public void setShapeSize(int shapeID, int mad, BS bsSelected) {
    // might be atoms or bonds
    if (bsSelected == null)
      bsSelected = bsA();
    shm.setShapeSizeBs(shapeID, mad, null, bsSelected);
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value) {
    // Eval, BondCollection, StateManager, local
    if (shapeID < 0)
      return; // not applicable
    shm.setShapePropertyBs(shapeID, propertyName, value, null);
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return shm
        .getShapePropertyIndex(shapeType, propertyName, Integer.MIN_VALUE);
  }

  private int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  public void setModelVisibility() {
    if (shm == null) // necessary for file chooser
      return;
    shm.setModelVisibility();
  }

  public void resetShapes(boolean andCreateNew) {
    shm.resetShapes();
    if (andCreateNew) {
      shm.loadDefaultShapes(ms);
      clearRepaintManager(-1);
    }
  }

  private boolean isParallel;

  public boolean setParallel(boolean TF) {
    return (isParallel = g.multiProcessor && TF);
  }

  public boolean isParallel() {
    return g.multiProcessor && isParallel;
  }

  private void setAtomPickingOption(String option) {
    if (haveDisplay)
      acm.setAtomPickingOption(option);
  }

  private void setBondPickingOption(String option) {
    if (haveDisplay)
      acm.setBondPickingOption(option);
  }

  void undoClear() {
    actionStates.clear();
    actionStatesRedo.clear();
  }

  /**
   * 
   * @param action
   *        Token.undo or Token.redo
   * @param n
   *        number of steps to go back/forward; 0 for all; -1 for clear; -2 for
   *        clear BOTH
   * 
   */
  public void undoMoveAction(int action, int n) {
    getStateCreator().undoMoveAction(action, n);
  }

  void undoMoveActionClear(int taintedAtom, int type, boolean clearRedo) {
    // called by actionManager
    if (!g.preserveState)
      return;
    getStateCreator().undoMoveActionClear(taintedAtom, type, clearRedo);
  }

  protected void moveAtomWithHydrogens(int atomIndex, int deltaX, int deltaY,
                                       int deltaZ, BS bsAtoms) {
    // called by actionManager
    stopMinimization();
    if (bsAtoms == null) {
      Atom atom = ms.at[atomIndex];
      bsAtoms = BSUtil.newAndSetBit(atomIndex);
      Bond[] bonds = atom.bonds;
      if (bonds != null)
        for (int i = 0; i < bonds.length; i++) {
          Atom atom2 = bonds[i].getOtherAtom(atom);
          if (atom2.getElementNumber() == 1)
            bsAtoms.set(atom2.i);
        }
    }
    moveSelected(deltaX, deltaY, deltaZ, Integer.MIN_VALUE, Integer.MIN_VALUE,
        bsAtoms, true, true);
  }

  public boolean isModelPDB(int i) {
    return ms.am[i].isBioModel;
  }

  @Override
  public void deleteMeasurement(int i) {
    setShapeProperty(JC.SHAPE_MEASURES, "delete", Integer.valueOf(i));
  }

  public String getDataBaseName(BS bs) throws Exception {
    return ms.getModelDataBaseName(bs == null ? bsA() : bs);
  }

  @Override
  public String getSmiles(BS bs) throws Exception {
    return getSmilesOpt(bs, -1, -1, Logger.debugging ? JC.SMILES_ATOM_COMMENT : 0);
  }

  public String getBioSmiles(BS bs) throws Exception {
    return getSmilesOpt(bs, -1, -1,  
          JC.SMILES_BIO_ALLOW_UNMATCHED_RINGS
        | JC.SMILES_BIO_COV_CROSSLINK
        | JC.SMILES_BIO_COMMENT
        | (Logger.debugging ? JC.SMILES_ATOM_COMMENT : 0));
  }

  /**
   * returns the SMILES string for a sequence or atom set does not include
   * attached protons on groups
   * 
   * @param bsSelected
   *        selected atom set or null for current or specified range
   * @param index1
   *        when bsSeleced == null, first atomIndex or -1 for current
   * @param index2
   *        when bsSeleced == null, end atomIndex or -1 for current
   * @param flags see JC.SMILES_xxxx
   * @return SMILES string
   * @throws Exception
   */
  public String getSmilesOpt(BS bsSelected, int index1, int index2, int flags)
      throws Exception {
    String bioComment = ((flags & JC.SMILES_BIO_COMMENT) == JC.SMILES_BIO_COMMENT ? getJmolVersion() + " "
        + getModelName(am.cmi) : null);
    Atom[] atoms = ms.at;
    if (bsSelected == null) {
      if (index1 < 0 || index2 < 0) {
        bsSelected = bsA();
      } else {
        if ((flags & JC.SMILES_BIO) == JC.SMILES_BIO) {
          if (index1 > index2) {
            int i = index1;
            index1 = index2;
            index2 = i;
          }
          index1 = atoms[index1].group.firstAtomIndex;
          index2 = atoms[index2].group.lastAtomIndex;
        }
        bsSelected = new BS();
        bsSelected.setBits(index1, index2 + 1);
      }
    }
    return getSmilesMatcher().getSmiles(atoms, ms.ac, bsSelected, bioComment, flags);
  }

  public void alert(String msg) {
    prompt(msg, "OK", null, true);
  }

  public String prompt(String label, String data, String[] list,
                       boolean asButtons) {
    return (isKiosk ? "null" : apiPlatform.prompt(label, data, list, asButtons));
  }

  /**
   * Ask for new file name when opening a file
   * 
   * @param type
   * @param fileName
   * @param params
   * @return new file name
   */
  public String dialogAsk(String type, String fileName, Map<String, Object> params) {
    /**
     * @j2sNative
     * 
     *            return prompt(type, fileName);
     * 
     */
    {
      // may have #NOCARTOONS#; and/or "#APPEND#; prepended
      return (isKiosk || !haveAccess(ACCESS.ALL) ? null : sm.dialogAsk(type,
          fileName, params));
    }
  }

  public int stateScriptVersionInt;

  private JmolRendererInterface jsExporter3D;

  public JmolRendererInterface initializeExporter(Map<String, Object> params) {
    boolean isJS = params.get("type").equals("JS");
    if (isJS) {
      if (jsExporter3D != null) {
        jsExporter3D.initializeOutput(this, privateKey, params);
        return jsExporter3D;
      }
    } else {
      String fileName = (String) params.get("fileName");
      String[] fullPath = (String[]) params.get("fullPath");
      OC out = getOutputChannel(fileName, fullPath);
      if (out == null)
        return null;
      params.put("outputChannel", out);
    }
    JmolRendererInterface export3D = (JmolRendererInterface) Interface
        .getOption("export.Export3D", this, "export");
    if (export3D == null)
      return null;
    Object exporter = export3D.initializeExporter(this, privateKey, gdata,
        params);
    if (isJS && exporter != null)
      jsExporter3D = export3D;
    return (exporter == null ? null : export3D);
  }

  public boolean getMouseEnabled() {
    return refreshing && !creatingImage;
  }

  @Override
  public void calcAtomsMinMax(BS bs, BoxInfo boxInfo) {
    ms.calcAtomsMinMax(bs, boxInfo);
  }

  /**
   * used in autocompletion in console using TAB
   * 
   * @param map
   * @param c
   */
  @SuppressWarnings("unchecked")
  public void getObjectMap(Map<String, ?> map, char c) {
    switch (c) {
    case '{':
      if (getScriptManager() != null) {
        Map<String, Object> m = (Map<String, Object>) map;
        if (definedAtomSets != null)
          m.putAll(definedAtomSets);
        T.getTokensType(m, T.predefinedset);
      }
      return;
    case '$':
    case '0':
      shm.getObjectMap(map, c == '$');
      return;
    }
  }

  public void setPicked(int atomIndex) {
    SV pickedSet = null;
    SV pickedList = null;
    if (atomIndex >= 0) {
      g.setI("_atompicked", atomIndex);
      pickedSet = (SV) g.getParam("picked", true);
      pickedList = (SV) g.getParam("pickedList", true);
    }
    if (pickedSet == null || pickedSet.tok != T.bitset) {
      pickedSet = SV.newV(T.bitset, new BS());
      pickedList = SV.getVariableList(new Lst<Object>());
      g.setUserVariable("picked", pickedSet);
      g.setUserVariable("pickedList", pickedList);
    }
    if (atomIndex < 0)
      return;
    SV.getBitSet(pickedSet, false).set(atomIndex);
    SV p = pickedList.pushPop(null, null);
    // don't allow double click
    if (p.tok == T.bitset)
      pickedList.pushPop(p, null);
    if (p.tok != T.bitset || !((BS) p.value).get(atomIndex))
      pickedList.pushPop(SV.newV(T.bitset, BSUtil.newAndSetBit(atomIndex)),
          null);
  }

  @Override
  public String runScript(String script) {
    // from isosurface reading JVXL file with slab
    SB outputBuffer = new SB();
    try {
      if (getScriptManager() == null)
        return null;
      eval.runScriptBuffer(script, outputBuffer, false);
    } catch (Exception e) {
      return eval.getErrorMessage();
    }
    return outputBuffer.toString();
  }

  public void setFrameDelayMs(long millis) {
    ms.setFrameDelayMs(millis, getVisibleFramesBitSet());
  }

  public BS getBaseModelBitSet() {
    return ms.getModelAtomBitSetIncludingDeleted(getJDXBaseModelIndex(am.cmi),
        true);
  }

  public Map<String, Object> timeouts;

  public void clearTimeouts() {
    if (timeouts != null)
      TimeoutThread.clear(timeouts);
  }

  public void setTimeout(String name, int mSec, String script) {
    if (!haveDisplay || headless || autoExit)
      return;
    if (name == null) {
      clearTimeouts();
      return;
    }
    if (timeouts == null) {
      timeouts = new Hashtable<String, Object>();
    }
    TimeoutThread.setTimeout(this, timeouts, name, mSec, script);
  }

  public void triggerTimeout(String name) {
    if (!haveDisplay || timeouts == null)
      return;
    TimeoutThread.trigger(timeouts, name);
  }

  public void clearTimeout(String name) {
    setTimeout(name, 0, null);
  }

  public String showTimeout(String name) {
    return (haveDisplay ? TimeoutThread.showTimeout(timeouts, name) : "");
  }

  public void calculatePartialCharges(BS bsSelected) throws JmolAsyncException {
    if (bsSelected == null || bsSelected.isEmpty())
      bsSelected = getModelUndeletedAtomsBitSetBs(getVisibleFramesBitSet());
    int pt = bsSelected.nextSetBit(0);
    if (pt < 0)
      return;
    // this forces an array if it does not exist 
    setAtomProperty(BSUtil.newAndSetBit(pt), T.partialcharge, 0, 1f, null, null, null);
    getMinimizer(true).calculatePartialCharges(ms.bo, ms.bondCount, ms.at,
        bsSelected);
  }

  public void setCurrentModelID(String id) {
    int modelIndex = am.cmi;
    if (modelIndex >= 0)
      ms.setInfo(modelIndex, "modelID", id);
  }

  public void cacheClear() {
    // script: reset cache
    fm.cacheClear();
    ligandModelSet = null;
    ligandModels = null;
    ms.clearCache();
  }

  /**
   * JSInterface -- allows saving files in memory for later retrieval
   * 
   * @param key
   * @param data
   * 
   */

  public void cachePut(String key, Object data) {
    // PyMOL reader and isosurface
    // HTML5/JavaScript load ?  and  script ? 
    Logger.info("Viewer cachePut " + key);
    fm.cachePut(key, data);
  }

  public int cacheFileByName(String fileName, boolean isAdd) {
    // cache command in script
    if (fileName == null) {
      cacheClear();
      return -1;
    }
    return fm.cacheFileByNameAdd(fileName, isAdd);
  }

  public void clearThreads() {
    if (eval != null)
      eval.stopScriptThreads();
    stopMinimization();
    tm.clearThreads();
    setAnimationOn(false);
  }

  public ScriptContext getEvalContextAndHoldQueue(JmolScriptEvaluator eval) {
    if (eval == null || !isJS && !testAsync)
      return null;
    eval.pushContextDown("getEvalContextAndHoldQueue");
    ScriptContext sc = eval.getThisContext();
    sc.setMustResume();
    sc.isJSThread = true;
    queueOnHold = true;
    return sc;
  }

  @Override
  public int[] resizeInnerPanel(int width, int height) {
    if (!autoExit && haveDisplay)
      return sm.resizeInnerPanel(width, height);
    setScreenDimension(width, height);
    return new int[] { dimScreen.width, dimScreen.height };
  }

  public String getFontLineShapeState(String s, String myType,
                                      TickInfo[] tickInfos) {
    return getStateCreator().getFontLineShapeState(s, myType, tickInfos);
  }

  public void getShapeSetState(AtomShape atomShape, Shape shape,
                               int monomerCount, Group[] monomers,
                               BS bsSizeDefault, Map<String, BS> temp,
                               Map<String, BS> temp2) {
    getStateCreator().getShapeSetState(atomShape, shape, monomerCount,
        monomers, bsSizeDefault, temp, temp2);

  }

  public String getMeasurementState(Measures measures, Lst<Measurement> mList,
                                    int measurementCount, Font font3d,
                                    TickInfo ti) {
    return getStateCreator().getMeasurementState(measures, mList,
        measurementCount, font3d, ti);
  }

  public String getBondState(Shape shape, BS bsOrderSet, boolean reportAll) {
    return getStateCreator().getBondState(shape, bsOrderSet, reportAll);
  }

  public String getAtomShapeSetState(Shape shape, AtomShape[] shapes) {
    return getStateCreator().getAtomShapeSetState(shape, shapes);
  }

  public String getShapeState(Shape shape) {
    return getStateCreator().getShapeState(shape);
  }

  public String getAtomShapeState(AtomShape shape) {
    return getStateCreator().getAtomShapeState(shape);
  }

  public String getDefaultPropertyParam(int propertyID) {
    return getPropertyManager().getDefaultPropertyParam(propertyID);
  }

  public int getPropertyNumber(String name) {
    return getPropertyManager().getPropertyNumber(name);
  }

  public boolean checkPropertyParameter(String name) {
    return getPropertyManager().checkPropertyParameter(name);
  }

  public Object extractProperty(Object property, Object args, int pt) {
    return getPropertyManager()
        .extractProperty(property, args, pt, null, false);
  }

  //// requiring ScriptEvaluator:

  public BS addHydrogens(BS bsAtoms, boolean is2DLoad, boolean isSilent) {
    boolean doAll = (bsAtoms == null);
    if (bsAtoms == null)
      bsAtoms = getModelUndeletedAtomsBitSet(getVisibleFramesBitSet().length() - 1);
    BS bsB = new BS();
    if (bsAtoms.isEmpty())
      return bsB;
    int modelIndex = ms.at[bsAtoms.nextSetBit(0)].mi;
    if (modelIndex != ms.mc - 1)
      return bsB;
    Lst<Atom> vConnections = new Lst<Atom>();
    P3[] pts = getAdditionalHydrogens(bsAtoms, doAll, false, vConnections);
    boolean wasAppendNew = false;
    wasAppendNew = g.appendNew;
    if (pts.length > 0) {
      clearModelDependentObjects();
      try {
        bsB = (is2DLoad ? ms.addHydrogens(vConnections, pts)
            : addHydrogensInline(bsAtoms, vConnections, pts));
      } catch (Exception e) {
        System.out.println(e.toString());
        // ignore
      }
      if (wasAppendNew)
        g.appendNew = true;
    }
    if (!isSilent)
      scriptStatus(GT.i(GT._("{0} hydrogens added"), pts.length));
    return bsB;
  }

  public BS addHydrogensInline(BS bsAtoms, Lst<Atom> vConnections, P3[] pts)
      throws Exception {
    if (getScriptManager() == null)
      return null;
    return scm.addHydrogensInline(bsAtoms, vConnections, pts);
  }

  @Override
  public float evalFunctionFloat(Object func, Object params, float[] values) {
    return (getScriptManager() == null ? 0 : eval.evalFunctionFloat(func,
        params, values));
  }

  public boolean evalParallel(ScriptContext context, ShapeManager shapeManager) {
    displayLoadErrors = false;
    boolean isOK = getScriptManager() != null
        && eval.evalParallel(context, (shapeManager == null ? this.shm
            : shapeManager));
    displayLoadErrors = true;
    return isOK;
  }

  /**
   * synchronized here trapped the eventQueue; see also
   * evaluateExpressionAsVariable
   * 
   */
  @Override
  public Object evaluateExpression(Object stringOrTokens) {
    return (getScriptManager() == null ? null : eval.evaluateExpression(
        stringOrTokens, false, false));
  }

  public SV evaluateExpressionAsVariable(Object stringOrTokens) {
    return (getScriptManager() == null ? null : (SV) eval.evaluateExpression(
        stringOrTokens, true, false));
  }

  public BS getAtomBitSet(Object atomExpression) {
    // SMARTS searching
    // getLigandInfo
    // used in interaction with JSpecView
    // used for set picking SELECT

    if (atomExpression instanceof BS)
      return slm.excludeAtoms((BS) atomExpression, false);
    getScriptManager();
    return getAtomBitSetEval(eval, atomExpression);
  }

  public ScriptContext getScriptContext(String why) {
    return (getScriptManager() == null ? null : eval.getScriptContext(why));
  }

  public String getAtomDefs(Map<String, Object> names) {
    SB sb = new SB();
    for (Map.Entry<String, ?> e : names.entrySet()) {
      if (e.getValue() instanceof BS)
        sb.append("{" + e.getKey() + "} <" + ((BS) e.getValue()).cardinality()
            + " atoms>\n");
    }
    return sb.append("\n").toString();
  }

  public void setCGO(Lst<Object> info) {
    shm.loadShape(JC.SHAPE_CGO);
    shm.setShapePropertyBs(JC.SHAPE_CGO, "setCGO", info, null);
  }

  public void setModelSet(ModelSet modelSet) {
    this.ms = mm.modelSet = modelSet;
  }

  public String setObjectProp(String id, int tokCommand) {
    // for PyMOL session scene setting
    getScriptManager();
    if (id == null)
      id = "*";
    return (eval == null ? null : eval.setObjectPropSafe(id, tokCommand));
  }

  public void setDihedrals(float[] dihedralList, BS[] bsBranches, float rate) {
    if (bsBranches == null)
      bsBranches = ms.getBsBranches(dihedralList);
    ms.setDihedrals(dihedralList, bsBranches, rate);
  }

  private boolean chainCaseSpecified;

  /**
   * Create a unique integer for any chain string. Note that if there are any
   * chains that are more than a single character, chainCaseSensitive is
   * automatically set TRUE
   * 
   * 
   * @param id
   *        < 256 is just the character of a single-character upper-case chain
   *        id, upper or lower case query;
   * 
   *        >= 256 < 300 is lower case found in structure
   * 
   * @param isAssign
   *        from a file reader, not a select query
   * 
   * @return i
   */
  public int getChainID(String id, boolean isAssign) {
    // if select :a and there IS chain "a" in a structure,
    // then we return that id, and all is good. Chain selectivity
    // is inforced, and we will find it.
    Integer iboxed = (Integer) chainMap.get(id);
    if (iboxed != null)
      return iboxed.intValue();
    int i = id.charAt(0);
    if (id.length() > 1) {
      i = 300 + chainList.size();
    } else if (isAssign && 97 <= i && i <= 122) { // lower case
      i += 159; // starts at 256
    }
    if (i >= 256) {
      //this will force chainCaseSensitive when it is necessary
      chainCaseSpecified |= isAssign;
      chainList.addLast(id);
    }
    // if select :a and there is NO chain "a" in the structure,
    // there still might be an "A" and we cannot check for 
    // chain case sensitivity yet, as we are parsing the script,
    // not processing it. So we just store this one as 97-122.

    iboxed = Integer.valueOf(i);
    chainMap.put(iboxed, id);
    chainMap.put(id, iboxed);
    return i;
  }

  public String getChainIDStr(int id) {
    return (String) chainMap.get(Integer.valueOf(id));
  }

  public Boolean getScriptQueueInfo() {
    return (scm != null && scm.isQueueProcessing() ? Boolean.TRUE
        : Boolean.FALSE);
  }

  JmolNMRInterface nmrCalculation;

  public JmolNMRInterface getNMRCalculation() {
    return (nmrCalculation == null ? (nmrCalculation = (JmolNMRInterface) Interface
        .getOption("quantum.NMRCalculation", this, "script")).setViewer(this)
        : nmrCalculation);
  }

  public String getDistanceUnits(String s) {
    if (s == null)
      s = getDefaultMeasurementLabel(2);
    int pt = s.indexOf("//");
    return (pt < 0 ? g.measureDistanceUnits : s.substring(pt + 2));
  }

  public int calculateFormalCharges(BS bs) {
    return ms.fixFormalCharges(bs == null ? bsA() : bs);
  }

  public boolean cachePngFiles() {
    return (!getTestFlag(1));
  }

  public void setModulation(BS bs, boolean isOn, P3 t1, boolean isQ) {
    if (isQ)
      g.setO("_modt", Escape.eP(t1));
    ms.setModulation(bs == null ? getAllAtoms() : bs, isOn, t1, isQ);
    refreshMeasures(true);
  }

  public void checkInMotion(int state) {
    switch (state) {
    case 0: // off
      setTimeout("_SET_IN_MOTION_", 0, null);
      break;
    case 1: // start 1-second timer (by default)
      if (!inMotion)
        setTimeout("_SET_IN_MOTION_", g.hoverDelayMs * 2, "!setInMotion");
      break;
    case 2: // trigger, from a timeout thread
      setInMotion(true);
      refresh(3, "timeoutThread set in motion");
      break;
    }
  }

  /**
   * check motion for rendering during mouse movement, spin, vibration, and
   * animation
   * 
   * @param tok
   * @return TRUE if allowed
   */
  public boolean checkMotionRendering(int tok) {
    if (!getInMotion(true) && !tm.spinOn && !tm.vibrationOn && !am.animationOn)
      return true;
    if (g.wireframeRotation)
      return false;
    int n = 0;
    switch (tok) {
    case T.bonds:
    case T.atoms:
      n = 2;
      break;
    case T.ellipsoid:
      n = 3;
      break;
    case T.geosurface:
      n = 4;
      break;
    case T.cartoon:
      n = 5;
      break;
    case T.mesh:
      n = 6;
      break;
    case T.translucent:
      n = 7;
      break;
    case T.antialiasdisplay:
      n = 8;
      break;
    }
    return g.platformSpeed >= n;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to JmolFileAdapter
  // ///////////////////////////////////////////////////////////////

  public OC openExportChannel(double privateKey, String fileName,
                              boolean asWriter) throws IOException {
    return getOutputManager().openOutputChannel(privateKey, fileName, asWriter,
        false);
  }

  /*default*/String logFileName;

  @Override
  public void log(String data) {
    if (data != null)
      getOutputManager().logToFile(data);
  }

  public String getLogFileName() {
    return (logFileName == null ? "" : logFileName);
  }

  public String getCommands(Map<String, BS> htDefine, Map<String, BS> htMore,
                            String select) {
    return getStateCreator().getCommands(htDefine, htMore, select);
  }

  public boolean allowCapture() {
    return !isApplet || isSignedApplet;
  }

  public T[] compileExpr(String expr) {
    Object o = (getScriptManager() == null ? null : eval.evaluateExpression(
        expr, false, true));
    return (o instanceof T[] ? (T[]) o : new T[] { T.o(T.string, expr) });
  }

  public boolean checkSelect(Map<String, SV> h, T[] value) {
    return getScriptManager() != null && eval.checkSelect(h, value);
  }

  public String getAnnotationInfo(SV d, String match, int type) {
    return getAnnotationParser(type == T.dssr)
        .getAnnotationInfo(this, d, match, type, am.cmi);
  }

  public Lst<Float> getAtomValidation(String type, Atom atom) {
    return getAnnotationParser(false).getAtomValidation(this, type, atom);
  }

  private GenericZipTools jzt;

  public GenericZipTools getJzt() {
    return (jzt == null ? jzt = (GenericZipTools) Interface.getInterface(
        "javajs.util.ZipTools", this, "zip") : jzt);
  }

  void dragMinimizeAtom(int iAtom) {
    stopMinimization();
    BS bs = (getMotionFixedAtoms().isEmpty() ? ms.getAtoms(
        (ms.isAtomPDB(iAtom) ? T.group : T.molecule),
        BSUtil.newAndSetBit(iAtom)) : BSUtil.setAll(ms.ac));
    try {
      minimize(null, Integer.MAX_VALUE, 0, bs, null, 0, false, false, false,
          false);
    } catch (Exception e) {
      if (!async)
        return;
      /**
       * @j2sNative
       * 
       *            var me = this; setTimeout(function()
       *            {me.dragMinimizeAtom(iAtom)}, 100);
       * 
       */
      {
      }
    }
  }

  JmolBioResolver jbr;

  public JmolBioResolver getJBR() {
    return (jbr == null ? jbr = ((JmolBioResolver) Interface.getInterface(
        "org.jmol.modelsetbio.Resolver", this, "file")).setViewer(this) : jbr);
  }

  public void checkMenuUpdate() {
    if (jmolpopup != null)
      jmolpopup.jpiUpdateComputedMenus();
  }

  private JmolChimeMessenger jcm;

  public JmolChimeMessenger getChimeMessenger() {
    return (jcm == null ? jcm = ((JmolChimeMessenger) Interface.getInterface(
        "org.jmol.viewer.ChimeMessenger", this, "script")).set(this) : jcm);
  }

  public Object getAuxiliaryInfoForAtoms(Object atomExpression) {
    return ms.getAuxiliaryInfo(ms.getModelBS(getAtomBitSet(atomExpression),
        false));
  }

  private JSJSONParser jsonParser;
  
  public Map<String, Object> parseJSON(String ann) {
    if (jsonParser == null)
      jsonParser = ((JSJSONParser) Interface.getInterface("javajs.util.JSJSONParser", this, "script"));
    return jsonParser.parseMap(ann, true);
  }
}
