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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;

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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogConfig extends JDialog {

  private static final String DEFAULT_SCRIPT = "set bondpicking true;set multipleBondSpacing -0.5;set zoomlarge false;select none;";

  protected static final String INPUT_FILE_EXTENSIONS = "adf;cfi;com;g09;gau;gms;jag;log;mm2;mnd;mol;mp;nw;orc;pqs;qc;vfi;xyz;47";
  protected static final String OUTPUT_FILE_EXTENSIONS = "adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;mol;xyz;vfi;g09;com";
  protected static final String JMOL_EXTENSIONS = "xyz;mol";

  protected static final String sep = System.getProperty("line.separator");

  protected Viewer vwr;
  protected NBOService nboService;

  protected JLabel icon;

  protected JSplitPane centerPanel;
  protected JPanel modulePanel;

  //abstract protected void notifyLoad();
  //abstract protected void rawInput(String str);

  protected NBOFileHandler inputFileHandler;
  protected NBOFileHandler saveFileHandler;

  final static protected Font nboFont = new Font("Arial", Font.BOLD, 16);
  final static protected Font monoFont = new Font("Monospaced", Font.BOLD, 16);
  final static protected Font titleFont = new Font("Arial", Font.BOLD
      | Font.ITALIC, 18);

  private static final int MODE_PATH_SERVICE = 0;
  private static final int MODE_PATH_WORKING = 1;

  final static protected Color titleColor = Color.blue;

  protected JTextPane jpNBOLog;
  protected JSlider opacity = new JSlider();

  protected String bodyText = "";

  protected boolean showAtNum, nboView, useWireMesh;
  protected char dialogMode;

  protected Color orbColor1, orbColor2;
  protected String color1, color2;
  protected float opacityOp;

  //protected Hashtable<String, String[]> lists;

  protected String reqInfo;

  protected boolean debugVerbose;

  protected NBODialogConfig(JFrame f) {
    super(f);
  }

  protected String getJmolWorkingPath() {
    String path = JmolPanel.getJmolProperty("workingPath",
        System.getProperty("user.home"));
    saveWorkingPath(path);
    return path;
  }

  protected String getWorkingPath() {
    String path = nboService.getNBOProperty("workingPath", null);
    return (path == null ? getJmolWorkingPath() : path);
  }

  protected void saveWorkingPath(String path) {
    nboService.setNBOProperty("workingPath", path);
  }

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @return settings panel
   */
  @SuppressWarnings("unchecked")
  protected JPanel buildSettingsPanel() {
    checkNBOStatus();
    String viewOpts = getOrbitalDisplayOptions();

    JPanel filePanel = new JPanel();
    filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
    addPathSetting(filePanel, MODE_PATH_SERVICE);
    addPathSetting(filePanel, MODE_PATH_WORKING);

    //Settings
    filePanel.add(createTitleBox(" Settings ", null));
    JCheckBox jCheckAtomNum = new JCheckBox("Show Atom Numbers");//.setAlignmentX(0.5f);
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

    JCheckBox jCheckSelHalo = new JCheckBox("Show selection halos on atoms");
    jCheckSelHalo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        runScriptNow("select " + ((JCheckBox) e.getSource()).isSelected());
      }
    });
    jCheckSelHalo.doClick();
    settingsBox.add(jCheckSelHalo);

    color1 = "[" + orbColor1.getRed() + " " + orbColor1.getGreen() + " "
        + orbColor1.getBlue() + "]";
    color2 = "[" + orbColor2.getRed() + " " + orbColor2.getGreen() + " "
        + orbColor2.getBlue() + "]";
    final JCheckBox jCheckWireMesh = new JCheckBox(
        "Use wire mesh for orbital display");

    settingsBox.add(jCheckWireMesh);
    Color[] colors = { Color.red, Color.orange, Color.yellow, Color.green,
        Color.cyan, Color.blue, Color.magenta, };

    JPanel displayOps = new JPanel(new GridLayout(1, 4));
    displayOps.add(new JLabel("(+) color: "));
    final JComboBox<Color> colorBox1 = new JComboBox<Color>(colors);
    colorBox1.setRenderer(new ColorRenderer());
    colorBox1.setSelectedItem(orbColor1);
    displayOps.add(colorBox1);
    colorBox1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor1 = ((Color) colorBox1.getSelectedItem());
        color1 = "[" + orbColor1.getRed() + " " + orbColor1.getGreen() + " "
            + orbColor1.getBlue() + "]";
        setOrbitalDisplayOptions();
      }
    });

    displayOps.add(new JLabel("  (-) color: "));
    final JComboBox<Color> colorBox2 = new JComboBox<Color>(colors);
    colorBox2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        orbColor2 = ((Color) colorBox2.getSelectedItem());
        color2 = "[" + orbColor2.getRed() + " " + orbColor2.getGreen() + " "
            + orbColor2.getBlue() + "]";
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
    opacity.setMinimum(0);
    opacity.setMaximum(10);
    opacity.setMajorTickSpacing(1);
    opacity.setPaintTicks(true);
    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
    for (int i = 0; i < 10; i++)
      labelTable.put(new Integer(i), new JLabel("0." + i));
    labelTable.put(new Integer(10), new JLabel("1"));
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
    JCheckBox jCheckNboView = new JCheckBox("Emulate NBO View");
    settingsBox.add(jCheckNboView);
    jCheckNboView.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (((JCheckBox) e.getSource()).isSelected()) {
          setNBOColorScheme();
          colorBox1.setSelectedItem(Color.cyan);
          colorBox2.setSelectedItem(Color.yellow);
          if (jCheckWireMesh.isSelected())
            jCheckWireMesh.doClick();
          opacity.setValue(3);
        } else
          resetColorScheme();

      }
    });
    JCheckBox jb = new JCheckBox("Verbose Debugging");
    settingsBox.add(jb);
    jb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        debugVerbose = ((JCheckBox) e.getSource()).isSelected();
      }
    });
    if ("nboView".equals(viewOpts)) {
      jCheckNboView.doClick();
    } else {
      opacity.setValue((int) (opacityOp * 10));
    }
    settingsBox.setBorder(BorderFactory.createLineBorder(Color.black));
    filePanel.add(settingsBox);
    return filePanel;
  }

  protected void setOrbitalDisplayOptions() {
    String script = "nbo color " + color2 + " " + color1 + ";mo color "
        + color2 + " " + color1 + ";";
    script += (useWireMesh ? "nbo nofill mesh;mo nofill mesh"
        : "nbo fill nomesh;mo fill nomesh");
    script += ";nbo translucent " + opacityOp + ";mo translucent " + opacityOp;
    runScriptNow(script);
    if (!nboView)
      nboService.setNBOProperty("orbitalDisplayOptions", orbColor1.getRGB() + ","
          + orbColor2.getRGB() + "," + opacityOp + "," + useWireMesh);
  }

  private String getOrbitalDisplayOptions() {
    String options = nboService.getNBOProperty("orbitalDisplayOptions", null);
    if (options == null) {
      orbColor1 = Color.blue;
      orbColor2 = Color.red;
      opacityOp = 0;
      useWireMesh = true;
    } else if (options.equals("nboView")) {
      orbColor1 = Color.cyan;
      orbColor2 = Color.yellow;
      opacityOp = 0.3f;
      useWireMesh = false;
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

  private void addPathSetting(JPanel filePanel, final int mode) {
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
    filePanel.add(createTitleBox(title, null));
    Box serverBox = createBorderBox(true);
    serverBox.add(createPathBox(tfPath, mode));
    serverBox.setMaximumSize(new Dimension(350, 50));
    filePanel.add(serverBox);

    //Job files
    //    
    //    JLabel label = new JLabel(
    //        "(This directory must be different than NBOServe's directory)");
    //    label.setAlignmentX(0.5f);
    //    workBox.add(label);
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
      nboService.setNBOProperty("workingPath", path);
      log("Working path directory changed:<br> " + path, 'b');
      break;
    }
  }

  private void checkNBOStatus() {
    //    final JLabel lab = new JLabel();
    //    lab.setAlignmentX(0.5f);
    if (nboService.restartIfNecessary()) {
      //      lab.setText("NBOServe connected successfully");
      //      lab.setForeground(Color.black);
    } else {
      String s = "Could not connect to NBOServe. Check to make sure its path is correct. \nIf it is already running from another instance of Jmol, you must first close that instance.";
      //      lab.setText(s);
      //      lab.setForeground(Color.red);
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
    String exe = "";
    switch (mode) {
    case MODE_PATH_SERVICE:
      exe = "exe";
      myChooser.setFileFilter(new FileNameExtensionFilter(exe, exe));
      myChooser.setFileHidingEnabled(true);
      myChooser.setSelectedFile(new File(fname));
      break;
    case MODE_PATH_WORKING:
      myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      myChooser.setSelectedFile(new File(fname + "/ "));
      break;
    }
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      tf.setText(PT.rep(newFile.toString(), "\\", "/"));
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
   * Sets color scheme to emulate look of NBO view
   */
  protected void setNBOColorScheme() {
    nboView = true;
    String atomColors = "";
    String fname = "org/openscience/jmol/app/nbo/help/atomColors.txt";
    try {
      atomColors = GuiMap.getResourceString(this, fname);
    } catch (IOException e) {
      alertError("Atom colors not found");
    }
    runScriptNow(atomColors + ";refresh");
  }

  /**
   * Resets Jmol look and feel
   */
  protected void resetColorScheme() {
    nboView = false;
    runScriptNow("background black;set defaultcolors Jmol;refresh;");
  }

  //  /**
  //   * sets components visible recursively
  //   * @param c
  //   * @param b
  //   */
  //  protected void enableComponentsR(Component c,boolean b){
  //    c.setEnabled(b);
  //    if(c instanceof Container){
  //      if(!(c instanceof JComboBox)) c.setVisible(true);
  //      for(Component c2:((Container) c).getComponents())
  //        enableComponentsR(c2,b);
  //    }
  //  }

  /**
   * label atoms: (number lone pairs)+atomnum
   * 
   * @param alpha
   */
  protected void showAtomNums(boolean alpha) {
    if (!showAtNum) {
      runScriptNow("select {*};label off; select remove {*}");
      return;
    }
    SB sb = new SB();
    sb.append("select {*};label %a;");
    String color = "black";
    if (!nboView)
      color = "gray";

    sb.append("select {*};color labels white;");
    sb.append("select {H*};color labels " + color + ";"
        + "set labeloffset 0 0 {*}; select remove {*};");

    runScriptNow(sb.toString());
  }

  //  protected void rawCmd(String name, final String cmd, final int mode) {
  //    nboService.queueJob(name, null, new Runnable() {
  //      @Override
  //      public void run() {
  //        nboService.rawCmdNew(cmd, null, false, mode);
  //      }
  //    });
  //  }

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

  //  /**
  //   * Retrieve and cache a help string.
  //   *  
  //   * @param st
  //   * 
  //   */
  //  synchronized protected void getHelp(String st) {
  //    JDialog help = new JDialog(this, "NBO Help");
  //    JTextPane p = new JTextPane();
  //    p.setEditable(false);
  //    p.setFont(new Font("Arial", Font.PLAIN, 16));
  //    p.setText(getHelpContents(st));
  //    JScrollPane sp = new JScrollPane();
  //    sp.getViewport().add(p);
  //    help.add(sp);
  //    help.setSize(new Dimension(400, 400));
  //    p.setCaretPosition(0);
  //    centerDialog(help);
  //    help.setVisible(true);
  //    //return getHelpContents(htHelp.get(c));
  //  }
  //  
  //  protected String getHelpContents(String s){
  //    String help = "<error>";
  //    try {
  //      String fname = "org/openscience/jmol/app/nbo/help/" + s + ".txt";
  //      help = GuiMap.getResourceString(this, fname);
  //    } catch (IOException e) {
  //      help = "<resource not found>";
  //    }
  //    return help;
  //  }

  /**
   * appends output to session dialog panel
   * 
   * @param line
   *        output message to append
   * @param chFormat
   *        p, b, r ("red"), i, etc.
   */
  protected synchronized void log(String line, char chFormat) {
    if (line.trim().equals("") || jpNBOLog == null || !debugVerbose
        && "b|r".indexOf("" + chFormat) < 0)
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
      jpNBOLog.setText("<html>" + (bodyText = bodyText + line + "\n<br>")
          + "</html>");
    }
    jpNBOLog.setCaretPosition(jpNBOLog.getDocument().getLength());
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

  private boolean connect() {
    if (!nboService.connect())
      return false;
    boolean isOK = checkEnabled();
    if (isOK)
      this.icon.setText("Connected");
    //appendOutputWithCaret(isOK ? "NBOServe successfully connected" : "Could not connect",'p');
    return isOK;
  }

  protected boolean checkEnabled() {
    return (nboService.isEnabled() && nboService.restartIfNecessary());
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

  protected void loadModelFileQueued(File f, boolean saveOrientation,
                                     boolean isAppend) {
    String s = "load " + (isAppend ? "append " : "") + "\""
        + f.getAbsolutePath() + "\"";
    if (saveOrientation)
      s = "save orientation o1;" + s + ";restore orientation o1";
    runScriptQueued(s);
  }

  protected void colorMeshes() {
    runScriptNow((useWireMesh ? "nbo mesh nofill translucent " + opacityOp
        + ";mo mesh nofill translucent " + opacityOp
        : "nbo nomesh fill translucent " + opacityOp
            + ";mo nomesh fill translucent " + opacityOp)
        + ";nbo color "
        + color2
        + " "
        + color1
        + ";mo color "
        + color2
        + " "
        + color1);
  }

}
