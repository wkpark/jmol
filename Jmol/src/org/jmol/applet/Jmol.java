/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.applet;

import org.jmol.api.*;
import org.jmol.appletwrapper.*;
import org.jmol.popup.JmolPopup;
import org.jmol.i18n.GT;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Vector;

import netscape.javascript.JSObject;

/*
 * these are *required*:
 * 
 * [param name="progressbar" value="true" /] [param name="progresscolor"
 * value="blue" /] [param name="boxmessage" value="your-favorite-message" /]
 * [param name="boxbgcolor" value="#112233" /] [param name="boxfgcolor"
 * value="#778899" /]
 * 
 * these are *optional*:
 * 
 * [param name="syncId" value="nnnnn" /]
 * 
 * determines the subset of applets *across pages* that are to be synchronized
 * (usually just a random number assigned in Jmol.js)
 * if this is fiddled with, it still should be a random number, not
 * one that is assigned statically for a given web page.
 * 
 * [param name="menuFile" value="myMenu.mnu" /]
 * 
 * optional file to load containing menu data in the format of Jmol.mnu (Jmol 11.3.15)
 * 
 * [param name="loadInline" value=" | do | it | this | way " /]
 * 
 * [param name="script" value="your-script" /]
 *  // this one flips the orientation and uses RasMol/Chime colors [param
 * name="emulate" value="chime" /]
 *  // this is *required* if you want the applet to be able to // call your
 * callbacks
 * 
 * mayscript="true" is required as an applet/object for any callback, eval, or text/textarea setting)
 *
 * To disable ALL access to JavaScript (as, for example, in a Wiki) 
 * remove the MAYSCRIPT tag or set MAYSCRIPT="false"
 * 
 * To set a maximum size for the applet if resizable:
 *
 * [param name="maximumSize" value="nnnn" /]
 * 
 * 
 * You can specify that the signed or unsign applet or application should
 * use an independent command thread (EXCEPT for scripts containing the "javascript" command)  
 * 
 * [param name="useCommandThread" value="true"]
 * 
 * You can specify a language (French in this case) using  
 * 
 * [param name="language" value="fr"]
 * 
 * You can check that it is set correctly using 
 * 
 * [param name="debug" value="true"]
 *  
 *  or
 *  
 * [param name="logLevel" value="5"]
 * 
 * and then checking the console for a message about MAYSCRIPT
 * 
 * In addition, you can turn off JUST EVAL, by setting on the web page
 * 
 * _jmol.noEval = true
 * 
 * This allows callbacks but does not allow the script constructs: 
 * 
 *  script javascript:...
 *  javascript ...
 *  x = javascript(...) 
 *  
 * However, this can be overridden by adding an evalCallback function 
 * This MUST be defined along with applet loading using a <param> tag
 * Easiest way to do this is to define
 * 
 * jmolSetCallback("evalCallback", "whateverFunction")
 * 
 * prior to the jmolApplet() command
 * 
 * This is because the signed applet was having trouble finding _jmol in 
 * Protein Explorer
 * 
 * see JmolConstants for callback types.
 * 
 * The use of jmolButtons is fully deprecated and NOT recommended.
 * 
 */

public class Jmol implements WrappedApplet {

  Jvm12 jvm12;
  JmolPopup jmolpopup;

  boolean mayScript;
  boolean haveDocumentAccess;
  boolean popupMenuAllowed = true;
  boolean needPopupMenu;
  boolean loading;

  String[] callbacks = new String[JmolConstants.CALLBACK_COUNT];

  String language;
  String menuStructure;
  String htmlName;
  String fullName;
  String syncId;
  String languagePath;

  AppletWrapper appletWrapper;
  private JmolViewer viewer;

  private final static boolean REQUIRE_PROGRESSBAR = true;
  private boolean jvm12orGreater;
  private boolean hasProgressBar;

  protected boolean doTranslate = true;

  private String statusForm;
  private String statusText;
  private String statusTextarea;

  private int paintCounter;

  /*
   * miguel 2004 11 29
   * 
   * WARNING! DANGER!
   * 
   * I have discovered that if you call JSObject.getWindow().toString() on
   * Safari v125.1 / Java 1.4.2_03 then it breaks or kills Safari I filed Apple
   * bug report #3897879
   * 
   * Therefore, do *not* call System.out.println("" + jsoWindow);
   */

  /*
   * see below public String getAppletInfo() { return appletInfo; }
   * 
   * static String appletInfo = GT._("Jmol Applet. Part of the OpenScience
   * project. " + "See http://www.jmol.org for more information");
   */
  public void setAppletWrapper(AppletWrapper appletWrapper) {
    this.appletWrapper = appletWrapper;
  }

  //protected void finalize() throws Throwable {
  //  System.out.println("Jmol finalize " + this);
  //  super.finalize();
  //}

  public void init() {
    htmlName = getParameter("name");
    syncId = getParameter("syncId");
    fullName = htmlName + "__" + syncId + "__";
    System.out.println("Jmol applet " + fullName + " initializing");
    setLogging();

    String ms = getParameter("mayscript");
    mayScript = (ms != null) && (!ms.equalsIgnoreCase("false"));
    JmolAppletRegistry.checkIn(fullName, appletWrapper);
    initWindows();
    initApplication();
  }

