/* $RCSfile$
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
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Dialog;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Dictionary;
import java.util.EventObject;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JRootPane;
import javax.swing.JComboBox;
import javax.swing.Box;
import javax.swing.JTabbedPane;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

public class PreferencesDialog extends JDialog implements ActionListener {

  private boolean autoBond;
  private boolean showHydrogens;
  private boolean showVectors;
  private boolean showMeasurements;
  private boolean wireframeRotation;
  private boolean perspectiveDepth;
  private boolean showAxes;
  private boolean showBoundingBox;
  private boolean orientationRasMolChime;
  private boolean isLabelAtomColor;
  private boolean isBondAtomColor;
  private Color colorBackground;
  private Color colorSelection;
  private Color colorText;
  private Color colorBond;
  private Color colorVector;
  private Color colorMeasurement;
  private byte modeAtomColorProfile;
  private byte styleLabel;
  private float minBondDistance;
  private float bondTolerance;
  private short marBond;
  private int percentVdwAtom;
  private double VibrateAmplitudeScale;
  private double VibrateVectorScale;
  private int VibrationFrames;
  private JButton bButton, pButton, tButton, eButton, vButton;
  private JButton measurementColorButton;
  private JRadioButton pYes, pNo, abYes, abNo;
  private JComboBox aLabel, aProps, cRender;
  private JSlider vdwPercentSlider;
  private JSlider bdSlider, bwSlider, btSlider;
  private JSlider vasSlider;
  private JSlider vvsSlider;
  private JSlider vfSlider;
  private JCheckBox cH, cV, cM;
  private JCheckBox cbWireframeRotation, cbPerspectiveDepth;
  private JCheckBox cbShowAxes, cbShowBoundingBox;
  private JCheckBox cbOrientationRasMolChime;
  private JCheckBox cbIsLabelAtomColor, cbIsBondAtomColor;
  private Properties originalSystemProperties;
  private Properties jmolDefaultProperties;
  private Properties currentProperties;

  // The actions:

  private PrefsAction prefsAction = new PrefsAction();
  private Hashtable commands;

  final static String[] jmolDefaults  = {
    "showHydrogens",                  "true",
    "showVectors",                    "true",
    "showMeasurements",               "true",
    "wireframeRotation",              "false",
    "perspectiveDepth",               "true",
    "showAxes",                       "false",
    "showBoundingBox",                "false",
    "orientationRasMolChime",         "true",
    "styleLabel",                     "0",
    "percentVdwAtom",                 "20",
    "autoBond",                       "true",
    "marBond",                        "100",
    "minBondDistance",                "0.40",
    "bondTolerance",                  "0.45",
    "colorSelection",                 "16762880",
    "colorBackground",                "16777215",
    "isLabelAtomColor",               "false",
    "colorText",                      "0",
    "isBondAtomColor",                "true",
    "colorBond",                      "0",
    "colorVector",                    "0",
    "colorMeasurement",               "0",
    "VibrateAmplitudeScale",          "0.7",
    "VibrateVectorScale",             "1.0",
    "VibrationFrames",                "20",
  };

  final static String[] rasmolOverrides = {
    "colorBackground",                "0",
    "isLabelAtomColor",               "true",
    "colorVector",                    "16777215",
    "colorMeasurement",               "16777215",
    "percentVdwAtom",                 "0",
    "marBond",                        "1",
  };

  private JmolViewer viewer;
  private GuiMap guimap;

  public PreferencesDialog(JFrame f, GuiMap guimap, JmolViewer viewer) {

    super(f, false);
    this.guimap = guimap;
    this.viewer = viewer;

    initializeProperties();

    JmolResourceHandler jrh = JmolResourceHandler.getInstance();
    this.setTitle(jrh.translate("Preferences"));

    initVariables();
    commands = new Hashtable();
    Action[] actions = getActions();
    for (int i = 0; i < actions.length; i++) {
      Action a = actions[i];
      commands.put(a.getValue(Action.NAME), a);
    }
    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

    JTabbedPane tabs = new JTabbedPane();
    JPanel disp = buildDispPanel();
    JPanel atoms = buildAtomsPanel();
    JPanel bonds = buildBondPanel();
    JPanel colors = buildColorsPanel();
    JPanel vibrate = buildVibratePanel();
    tabs.addTab(jrh.getString("Prefs.displayLabel"), null, disp);
    tabs.addTab(jrh.getString("Prefs.atomsLabel"), null, atoms);
    tabs.addTab(jrh.getString("Prefs.bondsLabel"), null, bonds);
    tabs.addTab(jrh.getString("Prefs.colorsLabel"), null, colors);
    tabs.addTab(jrh.getString("Prefs.vibrateLabel"), null, vibrate);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    jmolDefaultsButton = new JButton(jrh.getString("Prefs.jmolDefaultsLabel"));
    jmolDefaultsButton.addActionListener(this);
    buttonPanel.add(jmolDefaultsButton);

    rasmolDefaultsButton =
      new JButton(jrh.getString("Prefs.rasmolDefaultsLabel"));
    rasmolDefaultsButton.addActionListener(this);
    buttonPanel.add(rasmolDefaultsButton);

    cancelButton = new JButton(jrh.getString("Prefs.cancelButton"));
    cancelButton.addActionListener(this);
    buttonPanel.add(cancelButton);

    applyButton = new JButton(jrh.getString("Prefs.applyButton"));
    applyButton.addActionListener(this);
    buttonPanel.add(applyButton);

    okButton = new JButton(jrh.getString("Prefs.okLabel"));
    okButton.addActionListener(this);
    buttonPanel.add(okButton);
    getRootPane().setDefaultButton(okButton);

    container.add(tabs, BorderLayout.CENTER);
    container.add(buttonPanel, BorderLayout.SOUTH);
    getContentPane().add(container);

    updateComponents();

    pack();
    centerDialog();
  }

  public JPanel buildDispPanel() {

    JPanel disp = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    disp.setLayout(gridbag);
    GridBagConstraints constraints;

    JPanel showPanel = new JPanel();
    showPanel.setLayout(new GridLayout(1, 3));
    showPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.showLabel")));
    cH = guimap.newJCheckBox("Prefs.showHydrogens",
                             viewer.getShowHydrogens());
    cH.addItemListener(checkBoxListener);
    cV = guimap.newJCheckBox("Prefs.showVectors", viewer.getShowVectors());
    cV.addItemListener(checkBoxListener);
    cM = guimap.newJCheckBox("Prefs.showMeasurements",
                             viewer.getShowMeasurements());
    cM.addItemListener(checkBoxListener);
    showPanel.add(cH);
    showPanel.add(cV);
    showPanel.add(cM);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(showPanel, constraints);

    JPanel fooPanel = new JPanel();
    fooPanel.setBorder(new TitledBorder(""));
    fooPanel.setLayout(new GridLayout(2, 1));

    cbWireframeRotation =
      guimap.newJCheckBox("Prefs.wireframeRotation",
                          viewer.getWireframeRotation());
    cbWireframeRotation.addItemListener(checkBoxListener);
    fooPanel.add(cbWireframeRotation);

    cbPerspectiveDepth =
      guimap.newJCheckBox("Prefs.perspectiveDepth",
                          viewer.getPerspectiveDepth());
    cbPerspectiveDepth.addItemListener(checkBoxListener);
    fooPanel.add(cbPerspectiveDepth);

    cbShowAxes =
      guimap.newJCheckBox("Prefs.showAxes", viewer.getShowAxes());
    cbShowAxes.addItemListener(checkBoxListener);
    fooPanel.add(cbShowAxes);

    cbShowBoundingBox =
      guimap.newJCheckBox("Prefs.showBoundingBox",
                          viewer.getShapeShow(JmolConstants.SHAPE_BBOX));
    cbShowBoundingBox.addItemListener(checkBoxListener);
    fooPanel.add(cbShowBoundingBox);

    cbOrientationRasMolChime =
      guimap.newJCheckBox("Prefs.orientationRasMolChime",
                          viewer.getOrientationRasMolChime());

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(fooPanel, constraints);

    JPanel axesPanel = new JPanel();
    axesPanel.setBorder(new TitledBorder(""));
    axesPanel.setLayout(new GridLayout(1, 1));

    cbOrientationRasMolChime.addItemListener(checkBoxListener);
    axesPanel.add(cbOrientationRasMolChime);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(axesPanel, constraints);



    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.gridheight = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    disp.add(filler, constraints);

    return disp;
  }

  public JPanel buildAtomsPanel() {

    JPanel atomPanel = new JPanel(new GridBagLayout());
    GridBagConstraints constraints;

    JPanel sfPanel = new JPanel();
    sfPanel.setLayout(new BorderLayout());
    sfPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.atomSizeLabel")));
    JLabel sfLabel = new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.atomSizeExpl"), JLabel.CENTER);
    sfPanel.add(sfLabel, BorderLayout.NORTH);
    vdwPercentSlider =
      new JSlider(JSlider.HORIZONTAL, 0, 100, viewer.getPercentVdwAtom());
    vdwPercentSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    vdwPercentSlider.setPaintTicks(true);
    vdwPercentSlider.setMajorTickSpacing(20);
    vdwPercentSlider.setMinorTickSpacing(10);
    vdwPercentSlider.setPaintLabels(true);
    vdwPercentSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        percentVdwAtom = source.getValue();
        viewer.setPercentVdwAtom(percentVdwAtom);
        currentProperties.put("percentVdwAtom", "" + percentVdwAtom);
      }
    });
    sfPanel.add(vdwPercentSlider, BorderLayout.CENTER);
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    atomPanel.add(sfPanel, constraints);


    JLabel atomColoringLabel = new JLabel(JmolResourceHandler.getInstance()
          .getString("Prefs.atomColoringLabel"));
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.EAST;
    atomPanel.add(atomColoringLabel, constraints);
    cRender = new JComboBox();
    cRender.addItem(JmolResourceHandler.getInstance().getString("Prefs.cATChoice"));
    cRender.addItem(JmolResourceHandler.getInstance().getString("Prefs.cCChoice"));
    cRender.setSelectedIndex(viewer.getModeAtomColorProfile());
    cRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        modeAtomColorProfile = (byte)source.getSelectedIndex();
        viewer.setModeAtomColorProfile(modeAtomColorProfile);
        currentProperties.put("modeAtomColorProfile", ""+modeAtomColorProfile);
      }
    });
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    atomPanel.add(cRender, constraints);

    JLabel atomLabelsLabel = new JLabel(JmolResourceHandler.getInstance()
          .getString("Prefs.atomLabelsLabel"));
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.EAST;
    atomPanel.add(atomLabelsLabel, constraints);
    aLabel = new JComboBox();
    aLabel.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aPLChoice"));
    aLabel.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aSLChoice"));
    aLabel.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aTLChoice"));
    aLabel.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aNLChoice"));
    aLabel.setSelectedIndex(viewer.getStyleLabel());
    aLabel.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        styleLabel = (byte)source.getSelectedIndex();
        viewer.setStyleLabel(styleLabel);
        currentProperties.put("styleLabel", "" + styleLabel);
      }
    });
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    atomPanel.add(aLabel, constraints);

    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.gridheight = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    constraints.weighty = 1.0;
    atomPanel.add(filler, constraints);

    return atomPanel;
  }

  public JPanel buildBondPanel() {

    JPanel bondPanel = new JPanel();
    GridBagLayout gridbag = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    bondPanel.setLayout(gridbag);
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0;
    c.weighty = 1.0;

    JPanel autobondPanel = new JPanel();
    autobondPanel.setLayout(new BoxLayout(autobondPanel, BoxLayout.Y_AXIS));
    autobondPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.autoBondLabel")));
    ButtonGroup abGroup = new ButtonGroup();
    abYes =
        new JRadioButton(JmolResourceHandler.getInstance()
          .getString("Prefs.abYesLabel"));
    abNo = new JRadioButton(JmolResourceHandler.getInstance()
        .getString("Prefs.abNoLabel"));
    abGroup.add(abYes);
    abGroup.add(abNo);
    autobondPanel.add(abYes);
    autobondPanel.add(abNo);
    autobondPanel.add(Box.createVerticalGlue());
    abYes.setSelected(viewer.getAutoBond());
    abYes.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        viewer.setAutoBond(true);
      }
    });

    abNo.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        viewer.setAutoBond(false);
      }
    });

    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(autobondPanel, c);
    bondPanel.add(autobondPanel);

    JPanel bwPanel = new JPanel();
    bwPanel.setLayout(new BorderLayout());
    bwPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.bondRadiusLabel")));
    JLabel bwLabel =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.bondRadiusExpl"), JLabel.CENTER);
    bwPanel.add(bwLabel, BorderLayout.NORTH);

    bwSlider = new JSlider(0, 250,viewer.getMarBond());
    bwSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bwSlider.setPaintTicks(true);
    bwSlider.setMajorTickSpacing(50);
    bwSlider.setMinorTickSpacing(25);
    bwSlider.setPaintLabels(true);
    for (int i = 0; i <= 250; i += 50) {
      String label = "" + (1000 + i);
      label = "0." + label.substring(1);
      bwSlider.getLabelTable().put(new Integer(i),
                                   new JLabel(label, JLabel.CENTER));
      bwSlider.setLabelTable(bwSlider.getLabelTable());
    }
    bwSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        marBond = (short)source.getValue();
        viewer.setMarBondDefault(marBond);
        currentProperties.put("marBond", "" + marBond);
      }
    });

    bwPanel.add(bwSlider, BorderLayout.SOUTH);

    c.weightx = 0.0;
    gridbag.setConstraints(bwPanel, c);
    bondPanel.add(bwPanel);

    // Bond Tolerance Slider
    JPanel btPanel = new JPanel();
    btPanel.setLayout(new BorderLayout());
    btPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.bondToleranceLabel")));
    JLabel btLabel =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.bondToleranceExpl"), JLabel.CENTER);
    btPanel.add(btLabel, BorderLayout.NORTH);

    btSlider = new JSlider(JSlider.HORIZONTAL, 0, 100,
        (int) (100 * viewer.getBondTolerance()));
    btSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    btSlider.setPaintTicks(true);
    btSlider.setMajorTickSpacing(20);
    btSlider.setMinorTickSpacing(10);
    btSlider.setPaintLabels(true);
    btSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());
    btSlider.getLabelTable().put(new Integer(20),
        new JLabel("0.2", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());
    btSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());
    btSlider.getLabelTable().put(new Integer(60),
        new JLabel("0.6", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());
    btSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());
    btSlider.getLabelTable().put(new Integer(100),
        new JLabel("1.0", JLabel.CENTER));
    btSlider.setLabelTable(btSlider.getLabelTable());

    btSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        bondTolerance = source.getValue() / 100f;
        viewer.setBondTolerance(bondTolerance);
        currentProperties.put("bondTolerance", "" + bondTolerance);
        viewer.rebond();
      }
    });
    btPanel.add(btSlider);


    c.weightx = 0.0;
    gridbag.setConstraints(btPanel, c);
    bondPanel.add(btPanel);

    // minimum bond distance slider
    JPanel bdPanel = new JPanel();
    bdPanel.setLayout(new BorderLayout());
    bdPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.minBondDistanceLabel")));
    JLabel bdLabel =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.minBondDistanceExpl"), JLabel.CENTER);
    bdPanel.add(bdLabel, BorderLayout.NORTH);

    bdSlider = new JSlider(JSlider.HORIZONTAL, 0, 100,
        (int) (100 * viewer.getMinBondDistance()));
    bdSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bdSlider.setPaintTicks(true);
    bdSlider.setMajorTickSpacing(20);
    bdSlider.setMinorTickSpacing(10);
    bdSlider.setPaintLabels(true);
    bdSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());
    bdSlider.getLabelTable().put(new Integer(20),
        new JLabel("0.2", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());
    bdSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());
    bdSlider.getLabelTable().put(new Integer(60),
        new JLabel("0.6", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());
    bdSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());
    bdSlider.getLabelTable().put(new Integer(100),
        new JLabel("1.0", JLabel.CENTER));
    bdSlider.setLabelTable(bdSlider.getLabelTable());

    bdSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        minBondDistance = source.getValue() / 100f;
        viewer.setMinBondDistance(minBondDistance);
        currentProperties.put("minBondDistance", "" + minBondDistance);
        viewer.rebond();
      }
    });
    bdPanel.add(bdSlider);

    c.weightx = 0.0;
    gridbag.setConstraints(bdPanel, c);
    bondPanel.add(bdPanel);

    return bondPanel;
  }

  public JPanel buildColorsPanel() {

    JPanel colorPanel = new JPanel();
    colorPanel.setLayout(new GridLayout(0, 2));

    JPanel backgroundPanel = new JPanel();
    backgroundPanel.setLayout(new BorderLayout());
    backgroundPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.bgLabel")));
    bButton = new JButton();
    bButton.setBackground(colorBackground);
    bButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.bgToolTip"));
    ActionListener startBackgroundChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(bButton, JmolResourceHandler.getInstance()
              .getString("Prefs.bgChooserTitle"), colorBackground);
        colorBackground = color;
        bButton.setBackground(colorBackground);
        viewer.setColorBackground(colorBackground);
        currentProperties.put("colorBackground",
            Integer.toString(colorBackground.getRGB()));
      }
    };
    bButton.addActionListener(startBackgroundChooser);
    backgroundPanel.add(bButton, BorderLayout.CENTER);
    colorPanel.add(backgroundPanel);

    JPanel pickedPanel = new JPanel();
    pickedPanel.setLayout(new BorderLayout());
    pickedPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.pickedLabel")));
    pButton = new JButton();
    pButton.setBackground(colorSelection);
    pButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.pickedToolTip"));
    ActionListener startPickedChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(pButton, JmolResourceHandler.getInstance()
              .getString("Prefs.pickedChooserTitle"), colorSelection);
        colorSelection = color;
        pButton.setBackground(colorSelection);
        viewer.setColorSelection(colorSelection);
        currentProperties.put("colorSelection", Integer.toString(colorSelection.getRGB()));
      }
    };
    pButton.addActionListener(startPickedChooser);
    pickedPanel.add(pButton, BorderLayout.CENTER);
    colorPanel.add(pickedPanel);

    // text color panel
    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BorderLayout());
    textPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.textLabel")));

    isLabelAtomColor = viewer.getColorLabel() == null;
    cbIsLabelAtomColor =
      guimap.newJCheckBox("Prefs.isLabelAtomColor", isLabelAtomColor);
    cbIsLabelAtomColor.addItemListener(checkBoxListener);
    textPanel.add(cbIsLabelAtomColor, BorderLayout.NORTH);

    tButton = new JButton();
    tButton.setBackground(colorText);
    tButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.textToolTip"));
    tButton.setEnabled(!isLabelAtomColor);
    ActionListener startTextChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(tButton, JmolResourceHandler.getInstance()
              .getString("Prefs.textChooserTitle"), colorText);
        colorText = color;
        tButton.setBackground(colorText);
        viewer.setColorLabel(colorText);
        currentProperties.put("colorText", Integer.toString(colorText.getRGB()));
      }
    };
    tButton.addActionListener(startTextChooser);
    textPanel.add(tButton, BorderLayout.CENTER);
    colorPanel.add(textPanel);

    // bond color panel
    JPanel bondPanel = new JPanel();
    bondPanel.setLayout(new BorderLayout());
    bondPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.bondLabel")));

    isBondAtomColor = viewer.getColorBond() == null;
    cbIsBondAtomColor =
      guimap.newJCheckBox("Prefs.isBondAtomColor", isBondAtomColor);
    cbIsBondAtomColor.addItemListener(checkBoxListener);
    bondPanel.add(cbIsBondAtomColor, BorderLayout.NORTH);

    eButton = new JButton();
    eButton.setBackground(colorBond);
    eButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.textToolTip"));
    eButton.setEnabled(!isBondAtomColor);
    ActionListener startBondChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(eButton, JmolResourceHandler.getInstance()
              .getString("Prefs.bondChooserTitle"), colorBond);
        colorBond = color;
        eButton.setBackground(colorBond);
        viewer.setColorBond(colorBond);
        currentProperties.put("colorBond", "" + colorBond.getRGB());
      }
    };
    eButton.addActionListener(startBondChooser);
    bondPanel.add(eButton, BorderLayout.CENTER);
    colorPanel.add(bondPanel);

    // vector color panel
    JPanel vectorPanel = new JPanel();
    vectorPanel.setLayout(new BorderLayout());
    vectorPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.vectorLabel")));
    vButton = new JButton();
    vButton.setBackground(colorVector);
    vButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.vectorToolTip"));
    ActionListener startVectorChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(vButton, JmolResourceHandler.getInstance()
              .getString("Prefs.vectorChooserTitle"), colorVector);
        colorVector = color;
        vButton.setBackground(colorVector);
        viewer.setColorVector(colorVector);
        currentProperties.put("colorVector", Integer.toString(colorVector.getRGB()));
        viewer.refresh();
      }
    };
    vButton.addActionListener(startVectorChooser);
    vectorPanel.add(vButton, BorderLayout.CENTER);
    colorPanel.add(vectorPanel);

    // measurement color panel
    JPanel measurementColorPanel = new JPanel();
    measurementColorPanel.setLayout(new BorderLayout());
    measurementColorPanel
      .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
                                  .getString("Prefs.measurementColorLabel")));
    measurementColorButton = new JButton();
    measurementColorButton.setBackground(colorVector);
    measurementColorButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.measurementColorToolTip"));
    ActionListener startMeasurementColorChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(measurementColorButton,
                        JmolResourceHandler.getInstance()
                        .getString("Prefs.measurementColorChooserTitle"),
                        colorMeasurement);
        colorMeasurement = color;
        measurementColorButton.setBackground(colorMeasurement);
        viewer.setColorMeasurement(colorMeasurement);
        currentProperties.put("colorMeasurement",
                              Integer.toString(colorMeasurement.getRGB()));
        viewer.refresh();
      }
    };
    measurementColorButton.addActionListener(startMeasurementColorChooser);
    measurementColorPanel.add(measurementColorButton, BorderLayout.CENTER);
    colorPanel.add(measurementColorPanel);

    return colorPanel;
  }

  public JPanel buildVibratePanel() {

    JPanel vibratePanel = new JPanel();
    vibratePanel.setLayout(new GridLayout(0, 1));

    JPanel notePanel = new JPanel();
    notePanel.setLayout(new BorderLayout());
    notePanel.setBorder(new EtchedBorder());
    JLabel noteLabel =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.vibNoteLabel"));
    notePanel.add(noteLabel, BorderLayout.CENTER);
    vibratePanel.add(notePanel);

    JPanel vasPanel = new JPanel();
    vasPanel.setLayout(new BorderLayout());
    vasPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.vibAmplitudeScaleLabel")));
    vasSlider = new JSlider(JSlider.HORIZONTAL, 0, 200,
        (int) (100.0 * Vibrate.getAmplitudeScale()));
    vasSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    vasSlider.setPaintTicks(true);
    vasSlider.setMajorTickSpacing(40);
    vasSlider.setPaintLabels(true);
    vasSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());
    vasSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());
    vasSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());
    vasSlider.getLabelTable().put(new Integer(120),
        new JLabel("1.2", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());
    vasSlider.getLabelTable().put(new Integer(160),
        new JLabel("1.6", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());
    vasSlider.getLabelTable().put(new Integer(200),
        new JLabel("2.0", JLabel.CENTER));
    vasSlider.setLabelTable(vasSlider.getLabelTable());

    vasSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        VibrateAmplitudeScale = source.getValue() / 100.0;
        Vibrate.setAmplitudeScale(VibrateAmplitudeScale);
        currentProperties.put("VibrateAmplitudeScale",
            Double.toString(VibrateAmplitudeScale));
      }
    });
    vasPanel.add(vasSlider, BorderLayout.SOUTH);
    vibratePanel.add(vasPanel);

    JPanel vvsPanel = new JPanel();
    vvsPanel.setLayout(new BorderLayout());
    vvsPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.vibVectorScaleLabel")));
    vvsSlider = new JSlider(JSlider.HORIZONTAL, 0, 200,
        (int) (100.0 * Vibrate.getVectorScale()));
    vvsSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    vvsSlider.setPaintTicks(true);
    vvsSlider.setMajorTickSpacing(40);
    vvsSlider.setPaintLabels(true);
    vvsSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());
    vvsSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());
    vvsSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());
    vvsSlider.getLabelTable().put(new Integer(120),
        new JLabel("1.2", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());
    vvsSlider.getLabelTable().put(new Integer(160),
        new JLabel("1.6", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());
    vvsSlider.getLabelTable().put(new Integer(200),
        new JLabel("2.0", JLabel.CENTER));
    vvsSlider.setLabelTable(vvsSlider.getLabelTable());

    vvsSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        VibrateVectorScale = source.getValue() / 100.0;
        Vibrate.setVectorScale(VibrateVectorScale);
        currentProperties.put("VibrateVectorScale", Double.toString(VibrateVectorScale));
      }
    });
    vvsPanel.add(vvsSlider, BorderLayout.SOUTH);
    vibratePanel.add(vvsPanel);

    JPanel vfPanel = new JPanel();
    vfPanel.setLayout(new BorderLayout());
    vfPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.vibFrameLabel")));

    vfSlider = new JSlider(JSlider.HORIZONTAL, 0, 50,
        Vibrate.getNumberFrames());
    vfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    vfSlider.setPaintTicks(true);
    vfSlider.setMajorTickSpacing(5);
    vfSlider.setPaintLabels(true);
    vfSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        VibrationFrames = source.getValue();
        Vibrate.setNumberFrames(VibrationFrames);
        currentProperties.put("VibrationFrames", Integer.toString(VibrationFrames));
      }
    });

    vfPanel.add(vfSlider, BorderLayout.SOUTH);
    vibratePanel.add(vfPanel);

    return vibratePanel;
  }

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

  public void ok() {
    save();
    dispose();
  }

  public void cancel() {
    updateComponents();
    dispose();
  }

  private void updateComponents() {
    // Display panel
    cH.setSelected(viewer.getShowHydrogens());
    cV.setSelected(viewer.getShowVectors());
    cM.setSelected(viewer.getShowMeasurements());

    cbWireframeRotation.setSelected(viewer.getWireframeRotation());

    cbPerspectiveDepth.setSelected(viewer.getPerspectiveDepth());
    cbShowAxes.setSelected(viewer.getShowAxes());
    cbShowBoundingBox.setSelected(viewer.
                                  getShapeShow(JmolConstants.SHAPE_BBOX));

    cbOrientationRasMolChime.setSelected(viewer.getOrientationRasMolChime());

    // Atom panel controls: 
    aLabel.setSelectedIndex(viewer.getStyleLabel());
    vdwPercentSlider.setValue(viewer.getPercentVdwAtom());

    // Bond panel controls:
    abYes.setSelected(viewer.getAutoBond());
    bwSlider.setValue(viewer.getMarBond());
    bdSlider.setValue((int) (100 * viewer.getMinBondDistance()));
    btSlider.setValue((int) (100 * viewer.getBondTolerance()));

    // Color panel controls:
    bButton.setBackground(colorBackground);
    pButton.setBackground(colorSelection);
    cbIsLabelAtomColor.setSelected(isLabelAtomColor);
    tButton.setBackground(colorText);
    tButton.setEnabled(!isLabelAtomColor);
    cbIsBondAtomColor.setSelected(isBondAtomColor);
    eButton.setBackground(colorBond);
    eButton.setEnabled(!isBondAtomColor);
    vButton.setBackground(colorVector);
    measurementColorButton.setBackground(colorMeasurement);

    // Vibrate panel controls
    vasSlider.setValue((int) (100.0 * Vibrate.getAmplitudeScale()));
    vvsSlider.setValue((int) (100.0 * Vibrate.getVectorScale()));
    vfSlider.setValue(Vibrate.getNumberFrames());

  }

  private void save() {
    try {
      FileOutputStream fileOutputStream =
        new FileOutputStream(Jmol.UserPropsFile);
      currentProperties.store(fileOutputStream, "Jmol");
      fileOutputStream.close();
    } catch (Exception e) {
      System.out.println("Error saving preferences" + e);
    }
    viewer.refresh();
  }

  void initializeProperties() {
    originalSystemProperties = System.getProperties();
    jmolDefaultProperties = new Properties(originalSystemProperties);
    for (int i = jmolDefaults.length; (i -= 2) >= 0; )
      jmolDefaultProperties.put(jmolDefaults[i], jmolDefaults[i+1]);
    currentProperties = new Properties(jmolDefaultProperties);
    try {
      FileInputStream fis2 = new FileInputStream(Jmol.UserPropsFile);
      currentProperties.load(new BufferedInputStream(fis2, 1024));
      fis2.close();
    } catch (Exception e2) {
    }
    System.setProperties(currentProperties);
  }

  void resetDefaults(String[] overrides) {
    currentProperties = new Properties(jmolDefaultProperties);
    System.setProperties(currentProperties);
    if (overrides != null) {
      for (int i = overrides.length; (i -= 2) >= 0; )
        currentProperties.put(overrides[i], overrides[i+1]);
    }
    initVariables();
    viewer.refresh();
    updateComponents();
  }

  void initVariables() {

    autoBond = Boolean.getBoolean("autoBond");
    showHydrogens = Boolean.getBoolean("showHydrogens");
    showVectors = Boolean.getBoolean("showVectors");
    showMeasurements = Boolean.getBoolean("showMeasurements");
    wireframeRotation = Boolean.getBoolean("wireframeRotation");
    perspectiveDepth = Boolean.getBoolean("perspectiveDepth");
    showAxes = Boolean.getBoolean("showAxes");
    showBoundingBox = Boolean.getBoolean("showBoundingBox");
    orientationRasMolChime = Boolean.getBoolean("orientationRasMolChime");
    colorBackground = Color.getColor("colorBackground");
    colorSelection = Color.getColor("colorSelection");
    isLabelAtomColor = Boolean.getBoolean("isLabelAtomColor");
    colorText = Color.getColor("colorText");
    isBondAtomColor = Boolean.getBoolean("isBondAtomColor");
    colorBond = Color.getColor("colorBond");
    colorVector = Color.getColor("colorVector");
    colorMeasurement = Color.getColor("colorMeasurement");
    styleLabel = (byte)Integer.getInteger("styleLabel").intValue();
    VibrationFrames = Integer.getInteger("VibrationFrames").intValue();

    minBondDistance =
      new Float(currentProperties.getProperty("minBondDistance")).floatValue();
    bondTolerance =
      new Float(currentProperties.getProperty("bondTolerance")).floatValue();
    marBond = Short.parseShort(currentProperties.getProperty("marBond"));
    percentVdwAtom =
      Integer.parseInt(currentProperties.getProperty("percentVdwAtom"));
    VibrateAmplitudeScale =
        new Double(currentProperties.getProperty("VibrateAmplitudeScale")).doubleValue();
    VibrateVectorScale =
        new Double(currentProperties.getProperty("VibrateVectorScale")).doubleValue();

    //    viewer.setColorOutline(colorOutline);
    viewer.setColorSelection(colorSelection);
    viewer.setColorLabel(isLabelAtomColor ? null : colorText);
    viewer.setColorBond(isBondAtomColor ? null : colorBond);
    viewer.setPercentVdwAtom(percentVdwAtom);
    viewer.setStyleLabel(styleLabel);
    //viewer.setPropertyStyleString(AtomPropsMode);
    viewer.setMarBondDefault(marBond);
    viewer.setColorVector(colorVector);
    viewer.setColorMeasurement(colorMeasurement);
    viewer.setColorBackground(colorBackground);
    viewer.setMinBondDistance(minBondDistance);
    viewer.setBondTolerance(bondTolerance);
    viewer.setAutoBond(autoBond);
    viewer.setShowHydrogens(showHydrogens);
    viewer.setShowVectors(showVectors);
    viewer.setShowMeasurements(showMeasurements);
    viewer.setWireframeRotation(wireframeRotation);
    viewer.setPerspectiveDepth(perspectiveDepth);
    viewer.setShowAxes(showAxes);
    viewer.setShapeShow(JmolConstants.SHAPE_BBOX, showBoundingBox);
    viewer.setOrientationRasMolChime(orientationRasMolChime);
    Vibrate.setAmplitudeScale(VibrateAmplitudeScale);
    Vibrate.setVectorScale(VibrateVectorScale);
    Vibrate.setNumberFrames(VibrationFrames);
  }

  class PrefsAction extends AbstractAction {

    public PrefsAction() {
      super("prefs");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      show();
    }
  }

  public Action[] getActions() {
    Action[] defaultActions = {
      prefsAction
    };
    return defaultActions;
  }

  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  ItemListener checkBoxListener = new ItemListener() {

    Component c;
    AbstractButton b;

    public void itemStateChanged(ItemEvent e) {

      JCheckBox cb = (JCheckBox) e.getSource();
      String key = guimap.getKey(cb);
      boolean isSelected = cb.isSelected();
      String strSelected = isSelected ? "true" : "false";
      if (key.equals("Prefs.showHydrogens")) {
        showHydrogens = isSelected;
        viewer.setShowHydrogens(showHydrogens);
        currentProperties.put("showHydrogens", strSelected);
      } else if (key.equals("Prefs.showVectors")) {
        showVectors = isSelected;
        viewer.setShowVectors(showVectors);
        currentProperties.put("showVectors", strSelected);
      } else if (key.equals("Prefs.showMeasurements")) {
        showMeasurements = isSelected;
        viewer.setShowMeasurements(showMeasurements);
        currentProperties.put("showMeasurements", strSelected);
      } else if (key.equals("Prefs.isLabelAtomColor")) {
        isLabelAtomColor = isSelected;
        viewer.setColorLabel(isLabelAtomColor ? null : colorText);
        currentProperties.put("isLabelAtomColor", strSelected);
        tButton.setEnabled(!isLabelAtomColor);
      } else if (key.equals("Prefs.isBondAtomColor")) {
        isBondAtomColor = isSelected;
        viewer.setColorBond(isBondAtomColor ? null : colorBond);
        currentProperties.put("isBondAtomColor", strSelected);
        eButton.setEnabled(!isBondAtomColor);
      } else if (key.equals("Prefs.wireframeRotation")) {
        wireframeRotation = isSelected;
        viewer.setWireframeRotation(wireframeRotation);
        currentProperties.put("wireframeRotation", strSelected);
      } else if (key.equals("Prefs.perspectiveDepth")) {
        perspectiveDepth = isSelected;
        viewer.setPerspectiveDepth(perspectiveDepth);
        currentProperties.put("perspectiveDepth", strSelected);
      } else if (key.equals("Prefs.showAxes")) {
        showAxes = isSelected;
        viewer.setShowAxes(showAxes);
        currentProperties.put("showAxes", strSelected);
      } else if (key.equals("Prefs.showBoundingBox")) {
        showBoundingBox = isSelected;
        viewer.setShapeShow(JmolConstants.SHAPE_BBOX, isSelected);
        currentProperties.put("showBoundingBox", strSelected);
      } else if (key.equals("Prefs.orientationRasMolChime")) {
        orientationRasMolChime = isSelected;
        viewer.setOrientationRasMolChime(isSelected);
        currentProperties.put("orientationRasMolChime", strSelected);
      }
    }
  };

  private JButton applyButton;
  private JButton jmolDefaultsButton;
  private JButton rasmolDefaultsButton;
  private JButton cancelButton;
  private JButton okButton;
  
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == applyButton) {
      save();
    } else if (event.getSource() == jmolDefaultsButton) {
      resetDefaults(null);
    } else if (event.getSource() == rasmolDefaultsButton) {
      resetDefaults(rasmolOverrides);
    } else if (event.getSource() == cancelButton) {
      cancel();
    } else if (event.getSource() == okButton) {
      ok();
    }
  }

}
