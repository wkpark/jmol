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
import org.jmol.api.AtomSet;

import java.awt.BorderLayout;
import java.awt.Container;
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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class AtomSetChooser extends JDialog
                           implements TreeSelectionListener {
  
  private JEditorPane propertiesPane;
  private JTree tree;
  private DefaultMutableTreeNode rootNode;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JButton refreshButton;
  
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

    rootNode = new DefaultMutableTreeNode("File");
    treeModel = new DefaultTreeModel(rootNode);
    tree = new JTree(treeModel);
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
   
    container.add(splitPane,BorderLayout.CENTER);
    
    JPanel buttonPanel = new JPanel();
    container.add(buttonPanel, BorderLayout.SOUTH);
    
    refreshButton = new JButton("Refresh");
    refreshButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          refreshTreeModel();
        }
      }
    );
    buttonPanel.add(refreshButton);

    // Now refresh the tree
    refreshTreeModel();
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
    
    if (node ==  null) return;
    
    Object nodeObject = node.getUserObject();
    if (node.isLeaf()) {
      Object clientFile = viewer.modelManager.getClientFile();
      AtomSet atomSet  = (AtomSet) nodeObject;
// HELP I don't know how to get the 'clientFile' in order to get the atomSetNumber for the frame to show...
//      so I added a getClientFile() and clientField field to the ModelManager
      int atomSetIndex = atomSet.getAtomSetIndex();
      int atomSetNumber = viewer.modelAdapter.getAtomSetNumber(clientFile,atomSetIndex);
      viewer.evalString("frame " + atomSetNumber);      
      propertiesPane.setText("Properties to be shown later...");
    } else { // selected brach
      propertiesPane.setText("Clicked on a branch");
    }
  }
  
  /**
   * Gets the AtomSetCollectionTree from the modelAdapter.
   */
  void refreshTreeModel() {
    Object clientFile = viewer.modelManager.getClientFile();
    if (clientFile != null) {
      // set the new root of the tree
      treeModel.setRoot(viewer.modelAdapter.getAtomSetCollectionTree(clientFile));
      treeModel.reload();
      propertiesPane.setText("Tree refreshed.");
    } else {
      propertiesPane.setText("No file opened.");
    }
  }
  
}