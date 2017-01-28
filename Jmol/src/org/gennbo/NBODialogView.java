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
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javajs.util.SB;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
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
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jmol.awt.AwtColor;
import org.jmol.java.BS;
import org.jmol.util.C;

abstract class NBODialogView extends NBODialogRun {

  protected NBODialogView(JFrame f) {
    super(f);
  }

  protected final static String[] basSet = { "AO", "PNAO", "NAO", "PNHO",
      "NHO", "PNBO", "NBO", "PNLMO", "NLMO", "MO" };

  protected final int BASIS_AO = 0;
  protected final int BASIS_PNAO = 1;
  protected final int BASIS_NAO = 2;
  protected final int BASIS_PNHO = 3;
  protected final int BASIS_NHO = 4;
  protected final int BASIS_PNBO = 5;
  protected final int BASIS_NBO = 6;
  protected final int BASIS_PNLMO = 7;
  protected final int BASIS_NLMO = 8;
  protected final int BASIS_MO = 9;

  protected OrbitalList orbitals;
  private JScrollPane orbScroll;
  private Box centerBox, bottomBox;
  private Box vecBox;
  protected Box planeBox;

  //  protected String vectorDef, planeDef;
  protected DefaultListModel<String> alphaList, betaList;
  protected JComboBox<String> basis;
  protected JRadioButton alphaSpin, betaSpin;
  private JRadioButton atomOrient;

  protected int viewState;
  protected boolean positiveSign, jmolView;
  protected JRadioButton[] storage;
  protected Container viewSettingsBox;
  protected int startingModelCount;
  protected int modelCount = 1;
  protected boolean isNewModel = true;

  //  private String selectedOrbs = "";
  protected String currOrb = "";
  private char currSign = '+';
  private char lastSign = '+';

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
    panel = new JPanel(new BorderLayout());
    viewState = VIEW_STATE_MAIN;
    ///panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    runScriptNow("set bondpicking true");

    Box b = createViewJobBox();
    if (!jmolOptionNONBO)
      panel.add(b, BorderLayout.NORTH);

    centerBox = createTitleBox(" Select Orbital(s) ", createSelectOrbitalBox());
    centerBox.setVisible(false);
    centerBox.add(createOrbitalPanel());
    panel.add(centerBox, BorderLayout.CENTER);

    panel.add(createBottomBox(), BorderLayout.SOUTH);

    updateViewSettings();

    inputFileHandler.setBrowseEnabled(true);

    String fileType = runScriptNow("print _fileType");
    if (fileType.equals("GenNBO")) {
      File f = new File(
          runScriptNow("select within(model, visible); print _modelFile"));
      inputFileHandler.setInputFile(NBOFileHandler.newNBOFile(f, "47"));
    }

