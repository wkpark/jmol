
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
package org.openscience.jmol.applet;

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
import java.awt.event.ComponentListener;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import org.openscience.jmol.io.ReaderProgress;
import org.openscience.jmol.io.ReaderFactory;
import org.openscience.jmol.StatusDisplay;
import org.openscience.jmol.FortranFormat;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.CMLReader;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class JmolApplet extends Applet implements StatusDisplay {

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

  AppletCanvas canvas;
  DisplayControl control;

  Eval eval;
  
  int mode;
  int labelMode;
  private String helpMessage =
    "Keys: S- change style; L- Show labels; B-Toggle Bonds";
  private String errorMessage;

  private String defaultAtomTypesFileName = "Data/AtomTypes.txt";

  private boolean bondsEnabled = true;

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

    canvas.addMouseListener(new MyMouseListener());
    canvas.addKeyListener(new MyKeyListener());

    setLayout(new java.awt.BorderLayout());
    add(canvas, "Center");

    eval = new Eval(control);
  }

  public void initApplication() {
    control.setShowBonds(true);
    control.setShowAtoms(true);
    control.zoomToPercent(100);
    control.setPercentVdwAtom(20);
    control.setModeBondDraw(DisplayControl.SHADING);
    control.setModeAtomDraw(DisplayControl.SHADING);
    /*
    double zoomFactor = 1;

    myBean.setBondsShown(true);
    String bonds = getParameter("BONDS");
    if (bonds != null) {
      if (bonds.equalsIgnoreCase("OFF")) {
        myBean.setBondsShown(false);
      } else if (bonds.equalsIgnoreCase("NEVER")) {
        myBean.setBondsShown(false);
        bondsEnabled = false;
        helpMessage = "Keys: S- change style; L- Show labels";
      }
    }
    String zoom = getParameter("ZOOM");
    if (zoom != null) {
      zoomFactor = FortranFormat.atof(zoom);
    }
    myBean.setZoomFactor(zoomFactor);
    zoomFactor = .2;
    zoom = getParameter("ATOMSIZE");
    if (zoom != null) {
      zoomFactor = FortranFormat.atof(zoom);
    }
    myBean.setAtomSphereFactor(zoomFactor);
    String customViews = getParameter("CUSTOMVIEWS");
    if (customViews != null) {
      myBean.setCustomViews(customViews);
    }
    */

    String wfr = getParameter("WIREFRAMEROTATION");
    if (wfr != null &&
        (wfr.equalsIgnoreCase("on") || wfr.equalsIgnoreCase("true")))
      control.setWireframeRotation(true);

    setBackgroundColor(getParameter("BCOLOR"));
    setBackgroundColor(getParameter("BCOLOUR"));
    setForegroundColor(getParameter("FCOLOR"));
    setForegroundColor(getParameter("FCOLOUR"));

    setAtomPropertiesFromFile(getParameter("ATOMTYPES"));

    String model = getParameter("MODEL");
    String format = getParameter("FORMAT");
    if (model != null) {
      try {
        ChemFileReader cfr = null;
        ReaderProgress readerProgress = new ReaderProgress(this);
        if (format != null && format.equalsIgnoreCase("CMLSTRING")) {
          StringBuffer cmlString = new StringBuffer();
          cmlString.append(convertEscapeChars(model));
          cfr = new CMLReader(new java.io.StringReader(cmlString.toString()));
          readerProgress.setFileName("CML string");
        } else {
          URL modelURL = null;
          try {
            modelURL = new URL(getDocumentBase(), model);
            String fileName = modelURL.getFile();
            int fileNameIndex = fileName.lastIndexOf('/');
            if (fileNameIndex >= 0) {
              fileName = fileName.substring(fileNameIndex + 1);
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
          control.setChemFile(cfr.read());
        } else {
          setErrorMessage("Error: Unable to read input format");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    /*
      mode = QUICKDRAW;
      String style = getParameter("STYLE");
      if (style != null) {
      mode = getDrawMode(style);
      }
      setRenderingStyle();
      
      String pickmode = getParameter("PICKMODE");
      if (pickmode != null) {
      myBean.setPickingMode(pickmode);
      }
      
      String atomLabels = getParameter("ATOMLABELS");
      if (atomLabels != null) {
      myBean.setLabelRenderingStyle(atomLabels);
      }
    */
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
    "WIREFRAME", "QUICKDRAW", "HALFSHADED", "SHADED"
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

  // FIXME -- modes should be the same so that map is not necessary
  // map from applet modes to DisplayControl modes
  static int[] mapModeAtomDraw = {
    DisplayControl.WIREFRAME,
    DisplayControl.QUICKDRAW,
    DisplayControl.SHADING,
    DisplayControl.SHADING};
    
  static int[] mapModeBondDraw = {
    DisplayControl.LINE,
    DisplayControl.QUICKDRAW,
    DisplayControl.QUICKDRAW,
    DisplayControl.SHADING};

  void setRenderingStyle() {
    control.setModeAtomDraw(mapModeAtomDraw[mode]);
    control.setModeBondDraw(mapModeBondDraw[mode]);
  }

  void setLabelStyle() {
    // no mapping is necessary in this case
    control.setModeLabel(labelMode);
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
          if (tokenString.equalsIgnoreCase("&lt")) {
            out.append(lessThan);
          } else if (tokenString.equalsIgnoreCase("&gt")) {
            out.append(moreThan);
          } else if (tokenString.equalsIgnoreCase("&quot")) {
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


  class MyMouseListener extends MouseAdapter {

    public void mouseClicked(MouseEvent e) {
      setStatusMessage(helpMessage);
    }

    public void mouseEntered(MouseEvent e) {
      setStatusMessage(helpMessage);
    }
  }

  class MyKeyListener extends KeyAdapter {

    public void keyTyped(KeyEvent e) {

      switch(e.getKeyChar()) {
      case 's':
      case 'S':
        mode++;
        mode %= drawModeNames.length;
        setStatusMessage("JmolApplet: Changing rendering style to "
                         + drawModeNames[mode]);
        setRenderingStyle();
        break;
      case 'l':
      case 'L':
        labelMode++;
        labelMode %= 4;
        setLabelStyle();
        break;
      case 'b':
      case 'B':
        if (bondsEnabled)
          control.setShowBonds(!control.showBonds);
        break;
      case 'p':
      case 'P':
        control.setModeMouse(DisplayControl.PICK);
        break;
      case 't':
      case 'T':
        control.setModeMouse(DisplayControl.XLATE);
        break;
      case 'r':
      case 'R':
        control.setModeMouse(DisplayControl.ROTATE);
        break;
      case 'z':
      case 'Z':
        control.setModeMouse(DisplayControl.ZOOM);
        break;
      }
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
      control.setChemFile(cfr.read());
    } catch (IOException e) {
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
      control.setChemFile(cfr.read());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

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

  /**
   * <b>For Javascript:<\b> Set the background colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
  public void setBackgroundColor(String colorInHex) {
    if (colorInHex != null)
      control.setColorBackground(colorInHex);
  }

  /**
   * <b>For Javascript:<\b> Set the foreground colour.
   * @param colourInHex The colour in the format #FF0000 for red etc
   */
  public void setForegroundColor(String colorInHex) {
    if (colorInHex != null)
      control.setColorForeground(colorInHex);
  }

  /**
   * <b>For Javascript:<\b> Causes Atoms to be shown or hidden.
   * @param value if 'T' then atoms are displayed, if 'F' then they aren't.
   */
  public void setAtomsShown(String value) {
    //    myBean.setAtomsShown(value);
  }

  /**
   * <b>For Javascript:<\b> Causes bonds to be shown or hidden.
   * @param value if 'T' then atoms are displayed, if 'F' then they aren't.
   */
  public void setBondsShown(String value) {
    //    myBean.setBondsShown(value);
  }

  /**
   * <b>For Javascript:<\b> Sets the rendering mode for atoms. Valid values are 'QUICKDRAW', 'SHADED' and 'WIREFRAME'.
   */
  private final String[] atomStyles = {"QUICKDRAW", "SHADED", "WIREFRAME"};
  private final int[] atomModes = {DisplayControl.QUICKDRAW,
                                   DisplayControl.SHADING,
                                   DisplayControl.WIREFRAME};
  private final int[] bondModes = {DisplayControl.QUICKDRAW,
                                   DisplayControl.SHADING,
                                   DisplayControl.LINE};
  public void setRenderingStyle(String style) {
    for (int i = 0; i < atomStyles.length; ++i) {
      if (atomStyles[i].equalsIgnoreCase(style)) {
        control.setModeAtomDraw(atomModes[i]);
        control.setModeBondDraw(bondModes[i]);
        return;
      }
    }
  }

  /**
   * <b>For Javascript:<\b> Sets the rendering mode for labels. Valid values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
  private final String[] labelStyles = {"NONE","SYMBOLS","TYPES","NUMBERS"};
  private final int[] labelModes = {DisplayControl.NOLABELS,
                                    DisplayControl.SYMBOLS,
                                    DisplayControl.TYPES,
                                    DisplayControl.NUMBERS};
  public void setLabelRenderingStyle(String style) {
    for (int i = 0; i < labelStyles.length; ++i) {
      if (labelStyles[i].equalsIgnoreCase(style)) {
        control.setModeLabel(labelModes[i]);
        return;
      }
    }
  }

  /**
   * <b>For Javascript:<\b> Gets the rendering mode for labels. Values are 'NONE', 'SYMBOLS', 'TYPES' and 'NUMBERS'.
   */
  public String getLabelRenderingStyleDescription() {
    //    return myBean.getLabelRenderingStyleDescription();
    return "FOO";
  }

  /**
   * <b>For Javascript:<\b> Sets whether they view automatically goes to wireframe when they model is rotated.
   * @param doesIt String either 'T' or 'F'
   */
  public void setPerspectiveDepth(boolean perspectiveDepth) {
    control.setPerspectiveDepth(perspectiveDepth);
  }

  public void setWireframeRotation(boolean wireframeRotation) {
    control.setWireframeRotation(wireframeRotation);
  }

  public void rasmolScript(String scriptName) {
    long timeBegin = System.currentTimeMillis();
    System.out.println("rasmolScript:" + timeBegin);
    if (eval.loadFile(scriptName)) {
      System.out.println("loadTime=" +
                         (int)(System.currentTimeMillis() - timeBegin));
      eval.run();
    }
    System.out.println("totalTime=" +
                       (int)(System.currentTimeMillis() - timeBegin));
  }

  public void rasmolScriptInline(String script) {
    if (eval.loadString(script))
      eval.run();
  }

  public void load(String modelName) {
    control.openFile(modelName);
  }
}
