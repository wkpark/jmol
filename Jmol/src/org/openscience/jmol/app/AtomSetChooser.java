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
import java.beans.*;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
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
import java.util.Properties;
import java.util.Enumeration;

public class AtomSetChooser extends JDialog
  implements TreeSelectionListener, PropertyChangeListener {
  
  private JTextArea propertiesPane;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  
  public AtomSetChooser(JmolViewer viewer, JFrame frame) {
    super(frame,"AtomSetChooser", false);
    this.viewer = viewer;
    layoutWindow(getContentPane());
    setSize(300,200);
    setLocationRelativeTo(frame);
  }
  
  private void layoutWindow(Container container) {
    // setup the container like the TreeDemo.java from the Java Tutorial
    container.setLayout(new BorderLayout());

    // create the root for the treeModel only, later this gets set
    // to the tree based on the frame.
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("");
    treeModel = new DefaultTreeModel(rootNode);
    tree = new JTree(treeModel);
    // only allow single selection (may want to change this later?)
    tree.getSelectionModel().setSelectionMode
            (TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);
    
    // create the scroll pane and add the tree to it.
    JScrollPane treeView = new JScrollPane(tree);
    
    // create the properties pane.
    propertiesPane = new JTextArea();
    propertiesPane.setEditable(false);
    JScrollPane propertiesView = new JScrollPane(propertiesPane);
    
    // create the split pane with the treeView and propertiesView
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setTopComponent(treeView);
    splitPane.setBottomComponent(propertiesView);
   
    // set the dimensions of the views
    treeView.setMinimumSize(new Dimension(100, 50));
    splitPane.setDividerLocation(120);
    splitPane.setPreferredSize(new Dimension(300,200));
    
    // now we are ready to add the split frame
    container.add(splitPane,BorderLayout.CENTER);
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
    
    if (node ==  null) return;
    
    if (node.isLeaf()) {
      int atomSetIndex = ((AtomSet) node).getAtomSetIndex();
      int atomSetNumber = viewer.getModelNumber(atomSetIndex);
      // show the model in the viewer
      viewer.evalStringQuiet("frame " + atomSetNumber);
      // show the properties in the properties pane
      showProperties(viewer.getModelProperties(atomSetIndex));
    } else { // selected branch
      propertiesPane.setText("Collection has " +
        node.getLeafCount() + " AtomSets");
    }
  }
  
  public void showProperties(Properties properties) {
    propertiesPane.setText("Properties:");
    if (properties == null) {
       propertiesPane.append(" null");
    } else {
      Enumeration e = properties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        propertiesPane.append("\n " + propertyName + "=" +
                   properties.getProperty(propertyName));
      }
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
  private void createTreeModel() {
    String key=".PATH";
    String separator=System.getProperty("path.separator");
    DefaultMutableTreeNode root =
      new DefaultMutableTreeNode(viewer.getModelSetName());
      
    // first determine whether we have a PATH_KEY in the modelSetProperties
    Properties modelSetProperties = viewer.getModelSetProperties();
    if (modelSetProperties != null) {
      key = modelSetProperties.getProperty("PATH_KEY");
      separator = modelSetProperties.getProperty("PATH_SEPARATOR");
    } else {
      System.out.println("No PATH info found in the atomSetCollectionProperties "    
         + modelSetProperties);
    }
    if (key == null || separator == null) {
      // make a flat hierarchy currently won't happen since I initialize
      // key and separator with the values I used in the GaussianReader
      // FIXME when viewer.getModelSetProperties indeed returns the
      // properties, I can initialize key and separator back to null
      for (int atomSetIndex = 0, count = viewer.getModelCount();
           atomSetIndex < count; ++atomSetIndex) {
        root.add(new AtomSet(atomSetIndex,
                 viewer.getModelName(atomSetIndex)));
      }
    } else {
    // flat: add every AtomSet to the root
      for (int atomSetIndex = 0, count = viewer.getModelCount();
           atomSetIndex < count; ++atomSetIndex) {
        DefaultMutableTreeNode current = root;
        String path = viewer.getModelProperty(atomSetIndex,key);
        if (path != null) {
          DefaultMutableTreeNode child = null;
          String[] folders = path.split(separator);
          for (int i=0, nFolders=folders.length; --nFolders>=0; i++) {
            boolean found = false; // folder is initially not found
            String lookForFolder = folders[i];
            for (int childIndex = current.getChildCount(); --childIndex>=0;) {
              child = (DefaultMutableTreeNode) current.getChildAt(childIndex);
              found = lookForFolder.equals(child.toString());
              if (found) break;
            }
            if (found) {
              current = child;
            } else {
              DefaultMutableTreeNode newFolder = 
                new DefaultMutableTreeNode(lookForFolder);
              current.add(newFolder);
              current = newFolder;
            }
          }
        }
        current.add(new AtomSet(atomSetIndex,
                    viewer.getModelName(atomSetIndex)));
      }
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

  ////////////////////////////////////////////////////////////////
  // PropertyChangeListener to receive notification that
  // the underlying AtomSetCollection has changed
  ////////////////////////////////////////////////////////////////

  public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
    String eventName = propertyChangeEvent.getPropertyName();
    if (eventName.equals(Jmol.chemFileProperty)) {
      // Rene, put your things here
      createTreeModel(); // all I need to do is to recreate the tree model
      return;
    }
  }
}
