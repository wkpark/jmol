/*
 * @(#)Jmol.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */
package org.openscience.jmol;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.io.File;
import java.util.*;

import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.JToolBar.*;

import Acme.JPM.Encoders.*;

class Jmol extends JPanel {

    private JScrollPane scroller;
    private JViewport port;
    public static displayPanel display;
    public StatusBar status;
    private AtomPropsMenu apm;
    static AtomTypeTable atomTypeTable;
    private Preferences prefs;
    private Animate anim;
    private Vibrate vib;
    private PropertyGraph pg;
    private Measure meas;
    private MeasurementList mlist;
    private static JFrame frame;
    private ChemFile cf;
    private JFileChooser openChooser = new JFileChooser();
    private JFileChooser saveChooser = new JFileChooser();
    private JFileChooser exportChooser = new JFileChooser();
    private FileTyper ft;

    private static JmolResourceHandler jrh;
    
    public static File UserPropsFile;
    public static File UserAtypeFile;
    
    private static JFrame consoleframe;

    private DisplaySettings settings = new DisplaySettings();

    static {
        if (System.getProperty("user.home") == null) {
            System.err.println("Error starting Jmol: the property 'user.home' is not defined.");
            System.exit(1);
        }
        File ujmoldir = new File(new File(System.getProperty("user.home")), ".jmol");
        ujmoldir.mkdirs();
        JmolResourceHandler.initialize("org.openscience.jmol.Properties.Jmol");        
        UserPropsFile = new File(ujmoldir, "properties");
        UserAtypeFile = new File(ujmoldir, "AtomTypes");
        jrh = new JmolResourceHandler("Jmol");
    }
    
    Jmol(Splash splash) {
	super(true);
        
        splash.showStatus("Initializing Swing...");
        try {
	    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
	} catch (Exception exc) {
	    System.err.println("Error loading L&F: " + exc);
	}
        
	setBorder(BorderFactory.createEtchedBorder());
	setLayout(new BorderLayout());
        
        scroller = new JScrollPane();
        port = scroller.getViewport();
        
	try {
	    String vpFlag = jrh.getString("ViewportBackingStore");
	    Boolean bs = new Boolean(vpFlag);
	    port.setBackingStoreEnabled(bs.booleanValue());
	} catch (MissingResourceException mre) {
	    // just use the viewport default
	}

        status = (StatusBar)createStatusBar();
        splash.showStatus("Initializing 3D display...");
        display = new displayPanel(status, settings);
        splash.showStatus("Initializing Preferences...");
        prefs = new Preferences(frame, display);
        splash.showStatus("Initializing Animate...");
        anim = new Animate(frame, display);
        splash.showStatus("Initializing Vibrate...");
        vib = new Vibrate(frame, display);
        splash.showStatus("Initializing Property Graph...");
        pg = new PropertyGraph(frame);
        splash.showStatus("Initializing Measurements...");
        mlist = new MeasurementList(frame, display);
        meas = new Measure(frame, display);
        meas.setMeasurementList(mlist);
        display.setMeasure(meas);
        mlist.addMeasurementListListener(display);
        port.add(display);
        splash.showStatus("Initializing Chemical Shifts...");
        chemicalShifts.initialize();
		// install the command table
        splash.showStatus("Building Command Hooks...");
		commands = new Hashtable();
        Action[] actions = getActions();
		for (int i = 0; i < actions.length; i++) {
			Action a = actions[i];
			commands.put(a.getValue(Action.NAME), a);
        }
        // Fix for actions that confict with the operation of
        // vibration animations
        Action[] animActions = anim.getActions();
        for (int i=0; i < animActions.length; ++i) {
            vib.addConflictingAction(animActions[i]);
        }
        vib.addConflictingAction(getAction(openAction));
	menuItems = new Hashtable();
        splash.showStatus("Building Menubar...");
	menubar = createMenubar();
        add("North", menubar);
        
	JPanel panel = new JPanel();
	panel.setLayout(new BorderLayout());	
	panel.add("North",createToolbar1());
        
        JPanel ip = new JPanel();
        ip.setLayout(new BorderLayout());
        ip.add("West", createToolbar2());
        ip.add("Center", scroller);
	panel.add("Center", ip);
        add("Center", panel);
        add("South", status);       

        splash.showStatus("Starting display...");
        display.start();        

        splash.showStatus("Reading AtomTypes...");
        atomTypeTable = new AtomTypeTable(frame, UserAtypeFile);
        splash.showStatus("Setting up File Choosers...");
        ft = new FileTyper(openChooser);
        openChooser.setAccessory(ft);
        File currentDir = getUserDirectory();
        openChooser.setCurrentDirectory(currentDir);
        saveChooser.setCurrentDirectory(currentDir);
        exportChooser.setCurrentDirectory(currentDir);        
    }
    
