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

import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

import java.util.Properties;
import java.util.Enumeration;

/**
 * A JDialog that allows for choosing an Atomset to view.
 * 
 * @author Ren&eacute; Kanters, University of Richmond
 */
/**
 * @author rkanters
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AtomSetChooser extends JDialog
  implements TreeSelectionListener, PropertyChangeListener,
  ActionListener, ChangeListener {

  private JTextArea propertiesTextArea;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JSlider selectSlider;
  private JLabel infoLabel = new JLabel(" ");
  private JSlider amplitudeSlider;
  private JSlider periodSlider;
  
  // Strings for the commands of the buttons and the determination
  // of the tooltips and images associated with them
  static final String REWIND="rewind";
  static final String PREVIOUS="prev";
  static final String PLAY="play";
  static final String PAUSE="pause";
  static final String NEXT="next";
  static final String FF="ff";
  
  private int atomSetCollectionIndexes[];
  
  public AtomSetChooser(JmolViewer viewer, JFrame frame) {
    super(frame,"AtomSetChooser", false);
    this.viewer = viewer;
    
    // initialize the treeModel
    treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("No AtomSets"));

    layoutWindow(getContentPane());
    pack();
    setLocationRelativeTo(frame);
  }
  
  private void layoutWindow(Container container) {
     
    // user a BoxLayout to do a vertical arrangement
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    
    // the top panel with the tree and properties
    JPanel treePanel = new JPanel();
    treePanel.setLayout(new BorderLayout());
    tree = new JTree(treeModel);
    tree.setVisibleRowCount(5);
    // only allow single selection (may want to change this later?)
    tree.getSelectionModel().setSelectionMode(
        TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.addTreeSelectionListener(this);
    treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
    
    // create the properties panel.
    JPanel propertiesPanel = new JPanel();
    propertiesPanel.setLayout(new BorderLayout());
    propertiesPanel.setBorder(new TitledBorder("Properties"));
    propertiesTextArea = new JTextArea();
    propertiesTextArea.setEditable(false);
    propertiesPanel.add(new JScrollPane(propertiesTextArea), BorderLayout.CENTER);
        
    // create the split pane with the treeView and propertiesView
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
    splitPane.setTopComponent(treePanel);
    splitPane.setBottomComponent(propertiesPanel);
    splitPane.setResizeWeight(1.0);
    
    JPanel astPanel = new JPanel();
    astPanel.setLayout(new BorderLayout());
    astPanel.setBorder(new TitledBorder("Atom Set Tree"));
    astPanel.add(splitPane, BorderLayout.CENTER);
    container.add(astPanel);
    
    // The collection chooser/controller/feedback area
    JPanel collectionPanel = new JPanel();
    collectionPanel.setLayout(new BoxLayout(collectionPanel, BoxLayout.Y_AXIS));
    collectionPanel.setBorder(new TitledBorder("Collection"));
    
    JPanel cpsPanel = new JPanel();
    cpsPanel.setLayout(new BorderLayout());
    cpsPanel.setBorder(new TitledBorder("Select"));
    selectSlider = new JSlider();
    selectSlider.addChangeListener(this);
    cpsPanel.add(selectSlider); // no need for CENTER...
    collectionPanel.add(cpsPanel);
    
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel.setBorder(new TitledBorder("Info"));
    infoPanel.add(infoLabel);
    collectionPanel.add(infoPanel);
    
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
    controlPanel.setBorder(new TitledBorder("Controller"));
    String buttons[] = {REWIND,PREVIOUS,PLAY,PAUSE,NEXT,FF};
    Insets inset = new Insets(1,1,1,1);
    for (int i=buttons.length, idx=0; --i>=0; idx++) {
      String action = buttons[idx];
      JButton btn = new JButton(
          JmolResourceHandler.getInstance().getIconX(
              "AtomSetChooser."+action+"Image")
      );
      btn.setMargin(inset);
      btn.setToolTipText(
          JmolResourceHandler.getInstance().getStringX(
              "AtomSetChooser."+action+"Tooltip")
      );
      btn.setActionCommand(action);
      btn.addActionListener(this);
      controlPanel.add(btn);
    }
    controlPanel.add(Box.createHorizontalGlue());
    collectionPanel.add(controlPanel);
    container.add(collectionPanel);
    
    JPanel vectorPanel = new JPanel();
    vectorPanel.setLayout(new BoxLayout(vectorPanel, BoxLayout.Y_AXIS));
    vectorPanel.setBorder(new TitledBorder("Vectors"));
    JPanel checkBoxPanel= new JPanel();
    JCheckBox vibrateCheckBox = new JCheckBox("Vibrate");
    vibrateCheckBox.setActionCommand("vibrate");
    checkBoxPanel.add(vibrateCheckBox);
    JCheckBox gradientCheckBox = new JCheckBox("Gradient");
    gradientCheckBox.setActionCommand("gradient");
    checkBoxPanel.add(gradientCheckBox);
    checkBoxPanel.add(Box.createHorizontalGlue());
    vectorPanel.add(checkBoxPanel);
    
    JPanel amplitudePanel = new JPanel();
    amplitudePanel.setLayout(new BorderLayout());
    amplitudePanel.setBorder(new TitledBorder("Amplitude"));
    amplitudeSlider = new JSlider();
    amplitudePanel.add(amplitudeSlider);
    vectorPanel.add(amplitudePanel);

    JPanel periodPanel = new JPanel();
    periodPanel.setLayout(new BorderLayout());
    periodPanel.setBorder(new TitledBorder("Prediod"));
    periodSlider = new JSlider();
    periodPanel.add(periodSlider);
    vectorPanel.add(periodPanel);
    container.add(vectorPanel);
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
    try {
        if (node.isLeaf()) {
          selectAtomSet((AtomSet) node);
        } else // selected branch
          selectAtomSetCollection(node);
    }
    catch (Exception exception) {
      System.out.println(exception.getMessage());
      exception.printStackTrace();
      }
  }
  
  public void selectAtomSet(AtomSet node) {
    setAtomSetCollectionIndexes((DefaultMutableTreeNode) node.getParent());
    showAtomSet(node.getAtomSetIndex());
  }
  
  public void selectAtomSetCollection(DefaultMutableTreeNode node) {
    setAtomSetCollectionIndexes(node);
    showAtomSet(atomSetCollectionIndexes[0]);
    if (node == treeModel.getRoot())
      showProperties(viewer.getModelSetProperties());
    else
      propertiesTextArea.setText("Collection has " +
          node.getLeafCount() + " AtomSets");
  }

  /**
   * Show atom set in viewer, info and properties
   * @param atomSetIndex Index of atom set to be shown
   */
  public void showAtomSet(int atomSetIndex) {
    viewer.setDisplayModelIndex(atomSetIndex);
    infoLabel.setText(viewer.getModelName(atomSetIndex));
    showProperties(viewer.getModelProperties(atomSetIndex));
  }
 
  public void setAtomSetCollectionIndexes(DefaultMutableTreeNode node) {
    int atomSetCount = node.getLeafCount();
    atomSetCollectionIndexes = new int[atomSetCount];
    Enumeration e = node.depthFirstEnumeration();
    int idx=0;
    while (e.hasMoreElements()) {
      node = (DefaultMutableTreeNode) e.nextElement();
      if (node.isLeaf())
        atomSetCollectionIndexes[idx++]= ((AtomSet) node).getAtomSetIndex();
    }
    System.out.print("Indexes: ");
    for (int i=0; i<atomSetCount; i++)
      System.out.print(" "+atomSetCollectionIndexes[i]);
    System.out.println();
  }
  
  // this is for the frequencies, but should be for the list of atomsets
  // that are currently in the selection of the tree
  public void actionPerformed (ActionEvent e) {
    String cmd = e.getActionCommand();
    if (REWIND.equals(cmd)) {
      findFrequency(0,1);
    } else if (PREVIOUS.equals(cmd)) {
      findFrequency(viewer.getDisplayModelIndex()-1,-1);
    } else if (NEXT.equals(cmd)) {
      findFrequency(viewer.getDisplayModelIndex()+1,1);
    } else if (FF.equals(cmd)) {
      findFrequency(viewer.getModelCount()-1,-1);
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
      viewer.evalStringQuiet("vibration on;");
      viewer.setDisplayModelIndex(index);
      showProperties(viewer.getModelProperties(index));
    } else {
      System.out.println("Frequency not found.");
    }
  }
 
  // TODO implement the slider state changes
  public void stateChanged(ChangeEvent e) {
  }
  
  /**
   * Shows the properties in the propertiesPane of the
   * AtomSetChooser window
   * @param properties Properties to be shown.
   */
  public void showProperties(Properties properties) {
    boolean needLF = false;
    propertiesTextArea.setText("");
    if (properties != null) {
      Enumeration e = properties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String)e.nextElement();
        propertiesTextArea.append((needLF?"\n ":" ") 
            + propertyName + "=" + properties.getProperty(propertyName));
        needLF = true;
      }
    }
  }

  /**
   * Creates the treeModel of the AtomSets available in the JmolViewer
   */
  private void createTreeModel() {
    String key=null;
    String separator=null;
    DefaultMutableTreeNode root =
      new DefaultMutableTreeNode(viewer.getModelSetName());
      
    // first determine whether we have a PATH_KEY in the modelSetProperties
    Properties modelSetProperties = viewer.getModelSetProperties();
    if (modelSetProperties != null) {
      key = modelSetProperties.getProperty("PATH_KEY");
      separator = modelSetProperties.getProperty("PATH_SEPARATOR");
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
