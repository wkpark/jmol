
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

public class FileTyper extends JPanel implements PropertyChangeListener {

  File f = null;
  private JFileChooser myChooser;
  private JComboBox cb;
  private static boolean UseFileExtensions = true;
  private static JmolResourceHandler jrh;

  static {
    jrh = new JmolResourceHandler("FileTyper");
  }
  private String[] Choices = {
    jrh.getString("Automatic"), jrh.getString("XYZ"), jrh.getString("PDB"),
    jrh.getString("CML"), jrh.getString("GhemicalMM")
  };

  // Default is the first one:
  private int def = 0;
  private String result = Choices[def];

  /**
   * Should we use the file extension to set the file type????
   *
   * @param ufe boolean controlling the behavior of this component
   */
  public static void setUseFileExtensions(boolean ufe) {
    UseFileExtensions = ufe;
  }

  /**
   * Are we using the file extension to set the file type????
   */
  public static boolean getUseFileExtensions() {
    return UseFileExtensions;
  }

  /**
   * A simple panel with a combo box for allowing the user to choose
   * the input file type.
   *
   * @param fc the file chooser
   */
  public FileTyper(JFileChooser fc) {

    myChooser = fc;

    setLayout(new BorderLayout());

    JPanel cbPanel = new JPanel();
    cbPanel.setLayout(new FlowLayout());
    cbPanel.setBorder(new TitledBorder(jrh.getString("Title")));
    cb = new JComboBox();
    for (int i = 0; i < Choices.length; i++) {
      cb.addItem(Choices[i]);
    }
    cbPanel.add(cb);
    cb.setSelectedIndex(def);
    cb.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {
        JComboBox source = (JComboBox) e.getSource();
        result = (String) source.getSelectedItem();
      }
    });
    add(cbPanel, BorderLayout.CENTER);    // Change to NORTH if other controls

    fc.addPropertyChangeListener(this);
  }

  /**
   * returns the file type which contains the user's choice
   */
  public String getType() {
    return result;
  }

  public void propertyChange(PropertyChangeEvent e) {

    String prop = e.getPropertyName();
    if (prop == JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) {
      f = (File) e.getNewValue();
      String fname = f.toString().toLowerCase();
      System.out.println(fname);
      String lastSection = f.getName();
      if (lastSection.startsWith("*.")) {
        String type = lastSection.substring(2);

        //myChooser.setFileFilter(new JmolFileFilter(type,null,true));
      } else if (UseFileExtensions) {
        if (fname.endsWith("xyz")) {
          cb.setSelectedIndex(1);
        } else if (fname.endsWith("pdb")) {
          cb.setSelectedIndex(2);
        } else if (fname.endsWith("cml")) {
          cb.setSelectedIndex(3);
        } else if (fname.endsWith("mm1gp")) {
          cb.setSelectedIndex(4);
        } else {
          cb.setSelectedIndex(0);
        }
      }
    }
  }
}

