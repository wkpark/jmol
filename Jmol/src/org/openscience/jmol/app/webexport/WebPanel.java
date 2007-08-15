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

  abstract String getAppletDefs(int i, String html, StringBuffer appletDefs,
                                JmolInstance instance);

  abstract String fixHtml(String html);

  //The constants used to generate panels, etc.
  JButton saveButton, addInstanceButton, deleteInstanceButton,
      showInstanceButton;
  JTextField appletPath;
  JSpinner appletSizeSpinnerW;
  JSpinner appletSizeSpinnerH;
  JSpinner appletSizeSpinnerP;
  JFileChooser fc;
  JList instanceList;
  JmolViewer viewer;

  String templateName;
  String description, listLabel;
  String infoFile;
  String appletInfoDivs;
  String htmlAppletTemplate;
  String appletTemplateName;
  boolean useAppletJS;

  int panelIndex;
  WebPanel[] webPanels;

  WebPanel(JmolViewer viewer, JFileChooser fc, WebPanel[] webPanels,
      int panelIndex) {
    this.viewer = viewer;
    this.fc = fc;
    this.webPanels = webPanels;
    this.panelIndex = panelIndex;
    //Create the text field for the path to the Jmol applet
    appletPath = new JTextField(20);
    appletPath.addActionListener(this);
    appletPath.setText(WebExport.getAppletPath());
  }

  //Need the panel maker and the action listener.
  JPanel getPanel() {

    useAppletJS = WebExport.checkOption(viewer, "webMakerCreateJS");

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
    JPanel appletSizeWHPanel = new JPanel();
    appletSizeWHPanel.add(new JLabel("Applet width:"));
    appletSizeWHPanel.add(appletSizeSpinnerW);
    appletSizeWHPanel.add(new JLabel("height:"));
    appletSizeWHPanel.add(appletSizeSpinnerH);

    //Create the appletSize percent spinner so the user can decide what %
    // of the window width the applet should be.
    SpinnerNumberModel appletSizeModel = new SpinnerNumberModel(60, //initial value
        20, //min
        100, //max
        5); //step size
    appletSizeSpinnerP = new JSpinner(appletSizeModel);
    //panel to hold spinner and label

    JPanel appletSizePPanel = new JPanel();
    appletSizePPanel.add(new JLabel("% of window for applet width:"));
    appletSizePPanel.add(appletSizeSpinnerP);
    /*    SpinnerNumberModel appletSizeModelH = new SpinnerNumberModel(60, //initial value
     20, //min
     100, //max
     5); //step size
     appletSizeSpinnerH = new JSpinner(appletSizeModelH);
     appletSizePanel.add(new JLabel("height:"));
     appletSizePanel.add(appletSizeSpinnerH);
     */
    //Create the overall panel

    JPanel appletSizePanel = new JPanel(new BorderLayout());
    appletSizePanel.setMaximumSize(new Dimension(350, 70));
    appletSizePanel.add(appletSizeWHPanel, BorderLayout.NORTH);
    appletSizePanel.add(appletSizePPanel, BorderLayout.SOUTH);

    //Create the brief description text
    JLabel jDescription = new JLabel("      " + description + "\n\n");

    //For layout purposes, put things in separate panels

    //Create the save button. 
    saveButton = new JButton("Save HTML as...");
    saveButton.addActionListener(this);

    //save file selection panel
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);

    //Create the list and list view to handle the list of 
    //Jmol Instances.
    instanceList = new JList(new DefaultListModel());
    instanceList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    instanceList.setTransferHandler(new ArrayListTransferHandler(this));
    instanceList.setCellRenderer(new InstanceCellRenderer());
    instanceList.setDragEnabled(true);
    JScrollPane instanceListView = new JScrollPane(instanceList);
    instanceListView.setPreferredSize(new Dimension(350, 200));
    JPanel instanceSet = new JPanel();
    instanceSet.setLayout(new BorderLayout());
    instanceSet.add(new JLabel(listLabel), BorderLayout.NORTH);
    instanceSet.add(instanceListView, BorderLayout.CENTER);
    instanceSet.add(new JLabel("double-click and drag to reorder"),
        BorderLayout.SOUTH);

    //Create the Instance add button.
    addInstanceButton = new JButton("Add Present Jmol State as Instance...");
    addInstanceButton.addActionListener(this);

    //Instance selection
    JPanel instanceButtonPanel = new JPanel();
    instanceButtonPanel.add(addInstanceButton);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setMaximumSize(new Dimension(350, 50));
    showInstanceButton = new JButton("Show Selected");
    showInstanceButton.addActionListener(this);
    deleteInstanceButton = new JButton("Delete Selected");
    deleteInstanceButton.addActionListener(this);
    buttonPanel.add(showInstanceButton);
    buttonPanel.add(deleteInstanceButton);

    //Title and border for the Instance selection
    JPanel instancePanel = new JPanel();
    instancePanel.setLayout(new BorderLayout());
    instancePanel.add(instanceSet, BorderLayout.PAGE_START);
    instancePanel.add(instanceButtonPanel, BorderLayout.CENTER);
    instancePanel.add(buttonPanel, BorderLayout.PAGE_END);

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    rightPanel.setMinimumSize(new Dimension(350, 350));
    rightPanel.setMaximumSize(new Dimension(350, 1000));
    rightPanel.add(appletSizePanel, BorderLayout.PAGE_START);
    rightPanel.add(instancePanel, BorderLayout.PAGE_END);
    rightPanel.setBorder(BorderFactory.createTitledBorder("Jmol Instances:"));

    //Create the overall panel
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JPanel leftPanel = getLeftPanel(null);
    leftPanel.setMaximumSize(new Dimension(350, 1000));

    //Add everything to this panel.
    panel.add(jDescription, BorderLayout.PAGE_START);
    panel.add(leftPanel, BorderLayout.CENTER);
    panel.add(rightPanel, BorderLayout.LINE_END);

    enableButtons(instanceList);
    return panel;
  }

  URL getResource(String fileName) {
    URL url = this.getClass().getResource(fileName);
    if (url == null) {
      System.err.println("Couldn't find file: " + infoFile);
    }
    return url;
  }

  String getResourceString(String name) throws IOException {
    URL templateFile = this.getClass().getResource(name);
    if (templateFile == null)
      throw new FileNotFoundException("Error loading resource " + name);
    String filename = templateFile.getFile();
    BufferedReader br = new BufferedReader(new FileReader(filename));
    StringBuffer htmlBuf = new StringBuffer();
    String line;
    while ((line = br.readLine()) != null)
      htmlBuf.append(line).append("\n");
    br.close();
    String html = htmlBuf.toString();
    //LogPanel.Log("Loading html template " + templateFile + "("
    //  + html.length() + " bytes)");
    return html;
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
      pathSizePanel.setSize(new Dimension(300, 60));
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
    leftpanel.add(editorScrollPane, BorderLayout.PAGE_START);
    leftpanel.add(pathSizePanel, BorderLayout.CENTER);
    leftpanel.add(savePanel, BorderLayout.PAGE_END);
    return leftpanel;
  }

  private JScrollPane getInstructionPane() {

    //Create the instructions sub window (scrolling to read html file)
    JEditorPane instructions = new JEditorPane();
    instructions.setEditable(false);
    URL InstructionsURL = getResource(infoFile);
    if (InstructionsURL != null) {
      try {
        instructions.setPage(InstructionsURL);
      } catch (IOException e) {
        System.err.println("Attempted to read a bad URL: " + InstructionsURL);
      }
    }

    //Put the editor pane in a scroll pane.
    JScrollPane editorScrollPane = new JScrollPane(instructions);
    editorScrollPane
        .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    editorScrollPane.setPreferredSize(new Dimension(250, 350));
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
      if (name == null)
        return;
      name = TextFormat
          .replaceAllCharacters(name, "[]/\\#*&^%$?.,%<>' \"", "_");
      //need to get the script...
      String script = viewer.getStateInfo();
      if (script == null) {
        LogPanel.Log("Error trying to get Jmol State within pop_in_Jmol.");
      }
      DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
      int width = 300;
      int height = 300;
      if (appletSizeSpinnerH != null) {
        width = ((SpinnerNumberModel) (appletSizeSpinnerW.getModel()))
            .getNumber().intValue();
        height = ((SpinnerNumberModel) (appletSizeSpinnerH.getModel()))
            .getNumber().intValue();
      }
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
      instanceList.setSelectedIndices(new int[] { listModel.getSize() - 1 });
      LogPanel.Log("added Instance " + instance.name);
      syncLists();
      return;
    }

    if (e.getSource() == deleteInstanceButton) {
      DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
      //find out which are selected and remove them.
      int[] todelete = instanceList.getSelectedIndices();
      int nDeleted = 0;
      for (int i = 0; i < todelete.length; i++)
        listModel.remove(todelete[i] - nDeleted++);
      syncLists();
      return;
    }

    if (e.getSource() == showInstanceButton) {
      DefaultListModel listModel = (DefaultListModel) instanceList.getModel();
      //find out which are selected and remove them.
      int[] list = instanceList.getSelectedIndices();
      if (list.length != 1)
        return;
      JmolInstance instance = (JmolInstance) listModel.get(list[0]);
      viewer.evalStringQuiet(instance.script);
      return;
    }

    if (e.getSource() == saveButton) {
      fc.setDialogTitle("Save file as (please do not use an extension):");
      int returnVal = fc.showSaveDialog(this);
      if (returnVal != JFileChooser.APPROVE_OPTION)
        return;
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
    LogPanel.Log("");
    if (made_datadir) {
      LogPanel.Log("Using directory " + datadirPath);
      LogPanel.Log("  adding JmolPopIn.js");
      URL url = getResource("JmolPopIn.js");
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
      String lastFile = "";
      String lastName = "";
      String outfilename = "";
      for (int i = 0; i < listModel.getSize(); i++) {
        JmolInstance thisInstance = (JmolInstance) (listModel.getElementAt(i));
        String name = thisInstance.name;
        String script = thisInstance.script;
        LogPanel.Log("  ...jmolApplet" + i);
        LogPanel.Log("      ...adding " + name + ".png");
        try {
          thisInstance.movepict(datadirPath);
        } catch (IOException IOe) {
          throw IOe;
        }
        out = null;
        //Get the path to the file from the Jmol

        String structureFile = thisInstance.file;
        String extension = structureFile.substring(structureFile
            .lastIndexOf(".") + 1, structureFile.length());
        String newName = lastName;
        if (!structureFile.equals(lastFile)) {
          newName = name + "." + extension; //assuming things are relative to calling page.
          outfilename = datadirPath
              + (datadirPath.indexOf("\\") >= 0 ? "\\" : "/") + newName;
          LogPanel
              .Log("      ...copying " + newName + " from " + structureFile);
          String data = viewer.getFileAsString(structureFile);
          viewer.createImage(outfilename, data, Integer.MIN_VALUE, 0, 0);
          lastFile = structureFile;
          lastName = newName;
        } else {
          LogPanel.Log("      ..." + name + " uses " + outfilename);
        }
        //First modify to use the newly copied structure file
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
            '"' + structureFileName + '"', '"' + newName + '"');
        //script = fixScript(script);
        LogPanel.Log("      ...adding " + name + ".spt");
        try {
          String scriptname = datadirPath + "/" + name + ".spt";
          out = new PrintStream(new FileOutputStream(scriptname));
          out.print(script);
          out.close();
        } catch (FileNotFoundException IOe) {
          throw IOe;
        }
      }
      String html = getResourceString(templateName);
      html = fixHtml(html);
      appletInfoDivs = "";
      StringBuffer appletDefs = new StringBuffer();
      if (!useAppletJS)
        htmlAppletTemplate = getResourceString(appletTemplateName);
      for (int i = 0; i < listModel.getSize(); i++)
        html = getAppletDefs(i, html, appletDefs, (JmolInstance) listModel
            .getElementAt(i));
      html = TextFormat.simpleReplace(html, "@APPLETPATH@", appletPath);
      html = TextFormat.simpleReplace(html, "@DATADIRNAME@", datadirName);
      if (appletInfoDivs.length() > 0)
        appletInfoDivs = "\n<div style='display:none'>\n" + appletInfoDivs
            + "\n</div>\n";
      String str = appletDefs.toString();
      if (htmlAppletTemplate == null)
        str = "<script type='text/javascript'>\n" + str + "\n</script>";
      html = TextFormat.simpleReplace(html, "@APPLETINFO@", appletInfoDivs);
      html = TextFormat.simpleReplace(html, "@APPLETDEFS@", str);
      html = TextFormat.simpleReplace(html, "@CREATIONDATA@", WebExport
          .TimeStamp_WebLink());
      LogPanel.Log("      ...creating " + fileName);
      html = TextFormat.simpleReplace(html, "</body>",
          "<div style='display:none'>\n<pre>\n" + LogPanel.getText()
              + "\n</pre></div>\n</body>");
      viewer.createImage(datadirPath + "/" + fileName, html, Integer.MIN_VALUE,
          0, 0);
    } else {
      IOException IOe = new IOException("Error creating directory: "
          + datadirPath);
      throw IOe;
    }
    LogPanel.Log("");
    return true;
  }

  void syncLists() {
    JList list = webPanels[1 - panelIndex].instanceList;
    DefaultListModel model1 = (DefaultListModel) instanceList.getModel();
    DefaultListModel model2 = (DefaultListModel) list.getModel();
    model2.clear();
    int n = model1.getSize();
    for (int i = 0; i < n; i++)
      model2.addElement(model1.get(i));
    list.setSelectedIndices(new int[] {});
    enableButtons(instanceList);
    webPanels[1 - panelIndex].enableButtons(list);
  }

  void enableButtons(JList list) {
    int nSelected = list.getSelectedIndices().length;
    int nListed = list.getModel().getSize();
    saveButton.setEnabled(nListed > 0);
    deleteInstanceButton.setEnabled(nSelected > 0);
    showInstanceButton.setEnabled(nSelected == 1);
  }

  class InstanceCellRenderer extends JLabel implements ListCellRenderer {

    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
      setText(" " + ((JmolInstance) value).name);
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
      enableButtons(list);
      return this;
    }
  }

}

