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

import org.jmol.api.JmolViewer;
import org.jmol.util.TextFormat;

import java.net.*;
import java.io.IOException;

abstract class WebPanel extends JPanel implements ActionListener {

  //The constants used to generate panels, etc.
  JButton saveButton, addInstanceButton, deleteInstanceButton;
  JTextField appletPath;
  JSpinner appletSizeSpinnerW;
  JSpinner appletSizeSpinnerH;
  ArrayListTransferHandler arrayListHandler;
  JFileChooser fc;
  JList instanceList, scriptList;
  JmolViewer viewer;
  
  String templateName;
  String description;
  String infoFile;

  WebPanel(JmolViewer viewer) {
    this.viewer = viewer;
    //Create the text field for the path to the Jmol applet
    appletPath = new JTextField(20);
    appletPath.addActionListener(this);
    appletPath.setText(WebExport.getAppletPath());
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
  protected JPanel getPanel(JComponent centerPanel, JPanel appletSizePanel) {

    //Create the brief description text
    JLabel jDescription = new JLabel(description);

    //For layout purposes, put things in separate panels

    //Create the save button. 
    saveButton = new JButton("Save .html as...");
    saveButton.addActionListener(this);

    //save file selection panel
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);

    //Create file chooser
    fc = new JFileChooser();

    //Create the list and list view to handle the list of 
    //Jmol Instances.
    arrayListHandler = new ArrayListTransferHandler();
    DefaultListModel InstanceFilelist = new DefaultListModel();
    instanceList = new JList(InstanceFilelist);
    instanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    //        InstanceList.setTransferHandler(arrayListHandler);
    instanceList.setCellRenderer(new InstanceCellRenderer());
    instanceList.setDragEnabled(true);
    JScrollPane instanceListView = new JScrollPane(instanceList);
    instanceListView.setPreferredSize(new Dimension(300, 200));

    //Create the Instance add button.
    addInstanceButton = new JButton("Add Present Jmol State as Instance...");
    addInstanceButton.addActionListener(this);

    //Create the delete Instance button
    deleteInstanceButton = new JButton("Delete Selected");
    deleteInstanceButton.addActionListener(this);

    //Instance selection
    JPanel instanceButtonsPanel = new JPanel();
    instanceButtonsPanel.add(addInstanceButton);
    instanceButtonsPanel.add(deleteInstanceButton);

    //Title and border for the Instance selection
    JPanel instancePanel = new JPanel();
    instancePanel.setLayout(new BorderLayout());
    instancePanel.add(instanceButtonsPanel, BorderLayout.PAGE_START);
    instancePanel.add(centerPanel, BorderLayout.CENTER);
    instancePanel.add(instanceListView, BorderLayout.PAGE_END);
    instancePanel.setBorder(BorderFactory
        .createTitledBorder("Jmol Instances (Reorder by deleting):"));

    //Create the overall panel
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JPanel leftpanel = getLeftPanel(appletSizePanel);

    //Add everything to this panel.
    panel.add(jDescription, BorderLayout.PAGE_START);
    panel.add(leftpanel, BorderLayout.CENTER);
    panel.add(instancePanel, BorderLayout.LINE_END);

    return panel;
  }

  private JPanel getLeftPanel(JPanel sizePanel) {

    //Path to applet panel
    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    //    JLabel pathLabel = new JLabel("Relative Path to Jmol Applet:");
    //    pathPanel.add(pathLabel, BorderLayout.PAGE_START);
    pathPanel.add(appletPath, BorderLayout.PAGE_END);
    pathPanel.setBorder(BorderFactory
        .createTitledBorder("Relative Path to Jmol Applet:"));

    JScrollPane editorScrollPane = getInstructionPane();

    //For layout purposes, put things in separate panels

    //For layout combine path and size into one panel
    JPanel pathSizePanel = null;

    if (sizePanel == null) {
      pathSizePanel = pathPanel;
    } else {
      pathSizePanel = new JPanel();
      pathSizePanel.setLayout(new BorderLayout());
      pathSizePanel.add(pathPanel, BorderLayout.PAGE_START);
      pathSizePanel.add(sizePanel, BorderLayout.CENTER);
    }

    //Create the save button. 
    saveButton = new JButton("Save .html as...");
    saveButton.addActionListener(this);

    //save file selection panel
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);

