/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;
import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.JmolModelAdapter;
import org.openscience.jmol.viewer.JmolStatusListener;

import org.openscience.jmol.adapters.DeprecatedJmolModelAdapter;
import org.openscience.jmol.adapters.CdkJmolModelAdapter;
import org.openscience.jmol.adapters.XyzJmolModelAdapter;

import org.openscience.jmol.*;
import org.openscience.cdk.io.ChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.PdbSaver;
import org.openscience.jmol.ui.JmolPopup;
import Acme.JPM.Encoders.GifEncoder;
import Acme.JPM.Encoders.ImageEncoder;
import Acme.JPM.Encoders.PpmEncoder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.obrador.JpegEncoder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;

/**
 * The main class in Jmol.
 *
 * @author Bradley A. Smith (bradley@baysmith.com)
 * @author Peter Murray-Rust
 */
public class Jmol extends JPanel {

  /**
   * The data model.
   */

  public JmolViewer viewer;

  private DisplayPanel display;
  private StatusBar status;
  private AtomPropsMenu apm;
  public static AtomTypeTable atomTypeTable;
  private PreferencesDialog preferencesDialog;
  private Animate anim;
  private Vibrate vib;
  private CrystalPropertiesDialog crystprop;
  private MakeCrystal makecrystal;
  private TransformDialog transform;
  private PropertyGraph pg;
  private MeasurementTable measurementTable;
  private RecentFilesDialog recentFiles;
  public ScriptWindow scriptWindow;
  protected JFrame frame;
  private static File currentDir;
  private JFileChooser openChooser;
  private JFileChooser saveChooser;
  private FileTyper fileTyper;
  private JFileChooser exportChooser;
  private JmolPopup jmolpopup;

  private GuiMap guimap = new GuiMap();
  
