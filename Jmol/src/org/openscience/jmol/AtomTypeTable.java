
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

import javax.swing.JTable;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;

import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;
import javax.swing.JColorChooser;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.AbstractCellEditor;
import javax.swing.SwingConstants;
import java.awt.Container;
import java.awt.Color;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FilterOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.EventObject;

/**
 *  @author Bradley A. Smith (bradley@baysmith.com)
 */
public class AtomTypeTable extends JDialog implements ActionListener {

  private static String SAU = "org/openscience/jmol/Data/AtomTypes";

  /*
     OK, this is going to be a bit confusing:
     There is a TableModel that is used to display the spreadsheet-like
     list of atom types.   However, we use a Hashtable to do (relatively)
     fast lookups when we need to know what atom Type is what.  So, the
     data is stored twice.  For now, I am treating the TableModel as
     the primary source that we use to load up the Hashtable.
  */

  AtomTypesModel atModel;

  //static BaseAtomType defaultAtomType;

  public AtomTypeTable(JFrame fr, File UAF) {

    this(fr);

    try {
      ReadAtypes(new FileInputStream(UAF));
    } catch (Exception e1) {
      try {
        URL url = this.getClass().getClassLoader().getResource(SAU);
        // URL url = ClassLoader.getSystemResource(SAU);
        ReadAtypes(url.openStream());
      } catch (Exception e2) {
        System.err.println("Cannot read System AtomTypes: " + e2.toString());
        e2.printStackTrace();
      }
    }
  }

