
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

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;


public class MultiLineHeaderRenderer extends JPanel
        implements TableCellRenderer {

  private ImageIcon theIcon;
  private JList theList = new JList();
  private JLabel theImage = new JLabel();

  public MultiLineHeaderRenderer() {

    setLayout(new BorderLayout());
    theList.setOpaque(true);
    theImage.setOpaque(true);
    setOpaque(true);
    theList.setForeground(UIManager.getColor("TableHeader.foreground"));
    theImage.setForeground(UIManager.getColor("TableHeader.foreground"));
    setForeground(UIManager.getColor("TableHeader.foreground"));
    theList.setBackground(UIManager.getColor("TableHeader.background"));
    theImage.setBackground(UIManager.getColor("TableHeader.background"));
    setBackground(UIManager.getColor("TableHeader.background"));
    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    ListCellRenderer renderer = theList.getCellRenderer();
    ((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
    ((JLabel) renderer).setVerticalAlignment(JLabel.CENTER);
    theList.setCellRenderer(renderer);
    add(theList, BorderLayout.CENTER);
    add(theImage, BorderLayout.WEST);
  }

  public ImageIcon getIcon() {
    return (ImageIcon) theImage.getIcon();
  }

  public void setIcon(ImageIcon ii) {
    theImage.setIcon(ii);
  }

  public Component getTableCellRendererComponent(JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int column) {

    setFont(table.getFont());
    String str = (value == null)
                 ? ""
                 : value.toString();
    BufferedReader br = new BufferedReader(new StringReader(str));
    String line;
    Vector v = new Vector();
    try {
      while ((line = br.readLine()) != null) {
        v.addElement(line);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    theList.setListData(v);
    return this;
  }
}