    public static void main(String[] args) {

        // Read the first argument as a file name:
        File initialFile = null;
        if (args.length != 0) {            
            /* Read only one argument as a file name for now: */
            String astring = args[0];            
            initialFile = new File(getUserDirectory(), astring);
        }        

        try {
            String vers = System.getProperty("java.version");
            if (vers.compareTo("1.1.2") < 0) {
                System.out.println("!!!WARNING: Swing components require a " +
                                   "1.1.2 or higher version VM!!!");
            }
            
            frame = new JFrame();
            
            ImageIcon splash_image = jrh.getIcon("splash");
            Splash splash = new Splash(frame, splash_image);
            splash.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            splash.showStatus("Creating main window...");
            frame.setTitle(jrh.getString("Title"));
            frame.setBackground(Color.lightGray);
            frame.getContentPane().setLayout(new BorderLayout());
            splash.showStatus("Initializing Jmol...");
            frame.getContentPane().add("Center", new Jmol(splash));
            frame.addWindowListener(new AppCloser());
            frame.pack();
            frame.setSize(500, 600);
            splash.showStatus("Launching main frame...");
            frame.show();
        } catch (Throwable t) {
            System.out.println("uncaught exception: " + t);
            t.printStackTrace();
        }
        
        // Adding consoleframe to grab System.out & System.err
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
        
        Point location = frame.getLocation();
        Dimension size = frame.getSize();
        consoleframe.setBounds(location.x, location.y+size.height, 
                               size.width, 200);        
        
        // I'd prefer that the console stay out of the way until requested,
        // so I'm commenting this line out for now...
        // consoleframe.show();        

    }

    /**
     * Opens a file with a hint to use a particular reader, defaulting to
     * the ReaderFactory if the hint doesn't match any known file types.
     */
    public void openFile(File theFile, String typeHint) {
        if(theFile != null) {
            try {
                FileInputStream is = new FileInputStream(theFile);
                
                if (typeHint.equals("PDB")) {
                    ChemFileReader reader = new PDBReader(new InputStreamReader(is)); 
                    cf = reader.read();
                } else if (typeHint.equals("CML")) {
                    ChemFileReader reader = new CMLReader(new InputStreamReader(is));
                    cf = reader.read();
                } else if(typeHint.equals("XYZ (xmol)")) {
                    ChemFileReader reader = new XYZReader(new InputStreamReader(is)); 
                    cf = reader.read();
                } else {
                    // Try to automagically determine file type:
                    ChemFileReader reader = ReaderFactory.createReader(new InputStreamReader(is));                
                    if (reader == null){
                        throw new JmolException("openFile",
                                                "Unknown file type");
                    }
                    cf = reader.read();
                }
            
                if (cf != null) {
                    display.setChemFile(cf);
                    anim.setChemFile(cf);
                    vib.setChemFile(cf);
                    pg.setChemFile(cf);
                    frame.setTitle(theFile.getName());     
                    apm.replaceList(cf.getAtomPropertyList());
                    mlist.clear();
					
					chemicalShifts.setChemFile(cf, apm);
                }

            } catch(java.io.FileNotFoundException e2) {
                JOptionPane.showMessageDialog(Jmol.this, 
                                              ("File not found: " +
                                               theFile));
            } catch(JmolException e3) {
                JOptionPane.showMessageDialog(Jmol.this, 
                                              ("File type undetermined: " +
                                               theFile));
            } catch(Exception exc) {
                System.out.println(exc.toString());
                exc.printStackTrace();
            }
            return;
        }
    } 

    /**
     * returns the ChemFile that we are currently working with
     *
     * @see ChemFile
     */
    public ChemFile getCurrentFile() {
        return cf;
    }

