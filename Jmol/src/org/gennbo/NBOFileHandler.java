package org.gennbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.adapter.readers.quantum.NBOParser;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;

/**
 * Builds the input file box found in all 4 modules JPanel containing file input
 * box
 */
class NBOFileHandler extends JPanel {

  protected static final String sep = System.getProperty("line.separator");

  protected static final String EXTENSIONS = "31;32;33;34;35;36;37;38;39;40;41;42;46;nbo";
  protected static final String[] EXT_ARRAY = PT.split(EXTENSIONS, ";");
  
  protected File inputFile;


  protected JTextField tfDir, tfName, tfExt;
  private JButton btnBrowse;
  
  protected String fileDir, jobStem;
  protected String useExt;
  protected NBODialog dialog;
  protected boolean canReRun;
  protected boolean isOpenShell;

  private Lst<Object> structureList;

  protected final static int MODE_MODEL_USE = 1;
  protected final static int MODE_RUN = 2;
  protected final static int MODE_VIEW = 3;
  protected final static int MODE_SEARCH = 4;
  protected final static int MODE_MODEL_SAVE = 5;

  
  public NBOFileHandler(String name, String ext, final int mode, String useExt,
      NBODialog dialog) {
    this.dialog = dialog;
    canReRun = true;
    fileDir = dialog.getWorkingPath();
    this.useExt = useExt;
    setLayout(new GridBagLayout());
    setMaximumSize(new Dimension(350, 40));
    setPreferredSize(new Dimension(350, 40));
    setMinimumSize(new Dimension(350, 40));
    GridBagConstraints c = new GridBagConstraints();
    boolean canEditTextFields = (mode == MODE_MODEL_SAVE || mode == MODE_MODEL_USE);

    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    (tfDir = new JTextField()).setPreferredSize(new Dimension(110, 20));
    tfDir.setEditable(canEditTextFields);
    //    tfDir.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //
    //        browsePressed();
    //      }
    //    });
    tfDir.setText(fileDir);
    add(tfDir, c);
    c.gridx = 1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(120, 20));
    tfName.setEditable(canEditTextFields);
    //    tfName.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        //        if(mode == MODEL)
    //        //          showWorkpathDialogM(null,null);
    //        browsePressed();
    //      }
    //    });
    tfName.setText(name);
    add(tfName, c);
    c.gridx = 0;
    c.gridy = 1;
    add(new JLabel("         folder"), c);
    c.gridx = 1;
    add(new JLabel("          name"), c);
    c.gridx = 2;
    c.gridy = 0;
    (tfExt = new JTextField()).setPreferredSize(new Dimension(40, 20));
    tfExt.setEditable(canEditTextFields);
    tfExt.setText(ext);
    //    tfExt.addActionListener(new ActionListener() {
    //      @Override
    //      public void actionPerformed(ActionEvent e) {
    //        browsePressed();
    //      }
    //    });
    if (canEditTextFields) {
      add(tfExt, c);
      c.gridy = 1;
      add(new JLabel("  ext"), c);
    }
    c.gridx = 3;
    c.gridy = 0;
    c.gridheight = 2;
    btnBrowse = new JButton(mode == MODE_MODEL_SAVE ? "Save" : "Browse");
    btnBrowse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        doFileBrowsePressed();
      }
    });
    add(btnBrowse, c);
    jobStem = name;
    setInput(fileDir, name, ext);
  }

  protected boolean doFileBrowsePressed() {
    if (dialog.nboService.isWorking()
        && dialog.statusLab.getText().startsWith("Running")) {
      int i = JOptionPane.showConfirmDialog(dialog,
          "Warning, changing jobs while running GenNBO can effect output files."
              + "\nContinue anyway?");
      if (i == JOptionPane.NO_OPTION)
        return false;
    }
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
    myChooser.setFileHidingEnabled(true);
    String folder = tfDir.getText();
    String name = tfName.getText();
    if (!folder.equals("")) {
      if (!folder.contains(":"))
        folder = "C:/" + folder;
      fileDir = folder + "/" + (name.equals("") ? " " : name);
      if (name.length() > 0 && useExt.equals("47"))
        fileDir += ".47";
    }
    myChooser.setSelectedFile(new File(fileDir));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION)
      return loadSelectedFile(myChooser.getSelectedFile());
    return true;
  }

  protected boolean loadSelectedFile(File selectedFile) {
    dialog.nboService.restartIfNecessary();
    inputFile = selectedFile;
    clearStructureList();
    isOpenShell = false;
    //if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
    //return false;
    if (dialog.dialogMode == NBODialog.DIALOG_MODEL)
      return true;
    if (!useExt.equals("47")) {
      jobStem = NBOUtil.getJobStem(inputFile);
      dialog.modelPanel.loadModelFromNBO(fileDir, jobStem, useExt);
      tfName.setText(jobStem);
      tfExt.setText(useExt);
      return true;
    }
    canReRun = true;
    setInputFile(inputFile);
    dialog.runPanel.doLogJobName(jobStem);
    fileDir = inputFile.getParent();
    dialog.saveWorkingPath(fileDir.toString());
    return true;
  }

  protected void clearStructureList() {
    structureList = null;
  }

  /**
   * Sets up the input file, currently only support for .47/model input file
   * types
   * 
   * @param inputFile
   */
  protected void setInputFile(File inputFile) {
    dialog.logValue("Input file=" + inputFile);
    clearInputFile(false); // clear CURRENT input file's server directory
    isOpenShell = false;
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = NBOUtil.getJobStem(inputFile);
    if (dialog.modelOrigin == NBODialog.ORIGIN_NBO_ARCHIVE)
      clearInputFile(true);
    setInput(inputFile.getParent(), jobStem, useExt);
    if (!NBOUtil.getExt(inputFile).equals("47"))
      return;
    if (NBOUtil.fixPath(inputFile.getParent().toString()).equals(
        dialog.nboService.getServerPath(null))) {
      JOptionPane.showMessageDialog(this,
          "Select a directory that does not contain the NBOServe executable,"
              + "\nor select a new location for your NBOServe executable");
      return;
    }
    fileDir = inputFile.getParent();
    boolean canLoad = true;
    boolean isOK = true;
    String msg = "";
    if (dialog.dialogMode != NBODialog.DIALOG_MODEL) {
      setStructure(null, null, -1);
      if (structureList == null  || structureList.size() == 0) {
        msg = "problems getting a $CHOOSE list for " + inputFile;
        isOK = false;
      } else {
        for (String x : EXT_ARRAY) {
          File f3 = newNBOFileForExt(x);
          if (!f3.exists() || x.equals("36") && f3.length() == 0) {
            msg = "file " + f3 + " is missing or zero length";
            // BH: But this means all  || f3.length() == 0) {
            isOK = false;
            break;
          }
        }
      }
    }
    if (!isOK) {
      if (dialog.dialogMode != NBODialog.DIALOG_RUN) {
        if (canReRun) {
          canReRun = false;
          dialog.runPanel.doRunGenNBOJob("PLOT");
        } else {
          dialog.alertError("Error occurred during run: " + msg);
        }
        return;
      }
      canLoad = false;
    }
    if (canLoad) {
      dialog.loadOrSetBasis(new File(fileDir + "/" + jobStem + ".47"));
    } else if (dialog.dialogMode == NBODialog.DIALOG_RUN) {
      dialog.modelPanel.loadModelFromNBO(fileDir, jobStem, useExt);
      tfName.setText(jobStem);
      tfExt.setText("47");
    }
  }
  
  /**
   * Read input parameters from .47 file
   * 
   * @param doAll read the whole thing; else just for keywords (stops at $COORD) 
   * 
   * @return [ pre-keyword params, keywords, post-keyword params ]
   */
  protected String[] read47File(boolean doAll) {
    clearStructureList();
    String[] fileData = new String[] { "", "", "", "" };
    String nboKeywords = "";
    SB data = new SB();
    if (!NBOUtil.read47FileBuffered(inputFile, data, doAll))
      return fileData;
    String s = PT.trim(data.toString(), "\t\r\n ");
    String[] tokens = PT.split(s, "$END");
    if (tokens.length == 0)
      return fileData;
    SB preParams = new SB();
    SB postParams = new SB();
    SB params = preParams;
    // ignore everything after the last $END token
    for (int i = 0, n = tokens.length; i < n; i++) {
      s = PT.trim(tokens[i], "\t\r\n ");
      if (params == preParams && s.indexOf("$NBO") >= 0) {
        String[] prePost = PT.split(s, "$NBO");
        if (prePost[0].length() > 0)
          params.append(s).append(sep);
        nboKeywords = PT.trim(prePost[1], "\t\r\n ");
        params = postParams;
        if (!doAll)
          break;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    dialog.logInfo("$NBO: " + nboKeywords, Logger.LEVEL_INFO);
    fileData[0] = NBOUtil.fix47File(preParams.toString());
    fileData[1] = NBOUtil.removeNBOFileKeyword(nboKeywords, null);
    fileData[2] = postParams.toString();
    fileData[3] = nboKeywords;
    return fileData;
  }

  public void clear() {
    tfName.setText("");
    tfExt.setText("");
  }

  protected void clearInputFile(boolean andUserDir) {
    if (jobStem.length() == 0)
      return;
    for (String ext : EXT_ARRAY)
      try {
        new File(dialog.nboService.getServerPath(jobStem + "." + ext)).delete();
      } catch (Exception e) {
        // ignore
      }
    if (andUserDir)
      for (String ext : EXT_ARRAY)
        try {
          NBOUtil.newNBOFile(inputFile, ext).delete();
        } catch (Exception e) {
          // ignore
        }
    inputFile = null;
    if (dialog.dialogMode == NBODialog.DIALOG_VIEW)
      dialog.viewPanel.resetView();
  }

  protected void setInput(String dir, String name, String ext) {
    if (dir != null)
      tfDir.setText(dir);
    if (name != null)
      tfName.setText(name);
    if (tfExt != null)
      tfExt.setText(ext);
    if (dir != null && name != null && ext != null) {
      dialog.modelPanel.modelSetSaveParametersFromInput(this, dir, name, ext);
      inputFile = new File(dir + "\\" + name + "." + ext);
    }
  }

  protected String getFileData(String fileName) {
    return dialog.vwr.getAsciiFileOrNull(fileName);
  }

  boolean writeToFile(String fileName, String s) {
    String ret = dialog.vwr.writeTextFile(fileName, s);
    return (ret != null && ret.startsWith("OK"));
  }

  public void setBrowseEnabled(boolean b) {
    btnBrowse.setEnabled(b);
  }

  public String getInputFile(String name) {
    return getFileData(newNBOFileForExt(name).toString());
  }

  public File newNBOFileForExt(String filenum) {
    return NBOUtil.newNBOFile(inputFile, filenum);
  }

  public void copyAndSwitch47FileTo(String jobName) {
    String data = dialog.vwr.getAsciiFileOrNull(inputFile.getAbsolutePath());
    tfName.setText(jobName);
    setInput(tfDir.getText(), jobName, "47");
    if (data != null)
      this.writeToFile(inputFile.getAbsolutePath(), data);    
  }

  public String[] update47File(String jobName, String keywords, boolean doMerge) {
    if (!useExt.equals("47"))
      return null;
    String[] fileData = read47File(true);
    if (writeToFile(inputFile.getAbsolutePath(), fileData[0] + "$NBO\n "
        + "FILE=" + jobName + " " + keywords + "  $END" + sep + fileData[2])) {
      fileData[1] = keywords;
      fileData[3] = "FILE=" + jobName + " " + keywords; 
      dialog.runPanel.doLogJobName(jobName);
      dialog.runPanel.doLogKeywords(keywords);
      return fileData;
    }
    dialog.logInfo("Could not create " + inputFile, Logger.LEVEL_ERROR);
    return null;
  }


  /**
   * create the resonance structure list.
   * @param sb string buffer to write Jmol scripts to if desired
   * 
   * @param type
   *        nrtstra, nrtstrb, alpha, beta
   * @param index index into list of this type
   * @return the map for this structure
   */
  public String setStructure(SB sb, String type, int index) {
    if (structureList == null) {
      NBOParser nboParser = new NBOParser();
      structureList = nboParser.getAllStructures(getInputFile("nbo"));
      isOpenShell = nboParser.isOpenShell();
    }
    Map<String, Object> map = NBOParser.getStructureMap(structureList, type, index);
    boolean addCharge = false; // BH does not work. You cannot get charges just from 
    // counting bonds.
    return (map == null ? null : NBOParser.setStructure(sb, dialog.vwr, map, addCharge));
  }


}
