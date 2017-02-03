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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
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

  //encodes number of atoms that can be selected
  protected int boxCount;
  private final static int BOX_COUNT_4 = 4, BOX_COUNT_2 = 2, BOX_COUNT_1 = 1;
  private final static int MAX_HISTORY = 5;

  ///  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";

  protected String editActionName;
  private Box innerEditBox;
  private JTextField jtNIHInput, jtLineFormula;
  private JComboBox<String> jcSymOps;
  protected JTextField editValueTf;
  protected JButton jbApply, jbClear;

  protected JComboBox<String> jComboSave;

  protected JButton undo, redo;
  Stack<String> undoStack, redoStack;

  protected JTextField currVal;
  protected JTextField[] atomNumBox;
  protected JLabel valLab = new JLabel("");
  protected JPanel panel;

  /**
   * A model is being loaded into Jmol that NBO does not know about yet
   */
  protected boolean notFromNBO;

  protected String selected = "";

  private JButton rebond;

  private JLabel atomsLabel;
  protected int editAction;
  private Box editComponent;
  private Box inputHeader;
  private Box saveHeader;
  private Component inputComponent;
  private Box saveComponent;
  private Box editHeader;

  private void showComponents(boolean tf) {
    editHeader.setVisible(tf);
    editComponent.setVisible(tf);
    saveHeader.setVisible(tf);
    saveComponent.setVisible(tf);
  }

  protected JPanel buildModelPanel() {
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
        Object item = jComboUse.getSelectedItem();
        String s = "";
        if (jComboUse.getSelectedIndex() > 0) {
          s = item.toString();
          s = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
          inputFileHandler.tfExt.setText(s);
          inputFileHandler.useExt = s;
        } else {
          inputFileHandler.tfExt.setText("");
          s = INPUT_FILE_EXTENSIONS;
          inputFileHandler.useExt = s;
        }
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
      protected boolean browsePressed() {
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
          actionSelected(op);
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
        getSymmetry();
      }
    });
    lowBox.add(sym);
    rebond = new JButton(MODEL_ACTIONS[MODEL_ACTION_REBOND]);
    rebond.setEnabled(false);
    rebond.setToolTipText(EDIT_INFO[MODEL_ACTION_REBOND]);

    rebond.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        actionSelected(MODEL_ACTION_REBOND);
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
    atomNumBox = new JTextField[4];
    for (int i = 0; i < 4; i++) {
      atomNumBox[i] = new JTextField();
      atomNumBox[i].setFont(userInputFont);
      atomNumBox[i].setMaximumSize(new Dimension(50, 50));
      atBox.add(atomNumBox[i]).setVisible(false);
      final int num = i;
      atomNumBox[i].addKeyListener(new KeyListener(){

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
      atomNumBox[i].addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent arg0) {
          if (num == boxCount - 1) {
            jbApply.setEnabled(modelEditGetSelected().length() > 0);
          }
        }

        @Override
        public void focusLost(FocusEvent arg0) {
          int atnum = Integer.MAX_VALUE;
          try {
            atnum = Integer.parseInt(atomNumBox[num].getText());
          } catch (Exception e) {
          }
          if (atnum > vwr.ms.ac) {
            atomNumBox[num].setText("");
          }
          selected = modelEditGetSelected();
          getValue();
          showSelected(PT.getTokens(selected));
          editValueTf.setText("");
          editValueTf.setEnabled(selected.length() > 0);
        }
      });
      atomNumBox[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          selected = modelEditGetSelected();
          applyEdit();
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
    currVal = new JTextField();
    currVal.setFont(titleFont);
    currVal.setMaximumSize(new Dimension(200, 40));
    currVal.setEditable(false);
    innerEditBox.add(currVal).setVisible(false);
    valLab = new JLabel();
    valLab.setAlignmentX(0.5f);
    innerEditBox.add(valLab).setVisible(false);

    editValueTf = new JTextField("Select atoms...");
    editValueTf.setVisible(false);
    editValueTf.setMaximumSize(new Dimension(200, 30));
    editValueTf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        applyEdit();
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

    jbClear = new JButton("Clear Selected");
    jbClear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected();
      }
    });
    jbApply = new JButton("Apply");
    jbApply.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        applyEdit();
      }
    });
    Box lowBox = Box.createHorizontalBox();
    lowBox.add(jbClear).setVisible(false);
    lowBox.add(jbApply).setVisible(false);
    innerEditBox.add(lowBox);

  }

  protected String modelEditGetSelected() {
    String s = "";
    for (int j = 0; j < boxCount; j++)
      s += atomNumBox[j].getText() + " ";
    s = PT.rep(s.trim(), "  ", " ");
    return  s.trim();
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
      protected boolean browsePressed() {
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
        Object item = jComboSave.getSelectedItem();
        if (jComboSave.getSelectedIndex() != 0) {
          String s = item.toString();
          String ext = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
          saveFileHandler.tfExt.setText(ext);
          //showSaveDialog(ext);
        }
      }
    });
    sBox.add(jComboSave);
    sBox.add(saveFileHandler);
    return sBox;
  }

  /**
   * add selection halos to atoms in s
   * 
   * @param s
   *        - array containing atomnums
   */
  protected void showSelected(String[] s) {
    String sel = "";
    for (String x : s)
      sel += " " + (Integer.parseInt(x) - 1);
    runScriptNow("select on ({" + sel + " })");
  }

  private void createInput(final JTextField field, JRadioButton radio) {
    field.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getModelFromTextBox(field);
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
  protected void actionSelected(int action) {
    editActionName = MODEL_ACTIONS[action].toLowerCase();
    editAction = action;
    runScriptNow("set refreshing true"); // just in case
    clearSelected();
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

  protected void setEditBox(String label) {
    if (label == null)
      label = "Select atom" + (boxCount > 1 ? "s" : "") + "...";
    jbApply.setEnabled(false);
    for (int i = 0; i < 4; i++)
      atomNumBox[i].setVisible(i < boxCount);
    atomsLabel.setText(boxCount == 0 ? "" : "Atom" + (boxCount > 1 ? "s" : "") + ":");
    editValueTf.setText(label);
    editValueTf.setEnabled(false);
    jcSymOps.getParent().setVisible(editAction == MODEL_ACTION_REBOND);
    switch (editAction) {
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
    valLab.setVisible(editAction == MODEL_ACTION_ALTER);

    jbApply.setVisible(editAction != MODEL_ACTION_VALUE);
    jbClear.setVisible(editAction != MODEL_ACTION_VALUE);
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
   * 
   */
  protected void clearSelected() {
    for (int i = 0; i < boxCount; i++)
      atomNumBox[i].setText("");
    if (currVal != null)
      currVal.setText("");
    if (valLab != null)
      valLab.setText(" ");
    runScriptNow("measure off;select none;refresh");
    selected = "";
    if (editValueTf != null) {
      editValueTf.setText("Select atoms...");
      editValueTf.setEnabled(false);
      jbApply.setEnabled(false);
    }
  }

  /**
   * Apply the selected edit action to a model.
   * 
   */
  protected void applyEdit() {
    SB sb = new SB();

    String cmd = editActionName + " " + selected + " ";
    String val = editValueTf.getText();
    if (editAction == MODEL_ACTION_ALTER && PT.parseInt(val) == Integer.MIN_VALUE)
      val = "" + Elements.elementNumberFromSymbol(val, true);
      if (boxCount == BOX_COUNT_4 || boxCount == BOX_COUNT_1)
        cmd += val;
      else if (editAction == MODEL_ACTION_3CHB) {
        if (!val.startsWith(":"))
          cmd += ":";
        cmd += val;
      }
    if (editAction == MODEL_ACTION_REBOND)
      cmd += jcSymOps.getSelectedItem().toString();
    runScriptNow("save orientation o2");
    sb.append("CMD " + cmd);
    logCmd(cmd);
    jbApply.setEnabled(false);
    
    if (editAction  == MODEL_ACTION_VALUE)
      postNBO_m(sb, NBOService.MODE_MODEL_VALUE, "Checking Value", null, null);
    else
      postNBO_m(sb, (editAction == MODEL_ACTION_ALTER ? 
          NBOService.MODE_MODEL_ALTER : NBOService.MODE_MODEL_EDIT), "Editing model", null, null);

  }

  /**
   * Post a request for a point group symmetry check.
   */
  protected void getSymmetry() {
    String cmd = "symmetry";
    logCmd(cmd);
    postNBO_m(new SB().append("CMD " + cmd), NBOService.MODE_MODEL_SYMMETRY,  "Checking Symmetry", null, null);
  }

  /**
   * clipped in?
   * 
   * @param textBox
   */
  protected void getModelFromTextBox(JTextField textBox) {
    String model = textBox.getText().trim();
    if (model.length() == 0)
      return;
    String s = "";
    inputFileHandler.setInput(null, "", "");
    saveFileHandler.setInput(null, "", "");
    clearSelected();
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
      sb.append("CMD " + s);
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
    sb.append("GLOBAL C_PATH " + nboService.getServerPath(null) + "/" + sep);
    sb.append("GLOBAL C_ESS c" + sep);
    sb.append("GLOBAL C_FNAME jmol_outfile" + sep);
    sb.append("GLOBAL C_IN_EXT cfi" + sep);
    sb.append("CMD use");
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
    sb.append("GLOBAL C_PATH " + path + sep);
    sb.append("GLOBAL C_ESS " + ess + sep);
    sb.append("GLOBAL C_FNAME " + fname + sep);
    sb.append("GLOBAL C_IN_EXT " + ext.toLowerCase() + sep);
    sb.append("CMD use");
    clearSelected();
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
    sb.append("GLOBAL C_PATH " + path + sep);
    sb.append("GLOBAL C_ESS " + ess + sep);
    sb.append("GLOBAL C_FNAME " + fname + sep);
    sb.append("GLOBAL C_OUT_EXT " + ext + sep);
    sb.append("CMD save");
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
   * @param atomno Jmol's atom number - 1-based
   * 
   */
  protected void notifyPick_m(String atomno) {

    if (boxCount == 0)
      return;
    String[] tok = atomno.split(",");
    if (tok.length > 1) {
      //Bond selection
      if (boxCount == BOX_COUNT_2)
        clearSelected();
      String[] tok2 = tok[1].split(" ");
      String at1 = tok2[2].replaceAll("[\\D]", "");
      if (!selected.contains(" " + at1 + " "))
        notifyPick_m(at1);
      String at2 = tok2[5].replaceAll("[\\D]", "");
      if (!selected.contains(" " + at2 + " "))
        notifyPick_m(at2);
      return;
    }
    editValueTf.requestFocus();
    editValueTf.setText("");
    if (boxCount < 3)
      editValueTf.setEnabled(true);
    boolean isSelected = (vwr.evaluateExpressionAsVariable("{*}[" + atomno + "].selected")
        .asFloat() == 1); 
    if (isSelected) {
      selected = (" " + selected).replace(" " + atomno + " ", " ").trim();
      if (selected.length() > 0)
        selected += " ";
      runScriptNow("select remove {*}[" + atomno + "];measure off;");
      String[] split = selected.split(" ");
      for (int i = 0; i < atomNumBox.length; i++)
        atomNumBox[i].setText(i >= split.length ? "" : "  " + split[i]);
    } else {
      selected += atomno + " ";
    }
    
  //  System.out.println(atomno + " / " + selected);
    selected = PT.rep(selected, "  ", " ").trim();
    int cnt = (selected.equals("") ? 1 : PT.getTokens(selected).length);
    if (selected.length() > 0)
      selected += " ";
    switch (boxCount) {
    case BOX_COUNT_4:
      String desc = "atomic number or symbol";
      String script = null;
      switch (cnt) {
      case 0:
        desc = "";
        script = "";
        break;
      case 5:
        clearSelected();
        selected += atomno + " ";
        cnt = 1;
        //$FALL-THROUGH$
      case 1:
        editValueTf.setText("");
        script = "print {*}[" + atomno + "].elemno";
        editValueTf.setEnabled(true);
        break;
      case 2:
        desc = "distance";
        runScriptNow("measure off;measure " + selected 
            + "\"2:%0.4VALUE //A\"" + ";measure " + selected
            + "\"2:%0.4VALUE //A\"");
        break;
      case 3:
      case 4:
        desc = (cnt == 3 ? "angle" : "dihedral angle");
        runScriptNow("measure off;measure " + selected);
        break;
      }
      String sval = "";
      if (script == null) {
        script = "print measure(";
        String[] tokens = PT.getTokens(selected);
        for (String x : tokens)
          script += "{*}[" + x + "] ";
        String s = runScriptNow(script + ")");
        try {
        String[] s2 = PT.getTokens("" + s); 
        if (s2.length < 2)
          System.out.println(script);
        s = s2[1];
        double val = Double.parseDouble(s);
        val = round(val, 2);
        sval = "" + val;
        } catch (Exception e) {
          System.out.println("TESTERROR1");
        }
      } else {
        sval = "" + runScriptNow(script);
      }
      currVal.setText("current value: " + sval);
      String s = "(" + desc + ")";//editAction.equals("value") ? desc : "new " + desc;
      valLab.setText(s);// + ":");
      valLab.setVisible(true);
      //log(sval, 'b');
      break;
    case BOX_COUNT_2:
      if (cnt == 2) {
        jbApply.setEnabled(true);
        if (editValueTf.isVisible())
          editValueTf.requestFocus();
        else
          atomNumBox[1].requestFocus();
      } else if (cnt == 3) {
        clearSelected();
        selected += atomno + " ";
        cnt = 1;
      }
      break;
    case BOX_COUNT_1:
      if (cnt == 2) {
        clearSelected();
        selected += atomno + " ";
        cnt = 1;
      }
      if (cnt == 1) {
        if (editAction == MODEL_ACTION_REBOND) {
          jcSymOps.removeAllItems();
          jcSymOps.setEnabled(true);
          int atomInd = Integer.parseInt(atomno) - 1;
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
        editValueTf.setEnabled(true);
        editValueTf.setText("");
        editValueTf.requestFocus();
      }
    }
    if (cnt == 0 || isSelected)
      return;
    runScriptNow("select add {*}[" + atomno + "]");
      atomNumBox[cnt - 1].setText("  " + atomno);

  }

  protected void getValue() {
    String script = "";
    String[] ats = selected.split(" ");
    int cnt = ats.length;
    if (cnt == 1) {
     // valLab.setText("(Atomic number)");
      editValueTf.setText("");
      editValueTf.setEnabled(true);
      script = "print {*}[" + selected + "].elemno";
      if (currVal != null)
        currVal.setText("atomic number: " + runScriptNow(script));
    } else {
      runScriptNow("measure off;measure " + selected + " " + (cnt == 2 ? 
             "\"2:%0.4VALUE //A\"" + ";measure " + selected
            + "\"2:%0.4VALUE //A\"" :""));
      script = "print measure({*}[";
      for (int i = 0; i < ats.length - 1; i++)
        script += ats[i] + "],{*}[";
      script += ats[ats.length - 1] + "]);";
      String s = runScriptNow(script);
      if (s != null) {
        if (s.split("\\s+").length > 1)
          currVal.setText((cnt == 2 ? "distance: " : cnt == 3 ? "angle: " : cnt == 4 ? "dihedral angle: " : "value: ") + s.split("\\s+")[1]);
      }
    }
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
    showAtomNums(false);
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
    if (editAction == MODEL_ACTION_MUTATE) {
      actionSelected(editAction);
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
      logValue(a[0]);
      break;
    }
  }
}
