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
import org.jmol.adapter.smarter.SmarterJmolAdapter;
// import org.openscience.jmol.adapters.CdkJmolAdapter;
import org.jmol.popup.JmolPopup;
import org.jmol.i18n.GT;

import netscape.javascript.JSObject;
import org.jmol.viewer.JmolConstants;

import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.jmol.util.Logger;

import org.jmol.util.Parser;
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
 *  x = eval(...) 
 * 
 * callbacks include:
 * 
 * [param name="AnimFrameCallback" value="yourJavaScriptMethodName" /]
 * [param name="LoadStructCallback" value="yourJavaScriptMethodName" /]
 * [param name="MessageCallback" value="yourJavaScriptMethodName" /] 
 * [param name="HoverCallback" value="yourJavaScriptMethodName" /] 
 * [param name="ResizeCallback" value="yourJavaScriptMethodName" /] 
 * [param name="PickCallback" value="yourJavaScriptMethodName" /]
 * 
 * The use of jmolButtons is NOT recommended.
 * 
 */

public class Jmol implements WrappedApplet, JmolAppletInterface {

  JmolViewer viewer;

  boolean jvm12orGreater;

  String emulate;

  Jvm12 jvm12;

  JmolPopup jmolpopup;

  String htmlName;

  public JmolAppletRegistry appletRegistry;

  MyStatusListener myStatusListener;

  AppletWrapper appletWrapper;

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
  JSObject jsoWindow;
  JSObject jsoDocument;
  boolean mayScript;
  boolean haveDocumentAccess;
  boolean doTranslate = true;
  
  String animFrameCallback;
  String resizeCallback;
  String loadStructCallback;
  String messageCallback;
  String pickCallback;
  String hoverCallback;

  String statusForm;
  String statusText;
  String statusTextarea;
  
  final static boolean REQUIRE_PROGRESSBAR = true;

  boolean hasProgressBar;

  int paintCounter;

  /*
   * see below public String getAppletInfo() { return appletInfo; }
   * 
   * static String appletInfo = GT._("Jmol Applet. Part of the OpenScience
   * project. " + "See http://www.jmol.org for more information");
   */
  public void setAppletWrapper(AppletWrapper appletWrapper) {
    this.appletWrapper = appletWrapper;
  }

  String language;
  
  public void init() {
    System.out.println("Init jmol");
    htmlName = getParameter("name");
    language = getParameter("language");
    if (language != null) {
      System.out.print("requested language=" + language + "; ");
      new GT(language);
    }
    language = GT.getLanguage();
    System.out.println("language=" + language);
    doTranslate = getBooleanValue("doTranslate", true);
    System.out.println("Jmol applet "+htmlName);
    setLogging();
    String ms = getParameter("mayscript");
    mayScript = (ms != null) && (!ms.equalsIgnoreCase("false"));
    appletRegistry = new JmolAppletRegistry(htmlName, mayScript, appletWrapper);    
    initWindows();
    initApplication();
  }
  
  String getParameter(String paramName) {
    return appletWrapper.getParameter(paramName);
  }

  boolean haveNotifiedError = false;
  
