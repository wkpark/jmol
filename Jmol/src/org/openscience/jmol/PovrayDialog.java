
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

//import java.awt.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 * A dialog for controling the creation of a povray input file from a Chemframe and a display. The actual leg work of writing the
 * file out is done by PovrayWriter.java.
 * <p>Borrows code from org.openscience.jmol.Vibrate (Thanks!).
 * @author Thomas James Grey (tjg1@ch.ic.ac.uk)
 */

public class PovrayDialog extends JDialog {

	private static JmolResourceHandler jrh =
		new JmolResourceHandler("Povray");
	private DisplayPanel display;
	private ChemFile currentFile;
	private boolean callPovray = true;

	/**
	 * Creates a dialog.
	 *
	 * @param f the parent frame
	 * @param dp the DisplayPanel in which the vibration will be displayed
	 */

	String savePath;
	String basename = "I'm a bug";
	String cmdLine = "Hello, I'm a bug!!";
	String povrayPath = "I'm a bug, lets party";	// "c:\\program files\\pov-ray for windows\\bin\\pvengine";
	private JButton povrayPathButton;
	private JTextField cmdLineField;
	private JButton goButton;
	private JTextField saveField;
	private JLabel savePathLabel;
	private int outputWidth = -1;
	private int outputHeight = -1;
	private JLabel povrayPathLabel;

