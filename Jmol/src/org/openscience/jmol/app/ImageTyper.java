/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.app;

import java.io.File;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFileChooser;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class ImageTyper extends JPanel {

  private String[] Choices = {
    "BMP", "JPEG", "PNG", "PPM"
  };
  private int def = 0;
  private String result = Choices[def];
  private int JpegQuality;
  private JSlider qSlider;
  private JComboBox cb;

  /**
   * A simple panel with a combo box for allowing the user to choose
   * the input file type.
   *
   * @param fc the file chooser
   */
  public ImageTyper(JFileChooser fc) {

    setLayout(new BorderLayout());

    JPanel cbPanel = new JPanel();
    cbPanel.setLayout(new FlowLayout());
    cbPanel.setBorder(new TitledBorder("Image Type"));
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
        if (result.equals("JPEG")) {
          qSlider.setEnabled(true);
        } else {
          qSlider.setEnabled(false);
        }
      }
    });

    add(cbPanel, BorderLayout.NORTH);

    JPanel qPanel = new JPanel();
    qPanel.setLayout(new BorderLayout());
    qPanel.setBorder(new TitledBorder("JPEG Quality"));
    qSlider = new JSlider(JSlider.HORIZONTAL, 0, 10, 8);
    qSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSlider.setPaintTicks(true);
    qSlider.setMajorTickSpacing(1);
    qSlider.setPaintLabels(true);
    qSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        JpegQuality = source.getValue();
      }
    });

    // by default, disabled.  Can be turned on with choice of JPEG.
    qSlider.setEnabled(false);
    qPanel.add(qSlider, BorderLayout.SOUTH);
    add(qPanel, BorderLayout.SOUTH);
  }

  /**
   * returns the file type which contains the user's choice
   */
  public String getType() {
    return result;
  }

  /**
   * returns the quality (on a scale from 0 to 10) of the JPEG
   * image that is to be generated.  Returns -1 if choice was not JPEG.
   */
  public int getQuality() {

    int qual = -1;
    if (result.equals("JPEG")) {
      qual = JpegQuality;
    }
    return qual;
  }
}
