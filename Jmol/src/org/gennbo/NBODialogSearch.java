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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.AbstractListModel;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import org.gennbo.NBOFileHandler.ChooseList;

abstract class NBODialogSearch extends NBODialogView {

  private final static int KEYWD_WEBHELP = 0;

  private final static int KEYWD_NPA = 1;
  private final static int KEYWD_NBO = 2;
  private final static int KEYWD_BEND = 3;
  private final static int KEYWD_E2PERT = 4;
  private final static int KEYWD_NLMO = 5;

  // Note: The next three are not in the order the buttons are presented
  private final static int KEYWD_NRT = 6;
  private final static int KEYWD_STERIC = 7;
  private final static int KEYWD_CMO = 8;

  private final static int KEYWD_DIPOLE = 9;
  private final static int KEYWD_OPBAS = 10;
  private final static int KEYWD_BAS1BAS2 = 11;

  /**
   * keywords in order of PRESENTATION
   * 
   */
  private final static String[] searchButtonLabels = { "NPA", "NBO", "BEND",
      "E2PERT", "NLMO", "CMO", "NRT", "STERIC", "DIPOLE", "OPBAS", "B1B2" };

  /**
   * map button index to NBO keyword numbers.
   */
  private final static int[] btnIndexToNBOKeyword = new int[] { KEYWD_NPA,
      KEYWD_NBO, KEYWD_BEND, KEYWD_E2PERT, KEYWD_NLMO, KEYWD_CMO, KEYWD_NRT,
      KEYWD_STERIC, // these three are shifted 
      KEYWD_DIPOLE, KEYWD_OPBAS, KEYWD_BAS1BAS2 };

  /**
   * Return the NBOServe keyword for a given SEARCH option; allows for a
   * different presentation ordering in Jmol relative to actual numbers in
   * NBOServe
   * 
   * @param index
   *        the button index
   * @return the NBO keyword number for this option
   */
  protected final static int getNboKeywordNumber(int index) {
    return (btnIndexToNBOKeyword[index]);
  }

  protected final static String[] keyW = {
      "NPA    : Atomic and NAO properties",
      "NBO    : Natural Lewis Structure and\n NBO properties",
      "BEND   : NHO directionality and\n bond-bending",
      "E2PERT : 2nd-order energtics of NBO\n donor-acceptor interactions",
      "NLMO   : NLMO properties",
      "CMO    : NBO-based character of canonical\n molecular orbitals",
      "NRT    : Natural Resonance Theory\n weightings and bond orders",
      "STERIC : Total/pairwise contributions\n to steric exchange energy",
      "DIPOLE : L/NL contributions to electric\n dipole moment",
      "OPBAS  : Matrix elements of chosen operator   \n in chosen basis set",
      "B1B2   : Transformation matrix between\n chosen basis sets" };

  protected final static String[] 
      npa = { "NPA Atomic Properties:",
        "  (1) NPA atomic charge", "  (2) NPA atomic spin density",
        "  (3) NEC atomic electron configuration",
        "NPA Molecular Unit Properties:", "  (4) NPA molecular unit charge",
        "  (5) NPA molecular unit spin density", "NAO Orbital Properties:",
        "  (6) NAO label", "  (7) NAO orbital population",
        "  (8) NAO orbital energy", "  (9) NAO orbital spin density",
        "  (10) NMB minimal basis %-accuracy", "Display Options:",
        "  (11) Display atomic charges" }, 
      nbo = { "NBO Orbital Properties:",
        "  (1) NBO orbital label", "  (2) NBO orbital population",
        "  (3) NBO orbital energy", "  (4) NBO ionicity",
        "Natural Lewis Structure Properties:", "  (5) NLS rho(NL)",
        "  (6) NLS %-rho(L)" }, 
      bend = { "NHO Orbital Prperties:",
        "  (1) NHO orbital label", "  (2) NHO orbital population",
        "  (3) NHO orbital energy", "  (4) NHO hybrid composition",
        "  (5) NHO direction angles",
        "  (6) NHO bending deviation from line of centers",
        "  (7) Strongest bending deviation for any NHO" }, 
      e2 = {"E2 Values for Selected Donor-Acceptor NBOs:",
        "  (1) E(2) interaction for current d/a NBOs",
        "  (2) Strongest E(2) interaction for current d-NBO",
        "  (3) Strongest E(2) interaction for current a-NBO",
        "  (4) Strongest E(2) interaction for any d/a NBOs",
      "Intermolecular E2 Options:",
        "  (5) Strongest intermolecular E(2) for current unit",
        "  (6) Strongest intermolecular E(2) for any units" }, 
      nlmo = {"NLMO Orbital Properties:", "  (1) NLMO orbital label",
        "  (2) NLMO population", "  (3) NLMO orbital energy",
        "  (4) NLMO %-NBO parentage", "NLMO Delocalization Tail Properties:",
        "  (5) NLMO delocalization tail population",
        "  (6) NLMO delocalization tail NBO components" }, 
      nrt = {"Atom (A) Properties:", "  (1) atomic valency (total)",
        "  (2) atomic covalency", "  (3) atomic electrovalency",
        "Bond [A-A'] Properties:", "  (4) bond order (total)",
        "  (5) covalent bond order", "  (6) electrovalent bond order",
        "Resonance Structure Properties:", "  (7) RS weighting",
        "  (8) RS rho(NL) (reference structures only)", "Display Options:",
        "  (9) Display NRT atomic valencies", "  (10) Display NRT bond orders" },
      steric = { "Total Steric Exchange Energy (SXE) Estimates:",
          "  (1) Total SXE", "  (2) Sum of pairwise (PW-SXE) contributions",
          "Selected PW-SXE contributions:",
          "  (3) PW-SXE for current d-d' NLMOs",
          "  (4) Strongest PW-SXE for current d NLMO",
          "Intra- and intermolecular options:",
          "  (5) Strongest PW-SXE within current unit",
          "  (6) Strongest PW-SXE within any unit",
          "  (7) Strongest PW-SXE between any units" }, 
      mo = {"Character of current MO (c):", "  (1) Current MO energy and type",
          "  (2) Bonding character of current MO",
          "  (3) Nonbonding character of current MO",
          "  (4) Antibonding character of current MO",
          "NBO (n) %-contribution to selected MO (c):",
          "  (5) %-contribution of current NBO to current MO",
          "  (6) Largest %-contribution to current MO from any NBO",
          "  (7) Largest %-contribution of current NBO to any MO" }, 
     dip = {
          "Total Dipole Properties:", "  (1) Total dipole moment",
          "  (2) Total L-type (Lewis) dipole",
          "  (3) Total NL-type (resonance) dipole",
          "Bond [NBO/NLMO] Dipole Properties:",
          "  (4) Dipole moment of current NLMO",
          "  (5) L-type (NBO bond dipole) contribution",
          "  (6) NL-type (resonance dipole) contribution",
          "Molecular Unit Dipole Properties:",
          "  (7) Dipole moment of current molecular unit",
          "  (8) L-type contribution to unit dipole",
          "  (9) NL-type contribution to unit dipole" }, 
     op = {"<select an operation>",
          " S    : overlap (unit) operator",
          " F    : 1e Hamiltonian (Fock/Kohn-Sham) operator",
          " K    : kinetic energy operator",
          " V    : 1e potential (nuclear-electron attraction) operator",
          " DM   : 1e density matrix operator",
          " DIx  : dipole moment operator (x component)",
          " DIy  : dipole moment operator (y component)",
          " DIz  : dipole moment operator (z component)" };

