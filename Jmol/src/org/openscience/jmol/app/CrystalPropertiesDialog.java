/* Copyright 2002 The Jmol Development Team
 * $RCSfile$
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

import org.openscience.jmol.viewer.*;
import  org.openscience.jmol.util.*;
import  org.openscience.jmol.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.JTable;  //TBL
import javax.swing.JScrollPane; //SCP
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import javax.swing.JDialog; //DLG
import javax.swing.JPanel;  //PNL
import javax.swing.Action;
import javax.swing.JFrame;  //FRM
import javax.swing.AbstractAction;
import javax.swing.JLabel;  //LBL
import javax.swing.JButton; //BUT
import javax.swing.JTextField; //TXF
import javax.swing.JCheckBox;  //CKB
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.TitledBorder;
import java.util.StringTokenizer;
import java.util.Vector;   //VEC
import java.lang.reflect.Array;
import javax.swing.JTabbedPane; //TBP
import javax.swing.ImageIcon;
import javax.swing.JComboBox;  //CBO  and CBOLST for the associated list
import javax.swing.JOptionPane;
import java.io.IOException;
import javax.swing.JFileChooser;
import java.io.File;
import java.awt.BorderLayout;
import javax.vecmath.Point3d;

/**
 * Cystal -> Properties dialog.
 *
 * @author  Fabian Dortu (Fabian Dortu@wanadoo.be)
 * @version 1.3
 */
