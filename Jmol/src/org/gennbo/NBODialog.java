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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javajs.swing.SwingConstants;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.AbstractListModel;
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
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

/**
 * A dialog for interacting with NBOServer
 * 
 * The NBODialog class includes all public entry points.
 * 
 * There is really only ONE dialog -- This one. To help in managing the
 * different aspects of the task, there are several superclasses:
 * 
 * JDialog NBODialogConfig
 * 
 * -- NBODialogModel
 * 
 * ---- NBODialogRun
 * 
 * ------ NBODialogView
 * 
 * -------- NBODialogSearch
 * 
 * ---------- NBODialog
 * 
 * All of these are one object, just separated this way to allow some
 * compartmentalization of tasks along the lines of NBOPro6.
 * 
 * 
 */
public class NBODialog extends NBODialogSearch {

  protected JLabel licenseInfo;

  private JButton helpBtn;
  
  private JDialog settingsDialog;
  private JPanel topPanel;
  protected JButton modelButton, runButton, viewButton, searchButton;
  
  private JPanel homePanel;

  protected JPanel nboOutput;

  protected NBOPlugin nboPlugin;
  
  //static final int DIALOG_LIST = 64; // used only for addLine
  // local settings of the dialog type

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
  public NBODialog(NBOPlugin plugin, JFrame jmolFrame, Viewer vwr, Map<String, Object> jmolOptions) {
    super(jmolFrame);
    setTitle("NBOPro6@Jmol " + plugin.getVersion());
    nboPlugin = plugin;
    this.vwr = vwr;
    setJmolOptions(jmolOptions);
    this.nboService = new NBOService(this, vwr, !jmolOptionNONBO);
    this.setIconImage(getIcon("nbo6logo20x20").getImage());
    this.setLayout(new BorderLayout());
    sendDefaultScript();
    
    //get saved properties
    
    createDialog(jmolFrame.getBounds());
    
    if (!jmolOptionNOSET)
      setDefaults(false);
    
    if (jmolOptionVIEW || jmolOptionNONBO)
      this.openPanel('v');
  }
  
  protected Component getComponentatPoint(Point p, Component top){
    Component c = null;
    if(top.isShowing()) {
      do{
        c = ((Container) top).findComponentAt(p);
      }while(!(c instanceof Container));
    }
    return c;
  }
  