    /**
     * returns a list of Actions that is understood by the upper level
     * application
     */
    public Action[] getActions() {

        Action[] displayActions = display.getActions();        
        Action[] prefActions = prefs.getActions();
        Action[] animActions = anim.getActions();
        Action[] measActions = meas.getActions();
        Action[] mlistActions = mlist.getActions();
        Action[] vibActions = vib.getActions();
        Action[] pgActions = pg.getActions();

        int nactions = defaultActions.length + displayActions.length + 
            prefActions.length + animActions.length + vibActions.length +
            measActions.length + mlistActions.length + pgActions.length; 
        
        Action[] theActions = new Action[nactions];

        // YARG.  This is way ugly.  Clean this up!

        System.arraycopy(defaultActions, 0, theActions, 0, 
                         defaultActions.length); 
        System.arraycopy(displayActions, 0, theActions, 
                         defaultActions.length, displayActions.length);
        System.arraycopy(prefActions, 0, theActions, 
                         defaultActions.length+displayActions.length, 
                         prefActions.length); 
        System.arraycopy(animActions, 0, theActions, 
                         defaultActions.length+displayActions.length 
                         +prefActions.length, 
                         animActions.length); 
        System.arraycopy(measActions, 0, theActions, 
                         defaultActions.length+displayActions.length 
                         +prefActions.length+animActions.length, 
                         measActions.length); 
        System.arraycopy(mlistActions, 0, theActions, 
                         defaultActions.length+displayActions.length 
                         +prefActions.length+animActions.length + 
                         measActions.length, 
                         mlistActions.length); 
        System.arraycopy(vibActions, 0, theActions, 
                         defaultActions.length+displayActions.length 
                         +prefActions.length+animActions.length + 
                         measActions.length + mlistActions.length, 
                         vibActions.length); 
        System.arraycopy(pgActions, 0, theActions, 
                         defaultActions.length+displayActions.length 
                         +prefActions.length+animActions.length + 
                         measActions.length + mlistActions.length +
                         vibActions.length, pgActions.length);
        return theActions;
    }
    
