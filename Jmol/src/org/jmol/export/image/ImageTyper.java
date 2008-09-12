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
package org.jmol.export.image;

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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;

import org.jmol.api.JmolImageTyperInterface;
import org.jmol.i18n.GT;

public class ImageTyper extends JPanel implements JmolImageTyperInterface {

  String[] extensions = new String[10];
  String choice;
  String extension;
  int defaultChoice;
  JSlider qSliderJPEG, qSliderPNG;
  JPanel qPanelJPEG, qPanelPNG;
  private JComboBox cb;
  JFileChooser fileChooser;

  public ImageTyper() {
  }
  
  /* (non-Javadoc)
   * @see org.jmol.export.JmolImageTyperInterface#createPanel(javax.swing.JFileChooser, java.lang.String[], java.lang.String[], int)
   */
  public void createPanel(JFileChooser fc, String[] choices, String[] extensions, String type) {
    fileChooser = fc;
    fc.setAccessory(this);
    setLayout(new BorderLayout());
    choice = null;
    for (defaultChoice = choices.length; --defaultChoice >= 1; )
      if (choices[defaultChoice].equals(type))
        break;
    extension = extensions[defaultChoice];
    this.extensions = extensions;
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
    
    qPanelJPEG = new JPanel();
    qPanelJPEG.setLayout(new BorderLayout());
    qPanelJPEG.setBorder(new TitledBorder(GT._("JPEG Quality ({0})", 75)));
    qSliderJPEG = new JSlider(SwingConstants.HORIZONTAL, 50, 100, 75);
    qSliderJPEG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderJPEG.setPaintTicks(true);
    qSliderJPEG.setMajorTickSpacing(10);
    qSliderJPEG.setPaintLabels(true);
    qSliderJPEG.addChangeListener(new QualityListener(true, qSliderJPEG));
    qPanelJPEG.add(qSliderJPEG, BorderLayout.SOUTH);
    qPanel2.add(qPanelJPEG, BorderLayout.NORTH);
    
    qPanelPNG = new JPanel();
    qPanelPNG.setLayout(new BorderLayout());
    qPanelPNG.setBorder(new TitledBorder(GT._("PNG Quality ({0})", 2)));
    qSliderPNG = new JSlider(SwingConstants.HORIZONTAL, 0, 9, 2);
    qSliderPNG.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSliderPNG.setPaintTicks(true);
    qSliderPNG.setMajorTickSpacing(2);
    qSliderPNG.setPaintLabels(true);
    qSliderPNG.addChangeListener(new QualityListener(false, qSliderPNG));
    qPanelPNG.add(qSliderPNG, BorderLayout.SOUTH);
    qPanel2.add(qPanelPNG, BorderLayout.SOUTH);
    add(qPanel2, BorderLayout.SOUTH);
    setEnables();
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
        setEnables();
        File selectedFile = fileChooser.getSelectedFile();
        if ((selectedFile != null) &&
            (selectedFile.getName() != null) &&
            (selectedFile.getName().endsWith("." + extension))) {
          String name = selectedFile.getName();
          name = name.substring(0, name.length() - extension.length());
          name += extensions[source.getSelectedIndex()];
          File newFile = new File(selectedFile.getPath(), name);
          fileChooser.setSelectedFile(newFile);
        }
        extension = extensions[source.getSelectedIndex()];
      }
  }

  protected void setEnables() {
    if (choice == null)
      return;
    qSliderJPEG.setEnabled(choice.equals("JPEG"));
    qSliderPNG.setEnabled(choice.equals("PNG"));
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
    return (sType.equals("JPEG") ? qSliderJPEG.getValue() : sType.equals("PNG") ? qSliderPNG.getValue() : -1);
  }

  private boolean doOverWrite(File file) {
    Object[] options = { GT._("Yes"), GT._("No") };
    int opt = JOptionPane.showOptionDialog(fileChooser, GT._("Do you want to overwrite file {0}?", file
        .getAbsolutePath()), GT._("Warning"), JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE, null, options, options[1]);
    return (opt == 0);
  }
  
  public File showDialog(Component c, File file) {
    while (true) {
      if (fileChooser.showSaveDialog(c) != JFileChooser.APPROVE_OPTION)
        return null;
      memorizeDefaultType();
      file = fileChooser.getSelectedFile();
      if (file == null)
        return null;
      if (!file.exists() || doOverWrite(file))
        return file;
    }
  }
}
