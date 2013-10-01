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

package jspecview.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanelNode;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;
import jspecview.util.JSVLogger;

/**
 * Dialog that displays String of text or contents of a file in a
 * </code>JEditorPane</code>.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class TextDialog extends JDialog {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  JPanel contentPanel = new JPanel();
  BorderLayout borderLayout = new BorderLayout();
  Reader reader;
  JScrollPane scrollPane;
  JEditorPane sourcePane = new JEditorPane();

  /**
   * Intilialises a <code>TextDialog</code> with a Reader from which to read the
   * pane content
   * @param frame the parent frame
   * @param title the title
   * @param modal true if modal, otherwise false
   * @param reader the Reader
   */
  public TextDialog(Frame frame, String title, boolean modal, Reader reader) {
    super(frame, title, modal);
    try {
      this.reader = (reader == null ? JSVFileManager.getBufferedReaderFromName(title, null, null) : reader);
      jbInit();
      //setSize(500, 400);
      pack();
      setVisible(true);
    }
    catch(Exception ex) {
    	JSVLogger.error(ex.getMessage());
    }
  }

  /**
   * Intialises <code>TextDialog</code> with a String
   * @param frame the parent frame
   * @param title the title of the dialog
   * @param string the string to write to the JEditorPane
   * @param modal true if modal, false otherwise
   */
  public TextDialog(Frame frame, String title, String string, boolean modal) {
    this(frame, title, modal, new StringReader(string));
  }

  /**
   * Intialises <code>TextDialog</code> with the contents of a file
   * @param frame the parent frame
   * @param file the file to write to the JEditorPane
   * @param modal true if modal, false otherwise
   * @throws IOException
   */
  public TextDialog(Frame frame, String file, boolean modal) throws IOException{
    this(frame, file, modal, null);
  }

  /**
   * Initialises GUI Components
   * @throws Exception
   */
  void jbInit() throws Exception {
    contentPanel.setLayout(borderLayout);
    sourcePane.read(reader, "the text");
    sourcePane.setEditable(false);
    sourcePane.setFont(new Font(null, Font.BOLD, 12));
    getContentPane().add(contentPanel);
    scrollPane = new JScrollPane(sourcePane);
    scrollPane.setPreferredSize(new Dimension(500, 400));
    scrollPane.setMinimumSize(new Dimension(500, 400));
    contentPanel.add(scrollPane,  BorderLayout.CENTER);
  }

  public static void showProperties(Component c, JDXSpectrum spectrum) {
    Object[][] rowData = spectrum.getHeaderRowDataAsArray();
    String[] columnNames = { "Label", "Description" };
    JTable table = new JTable(rowData, columnNames);
    table.setPreferredScrollableViewportSize(new Dimension(400, 195));
    JScrollPane scrollPane = new JScrollPane(table);
    JOptionPane.showMessageDialog(c, scrollPane, "Header Information",
        JOptionPane.PLAIN_MESSAGE);
  }

  public static void showSource(MainFrame mainFrame) {
    JDXSource currentSource = mainFrame.getCurrentSource();
    List<JSVPanelNode> panelNodes = mainFrame.getPanelNodes();
    if (currentSource == null) {
      if (panelNodes.size() > 0) {
        JOptionPane.showMessageDialog(mainFrame, "Please Select a Spectrum",
            "Select Spectrum", JOptionPane.ERROR_MESSAGE);
      }
      return;
    }
    try {
      new TextDialog(mainFrame, currentSource.getFilePath(), true);
    } catch (IOException ex) {
      new TextDialog(mainFrame, "File Not Found", "File Not Found", true);
    }
  }

  public static void showError(MainFrame mainFrame) {
    JDXSource currentSource = mainFrame.getCurrentSource();
    if (currentSource == null) {
      JOptionPane.showMessageDialog(null, "Please Select a Spectrum",
          "Select Spectrum", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String errorLog = currentSource.getErrorLog();
    if (errorLog != null)
      new TextDialog(mainFrame, currentSource.getFilePath(), errorLog, true);
  }
}
