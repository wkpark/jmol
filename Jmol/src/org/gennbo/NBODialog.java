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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javajs.swing.SwingConstants;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.c.CBK;
import org.jmol.java.BS;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

/**
 * A dialog for interacting with NBOServer
 * 
 * MEMO: all save/restore orientation is turned off.
 * 
 */
public class NBODialog extends JDialog {

  protected JLabel licenseInfo;

  private JButton helpBtn;

  private JDialog settingsDialog;
  private JPanel topPanel;
  protected JButton modelButton, runButton, viewButton, searchButton;
  protected JPanel settingsPanel;

  private JPanel homePanel;

  protected JPanel nboOutput;

  protected String lastOutputSaveName;

  protected NBOModel modelPanel;

  protected NBORun runPanel;

  private NBOSearch searchPanel;

  protected NBOView viewPanel;

  protected JPanel viewSettingsBox;


  protected static final int DIALOG_HOME = 0;
  protected static final int DIALOG_MODEL = 1;
  protected static final int DIALOG_RUN = 2;
  protected static final int DIALOG_VIEW = 3;
  protected static final int DIALOG_SEARCH = 4;
  protected static final int DIALOG_CONFIG = 5;
  protected static final int DIALOG_HELP = 6;
  
  final private static String[] dialogNames = new String[] { "Home", "Model",
      "Run", "View", "Search", "Settings", "Help" };

  protected static String getDialogName(int type) {
    return dialogNames[type];
  }

  protected static final int ORIGIN_UNKNOWN = 0;
  protected static final int ORIGIN_NIH = 1;
  protected static final int ORIGIN_LINE_FORMULA = 2;
  protected static final int ORIGIN_FILE_INPUT = 3;
  protected static final int ORIGIN_NBO_ARCHIVE = 4;

  protected int modelOrigin = ORIGIN_UNKNOWN;


  
  protected boolean jmolOptionNOZAP = false; // do no zap between modules
  protected boolean jmolOptionNOSET = false; // do not use NBO settings by default
  protected boolean jmolOptionVIEW = false; // present only the VIEW option
  protected boolean jmolOptionNONBO = false; // do not try to contact NBOServe

  
  /**
   * Allowing passage of Jmol options. Currently: NOZAP;NOSET;JMOL;VIEW
   * 
   * @param jmolOptions
   */
  protected void setJmolOptions(Map<String, Object> jmolOptions) {
    String options = ("" + (jmolOptions == null ? "" : jmolOptions
        .get("options"))).toUpperCase();
    if (options.equals("VIEW"))
      options = "VIEW;NOZAP;NOSET;NONBO";
    jmolOptionVIEW = (options.indexOf("VIEW") >= 0);
    jmolOptionNOZAP = (options.indexOf("NOZAP") >= 0);
    jmolOptionNOSET = (options.indexOf("NOSET") >= 0);
    jmolOptionNONBO = (options.indexOf("NONBO") >= 0);
  }

  protected Viewer vwr;
  protected NBOService nboService;

  /**
   * Jmol plugin object for NBO
   * 
   */
  protected NBOPlugin nboPlugin;

  // private/protected variables

  /**
   * Tracks the last resonance structure type (nrtstra, nrtstrb, alpha, beta);
   * reset to “alpha” by openPanel()
   */
  private String rsTypeLast = "alpha";

  /**
   * String value of what is showing in the session dialog -- persistent
   */
  protected String nboOutputBodyText = "";
  
  /**
   * The input file handler; recreated via openPanel()
   */
  protected NBOFileHandler inputFileHandler;

  protected JLabel icon;
  protected JSplitPane centerPanel;
  protected JPanel modulePanel;

  protected JLabel statusLab;
  protected JTextPane jpNBODialog;

  
  /**
   * true if NBOServe has successfully restarted-- persistent
   */
  protected boolean haveService;

  /**
   * the dialog that is currently open, for example DIALOG_MODEL-- persistent
   */
  protected int dialogMode;
  
