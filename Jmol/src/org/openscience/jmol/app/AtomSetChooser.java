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
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.swing.tree.*;
import java.util.Properties;
import java.util.Enumeration;

/**
 * A JDialog that allows for choosing an Atomset to view.
 * 
 * @author rkanters
 */
public class AtomSetChooser extends JDialog
  implements TreeSelectionListener, PropertyChangeListener,
  ActionListener {
  
  private JTextArea propertiesPane;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JButton firstFrequencyButton;
  private JButton nextFrequencyButton;
  private JButton previousFrequencyButton;
  
  private int currentAtomSetIndex; // should really be determined from the viewer...
   
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

    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("No AtomSets");
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
    container.add(splitPane,BorderLayout.CENTER);

    splitPane.setTopComponent(treeView);
    splitPane.setBottomComponent(propertiesView);
   
    // set the dimensions of the views
    treeView.setMinimumSize(new Dimension(100, 50));
    propertiesView.setMinimumSize(new Dimension(100,0));
    splitPane.setDividerLocation(120);
    splitPane.setPreferredSize(new Dimension(300,200));
    
    // now we are ready to add the split frame
 
    JPanel buttonPanel = new JPanel();
    container.add(buttonPanel, BorderLayout.SOUTH);
    buttonPanel.add(new JLabel("Frequencies:"));
    
    firstFrequencyButton = new JButton("First");
    firstFrequencyButton.addActionListener(this);
    buttonPanel.add(firstFrequencyButton);
    
    nextFrequencyButton = new JButton("Next");
    nextFrequencyButton.addActionListener(this);
    buttonPanel.add(nextFrequencyButton);
    
    previousFrequencyButton = new JButton("Previous");
	previousFrequencyButton.addActionListener(this);
    buttonPanel.add(previousFrequencyButton);
 
  }
  
  public void actionPerformed (ActionEvent e) {
    Object source = e.getSource();
    if (source == firstFrequencyButton) {
      findFrequency(0,1);
    } else if (source == previousFrequencyButton) {
      findFrequency(currentAtomSetIndex-1,-1);
    } else if (source == nextFrequencyButton) {
      findFrequency(currentAtomSetIndex+1,1);
    }
  }
  
  
  /**
   * Has the viewer show a particular frame with frequencies
   * if it can be found.
   * @param index Starting index where to start looking for frequencies
   * @param increment Increment value for how to go through the list
   */
  public void findFrequency(int index, int increment) {
    int maxIndex = viewer.getModelCount();
    boolean foundFrequency = false;
    
    // search till get to either end of found a frequency
    while (index >= 0 && index < maxIndex 
        && !(foundFrequency=viewer.modelHasVibrationVectors(index))) {
      index+=increment;
    }
    
    if (foundFrequency) {
      viewer.evalStringQuiet("vibration on; frame "+viewer.getModelNumber(index));
      currentAtomSetIndex = index;
      showProperties(viewer.getModelProperties(index));
    } else {
      System.out.println("Frequency not found.");
    }
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
    
    if (node ==  null) return;
    
    // not the prettiest solution to the problem that a starting out
    // tree has a root leaf that is not an AtomSet, so the cast
    // goes wrong...
    try {
    	  if (node.isLeaf()) {
    	  	int atomSetIndex = ((AtomSet) node).getAtomSetIndex();
    	  	currentAtomSetIndex = atomSetIndex; // u
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
    catch (Exception exception) {}
  }
  
  /**
   * Shows the properties in the propertiesPane of the
   * AtomSetChooser window
   * @param properties Properties to be shown.
   */
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
   * <p>Since currently the <code>AtomSetCollectionProperties</code>
   * (or <code>ModelSetProperties</code>) are not returned, the
   * system's path.separator and .PATH key are used to determine
   * the which property should be decoded and how.
   */
  private void createTreeModel() {
  	// FIXME when viewer.getModelSetProperties indeed returns the
    // properties, initialize key and separator back to null
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
      // make a flat hierarchy if no key or separator are known
      for (int atomSetIndex = 0, count = viewer.getModelCount();
           atomSetIndex < count; ++atomSetIndex) {
        root.add(new AtomSet(atomSetIndex,
                 viewer.getModelName(atomSetIndex)));
      }
    } else {
    	  for (int atomSetIndex = 0, count = viewer.getModelCount();
    	       atomSetIndex < count; ++atomSetIndex) {
        DefaultMutableTreeNode current = root;
        String path = viewer.getModelProperty(atomSetIndex,key);
        // if the path is not null we need to find out where to add a leaf
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
            	 current = child; // follow the found folder
            } else {
            	 // the 'folder' was not found: we need to add it
              DefaultMutableTreeNode newFolder = 
              	new DefaultMutableTreeNode(lookForFolder);
              current.add(newFolder);
              current = newFolder; // follow the new folder
            }
          }
        }
        // current is the folder where the AtomSet is to be added
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
      createTreeModel(); // all I need to do is to recreate the tree model
    }
  }
}
