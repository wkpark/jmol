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

import java.text.DecimalFormat;
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
  
  // I use this to make sure that a script gets a decimal.
  // Some script commands act different depending on whether the number is
  // a decimal or an integer.
  private static DecimalFormat threeDigits = new DecimalFormat("0.000");
  
  private JTextArea propertiesTextArea;
  private JTree tree;
  private DefaultTreeModel treeModel;
  private JmolViewer viewer;
  private JSlider selectSlider;
  private JLabel infoLabel;
  private JSlider amplitudeSlider;
  private JSlider periodSlider;
  private JSlider scaleSlider;
  private JSlider radiusSlider;
  
  // Strings for the commands of the buttons and the determination
  // of the tooltips and images associated with them
  static final String REWIND="rewind";
  static final String PREVIOUS="prev";
  static final String PLAY="play";
  static final String PAUSE="pause";
  static final String NEXT="next";
  static final String FF="ff";
  
  /**
   * String for prefix/resource identifier for the collection area.
   * This value is used in the Jmol properties files.
   */
  static final String COLLECTION = "collection";
  /**
   * String for prefix/resource identifier for the vectors area.
   * This value is used in the Jmol properties files.
   */
  static final String VECTORS = "vectors";
  

  /**
   * Sequence if atom set indexes in current tree selection for a branch,
   * or siblings for a leaf.
   */
  int indexes[];
  int currentIndex=-1;
  
  /**
   * Precision of the vibration scale slider
   */
  private static final float AMPLITUDE_PRECISION = 0.01f;
  /**
   * Maximum value for vibration scale. Should be in preferences?
   */
  private static final float AMPLITUDE_MAX = 1;
  /**
   * Initial value of vibration scale. Should be in preferences?
   */
  private static final float AMPLITUDE_VALUE = 0.5f;

  /**
   * Precision of the vibration period slider in seconds.
   */
  private static final float PERIOD_PRECISION = 0.001f;
  /**
   * Maximum value for the vibration period in seconds. Should be in preferences?
   */
  private static final float PERIOD_MAX = 1; // in seconds
  /**
   * Initial value for the vibration period in seconds. Should be in preferences?
   */
  private static final float PERIOD_VALUE = 0.5f;

  /**
   * Precision of the vector radius slider
   */
  private static final float RADIUS_PRECISION = 0.01f;
  /**
   * Maximum value for vector radius. Should be in preferences?
   */
  private static final float RADIUS_MAX = 1.0f;
  /**
   * Initial value of vector radius. Should be in preferences?
   */
  private static final float RADIUS_VALUE = 0.05f;

  /**
   * Precision of the vector scale slider
   */
  private static final float SCALE_PRECISION = 0.01f;
  /**
   * Maximum value for vector scale. Should be in preferences?
   */
  private static final float SCALE_MAX = 2.0f;
  /**
   * Initial value of vector scale. Should be in preferences?
   */
  private static final float SCALE_VALUE = 1.0f;

 
  
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
    propertiesPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.tree.properties.label")));
    propertiesTextArea = new JTextArea();
    propertiesTextArea.setEditable(false);
    propertiesPanel.add(new JScrollPane(propertiesTextArea), BorderLayout.CENTER);
    
    // create the split pane with the treePanel and propertiesPanel
    JPanel astPanel = new JPanel();
    astPanel.setLayout(new BorderLayout());
    astPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.tree.label")));
    
    JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT, treePanel, propertiesPanel); 
    astPanel.add(splitPane, BorderLayout.CENTER);
    splitPane.setResizeWeight(1.0);
    container.add(astPanel, BorderLayout.CENTER);
    
    //////////////////////////////////////////////////////////
    // The Controller area is south of the container
    //////////////////////////////////////////////////////////
    JPanel controllerPanel = new JPanel();
    controllerPanel.setLayout(new BoxLayout(controllerPanel, BoxLayout.Y_AXIS));
    container.add(controllerPanel, BorderLayout.SOUTH);
    
    //////////////////////////////////////////////////////////
    // The collection chooser/controller/feedback area
    //////////////////////////////////////////////////////////
    JPanel collectionPanel = new JPanel();
    collectionPanel.setLayout(new BoxLayout(collectionPanel, BoxLayout.Y_AXIS));
    collectionPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.collection.label")));
    controllerPanel.add(collectionPanel);
    // info area
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BorderLayout());
    infoPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.collection.info.label")));
    infoLabel = new JLabel(" ");
    infoPanel.add(infoLabel, BorderLayout.SOUTH);
    collectionPanel.add(infoPanel);
    // select slider area
    JPanel cpsPanel = new JPanel();
    cpsPanel.setLayout(new BorderLayout());
    cpsPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.collection.select.label")));
    selectSlider = new JSlider(0,0,0);
    selectSlider.addChangeListener(this);
    selectSlider.setMajorTickSpacing(5);
    selectSlider.setMinorTickSpacing(1);
    selectSlider.setPaintTicks(true);
    selectSlider.setSnapToTicks(true);
    cpsPanel.add(selectSlider, BorderLayout.SOUTH);
    collectionPanel.add(cpsPanel);
    // VCR-like play controller
    collectionPanel.add(createVCRController("collection"));
    //////////////////////////////////////////////////////////
    // The vector panel
    //////////////////////////////////////////////////////////
    JPanel vectorPanel = new JPanel();
    controllerPanel.add(vectorPanel);
    // fill out the contents of the vectorPanel
    vectorPanel.setLayout(new BoxLayout(vectorPanel, BoxLayout.Y_AXIS));
    vectorPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.vector.label")));
    // the first row in the vectoPanel: radius and scale of the vector
    JPanel row1 = new JPanel();
    row1.setLayout(new BoxLayout(row1,BoxLayout.X_AXIS));
    // controller for the vector representation
    JPanel radiusPanel = new JPanel();
    radiusPanel.setLayout(new BorderLayout());
    radiusPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.vector.radius.label")));
    radiusSlider = new JSlider(0, (int)(RADIUS_MAX/RADIUS_PRECISION),
        (int) (RADIUS_VALUE/RADIUS_PRECISION));
    radiusSlider.addChangeListener(this);
    viewer.evalStringQuiet("vector "+ threeDigits.format(RADIUS_VALUE));
    radiusPanel.add(radiusSlider);
    row1.add(radiusPanel);
    // controller for the vector scale
    JPanel scalePanel = new JPanel();
    scalePanel.setLayout(new BorderLayout());
    scalePanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.vector.scale.label")));
    scaleSlider = new JSlider(0, (int)(SCALE_MAX/SCALE_PRECISION),
        (int) (SCALE_VALUE/SCALE_PRECISION));
    scaleSlider.addChangeListener(this);
    viewer.setVectorScale(SCALE_VALUE);
    scalePanel.add(scaleSlider);
    row1.add(scalePanel);
    vectorPanel.add(row1);
    // the second row: amplitude and period of the vibration animation
    JPanel row2 = new JPanel();
    row2.setLayout(new BoxLayout(row2,BoxLayout.X_AXIS));
    // controller for vibrationScale = amplitude
    JPanel amplitudePanel = new JPanel();
    amplitudePanel.setLayout(new BorderLayout());
    amplitudePanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.vector.amplitude.label")));
    amplitudeSlider = new JSlider(0, (int) (AMPLITUDE_MAX/AMPLITUDE_PRECISION),
        (int)(AMPLITUDE_VALUE/AMPLITUDE_PRECISION));
    viewer.setVibrationScale(AMPLITUDE_VALUE);
    amplitudeSlider.addChangeListener(this);
    amplitudePanel.add(amplitudeSlider);
    row2.add(amplitudePanel);
    // controller for the vibrationPeriod
    JPanel periodPanel = new JPanel();
    periodPanel.setLayout(new BorderLayout());
    periodPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser.vector.period.label")));
    periodSlider = new JSlider(0,
        (int)(PERIOD_MAX/PERIOD_PRECISION),
        (int)(PERIOD_VALUE/PERIOD_PRECISION));
    viewer.setVibrationPeriod(PERIOD_VALUE);
    periodSlider.addChangeListener(this);
    periodPanel.add(periodSlider);
    row2.add(periodPanel);
    vectorPanel.add(row2);
    // finally the controller at the bottom
    vectorPanel.add(createVCRController("vector"));
  }
  
  /**
   * Creates a VCR type set of controller inside a JPanel.
   * 
   * <p>Uses the JmolResourceHandler to get the label for the panel,
   * the images for the buttons, and the tooltips. The button names are 
   * <code>rewind</code>, <code>prev</code>, <code>play</code>, <code>pause</code>,
   * <code>next</code>, and <code>ff</code>.
   * <p>The handler for the buttons should determine from the getActionCommand
   * which button in which section triggered the actionEvent, which is identified
   * by <code>{section}.{name}</code>.
   * @param section String of the section that the controller belongs to.
   * @return The JPanel
   */
  public JPanel createVCRController(String section) {
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
    controlPanel.setBorder(new TitledBorder(
        JmolResourceHandler.translateX("AtomSetChooser."+section+".VCR.label")));
    Insets inset = new Insets(1,1,1,1);
    String buttons[] = {REWIND,PREVIOUS,PLAY,PAUSE,NEXT,FF};
    for (int i=buttons.length, idx=0; --i>=0; idx++) {
      String action = buttons[idx];
      // the icon and tool tip come from 
      JButton btn = new JButton(
          JmolResourceHandler.getIconX("AtomSetChooser."+action+"Image"));
      btn.setToolTipText(
          JmolResourceHandler.translateX(
              "AtomSetChooser."+section+"."+action+"Tooltip"));
      btn.setMargin(inset);
      btn.setActionCommand(section+"."+action);
      btn.addActionListener(this);
      if (idx>0)
        controlPanel.add(Box.createHorizontalGlue());
      controlPanel.add(btn);
    }
    return controlPanel;
  }
  
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
    tree.getLastSelectedPathComponent();
    if (node == null) {
      return;
    }
    try {
      if (node.isLeaf()) {
        setAtomSet((AtomSet) node);
      } else { // selected branch
        setAtomSetCollection(node, true);
      }
    }
    catch (Exception exception) {
 //     exception.printStackTrace();
    }
  }
  
  /**
   * Show atom set node and update dependent views/controllers.
   * @param node The atom set node
   */
  public void setAtomSet(AtomSet node) {
    setIndexes((DefaultMutableTreeNode) node.getParent());
    setAtomSet(node.getAtomSetIndex(), true);
  }
  
  /**
   * Sets the atomSetCollection to the leafs of the node.
   * @param node The node whose leafs needs to be the collection
   * @param bSetAtomSet If true sets the atom set to the first on in the collection
   */
  public void setAtomSetCollection(DefaultMutableTreeNode node, 
      boolean bSetAtomSet) {
    setIndexes(node);
    if (bSetAtomSet) setAtomSet(indexes[0], true);
    propertiesTextArea.setText("Collection has " +
        node.getLeafCount() + " AtomSets");
  }
  
  /**
   * Show atom set and update dependent views/controllers
   * @param atomSetIndex Index of atom set to be shown
   * @param bSetSelectSlider If true, updates the selectSlider
   */
  public void setAtomSet(int atomSetIndex, boolean bSetSelectSlider) {
    //    viewer.setDisplayModelIndex(atomSetIndex);  // does not update
    viewer.evalStringQuiet("frame "+viewer.getModelNumber(atomSetIndex));
    infoLabel.setText(viewer.getModelName(atomSetIndex));
    showProperties(viewer.getModelProperties(atomSetIndex));
    currentIndex = getAtomSetCollectionIndex(atomSetIndex);
    if (bSetSelectSlider) {
      if (currentIndex>=0)
        selectSlider.setValue(currentIndex);
    }
  }
  
  /**
   * Sets the indexes to the atomSetIndex values of each leaf of the node.
   * @param node The node whose leaf's atomSetIndex values should be used
   */
  public void setIndexes(DefaultMutableTreeNode node) {
    int atomSetCount = node.getLeafCount();
    indexes = new int[atomSetCount];
    Enumeration e = node.depthFirstEnumeration();
    int idx=0;
    while (e.hasMoreElements()) {
      node = (DefaultMutableTreeNode) e.nextElement();
      if (node.isLeaf())
        indexes[idx++]= ((AtomSet) node).getAtomSetIndex();
    }
    // now update the selectSlider (may trigger a valueChanged event...)
    selectSlider.setEnabled(atomSetCount>0);
    selectSlider.setMaximum(atomSetCount-1);
  }
  
  public int getAtomSetCollectionIndex(int atomSetIndex) {
    int count = indexes.length;
    int idx = 0;
    while (idx<count && indexes[idx]!=atomSetIndex) {
      idx++;
    }
    if (idx >= count)
      idx = -1;
    return idx;
  }
  
  // FIXME the play and pause for the collection are not working the way they should
  public void actionPerformed (ActionEvent e) {
    String cmd = e.getActionCommand();
    String parts[]=cmd.split("\\.");
    if (parts.length==2) {
      String section = parts[0];
      cmd = parts[1];
      if (section.equals("collection")) {
        if (REWIND.equals(cmd)) {
          setAtomSet(indexes[0], true);
        } else if (PREVIOUS.equals(cmd)) {
          if (currentIndex>0) {
            setAtomSet(indexes[currentIndex-1], true);
          }
        } else if (PLAY.equals(cmd)) {
          StringBuffer command = new StringBuffer();
          int maxIndex = indexes.length;
          String delay = ";delay "+1.0/viewer.getAnimationFps()+";";
          for (int i=0; i<maxIndex; i++) {
            command.append("frame "+
                viewer.getModelNumber(indexes[(currentIndex+i)%maxIndex])
                + delay);
          }
          viewer.evalStringQuiet(command+"loop");
        } else if (PAUSE.equals(cmd)) {
          // since I don't think I can get the current frame I will go back to
          // the first one when I 'paused'
          viewer.haltScriptExecution();
          setAtomSet(indexes[0],true);
        } else if (NEXT.equals(cmd)) {
          if (currentIndex<=(indexes.length-1)) 
            setAtomSet(indexes[currentIndex+1], true);
        } else if (FF.equals(cmd)) {
          setAtomSet(indexes[indexes.length-1], true);
        }       
      } else if (section.equals("vector")) {
        if (REWIND.equals(cmd)) {
          findFrequency(0,1);
        } else if (PREVIOUS.equals(cmd)) {
          findFrequency(currentIndex-1,-1);
        } else if (PLAY.equals(cmd)) {
          viewer.evalStringQuiet("vibration on");
        } else if (PAUSE.equals(cmd)) {
          viewer.evalStringQuiet("vibration off");
        } else if (NEXT.equals(cmd)) {
          findFrequency(currentIndex+1,1);
        } else if (FF.equals(cmd)) {
          findFrequency(indexes.length-1,-1);
        }     
      }
    }
  }
  
  /**
   * Have the viewer show a particular frame with frequencies
   * if it can be found.
   * @param index Starting index where to start looking for frequencies
   * @param increment Increment value for how to go through the list
   */
  public void findFrequency(int index, int increment) {
    // FIX ME this should only look in the collection set
    int maxIndex = indexes.length;
    boolean foundFrequency = false;
    
    // search till get to either end of found a frequency
    while (index >= 0 && index < maxIndex 
        && !(foundFrequency=viewer.modelHasVibrationVectors(indexes[index]))) {
      index+=increment;
    }
    
    if (foundFrequency) {
      setAtomSet(indexes[index],true);      
    }
  }
  
  public void stateChanged(ChangeEvent e) {
    Object src = e.getSource();
    int value = ((JSlider)src).getValue();
    if (src == selectSlider) {
      setAtomSet(indexes[value], false);
    } else if (src == radiusSlider) {
      viewer.evalStringQuiet("vector " + threeDigits.format(value*RADIUS_PRECISION));
    } else if (src == scaleSlider) {
//      viewer.setVectorScale(value*SCALE_PRECISION); // no update: use script
      viewer.evalStringQuiet("vector scale "+value*SCALE_PRECISION);
    } else if (src == amplitudeSlider) {
      viewer.setVibrationScale(value*AMPLITUDE_PRECISION);
    } else if (src == periodSlider) {
      viewer.setVibrationPeriod(value*PERIOD_PRECISION);
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
    // make sure that we start with the whole set selected
    tree.setSelectionPath(tree.getPathForRow(0));
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