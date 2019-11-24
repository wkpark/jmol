/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-26 23:35:44 -0500 (Fri, 26 Jun 2009) $
 * $Revision: 11131 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.openscience.jmol.app.janocchio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javajs.util.JSJSONParser;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import org.jmol.c.CBK;
import org.jmol.dialog.FileChooser;
import org.jmol.i18n.GT;
import org.openscience.jmol.app.Jmol;
import org.openscience.jmol.app.JmolApp;
import org.openscience.jmol.app.jmolpanel.DisplayPanel;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;
import org.openscience.jmol.app.jmolpanel.JmolResourceHandler;
import org.openscience.jmol.app.jmolpanel.Splash;
import org.openscience.jmol.app.jmolpanel.StatusBar;
import org.openscience.jmol.app.jmolpanel.StatusListener;
//import org.json.Lst<Object>;
//import org.json.JSONException;
//import org.json.JSONObject;

public class NMR_JmolPanel extends JmolPanel {

  final static int MIN_SIZE = 600;

  private NMR_DisplayPanel nmrDisplay;

  public JSplitPane mainSplitPane;
  public NoeTable noeTable;
  public CoupleTable coupleTable;
  public FrameCounter frameCounter;
  public LabelSetter labelSetter;
  public PopulationDisplay populationDisplay;
  public FrameDeltaDisplay frameDeltaDisplay;

  int nAtomsPerModel;

  protected static File currentDir;

  FileChooser openChooser;
  JFileChooser exportChooser;
  JFileChooser saveNmrChooser;
  JFileChooser readNmrChooser;
  JFileChooser saveNamfisChooser;
  JFileChooser readNamfisChooser;

  NmrApplet nmrApplet;
  boolean isApplet;
  // private CDKPluginManager pluginManager;

  private NmrGuiMap nmrguimap; // ha ha! Can't initialize this if superclass is also initializing!

  @Override
  public JMenuItem getMenuItem(String name) {
    return (JMenuItem) nmrguimap.get(name);
  }

  static Point border;
  static Boolean haveBorder = Boolean.FALSE;

  static Set<String> htGuiChanges;

  static {
    htGuiChanges = new HashSet<String>();
    String[] changes = tokenize(NmrResourceHandler.getStringX("changes"));
    for (int i = changes.length; --i >= 0;)
      htGuiChanges.add(changes[i]);
  }

  /**
   * The current file.
   */
  File currentFile;

  public NMR_JmolPanel(JmolApp jmolApp, Splash splash, JFrame frame,
      Jmol parent, int startupWidth, int startupHeight,
      Map<String, Object> vwrOptions, Point loc) {
    super(jmolApp, splash, frame, parent, startupWidth, startupHeight,
        vwrOptions, loc);
  }

  @Override
  protected String getWindowName() {
    return "Janocchio";
  }

  /**
   * @return A list of Actions that is understood by the upper level application
   */
  @Override
  protected List<Action> getFrameActions() {
    List<Action> actions = super.getFrameActions();
    Action[] a = {
        //new PdfAction(), 
        new ReadNmrAction(), new SaveNmrAction(), new DetachAppletAction(),
        new ReattachAppletAction(), new WriteNamfisAction(),
        new ReadNamfisAction(), new LabelNmrAction(),
        new JumpBestFrameAction(), new ViewNoeTableAction(),
        new ViewCoupleTableAction() };
    for (int i = a.length; --i >= 0;)
      actions.add(a[i]);
    return actions;
  }

  @Override
  protected String getStringX(String cmd) {
    return (cmd.indexOf("NMR.") == 0 ? NmrResourceHandler.getStringX(cmd)
        : JmolResourceHandler.getStringX(cmd));
  }

  @Override
  protected ImageIcon getIconX(String img) {
    return (img.indexOf("NMR.") == 0 ? NmrResourceHandler.getIconX(img)
        : JmolResourceHandler.getIconX(img));
  }

  @Override
  protected GuiMap createGuiMap() {
    return nmrguimap = new NmrGuiMap();
  }

  @Override
  protected StatusBar createStatusBar() {
    return super.createStatusBar();
  }

