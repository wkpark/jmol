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
package org.openscience.jmol.app.jmolpanel;

import org.jmol.api.*;
import org.jmol.console.ScriptEditor;
import org.jmol.export.dialog.Dialog;
import org.jmol.export.history.HistoryFile;
import org.jmol.export.image.ImageCreator;
import org.jmol.popup.JmolPopup;
import org.jmol.i18n.GT;
import org.jmol.util.*;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.ScriptContext;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.*;
import org.openscience.jmol.app.webexport.WebExport;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.awt.print.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import javax.swing.*;

import java.io.FileOutputStream;

public class JmolPanel extends JPanel implements SplashInterface {

  /**
   * The data model.
   */

  public JmolViewer viewer;
  JmolAdapter modelAdapter;
  JmolApp jmolApp;


  DisplayPanel display;
  StatusBar status;
  protected GaussianDialog gaussianDialog;
  private PreferencesDialog preferencesDialog;
  MeasurementTable measurementTable;
  RecentFilesDialog recentFiles;
  //private JMenu recentFilesMenu;
  public AppConsole appConsole;
  public ScriptEditor scriptEditor;
  public AtomSetChooser atomSetChooser;
  private ExecuteScriptAction executeScriptAction;
  protected JFrame frame;

  JmolPopup jmolpopup;
  String language;

  // private CDKPluginManager pluginManager;

  GuiMap guimap = new GuiMap();

  private static int numWindows = 0;
  private static Dimension screenSize = null;
  int startupWidth, startupHeight;

  PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  // Window names for the history file
  private final static String CONSOLE_WINDOW_NAME = "Console";
  private final static String EDITOR_WINDOW_NAME = "ScriptEditor";
  private final static String SCRIPT_WINDOW_NAME = "ScriptWindow";
  private final static String FILE_OPEN_WINDOW_NAME = "FileOpen";
  private final static String WEB_MAKER_WINDOW_NAME = "JmolWebPageMaker";


  /**
   * Button group for toggle buttons in the toolbar.
   */
  
  protected SplashInterface splash;

  protected JFrame consoleframe;
  
  String appletContext;
  
  String menuStructure;
  String menuFile;

  static HistoryFile historyFile;

  public JmolPanel(JmolApp jmolApp, Splash splash, JFrame frame, JmolPanel parent,
      int startupWidth, int startupHeight, String commandOptions, Point loc) {
    super(true);
    this.jmolApp = jmolApp;
    this.frame = frame;
    this.startupWidth = startupWidth;
    this.startupHeight = startupHeight;
    historyFile = jmolApp.historyFile;

    numWindows++;

    try {
      say("history file is " + historyFile.getFile().getAbsolutePath());
    } catch (Exception e) {
    }

    frame.setTitle("Jmol");
    frame.getContentPane().setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());

    this.splash = splash;

    setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());
    language = GT.getLanguage();

    status = (StatusBar) createStatusBar();
    say(GT._("Initializing 3D display..."));

    // only the SmarterJmolAdapter is allowed -- just send it in as null

    /*
     * String adapter = System.getProperty("model"); if (adapter == null ||
     * adapter.length() == 0) adapter = "smarter"; if
     * (adapter.equals("smarter")) { report("using Smarter Model Adapter");
     * modelAdapter = new SmarterJmolAdapter(); } else if
     * (adapter.equals("cdk")) {report(
     * "the CDK Model Adapter is currently no longer supported. Check out http://bioclipse.net/. -- using Smarter"
     * ); // modelAdapter = new CdkJmolAdapter(null); modelAdapter = new
     * SmarterJmolAdapter(); } else { report("unrecognized model adapter:" +
     * adapter + " -- using Smarter"); modelAdapter = new SmarterJmolAdapter();
     * }
     */
    
    /*
     * this version of Jmol needs to have a display so that it can 
     * construct JPG images -- if that is not needed, then you can
     * use JmolFrameless.jar
     * 
     */
    display = new DisplayPanel(this);
    viewer = JmolViewer.allocateViewer(display, modelAdapter, null, null, null,
        appletContext = commandOptions, new MyStatusListener());
