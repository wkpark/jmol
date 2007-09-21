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

import java.awt.*;
import java.io.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.*;

import javax.swing.*;

import org.jmol.api.JmolViewer;
import org.jmol.util.TextFormat;

abstract class WebPanel extends JPanel implements ActionListener {

  abstract String getAppletDefs(int i, String html, StringBuffer appletDefs,
                                JmolInstance instance);

  abstract String fixHtml(String html);

  abstract JPanel appletParamPanel(); //should be defined in the code for the specific case e.g. ScriptButtons.java

  protected String templateName;
  protected String listLabel;
  protected String infoFile;
  protected String appletInfoDivs;
  protected String htmlAppletTemplate;
  protected String appletTemplateName;
  protected boolean useAppletJS;

  protected JSpinner appletSizeSpinnerW;
  protected JSpinner appletSizeSpinnerH;
  protected JSpinner appletSizeSpinnerP;

  private JScrollPane editorScrollPane;
  private JButton saveButton, addInstanceButton;
  private JButton deleteInstanceButton, showInstanceButton;
  private JTextField appletPath, pageAuthorName, webPageTitle;
  private JFileChooser fc;
  private JList instanceList;
  private JmolViewer viewer;
  private int panelIndex;
  private WebPanel[] webPanels;

  protected WebPanel(JmolViewer viewer, JFileChooser fc, WebPanel[] webPanels,
      int panelIndex) {
    this.viewer = viewer;
    this.fc = fc;
    this.webPanels = webPanels;
    this.panelIndex = panelIndex;
    //Create the text fields for the path to the Jmol applet, page author(s) name(s) and  web page title.
    appletPath = new JTextField(20);
    appletPath.addActionListener(this);
    appletPath.setText(WebExport.getAppletPath());
    pageAuthorName= new JTextField(20);
    pageAuthorName.addActionListener(this);
    pageAuthorName.setText(WebExport.getPageAuthorName());
    webPageTitle = new JTextField(20);
    webPageTitle.addActionListener(this);
    webPageTitle.setText("A web page containing Jmol applets");
  }

  //Need the panel maker and the action listener.

  JPanel getPanel(int infoWidth, int infoHeight) {

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
    instanceList.setPreferredSize(new Dimension(350, 200));

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

    JPanel buttonPanel = new JPanel();
    buttonPanel.setMaximumSize(new Dimension(350, 50));
    showInstanceButton = new JButton("Show Selected");
    showInstanceButton.addActionListener(this);
    deleteInstanceButton = new JButton("Delete Selected");
    deleteInstanceButton.addActionListener(this);
    buttonPanel.add(showInstanceButton);
    buttonPanel.add(deleteInstanceButton);

    // width height or %width

    JPanel paramPanel = appletParamPanel();
    paramPanel.setMaximumSize(new Dimension(350, 70));

    //Instance selection
    JPanel instanceButtonPanel = new JPanel();
    instanceButtonPanel.add(addInstanceButton);
    instanceButtonPanel.setSize(300, 70);

    JPanel p = new JPanel();
    p.setLayout(new BorderLayout());
    p.add(instanceButtonPanel, BorderLayout.NORTH);
    p.add(buttonPanel, BorderLayout.SOUTH);

    JPanel instancePanel = new JPanel();
    instancePanel.setLayout(new BorderLayout());
    instancePanel.add(instanceSet, BorderLayout.CENTER);
    instancePanel.add(p, BorderLayout.SOUTH);

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    rightPanel.setMinimumSize(new Dimension(350, 350));
    rightPanel.setMaximumSize(new Dimension(350, 1000));
    rightPanel.add(paramPanel, BorderLayout.NORTH);
    rightPanel.add(instancePanel, BorderLayout.CENTER);
    rightPanel.setBorder(BorderFactory.createTitledBorder("Jmol Instances:"));

    //Create the overall panel
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    JPanel leftPanel = getLeftPanel(infoWidth, infoHeight);
    leftPanel.setMaximumSize(new Dimension(350, 1000));

    //Add everything to this panel.
    panel.add(leftPanel, BorderLayout.CENTER);
    panel.add(rightPanel, BorderLayout.EAST);

    enableButtons(instanceList);
    return panel;
  }

  int getInfoWidth() {
    return editorScrollPane.getWidth();
  }

  int getInfoHeight() {
    return editorScrollPane.getHeight();
  }

  private URL getResource(String fileName) {
    URL url = this.getClass().getResource(fileName);
    if (url == null) {
      System.err.println("Couldn't find file: " + infoFile);
    }
    return url;
  }