    return panel;
  }

  private Component createBottomBox() {
    bottomBox = createTitleBox(" Display Type ", new HelpBtn(
        "view_display_help.htm"));
    JPanel profBox = new JPanel(new GridLayout(2, 3, 0, 0));
    profBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    profBox.setAlignmentX(0.0f);

    ButtonGroup bg = new ButtonGroup();
    final JRadioButton profileBtn = new JRadioButton("1D Profile");
    profileBtn.setToolTipText("Produce profile plot from axis parameters");
    bg.add(profileBtn);

    final JButton goBtn = new JButton("GO");
    goBtn.setEnabled(false);

    ActionListener al = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goBtn.setEnabled(true);
      }
    };
    profileBtn.addActionListener(al);
    profBox.add(profileBtn);//.setFont(nboFont);

    final JRadioButton contourBtn = new JRadioButton("2D Contour");
    contourBtn.setToolTipText("Produce contour plot from plane parameters");
    profBox.add(contourBtn);//.setFont(nboFont);
    contourBtn.addActionListener(al);
    bg.add(contourBtn);

    final JRadioButton viewBtn = new JRadioButton("3D view");
    viewBtn.addActionListener(al);
    bg.add(viewBtn);
    profBox.add(viewBtn);//.setFont(nboFont);

    vecBox = Box.createHorizontalBox();
    vecBox.setAlignmentX(0.0f);
    vecBox.setMaximumSize(new Dimension(120, 25));
    profBox.add(vecBox);

    planeBox = Box.createHorizontalBox();
    planeBox.setAlignmentX(0.0f);
    planeBox.setMaximumSize(new Dimension(120, 25));
    profBox.add(planeBox);

    goBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int[] sel = orbitals.getSelectedIndices();
        if (sel.length > 9) {
          vwr.alert("More than 9 orbitals selected");
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

    bottomBox.add(profBox);
    bottomBox.setVisible(false);
    return bottomBox;
  }

  private Box createViewJobBox() {
    Box topBox = createTitleBox(" Select Job ",
        new HelpBtn("view_job_help.htm"));
    Box inputBox = createBorderBox(true);
    inputBox.setPreferredSize(new Dimension(355, 40));
    inputBox.setMaximumSize(new Dimension(355, 40));
    topBox.add(inputBox);
    inputFileHandler = newNBOFileHandler("", "47", NBOFileHandler.MODE_VIEW, "47");
    inputBox.add(inputFileHandler);
    inputBox.setMaximumSize(new Dimension(350, 75));
    return topBox;
  }

  private Component createSelectOrbitalBox() {
    Box horizBox = Box.createHorizontalBox();
    alphaList = betaList = null;
    basis = new JComboBox<String>(basSet);
    basis.setMaximumSize(new Dimension(70, 25));
    basis.setUI(new StyledComboBoxUI(180, -1));
    horizBox.add(basis);
    basis.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {

          @Override
          public void run() {

            if (alphaList != null)
              alphaList = betaList = null;
            setNewBasis();
          }
        });

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
        EventQueue.invokeLater(new Runnable() {

          @Override
          public void run() {
            if (betaSpin.isSelected()) {
              setBonds(false);
              showAtomNums(false);
            }
            if (nboView) {
              runScriptNow("select *;color bonds lightgrey");
            }
            setNewBasis();
          }

        });
      }
    });
    alphaSpin.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EventQueue.invokeLater(new Runnable() {

          @Override
          public void run() {
            if (alphaSpin.isSelected()) {
              setBonds(true);
              showAtomNums(true);
            }
            if (nboView) {
              runScriptNow("select*;color bonds lightgrey");
            }
            setNewBasis();
          }

        });
      }
    });
    horizBox.add(alphaSpin);
    horizBox.add(betaSpin);
    alphaSpin.setVisible(isOpenShell);
    betaSpin.setVisible(isOpenShell);
    horizBox.add(new HelpBtn("view_orbital_help.htm"));
    return horizBox;
  }

  private Component createOrbitalPanel() {
    JPanel orbPanel = new JPanel(new BorderLayout());
    orbPanel.setBorder(BorderFactory.createLineBorder(Color.black));
    orbScroll = new JScrollPane();
    orbScroll.setMaximumSize(new Dimension(355,400));
    orbScroll.getViewport().setMinimumSize(new Dimension(250,400));
    orbScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
    orbPanel.add(orbScroll, BorderLayout.CENTER);

    orbPanel.setAlignmentX(0.0f);
    orbPanel.add(new JLabel(
        "click to turn up to 9 orbitals on/off; dbl-click to reverse phase"),
        BorderLayout.SOUTH);
    newOrbitals();
    return orbPanel;
  }

  private void newOrbitals() {
    if (orbitals != null) {
      orbitals.removeListSelectionListener(orbitals);
      orbitals.removeMouseListener(orbitals);
      orbScroll.getViewport().remove(orbitals);
    }
    orbScroll.getViewport().add(orbitals = new OrbitalList());
  }

  private void updateViewSettings() {
    viewSettingsBox.removeAll();
    viewSettingsBox.setLayout(new BorderLayout());
    //Box top = Box.createVerticalBox();
    JLabel lab = new JLabel("Settings:");
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    lab.setOpaque(true);
    lab.setFont(nboFont);
    //top.add(lab);

    Box middle = Box.createVerticalBox();
    Box tmp = Box.createHorizontalBox();
    tmp.add(new JLabel("Orientation: "));
    atomOrient = new JRadioButton("Atoms");
    atomOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        planeBox.setVisible(true);
        nboService.restart();
        nboReset();
        jmolView = false;
      }
    });
    tmp.add(atomOrient);

    final JRadioButton jmolOrient = new JRadioButton("Jmol");
    jmolOrient.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        planeBox.setVisible(false);
        jmolView = true;
      }
    });
    tmp.add(jmolOrient);

    ButtonGroup bg = new ButtonGroup();
    bg.add(jmolOrient);
    bg.add(atomOrient);

    viewSettingsBox.add(lab, BorderLayout.NORTH);

    middle.add(tmp);

    tmp = Box.createHorizontalBox();
    final JButton btnVec = new JButton("Axis");
    final JButton btnPla = new JButton("Plane");
    final JButton btnLines = new JButton("Lines");

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
    tmp.add(btnVec);
    tmp.add(btnPla);
    tmp.add(btnLines);
    middle.add(tmp);
    viewSettingsBox.add(middle, BorderLayout.CENTER);

    final JButton btnCam = new JButton("Camera");
    btnCam.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam1();
      }
    });

    atomOrient.setSelected(true);
    viewSettingsBox.add(btnCam, BorderLayout.SOUTH);
    repaint();
    revalidate();
  }

  protected void setNewBasis() {
    if (orbitals == null)
      return;
    isNewModel = true;
    int fileNum = 31 + basis.getSelectedIndex();
    File f = NBOFileHandler
        .newNBOFile(inputFileHandler.inputFile, "" + fileNum);
    if (!f.exists()) {
      runJob("PLOT", inputFileHandler.inputFile, "gennbo");
      return;
    }

    orbitals.removeAll();

    if (basis.getSelectedIndex() == BASIS_MO) {
      nboKeywords = cleanNBOKeylist(inputFileHandler.read47File()[1]);
      if (!nboKeywords.contains("CMO")) {
        runJob("CMO", inputFileHandler.inputFile, "gennbo");
        return;
      }
    }

    reqInfo = "";

    // version  that does not user Jmol for reading the .46 labels file
    //    final SB sb = new SB();
    //    sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
    //    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    //    sb.append("GLOBAL I_BAS_1 " + keywordNumber + sep);
    //    if(isOpenShell){
    //      if(alphaSpin.isSelected())
    //        sb.append("GLOBAL I_SPIN 1" + sep);
    //      else
    //        sb.append("GLOBAL I_SPIN -1" + sep);
    //    }else
    //      sb.append("GLOBAL I_SPIN 0" + sep);
    //
    //    sb.append("CMD LABEL");
    //    nboService.rawCmdNew("v", sb, NBOService.MODE_LIST,orbitals.model, "Getting list");
    //    String script = "load \"" + 
    //        FileHndlr.newNBOFile(fileHndlr.inputFile,"" + (31 + basis.getSelectedIndex()) + "\""); 
    //    final SB sb = new SB();
    //    sb.append("GLOBAL C_PATH " + fileHndlr.inputFile.getParent() + sep);
    //    sb.append("GLOBAL C_JOBSTEM " + fileHndlr.jobStem + sep);
    //    sb.append("GLOBAL I_BAS_1 " + keywordNumber + sep);
    boolean isBeta = isOpenShell && !alphaSpin.isSelected();

    DefaultListModel<String> list = (isBeta ? betaList : alphaList);
    if (list != null) {
      orbitals.setModelList(list, false);
      return;
    }
    list = new DefaultListModel<String>();
    if (isBeta)
      betaList = list;
    else
      alphaList = list;
    loadModelFileQueued(
        f,
        NBOFileHandler.pathWithoutExtension(f.getAbsolutePath()).equals(
            NBOFileHandler.pathWithoutExtension(getJmolFilename())), false);
  }

  private String getPlaneParams() {
    SB s = new SB();
    //Plane definitions included every time
    s.append("GLOBAL PLANE_a " + (planeFields[0].getText()) + sep);

    s.append("GLOBAL PLANE_b " + (planeFields[1].getText()) + sep);

    s.append("GLOBAL PLANE_c " + (planeFields[2].getText()) + sep);
    //other variables only included if changed
    for (int i = 3; i < planeFields.length; i++)
      if (!plVal[i].equals(planeFields[i].getText())) {
        plVal[i] = planeFields[i].getText();
        s.append("GLOBAL PLANE_" + (char) ('a' + i) + " "
            + (planeFields[i].getText()) + sep);
      }
    return s.toString();
  }

  /**
   * Plane dialog
   */
  protected void plane() {
    viewPlanePt = 0;
    runScriptNow("set bondpicking false");
    viewState = VIEW_STATE_PLANE;
    Box box = createTitleBox(" Definiton of Plane ", null);
    final JPanel plane = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(6, 1, 5, 0));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter two rotation angles:"));
    labs.add(new JLabel("Enter shift of plane along normal:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max Y values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    plane.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(6, 1, 5, 0));
    in.add(planeFields[3]);
    Box bo = Box.createHorizontalBox();
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
    String[] sel = new String[3];
    sel[0] = planeFields[0].getText();
    sel[1] = planeFields[1].getText();
    sel[2] = planeFields[2].getText();
    showSelected(sel);
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });

    plane.setVisible(true);
  }

  private String getVectorParams() {
    SB s = new SB();
    //vector definitions included every time
    s.append("GLOBAL VECTOR_a " + (vectorFields[0].getText()) + sep);
    s.append("GLOBAL VECTOR_b " + (vectorFields[1].getText()) + sep);
    //other variables only included as needed
    for (int i = 2; i < vectorFields.length; i++)
      if (!vecVal[i].equals(vectorFields[i].getText())) {
        vecVal[i] = vectorFields[i].getText();
        s.append("GLOBAL VECTOR_" + (char) ('a' + i) + " "
            + (vectorFields[i].getText()) + sep);
      }
    return s.toString();
  }

  /**
   * Vector dialog
   */
  protected void vector() {
    runScriptNow("set bondpicking false");
    viewState = VIEW_STATE_VECTOR;
    viewVectorPt = 0;
    Box box = createTitleBox(" Vector Definition ", null);
    JPanel vect = new JPanel(new BorderLayout());
    JPanel labs = new JPanel(new GridLayout(4, 1, 4, 0));
    //    labs.add(new JLabel("Enter or select two atom numbers:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max function values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    vect.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 4, 0));
    //    Box bo = Box.createHorizontalBox();
    //    bo.add(vectorFields[0]);
    //    bo.add(vectorFields[1]);
    //    in.add(bo);
    in.add(vectorFields[2]);
    Box bo = Box.createHorizontalBox();
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
    String[] sel = new String[2];
    sel[0] = vectorFields[0].getText();
    sel[1] = vectorFields[1].getText();
    showSelected(sel);
    d.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        d.dispose();
        runScriptQueued("select off");
        runScriptNow("set bondpicking true");
        viewState = VIEW_STATE_MAIN;
      }
    });
  }

  private String getLineParams() {
    SB s = new SB();
    for (int i = 0; i < lineFields.length; i++)
      if (!lineVal[i].equals(lineFields[i].getText())) {
        lineVal[i] = lineFields[i].getText();
        s.append("GLOBAL LINES_" + (char) ('a' + i) + " " + lineVal[i] + sep);
      }
    return s.toString();
  }

  /**
   * Vector dialog
   */
  protected void lines() {
    Box box = createTitleBox(" Contour lines ", null);
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

    box = createTitleBox(" Orbital diagram lines ", null);

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
    SB s = new SB();
    for (int i = 0; i < camFields.length; i++)
      if (!camFields[i].getText().equals(camVal[i])) {
        camVal[i] = camFields[i].getText();
        s.append("GLOBAL CAMERA_" + camFieldIDs[i] + " " + camVal[i] + sep);
      }
    return s.toString();

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
    Box box = createTitleBox(" Camera and Light-Source ", null);
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
    Box box = createTitleBox(" Surface Optical Parameters: ", null);
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
    Box box = createTitleBox(" Color (Blue/Green/Red) Parameters: ", null);
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
    Box box = createTitleBox(" Atomic and Bond Radii: ", null);
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
    Box box = createTitleBox(" Contour Parameters: ", null);
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
    inputFileHandler.writeToFile(nboService.getServerPath("jview.txt"),
        sb.toString());

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
      inputFileHandler.writeToFile(nboService.getServerPath("jview.txt"),
          sb.toString());
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
      inputFileHandler.writeToFile(nboService.getServerPath("jview.txt"),
          sb.toString());
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
    vwr.writeTextFile(nboService.getServerPath("jview"), sb.toString());

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
    String[] sel;
    switch (viewState) {
    case VIEW_STATE_VECTOR:
      vectorFields[viewVectorPt++].setText(atomno);
      sel = new String[2];
      sel[0] = vectorFields[0].getText();
      sel[1] = vectorFields[1].getText();
      showSelected(sel);
      viewVectorPt = viewVectorPt % 2;
      break;
    case VIEW_STATE_PLANE:
      planeFields[viewPlanePt++].setText(atomno);
      sel = new String[3];
      sel[0] = planeFields[0].getText();
      sel[1] = planeFields[1].getText();
      sel[2] = planeFields[2].getText();
      showSelected(sel);
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
      case BASIS_AO:
      case BASIS_PNAO:
      case BASIS_NAO:
      case BASIS_MO:
        break;
      case BASIS_PNHO:
      case BASIS_NHO:
        orbitals.setValueIsAdjusting(true);
        orbitals.setSelectedIndex(pickNHO_v(tok2[2], tok2[5], (DefaultListModel<String>) orbitals.getModel()));
        orbitals.setValueIsAdjusting(false);
        break;
      case BASIS_PNBO:
      case BASIS_NBO:
      case BASIS_PNLMO:
      case BASIS_NLMO:
        orbitals.setValueIsAdjusting(true);
        orbitals.setSelectedIndex(pickNBO_v(tok2[2], tok2[5], (DefaultListModel<String>) orbitals.getModel()));
        orbitals.setValueIsAdjusting(false);
        break;
      }
    }
  }

  protected int pickNBO_v(String at1, String at2, DefaultListModel<String> list) {
    String bond = at1 + "-" + at2;
    int curr = 0, size = list.getSize();
    if (currOrb.replace(" ", "").contains(bond))
      curr = list.indexOf(currOrb);
    for (int i = curr + 1; i < size + curr; i++) {
      String str = list.getElementAt(i % size).replace(" ", "");
      if (str.contains(bond)) {
        orbitals.setSelectedIndex(i % size);
        currOrb = list.get(i % size);
        return i % size;
      }
    }
    return curr;
  }

  protected int pickNHO_v(String at1, String at2, DefaultListModel<String> list) {
    String bond = at1 + "(" + at2 + ")";
    String bond2 = at2 + "(" + at1 + ")";
    int curr = 0, size = list.getSize();
    if (currOrb.replace(" ", "").contains(bond))
      curr = list.indexOf(currOrb);
    for (int i = curr + 1; i < size + curr; i++) {
      String str = list.getElementAt(i % size).replace(" ", "");
      if (str.contains(bond)) {
        orbitals.setSelectedIndex(i % size);
        currOrb = list.get(i % size);
        return i % size;
      } else if (str.contains(bond2)) {
        orbitals.setSelectedIndex(i % size);
        currOrb = list.get(i % size);
        return i % size;
      }
    }
    return curr;
  }

  protected void resetView() {
    isNewModel = true;
    orbitals.clearOrbitals(true);
  }

  private void resetValues() {
    nboReset();
    for (int i = 0; i < planeFields.length; i++)
      planeFields[i] = new JTextField(plVal[i]);
    for (int i = 0; i < vectorFields.length; i++)
      vectorFields[i] = new JTextField(vecVal[i]);
    for (int i = 0; i < lineFields.length; i++)
      lineFields[i] = new JTextField(lineVal[i]);
    for (int i = 0; i < camFields.length; i++)
      camFields[i] = new JTextField(camVal[i]);
    //    vectorDef = "1;2";
    //    planeDef = "1;2;3";
  }

  protected void nboReset() {
    plVal = new String[] { "1", "2", "3", "0.5", "0.0", "0.0", "0.0", "-3.0",
        "3.0", "-3.0", "3.0", "25" };
    vecVal = new String[] { "1", "2", "0.5", "-2.0", "2.0", "-1.0", "1.0",
        "100" };
    lineVal = new String[] { "0.03", "0.05", "4", "0.05", "0.05", "0.1", "0.1" };
  }

  // we already have this information in Jmol; no need to get it again

  protected void notifyList_v(AbstractListModel<String> list) {
    if (list != null) {
      orbitals.setLayoutOrientation(JList.VERTICAL_WRAP);
      orbitals.requestFocus();
    }
  }

  @SuppressWarnings("unchecked")
  protected void notifyLoad_v() {

    if (vwr.ms.ac == 0)
      return;
    showAtomNums(alphaSpin.isSelected());
    setBonds(alphaSpin.isSelected());

    centerBox.setVisible(true);
    bottomBox.setVisible(!jmolOptionNONBO);

    //OLD

    //    if (!newModel) {
    //      String frame = (startingModelCount + modelCount - 1) + ".1";
    //      runScriptQueued("frame " + frame + ";refesh");
    //      colorMeshes();
    //      runScriptQueued("var b = {visible}.bonds;select bonds @b;wireframe 0;refresh");
    //      return;
    //    }

    //<--

    // retrieve model from currently loaded model

    Map<String, Object> moData = (Map<String, Object>) vwr
        .getCurrentModelAuxInfo().get("moData");
    String type = basis.getSelectedItem().toString();
    if (type.charAt(0) == 'P')
      type = type.substring(1);
    boolean isBeta = isOpenShell && !alphaSpin.isSelected() && betaList != null;
    try {
      alphaSpin.setVisible(isOpenShell); // old
      betaSpin.setVisible(isOpenShell); // old

      resetValues();
      vecBox.removeAll();
      vecBox.add(new JLabel("Axis: "));
      vecBox.add(vectorFields[0]);
      vecBox.add(vectorFields[1]);
      planeBox.removeAll();
      planeBox.add(new JLabel("Plane: "));
      planeBox.add(planeFields[0]);
      planeBox.add(planeFields[1]);
      planeBox.add(planeFields[2]);
      viewSettingsBox.setVisible(!jmolOptionNONBO);

      // set list
      
      String[] a = ((Map<String, String[]>) moData.get("nboLabelMap"))
          .get((isBeta ? "beta_" : "") + type);
      DefaultListModel<String> list = (isBeta ? betaList : alphaList);
      for (int i = 0; i < a.length; i++)
        list.addElement((i + 1) + ". " + a[i] + "   ");
      orbitals.setModelList(list, true);
    } catch (NullPointerException e) {
      //not a problem? log(e.getMessage() + " reading file", 'r');
      e.printStackTrace();
    }
    showAtomNums(true);
    colorMeshes();
    setBonds(true);
    
  }

  protected void setViewerBasis() {
    if (basis.getSelectedIndex() != BASIS_MO)
      basis.setSelectedIndex(BASIS_PNBO);
    else
      basis.setSelectedIndex(BASIS_MO);
    // TODO

  }


  class OrbitalList extends JList<String> implements ListSelectionListener,
      MouseListener, KeyListener {

    private BS bsOn = new BS();
    private BS bsNeg = new BS();
    private BS bsKnown = new BS();
    private Color contrastColor;
    
    public OrbitalList() {
      super();
      setLayoutOrientation(JList.VERTICAL_WRAP);
      setVisibleRowCount(jmolOptionNONBO ? 15 : 10);
      setFont(nboFontLarge);
      setColorScheme();
      //setFont(new Font("MONOSPACED", Font.PLAIN, 14));
      setModel(new DefaultListModel<String>());
      setCellRenderer(new ListCellRenderer<String>(){


        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                                                      String value, int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
          return renderCell(index);
        }     
      });
      addListSelectionListener(this);
      addMouseListener(this);
    }

    public void setModelList(DefaultListModel<String> list, boolean isNew) {
      setSelectedIndices(new int[0]);
      setModel(list);
      setColorScheme();
      if (isNew) {
        clearOrbitals(false);
        setLastOrbitalSelection();
      }
      for (int i = list.getSize(); --i >= 0;)
        setLabel(i);
      clearOrbitals(false);
      updateIsosurfacesInJmol(Integer.MIN_VALUE, false);
    }

    private void setColorScheme() {
        int bgcolor = vwr.getBackgroundArgb();
        if (bgcolor == 0xFF000000)
          bgcolor = 0xFFE0E0E0;
        //int color = C.getArgb(C.getBgContrast(bgcolor));
        //setBackground(new AwtColor(bgcolor));
        //contrastColor = new AwtColor(color);// Black or White
    }

    private JLabel cellLabel;
    
    protected Component renderCell(int index) {      
      if (cellLabel == null) {
        cellLabel = new JLabel();
        cellLabel.setFont(nboFontLarge);
      }
      cellLabel.setText(getModel().getElementAt(index));
      cellLabel.setForeground(!bsOn.get(index) ? contrastColor : bsNeg.get(index) ? orbColor2 : orbColor1);

     return cellLabel;
    }

    /**
     * Clear the orbital display bitsets.
     * 
     * @param isNewFile 
     */
    protected void clearOrbitals(boolean isNewFile) {
      bsKnown.clearAll();
      if (isNewFile) {
        bsOn.clearAll();
        bsNeg.clearAll();
      }
    }

    public void setLastOrbitalSelection() {
      updateIsosurfacesInJmol(-1, false);
    }

    private void updateIsosurfacesInJmol(int iClicked, boolean fromModel) {
      DefaultListModel<String> model = (DefaultListModel<String>) getModel();
      boolean  isBeta = betaSpin.isSelected();
      String type = basis.getSelectedItem().toString();
      String script = "select visible;";
      if (fromModel)
        script += updateBitSetFromModel();
      else
        updateModelFromBitSet();
        
      for (int i = model.getSize(); --i >= 0;) {
        boolean isOn = bsOn.get(i);
        if (i == iClicked || isOn && !bsKnown.get(i) || isSelectedIndex(i) != isOn) {
          String id = "mo" + i;
          if (!isOn || bsKnown.get(i))
            bsOn.setBitTo(i, !isOn);
          boolean isKnown = bsKnown.get(i); 
          if (isKnown && !bsOn.get(i)) {
            // just turn it off
            script += "isosurface mo" + i + " off;";            
          } else if (isKnown){
            // just turn it on
            script += "isosurface mo" + i + " on;";            
          } else {
            bsKnown.set(i);
            // create the isosurface
            script += "isosurface " + id
                + " color " + (bsNeg.get(i) ? color1 + " " + color2 : color2 + " " + color1)
                + " cutoff 0.0316 NBO " + type + " " + (i + 1) + (isBeta ? " beta" : "") 
                + " frontonly "
                + (useWireMesh ? " mesh nofill" : " nomesh fill translucent " + opacityOp) + ";";
          }
        }
      }
      updateModelFromBitSet();
      runScriptQueued(script);
//      System.out.println("known" + bsKnown + " on" + bsOn + " neg" + bsNeg + " " + script);
    }

    private String updateBitSetFromModel() {
      int[] a = getSelectedIndices();
      BS bsModel = new BS();
      for (int i = a.length; --i >= 0;)
        bsModel.set(i);
      String script = "";
      for (int i = getModel().getSize(); --i >= 0;) {
        if (bsModel.get(i) != bsOn.get(i)) {
          if (bsOn.get(i)) {
            script += "isosurface mo" + i + " off;";
            bsOn.clear(i);
          } else {
            bsOn.set(i);
          }
        }
      }
      return script;
    }
    
    private void updateModelFromBitSet() {
      int[] a = new int[bsOn.cardinality()];
      for (int i = bsOn.nextSetBit(0), pt = 0; i >= 0; i = bsOn.nextSetBit(i + 1))
        a[pt++] = i; 
      try  {
      setSelectedIndices(a);
      } catch (Exception e) {
        System.out.println("render error " + e);
        // this is due to underlying list changing. Ignore
      }
    }
    @Override
    public void valueChanged(ListSelectionEvent e) {
    }

    private void toggleOrbitalNegation(int i) {
      bsNeg.setBitTo(i, !bsNeg.get(i));
      bsKnown.clear(i); // to - just switch colors?
      setLabel(i);
    }

    protected void setLabel(int i) {
      String label0 = getModel().getElementAt(i);
      int pt = label0.indexOf('[');
      if (pt > 0)
        label0 = label0.substring(0, pt);
      ((DefaultListModel<String>) getModel()).set(i, label0.trim()
          + (bsNeg.get(i) ? "[-]  " : "     "));
    }

    @Override
    public void mousePressed(MouseEvent e) {
//      System.out.println("PRESS" + e);
//      System.out.println("press " + PT.toJSON(null, getSelectedIndices()) + e.getClickCount());
    }

    
    @Override
    public void mouseReleased(MouseEvent e) {
//      System.out.println("RELEASE" + e);      
//      System.out.println("release " + PT.toJSON(null, getSelectedIndices()));
    }

    private long lastTime = 0;
    private int lastPicked = 999;
    private final static long DBLCLICK_THRESHOLD_MS = 300;
    
    @Override
    public void mouseClicked(MouseEvent e) {
//      System.out.println("click " + PT.toJSON(null, getSelectedIndices())
//          + e.getClickCount());

      int i = getSelectedIndex();
      
      System.out.println("NBODialogView: picked " + lastPicked + "/" + i + " ms=" + (System.currentTimeMillis() - lastTime));
      if (e.getClickCount() > 1 || lastPicked == i && System.currentTimeMillis() - lastTime < DBLCLICK_THRESHOLD_MS) {
        toggleOrbitalNegation(i);
        bsOn.set(i);
        updateIsosurfacesInJmol(i, false);
      }
      updateIsosurfacesInJmol(i, false);
      lastTime = System.currentTimeMillis();
      lastPicked = i;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
//      System.out.println("KEYDN" +  orbitals.getSelectedIndex() + " " + e);
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
//      System.out.println("KEYUP" + orbitals.getSelectedIndex() + " " + e);
      updateIsosurfacesInJmol(-1, true);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

  }

}