public class CrystalPropertiesDialog extends JDialog
  implements ActionListener, PropertyChangeListener {

  // This class is instanciated in Jmol.java as "crystprop". 
  // In Properties/Jmol.Properties, the dialog box must
  // be defined as "crystprop" exactly!

  private JDialog thisDialog;  //reference to this dialog
  
  private JmolViewer viewer;

  private boolean hasFile = false;
  private boolean hasCrystalInfo = false;
  private ChemFile chemFile;
  private CrystalFile crystalFile; //Reference to the crystalFile
  private UnitCellBox unitCellBox; //Reference to the current frame unitCellBox
  private CrystalBox crystalBox; //Reference to the current frame crystalBox
  private ChemFrame currentFrame; //Reference to the current frame.

  private int currentFrameIndex; //the current frame index(modified by Animate.java)

  //The Main Panel
  JTabbedPane tabbedPane;

  //Primitive Vectors fields
  JComboBox primitiveVectorTypeCBO;
  Vector jRprim_VEC_TXF = new Vector(3);    //basis vectors text field
  Vector jRprim_VEC_LBL = new Vector(3);
  Vector jAcell_VEC_TXF = new Vector(3);
  Vector jAcell_VEC_LBL = new Vector(3);
  Vector jEdges_VEC_TXF = new Vector(3);
  Vector jEdges_VEC_LBL = new Vector(3);
  Vector jAngles_VEC_TXF = new Vector(3);
  Vector jAngles_VEC_LBL = new Vector(3);
  JComboBox primitiveVectors_ApplyToWhichFrameCBO;        // apply to which frame?
  String[] applyToList = new String[2];

  //Crystal Box fields
  JComboBox translationTypeCBO;
  Vector jAtomBox_VEC_TXF = new Vector(2);
  Vector jAtomBox_VEC_LBL = new Vector(2);
  Vector jBondBox_VEC_TXF = new Vector(2);
  Vector jBondBox_VEC_LBL = new Vector(2);
  Vector jUnitBox_VEC_TXF = new Vector(2);
  Vector jUnitBox_VEC_LBL = new Vector(2);

  JComboBox crystalBox_ApplyToWhichFrameCBO;    //aplly to which frame?

  JLabel jNatomInBoxLBL;
  JLabel jNatomInCellLBL;
  JLabel jNatomInClipLBL;

  //Basis Vectors fields
  JComboBox basisVectorType_CBO;
  BasisVTableModel basisVectorTableModel = new BasisVTableModel();
  JComboBox basisVector_ApplyToWhichFrameCBO;

  //Energy Band fields
  EnergyLinesTableModel energyLinesTableModel = new EnergyLinesTableModel();
  JTextField plotDefTXF;
  JTextField resolutionTXF;
  JTextField ratioTXF;
  JTextField nRoundTXF;
  JComboBox roundSchemeCBO;
  JComboBox unitsCBO;
  int unitsComboOldIndex;
  JTextField maxETXF;
  JTextField minETXF;
  JTextField fermiETXF;
  JTextField nvTicsTXF;
  JTextField nhTicsTXF;
  JTextField ticSizeTXF;
  JTextField fontsize1TXF;
  JTextField fontsize2TXF;
  JTextField fontsize3TXF;
  JTextField sectionSepTXF;
  JTextField yLabelTXF;
  String[] yLabels;

  boolean hasEnergyBand;
  EnergyBand energyBand;
  JFileChooser chooser;
  int fileChooserReturnVal;
  BandPlot bandPlot;

  // The actions:

  private CrystpropAction crystpropAction = new CrystpropAction();
  private Hashtable commands;


  /**
   * Constructor
   */
  public CrystalPropertiesDialog(JmolViewer viewer, JFrame f) {


    // Invoke JDialog constructor
    super(f, "Crystal Properties...", false);
    this.thisDialog = this;
    this.viewer = viewer;
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    currentFrameIndex = 0;
    applyToList[0] = "Apply to all frames";
    applyToList[1] = "Apply to current frame (0)";


    JPanel container = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    container.setLayout(gridbag);

    JmolResourceHandler resources = JmolResourceHandler.getInstance();


    //****************
    //* Tabbed Panel
    //***************

    tabbedPane = new JTabbedPane();

    Component panel1 = makePrimVectorsPanel();
    tabbedPane.addTab(resources.getString("Crystprop.primVLabel"),
		      resources.getIcon("Crystprop.primVImage"), panel1,
		      resources.getString("Crystprop.primVToolTip"));
    tabbedPane.setSelectedIndex(0);

    Component panel2 = makeCrystalBoxPanel();
    tabbedPane.addTab(resources.getString("Crystprop.crystalboxLabel"),
		      resources.getIcon("Crystprop.crystalboxImage"), panel2,
		      resources.getString("Crystprop.crystalboxToolTip"));

    Component panel3 = makeBasisVectorsPanel();
    tabbedPane.addTab(resources.getString("Crystprop.basisVLabel"),
		      resources.getIcon("Crystprop.basisVImage"), panel3,
		      resources.getString("Crystprop.basisVToolTip"));

    Component panel4 = makeEnergyBandPanel();
    tabbedPane.addTab(resources.getString("Crystprop.energyBandLabel"),
		      resources.getIcon("Crystprop.energyBandImage"), panel4,
		      resources.getString("Crystprop.energyBandToolTip"));
    //EnergyBand is disabled by default
    tabbedPane.setEnabledAt(3,false);
    
    //Component panel4 = makeSpaceGroupPanel("Blah blah blah blah");
    //tabbedPane.addTab("Space Group", icon, panel4, 
    //                "Does nothing at all");
    //Add the tabbed pane to this panel.
    //setLayout(new GridLayout(1, 1)); 


    //************************
    // Confirmation Panel
    //************************

    JPanel jConfirmPanel = new JPanel();
    jConfirmPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    //The "Commit Changes" button
    JButton jApplyButton = new JButton(resources.translate("Apply"));
    JButton jOkButton = new JButton(resources.translate("OK"));
    JButton jCancelButton = new JButton(resources.translate("Cancel"));
    JButton jReadButton =
      new JButton(resources.translate("Read current frame"));

    jApplyButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  commitChange();
	  updateDialog();

	}
      });

    jOkButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        commitChange();
        updateDialog();
        crystpropAction.setEnabled(true);
        thisDialog.setVisible(false);
      }
    });

    jCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        crystpropAction.setEnabled(true);
        thisDialog.setVisible(false);
      }
    });

    jReadButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  updateCurrentFrameIndex();
	  updateDialog();
	}
      });


    jConfirmPanel.add(jOkButton);
    jConfirmPanel.add(jApplyButton);
    jConfirmPanel.add(jCancelButton);
    jConfirmPanel.add(jReadButton);

    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridheight = 2;
    c.weightx = 1.0;
    c.weighty = 10000;
    c.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(tabbedPane, c);
    container.add(tabbedPane);
    c.gridheight = GridBagConstraints.REMAINDER;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weighty = 1.0;
    gridbag.setConstraints(jConfirmPanel, c);
    container.add(jConfirmPanel);


    //*******************
    // Draw main Panel
    //*******************

    getContentPane().add(container);
    addWindowListener(new CrystpropWindowListener());
    pack();
    centerDialog();

  }    //end constructor CrystalPropertiesDialog



  /**
   * This method changes the basis vectors
   * representation (cartesian or lattice)
   * index==0 -> cartesian
   * index==1 -> lattice
   * if hasCrystallInfo==false, only the cartesian is possible
   *
   * @param index an <code>int</code> value
   * @param hasCrystalInfo a <code>boolean</code> value
   */
  protected void updateBasisVectorsPanel() {
    
    int index = basisVectorType_CBO.getSelectedIndex();
      
    if (hasCrystalInfo) {
      int natom = unitCellBox.getAtomCount();
      Object[][] basisVectorData = new Object[natom][5];
      Atom atom;
	
	if (index == 0) {    //Cartesian coord.
        double[][] cartPos = unitCellBox.getCartesianPos();
	for (int i = 0; i < natom; i++) {
          basisVectorData[i][0] = new String(String.valueOf(i));
          basisVectorData[i][1] = new String(unitCellBox.getBaseAtomType(i).
                                             getAtomTypeName());
          basisVectorData[i][2] = new Double(cartPos[i][0]);
          basisVectorData[i][3] = new Double(cartPos[i][1]);
          basisVectorData[i][4] = new Double(cartPos[i][2]);
        }
        basisVectorTableModel.setData(basisVectorData);
        basisVectorTableModel.fireTableDataChanged();
      } else {             //Reduced coord.
        double[][] redPos = unitCellBox.getReducedPos();
        for (int i = 0; i < natom; i++) {
          basisVectorData[i][0] = new String(String.valueOf(i));
          basisVectorData[i][1] = new String(unitCellBox.getBaseAtomType(i).
                                             getAtomTypeName());
          basisVectorData[i][2] = new Double(redPos[i][0]);
          basisVectorData[i][3] = new Double(redPos[i][1]);
          basisVectorData[i][4] = new Double(redPos[i][2]);
        }
        basisVectorTableModel.setData(basisVectorData);
        basisVectorTableModel.fireTableDataChanged();
      }
    } else {               //is not a crystal
      if (index == 1) {
        basisVectorType_CBO.setSelectedIndex(0);
        String message =
          JmolResourceHandler.getInstance().translate(
						      "You must define the primitive vectors to use lattice coordinates");
        errorDialog(message);
        System.out.println(message);
      }

      ChemFrame currentFrame = chemFile.getFrame(currentFrameIndex);
      int natom = currentFrame.getAtomCount();
      Object[][] basisVectorData = new Object[natom][5];
      Atom atom;
      for (int i = 0; i < natom; i++) {
        atom = currentFrame.getJmolAtomAt(i);
        basisVectorData[i][0] = new String(String.valueOf(i));
        basisVectorData[i][1] = new String(atom.getAtomTypeName());
        basisVectorData[i][2] = new Double(atom.getPoint3D().x);
        basisVectorData[i][3] = new Double(atom.getPoint3D().y);
        basisVectorData[i][4] = new Double(atom.getPoint3D().z);
      }
      basisVectorTableModel.setData(basisVectorData);
      basisVectorTableModel.fireTableDataChanged();

    }
  }                        //end updateBasisVectorsPanel



  /**
   * This method set which of the cartesian or crystallographic
   * primitive vectors is editable.
   * Depends of primitiveVectorTypeCBO state
   *
   * @param index an <code>int</code> value
   */
  protected void setPrimVectorsState() {

    //Set what is editable depending on the combo box value
    int index = primitiveVectorTypeCBO.getSelectedIndex();
    if (index == 0) {
      for (int i = 0; i < 3; i++) {
        ((JTextField) (jRprim_VEC_TXF.elementAt(i))).setEditable(true);
        ((JTextField) (jAcell_VEC_TXF.elementAt(i))).setEditable(true);
        ((JTextField) (jEdges_VEC_TXF.elementAt(i))).setEditable(false);
        ((JTextField) (jAngles_VEC_TXF.elementAt(i))).setEditable(false);
      }
    } else if (index == 1) {
      for (int i = 0; i < 3; i++) {
        ((JTextField) (jRprim_VEC_TXF.elementAt(i))).setEditable(false);
        ((JTextField) (jAcell_VEC_TXF.elementAt(i))).setEditable(false);
        ((JTextField) (jEdges_VEC_TXF.elementAt(i))).setEditable(true);
        ((JTextField) (jAngles_VEC_TXF.elementAt(i))).setEditable(true);
      }
    }
  }    //end setPrimVectorsState


  /**
   * Make usage or not of atomBox 
   * Depends of translationTypeCBO state
   */
  protected void setTranslationState() {

    //Set what is editable depending on the checkbox value
    if (translationTypeCBO.getSelectedIndex() == CrystalBox.CRYSTAL ) {
      for (int i = 0; i < 2; i++) {
        ((JTextField) (jAtomBox_VEC_TXF.elementAt(i))).setEditable(true);
      }
    } else {
      for (int i = 0; i < 2; i++) {
        ((JTextField) (jAtomBox_VEC_TXF.elementAt(i))).setEditable(false);
      }
    }
  }    //end setTranslationState
  
  
  /**
   * Describe <code>makePrimVectorsPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makePrimVectorsPanel() {

    JmolResourceHandler resources = JmolResourceHandler.getInstance();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 1.0;
    c.weighty = 1.0;

    // The jPrimVPanel (level 0 panel) which contains 
    // the jPrimVType, the jCart and the jCryst panel.
    JPanel jPrimVPanel = new JPanel();
    jPrimVPanel.setLayout(gridbag);


    // The jPrimVType subPanel (level 1)
    JPanel jPrimVType = new JPanel();
    jPrimVType.setLayout(gridbag);

    String[] primVTypeStrings = {
      resources.translate("Cartesian"),
      resources.translate("Crystallographic")
    };
    primitiveVectorTypeCBO = new JComboBox(primVTypeStrings);
    primitiveVectorTypeCBO.setSelectedIndex(0);
    primitiveVectorTypeCBO.addActionListener(new ActionListener() {

	public void actionPerformed(ActionEvent e) {
	  //        JComboBox cb = (JComboBox) e.getSource();
	  //int primVTypeIndex = cb.getSelectedIndex();
	  setPrimVectorsState();//FIX
	}
      });

    JLabel jPrimVTypeLabel = new JLabel(resources.translate("Representation")
					+ ": ", SwingConstants.LEFT);


    //The jCart subPanel (level 1)
    JPanel jCart = new JPanel();
    jCart.setLayout(gridbag);
    jCart.setBorder(new TitledBorder(resources
				     .getString("Crystprop.cartesianLabel")));


    //The 3 basis vectors text box in Cartesian representation
    jRprim_VEC_TXF.addElement(new JTextField(22));
    jRprim_VEC_TXF.addElement(new JTextField(22));
    jRprim_VEC_TXF.addElement(new JTextField(22));
    jRprim_VEC_LBL.addElement(new JLabel("1:", SwingConstants.RIGHT));
    jRprim_VEC_LBL.addElement(new JLabel("2:", SwingConstants.RIGHT));
    jRprim_VEC_LBL.addElement(new JLabel("3:", SwingConstants.RIGHT));
    ((JTextField) (jRprim_VEC_TXF.elementAt(0)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.rprim1Tooltip"));
    ((JTextField) (jRprim_VEC_TXF.elementAt(1)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.rprim2Tooltip"));
    ((JTextField) (jRprim_VEC_TXF.elementAt(2)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.rprim3Tooltip"));

    //The 3 acell text box
    jAcell_VEC_TXF.addElement(new JTextField(7));
    jAcell_VEC_TXF.addElement(new JTextField(7));
    jAcell_VEC_TXF.addElement(new JTextField(7));
    jAcell_VEC_LBL.addElement(new JLabel("*", SwingConstants.RIGHT));
    jAcell_VEC_LBL.addElement(new JLabel("*", SwingConstants.RIGHT));
    jAcell_VEC_LBL.addElement(new JLabel("*", SwingConstants.RIGHT));
    ((JTextField) (jAcell_VEC_TXF.elementAt(0)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.acell1Tooltip"));
    ((JTextField) (jAcell_VEC_TXF.elementAt(1)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.acell2Tooltip"));
    ((JTextField) (jAcell_VEC_TXF.elementAt(2)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.acell3Tooltip"));



    //The jCryst subPanel (level 1)
    JPanel jCryst = new JPanel();
    jCryst.setLayout(gridbag);
    jCryst.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
				      .getString("Crystprop.crystalloLabel")));

    //The 3 edges (Crystallographic representation)
    jEdges_VEC_TXF.addElement(new JTextField(7));
    jEdges_VEC_TXF.addElement(new JTextField(7));
    jEdges_VEC_TXF.addElement(new JTextField(7));
    jEdges_VEC_LBL.addElement(new JLabel("a:", SwingConstants.RIGHT));
    jEdges_VEC_LBL.addElement(new JLabel("b:", SwingConstants.RIGHT));
    jEdges_VEC_LBL.addElement(new JLabel("c:", SwingConstants.RIGHT));
    ((JTextField) (jEdges_VEC_TXF.elementAt(0)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.edgeaTooltip"));
    ((JTextField) (jEdges_VEC_TXF.elementAt(1)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.edgebTooltip"));
    ((JTextField) (jEdges_VEC_TXF.elementAt(2)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.edgecTooltip"));


    //The 3 angles
    jAngles_VEC_TXF.addElement(new JTextField(7));
    jAngles_VEC_TXF.addElement(new JTextField(7));
    jAngles_VEC_TXF.addElement(new JTextField(7));
    jAngles_VEC_LBL.addElement(new JLabel("\u03B1:", SwingConstants.RIGHT)); //alpha
    jAngles_VEC_LBL.addElement(new JLabel("\u03B2:", SwingConstants.RIGHT)); //beta
    jAngles_VEC_LBL.addElement(new JLabel("\u03B3:", SwingConstants.RIGHT)); //gamma
    ((JTextField) (jAngles_VEC_TXF.elementAt(0)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.angleaTooltip"));
    ((JTextField) (jAngles_VEC_TXF.elementAt(1)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.anglebTooltip"));
    ((JTextField) (jAngles_VEC_TXF.elementAt(2)))
      .setToolTipText(JmolResourceHandler.getInstance()
		      .getString("Crystprop.anglecTooltip"));


    //Apply to all/current frame radio box
    primitiveVectors_ApplyToWhichFrameCBO = new JComboBox(applyToList);
    primitiveVectors_ApplyToWhichFrameCBO.setSelectedIndex(1);



    //add level 2 widgets to level 1 panels

    //   add widgets to jPrimVType panel
    c.gridwidth = 2;
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(jPrimVTypeLabel, c);
    jPrimVType.add(jPrimVTypeLabel);
    c.weightx = 100000;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(primitiveVectorTypeCBO, c);
    jPrimVType.add(primitiveVectorTypeCBO);


    //   add widgets to jCart Panel.
    //c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprim_VEC_LBL.elementAt(0), c);
    jCart.add((JLabel) jRprim_VEC_LBL.elementAt(0));
    gridbag.setConstraints((JTextField) jRprim_VEC_TXF.elementAt(0), c);
    jCart.add((JTextField) jRprim_VEC_TXF.elementAt(0));
    gridbag.setConstraints((JLabel) jAcell_VEC_LBL.elementAt(0), c);
    jCart.add((JLabel) jAcell_VEC_LBL.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell_VEC_TXF.elementAt(0), c);
    jCart.add((JTextField) jAcell_VEC_TXF.elementAt(0));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprim_VEC_LBL.elementAt(1), c);
    jCart.add((JLabel) jRprim_VEC_LBL.elementAt(1));
    gridbag.setConstraints((JTextField) jRprim_VEC_TXF.elementAt(1), c);
    jCart.add((JTextField) jRprim_VEC_TXF.elementAt(1));
    gridbag.setConstraints((JLabel) jAcell_VEC_LBL.elementAt(1), c);
    jCart.add((JLabel) jAcell_VEC_LBL.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell_VEC_TXF.elementAt(1), c);
    jCart.add((JTextField) jAcell_VEC_TXF.elementAt(1));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprim_VEC_LBL.elementAt(2), c);
    jCart.add((JLabel) jRprim_VEC_LBL.elementAt(2));
    gridbag.setConstraints((JTextField) jRprim_VEC_TXF.elementAt(2), c);
    jCart.add((JTextField) jRprim_VEC_TXF.elementAt(2));
    gridbag.setConstraints((JLabel) jAcell_VEC_LBL.elementAt(2), c);
    jCart.add((JLabel) jAcell_VEC_LBL.elementAt(2));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell_VEC_TXF.elementAt(2), c);
    jCart.add((JTextField) jAcell_VEC_TXF.elementAt(2));

    //   add widgets to jCryst Panel.
    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdges_VEC_LBL.elementAt(0), c);
    jCryst.add((JLabel) jEdges_VEC_LBL.elementAt(0));
    gridbag.setConstraints((JTextField) jEdges_VEC_TXF.elementAt(0), c);
    jCryst.add((JTextField) jEdges_VEC_TXF.elementAt(0));
    gridbag.setConstraints((JLabel) jAngles_VEC_LBL.elementAt(0), c);
    jCryst.add((JLabel) jAngles_VEC_LBL.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles_VEC_TXF.elementAt(0), c);
    jCryst.add((JTextField) jAngles_VEC_TXF.elementAt(0));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdges_VEC_LBL.elementAt(1), c);
    jCryst.add((JLabel) jEdges_VEC_LBL.elementAt(1));
    gridbag.setConstraints((JTextField) jEdges_VEC_TXF.elementAt(1), c);
    jCryst.add((JTextField) jEdges_VEC_TXF.elementAt(1));
    gridbag.setConstraints((JLabel) jAngles_VEC_LBL.elementAt(1), c);
    jCryst.add((JLabel) jAngles_VEC_LBL.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles_VEC_TXF.elementAt(1), c);
    jCryst.add((JTextField) jAngles_VEC_TXF.elementAt(1));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdges_VEC_LBL.elementAt(2), c);
    jCryst.add((JLabel) jEdges_VEC_LBL.elementAt(2));
    gridbag.setConstraints((JTextField) jEdges_VEC_TXF.elementAt(2), c);
    jCryst.add((JTextField) jEdges_VEC_TXF.elementAt(2));
    gridbag.setConstraints((JLabel) jAngles_VEC_LBL.elementAt(2), c);
    jCryst.add((JLabel) jAngles_VEC_LBL.elementAt(2));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles_VEC_TXF.elementAt(2), c);
    jCryst.add((JTextField) jAngles_VEC_TXF.elementAt(2));


    //add level 1 Panels to level 0 Panel
    c.weighty = 1.0;
    c.gridheight = 4;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jPrimVType, c);
    jPrimVPanel.add(jPrimVType);
    c.weighty = 10000;
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(jCart, c);
    jPrimVPanel.add(jCart);
    gridbag.setConstraints(jCryst, c);
    jPrimVPanel.add(jCryst);
    c.weighty = 1.0;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(primitiveVectors_ApplyToWhichFrameCBO, c);
    jPrimVPanel.add(primitiveVectors_ApplyToWhichFrameCBO);

    //JLabel picture =
    //    new JLabel(JmolResourceHandler.getInstance()
    //             .getIcon("Crystprop.notationImage"));
    //
    //picture.setPreferredSize(new Dimension(500, 500));
    //      jPrimVPanel.add(picture);

    //return level 0 panel
    return jPrimVPanel;


  }    //end  makePrimVectorsPanel

  

  protected void updatePrimVectorsPanel() {
    
    boolean isPrimVectorsCart = unitCellBox.isPrimVectorsCartesian();
    
    if (isPrimVectorsCart) {
      primitiveVectorTypeCBO.setSelectedIndex(0);
      setPrimVectorsState();
    } else {
      primitiveVectorTypeCBO.setSelectedIndex(1);
      setPrimVectorsState();
    }

    double[][] rprim = unitCellBox.getRprim();
    double[] acell = unitCellBox.getAcell();
    ((JTextField) (jRprim_VEC_TXF.elementAt(0)))
      .setText((float)rprim[0][0] + ", " + (float)rprim[0][1] + ", " 
	       + (float)rprim[0][2]);
    ((JTextField) (jRprim_VEC_TXF.elementAt(1)))
      .setText((float)rprim[1][0] + ", " + (float)rprim[1][1] + ", " 
	       + (float)rprim[1][2]);
    ((JTextField) (jRprim_VEC_TXF.elementAt(2)))
      .setText((float)rprim[2][0] + ", " + (float)rprim[2][1] + ", " 
	       + (float)rprim[2][2]);
    ((JTextField) (jAcell_VEC_TXF.elementAt(0))).setText((float)acell[0] 
							 + "");
    ((JTextField) (jAcell_VEC_TXF.elementAt(1))).setText((float)acell[1] 
							 + "");
    ((JTextField) (jAcell_VEC_TXF.elementAt(2))).setText((float)acell[2] 
							 + "");
    

    double[] edges = unitCellBox.getEdges();
    double[] angles = unitCellBox.getAngles();
    ((JTextField) (jEdges_VEC_TXF.elementAt(0))).setText((float)edges[0] 
							 + "");
    ((JTextField) (jEdges_VEC_TXF.elementAt(1))).setText((float)edges[1] 
							 + "");
    ((JTextField) (jEdges_VEC_TXF.elementAt(2))).setText((float)edges[2] 
							 + "");
    ((JTextField) (jAngles_VEC_TXF.elementAt(0))).setText((float)angles[0] 
							  + "");
    ((JTextField) (jAngles_VEC_TXF.elementAt(1))).setText((float)angles[1] 
							  + "");
    ((JTextField) (jAngles_VEC_TXF.elementAt(2))).setText((float)angles[2] 
							  + "");


    
  } //end updatePrimVectorsPanel
  
  
  /**
   * Describe <code>makeCrystalBoxPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeCrystalBoxPanel() {

    JmolResourceHandler resources = JmolResourceHandler.getInstance();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();

    //Panel that contains jAtomBox_VEC_TXF, the jBondBox_VEC_TXF and the jUnitBox_VEC_TXF panels
    JPanel jCrystBoxPanel = new JPanel();
    jCrystBoxPanel.setLayout(gridbag);





    String[] translationTypeStrings = {
	resources.translate("Original atoms"),   // CrystalBox.ORIGINAL=0
	resources.translate("Fit in unit cell"), // CrystalBox.INBOX=1
	resources.translate("Set Range")         // CrystalBox.CRYSTAL=2
    };
    translationTypeCBO = new JComboBox(translationTypeStrings);
    translationTypeCBO.setSelectedIndex(0);
    translationTypeCBO.addActionListener(new ActionListener() {

	public void actionPerformed(ActionEvent e) {
	  //        JComboBox cb = (JComboBox) e.getSource();
	  //int primVTypeIndex = cb.getSelectedIndex();
	  setTranslationState();//FIX
	}
      });



    JPanel jAtomBoxPanel = new JPanel();
    jAtomBoxPanel
      .setBorder(new TitledBorder
		 (resources.getString("Crystprop.atomboxLabel")));
    jAtomBoxPanel.setLayout(gridbag);
    
    JPanel jBondBoxPanel = new JPanel();
    jBondBoxPanel
      .setBorder(new TitledBorder
		 (resources.getString("Crystprop.bondboxLabel")));
    jBondBoxPanel.setLayout(gridbag);

    JPanel jUnitBoxPanel = new JPanel();
    jUnitBoxPanel
      .setBorder(new TitledBorder
		 (resources.getString("Crystprop.unitboxLabel")));
    jUnitBoxPanel.setLayout(gridbag);

    jAtomBox_VEC_TXF.addElement(new JTextField(15));
    jAtomBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Minimum atom box coordinate") + ": ",
					SwingConstants.LEFT));
    jAtomBox_VEC_TXF.addElement(new JTextField(15));
    jAtomBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Maximum atom box coordinate") + ": ",
					SwingConstants.LEFT));
    jBondBox_VEC_TXF.addElement(new JTextField(15));
    jBondBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Minimum bond box coordinate") + ": ",
					SwingConstants.LEFT));
    jBondBox_VEC_TXF.addElement(new JTextField(15));
    jBondBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Maximum bond box coordinate") + ": ",
					SwingConstants.LEFT));
    jUnitBox_VEC_TXF.addElement(new JTextField(15));
    jUnitBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Minimum unit box coordinate") + ": ",
					SwingConstants.LEFT));
    jUnitBox_VEC_TXF.addElement(new JTextField(15));
    jUnitBox_VEC_LBL.addElement(
			     new JLabel(
					resources.translate("Maximum unit box coordinate") + ": ",
					SwingConstants.LEFT));

    jNatomInBoxLBL =
      new JLabel(JmolResourceHandler.getInstance()
		 .getString("Crystprop.NatomBox"), SwingConstants.LEFT);

    jNatomInClipLBL =
      new JLabel(JmolResourceHandler.getInstance()
		 .getString("Crystprop.NCell"), SwingConstants.LEFT);


    //AtomBox
    c.weightx = 1.0;
    c.weighty = 1.0;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = 4;
    c.gridwidth= GridBagConstraints.REMAINDER;
    gridbag.setConstraints(translationTypeCBO, c);
    jAtomBoxPanel.add(translationTypeCBO);
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jAtomBox_VEC_LBL.elementAt(0), c);
    jAtomBoxPanel.add((JLabel) jAtomBox_VEC_LBL.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAtomBox_VEC_TXF.elementAt(0), c);
    jAtomBoxPanel.add((JTextField) jAtomBox_VEC_TXF.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jAtomBox_VEC_LBL.elementAt(1), c);
    jAtomBoxPanel.add((JLabel) jAtomBox_VEC_LBL.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAtomBox_VEC_TXF.elementAt(1), c);
    jAtomBoxPanel.add((JTextField) jAtomBox_VEC_TXF.elementAt(1));
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jNatomInBoxLBL, c);
    jAtomBoxPanel.add(jNatomInBoxLBL);

    
    //BondBox
    c.gridwidth = 2;
    c.gridheight = 3;
    gridbag.setConstraints((JLabel) jBondBox_VEC_LBL.elementAt(0), c);
    jBondBoxPanel.add((JLabel) jBondBox_VEC_LBL.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jBondBox_VEC_TXF.elementAt(0), c);
    jBondBoxPanel.add((JTextField) jBondBox_VEC_TXF.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jBondBox_VEC_LBL.elementAt(1), c);
    jBondBoxPanel.add((JLabel) jBondBox_VEC_LBL.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jBondBox_VEC_TXF.elementAt(1), c);
    jBondBoxPanel.add((JTextField) jBondBox_VEC_TXF.elementAt(1));
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jNatomInClipLBL, c);
    jBondBoxPanel.add(jNatomInClipLBL);

    //UnitBox
    c.gridwidth = 2;
    c.gridheight = 3;
    gridbag.setConstraints((JLabel) jUnitBox_VEC_LBL.elementAt(0), c);
    jUnitBoxPanel.add((JLabel) jUnitBox_VEC_LBL.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jUnitBox_VEC_TXF.elementAt(0), c);
    jUnitBoxPanel.add((JTextField) jUnitBox_VEC_TXF.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jUnitBox_VEC_LBL.elementAt(1), c);
    jUnitBoxPanel.add((JLabel) jUnitBox_VEC_LBL.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jUnitBox_VEC_TXF.elementAt(1), c);
    jUnitBoxPanel.add((JTextField) jUnitBox_VEC_TXF.elementAt(1));


    crystalBox_ApplyToWhichFrameCBO = new JComboBox(applyToList);
    crystalBox_ApplyToWhichFrameCBO.setSelectedIndex(0);

    c.gridheight = 4;
    c.weighty = 10000;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jAtomBoxPanel, c);
    jCrystBoxPanel.add(jAtomBoxPanel);
    gridbag.setConstraints(jBondBoxPanel, c);
    jCrystBoxPanel.add(jBondBoxPanel);
    gridbag.setConstraints(jUnitBoxPanel, c);
    jCrystBoxPanel.add(jUnitBoxPanel);
    c.weighty = 1;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(crystalBox_ApplyToWhichFrameCBO, c);
    jCrystBoxPanel.add(crystalBox_ApplyToWhichFrameCBO);


    return jCrystBoxPanel;
  }    //end makeCrystalBoxPanel


  protected void updateCrystalBoxPanel() {

    double[][] atomBox = crystalBox.getAtomBox();
    double[][] bondBox = crystalBox.getBondBox();
    double[][] unitBox = crystalBox.getUnitBox();
    
    translationTypeCBO.setSelectedIndex(crystalBox.getTranslationType());

    ((JTextField) (jAtomBox_VEC_TXF.elementAt(0)))
      .setText((float)atomBox[0][0] + ", " + (float)atomBox[0][1] 
	       + ", " + (float)atomBox[0][2]);
    ((JTextField) (jAtomBox_VEC_TXF.elementAt(1)))
      .setText((float)atomBox[1][0] + ", " + (float)atomBox[1][1] 
	       + ", " + (float)atomBox[1][2]);
    ((JTextField) (jBondBox_VEC_TXF.elementAt(0)))
      .setText((float)bondBox[0][0] + ", " + (float)bondBox[0][1] 
	       + ", " + (float)bondBox[0][2]);
    ((JTextField) (jBondBox_VEC_TXF.elementAt(1)))
      .setText((float)bondBox[1][0] + ", " + (float)bondBox[1][1] 
	       + ", " + (float)bondBox[1][2]);
    ((JTextField) (jUnitBox_VEC_TXF.elementAt(0)))
      .setText((float)unitBox[0][0] + ", " + (float)unitBox[0][1] 
	       + ", " + (float)unitBox[0][2]);
    ((JTextField) (jUnitBox_VEC_TXF.elementAt(1)))
      .setText((float)unitBox[1][0] + ", " + (float)unitBox[1][1] 
	       + ", " + (float)unitBox[1][2]);
    
    // the number of atoms in the unit cell

    jNatomInCellLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NCell")
       + " " + unitCellBox.getAtomCount());
    
    // the number of atoms in the crystal box
    jNatomInBoxLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
       + " " + crystalFile.getFrame(currentFrameIndex).getAtomCount());
    
    jNatomInClipLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NbondBox")
       + " " + crystalFile.getNumberBondedAtoms(currentFrameIndex));
    
  } //end updateCrystalBoxPanel
  

  /**
   * Describe <code>makeBasisVectorsPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeBasisVectorsPanel() {

    JmolResourceHandler resources = JmolResourceHandler.getInstance();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 1.0;
    c.weighty = 1.0;

    JPanel jBasisVPanel = new JPanel();
    jBasisVPanel.setLayout(gridbag);


    String[] basisVectorTypeStrings = {
      resources.translate("Cartesian"), resources.translate("Lattice")
    };
    basisVectorType_CBO = new JComboBox(basisVectorTypeStrings);
    basisVectorType_CBO.setSelectedIndex(0);
    basisVectorType_CBO.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  updateBasisVectorsPanel();
	}
      });
    
    
    JTable table = new JTable(basisVectorTableModel);
    table.setPreferredScrollableViewportSize(new Dimension(500, 180));

    //Create the scroll pane and add the table to it. 
    JScrollPane scrollPane = new JScrollPane(table);

    jNatomInCellLBL =
      new JLabel(JmolResourceHandler.getInstance()
		 .getString("Crystprop.NbondBox"), SwingConstants.LEFT);



    String[] basisApplyToList = {
      resources.translate("Apply to all frames"),
      resources.translate("Apply to current frame")
    };
    basisVector_ApplyToWhichFrameCBO = new JComboBox(basisApplyToList);
    basisVector_ApplyToWhichFrameCBO.setSelectedIndex(1);



    c.gridheight = 4;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(basisVectorType_CBO, c);
    jBasisVPanel.add(basisVectorType_CBO);
    c.weighty = 10000;
    c.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(scrollPane, c);
    jBasisVPanel.add(scrollPane);
    c.fill = GridBagConstraints.NONE;
    c.weighty = 1;
    gridbag.setConstraints(jNatomInCellLBL, c);
    jBasisVPanel.add(jNatomInCellLBL);
    c.gridheight = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.NONE;
    gridbag.setConstraints(basisVector_ApplyToWhichFrameCBO, c);

    //jBasisVPanel.add(basisVector_ApplyToWhichFrameCBO);


    return jBasisVPanel;

  }    //end makeBasisVectorsPanel



  /**
   * Describe <code>makeEnergyBandPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeEnergyBandPanel() {

    JmolResourceHandler resources = JmolResourceHandler.getInstance();

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 1.0;
    c.weighty = 1.0;

    JPanel jEnergyBandPanel = new JPanel();
    jEnergyBandPanel.setLayout(gridbag);

    JTable table1 = new JTable(energyLinesTableModel);
    table1.setPreferredScrollableViewportSize(new Dimension(250, 90));
    
    JLabel resolutionLabel = new JLabel
      (resources.getString("Crystalprop.ebTab.resolution"));
    resolutionTXF = new JTextField(5);
    resolutionTXF.setText("300"); //Default value
    JLabel ratioLabel = new JLabel
      (resources.getString("Crystalprop.ebTab.ratio"));
    ratioTXF = new JTextField(5);
    ratioTXF.setText("1"); //Default value
    JLabel unitsLabel = new JLabel
      (resources.getString("Crystalprop.ebTab.eunits"));
    yLabels = Units.getFormatedEnergyList();
    String[] unitsList = Units.getEnergyList(); //Order must follow Units class
    unitsCBO = new JComboBox(unitsList); 
    unitsCBO.addActionListener(new ActionListener() {
	
	public void actionPerformed(ActionEvent e) {
	  double cf = (float)Units.getConversionFactor(unitsComboOldIndex+100,
				     unitsCBO.getSelectedIndex()+100);
	  minETXF.setText(Float.toString((float)(FieldReader.readField1(minETXF)*cf)));
	  maxETXF.setText(Float.toString((float)(FieldReader.readField1(maxETXF)*cf)));
	  fermiETXF.setText(Float.toString
			    ((float)(FieldReader.readField1(fermiETXF)*cf)));
	  yLabelTXF.setText(yLabels[unitsCBO.getSelectedIndex()]);
	  unitsComboOldIndex=unitsCBO.getSelectedIndex();
	}
      });

    JLabel plotDefLabel = new JLabel
      (resources.getString("Crystalprop.ebTab.defplot"));
    plotDefTXF = new JTextField(50);
    plotDefTXF.setToolTipText
      (resources.getString("Crystalprop.ebTab.defplotToolTip"));
    plotDefTXF.setText("0");
    nRoundTXF = new JTextField(3);
    nRoundTXF.setText("2");
    String[] roundSchemeList = 
      {resources.getString("Crystalprop.ebTab.ffd"), 
       resources.getString("Crystalprop.ebTab.efd")};
    roundSchemeCBO = new JComboBox(roundSchemeList);
    JLabel maxELabel = new JLabel
      (resources.getString("Crystalprop.ebTab.emax"));
    maxETXF = new JTextField(10);
    JLabel minELabel = new JLabel
      (resources.getString("Crystalprop.ebTab.emin"));
    minETXF = new JTextField(10);
    JLabel fermiELabel = new JLabel
      (resources.getString("Crystalprop.ebTab.efermi"));
    fermiETXF = new JTextField(10);
    
    JLabel nvTicsLBL = new JLabel
      (resources.getString("Crystalprop.ebTab.nvtics"));
    nvTicsTXF = new JTextField(5);
    nvTicsTXF.setText("10");

    JLabel nhTicsLBL = new JLabel
      (resources.getString("Crystalprop.ebTab.nhtics"));
    nhTicsTXF = new JTextField(5);
    nhTicsTXF.setText("5");

    JLabel ticSizeLBL = new JLabel
      (resources.getString("Crystalprop.ebTab.ticsize"));
    ticSizeTXF = new JTextField(5);
    ticSizeTXF.setText("5");
       
    
    JLabel fontsize1LBL = new JLabel
      (resources.getString("Crystalprop.ebTab.fontsize1"));
    fontsize1TXF = new JTextField(5);
    fontsize1TXF.setText("20");

    JLabel fontsize2LBL = new JLabel
      (resources.getString("Crystalprop.ebTab.fontsize2"));
    fontsize2TXF = new JTextField(5);
    fontsize2TXF.setText("10");

    JLabel fontsize3LBL = new JLabel
      (resources.getString("Crystalprop.ebTab.fontsize3"));
    fontsize3TXF = new JTextField(5);
    fontsize3TXF.setText("25");

    JLabel sectionSepLBL = new JLabel
      (resources.getString("Crystalprop.ebTab.sepsize"));
    sectionSepTXF = new JTextField(5);
    sectionSepTXF.setText("0.1");

    JLabel yLabelLBL = new JLabel
      (resources.getString("Crystalprop.ebTab.ylabel"));
    yLabelTXF = new JTextField(15);
    yLabelTXF.setText("(eV)");
    yLabelTXF.setToolTipText(resources.getString
			 ("Crystalprop.ebTab.ylabelToolTip"));
    
    JButton showBUT = new JButton
      (resources.getString("Crystalprop.ebTab.showBUT"));
    
    showBUT.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  // Draw the band plot in new Frame
	  showEnergyBandPlot();	  
	}
      });
    
    // Logical:
    //
    //               /-scrollPane1             
    //               |                         /-col1Panel
    //energyBandPanel|          /- tunningPanel|-col2Panel
    //               \-plotPanel|              \-col3Panel 
    //                          |- linePanel
    //                          \- buttonPanel

    // Visual:
    // |------------------------|
    // |                        |
    // |                        |
    // |      scrollPane1       |
    // |                        |
    // |------------------------|
    // | col1Panel | col2Panel  |
    // |           |            |
    // |------------------------|
    // |     LinePanel          |
    // |------------------------| 
    // |     buttonPanel        |    
    // |------------------------|
     
    //Create the scroll pane and add the table to it. 
    JScrollPane scrollPane1 = new JScrollPane(table1);
    
    // JPanel containing plot definition stuff (col1Panel, col2Panel, linePanel)
    JPanel tunningPanel = new JPanel();
    tunningPanel.setLayout(new FlowLayout(FlowLayout.LEFT,20,5));
    
    JPanel col1Panel = new JPanel();
    col1Panel.setLayout(gridbag);
    //    c.anchor = GridBagConstraints.NORTHEAST;
    c.fill = GridBagConstraints.BOTH;
    c.weighty=1;
    c.weightx=1;
    c.gridheight = 5;
    c.gridwidth = 2;
    gridbag.setConstraints(resolutionLabel, c);
    col1Panel.add(resolutionLabel);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.weightx = 1000;
    gridbag.setConstraints(resolutionTXF, c);
    col1Panel.add(resolutionTXF);
    c.weightx=1;
    c.gridwidth = 2;
    gridbag.setConstraints(ratioLabel, c);
    col1Panel.add(ratioLabel);
    c.weightx = 1000;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(ratioTXF, c);
    col1Panel.add(ratioTXF);
    c.gridwidth = 2;
    gridbag.setConstraints(fontsize1LBL, c);
    col1Panel.add(fontsize1LBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(fontsize1TXF, c);
    col1Panel.add(fontsize1TXF);
    c.gridwidth = 2;
    gridbag.setConstraints(fontsize2LBL, c);
    col1Panel.add(fontsize2LBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(fontsize2TXF, c);
    col1Panel.add(fontsize2TXF);
    c.gridwidth = 2;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(fontsize3LBL, c);
    col1Panel.add(fontsize3LBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(fontsize3TXF, c);
    col1Panel.add(fontsize3TXF);


    JPanel col2Panel = new JPanel();
    col2Panel.setLayout(gridbag);
    c.gridheight = 4;
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(unitsLabel, c);
    col2Panel.add(unitsLabel);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(unitsCBO, c);
    col2Panel.add(unitsCBO);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(minELabel, c);
    col2Panel.add(minELabel);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(minETXF, c);
    col2Panel.add(minETXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(maxELabel, c);
    col2Panel.add(maxELabel);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(maxETXF, c);
    col2Panel.add(maxETXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(fermiELabel, c);
    col2Panel.add(fermiELabel);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(fermiETXF, c);
    col2Panel.add(fermiETXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(roundSchemeCBO, c);
    col2Panel.add(roundSchemeCBO);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(nRoundTXF, c);
    col2Panel.add(nRoundTXF);


    JPanel col3Panel = new JPanel();
    col3Panel.setLayout(gridbag);
    c.gridheight = 5;
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(nvTicsLBL, c);
    col3Panel.add(nvTicsLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(nvTicsTXF, c);
    col3Panel.add(nvTicsTXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(nhTicsLBL, c);
    col3Panel.add(nhTicsLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(nhTicsTXF, c);
    col3Panel.add(nhTicsTXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(ticSizeLBL, c);
    col3Panel.add(ticSizeLBL);
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(ticSizeTXF, c);
    col3Panel.add(ticSizeTXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(sectionSepLBL, c);
    col3Panel.add(sectionSepLBL);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(sectionSepTXF, c);
    col3Panel.add(sectionSepTXF);
    c.gridwidth = 2;
    c.anchor = GridBagConstraints.NORTHEAST;
    gridbag.setConstraints(yLabelLBL, c);
    col3Panel.add(yLabelLBL);
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(yLabelTXF, c);
    col3Panel.add(yLabelTXF);

    tunningPanel.add(col1Panel);
    tunningPanel.add(col2Panel);
    tunningPanel.add(col3Panel);


    JPanel linePanel = new JPanel();
    linePanel.setLayout(gridbag);
    c.gridwidth = 2;
    c.gridheight = GridBagConstraints.REMAINDER;    
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    gridbag.setConstraints(plotDefLabel, c);
    linePanel.add(plotDefLabel);
    c.weightx = 10000;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(plotDefTXF, c);
    linePanel.add(plotDefTXF);


    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    gridbag.setConstraints(showBUT, c);
    buttonPanel.add(showBUT);
    //gridbag.setConstraints(selectFileBUT, c);
    //buttonPanel.add(selectFileBUT);

    JPanel plotPanel = new JPanel();
    plotPanel.setLayout(gridbag);
    c.gridwidth = GridBagConstraints.REMAINDER;;
    c.gridheight = 3;
    gridbag.setConstraints(tunningPanel, c);
    plotPanel.add(tunningPanel);
    gridbag.setConstraints(linePanel, c);
    plotPanel.add(linePanel);
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(buttonPanel, c);
    plotPanel.add(buttonPanel);
    
    
    // Add widget to jEnergyBandPanel
    c.gridheight = 2;
    c.weighty =10000;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(scrollPane1, c);
    jEnergyBandPanel.add(scrollPane1);
    c.weighty = 1;
    c.weightx = 1;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(plotPanel, c);
    jEnergyBandPanel.add(plotPanel);
    
    return jEnergyBandPanel;

  }    //end makeEnergyBandPanel

  protected void updateEnergyBandPanel() {
    
    //Do we have an EnergyBand?
    if(hasEnergyBand) {
      tabbedPane.setEnabledAt(3, true);
    } else { 
      tabbedPane.setEnabledAt(3, false);
      tabbedPane.setSelectedIndex(0); 
    }
    
    if(hasEnergyBand) {
      Object data[][]=new Object[energyBand.getNumberOfKLines()][6];
      for (int i=0; i< energyBand.getNumberOfKLines();i++) {
	data[i][0] = new String(String.valueOf(i));
	data[i][1] = new Point3d(energyBand.getKLine(i).getOrigin());
	data[i][2] = new String(energyBand.getKLine(i).getOriginName());
	data[i][3] = new Point3d(energyBand.getKLine(i).getEnd());
	data[i][4] = new String(energyBand.getKLine(i).getEndName());
	data[i][5] = new String
	  (String.valueOf(energyBand.getKLine(i).getNumberOfkPoints()));
      }
      energyLinesTableModel.setData(data);
      unitsCBO.setSelectedIndex(energyBand.getEnergyUnits()-100);
      minETXF.setText(Double.toString(energyBand.getMinE()));    
      maxETXF.setText(Double.toString(energyBand.getMaxE()));
      fermiETXF.setText(Double.toString(energyBand.getFermiE()));
      unitsComboOldIndex=unitsCBO.getSelectedIndex();
    }
  }
  
  
  protected void showEnergyBandPlot() {
    JmolResourceHandler resources = JmolResourceHandler.getInstance();
    int roundScheme=0;
    switch(roundSchemeCBO.getSelectedIndex()) {
    case 0: roundScheme = Rounder.FIX; break;
    case 1: roundScheme = Rounder.EXP; break;
    }
    // Create a new EnergyBandPlot
    create:
    {
      try {
	bandPlot 
	  = new BandPlot(energyBand, 
			       plotDefTXF.getText(),
			       unitsCBO.getSelectedIndex()+100,
			       FieldReader.readField1(minETXF),
			       FieldReader.readField1(maxETXF),
			       FieldReader.readField1(fermiETXF),
			       (int)FieldReader.readField1(nRoundTXF), 
			       roundScheme,
			       (int)FieldReader.readField1(nvTicsTXF),
			       (int)FieldReader.readField1(nhTicsTXF),
			       (int)FieldReader.readField1(ticSizeTXF),
			       FieldReader.readField1(fontsize1TXF),
			       FieldReader.readField1(fontsize2TXF),
			       FieldReader.readField1(fontsize3TXF),
			       FieldReader.readField1(sectionSepTXF),
			       yLabelTXF.getText());
	
      } catch (BandPlot.ParseErrorException peEc) {
	System.out.println ("Parse Error");
	break create;
      }
      
      BandPlotG2DRenderer ebpr
	= new BandPlotG2DRenderer
	(bandPlot,
	 FieldReader.readField1(resolutionTXF),
	 FieldReader.readField1(ratioTXF));
      
      // Create a new frame containing the plot and a "Save As..." button
      //
      //  Logical:
      //  
      //                         /- scrollPane - plot_PNL
      //   f_FRM--container_PNL--|
      //                         \- button_PNL - saveAsBUT
      //
      //  
      //  Visual:
      //
      //    -----------------
      //    |              "|
      //    |              "|
      //    |    plot_PNL  "|
      //    |              "|
      //    |=============="|
      //    |---------------|
      //    |   button_PNL  |
      //    |---------------|
      
      
      final JFrame f_FRM = new JFrame("Energy Band Plot");
      f_FRM.addWindowListener(new WindowAdapter() {
	  public void windowClosing(WindowEvent e) {
	    f_FRM.dispose();
	  }
	});
      
      JPanel plot_PNL =ebpr.getJPanel();
      int height = (int)ebpr.getPlotHeight();
      int width = (int)ebpr.getPlotLength();
      plot_PNL.setPreferredSize(new Dimension(width+5,
					      height+5));
      JScrollPane scrollPane = new JScrollPane(plot_PNL);    
      
      // Save as button
      JButton saveAsBUT = new JButton
	(resources.getString("Crystalprop.ebTab.saveAs"));
      fileChooserReturnVal = JFileChooser.CANCEL_OPTION;
      saveAsBUT.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    chooser = new JFileChooser();
	    fileChooserReturnVal = chooser.showDialog
	      (null,JmolResourceHandler.getInstance().getString("Crystalprop.ebTab.saveEPS"));
	    
	    //If a file was selected
	    if (fileChooserReturnVal == JFileChooser.APPROVE_OPTION) {
	      // Draw the band plot as a PostScript
	      try {
		BandPlotEPSRenderer ebprT
		  = new BandPlotEPSRenderer
		  (bandPlot,
		   FieldReader.readField1(resolutionTXF),
		   FieldReader.readField1(ratioTXF),
		   chooser.getSelectedFile());
		ebprT.generateEPS();
		
	      } catch (IOException ex) {
		System.out.println ("Error while writing the file");
	      }
	    }
	  }
	});
      
      JButton dismissBUT = new JButton(resources.translate("Dismiss"));
      dismissBUT.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    f_FRM.dispose();
	  }
	  
	});
      
      JPanel button_PNL = new JPanel();
      button_PNL.setLayout(new FlowLayout(FlowLayout.RIGHT));    
      button_PNL.add(saveAsBUT);        
      button_PNL.add(dismissBUT);        
      
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      JPanel container_PNL = new JPanel();  
      container_PNL.setLayout(gridbag);
      c.weightx = 1;
      c.weighty = 100000;
      c.fill = GridBagConstraints.BOTH;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(scrollPane, c);
      container_PNL.add(scrollPane);    
      c.weighty = 1; 
      c.gridheight = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(button_PNL, c);
      container_PNL.add(button_PNL);    
      
      
      f_FRM.getContentPane().add(container_PNL, BorderLayout.CENTER);
      
      
      f_FRM.setSize(new Dimension(width < 700 ? width + 50 : 700,
				  height < 700 ? height + 90 : 700));
      f_FRM.setVisible(true);
      
    } //label "create"
  }
  
  
  /**
   * Describe <code>makeSpaceGroupPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeSpaceGroupPanel() {

    JPanel jSpgroupPanel = new JPanel();

    //jSpgroupPanel.setLayout(new BorderLayout());

    return jSpgroupPanel;

  }    //end makeSpaceGroupPanel

  /**
   * Update panel with dummy information.
   * Used when no crystal info is available.
   */
  protected void updateBarePanels() {

    primitiveVectorTypeCBO.setSelectedIndex(0);
    
    
    ((JTextField) (jRprim_VEC_TXF.elementAt(0))).setText("1.0, 0.0, 0.0");
    ((JTextField) (jRprim_VEC_TXF.elementAt(1))).setText("0.0, 1.0, 0.0");
    ((JTextField) (jRprim_VEC_TXF.elementAt(2))).setText("0.0, 0.0, 1.0");
    ((JTextField) (jAcell_VEC_TXF.elementAt(0))).setText("1.0");
    ((JTextField) (jAcell_VEC_TXF.elementAt(1))).setText("1.0");
    ((JTextField) (jAcell_VEC_TXF.elementAt(2))).setText("1.0");
    
    ((JTextField) (jEdges_VEC_TXF.elementAt(0))).setText("1.0");
    ((JTextField) (jEdges_VEC_TXF.elementAt(1))).setText("1.0");
    ((JTextField) (jEdges_VEC_TXF.elementAt(2))).setText("1.0");
    ((JTextField) (jAngles_VEC_TXF.elementAt(0))).setText("90.0");
    ((JTextField) (jAngles_VEC_TXF.elementAt(1))).setText("90.0");
    ((JTextField) (jAngles_VEC_TXF.elementAt(2))).setText("90.0");
    
    for (int i = 0; i < 3; i++) {
      ((JTextField) (jRprim_VEC_TXF.elementAt(i))).setEditable(true);
      ((JTextField) (jAcell_VEC_TXF.elementAt(i))).setEditable(true);
      ((JTextField) (jEdges_VEC_TXF.elementAt(i))).setEditable(false);
      ((JTextField) (jAngles_VEC_TXF.elementAt(i))).setEditable(false);
    }
    

    // crystal box
    
    ((JTextField) (jAtomBox_VEC_TXF.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jAtomBox_VEC_TXF.elementAt(1))).setText("1.0, 1.0, 1.0");
    ((JTextField) (jBondBox_VEC_TXF.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jBondBox_VEC_TXF.elementAt(1))).setText("1.0, 1.0, 1.0");
    ((JTextField) (jUnitBox_VEC_TXF.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jUnitBox_VEC_TXF.elementAt(1))).setText("1.0, 1.0, 1.0");
    
    // Display other usefull information:
    // the number of atoms in the unit cell
    jNatomInCellLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NCell")
       + " " + chemFile.getFrame(currentFrameIndex).getAtomCount());
    
    // the number of atoms in the crystal box
    jNatomInBoxLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
       + " " + chemFile.getFrame(currentFrameIndex).getAtomCount());
    
    jNatomInClipLBL.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NbondBox")
       + " " + chemFile.getFrame(currentFrameIndex).getAtomCount());
    
  }
  
  
  class BasisVTableModel extends AbstractTableModel {

    private String[] columnNames = {
      JmolResourceHandler.getInstance().translate("Atom Number"),
      JmolResourceHandler.getInstance().translate("Atom Type"), "x", "y", "z"
    };
    private Object[][] data = {
    };


    public void setData(Object[][] data) {
      this.data = data;
    }

    public int getColumnCount() {
      return columnNames.length;
    }

    public int getRowCount() {
      return data.length;
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

      //Note that the data/cell address is constant,
      //no matter where the cell appears onscreen.
      if (col < 5) {    //no cell editable
        return false;
      } else {
        return true;
      }
    }

    public void setValueAt(Object value, int row, int col) {
      data[row][col] = value;
      fireTableCellUpdated(row, col);
    }

  }    //end class BasisVTableModel

  class EnergyLinesTableModel extends AbstractTableModel {

    private String[] columnNames = {
      JmolResourceHandler.getInstance().translate("Line Number"),
      JmolResourceHandler.getInstance().translate("Origin"),
      JmolResourceHandler.getInstance().translate("Origin Label"),      
      JmolResourceHandler.getInstance().translate("End"),
      JmolResourceHandler.getInstance().translate("End Label"),
      JmolResourceHandler.getInstance().translate("Number Of Points")
    };
    private Object[][] data = {
    };
    
    
    public void setData(Object[][] data) {
      this.data = data;
    }

    public int getColumnCount() {
      return columnNames.length;
    }
    
    public int getRowCount() {
      return data.length;
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
      
      //Note that the data/cell address is constant,
      //no matter where the cell appears onscreen.
      if (col == 2 || col == 4) {    //no cell editable
        return true;
      } else {
        return false;
      }
    }
    
    public void setValueAt(Object value, int row, int col) {
      data[row][col] = value;
      fireTableCellUpdated(row, col);
      
      // Save value in energyBand object
      switch(col) {
      case 2:energyBand.getKLine(row).setOriginName((String)value); break;
      case 4:energyBand.getKLine(row).setEndName((String)value); break;
      }
      
    }
  }    //end class EnergyLinesTableModel
  
  private void commitChange() {

    boolean isPrimVectorsCart;
    double[][] rprim;
    double[] acell;
    double[] edges;
    double[] angles;
    double[][] atomBox;
    double[][] bondBox;
    double[][] unitBox;
    UnitCellBox unitCellBox;
    CrystalBox crystalBox;
    
    // Read text from the various text fields
    // and set it in the CrystalFile object.
    
    commit:
    {
      String unreadField="";
      try {
	unreadField = "rprim";
	rprim = FieldReader.readField3(jRprim_VEC_TXF);
	unreadField = "acell";
	acell = FieldReader.readField1(jAcell_VEC_TXF);
	unreadField = "edges";
	edges = FieldReader.readField1(jEdges_VEC_TXF);
	unreadField = "angles";
	angles = FieldReader.readField1(jAngles_VEC_TXF);
	unreadField = "atomBox";
	atomBox = FieldReader.readField3(jAtomBox_VEC_TXF);
	unreadField = "bondBox";
	bondBox = FieldReader.readField3(jBondBox_VEC_TXF);
	unreadField = "unitBox";
	unitBox = FieldReader.readField3(jUnitBox_VEC_TXF);
      } catch (NumberFormatException nfe) {
	System.out.println(nfe.getMessage());
	errorDialog(JmolResourceHandler.getInstance().translate
		    ("Ooups! The value you entered in the field \"" +
		     unreadField + 
		     "\" is not a valid number."));
	break commit; //Exit the commitChange method
      }
      
      // In case of a classical ChemFile has been loaded,
      // a CrystalFile is created.
      if (hasCrystalInfo == false) {
	this.crystalFile = new CrystalFile(viewer, chemFile, rprim, acell);
      this.chemFile = (ChemFile) crystalFile;
      
      // Say to everybody that we have a new chemfile!
      viewer.openClientFile(null, "Crystal", this.chemFile);
      hasCrystalInfo = true;
      primitiveVectors_ApplyToWhichFrameCBO.setEnabled(true);
      crystalBox_ApplyToWhichFrameCBO.setEnabled(true);
      }
      
      
      
      int frameCount = viewer.getFrameCount();
      for (int i = 0; i < frameCount; i++) {
	
	//set Primitive Vectors
	unitCellBox = crystalFile.getUnitCellBox(i);
	if (primitiveVectors_ApplyToWhichFrameCBO.getSelectedIndex() == 0
	    | ((primitiveVectors_ApplyToWhichFrameCBO.getSelectedIndex() == 1) 
	       && (i == currentFrameIndex))) {
	  if (primitiveVectorTypeCBO.getSelectedIndex() == 0) {   
	    //Cartesian repres.
	    unitCellBox.setPrimVectorsCartesian(rprim, acell);
	  } else {    
	    //Crystallographic representation
	    unitCellBox.setPrimVectorsCrystallo(edges, angles);
	  }
	}
	crystalFile.setUnitCellBox(unitCellBox);
	
	//set Crystal Box
	crystalBox = crystalFile.getCrystalBox(i);
	
	// Set the translation type
	// ORIGINAL, INBOX or CRYSTAL
	crystalBox.setTranslationType(translationTypeCBO.getSelectedIndex());

	if (crystalBox_ApplyToWhichFrameCBO.getSelectedIndex() == 0
	    | ((crystalBox_ApplyToWhichFrameCBO.getSelectedIndex() == 1) 
	       && (i == currentFrameIndex))) {
	  crystalBox.setAtomBox(atomBox);
	  crystalBox.setBondBox(bondBox);
	  crystalBox.setUnitBox(unitBox);
	  
	}
	crystalFile.setCrystalBox(crystalBox);
	
	if (((primitiveVectors_ApplyToWhichFrameCBO.getSelectedIndex() == 1) 
	     && (crystalBox_ApplyToWhichFrameCBO.getSelectedIndex() == 1) 
	     && (i == currentFrameIndex))
	    | !((primitiveVectors_ApplyToWhichFrameCBO.getSelectedIndex() == 1)
		&& (crystalBox_ApplyToWhichFrameCBO.getSelectedIndex() == 1)))
	  {
	  
	  
	  crystalFile.generateCrystalFrame(i);
	}
      }
      
      //Display the updated ChemFile
      restoreInFile();
    }
  }    //end commitChange()
  
  private void updateDialog() {

    
    // Reput(or put it for the first time)
    // the text in the box so that the user
    // can see if his input has been interpreted correctly.

    if (hasCrystalInfo) {
      
      unitCellBox= crystalFile.getUnitCellBox(currentFrameIndex);
      crystalBox= crystalFile.getCrystalBox(currentFrameIndex);
      currentFrame = crystalFile.getFrame(currentFrameIndex);

      // The primitive Vectors
      updatePrimVectorsPanel();
 
      // The atom and bond box
      updateCrystalBoxPanel();
      
      // The basis Vectors
      updateBasisVectorsPanel();

      // The EnergyBand
      updateEnergyBandPanel();


    } else {    // hasNoCrystalInfo
      
      // Put dummy values
      updateBarePanels();

      // Update Basis Vectors. The basis vector are simply 
      // all the atoms in the frame
      updateBasisVectorsPanel();
      
    }  //endif hasCrystalInfo


  }  //end updateDialog()

  private void updateCurrentFrameIndex() {
  
    int primIndex = primitiveVectors_ApplyToWhichFrameCBO.getSelectedIndex();
    int boxIndex = crystalBox_ApplyToWhichFrameCBO.getSelectedIndex();
    primitiveVectors_ApplyToWhichFrameCBO.removeItem(applyToList[1]);
    primitiveVectors_ApplyToWhichFrameCBO.removeItem(applyToList[0]);
    crystalBox_ApplyToWhichFrameCBO.removeItem(applyToList[1]);
    crystalBox_ApplyToWhichFrameCBO.removeItem(applyToList[0]);

    currentFrameIndex = Animate.currentFrame;
    String s = new String("(" + currentFrameIndex + ")");
    applyToList[0] =
      JmolResourceHandler.getInstance().translate("Apply to all frames");
    applyToList[1] =
      JmolResourceHandler.getInstance().translate("Apply to current frame ")
      + s;

    primitiveVectors_ApplyToWhichFrameCBO.addItem(applyToList[0]);
    primitiveVectors_ApplyToWhichFrameCBO.addItem(applyToList[1]);
    primitiveVectors_ApplyToWhichFrameCBO.setSelectedIndex(primIndex);
    crystalBox_ApplyToWhichFrameCBO.addItem(applyToList[0]);
    crystalBox_ApplyToWhichFrameCBO.addItem(applyToList[1]);
    crystalBox_ApplyToWhichFrameCBO.setSelectedIndex(boxIndex);

    
    //Check if we have an "EnergyBand" property
    hasEnergyBand=false;    
    if (hasCrystalInfo) {
      Vector frameProperties 
	= crystalFile.getFrame(currentFrameIndex).getFrameProperties();
      for (int i=0; i< frameProperties.size();i++) {
	if(((PhysicalProperty)frameProperties.elementAt(i)).getDescriptor()
	   .equals("EnergyBand")) {
	  energyBand =
	    (EnergyBand)frameProperties.elementAt(i);
	  hasEnergyBand=true;
	}
      }
    }
    
  }
  

  /**
   * Describe <code>errorDialog</code> method here.
   *
   * @param s a <code>String</code> value
   */
  protected void errorDialog(String s) {

    JOptionPane.showMessageDialog
      (null, s, "alert", JOptionPane.ERROR_MESSAGE);

  }    //end errorDialog

  private void restoreInFile() {

    this.chemFile = (ChemFile) crystalFile;
    hasFile = true;

    viewer.openClientFile(null, "CrystalProp", this.chemFile);
    viewer.setFrame(currentFrameIndex);

    //The chemfile is updated globally.
    //jmol.setChemFile(this.chemFile);

    // Set if the crystprop Action is enabled 
    // or not (appear black or gray in the menu)
    if (this.isShowing()) {                 //the Crystal dialog is open
      crystpropAction.setEnabled(false);    //appear gray (disabled)
    } else {
      crystpropAction.setEnabled(true);     //appear black (enabled)
    }
  }



  /**
   * Set the Crystal properties of the file.
   * This function is called when a new file is opened.
   *
   * @param cf the ChemFile
   */
  public void setChemFile(ChemFile cf) {

    if (cf instanceof CrystalFile) {
      hasCrystalInfo = true;
      this.crystalFile = (CrystalFile) cf;
      primitiveVectors_ApplyToWhichFrameCBO.setEnabled(true);
      primitiveVectors_ApplyToWhichFrameCBO.setSelectedIndex(1);
      crystalBox_ApplyToWhichFrameCBO.setEnabled(true);
      crystalBox_ApplyToWhichFrameCBO.setSelectedIndex(0);
      basisVector_ApplyToWhichFrameCBO.setEnabled(true);
      basisVector_ApplyToWhichFrameCBO.setSelectedIndex(1);

      
    } else if (cf instanceof ChemFile) {
      hasCrystalInfo = false;
      this.chemFile = cf;

      // When a ChemFile is loaded, crystal parameters
      // can only be applied to *all* frames the first time
      // the ok or apply button is clicked.
      primitiveVectors_ApplyToWhichFrameCBO.setEnabled(false);
      primitiveVectors_ApplyToWhichFrameCBO.setSelectedIndex(0);
      crystalBox_ApplyToWhichFrameCBO.setEnabled(false);
      crystalBox_ApplyToWhichFrameCBO.setSelectedIndex(0);
      basisVector_ApplyToWhichFrameCBO.setEnabled(false);
      basisVector_ApplyToWhichFrameCBO.setSelectedIndex(0);
    }

    //Hide CystalPropertiesDialog if we have a normal ChemFile
    if (!hasCrystalInfo) {
      thisDialog.setVisible(false);
    }

    // Set if the crystprop Action is enabled 
    // or not (appear black or gray in the menu)
    if (this.isShowing()) {                 //the Crystal dialog is open
      crystpropAction.setEnabled(false);    //appear gray (disabled)
    } else if (!hasCrystalInfo) {
      crystpropAction.setEnabled(false);    //appear gray (disabled)
    } else if (hasCrystalInfo && !this.isShowing()) {
      crystpropAction.setEnabled(true);  //appear black (enabled)
    }
    hasFile = true;
    
    //Need to refresh the dialog box in case the dialog was open
    //when a new file was loaded.
    updateCurrentFrameIndex();
    updateDialog();
  } //end setChemFile(ChemFile)


  /**
   * Describe <code>centerDialog</code> method here.
   *
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
   * Describe <code>close</code> method here.
   *
   */
  public void close() {

    //restoreInFile();
    this.setVisible(false);
    if (hasCrystalInfo) {
      crystpropAction.setEnabled(true);
    } else {
      crystpropAction.setEnabled(false);
      MakeCrystal.setEnabled(true);
    }
  }
  

  /**
   * Describe <code>actionPerformed</code> method here.
   *
   * @param evt an <code>ActionEvent</code> value
   */
  public void actionPerformed(ActionEvent evt) {
  }


  class CrystpropAction extends AbstractAction {

    public CrystpropAction() {

      super("crystprop");

      //The crystprop dialog is available only if a file is loaded
      if (hasFile) {
	this.setEnabled(true);
      } else {
	this.setEnabled(false);
      }
    }

    public void actionPerformed(ActionEvent e) {

      //When the dialog Crystal-->Properties is clicked,
      //this method is executed.

      //The crystprop dialog is no more available because already opened
      this.setEnabled(false);
      //The Makecrytal action is no more available...
      MakeCrystal.setEnabled(false);

      //Update the content of the dialog box
      updateCurrentFrameIndex();
      updateDialog();


      //Show the dialog box
      show();
    }


  }    //end class CrystpropAction 



  /**
   * Describe <code>getActions</code> method here.
   *
   * @return an <code>Action[]</code> value
   */
  public Action[] getActions() {
    Action[] defaultActions = {
      crystpropAction
    };
    return defaultActions;
  }

  /**
   * Describe <code>getAction</code> method here.
   *
   * @param cmd a <code>String</code> value
   * @return an <code>Action</code> value
   */
  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  class CrystpropWindowListener extends WindowAdapter {

    public void windowClosing(WindowEvent e) {
      close();
    }
  }


  class NondragChangeListener implements ChangeListener {

    private boolean overrideIsAdjusting = false;
    public NondragChangeListener() {

      // Workaround for documented bug 4246117 for JDK 1.2.2
      if (System.getProperty("java.version").equals("1.2.2")
	  && System.getProperty("java.vendor").startsWith("Sun Micro")) {
	overrideIsAdjusting = true;
      }
    }

    public void stateChanged(ChangeEvent e) {
    }

  }    //end class NondragChangeListener



  /**
   * Describe <code>propertyChange</code> method here.
   *
   * @param event a <code>PropertyChangeEvent</code> value
   */
  public void propertyChange(PropertyChangeEvent event) {

    if (event.getPropertyName().equals(Jmol.chemFileProperty)) {
      if (event.getNewValue() != chemFile) {
	setChemFile((ChemFile) event.getNewValue());
      }
    }

    //if (event.getPropertyName().equals(JmolModel.chemFrameProperty)) {
    //System.out.println(event.getNewValue().);

    // Find a way to get the index of the active frame
    // but it could slow down the animate action.

    //}
  }

}    //end calls CrystalPropertiesDialog


