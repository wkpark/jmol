
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

import jas.hist.ScatterPlotSource;
import jas.hist.ScatterEnumeration;
import jas.hist.JASHistData;
import jas.hist.JASHist;
import jas.hist.DataSource;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.Enumeration;

public class PropertyGraph extends JDialog implements ActionListener {

  /**
   * Creates a dialog.
   *
   * @param f the parent frame
   */
  public PropertyGraph(JFrame f) {

    super(f, "Property Graph", false);

    gl = new GridLayout(1, 0);
    panel = new JPanel(gl);

    setContentPane(panel);
    setSize(500, 500);
    addWindowListener(new GraphWindowListener());
    pack();
    centerDialog();
  }

  /**
   * Set ChemFile from which the Frame Properties will be loaded.
   * If no Frame Properties are found, the PropertyGraph action will
   * be disabled; otherwise, it will be enabled.
   *
   * @param inputFile the ChemFile containing potentially graphable data
   */
  public void setChemFile(ChemFile inputFile) {

    System.err.println("Checking for viewable properties...");
    if (isVisible()) {
      setVisible(false);
    }
    restoreConflictingActions();
    hasGraphableProperties = false;
    GPs = new Vector();
    this.inputFile = inputFile;

    // Check for graphable properties

    Vector filePL = inputFile.getFramePropertyList();

    for (int j = 0; j < filePL.size(); j++) {

      System.out.println(filePL.elementAt(j));

      if (GPs.indexOf(filePL.elementAt(j)) < 0) {

        System.out.println("Found one");
        if (filePL.elementAt(j).equals("Energy")) {
          GPs.addElement(filePL.elementAt(j));
          hasGraphableProperties = true;
        }
      }

    }

    if (hasGraphableProperties) {
      graphAction.setEnabled(true);
    } else {
      graphAction.setEnabled(false);
    }
  }

  public void findData() {

    if (hasGraphableProperties) {

      int nGraphs = GPs.size();
      int nPoints = inputFile.getNumberFrames();

      gl.setRows(nGraphs);

      double[][] data = new double[nGraphs][nPoints];
      String[] titles = new String[nGraphs];
      JASHist[] graphs = new JASHist[nGraphs];

      for (int j = 0; j < nGraphs; j++) {
        String desc = (String) GPs.elementAt(j);

        titles[j] = new String(desc);
        graphs[j] = new JASHist();

        for (int i = 0; i < nPoints; i++) {
          Vector fp = inputFile.getFrame(i).getFrameProperties();
          Enumeration ef = fp.elements();
          while (ef.hasMoreElements()) {

            PhysicalProperty pf = (PhysicalProperty) ef.nextElement();
            if (pf.getDescriptor().equals(desc)) {
              data[j][i] = ((Double) pf.getProperty()).doubleValue();
            }
          }
        }
      }
      for (int j = 0; j < nGraphs; j++) {
        graphs[j].addData(new ArrayDataSource(data[j], titles[j])).show(true);
        panel.add(graphs[j]);
      }
    }
  }

  /**
   * Shows or hides this component depending on the value of
   * parameter b.
   *
   * @param b If true, shows this component; otherwise, hides
   *   this component.
   */
  public void setVisible(boolean b) {

    if (b) {
      graphAction.setEnabled(false);
      disableConflictingActions();
    } else {
      graphAction.setEnabled(true);
      restoreConflictingActions();
    }
    super.setVisible(b);
  }

  /**
   * Returns the preferred size of the Graph dialog.
   *
   * @return the Dimension preferred by the dialog
   */
  public Dimension getPreferredSize() {
    return new Dimension(500, 500);
  }

  public void actionPerformed(ActionEvent evt) {
  }

  /**
   * Centers the dialog on the screen.
   */
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

  /**
   * The action used to activate this dialog.
   */
  class GraphAction extends AbstractAction {

    /**
     * Create the action.
     */
    public GraphAction() {
      super("graph");
      this.setEnabled(false);
    }

    /**
     * Shows the Property Graph dialog when an action occurs.
     */
    public void actionPerformed(ActionEvent e) {
      findData();
      setVisible(true);
    }
  }

  /**
   * Returns the actions available from the Property Graph dialog.
   *
   * @return the actions for the PropertyGraph dialog
   */
  public Action[] getActions() {
    return new Action[] {
      graphAction
    };
  }

  /**
   * Gets the Vibrate action identified by the string given.
   *
   * @param cmd key for the desired action
   * @return the action associated with cmd, or null if not found
   */
  public Action getAction(String cmd) {
    if (cmd.equals(graphAction.getValue(Action.NAME))) {
      return graphAction;
    }
    return null;
  }

  /**
   * Add a action to be disabled when this dialog is shown.
   *
   * @param a1  a conflicting action
   */
  public void addConflictingAction(Action a1) {
    conflictingActions.addElement(a1);
  }

  /**
   * Disables conflicting actions. Saves action enabled
   * states for restoration.
   */
  private void disableConflictingActions() {

    actionStates = new Vector();
    Enumeration iter = conflictingActions.elements();
    while (iter.hasMoreElements()) {
      Action a1 = (Action) iter.nextElement();
      actionStates.addElement(new Boolean(a1.isEnabled()));
      a1.setEnabled(false);
    }
  }

