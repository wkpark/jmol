/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002  The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.render.Angle;
import org.openscience.jmol.render.Distance;
import org.openscience.jmol.render.Dihedral;

import java.io.File;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.JScrollPane;
import java.awt.Container;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.EventObject;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.ListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.border.TitledBorder;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class Measure extends JDialog {

  private final static int ADD = 1;
  private final static int DELETE = 2;
  private final static int DISTANCE = 2;
  private final static int ANGLE = 3;
  private final static int DIHEDRAL = 4;
  private int action;
  private int measure = 1;
  private int oldMode;
  private int currentAtom = 0;
  private int[] selection = {
    -1, -1, -1, -1
  };
  private ChemFile cf;

  private DisplayPanel display;
  private MouseManager mouseman;
  private MeasurementList mlist;

  // The actions:

  private DistanceAction distanceAction = new DistanceAction();
  private AngleAction angleAction = new AngleAction();
  private DihedralAction dihedralAction = new DihedralAction();
  private DDistanceAction ddistanceAction = new DDistanceAction();
  private DAngleAction dangleAction = new DAngleAction();
  private DDihedralAction ddihedralAction = new DDihedralAction();
  private Hashtable commands;
  private MeasureTableModel mtm = new MeasureTableModel();
  private JTable table = new JTable();

  private JButton mButton =
    new JButton(JmolResourceHandler.getInstance()
      .getString("Measure.deleteLabel"));
  private TitledBorder mBorder = new TitledBorder(" ");


  class MeasureTableModel extends AbstractTableModel {

    final String[] columnNames = {
      " ", JmolResourceHandler.getInstance().getString("Measure.atomnLabel"),
      JmolResourceHandler.getInstance().getString("Measure.atomidentLabel"),
      JmolResourceHandler.getInstance().getString("Measure.xLabel"),
      JmolResourceHandler.getInstance().getString("Measure.yLabel"),
      JmolResourceHandler.getInstance().getString("Measure.zLabel")
    };

    final Object[][] data = {
      {
        JmolResourceHandler.getInstance().getString("Measure.atomaLabel"),
        " ", " ", " ", " ", " "
      }, {
        JmolResourceHandler.getInstance().getString("Measure.atombLabel"),
        " ", " ", " ", " ", " "
      }, {
        JmolResourceHandler.getInstance().getString("Measure.atomcLabel"),
        " ", " ", " ", " ", " "
      }, {
        JmolResourceHandler.getInstance().getString("Measure.atomdLabel"),
        " ", " ", " ", " ", " "
      }
    };
    public int getColumnCount() {
      return columnNames.length;
    }

    public int getRowCount() {
      return measure;
    }

    public String getColumnName(int col) {
      return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
      return data[row][col];
    }

    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
      return false;
    }

    public void setValueAt(Object value, int row, int col) {
      data[row][col] = value;
      fireTableCellUpdated(row, col);
    }

    public void updateRow(int row, int i, String n, double[] c) {

      data[row][1] = (new Integer(i)).toString();
      data[row][2] = n;
      data[row][3] = (new Double(c[0])).toString();
      data[row][4] = (new Double(c[1])).toString();
      data[row][5] = (new Double(c[2])).toString();
      fireTableDataChanged();
    }

    public void wipe() {

      for (int row = 0; row < 4; row++) {
        for (int col = 1; col < 6; col++) {
          data[row][col] = " ";
        }
      }
    }
  }

  /**
   * Constructor
   *
   * @param f the parent frame
   * @param dp the DisplayPanel in which the animation will take place
   */
  public Measure(JFrame f, DisplayPanel dp, MouseManager mouseman) {

    super(f, JmolResourceHandler.getInstance()
             .getString("Measure.windowTitle"), false);
    this.display = dp;
    this.mouseman = mouseman;
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

    JPanel mPanel = new JPanel();
    mPanel.setLayout(new BorderLayout());
    mPanel.setBorder(mBorder);

    table.setModel(mtm);
    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    ListSelectionModel rowSM = table.getSelectionModel();
    rowSM.addListSelectionListener(new ListSelectionListener() {

      public void valueChanged(ListSelectionEvent e) {

        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (lsm.isSelectionEmpty()) {

          // System.out.println("No rows are selected.");
        } else {
          int selectedRow = lsm.getMinSelectionIndex();
          currentAtom = selectedRow;
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane(table);
    mPanel.add(scrollPane, BorderLayout.CENTER);
    container.add(mPanel);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    mButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        MeasurePressed();
      }
    });
    buttonPanel.add(mButton);
    mButton.setEnabled(false);
    JButton cancel =
      new JButton(JmolResourceHandler.getInstance()
        .getString("Measure.cancelLabel"));
    cancel.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    buttonPanel.add(cancel);
    getRootPane().setDefaultButton(mButton);

    container.add(buttonPanel);

    addWindowListener(new MeasureWindowListener());

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  public void setMeasurementList(MeasurementList mlist) {
    this.mlist = mlist;
  }

  private void initialize() {

    for (int i = 0; i < 4; i++) {
      selection[i] = -1;
    }
    table.setRowSelectionInterval(0, 0);
    mtm.fireTableDataChanged();
    switch (measure) {
    case ANGLE :
      mBorder.setTitle(JmolResourceHandler.getInstance()
          .getString("Measure.infoString3"));
      break;

    case DIHEDRAL :
      mBorder.setTitle(JmolResourceHandler.getInstance()
          .getString("Measure.infoString4"));
      break;

    default :
      mBorder.setTitle(JmolResourceHandler.getInstance()
          .getString("Measure.infoString2"));
      break;
    }
    if (action == DELETE) {
      mButton.setText(JmolResourceHandler.getInstance()
          .getString("Measure.deleteLabel"));
    } else {
      mButton.setText(JmolResourceHandler.getInstance()
          .getString("Measure.addLabel"));
    }
    oldMode = mouseman.getMode();
    mouseman.setMode(MouseManager.MEASURE);
    disableActions();
    show();
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

    mouseman.setMode(oldMode);
    this.setVisible(false);
    mtm.wipe();
    mButton.setEnabled(false);
    enableActions();
  }

  public void firePicked(int measured) {

    for (int i = 0; i < 4; i++) {
      if (selection[i] == measured) {

        JmolResourceHandler jrh = JmolResourceHandler.getInstance();

        String error = jrh.translate("You must have") + " " +
                       measure + " " +
                       jrh.translate("unique atoms for this measurement.");

        JOptionPane.showMessageDialog(null, error,
            jrh.translate("Invalid Input"),
            JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
    ChemFrame cf = display.getFrame();
    Atom a = (org.openscience.jmol.Atom)cf.getAtomAt(measured);
    double[] c = cf.getAtomCoords(measured);
    selection[currentAtom] = measured;

    mtm.updateRow(currentAtom, a.getAtomNumber() + 1, a.getSymbol(), c);
    if (currentAtom < measure - 1) {
      currentAtom++;
      table.setRowSelectionInterval(currentAtom, currentAtom);
    } else {
      mButton.setEnabled(true);
    }
  }

  public void MeasurePressed() {

    switch (measure) {
    case ANGLE :
      if (action == ADD) {
        Angle a = new Angle(selection[0], selection[1], selection[2]);
        mlist.addAngle(a);
      } else {
        boolean ok = mlist.deleteMatchingAngle(selection[0], selection[1],
                       selection[2]);
        if (!ok) {
          JmolResourceHandler jrh = JmolResourceHandler.getInstance();

          String error = jrh.translate("No matching Angle was found");
          JOptionPane.showMessageDialog(null, error,
              jrh.translate("Invalid Input"),
              JOptionPane.ERROR_MESSAGE);
        }
      }
      break;

    case DIHEDRAL :
      if (action == ADD) {
        Dihedral d = new Dihedral(selection[0], selection[1], selection[2],
                       selection[3]);
        mlist.addDihedral(d);
      } else {
        boolean ok = mlist.deleteMatchingDihedral(selection[0], selection[1],
                       selection[2], selection[3]);
        if (!ok) {
          JmolResourceHandler jrh = JmolResourceHandler.getInstance();

          String error = jrh.translate("No matching Dihedral was found");
          JOptionPane.showMessageDialog(null, error,
              jrh.translate("Invalid Input"),
              JOptionPane.ERROR_MESSAGE);
        }
      }
      break;

    default :
      if (action == ADD) {
        Distance ds = new Distance(selection[0], selection[1]);
        mlist.addDistance(ds);
      } else {
        boolean ok = mlist.deleteMatchingDistance(selection[0], selection[1]);
        if (!ok) {
          JmolResourceHandler jrh = JmolResourceHandler.getInstance();

          String error = jrh.translate("No matching Distance was found");
          JOptionPane.showMessageDialog(null, error,
              jrh.translate("Invalid Input"),
              JOptionPane.ERROR_MESSAGE);
        }
      }
      break;
    }
    
    mtm.wipe();
    for (int i = 0; i < 4; i++) {
      selection[i] = -1;
    }
    table.setRowSelectionInterval(0, 0);
    mtm.fireTableDataChanged();
  }

  public void enableActions() {

    distanceAction.setEnabled(true);
    angleAction.setEnabled(true);
    dihedralAction.setEnabled(true);
    ddistanceAction.setEnabled(true);
    dangleAction.setEnabled(true);
    ddihedralAction.setEnabled(true);
  }

  public void disableActions() {

    distanceAction.setEnabled(false);
    angleAction.setEnabled(false);
    dihedralAction.setEnabled(false);
    ddistanceAction.setEnabled(false);
    dangleAction.setEnabled(false);
    ddihedralAction.setEnabled(false);
  }

  class DistanceAction extends AbstractAction {

    public DistanceAction() {
      super("distance");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = ADD;
      measure = DISTANCE;
      initialize();
    }
  }

  class AngleAction extends AbstractAction {

    public AngleAction() {
      super("angle");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = ADD;
      measure = ANGLE;
      initialize();
    }
  }

  class DihedralAction extends AbstractAction {

    public DihedralAction() {
      super("dihedral");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = ADD;
      measure = DIHEDRAL;
      initialize();
    }
  }

  class DDistanceAction extends AbstractAction {

    public DDistanceAction() {
      super("ddistance");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = DELETE;
      measure = DISTANCE;
      initialize();
    }
  }

  class DAngleAction extends AbstractAction {

    public DAngleAction() {
      super("dangle");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = DELETE;
      measure = ANGLE;
      initialize();
    }
  }

  class DDihedralAction extends AbstractAction {

    public DDihedralAction() {
      super("ddihedral");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      action = DELETE;
      measure = DIHEDRAL;
      initialize();
      show();
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      distanceAction, angleAction, dihedralAction, ddistanceAction,
      dangleAction, ddihedralAction
    };
    return defaultActions;
  }

  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  class MeasureWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }


}

