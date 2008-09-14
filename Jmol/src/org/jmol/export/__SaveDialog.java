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
package org.jmol.export;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;

import org.jmol.api.JmolSaveDialogInterface;
import org.jmol.api.JmolViewer;
import org.jmol.i18n.GT;
import org.jmol.viewer.FileManager;

public class __SaveDialog extends JPanel implements JmolSaveDialogInterface {

  String[] extensions = new String[10];
  String choice;
  String extension;
  private int defaultChoice;
  private JSlider qSliderJPEG, qSliderPNG;
  private JComboBox cb;

  JPanel qPanelJPEG, qPanelPNG;
  JFileChooser exportChooser;
  
  public __SaveDialog() {
  }

  public String getSaveFileNameFromDialog(JFileChooser chooser,
                                          JmolViewer viewer, String fileName,
                                          String type) {
    chooser.setCurrentDirectory(FileManager.getLocalDirectory(viewer));
    File file = null;
    chooser.resetChoosableFileFilters();
    if (fileName != null) {
      int pt = fileName.lastIndexOf(".");
      String sType = fileName.substring(pt + 1);
      if (pt >= 0 && sType.length() > 0)
        chooser.addChoosableFileFilter(new TypeFilter(sType));
      file = new File(fileName);
    }
    if (type != null)
      chooser.addChoosableFileFilter(new TypeFilter(type));
    if ((file = showSaveDialog(this, viewer.getAwtComponent(), chooser, file)) == null)
      return null;
    viewer.setStringProperty("currentLocalPath", file.getParent());
    return file.getAbsolutePath();
  }
  
  public String getImageFileNameFromDialog(JFileChooser chooser,
                                           JmolViewer viewer, String fileName,
                                           String type, String[] imageChoices,
                                           String[] imageExtensions,
                                           int qualityJPG, int qualityPNG) {
    exportChooser = chooser;
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

    initialFile = file;
    if ((file = showSaveDialog(this, viewer.getAwtComponent(), exportChooser,
        file)) == null)
      return null;
    viewer.setStringProperty("currentLocalPath", file.getParent());
    return file.getAbsolutePath();
  }

  
  File initialFile;

  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#createPanel(javax.swing.JFileChooser, java.lang.String[], java.lang.String[], int)
   */
  public void createPanel(JFileChooser fc, String[] choices, 
                          String[] extensions, String type, int qualityJPEG, int qualityPNG) {
    exportChooser = fc;
    fc.setAccessory(this);
    setLayout(new BorderLayout());
    choice = null;
    if (type.equals("JPG"))
      type = "JPEG";
    for (defaultChoice = choices.length; --defaultChoice >= 1; )
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
    qPanelJPEG.setBorder(new TitledBorder(GT._("JPEG Quality ({0})", qualityJPEG)));
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
    qPanelPNG.setBorder(new TitledBorder(GT._("PNG Quality ({0})", qualityPNG)));
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
        qPanelJPEG.setBorder(new TitledBorder(GT._("JPEG Quality ({0})", value)));
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
    return (sType.equals("JPEG") || sType.equals("JPG") ? 
        qSliderJPEG.getValue() : sType.equals("PNG") ? qSliderPNG.getValue() : -1);
  }

  private static boolean doOverWrite(JFileChooser chooser, File file) {
    Object[] options = { GT._("Yes"), GT._("No") };
    int opt = JOptionPane.showOptionDialog(chooser, GT._("Do you want to overwrite file {0}?", file
        .getAbsolutePath()), GT._("Warning"), JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE, null, options, options[0]);
    return (opt == 0);
  }
  
  private static File showSaveDialog(__SaveDialog it, Component c,
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
      return (pt >=0 && ext.substring(pt + 1).toLowerCase().equals(thisType));    
    }

    public String getDescription() {
      return thisType.toUpperCase() + " (*." + thisType + ")";
    }
    
  }
}
