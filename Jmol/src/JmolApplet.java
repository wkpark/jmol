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

import org.openscience.jmol.applet.*;
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolStatusListener;
import org.openscience.jmol.adapters.DeprecatedJmolModelAdapter;
import org.openscience.jmol.ui.JmolPopup;

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

public class JmolApplet extends Applet implements JmolStatusListener {

  AppletCanvas canvas;
  JmolViewer viewer;
  JmolPopup jmolpopup;

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

    canvas = new AppletCanvas();
    viewer = new JmolViewer(canvas, new DeprecatedJmolModelAdapter(), false);
    canvas.setJmolViewer(viewer);
    viewer.setJmolStatusListener(this);

    jmolpopup = new JmolPopup(viewer, canvas);

    viewer.setAppletDocumentBase(getDocumentBase());

    setLayout(new java.awt.BorderLayout());
    add(canvas, "Center");
  }

  public void initApplication() {
    viewer.pushHoldRepaint();
    {
      viewer.setPercentVdwAtom(20);
      viewer.zoomToPercent(100);
      viewer.setStyleBond(JmolViewer.SHADED);
      viewer.setStyleAtom(JmolViewer.SHADED);
      
      viewer.setColorBackground(getParameter("bgcolor"));
      setStyle(getParameter("style"));
      setLabelStyle(getParameter("label"));

      String wfr = getParameter("wireframeRotation");
      setWireframeRotation(wfr != null &&
                           (wfr.equalsIgnoreCase("on") ||
                            wfr.equalsIgnoreCase("true")));

      String pd = getParameter("perspectiveDepth");
      setPerspectiveDepth(pd == null ||
                          pd.equalsIgnoreCase("on") ||
                          pd.equalsIgnoreCase("true"));
      
      load(getParameter("load"));
      loadInline(getParameter("loadInline"));
      script(getParameter("script"));
    }
    viewer.popHoldRepaint();
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

  public void handlePopupMenu(MouseEvent e) {
    jmolpopup.show(e.getComponent(), e.getX(), e.getY());
  }

  public void measureSelection(int atomIndex) {
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

  public void script(String script) {
    String strError = viewer.evalString(script);
    setStatusMessage(strError);
  }

  public void load(String modelName) {
    if (modelName != null) {
      String strError = viewer.openFile(modelName);
      setStatusMessage(strError);
    }
  }

  public void loadInline(String strModel) {
    if (strModel != null) {
      String strError = viewer.openStringInline(strModel);
      setStatusMessage(strError);
    }
  }
}
