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

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jmol.util.TextFormat;
import org.jmol.viewer.*;

import java.awt.Image;
import java.net.*;
import java.io.IOException;

public class Resizable_User_Activated_Scripts extends JPanel
implements ActionListener{
	
	//The constants used to generate panels, etc.
	JButton saveButton, AddInstanceButton, deleteInstanceButton;
	JTextField appletPath;
	JEditorPane Instructions;
	JSpinner appletSizeSpinnerW;
  JSpinner appletSizeSpinnerH;
	ArrayListTransferHandler arrayListHandler;
	JFileChooser fc;
	JList InstanceList, ScriptList;
	Viewer viewer;

	Resizable_User_Activated_Scripts (Viewer viewer){
		this.viewer = viewer;
	}
	

	class InstanceCellRenderer extends JLabel implements ListCellRenderer {

	     public Component getListCellRendererComponent(
	       JList list,              // the list
	       Object value,            // value to display
	       int index,               // cell index
	       boolean isSelected,      // is the cell selected
	       boolean cellHasFocus)    // does the cell have focus
	     {
	         String s = ((JmolInstance)value).name;
	         setText(s);
	         if (isSelected) {
	             setBackground(list.getSelectionBackground());
	             setForeground(list.getSelectionForeground());
	         } else {
	             setBackground(list.getBackground());
	             setForeground(list.getForeground());
	         }
	         setEnabled(list.isEnabled());
	         setFont(list.getFont());
	         setOpaque(true);
	         return this;
	     }
	 }


		
//Need the panel maker and the action listener.
	public JComponent Panel(){
		
		//Create the brief description text
		JLabel Description = new JLabel("Create a web page where a text and button pane scrolls next to a resizable Jmol.");
		
		//Create the text field for the path to the Jmol applet
		appletPath = new JTextField(20);
		appletPath.addActionListener(this);
		appletPath.setText("../../Applets/Java/Jmol");
		
		//Path to applet panel
		JPanel pathPanel = new JPanel(); 
		pathPanel.setLayout(new BorderLayout());
//		JLabel pathLabel = new JLabel("Relative Path to Jmol Applet:");
//		pathPanel.add(pathLabel, BorderLayout.PAGE_START);
		pathPanel.add(appletPath, BorderLayout.PAGE_END);
		pathPanel.setBorder(BorderFactory.createTitledBorder("Relative Path to Jmol Applet:"));
		
		//Create the appletSize spinner so the user can decide what %
        // of the window width the applet should be.
    SpinnerNumberModel appletSizeModelW = new SpinnerNumberModel(60, //initial value
        20, //min
        100, //max
        5); //step size
appletSizeSpinnerW= new JSpinner(appletSizeModelW);
SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(60, //initial value
    20, //min
    100, //max
    5); //step size
appletSizeSpinnerH= new JSpinner(appletSizeModelH);
        //panel to hold spinner and label
        JPanel appletSizePanel = new JPanel();
        appletSizePanel.add(new JLabel("% of window for applet width:"));
        appletSizePanel.add(appletSizeSpinnerW);
        appletSizePanel.add(new JLabel("height:"));
        appletSizePanel.add(appletSizeSpinnerH);

        //For layout combine path and size into one panel
        JPanel PathSizePanel = new JPanel();
        PathSizePanel.setLayout(new BorderLayout());	
        PathSizePanel.add(pathPanel, BorderLayout.PAGE_START);
        PathSizePanel.add(appletSizePanel, BorderLayout.CENTER);
        
		//Create the instructions sub window (scrolling to read html file)
		Instructions = new JEditorPane();
		Instructions.setEditable(false);
		URL InstructionsURL = this.getClass().getResource(
		                                "resizable_instructions.html");
		if (InstructionsURL != null) {
		    try {
		    	Instructions.setPage(InstructionsURL);
		    } catch (IOException e) {
		        System.err.println("Attempted to read a bad URL: " + InstructionsURL);
		    }
		} else {
		    System.err.println("Couldn't find file: resizable_instructions.html");
		}

		//Put the editor pane in a scroll pane.
		JScrollPane editorScrollPane = new JScrollPane(Instructions);
		editorScrollPane.setVerticalScrollBarPolicy(
		                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		editorScrollPane.setPreferredSize(new Dimension(250, 145));
		editorScrollPane.setMinimumSize(new Dimension(10, 10));
				
		//For layout purposes, put things in separate panels
			
		//Create the save button. 
        saveButton = new JButton("Save .html as...");
        saveButton.addActionListener(this);
		
		//save file selection panel
		JPanel savePanel = new JPanel();
		savePanel.add(saveButton);
		
		//Combine previous three panels into one
		JPanel leftpanel = new JPanel();
		leftpanel.setLayout(new BorderLayout());
		leftpanel.add(PathSizePanel, BorderLayout.PAGE_START);
		leftpanel.add(editorScrollPane, BorderLayout.CENTER);
		leftpanel.add(savePanel, BorderLayout.PAGE_END);
		
        //Create file chooser
        fc = new JFileChooser();
		

        //Create the list and list view to handle the list of 
		//Jmol Instances.
		arrayListHandler = new ArrayListTransferHandler();
		DefaultListModel InstanceFilelist = new DefaultListModel();
		InstanceList = new JList(InstanceFilelist);
		InstanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//        InstanceList.setTransferHandler(arrayListHandler);
        InstanceList.setCellRenderer(new InstanceCellRenderer());
        InstanceList.setDragEnabled(true);
        JScrollPane InstanceListView = new JScrollPane(InstanceList);
        InstanceListView.setPreferredSize(new Dimension(300, 200));
        
        //Create the Instance add button.
        AddInstanceButton = new JButton("Add Present Jmol State as Instance...");
        AddInstanceButton.addActionListener(this);
		
		//Create the delete Instance button
		deleteInstanceButton = new JButton("Delete Selected");
		deleteInstanceButton.addActionListener(this);
		
		//Instance selection
		JPanel InstanceButtonsPanel = new JPanel();
		InstanceButtonsPanel.add(AddInstanceButton);
		InstanceButtonsPanel.add(deleteInstanceButton);
		
		//Text info on the name choices for the instance
		JEditorPane NameChoice = new JEditorPane();
		NameChoice.setEditable(false);
		NameChoice.setPreferredSize(new Dimension(300,1));
		NameChoice.setText("The names you choose will be used as the button lables");

		
		
		//Title and border for the Instance selection
		JPanel InstancePanel = new JPanel();
		InstancePanel.setLayout(new BorderLayout());
		InstancePanel.add(InstanceButtonsPanel, BorderLayout.PAGE_START);
		InstancePanel.add(NameChoice,BorderLayout.CENTER);
		InstancePanel.add(InstanceListView, BorderLayout.PAGE_END);
    	InstancePanel.setBorder(BorderFactory.createTitledBorder("Jmol Instances (Reorder by deleting):"));

		//Create the overall panel
		JPanel PopInPanel = new JPanel();
		PopInPanel.setLayout(new BorderLayout());
		
        //Add everything to this panel.
        PopInPanel.add(Description, BorderLayout.PAGE_START);
        PopInPanel.add(leftpanel, BorderLayout.CENTER);
		PopInPanel.add(InstancePanel, BorderLayout.LINE_END);
		
		return (PopInPanel);
	}
	
	public void actionPerformed(ActionEvent e) {
		
        //Handle open button action.
        if (e.getSource() == AddInstanceButton) {
			//make dialog to get name for instance
			//create an instance with this name.  Each instance is just a container for a string with the Jmol state
			//which contains the full information on the file that is loaded and manipulations done.
        	String name = JOptionPane.showInputDialog("Give the button a name:");
        	if (name != null){
        		//need to get the script...
           		String script = viewer.getStateInfo();
        		if (script == null){
                    LogPanel.Log("Error trying to get Jmol State within pop_in_Jmol.");
                }
        		DefaultListModel listModel = (DefaultListModel)InstanceList.getModel();
        		Image pict = viewer.getScreenImage();
        		if (pict == null){
        			LogPanel.Log("Error trying to add image to instance within pop_in_Jmol.");
        		}
        		String StructureFile = null;
				StructureFile = viewer.getFullPathName();
				if (StructureFile==null){
					LogPanel.Log("Error trying to get name and path to file containing structure in pop_in_Jmol.");
				}
        		JmolInstance Instance = null;
        		Instance = new JmolInstance(viewer, name, StructureFile, script, 
                ((SpinnerNumberModel)appletSizeSpinnerW.getModel()).getNumber().intValue(),
                ((SpinnerNumberModel)appletSizeSpinnerH.getModel()).getNumber().intValue());
        		if (Instance == null){
        			LogPanel.Log("Error creating new instance containing script and image in pop_in_Jmol.");
        		}
        		listModel.addElement(Instance);
        		LogPanel.Log("Successfully added Instance "+Instance.name+" to pop_in_Jmol list.");
//        		JmolInstance check = new JmolInstance("","");
//        		check = (JmolInstance)(listModel.getElementAt(listModel.getSize()-1));
//        		LogPanel.Log("Script is:\n"+check.InstanceScript);
			}
        	else{
        		LogPanel.Log("Add instance cancelled by user.");
        	}
        }
                 
			//Handle Delete button
		if (e.getSource() == deleteInstanceButton){
			DefaultListModel listModel = (DefaultListModel)InstanceList.getModel();
			//find out which are selected and remove them.
			int[] todelete = InstanceList.getSelectedIndices();
			for (int i = 0; i<todelete.length; i++) {
				listModel.remove(todelete[i]);
        		LogPanel.Log("Successfully removed Instances from pop_in_Jmol list.");
			}
			//Handle save button action.
        } else if (e.getSource() == saveButton) {
			fc.setDialogTitle("Save file as (please do not use an extension):");
            int returnVal = fc.showSaveDialog(Resizable_User_Activated_Scripts.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
				boolean retVal = true;
				try {
					retVal = FileWriter(file, InstanceList, appletPath.getText());
				}catch (IOException IOe) {
					LogPanel.Log (IOe.getMessage());
				}
				if (!retVal) {
					LogPanel.Log("Call to FileWriter unsuccessful.");
				}
            } else {
                LogPanel.Log("Save command cancelled by user.");
            }
       }
    }	

	
public boolean FileWriter(File file, JList InstanceList, String appletPath) 
		throws IOException { //returns true if successful.
//          JOptionPane.showMessageDialog(null, "Creating directory for data...");
            String datadirPath = file.getPath();
            String datadirName=file.getName();
            boolean made_datadir = file.mkdir();
			DefaultListModel listModel = (DefaultListModel)InstanceList.getModel();
			if (made_datadir){
				LogPanel.Log("Created directory: "+datadirPath);
				PrintStream out = null;
				BufferedReader in = null;
				for (int i = 0; i < listModel.getSize(); i++) {
					JmolInstance thisInstance = null;
					thisInstance = (JmolInstance)(listModel.getElementAt(i));
					String buttonname = thisInstance.name;
					String name = TextFormat.simpleReplace(buttonname, " ", "_");
					String script = thisInstance.script;
					LogPanel.Log("Writing Data for "+buttonname+".");
					LogPanel.Log("  Copying image file from scratch...");
					try {
					  thisInstance.movepict(datadirPath);
					} catch (IOException IOe){
						throw IOe;
					}
					LogPanel.Log("  Copying the structure data file...");
					out = null;
					//Get the path to the file from the Jmol
					String StructureFile = thisInstance.file;
					String extension = StructureFile.substring(StructureFile.lastIndexOf(".")+1, StructureFile.length());
					//need to open the file and copy it...
					try {
						String outfilename = datadirPath+"/"+name+"."+extension;
						out = new PrintStream(new FileOutputStream(outfilename));
					} catch (FileNotFoundException IOe){
						throw IOe;
					}
					in = null;
					try{
						in = new BufferedReader(new FileReader(StructureFile));
					} catch (IOException IOe){
						throw IOe;
					} 
					try{
						String str = null;
						while((str = in.readLine()) != null){
							out.println(str);
						}
						out.close();
						in.close();
					}catch (IOException IOe){
						throw IOe;
					}
//					JOptionPane.showMessageDialog(null, "Writing Script file...");
					LogPanel.Log("  Writing script for this instance...");
					//First modify to use the newly copied structure file
					String newstructurefile = name+"."+extension; //assuming things are relative to calling page.
					script = TextFormat.simpleReplace(script, StructureFile, newstructurefile);
					script = TextFormat.simpleReplace(script, "set refreshing false;", "set refreshing true;");
					script = TextFormat.simpleReplace(script, "moveto /* time, axisAngle */ 0.0", "moveto /* time, axisAngle */ 5.0");
					out = null;
					try {
						String scriptname = datadirPath+"/"+name+".scpt";
						out = new PrintStream(new FileOutputStream(scriptname));
					} catch (FileNotFoundException IOe){
						throw IOe;
					}
					out.print(script);
					out.close();
				}
				//open the printstream for outfile
				LogPanel.Log("Writing .html file for this web page...");
				out = null;
				try {
					String outfilename = datadirPath+"/"+datadirName+".html";
					out = new PrintStream(new FileOutputStream(outfilename));
				} catch (FileNotFoundException e){
					throw e; //Pass the error up the line so it can go in the log window.
				}
				//html output.
//				Below is the code to reproduce the file that was read in.
//				This is the code that should be modified to allow the user of your
//				program to adjust what is displayed.  The Panel() routine is a
//				good place to put the user interface for adjusting the parameters.
		        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		        out.println("<html>");
		        out.println("<head>");
		        out.println("  <meta http-equiv=\"Content-Type\"");
		        out.println(" content=\"text/html; charset=iso-8859-1\">");
		        out.println("  <meta name=\"Author\" content=\"Angel Herr&aacute;ez\">");
		        out.println("<!-- Template for building web pages with Jmol");
		        out.println("");
		        out.println("	by Angel Herráez; version 2007.04.24 (based on v.0.7a)");
		        out.println("");
		        out.println("	");
		        out.println("");
		        out.println("	Available from  http://biomodel.uah.es/Jmol/  and  http://wiki.jmol.org/");
		        out.println("");
		        out.println("");
		        out.println("");
		        out.println("	Use according to Creative Commons \"Attribution-ShareAlike\" License, ");
		        out.println("");
		        out.println("	http://creativecommons.org/licenses/by-sa/3.0/");
		        out.println("");
		        out.println("-->");
		        out.println("  <title>Dynamically resized Jmol</title>");
		        out.println("  <style type=\"text/css\">");
		        out.println("/* These are important, don't change: */");
		        out.println("html, body { height:100%; overflow:hidden; margin:0; padding:0; }");
		        out.println(".JmolPanels { position:absolute; overflow:hidden; }");
		        out.println(".textPanels { position:absolute; overflow:auto; }");
		        out.println("/* Don't add margin nor padding to textPane; if needed, use an inner div with desired style (like 'contents' below) */");
		        out.println("");
		        out.println("/* These are aesthetic, can be customized: */");
		        out.println(".content { padding:0.5em 1ex; }");
		        out.println(".textPanels, .JmolPanels { background-color:rgb(102, 255, 255); }");
		        out.println("  </style>");
		        out.println("  <script type=\"text/javascript\">");
		        out.println("//  	USER'S SETTINGS: ");
		        out.println("	var side = \"right\"	// sets the side of the page that the model appears on, you can set this to \"left\" or \"right\"");
        		SpinnerNumberModel sizeModel = (SpinnerNumberModel)(appletSizeSpinnerW.getModel());
        		int size = sizeModel.getNumber().intValue();
        		out.println("	var w = "+size+"			// you can set this to any integer, meaning percent of window width assigned to Jmol");
        		out.println("	var JmolPath = \""+appletPath+"/\"	// adjust according to your location of the Jmol applet files");
		        out.println("							// (Jmol.js, JmolApplet0.jar and the others) ");
		        out.println("							// If you place them in the same directory as this file, use \"./\"");
		        out.println("//      --------------");
		        out.println("");
		        out.println("document.writeln('<script src=\"' + JmolPath + 'Jmol.js\" type=\"text/javascript\"><' + '/script>')");
		        out.println("var cssTx = '<style type=\"text/css\">'");
		        out.println("	cssTx += '#JmolPane { left:' + ( (side==\"left\") ? \"0px\" : ((100-w)+\"%\") ) + '; width:' + w + '%; top:0px; height:100%; } '	");
		        out.println("	cssTx += '#mainPane { left:' + ( (side==\"left\") ? (w+\"%\") : \"0px\" ) + '; width:' + (100-w) + '%; top:0px; height:100%;} '");
		        out.println("	cssTx += '</style>'");
		        out.println("document.writeln(cssTx)");
		        out.println("  </script>");
		        out.println("</head>");
		        out.println("<body style=\"color: rgb(0, 0, 0); background-color: rgb(102, 255, 255);\"");
		        out.println(" alink=\"#000099\" link=\"#000099\" vlink=\"#990099\">");
		        out.println("<div id=\"JmolPane\" class=\"JmolPanels\">");
		        out.println("<script type=\"text/javascript\">");
		        out.println("	jmolInitialize(JmolPath)");
		        out.println("	//jmolSetAppletColor(\"#CCFFCC\")	//Only set if you don't like the default");
				JmolInstance thisInstance = null;
				thisInstance = (JmolInstance)(listModel.getElementAt(0));
				String buttonname = thisInstance.name;
				String name = TextFormat.simpleReplace(buttonname, " ", "_");
				String scriptname = name+".scpt";
		        out.println("	jmolApplet(\"100%\", \"script "+scriptname+"\")		// DO NOT change 100% ");
		        out.println("		// of course, you should change the script to load your model or run your script");
		        out.println("</script></div>");
		        out.println("<div id=\"mainPane\" class=\"textPanels\">");
		        out.println("<div class=\"content\">&lt;Insert your TITLE and INTRODUCTION here.&gt;<br>");
		        out.println("<table style=\"text-align: center; width: 100%\" border=\"1\" cellpadding=\"2\"");
				out.println(" cellspacing=\"2\">");
				out.println("  <tbody>");
				out.println("    <tr>");
				out.println("      <td>");
				out.println("      <script type=\"text/javascript\">");
				out.println("		jmolButton('Script "+scriptname+"', 'Restore Initial View'); ");
				out.println("	</script>");
				out.println("      </td>");
				out.println("    </tr>");
				out.println("  </tbody>");
				out.println("</table>");
		        out.println("&lt;Insert your description of the initial view, which will appear in the space at right. &gt;<br>");
		        for (int i = 1; i < listModel.getSize(); i++) {
					thisInstance = (JmolInstance)(listModel.getElementAt(i));
					buttonname = thisInstance.name;
					name = TextFormat.simpleReplace(buttonname, " ", "_");
					scriptname = name+".scpt";
					out.println("&lt;The button to switch to the view "+buttonname+" will appear in the box below.");
					out.println("  Replace this text with appropriate information. &gt;<br>");
					out.println("<table style=\"text-align: center; width: 100%\" border=\"1\" cellpadding=\"2\"");
					out.println(" cellspacing=\"2\">");
					out.println("  <tbody>");
					out.println("    <tr>");
					out.println("      <td>");
					out.println("      <script type=\"text/javascript\">");
					out.println("		jmolButton('Script "+scriptname+"', '"+ buttonname +"'); ");
					out.println("	</script>");
					out.println("      </td>");
					out.println("    </tr>");
					out.println("  </tbody>");
					out.println("</table>");	
					}
				out.println("<div style=\"text-align: right;\">Based on template by A. Herraez as modified by J. Gutow</div>");
				out.println("<div style=\"text-align: right;\">"+WebExport.TimeStamp_WebLink()+"</div>");
		        out.println("</div>");
		        out.println("<!--content--></div>");
		        out.println("<!--mainPane-->");
				out.println("</body>");
				out.println("</html>");
				out.close();
			} else {
				IOException IOe = new IOException("Error creating directory: "+datadirPath);
				throw IOe;
			}
			return true;
}

}
