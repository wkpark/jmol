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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Stack;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.util.Elements;

abstract class NBODialogModel extends NBODialogConfig {

  protected NBODialogModel(JFrame f) {
    super(f);
  }

  private final static int MODEL_ACTION_ALTER  = 0;
  private final static int MODEL_ACTION_CLIP   = 1;
  private final static int MODEL_ACTION_FUSE   = 2;
  private final static int MODEL_ACTION_LINK   = 3;
  private final static int MODEL_ACTION_MUTATE = 4;
  private final static int MODEL_ACTION_SWITCH = 5;
  private final static int MODEL_ACTION_TWIST  = 6;
  private final static int MODEL_ACTION_VALUE  = 7;
  private final static int MODEL_ACTION_3CHB   = 8;
  
  private static final int MODEL_ACTION_MAX    = 9;

  private static final int MODEL_ACTION_REBOND = 9;
  private static final int MODEL_ACTION_SYMMETRY = 10;

  
  final static String[] MODEL_ACTIONS = { 
      "Alter", "Clip", "Fuse", "Link", "Mutate",
      "Switch", "Twist", "Value", "3chb", "Rebond", 
      "Symmetry?" };
  
  private static final String[] EDIT_INFO = {
      "Edit nuclear charge, bond length, bond angle, or dihedral angle",
      "Remove bond between two atoms",
      "Delete monovalent atoms and replace with bond",
      "Add bond between two atoms",
      "Replace atom with a new substituent-group",
      "Switch location of two groups",
      "Perform rigid torsional twist about dihedral angle",
      "Value of nuclear charge, bond length, bond angle, and dihedral angle",
      "Create 3-center linkage between two atoms and a ligand",
      "Change bonding symmetry around transition metal",
      "Display point-group symmetry of current model"
      };

  private final static int BOX_COUNT_4 = 4, BOX_COUNT_2 = 2, BOX_COUNT_1 = 1;
  private final static int MAX_HISTORY = 5;

  ///  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";

  private NBOFileHandler saveFileHandler;

  private Box innerEditBox;
  private JTextField jtNIHInput, jtLineFormula;
  private JTextField currVal;
  private JComboBox<String> jcSymOps;
  private JButton rebond, jbClear;
  private JLabel atomsLabel;
  private Box editComponent;
  private Box inputHeader;
  private Box saveHeader;
  private Component inputComponent;
  private Box saveComponent;
  private Box editHeader;  
  private JTextField[] atomNumBoxes;
  private JLabel valueLabel = new JLabel("");
  
  // only used by private listeners 
  
  protected JTextField editValueTf;
  protected JButton jbApply;
  protected JComboBox<String> jComboSave;
  protected JButton undo, redo;
  protected Stack<String> undoStack, redoStack;

  // used by other modules
  
  JPanel panel;


  /**
   * identifies which action button as pressed -- for example, MODEL_ACTION_ALTER
   */
  private int actionID;

  /**
   * encodes number of atoms that can be selected
   */  
  private int boxCount;
  
 /**
   * A model is being loaded into Jmol that NBO does not know about yet
   */
  private boolean notFromNBO;

  /**
   * A flag to indicate that when the next atom is clicked, the selection should be cleared.
   * Set to true each time a non-value option action is processed.
   */
  private boolean resetOnAtomClick;

  protected void setModelNotFromNBO() {
    notFromNBO = true;
  }

  private void showComponents(boolean tf) {
    editHeader.setVisible(tf);
    editComponent.setVisible(tf);
    saveHeader.setVisible(tf);
    saveComponent.setVisible(tf);
  }

  void modelSetSaveParametersFromInput(NBOFileHandler nboFileHandler,
                                       String dir, String name, String ext) {
    if (saveFileHandler != null && nboFileHandler != saveFileHandler)
      saveFileHandler.setInput(dir, name,
          PT.isOneOf(ext, NBODialogConfig.OUTPUT_FILE_EXTENSIONS) ? ext : "");
  }


  protected JPanel buildModelPanel() {
    resetVariables_m();
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    inputHeader = createTitleBox(" Input Model ", new HelpBtn(
        "model_input_intro_help.htm"));
    panel.add(inputHeader);
    inputComponent = getInputComponent();
    panel.add(inputComponent);
    
    editHeader = getEditHeader();
    panel.add(editHeader).setVisible(false);
    editComponent = getEditComponent();
    panel.add(editComponent).setVisible(false);
    
    saveHeader = createTitleBox(" Save Model ", new HelpBtn(
        "model_save_intro_help.htm"));
    panel.add(saveHeader).setVisible(false);
    saveComponent = getSaveComponent();
    panel.add(saveComponent).setVisible(false);
    panel.add(Box.createGlue());
    
    if (vwr.ms.ac > 0) {
      loadModelToNBO(null, false);
    }
    return panel;

  }

  private void resetVariables_m() {
    actionID = 0;
    boxCount = 0;
    notFromNBO = false;
  }

  private Box getEditHeader() {
    Box topBox = Box.createHorizontalBox();
    undo = new JButton("<HTML>&#8592Undo</HTML>");
    redo = new JButton("<HTML>Redo&#8594</HTML>");
    undoStack = new Stack<String>();
    redoStack = new Stack<String>();
    redo.addActionListener(redoAction);
    undo.addActionListener(undoAction);
    topBox.add(undo);
    topBox.add(redo);
    topBox.add(new HelpBtn("model_edit_intro_help.htm"));
    return createTitleBox(" Edit Model ", topBox);
  }

