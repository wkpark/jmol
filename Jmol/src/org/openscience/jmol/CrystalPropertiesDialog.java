

/*
 * Copyright 2001 The Jmol Development Team
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


/**
 * Cystal -> Properties dialog.
 *
 * @author  Fabian Dortu (Fabian Dortu@wanadoo.be)
 * @version 1.1
 */
public class CrystalPropertiesDialog extends JDialog
    implements ActionListener, PropertyChangeListener {

  // This class is instanciated in Jmol.java as "crystprop". 
  // In Properties/Jmol.Properties, the dialog box must
  // be defined as "crystprop" exactly!


  /**
   * Reference to the data model.
   */
  private JmolModel model;

  private boolean hasFile = false;
  private boolean hasCrystalInfo = false;
  private ChemFile chemFile;
  private CrystalFile crystalFile;
  private int currentFrame;

  //Swing widget
  JComboBox primVTypeList;
  Vector jRprim = new Vector(3);    //base vectors text field
  Vector jRprimLabel = new Vector(3);
  Vector jAcell = new Vector(3);
  Vector jAcellLabel = new Vector(3);
  Vector jEdges = new Vector(3);
  Vector jEdgesLabel = new Vector(3);
  Vector jAngles = new Vector(3);
  Vector jAnglesLabel = new Vector(3);
  JComboBox primApplyTo;
  String[] applyToList = new String[2];


  Vector jAtomBox = new Vector(2);
  Vector jAtomBoxLabel = new Vector(2);
  Vector jBondBox = new Vector(2);
  Vector jBondBoxLabel = new Vector(2);
  JComboBox boxApplyTo;

  JLabel jNatomInBoxInfo;
  JLabel jNatomInCellInfo;
  JLabel jNatomInClipInfo;

  JComboBox baseVTypeList;
  BasisVTableModel basisVTableModel = new BasisVTableModel();
  JComboBox baseApplyTo;

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
    this.model = model;
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }

    currentFrame = 0;
    applyToList[0] = "Apply to all frames";
    applyToList[1] = "Apply to current frame (0)";


    JPanel container = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    container.setLayout(gridbag);


    //****************
    //* Tabbed Panel
    //***************

    JTabbedPane tabbedPane = new JTabbedPane();

    Component panel1 = makePrimVectorsPanel();
    tabbedPane
        .addTab(JmolResourceHandler.getInstance()
          .getString("Crystprop.primVLabel"), JmolResourceHandler
            .getInstance()
              .getIcon("Crystprop.primVImage"), panel1, JmolResourceHandler
                .getInstance().getString("Crystprop.primVToolTip"));
    tabbedPane.setSelectedIndex(0);

    Component panel2 = makeCrystalBoxPanel();
    tabbedPane.addTab(
        JmolResourceHandler.getInstance().getString(
          "Crystprop.crystalboxLabel"), JmolResourceHandler.getInstance()
            .getIcon(
              "Crystprop.crystalboxImage"), panel2, JmolResourceHandler
                .getInstance().getString("Crystprop.crystalboxToolTip"));

    Component panel3 = makeBasisVPanel();
    tabbedPane
        .addTab(JmolResourceHandler.getInstance()
          .getString("Crystprop.basisVLabel"), JmolResourceHandler
            .getInstance()
              .getIcon("Crystprop.basisVImage"), panel3, JmolResourceHandler
                .getInstance().getString("Crystprop.basisVToolTip"));



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
    JButton jApplyButton = new JButton("Apply");
    JButton jOkButton = new JButton("OK");
    JButton jCancelButton = new JButton("Cancel");
    JButton jReadButton = new JButton("Read current frame");

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
        Hide();
      }
    });


    jCancelButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Hide();
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
  protected void updateBasisV(int index, boolean hasCrystalInfo) {

    if (hasCrystalInfo) {
      UnitCellBox unitCellBox = crystalFile.getUnitCellBox(currentFrame);
      int natom = unitCellBox.getNumberOfAtoms();
      int[] dims = {
        natom, 5
      };
      Object[][] basisVData = (Object[][]) Array.newInstance(Object.class,
                                dims);
      Atom atom;


      if (index == 0) {    //Cartesian coord.
        float[][] cartPos = unitCellBox.getCartesianPos();
        for (int i = 0; i < natom; i++) {
          basisVData[i][0] = new String(String.valueOf(i));
          basisVData[i][1] = new String(unitCellBox.getAtomType(i).getName());
          basisVData[i][2] = new Float(cartPos[i][0]);
          basisVData[i][3] = new Float(cartPos[i][1]);
          basisVData[i][4] = new Float(cartPos[i][2]);
        }
        basisVTableModel.setData(basisVData);
        basisVTableModel.fireTableDataChanged();
      } else {             //Reduced coord.
        float[][] redPos = unitCellBox.getReducedPos();
        for (int i = 0; i < natom; i++) {
          basisVData[i][0] = new String(String.valueOf(i));
          basisVData[i][1] = new String(unitCellBox.getAtomType(i).getName());
          basisVData[i][2] = new Float(redPos[i][0]);
          basisVData[i][3] = new Float(redPos[i][1]);
          basisVData[i][4] = new Float(redPos[i][2]);
        }
        basisVTableModel.setData(basisVData);
        basisVTableModel.fireTableDataChanged();
      }
    } else {               //is not a crystal
      if (index == 1) {
        baseVTypeList.setSelectedIndex(0);
        popup("You must define the primitive vectors to use lattice coordinates");
        System.out.println(
            "You must define the primitive vectors to use lattice coordinates");
      }

      int natom = chemFile.getFrame(currentFrame).getNumberOfAtoms();
      int[] dims = {
        natom, 5
      };
      Object[][] basisVData = (Object[][]) Array.newInstance(Object.class,
                                dims);
      Atom atom;
      for (int i = 0; i < natom; i++) {
        atom = chemFile.getFrame(currentFrame).getAtomAt(i);
        basisVData[i][0] = new String(String.valueOf(i));
        basisVData[i][1] = new String(atom.getType().getName());
        basisVData[i][2] = new Float(atom.getPosition().x);
        basisVData[i][3] = new Float(atom.getPosition().y);
        basisVData[i][4] = new Float(atom.getPosition().z);
      }
      basisVTableModel.setData(basisVData);
      basisVTableModel.fireTableDataChanged();

    }
  }                        //end updateBasisV



  /**
   * This method set which of the cartesian or crystallographic
   * primitive vectors is editable.
   * index==0 --> Cartesian
   * index==1 --> Crystallographic
   *
   * @param index an <code>int</code> value
   */
  protected void setPrimVType(int index) {

    //Set what is editable depending on the combo box value
    primVTypeList.setSelectedIndex(index);
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
  }    //end setPrimVType

  /**
   * Describe <code>makePrimVectorsPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makePrimVectorsPanel() {

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
      "Cartesian", "Crystallographic"
    };
    primVTypeList = new JComboBox(primVTypeStrings);
    primVTypeList.setSelectedIndex(0);
    primVTypeList.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();
        int primVTypeIndex = cb.getSelectedIndex();
        setPrimVType(primVTypeIndex);
      }
    });

    JLabel jPrimVTypeLabel = new JLabel("Representation: ",
                               SwingConstants.LEFT);


    //The jCart subPanel (level 1)
    JPanel jCart = new JPanel();
    jCart.setLayout(gridbag);
    jCart.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Crystprop.cartesianLabel")));


    //The 3 base vectors text box in Cartesian representation
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


  /**
   * Describe <code>makeCrystalBoxPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeCrystalBoxPanel() {

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();

    //Panel that contains jAtomBox and jBondBox
    JPanel jCrystBoxPanel = new JPanel();
    jCrystBoxPanel.setLayout(gridbag);

    JPanel jAtomBoxPanel = new JPanel();
    jAtomBoxPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Crystprop.atomboxLabel")));
    jAtomBoxPanel.setLayout(gridbag);

    JPanel jBondBoxPanel = new JPanel();
    jBondBoxPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Crystprop.bondboxLabel")));
    jBondBoxPanel.setLayout(gridbag);


    jAtomBox.addElement(new JTextField(15));
    jAtomBoxLabel.addElement(new JLabel("Minimum atom box coordinate: ",
        SwingConstants.LEFT));
    jAtomBox.addElement(new JTextField(15));
    jAtomBoxLabel.addElement(new JLabel("Maximum atom box coordinate: ",
        SwingConstants.LEFT));
    jBondBox.addElement(new JTextField(15));
    jBondBoxLabel.addElement(new JLabel("Minimum bond box coordinate: ",
        SwingConstants.LEFT));
    jBondBox.addElement(new JTextField(15));
    jBondBoxLabel.addElement(new JLabel("Maximum bond box coordinate: ",
        SwingConstants.LEFT));


    jNatomInBoxInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NatomBox"), SwingConstants.LEFT);

    jNatomInClipInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NCell"), SwingConstants.LEFT);


    c.weightx = 1.0;
    c.weighty = 1.0;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.NONE;
    c.gridwidth = 2;
    c.gridheight = 3;
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


    boxApplyTo = new JComboBox(applyToList);
    boxApplyTo.setSelectedIndex(0);



    c.gridheight = 3;
    c.weighty = 10000;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.BOTH;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(jAtomBoxPanel, c);
    jCrystBoxPanel.add(jAtomBoxPanel);
    gridbag.setConstraints(jBondBoxPanel, c);
    jCrystBoxPanel.add(jBondBoxPanel);
    c.weighty = 1;
    c.fill = GridBagConstraints.NONE;
    c.gridheight = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(boxApplyTo, c);
    jCrystBoxPanel.add(boxApplyTo);


    return jCrystBoxPanel;
  }    //end makeCrystalBoxPanel


  /**
   * Describe <code>makeBasisVPanel</code> method here.
   *
   * @return a <code>Component</code> value
   */
  protected Component makeBasisVPanel() {

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 1.0;
    c.weighty = 1.0;

    JPanel jBasisVPanel = new JPanel();
    jBasisVPanel.setLayout(gridbag);


    String[] baseVTypeStrings = {
      "Cartesian", "Lattice"
    };
    baseVTypeList = new JComboBox(baseVTypeStrings);
    baseVTypeList.setSelectedIndex(0);
    baseVTypeList.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        JComboBox cb = (JComboBox) e.getSource();
        int baseVTypeIndex = cb.getSelectedIndex();
        updateBasisV(baseVTypeIndex, hasCrystalInfo);
      }
    });


    JTable table = new JTable(basisVTableModel);
    table.setPreferredScrollableViewportSize(new Dimension(500, 180));

    //Create the scroll pane and add the table to it. 
    JScrollPane scrollPane = new JScrollPane(table);

    jNatomInCellInfo =
        new JLabel(JmolResourceHandler.getInstance()
          .getString("Crystprop.NbondBox"), SwingConstants.LEFT);



    String[] baseApplyToList = {
      "Apply to all frames", "Apply to current frame"
    };
    baseApplyTo = new JComboBox(baseApplyToList);
    baseApplyTo.setSelectedIndex(1);



    c.gridheight = 4;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(baseVTypeList, c);
    jBasisVPanel.add(baseVTypeList);
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
    gridbag.setConstraints(baseApplyTo, c);

    //jBasisVPanel.add(baseApplyTo);


    return jBasisVPanel;

  }    //end makeBasisVPanel

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



  class BasisVTableModel extends AbstractTableModel {


    private String[] columnNames = {
      "Atom Number", "Atom Type", "x", "y", "z"
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

  }    //end class myTableModel



  private void commitChange() {

    boolean isPrimVectorsCart;
    float[][] rprim;
    float[] acell;
    float[] edges;
    float[] angles;
    float[][] atomBox;
    float[][] bondBox;
    UnitCellBox unitCellBox;
    CrystalBox crystalBox;


    // In case of a classical ChemFile has been loaded,
    // a CrystalFile is created with defaults parameters
    if (hasCrystalInfo == false) {
      this.crystalFile = new CrystalFile(chemFile);
      this.chemFile = (ChemFile) crystalFile;

      // Say to everybody that we have a new chemfile!
      model.setChemFile(this.chemFile);
      hasCrystalInfo = true;
      primApplyTo.setEnabled(true);
      boxApplyTo.setEnabled(true);
    }

    // Read text from the various text fields
    // and set it in the CrystalFile object.

    rprim = readField3(jRprim);
    acell = readField1(jAcell);
    edges = readField1(jEdges);
    angles = readField1(jAngles);
    atomBox = readField3(jAtomBox);
    bondBox = readField3(jBondBox);

    for (int i = 0; i < model.getNumberOfFrames(); i++) {

      //set Primitive Vectors
      unitCellBox = crystalFile.getUnitCellBox(i);
      if (primApplyTo.getSelectedIndex() == 0
          | ((primApplyTo.getSelectedIndex() == 1) && (i == currentFrame))) {
        if (primVTypeList.getSelectedIndex() == 0) {    //Cartesian repres.
          unitCellBox.setPrimVectorsCartesian(rprim, acell);
        } else {                                        //Crystallographic representation
          unitCellBox.setPrimVectorsCrystallo(edges, angles);
        }
      }
      crystalFile.setUnitCellBox(unitCellBox);

      //set Crystal Box
      crystalBox = crystalFile.getCrystalBox(i);
      if (boxApplyTo.getSelectedIndex() == 0
          | ((boxApplyTo.getSelectedIndex() == 1) && (i == currentFrame))) {
        crystalBox.setAtomBox(atomBox);
        crystalBox.setBondBox(bondBox);
      }
      crystalFile.setCrystalBox(crystalBox);

      if (((primApplyTo.getSelectedIndex() == 1) && (boxApplyTo.getSelectedIndex() == 1) && (i == currentFrame))
          | !((primApplyTo.getSelectedIndex() == 1)
            && (boxApplyTo.getSelectedIndex() == 1))) {


        crystalFile.generateCrystalFrame(i);
      }
    }

    //Display the updated ChemFile
    restoreInFile();

  }    //end commitChange()

  private void updateDialog() {

    boolean isPrimVectorsCart;
    float[][] rprim;
    float[] acell;
    float[] edges;
    float[] angles;
    float[][] atomBox;
    float[][] bondBox;
    UnitCellBox unitCellBox;
    CrystalBox crystalBox;



    // Reput(or put it for the first time)
    // the text in the box so that the user
    // can see if his input has been interpreted correctly.

    if (hasCrystalInfo) {
      unitCellBox = crystalFile.getUnitCellBox(currentFrame);
      isPrimVectorsCart = unitCellBox.isPrimVectorsCartesian();
      rprim = unitCellBox.getRprim();
      acell = unitCellBox.getAcell();

      if (isPrimVectorsCart) {
        setPrimVType(0);
      } else {
        setPrimVType(1);
      }

      ((JTextField) (jRprim.elementAt(0))).setText(rprim[0][0] + ", "
          + rprim[0][1] + ", " + rprim[0][2]);
      ((JTextField) (jRprim.elementAt(1))).setText(rprim[1][0] + ", "
          + rprim[1][1] + ", " + rprim[1][2]);
      ((JTextField) (jRprim.elementAt(2))).setText(rprim[2][0] + ", "
          + rprim[2][1] + ", " + rprim[2][2]);
      ((JTextField) (jAcell.elementAt(0))).setText(acell[0] + "");
      ((JTextField) (jAcell.elementAt(1))).setText(acell[1] + "");
      ((JTextField) (jAcell.elementAt(2))).setText(acell[2] + "");

      edges = unitCellBox.getEdges();
      angles = unitCellBox.getAngles();
      ((JTextField) (jEdges.elementAt(0))).setText(edges[0] + "");
      ((JTextField) (jEdges.elementAt(1))).setText(edges[1] + "");
      ((JTextField) (jEdges.elementAt(2))).setText(edges[2] + "");
      ((JTextField) (jAngles.elementAt(0))).setText(angles[0] + "");
      ((JTextField) (jAngles.elementAt(1))).setText(angles[1] + "");
      ((JTextField) (jAngles.elementAt(2))).setText(angles[2] + "");

      // crystal and clipping box
      crystalBox = crystalFile.getCrystalBox(currentFrame);
      atomBox = crystalBox.getAtomBox();
      bondBox = crystalBox.getBondBox();


      ((JTextField) (jAtomBox.elementAt(0))).setText(atomBox[0][0] + ", "
          + atomBox[0][1] + ", " + atomBox[0][2]);
      ((JTextField) (jAtomBox.elementAt(1))).setText(atomBox[1][0] + ", "
          + atomBox[1][1] + ", " + atomBox[1][2]);
      ((JTextField) (jBondBox.elementAt(0))).setText(bondBox[0][0] + ", "
          + bondBox[0][1] + ", " + bondBox[0][2]);
      ((JTextField) (jBondBox.elementAt(1))).setText(bondBox[1][0] + ", "
          + bondBox[1][1] + ", " + bondBox[1][2]);



      // Display other usefull information:
      // the number of atoms in the unit cell

      jNatomInCellInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NCell")
            + " " + unitCellBox.getNumberOfAtoms());

      // the number of atoms in the crystal box
      jNatomInBoxInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
            + " " + crystalFile.getFrame(currentFrame).getNumberOfAtoms());

      jNatomInClipInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NbondBox")
            + " " + crystalFile.getNumberBondedAtoms(currentFrame));


      updateBasisV(baseVTypeList.getSelectedIndex(), true);


    } else {    // hasNoCrystalInfo
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


      // Display other usefull information:
      // the number of atoms in the unit cell
      jNatomInCellInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NCell")
            + " " + chemFile.getFrame(currentFrame).getNumberOfAtoms());

      // the number of atoms in the crystal box
      jNatomInBoxInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NatomBox")
            + " " + chemFile.getFrame(currentFrame).getNumberOfAtoms());

      jNatomInClipInfo.setText(
          JmolResourceHandler.getInstance().getString("Crystprop.NbondBox")
            + " " + chemFile.getFrame(currentFrame).getNumberOfAtoms());

      // Update Basis Vectors.
      updateBasisV(baseVTypeList.getSelectedIndex(), false);

    }           //endif hasCrystalInfo


  }             //end updateDialog()

  private void updateCurrentFrameIndex() {

    int primIndex = primApplyTo.getSelectedIndex();
    int boxIndex = boxApplyTo.getSelectedIndex();
    primApplyTo.removeItem(applyToList[1]);
    primApplyTo.removeItem(applyToList[0]);
    boxApplyTo.removeItem(applyToList[1]);
    boxApplyTo.removeItem(applyToList[0]);

    currentFrame = Animate.currentFrame;
    String s = new String("(" + currentFrame + ")");
    applyToList[0] = "Apply to all frames";
    applyToList[1] = "Apply to current frame " + s;

    primApplyTo.addItem(applyToList[0]);
    primApplyTo.addItem(applyToList[1]);
    primApplyTo.setSelectedIndex(primIndex);
    boxApplyTo.addItem(applyToList[0]);
    boxApplyTo.addItem(applyToList[1]);
    boxApplyTo.setSelectedIndex(boxIndex);
  }

  /**
   * Describe <code>popup</code> method here.
   *
   * @param s a <code>String</code> value
   */
  protected void popup(String s) {

    final JFrame myframe = new JFrame("");
    JPanel panel = new JPanel();
    JLabel mylabel = new JLabel(s);
    JButton ok = new JButton("OK");
    myframe.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    ok.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        myframe.dispose();
      }
    });

    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(gridbag);
    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(mylabel, c);
    panel.add(mylabel);
    panel.add(ok);
    myframe.getContentPane().add(panel);

    myframe.pack();
    myframe.setVisible(true);
  }    //end popup


  /**
   * Read a vector of text fields of the form "float, float, float".
   */
  private float[][] readField3(Vector jTextField) {

    StringTokenizer st;
    String sn;
    int[] dims = new int[2];
    dims[0] = jTextField.size();
    dims[1] = 3;
    float matrix[][] = (float[][]) Array.newInstance(float.class, dims);

    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
          .getText(), ",");
      for (int j = 0; j < 3; j++) {
        if (st.hasMoreTokens()) {
          sn = st.nextToken();
          try {
            matrix[i][j] = Float.parseFloat(sn);
          } catch (NumberFormatException e) {
            System.out.println(
                "Ooups! The value you entered in the field is not a valid number.");
            popup("Ooups! The value you entered in the field is not a valid number.");

            matrix[i][j] = 1f;
          }

        }
      }
    }
    return matrix;
  }    //end readField3(...)



  /**
   * Read a vector of text fields of the form "float".
   */
  private float[] readField1(Vector jTextField) {

    StringTokenizer st;
    String sn;
    int dim = jTextField.size();
    float vect[] = (float[]) Array.newInstance(float.class, dim);

    for (int i = 0; i < jTextField.size(); i++) {
      st = new StringTokenizer(((JTextField) jTextField.elementAt(i))
          .getText(), ",");

      if (st.hasMoreTokens()) {
        sn = st.nextToken();
        try {
          vect[i] = Float.parseFloat(sn);
        } catch (NumberFormatException e) {
          System.out.println(
              "Ooups! The value you entered in the field is not a valid number");
          popup("Ooups! The value you entered in the field is not a valid number");
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
    model.setChemFrame(currentFrame);

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
      baseApplyTo.setEnabled(true);
      baseApplyTo.setSelectedIndex(1);
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
      baseApplyTo.setEnabled(false);
      baseApplyTo.setSelectedIndex(0);
    }


    // Set if the crystprop Action is enabled 
    // or not (appear black or gray in the menu)
    if (this.isShowing()) {                 //the Crystal dialog is open
      crystpropAction.setEnabled(false);    //appear gray (disabled)
    } else {
      crystpropAction.setEnabled(true);     //appear black (enabled)
    }
    hasFile = true;

    //Need to refresh the dialog box in case the dialog was open
    //when a new file was loaded.
    updateCurrentFrameIndex();
    updateDialog();


  }


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
    crystpropAction.setEnabled(true);
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
   * Describe <code>Hide</code> method here.
   *
   */
  public void Hide() {
    crystpropAction.setEnabled(true);
    this.setVisible(false);
  }



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




