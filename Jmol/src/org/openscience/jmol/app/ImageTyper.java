/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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
package org.openscience.jmol.app;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JFileChooser;
import javax.swing.JSlider;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import org.jmol.i18n.GT;

public class ImageTyper extends JPanel {

  private String[] Choices = { "JPEG", "PNG", "PPM" };
  private int def = 0;
  String result = Choices[def];
  JSlider qSlider;
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
    cbPanel.setBorder(new TitledBorder(GT._("Image Type")));
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
    qPanel.setBorder(new TitledBorder(GT._("JPEG Quality")));
    qSlider = new JSlider(SwingConstants.HORIZONTAL, 50, 100, 90);
    qSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    qSlider.setPaintTicks(true);
    qSlider.setMajorTickSpacing(10);
    qSlider.setPaintLabels(true);

    qSlider.setEnabled(true);
    qPanel.add(qSlider, BorderLayout.SOUTH);
    add(qPanel, BorderLayout.SOUTH);
  }

  /**
   * @return The file type which contains the user's choice
   */
  public String getType() {
    return result;
  }

  /**
   * @return The quality (on a scale from 0 to 10) of the JPEG
   * image that is to be generated.  Returns -1 if choice was not JPEG.
   */
  public int getQuality() {
    return qSlider.getValue();
  }
}
