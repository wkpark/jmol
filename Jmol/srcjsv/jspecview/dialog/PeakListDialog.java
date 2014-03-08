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

package jspecview.dialog;

/**
 * Dialog for managing the peak listing 
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class PeakListDialog extends JSVDialog {

	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};

	public PeakListDialog() {
		// called by reflection in JSViewer
		type = AType.PeakList;
	}

	@Override
	public int[] getPosXY() {
		return posXY;
	}

	@Override
	public void addUniqueControls() {
		txt1 = dialog.addTextField("txtThreshold", "Threshold", null, "",
				"", true);
		setThreshold(Double.NaN);
		combo1 = dialog.addSelectOption("cmbInterpolation", "Interpolation", 
				new String[] { "parabolic", "none" }, 0, true);
	}

	@Override
	public boolean callback(String id, String msg) {
		if (id.equals("cmbInterpolation") || id.equals("txtThreshold"))
			id = "btnApply";
		return callbackAD(id, msg);
	}

}
