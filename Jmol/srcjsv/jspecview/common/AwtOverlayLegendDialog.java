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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jspecview.common.JDXSpectrum;
import jspecview.util.JSVFileManager;

/**
 * Dialog for showing the legend or key for overlaid plots in a
 * <code>JSVPanel</code>.
 * 
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class AwtOverlayLegendDialog extends JDialog implements JSVDialog {

	/**
   * 
   */
	private static final long serialVersionUID = 1L;
	JSVPanel jsvp;

	/**
	 * Initialises a non-modal <code>OverlayLegendDialog</code> with a default
	 * title of "Legend: " + jsvp.getTitle() and parent frame
	 * 
	 * @param frame
	 *          the parent frame
	 * @param jsvp
	 *          the <code>JSVPanel</code>
	 */
	public AwtOverlayLegendDialog(Frame frame, JSVPanel jsvp) {
		super(frame, jsvp.getPanelData().getViewTitle(), false);
		this.jsvp = jsvp;
		init();
		this.pack();
		setVisible(false);
	}

	/**
	 * Initialises GUI Components
	 */
	private void init() {

		LegendTableModel tableModel = new LegendTableModel();
		JTable table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ListSelectionModel specSelection = table.getSelectionModel();
		specSelection.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				jsvp.getPanelData().setSpectrum(lsm.getMinSelectionIndex(), false);
			}
		});
		table.setDefaultRenderer(Color.class, new ColorRenderer());
		table.setDefaultRenderer(String.class, new TitleRenderer());
		table.setPreferredScrollableViewportSize(new Dimension(350, 95));
		TableColumn column = null;
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(30);
		column = table.getColumnModel().getColumn(1);
		column.setPreferredWidth(60);
		column = table.getColumnModel().getColumn(2);
		column.setPreferredWidth(250);
		JScrollPane scrollPane = new JScrollPane(table);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * The Table Model for Legend
	 */
	class LegendTableModel extends AbstractTableModel {
		/**
     * 
     */
		private static final long serialVersionUID = 1L;
		String[] columnNames = { "No.", "Plot Color", "Title" };
		Object[][] data;

		public LegendTableModel() {
			init();
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.length;
		}

		@Override
		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		private void init() {
			Color plotColor;
			String title;
			JDXSpectrum spectrum;
			Object[] cols;

			int numSpectra = jsvp.getPanelData().getNumberOfSpectraInCurrentSet();
			data = new Object[numSpectra][];
			PanelData pd = jsvp.getPanelData();
			String f1 = pd.getSpectrumAt(0).getFilePath();
			String f2 = pd.getSpectrumAt(numSpectra - 1).getFilePath();
			boolean useFileName = !f1.equals(f2);

			for (int index = 0; index < numSpectra; index++) {
				cols = new Object[3];

				spectrum = pd.getSpectrumAt(index);
				title = spectrum.getTitle();
				if (useFileName)
					title = JSVFileManager.getName(spectrum.getFilePath()) + " - " + title;
				plotColor = (Color) jsvp.getPlotColor(index);

				cols[0] = new Integer(index + 1);
				cols[1] = plotColor;
				cols[2] = " " + title;

				data[index] = cols;
			}
		}
	}

	/**
	 * TableCellRenderer that allows the colors to be displayed in a JTable cell
	 */
	class ColorRenderer extends JLabel implements TableCellRenderer {

		/**
       * 
       */
		private static final long serialVersionUID = 1L;

		public ColorRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object color,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Border border;
			setBackground((Color) color);
			if (isSelected) {
				border = BorderFactory.createMatteBorder(2, 5, 2, 5, table
						.getSelectionBackground());
				setBorder(border);
			} else {
				border = BorderFactory.createMatteBorder(2, 5, 2, 5, table
						.getBackground());
				setBorder(border);
			}

			return this;
		}
	}

	/**
	 * TableCellRenderer that aligns text in the center of a JTable Cell
	 */
	class TitleRenderer extends JLabel implements TableCellRenderer {
		/**
       * 
       */
		private static final long serialVersionUID = 1L;

		public TitleRenderer() {
			setOpaque(true);
		}

		public Component getTableCellRendererComponent(JTable table, Object title,
				boolean isSelected, boolean hasFocus, int row, int column) {
			setHorizontalAlignment(SwingConstants.LEFT);
			setText(title.toString());
			// setText("   " + title.toString());

			if (isSelected)
				setBackground(table.getSelectionBackground());
			else
				setBackground(table.getBackground());

			return this;
		}
	}
}