  public AtomTypeTable(JFrame fr) {

    super(fr, JmolResourceHandler.getInstance().getString("AtomTypeTable.Title"), true);

    // Create a model of the data.
    atModel = new AtomTypesModel();

    SortedTableModel sorter = new SortedTableModel(atModel);
    final JTable tableView = new JTable(sorter);

    // Change sort icon when sort properties change.
    sorter.addPropertyChangeListener(new PropertyChangeListener() {

      ImageIcon iconUp = JmolResourceHandler.getInstance().getIcon("AtomTypeTable.upImage");
      ImageIcon iconDown = JmolResourceHandler.getInstance().getIcon("AtomTypeTable.downImage");

      public void propertyChange(PropertyChangeEvent event) {

        if (event.getPropertyName().equals("sortColumn")) {
          Integer oldSortColumn = (Integer) event.getOldValue();
          if (oldSortColumn != null) {
            setIcon(oldSortColumn.intValue(), null);
          }
        }
        SortedTableModel model = (SortedTableModel) event.getSource();
        int column = model.getSortedColumn();
        boolean ascending = model.isAscending();

        if (ascending) {
          setIcon(column, iconDown);
        } else {
          setIcon(column, iconUp);
        }
      }

      void setIcon(int column, ImageIcon icon) {

        TableColumnModel cm = tableView.getColumnModel();
        TableColumn tc = cm.getColumn(column);
        MultiLineHeaderRenderer rr =
          (MultiLineHeaderRenderer) tc.getHeaderRenderer();
        rr.setIcon(icon);
      }

    });

    Enumeration enum = tableView.getColumnModel().getColumns();
    while (enum.hasMoreElements()) {
      MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
      TableColumn tc = (TableColumn) enum.nextElement();
      tc.setHeaderRenderer(renderer);

      // Not yet ready for prime time:
      // tc.setCellRenderer()
    }
    sorter.addMouseListenerToHeaderInTable(tableView);

    tableView.setPreferredScrollableViewportSize(new Dimension(800, 300));
    JScrollPane scrollpane = new JScrollPane(tableView);

    //Set up renderer and editor for the Atom Color column.
    setUpColorRenderer(tableView);
    setUpColorEditor(tableView);

    //Set up real input validation for integer data.
    setUpIntegerEditor(tableView);

    //Set up real input validation for double data.
    setUpDoubleEditor(tableView);

    JPanel AtomTableWrapper = new JPanel(new BorderLayout());
    AtomTableWrapper.add(scrollpane, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton save = new JButton(JmolResourceHandler.getInstance().getString("AtomTypeTable.saveLabel"));
    save.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        SaveAtypes();
      }
    });
    buttonPanel.add(save);
    JButton revert = new JButton(JmolResourceHandler.getInstance().getString("AtomTypeTable.revertLabel"));
    revert.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        atModel.clear();
      }
    });
    buttonPanel.add(revert);
    JButton cancel = new JButton(JmolResourceHandler.getInstance().getString("AtomTypeTable.cancelLabel"));
    cancel.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        CancelPressed();
      }
    });
    buttonPanel.add(cancel);
    JButton OK = new JButton(JmolResourceHandler.getInstance().getString("AtomTypeTable.OKLabel"));
    OK.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        OKPressed();
      }
    });
    buttonPanel.add(OK);

    JToolBar toolbar = new JToolBar();

    JButton natb = new JButton(JmolResourceHandler.getInstance().getIcon("AtomTypeTable.newAtypeImage"));
    natb.setMargin(new Insets(1, 1, 1, 1));
    natb.setToolTipText(JmolResourceHandler.getInstance().getString("AtomTypeTable.newAtypeTooltip"));
    natb.setActionCommand("newatype");
    natb.addActionListener(this);

    JButton datb = new JButton(JmolResourceHandler.getInstance().getIcon("AtomTypeTable.delAtypeImage"));
    datb.setMargin(new Insets(1, 1, 1, 1));
    datb.setToolTipText(JmolResourceHandler.getInstance().getString("AtomTypeTable.delAtypeTooltip"));
    datb.setActionCommand("delatype");
    datb.addActionListener(this);

    toolbar.add(natb);
    toolbar.add(datb);

    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());

    container.add(toolbar, BorderLayout.NORTH);
    container.add(AtomTableWrapper, BorderLayout.CENTER);
    container.add(buttonPanel, BorderLayout.SOUTH);

    getContentPane().add(container);
    pack();
    centerDialog();
  }

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

  public void CancelPressed() {

    // ADD CODE HERE TO CHANGE BACK TO "CLEAN" AtomType VALUES
    this.setVisible(false);
  }

  public void OKPressed() {
    this.setVisible(false);
  }

  public void actionPerformed(ActionEvent evt) {

    String arg = evt.getActionCommand();

    if (arg == "newatype") {

      // ADD CODE HERE TO ADD AN ATOM TYPE
      System.out.println(
              "Not yet implemented: User wants to add an atom type!");
    }
    if (arg == "delatype") {

      // ADD CODE HERE TO DELETE AN ATOM TYPE
      System.out.println(
              "Not yet implemented: User wants to delete an atom type!");
    }
  }

  class ColorRenderer extends JLabel implements TableCellRenderer {

    Border unselectedBorder = null;
    Border selectedBorder = null;
    boolean isBordered = true;

    public ColorRenderer(boolean isBordered) {
      super();
      this.isBordered = isBordered;
      setOpaque(true);    //MUST do this for background to show up.
    }

    public Component getTableCellRendererComponent(JTable table,
            Object color, boolean isSelected, boolean hasFocus, int row,
              int column) {

      this.setBackground((Color) color);
      if (isBordered) {
        if (isSelected) {
          if (selectedBorder == null) {
            selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                    table.getSelectionBackground());
          }
          setBorder(selectedBorder);
        } else {
          if (unselectedBorder == null) {
            unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                    table.getBackground());
          }
          setBorder(unselectedBorder);
        }
      }
      return this;
    }
  }

  private void setUpColorRenderer(JTable table) {
    table.setDefaultRenderer(Color.class, new ColorRenderer(true));
  }

  //Set up the editor for the Color cells.
  private void setUpColorEditor(JTable table) {

    //First, set up the button that brings up the dialog.
    final JButton button = new JButton("") {

      public void setText(String s) {

        //Button never shows text -- only color.
      }
    };
    button.setBackground(Color.white);
    button.setBorderPainted(false);
    button.setMargin(new Insets(0, 0, 0, 0));

    //Now create an editor to encapsulate the button, and
    //set it up as the editor for all Color cells.
    try {
      final ColorEditor colorEditor = new ColorEditor(button);
      table.setDefaultEditor(Color.class, colorEditor);

      //Set up the dialog that the button brings up.
      final JColorChooser colorChooser = new JColorChooser();
      
      //XXX: PENDING: add the following when setPreviewPanel
      //XXX: starts working.
      //JComponent preview = new ColorRenderer(false);
      //preview.setPreferredSize(new Dimension(50, 10));
      //colorChooser.setPreviewPanel(preview);
      ActionListener okListener = new ActionListener() {
	      
	      public void actionPerformed(ActionEvent e) {
		  colorEditor.currentColor = colorChooser.getColor();
	      }
	  };
      final JDialog dialog = JColorChooser.createDialog(button, "Pick a Color",
							true, colorChooser, okListener, null);    //XXXDoublecheck this is OK
      
      //Here's the code that brings up the dialog.
      button.addActionListener(new ActionListener() {
	      
	      public void actionPerformed(ActionEvent e) {
		  
		  button.setBackground(colorEditor.currentColor);
		  colorChooser.setColor(colorEditor.currentColor);
		  
		  //Without the following line, the dialog comes up
		  //in the middle of the screen.
		  //dialog.setLocationRelativeTo(button);
		  dialog.show();
	      }
	  });
    } catch (VerifyError e) {
	/* not catching this gives problems on 
           "SunOS ac9 5.8 Generic_108528-10 sun4u sparc SUNW,Ultra-5_10" with
           java version "1.2.2" Solaris VM (build Solaris_JDK_1.2.2_07a, native threads, sunwjit) 
	   
           See also: http://www.geocrawler.com/lists/3/SourceForge/11143/0/
	   date: 22 January 2002
	*/
    }
  }


  /*
   * The editor button that brings up the dialog.
   * We extend DefaultCellEditor for convenience,
   * even though it mean we have to create a dummy
   * check box.  Another approach would be to copy
   * the implementation of TableCellEditor methods
   * from the source code for DefaultCellEditor.
   */
  class ColorEditor extends DefaultCellEditor {

    Color currentColor = null;

    public ColorEditor(JButton b) {

      super(new JCheckBox());    //Unfortunately, the constructor

      //expects a check box, combo box,
      //or text field.
      editorComponent = b;
      setClickCountToStart(1);    //This is usually 1 or 2.

      //Must do this so that editing stops when appropriate.
      b.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          fireEditingStopped();
        }
      });
    }

    protected void fireEditingStopped() {
      super.fireEditingStopped();
    }

    public Object getCellEditorValue() {
      return currentColor;
    }

    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
      ((JButton) editorComponent).setText(value.toString());
      currentColor = (Color) value;
      return editorComponent;
    }
  }

  private void setUpIntegerEditor(JTable table) {

    //Set up the editor for the integer cells.
    final WholeNumberField integerField = new WholeNumberField(0, 5);
    integerField.setHorizontalAlignment(WholeNumberField.RIGHT);

    DefaultCellEditor integerEditor = new DefaultCellEditor(integerField) {

      //Override DefaultCellEditor's getCellEditorValue method
      //to return an Integer, not a String:
      public Object getCellEditorValue() {
        return new Integer(integerField.getValue());
      }
    };
    table.setDefaultEditor(Integer.class, integerEditor);
  }

  private void setUpDoubleEditor(JTable table) {

    //Set up the editor for the double cells.
    final DecimalNumberField doubleField = new DecimalNumberField(0, 10);
    doubleField.setHorizontalAlignment(DecimalNumberField.RIGHT);

    DefaultCellEditor doubleEditor = new DefaultCellEditor(doubleField) {

      //Override DefaultCellEditor's getCellEditorValue method
      //to return a double, not a String:
      public Object getCellEditorValue() {
        return new Double(doubleField.getValue());
      }
    };
    table.setDefaultEditor(Double.class, doubleEditor);
  }

  void SaveAtypes() {

    try {
      FileOutputStream fdout = new FileOutputStream(Jmol.UserAtypeFile);
      BufferedOutputStream bos = new BufferedOutputStream(fdout, 1024);
      PrintWriter pw = new PrintWriter(bos);

      int numRows = atModel.getRowCount();
      int numCols = atModel.getColumnCount();

      String headline = "#" + atModel.getColumnName(0);
      for (int j = 1; j < numCols - 1; j++) {
        headline += "\t";

        String str = atModel.getColumnName(j);
        BufferedReader br = new BufferedReader(new StringReader(str));
        String line;
        try {
          line = br.readLine();
          while (line != null) {
            headline = headline + line + " ";
            line = br.readLine();
          }
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
      headline += "\tRed\tGreen\tBlue";
      pw.println(headline);

      for (int i = 0; i < numRows; i++) {
        String outline = (String) atModel.getValueAt(i, 0);
        for (int j = 1; j < numCols - 1; j++) {
          outline = outline + "\t" + atModel.getValueAt(i, j);
        }
        Color c = (Color) atModel.getValueAt(i, numCols - 1);
        outline = outline + "\t" + c.getRed();
        outline = outline + "\t" + c.getGreen();
        outline = outline + "\t" + c.getBlue();
        pw.println(outline);
      }
      pw.flush();
      pw.close();
      bos.close();
      fdout.close();

    } catch (IOException e) {
      System.err.println("Exception: " + e);
    }
    return;
  }

  public void setAtomTypes(AtomTypeSet ats) {
    Enumeration iter = ats.elements();
    while (iter.hasMoreElements()) {
      atModel.updateAtomType((BaseAtomType) iter.nextElement());
    }
  }


  void ReadAtypes(InputStream is) throws Exception {

    BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);
    StringTokenizer st;

    String s;

    atModel.clear();

    try {
      while (true) {
        s = r.readLine();
        if (s == null) {
          break;
        }
        if (!s.startsWith("#")) {
          String name = "";
          String rootType = "";
          int an = 0, rl = 0, gl = 0, bl = 0;
          double mass = 0.0, vdw = 0.0, covalent = 0.0;
          st = new StringTokenizer(s, "\t ,;");
          int nt = st.countTokens();

          if (nt == 9) {
            name = st.nextToken();
            rootType = st.nextToken();
            String san = st.nextToken();
            String sam = st.nextToken();
            String svdw = st.nextToken();
            String scov = st.nextToken();
            String sr = st.nextToken();
            String sg = st.nextToken();
            String sb = st.nextToken();

            try {
              mass = new Double(sam).doubleValue();
              vdw = new Double(svdw).doubleValue();
              covalent = new Double(scov).doubleValue();
              an = new Integer(san).intValue();
              rl = new Integer(sr).intValue();
              gl = new Integer(sg).intValue();
              bl = new Integer(sb).intValue();
            } catch (NumberFormatException nfe) {
              throw new JmolException("AtomTypeTable.ReadAtypes",
                      "Malformed Number");
            }

            AtomType at = new AtomType(name, rootType, an, mass, vdw,
                            covalent, rl, gl, bl);

            atModel.updateAtomType(at.getBaseAtomType());

          } else {
            throw new JmolException("AtomTypeTable.ReadAtypes",
                    "Wrong Number of fields");
          }
        }
      }    // end while

      is.close();

    }      // end Try
            catch (IOException e) {
    }

  }

  public BaseAtomType get(String name) {
    return atModel.get(name);
  }

  public BaseAtomType get(int atomicNumber) {
    return atModel.get(atomicNumber);
  }

  public synchronized Enumeration elements() {
    return atModel.elements();
  }
}

