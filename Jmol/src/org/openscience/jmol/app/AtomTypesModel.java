/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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

import org.openscience.jmol.BaseAtomType;
import org.openscience.jmol.AtomTypeList;

import java.awt.Color;
import javax.swing.table.AbstractTableModel;
import java.util.Vector;
import java.util.Enumeration;
import org.openscience.jmol.render.AtomColors;

class AtomTypesModel extends AbstractTableModel {

  protected static int NUM_COLUMNS = 7;
  protected int nextEmptyRow = 0;
  protected int numRows = 0;

  protected static final String[] names = {
    "Atom Type", "Base Atom Type", "Atomic Number", "Atomic Mass",
    "Van derWaals Radius", "Covalent Radius", "Color"
  };
  protected static final Class[] classes = {
    String.class, String.class, Integer.class, Double.class, Double.class,
    Double.class, Color.class
  };

  public AtomTypesModel() {}

  /**
   * Returns the name of the column at the index given. If the column is undefined,
   * null is returned.
   */
  public String getColumnName(int index) {
    if ((index < 0) || (index > names.length)) {
      return null;
    }
    return JmolResourceHandler.getInstance().translate(names[index]);
  }

  public synchronized int getColumnCount() {
    return NUM_COLUMNS;
  }

  public synchronized int getRowCount() {
      return AtomTypeList.getInstance().getSize();
  }

  /**
   * Returns whether a specified cell is editable. All valid rows and columns will be
   * editable, except column 0.
   */
  public boolean isCellEditable(int row, int column) {

    boolean isNotColumn0 = column != 0;
    if (isValidRow(row) && isValidColumn(column) && isNotColumn0) {
      return true;
    } else {
      return false;
    }
  }

  public void setValueAt(Object o, int row, int col) {

      AtomTypeList atl = AtomTypeList.getInstance();
      
    BaseAtomType at = atl.getElementAt(row);
    switch (col) {
    case 1 :
      at.setSymbol((String) o);
      break;

    case 2 :
      at.setAtomicNumber(((Integer) o).intValue());
      break;

    case 3 :
      at.setExactMass(((Double) o).doubleValue());
      break;

    case 4 :
      at.setVdwRadius(((Double) o).doubleValue());
      break;

    case 5 :
      at.setCovalentRadius(((Double) o).doubleValue());
      break;

    case 6 :
      at.setColor((Color) o);
      break;
    }
  }

  public synchronized Object getValueAt(int row, int column) {

    try {
        // System.err.println("Getting data for atom type: " + row + ", " + column);
        BaseAtomType at = AtomTypeList.getInstance().getElementAt(row);
        if (at == null) {
            System.err.println("Retrieved BAT is null!");
            return "";
        }
      switch (column) {
      case 0 :
        return at.getID();

      case 1 :
        return at.getSymbol();

      case 2 :
        return new Integer(at.getAtomicNumber());

      case 3 :
        double m = at.getExactMass();
        System.err.println("Mass: " + m);
        Double mass = new Double(m);
        System.err.println("Mass: " + mass.toString());
        return mass;

      case 4 :
        return new Double(at.getVdwRadius());

      case 5 :
        return new Double(at.getCovalentRadius());

      case 6 :
        return at.getColor();
      }
    } catch (Exception e) {
        System.err.println(e.toString());
        e.printStackTrace();
    }
    return "";
  }

  public Class getColumnClass(int column) {
    if ((column >= 0) && (column < classes.length)) {
      return classes[column];
    }
    return String.class;
  }

  public synchronized void update() {
      fireTableRowsUpdated(0, AtomTypeList.getInstance().getSize() - 1);
  }
  
  public synchronized void clear() {

    int oldNumRows = numRows;

    numRows = 5;
    AtomTypeList.getInstance().removeAllElements();
    nextEmptyRow = 0;

    if (oldNumRows > 5) {
      fireTableRowsDeleted(5, oldNumRows - 1);
    }
    fireTableRowsUpdated(0, 5 - 1);
  }

  public synchronized Enumeration elements() {
    return AtomTypeList.getInstance().elements();
  }

  /**
   * Returns whether the row index is valid.
   */
  boolean isValidRow(int row) {
    return (row >= 0) && (row < numRows);
  }

  /**
   * Returns whether the column index is valid.
   */
  boolean isValidColumn(int column) {
    return (column >= 0) && (column < names.length);
  }

}

