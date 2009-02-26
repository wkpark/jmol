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
import org.jmol.export.dialog.Dialog;
import org.jmol.export.dialog.HistoryFile;
import org.jmol.export.image.ImageCreator;
import org.jmol.popup.JmolPopup;
import org.jmol.i18n.GT;
import org.jmol.util.*;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;
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

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

import java.io.FileOutputStream;

public class Jmol extends JPanel {

  /**
   * The data model.
   */

  public JmolViewer viewer;

  DisplayPanel display;
  StatusBar status;
  protected GaussianDialog gaussianDialog;
  private PreferencesDialog preferencesDialog;
  MeasurementTable measurementTable;
  RecentFilesDialog recentFiles;
  //private JMenu recentFilesMenu;
  public ScriptWindow scriptWindow;
  public AtomSetChooser atomSetChooser;
  private ExecuteScriptAction executeScriptAction;
  protected JFrame frame;

  JmolPopup jmolpopup;
  String language;
  static String menuStructure;
  static String menuFile;

  // private CDKPluginManager pluginManager;

  private GuiMap guimap = new GuiMap();

  private static int numWindows = 0;
  private static Dimension screenSize = null;
  int startupWidth, startupHeight;

  PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  // Window names for the history file
  private final static String JMOL_WINDOW_NAME = "Jmol";
  private final static String CONSOLE_WINDOW_NAME = "Console";
  private final static String SCRIPT_WINDOW_NAME = "ScriptWindow";
  private final static String FILE_OPEN_WINDOW_NAME = "FileOpen";
  private final static String WEB_MAKER_WINDOW_NAME = "JmolWebPageMaker";

