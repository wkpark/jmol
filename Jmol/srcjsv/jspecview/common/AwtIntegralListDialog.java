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

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;
import jspecview.util.JSVTextFormat;

/**
 * Dialog for managing the integral listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtIntegralListDialog extends AwtAnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
	private JTextField txtScale;
	private JTextField txtOffset;
	//private JTextField txtNormalization;
	
	private AwtIntegralListDialog dialog;

	protected AwtIntegralListDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp) {
		super(si, spec, jsvp);
		thisType = AType.Integration;
		setTitle(title);
		setup();
		myParams.integralOffset = si.getParameters().integralOffset;
		myParams.integralRange = si.getParameters().integralRange;
		xyData = new IntegralData(spec, myParams);
		dialog = this;
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	private int iSelected = -1;
	
	protected double lastNorm = 1.0;
	
	
	@Override
	protected void addControls() {
		txtScale = dialogHelper.addInputOption("Scale", "Scale", null, "%", ""
				+ si.getParameters().integralRange, true);
		txtOffset = dialogHelper.addInputOption("BaselineOffset", "Baseline Offset", null, "%",
				"" + si.getParameters().integralOffset, true);
		//chkResets = dialogHelper.addCheckBoxOption("BaselineResets", "Baseline Resets", true);
  	JButton autoButton = newJButton();
    autoButton.setText("Auto");
    autoButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        si.runScript("integrate auto");
      }
    });
		dialogHelper.addButton(autoButton);
    
		JButton deleteButton = newJButton();
		deleteButton.setText("Delete");
		deleteButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});
		dialogHelper.addButton(deleteButton);

		JButton normalizeButton = newJButton();
		normalizeButton.setText("Normalize");
		normalizeButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				normalize();
			}
		});

		dialogHelper.addButton(normalizeButton);
		JButton minButton = newJButton();
		minButton.setText("Minimum");
		minButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setMinimum();
			}
		});
		dialogHelper.addButton(minButton);
	}


	protected void delete() {
		if (!checkSelected())
			return;
		xyData.remove(iSelected);
		iSelected = -1;
		iRowColSelected = -1;
		apply();
		jsvp.doRepaint();
	}

	private boolean checkSelected() {
		if (iSelected < 0) {
			JOptionPane.showMessageDialog(dialog,
					"Select a line on the table first, then click this button.");
			return false;
		}
		return true;
	}

	protected void normalize() {
		if (!checkSelected())
			return;
		try {
			String ret = (String) JOptionPane.showInputDialog(dialog,
					"Enter a normalization factor", "Normalize",
					JOptionPane.QUESTION_MESSAGE, null, null, "" + lastNorm);
			double val = Double.parseDouble(ret);
			if (val > 0)
  			lastNorm = val;
			((IntegralData) xyData).setSelectedIntegral(xyData.get(iSelected), val);
			apply();
			jsvp.doRepaint();
		} catch (Exception ee) {
			// ignore
		}
	}

	double lastMin = 0;
	protected void setMinimum() {
		try {
			String ret = (String) JOptionPane.showInputDialog(dialog,
					"Minimum value?", "Set Minimum Value",
					JOptionPane.QUESTION_MESSAGE, null, null, "" + lastMin);
			double val = Double.parseDouble(ret);
			((IntegralData) xyData).setMinimumIntegral(val);
			apply();
			jsvp.doRepaint();
		} catch (Exception ee) {
			// ignore
		}
	}
	@Override
	public void apply() {
		try {
			myParams.integralOffset = Double.valueOf(txtOffset.getText()).doubleValue();
			myParams.integralRange = Double.valueOf(txtScale.getText()).doubleValue();
			myParams.integralDrawAll = false;//chkResets.isSelected();
			((IntegralData) getData()).update(myParams);
			jsvp.doRepaint();
			super.apply();
		} catch (Exception e) {
			// ignore?
		}
		//JSViewer.runScriptNow(si, "integralOffset " + txtOffset.getText() 
			//	+ ";integralRange " + txtRange.getText() + ";showIntegration");
	}
	
	@Override
	protected void done() {
		super.done();
	}

	public void update(Coordinate clicked) {
		updateValues();
		checkEnables();
	}

	@Override
	protected void updateValues() {
		loadData();
	}

	private void loadData() {
		if (xyData == null)
			createData();
		iSelected = -1;
		String[][] data = ((IntegralData) xyData).getMeasurementListArray(null);
		String[] header = xyData.getDataHeader();
		int[] widths = new int[] {40, 65, 65, 50};
		loadData(data, header, widths);
	}

	@Override
	protected void createData() {
		xyData = new IntegralData(spec, myParams);
		iSelected = -1;
	}

	@Override
	public void tableCellSelectedEvent(int iRow, int iCol) {
		DecimalFormat df2 = JSVTextFormat.getDecimalFormat("#0.00");
		String value = tableData[iRow][1];
		for (int i = 0; i < xyData.size(); i++) 
			if (df2.format(xyData.get(i).getXVal()).equals(value)) {
				iSelected = i;
				jsvp.getPanelData().findX2(spec, xyData.get(i).getXVal(), spec, xyData.get(i).getXVal2());
				jsvp.doRepaint();
				break;
			}		
		checkEnables();
	}

}