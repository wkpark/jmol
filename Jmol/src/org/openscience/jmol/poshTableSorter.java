
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

import java.util.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;

public class poshTableSorter extends TableSorter {
    private static boolean DIR_ASCENDING = true;
    private static boolean DIR_DESCENDING = false;
    protected boolean sortedDir = DIR_ASCENDING;
    protected int sortedColumn;
    private int hTextPos = SwingConstants.RIGHT;
    
    private static JmolResourceHandler jrh;
    protected static ImageIcon iconUp;
    protected static ImageIcon iconDown;

    static {
        jrh = new JmolResourceHandler("poshTableSorter");
    }     
    
    public poshTableSorter() {
        super();
        sortedColumn = -1;
    }
    
    public poshTableSorter(TableModel model) {
        super(model);
        iconUp = jrh.getIcon("upImage");
        iconDown = jrh.getIcon("downImage");
        sortedColumn = -1;
    }
    
    public void setHorizontalTextPosition( int x ) {
        hTextPos = x;
    }

    public void sortByColumn(int column, int vcolumn, JTable table ) {
        if( sortedColumn != -1 ) {
            setColumnIcon( table.convertColumnIndexToView(sortedColumn), 
                           null, table );
        }

        if( column == sortedColumn ) {
            sortedDir = !sortedDir;
        } else {
            if( sortedColumn != -1 )
                setColumnIcon( sortedColumn, null, table );
            sortedDir = DIR_ASCENDING;
        }

        if( sortedDir == DIR_ASCENDING ) {
            setColumnIcon( vcolumn, iconDown, table );
        } else {
            setColumnIcon( vcolumn, iconUp, table );
        }

        sortedColumn = column;
        super.sortByColumn( column, sortedDir );
    }

    private void setColumnIcon( int col, ImageIcon icon, JTable table ) {
        TableColumnModel cm = table.getColumnModel();
        TableColumn tc = cm.getColumn( col );
        MultiLineHeaderRenderer rr = (MultiLineHeaderRenderer)tc.getHeaderRenderer();
        rr.setIcon( icon );
        //rr.setHorizontalTextPosition( hTextPos );
    }

    public void addMouseListenerToHeaderInTable(JTable table) {
        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if(e.getClickCount() == 1 && column != -1) {
                    sortByColumn(column, viewColumn, tableView);
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }    
    
}
