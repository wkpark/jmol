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

import org.openscience.jmol.viewer.JmolViewer;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;

public class MeasurementTable extends JDialog {

  private JmolViewer viewer;
  private JTable measurementTable;
  private MeasurementTableModel measurementTableModel;
  private ListSelectionModel measurementSelection;
  private int selectedMeasurementRow = -1;
  private JButton deleteButton;
  private JButton deleteAllButton;

  private JTable pickingTable;
  private PickingTableModel pickingTableModel;
  private ListSelectionModel pickingSelection;
  private int selectedPickingRow = -1;
  private JButton addButton;
  private JButton clearButton;
  private JButton measureButton;

  /**
   * Constructor
   *
   * @param f the parent frame
   * @param dp the DisplayPanel in which the animation will take place
   */
  public MeasurementTable(JmolViewer viewer, JFrame parentFrame) {

    super(parentFrame, JmolResourceHandler.getInstance()
          .translate("Measurements..."), false);
    this.viewer = viewer;

    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

    container.add(constructPickingTable());
    container.add(constructPickingButtonPanel());
    container.add(constructMeasurementTable());

    JPanel foo = new JPanel();
    foo.setLayout(new BorderLayout());
    foo.add(constructMeasurementButtonPanel(), BorderLayout.WEST);
    foo.add(constructDismissButtonPanel(), BorderLayout.EAST);

    container.add(foo);

    addWindowListener(new MeasurementListWindowListener());

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  JComponent constructPickingTable() {
    pickingTableModel = new PickingTableModel();
    pickingTable = new JTable(pickingTableModel);

    pickingTable
      .setPreferredScrollableViewportSize(new Dimension(300, 75));

    for (int i = 5; --i >= 0; )
      pickingTable.getColumnModel().getColumn(i)
        .setPreferredWidth(i <= 1 ? 20 : 25);

    pickingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    pickingTable.setRowSelectionAllowed(true);
    pickingTable.setColumnSelectionAllowed(false);
    pickingSelection = pickingTable.getSelectionModel();
    pickingSelection.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) return;
          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
          if (! lsm.isSelectionEmpty()) {
            int row = lsm.getMinSelectionIndex();
            // I tried to adjust the row here, but it didn't work
            // presumably because we are in the midst of setting the row
            if (! inPickingMode)
              startPickingMode();
            measurementSelection.clearSelection();
          }
        }
      });

    return new JScrollPane(pickingTable);
  }

  JComponent constructPickingButtonPanel() {
    JPanel pickingButtonPanel = new JPanel();
    pickingButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    measureButton = new JButton("Measure");
    measureButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewer.defineMeasurement(pickedAtomCount, pickedAtoms);
          updateMeasurementTableData();
          pickedAtomCount = 0;
          updatePickingTableData();
        }
      });
    measureButton.setVisible(false);
    
    clearButton = new JButton(JmolResourceHandler
                              .getInstance().translate("Clear"));
    clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          startPickingMode();
        }
      });
    clearButton.setEnabled(false);
    
    pickingButtonPanel.add(measureButton);
    pickingButtonPanel.add(clearButton);
    return pickingButtonPanel;
  }

  JComponent constructMeasurementTable() {
    measurementTableModel = new MeasurementTableModel();
    measurementTable = new JTable(measurementTableModel);

    measurementTable
      .setPreferredScrollableViewportSize(new Dimension(300, 100));

    measurementTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    for (int i = 5; --i > 0; )
      measurementTable.getColumnModel().getColumn(i).setPreferredWidth(15);

    measurementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    measurementTable.setRowSelectionAllowed(true);
    measurementTable.setColumnSelectionAllowed(false);
    measurementSelection = measurementTable.getSelectionModel();
    measurementSelection.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) return;
          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
          if (lsm.isSelectionEmpty()) {
            selectedMeasurementRow = -1;
            deleteButton.setEnabled(false);
          } else {
            selectedMeasurementRow = lsm.getMinSelectionIndex();
            deleteButton.setEnabled(true);
            pickingSelection.clearSelection();
            clearPickingMode();
          }
        }
      });

    return new JScrollPane(measurementTable);
  }

  JComponent constructMeasurementButtonPanel() {
    JPanel measurementButtonPanel = new JPanel();
    measurementButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

    deleteButton = new JButton(JmolResourceHandler
                               .getInstance().translate("Delete"));
    deleteButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewer.deleteMeasurement(selectedMeasurementRow);
          updateMeasurementTableData();
          startPickingMode();
        }
      });
    deleteButton.setEnabled(false);
    
    deleteAllButton = new JButton(JmolResourceHandler
                                  .getInstance().translate("DeleteAll"));
    deleteAllButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewer.clearMeasurements();
          updateMeasurementTableData();
          startPickingMode();
        }
      });
    deleteAllButton.setEnabled(false);

    measurementButtonPanel.add(deleteAllButton);
    measurementButtonPanel.add(deleteButton);
    return measurementButtonPanel;
  }

  JComponent constructDismissButtonPanel() {
    JPanel dismissButtonPanel = new JPanel();
    dismissButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    JButton dismissButton = new JButton(JmolResourceHandler.getInstance()
                                        .translate("Dismiss"));
    dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clearPickingMode();
          close();
        }
      });
    dismissButtonPanel.add(dismissButton);
    getRootPane().setDefaultButton(dismissButton);
    return dismissButtonPanel;
  }

  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  public void close() {
    this.setVisible(false);
  }

  public void activate() {
    updateMeasurementTableData();
    startPickingMode();
    show();
  }

  void updateMeasurementTableData() {
    deleteAllButton.setEnabled(viewer.getMeasurementCount() > 0);
    measurementTableModel.fireTableDataChanged();
  }

  void updatePickingTableData() {
    clearButton.setEnabled(pickedAtomCount > 0);
    String measureButtonLabel = "Distance";
    if (pickedAtomCount == 3)
      measureButtonLabel = "Angle";
    else if (pickedAtomCount == 4)
      measureButtonLabel = "Dihedral Angle";
    measureButton.setLabel(measureButtonLabel);
    measureButton.setVisible(pickedAtomCount >= 2);
    pickingTableModel.fireTableDataChanged();

    int row = (pickedAtomCount < 4 ? pickedAtomCount : 3);
    pickingTable.setRowSelectionInterval(row, row);
    pickingTable.requestFocus();
    requestFocus();
  }

  class MeasurementListWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      clearPickingMode();
      close();
    }
  }

  final Class stringClass = "".getClass();
  final Class integerClass = new Integer(0).getClass();
  final Class floatClass = new Float(0).getClass();

  class MeasurementTableModel extends AbstractTableModel {

    final String[] measurementHeaders = {
      JmolResourceHandler.getInstance().translate("Value"),
      "a", "b", "c", "d", };

    public String getColumnName(int col) { 
      return measurementHeaders[col];
    }
    public int getRowCount() { return viewer.getMeasurementCount(); }
    public int getColumnCount() { return 5; };

    public Class getColumnClass(int col) {
      return integerClass;
    }
    public Object getValueAt(int row, int col) {
      if (col == 0)
        return viewer.getMeasurementString(row);
      int[] indices = viewer.getMeasurementIndices(row);
      int i = col-1;
      return (i < indices.length ? new Integer(indices[i]) : null);
    }

    public boolean isCellEditable(int row, int col) { return false; }
  }

  int pickedAtomCount;
  final int[] pickedAtoms = new int[4];

  class PickingTableModel extends AbstractTableModel {
    final String[] pickingHeaders = {
            JmolResourceHandler.getInstance().translate("Atom"),
            JmolResourceHandler.getInstance().translate("Symbol"),
            "x", "y", "z"};

    public String getColumnName(int col) { 
      return pickingHeaders[col];
    }
    public int getRowCount() { return 4; };
    public int getColumnCount() { return 5; };

    public Class getColumnClass(int col) {
      if (col == 0)
        return integerClass;
      if (col == 1)
        return stringClass;
      return floatClass;
    }
    public Object getValueAt(int row, int col) {
      if (row < pickedAtomCount)
        switch(col) {
        case 0:
          return new Integer(pickedAtoms[row]);
        case 1:
          return viewer.getAtomicSymbol(pickedAtoms[row]);
        case 2:
          return new Float(viewer.getAtomX(pickedAtoms[row]));
        case 3:
          return new Float(viewer.getAtomY(pickedAtoms[row]));
        case 4:
          return new Float(viewer.getAtomZ(pickedAtoms[row]));
        }
      return null;
    }
    public boolean isCellEditable(int row, int col) { return false; }
  }

  boolean inPickingMode = false;
  int previousMouseMode;

  void startPickingMode() {
    if (! inPickingMode) {
      inPickingMode = true;
      previousMouseMode = viewer.getModeMouse();
      viewer.setModeMouse(JmolViewer.MEASURE);
    }
    pickedAtomCount = 0;
    updatePickingTableData();
  }

  void clearPickingMode() {
    if (inPickingMode) {
      inPickingMode = false;
      pickedAtomCount = 0;
      pickingTableModel.fireTableDataChanged();
      viewer.setModeMouse(previousMouseMode);
    }
  }

  public void firePicked(int atomIndex) {
    if (! inPickingMode)
      return;
    if (pickedAtomCount < 4) {
      for (int i = pickedAtomCount; --i >= 0; )
        if (pickedAtoms[i] == atomIndex) {
          System.out.println("double picked");
          return;
        }
      pickedAtoms[pickedAtomCount++] = atomIndex;
      updatePickingTableData();
    }
  }
}
