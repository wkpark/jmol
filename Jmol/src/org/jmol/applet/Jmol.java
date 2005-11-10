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
//import org.openscience.jmol.adapters.CdkJmolAdapter;
import org.openscience.jmol.ui.JmolPopup;
import org.jmol.util.GT;

import netscape.javascript.JSObject;

import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;

/*
  these are *required*

   [param name="progressbar" value="true" /]
   [param name="progresscolor" value="blue" /]
   [param name="boxmessage" value="your-favorite-message" /]
   [param name="boxbgcolor" value="#112233" /]
   [param name="boxfgcolor" value="#778899" /]  

   [param name="loadInline" value="
| do
| it
| this
| way
" /]

   [param name="script"             value="your-script" /]

   // this one flips the orientation and uses RasMol/Chime colors
   [param name="emulate"    value="chime" /]

   // this is *required* if you want the applet to be able to
   // call your callbacks

   mayscript="true" is required as an applet tag

   [param name="AnimFrameCallback"  value="yourJavaScriptMethodName" /]
   [param name="LoadStructCallback" value="yourJavaScriptMethodName" /]
   [param name="MessageCallback"    value="yourJavaScriptMethodName" /]
   [param name="PauseCallback"      value="yourJavaScriptMethodName" /]
   [param name="PickCallback"       value="yourJavaScriptMethodName" /]

*/

public class Jmol implements WrappedApplet, JmolAppletInterface {

  JmolViewer viewer;
  boolean jvm12orGreater;
  String emulate;
  Jvm12 jvm12;
  JmolPopup jmolpopup;
  String htmlName;
  JmolAppletRegistry appletRegistry;

  MyStatusListener myStatusListener;

  AppletWrapper appletWrapper;

  /*
   * miguel 2004 11 29
   *
   * WARNING! DANGER!
   *
   * I have discovered that if you call JSObject.getWindow().toString()
   * on Safari v125.1 / Java 1.4.2_03 then it breaks or kills Safari
   * I filed Apple bug report #3897879
   *
   * Therefore, do *not* call System.out.println("" + jsoWindow);
   */
  JSObject jsoWindow;
  JSObject jsoDocument;

  boolean mayScript;
  String animFrameCallback;
  String loadStructCallback;
  String messageCallback;
  String pauseCallback;
  String pickCallback;

  final static boolean REQUIRE_PROGRESSBAR = true;
  boolean hasProgressBar;
  int paintCounter;

  public String getAppletInfo() {
    return appletInfo;
  }

  static String appletInfo =
    GT._("Jmol Applet.  Part of the OpenScience project. " +
         "See http://www.jmol.org for more information");

  public void setAppletWrapper(AppletWrapper appletWrapper) {
    this.appletWrapper = appletWrapper;
  }
  
  public void init() {
    htmlName = getParameter("name");
    String ms = getParameter("mayscript");
    mayScript = (ms != null) && (! ms.equalsIgnoreCase("false"));
    appletRegistry =
      new JmolAppletRegistry(htmlName, mayScript, appletWrapper);

    initWindows();
    initApplication();
  }

  String getParameter(String paramName) {
    return appletWrapper.getParameter(paramName);
  }
  
