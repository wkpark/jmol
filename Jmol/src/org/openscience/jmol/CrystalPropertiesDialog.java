/*
 * Copyright 2002 The Jmol Development Team
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

import javax.swing.table.AbstractTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.TitledBorder;
import java.util.StringTokenizer;
import java.util.Vector;
import java.lang.reflect.Array;
import javax.swing.JTabbedPane;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
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
  
  /**
   * Reference to the data model.
   */
  private JmolModel model;

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
  JComboBox primVTypeList;
  Vector jRprim = new Vector(3);    //basis vectors text field
  Vector jRprimLabel = new Vector(3);
  Vector jAcell = new Vector(3);
  Vector jAcellLabel = new Vector(3);
  Vector jEdges = new Vector(3);
  Vector jEdgesLabel = new Vector(3);
  Vector jAngles = new Vector(3);
  Vector jAnglesLabel = new Vector(3);
  JComboBox primApplyTo;        // apply to which frame?
  String[] applyToList = new String[2];

  //Crystal Box fields
  JCheckBox origAtomsOnly;
  Vector jAtomBox = new Vector(2);
  Vector jAtomBoxLabel = new Vector(2);
  Vector jBondBox = new Vector(2);
  Vector jBondBoxLabel = new Vector(2);
  Vector jUnitBox = new Vector(2);
  Vector jUnitBoxLabel = new Vector(2);

  JComboBox boxApplyTo;    //aplly to which frame?

  JLabel jNatomInBoxInfo;
  JLabel jNatomInCellInfo;
  JLabel jNatomInClipInfo;

  //Basis Vectors fields
  JComboBox basisVectorTypeList;
  BasisVTableModel basisVectorTableModel = new BasisVTableModel();
  JComboBox basisApplyTo;

  //Energy Band fields

  EnergyLinesTableModel energyLinesTableModel = new EnergyLinesTableModel();
  EnergyLineTableModel energyLineTableModel = new EnergyLineTableModel();
  

  // The actions:

  private CrystpropAction crystpropAction = new CrystpropAction();
  private Hashtable commands;


  /**
   * Constructor
   *
   * @param model a <code>JmolModel</code> value
   * @param f the parent frame
   */
  public CrystalPropertiesDialog(JmolModel model, JFrame f) {


    // Invoke JDialog constructor
    super(f, "Crystal Properties...", false);
    this.thisDialog = this;
    this.model = model;
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

    //Component panel4 = makeEnergyBandPanel();
    //tabbedPane.addTab(resources.getString("Crystprop.energyBandLabel"),
    //    resources.getIcon("Crystprop.energyBandImage"), panel4,
    //      resources.getString("Crystprop.energyBandToolTip"));
    ////EnergyBand is disabled by default
    //tabbedPane.setEnabledAt(3,false);

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
    
    int index = basisVectorTypeList.getSelectedIndex();
      
      if (hasCrystalInfo) {
	int natom = unitCellBox.getNumberOfAtoms();
	Object[][] basisVectorData = new Object[natom][5];
	Atom atom;
	
	
	if (index == 0) {    //Cartesian coord.
        double[][] cartPos = unitCellBox.getCartesianPos();
        for (int i = 0; i < natom; i++) {
          basisVectorData[i][0] = new String(String.valueOf(i));
          basisVectorData[i][1] = new String(unitCellBox.getAtomType(i).getName());
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
          basisVectorData[i][1] = new String(unitCellBox.getAtomType(i).getName());
          basisVectorData[i][2] = new Double(redPos[i][0]);
          basisVectorData[i][3] = new Double(redPos[i][1]);
          basisVectorData[i][4] = new Double(redPos[i][2]);
        }
        basisVectorTableModel.setData(basisVectorData);
        basisVectorTableModel.fireTableDataChanged();
      }
    } else {               //is not a crystal
      if (index == 1) {
        basisVectorTypeList.setSelectedIndex(0);
        String message =
          JmolResourceHandler.getInstance().translate(
            "You must define the primitive vectors to use lattice coordinates");
        errorDialog(message);
        System.out.println(message);
      }

      int natom = chemFile.getFrame(currentFrameIndex).getNumberOfAtoms();
      Object[][] basisVectorData = new Object[natom][5];
      Atom atom;
      for (int i = 0; i < natom; i++) {
        atom = chemFile.getFrame(currentFrameIndex).getAtomAt(i);
        basisVectorData[i][0] = new String(String.valueOf(i));
        basisVectorData[i][1] = new String(atom.getType().getName());
        basisVectorData[i][2] = new Double(atom.getPosition().x);
        basisVectorData[i][3] = new Double(atom.getPosition().y);
        basisVectorData[i][4] = new Double(atom.getPosition().z);
      }
      basisVectorTableModel.setData(basisVectorData);
      basisVectorTableModel.fireTableDataChanged();

    }
  }                        //end updateBasisVectorsPanel



  /**
   * This method set which of the cartesian or crystallographic
   * primitive vectors is editable.
   * Depends of primVTypeList state
   *
   * @param index an <code>int</code> value
   */
  protected void setPrimVectorsState() {

    //Set what is editable depending on the combo box value
    int index = primVTypeList.getSelectedIndex();
    if (index == 0) {
      for (int i = 0; i < 3; i++) {
        ((JTextField) (jRprim.elementAt(i))).setEditable(true);
        ((JTextField) (jAcell.elementAt(i))).setEditable(true);
        ((JTextField) (jEdges.elementAt(i))).setEditable(false);
        ((JTextField) (jAngles.elementAt(i))).setEditable(false);
      }
    } else if (index == 1) {
      for (int i = 0; i < 3; i++) {
        ((JTextField) (jRprim.elementAt(i))).setEditable(false);
        ((JTextField) (jAcell.elementAt(i))).setEditable(false);
        ((JTextField) (jEdges.elementAt(i))).setEditable(true);
        ((JTextField) (jAngles.elementAt(i))).setEditable(true);
      }
    }
  }    //end setPrimVectorsState


  /**
   * Make usage or not of atomBox, bondBox, unitBox 
   * Depends of origAtomsOnly state
   */
  protected void setCrystalBoxState() {

    //Set what is editable depending on the checkbox value
    if (origAtomsOnly.isSelected()) {
      for (int i = 0; i < 2; i++) {
        ((JTextField) (jAtomBox.elementAt(i))).setEditable(false);
        ((JTextField) (jBondBox.elementAt(i))).setEditable(false);
        ((JTextField) (jUnitBox.elementAt(i))).setEditable(false);
      }
    } else {
      for (int i = 0; i < 2; i++) {
        ((JTextField) (jAtomBox.elementAt(i))).setEditable(true);
        ((JTextField) (jBondBox.elementAt(i))).setEditable(true);
        ((JTextField) (jUnitBox.elementAt(i))).setEditable(true);
      }
    }
  }    //end setCrystalBoxState
  
  
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
    primVTypeList = new JComboBox(primVTypeStrings);
    primVTypeList.setSelectedIndex(0);
    primVTypeList.addActionListener(new ActionListener() {

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
    jRprim.addElement(new JTextField(22));
    jRprim.addElement(new JTextField(22));
    jRprim.addElement(new JTextField(22));
    jRprimLabel.addElement(new JLabel("1:", SwingConstants.RIGHT));
    jRprimLabel.addElement(new JLabel("2:", SwingConstants.RIGHT));
    jRprimLabel.addElement(new JLabel("3:", SwingConstants.RIGHT));
    ((JTextField) (jRprim.elementAt(0)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.rprim1Tooltip"));
    ((JTextField) (jRprim.elementAt(1)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.rprim2Tooltip"));
    ((JTextField) (jRprim.elementAt(2)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.rprim3Tooltip"));

    //The 3 acell text box
    jAcell.addElement(new JTextField(7));
    jAcell.addElement(new JTextField(7));
    jAcell.addElement(new JTextField(7));
    jAcellLabel.addElement(new JLabel("*", SwingConstants.RIGHT));
    jAcellLabel.addElement(new JLabel("*", SwingConstants.RIGHT));
    jAcellLabel.addElement(new JLabel("*", SwingConstants.RIGHT));
    ((JTextField) (jAcell.elementAt(0)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.acell1Tooltip"));
    ((JTextField) (jAcell.elementAt(1)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.acell2Tooltip"));
    ((JTextField) (jAcell.elementAt(2)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.acell3Tooltip"));



    //The jCryst subPanel (level 1)
    JPanel jCryst = new JPanel();
    jCryst.setLayout(gridbag);
    jCryst.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Crystprop.crystalloLabel")));

    //The 3 edges (Crystallographic representation)
    jEdges.addElement(new JTextField(7));
    jEdges.addElement(new JTextField(7));
    jEdges.addElement(new JTextField(7));
    jEdgesLabel.addElement(new JLabel("a:", SwingConstants.RIGHT));
    jEdgesLabel.addElement(new JLabel("b:", SwingConstants.RIGHT));
    jEdgesLabel.addElement(new JLabel("c:", SwingConstants.RIGHT));
    ((JTextField) (jEdges.elementAt(0)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.edgeaTooltip"));
    ((JTextField) (jEdges.elementAt(1)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.edgebTooltip"));
    ((JTextField) (jEdges.elementAt(2)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.edgecTooltip"));


    //The 3 angles
    jAngles.addElement(new JTextField(7));
    jAngles.addElement(new JTextField(7));
    jAngles.addElement(new JTextField(7));
    jAnglesLabel.addElement(new JLabel("Alpha:", SwingConstants.RIGHT));
    jAnglesLabel.addElement(new JLabel("Beta:", SwingConstants.RIGHT));
    jAnglesLabel.addElement(new JLabel("Gamma:", SwingConstants.RIGHT));
    ((JTextField) (jAngles.elementAt(0)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.angleaTooltip"));
    ((JTextField) (jAngles.elementAt(1)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.anglebTooltip"));
    ((JTextField) (jAngles.elementAt(2)))
        .setToolTipText(JmolResourceHandler.getInstance()
          .getString("Crystprop.anglecTooltip"));


    //Apply to all/current frame radio box
    primApplyTo = new JComboBox(applyToList);
    primApplyTo.setSelectedIndex(1);



    //add level 2 widgets to level 1 panels

    //   add widgets to jPrimVType panel
    c.gridwidth = 2;
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTHWEST;
    gridbag.setConstraints(jPrimVTypeLabel, c);
    jPrimVType.add(jPrimVTypeLabel);
    c.weightx = 100000;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(primVTypeList, c);
    jPrimVType.add(primVTypeList);


    //   add widgets to jCart Panel.
    //c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHEAST;
    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprimLabel.elementAt(0), c);
    jCart.add((JLabel) jRprimLabel.elementAt(0));
    gridbag.setConstraints((JTextField) jRprim.elementAt(0), c);
    jCart.add((JTextField) jRprim.elementAt(0));
    gridbag.setConstraints((JLabel) jAcellLabel.elementAt(0), c);
    jCart.add((JLabel) jAcellLabel.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell.elementAt(0), c);
    jCart.add((JTextField) jAcell.elementAt(0));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprimLabel.elementAt(1), c);
    jCart.add((JLabel) jRprimLabel.elementAt(1));
    gridbag.setConstraints((JTextField) jRprim.elementAt(1), c);
    jCart.add((JTextField) jRprim.elementAt(1));
    gridbag.setConstraints((JLabel) jAcellLabel.elementAt(1), c);
    jCart.add((JLabel) jAcellLabel.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell.elementAt(1), c);
    jCart.add((JTextField) jAcell.elementAt(1));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jRprimLabel.elementAt(2), c);
    jCart.add((JLabel) jRprimLabel.elementAt(2));
    gridbag.setConstraints((JTextField) jRprim.elementAt(2), c);
    jCart.add((JTextField) jRprim.elementAt(2));
    gridbag.setConstraints((JLabel) jAcellLabel.elementAt(2), c);
    jCart.add((JLabel) jAcellLabel.elementAt(2));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAcell.elementAt(2), c);
    jCart.add((JTextField) jAcell.elementAt(2));

    //   add widgets to jCryst Panel.
    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdgesLabel.elementAt(0), c);
    jCryst.add((JLabel) jEdgesLabel.elementAt(0));
    gridbag.setConstraints((JTextField) jEdges.elementAt(0), c);
    jCryst.add((JTextField) jEdges.elementAt(0));
    gridbag.setConstraints((JLabel) jAnglesLabel.elementAt(0), c);
    jCryst.add((JLabel) jAnglesLabel.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles.elementAt(0), c);
    jCryst.add((JTextField) jAngles.elementAt(0));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdgesLabel.elementAt(1), c);
    jCryst.add((JLabel) jEdgesLabel.elementAt(1));
    gridbag.setConstraints((JTextField) jEdges.elementAt(1), c);
    jCryst.add((JTextField) jEdges.elementAt(1));
    gridbag.setConstraints((JLabel) jAnglesLabel.elementAt(1), c);
    jCryst.add((JLabel) jAnglesLabel.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles.elementAt(1), c);
    jCryst.add((JTextField) jAngles.elementAt(1));

    c.gridwidth = 4;
    gridbag.setConstraints((JLabel) jEdgesLabel.elementAt(2), c);
    jCryst.add((JLabel) jEdgesLabel.elementAt(2));
    gridbag.setConstraints((JTextField) jEdges.elementAt(2), c);
    jCryst.add((JTextField) jEdges.elementAt(2));
    gridbag.setConstraints((JLabel) jAnglesLabel.elementAt(2), c);
    jCryst.add((JLabel) jAnglesLabel.elementAt(2));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAngles.elementAt(2), c);
    jCryst.add((JTextField) jAngles.elementAt(2));


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
    gridbag.setConstraints(primApplyTo, c);
    jPrimVPanel.add(primApplyTo);

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
      primVTypeList.setSelectedIndex(0);
      setPrimVectorsState();
    } else {
      primVTypeList.setSelectedIndex(1);
      setPrimVectorsState();
    }

    double[][] rprim = unitCellBox.getRprim();
    double[] acell = unitCellBox.getAcell();
    ((JTextField) (jRprim.elementAt(0)))
      .setText(rprim[0][0] + ", " + rprim[0][1] + ", " + rprim[0][2]);
    ((JTextField) (jRprim.elementAt(1)))
      .setText(rprim[1][0] + ", " + rprim[1][1] + ", " + rprim[1][2]);
    ((JTextField) (jRprim.elementAt(2)))
      .setText(rprim[2][0] + ", " + rprim[2][1] + ", " + rprim[2][2]);
    ((JTextField) (jAcell.elementAt(0))).setText(acell[0] + "");
    ((JTextField) (jAcell.elementAt(1))).setText(acell[1] + "");
    ((JTextField) (jAcell.elementAt(2))).setText(acell[2] + "");
    
    double[] edges = unitCellBox.getEdges();
    double[] angles = unitCellBox.getAngles();
    ((JTextField) (jEdges.elementAt(0))).setText(edges[0] + "");
    ((JTextField) (jEdges.elementAt(1))).setText(edges[1] + "");
    ((JTextField) (jEdges.elementAt(2))).setText(edges[2] + "");
    ((JTextField) (jAngles.elementAt(0))).setText(angles[0] + "");
    ((JTextField) (jAngles.elementAt(1))).setText(angles[1] + "");
    ((JTextField) (jAngles.elementAt(2))).setText(angles[2] + "");
    
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

    //Panel that contains jAtomBox, the jBondBox and the jUnitBox panels
    JPanel jCrystBoxPanel = new JPanel();
    jCrystBoxPanel.setLayout(gridbag);

    origAtomsOnly = new JCheckBox
      (resources.getString("Crystprop.origatomCheckBox"));
    origAtomsOnly.addItemListener(new ItemListener() {
	
	public void itemStateChanged(ItemEvent e) {
	  setCrystalBoxState();
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

    jAtomBox.addElement(new JTextField(15));
    jAtomBoxLabel.addElement(
        new JLabel(
          resources.translate("Minimum atom box coordinate") + ": ",
            SwingConstants.LEFT));
    jAtomBox.addElement(new JTextField(15));
    jAtomBoxLabel.addElement(
        new JLabel(
          resources.translate("Maximum atom box coordinate") + ": ",
            SwingConstants.LEFT));
    jBondBox.addElement(new JTextField(15));
    jBondBoxLabel.addElement(
        new JLabel(
          resources.translate("Minimum bond box coordinate") + ": ",
            SwingConstants.LEFT));
    jBondBox.addElement(new JTextField(15));
    jBondBoxLabel.addElement(
        new JLabel(
          resources.translate("Maximum bond box coordinate") + ": ",
            SwingConstants.LEFT));
    jUnitBox.addElement(new JTextField(15));
    jUnitBoxLabel.addElement(
        new JLabel(
          resources.translate("Minimum unit box coordinate") + ": ",
            SwingConstants.LEFT));
    jUnitBox.addElement(new JTextField(15));
    jUnitBoxLabel.addElement(
        new JLabel(
          resources.translate("Maximum unit box coordinate") + ": ",
            SwingConstants.LEFT));

    jNatomInBoxInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NatomBox"), SwingConstants.LEFT);

    jNatomInClipInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NCell"), SwingConstants.LEFT);


    //AtomBox
    c.weightx = 1.0;
    c.weighty = 1.0;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = 4;
    c.gridwidth= GridBagConstraints.REMAINDER;
    gridbag.setConstraints(origAtomsOnly, c);
    jAtomBoxPanel.add(origAtomsOnly);
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jAtomBoxLabel.elementAt(0), c);
    jAtomBoxPanel.add((JLabel) jAtomBoxLabel.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAtomBox.elementAt(0), c);
    jAtomBoxPanel.add((JTextField) jAtomBox.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jAtomBoxLabel.elementAt(1), c);
    jAtomBoxPanel.add((JLabel) jAtomBoxLabel.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jAtomBox.elementAt(1), c);
    jAtomBoxPanel.add((JTextField) jAtomBox.elementAt(1));
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jNatomInBoxInfo, c);
    jAtomBoxPanel.add(jNatomInBoxInfo);

    //BondBox
    c.gridwidth = 2;
    c.gridheight = 3;
    gridbag.setConstraints((JLabel) jBondBoxLabel.elementAt(0), c);
    jBondBoxPanel.add((JLabel) jBondBoxLabel.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jBondBox.elementAt(0), c);
    jBondBoxPanel.add((JTextField) jBondBox.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jBondBoxLabel.elementAt(1), c);
    jBondBoxPanel.add((JLabel) jBondBoxLabel.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jBondBox.elementAt(1), c);
    jBondBoxPanel.add((JTextField) jBondBox.elementAt(1));
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jNatomInClipInfo, c);
    jBondBoxPanel.add(jNatomInClipInfo);

    //UnitBox
    c.gridwidth = 2;
    c.gridheight = 3;
    gridbag.setConstraints((JLabel) jUnitBoxLabel.elementAt(0), c);
    jUnitBoxPanel.add((JLabel) jUnitBoxLabel.elementAt(0));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jUnitBox.elementAt(0), c);
    jUnitBoxPanel.add((JTextField) jUnitBox.elementAt(0));
    c.gridwidth = 2;
    gridbag.setConstraints((JLabel) jUnitBoxLabel.elementAt(1), c);
    jUnitBoxPanel.add((JLabel) jUnitBoxLabel.elementAt(1));
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints((JTextField) jUnitBox.elementAt(1), c);
    jUnitBoxPanel.add((JTextField) jUnitBox.elementAt(1));


    boxApplyTo = new JComboBox(applyToList);
    boxApplyTo.setSelectedIndex(0);

    c.gridheight = 5;
    c.weighty = 10000;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(origAtomsOnly, c);
    jCrystBoxPanel.add(origAtomsOnly);
    gridbag.setConstraints(jAtomBoxPanel, c);
    jCrystBoxPanel.add(jAtomBoxPanel);
    gridbag.setConstraints(jBondBoxPanel, c);
    jCrystBoxPanel.add(jBondBoxPanel);
    gridbag.setConstraints(jUnitBoxPanel, c);
    jCrystBoxPanel.add(jUnitBoxPanel);
    c.weighty = 1;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(boxApplyTo, c);
    jCrystBoxPanel.add(boxApplyTo);


    return jCrystBoxPanel;
  }    //end makeCrystalBoxPanel


  protected void updateCrystalBoxPanel() {

    double[][] atomBox = crystalBox.getAtomBox();
    double[][] bondBox = crystalBox.getBondBox();
    double[][] unitBox = crystalBox.getUnitBox();
    
    origAtomsOnly.setSelected(crystalBox.getOrigAtomsOnly());

    ((JTextField) (jAtomBox.elementAt(0)))
      .setText(atomBox[0][0] + ", " + atomBox[0][1] + ", " + atomBox[0][2]);
    ((JTextField) (jAtomBox.elementAt(1)))
      .setText(atomBox[1][0] + ", " + atomBox[1][1] + ", " + atomBox[1][2]);
    ((JTextField) (jBondBox.elementAt(0)))
      .setText(bondBox[0][0] + ", " + bondBox[0][1] + ", " + bondBox[0][2]);
    ((JTextField) (jBondBox.elementAt(1)))
      .setText(bondBox[1][0] + ", " + bondBox[1][1] + ", " + bondBox[1][2]);
    ((JTextField) (jUnitBox.elementAt(0)))
      .setText(unitBox[0][0] + ", " + unitBox[0][1] + ", " + unitBox[0][2]);
    ((JTextField) (jUnitBox.elementAt(1)))
      .setText(unitBox[1][0] + ", " + unitBox[1][1] + ", " + unitBox[1][2]);
    
    // the number of atoms in the unit cell

    jNatomInCellInfo.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NCell")
       + " " + unitCellBox.getNumberOfAtoms());
    
    // the number of atoms in the crystal box
    jNatomInBoxInfo.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
       + " " + crystalFile.getFrame(currentFrameIndex).getNumberOfAtoms());
    
    jNatomInClipInfo.setText
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
    basisVectorTypeList = new JComboBox(basisVectorTypeStrings);
    basisVectorTypeList.setSelectedIndex(0);
    basisVectorTypeList.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  updateBasisVectorsPanel();
	}
      });
    
    
    JTable table = new JTable(basisVectorTableModel);
    table.setPreferredScrollableViewportSize(new Dimension(500, 180));

    //Create the scroll pane and add the table to it. 
    JScrollPane scrollPane = new JScrollPane(table);

    jNatomInCellInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NbondBox"), SwingConstants.LEFT);



    String[] basisApplyToList = {
      resources.translate("Apply to all frames"),
      resources.translate("Apply to current frame")
    };
    basisApplyTo = new JComboBox(basisApplyToList);
    basisApplyTo.setSelectedIndex(1);



    c.gridheight = 4;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(basisVectorTypeList, c);
    jBasisVPanel.add(basisVectorTypeList);
    c.weighty = 10000;
    c.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(scrollPane, c);
    jBasisVPanel.add(scrollPane);
    c.fill = GridBagConstraints.NONE;
    c.weighty = 1;
    gridbag.setConstraints(jNatomInCellInfo, c);
    jBasisVPanel.add(jNatomInCellInfo);
    c.gridheight = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.NONE;
    gridbag.setConstraints(basisApplyTo, c);

    //jBasisVPanel.add(basisApplyTo);


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
    
    JTable table2 = new JTable(energyLineTableModel);
    table2.setPreferredScrollableViewportSize(new Dimension(150, 90));
    
    //Create the scroll pane and add the table to it. 
    JScrollPane scrollPane1 = new JScrollPane(table1);
    JScrollPane scrollPane2 = new JScrollPane(table2);

    c.gridheight = 1;
    c.anchor = GridBagConstraints.NORTHWEST;
    //c.weighty = 10000;
    c.fill = GridBagConstraints.BOTH;
    gridbag.setConstraints(scrollPane1, c);
    jEnergyBandPanel.add(scrollPane1);
    //c.fill = GridBagConstraints.NONE;
    //c.weighty = 1;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(scrollPane2, c);
    jEnergyBandPanel.add(scrollPane2);
    


    return jEnergyBandPanel;

  }    //end makeEnergyBandPanel

  protected void updateEnergyBandPanel() {
    //Vector frameProperties = crystalFile.getFramePropertyList();
    Vector frameProperties = currentFrame.getFrameProperties();
    boolean hasEnergyBand=false;
    EnergyBand energyBand=null;

    //Do we have an EnergyBand?
    for (int i=0; i< frameProperties.size();i++) {
      if(((PhysicalProperty)frameProperties.elementAt(i)).getDescriptor()
	 .equals("EnergyBand")) {
	//We have an EnergyBand
	hasEnergyBand=true;
	energyBand =
	  (EnergyBand)frameProperties.elementAt(i);
	tabbedPane.setEnabledAt(3, true);
      } else { //We don't have an EnergyBand
	tabbedPane.setEnabledAt(3, false);
      }
    }
    

    if(hasEnergyBand) {
      Object data[][]=new Object[energyBand.getNumberOfKLines()][4];
      for (int i=0; i< energyBand.getNumberOfKLines();i++) {
	data[i][0] = new String(String.valueOf(i));
	data[i][1] = new Point3d(energyBand.getKLine(i).getOrigin());
	data[i][2] = new Point3d(energyBand.getKLine(i).getEnd());
	data[i][3] = new String
	  (String.valueOf(energyBand.getKLine(i).getNumberOfkPoints()));
      }
      energyLinesTableModel.setData(data);
    }
    
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

    primVTypeList.setSelectedIndex(0);
    
    
    ((JTextField) (jRprim.elementAt(0))).setText("1.0, 0.0, 0.0");
    ((JTextField) (jRprim.elementAt(1))).setText("0.0, 1.0, 0.0");
    ((JTextField) (jRprim.elementAt(2))).setText("0.0, 0.0, 1.0");
    ((JTextField) (jAcell.elementAt(0))).setText("1.0");
    ((JTextField) (jAcell.elementAt(1))).setText("1.0");
    ((JTextField) (jAcell.elementAt(2))).setText("1.0");
    
    ((JTextField) (jEdges.elementAt(0))).setText("1.0");
    ((JTextField) (jEdges.elementAt(1))).setText("1.0");
    ((JTextField) (jEdges.elementAt(2))).setText("1.0");
    ((JTextField) (jAngles.elementAt(0))).setText("90.0");
    ((JTextField) (jAngles.elementAt(1))).setText("90.0");
    ((JTextField) (jAngles.elementAt(2))).setText("90.0");
    
    for (int i = 0; i < 3; i++) {
      ((JTextField) (jRprim.elementAt(i))).setEditable(true);
      ((JTextField) (jAcell.elementAt(i))).setEditable(true);
      ((JTextField) (jEdges.elementAt(i))).setEditable(false);
      ((JTextField) (jAngles.elementAt(i))).setEditable(false);
    }
    

    // crystal box
    
    ((JTextField) (jAtomBox.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jAtomBox.elementAt(1))).setText("1.0, 1.0, 1.0");
    ((JTextField) (jBondBox.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jBondBox.elementAt(1))).setText("1.0, 1.0, 1.0");
    ((JTextField) (jUnitBox.elementAt(0))).setText("0.0, 0.0, 0.0");
    ((JTextField) (jUnitBox.elementAt(1))).setText("1.0, 1.0, 1.0");
    
    // Display other usefull information:
    // the number of atoms in the unit cell
    jNatomInCellInfo.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NCell")
       + " " + chemFile.getFrame(currentFrameIndex).getNumberOfAtoms());
    
    // the number of atoms in the crystal box
    jNatomInBoxInfo.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
       + " " + chemFile.getFrame(currentFrameIndex).getNumberOfAtoms());
    
    jNatomInClipInfo.setText
      (JmolResourceHandler.getInstance().getString("Crystprop.NbondBox")
       + " " + chemFile.getFrame(currentFrameIndex).getNumberOfAtoms());
    
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
      System.out.println("Table Updated");

    }

  }    //end class BasisVTableModel

  class EnergyLinesTableModel extends AbstractTableModel {

    private String[] columnNames = {
      JmolResourceHandler.getInstance().translate("Line Number"),
      JmolResourceHandler.getInstance().translate("Origin"),
      JmolResourceHandler.getInstance().translate("End"),
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
      if (col < 5) {    //no cell editable
        return false;
      } else {
        return true;
      }
    }
    
    public void setValueAt(Object value, int row, int col) {
      data[row][col] = value;
      fireTableCellUpdated(row, col);
      System.out.println("Table Updated");
      
    }

  }    //end class EnergyLinesTableModel

  class EnergyLineTableModel extends AbstractTableModel {

    private String[] columnNames = {
      JmolResourceHandler.getInstance().translate("Point Number"),
      "kx", "ky", "kz" };
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
  }    //end class EnergyLineTableModel

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

    rprim = readField3(jRprim);
    acell = readField1(jAcell);
    edges = readField1(jEdges);
    angles = readField1(jAngles);
    atomBox = readField3(jAtomBox);
    bondBox = readField3(jBondBox);
    unitBox = readField3(jUnitBox);
    

    // In case of a classical ChemFile has been loaded,
    // a CrystalFile is created with defaults parameters
    if (hasCrystalInfo == false) {
      this.crystalFile = new CrystalFile(chemFile, rprim, acell);
      this.chemFile = (ChemFile) crystalFile;

      // Say to everybody that we have a new chemfile!
      model.setChemFile(this.chemFile);
      hasCrystalInfo = true;
      primApplyTo.setEnabled(true);
      boxApplyTo.setEnabled(true);
    }

   

    for (int i = 0; i < model.getNumberOfFrames(); i++) {

      //set Primitive Vectors
      unitCellBox = crystalFile.getUnitCellBox(i);
      if (primApplyTo.getSelectedIndex() == 0
          | ((primApplyTo.getSelectedIndex() == 1) 
	     && (i == currentFrameIndex))) {
        if (primVTypeList.getSelectedIndex() == 0) {   
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
      crystalBox.setOrigAtomsOnly(origAtomsOnly.isSelected());
      if (boxApplyTo.getSelectedIndex() == 0
          | ((boxApplyTo.getSelectedIndex() == 1) 
	     && (i == currentFrameIndex))) {
        crystalBox.setAtomBox(atomBox);
        crystalBox.setBondBox(bondBox);
        crystalBox.setUnitBox(unitBox);

      }
      crystalFile.setCrystalBox(crystalBox);

      if (((primApplyTo.getSelectedIndex() == 1) 
	   && (boxApplyTo.getSelectedIndex() == 1)
	   && (i == currentFrameIndex))
          | !((primApplyTo.getSelectedIndex() == 1)
	      && (boxApplyTo.getSelectedIndex() == 1))) {
	
	
        crystalFile.generateCrystalFrame(i);
      }
    }

    //Display the updated ChemFile
    restoreInFile();
    
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
      //updateEnergyBandPanel();


    } else {    // hasNoCrystalInfo
      
      // Put dummy values
      updateBarePanels();

      // Update Basis Vectors. The basis vector are simply 
      // all the atoms in the frame
      updateBasisVectorsPanel();
      
    }  //endif hasCrystalInfo


  }  //end updateDialog()

  private void updateCurrentFrameIndex() {
  
    int primIndex = primApplyTo.getSelectedIndex();
    int boxIndex = boxApplyTo.getSelectedIndex();
    primApplyTo.removeItem(applyToList[1]);
    primApplyTo.removeItem(applyToList[0]);
    boxApplyTo.removeItem(applyToList[1]);
    boxApplyTo.removeItem(applyToList[0]);

    currentFrameIndex = Animate.currentFrame;
    String s = new String("(" + currentFrameIndex + ")");
    applyToList[0] =
        JmolResourceHandler.getInstance().translate("Apply to all frames");
    applyToList[1] =
        JmolResourceHandler.getInstance().translate("Apply to current frame ")
          + s;

    primApplyTo.addItem(applyToList[0]);
    primApplyTo.addItem(applyToList[1]);
    primApplyTo.setSelectedIndex(primIndex);
    boxApplyTo.addItem(applyToList[0]);
    boxApplyTo.addItem(applyToList[1]);
    boxApplyTo.setSelectedIndex(boxIndex);

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


  /**
   * Read a vector of text fields of the form "double, double, double".
   */
  private double[][] readField3(Vector jTextField) {

    StringTokenizer st;
    String sn;
    double matrix[][] = new double[jTextField.size()][3] ;

    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
          .getText(), ",");
      for (int j = 0; j < 3; j++) {
        if (st.hasMoreTokens()) {
          sn = st.nextToken();
          try {
            matrix[i][j] = Double.parseDouble(sn);
          } catch (NumberFormatException e) {
            String message =
              JmolResourceHandler.getInstance().translate(
                "Ooups! The value you entered in the field is not a valid number.");
            System.out.println(message);
            errorDialog(message);

            matrix[i][j] = 1f;
          }

        }
      }
    }
    return matrix;
  }    //end readField3(...)



  /**
   * Read a vector of text fields of the form "double".
   */
  private double[] readField1(Vector jTextField) {

    StringTokenizer st;
    String sn;
    int dim = jTextField.size();
    double vect[] = (double[]) Array.newInstance(double.class, dim);

    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
          .getText(), ",");

      if (st.hasMoreTokens()) {
        sn = st.nextToken();
        try {
          vect[i] = Double.parseDouble(sn);
        } catch (NumberFormatException e) {
          String message =
            JmolResourceHandler.getInstance().translate(
              "Ooups! The value you entered in the field is not a valid number.");
          System.out.println(message);
          errorDialog(message);
          vect[i] = 1f;
        }

      }

    }
    return vect;
  }    //end readField1(...)



  private void restoreInFile() {

    this.chemFile = (ChemFile) crystalFile;
    hasFile = true;

    model.setChemFile(this.chemFile);
    model.setChemFrame(currentFrameIndex);

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
      primApplyTo.setEnabled(true);
      primApplyTo.setSelectedIndex(1);
      boxApplyTo.setEnabled(true);
      boxApplyTo.setSelectedIndex(0);
      basisApplyTo.setEnabled(true);
      basisApplyTo.setSelectedIndex(1);
    } else if (cf instanceof ChemFile) {
      hasCrystalInfo = false;
      this.chemFile = cf;

      // When a ChemFile is loaded, crystal parameters
      // can only be applied to *all* frames the first time
      // the ok or apply button is clicked.
      primApplyTo.setEnabled(false);
      primApplyTo.setSelectedIndex(0);
      boxApplyTo.setEnabled(false);
      boxApplyTo.setSelectedIndex(0);
      basisApplyTo.setEnabled(false);
      basisApplyTo.setSelectedIndex(0);
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

    if (event.getPropertyName().equals(JmolModel.chemFileProperty)) {
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