//    if (jmolApp.haveDisplay)
      display.setViewer(viewer);
  //  else
    //  viewer.setScreenDimension(new Dimension(jmolApp.startupWidth,
      //    jmolApp.startupHeight));

    if (!jmolApp.haveDisplay)
      return;
    say(GT._("Initializing Preferences..."));
    preferencesDialog = new PreferencesDialog(this, frame, guimap, viewer);
    say(GT._("Initializing Recent Files..."));
    recentFiles = new RecentFilesDialog(frame);
    say(GT._("Initializing Script Window..."));
    appConsole = new AppConsole(viewer, frame);
    say(GT._("Initializing Script Editor..."));
    scriptEditor = new ScriptEditor(viewer, frame, appConsole);
    appConsole.setScriptEditor(scriptEditor);
    say(GT._("Initializing Measurements..."));
    measurementTable = new MeasurementTable(viewer, frame);


    // Setup Plugin system
    // say(GT._("Loading plugins..."));
    // pluginManager = new CDKPluginManager(
    // System.getProperty("user.home") + System.getProperty("file.separator")
    // + ".jmol", new JmolEditBus(viewer)
    // );
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DirBrowserPlugin");
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DadmlBrowserPlugin");
    // pluginManager.loadPlugins(
    // System.getProperty("user.home") + System.getProperty("file.separator")
    // + ".jmol/plugins"
    // );
    // feature to allow for globally installed plugins
    // if (System.getProperty("plugin.dir") != null) {
    // pluginManager.loadPlugins(System.getProperty("plugin.dir"));
    // }


    // install the command table
    say(GT._("Building Command Hooks..."));
    commands = new Hashtable();
    if (display != null) {
      Action[] actions = getActions();
      for (int i = 0; i < actions.length; i++) {
        Action a = actions[i];
        commands.put(a.getValue(Action.NAME), a);
      }
    }

    menuItems = new Hashtable();
    say(GT._("Building Menubar..."));
    executeScriptAction = new ExecuteScriptAction();
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

    say(GT._("Starting display..."));
    display.start();

    if (menuFile != null) {
      menuStructure = viewer.getFileAsString(menuFile);
    }
    jmolpopup = JmolPopup.newJmolPopup(viewer, true, menuStructure, true);

    // prevent new Jmol from covering old Jmol
    if (loc != null) {
      frame.setLocation(loc);
    } else if (parent != null) {
      Point location = parent.frame.getLocationOnScreen();
      int maxX = screenSize.width - 50;
      int maxY = screenSize.height - 50;

      location.x += 40;
      location.y += 40;
      if ((location.x > maxX) || (location.y > maxY)) {
        location.setLocation(0, 0);
      }
      frame.setLocation(location);
    }
    frame.getContentPane().add("Center", this);

    frame.addWindowListener(new JmolPanel.AppCloser());
    frame.pack();
    frame.setSize(startupWidth, startupHeight);
    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);

    // Repositionning windows
    if (appConsole != null)
      historyFile.repositionWindow(SCRIPT_WINDOW_NAME, appConsole, 200, 100);
    if (scriptEditor != null)
      historyFile.repositionWindow(EDITOR_WINDOW_NAME, scriptEditor, 150, 50);

    say(GT._("Setting up Drag-and-Drop..."));
    FileDropper dropper = new FileDropper();
    final JFrame f = frame;
    dropper.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        // System.out.println("Drop triggered...");
        f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (evt.getPropertyName().equals(FileDropper.FD_PROPERTY_FILENAME)) {
          final String filename = evt.getNewValue().toString();
          viewer.openFileAsynchronously(filename);
        } else if (evt.getPropertyName().equals(FileDropper.FD_PROPERTY_INLINE)) {
          final String inline = evt.getNewValue().toString();
          viewer.openStringInline(inline);
        }
        f.setCursor(Cursor.getDefaultCursor());
      }
    });

    this.setDropTarget(new DropTarget(this, dropper));
    this.setEnabled(true);

    say(GT._("Launching main frame..."));
  }

  protected static void startJmol(JmolApp jmolApp) {
    
    Dialog.setupUIManager();
    
    JFrame jmolFrame = new JFrame();
    
    // now pass these to viewer
    
    Jmol jmol = null;
    
    try {
      if (jmolApp.jmolPosition != null) {
        jmolFrame.setLocation(jmolApp.jmolPosition);
      }
      
      jmol = getJmol(jmolApp, jmolFrame);

      jmolApp.startViewer(jmol.viewer, jmol.splash);
    
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    if (jmolApp.haveConsole) {
      // Adding console frame to grab System.out & System.err
      jmol.consoleframe = new JFrame(GT._("Jmol Console"));
      jmol.consoleframe.setIconImage(jmol.frame.getIconImage());
      try {
        final ConsoleTextArea consoleTextArea = new ConsoleTextArea(true);
        consoleTextArea.setFont(java.awt.Font.decode("monospaced"));
        jmol.consoleframe.getContentPane().add(new JScrollPane(consoleTextArea),
            java.awt.BorderLayout.CENTER);
        if (Boolean.getBoolean("clearConsoleButton")) {
          JButton buttonClear = new JButton(GT._("Clear"));
          buttonClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              consoleTextArea.setText("");
            }
          });
          jmol.consoleframe.getContentPane().add(buttonClear,
              java.awt.BorderLayout.SOUTH);
        }
      } catch (IOException e) {
        JTextArea errorTextArea = new JTextArea();
        errorTextArea.setFont(java.awt.Font.decode("monospaced"));
        jmol.consoleframe.getContentPane().add(new JScrollPane(errorTextArea),
            java.awt.BorderLayout.CENTER);
        errorTextArea.append(GT._("Could not create ConsoleTextArea: ") + e);
      }
      setWindow(CONSOLE_WINDOW_NAME, jmol.consoleframe, jmol);     
    }
  }

  public static Jmol getJmol(JmolApp jmolApp, JFrame frame) {

    String commandOptions = jmolApp.commandOptions;
    Splash splash = null;
    if (jmolApp.haveDisplay && jmolApp.splashEnabled) {
      ImageIcon splash_image = JmolResourceHandler.getIconX("splash");
      if (!jmolApp.isSilent)
        Logger.info("splash_image=" + splash_image);
      splash = new Splash((commandOptions != null
          && commandOptions.indexOf("-L") >= 0 ? null : frame), splash_image);
      splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
      splash.showStatus(GT._("Creating main window..."));
      splash.showStatus(GT._("Initializing Swing..."));
    }
    try {
      UIManager
          .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception exc) {
      System.err.println("Error loading L&F: " + exc);
    }

    screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    if (splash != null)
      splash.showStatus(GT._("Initializing Jmol..."));

    Jmol window = new Jmol(jmolApp, splash, frame, null, jmolApp.startupWidth,
        jmolApp.startupHeight, commandOptions, null);
    if (jmolApp.haveDisplay)
      frame.setVisible(true);
    return window;
  }

  /*
   * Convenient method to get values of UIManager strings private static void
   * analyzeUIManagerString(String name, String value) {
   * System.err.println(name); System.err.println(" en=[" +
   * UIManager.getString(name) + "]"); System.err.println(" de=[" +
   * UIManager.getString(name, Locale.GERMAN) + "]"); System.err.println(" es=["
   * + UIManager.getString(name, new Locale("es")) + "]");
   * System.err.println(" fr=[" + UIManager.getString(name, Locale.FRENCH) +
   * "]"); UIManager.put(name, value); }
   */

  private static void setWindow(String name,
                                          JFrame frame, JmolPanel jmol) {
    Point location = jmol.frame.getLocation();
    Dimension size = jmol.frame.getSize();
    Dimension consoleSize = historyFile.getWindowSize(name);
    Point consolePosition = historyFile
        .getWindowPosition(name);
    if ((consoleSize != null) && (consolePosition != null)) {
      frame.setBounds(consolePosition.x, consolePosition.y,
          consoleSize.width, consoleSize.height);
    } else {
      frame.setBounds(location.x, location.y + size.height,
          size.width, 200);
    }

    Boolean consoleVisible = historyFile
        .getWindowVisibility(name);
    if ((consoleVisible != null) && (consoleVisible.equals(Boolean.TRUE))) {
      frame.setVisible(true);
    }
  }

  public void showStatus(String message) {
    splash.showStatus(message);    
  }

  void report(String str) {
    if (jmolApp.isSilent)
      return;
    Logger.info(str);
  }

  private void say(String message) {
    if (jmolApp.haveDisplay)
      if (splash == null) {
        report(message);
      } else {
        splash.showStatus(message);
      }
  }

  /**
   * @return A list of Actions that is understood by the upper level
   * application
   */
  public Action[] getActions() {

    ArrayList actions = new ArrayList();
    actions.addAll(Arrays.asList(defaultActions));
    actions.addAll(Arrays.asList(display.getActions()));
    actions.addAll(Arrays.asList(preferencesDialog.getActions()));
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
      JmolPanel.this.doClose();
    }
  }

  void doClose() {
    // Save window positions and status in the history
    if (historyFile != null) {
      if (display != null) {
        jmolApp.border.x = this.getFrame().getWidth() - display.dimSize.width;
        jmolApp.border.y = this.getFrame().getHeight() - display.dimSize.height;
        historyFile.addWindowInfo("Jmol", this.frame, jmolApp.border);
      }
      //System.out.println("doClose border: " + border);
      //historyFile.addWindowInfo(CONSOLE_WINDOW_NAME, consoleframe);
    }
    dispose(this.frame);
  }

  private void dispose(JFrame f) {
    if (historyFile != null && appConsole != null)
      historyFile.addWindowInfo(SCRIPT_WINDOW_NAME, appConsole, null);
    if (historyFile != null && scriptEditor != null)
      historyFile.addWindowInfo(EDITOR_WINDOW_NAME, scriptEditor, null);
    if (historyFile != null && webExport != null) {
      WebExport.saveHistory();
      WebExport.cleanUp();
    }
    if (numWindows <= 1) {
      // Close Jmol
      report(GT._("Closing Jmol..."));
      // pluginManager.closePlugins();
      System.exit(0);
    } else {
      numWindows--;
      viewer.setModeMouse(JmolConstants.MOUSE_NONE);
      try {
        f.dispose();
        if (appConsole != null) {
          appConsole.dispose();
        }
        if (scriptEditor != null) {
          scriptEditor.dispose();
        }
      } catch (Exception e) {
        System.out.println("frame disposal exception");
        // ignore
      }
    }
  }

  protected void setupNewFrame(String state) {
    JFrame newFrame = new JFrame();
    JFrame f = this.frame;
    Jmol j = new Jmol(jmolApp, null, newFrame, (Jmol) this, startupWidth, startupHeight,
        "", (state == null ? null : f.getLocationOnScreen()));
    newFrame.setVisible(true);
    if (state != null) {
      dispose(f);
      j.viewer.evalStringQuiet(state);
    }
  }

  /**
   * @return The hosting frame, for the file-chooser dialog.
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
   * @param cmd
   * @return Menu item created
   * @see #getMenuItem
   */
  protected JMenuItem createMenuItem(String cmd) {

    JMenuItem mi;
    if (cmd.endsWith("Check")) {
      mi = guimap.newJCheckBoxMenuItem(cmd, false);
    } else {
      mi = guimap.newJMenuItem(cmd);
    }

    ImageIcon f = JmolResourceHandler.getIconX(cmd + "Image");
    if (f != null) {
      mi.setHorizontalTextPosition(SwingConstants.RIGHT);
      mi.setIcon(f);
    }

    if (cmd.endsWith("Script")) {
      mi.setActionCommand(JmolResourceHandler.getStringX(cmd));
      mi.addActionListener(executeScriptAction);
    } else {
      mi.setActionCommand(cmd);
      Action a = getAction(cmd);
      if (a != null) {
        mi.addActionListener(a);
        a.addPropertyChangeListener(new ActionChangedListener(mi));
        mi.setEnabled(a.isEnabled());
      } else {
        mi.setEnabled(false);
      }
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
   * @return The action
   */
  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  /**
   * Create the toolbar.  By default this reads the
   * resource file for the definition of the toolbars.
   * @return The toolbar
   */
  private Component createToolbar() {

    toolbar = new JToolBar();
    String[] tool1Keys = tokenize(JmolResourceHandler.getStringX("toolbar"));
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
   * @param key
   * @return Toolbar item
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
   * @return Button
   */
  protected AbstractButton createToolbarButton(String key) {

    ImageIcon ii = JmolResourceHandler.getIconX(key + "Image");
    AbstractButton b = new JButton(ii);
    String isToggleString = JmolResourceHandler.getStringX(key + "Toggle");
    if (isToggleString != null) {
      boolean isToggle = Boolean.valueOf(isToggleString).booleanValue();
      if (isToggle) {
        b = new JToggleButton(ii);
        if (key.equals("rotate"))
          display.buttonRotate = (JToggleButton) b;
        display.toolbarButtonGroup.add(b);
        String isSelectedString = JmolResourceHandler.getStringX(key
            + "ToggleSelected");
        if (isSelectedString != null) {
          boolean isSelected = Boolean.valueOf(isSelectedString).booleanValue();
          b.setSelected(isSelected);
        }
      }
    }
    b.setRequestFocusEnabled(false);
    b.setMargin(new Insets(1, 1, 1, 1));

    Action a = null;
    String actionCommand = null;
    if (key.endsWith("Script")) {
      actionCommand = JmolResourceHandler.getStringX(key);
      a = executeScriptAction;
    } else {
      actionCommand = key;
      a = getAction(key);
    }
    if (a != null) {
      b.setActionCommand(actionCommand);
      b.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(b));
      b.setEnabled(a.isEnabled());
    } else {
      b.setEnabled(false);
    }

    String tip = guimap.getLabel(key + "Tip");
    if (tip != null) {
      b.setToolTipText(tip);
    }

    return b;
  }

  /**
   * Take the given string and chop it up into a series
   * of strings on whitespace boundries.  This is useful
   * for trying to get an array of strings out of the
   * resource file.
   * @param input String to chop
   * @return Strings chopped on whitespace boundries
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
   * @return Menubar
   */
  protected JMenuBar createMenubar() {
    JMenuBar mb = new JMenuBar();
    addNormalMenuBar(mb);
    // The Macros Menu
    addMacrosMenuBar(mb);
    // The Plugin Menu
    // if (pluginManager != null) {
    //     mb.add(pluginManager.getMenu());
    // }
    // The Help menu, right aligned
    mb.add(Box.createHorizontalGlue());
    addHelpMenuBar(mb);
    return mb;
  }

  protected void addMacrosMenuBar(JMenuBar menuBar) {
    // ok, here needs to be added the funny stuff
    JMenu macroMenu = guimap.newJMenu("macros");
    File macroDir = new File(System.getProperty("user.home")
        + System.getProperty("file.separator") + ".jmol"
        + System.getProperty("file.separator") + "macros");
    report("User macros dir: " + macroDir);
    report("       exists: " + macroDir.exists());
    report("  isDirectory: " + macroDir.isDirectory());
    if (macroDir.exists() && macroDir.isDirectory()) {
      File[] macros = macroDir.listFiles();
      for (int i = 0; i < macros.length; i++) {
        // loop over these files and load them
        String macroName = macros[i].getName();
        if (macroName.endsWith(".macro")) {
          if (Logger.debugging) {
            Logger.debug("Possible macro found: " + macroName);
          }
          FileInputStream macro = null;
          try {
            macro = new FileInputStream(macros[i]);
            Properties macroProps = new Properties();
            macroProps.load(macro);
            String macroTitle = macroProps.getProperty("Title");
            String macroScript = macroProps.getProperty("Script");
            JMenuItem mi = new JMenuItem(macroTitle);
            mi.setActionCommand(macroScript);
            mi.addActionListener(executeScriptAction);
            macroMenu.add(mi);
          } catch (IOException exception) {
            System.err.println("Could not load macro file: ");
            System.err.println(exception);
          } finally {
            if (macro != null) {
              try {
                macro.close();
              } catch (IOException e) {
                // Nothing
              }
              macro = null;
            }
          }
        }
      }
    }
    menuBar.add(macroMenu);
  }

  protected void addNormalMenuBar(JMenuBar menuBar) {
    String[] menuKeys = tokenize(JmolResourceHandler.getStringX("menubar"));
    for (int i = 0; i < menuKeys.length; i++) {
      if (menuKeys[i].equals("-")) {
        menuBar.add(Box.createHorizontalGlue());
      } else {
        JMenu m = createMenu(menuKeys[i]);
        if (m != null)
          menuBar.add(m);
      }
    }
  }

  protected void addHelpMenuBar(JMenuBar menuBar) {
    String menuKey = "help";
    JMenu m = createMenu(menuKey);
    if (m != null) {
      menuBar.add(m);
    }
  }

  /**
   * Create a menu for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   * @param key
   * @return Menu created
   */
  protected JMenu createMenu(String key) {

    // Get list of items from resource file:
    String[] itemKeys = tokenize(JmolResourceHandler.getStringX(key));

    // Get label associated with this menu:
    JMenu menu = guimap.newJMenu(key);
    ImageIcon f = JmolResourceHandler.getIconX(key + "Image");
    if (f != null) {
      menu.setHorizontalTextPosition(SwingConstants.RIGHT);
      menu.setIcon(f);
    }

    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {

      String item = itemKeys[i];
      if (item.equals("-")) {
        menu.addSeparator();
        continue;
      }
      if (item.endsWith("Menu")) {
        JMenu pm;
        if ("recentFilesMenu".equals(item)) {
          /*recentFilesMenu = */pm = createMenu(item);
        } else {
          pm = createMenu(item);
        }
        menu.add(pm);
        continue;
      }
      JMenuItem mi = createMenuItem(item);
      menu.add(mi);
    }
    menu.addMenuListener(display.getMenuListener());
    return menu;
  }

  private static class ActionChangedListener implements PropertyChangeListener {

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

  // these correlate with items xxx in GuiMap.java 
  // that have no associated xxxScript property listed
  // in org.openscience.jmol.Properties.Jmol-resources.properties

  private static final String newwinAction = "newwin";
  private static final String openAction = "open";
  private static final String openurlAction = "openurl";
  private static final String newAction = "new";
  //private static final String saveasAction = "saveas";
  private static final String exportActionProperty = "export";
  private static final String closeAction = "close";
  private static final String exitAction = "exit";
  private static final String aboutAction = "about";
  //private static final String vibAction = "vibrate";
  private static final String whatsnewAction = "whatsnew";
  private static final String uguideAction = "uguide";
  private static final String printActionProperty = "print";
  private static final String recentFilesAction = "recentFiles";
  private static final String povrayActionProperty = "povray";
  private static final String writeActionProperty = "write";
  private static final String editorAction = "editor";
  private static final String consoleAction = "console";
  private static final String toWebActionProperty = "toweb";
  private static final String atomsetchooserAction = "atomsetchooser";
  private static final String copyImageActionProperty = "copyImage";
  private static final String copyScriptActionProperty = "copyScript";
  private static final String pasteClipboardActionProperty = "pasteClipboard";
  private static final String gaussianAction = "gauss";

  // --- action implementations -----------------------------------

  private ExportAction exportAction = new ExportAction();
  private PovrayAction povrayAction = new PovrayAction();
  private ToWebAction toWebAction = new ToWebAction();
  private WriteAction writeAction = new WriteAction();
  private PrintAction printAction = new PrintAction();
  private CopyImageAction copyImageAction = new CopyImageAction();
  private CopyScriptAction copyScriptAction = new CopyScriptAction();
  private PasteClipboardAction pasteClipboardAction = new PasteClipboardAction();
  private ViewMeasurementTableAction viewMeasurementTableAction = new ViewMeasurementTableAction();

  int qualityJPG = -1;
  int qualityPNG = -1;
  String imageType;

  /**
   * Actions defined by the Jmol class
   */
  private Action[] defaultActions = { new NewAction(), new NewwinAction(),
      new OpenAction(), new OpenUrlAction(), printAction, exportAction,
      new CloseAction(), new ExitAction(), copyImageAction, copyScriptAction,
      pasteClipboardAction, new AboutAction(), new WhatsNewAction(),
      new UguideAction(), new ConsoleAction(),  
      new RecentFilesAction(), povrayAction, writeAction, toWebAction, 
      new ScriptWindowAction(), new ScriptEditorAction(),
      new AtomSetChooserAction(), viewMeasurementTableAction, 
      new GaussianAction() }
  ;

  class CloseAction extends AbstractAction {
    CloseAction() {
      super(closeAction);
    }

    public void actionPerformed(ActionEvent e) {
      JmolPanel.this.frame.setVisible(false);
      JmolPanel.this.doClose();
    }
  }

  class ConsoleAction extends AbstractAction {

    public ConsoleAction() {
      super("jconsole");
    }

    public void actionPerformed(ActionEvent e) {
      if (consoleframe != null)
        consoleframe.setVisible(true);
    }

  }

  class AboutAction extends AbstractAction {

    public AboutAction() {
      super(aboutAction);
    }

    public void actionPerformed(ActionEvent e) {
      AboutDialog ad = new AboutDialog(frame);
      ad.setVisible(true);
    }

  }

  class WhatsNewAction extends AbstractAction {

    public WhatsNewAction() {
      super(whatsnewAction);
    }

    public void actionPerformed(ActionEvent e) {
      WhatsNewDialog wnd = new WhatsNewDialog(frame);
      wnd.setVisible(true);
    }
  }

  class GaussianAction extends AbstractAction {
    public GaussianAction() {
      super(gaussianAction);
    }
    
    public void actionPerformed(ActionEvent e) {
      if (gaussianDialog == null)
        gaussianDialog = new GaussianDialog(frame, viewer);
      gaussianDialog.setVisible(true);
    }
  }
    
  class NewwinAction extends AbstractAction {

    NewwinAction() {
      super(newwinAction);
    }

    public void actionPerformed(ActionEvent e) {
      JFrame newFrame = new JFrame();
      new Jmol(jmolApp, null, newFrame, (Jmol) JmolPanel.this, startupWidth, startupHeight, "", null);
      newFrame.setVisible(true);
    }

  }

  class UguideAction extends AbstractAction {

    public UguideAction() {
      super(uguideAction);
    }

    public void actionPerformed(ActionEvent e) {
      (new HelpDialog(frame)).setVisible(true);
    }
  }

  class PasteClipboardAction extends AbstractAction {

    public PasteClipboardAction() {
      super(pasteClipboardActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      String str = ImageCreator.getClipboardTextStatic();
      if (str != null && str.length() > 0)
        viewer.loadInline(str, false);
    }
  }

  /**
   * An Action to copy the current image into the clipboard. 
   */
  class CopyImageAction extends AbstractAction {

    public CopyImageAction() {
      super(copyImageActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      (new ImageCreator(viewer)).clipImage(null);
    }
  }

  class CopyScriptAction extends AbstractAction {

    public CopyScriptAction() {
      super(copyScriptActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      (new ImageCreator(viewer)).clipImage((String) viewer.getProperty(
          "string", "stateInfo", null));
    }
  }

  class PrintAction extends AbstractAction {

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

    PrinterJob job = PrinterJob.getPrinterJob();
    job.setPrintable(display);
    if (job.printDialog()) {
      try {
        job.print();
      } catch (PrinterException e) {
        Logger.error("Error while printing", e);
      }
    }
  }

  class OpenAction extends NewAction {

    OpenAction() {
      super(openAction);
    }

    public void actionPerformed(ActionEvent e) {
      String fileName = getOpenFileNameFromDialog(null);
      if (fileName == null)
        return;
      if (fileName.startsWith("load append"))
        viewer.scriptWait(fileName);
      else
        viewer.openFileAsynchronously(fileName);
    }
  }

  class OpenUrlAction extends NewAction {

    String title;
    String prompt;

    OpenUrlAction() {
      super(openurlAction);
      title = GT._("Open URL");
      prompt = GT._("Enter URL of molecular model");
    }

    public void actionPerformed(ActionEvent e) {
      String url = JOptionPane.showInputDialog(frame, prompt, title,
          JOptionPane.PLAIN_MESSAGE);
      if (url != null) {
        if (url.indexOf("://") == -1)
          url = "http://" + url;
        viewer.openFileAsynchronously(url);
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
      JmolPanel.this.doClose();
    }
  }

  final static String[] imageChoices = { "JPEG", "PNG", "GIF", "PPM", "PDF" };
  final static String[] imageExtensions = { "jpg", "png", "gif", "ppm", "pdf" };

  class ExportAction extends AbstractAction {

    ExportAction() {
      super(exportActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      Dialog sd = new Dialog();
      String fileName = sd.getImageFileNameFromDialog(viewer, null, imageType,
          imageChoices, imageExtensions, qualityJPG, qualityPNG);
      if (fileName == null)
        return;
      qualityJPG = sd.getQuality("JPG");
      qualityPNG = sd.getQuality("PNG");
      String sType = imageType = sd.getType();
      if (sType == null) {
        // file type changer was not touched
        sType = fileName;
        int i = sType.lastIndexOf(".");
        if (i < 0)
          return; // make no assumptions - require a type by extension
        sType = sType.substring(i + 1).toUpperCase();
      }
      String msg = (sType.equals("PDF") ?createPdfDocument(new File(fileName))
          : createImageStatus(fileName, sType, (String) null, sd.getQuality(sType)));
      Logger.info(msg);
    }

    private String createPdfDocument(File file) {
      // PDF is application-only
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
        return de.getMessage();
      } catch (IOException ioe) {
        return ioe.getMessage();
      }
      document.close();
      return "OK PDF " + file.length() + " " + file.getAbsolutePath();
    }

  }

  class RecentFilesAction extends AbstractAction {

    public RecentFilesAction() {
      super(recentFilesAction);
    }

    public void actionPerformed(ActionEvent e) {

      recentFiles.setVisible(true);
      String selection = recentFiles.getFile();
      if (selection != null)
        viewer.openFileAsynchronously(selection);
    }
  }

  class ScriptWindowAction extends AbstractAction {

    public ScriptWindowAction() {
      super(consoleAction);
    }

    public void actionPerformed(ActionEvent e) {
      if (appConsole != null)
        appConsole.setVisible(true);
    }
  }

  class ScriptEditorAction extends AbstractAction {

    public ScriptEditorAction() {
      super(editorAction);
    }

    public void actionPerformed(ActionEvent e) {
      if (scriptEditor != null)
        scriptEditor.setVisible(true);
    }
  }

  class AtomSetChooserAction extends AbstractAction {
    public AtomSetChooserAction() {
      super(atomsetchooserAction);
    }

    public void actionPerformed(ActionEvent e) {
      atomSetChooser.setVisible(true);
    }
  }

  class PovrayAction extends AbstractAction {

    public PovrayAction() {
      super(povrayActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      new PovrayDialog(frame, viewer);
    }

  }

  class WriteAction extends AbstractAction {

    public WriteAction() {
      super(writeActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      String fileName = (new Dialog()).getSaveFileNameFromDialog(viewer,
          null, "SPT");
      if (fileName != null)
        Logger.info(createImageStatus(fileName, "SPT", viewer.getStateInfo(),
            Integer.MIN_VALUE));
    }
  }

  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return          null (canceled) or a message starting with OK or an error message
   */
  String createImageStatus(String fileName, String type, Object text_or_bytes,
                           int quality) {
    String msg = (String) (new ImageCreator(viewer)).createImage(fileName,
        type, text_or_bytes, quality);
    if (msg == null || msg.startsWith("OK"))
      return msg;
    if (status != null) {
      status.setStatus(1, GT._("IO Exception:"));
      status.setStatus(2, msg);
    }
    return msg;
  }

  WebExport webExport;

  class ToWebAction extends AbstractAction {

    public ToWebAction() {
      super(toWebActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          webExport = WebExport.createAndShowGUI(viewer, historyFile,
              WEB_MAKER_WINDOW_NAME);
        }
      });
    }
  }

  class ViewMeasurementTableAction extends AbstractAction {

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
    String dir = System.getProperty("user.dir");
    return dir == null ? null : new File(System.getProperty("user.dir"));
  }

  String getOpenFileNameFromDialog(String fileName) {
    return (new Dialog()).getOpenFileNameFromDialog(appletContext,
        viewer, fileName, historyFile, FILE_OPEN_WINDOW_NAME, (fileName == null));
  }

  void showEditor(boolean showEditor, String text) {
    if (scriptEditor == null)
      return;
    if (showEditor) {
      scriptEditor.setVisible(true);
      if (text != null)
        scriptEditor.output(text);
    } else {
      scriptEditor.setVisible(false);
    }
  }


  public static final String chemFileProperty = "chemFile";

  class MyStatusListener implements JmolStatusListener {

    /* starting with Jmol 11.7.27, JmolStatusListener extends JmolCallbackListener
     * 
     * providing a simpler interface if all that is wanted is callback functionality.
     * 
     * Only three methods are involved:
     * 
     * boolean notifyEnabled(int type) 
     *   -- lets the statusManager know if there is an implementation
     *      of a given callback type
     * 
     * void notifyCallback(int type, Object[] data)
     *   -- callback action; data varies with callback type 
     *   -- see org.jmol.viewer.StatusManager for details
     *   
     * void setCallbackFunction(String callbackType, String callbackFunction)
     *   -- called by statusManager in response to the 
     *      "set callback" script command
     *   -- also used by the Jmol application to change menus and languages
     *   -- can remain unimplemented if no such user action is intended
     * 
     */
    
    /// JmolCallbackListener interface ///

    public boolean notifyEnabled(int type) {
      switch (type) {
      case JmolConstants.SHOW_EDITOR:
      case JmolConstants.CALLBACK_ANIMFRAME:
      case JmolConstants.CALLBACK_ECHO:
      case JmolConstants.CALLBACK_LOADSTRUCT:
      case JmolConstants.CALLBACK_MEASURE:
      case JmolConstants.CALLBACK_MESSAGE:
      case JmolConstants.CALLBACK_PICK:
      case JmolConstants.CALLBACK_SCRIPT:
        return true;
      case JmolConstants.CALLBACK_ERROR:
      case JmolConstants.CALLBACK_HOVER:
      case JmolConstants.CALLBACK_MINIMIZATION:
      case JmolConstants.CALLBACK_RESIZE:
      case JmolConstants.CALLBACK_SYNC:
      //applet only (but you could change this for your listener)
      }
      return false;
    }

    public void notifyCallback(int type, Object[] data) {
      String strInfo = (data == null || data[1] == null ? null : data[1]
          .toString());
      switch (type) {
      case JmolConstants.SHOW_EDITOR:
        showEditor(true, strInfo);
        return;
      case JmolConstants.CALLBACK_LOADSTRUCT:
        notifyFileLoaded(strInfo, (String) data[2], (String) data[3],
            (String) data[4]);
        break;
      case JmolConstants.CALLBACK_ANIMFRAME:
        int[] iData = (int[]) data[1];
        notifyFrameChanged(iData[0], iData[1], iData[2]);
        break;
      case JmolConstants.CALLBACK_ECHO:
        sendConsoleEcho(strInfo);
        break;
      case JmolConstants.CALLBACK_MEASURE:
        String status = (String) data[3]; 
        if (status.indexOf("Picked") >= 0) //picking mode
          notifyAtomPicked(strInfo);
        else if (status.indexOf("Completed") >= 0)
          sendConsoleEcho(strInfo.substring(strInfo.lastIndexOf(",") + 2,
              strInfo.length() - 1));
        if (status.indexOf("Pending") < 0) {
          //System.out.println("jmol callback measure" + status); 
          measurementTable.updateTables();
        }
        break;
      case JmolConstants.CALLBACK_MESSAGE:
        sendConsoleMessage(data == null ? null : strInfo);
        break;
      case JmolConstants.CALLBACK_PICK:
        notifyAtomPicked(strInfo);
        break;
      case JmolConstants.CALLBACK_SCRIPT:
        int msWalltime = ((Integer) data[3]).intValue();
        if (appConsole != null) {
          // general message has msWalltime = 0
          // special messages have msWalltime < 0
          // termination message has msWalltime > 0 (1 + msWalltime)
          // "script started"/"pending"/"script terminated"/"script completed"
          //   do not get sent to console
          if (msWalltime == 0) {
            appConsole.sendConsoleMessage(strInfo);
            if (data[2] != null && display != null)
                display.status.setStatus(1, (String) data[2]);  
          }
        }
        if (scriptEditor != null) {
          // general message has msWalltime = 0
          // special messages have msWalltime < 0
          // termination message has msWalltime > 0 (1 + msWalltime)
          // "script started"/"pending"/"script terminated"/"script completed"
          //   do not get sent to console
          if (msWalltime > 0) {
            // termination -- button legacy
            scriptEditor.notifyScriptTermination();
          } else if (msWalltime < 0) {
            if (msWalltime == -2)
              scriptEditor.notifyScriptStart();
          } else if (scriptEditor.isVisible() && ((String) data[2]).length() > 0 || data[4] != null) {
            System.out.println("Jmol notifyContext " + data[1]);
            scriptEditor.notifyContext((ScriptContext)viewer.getProperty("DATA_API", "scriptContext", null), data);
          }
        }
        break;
      case JmolConstants.CALLBACK_RESIZE:
      case JmolConstants.CALLBACK_SYNC:
      case JmolConstants.CALLBACK_HOVER:
      case JmolConstants.CALLBACK_MINIMIZATION:
        break;
      }
    }

    public void setCallbackFunction(String callbackType, String callbackFunction) {
      if (callbackType.equalsIgnoreCase("menu")) {
        menuStructure = callbackFunction;
        menuFile = null;
        setupNewFrame(viewer.getStateInfo());
        return;
      }
      if (callbackType.equalsIgnoreCase("language")) {
        new GT(callbackFunction);
        language = GT.getLanguage();
        Dialog.setupUIManager();
        if (webExport != null) {
          WebExport.saveHistory();
          WebExport.dispose();
          webExport = WebExport.createAndShowGUI(viewer, historyFile,
              WEB_MAKER_WINDOW_NAME);
        }
        setupNewFrame(viewer.getStateInfo());
      }
    }
    
    /// end of JmolCallbackListener interface ///

    public String eval(String strEval) {
      if (strEval.startsWith("_GET_MENU"))
        return (jmolpopup == null ? "" : jmolpopup.getMenu("Jmol version "
            + Viewer.getJmolVersion() + "|" + strEval));
      sendConsoleMessage("javascript: " + strEval);
      return "# 'eval' is implemented only for the applet.";
    }

    /**
     * 
     * @param fileName
     * @param type
     * @param text_or_bytes
     * @param quality
     * @return          null (canceled) or a message starting with OK or an error message
     */
    public String createImage(String fileName, String type, Object text_or_bytes,
                              int quality) {
      return createImageStatus(fileName, type, text_or_bytes, quality);
    }

    private void notifyAtomPicked(String info) {
      if (appConsole != null) {
        appConsole.sendConsoleMessage(info);
        appConsole.sendConsoleMessage("\n");
      }
    }

    private void notifyFileLoaded(String fullPathName, String fileName,
                                  String modelName, String errorMsg) {
      if (errorMsg != null) {
        return;
      }
      if (!jmolApp.haveDisplay)
        return;
      
      // this code presumes only ptLoad = -1 (error), 0 (zap), or 3 (completed)
      
      //      jmolpopup.updateComputedMenus();
      String title = "Jmol";
      if (fullPathName == null) {
        if (fileName != null && appConsole != null)
          appConsole.undoClear();
        // a 'clear/zap' operation
      } else {
        if (modelName != null && fileName != null)
          title = fileName + " - " + modelName;
        else if (fileName != null)
          title = fileName;
        else if (modelName != null)
          title = modelName;
        recentFiles.notifyFileOpen(fullPathName);
      }
      frame.setTitle(title);
      if (atomSetChooser == null) {
        atomSetChooser = new AtomSetChooser(viewer, frame);
        pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);
      }
      pcs.firePropertyChange(chemFileProperty, null, null);
    }

    private void notifyFrameChanged(int frameNo, int file, int model) {
      // Note: twos-complement. To get actual frame number, use 
      // Math.max(frameNo, -2 - frameNo)
      // -1 means all frames are now displayed
      boolean isAnimationRunning = (frameNo <= -2);

      /*
       * animationDirection is set solely by the "animation direction +1|-1" script command
       * currentDirection is set by operations such as "anim playrev" and coming to the end of 
       * a sequence in "anim mode palindrome"
       * 
       * It is the PRODUCT of these two numbers that determines what direction the animation is
       * going.
       * 
       */
      //int animationDirection = (firstNo < 0 ? -1 : 1);
      //int currentDirection = (lastNo < 0 ? -1 : 1);
      //System.out.println("notifyFrameChange " + frameNo + " " + fileNo + " " + modelNo + " " + firstNo + " " + lastNo + " " + animationDirection + " " + currentDirection);
      if (display != null)
        display.status.setStatus(1, file + "." + model);
      if (jmolpopup == null || isAnimationRunning)
        return;
      jmolpopup.updateComputedMenus();
    }

    private void sendConsoleEcho(String strEcho) {
      if (appConsole != null)
        appConsole.sendConsoleEcho(strEcho);
    }

    private void sendConsoleMessage(String strStatus) {
      if (appConsole != null)
        appConsole.sendConsoleMessage(strStatus);
    }

    public void handlePopupMenu(int x, int y) {
      if (!language.equals(GT.getLanguage())) {
        jmolpopup = JmolPopup.newJmolPopup(viewer, true, menuStructure, true);
        language = GT.getLanguage();
      }
      jmolpopup.show(x, y);
    }

    public void showUrl(String url) {
      try {
        Class c = Class.forName("java.awt.Desktop");
        Method getDesktop = c.getMethod("getDesktop", new Class[] {});
        Object deskTop = getDesktop.invoke(null, new Class[] {});
        Method browse = c.getMethod("browse", new Class[] { URI.class });
        Object arguments[] = { new URI(url) };
        browse.invoke(deskTop, arguments);
      } catch (Exception e) {
        System.out.println(e.getMessage());
        if (appConsole != null) {
          appConsole
              .sendConsoleMessage("Java 6 Desktop.browse() capability unavailable. Could not open "
                  + url);
        } else {
          Logger
              .error("Java 6 Desktop.browse() capability unavailable. Could not open "
                  + url);
        }
      }
    }

    public void showConsole(boolean showConsole) {
      if (appConsole == null)
        return;
      if (showConsole)
        appConsole.setVisible(true);
      else
        appConsole.setVisible(false);
    }

    /**
     * this is just a test method for isosurface FUNCTIONXY
     * @param functionName 
     * @param nX 
     * @param nY 
     * @return f(x,y) as a 2D array
     * 
     */
    public float[][] functionXY(String functionName, int nX, int nY) {
      nX = Math.abs(nX);
      nY = Math.abs(nY);
      float[][] f = new float[nX][nY];
      //boolean isSecond = (functionName.indexOf("2") >= 0);
      for (int i = nX; --i >= 0;)
        for (int j = nY; --j >= 0;) {
          float x = i / 15f - 1;
          float y = j / 15f - 1;
          f[i][j] = (float) Math.sqrt(x*x + y);
          if (Float.isNaN(f[i][j]))
              f[i][j] = -(float) Math.sqrt(-x*x - y);
         // f[i][j] = (isSecond ? (float) ((i + j - nX) / (2f)) : (float) Math
           //   .sqrt(Math.abs(i * i + j * j)) / 2f);
          //if (i < 10 && j < 10)
          System.out.println(" functionXY " + i + " " + j + " " + f[i][j]);
        }

      return f; // for user-defined isosurface functions (testing only -- bob hanson)
    }

    public float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
      nX = Math.abs(nX);
      nY = Math.abs(nY);
      nZ = Math.abs(nZ);
      float[][][] f = new float[nX][nY][nZ];
      for (int i = nX; --i >= 0;)
        for (int j = nY; --j >= 0;)
          for (int k = nZ; --k >= 0;) {
          float x = i / ((nX-1)/2f) - 1;
          float y = j / ((nY-1)/2f) - 1;
          float z = k / ((nZ-1)/2f) - 1;
          f[i][j][k] = (float) x*x + y - z*z;
          //if (i == 22 || i == 23)
            //System.out.println(" functionXYZ " + i + " " + j + " " + k + " " + f[i][j][k]);
        }
      return f; // for user-defined isosurface functions (testing only -- bob hanson)
    }

    public Hashtable getRegistryInfo() {
      return null;
    }

    public String dialogAsk(String type, String fileName) {
      if (type.equals("load"))
        return getOpenFileNameFromDialog(fileName);
      if (type.equals("save")) {
        return (new Dialog()).getSaveFileNameFromDialog(viewer, fileName,
            null);
      }
      if (type.equals("saveImage")) {
        Dialog sd = new Dialog();
        fileName = sd.getImageFileNameFromDialog(viewer,
            fileName, imageType, imageChoices, imageExtensions, qualityJPG,
            qualityPNG);
        imageType = sd.getType();
        qualityJPG = sd.getQuality("JPG");
        qualityPNG = sd.getQuality("PNG");
        return fileName;
      }
      return null;
    }
  }

  class ExecuteScriptAction extends AbstractAction {
    public ExecuteScriptAction() {
      super("executeScriptAction");
    }

    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script.indexOf("#showMeasurementTable") >= 0)
        measurementTable.activate();
      //      viewer.script("set picking measure distance;set pickingstyle measure");
      viewer.evalStringQuiet(script);
    }
  }

}