  /**
   * Restores conflicting actions to saved state.
   */
  private void restoreConflictingActions() {

    if (actionStates != null) {
      Enumeration iter = conflictingActions.elements();
      Enumeration stateIter = actionStates.elements();
      while (iter.hasMoreElements() && stateIter.hasMoreElements()) {
        Action a1 = (Action) iter.nextElement();
        boolean state = ((Boolean) stateIter.nextElement()).booleanValue();
        a1.setEnabled(state);
      }
      actionStates = null;
    }
  }

  /**
   * Listener for responding to dialog window events.
   */
  class GraphWindowListener extends WindowAdapter {

    /**
     * Closes the dialog when window closing event occurs.
     */
    public void windowClosing(WindowEvent e) {
      setVisible(false);
    }
  }


  private JPanel panel;
  private GridLayout gl;

  /**
   * Does the dialog have any vibration data.
   */
  private boolean hasGraphableProperties;

  /*
   * The Vector containing the list of Graphable Properties.
   */
  private Vector GPs = new Vector();

  /**
   * The ChemFile containing frames with graphable data.
   */
  private ChemFile inputFile;

  /**
   * The action used to activate this dialog.
   */
  private GraphAction graphAction = new GraphAction();

  /**
   * List of actions which will interfere with the operation of this
   * dialog.
   */
  private Vector conflictingActions = new Vector();

  /**
   * Enabled states of conficting actions which are disabled
   * during the operation of this dialog. Used to restore states
   * when the dialog is closed.
   */
  private Vector actionStates = null;

}

class ArrayDataSource implements ScatterPlotSource {

  ArrayDataSource(double[] xData, double[] yData, String title) {
    this.xData = xData;
    this.yData = yData;
    this.title = title;
    init();
  }

  ArrayDataSource(double[] yData, String title) {

    xData = new double[yData.length];
    for (int i = 0; i < yData.length; i++) {
      xData[i] = (new Double(i)).doubleValue();
    }
    this.yData = yData;
    this.title = title;
    init();
  }

  private void init() {

    xAxisType = DOUBLE;
    yAxisType = DOUBLE;
    xmin = xData[0];
    xmax = xmin;
    ymin = yData[0];
    ymax = ymin;
    for (int i = 1; i < xData.length; i++) {
      if (xData[i] < xmin) {
        xmin = xData[i];
      }
      if (xData[i] > xmax) {
        xmax = xData[i];
      }
      if (yData[i] < ymin) {
        ymin = yData[i];
      }
      if (yData[i] > ymax) {
        ymax = yData[i];
      }
    }
  }

  public double getXMin() {
    return xmin;
  }

  public double getXMax() {
    return xmax;
  }

  public double getYMin() {
    return ymin;
  }

  public double getYMax() {
    return ymax;
  }

  public int getXAxisType() {
    return xAxisType;
  }

  public int getYAxisType() {
    return yAxisType;
  }

  public java.lang.String getTitle() {
    if (title != null) {
      return title;
    }
    return null;
  }

  public ScatterEnumeration startEnumeration(double xMin, double xMax,
      double yMin, double yMax) {
    return new FixedEnumeration(xData, yData, xMin, xMax, yMin, yMax);
  }

  public ScatterEnumeration startEnumeration() {
    return new FixedEnumeration(xData, yData);
  }

  private double xmin;
  private double xmax;
  private double ymin;
  private double ymax;
  private int xAxisType;
  private int yAxisType;
  private String title;
  private double[] xData;
  private double[] yData;

  private class FixedEnumeration implements ScatterEnumeration {

    public FixedEnumeration(double[] xdata, double[] ydata) {
      this.xdata = xdata;
      this.ydata = ydata;
      selectAll = true;
    }

    public FixedEnumeration(double[] xdata, double[] ydata, double xMin,
        double xMax, double yMin, double yMax) {

      this.xdata = xdata;
      this.ydata = ydata;
      selectAll = false;
      m_xmin = xMin;
      m_xmax = xMax;
      m_ymin = yMin;
      m_ymax = yMax;
    }

    public boolean getNextPoint(double[] a) {

      if (selectAll) {
        if (pos < (xdata.length - 1)) {
          a[0] = xdata[pos];
          a[1] = ydata[pos++];
          return true;
        } else {
          return false;
        }
      } else {
        while (!((xdata[pos] >= m_xmin) && (xdata[pos] <= m_xmax)
            && (ydata[pos] >= m_ymin) && (ydata[pos] <= m_ymax))) {

          //skip points that don't satisfy the conditions
          pos++;
          if (pos > (xdata.length - 1)) {

            // if no more points left (don't want to overstep 
            // bounds)
            return false;
          }
        }

        // okay, if we're here the point satisfies the min 
        // and max conditions
        a[0] = xdata[pos];
        a[1] = ydata[pos++];
        return true;
      }
    }

    public void resetEndPoint() {

      //what does this do?
      //(it seems like Gauss2D.java doesn't know what to do here either)
    }

    public void restart() {
      pos = 0;
    }

    private int pos = 0;
    private double[] xdata;
    private double[] ydata;
    private boolean selectAll;
    private double m_xmin;
    private double m_xmax;
    private double m_ymin;
    private double m_ymax;
  }
}
