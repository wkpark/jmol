
/*
 * Copyright 2001 The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.beans.*;
import java.awt.*;
import java.io.File;

public class FileTyper extends JPanel implements PropertyChangeListener,
    ItemListener {

  private JComboBox fileTypeComboBox;
  private static boolean useFileExtensions = true;

  private String[] choices = {
    JmolResourceHandler.getInstance().getString("FileTyper.XYZ"),
    JmolResourceHandler.getInstance().getString("FileTyper.PDB"),
    JmolResourceHandler.getInstance().getString("FileTyper.CML"),
  };

  // Default is the first one:
  private int defaultTypeIndex = 0;
  private String fileType = choices[defaultTypeIndex];

  /**
   * Whether to use the file extension to set the file type.
   *
   * @param on if true file extensions are used.
   */
  public static void setUseFileExtensions(boolean on) {
    useFileExtensions = on;
  }

  /**
   * Whether file extensions are used to set the file type.
   */
  public static boolean getUseFileExtensions() {
    return useFileExtensions;
  }

  /**
   * A simple panel with a combo box for allowing the user to choose
   * the input file type.
   *
   * @param fileChooser the file chooser
   */
  public FileTyper() {

    setLayout(new BorderLayout());

    JPanel fileTypePanel = new JPanel();
    fileTypePanel.setLayout(new FlowLayout());
    fileTypePanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance().getString("FileTyper.Title")));
    fileTypeComboBox = new JComboBox();
    for (int i = 0; i < choices.length; i++) {
      fileTypeComboBox.addItem(choices[i]);
    }
    fileTypePanel.add(fileTypeComboBox);
    fileTypeComboBox.setSelectedIndex(defaultTypeIndex);
    fileTypeComboBox.addItemListener(this);
    add(fileTypePanel, BorderLayout.CENTER);
  }

  /**
   * Returns the file type which contains the user's choice
   */
  public String getType() {
    return fileType;
  }

  public void itemStateChanged(ItemEvent event) {
    if (event.getSource() == fileTypeComboBox) {
      fileType = (String) fileTypeComboBox.getSelectedItem();
    }
  }

  public void propertyChange(PropertyChangeEvent event) {
    
    String property = event.getPropertyName();
    if (useFileExtensions) {
      if (property.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
        File file = (File) event.getNewValue();
        String fileName = file.toString().toLowerCase();
        if (fileName.endsWith("xyz")) {
          fileTypeComboBox.setSelectedIndex(0);
        } else if (fileName.endsWith("pdb")) {
          fileTypeComboBox.setSelectedIndex(1);
        } else if (fileName.endsWith("cml")) {
          fileTypeComboBox.setSelectedIndex(2);
        } else {
          fileTypeComboBox.setSelectedIndex(0);
        }
      }
    }
  }
  
}

