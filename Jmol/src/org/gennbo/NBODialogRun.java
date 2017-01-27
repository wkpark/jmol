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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javajs.util.JSJSONParser;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.jmol.util.Logger;

abstract class NBODialogRun extends NBODialogModel {
  protected NBODialogRun(JFrame f) {
    super(f);
  }

  protected static boolean ALLOW_SELECT_ALL = false;

  protected static final String ARCHIVE_DIR = "http://nbo6.chem.wisc.edu/jmol_nborxiv/";

  protected static final String RUN_EXTENSIONS = "47;gau;gms";

  protected static final String[] keywordList = {
      "CMO: Bonding character of canonical MO's",
      "DIPOLE: Dipole moment analysis",
      "NBBP: Natural bond-bond polarizability indices",
      "NBCP: Natural bond critical point analysis",
      "NCE: Natural coulomb electrostatics analysis",
      "NCU: Natural cluster unit analysis",
      "NRT: Natural resonance theory analysis",
      "PLOT: Write files for orbital plotting",
      "STERIC: Natural steric analysis" };

  protected Box editBox;

  protected JRadioButton rbLocal;
  protected JRadioButton[] keywordButtons;
  protected JButton btnRun;
  protected JTextField tfJobName;

  private String[] fileData;

  protected String nboKeywords;