  /**
   * configuration information source
   */
  protected NBOConfig config;  
  

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param jmolFrame
   *        The Jmol frame associated with the dialog
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   * @param plugin
   * @param jmolOptions
   */
  public NBODialog(NBOPlugin plugin, JFrame jmolFrame, Viewer vwr,
      Map<String, Object> jmolOptions) {
    super(jmolFrame);
    setName(plugin.getName());
    setTitle(getName() + " " + plugin.getVersion());
    setJmolOptions(jmolOptions);
    this.vwr = vwr;
    nboPlugin = plugin;
    nboService = new NBOService(this, vwr, !jmolOptionNONBO);
    config = new NBOConfig(this);
    setIconImage(nboPlugin.getIcon("nbo6logo20x20").getImage());
    setLayout(new BorderLayout());
      if (!jmolOptionNOSET)
        runScriptQueued(NBOConfig.DEFAULT_SCRIPT);

    modelPanel = new NBOModel(this);
    runPanel = new NBORun(this);
    viewPanel = searchPanel = new NBOSearch(this);
    
    createDialog(jmolFrame);

    if (!jmolOptionNOSET)
      config.doSetDefaults(false);


    if (jmolOptionVIEW || jmolOptionNONBO)
      doOpenPanel(DIALOG_VIEW);
  }

  protected void doOpenPanel(int type) {
    if (jmolOptionNONBO && "rsm".indexOf("" + type) >= 0) {
      vwr.alert("This option requires NBOServe");
      return;
    }
    if (type == DIALOG_CONFIG) {
      settingsDialog.setVisible(true);
      return;
    }

    if (nboService.isWorking()) {
      int i = JOptionPane.showConfirmDialog(this,
          "NBOServe is working. Cancel current job?\n"
              + "This could affect input/output files\n"
              + "if GenNBO is running.", "Message", JOptionPane.YES_NO_OPTION);
      if (i == JOptionPane.NO_OPTION) {
        return;
      }
    }

    logCmd("Entering " + getDialogName(type));

    nboService.restart();
    //  nboService.restartIfNecessary();
    nboService.clearQueue();

    if (!checkEnabled()) {
      doOpenPanel(DIALOG_CONFIG);
      return;
    }

    if (type != DIALOG_CONFIG) {
      if (dialogMode == DIALOG_HOME) {
        remove(homePanel);
        add(centerPanel, BorderLayout.CENTER);
      }
      resetModuleVariables();
    }
    viewSettingsBox.setVisible(false);

    if (topPanel != null)
      topPanel.remove(icon);

    switch (dialogMode = type) {
    case DIALOG_CONFIG:
      break;
    case DIALOG_MODEL:
      centerPanel.setLeftComponent(modulePanel = modelPanel.buildModelPanel());
      icon = new JLabel(nboPlugin.getIcon("nbomodel_logo"));
      setThis(modelButton);
      break;
    case DIALOG_RUN:
      centerPanel.setLeftComponent(modulePanel = runPanel.buildRunPanel());
      icon = new JLabel(nboPlugin.getIcon("nborun_logo"));
      setThis(runButton);
      break;
    case DIALOG_VIEW:
      centerPanel.setLeftComponent(modulePanel = viewPanel.buildViewPanel());
      icon = new JLabel(nboPlugin.getIcon("nboview_logo"));
      setThis(viewButton);
      break;
    case DIALOG_SEARCH:
      centerPanel.setLeftComponent(modulePanel = searchPanel.buildSearchPanel());
      //settingsBox.setVisible(true);
      icon = new JLabel(nboPlugin.getIcon("nbosearch_logo"));
      setThis(searchButton);
      break;
    }
    resetDivider();
    if (topPanel != null)
      topPanel.add(icon, BorderLayout.EAST);
    setStatus("");
    invalidate();
    setVisible(true);
    runScriptQueued(jmolOptionNOZAP ? "select none" : "zap");
  }

  /**
   * Rest all variables that might be an issue
   * 
   */
  private void resetModuleVariables() {

    // Anything here that looks like it needs resetting prior to changing panels.

    viewPanel.resetCurrentOrbitalClicked();
    resetVariables_c();
    
  }

