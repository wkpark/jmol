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
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Map;

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

/**
 * Builds the input file box found in all 4 modules JPanel containing file input
 * box
 */
class NBOFileHandler extends JPanel {

  protected static final String sep = System.getProperty("line.separator");

  protected static final String EXTENSIONS = "31;32;33;34;35;36;37;38;39;40;41;42;46;nbo";
  protected static final String[] EXT_ARRAY = PT.split(EXTENSIONS, ";");
  
  protected File inputFile;

  protected ChooseList chooseList;


  protected JTextField tfDir, tfName, tfExt;
  private JButton btnBrowse;
  
  protected String fileDir, jobStem;
  protected String useExt;
  protected NBODialog dialog;
  protected boolean canReRun;

  protected final static int MODE_MODEL_USE = 1;
  protected final static int MODE_RUN = 2;
  protected final static int MODE_VIEW = 3;
  protected final static int MODE_SEARCH = 4;
  protected final static int MODE_MODEL_SAVE = 5;

  protected int[][] resStructDef;
  protected Map<Integer, String> resStructList;

  
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
        browsePressed();
      }
    });
    add(btnBrowse, c);
    jobStem = name;
    setInput(fileDir, name, ext);
  }

  protected boolean browsePressed() {
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
    //if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
    //return false;
    if (dialog.dialogMode == NBODialogConfig.DIALOG_MODEL)
      return true;
    if (!useExt.equals("47")) {
      jobStem = getJobStem(inputFile);
      dialog.loadModelFromNBO(fileDir, jobStem, useExt);
      tfName.setText(jobStem);
      tfExt.setText(useExt);
      return true;
    }
    canReRun = true;
    setInputFile(inputFile);
    dialog.logJobName(jobStem);
    fileDir = inputFile.getParent();
    dialog.saveWorkingPath(fileDir.toString());
    return true;
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
    dialog.isOpenShell = false;
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = getJobStem(inputFile);
    if (dialog.modelOrigin == NBODialogConfig.ORIGIN_NBO_ARCHIVE)
      clearInputFile(true);
    setInput(inputFile.getParent(), jobStem, useExt);
    if (!getExt(inputFile).equals("47"))
      return;
    if (fixPath(inputFile.getParent().toString()).equals(
        dialog.nboService.getServerPath(null))) {
      JOptionPane.showMessageDialog(this,
          "Select a directory that does not contain the NBOServe executable,"
              + "\nor select a new location for your NBOServe executable");
      return;
    }
    dialog.isJmolNBO = true;
    fileDir = inputFile.getParent();
    boolean canLoad = true;
    boolean isOK = true;
    String msg = "";
    if (dialog.dialogMode != NBODialogConfig.DIALOG_MODEL) {
      if (!createChooseList(true)) {
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
      if (dialog.dialogMode != NBODialogConfig.DIALOG_RUN) {
        if (canReRun) {
          canReRun = false;
          dialog.runGenNBOJob("PLOT");
        } else {
          dialog.alertError("Error occurred during run: " + msg);
        }
        return;
      }
      canLoad = false;
    }
    if (canLoad) {
      dialog.loadOrSetBasis(new File(fileDir + "/" + jobStem + ".47"));
    } else if (dialog.dialogMode == NBODialogConfig.DIALOG_RUN) {
      dialog.loadModelFromNBO(fileDir, jobStem, useExt);
      tfName.setText(jobStem);
      tfExt.setText("47");
    }
  }

  /**
   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds
   * in the Jmol model
   * @param isCheckOnly 
   * 
   * 
   * @return false if output contains error
   */
  private boolean createChooseList(boolean isCheckOnly) {
    chooseList = null;
    dialog.isOpenShell = false;

    File f = newNBOFileForExt("nbo");
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
          System.out.println("Ignoring $CHOOSE list");
        } else {
          return false;
        }
      }
      i = 3;
    }
    //if (!isCheckOnly)
    chooseList = new ChooseList(tokens[i].substring(0, tokens[i].indexOf("$END")));
    dialog.isOpenShell = chooseList.isOpenShell;
    return true;
  }
  
  /**
   * change \ to /
   * 
   * @param path
   * @return fixed path
   */
  static String fixPath(String path) {
    return path.replace('\\',  '/');
  }

  /**
   * Read input parameters from .47 file
   * 
   * @return [ pre-keyword params, keywords, post-keyword params ]
   */
  protected String[] read47File() {
    String[] fileData = new String[] { "", "", "", "" };
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
    for (int i = 0, n = tokens.length; i < n; i++) {
      s = PT.trim(tokens[i], "\t\r\n ");
      if (params == preParams && s.indexOf("$NBO") >= 0) {
        String[] prePost = PT.split(s, "$NBO");
        if (prePost[0].length() > 0)
          params.append(s).append(sep);
        nboKeywords = PT.trim(prePost[1], "\t\r\n ");
        params = postParams;
        continue;
      }
      params.append(s).append(sep).append("$END").append(sep);
    }
    dialog.logInfo("$NBO: " + nboKeywords, Logger.LEVEL_INFO);
    fileData[0] = fix47File(preParams.toString());
    fileData[1] = removeFileKeyword(nboKeywords);
    fileData[2] = postParams.toString();
    fileData[3] = nboKeywords;
    return fileData;
  }

  private String removeFileKeyword(String nboKeywords) {
    String[] tokens = PT.getTokens(nboKeywords);
    nboKeywords = "";
    for (int i = tokens.length; --i >= 0;)
      if (tokens[i].toUpperCase().indexOf("FILE=") < 0)
        nboKeywords += " " + tokens[i];
    return nboKeywords.trim();
  }

  private String fix47File(String data) {
    return PT.rep(data, "FORMAT=PRECISE", ""); 
    
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

  public void clear() {
    tfName.setText("");
    tfExt.setText("");
  }

  //useful file manipulation methods /////////////////////////////

  protected static File newNBOFile(File f, String ext) {
    return new File(pathWithoutExtension(fixPath(f.toString())) + "." + ext);
  }

  protected static String pathWithoutExtension(String fname) {
    int pt = fname.lastIndexOf(".");
    return (pt < 0 ? fname : fname.substring(0, pt));
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
          newNBOFile(inputFile, ext).delete();
        } catch (Exception e) {
          // ignore
        }
    inputFile = null;
    if (dialog.dialogMode == NBODialogConfig.DIALOG_VIEW)
      dialog.resetView();
  }

  protected static String getJobStem(File inputFile) {
    String fname = inputFile.getName();
    return fname.substring(0, fname.lastIndexOf("."));
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
    if (dir != null && name != null && ext != null) {
      inputFile = new File(dir + "\\" + name + "." + ext);
    }
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

  public void setBrowseEnabled(boolean b) {
    btnBrowse.setEnabled(b);
  }

  public String getInputFile(String name) {
    return getFileData(newNBOFileForExt(name).toString());
  }

  public File newNBOFileForExt(String filenum) {
    return newNBOFile(inputFile, filenum);
  }

  public void copyAndSwitch47FileTo(String jobName) {
    String data = dialog.vwr.getAsciiFileOrNull(inputFile.getAbsolutePath());
    tfName.setText(jobName);
    setInput(tfDir.getText(), jobName, "47");
    if (data != null)
      this.writeToFile(inputFile.getAbsolutePath(), data);    
  }

  public String[] update47File(String jobName, String keywords) {
    if (!useExt.equals("47"))
      return null;
    String[] fileData = read47File();
    if (writeToFile(inputFile.getAbsolutePath(), fileData[0] + "$NBO\n "
        + "FILE=" + jobName + " " + keywords + "  $END" + sep + fileData[2])) {
      fileData[1] = keywords;
      fileData[3] = "FILE=" + jobName + " " + keywords; 
      dialog.logJobName(jobName);
      dialog.logKeywords(keywords);
      return fileData;
    }
    dialog.logInfo("Could not create " + inputFile, Logger.LEVEL_ERROR);
    return null;
  }

  /**
   * Structure for maintaining contents of $CHOOSE list
   */
  class ChooseList {

    protected Hashtable<String, String> lv;
    protected Hashtable<String, String> lv_b;
    protected Hashtable<String, String> lonePairs;
    protected Hashtable<String, String> lonePairs_b;
    protected SB bonds;
    protected SB bonds_b;
    protected SB bonds3c;
    protected SB bonds3c_b;
    protected boolean isOpenShell;

    public ChooseList(String data) {
      lv = new Hashtable<String, String>();
      lv_b = new Hashtable<String, String>();
      lonePairs = new Hashtable<String, String>();
      lonePairs_b = new Hashtable<String, String>();
      bonds = new SB();
      bonds_b = new SB();
      bonds3c = new SB();
      bonds3c_b = new SB();

      setData(data);
    }

    private void setData(String data) {
      String[] tokens = PT.split(data, "END");
      int ind = 0;
      SB bonds = this.bonds;
      SB bonds3c = this.bonds3c;
      Hashtable<String, String> lonePairs = this.lonePairs;
      if (data.trim().contains("ALPHA")) {
        isOpenShell = true;
        ind = 1;
      }

      for (String x : tokens) {
        String[] list = x.trim().split("\\s+");
        if (list[0].trim().equals("BETA")) {
          bonds = this.bonds_b;
          bonds3c = this.bonds3c_b;
          lonePairs = this.lonePairs_b;
          ind = 1;
        }

        if (list[ind].trim().equals("LONE"))
          for (int j = 1 + ind; j < list.length; j += 2)
            lonePairs.put(list[j], list[j + 1]);

        else if (list[ind].trim().equals("BOND"))
          for (int j = 1 + ind; j < list.length; j += 3)
            bonds
                .append(list[j] + ":" + list[j + 1] + " " + list[j + 2] + "\n");

        else if (list[ind].equals("3C"))
          for (int j = 1 + ind; j < list.length; j += 4)
            bonds3c.append(list[j] + ":" + list[j + 1] + " " + list[j + 2]
                + " " + list[j + 3] + "\n");

        ind = 0;

      }

    }
    
    /**
     * Takes two strings from .nbo file and parses RS list information toks[0] =
     * primary rs matrix toks[1] = change list using accountants notation; creates
     * fields int[][] resStructDef and Map<Integer, String> resStructList
     * 
     */
    protected void parseRSList() {
      
//      TOPO matrix for the leading resonance structure:
  //
//        Atom  1   2   3
//        ---- --- --- ---
//      1.  O   2   2   0
//      2.  C   2   0   2
//      3.  O   0   2   2
  //
//            Resonance
//       RS   Weight(%)                  Added(Removed)
//    ---------------------------------------------------------------------------
//       1*(3)  49.51
//       2*(2)  25.63   ( O  1- C  2),  C  2- O  3,  O  1, ( O  3)
//       3*(2)  24.45    O  1- C  2, ( C  2- O  3), ( O  1),  O  3
//       4       0.21   ( O  1- C  2), ( O  1- C  2),  C  2- O  3,  C  2- O  3,
//                       O  1,  O  1, ( O  3), ( O  3)
//       5       0.21    O  1- C  2,  O  1- C  2, ( C  2- O  3), ( C  2- O  3),
//                      ( O  1), ( O  1),  O  3,  O  3
//       6-11    0.00
//    ---------------------------------------------------------------------------
//             100.00   * Total *                [* = reference structure]
  //
      
      
      String data = getInputFile("nbo");
      String[] toks = PT.split(data,
          "TOPO matrix for the leading resonance structure:\n");
      if (toks.length < 2) {
        if (toks[0].contains("0 candidate reference structure(s)"))
          dialog.alertError("0 candidate reference structure(s) calculated by SR LEWIS"
              + " Candidate reference structure taken from NBO search");
        return;
      }
      String[] toks2 = PT
          .split(toks[1],
              "---------------------------------------------------------------------------");
      String[] rsList = new String[2];
      rsList[0] = toks2[0].substring(toks2[0].lastIndexOf('-'),
          toks2[0].indexOf("Res")).trim();
      rsList[0] = rsList[0].replace("-\n", "");
      rsList[1] = toks2[1];
      String[] tmp1 = rsList[0].split("\n");
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
        BufferedReader br = new BufferedReader(new StringReader(rsList[1]));
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
      }
    }

    public Map<String, String> getMap(boolean isAlpha, boolean isLonePair) {
      return (isLonePair ? (isAlpha ? lonePairs : lonePairs_b) : isAlpha ? lv
          : lv_b);
    }

    
  }


  public void parseRSList() {
    if (chooseList != null)
      chooseList.parseRSList();
  }


  public SB getChooseListBonds(boolean alpha) {
    return (chooseList == null ? null : alpha ? chooseList.bonds : chooseList.bonds_b);
  }

  public Map<String, String> getChooseListMap(boolean isAlpha, boolean isLonePair) {
    return (chooseList == null ? null : chooseList.getMap(isAlpha, isLonePair));

  }


}