  protected JPanel buildRunPanel() {
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    //ESS////////////////////////////////////////
    panel.add(createTitleBox(" Select Job ", new HelpBtn("run_job_help.htm")));
    Box inputBox = createBorderBox(true);
    panel.add(inputBox);

    //INPUT/////////////////////////
    if (inputFileHandler == null) {
      inputFileHandler = newNBOFileHandler("", "47", NBOFileHandler.MODE_RUN,
          "47");
    } else {
      inputFileHandler = newNBOFileHandler(inputFileHandler.jobStem,
          inputFileHandler.tfExt.getText(), NBOFileHandler.MODE_RUN, "47");
    }
    inputFileHandler.setBrowseEnabled(false);

    inputBox.add(createSourceBox());
    inputBox.add(inputFileHandler);
    inputBox.setMaximumSize(new Dimension(355, 80));
    //EDIT////////////////
    panel
        .add(
            createTitleBox(" Choose $NBO Keywords ", new HelpBtn(
                "run_keywords_help.htm"))).setVisible(false);
    editBox = createBorderBox(true);
    editBox.setMinimumSize(new Dimension(350, 400));
    tfJobName = new JTextField();
    editBox.setVisible(false);
    panel.add(editBox);
    //BOTTOM OPTIONS///////////////
    btnRun = new JButton("Run");
    btnRun.setVisible(false);
    btnRun.setEnabled(true);
    panel.add(btnRun).setFont(new Font("Arial", Font.PLAIN, 20));
    btnRun.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        runJob("", inputFileHandler.inputFile, "gennbo");
      }
    });
    String ext = inputFileHandler.tfExt.getText();

    if (ext.equals("47"))
      notifyLoad_r();

    return panel;
  }

  /**
   * set up the local/archive/webmo box
   * @return Box
   */
  private Box createSourceBox() {
    Box box = Box.createHorizontalBox();
    ButtonGroup bg = new ButtonGroup();
    rbLocal = new JRadioButton("Local");
    rbLocal.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        inputFileHandler.setBrowseEnabled(true);
      }
    });
    box.add(rbLocal);
    bg.add(rbLocal);
    JRadioButton btn = new JRadioButton("NBOrXiv");
    final NBODialogRun d = this;
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ArchiveViewer aView = new ArchiveViewer(d, ARCHIVE_DIR);
        aView.setVisible(true);
      }
    });
    box.add(btn);
    bg.add(btn);
    btn = new JRadioButton("WebMO");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String url = "http://www.webmo.net/demoserver/cgi-bin/webmo/jobmgr.cgi";
        try {
          Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e1) {
          alertError("Could not open WebMO");
        }
      }
    });
    box.add(btn);
    bg.add(btn);
    return box;
  }

  protected String cleanNBOKeylist(String params) {
    String[] tokens = PT.getTokens(PT.rep(PT.clean(params), "file=", "FILE="));
    String tmp = "";
    for (String s : tokens)
      if (s.length() > 0)
        if (s.toLowerCase().contains("file=") && tfJobName != null) {
          tfJobName.setText(s.substring(s.indexOf("=") + 1));
        } else {
          if (tmp.length() + s.length() - tmp.lastIndexOf(sep) >= 80)
            tmp += sep + " ";
          //sList.addElement(s);
          tmp += s.toUpperCase() + " ";
        }
    if (tfJobName != null && tfJobName.getText().equals(""))
      tfJobName.setText(inputFileHandler.jobStem);
    return tmp;
  }

  protected void setBonds(boolean alpha) {
    if (chooseList == null)
      return;
    SB tmp = (alpha ? chooseList.bonds : chooseList.bonds_b);
    if (tmp == null)
      return;
    String bonds = tmp.toString();
    if (!bonds.trim().equals("")) {
      vwr.ms.deleteAllBonds();
      for (String s : bonds.split("\n")) {
        String[] tokens = s.split(":");
        String key = tokens[0];
        String[] atoms = tokens[1].split(" ");
        int at1 = Integer.parseInt(atoms[0]);
        int at2 = Integer.parseInt(atoms[1]);
        int order = 0;
        short mag = 250;
        switch (key.charAt(0)) {
        case 'S':
          order = 1;
          break;
        case 'D':
          order = 2;
          break;
        case 'T':
          order = 3;
          mag = 150;
          break;
        case 'Q':
          order = 4;
          mag = 100;
          break;
        default:
          order = Integer.parseInt(key);
          mag = 100;
        }
        vwr.ms.bondAtoms(vwr.ms.at[at1 - 1], vwr.ms.at[at2 - 1], order, mag,
            vwr.ms.bsVisible, 0, true, true);
      }
    }
    if (nboView) {
      String s2 = runScriptNow("print {*}.bonds");
      runScriptNow("select " + s2 + ";color bonds lightgrey");
    }
  }

  //  /**
  //   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds in the jmol model
  //   * @param f 
  //   * @return false if output contains error
  //   */
  //  protected boolean getChooseList(File f){
  //    if(!f.exists()||f.length()==0)
  //      return false;
  //    String[] tokens = PT.split(nboService.getFileData(f.toString()), "$CHOOSE");
  //    int i = 1;
  //    if(tokens.length<2){
  //      showConfirmationDialog("An error occurred during run, view .nbo output?", f,"47");
  //      return false;
  //    }
  //    if(tokens[1].trim().startsWith("keylist")){
  //      if(!tokens[1].contains("Structure accepted:")){
  //        if(tokens[1].contains("missing END?")){
  //          showConfirmationDialog("Plot files not found. Run now with PLOT keyword?", f,"47");
  //          return false;
  //        }else if(tokens[2].contains("ignoring")){
  //          alertError("Ignoring $CHOOSE list"); 
  //        }else{
  //          return false;
  //        }
  //      }
  //      i = 3;
  //    }
  //    String data = tokens[i].substring(0,tokens[i].indexOf("$END"));
  //    setChooseList(data,true);
  //    return true;
  //  }


  /**
   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds
   * in the Jmol model
   * 
   * 
   * @return false if output contains error
   */
  protected boolean getChooseList() {
    File f = NBOFileHandler.newNBOFile(inputFileHandler.inputFile, "nbo");
    if (!f.exists() || f.length() == 0)
      return false;    
    String fdata = inputFileHandler.getFileData(f.toString());
    String[] tokens = PT.split(fdata, "\n $CHOOSE");
    int i = 1;
    if (tokens.length < 2) {
      logInfo("$CHOOSE record was not found in " + f,
          Logger.LEVEL_INFO);
      return false;
    }
    if (tokens[1].trim().startsWith("keylist")) {
      if (!tokens[1].contains("Structure accepted:")) {
        if (tokens[1].contains("missing END?")) {
          logInfo("Plot files not found. Have you used RUN yet?",
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
    setChooseList(tokens[i].substring(0, tokens[i].indexOf("$END")));
    return true;
  }
  
  protected void setChooseList(String data) {
    chooseList = new ChooseList();
    String[] tokens = PT.split(data, "END");
    int ind = 0;
    SB bonds = chooseList.bonds;
    SB bonds3c = chooseList.bonds3c;
    Hashtable<String, String> lonePairs = chooseList.lonePairs;
    if (data.trim().contains("ALPHA")) {
      isOpenShell = true;
      ind = 1;
    }

    for (String x : tokens) {
      String[] list = x.trim().split("\\s+");
      if (list[0].trim().equals("BETA")) {
        bonds = chooseList.bonds_b;
        bonds3c = chooseList.bonds3c_b;
        lonePairs = chooseList.lonePairs_b;
        ind = 1;
      }

      if (list[ind].trim().equals("LONE"))
        for (int j = 1 + ind; j < list.length; j += 2)
          lonePairs.put(list[j], list[j + 1]);

      else if (list[ind].trim().equals("BOND"))
        for (int j = 1 + ind; j < list.length; j += 3)
          bonds.append(list[j] + ":" + list[j + 1] + " " + list[j + 2] + "\n");

      else if (list[ind].equals("3C"))
        for (int j = 1 + ind; j < list.length; j += 4)
          bonds3c.append(list[j] + ":" + list[j + 1] + " " + list[j + 2] + " "
              + list[j + 3] + "\n");

      ind = 0;

    }
  }

  //  /**
  //   * opens contents of file with path f in new dialog window
  //   * 
  //   * @param f
  //   *        - absolute file path
  //   */
  //  protected void showNboOutput(String f) {
  //    String data = fileHndlr.getFileData(f);
  //    JDialog d = new JDialog();
  //    d.setLayout(new BorderLayout());
  //    JTextPane p = new JTextPane();
  //    JScrollPane sp = new JScrollPane();
  //    sp.getViewport().add(p);
  //    p.setEditable(false);
  //    p.setText(data);
  //    d.add(sp, BorderLayout.CENTER);
  //    centerDialog(d);
  //    d.setSize(new Dimension(500, 500));
  //    d.setVisible(true);
  //  }

  protected String getFileContents(String jobName) {
    String fileContents = fileData[0] + "$NBO\n " + "FILE=" + jobName + " "
        + nboKeywords + "  $END" + sep;
    return fileContents + fileData[2];
  }

  //  protected void essChanged(String item, DefaultComboBoxModel<String> editModel) {
  //    fileHndlr.clearInputFile();
  //    editBox.removeAll();
  //    editBox.add(Box.createRigidArea(new Dimension(430, 230)));
  //    //appendOutputWithCaret("ESS changed:\n  " + action.getSelectedItem().toString());
  //    if (item.equals("GenNBO")) {
  //      fileHndlr.tfExt.setText("47");
  //      fileHndlr.useExt = "47";
  //      editModel.removeElement("Gaussian Input File");
  //      return;
  //    } else if (item.equals("GO9")) {
  //      fileHndlr.tfExt.setText("gau");
  //      fileHndlr.useExt = "gau";
  //      //editModel.addElement("Gaussian Input File");
  //      return;
  //    }
  //  }

  //  protected void editOpChanged(String item) {
  //    editBox.removeAll();
  //    if (item.equals("-type-")) {
  //      editBox.add(Box.createRigidArea(new Dimension(320, 295)));
  //    } else if (item.startsWith("$NBO")) {
  //      addNBOKeylist();
  //    } else if (item.startsWith("Gaussian")) {
  //      JScrollPane p = new JScrollPane();
  //      //GaussianDialog gau = new GaussianDialog(runFrame, vwr);
  //      //p.setViewportView(gau.getContentPane());
  //      editBox.add(p);
  //    }
  //    this.repaint();
  //    this.revalidate();
  //  }

  protected void addNBOKeylist() {

    if (inputFileHandler.inputFile != null) {
      editBox.removeAll();
      editBox.add(Box.createRigidArea(new Dimension(350, 0)));
      final DefaultListModel<String> sList = new DefaultListModel<String>();
      String tmp = "";
      for (String s : nboKeywords.split("\\s+"))
        if (s.length() > 0 && !s.toUpperCase().contains("FILE=")) {
          sList.addElement(s);
          tmp += s + " ";
        }
      nboKeywords = tmp;
      final JList<String> keywords = new JList<String>(sList);
      keywords.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2)
            removeListParams(keywords.getSelectedValuesList(), sList);
        }
      });
      //FILE=///////////
      Box box = Box.createHorizontalBox();
      box.add(new JLabel("Jobname ")).setFont(new Font("Arial", Font.BOLD, 16));
      box.add(tfJobName).setMaximumSize(new Dimension(100, 30));
      box.setAlignmentX(0.5f);
      Box box2 = Box.createVerticalBox();
      box2.add(box);
      JLabel lab = new JLabel("(Plot files will be created with this name)");
      lab.setAlignmentX(0.5f);
      box2.add(lab);
      editBox.add(box2);
      //NBOKEYLIST/////////
      box2 = Box.createHorizontalBox();
      box2.add(new JLabel("Keywords:  ")).setFont(
          new Font("Arial", Font.BOLD, 16));
      final JPanel menu = menuNboKeywords();
      ButtonGroup bg = new ButtonGroup();
      JRadioButton btn = new JRadioButton("Menu Select");
      bg.add(btn);
      box2.add(btn);
      final JPanel manEdit = addManualEditor();
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          manEdit.setVisible(false);
          menu.setVisible(true);
        }
      });
      btn.doClick();
      btn = new JRadioButton("Text Editor");
      bg.add(btn);
      box2.add(btn);
      btn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          menu.setVisible(false);
          manEdit.setVisible(true);
        }
      });
      editBox.add(box2);
      manEdit.setAlignmentX(0.5f);
      editBox.add(manEdit);
      editBox.add(menu);
    }
  }

  private JPanel menuNboKeywords() {
    JPanel menu = new JPanel();
    menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
    menu.setBorder(BorderFactory.createLoweredBevelBorder());
    keywordButtons = new JRadioButton[keywordList.length];
    for (int i = 0; i < keywordButtons.length; i++) {
      keywordButtons[i] = new JRadioButton(keywordList[i]);
      if (nboKeywords.contains(keywordList[i].split(":")[0]))
        keywordButtons[i].setSelected(true);
      final int op = i;
      keywordButtons[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          String key = keywordList[op].split(":")[0];
          if (keywordButtons[op].isSelected()) {
            nboKeywords += key + " ";
            log("Keyword added: " + key, 'i');
          } else {
            nboKeywords = nboKeywords.replaceAll(key + " ", "");
            log("Keyword removed: " + key, 'i');
          }
        }
      });
      keywordButtons[i].setAlignmentX(0.0f);
      menu.add(keywordButtons[i]);
    }
    JLabel lab2 = new JLabel("(Select one or more)");
    menu.add(lab2);
    menu.setAlignmentX(0.5f);
    menu.setMinimumSize(new Dimension(300, 250));
    menu.setVisible(false);
    return menu;
  }

  private JPanel addManualEditor() {
    JScrollPane sp = new JScrollPane();
    JPanel d = new JPanel(new BorderLayout());
    final JTextPane p = new JTextPane();
    sp.setPreferredSize(new Dimension(200, 200));
    sp.getViewport().add(p);
    d.add(sp, BorderLayout.CENTER);
    p.setText("$NBO\nFile=" + tfJobName.getText() + " " + nboKeywords
        + "\n$END");
    p.setCaretPosition(7);
    JButton btn = new JButton("Save Changes");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String s = p.getText();
        String tmp = s.replace("$NBO", "").replace("$END", "");
        nboKeywords = "";
        for (String x : tmp.split("\\s+")) {
          x = x.trim();
          System.out.println(x);
          if (x.length() == 0)
            continue;
          if (x.indexOf("=") < 0) {
            nboKeywords += x + " ";
          } else {
            tfJobName.setText(x.substring(x.indexOf("=") + 1));
          }
        }
        addNBOKeylist();
        editBox.repaint();
        editBox.revalidate();
      }
    });
    d.add(btn, BorderLayout.SOUTH);
    d.setVisible(false);
    d.setMaximumSize(new Dimension(300, 200));
    return d;
  }

  protected void removeListParams(List<String> list,
                                  DefaultListModel<String> listModel) {
    log("Keyword(s) removed:", 'p');
    for (String x : list) {
      listModel.removeElement(x);
      if (nboKeywords.toUpperCase().contains(x.toUpperCase())) {
        nboKeywords = nboKeywords.substring(0,
            nboKeywords.indexOf(x.toUpperCase()))
            + nboKeywords.substring(nboKeywords.indexOf(x.toUpperCase())
                + x.length());
        log("  " + x, 'i');
      }
    }
  }

  protected void runJob(String keywords, final File inputFile, String ess) {

    if (fileData == null) {
      fileData = inputFileHandler.read47File();
      nboKeywords = fileData[1];
    }
    //Check the plot file names match job name, warn user otherwise
    inputFileHandler.jobStem = inputFileHandler.jobStem.trim();
    String jobName = (tfJobName == null ? inputFileHandler.jobStem
        : tfJobName.getText().trim());

    // BH Q: Would it be reasonable if the NO option is chosen to put that other job name in to the jobStem field, and also copy the .47 file to that? Or use that?
    // Or, would it be better to ask this question immediately upon file loading so that it doesn't come up, and make it so that
    // you always MUST have these two the same?

    if (!jobName.equals(inputFileHandler.jobStem)) {
      int i = JOptionPane
          .showConfirmDialog(
              null,
              "Warning! Plot files are being created with name \""
                  + jobName
                  + "\", which does not match your file name \""
                  + inputFileHandler.jobStem
                  + "\"\nDo you want to change that to \""
                  + inputFileHandler.jobStem
                  + "\" so that all files related to this job are under the same name, and View will work correctly?",
              "Warning", JOptionPane.YES_NO_OPTION);
      // BH adds setting of plotFileName
      if (i == JOptionPane.YES_OPTION)
        tfJobName.setText(jobName = inputFileHandler.jobStem);
    }

    for (String x : keywords.split(" ")) {
      if (!nboKeywords.contains(x + " ")) {
        nboKeywords += x + " ";
      }
    }

    if (!nboKeywords.contains("PLOT"))
      nboKeywords += "PLOT";

    if (inputFileHandler.useExt.equals("47"))
      if (!inputFileHandler.writeToFile(inputFile.getAbsolutePath(),
          getFileContents(jobName))) {
        logInfo("Could not create " + inputFile, Logger.LEVEL_ERROR);
        return;
      }
    SB sb = new SB();
    sb.append("GLOBAL C_PATH " + inputFile.getParent() + sep);
    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    sb.append("GLOBAL C_ESS " + ess.toLowerCase() + sep);

    sb.append("GLOBAL C_LABEL_1 FILE="
        + (jobName.equals("") ? inputFileHandler.jobStem : jobName));

    nboService.rawCmdNew("r", sb, NBOService.MODE_RUN, null,
        "Running GenNBO...");

  }

  protected void notifyLoad_r() {
    if (vwr.ms.ac == 0)
      return;

    fileData = inputFileHandler.read47File();
    nboKeywords = cleanNBOKeylist(fileData[1]);
    if (inputFileHandler.useExt.equals("47"))
      getChooseList();
    showAtomNums(true);
    setBonds(true);
    addNBOKeylist();
    for (Component c : panel.getComponents())
      c.setVisible(true);
    editBox.getParent().setVisible(true);
    editBox.setVisible(true);
    repaint();
    revalidate();
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

  class ArchiveViewer extends JDialog implements ActionListener {
    private JScrollPane archivePanel;
    private JButton selectAll, download;
    private JCheckBox[] jcLinks;
    private JTextField tfPath;
    private String baseDir;

    public ArchiveViewer(NBODialogRun d, String url) {
      super(d, "NBO Archive Files");
      GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
          .getDefaultScreenDevice();
      int width = gd.getDisplayMode().getWidth() / 2 - 250;
      int height = gd.getDisplayMode().getHeight() / 2 - 120;
      setLocation(width, height);
      setSize(new Dimension(500, 240));
      setLayout(new BorderLayout());
      setResizable(false);
      archivePanel = new JScrollPane();

      archivePanel.setBorder(BorderFactory.createLineBorder(Color.black));
      add(archivePanel, BorderLayout.CENTER);
      String[] links = getLinks(url);
      setLinks(links, null);

      Box bottom = Box.createHorizontalBox();
      tfPath = new JTextField(d.inputFileHandler.fileDir);
      bottom.add(new JLabel("  Download to: "));
      bottom.add(tfPath);
      if (ALLOW_SELECT_ALL) {
        selectAll = new JButton("Select All");
        selectAll.addActionListener(this);
        bottom.add(selectAll);
      }
      download = new JButton("Download");
      download.addActionListener(this);
      bottom.add(download);
      add(bottom, BorderLayout.SOUTH);
    }

    /**
     * look for a file "47files" in the directory, which should be a simple list
     * of files.
     * 
     * @author Bob Hanson
     * @param baseDir
     * @return array of fully-elaborated file names
     */
    @SuppressWarnings("unchecked")
    private String[] getLinks(String baseDir) {
      if (!baseDir.endsWith("/"))
        baseDir += "/";
      this.baseDir = baseDir;
      String fileList = inputFileHandler.getFileData(baseDir + "47files.txt");
      String html;
      String sep;
      if (fileList == null) {
        // presumes a raw directory listing from Apache
        html = inputFileHandler.getFileData(baseDir);
        sep = "<a";
      } else if (fileList.indexOf("{") == 0 || fileList.indexOf("[") == 0) {
        Map<String, Object> map = new JSJSONParser().parseMap(fileList, true);
        ArrayList<String> list = (map == null ? null : (ArrayList<String>) map
            .get("47files"));
        if (list == null || list.size() == 0)
          return new String[0];
        String ext = (list.get(0).indexOf(".47") >= 0 ? "" : ".47");
        String[] a = list.toArray(new String[0]);
        for (int i = 0; i < list.size(); i++)
          a[i] = baseDir + list.get(i) + ext;
        return a;
      } else {
        html = PT.rep(fileList, "\r", "");
        sep = "\n";
      }
      ArrayList<String> files = new ArrayList<String>();
      String[] toks = html.split(sep);
      for (int i = 1; i < toks.length; i++) {
        String file = PT.getQuotedAttribute(toks[i], "href");
        if (file != null && file.endsWith(".47"))
          files.add(file);
      }
      return files.toArray(new String[0]);
    }

    private void setLinks(String[] links, String startsWith) {
      jcLinks = new JCheckBox[links.length];
      JPanel filePanel = new JPanel(new FlowLayout());
      if (startsWith == null)
        startsWith = "";
      ButtonGroup bg = (ALLOW_SELECT_ALL ? null : new ButtonGroup());
      for (int i = 0; i < links.length; i += 6) {
        Box box = Box.createVerticalBox();
        for (int j = 0; j < 6; j++) {
          if (i + j >= jcLinks.length)
            break;
          jcLinks[i + j] = new JCheckBox(links[i + j]);
          if (bg != null)
            bg.add(jcLinks[i + j]);
          jcLinks[i + j].setBackground(Color.white);
          box.add(jcLinks[i + j]);
        }
        filePanel.add(box);
      }
      filePanel.setBackground(Color.white);
      archivePanel.getViewport().add(filePanel);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == selectAll) {
        boolean didCheck = false;
        for (int i = 0; i < jcLinks.length; i++) {
          if (!jcLinks[i].isSelected()) {
            didCheck = true;
            jcLinks[i].setSelected(true);
          }
        }
        if (!didCheck) {
          for (int i = 0; i < jcLinks.length; i++) {
            jcLinks[i].setSelected(false);
          }
        }
        repaint();
      } else if (e.getSource() == download) {
        downloadFiles();
      }
    }

    public void downloadFiles() {
      File f = null;
      logInfo("saving to " + tfPath.getText().trim(), Logger.LEVEL_INFO);

      int n = 0;
      for (int i = 0; i < jcLinks.length; i++) {
        if (jcLinks[i].isSelected()) {
          String path = tfPath.getText().trim();
          if (path.endsWith("/") || path.endsWith("\\"))
            path += jcLinks[i].getText();
          else
            path += "/" + jcLinks[i].getText();
          f = new File(path);
          if (f.exists()) {
            int j = JOptionPane.showConfirmDialog(null,
                "File " + f.getAbsolutePath()
                    + " already exists, do you want to overwrite contents?",
                "Warning", JOptionPane.YES_NO_OPTION);
            if (j == JOptionPane.NO_OPTION)
              return;
          }
          String s = baseDir + jcLinks[i].getText();
          try {
            String fileData = vwr.getAsciiFileOrNull(s);
            if (fileData == null) {
              logInfo("Error reading " + s, Logger.LEVEL_ERROR);
              break;
            }
            if (inputFileHandler.writeToFile(path, fileData)) {
              logInfo(f.getName() + " (" + fileData.length() + " bytes)",
                  Logger.LEVEL_INFO);
              n++;
            } else {
              logInfo("Error writing to " + f, Logger.LEVEL_ERROR);
              break;
            }
          } catch (Throwable e) {
            alertError("Error reading " + s + ": " + e);
            break;
          }
        }
      }
      logInfo("saved " + n + "file" + (n == 1 ? "" : "s"), Logger.LEVEL_INFO);
      if (f == null)
        return;
      inputFileHandler.setInputFile(f);
      rbLocal.doClick();
      setVisible(false);
      dispose();
    }
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

    public ChooseList() {
      lv = new Hashtable<String, String>();
      lv_b = new Hashtable<String, String>();
      lonePairs = new Hashtable<String, String>();
      lonePairs_b = new Hashtable<String, String>();
      bonds = new SB();
      bonds_b = new SB();
      bonds3c = new SB();
      bonds3c_b = new SB();

    }
  }

}