  public void destroy() {
    gRight = null;
    JmolAppletRegistry.checkOut(fullName);
    viewer.setModeMouse(JmolConstants.MOUSE_NONE);
    viewer = null;
    if (jvm12 != null) {
      jvm12.console = null;
      jvm12 = null;
    }
    System.out.println("Jmol applet " + fullName + " destroyed");
  }

  String getParameter(String paramName) {
    return appletWrapper.getParameter(paramName);
  }

  public Graphics setStereoGraphics(boolean isStereo) {
    isStereoSlave = isStereo;
    return (isStereo ? appletWrapper.getGraphics() : null);
  }

  boolean isSigned;

  public void initWindows() {

    String options = "-applet";
    isSigned = getBooleanValue("signed", false) || appletWrapper.isSigned();
    if (isSigned)
      options += "-signed";
    if (getBooleanValue("useCommandThread", isSigned))
      options += "-threaded";
    String s = getValue("MaximumSize", null);
    if (s != null)
      options += "-maximumSize " + s;
    // note, -appletProxy must be the LAST item added
    s = getValue("JmolAppletProxy", null);
    if (s != null)
      options += "-appletProxy " + s;
    viewer = JmolViewer.allocateViewer(appletWrapper,
        null, fullName, appletWrapper.getDocumentBase(),
        appletWrapper.getCodeBase(), options, new MyStatusListener());
    String menuFile = getParameter("menuFile");
    if (menuFile != null)
      menuStructure = viewer.getFileAsString(menuFile);
    jvm12orGreater = viewer.isJvm12orGreater();
    if (jvm12orGreater)
      jvm12 = new Jvm12(appletWrapper, viewer, options);
    if (Logger.debugging) {
      Logger.debug("checking for jsoWindow mayScript=" + mayScript);
    }
    if (mayScript) {
      mayScript = haveDocumentAccess = false;
      JSObject jsoWindow = null;
      JSObject jsoDocument = null;
      try {
        jsoWindow = JSObject.getWindow(appletWrapper);
        if (Logger.debugging) {
          Logger.debug("jsoWindow=" + jsoWindow);
        }
        if (jsoWindow == null) {
          Logger
              .error("jsoWindow returned null ... no JavaScript callbacks :-(");
        } else {
          mayScript = true;
        }
        jsoDocument = (JSObject) jsoWindow.getMember("document");
        if (jsoDocument == null) {
          Logger
              .error("jsoDocument returned null ... no DOM manipulations :-(");
        } else {
          haveDocumentAccess = true;
        }
      } catch (Exception e) {
        Logger
            .error("Microsoft MSIE bug -- http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5012558 "
                + e);
      }
      if (Logger.debugging) {
        Logger.debug("jsoWindow:" + jsoWindow + " jsoDocument:" + jsoDocument
            + " mayScript:" + mayScript + " haveDocumentAccess:"
            + haveDocumentAccess);
      }
    }
  }

  private void setLogging() {
    int iLevel = (getValue("logLevel", (getBooleanValue("debug", false) ? "5"
        : "4"))).charAt(0) - '0';
    if (iLevel != 4)
      System.out.println("setting logLevel=" + iLevel
          + " -- To change, use script \"set logLevel [0-5]\"");
    Logger.setLogLevel(iLevel);
  }

