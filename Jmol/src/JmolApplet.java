/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

import org.openscience.jmol.applet.*;
import org.openscience.jmol.viewer.*;
import org.jmol.api.ModelAdapter;
import org.openscience.jmol.viewer.JmolStatusListener;
import org.jmol.adapter.smarter.SmarterModelAdapter;
//import org.openscience.jmol.adapters.CdkModelAdapter;
import org.openscience.jmol.ui.JmolPopup;

import netscape.javascript.JSObject;

import java.applet.*;
import java.awt.*;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.MissingResourceException;

/*
  these are *required*
  I may make them mandatory, otherwise the JmolApplet may not load :-)

   [param name="progressbar" value="true" /]
   [param name="progresscolor" value="blue" /]
   [param name="boxmessage" value="your-favorite-message" /]
   [param name="boxbgcolor" value="#112233" /]
   [param name="boxfgcolor" value="#778899" /]  

   // load should *never* have an http server name
   // it will fail unless the applet is signed
   // must be a path relative to the html page
   // can be absolute on the server ... starting with '/'
   [param name="load"       value="relative-pathname-with-no-http://" /]

   [param name="loadInline" value="
| do
| it
| this
| way
" /]

   [param name="script"             value="your-script" /]

   // this one flips the orientation and uses RasMol/Chime colors
   [param name="emulate"    value="chime" /]

   // I have thought about making this boxbgcolor ... what do you think?
   [param name="bgcolor"    value="#AABBCC" /]

   // the Jmol frank
   [param name="frank"      value="false" /]

   // if you don't give me a good reason to keep these two,
   // I am going to remove them
   [param name="vdwPercent" value="20" /]
   [param name="perspectiveDepth" value="true" /]

   // for verbose MessageCallback
   [param name="debugscript"      value="true" /]


   // this is *required* if you want the applet to be able to
   // call your callbacks

   mayscript="true" is required as an applet tag

   [param name="AnimFrameCallback"  value="yourJavaScriptMethodName" /]
   [param name="LoadStructCallback" value="yourJavaScriptMethodName" /]
   [param name="MessageCallback"    value="yourJavaScriptMethodName" /]
   [param name="PauseCallback"      value="yourJavaScriptMethodName" /]
   [param name="PickCallback"       value="yourJavaScriptMethodName" /]

*/

public class JmolApplet extends Applet implements JmolStatusListener {

  JmolViewer viewer;
  boolean jvm12orGreater;
  String emulate;
  Jvm12 jvm12;
  JmolPopup jmolpopup;
  String htmlName;
  JmolAppletRegistry appletRegistry;

  JSObject jsoWindow;

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

  private static String appletInfo =
    "Jmol Applet.  Part of the OpenScience project. " +
    "See jmol.sourceforge.net for more information";

  public void init() {
    htmlName = getParameter("name");
    String ms = getParameter("mayscript");
    mayScript = (ms != null) && (! ms.equalsIgnoreCase("false"));
    appletRegistry = new JmolAppletRegistry(htmlName, mayScript, this);

    loadProperties();
    initWindows();
    initApplication();
  }
  
  public void initWindows() {

    // to enable CDK
    //    viewer = new JmolViewer(this, new CdkModelAdapter(null));
    viewer = new JmolViewer(this, new SmarterModelAdapter(null));
    viewer.setJmolStatusListener(this);

    viewer.setAppletContext(getDocumentBase(), getCodeBase(),
                            getValue("JmolAppletProxy", null));

    jvm12orGreater = viewer.jvm12orGreater;
    if (jvm12orGreater)
      jvm12 = new Jvm12(this);

    if (mayScript) {
      try {
        jsoWindow = JSObject.getWindow(this);
      } catch (Exception e) {
        System.out.println("" + e);
      }
    }
  }
    
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

  private boolean getBooleanValue(String propertyName, boolean defaultValue) {
    String value = getValue(propertyName, defaultValue ? "true" : "");
    return (value.equalsIgnoreCase("true") ||
            value.equalsIgnoreCase("on") ||
            value.equalsIgnoreCase("yes"));
  }

  private String getValue(String propertyName, String defaultValue) {
    String stringValue = getParameter(propertyName);
    if (stringValue != null)
      return stringValue;
    if (appletProperties != null) {
      try {
        stringValue = appletProperties.getString(propertyName);
        return stringValue;
      } catch (MissingResourceException ex) {
      }
    }
    return defaultValue;
  }

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