  /**
   * adds use elements to main panel
   * 
   * @return use elements
   */
  private Component getInputComponent() {

    Box inputBox = createBorderBox(true);
    inputBox.setMaximumSize(new Dimension(360, 140));
    inputBox.setPreferredSize(new Dimension(360, 140));
    inputBox.setMinimumSize(new Dimension(360, 140));
    JPanel p2 = new JPanel(new GridLayout(3, 2));
    p2.setMaximumSize(new Dimension(360, 90));
    p2.setPreferredSize(new Dimension(360, 90));
    p2.setMinimumSize(new Dimension(360, 90));

    final JRadioButton jrJmolIn = new JRadioButton("NIH/PubChem/PDB");
    jrJmolIn.setFont(monoFont);
    final JRadioButton jrLineIn = new JRadioButton("Line Formula");
    jrLineIn.setFont(monoFont);
    jrLineIn.setSelected(true);
    final JRadioButton jrFileIn = new JRadioButton("File Input");
    jrFileIn.setFont(monoFont);
    ButtonGroup rg = new ButtonGroup();
    rg.add(jrJmolIn);
    rg.add(jrLineIn);
    rg.add(jrFileIn);
    createInput(jtNIHInput = new JTextField(), jrJmolIn);
    createInput(jtLineFormula = new JTextField(), jrLineIn);
    jtNIHInput.setFont(userInputFont);
    jtLineFormula.setFont(userInputFont);
    jtLineFormula.add(new JLabel("line formula"));
    String[] useOps = { "<Select File  Type>", "[.xyz]  XYZ", "[.mol]  MOL",
        "[.cfi]  NBO Cartesian", "[.vfi]  NBO Valence", "[.47]   NBO Archive",
        "[.gau]  Gaussian Input", "[.log]  Gaussian Output",
        "[.gms]  GAMESS Input", "[.adf]  ADF Input", "[.jag]  Jaguar Input",
        "[.mm2]  MM2-Input", "[.mnd]  Dewar Type Input",
        "[.mp]   Molpro Input", "[.nw]   NWChem Input", "[.orc]  Orca Input",
        "[.pqs]  PQS Input", "[.qc]   Q-Chem Input" };
    final JComboBox<String> jComboUse = new JComboBox<String>(useOps);
    jComboUse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doComboUseAction(jComboUse.getSelectedIndex() > 0 ? jComboUse.getSelectedItem().toString() : null);
      }
    });
    p2.add(jrLineIn);
    p2.add(jtLineFormula);
    p2.add(jrJmolIn);
    p2.add(jtNIHInput);
    p2.add(jrFileIn);
    p2.add(jComboUse);
    addFocusListeners(jComboUse, jrFileIn);

    inputBox.add(p2);
    inputFileHandler = new NBOFileHandler("", "", NBOFileHandler.MODE_MODEL_USE, INPUT_FILE_EXTENSIONS, (NBODialog) this) {

      @Override
      protected boolean doFileBrowsePressed() {
        String folder = tfDir.getText().trim();
        String name = tfName.getText();
        String ext = tfExt.getText();
        if (!folder.equals("")) {
          if (!folder.contains(":"))
            folder = "C:/" + folder;
          folder = folder + "/" + (name.equals("") ? "new" : name + "." + ext);
        }
        JFileChooser myChooser = new JFileChooser();
        if (useExt.contains(";"))
          myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt
              .split(";")));
        else
          myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
        myChooser.setFileHidingEnabled(true);
        if (!folder.equals(""))
          myChooser.setSelectedFile(new File(folder));
        int button = myChooser.showDialog(this, GT._("Select"));
        if (button == JFileChooser.APPROVE_OPTION) {
          File newFile = myChooser.getSelectedFile();
          if (newFile.toString().indexOf(".") < 0) {
            logError("File not found");
            return false;
          }
          loadModelFromNBO(newFile.getParent(),
              (jobStem = getJobStem(newFile)), NBOFileHandler.getExt(newFile));

          inputFileHandler
              .setInput(fileDir, jobStem, NBOFileHandler.getExt(newFile));
          fileDir = newFile.getParent();

          return true;
        }
        return false;
      }
    };
    // BH adding focus for these as well
    addFocusListeners(inputFileHandler.tfDir, jrFileIn);
    addFocusListeners(inputFileHandler.tfExt, jrFileIn);
    addFocusListeners(inputFileHandler.tfName, jrFileIn);
    inputBox.add(inputFileHandler);
    inputBox.add(Box.createGlue());
    return inputBox;
  }

  protected void doComboUseAction(String item) {
    if (item == null) {
      inputFileHandler.tfExt.setText("");
      inputFileHandler.useExt = INPUT_FILE_EXTENSIONS;
    } else {
      item = item.substring(item.indexOf("[") + 2, item.indexOf("]"));
      inputFileHandler.tfExt.setText(item);
      inputFileHandler.useExt = item;
    }
  }

  private Box getEditComponent() {
    Box editBox = createBorderBox(false);
    Box actionBox = Box.createVerticalBox();
    
    final JRadioButton[] jrModelActions = new JRadioButton[MODEL_ACTION_MAX];
    ButtonGroup rg = new ButtonGroup();
    for (int i = 0; i < MODEL_ACTION_MAX; i++) {
      jrModelActions[i] = new JRadioButton(MODEL_ACTIONS[i]);
      jrModelActions[i].setToolTipText(EDIT_INFO[i]);
      actionBox.add(jrModelActions[i]);
      rg.add(jrModelActions[i]);
      final int op = i;
      jrModelActions[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doModelAction(op);
        }
      });
    }

    editBox.add(actionBox);
    Box rightBox = Box.createVerticalBox();
    createInnerEditBox();
    rightBox.add(this.innerEditBox);
    Box lowBox = Box.createHorizontalBox();
    JButton sym = new JButton(MODEL_ACTIONS[MODEL_ACTION_SYMMETRY]);
    sym.setToolTipText(EDIT_INFO[MODEL_ACTION_SYMMETRY]);
    sym.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doGetSymmetry();
      }
    });
    lowBox.add(sym);
    rebond = new JButton(MODEL_ACTIONS[MODEL_ACTION_REBOND]);
    rebond.setEnabled(false);
    rebond.setToolTipText(EDIT_INFO[MODEL_ACTION_REBOND]);

    rebond.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doModelAction(MODEL_ACTION_REBOND);
      }
    });
    lowBox.add(rebond);
    rightBox.add(lowBox);
    editBox.add(rightBox);

    //btns[0].doClick();
    return editBox;
  }

  private void createInnerEditBox() {

    innerEditBox = Box.createVerticalBox();
    innerEditBox.setBorder(BorderFactory.createLoweredBevelBorder());
    innerEditBox.setMaximumSize(new Dimension(275, 200));
    innerEditBox.setAlignmentX(0.5f);
    innerEditBox.setVisible(false);
    Box atBox = Box.createHorizontalBox();
    atBox.add(atomsLabel = new JLabel("")); // "Atoms:"
    atomNumBoxes = new JTextField[4];
    for (int i = 0; i < 4; i++) {
      atomNumBoxes[i] = new JTextField();
      atomNumBoxes[i].setFont(userInputFont);
      atomNumBoxes[i].setMaximumSize(new Dimension(50, 50));
      atBox.add(atomNumBoxes[i]).setVisible(false);
      final int num = i;
      atomNumBoxes[i].addKeyListener(new KeyListener(){

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {}

        @Override
        public void keyReleased(KeyEvent e) {
          editValueTf.setText("");
          editValueTf.setEnabled(modelEditGetSelected().length() > 0);
        }
        
      });
      atomNumBoxes[i].addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent arg0) {
          doAtomNumBoxFocus(true, num);
        }

        @Override
        public void focusLost(FocusEvent arg0) {
          doAtomNumBoxFocus(false, 0);
        }
      });
      atomNumBoxes[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          doSetAtomBoxesFromSelection(null);
        }
      });
    }

    innerEditBox.add(atBox);

    Box box = Box.createHorizontalBox();
    box.add(new JLabel("Symmetry Type: "));
    jcSymOps = new JComboBox<String>();
    jcSymOps.addItem("<Select Transition Metal>");
    jcSymOps.setMaximumSize(new Dimension(180, 40));
    jcSymOps.setEnabled(false);
    box.add(jcSymOps);
    box.setVisible(false);
    innerEditBox.add(box);
    
    currVal = new JTextField("pick atoms...");
    currVal.setFont(titleFont);
    currVal.setBackground(new Color(220,220,220));
    currVal.setMinimumSize(new Dimension(250, 40));
    currVal.setPreferredSize(new Dimension(250, 40));
    currVal.setMaximumSize(new Dimension(250, 40));
