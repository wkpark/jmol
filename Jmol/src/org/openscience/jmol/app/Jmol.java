/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2004  The Jmol Development Team
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
import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.JmolStatusListener;

import org.jmol.api.ModelAdapter;
import org.jmol.adapter.cdk.CdkModelAdapter;
import org.jmol.adapter.smarter.SmarterModelAdapter;

import org.openscience.cdk.applications.plugin.CDKPluginManager;
import org.openscience.jmol.ui.JmolPopup;
import Acme.JPM.Encoders.ImageEncoder;
import Acme.JPM.Encoders.PpmEncoder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.obrador.JpegEncoder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
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

  private DisplayPanel display;
  private StatusBar status;
  private PreferencesDialog preferencesDialog;
  private MeasurementTable measurementTable;
  private RecentFilesDialog recentFiles;
  private JMenu recentFilesMenu;
  public ScriptWindow scriptWindow;
  private ExecuteScriptAction executeScriptAction;
  protected JFrame frame;
  private static File currentDir;
  private JFileChooser openChooser;
  private JFileChooser saveChooser;
  private FileTyper fileTyper;
  private JFileChooser exportChooser;
  private JmolPopup jmolpopup;
  private CDKPluginManager pluginManager;

  private GuiMap guimap = new GuiMap();
  
  private static int numWindows = 0;
  private static Dimension screenSize = null;
  int startupWidth, startupHeight;

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
  private static HistoryFile historyFile;

  Splash splash;

  public static HistoryFile getHistoryFile() {
    return historyFile;
  }

  private static JFrame consoleframe;

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
    historyFile = new HistoryFile(new File(ujmoldir, "history"),
        "Jmol's persistent values");
  }

  Jmol(Splash splash, JFrame frame, Jmol parent,
       int startupWidth, int startupHeight) {
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
    say("Initializing 3D display...");
    //
    display = new DisplayPanel(status, guimap);
    ModelAdapter modelAdapter;
    String adapter= System.getProperty("model");
    if (adapter == null || adapter.length() == 0)
      adapter = "smarter";
    if (adapter.equals("smarter")) {
      System.out.println("using Smarter Model Adapter");
      modelAdapter = new SmarterModelAdapter(null);
    } else if (adapter.equals("cdk")) {
      System.out.println("using CDK Model Adapter");
      modelAdapter = new CdkModelAdapter(null);
    } else {
      System.out.println("unrecognized model adapter:" + adapter +
                         " -- using Smarter");
      modelAdapter = new SmarterModelAdapter(null);
    }

    viewer = new JmolViewer(display, modelAdapter);
    display.setViewer(viewer);
    
    say("Initializing Preferences...");
    preferencesDialog = new PreferencesDialog(frame, guimap, viewer);
    say("Initializing Recent Files...");
    recentFiles = new RecentFilesDialog(frame);
    say("Initializing Script Window...");
    scriptWindow = new ScriptWindow(viewer, frame);
    viewer.setJmolStatusListener(new MyJmolStatusListener());

    say("Initializing Measurements...");
    measurementTable = new MeasurementTable(viewer, frame);

    // Setup Plugin system
    say("Loading plugins...");
    pluginManager = new CDKPluginManager(
        System.getProperty("user.home") + System.getProperty("file.separator")
        + ".jmol", new JmolEditBus(viewer)
    );
    pluginManager.loadPlugin("org.openscience.cdkplugin.dirbrowser.DirBrowserPlugin");
    pluginManager.loadPlugins(
        System.getProperty("user.home") + System.getProperty("file.separator")
        + ".jmol/plugins"
    );
    // feature to allow for globally installed plugins
    if (System.getProperty("plugin.dir") != null) {
        pluginManager.loadPlugins(System.getProperty("plugin.dir"));
    }

    // install the command table
    say("Building Command Hooks...");
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    menuItems = new Hashtable();
    say("Building Menubar...");
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

    pcs.addPropertyChangeListener(chemFileProperty, exportAction);
    pcs.addPropertyChangeListener(chemFileProperty, povrayAction);
    pcs.addPropertyChangeListener(chemFileProperty, pdfAction);
    pcs.addPropertyChangeListener(chemFileProperty, printAction);
    pcs.addPropertyChangeListener(chemFileProperty,
                                  viewMeasurementTableAction);

    jmolpopup = JmolPopup.newJmolPopup(viewer);

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
    frame.setSize(startupWidth, startupHeight);
    ImageIcon jmolIcon =
      JmolResourceHandler.getIconX("icon");
    Image iconImage = jmolIcon.getImage();
    frame.setIconImage(iconImage);
    // splash.showStatus(jrh.translate("Launching main frame..."));
    say("Launching main frame...");
  }

  public static Jmol getJmol(JFrame frame,
                             int startupWidth, int startupHeight) {
    ImageIcon splash_image = JmolResourceHandler.getIconX("splash");
    System.out.println("splash_image=" + splash_image);
    Splash splash = new Splash(frame, splash_image);
    splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
    splash.showStatus(JmolResourceHandler
                      .translateX("Creating main window..."));
    splash.showStatus(JmolResourceHandler.
                      translateX("Initializing Swing..."));
    try {
        UIManager
        .setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception exc) {
        System.err.println("Error loading L&F: " + exc);
    }
    
    screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    splash.showStatus(JmolResourceHandler.translateX("Initializing Jmol..."));
    
    // cache the current directory to speed up Jmol window creation
    currentDir = getUserDirectory();
    
    Jmol window = new Jmol(splash, frame, null, startupWidth, startupHeight);
    frame.show();
    return window;
  }

  public static void main(String[] args) {

    Jmol jmol = null;

    String modelFilename = null;
    String scriptFilename = null;

    Options options = new Options();
    options.addOption("h", "help", false, "give this help page");
    options.addOption(
        OptionBuilder.withLongOpt("script").
                      withDescription("script to run").
                      withValueSeparator('=').
                      hasArg().
                      create("s")
    );
    options.addOption(
        OptionBuilder.withArgName("property=value").
                      hasArg().
                      withValueSeparator().
                      withDescription(
"supported options are given below").
                      create("D")
    );
    
    options.addOption(
        OptionBuilder.withLongOpt("geometry").
                      withDescription("window size 500x500").
                      withValueSeparator().
                      hasArg().
                      create("g")
    );

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
        System.out.println("The -D options are as follows (defaults in parathesis):");
        System.out.println("  cdk.debugging=[true|false] (false)");
        System.out.println("  cdk.debug.stdout=[true|false] (false)");
        System.out.println("  display.speed=[fps|ms] (ms)");
        System.out.println("  JmolConsole=[true|false] (true)");
        System.out.println("  plugin.dir (unset)");
        System.out.println("  user.language=[EN|ES|NL] (EN)");

        System.exit(0);
    }

    try {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.1.2") < 0) {
        System.out.println("!!!WARNING: Swing components require a "
            + "1.1.2 or higher version VM!!!");
      }

      int startupWidth = 0, startupHeight = 0;
      if (line.hasOption("g")) {
        String geometry = line.getOptionValue("g");
        int indexX = geometry.indexOf('x');
        if (indexX > 0) {
          startupWidth = parseInt(geometry.substring(0, indexX));
          startupHeight = parseInt(geometry.substring(indexX + 1));
        }
      }
      if (startupWidth <= 0 || startupHeight <= 0) {
        startupWidth = 500;
        startupHeight = 550;
      }

      JFrame jmolFrame = new JFrame();
      jmol = getJmol(jmolFrame, startupWidth, startupHeight);

      // Process command line arguments
      args = line.getArgs();
      if (args.length > 0) {
          modelFilename = args[0];
      }
      if (line.hasOption("s")) {
          scriptFilename = line.getOptionValue("s");
      }

      // Open a file if one is given as an argument
      if (modelFilename != null)
        {
        jmol.viewer.openFile(modelFilename);
        jmol.viewer.getOpenFileError();
        }

      // Oke, by now it is time to execute the script
      if (scriptFilename != null) {
        System.out.println("Executing script: " + scriptFilename);
        jmol.splash
          .showStatus(JmolResourceHandler.getStringX("Executing script..."));
        jmol.viewer.evalFile(scriptFilename);
      }
    } catch (Throwable t) {
      System.out.println("uncaught exception: " + t);
      t.printStackTrace();
    }

    Point location = jmol.frame.getLocation();
    Dimension size = jmol.frame.getSize();

    // Adding console frame to grab System.out & System.err
    consoleframe = new JFrame("Jmol Console");
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
    
    consoleframe.setBounds(location.x, location.y + size.height, size.width,
        200);

    // I'd prefer that the console stay out of the way until requested,
    // so I'm commenting this line out for now...
    // consoleframe.show();

  }

  static int parseInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException nfe) {
      return Integer.MIN_VALUE;
    }
  }

  private void say(String message) {
      if (splash == null) {
          System.out.println(message);
      } else {
          splash.showStatus(JmolResourceHandler.translateX(message));
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
      if (numWindows <= 1) {
          System.out.println("Closing Jmol...");
          pluginManager.closePlugins();
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
  protected JMenuItem createMenuItem(String cmd) {

    JMenuItem mi;
    if (cmd.endsWith("Check")) {
      mi = guimap.newJCheckBoxMenuItem(cmd, false);
    } else {
      mi = guimap.newJMenuItem(cmd);
    }
    String mnem = JmolResourceHandler.getStringX(cmd + "Mnemonic");
    if (mnem != null) {
      char mn = mnem.charAt(0);
      mi.setMnemonic(mn);
    }
    
    ImageIcon f =
      JmolResourceHandler.getIconX(cmd + "Image");
    if (f != null) {
      mi.setHorizontalTextPosition(JButton.RIGHT);
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

    Action a = getAction(key);
    if (a != null) {
      b.setActionCommand(key);
      b.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(b));
      b.setEnabled(a.isEnabled());
    } else {
      b.setEnabled(false);
    }

    String tip = JmolResourceHandler.getStringX(key + "Tip");
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
    JMenuBar mb = new JMenuBar();
    addNormalMenuBar(mb);
    if (pluginManager != null) {
        mb.add(pluginManager.getMenu());
    }
    mb.add(Box.createHorizontalGlue());
    addHelpMenuBar(mb);
    return mb;
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
            String mnem = JmolResourceHandler
              .getStringX(menuKeys[i] + "Mnemonic");
            if (mnem != null) {
                char mn = mnem.charAt(0);
                m.setMnemonic(mn);
            }
        }
    }
  }
  
  protected void addHelpMenuBar(JMenuBar menuBar) {
      String menuKey = "help";
      JMenu m = createMenu(menuKey);
      if (m != null) {
          menuBar.add(m);
      }
      String mnem =
        JmolResourceHandler.getStringX(menuKey + "Mnemonic");
      if (mnem != null) {
          char mn = mnem.charAt(0);
          m.setMnemonic(mn);
      }
  }
      
  /**
   * Create a menu for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
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
          pm = recentFilesMenu = createMenu(item);
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
  private static final String printActionProperty = "print";
  private static final String recentFilesAction = "recentFiles";
  private static final String povrayActionProperty = "povray";
  private static final String pdfActionProperty = "pdf";
  private static final String scriptAction = "script";


  // --- action implementations -----------------------------------

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
    new OpenUrlAction(), printAction, exportAction,
    new CloseAction(), new ExitAction(), new AboutAction(),
    new WhatsNewAction(),
    new UguideAction(), new ConsoleAction(),
    new RecentFilesAction(), povrayAction, pdfAction,
    new ScriptWindowAction(), viewMeasurementTableAction
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
          Jmol newJmol = new Jmol(null, newFrame, Jmol.this,
                                  startupWidth, startupHeight);
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

      int retval = openChooser.showOpenDialog(Jmol.this);
      if (retval == 0) {
        File file = openChooser.getSelectedFile();
        viewer.evalString("load " + file.getAbsolutePath());
        return;
      }
    }
  }

  class OpenUrlAction extends NewAction {

    String title;
    String prompt;

    OpenUrlAction() {
      super(openurlAction);
      title = JmolResourceHandler.getStringX("OpenUrl.title");
      prompt = JmolResourceHandler.getStringX("OpenUrl.prompt");
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

      Frame frame = getFrame();

      ImageTyper it = new ImageTyper(exportChooser);

      exportChooser.setAccessory(it);

      int retval = exportChooser.showSaveDialog(Jmol.this);
      if (retval == 0) {
        File file = exportChooser.getSelectedFile();

        System.out.println("file chosen=" + file);
        if (file != null) {
          try {
            Image eImage = viewer.getScreenImage();
            FileOutputStream os = new FileOutputStream(file);
            
            if (it.getType().equals("JPEG")) {
              int quality = it.getQuality();
              JpegEncoder jc = new JpegEncoder(eImage, quality, os);
              jc.Compress();
            } else if (it.getType().equals("PPM")) {
              PpmEncoder pc = new PpmEncoder(eImage, os);
              pc.encode();
            } else if (it.getType().equals("PNG")) {
              PngEncoder png = new PngEncoder(eImage);
              byte[] pngbytes = png.pngEncode();
              os.write(pngbytes);
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
          viewer.releaseScreenImage();
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
        viewer.getOpenFileError();
      }
    }
  }

  class ScriptWindowAction extends AbstractAction {

    public ScriptWindowAction() {
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
      String title = "Jmol";
      if (modelName != null && fileName != null)
	  title = fileName + " - " + modelName;
      else if (fileName != null)
	  title = fileName;
      else if (modelName != null)
	  title = modelName;
      frame.setTitle(title);
      recentFiles.notifyFileOpen(fullPathName);
      pcs.firePropertyChange(chemFileProperty, null, clientFile);
      jmolpopup.updateComputedMenus();
    }

    public void notifyFileNotLoaded(String fullPathName, String errorMsg) {
      JOptionPane.showMessageDialog(null,
                                    fullPathName + '\n' + errorMsg,
                                    "File not loaded",
                                    JOptionPane.ERROR_MESSAGE);
    }

    public void notifyFrameChanged(int frameNo) {
        // don't do anything
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

    public void handlePopupMenu(int x, int y) {
      jmolpopup.show(x, y);
    }

    public void notifyMeasurementsChanged() {
      measurementTable.updateTables();
    }

    public void notifyAtomPicked(int atomIndex, String strInfo) {
      if (scriptWindow != null) {
        scriptWindow.scriptStatus(strInfo);
        scriptWindow.scriptStatus("\n");
      }
    }
  }

  class ExecuteScriptAction extends AbstractAction {
    public ExecuteScriptAction() {
      super("executeScriptAction");
    }

    public void actionPerformed(ActionEvent e) {
      viewer.evalString(e.getActionCommand());
    }
  }
}
