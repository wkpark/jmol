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
public class AtomSetChooser extends JDialog
  implements TreeSelectionListener, PropertyChangeListener,
  ActionListener, ChangeListener {

  private static final boolean DEBUG = false;
  
  private JTextArea propertiesTextArea;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JSlider selectSlider;
  private JLabel infoLabel;
  private JSlider scaleSlider;
  private JSlider periodSlider;
  
  // Strings for the commands of the buttons and the determination
  // of the tooltips and images associated with them
  static final String REWIND="rewind";
  static final String PREVIOUS="prev";
  static final String PLAY="play";
  static final String PAUSE="pause";
  static final String NEXT="next";
  static final String FF="ff";
  
  int atomSetCollectionIndexes[];
  /**
   * Precision of the vibration scale slider
   */
  private static final float VIBRATION_SCALE_PRECISION = 0.01f;
  private static final float VIBRATION_SCALE_VALUE = 0.5f;
  private static final float VIBRATION_SCALE_MAX = 1;
  /**
   * Precision of the vibration period slider
   */
  private static final float VIBRATION_PERIOD_PRECISION = 0.001f;
  private static final float VIBRATION_PERIOD_MAX = 1; // in seconds
  private static final float VIBRATION_PERIOD_VALUE = 0.5f;
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
//  container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
  container.setLayout(new BorderLayout());
  
  //////////////////////////////////////////////////////////
  // The tree and properties panel
  // as a split pane in the center of the container
  //////////////////////////////////////////////////////////
   JPanel treePanel = new JPanel();
   treePanel.setLayout(new BorderLayout());
   tree = new JTree(treeModel);
   tree.setVisibleRowCount(5);
   // only allow single selection (may want to change this later?)
   tree.getSelectionModel().setSelectionMode(
       TreeSelectionModel.SINGLE_TREE_SELECTION);
   tree.addTreeSelectionListener(this);
   treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
   // the panel for the properties
   JPanel propertiesPanel = new JPanel();
   propertiesPanel.setLayout(new BorderLayout());
   propertiesPanel.setBorder(new TitledBorder("Properties"));
   propertiesTextArea = new JTextArea();
   propertiesTextArea.setEditable(false);
   propertiesPanel.add(new JScrollPane(propertiesTextArea), BorderLayout.CENTER);
       
   // create the split pane with the treePanel and propertiesPanel
   JPanel astPanel = new JPanel();
   astPanel.setLayout(new BorderLayout());
   astPanel.setBorder(new TitledBorder("Atom Set Tree"));
    
   JSplitPane splitPane = new JSplitPane(
       JSplitPane.VERTICAL_SPLIT, treePanel, propertiesPanel); 
   astPanel.add(splitPane, BorderLayout.CENTER);
   splitPane.setResizeWeight(1.0);
   container.add(astPanel, BorderLayout.CENTER);
  
   //////////////////////////////////////////////////////////
   // The Controller area as south of the container
   //////////////////////////////////////////////////////////
   JPanel controllerPanel = new JPanel();
   controllerPanel.setLayout(new BoxLayout(controllerPanel, BoxLayout.Y_AXIS));
   container.add(controllerPanel, BorderLayout.SOUTH);

   //////////////////////////////////////////////////////////
   // The collection chooser/controller/feedback area
   //////////////////////////////////////////////////////////
   JPanel collectionPanel = new JPanel();
   collectionPanel.setLayout(new BoxLayout(collectionPanel, BoxLayout.Y_AXIS));
   collectionPanel.setBorder(new TitledBorder("Collection"));
   controllerPanel.add(collectionPanel);
   // select slider area
   JPanel cpsPanel = new JPanel();
   cpsPanel.setLayout(new BorderLayout());
   cpsPanel.setBorder(new TitledBorder("Select"));
   selectSlider = new JSlider(0,0,0);
   selectSlider.addChangeListener(this);
   selectSlider.setMajorTickSpacing(5);
   selectSlider.setMinorTickSpacing(1);
   selectSlider.setPaintTicks(true);
   selectSlider.setSnapToTicks(true);
   cpsPanel.add(selectSlider, BorderLayout.SOUTH);
   collectionPanel.add(cpsPanel);
   // info area
   JPanel infoPanel = new JPanel();
   infoPanel.setLayout(new BorderLayout());
   infoPanel.setBorder(new TitledBorder("Info"));
   infoLabel = new JLabel(" ");
   infoPanel.add(infoLabel, BorderLayout.SOUTH);
   collectionPanel.add(infoPanel);
   // VCR-like play controller
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
   
   //////////////////////////////////////////////////////////
   // The vector panel
   //////////////////////////////////////////////////////////
  JPanel vectorPanel = new JPanel();
//   container.add(vectorPanel);
  controllerPanel.add(vectorPanel);
   // fill out the contents of the vectorPanel
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
   // controller for vibrationScale    
   JPanel scalePanel = new JPanel();
   scalePanel.setLayout(new BorderLayout());
   scalePanel.setBorder(new TitledBorder("Scale"));
   int scaleEnd = (int) (VIBRATION_SCALE_MAX/VIBRATION_SCALE_PRECISION);
   scaleSlider = new JSlider(-scaleEnd, scaleEnd,
       (int)(VIBRATION_SCALE_VALUE/VIBRATION_SCALE_PRECISION));
   viewer.setVibrationScale(VIBRATION_SCALE_VALUE);
//   scaleSlider.setMajorTickSpacing(100);
//   scaleSlider.setMinorTickSpacing(50);
   scaleSlider.addChangeListener(this);
   scalePanel.add(scaleSlider);
   vectorPanel.add(scalePanel);
   // controller for the vibrationPeriod
   JPanel periodPanel = new JPanel();
   periodPanel.setLayout(new BorderLayout());
   periodPanel.setBorder(new TitledBorder("Period"));
   periodSlider = new JSlider(0,
       (int)(VIBRATION_PERIOD_MAX/VIBRATION_PERIOD_PRECISION),
       (int)(VIBRATION_PERIOD_VALUE/VIBRATION_PERIOD_PRECISION));
   viewer.setVibrationPeriod(VIBRATION_PERIOD_VALUE);
   periodSlider.addChangeListener(this);
   periodPanel.add(periodSlider);
   vectorPanel.add(periodPanel);
 }
 
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
    tree.getLastSelectedPathComponent();
    if (DEBUG) debug("valueChanged",1);
    if (node == null) {
      if (DEBUG) debug("Done",-1);
      return;
    }
    try {
      if (node.isLeaf()) {
        if (DEBUG) debug("is leaf");
        setAtomSet((AtomSet) node);
      } else { // selected branch
        if (DEBUG) debug("is branch");
        setAtomSetCollection(node, true);
      }
    }
    catch (Exception exception) {
      exception.printStackTrace();
    }
    finally {
      if (DEBUG) debug("Done",-1);
    }
  }
  
  public void setAtomSet(AtomSet node) {
    if (DEBUG) debug("setAtomSet(node:"+node+")",1);
    setAtomSetCollectionIndexes((DefaultMutableTreeNode) node.getParent());
    setAtomSet(node.getAtomSetIndex(), true); // if true: infinite loop
    if (DEBUG) debug("Done",-1);
  }
  
  /**
   * Sets the atomSetCollection to the leafs of the node
   * @param node The node whose leafs needs to be the collection
   * @param bSetAtomSet If true sets the atom set to the first on in the collection
   */
  public void setAtomSetCollection(DefaultMutableTreeNode node, 
      boolean bSetAtomSet) {
    if (DEBUG) debug("setAtomSetCollection("+node+","+bSetAtomSet+")",1);
    setAtomSetCollectionIndexes(node);
    if (bSetAtomSet) setAtomSet(atomSetCollectionIndexes[0], true);
    propertiesTextArea.setText("Collection has " +
          node.getLeafCount() + " AtomSets");
    if (DEBUG) debug("Done",-1);
  }

  /**
   * Show atom set and update dependent views/controllers
   * @param atomSetIndex Index of atom set to be shown
   * @param bSetSelectSlider If true, updates the selectSlider
   */
  public void setAtomSet(int atomSetIndex, boolean bSetSelectSlider) {
    if (DEBUG) debug("setAtomSet("+atomSetIndex+","+bSetSelectSlider+")",1);
//    viewer.setDisplayModelIndex(atomSetIndex);
    viewer.evalStringQuiet("frame "+viewer.getModelNumber(atomSetIndex));
    infoLabel.setText(viewer.getModelName(atomSetIndex));
    showProperties(viewer.getModelProperties(atomSetIndex));
    if (bSetSelectSlider) {
      int sliderValue = getAtomSetCollectionIndex(atomSetIndex);
      if (sliderValue>=0)
        selectSlider.setValue(sliderValue);
      else
        System.out.println("Encountered sliderValue of -1 needed.");
    }
    if (DEBUG) debug("Done",-1);
  }
 
  /**
   * Sets the atomSetCollectionIndexes to the atomSetIndex values of each leaf
   * @param node The node whose leaf's atomSetIndex values should be used
   */
  public void setAtomSetCollectionIndexes(DefaultMutableTreeNode node) {
    if (DEBUG) debug("setAtomSetCollectionIndexes("+node+")",1);

    int atomSetCount = node.getLeafCount();
    atomSetCollectionIndexes = new int[atomSetCount];
    Enumeration e = node.depthFirstEnumeration();
    int idx=0;
    while (e.hasMoreElements()) {
      node = (DefaultMutableTreeNode) e.nextElement();
      if (node.isLeaf())
        atomSetCollectionIndexes[idx++]= ((AtomSet) node).getAtomSetIndex();
    }
    if (DEBUG) {
      System.out.print(indentStr+"Indexes: ");
      for (int i=0; i<atomSetCount; i++)
        System.out.print(" "+atomSetCollectionIndexes[i]);
      System.out.println();      
    }
    // now update the selectSlider
    selectSlider.setEnabled(atomSetCount>0);
    if (DEBUG) debug("calling selectSlider.setMaximum");
    selectSlider.setMaximum(atomSetCount-1);
    if (DEBUG) debug("Done",-1);
  }
    
  public int getAtomSetCollectionIndex(int atomSetIndex) {
    if (DEBUG) debug("getAtomSetCollectionIndex("+atomSetIndex+")",1);
    int count = atomSetCollectionIndexes.length;
    int idx = 0;
    while (idx<count && atomSetCollectionIndexes[idx]!=atomSetIndex) {
      idx++;
    }
    idx = (idx>=count)?-1:idx;
    if (DEBUG) debug("Done : "+idx,-1);
    return idx;
  }

  /* for the real buttons down the line;
  // button clicks
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (REWIND.equals(cmd)) {
      setAtomSet(atomSetCollectionIndexes[0], true);
    } else if (PREVIOUS.equals(cmd)) {
      if (atomSetCollectionIndex>0) {
        setAtomSet(atomSetCollectionIndexes[atomSetCollectionIndex-1], true);
      }
    } else if (NEXT.equals(cmd)) {
      if (atomSetCollectionIndex<=(atomSetCollectionIndexes.length-1)) 
        setAtomSet(atomSetCollectionIndexes[atomSetCollectionIndex-1], true);
    } else if (FF.equals(cmd)) {
      setAtomSet(atomSetCollectionIndexes[atomSetCollectionIndexes.length], true);
    }   
  }
   */
  
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
 
  public void stateChanged(ChangeEvent e) {
    Object src = e.getSource();
    int value = ((JSlider)src).getValue();
    if (src == selectSlider) {
      if (DEBUG) debug("stateChanged:selectSlider",1);
      // do not follow up with setting the selectSlider: infinite loop
      setAtomSet(atomSetCollectionIndexes[value], false);
      if (DEBUG) debug("Done",-1);
    } else if (src == scaleSlider) {
      if (DEBUG) debug("stateChanged:scaleSlider",1);
      viewer.setVibrationScale(value*VIBRATION_SCALE_PRECISION);
      if (DEBUG) debug("Done",-1);
    } else if (src == periodSlider) {
      if (DEBUG) debug("stateChanged:periodSlider",1);
      viewer.setVibrationPeriod(value*VIBRATION_PERIOD_PRECISION);
      if (DEBUG) debug("Done",-1);
    }
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
    if (DEBUG) debug("createTreeModel()",1);

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
    if (DEBUG) debug("Done",-1);
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
  
  private static String indentStr = "";
  public void debug(String str) {
    System.out.println(indentStr+""+str);
  }
  public void debug(String str,int indent) {
    if (indent<0) indentStr=indentStr.substring(1);
    debug(str);
    if (indent>0) indentStr+=" ";
  }
}