  public void initWindows() {

    // to enable CDK
    // viewer = new JmolViewer(this, new CdkJmolAdapter(null));
    viewer = JmolViewer.allocateViewer(appletWrapper, new SmarterJmolAdapter());
    viewer.setAppletContext(htmlName, appletWrapper.getDocumentBase(),
        appletWrapper.getCodeBase(), getValue("JmolAppletProxy", null));
    myStatusListener = new MyStatusListener();
    viewer.setJmolStatusListener(myStatusListener);

    jvm12orGreater = viewer.isJvm12orGreater();
    if (jvm12orGreater)
      jvm12 = new Jvm12(appletWrapper, viewer);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("checking for jsoWindow mayScript=" + mayScript);
    }
    if (mayScript) {
      mayScript = haveDocumentAccess = false;
      try {
        jsoWindow = JSObject.getWindow(appletWrapper);
        if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
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
        jsoWindow = null;
        jsoDocument = null;
        Logger
            .error("Microsoft MSIE bug -- http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5012558 "
                + e);
      }
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("jsoWindow:" + jsoWindow + " jsoDocument:" + jsoDocument
            + " mayScript:" + mayScript + " haveDocumentAccess:"
            + haveDocumentAccess);
      }
    }
  }

  void setLogging() {
    int iLevel = (getValue("logLevel", "")
        + (getBooleanValue("debug", false) ? "5" : "4")).charAt(0) - '0';
    if (iLevel != 4)
      System.out.println("setting logLevel=" + iLevel
          + " -- To change, use script \"set logLevel [0-5]\"");
    Logger.setLogLevel(iLevel);
  }
  /*
   * PropertyResourceBundle appletProperties = null;
   * 
   * private void loadProperties() { URL codeBase = getCodeBase(); try { URL
   * urlProperties = new URL(codeBase, "JmolApplet.properties");
   * appletProperties = new PropertyResourceBundle(urlProperties.openStream()); }
   * catch (Exception ex) { Logger.error("JmolApplet.loadProperties() -> " +
   * ex); } }
   */

  boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value
        .equalsIgnoreCase("yes"));
  }

  String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    if (stringValue != null)
      return stringValue;
    /*
     * if (appletProperties != null) { try { stringValue =
     * appletProperties.getString(propertyName); return stringValue; } catch
     * (MissingResourceException ex) { } }
     */
    return defaultValue;
  }

  /*
   * private int getValue(String propertyName, int defaultValue) { String
   * stringValue = getValue(propertyName, null); if (stringValue != null) try {
   * return Integer.parseInt(stringValue); } catch (NumberFormatException ex) {
   * Logger.error(propertyName + ":" + stringValue + " is not an
   * integer"); } return defaultValue; }
   * 
   * private double getValue(String propertyName, double defaultValue) { String
   * stringValue = getValue(propertyName, null); if (stringValue != null) try {
   * return (new Double(stringValue)).doubleValue(); } catch
   * (NumberFormatException ex) { Logger.error(propertyName + ":" +
   * stringValue + " is not a double"); } return defaultValue; }
   */
  String getValueLowerCase(String paramName, String defaultValue) {
    String value = getValue(paramName, defaultValue);
    if (value != null) {
      value = value.trim().toLowerCase();
      if (value.length() == 0)
        value = null;
    }
    return value;
  }

  
  
  
  boolean needPopupMenu;
  
  public void initApplication() {
    viewer.pushHoldRepaint();
    {
      // REQUIRE that the progressbar be shown
      hasProgressBar = getBooleanValue("progressbar", false);
      // should the popupMenu be loaded ?
      needPopupMenu = getBooleanValue("popupMenu", true);
      if (needPopupMenu)
        jmolpopup = JmolPopup.newJmolPopup(viewer, doTranslate);  
      //if (needPopupMenu)
        //loadPopupMenuAsBackgroundTask();

      emulate = getValueLowerCase("emulate", "jmol");
      if (emulate.equals("chime")) {
        viewer.setStringProperty("defaults", "RasMol");
      } else {
        viewer.setStringProperty("defaults", "Jmol");
      }
      String bgcolor = getValue("boxbgcolor", "black");
      bgcolor = getValue("bgcolor", bgcolor);
      viewer.setStringProperty("backgroundColor", bgcolor);

      // loadInline(getValue("loadInline", null));
      loadNodeId(getValue("loadNodeId", null));

      viewer.setBooleanProperty("frank", true);
      animFrameCallback = getValue("AnimFrameCallback", null);
      loadStructCallback = getValue("LoadStructCallback", null);
      messageCallback = getValue("MessageCallback", null);
      // pauseCallback = getValue("PauseCallback", null);
      pickCallback = getValue("PickCallback", null);
      resizeCallback = getValue("ResizeCallback", null);
      hoverCallback = getValue("HoverCallback", null);

      statusForm = getValue("StatusForm", null);
      statusText = getValue("StatusText", null); //text
      statusTextarea = getValue("StatusTextarea", null); //textarea
      boolean haveTranslateFlag = (getValue("doTranslate", null) != null);

      if (animFrameCallback != null)
        Logger.info("animFrameCallback=" + animFrameCallback);
      if (hoverCallback != null)
        Logger.info("hoverCallback=" + hoverCallback);
      if (loadStructCallback != null)
        Logger.info("loadStructCallback=" + loadStructCallback);
      if (messageCallback != null)
        Logger.info("messageCallback=" + messageCallback);
      if (pickCallback != null)
        Logger.info("pickCallback=" + pickCallback);
      if (resizeCallback != null)
        Logger.info("resizeCallback=" + resizeCallback);
      if (statusForm != null && statusText != null) {
        Logger.info("applet text status will be reported to document."
            + statusForm + "." + statusText);
      }
      if (statusForm != null && statusTextarea != null) {
        Logger.info("applet textarea status will be reported to document."
            + statusForm + "." + statusTextarea);
      }
      if (animFrameCallback != null || loadStructCallback != null
          || messageCallback != null || hoverCallback != null
          // || pauseCallback != null
          || pickCallback != null || statusForm != null || statusText != null) {
        if (!mayScript)
          Logger
              .warn("MAYSCRIPT missing -- all applet JavaScript calls disabled");
      }
      if (messageCallback != null || statusForm != null || statusText != null) {
        if (!haveTranslateFlag) {
          doTranslate = false;
          Logger
              .warn("Note -- Presence of message callback will disable translation; "
                  + "unless you use jmolSetTranslation(true) prior to jmolApplet()");
        }
        if (doTranslate)
          Logger
              .warn("Note -- language translation may affect parsing of callback messages");
      }
      if ("none".equals(language))
        doTranslate = false;
      if (!doTranslate) {
        GT.setDoTranslate(false);
        Logger.warn("Note -- language translation disabled");
      }
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

  void showStatusAndConsole(String message) {
    appletWrapper.showStatus(message);
    sendJsTextStatus(message);
    consoleMessage(message);
  }

  void sendMessageCallback(String strMsg) {
    if (!mayScript || messageCallback == null)
      return;
    try {
        if (messageCallback.equals("alert"))
          jsoWindow.call(messageCallback, new Object[] { strMsg });
        else
          jsoWindow.call(messageCallback, new Object[] { htmlName, strMsg });
    } catch (Exception e) {
      if (!haveNotifiedError)
        if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
          Logger.debug(
              "messageCallback call error to " + messageCallback + ": " + e);
        }
      haveNotifiedError = true;
    }
  }
  
  void sendJsTextStatus(String message) {
    if (!haveDocumentAccess || statusForm == null || statusText == null)
      return;
    try {
      JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
      if (statusText != null) {
        JSObject jsoText = (JSObject) jsoForm.getMember(statusText);
        jsoText.setMember("value", message);
      }
    } catch (Exception e) {
      Logger.error("error indicating status at document." + statusForm
          + "." + statusText + ":" + e.toString());
    }
  }
  
  void sendJsTextareaStatus(String message) {
    if (!haveDocumentAccess || statusForm == null || statusTextarea == null)
      return;
    try {
      JSObject jsoForm = (JSObject) jsoDocument.getMember(statusForm);
      if (statusTextarea != null) {
        JSObject jsoTextarea = (JSObject) jsoForm.getMember(statusTextarea);
        String info = (String) jsoTextarea.getMember("value");
        jsoTextarea.setMember("value", info + "\n" + message);
      }
    } catch (Exception e) {
      Logger.error("error indicating status at document." + statusForm
          + "." + statusTextarea + ":" + e.toString());
    }
  }
  
  void consoleMessage(String message) {
    if (jvm12 != null)
      jvm12.consoleMessage(message);
    sendJsTextareaStatus(message);
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
      Rectangle rectClip = jvm12orGreater ? jvm12.getClipBounds(g) : g
          .getClipRect();
      ++paintCounter;
      if (REQUIRE_PROGRESSBAR && !hasProgressBar && paintCounter < 30
          && (paintCounter & 1) == 0) {
        printProgressbarMessage(g);
        viewer.repaintView();
      } else {
        //System.out.println("UPDATE1: " + source + " " + Thread.currentThread());
        viewer.renderScreenImage(g, size, rectClip);
        //System.out.println("UPDATE2: " + source + " " + Thread.currentThread());
      }
  
      if (showPaintTime) {
        stopPaintClock();
        showTimes(10, 10, g);
      }
      isUpdating = false;
    }
  
  final static String[] progressbarMsgs = { "Jmol developer alert!", "",
      "Please use jmol.js. You are missing the require 'progressbar' parameter.",
      "  <param name='progressbar' value='true' />",};

  void printProgressbarMessage(Graphics g) {
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

  int timeLast, timeCount, timeTotal;
  void resetTimes() {
    timeCount = timeTotal = 0;
    timeLast = -1;
  }

  void recordTime(int time) {
    if (timeLast != -1) {
      timeTotal += timeLast;
      ++timeCount;
    }
    timeLast = time;
  }

  long timeBegin;

  int lastMotionEventNumber;

  void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    int motionEventNumber = viewer.getMotionEventNumber();
    if (lastMotionEventNumber != motionEventNumber) {
      lastMotionEventNumber = motionEventNumber;
      resetTimes();
    }
  }

  void stopPaintClock() {
    int time = (int) (System.currentTimeMillis() - timeBegin);
    recordTime(time);
  }

  String fmt(int num) {
    if (num < 0)
      return "---";
    if (num < 10)
      return "  " + num;
    if (num < 100)
      return " " + num;
    return "" + num;
  }

  void showTimes(int x, int y, Graphics g) {
    int timeAverage = (timeCount == 0) ? -1 : (timeTotal + timeCount / 2)
        / timeCount; // round, don't truncate
    g.setColor(Color.green);
    g.drawString(fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms", x, y);
  }

  final Object[] buttonCallbackBefore = { null, Boolean.FALSE };

  final Object[] buttonCallbackAfter = { null, Boolean.TRUE };

  boolean buttonCallbackNotificationPending;

  String buttonCallback;
  String buttonName;
  JSObject buttonWindow;

  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    if (!mayScript || buttonWindow == null || buttonCallback == null) {
      buttonCallbackNotificationPending = false;
      return;
    }
    Logger.info(htmlName + " JmolApplet.scriptButton(" + buttonWindow + ","
        + buttonName + "," + script + "," + buttonCallback);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("!!!! calling back " + buttonCallback);
    }
    buttonCallbackBefore[0] = buttonName;
    Logger.debug("trying...");
    buttonWindow.call(buttonCallback, buttonCallbackBefore);
    Logger.debug("made it");

    buttonCallbackNotificationPending = true;
    this.buttonCallback = buttonCallback;
    this.buttonWindow = buttonWindow;
    this.buttonName = buttonName;
    scriptProcessor(script, null, SCRIPT_NOWAIT);
  }

  final static int SCRIPT_CHECK = 0;
  final static int SCRIPT_WAIT = 1;
  final static int SCRIPT_NOWAIT = 2;
  
  
  private String scriptProcessor(String script, String statusParams, int processType) {
    /*
     * Idea here is to provide a single point of entry
     * Synchronization does not work, because it is possible for the NOWAIT variety of
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
    return scriptProcessor(script, null, SCRIPT_WAIT);
  }   
  
  public String scriptWait(String script, String statusParams) {
    if (script == null || script.length() == 0)
      return "";
    return scriptProcessor(script, statusParams, SCRIPT_WAIT);
  }   

  // not implemented:
  synchronized public void syncScript(String script) {
    if (script.equalsIgnoreCase("on")) {
      myStatusListener.sendSyncScript("SLAVE",null);
      viewer.setSyncDriver(1);
      return;
    }
    if (script.equalsIgnoreCase("off")) {
      viewer.setSyncDriver(0);
      return;
    }
    if (script.equalsIgnoreCase("slave")) {
      viewer.setSyncDriver(-1);
      return;
    }
    if (script.equalsIgnoreCase("sync")) {
      viewer.setSyncDriver(2);
      return;
    }
    if (viewer.getSyncMode() != 0)
      return;
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug(htmlName + " syncing with script: " + script);
    }
    script(script);
  }
  
  public String getAppletInfo() {
    return GT
        ._(
            "Jmol Applet version {0} {1}.\n\nAn OpenScience project.\n\nSee http://www.jmol.org for more information",
            new Object[] { JmolConstants.version, JmolConstants.date });
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

  public void loadInline(String strModel) {
    if (strModel == null)
      return;
    viewer.loadInline(strModel, (char) 0);
  }
  
  public void loadInline(String[] strModels) {
    loadInline(strModels, "");
  }
  
  public void loadInline(String strModel, String script) {
    loadInline(strModel);
    script(script);
  }

  public void loadInline(String[] strModels, String script) {
    if (strModels == null || strModels.length == 0)
      return;
    viewer.loadInline(strModels);
    script(script);
  }
  
  private void loadInlineSeparated(String strModel, String script) {
    if (strModel == null)
      return;
    viewer.loadInline(strModel);
    script(script);
  }
  
  public void loadDOMNode(JSObject DOMNode) {
    // This should provide a route to pass in a browser DOM node
    // directly as a JSObject. Unfortunately does not seem to work with
    // current browsers
    viewer.openDOM(DOMNode);
  }

  public void loadNodeId(String nodeId) {
    if (!haveDocumentAccess)
      return;
    if (nodeId != null) {
      // Retrieve Node ...
      // First try to find by ID
      Object[] idArgs = { nodeId };
      JSObject tryNode = (JSObject) jsoDocument.call("getElementById", idArgs);

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
          }
        }
      }
      if (tryNode != null)
        loadDOMNode(tryNode);
    }
  }

  void loadPopupMenuAsBackgroundTask() {
    // no popup on MacOS 9 NetScape
    if (viewer.getOperatingSystemName().equals("Mac OS")
        && viewer.getJavaVersion().equals("1.1.5"))
      return;
    new Thread(new LoadPopupThread()).start();
  }

  class LoadPopupThread implements Runnable {

    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      // long beginTime = System.currentTimeMillis();
      // Logger.debug("LoadPopupThread starting ");
      // this is a background task
      jmolpopup = JmolPopup.newJmolPopup(viewer, doTranslate);
    }
  }

  class MyStatusListener implements JmolStatusListener {

    public String eval(String strEval) {
      try {
        if(!haveDocumentAccess || ((Boolean)jsoDocument.eval("!!_jmol.noEval")).booleanValue())
          return "NO EVAL ALLOWED";
      } catch (Exception e) {
        Logger.error("# no _jmol in evaluating " + strEval + ":" + e.toString());
        return "";
      }
      try {
        return "" + jsoDocument.eval(strEval);
      } catch (Exception e) {
        Logger.error("# error evaluating " + strEval + ":" + e.toString());
      }
      return "";
    }
    
    public void createImage(String file, String type_or_text, int quality) {
      if (quality == Integer.MAX_VALUE)
        consoleMessage(type_or_text);
      // application-only if not text 
    }
    
    public void notifyFileLoaded(String fullPathName, String fileName,
                                 String modelName, Object clientFile,
                                 String errorMsg) {
      if (errorMsg != null) {
        showStatusAndConsole(GT._("File Error:") + errorMsg);
        return;
      }
      if (!mayScript || loadStructCallback == null || fullPathName == null)
        return;
      try {
        if (loadStructCallback.equals("alert"))
          jsoWindow.call(loadStructCallback, new Object[] { fullPathName });
        else
          jsoWindow.call(loadStructCallback, new Object[] { htmlName,
              fullPathName });
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug("loadStructCallback call error to "
                + loadStructCallback + ": " + e);
          }
        haveNotifiedError = true;
      }
    }

    public void notifyScriptStart(String statusMessage, String additionalInfo) {
      if (!mayScript || messageCallback == null)
        return;
      try {
        if (messageCallback.equals("alert"))
          jsoWindow.call(messageCallback, new Object[] { statusMessage + " ; "
              + additionalInfo });
        else
          jsoWindow.call(messageCallback, new Object[] { htmlName,
              statusMessage, additionalInfo });
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug("messageCallback call error to " + messageCallback
                + ": " + e);
          }
        haveNotifiedError = true;
      }

      showStatusAndConsole(statusMessage);
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
        if (nX > 0 && nY > 0) {    // fill with individual function calls (slow)
          for (int i = 0; i < nX; i++)
            for (int j = 0; j < nY; j++) {
              fxy[i][j] = ((Double) jsoWindow.call(functionName, new Object[] {
                  htmlName, new Integer(i), new Integer(j) })).floatValue();
            }
        } else if (nY > 0){       // fill with parsed values from a string (pretty fast)
          String data =  (String) jsoWindow.call(functionName, new Object[] {
              htmlName, new Integer(nX), new Integer(nY) });
          System.out.println(data);
          nX = Math.abs(nX);
          float[] fdata = new float[nX * nY]; 
          Parser.parseFloatArray(data, null, fdata);
          for (int i = 0, ipt = 0; i < nX; i++) {
            for (int j = 0; j < nY; j++, ipt++) {
              fxy[i][j] = fdata[ipt];
            }
          }
        } else {                 // fill float[][] directly using JavaScript
          jsoWindow.call(functionName,
              new Object[] { htmlName, new Integer(nX), new Integer(nY), fxy });
        }
      } catch (Exception e) {
        Logger.error("Exception " + e.getMessage() + " with nX, nY: "+ nX + " " + nY);
      }
      for (int i = 0; i < nX; i++)
        for (int j = 0; j < nY; j++) 
          System.out.println("i j fxy " + i + " " + j + " " + fxy[i][j]);
      return fxy;
    }
    
    public void notifyNewPickingModeMeasurement(int iatom, String strMeasure) {
      sendConsoleMessage(strMeasure);
    }

    public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
      //shows pending, etc. -- ok for an overwrite, not for a listing
      showStatusAndConsole(strInfo);
    }

    public void notifyResized(int newWidth, int newHeight) {
      if (!mayScript || resizeCallback == null)
        return;
      try {
          jsoWindow.call(resizeCallback, new Object[] { htmlName,
              new Integer(newWidth), new Integer(newHeight)});
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug(
                "resizeCallback call error to " + resizeCallback + ": " + e);
          }
          haveNotifiedError = true;
      }
    }

    public void notifyFrameChanged(int frameNo, int fileNo, int modelNo,
                                   int firstNo, int lastNo) {
      boolean isAnimationRunning = (frameNo <= -2);
      if (mayScript && animFrameCallback != null) {
        try {
          jsoWindow.call(animFrameCallback, new Object[] { htmlName,
              new Integer(Math.max(frameNo, -2 - frameNo)),
              new Integer(fileNo), new Integer(modelNo), new Integer(firstNo),
              new Integer(lastNo) });
        } catch (Exception e) {
          if (!haveNotifiedError)
            if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
              Logger.debug("animFrameCallback call error to "
                  + animFrameCallback + ": " + e);
            }
          haveNotifiedError = true;
        }
      }
      if (jmolpopup == null || isAnimationRunning)
        return;
      jmolpopup.updateComputedMenus();
    }

    public void notifyAtomPicked(int atomIndex, String strInfo) {
      showStatusAndConsole(strInfo);
      if (!mayScript || pickCallback == null)
        return;
      try {
        if (pickCallback.equals("alert"))
          jsoWindow.call(pickCallback, new Object[] { strInfo });
        else
          jsoWindow.call(pickCallback, new Object[] { htmlName, strInfo,
              new Integer(atomIndex) });
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug("pickCallback call error to " + pickCallback + ": "
                + e);
          }
        haveNotifiedError = true;
      }
    }

    public void notifyAtomHovered(int atomIndex, String strInfo) {
      if (!mayScript || hoverCallback == null)
        return;
      try {
        if (hoverCallback.equals("alert"))
          jsoWindow.call(hoverCallback, new Object[] { strInfo });
        else
          jsoWindow.call(hoverCallback, new Object[] { htmlName, strInfo,
              new Integer(atomIndex) });
      } catch (Exception e) {
        if (!haveNotifiedError)
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug("hoverCallback call error to " + hoverCallback + ": "
                + e);
          }
        haveNotifiedError = true;
      }
    }
    
    public void notifyScriptTermination(String errorMessage, int msWalltime) {
      showStatusAndConsole(GT._(errorMessage));
      if (buttonCallbackNotificationPending) {
        if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
          Logger.debug("!!!! calling back " + buttonCallback);
        }
        buttonCallbackAfter[0] = buttonName;
        buttonWindow.call(buttonCallback, buttonCallbackAfter);
      }
    }

    public void sendConsoleEcho(String strEcho) {
      sendConsoleMessage(strEcho);
    }

    public void sendConsoleMessage(String strMsg) {
      sendMessageCallback(strMsg);
      consoleMessage(strMsg);
    }

    public void setCallbackFunction(String callbackType, String callbackFunction) {
      //also serves to change language for callbacks and menu
      if (callbackType.equalsIgnoreCase("language")) {
        new GT(callbackFunction);
        language = GT.getLanguage();
        if (needPopupMenu)
          jmolpopup = JmolPopup.newJmolPopup(viewer, doTranslate);  
        return;
      }
      if (callbackType.equalsIgnoreCase("AnimFrameCallback"))
        animFrameCallback = callbackFunction;
      else if (callbackType.equalsIgnoreCase("HoverCallback"))
        hoverCallback = callbackFunction;
      else if (callbackType.equalsIgnoreCase("LoadStructCallback"))
        loadStructCallback = callbackFunction;
      else if (callbackType.equalsIgnoreCase("MessageCallback"))
        messageCallback = callbackFunction;
      else if (callbackType.equalsIgnoreCase("PickCallback"))
        pickCallback = callbackFunction;
      else if (callbackType.equalsIgnoreCase("ResizeCallback"))
        resizeCallback = callbackFunction;
      else
        sendConsoleMessage("Available callbacks include: AnimFrameCallback, HoverCallback, LoadStructCallback, MessageCallback, PickCallback, and ResizeCallback");
    }
    
    public void handlePopupMenu(int x, int y) {
      if (jmolpopup == null)
        return;
      if (!language.equals(GT.getLanguage())) {
        jmolpopup = JmolPopup.newJmolPopup(viewer, doTranslate);
        language = GT.getLanguage();
      }
      jmolpopup.show(x, y);
    }

    public void showUrl(String urlString) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug("showUrl(" + urlString + ")");
      }
      if (urlString != null && urlString.length() > 0) {
        try {
          URL url = new URL(urlString);
          appletWrapper.getAppletContext().showDocument(url, "_blank");
        } catch (MalformedURLException mue) {
          showStatusAndConsole("Malformed URL:" + urlString);
        }
      }
    }

    public void showConsole(boolean showConsole) {
      //Logger.info("JmolApplet.showConsole(" + showConsole + ")");
      if (jvm12 != null)
        jvm12.showConsole(showConsole);
    }
  
    public void sendSyncScript(String script, String appletName) {
      //how to get rid of this warning? - RMH
      Hashtable h = JmolAppletRegistry.htRegistry;
      Enumeration keys = h.keys();
      while (keys.hasMoreElements()) {
        String theApplet = (String)keys.nextElement();
        if (! theApplet.equals(htmlName) &&
            (appletName == null || appletName == theApplet)) {
          if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
            Logger.debug("sendSyncScript class "+h.get(theApplet).getClass().getName());
          }
          JmolAppletInterface app = (JmolAppletInterface)(h.get(theApplet));
          try {
            if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
              Logger.debug(htmlName + " sending " + script + " to " + theApplet);
            }
            app.syncScript(script);
          } catch (Exception e) {
            Logger.debug(htmlName + " couldn't send " + script + " to " + theApplet + ": " + e);
          }
        }
      }
    }
    
  }

}