    /**
     * To shutdown when run as an application.  This is a
     * fairly lame implementation.   A more self-respecting
     * implementation would at least check to see if a save
     * was needed.
     */
    protected static final class AppCloser extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
	    System.exit(0);
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
        JMenuItem mi;
        if (isRadio) {
            mi = new JRadioButtonMenuItem(jrh.getString(cmd + 
                                                            labelSuffix));
        } else {
            String checked = jrh.getString(cmd + checkSuffix);        
            if (checked != null) {
                boolean c = false;
                if (checked.equals("true")) c = true;
                mi = new JCheckBoxMenuItem(jrh.getString(cmd + 
                                                             labelSuffix), c);
            } else {
                mi = new JMenuItem(jrh.getString(cmd + labelSuffix));
            }
        }
        String mnem = jrh.getString(cmd + mnemonicSuffix);
	if (mnem != null) {
            char mn = mnem.charAt(0);
            mi.setMnemonic(mn);
        }
        /*        String accel = jrh.getString(cmd + acceleratorSuffix);
	if (accel != null) {
            if (accel.startsWith("Ctrl-")) {
                char ac = accel.charAt(5);
                mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 
                                                         ActionEvent.CTRL_MASK));
            }
            if (accel.startsWith("Alt-")) {
                char ac = accel.charAt(4);
                mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 
                                                         ActionEvent.ALT_MASK));
            }
        }
        */
        ImageIcon f = jrh.getIcon(cmd + imageSuffix);
	if (f != null) {
	    mi.setHorizontalTextPosition(JButton.RIGHT);
	    mi.setIcon(f);
	}
	String astr = jrh.getString(cmd + actionSuffix);
	if (astr == null) {
	    astr = cmd;
	}
	mi.setActionCommand(astr);
	Action a = getAction(astr);
	if (a != null) {
	    mi.addActionListener(a);
	    a.addPropertyChangeListener(createActionChangeListener(mi));
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
     * @returns item created for the given command or null
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
        
    protected Container getToolbar1() {
	return toolbar1;
    }
    
    protected Container getToolbar2() {
	return toolbar2;
    }

    protected JMenuBar getMenubar() {
	return menubar;
    }


    /**
     * Create the first toolbar.  By default this reads the 
     * resource file for the definition of the toolbars.
     */
    private Component createToolbar1() {
	toolbar1 = new JToolBar();
	String[] tool1Keys = tokenize(jrh.getString("toolbar1"));
	for (int i = 0; i < tool1Keys.length; i++) {
	    if (tool1Keys[i].equals("-")) {
                toolbar1.addSeparator();
	    } else {
                toolbar1.add(createTool(tool1Keys[i]));
            }
        }
        //Action handler implementation would go here.
        toolbar1.add(Box.createHorizontalGlue());
        // toolbar1.putClientProperty( "JToolBar.isRollover", Boolean.TRUE);
        return toolbar1;
    }

    private Component createToolbar2() {
	toolbar2 = new JToolBar();
        // make it vertical;
        toolbar2.setOrientation(JToolBar.VERTICAL);
        // BasicToolBarUI ui = (BasicToolBarUI) toolbar2.getUI();
        // ui.setOrientation(BasicToolBarUI.VERTICAL);
	String[] tool2Keys = tokenize(jrh.getString("toolbar2"));
	for (int i = 0; i < tool2Keys.length; i++) {
	    if (tool2Keys[i].equals("-")) {
                toolbar2.addSeparator();
	    } else {
                toolbar2.add(createTool(tool2Keys[i]));
            }
        }
        //Action handler implementation would go here.
        toolbar2.add(Box.createVerticalGlue());
        // toolbar2.putClientProperty( "JToolBar.isRollover", Boolean.TRUE);
        return toolbar2;
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
    protected JButton createToolbarButton(String key) {
	ImageIcon ii = jrh.getIcon(key + imageSuffix);
        JButton b = new JButton(ii) {
            public float getAlignmentY() { return 0.5f; }
	};
        b.setRequestFocusEnabled(false);
        b.setMargin(new Insets(1,1,1,1));

	String astr = jrh.getString(key + actionSuffix);
	if (astr == null) {
	    astr = key;
	}
	Action a = getAction(astr);
	if (a != null) {
	    b.setActionCommand(astr);
	    b.addActionListener(a);
	} else {
	    b.setEnabled(false);
	}
        
	String tip = jrh.getString(key + tipSuffix);
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
     */
    protected String[] tokenize(String input) {
	Vector v = new Vector();
	StringTokenizer t = new StringTokenizer(input);
	String cmd[];

	while (t.hasMoreTokens())
	    v.addElement(t.nextToken());
	cmd = new String[v.size()];
	for (int i = 0; i < cmd.length; i++)
	    cmd[i] = (String) v.elementAt(i);

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

	String[] menuKeys = tokenize(jrh.getString("menubar"));
	for (int i = 0; i < menuKeys.length; i++) {
            if (menuKeys[i].equals("-")) {
                mb.add(Box.createHorizontalGlue());
            } else {                
                JMenu m = createMenu(menuKeys[i],false);

                if (m != null) {
                    mb.add(m);
                }
                String mnem = jrh.getString(menuKeys[i] + mnemonicSuffix);
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
        if (isPopup) itemKeys = tokenize(jrh.getString(key + popupSuffix));
        else itemKeys = tokenize(jrh.getString(key));
        // Get label associated with this menu:
        JMenu menu = new JMenu(jrh.getString(key + "Label"));        
        // Loop over the items in this menu:
        for (int i = 0; i < itemKeys.length; i++) {
            // Check to see if it is a radio group:
            String radiogroup = jrh.getString(itemKeys[i] + radioSuffix);
            if (radiogroup != null) {
                // Get the list of items in the radio group:
                String[] radioKeys = tokenize(radiogroup);
                // See what is the selected member of the radio group:
                String si = jrh.getString(itemKeys[i] + selectedSuffix);
                // Create the button group:
                ButtonGroup bg = new ButtonGroup();
                // Loop over the items in the radio group:
                for (int j = 0; j < radioKeys.length; j++) {
                    JRadioButtonMenuItem mi = (JRadioButtonMenuItem) 
                        createMenuItem(radioKeys[j], true);
                    menu.add(mi);
                    bg.add(mi);              
                    if (radioKeys[j].equals(si)) mi.setSelected(true);
                }                                        
            } else {                
                if (itemKeys[i].equals("-")) {
                    menu.addSeparator();
                } else {
                    // Check to see if it is a popup menu:
                    String popup = jrh.getString(itemKeys[i] + 
                                                     popupSuffix);
                    if (popup != null) {
                        if (popup.equals("prop")) {                            
                            apm = new AtomPropsMenu(jrh.getString(itemKeys[i] 
                                                                  + "Label"), settings);
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
        }
	return menu;
    }


    // Yarked from JMenu, ideally this would be public.
    protected PropertyChangeListener createActionChangeListener(JMenuItem b) {
	return new ActionChangedListener(b);
    }
    
    // Yarked from JMenu, ideally this would be public.
    private class ActionChangedListener implements PropertyChangeListener {
        JMenuItem menuItem;
        
        ActionChangedListener(JMenuItem mi) {
            super();
            this.menuItem = mi;
        }
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)) {
                String text = (String) e.getNewValue();
                menuItem.setText(text);
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                menuItem.setEnabled(enabledState.booleanValue());
            }
        }
    }

    private Hashtable commands;
    private Hashtable menuItems;
    private JMenuBar menubar;
    private JToolBar toolbar1;
    private JToolBar toolbar2;


    /**
     * Listener for the edits on the current document.
     */
    protected UndoableEditListener undoHandler = new UndoHandler();

    /** UndoManager that we add edits to. */
    protected UndoManager undo = new UndoManager();

    /**
     * Suffix applied to the key used in resource file
     * lookups for an image.
     */
    public static final String imageSuffix = "Image";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a label.
     */
    public static final String labelSuffix = "Label";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a checkbox menu item.
     */
    public static final String checkSuffix = "Check";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a radio group.
     */
    public static final String radioSuffix = "Radio";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a selected member of a radio group.
     */
    public static final String selectedSuffix = "Selected";

    /**
     * Suffix applied to the key used in resource file
     * lookups for a popup menu.
     */
    public static final String popupSuffix = "Popup";

    /**
     * Suffix applied to the key used in resource file
     * lookups for an action.
     */
    public static final String actionSuffix = "Action";

    /**
     * Suffix applied to the key used in resource file
     * lookups for tooltip text.
     */
    public static final String tipSuffix = "Tooltip";

    /**
     * Suffix applied to the key used in resource file
     * lookups for Mnemonics.
     */
    public static final String mnemonicSuffix = "Mnemonic";

    /**
     * Suffix applied to the key used in resource file
     * lookups for Mnemonics.
     */
    public static final String acceleratorSuffix = "Accelerator";
    
    public static final String openAction = "open";
    public static final String newAction  = "new";
    public static final String saveAction = "save";
    public static final String exportAction = "export";
    public static final String exitAction = "exit";
    public static final String aboutAction = "about";
    public static final String prefsAction = "prefs";
    public static final String animAction = "animate";
    public static final String vibAction = "vibrate";
    public static final String graphAction = "graph";
    public static final String whatsnewAction = "whatsnew";
    public static final String uguideAction = "uguide";
    public static final String atompropsAction = "atomprops";
    public static final String printAction = "print";

    class UndoHandler implements UndoableEditListener {

	/**
	 * Messaged when the Document has created an edit, the edit is
	 * added to <code>undo</code>, an instance of UndoManager.
	 */
        public void undoableEditHappened(UndoableEditEvent e) {
	    undo.addEdit(e.getEdit());
	    undoAction.update();
	    redoAction.update();
	}
    }

    
    // --- action implementations -----------------------------------
    
    private CalculateChemicalShifts chemicalShifts = new CalculateChemicalShifts();
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();
    
    /**
     * Actions defined by the Jmol class
     */
    private Action[] defaultActions = {
	new NewAction(),
	new OpenAction(),
        new SaveAction(),
        new PrintAction(),
        new ExportAction(),
	new ExitAction(),
        new AboutAction(),
        new WhatsNewAction(),
        new UguideAction(),
        new AtompropsAction(),
        undoAction,
        redoAction,
	new ConsoleAction(),
	chemicalShifts
            };        
    
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

    class PrintAction extends AbstractAction {
        public PrintAction() {
            super(printAction);
        }

        public void actionPerformed(ActionEvent e) {
            Toolkit tk = Toolkit.getDefaultToolkit();
            PrintJob pJob = tk.getPrintJob(frame, "Jmol Print Job", null);
            Graphics pg = pJob.getGraphics();
            
            if (pg != null) {
                display.print(pg);
                pg.dispose();    // Flushes the print job
            }
        }
    }

    class UndoAction extends AbstractAction {
	public UndoAction() {
	    super("Undo");
	    this.setEnabled(false);
	}
        
	public void actionPerformed(ActionEvent e) {
	    try {
		undo.undo();
	    } catch (CannotUndoException ex) {
		status.setStatus(1, "Unable to undo:");
		status.setStatus(2, ex.toString());
		ex.printStackTrace();
	    }
	    update();
	    redoAction.update();
	}

	protected void update() {
	    if(undo.canUndo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undo.getUndoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Undo");
	    }
	}
    }

    class RedoAction extends AbstractAction {
	public RedoAction() {
	    super("Redo");
	    this.setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
	    try {
		undo.redo();
	    } catch (CannotRedoException ex) {
		status.setStatus(1, "Unable to redo:");
                status.setStatus(2, ex.toString());
		ex.printStackTrace();
	    }
	    update();
	    undoAction.update();
	}
        
	protected void update() {
	    if(undo.canRedo()) {
		this.setEnabled(true);
		putValue(Action.NAME, undo.getRedoPresentationName());
	    }
	    else {
		this.setEnabled(false);
		putValue(Action.NAME, "Redo");
	    }
	}
    }

    class OpenAction extends NewAction {

	OpenAction() {
	    super(openAction);
	}
        
        public void actionPerformed(ActionEvent e) {
            
            int retval = openChooser.showOpenDialog(Jmol.this);
            if(retval == 0) {
                File theFile = openChooser.getSelectedFile();
                openFile(theFile, ft.getType());
                return;
            } 
            JOptionPane.showMessageDialog(Jmol.this, "No file chosen");
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

    class SaveAction extends AbstractAction{
        
	SaveAction() {
	    super(saveAction);
	}
        
        public void actionPerformed(ActionEvent e) {
            Frame frame = getFrame();                        
            FileTyper ft = new FileTyper(saveChooser);
            saveChooser.setAccessory(ft);
            int retval = saveChooser.showSaveDialog(Jmol.this);
            if(retval == 0) {
                File theFile = saveChooser.getSelectedFile();
                if(theFile != null) {
                    try {
                        FileOutputStream os = new FileOutputStream(theFile);
                        
                        if (ft.getType().equals("XYZ (xmol)")) {
                            XYZSaver xyzs = new XYZSaver(getCurrentFile(),os);
                            xyzs.writeFile();
                        } else if (ft.getType().equals("PDB")) {
                            // PDBSaver ps = new PDBSaver(getCurrentFile(), os);
                            // ps.writeFile();
                        } else if (ft.getType().equals("CML")) {
                            CMLSaver cs = new CMLSaver(getCurrentFile(), os);
                            cs.writeFile();
                        } else {
                        }
                        
                        os.flush();
                        os.close();
                        
                    } catch(Exception exc) {
                        status.setStatus(1, "Exception:");
                        status.setStatus(2, exc.toString());
                        System.out.println(exc.toString());
                    }
                    return;
                }
            } 
        }                        
    }

    class ExportAction extends AbstractAction{
        
	ExportAction() {
	    super(exportAction);
	}
        
        public void actionPerformed(ActionEvent e) {
            Frame frame = getFrame();                        
            
            
            ImageTyper it = new ImageTyper(exportChooser);
            // GIF doesn't support more than 8 bits:
            if (settings.getAtomDrawMode() == DisplaySettings.SHADING){
                it.disableGIF();
            }
            exportChooser.setAccessory(it);
            
            int retval = exportChooser.showSaveDialog(Jmol.this);
            if(retval == 0) {
                File theFile = exportChooser.getSelectedFile();
                
                if(theFile != null) {
                    try {
                        Image eImage = display.takeSnapshot();
                        FileOutputStream os = new FileOutputStream(theFile);
                        
                        if (it.getType().equals("JPEG")) {
                            int qual = 10*it.getQuality();
                            JpegEncoder jc = new JpegEncoder(eImage, qual, os);
                            jc.Compress();
                        } else if (it.getType().equals("PPM")) {
                            PpmEncoder pc = new PpmEncoder(eImage, os);
                            pc.encode();
                        } else if (it.getType().equals("GIF")) {
                            GifEncoder gc = new GifEncoder(eImage, os, true);
                            gc.encode();
                        } else {
                            // Do nothing
                        }
                        
                        os.flush();
                        os.close();
                        
                    } catch(IOException exc) {
                        status.setStatus(1, "IO Exception:");
                        status.setStatus(2, exc.toString());
                        System.out.println(exc.toString());
                    }
                    return;
                }
            } 
        }                        
    }
    
    /**
     * Returns a new File referenced by the property 'user.dir', or null
     * if the property is not defined.
     *
     * @returns  a File to the user directory 
     */
    static File getUserDirectory() {
        if (System.getProperty("user.dir") == null) {
            return null;
        }
        return new File(System.getProperty("user.dir"));
    }
}