class ArrayListTransferHandler extends TransferHandler {
  DataFlavor localArrayListFlavor, serialArrayListFlavor;
  String localArrayListType = DataFlavor.javaJVMLocalObjectMimeType
      + ";class=java.util.ArrayList";
  JList source = null;
  int[] sourceIndices = null;
  int addIndex = -1; //Location where items were added
  int addCount = 0; //Number of items added
  WebPanel webPanel;

  public ArrayListTransferHandler(WebPanel webPanel) {
    this.webPanel = webPanel;
    try {
      localArrayListFlavor = new DataFlavor(localArrayListType);
    } catch (ClassNotFoundException e) {
      System.out
          .println("ArrayListTransferHandler: unable to create data flavor");
    }
    serialArrayListFlavor = new DataFlavor(ArrayList.class, "ArrayList");
  }

  public boolean importData(JComponent c, Transferable t) {
    if (sourceIndices == null || !canImport(c, t.getTransferDataFlavors())) {
      return false;
    }
    JList target = null;
    ArrayList alist = null;
    try {
      target = (JList) c;
      if (hasLocalArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (ArrayList) t.getTransferData(localArrayListFlavor);
      } else if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
        alist = (ArrayList) t.getTransferData(serialArrayListFlavor);
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
      if (targetIndex >= sourceIndices[0]
          && targetIndex <= sourceIndices[sourceIndices.length - 1]) {
        //System.out.println("setting indices null : " + targetIndex + " " + sourceIndices[0] + " " + sourceIndices[sourceIndices.length - 1]);
        sourceIndices = null;
        return true;
      }
    }

    DefaultListModel listModel = (DefaultListModel) target.getModel();
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
    for (int i = 0; i < alist.size(); i++) {
      listModel.add(targetIndex++, objectOf(listModel, alist.get(i)));
    }
    return true;
  }

