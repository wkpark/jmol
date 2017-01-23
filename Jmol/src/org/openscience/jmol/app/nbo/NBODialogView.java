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
package org.openscience.jmol.app.nbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jmol.util.Logger;

abstract class NBODialogView extends NBODialogRun {

  protected NBODialogView(JFrame f) {
    super(f);
  }

  protected final static String[] basSet = { "AO", "PNAO", "NAO", "PNHO",
      "NHO", "PNBO", "NBO", "PNLMO", "NLMO", "MO" };

  protected final int BASIS_PNBO = 5;
  protected final int BASIS_MO = 9;

  protected JList<String> orbitals;

  protected String vectorDef, planeDef;
  protected DefaultComboBoxModel<String> alphaList, betaList;
  protected JComboBox<String> basis;
  protected JRadioButton alphaSpin, betaSpin;
  protected int viewState;
  protected boolean positiveSign, jmolView;
  protected JRadioButton[] storage;
  protected Container settingsBox;
  protected int startingModelCount;
  protected int modelCount = 1;
  protected boolean newModel = true;

  private String currOrb = "";
  private char currSign = '+';
  private char lastSign = '+';
  private int iLast = 0;
  protected String selectedOrbs = "";

  //NBOServe view settings
  private String[] plVal, vecVal, lineVal;
  protected final JTextField[] vectorFields = new JTextField[8];
  protected final JTextField[] planeFields = new JTextField[12];
  protected final JTextField[] camFields = new JTextField[53];
  protected final JTextField[] lineFields = new JTextField[7];

  private String[] camVal = { "6.43", "0.0", "0.0", "50.0", "2.0", "2.0",
      "0.0", "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0", "40.0",
      "0.0", "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0", "40.0",
      "0.5", "1.0", "1.0", "1.0", "0.8", "0.0", "0.0", "1.0", "0.8", "0.4",
      "0.0", "1.0", "1.0", "0.5", "0.5", "0.5", "0.0", "0.7", "1.0", "0.22",
      "0.40", "0.10", "0.05", "0.0316", "0.0001", "0.4000", "1" };

  private String[] camFieldIDs = {
      // 0 - 6
      "1a", "1b", "1c", "1d", "1e", "1f",
      "1g",
      // 7 - 26
      "2a", "2b", "2c", "2d", "2e", "2f", "2g", "2h", "2i", "2j", "2k", "2l",
      "2m", "2n", "2o", "2p", "2q", "2r", "2s", "2t",
      // 27 - 44
      "3a", "3b", "3c", "3d", "3e", "3f", "3g", "3h", "3i", "3j", "3k", "3l",
      "3m", "3n", "3o", "3p", "3q", "3r",
      // 45 - 48
      "4a", "4b", "4c", "4d",
      // 49 - 51
      "5a", "5b", "5c", "6" };

  //  protected final JTextField[] contourFields = new JTextField[7];

  protected final static int VIEW_STATE_MAIN = 0;
  protected final static int VIEW_STATE_PLANE = 1;
  protected final static int VIEW_STATE_VECTOR = 2;
  protected final static int VIEW_STATE_CAMERA = 3;