	/** Creates a dialog for getting info related to output frames in povray format.
		@param f The frame assosiated with the dialog
		@param dp The interacting display we are reproducing (source of view angle info etc)
		@param bn The default name to base frame names on
	**/
	public PovrayDialog(JFrame f, DisplayPanel dp, ChemFile cf, String bn) {

		super(f, jrh.getString("povrayDialogTitle"), true);
		display = dp;
		currentFile = cf;
		basename = bn;

		//Take the height and width settings from the JFrame
		Dimension d = dp.getSize();
		int w = d.width;
		int h = d.height;
		getPathHistory();
		setImageDimensions(w, h);
		getContentPane().setLayout(new BorderLayout());
		JPanel mainPanel = new JPanel(new GridLayout(2, 1));

		//GUI for save name selection
		JPanel justSavingPanel = new JPanel(new GridLayout(2, 1));
		justSavingPanel
				.setBorder(new TitledBorder(jrh.getString("savingPov")));
		JPanel savePanel = new JPanel(new FlowLayout());
		savePanel.setBorder(new TitledBorder(jrh.getString("workingName")));
		savePanel.setToolTipText(jrh.getString("workingNameTip"));
		saveField = new JTextField(basename, 20);
		saveField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				basename = saveField.getText();
				updateCommandLine();
			}
		});
		savePanel.add(saveField);
		justSavingPanel.add(savePanel);

		//GUI for save path selection
		JPanel savePathPanel = new JPanel(new FlowLayout());
		savePathPanel
				.setBorder(new TitledBorder(jrh
					.getString("workingDirectory")));
		savePathPanel.setToolTipText(jrh.getString("workingDirectoryTip"));
		savePathLabel = new JLabel(savePath);
		savePathPanel.add(savePathLabel);
		JButton savePathButton = new JButton(jrh.getString("selectButton"));
		savePathButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showSavePathDialog();
			}
		});
		savePathPanel.add(savePathButton);
		justSavingPanel.add(savePathPanel);
		mainPanel.add(justSavingPanel);

		//GUI for povray path selection
		JPanel runningPovPanel = new JPanel(new GridLayout(3, 1));
		runningPovPanel
				.setBorder(new TitledBorder(jrh.getString("runningPov")));
		JCheckBox runPovCheck = new JCheckBox(jrh.getString("runPov"), true);
		runPovCheck.setToolTipText(jrh.getString("runPovTip"));
		runPovCheck.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				runPovCheckToggled(e);
			}
		});
		runningPovPanel.add(runPovCheck);
		JPanel povrayPathPanel = new JPanel(new FlowLayout());
		povrayPathPanel
				.setBorder(new TitledBorder(jrh
					.getString("povrayExecutable")));
		povrayPathPanel.setToolTipText(jrh.getString("povrayExecutableTip"));
		povrayPathLabel = new JLabel(povrayPath);
		povrayPathPanel.add(povrayPathLabel);
		povrayPathButton = new JButton(jrh.getString("selectButton"));
		povrayPathButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showPovrayPathDialog();
			}
		});
		povrayPathPanel.add(povrayPathButton);
		runningPovPanel.add(povrayPathPanel);

		//GUI for command selection
		JPanel cmdLinePanel = new JPanel();
		cmdLinePanel
				.setBorder(new TitledBorder(jrh.getString("cmdLineTitle")));
		cmdLinePanel.setToolTipText(jrh.getString("cmdLineTip"));
		cmdLineField = new JTextField(cmdLine, 30);
		cmdLineField.setToolTipText(jrh.getString("cmdLineTip"));
		cmdLineField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				cmdLine = cmdLineField.getText();
			}
		});
		cmdLinePanel.add(cmdLineField);
		runningPovPanel.add(cmdLinePanel);
		mainPanel.add(runningPovPanel);

		//GUI for panel with go, cancel and stop (etc) buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		goButton = new JButton(jrh.getString("goLabel"));
		goButton.setToolTipText(jrh.getString("goButtonTip"));
		goButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				goPressed();
			}
		});
		buttonPanel.add(goButton);
		JButton cancelButton = new JButton(jrh.getString("cancelLabel"));
		cancelButton.setToolTipText(jrh.getString("cancelButtonTip"));
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
	 *  Sets the output image dimensions. Setting either to &lt;= 0 will remove
	 *  the height and width specification from the commandline- the
	 *  resulting behaviour depends on povray!
	 *  @param imageWidth The width of the image.
	 *  @param imageHeight The height of the image.
	 */
	public void setImageDimensions(int imageWidth, int imageHeight) {
		outputWidth = imageWidth;
		outputHeight = imageHeight;
		updateCommandLine();
	}

	//Save or else launch povray- ie do our thang!    
	private void goPressed() {

		//                File theFile = new.getSelectedFile();              
		cmdLine = cmdLineField.getText();
		String filename = basename + ".pov";
		java.io.File theFile = new java.io.File(savePath, filename);
		PovrayStyleWriter style = new PovrayStyleWriter();
		if (theFile != null) {
			try {
				java.io.FileOutputStream os =
					new java.io.FileOutputStream(theFile);

				PovraySaver povs = new PovraySaver(currentFile, os);
				povs.setStyleController(style);
				povs.setTransformMatrix(display.getViewTransformMatrix());
				povs.setBackgroundColor(display.getBackgroundColor());
				povs.writeFile();
				os.flush();
				os.close();

			} catch (Exception exc) {
				System.out.println("Exception:");
				System.out.println(exc.toString());
			}
		}
		try {
			if (callPovray) {
				Runtime.getRuntime().exec(cmdLine);
			}
		} catch (java.io.IOException e) {
			System.out.println("Caught IOException in povray exec: " + e);
			System.out.println("CmdLine: " + cmdLine);
		}
		setVisible(false);
		saveHistory();
		dispose();
	}

	//Responds to cancel being press- or equivalent eg window closed    

	private void cancelPressed() {
		setVisible(false);
		dispose();
	}

	//Show a file selector when the savePath button is pressed
	private void showSavePathDialog() {

		JFileChooser myChooser = new JFileChooser();
		myChooser.setFileSelectionMode(myChooser.DIRECTORIES_ONLY);
		int button = myChooser.showDialog(this, "Select");
		if (button == myChooser.APPROVE_OPTION) {
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

	//Show a file selector when the savePath button is pressed
	private void showPovrayPathDialog() {

		JFileChooser myChooser = new JFileChooser();
		int button = myChooser.showDialog(this, "Select");
		if (button == myChooser.APPROVE_OPTION) {
			java.io.File newFile = myChooser.getSelectedFile();
			povrayPath = newFile.toString();
			povrayPathLabel.setText(povrayPath);
			updateCommandLine();
			pack();
		}
	}

	private void runPovCheckToggled(ItemEvent e) {

		if (e.getStateChange() == ItemEvent.SELECTED) {
			callPovray = true;
			goButton.setText(jrh.getString("goLabel"));
		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
			callPovray = false;
			goButton.setText(jrh.getString("saveLabel"));
		}
		povrayPathButton.setEnabled(callPovray);
		cmdLineField.setEnabled(callPovray);
	}

	/** Generates a commandline from the options set for povray path etc and sets in the textField.**/
	protected void updateCommandLine() {

		if ((savePath == null) || (povrayPath == null) || (basename == null)
				|| (cmdLine == null)) {
			cmdLine = "null componant string";
		}

		//Append a file separator to the savePath is necessary
		if (!savePath.endsWith(java.io.File.separator)) {
			savePath = savePath + java.io.File.separator;
		}

		//Get the current setup
		//        cmdLine = cmdLineField.getText();
		if ((outputWidth > 0) && (outputHeight > 0)) {
			cmdLine = (povrayPath + " +W" + outputWidth + " +H"
					+ outputHeight + " +I" + savePath + basename + ".pov");
		} else {
			cmdLine = (povrayPath + " +I" + savePath + basename + ".pov");
		}
		if (cmdLineField != null) {
			cmdLineField.setText(cmdLine);
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
		 */
		public void windowClosing(WindowEvent e) {
			cancelPressed();
			setVisible(false);
			dispose();
		}
	}

	// Just recovers the path settings from last session
	private void getPathHistory() {

		java.util.Properties props = new java.util.Properties();
		try {
			java.io.FileInputStream in =
				new java.io.FileInputStream(Jmol.HistoryPropsFile);
			props.load(in);
		} catch (java.io.IOException e) {
			System.err.println("PovrayDialog: Error reading histroy");
		}
		povrayPath = props.getProperty("povrayPath",
				System.getProperty("user.home"));
		savePath = props.getProperty("povraySavePath",
				System.getProperty("user.home"));
	}

	// Just saves the path settings from this session
	private void saveHistory() {

		try {
			java.io.FileOutputStream out =
				new java.io.FileOutputStream(Jmol.HistoryPropsFile);
			java.util.Properties props = new java.util.Properties();
			props.setProperty("povrayPath", povrayPath);
			props.setProperty("povraySavePath", savePath);
			props.store(out, Jmol.HistroyFileHeader);
		} catch (java.io.IOException e) {
			System.err.println("PovrayDialog: Error saving histroy");
		}
	}

}
