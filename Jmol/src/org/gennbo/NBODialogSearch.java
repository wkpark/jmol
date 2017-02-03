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
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Hashtable;

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

  protected final static String[] npa = { "NPA Atomic Properties:",
      "  (1) NPA atomic charge", "  (2) NPA atomic spin density",
      "  (3) NEC atomic electron configuration",
      "NPA Molecular Unit Properties:", "  (4) NPA molecular unit charge",
      "  (5) NPA molecular unit spin density", "NAO Orbital Properties:",
      "  (6) NAO label", "  (7) NAO orbital population",
      "  (8) NAO orbital energy", "  (9) NAO orbital spin density",
      "  (10) NMB minimal basis %-accuracy", "Display Options:",
      "  (11) Display atomic charges" }, nbo = { "NBO Orbital Properties:",
      "  (1) NBO orbital label", "  (2) NBO orbital population",
      "  (3) NBO orbital energy", "  (4) NBO ionicity",
      "Natural Lewis Structure Properties:", "  (5) NLS rho(NL)",
      "  (6) NLS %-rho(L)" }, bend = { "NHO Orbital Prperties:",
      "  (1) NHO orbital label", "  (2) NHO orbital population",
      "  (3) NHO orbital energy", "  (4) NHO hybrid composition",
      "  (5) NHO direction angles",
      "  (6) NHO bending deviation from line of centers",
      "  (7) Strongest bending deviation for any NHO" }, e2 = {
      "E2 Values for Selected Donor-Acceptor NBOs:",
      "  (1) E(2) interaction for current d/a NBOs",
      "  (2) Strongest E(2) interaction for current d-NBO",
      "  (3) Strongest E(2) interaction for current a-NBO",
      "  (4) Strongest E(2) interaction for any d/a NBOs",
      "Intermolecular E2 Options:",
      "  (5) Strongest intermolecular E(2) for current unit",
      "  (6) Strongest intermolecular E(2) for any units" }, nlmo = {
      "NLMO Orbital Properties:", "  (1) NLMO orbital label",
      "  (2) NLMO population", "  (3) NLMO orbital energy",
      "  (4) NLMO %-NBO parentage", "NLMO Delocalization Tail Properties:",
      "  (5) NLMO delocalization tail population",
      "  (6) NLMO delocalization tail NBO components" }, nrt = {
      "Atom (A) Properties:", "  (1) atomic valency (total)",
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
          "  (7) Strongest PW-SXE between any units" }, mo = {
          "Character of current MO (c):", "  (1) Current MO energy and type",
          "  (2) Bonding character of current MO",
          "  (3) Nonbonding character of current MO",
          "  (4) Antibonding character of current MO",
          "NBO (n) %-contribution to selected MO (c):",
          "  (5) %-contribution of current NBO to current MO",
          "  (6) Largest %-contribution to current MO from any NBO",
          "  (7) Largest %-contribution of current NBO to any MO" }, dip = {
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
          "  (9) NL-type contribution to unit dipole" }, op = {
          " S    : overlap (unit) operator",
          " F    : 1e Hamiltonian (Fock/Kohn-Sham) operator",
          " K    : kinetic energy operator",
          " V    : 1e potential (nuclear-electron attraction) operator",
          " DM   : 1e density matrix operator",
          " DIx  : dipole moment operator (x component)",
          " DIy  : dipole moment operator (y component)",
          " DIz  : dipole moment operator (z component)" };

  int operator = 1;
  protected JComboBox<String> orb, orb2, unit, at1, at2, bas2, opBas;

  protected JLabel unitLabel;
  protected DefaultComboBoxModel<String> list1, list2, list3;
  private int[][] resStructDef;
  private Hashtable<Integer, String> resStructList;
  protected boolean relabel;

  private Box optionBox;
  private JButton keyWdBtn;
  protected JPanel opList;
  protected JRadioButton[] rBtns = new JRadioButton[12];
  protected JRadioButton moRb, nboRb;
  protected int rbSelection;

  protected int nBonds;
  private JButton back;

  private String keyProp;
  private int nboKeywordNumber = KEYWD_WEBHELP;

  protected NBODialogSearch(JFrame f) {
    super(f);
  }

  protected JPanel buildSearchPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());    
    panel.add(createViewSearchJobBox(NBOFileHandler.MODE_SEARCH), BorderLayout.NORTH);
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
    moRb = new JRadioButton("MO");
    moRb.setSelected(true);
    moRb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        showOrbJmol("MO", orb2.getSelectedIndex() , "MO");
      }
    });
    moRb.setBackground(null);
    nboRb = new JRadioButton("NBO");
    nboRb.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        showOrbJmol("PNBO", orb.getSelectedIndex(), "NBO");
      }
    });
    nboRb.setBackground(null);
    bg.add(moRb);
    bg.add(nboRb);
    /////SELECT KEYWORD///////////
    Box optionBox2 = Box.createVerticalBox();
    opList = new JPanel();
    opBas = new JComboBox<String>(op);
    opBas.setUI(new StyledComboBoxUI(150, 350));
    back = new JButton("<html>&#8592Back</html>");

    opBas.setMaximumSize(new Dimension(350, 30));
    opBas.setAlignmentX(0.0f);

    opBas.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String s = opBas.getSelectedItem().toString();
        operator = opBas.getSelectedIndex();
        changeKey(opBas(s.trim().split(" ")[0]));
      }
    });
    opBas.setVisible(false);

    optionBox2.add(opBas);
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

    comboBasis = new JComboBox<String>(NBODialogView.basSet);
    comboBasis.setUI(new StyledComboBoxUI(180, -1));

    inputFileHandler.setBrowseEnabled(true);

    keyProp = "";
    nboKeywordNumber = 0;

    viewSettingsBox.removeAll();
    String file = vwr.getProperty("String", "filename", null).toString();
    String ext = NBOFileHandler.getExt(new File(file));

    if (PT.isOneOf(ext, NBOFileHandler.EXTENSIONS))
      notifyLoad_s();

    return panel;
  }

  protected String getSearchHelpURL() {
    return (nboKeywordNumber == KEYWD_WEBHELP ? "search_help.htm"
        : keyProp.equals("E2") ? "search_e2pert_help.htm"
            : "search_" + keyProp + "_help.htm");
  }

  protected void doSetSpin() {
    if (alphaSpin.isSelected()) {
      setBonds(true);
      showAtomNums(true);
    } else {
      setBonds(false);
      showAtomNums(false);
    }
    switch (nboKeywordNumber) {
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_DIPOLE:
      list1.removeAllElements();
      getListSearch("o", list1);
      break;
    case KEYWD_E2PERT:
      list1.removeAllElements();
      getListSearch("d nbo", list1);
      list2.removeAllElements();
      getListSearch("a nbo", list2);
      break;
    case KEYWD_NLMO:
      list2.removeAllElements();
      getListSearch("o", list2);
      break;
    case KEYWD_STERIC:
      list1.removeAllElements();
      getListSearch("d", list1);
      list2.removeAllElements();
      getListSearch("d'", list2);
      break;
    case KEYWD_CMO:
      list1.removeAllElements();
      getListSearch("n", list1);
    }
  }

  protected void doSelectKeyword() {
    if (nboKeywordNumber == KEYWD_NRT && list3.getSize() > 0)
      unit.setSelectedIndex(0);        
    SB script = new SB();
    if (relabel) {
      showAtomNums(alphaSpin.isSelected());  
      script.append("select add {*}.bonds; color bonds lightgrey; select none;");
      for (int i = 0; i < nBonds; i++) {
        script.append("MEASUREMENT ID '" + i + "' off;");
      }
    }
    script.append("isosurface delete; select off;refresh");
    runScriptNow(script.toString());
    buildHome();
  }

  protected void buildHome() {
    rbSelection = -1;
    opList.removeAll();
    opBas.setVisible(false);
    keyWdBtn.setVisible(false);
    opList.setLayout(new GridBagLayout());
    opList.setBackground(Color.white);
    nboKeywordNumber = KEYWD_WEBHELP;
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

  protected void changeKey(final String[] s) {

    secondPick = true;
    back.setEnabled(true);
    viewSettingsBox.setVisible(!jmolOptionNONBO);
    keyWdBtn.setText("<html><font color=black>" + keyProp + "</font></html>");
    keyWdBtn.setVisible(true);
    runScriptNow("isosurface delete;refresh");
    opList.removeAll();

    ButtonGroup btnGroup = new ButtonGroup();

    opList.setLayout(new BoxLayout(opList, BoxLayout.Y_AXIS));
    if (nboKeywordNumber == KEYWD_OPBAS) {
      opBas.setVisible(true);
      opList.add(opBas);
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

  protected void getListSearch(String get, DefaultComboBoxModel<String> list) {

    int mode = NBOService.MODE_SEARCH_LIST;
    SB sb = getMetaHeader(false);
    String key;
    if (nboKeywordNumber >= KEYWD_OPBAS) {
      int tmpKey = nboKeywordNumber;
      String tmpCmd = "o";
      JComboBox<String> tmpBas = ((get.startsWith("c") && nboKeywordNumber == KEYWD_BAS1BAS2) ? bas2
          : comboBasis);
      switch (tmpBas.getSelectedIndex()) {
      case 0:
      case 1:
      case 2:
        tmpKey = 1;
        break;
      case 3:
      case 4:
        tmpKey = 3;
        break;
      case 5:
      case 6:
        tmpKey = 2;
        break;
      case 7:
      case 8:
        tmpKey = 5;
        break;
      case 9:
        tmpCmd = "c";
        tmpKey = 8;
      }
      sb.append("GLOBAL I_KEYWORD " + tmpKey + sep);
      sb.append("GLOBAL I_BAS_1 " + (tmpBas.getSelectedIndex() + 1) + sep);
      key = get.split(" ")[0];
      sb.append("CMD " + tmpCmd);
    } else {
      sb.append("GLOBAL I_KEYWORD " + nboKeywordNumber + sep);
      sb.append("GLOBAL I_BAS_1 " + (comboBasis.getSelectedIndex() + 1) + sep);
      key = get.split(" ")[0];
      sb.append("CMD " + key);
    }
    if (get.equals("c") && nboKeywordNumber == KEYWD_CMO)
      mode = NBOService.MODE_SEARCH_LIST_MO;
    postNBO_s(sb, mode, list, "Getting list " + key);
  }

  protected void showMessage() {
    JOptionPane.showMessageDialog(this,
        "Error getting lists, an error may have occured during run");
  }

  protected void basChange() {
    String b1 = comboBasis.getSelectedItem().toString();
    String b2 = bas2.getSelectedItem().toString();
    String[] b1b2 = { "Current r(ow),c(olumn) matrix element:",
        "  (1) current <" + b1 + "(r)|" + b2 + "(c)> value",
        "Extremal off-diagonal values for current r orbital:",
        "  (2) max <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "  (3) min <" + b1 + "(r)|" + b2 + "(*c)> value for current r",
        "Extremal off-diagonal values for current c orbital:",
        "  (4) max <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "  (5) min <" + b1 + "(*r)|" + b2 + "(c)> value for current c",
        "Extremal off-diagonal values for any (*r,*c) orbitals:",
        "  (6) max <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c",
        "  (7) min <" + b1 + "(*r)|" + b2 + "(*c)> value for any *r,*c" };
    changeKey(b1b2);
  }

  protected String[] opBas(String operator) {
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
    nboKeywordNumber = index;
    switch (index) {
    case KEYWD_NPA:
      load(31, false);
      at2 = null;
      comboBasis.setSelectedIndex(BASIS_PNAO); // WAS BASIS_NAO ? 
      keyProp = "NPA";
      setKeyword(new String[] { "b", "a", "o PNAO", "u" }, new String[] {
          "Basis: ", "Atom: ", "Orbital: ", "Unit: " });
      changeKey(npa);
      break;
    case KEYWD_NBO:
      load(36, true);
      comboBasis.setSelectedIndex(BASIS_PNBO);
      keyProp = "NBO";
      setKeyword(new String[] { "b", "o PNBO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nbo);
      break;
    case KEYWD_NLMO:
      load(38, true);
      comboBasis.setSelectedIndex(BASIS_PNLMO);
      keyProp = "NLMO";
      setKeyword(new String[] { "b", "o PNLMO" }, new String[] { "Basis: ",
          "Orbital: " });
      changeKey(nlmo);
      break;
    case KEYWD_BEND:
      load(34, true);
      comboBasis.setSelectedIndex(BASIS_PNHO);
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
      comboBasis.setSelectedIndex(BASIS_PNBO);
      keyProp = "E2";
      setKeyword(new String[] { "b", "d nbo", "a nbo", "u" }, new String[] {
          "Basis: ", "d-NBO: ", "a-NBO:", "Unit: " });
      changeKey(e2);
      break;
    case KEYWD_STERIC:
      load(38, true);
      comboBasis.setSelectedIndex(BASIS_PNLMO);
      keyProp = "STERIC";
      setKeyword(new String[] { "b", "d nlmo", "d' nlmo", "u" }, new String[] {
          "Basis: ", "d-NLMO: ", "d'-NLMO:", "Unit: " });
      changeKey(steric);
      break;
    case KEYWD_CMO:
      load(40, true);
      comboBasis.setSelectedIndex(BASIS_MO);
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
      comboBasis.setSelectedIndex(BASIS_PNLMO);
      keyProp = "DIPOLE";
      setKeyword(new String[] { "b", "o", "u" }, new String[] { "Basis: ",
          "Orbital: ", "Unit:" });
      changeKey(dip);
      break;
    case KEYWD_OPBAS:
      load(31, true);
      viewSettingsBox.removeAll();
      comboBasis = new JComboBox<String>(NBODialogView.basSet);
      comboBasis.setUI(new StyledComboBoxUI(180, -1));
      comboBasis.setEditable(false);
      opBas.requestFocus();
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
      basChange();
      break;
    }
    this.repaint();
    this.revalidate();

    if (index == KEYWD_OPBAS)
      opBas.showPopup();
  }

  private void load(int nn, boolean withBondPicking) {
    loadModelFileNow(inputFileHandler.newNBOFileForExt("" + nn) + (withBondPicking ? ";set bondpicking true" : ""));
  }

  protected void setKeyword(final String[] get, final String[] labs) {
    viewSettingsBox.removeAll();
    viewSettingsBox.setLayout(new BorderLayout());
    JPanel l = new JPanel(new GridLayout(labs.length, 1));
    JPanel l2 = new JPanel(new GridLayout(labs.length, 1));
    for (int i = 0; i < labs.length; i++) {
      final String key = get[i].split(" ")[0];
      l.add(new JLabel(labs[i]));
      if (key.equals("b") || key.equals("s")) {
        Box b = Box.createHorizontalBox();

        if (nboKeywordNumber == KEYWD_CMO) {
          b.add(moRb);
          b.add(nboRb);
        } else if (key.equals("b")) {
          String str = peeify(comboBasis.getSelectedItem().toString());
          b.add(new JLabel(str));
          //runScriptQueued("NBO TYPE " + str + ";MO TYPE " + str);
          b.add(Box.createRigidArea(new Dimension(20, 0)));
        }
        b.add(alphaSpin);
        b.add(betaSpin);
        //}
        l2.add(b);
      } else if (PT.isOneOf(get[i], "o PNBO;o PNLMO;r;d nlmo;n;d nbo;o PNHO;o")) {
        list1 = new DefaultComboBoxModel<String>();
        orb = new JComboBox<String>(list1);

        l2.add(orb);
        getListSearch(key, list1);
        orb.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (orb.getSelectedIndex() < 0)
              return;
            orbPick();
            if (key.equals("n")) {
              showOrbJmol("NBO", orb.getSelectedIndex(), key);
              nboRb.doClick();
            } else
              showOrbJmol(comboBasis.getSelectedItem().toString(),
                  orb.getSelectedIndex(), key);
          }
        });
      } else if (PT.isOneOf(get[i], "c;d' nlmo;a nbo;c cmo;o PNAO")) {
        list2 = new DefaultComboBoxModel<String>();
        orb2 = new JComboBox<String>(list2);
        getListSearch(key, list2);
        final int ind = i;
        orb2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (orb2.getSelectedIndex() < 0)
              return;
            orbPick();
            if (key.equals("a")) {
              showMOJmol("NBO", list1.getSize() + orb2.getSelectedIndex(), key);
            } else if (key.equals("c")) {
              showOrbJmol(comboBasis.getSelectedItem().toString(),
                  orb2.getSelectedIndex(), key);
            } else
              showMOJmol(comboBasis.getSelectedItem().toString(),
                  orb2.getSelectedIndex(), key);
            if (get[ind].equals("c cmo")) {
              moRb.doClick();
            }
          }
        });
        l2.add(orb2);
      } else if (key.equals("u")) {
        list3 = new DefaultComboBoxModel<String>();
        unit = new JComboBox<String>(list3);
        getListSearch(key, list3);
        unitLabel = new JLabel();
        Box box = Box.createHorizontalBox();
        box.add(unit);
        box.add(unitLabel);
        l2.add(box);
        unitLabel.setVisible(false);
      } else if (key.equals("rs")) {
        list3 = new DefaultComboBoxModel<String>();
        unit = new JComboBox<String>(list3);

        getListSearch("r", list3);
        l2.add(unit);
      } else if (key.equals("a")) {
        list1 = new DefaultComboBoxModel<String>();
        at1 = new JComboBox<String>(list1);
        getListSearch("a", list1);
        at1.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (at2 == null)
              runScriptQueued("select on; select {*}["
                  + (at1.getSelectedIndex() + 1) + "]");
            else
              runScriptQueued("select on; select remove{*}; "
                  + "select add {*}[" + (at1.getSelectedIndex() + 1)
                  + "]; select add {*}[" + (at2.getSelectedIndex() + 1) + "]");
          }
        });
        l2.add(at1);
      } else if (key.equals("a'")) {
        list2 = new DefaultComboBoxModel<String>();
        at2 = new JComboBox<String>(list2);
        getListSearch("a", list2);
        at2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            runScriptQueued("select on; select remove{*}; " + "select add {*}["
                + (at1.getSelectedIndex() + 1) + "]; select add {*}["
                + (at2.getSelectedIndex() + 1) + "]");
          }
        });
        l2.add(at2);
      } else if (key.equals("b1")) {
        Box b = Box.createHorizontalBox();
        b.add(comboBasis);
        comboBasis.setSelectedIndex(BASIS_AO);
        comboBasis.addActionListener(new ActionListener() {
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
        l2.add(b);
      } else if (key.equals("b2")) {
        bas2 = new JComboBox<String>(NBODialogView.basSet);

        bas2.setUI(new StyledComboBoxUI(180, -1));
        bas2.setSelectedIndex(1);
        bas2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            basChange();
            runScriptQueued("MO TYPE "
                + comboBasis.getSelectedItem().toString());

            list2.removeAllElements();
            getListSearch("c", list2);
          }
        });
        //bas2.setSelectedIndex(1);
        l2.add(bas2);
      } else if (key.equals("b12")) {
        comboBasis = new JComboBox<String>(NBODialogView.basSet);
        comboBasis.setUI(new StyledComboBoxUI(180, -1));
        l2.add(comboBasis);
      }
    }
    logCmd(keyProp + " Search Results:");
    JLabel lab = new JLabel("Settings");
    lab.setFont(nboFont);

    lab.setOpaque(true);
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    viewSettingsBox.add(lab, BorderLayout.NORTH);
    viewSettingsBox.add(l, BorderLayout.WEST);
    viewSettingsBox.add(l2, BorderLayout.CENTER);
  }

  protected void doSetBasis() {
    if (keyProp.equals("B1B2")) {
      if (bas2 == null)
        return;
      basChange();
      runScriptQueued("NBO TYPE "
          + comboBasis.getSelectedItem().toString());
      list1.removeAllElements();
      getListSearch("r", list1);
    } else if (keyProp.equals("OPBAS")) {
      loadModelFileNow("" + inputFileHandler.newNBOFileForExt(""
              + (31 + comboBasis.getSelectedIndex())));
      list1.removeAllElements();
      list2.removeAllElements();
      runScriptQueued("NBO TYPE "
          + comboBasis.getSelectedItem().toString());
      getListSearch("r", list1);
      getListSearch("c", list2);
    } else {
      runScriptQueued("NBO TYPE "
          + comboBasis.getSelectedItem().toString());
      runScriptQueued("MO TYPE "
          + comboBasis.getSelectedItem().toString());
      opBas(opBas.getSelectedItem().toString().trim().split(" ")[0]);
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
    return (!str.equals("MO") && !str.equals("AO") && str.charAt(0) != 'P' ? "P" + str : str);
  }

  protected void getSearchValue(int op) {
    if (relabel) {
      runScriptQueued("select add {*}.bonds; color bonds lightgrey; select none");
      showAtomNums(alphaSpin.isSelected());
      for (int i = 0; i < nBonds; i++) {
        runScriptQueued("MEASUREMENT ID '" + i + "' off ");
      }
      relabel = false;
    }
    final SB sb = getMetaHeader(false);
    sb.append("GLOBAL I_KEYWORD " + nboKeywordNumber + sep);
    boolean isLabel = false;
    boolean isLabelBonds = false;
    switch (nboKeywordNumber) {
    case KEYWD_NPA:
      sb.append("GLOBAL I_ATOM_1 " + (at1.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_UNIT_1 " + (unit.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_ORB_1 " + (orb2.getSelectedIndex() + 1) + sep);
      if (op > 10) {
        isLabel = true;
        op = 12;
      }
      break;
    case KEYWD_NBO:
    case KEYWD_BEND:
    case KEYWD_NLMO:
      sb.append("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_E2PERT:
      sb.append("GLOBAL I_d_NBO_1 " + (orb.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_a_NBO "
          + (orb2.getSelectedIndex() + orb.getModel().getSize() + 1) + sep);
      sb.append("GLOBAL I_UNIT_1 " + (unit.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_NRT:
      sb.append("GLOBAL I_ATOM_1 " + (at1.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_ATOM_2 " + (at2.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_RES_STR " + (unit.getSelectedIndex() + 1) + sep);
      isLabel = (op == 9);
      isLabelBonds = (op == 10);
      break;
    case KEYWD_STERIC:
      sb.append("GLOBAL I_d_NBO_1 " + (orb.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_d_NBO_2 " + (orb2.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_UNIT_1 " + (unit.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_CMO:
      sb.append("GLOBAL I_CMO " + (orb2.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_NBO " + (orb.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_DIPOLE:
      sb.append("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_UNIT_1 " + (unit.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_OPBAS:
      sb.append("GLOBAL I_BAS_1 " + (comboBasis.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_OPERATOR " + (operator + 1) + sep);
      sb.append("GLOBAL I_ROW " + (orb.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_COLUMN " + (orb2.getSelectedIndex() + 1) + sep);
      break;
    case KEYWD_BAS1BAS2:
      sb.append("GLOBAL I_BAS_1 " + (comboBasis.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_BAS_2 " + (bas2.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_ROW " + (orb.getSelectedIndex() + 1) + sep);
      sb.append("GLOBAL I_COLUMN " + (orb2.getSelectedIndex() + 1) + sep);
      break;
    }

    sb.append("GLOBAL I_OPT_" + keyProp + " " + op);
    if (isLabel) {
      relabel = true;
      postNBO_s(sb, NBOService.MODE_SEARCH_LABEL, null, "Getting labels");
    } else if (isLabelBonds) {
      relabel = true;
      nBonds = 0;
      //runScriptQueued("select {*};label off;select remove {*}");
      runScriptQueued("select add {*}.bonds; color bonds [170,170,170]; select remove {*}");
      postNBO_s(sb, NBOService.MODE_SEARCH_LABEL_BONDS, null, "Getting bonds list");
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
   * @param list
   */
  @SuppressWarnings("fallthrough")
  protected void setSearchList(AbstractListModel<String> list) {
    if (list == null)
      return;

    switch (nboKeywordNumber) {
    case KEYWD_E2PERT:
      if (list.equals(list2)) {
        //Relabel a-nbo to correct orbital number
        int offset = list1.getSize() + 1;
        int sz = list2.getSize();
        ActionListener l = orb2.getActionListeners()[0];
        orb2.removeActionListener(l);
        for (int i = 0; i < sz; i++) {
          String s = list2.getElementAt(i);
          list2.removeElementAt(i);
          s = "   " + (offset + i) + s.substring(s.indexOf("."));
          list2.insertElementAt(s, i);
        }
        orb2.addActionListener(l);
      }
    case KEYWD_NPA:
    case KEYWD_STERIC:
    case KEYWD_DIPOLE:
      if (list.equals(list3))
        if (list3.getSize() == 1) {
          unit.setVisible(false);
          unitLabel.setVisible(true);
          unitLabel.setText(list3.getElementAt(0).substring(6));
        }
      break;
    case KEYWD_NRT:
      if (list.equals(list3)) {
        changeKey(nrt);
        //Parsing RS list here ensures RS list will be in .nbo file
        parseRsList(getRSList());
        unit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (isOpenShell)
              setResStruct(unit.getSelectedIndex() + 1, alphaSpin.isSelected());
            else
              setResStruct(unit.getSelectedIndex() + 1, true);
          }
        });
        setResStruct(1, alphaSpin.isSelected());
      }
      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      if (list.equals(list2)) {
        orb.removeActionListener(orb.getActionListeners()[0]);
        orb.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            showOrbJmol(comboBasis.getSelectedItem().toString(),
                orb.getSelectedIndex(), "b1");
          }
        });
        orb2.removeActionListener(orb2.getActionListeners()[0]);
        orb2.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            doShowOrbitals();
          }
        });
        orb.setSelectedIndex(0);
        orb2.setSelectedIndex(0);
      }
    }
  }

  protected void doShowOrbitals() {
    if (nboKeywordNumber == KEYWD_OPBAS)
      showMOJmol(comboBasis.getSelectedItem().toString(),
          orb2.getSelectedIndex(), "b2");
    else
      showMOJmol(bas2.getSelectedItem().toString(),
          orb2.getSelectedIndex(), "b2");
  }

  /**
   * Takes two strings from .nbo file and parses RS list information toks[0] =
   * primary rs matrix toks[1] = change list using accountants notation
   * 
   * @param toks
   */
  private void parseRsList(String[] toks) {
    String[] tmp1 = toks[0].split("\n");
    int size = tmp1.length;
    resStructDef = new int[size][size];
    for (int i = 0; i < size; i++) {
      String[] tmp = tmp1[i].substring(10).trim().split("\\s+");
      for (int j = 0; j < tmp.length; j++) {
        if (tmp[j].length() > 0)
          resStructDef[i][j] = Integer.parseInt(tmp[j]);
      }
    }
    resStructList = new Hashtable<Integer, String>();
    try {
      BufferedReader br = new BufferedReader(new StringReader(toks[1]));
      String line = br.readLine();
      line = br.readLine();
      int num = 1;
      String list = "";
      while ((line = br.readLine()) != null) {
        String n = line.substring(0, 10).trim();
        if (n.equals("")) {
          list += line.substring(18).trim();
        } else if (n.contains("-")) {
          break;
        } else {
          if (!list.equals("")) {
            Integer rs = new Integer(++num);
            resStructList.put(rs, list);
          }
          list = "";
          list += line.substring(18).trim();
        }
      }
      Integer rs = new Integer(++num);
      resStructList.put(rs, list);
    } catch (Exception e) {
      //vwr.alert(e.getMessage());
      return;
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
    int sz = resStructDef.length;
    chooseList.lonePairs = new Hashtable<String, String>();
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
            chooseList.lonePairs.put(new Integer(i + 1).toString(),
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
    if (chooseList != null) {
      Hashtable<String, String> lonePairs = (alpha) ? chooseList.lonePairs
          : chooseList.lonePairs_b;
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
          sb.append("<sup>" + ((charge > 0) ? "+" : "") + charge + "</sup>;");
        else
          sb.append(";");
      }
      runScriptQueued(sb.toString());
    }
    runScriptQueued("select remove{*}; " + "select add (atomno="
        + (at1.getSelectedIndex() + 1) + ");" + "select add (atomno="
        + (at2.getSelectedIndex() + 1) + ");");

  }

  protected void showMOJmol(String type, int i, String key) {
    showOrbJmol(type, i, key);
  }
  
  protected void showOrbJmol(String type, int i, String id) {
    id = fixID(id);
    runScriptQueued(
        "select visible;isosurface ID \"" + id + "\" delete;" +  
        getJmolIsosurfaceScript(id, peeify(type), i,
        betaSpin.isSelected(), false));
  }

  private String fixID(String id) {
    return PT.replaceAllCharacters(id, "'\"", "_");
  }

  protected void orbPick() {
    if (rbSelection < 0)
      return;
    switch (nboKeywordNumber) {
    case KEYWD_NPA:
      if (rbSelection > 4 && rbSelection < 10)
        rBtns[rbSelection].doClick();
      break;
    case KEYWD_NBO:
      if (rbSelection < 4)
        rBtns[rbSelection].doClick();
      break;
    case KEYWD_BEND:
      if (rbSelection < 6)
        rBtns[rbSelection].doClick();
      break;
    case KEYWD_E2PERT:
      if (rbSelection < 3)
        rBtns[rbSelection].doClick();
      break;
    case KEYWD_NLMO:
      rBtns[rbSelection].doClick();
      break;
    case KEYWD_STERIC:
      if (rbSelection > 1 && rbSelection < 4)
        rBtns[rbSelection].doClick();
      break;
    case KEYWD_CMO:
      rBtns[rbSelection].doClick();
      break;
    case KEYWD_DIPOLE:
      if (rbSelection > 2 && rbSelection < 6)
        rBtns[rbSelection].doClick();
      break;

    }
  }


  protected String[] getRSList() {
    String data = inputFileHandler.getInputFile("nbo");
    String[] toks = PT.split(data,
        "TOPO matrix for the leading resonance structure:\n");
    if (toks.length < 2) {
      if (toks[0].contains("0 candidate reference structure(s)"))
        alertError("0 candidate reference structure(s) calculated by SR LEWIS"
            + "Candidate reference structure taken from NBO search");
      return null;
    }
    String[] toks2 = PT
        .split(toks[1],
            "---------------------------------------------------------------------------");
    String[] rsList = new String[2];
    rsList[0] = toks2[0].substring(toks2[0].lastIndexOf('-'),
        toks2[0].indexOf("Res")).trim();
    rsList[0] = rsList[0].replace("-\n", "");
    rsList[1] = toks2[1];
    return rsList;
  }

  protected void notifyPick_s(String atomno) {
    runScriptNow("isosurface delete");
    String[] tok = atomno.split(",");
    if (tok.length < 2) {
      int n = PT.parseInt(atomno);
      if (n == Integer.MIN_VALUE) {
        showOrbital(findNextAtomicOrbital(atomno, list1));
      } else {
        switch (nboKeywordNumber) {
        case KEYWD_NBO:
        case KEYWD_BEND:
        case KEYWD_NLMO:
        case KEYWD_E2PERT:
          showOrbital(findNextAtomicOrbital(atomno, list1));
          return;
        case KEYWD_CMO:
          showOrbital(findNextAtomicOrbital(atomno, list2));
          return;
        }
        int atomIndex = n - 1;
        if (at1 != null && at2 == null) {
          at1.setSelectedIndex(atomIndex);
          if (rbSelection < 3 && rbSelection > -1)
            rBtns[rbSelection].doClick();
        } else if (at1 != null && at2 != null) {
          if (secondPick)
            at1.setSelectedIndex(atomIndex);
          else
            at2.setSelectedIndex(atomIndex);
          secondPick = !secondPick;
        }
      }
      return;
    }
    String[] tok2 = tok[1].split(" ");
    switch (nboKeywordNumber) {
    case KEYWD_NBO:
    case KEYWD_NLMO:
    case KEYWD_DIPOLE:
    case KEYWD_CMO:
      orb.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list1));
      break;
    case KEYWD_BEND:
      orb.setSelectedIndex(pickNHO_s(tok2[2], tok2[5], list1));
      break;
    case KEYWD_NRT:
      tok = atomno.split(",");
      int a1 = Integer.parseInt(tok2[2].replaceAll("[\\D]", ""));
      int a2 = Integer.parseInt(tok2[5].replaceAll("[\\D]", ""));
      at1.setSelectedIndex(a1 - 1);
      at2.setSelectedIndex(a2 - 1);
      if (rbSelection > 2 && rbSelection < 6)
        rBtns[rbSelection].doClick();
      return;
    case KEYWD_E2PERT:
    case KEYWD_STERIC:
      tok = atomno.split(",");
      tok2 = tok[1].split(" ");
      String bond = tok2[2] + "-" + tok2[5];
      String str = orb.getSelectedItem().toString().replace(" ", "");
      if (str.contains(bond)) {
        orb.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list1));
        return;
      }
      str = orb2.getSelectedItem().toString().replace(" ", "");
      if (str.contains(bond)) {
        orb2.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list2));
        return;
      }
      if (!secondPick)
        orb2.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list2));
      else
        orb.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list1));
      secondPick = !secondPick;

      break;
    case KEYWD_OPBAS:
    case KEYWD_BAS1BAS2:
      JComboBox<String> tmpBas = comboBasis;
      if (nboKeywordNumber == KEYWD_BAS1BAS2) {
        if (!secondPick)
          tmpBas = bas2;
      }
      switch (tmpBas.getSelectedIndex()) {
      case 0:
      case 1:
      case 2:
      case 9:
      default:
        secondPick = !secondPick;
        break;
      case 3:
      case 4:
        if (!secondPick)
          orb2.setSelectedIndex(pickNHO_s(tok2[2], tok2[5], list2));
        else
          orb.setSelectedIndex(pickNHO_s(tok2[2], tok2[5], list1));
        secondPick = !secondPick;
        break;
      case 5:
      case 6:
      case 7:
      case 8:
        tok = atomno.split(",");
        tok2 = tok[1].split(" ");
        bond = tok2[2] + "-" + tok2[5];
        str = orb.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          orb.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list1));
          return;
        }
        str = orb2.getSelectedItem().toString().replace(" ", "");
        if (str.contains(bond)) {
          orb2.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list2));
          return;
        }
        if (!secondPick)
          orb2.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list2));
        else
          orb.setSelectedIndex(pickNBO_s(tok2[2], tok2[5], list1));
        secondPick = !secondPick;
        break;
      }
    }
  }

  private int pickNBO_s(String at1, String at2,
                        DefaultComboBoxModel<String> list) {
    String bond = at1 + "-" + at2;
    int curr = 0, size = list.getSize();
    if (currOrb.contains(bond))
      curr = list.getIndexOf(list.getSelectedItem());
    for (int i = curr + 1; i < size + curr; i++) {
      String str = list.getElementAt(i % size).replace(" ", "");
      if (str.contains(bond)) {
        list.setSelectedItem(list.getElementAt(i % size));
        currOrb = str;
        return i % size;
      }
    }
    return curr;
  }

  protected int pickNHO_s(String at1, String at2,
                          DefaultComboBoxModel<String> list) {
    String bond = at1 + "(" + at2 + ")";
    String bond2 = at2 + "(" + at1 + ")";
    int curr = 0, size = list.getSize();
    if (currOrb.contains(bond))
      curr = list.getIndexOf(list.getSelectedItem());
    for (int i = curr + 1; i < size + curr; i++) {
      String str = list.getElementAt(i % size).replace(" ", "");
      if (str.contains(bond)) {
        list.setSelectedItem(list.getElementAt(i % size));
        currOrb = str;
        return i % size;
      } else if (str.contains(bond2)) {
        list.setSelectedItem(list.getElementAt(i % size));
        currOrb = str;
        return i % size;
      }
    }
    return curr;
  }

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
    if (nboKeywordNumber > 0 && isNewModel) {
      keywordClicked(nboKeywordNumber);
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

  /**
   * Post a request to NBOServe with a callback to processNBO_s.
   * 
   * @param sb
   *        command data
   * @param mode
   *        type of request
   * @param list
   *        optional list to fill
   * @param statusMessage
   */
  private void postNBO_s(SB sb, final int mode,
                         final DefaultComboBoxModel<String> list,
                         String statusMessage) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO_s(req, mode, list);
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
   * @param list
   */
  protected void processNBO_s(NBORequest req, int mode,
                              DefaultComboBoxModel<String> list) {
    String[] lines = req.getReplyLines();
    String line;
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
      for (int i = 0; i < lines.length; i++) {
        list.addElement(lines[i]);
      }
      setSearchList(list);
      break;
    case NBOService.MODE_SEARCH_LIST_MO:
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
        if (isLabel ? !processLabel(sb, lines[i], pt) : !processLabelBonds(sb, lines[i]))
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
        
        sb.append("font measures 20; measure id 'm" + toks[0] + "_" + toks[1] + "' @" + toks[0] + " @" + toks[1] + " radius 0.02 \"" + toks[2] + "\";");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }


}