  private String getResourceString(String name) throws IOException {
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

  private JPanel getLeftPanel(int w, int h) {

    editorScrollPane = getInstructionPane(w, h);

    //Create the save button. 
    saveButton = new JButton("Save HTML as...");
    saveButton.addActionListener(this);
    JPanel savePanel = new JPanel();
    savePanel.add(saveButton);
    
    //Path to applet panel

    JPanel pathPanel = new JPanel();
    pathPanel.setLayout(new BorderLayout());
    pathPanel.setBorder(BorderFactory
        .createTitledBorder("Relative Path to Jmol Applet:"));
    pathPanel.add(appletPath, BorderLayout.NORTH);
   
    //Page Author Panel
    JPanel authorPanel = new JPanel();
    authorPanel.setBorder(BorderFactory
        .createTitledBorder("Author (your name):"));
    authorPanel.add(pageAuthorName, BorderLayout.NORTH);
    
    //Page Title Panel
    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BorderLayout());
    titlePanel.setBorder(BorderFactory
        .createTitledBorder("Browser window title for this web page:"));
    titlePanel.add(webPageTitle, BorderLayout.NORTH);
    titlePanel.add(savePanel, BorderLayout.SOUTH);
    
    JPanel settingsPanel = new JPanel();
    settingsPanel.setLayout(new BorderLayout());
    settingsPanel.add(pathPanel, BorderLayout.NORTH);
    settingsPanel.add(authorPanel, BorderLayout.CENTER);
    settingsPanel.add(titlePanel, BorderLayout.SOUTH);

    //Combine previous three panels into one
    JPanel leftpanel = new JPanel();
    leftpanel.setLayout(new BorderLayout());
    leftpanel.add(editorScrollPane, BorderLayout.CENTER);
    leftpanel.add(settingsPanel, BorderLayout.SOUTH);
    return leftpanel;
  }