  int operator = 1;
  protected JComboBox<String> comboSearchOrb1, comboSearchOrb2, comboUnit1,
      comboAtom1, comboAtom2, comboBasis2, comboBasisOperation;

  protected JLabel unitLabel;
  protected boolean relabel;

  private Box optionBox;
  private JButton keyWdBtn;
  protected JPanel opList;
  protected JRadioButton[] rBtns = new JRadioButton[12];
  protected JRadioButton radioOrbMO, radioOrbNBO;
  protected int rbSelection;

  protected int nBonds;
  private JButton back;

  private String keyProp;
  protected int searchKeywordNumber = KEYWD_WEBHELP;

  protected NBODialogSearch(JFrame f) {
    super(f);
  }

  protected JPanel buildSearchPanel() {

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(createViewSearchJobBox(NBOFileHandler.MODE_SEARCH),
        BorderLayout.NORTH);
    //    inputFileHandler.tfName.setText("");

    /////INPUT FILE/////////////

    //    Box inputBox = Box.createHorizontalBox();
    //    inputBox.setAlignmentX(0.0f);
    //    if (inputFileHandler == null)
    //      inputFileHandler = newNBOFileHandler("", "47", 4, "47");
    //    else
    //      inputFileHandler = newNBOFileHandler(inputFileHandler.jobStem, "47", 4,
    //          "47");
    //    inputBox.add(inputFileHandler);
    //    inputBox.setBorder(BorderFactory.createLineBorder(Color.black));
    //    inputBox.setMinimumSize(new Dimension(360, 60));
    //    inputBox.setPreferredSize(new Dimension(360,60));
    //    inputBox.setMaximumSize(new Dimension(360, 60));
    //
    //    Box box1 = createTitleBox(" Select Job ",
    //        new HelpBtn("search_job_help.htm"));
    //    box1.add(inputBox);
    //    
    //    
    //    panel.add(box1, BorderLayout.NORTH);

    back = new JButton("<html>&#8592Back</html>");

    /////ALPHA-BETA SPIN/////////////////
    betaSpin = new JRadioButton("<html>&#x3B2</html>");
    alphaSpin = new JRadioButton("<html>&#x3B1</html>");
    ActionListener spinListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doSetSpin();
      }
    };
    alphaSpin.addActionListener(spinListener);
    betaSpin.addActionListener(spinListener);
    alphaSpin.setSelected(true);
    ButtonGroup bg = new ButtonGroup();
    bg.add(alphaSpin);
    bg.add(betaSpin);

    /////////CMO Radio Buttons/////////////////
    bg = new ButtonGroup();
    radioOrbMO = new JRadioButton("MO");
    radioOrbMO.setSelected(true);
    radioOrbMO.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        showOrbJmol("MO", comboSearchOrb2.getSelectedIndex(), "MO");
      }
    });
    radioOrbMO.setBackground(null);
    bg.add(radioOrbMO);

    radioOrbNBO = new JRadioButton("NBO");
    radioOrbNBO.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        showOrbJmol("PNBO", comboSearchOrb1.getSelectedIndex(), "NBO");
      }
    });
    radioOrbNBO.setBackground(null);
    bg.add(radioOrbNBO);

    /////SELECT KEYWORD///////////
    Box optionBox2 = Box.createVerticalBox();
    opList = new JPanel();
    comboBasisOperation = new JComboBox<String>(op);
    comboBasisOperation.setUI(new StyledComboBoxUI(150, 350));

    comboBasisOperation.setMaximumSize(new Dimension(350, 30));
    comboBasisOperation.setAlignmentX(0.0f);
    comboBasisOperation.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doComboBasisOperationAction();
      }
    });
    comboBasisOperation.setVisible(false);

    optionBox2.add(comboBasisOperation);
    optionBox2.add(opList);
    optionBox2.setBorder(BorderFactory.createLineBorder(Color.black));
    optionBox = Box.createVerticalBox();
    optionBox.setVisible(false);
    Box topBox = Box.createHorizontalBox();
    keyWdBtn = new JButton("<html></html>");

    keyWdBtn.setVisible(false);
    keyWdBtn.setRolloverEnabled(false);
    topBox.add(keyWdBtn);
    topBox.add(back);
    topBox.add(new HelpBtn("a") {
      @Override
      public String getHelpPage() {
        return getSearchHelpURL();
      }
    });
    Box box2 = createTitleBox(" Select Keyword ", topBox);
    back.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doSelectKeyword();
      }
    });

    back.setForeground(Color.blue);
    back.setEnabled(false);
    buildHome();

    box2.setAlignmentX(0.0f);
    optionBox.add(box2);
    optionBox2.setAlignmentX(0.0f);
    optionBox.add(optionBox2);
    panel.add(optionBox);

    comboBasis1 = new JComboBox<String>(NBODialogView.basSet);
    comboBasis1.setUI(new StyledComboBoxUI(180, -1));

    inputFileHandler.setBrowseEnabled(true);

    keyProp = "";
    searchKeywordNumber = 0;

    viewSettingsBox.removeAll();
    String file = vwr.getProperty("String", "filename", null).toString();
    String ext = NBOFileHandler.getExt(new File(file));

    if (PT.isOneOf(ext, NBOFileHandler.EXTENSIONS))
      notifyLoad_s();

    return panel;
  }


  protected void doComboBasisOperationAction() {
    operator = comboBasisOperation.getSelectedIndex();
    if (operator > 0)
      changeKey(getBasisOperations(comboBasisOperation.getSelectedItem().toString().trim().split(" ")[0]));
  }

  protected void buildHome() {
    rbSelection = -1;
    opList.removeAll();
    comboBasisOperation.setVisible(false);
    keyWdBtn.setVisible(false);
    opList.setLayout(new GridBagLayout());
    opList.setBackground(Color.white);
    searchKeywordNumber = KEYWD_WEBHELP;
    viewSettingsBox.setVisible(false);
    GridBagConstraints c = new GridBagConstraints();
    keyProp = "";
    c.fill = GridBagConstraints.HORIZONTAL;
    for (int i = 0; i < searchButtonLabels.length * 2; i += 2) {
      c.gridy = i;
      c.gridx = 0;
      c.gridwidth = 1;

      JButton btn = new JButton(searchButtonLabels[i / 2]);
      final int index = i / 2;
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          if (nboService.isWorking()) {
            vwr.alert("Please wait for NBOServe to finish working");
            return;
          }
          keywordClicked(getNboKeywordNumber(index));
        }
      });
      opList.add(btn, c);
      c.gridx = 1;
      String st = keyW[i / 2].substring(keyW[i / 2].indexOf(":") + 1);
      JTextArea jt = new JTextArea(st);
      jt.setBackground(null);
      jt.setFont(searchTextAreaFont);
      jt.setEditable(false);
      opList.add(jt, c);
      c.gridy = i + 1;
      c.gridx = 0;
      c.gridwidth = 2;
      JSeparator sp = new JSeparator(SwingConstants.HORIZONTAL);
      sp.setForeground(Color.BLACK);
      sp.setSize(350, 10);
      opList.add(sp, c);
    }
    opList.repaint();
    opList.revalidate();
    back.setEnabled(false);
  }

  protected String getSearchHelpURL() {
    return (searchKeywordNumber == KEYWD_WEBHELP ? "search_help.htm" : keyProp
        .equals("E2") ? "search_e2pert_help.htm" : "search_" + keyProp
        + "_help.htm");
  }

  protected void doSetSpin() {
    if (alphaSpin.isSelected()) {
      setBonds(true);
      showAtomNums(true);
    } else {
      setBonds(false);
      showAtomNums(false);
    }
    switch (searchKeywordNumber) {
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_DIPOLE:
      postListRequest("o", comboSearchOrb1);
      break;
    case KEYWD_E2PERT:
      postListRequest("d nbo", comboSearchOrb1);
      postListRequest("a nbo", comboSearchOrb2);
      break;
    case KEYWD_NLMO:
      postListRequest("o", comboSearchOrb2);
      break;
    case KEYWD_STERIC:
      postListRequest("d", comboSearchOrb1);
      postListRequest("d'", comboSearchOrb2);
      break;
    case KEYWD_CMO:
      postListRequest("n", comboSearchOrb1);
      break;
    }
  }

  protected void doSelectKeyword() {
    if (searchKeywordNumber == KEYWD_NRT && comboUnit1.getModel().getSize() > 0)
      comboUnit1.setSelectedIndex(0);
    SB script = new SB();
    if (relabel) {
      showAtomNums(alphaSpin.isSelected());
      script
          .append("select add {*}.bonds; color bonds lightgrey; select none;");
      for (int i = 0; i < nBonds; i++) {
        script.append("MEASUREMENT ID '" + i + "' off;");
      }
    }
    script.append("isosurface delete; select off;refresh");
    runScriptNow(script.toString());
    buildHome();
  }

  private void changeKey(final String[] s) {

    secondPick = true;
    back.setEnabled(true);
    viewSettingsBox.setVisible(!jmolOptionNONBO);
    keyWdBtn.setText("<html><font color=black>" + keyProp + "</font></html>");
    keyWdBtn.setVisible(true);
    runScriptNow("isosurface delete;refresh");
    opList.removeAll();

    ButtonGroup btnGroup = new ButtonGroup();

    opList.setLayout(new BoxLayout(opList, BoxLayout.Y_AXIS));
    if (searchKeywordNumber == KEYWD_OPBAS) {
      comboBasisOperation.setVisible(true);
      opList.add(comboBasisOperation);
    }

    for (int i = 0; i < s.length; i++) {
      if (!s[i].trim().startsWith("(")) {
        JLabel lab = new JLabel(s[i]);
        lab.setFont(searchOpListFont);
        lab.setForeground(Color.blue);
        opList.add(lab);
      } else {
        final int num = Integer.parseInt(s[i].substring(s[i].indexOf("(") + 1,
            s[i].indexOf(")"))) - 1;
        rBtns[num] = new JRadioButton(s[i].substring(s[i].indexOf(')') + 1));
        rBtns[num].addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            rbSelection = num;
            getSearchValue(num + 1);
          }
        });
        rBtns[num].setBackground(null);
        opList.add(rBtns[num]);
        btnGroup.add(rBtns[num]);
      }
      opList.add(new JSeparator());
    }

    opList.add(Box.createRigidArea(new Dimension(0, (16 - s.length) * 20)));

    opList.repaint();
    opList.revalidate();

  }

  protected void showMessage() {
    JOptionPane.showMessageDialog(this,
        "Error getting lists, an error may have occured during run");
  }

  protected void getBasisOperations2(boolean andUpdateList2) {
    String b1 = comboBasis1.getSelectedItem().toString();
    String b2 = comboBasis2.getSelectedItem().toString();
    changeKey(new String[] { "Current r(ow),c(olumn) matrix element:",
        "  (1) current <" + b1 + "(r)|" + b2 + "(c)> value",
        "Extremal off-diagonal values for current r orbital:",
        "  (2) max <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "  (3) min <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "Extremal off-diagonal values for current c orbital:",
        "  (4) max <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "  (5) min <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "Extremal off-diagonal values for any (*r,*c) orbitals:",
        "  (6) max <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c",
        "  (7) min <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c" });
    if (andUpdateList2) {
      postListRequest("c", comboSearchOrb2);
    }
  }

  private static String[] getBasisOperations(String operator) {
    return new String[] { "Current [r(ow),c(ol)] matrix element",
        "  (1) current <r|" + operator + "|c> value",
        "Extremal off-diagonal values for current r orbital:",
        "  (2) max <r|" + operator + "|*c> value for current r",
        "  (3) min <r|" + operator + "|*c> value for current r",
        "Extremal off-diagonal values for current c orbital:",
        "  (4) max <*r|" + operator + "|c> value for current c",
        "  (5) min <*r|" + operator + "|c> value for current c",
        "Extremal off-diagonal values for any [*r,*c] orbitals:",
        "  (6) max <*r|" + operator + "|*c> value for any *r,*c",
        "  (7) min <*r|" + operator + "|*c> value for any *r,*c" };
  }

  protected void keywordClicked(int index) throws IllegalArgumentException {
    isNewModel = false;
    searchKeywordNumber = index;
    switch (index) {
    case KEYWD_NPA:
      load(31, false);
      comboAtom2 = null;
      comboBasis1.setSelectedIndex(BASIS_PNAO); // WAS BASIS_NAO ? 
      keyProp = "NPA";
      setKeyword(new String[] { "b", "a", "o PNAO", "u" }, new String[] {
          "Basis: ", "Atom: ", "Orbital: ", "Unit: " });
      changeKey(npa);
      break;
    case KEYWD_NBO:
      load(36, true);
      comboBasis1.setSelectedIndex(BASIS_PNBO);
      keyProp = "NBO";
      setKeyword(new String[] { "b", "o PNBO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nbo);
      break;
    case KEYWD_NLMO:
      load(38, true);
      comboBasis1.setSelectedIndex(BASIS_PNLMO);
      keyProp = "NLMO";
      setKeyword(new String[] { "b", "o PNLMO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nlmo);
      break;
    case KEYWD_BEND:
      load(34, true);
      comboBasis1.setSelectedIndex(BASIS_PNHO);
      keyProp = "BEND";
      setKeyword(new String[] { "b", "o PNHO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(bend);
      break;
    case KEYWD_NRT:
      runScriptNow("set bondpicking true");
      keyProp = "NRT";
      if (isOpenShell)
        setKeyword("s a a' rs".split(" "), new String[] { "Spin: ", "Atom A: ",
            "Atom A': ", "Res Struct: " });
      else
        setKeyword("a a' rs".split(" "), new String[] { "Atom A: ",
            "Atom A': ", "Res Struct: " });
      changeKey(nrt);
      break;
    case KEYWD_E2PERT:
      load(36, true);
      comboBasis1.setSelectedIndex(BASIS_PNBO);
      keyProp = "E2";
      setKeyword(new String[] { "b", "d nbo", "a nbo", "u" }, new String[] {
          "Basis: ", "d-NBO: ", "a-NBO:", "Unit: " });
      changeKey(e2);
      break;
    case KEYWD_STERIC:
      load(38, true);
      comboBasis1.setSelectedIndex(BASIS_PNLMO);
      keyProp = "STERIC";
      setKeyword(new String[] { "b", "d nlmo", "d' nlmo", "u" }, new String[] {
          "Basis: ", "d-NLMO: ", "d'-NLMO:", "Unit: " });
      changeKey(steric);
      break;
    case KEYWD_CMO:
      load(40, true);
      comboBasis1.setSelectedIndex(BASIS_MO);
      keyProp = "CMO";
      file47Keywords = cleanNBOKeylist(get47FileData(true)[1], true);
      if (!file47Keywords.contains("CMO")) {
        runGenNBOJob("CMO");
        return;
      }
      setKeyword(new String[] { "b", "c cmo", "n" }, new String[] { "Basis: ",
          "MO: ", "NBO:" });
      changeKey(mo);
      break;
    case KEYWD_DIPOLE:
      load(38, true);
      comboBasis1.setSelectedIndex(BASIS_PNLMO);
      keyProp = "DIPOLE";
      setKeyword(new String[] { "b", "o", "u" }, new String[] { "Basis: ",
          "Orbital: ", "Unit:" });
      changeKey(dip);
      break;
    case KEYWD_OPBAS:
      load(31, true);
      viewSettingsBox.removeAll();
      comboBasis1 = new JComboBox<String>(NBODialogView.basSet);
      comboBasis1.setUI(new StyledComboBoxUI(180, -1));
      comboBasis1.setEditable(false);
      comboBasisOperation.requestFocus();
      keyProp = "OPBAS";
      setKeyword("b1 r c".split(" "), new String[] { "Basis:", "Row:",
          "Collumn:" });
      changeKey(new String[] {});
      break;
    case KEYWD_BAS1BAS2:
      runScriptNow("set bondpicking true");
      keyProp = "B1B2";
      setKeyword("b1 b2 r c".split(" "), new String[] { "Basis 1:", "Basis 2:",
          "Row:", "Collumn:" });
      getBasisOperations2(false);
      break;
    }
    this.repaint();
    this.revalidate();

    if (index == KEYWD_OPBAS)
      comboBasisOperation.showPopup();
  }

  private void load(int nn, boolean withBondPicking) {
    loadModelFileNow(inputFileHandler.newNBOFileForExt("" + nn)
        + (withBondPicking ? ";set bondpicking true" : ""));
  }

  /**
   * A generalized method for retrieving items from NBO with specific labels.
   * 
   * @param items
   *        Data items coded for use in meta commands
   * @param labels
   *        labels for these
   */
  protected void setKeyword(final String[] items, final String[] labels) {
    viewSettingsBox.removeAll();
    viewSettingsBox.setLayout(new BorderLayout());
    JPanel outerListPanel = new JPanel(new GridLayout(labels.length, 1));
    JPanel innerListPanel = new JPanel(new GridLayout(labels.length, 1));
    for (int i = 0; i < labels.length; i++) {
      final String key = items[i].split(" ")[0];
      outerListPanel.add(new JLabel(labels[i]));
      if (key.equals("b") || key.equals("s")) {
        Box b = Box.createHorizontalBox();

        if (searchKeywordNumber == KEYWD_CMO) {
          b.add(radioOrbMO);
          b.add(radioOrbNBO);
        } else if (key.equals("b")) {
          String str = peeify(comboBasis1.getSelectedItem().toString());
          b.add(new JLabel(str));
          //runScriptQueued("NBO TYPE " + str + ";MO TYPE " + str);
          b.add(Box.createRigidArea(new Dimension(20, 0)));
        }
        b.add(alphaSpin);
        b.add(betaSpin);
        //}
        innerListPanel.add(b);
      } else if (PT.isOneOf(items[i],
          "o PNBO;o PNLMO;r;d nlmo;n;d nbo;o PNHO;o")) {
        comboSearchOrb1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        setComboSearchOrbDefaultAction(key);
        innerListPanel.add(comboSearchOrb1);
        postListRequest(key, comboSearchOrb1);
      } else if (PT.isOneOf(items[i], "c;d' nlmo;a nbo;c cmo;o PNAO")) {
        comboSearchOrb2 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest(key, comboSearchOrb2);
        setComboSearchOrb2DefaultAction(key, items[i]);
        innerListPanel.add(comboSearchOrb2);
      } else if (key.equals("u")) {
        comboUnit1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest(key, comboUnit1);
        unitLabel = new JLabel();
        Box box = Box.createHorizontalBox();
        box.add(comboUnit1);
        box.add(unitLabel);
        innerListPanel.add(box);
        unitLabel.setVisible(false);
      } else if (key.equals("rs")) {
        comboUnit1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("r", comboUnit1);
        innerListPanel.add(comboUnit1);
      } else if (key.equals("a")) {
        comboAtom1 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("a", comboAtom1);
        comboAtom1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (comboAtom2 == null)
              runScriptQueued("select on; select {*}["
                  + (comboAtom1.getSelectedIndex()) + "]");
            else
              runScriptQueued("select on; select remove{*}; "
                  + "select add {*}[" + (comboAtom1.getSelectedIndex())
                  + "]; select add {*}[" + (comboAtom2.getSelectedIndex())
                  + "]");
          }
        });
        innerListPanel.add(comboAtom1);
      } else if (key.equals("a'")) {
        comboAtom2 = new JComboBox<String>(new DefaultComboBoxModel<String>());
        postListRequest("a", comboAtom2);
        comboAtom2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            runScriptQueued("select on; select remove{*}; " + "select add {*}["
                + (comboAtom1.getSelectedIndex() + 1) + "]; select add {*}["
                + (comboAtom2.getSelectedIndex() + 1) + "]");
          }
        });
        innerListPanel.add(comboAtom2);
      } else if (key.equals("b1")) {
        Box b = Box.createHorizontalBox();
        b.add(comboBasis1);
        comboBasis1.setSelectedIndex(BASIS_AO);
        comboBasis1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            doSetBasis();
          }
        });
        if (isOpenShell) {
          //b.add(Box.createRigidArea(new Dimension(20,0)));
          b.add(alphaSpin);
          b.add(betaSpin);
        }
        innerListPanel.add(b);
      } else if (key.equals("b2")) {
        comboBasis2 = new JComboBox<String>(NBODialogView.basSet);
        comboBasis2.setUI(new StyledComboBoxUI(180, -1));
        comboBasis2.setSelectedIndex(1);
        comboBasis2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            getBasisOperations2(true);
          }
        });
        innerListPanel.add(comboBasis2);
      } else if (key.equals("b12")) {
        comboBasis1 = new JComboBox<String>(NBODialogView.basSet);
        comboBasis1.setUI(new StyledComboBoxUI(180, -1));
        innerListPanel.add(comboBasis1);
      }
    }
    logCmd(keyProp + " Search Results:");
    JLabel lab = new JLabel("Settings");
    lab.setFont(nboFont);

    lab.setOpaque(true);
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    viewSettingsBox.add(lab, BorderLayout.NORTH);
    viewSettingsBox.add(outerListPanel, BorderLayout.WEST);
    viewSettingsBox.add(innerListPanel, BorderLayout.CENTER);
  }

  private void setComboSearchOrbDefaultAction(final String key) {
    comboSearchOrb1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (comboSearchOrb1.getSelectedIndex() <= 0)
          return;
        checkOptionClickForOrbitalSelection();
        if (key.equals("n")) {
          showOrbJmol("NBO", comboSearchOrb1.getSelectedIndex(), key);
          radioOrbNBO.doClick();
        } else
          showOrbJmol(comboBasis1.getSelectedItem().toString(),
              comboSearchOrb1.getSelectedIndex(), key);
      }
    });
  }

