package jspecview.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;
import jspecview.export.Exporter.ExportType;
import jspecview.util.JSVBase64;
import jspecview.util.JSVFileManager;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class DialogHelper {

	private String thisKey;
	private ActionListener eventListener;
	private Map<String, Object> options;
	private JPanel leftPanel;
	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	private Insets cbInsets = new Insets(0, 0, 2, 2);
	private int iRow;
	private ScriptInterface si;

	public DialogHelper(ScriptInterface si) {
		this.si = si;
	}

	
	public DialogHelper(String thisKey, Map<String, Object> options,
			JPanel leftPanel, ActionListener eventListener) {
		this.thisKey = thisKey;
		this.options = options;
		this.leftPanel = leftPanel;
		this.eventListener = eventListener;
	}

	protected void addButton(JButton selectAllButton) {
		leftPanel.add(selectAllButton, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
	}
//
//	protected JCheckBox addCheckBoxOption(String name, String label,
//			boolean isSelected) {
//		JCheckBox obj = new JCheckBox();
//		obj.setText(label == null ? name : label);
//		obj.addActionListener(eventListener);
//		leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
//				GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
//		iRow++;
//		return obj;
//	}

	protected JTextField addInputOption(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = thisKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		if (visible) {
			obj.setPreferredSize(new Dimension(75, 25));
			obj.addActionListener(eventListener);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
	}

	protected JComboBox<String> addSelectOption(String name, String label, String[] info,
			int iPt, boolean visible) {
		JComboBox<String> obj = new JComboBox<String>(info);
		obj.setSelectedIndex(iPt);
		if (visible) {
			obj.setActionCommand(name);
			// obj.setPreferredSize(new Dimension(100, 25));
			obj.addActionListener(eventListener);
			addPanelLine(name, label, obj, null);
		}
		return obj;
	}

	private void addPanelLine(String name, String label, JComponent obj,
			String units) {
		leftPanel.add(new JLabel(label == null ? name : label),
				new GridBagConstraints(0, iRow, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, cbInsets, 0, 0));
		if (units == null) {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
		} else {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, cbInsets, 0, 0));
			leftPanel.add(new JLabel(units), new GridBagConstraints(2, iRow, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets,
					0, 0));
		}
		iRow++;
	}

	protected synchronized JTable getDataTable(AwtAnnotationDialog ad, 
			String[][] data, String[] columnNames, int[] columnWidths, int height) {
		
		LegendTableModel tableModel = new LegendTableModel(columnNames, data);
		JTable table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setCellSelectionEnabled(true);
    table.getSelectionModel().addListSelectionListener(ad);
    ad.columnSelector = table.getColumnModel().getSelectionModel();
    ad.columnSelector.addListSelectionListener(ad);
		int n = 0;
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
			n += columnWidths[i];
		}
		table.setPreferredScrollableViewportSize(new Dimension(n, height));
		return table;
	}

  /**
   * TableCellRenderer that aligns text in the center of a JTable Cell
   */
  class TitleRenderer extends JLabel
                      implements TableCellRenderer {
      /**
     * 
     */
    private static final long serialVersionUID = 1L;


      public TitleRenderer(){
        setOpaque(true);
      }


      public Component getTableCellRendererComponent(
                              JTable table, Object title,
                              boolean isSelected, boolean hasFocus,
                              int row, int column) {
          setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.RIGHT);
          setText(title.toString());
          if(isSelected)
            setBackground(table.getSelectionBackground());
          else
            setBackground(table.getBackground());

          return this;
      }
  }
	/**
	 * The Table Model for Legend
	 */
	class LegendTableModel extends AbstractTableModel {
		/**
     * 
     */
		private static final long serialVersionUID = 1L;
		String[] columnNames;
		Object[][] data;

		public LegendTableModel(String[] columnNames, String[][] data) {
			this.columnNames = columnNames;
			this.data = data;
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
			return " " + data[row][col] + " ";
		}
		
    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

	}

	private PrintLayout lastPrintLayout;
	private JFileChooser fc;
	public String dirLastOpened;
	public boolean useDirLastOpened;
	public boolean useDirLastExported;
	public String dirLastExported;

	private void saveImage(JSVPanel jsvp, ExportType itype) {
		setFileChooser(itype);
		String name = getSuggestedFileName(itype);
		File file = getFile(name, (Component) jsvp, true);
		if (file == null)
			return;
    Image image = ((Component) jsvp).createImage(jsvp.getWidth(), jsvp.getHeight());
    ((Component) jsvp).paint(image.getGraphics());
    try {
			ImageIO.write((RenderedImage) image, itype.toString().toLowerCase(), file);
		} catch (IOException e) {
			jsvp.showMessage(e.getMessage(), "Error Saving Image");
		}
	}

	public String print(Frame frame, String pdfFileName) {
		if (!si.isSigned())
			return "Error: Applet must be signed for the PRINT command.";
		boolean isJob = (pdfFileName == null || pdfFileName.length() == 0);
		boolean isBase64 = (!isJob && pdfFileName.toLowerCase().startsWith("base64"));
		JSVPanel jsvp = si.getSelectedPanel();
		if (jsvp == null)
			return null;
    jsvp.getPanelData().closeAllDialogsExcept(AType.NONE);
		PrintLayout pl = new AwtPrintLayoutDialog(frame, lastPrintLayout, isJob).getPrintLayout();
		if (pl == null)
			return null;
		lastPrintLayout = pl;
		if (isJob && pl.asPDF) {
			isJob = false;
			pdfFileName = "PDF";
		}		
		if (!isBase64 && !isJob) {
			setFileChooser(ExportType.PDF);
			if (pdfFileName.equals("?") || pdfFileName.equalsIgnoreCase("PDF"))
  			pdfFileName = getSuggestedFileName(ExportType.PDF);
			File file = getFile(pdfFileName, (Component) jsvp, true);
			if (file == null)
				return null;
			si.setProperty("directoryLastExporteFile", dirLastExported = file.getParent());
			pdfFileName = file.getAbsolutePath();
		}
		String s = null;
		try {
			OutputStream os = (isJob ? null : isBase64 ? new ByteArrayOutputStream() 
			    : new FileOutputStream(pdfFileName));
			String printJobTitle = jsvp.getPanelData().getPrintJobTitle(true);
			if (pl.showTitle) {
				printJobTitle = jsvp.getInput("Title?", "Title for Printing", printJobTitle);
				if (printJobTitle == null)
					return null;
			}
			((AwtPanel) jsvp).printPanel(pl, os, printJobTitle);
			s = (isBase64 ? JSVBase64.getBase64(
					((ByteArrayOutputStream) os).toByteArray()).toString() : "OK");
		} catch (Exception e) {
			jsvp.showMessage(e.getMessage(), "File Error");
		}
		return s;
	}


	public void setFileChooser(ExportType imode) {
		if (fc == null)
		  fc = new JFileChooser();
    JSVFileFilter filter = new JSVFileFilter();
    fc.resetChoosableFileFilters();
    switch (imode) {
    case UNK:
  		filter = new JSVFileFilter();
  		filter.addExtension("xml");
  		filter.addExtension("aml");
  		filter.addExtension("cml");
  		filter.setDescription("CML/XML Files");
  		fc.setFileFilter(filter);
  		filter = new JSVFileFilter();
  		filter.addExtension("jdx");
  		filter.addExtension("dx");
  		filter.setDescription("JCAMP-DX Files");
  		fc.setFileFilter(filter);
    	break;
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      break;
    default:
      filter.addExtension(imode.toString().toLowerCase());
      filter.setDescription(imode + " Files");
    }
    fc.setFileFilter(filter);    
	}

	public File showFileOpenDialog(Frame frame) {
		setFileChooser(ExportType.UNK);
		return getFile("", frame, false);
	}


	public void exportSpectrum(JFrame frame, String type) {
		JSVPanel jsvp = si.getSelectedPanel();
		if (jsvp == null)
			return;
		ExportType itype = ExportType.getType(type);
		switch (itype) {
		case PDF:
			print(frame, "PDF");
			break;
		case PNG:
		case JPG:
			saveImage(jsvp, itype);
			break;
		default:
			setFileChooser(itype);
			Exporter.exportSpectrum(si, frame, this, type);
			jsvp.getFocusNow(true);
		}
	}

	public File getFile(String name, Component c, boolean isSave) {
		fc.setSelectedFile(new File(name));
		if (isSave) {
			if (useDirLastExported)
				fc.setCurrentDirectory(new File(dirLastExported));
		} else {
			if (useDirLastOpened)
				fc.setCurrentDirectory(new File(dirLastOpened));
		}
		int returnVal = (isSave ? fc.showSaveDialog(c) : fc.showOpenDialog(c));
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		File file = fc.getSelectedFile();
		if (isSave) {
			si.setProperty("directoryLastExportedFile", dirLastExported = file.getParent());
	    if (file.exists()) {
	      int option = JOptionPane.showConfirmDialog(c,
	          "Overwrite " + file.getName() + "?", "Confirm Overwrite Existing File",
	          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	      if (option == JOptionPane.NO_OPTION)
	        return null;
	    }
		} else {
			si.setProperty("directoryLastOpenedFile", dirLastOpened = file.getParent());
		}
		return file;
	}

	public String getSuggestedFileName(ExportType imode) {
		PanelData pd = si.getSelectedPanel().getPanelData();
    String sourcePath = pd.getSpectrum().getFilePath();
    String newName = JSVFileManager.getName(sourcePath);
    int pt = newName.lastIndexOf(".");
    String name = (pt < 0 ? newName : newName.substring(0, pt));
    String ext = ".jdx";
    boolean isPrint = false;
    switch (imode) {
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
    	if (!(name.endsWith("_" + imode)))
    		name += "_" + imode;    		
      ext = ".jdx";
      break;
    case AML:
    	ext = ".xml";
    	break;
    case JPG:
    case PNG:
    case PDF:
    	isPrint = true;
			//$FALL-THROUGH$
		default:
      ext = "." + imode.toString().toLowerCase();
    }
    if (si.getCurrentSource().isView)
    	name = pd.getPrintJobTitle(isPrint);
    name += ext;
    return name;
	}
}