  private JScrollPane getInstructionPane(int w, int h) {

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
    editorScrollPane.setPreferredSize(new Dimension(w, h));
    editorScrollPane.setMinimumSize(new Dimension(250, 10));
    return editorScrollPane;
  }

  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == appletPath) {
      String path = appletPath.getText();
      WebExport.setAppletPath(path);
      return;
    }

    //Handle open button action.
    if (e.getSource() == addInstanceButton) {
      //make dialog to get name for instance
      //create an instance with this name.  Each instance is just a container for a string with the Jmol state
      //which contains the full information on the file that is loaded and manipulations done.
      String label = (instanceList.getSelectedIndices().length != 1 ? ""
          : getInstanceName(-1));
      String name = JOptionPane.showInputDialog(
          "Give the occurance of Jmol a name:", label);
      if (name == null)
        return;
      //need to get the script...
      String script = viewer.getStateInfo();
      if (script == null) {
        LogPanel.log("Error trying to get Jmol State within pop_in_Jmol.");
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
            .log("Error trying to get name and path to file containing structure in pop_in_Jmol.");
      }
      JmolInstance instance = new JmolInstance(viewer, name, StructureFile,
          script, width, height);
      if (instance == null) {
        LogPanel
            .log("Error creating new instance containing script and image in pop_in_Jmol.");
      }

      int i;
      for (i = instanceList.getModel().getSize(); --i >= 0;)
        if (getInstanceName(i).equals(instance.name))
          break;
      if (i < 0) {
        i = listModel.getSize();
        listModel.addElement(instance);
        LogPanel.log("added Instance " + instance.name);
      } else {
        listModel.setElementAt(instance, i);
        LogPanel.log("updated Instance " + instance.name);
      }
      instanceList.setSelectedIndex(i);
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
      viewer.evalStringQuiet(")" + instance.script); //leading paren disabled history
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
        String authorName = pageAuthorName.getText();
        WebExport.setWebPageAuthor(authorName);
        retVal = fileWriter(file, instanceList);
      } catch (IOException IOe) {
        LogPanel.log(IOe.getMessage());
      }
      if (!retVal) {
        LogPanel.log("Call to FileWriter unsuccessful.");
      }
    }
  }

  String getInstanceName(int i) {
    if (i < 0)
      i = instanceList.getSelectedIndex();
    JmolInstance instance = (JmolInstance) instanceList.getModel()
        .getElementAt(i);
    return (instance == null ? "" : instance.name);
  }

  boolean fileWriter(File file, JList InstanceList)
      throws IOException { //returns true if successful.
    useAppletJS = JmolViewer.checkOption(viewer, "webMakerCreateJS");
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
    LogPanel.log("");
    if (made_datadir) {
      LogPanel.log("Using directory " + datadirPath);
      LogPanel.log("  adding JmolPopIn.js");
      viewer.createImage(datadirPath + "/JmolPopIn.js", getResourceString("JmolPopIn.html"), 
          Integer.MIN_VALUE, 0, 0);
      String lastFile = "";
      String lastName = "";
      String outfilename = "";
      for (int i = 0; i < listModel.getSize(); i++) {
        JmolInstance thisInstance = (JmolInstance) (listModel.getElementAt(i));
        String javaname = thisInstance.javaname;
        String script = thisInstance.script;
        LogPanel.log("  ...jmolApplet" + i);
        LogPanel.log("      ...adding " + javaname + ".png");
        try {
          thisInstance.movepict(datadirPath);
        } catch (IOException IOe) {
          throw IOe;
        }
        //Get the path to the file from the Jmol

        String structureFile = thisInstance.file;
        long structureFileSize = (new File(structureFile).length());
        String extension = structureFile.substring(structureFile
            .lastIndexOf(".") + 1, structureFile.length());
        String newName = lastName;
        if (!structureFile.equals(lastFile)) {
          newName = javaname + "." + extension; //assuming things are relative to calling page.
          outfilename = datadirPath
              + (datadirPath.indexOf("\\") >= 0 ? "\\" : "/") + newName;
          LogPanel
              .log("      ...copying " + newName + " from " + structureFile);
          String data = viewer.getFileAsString(structureFile);
          if (structureFileSize > 32000){ //gzip it!
            outfilename  = outfilename+".gz";
            newName = newName+".gz";
            GZIPOutputStream gzFile = new GZIPOutputStream(new FileOutputStream(outfilename));
            gzFile.write(data.getBytes());
            gzFile.close();
            LogPanel
              .log("          ...compressing large file "+structureFile);
          } else {
            viewer.createImage(outfilename, data, Integer.MIN_VALUE, 0, 0);
          }
          lastFile = structureFile;
          lastName = newName;
        } else {
          LogPanel.log("      ..." + javaname + " uses " + outfilename);
        }
        //First modify to use the newly copied structure file
        String structureFileName = (new File(structureFile)).getName();
        int pt = script.indexOf("/" + structureFileName + "\"");
        if (pt < 0)
          pt = script.indexOf("\\" + structureFileName + "\"");
        if (pt >= 0) {
          int pt0 = pt;
          while (pt0 >= 0 && script.charAt(pt0) != '"')
            pt0--;
          pt0++;
          structureFileName = script.substring(pt0, pt + 1) + structureFileName;
        }
        script = TextFormat.simpleReplace(script,
            '"' + structureFileName + '"', '"' + newName + '"');
        //script = fixScript(script);
        LogPanel.log("      ...adding " + javaname + ".spt");
        String scriptname = datadirPath + "/" + javaname + ".spt";
        viewer.createImage(scriptname, script, Integer.MIN_VALUE, 0, 0);
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
      html = TextFormat.simpleReplace(html, "@AUTHOR@", pageAuthorName.getText());
      html = TextFormat.simpleReplace(html, "@TITLE@", webPageTitle.getText());
      html = TextFormat.simpleReplace(html, "@APPLETPATH@", appletPath.getText());
      html = TextFormat.simpleReplace(html, "@DATADIRNAME@", datadirName);
      if (appletInfoDivs.length() > 0)
        appletInfoDivs = "\n<div style='display:none'>\n" + appletInfoDivs
            + "\n</div>\n";
      String str = appletDefs.toString();
      if (useAppletJS)
        str = "<script type='text/javascript'>\n" + str + "\n</script>";
      html = TextFormat.simpleReplace(html, "@APPLETINFO@", appletInfoDivs);
      html = TextFormat.simpleReplace(html, "@APPLETDEFS@", str);
      html = TextFormat.simpleReplace(html, "@CREATIONDATA@", WebExport
          .TimeStamp_WebLink());
      html = TextFormat.simpleReplace(html, "@AUTHORDATA@",
          "Based on template by A. Herr&aacute;ez as modified by J. Gutow");
      html = TextFormat.simpleReplace(html, "@LOGDATA@", "<pre>\n"
          + LogPanel.getText() + "\n</pre>\n");
      LogPanel.log("      ...creating " + fileName);
      viewer.createImage(datadirPath + "/" + fileName, html, Integer.MIN_VALUE,
          0, 0);
    } else {
      IOException IOe = new IOException("Error creating directory: "
          + datadirPath);
      throw IOe;
    }
    LogPanel.log("");
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

  ArrayListTransferHandler(WebPanel webPanel) {
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

  class ArrayListTransferable implements Transferable {
    ArrayList data;

    ArrayListTransferable(ArrayList alist) {
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