//  private void setComboSearchOrbsActionForB1B2() {
//    if (false) {
//    comboSearchOrb
//        .removeActionListener(comboSearchOrb.getActionListeners()[0]);
//    comboSearchOrb.addActionListener(new ActionListener() {
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        showOrbJmol(comboBasis.getSelectedItem().toString(),
//            comboSearchOrb.getSelectedIndex(), "b1");
//      }
//    });
//    comboSearchOrb2.removeActionListener(comboSearchOrb2
//        .getActionListeners()[0]);
//    comboSearchOrb2.addActionListener(new ActionListener() {
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        showOrbJmol((searchKeywordNumber == KEYWD_OPBAS ? comboBasis
//            : comboBasis2).getSelectedItem().toString(), comboSearchOrb2
//            .getSelectedIndex(), "b2");
//      }
//      
//    });
//    }
//}

  private void setComboSearchOrb2DefaultAction(final String key, final String item) {
    comboSearchOrb2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int index2 = (comboSearchOrb2.isEnabled() ? comboSearchOrb2.getSelectedIndex() : 0);
        if (index2 <= 0)
          return;
        checkOptionClickForOrbitalSelection();
        if (searchKeywordNumber == KEYWD_BAS1BAS2) {
          showOrbJmol(comboBasis2.getSelectedItem().toString(), index2, "b2");
        } else if (key.equals("a")) {
          showOrbJmol("NBO", comboSearchOrb1.getModel().getSize() + (index2 - 1), key);
        } else {
          showOrbJmol(comboBasis1.getSelectedItem().toString(), index2, key);
        }
        if (item.equals("c cmo")) {
          radioOrbMO.doClick();
        }
      }
    });
  }




  protected void doSetBasis() {
    switch (searchKeywordNumber) {
    case KEYWD_BAS1BAS2:
      if (comboBasis2 == null)
        return;
      getBasisOperations2(false);
      postListRequest("r", comboSearchOrb1);
      break;
    case KEYWD_OPBAS:
      loadModelFileNow(""
          + inputFileHandler.newNBOFileForExt(""
              + (31 + comboBasis1.getSelectedIndex())));
      postListRequest("r", comboSearchOrb1);
      postListRequest("c", comboSearchOrb2);
      break;
    default:
      getBasisOperations(comboBasisOperation.getSelectedItem().toString().trim().split(" ")[0]);
    }
  }

  /**
   * add "P" except to MO and AO
   * 
   * @param str
   * @return trimmed and P-prepended label
   */
  private String peeify(String str) {
    str = str.trim();
    return (!str.equals("MO") && !str.equals("AO") && str.charAt(0) != 'P' ? "P"
        + str
        : str);
  }

  protected void getSearchValue(int op) {
    // check orbital is selected
    JComboBox<String> orb1 = comboSearchOrb1;
    JComboBox<String> orb2 = null, atom1 = null, atom2 = null, unit1 = null;
    String labelOrb1 = "ORB_1", labelOrb2 = "ORB_2", 
        labelAtom1 = "ATOM_1", labelAtom2 = "ATOM_2", 
        labelUnit1 = "UNIT_1";
    int offset1 = 0, offset2 = 0;

    final SB sb = getMetaHeader(false);
    postAddGlobalI(sb, "KEYWORD", searchKeywordNumber, null);
    boolean isLabel = false;
    boolean isLabelBonds = false;

    // generally an offset is 1 because meta commands are 1-based, 
    // but if a combobox has a <select ...> in position 1, then the offset will be 0

    switch (searchKeywordNumber) {
    case KEYWD_NPA:
      orb1 = comboSearchOrb2;
      atom1 = comboAtom1;
      unit1 = comboUnit1;
      if (op > 10) {
        isLabel = true;
        op = 12;
      }
      break;
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_NLMO:
      // just orb1
      break;
    case KEYWD_E2PERT:
      labelOrb1 = "d_NBO_1";
      labelOrb2 = "a_NBO";
      orb2 = comboSearchOrb2;
      offset2 = orb1.getModel().getSize() + 1;
      break;
    case KEYWD_NRT:
      orb1 = null;
      atom1 = comboAtom1;
      atom2 = comboAtom2;
      unit1 = comboUnit1;
      labelUnit1 = "RES_STR";
      isLabel = (op == 9);
      isLabelBonds = (op == 10);
      break;
    case KEYWD_STERIC:
      labelOrb1 = "d_NBO_1";
      orb2 = comboSearchOrb2;
      labelOrb2 = "d_NBO_2";
      unit1 = comboUnit1;
      break;
    case KEYWD_CMO:
      labelOrb1 = "NBO";
      orb2 = comboSearchOrb2;
      labelOrb2 = "CMO";
      break;
    case KEYWD_DIPOLE:
      unit1 = comboUnit1;
      break;
    case KEYWD_OPBAS:
      labelOrb1 = "ROW";
      orb2 = comboSearchOrb2;
      labelOrb2 = "COLUMN";
      postAddGlobalI(sb, "OPERATOR", operator, null);
      postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      break;
    case KEYWD_BAS1BAS2:
      labelOrb1 = "ROW";
      orb2 = comboSearchOrb2;
      labelOrb2 = "COLUMN";
      postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      postAddGlobalI(sb, "BAS_2", 1, comboBasis2);
      break;
    }

    boolean isOK = (orb1 == null || orb1.getSelectedIndex() > 0)
        && (orb2 == null || orb2.getSelectedIndex() > 0);
    if (!isOK)
      return;
    if (orb1 != null)
      postAddGlobalI(sb, labelOrb1, offset1, orb1);
    if (orb2 != null)
      postAddGlobalI(sb, labelOrb2, offset2, orb1);
    if (atom1 != null)
      postAddGlobalI(sb, labelAtom1, 1, atom1);
    if (atom2 != null)
      postAddGlobalI(sb, labelAtom2, 1, atom2);
    if (unit1 != null)
      postAddGlobalI(sb, labelUnit1, 1, unit1);
    postAddGlobalI(sb, "OPT_" + keyProp, op, null);


    //
    //    switch (searchKeywordNumber) {
    //    case KEYWD_NPA:
    //      sb.append("GLOBAL I_ATOM_1 " + (comboAtom1.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_UNIT_1 " + (comboUnit1.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_ORB_1 " + (comboSearchOrb2.getSelectedIndex()) + sep);
    //      if (op > 10) {
    //        isLabel = true;
    //        op = 12;
    //      }
    //      break;
    //    case KEYWD_NBO:
    //    case KEYWD_BEND:
    //    case KEYWD_NLMO:
    //      sb.append("GLOBAL I_ORB_1 " + (comboSearchOrb1.getSelectedIndex()) + sep);
    //      break;
    //    case KEYWD_E2PERT:
    //      sb.append("GLOBAL I_d_NBO_1 " + (comboSearchOrb1.getSelectedIndex())
    //          + sep);
    //      sb.append("GLOBAL I_a_NBO "
    //          + (comboSearchOrb2.getSelectedIndex()
    //              + comboSearchOrb1.getModel().getSize() + 1) + sep);
    //      sb.append("GLOBAL I_UNIT_1 " + (comboUnit1.getSelectedIndex() + 1) + sep);
    //      break;
    //    case KEYWD_NRT:
    //      sb.append("GLOBAL I_ATOM_1 " + (comboAtom1.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_ATOM_2 " + (comboAtom2.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_RES_STR " + (comboUnit1.getSelectedIndex() + 1) + sep);
    //      isLabel = (op == 9);
    //      isLabelBonds = (op == 10);
    //      break;
    //    case KEYWD_STERIC:
    //      sb.append("GLOBAL I_d_NBO_1 " + (comboSearchOrb1.getSelectedIndex())
    //          + sep);
    //      sb.append("GLOBAL I_d_NBO_2 " + (comboSearchOrb2.getSelectedIndex())
    //          + sep);
    //      sb.append("GLOBAL I_UNIT_1 " + (comboUnit1.getSelectedIndex() + 1) + sep);
    //      break;
    //    case KEYWD_CMO:
    //      sb.append("GLOBAL I_CMO " + (comboSearchOrb2.getSelectedIndex()) + sep);
    //      sb.append("GLOBAL I_NBO " + (comboSearchOrb1.getSelectedIndex()) + sep);
    //      break;
    //    case KEYWD_DIPOLE:
    //      sb.append("GLOBAL I_ORB_1 " + (comboSearchOrb1.getSelectedIndex()) + sep);
    //      sb.append("GLOBAL I_UNIT_1 " + (comboUnit1.getSelectedIndex() + 1) + sep);
    //      break;
    //    case KEYWD_OPBAS:
    //      sb.append("GLOBAL I_BAS_1 " + (comboBasis.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_OPERATOR " + operator + sep);
    //      sb.append("GLOBAL I_ROW " + (comboSearchOrb1.getSelectedIndex()) + sep);
    //      sb.append("GLOBAL I_COLUMN " + (comboSearchOrb2.getSelectedIndex()) + sep);
    //      break;
    //    case KEYWD_BAS1BAS2:
    //      sb.append("GLOBAL I_BAS_1 " + (comboBasis.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_BAS_2 " + (comboBasis2.getSelectedIndex() + 1) + sep);
    //      sb.append("GLOBAL I_ROW " + (comboSearchOrb1.getSelectedIndex()) + sep);
    //      sb.append("GLOBAL I_COLUMN " + (comboSearchOrb2.getSelectedIndex()) + sep);
    //      break;
    //    }

    if (relabel) {
      runScriptQueued("select add {*}.bonds; color bonds lightgrey; select none; measurements off");
      showAtomNums(alphaSpin.isSelected());
      relabel = false;
    }
    if (isLabel) {
      relabel = true;
      postNBO_s(sb, NBOService.MODE_SEARCH_LABEL, null, "Getting labels");
    } else if (isLabelBonds) {
      relabel = true;
      nBonds = 0;
      //runScriptQueued("select {*};label off;select remove {*}");
      runScriptQueued("select add {*}.bonds; color bonds [170,170,170]; select none");
      postNBO_s(sb, NBOService.MODE_SEARCH_LABEL_BONDS, null,
          "Getting bonds list");
    } else {
      postNBO_s(sb, NBOService.MODE_SEARCH_VALUE, null, "Getting value...");
    }
  }

  private boolean secondPick = true;

  /**
   * Runs when list is finished being sent by nboServe -E2PERT: orbital
   * numbering of second list offset by length of first -NPA, STERIC, DIPOLE:
   * checks for more than 1 unit -NRT: gets res structure info
   * 
   * @param cb
   */
  protected void processReturnedSearchList(JComboBox<String> cb) {
    DefaultComboBoxModel<String> list = (DefaultComboBoxModel<String>) cb.getModel();
    switch (searchKeywordNumber) {
    case KEYWD_E2PERT:
      if (cb == comboSearchOrb2) {
        //Relabel a-nbo to correct orbital number
        int offset = comboSearchOrb1.getModel().getSize() - 1; // list includes "<select an orbital>"
        int sz = cb.getModel().getSize();
        comboSearchOrb2.setEnabled(false);
//        ActionListener listener = comboSearchOrb2.getActionListeners()[0];
//        comboSearchOrb2.removeActionListener(listener);
        for (int i = 1; i < sz; i++) {
          String s = list.getElementAt(i);
          list.removeElementAt(i);
          s = "   " + (offset + i) + s.substring(s.indexOf("."));
          list.insertElementAt(s, i);
        }
        comboSearchOrb2.setEnabled(true);
//        comboSearchOrb2.addActionListener(listener);
      }
      break;
    case KEYWD_NPA:
    case KEYWD_STERIC:
    case KEYWD_DIPOLE:
      if (cb == comboUnit1)
        if (list.getSize() == 1) {
          comboUnit1.setVisible(false);
          unitLabel.setVisible(true);
          unitLabel.setText(list.getElementAt(0).substring(6)); // "unit:"
        } else {
          unitLabel.setVisible(false);
          comboUnit1.setVisible(true);
        }
      break;
    case KEYWD_NRT:
      if (cb == comboUnit1) {
        changeKey(nrt);
        //Parsing RS list here ensures RS list will be in .nbo file
        inputFileHandler.parseRSList();
        comboUnit1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (isOpenShell)
              setResStruct(comboUnit1.getSelectedIndex() + 1,
                  alphaSpin.isSelected());
            else
              setResStruct(comboUnit1.getSelectedIndex() + 1, true);
          }
        });
        setResStruct(1, alphaSpin.isSelected());
      }
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      if (cb == comboBasis2) {
        //setComboSearchOrbsActionForB1B2();
        comboSearchOrb1.setSelectedIndex(0);
        comboSearchOrb2.setSelectedIndex(0);

      }
    }
  }

  /**
   * Changes bonds and labels on the Jmol model when new RS is selected
   * 
   * @param rsNum
   *        - index of RS in Combo Box
   * @param alpha
   */
  protected void setResStruct(int rsNum, boolean alpha) {
    ChooseList chooseList = inputFileHandler.chooseList;

    if (chooseList == null)
      return;

    int[][] resStructDef = inputFileHandler.resStructDef;
    Map<Integer, String> resStructList = inputFileHandler.resStructList;
    int sz = resStructDef.length;
    Map<String, String> lonePairs = new Hashtable<String, String>();
    int[][] tmp = new int[sz][sz];
    for (int i = 0; i < sz; i++)
      for (int j = 0; j < sz; j++)
        tmp[i][j] = resStructDef[i][j];
    String rs = resStructList.get(new Integer(rsNum));
    if (rs != null) {
      String[] rsList = rs.split(",");
      int inc;
      for (int i = 0; i < rsList.length; i++) {
        if (rsList[i].contains("("))
          inc = -1;
        else
          inc = 1;
        String bond = rsList[i].replaceAll("[\\D]", " ").trim();
        String[] toks = bond.split("\\s+");
        int a1 = Integer.parseInt(toks[0]) - 1;
        if (toks.length < 2) {
          tmp[a1][a1] += inc;
        } else {
          int a2 = Integer.parseInt(toks[1]) - 1;
          tmp[a1][a2] += inc;
          tmp[a2][a1] += inc;
        }
      }
    }
    vwr.ms.deleteAllBonds();
    int[] bondCounts = new int[vwr.ms.ac];
    for (int i = 0; i < sz; i++) {
      for (int j = i; j < sz; j++) {
        if (tmp[i][j] > 0) {
          if (i == j) {
            lonePairs.put(new Integer(i + 1).toString(),
                new Integer(tmp[i][j]).toString());
            //vwr.ms.at[i].setValence(vwr.ms.at[i].getValence() + 2*tmp[i][j]);
            continue;
          }
          if (tmp[i][j] > 0) {
            int mad = (tmp[i][j] > 2) ? 150 : 250;
            vwr.ms.bondAtoms(vwr.ms.at[i], vwr.ms.at[j], tmp[i][j],
                (short) mad, vwr.ms.bsVisible, 0, true, true);
            bondCounts[i] += tmp[i][j];
            bondCounts[j] += tmp[i][j];
          }
        }
      }
    }
    if (nboView) {
      runScriptQueued("select add {*}.bonds;color bonds lightgrey;"
          + "wireframe 0.1;select remove {*}");
    }
    for (int i = 0; i < vwr.ms.ac; i++) {
      vwr.ms.at[i].setFormalCharge(0);
      vwr.ms.at[i].setValence(bondCounts[i]);
    }
    SB sb = new SB();
    vwr.ms.fixFormalCharges(vwr.getAllAtoms());
    for (int i = 1; i <= vwr.ms.ac; i++) {
      sb.append("select (atomno=" + i + ");label ");
      String atNum = new Integer(i).toString();
      String lp;
      if ((lp = lonePairs.get(atNum)) != null)
        if (!lp.equals("0"))
          sb.append("<sup>(" + lp + ")</sup>");
      sb.append("%a");
      int charge = vwr.ms.at[i - 1].getFormalCharge();
      if (charge != 0)
         sb.append("<sup>" + Math.abs(charge) + (charge > 0 ? "+" : charge < 0 ? "-" : "") + "</sup>");
      sb.append(";");
    }
    runScriptQueued(sb.toString());
    runScriptQueued("select remove{*}; " + "select add (atomno="
        + (comboAtom1.getSelectedIndex()) + ");" + "select add (atomno="
        + (comboAtom2.getSelectedIndex()) + ");");

  }



  /**
   * 
   * @param type
   * @param i
   * @param id
   */
  protected void showOrbJmol(String type, int i, String id) {
    if (i <= 0)
      return;
    id = fixID(id);
    runScriptQueued("select visible;isosurface ID \""
        + id
        + "\" delete;"
        + getJmolIsosurfaceScript(id, peeify(type), i, betaSpin.isSelected(),
            false));
  }

  /**
   * Fix ID for isosurface command purposes only
   * 
   * @param id
   * @return id with quotes replaced by underscore
   * 
   */
  private String fixID(String id) {
    return PT.replaceAllCharacters(id, "'\"", "_");
  }

  /**
   * Click the current radio option button in certain cases after an orbital is
   * selected.
   */
  protected void checkOptionClickForOrbitalSelection() {
    if (rbSelection < 0)
      return;
    boolean doClick = false;
    switch (searchKeywordNumber) {
    default:
    case KEYWD_NRT:
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      break;
    case KEYWD_NPA:
      doClick = (rbSelection > 4 && rbSelection < 10);
      break;
    case KEYWD_NBO:
      doClick = (rbSelection < 4);
      break;
    case KEYWD_BEND:
      doClick = (rbSelection < 6);
      break;
    case KEYWD_E2PERT:
      doClick = (rbSelection < 3);
      break;
    case KEYWD_NLMO:
      doClick = true;
      break;
    case KEYWD_STERIC:
      doClick = (rbSelection > 1 && rbSelection < 4);
      break;
    case KEYWD_CMO:
      doClick = true;
      break;
    case KEYWD_DIPOLE:
      doClick = (rbSelection > 2 && rbSelection < 6);
      break;
    }
    if (doClick)
      rBtns[rbSelection].doClick();
  }

  /**
   * callback for Jmol atom or bond click.
   * 
   * 
   * @param atomnoOrBondInfo
   *        either a single number or
   *        ["bond","1 3 O1 #1 -- C2 #2 1.171168",0.0,0.0,0.58555] as a String
   */
  @SuppressWarnings("unchecked")
  protected void notifyPick_s(String atomnoOrBondInfo) {
    runScriptNow("isosurface delete");
    String[] tok = atomnoOrBondInfo.split(",");
    if (tok.length < 2) {
      int n = PT.parseInt(atomnoOrBondInfo);
      switch (n == Integer.MIN_VALUE ? KEYWD_NBO : searchKeywordNumber) {
      case KEYWD_NBO:
      case KEYWD_BEND:
      case KEYWD_NLMO:
      case KEYWD_E2PERT:
        showOrbital(findNextAtomicOrbital(atomnoOrBondInfo, (AbstractListModel<String>) comboSearchOrb1.getModel()));
        return;
      case KEYWD_CMO:
        showOrbital(findNextAtomicOrbital(atomnoOrBondInfo, (AbstractListModel<String>) comboSearchOrb2.getModel()));
        return;
      case KEYWD_NPA:
      case KEYWD_NRT:
      case KEYWD_STERIC:
      case KEYWD_DIPOLE:
      case KEYWD_OPBAS:
      case KEYWD_BAS1BAS2:
        break;
      }
      if (comboAtom1 != null && comboAtom2 == null) {
        comboAtom1.setSelectedIndex(n);
        if (rbSelection < 3 && rbSelection > -1)
          rBtns[rbSelection].doClick();
      } else if (comboAtom1 != null && comboAtom2 != null) {
        secondPick = !secondPick;
        if (secondPick)
          comboAtom2.setSelectedIndex(n);
        else
          comboAtom1.setSelectedIndex(n);
      }
      return;
    }
    String[] tok2 = tok[1].split(" ");
    String at1 = tok2[2];
    String at2 = tok2[5];
    switch (searchKeywordNumber) {
    case KEYWD_NBO:
    case KEYWD_NLMO:
    case KEYWD_DIPOLE:
    case KEYWD_CMO:
      comboSearchOrb1.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_BEND:
      comboSearchOrb1.setSelectedIndex(pickNHO_s(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_NRT:
      tok = atomnoOrBondInfo.split(",");
      int a1 = Integer.parseInt(at1.replaceAll("[\\D]", ""));
      int a2 = Integer.parseInt(at2.replaceAll("[\\D]", ""));
      this.comboAtom1.setSelectedIndex(a1);
      this.comboAtom2.setSelectedIndex(a2);
      if (rbSelection > 2 && rbSelection < 6)
        rBtns[rbSelection].doClick();
      return;
    case KEYWD_E2PERT:
    case KEYWD_STERIC:
      tok = atomnoOrBondInfo.split(",");
      tok2 = tok[1].split(" ");
      String bond = tok2[2] + "-" + tok2[5];
      String str = comboSearchOrb1.getSelectedItem().toString().replace(" ", "");
      if (str.contains(bond)) {
        comboSearchOrb1.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb1));
        return;
      }
      str = comboSearchOrb2.getSelectedItem().toString().replace(" ", "");
      if (str.contains(bond)) {
        comboSearchOrb2.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb2));
        return;
      }
      secondPick = !secondPick;
      if (secondPick)
        comboSearchOrb2.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb2));
      else
        comboSearchOrb1.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb1));
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      JComboBox<String> tmpBas = comboBasis1;
      if (searchKeywordNumber == KEYWD_BAS1BAS2) {
        if (!secondPick)
          tmpBas = comboBasis2;
      }
      switch (tmpBas.getSelectedIndex()) {
      case BASIS_AO:
      case BASIS_PNAO:
      case BASIS_NAO:
      case BASIS_MO:
      default:
        secondPick = !secondPick;
        break;
      case BASIS_PNHO:
      case BASIS_NHO:
        secondPick = !secondPick;
        if (secondPick)
          comboSearchOrb2.setSelectedIndex(pickNHO_s(at1, at2, comboSearchOrb2));
        else
          comboSearchOrb1.setSelectedIndex(pickNHO_s(at1, at2, comboSearchOrb1));
        break;
      case BASIS_PNBO:
      case BASIS_NBO:
      case BASIS_PNLMO:
      case BASIS_NLMO:
        bond = at1 + "-" + at2;
        str = comboSearchOrb1.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          comboSearchOrb1.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb1));
          return;
        }
        str = comboSearchOrb2.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          comboSearchOrb2.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb2));
          return;
        }
        secondPick = !secondPick;
        if (secondPick)
          comboSearchOrb2.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb2));
        else
          comboSearchOrb1.setSelectedIndex(pickNBO_s(at1, at2, comboSearchOrb1));
        break;
      }
    }
  }

  /**
   * An NBO type bond was clicked
   * 
   * @param at1
   * @param at2
   * @param cb
   * @return the orbital index, one-based
   */
  private int pickNBO_s(String at1, String at2,
                        JComboBox<String> cb) {
    return selectOnOrb_s(at1 + "-" + at2, null, cb);
  }

  /**
   * An NHO type bond was clicked
   * 
   * @param at1
   * @param at2
   * @param cb
   * @return the orbital index, one-based
   */
  private int pickNHO_s(String at1, String at2, JComboBox<String> cb) {
    return selectOnOrb_s(at1 + "(" + at2 + ")", at2 + "(" + at1 + ")", cb);
  }

  /**
   * find next orbital from clicking a bond. Note that these lists have [select
   * an orbital] at the top, so the number returned will be the same as the index on the list
   * 
   * @param b1
   * @param b2
   * @param cb 
   * @return an orbital index -- one-based
   */
  protected int selectOnOrb_s(String b1, String b2, JComboBox<String> cb) {
    DefaultComboBoxModel<String> list = (DefaultComboBoxModel<String>) cb.getModel();
    int curr = 0, size = list.getSize();
    if (currOrb.contains(b1))
      curr = list.getIndexOf(list.getSelectedItem()) - 1;
    for (int i = curr + 1; i < size + curr; i++) {
      String str = list.getElementAt(i % size).replace(" ", "");
      if (str.contains(b1) || b2 != null && str.contains(b2)) {
        int ipt = i % size;
        list.setSelectedItem(list.getElementAt(ipt));
        currOrb = str;
        return ipt;
      }
    }
    return curr;
  }

  /**
   * callback notification that Jmol has loaded a model while SEARCH was active
   * 
   */
  protected void notifyLoad_s() {
    file47Data = null;
    if (vwr.ms.ac == 0)
      return;
    runScriptNow("isosurface delete");

    rbSelection = -1;
    showAtomNums(true);
    setBonds(true);
    if (isOpenShell) {
      alphaSpin.setVisible(true);
      betaSpin.setVisible(true);
    } else {
      alphaSpin.setVisible(false);
      betaSpin.setVisible(false);
    }

    optionBox.setVisible(true);
    if (searchKeywordNumber > 0 && isNewModel) {
      keywordClicked(searchKeywordNumber);
    }
  }

  @Override
  protected void showConfirmationDialog(String st, File newFile, String ext) {
    int i = JOptionPane.showConfirmDialog(this, st, "Message",
        JOptionPane.YES_NO_OPTION);
    if (i == JOptionPane.YES_OPTION) {
      JDialog d = new JDialog(this);
      d.setLayout(new BorderLayout());
      JTextPane tp = new JTextPane();
      d.add(tp, BorderLayout.CENTER);
      d.setSize(new Dimension(500, 600));
      tp.setText(inputFileHandler.getFileData(NBOFileHandler.newNBOFile(
          newFile, "nbo").toString()));
      d.setVisible(true);
    }
  }

  ////////////////////////// SEARCH POSTS TO NBO ///////////////////

  private void postListRequest(String get, JComboBox<String> cb) {
    int mode = NBOService.MODE_SEARCH_LIST;
    SB sb = getMetaHeader(false);
    String cmd;
    int metaKey = searchKeywordNumber;
    if (metaKey >= KEYWD_OPBAS) {
      cmd = "o";
      JComboBox<String> tmpBas = ((get.startsWith("c") && metaKey == KEYWD_BAS1BAS2) ? comboBasis2
          : comboBasis1);
      switch (tmpBas.getSelectedIndex()) {
      case BASIS_AO:
      case BASIS_PNAO:
      case BASIS_NAO:
        metaKey = 1;
        break;
      case BASIS_PNBO:
      case BASIS_NBO:
        metaKey = 2;
        break;
      case BASIS_PNHO:
      case BASIS_NHO:
        metaKey = 3;
        break;
      case BASIS_PNLMO:
      case BASIS_NLMO:
        metaKey = 5;
        break;
      case BASIS_MO:
        cmd = "c";
        metaKey = 8;
      }
      postAddGlobalI(sb, "BAS_1", 1, tmpBas);
    } else {
      postAddGlobalI(sb, "BAS_1", 1, comboBasis1);
      cmd = get.split(" ")[0];
    }
    postAddGlobalI(sb, "KEYWORD", metaKey, null);
    postAddCmd(sb, cmd);
    if (get.equals("c") && searchKeywordNumber == KEYWD_CMO)
      mode = NBOService.MODE_SEARCH_LIST_MO;
    postNBO_s(sb, mode, cb, "Getting list " + cmd);
  }

  /**
   * Post a request to NBOServe with a callback to processNBO_s.
   * 
   * @param sb
   *        command data
   * @param mode
   *        type of request
   * @param cb
   *        optional JComboBox to fill
   * @param statusMessage
   */
  private void postNBO_s(SB sb, final int mode,
                         final JComboBox<String> cb,
                         String statusMessage) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO_s(req, mode, cb);
      }
    }, statusMessage, "s_cmd.txt", sb.toString());
    nboService.postToNBO(req);
  }

  protected void showMax(String line) {
    //BH not implemented?  
  }

  /**
   * Process the reply from NBOServe.
   * 
   * @param req
   * @param mode
   * @param cb 
   */
  protected void processNBO_s(NBORequest req, int mode,
                              JComboBox<String> cb) {
    String[] lines = req.getReplyLines();
    String line;
    DefaultComboBoxModel<String> list;
    switch (mode) {
    case NBOService.MODE_SEARCH_VALUE:
      line = lines[0];
      if (isOpenShell) {
        String spin = (alphaSpin.isSelected() ? "&uarr;" : "&darr;");
        int ind = line.indexOf(')') + 1;
        line = line.substring(0, ind) + spin + line.substring(ind);
      }
      logValue(" " + line);
      if (line.contains("*"))
        showMax(line);
      break;
    case NBOService.MODE_SEARCH_LIST:
      list = (DefaultComboBoxModel<String>)cb.getModel();
      list.removeAllElements();
      if (cb == comboAtom1 || cb == comboAtom2)
        list.addElement("<select an atom>");
      else if (cb == comboSearchOrb1 || cb == comboSearchOrb2)
        list.addElement("<select an orbital>");
      for (int i = 0; i < lines.length; i++) {
        list.addElement(lines[i]);
      }
      processReturnedSearchList(cb);
      break;
    case NBOService.MODE_SEARCH_LIST_MO:
      list = (DefaultComboBoxModel<String>)cb.getModel();
      for (int i = 0; i < lines.length; i++)
        list.addElement("  " + PT.rep(PT.rep(lines[i], "MO ", ""), " ", ".  "));
      break;
    case NBOService.MODE_SEARCH_LABEL_BONDS:
    case NBOService.MODE_SEARCH_LABEL:
      int i0 = -1;
      for (int i = lines.length; --i >= 0 && lines[i].indexOf("END") < 0;) {
        i0 = i;
      }
      boolean isLabel = (mode == NBOService.MODE_SEARCH_LABEL);
      SB sb = new SB();
      for (int i = i0, pt = 1; i < lines.length; i++, pt++)
        if (isLabel ? !processLabel(sb, lines[i], pt) : !processLabelBonds(sb,
            lines[i]))
          break;
      runScriptQueued(sb.toString());
      break;
    }
  }

  protected boolean processLabel(SB sb, String line, int count) {
    try {
      double val = Double.parseDouble(line);
      val = round(val, 4);
      sb.append(";select{*}[" + (count) + "];label " + val);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  protected boolean processLabelBonds(SB sb, String line) {
    try {
      String[] toks = PT.getTokens(line);
      if (toks.length < 3)
        return false; // must be done
      float order = Float.parseFloat(toks[2]);
      if (order > 0.01) {

        // measure id "m1" {C1} {C3} radius 0 "testing"

        sb.append("font measures 20; measure id 'm" + toks[0] + "_" + toks[1]
            + "' @" + toks[0] + " @" + toks[1] + " radius 0.02 \"" + toks[2]
            + "\";");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

}
