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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.JSJSONParser;
import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

  protected static final String[] RUN_KEYWORD_LIST = {
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

  protected String[] file47Data;

  protected String file47Keywords;

  protected JPanel buildRunPanel() {
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    getNewInputFileHandler(NBOFileHandler.MODE_RUN);
    inputFileHandler.setBrowseEnabled(false);

    panel.add(createTitleBox(" Select Job ", new HelpBtn("run_job_help.htm")));
    Box inputBox = createBorderBox(true);
    inputBox.add(createSourceBox());
    inputBox.add(inputFileHandler);
    inputBox.setMinimumSize(new Dimension(360, 80));
    inputBox.setPreferredSize(new Dimension(360,80));
    inputBox.setMaximumSize(new Dimension(360, 80));
    panel.add(inputBox);

    //EDIT////////////////
    panel.add(
        createTitleBox(" Choose $NBO Keywords ", new HelpBtn(
            "run_keywords_help.htm"))).setVisible(false);
    editBox = createBorderBox(true);
    editBox.setSize(new Dimension(350, 400));
    tfJobName = new JTextField();
    tfJobName.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        logJobName(null);
      }
    });
    tfJobName.addFocusListener(new FocusListener() {

      @Override
      public void focusGained(FocusEvent e) {
      }

      @Override
      public void focusLost(FocusEvent e) {
        logJobName(null);
      }});
    
    editBox.setVisible(false);
    panel.add(editBox);
    //BOTTOM OPTIONS///////////////
    
