/*
 * @(#)Preferences.java    1.0 98/08/27
 *
 * Copyright (c) 1998 J. Daniel Gezelter. All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.metal.*;
import javax.swing.JColorChooser.*;

public class Preferences extends JDialog {

    private static boolean AutoBond;
    private static boolean ConfirmExit;
    private static boolean AntiAliased;
    private static boolean Perspective;
    private static boolean UseFileExtensions;
    private static boolean ShowAtoms;
    private static boolean ShowBonds;
    private static boolean ShowHydrogens;
    private static boolean ShowVectors;
    private static boolean ShowDummies;
    private static boolean ShowAxes;
    private static boolean ShowBoundingBox;
    private static Color   backgroundColor;
    private static Color   outlineColor;
    private static Color   pickedColor;
    private static Color   textColor;
    private static Color   vectorColor;
    private static int     AtomRenderMode;
    private static int     AtomLabelMode;
    private static String  AtomPropsMode;
    private static int     BondRenderMode;
    private static float   ArrowHeadLengthScale;
    private static float   ArrowHeadRadiusScale;
    private static double  AutoTimeout;
    private static float   BondFudge;
    private static double  BondWidth;
    private static float   FieldOfView;
    private static double  MessageTime;
    private static double  SphereFactor;
    private static double  VibrationScale;
    private static int     VibrationFrames;
    private displayPanel display;
    private static JmolResourceHandler jrh, defaults;
    private JButton bButton, oButton, pButton, tButton, vButton;
    private JRadioButton ceYes, ceNo, ufeYes, ufeNo, aaYes, aaNo;
    private JRadioButton pYes, pNo, abYes, abNo;
    private JComboBox aRender, aLabel, aProps, bRender;
    private JSlider mtSlider, fovSlider, sfSlider;
    private JSlider bfSlider, bwSlider, ahSlider, arSlider;
    private JSlider vsSlider, vfSlider;
    private JCheckBox cB, cA, cV, cH, cD, cX, cBB;
    private static Properties props;
    
    // The actions:
    
    private PrefsAction prefsAction = new PrefsAction();
    private Hashtable commands;

    static {
        jrh = new JmolResourceHandler("Prefs");
        defaults = new JmolResourceHandler("Defaults");
        props = System.getProperties();
        defaults();
        try {
            FileInputStream fis2 = new FileInputStream(Jmol.UserPropsFile);
            props.load(new BufferedInputStream(fis2, 1024));
            fis2.close();
        } catch (Exception e2) {}
        System.setProperties(props);
    }

    private static void defaults() {
        props.put("ConfirmExit", "false"); 
        props.put("UseFileExtensions", "true");
        props.put("ShowAtoms", "true");
        props.put("ShowBonds", "true");
        props.put("ShowHydrogens", "true");
        props.put("ShowVectors", "false");
        props.put("ShowDummies", "false");
        props.put("ShowAxes", "false");
        props.put("ShowBoundingBox", "false");
        props.put("MessageTime", "5.0");
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
        props.put("ArrowHeadLengthScale", "1.0");
        props.put("ArrowHeadRadiusScale", "1.0");
        props.put("backgroundColor", "16777215");
        props.put("outlineColor", "0");
        props.put("pickedColor", "16762880");
        props.put("textColor", "0");
        props.put("vectorColor", "0");
        props.put("AutoTimeout", "15.0");
        props.put("VibrationScale", "0.7");
        props.put("VibrationFrames", "20");
        props = new Properties(props);
    }        

    public Preferences(JFrame f, displayPanel dp) {
        super(f, "Preferences", false);
        this.display = dp;
        initVariables();
        commands = new Hashtable();
        Action[] actions = getActions();
        for (int i = 0; i < actions.length; i++) {
            Action a = actions[i];
            commands.put(a.getValue(Action.NAME), a);
        }
        JPanel container = new JPanel();
        container.setLayout( new BorderLayout() );
        
        JTabbedPane tabs = new JTabbedPane();
        JPanel general = buildGeneralPanel();
        JPanel disp = buildDispPanel();
        JPanel atoms = buildAtomsPanel();
        JPanel bonds = buildBondPanel();
        JPanel vectors = buildVectorsPanel();
        JPanel colors = buildColorsPanel();
        JPanel vibrate = buildVibratePanel();
        tabs.addTab(jrh.getString("generalLabel"), null, general );
        tabs.addTab(jrh.getString("displayLabel"), null, disp );
        tabs.addTab(jrh.getString("atomsLabel"), null, atoms );
        tabs.addTab(jrh.getString("bondsLabel"), null, bonds );
        tabs.addTab(jrh.getString("vectorsLabel"), null, vectors );
        tabs.addTab(jrh.getString("colorsLabel"), null, colors );
        tabs.addTab(jrh.getString("vibrateLabel"), null, vibrate );
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
        JButton save = new JButton(jrh.getString("saveLabel"));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SavePressed();
            }}); 
        buttonPanel.add(save);
        JButton reset = new JButton(jrh.getString("resetLabel"));
        reset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ResetPressed();
            }}); 
        buttonPanel.add(reset);
        JButton ok = new JButton(jrh.getString("okLabel"));
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OKPressed();
            }});
        buttonPanel.add(ok);
        getRootPane().setDefaultButton(ok);
        
        container.add(tabs, BorderLayout.CENTER) ;
        container.add(buttonPanel, BorderLayout.SOUTH);
                getContentPane().add(container);
        pack();
        centerDialog();
    }
    
    public JPanel buildGeneralPanel() {
        JPanel general = new JPanel();

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        general.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        JPanel cePanel = new JPanel();        
        cePanel.setLayout(new BoxLayout(cePanel, 
                                                 BoxLayout.Y_AXIS));
        cePanel.setBorder( new TitledBorder(jrh.getString("ceLabel")));
        ButtonGroup ceGroup = new ButtonGroup();
        ceYes = new JRadioButton(jrh.getString("ceYesLabel"));
        ceYes.addItemListener(radioButtonListener);                       
        ceNo = new JRadioButton(jrh.getString("ceNoLabel"));
        ceNo.addItemListener(radioButtonListener);                       
        ceGroup.add(ceYes);
        ceGroup.add(ceNo);
        cePanel.add(ceYes);
        cePanel.add(ceNo);
        if (ConfirmExit) {
            ceYes.setSelected(true);
        } else {
            ceNo.setSelected(true);
        }

        gridbag.setConstraints(cePanel,c);
        general.add(cePanel);

        JPanel ufePanel = new JPanel();        
        ufePanel.setLayout(new BoxLayout(ufePanel, 
                                        BoxLayout.Y_AXIS));
        ufePanel.setBorder( new TitledBorder(jrh.getString("ufeLabel")));
        ButtonGroup ufeGroup = new ButtonGroup();
        ufeYes = new JRadioButton(jrh.getString("ufeYesLabel"));
        ufeNo = new JRadioButton(jrh.getString("ufeNoLabel"));
        ufeYes.addItemListener(radioButtonListener);                       
        ufeNo.addItemListener(radioButtonListener);                       
        ufeGroup.add(ufeYes);
        ufeGroup.add(ufeNo);
        ufePanel.add(ufeYes);
        ufePanel.add(ufeNo);
        if (FileTyper.getUseFileExtensions()) {            
            ufeYes.setSelected(true); 
        } else {
            ufeNo.setSelected(true);
        }

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(ufePanel,c);
        general.add(ufePanel);

        JPanel mtPanel = new JPanel();
        mtPanel.setLayout(new BorderLayout());
        mtPanel.setBorder(new TitledBorder(jrh.getString("mtLabel")));      
        JLabel mtLabel = new JLabel(jrh.getString("mtExpl"),
                                    JLabel.CENTER);
        mtPanel.add(mtLabel,BorderLayout.NORTH);
        mtSlider = new JSlider(JSlider.HORIZONTAL, 0, 10, (int) MessageTime);
        mtSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        mtSlider.setPaintTicks(true);
        mtSlider.setMajorTickSpacing(1);
        mtSlider.setPaintLabels(true);
        mtSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                MessageTime = source.getValue();
                props.put("MessageTime", Double.toString(MessageTime));
            }
        });
        mtPanel.add(mtSlider,BorderLayout.SOUTH);

        c.weightx = 0.0;
        gridbag.setConstraints(mtPanel,c);
        general.add(mtPanel);

        return general;
    }

    public JPanel buildDispPanel() {
        JPanel disp = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        disp.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        JPanel aaPanel = new JPanel();        
        aaPanel.setLayout(new BoxLayout(aaPanel, 
                                        BoxLayout.Y_AXIS));
        aaPanel.setBorder( new TitledBorder(jrh.getString("aaLabel")));
        ButtonGroup aaGroup = new ButtonGroup();
        aaYes = new JRadioButton(jrh.getString("aaYesLabel"));
        aaNo = new JRadioButton(jrh.getString("aaNoLabel"));
        aaYes.addItemListener(radioButtonListener);                       
        aaNo.addItemListener(radioButtonListener);                       
        aaGroup.add(aaYes);
        aaGroup.add(aaNo);
        aaPanel.add(aaYes);
        aaPanel.add(aaNo);  
        if (display.getAntiAliased()) {            
            aaYes.setSelected(true);
        } else {
            aaNo.setSelected(true);
        }
        String vers = System.getProperty("java.version");
        if (vers.compareTo("1.2") < 0) {
            aaYes.setEnabled(false);
            aaNo.setEnabled(false); 
        }
        gridbag.setConstraints(aaPanel,c);
        disp.add(aaPanel);

        JPanel pPanel = new JPanel();        
        pPanel.setLayout(new BoxLayout(pPanel, 
                                        BoxLayout.Y_AXIS));
        pPanel.setBorder( new TitledBorder(jrh.getString("pLabel")));
        ButtonGroup pGroup = new ButtonGroup();
        pYes = new JRadioButton(jrh.getString("pYesLabel"));
        pNo = new JRadioButton(jrh.getString("pNoLabel"));
        pYes.addItemListener(radioButtonListener);                       
        pNo.addItemListener(radioButtonListener);                       
        pGroup.add(pYes);
        pGroup.add(pNo);
        pPanel.add(pYes);
        pPanel.add(pNo);
        pYes.setSelected(displayPanel.getPerspective());
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(pPanel,c);
        disp.add(pPanel);

        JPanel choicesPanel = new JPanel();
        choicesPanel.setLayout(new GridLayout(0,4));
        choicesPanel.setBorder( new TitledBorder(jrh.getString("cLabel")));
        cB = new JCheckBox(jrh.getString("cBLabel"), display.getSettings().getShowBonds());
        cB.addItemListener(checkBoxListener);                       
        cA = new JCheckBox(jrh.getString("cALabel"), display.getSettings().getShowAtoms());
        cA.addItemListener(checkBoxListener);                       
        cV = new JCheckBox(jrh.getString("cVLabel"), 
                           display.getSettings().getShowVectors());
        cV.addItemListener(checkBoxListener);                       
        cH = new JCheckBox(jrh.getString("cHLabel"), 
                           display.getSettings().getShowHydrogens());
        cH.addItemListener(checkBoxListener);                       
        cD = new JCheckBox(jrh.getString("cDLabel"),false);
        cD.addItemListener(checkBoxListener);                       
        cX = new JCheckBox(jrh.getString("cXLabel"),false);
        cX.addItemListener(checkBoxListener);                       
        cBB = new JCheckBox(jrh.getString("cBBLabel"),false);
        cBB.addItemListener(checkBoxListener);                       
        cD.setEnabled(false);
        cX.setEnabled(false);
        cBB.setEnabled(false);
        choicesPanel.add(cB);
        choicesPanel.add(cA);
        choicesPanel.add(cBB);
        choicesPanel.add(cX);
        choicesPanel.add(cV);
        choicesPanel.add(cH);
        choicesPanel.add(cD);
      
        JPanel fovPanel = new JPanel();
        fovPanel.setLayout(new BorderLayout());
        fovPanel.setBorder(new TitledBorder(jrh.getString("fovLabel")));
        JLabel fovLabel = new JLabel(jrh.getString("fovExpl"),
                                     JLabel.CENTER);
        fovPanel.add(fovLabel,BorderLayout.NORTH);
        fovSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 
                                (int) FieldOfView);
        fovSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        fovSlider.setPaintTicks(true);
        fovSlider.setMajorTickSpacing(20);
        fovSlider.setMinorTickSpacing(10);
        fovSlider.setPaintLabels(true);
        fovSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                FieldOfView = source.getValue();
                displayPanel.setFieldOfView(FieldOfView);
                props.put("FieldOfView", Float.toString(FieldOfView));
            }
        });
        fovPanel.add(fovSlider,BorderLayout.SOUTH);
        gridbag.setConstraints(fovPanel,c);
        disp.add(fovPanel);
        c.weightx = 0.0;
        gridbag.setConstraints(choicesPanel,c);
        disp.add(choicesPanel);

        return disp;
    }

    public JPanel buildAtomsPanel() {
        JPanel atomPanel = new JPanel();   
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        atomPanel.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        
        JPanel renderPanel = new JPanel();
        renderPanel.setLayout(new BoxLayout(renderPanel,BoxLayout.Y_AXIS));
        renderPanel.setBorder(new TitledBorder(jrh.getString("aRenderStyleLabel")));
        aRender = new JComboBox();
        aRender.addItem(jrh.getString("aQDChoice"));
        aRender.addItem(jrh.getString("aSChoice"));
        aRender.addItem(jrh.getString("aWFChoice"));
        renderPanel.add(aRender);
        aRender.setSelectedIndex(display.getSettings().getAtomDrawMode());
        aRender.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox source = (JComboBox)e.getSource();
                AtomRenderMode = source.getSelectedIndex();
                display.getSettings().setAtomDrawMode(AtomRenderMode);
                props.put("AtomRenderMode", Integer.toString(AtomRenderMode));
                display.repaint();
            }
        });                     
        gridbag.setConstraints(renderPanel,c);
        atomPanel.add(renderPanel);

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel,BoxLayout.Y_AXIS));
        labelPanel.setBorder(new TitledBorder(jrh.getString("aLabelStyleLabel")));
        aLabel = new JComboBox();
        aLabel.addItem(jrh.getString("aPLChoice"));
        aLabel.addItem(jrh.getString("aSLChoice"));
        aLabel.addItem(jrh.getString("aTLChoice"));
        aLabel.addItem(jrh.getString("aNLChoice"));
        labelPanel.add(aLabel);
        aLabel.setSelectedIndex(display.getSettings().getLabelMode());
        aLabel.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox source = (JComboBox)e.getSource();
                AtomLabelMode = source.getSelectedIndex();
                display.getSettings().setLabelMode(AtomLabelMode);
                props.put("AtomLabelMode", Integer.toString(AtomLabelMode));
                display.repaint();
            }
        });                        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labelPanel,c);
        atomPanel.add(labelPanel);

        JPanel propsPanel = new JPanel();
        propsPanel.setLayout(new BoxLayout(propsPanel,BoxLayout.Y_AXIS));
        propsPanel.setBorder(new TitledBorder(jrh.getString("aPropsStyleLabel")));
        aProps = new JComboBox();
        aProps.addItem(jrh.getString("apPChoice"));
        aProps.addItem(jrh.getString("apCChoice"));
        aProps.addItem(jrh.getString("apNChoice"));
        aProps.addItem(jrh.getString("apUChoice"));
        propsPanel.add(aProps);
        aProps.setSelectedItem(display.getSettings().getPropertyMode());
        aProps.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox source = (JComboBox)e.getSource();
                AtomPropsMode = (String)source.getSelectedItem();
                display.getSettings().setPropertyMode(AtomPropsMode);
                props.put("AtomPropsMode", AtomPropsMode);
                display.repaint();
            }
        });                        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(propsPanel,c);
        atomPanel.add(propsPanel);

        JPanel sfPanel = new JPanel();
        sfPanel.setLayout(new BorderLayout());
        sfPanel.setBorder(new TitledBorder(jrh.getString("aSizeLabel")));      
        JLabel sfLabel = new JLabel(jrh.getString("aSizeExpl"),
                                    JLabel.CENTER);
        sfPanel.add(sfLabel,BorderLayout.NORTH);
        sfSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 
                               (int) (100.0 * display.getSettings().getAtomSphereFactor()));
        sfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        sfSlider.setPaintTicks(true);
        sfSlider.setMajorTickSpacing(20);
        sfSlider.setMinorTickSpacing(10);
        sfSlider.setPaintLabels(true);
        sfSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                SphereFactor = source.getValue() / 100.0;
                display.getSettings().setAtomSphereFactor(SphereFactor);
                props.put("SphereFactor", Double.toString(SphereFactor));
                display.repaint();
            }
        });
        sfPanel.add(sfSlider,BorderLayout.SOUTH);

        c.weightx = 0.0;
        gridbag.setConstraints(sfPanel,c);
        atomPanel.add(sfPanel);

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
        renderPanel.setLayout(new BoxLayout(renderPanel,BoxLayout.Y_AXIS));
        renderPanel.setBorder(new TitledBorder(jrh.getString("bRenderStyleLabel")));
        bRender = new JComboBox();
        bRender.addItem(jrh.getString("bQDChoice"));
        bRender.addItem(jrh.getString("bSChoice"));
        bRender.addItem(jrh.getString("bWFChoice"));
        bRender.addItem(jrh.getString("bLChoice"));
        bRender.setSelectedIndex(display.getSettings().getBondDrawMode());
        bRender.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox source = (JComboBox)e.getSource();
                BondRenderMode = source.getSelectedIndex();
                display.getSettings().setBondDrawMode(BondRenderMode);
                props.put("BondRenderMode", Integer.toString(BondRenderMode));
                display.repaint();
            }
        });               
        renderPanel.add(bRender);
        gridbag.setConstraints(renderPanel,c);
        bondPanel.add(renderPanel);

        JPanel autobondPanel = new JPanel();
        autobondPanel.setLayout(new BoxLayout(autobondPanel,BoxLayout.Y_AXIS));
        autobondPanel.setBorder( new TitledBorder(jrh.getString("autoBondLabel")));
        ButtonGroup abGroup = new ButtonGroup();
        abYes = new JRadioButton(jrh.getString("abYesLabel"));
        abNo = new JRadioButton(jrh.getString("abNoLabel"));
        abGroup.add(abYes);
        abGroup.add(abNo);
        autobondPanel.add(abYes);
        autobondPanel.add(abNo);
        autobondPanel.add(Box.createVerticalGlue());
        abYes.setSelected(ChemFrame.getAutoBond());

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(autobondPanel,c);
        bondPanel.add(autobondPanel);

        JPanel bwPanel = new JPanel();
        bwPanel.setLayout(new BorderLayout());
        bwPanel.setBorder(new TitledBorder(jrh.getString("bondWidthLabel")));      
        JLabel bwLabel = new JLabel(jrh.getString("bondWidthExpl"),
                                    JLabel.CENTER);
        bwPanel.add(bwLabel,BorderLayout.NORTH);
        bwSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 
                               (int) (100.0 * display.getSettings().getBondWidth()));
        bwSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        bwSlider.setPaintTicks(true);
        bwSlider.setMajorTickSpacing(20);
        bwSlider.setMinorTickSpacing(10);
        bwSlider.setPaintLabels(true);
        bwSlider.getLabelTable().put(new Integer(0), 
                                     new JLabel("0.0", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );
        bwSlider.getLabelTable().put(new Integer(20), 
                                     new JLabel("0.2", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );
        bwSlider.getLabelTable().put(new Integer(40), 
                                     new JLabel("0.4", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );
        bwSlider.getLabelTable().put(new Integer(60), 
                                     new JLabel("0.6", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );
        bwSlider.getLabelTable().put(new Integer(80), 
                                     new JLabel("0.8", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );
        bwSlider.getLabelTable().put(new Integer(100), 
                                     new JLabel("1.0", JLabel.CENTER));
        bwSlider.setLabelTable( bwSlider.getLabelTable() );

        bwSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                BondWidth = source.getValue() / 100.0;
                display.getSettings().setBondWidth((float)BondWidth);
                props.put("BondWidth", Double.toString(BondWidth));
                display.repaint();
            }
        });

        bwPanel.add(bwSlider,BorderLayout.SOUTH);

        c.weightx = 0.0;
        gridbag.setConstraints(bwPanel,c);
        bondPanel.add(bwPanel);

        JPanel bfPanel = new JPanel();
        bfPanel.setLayout(new BorderLayout());
        bfPanel.setBorder(new TitledBorder(jrh.getString("bondFudgeLabel")));
        bfSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 
                               (int) (50.0 * ChemFrame.getBondFudge()));
        bfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        bfSlider.setPaintTicks(true);
        bfSlider.setMajorTickSpacing(20);
        bfSlider.setMinorTickSpacing(10);
        bfSlider.setPaintLabels(true);
        bfSlider.getLabelTable().put(new Integer(0), 
                                     new JLabel("0.0", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );
        bfSlider.getLabelTable().put(new Integer(20), 
                                     new JLabel("0.4", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );
        bfSlider.getLabelTable().put(new Integer(40), 
                                     new JLabel("0.8", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );
        bfSlider.getLabelTable().put(new Integer(60), 
                                     new JLabel("1.2", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );
        bfSlider.getLabelTable().put(new Integer(80), 
                                     new JLabel("1.6", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );
        bfSlider.getLabelTable().put(new Integer(100), 
                                     new JLabel("2.0", JLabel.CENTER));
        bfSlider.setLabelTable( bfSlider.getLabelTable() );

        bfSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
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
        gridbag.setConstraints(bfPanel,c);
        bondPanel.add(bfPanel);

        return bondPanel;
    }

    public JPanel buildVectorsPanel() {    
        JPanel vPanel = new JPanel();
        vPanel.setLayout(new GridLayout(0,1));
        
        JPanel sample = new JPanel();
        sample.setLayout(new BorderLayout());
        sample.setBorder(new TitledBorder(jrh.getString("sampleLabel")));
        vPanel.add(sample);

        JPanel ahPanel = new JPanel();
        ahPanel.setLayout(new BorderLayout());
        ahPanel.setBorder(new TitledBorder(jrh.getString("ahLabel")));      
        ahSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 
                               (int) (100.0f*ArrowLine.getLengthScale()));
        ahSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        ahSlider.setPaintTicks(true);
        ahSlider.setMajorTickSpacing(40);
        ahSlider.setPaintLabels(true);
        ahSlider.getLabelTable().put(new Integer(0), 
                                     new JLabel("0.0", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );
        ahSlider.getLabelTable().put(new Integer(40), 
                                     new JLabel("0.4", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );
        ahSlider.getLabelTable().put(new Integer(80), 
                                     new JLabel("0.8", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );
        ahSlider.getLabelTable().put(new Integer(120), 
                                     new JLabel("1.2", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );
        ahSlider.getLabelTable().put(new Integer(160), 
                                     new JLabel("1.6", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );
        ahSlider.getLabelTable().put(new Integer(200), 
                                     new JLabel("2.0", JLabel.CENTER));
        ahSlider.setLabelTable( ahSlider.getLabelTable() );

        ahSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                ArrowHeadLengthScale = source.getValue()/100.0f;
                ArrowLine.setLengthScale(ArrowHeadLengthScale);
                props.put("ArrowHeadLengthScale", 
                          Float.toString(ArrowHeadLengthScale));
                display.repaint();
            }
        });
        ahPanel.add(ahSlider,BorderLayout.SOUTH);
        vPanel.add(ahPanel);

        JPanel arPanel = new JPanel();
        arPanel.setLayout(new BorderLayout());
        arPanel.setBorder(new TitledBorder(jrh.getString("arLabel")));      
        arSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 
                               (int) (100.0f*ArrowLine.getRadiusScale()));
        arSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        arSlider.setPaintTicks(true);
        arSlider.setMajorTickSpacing(40);
        arSlider.setPaintLabels(true);
        arSlider.getLabelTable().put(new Integer(0), 
                                     new JLabel("0.0", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.getLabelTable().put(new Integer(40), 
                                     new JLabel("0.4", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.getLabelTable().put(new Integer(80), 
                                     new JLabel("0.8", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.getLabelTable().put(new Integer(120), 
                                     new JLabel("1.2", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.getLabelTable().put(new Integer(160), 
                                     new JLabel("1.6", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.getLabelTable().put(new Integer(200), 
                                     new JLabel("2.0", JLabel.CENTER));
        arSlider.setLabelTable( arSlider.getLabelTable() );
        arSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                ArrowHeadRadiusScale = source.getValue()/100.0f;
                ArrowLine.setRadiusScale(ArrowHeadRadiusScale);
                props.put("ArrowHeadRadiusScale", 
                          Float.toString(ArrowHeadRadiusScale));
                display.repaint();
            }
        });
        arPanel.add(arSlider,BorderLayout.SOUTH);
        vPanel.add(arPanel);

        return vPanel;
    }

    public JPanel buildColorsPanel() {
        JPanel colorPanel = new JPanel();
        colorPanel.setLayout(new GridLayout(0,2));
        
        JPanel backgroundPanel = new JPanel();        
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.setBorder( new TitledBorder(jrh.getString("bgLabel")));
        bButton = new JButton();
        bButton.setBackground(backgroundColor);
        bButton.setToolTipText(jrh.getString("bgToolTip"));
        ActionListener startBackgroundChooser = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(bButton, 
                                                       jrh.getString("bgChooserTitle"), 
                                                       backgroundColor);
                backgroundColor = color;
                bButton.setBackground(backgroundColor);
                displayPanel.setBackgroundColor(backgroundColor);
                props.put("backgroundColor", Integer.toString(backgroundColor.getRGB()));
                display.repaint();
                
            }
        };
        bButton.addActionListener(startBackgroundChooser);
        backgroundPanel.add(bButton,BorderLayout.CENTER);
        colorPanel.add(backgroundPanel);                

        JPanel outlinePanel = new JPanel();        
        outlinePanel.setLayout(new BorderLayout());
        outlinePanel.setBorder( new TitledBorder(jrh.getString("outlineLabel")));
        oButton = new JButton();
        oButton.setBackground(outlineColor);
        oButton.setToolTipText(jrh.getString("outlineToolTip"));
        ActionListener startOutlineChooser = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(oButton, 
                                                       jrh.getString("outlineChooserTitle"), 
                                                       outlineColor);
                outlineColor = color;
                oButton.setBackground(outlineColor);
                display.getSettings().setOutlineColor(outlineColor); 
                props.put("outlineColor", Integer.toString(outlineColor.getRGB()));
                display.repaint();
                
            }
        };
        oButton.addActionListener(startOutlineChooser);
        outlinePanel.add(oButton,BorderLayout.CENTER);
        colorPanel.add(outlinePanel);                

        JPanel pickedPanel = new JPanel();        
        pickedPanel.setLayout(new BorderLayout());
        pickedPanel.setBorder( new TitledBorder(jrh.getString("pickedLabel")) );
        pButton = new JButton();
        pButton.setBackground(pickedColor);
        pButton.setToolTipText(jrh.getString("pickedToolTip"));
        ActionListener startPickedChooser = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(pButton, 
                                                       jrh.getString("pickedChooserTitle"), 
                                                       pickedColor);
                pickedColor = color;
                pButton.setBackground(pickedColor);
                display.getSettings().setPickedColor(pickedColor); 
                props.put("pickedColor", Integer.toString(pickedColor.getRGB()));
                display.repaint();
                
            }
       };
        pButton.addActionListener(startPickedChooser);
        pickedPanel.add(pButton,BorderLayout.CENTER);
        colorPanel.add(pickedPanel);                

        JPanel textPanel = new JPanel();        
        textPanel.setLayout(new BorderLayout());
        textPanel.setBorder( new TitledBorder(jrh.getString("textLabel")) );
        tButton = new JButton();
        tButton.setBackground(textColor);
        tButton.setToolTipText(jrh.getString("textToolTip"));
        ActionListener startTextChooser = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(tButton, 
                                                       jrh.getString("textChooserTitle"), 
                                                       textColor);
                textColor = color;
                tButton.setBackground(textColor);
                display.getSettings().setTextColor(textColor); 
                props.put("textColor", Integer.toString(textColor.getRGB()));
                display.repaint();
                
            }
        };
        tButton.addActionListener(startTextChooser);
        textPanel.add(tButton,BorderLayout.CENTER);
        colorPanel.add(textPanel);

        JPanel vectorPanel = new JPanel();        
        vectorPanel.setLayout(new BorderLayout());
        vectorPanel.setBorder( new TitledBorder(jrh.getString("vectorLabel")) );
        vButton = new JButton();
        vButton.setBackground(vectorColor);
        vButton.setToolTipText(jrh.getString("vectorToolTip"));
        ActionListener startVectorChooser = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(vButton, 
                                                       jrh.getString("vectorChooserTitle"), 
                                                       vectorColor);
                vectorColor = color;
                vButton.setBackground(vectorColor);
                ArrowLine.setVectorColor(vectorColor); 
                props.put("vectorColor", Integer.toString(vectorColor.getRGB()));
                display.repaint();
                
            }
        };
        vButton.addActionListener(startVectorChooser);
        vectorPanel.add(vButton,BorderLayout.CENTER);
        colorPanel.add(vectorPanel);
            
        return colorPanel;
    }

    public JPanel buildVibratePanel() {
        JPanel vibratePanel = new JPanel();
        vibratePanel.setLayout(new GridLayout(0,1));
                
        JPanel notePanel = new JPanel();
        notePanel.setLayout(new BorderLayout());
        notePanel.setBorder(new EtchedBorder());
        JLabel noteLabel = new JLabel(jrh.getString("vibNoteLabel"));
        notePanel.add(noteLabel,BorderLayout.CENTER);
        vibratePanel.add(notePanel);

        JPanel vsPanel = new JPanel();
        vsPanel.setLayout(new BorderLayout());
        vsPanel.setBorder(new TitledBorder(jrh.getString("vibScaleLabel")));
        vsSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 
                               (int) (100.0*Vibrate.getScale()));
        vsSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        vsSlider.setPaintTicks(true);
        vsSlider.setMajorTickSpacing(40);
        vsSlider.setPaintLabels(true);
        vsSlider.getLabelTable().put(new Integer(0), 
                                     new JLabel("0.0", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );
        vsSlider.getLabelTable().put(new Integer(40), 
                                     new JLabel("0.4", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );
        vsSlider.getLabelTable().put(new Integer(80), 
                                     new JLabel("0.8", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );
        vsSlider.getLabelTable().put(new Integer(120), 
                                     new JLabel("1.2", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );
        vsSlider.getLabelTable().put(new Integer(160), 
                                     new JLabel("1.6", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );
        vsSlider.getLabelTable().put(new Integer(200), 
                                     new JLabel("2.0", JLabel.CENTER));
        vsSlider.setLabelTable( vsSlider.getLabelTable() );

        vsSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                VibrationScale = source.getValue()/100.0;
                Vibrate.setScale(VibrationScale);
                props.put("VibrationScale", 
                          Double.toString(VibrationScale));
            }
        });
        vsPanel.add(vsSlider,BorderLayout.SOUTH);
        vibratePanel.add(vsPanel);

        JPanel vfPanel = new JPanel();
        vfPanel.setLayout(new BorderLayout());
        vfPanel.setBorder(new TitledBorder(jrh.getString("vibFrameLabel")));      

        vfSlider = new JSlider(JSlider.HORIZONTAL, 0, 50, Vibrate.getNumberFrames());
        vfSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        vfSlider.setPaintTicks(true);
        vfSlider.setMajorTickSpacing(5);
        vfSlider.setPaintLabels(true);
        vfSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                VibrationFrames = source.getValue();
                Vibrate.setNumberFrames(VibrationFrames);
                props.put("VibrationFrames", Integer.toString(VibrationFrames));
            }
        });

        vfPanel.add(vfSlider,BorderLayout.SOUTH);
        vibratePanel.add(vfPanel);

        return vibratePanel; 
    }
    
    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
        Dimension size = this.getSize();
        screenSize.height = screenSize.height/2;
        screenSize.width = screenSize.width/2;
        size.height = size.height/2;
        size.width = size.width/2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x,y);
    }

    public void OKPressed() {
        this.setVisible(false);
    }

    public void SavePressed() {
        
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(Jmol.UserPropsFile);
            props.save(fileOutputStream, "Jmol");
            // props.store(fileOutputStream, "Jmol");
            fileOutputStream.close();
        } catch (Exception e) {
            System.out.println("Error saving Preferences" + e.toString());
        }
        return;
    }

    public void ResetPressed() { 
        defaults();
        initVariables();
        display.repaint();
        // General panel controls:
        ceYes.setSelected(ConfirmExit);
        // FileTyper controls:
        if (FileTyper.getUseFileExtensions()) {            
            ufeYes.setSelected(true); 
        } else {
            ufeNo.setSelected(true);
        }
        mtSlider.setValue((int) MessageTime);
        // Display panel controls:
        if (display.getAntiAliased()) {            
            aaYes.setSelected(true);
        } else {
            aaNo.setSelected(true);
        }
        pYes.setSelected(displayPanel.getPerspective());
        fovSlider.setValue((int) displayPanel.getFieldOfView());
        cB.setSelected(display.getSettings().getShowBonds());
        cA.setSelected(display.getSettings().getShowAtoms());
        cV.setSelected(display.getSettings().getShowVectors());
        cH.setSelected(display.getSettings().getShowHydrogens());
        // Atom panel controls:
        aRender.setSelectedIndex(display.getSettings().getAtomDrawMode());
        aLabel.setSelectedIndex(display.getSettings().getLabelMode());
        sfSlider.setValue((int) (100.0 * display.getSettings().getAtomSphereFactor()));
        // Bond panel controls:
        bRender.setSelectedIndex(display.getSettings().getBondDrawMode());        
        abYes.setSelected(ChemFrame.getAutoBond());
        bwSlider.setValue((int) (100.0 * display.getSettings().getBondWidth()));
        bfSlider.setValue((int) (50.0 * ChemFrame.getBondFudge()));
        // Vector panel controls:
        ahSlider.setValue((int) (100.0f * ArrowLine.getLengthScale()));
        arSlider.setValue((int) (100.0f * ArrowLine.getRadiusScale()));
        // Color panel controls:
        bButton.setBackground(backgroundColor);
        oButton.setBackground(outlineColor);
        pButton.setBackground(pickedColor);
        tButton.setBackground(textColor);
        vButton.setBackground(vectorColor);
        // Vibrate panel controls
        vsSlider.setValue((int) (100.0 * Vibrate.getScale()));
        vfSlider.setValue(Vibrate.getNumberFrames());
        
        SavePressed();
        return;
    }

    void initVariables() {
        AutoBond = Boolean.getBoolean("AutoBond");
        ConfirmExit = Boolean.getBoolean("ConfirmExit");
        AntiAliased = Boolean.getBoolean("AntiAliased");
        Perspective = Boolean.getBoolean("Perspective");
        UseFileExtensions = Boolean.getBoolean("UseFileExtensions");
        ShowAtoms = Boolean.getBoolean("ShowAtoms");
        ShowBonds = Boolean.getBoolean("ShowBonds");
        ShowHydrogens = Boolean.getBoolean("ShowHydrogens");
        ShowVectors = Boolean.getBoolean("ShowVectors");
        ShowDummies = Boolean.getBoolean("ShowDummies");
        ShowAxes = Boolean.getBoolean("ShowAxes");
        ShowBoundingBox = Boolean.getBoolean("ShowBoundingBox");
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
        ArrowHeadLengthScale = new Float(props.getProperty("ArrowHeadLengthScale")).floatValue();
        ArrowHeadRadiusScale = new Float(props.getProperty("ArrowHeadRadiusScale")).floatValue();
        BondFudge = new Float(props.getProperty("BondFudge")).floatValue();
        AutoTimeout = new Double(props.getProperty("AutoTimeout")).doubleValue();
        BondWidth = new Double(props.getProperty("BondWidth")).doubleValue();
        FieldOfView = new Float(props.getProperty("FieldOfView")).floatValue();
        MessageTime = new Double(props.getProperty("MessageTime")).doubleValue();
        SphereFactor = new Double(props.getProperty("SphereFactor")).doubleValue();
        VibrationScale = new Double(props.getProperty("VibrationScale")).doubleValue();

        display.getSettings().setOutlineColor(outlineColor);            
        display.getSettings().setPickedColor(pickedColor);
        display.getSettings().setTextColor(textColor);
        display.getSettings().setAtomSphereFactor(SphereFactor);
        display.getSettings().setAtomDrawMode(AtomRenderMode);
        display.getSettings().setLabelMode(AtomLabelMode);
        display.getSettings().setPropertyMode(AtomPropsMode);
        display.getSettings().setBondWidth((float)BondWidth);
        display.getSettings().setBondDrawMode(BondRenderMode);
        ArrowLine.setVectorColor(vectorColor);
        ArrowLine.setRadiusScale(ArrowHeadRadiusScale);
        ArrowLine.setLengthScale(ArrowHeadLengthScale);
        displayPanel.setBackgroundColor(backgroundColor);
        displayPanel.setFieldOfView(FieldOfView);
        displayPanel.setPerspective(Perspective);
        display.setAntiAliased(AntiAliased);        
        FileTyper.setUseFileExtensions(UseFileExtensions);
        ChemFrame.setBondFudge(BondFudge);
        ChemFrame.setAutoBond(AutoBond);
        display.getSettings().setShowAtoms(ShowAtoms);
        display.getSettings().setDrawBondsToAtomCenters(!ShowAtoms);
        display.getSettings().setShowBonds(ShowBonds);
        display.getSettings().setShowHydrogens(ShowHydrogens);
        display.getSettings().setShowVectors(ShowVectors);
        Vibrate.setScale(VibrationScale);
        Vibrate.setNumberFrames(VibrationFrames);        
    }

    static boolean getConfirmExit() {
        return ConfirmExit;
    }
    static double getAutoTimeout() {
        return AutoTimeout;
    }
    static double getMessageTime() {
        return MessageTime;
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
            if(cb.getText().equals(jrh.getString("cBLabel"))) {
                ShowBonds = cb.isSelected();
                display.getSettings().setShowBonds(ShowBonds);
                props.put("ShowBonds", new Boolean(ShowBonds).toString());
            } else if(cb.getText().equals(jrh.getString("cALabel"))) {
                ShowAtoms = cb.isSelected();
                display.getSettings().setShowAtoms(ShowAtoms);
                display.getSettings().setDrawBondsToAtomCenters(!ShowAtoms);
                props.put("ShowAtoms", new Boolean(ShowAtoms).toString());
            } else if(cb.getText().equals(jrh.getString("cVLabel"))) {
                ShowVectors = cb.isSelected();
                display.getSettings().setShowVectors(ShowVectors);
                props.put("ShowVectors", new Boolean(ShowVectors).toString());
            } else if(cb.getText().equals(jrh.getString("cHLabel"))) {
                ShowHydrogens = cb.isSelected();
                display.getSettings().setShowHydrogens(ShowHydrogens);
                props.put("ShowHydrogens", 
                          new Boolean(ShowHydrogens).toString());
            } else if(cb.getText().equals(jrh.getString("cDLabel"))) {
                ShowDummies = cb.isSelected();
                props.put("ShowDummies", new Boolean(ShowDummies).toString());
            } else if(cb.getText().equals(jrh.getString("cXLabel"))) {
                ShowAxes = cb.isSelected();
                props.put("ShowAxes", new Boolean(ShowAxes).toString());
            } else if(cb.getText().equals(jrh.getString("cBBLabel"))) {
                ShowBoundingBox = cb.isSelected();
                props.put("ShowBoundingBox", 
                          new Boolean(ShowBoundingBox).toString());
            }
            display.repaint();
        }
    };

    ItemListener radioButtonListener = new ItemListener() {
        Component c;
        AbstractButton b;
        
        public void itemStateChanged(ItemEvent e) {
            JRadioButton rb = (JRadioButton) e.getSource();
            if(rb.getText().equals(jrh.getString("ceYesLabel"))) {
                ConfirmExit = rb.isSelected();
                props.put("ConfirmExit", new Boolean(ConfirmExit).toString());
            } else if(rb.getText().equals(jrh.getString("ceNoLabel"))) {
                ConfirmExit = !rb.isSelected();
                props.put("ConfirmExit", new Boolean(ConfirmExit).toString());
            } else if(rb.getText().equals(jrh.getString("aaYesLabel"))) {
                AntiAliased = rb.isSelected();
                display.setAntiAliased(AntiAliased);
                props.put("AntiAliased", new Boolean(AntiAliased).toString());
                display.repaint();
            } else if(rb.getText().equals(jrh.getString("aaNoLabel"))) {
                AntiAliased = !rb.isSelected();
                display.setAntiAliased(AntiAliased);
                props.put("AntiAliased", new Boolean(AntiAliased).toString());
                display.repaint();
            } else if(rb.getText().equals(jrh.getString("ufeYesLabel"))) {
                UseFileExtensions = rb.isSelected();
                FileTyper.setUseFileExtensions(UseFileExtensions);
                props.put("UseFileExtensions", new Boolean(UseFileExtensions).toString());
            } else if(rb.getText().equals(jrh.getString("ufeNoLabel"))) {
                UseFileExtensions = !rb.isSelected();
                FileTyper.setUseFileExtensions(UseFileExtensions);
                props.put("UseFileExtensions", new Boolean(UseFileExtensions).toString());
            } else if(rb.getText().equals(jrh.getString("pYesLabel"))) {
                Perspective = rb.isSelected();
                displayPanel.setPerspective(Perspective);
                props.put("Perspective", new Boolean(Perspective).toString());
                display.repaint();
            } else if(rb.getText().equals(jrh.getString("pNoLabel"))) {
                Perspective = !rb.isSelected();
                displayPanel.setPerspective(Perspective);
                props.put("Perspective", new Boolean(Perspective).toString());
                display.repaint();
            }
        }
    };

}    