  private void createDialog(Rectangle bounds) {
    dialogMode = DIALOG_HOME;
    // createDialog(Math.max(570, 615);
    setBounds(bounds.x + bounds.width - 75, bounds.y, 615, Math.max(bounds.height, 660));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        nboService.closeProcess(); 
        close();
      }
    });

    placeNBODialog(this);
    helpBtn = new HelpBtn(""){
      @Override
      public void actionPerformed(ActionEvent e){
        String url = "http://nbo6.chem.wisc.edu/jmol_help/";
        switch(dialogMode){
        case DIALOG_MODEL:
          url += "model_help.htm";
          break;
        case DIALOG_RUN:
          url += "run_help.htm";
          break;
        case DIALOG_VIEW:
          url += "view_help.htm";
          break;
        case DIALOG_SEARCH:
          url += "search_help.htm";
          break;
        default:
          url += "Jmol_NBOPro6_help.htm";
        }
        try {
          Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e1) {
          alertError("Could not open help pages");
        } 
        
      }
    };
    
    licenseInfo = new JLabel("License not found", SwingConstants.CENTER);
    //licenseInfo.setBackground(null);

    licenseInfo.setOpaque(true);
    licenseInfo.setForeground(Color.white);
    licenseInfo.setBackground(Color.black);

    nboOutput();
    centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JPanel(),nboOutput);
    topPanel = buildTopPanel();
    getContentPane().add((homePanel = mainPage()),BorderLayout.CENTER);
    getContentPane().add(licenseInfo,BorderLayout.SOUTH);
    //centerPanel.setLeftComponent(mainPage());
    this.dialogMode = DIALOG_HOME;
    this.getContentPane().add(topPanel,BorderLayout.NORTH);    
    settingsDialog = new JDialog(this,"Settings");
    settingsDialog.setSize(new Dimension(350,400));
    settingsDialog.setLocation(this.getX() + 100,this.getY()+100);
    settingsPanel = new JPanel();
    buildSettingsPanel(settingsPanel);
    settingsDialog.add(settingsPanel);
    this.setVisible(true);
    if (!jmolOptionNONBO && nboService.isOffLine())
      settingsDialog.setVisible(true);
  }
  
  /**
   * Places main dialog adjacent to main jmol window
   * @param d
   */
  private void placeNBODialog(JDialog d) {
    Dimension screenSize = d.getToolkit().getScreenSize();
    Dimension size = d.getSize();
    int y = d.getParent().getY();
    int x = Math.min(screenSize.width - size.width,
        d.getParent().getX()+d.getParent().getWidth());
    System.out.println("------" + x + "   " + y);
    d.setLocation(x, y);
  } 

  private JButton getMainButton(final JButton b, final char mode, Font font){
    b.setBorder(null);
    b.setMargin(new Insets(5, 5, 5, 5));
    b.setContentAreaFilled(false);
    b.setForeground(Color.white);
    b.setFont(font);
    if(mode != 'h')
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openPanel(mode);
      }
    });
    return b;
  }
  
  /**
   * Top panel with logo/modules/file choosing options
   * 
   * @return top panel
   */
  protected JPanel buildTopPanel() {
    JPanel p = new JPanel(new BorderLayout());
    Font f = topFont;
    Box b = Box.createHorizontalBox();
    b.add(Box.createRigidArea(new Dimension(20, 0)));

    modelButton = new JButton("Model");
    runButton = new JButton("Run");
    viewButton = new JButton("View");
    searchButton = new JButton("Search");

    if (!jmolOptionNONBO) {
      b.add(getMainButton(modelButton, 'm', f));
      b.add(Box.createRigidArea(new Dimension(20, 0)));

      b.add(getMainButton(runButton, 'r', f));
      b.add(Box.createRigidArea(new Dimension(20, 0)));

      b.add(getMainButton(viewButton, 'v', f));
      b.add(Box.createRigidArea(new Dimension(20, 0)));

      b.add(getMainButton(searchButton, 's', f));
      b.add(Box.createRigidArea(new Dimension(30, 50)));
    }

    b.add(getMainButton(new JButton("Settings"), 'c', settingHelpFont));
    b.add(Box.createRigidArea(new Dimension(20, 0)));
    b.add(getMainButton(helpBtn, 'h', settingHelpFont));

    p.add(b, BorderLayout.CENTER);
    icon = new JLabel();
    icon.setFont(nboFont);
    icon.setForeground(Color.white);
    p.add(icon, BorderLayout.EAST);
    p.setBackground(Color.BLACK);
    p.setPreferredSize(new Dimension(500, 60));
    return p;
  }
  
  /**
   * sets components colors in container recursively
   * @param comp
   * @param foregroundColor
   * @param backgroundColor
   */
  protected void setComponents(Component comp, Color foregroundColor, Color backgroundColor){
    if(comp instanceof JTextField ||comp instanceof JTextPane || comp instanceof JButton)
      return;
    if(comp instanceof JComboBox)
      comp.setBackground(new Color(248,248,248));
    if(foregroundColor!=null)comp.setForeground(foregroundColor);
    if(backgroundColor!=null)comp.setBackground(backgroundColor);
    if(comp instanceof Container){
      for(Component c:((Container)comp).getComponents()){
        setComponents(c, foregroundColor, backgroundColor);
      }
    }
  }
  
  private JPanel mainPage(){
    JPanel p = new JPanel();
    p.setBackground(Color.white);
    
    
   haveService = nboService.restartIfNecessary(); // BH temporarily
    
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    //Header stuff////////////
    ImageIcon imageIcon = getIcon("nbo6logo");        

    Image image = imageIcon.getImage(); 
    Image newimg = image.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); 
    imageIcon = new ImageIcon(newimg);
    JLabel lab = new JLabel(imageIcon);
    Box b = Box.createHorizontalBox();
    
    b.add(lab);
    lab = new JLabel("NBOServe (v6) toolbox");
    b.add(lab);
    b.add(Box.createRigidArea(new Dimension(370,0)));
    icon.setOpaque(true);
    icon.setBackground(Color.LIGHT_GRAY);
    icon.setText(haveService ? "  Connected  ":"<html><center>Not<br>Connected</center></html>");
    icon.setForeground(haveService ? Color.black:Color.red);
    icon.setBorder(BorderFactory.createLineBorder(Color.black));
    
    p.add(b);
    lab = new JLabel("NBOPro6@Jmol");
    lab.setFont(nboProTitleFont);
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
    Font f = new Font("Arial",Font.BOLD | Font.ITALIC,16);
    Font f2 = new Font("Arial",Font.BOLD | Font.ITALIC,22);
    JButton btn = new JButton("Model");
  
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150,30));
    btn.setFont(f2);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openPanel('m');
      }
    });
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    p2.add(btn,c);
    c.gridx = 1;
    c.gridy = 0;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Create & edit molecular model and input files"),c);
  
    c.gridx = 5;
    c.gridwidth = 1;
    p2.add(Box.createRigidArea(new Dimension(60,10)),c);
    lab.setFont(f);
    JTextPane tp = new JTextPane();
    tp.setContentType("text/html");
    tp.setText("<HTML><center>Frank Weinhold<br><I>(Acknowledgments: Eric Glendening, John Carpenter, " +
        "Mark Muyskens, Isaac Mades, Scott Ostrander, John Blair, Craig Weinhold)</I></center></HTML>");
    tp.setEditable(false);
    tp.setBackground(null);
    tp.setPreferredSize(new Dimension(430,60));
    c.gridx = 1;
    c.gridy = 1;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.HORIZONTAL;
    p2.add(tp,c);
  
    c.weightx = 0;
    
    //RUN/////////////
    btn = new JButton("Run");
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150,30));
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
        openPanel('r');
      }
    });
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 1;
    btn.setFont(f2);
    p2.add(btn,c);
    c.gridx = 1;
    c.gridy = 2;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Launch NBO analysis for chosen archive file"),c);
    lab.setFont(f);
    tp = new JTextPane();
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Eric Glendening, Jay Badenhoop, Alan Reed, John Carpenter, Jon Bohmann, " +
    		"Christine Morales, and Frank Weinhold</center></HTML>");
    c.gridx = 1;
    c.gridy = 3;
    c.gridwidth = 3;
    p2.add(tp,c);
  
    //VIEW//////////////
    btn = new JButton("View");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
        openPanel('v');
      }
    });
    btn.setFont(f2);
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150,30));
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 1;
    p2.add(btn,c);
    c.gridx = 1;
    c.gridy = 4;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Display NBO orbitals in 1D/2D/3D imagery"),c);
    lab.setFont(f);
    tp = new JTextPane();
    tp.setMaximumSize(new Dimension(430,60));
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Mark Wendt and Frank Weinhold<br><I> (Acknowledgments: Eric Glendening, John Carpenter, " +
        "Mark Muyskens, Scott Ostrander, Zdenek Havlas, Dave Anderson)</I></center></HTML>");
    c.gridx = 1;
    c.gridy = 5;
    c.gridwidth = 3;
    p2.add(tp,c);
  
    //SEARCH/////////////
    btn = new JButton("Search");
    btn.setForeground(Color.WHITE);
    btn.setBackground(Color.BLUE);
    btn.setMinimumSize(new Dimension(150,30));
    btn.setFont(f2);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        
        openPanel('s');
      }
    });
    c.gridx = 0;
    c.gridy = 6;
    c.gridwidth = 1;
    p2.add(btn,c);
    c.gridx = 1;
    c.gridy = 6;
    c.gridwidth = 3;
    p2.add(lab = new JLabel("  Search NBO output interactively"),c);
    lab.setFont(f);
    tp = new JTextPane();
    tp.setMaximumSize(new Dimension(430,60));
    tp.setContentType("text/html");
    tp.setBackground(null);
    tp.setText("<HTML><center>Frank Weinhold</center></HTML>");
    c.gridx = 1;
    c.gridy = 7;
    c.gridwidth = 3;
    p2.add(tp,c);
  
    p.add(p2);
    b = Box.createHorizontalBox();
    b.add(btn = new JButton("NBOPro6 Manual"));
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (Desktop.isDesktopSupported()) {
          try {
            String url = 
                "http://nbo6.chem.wisc.edu/jmol_help/NBOPro6_man.pdf";
            Desktop.getDesktop().browse(new URI(url));
          } catch (IOException ex) {
            alertError("Could open NBOPro6 manual");
          } catch (URISyntaxException e) {
            alertError("Could open NBOPro6 manual");
          }
      }
      }
    });
    btn.setFont(f);
    p.add(b);
    JTextPane t = new JTextPane();
    t.setContentType("text/html");
    t.setText("<HTML><Font color=\"RED\"><center>\u00a9Copyright 2016 Board of Regents of the University of Wisconsin System " +
        "on behalf of \nthe Theoretical Chemistry Institute.  All Rights Reserved</center></font></HTML>");
    
    t.setForeground(Color.RED);
    t.setBackground(null);
    t.setAlignmentX(0.5f);
    t.setMaximumSize(new Dimension(10000, 80));
    p.add(t);

    centerPanel.setDividerLocation(355);
    return p;
  }

  private ImageIcon getIcon(String name) {
    return new ImageIcon(this.getClass().getResource("assets/" + name + ".gif"));
  }

  protected void nboOutput() {
    nboOutput = new JPanel(new BorderLayout());
    viewSettingsBox = new JPanel(new BorderLayout());
    viewSettingsBox.add(new JLabel("Settings"), BorderLayout.NORTH);
    JPanel s = new JPanel(new BorderLayout());
    
    
    s.add(viewSettingsBox,BorderLayout.NORTH);
    viewSettingsBox.setVisible(!jmolOptionNONBO);
    nboOutput.add(viewSettingsBox,BorderLayout.NORTH);
    nboOutput.add(s,BorderLayout.CENTER);
    JLabel lab = new JLabel("Session Dialog");
    lab.setFont(monoFont);
    s.add(lab, BorderLayout.PAGE_START);
    JScrollPane p1 = new JScrollPane();
    if(jpNBODialog == null){
      jpNBODialog = new JTextPane();
      jpNBODialog.setEditable(false);
      jpNBODialog.setBorder(null);
      //jpNBODialog.setFont(new Font("Arial", Font.PLAIN, 16));
      bodyText = "";
    }
    jpNBODialog.setContentType("text/html");
    //jpNBODialog.setFont(new Font("Arial",Font.PLAIN,10));
    setComponents(s,Color.WHITE,Color.BLACK);
    p1.getViewport().add(jpNBODialog);
    p1.setBorder(null);
    s.add(p1, BorderLayout.CENTER);
    JPanel box = new JPanel(new GridLayout(2,1));
    statusLab = new JLabel();
    statusLab.setForeground(Color.red);
    statusLab.setBackground(Color.white);
    statusLab.setFont(new Font("Arial",Font.BOLD,14));
    statusLab.setOpaque(true);
    box.add(statusLab);
    Box box2 = Box.createHorizontalBox();
    JButton clear = new JButton("Clear");
    clear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearOutput();
      }
    });
    box2.add(clear);
    JButton btn = new JButton("Save Output");
    btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (jpNBODialog == null)
            return;
          JFileChooser myChooser = new JFileChooser();
          myChooser.setFileFilter(new FileNameExtensionFilter(".txt",".txt"));
          myChooser.setFileHidingEnabled(true);
          
          int button = myChooser.showSaveDialog(jpNBODialog);
          if (button == JFileChooser.APPROVE_OPTION) {
            String output = bodyText.replaceAll("<br>",sep);
            output = output.replaceAll("<b>", "");
            output = output.replaceAll("</b>", "");
            output = output.replaceAll("<i>","");
            output = output.replaceAll("</i>","");
            inputFileHandler.writeToFile(myChooser.getSelectedFile().toString(), output);
          }
        }
      });
    box2.add(btn);
    box.add(box2);
    s.add(box, BorderLayout.SOUTH);
  }

  public void close() {
    if(modulePanel != null)
      inputFileHandler.clearInputFile();
    runScriptNow("select off");
    dispose();
  }
  

  protected void openPanel(char type) {
    if (jmolOptionNONBO && "rsm".indexOf("" + type) >= 0) {
      vwr.alert("This option requires NBOServe");
      return;
    }    

    if (type == 'c') {
      settingsDialog.setVisible(true);
      return;
    }
    if (nboService.isWorking) {
      int i = JOptionPane.showConfirmDialog(this,
          "NBOServe is working. Cancel current job?\n"
              + "This could affect input/output files\n"
              + "if GenNBO is running.", "Message", JOptionPane.YES_NO_OPTION);
      if (i == JOptionPane.NO_OPTION) {
        return;
      }
    }
    if (!jmolOptionNOZAP) // use Jmol command NBO OPTIONS NOZAP to allow this
      runScriptNow("zap");
    nboService.restart();
    if (dialogMode == DIALOG_HOME) {
      remove(homePanel);
      add(centerPanel, BorderLayout.CENTER);
      if (type != 'c')
        dialogMode = type;
    }
    switch (dialogMode) {
    case DIALOG_VIEW:
    case DIALOG_SEARCH:
      runScriptNow("select none");
      break;
    }
    nboService.clearQueue();
    nboService.isWorking = false;
    viewSettingsBox.setVisible(false);
    if (!checkEnabled())
      type = 'c';
    if (topPanel != null) {
      topPanel.remove(icon);
    }
    switch (type) {
    case 'm':
      dialogMode = DIALOG_MODEL;
      centerPanel.setLeftComponent(modulePanel = buildModelPanel());
      icon = new JLabel(getIcon("nbomodel_logo"));
      setThis(modelButton);
      break;
    case 'r':
      dialogMode = DIALOG_RUN;
      centerPanel.setLeftComponent(modulePanel = buildRunPanel());
      icon = new JLabel(getIcon("nborun_logo"));
      setThis(runButton);
      break;
    case 'v':
      dialogMode = DIALOG_VIEW;
      centerPanel.setLeftComponent(modulePanel = buildViewPanel());
      icon = new JLabel(getIcon("nboview_logo"));
      setThis(viewButton);
      break;
    case 's':
      dialogMode = DIALOG_SEARCH;
      centerPanel.setLeftComponent(modulePanel = buildSearchPanel());
      //settingsBox.setVisible(true);
      icon = new JLabel(getIcon("nbosearch_logo"));
      setThis(searchButton);
      break;
    }
    centerPanel.setDividerLocation(350);
    if (topPanel != null)
      topPanel.add(icon, BorderLayout.EAST);
    this.statusLab.setText("");
    invalidate();
    setVisible(true);
  }
  
  protected void setThis(JButton btn) {
    for(Component c:((Container)topPanel.getComponent(0)).getComponents()){
      if(c instanceof JButton){
        if(((JButton)c).equals(btn)){
          btn.setEnabled(false);
          btn.setBorder(new LineBorder(Color.WHITE, 2));
        }else{
          ((JButton)c).setBorder(null);
          ((JButton)c).setEnabled(true);
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
    if (!isVisible()) 
      return;
    switch (type) {
    case STRUCTUREMODIFIED:
      if(dialogMode == DIALOG_MODEL){
      }
      break;
    case PICK:
      int atomIndex = ((Integer) data[2]).intValue();
      System.out.println("----" + type.toString() + ":  " + atomIndex);
      String atomno;
      if(atomIndex == -3)
        atomno = data[1].toString();
      else if (atomIndex < 0)
        break;
      else
        atomno = "" + (atomIndex + 1);
      switch (dialogMode) {
      case DIALOG_MODEL:
        notifyPick_m(atomno);
        return;
      case DIALOG_VIEW:
        notifyPick_v(atomno);
        return;
      case DIALOG_SEARCH:
        notifyPick_s(atomno);
        return;
      }
      break;
    case LOADSTRUCT:
      if (vwr.ms.ac == 0) 
        return;
      if (nboView)
        runScriptNow("select add visible.bonds;color bonds lightgrey;" +
          "wireframe 0.1;select none");
      switch (dialogMode) {
      case DIALOG_MODEL:
        notifyLoad_m();
        return;
      case DIALOG_RUN:
        notifyLoad_r();
        return;
      case DIALOG_VIEW:
        notifyLoad_v();
        return;
      case DIALOG_SEARCH:
        notifyLoad_s();
        return;
      }
      break;
    }
  }
  
  void alert(String msg) {
    try {
      switch (dialogMode) {
      case DIALOG_MODEL:
      case DIALOG_RUN:
      case DIALOG_VIEW:
      case DIALOG_SEARCH:
        log(msg,'b');
        break;
      }
    } catch (Exception e) {
      alertError(msg);
    }
  }  
  

    

  /**
   * clear output panel
   */
  protected void clearOutput(){
    bodyText = "";
   // String fontFamily = jpNBOLog.getFont().getFamily();
    if (jpNBODialog != null)
      jpNBODialog.setText("");
  }

  protected boolean checkJmolNBO(){
    return(vwr.ms.getInfo(vwr.am.cmi, "nboType") != null ||
        NBOFileHandler.getExt(inputFileHandler.inputFile).equals("47"));
  }
    
  void setStatus(String statusInfo) {
    if (statusInfo.length() > 0)
      log(statusInfo, 'p');  
      statusLab.setText(statusInfo);
  }

  void processEnd(int dialogMode, AbstractListModel<String> list) {
    statusLab.setText("");
    switch (dialogMode) {
    case NBOService.MODE_IMAGE:
      String fname = inputFileHandler.inputFile.getParent() + "\\" + inputFileHandler.jobStem
          + ".bmp";
      File f = new File(fname);
      final SB title = new SB();
      String id = "id " + PT.esc(title.toString().trim());
      String script = "image " + id + " close;image id \"\" "
          + PT.esc(f.toString().replace('\\', '/'));
      runScriptNow(script);
      break;
    case NBOService.MODE_RUN:
      inputFileHandler.setInputFile(inputFileHandler.inputFile);
      break;
    case DIALOG_VIEW:
      if (list != null) 
        orbitals.setLastOrbitalSelection();
      break;
    case DIALOG_SEARCH:
      if (list != null)
        setSearchList(list);
      break;
    }
    if (debugVerbose)
      setStatus("OK mode=" + dialogMode);
  }
  
  /**
   * user has made changes to the settings, so we need to update panels
   */
  @Override
  protected void updatePanelSettings() {
    switch (dialogMode) {
    case DIALOG_VIEW:
      setNewBasis();
      break;
    }
  }  

  void loadFromHandler(File file) {
    isNewModel = true;
    if (dialogMode == DIALOG_VIEW) 
      setViewerBasis();
     else
      loadModelFileQueued(file, false, false);
  }

  void setLicense(String line) {
    licenseInfo.setText("<html><div style='text-align: center'>" + line + "</html>");
  }
  
  @Override
  protected NBOFileHandler newNBOFileHandler(String name, String ext, int mode, String useExt) {
    return new NBOFileHandler(name, ext, mode, useExt, this);
  }

}