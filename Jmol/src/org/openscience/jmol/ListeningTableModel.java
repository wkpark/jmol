
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

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;

/**
 *  A table model wrapper which listens to model changes and fowards them
 *  to other listeners.
 *
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class ListeningTableModel extends AbstractTableModel
    implements TableModelListener {

  /**
   *  Creates a listening table model for the given table model. This table
   *  model will listen for table change events from the given table model.
   */
  public ListeningTableModel(TableModel model) {
    this.model = model;
    model.addTableModelListener(this);
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public Object getValueAt(int aRow, int aColumn) {
    return model.getValueAt(aRow, aColumn);
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public void setValueAt(Object aValue, int aRow, int aColumn) {
    model.setValueAt(aValue, aRow, aColumn);
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public int getRowCount() {
    return model.getRowCount();
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public int getColumnCount() {
    return model.getColumnCount();
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public String getColumnName(int aColumn) {
    return model.getColumnName(aColumn);
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public Class getColumnClass(int aColumn) {
    return model.getColumnClass(aColumn);
  }

  /**
   *  Forwards messages to the AbstractTableModel
   */
  public boolean isCellEditable(int row, int column) {
    return model.isCellEditable(row, column);
  }

  /**
   *  Forwards events to listeners of this model.
   */
  public void tableChanged(TableModelEvent event) {
    fireTableChanged(event);
  }

  /**
   *  The actual table model.
   */
  protected TableModel model;
}