  private Object objectOf(DefaultListModel listModel, Object objectName) {
    if (objectName instanceof String) {
      String name = (String) objectName;
      Object o;
      for (int i = listModel.size(); --i >= 0;)
        if (!((o = listModel.get(i)) instanceof String)
            && o.toString().equals(name))
          return listModel.get(i);
    }
    return objectName;
  }

  protected void exportDone(JComponent c, Transferable data, int action) {
    //System.out.println("action="+action + " " + addCount + " " + sourceIndices);
    if ((action == MOVE) && (sourceIndices != null)) {
      DefaultListModel model = (DefaultListModel) source.getModel();

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
      for (int i = sourceIndices.length - 1; i >= 0; i--)
        model.remove(sourceIndices[i]);
      ((JList) c).setSelectedIndices(new int[] {});
      if (webPanel != null)
        webPanel.syncLists();
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
    if (hasLocalArrayListFlavor(flavors)) {
      return true;
    }
    if (hasSerialArrayListFlavor(flavors)) {
      return true;
    }
    return false;
  }

  protected Transferable createTransferable(JComponent c) {
    if (c instanceof JList) {
      source = (JList) c;
      sourceIndices = source.getSelectedIndices();
      Object[] values = source.getSelectedValues();
      if (values == null || values.length == 0) {
        return null;
      }
      ArrayList alist = new ArrayList(values.length);
      for (int i = 0; i < values.length; i++) {
        Object o = values[i];
        String str = o.toString();
        if (str == null)
          str = "";
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
      return new DataFlavor[] { localArrayListFlavor, serialArrayListFlavor };
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
