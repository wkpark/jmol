/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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

import org.jmol.viewer.*;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class AtomSetChooser extends JDialog
                           implements TreeSelectionListener {
  
  private JEditorPane propertiesPane;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JButton recreateButton;
  
  public AtomSetChooser(JmolViewer viewer, JFrame frame) {
    super(frame,"AtomSetChooser", false);
    this.viewer = viewer;
    layoutWindow(getContentPane());
    setSize(500,400);
    setLocationRelativeTo(frame);
  }
  
  void layoutWindow(Container container) {
    // setup the container like the TreeDemo.java from the Java Tutorial
    container.setLayout(new BorderLayout());

    // create the root for the treeModel only, later this gets set
    // to the tree based on the frame.
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("File");
    treeModel = new DefaultTreeModel(rootNode);
    tree = new JTree(treeModel);
    // only allow single selection (may want to change this later?)
    tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);
    
    // create the scroll pane and add the tree to it.
    JScrollPane treeView = new JScrollPane(tree);
    
    // create the properties pane.
    propertiesPane = new JEditorPane();
    propertiesPane.setEditable(false);
    JScrollPane propertiesView = new JScrollPane(propertiesPane);
    
    // create the split pane with the treeView and propertiesView
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setTopComponent(treeView);
    splitPane.setBottomComponent(propertiesView);
   
    // set the dimensions of the views
    Dimension minimumSize = new Dimension(100, 50);
    treeView.setMinimumSize(minimumSize);
    propertiesView.setMinimumSize(minimumSize);
    splitPane.setDividerLocation(100);
    splitPane.setPreferredSize(new Dimension(500,300));
    
    // now we are ready to add the split frame
    container.add(splitPane,BorderLayout.CENTER);
    
    JPanel buttonPanel = new JPanel();
    container.add(buttonPanel, BorderLayout.SOUTH);
    
    recreateButton = new JButton("Recreate");
    recreateButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          createTreeModel();
        }
      }
    );
    buttonPanel.add(recreateButton);
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
    
    if (node ==  null) return;
    
    if (node.isLeaf()) {
      int atomSetIndex = ((AtomSet) node).getAtomSetIndex();
      int atomSetNumber = viewer.getModelNumber(atomSetIndex);
      viewer.evalString("frame " + atomSetNumber);      
      // Currently I don't see how I can get a hold of the properties that
      // were read.
      propertiesPane.setText("Properties to be shown later...");
    } else { // selected brach
      propertiesPane.setText("Clicked on a branch");
    }
  }
  
  /**
   * Creates the treeModel of the AtomSets the ModelManager knows about.
   *
   * <p>Since we can not get a direct DefaultMutableTreeNode from the
   * adapter, we'll have to build it ourselves, which currently is a
   * completely flat tree.
   * <p>The construction of the tree should probably really be in the
   * FrameManager, so it is automatically done when the file is loaded
   * and only needs to be done once. Or it could be added to the ModelManager's
   * buildFrame method.
   */
  public void createTreeModel() {
    DefaultMutableTreeNode root =
      new DefaultMutableTreeNode(viewer.getModelSetName());
    // flat: add every AtomSet to the root
    for (int atomSetIndex = 0, count = viewer.getModelCount();
         atomSetIndex < count; ++atomSetIndex) {
      root.add(new AtomSet(atomSetIndex, viewer.getModelName(atomSetIndex))
               );
    }
    treeModel.setRoot(root);
    treeModel.reload();
  }
  
  /**
   * Objects in the AtomSetChooser tree
   */
  private class AtomSet extends DefaultMutableTreeNode {
    /**
     * The index of that AtomSet
     */
    private int atomSetIndex;
    /**
     * The name of the AtomSet
     */
    private String atomSetName;
  
    public AtomSet(int atomSetIndex, String atomSetName) {
      this.atomSetIndex = atomSetIndex;
      this.atomSetName = atomSetName;
    }
    
    public int getAtomSetIndex() {
      return atomSetIndex;
    }
    
    public String toString() {
      return atomSetName;
    }
    
  }
  
}
