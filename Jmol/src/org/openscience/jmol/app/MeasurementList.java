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
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import java.awt.event.ActionListener;


public class MeasurementList extends JDialog {

  private JmolViewer viewer;
  private JTable measurementTable;
  private MeasurementTableModel measurementTableModel;
  private int selectedRow = -1;

  private JButton deleteButton;
  private JButton deleteAllButton;

  private ViewMListAction viewmlistAction = new ViewMListAction();

  private String[] headers = { "Value", "1", "2", "3", "4", };

  /**
   * Constructor
   *
   * @param f the parent frame
   * @param dp the DisplayPanel in which the animation will take place
   */
  public MeasurementList(JFrame f, JmolViewer viewer) {

    super(f, JmolResourceHandler.getInstance()
          .translate("Measurement List"), false);
    this.viewer = viewer;

    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

    container.add(constructTable());
    container.add(constructButtonPanel());

    addWindowListener(new MeasurementListWindowListener());

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  JComponent constructTable() {
    measurementTableModel = new MeasurementTableModel();
    measurementTable = new JTable(measurementTableModel);

    measurementTable
      .setPreferredScrollableViewportSize(new Dimension(150, 100));

    measurementTable.getColumnModel().getColumn(0).setPreferredWidth(50);
    for (int i = 5; --i > 0; )
      measurementTable.getColumnModel().getColumn(i).setPreferredWidth(15);

    measurementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    ListSelectionModel lsm = measurementTable.getSelectionModel();
    lsm.addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          //Ignore extra messages.
          if (e.getValueIsAdjusting()) return;
          
          ListSelectionModel lsm = (ListSelectionModel)e.getSource();
          selectedRow = (lsm.isSelectionEmpty()
                         ? -1
                         : lsm.getMinSelectionIndex());
          deleteButton.setEnabled(selectedRow >= 0);
        }
      });

    return new JScrollPane(measurementTable);
  }

  JComponent constructButtonPanel() {
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    deleteButton = new JButton(JmolResourceHandler
                               .getInstance().translate("Delete"));
    deleteButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewer.deleteMeasurement(selectedRow);
          updateTableData();
        }
      });
    deleteButton.setEnabled(false);
    
    deleteAllButton = new JButton(JmolResourceHandler
                                  .getInstance().translate("Delete all"));
    deleteAllButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewer.clearMeasurements();
          updateTableData();
        }
      });
    deleteAllButton.setEnabled(false);

    JButton dismissButton = new JButton(JmolResourceHandler.getInstance()
                                        .translate("Dismiss"));
    dismissButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          close();
        }
      });

    buttonPanel.add(deleteAllButton);
    buttonPanel.add(deleteButton);
    buttonPanel.add(dismissButton);
    getRootPane().setDefaultButton(dismissButton);
    return buttonPanel;
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
    enableActions();
  }

  public void enableActions() {
    viewmlistAction.setEnabled(true);
  }

  public void disableActions() {
    viewmlistAction.setEnabled(false);
  }

  class ViewMListAction extends AbstractAction {

    public ViewMListAction() {
      super("viewmlist");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      updateTableData();
      show();
    }
  }

  void updateTableData() {
    deleteAllButton.setEnabled(viewer.getMeasurementCount() > 0);
    measurementTableModel.fireTableDataChanged();
  }

  Action[] actions = {viewmlistAction};
  public Action[] getActions() {
    return actions;
  }

  protected Action getAction(String cmd) {
    if (cmd.equals("viewmlist"))
      return viewmlistAction;
    System.out.println("getAction called with cmd:" + cmd);
    return null;
  }

  class MeasurementListWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  class MeasurementTableModel extends AbstractTableModel {

    public String getColumnName(int col) { 
      return headers[col];
    }
    public int getRowCount() { return viewer.getMeasurementCount(); }
    public int getColumnCount() { return 5; };

    Class stringClass = "".getClass();
    Class integerClass = new Integer(0).getClass();

    public Class getColumnClass(int col) {
      return (col == 0 ? stringClass : integerClass);
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
}
