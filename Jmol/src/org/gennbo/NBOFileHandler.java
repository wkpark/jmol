package org.gennbo;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

/**
 * Builds the input file box found in all 4 modules JPanel containing file input
 * box
 */
class NBOFileHandler extends JPanel {

  protected static final String sep = System.getProperty("line.separator");

  protected static final String EXTENSIONS = "31;32;33;34;35;36;37;38;39;40;41;42;46;nbo";
  protected static final String[] EXT_ARRAY = PT.split(EXTENSIONS, ";");
  protected JTextField tfDir, tfName, tfExt;
  protected File inputFile;
  protected String fileDir, jobStem;
  protected String useExt;
  protected JButton browse;
  protected NBODialog dialog;
  protected boolean canReRun;

  protected final static int MODE_MODEL_USE = 1;
  protected final static int MODE_RUN = 2;
  protected final static int MODE_VIEW = 3;
  protected final static int MODE_SEARCH = 4;
  protected final static int MODE_MODEL_SAVE = 5;

  public NBOFileHandler(String name, String ext, final int mode, String useExt,
      NBODialog d) {
    dialog = d;
    canReRun = true;
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    fileDir = (props
        .getProperty("workingPath", System.getProperty("user.home")));
    this.useExt = useExt;
    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    (tfDir = new JTextField()).setPreferredSize(new Dimension(110, 20));
    tfDir.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        browsePressed();
      }
    });
    tfDir.setText(fileDir);
    add(tfDir, c);
    c.gridx = 1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(100, 20));
    tfName.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        //        if(mode == MODEL)
        //          showWorkpathDialogM(null,null);
        browsePressed();
      }
    });
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
    tfExt.setText(ext);
    tfExt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        browsePressed();
      }
    });
    if (mode != MODE_VIEW && mode != MODE_SEARCH) {
      add(tfExt, c);
      c.gridy = 1;
      add(new JLabel("  ext"), c);
    }
    c.gridx = 3;
    c.gridy = 0;
    c.gridheight = 2;
    browse = new JButton(mode == MODE_MODEL_SAVE ? "Save" : "Browse");
    browse.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        browsePressed();
      }
    });
    add(browse, c);
    jobStem = name;
    setInput(fileDir, name, ext);
  }

  protected boolean browsePressed() {
    if (dialog.nboService.isWorking
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
    }
    myChooser.setSelectedFile(new File(fileDir));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION)
      return loadSelectedFile(myChooser.getSelectedFile());
    return true;
  }

  private boolean loadSelectedFile(File selectedFile) {
    dialog.nboService.restart();
    inputFile = selectedFile;
    //if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
    //return false;
    if (dialog.dialogMode == NBODialog.DIALOG_MODEL)
      return true;
    if (!useExt.equals("47")) {
      jobStem = NBOFileHandler.getJobStem(inputFile);
      dialog.loadModelFromNBO(fileDir, jobStem, useExt);
      tfName.setText(jobStem);
      tfExt.setText(useExt);
      return true;
    }
    canReRun = true;
    setInputFile(inputFile);
    dialog.log("Job: " + jobStem, 'b');
    fileDir = inputFile.getParent();
    saveWorkHistory();
    return true;

  }

  /**
   * Sets up the input file, currently only support for .47/model input file
   * types
   * 
   * @param inputFile
   */
  protected void setInputFile(File inputFile) {
    clearInputFile();
    //dialog.nboService.restart();
    dialog.isOpenShell = false;
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = getJobStem(inputFile);
    setInput(inputFile.getParent(), jobStem, useExt);
    if (getExt(inputFile).equals("47")) {
      if ((inputFile.getParent() + "/").equals(dialog.nboService.serverDir)) {
        JOptionPane.showMessageDialog(this,
            "Select a directory that does not contain the NBOServe executable,"
                + "\nor select a new location for your NBOServe executable");
        return;
      }
      dialog.isJmolNBO = true;
      fileDir = inputFile.getParent();
      boolean canLoad = true;
      for (String x : EXT_ARRAY) {
        File f3 = newNBOFile(inputFile, x);
        if (!f3.exists() && (dialog.dialogMode != NBODialog.DIALOG_MODEL)) {
          if (dialog.dialogMode != NBODialog.DIALOG_RUN) {
            if (canReRun) {
              canReRun = false;
              dialog.runJob("PLOT", inputFile, "gennbo");
            } else {
              dialog.alertError("Error occurred during run");
            }
            return;
          }
          canLoad = false;
        }
      }
      if (canLoad) {
        dialog.loadFromHandler(new File(fileDir + "/" + jobStem + ".47"));
      } else if (dialog.dialogMode == NBODialog.DIALOG_RUN) {
        dialog.loadModelFromNBO(fileDir, jobStem, useExt);
        tfName.setText(jobStem);
        tfExt.setText("47");
      }
    }
  }

  /**
   * Read input parameters from .47 file
   * 
   * @return [ pre-keyword params, keywords, post-keyword params ]
   */
  protected String[] read47File() {
    String[] fileData = new String[] { "", "", "" };
    String nboKeywords = "";
    SB data = new SB();
    if (!readFileBuffered(inputFile, data))
      return fileData;
    String s = PT.trim(data.toString(), "\t\r\n ");
    String[] tokens = PT.split(s, "$END");
    if (tokens.length == 0)
      return fileData;
    SB preParams = new SB();
    SB postParams = new SB();
    SB params = preParams;
    // ignore everything after the last $END token
    for (int i = 0, n = tokens.length - 1; i < n; i++) {
      s = PT.trim(tokens[i], "\t\r\n ");
      if (params == preParams && s.indexOf("$NBO") >= 0) {
        String[] prePost = PT.split(s, "$NBO");
        if (prePost[0].length() > 0)
          params.append(s).append(sep);
        nboKeywords = prePost[1];
        params = postParams;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    dialog.logInfo("$NBO: " + nboKeywords, Logger.LEVEL_INFO);
    fileData[0] = preParams.toString();
    fileData[1] = nboKeywords;
    fileData[2] = postParams.toString();
    return fileData;
  }

  /**
   * Read a file reducing lines to
   * 
   * @param inputFile
   * @param data
   * @return true if successful; false if not
   */
  private boolean readFileBuffered(File inputFile, SB data) {
    try {
      BufferedReader b = null;
      b = new BufferedReader(new FileReader(inputFile));
      String line;
      while ((line = b.readLine()) != null)
        data.append(line + sep);
      b.close();
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  /**
   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds
   * in the jmol model
   * 
   * @return false if output contains error
   */
  protected boolean getChooseList() {
    File f = newNBOFile(inputFile, "nbo");
    if (!f.exists() || f.length() == 0)
      return false;
    String fdata = getFileData(f.toString());
    String[] tokens = PT.split(fdata, "\n $CHOOSE");
    int i = 1;
    if (tokens.length < 2) {
      dialog.logInfo("$CHOOSE record was not found in " + f,
          Logger.LEVEL_INFO);
      return false;
    }
    if (tokens[1].trim().startsWith("keylist")) {
      if (!tokens[1].contains("Structure accepted:")) {
        if (tokens[1].contains("missing END?")) {
          dialog.logInfo("Plot files not found. Have you used RUN yet?",
              Logger.LEVEL_ERROR);
          return false;
        } else if (tokens[2].contains("ignoring")) {
          dialog.alert("Ignoring $CHOOSE list");
        } else {
          return false;
        }
      }
      i = 3;
    }
    String data = tokens[i].substring(0, tokens[i].indexOf("$END"));

    dialog.setChooseList(data);

    return true;
  }

  protected String[] getRSList() {
    String data = getFileData(newNBOFile(inputFile, "nbo").toString());
    String[] toks = PT.split(data,
        "TOPO matrix for the leading resonance structure:\n");
    if (toks.length < 2) {
      if (toks[0].contains("0 candidate reference structure(s)"))
        dialog
            .alertError("0 candidate reference structure(s) calculated by SR LEWIS"
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

  public void clear() {
    tfName.setText("");
    tfExt.setText("");
  }

  //useful file manipulation methods /////////////////////////////

  protected static File newNBOFile(File f, String ext) {
    return new File(pathWithoutExtension(f.toString().replace('\\', '/')) + "."
        + ext);
  }

  protected static String pathWithoutExtension(String fname) {
    int pt = fname.lastIndexOf(".");
    return (pt < 0 ? fname : fname.substring(0, pt));
  }

  protected void clearInputFile() {
    //    if (jobStem.length() == 0)
    //      return;
    //    for (String ext : EXT_ARRAY)
    //      new File(dialog.nboService.serverDir + "/" + jobStem + "." + ext).delete();
    inputFile = null;
    if (dialog.dialogMode == 'v')
      dialog.resetView();
  }

  protected static String getJobStem(File inputFile) {
    String fname = inputFile.getName();
    return fname.substring(0, fname.lastIndexOf("."));
  }

  protected void saveWorkHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("workingPath", fileDir);
    JmolPanel.historyFile.addProperties(props);
  }

  protected void setInput(String dir, String name, String ext) {
    if (dir != null)
      tfDir.setText(dir);
    if (name != null)
      tfName.setText(name);
    if (tfExt != null)
      tfExt.setText(ext);
    if (dialog.saveFileHandler != null && this != dialog.saveFileHandler)
      dialog.saveFileHandler.setInput(dir, name,
          PT.isOneOf(ext, NBODialogConfig.OUTPUT_FILE_EXTENSIONS) ? ext : "");
    //System.out.println("-------" + f + n + "/" + e);
    if (dir != null && name != null && ext != null)
      inputFile = new File(dir + "\\" + name + "." + ext);
  }

  protected static String getExt(File newFile) {
    String fname = newFile.toString();
    return fname.substring(fname.lastIndexOf(".") + 1);
  }

  protected String getFileData(String fileName) {
    return dialog.vwr.getAsciiFileOrNull(fileName);
  }

  boolean writeToFile(String fileName, String s) {
    String ret = dialog.vwr.writeTextFile(fileName, s);
    return (ret != null && ret.startsWith("OK"));
  }

}