  @Override
  protected JToolBar createToolBar() {
    JToolBar toolbar = newToolbar(tokenize(NmrResourceHandler
        .getStringX("toolbar")));
    //DAE add extra label
    frameCounter = new FrameCounter((NMR_Viewer) vwr);
    toolbar.add(frameCounter, BorderLayout.EAST);
    say(GT.$("Initializing Noes..."));
    noeTable = new NoeTable((NMR_Viewer) vwr, frame);
    say(GT.$("Initializing Couples..."));
    coupleTable = new CoupleTable((NMR_Viewer) vwr, frame);
    labelSetter = new LabelSetter((NMR_Viewer) vwr, noeTable, coupleTable);
    toolbar.add(labelSetter, BorderLayout.EAST);
    //Action handler implementation would go here.
    toolbar.add(Box.createHorizontalGlue());
    return toolbar;
  }

  @Override
  protected void createDisplayAndAddStatusListener() {
    isPlugin = ((Nmr) jmolApp).isPlugin();
    say(GT.$("Initializing 3D display..."));
    display = nmrDisplay = new NMR_DisplayPanel(this);
    vwrOptions.put("display", display);
    myStatusListener = new MyStatusListener(this, display);
    vwrOptions.put("statusListener", myStatusListener);
  }

  @Override
  protected void setupModelAdapterAndViewer() {
    if (JmolResourceHandler.codePath != null)
      vwrOptions.put("codePath", JmolResourceHandler.codePath);
    if (modelAdapter != null)
      vwrOptions.put("modelAdapter", modelAdapter);
    say(GT.$("Initializing 3D display...4"));
    vwr = new NMR_Viewer(vwrOptions);
    say(GT.$("Initializing 3D display...5"));
    nmrDisplay.setViewer(vwr);
    myStatusListener.setViewer(vwr);

  }

  @Override
  protected void getDialogs() {
    super.getDialogs();
  }

  @Override
  protected void getMeasurementTable() {
    super.getMeasurementTable();
  }

  @Override
  protected void setupDisplay() {
    super.setupDisplay();
  }

  @Override
  protected void setFrameLocation(Point loc, JmolPanel parent) {
    super.setFrameLocation(loc, parent);
  }

  @Override
  protected void setIntoFrame() {

    frame.setTitle("Janocchio");
    frame.setBackground(Color.lightGray);
    frame.setLayout(new BorderLayout());
    frame.setBounds(0, 0, startupWidth, startupHeight);
    Container contentPane = frame.getContentPane();
    //    contentPane.setPreferredSize(new Dimension(500, 500));
    //    display.setPreferredSize(new Dimension(500, 500));

    mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, null, null);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setContinuousLayout(true);
    mainSplitPane.setLeftComponent(this);
//    setMinimumSize(new Dimension(MIN_SIZE, MIN_SIZE));
    frame.setMinimumSize(new Dimension(startupWidth + 400, 100));

    JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, null,
        null);
    splitPaneRight.setOneTouchExpandable(true);
    //splitPaneRight.setDividerLocation(0.5); Doesn't do anything here
    //splitPaneRight.setContinuousLayout(true);
    JPanel noePanel = new JPanel();
    noePanel.setLayout(new BoxLayout(noePanel, BoxLayout.PAGE_AXIS));

    JLabel label = new JLabel("NOE Table", null, SwingConstants.CENTER);
    label.setAlignmentX(Component.CENTER_ALIGNMENT);

    noePanel.add(label);
    noePanel.add(noeTable);

    JPanel couplePanel = new JPanel();
    couplePanel.setLayout(new BoxLayout(couplePanel, BoxLayout.PAGE_AXIS));

    JLabel label1 = new JLabel("Couple Table", null, SwingConstants.CENTER);
    label1.setAlignmentX(Component.CENTER_ALIGNMENT);

    populationDisplay = new PopulationDisplay(((NMR_Viewer) vwr));
    frameDeltaDisplay = new FrameDeltaDisplay(((NMR_Viewer) vwr));

    nmrDisplay.setPopulationDisplay(populationDisplay);
    nmrDisplay.setFrameDeltaDisplay(frameDeltaDisplay);

    noeTable.setFrameDeltaDisplay(frameDeltaDisplay);
    coupleTable.setFrameDeltaDisplay(frameDeltaDisplay);

    couplePanel.add(label1, BorderLayout.PAGE_START);
    couplePanel.add(coupleTable, BorderLayout.CENTER);

    couplePanel.add(populationDisplay, BorderLayout.PAGE_END);
    couplePanel.add(frameDeltaDisplay, BorderLayout.PAGE_END);

    splitPaneRight.setTopComponent(couplePanel);
    splitPaneRight.setBottomComponent(noePanel);

    mainSplitPane.setRightComponent(splitPaneRight);
    splitPaneRight.setMinimumSize(new Dimension(300, 500));

    //noeTable.setMinimumSize(new Dimension(0,0));
    contentPane.add(mainSplitPane, BorderLayout.CENTER);

    contentPane.setPreferredSize(new Dimension(startupWidth, startupHeight));
    //dumpContainer(frame, "");
    frame.pack();
    //dumpContainer(frame, "");
    //    dumpContainer(frame, "");

    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);
    if (!((Nmr)jmolApp).isPlugin())
      frame.addWindowListener(new AppCloser());

  }

  @Override
  protected void setupConsole() {
    super.setupConsole();
  }

  @Override
  protected void setupDnD() {
    super.setupDnD();
  }

  @Override
  protected void setAtomChooser() {
    super.setAtomChooser();
  }

  @Override
  protected void launchMainFrame() {
    say(GT.$("Launching main frame..."));
  }

  @Override
  protected void saveWindowSizes() {
    super.saveWindowSizes();
  }

  @Override
  public void getJavaConsole() {
    super.getJavaConsole();
  }

  @Override
  protected String setMenuKeys(String key, String tokens) {
    if (htGuiChanges.contains(key)) {
      String s = NmrResourceHandler.getStringX(key);
      if (s == null) {
        System.err.println("Replacement for " + key + " not found; using "
            + tokens);
      } else {
        // insert at end or prior to last - 
        if (s.startsWith("+")) {
          s = s.substring(1);
          int pt = tokens.lastIndexOf(" - ");
          if (pt < 0) {
            // menubar, toolbar will not hvae - s
            tokens += (key.endsWith("bar") ? " " : " - ") + s;
          } else {
            tokens = tokens.substring(0, pt + 3) + s + tokens.substring(pt);
          }
        } else {
          tokens = s;
        }
        System.out.println("Replacement for " + key + " = " + tokens);
      }
    }
    return tokens;
  }

  @Override
  protected boolean doClose(boolean saveSize) {
    if (isPlugin) {
      frame.setVisible(false);
      ((Nmr) jmolApp).plugin.setVisible(false);
    } else {
      super.doClose(saveSize);
    }
    return true;
    
  }

  void setCurrentDirectoryAll(File cDir) {
    openChooser.setCurrentDirectory(cDir);
    saveNmrChooser.setCurrentDirectory(cDir);
    saveNamfisChooser.setCurrentDirectory(cDir);

    readNmrChooser.setCurrentDirectory(cDir);
    readNamfisChooser.setCurrentDirectory(cDir);
  }

  public int getMinindex() {
    return labelSetter.getMinindex();
  }

  public String getCurrentStructureFile() {
    return vwr.getModelSetPathName();
  }

  /**
   * Returns a new File referenced by the property 'user.dir', or null if the
   * property is not defined.
   * 
   * @return a File to the user directory
   */
  public static File getUserDirectory() {
    if (System.getProperty("user.dir") == null) {
      return null;
    }
    return new File(System.getProperty("user.dir"));
  }

  protected static void dumpContainer(Container c, String s) {
    if (c == null)
      return;
    for (int i = c.getComponentCount(); --i >= 0;) {
      Container c1 = (Container) c.getComponent(i);
      System.out.println(s + c1);
      dumpContainer(c1, s + " ");
    }
  }

  /**
   * Take the given string and chop it up into a series of strings on whitespace
   * boundries. This is useful for trying to get an array of strings out of the
   * resource file.
   * 
   * @param input
   *        String to chop
   * @return Strings chopped on whitespace boundries
   */
  protected static String[] tokenize(String input) {

    List<String> v = new ArrayList<String>();
    StringTokenizer t = new StringTokenizer(input);
    String cmd[];

    while (t.hasMoreTokens()) {
      v.add(t.nextToken());
    }
    cmd = new String[v.size()];
    for (int i = 0; i < cmd.length; i++) {
      cmd[i] = v.get(i);
    }
    return cmd;
  }

  // these correlate with items in NMR_GuiMap.java

  //  private static final String pdfActionProperty = "NMR.pdf";
  private static final String saveNmrAction = "NMR.saveNmr";
  private static final String readNmrAction = "NMR.readNmr";
  private static final String detachAppletAction = "NMR.detachApplet";
  private static final String reattachAppletAction = "NMR.reattachApplet";
  private static final String writeNamfisAction = "NMR.writeNamfis";
  private static final String readNamfisAction = "NMR.readNamfis";
  private static final String jumpBestFrameAction = "NMR.jumpBestFrame";
  private static final String labelNmrAction = "NMR.labelNmr";

  // --- action implementations -----------------------------------

  //  private PdfAction pdfAction;
  //  private ViewNoeTableAction viewNoeTableAction;
  //  private ViewCoupleTableAction viewCoupleTableAction;

  class SaveNmrAction extends AbstractAction {

    public SaveNmrAction() {
      super(saveNmrAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      NmrSaver nmrSaver = new NmrSaver(saveNmrChooser);

      saveNmrChooser.setAccessory(nmrSaver);

      int retval = saveNmrChooser.showSaveDialog(NMR_JmolPanel.this);
      File cDir = saveNmrChooser.getCurrentDirectory();
      setCurrentDirectoryAll(cDir);
      if (retval == 0) {
        File file = saveNmrChooser.getSelectedFile();
        MyFileFilter filter = new MyFileFilter("jsn", "NMR Data files");

        if (file != null) {
          if (!filter.checkExtension(file)) {
            String name = file.getAbsolutePath();
            // name = name + ".jnc";
            // file = new File(name);
            name = name + ".jsn";
            file = new File(name);
          }

          try {
            new LoadMeasureThreadJSON(NMR_JmolPanel.this, null).writeNmrDataJSON(file);
          } catch (Exception exc) {
            // Help
          }
        }
        //
      }

    }

  }

  public void writeNmrData(File file) throws IOException {
    PrintWriter out = new PrintWriter(new FileWriter(file));

    String structurefile = vwr.getModelSetPathName();
    out.println(structurefile);

    String[] labelArray = labelSetter.getLabelArray();
    for (int i = 0; i < labelArray.length; i++) {
      if (labelArray[i] != null) {
        out.println(String.valueOf(i + 1) + " " + labelArray[i]);
      }
    }
    out.println("");
    int noeCount = noeTable.getRowCount();
    for (int i = 0; i < noeCount; i++) {
      int[] atomIndices = noeTable.getMeasurementCountPlusIndices(i);
      out.println(String.valueOf(atomIndices[1] + 1) + " "
          + String.valueOf(atomIndices[2] + 1) + " "
          + noeTable.getExpNoe(atomIndices[1], atomIndices[2]));
    }
    out.println("");

    int coupleCount = coupleTable.getRowCount();
    for (int i = 0; i < coupleCount; i++) {
      int[] atomIndices = coupleTable.getMeasurementCountPlusIndices(i);
      out.println(String.valueOf(atomIndices[1] + 1) + " "
          + String.valueOf(atomIndices[2] + 1) + " "
          + String.valueOf(atomIndices[3] + 1) + " "
          + String.valueOf(atomIndices[4] + 1) + " "
          + coupleTable.getExpCouple(atomIndices[1], atomIndices[4]));
    }

    out.flush();
    out.close();
  }

  public class ReadNmrAction extends AbstractAction {

    public ReadNmrAction() {
      super(readNmrAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      NmrReader nmrReader = new NmrReader(readNmrChooser);

      readNmrChooser.setAccessory(nmrReader);

      int retval = readNmrChooser.showOpenDialog(NMR_JmolPanel.this);
      File cDir = readNmrChooser.getCurrentDirectory();
      setCurrentDirectoryAll(cDir);
      if (retval == 0) {
        File file = readNmrChooser.getSelectedFile();
        if (file != null) {
          try {
            MyFileFilter filter = new MyFileFilter("jnc", "NMR Data files");
            if (filter.checkExtension(file)) {
              readNmrData(file);
            } else {
              readNmrDataJSON(file);
            }

          } catch (Exception exc) {
            // Help
          }
        }
        //
      }

    }

  }

  public void readNmrData(File file) throws IOException {
    String line;

    // This routine is problematic because we want to load a structure file,
    // wait for it to load, then load in the measurements.
    // Jmol does all scripting commands (e.g. file loading) in a
    // separate thread. viewer.scriptWait() doesn't work because
    // this is the main event handling thread, which includes
    // watching file loading events (I think).
    // So, it is necessary to start both file loading and measurement
    // loading in separate threads, then have the measurement loading
    // thread wait on the the file loading.

    // Structure File
    String currentStructureFile = getCurrentStructureFile();
    BufferedReader inp = new BufferedReader(new FileReader(file));
    line = inp.readLine().trim();
    inp.close();
    if (currentStructureFile == null) {
      int opt = JOptionPane.showConfirmDialog(NMR_JmolPanel.this,
          "No Structure File currently loaded.\nLoad Structure File " + line
              + "\ndefined in this NMR Data File?", "No Structure Warning",
          JOptionPane.YES_NO_OPTION);
      if (opt == JOptionPane.YES_OPTION) {
        // viewer.script("load " + line);
        // It seems to be necessary to load the file in a separate thread
        // See also LoadMeasuresThread.java
        ScriptWaitThread thread = new ScriptWaitThread("load \"" + line + "\"",
            ((NMR_Viewer) vwr));
        thread.start();
      } else {
        return;
      }
    } else {
      if (!currentStructureFile.equals(line)) {

        // Warn that the structure data was saved from a different file name from the
        // current model
        int opt = JOptionPane
            .showConfirmDialog(
                NMR_JmolPanel.this,
                "This NMR Data file was saved from a different structure file from that currently loaded.\nContinue Reading Data?",
                "Read NMR Data Warning", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.NO_OPTION) {
          return;
        }

      }
    }

    // Load measurements (and labels) in a separate thread

    LoadMeasureThread thread = new LoadMeasureThread(this, inp);
    thread.start();

  }

  private Map<String, Object> jsonData;
  
  public void readNmrDataJSON(File file) throws Exception {

    //    BufferedReader inp = new BufferedReader(new FileReader(file));

    // This routine is problematic because we want to load a structure file,
    // wait for it to load, then load in the measurements.
    // Jmol does all scripting commands (e.g. file loading) in a
    // separate thread. viewer.scriptWait() doesn't work because
    // this is the main event handling thread, which includes
    // watching file loading events (I think).
    // So, it is necessary to start both file loading and measurement
    // loading in separate threads, then have the measurement loading
    // thread wait on the the file loading.

    if (file != null) {
      // Structure File
      String currentStructureFile = getCurrentStructureFile();

      //    String jsonstring = inp.readLine().trim();
      //    inp.close();

      String json = vwr.getFileAsString(file.getAbsolutePath());

      jsonData = new JSJSONParser().parseMap(json, true);

      //    JSONObject data = new JSONObject(jsonstring);
      String structureFile = (String) jsonData.get("StructureFile");

      if ((currentStructureFile == null) || (currentStructureFile == "zapped")) {
        int opt = JOptionPane.showConfirmDialog(NMR_JmolPanel.this,
            "No Structure File currently loaded.\nLoad Structure File "
                + structureFile + "\ndefined in this NMR Data File?",
            "No Structure Warning", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {

          // viewer.script("load " + line);
          // It seems to be necessary to load the file in a separate thread
          // See also LoadMeasuresThread.java
          //        ScriptWaitThread thread = new ScriptWaitThread("load \""
          //            + structureFile + "\"", ((NMR_Viewer) vwr));
          //        thread.start();

          vwr.script("load \"" + structureFile + "\";message NMR-loadData");
        }
        return;
      }
      if (!currentStructureFile.equals(structureFile)) {

        // Warn that the structure data was saved from a different file name from the
        // current model
        int opt = JOptionPane
            .showConfirmDialog(
                NMR_JmolPanel.this,
                "This NMR Data file was saved from a different structure file from that currently loaded.\nContinue Reading Data?",
                "Read NMR Data Warning", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.NO_OPTION) {
          return;
        }
      }
    }
    Map<String, Object> data = jsonData;
    jsonData = null;
    new LoadMeasureThreadJSON(this, data).run();
    // Load measurements (and labels) in a separate thread

    //    LoadMeasureThreadJSON thread = new LoadMeasureThreadJSON(this, jsonData);
    //    thread.start();

  }

  public class LabelNmrAction extends AbstractAction {
    public LabelNmrAction() {
      super(labelNmrAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String[] labelArray = labelSetter.getLabelArray();
      String command = new String();
      //    int minindex = labelSetter.getMinindex();
      for (int i = 0; i < labelArray.length; i++) {
        // if (labelArray[i] != null) {
        command = command + labelSetter.setLabelString(i, labelArray[i]);
        // }
      }
      vwr.script(command);
    }
  }

  public class DetachAppletAction extends AbstractAction {

    public DetachAppletAction() {
      super(detachAppletAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // ??
      //      nmrApplet.detach();
    }

  }

  public class ReattachAppletAction extends AbstractAction {

    public ReattachAppletAction() {
      super(reattachAppletAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      //      nmrApplet.reattach();
    }

  }

  public class ReadNamfisAction extends AbstractAction {

    public ReadNamfisAction() {
      super(readNamfisAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

      NamfisReader namfisReader = new NamfisReader(readNamfisChooser);

      readNamfisChooser.setAccessory(namfisReader);

      int retval = readNamfisChooser.showOpenDialog(NMR_JmolPanel.this);
      File cDir = readNamfisChooser.getCurrentDirectory();
      setCurrentDirectoryAll(cDir);
      if (retval == 0) {
        File file = readNamfisChooser.getSelectedFile();
        // MyFileFilter filter = new MyFileFilter("in1");

        if (file != null) {

          try {
            readNamfisOutput(file);
          } catch (Exception exc) {
            // Help
          }
        }
        //
      }

    }

    private boolean readNamfisOutput(File file) throws IOException {
      BufferedReader inp = new BufferedReader(new FileReader(file));
      String line;

      // NAMFIS often fails to fit the data
      // Indicated by first line

      line = inp.readLine();
      if (line.matches("No feasible solution")) {
        inp.close();
        return false;
      }
      inp.readLine();
      inp.readLine();
      inp.readLine();
      int nmodel = ((NMR_Viewer) vwr).getModelCount();
      double[] population = new double[nmodel + 1];
      for (int i = 0; i <= nmodel; i++) {
        population[i] = 0.0;
      }

      while ((line = inp.readLine()).trim().length() != 0) {
        String[] l = line.split("[()=\\s]+");

        int i = (new Integer(l[1])).intValue();
        double p = (new Double(l[2])).doubleValue();

        population[i] = p;
      }
      populationDisplay.addPopulation(population);
      frameDeltaDisplay.setVisible(false);
      JCheckBoxMenuItem mi = (JCheckBoxMenuItem) getMenuItem("NMR.populationDisplayCheck");
      mi.setSelected(true);

      inp.close();
      return true;
    }
  }

  public class WriteNamfisAction extends AbstractAction {

    public WriteNamfisAction() {
      super(writeNamfisAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      // nmrApplet.reattach();
      NamfisSaver namfisSaver = new NamfisSaver(saveNamfisChooser);

      saveNamfisChooser.setAccessory(namfisSaver);

      int retval = saveNamfisChooser.showSaveDialog(NMR_JmolPanel.this);
      File cDir = saveNamfisChooser.getCurrentDirectory();
      setCurrentDirectoryAll(cDir);
      if (retval == 0) {
        File file = saveNamfisChooser.getSelectedFile();
        // MyFileFilter filter = new MyFileFilter("in1");

        if (file != null) {
          String name = file.getAbsolutePath();
          String[] exts = { "in1", "in2", "in3" };
          MyFileFilter filter = new MyFileFilter(exts, "");
          if (filter.checkExtension(file)) {
            // Pattern p = Pattern.compile(".in[0-9]$");
            name = name.replaceFirst(".in[0-9]$", "");

          }

          try {
            new LoadMeasureThreadJSON(NMR_JmolPanel.this, null).writeNamfisFiles(name);
          } catch (Exception exc) {
            // Help
          }
        }
        //
      }

    }

  }

  public class JumpBestFrameAction extends AbstractAction {

    public JumpBestFrameAction() {
      super(jumpBestFrameAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int frame = new LoadMeasureThreadJSON(NMR_JmolPanel.this, null).jumpBestFrame();
      if (frame >= 0)
        frameCounter.setFrameNumberChangeViewer(frame);


    }

  }

  class AtomSetChooserAction extends AbstractAction {
    public AtomSetChooserAction() {
      super(atomsetchooserAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      atomSetChooser.setVisible(true);
    }
  }

  //  public class PdfAction extends MoleculeDependentAction {
  //
  //    public PdfAction() {
  //      super(pdfActionProperty);
  //    }
  //
  //    @Override
  //    public void actionPerformed(ActionEvent e) {
  //
  //      exportChooser.setAccessory(null);
  //
  //      int retval = exportChooser.showSaveDialog(NMR_JmolPanel.this);
  //      File cDir = exportChooser.getCurrentDirectory();
  //      setCurrentDirectoryAll(cDir);
  //      if (retval == JFileChooser.APPROVE_OPTION) {
  //        File file = exportChooser.getSelectedFile();
  //
  //        if (file != null) {
  //          Document document = new Document();
  //
  //          try {
  //            PdfWriter writer = PdfWriter.getInstance(document,
  //                new FileOutputStream(file));
  //
  //            document.open();
  //
  //            int w = display.getWidth();
  //            int h = display.getHeight();
  //            PdfContentByte cb = writer.getDirectContent();
  //            PdfTemplate tp = cb.createTemplate(w, h);
  //            Graphics2D g2 = tp.createGraphics(w, h);
  //            g2.setStroke(new BasicStroke(0.1f));
  //            tp.setWidth(w);
  //            tp.setHeight(h);
  //
  //            display.print(g2);
  //            g2.dispose();
  //            cb.addTemplate(tp, 72, 720 - h);
  //          } catch (DocumentException de) {
  //            System.err.println(de.getMessage());
  //          } catch (IOException ioe) {
  //            System.err.println(ioe.getMessage());
  //          }
  //
  //          document.close();
  //        }
  //      }
  //    }
  //
  //  }

  public class ViewNoeTableAction extends MoleculeDependentAction {

    public ViewNoeTableAction() {
      super("viewNoeTable");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      noeTable.activate();
    }
  }

  public class ViewCoupleTableAction extends MoleculeDependentAction {

    public ViewCoupleTableAction() {
      super("viewCoupleTable");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      coupleTable.activate();
    }
  }

  public static final String chemFileProperty = "chemFile";

  private abstract class MoleculeDependentAction extends AbstractAction
      implements PropertyChangeListener {

    public MoleculeDependentAction(String name) {
      super(name);
      setEnabled(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {

      if (event.getPropertyName().equals(chemFileProperty)) {
        if (event.getNewValue() != null) {
          setEnabled(true);
        } else {
          setEnabled(false);
        }
      }
    }
  }

  class MyStatusListener extends StatusListener {

    private String defaultFormat = "set measurementUnits hz;measure \"2:%1.1VALUE %UNITS//hz\"";

    MyStatusListener(JmolPanel jmol, DisplayPanel display) {
      super(jmol, display);
    }

    /**
     * @param fullPathName
     * @param fileName
     * @param modelName
     * @param errorMsg
     * @param isAsync
     */
    private void notifyFileLoaded(String fullPathName, String fileName,
                                  String modelName, String errorMsg,
                                  Boolean isAsync) {
      if (errorMsg != null) {
        return;
      }
      if (jmolApp.haveDisplay)
        pcs.firePropertyChange(chemFileProperty, null, fullPathName);

      int nmodel = ((NMR_Viewer) vwr).getModelCount();

      
      frameCounter.setFrameCount(nmodel);

      populationDisplay.setVisible(false);
      frameDeltaDisplay.setVisible(true);
      JCheckBoxMenuItem mi = (JCheckBoxMenuItem) getMenuItem("NMR.frameDeltaDisplayCheck");
      mi.setSelected(true);
      vwr.script(defaultFormat);
    }

    public void notifyFrameChanged(int frameNo) {
      if (vwr == null)
        return;
      int ntot = ((NMR_Viewer) vwr).getAtomCount();
      int nmodel = ((NMR_Viewer) vwr).getModelCount();
      int natpm = 0;
      if (nmodel > 0) {
        natpm = ntot / nmodel;
      }
      if (natpm != nAtomsPerModel) {
        // new molecule loaded, need to allocate new label array
        nAtomsPerModel = natpm;
        labelSetter.allocateLabelArray(nAtomsPerModel);

        noeTable.allocateLabelArray(nAtomsPerModel);
        noeTable.allocateExpNoes(nAtomsPerModel);
        coupleTable.allocateLabelArray(nAtomsPerModel);
        coupleTable.allocateExpCouples(nAtomsPerModel);
      }

      frameCounter.setFrameNumberFromViewer(frameNo + 1);
      populationDisplay.setFrameNumberFromViewer(frameNo + 1);

      coupleTable.setmolCDKuptodate(false);
      noeTable.setmolCDKuptodate(false);
      noeTable.addMol();

      coupleTable.updateTables();
      noeTable.updateTables();
      vwr.script(defaultFormat);

    }

    /**
     * @param atomIndex
     * @param strInfo
     */
    public void notifyAtomPicked(int atomIndex, String strInfo) {
      int iModel = atomIndex / nAtomsPerModel;
      int firstModelAtomIndex = atomIndex - iModel * nAtomsPerModel;
      labelSetter.setSelectedAtomIndex(firstModelAtomIndex);

      int minindex = labelSetter.getMinindex();

      String command = "set display SELECTED; select (atomno="
          + String.valueOf(firstModelAtomIndex + minindex) + ")";
      vwr.script(command);
    }

    @Override
    public void notifyCallback(CBK type, Object[] data) {
      String strInfo = (data == null || data[1] == null ? null : data[1]
          .toString());

      super.notifyCallback(type, data);
      switch (type) {
      case LOADSTRUCT:
        notifyFileLoaded(strInfo, (String) data[2], (String) data[3],
            (String) data[4], (Boolean) data[8]);
        break;
      case ANIMFRAME:
        int[] iData = (int[]) data[1];
        int modelIndex = iData[0];
        notifyFrameChanged(modelIndex);
        break;
      case PICK:
        int atomIndex = ((Integer) data[2]).intValue();
        notifyAtomPicked(atomIndex, strInfo);
        break;
      case MEASURE: {
        String mystatus = (String) data[3];
        if (defaultFormat  != null) {
          vwr.script(defaultFormat);
          defaultFormat = null;
        }
        if (mystatus.equals("measurePending") || mystatus.equals("measureDeleted"))
          return;
        if (mystatus.indexOf("Sequence") < 0) {
          if (mystatus.indexOf("Picked") >= 0) {
            // picking mode
            Integer picked = (Integer) vwr.getPOrNull("_picked");
            if (picked != null)
              notifyAtomPicked(picked.intValue(), strInfo);
          }
        }
        noeTable.updateTables();
        if (coupleTable.getViewerMeasurement(-1) == null) {
          vwr.deleteMeasurement(vwr.getMeasurementCount() - 1);
          return;
        }
        coupleTable.updateTables();
      }
        break;
      case APPLETREADY:
        break;
      case ATOMMOVED:
        break;
      case AUDIO:
        break;
      case CLICK:
        break;
      case DRAGDROP:
        break;
      case ECHO:
        break;
      case ERROR:
        break;
      case EVAL:
        break;
      case HOVER:
        break;
      case IMAGE:
        break;
      case MESSAGE:
        break;
      case MINIMIZATION:
        break;
      case RESIZE:
        break;
      case SCRIPT:
        break;
      case SERVICE:
        break;
      case STRUCTUREMODIFIED:
        break;
      case SYNC:
        break;
      default:
        break;
      }

    }

  }

}
