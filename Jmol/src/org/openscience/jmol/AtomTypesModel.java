/*
 * @(#)AtomTypesModel.java    1.0 99/05/06
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import javax.swing.table.*;
import java.util.*;
import java.awt.Color;

class AtomTypesModel extends AbstractTableModel {

    protected static int NUM_COLUMNS = 7;
    protected static int START_NUM_ROWS = 5;
    protected int nextEmptyRow = 0;
    protected int numRows = 0;
    
    static final public String[] names = {"Atom Type", "Base\nAtom Type", 
                                          "Atomic\nNumber", "Atomic\nMass", 
                                          "Van derWaals\nRadius",
                                          "Covalent\nRadius", "Color"};

    protected Vector data = null;

    public AtomTypesModel() {
        data = new Vector();
    }

	/**
	 * Returns the name of the column at the index given. If the column is undefined,
	 * null is returned.
	 */
    public String getColumnName(int index) {
		if (index < 0 || index > names.length) {
			return null;
		}
        return names[index];
    }

    public synchronized int getColumnCount() {
        return NUM_COLUMNS;
    }

    public synchronized int getRowCount() {
        if (numRows < START_NUM_ROWS) {
            return START_NUM_ROWS;
        } else {
            return numRows;
        }
    }

    public boolean isCellEditable(int row, int col) {
        if (row < START_NUM_ROWS && row > numRows) 
            return false;
        else 
            return true;
    }
    
    public void setValueAt(Object o, int row, int col) {
		AtomType at = (AtomType)data.elementAt(row);
		switch (col) {
		case 1:            
			at.setRoot((String)o);
			break;
		case 2:
			at.setAtomicNumber(((Integer)o).intValue());
			break;
		case 3:
			at.setMass(((Double)o).doubleValue());
			break;
		case 4:
			at.setvdWRadius(((Double)o).doubleValue());
			break;
		case 5:
			at.setCovalentRadius(((Double)o).doubleValue());
			break;
		case 6:
			at.setColor((Color)o);
			break;
		}
		updateAtomType(at);
    }
    
    public synchronized Object getValueAt(int row, int column) {
        try {
            AtomType at = (AtomType)data.elementAt(row);
            switch (column) {
            case 0:
                return at.getName();
            case 1:
                return at.getRoot();
            case 2:
                return new Integer(at.getAtomicNumber());
            case 3:
                return new Double(at.getMass());
            case 4:
                return new Double(at.getvdWRadius());
            case 5:
                return new Double(at.getCovalentRadius());
            case 6:
                return (Color)at.getColor();
            }
        } catch (Exception e) {
        }
        return "";
    }

    public Class getColumnClass(int column) {
        String s = "0";
        Integer i = new Integer(0);
        Double d = new Double(0.0);
        Color c = new Color(0,0,0);
        switch (column) {
        case 2:
            return i.getClass();
        case 3:
            return d.getClass();
        case 4:
            return d.getClass();
        case 5:
            return d.getClass();
        case 6:
            return c.getClass();
        default:
            return s.getClass();
        }
    }
            
    public synchronized void updateAtomType(AtomType atomType) {
        String name = atomType.getName(); 
        AtomType at = null;
        int index = -1; 
        boolean found = false;
        boolean addedRow = false;
        
        int i = 0;
        while (!found && (i < nextEmptyRow)) {
            at = (AtomType)data.elementAt(i);
            if (name.equals(at.getName())) {
                found = true;
                index = i;
            } else {
                i++;
            }
        }
        
        if (found) { //update old AtomType
            data.setElementAt(atomType, index);
        } else { //add new AtomType
            if (numRows <= nextEmptyRow) {
                //add a row
                numRows++;
                addedRow = true;
            }
            index = nextEmptyRow;
            data.addElement(atomType);
        }
    
        nextEmptyRow++;

        //Notify listeners that the data changed.
        if (addedRow) {
            fireTableRowsInserted(index, index);
        } else {
            fireTableRowsUpdated(index, index);
        }
    }


    public synchronized void clear() {
        int oldNumRows = numRows;
        
        numRows = START_NUM_ROWS;
        data.removeAllElements();
        nextEmptyRow = 0;
        
        if (oldNumRows > START_NUM_ROWS) {
            fireTableRowsDeleted(START_NUM_ROWS, oldNumRows - 1);
        }
        fireTableRowsUpdated(0, START_NUM_ROWS - 1);
    }

	/**
	 * Returns the first occurence of an AtomType with the given name.
	 */
    public AtomType get(String name) {
		if (name != null) {
			for (Enumeration e = data.elements() ; e.hasMoreElements() ;) {
				AtomType at = (AtomType) e.nextElement();
				if (name.equalsIgnoreCase(at.getName())) {
					return at;
				}
			}
		}
        return null;
    }

	/**
	 * Returns the first occurence of an AtomType with the given atomic number.
	 */
    public AtomType get(int atomicNumber) {
        for (Enumeration e = data.elements() ; e.hasMoreElements() ;) {
            AtomType at = (AtomType) e.nextElement();
            if (atomicNumber == at.getAtomicNumber()) {
                return at;
            }
        }
        return null;
    }

    public synchronized Enumeration elements() {
        return data.elements();
    }

}

