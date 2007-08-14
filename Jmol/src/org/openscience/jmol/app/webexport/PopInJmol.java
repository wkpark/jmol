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
import org.jmol.api.JmolViewer;
import java.net.*;
import java.io.IOException;

public class PopInJmol extends JPanel implements ActionListener {

  //The constants used to generate panels, etc.
  JButton saveButton, addInstanceButton, deleteInstanceButton;
  JTextField appletPath;
  JEditorPane Instructions;
  JSpinner appletSizeSpinnerW;
  JSpinner appletSizeSpinnerH;
  ArrayListTransferHandler arrayListHandler;
  JFileChooser fc;
  JList instanceList, scriptList;
  JmolViewer viewer;

  PopInJmol(JmolViewer viewer) {
    this.viewer = viewer;
  }

  class InstanceCellRenderer extends JLabel implements ListCellRenderer {
    public Component getListCellRendererComponent(JList list, // the list
                                                  Object value, // value to display
                                                  int index, // cell index
                                                  boolean isSelected, // is the cell selected
                                                  boolean cellHasFocus) // does the cell have focus
    {
      String s = ((JmolInstance) value).name;
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
  public JComponent Panel() {

    //Create the brief description text
    JLabel Description = new JLabel(
        "Create a web page with images that convert to live Jmol on user click...");

    //Create the text field for the path to the JMol applet
    appletPath = new JTextField(20);
    appletPath.addActionListener(this);
    appletPath.setText("../../Applets/Java/Jmol");

    //Path to applet panel
    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    //		JLabel pathLabel = new JLabel("Relative Path to Jmol Applet:");
    //		pathPanel.add(pathLabel, BorderLayout.PAGE_START);
    pathPanel.add(appletPath, BorderLayout.PAGE_END);
    pathPanel.setBorder(BorderFactory
        .createTitledBorder("Relative Path to Jmol Applet:"));

    //Create the instructions sub window (scrolling to read html file)
    Instructions = new JEditorPane();
    Instructions.setEditable(false);
    URL InstructionsURL = this.getClass().getResource(
        "pop_in_instructions.html");
    if (InstructionsURL != null) {
      try {
        Instructions.setPage(InstructionsURL);
      } catch (IOException e) {
        System.err.println("Attempted to read a bad URL: " + InstructionsURL);
      }
    } else {
      System.err.println("Couldn't find file: pop_in_instructions.html");
    }

    //Put the editor pane in a scroll pane.
    JScrollPane editorScrollPane = new JScrollPane(Instructions);
    editorScrollPane
        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
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
    leftpanel.add(pathPanel, BorderLayout.PAGE_START);
    leftpanel.add(editorScrollPane, BorderLayout.CENTER);
    leftpanel.add(savePanel, BorderLayout.PAGE_END);

    //Create file chooser
    fc = new JFileChooser();

    //Create the appletSize spinner so the user can decide how big
    //the applet should be.
    SpinnerNumberModel appletSizeModelW = new SpinnerNumberModel(300, //initial value
        50, //min
        500, //max
        25); //step size
    SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(300, //initial value
        50, //min
        500, //max
        25); //step size
    appletSizeSpinnerW = new JSpinner(appletSizeModelW);
    appletSizeSpinnerH = new JSpinner(appletSizeModelH);
    //panel to hold spinner and label
    JPanel appletSizePanel = new JPanel();
    appletSizePanel.add(new JLabel("Applet width:"));
    appletSizePanel.add(appletSizeSpinnerW);
    appletSizePanel.add(new JLabel("height:"));
    appletSizePanel.add(appletSizeSpinnerH);

    //Create the list and list view to handle the list of 
    //Jmol Instances.
    arrayListHandler = new ArrayListTransferHandler();
    DefaultListModel InstanceFilelist = new DefaultListModel();
    instanceList = new JList(InstanceFilelist);
    instanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    //        InstanceList.setTransferHandler(arrayListHandler);
    instanceList.setCellRenderer(new InstanceCellRenderer());
    instanceList.setDragEnabled(true);
    JScrollPane InstanceListView = new JScrollPane(instanceList);
    InstanceListView.setPreferredSize(new Dimension(300, 200));

    //Create the Instance add button.
    addInstanceButton = new JButton("Add Present Jmol State as Instance...");
    addInstanceButton.addActionListener(this);

    //Create the delete Instance button
    deleteInstanceButton = new JButton("Delete Selected");
    deleteInstanceButton.addActionListener(this);

    //Instance selection
    JPanel InstanceButtonsPanel = new JPanel();
    InstanceButtonsPanel.add(addInstanceButton);
    InstanceButtonsPanel.add(deleteInstanceButton);

    //Title and border for the Instance selection
    JPanel InstancePanel = new JPanel();
    InstancePanel.setLayout(new BorderLayout());
    InstancePanel.add(InstanceButtonsPanel, BorderLayout.PAGE_START);
    InstancePanel.add(appletSizePanel, BorderLayout.CENTER);
    InstancePanel.add(InstanceListView, BorderLayout.PAGE_END);
    InstancePanel.setBorder(BorderFactory
        .createTitledBorder("Jmol Instances (Reorder by deleting):"));

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
    if (e.getSource() == addInstanceButton) {
      //make dialog to get name for instance
      //create an instance with this name.  Each instance is just a container for a string with the Jmol state
      //which contains the full information on the file that is loaded and manipulations done.
      String name = JOptionPane
          .showInputDialog("Give the occurance of Jmol a one word name:");
      if (name != null) {
        //need to get the script...
        String script = viewer.getStateInfo();
        if (script == null) {
          LogPanel.Log("Error trying to get Jmol State within pop_in_Jmol.");
        }
        DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
        SpinnerNumberModel sizeModelW = (SpinnerNumberModel) (appletSizeSpinnerW
            .getModel());
        SpinnerNumberModel sizeModelH = (SpinnerNumberModel) (appletSizeSpinnerH
            .getModel());
        int width = sizeModelW.getNumber().intValue();
        int height = sizeModelH.getNumber().intValue();
        String StructureFile = viewer.getModelSetPathName();
        if (StructureFile == null) {
          LogPanel
              .Log("Error trying to get name and path to file containing structure in pop_in_Jmol.");
        }
        JmolInstance Instance = new JmolInstance(viewer, name, StructureFile, script, width, height);
        if (Instance == null) {
          LogPanel
              .Log("Error creating new instance containing script and image in pop_in_Jmol.");
        }
        listModel.addElement(Instance);
        LogPanel.Log("Successfully added Instance " + Instance.name
            + " to pop_in_Jmol list.");
        //        		JmolInstance check = new JmolInstance("","");
        //        		check = (JmolInstance)(listModel.getElementAt(listModel.getSize()-1));
        //        		LogPanel.Log("Script is:\n"+check.InstanceScript);
      } else {
        LogPanel.Log("Add instance cancelled by user.");
      }
    }

    //Handle Delete button
    if (e.getSource() == deleteInstanceButton) {
      DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
      //find out which are selected and remove them.
      int[] todelete = instanceList.getSelectedIndices();
      for (int i = 0; i < todelete.length; i++) {
        listModel.remove(todelete[i]);
        LogPanel.Log("Successfully removed Instances from pop_in_Jmol list.");
      }
      //Handle save button action.
    } else if (e.getSource() == saveButton) {
      fc.setDialogTitle("Save file as (please do not use an extension):");
      int returnVal = fc.showSaveDialog(PopInJmol.this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        boolean retVal = true;
        try {
          retVal = FileWriter(file, instanceList, appletPath.getText());
        } catch (IOException IOe) {
          LogPanel.Log(IOe.getMessage());
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
    String datadirName = file.getName();
    String fileName = null;
    if (datadirName.indexOf(".htm") > 0) {
      fileName = datadirName;
      datadirPath = file.getParent();
      file = new File(datadirPath);
      datadirName = file.getName();
    } else {
      fileName = datadirName + ".html";
    }
    boolean made_datadir = (file.exists() && file.isDirectory() || file.mkdir());
    DefaultListModel listModel = (DefaultListModel) InstanceList.getModel();
    if (made_datadir) {
      LogPanel.Log("Created directory: " + datadirPath);
      LogPanel.Log("Writing javascript file pop_Jmol.js...");
      URL pop_JmolURL = this.getClass().getResource("pop_Jmol.js");
      PrintStream out = null;
      try {
        String outfilename = datadirPath + "/pop_Jmol.js";
        out = new PrintStream(new FileOutputStream(outfilename));
      } catch (FileNotFoundException IOe) {
        throw IOe;
      }
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(pop_JmolURL.getPath()));
      } catch (IOException IOe) {
        throw IOe;
      }
      try {
        String str = null;
        while ((str = in.readLine()) != null) {
          out.println(str);
        }
        out.close();
        in.close();
      } catch (IOException IOe) {
        throw IOe;
      }
      for (int i = 0; i < listModel.getSize(); i++) {
        JmolInstance thisInstance = (JmolInstance) (listModel.getElementAt(i));
        String name = thisInstance.name;
        String script = thisInstance.script;
        LogPanel.Log("Writing Data for " + name + ".");
        LogPanel.Log("  Copying image file from scratch...");
        try {
          thisInstance.movepict(datadirPath);
        } catch (IOException IOe) {
          throw IOe;
        }
        LogPanel.Log("  Copying the structure data file...");
        out = null;
        //Get the path to the file from the Jmol
        String structureFile = thisInstance.file;
        String extension = structureFile.substring(structureFile
            .lastIndexOf(".") + 1, structureFile.length());
        String outfilename = datadirPath + "/" + name + "." + extension;

        String data = viewer.getFileAsString(structureFile);
        viewer.createImage(outfilename, data, Integer.MIN_VALUE, 0, 0);
        //					JOptionPane.showMessageDialog(null, "Writing Script file...");
        LogPanel.Log("  Writing script for this instance...");
        //First modify to use the newly copied structure file
        String newstructurefile = name + "." + extension; //assuming things are relative to calling page.
        String structureFileName = (new File(structureFile)).getName();
        int pt = script.indexOf("/" + structureFileName + "\"");
        if (pt > 0) {
          int pt0 = pt;
          while (pt0 >= 0 && script.charAt(pt0) != '"')
            pt0--;
          pt0++;
          structureFileName = script.substring(pt0, pt + 1) + structureFileName;
        }
        script = TextFormat.simpleReplace(script,
            '"' + structureFileName + '"', '"' + newstructurefile + '"');
        try {
          String scriptname = datadirPath + "/" + name + ".scpt";
          out = new PrintStream(new FileOutputStream(scriptname));
          out.print(script);
          out.close();
        } catch (FileNotFoundException IOe) {
          throw IOe;
        }
      }
      URL templateFile = this.getClass().getResource("pop_in_template.html");
      String filename = templateFile.getFile();
      BufferedReader br = new BufferedReader(new FileReader(filename));
      StringBuffer htmlBuf = new StringBuffer();
      String line;
      while ((line = br.readLine()) != null)
        htmlBuf.append(line).append("\n");
      br.close();
      String html = htmlBuf.toString();
      LogPanel.Log("Loading html template " + templateFile + "("
          + html.length() + " bytes)");
      if (html == null)
        throw new FileNotFoundException("Error loading pop_in_template.html");
      StringBuffer appletDefs = new StringBuffer();
      for (int i = 0; i < listModel.getSize(); i++) {
        String name = ((JmolInstance) (listModel.getElementAt(i))).name;
        int JmolSizeW = ((JmolInstance) (listModel.getElementAt(i))).width;
        int JmolSizeH = ((JmolInstance) (listModel.getElementAt(i))).height;
        String floatDiv = (i % 2 == 0 ? "floatRightDiv" : "floatLeftDiv");
        appletDefs.append("\naddJmolDiv(" + i + ",'"+floatDiv+"','" + name + "',"
            + JmolSizeW + "," + JmolSizeH + ",'insert caption here','insert note here')");
      }
      html = TextFormat.simpleReplace(html, "@APPLETPATH@", appletPath);
      html = TextFormat.simpleReplace(html, "@DATADIRNAME@", datadirName);
      html = TextFormat.simpleReplace(html, "@APPLETDEFS@", appletDefs
          .toString());
      html = TextFormat.simpleReplace(html, "@CREATIONDATA@",
          WebExport.TimeStamp_WebLink());
      LogPanel.Log("Writing .html file for this web page at " + datadirPath
          + "/" + fileName);
      viewer.createImage(datadirPath + "/" + fileName, html, Integer.MIN_VALUE, 0, 0);
    } else {
      IOException IOe = new IOException("Error creating directory: "
          + datadirPath);
      throw IOe;
    }
    return true;
  }

}
