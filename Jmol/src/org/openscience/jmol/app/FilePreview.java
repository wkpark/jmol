/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;

/**
 * File previsualisation before opening
 */
public class FilePreview extends JPanel implements PropertyChangeListener {

  JCheckBox active = null;
  JFileChooser chooser = null;
  private JmolPanel display = null;

  /**
   * Constructor
   * @param fileChooser File chooser
   * @param modelAdapter Model adapter
   */
  public FilePreview(JFileChooser fileChooser, JmolAdapter modelAdapter) {
    super();
    chooser = fileChooser;

    // Create a box to do the layout
    Box box = Box.createVerticalBox();

    // Add a checkbox to activate / deactivate preview
    active = new JCheckBox("Preview", false);
    active.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e) {
          if (active.isSelected()) {
            updatePreview(chooser.getSelectedFile());
          } else {
            updatePreview(null);
          }
        }
      });
    box.add(active);

    // Add a preview area
    display = new JmolPanel(modelAdapter);
    display.setPreferredSize(new Dimension(80, 80));
    display.setMinimumSize(new Dimension(50, 50));
    box.add(display);

    // Add the preview to the File Chooser
    add(box);
    fileChooser.setAccessory(this);
    fileChooser.addPropertyChangeListener(this);
  }
	
  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if (active.isSelected()) {
      String prop = evt.getPropertyName();
      if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
        updatePreview((File) evt.getNewValue());
      }
    }
  }
  
  /**
   * Update preview
   * 
   * @param file File selected
   */
  void updatePreview(File file) {
    if (file != null) {
      display.getViewer().evalStringQuiet("load " + file.getAbsolutePath());
      display.repaint();
    } else {
      display.getViewer().evalStringQuiet("zap");
    }
  }
}

class JmolPanel extends JPanel {
  JmolViewer viewer;
  
  JmolPanel(JmolAdapter modelAdapter) {
    viewer = JmolViewer.allocateViewer(this, modelAdapter);
  }

  public JmolViewer getViewer() {
    return viewer;
  }

  final Dimension currentSize = new Dimension();

  public void paint(Graphics g) {
    viewer.setScreenDimension(getSize(currentSize));
    Rectangle rectClip = new Rectangle();
    g.getClipBounds(rectClip);
    viewer.renderScreenImage(g, currentSize, rectClip);
  }
}
