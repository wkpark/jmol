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

import org.jmol.api.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.JComponent;
import javax.swing.InputVerifier;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Vector;

/**
 * A dialog for controling the creation of a povray input file from a
 * Chemframe and a display. The actual leg work of writing the file
 * out is done by PovrayWriter.java.
 * <p>Borrows code from org.openscience.jmol.Vibrate (Thanks!).
 * @author Thomas James Grey (tjg1@ch.ic.ac.uk)
 * @author Matthew A. Meineke (mmeineke@nd.edu)
 */
public class PovrayDialog extends JDialog {

  private JmolViewer viewer;
  boolean callPovray = true;
  boolean doAntiAlias = true;
  boolean displayWhileRendering = true;
  String savePath;
  String basename = "I'm a bug";
  String commandLine = "Hello, I'm a bug!!";
  String povrayPath = "I'm a bug, lets party";
  JButton povrayPathButton;
  JCheckBox antiAlias;
  JCheckBox displayWhileRenderingBox;
  JTextField commandLineField;
  JButton goButton;
  JTextField saveField;
  private JTextField savePathLabel;
  private int outputWidth = -1;
  private int outputHeight = -1;
  private JTextField povrayPathLabel;
  
  private JCheckBox           imageSizeBox;
  private JLabel              imageSizeWidth;
  private JFormattedTextField imageSizeTextWidth;
  private JLabel              imageSizeHeight;
  private JFormattedTextField imageSizeTextHeight;
  private JCheckBox	          imageSizeRatioBox;
  private JComboBox           imageSizeRatioCombo;
  
  private JCheckBox outputFormatBox;
  private JComboBox outputFormatCombo;
  
  private JCheckBox outputAlphaBox;
  
  private JCheckBox mosaicPreviewBox;
  private JLabel    mosaicPreviewStart;
  private JComboBox mosaicPreviewComboStart;
  private JLabel    mosaicPreviewEnd;
  private JComboBox mosaicPreviewComboEnd;

