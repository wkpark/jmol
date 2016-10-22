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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
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

abstract class NBODialogModel extends NBODialogConfig {

  protected NBODialogModel(JFrame f) {
    super(f);
  }

  private static final String INPUT_FILE_EXTENSIONS = 
      "adf;cfi;gau;log;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;vfi;g09;com;47";
  //TODO Jmol file extensions
  private static final String OUTPUT_FILE_EXTENSIONS = 
      "adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;vfi;g09;com";
  
  private static final String[] editInfo = {
    "Edit nuclear charge, bond length, bond angle, or dihedral angle",
    "Remove bond between two atoms",
    "Delete monovalent atoms and replace with bond",
    "Add bond between two atoms",
    "Replace atom with a new substituent-group",
    "Switch location of two groups",
    "Perform rigid torsional twist about dihedral angle",
    "Value of nuclear charge, bond length, bond angle, and dihedral angle",
    "Create 3-center linkage between two atoms and a ligand"
  };
  
  //encodes number of atoms that can be selected
  protected int editMode;
  private final static int ALTER = 4, CLIP = 2, MUTATE = 1;
  private final static int MAX_HISTORY = 5;

  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";

  protected String editAction;
  private String moveTo;
  
  private Box editBox;
  private JTextField jtJmolInput, jtLineInput;
  private JComboBox<String> jcSymOps;
  protected JTextField editValueTf;
  protected JButton jbEdit, jbClear;
  
  protected JComboBox<String> jComboSave;
  
  protected JButton undo, redo;
  Stack<String> undoStack, redoStack;
  
  protected JTextField currVal;
  protected JTextField[] atomNumBox;
  protected JLabel valLab = new JLabel("");
  protected JPanel panel;
  
  private boolean loadModel;

  protected String selected = "";

