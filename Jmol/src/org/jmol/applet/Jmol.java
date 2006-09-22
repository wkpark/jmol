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

/*
 * these are *required*
 * 
 * [param name="progressbar" value="true" /] [param name="progresscolor"
 * value="blue" /] [param name="boxmessage" value="your-favorite-message" /]
 * [param name="boxbgcolor" value="#112233" /] [param name="boxfgcolor"
 * value="#778899" /]
 * 
 * [param name="loadInline" value=" | do | it | this | way " /]
 * 
 * [param name="script" value="your-script" /]
 *  // this one flips the orientation and uses RasMol/Chime colors [param
 * name="emulate" value="chime" /]
 *  // this is *required* if you want the applet to be able to // call your
 * callbacks
 * 
 * mayscript="true" is required as an applet tag (for true callbacks only)
 * but this is being taken over by setTimeout() polling using getPropertyAsString()
 * and getPropertyAsJSON() [JavaScript Object Notation]
 * 
 * [param name="AnimFrameCallback" value="yourJavaScriptMethodName" /] [param
 * name="LoadStructCallback" value="yourJavaScriptMethodName" /] [param
 * name="MessageCallback" value="yourJavaScriptMethodName" /] [param
 * name="HoverCallback" value="yourJavaScriptMethodName" /] [param
 * name="PickCallback" value="yourJavaScriptMethodName" /]
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

  String animFrameCallback;

  String loadStructCallback;

  String messageCallback;

  // ??? String pauseCallback;
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

  public void init() {
    htmlName = getParameter("name");
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

  public void initWindows() {

    // to enable CDK
    // viewer = new JmolViewer(this, new CdkJmolAdapter(null));
    viewer = JmolViewer.allocateViewer(appletWrapper, new SmarterJmolAdapter(
        null));
    myStatusListener = new MyStatusListener();
    viewer.setJmolStatusListener(myStatusListener);

    viewer.setAppletContext(htmlName, appletWrapper.getDocumentBase(), appletWrapper
        .getCodeBase(), getValue("JmolAppletProxy", null));

    jvm12orGreater = viewer.isJvm12orGreater();
    if (jvm12orGreater)
      jvm12 = new Jvm12(appletWrapper, viewer);
    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(appletWrapper);
        if (jsoWindow == null)
          Logger.error("jsoWindow returned null ... no JavaScript callbacks :-(");
        jsoDocument = (JSObject) jsoWindow.getMember("document");
        if (jsoDocument == null)
          Logger.error("jsoDocument returned null ... no DOM manipulations :-(");
      } catch (Exception e) {
        Logger.error("" + e);
      }
    }
  }

  void setLogging(){
    String logLevel = getValue("logLevel", "") + "4";
    for (int i = 0; i < Logger.NB_LEVELS; i++)
      Logger.setActiveLevel(i, false);
    if (logLevel.charAt(0) > '5')
      logLevel = "5";
    if (logLevel.charAt(0)!='4')
      System.out.println("setting logLevel="+logLevel.charAt(0)+" -- To change, use script \"set logLevel [0-5]\"");
    switch (logLevel.charAt(0)) {
    case '5':
      Logger.setActiveLevel(Logger.LEVEL_DEBUG, true);
    case '4':
      Logger.setActiveLevel(Logger.LEVEL_INFO, true);
    case '3':
      Logger.setActiveLevel(Logger.LEVEL_WARN, true);
    case '2':
      Logger.setActiveLevel(Logger.LEVEL_ERROR, true);
    case '1':
      Logger.setActiveLevel(Logger.LEVEL_FATAL, true);
    }
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

  public void initApplication() {
    viewer.pushHoldRepaint();
    {
      // REQUIRE that the progressbar be shown
      hasProgressBar = getBooleanValue("progressbar", false);
      // should the popupMenu be loaded ?
      boolean popupMenu = getBooleanValue("popupMenu", true);
      if (popupMenu)
        loadPopupMenuAsBackgroundTask();

      emulate = getValueLowerCase("emulate", "jmol");
      if (emulate.equals("chime")) {
        viewer.setRasmolDefaults();
      } else {
        viewer.setJmolDefaults();
      }
      String bgcolor = getValue("boxbgcolor", "black");
      bgcolor = getValue("bgcolor", bgcolor);
      viewer.setColorBackground(bgcolor);

      // loadInline(getValue("loadInline", null));
      loadNodeId(getValue("loadNodeId", null));

      viewer.setFrankOn(true);
      animFrameCallback = getValue("AnimFrameCallback", null);
      loadStructCallback = getValue("LoadStructCallback", null);
      messageCallback = getValue("MessageCallback", null);
      // pauseCallback = getValue("PauseCallback", null);
      pickCallback = getValue("PickCallback", null);
      hoverCallback = getValue("HoverCallback", null);

      statusForm = getValue("StatusForm", null);
      statusText = getValue("StatusText", null); //text
      statusTextarea = getValue("StatusTextarea", null); //textarea

      if (animFrameCallback != null)
        Logger.info("animFrameCallback=" + animFrameCallback);
      if (loadStructCallback != null)
        Logger.info("loadStructCallback=" + loadStructCallback);
      if (messageCallback != null)
        Logger.info("messageCallback=" + messageCallback);
      if (pickCallback != null)
        Logger.info("pickCallback=" + pickCallback);
      if (hoverCallback != null)
        Logger.info("hoverCallback=" + hoverCallback);
      if (statusForm != null && statusText != null) {
        Logger.info("applet text status will be reported to document."
            + statusForm + "." + statusText);
      }
      if (statusForm != null && statusTextarea != null) {
        Logger.info("applet textarea status will be reported to document."
            + statusForm + "." + statusTextarea);
      }
      if (!mayScript
          && (animFrameCallback != null || loadStructCallback != null
              || messageCallback != null ||
          // pauseCallback != null ||
          pickCallback != null))
        Logger.warn("WARNING!! MAYSCRIPT not found");

      String loadParam;
      String scriptParam = getValue("script", "");
      if ((loadParam = getValue("loadInline", null)) != null)
        loadInline(loadParam, (scriptParam.length() > 0 ? scriptParam : null));
      else {
        if ((loadParam = getValue("load", null)) != null)
          scriptParam = "load " + loadParam + ";" + scriptParam;
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
    if (messageCallback != null && jsoWindow != null)
      if (messageCallback.equals("alert"))
        jsoWindow.call(messageCallback, new Object[] { strMsg });
      else
        jsoWindow.call(messageCallback, new Object[] { htmlName, strMsg });
  }
  
  void sendJsTextStatus(String message) {
    if (statusForm == null || statusText == null)
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
    if (statusForm == null || statusTextarea == null)
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

  public void update(Graphics g) {
    if (viewer == null) // it seems that this can happen at startup sometimes
      return;
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
      viewer.renderScreenImage(g, size, rectClip);
    }

    if (showPaintTime) {
      stopPaintClock();
      showTimes(10, 10, g);
    }
  }

  final static String[] progressbarMsgs = { "Jmol developer alert!", "",
      "progressbar is REQUIRED ... otherwise users",
      "will have no indicate that the applet is loading", "",
      "<applet code='JmolApplet' ... >",
      "  <param name='progressbar' value='true' />",
      "  <param name='progresscolor' value='blue' />",
      "  <param name='boxmessage' value='your-favorite-message' />",
      "  <param name='boxbgcolor' value='#112233' />",
      "  <param name='boxfgcolor' value='#778899' />", "   ...", "</applet>", };

  void printProgressbarMessage(Graphics g) {
    g.setColor(Color.yellow);
    g.fillRect(0, 0, 10000, 10000);
    g.setColor(Color.black);
    for (int i = 0, y = 13; i < progressbarMsgs.length; ++i, y += 13) {
      g.drawString(progressbarMsgs[i], 10, y);
    }
  }

  public boolean showPaintTime = false;

  public void paint(Graphics g) {
    update(g);
  }

  public boolean handleEvent(Event e) {
    if (viewer == null)
      return false;
    return viewer.handleOldJvm10Event(e);
  }

  // code to record last and average times
  // last and average of all the previous times are shown in the status window

  static int timeLast = 0;

  static int timeCount;

  static int timeTotal;

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
    Logger.info(htmlName + " JmolApplet.scriptButton(" + buttonWindow
        + "," + buttonName + "," + script + "," + buttonCallback);
    if (buttonWindow != null && buttonCallback != null) {
      Logger.debug("!!!! calling back " + buttonCallback);
      buttonCallbackBefore[0] = buttonName;
      Logger.debug("trying...");
      buttonWindow.call(buttonCallback, buttonCallbackBefore);
      Logger.debug("made it");

      buttonCallbackNotificationPending = true;
      this.buttonCallback = buttonCallback;
      this.buttonWindow = buttonWindow;
      this.buttonName = buttonName;
    } else {
      buttonCallbackNotificationPending = false;
    }
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
    switch (processType) {
    case SCRIPT_CHECK:
      return viewer.scriptCheck(script);
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
    scriptProcessor(script, null, SCRIPT_NOWAIT);
  }   
  
  public String scriptCheck(String script) {
    return scriptProcessor(script, null, SCRIPT_CHECK);
  }   
  
  public String scriptNoWait(String script) {
    return scriptProcessor(script, null, SCRIPT_NOWAIT);
  }   
  
  public String scriptWait(String script) {
    return scriptProcessor(script, null, SCRIPT_WAIT);
  }   
  
  public String scriptWait(String script, String statusParams) {
    return scriptProcessor(script, statusParams, SCRIPT_WAIT);
  }   

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
    Logger.debug(htmlName + " syncing with script: " + script);
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
    return viewer.getProperty("String", infoType, "").toString();
  }
  public String getPropertyAsString(String infoType, String paramInfo) {
    return viewer.getProperty("String",infoType, paramInfo).toString();
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
    viewer.loadInline(strModel);
  }
  
  public void loadInline(String strModel, String script) {
    loadInline(strModel);
    script(script);
  }

  public void loadInline(String[] arrayModel, String script) {
    // not implemented
  }
  
  public void loadDOMNode(JSObject DOMNode) {
    // This should provide a route to pass in a browser DOM node
    // directly as a JSObject. Unfortunately does not seem to work with
    // current browsers
    viewer.openDOM(DOMNode);
  }

  public void loadNodeId(String nodeId) {
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
      JmolPopup popup;
      try {
        popup = JmolPopup.newJmolPopup(viewer);
      } catch (Exception e) {
        Logger.error("JmolPopup not loaded");
        return;
      }
      if (viewer.haveFrame())
        popup.updateComputedMenus();
      jmolpopup = popup;
      // long runTime = System.currentTimeMillis() - beginTime;
      // Logger.debug("LoadPopupThread finished " + runTime + " ms");
    }
  }

  class MyStatusListener implements JmolStatusListener {

    public void notifyFileLoaded(String fullPathName, String fileName,
                                 String modelName, Object clientFile,
                                 String errorMsg) {
      if (errorMsg != null) {
        showStatusAndConsole(GT._("File Error:") + errorMsg);
        return;
      }
      if (fullPathName != null)
        if (loadStructCallback != null && jsoWindow != null)
          if (loadStructCallback.equals("alert"))
            jsoWindow.call(loadStructCallback, new Object[] { fullPathName });
          else
            jsoWindow.call(loadStructCallback, new Object[] { htmlName,
                fullPathName });
      if (jmolpopup != null)
        jmolpopup.updateComputedMenus();
    }

    public void notifyScriptStart(String statusMessage, String additionalInfo) {
      if (messageCallback != null && jsoWindow != null)
        if (messageCallback.equals("alert"))
          jsoWindow.call(messageCallback, new Object[] { statusMessage + " ; "
              + additionalInfo });
        else
          jsoWindow.call(messageCallback, new Object[] { htmlName,
              statusMessage, additionalInfo });
      showStatusAndConsole(statusMessage);
    }

    public float functionXY(String functionName, int x, int y) {
      return ((Double) jsoWindow.call(functionName, new Object[] { htmlName, new Integer(x), new Integer(y)})).floatValue();
    }
    
    public void notifyNewPickingModeMeasurement(int iatom, String strMeasure) {
      sendConsoleMessage(strMeasure);
    }

    public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
      //shows pending, etc. -- ok for an overwrite, not for a listing
      showStatusAndConsole(strInfo);
    }

    public void notifyFrameChanged(int frameNo) {
      if (animFrameCallback != null && jsoWindow != null)
        jsoWindow.call(animFrameCallback, new Object[] { htmlName,
            new Integer(frameNo) });
    }

    public void notifyAtomPicked(int atomIndex, String strInfo) {
      showStatusAndConsole(strInfo);
      if (pickCallback != null && jsoWindow != null)
        if (pickCallback.equals("alert"))
          jsoWindow.call(pickCallback, new Object[] { strInfo });
        else
          jsoWindow.call(pickCallback, new Object[] { htmlName, strInfo,
              new Integer(atomIndex) });
    }

    public void notifyAtomHovered(int atomIndex, String strInfo) {
      if (hoverCallback != null && jsoWindow != null)
        if (hoverCallback.equals("alert"))
          jsoWindow.call(hoverCallback, new Object[] { strInfo });
        else
          jsoWindow.call(hoverCallback, new Object[] { htmlName, strInfo,
              new Integer(atomIndex) });
    }
    
    public void notifyScriptTermination(String errorMessage, int msWalltime) {
      showStatusAndConsole(GT._(errorMessage));
      if (buttonCallbackNotificationPending) {
        Logger.debug("!!!! calling back " + buttonCallback);
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
      else
        sendConsoleMessage("Available callbacks include: AnimFrameCallback, HoverCallback, LoadStructCallback, MessageCallback, and PickCallback");
    }
    
    public void handlePopupMenu(int x, int y) {
      if (jmolpopup != null)
        jmolpopup.show(x, y);
    }

    public void showUrl(String urlString) {
      Logger.debug("showUrl(" + urlString + ")");
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
          Logger.debug("sendSyncScript class "+h.get(theApplet).getClass().getName());
          JmolAppletInterface app = (JmolAppletInterface)(h.get(theApplet));
          try {
            Logger.debug(htmlName + " sending " + script + " to " + theApplet);
            app.syncScript(script);
          } catch (Exception e) {
            Logger.debug(htmlName + " couldn't send " + script + " to " + theApplet + ": " + e);
          }
        }
      }
    }
    
  }

}
