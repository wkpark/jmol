/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.openscience.jmol.app;

import org.jmol.api.*;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.popup.JmolPopup;
import org.jmol.i18n.GT;
import org.jmol.util.*;

import Acme.JPM.Encoders.PpmEncoder;
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
import java.util.*;
import javax.swing.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

public class Jmol extends JPanel {

  /**
   * The data model.
   */

  public JmolViewer viewer;

  DisplayPanel display;
  StatusBar status;
  private PreferencesDialog preferencesDialog;
  MeasurementTable measurementTable;
  RecentFilesDialog recentFiles;
  //private JMenu recentFilesMenu;
  public ScriptWindow scriptWindow;
  public AtomSetChooser atomSetChooser;
  private ExecuteScriptAction executeScriptAction;
  protected JFrame frame;
   protected static File currentDir;
  FileChooser openChooser;
  private JFileChooser saveChooser;
  private FileTyper fileTyper;
  JFileChooser exportChooser;
  JmolPopup jmolpopup;
  // private CDKPluginManager pluginManager;

  private GuiMap guimap = new GuiMap();
  
  private static int numWindows = 0;
  private static Dimension screenSize = null;
  int startupWidth, startupHeight;

  PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  // Window names for the history file
  private final static String JMOL_WINDOW_NAME = "Jmol";
  private final static String CONSOLE_WINDOW_NAME = "Console";
  private final static String SCRIPT_WINDOW_NAME = "Script";
  private final static String FILE_OPEN_WINDOW_NAME = "FileOpen";

  static Point border;
  /**
   * The current file.
   */
  File currentFile;

  /**
   * Button group for toggle buttons in the toolbar.
   */
  static AbstractButton buttonRotate = null;
  static ButtonGroup toolbarButtonGroup = new ButtonGroup();

  static File UserPropsFile;
  static HistoryFile historyFile;

  Splash splash;

  public static HistoryFile getHistoryFile() {
    return historyFile;
  }

  static JFrame consoleframe;

  static {
    if (System.getProperty("javawebstart.version") != null) {

      // If the property is found, Jmol is running with Java Web Start. To fix
      // bug 4621090, the security manager is set to null.
      System.setSecurityManager(null);
    }
    if (System.getProperty("user.home") == null) {
      System.err.println(
          GT._("Error starting Jmol: the property 'user.home' is not defined."));
      System.exit(1);
    }
    File ujmoldir = new File(new File(System.getProperty("user.home")),
                      ".jmol");
    ujmoldir.mkdirs();
    UserPropsFile = new File(ujmoldir, "properties");
    historyFile = new HistoryFile(new File(ujmoldir, "history"),
        "Jmol's persistent values");
  }