  /**
   * Creates a dialog for getting info related to output frames in
   *  povray format.
   * @param f The frame assosiated with the dialog
   * @param viewer The interacting display we are reproducing (source of view angle info etc)
   */
  public PovrayDialog(JFrame f, JmolViewer viewer) {

    super(f, JmolResourceHandler
        .getStringX("Povray.povrayDialogTitle"), true);
    this.viewer = viewer;
    basename = "jmol";

    //
    String text = null;
    
    //Take the height and width settings from the JFrame
    int screenWidth = viewer.getScreenWidth();
    int screenHeight = viewer.getScreenHeight();
    getPathHistory();
    setImageDimensions(screenWidth, screenHeight);

    //Box: Window
    Box windowBox = Box.createVerticalBox();
    getContentPane().add(windowBox);
    
    //Box: Main
    Box mainBox = Box.createVerticalBox();
    
    //GUI for save name selection
    Box justSavingBox = Box.createVerticalBox();
    text = JmolResourceHandler.getStringX("Povray.savingPov");
    justSavingBox.setBorder(new TitledBorder(text));
    
    Box saveBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.workingName");
    saveBox.setBorder(new TitledBorder(text));
    text = JmolResourceHandler.getStringX("Povray.workingNameTip");
    saveBox.setToolTipText(text);
    saveField = new JTextField(basename, 20);
    saveField.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        basename = saveField.getText();
        updateCommandLine();
      }
    });
    saveField.setInputVerifier(new InputVerifier() {
        public boolean verify(JComponent component) {
          JTextField fld = (JTextField)component;
          basename = fld.getText();
          updateCommandLine();
          return true;
        }
      });
    saveBox.add(saveField);
    justSavingBox.add(saveBox);

    //GUI for save path selection
    Box savePathBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.workingDirectory");
    savePathBox.setBorder(new TitledBorder(text));
    text = JmolResourceHandler.getStringX("Povray.workingDirectoryTip");
    savePathBox.setToolTipText(text);
    savePathLabel = new JTextField(savePath);
    savePathLabel.setEditable(false);
    savePathLabel.setBorder(null);
    savePathBox.add(savePathLabel);
    text = JmolResourceHandler.getStringX("Povray.selectButton");
    JButton savePathButton = new JButton(text);
    savePathButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showSavePathDialog();
      }
    });
    savePathBox.add(savePathButton);
    justSavingBox.add(savePathBox);
    mainBox.add(justSavingBox);

    //GUI for povray options
    Box povOptionsBox = Box.createVerticalBox();
    text = JmolResourceHandler.getStringX("Povray.povOptions");
    povOptionsBox.setBorder(new TitledBorder(text));
    
    // Run povray option
    Box runPovBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.runPov");
    JCheckBox runPovCheck = new JCheckBox(text, true);
    text = JmolResourceHandler.getStringX("Povray.runPovTip");
    runPovCheck.setToolTipText(text);
    runPovCheck.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        if (e.getStateChange() == ItemEvent.SELECTED) {
          callPovray = true;
          String text = JmolResourceHandler.getStringX("Povray.goLabel");
          goButton.setText(text);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          callPovray = false;
          String text = JmolResourceHandler.getStringX("Povray.saveLabel");
          goButton.setText(text);
        }
        povrayPathButton.setEnabled(callPovray);
        antiAlias.setEnabled(callPovray);
        displayWhileRenderingBox.setEnabled(callPovray);
        commandLineField.setEnabled(callPovray);
      }
    });
    runPovBox.add(runPovCheck);
    runPovBox.add(Box.createGlue());
    povOptionsBox.add(runPovBox);

    // Antialias option
    Box antiAliasBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.antiAlias");
    antiAlias = new JCheckBox(text, doAntiAlias);
    text = JmolResourceHandler.getStringX("Povray.antiAliasTip");
    antiAlias.setToolTipText(text);
    antiAlias.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        if (e.getStateChange() == ItemEvent.SELECTED) {
          doAntiAlias = true;
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          doAntiAlias = false;
        }
        updateCommandLine();
      }
    });
    antiAliasBox.add(antiAlias);
    antiAliasBox.add(Box.createGlue());
    povOptionsBox.add(antiAliasBox);

    // Display when rendering option
    Box displayBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.displayWhileRendering");
    displayWhileRenderingBox = new JCheckBox(text, displayWhileRendering);
    text = JmolResourceHandler.getStringX("Povray.displayWhileRenderingTip");
    displayWhileRenderingBox.setToolTipText(text);
    displayWhileRenderingBox.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        if (e.getStateChange() == ItemEvent.SELECTED) {
          displayWhileRendering = true;
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          displayWhileRendering = false;
        }
        updateCommandLine();
      }
    });
    displayBox.add(displayWhileRenderingBox);
    displayBox.add(Box.createGlue());
    povOptionsBox.add(displayBox);

    // Image size option
    Box imageBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.imageSize");
    imageSizeBox = new JCheckBox(text, false);
    text = JmolResourceHandler.getStringX("Povray.imageSizeTip");
    imageSizeBox.setToolTipText(text);
    imageSizeBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        imageSizeChanged();
        updateCommandLine();
      }
    });
    imageBox.add(imageSizeBox);
    imageBox.add(Box.createHorizontalStrut(10));
    Box imageSizeDetailBox = Box.createVerticalBox();
    Box imageSizeXYBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.imageSizeWidth");
    imageSizeWidth = new JLabel(text);
    text = JmolResourceHandler.getStringX("Povray.imageSizeWidthTip");
    imageSizeWidth.setToolTipText(text);
    imageSizeXYBox.add(imageSizeWidth);
    imageSizeTextWidth = new JFormattedTextField();
    imageSizeTextWidth.setValue(new Integer(outputWidth));
    imageSizeTextWidth.addPropertyChangeListener("value",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          imageSizeChanged();
          updateCommandLine();
        }
      }
    );
    imageSizeXYBox.add(imageSizeTextWidth);
    imageSizeXYBox.add(Box.createHorizontalStrut(10));
    text = JmolResourceHandler.getStringX("Povray.imageSizeHeight");
    imageSizeHeight = new JLabel(text);
    text = JmolResourceHandler.getStringX("Povray.imageSizeHeightTip");
    imageSizeHeight.setToolTipText(text);
    imageSizeXYBox.add(imageSizeHeight);
    imageSizeTextHeight = new JFormattedTextField();
    imageSizeTextHeight.setValue(new Integer(outputHeight));
    imageSizeTextHeight.addPropertyChangeListener("value",
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          imageSizeChanged();
          updateCommandLine();
        }
      }
    );
    imageSizeXYBox.add(imageSizeTextHeight);
    imageSizeXYBox.add(Box.createGlue());
    imageSizeDetailBox.add(imageSizeXYBox);
    Box imageSizeBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.imageSizeKeepRatio");
    imageSizeRatioBox = new JCheckBox(text, true);
    text = JmolResourceHandler.getStringX("Povray.imageSizeKeepRatioTip");
    imageSizeRatioBox.setToolTipText(text);
    imageSizeRatioBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        imageSizeChanged();
        updateCommandLine();
      }
    });
    imageSizeBox.add(imageSizeRatioBox);
    imageSizeBox.add(Box.createHorizontalStrut(10));
    imageSizeRatioCombo = new JComboBox();
    text = JmolResourceHandler.getStringX("Povray.imageSizeRatioFree");
    imageSizeRatioCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.imageSizeRatioJmol");
    imageSizeRatioCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.imageSizeRatio4_3");
    imageSizeRatioCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.imageSizeRatio16_9");
    imageSizeRatioCombo.addItem(text);
    imageSizeRatioCombo.setSelectedIndex(1);
    imageSizeRatioCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        imageSizeChanged();
        updateCommandLine();
      }
    });
    imageSizeBox.add(imageSizeRatioCombo);
    imageSizeBox.add(Box.createGlue());
    imageSizeDetailBox.add(imageSizeBox);
    imageSizeDetailBox.add(Box.createGlue());
    imageBox.add(imageSizeDetailBox);
    imageBox.add(Box.createGlue());
    povOptionsBox.add(imageBox);
    imageSizeChanged();
    
    // Output format option
    Box outputBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.outputFormat");
    outputFormatBox = new JCheckBox(text, false);
    text = JmolResourceHandler.getStringX("Povray.outputFormatTip");
    outputFormatBox.setToolTipText(text);
    outputFormatBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        outputFormatChanged();
        updateCommandLine();
      }
    });
    outputBox.add(outputFormatBox);
    outputFormatCombo = new JComboBox();
    text = JmolResourceHandler.getStringX("Povray.outputFormatC");
    outputFormatCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.outputFormatN");
    outputFormatCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.outputFormatP");
    outputFormatCombo.addItem(text);
    text = JmolResourceHandler.getStringX("Povray.outputFormatT");
    outputFormatCombo.addItem(text);
    outputFormatCombo.setSelectedIndex(3);
    outputFormatCombo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        outputFormatChanged();
        updateCommandLine();
      }
    });
    outputBox.add(outputFormatCombo);
    outputBox.add(Box.createGlue());
    povOptionsBox.add(outputBox);
    outputFormatChanged();

    // Alpha option
    Box alphaBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.outputAlpha");
    outputAlphaBox = new JCheckBox(text, false);
    text = JmolResourceHandler.getStringX("Povray.outputAlphaTip");
    outputAlphaBox.setToolTipText(text);
    outputAlphaBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateCommandLine();
      }
    });
    alphaBox.add(outputAlphaBox);
    alphaBox.add(Box.createGlue());
    povOptionsBox.add(alphaBox);
    
    // Mosaic preview option
    Box mosaicBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.mosaicPreview");
    mosaicPreviewBox = new JCheckBox(text, false);
    text = JmolResourceHandler.getStringX("Povray.mosaicPreviewTip");
    mosaicPreviewBox.setToolTipText(text);
    mosaicPreviewBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
      	mosaicPreviewChanged();
      	updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewBox);
    mosaicBox.add(Box.createHorizontalStrut(10));
    text = JmolResourceHandler.getStringX("Povray.mosaicPreviewStart");
    mosaicPreviewStart = new JLabel(text);
    text = JmolResourceHandler.getStringX("Povray.mosaicPreviewStartTip");
    mosaicPreviewStart.setToolTipText(text);
    mosaicBox.add(mosaicPreviewStart);
    mosaicPreviewComboStart = new JComboBox();
    for (int power = 0; power < 8; power++) {
    	mosaicPreviewComboStart.addItem(Integer.toString((int)Math.pow(2, power)));
    }
    mosaicPreviewComboStart.setSelectedIndex(3);
    mosaicPreviewComboStart.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mosaicPreviewChanged();
        updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewComboStart);
    mosaicBox.add(Box.createHorizontalStrut(10));
    text = JmolResourceHandler.getStringX("Povray.mosaicPreviewEnd");
    mosaicPreviewEnd = new JLabel(text);
    text = JmolResourceHandler.getStringX("Povray.mosaicPreviewEndTip");
    mosaicPreviewEnd.setToolTipText(text);
    mosaicBox.add(mosaicPreviewEnd);
    mosaicPreviewComboEnd = new JComboBox();
    for (int power = 0; power < 8; power++) {
    	mosaicPreviewComboEnd.addItem(Integer.toString((int)Math.pow(2, power)));
    }
    mosaicPreviewComboEnd.setSelectedIndex(0);
    mosaicPreviewComboEnd.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mosaicPreviewChanged();
        updateCommandLine();
      }
    });
    mosaicBox.add(mosaicPreviewComboEnd);
    mosaicBox.add(Box.createGlue());
    povOptionsBox.add(mosaicBox);
    mosaicPreviewChanged();
    
    //GUI for povray path selection
    Box povrayPathBox = Box.createHorizontalBox();
    text = JmolResourceHandler.getStringX("Povray.povrayExecutable");
    povrayPathBox.setBorder(new TitledBorder(text));
    text = JmolResourceHandler.getStringX("Povray.povrayExecutableTip");
    povrayPathBox.setToolTipText(text);
    povrayPathLabel = new JTextField(povrayPath);
    povrayPathLabel.setEditable(false);
    povrayPathLabel.setBorder(null);
    povrayPathBox.add(povrayPathLabel);
    text = JmolResourceHandler.getStringX("Povray.selectButton");
    povrayPathButton = new JButton(text);
    povrayPathButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showPovrayPathDialog();
      }
    });
    povrayPathBox.add(povrayPathButton);
    povOptionsBox.add(povrayPathBox);

    //GUI for command selection
    Box commandLineBox = Box.createVerticalBox();
    text = JmolResourceHandler.getStringX("Povray.commandLineTitle");
    commandLineBox.setBorder(new TitledBorder(text));
    text = JmolResourceHandler.getStringX("Povray.commandLineTip");
    commandLineBox.setToolTipText(text);
    commandLineField = new JTextField(commandLine, 30);
    text = JmolResourceHandler.getStringX("Povray.commandLineTip");
    commandLineField.setToolTipText(text);
    commandLineField.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        commandLine = commandLineField.getText();
      }
    });
    commandLineBox.add(commandLineField);
    povOptionsBox.add(commandLineBox);
    mainBox.add(povOptionsBox);

    //GUI for panel with go, cancel and stop (etc) buttons
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.add(Box.createGlue());
    text = JmolResourceHandler.getStringX("Povray.goLabel");
    goButton = new JButton(text);
    text = JmolResourceHandler.getStringX("Povray.goButtonTip");
    goButton.setToolTipText(text);
    goButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        goPressed();
      }
    });
    buttonBox.add(goButton);
    text = JmolResourceHandler.getStringX("Povray.cancelLabel");
    JButton cancelButton = new JButton(text);
    text = JmolResourceHandler.getStringX("Povray.cancelButtonTip");
    cancelButton.setToolTipText(text);
    cancelButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        cancelPressed();
      }
    });
    buttonBox.add(cancelButton);
    
    windowBox.add(mainBox);
    windowBox.add(buttonBox);

    updateCommandLine();
    pack();
    centerDialog();
    setVisible(true);
  }

  /**
   *  Sets the output image dimensions. Setting either to &lt;= 0 will
   *  remove the height and width specification from the commandline- the
   * resulting behaviour depends on povray!
   * @param imageWidth The width of the image.
   * @param imageHeight The height of the image.
   */
  public void setImageDimensions(int imageWidth, int imageHeight) {
    outputWidth = imageWidth;
    outputHeight = imageHeight;
    updateCommandLine();
  }

  /**
   * Save or else launch povray- ie do our thang!
   */
  void goPressed() {

    // File theFile = new.getSelectedFile();
    String filename = basename + ".pov";
    File theFile = new File(savePath, filename);
    if (theFile != null) {
      try {
        java.io.FileOutputStream os = new java.io.FileOutputStream(theFile);
      
        PovraySaver povs = new PovraySaver(viewer, os);
        povs.writeFile();
      } catch (FileNotFoundException fnf) {
        System.out.println("Povray Dialog FileNotFoundException:" + theFile);
        return;
      }
    }
    try {
      if (callPovray) {
      	String[] commandLineArgs = getCommandLineArgs();
        Runtime.getRuntime().exec(commandLineArgs);
      }
    } catch (java.io.IOException e) {
      System.out.println("Caught IOException in povray exec: " + e);
      System.out.println("CmdLine: " + commandLine);
    }
    setVisible(false);
    saveHistory();
    dispose();
  }

  /**
   * Responds to cancel being press- or equivalent eg window closed.
   */
  void cancelPressed() {
    setVisible(false);
    dispose();
  }

  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showSavePathDialog() {

    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int button = myChooser.showDialog(this, "Select");
    if (button == JFileChooser.APPROVE_OPTION) {
      java.io.File newFile = myChooser.getSelectedFile();
      if (newFile.isDirectory()) {
        savePath = newFile.toString();
      } else {
        savePath = newFile.getParent().toString();
      }
      savePathLabel.setText(savePath);
      updateCommandLine();
      pack();
    }
  }

  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showPovrayPathDialog() {

    JFileChooser myChooser = new JFileChooser();
    int button = myChooser.showDialog(this, "Select");
    if (button == JFileChooser.APPROVE_OPTION) {
      java.io.File newFile = myChooser.getSelectedFile();
      povrayPath = newFile.toString();
      povrayPathLabel.setText(povrayPath);
      updateCommandLine();
      pack();
    }
  }

  /**
   * Called when the ImageSize check box is modified 
   */
  void imageSizeChanged() {
  	if (imageSizeBox != null) {
  	  boolean selected = imageSizeBox.isSelected();
  	  boolean ratioSelected = false;
  	  if (imageSizeRatioBox != null) {
  	    ratioSelected = imageSizeRatioBox.isSelected();
  	    imageSizeRatioBox.setEnabled(selected);
  	  }
  	  if (imageSizeWidth != null) {
  	    imageSizeWidth.setEnabled(selected);
  	  }
  	  if (imageSizeTextWidth != null) {
  	    imageSizeTextWidth.setEnabled(selected);
  	  }
  	  if (imageSizeHeight != null) {
  	    imageSizeHeight.setEnabled(selected && !ratioSelected);
  	  }
  	  if (imageSizeTextHeight != null) {
  	    imageSizeTextHeight.setEnabled(selected && !ratioSelected);
  	  }
  	  if (imageSizeRatioCombo != null) {
  	  	imageSizeRatioCombo.setEnabled(selected && ratioSelected);
  	    if ((imageSizeTextWidth != null) && (imageSizeTextHeight != null)) {
  	      int width = Integer.parseInt(
  	        imageSizeTextWidth.getValue().toString());
  	      int height;
  	      switch (imageSizeRatioCombo.getSelectedIndex()) {
  	      case 0: // Free
  	        break;
  	      case 1: // Jmol
  	        height = (int)(((double) width) * outputHeight / outputWidth);
  	        imageSizeTextHeight.setValue(new Integer(height));
  	        break;
  	      case 2: // 4/3
  	        height = (int)(((double) width) * 3 / 4);
  	        imageSizeTextHeight.setValue(new Integer(height));
  	        break;
  	      case 3: // 16/9
  	        height = (int)(((double) width) * 9 / 16);
  	        imageSizeTextHeight.setValue(new Integer(height));
  	        break;
  	      }
  	    }
  	  }
  	}
  }
  
  /**
   * Called when the OutputFormat check box is modified 
   */
  void outputFormatChanged() {
  	if (outputFormatBox != null) {
  	  boolean selected = outputFormatBox.isSelected();
  	  if (outputFormatCombo != null) {
  	    outputFormatCombo.setEnabled(selected);
  	  }
  	}
  }
  
  /**
   * Called when the MosaicPreview check box is modified 
   */
  void mosaicPreviewChanged() {
  	if (mosaicPreviewBox != null) {
  	  boolean selected = mosaicPreviewBox.isSelected();
  	  if (mosaicPreviewStart != null) {
  	    mosaicPreviewStart.setEnabled(selected);
  	  }
  	  if (mosaicPreviewComboStart != null) {
  	    mosaicPreviewComboStart.setEnabled(selected);
  	  }
  	  if (mosaicPreviewEnd != null) {
  	    mosaicPreviewEnd.setEnabled(selected);
  	  }
  	  if (mosaicPreviewComboEnd != null) {
  	    mosaicPreviewComboEnd.setEnabled(selected);
  	  }
  	}
  }
  
  /**
   * Generates a commandline from the options set for povray path
   * etc and sets in the textField.
   */
  protected void updateCommandLine() {

    if ((savePath == null) || (povrayPath == null) || (basename == null)
        || (commandLine == null)) {
      commandLine = "null component string";
    }

    //Append a file separator to the savePath is necessary
    if (!savePath.endsWith(java.io.File.separator)) {
      savePath += java.io.File.separator;
    }

    //Get the current setup
    //        commandLine = commandLineField.getText();

    commandLine =
      doubleQuoteIfContainsSpace(povrayPath) +
      " +I" + simpleQuoteIfContainsSpace(savePath + basename + ".pov");

    // Output format options
    String outputExtension = ".tga";
    String outputFileType = " +FT";
    if ((outputFormatBox != null) && (outputFormatBox.isSelected())) {
      switch (outputFormatCombo.getSelectedIndex()) {
      case 0: // Compressed TARGA
        outputFileType = " +FC";
        break;
      case 1: // PNG
        outputExtension = ".png";
        outputFileType = " +FN";
        break;
      case 2: // PPM
        outputExtension = ".ppm";
        outputFileType = " +FP";
        break;
      default: // Uncompressed TARGA
        break;
      }
    }
    commandLine +=
      " +O" +
      simpleQuoteIfContainsSpace(savePath + basename + outputExtension) +
      outputFileType;
    
    // Output alpha options
    if ((outputAlphaBox != null) && (outputAlphaBox.isSelected())) {
      commandLine +=
        " +UA";
    }
    
    // Image size options
    if ((imageSizeBox != null) && (imageSizeBox.isSelected())) {
      commandLine +=
        " +H" + imageSizeTextHeight.getValue() +
		" +W" + imageSizeTextWidth.getValue();
    } else {
      if ((outputWidth > 0) && (outputHeight > 0)) {
        commandLine +=
	  	  " +H" + outputHeight +
		  " +W" + outputWidth;
      }
    }

    if (doAntiAlias) {
      commandLine += " +A0.1";
    }

    if (displayWhileRendering) {
      commandLine += " +D +P";
    }

    // Mosaic preview options
    if ((mosaicPreviewBox != null) && (mosaicPreviewBox.isSelected())) {
      commandLine +=
        " +SP" + mosaicPreviewComboStart.getSelectedItem() +
		" +EP" + mosaicPreviewComboEnd.getSelectedItem();
    }
    
    commandLine += " -V"; // turn off verbose messages ... although it is still rather verbose

    if (commandLineField != null) {
      commandLineField.setText(commandLine);
    }
  }

  /**
   * @return Command line split into arguments
   */
  private String[] getCommandLineArgs() {
  	
    //Parsing command line
    commandLine = commandLineField.getText();
    Vector vector = new Vector();
    int begin = 0;
    int end = 0;
    int doubleQuoteCount = 0;
    while (end < commandLine.length()) {
      if (commandLine.charAt(end) == '\"') {
        doubleQuoteCount++;
      }
      if (Character.isSpaceChar(commandLine.charAt(end))) {
        while ((begin < end) &&
               (Character.isSpaceChar(commandLine.charAt(begin)))) {
          begin++;
        }
        if (end > begin + 1) {
          if (doubleQuoteCount % 2 == 0) {
            vector.add(commandLine.substring(begin, end));
            begin = end;
          }
        }
      }
      end++;
    }
    while ((begin < end) &&
           (Character.isSpaceChar(commandLine.charAt(begin)))) {
      begin++;
    }
    if (end > begin + 1) {
      vector.add(commandLine.substring(begin, end));
    }
    
    //Construct result
    String[] args = new String[vector.size()];
    for (int pos = 0; pos < vector.size(); pos++) {
      args[pos] = vector.get(pos).toString();
      if ((args[pos].charAt(0) == '\"') &&
          (args[pos].charAt(args[pos].length() - 1) == '\"')) {
        args[pos] = args[pos].substring(1, args[pos].length() - 1);
      }
    }
    return args;
  }
  
  /**
   * Centers the dialog on the screen.
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
   * Listener for responding to dialog window events.
   */
  class PovrayWindowListener extends WindowAdapter {

    /**
     * Closes the dialog when window closing event occurs.
     * @param e Event
     */
    public void windowClosing(WindowEvent e) {
      cancelPressed();
      setVisible(false);
      dispose();
    }
  }

  /**
   * Just recovers the path settings from last session.
   */
  private void getPathHistory() {

    java.util.Properties props = Jmol.getHistoryFile().getProperties();
    povrayPath = props.getProperty("povrayPath",
        System.getProperty("user.home"));
    savePath = props.getProperty("povraySavePath",
        System.getProperty("user.home"));
  }

  /**
   * Just saves the path settings from this session.
   */
  private void saveHistory() {

    java.util.Properties props = new java.util.Properties();
    props.setProperty("povrayPath", povrayPath);
    props.setProperty("povraySavePath", savePath);
    Jmol.getHistoryFile().addProperties(props);
  }

  String doubleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\"" + str + "\"";
    return str;
  }

  String simpleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\'" + str + "\'";
    return str;
  }
}
