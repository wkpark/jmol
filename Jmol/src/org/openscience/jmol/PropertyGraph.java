/*
 * PropertyGraph.java
 *
 */
package org.openscience.jmol;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import ptolemy.plot.*;

public class PropertyGraph extends JDialog {

    private Plot plotter;

    /**
     * Creates a dialog. 
     *
     * @param f the parent frame
     * @param dp the displayPanel in which the vibration will be displayed
     */
    public PropertyGraph(JFrame f, displayPanel dp) {
        super(f, "Property Graph", false);
        display = dp;
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        /* Put all of the UI stuff here */
        plotter = new Plot();
        plotter.setSize(560,385);  
        container.add(plotter);
        
        addWindowListener(new GraphWindowListener());
        getContentPane().add(container);
        pack();
        centerDialog();
    }
    
    /**
     * Set ChemFile from which the Frame Properties will be loaded.
     * If no Frame Properties are found, the PropertyGraph action will
     * be disabled; otherwise, it will be enabled.
     *
     * @param inputFile the ChemFile containing potentially graphable data
     */
    public void setChemFile(ChemFile inputFile) {
        System.err.println("Checking for viewable properties...");
        if (isVisible()) {
            setVisible(false);
        }
        restoreConflictingActions();
        hasGraphableProperties = false;
        GPs = new Vector();
        this.inputFile = inputFile;
        
        // Check for graphable properties

        Vector filePL = inputFile.getFramePropertyList();

        for (int j = 0; j < filePL.size(); j++) {          
            
            System.out.println(filePL.elementAt(j));
            
            if (GPs.indexOf(filePL.elementAt(j)) < 0) {
                
                System.out.println("Found one");
                if (filePL.elementAt(j).equals("Energy")) {
                    GPs.addElement(filePL.elementAt(j));
                    hasGraphableProperties = true;
                }
            }            
            
        }

        if (hasGraphableProperties) { 
            graphAction.setEnabled(true);
        } else {
            graphAction.setEnabled(false);
        }
    }

    public void findData() {        
        if (hasGraphableProperties) {
            
            plotter.setTitle("Properties vs. Frame Number");
            
            for (int j = 0; j < GPs.size(); j++ ) {
                String desc = (String)GPs.elementAt(j);
                
                plotter.addLegend(j, desc);
                
                for (int i = 0; i < inputFile.nFrames(); i++) {
                    Vector fp = inputFile.getFrame(i).getFrameProps();
                    for (Enumeration ef = fp.elements() ; 
                         ef.hasMoreElements() ;) {
                        PhysicalProperty pf = (PhysicalProperty) ef.nextElement();
                        
                        if (pf.getDescriptor().equals(desc)) {
                            
                            plotter.addPoint(j,
                                             (new Double(i+1)).doubleValue(),
                                             ((Double)pf.getProperty()).doubleValue(),
                                             true);
                        }
                    }
                }
            }
            plotter.fillPlot();
            plotter.setMarksStyle("dots");
            plotter.setConnected(true);                
        }
    }       
        
    /**
     * Shows or hides this component depending on the value of
     * parameter b.
     *
     * @param b If true, shows this component; otherwise, hides
     *   this component.
     */
    public void setVisible(boolean b) {
        if (b) {
            graphAction.setEnabled(false);
            disableConflictingActions();
        } else {
            graphAction.setEnabled(true);
            restoreConflictingActions();
        }
        super.setVisible(b);
    }
            
    /**
     * Returns the preferred size of the Graph dialog.
     *
     * @return the Dimension preferred by the dialog
     */
    public Dimension getPreferredSize() {
        return new Dimension(580, 450);
    }
    
    /**
     * Centers the dialog on the screen.
     */
    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height / 2;
        screenSize.width = screenSize.width / 2;
        size.height = size.height / 2;
        size.width = size.width / 2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x, y);
    }
    
    /**
     * The action used to activate this dialog.
     */
    class GraphAction extends AbstractAction {
        /**
         * Create the action.
         */
        public GraphAction() {
            super("graph");
            this.setEnabled(false);
        }
        
        /**
         * Shows the Property Graph dialog when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            findData();
            setVisible(true);
        }
    }
    
    /**
     * Returns the actions available from the Property Graph dialog.
     *
     * @return the actions for the PropertyGraph dialog
     */
    public Action[] getActions() {
        return new Action[] { graphAction };
    }
    
    /**
     * Gets the Vibrate action identified by the string given.
     *
     * @param cmd key for the desired action
     * @return the action associated with cmd, or null if not found
     */
    public Action getAction(String cmd) {
        if (cmd.equals(graphAction.getValue(Action.NAME))) {
            return graphAction;
        }
        return null;
    }
    
    /**
     * Add a action to be disabled when this dialog is shown.
     *
     * @param a1  a conflicting action
     */
    public void addConflictingAction(Action a1) {
        conflictingActions.addElement(a1);
    }
    
    /**
     * Disables conflicting actions. Saves action enabled
     * states for restoration.
     */
    private void disableConflictingActions() {
        actionStates = new Vector();
        Enumeration iter = conflictingActions.elements();
        while (iter.hasMoreElements()) {
            Action a1 = (Action)iter.nextElement();
            actionStates.addElement(new Boolean(a1.isEnabled()));
            a1.setEnabled(false);
        }
    }
    
    /**
     * Restores conflicting actions to saved state.
     */
    private void restoreConflictingActions() {
        if (actionStates != null) {
            Enumeration iter = conflictingActions.elements();
            Enumeration stateIter = actionStates.elements();
            while (iter.hasMoreElements() && stateIter.hasMoreElements()) {
                Action a1 = (Action)iter.nextElement();
                boolean state = ((Boolean)stateIter.nextElement()).booleanValue
                    ();
                a1.setEnabled(state);
            }
            actionStates = null;
        }
    }
        
    /**
     * Listener for responding to dialog window events.
     */
    class GraphWindowListener extends WindowAdapter  {
        /**
         * Closes the dialog when window closing event occurs.
         */
        public void windowClosing(WindowEvent e) {
            setVisible(false);
        }
    }
    
    /**
     * Reference to the panel for displaying frames. Used to load and unload
     * the vibration ChemFile, and to set the frame to be displayed.
     */
    private displayPanel display;
    /**
     * Does the dialog have any vibration data.
     */
    private boolean hasGraphableProperties;
    /*
     * The Vector containing the list of Graphable Properties.
     */
    private Vector GPs = new Vector();
    /**
     * The ChemFile containing frames with graphable data.
     */
    private ChemFile inputFile;
    /**
     * The action used to activate this dialog.
     */
    private GraphAction graphAction = new GraphAction();
    /**
     * List of actions which will interfere with the operation of this
     * dialog.
     */
    private Vector conflictingActions = new Vector();
    /**
     * Enabled states of conficting actions which are disabled
     * during the operation of this dialog. Used to restore states
     * when the dialog is closed.
     */
    private Vector actionStates = null;
    /**
     * Resource handler for loading interface strings and icons.
     */
    private static JmolResourceHandler jrh = new JmolResourceHandler("Graph");
}
