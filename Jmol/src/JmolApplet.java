/*
 * Copyright 2002 The Jmol Development Team
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
import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.AtomTypeSet;
import org.openscience.jmol.script.Eval;

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import org.openscience.jmol.io.ReaderProgress;
import org.openscience.jmol.io.ReaderFactory;
import org.openscience.jmol.JmolStatusListener;
import org.openscience.jmol.FortranFormat;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.CMLReader;

public class JmolApplet extends Applet implements JmolStatusListener {

  AppletCanvas canvas;
  DisplayControl control;

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
    initWindows();
    initApplication();
  }
  
  public void initWindows() {

    String strJvmVersion = System.getProperty("java.version");

    canvas = new AppletCanvas();
    control = new DisplayControl(strJvmVersion, canvas);
    canvas.setDisplayControl(control);

    control.setAppletDocumentBase(getDocumentBase());

    setLayout(new java.awt.BorderLayout());
    add(canvas, "Center");
  }

  public void initApplication() {
    control.pushHoldRepaint();
    {
      control.setShowBonds(true);
      control.setShowAtoms(true);
      control.setPercentVdwAtom(20);
      control.zoomToPercent(100);
      control.setStyleBond(DisplayControl.SHADING);
      control.setStyleAtom(DisplayControl.SHADING);
      
      control.setColorBackground(getParameter("bgcolor"));
      style(getParameter("style"));
      label(getParameter("label"));
      //      setAtomPropertiesFromFile(getParameter("atomTypes"));

      String wfr = getParameter("wireframeRotation");
      setWireframeRotation(wfr != null &&
                           (wfr.equalsIgnoreCase("on") ||
                            wfr.equalsIgnoreCase("true")));
      
      load(getParameter("load"));
      loadInline(getParameter("loadInline"));
      script(getParameter("script"));
    }
    control.popHoldRepaint();
  }

  public void setStatusMessage(String statusMessage) {
    if (statusMessage != null)
      showStatus(statusMessage);
  }

  public void scriptEcho(String strEcho) {
  }

  public void scriptStatus(String strStatus) {
  }

  public void notifyScriptTermination(String errorMessage, int msWalltime) {
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


  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a file and allocates this as the current atom types- eg radius etc.
   * @param atomTypesFile The filename of the properties we want.
   */
  public void setAtomPropertiesFromFile(String atomTypesFile) {
    try {
      InputStream atis = null;
      if (atomTypesFile == null || atomTypesFile.length() == 0) {
        atis = getClass().getResourceAsStream(defaultAtomTypesFileName);
        atomTypesFile = defaultAtomTypesFileName + "(default)";
      } else {
        URL atURL = new URL(getDocumentBase(), atomTypesFile);
        atis = atURL.openStream();
      }
      if (atis == null) {
        System.err.println("Unable to open the atom types file:" +
                           atomTypesFile);
        return;
      }
      AtomTypeSet ats1 = new AtomTypeSet();
      ats1.load(atis);
    } catch (IOException e) {
      System.err.println("IOException reading atom properties:" + e);
    }
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a URL and allocates this as the current atom types- eg radius etc.
   * @param propertiesFileURL The URL of the properties we want.
   */
  public void setAtomPropertiesFromURL(String propertiesURL) {
    //    myBean.setAtomPropertiesFromURL(propertiesURL);
  }


  private final String[] styleStrings = {"QUICKDRAW", "SHADED", "WIREFRAME"};
  private final byte[] styles = {DisplayControl.QUICKDRAW,
                                 DisplayControl.SHADING,
                                 DisplayControl.WIREFRAME};

  public void style(String style) {
    for (int i = 0; i < styleStrings.length; ++i) {
      if (styleStrings[i].equalsIgnoreCase(style)) {
        control.setStyleAtom(styles[i]);
        control.setStyleBond(styles[i]);
        return;
      }
    }
  }

  private final String[] labelStyleStrings = {"NONE","SYMBOL","NUMBER"};
  private final byte[] labelStyles = {DisplayControl.NOLABELS,
                                      DisplayControl.SYMBOLS,
                                      DisplayControl.NUMBERS};

  public void label(String style) {
    for (int i = 0; i < labelStyles.length; ++i) {
      if (labelStyleStrings[i].equalsIgnoreCase(style)) {
        control.setStyleLabel(labelStyles[i]);
        return;
      }
    }
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    control.setPerspectiveDepth(perspectiveDepth);
  }

  public void setWireframeRotation(boolean wireframeRotation) {
    control.setWireframeRotation(wireframeRotation);
  }

  public void script(String script) {
    String strError = control.evalString(script);
    setStatusMessage(strError);
  }

  public void load(String modelName) {
    if (modelName != null) {
      String strError = control.openFile(modelName);
      setStatusMessage(strError);
    }
  }

  public void loadInline(String strModel) {
    if (strModel != null) {
      String strError = control.openStringInline(strModel);
      setStatusMessage(strError);
    }
  }
}