  private static int numWindows = 0;
  private static Dimension screenSize = null;

  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);


  /**
   * The current file.
   */
  private File currentFile;

  /**
   * Button group for toggle buttons in the toolbar.
   */
  static AbstractButton buttonRotate = null;
  static ButtonGroup toolbarButtonGroup = new ButtonGroup();

  static File UserPropsFile;
  static File UserAtypeFile;
  private static HistoryFile historyFile;

  Splash splash;

  public static HistoryFile getHistoryFile() {
    return historyFile;
  }

  private static JFrame consoleframe;

  private JmolResourceHandler resourceHandler;

  static {
    if (System.getProperty("javawebstart.version") != null) {

      // If the property is found, Jmol is running with Java Web Start. To fix
      // bug 4621090, the security manager is set to null.
      System.setSecurityManager(null);
    }
    if (System.getProperty("user.home") == null) {
      System.err.println(
          "Error starting Jmol: the property 'user.home' is not defined.");
      System.exit(1);
    }
    File ujmoldir = new File(new File(System.getProperty("user.home")),
                      ".jmol");
    ujmoldir.mkdirs();
    UserPropsFile = new File(ujmoldir, "properties");
    UserAtypeFile = new File(ujmoldir, "jmol_atomtypes.txt");
    historyFile = new HistoryFile(new File(ujmoldir, "history"),
        "Jmol's persistent values");
  }

  Jmol(Splash splash, JFrame frame, Jmol parent) {
    super(true);
    this.frame = frame;
    numWindows++;
    
    // get a resource handler
    resourceHandler = JmolResourceHandler.getInstance();

    frame.setTitle("Jmol");
    frame.setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());
    
    this.splash = splash;

    setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());

    status = (StatusBar) createStatusBar();
    say("Initializing 3D display...");
    //
    display = new DisplayPanel(status, guimap);
    JmolModelAdapter modelAdapter;
    String adapter= System.getProperty("model");
    if (adapter == null || adapter.length() == 0)
      adapter = "cdk";
    if (adapter.equals("xyz")) {
      System.out.println("using Xyz Model Adapter");
      modelAdapter = new XyzJmolModelAdapter();
    } else if (adapter.equals("jmol")) {
      System.out.println("using jmol Deprecated Model Adapter");
      modelAdapter = new DeprecatedJmolModelAdapter();
    } else {
      System.out.println("using CDK Model Adapter");
      modelAdapter = new CdkJmolModelAdapter();
    }

    viewer = new JmolViewer(display, modelAdapter);
    display.setViewer(viewer);

    //    pcs.addPropertyChangeListener(display);
    say("Initializing Preferences...");
    preferencesDialog = new PreferencesDialog(frame, guimap, viewer);
    say("Initializing Animate...");
    anim = new Animate(viewer, frame);
    // mth 2003 08 15
    // get rid of this property change listener for Animate.java
    // may break animations
    pcs.addPropertyChangeListener(anim);
    say("Initializing Vibrate...");
    vib = new Vibrate(viewer, frame);
    // pcs.addPropertyChangeListener(vib);
    say("Initializing Crystal...");
    crystprop = new CrystalPropertiesDialog(viewer, frame);
    // pcs.addPropertyChangeListener(crystprop);
    makecrystal = new MakeCrystal(viewer, crystprop);
    // pcs.addPropertyChangeListener(makecrystal);
    transform = new TransformDialog(viewer, frame);
    // pcs.addPropertyChangeListener(transform);
    say("Initializing Recent Files...");
    recentFiles = new RecentFilesDialog(frame);
    say("Initializing Script Window...");
    scriptWindow = new ScriptWindow(viewer, frame);
    //scriptWindow = new ScriptWindow(frame, new RasMolScriptHandler(this));
    viewer.setJmolStatusListener(new MyJmolStatusListener());
    say("Initializing Property Graph...");
    pg = new PropertyGraph(frame);
    // pcs.addPropertyChangeListener(pg);

    say("Initializing Measurements...");
    measurementTable = new MeasurementTable(viewer, frame);

    // install the command table
    say("Building Command Hooks...");
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    // Fix for actions that confict with the operation of
    // vibration animations
    Action[] animActions = anim.getActions();
    for (int i = 0; i < animActions.length; ++i) {
      vib.addConflictingAction(animActions[i]);
    }
    vib.addConflictingAction(getAction(openAction));
    menuItems = new Hashtable();
    say("Building Menubar...");
    menubar = createMenubar();
    add("North", menubar);

    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add("North", createToolbar());

    JPanel ip = new JPanel();
    ip.setLayout(new BorderLayout());
    ip.add("Center", display);
    panel.add("Center", ip);
    add("Center", panel);
    add("South", status);

    say("Initializing Chemical Shifts...");
    chemicalShifts.initialize(apm);
    // pcs.addPropertyChangeListener(chemicalShifts);

    say("Starting display...");
    display.start();

    say("Setting up File Choosers...");
    openChooser = new JFileChooser();
    openChooser.setCurrentDirectory(currentDir);
    saveChooser = new JFileChooser();
    fileTyper = new FileTyper();
    saveChooser.addPropertyChangeListener(fileTyper);
    saveChooser.setAccessory(fileTyper);
    saveChooser.setCurrentDirectory(currentDir);
    exportChooser = new JFileChooser();
    exportChooser.setCurrentDirectory(currentDir);

    pcs.addPropertyChangeListener(chemFileProperty, saveAction);
    pcs.addPropertyChangeListener(chemFileProperty, exportAction);
    pcs.addPropertyChangeListener(chemFileProperty, povrayAction);
    pcs.addPropertyChangeListener(chemFileProperty, pdfAction);
    pcs.addPropertyChangeListener(chemFileProperty, printAction);
    pcs.addPropertyChangeListener(chemFileProperty,
                                  viewMeasurementTableAction);

    jmolpopup = new JmolPopup(viewer, display);

    // prevent new Jmol from covering old Jmol
    if (parent != null) {
        Point location = parent.frame.getLocationOnScreen();
        int maxX = screenSize.width - 50;
        int maxY = screenSize.height - 50;
        
        location.x += 40;
        location.y += 40;
        if ((location.x > maxX) || (location.y > maxY))
        {
            location.setLocation(0, 0);
        }
        frame.setLocation(location);
    }

    frame.getContentPane().add("Center", this);
    frame.addWindowListener(new Jmol.AppCloser());
    frame.pack();
    frame.setSize(400, 400);
    ImageIcon jmolIcon =
      JmolResourceHandler.getInstance().getIcon("Jmol.icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);
    // splash.showStatus(jrh.translate("Launching main frame..."));
    say("Launching main frame...");
  }

  public static Jmol getJmol(JFrame frame) {
    JmolResourceHandler jrh = JmolResourceHandler.getInstance();
    ImageIcon splash_image = jrh.getIcon("Jmol.splash");
    Splash splash = new Splash(frame, splash_image);
    splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    splash.showStatus(jrh.translate("Creating main window..."));
    splash.showStatus(jrh.translate("Initializing Swing..."));
    try {
        UIManager
        .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception exc) {
        System.err.println("Error loading L&F: " + exc);
    }
    
    screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    splash.showStatus(jrh.translate("Initializing Jmol..."));
    
    // cache the current directory to speed up Jmol window creation
    currentDir = getUserDirectory();
    
    splash.showStatus(jrh.translate("Reading AtomTypes..."));
    AtomTypeList atl = AtomTypeList.getInstance(UserAtypeFile);
    atomTypeTable = new AtomTypeTable(frame, UserAtypeFile);
    
    Jmol window = new Jmol(splash, frame, null);
    frame.show();
    return window;
  }

  public static void main(String[] args) {

    Jmol jmol = null;

    String modelFilename = null;
    String scriptFilename = null;

    if (args.length == 2) {
      String s = args[0];
      if (s.equals("-script"))
        scriptFilename = args[1];
    }
    if (args.length == 1)
      modelFilename = args[0];

    try {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.1.2") < 0) {
        System.out.println("!!!WARNING: Swing components require a "
            + "1.1.2 or higher version VM!!!");
      }

      JFrame jmolFrame = new JFrame();
      jmol = getJmol(jmolFrame);

      // Adding consoleframe to grab System.out & System.err
      consoleframe = new JFrame("Jmol Console");


      // Open a file if one is given as an argument
      if (modelFilename != null)
        jmol.viewer.openFile(modelFilename);

      // Oke, by now it is time to execute the script
      if (scriptFilename != null) {
        System.out.println("Executing script: " + scriptFilename);
        jmol.splash
          .showStatus(JmolResourceHandler.getInstance()
                      .getString("Executing script..."));
        jmol.viewer.evalFile(scriptFilename);
      }
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    try {
      ConsoleTextArea consoleTextArea = new ConsoleTextArea();
      consoleTextArea.setFont(java.awt.Font.decode("monospaced"));
      consoleframe.getContentPane().add(new JScrollPane(consoleTextArea),
          java.awt.BorderLayout.CENTER);
    } catch (IOException e) {
      JTextArea errorTextArea = new JTextArea();
      errorTextArea.setFont(java.awt.Font.decode("monospaced"));
      consoleframe.getContentPane().add(new JScrollPane(errorTextArea),
          java.awt.BorderLayout.CENTER);
      errorTextArea.append("Could not create ConsoleTextArea: " + e);
    }

    Point location = jmol.frame.getLocation();
    Dimension size = jmol.frame.getSize();
    consoleframe.setBounds(location.x, location.y + size.height, size.width,
        200);

    // I'd prefer that the console stay out of the way until requested,
    // so I'm commenting this line out for now...
    // consoleframe.show();

  }

  private void say(String message) {
      if (splash == null) {
          System.out.println(message);
      } else {
          splash.showStatus(resourceHandler.translate(message));
      }
  }
  
  /**
   * returns a list of Actions that is understood by the upper level
   * application
   */
  public Action[] getActions() {

    ArrayList actions = new ArrayList();
    actions.addAll(Arrays.asList(defaultActions));
    actions.addAll(Arrays.asList(display.getActions()));
    actions.addAll(Arrays.asList(preferencesDialog.getActions()));
    actions.addAll(Arrays.asList(anim.getActions()));
    actions.addAll(Arrays.asList(vib.getActions()));
    actions.addAll(Arrays.asList(crystprop.getActions()));
    actions.addAll(Arrays.asList(makecrystal.getActions()));
    actions.addAll(Arrays.asList(transform.getActions()));
    actions.addAll(Arrays.asList(pg.getActions()));

    return (Action[]) actions.toArray(new Action[0]);
  }

  /**
   * To shutdown when run as an application.  This is a
   * fairly lame implementation.   A more self-respecting
   * implementation would at least check to see if a save
   * was needed.
   */
  protected final class AppCloser extends WindowAdapter {

      public void windowClosing(WindowEvent e) {
          Jmol.this.doClose();
      }
  }

  private void doClose() {
      numWindows--;
      if (numWindows <= 0) {
          System.exit(0);
      } else {
          this.frame.dispose();
      }
  }

  
  /**
   * Find the hosting frame, for the file-chooser dialog.
   */
  protected Frame getFrame() {

    for (Container p = getParent(); p != null; p = p.getParent()) {
      if (p instanceof Frame) {
        return (Frame) p;
      }
    }
    return null;
  }

  /**
   * This is the hook through which all menu items are
   * created.  It registers the result with the menuitem
   * hashtable so that it can be fetched with getMenuItem().
   * @see #getMenuItem
   */
  protected JMenuItem createMenuItem(String cmd, boolean isRadio) {

      // System.out.println("Creating menu for " + cmd + " (radio:" + isRadio + ")");
      
    JMenuItem mi;
    if (isRadio) {
      mi = guimap.newJRadioButtonMenuItem("Jmol."+cmd);
    } else {
      String checked = JmolResourceHandler.getInstance().getString("Jmol."
                         + cmd + checkSuffix);
      if (checked != null) {
        boolean c = false;
        if (checked.equals("true")) {
          c = true;
        }
        mi = guimap.newJCheckBoxMenuItem("Jmol."+cmd, c);
      } else {
        mi = guimap.newJMenuItem("Jmol."+cmd);
      }
    }
    String mnem = JmolResourceHandler.getInstance().getString("Jmol." + cmd
                    + mnemonicSuffix);
    if (mnem != null) {
      char mn = mnem.charAt(0);
      mi.setMnemonic(mn);
    }

    ImageIcon f = JmolResourceHandler.getInstance().getIcon("Jmol." + cmd
                    + imageSuffix);
    if (f != null) {
      mi.setHorizontalTextPosition(JButton.RIGHT);
      mi.setIcon(f);
    }
    String astr = JmolResourceHandler.getInstance().getString("Jmol." + cmd
                    + actionSuffix);
    if (astr == null) {
      astr = cmd;
    }
    mi.setActionCommand(astr);
    Action a = getAction(astr);
    if (a != null) {
        // System.out.println("Connected to Action: " + a.getClass().getName());
      mi.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(mi));
      mi.setEnabled(a.isEnabled());
    } else {
      mi.setEnabled(false);
    }
    menuItems.put(cmd, mi);
    return mi;
  }

  /**
   * Fetch the menu item that was created for the given
   * command.
   * @param cmd  Name of the action.
   * @return item created for the given command or null
   *  if one wasn't created.
   */
  protected JMenuItem getMenuItem(String cmd) {
    return (JMenuItem) menuItems.get(cmd);
  }

  /**
   * Fetch the action that was created for the given
   * command.
   * @param cmd  Name of the action.
   */
  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  /**
   * Create the toolbar.  By default this reads the
   * resource file for the definition of the toolbars.
   */
  private Component createToolbar() {

    toolbar = new JToolBar();
    String[] tool1Keys =
      tokenize(JmolResourceHandler.getInstance().getString("Jmol.toolbar"));
    for (int i = 0; i < tool1Keys.length; i++) {
      if (tool1Keys[i].equals("-")) {
        toolbar.addSeparator();
      } else {
        toolbar.add(createTool(tool1Keys[i]));
      }
    }

    //Action handler implementation would go here.
    toolbar.add(Box.createHorizontalGlue());

    return toolbar;
  }

  /**
   * Hook through which every toolbar item is created.
   */
  protected Component createTool(String key) {
    return createToolbarButton(key);
  }

  /**
   * Create a button to go inside of the toolbar.  By default this
   * will load an image resource.  The image filename is relative to
   * the classpath (including the '.' directory if its a part of the
   * classpath), and may either be in a JAR file or a separate file.
   *
   * @param key The key in the resource file to serve as the basis
   *  of lookups.
   */
  protected AbstractButton createToolbarButton(String key) {

    ImageIcon ii = JmolResourceHandler.getInstance().getIcon("Jmol." + key
                     + imageSuffix);
    AbstractButton b = new JButton(ii);
    String isToggleString =
      JmolResourceHandler.getInstance().getString("Jmol." + key + "Toggle");
    if (isToggleString != null) {
      boolean isToggle = Boolean.valueOf(isToggleString).booleanValue();
      if (isToggle) {
        b = new JToggleButton(ii);
        if (key.equals("rotate"))
          buttonRotate = b;
        toolbarButtonGroup.add(b);
        String isSelectedString =
          JmolResourceHandler.getInstance().getString("Jmol." + key
            + "ToggleSelected");
        if (isSelectedString != null) {
          boolean isSelected =
            Boolean.valueOf(isSelectedString).booleanValue();
          b.setSelected(isSelected);
        }
      }
    }
    b.setRequestFocusEnabled(false);
    b.setMargin(new Insets(1, 1, 1, 1));

    String astr = JmolResourceHandler.getInstance().getString("Jmol." + key
                    + actionSuffix);
    if (astr == null) {
      astr = key;
    }
    Action a = getAction(astr);
    if (a != null) {

      // b = new JButton(a);
      b.setActionCommand(astr);
      b.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(b));
      b.setEnabled(a.isEnabled());
    } else {
      b.setEnabled(false);
    }

    String tip = JmolResourceHandler.getInstance().getString("Jmol." + key
                   + tipSuffix);
    if (tip != null) {
      b.setToolTipText(tip);
    }

    return b;
  }

  public static void setRotateButton() {
    if (buttonRotate != null)
      buttonRotate.setSelected(true);
  }

  /**
   * Take the given string and chop it up into a series
   * of strings on whitespace boundries.  This is useful
   * for trying to get an array of strings out of the
   * resource file.
   */
  protected String[] tokenize(String input) {

    Vector v = new Vector();
    StringTokenizer t = new StringTokenizer(input);
    String cmd[];

    while (t.hasMoreTokens()) {
      v.addElement(t.nextToken());
    }
    cmd = new String[v.size()];
    for (int i = 0; i < cmd.length; i++) {
      cmd[i] = (String) v.elementAt(i);
    }

    return cmd;
  }

  protected Component createStatusBar() {
    return new StatusBar();
  }

  /**
   * Create the menubar for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   */
  protected JMenuBar createMenubar() {

    JMenuItem mi;
    JMenuBar mb = new JMenuBar();

    String[] menuKeys =
      tokenize(JmolResourceHandler.getInstance().getString("Jmol.menubar"));
    for (int i = 0; i < menuKeys.length; i++) {
      if (menuKeys[i].equals("-")) {
        mb.add(Box.createHorizontalGlue());
      } else {
        JMenu m = createMenu(menuKeys[i], false);

        if (m != null) {
          mb.add(m);
        }
        String mnem = JmolResourceHandler.getInstance().getString("Jmol."
                        + menuKeys[i] + mnemonicSuffix);
        if (mnem != null) {
          char mn = mnem.charAt(0);
          m.setMnemonic(mn);
        }

      }
    }
    return mb;
  }

  /**
   * Create a menu for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   */
  protected JMenu createMenu(String key, boolean isPopup) {

    // Get list of items from resource file:
    String[] itemKeys;
    if (isPopup) {
      itemKeys = tokenize(JmolResourceHandler.getInstance().getString("Jmol."
          + key + popupSuffix));
    } else {
      itemKeys = tokenize(JmolResourceHandler.getInstance().getString("Jmol."
          + key));
    }

    // Get label associated with this menu:
    JMenu menu = guimap.newJMenu("Jmol."+key);

    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {

      // Check to see if it is a radio group:
      String radiogroup = JmolResourceHandler.getInstance().getString("Jmol."
                            + itemKeys[i] + radioSuffix);
      if (radiogroup != null) {

        // Get the list of items in the radio group:
        String[] radioKeys = tokenize(radiogroup);

        // See what is the selected member of the radio group:
        String si = JmolResourceHandler.getInstance().getString("Jmol."
                      + itemKeys[i] + selectedSuffix);

        // Create the button group:
        ButtonGroup bg = new ButtonGroup();

        // Loop over the items in the radio group:
        for (int j = 0; j < radioKeys.length; j++) {
          JRadioButtonMenuItem mi =
            (JRadioButtonMenuItem) createMenuItem(radioKeys[j], true);
          menu.add(mi);
          bg.add(mi);
          if (radioKeys[j].equals(si)) {
            mi.setSelected(true);
          }
        }
      } else if (itemKeys[i].equals("-")) {
        menu.addSeparator();
      } else {
        // Check to see if it is a popup menu:
        String popup = JmolResourceHandler.getInstance().
          getString("Jmol." + itemKeys[i] + popupSuffix);
        if (popup != null) {
          if (popup.equals("prop")) {
            apm =
              new AtomPropsMenu(JmolResourceHandler.getInstance()
                                .getString("Jmol." + itemKeys[i] + "Label"),
                                viewer);
            menu.add(apm);
          } else {
            JMenu pm;
            pm = createMenu(itemKeys[i], true);
            menu.add(pm);
          }
        } else {
          JMenuItem mi = createMenuItem(itemKeys[i], false);
          menu.add(mi);
        }
      }
    }
    menu.addMenuListener(display.getMenuListener());
    return menu;
  }


  private class ActionChangedListener implements PropertyChangeListener {

    AbstractButton button;

    ActionChangedListener(AbstractButton button) {
      super();
      this.button = button;
    }

    public void propertyChange(PropertyChangeEvent e) {

      String propertyName = e.getPropertyName();
      if (e.getPropertyName().equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        if (button.getText() != null) {
          button.setText(text);
        }
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        button.setEnabled(enabledState.booleanValue());
      }
    }
  }

  private Hashtable commands;
  private Hashtable menuItems;
  private JMenuBar menubar;
  private JToolBar toolbar;


  /**
   * Suffix applied to the key used in resource file
   * lookups for an image.
   */
  private static final String imageSuffix = "Image";

  /**
   * Suffix applied to the key used in resource file
   * lookups for a label.
   */
  public static final String labelSuffix = "Label";

  /**
   * Suffix applied to the key used in resource file
   * lookups for a checkbox menu item.
   */
  private static final String checkSuffix = "Check";

  /**
   * Suffix applied to the key used in resource file
   * lookups for a radio group.
   */
  private static final String radioSuffix = "Radio";

  /**
   * Suffix applied to the key used in resource file
   * lookups for a selected member of a radio group.
   */
  private static final String selectedSuffix = "Selected";

  /**
   * Suffix applied to the key used in resource file
   * lookups for a popup menu.
   */
  private static final String popupSuffix = "Popup";

  /**
   * Suffix applied to the key used in resource file
   * lookups for an action.
   */
  private static final String actionSuffix = "Action";

  /**
   * Suffix applied to the key used in resource file
   * lookups for tooltip text.
   */
  private static final String tipSuffix = "Tooltip";

  /**
   * Suffix applied to the key used in resource file
   * lookups for Mnemonics.
   */
  private static final String mnemonicSuffix = "Mnemonic";

  private static final String newwinAction = "newwin";
  private static final String openAction = "open";
  private static final String openurlAction = "openurl";
  private static final String newAction = "new";
  private static final String saveasAction = "saveas";
  private static final String exportActionProperty = "export";
  private static final String closeAction = "close";
  private static final String exitAction = "exit";
  private static final String aboutAction = "about";
  private static final String vibAction = "vibrate";
  private static final String whatsnewAction = "whatsnew";
  private static final String uguideAction = "uguide";
  private static final String atompropsAction = "atomprops";
  private static final String printActionProperty = "print";
  private static final String recentFilesAction = "recentFiles";
  private static final String povrayActionProperty = "povray";
  private static final String pdfActionProperty = "pdf";
  private static final String scriptAction = "script";


  // --- action implementations -----------------------------------

  private CalculateChemicalShifts chemicalShifts =
    new CalculateChemicalShifts();

  private SaveAction saveAction = new SaveAction();
  private ExportAction exportAction = new ExportAction();
  private PovrayAction povrayAction = new PovrayAction();
  private PdfAction pdfAction = new PdfAction();
  private PrintAction printAction = new PrintAction();
  private ViewMeasurementTableAction viewMeasurementTableAction
    = new ViewMeasurementTableAction();


  /**
   * Actions defined by the Jmol class
   */
  private Action[] defaultActions = {
    new NewAction(), new NewwinAction(), new OpenAction(),
    new OpenUrlAction(), saveAction, printAction, exportAction,
    new CloseAction(), new ExitAction(), new AboutAction(),
    new WhatsNewAction(),
    new UguideAction(), new AtompropsAction(), new ConsoleAction(),
    chemicalShifts, new RecentFilesAction(), povrayAction, pdfAction,
    new ScriptAction(), viewMeasurementTableAction
  };

  class CloseAction extends AbstractAction {
      CloseAction() {
          super(closeAction);
      }
      
      public void actionPerformed(ActionEvent e) {
          Jmol.this.frame.hide();
          Jmol.this.doClose();
      }
  }
  
  class ConsoleAction extends AbstractAction {

    public ConsoleAction() {
      super("console");
    }

    public void actionPerformed(ActionEvent e) {
      consoleframe.show();
    }

  }

  class AboutAction extends AbstractAction {

    public AboutAction() {
      super(aboutAction);
    }

    public void actionPerformed(ActionEvent e) {
      AboutDialog ad = new AboutDialog(frame);
      ad.show();
    }

  }

  class WhatsNewAction extends AbstractAction {

    public WhatsNewAction() {
      super(whatsnewAction);
    }

    public void actionPerformed(ActionEvent e) {
      WhatsNewDialog wnd = new WhatsNewDialog(frame);
      wnd.show();
    }
  }
  
  class NewwinAction extends AbstractAction {
      
      NewwinAction() {
          super(newwinAction);
      }
      
      public void actionPerformed(ActionEvent e) {
          JFrame newFrame = new JFrame();
          Jmol newJmol = new Jmol(null, newFrame, Jmol.this);
          newFrame.show();
      }
      
  }

  class UguideAction extends AbstractAction {

    public UguideAction() {
      super(uguideAction);
    }

    public void actionPerformed(ActionEvent e) {
      HelpDialog hd = new HelpDialog(frame);
      hd.show();
    }
  }

  class AtompropsAction extends AbstractAction {

    public AtompropsAction() {
      super(atompropsAction);
    }

    public void actionPerformed(ActionEvent e) {
      atomTypeTable.show();
    }
  }

  class PrintAction extends MoleculeDependentAction {

    public PrintAction() {
      super(printActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      print();
    }

  }

  /**
   * added print command, so that it can be used by RasmolScriptHandler
   **/
  public void print() {

    Toolkit tk = Toolkit.getDefaultToolkit();
    PrintJob pJob = tk.getPrintJob(frame, "Jmol Print Job", null);

    if (pJob != null) {
      Graphics pg = pJob.getGraphics();
      if (pg != null) {
        display.print(pg);

        // Flush the print job
        pg.dispose();
      }
      pJob.end();
    }
  
  }

  class OpenAction extends NewAction {

    OpenAction() {
      super(openAction);
    }

    public void actionPerformed(ActionEvent e) {

      int retval = openChooser.showOpenDialog(Jmol.this);
      if (retval == 0) {
        File file = openChooser.getSelectedFile();
        viewer.openFile(file.getAbsolutePath());
        return;
      }
    }
  }

  class OpenUrlAction extends NewAction {

    String title;
    String prompt;

    OpenUrlAction() {
      super(openurlAction);
      title = JmolResourceHandler.getInstance().getString("OpenUrl.title");
      prompt = JmolResourceHandler.getInstance().getString("OpenUrl.prompt");
    }

    public void actionPerformed(ActionEvent e) {
      String url = JOptionPane.showInputDialog(frame, prompt, title, 
                                               JOptionPane.PLAIN_MESSAGE);
      if (url != null) {
        if (url.indexOf("://") == -1)
          url = "http://" + url;
        viewer.openFile(url);
      }
      return;
    }
  }

  class NewAction extends AbstractAction {

    NewAction() {
      super(newAction);
    }

    NewAction(String nm) {
      super(nm);
    }

    public void actionPerformed(ActionEvent e) {
      revalidate();
    }
  }

  /**
   * Really lame implementation of an exit command
   */
  class ExitAction extends AbstractAction {

    ExitAction() {
      super(exitAction);
    }

    public void actionPerformed(ActionEvent e) {
      System.exit(0);
    }
  }

  class SaveAction extends MoleculeDependentAction {

    SaveAction() {
      super(saveasAction);
    }

    public void actionPerformed(ActionEvent e) {

      Frame frame = getFrame();
      int retval = saveChooser.showSaveDialog(Jmol.this);
      if (retval == 0) {
        File file = saveChooser.getSelectedFile();
        if (file != null) {
          try {
            ChemFile chemFile = (ChemFile)viewer.getClientFile();
            if (fileTyper.getType().equals("XYZ (xmol)")) {
                FileWriter writer = new FileWriter(file);
                int frameCount = chemFile.getNumberOfFrames();
                // note, that it only saves the first frame
                ChemFrame chemFrame = chemFile.getFrame(0);
                XYZWriter xyzwriter = new XYZWriter(writer);
                xyzwriter.write(chemFrame);
                xyzwriter.close();
            } else if (fileTyper.getType().equals("PDB")) {
                FileOutputStream os = new FileOutputStream(file);
                PdbSaver ps = new PdbSaver(chemFile, os);
                ps.writeFile();
                os.flush();
                os.close();
            } else if (fileTyper.getType().equals("CML")) {
                FileWriter writer = new FileWriter(file);
                int frameCount = chemFile.getNumberOfFrames();
                // note, that it only saves the first frame
                ChemFrame chemFrame = chemFile.getFrame(0);
                CMLWriter cmlwriter = new CMLWriter(writer);
                cmlwriter.write(chemFrame);
                cmlwriter.close();
            } else {
            }
          } catch (Exception exc) {
            status.setStatus(1, "Exception:");
            status.setStatus(2, exc.toString());
            exc.printStackTrace();
          }
          return;
        }
      }
    }

  }

  class ExportAction extends MoleculeDependentAction {

    ExportAction() {
      super(exportActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      Frame frame = getFrame();


      ImageTyper it = new ImageTyper(exportChooser);

      // GIF doesn't support more than 8 bits:
      if (viewer.getStyleAtom() == JmolViewer.SHADED) {
        it.disableGIF();
      }
      exportChooser.setAccessory(it);

      int retval = exportChooser.showSaveDialog(Jmol.this);
      if (retval == 0) {
        File file = exportChooser.getSelectedFile();

        if (file != null) {
          try {
            Image eImage = display.takeSnapshot();
            FileOutputStream os = new FileOutputStream(file);

            if (it.getType().equals("JPEG")) {
              int qual = 10 * it.getQuality();
              JpegEncoder jc = new JpegEncoder(eImage, qual, os);
              jc.Compress();
            } else if (it.getType().equals("PPM")) {
              PpmEncoder pc = new PpmEncoder(eImage, os);
              pc.encode();
            } else if (it.getType().equals("GIF")) {
              GifEncoder gc = new GifEncoder(eImage, os, true);
              gc.encode();
            } else if (it.getType().equals("PNG")) {
              PngEncoder png = new PngEncoder(eImage);
              byte[] pngbytes = png.pngEncode();
              os.write(pngbytes);
            } else if (it.getType().equals("BMP")) {
              BMPFile bmp = new BMPFile();
              bmp.saveBitmap(os, eImage);
            } else {

              // Do nothing
            }

            os.flush();
            os.close();

          } catch (IOException exc) {
            status.setStatus(1, "IO Exception:");
            status.setStatus(2, exc.toString());
            System.out.println(exc.toString());
          }
          return;
        }
      }
    }

  }

  class RecentFilesAction extends AbstractAction {

    public RecentFilesAction() {
      super(recentFilesAction);
    }

    public void actionPerformed(ActionEvent e) {

      recentFiles.show();
      String selection = recentFiles.getFile();
      if (selection != null) {
        viewer.openFile(selection);
      }
    }
  }

  class ScriptAction extends AbstractAction {

    public ScriptAction() {
      super(scriptAction);
    }

    public void actionPerformed(ActionEvent e) {
      scriptWindow.show();
    }
  }

  class PovrayAction extends MoleculeDependentAction {

    public PovrayAction() {
      super(povrayActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      if (currentFile != null) {
        currentFile.getName().substring(0,
            currentFile.getName().lastIndexOf("."));
      }
      new PovrayDialog(frame, viewer);
    }

  }

  class PdfAction extends MoleculeDependentAction {

    public PdfAction() {
      super(pdfActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      exportChooser.setAccessory(null);

      int retval = exportChooser.showSaveDialog(Jmol.this);
      if (retval == 0) {
        File file = exportChooser.getSelectedFile();

        if (file != null) {
          Document document = new Document();

          try {
            PdfWriter writer = PdfWriter.getInstance(document,
                                 new FileOutputStream(file));

            document.open();

            int w = display.getWidth();
            int h = display.getHeight();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(w, h);
            Graphics2D g2 = tp.createGraphics(w, h);
            g2.setStroke(new BasicStroke(0.1f));
            tp.setWidth(w);
            tp.setHeight(h);

            display.print(g2);
            g2.dispose();
            cb.addTemplate(tp, 72, 720 - h);
          } catch (DocumentException de) {
            System.err.println(de.getMessage());
          } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
          }

          document.close();
        }
      }
    }

  }

  class ViewMeasurementTableAction extends MoleculeDependentAction {

    public ViewMeasurementTableAction() {
      super("viewMeasurementTable");
    }

    public void actionPerformed(ActionEvent e) {
      measurementTable.activate();
    }
  }

  /**
   * Returns a new File referenced by the property 'user.dir', or null
   * if the property is not defined.
   *
   * @return  a File to the user directory
   */
  public static File getUserDirectory() {
    if (System.getProperty("user.dir") == null) {
      return null;
    }
    return new File(System.getProperty("user.dir"));
  }

  public static final String chemFileProperty = "chemFile";

  private abstract class MoleculeDependentAction extends AbstractAction
      implements PropertyChangeListener {

    public MoleculeDependentAction(String name) {
      super(name);
      setEnabled(false);
    }

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

  class MyJmolStatusListener implements JmolStatusListener {
    public void notifyFileLoaded(String fullPathName, String fileName,
                                 String modelName, Object clientFile) {
      System.out.println("notifyFileLoaded(" + fullPathName + "," +
                         fileName + "," + modelName + "," +
                         (clientFile != null));
      String title = modelName;
      if (title == null)
        title = fileName;
      if (title == null)
        title = "Jmol";
      frame.setTitle(title);
      recentFiles.notifyFileOpen(fullPathName);
      pcs.firePropertyChange(chemFileProperty, null, clientFile);
    }

    public void notifyFileNotLoaded(String fullPathName, String errorMsg) {
      JOptionPane.showMessageDialog(null,
                                    fullPathName + '\n' + errorMsg,
                                    "File not loaded",
                                    JOptionPane.ERROR_MESSAGE);
    }

    public void setStatusMessage(String statusMessage) {
      System.out.println("setStatusMessage:" + statusMessage);
    }

    public void scriptEcho(String strEcho) {
      if (scriptWindow != null)
        scriptWindow.scriptEcho(strEcho);
    }

    public void scriptStatus(String strStatus) {
      if (scriptWindow != null)
        scriptWindow.scriptStatus(strStatus);
    }

    public void notifyScriptTermination(String strStatus, int msWalltime) {
      if (scriptWindow != null)
        scriptWindow.notifyScriptTermination(strStatus, msWalltime);
    }

    public void handlePopupMenu(MouseEvent e) {
      jmolpopup.show(e.getComponent(), e.getX(), e.getY());
    }

    public void measureSelection(int atomIndex) {
      measurementTable.firePicked(atomIndex);
    }

    public void notifyMeasurementsChanged() {
      measurementTable.updateTables();
    }
  }

}
