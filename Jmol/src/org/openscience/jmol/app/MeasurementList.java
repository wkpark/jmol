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

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.render.Angle;
import org.openscience.jmol.render.Distance;
import org.openscience.jmol.render.Dihedral;

import java.io.File;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.JScrollPane;
import java.util.Vector;
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
import java.util.Enumeration;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.BoxLayout;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

public class MeasurementList extends JDialog {

  private DisplayControl control;
  protected DefaultMutableTreeNode top;
  protected ListNode distances, angles, dihedrals;
  protected DefaultTreeModel treeModel;
  protected JTree tree;
  protected JButton xButton = new JButton(
    JmolResourceHandler.getInstance().translate("Delete Measurement"));

  private Vector distanceList;
  private Vector angleList;
  private Vector dihedralList;

  // The actions:
  private CDistanceAction cdistanceAction = new CDistanceAction();
  private CAngleAction cangleAction = new CAngleAction();
  private CDihedralAction cdihedralAction = new CDihedralAction();
  private CMeasureAction cmeasureAction = new CMeasureAction();
  private ViewMListAction viewmlistAction = new ViewMListAction();
  private Hashtable commands;

  /**
   * Constructor
   *
   * @param f the parent frame
   * @param dp the DisplayPanel in which the animation will take place
   */
  public MeasurementList(JFrame f, DisplayControl control) {

    super(f, JmolResourceHandler.getInstance()
          .translate("Measurement List"), false);
    this.control = control;

    distanceList = control.getDistanceMeasurements();
    angleList = control.getAngleMeasurements();
    dihedralList = control.getDihedralMeasurements();

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

    top = new DefaultMutableTreeNode(JmolResourceHandler.getInstance()
        .getString("MeasurementList.mLabel"));
    treeModel = new DefaultTreeModel(top);

    distances =
        new ListNode(JmolResourceHandler.getInstance()
          .getString("MeasurementList.distanceLabel"), distanceList);
    angles =
        new ListNode(JmolResourceHandler.getInstance()
          .getString("MeasurementList.angleLabel"), angleList);
    dihedrals =
        new ListNode(JmolResourceHandler.getInstance()
          .getString("MeasurementList.dihedralLabel"), dihedralList);

    treeModel.insertNodeInto(distances, top, top.getChildCount());
    treeModel.insertNodeInto(angles, top, top.getChildCount());
    treeModel.insertNodeInto(dihedrals, top, top.getChildCount());

    tree = new JTree(treeModel);
    tree.setEditable(false);
    tree.getSelectionModel()
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setShowsRootHandles(true);

    //Listen for when the selection changes.
    tree.addTreeSelectionListener(new TreeSelectionListener() {

      public void valueChanged(TreeSelectionEvent e) {

        DefaultMutableTreeNode node =
          (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (node == null) {
          return;
        }

        Object nodeInfo = node.getUserObject();
        System.out.println(nodeInfo.toString());
        if (node.isLeaf()) {
          if (nodeInfo.toString().equalsIgnoreCase("Empty")) {
            xButton.setEnabled(false);
          } else {
            xButton.setEnabled(true);
          }
        } else {
          xButton.setEnabled(false);
        }
      }
    });
    tree.putClientProperty("JTree.lineStyle", "Angled");

    //Create the scroll pane and add the tree to it. 
    JScrollPane treeView = new JScrollPane(tree);
    mPanel.add(treeView, BorderLayout.CENTER);
    container.add(mPanel);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    xButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        DeletePressed();
      }
    });
    buttonPanel.add(xButton);
    xButton.setEnabled(false);
    JButton dismiss = new JButton(JmolResourceHandler.getInstance()
        .translate("Dismiss"));
    dismiss.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    buttonPanel.add(dismiss);
    getRootPane().setDefaultButton(xButton);

    container.add(buttonPanel);

    addWindowListener(new MeasurementListWindowListener());

    getContentPane().add(container);
    pack();
    centerDialog();
  }

  public void updateTree() {

    distances.update();
    angles.update();
    dihedrals.update();
    treeModel.reload(top);
  }

  private void notifyControl() {
    control.refresh();
  }

  public Vector getDistanceList() {
    return distanceList;
  }

  public Vector getAngleList() {
    return angleList;
  }

  public Vector getDihedralList() {
    return dihedralList;
  }

  public void addDistance(int atom1, int atom2) {
    control.defineMeasure(atom1, atom2);
    distances.update();
    treeModel.reload(distances);
  }

  public void addAngle(int atom1, int atom2, int atom3) {
    control.defineMeasure(atom1, atom2, atom3);
    angles.update();
    treeModel.reload(angles);
  }

  public void addDihedral(int atom1, int atom2, int atom3, int atom4) {
    control.defineMeasure(atom1, atom2, atom3, atom4);
    dihedrals.update();
    treeModel.reload(dihedrals);
  }


  public void clear() {

    distanceList.removeAllElements();
    angleList.removeAllElements();
    dihedralList.removeAllElements();
    distances.update();
    angles.update();
    dihedrals.update();
    treeModel.reload(top);
    notifyControl();
  }

  public void clearDistanceList() {

    distanceList.removeAllElements();
    distances.update();
    treeModel.reload(distances);
    notifyControl();
  }

  public void clearAngleList() {

    angleList.removeAllElements();
    angles.update();
    treeModel.reload(angles);
    notifyControl();
  }

  public void clearDihedralList() {

    dihedralList.removeAllElements();
    dihedrals.update();
    treeModel.reload(dihedrals);
    notifyControl();
  }

  public boolean deleteMatchingDistance(int i1, int i2) {

    Enumeration e = distanceList.elements();
    while (e.hasMoreElements()) {
      Distance d = (Distance) e.nextElement();
      if (d.sameAs(i1, i2)) {
        distanceList.removeElement(d);
        distances.update();
        treeModel.reload(distances);
        notifyControl();
        return true;
      }
    }

    // No match found, return a failure.
    return false;
  }

  public boolean deleteMatchingAngle(int i1, int i2, int i3) {

    Enumeration e = angleList.elements();
    while (e.hasMoreElements()) {
      Angle a = (Angle) e.nextElement();
      if (a.sameAs(i1, i2, i3)) {
        angleList.removeElement(a);
        angles.update();
        treeModel.reload(angles);
        notifyControl();
        return true;
      }
    }

    // No match found, return a failure.
    return false;
  }

  public boolean deleteMatchingDihedral(int i1, int i2, int i3, int i4) {

    Enumeration e = dihedralList.elements();
    while (e.hasMoreElements()) {
      Dihedral dh = (Dihedral) e.nextElement();
      if (dh.sameAs(i1, i2, i3, i4)) {
        dihedralList.removeElement(dh);
        dihedrals.update();
        treeModel.reload(dihedrals);
        notifyControl();
        return true;
      }
    }

    // No match found, return a failure.
    return false;
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

  public void DeletePressed() {

    DefaultMutableTreeNode node =
      (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
    if (node == null) {
      return;
    }

    Object nodeInfo = node.getUserObject();
    String mType = nodeInfo.getClass().getName();
    if (mType.endsWith("Distance")) {
      Distance d = (Distance) nodeInfo;
      int[] at = d.getAtomList();
      boolean b = deleteMatchingDistance(at[0], at[1]);
    } else {
      if (mType.endsWith("Angle")) {
        Angle a = (Angle) nodeInfo;
        int[] at = a.getAtomList();
        boolean b = deleteMatchingAngle(at[0], at[1], at[2]);
      } else {
        if (mType.endsWith("Dihedral")) {
          Dihedral dh = (Dihedral) nodeInfo;
          int[] at = dh.getAtomList();
          boolean b = deleteMatchingDihedral(at[0], at[1], at[2], at[3]);
        }
      }
    }
  }

  public void enableActions() {

    cdistanceAction.setEnabled(true);
    cangleAction.setEnabled(true);
    cdihedralAction.setEnabled(true);
    cmeasureAction.setEnabled(true);
    viewmlistAction.setEnabled(true);
  }

  public void disableActions() {

    cdistanceAction.setEnabled(false);
    cangleAction.setEnabled(false);
    cdihedralAction.setEnabled(false);
    cmeasureAction.setEnabled(false);
    viewmlistAction.setEnabled(false);
  }

  class CDistanceAction extends AbstractAction {

    public CDistanceAction() {
      super("cdistance");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      clearDistanceList();
    }
  }

  class CAngleAction extends AbstractAction {

    public CAngleAction() {
      super("cangle");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      clearAngleList();
    }
  }

  class CDihedralAction extends AbstractAction {

    public CDihedralAction() {
      super("cdihedral");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      clearDihedralList();
    }
  }

  class CMeasureAction extends AbstractAction {

    public CMeasureAction() {
      super("cmeasure");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      clearDistanceList();
      clearAngleList();
      clearDihedralList();
    }
  }

  class ViewMListAction extends AbstractAction {

    public ViewMListAction() {
      super("viewmlist");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      updateTree();
      show();
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      cdistanceAction, cangleAction, cdihedralAction, cmeasureAction,
      viewmlistAction
    };
    return defaultActions;
  }

  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  class MeasurementListWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }
}
