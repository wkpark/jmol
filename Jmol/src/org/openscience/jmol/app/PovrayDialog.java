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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import java.io.FileNotFoundException;
import java.io.File;

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
  private JLabel savePathLabel;
  private int outputWidth = -1;
  private int outputHeight = -1;
  private JLabel povrayPathLabel;

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

    //Take the height and width settings from the JFrame
    int screenWidth = viewer.getScreenWidth();
    int screenHeight = viewer.getScreenHeight();
    getPathHistory();
    setImageDimensions(screenWidth, screenHeight);
    getContentPane().setLayout(new BorderLayout());
    GridBagLayout gridBagLayout = new GridBagLayout();
    GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    JPanel mainPanel = new JPanel(gridBagLayout);

    //GUI for save name selection
    JPanel justSavingPanel = new JPanel(new GridLayout(2, 1));
    justSavingPanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.savingPov")));
    JPanel savePanel = new JPanel(new BorderLayout());
    savePanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.workingName")));
    savePanel
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.workingNameTip"));
    saveField = new JTextField(basename, 20);
    saveField.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        basename = saveField.getText();
        updateCommandLine();
      }
    });
    savePanel.add(saveField, BorderLayout.CENTER);
    justSavingPanel.add(savePanel);

    //GUI for save path selection
    JPanel savePathPanel = new JPanel(new BorderLayout());
    savePathPanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.workingDirectory")));
    savePathPanel
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.workingDirectoryTip"));
    savePathLabel = new JLabel(savePath);
    savePathPanel.add(savePathLabel, BorderLayout.CENTER);
    JButton savePathButton =
      new JButton(JmolResourceHandler
        .getStringX("Povray.selectButton"));
    savePathButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showSavePathDialog();
      }
    });
    savePathPanel.add(savePathButton, BorderLayout.EAST);
    justSavingPanel.add(savePathPanel);
    mainPanel.add(justSavingPanel);
    gridBagLayout.setConstraints(justSavingPanel, gridBagConstraints);

    //GUI for povray path selection
    GridBagLayout gridBagLayout2 = new GridBagLayout();

    JPanel povOptionsPanel = new JPanel(gridBagLayout2);
    povOptionsPanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.povOptions")));
    JCheckBox runPovCheck =
      new JCheckBox(JmolResourceHandler
        .getStringX("Povray.runPov"), true);
    runPovCheck
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.runPovTip"));
    runPovCheck.addItemListener(new ItemListener() {

      public void itemStateChanged(ItemEvent e) {

        if (e.getStateChange() == ItemEvent.SELECTED) {
          callPovray = true;
          goButton
              .setText(JmolResourceHandler
                .getStringX("Povray.goLabel"));
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          callPovray = false;
          goButton
              .setText(JmolResourceHandler
                .getStringX("Povray.saveLabel"));
        }
        povrayPathButton.setEnabled(callPovray);
        antiAlias.setEnabled(callPovray);
        displayWhileRenderingBox.setEnabled(callPovray);
        commandLineField.setEnabled(callPovray);
      }
    });
    povOptionsPanel.add(runPovCheck);
    gridBagLayout2.setConstraints(runPovCheck, gridBagConstraints);

    antiAlias =
        new JCheckBox(JmolResourceHandler
          .getStringX("Povray.antiAlias"), doAntiAlias);
    antiAlias
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.antiAliasTip"));
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
    povOptionsPanel.add(antiAlias);
    gridBagLayout2.setConstraints(antiAlias, gridBagConstraints);

    displayWhileRenderingBox =
        new JCheckBox(JmolResourceHandler
          .getStringX("Povray.displayWhileRendering"), displayWhileRendering);
    displayWhileRenderingBox
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.displayWhileRenderingTip"));
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
    povOptionsPanel.add(displayWhileRenderingBox);
    gridBagLayout2.setConstraints(displayWhileRenderingBox,
        gridBagConstraints);

    JPanel povrayPathPanel = new JPanel(new FlowLayout());
    povrayPathPanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.povrayExecutable")));
    povrayPathPanel
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.povrayExecutableTip"));
    povrayPathLabel = new JLabel(povrayPath);
    povrayPathPanel.add(povrayPathLabel);
    povrayPathButton =
        new JButton(JmolResourceHandler
          .getStringX("Povray.selectButton"));
    povrayPathButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showPovrayPathDialog();
      }
    });
    povrayPathPanel.add(povrayPathButton);
    povOptionsPanel.add(povrayPathPanel);
    gridBagLayout2.setConstraints(povrayPathPanel, gridBagConstraints);

    //GUI for command selection
    JPanel commandLinePanel = new JPanel();
    commandLinePanel
        .setBorder(new TitledBorder(JmolResourceHandler
          .getStringX("Povray.commandLineTitle")));
    commandLinePanel
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.commandLineTip"));
    commandLineField = new JTextField(commandLine, 30);
    commandLineField
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.commandLineTip"));
    commandLineField.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        commandLine = commandLineField.getText();
      }
    });
    commandLinePanel.add(commandLineField);
    povOptionsPanel.add(commandLinePanel);
    gridBagLayout2.setConstraints(commandLinePanel, gridBagConstraints);
    mainPanel.add(povOptionsPanel);
    gridBagLayout.setConstraints(povOptionsPanel, gridBagConstraints);

    //GUI for panel with go, cancel and stop (etc) buttons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    goButton =
        new JButton(JmolResourceHandler
          .getStringX("Povray.goLabel"));
    goButton
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.goButtonTip"));
    goButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        goPressed();
      }
    });
    buttonPanel.add(goButton);
    JButton cancelButton =
      new JButton(JmolResourceHandler
        .getStringX("Povray.cancelLabel"));
    cancelButton
        .setToolTipText(JmolResourceHandler
          .getStringX("Povray.cancelButtonTip"));
    cancelButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        cancelPressed();
      }
    });
    buttonPanel.add(cancelButton);
    getContentPane().add("Center", mainPanel);
    getContentPane().add("South", buttonPanel);

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
    commandLine = commandLineField.getText();
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
        Runtime.getRuntime().exec(commandLine);
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

    commandLine = (povrayPath + " +I" + savePath + basename + ".pov" + " +O"
        + savePath + basename + ".tga" + " +FT");

    if ((outputWidth > 0) && (outputHeight > 0)) {
      commandLine = commandLine + " +H" + outputHeight + " +W" + outputWidth;
    }

    if (doAntiAlias) {
      commandLine += " +A0.1";
    }

    if (displayWhileRendering) {
      commandLine += " +D +P";
    }

    commandLine += " -V"; // turn off verbose messages ... although it is still rather verbose

    if (commandLineField != null) {
      commandLineField.setText(commandLine);
    }
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

}