  private boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value
        .equalsIgnoreCase("yes"));
  }

  private String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    if (stringValue != null)
      return stringValue;
    return defaultValue;
  }

  private String getValueLowerCase(String paramName, String defaultValue) {
    String value = getValue(paramName, defaultValue);
    if (value != null) {
      value = value.trim().toLowerCase();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }

  public void initApplication() {
    viewer.pushHoldRepaint();
    {
      // REQUIRE that the progressbar be shown
      hasProgressBar = getBooleanValue("progressbar", false);
      String emulate = getValueLowerCase("emulate", "jmol");
      setStringProperty("defaults", emulate.equals("chime") ? "RasMol" : "Jmol");
      setStringProperty("backgroundColor", getValue("bgcolor", getValue(
          "boxbgcolor", "black")));

      viewer.setBooleanProperty("frank", true);
      loading = true;
      for (int i = 0; i < JmolConstants.CALLBACK_COUNT; i++) {
        String name = JmolConstants.getCallbackName(i);
        setValue(name, null);
      }
      loading = false;

      language = getParameter("language");
      if (language != null) {
        System.out.print("requested language=" + language + "; ");
        new GT(language);
      }
      doTranslate = (!"none".equals(language) && getBooleanValue("doTranslate",
          true));
      language = GT.getLanguage();
      System.out.println("language=" + language);

      boolean haveCallback = false;
      //these are set by viewer.setStringProperty() from setValue
      for (int i = 0; i < JmolConstants.CALLBACK_COUNT && !haveCallback; i++)
        haveCallback = (callbacks[i] != null);
      if (haveCallback || statusForm != null || statusText != null) {
        if (!mayScript)
          Logger
              .warn("MAYSCRIPT missing -- all applet JavaScript calls disabled");
      }
      if (callbacks[JmolConstants.CALLBACK_SCRIPT] == null
          && callbacks[JmolConstants.CALLBACK_ERROR] == null)
        if (callbacks[JmolConstants.CALLBACK_MESSAGE] != null
            || statusForm != null || statusText != null) {
          if (doTranslate && (getValue("doTranslate", null) == null)) {
            doTranslate = false;
            Logger
                .warn("Note -- Presence of message callback disables disable translation;"
                    + " to enable message translation use jmolSetTranslation(true) prior to jmolApplet()");
          }
          if (doTranslate)
            Logger
                .warn("Note -- Automatic language translation may affect parsing of message callbacks"
                    + " messages; use scriptCallback or errorCallback to process errors");
        }

      if (!doTranslate) {
        GT.setDoTranslate(false);
        Logger.warn("Note -- language translation disabled");
      }

      statusForm = getValue("StatusForm", null);
      statusText = getValue("StatusText", null); //text
      statusTextarea = getValue("StatusTextarea", null); //textarea

      if (statusForm != null && statusText != null) {
        Logger.info("applet text status will be reported to document."
            + statusForm + "." + statusText);
      }
      if (statusForm != null && statusTextarea != null) {
        Logger.info("applet textarea status will be reported to document."
            + statusForm + "." + statusTextarea);
      }

      // should the popupMenu be loaded ?
      needPopupMenu = getBooleanValue("popupMenu", true);
      if (needPopupMenu)
        getPopupMenu(false);
      //if (needPopupMenu)
      //loadPopupMenuAsBackgroundTask();

      loadNodeId(getValue("loadNodeId", null));

      String loadParam;
      String scriptParam = getValue("script", "");
      if ((loadParam = getValue("loadInline", null)) != null) {
        loadInlineSeparated(loadParam, (scriptParam.length() > 0 ? scriptParam
            : null));
      } else {
        if ((loadParam = getValue("load", null)) != null)
          scriptParam = "load \"" + loadParam + "\";" + scriptParam;
        if (scriptParam.length() > 0)
          scriptProcessor(scriptParam, null, SCRIPT_NOWAIT);
      }
    }
    viewer.popHoldRepaint();
  }

  private void setValue(String name, String defaultValue) {
    setStringProperty(name, getValue(name, defaultValue));
  }

  private void setStringProperty(String name, String value) {
    if (value == null)
      return;
    Logger.info(name + " = \"" + value + "\"");
    viewer.setStringProperty(name, value);
  }

  void sendJsTextStatus(String message) {
    if (!haveDocumentAccess || statusForm == null || statusText == null)
      return;
    try {
      JSObject jsoWindow = JSObject.getWindow(appletWrapper);
      JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
      JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
      if (statusText != null) {
        JSObject jsoText = (JSObject) jsoForm.getMember(statusText);
        jsoText.setMember("value", message);
      }
    } catch (Exception e) {
      Logger.error("error indicating status at document." + statusForm + "."
          + statusText + ":" + e.toString());
    }
  }

  void sendJsTextareaStatus(String message) {
    if (!haveDocumentAccess || statusForm == null || statusTextarea == null)
      return;
    try {
      JSObject jsoWindow = JSObject.getWindow(appletWrapper);
      JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
      JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
      if (statusTextarea != null) {
        JSObject jsoTextarea = (JSObject) jsoForm.getMember(statusTextarea);
        if (message == null) {
          jsoTextarea.setMember("value", "");
        } else {
          String info = (String) jsoTextarea.getMember("value");
          jsoTextarea.setMember("value", info + "\n" + message);
        }
      }
    } catch (Exception e) {
      Logger.error("error indicating status at document." + statusForm + "."
          + statusTextarea + ":" + e.toString());
    }
  }

  public boolean showPaintTime = false;
  public void paint(Graphics g) {
    //paint is invoked for system-based updates (obscurring, for example)
    //Opera has a bug in relation to displaying the Java Console. 
    update(g, "paint ");
  }

  private boolean isUpdating;

  public void update(Graphics g) {
    //update is called in response to repaintManager's repaint() request.
    update(g, "update");
  }

  protected Graphics gRight;
  protected boolean isStereoSlave;
  
  private void update(Graphics g, String source) {
    if (viewer == null) // it seems that this can happen at startup sometimes
      return;
    if (isUpdating)
      return;

    //Opera has been known to allow entry to update() by one thread
    //while another thread is doing a paint() or update(). 

    //for now, leaving out the "needRendering" idea

    isUpdating = true;
    if (showPaintTime)
      startPaintClock();
    Dimension size = jvm12orGreater ? jvm12.getSize() : appletWrapper.size();
    viewer.setScreenDimension(size);
    //Rectangle rectClip = jvm12orGreater ? jvm12.getClipBounds(g) : g.getClipRect();
    ++paintCounter;
    if (REQUIRE_PROGRESSBAR && !isSigned && !hasProgressBar
        && paintCounter < 30 && (paintCounter & 1) == 0) {
      printProgressbarMessage(g);
      viewer.repaintView();
    } else {
      //System.out.println("UPDATE1: " + source + " " + Thread.currentThread());
      //System.out.println(fullName + " update gRight = " + gRight);
      if (!isStereoSlave)
        viewer.renderScreenImage(g, gRight, size, null);//rectClip);
      //System.out.println("UPDATE2: " + source + " " + Thread.currentThread());
    }

    if (showPaintTime) {
      stopPaintClock();
      showTimes(10, 10, g);
    }
    isUpdating = false;
  }

  private final static String[] progressbarMsgs = {
      "Jmol developer alert!",
      "",
      "Please use jmol.js. You are missing the require 'progressbar' parameter.",
      "  <param name='progressbar' value='true' />", };

  private void printProgressbarMessage(Graphics g) {
    g.setColor(Color.yellow);
    g.fillRect(0, 0, 10000, 10000);
    g.setColor(Color.black);
    for (int i = 0, y = 13; i < progressbarMsgs.length; ++i, y += 13) {
      g.drawString(progressbarMsgs[i], 10, y);
    }
  }

  public boolean handleEvent(Event e) {
    if (viewer == null)
      return false;
    return viewer.handleOldJvm10Event(e);
  }

  // code to record last and average times
  // last and average of all the previous times are shown in the status window

  private int timeLast, timeCount, timeTotal;
  private long timeBegin;

  private int lastMotionEventNumber;

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    int motionEventNumber = viewer.getMotionEventNumber();
    if (lastMotionEventNumber != motionEventNumber) {
      lastMotionEventNumber = motionEventNumber;
      timeCount = timeTotal = 0;
      timeLast = -1;
    }
  }

  private void stopPaintClock() {
    int time = (int) (System.currentTimeMillis() - timeBegin);
    if (timeLast != -1) {
      timeTotal += timeLast;
      ++timeCount;
    }
    timeLast = time;
  }

  private String fmt(int num) {
    if (num < 0)
      return "---";
    if (num < 10)
      return "  " + num;
    if (num < 100)
      return " " + num;
    return "" + num;
  }

  private void showTimes(int x, int y, Graphics g) {
    int timeAverage = (timeCount == 0) ? -1 : (timeTotal + timeCount / 2)
        / timeCount; // round, don't truncate
    g.setColor(Color.green);
    g.drawString(fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms", x, y);
  }

  private final static int SCRIPT_CHECK = 0;
  private final static int SCRIPT_WAIT = 1;
  private final static int SCRIPT_NOWAIT = 2;

  private String scriptProcessor(String script, String statusParams,
                                 int processType) {
    /*
     * Idea here is to provide a single point of entry
     * Synchronization may not work, because it is possible for the NOWAIT variety of
     * scripts to return prior to full execution 
     * 
     */
    if (script == null || script.length() == 0)
      return "";
    switch (processType) {
    case SCRIPT_CHECK:
      String err = viewer.scriptCheck(script);
      return (err == null ? "" : err);
    case SCRIPT_WAIT:
      if (statusParams != null)
        return viewer.scriptWaitStatus(script, statusParams).toString();
      return viewer.scriptWait(script);
    case SCRIPT_NOWAIT:
    default:
      return viewer.script(script);
    }
  }

  public void script(String script) {
    if (script == null || script.length() == 0)
      return;
    scriptProcessor(script, null, SCRIPT_NOWAIT);
  }

  public String scriptCheck(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_CHECK);
  }

  public String scriptNoWait(String script) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, null, SCRIPT_NOWAIT);
  }

  public String scriptWait(String script) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = null;
    return scriptProcessor(script, null, SCRIPT_WAIT);
  }

  StringBuffer outputBuffer;
  
  public String scriptWait(String script, String statusParams) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = null;
    return scriptProcessor(script, statusParams, SCRIPT_WAIT);
  }

  public String scriptWaitOutput(String script) {
    if (script == null || script.length() == 0)
      return "";
    outputBuffer = new StringBuffer();
    viewer.scriptWaitStatus(script, "");
    String str = (outputBuffer == null ? "" : outputBuffer.toString());
    outputBuffer = null;
    return str;
  }

  synchronized public void syncScript(String script) {
    viewer.syncScript(script, "~");
  }

  public String getAppletInfo() {
    return GT
        ._(
            "Jmol Applet version {0} {1}.\n\nAn OpenScience project.\n\nSee http://www.jmol.org for more information",
            new Object[] { JmolConstants.version, JmolConstants.date })
        + "\nhtmlName = "
        + Escape.escape(htmlName)
        + "\nsyncId = "
        + Escape.escape(syncId)
        + "\ndocumentBase = "
        + Escape.escape("" + appletWrapper.getDocumentBase())
        + "\ncodeBase = "
        + Escape.escape("" + appletWrapper.getCodeBase());
  }

  public Object getProperty(String infoType) {
    return viewer.getProperty(null, infoType, "");
  }

  public Object getProperty(String infoType, String paramInfo) {
    return viewer.getProperty(null, infoType, paramInfo);
  }

  public String getPropertyAsString(String infoType) {
    return viewer.getProperty("readable", infoType, "").toString();
  }

  public String getPropertyAsString(String infoType, String paramInfo) {
    return viewer.getProperty("readable", infoType, paramInfo).toString();
  }

  public String getPropertyAsJSON(String infoType) {
    return viewer.getProperty("JSON", infoType, "").toString();
  }

  public String getPropertyAsJSON(String infoType, String paramInfo) {
    return viewer.getProperty("JSON", infoType, paramInfo).toString();
  }

  public String loadInlineString(String strModel, String script, boolean isAppend) {
    String errMsg = viewer.loadInline(strModel, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  public String loadInlineArray(String[] strModels, String script,
                              boolean isAppend) {
    if (strModels == null || strModels.length == 0)
      return null;
    String errMsg = viewer.loadInline(strModels, isAppend);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  /**
   * @deprecated
   * @param strModel
   * @return         error or null
   */
  public String loadInline(String strModel) {
    return loadInlineString(strModel, "", false);
  }

  /**
   * @deprecated
   * @param strModel
   * @param script
   * @return         error or null
   */
  public String loadInline(String strModel, String script) {
    return loadInlineString(strModel, script, false);
  }

  /**
   * @deprecated
   * @param strModels
   * @return         error or null
   */
  public String loadInline(String[] strModels) {
    return loadInlineArray(strModels, "", false);
  }

  /**
   * @deprecated
   * @param strModels
   * @param script
   * @return       error or null
   */
  public String loadInline(String[] strModels, String script) {
    return loadInlineArray(strModels, script, false);
  }

  private String loadInlineSeparated(String strModel, String script) {
    // from an applet PARAM only -- because it converts | into \n
    if (strModel == null)
      return null;
    String errMsg = viewer.loadInline(strModel);
    if (errMsg == null)
      script(script);
    return errMsg;
  }

  public String loadDOMNode(JSObject DOMNode) {
    // This should provide a route to pass in a browser DOM node
    // directly as a JSObject. Unfortunately does not seem to work with
    // current browsers
    return viewer.openDOM(DOMNode);
  }

  public String loadNodeId(String nodeId) {
    if (!haveDocumentAccess)
      return "ERROR: NO DOCUMENT ACCESS";
    if (nodeId == null)
      return null;
    // Retrieve Node ...
    // First try to find by ID
    Object[] idArgs = { nodeId };
    JSObject tryNode = null;
    try {
      JSObject jsoWindow = JSObject.getWindow(appletWrapper);
      JSObject jsoDocument = (JSObject) jsoWindow.getMember("document");
      tryNode = (JSObject) jsoDocument.call("getElementById", idArgs);

      // But that relies on a well-formed CML DTD specifying ID search.
      // Otherwise, search all cml:cml nodes.
      if (tryNode == null) {
        Object[] searchArgs = { "http://www.xml-cml.org/schema/cml2/core",
            "cml" };
        JSObject tryNodeList = (JSObject) jsoDocument.call(
            "getElementsByTagNameNS", searchArgs);
        if (tryNodeList != null) {
          for (int i = 0; i < ((Number) tryNodeList.getMember("length"))
              .intValue(); i++) {
            tryNode = (JSObject) tryNodeList.getSlot(i);
            Object[] idArg = { "id" };
            String idValue = (String) tryNode.call("getAttribute", idArg);
            if (nodeId.equals(idValue))
              break;
            tryNode = null;
          }
        }
      }
    } catch (Exception e) {
      return "" + e;
    }
    return (tryNode == null ? "ERROR: No CML node" : loadDOMNode(tryNode));
  }

  class MyStatusListener implements JmolStatusListener {

    public boolean notifyEnabled(int type) {
      switch (type) {
      case JmolConstants.CALLBACK_ANIMFRAME:
      case JmolConstants.CALLBACK_ECHO:
      case JmolConstants.CALLBACK_ERROR:
      case JmolConstants.CALLBACK_EVAL:
      case JmolConstants.CALLBACK_LOADSTRUCT:
      case JmolConstants.CALLBACK_MEASURE:
      case JmolConstants.CALLBACK_MESSAGE:
      case JmolConstants.CALLBACK_PICK:
      case JmolConstants.CALLBACK_SYNC:
      case JmolConstants.CALLBACK_SCRIPT:
        return true;
      case JmolConstants.CALLBACK_HOVER:
      case JmolConstants.CALLBACK_MINIMIZATION:
      case JmolConstants.CALLBACK_RESIZE:
      }
      return (callbacks[type] != null);
    }

    private boolean haveNotifiedError;

    public void notifyCallback(int type, Object[] data) {

      String callback = callbacks[type];
      boolean doCallback = (callback != null && (data == null || data[0] == null));
      if (data != null)
        data[0] = htmlName;
      String strInfo = (data == null || data[1] == null ? null : data[1]
          .toString());

      //System.out.println("Jmol.java notifyCallback " + type + " " + callback
      //  + " " + strInfo);
      switch (type) {
      case JmolConstants.CALLBACK_ANIMFRAME:
        // Note: twos-complement. To get actual frame number, use 
        // Math.max(frameNo, -2 - frameNo)
        // -1 means all frames are now displayed
        int[] iData = (int[]) data[1];
        int frameNo = iData[0];
        int fileNo = iData[1];
        int modelNo = iData[2];
        int firstNo = iData[3];
        int lastNo = iData[4];
        boolean isAnimationRunning = (frameNo <= -2);
        int animationDirection = (firstNo < 0 ? -1 : 1);
        int currentDirection = (lastNo < 0 ? -1 : 1);

        /*
         * animationDirection is set solely by the "animation direction +1|-1" script command
         * currentDirection is set by operations such as "anim playrev" and coming to the end of 
         * a sequence in "anim mode palindrome"
         * 
         * It is the PRODUCT of these two numbers that determines what direction the animation is
         * going.
         * 
         */
        if (doCallback) {
          data = new Object[] { htmlName,
              new Integer(Math.max(frameNo, -2 - frameNo)),
              new Integer(fileNo), new Integer(modelNo),
              new Integer(Math.abs(firstNo)), new Integer(Math.abs(lastNo)),
              new Integer(isAnimationRunning ? 1 : 0),
              new Integer(animationDirection), new Integer(currentDirection) };
        }
        if (jmolpopup != null && !isAnimationRunning)
          jmolpopup.updateComputedMenus();
        break;
      case JmolConstants.CALLBACK_ECHO:
        boolean isScriptQueued = (((Integer) data[2]).intValue() == 1);
        boolean doOutput = true;
        if (!doCallback) {
          if (isScriptQueued) {
            consoleMessage(strInfo);
            doOutput = false;
          }
          doCallback = ((callback = callbacks[type = JmolConstants.CALLBACK_MESSAGE]) != null);
        }
        if (doOutput)
          output(strInfo);
        break;
      case JmolConstants.CALLBACK_EVAL:
      case JmolConstants.CALLBACK_HOVER:
      case JmolConstants.CALLBACK_ERROR:
        break;
      case JmolConstants.CALLBACK_LOADSTRUCT:
        String errorMsg = (String) data[4];
        if (errorMsg != null) {
          showStatusAndConsole((errorMsg.indexOf("NOTE:") >= 0 ? "" : GT
              ._("File Error:"))
              + errorMsg, true);
          return;
        }
        break;
      case JmolConstants.CALLBACK_MEASURE:
        //pending, deleted, or completed
        if (!doCallback)
          doCallback = ((callback = callbacks[type = JmolConstants.CALLBACK_MESSAGE]) != null);
        String status = (String) data[3]; 
        if (status.indexOf("Picked") >= 0) {//picking mode
          showStatusAndConsole(strInfo, true); // set picking measure distance
        } else if (status.indexOf("Completed") >= 0) {
          strInfo = status + ": " + strInfo;
          consoleMessage(strInfo);
        }
        break;
      case JmolConstants.CALLBACK_MESSAGE:
        if (doCallback)
          output(strInfo);
        else
          consoleMessage(strInfo);
        if (strInfo == null)
          return;
        break;
      case JmolConstants.CALLBACK_MINIMIZATION:
        //just send it
        break;
      case JmolConstants.CALLBACK_PICK:
        showStatusAndConsole(strInfo, true);
        break;
      case JmolConstants.CALLBACK_RESIZE:
        //just send it
        break;
      case JmolConstants.CALLBACK_SCRIPT:
        int msWalltime = ((Integer) data[3]).intValue();
        // general message has msWalltime = 0
        // special messages have msWalltime < 0
        // termination message has msWalltime > 0 (1 + msWalltime)
        // "script started"/"pending"/"script terminated"/"script completed"
        //   do not get sent to console
        boolean toConsole = (msWalltime == 0);
        if (msWalltime > 0) {
          // termination -- button legacy
          notifyScriptTermination();
        } else if (!doCallback) {
          //termination messsage ONLY if script callback enabled -- not to message queue
          //for compatibility reasons
          doCallback = ((callback = callbacks[type = JmolConstants.CALLBACK_MESSAGE]) != null);
        }
        showStatusAndConsole(strInfo, toConsole);
        break;
      case JmolConstants.CALLBACK_SYNC:
        sendScript(strInfo, (String) data[2], true, doCallback);
        return;
      }
      if (!doCallback || !mayScript)
        return;
      try {
        JSObject jsoWindow = JSObject.getWindow(appletWrapper);
        if (callback.equals("alert"))
          jsoWindow.call(callback, new Object[] { strInfo });
        else if (callback.length() > 0)
          jsoWindow.call(callback, data);
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.debugging) {
            Logger.debug(JmolConstants.getCallbackName(type)
                + " call error to " + callback + ": " + e);
          }
        haveNotifiedError = true;
      }
    }

    private void output(String s) {
      if (outputBuffer != null && s != null)
        outputBuffer.append(s).append('\n');
    }

    private void notifyScriptTermination() {
      // this had to do with button callbacks
    }

    private String notifySync(String info, String appletName) {
      String syncCallback = callbacks[JmolConstants.CALLBACK_SYNC];
      if (!mayScript || syncCallback == null)
        return info;
      try {
        JSObject jsoWindow = JSObject.getWindow(appletWrapper);
        if (syncCallback.length() > 0)
          return "" + jsoWindow.call(syncCallback, new Object[] { htmlName,
              info, appletName });
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.debugging) {
            Logger.debug("syncCallback call error to " + syncCallback + ": "
                + e);
          }
        haveNotifiedError = true;
      }
      return info;
    }

    public void setCallbackFunction(String callbackName, String callbackFunction) {
      //also serves to change language for callbacks and menu
      if (callbackName.equalsIgnoreCase("menu")) {
        menuStructure = callbackFunction;
        if (needPopupMenu)
          getPopupMenu(false);
        return;
      }
      if (callbackName.equalsIgnoreCase("language")) {
        new GT(callbackFunction);
        language = GT.getLanguage();
        if (needPopupMenu)
          getPopupMenu(true);
        clearDefaultConsoleMessage();
        if (jvm12 != null && isSigned)
          Jvm12.newDialog(true);
        return;
      }
      int id = JmolConstants.getCallbackId(callbackName);
      if (id >= 0 && (loading || id != JmolConstants.CALLBACK_EVAL)) {
        callbacks[id] = callbackFunction;
        return;
      }
      String s = "";
      for (int i = 0; i < JmolConstants.CALLBACK_COUNT; i++)
        s += " " + JmolConstants.getCallbackName(i);
      consoleMessage("Available callbacks include: " + s);
    }

    protected void finalize() throws Throwable {
      Logger.debug("MyStatusListener finalize " + this);
      super.finalize();
    }

    public String eval(String strEval) {
      // may be appletName\1script
      int pt = strEval.indexOf("\1");
      if (pt >= 0)
        return sendScript(strEval.substring(pt + 1), strEval.substring(0, pt),
            false, false);
      if (strEval.startsWith("_GET_MENU"))
        return (jmolpopup == null ? "" : jmolpopup.getMenu("Jmol version "
            + Viewer.getJmolVersion() + "|" + strEval));
      if (!haveDocumentAccess)
        return "NO EVAL ALLOWED";
      JSObject jsoWindow = null;
      JSObject jsoDocument = null;
      try {
        jsoWindow = JSObject.getWindow(appletWrapper);
        jsoDocument = (JSObject) jsoWindow.getMember("document");
      } catch (Exception e) {
        if (Logger.debugging)
          Logger.debug(" error setting jsoWindow or jsoDocument:" + jsoWindow
              + ", " + jsoDocument);
        return "NO EVAL ALLOWED";
      }
      if (callbacks[JmolConstants.CALLBACK_EVAL] != null) {
        notifyCallback(JmolConstants.CALLBACK_EVAL, new Object[] { null,
            strEval });
        return "";
      }
      try {
        if (!haveDocumentAccess
            || ((Boolean) jsoDocument.eval("!!_jmol.noEval")).booleanValue())
          return "NO EVAL ALLOWED";
      } catch (Exception e) {
        Logger
            .error("# no _jmol in evaluating " + strEval + ":" + e.toString());
        return "";
      }
      try {
        return "" + jsoDocument.eval(strEval);
      } catch (Exception e) {
        Logger.error("# error evaluating " + strEval + ":" + e.toString());
      }
      return "";
    }

    /**
     * 
     * @param fileName
     * @param type
     * @param text_or_bytes
     * @param quality
     * @return          null (canceled) or a message starting with OK or an error message
     */
    public String createImage(String fileName, String type, Object text_or_bytes,
                              int quality) {
      boolean isImage = (quality != Integer.MIN_VALUE); 
      if (isSigned) {
        if (jvm12 != null) {
          try {
            return jvm12.createImage(fileName, type, text_or_bytes, quality);
          } catch (Exception e) {
          }
        }
      } else if (isImage) {
        return GT
            ._(
                "File creation by this applet is not allowed. For Base64 JPEG format, use {0}.",
                "jmolGetPropertyAsString('image')");
      }
      return GT._("File creation failed.");
    }

    public float[][] functionXY(String functionName, int nX, int nY) {
      /*three options:
       * 
       *  nX > 0  and  nY > 0        return one at a time, with (slow) individual function calls
       *  nX < 0  and  nY > 0        return a string that can be parsed to give the list of values
       *  nX < 0  and  nY < 0        fill the supplied float[-nX][-nY] array directly in JavaScript 
       *  
       */

      //System.out.println("functionXY" + nX + " " + nY  + " " + functionName);
      float[][] fxy = new float[Math.abs(nX)][Math.abs(nY)];
      if (!mayScript || nX == 0 || nY == 0)
        return fxy;
      try {
        JSObject jsoWindow = JSObject.getWindow(appletWrapper);
        if (nX > 0 && nY > 0) { // fill with individual function calls (slow)
          for (int i = 0; i < nX; i++)
            for (int j = 0; j < nY; j++) {
              fxy[i][j] = ((Double) jsoWindow.call(functionName, new Object[] {
                  htmlName, new Integer(i), new Integer(j) })).floatValue();
            }
        } else if (nY > 0) { // fill with parsed values from a string (pretty fast)
          String data = (String) jsoWindow.call(functionName, new Object[] {
              htmlName, new Integer(nX), new Integer(nY) });
          //System.out.println(data);
          nX = Math.abs(nX);
          float[] fdata = new float[nX * nY];
          Parser.parseFloatArray(data, null, fdata);
          for (int i = 0, ipt = 0; i < nX; i++) {
            for (int j = 0; j < nY; j++, ipt++) {
              fxy[i][j] = fdata[ipt];
            }
          }
        } else { // fill float[][] directly using JavaScript
          jsoWindow.call(functionName, new Object[] { htmlName,
              new Integer(nX), new Integer(nY), fxy });
        }
      } catch (Exception e) {
        Logger.error("Exception " + e.getMessage() + " with nX, nY: " + nX
            + " " + nY);
      }
     // for (int i = 0; i < nX; i++)
       // for (int j = 0; j < nY; j++)
         // System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
      return fxy;
    }

    public void handlePopupMenu(int x, int y) {
      if (!popupMenuAllowed) {
        showConsole(true);
        return;
      }
      if (jmolpopup == null)
        return;
      if (!language.equals(GT.getLanguage())) {
        getPopupMenu(true);
        language = GT.getLanguage();
      }
      jmolpopup.show(x, y);
    }

    public void showUrl(String urlString) {
      if (Logger.debugging) {
        Logger.debug("showUrl(" + urlString + ")");
      }
      if (urlString != null && urlString.length() > 0) {
        try {
          URL url = new URL(urlString);
          appletWrapper.getAppletContext().showDocument(url, "_blank");
        } catch (MalformedURLException mue) {
          showStatusAndConsole("Malformed URL:" + urlString, true);
        }
      }
    }

    private void showStatusAndConsole(String message, boolean toConsole) {
      try {
        appletWrapper.showStatus(message);
        sendJsTextStatus(message);
        if (toConsole)
          consoleMessage(message);
        else
          output(message);
      } catch (Exception e) {
        //ignore if page is closing
      }
    }

    private String defaultMessage;

    private void clearDefaultConsoleMessage() {
      defaultMessage = null;
    }

    private void consoleMessage(String message) {
      if (jvm12 != null && jvm12.haveConsole()) {
        if (defaultMessage == null) {
          GT.setDoTranslate(true);
          defaultMessage = GT
              ._("Messages will appear here. Enter commands in the box below. Click the console Help menu item for on-line help, which will appear in a new browser window.");
          GT.setDoTranslate(doTranslate);
        }
        if (jvm12.getConsoleMessage().startsWith(defaultMessage))
          jvm12.consoleMessage("");
        jvm12.consoleMessage(message);
        if (message == null) {
          jvm12.consoleMessage(defaultMessage);
        }
      }
      output(message);
      sendJsTextareaStatus(message);
    }

    public void showConsole(boolean showConsole) {
      //Logger.info("JmolApplet.showConsole(" + showConsole + ")");
      if (jvm12 != null)
        jvm12.showConsole(showConsole);
    }

    private String sendScript(String script, String appletName, boolean isSync,
                              boolean doCallback) {
      if (doCallback) {
        script = notifySync(script, appletName);
        // if the notified JavaScript function returns "" or 0, then 
        // we do NOT continue to notify the other applets
        if (script == null || script.length() == 0 || script.equals("0"))
          return "";
      }

      Vector apps = new Vector();
      JmolAppletRegistry.findApplets(appletName, syncId, fullName, apps);
      int nApplets = apps.size();
      if (nApplets == 0) {
        if (!doCallback && !appletName.equals("*"))
          Logger.error(fullName + " couldn't find applet " + appletName);
        return "";
      }
      StringBuffer sb = (isSync ? null : new StringBuffer());
      boolean getGraphics = (isSync && script.equals(Viewer.SYNC_GRAPHICS_MESSAGE));
      boolean setNoGraphics = (isSync && script.equals(Viewer.SYNC_NO_GRAPHICS_MESSAGE));
      if (getGraphics)
        gRight = null;
      for (int i = 0; i < nApplets; i++) {
        String theApplet = (String) apps.elementAt(i);
        JmolAppletInterface app = (JmolAppletInterface) JmolAppletRegistry.htRegistry
            .get(theApplet);
        if (Logger.debugging)
          Logger.debug(fullName + " sending to " + theApplet + ": " + script);
        try {
          if (getGraphics || setNoGraphics) {
            gRight = app.setStereoGraphics(getGraphics);
            return "";
          }
          if (isSync)
            app.syncScript(script);
          else
            sb.append(app.scriptWait(script, "output")).append("\n");
        } catch (Exception e) {
          String msg = htmlName + " couldn't send to " + theApplet + ": "
              + script + ": " + e;
          Logger.error(msg);
          if (!isSync)
            sb.append(msg);
        }
      }
      return (isSync ? "" : sb.toString());
    }

    public Hashtable getRegistryInfo() {
      JmolAppletRegistry.checkIn(null, null); //cleans registry
      return JmolAppletRegistry.htRegistry;
    }

    public String dialogAsk(String type, String fileName) {
      if (!isSigned || jvm12 == null)
        return null;
      return jvm12.dialogAsk(type, fileName);
    }

  }

  public void getPopupMenu(boolean forceNewConsole) {
    jmolpopup = JmolPopup.newJmolPopup(viewer, doTranslate, menuStructure,
        popupMenuAllowed);
    if (jmolpopup != null && jvm12 != null && !popupMenuAllowed) {
      if (forceNewConsole)
        jvm12.showConsole(false);
      if (jvm12.console == null)
        jvm12.getConsole();
      jmolpopup.installMainMenu(jvm12.console.getMyMenuBar());
    }
  }
}
