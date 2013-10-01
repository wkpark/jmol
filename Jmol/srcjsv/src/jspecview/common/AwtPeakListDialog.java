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

import javax.swing.JComboBox;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;
import jspecview.util.JSVTextFormat;

/**
 * Dialog for managing the peak listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

class AwtPeakListDialog extends AwtAnnotationDialog {

	private static final long serialVersionUID = 1L;
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
	private JTextField txtThreshold;
//	private JTextField txtInclude;
//	private JTextField txtSkip;
	private JComboBox<String> cbInterpolation;
	private boolean skipCreate;

	protected AwtPeakListDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp) {
		super(si, spec, jsvp);
		thisType = AType.PeakList;
		setTitle(title);
		setup();
	}

	@Override
	protected int[] getPosXY() {
		return posXY;
	}

	@Override
	protected void addControls() {
		txtThreshold = dialogHelper.addInputOption("Threshold", null, null, "",
				"", true);
		setThreshold(Double.NaN);
		cbInterpolation = dialogHelper.addSelectOption("Interpolation", null,
				new String[] { "parabolic", "none" }, 0, true);
	}

	private void setThreshold(double y) {
		if (Double.isNaN(y)) {
			PanelData pd = jsvp.getPanelData();
			double f = (pd.getSpectrum().isInverted() ? 0.1 : 0.9);
			Coordinate c = pd.getClickedCoordinate();
			y = (c == null ? (pd.getView().minYOnScale * f 
					+ pd.getView().maxYOnScale) * (1 -f) : c.getYVal());
		}
		String sy = JSVTextFormat.getDecimalFormat(y < 1000 ? "#0.00" : "0.00E0")
				.format(y);
		txtThreshold.setText(" " + sy);
		//setVisible(true);
	}




	/*		String script = "PEAKLIST";
			String s = txtThreshold.getText();
			if (s.startsWith("("))
				script += " include=" + txtInclude.getText();
			else
				script += " threshold=" + s;
			script += " skip=" + txtSkip.getText();
			script += " interpolate=" + cbInterpolation.getSelectedItem().toString();
			System.out.println(script);
			JSViewer.runScriptNow(si, script);
	*/		

	@Override
	protected void applyButtonPressed() {
		createData();
		skipCreate = true;
		apply();
	}

	@Override
	public void apply() {
		if (!skipCreate) {
  		setThreshold(Double.NaN);
  		createData();
		}
		skipCreate = false;
		super.apply();
	}

	@Override
	public void setFields() {
		myParams = xyData.getParameters();
		setThreshold(myParams.peakListThreshold);
		cbInterpolation.setSelectedIndex(myParams.peakListInterpolation.equals("none") ? 1 : 0);
		createData();
	}
	
	@Override
	protected void setParams() {
		try {
//			String s = txtInclude.getText();
//			 myParams.peakListInclude = (s.startsWith("(") ? -1 :  Integer.valueOf(s));
			 String s = txtThreshold.getText();
			 myParams.peakListThreshold = (/*s.startsWith("(") ? -1 : */ Double.valueOf(s)).doubleValue();
//			 myParams.peakListSkip = Integer.valueOf(txtSkip.getText());
			 myParams.peakListInterpolation = cbInterpolation.getSelectedItem().toString();
			 super.setParams();
		} catch (Exception e) {
			//
		}
	}

	@Override
	protected void clear() {
		// n/a
	}
		
	@Override
	protected void done() {
		super.done();
	}

	@Override
	protected void updateValues() {
		loadData();
	}

	@Override
	protected void createData() {
		setParams();
		PeakData md = new PeakData(AType.PeakList, spec);
	  md.setPeakList(myParams, numberFormatter, jsvp.getPanelData().getView());
		xyData = md;
		loadData();
	}

	private void loadData() {
		if (xyData == null)
			createData();
		String[][] data = ((PeakData)xyData).getMeasurementListArray(null);
		String[] header = ((PeakData)xyData).getDataHeader();
		int[] widths = new int[] {40, 65, 50, 50, 50, 50, 50};
		loadData(data, header, widths);
    dataTable.setCellSelectionEnabled(true);
	}

	public synchronized void update(Coordinate clicked) {
		apply();
		if (xyData == null || clicked == null)
			return;
		int ipt = 0;
		double dx0 = 1e100;
		double xval = clicked.getXVal();
		PeakData md = (PeakData) xyData;
		for (int i = md.size(); --i >= 0;) {
			double dx = Math.abs(xval - md.get(i).getXVal());
			if (dx < dx0) {
				dx0 = dx;
				ipt = i;
			}
			if (dx0 < 0.1)
				dataTable.getSelectionModel().setSelectionInterval(md.size() - 2 - ipt,
						md.size() - 1 - ipt);
		}
	}

	@Override
	public void tableCellSelectedEvent(int iRow, int iCol) {
		try {
			String value = tableData[iRow][1];
			switch (iCol) {
			case 6:
			case 5:
			case 4:
				String value2 = tableData[iRow + 3 - iCol][1];
				jsvp.getPanelData().findX2(spec, Double.parseDouble(value),
						spec, Double.parseDouble(value2));
				break;
			default:
				jsvp.getPanelData().findX(spec, Double.parseDouble(value));
			}
		} catch (Exception e) {
			jsvp.getPanelData().findX(spec, 1E100);
		}
		jsvp.doRepaint();
	}
	
	@Override
	public void reEnable() {
		skipCreate = true;
		super.reEnable();
	}
	

}
