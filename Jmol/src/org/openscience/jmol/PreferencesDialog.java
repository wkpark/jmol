
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

import org.openscience.jmol.render.ArrowLine;
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

  private static boolean AutoBond;
  private static boolean Perspective;
  private static boolean showAtoms;
  private static boolean showBonds;
  private static boolean showHydrogens;
  private static boolean showVectors;
  private static boolean showDarkerOutline;
  private static Color backgroundColor;
  private static Color outlineColor;
  private static Color pickedColor;
  private static Color textColor;
  private static Color vectorColor;
  private static int AtomRenderMode;
  private static int AtomColorProfile;
  private static int AtomLabelMode;
  private static String AtomPropsMode;
  private static int BondRenderMode;
  private static float ArrowHeadSize;
  private static float ArrowHeadRadius;
  private static float ArrowLengthScale;
  private static float BondFudge;
  private static double BondWidth;
  private static float FieldOfView;
  private static double SphereFactor;
  private static double VibrateAmplitudeScale;
  private static double VibrateVectorScale;
  private static int VibrationFrames;
  private DisplayPanel display;
  private JButton bButton, oButton, pButton, tButton, vButton;
  private JRadioButton antiAliasedYes, antiAliasedNo;
  private JRadioButton pYes, pNo, abYes, abNo;
  private JComboBox aRender, aLabel, aProps, bRender, cRender;
  private JSlider fovSlider, sfSlider;
  private JSlider bfSlider, bwSlider, ahSlider, arSlider, alSlider;
  private JSlider vasSlider;
  private JSlider vvsSlider;
  private JSlider vfSlider;
  private JCheckBox cB, cA, cV, cH;
  private JCheckBox cbDarkerOutline;
  private static Properties props;

  // The actions:

  private PrefsAction prefsAction = new PrefsAction();
  private Hashtable commands;

  static {
    props = System.getProperties();
    defaults();
    try {
      FileInputStream fis2 = new FileInputStream(Jmol.UserPropsFile);
      props.load(new BufferedInputStream(fis2, 1024));
      fis2.close();
    } catch (Exception e2) {
    }
    System.setProperties(props);
  }

  private static void defaults() {

    props.put("showAtoms", "true");
    props.put("showBonds", "true");
    props.put("showHydrogens", "true");
    props.put("showVectors", "false");
    props.put("showDarkerOutline", "false");
    props.put("AntiAliased", "false");
    props.put("Perspective", "false");
    props.put("FieldOfView", "20.0");
    props.put("AtomRenderMode", "0");
    props.put("AtomLabelMode", "0");
    props.put("AtomPropsMode", "");
    props.put("SphereFactor", "0.2");
    props.put("BondRenderMode", "0");
    props.put("AutoBond", "true");
    props.put("BondWidth", "0.1");
    props.put("BondFudge", "1.12");
    props.put("ArrowHeadSize", "1.0");
    props.put("ArrowHeadRadius", "1.0");
    props.put("ArrowLengthScale", "1.0");
    props.put("backgroundColor", "16777215");
    props.put("outlineColor", "0");
    props.put("pickedColor", "16762880");
    props.put("textColor", "0");
    props.put("vectorColor", "0");
    props.put("VibrateAmplitudeScale", "0.7");
    props.put("VibrateVectorScale", "1.0");
    props.put("VibrationFrames", "20");
    props = new Properties(props);
  }

  public PreferencesDialog(JFrame f, DisplayPanel dp) {

    super(f, false);
    JmolResourceHandler jrh = JmolResourceHandler.getInstance();
    this.setTitle(jrh.translate("Preferences"));

    this.display = dp;
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
    JPanel vectors = buildVectorsPanel();
    JPanel colors = buildColorsPanel();
    JPanel vibrate = buildVibratePanel();
    tabs.addTab(jrh.getString("Prefs.displayLabel"), null, disp);
    tabs.addTab(jrh.getString("Prefs.atomsLabel"), null, atoms);
    tabs.addTab(jrh.getString("Prefs.bondsLabel"), null, bonds);
    tabs.addTab(jrh.getString("Prefs.vectorsLabel"), null, vectors);
    tabs.addTab(jrh.getString("Prefs.colorsLabel"), null, colors);
    tabs.addTab(jrh.getString("Prefs.vibrateLabel"), null, vibrate);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    resetButton = new JButton(jrh.getString("Prefs.resetLabel"));
    resetButton.addActionListener(this);
    buttonPanel.add(resetButton);

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

    JLabel antiAliasedLabel = new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.antiAliasedLabel"));
    constraints = new GridBagConstraints();
    disp.add(antiAliasedLabel, constraints);
        
    ButtonGroup antiAliasedGroup = new ButtonGroup();
    antiAliasedYes = new JRadioButton(JmolResourceHandler.getInstance()
        .getString("Prefs.antiAliasedYesLabel"));
    antiAliasedNo = new JRadioButton(JmolResourceHandler.getInstance()
        .getString("Prefs.antiAliasedNoLabel"));
    antiAliasedGroup.add(antiAliasedYes);
    antiAliasedGroup.add(antiAliasedNo);
    constraints = new GridBagConstraints();
    disp.add(antiAliasedYes, constraints);
    disp.add(antiAliasedNo, constraints);
    
    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(filler, constraints);

    JPanel showPanel = new JPanel();
    showPanel.setLayout(new GridLayout(0, 4));
    showPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.showLabel")));
    cA = new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Prefs.showAtomsLabel"), display.getSettings().getShowAtoms());
    cA.addItemListener(checkBoxListener);
    cB = new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Prefs.showBondsLabel"), display.getSettings().getShowBonds());
    cB.addItemListener(checkBoxListener);
    cV = new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Prefs.showVectorsLabel"), display.getSettings().getShowVectors());
    cV.addItemListener(checkBoxListener);
    cH = new JCheckBox(JmolResourceHandler.getInstance()
        .getString("Prefs.showHydrogensLabel"), display.getSettings()
          .getShowHydrogens());
    cH.addItemListener(checkBoxListener);
    showPanel.add(cA);
    showPanel.add(cB);
    showPanel.add(cV);
    showPanel.add(cH);

    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(showPanel, constraints);

    filler = new JLabel();
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

    JLabel atomStyleLabel = new JLabel(JmolResourceHandler.getInstance()
          .getString("Prefs.atomStyleLabel"));
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.EAST;
    atomPanel.add(atomStyleLabel, constraints);
    aRender = new JComboBox();
    aRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aQDChoice"));
    aRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aSChoice"));
    aRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aWFChoice"));
    aRender.setSelectedIndex(display.getSettings().getAtomDrawMode());
    aRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        AtomRenderMode = source.getSelectedIndex();
        display.getSettings().setAtomDrawMode(AtomRenderMode);
        props.put("AtomRenderMode", Integer.toString(AtomRenderMode));
        display.repaint();
      }
    });

    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    atomPanel.add(aRender, constraints);

    JLabel atomColoringLabel = new JLabel(JmolResourceHandler.getInstance()
          .getString("Prefs.atomColoringLabel"));
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.EAST;
    atomPanel.add(atomColoringLabel, constraints);
    cRender = new JComboBox();
    cRender.addItem(JmolResourceHandler.getInstance().getString("Prefs.cATChoice"));
    cRender.addItem(JmolResourceHandler.getInstance().getString("Prefs.cCChoice"));
    cRender.setSelectedIndex(display.getSettings().getAtomColorProfile());
    cRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        AtomColorProfile = source.getSelectedIndex();
        display.getSettings().setAtomColorProfile(AtomColorProfile);
        props.put("AtomColorProfile", Integer.toString(AtomColorProfile));
        display.repaint();
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
    aLabel.setSelectedIndex(display.getSettings().getLabelMode());
    aLabel.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        AtomLabelMode = source.getSelectedIndex();
        display.getSettings().setLabelMode(AtomLabelMode);
        props.put("AtomLabelMode", Integer.toString(AtomLabelMode));
        display.repaint();
      }
    });
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    atomPanel.add(aLabel, constraints);

    JLabel propertyLabelsLabel = new JLabel(JmolResourceHandler.getInstance()
          .getString("Prefs.propertyLabelsLabel"));
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.EAST;
    atomPanel.add(propertyLabelsLabel, constraints);
    aProps = new JComboBox();
    aProps.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.apPChoice"));
    aProps.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.apCChoice"));
    aProps.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.apNChoice"));
    aProps.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.apUChoice"));
    aProps.setSelectedItem(display.getSettings().getPropertyMode());
    aProps.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        AtomPropsMode = (String) source.getSelectedItem();
        display.getSettings().setPropertyMode(AtomPropsMode);
        props.put("AtomPropsMode", AtomPropsMode);
        display.repaint();
      }
    });
    constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    atomPanel.add(aProps, constraints);

    JPanel sfPanel = new JPanel();
    sfPanel.setLayout(new BorderLayout());
    sfPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.atomSizeLabel")));
    JLabel sfLabel = new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.atomSizeExpl"), JLabel.CENTER);
    sfPanel.add(sfLabel, BorderLayout.NORTH);
    sfSlider = new JSlider(JSlider.HORIZONTAL, 0, 100,
        (int) (100.0 * display.getSettings().getAtomSphereFactor()));
    sfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    sfSlider.setPaintTicks(true);
    sfSlider.setMajorTickSpacing(20);
    sfSlider.setMinorTickSpacing(10);
    sfSlider.setPaintLabels(true);
    sfSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        SphereFactor = source.getValue() / 100.0;
        display.getSettings().setAtomSphereFactor(SphereFactor);
        props.put("SphereFactor", Double.toString(SphereFactor));
        display.repaint();
      }
    });
    sfPanel.add(sfSlider, BorderLayout.CENTER);
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    atomPanel.add(sfPanel, constraints);

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

    JPanel renderPanel = new JPanel();
    renderPanel.setLayout(new BoxLayout(renderPanel, BoxLayout.Y_AXIS));
    renderPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.bRenderStyleLabel")));
    bRender = new JComboBox();
    bRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.bQDChoice"));
    bRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.bSChoice"));
    bRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.bWFChoice"));
    bRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.bLChoice"));
    bRender.setSelectedIndex(display.getSettings().getBondDrawMode());
    bRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        BondRenderMode = source.getSelectedIndex();
        display.getSettings().setBondDrawMode(BondRenderMode);
        props.put("BondRenderMode", Integer.toString(BondRenderMode));
        display.repaint();
      }
    });
    renderPanel.add(bRender);
    gridbag.setConstraints(renderPanel, c);
    bondPanel.add(renderPanel);

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
    abYes.setSelected(ChemFrame.getAutoBond());
    abYes.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        ChemFrame.setAutoBond(true);
        display.repaint();
      }
    });

    abNo.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        ChemFrame.setAutoBond(false);
        display.repaint();
      }
    });

    c.gridwidth = GridBagConstraints.REMAINDER;
    gridbag.setConstraints(autobondPanel, c);
    bondPanel.add(autobondPanel);

    JPanel bwPanel = new JPanel();
    bwPanel.setLayout(new BorderLayout());
    bwPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.bondWidthLabel")));
    JLabel bwLabel =
      new JLabel(JmolResourceHandler.getInstance()
        .getString("Prefs.bondWidthExpl"), JLabel.CENTER);
    bwPanel.add(bwLabel, BorderLayout.NORTH);
    bwSlider = new JSlider(JSlider.HORIZONTAL, 0, 100,
        (int) (100.0 * display.getSettings().getBondWidth()));
    bwSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bwSlider.setPaintTicks(true);
    bwSlider.setMajorTickSpacing(20);
    bwSlider.setMinorTickSpacing(10);
    bwSlider.setPaintLabels(true);
    bwSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());
    bwSlider.getLabelTable().put(new Integer(20),
        new JLabel("0.2", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());
    bwSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());
    bwSlider.getLabelTable().put(new Integer(60),
        new JLabel("0.6", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());
    bwSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());
    bwSlider.getLabelTable().put(new Integer(100),
        new JLabel("1.0", JLabel.CENTER));
    bwSlider.setLabelTable(bwSlider.getLabelTable());

    bwSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        BondWidth = source.getValue() / 100.0;
        display.getSettings().setBondWidth((float) BondWidth);
        props.put("BondWidth", Double.toString(BondWidth));
        display.repaint();
      }
    });

    bwPanel.add(bwSlider, BorderLayout.SOUTH);

    c.weightx = 0.0;
    gridbag.setConstraints(bwPanel, c);
    bondPanel.add(bwPanel);

    JPanel bfPanel = new JPanel();
    bfPanel.setLayout(new BorderLayout());
    bfPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.bondFudgeLabel")));
    bfSlider = new JSlider(JSlider.HORIZONTAL, 0, 100,
        (int) (50.0 * ChemFrame.getBondFudge()));
    bfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    bfSlider.setPaintTicks(true);
    bfSlider.setMajorTickSpacing(20);
    bfSlider.setMinorTickSpacing(10);
    bfSlider.setPaintLabels(true);
    bfSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());
    bfSlider.getLabelTable().put(new Integer(20),
        new JLabel("0.4", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());
    bfSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.8", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());
    bfSlider.getLabelTable().put(new Integer(60),
        new JLabel("1.2", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());
    bfSlider.getLabelTable().put(new Integer(80),
        new JLabel("1.6", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());
    bfSlider.getLabelTable().put(new Integer(100),
        new JLabel("2.0", JLabel.CENTER));
    bfSlider.setLabelTable(bfSlider.getLabelTable());

    bfSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        BondFudge = source.getValue() / 50.0f;

        // this doesn't make me happy, but we don't want static
        // reference to ChemFrame here.  We only want to rebond
        // the current frame. (I think).
        ChemFrame.setBondFudge(BondFudge);
        props.put("BondFudge", Float.toString(BondFudge));
        try {
          display.rebond();
        } catch (Exception ex) {
        }
        display.repaint();
      }
    });
    bfPanel.add(bfSlider);


    c.weightx = 0.0;
    gridbag.setConstraints(bfPanel, c);
    bondPanel.add(bfPanel);

    return bondPanel;
  }

  public JPanel buildVectorsPanel() {

    JPanel vPanel = new JPanel();
    vPanel.setLayout(new GridLayout(0, 1));

    JPanel sample = new JPanel();
    sample.setLayout(new BorderLayout());
    sample.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.sampleLabel")));
    vPanel.add(sample);

    JPanel ahPanel = new JPanel();
    ahPanel.setLayout(new BorderLayout());
    ahPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.ahLabel")));
    ahSlider = new JSlider(JSlider.HORIZONTAL, 0, 200,
        (int) (100.0f * ArrowLine.getArrowHeadSize()));
    ahSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    ahSlider.setPaintTicks(true);
    ahSlider.setMajorTickSpacing(40);
    ahSlider.setPaintLabels(true);
    ahSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());
    ahSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());
    ahSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());
    ahSlider.getLabelTable().put(new Integer(120),
        new JLabel("1.2", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());
    ahSlider.getLabelTable().put(new Integer(160),
        new JLabel("1.6", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());
    ahSlider.getLabelTable().put(new Integer(200),
        new JLabel("2.0", JLabel.CENTER));
    ahSlider.setLabelTable(ahSlider.getLabelTable());

    ahSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        ArrowHeadSize = source.getValue() / 100.0f;
        ArrowLine.setArrowHeadSize(ArrowHeadSize);
        props.put("ArrowHeadSize", Float.toString(ArrowHeadSize));
        display.repaint();
      }
    });
    ahPanel.add(ahSlider, BorderLayout.SOUTH);
    vPanel.add(ahPanel);

    JPanel arPanel = new JPanel();
    arPanel.setLayout(new BorderLayout());
    arPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.arLabel")));
    arSlider = new JSlider(JSlider.HORIZONTAL, 0, 200,
        (int) (100.0f * ArrowLine.getArrowHeadRadius()));
    arSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    arSlider.setPaintTicks(true);
    arSlider.setMajorTickSpacing(40);
    arSlider.setPaintLabels(true);
    arSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.getLabelTable().put(new Integer(40),
        new JLabel("0.4", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.getLabelTable().put(new Integer(80),
        new JLabel("0.8", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.getLabelTable().put(new Integer(120),
        new JLabel("1.2", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.getLabelTable().put(new Integer(160),
        new JLabel("1.6", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.getLabelTable().put(new Integer(200),
        new JLabel("2.0", JLabel.CENTER));
    arSlider.setLabelTable(arSlider.getLabelTable());
    arSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        ArrowHeadRadius = source.getValue() / 100.0f;
        ArrowLine.setArrowHeadRadius(ArrowHeadRadius);
        props.put("ArrowHeadRadius", Float.toString(ArrowHeadRadius));
        display.repaint();
      }
    });
    arPanel.add(arSlider, BorderLayout.SOUTH);
    vPanel.add(arPanel);

    JPanel alPanel = new JPanel();
    alPanel.setLayout(new BorderLayout());
    alPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.alLabel")));
    alSlider = new JSlider(JSlider.HORIZONTAL, -200, 200,
        (int) (100.0f * ArrowLine.getLengthScale()));
    alSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    alSlider.setPaintTicks(true);
    alSlider.setMajorTickSpacing(50);
    alSlider.setPaintLabels(true);
    alSlider.getLabelTable().put(new Integer(-200),
        new JLabel("-2.0", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(-150),
        new JLabel("-1.5", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(-100),
        new JLabel("-1.0", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(-50),
        new JLabel("-0.5", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(0),
        new JLabel("0.0", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(50),
        new JLabel("0.5", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(100),
        new JLabel("1.0", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(150),
        new JLabel("1.5", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.getLabelTable().put(new Integer(200),
        new JLabel("2.0", JLabel.CENTER));
    alSlider.setLabelTable(alSlider.getLabelTable());
    alSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        ArrowLengthScale = source.getValue() / 100.0f;
        ArrowLine.setLengthScale(ArrowLengthScale);
        props.put("ArrowLengthScale", Float.toString(ArrowLengthScale));
        display.repaint();
      }
    });
    alPanel.add(alSlider, BorderLayout.SOUTH);
    vPanel.add(alPanel);

    return vPanel;
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
    bButton.setBackground(backgroundColor);
    bButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.bgToolTip"));
    ActionListener startBackgroundChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(bButton, JmolResourceHandler.getInstance()
              .getString("Prefs.bgChooserTitle"), backgroundColor);
        backgroundColor = color;
        bButton.setBackground(backgroundColor);
        DisplayPanel.setBackgroundColor(backgroundColor);
        props.put("backgroundColor",
            Integer.toString(backgroundColor.getRGB()));
        display.repaint();

      }
    };
    bButton.addActionListener(startBackgroundChooser);
    backgroundPanel.add(bButton, BorderLayout.CENTER);
    colorPanel.add(backgroundPanel);

    JPanel outlinePanel = new JPanel();
    outlinePanel.setLayout(new BorderLayout());
    outlinePanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.outlineLabel")));

    cbDarkerOutline =
      new JCheckBox(JmolResourceHandler.getInstance()
                    .getString("Prefs.showDarkerOutlineLabel"),
                    display.getSettings().getShowDarkerOutline());
    cbDarkerOutline.addItemListener(checkBoxListener);
    outlinePanel.add(cbDarkerOutline, BorderLayout.NORTH);

    oButton = new JButton();
    oButton.setBackground(outlineColor);
    oButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.outlineToolTip"));
    ActionListener startOutlineChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(oButton, JmolResourceHandler.getInstance()
              .getString("Prefs.outlineChooserTitle"), outlineColor);
        outlineColor = color;
        oButton.setBackground(outlineColor);
        display.getSettings().setOutlineColor(outlineColor);
        props.put("outlineColor", Integer.toString(outlineColor.getRGB()));
        display.repaint();

      }
    };
    oButton.addActionListener(startOutlineChooser);
    outlinePanel.add(oButton, BorderLayout.CENTER);
    colorPanel.add(outlinePanel);

    JPanel pickedPanel = new JPanel();
    pickedPanel.setLayout(new BorderLayout());
    pickedPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.pickedLabel")));
    pButton = new JButton();
    pButton.setBackground(pickedColor);
    pButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.pickedToolTip"));
    ActionListener startPickedChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(pButton, JmolResourceHandler.getInstance()
              .getString("Prefs.pickedChooserTitle"), pickedColor);
        pickedColor = color;
        pButton.setBackground(pickedColor);
        display.getSettings().setPickedColor(pickedColor);
        props.put("pickedColor", Integer.toString(pickedColor.getRGB()));
        display.repaint();

      }
    };
    pButton.addActionListener(startPickedChooser);
    pickedPanel.add(pButton, BorderLayout.CENTER);
    colorPanel.add(pickedPanel);

    JPanel textPanel = new JPanel();
    textPanel.setLayout(new BorderLayout());
    textPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.textLabel")));
    tButton = new JButton();
    tButton.setBackground(textColor);
    tButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.textToolTip"));
    ActionListener startTextChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(tButton, JmolResourceHandler.getInstance()
              .getString("Prefs.textChooserTitle"), textColor);
        textColor = color;
        tButton.setBackground(textColor);
        display.getSettings().setTextColor(textColor);
        props.put("textColor", Integer.toString(textColor.getRGB()));
        display.repaint();

      }
    };
    tButton.addActionListener(startTextChooser);
    textPanel.add(tButton, BorderLayout.CENTER);
    colorPanel.add(textPanel);

    JPanel vectorPanel = new JPanel();
    vectorPanel.setLayout(new BorderLayout());
    vectorPanel
        .setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.vectorLabel")));
    vButton = new JButton();
    vButton.setBackground(vectorColor);
    vButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.vectorToolTip"));
    ActionListener startVectorChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(vButton, JmolResourceHandler.getInstance()
              .getString("Prefs.vectorChooserTitle"), vectorColor);
        vectorColor = color;
        vButton.setBackground(vectorColor);
        ArrowLine.setVectorColor(vectorColor);
        props.put("vectorColor", Integer.toString(vectorColor.getRGB()));
        display.repaint();

      }
    };
    vButton.addActionListener(startVectorChooser);
    vectorPanel.add(vButton, BorderLayout.CENTER);
    colorPanel.add(vectorPanel);

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
        props.put("VibrateAmplitudeScale",
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
        props.put("VibrateVectorScale", Double.toString(VibrateVectorScale));
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
        props.put("VibrationFrames", Integer.toString(VibrationFrames));
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
    // Display panel controls:
    if (display.getSettings().isAntiAliased()) {
      antiAliasedYes.setSelected(true);
    } else {
      antiAliasedNo.setSelected(true);
    }
    cB.setSelected(display.getSettings().getShowBonds());
    cA.setSelected(display.getSettings().getShowAtoms());
    cV.setSelected(display.getSettings().getShowVectors());
    cH.setSelected(display.getSettings().getShowHydrogens());

    // Atom panel controls:
    aRender.setSelectedIndex(display.getSettings().getAtomDrawMode());
    aLabel.setSelectedIndex(display.getSettings().getLabelMode());
    sfSlider.setValue((int) (100.0
        * display.getSettings().getAtomSphereFactor()));

    // Bond panel controls:
    bRender.setSelectedIndex(display.getSettings().getBondDrawMode());
    abYes.setSelected(ChemFrame.getAutoBond());
    bwSlider.setValue((int) (100.0 * display.getSettings().getBondWidth()));
    bfSlider.setValue((int) (50.0 * ChemFrame.getBondFudge()));

    // Vector panel controls:
    ahSlider.setValue((int) (100.0f * ArrowLine.getArrowHeadSize()));
    arSlider.setValue((int) (100.0f * ArrowLine.getArrowHeadRadius()));
    alSlider.setValue((int) (100.0f * ArrowLine.getLengthScale()));

    // Color panel controls:
    bButton.setBackground(backgroundColor);
    cbDarkerOutline.setSelected(display.getSettings().getShowDarkerOutline());
    oButton.setBackground(outlineColor);
    pButton.setBackground(pickedColor);
    tButton.setBackground(textColor);
    vButton.setBackground(vectorColor);

    // Vibrate panel controls
    vasSlider.setValue((int) (100.0 * Vibrate.getAmplitudeScale()));
    vvsSlider.setValue((int) (100.0 * Vibrate.getVectorScale()));
    vfSlider.setValue(Vibrate.getNumberFrames());

  }

  private void save() {
    
    boolean antiAliased = antiAliasedYes.isSelected();
    display.getSettings().setAntiAliased(antiAliased);
    props.put("AntiAliased", new Boolean(antiAliased).toString());
    
    try {
      FileOutputStream fileOutputStream =
        new FileOutputStream(Jmol.UserPropsFile);
      props.store(fileOutputStream, "Jmol");
      fileOutputStream.close();
    } catch (Exception e) {
      System.out.println("Error saving preferences" + e.toString());
    }
    
    display.repaint();
  }

  public void ResetPressed() {

    defaults();
    initVariables();
    display.repaint();

    updateComponents();
    
    save();
    return;
  }

  void initVariables() {

    AutoBond = Boolean.getBoolean("AutoBond");
    Perspective = Boolean.getBoolean("Perspective");
    showAtoms = Boolean.getBoolean("showAtoms");
    showBonds = Boolean.getBoolean("showBonds");
    showHydrogens = Boolean.getBoolean("showHydrogens");
    showVectors = Boolean.getBoolean("showVectors");
    showDarkerOutline = Boolean.getBoolean("showDarkerOutline");
    backgroundColor = Color.getColor("backgroundColor");
    outlineColor = Color.getColor("outlineColor");
    pickedColor = Color.getColor("pickedColor");
    textColor = Color.getColor("textColor");
    vectorColor = Color.getColor("vectorColor");
    AtomRenderMode = Integer.getInteger("AtomRenderMode").intValue();
    AtomLabelMode = Integer.getInteger("AtomLabelMode").intValue();
    AtomPropsMode = props.getProperty("AtomPropsMode");
    BondRenderMode = Integer.getInteger("BondRenderMode").intValue();
    VibrationFrames = Integer.getInteger("VibrationFrames").intValue();

    // Doubles and Floats are special:
    ArrowHeadSize =
        new Float(props.getProperty("ArrowHeadSize")).floatValue();
    ArrowHeadRadius =
        new Float(props.getProperty("ArrowHeadRadius")).floatValue();
    ArrowLengthScale =
        new Float(props.getProperty("ArrowLengthScale")).floatValue();
    BondFudge = new Float(props.getProperty("BondFudge")).floatValue();
    BondWidth = new Double(props.getProperty("BondWidth")).doubleValue();
    FieldOfView = new Float(props.getProperty("FieldOfView")).floatValue();
    SphereFactor =
        new Double(props.getProperty("SphereFactor")).doubleValue();
    VibrateAmplitudeScale =
        new Double(props.getProperty("VibrateAmplitudeScale")).doubleValue();
    VibrateVectorScale =
        new Double(props.getProperty("VibrateVectorScale")).doubleValue();

    display.getSettings().setOutlineColor(outlineColor);
    display.getSettings().setPickedColor(pickedColor);
    display.getSettings().setTextColor(textColor);
    display.getSettings().setAtomSphereFactor(SphereFactor);
    display.getSettings().setAtomDrawMode(AtomRenderMode);
    display.getSettings().setLabelMode(AtomLabelMode);
    display.getSettings().setPropertyMode(AtomPropsMode);
    display.getSettings().setBondWidth((float) BondWidth);
    display.getSettings().setBondDrawMode(BondRenderMode);
    ArrowLine.setVectorColor(vectorColor);
    ArrowLine.setArrowHeadRadius(ArrowHeadRadius);
    ArrowLine.setArrowHeadSize(ArrowHeadSize);
    ArrowLine.setLengthScale(ArrowLengthScale);
    DisplayPanel.setBackgroundColor(backgroundColor);
    display.getSettings().setAntiAliased(Boolean.getBoolean("AntiAliased"));
    ChemFrame.setBondFudge(BondFudge);
    ChemFrame.setAutoBond(AutoBond);
    display.getSettings().setShowAtoms(showAtoms);
    display.getSettings().setDrawBondsToAtomCenters(!showAtoms);
    display.getSettings().setShowBonds(showBonds);
    display.getSettings().setShowHydrogens(showHydrogens);
    display.getSettings().setShowVectors(showVectors);
    display.getSettings().setShowDarkerOutline(showDarkerOutline);
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
      if (cb.getText()
          .equals(JmolResourceHandler.getInstance()
            .getString("Prefs.showBondsLabel"))) {
        showBonds = cb.isSelected();
        display.getSettings().setShowBonds(showBonds);
        props.put("showBonds", new Boolean(showBonds).toString());
      } else if (cb.getText()
          .equals(JmolResourceHandler.getInstance()
            .getString("Prefs.showAtomsLabel"))) {
        showAtoms = cb.isSelected();
        display.getSettings().setShowAtoms(showAtoms);
        display.getSettings().setDrawBondsToAtomCenters(!showAtoms);
        props.put("showAtoms", new Boolean(showAtoms).toString());
      } else if (cb.getText()
          .equals(JmolResourceHandler.getInstance()
            .getString("Prefs.showVectorsLabel"))) {
        showVectors = cb.isSelected();
        display.getSettings().setShowVectors(showVectors);
        props.put("showVectors", new Boolean(showVectors).toString());
      } else if (cb.getText()
          .equals(JmolResourceHandler.getInstance()
            .getString("Prefs.showHydrogensLabel"))) {
        showHydrogens = cb.isSelected();
        display.getSettings().setShowHydrogens(showHydrogens);
        props.put("showHydrogens", new Boolean(showHydrogens).toString());
      } else if (cb.getText()
                 .equals(JmolResourceHandler.getInstance().
                         getString("Prefs.showDarkerOutlineLabel"))) {
        showDarkerOutline = cb.isSelected();
        display.getSettings().setShowDarkerOutline(showDarkerOutline);
        props.put("showDarkerOutline",
                  new Boolean(showDarkerOutline).toString());
        oButton.setEnabled(!showDarkerOutline);
      }
      display.repaint();
    }
  };

  ItemListener radioButtonListener = new ItemListener() {

    Component c;
    AbstractButton b;

    public void itemStateChanged(ItemEvent e) {

    }
  };

  private JButton applyButton;
  private JButton resetButton;
  private JButton cancelButton;
  private JButton okButton;
  
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == applyButton) {
      save();
    } else if (event.getSource() == resetButton) {
      ResetPressed();
    } else if (event.getSource() == cancelButton) {
      cancel();
    } else if (event.getSource() == okButton) {
      ok();
    }
  }

}
