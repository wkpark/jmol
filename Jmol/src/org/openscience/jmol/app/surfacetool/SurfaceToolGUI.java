/* $RCSfile$
 * J. Gutow
 * $July 22, 2011$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.openscience.jmol.app.surfacetool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import org.jmol.api.JmolViewer;
import org.jmol.export.history.HistoryFile;
import org.jmol.i18n.GT;

/**
 * GUI for the Jmol surfaceTool
 * 
 * @author Jonathan Gutow (gutow@uwosh.edu)
 */
class SurfaceToolGUI extends JPanel implements 
    WindowConstants, WindowListener, WindowFocusListener, ChangeListener,
    ActionListener {

  private HistoryFile historyFile;
  private String histWinName;
  private JFrame slicerFrame;
  private SurfaceTool slicer;
  private JPanel objectsPanel;
  private JPanel unitsPanel;
  private JPanel angleUnitsPanel;
  private JComboBox angleUnitsList;
  private JPanel originPanel;
  private JRadioButton viewCenterButton;
  private JRadioButton absoluteButton;
  private JPanel ghostPanel;
  private JCheckBox ghostCheck;
  private JCheckBox boundaryPlaneCheck;
  private JPanel sliderPanel;
  private JPanel normAnglePanel;
  private JSlider angleXYSlider;
//  private JSpinner angleXYSpinner;
  private JSlider angleZSlider;
//  private JSpinner angleZSpinner;
  private JPanel positionThicknessPanel;
  private JSlider positionSlider;
//  private JSpinner positionSpinner;
  private JSlider thicknessSlider;
//  private JSpinner thicknessSpinner;
  //private JCheckBox[] objectCheckBoxes;
  private ButtonGroup whichOrigin;

  /**
   * Builds and opens a GUI to control slicing. Called automatically when a new
   * SurfaceTool is created with useGUI = true.
   * 
   * @param viewer
   *        (JmolViewer) the viewer that called for this surfaceTool.
   * @param hfile
   *        (HistoryFile) the history file used by this instance of Jmol
   * @param winName
   *        (String) name used for this window in history probably
   *        JmolPanel.SURFACETOOL_WINDOW_NAME
   * @param slicer
   *        (SurfaceTool) the surfaceTool that activated this GUI
   * @author Jonathan Gutow (gutow@uwosh.edu)
   */
  SurfaceToolGUI(JmolViewer viewer, HistoryFile hfile, String winName,
      SurfaceTool slicer) {
    super(new BorderLayout());
    this.historyFile = hfile;
    this.histWinName = winName;
    this.slicer = slicer;
    if (slicerFrame != null) {
      slicerFrame.setVisible(true);
      slicerFrame.toFront();
    } else {
      slicerFrame = new JFrame(GT._("SurfaceTool"));
      slicerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      String imageName = "org/openscience/jmol/app/images/icon.png";
      URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
      ImageIcon jmolIcon = new ImageIcon(imageUrl);
      slicerFrame.setIconImage(jmolIcon.getImage());
      slicerFrame.addWindowFocusListener(this);
      slicerFrame.addWindowListener(this);
      //Create and set up the content pane.
      setOpaque(true); //content panes must be opaque

      //Units panel
      unitsPanel = new JPanel(new GridLayout(1, 0));
      angleUnitsPanel = new JPanel(new GridLayout(0, 1));
      String [] angleUnits = slicer.getAngleUnitsList();
      angleUnitsList = new JComboBox(angleUnits);
      angleUnitsList.setSelectedIndex(slicer.getAngleUnits());
      angleUnitsList.addActionListener(this);
      angleUnitsPanel.add(angleUnitsList);
      angleUnitsPanel
          .setBorder(BorderFactory.createTitledBorder(GT._("Angle Units")));

      whichOrigin = new ButtonGroup();
      originPanel = new JPanel(new GridLayout(0, 1));
      if (slicer.getUseMolecular()) {
        viewCenterButton = new JRadioButton(GT._("View Center"), false);
        absoluteButton = new JRadioButton(GT._("Absolute"), true);
      } else {
        viewCenterButton = new JRadioButton(GT._("View Center"), true);
        absoluteButton = new JRadioButton(GT._("Absolute"), false);
      }
      viewCenterButton.addActionListener(this);
      absoluteButton.addActionListener(this);
      whichOrigin.add(viewCenterButton);
      whichOrigin.add(absoluteButton);
      originPanel.add(viewCenterButton);
      originPanel.add(absoluteButton);
      originPanel.setBorder(BorderFactory.createTitledBorder(GT._("Origin")));

      ghostPanel = new JPanel(new GridLayout(0, 1));
      ghostCheck = new JCheckBox(GT._("Ghost On"));
      ghostCheck.setSelected(slicer.getGhoston());
      ghostCheck.addActionListener(this);
      boundaryPlaneCheck = new JCheckBox(GT._("Slice Planes"));
      boundaryPlaneCheck.setSelected(false);
      slicer.showSliceBoundaryPlanes(false);
      boundaryPlaneCheck.addActionListener(this);
      ghostPanel.add(ghostCheck);
      ghostPanel.add(boundaryPlaneCheck);

      unitsPanel.add(angleUnitsPanel);
      unitsPanel.add(originPanel);
      unitsPanel.add(ghostPanel);
      unitsPanel.setSize(200, 40);

      //slider panel
      sliderPanel = new JPanel(new GridLayout(0, 1));
      normAnglePanel = new JPanel(new GridLayout(0, 1));
      JLabel sliderLabel = new JLabel(GT._("Angle from X-axis in XY plane"),
          SwingConstants.CENTER);
      sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      normAnglePanel.add(sliderLabel);
      angleXYSlider = new JSlider(0, 180, 0);
      angleXYSlider.setMajorTickSpacing(30);
      angleXYSlider.setPaintTicks(true);
      angleXYSlider.addChangeListener(this);
      normAnglePanel.add(angleXYSlider);
      JLabel sliderLabel2 = new JLabel(GT._("Angle from Z-axis"),
          SwingConstants.CENTER);
      sliderLabel2.setAlignmentX(Component.CENTER_ALIGNMENT);
      normAnglePanel.add(sliderLabel2);
      angleZSlider = new JSlider(0, 180, 0);
      angleZSlider.setMajorTickSpacing(30);
      angleZSlider.setPaintTicks(true);
      angleZSlider.addChangeListener(this);
      updateAngleSliders();
      normAnglePanel.add(angleZSlider);
      normAnglePanel.setBorder(BorderFactory.createTitledBorder(GT
          ._("Direction vector of normal to slice")));
      sliderPanel.add(normAnglePanel);
      positionThicknessPanel = new JPanel(new GridLayout(0, 1));
      JLabel sliderLabel3 = new JLabel(GT._("Distance of slice from origin"),
          SwingConstants.CENTER);
      sliderLabel3.setAlignmentX(Component.CENTER_ALIGNMENT);
      positionThicknessPanel.add(sliderLabel3);
      int tempPos = (int) (180 * (slicer.getSlicePosition() - slicer
          .getPositionMin()) / slicer.getThicknessMax());
      positionSlider = new JSlider(0, 180, tempPos);
      positionSlider.setMajorTickSpacing(30);
      positionSlider.setPaintTicks(true);
      positionSlider.addChangeListener(this);
      updatePositionSlider();
      positionThicknessPanel.add(positionSlider);
      JLabel sliderLabel4 = new JLabel(GT._("Thickness of slice"),
          SwingConstants.CENTER);
      sliderLabel4.setAlignmentX(Component.CENTER_ALIGNMENT);
      positionThicknessPanel.add(sliderLabel4);
      thicknessSlider = new JSlider(0, 180,
          (int) (180 * slicer.getSliceThickness() / slicer.getThicknessMax()));
      thicknessSlider.setMajorTickSpacing(30);
      thicknessSlider.setPaintTicks(true);
      thicknessSlider.addChangeListener(this);
      updateThicknessSlider();
      positionThicknessPanel.add(thicknessSlider);
      sliderPanel.add(positionThicknessPanel);

      //objects panel
      objectsPanel = new JPanel();

      //add everything
      add(unitsPanel, BorderLayout.NORTH);
      add(sliderPanel, BorderLayout.SOUTH);
      add(objectsPanel, BorderLayout.EAST);

      slicerFrame.setContentPane(this);
      slicerFrame.addWindowListener(this);
      historyFile.repositionWindow(winName, slicerFrame, 200, 300);

      //Display the window.
      slicerFrame.pack();
      slicerFrame.setVisible(true);

      //save the window properties
      saveHistory();

    }
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == angleUnitsList) {
      slicer.setAngleUnits(angleUnitsList.getSelectedIndex());
      updateAngleSliders();
    }  
    if (e.getSource() == viewCenterButton || e.getSource() == absoluteButton) {
      if (absoluteButton.isSelected() && !slicer.getUseMolecular()) {
        slicer.setUseMolecular(true);
        slicer.setSurfaceToolParam();
        updatePositionSlider();
      }
      if (viewCenterButton.isSelected() && slicer.getUseMolecular()) {
        slicer.setUseMolecular(false);
        slicer.setSurfaceToolParam();
        updatePositionSlider();
      }
    }
    if (e.getSource() == ghostCheck) {
      slicer.setGhostOn(ghostCheck.isSelected());
      slicer.updateSlices();
    }
    if (e.getSource() == boundaryPlaneCheck) {
      slicer.showSliceBoundaryPlanes(boundaryPlaneCheck.isSelected());
    }
  }

  public void stateChanged(ChangeEvent e) {
    JSlider source = (JSlider) e.getSource();
    if (source == angleXYSlider || source == angleZSlider) {
      float tempAngleZ = (float) (Math.PI * angleZSlider.getValue() / 180);
      float tempAngleXY = (float) (Math.PI * angleXYSlider.getValue() / 180);
      slicer.setSliceAnglefromZ(tempAngleZ);
      slicer.setSliceAngleXY(tempAngleXY);
      if (!source.getValueIsAdjusting()) {
        slicer.updateSlices();
      }
    }
    if (source == positionSlider || source == thicknessSlider) {
      float tempThickness = thicknessSlider.getValue()
          * slicer.getThicknessMax() / 180;
      float tempPos = positionSlider.getValue() * slicer.getThicknessMax()
          / 180 + slicer.getPositionMin();
      slicer.setSliceThickness(tempThickness);
      slicer.setSlicePosition(tempPos);
      if (!source.getValueIsAdjusting()) {
        slicer.updateSlices();
      }
    }
  }

  private void updatePositionSlider() {
    Hashtable<Integer, JLabel> positionLabels = new Hashtable<Integer, JLabel>();
    String temp = "";
    for (int i = 0; i < 7; i++) {
      float tempVal = (float) (slicer.getPositionMin() + i * 0.16666666666
          * slicer.getThicknessMax());
      if (Math.abs(tempVal) < 0.001)
        tempVal = 0;
      temp = "" + tempVal;
      if (temp.length() > 5) {
        if (tempVal < 0) {
          temp = temp.substring(0, 5);
        } else {
          temp = temp.substring(0, 4);
        }
      }
      positionLabels.put(Integer.valueOf(i * 30), new JLabel(temp));
    }
    positionSlider.setLabelTable(positionLabels);
    positionSlider.setPaintLabels(true);
    int tempPos = (int) (180 * (slicer.getSlicePosition() - slicer
        .getPositionMin()) / slicer.getThicknessMax());
    positionSlider.setValue(tempPos);
  }

  private void updateThicknessSlider() {
    Hashtable<Integer, JLabel> thicknessLabels = new Hashtable<Integer, JLabel>();
    String temp = "";
    for (int i = 0; i < 7; i++) {
      float tempVal = (float) (i * 0.16666666666 * slicer.getThicknessMax());
      temp = "" + tempVal;
      if (temp.length() > 5) {
        temp = temp.substring(0, 4);
      }
      thicknessLabels.put(Integer.valueOf(i * 30), new JLabel(temp));
    }
    thicknessSlider.setLabelTable(thicknessLabels);
    thicknessSlider.setPaintLabels(true);
    int tempPos = (int) (180 * slicer.getSliceThickness() / slicer
        .getThicknessMax());
    thicknessSlider.setValue(tempPos);
  }

  private void updateAngleSliders() {
    Hashtable<Integer, JLabel> angleLabels = new Hashtable<Integer, JLabel>();
    angleLabels.put(new Integer(0), new JLabel("0"));
    switch (slicer.getAngleUnits()) {
    case SurfaceTool.DEGREES:
      angleLabels.put(Integer.valueOf(30), new JLabel("30"));
      angleLabels.put(Integer.valueOf(60), new JLabel("60"));
      angleLabels.put(Integer.valueOf(90), new JLabel("90"));
      angleLabels.put(Integer.valueOf(120), new JLabel("120"));
      angleLabels.put(Integer.valueOf(150), new JLabel("150"));
      angleLabels.put(Integer.valueOf(180), new JLabel("180"));
      break;
    case SurfaceTool.RADIANS:
      angleLabels.put(Integer.valueOf(30), new JLabel("0.52"));
      angleLabels.put(Integer.valueOf(60), new JLabel("1.05"));
      angleLabels.put(Integer.valueOf(90), new JLabel("1.75"));
      angleLabels.put(Integer.valueOf(120), new JLabel("2.09"));
      angleLabels.put(Integer.valueOf(150), new JLabel("2.62"));
      angleLabels.put(Integer.valueOf(180), new JLabel("3.14"));
      break;
    case SurfaceTool.GRADIANS:
      angleLabels.put(Integer.valueOf(30), new JLabel("33.3"));
      angleLabels.put(Integer.valueOf(60), new JLabel("66.7"));
      angleLabels.put(Integer.valueOf(90), new JLabel("100"));
      angleLabels.put(Integer.valueOf(120), new JLabel("133"));
      angleLabels.put(Integer.valueOf(150), new JLabel("167"));
      angleLabels.put(Integer.valueOf(180), new JLabel("200"));
      break;
    case SurfaceTool.CIRCLE_FRACTION:
      angleLabels.put(Integer.valueOf(30), new JLabel("1/12"));
      angleLabels.put(Integer.valueOf(60), new JLabel("1/6"));
      angleLabels.put(Integer.valueOf(90), new JLabel("1/4"));
      angleLabels.put(Integer.valueOf(120), new JLabel("1/3"));
      angleLabels.put(Integer.valueOf(150), new JLabel("5/12"));
      angleLabels.put(Integer.valueOf(180), new JLabel("1/2"));
      break;
    case SurfaceTool.UNITS_PI:
      String piStr = "\u03C0";
      angleLabels.put(Integer.valueOf(30), new JLabel(piStr+"/6"));
      angleLabels.put(Integer.valueOf(60), new JLabel(piStr+"/3"));
      angleLabels.put(Integer.valueOf(90), new JLabel(piStr+"/2"));
      angleLabels.put(Integer.valueOf(120), new JLabel("2"+piStr+"/3"));
      angleLabels.put(Integer.valueOf(150), new JLabel("5"+piStr+"/6"));
      angleLabels.put(Integer.valueOf(180), new JLabel(piStr));
      break;      
    }
    angleXYSlider.setLabelTable(angleLabels);
    angleXYSlider.setPaintLabels(true);
    angleZSlider.setLabelTable(angleLabels);
    angleZSlider.setPaintLabels(true);
    int tempAngle = (int) (180 * slicer.getSliceAngleXY() / Math.PI);
    angleXYSlider.setValue(tempAngle);
    tempAngle = (int) (180 * slicer.getAnglefromZ() / Math.PI);
    angleZSlider.setValue(tempAngle);
  }

  void saveHistory() {
    if (historyFile == null)
      return;
    historyFile.addWindowInfo(histWinName, slicerFrame, null);
    //TODO
    //    prop.setProperty("webMakerInfoWidth", "" + webPanels[0].getInfoWidth());
    //    prop.setProperty("webMakerInfoHeight", "" + webPanels[0].getInfoHeight());
    //    prop.setProperty("webMakerAppletPath", remoteAppletPath);
    //    prop.setProperty("webMakerLocalAppletPath", localAppletPath);
    //    prop.setProperty("webMakerPageAuthorName", pageAuthorName);
    //    historyFile.addProperties(prop);
  }

  /**
   * @param layout
   */
  SurfaceToolGUI(LayoutManager layout) {
    super(layout);
    // TODO
  }

  /**
   * @param isDoubleBuffered
   */
  SurfaceToolGUI(boolean isDoubleBuffered) {
    super(isDoubleBuffered);
    // TODO
  }

  /**
   * @param layout
   * @param isDoubleBuffered
   */
  SurfaceToolGUI(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
    // TODO
  }

  /**
   * @return (JFrame) The frame for the slicerGUI
   */
  JFrame getFrame() {
    return slicerFrame;
  }

  /**
   * Brings the surfaceTool to the front and updates sliders, etc...
   */
  void toFront() {
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
    slicerFrame.setVisible(true);
    slicerFrame.toFront();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowFocusListener#windowGainedFocus(java.awt.event.WindowEvent)
   */
  public void windowGainedFocus(WindowEvent e) {
    // TODO This is where the surface list should be updated...
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowFocusListener#windowLostFocus(java.awt.event.WindowEvent)
   */
  public void windowLostFocus(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
   */
  public void windowOpened(WindowEvent e) {
    // TODO
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
   */
  public void windowClosing(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
   */
  public void windowClosed(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
   */
  public void windowIconified(WindowEvent e) {
    // TODO

  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
   */
  public void windowDeiconified(WindowEvent e) {
    // TODO
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
   */
  public void windowActivated(WindowEvent e) {
    // TODO
    slicer.toFrontOrGotFocus();
    updateAngleSliders();
    updatePositionSlider();
    updateThicknessSlider();
  }

  /* (non-Javadoc)
   * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
   */
  public void windowDeactivated(WindowEvent e) {
    // TODO

  }

}