  static Point border;
  static Boolean haveBorder = Boolean.FALSE;

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
      System.err.println(GT
          ._("Error starting Jmol: the property 'user.home' is not defined."));
      System.exit(1);
    }
    File ujmoldir = new File(new File(System.getProperty("user.home")), ".jmol");
    ujmoldir.mkdirs();
    UserPropsFile = new File(ujmoldir, "properties");
    historyFile = new HistoryFile(new File(ujmoldir, "history"),
        "Jmol's persistent values");
  }

  static boolean isSilent = false;
  static boolean haveConsole = true;
  static boolean haveDisplay = true;
  JmolAdapter modelAdapter;
  String appletContext;

  Jmol(Splash splash, JFrame frame, Jmol parent, int startupWidth,
      int startupHeight, String commandOptions) {
    this(splash, frame, parent, startupWidth, startupHeight, commandOptions,
        null);
  }

  Jmol(Splash splash, JFrame frame, Jmol parent, int startupWidth,
      int startupHeight, String commandOptions, Point loc) {
    super(true);
    this.frame = frame;
    this.startupWidth = startupWidth;
    this.startupHeight = startupHeight;
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
    //
    display = new DisplayPanel(status, guimap, haveDisplay,
        startupWidth, startupHeight);
    String adapter = System.getProperty("model");
    if (adapter == null || adapter.length() == 0)
      adapter = "smarter";
    if (adapter.equals("smarter")) {
      report("using Smarter Model Adapter");
      modelAdapter = new SmarterJmolAdapter();
    } else if (adapter.equals("cdk")) {
      report("the CDK Model Adapter is currently no longer supported. Check out http://bioclipse.net/. -- using Smarter");
      // modelAdapter = new CdkJmolAdapter(null);
      modelAdapter = new SmarterJmolAdapter();
    } else {
      report("unrecognized model adapter:" + adapter + " -- using Smarter");
      modelAdapter = new SmarterJmolAdapter();
    }
    viewer = JmolViewer.allocateViewer(display, modelAdapter, 
        null, null, null, appletContext = commandOptions, 
        new MyStatusListener());
    if (display != null)
      display.setViewer(viewer);

    say(GT._("Initializing Preferences..."));
    preferencesDialog = new PreferencesDialog(frame, guimap, viewer);
    say(GT._("Initializing Recent Files..."));
    recentFiles = new RecentFilesDialog(frame);
    if (haveDisplay) {
      say(GT._("Initializing Script Window..."));
      scriptWindow = new ScriptWindow(viewer, frame);
    }

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

    if (haveDisplay) {

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

      //say(GT._("Setting up File Choosers..."));

      /*      pcs.addPropertyChangeListener(chemFileProperty, exportAction);
       pcs.addPropertyChangeListener(chemFileProperty, povrayAction);
       pcs.addPropertyChangeListener(chemFileProperty, writeAction);
       pcs.addPropertyChangeListener(chemFileProperty, toWebAction);
       pcs.addPropertyChangeListener(chemFileProperty, printAction);
       pcs.addPropertyChangeListener(chemFileProperty,
       viewMeasurementTableAction);
       */

      if (menuFile != null) {
        menuStructure = viewer.getFileAsString(menuFile);
      }
      jmolpopup = JmolPopup.newJmolPopup(viewer, true, menuStructure, true);

    }

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

    frame.addWindowListener(new Jmol.AppCloser());
    frame.pack();
    frame.setSize(startupWidth, startupHeight);
    ImageIcon jmolIcon = JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);

    // Repositionning windows
    if (scriptWindow != null)
      historyFile.repositionWindow(SCRIPT_WINDOW_NAME, scriptWindow, 200, 100);

    say(GT._("Setting up Drag-and-Drop..."));
    FileDropper dropper = new FileDropper();
    final JFrame f = frame;
    dropper.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        //System.out.println("Drop triggered...");
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

  static void report(String str) {
    if (isSilent)
      return;
    Logger.info(str);
  }

  public static Jmol getJmol(JFrame frame, int startupWidth, int startupHeight,
                             String commandOptions) {

    Splash splash = null;
    if (haveDisplay) {
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

    Jmol window = new Jmol(splash, frame, null, startupWidth, startupHeight,
        commandOptions);
    if (haveDisplay)
      frame.show();
    return window;
  }

  /* Convenient method to get values of UIManager strings
   private static void analyzeUIManagerString(String name, String value) {
   System.err.println(name);
   System.err.println(" en=[" + UIManager.getString(name) + "]");
   System.err.println(" de=[" + UIManager.getString(name, Locale.GERMAN) + "]");
   System.err.println(" es=[" + UIManager.getString(name, new Locale("es")) + "]");
   System.err.println(" fr=[" + UIManager.getString(name, Locale.FRENCH) + "]");
   UIManager.put(name, value);
   }*/

  public static void main(String[] args) {

    Dialog.setupUIManager();

    Jmol jmol = null;

    String modelFilename = null;
    String scriptFilename = null;
    String script2 = "";
    String script1 = "";

    Options options = new Options();
    options.addOption("b", "backgroundtransparent", false, GT
        ._("transparent background"));
    options.addOption("h", "help", false, GT._("give this help page"));
    options.addOption("n", "nodisplay", false, GT
        ._("no display (and also exit when done)"));
    options.addOption("c", "check", false, GT._("check script syntax only - no file loading"));
    options.addOption("C", "checkload", false, GT._("check script syntax only - with file loading"));
    options.addOption("d", "debug", false, GT._("debug"));
    options.addOption("i", "silent", false, GT._("silent startup operation"));
    options.addOption("l", "list", false, GT
        ._("list commands during script execution"));
    options.addOption("o", "noconsole", false, GT
        ._("no console -- all output to sysout"));
    options.addOption("t", "threaded", false, GT
        ._("independent commmand thread"));
    options.addOption("x", "exit", false, GT
        ._("exit after script (implicit with -n)"));

    OptionBuilder.withLongOpt("script");
    OptionBuilder.withDescription(GT._("script file to execute"));
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("s"));

    OptionBuilder.withLongOpt("jmolscript1");
    OptionBuilder.withDescription(GT._("Jmol script to execute BEFORE -s option"));
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("J"));

    OptionBuilder.withLongOpt("jmolscript2");
    OptionBuilder.withDescription(GT._("Jmol script to execute AFTER -s option"));
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("j"));

    OptionBuilder.withLongOpt("menu");
    OptionBuilder.withDescription("menu file to use");
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("m"));

    OptionBuilder.withArgName(GT._("property=value"));
    OptionBuilder.hasArg();
    OptionBuilder.withValueSeparator();
    OptionBuilder.withDescription(GT._("supported options are given below"));
    options.addOption(OptionBuilder.create("D"));

    OptionBuilder.withLongOpt("geometry");
    // OptionBuilder.withDescription(GT._("overall window width x height, e.g. {0}", "-g512x616"));
    OptionBuilder.withDescription(GT._("window width x height, e.g. {0}",
        "-g500x500"));
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("g"));

    OptionBuilder.withLongOpt("quality");
    // OptionBuilder.withDescription(GT._("overall window width x height, e.g. {0}", "-g512x616"));
    OptionBuilder
        .withDescription(GT
            ._("JPG image quality (1-100; default 75) or PNG image compression (0-9; default 2, maximum compression 9)"));
    OptionBuilder.hasArg();
    options.addOption(OptionBuilder.create("q"));

    OptionBuilder.withLongOpt("write");
    OptionBuilder.withDescription(GT._("{0} or {1}:filename", new Object[] {
        "CLIP", "GIF|JPG|JPG64|PNG|PPM" }));
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
          ._("The -D options are as follows (defaults in parenthesis):"));
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
      System.out.println("  user.language=[CA|CS|DE|EN|ES|FR|NL|PT|TR] (EN)");

      System.exit(0);
    }

    args = line.getArgs();
    if (args.length > 0) {
      modelFilename = args[0];
    }

    // Process more command line arguments
    // these are also passed to viewer

    String commandOptions = "";

    //debug mode
    if (line.hasOption("d")) {
      Logger.setLogLevel(Logger.LEVEL_DEBUG);
    }

    //silent startup
    if (line.hasOption("i")) {
      commandOptions += "-i";
      isSilent = true;
    }

    //output to sysout
    if (line.hasOption("o")) {
      commandOptions += "-o";
      haveConsole = false;
    }

    // transparent background
    if (line.hasOption("b")) {
      commandOptions += "-b";
    }

    // independent command thread
    if (line.hasOption("t")) {
      commandOptions += "-t";
    }

    //list commands during script operation
    if (line.hasOption("l")) {
      commandOptions += "-l";
    }

    //check script only -- don't open files
    if (line.hasOption("c")) {
      commandOptions += "-c";
    } else if (line.hasOption("C")) {
      commandOptions += "-C";
    }

    //menu file
    if (line.hasOption("m")) {
      menuFile = line.getOptionValue("m");
    }

    //run pre Jmol script
    if (line.hasOption("J")) {
      commandOptions += "-J";
      script1 = line.getOptionValue("J");
    }

    //run script from file
    if (line.hasOption("s")) {
      commandOptions += "-s";
      scriptFilename = line.getOptionValue("s");
    }
    
    //run post Jmol script
    if (line.hasOption("j")) {
      commandOptions += "-j";
      script2 = line.getOptionValue("j");
    }

    Dimension size;
    String vers = System.getProperty("java.version");
    if (vers.compareTo("1.1.2") < 0) {
      System.out.println("!!!WARNING: Swing components require a "
          + "1.1.2 or higher version VM!!!");
    }

    size = historyFile.getWindowSize(JMOL_WINDOW_NAME);
    if (size != null && haveDisplay) {
      startupWidth = size.width;
      startupHeight = size.height;
    }

    //OUTER window dimensions
    /*
     if (line.hasOption("g") && haveDisplay) {
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

    int width = -1;
    int height = -1;
    //INNER frame dimensions
    if (line.hasOption("g")) {
      String geometry = line.getOptionValue("g");
      int indexX = geometry.indexOf('x');
      if (indexX > 0) {
        width = Parser.parseInt(geometry.substring(0, indexX));
        height = Parser.parseInt(geometry.substring(indexX + 1));
        //System.out.println("setting geometry to " + geometry + " " + border + " " + startupWidth + startupHeight);
      }
      if (haveDisplay) {
        startupWidth = width + border.x;
        startupHeight = height + border.y;
      }
    }

    if (startupWidth <= 0 || startupHeight <= 0) {
      startupWidth = 500 + border.x;
      startupHeight = 500 + border.y;
    }

    //write image to clipboard or image file  
    if (line.hasOption("w")) {
      int quality = -1;
      if (line.hasOption("q"))
        quality = Parser.parseInt(line.getOptionValue("q"));
      String type_name = line.getOptionValue("w");
      if (type_name != null) {
        if (type_name.length() == 0)
          type_name = "JPG:jmol.jpg";
        if (type_name.indexOf(":") < 0)
          type_name += ":jmol.jpg";
        int i = type_name.indexOf(":");
        String type = type_name.substring(0, i).toUpperCase();
        type_name = " \"" + type_name.substring(i + 1) + "\"";
        if (type.indexOf(" ") < 0)
          type += " " + quality;
        script2 += ";write image " + width + " " + height + " " + type + type_name;
      }
    }


    //no display (and exit)
    if (line.hasOption("n")) {
      // this ensures that noDisplay also exits
      commandOptions += "-n-x";
      haveDisplay = false;
      script2 += ";exitJmol";
    }

    //exit when script completes (or file is read)
    if (line.hasOption("x")) {
      commandOptions += "-x";
      if (haveDisplay)
        script2 += ";exitJmol";
    }

    try {
      JFrame jmolFrame = new JFrame();
      Point jmolPosition = historyFile.getWindowPosition(JMOL_WINDOW_NAME);
      if (jmolPosition != null) {
        jmolFrame.setLocation(jmolPosition);
      }

      Logger.debug("command options: " + commandOptions);
      //now pass these to viewer
      jmol = getJmol(jmolFrame, startupWidth, startupHeight, commandOptions);

      // Open a file if one is given as an argument -- note, this CAN be a script file
      if (modelFilename != null) {
        jmol.viewer.openFileAsynchronously(modelFilename);
      }

      //jmol.viewer.scriptWait("load 1crn.pdb;cartoons on;set antialiasdisplay");
      //System.out.println("" + jmol.viewer.getImageAs("JPG", -1, 300, 300, "t75n.jpg", null));
      //System.out.println("" + jmol.viewer.getImageAs("JPG", -1, 3000, 3000, "t75n.jpg", null));
      //System.out.println("" + jmol.viewer.getImageAs("JPG", -1, 300, 300, null, null));
      //System.out.println("" + jmol.viewer.getImageAs("JPG", -1, 3000, 3000, null, null));
      
      // OK, by now it is time to execute the script

      // then command script
      if (script1 != null) {
        report("Executing script: " + script1);
        if (haveDisplay)
          jmol.splash.showStatus(GT._("Executing script 1..."));
        jmol.viewer.script(script1);
      }
      
      // next the file
      
      if (scriptFilename != null) {
        report("Executing script from file: " + scriptFilename);
        if (haveDisplay)
          jmol.splash.showStatus(GT._("Executing script file..."));
        jmol.viewer.evalFile(scriptFilename);
      }
      // then command script
      if (script2 != null) {
        report("Executing script: " + script2);
        if (haveDisplay)
          jmol.splash.showStatus(GT._("Executing script 2..."));
        jmol.viewer.script(script2);
      }
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    
    if (haveConsole) {
      Point location = jmol.frame.getLocation();
      size = jmol.frame.getSize();
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

  private void say(String message) {
    if (haveDisplay)
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
      if (display != null) {
        Jmol.border.x = this.getFrame().getWidth() - display.dimSize.width;
        Jmol.border.y = this.getFrame().getHeight() - display.dimSize.height;
        historyFile.addWindowInfo(JMOL_WINDOW_NAME, this.frame, border);
      }
      //System.out.println("doClose border: " + border);
      //historyFile.addWindowInfo(CONSOLE_WINDOW_NAME, consoleframe);
    }
    dispose(this.frame);
  }

  private void dispose(JFrame f) {
    if (historyFile != null && scriptWindow != null)
      historyFile.addWindowInfo(SCRIPT_WINDOW_NAME, scriptWindow, null);
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
        if (scriptWindow != null) {
          scriptWindow.dispose();
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
    Jmol j = new Jmol(null, newFrame, Jmol.this, startupWidth, startupHeight,
        "", (state == null ? null : f.getLocationOnScreen()));
    newFrame.show();
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
          buttonRotate = b;
        toolbarButtonGroup.add(b);
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
  private static final String writeActionProperty = "write";
  private static final String scriptAction = "script";
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
      new UguideAction(), new ConsoleAction(), new RecentFilesAction(),
      povrayAction, writeAction, toWebAction, new ScriptWindowAction(),
      new AtomSetChooserAction(), viewMeasurementTableAction, new GaussianAction() }
  ;

  class CloseAction extends AbstractAction {
    CloseAction() {
      super(closeAction);
    }

    public void actionPerformed(ActionEvent e) {
      Jmol.this.frame.hide();
      Jmol.this.doClose();
    }
  }

  static class ConsoleAction extends AbstractAction {

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

  class GaussianAction extends AbstractAction {
    public GaussianAction() {
      super(gaussianAction);
    }
    
    public void actionPerformed(ActionEvent e) {
      if (gaussianDialog == null)
        gaussianDialog = new GaussianDialog(frame, viewer);
      gaussianDialog.show();
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
      (new HelpDialog(frame)).show();
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
      Jmol.this.doClose();
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

      recentFiles.show();
      String selection = recentFiles.getFile();
      if (selection != null)
        viewer.openFileAsynchronously(selection);
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
        if (scriptWindow == null)
          return;
        int msWalltime = ((Integer) data[3]).intValue();
        // general message has msWalltime = 0
        // special messages have msWalltime < 0
        // termination message has msWalltime > 0 (1 + msWalltime)
        // "script started"/"pending"/"script terminated"/"script completed"
        //   do not get sent to console
        if (msWalltime > 0) {
          // termination -- button legacy
          scriptWindow.notifyScriptTermination();
        } else if (msWalltime < 0) {
          if (msWalltime == -2)
            scriptWindow.notifyScriptStart();
        } else {
          scriptWindow.sendConsoleMessage(strInfo);
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
      if (scriptWindow != null) {
        scriptWindow.sendConsoleMessage(info);
        scriptWindow.sendConsoleMessage("\n");
      }
    }

    private void notifyFileLoaded(String fullPathName, String fileName,
                                  String modelName, String errorMsg) {
      if (errorMsg != null) {
        return;
      }
      if (display == null)
        return;
      
      // this code presumes only ptLoad = -1 (error), 0 (zap), or 3 (completed)
      
      //      jmolpopup.updateComputedMenus();
      String title = "Jmol";
      if (fullPathName == null) {
        if (fileName != null && scriptWindow != null)
          scriptWindow.undoClear();
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
      if (scriptWindow != null)
        scriptWindow.sendConsoleEcho(strEcho);
    }

    private void sendConsoleMessage(String strStatus) {
      if (scriptWindow != null)
        scriptWindow.sendConsoleMessage(strStatus);
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
        if (scriptWindow != null) {
          scriptWindow
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
      if (scriptWindow == null)
        return;
      if (showConsole)
        scriptWindow.show();
      else
        scriptWindow.hide();
    }

    public float[][] functionXY(String functionName, int nX, int nY) {
      nX = Math.abs(nX);
      nY = Math.abs(nY);
      float[][] f = new float[nX][nY];
      boolean isSecond = (functionName.indexOf("2") >= 0);

      for (int i = nX; --i >= 0;)
        for (int j = nY; --j >= 0;) {
          f[i][j] = (isSecond ? (float) ((i + j - nX) / (2f)) : (float) Math
              .sqrt(Math.abs(i * i + j * j)) / 2f);
          //if (i < 10 && j < 10)
          //System.out.println(" functionXY " + i + " " + j + " " + f[i][j]);
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
