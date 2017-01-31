/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.gennbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;

import org.gennbo.NBODialogRun.ChooseList;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogConfig extends JDialog {

  abstract protected void updatePanelSettings();

  abstract protected NBOFileHandler newNBOFileHandler(String name, String ext,
                                                      int mode, String useExt);

  static final char DIALOG_HOME =   'h';
  static final char DIALOG_SEARCH = 's';
  static final char DIALOG_VIEW =   'v';
  static final char DIALOG_RUN =    'r';
  static final char DIALOG_MODEL =  'm';
  static final char DIALOG_CONFIG = 'c';

  protected static final int ORIGIN_UNKNOWN = 0;
  protected static final int ORIGIN_NIH = 1;
  protected static final int ORIGIN_LINE_FORMULA = 2;
  protected static final int ORIGIN_FILE_INPUT = 3;
  protected static final int ORIGIN_NBO_ARCHIVE = 4;
  
  protected int modelOrigin = ORIGIN_UNKNOWN;
  

  private static final String DEFAULT_SCRIPT = "zap;set bondpicking true;set multipleBondSpacing -0.5;set zoomlarge false;select none;";

  protected static final String INPUT_FILE_EXTENSIONS = "adf;cfi;com;g09;gau;gms;jag;log;mm2;mnd;mol;mp;nw;orc;pqs;qc;vfi;xyz;47";
  protected static final String OUTPUT_FILE_EXTENSIONS = "adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;mol;xyz;vfi;g09;com";
  protected static final String JMOL_EXTENSIONS = "xyz;mol";

  protected static final String sep = System.getProperty("line.separator");

  

  /**
   * Jmol plugin object for NBO
   * 
   */
  protected NBOPlugin nboPlugin;
  

  /**
   * MODEL VIEW ....
   */
  final static protected Font topFont = new Font("Arial", Font.BOLD, 20);
  
  /**
   * user input box .... -- should  be monospace
   */
  final static protected Font userInputFont = new Font("Arial", Font.PLAIN, 12);
  

 
  /**
   * 14 pt   M O N O S P A C E D
   */
  final static protected Font listFont = new Font("Monospaced", Font.BOLD, 14);
  
  
  /**
   * 16 pt   M O N O S P A C E D
   */
  final static protected Font monoFont = new Font("Monospaced", Font.BOLD, 16);
  
  /**
   * 18 pt Arial bold italic
   *   
   */
  final static protected Font titleFont = new Font("Arial", Font.BOLD | Font.ITALIC, 18);
  
  
  /**
   * 16 pt Arial plain for search area text
   *   
   */
  final static protected Font searchTextAreaFont = new Font("Arial",Font.PLAIN,16);
  
  /**
   * Settings and Help
   */
  final static protected Font settingHelpFont = new Font("Arial", Font.PLAIN, 14);

  /**
   * Settings and Help
   */
  final static protected Font searchOpListFont = new Font("Arial", Font.BOLD, 14);

  /**
   *  arial plain 16
   */
  final static protected Font iconFont = new Font("Arial", Font.PLAIN, 16);

  
  /**
   * 16 pt arial bold
   */
  final static protected Font nboFont = new Font("Arial", Font.BOLD, 16);
  

  /**
   * 26 pt arial bold
   */
  final static protected Font nboFontLarge = new Font("Arial", Font.BOLD, 26);

  /**
   * run button 20-pt arial plain
   */
  final static protected Font runButtonFont = new Font("Arial",Font.PLAIN,20);

  /**
   * "NBOPro6@Jmol title 26-pt arial bold
   */
  final static protected Font nboProTitleFont = new Font("Arial",Font.BOLD,26);
    
  /**
   * Jmol-style label font 16 bold
   */
  private static final String JMOL_LABEL_FONT = "16 bold";

  private static final int MODE_PATH_SERVICE = 0;
  private static final int MODE_PATH_WORKING = 1;

  final static protected Color titleColor = Color.blue;

  protected Viewer vwr;
  protected NBOService nboService;

  protected JLabel icon;

  protected JSplitPane centerPanel;
  protected JPanel modulePanel;
  
  protected boolean jmolOptionNOZAP = false; // do no zap between modules
  protected boolean jmolOptionNOSET = false; // do not use NBO settings by default
  protected boolean jmolOptionVIEW = false;  // present only the VIEW option
  protected boolean jmolOptionNONBO = false; // do not try to contact NBOServe
  
  //abstract protected void notifyLoad();
  //abstract protected void rawInput(String str);

  protected NBOFileHandler inputFileHandler;
  protected NBOFileHandler saveFileHandler;
  protected char dialogMode;
  protected boolean isOpenShell;
  protected boolean isJmolNBO;
  protected boolean haveService;

  protected JLabel statusLab;
  protected JTextPane jpNBODialog;
  protected JSlider opacity = new JSlider(0, 10);
  protected JPanel settingsPanel;
  protected JComboBox<Color> colorBox1, colorBox2;
  protected JCheckBox jCheckAtomNum, jCheckSelHalo, jCheckDebugVerbose,
      jCheckNboView, jCheckWireMesh;

  protected ChooseList chooseList;

  protected String bodyText = "";
  protected boolean showAtNum, nboView, useWireMesh;
  protected Color orbColor1, orbColor2, backgroundColor;
  protected String color1, color2;
  protected float opacityOp;

  //protected Hashtable<String, String[]> lists;

  protected boolean debugVerbose;

  protected NBODialogConfig(JFrame f) {
    super(f);
  }

  protected void alertRequiresNBOServe() {
    vwr.alert("This functionality requires NBOServe.");
  }

  protected String getJmolWorkingPath() {
    String path = JmolPanel.getJmolProperty("workingPath",
        System.getProperty("user.home"));
    saveWorkingPath(path);
    return path;
  }

  protected String getWorkingPath() {
    String path = nboPlugin.getNBOProperty("workingPath", null);
    return (path == null ? getJmolWorkingPath() : path);
  }

  protected void saveWorkingPath(String path) {
    nboPlugin.setNBOProperty("workingPath", path);
  }

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param settingsPanel
   * 
   * @return settings panel
   */
  @SuppressWarnings("unchecked")
  protected JPanel buildSettingsPanel(JPanel settingsPanel) {

    settingsPanel.removeAll();
    checkNBOStatus();
    String viewOpts = getOrbitalDisplayOptions();

    settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
    addPathSetting(settingsPanel, MODE_PATH_SERVICE);
    addPathSetting(settingsPanel, MODE_PATH_WORKING);

    //Settings

    JButton jbDefaults = new JButton("Set Defaults");
    jbDefaults.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        setDefaults(false);
      }

    });
    settingsPanel.add(createTitleBox(" Settings ", jbDefaults));
    jCheckAtomNum = new JCheckBox("Show Atom Numbers");//.setAlignmentX(0.5f);
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showAtNum = !showAtNum;
        showAtomNums(false);
      }
    });
    showAtNum = true;
    jCheckAtomNum.setSelected(true);
    Box settingsBox = createBorderBox(true);
    settingsBox.add(jCheckAtomNum);

    jCheckSelHalo = new JCheckBox("Show selection halos on atoms");
    jCheckSelHalo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        runScriptNow("select " + jCheckSelHalo.isSelected());
      }
    });
    jCheckSelHalo.doClick();
    settingsBox.add(jCheckSelHalo);

    jCheckWireMesh = new JCheckBox("Use wire mesh for orbital display");

    settingsBox.add(jCheckWireMesh);
    Color[] colors = { Color.red, Color.orange, Color.yellow, Color.green,
        Color.cyan, Color.blue, Color.magenta, };

    JPanel displayOps = new JPanel(new GridLayout(1, 4));
    JLabel label = new JLabel("     (+) color: ");
    displayOps.add(label);
    colorBox1 = new JComboBox<Color>(colors);
    colorBox1.setRenderer(new ColorRenderer());
    colorBox1.setSelectedItem(orbColor1);
    displayOps.add(colorBox1);
    colorBox1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor1 = ((Color) colorBox1.getSelectedItem());
        setOrbitalDisplayOptions();
      }
    });

    displayOps.add(new JLabel("     (-) color: "));
    colorBox2 = new JComboBox<Color>(colors);
    colorBox2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor2 = ((Color) colorBox2.getSelectedItem());
        setOrbitalDisplayOptions();
      }
    });
    colorBox2.setSelectedItem(orbColor2);
    colorBox2.setRenderer(new ColorRenderer());

    displayOps.add(colorBox2);
    displayOps.setAlignmentX(0.0f);
    settingsBox.add(displayOps);
    settingsBox.add(Box.createRigidArea(new Dimension(10, 10)));

    //Opacity slider///////////////////
    opacity.setMajorTickSpacing(1);
    opacity.setPaintTicks(true);
    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
    for (int i = 0; i <= 10; i++)
      labelTable.put(new Integer(i), new JLabel(i == 10 ? "1" : "0." + i));
    opacity.setPaintLabels(true);
    opacity.setLabelTable(labelTable);
    opacity.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        opacityOp = (float) opacity.getValue() / 10;
        setOrbitalDisplayOptions();
      }
    });
    Box opacBox = Box.createHorizontalBox();
    opacBox.add(new JLabel("Orbital opacity:  "));
    opacBox.setAlignmentX(0.0f);
    opacBox.add(opacity);
    settingsBox.add(opacBox);

    jCheckWireMesh.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        useWireMesh = !useWireMesh;
        opacity.setValue(0);
      }
    });
    if (useWireMesh)
      jCheckWireMesh.setSelected(true);

    jCheckNboView = new JCheckBox("Emulate NBO View");
    jCheckNboView.setSelected(true);
    settingsBox.add(jCheckNboView);
    jCheckNboView.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setDefaults(!((JCheckBox) e.getSource()).isSelected());
      }
    });
    jCheckDebugVerbose = new JCheckBox("Verbose Debugging");
    settingsBox.add(jCheckDebugVerbose);
    jCheckDebugVerbose.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        debugVerbose = ((JCheckBox) e.getSource()).isSelected();
      }
    });
    if (!"nboView".equals(viewOpts)) {
      jCheckNboView.doClick();
    } else {
      opacity.setValue((int) (opacityOp * 10));
    }
    settingsBox.setBorder(BorderFactory.createLineBorder(Color.black));
    settingsPanel.add(settingsBox);
    return settingsPanel;
  }

  /**
   * Allowing passage of Jmol options. Currently: NOZAP;NOSET;JMOL;VIEW
   * @param jmolOptions 
   */
  protected void setJmolOptions(Map<String, Object> jmolOptions) {
    String options = ("" + (jmolOptions == null ? "" : jmolOptions.get("options"))).toUpperCase();
    if (options.equals("VIEW"))
      options = "VIEW;NOZAP;NOSET;NONBO";
    jmolOptionVIEW = (options.indexOf("VIEW") >= 0);
    jmolOptionNOZAP = (options.indexOf("NOZAP") >= 0);
    jmolOptionNOSET = (options.indexOf("NOSET") >= 0);
    jmolOptionNONBO = (options.indexOf("NONBO") >= 0);
  }

  protected void setDefaults(boolean isJmol) {
    nboPlugin.setNBOProperty("orbitalDisplayOptions", "default");
    getOrbitalDisplayOptions();
    opacity.setValue((int) (opacityOp * 10));
    colorBox1.setSelectedItem(orbColor1);
    colorBox2.setSelectedItem(orbColor2);

    jCheckWireMesh.setSelected(useWireMesh);
    //jCheckWireMesh.doClick();
    jCheckAtomNum.setSelected(true);
    //jCheckAtomNum.doClick();
    jCheckSelHalo.setSelected(true);
    //jCheckSelHalo.doClick();
    jCheckDebugVerbose.setSelected(false);
    //jCheckDebugVerbose.doClick();
    jCheckNboView.setSelected(false);
    //jCheckNboView.doClick();

    if (isJmol) {
      if (!jmolOptionNONBO)
        runScriptNow("background gray;set defaultcolors Jmol;refresh;");
    } else {
      if (jCheckWireMesh.isSelected())
        jCheckWireMesh.doClick();
      colorBox1.setSelectedItem(Color.cyan);
      colorBox2.setSelectedItem(Color.yellow);
      opacity.setValue(3);
      try {
        String atomColors = "";
        atomColors = GuiMap.getResourceString(this,
            "org/gennbo/assets/atomColors.txt");
        runScriptNow(atomColors + ";refresh");
      } catch (IOException e) {
        log("atomColors.txt not found", 'r');
      }
      nboView = true;
    }
    updatePanelSettings();
  }

  protected void setOrbitalDisplayOptions() {
    color1 = "[" + orbColor1.getRed() + " " + orbColor1.getGreen() + " "
        + orbColor1.getBlue() + "]";
    color2 = "[" + orbColor2.getRed() + " " + orbColor2.getGreen() + " "
        + orbColor2.getBlue() + "]";
    colorMeshes();
    if (!nboView)
      nboPlugin.setNBOProperty("orbitalDisplayOptions", orbColor1.getRGB()
          + "," + orbColor2.getRGB() + "," + opacityOp + "," + useWireMesh);
  }

  private String getOrbitalDisplayOptions() {
    String options = (jmolOptionNONBO ? "jmol" : nboPlugin.getNBOProperty("orbitalDisplayOptions",
        "default"));
    if (options.equals("default") || options.equals("nboView")) {
      orbColor1 = Color.cyan;
      orbColor2 = Color.yellow;
      opacityOp = 0.3f;
      useWireMesh = false;
    } else if (options.equals("jmol")){
      orbColor1 = Color.blue;
      orbColor2 = Color.red;
      opacityOp = 0f;
      useWireMesh = true;
    } else {
      // color1, color2, useMesh
      String[] toks = options.split(",");
      orbColor1 = new Color(Integer.parseInt(toks[0]));
      orbColor2 = new Color(Integer.parseInt(toks[1]));
      opacityOp = Float.parseFloat(toks[2]);
      useWireMesh = toks[3].contains("true");
    }
    return options;
  }

  private void addPathSetting(JPanel panel, final int mode) {
    //GUI for NBO path selection
    String title = "";
    String path = "";
    switch (mode) {
    case MODE_PATH_SERVICE:
      title = " NBO Directory ";
      path = nboService.getServerPath(null);
      break;
    case MODE_PATH_WORKING:
      title = " Working Directory ";
      path = getWorkingPath();
      break;
    }
    final JTextField tfPath = new JTextField(path);
    tfPath.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        setNBOPath(tfPath, mode);
      }

    });
    panel.add(createTitleBox(title, null));
    Box serverBox = createBorderBox(true);
    serverBox.add(createPathBox(tfPath, mode));
    serverBox.setMaximumSize(new Dimension(350, 50));
    panel.add(serverBox);

  }

  protected void setNBOPath(JTextField tf, int mode) {
    String path = PT.rep(tf.getText(), "\\", "/");
    if (path.length() == 0)
      return;
    switch (mode) {
    case MODE_PATH_SERVICE:
      nboService.setServerPath(path);
      log("NBOServe location changed changed:<br> " + path, 'b');
      connect();
      break;
    case MODE_PATH_WORKING:
      if ((path + "/").equals(nboService.getServerPath(null))) {
        vwr.alert("The working directory may not be the same as the directory containing NBOServe");
        return;
      }
      nboPlugin.setNBOProperty("workingPath", path);
      log("Working path directory changed:<br> " + path, 'b');
      break;
    }
  }

  private void checkNBOStatus() {
    if (jmolOptionNONBO)
      return;
    if (nboService.restartIfNecessary()) {
    } else {
      String s = "Could not connect to NBOServe. Check to make sure its path is correct. \nIf it is already running from another instance of Jmol, you must first close that instance.";
      alertError(s);
    }

  }

  /**
   * Create a horizontal box with a text field, a
   * 
   * @param tf
   * @param mode
   *        MODE_PATH_SERVICE or MODE_PATH_WORKING
   * @return a box
   */
  private Box createPathBox(final JTextField tf, final int mode) {
    Box box = Box.createHorizontalBox();
    box.add(tf);
    JButton b = new JButton("Browse");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doPathBrowseClicked(tf, tf.getText(), mode);
      }
    });
    box.add(b);
    return box;
  }

  /**
   * Show a file selector for choosing NBOServe.exe from config.
   * 
   * @param tf
   * @param fname
   * @param mode
   */
  protected void doPathBrowseClicked(final JTextField tf, String fname, int mode) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    myChooser.setSelectedFile(new File(fname + "/ "));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      tf.setText(PT.rep(newFile.toString(), "\\", "/"));
      tf.postActionEvent();
    }
  }

  /**
   * Creates the title blocks with background color for headers.
   * 
   * @param title
   *        - title for the section
   * @param rightSideComponent
   *        help button, for example
   * @return Box formatted title box
   */
  protected static Box createTitleBox(String title, Component rightSideComponent) {
    Box box = Box.createVerticalBox();
    JLabel label = new JLabel(title);
    label.setAlignmentX(0);
    label.setBackground(titleColor);
    label.setForeground(Color.white);
    label.setFont(titleFont);
    label.setOpaque(true);
    if (rightSideComponent != null) {
      JPanel box2 = new JPanel(new BorderLayout());
      box2.setAlignmentX(0);
      box2.add(label, BorderLayout.WEST);
      box2.add(rightSideComponent, BorderLayout.EAST);
      box2.setMaximumSize(new Dimension(355, 25));
      box.add(box2);
    } else
      box.add(label);
    box.setAlignmentX(0.0f);

    return box;
  }

  /**
   * create a bordered box, either vertical or horizontal
   * 
   * @param isVertical
   * @return a box
   */
  protected Box createBorderBox(boolean isVertical) {
    Box box = isVertical ? Box.createVerticalBox() : Box.createHorizontalBox();
    box.setAlignmentX(0.0f);
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    return box;
  }

  /**
   * label atoms: (number lone pairs)+atomnum
   * 
   * @param alpha
   */
  protected void showAtomNums(boolean alpha) {
    if (!showAtNum) {
      runScriptNow("select visible;label off; select none;refresh");
      return;
    }
    SB sb = new SB();
    sb.append("select visible;font label " + JMOL_LABEL_FONT + ";label %a;");
    if (chooseList != null) {
      Hashtable<String, String> lonePairs = (alpha) ? chooseList.lonePairs
          : chooseList.lonePairs_b;
      Hashtable<String, String> loneV = chooseList.lv;
      for (int i = 1; i <= vwr.ms.ac; i++) {
        sb.append("select visible && atomno=" + i + ";label ");
        String atNum = new Integer(i).toString();
        String lp, lv;
        if ((lp = lonePairs.get(atNum)) != null)
          if (!lp.equals("0"))
            sb.append("<sup>(" + lp + ")</sup>");
        if ((lv = loneV.get(atNum)) != null)
          if (!lv.equals("0"))
            sb.append("<sub>[" + lv + "]</sub>");
        sb.append("%a;");
      }
    }
    String color = (nboView) ? "black" : "gray";
    sb.append("select visible;color labels white;" +
    		"select visible && {H*};color labels " + color + ";"
        + "set labeloffset 0 0 {visible}; select none;refresh");
    runScriptNow(sb.toString());

  }

  /**
   * Centers the dialog on the screen.
   * 
   * @param d
   */
  protected void centerDialog(JDialog d) {
    int x = getWidth() / 2 - d.getWidth() / 2 + this.getX();
    int y = getHeight() / 2 - d.getHeight() / 2;
    d.setLocation(x, y);
  }

  /**
   * appends output to session dialog panel
   * 
   * @param line
   *        output message to append
   * @param chFormat
   *        p, b, r ("red"), i, etc.
   */
  protected synchronized void log(String line, char chFormat) {
    if (dontLog(line, chFormat))
      return;
    if (line.trim().length() >= 1) {
      line = PT.rep(line.trim(), "<", "&lt;");
      line = PT.rep(line, ">", "&gt;");
      line = PT.rep(line, "&lt;br&gt;", "<br>");
      String format0 = "" + chFormat;
      String format1 = format0;
      //      String fontFamily = jpNBOLog.getFont().getFamily();
      if (chFormat == 'r') {
        format0 = "b style=color:red";
        format1 = "b";
      }

      if (!format0.equals("p"))
        line = "<" + format0 + ">" + line + "</" + format1 + ">";
      jpNBODialog.setText("<html><font face=\"Arial\">" + (bodyText = bodyText + line + "\n<br>")
          + "</font></html>");
    }
    jpNBODialog.setCaretPosition(jpNBODialog.getDocument().getLength());
  }

  private boolean dontLog(String line, char chFormat) {
    return (jpNBODialog == null || line.trim().equals("")
        || line.indexOf("read/unit=5/attempt to read past end") >= 0
        || line.indexOf("*end*") >= 0
        || !debugVerbose
        && "b|r|I".indexOf("" + chFormat) < 0);
  }

  protected void alertError(String line) {
    log(line, 'r');
    vwr.alert(line);
  }

  protected void logInfo(String msg, int mode) {
    Logger.info(msg);
    log(msg, mode == Logger.LEVEL_INFO ? 'p' : mode == Logger.LEVEL_ERROR ? 'r'
        : mode == Logger.LEVEL_WARN ? 'b' : 'i');
  }

  protected void sendDefaultScript() {
    if (!jmolOptionNOSET)
      runScriptQueued(DEFAULT_SCRIPT);
  }

  protected void runScriptQueued(String script) {
    logInfo("_$ " + PT.rep(script, "\n", "<br>"), Logger.LEVEL_DEBUG);
    vwr.script(script);
  }

  synchronized protected String runScriptNow(String script) {
    logInfo("!$ " + script, Logger.LEVEL_DEBUG);
    return PT.trim(vwr.runScript(script), "\n");
  }
  
  protected void loadModelFileQueued(File f, boolean saveOrientation,
                                     boolean isAppend) {
    String s = "load " + (isAppend ? "append " : "") + "\""
        + f.getAbsolutePath() + "\"";
    if (saveOrientation)
      s = "save orientation o1;" + s + ";restore orientation o1";
    runScriptQueued(s);
  }


  private boolean connect() {
    if (!nboService.haveGenNBO())
      return false;
    boolean isOK = checkEnabled();
    if (isOK)
      this.icon.setText("Connected");
    //appendOutputWithCaret(isOK ? "NBOServe successfully connected" : "Could not connect",'p');
    return isOK;
  }

  protected boolean checkEnabled() {
    return (jmolOptionNONBO || nboService.isEnabled() && nboService.restartIfNecessary());
  }

  protected String evaluateJmolString(String expr) {
    return vwr.evaluateExpressionAsVariable(expr).asString();
  }

  protected String getJmolFilename() {
    return evaluateJmolString("getProperty('filename')");
  }

  @SuppressWarnings("rawtypes")
  class ColorRenderer extends JButton implements ListCellRenderer {

    boolean b = false;

    public ColorRenderer() {
      setOpaque(true);
    }

    @Override
    public void setBackground(Color bg) {
      if (!b)
        return;
      super.setBackground(bg);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      b = true;
      setText(" ");
      setBackground((Color) value);
      b = false;
      return this;
    }

  }

  class HelpBtn extends JButton implements ActionListener {

    private String url;

    protected HelpBtn(String url) {
      super("Help");
      setBackground(Color.black);
      setForeground(Color.white);
      this.url = url;
      addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
      try {
        URI uri = new URI("http://nbo6.chem.wisc.edu/jmol_help/" + url);
        Desktop.getDesktop().browse(uri);
      } catch (URISyntaxException e) {
        // TODO
      } catch (IOException e) {
        // TODO
      }
    }
  }

  class StyledComboBoxUI extends MetalComboBoxUI {

    protected int height;
    protected int width;

    protected StyledComboBoxUI(int h, int w) {
      super();
      height = h;
      width = w;
    }

    @Override
    protected ComboPopup createPopup() {
      BasicComboPopup popup = new BasicComboPopup(comboBox) {
        @Override
        protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
          return super.computePopupBounds(px, py, Math.max(width, pw), height);
        }
      };
      popup.getAccessibleContext().setAccessibleParent(comboBox);
      return popup;
    }
  }

  protected void colorMeshes() {
    updatePanelSettings();
  }

}