  static Boolean isSilent = Boolean.FALSE;
  static Boolean haveConsole = Boolean.TRUE;
  static Boolean haveDisplay = Boolean.TRUE;
  
  
  Jmol(Splash splash, JFrame frame, Jmol parent, int startupWidth,
      int startupHeight, String commandOptions) {
    super(true);
    this.frame = frame;
    this.startupWidth = startupWidth;
    this.startupHeight = startupHeight;
    numWindows++;

    frame.setTitle("Jmol");
    frame.setBackground(Color.lightGray);
    frame.getContentPane().setLayout(new BorderLayout());

    this.splash = splash;

    setBorder(BorderFactory.createEtchedBorder());
    setLayout(new BorderLayout());

    status = (StatusBar) createStatusBar();
    say(GT._("Initializing 3D display..."));
    //
    display = new DisplayPanel(status, guimap, haveDisplay.booleanValue(),
        startupWidth, startupHeight);
    JmolAdapter modelAdapter;
    String adapter = System.getProperty("model");
    if (adapter == null || adapter.length() == 0)
      adapter = "smarter";
    if (adapter.equals("smarter")) {
      report("using Smarter Model Adapter");
      modelAdapter = new SmarterJmolAdapter(null);
    } else if (adapter.equals("cdk")) {
      report("the CDK Model Adapter is currently no longer supported. Check out http://bioclipse.net/. -- using Smarter");
      // modelAdapter = new CdkJmolAdapter(null);
      modelAdapter = new SmarterJmolAdapter(null);
    } else {
      report("unrecognized model adapter:" + adapter + " -- using Smarter");
      modelAdapter = new SmarterJmolAdapter(null);
    }

    viewer = JmolViewer.allocateViewer(display, modelAdapter);
    viewer.setAppletContext("", null, null, commandOptions);

    display.setViewer(viewer);

    say(GT._("Initializing Preferences..."));
    preferencesDialog = new PreferencesDialog(frame, guimap, viewer);
    say(GT._("Initializing Recent Files..."));
    recentFiles = new RecentFilesDialog(frame);
    if (haveDisplay.booleanValue()) {
      say(GT._("Initializing Script Window..."));
      scriptWindow = new ScriptWindow(viewer, frame);
      say(GT._("Initializing AtomSetChooser Window..."));
      atomSetChooser = new AtomSetChooser(viewer, frame);
    }

    MyStatusListener myStatusListener;
    myStatusListener = new MyStatusListener();
    viewer.setJmolStatusListener(myStatusListener);

    say(GT._("Initializing Measurements..."));
    measurementTable = new MeasurementTable(viewer, frame);

    // Setup Plugin system
    // say(GT._("Loading plugins..."));
    // pluginManager = new CDKPluginManager(
    //     System.getProperty("user.home") + System.getProperty("file.separator")
    //     + ".jmol", new JmolEditBus(viewer)
    // );
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DirBrowserPlugin");
    // pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DadmlBrowserPlugin");
    // pluginManager.loadPlugins(
    //     System.getProperty("user.home") + System.getProperty("file.separator")
    //     + ".jmol/plugins"
    // );
    // feature to allow for globally installed plugins
    // if (System.getProperty("plugin.dir") != null) {
    //     pluginManager.loadPlugins(System.getProperty("plugin.dir"));
    // }

    // install the command table
    say(GT._("Building Command Hooks..."));
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
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

    if (haveDisplay.booleanValue()) {

      say(GT._("Setting up File Choosers..."));
      openChooser = new FileChooser();
      openChooser.setCurrentDirectory(currentDir);
      String previewProperty = System.getProperty("openFilePreview", "true");
      if (Boolean.valueOf(previewProperty).booleanValue()) {
        new FilePreview(openChooser, modelAdapter);
      }
      saveChooser = new JFileChooser();
      fileTyper = new FileTyper();
      saveChooser.addPropertyChangeListener(fileTyper);
      saveChooser.setAccessory(fileTyper);
      saveChooser.setCurrentDirectory(currentDir);
      exportChooser = new JFileChooser();
      exportChooser.setCurrentDirectory(currentDir);

      pcs.addPropertyChangeListener(chemFileProperty, exportAction);
      pcs.addPropertyChangeListener(chemFileProperty, povrayAction);
      pcs.addPropertyChangeListener(chemFileProperty, pdfAction);
      pcs.addPropertyChangeListener(chemFileProperty, printAction);
      pcs.addPropertyChangeListener(chemFileProperty,
          viewMeasurementTableAction);
      pcs.addPropertyChangeListener(chemFileProperty, atomSetChooser);

      jmolpopup = JmolPopup.newJmolPopup(viewer);

    }

    // prevent new Jmol from covering old Jmol
    if (parent != null) {
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
    frame.addWindowListener(new Jmol.AppCloser());
    frame.pack();
    frame.setSize(startupWidth, startupHeight);
    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);

    // Repositionning windows
    if (scriptWindow != null)
      historyFile.repositionWindow(SCRIPT_WINDOW_NAME, scriptWindow);

    say(GT._("Setting up Drag-and-Drop..."));
    FileDropper dropper = new FileDropper();
    final JFrame f = frame;
    dropper.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        //System.out.println("Drop triggered...");
        f.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (evt.getPropertyName().equals(FileDropper.FD_PROPERTY_FILENAME)) {
          final String filename = evt.getNewValue().toString();
          viewer.openFile(filename);
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

  static void report(String str) {
    if (isSilent.booleanValue())
      return;
    System.out.println(str);
  }
  
  public static Jmol getJmol(JFrame frame, int startupWidth, int startupHeight,
                             String commandOptions) {

    Splash splash = null;
    if (haveDisplay.booleanValue()) {
      ImageIcon splash_image = JmolResourceHandler.getIconX("splash");
      report("splash_image=" + splash_image);
      splash = new Splash(frame, splash_image);
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

    // cache the current directory to speed up Jmol window creation
    currentDir = getUserDirectory();

    Jmol window = new Jmol(splash, frame, null, startupWidth, startupHeight,
        commandOptions);
    if (haveDisplay.booleanValue())
      frame.show();
    return window;
  }

  public static void main(String[] args) {

    Jmol jmol = null;

    String modelFilename = null;
    String scriptFilename = null;

    Options options = new Options();
    options.addOption("h", "help", false, GT._("give this help page"));
    options.addOption("n", "nodisplay", false, GT
        ._("no display (and also exit when done)"));
    options.addOption("c", "check", false, GT._("check script syntax only"));
    options.addOption("i", "silent", false, GT._("silent startup operation"));
    options.addOption("o", "noconsole", false, GT
        ._("no console -- all output to sysout"));
    options.addOption("x", "exit", false, GT
        ._("exit after script (implicit with -n)"));

    OptionBuilder.withLongOpt("script");
    OptionBuilder.withDescription("script file to execute");
    OptionBuilder.withValueSeparator('=');
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("s"));

    OptionBuilder.withArgName(GT._("property=value"));
    OptionBuilder.hasArg();
    OptionBuilder.withValueSeparator();
    OptionBuilder.withDescription(GT._("supported options are given below"));
    options.addOption(OptionBuilder.create("D"));

    OptionBuilder.withLongOpt("geometry");
    // OptionBuilder.withDescription(GT._("overall window width x height, e.g. {0}", "-g512x616"));
    OptionBuilder.withDescription(GT._("window width x height, e.g. {0}",
        "-g500x500"));
    OptionBuilder.withValueSeparator();
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("g"));

    OptionBuilder.withLongOpt("write");
    OptionBuilder.withDescription(GT._("{0} or {1}:filename", new Object[] {
        "CLIP", "JPG|JPG64|PNG|PPM" }));
    OptionBuilder.withValueSeparator();
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("w"));

    int startupWidth = 0, startupHeight = 0;

    CommandLine line = null;
    try {
      CommandLineParser parser = new PosixParser();
      line = parser.parse(options, args);
    } catch (ParseException exception) {
      System.err.println("Unexpected exception: " + exception.toString());
    }

    if (line.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Jmol", options);

      // now report on the -D options
      System.out.println();
      System.out.println(GT._("For example:"));
      System.out.println();
      System.out
          .println("Jmol -ions myscript.spt -w JPEG:myfile.jpg > output.txt");
      System.out.println();
      System.out.println(GT
          ._("The -D options are as follows (defaults in parathesis):"));
      System.out.println();
      System.out.println("  cdk.debugging=[true|false] (false)");
      System.out.println("  cdk.debug.stdout=[true|false] (false)");
      System.out.println("  display.speed=[fps|ms] (ms)");
      System.out.println("  JmolConsole=[true|false] (true)");
      System.out.println("  jmol.logger.debug=[true|false] (false)");
      System.out.println("  jmol.logger.error=[true|false] (true)");
      System.out.println("  jmol.logger.fatal=[true|false] (true)");
      System.out.println("  jmol.logger.info=[true|false] (true)");
      System.out.println("  jmol.logger.logLevel=[true|false] (false)");
      System.out.println("  jmol.logger.warn=[true|false] (true)");
      System.out.println("  plugin.dir (unset)");
      System.out.println("  user.language=[DE|EN|ES|FR|NL|PL|TR] (EN)");

      System.exit(0);
    }

    args = line.getArgs();
    if (args.length > 0) {
      modelFilename = args[0];
    }

    // Process more command line arguments
    // these are also passed to viewer

    String commandOptions = "";

    //silent startup
    if (line.hasOption("i")) {
      commandOptions += "-i";
      isSilent = Boolean.TRUE;
    }

    //output to sysout
    if (line.hasOption("o")) {
      commandOptions += "-o";
      haveConsole = Boolean.FALSE;
    }

    //no display (and exit)
    if (line.hasOption("n")) {
      // this ensures that noDisplay also exits
      commandOptions += "-n-x";
      haveDisplay = Boolean.FALSE;
    }

    //check script only
    if (line.hasOption("c")) {
      commandOptions += "-c";
    }

    //run script
    if (line.hasOption("s")) {
      commandOptions += "-s";
      scriptFilename = line.getOptionValue("s");
    }

    //exit when script completes (or file is read)
    if (line.hasOption("x")) {
      commandOptions += "-x";
    }

    //write image to clipboard or image file  
    if (line.hasOption("w")) {
      String type_name = line.getOptionValue("w");
      commandOptions += "-w\1" + type_name + "\1";
    }

    try {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.1.2") < 0) {
        System.out.println("!!!WARNING: Swing components require a "
            + "1.1.2 or higher version VM!!!");
      }

      Dimension size = historyFile.getWindowSize(JMOL_WINDOW_NAME);
      if (size != null && haveDisplay.booleanValue()) {
        startupWidth = size.width;
        startupHeight = size.height;
      }

      //OUTER window dimensions
      /*
       if (line.hasOption("g") && haveDisplay.booleanValue()) {
       String geometry = line.getOptionValue("g");
       int indexX = geometry.indexOf('x');
       if (indexX > 0) {
       startupWidth = parseInt(geometry.substring(0, indexX));
       startupHeight = parseInt(geometry.substring(indexX + 1));
       }
       }
       */

      Point b = historyFile.getWindowBorder(JMOL_WINDOW_NAME);
      //first one is just approximate, but this is set in doClose()
      //so it will reset properly -- still, not perfect
      //since it is always one step behind.
      if (b == null)
        border = new Point(12, 116);
      else
        border = new Point(b.x, b.y);
      //note -- the first time this is run after changes it will not work
      //because there is a bootstrap problem.
      
      //INNER frame dimensions
      if (line.hasOption("g") && haveDisplay.booleanValue()) {
        String geometry = line.getOptionValue("g");
        int indexX = geometry.indexOf('x');
        if (indexX > 0) {
          startupWidth = parseInt(geometry.substring(0, indexX)) + border.x;
          startupHeight = parseInt(geometry.substring(indexX + 1)) + border.y;
        }
      }
      if (startupWidth <= 0 || startupHeight <= 0) {
        startupWidth = 500 + border.x;
        startupHeight = 500 + border.y;
      }
      JFrame jmolFrame = new JFrame();
      Point jmolPosition = historyFile.getWindowPosition(JMOL_WINDOW_NAME);
      if (jmolPosition != null) {
        jmolFrame.setLocation(jmolPosition);
      }

      //now pass these to viewer
      jmol = getJmol(jmolFrame, startupWidth, startupHeight, commandOptions);

      // Open a file if one is given as an argument -- note, this CAN be a script file
      if (modelFilename != null) {
        jmol.viewer.openFile(modelFilename);
        jmol.viewer.getOpenFileError();
      }

      // OK, by now it is time to execute the script
      if (scriptFilename != null) {
        report("Executing script: " + scriptFilename);
        if (haveDisplay.booleanValue())
          jmol.splash.showStatus(GT._("Executing script..."));
        jmol.viewer.openFile(scriptFilename); //now the same interface
      }
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    if (haveConsole.booleanValue()) {
      Point location = jmol.frame.getLocation();
      Dimension size = jmol.frame.getSize();
      // Adding console frame to grab System.out & System.err
      consoleframe = new JFrame(GT._("Jmol Java Console"));
      consoleframe.setIconImage(jmol.frame.getIconImage());
      try {
        final ConsoleTextArea consoleTextArea = new ConsoleTextArea();
        consoleTextArea.setFont(java.awt.Font.decode("monospaced"));
        consoleframe.getContentPane().add(new JScrollPane(consoleTextArea),
            java.awt.BorderLayout.CENTER);
        if (Boolean.getBoolean("clearConsoleButton")) {
          JButton buttonClear = new JButton(GT._("Clear"));
          buttonClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              consoleTextArea.setText("");
            }
          });
          consoleframe.getContentPane().add(buttonClear,
              java.awt.BorderLayout.SOUTH);
        }
      } catch (IOException e) {
        JTextArea errorTextArea = new JTextArea();
        errorTextArea.setFont(java.awt.Font.decode("monospaced"));
        consoleframe.getContentPane().add(new JScrollPane(errorTextArea),
            java.awt.BorderLayout.CENTER);
        errorTextArea.append(GT._("Could not create ConsoleTextArea: ") + e);
      }

      Dimension consoleSize = historyFile.getWindowSize(CONSOLE_WINDOW_NAME);
      Point consolePosition = historyFile
          .getWindowPosition(CONSOLE_WINDOW_NAME);
      if ((consoleSize != null) && (consolePosition != null)) {
        consoleframe.setBounds(consolePosition.x, consolePosition.y,
            consoleSize.width, consoleSize.height);
      } else {
        consoleframe.setBounds(location.x, location.y + size.height,
            size.width, 200);
      }

      Boolean consoleVisible = historyFile
          .getWindowVisibility(CONSOLE_WINDOW_NAME);
      if ((consoleVisible != null) && (consoleVisible.equals(Boolean.TRUE))) {
        consoleframe.show();
      }
    }
  }

  static int parseInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException nfe) {
      return Integer.MIN_VALUE;
    }
  }

  private void say(String message) {
    if (haveDisplay.booleanValue())
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
          Jmol.this.doClose();
      }
  }

  void doClose() {
      // Save window positions and status in the history
      if (historyFile != null) {
        historyFile.addWindowInfo(JMOL_WINDOW_NAME, this.frame, border);
        //historyFile.addWindowInfo(CONSOLE_WINDOW_NAME, consoleframe);
        if (scriptWindow != null)
          historyFile.addWindowInfo(SCRIPT_WINDOW_NAME, scriptWindow, null);
      }
      
      // Close Jmol
      numWindows--;
      if (numWindows <= 1) {
          report(GT._("Closing Jmol..."));
          // pluginManager.closePlugins();
          System.exit(0);
      } else {
          this.frame.dispose();
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
    
    ImageIcon f =
      JmolResourceHandler.getIconX(cmd + "Image");
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
    String[] tool1Keys =
      tokenize(JmolResourceHandler.getStringX("toolbar"));
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

    ImageIcon ii =
      JmolResourceHandler.getIconX(key + "Image");
    AbstractButton b = new JButton(ii);
    String isToggleString =
      JmolResourceHandler.getStringX(key + "Toggle");
    if (isToggleString != null) {
      boolean isToggle = Boolean.valueOf(isToggleString).booleanValue();
      if (isToggle) {
        b = new JToggleButton(ii);
        if (key.equals("rotate"))
          buttonRotate = b;
        toolbarButtonGroup.add(b);
        String isSelectedString =
          JmolResourceHandler.getStringX(key + "ToggleSelected");
        if (isSelectedString != null) {
          boolean isSelected =
            Boolean.valueOf(isSelectedString).booleanValue();
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

  public static void setRotateButton() {
    if (buttonRotate != null)
      buttonRotate.setSelected(true);
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
      JMenu macroMenu = new JMenu(GT._("Macros"));
      File macroDir = new File(
          System.getProperty("user.home") + System.getProperty("file.separator")
          + ".jmol" + System.getProperty("file.separator") + "macros"
      );
      report("User macros dir: " + macroDir);
      report("       exists: " + macroDir.exists());
      report("  isDirectory: " + macroDir.isDirectory());
      if (macroDir.exists() && macroDir.isDirectory()) {
          File[] macros = macroDir.listFiles();
          for (int i=0; i<macros.length; i++) {
              // loop over these files and load them
              String macroName = macros[i].getName();
              if (macroName.endsWith(".macro")) {
                  System.out.println("Possible macro found: " + macroName);
                  try {
                      FileInputStream macro = new FileInputStream(macros[i]);
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
                  }
              }
          }
      }
      menuBar.add(macroMenu);
  }
  
  protected void addNormalMenuBar(JMenuBar menuBar) {
    String[] menuKeys =
      tokenize(JmolResourceHandler.getStringX("menubar"));
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

  // these correlate with items in GuiMap.java
  
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
  private static final String pdfActionProperty = "pdf";
  private static final String scriptAction = "script";
  private static final String atomsetchooserAction = "atomsetchooser";
  private static final String copyImageActionProperty = "copyImage";
  private static final String copyScriptActionProperty = "copyScript";
  private static final String pasteClipboardActionProperty = "pasteClipboard";


  // --- action implementations -----------------------------------

  private ExportAction exportAction = new ExportAction();
  private PovrayAction povrayAction = new PovrayAction();
  private PdfAction pdfAction = new PdfAction();
  private PrintAction printAction = new PrintAction();
  private CopyImageAction copyImageAction = new CopyImageAction();
  private CopyScriptAction copyScriptAction = new CopyScriptAction();
  private PasteClipboardAction pasteClipboardAction = new PasteClipboardAction();
  private ViewMeasurementTableAction viewMeasurementTableAction
    = new ViewMeasurementTableAction();


  /**
   * Actions defined by the Jmol class
   */
  private Action[] defaultActions = {
    new NewAction(), new NewwinAction(), new OpenAction(),
    new OpenUrlAction(), printAction, exportAction,
    new CloseAction(), new ExitAction(), copyImageAction, copyScriptAction,
    pasteClipboardAction,
    new AboutAction(), new WhatsNewAction(),
    new UguideAction(), new ConsoleAction(),
    new RecentFilesAction(), povrayAction, pdfAction,
    new ScriptWindowAction(), new AtomSetChooserAction(),
    viewMeasurementTableAction
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
      if (consoleframe != null)
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
      new Jmol(null, newFrame, Jmol.this, startupWidth, startupHeight, "");
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

  class PasteClipboardAction extends AbstractAction {
  
    public PasteClipboardAction() {
      super(pasteClipboardActionProperty);
    }
  
    public void actionPerformed(ActionEvent e) {
      String str = ImageSelection.getClipboardText();
      if (str != null && str.length() > 0)
        viewer.loadInline(str);
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
      ImageCreator c = new ImageCreator(viewer, status);
      c.clipImage(null);
    }
  }

  class CopyScriptAction extends AbstractAction {

    public CopyScriptAction() {
      super(copyScriptActionProperty);
    }

    public void actionPerformed(ActionEvent e) {
      ImageCreator c = new ImageCreator(viewer, status);
      c.clipImage((String) viewer.getProperty("string","stateInfo", null));
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

    PrinterJob job = PrinterJob.getPrinterJob();
    job.setPrintable(display);
    if (job.printDialog()) {
      try {
        job.print();
      } catch (PrinterException e) {
        System.out.println("" + e);
      }
    }
  }

  class OpenAction extends NewAction {

    OpenAction() {
      super(openAction);
    }

    public void actionPerformed(ActionEvent e) {

      openChooser.setDialogSize(
        historyFile.getWindowSize(FILE_OPEN_WINDOW_NAME));
      openChooser.setDialogLocation(
        historyFile.getWindowPosition(FILE_OPEN_WINDOW_NAME));
      int retval = openChooser.showOpenDialog(Jmol.this);
      if (retval == 0) {
        File file = openChooser.getSelectedFile();
        viewer.openFile(file.getAbsolutePath());
        return;
      }
      historyFile.addWindowInfo(FILE_OPEN_WINDOW_NAME, 
          openChooser.getDialog(), null);
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
        viewer.openFile(url);
        viewer.getOpenFileError();
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
        Jmol.this.doClose();
    }
  }

  class ExportAction extends MoleculeDependentAction {

    ExportAction() {
      super(exportActionProperty);
    }

    public void actionPerformed(ActionEvent e) {

      ImageTyper it = new ImageTyper(exportChooser);
      exportChooser.setAccessory(it);
      
      String fileName = viewer.getModelSetFileName();
      String pathName = viewer.getModelSetPathName();
      File file = null;
      if ((fileName != null) && (pathName != null)) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart != -1) {
          fileName = fileName.substring(0, extensionStart) + "." + it.getExtension();
        }
        file = new File(pathName, fileName);
        exportChooser.setSelectedFile(file);
      }

      int retval = exportChooser.showSaveDialog(Jmol.this);
      if (retval == 0) {
        file = exportChooser.getSelectedFile();
        if (file != null) {
          ImageCreator c = new ImageCreator(viewer, status);
          c.createImage(file.getAbsolutePath(), it.getType(), it.getQuality());
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
        viewer.getOpenFileError();
      }
    }
  }

  class ScriptWindowAction extends AbstractAction {

    public ScriptWindowAction() {
      super(scriptAction);
    }

    public void actionPerformed(ActionEvent e) {
      if (scriptWindow != null)
      scriptWindow.show();
    }
  }
  
  class AtomSetChooserAction extends AbstractAction {
    public AtomSetChooserAction() {
      super(atomsetchooserAction);
    }
    
    public void actionPerformed(ActionEvent e) {
      atomSetChooser.show();
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
      if (retval == JFileChooser.APPROVE_OPTION) {
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

  class MyStatusListener implements JmolStatusListener {
    
    public String eval(String strEval) {
      return "# 'eval' is implemented only for the applet.";
    }
    
    public void createImage(String file, String type, int quality) {
      ImageCreator c = new ImageCreator(viewer, status);
      c.createImage(file, type, quality);
    }
    
    public void setCallbackFunction(String callbackType, String callbackFunction) {
      // applet only?
    }

    public void notifyFileLoaded(String fullPathName, String fileName,
                                 String modelName, Object clientFile,
                                 String errorMsg) {
      if (errorMsg != null) {
        //        JOptionPane.showMessageDialog(null,
        //          fullPathName + "\n\n" + errorMsg + "\n\n" ,
        //          GT._("File not loaded"),
        //          JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (!haveDisplay.booleanValue())
        return;
      //      jmolpopup.updateComputedMenus();
      String title = "Jmol";
      if (fullPathName == null) {
        // a 'clear/zap' operation
        frame.setTitle(title);
        return;
      }
      if (modelName != null && fileName != null)
        title = fileName + " - " + modelName;
      else if (fileName != null)
        title = fileName;
      else if (modelName != null)
        title = modelName;
      frame.setTitle(title);
      recentFiles.notifyFileOpen(fullPathName);
      if (haveDisplay.booleanValue())
        pcs.firePropertyChange(chemFileProperty, null, clientFile);
    }

    public void notifyFrameChanged(int frameNo) {
      // Note: twos-complement. To get actual frame number, use 
      // Math.max(frameNo, -2 - frameNo)
      // -1 means all frames are now displayed
      
      boolean isAnimationRunning = (frameNo <= -2);
      if (jmolpopup == null || isAnimationRunning)
        return;
      jmolpopup.updateComputedMenus();
    }

    public void notifyScriptStart(String statusMessage, String additionalInfo) {
      //System.out.println("notifyScriptStart:" + statusMessage + (additionalInfo == "" ? "" : additionalInfo));
    }
    
    public void sendConsoleEcho(String strEcho) {
      if (scriptWindow != null)
        scriptWindow.sendConsoleEcho(strEcho);
    }

    public void sendConsoleMessage(String strStatus) {
      if (scriptWindow != null)
        scriptWindow.sendConsoleMessage(strStatus);
    }

    public void notifyScriptTermination(String strStatus, int msWalltime) {
      if (scriptWindow != null)
        scriptWindow.notifyScriptTermination(strStatus, msWalltime);
    }

    public void handlePopupMenu(int x, int y) {
      jmolpopup.show(x, y);
    }

    public void notifyNewPickingModeMeasurement(int iatom, String strMeasure) {
      notifyAtomPicked(iatom, strMeasure);
    }

    public void notifyNewDefaultModeMeasurement(int count, String strInfo) {
      measurementTable.updateTables();
    }

    public void notifyAtomPicked(int atomIndex, String strInfo) {
      if (scriptWindow != null) {
        scriptWindow.sendConsoleMessage(strInfo);
        scriptWindow.sendConsoleMessage("\n");
      }
    }

    public void notifyAtomHovered(int atomIndex, String strInfo) {
      
    }

    public void sendSyncScript(String script, String appletName) {  
    }
    
    public void showUrl(String url) {
    }

    public void showConsole(boolean showConsole) {
      if (scriptWindow == null)
        return;
      if (showConsole)
        scriptWindow.show();
      else
        scriptWindow.hide();
    }
    
    public float functionXY(String functionName, int x, int y) {
      return 0;  // for user-defined isosurface functions (testing only -- bob hanson)
    }

  }

  class ExecuteScriptAction extends AbstractAction {
    public ExecuteScriptAction() {
      super("executeScriptAction");
    }

    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet(e.getActionCommand());
    }
  }
  
}

class ImageCreator {
  
  JmolViewer viewer;
  StatusBar status;
  
  ImageCreator(JmolViewer viewer, StatusBar status) {
    this.viewer = viewer;
    this.status = status;
  }
 
  void clipImage(String script) {
    if (script == null) {
      Image eImage = viewer.getScreenImage();
      ImageSelection.setClipboard(eImage);
      viewer.releaseScreenImage();
      return;
    }
    ImageSelection.setClipboard(script);
  }
  
  void createImage(String fileName, String type, int quality) {
    String script = null;
    boolean isScript = type.equals("SPT") || type.equals("HIS");
    if (isScript)
      script = (type.equals("SPT") ? (String)viewer.getProperty("string", "stateInfo", null) 
          : viewer.getSetHistory(Integer.MAX_VALUE));
    if (fileName == null) {
      clipImage(script);
      return;
    }
    try {
      FileOutputStream os = new FileOutputStream(fileName);
      if (isScript) {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os), 8192);
        bw.write(script);
        bw.close();
        os = null;
      } else {
        Image eImage = viewer.getScreenImage();
        if (type.equalsIgnoreCase("JPEG") || type.equalsIgnoreCase("JPG")) {
          JpegEncoder jc = new JpegEncoder(eImage, quality, os);
          jc.Compress();
        } else if (type.equalsIgnoreCase("PPM")) {
          PpmEncoder pc = new PpmEncoder(eImage, os);
          pc.encode();
        } else if (type.equalsIgnoreCase("PNG")) {
          PngEncoder png = new PngEncoder(eImage);
          byte[] pngbytes = png.pngEncode();
          os.write(pngbytes);
        } else if (type.equalsIgnoreCase("JPG64")) {
          ByteArrayOutputStream osb = new ByteArrayOutputStream();
          JpegEncoder jc = new JpegEncoder(eImage, quality, osb);
          jc.Compress();
          osb.flush();
          osb.close();
          StringBuffer jpg = Base64.getBase64(osb.toByteArray());
          os.write(Base64.toBytes(jpg));
        }
        os.flush();
        os.close();
        viewer.releaseScreenImage();
      }
    } catch (IOException exc) {
      viewer.releaseScreenImage();
      if (exc != null) {
        if (status != null) {
          status.setStatus(1, GT._("IO Exception:"));
          status.setStatus(2, exc.toString());
        }
        System.out.println(exc.toString());
      }
    }
  }
}