//    currVal.setEditable(false);
    currVal.setHorizontalAlignment(SwingConstants.CENTER);
    innerEditBox.add(currVal).setVisible(false);
    
    valueLabel = new JLabel();
    valueLabel.setAlignmentX(0.5f);
    innerEditBox.add(valueLabel).setVisible(false);

    editValueTf = new JTextField("Select atoms...");
    editValueTf.setVisible(false);
    editValueTf.setMaximumSize(new Dimension(200, 30));
    editValueTf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doEditValueTextField();
      }
    });
    editValueTf.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent arg0) {
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        if (!editValueTf.getText().equals("")
            && !editValueTf.getText().contains("Select"))
          jbApply.setEnabled(true);
      }

      @Override
      public void removeUpdate(DocumentEvent arg0) {
        if (editValueTf.getText().equals(""))
          jbApply.setEnabled(false);
      }
    });
    innerEditBox.add(editValueTf).setVisible(false);

    
    Box lowBox = Box.createHorizontalBox();
    jbClear = new JButton("Clear Selected");
    jbClear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected(true);
      }
    });
    jbApply = new JButton("Apply");
    jbApply.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doApply();
      }
    });
    lowBox.add(jbClear).setVisible(false);
    lowBox.add(jbApply).setVisible(false);
    innerEditBox.add(lowBox);

  }

  protected void doAtomNumBoxFocus(boolean isGained, int num) {
    if (!isGained) {
      int atnum = PT.parseInt(atomNumBoxes[num].getText());
      if (atnum > vwr.ms.ac || atnum < 1) {
        atomNumBoxes[num].setText("");
      } else {
        doSetAtomBoxesFromSelection(null);
      }
    } else if (num == boxCount - 1) {
      jbApply.setEnabled(modelEditGetSelected().length() > 0);
    }
  }

  protected void doApply() {
    postActionToNBO_m(actionID);
  }

  protected void doEditValueTextField() {
    postActionToNBO_m(actionID);
  }

  protected void updateSelected(boolean doPost) {
    String selected = modelEditGetSelected();          
    String script = "measure delete;";
    int cnt = selected.split(" ").length;
    editValueTf.setEnabled(cnt > 0);
    editValueTf.setText("");
    editValueTf.requestFocus();
    switch (boxCount) {
    case BOX_COUNT_4:
      String desc = "";
      if (cnt > 1) 
        script += "measure " + selected + " \" \";";
      switch (cnt) {
      case 0:
        currVal.setText("pick atoms...");
        break;
      case 1:
        desc = (actionID == MODEL_ACTION_ALTER ? "atomic number or symbol"
            : "atomic number");
        break;
      case 2:
        desc = "distance";
        break;
      case 3:
      case 4:
        desc = (cnt == 3 ? "angle" : "dihedral angle");
        break;
      }
      valueLabel.setText("(" + desc + ")");
      valueLabel.setVisible(cnt > 0);
      break;
    case BOX_COUNT_2:
      if (cnt == 2) {
        jbApply.setEnabled(true);
        if (editValueTf.isVisible())
          editValueTf.requestFocus();
        else
          atomNumBoxes[1].requestFocus();
      }
      break;
    case BOX_COUNT_1:
      if (cnt == 1) {
        if (actionID == MODEL_ACTION_REBOND) {
          jcSymOps.removeAllItems();
          jcSymOps.setEnabled(true);
          int atomInd = Integer.parseInt(atomNumBoxes[0].getText()) - 1;
          int val = vwr.ms.at[atomInd].getValence();
          jbApply.setEnabled(true);
          switch (val) {
          case 4:
            for (String x : new String[] { "td", "c3vi", "c4v" })
              jcSymOps.addItem(x);
            break;
          case 5:
            for (String x : new String[] { "c4vo", "c4vi" })
              jcSymOps.addItem(x);
            break;
          case 6:
            for (String x : new String[] { "c3vo", "c3vi", "c5vo", "c5vi" })
              jcSymOps.addItem(x);
            break;
          default:
            jcSymOps.addItem("<Select Transition Metal>");
            jcSymOps.setEnabled(false);
            jbApply.setEnabled(false);
          }
        }
      }
    }
    if (actionID == MODEL_ACTION_ALTER) {
      postActionToNBO_m(MODEL_ACTION_VALUE);
    }
    runScriptQueued(script);
    editValueTf.setText("");
    editValueTf.setEnabled(selected.length() > 0);
    showSelected(selected);
    if (actionID == MODEL_ACTION_VALUE || doPost)
      postActionToNBO_m(actionID);
  }

  protected String modelEditGetSelected() {
    String s = "";
    for (int j = 0; j < boxCount; j++)
      s += atomNumBoxes[j].getText().trim() + " ";
    return PT.rep(s.trim(), "  ", " ").trim();
  }

  private Box getSaveComponent() {
    Box sBox = createBorderBox(true);
    final String[] SAVE_OPTIONS = { "<Select File Type>",
        "XYZ                        [.xyz]",
        "MOL                        [.mol]",
        "NBO Cartesian              [.cfi]",
        "NBO Valence                [.vfi]",
        "Gaussian Input             [.gau]",
        "Gaussian Input (Cartesian) [.gau]",
        "Gaussian Input (z-Matrix)  [.gau]",
        "GAMESS Input               [.gms]",
        "ADF Input                  [.adf]",
        "Jaguar Input               [.jag]",
        "MM2-Input                  [.mm2]",
        "Dewar Type Input           [.mnd]",
        "Molpro Input               [.mp]", "NWChem Input               [.nw]",
        "Orca Input                 [.orc]",
        "PQS Input                  [.pqs]", "Q-Chem Input               [.qc]" };
    jComboSave = new JComboBox<String>(SAVE_OPTIONS);

    jComboSave.setFont(monoFont);
    saveFileHandler = new NBOFileHandler("", "", NBOFileHandler.MODE_MODEL_SAVE,
        OUTPUT_FILE_EXTENSIONS, (NBODialog) this) {
      @Override
      protected boolean doFileBrowsePressed() {
        String folder = tfDir.getText().trim();
        String name = tfName.getText().trim();
        String ext = tfExt.getText().trim();
        if (!ext.equals("") && !folder.equals("") && !name.equals("")) {
          File f = new File(folder + "/" + name + "." + ext);
          if (!PT.isOneOf(ext, OUTPUT_FILE_EXTENSIONS)) {
            alertError("Invalid output extenstion");
            return false;
          }
          if (f.exists()) {
            int i = JOptionPane.showConfirmDialog(null, "File " + f
                + " already exists, do you want to overwrite contents?",
                "Warning", JOptionPane.YES_NO_OPTION);
            if (i == JOptionPane.NO_OPTION)
              return false;
            dialog.inputFileHandler.setInput(folder, name, ext);

          }
          saveModel(folder, name, ext);
          saveWorkingPath(fileDir);
          return true;
        }
        JFileChooser myChooser = new JFileChooser();
        if (ext.equals(""))
          useExt = OUTPUT_FILE_EXTENSIONS;
        else
          useExt = ext;
        myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt
            .split(",")));
        myChooser.setFileHidingEnabled(true);
        String savePath = fileDir;
        if (!folder.equals("")) {
          if (!folder.contains(":"))
            folder = "C:/" + folder;
        } else
          folder = new File(this.fileDir).getParent();
        if (name.equals("") && jobStem != null)
          savePath = tfDir.getText()
              + "/"
              + (jobStem.equals("") ? "new.cfi" : jobStem
                  + (ext.contains(";") ? "" : "." + ext));
        else
          savePath = tfDir.getText() + "/" + name + "." + ext;
        myChooser.setSelectedFile(new File(savePath));
        int button = myChooser.showSaveDialog(this);
        if (button == JFileChooser.APPROVE_OPTION) {
          File newFile = myChooser.getSelectedFile();
          ext = NBOFileHandler.getExt(newFile);
          if (PT
              .isOneOf(NBOFileHandler.getExt(newFile), OUTPUT_FILE_EXTENSIONS)) {
            if (newFile.exists()) {
              int i = JOptionPane.showConfirmDialog(null, "File " + newFile
                  + " already exists, do you want to overwrite contents?",
                  "Warning", JOptionPane.YES_NO_OPTION);
              if (i == JOptionPane.NO_OPTION)
                return false;
            }

            dialog.inputFileHandler.setInput(folder, name, ext);

            fileDir = newFile.getParent();
            saveModel(newFile.getParent(), NBOFileHandler.getJobStem(newFile),
                ext);
            saveWorkingPath(fileDir);
          } else
            logError("Invalid extension defined");
        }
        return false;
      }

    };
    jComboSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (jComboSave.getSelectedIndex() > 0)
          doComboSaveAction(jComboSave.getSelectedItem().toString());
      }
    });
    sBox.add(jComboSave);
    sBox.add(saveFileHandler);
    return sBox;
  }

  protected void doComboSaveAction(String item) {
      String ext = item.substring(item.indexOf("[") + 2, item.indexOf("]"));
      saveFileHandler.tfExt.setText(ext);
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

  private void createInput(final JTextField field, JRadioButton radio) {
    field.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doLoadtModelFromTextBox(field);
      }
    });
    addFocusListeners(field, radio);
  }

  private void addFocusListeners(final JComponent field,
                                   final JRadioButton radio) {
    field.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        radio.setSelected(true);
      }

      @Override
      public void focusLost(FocusEvent arg0) {
      }
    });
    radio.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        field.requestFocus();
      }

      @Override
      public void focusLost(FocusEvent arg0) {
      }
    });
  }

  /**
   * edit action selected
   * 
   * @param action
   */
  protected void doModelAction(int action) {
    actionID = action;
    runScriptQueued("set refreshing true; measurements delete"); // just in case
    clearSelected(true);
    switch (action) {
    case MODEL_ACTION_MUTATE:
      boxCount = BOX_COUNT_1;
      setEditBox("Radical name or line formula...");
      break;
    case MODEL_ACTION_REBOND:
      boxCount = BOX_COUNT_1;
      setEditBox(null);
      break;
    case MODEL_ACTION_CLIP:
    case MODEL_ACTION_FUSE:
    case MODEL_ACTION_LINK:
    case MODEL_ACTION_SWITCH:
    case MODEL_ACTION_3CHB:
      boxCount = BOX_COUNT_2;
      setEditBox(null);
      break;
    case MODEL_ACTION_ALTER:
    case MODEL_ACTION_TWIST:
    case MODEL_ACTION_VALUE:
      boxCount = BOX_COUNT_4;
      setEditBox(null);
      break;
    }
  }

  private void setEditBox(String label) {
    if (label == null)
      label = "Select atom" + (boxCount > 1 ? "s" : "") + "...";
    jbApply.setEnabled(false);
    for (int i = 0; i < 4; i++)
      atomNumBoxes[i].setVisible(i < boxCount);
    atomsLabel.setText(boxCount == 0 ? "" : "Atom" + (boxCount > 1 ? "s" : "") + ":");
    editValueTf.setText(label);
    editValueTf.setEnabled(false);
    jcSymOps.getParent().setVisible(actionID == MODEL_ACTION_REBOND);
    switch (actionID) {
    case MODEL_ACTION_ALTER:
    case MODEL_ACTION_MUTATE:
    case MODEL_ACTION_TWIST:
    case MODEL_ACTION_3CHB:
      editValueTf.setVisible(true);
      break;
    default:
      editValueTf.setVisible(false);
    }

    currVal.setVisible(boxCount == BOX_COUNT_4);
    valueLabel.setVisible(true);//actionID == MODEL_ACTION_ALTER);

    jbApply.setVisible(actionID != MODEL_ACTION_VALUE);
    jbClear.setVisible(true);
    innerEditBox.repaint();
    innerEditBox.revalidate();
  }

  ActionListener redoAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String curr = redoStack.pop();
      if (redoStack.isEmpty()) {
        redo.setEnabled(false);
      }
      loadModelToNBO(curr, true);
      logCmd("Redo");
    }
  };

  ActionListener undoAction = new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      String curr = undoStack.pop();
      if (undoStack.isEmpty()) {
        undo.setEnabled(false);
        return;
      }
      String tmp = undoStack.pop();
      loadModelToNBO(tmp, true);
      redoStack.push(curr);
      if (redoStack.size() > MAX_HISTORY)
        redoStack.removeElementAt(MAX_HISTORY);
      logCmd("Undo");
    }
  };
  
  /**
   * Clear out the text fields
   * @param andShow TODO
   * 
   */
  protected void clearSelected(boolean andShow) {
    for (int i = 0; i < boxCount; i++) {
      System.out.println("clearing all boxes");
      atomNumBoxes[i].setText("");
    }
    
    if (currVal != null)
      currVal.setText("");
    if (valueLabel != null)
      valueLabel.setText(" ");
    if (editValueTf != null) {
      editValueTf.setText("Select atoms...");
      editValueTf.setEnabled(false);
      jbApply.setEnabled(false);
    }
    if (andShow)
      updateSelected(false);
  }

  /**
   * Apply the selected edit action to a model.
   * 
   * @param actionID
   * 
   */
  protected void postActionToNBO_m(int actionID) {
    SB sb = new SB();
    String selected = modelEditGetSelected();
    String cmd = MODEL_ACTIONS[actionID].toLowerCase() + " " + selected + " ";
    String val = editValueTf.getText().trim();
    if (actionID == MODEL_ACTION_ALTER && PT.parseInt(val) == Integer.MIN_VALUE) {
      if (val.length() == 0)
        return;
      val = "" + Elements.elementNumberFromSymbol(val, true);
    }
    if (boxCount == BOX_COUNT_4 || boxCount == BOX_COUNT_1)
      cmd += val;
    else if (actionID == MODEL_ACTION_3CHB) {
      if (!val.startsWith(":"))
        cmd += ":";
      cmd += val;
    }
    if (actionID == MODEL_ACTION_REBOND)
      cmd += jcSymOps.getSelectedItem().toString();
    runScriptNow("save orientation o2");
    postAddCmd(sb, cmd);
    logCmd(cmd);
    jbApply.setEnabled(false);

    if (actionID == MODEL_ACTION_VALUE) {
      postNBO_m(sb, NBOService.MODE_MODEL_VALUE, "Checking Value", null, null);
    } else {
      postNBO_m(sb,
          (actionID == MODEL_ACTION_ALTER ? NBOService.MODE_MODEL_ALTER
              : NBOService.MODE_MODEL_EDIT), "Editing model", null, null);
      resetOnAtomClick = true;
    } 
    
  }

  /**
   * Post a request for a point group symmetry check.
   */
  protected void doGetSymmetry() {
    String cmd = "symmetry";
    logCmd(cmd);
    postNBO_m(postAddCmd(new SB(), cmd), NBOService.MODE_MODEL_SYMMETRY,  "Checking Symmetry", null, null);
  }

  /**
   * clipped in?
   * 
   * @param textBox
   */
  protected void doLoadtModelFromTextBox(JTextField textBox) {
    String model = textBox.getText().trim();
    if (model.length() == 0)
      return;
    String s = "";
    inputFileHandler.setInput(null, "", "");
    saveFileHandler.setInput(null, "", "");
    clearSelected(false);
    if (textBox == jtNIHInput) {
      modelOrigin = ORIGIN_NIH;
      notFromNBO = true;
      if ("$:=".indexOf(model.charAt(0)) < 0)
        model = "$" + model;
      if (model.startsWith("=")) {
        switch (model.length()) {
        case 5:
          break;
        case 4:
          model = "=" + model; // ligand codes require two == signs, for example ==HEM
          break;
        default:
          // "=" can be the start of may databases if there is a / present
          if (model.indexOf("/") < 0)
            logError("PDB codes must be of the form XXX for ligands and XXXX for standard PDB entries.");
          break;
        }
      }
      jtLineFormula.setText("");
      saveFileHandler.setInput(null, model, "mol");
      logCmd("get " + model);
      if (loadModelFileNow(model) == null) {
        model = (model.charAt(0) == ':' ? "$" : ":") + model.substring(1);
        if (model.startsWith("$=")) {
          logError("RCSB does not recognize ligand code " + model.substring(2) + ".");
          return;
        }
        logCmd("get " + model);
        if (loadModelFileNow(model) == null) {
          logError("Neither NIH/CIR nor PubChem have recognize this identifier.");
          notFromNBO = false;
        }
      }
    } else {
      modelOrigin = ORIGIN_LINE_FORMULA;
      SB sb = new SB();
      jtNIHInput.setText("");
      s = "show " + model;
      saveFileHandler.setInput(null, "line", "mol");
      postAddCmd(sb, s);
      logCmd(s);
      postNBO_m(sb, NBOService.MODE_MODEL_NEW, "model from line input...",
          null, null);
    }
  }

  /**
   * Loads model gotten from Pubchem/NIS databases
   * 
   * @param s
   *        - cfi formatted model string
   * @param undoRedo 
   */

  protected void loadModelToNBO(String s, boolean undoRedo) {
    boolean alsoLoadJmol = true;
    if (s == null) {
      s = runScriptNow(";print data({*},'cfi');");
      alsoLoadJmol = false;
    }
    if (undoRedo)
      runScriptNow("save orientation o2");
    SB sb = new SB();
    postAddGlobalC(sb, "PATH", nboService.getServerPath(null) + "/");
    postAddGlobalC(sb, "ESS", "c");
    postAddGlobalC(sb, "FNAME", "jmol_outfile");
    postAddGlobalC(sb, "IN_EXT", "cfi");
    postAddCmd(sb, "use");
    postNBO_m(sb, (undoRedo ? NBOService.MODE_MODEL_UNDO_REDO : NBOService.MODE_MODEL_TO_NBO), (alsoLoadJmol ? "Loading" : "Sending") + " model to NB", "jmol_outfile.cfi", s);

  }

  /**
   * Loads model from file type after browse
   * 
   * @param path
   * @param fname
   * @param ext
   */
  protected void loadModelFromNBO(String path, String fname, String ext) {
    if (PT.isOneOf(ext, JMOL_EXTENSIONS)) {
      notFromNBO = true;
      runScriptNow("set refreshing false"); // a bit risky
      loadModelFileQueued(new File(path  + "\\" + fname + "." + ext), false);
      return;
    }
    String ess = getEss(ext, true);
    SB sb = new SB();
    if (jtNIHInput != null) {
      jtNIHInput.setText("");
      jtLineFormula.setText("");
    }
    modelOrigin = ORIGIN_FILE_INPUT;
    postAddGlobalC(sb, "PATH", path);
    postAddGlobalC(sb, "ESS", ess);
    postAddGlobalC(sb, "FNAME", fname);
    postAddGlobalC(sb, "IN_EXT", ext.toLowerCase());
    postAddCmd(sb, "use");
    clearSelected(false);
    logCmd("use." + ess + " " + fname + "." + ext);
    postNBO_m(sb, NBOService.MODE_MODEL_NEW, "Loading model from NBO...", null, null);

  }

  /**
   * Save the model either by having Jmol convert it or NBOServe.
   * 
   * @param path
   * @param fname
   * @param ext
   */
  protected void saveModel(String path, String fname, String ext) {
    if (PT.isOneOf(ext, JMOL_EXTENSIONS)) {
      String s = vwr.getModelExtract("1.1", false, false, ext.toUpperCase());
      String ret = vwr.writeTextFile(path + "\\" + fname + "." + ext, s);
      logValue(ret);
      return;
    }
    String ess = getEss(ext, false);
    SB sb = new SB();
    
    postAddGlobalC(sb, "PATH", path);
    postAddGlobalC(sb, "ESS", ess);
    postAddGlobalC(sb, "FNAME", fname);
    postAddGlobalC(sb, "OUT_EXT", ext.toLowerCase());
    postAddCmd(sb, "save");
    postNBO_m(sb, NBOService.MODE_MODEL_SAVE, "Saving model...", null, null);
    logCmd("save." + ess + " " + fname);
    logValue("--Model Saved--<br>" + path + "\\" + fname + "." + ext);
  }

  /**
   * @param ext
   *        - extension to convert
   * @param isLoading
   *        - true if "use" (loading), false if saving
   * @return ess code used internally by NBOServie
   */
  private String getEss(String ext, boolean isLoading) {
    ext = ext.toLowerCase();
    if ((ext.equals("gau") || ext.equals("g09") || ext.equals("com")) && !isLoading) {
      if (jComboSave.getSelectedItem().toString().contains("(C")) {
        return ext.charAt(0) + "c";
      } else if (jComboSave.getSelectedItem().toString().contains("(z")) {
        return ext.charAt(0) + "z";
      } else
        return "g";
    } else if (ext.equals("cfi") || ext.equals("vfi") || ext.equals("gau")
        || ext.equals("log") || ext.equals("g09"))
      return "" + ext.charAt(0);
    else if (ext.equals("47"))
      return "a";
    else if (ext.equals("mm2"))
      return "mm";
    else if (ext.equals("com"))
      return "g";
    else
      return ext;
  }

  /**
   * callback notification from Jmol
   * 
   * @param atomno
   *        Jmol's atom number - 1-based
   * 
   */
  protected void notifyPick_m(String atomno) {
    runScriptNow("measure delete;" + (resetOnAtomClick ? "select none" : ""));
    if (resetOnAtomClick) {
      clearSelected(false);
    }
    resetOnAtomClick = false;
    if (boxCount == 0)
      return;
    String[] tok = atomno.split(",");
    String selected = " " + modelEditGetSelected() + " ";
    if (tok.length > 1) {
      //Bond selection
      if (boxCount == BOX_COUNT_2)
        clearSelected(true);
      String[] tok2 = tok[1].split(" ");
      String at1 = tok2[2].replaceAll("[\\D]", "");
      if (!selected.contains(" " + at1 + " "))
        notifyPick_m(at1);
      String at2 = tok2[5].replaceAll("[\\D]", "");
      if (!selected.contains(" " + at2 + " "))
        notifyPick_m(at2);
      return;
    }
    boolean isSelected = (vwr.evaluateExpressionAsVariable(
        "{*}[" + atomno + "].selected").asFloat() == 1);
    if (isSelected) {
      selected = PT.rep(selected, " " + atomno + " ", " ").trim();
    } else {
      if (PT.getTokens(selected).length >= boxCount) {
        clearSelected(true);
        selected = "";
      }
      selected += " " + atomno;
    }
    doSetAtomBoxesFromSelection(selected);
  }

  protected void doSetAtomBoxesFromSelection(String selected) {
    if (selected == null)
      selected = modelEditGetSelected();
    String[] split = PT.getTokens(selected);
    System.out.println("setting " + selected);
    for (int i = 0; i < atomNumBoxes.length; i++) {
      atomNumBoxes[i].setText(i >= split.length ? "" : "  " + split[i]);
      System.out.println("set  i=" + i + " " + atomNumBoxes[i].getText());
    }
    updateSelected(false);
    
  }

  private void setCurrentValue(String sval) {
    currVal.setText(sval.length() == 0 ? "pick atoms..." : "current value: " + sval);
  }

  /**
   * callback notification from Jmol
   * 
   */
  protected void notifyLoad_m() {

    String fileContents = runScriptNow("print data({*},'cfi')");
    if (notFromNBO) {
      notFromNBO = false;
      loadModelToNBO(fileContents, false);
      return;
    }
    runScriptNow(JMOL_FONT_SCRIPT + ";select within(model,visible);rotate best;");
    doSetStructure(null);
    showComponents(true);
    innerEditBox.setVisible(true);
    if (vwr.ms.ac > 0)
      if (fileContents != null) {
        undoStack.push(fileContents);
        if (undoStack.size() > MAX_HISTORY)
          undoStack.removeElementAt(0);
      }
      undo.setEnabled(undoStack.size() > 1);
      redo.setEnabled(!redoStack.isEmpty());
    // "({1})"
    rebond.setEnabled(((String) vwr.evaluateExpression("{transitionMetal}")).length() > 4);
    if (actionID == MODEL_ACTION_MUTATE) {
      doModelAction(actionID);
    }
    runScriptNow("select none; select on;refresh");
  }
  
  protected void showConfirmationDialog(String st, File newFile, String ext) {
    int i = JOptionPane.showConfirmDialog(null, st, "Warning",
        JOptionPane.YES_NO_OPTION);
    if (i == JOptionPane.YES_OPTION)
      saveModel(newFile.getParent(), NBOFileHandler.getJobStem(newFile), ext);
  }

  protected static double round(double value, int places) {
    if (places < 0)
      throw new IllegalArgumentException();
    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  /**
   * Post a request to NBOServe with a callback to processNBO_s.
   * 
   * @param sb
   *        command data
   * @param mode 
   * @param statusMessage
   * @param fileName  optional
   * @param fileData  optional
   */
  private void postNBO_m(SB sb, final int mode, String statusMessage, String fileName, String fileData) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO_m(mode, req);
      }
    }, statusMessage, "m_cmd.txt", sb.toString(), fileName, fileData);
    nboService.postToNBO(req);
  }

  /**
   * Process the reply from NBOServe for a MODEL request
   * 
   * @param mode
   * @param req
   */
  protected void processNBO_m(int mode, NBORequest req) {
    String[] a = req.getReplyLines();
    if (a[0].indexOf("DATA") >= 0) {
      a[0] += " NBO";
    }
//    boolean hasModel = ();
//    String postFix = "";
//    String s = "";
//    String sep = "\n";
//    if (hasModel) {
//      a[0] += " NBO ";
//      boolean hasEnd = false;
//      for (int i = 0; i < a.length; i++) {
//        if (i == a.length - 1)
//          sep = "";
//        if (a[i].indexOf("END") >= 0) {
//          hasEnd = true;
//          s += a[i] + sep;
//          continue;
//        } 
//        if (hasEnd)
//          postFix += a[i] + sep;
//        else
//          s += a[i] + sep;
//      }
//    }
    String s = PT.join(a, '\n', 0);
    switch (mode) {
    case NBOService.MODE_MODEL_ALTER:
      // using quaternion analysis to reorient the structure even though it has been messed up.
      runScriptQueued("z = show('zoom');set refreshing false;x = {*}.xyz.all;load " + s + JMOL_FONT_SCRIPT
          + ";compare {*} @x rotate translate 0;script inline @z;set refreshing true");
      break;
    case NBOService.MODE_MODEL_NEW:
    case NBOService.MODE_MODEL_EDIT:
      if (s.contains("\\"))
        s = s.replaceAll("\\\\", "");
      s += JMOL_FONT_SCRIPT;
      if (mode == NBOService.MODE_MODEL_EDIT)
        s = "set refreshing off;save orientation o4;load " + s
            + ";restore orientation o4;set refreshing on";
      else
        s = "load " + s;
      runScriptQueued(s);
      break;
    case NBOService.MODE_MODEL_SAVE:
      break;
    case NBOService.MODE_MODEL_SYMMETRY:
      // do not reorient the return in this case
      String symmetry = s.substring(0, s.indexOf("\n"));
      logValue(symmetry);
      // fix issue with blank first line
      s = PT.rep(s.substring(s.indexOf("\n") + 1), "\"\n", "\" NBO\n");
      s = "set refreshing false;load " + s + JMOL_FONT_SCRIPT
          + ";set refreshing true";
      runScriptQueued(s);
      break;
    case NBOService.MODE_MODEL_TO_NBO:
      s = "load " + s  + JMOL_FONT_SCRIPT + ";set refreshing true;";
//      s = "set refreshing off;save orientation o3;load " + s
//          + ";restore orientation o3;set refreshing on";
      runScriptQueued(s);
      break;
    case NBOService.MODE_MODEL_UNDO_REDO:
      runScriptQueued("set refreshing false;load " + s + JMOL_FONT_SCRIPT
          + ";restore orientation o2;set refreshing true");
      break;
    case NBOService.MODE_MODEL_VALUE:
      String sval = a[0].trim();
      logValue(sval);
      setCurrentValue(sval);
      break;
    }
  }
}
