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

import org.openscience.jmol.DisplayControl;
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
  private static boolean graphics2D;
  private static boolean antialias;
  private static boolean antialiasAlways;
  private static boolean showHydrogens;
  private static boolean showVectors;
  private static boolean showMeasurements;
  private static boolean wireframeRotation;
  private static boolean perspectiveDepth;
  private static boolean showAxes;
  private static boolean showBoundingBox;
  private static boolean showDarkerOutline;
  private static boolean isLabelAtomColor;
  private static boolean isBondAtomColor;
  private static Color colorBackground;
  private static Color colorOutline;
  private static Color colorSelection;
  private static Color colorText;
  private static Color colorBond;
  private static Color colorVector;
  private static byte styleAtom;
  private static byte modeAtomColorProfile;
  private static byte styleLabel;
  private static String AtomPropsMode;
  private static byte styleBond;
  private static double ArrowHeadSize;
  private static double ArrowHeadRadius;
  private static double ArrowLengthScale;
  private static double minBondDistance;
  private static double bondTolerance;
  private static short marBond;
  private static double FieldOfView;
  private static int percentVdwAtom;
  private static double VibrateAmplitudeScale;
  private static double VibrateVectorScale;
  private static int VibrationFrames;
  private JButton bButton, oButton, pButton, tButton, eButton, vButton;
  private JRadioButton pYes, pNo, abYes, abNo;
  private JComboBox aRender, aLabel, aProps, bRender, cRender;
  private JSlider fovSlider, sfSlider;
  private JSlider bdSlider, bwSlider, btSlider, ahSlider, arSlider, alSlider;
  private JSlider vasSlider;
  private JSlider vvsSlider;
  private JSlider vfSlider;
  private JCheckBox cH, cV, cM;
  private JCheckBox cbWireframeRotation, cbPerspectiveDepth;
  private JCheckBox cbShowAxes, cbShowBoundingBox;
  private JCheckBox cbDarkerOutline, cbIsLabelAtomColor, cbIsBondAtomColor;
  private JCheckBox cbGraphics2D, cbAntialias, cbAntialiasAlways;
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

    props.put("showHydrogens", "true");
    props.put("showVectors", "true");
    props.put("showMeasurements", "true");
    props.put("wireframeRotation", "false");
    props.put("perspectiveDepth", "true");
    props.put("showAxes", "false");
    props.put("showBoundingBox", "false");
    props.put("showDarkerOutline", "true");
    props.put("isLabelAtomColor", "false");
    props.put("isBondAtomColor", "true");
    // mth 2003 05 20
    // The Apple JVMs have a problem with graphics2D operations
    // so, we will default to turning graphics2D off until
    // they get their issues resolved.
    // props.put("graphics2D", "true");
    String vendor = System.getProperty("java.vendor");
    String strBool = vendor.startsWith("Apple Computer") ? "false" : "true";
    props.put("graphics2D", strBool);
    //
    props.put("antialias", "true");
    props.put("antialiasAlways", "false");
    props.put("Perspective", "false");
    props.put("FieldOfView", "20.0");
    props.put("styleAtom", "1");
    props.put("styleBond", "1");
    props.put("styleLabel", "0");
    props.put("AtomPropsMode", "");
    props.put("percentVdwAtom", "20");
    props.put("AutoBond", "true");
    props.put("marBond", "100");
    props.put("minBondDistance", "0.40");
    props.put("bondTolerance", "0.45");
    props.put("ArrowHeadSize", "1.0");
    props.put("ArrowHeadRadius", "1.0");
    props.put("ArrowLengthScale", "1.0");
    props.put("colorBackground", "16777215");
    props.put("colorOutline", "0");
    props.put("colorSelection", "16762880");
    props.put("colorText", "0");
    props.put("colorBond", "0");
    props.put("colorVector", "0");
    props.put("VibrateAmplitudeScale", "0.7");
    props.put("VibrateVectorScale", "1.0");
    props.put("VibrationFrames", "20");
    props = new Properties(props);
  }

  private DisplayControl control;
  private GuiMap guimap;

  public PreferencesDialog(JFrame f, GuiMap guimap, DisplayControl control) {

    super(f, false);
    this.guimap = guimap;
    this.control = control;

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

    JPanel g2dPanel = new JPanel();
    g2dPanel.setLayout(new GridLayout(0, 4));
    g2dPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.graphics2DPanelLabel")));
    //    graphics2D = control.getWantsGraphics2D();
    cbGraphics2D = guimap.newJCheckBox("Prefs.graphics2D", graphics2D);
    cbGraphics2D.addItemListener(checkBoxListener);
    //    antialias = control.getWantsAntialias();
    cbAntialias = guimap.newJCheckBox("Prefs.antialias", antialias);
    cbAntialias.addItemListener(checkBoxListener);
    //    antialiasAlways = control.getWantsAntialiasAlways();
    cbAntialiasAlways = guimap.newJCheckBox("Prefs.antialiasAlways",
                                            antialiasAlways);
    cbAntialiasAlways.addItemListener(checkBoxListener);
    g2dPanel.add(cbGraphics2D);
    g2dPanel.add(cbAntialias);
    g2dPanel.add(cbAntialiasAlways);
    setEnabledGraphics();
    
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(g2dPanel, constraints);

        
    JLabel filler = new JLabel();
    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(filler, constraints);

    JPanel showPanel = new JPanel();
    showPanel.setLayout(new GridLayout(1, 3));
    showPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
          .getString("Prefs.showLabel")));
    cH = guimap.newJCheckBox("Prefs.showHydrogens",
                             control.getShowHydrogens());
    cH.addItemListener(checkBoxListener);
    cV = guimap.newJCheckBox("Prefs.showVectors", control.getShowVectors());
    cV.addItemListener(checkBoxListener);
    cM = guimap.newJCheckBox("Prefs.showMeasurements",
                             control.getShowMeasurements());
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
    fooPanel.setLayout(new GridLayout(2, 1));

    cbWireframeRotation =
      guimap.newJCheckBox("Prefs.wireframeRotation",
                          control.getWireframeRotation());
    cbWireframeRotation.addItemListener(checkBoxListener);
    fooPanel.add(cbWireframeRotation);

    cbPerspectiveDepth =
      guimap.newJCheckBox("Prefs.perspectiveDepth",
                          control.getPerspectiveDepth());
    cbPerspectiveDepth.addItemListener(checkBoxListener);
    fooPanel.add(cbPerspectiveDepth);

    cbShowAxes =
      guimap.newJCheckBox("Prefs.showAxes", control.getShowAxes());
    cbShowAxes.addItemListener(checkBoxListener);
    fooPanel.add(cbShowAxes);

    cbShowBoundingBox =
      guimap.newJCheckBox("Prefs.showBoundingBox",
                          control.getShowBoundingBox());
    cbShowBoundingBox.addItemListener(checkBoxListener);
    fooPanel.add(cbShowBoundingBox);



    constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    disp.add(fooPanel, constraints);


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
    aRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aInvisibleChoice"));
    aRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.aNoneChoice"));
    aRender.setSelectedIndex(control.getStyleAtom());
    aRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        styleAtom = (byte)source.getSelectedIndex();
        control.setStyleAtom(styleAtom);
        props.put("styleAtom", Integer.toString(styleAtom));
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
    cRender.setSelectedIndex(control.getModeAtomColorProfile());
    cRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        modeAtomColorProfile = (byte)source.getSelectedIndex();
        control.setModeAtomColorProfile(modeAtomColorProfile);
        props.put("modeAtomColorProfile", ""+modeAtomColorProfile);
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
    aLabel.setSelectedIndex(control.getStyleLabel());
    aLabel.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        styleLabel = (byte)source.getSelectedIndex();
        control.setStyleLabel(styleLabel);
        props.put("styleLabel", "" + styleLabel);
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
    aProps.setSelectedItem(control.getPropertyStyleString());
    aProps.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        AtomPropsMode = (String) source.getSelectedItem();
        control.setPropertyStyleString(AtomPropsMode);
        props.put("AtomPropsMode", AtomPropsMode);
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
    sfSlider =
      new JSlider(JSlider.HORIZONTAL, 0, 100, control.getPercentVdwAtom());
    sfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
    sfSlider.setPaintTicks(true);
    sfSlider.setMajorTickSpacing(20);
    sfSlider.setMinorTickSpacing(10);
    sfSlider.setPaintLabels(true);
    sfSlider.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {

        JSlider source = (JSlider) e.getSource();
        percentVdwAtom = source.getValue();
        control.setPercentVdwAtom(percentVdwAtom);
        props.put("percentVdwAtom", "" + percentVdwAtom);
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
        .getString("Prefs.bBoxChoice"));
    bRender.addItem(JmolResourceHandler.getInstance()
        .getString("Prefs.bNoneChoice"));
    bRender.setSelectedIndex(control.getStyleBond());
    bRender.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        JComboBox source = (JComboBox) e.getSource();
        styleBond = (byte)source.getSelectedIndex();
        control.setStyleBond(styleBond);
        props.put("styleBond", "" + styleBond);
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
    abYes.setSelected(control.getAutoBond());
    abYes.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        control.setAutoBond(true);
      }
    });

    abNo.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        control.setAutoBond(false);
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

    bwSlider = new JSlider(0, 250,control.getMarBond());
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
        control.setMarBond(marBond);
        props.put("marBond", "" + marBond);
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
        (int) (100 * control.getBondTolerance()));
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
        bondTolerance = source.getValue() / 100.0;
        control.setBondTolerance(bondTolerance);
        props.put("bondTolerance", Double.toString(bondTolerance));
        control.rebond();
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
        (int) (100 * control.getMinBondDistance()));
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
        minBondDistance = source.getValue() / 100.0;
        control.setMinBondDistance(minBondDistance);
        props.put("minBondDistance", Double.toString(minBondDistance));
        control.rebond();
      }
    });
    bdPanel.add(bdSlider);

    c.weightx = 0.0;
    gridbag.setConstraints(bdPanel, c);
    bondPanel.add(bdPanel);

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
        (int) (100.0f * control.getArrowHeadSize()));
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
        control.setArrowHeadSize(ArrowHeadSize);
        props.put("ArrowHeadSize", Double.toString(ArrowHeadSize));
      }
    });
    ahPanel.add(ahSlider, BorderLayout.SOUTH);
    vPanel.add(ahPanel);

    JPanel arPanel = new JPanel();
    arPanel.setLayout(new BorderLayout());
    arPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.arLabel")));
    arSlider = new JSlider(JSlider.HORIZONTAL, 0, 200,
        (int) (100.0f * control.getArrowHeadRadius()));
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
        control.setArrowHeadRadius(ArrowHeadRadius);
        props.put("ArrowHeadRadius", Double.toString(ArrowHeadRadius));
        control.refresh();
      }
    });
    arPanel.add(arSlider, BorderLayout.SOUTH);
    vPanel.add(arPanel);

    JPanel alPanel = new JPanel();
    alPanel.setLayout(new BorderLayout());
    alPanel.setBorder(new TitledBorder(JmolResourceHandler.getInstance()
        .getString("Prefs.alLabel")));
    alSlider = new JSlider(JSlider.HORIZONTAL, -200, 200,
        (int) (100.0f * control.getArrowLengthScale()));
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
        control.setArrowLengthScale(ArrowLengthScale);
        props.put("ArrowLengthScale", Double.toString(ArrowLengthScale));
        control.refresh();
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
        control.setColorBackground(colorBackground);
        props.put("colorBackground",
            Integer.toString(colorBackground.getRGB()));
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

    showDarkerOutline = control.getShowDarkerOutline();
    cbDarkerOutline =
      guimap.newJCheckBox("Prefs.showDarkerOutline", showDarkerOutline);
    cbDarkerOutline.addItemListener(checkBoxListener);
    outlinePanel.add(cbDarkerOutline, BorderLayout.NORTH);

    oButton = new JButton();
    oButton.setBackground(colorOutline);
    oButton.setToolTipText(JmolResourceHandler.getInstance()
        .getString("Prefs.outlineToolTip"));
    oButton.setEnabled(!showDarkerOutline);
    ActionListener startOutlineChooser = new ActionListener() {

      public void actionPerformed(ActionEvent e) {

        Color color =
          JColorChooser
            .showDialog(oButton, JmolResourceHandler.getInstance()
              .getString("Prefs.outlineChooserTitle"), colorOutline);
        colorOutline = color;
        oButton.setBackground(colorOutline);
        control.setColorOutline(colorOutline);
        props.put("colorOutline", Integer.toString(colorOutline.getRGB()));
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
        control.setColorSelection(colorSelection);
        props.put("colorSelection", Integer.toString(colorSelection.getRGB()));
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

    isLabelAtomColor = control.getColorLabel() == null;
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
        control.setColorLabel(colorText);
        props.put("colorText", Integer.toString(colorText.getRGB()));
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

    isBondAtomColor = control.getColorBond() == null;
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
        control.setColorBond(colorBond);
        props.put("colorBond", "" + colorBond.getRGB());
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
        control.setColorVector(colorVector);
        props.put("colorVector", Integer.toString(colorVector.getRGB()));
        control.refresh();
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
    // Display panel
    cbGraphics2D.setSelected(control.getWantsGraphics2D());
    cbAntialias.setSelected(control.getWantsAntialias());
    cbAntialiasAlways.setSelected(control.getWantsAntialiasAlways());

    cH.setSelected(control.getShowHydrogens());
    cV.setSelected(control.getShowVectors());
    cM.setSelected(control.getShowMeasurements());

    cbWireframeRotation.setSelected(control.getWireframeRotation());

    cbPerspectiveDepth.setSelected(control.getPerspectiveDepth());
    cbShowAxes.setSelected(control.getShowAxes());
    cbShowBoundingBox.setSelected(control.getShowBoundingBox());

    // Atom panel controls: 
    aRender.setSelectedIndex(control.getStyleAtom());
    aLabel.setSelectedIndex(control.getStyleLabel());
    sfSlider.setValue(control.getPercentVdwAtom());

    // Bond panel controls:
    bRender.setSelectedIndex(control.getStyleBond());
    abYes.setSelected(control.getAutoBond());
    bwSlider.setValue(control.getMarBond());
    bdSlider.setValue((int) (100 * control.getMinBondDistance()));
    btSlider.setValue((int) (100 * control.getBondTolerance()));

    // Vector panel controls:
    ahSlider.setValue((int) (100.0f * control.getArrowHeadSize()));
    arSlider.setValue((int) (100.0f * control.getArrowHeadRadius()));
    alSlider.setValue((int) (100.0f * control.getArrowLengthScale()));

    // Color panel controls:
    bButton.setBackground(colorBackground);
    cbDarkerOutline.setSelected(showDarkerOutline);
    oButton.setBackground(colorOutline);
    oButton.setEnabled(!showDarkerOutline);
    pButton.setBackground(colorSelection);
    cbIsLabelAtomColor.setSelected(isLabelAtomColor);
    tButton.setBackground(colorText);
    tButton.setEnabled(!isLabelAtomColor);
    cbIsBondAtomColor.setSelected(isBondAtomColor);
    eButton.setBackground(colorBond);
    eButton.setEnabled(!isBondAtomColor);
    vButton.setBackground(colorVector);

    // Vibrate panel controls
    vasSlider.setValue((int) (100.0 * Vibrate.getAmplitudeScale()));
    vvsSlider.setValue((int) (100.0 * Vibrate.getVectorScale()));
    vfSlider.setValue(Vibrate.getNumberFrames());

  }

  private void save() {
    try {
      FileOutputStream fileOutputStream =
        new FileOutputStream(Jmol.UserPropsFile);
      props.store(fileOutputStream, "Jmol");
      fileOutputStream.close();
    } catch (Exception e) {
      System.out.println("Error saving preferences" + e);
    }
    control.refresh();
  }

  public void ResetPressed() {

    defaults();
    initVariables();
    control.refresh();

    updateComponents();
    
    save();
    return;
  }

  void initVariables() {

    AutoBond = Boolean.getBoolean("AutoBond");
    Perspective = Boolean.getBoolean("Perspective");
    graphics2D = Boolean.getBoolean("graphics2D");
    antialias = Boolean.getBoolean("antialias");
    antialiasAlways = Boolean.getBoolean("antialiasAlways");
    showHydrogens = Boolean.getBoolean("showHydrogens");
    showVectors = Boolean.getBoolean("showVectors");
    showMeasurements = Boolean.getBoolean("showMeasurements");
    wireframeRotation = Boolean.getBoolean("wireframeRotation");
    perspectiveDepth = Boolean.getBoolean("perspectiveDepth");
    showAxes = Boolean.getBoolean("showAxes");
    showBoundingBox = Boolean.getBoolean("showBoundingBox");
    showDarkerOutline = Boolean.getBoolean("showDarkerOutline");
    colorBackground = Color.getColor("colorBackground");
    colorOutline = Color.getColor("colorOutline");
    colorSelection = Color.getColor("colorSelection");
    isLabelAtomColor = Boolean.getBoolean("isLabelAtomColor");
    colorText = Color.getColor("colorText");
    isBondAtomColor = Boolean.getBoolean("isBondAtomColor");
    colorBond = Color.getColor("colorBond");
    colorVector = Color.getColor("colorVector");
    styleAtom = (byte)Integer.getInteger("styleAtom").intValue();
    styleLabel = (byte)Integer.getInteger("styleLabel").intValue();
    AtomPropsMode = props.getProperty("AtomPropsMode");
    styleBond = (byte)Integer.getInteger("styleBond").intValue();
    VibrationFrames = Integer.getInteger("VibrationFrames").intValue();

    // Doubles and Doubles are special:
    ArrowHeadSize =
        new Double(props.getProperty("ArrowHeadSize")).doubleValue();
    ArrowHeadRadius =
        new Double(props.getProperty("ArrowHeadRadius")).doubleValue();
    ArrowLengthScale =
        new Double(props.getProperty("ArrowLengthScale")).doubleValue();
    minBondDistance =
      new Double(props.getProperty("minBondDistance")).doubleValue();
    bondTolerance =
      new Double(props.getProperty("bondTolerance")).doubleValue();
    marBond = Short.parseShort(props.getProperty("marBond"));
    FieldOfView = new Double(props.getProperty("FieldOfView")).doubleValue();
    percentVdwAtom =
      Integer.parseInt(props.getProperty("percentVdwAtom"));
    VibrateAmplitudeScale =
        new Double(props.getProperty("VibrateAmplitudeScale")).doubleValue();
    VibrateVectorScale =
        new Double(props.getProperty("VibrateVectorScale")).doubleValue();

    control.setColorOutline(colorOutline);
    control.setColorSelection(colorSelection);
    control.setColorLabel(isLabelAtomColor ? null : colorText);
    control.setColorBond(isBondAtomColor ? null : colorBond);
    control.setPercentVdwAtom(percentVdwAtom);
    control.setStyleAtom(styleAtom);
    control.setStyleLabel(styleLabel);
    control.setPropertyStyleString(AtomPropsMode);
    control.setStyleBond(styleBond);
    control.setMarBond(marBond);
    control.setColorVector(colorVector);
    control.setArrowHeadRadius(ArrowHeadRadius);
    control.setArrowHeadSize(ArrowHeadSize);
    control.setArrowLengthScale(ArrowLengthScale);
    control.setColorBackground(colorBackground);
    control.setWantsGraphics2D(graphics2D);
    control.setWantsAntialias(antialias);
    control.setWantsAntialiasAlways(antialiasAlways);
    control.setMinBondDistance(minBondDistance);
    control.setBondTolerance(bondTolerance);
    control.setAutoBond(AutoBond);
    control.setShowHydrogens(showHydrogens);
    control.setShowVectors(showVectors);
    control.setShowMeasurements(showMeasurements);
    control.setWireframeRotation(wireframeRotation);
    control.setPerspectiveDepth(perspectiveDepth);
    control.setShowAxes(showAxes);
    control.setShowBoundingBox(showBoundingBox);
    control.setShowDarkerOutline(showDarkerOutline);
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
        control.setShowHydrogens(showHydrogens);
        props.put("showHydrogens", strSelected);
      } else if (key.equals("Prefs.showVectors")) {
        showVectors = isSelected;
        control.setShowVectors(showVectors);
        props.put("showVectors", strSelected);
      } else if (key.equals("Prefs.showMeasurements")) {
        showMeasurements = isSelected;
        control.setShowMeasurements(showMeasurements);
        props.put("showMeasurements", strSelected);
      } else if (key.equals("Prefs.showDarkerOutline")) {
        showDarkerOutline = isSelected;
        control.setShowDarkerOutline(showDarkerOutline);
        props.put("showDarkerOutline", strSelected);
        oButton.setEnabled(!showDarkerOutline);
      } else if (key.equals("Prefs.isLabelAtomColor")) {
        isLabelAtomColor = isSelected;
        control.setColorLabel(isLabelAtomColor ? null : colorText);
        props.put("isLabelAtomColor", strSelected);
        tButton.setEnabled(!isLabelAtomColor);
      } else if (key.equals("Prefs.isBondAtomColor")) {
        isBondAtomColor = isSelected;
        control.setColorBond(isBondAtomColor ? null : colorBond);
        props.put("isBondAtomColor", strSelected);
        eButton.setEnabled(!isBondAtomColor);
      } else if (key.equals("Prefs.graphics2D")) {
        graphics2D = isSelected;
        control.setWantsGraphics2D(graphics2D);
        props.put("graphics2D", strSelected);
        setEnabledGraphics();
      } else if (key.equals("Prefs.antialias")) {
        antialias = isSelected;
        control.setWantsAntialias(antialias);
        props.put("antialias", strSelected);
        setEnabledGraphics();
      } else if (key.equals("Prefs.antialiasAlways")) {
        antialiasAlways = isSelected;
        control.setWantsAntialiasAlways(antialiasAlways);
        props.put("antialiasAlways", strSelected);
      } else if (key.equals("Prefs.wireframeRotation")) {
        wireframeRotation = isSelected;
        control.setWireframeRotation(wireframeRotation);
        props.put("wireframeRotation", strSelected);
      } else if (key.equals("Prefs.perspectiveDepth")) {
        perspectiveDepth = isSelected;
        control.setPerspectiveDepth(perspectiveDepth);
        props.put("perspectiveDepth", strSelected);
      } else if (key.equals("Prefs.showAxes")) {
        showAxes = isSelected;
        control.setShowAxes(showAxes);
        props.put("showAxes", strSelected);
      } else if (key.equals("Prefs.showBoundingBox")) {
        showBoundingBox = isSelected;
        control.setShowBoundingBox(showBoundingBox);
        props.put("showBoundingBox", strSelected);
      }
    }
  };

  private void setEnabledGraphics() {
    cbAntialias.setEnabled(graphics2D);
    cbAntialiasAlways.setEnabled(graphics2D && antialias);
  }

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