    //Combine previous three panels into one
    JPanel leftpanel = new JPanel();
    leftpanel.setLayout(new BorderLayout());
    leftpanel.add(pathSizePanel, BorderLayout.PAGE_START);
    leftpanel.add(editorScrollPane, BorderLayout.CENTER);
    leftpanel.add(savePanel, BorderLayout.PAGE_END);

    return leftpanel;
  }

  private JScrollPane getInstructionPane() {

    //Create the instructions sub window (scrolling to read html file)
    JEditorPane instructions = new JEditorPane();
    instructions.setEditable(false);
    URL InstructionsURL = this.getClass().getResource(infoFile);
    if (InstructionsURL != null) {
      try {
        instructions.setPage(InstructionsURL);
      } catch (IOException e) {
        System.err.println("Attempted to read a bad URL: " + InstructionsURL);
      }
    } else {
      System.err.println("Couldn't find file: " + infoFile);
    }

    //Put the editor pane in a scroll pane.
    JScrollPane editorScrollPane = new JScrollPane(instructions);
    editorScrollPane
        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    editorScrollPane.setPreferredSize(new Dimension(250, 145));
    editorScrollPane.setMinimumSize(new Dimension(10, 10));
    return editorScrollPane;
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
        int width = sizeModelW.getNumber().intValue();
        SpinnerNumberModel sizeModelH = (appletSizeSpinnerH == null ? null : (SpinnerNumberModel) (appletSizeSpinnerH
            .getModel()));
        int height = (sizeModelH == null ? width : sizeModelH.getNumber().intValue());
        String StructureFile = viewer.getModelSetPathName();
        if (StructureFile == null) {
          LogPanel
              .Log("Error trying to get name and path to file containing structure in pop_in_Jmol.");
        }
        JmolInstance Instance = new JmolInstance(viewer, name, StructureFile,
            script, width, height);
        if (Instance == null) {
          LogPanel
              .Log("Error creating new instance containing script and image in pop_in_Jmol.");
        }
        listModel.addElement(Instance);
        LogPanel.Log("Successfully added Instance " + Instance.name
            + " to pop_in_Jmol list.");
        //            JmolInstance check = new JmolInstance("","");
        //            check = (JmolInstance)(listModel.getElementAt(listModel.getSize()-1));
        //            LogPanel.Log("Script is:\n"+check.InstanceScript);
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
      int returnVal = fc.showSaveDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        boolean retVal = true;
        try {
          String path = appletPath.getText();
          WebExport.setAppletPath(path);
          retVal = FileWriter(file, instanceList, path);
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

  boolean FileWriter(File file, JList InstanceList, String appletPath)
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
        //          JOptionPane.showMessageDialog(null, "Writing Script file...");
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
        script = fixScript(script);
        try {
          String scriptname = datadirPath + "/" + name + ".scpt";
          out = new PrintStream(new FileOutputStream(scriptname));
          out.print(script);
          out.close();
        } catch (FileNotFoundException IOe) {
          throw IOe;
        }
      }
      URL templateFile = this.getClass().getResource(templateName);
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
      html = fixHtml(html);
      StringBuffer appletDefs = new StringBuffer();
      for (int i = 0; i < listModel.getSize(); i++)
        html = getAppletDefs(i, html, appletDefs, (JmolInstance) listModel.getElementAt(i));
      html = TextFormat.simpleReplace(html, "@APPLETPATH@", appletPath);
      html = TextFormat.simpleReplace(html, "@DATADIRNAME@", datadirName);
      html = TextFormat.simpleReplace(html, "@APPLETDEFS@", appletDefs
          .toString());
      html = TextFormat.simpleReplace(html, "@CREATIONDATA@", WebExport
          .TimeStamp_WebLink());
      LogPanel.Log("Writing .html file for this web page at " + datadirPath
          + "/" + fileName);
      viewer.createImage(datadirPath + "/" + fileName, html, Integer.MIN_VALUE,
          0, 0);
    } else {
      IOException IOe = new IOException("Error creating directory: "
          + datadirPath);
      throw IOe;
    }
    return true;
  }

  abstract String getAppletDefs(int i, String html, StringBuffer appletDefs, JmolInstance instance);

  String fixScript(String script) {
    return script;    
  }
  
  String fixHtml(String html) {
    return html;
  }

}
