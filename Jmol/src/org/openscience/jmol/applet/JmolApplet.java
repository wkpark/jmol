
/*
 * Copyright 2001 The Jmol Development Team
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
package org.openscience.jmol.applet;

import org.openscience.jmol.*;
import java.awt.event.MouseListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class JmolApplet extends java.applet.Applet
        implements MouseListener, KeyListener, StatusDisplay {

  private static String appletInfo =
    "Jmol Applet.  Part of the OpenScience project.  See www.openscience.org/Jmol for more information";

  private static String[][] paramInfo = {
    {
      "FORMAT", "string",
      "Set this to CMLSTRING for an embedded CML string in the MODEL parameter, otherwise leave blank"
    }, {
      "MODEL", "url/model",
      "URL of the chemical data, or CML chemical data if FORMAT set to CMLSTRING.  REQUIRED"
    }, {
      "ATOMTYPES", "url",
      "URL of custom Atomtypes file, or leave blank to use the default atom definitions"
    }, {
      "BCOLOUR", "color", "Background color"
    }, {
      "FCOLOUR", "color", "Text color"
    }, {
      "STYLE", "SHADED, HALFSHADED, QUICKDRAW or WIREFRAME",
      "One of the four possible rendering styles"
    }, {
      "WIREFRAMEROTATION", "ON or OFF",
      "Select change to wireframe mode during rotation"
    }, {
      "ZOOM", "number",
      "Changes the initial zoom and perspective.  1 is the default"
    }, {
      "ATOMSIZE", "number",
      "Changes the size of the atoms without zooming.  1 is the default"
    }, {
      "BONDS", "ON OFF or NEVER",
      "Specifies if bonds are drawn initially, and if they can be selected by the user"
    }, {
      "CUSTOMVIEWS",
      "Button1{HOME;FRAME=n;ROTATE=y,x,y;ZOOM=n;TRANSLATE=x,y}Button2{HOME;ZOOM=n;...}...",
      "Specifies custom actions to be performed by custom buttons"
    }, {
      "PICKMODE", "SINGLE or MULTIPLE",
      "Sets the picking mode to single or multiple atoms. (Default is single picking)."
    }
  };

  JmolSimpleBean myBean;
  int mode;
  int labelMode;
  private String helpMessage =
    "Keys: S- change style; L- Show labels; B-Toggle Bonds";
  private String errorMessage;

  private String atomTypesFileName = "Data/AtomTypes.txt";

  private boolean bondsEnabled = true;

  public void init() {

    float zoomFactor = 1;
    myBean = new JmolSimpleBean();

    myBean.setBondsShown(true);
    String bonds = getParameter("BONDS");
    if (bonds != null) {
      if (bonds.equals("OFF")) {
        myBean.setBondsShown(false);
      } else if (bonds.equals("NEVER")) {
        myBean.setBondsShown(false);
        bondsEnabled = false;
        helpMessage = "Keys: S- change style; L- Show labels";
      }
    }
    String zoom = getParameter("ZOOM");
    if (zoom != null) {
      zoomFactor = (float) FortranFormat.atof(zoom);
    }
    myBean.setZoomFactor(zoomFactor);
    zoomFactor = 1;
    zoom = getParameter("ATOMSIZE");
    if (zoom != null) {
      zoomFactor = (float) FortranFormat.atof(zoom);
    }
    myBean.setAtomSphereFactor(zoomFactor);
    String customViews = getParameter("CUSTOMVIEWS");
    if (customViews != null) {
      myBean.setCustomViews(customViews);
    }
    String wfr = getParameter("WIREFRAMEROTATION");
    if ((wfr != null) && (wfr.equals("OFF"))) {
      myBean.setWireframeRotation(false);
    }

    InputStream atis = null;
    String atomtypes = getParameter("ATOMTYPES");
    try {
      if ((atomtypes == null) || (atomtypes.length() == 0)) {
        atis = getClass().getResourceAsStream(atomTypesFileName);
        if (atis == null) {
          System.err.println("Unable to open the atom types resource \""
              + atomTypesFileName + "\"");
        }
      } else {
        java.net.URL atURL = new java.net.URL(getDocumentBase(), atomtypes);
        atis = atURL.openStream();
        if (atis == null) {
          System.err.println("Unable to open the atom types URL \""
              + atomTypesFileName + "\"");
        }
      }
      myBean.setAtomPropertiesFromStream(atis);
    } catch (java.io.IOException ex) {
      System.err.println("Error loading atom types: " + ex);
    }
    String model = getParameter("MODEL");
    if (model != null) {
      try {
        ChemFileReader cfr = null;
        ReaderProgress readerProgress = new ReaderProgress(this);
        if ((getParameter("FORMAT") != null)
                && getParameter("FORMAT").toUpperCase().equals("CMLSTRING")) {
          StringBuffer cmlString = new StringBuffer();
          cmlString.append(convertEscapeChars(model));
          for (int i = 0; i < cmlString.length(); ++i) {
            if (cmlString.charAt(i) == '>') {
              cmlString.insert(i+1, '\n');
            }
          }
          cfr = ReaderFactory
                  .createReader(new java.io.StringReader(cmlString.toString()));
          readerProgress.setFileName("CML string");
        } else {
          java.net.URL modelURL = null;
          try {
            modelURL = new java.net.URL(getDocumentBase(), model);
            String fileName = modelURL.getFile();
            int fileNameIndex = fileName.lastIndexOf('/');
            if (fileNameIndex >= 0) {
              fileName = fileName.substring(fileNameIndex+1);
            }
            readerProgress.setFileName(fileName);
          } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(("Got MalformedURL for model: "
                    + e.toString()));
          }
          cfr = ReaderFactory
                  .createReader(new java.io
                    .InputStreamReader(modelURL.openStream()));
        }
        if (cfr != null) {
          cfr.addReaderListener(readerProgress);
          cfr.setBondsEnabled(bondsEnabled);
          myBean.setModel(cfr.read());
        } else {
          setErrorMessage("Error: Unable to read input format");
        }
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }
    }
    myBean.addMouseListener(this);
    myBean.addKeyListener(this);
    String bg = getParameter("BCOLOUR");
    if (bg != null) {
      myBean.setBackgroundColour(bg);
    } else {
      bg = getParameter("BCOLOR");
      if (bg != null) {
        myBean.setBackgroundColour(bg);
      }
    }

    String fg = getParameter("FCOLOUR");
    if (fg != null) {
      myBean.setForegroundColour(fg);
    } else {
      fg = getParameter("FCOLOR");
      if (fg != null) {
        myBean.setForegroundColour(fg);
      }
    }
    mode = QUICKDRAW;
    String style = getParameter("STYLE");
    if (style != null) {
      mode = getDrawMode(style);
    }
    setRenderingStyle();
    setLayout(new java.awt.BorderLayout());
    add(myBean, "Center");

    String pickmode = getParameter("PICKMODE");
    if (pickmode != null) {
      myBean.setPickingMode(pickmode);
    }
  }

  public String[][] getParameterInfo() {
    return paramInfo;
  }

  public String getAppletInfo() {
    return appletInfo;
  }

  public void setStatusMessage(String statusMessage) {

    if (errorMessage != null) {
      showStatus(errorMessage);
    } else {
      showStatus(statusMessage);
    }
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    setStatusMessage(errorMessage);
  }

  /**
   * Draw atoms as transparent circles and bonds as transparent rectangles.
   */
  public static final int WIREFRAME = 0;

  /**
   * Draw atoms as filled circles and bonds as filled rectangles.
   */
  public static final int QUICKDRAW = 1;

  /**
   * Draw atoms as lighted spheres and bonds as filled rectangles.
   */
  public static final int HALFSHADED = 2;

  /**
   * Draw atoms as lighted spheres and bonds as lighted cylinders.
   */
  public static final int SHADED = 3;

  static String[] drawModeNames = {
    "WIREFRANE", "QUICKDRAW", "HALFSHADED", "SHADED"
  };

  /**
   *  Returns the string representation for the given drawing mode.
   *  If the mode is invalid, null is returned.
   */
  public static String getDrawModeName(int mode) {
    if ((mode < 0) || (mode > drawModeNames.length)) {
      return null;
    }
    return drawModeNames[mode];
  }

  /**
   *  Returns the integer representation for the given drawing mode.
   *  If the mode is invalid, -1 is returned.
   */
  public static int getDrawMode(String mode) {

    for (int i = 0; i < drawModeNames.length; ++i) {
      if (drawModeNames[i].equalsIgnoreCase(mode)) {
        return i;
      }
    }
    return -1;
  }

  void setRenderingStyle() {

    if (mode == WIREFRAME) {
      myBean.setAtomRenderingStyle("WIREFRAME");
      myBean.setBondRenderingStyle("WIREFRAME");
    } else if (mode == QUICKDRAW) {
      myBean.setAtomRenderingStyle("QUICKDRAW");
      myBean.setBondRenderingStyle("QUICKDRAW");
    } else if (mode == HALFSHADED) {
      myBean.setAtomRenderingStyle("SHADED");
      myBean.setBondRenderingStyle("QUICKDRAW");
    } else if (mode == SHADED) {
      myBean.setAtomRenderingStyle("SHADED");
      myBean.setBondRenderingStyle("SHADED");
    }
  }

  /**
   * Converts the html escape chars in the input and replaces them
   * with the required chars. Handles &lt; &gt and &quot;
   */
  static String convertEscapeChars(String eChars) {

    String less = "<";
    char lessThan = less.charAt(0);
    String more = ">";
    char moreThan = more.charAt(0);
    String q = "\"";
    char quote = q.charAt(0);
    String am = "&";
    char amp = am.charAt(0);
    String sc = ";";
    char semi = sc.charAt(0);
    StringBuffer eCharBuffer = new StringBuffer(eChars);
    StringBuffer out = new StringBuffer(0);

    //Scan the string for html escape chars and replace them with
    int state = 0;    //0=scanning, 1 = reading
    StringBuffer token = new StringBuffer(0);    //The escape char we are reading
    for (int position = 0; position < eCharBuffer.length(); position++) {
      char current = eCharBuffer.charAt(position);
      if (state == 0) {
        if (current == amp) {
          state = 1;

          //For some reason we have problems with setCharAt so use append
          token = new StringBuffer(0);
          token.append(current);
        } else {

          //Copy through to output
          out.append(current);
        }
      } else {
        if (current == semi) {
          state = 0;

          //Right replace this token
          String tokenString = token.toString();
          if (tokenString.equals("&lt")) {
            out.append(lessThan);
          } else if (tokenString.equals("&gt")) {
            out.append(moreThan);
          } else if (tokenString.equals("&quot")) {
            out.append(quote);
          }
        } else {
          token.append(current);
        }
      }
    }

    String returnValue = out.toString();
    return returnValue;
  }

  /** Takes the string and replaces '%' with EOL chars.
   * Used by the javascript setModelToRenderFromXYZString- more robust
   * than whitescapes you see!
   **/
  public String recoverEOLSymbols(String inputString) {

    String at = "%";
    char mark = at.charAt(0);
    String dt = ".";
    char dot = dt.charAt(0);
    String min = "-";
    char minus = min.charAt(0);
    String sp = " ";
    char space = sp.charAt(0);
    String nlString = "\n";
    char nl = nlString.charAt(0);    //(char)Character.LINE_SEPARATOR;
    StringBuffer eCharBuffer = new StringBuffer(inputString);
    StringBuffer out = new StringBuffer(0);

    //Scan the string for & and replace with /n
    boolean lastWasSpace = false;
    for (int position = 0; position < eCharBuffer.length(); position++) {
      char current = eCharBuffer.charAt(position);
      if (current == mark) {

        //For some reason we have problems with setCharAt so use append
        out.append(nl);
        lastWasSpace = false;
      } else if (current == space) {
        if (!lastWasSpace) {
          out.append(current);
          lastWasSpace = true;
        }
      } else if (Character.isLetterOrDigit(current) || (current == dot)
              || (current == minus)) {

        //Copy through to output
        out.append(current);
        lastWasSpace = false;
      }
    }

    //No idea why but a space at the very end seems to be unhealthy
    if (lastWasSpace) {
      out.setLength(out.length() - 1);
    }
    String returnValue = out.toString();
    return returnValue;

  }


  /**
   * Invoked when the mouse has been clicked on a component.
   */
  public void mouseClicked(MouseEvent e) {
    setStatusMessage(helpMessage);
  }

  /**
   * Invoked when the mouse enters a component.
   */
  public void mouseEntered(MouseEvent e) {
    setStatusMessage(helpMessage);
  }

  /**
   * Invoked when the mouse exits a component.
   */
  public void mouseExited(MouseEvent e) {
  }

  /**
   * Invoked when a mouse button has been pressed on a component.
   */
  public void mousePressed(MouseEvent e) {
  }

  /**
   * Invoked when a mouse button has been released on a component.
   */
  public void mouseReleased(MouseEvent e) {
  }

  /**
   * Invoked when a key has been pressed.
   */
  public void keyPressed(KeyEvent e) {
  }

  /**
   * Invoked when a key has been released.
   */
  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {

    String keyChar = new Character(e.getKeyChar()).toString();
    if (keyChar.equals("s") || keyChar.equals("S")) {
      mode++;
      mode %= drawModeNames.length;
      setStatusMessage("JmolApplet: Changing rendering style to "
              + drawModeNames[mode]);
      setRenderingStyle();
    } else if (keyChar.equals("l") || keyChar.equals("L")) {
      labelMode++;
      labelMode %= 4;
      if (labelMode == 0) {
        setStatusMessage("JmolApplet: Changing label style to NONE");
        myBean.setLabelRenderingStyle("NONE");
      } else if (labelMode == 1) {
        setStatusMessage("JmolApplet: Changing label style to SYMBOLS");
        myBean.setLabelRenderingStyle("SYMBOLS");
      } else if (labelMode == 2) {
        setStatusMessage("JmolApplet: Changing label style to TYPES");
        myBean.setLabelRenderingStyle("TYPES");
      } else if (labelMode == 3) {
        setStatusMessage("JmolApplet: Changing label style to NUMBERS");
        myBean.setLabelRenderingStyle("NUMBERS");
      } else {
        setStatusMessage("JmolApplet: Changing label style to default");
        myBean.setBondRenderingStyle("NONE");
      }
    } else if ((bondsEnabled)
            && ((keyChar.equals("b") || keyChar.equals("B")))) {
      myBean.toggleBonds();
    }
  }

  //METHODS FOR JAVASCRIPT

  /**
   * <b>For Javascript:<\b> Takes the argument, pharses it as an XYZ file and sets it as the current model.
   * For robustness EOL chars can be ignored and should then be replaced with % symbols.
   * @param xyzString The whole of the molecule XYZ file as a single string.
   * @param aliasedEndOfLine If 'T' then EOL chars should be replaced by % symbols otherwise 'F'.
   */
  public void setModelToRenderFromXYZString(String xyzString,
          String aliasedEndOfLine) {

    String aliasedEOL = aliasedEndOfLine.toUpperCase();
    String hugeXYZString = xyzString;
    if (aliasedEOL.equals("T")) {
      hugeXYZString = recoverEOLSymbols(hugeXYZString);
    }
    try {
      ChemFileReader cfr =
        ReaderFactory.createReader(new java.io.StringReader(hugeXYZString));
          cfr.setBondsEnabled(bondsEnabled);
      myBean.setModel(cfr.read());
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, pharses it as CML and sets it as the current model.
   * Note that the CML should be straight- it is not necessary to use HTML escape codes.
   * @param hugeCMLString The whole of the molecule CML as a single string.
   */
  public void setModelToRenderFromCMLString(String hugeCMLString) {

    try {
      ChemFileReader cfr =
        ReaderFactory.createReader(new java.io.StringReader(hugeCMLString));
          cfr.setBondsEnabled(bondsEnabled);
      myBean.setModel(cfr.read());
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a URL and sets it as the current model.
   * @param modelURL The URL of the model we want.
   */
  public void setModelToRenderFromURL(String modelURLString) {

    try {
      java.net.URL modelURL = null;
      try {
        modelURL = new java.net.URL(getDocumentBase(), modelURLString);
      } catch (java.net.MalformedURLException e) {
        throw new RuntimeException(("Got MalformedURL for model: "
                + e.toString()));
      }
      ChemFileReader cfr =
        ReaderFactory
          .createReader(new java.io.InputStreamReader(modelURL.openStream()));
          cfr.setBondsEnabled(bondsEnabled);
      myBean.setModel(cfr.read());
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a file and sets it as the current model.
   * @param modelFile The filename of the model we want.
   * @param type Either "XYZ", "CML" or "PDB"
   */
  public void setModelToRenderFromFile(String modelFile, String type) {
    setModelToRenderFromFile(modelFile, type);
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a file and allocates this as the current atom types- eg radius etc.
   * @param propertiesFile The filename of the properties we want.
   */
  public void setAtomPropertiesFromFile(String propertiesFile) {
    myBean.setAtomPropertiesFromFile(propertiesFile);
  }

  /**
   * <b>For Javascript:<\b> Takes the argument, reads it as a URL and allocates this as the current atom types- eg radius etc.
   * @param propertiesFileURL The URL of the properties we want.
   */
  public void setAtomPropertiesFromURL(String propertiesURL) {
    myBean.setAtomPropertiesFromURL(propertiesURL);
  }

  /**
   * <b>For Javascript:<\b> Set the background colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
  public void setBackgroundColour(String colourInHex) {
    myBean.setBackgroundColour(colourInHex);
  }

  /**
   * <b>For Javascript:<\b> Set the foreground colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
  public void setForegroundColour(String colourInHex) {
    myBean.setForegroundColour(colourInHex);
  }

  /**
   * <b>For Javascript:<\b> Causes Atoms to be shown or hidden.
   * @param value if 'T' then atoms are displayed, if 'F' then they aren't.
   */
  public void setAtomsShown(String value) {
    myBean.setAtomsShown(value);
  }

  /**
   * <b>For Javascript:<\b> Causes bonds to be shown or hidden.
   * @param value if 'T' then atoms are displayed, if 'F' then they aren't.
   */
  public void setBondsShown(String value) {
    myBean.setBondsShown(value);
  }

  /**
   * <b>For Javascript:<\b> Sets the rendering mode for atoms. Valid values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'.
   */
  public void setAtomRenderingStyle(String style) {
    myBean.setAtomRenderingStyle(style);
  }

  /**
   * <b>For Javascript:<\b> Gets the rendering mode for atoms. Values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'.
   */
  public String getAtomRenderingStyleDescription() {
    return getAtomRenderingStyleDescription();
  }

  /**
   * <b>For Javascript:<\b> Sets the rendering mode for bonds. Valid values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'.
   */
  public void setBondRenderingStyle(String style) {
    myBean.setBondRenderingStyle(style);
  }

  /**
   * <b>For Javascript:<\b> Gets the rendering mode for bonds. Values are 'QUICKDRAW', 'SHADED', 'LINE' and 'WIREFRAME'.
   */
  public String getBondRenderingStyleDescription() {
    return myBean.getBondRenderingStyleDescription();
  }

  /**
   * <b>For Javascript:<\b> Sets the rendering mode for labels. Valid values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
  public void setLabelRenderingStyle(String style) {
    myBean.setLabelRenderingStyle(style);
  }

  /**
   * <b>For Javascript:<\b> Gets the rendering mode for labels. Values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
  public String getLabelRenderingStyleDescription() {
    return myBean.getLabelRenderingStyleDescription();
  }

  /**
   * <b>For Javascript:<\b> Sets whether they view automatically goes to wireframe when they model is rotated.
   * @param doesIt String either 'T' or 'F'
   */
  public void setAutoWireframe(String doesIt) {
    myBean.setAutoWireframe(doesIt);
  }


}