  protected JPanel buildViewPanel() {
    startingModelCount = vwr.ms.mc;
    panel = new JPanel();
    viewState = VIEW_STATE_MAIN;
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    runScriptQueued("set bondpicking true");

    //JOBFILE////////
    panel.add(titleBox(" Select Job ", new HelpBtn("view_job_help.htm")));
    Box inputBox = borderBox(true);
    inputBox.setPreferredSize(new Dimension(355, 40));
    inputBox.setMaximumSize(new Dimension(355, 40));
    panel.add(inputBox);

    //if(fileHndlr == null)
    inputFileHandler = new NBOFileHandler("", "47", 3, "47", (NBODialog) this);
    
    //else
    //  fileHndlr = new FileHndlr(fileHndlr.jobStem,"47",3,"47",(NBODialog)this);

    inputBox.add(inputFileHandler);
    inputBox.setMaximumSize(new Dimension(350, 75));

    //BASIS/////////////
    Box horizBox = Box.createHorizontalBox();
    alphaList = betaList = null;
    basis = new JComboBox<String>(basSet);
    basis.setMaximumSize(new Dimension(70, 25));
    basis.setUI(new StyledComboBoxUI(180, -1));
    horizBox.add(basis);
    basis.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (alphaList != null)
          alphaList = betaList = null;
        basisSel();
      }
    });
    betaSpin = new JRadioButton("<html>&#x3B2</html>");
    alphaSpin = new JRadioButton("<html>&#x3B1</html>");
    alphaSpin.setSelected(true);
    ButtonGroup spinSelection = new ButtonGroup();
    spinSelection.add(alphaSpin);
    spinSelection.add(betaSpin);
    betaSpin.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (betaSpin.isSelected()) {
          setBonds(false);
          showAtomNums(false);
        }
        if (nboView) {
          String s2 = runScriptNow("print {*}.bonds");
          runScriptQueued("select " + s2 + ";color bonds lightgrey");
        }
        basisSel();
      }
    });
    alphaSpin.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (alphaSpin.isSelected()) {
          setBonds(true);
          showAtomNums(true);
        }
        if (nboView) {
          String s2 = runScriptNow("print {*}.bonds");
          runScriptQueued("select " + s2 + ";color bonds lightgrey");
        }
        basisSel();
      }
    });
    horizBox.add(alphaSpin);
    alphaSpin.setVisible(false);
    horizBox.add(betaSpin);
    betaSpin.setVisible(false);
    horizBox.add(new HelpBtn("view_orbital_help.htm"));
    panel.add(titleBox(" Select Orbital(s) ", horizBox)).setVisible(false);

    //ORBITAL//////////
    JPanel orbPanel = new JPanel(new BorderLayout());
    orbPanel.setBorder(BorderFactory.createLineBorder(Color.black));
    orbitals = new JList<String>();
    orbitals.setFont(nboFont);
    orbitals.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        doSelectOrbital();
      }
    });
    orbitals.setBackground(Color.WHITE);
    JScrollPane sp = new JScrollPane();
    sp.setMaximumSize(new Dimension(355, 400));
    sp.getViewport().setMinimumSize(new Dimension(250, 400));
    sp.getViewport().add(orbitals);
    orbPanel.add(new JLabel("(ctrl+click to select up to 9)"),
        BorderLayout.SOUTH);
    orbPanel.add(sp, BorderLayout.CENTER);

    orbPanel.setVisible(false);

    //PROFILE-CONTOUR////////////
    Box profBox = Box.createVerticalBox();
    profBox.add(new JLabel("<html><u> Display Type</u></html>")).setFont(
        nboFont);
    ButtonGroup bg = new ButtonGroup();

    final JButton goBtn = new JButton("GO");
    goBtn.setEnabled(false);

    final JRadioButton profileBtn = new JRadioButton("1D Profile");
    profileBtn.setToolTipText("Produce profile plot from axis parameters");
    bg.add(profileBtn);
    ActionListener al = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goBtn.setEnabled(true);
      }
    };
    profileBtn.addActionListener(al);
    profBox.add(profileBtn).setFont(nboFont);
    final JRadioButton contourBtn = new JRadioButton("2D Contour");
    contourBtn.setToolTipText("Produce contour plot from plane parameters");
    profBox.add(contourBtn).setFont(nboFont);
    contourBtn.addActionListener(al);
    bg.add(contourBtn);

    final JRadioButton viewBtn = new JRadioButton("3D view");
    viewBtn.addActionListener(al);
    profBox.add(viewBtn).setFont(nboFont);
    bg.add(viewBtn);

    goBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] sel = orbitals.getSelectedIndices();
        if (sel.length > 9) {
          alertError("More than 9 orbitals selected");
          return;
        }
        if (profileBtn.isSelected()) {
          goViewClicked(true);
        } else if (contourBtn.isSelected()) {
          goViewClicked(false);
        } else if (viewBtn.isSelected())
          view3D(orbitals.getSelectedIndices());
      }
    });

    profBox.add(goBtn);
    orbPanel.add(profBox, BorderLayout.EAST);
    orbPanel.setAlignmentX(0.0f);
    panel.add(orbPanel);

    viewSettings();

    inputFileHandler.browse.setEnabled(true);

    //    String file = vwr.getProperty("String", "filename", null).toString();
    //    String ext = FileHndlr.getExt(new File(file));    
    //    if(PT.isOneOf(ext, FileHndlr.EXTENSIONS) || ext.equals("47"))
    //      basis.setSelectedIndex(5);

    String fileType = runScriptNow("print _fileType");
    if (fileType.equals("GenNBO")) {
      File f = new File(runScriptNow("select within(model, visible); print _modelFile"));
      inputFileHandler.setInputFile(NBOFileHandler.newNBOFile(f, "47"));
    }

    return panel;
  }

  protected void doSelectOrbital() {
    int fileNum = 31 + basis.getSelectedIndex();
    File f = NBOFileHandler.newNBOFile(inputFileHandler.inputFile, "" + fileNum);
    int[] selected = orbitals.getSelectedIndices();
    int size = selected.length - 1;
    if (size < 0)
      return;
    if (size == 0) {
      selectedOrbs = "";
      runScriptQueued("mo delete");
    }
    //load model into new frame if necessary
    for (int i = modelCount - 1; i < size / 2; i++) {
      newModel = false;
      loadModelFileQueued(f, false, true);
    }
    modelCount = size / 2 + 1;
    String type = basis.getSelectedItem().toString();
    for (int i = 0; i <= size; i++) {
      if (PT.isOneOf("" + (selected[i] + 1), selectedOrbs))
        continue;
      selectedOrbs += (selected[i] + 1) + ";";
      runScriptNow("frame " + (i / 2 + 1) + ".1");
      if (size % 2 == 0)
        showJmolNBO(type, selected[i] + 1);
      else
        showJmolMO(type, selected[i] + 1);
    }
    String frame = (startingModelCount + modelCount - 1) + ".1";
    runScriptQueued("frame " + startingModelCount + ".1 " + frame);
  }

  private void viewSettings() {
    //p = new JPanel(new GridLayout(4,2));
    settingsBox.removeAll();
    settingsBox.setLayout(new BorderLayout());
    Box top = Box.createVerticalBox();

    JLabel lab = new JLabel("Settings:");
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    lab.setOpaque(true);
    lab.setFont(nboFont);
    top.add(lab);
    Box tmp = Box.createHorizontalBox();
    tmp.add(new JLabel("View Type: "));
    final JRadioButton customOrient = new JRadioButton("Atoms");
    final JRadioButton jmolOrient = new JRadioButton("Jmol");
    tmp.add(customOrient);
    tmp.add(jmolOrient);
    ButtonGroup bg = new ButtonGroup();
    bg.add(jmolOrient);
    bg.add(customOrient);
    settingsBox.add(lab, BorderLayout.NORTH);
    Box middle = Box.createVerticalBox();
    middle.add(tmp);
    tmp = Box.createHorizontalBox();
    final JButton btnVec = new JButton("Axis");
    final JButton btnPla = new JButton("Plane");
    final JButton btnLines = new JButton("Lines");
    tmp.add(btnVec);
    tmp.add(btnPla);
    tmp.add(btnLines);
    btnVec.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        vector();
      }
    });
    btnPla.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        plane();
      }
    });
    btnLines.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        lines();
      }
    });
    btnPla.setMargin(null);
    final JButton btnCam = new JButton("Camera");
    btnCam.setMargin(null);
    //tmp.add(btnCam);
    middle.add(tmp);
    btnCam.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam1();
      }
    });
    settingsBox.add(middle, BorderLayout.CENTER);
    jmolOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (jmolOrient.isSelected()) {
          SB sb = new SB();
          sb.append("CMD JVIEW");
          nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");
          jmolView = true;

        }
      }
    });

    customOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (customOrient.isSelected()) {
          SB sb = new SB();
          sb.append("CMD AVIEW");
          nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");
          jmolView = false;
        }
      }
    });
    customOrient.setSelected(true);
    Box bottom = Box.createHorizontalBox();
    bottom.add(btnCam);
    bottom.add(new JLabel("   Phase: "));
    final JRadioButton pSign = new JRadioButton("+");
    final JRadioButton nSign = new JRadioButton("-");
    positiveSign = true;
    pSign.setSelected(true);
    pSign.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (pSign.isSelected()) {
          positiveSign = true;
          setOrbitalColors('+');
        }
      }
    });
    nSign.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (nSign.isSelected()) {
          positiveSign = false;
          setOrbitalColors('-');
        }
      }
    });
    ButtonGroup bg2 = new ButtonGroup();
    bg2.add(pSign);
    bg2.add(nSign);
    bottom.add(pSign);
    bottom.add(nSign);

    settingsBox.add(bottom, BorderLayout.SOUTH);
    repaint();
    revalidate();
  }

  protected void setOrbitalColors(char plusMinus) {
    currSign = plusMinus;
    if (plusMinus == '+')
      runScriptQueued("nbo color " + color2 + " " + color1);
    else
      runScriptQueued("nbo color " + color1 + " " + color2);
  }

  private String getPlaneParams() {
    String s = "";
    for (int i = 0; i < planeFields.length; i++)
      if (!plVal[i].equals(planeFields[i].getText())) {
        plVal[i] = planeFields[i].getText();
        s += "GLOBAL PLANE_" + (char) ('a' + i) + " "
            + (planeFields[i].getText()) + sep;
      }
    return s;
  }

  /**
   * Plane dialog
   */
  protected void plane() {
    viewPlanePt = 0;
    runScriptNow("set bondpicking false");
    viewState = VIEW_STATE_PLANE;
    Box box = titleBox(" Definiton of Plane ", null);
    final JPanel plane = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(7, 1, 5, 0));
    labs.add(new JLabel("Enter or select three atoms:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter two rotation angles:"));
    labs.add(new JLabel("Enter shift of plane along normal:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max Y values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    plane.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(7, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(planeFields[0]);
    bo.add(planeFields[1]);
    bo.add(planeFields[2]);
    in.add(bo);
    in.add(planeFields[3]);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[4]);
    bo.add(planeFields[5]);
    in.add(bo);
    in.add(planeFields[6]);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[7]);
    bo.add(planeFields[8]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[9]);
    bo.add(planeFields[10]);
    in.add(bo);
    in.add(planeFields[11]);
    plane.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    plane.add(b, BorderLayout.SOUTH);
    Box box2 = Box.createVerticalBox();
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(plane);
    box2.setAlignmentX(0.0f);
    box2.setMaximumSize(new Dimension(355, 250));
    box.add(box2);
    final JDialog d = new JDialog(this, "Vector definition");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);
    int x = (getX() + getWidth()) / 2 + 150;
    int y = (getY() + getHeight()) / 2 - 175;
    d.setLocation(x, y);
    showSelected(planeDef.split(";"));
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        planeDef = planeFields[0].getText() + ";" + planeFields[1].getText()
            + ";" + planeFields[2].getText();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    //    centerPanel.setLeftComponent(box);
    //    centerPanel.setDividerLocation(350);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        planeDef = planeFields[0].getText() + ";" + planeFields[1].getText()
            + ";" + planeFields[2].getText();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });

    plane.setVisible(true);
  }

  private String getVectorParams() {
    String s = "";
    for (int i = 0; i < vectorFields.length; i++)
      if (!vecVal[i].equals(vectorFields[i].getText())) {
        vecVal[i] = vectorFields[i].getText();
        s += "GLOBAL VECTOR_" + (char) ('a' + i) + " "
            + (vectorFields[i].getText()) + sep;
      }
    return s;
  }

  /**
   * Vector dialog
   */
  protected void vector() {
    runScriptNow("set bondpicking false");
    viewState = VIEW_STATE_VECTOR;
    viewVectorPt = 0;
    Box box = titleBox(" Vector Definition ", null);
    JPanel vect = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Enter or select two atom numbers:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max function values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    vect.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(vectorFields[0]);
    bo.add(vectorFields[1]);
    in.add(bo);
    in.add(vectorFields[2]);
    bo = Box.createHorizontalBox();
    bo.add(vectorFields[3]);
    bo.add(vectorFields[4]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(vectorFields[5]);
    bo.add(vectorFields[6]);
    in.add(bo);
    in.add(vectorFields[7]);
    vect.add(in, BorderLayout.CENTER);

    JButton b = new JButton("OK");
    Box box2 = Box.createVerticalBox();
    vect.add(b, BorderLayout.SOUTH);
    vect.setAlignmentX(0.0f);
    box2.setAlignmentX(0.0f);
    box.setAlignmentX(0.0f);
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(vect);
    box2.setMaximumSize(new Dimension(355, 250));
    box.add(box2);

    final JDialog d = new JDialog(this, "Vector definition");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);
    int x = (getX() + getWidth()) / 2 + 150;
    int y = (getY() + getHeight()) / 2 - 150;
    d.setLocation(x, y);
    showSelected(vectorDef.split(";"));
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        vectorDef = vectorFields[0].getText() + ";" + vectorFields[1].getText();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        vectorDef = vectorFields[0].getText() + ";" + vectorFields[1].getText();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
  }

  private String getLineParams() {
    String s = "";
    for (int i = 0; i < lineFields.length; i++)
      if (!lineVal[i].equals(lineFields[i].getText())) {
        lineVal[i] = lineFields[i].getText();
        s += "GLOBAL LINES_" + (char) ('a' + i) + " " + lineVal[i] + sep;
      }
    return s;
  }

  /**
   * Vector dialog
   */
  protected void lines() {
    Box box = titleBox(" Contour lines ", null);
    JPanel lines = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Enter first contour line:"));
    labs.add(new JLabel("Enter contour step size:"));
    labs.add(new JLabel("Enter number of contours:"));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    lines.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    in.add(lineFields[0]);
    in.add(lineFields[1]);

    in.add(lineFields[2]);
    in.add(lineFields[3]);
    in.add(lineFields[4]);
    lines.add(in, BorderLayout.CENTER);
    Box box2 = Box.createVerticalBox();

    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(lines);
    box.add(box2);
    box2.setAlignmentX(0.0f);

    final JDialog d = new JDialog(this, "Line settings");
    d.setSize(new Dimension(300, 300));
    d.setVisible(true);
    d.add(box);

    box = titleBox(" Orbital diagram lines ", null);

    lines = new JPanel(new BorderLayout());
    labs = new JPanel(new GridLayout(2, 1, 5, 0));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    lines.add(labs, BorderLayout.WEST);
    in = new JPanel(new GridLayout(2, 1, 5, 0));

    in.add(lineFields[5]);
    in.add(lineFields[6]);
    lines.add(in, BorderLayout.CENTER);
    box2 = Box.createVerticalBox();
    box2.setAlignmentX(0.0f);
    box2.setBorder(BorderFactory.createLineBorder(Color.black));
    box2.add(lines);
    box.add(box2);
    JButton b = new JButton("OK");
    lines.add(b, BorderLayout.SOUTH);
    d.add(box, BorderLayout.SOUTH);
    int x = (getX() + getWidth()) / 2 + 150;
    int y = (getY() + getHeight()) / 2 - 150;
    d.setLocation(x, y);

    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });
    lines.setVisible(true);
  }

  private String getCameraParams() {
    String s = "";
    for (int i = 0; i < camFields.length; i++)
      if (!camFields[i].getText().equals(camVal[i])) {
        camVal[i] = camFields[i].getText();
        s += "GLOBAL CAMERA_" + camFieldIDs[i] + " " + camVal[i] + sep;
      }
    return s;

  }

  /**
   * Camera settings panel
   */
  protected void cam1() {
    viewState = VIEW_STATE_CAMERA;
    JPanel panel = new JPanel();
    JPanel cam1 = new JPanel();//(dialog, "Camera and Light-Source:", false);
    cam1.setLayout(new BorderLayout());
    cam1.setMinimumSize(new Dimension(350, 200));
    cam1.setVisible(true);
    //centerDialog(cam1);
    cam1.setBorder(BorderFactory.createLineBorder(Color.black));
    Box box = titleBox(" Camera and Light-Source ", null);
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Bounding sphere radius"));
    labs.add(new JLabel("Camera distance from screen center:"));
    labs.add(new JLabel("Two rotation angles (about X, Y):"));
    labs.add(new JLabel("Camera view angle:"));
    labs.add(new JLabel("Lighting (RL, UD, BF w.r.t. camera):"));
    cam1.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    in.add(camFields[52]);
    in.add(camFields[0]);
    bo.add(camFields[1]);
    bo.add(camFields[2]);
    in.add(bo);
    in.add(camFields[3]);
    bo = Box.createHorizontalBox();
    bo.add(camFields[4]);
    bo.add(camFields[5]);
    bo.add(camFields[6]);
    in.add(bo);
    cam1.add(in, BorderLayout.CENTER);
    cam1.setAlignmentX(0.0f);
    box.add(cam1);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(box);
    cam2(panel);
    cam3(panel);
    cam4(panel);
    cam5(panel);
    JScrollPane sp = new JScrollPane();
    sp.setMaximumSize(new Dimension(350, 500));
    sp.getViewport().add(panel);

    final JDialog d = new JDialog(this, "Camera parameters");
    d.setSize(new Dimension(360, 500));
    d.setVisible(true);
    d.add(sp, BorderLayout.CENTER);
    int x = (getX() + getWidth()) / 2 + 100;
    int y = (getY() + getHeight()) / 2 - 250;
    d.setLocation(x, y);
    JButton b = new JButton("OK");
    d.add(b, BorderLayout.SOUTH);
    b.setAlignmentX(0.0f);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
      }
    });
  }

  private void cam2(JPanel panel) {
    Box box = titleBox(" Surface Optical Parameters: ", null);
    JPanel cam2 = new JPanel(new BorderLayout());
    cam2.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("atoms:"));
    labs.add(new JLabel("bonds:"));
    labs.add(new JLabel("H-bonds:"));
    labs.add(new JLabel("orbitals:"));
    cam2.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(camFields[7]);
    bo.add(camFields[8]);
    bo.add(camFields[9]);
    bo.add(camFields[10]);
    bo.add(camFields[11]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[12]);
    bo.add(camFields[13]);
    bo.add(camFields[14]);
    bo.add(camFields[15]);
    bo.add(camFields[16]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[17]);
    bo.add(camFields[18]);
    bo.add(camFields[19]);
    bo.add(camFields[20]);
    bo.add(camFields[21]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[22]);
    bo.add(camFields[23]);
    bo.add(camFields[24]);
    bo.add(camFields[25]);
    bo.add(camFields[26]);
    in.add(bo);
    cam2.add(in, BorderLayout.CENTER);
    cam2.add(
        new JLabel(
            "                    amb              diff           spec        pow        transp"),
        BorderLayout.NORTH);
    //JButton b = new JButton("OK");
    //cam2.add(b, BorderLayout.SOUTH);
    cam2.setAlignmentX(0.0f);
    box.add(cam2);
    panel.add(box);
    //    b.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        cam2.dispose();
    //        cam3();
    //      }
    //    });
  }

  private void cam3(JPanel panel) {
    JPanel cam3 = new JPanel(new BorderLayout());
    Box box = titleBox(" Color (Blue/Green/Red) Parameters: ", null);
    cam3.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(6, 1, 5, 0));
    labs.add(new JLabel("light source color:"));
    labs.add(new JLabel("background color:"));
    labs.add(new JLabel("orbital (+ phase) color:"));
    labs.add(new JLabel("orbital (- phase) color:"));
    labs.add(new JLabel("bond color"));
    labs.add(new JLabel("H-Bond color"));
    cam3.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(6, 1, 5, 0));

    Box bo = Box.createHorizontalBox();
    bo.add(camFields[27]);
    bo.add(camFields[28]);
    bo.add(camFields[29]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[30]);
    bo.add(camFields[31]);
    bo.add(camFields[32]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[33]);
    bo.add(camFields[34]);
    bo.add(camFields[35]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[36]);
    bo.add(camFields[37]);
    bo.add(camFields[38]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[39]);
    bo.add(camFields[40]);
    bo.add(camFields[41]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[42]);
    bo.add(camFields[43]);
    bo.add(camFields[44]);
    in.add(bo);

    cam3.add(in, BorderLayout.CENTER);
    cam3.add(
        new JLabel(
            "                                                 Blue               Green             Red"),
        BorderLayout.NORTH);
    cam3.setAlignmentX(0.0f);
    box.add(cam3);
    panel.add(box);

  }

  private void cam4(JPanel panel) {
    JPanel cam4 = new JPanel(new BorderLayout());
    Box box = titleBox(" Atomic and Bond Radii: ", null);
    cam4.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("Atomic radius for H:"));
    labs.add(new JLabel("Atomic radius for C:"));
    labs.add(new JLabel("Bond radius:"));
    labs.add(new JLabel("H-bond radius:"));
    cam4.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    in.add(camFields[45]);
    in.add(camFields[46]);
    in.add(camFields[47]);
    in.add(camFields[48]);
    cam4.add(in, BorderLayout.CENTER);
    cam4.setAlignmentX(0.0f);
    box.add(cam4);
    panel.add(box);
  }

  private void cam5(JPanel panel) {
    JPanel cam5 = new JPanel(new BorderLayout());
    Box box = titleBox(" Contour Parameters: ", null);
    cam5.setBorder(BorderFactory.createLineBorder(Color.black));
    JPanel labs = new JPanel(new GridLayout(3, 1, 5, 0));
    labs.add(new JLabel("Contour value:"));
    labs.add(new JLabel("Contour tolerance:"));
    labs.add(new JLabel("Stepsize:"));
    cam5.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(3, 1, 5, 0));
    in.add(camFields[49]);
    in.add(camFields[50]);
    in.add(camFields[51]);
    cam5.add(in, BorderLayout.CENTER);
    cam5.setAlignmentX(0.0f);
    box.add(cam5);
    panel.add(box);
  }

  protected void showJmolMO(String type, int i) {
    if (!type.startsWith("P") && !type.equals("MO"))
      type = "P" + type;
    String script = "MO TYPE " + type + "; MO " + i;
    runScriptQueued(script);
    log(script, 'b');
  }

  protected void showJmolNBO(String type, int i) {
    if (type.trim().equals("NAO"))
      type = "PNAO";
    if (dialogMode == NBODialog.DIALOG_SEARCH)
      if (!type.startsWith("P") && !type.equals("MO") && !type.equals("AO"))
        type = "P" + type;
    String script = "NBO TYPE " + type + "; NBO " + i;
    runScriptQueued(script);
    log(script, 'b');
  }

  protected void basisSel() {
    newModel = true;
    int fileNum = 31 + basis.getSelectedIndex();
    File f = NBOFileHandler.newNBOFile(inputFileHandler.inputFile, "" + fileNum);
    if (!f.exists()) {
      runJob("PLOT", inputFileHandler.inputFile, "gennbo");
      return;
    }
    iLast = orbitals.getSelectedIndex();
    if (basis.getSelectedIndex() == BASIS_MO) {
      nboKeywords = cleanNBOKeylist(inputFileHandler.read47File()[1]);
      if (!nboKeywords.contains("CMO")) {
        runJob("CMO", inputFileHandler.inputFile, "gennbo");
        return;
      }
    }
    reqInfo = "";
    //    final SB sb = new SB();
    //    sb.append("GLOBAL C_PATH " + fileHndlr.inputFile.getParent() + sep);
    //    sb.append("GLOBAL C_JOBSTEM " + fileHndlr.jobStem + sep);

    //    sb.append("GLOBAL I_BAS_1 " + keywordNumber + sep);
    boolean isBeta = isOpenShell && !alphaSpin.isSelected();
    DefaultComboBoxModel<String> list = (isBeta ? betaList : alphaList);
    if (list != null) {
      orbitals.setModel(list);
      return;
    }
    list = new DefaultComboBoxModel<String>();
    if (isBeta)
      betaList = list;
    else
      alphaList = list;
    String s = getJmolFilename();
    loadModelFileQueued(f, f.getAbsolutePath().equals(s), false);
  }

  protected void setLastOrbitalSelection() {
    if (iLast < 0)
      iLast = 0;
    orbitals.setSelectedIndex(iLast);
  }

  /////////////////////// RAW NBOSERVE API ////////////////////

  //contour/profile selected orbital in orbital list
  protected void goViewClicked(boolean oneD) {
    runScriptNow("image close");
    int[] selected = orbitals.getSelectedIndices();
    int size = selected.length;
    if (size > 1) {
      showView(selected, oneD);
      return;
    }

    String tmp2 = "";
    SB sb = new SB();
    for (int i = 1; i <= 3; i++) {
      for (int j = 1; j <= 3; j++) {
        Object oi = vwr.getProperty("string", "orientationInfo.rotationMatrix["
            + j + "][" + i + "]", null);
        tmp2 += oi.toString() + " ";
      }
      sb.append("a U" + i + " " + tmp2 + sep);
      tmp2 = "";
    }
    inputFileHandler.writeToFile(nboService.serverDir + "/jview.txt", sb.toString());

    sb = new SB();
    sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    sb.append("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep);

    if (currSign != lastSign)
      sb.append("GLOBAL SIGN +1 " + sep);
    lastSign = currSign;

    int ind = orbitals.getSelectedIndex();
    if (isOpenShell)
      sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1") + sep);
    else
      sb.append("GLOBAL I_SPIN 0" + sep);

    sb.append(getLineParams());
    if (oneD) {
      sb.append(getVectorParams());
      sb.append("CMD PROFILE " + (ind + 1));
      log("Profile " + (ind + 1), 'i');
    } else {
      sb.append(getPlaneParams());
      sb.append("CMD CONTOUR " + (ind + 1));
      log("Contour " + (ind + 1), 'i');
    }

    nboService.rawCmdNew("v", sb, NBOService.MODE_IMAGE, null,
        (oneD ? "Profiling.." : "Contouring.."));
  }

  protected void showView(int[] selected, boolean oneD) {
    String tmp2 = "";
    SB sb = new SB();
    nboService.restart();
    if (jmolView) {
      for (int i = 1; i <= 3; i++) {
        for (int j = 1; j <= 3; j++) {
          Object oi = vwr.getProperty("string",
              "orientationInfo.rotationMatrix[" + j + "][" + i + "]", null);
          tmp2 += oi.toString() + " ";
        }
        sb.append("a V_U" + i + " " + tmp2 + sep);
        tmp2 = "";
      }
      inputFileHandler.writeToFile(nboService.serverDir + "/jview.txt", sb.toString());
      sb = new SB();
      sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
      sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
      sb.append("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep);
      if (isOpenShell)
        sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1")
            + sep);
      else
        sb.append("GLOBAL I_SPIN 0" + sep);
      sb.append("CMD LABEL");
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");
      sb = new SB();
      sb.append("CMD JVIEW");
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");

    }
    String tmp = (oneD) ? "Profile " : "Contour ";
    for (int i = 0; i < selected.length; i++) {
      sb = new SB();
      sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
      sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
      sb.append("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep);

      if (isOpenShell)
        sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1")
            + sep);
      else
        sb.append("GLOBAL I_SPIN 0" + sep);
      if (oneD)
        sb.append("CMD PROFILE " + (selected[i] + 1));
      else
        sb.append("CMD CONTOUR " + (selected[i] + 1));
      tmp += (selected[i] + 1) + " ";
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");

    }
    log(tmp, 'i');
    sb = new SB();
    runScriptNow("image close");
    tmp = "";
    for (int i = 0; i < selected.length; i++)
      tmp += (i + 1) + " ";

    if (tmp.equals("")) {
      alertError("Select an orbital");
      return;
    }
    sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    if (isOpenShell)
      sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1") + sep);
    else
      sb.append("GLOBAL I_SPIN 0" + sep);
    sb.append(getLineParams());
    sb.append("CMD DRAW " + tmp);
    nboService.rawCmdNew("v", sb, NBOService.MODE_IMAGE, null, "Raytracing...");
  }

  protected void view3D(int[] selected) {
    runScriptNow("image close");
    String tmp2 = "";
    nboService.restart();
    SB sb = new SB();
    if (jmolView) {
      for (int i = 1; i <= 3; i++) {
        for (int j = 1; j <= 3; j++) {
          Object oi = vwr.getProperty("string",
              "orientationInfo.rotationMatrix[" + j + "][" + i + "]", null);
          tmp2 += oi.toString() + " ";
        }
        sb.append("a V_U" + i + " " + tmp2 + sep);
        tmp2 = "";
      }
      inputFileHandler.writeToFile(nboService.serverDir + "/jview.txt", sb.toString());
      sb = new SB();
      sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
      sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
      sb.append("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep);
      if (isOpenShell)
        sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1")
            + sep);
      else
        sb.append("GLOBAL I_SPIN 0" + sep);
      sb.append("CMD LABEL");
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");
      sb = new SB();
      sb.append("CMD JVIEW");
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");

    }
    sb = new SB();
    String tmp = "View ";
    for (int i = 0; i < selected.length; i++) {
      sb = new SB();
      sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
      sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
      sb.append("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep);

      if (isOpenShell)
        sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1")
            + sep);
      else
        sb.append("GLOBAL I_SPIN 0" + sep);
      sb.append("CMD PROFILE " + (selected[i] + 1));
      tmp += (selected[i] + 1) + " ";
      nboService.rawCmdNew("v", sb, NBOService.MODE_RAW, null, "");

    }
    log(tmp, 'i');
    vwr.writeTextFile(nboService.serverDir + "/jview", sb.toString());

    tmp = "";
    for (int i = 0; i < selected.length; i++) {
      tmp += " " + (i + 1);
    }
    if (tmp.equals("")) {
      alertError("Select an orbital");
      return;
    }
    sb = new SB();
    sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    if (isOpenShell)
      sb.append("GLOBAL I_SPIN " + (alphaSpin.isSelected() ? "1" : "-1") + sep);
    else
      sb.append("GLOBAL I_SPIN 0" + sep);
    sb.append(getCameraParams());
    sb.append("CMD VIEW ");
    sb.append(tmp);
    nboService.rawCmdNew("v", sb, NBOService.MODE_IMAGE, null, "Raytracing...");

  }

  private int viewVectorPt = 0;
  private int viewPlanePt = 0;

  /**
   * Set the value of the atom number for vectors (profiles) or planes
   * (contours) via a callback from Jmol atom picking.
   * 
   * @param atomno
   */
  protected void notifyPick_v(String atomno) {
    switch (viewState) {
    case VIEW_STATE_VECTOR:
      vectorFields[viewVectorPt++].setText(atomno);
      vectorDef = vectorFields[0].getText() + ";" + vectorFields[1].getText();
      showSelected(vectorDef.split(";"));
      viewVectorPt = viewVectorPt % 2;
      break;
    case VIEW_STATE_PLANE:
      planeFields[viewPlanePt++].setText(atomno);
      planeDef = planeFields[0].getText() + ";" + planeFields[1].getText()
          + ";" + planeFields[2].getText();
      showSelected(planeDef.split(";"));
      viewPlanePt = viewPlanePt % 3;
      break;
    case VIEW_STATE_MAIN:
      String[] tok = atomno.split(",");
      if (tok.length < 2) {
        //pickAtomic(atomno,alphaList,orbitals);
        return;
      }
      String[] tok2 = tok[1].split(" ");
      //TODO

      switch (basis.getSelectedIndex()) {
      case 5:
      case 6:
      case 7:
      case 8:
        pickNBO(tok2[2], tok2[5], alphaList, orbitals);
        break;
      case 3:
      case 4:
        //pickNHO(tok2[2],tok2[5],alphaList,orbitals);
      }
    }
  }

  protected void pickAtomic(String atomno, DefaultComboBoxModel<String> list,
                            JComboBox<String> orb) {
    int ind = Integer.parseInt(atomno) - 1;
    String at = vwr.ms.at[ind].getElementSymbol() + atomno + "(";
    int curr = 0, size = list.getSize();
    if (currOrb.contains(at))
      curr = orb.getSelectedIndex() + 1;
    for (int i = 0; i < size; i++, curr++) {
      String str = list.getElementAt(curr % size).replaceAll(" ", "");
      if (str.contains(at + "lp)")) {
        orb.setSelectedIndex(curr % size);
        currOrb = str;
        break;
      } else if (str.contains(at + "ry)")) {
        orb.setSelectedIndex(curr % size);
        currOrb = str;
        break;
      }
    }
  }

  protected void pickNBO(String at1, String at2,
                         DefaultComboBoxModel<String> list,
                         JComboBox<String> orb) {
    String bond = at1 + "-" + at2;
    int curr = 0, size = list.getSize();
    if (currOrb.contains(bond))
      curr = orb.getSelectedIndex() + 1;
    for (int i = 0; i < size; i++, curr++) {
      String str = list.getElementAt(curr % size).replace(" ", "");
      if (str.contains(bond)) {
        orb.setSelectedIndex(curr % size);
        currOrb = str;
        break;
      }
    }
  }

  protected void pickNBO(String at1, String at2,
                         DefaultComboBoxModel<String> list, JList<String> orb) {
    String bond = at1 + "-" + at2;
    int curr = 0, size = list.getSize();
    if (currOrb.contains(bond))
      curr = orb.getSelectedIndex() + 1;
    for (int i = 0; i < size; i++, curr++) {
      String str = list.getElementAt(curr % size).replace(" ", "");
      if (str.contains(bond)) {
        orb.setSelectedIndex(curr % size);
        currOrb = str;
        break;
      }
    }
  }

  protected void pickNHO(String at1, String at2,
                         DefaultComboBoxModel<String> list,
                         JComboBox<String> orb) {
    String bond = at1 + "(" + at2 + ")";
    String bond2 = at2 + "(" + at1 + ")";
    int curr = 0, size = list.getSize();
    if (currOrb.contains(bond))
      curr = orb.getSelectedIndex() + 1;
    for (int i = 0; i < size; i++, curr++) {
      String str = list.getElementAt(curr % size).replace(" ", "");
      if (str.contains(bond)) {

        list.setSelectedItem(bond);
        //orb.setSelectedIndex(curr%size);
        currOrb = str;
        break;
      } else if (str.contains(bond2)) {
        orb.setSelectedIndex(curr % size);
        currOrb = str;
        break;
      }
    }
  }

  protected void resetView() {
    newModel = true;
    selectedOrbs = "";
    for (int i = 0; i < 9; i++) {
      //      storage[i].setText("");
      //      storage[i].setEnabled(false);
      //      storage[i].setSelected(false);
    }
    //panel.getComponent(6).setVisible(false);
    //dispBox.setVisible(false);
    iLast = 0;
  }

  private void resetValues() {
    plVal = new String[] { "1", "2", "3", "0.5", "0.0", "0.0", "0.0", "-3.0",
        "3.0", "-3.0", "3.0", "25" };
    vecVal = new String[] { "1", "2", "0.5", "-2.0", "2.0", "-1.0", "1.0",
        "100" };
    lineVal = new String[] { "0.03", "0.05", "4", "0.05", "0.05", "0.1", "0.1" };
    for (int i = 0; i < planeFields.length; i++)
      planeFields[i] = new JTextField(plVal[i]);
    for (int i = 0; i < vectorFields.length; i++)
      vectorFields[i] = new JTextField(vecVal[i]);
    for (int i = 0; i < lineFields.length; i++)
      lineFields[i] = new JTextField(lineVal[i]);
    for (int i = 0; i < camFields.length; i++)
      camFields[i] = new JTextField(camVal[i]);
    vectorDef = "1;2";
    planeDef = "1;2;3";
  }

  @SuppressWarnings("unchecked")
  protected void notifyLoad_v() {

    if (vwr.ms.ac == 0)
      return;
    if (!newModel) {
      String frame = (startingModelCount + modelCount - 1) + ".1";
      runScriptNow("frame " + frame);
      if (!useWireMesh) {
        runScriptNow("nbo nomesh fill translucent " + opacityOp);
        runScriptNow("mo nomesh fill translucent " + opacityOp);
      }

      runScriptNow("nbo color " + color2 + " " + color1);
      runScriptNow("mo color " + color2 + " " + color1);
      String bonds = runScriptNow("print {visible}.bonds");
      runScriptQueued("select bonds " + bonds + ";wireframe 0");
      return;
    }
    
    Map<String, Object> moData = (Map<String, Object>) vwr
        .getCurrentModelAuxInfo().get("moData");
    String type = basis.getSelectedItem().toString();
    if (type.charAt(0) == 'P')
      type = type.substring(1);
    boolean isBeta = isOpenShell && !alphaSpin.isSelected();
    String[] a = ((Map<String, String[]>) moData.get("nboLabelMap"))
        .get((isBeta ? "beta_" : "") + type);
    DefaultComboBoxModel<String> list = (isBeta ? betaList : alphaList);
    for (int i = 0; i < a.length; i++)
      list.addElement((i + 1) + ". " + a[i]);
    orbitals.setModel(list);
    setLastOrbitalSelection();

    resetValues();
    settingsBox.setVisible(true);
    //    for(int i = 0; i < 5; i++)
    //      panel.getComponents()[i].setVisible(true);
    if (!inputFileHandler.getChooseList())
      logInfo("Error reading $CHOOSE list", Logger.LEVEL_ERROR);
    showAtomNums(true);

    if (!useWireMesh) {
      runScriptQueued("nbo nomesh fill translucent " + opacityOp);
      runScriptQueued("mo nomesh fill translucent " + opacityOp);
    }

    runScriptQueued("nbo color " + color2 + " " + color1);
    runScriptQueued("mo color " + color2 + " " + color1);
    if (isOpenShell) {
      alphaSpin.setVisible(true);
      betaSpin.setVisible(true);
    } else {
      alphaSpin.setVisible(false);
      betaSpin.setVisible(false);
    }
    setBonds(true);
    for (int i = 0; i < panel.getComponentCount(); i++) {
      panel.getComponent(i).setVisible(true);
    }
  }

  protected void setViewerBasis() {
    if (basis.getSelectedIndex() != BASIS_MO)
      basis.setSelectedIndex(BASIS_PNBO);
    else
      basis.setSelectedIndex(BASIS_MO);
    // TODO
    
  }

//  @Override
//  protected void showConfirmationDialog(String st, File newFile, String ext) {
//    int i = JOptionPane.showConfirmDialog(this, st, "Message",
//        JOptionPane.YES_NO_OPTION);
//    if (i == JOptionPane.YES_OPTION) {
//      JDialog d = new JDialog(this);
//      d.setLayout(new BorderLayout());
//      JTextPane tp = new JTextPane();
//      d.add(tp, BorderLayout.CENTER);
//      d.setSize(new Dimension(500, 600));
//      tp.setText(fileHndlr.getFileData(FileHndlr.newNBOFile(newFile, "nbo")
//          .toString()));
//      d.setVisible(true);
//    }
//  }

}