  public void initWindows() {

    // to enable CDK
    //    viewer = new JmolViewer(this, new CdkJmolAdapter(null));
    viewer =
      JmolViewer.allocateViewer(appletWrapper, new SmarterJmolAdapter(null));
    myStatusListener = new MyStatusListener();
    viewer.setJmolStatusListener(myStatusListener);

    viewer.setAppletContext(appletWrapper.getDocumentBase(),
                            appletWrapper.getCodeBase(),
                            getValue("JmolAppletProxy", null));

    jvm12orGreater = viewer.isJvm12orGreater();
    if (jvm12orGreater)
      jvm12 = new Jvm12(appletWrapper, viewer);

    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(appletWrapper);
        if (jsoWindow == null)
          System.out.println("jsoWindow returned null ... no JavaScript callbacks :-(");
        jsoDocument = (JSObject) jsoWindow.getMember("document");
        if (jsoDocument == null)
          System.out.println("jsoDocument returned null ... no DOM manipulations :-(");
      } catch (Exception e) {
        System.out.println("" + e);
      }
    }
  }

  /*
  PropertyResourceBundle appletProperties = null;

  private void loadProperties() {
    URL codeBase = getCodeBase();
    try {
      URL urlProperties = new URL(codeBase, "JmolApplet.properties");
      appletProperties =
        new PropertyResourceBundle(urlProperties.openStream());
    } catch (Exception ex) {
      System.out.println("JmolApplet.loadProperties() -> " + ex);
    }
  }
  */

  boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") ||
            value.equalsIgnoreCase("on") ||
            value.equalsIgnoreCase("yes"));
  }

  String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    if (stringValue != null)
      return stringValue;
    /*
    if (appletProperties != null) {
      try {
        stringValue = appletProperties.getString(propertyName);
        return stringValue;
      } catch (MissingResourceException ex) {
      }
    }
    */
    return defaultValue;
  }
  /*
  private int getValue(String propertyName, int defaultValue) {
    String stringValue = getValue(propertyName, null);
    if (stringValue != null)
      try {
        return Integer.parseInt(stringValue);
      } catch (NumberFormatException ex) {
        System.out.println(propertyName + ":" +
                           stringValue + " is not an integer");
      }
    return defaultValue;
  }
  
  private double getValue(String propertyName, double defaultValue) {
    String stringValue = getValue(propertyName, null);
    if (stringValue != null)
      try {
        return (new Double(stringValue)).doubleValue();
      } catch (NumberFormatException ex) {
        System.out.println(propertyName + ":" +
                           stringValue + " is not a double");
      }
    return defaultValue;
  }
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
      
      loadInline(getValue("loadInline", null));
      loadNodeId(getValue("loadNodeId", null));

      viewer.setFrankOn(true);

      animFrameCallback = getValue("AnimFrameCallback", null);
      loadStructCallback = getValue("LoadStructCallback", null);
      messageCallback = getValue("MessageCallback", null);
      pauseCallback = getValue("PauseCallback", null);
      pickCallback = getValue("PickCallback", null);
      if (! mayScript &&
          (animFrameCallback != null ||
           loadStructCallback != null ||
           messageCallback != null ||
           pauseCallback != null ||
           pickCallback != null))
        System.out.println("WARNING!! MAYSCRIPT not found");
      
      String scriptParam = getValue("script", "");
      String loadParam = getValue("load", null);
      if (loadParam != null)
        scriptParam = "load " + loadParam + ";" + scriptParam;
      script(scriptParam);
    }
    viewer.popHoldRepaint();
  }

  void showStatusAndConsole(String message) {
    appletWrapper.showStatus(message);
    consoleMessage(message);
  }

  void consoleMessage(String message) {
    if (jvm12 != null)
      jvm12.consoleMessage(message);
  }

  public void update(Graphics g) {
    //    System.out.println("update called");
    if (viewer == null) // it seems that this can happen at startup sometimes
      return;
    if (showPaintTime)
      startPaintClock();
    Dimension size = jvm12orGreater ? jvm12.getSize() : appletWrapper.size();
    viewer.setScreenDimension(size);
    Rectangle rectClip =
      jvm12orGreater ? jvm12.getClipBounds(g) : g.getClipRect();
    ++paintCounter;
    if (REQUIRE_PROGRESSBAR &&
        !hasProgressBar &&
        paintCounter < 30 &&
        (paintCounter & 1) == 0) {
      printProgressbarMessage(g);
      viewer.notifyRepainted();
    } else {
      viewer.renderScreenImage(g, size, rectClip);
    }

    if (showPaintTime) {
      stopPaintClock();
      showTimes(10, 10, g);
    }
  }

  final static String[] progressbarMsgs = {
    "Jmol developer alert!",
    "",
    "progressbar is REQUIRED ... otherwise users",
    "will have no indicate that the applet is loading",
    "",
    "<applet code='JmolApplet' ... >",
    "  <param name='progressbar' value='true' />",
    "  <param name='progresscolor' value='blue' />",
    "  <param name='boxmessage' value='your-favorite-message' />",
    "  <param name='boxbgcolor' value='#112233' />",
    "  <param name='boxfgcolor' value='#778899' />",
    "   ...",
    "</applet>",
  };

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
    //    System.out.println("paint called");
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
    int time = (int)(System.currentTimeMillis() - timeBegin);
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
    int timeAverage =
      (timeCount == 0)
      ? -1
      : (timeTotal + timeCount/2) / timeCount; // round, don't truncate
    g.setColor(Color.green);
    g.drawString(fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms", x, y);
  }

  final Object[] buttonCallbackBefore = { null, new Boolean(false)};
  final Object[] buttonCallbackAfter = { null, new Boolean(true)};

  boolean buttonCallbackNotificationPending;
  String buttonCallback;
  String buttonName;
  JSObject buttonWindow;

  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    System.out.println(htmlName +" JmolApplet.scriptButton(" +
                       buttonWindow + "," + buttonName + "," +
                       script + "," + buttonCallback);
    if (buttonWindow != null && buttonCallback != null) {
      System.out.println("!!!! calling back " + buttonCallback);
      buttonCallbackBefore[0] = buttonName;
      System.out.println("trying...");
      buttonWindow.call(buttonCallback, buttonCallbackBefore);
      System.out.println("made it");

      buttonCallbackNotificationPending = true;
      this.buttonCallback = buttonCallback;
      this.buttonWindow = buttonWindow;
      this.buttonName = buttonName;
    } else {
      buttonCallbackNotificationPending = false;
    }
    script(script);
  }

  public void script(String script) {
    String strError = viewer.evalString(script);
    if (strError == null)
      strError = GT._("Jmol executing script ...");
    myStatusListener.setStatusMessage(strError);
  }

  char inlineNewlineChar = '|';

  public void loadInline(String strModel) {
    if (strModel != null) {
      if (inlineNewlineChar != 0) {
        int len = strModel.length();
        int i;
        for (i = 0; i < len && strModel.charAt(0) == ' '; ++i) {
        }
        if (i < len && strModel.charAt(i) == inlineNewlineChar)
          strModel = strModel.substring(i + 1);
        strModel = strModel.replace(inlineNewlineChar, '\n');
      }
      viewer.openStringInline(strModel);
      myStatusListener.setStatusMessage(viewer.getOpenFileError());
    }
  }

  public void loadDOMNode(JSObject DOMNode) {
      // This should provide a route to pass in a browser DOM node
      // directly as a JSObject. Unfortunately does not seem to work with
      // current browsers
      viewer.openDOM(DOMNode);
      myStatusListener.setStatusMessage(viewer.getOpenFileError());
  } 

  public void loadNodeId(String nodeId) {
    if (nodeId != null) {
      // Retrieve Node ...
      // First try to find by ID
      Object[] idArgs = { nodeId };
      JSObject tryNode = (JSObject) jsoDocument.call("getElementById",idArgs);

      // But that relies on a well-formed CML DTD specifying ID search.
      // Otherwise, search all cml:cml nodes.
     if (tryNode == null) {
        Object[] searchArgs = { "http://www.xml-cml.org/schema/cml2/core", "cml" };
        JSObject tryNodeList = (JSObject) jsoDocument.call("getElementsByTagNameNS", searchArgs);
        if (tryNodeList != null) {
          for (int i = 0; i < ((Number) tryNodeList.getMember("length")).intValue(); i++ ) {
	    tryNode = (JSObject) tryNodeList.getSlot(i);
	    Object[] idArg = { "id" };
            String idValue = (String) tryNode.call("getAttribute", idArg);
	    if (nodeId.equals(idValue))
                break;
	  }
	}
      }
      if (tryNode != null) {
        viewer.openDOM(tryNode);
        myStatusListener.setStatusMessage(viewer.getOpenFileError());
      }
    } 
  }


  void loadPopupMenuAsBackgroundTask() {
    // no popup on MacOS 9 NetScape
    if (viewer.getOperatingSystemName().equals("Mac OS") &&
        viewer.getJavaVersion().equals("1.1.5"))
      return;
    new Thread(new LoadPopupThread()).start();
  }

  class LoadPopupThread implements Runnable {

    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      // long beginTime = System.currentTimeMillis();
      // System.out.println("LoadPopupThread starting ");
      // this is a background task
      JmolPopup popup;
      try {
        popup = JmolPopup.newJmolPopup(viewer);
      } catch (Exception e) {
        System.out.println("JmolPopup not loaded");
        return;
      }
      if (viewer.haveFrame())
        popup.updateComputedMenus();
      jmolpopup = popup;
      // long runTime = System.currentTimeMillis() - beginTime;
      // System.out.println("LoadPopupThread finished " + runTime + " ms");
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
          jsoWindow.call(loadStructCallback, new Object[] {htmlName});
      if (jmolpopup != null)
        jmolpopup.updateComputedMenus();
    }

    public void setStatusMessage(String statusMessage) {
      if (statusMessage == null)
        return;
      if (messageCallback != null && jsoWindow != null)
        jsoWindow.call(messageCallback, new Object[] {htmlName, statusMessage});
      showStatusAndConsole(statusMessage);
    }

    public void scriptEcho(String strEcho) {
      scriptStatus(strEcho);
    }

    public void scriptStatus(String strStatus) {
      if (strStatus != null && messageCallback != null && jsoWindow != null)
        jsoWindow.call(messageCallback, new Object[] {htmlName, strStatus});
      consoleMessage(strStatus);
    }

    public void notifyScriptTermination(String errorMessage, int msWalltime) {
      showStatusAndConsole(GT._("Jmol script completed"));
      if (buttonCallbackNotificationPending) {
        System.out.println("!!!! calling back " + buttonCallback);
        buttonCallbackAfter[0] = buttonName;
        buttonWindow.call(buttonCallback, buttonCallbackAfter);
      }
    }

    public void handlePopupMenu(int x, int y) {
      if (jmolpopup != null)
        jmolpopup.show(x, y);
    }

    public void measureSelection(int atomIndex) {
    }

    public void notifyMeasurementsChanged() {
    }

    public void notifyFrameChanged(int frameNo) {
      //System.out.println("notifyFrameChanged(" + frameNo +")");
      if (animFrameCallback != null && jsoWindow != null)
        jsoWindow.call(animFrameCallback,
                       new Object[] {htmlName, new Integer(frameNo)});
    }

    public void notifyAtomPicked(int atomIndex, String strInfo) {
      //System.out.println("notifyAtomPicked(" + atomIndex + "," + strInfo +")");
      showStatusAndConsole(strInfo);
      if (pickCallback != null && jsoWindow != null)
        jsoWindow.call(pickCallback,
                       new Object[] {htmlName, strInfo, new Integer(atomIndex)});
    }

    public void showUrl(String urlString) {
      System.out.println("showUrl(" + urlString + ")");
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
      System.out.println("JmolApplet.showConsole(" + showConsole + ")");
      if (jvm12 != null)
        jvm12.showConsole(showConsole);
    }

  }
}
