// File: MultiLineHeaderRenderer.java
/* (swing1.1beta3) */
// Originally from: Nobuo Tamemasa (tame@gol.com)
// package jp.gr.java_conf.tame.swing.table;
//
// Modified heavily by Dan Gezelter to support icons as well as
// a multi-line header.

package org.openscience.jmol;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;


/**
 * @version 1.0 11/09/98
 */

public class MultiLineHeaderRenderer extends JPanel implements TableCellRenderer {

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
        ((JLabel)renderer).setHorizontalAlignment(JLabel.CENTER);
        ((JLabel)renderer).setVerticalAlignment(JLabel.CENTER);
        theList.setCellRenderer(renderer);
        add(theList, BorderLayout.CENTER);
        add(theImage, BorderLayout.WEST);
    }

    public void setIcon(ImageIcon ii) {
        theImage.setIcon(ii);
    }
    
    public Component getTableCellRendererComponent(JTable table, 
                                                   Object value,
                                                   boolean isSelected, 
                                                   boolean hasFocus, 
                                                   int row, int column) {
        setFont(table.getFont());
        String str = (value == null) ? "" : value.toString();
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

