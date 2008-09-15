/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-03-05 17:47:26 -0600 (Wed, 05 Mar 2008) $
 * $Revision: 9055 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.export.dialog;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolDialogInterface;
import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.util.Escape;
import org.jmol.util.TextFormat;
import org.jmol.viewer.FileManager;

public class Dialog extends JPanel implements JmolDialogInterface {

  String[] extensions = new String[10];
  String choice;
  String extension;
  private int defaultChoice;
  private JSlider qSliderJPEG, qSliderPNG;
  private JComboBox cb;

  JPanel qPanelJPEG, qPanelPNG;
  static JFileChooser exportChooser;
  static JFileChooser saveChooser;

  public Dialog() {
  }

  private static FileChooser openChooser;
  private FilePreview openPreview;

  public String getOpenFileNameFromDialog(JmolAdapter modelAdapter,
                                          String appletContext,
                                          JmolViewer viewer, String fileName,
                                          Object historyFileObject,
                                          String windowName, boolean allowAppend) {

    HistoryFile historyFile = (HistoryFile) historyFileObject;

    if (openChooser == null) {
      openChooser = new FileChooser();
      String previewProperty = System.getProperty("openFilePreview", "true");
      if (Boolean.valueOf(previewProperty).booleanValue()) {
        openPreview = new FilePreview(openChooser, modelAdapter, allowAppend,
            appletContext);
      }
    }

    if (historyFile != null) {
      openChooser.setDialogSize(historyFile.getWindowSize(windowName));
      openChooser.setDialogLocation(historyFile.getWindowPosition(windowName));
    }

    openChooser.resetChoosableFileFilters();

    if (fileName != null) {
      int pt = fileName.lastIndexOf(".");
      String sType = fileName.substring(pt + 1);
      if (pt >= 0 && sType.length() > 0)
        openChooser.addChoosableFileFilter(new TypeFilter(sType));
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      if (fileName.length() > 0)
        openChooser.setSelectedFile(new File(fileName));
      if (fileName.indexOf(":") < 0)
        openChooser.setCurrentDirectory(FileManager.getLocalDirectory(viewer));
    }
    File file = null;
    if (openChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
      file = openChooser.getSelectedFile();
    if (file == null)
      return null;

    if (historyFile != null)
      historyFile.addWindowInfo(windowName, openChooser.getDialog(), null);

    String url = getLocalUrl(file);
    if (url != null) {
      fileName = url;
    } else {
      viewer.setStringProperty("currentLocalPath", file.getParent());
      fileName = file.getAbsolutePath();
    }
    boolean doAppend = (openPreview != null && openPreview.isAppendSelected());
    openPreview = null;
    return (doAppend ? "load append " + Escape.escape(fileName) : fileName);
  }

  private final static String[] urlPrefixes = { "http:", "http://", "www.",
      "http://www.", "https:", "https://", "ftp:", "ftp://", "file:",
      "file:///" };

  static String getLocalUrl(File file) {
    // entering a url on a file input box will be accepted,
    // but cause an error later. We can fix that...
    // return null if there is no problem, the real url if there is
    if (file.getName().startsWith("="))
      return file.getName();
    String path = file.getAbsolutePath().replace('\\', '/');
    for (int i = 0; i < urlPrefixes.length; i++)
      if (path.indexOf(urlPrefixes[i]) == 0)
        return null;
    for (int i = 0; i < urlPrefixes.length; i += 2)
      if (path.indexOf(urlPrefixes[i]) > 0)
        return urlPrefixes[i + 1]
            + TextFormat.trim(path.substring(path.indexOf(urlPrefixes[i])
                + urlPrefixes[i].length()), "/");
    return null;
  }

  public String getSaveFileNameFromDialog(JmolViewer viewer, String fileName,
                                          String type) {
    if (saveChooser == null)
      saveChooser = new JFileChooser();
    saveChooser.setCurrentDirectory(FileManager.getLocalDirectory(viewer));
    File file = null;
    saveChooser.resetChoosableFileFilters();
    if (fileName != null) {
      int pt = fileName.lastIndexOf(".");
      String sType = fileName.substring(pt + 1);
      if (pt >= 0 && sType.length() > 0)
        saveChooser.addChoosableFileFilter(new TypeFilter(sType));
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      file = new File(fileName);
    }
    if (type != null)
      saveChooser.addChoosableFileFilter(new TypeFilter(type));
    saveChooser.setSelectedFile(file);
    if ((file = showSaveDialog(this, this, saveChooser, file)) == null)
      return null;
    viewer.setStringProperty("currentLocalPath", file.getParent());
    return file.getAbsolutePath();
  }

  public String getImageFileNameFromDialog(JmolViewer viewer, String fileName,
                                           String type, String[] imageChoices,
                                           String[] imageExtensions,
                                           int qualityJPG, int qualityPNG) {
    if (exportChooser == null)
      exportChooser = new JFileChooser();
    exportChooser.setCurrentDirectory(FileManager.getLocalDirectory(viewer));
    exportChooser.resetChoosableFileFilters();
    File file = null;
    if (fileName == null) {
      fileName = viewer.getModelSetFileName();
      String pathName = exportChooser.getCurrentDirectory().getPath();
      createPanel(exportChooser, imageChoices, imageExtensions, type,
          qualityJPG, qualityPNG);
      if (fileName != null && pathName != null) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart != -1) {
          fileName = fileName.substring(0, extensionStart) + "."
              + getExtension();
        }
        file = new File(pathName, fileName);
      }
    } else {
      if (fileName.indexOf(".") == 0)
        fileName = "Jmol" + fileName;
      String sType = fileName.substring(fileName.lastIndexOf(".") + 1);
      for (int i = 0; i < imageExtensions.length; i++)
        if (sType.equals(imageChoices[i])
            || sType.toLowerCase().equals(imageExtensions[i])) {
          sType = imageChoices[i];
          break;
        }
      createPanel(exportChooser, imageChoices, imageExtensions, sType,
          qualityJPG, qualityPNG);
      file = new File(fileName);
    }
    exportChooser.setSelectedFile(initialFile = file);
    if ((file = showSaveDialog(this, this, exportChooser, file)) == null)
      return null;
    viewer.setStringProperty("currentLocalPath", file.getParent());
    return file.getAbsolutePath();
  }

  File initialFile;

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#createPanel(javax.swing.JFileChooser, java.lang.String[], java.lang.String[], int)
   */
  public void createPanel(JFileChooser fc, String[] choices,
                          String[] extensions, String type, int qualityJPEG,
                          int qualityPNG) {
    exportChooser = fc;
    fc.setAccessory(this);
    setLayout(new BorderLayout());
    choice = null;
    if (type.equals("JPG"))
      type = "JPEG";
    for (defaultChoice = choices.length; --defaultChoice >= 1;)
      if (choices[defaultChoice].equals(type))
        break;
    extension = extensions[defaultChoice];
    this.extensions = extensions;
    exportChooser.resetChoosableFileFilters();
    exportChooser.addChoosableFileFilter(new TypeFilter(extension));
    JPanel cbPanel = new JPanel();
    cbPanel.setLayout(new FlowLayout());
    cbPanel.setBorder(new TitledBorder(GT._("Image Type")));
    cb = new JComboBox();
    for (int i = 0; i < choices.length; i++) {
      cb.addItem(choices[i]);
    }
    cbPanel.add(cb);
    cb.setSelectedIndex(defaultChoice);
    cb.addItemListener(new ChoiceListener());
    add(cbPanel, BorderLayout.NORTH);

    JPanel qPanel2 = new JPanel();
    qPanel2.setLayout(new BorderLayout());

    if (qualityJPEG < 0)
      qualityJPEG = 75;
    if (qualityPNG < 0)
      qualityPNG = 2;
    if (qualityPNG > 9)
      qualityPNG = 9;

    qPanelJPEG = new JPanel();
    qPanelJPEG.setLayout(new BorderLayout());
    qPanelJPEG.setBorder(new TitledBorder(GT._("JPEG Quality ({0})",
        qualityJPEG)));
    qSliderJPEG = new JSlider(SwingConstants.HORIZONTAL, 50, 100, qualityJPEG);
    qSliderJPEG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderJPEG.setPaintTicks(true);
    qSliderJPEG.setMajorTickSpacing(10);
    qSliderJPEG.setPaintLabels(true);
    qSliderJPEG.addChangeListener(new QualityListener(true, qSliderJPEG));
    qPanelJPEG.add(qSliderJPEG, BorderLayout.SOUTH);
    qPanel2.add(qPanelJPEG, BorderLayout.NORTH);

    qPanelPNG = new JPanel();
    qPanelPNG.setLayout(new BorderLayout());
    qPanelPNG
        .setBorder(new TitledBorder(GT._("PNG Quality ({0})", qualityPNG)));
    qSliderPNG = new JSlider(SwingConstants.HORIZONTAL, 0, 9, qualityPNG);
    qSliderPNG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderPNG.setPaintTicks(true);
    qSliderPNG.setMajorTickSpacing(2);
    qSliderPNG.setPaintLabels(true);
    qSliderPNG.addChangeListener(new QualityListener(false, qSliderPNG));
    qPanelPNG.add(qSliderPNG, BorderLayout.SOUTH);
    qPanel2.add(qPanelPNG, BorderLayout.SOUTH);
    add(qPanel2, BorderLayout.SOUTH);
  }

  public class QualityListener implements ChangeListener {

    private boolean isJPEG;
    private JSlider slider;

    public QualityListener(boolean isJPEG, JSlider slider) {
      this.isJPEG = isJPEG;
      this.slider = slider;
    }

    public void stateChanged(ChangeEvent arg0) {
      int value = slider.getValue();
      if (isJPEG) {
        qPanelJPEG
            .setBorder(new TitledBorder(GT._("JPEG Quality ({0})", value)));
      } else {
        qPanelPNG.setBorder(new TitledBorder(GT._("PNG Quality ({0})", value)));
      }
    }

  }

  public class ChoiceListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {

      JComboBox source = (JComboBox) e.getSource();
      choice = (String) source.getSelectedItem();
      String ext = extensions[source.getSelectedIndex()];
      File selectedFile = exportChooser.getSelectedFile();
      if (selectedFile == null)
        selectedFile = initialFile;
      File newFile = null;
      String name;
      if ((name = selectedFile.getName()) != null
          && name.endsWith("." + extension)) {
        name = name.substring(0, name.length() - extension.length());
        name += ext;
        newFile = new File(selectedFile.getPath(), name);
      }
      exportChooser.resetChoosableFileFilters();
      exportChooser.addChoosableFileFilter(new TypeFilter(ext));
      if (newFile != null)
        exportChooser.setSelectedFile(newFile);
      extension = extensions[source.getSelectedIndex()];
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#memorizeDefaultType()
   */
  public void memorizeDefaultType() {
    if ((cb != null) && (cb.getSelectedIndex() >= 0)) {
      defaultChoice = cb.getSelectedIndex();
    }
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#getType()
   */
  public String getType() {
    return choice;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#getExtension()
   */
  public String getExtension() {
    return extension;
  }

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#getQuality(java.lang.String)
   */
  public int getQuality(String sType) {
    return (sType.equals("JPEG") || sType.equals("JPG") ? qSliderJPEG
        .getValue() : sType.equals("PNG") ? qSliderPNG.getValue() : -1);
  }

  private static boolean doOverWrite(JFileChooser chooser, File file) {
    Object[] options = { GT._("Yes"), GT._("No") };
    int opt = JOptionPane.showOptionDialog(chooser, GT._(
        "Do you want to overwrite file {0}?", file.getAbsolutePath()), GT
        ._("Warning"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
        null, options, options[0]);
    return (opt == 0);
  }

  private static File showSaveDialog(Dialog it, Component c,
                                     JFileChooser chooser, File file) {
    while (true) {
      if (chooser.showSaveDialog(c) != JFileChooser.APPROVE_OPTION)
        return null;
      if (it != null)
        it.memorizeDefaultType();
      if ((file = chooser.getSelectedFile()) == null || !file.exists()
          || doOverWrite(chooser, file))
        return file;
    }
  }

  public class TypeFilter extends FileFilter {

    String thisType;

    TypeFilter(String type) {
      thisType = type.toLowerCase();
    }

    public boolean accept(File f) {
      if (f.isDirectory() || thisType == null) {
        return true;
      }
      String ext = f.getName();
      int pt = ext.lastIndexOf(".");
      return (pt >= 0 && ext.substring(pt + 1).toLowerCase().equals(thisType));
    }

    public String getDescription() {
      return thisType.toUpperCase() + " (*." + thisType + ")";
    }

  }

  static boolean haveTranslations = false;

  public void setupUI(boolean forceNewTranslation) {
    if (forceNewTranslation || !haveTranslations)
      setupUIManager();
    haveTranslations = true;
  }

  /**
   * Setup the UIManager (for i18n) 
   */

  public static void setupUIManager() {

    // FileChooser strings
    UIManager.put("FileChooser.acceptAllFileFilterText", GT._("All Files"));
    UIManager.put("FileChooser.cancelButtonText", GT._("Cancel"));
    UIManager.put("FileChooser.cancelButtonToolTipText", GT
        ._("Abort file chooser dialog"));
    UIManager.put("FileChooser.detailsViewButtonAccessibleName", GT
        ._("Details"));
    UIManager.put("FileChooser.detailsViewButtonToolTipText", GT._("Details"));
    UIManager.put("FileChooser.directoryDescriptionText", GT._("Directory"));
    UIManager.put("FileChooser.directoryOpenButtonText", GT._("Open"));
    UIManager.put("FileChooser.directoryOpenButtonToolTipText", GT
        ._("Open selected directory"));
    UIManager.put("FileChooser.fileAttrHeaderText", GT._("Attributes"));
    UIManager.put("FileChooser.fileDateHeaderText", GT._("Modified"));
    UIManager.put("FileChooser.fileDescriptionText", GT._("Generic File"));
    UIManager.put("FileChooser.fileNameHeaderText", GT._("Name"));
    UIManager.put("FileChooser.fileNameLabelText", GT._("File or URL:"));
    UIManager.put("FileChooser.fileSizeHeaderText", GT._("Size"));
    UIManager.put("FileChooser.filesOfTypeLabelText", GT._("Files of Type:"));
    UIManager.put("FileChooser.fileTypeHeaderText", GT._("Type"));
    UIManager.put("FileChooser.helpButtonText", GT._("Help"));
    UIManager
        .put("FileChooser.helpButtonToolTipText", GT._("FileChooser help"));
    UIManager.put("FileChooser.homeFolderAccessibleName", GT._("Home"));
    UIManager.put("FileChooser.homeFolderToolTipText", GT._("Home"));
    UIManager.put("FileChooser.listViewButtonAccessibleName", GT._("List"));
    UIManager.put("FileChooser.listViewButtonToolTipText", GT._("List"));
    UIManager.put("FileChooser.lookInLabelText", GT._("Look In:"));
    UIManager.put("FileChooser.newFolderErrorText", GT
        ._("Error creating new folder"));
    UIManager.put("FileChooser.newFolderAccessibleName", GT._("New Folder"));
    UIManager
        .put("FileChooser.newFolderToolTipText", GT._("Create New Folder"));
    UIManager.put("FileChooser.openButtonText", GT._("Open"));
    UIManager.put("FileChooser.openButtonToolTipText", GT
        ._("Open selected file"));
    UIManager.put("FileChooser.openDialogTitleText", GT._("Open"));
    UIManager.put("FileChooser.saveButtonText", GT._("Save"));
    UIManager.put("FileChooser.saveButtonToolTipText", GT
        ._("Save selected file"));
    UIManager.put("FileChooser.saveDialogTitleText", GT._("Save"));
    UIManager.put("FileChooser.saveInLabelText", GT._("Save In:"));
    UIManager.put("FileChooser.updateButtonText", GT._("Update"));
    UIManager.put("FileChooser.updateButtonToolTipText", GT
        ._("Update directory listing"));
    UIManager.put("FileChooser.upFolderAccessibleName", GT._("Up"));
    UIManager.put("FileChooser.upFolderToolTipText", GT._("Up One Level"));

    // OptionPane strings
    UIManager.put("OptionPane.cancelButtonText", GT._("Cancel"));
    UIManager.put("OptionPane.noButtonText", GT._("No"));
    UIManager.put("OptionPane.okButtonText", GT._("OK"));
    UIManager.put("OptionPane.yesButtonText", GT._("Yes"));
  }
}
