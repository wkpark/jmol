/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jspecview.common.Annotation.AType;
import jspecview.common.AnnotationData;
import jspecview.util.JSVTextFormat;

/**
 * Dialog for managing peak, integral, and measurement listings for a Spectrum
 * within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
abstract class AwtAnnotationDialog extends AwtDialog implements AnnotationDialog,
		ListSelectionListener, WindowListener {

	private static final long serialVersionUID = 1L;

	abstract protected void addControls();

	abstract protected void createData();

	abstract protected void updateValues();

	abstract protected void tableCellSelectedEvent(int iRow, int iCol);

	protected AType thisType;
	protected String subType;

	protected ScriptInterface si;
	protected JSVPanel jsvp;
	protected JDXSpectrum spec;

	protected String thisKey;

	private JPanel leftPanel, rightPanel;
	protected JButton showHideButton;

	private JButton clearButton, applyButton;
	protected final static Map<String, Object> options = new HashMap<String, Object>();

	private Object[] myOptions;
	private String[] unitOptions;
	private String[] formatOptions;

	private Integer unitPtr;
	protected JTextField txtFormat;
	protected JTextField txtFontSize;
	protected JComboBox<String> cmbUnits;

	/**
	 * Initialises the <code>IntegralDialog</code> with the given values for minY,
	 * offset and factor
	 * @param si 
	 * @param spec 
	 * @param jsvp
	 *          the parent panel
	 */
	protected AwtAnnotationDialog(ScriptInterface si,
			JDXSpectrum spec, JSVPanel jsvp) {
		this.si = si;
		this.jsvp = jsvp;
		this.spec = spec;
		setModal(false);
		setPosition((Component) jsvp, getPosXY());
		setResizable(true);
		// after specific constructor, run setup()
	}

	ActionListener eventListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			doEvent(e);
		}
	};

	protected DialogHelper dialogHelper;
	protected JTable dataTable;
	protected String[][] tableData;
	protected boolean addUnits;
	private JSplitPane mainSplitPane;

	protected void setup() {
		getContentPane().removeAll();
		subType = spec.getTypeLabel();
		thisKey = thisType + "_" + subType;
		myOptions = (Object[]) options.get(thisKey);
		if (myOptions == null)
			options.put(thisKey, myOptions = spec.getDefaultAnnotationInfo(thisType));
		unitOptions = (String[]) myOptions[0];
		formatOptions = (String[]) myOptions[1];
		unitPtr = (Integer) options.get(thisKey + "_unitPtr");
		if (unitPtr == null)
			unitPtr = (Integer) myOptions[2];

		try {
			jbInit();
			pack();
			setVisible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception {

		showHideButton = newJButton();
		showHideButton.setText("Show");
		showHideButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton b = (JButton) e.getSource();
				showHide(b.getText().equals("Show"));
			}
		});

		clearButton = newJButton();
		clearButton.setText("Clear");
		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});

		applyButton = newJButton();
		applyButton.setText("Apply");
		applyButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyButtonPressed();
			}
		});

		leftPanel = new JPanel(new GridBagLayout());
		dialogHelper = new DialogHelper(thisKey, options, leftPanel, eventListener);
		addControls();
		addTopControls();
		leftPanel.setMinimumSize(new Dimension(200, 300));
		dialogHelper.addButton(applyButton);
		dialogHelper.addButton(showHideButton);
		if (!(this instanceof AwtPeakListDialog))
	  	dialogHelper.addButton(clearButton);
		dialogHelper = null;

		rightPanel = new JPanel();
		JScrollPane scrollPane = new JScrollPane(rightPanel);

		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.setOneTouchExpandable(true);
		mainSplitPane.setResizeWeight(0);
		mainSplitPane.setRightComponent(scrollPane);
		mainSplitPane.setLeftComponent(leftPanel);

		setPreferredSize(new Dimension(600, 370)); // golden ratio
		getContentPane().removeAll();
		getContentPane().add(mainSplitPane);

		checkEnables();
	}

	protected void applyButtonPressed() {
		apply();
	}

	protected void checkEnables() {
		boolean isShow = si.getSelectedPanel().getPanelData().getShowAnnotation(
				thisType);
		showHideButton.setText(isShow ? "Hide" : "Show");
	}

	protected void loadData(String[][] data, String[] header, int[] widths) {
		try {
			tableData = data;
			rightPanel.removeAll();
			JScrollPane scrollPane = new JScrollPane(dataTable = (new DialogHelper(si))
					.getDataTable(this, data, header, widths, leftPanel.getHeight() - 50));
			mainSplitPane.setRightComponent(scrollPane);
			// .add(scrollPane);
		} catch (Exception e) {
			// not perfect.
		}
		validate();
		repaint();
	}

	protected JButton newJButton() {
		JButton b = new JButton();
		b.setPreferredSize(new Dimension(120, 25));
		return b;
	}

	private void addTopControls() {

		String key = thisKey + "_format";
		String format = (String) options.get(key);
		if (format == null)
			options.put(key, (format = formatOptions[unitPtr == null ? 0 : unitPtr
					.intValue()]));
		txtFormat = dialogHelper.addInputOption("numberFormat", "Number Format",
				format, null, null, false);
		if (unitPtr != null)
			cmbUnits = dialogHelper.addSelectOption("Units", null, unitOptions,
					unitPtr.intValue(), addUnits);

		// txtFontSize = ((DialogHelper dialogHelper)).addInputOption("FontSize",
		// "Font Size", null, null, "10");
	}

	protected void showHide(boolean isShow) {
		setState(isShow);
		if (isShow)
			applyButtonPressed();
		jsvp.doRepaint();

		// JSViewer.runScriptNow(si, "show" + thisType + (isShow ? " true" :
		// " false"));
		checkEnables();
	}

	protected void clear() {
		if (xyData != null) {
			xyData.clear();
			apply();
		}
	}

	protected void done() {
		jsvp.getPanelData().removeDialog(this);
		// setState(false);
		if (xyData != null)
			xyData.setState(isON);
		dispose();
		jsvp.doRepaint();
	}

	protected void doEvent(ActionEvent e) {
		if (e.getActionCommand().equals("Units")) {
			txtFormat.setText(formatOptions[cmbUnits.getSelectedIndex()]);
			return;
		}
		if (e.getSource() instanceof JTextField) {
			applyButtonPressed();
			return;
		}

	}

	public void reEnable() {
		setVisible(true);
		setState(true);
		apply();
	}

	public void apply() {
		updateValues();
		checkEnables();
		jsvp.doRepaint();
	}

	private boolean isON = true;

	public boolean getState() {
		return isON;
	}

	public void setState(boolean b) {
		isON = b;
	}

	protected Parameters myParams = new Parameters("MeasurementData");

	public Parameters getParameters() {
		return myParams;
	}

	public void setFields() {
	}

	public AType getAType() {
		return thisType;
	}

	public JDXSpectrum getSpectrum() {
		return spec;
	}

	protected MeasurementData xyData;
	protected DecimalFormat numberFormatter;
	private String key;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public MeasurementData getData() {
		if (xyData == null)
			createData();
		return xyData;
	}

	public void setData(AnnotationData data) {
		myParams = data.getParameters();
		xyData = (MeasurementData) data;
	}

	public void addSpecShift(double dx) {
		if (xyData != null)
			xyData.addSpecShift(dx);
	}

	protected void setParams() {
		myParams.numberFormat = txtFormat.getText();
		numberFormatter = JSVTextFormat.getDecimalFormat("#" + myParams.numberFormat);
	}

	private int iRowSelected = -1;
	private int iColSelected = -1;
	ListSelectionModel columnSelector;
	protected int iRowColSelected = -1;

	private int lastChanged = 0;
	
	synchronized public void valueChanged(ListSelectionEvent e) {

		try {
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();
			if (e.getValueIsAdjusting()) {
				if (lsm == columnSelector) {
					iColSelected = lsm.getLeadSelectionIndex();
					lastChanged = 1;
				} else {
					iRowSelected = lsm.getLeadSelectionIndex();
					lastChanged = 2;
				}
				return;
			}
			if ((lsm == columnSelector) != (lastChanged == 1))
				return;
			int icolrow = iRowSelected * 1000 + iColSelected;
			if (icolrow != iRowColSelected) {
				tableCellSelectedEvent(iRowSelected, iColSelected);
				iRowColSelected = icolrow;
			}
		} catch (Exception ee) {
			// ignore
		}
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowClosed(WindowEvent arg0) {
		done();
	}

	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

}