//    Box box = Box.createHorizontalBox();
//    box.setSize(new Dimension(250, 50));
//    box.setAlignmentX(0.5f);
    btnRun = new JButton("Run");
    btnRun.setFont(runButtonFont);
    btnRun.setVisible(false);
    btnRun.setEnabled(true);
    btnRun.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        runGenNBOJob("");
      }
    });

    //box.add(btnRun);
    panel.add(btnRun);
    
    if (inputFileHandler.tfExt.getText().equals("47"))
      notifyLoad_r();

    return panel;
  }

  /**
   * set up the local/archive/webmo box
   * 
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
        if (modelOrigin != ORIGIN_NBO_ARCHIVE)
          inputFileHandler.browsePressed();
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

  protected void addNBOKeylist() {
    if (inputFileHandler.inputFile == null)
      return;
    Box jobNameOuterBox = Box.createVerticalBox();
    jobNameOuterBox.setSize(new Dimension(250,75));

    Box selectBox = Box.createHorizontalBox(); 
    selectBox.setSize(new Dimension(250, 50));

    Box mainBox = Box.createVerticalBox(); 
    mainBox.setSize(new Dimension(250, 275));

    final JPanel mainMenuOptions = addMenuOption();
    mainBox.add(mainMenuOptions);

    final JPanel mainTextEditor = addTextOption();
    mainBox.add(mainTextEditor);
    


    editBox.removeAll();
    editBox.add(Box.createRigidArea(new Dimension(350, 0)));
    editBox.add(jobNameOuterBox);    
    editBox.add(selectBox);    
    editBox.add(mainBox);

    Box jobNameInnerBox = Box.createHorizontalBox();
    jobNameInnerBox.add(new JLabel("Jobname ")).setFont(nboFont);
    jobNameInnerBox.add(tfJobName).setMaximumSize(new Dimension(150, 30));
    jobNameInnerBox.setAlignmentX(0.5f);
    jobNameOuterBox.add(jobNameInnerBox);    
    JLabel lab = new JLabel("(Plot files will be created with this name)");
    lab.setAlignmentX(0.5f);
    jobNameOuterBox.add(lab);
    
    ButtonGroup bg = new ButtonGroup();
    JRadioButton btnMenuSelect = new JRadioButton("Menu Select");
    bg.add(btnMenuSelect);
    btnMenuSelect.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mainTextEditor.setVisible(false);
        mainMenuOptions.setVisible(true);
      }
    });    
    
    JRadioButton btnTextEditor = new JRadioButton("Text Editor");
    btnTextEditor.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mainMenuOptions.setVisible(false);
        setKeywordTextPane(getKeywordsFromButtons());
        mainTextEditor.setVisible(true);
      }
    });
    bg.add(btnTextEditor);
    
    selectBox.add(new JLabel("Keywords:  ")).setFont(nboFont);
    selectBox.add(btnMenuSelect);
    selectBox.add(btnTextEditor);

    mainTextEditor.setVisible(false);
    mainMenuOptions.setVisible(true);
    btnMenuSelect.doClick();
  }

  private JPanel addTextOption() {
    JPanel textPanel = new JPanel(new BorderLayout());
    textPanel.setPreferredSize(new Dimension(270, 240));
    textPanel.setMaximumSize(new Dimension(270, 240));
    textPanel.setAlignmentX(0.5f);
    
    keywordTextPane = new JTextPane();
    setKeywordTextPane(file47Keywords);
    
    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(keywordTextPane);
    textPanel.add(sp, BorderLayout.CENTER);
    
//    keywordTextPane.setCaretPosition(7);
    JButton saveBtn = new JButton("Save Changes");
    saveBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String s = keywordTextPane.getText();
        file47Keywords = "";
        String[] tokens = PT.getTokens(PT.rep(PT.rep(s, "$NBO", ""), "$END", "").trim());
        for (String x : tokens) {
          if (x.indexOf("=") < 0) {
            file47Keywords += x + " ";
          } else {
            tfJobName.setText(x.substring(x.indexOf("=") + 1));
          }
        }
        addNBOKeylist();
        editBox.repaint();
        editBox.revalidate();
      }
    });
    textPanel.add(saveBtn, BorderLayout.SOUTH);
    textPanel.setVisible(false);
    return textPanel;
  }

  private JPanel addMenuOption() {
    JPanel menuPanel = new JPanel();
    menuPanel.setPreferredSize(new Dimension(270, 240));
    menuPanel.setMaximumSize(new Dimension(270, 240));
    menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
    menuPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    keywordButtons = new JRadioButton[RUN_KEYWORD_LIST.length];
    for (int i = 0; i < keywordButtons.length; i++) {
      keywordButtons[i] = new JRadioButton(RUN_KEYWORD_LIST[i]);
      if (file47Keywords.contains(RUN_KEYWORD_LIST[i].split(":")[0]))
        keywordButtons[i].setSelected(true);
      keywordButtons[i].setAlignmentX(0.0f);
      menuPanel.add(keywordButtons[i]);
      keywordButtons[i].addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          logKeywords(null);
        }
        
      });
    }
    JLabel lab2 = new JLabel("(Select one or more)");
    menuPanel.add(lab2);
    menuPanel.setAlignmentX(0.5f);
    menuPanel.setVisible(true);
    return menuPanel;
  }

  /**
   * Clean parameters and remove all FILE=xxxx
   * @param params 
   * @param setJobNameTextField  
   * @return cleaned string, just CAPS, no FILE=xxxx
   */
  protected String cleanNBOKeylist(String params, boolean setJobNameTextField) {
    String[] tokens = PT.getTokens(PT.rep(PT.clean(params), "file=", "FILE="));
    String tmp = "";
    boolean haveJobName = false;
    setJobNameTextField &= (tfJobName != null);
    for (String s : tokens)
      if (s.length() > 0)
        if (s.contains("fILE=")) {
          if (!haveJobName) {
            if (setJobNameTextField)
              tfJobName.setText(s.substring(s.indexOf("=") + 1));
            haveJobName = true;
          }
        } else {
          if (tmp.length() + s.length() - tmp.lastIndexOf(sep) >= 80)
            tmp += sep + " ";
          tmp += s.toUpperCase() + " ";
        }
    if (setJobNameTextField && (tfJobName.getText().equals("") || !haveJobName))
      tfJobName.setText(inputFileHandler.jobStem);
    return tmp.trim();
  }

  protected void setBonds(boolean alpha) {
    try {
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
    } catch (Exception e) {
      System.out.println("Cannot create bonds: " + e.getMessage());
    }
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

  protected void logJobName(String name) {
    if (name == null)
      tfJobName.setText(name = tfJobName.getText().trim());
    logValue("Job: " + name);
  }

  protected void logKeywords(String keywords) {
    if (keywords == null)
      keywords = getKeywordsFromButtons();
    logValue("Keywords: " + keywords);    
  }

  JTextPane keywordTextPane;

  protected void setKeywordTextPane(String keywords) {
    keywordTextPane.setText(keywords);
  }

//  protected void removeListParams(List<String> list,
//                                  DefaultListModel<String> listModel) {
//    log("Keyword(s) removed:", 'p');
//    for (String x : list) {
//      listModel.removeElement(x);
//      if (file47Keywords.toUpperCase().contains(x.toUpperCase())) {
//        file47Keywords = file47Keywords.substring(0,
//            file47Keywords.indexOf(x.toUpperCase()))
//            + file47Keywords.substring(file47Keywords.indexOf(x.toUpperCase())
//                + x.length());
//        log("  " + x, 'i');
//      }
//    }
//  }

  /**
   * Open the current 47 file and parse its data into three sections: pre,
   * keywords, post;
   * 
   * @param andSetJobNameField
   *        set true to set the JobName text field as well
   * 
   * @return [pre, keywords, post]
   */
  protected String[] get47FileData(boolean andSetJobNameField) {
//    if (file47Data != null && !forceNew)
//      return file47Data;
    file47Data = inputFileHandler.read47File();
    file47Keywords = cleanNBOKeylist(file47Data[1], andSetJobNameField);
    return file47Data;
  }

  protected void notifyLoad_r() {
    if (vwr.ms.ac == 0)
      return;
    get47FileData(true);
    showAtomNums(true);
    setBonds(true);
    addNBOKeylist();
    for (Component c : panel.getComponents())
      c.setVisible(true);
    editBox.getParent().setVisible(true);
    editBox.setVisible(true);
    logKeywords(null);
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
        downloadNBOArchiveFile();
      }
    }

    /**
     * Get an NBO archive .47 file.
     * 
     */
    public void downloadNBOArchiveFile() {
      File f = null;
      String path = tfPath.getText().trim();
      logInfo("saving to " + path, Logger.LEVEL_INFO);

      int n = 0;
      for (int i = 0; i < jcLinks.length; i++) {
        if (!jcLinks[i].isSelected())
          continue;
        if (path.endsWith("/") || path.endsWith("\\"))
          path += jcLinks[i].getText();
        else
          path += "/" + jcLinks[i].getText();
        f = new File(path);
        if (f.exists()) {
          int j = JOptionPane
              .showConfirmDialog(
                  null,
                  "File "
                      + f.getAbsolutePath()
                      + " already exists, do you want to overwrite contents, along with its associated .nn and .nbo files?",
                  "Warning", JOptionPane.YES_NO_OPTION);
          if (j == JOptionPane.NO_OPTION)
            return;
        }
        String s = baseDir + jcLinks[i].getText();
        logCmd("retrieve " + s);

        try {
          String fileData = vwr.getAsciiFileOrNull(s);
          if (fileData == null) {
            logError("Error reading " + s);
            break;
          }
          if (inputFileHandler.writeToFile(path, fileData)) {
            logInfo(f.getName() + " (" + fileData.length() + " bytes)",
                Logger.LEVEL_INFO);
            n++;
          } else {
            logError("Error writing to " + f);
          }
        } catch (Throwable e) {
          alertError("Error reading " + s + ": " + e);
        }
        break;
      }
      if (f == null)
        return;
      modelOrigin = ORIGIN_NBO_ARCHIVE;
      inputFileHandler.setInputFile(f);
      modelOrigin = ORIGIN_NBO_ARCHIVE;
      rbLocal.doClick();
      modelOrigin = ORIGIN_FILE_INPUT;
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

  /**
   * Merge buttons with full list of file keywords.
   * 
   * @return new list, including trailing space character.
   */
  protected String getKeywordsFromButtons() {
    String keywords = " " + cleanNBOKeylist(file47Keywords, false) + " ";
    if (keywordButtons == null)
      return keywords;
    for (int i = 0; i < keywordButtons.length; i++) {
      String key = RUN_KEYWORD_LIST[i].substring(0, RUN_KEYWORD_LIST[i].indexOf(":"));
      keywords = PT.rep(keywords, " " + key + " ", " ");
      if (keywordButtons[i].isSelected())
        keywords += key + " ";
    }
    return keywords;
  }
  
  /**
   * Initiates a gennbo job via NBOServe; called from RUN, VIEW, and SEARCH
   * 
   * Note that there are issues with this method.
   * 
   * @param requiredKeyword
   */
  protected void runGenNBOJob(String requiredKeyword) {

    if (jmolOptionNONBO) {
      alertRequiresNBOServe();
      return;
    }

    // get the current file47Data and nboKeywords

    get47FileData(false);

    String newKeywords = getKeywordsFromButtons();
    
    //Check the plot file names match job name, warn user otherwise
    inputFileHandler.jobStem = inputFileHandler.jobStem.trim();

    if (requiredKeyword.length() > 0 && tfJobName != null) {
      // from another module
      tfJobName.setText(inputFileHandler.jobStem);
    }
    String jobName = (tfJobName == null ? inputFileHandler.jobStem : tfJobName
        .getText().trim());

    // BH Q: Would it be reasonable if the NO option is chosen to put that other job name in to the jobStem field, and also copy the .47 file to that? Or use that?
    // Or, would it be better to ask this question immediately upon file loading so that it doesn't come up, and make it so that
    // you always MUST have these two the same?

    if (!jobName.equals(inputFileHandler.jobStem)) {
      int i = JOptionPane
          .showConfirmDialog(
              null,
              "Note: Plot files are being created with name \""
                  + jobName
                  + "\", which does not match your file name \""
                  + inputFileHandler.jobStem
                  + "\"\nTo continue, we must create a new .47 file \""
                  + jobName
                  + ".47\" so that all files related to this job are under the same name. Continue?",
              "Warning", JOptionPane.YES_NO_OPTION);
     if (i != JOptionPane.YES_OPTION)
       return;
     inputFileHandler.copyAndSwitch47FileTo(jobName);
     
    }

    for (String x : PT.getTokens(requiredKeyword)) {
      if (!newKeywords.contains(" " + x + " ")) {
        newKeywords += x + " ";
      }
    }

    if (!newKeywords.contains("PLOT"))
      newKeywords += "PLOT";

    jobName = (jobName.equals("") ? inputFileHandler.jobStem : jobName);    

    String[] fileData = inputFileHandler.update47File(jobName, newKeywords);
    if (fileData == null)
      return;
    get47FileData(true);
    SB sb = new SB();
    sb.append("GLOBAL C_PATH " + inputFileHandler.inputFile.getParent() + sep);
    sb.append("GLOBAL C_JOBSTEM " + inputFileHandler.jobStem + sep);
    sb.append("GLOBAL C_ESS gennbo" + sep);
    sb.append("GLOBAL C_LABEL_1 FILE=" + jobName + sep);
    logCmd("RUN GenNBO FILE=" + jobName + " " + file47Keywords);
        
    postNBO_r(sb, NBOService.MODE_RUN_GENNBO, "Running GenNBO...");
  }

  /**
   * Post a request to NBOServe with a callback to processNBO_r.
   * 
   * @param sb
   *        command data
   * @param mode
   *        type of request
   * @param statusMessage
   */
  private void postNBO_r(SB sb, final int mode, String statusMessage) {
    final NBORequest req = new NBORequest();
    req.set(new Runnable() {
      @Override
      public void run() {
        processNBO_r(req, mode);
      }
    }, statusMessage, "r_cmd.txt", sb.toString());
    nboService.postToNBO(req);
  }

  
  /**
   * Process the reply from NBOServe for a RUN request
   * 
   * @param req
   * @param mode
   */
  protected void processNBO_r(NBORequest req, int mode) {
    inputFileHandler.setInputFile(inputFileHandler.inputFile);
  }

}