  private void createDialog(JFrame jmolFrame) {
    dialogMode = DIALOG_HOME;
    Rectangle bounds = jmolFrame.getBounds();
    if (bounds.height < 630)
      jmolFrame.setSize(bounds.width, 630);
    // createDialog(Math.max(570, 615);
    setBounds(bounds.x + bounds.width, bounds.y, 650,
        Math.max(bounds.height, 630));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        nboService.closeProcess(false);
        close();
      }
    });

    placeNBODialog(this);
    licenseInfo = new JLabel("License not found", SwingConstants.CENTER);
    //licenseInfo.setBackground(null);

    licenseInfo.setOpaque(true);
    licenseInfo.setForeground(Color.white);
    licenseInfo.setBackground(Color.black);

    nboOutput();
    centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JPanel(),
        nboOutput);
    topPanel = buildTopPanel();
    getContentPane().add((homePanel = mainPage()), BorderLayout.CENTER);
    getContentPane().add(licenseInfo, BorderLayout.SOUTH);
    resetDivider();

    //centerPanel.setLeftComponent(mainPage());
    this.dialogMode = DIALOG_HOME;
    this.getContentPane().add(topPanel, BorderLayout.NORTH);
    settingsDialog = new JDialog(this, "Settings");
    settingsDialog.setSize(new Dimension(350, 400));
    settingsDialog.setLocation(this.getX() + 100, this.getY() + 100);
    settingsPanel = new JPanel();
    config.buildSettingsPanel(settingsPanel);
    settingsDialog.add(settingsPanel);
    this.setVisible(true);
    if (!jmolOptionNONBO && nboService.isOffLine())
      settingsDialog.setVisible(true);
  }

  /**
   * Places main dialog adjacent to main jmol window
   * 
   * @param d
   */
  private void placeNBODialog(JDialog d) {
    Dimension screenSize = d.getToolkit().getScreenSize();
    Dimension size = d.getSize();
    int x = Math.min(screenSize.width - size.width, d.getParent().getX()
        + d.getParent().getWidth()) - 10;
    int y = d.getParent().getY();
    //System.out.println("------" + x + "   " + y);
    d.setLocation(x, y);
  }

  private JButton getMainButton(final JButton b, final int mode, Font font) {
    b.setBorder(null);
    b.setMargin(new Insets(5, 5, 5, 5));
    b.setContentAreaFilled(false);
    b.setForeground(Color.white);
    b.setFont(font);
    switch (mode) {
    case DIALOG_HELP:
    case DIALOG_HOME:
      break;
    default:
      b.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doOpenPanel(mode);
        }
      });
    }
    return b;
  }

  /**
   * Top panel with logo/modules/file choosing options
   * 
   * @return top panel
   */
  private JPanel buildTopPanel() {

    modelButton = new JButton("Model");
    runButton = new JButton("Run");
    viewButton = new JButton("View");
    searchButton = new JButton("Search");
    helpBtn = new HelpBtn(null);

    JPanel p = new JPanel(new BorderLayout());
    if (!jmolOptionNONBO) {
      Box b = Box.createHorizontalBox();
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(modelButton, DIALOG_MODEL, NBOConfig.topFont));
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(runButton, DIALOG_RUN, NBOConfig.topFont));
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(viewButton, DIALOG_VIEW, NBOConfig.topFont));
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(searchButton, DIALOG_SEARCH, NBOConfig.topFont));
      b.add(Box.createRigidArea(new Dimension(30, 50)));
      b.add(getMainButton(new JButton("Settings"), DIALOG_CONFIG,
          NBOConfig.settingHelpFont));
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(helpBtn, DIALOG_HELP, NBOConfig.settingHelpFont));
      b.add(Box.createRigidArea(new Dimension(20, 0)));
      b.add(getMainButton(new HelpBtn("Manual", "NBOPro6_man.pdf", "NBOPro6"), DIALOG_HOME, NBOConfig.settingHelpFont));
      p.add(b, BorderLayout.CENTER);
    }
    icon = new JLabel();
    icon.setFont(NBOConfig.nboFont);
    icon.setForeground(Color.white);
    p.add(icon, BorderLayout.EAST);
    p.setBackground(Color.BLACK);
    p.setPreferredSize(new Dimension(500, 60));
    return p;
  }

  /**
   * sets components colors in container recursively
   * 
   * @param comp
   * @param foregroundColor
   * @param backgroundColor
   */
  private void setComponents(Component comp, Color foregroundColor,
                             Color backgroundColor) {
    if (comp instanceof JTextField || comp instanceof JTextPane
        || comp instanceof JButton)
      return;
    if (comp instanceof JComboBox)
      comp.setBackground(new Color(248, 248, 248));
    if (foregroundColor != null)
      comp.setForeground(foregroundColor);
    if (backgroundColor != null)
      comp.setBackground(backgroundColor);
    if (comp instanceof Container) {
      for (Component c : ((Container) comp).getComponents()) {
        setComponents(c, foregroundColor, backgroundColor);
      }
    }
  }

  private JPanel mainPage() {
    JPanel p = new JPanel();
    p.setBackground(Color.white);

    haveService = nboService.restartIfNecessary(); // BH temporarily

    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
    //Header stuff////////////
    ImageIcon imageIcon = nboPlugin.getIcon("nbo6logo");

    Image image = imageIcon.getImage();
    Image newimg = image.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
    imageIcon = new ImageIcon(newimg);
    JLabel lab = new JLabel(imageIcon);
    Box b = Box.createHorizontalBox();

    b.add(lab);
    lab = new JLabel("NBOServe (v6) toolbox");
    b.add(lab);
    b.add(Box.createRigidArea(new Dimension(370, 0)));
    icon.setOpaque(true);
    icon.setBackground(Color.LIGHT_GRAY);
    icon.setText(haveService ? "  Connected  "
        : "<html><center>Not<br>Connected</center></html>");
    icon.setForeground(haveService ? Color.black : Color.red);
    icon.setBorder(BorderFactory.createLineBorder(Color.black));

    p.add(b);
    lab = new JLabel(getName());
    lab.setFont(NBOConfig.nboProTitleFont);
    lab.setForeground(Color.red);
    p.add(lab);
    lab.setAlignmentX(0.5f);
    lab = new JLabel("Frank Weinhold, Dylan Phillips, and Bob Hanson");
    lab.setAlignmentX(0.5f);
    p.add(lab);
    //Body/////////////
    JPanel p2 = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    p2.setBorder(BorderFactory.createLineBorder(Color.black));
    JButton btn = new JButton("Model");

    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150, 30));
    btn.setFont(NBOConfig.homeButtonFont);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doOpenPanel(DIALOG_MODEL);
      }
    });
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    p2.add(btn, c);
    c.gridx = 1;
    c.gridy = 0;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Create & edit molecular model and input files"),
        c);

    c.gridx = 5;
    c.gridwidth = 1;
    p2.add(Box.createRigidArea(new Dimension(60, 10)), c);
    lab.setFont(NBOConfig.homeTextFont);
    JTextPane tp = new JTextPane();
    tp.setContentType("text/html");
    tp.setText("<HTML><center>Frank Weinhold<br><I>(Acknowledgments: Eric Glendening, John Carpenter, "
        + "Mark Muyskens, Isaac Mades, Scott Ostrander, John Blair, Craig Weinhold)</I></center></HTML>");
    tp.setEditable(false);
    tp.setBackground(null);
    tp.setPreferredSize(new Dimension(430, 60));
    c.gridx = 1;
    c.gridy = 1;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.HORIZONTAL;
    p2.add(tp, c);

    c.weightx = 0;

    //RUN/////////////
    btn = new JButton("Run");
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150, 30));
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        doOpenPanel(DIALOG_RUN);
      }
    });
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 1;
    btn.setFont(NBOConfig.homeButtonFont);
    p2.add(btn, c);
    c.gridx = 1;
    c.gridy = 2;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Launch NBO analysis for chosen archive file"), c);
    lab.setFont(NBOConfig.homeTextFont);
    tp = new JTextPane();
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Eric Glendening, Jay Badenhoop, Alan Reed, John Carpenter, Jon Bohmann, "
        + "Christine Morales, and Frank Weinhold</center></HTML>");
    c.gridx = 1;
    c.gridy = 3;
    c.gridwidth = 3;
    p2.add(tp, c);

    //VIEW//////////////
    btn = new JButton("View");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        doOpenPanel(DIALOG_VIEW);
      }
    });
    btn.setFont(NBOConfig.homeButtonFont);
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150, 30));
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 1;
    p2.add(btn, c);
    c.gridx = 1;
    c.gridy = 4;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Display NBO orbitals in 1D/2D/3D imagery"), c);
    lab.setFont(NBOConfig.homeTextFont);
    tp = new JTextPane();
    tp.setMaximumSize(new Dimension(430, 60));
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Mark Wendt and Frank Weinhold<br><I> (Acknowledgments: Eric Glendening, John Carpenter, "
        + "Mark Muyskens, Scott Ostrander, Zdenek Havlas, Dave Anderson)</I></center></HTML>");
    c.gridx = 1;
    c.gridy = 5;
    c.gridwidth = 3;
    p2.add(tp, c);

    //SEARCH/////////////
    btn = new JButton("Search");
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150, 30));
    btn.setFont(NBOConfig.homeButtonFont);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        doOpenPanel(DIALOG_SEARCH);
      }
    });
    c.gridx = 0;
    c.gridy = 6;
    c.gridwidth = 1;
    p2.add(btn, c);
    c.gridx = 1;
    c.gridy = 6;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Search NBO output interactively"), c);
    lab.setFont(NBOConfig.homeTextFont);
    tp = new JTextPane();
    tp.setMaximumSize(new Dimension(430, 60));
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Frank Weinhold</center></HTML>");
    c.gridx = 1;
    c.gridy = 7;
    c.gridwidth = 3;
    p2.add(tp, c);
    p.add(p2);
    JTextPane t = new JTextPane();
    t.setContentType("text/html");
    t.setText("<HTML><Font color=\"RED\"><center>\u00a9Copyright 2017 Board of Regents of the University of Wisconsin System "
        + "on behalf of \nthe Theoretical Chemistry Institute.  All Rights Reserved</center></font></HTML>");

    t.setForeground(Color.RED);
    t.setBackground(null);
    t.setAlignmentX(0.5f);
    t.setMaximumSize(new Dimension(10000, 80));
    p.add(t);
    return p;
  }

  private void resetDivider() {
    centerPanel.setDividerLocation(365);
  }

  private void nboOutput() {
    nboOutput = new JPanel(new BorderLayout());
    viewSettingsBox = new JPanel(new BorderLayout());
    viewSettingsBox.add(new JLabel("Settings"), BorderLayout.NORTH);
    JPanel s = new JPanel(new BorderLayout());

    s.add(viewSettingsBox, BorderLayout.NORTH);
    viewSettingsBox.setVisible(!jmolOptionNONBO);
    nboOutput.add(viewSettingsBox, BorderLayout.NORTH);
    nboOutput.add(s, BorderLayout.CENTER);
    JLabel lab = new JLabel("Session Dialog");
    lab.setFont(NBOConfig.monoFont);
    s.add(lab, BorderLayout.PAGE_START);
    JScrollPane p1 = new JScrollPane();
    if (jpNBODialog == null) {
      jpNBODialog = new JTextPane();
      jpNBODialog.setEditable(false);
      jpNBODialog.setBorder(null);
      //jpNBODialog.setFont(new Font("Arial", Font.PLAIN, 16));
      nboOutputBodyText = "";
    }
    jpNBODialog.setContentType("text/html");
    //jpNBODialog.setFont(new Font("Arial",Font.PLAIN,10));
    setComponents(s, Color.WHITE, Color.BLACK);
    p1.getViewport().add(jpNBODialog);
    p1.setBorder(null);
    s.add(p1, BorderLayout.CENTER);
    JPanel box = new JPanel(new GridLayout(2, 1));
    statusLab = new JLabel();
    statusLab.setForeground(Color.red);
    statusLab.setBackground(Color.white);
    statusLab.setFont(NBOConfig.statusFont);
    statusLab.setOpaque(true);
    box.add(statusLab);
    Box box2 = Box.createHorizontalBox();
    JButton clear = new JButton("Clear");
    clear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doClearOutput();
      }
    });
    box2.add(clear);
    JButton btn = new JButton("Save Output");

    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {        
        doSaveOutput();
      }
    });
    box2.add(btn);
    box.add(box2);
    s.add(box, BorderLayout.SOUTH);
  }

  protected void doSaveOutput() {
    if (jpNBODialog == null)
      return;
    JFileChooser myChooser = new JFileChooser();
    if (lastOutputSaveName == null)
      lastOutputSaveName = inputFileHandler.tfDir.getText()
          + "/nboDialog.txt";
    String savePath = lastOutputSaveName;
    myChooser.setSelectedFile(new File(savePath));

    myChooser.setFileFilter(new FileNameExtensionFilter(".txt", ".txt"));
    myChooser.setFileHidingEnabled(true);

    int button = myChooser.showSaveDialog(jpNBODialog);
    if (button == JFileChooser.APPROVE_OPTION) {
      saveDialogOutput(myChooser.getSelectedFile().toString());
    }
  }

  protected void saveDialogOutput(String saveFileName) {
    String output = nboOutputBodyText.replaceAll("<br>", NBOUtil.sep);
    output = output.replaceAll("<b>", "");
    output = output.replaceAll("</b>", "");
    output = output.replaceAll("<i>", "");
    output = output.replaceAll("</i>", "");
    inputFileHandler.writeToFile(saveFileName, output);
  }

  public void close() {
    if (modulePanel != null)
      inputFileHandler.clearInputFile(false);
    runScriptNow("select off");
    dispose();
  }

  private void setThis(JButton btn) {
    for (Component c : ((Container) topPanel.getComponent(0)).getComponents()) {
      if (c instanceof JButton) {
        if (((JButton) c).equals(btn)) {
          btn.setEnabled(false);
          btn.setBorder(new LineBorder(Color.WHITE, 2));
        } else {
          ((JButton) c).setBorder(null);
          ((JButton) c).setEnabled(true);
        }
      }
    }
    invalidate();
  }

  /**
   * Callback from Jmol Viewer indicating user actions
   * 
   * @param type
   * @param data
   */
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(CBK type, Object[] data) {
    //System.out.println(type + "" +  data[1]);
    if (!isVisible())
      return;
    switch (type) {
    case STRUCTUREMODIFIED:
      if (dialogMode == DIALOG_MODEL) {
      }
      break;
    case PICK:
      int[] picked = NBOUtil.getAtomsPicked(data);
      if (picked[0] < 0)
        return; // not an atom or a bond -- maybe draw picking is on
      switch (dialogMode) {
      case DIALOG_MODEL:
        modelPanel.notifyPick(picked);
        return;
      case DIALOG_VIEW:
        viewPanel.notifyPick(picked);
        return;
      case DIALOG_SEARCH:
        searchPanel.notifyPick(picked);
        return;
      }
      break;
    case LOADSTRUCT:
      if (vwr.ms.ac == 0)
        return;
      String f = "" + vwr.getParameter("_modelFile");
      if (!iAmLoading) {
        if (!f.endsWith(".47")) {
          if (dialogMode != DIALOG_MODEL) {
            doOpenPanel(DIALOG_MODEL);
            return;
          }
        }
        if (dialogMode == DIALOG_MODEL) {
          modelPanel.setModelNotFromNBO();
        } else {
          if (dialogMode != DIALOG_RUN) {
            doOpenPanel(DIALOG_RUN);
            return;
          }
          if (f.startsWith("http")) {
            runPanel.retrieveFile(f, null);
            return;
          }
          if (f.startsWith("file:"))
            f = PT.trim(f.substring(5), "/");
          if (!f.contains(":"))
            f = "/" + f;
          inputFileHandler.loadSelectedFile(new File(f));
          return;
        }
      }

      iAmLoading = false;
      if (NBOConfig.nboView)
        runScriptNow("select add visible.bonds;color bonds lightgrey;"
            + "wireframe 0.1;select none");
      switch (dialogMode) {
      case DIALOG_MODEL:
        modelPanel.notifyLoad();
        return;
      case DIALOG_RUN:
        runPanel.notifyLoad();
        return;
      case DIALOG_VIEW:
        viewPanel.notifyLoad_v();
        return;
      case DIALOG_SEARCH:
        searchPanel.notifyLoad();
        return;
      }
      break;
    }
  }

  /**
   * clear output panel
   */
  protected void doClearOutput() {
    nboOutputBodyText = "";
    // String fontFamily = jpNBOLog.getFont().getFamily();
    if (jpNBODialog != null)
      jpNBODialog.setText("");
  }

  //  protected boolean checkJmolNBO(){
  //    return(vwr.ms.getInfo(vwr.am.cmi, "nboType") != null ||
  //        NBOFileHandler.getExt(inputFileHandler.inputFile).equals("47"));
  //  }

  /**
   * user has made changes to the settings, so we need to update panels
   */
  protected void updatePanelSettings() {
    switch (dialogMode) {
    case DIALOG_VIEW:
      viewPanel.doSetNewBasis(false);
      break;
    }
  }

  /**
   * Carry out all functions to load a new file or basis
   * 
   * @param file
   */
  void loadOrSetBasis(File file) {
    viewPanel.isNewModel = true;
    switch (dialogMode) {
    case DIALOG_VIEW:
      viewPanel.setViewerBasis();
      break;
    default:
      loadModelFileQueued(file, false);
    }
  }

  void setLicense(String line) {
    licenseInfo.setText("<html><div style='text-align: center'>" + line
        + "</html>");
  }

  //  protected Component getComponentatPoint(Point p, Component top){
  //    Component c = null;
  //    if(top.isShowing()) {
  //      do{
  //        c = ((Container) top).findComponentAt(p);
  //      }while(!(c instanceof Container));
  //    }
  //    return c;
  //  }

  protected void setStatus(String statusInfo) {
    boolean isBusy = (statusInfo != null && statusInfo.length() > 0);
    statusLab.setText(statusInfo);
    centerPanel.setCursor(Cursor
        .getPredefinedCursor(isBusy ? Cursor.WAIT_CURSOR
            : Cursor.DEFAULT_CURSOR));
    if (isBusy) {
      logStatus(statusInfo);
      if (statusInfo.indexOf("...") >= 0) {
        if (runTimer != null) {
          runTimer.stop();
        }
        runTimer = new Timer(1000, new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            doShowRunTime();
          }

        });
        runStartTime = System.currentTimeMillis();
        runTimer.setRepeats(true);
        runTimer.start();
      }
    } else if (runTimer != null) {
      runTimer.stop();
      runTimer = null;
    }
  }

  Timer runTimer;
  private long runStartTime;

  protected synchronized void doShowRunTime() {
    String t = statusLab.getText();
    int pt = t.indexOf("...");
    if (pt < 0)
      return;
    try {
      int time = (int) (System.currentTimeMillis() - runStartTime);
      int minutes = time / 60000;
      int seconds = (time % 60000) / 1000;
      String s = "00" + seconds;
      s = minutes + ":" + s.substring(s.length() - 2);
      statusLab.setText(t.substring(0, pt + 3) + " " + s);
    } catch (Exception e) {
      if (runTimer != null) {
        runTimer.stop();
        runTimer = null;
      }

    }
  }

  /**
   * add selection halos to atoms in s
   * 
   * @param s
   *        - array containing atomnums
   */
  protected void showSelected(String s) {
    BS bs = new BS();
    for (String x : PT.getTokens(s))
      bs.set((Integer.parseInt(x) - 1));
    String script = "select on " + bs + ";";
    runScriptQueued(script);
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

  protected static void colorMeshes() {
    // yeiks! causes file load again! updatePanelSettings();
  }

  protected void resetVariables_c() {
    rsTypeLast = "alpha";
  }

  protected void alertRequiresNBOServe() {
    vwr.alert("This functionality requires NBOServe.");
  }


  protected void logCmd(String msg) {
    log(msg, 'I');
  }

  protected void logValue(String msg) {
    log(msg, 'b');
  }

  protected void logStatus(String msg) {
    log(msg, 'p');
  }

  protected void logError(String msg) {
    log(msg, 'r');
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
        setStatus("");
      }

      if (!format0.equals("p"))
        line = "<" + format0 + ">" + line + "</" + format1 + ">";
      jpNBODialog.setText("<html><font face=\"Arial\">"
          + (nboOutputBodyText = nboOutputBodyText + line + "\n<br>")
          + "</font></html>");
    }
    jpNBODialog.setCaretPosition(jpNBODialog.getDocument().getLength());
  }

  private boolean dontLog(String line, char chFormat) {
    return (jpNBODialog == null || line.trim().equals("")
        || line.indexOf("read/unit=5/attempt to read past end") >= 0
        || line.indexOf("*end*") >= 0 || !NBOConfig.debugVerbose
        && "b|r|I".indexOf("" + chFormat) < 0);
  }

  protected void alertError(String line) {
    line = PT.rep(line.replace('\r', ' '), "\n\n", "\n");
    logError(line);
    vwr.alert(line);
  }

  protected void logInfo(String msg, int mode) {
    Logger.info(msg);
    log(msg, mode == Logger.LEVEL_INFO ? 'p' : mode == Logger.LEVEL_ERROR ? 'r'
        : mode == Logger.LEVEL_WARN ? 'b' : 'i');
  }

  protected void runScriptQueued(String script) {
    logInfo("_$ " + PT.rep(script, "\n", "<br>"), Logger.LEVEL_DEBUG);
    vwr.script(script);
  }

  protected boolean iAmLoading;

  protected void loadModelFileQueued(File f, boolean saveOrientation) {
    
    // BH removing re-orientation for now
    
    saveOrientation = false;
    iAmLoading = true;
    String s = "load \"" + f.getAbsolutePath().replace('\\', '/') + "\""
        + NBOConfig.JMOL_FONT_SCRIPT;
    if (saveOrientation)
      s = "save orientation o1;" + s + ";restore orientation o1";
    runScriptQueued(s);
  }

  /**
   * Uses the LOAD DATA option to load data from NBO; just getting all the
   * "load xxx" methods in the same place.
   * 
   * 
   * @param s
   */
  protected void loadModelDataQueued(String s) {
    iAmLoading = true;
    runScriptQueued(s);
  }

  protected String loadModelFileNow(String s) {
    return runScriptNow("load " + s.replace('\\', '/'));
  }

  protected boolean checkEnabled() {
    return (jmolOptionNONBO || nboService.isEnabled()
        && nboService.restartIfNecessary());
  }

  protected String evaluateJmolString(String expr) {
    return vwr.evaluateExpressionAsVariable(expr).asString();
  }

  protected String getJmolFilename() {
    return evaluateJmolString("getProperty('filename')");
  }

  /**
   * not used for MODEL
   * 
   * @param mode
   */
  protected void getNewInputFileHandler(int mode) {
    inputFileHandler = new NBOFileHandler(inputFileHandler == null ? ""
        : inputFileHandler.jobStem, "47", mode, "47", this);
  }

  /**
   * label atoms: (number lone pairs)+atomnum
   * 
   * @param type
   *        alpha or beta
   */
  protected void doSetStructure(String type) {
    doSearchSetResStruct(type, -1);
  }

  /**
   * Changes bonds and labels on the Jmol model when new resonance structure is
   * selected
   * 
   * @param type
   *        one of nrtstra, nrtstrb, alpha, beta
   * @param rsNum
   *        - index of RS in Combo Box
   */
  protected void doSearchSetResStruct(String type, int rsNum) {
    if (!NBOConfig.showAtNum) {
      runScriptNow("measurements off;isosurface off;select visible;label off; select none;refresh");
      return;
    }
    //    boolean atomsOnly = (type == null);
    if (type == null) {
      type = rsTypeLast;
    } else {
      rsTypeLast = type;
    }
    SB sb = new SB();
    sb.append("measurements off;isosurface off;select visible;label %a;");
    String color = (NBOConfig.nboView) ? "black" : "gray";
    sb.append("select visible;color labels white;"
        + "select visible & _H;color labels " + color + ";"
        + "set labeloffset 0 0 {visible}; select none;refresh;");

    String s = inputFileHandler.setStructure(sb, type, rsNum);
    if (s == null) {
      runScriptNow(sb.toString());
      return;
    }
    //sb.append(s);    
    if (NBOConfig.nboView) {
      sb.append("select add {*}.bonds;color bonds lightgrey;"
          + "wireframe 0.1;");
    }
    sb.append(NBOConfig.JMOL_FONT_SCRIPT);
    runScriptQueued(sb.toString());
  }

  protected boolean isOpenShell() {
    return inputFileHandler.isOpenShell;
  }

  synchronized protected String runScriptNow(String script) {
    logInfo("!$ " + script, Logger.LEVEL_DEBUG);
    return PT.trim(vwr.runScript(script.replace('"', '\'')), "\n");
  }

  class HelpBtn extends JButton {

    String page;

    protected HelpBtn(String page) {
      this("Help", page, null);
    }

    protected HelpBtn(String label, String page, String tooltip) {
      super(label);
      setBackground(Color.black);
      setForeground(Color.white);
      this.page = page;
      tooltip = "Help for "
          + (tooltip != null ? tooltip
              : page == null || page.length() == 1 ? "this module" : page);
      setToolTipText(tooltip);
      addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          doHelp();
        }
      });
    }

    protected void doHelp() {
      vwr.showUrl(NBOConfig.NBO_WEB_SITE + "/jmol_help/" + getHelpPage());
    }

    /**
     * Get the proper help page for this context
     * 
     * @return a web page URI
     */
    protected String getHelpPage() {
      if (page != null)
        return page;
      switch (dialogMode) {
      case DIALOG_MODEL:
        return "model_help.htm";
      case DIALOG_RUN:
        return "run_help.htm";
      case DIALOG_VIEW:
        return "view_help.htm";
      case DIALOG_SEARCH:
        return "search_help.htm";
      case DIALOG_CONFIG:
      case DIALOG_HOME:
      default:
        return "Jmol_NBOPro6_help.htm";
      }
    }
  }


}