  protected JPanel buildModelPanel(){
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));

    panel.add(titleBox(" Input Model ", new HelpBtn("model_input_intro_help.htm")));
    panel.add(useBox());
    panel.add(editBox(panel)).setVisible(false);
    panel.add(titleBox(" Save Model ", 
        new HelpBtn("model_save_intro_help.htm"))).setVisible(false);
    panel.add(saveBox()).setVisible(false);
    if(vwr.ms.ac > 0){
      runScriptNow("select within(model,visible)");
      loadModelToNBO(runScriptNow("print data({selected},'cfi')"));
    }
    return panel;
    
  }

  /**
   * adds use elements to main panel
   * 
   * @return use elements
   */
  private Component useBox() {

    Box inputBox = borderBox(true);
    inputBox.setMaximumSize(new Dimension(355,155));
    
    final JRadioButton jrJmolIn = new JRadioButton("NIH/PubChem");
    jrJmolIn.setFont(nboFont);
    jrJmolIn.setSelected(true);
    final JRadioButton jrLineIn = new JRadioButton("Line Formula");
    jrLineIn.setFont(nboFont);
    final JRadioButton jrFileIn = new JRadioButton("File Input");
    jrFileIn.setFont(nboFont);
    ButtonGroup rg = new ButtonGroup();
    rg.add(jrJmolIn);
    rg.add(jrLineIn);
    rg.add(jrFileIn);
    createInput(jtJmolInput = new JTextField(), jrJmolIn);
    createInput(jtLineInput = new JTextField(), jrLineIn);
    jtLineInput.add(new JLabel("line formula"));
    String[] useOps = {"<Select File  Type>",
        "[.cfi]  JMol Cartesian",
        "[.vfi]  JMol Valence",
        "[.47]   NBO Archive",
        "[.gau]  Gaussian Input",
        "[.log]  Gaussian Output",
        "[.gms]  GAMESS Input",
        "[.adf]  ADF Input", 
        "[.jag]  Jaguar Input", 
        "[.mm2]  MM2-Input",
        "[.mnd]  Dewar Type Input",
        "[.mp]   Molpro Input", 
        "[.nw]   NWChem Input",
        "[.orc]  Orca Input", 
        "[.pqs]  PQS Input", 
        "[.qc]   Q-Chem Input"};
    final JComboBox<String> jComboUse = new JComboBox<String>(useOps);
    jComboUse.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Object item = jComboUse.getSelectedItem();
            String tmp = fileHndlr.tfExt.getText();
            String s = "";
            if (jComboUse.getSelectedIndex()!=0) {
              s = item.toString();
              s = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
              fileHndlr.tfExt.setText(s);
              fileHndlr.useExt = s;
              if(!fileHndlr.browsePressed())
                fileHndlr.tfExt.setText(tmp);
            }else{
              fileHndlr.tfExt.setText("");
              s = INPUT_FILE_EXTENSIONS;
              fileHndlr.useExt = s;
              if(!fileHndlr.browsePressed())
                fileHndlr.tfExt.setText(tmp);
            }
          }
        });
    JPanel p2 = new JPanel(new GridLayout(3, 2));
    p2.add(jrJmolIn);
    p2.add(jtJmolInput);
    p2.add(jrLineIn);
    p2.add(jtLineInput);
    p2.add(jrFileIn);
    p2.add(jComboUse);
    addListenersAndSize(jComboUse, jrFileIn);
    inputBox.add(p2);
    
    fileHndlr = new FileHndlr("","",1,INPUT_FILE_EXTENSIONS,(NBODialog)this){
  
      @Override
      protected boolean browsePressed() {
        String folder = tfDir.getText().trim();
        String name = tfName.getText();
        String ext = tfExt.getText();
        if(!folder.equals("")){
          if(!folder.contains(":")) folder = "C:/"+folder;
          folder = folder + "/"+(name.equals("") ? "new":name+"."+ext);
        }
        JFileChooser myChooser = new JFileChooser(); 
        if(useExt.contains(";"))
          myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt.split(";")));
        else
          myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
        myChooser.setFileHidingEnabled(true);
        if(!folder.equals(""))
          myChooser.setSelectedFile(new File(folder));
        int button = myChooser.showDialog(this, GT._("Select"));
        if (button == JFileChooser.APPROVE_OPTION) {
          File newFile = myChooser.getSelectedFile();
          if(newFile.toString().indexOf(".")<0){
            appendOutputWithCaret("File not found",'i');
            return false;
          }
          loadModelFromNBO(newFile.getParent(), (jobStem = getJobStem(newFile)), FileHndlr.getExt(newFile));

          fileHndlr.setInput(fileDir, jobStem, FileHndlr.getExt(newFile));
          fileDir = newFile.getParent();

          return true;
        }
        return false;
      }
    };
    inputBox.add(fileHndlr);
    return inputBox;
  }

  private Box editBox(Container c) {
    
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
    c.add(titleBox(" Edit Model ",topBox)).setVisible(false);
    Box editBox = borderBox(false);
    Box actionBox = Box.createVerticalBox();
    final String[] actions = 
      {"Alter","Clip","Fuse","Link", "Mutate", "Switch","Twist","Value","3chb"};
    final JRadioButton[] btns = new JRadioButton[actions.length];
    ButtonGroup rg = new ButtonGroup();
    for(int i=0;i<actions.length;i++){
      btns[i]=new JRadioButton(actions[i]);
      btns[i].setToolTipText(editInfo[i]);
      actionBox.add(btns[i]);
      rg.add(btns[i]);
      final int op = i;
      btns[i].addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editAction = actions[op].toLowerCase();
        actionSelected(editAction.charAt(0));
      }
      });
    }
    
    editBox.add(actionBox);
    Box rightBox = Box.createVerticalBox();
    editBox2();
    rightBox.add(this.editBox);
    Box lowBox = Box.createHorizontalBox();
    JButton sym = new JButton("Symmetry?");
    sym.setToolTipText("Display point-group symmetry of current model");
    sym.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        getSymmetry();
      }
    });
    lowBox.add(sym);
    JButton rebond = new JButton("Rebond");
    rebond.setToolTipText("Change bonding symmetry around transition metal");
    
    rebond.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e) {
        editAction = "rebond";
        actionSelected(editAction.charAt(0));
      }
    });
    lowBox.add(rebond);
    rightBox.add(lowBox);
    editBox.add(rightBox);

    btns[0].doClick();
    return editBox;
  }

  private void editBox2(){
    
    editBox = Box.createVerticalBox();
    editBox.setBorder(BorderFactory.createLoweredBevelBorder());
    editBox.setMaximumSize(new Dimension(275,200));
    editBox.setAlignmentX(0.5f);
    editBox.setVisible(false);
    Box atBox = Box.createHorizontalBox();
    atBox.add(new JLabel("Atoms: "));
    atomNumBox = new JTextField[4];
    for(int i=0;i < 4;i++){
      atomNumBox[i] = new JTextField();
      atomNumBox[i].setMaximumSize(new Dimension(50,50));
      atBox.add(atomNumBox[i]).setVisible(false);
      final int num = i;
      atomNumBox[i].addFocusListener(new FocusListener(){
        @Override
        public void focusGained(FocusEvent arg0) {
          if(num==editMode-1){
            jbEdit.setEnabled(true);
          }
        }
        @Override
        public void focusLost(FocusEvent arg0) {
          int atnum = 0;
          try{
            atnum = Integer.parseInt(atomNumBox[num].getText());
          }catch(Exception e){
            return;
          }
          if(atnum > vwr.ms.ac){
            atomNumBox[num].setText("");
          }
          String[] tmp = new String[editMode];
          selected = "";
          for(int j = 0; j < editMode;j++){
            tmp[j] = atomNumBox[j].getText();
            selected += (tmp[j].length()>0?tmp[j]+" ":"");
          }
          getValue();
          showSelected(selected.split(" "));
          editValueTf.setText("");
          editValueTf.setEnabled(true);
        }
        });
        atomNumBox[i].addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
          String tmp;
          selected = "";
          for(int j = 0; j < atomNumBox.length;j++){
            tmp = atomNumBox[j].getText();
            selected += (tmp.length()>0?tmp+" ":"");
          }
          editModel(editAction);
        }
        });
    }
    
    editBox.add(atBox);
    
    Box box = Box.createHorizontalBox();
    box.add(new JLabel("Symmetry Type: "));
    
    jcSymOps = new JComboBox<String>();
    jcSymOps.addItem("<Select Transition Metal>");
    jcSymOps.setMaximumSize(new Dimension(180,40));
    jcSymOps.setEnabled(false);
    box.add(jcSymOps);
    box.setVisible(false);
    editBox.add(box);
    currVal = new JTextField();
    currVal.setMaximumSize(new Dimension(200,40));
    currVal.setEditable(false);
    editBox.add(currVal).setVisible(false);
    valLab = new JLabel();
    valLab.setAlignmentX(0.5f);
    editBox.add(valLab).setVisible(false);
    
    editValueTf = new JTextField("Select atoms...");
    editValueTf.setVisible(false);
    editValueTf.setMaximumSize(new Dimension(200,30));
    editValueTf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel(editAction);
      }
    });
    editValueTf.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent arg0) {}
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        if (!editValueTf.getText().equals("") && !editValueTf.getText().contains("Select"))
          jbEdit.setEnabled(true);
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        if (editValueTf.getText().equals(""))
          jbEdit.setEnabled(false);
      }
    });
    
    editBox.add(editValueTf).setVisible(false);
    
    jbClear = new JButton("Clear Selected");
    jbClear.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected();
      }
    });
    jbEdit = new JButton("Apply");
    jbEdit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel(editAction);
      }
    });
    Box lowBox = Box.createHorizontalBox();
    lowBox.add(jbClear).setVisible(false);
    lowBox.add(jbEdit).setVisible(false);
    editBox.add(lowBox);
    
  }

  private Box saveBox() {
    
    Box sBox = borderBox(true);
    final String[] SAVE_OPTIONS = {
        "<Select File Type>",
        "JMol Cartesian             [.cfi]",
        "JMol Valence               [.vfi]",
        "Gaussian Input             [.gau]",
        "Gaussian Input (Cartesian) [.gau]",
        "Gaussian Input (z-Matrix)  [.gau]",
        "GAMESS Input               [.gms]",
        "ADF Input                  [.adf]", 
        "Jaguar Input               [.jag]", 
        "MM2-Input                  [.mm2]",
        "Dewar Type Input           [.mnd]",
        "Molpro Input               [.mp]", 
        "NWChem Input               [.nw]",
        "Orca Input                 [.orc]", 
        "PQS Input                  [.pqs]", 
        "Q-Chem Input               [.qc]"
      };
    jComboSave = new JComboBox<String>(SAVE_OPTIONS);

    jComboSave.setFont(nboFont);
    final FileHndlr saveBox = new FileHndlr("","",5,OUTPUT_FILE_EXTENSIONS,(NBODialog)this){
      @Override
      protected boolean browsePressed(){
        String folder = tfDir.getText().trim();
        String name = tfName.getText().trim();
        String ext = tfExt.getText().trim();
        if(!ext.equals("")&&!folder.equals("")&&!name.equals("")){
          File f = new File(folder +"/"+name+"."+ext);
          if (!PT.isOneOf(ext, OUTPUT_FILE_EXTENSIONS)){
            vwr.alert("Invalid output extenstion");
            return false;
          }
          if(f.exists()) {
            int i = JOptionPane.showConfirmDialog(
                null, "File " + f +" already exists, do you want to overwrite contents?", 
                "Warning", JOptionPane.YES_NO_OPTION);
            if(i==JOptionPane.NO_OPTION) return false;
            dialog.fileHndlr.setInput(folder, name, ext);

          }
          saveModel(folder, name, ext);
          saveWorkHistory();
          return true;
        }
        JFileChooser myChooser = new JFileChooser();
        if(ext.equals("")) 
          useExt = OUTPUT_FILE_EXTENSIONS;
        else 
          useExt = ext;
        myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt.split(",")));
        myChooser.setFileHidingEnabled(true);
        String savePath = fileDir;
        if(!folder.equals("")){
          if(!folder.contains(":")) 
            folder = "C:/"+folder;
        }else
          folder = new File(this.fileDir).getParent();
        if(name.equals("") && jobStem != null)
          savePath = tfDir.getText() + "/"+(jobStem.equals("") ? "new.cfi":jobStem+(ext.contains(";")?"":"."+ext));
        else
          savePath = tfDir.getText() + "/"+name+"."+ext;
        myChooser.setSelectedFile(new File(savePath));
        int button = myChooser.showSaveDialog(this);
        if (button == JFileChooser.APPROVE_OPTION) {
          File newFile = myChooser.getSelectedFile();
          ext = FileHndlr.getExt(newFile);
          if (PT.isOneOf(FileHndlr.getExt(newFile),OUTPUT_FILE_EXTENSIONS)) {
            if(newFile.exists()) {
              int i = JOptionPane.showConfirmDialog(
                  null, "File " + newFile +" already exists, do you want to overwrite contents?", 
                  "Warning", JOptionPane.YES_NO_OPTION);
              if(i==JOptionPane.NO_OPTION) return false;
            }

            dialog.fileHndlr.setInput(folder, name, ext);         
            
            fileDir = newFile.getParent();
            saveModel(newFile.getParent(), FileHndlr.getJobStem(newFile), ext);
            saveWorkHistory();
          }else appendOutputWithCaret("Invalid extension defined",'b');
        }
        return false;
      }
      
    };
    jComboSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object item = jComboSave.getSelectedItem();
        if (jComboSave.getSelectedIndex()!=0) {
          String s = item.toString();
          String ext = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
          saveBox.tfExt.setText(ext);
          //showSaveDialog(ext);
        }
      }
    });
    sBox.add(jComboSave);
    sBox.add(saveBox);
    return sBox;
  }
  
  /**
   * add selection halos to atoms in s
   * @param s - array containing atomnums
   */
  protected void showSelected(String[] s){
    runScriptNow("select remove {*}");
//    if(!jCheckSelHalo.isSelected()) {
//      return;
//    }
    for(String x:s)
      runScriptNow("select add {*}["+x+"]");
    runScriptQueued("select on");
  }

  private void createInput(final JTextField field, JRadioButton radio) {

    field.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getModel(field);
      }
    });
    addListenersAndSize(field, radio);
  }

  private void addListenersAndSize(final JComponent field, final JRadioButton radio) {
    field.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        radio.setSelected(true);
      }
      @Override
      public void focusLost(FocusEvent arg0) {}
    });
    radio.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent arg0) {
        field.requestFocus();
      }
      @Override
      public void focusLost(FocusEvent arg0) {}
    });
  }
 
  /**
   * edit action selected
   * @param action 
   */
  protected void actionSelected(char action) {
    clearSelected();
    switch (action) {
    case 'c':
    case 'f':
    case 'l':
    case 's':
      editMode = CLIP;
      setEditBox();
      break;
    case '3':
      editMode = CLIP;
      setEditBox();
      break;
    case 'm':
      editMode = MUTATE;
      setEditBox();
      break;
    case 'a':
    case 't':
    case 'v':
      editMode = ALTER;
      setEditBox();
      break;
    case 'r':
      editMode = MUTATE;
      setEditBox();
    }
  }
  protected void setEditBox() {
    jbEdit.setEnabled(false);
    for(int i = 0; i < 4; i++)
      atomNumBox[i].setVisible(i < editMode);
    
    editValueTf.setText("Select atoms...");
    editValueTf.setEnabled(false);
    jcSymOps.getParent().setVisible(editAction.charAt(0) == 'r');
    switch (editAction.charAt(0)){
    case 'a':
    case 'm':
    case 't':
    case '3':
      editValueTf.setVisible(true);
      break;
    default:
      editValueTf.setVisible(false);
    }
  
    currVal.setVisible(editMode == ALTER);
    valLab.setVisible(editMode == ALTER);
    
    jbEdit.setVisible(!editAction.equals("value"));
    jbClear.setVisible(!editAction.equals("value"));
    editBox.repaint();
    editBox.revalidate();
  }

  ActionListener redoAction = new ActionListener(){
    @Override
    public void actionPerformed(ActionEvent e){
      String curr = redoStack.pop();
      if(redoStack.isEmpty()) {
        redo.setEnabled(false);
      }
      loadModelToNBO(curr);
      appendOutputWithCaret("Redo", 'i');
    }
  };
  
  ActionListener undoAction = new ActionListener(){
    @Override
    public void actionPerformed(ActionEvent e){
      String curr = undoStack.pop();
      if(undoStack.isEmpty()) {
        undo.setEnabled(false);
        return;
      }
      String tmp = undoStack.pop();
      loadModelToNBO(tmp);
      redoStack.push(curr);
      if(redoStack.size()>MAX_HISTORY)
        redoStack.removeElementAt(MAX_HISTORY);
      appendOutputWithCaret("Undo", 'i');
    }
  };
  
  protected void clearSelected() {
    for(int i=0; i < editMode; i++)
      atomNumBox[i].setText("");
    if(currVal != null)
      currVal.setText("");
    if(valLab != null)
      valLab.setText(" ");
    runScriptQueued("measure off;select remove {selected};refresh");
    selected = "";
    if(editValueTf!=null){
      editValueTf.setText("Select atoms...");
      editValueTf.setEnabled(false);
      jbEdit.setEnabled(false);
    }
  }

  /**
   * apply edit to model
   * @param item 
   */
  protected void editModel(String item) {
    SB sb = new SB();
    
    String cmd = item + " "
        + selected;
    String val = editValueTf.getText();
    if (editValueTf != null){
      if(editMode == ALTER || editMode == MUTATE)
        cmd += val;
      else if(item.equals("3chb")){
        if(!val.startsWith(":"))
          cmd += ":";
        cmd += val;
      }
    }
    if(editAction.charAt(0) == 'r')
      cmd += jcSymOps.getSelectedItem().toString();
    
    sb.append("CMD " + cmd);
    appendOutputWithCaret(cmd,'i');
    jbEdit.setEnabled(false);
    nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Getting model...");
    
  }
  
  protected void getSymmetry() {
    SB sb = new SB();
    sb.append("CMD symmetry");
    appendOutputWithCaret("Symmetry: ",'p');
    nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Getting model...");  
  }

  protected void getModel(JTextField textBox) {
    String model = textBox.getText();
    if (textBox.getText().equals(""))
      return;
    String s = "";
    fileHndlr.tfName.setText("");
    fileHndlr.tfExt.setText("");
    //clearInputFile();
    if (textBox.equals(jtJmolInput)) {
      loadModel = true;
      jtLineInput.setText("");
      s = "load $" + model;
      if (runScriptNow(s) == null) {
        if (runScriptNow("load :" + model) != null) {
          return;
        }
        appendOutputWithCaret("File not found",'i');
        loadModel = false;
        return;
      }
    } else if (textBox.equals(jtLineInput)) {
      SB sb = new SB();
      jtJmolInput.setText("");
      s = "show " + model;
      sb.append("CMD " + s);
      nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Getting model...");
    }
    textBox.setText(model);
    appendOutputWithCaret(s,'i');
  }
  
  /**
   * Loads model gotten from Pubchem/NIS databases
   * @param s - cfi formatted model string
   */

  protected void loadModelToNBO(String s) {
    //clearModel();
    String fName = nboService.serverDir + "/jmol_outfile.cfi";
    SB sb = new SB();
    vwr.writeTextFile(fName, s);
    sb.append("GLOBAL C_PATH " + nboService.serverDir + sep);
    sb.append("GLOBAL C_ESS c" + sep);
    sb.append("GLOBAL C_FNAME jmol_outfile" + sep);
    sb.append("GLOBAL C_IN_EXT cfi" + sep);
    sb.append("CMD use");  
    nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Getting model...");
    
  }

  /**
   * Loads model from file type after browse
   * @param path
   * @param fname
   * @param ext
   */
  protected void loadModelFromNBO(String path, String fname, String ext) {
    String ess = getEss(ext,true);
    SB sb = new SB();
    if(jtJmolInput != null){
      jtJmolInput.setText("");
      jtLineInput.setText("");
    }
    sb.append("GLOBAL C_PATH " + path + sep);
    sb.append("GLOBAL C_ESS " + ess + sep);
    sb.append("GLOBAL C_FNAME " + fname + sep);
    sb.append("GLOBAL C_IN_EXT " + ext.toLowerCase() + sep);
    sb.append("CMD use");
    appendOutputWithCaret("use." + ess + " " + fname + "." + ext,'i');
    nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Getting model...");
    
  }

  protected void saveModel(String path, String fname, String ext) {
    String ess = getEss(ext,false);
    SB sb = new SB();
    sb.append("GLOBAL C_PATH " + path + sep);
    sb.append("GLOBAL C_ESS " + ess + sep);
    sb.append("GLOBAL C_FNAME " + fname + sep);
    sb.append("GLOBAL C_OUT_EXT " + ext + sep);
    sb.append("CMD save");  
    nboService.rawCmdNew("m", sb, NBOService.MODE_MODEL, null, "Saving model...");
    appendOutputWithCaret("save." + ess + " " + fname,'i');
    appendOutputWithCaret("--Model Saved--<br>"+path+"\\"+fname+"."+ext,'b');
  }

  /**
   * @param ext - extenstion to convert
   * @param use - true if use, false if saving
   * @return ess code used internally by NBOServie
   */
  private String getEss(String ext, boolean use) {
    ext = ext.toLowerCase();
    if ((ext.equals("gau") || ext.equals("g09") || ext.equals("com")) && !use){
      if(jComboSave.getSelectedItem().toString().contains("(C")){
          return ext.charAt(0) + "c";
      }else if(jComboSave.getSelectedItem().toString().contains("(z")){
        return ext.charAt(0) + "z";
      }
        else return "g";
    }else if (ext.equals("cfi") || ext.equals("vfi") || ext.equals("gau")
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

  protected void notifyPick_m(String atomno) {

    if(editMode == 0) 
      return;
    String[] tok = atomno.split(",");
    if(tok.length > 1){
      //Bond selection
      if(editMode == CLIP)
        clearSelected();
      String[] tok2 = tok[1].split(" ");
      String at1 = tok2[2].replaceAll("[\\D]", "");
      if(!selected.contains(" " + at1 + " "))
        notifyPick_m(at1);
      String at2 = tok2[5].replaceAll("[\\D]", "");
      if(!selected.contains(" " + at2 + " "))
        notifyPick_m(at2);
      return;
    }
    editValueTf.requestFocus();
    editValueTf.setText("");
    if(editMode < 3)
      editValueTf.setEnabled(true);
    String st = runScriptNow("print {*}[" + atomno + "].selected");
    if (st.contains("1.0")) {
      selected = selected.replace(atomno + " ", "");
      runScriptQueued("select remove {*}[" + atomno + "];measure off;");
      if(selected.split(" ").length > 1)
        runScriptQueued("measure " + selected);
      for(int i=0;i<atomNumBox.length;i++)
        if(i>=selected.split(" ").length)
          atomNumBox[i].setText("");
        else
          atomNumBox[i].setText(selected.split(" ")[i]);
      return;
    }
    int cnt = (selected.equals("")?1:selected.split(" ").length + 1);
    switch (editMode) {
    case ALTER:
      String script = "";
      if(cnt == 1){
        editValueTf.setText("");
        script = "print {*}["+atomno+"].elemno";
        editValueTf.setEnabled(true);
        valLab.setText("(Atomic number)");
        currVal.setText("Current Value: " + runScriptNow(script));
      }else if (cnt == 5) {
        clearSelected();
        valLab.setText("(Atomic number)");
        script = "print {*}["+atomno+"].elemno";

        currVal.setText("Current Value: " + runScriptNow(script));
        cnt=1;
      } else{
        if (cnt == 2){
          valLab.setText("(Bond length)");
          runScriptQueued("measure off;measure " + selected + " " + atomno
              + "\"2:%0.4VALUE //A\"" + ";measure " + selected + " " + atomno
              + "\"2:%0.4VALUE //A\"");
        }
        else{
          if(cnt == 3) valLab.setText("(Valence angle)");
          else valLab.setText("(Dihedral angle)");
          runScriptQueued("measure off;measure " + selected + " " + atomno);
          
        }
        script = "print measure({*}[";
        for(String x:selected.split(" "))
          script += x +"],{*}[";
        script += atomno+"])";
        String s = runScriptNow(script).split("\\s+")[1];
        double val = Double.parseDouble(s);
        val = NBOService.round(val, 2);
        currVal.setText("Current Value: " + val);
      }
      break;
    case CLIP:
      if (cnt == 2){
        jbEdit.setEnabled(true);
        if(editValueTf.isVisible())
          editValueTf.requestFocus();
        else
          atomNumBox[1].requestFocus();
      }
      else if (cnt == 3) {
        clearSelected();
        cnt = 1;
      }
      break;
    case MUTATE:
      if (cnt == 2) {
        clearSelected();
        cnt = 1;
      }
      if (cnt == 1) {
        if (editAction.charAt(0) == 'r'){
          jcSymOps.removeAllItems();
          jcSymOps.setEnabled(true);
          int atomInd = Integer.parseInt(atomno)-1;
          int val = vwr.ms.at[atomInd].getValence();
          jbEdit.setEnabled(true);
          switch(val){
          case 4:
            for(String x : new String[]{"td","c3vi","c4v"})
              jcSymOps.addItem(x);
            break;
          case 5:
            for(String x : new String[]{"c4vo","c4vi"})
              jcSymOps.addItem(x);
            break;
          case 6:
            for(String x : new String[]{"c3vo","c3vi","c5vo","c5vi"})
              jcSymOps.addItem(x);
            break;
          default:
            jcSymOps.addItem("<Select Transition Metal>");
            jcSymOps.setEnabled(false);
            jbEdit.setEnabled(false);
          }
        }
        editValueTf.setEnabled(true);
        editValueTf.setText("");
        editValueTf.requestFocus();
      } 
    }

    selected += atomno + " ";
    
    runScriptQueued("select add {*}[" + atomno + "]");
    
    atomNumBox[cnt-1].setText("  " + atomno);
  
  }
  
  protected void getValue(){
    String script = "";
    String [] ats = selected.split(" ");
    int cnt = ats.length;
    if(cnt == 1){
      valLab.setText("(Atomic number)");
      editValueTf.setText("");
      editValueTf.setEnabled(true);
      script = "print {*}["+selected+"].elemno";
      if(currVal != null)
        currVal.setText("Current Value: " + runScriptNow(script));
    } else{
      if (cnt == 2){
        valLab.setText("(Bond length)");
        runScriptQueued("measure off;measure " + selected
            + "\"2:%0.4VALUE //A\"" + ";measure " + selected
            + "\"2:%0.4VALUE //A\"");
      }
      else{
        runScriptQueued("measure off;measure " + selected + " ");
        if(cnt == 3) valLab.setText("(Valence angle)");
        else if(cnt == 4) valLab.setText("(Dihedral angle)");
      }
      script = "print measure({*}[";
      for(int i = 0; i<ats.length-1; i++)
        script += ats[i] +"],{*}[";
      script += ats[ats.length-1]+"]);";
      String s = runScriptNow(script);
      if(s!=null){
        if(s.split("\\s+").length>1)
        currVal.setText("Current Value: " + s.split("\\s+")[1]);
      }
    }
  }
  
  protected void notifyLoad_m() {
    if (loadModel) {
      loadModel = false;
      runScriptNow("select within(model,visible)");
      loadModelToNBO(runScriptNow("print data({selected},'cfi')"));
      
      return;
    }
    clearSelected();

    showAtomNums(false);
    
    for(Component c:panel.getComponents())
      c.setVisible(true);
    editBox.setVisible(true);
    
    runScriptNow("select within(model,visible)");
    String fileContents = evaluateJmolString("data({selected},'cfi')");
    if(vwr.ms.ac > 0)
      if(fileContents!=null){
        undoStack.push(fileContents);
        if(undoStack.size() > MAX_HISTORY)
          undoStack.removeElementAt(0);
      }
    //TODO
//    if(moveTo != null)
//      runScriptQueued(moveTo);
//    moveTo = null;
    if(undoStack.size()>1) 
      undo.setEnabled(true);
    else 
      undo.setEnabled(false);
    if(!redoStack.isEmpty()) 
      redo.setEnabled(true);
    else 
      redo.setEnabled(false);
    runScriptQueued("select remove{*}; select on");
  }
 
  protected void showConfirmationDialog(String st, File newFile, String ext) {
    int i = JOptionPane.showConfirmDialog(null, st, "Warning", JOptionPane.YES_NO_OPTION);
    if(i==JOptionPane.YES_OPTION)
          saveModel(newFile.getParent(), FileHndlr.getJobStem(newFile), ext);
    
  }

}
