/* $RCSfile$
 * $Author jonathan gutow$
 * $Date Aug 5, 2007 9:19:06 AM $
 * $Revision$
 *
 * Copyright (C) 2005-2007  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */
package org.openscience.jmol.app.webexport;


import java.util.*;
import java.awt.*;
import javax.swing.*;

import java.text.*;
import org.jmol.api.*;

public class WebExport extends JPanel{
	
	/**
	 * 
	 */
	JmolViewer viewer;
	
	private static final long serialVersionUID = 1L;
	//run status
	static final int StandAlone = 0; 
	static final int inJmol = 1;
	static int RunStatus = inJmol; //assume running inside Jmol
	
	public WebExport(JmolViewer viewer){
		super(new GridLayout(1,1));
		//Define the tabbed pane
		JTabbedPane Maintabs = new JTabbedPane();

    //Create file chooser
    JFileChooser fc = new JFileChooser();

    WebPanel webPanels[] = new WebPanel[2];
    
    //Add tabs to the tabbed pane
    webPanels[0] = new PopInJmol(viewer, fc, webPanels, 0);
		Maintabs.addTab("Pop-In Jmol", ((PopInJmol) webPanels[0]).getPanel());
		webPanels[1] = new ScriptButtons(viewer, fc, webPanels, 1);
		Maintabs.addTab("ScriptButton Jmol", ((ScriptButtons) webPanels[1]).getPanel());
		Orbitals OrbitalCreator = new Orbitals();
		JComponent Orbitals = OrbitalCreator.Panel();
		Maintabs.addTab("Orbitals", Orbitals);
		Molecules MoleculeCreator = new Molecules();
		JComponent Molecules = MoleculeCreator.Panel();
		Maintabs.addTab("Molecules", Molecules);
// Uncomment to activate the test panel
//		Test TestCreator = new Test((Viewer)viewer);
//		JComponent Test = TestCreator.Panel();
//		Maintabs.addTab("Tests",Test);
		
		
		//The LogPanel should always be the last one
		LogPanel LogPane = new LogPanel();
		Maintabs.addTab("Log", LogPane.logPanel());
		
		//Add the tabbed pane to this panel
		add(Maintabs);
		
		//Uncomment the following line to use scrolling tabs.
        //tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

	}
	
	public static String TimeStamp_WebLink(){
		String out="      <small><a href=\"http://www.java.com\">Javascript</a>";
		Date now = new Date();
		String now_string = DateFormat.getDateInstance().format(now);//Specify medium verbosity on the date and time
		out = out+ " generated by a java program (<a href=\"http://www.uwosh.edu/faculty_staff/gutow/Jmol_Web_Page_Maker.shtml\">Jmol_Web_Page_Maker</a>) on "+now_string+". </small><small><br>";
		return out;	
	}
	
  private static JFrame webFrame;
    /*
   	 * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(JmolViewer viewer) {
		
        //Create and set up the window.
        if (webFrame != null) {
          webFrame.setVisible(true);
          webFrame.toFront();
          return;
        }
        webFrame = new JFrame("Jmol Web Page Maker");
        if (RunStatus == StandAlone) {
            //Make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        	webFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
        	webFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
		
        //Create and set up the content pane.
        JComponent newContentPane = new WebExport(viewer);
        newContentPane.setOpaque(true); //content panes must be opaque
        webFrame.setContentPane(newContentPane);
		
        //Display the window.
        webFrame.pack();
        webFrame.setVisible(true);
        if (RunStatus == StandAlone){
            //LogPanel.Log("Jmol_Web_Page_Maker is running as a standalone application");
        } else {
        	//LogPanel.Log("Jmol_Web_Page_Maker is running as a plug-in");
         }
    }
	
    //can we do this? -BH
    public static void main(String[] args) {
		//If we start here it is running as standalone
		RunStatus = StandAlone;
		System.out.println("Jmol_Web_Page_Maker is running as a standalone application");
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(null);
           }
        });
    }
    
    static String appletPath;
    static String getAppletPath() {
      return (appletPath == null ? ".." : appletPath);
    }
    static void setAppletPath(String path) {
      appletPath = path;
    }
}
