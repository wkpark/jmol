/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolStatusListener;
import org.openscience.jmol.adapters.SimpleModelAdapter;
//import org.openscience.jmol.adapters.CdkJmolModelAdapter;
import org.openscience.jmol.ui.JmolPopup;

import netscape.javascript.JSObject;

import java.applet.*;
import java.awt.*;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.MissingResourceException;

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

  private String defaultAtomTypesFileName = "Data/AtomTypes.txt";

  public String getAppletInfo() {
    return appletInfo;
  }
  private static String appletInfo =
    "Jmol Applet.  Part of the OpenScience project. " +
    "See jmol.sourceforge.net for more information";

  private static String[][] paramInfo = {
    { "bgcolor", "color",
      "Background color to HTML color name or #RRGGBB" },
    { "style", "SHADED, QUICKDRAW or WIREFRAME",
      "One of the three possible rendering styles" },
    { "label", "NONE, SYMBOL or NUMBER",
      "Select style for atom labels" },
    { "atomTypes", "url", "URL of custom Atomtypes file, " +
      "or leave blank to use the default atom definitions" },
    { "wireframeRotation", "ON or OFF",
      "Switch to wireframe during rotations for better performance" },
    { "load", "url",
      "URL of the chemical data" },
    { "loadInline", "fileformat",
      "Inline representation of chemical data" },
    { "script", "string",
      "Inline RasMol/Chime script commands " +
      "separated by newlines or semicolons" }
  };
  public String[][] getParameterInfo() {
    return paramInfo;
  }


  public void init() {
    htmlName = getParameter("name");
    mayScript = getParameter("mayscript") != null;
    appletRegistry = new JmolAppletRegistry(htmlName, mayScript, this);

    loadProperties();
    initWindows();
    initApplication();
  }
  
  public void initWindows() {

    // to enable CDK
    //viewer = new JmolViewer(this, new CdkJmolModelAdapter());
    viewer = new JmolViewer(this, new SimpleModelAdapter());
    viewer.setJmolStatusListener(this);

    if (! (viewer.strOSName.equals("Mac OS") &&
           viewer.strJavaVersion.equals("1.1.5")))
      jmolpopup = new JmolPopup(viewer, this);

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
      appletProperties = new PropertyResourceBundle(urlProperties.openStream());
    } catch (Exception ex) {
      System.out.println("JmolApplet.loadProperties() -> " + ex);
    }
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
        System.out.println(propertyName + ":" + stringValue + " is not an integer");
      }
    return defaultValue;
  }

  private double getValue(String propertyName, double defaultValue) {
    String stringValue = getValue(propertyName, null);
    if (stringValue != null)
      try {
        return (new Double(stringValue)).doubleValue();
      } catch (NumberFormatException ex) {
        System.out.println(propertyName + ":" + stringValue + " is not an integer");
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
      emulate = getValueLowerCase("emulate", "jmol");
      if (emulate.equals("chime")) {
        viewer.setRasmolDefaults();
      } else {
        viewer.setJmolDefaults();
        viewer.setPercentVdwAtom(getValue("vdwPercent", 20));
        setStyle(getValue("style", "shaded"));
        setLabelStyle(getValue("label", "none"));
        viewer.setColorBackground(getValue("bgcolor", "white"));
        String wfr = getValue("wireframeRotation", "false");
        setWireframeRotation(wfr.equalsIgnoreCase("on") ||
                             wfr.equalsIgnoreCase("true"));
        
        String pd = getValue("perspectiveDepth", "true");
        setPerspectiveDepth(pd.equalsIgnoreCase("on") ||
                            pd.equalsIgnoreCase("true"));
      }
      
      load(getValue("load", null));
      loadInline(getValue("loadInline", null));
      script(getValue("script", null));

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
        System.out.println("WARNING!! to use callback function you *must* " +
                           "declare the MAYSCRIPT attribute in your APPLET " +
                           "tag in your html code");

    }
    viewer.popHoldRepaint();
  }

  public void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile) {
    if (loadStructCallback != null && jsoWindow != null)
      jsoWindow.call(loadStructCallback, new Object[] {htmlName});
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

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics g) {
    Dimension size = jvm12orGreater ? jvm12.getSize() : size();
    viewer.setScreenDimension(size);
    Rectangle rectClip = jvm12orGreater ? jvm12.getClipBounds(g) : g.getClipRect();
    g.drawImage(viewer.renderScreenImage(rectClip), 0, 0, null);
  }

  public boolean handleEvent(Event e) {
    return viewer.handleEvent(e);
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

  private final String[] styleStrings = {"SHADED", "WIREFRAME"};
  private final byte[] styles = {JmolViewer.SHADED,
                                 JmolViewer.WIREFRAME};

  public void setStyle(String style) {
    for (int i = 0; i < styleStrings.length; ++i) {
      if (styleStrings[i].equalsIgnoreCase(style)) {
        viewer.setStyleAtom(styles[i]);
        viewer.setStyleBond(styles[i]);
        return;
      }
    }
  }

  private final String[] labelStyleStrings = {"NONE","SYMBOL","NUMBER"};
  private final byte[] labelStyles = {JmolViewer.NOLABELS,
                                      JmolViewer.SYMBOLS,
                                      JmolViewer.NUMBERS};

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

  private final Object[] buttonCallbackBefore = { null, new Boolean(false)};
  private final Object[] buttonCallbackAfter = { null, new Boolean(true)};

  public void scriptButton(JSObject buttonWindow, String buttonName,
                           String script, String buttonCallback) {
    System.out.println(htmlName + " button:" + buttonName +
                       " script:" + script +
                       " buttonCallback:" + buttonCallback +
                       " buttonWindow:" + buttonWindow);
    if (buttonCallback != null) {
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
    System.out.println(htmlName + " will try to run:\n-----" + script + "\n-----\n");
    setStatusMessage("Jmol script executing...");
    String strError = viewer.evalString(script);
    setStatusMessage(strError);
  }

  public void load(String modelName) {
    if (modelName != null) {
      viewer.openFile(modelName);
      String strError = viewer.getOpenFileError();
      setStatusMessage(strError);
    }
  }

  public void loadInline(String strModel) {
    if (strModel != null) {
      viewer.openStringInline(strModel);
      setStatusMessage(viewer.getOpenFileError());
    }
  }
}
