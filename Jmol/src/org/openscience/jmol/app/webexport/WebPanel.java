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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;

import javax.swing.*;

import org.jmol.api.JmolViewer;
import org.jmol.util.TextFormat;

import java.net.*;
import java.util.ArrayList;
import java.io.IOException;

abstract class WebPanel extends JPanel implements ActionListener {

  //The constants used to generate panels, etc.
  JButton saveButton, addInstanceButton, deleteInstanceButton;
  JTextField appletPath;
  JSpinner appletSizeSpinnerW;
  JSpinner appletSizeSpinnerH;
  JFileChooser fc;
  JList instanceList, scriptList;
  JmolViewer viewer;
  
  String templateName;
  String description;
  String infoFile;
  static String appletInfoDivs;

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
    saveButton = new JButton("Save HTML as...");
    saveButton.addActionListener(this);

    //save file selection panel
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);

    //Create file chooser
    fc = new JFileChooser();

    //Create the list and list view to handle the list of 
    //Jmol Instances.
    instanceList = new JList(new DefaultListModel());
    instanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    instanceList.setTransferHandler(new ArrayListTransferHandler());
    instanceList.setCellRenderer(new InstanceCellRenderer());
    instanceList.setDragEnabled(true);
    JScrollPane instanceListView = new JScrollPane(instanceList);
    instanceListView.setPreferredSize(new Dimension(300, 200));
    JPanel instanceSet = new JPanel();
    instanceSet.setLayout(new BorderLayout());
    instanceSet.add(instanceListView, BorderLayout.NORTH);
    instanceSet.add(new JLabel("double-click and drag to reorder"), BorderLayout.SOUTH);

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
    instancePanel.add(instanceSet, BorderLayout.PAGE_END);
    instancePanel.setBorder(BorderFactory
        .createTitledBorder("Jmol Instances:"));

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
    saveButton = new JButton("Save HTML as...");
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
        name = TextFormat.simpleReplace(name, " ", "_");
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
        JmolInstance instance = new JmolInstance(viewer, name, StructureFile,
            script, width, height);
        if (instance == null) {
          LogPanel
              .Log("Error creating new instance containing script and image in pop_in_Jmol.");
        }
        listModel.addElement(instance);
        LogPanel.Log("added Instance " + instance.name);
      } else {
        LogPanel.Log("Add instance cancelled by user.");
      }
    }

    //Handle Delete button
    if (e.getSource() == deleteInstanceButton) {
      DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
      //find out which are selected and remove them.
      int[] todelete = instanceList.getSelectedIndices();
      int nDeleted = 0;
      for (int i = 0; i < todelete.length; i++)
        listModel.remove(todelete[i] - nDeleted++);
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
      LogPanel.Log("Writing javascript file JmolPopIn.js...");
      URL url = this.getClass().getResource("JmolPopIn.js");
      PrintStream out = null;
      try {
        String outfilename = datadirPath + "/JmolPopIn.js";
        out = new PrintStream(new FileOutputStream(outfilename));
      } catch (FileNotFoundException IOe) {
        throw IOe;
      }
      BufferedReader in = null;
      try {
        in = new BufferedReader(new FileReader(url.getPath()));
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
          String scriptname = datadirPath + "/" + name + ".spt";
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
      appletInfoDivs = "";
      StringBuffer appletDefs = new StringBuffer();
      for (int i = 0; i < listModel.getSize(); i++)
        html = getAppletDefs(i, html, appletDefs, (JmolInstance) listModel.getElementAt(i));
      html = TextFormat.simpleReplace(html, "@APPLETPATH@", appletPath);
      html = TextFormat.simpleReplace(html, "@DATADIRNAME@", datadirName);
      html = TextFormat.simpleReplace(html, "@APPLETINFO@", appletInfoDivs);
      html = TextFormat.simpleReplace(html, "@APPLETDEFS@", appletDefs.toString());
      html = TextFormat.simpleReplace(html, "@CREATIONDATA@", WebExport
          .TimeStamp_WebLink());
      LogPanel.Log("Writing HTML file for this web page at " + datadirPath
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


class ArrayListTransferHandler extends TransferHandler {
  DataFlavor localArrayListFlavor, serialArrayListFlavor;
  String localArrayListType = DataFlavor.javaJVMLocalObjectMimeType +
  ";class=java.util.ArrayList";
  JList source = null;
  int[] sourceIndices = null;
  int addIndex = -1; //Location where items were added
  int addCount = 0;  //Number of items added
  

  public ArrayListTransferHandler() {
      try {
          localArrayListFlavor = new DataFlavor(localArrayListType);
      } catch (ClassNotFoundException e) {
          System.out.println(
               "ArrayListTransferHandler: unable to create data flavor");
      }
      serialArrayListFlavor = new DataFlavor(ArrayList.class,
                       "ArrayList");
  }

  public boolean importData(JComponent c, Transferable t) {
      if (sourceIndices == null || !canImport(c, t.getTransferDataFlavors())) {
          return false;
      }
      JList target = null;
      ArrayList alist = null;
      try {
          target = (JList)c;
          if (hasLocalArrayListFlavor(t.getTransferDataFlavors())) {
              alist = (ArrayList)t.getTransferData(localArrayListFlavor);
          } else if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
              alist = (ArrayList)t.getTransferData(serialArrayListFlavor);
          } else {
              return false;
          }
      } catch (UnsupportedFlavorException ufe) {
          System.out.println("importData: unsupported data flavor");
          return false;
      } catch (IOException ioe) {
          System.out.println("importData: I/O exception");
          return false;
      }
  
      //At this point we use the same code to retrieve the data
      //locally or serially.
  
      //We'll drop at the current selected index.
      int targetIndex = target.getSelectedIndex();
  
      //Prevent the user from dropping data back on itself.
      //For example, if the user is moving items #4,#5,#6 and #7 and
      //attempts to insert the items after item #5, this would
      //be problematic when removing the original items.
      //This is interpreted as dropping the same data on itself
      //and has no effect.
      if (source.equals(target)) {
        //System.out.print("checking indices index TO: " + targetIndex + " FROM:");
        //for (int i = 0; i < sourceIndices.length;i++)
          //System.out.print(" "+sourceIndices[i]);
        //System.out.println("");
          if (targetIndex >= sourceIndices[0] && targetIndex <= sourceIndices[sourceIndices.length - 1]) {
            //System.out.println("setting indices null : " + targetIndex + " " + sourceIndices[0] + " " + sourceIndices[sourceIndices.length - 1]);
              sourceIndices = null;
              return true;
          }
      }
  
      DefaultListModel listModel = (DefaultListModel)target.getModel();
      int max = listModel.getSize();
      if (targetIndex < 0) {
          targetIndex = max; 
      } else {
          if (sourceIndices[0] < targetIndex)
            targetIndex++;
          if (targetIndex > max) {
              targetIndex = max;
          }
      }
      addIndex = targetIndex;
      addCount = alist.size();
      for (int i=0; i < alist.size(); i++) {
          listModel.add(targetIndex++, objectOf(listModel, alist.get(i)));
       }
      return true;
  }

  private Object objectOf(DefaultListModel listModel, Object objectName) {
    if (objectName instanceof String) {
      String name = (String) objectName;
      Object o;
      for (int i = listModel.size(); --i >= 0;) 
        if (!((o=listModel.get(i)) instanceof String) && o.toString().equals(name))
            return listModel.get(i);
    }
    return objectName;
  }
  
  protected void exportDone(JComponent c, Transferable data, int action) {
    //System.out.println("action="+action + " " + addCount + " " + sourceIndices);
      if ((action == MOVE) && (sourceIndices != null)) {
          DefaultListModel model = (DefaultListModel)source.getModel();
    
          //If we are moving items around in the same list, we
          //need to adjust the indices accordingly since those
          //after the insertion point have moved.
          if (addCount > 0) {
              for (int i = 0; i < sourceIndices.length; i++) {
                  if (sourceIndices[i] > addIndex) {
                      sourceIndices[i] += addCount;
                  }
              }
          }
          for (int i = sourceIndices.length -1; i >= 0; i--)
              model.remove(sourceIndices[i]);
          ((JList)c).setSelectedIndices(new int[]{});
      }
      sourceIndices = null;
      addIndex = -1;
      addCount = 0;
  }

  private boolean hasLocalArrayListFlavor(DataFlavor[] flavors) {
      if (localArrayListFlavor == null) {
          return false;
      }
  
      for (int i = 0; i < flavors.length; i++) {
          if (flavors[i].equals(localArrayListFlavor)) {
              return true;
          }
      }
      return false;
  }

  private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
      if (serialArrayListFlavor == null) {
          return false;
      }
  
      for (int i = 0; i < flavors.length; i++) {
          if (flavors[i].equals(serialArrayListFlavor)) {
              return true;
          }
      }
      return false;
  }

  public boolean canImport(JComponent c, DataFlavor[] flavors) {
      if (hasLocalArrayListFlavor(flavors))  { return true; }
      if (hasSerialArrayListFlavor(flavors)) { return true; }
      return false;
  }

  protected Transferable createTransferable(JComponent c) {
      if (c instanceof JList) {
          source = (JList)c;
          sourceIndices = source.getSelectedIndices();
          Object[] values = source.getSelectedValues();
          if (values == null || values.length == 0) {
              return null;
          }
          ArrayList alist = new ArrayList(values.length);
          for (int i = 0; i < values.length; i++) {
              Object o = values[i];
              String str = o.toString();
              if (str == null) str = "";
              alist.add(str);
          }
          return new ArrayListTransferable(alist);
      }
      return null;
  }

  public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
  }

  public class ArrayListTransferable implements Transferable {
      ArrayList data;
  
      public ArrayListTransferable(ArrayList alist) {
          data = alist;
      }
  
      public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return data;
    }
  
      public DataFlavor[] getTransferDataFlavors() {
          return new DataFlavor[] { localArrayListFlavor,
      serialArrayListFlavor };
      }
  
      public boolean isDataFlavorSupported(DataFlavor flavor) {
          if (localArrayListFlavor.equals(flavor)) {
              return true;
          }
          if (serialArrayListFlavor.equals(flavor)) {
              return true;
          }
          return false;
      }
  }
}