      // should the popupMenu be loaded ?
      boolean popupMenu = getBooleanValue("popupMenu", true);
      if (popupMenu)
        loadPopupMenuAsBackgroundTask();

      emulate = getValueLowerCase("emulate", "jmol");
      if (emulate.equals("chime")) {
        viewer.setRasmolDefaults();
        viewer.setColorBackground(getValue("bgcolor", "black"));
        viewer.setPerspectiveDepth(getBooleanValue("perspectiveDepth", true));
      } else {
        viewer.setJmolDefaults();
        viewer.setPercentVdwAtom(getValue("vdwPercent", 20));
        viewer.setColorBackground(getValue("bgcolor", "black"));
        viewer.setWireframeRotation(getBooleanValue("wireframeRotation",
                                                    false));
        viewer.setPerspectiveDepth(getBooleanValue("perspectiveDepth", true));
      }

      viewer.setDebugScript(getBooleanValue("debugscript", false));
      
      load(getValue("load", null));
      loadInline(getValue("loadInline", null));
      if (getBooleanValue("frank", true)) {
        System.out.println("frank is ON");
        viewer.setShapeSize(JmolConstants.SHAPE_FRANK, -1);
      }

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
      
      script(getValue("script", null));
    }
    viewer.popHoldRepaint();
  }

  public void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile) {
    if (loadStructCallback != null && jsoWindow != null)
      jsoWindow.call(loadStructCallback, new Object[] {htmlName});
    if (jmolpopup != null)
      jmolpopup.updateComputedMenus();
  }

  public void notifyFileNotLoaded(String fullPathName, String errorMsg) {
    showStatus("File Error:" + errorMsg);
  }

  public void setStatusMessage(String statusMessage) {
    if (statusMessage == null)
      return;
    if (messageCallback != null && jsoWindow != null)
      jsoWindow.call(messageCallback, new Object[] {htmlName, statusMessage});
    else
      showStatus(statusMessage);
  }

  public void scriptEcho(String strEcho) {
  }

  public void scriptStatus(String strStatus) {
    if (strStatus != null && messageCallback != null && jsoWindow != null)
      jsoWindow.call(messageCallback, new Object[] {htmlName, strStatus});
  }

  boolean buttonCallbackNotificationPending;
  String buttonCallback;
  String buttonName;
  JSObject buttonWindow;

  public void notifyScriptTermination(String errorMessage, int msWalltime) {
    showStatus("Jmol script completed");
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
    System.out.println("notifyFrameChanged(" + frameNo +")");
    if (animFrameCallback != null && jsoWindow != null)
      jsoWindow.call(animFrameCallback,
                     new Object[] {htmlName, new Integer(frameNo)});
  }

  public void notifyAtomPicked(int atomIndex, String strInfo) {
    System.out.println("notifyAtomPicked(" + atomIndex + "," + strInfo +")");
    showStatus(strInfo);
    if (pickCallback != null && jsoWindow != null)
      jsoWindow.call(pickCallback,
                     new Object[] {htmlName, strInfo, new Integer(atomIndex)});
  }

  public void update(Graphics g) {
    //    System.out.println("update called");
    if (viewer == null) // it seems that this can happen at startup sometimes
      return;
    if (showPaintTime)
      startPaintClock();
    Dimension size = jvm12orGreater ? jvm12.getSize() : size();
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
    /*
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ie) {
    }
    */

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

  private void printProgressbarMessage(Graphics g) {
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

  private final static Color frankColor = Color.gray;
  private final static String frankString = "Jmol";
  private final static String frankFontName = "Serif";
  private final static int frankFontStyle = Font.BOLD;
  private final static int frankFontSize = 14;
  private final static int frankMargin = 4;
  Font frankFont;
  int frankWidth;
  int frankDescent;
  


  public boolean handleEvent(Event e) {
    if (viewer == null)
      return false;
    return viewer.handleOldJvm10Event(e);
  }

  // code to record last and average times
  // last and average of all the previous times are shown in the status window

  private static int timeLast = 0;
  private static int timeCount;
  private static int timeTotal;

  private void resetTimes() {
    timeCount = timeTotal = 0;
    timeLast = -1;
  }

  private void recordTime(int time) {
    if (timeLast != -1) {
      timeTotal += timeLast;
      ++timeCount;
    }
    timeLast = time;
  }

  private long timeBegin;
  private int lastMotionEventNumber;

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    int motionEventNumber = viewer.motionEventNumber;
    if (lastMotionEventNumber != motionEventNumber) {
      lastMotionEventNumber = motionEventNumber;
      resetTimes();
    }
  }

  private void stopPaintClock() {
    int time = (int)(System.currentTimeMillis() - timeBegin);
    recordTime(time);
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
    int timeAverage =
      (timeCount == 0)
      ? -1
      : (timeTotal + timeCount/2) / timeCount; // round, don't truncate
    g.setColor(Color.green);
    g.drawString(fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms", x, y);
  }

  //METHODS FOR JAVASCRIPT

  /****************************************************************
   * These methods are intended for use from JavaScript via LiveConnect
   *
   * Note that there are some bug in LiveConnect implementations that
   * place some restrictions on the names of the functions in this file.
   * For example, LiveConnect on Netscape 4.7 will get confused if you
   * overload a method name with different parameter signatures ...
   * ... even if one of the methods is private
   * mth 2003 02
   ****************************************************************/

  /****************************************************************
   * FIXME mth 2004 01 12
   * these routines are all DEPRECATED
   ****************************************************************/

  /*
  private final String[] styleStrings = {"SHADED", "WIREFRAME"};
  private final byte[] styles = {JmolConstants.STYLE_SHADED,
                                 JmolConstants.STYLE_WIREFRAME};

  public void setStyle(String style) {
    for (int i = 0; i < styleStrings.length; ++i) {
      if (styleStrings[i].equalsIgnoreCase(style)) {
        //        viewer.setStyleAtom(styles[i]);
        viewer.setStyleBond(styles[i]);
        return;
      }
    }
  }

  private final String[] labelStyleStrings = {"NONE","SYMBOL","NUMBER"};
  private final byte[] labelStyles = {JmolConstants.LABEL_NONE,
                                      JmolConstants.LABEL_SYMBOL,
                                      JmolConstants.LABEL_ATOMNO};

  public void setLabelStyle(String style) {
    for (int i = 0; i < labelStyles.length; ++i) {
      if (labelStyleStrings[i].equalsIgnoreCase(style)) {
        viewer.setStyleLabel(labelStyles[i]);
        return;
      }
    }
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    viewer.setPerspectiveDepth(perspectiveDepth);
  }

  public void setWireframeRotation(boolean wireframeRotation) {
    viewer.setWireframeRotation(wireframeRotation);
  }
  */

  private final Object[] buttonCallbackBefore = { null, new Boolean(false)};
  private final Object[] buttonCallbackAfter = { null, new Boolean(true)};

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
    System.out.println(htmlName + " will try to run:\n-----" +
                       script + "\n-----\n");
    String strError = viewer.evalString(script);
    if (strError == null)
      strError = "Jmol executing script ...";
    setStatusMessage(strError);
  }

  public void load(String modelName) {
    if (modelName != null) {
      viewer.openFile(modelName);
      String strError = viewer.getOpenFileError();
      setStatusMessage(strError);
    }
    repaint();
  }

  char inlineNewlineChar = '|';

  public void loadInline(String strModel) {
    if (strModel != null) {
      if (inlineNewlineChar != 0) {
        int len = strModel.length();
        int i;
        for (i = 0; i < len && strModel.charAt(0) == ' '; ++i)
          ;
        if (i < len && strModel.charAt(i) == inlineNewlineChar)
          strModel = strModel.substring(i + 1);
        strModel = strModel.replace(inlineNewlineChar, '\n');
      }
      viewer.openStringInline(strModel);
      setStatusMessage(viewer.getOpenFileError());
    }
  }

  private void loadPopupMenuAsBackgroundTask() {
    if (viewer.strOSName.equals("Mac OS") && // no popup on MacOS 9 NetScape
        viewer.strJavaVersion.equals("1.1.5"))
      return;
    new LoadPopupThread(this).run();
  }

  private class LoadPopupThread implements Runnable {

    JmolApplet jmolApplet;
    
    LoadPopupThread(JmolApplet jmolApplet) {
      this.jmolApplet = jmolApplet;
    }
    
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
}
