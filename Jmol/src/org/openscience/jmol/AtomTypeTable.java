/*
 * @(#)AtomTypeTable.java    1.0 98/11/04
 *
 * Copyright (c) 1998 J. Daniel Gezelter All Rights Reserved.
 *
 * J. Daniel Gezelter grants you ("Licensee") a non-exclusive, royalty
 * free, license to use, modify and redistribute this software in
 * source and binary code form, provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED.  J. DANIEL GEZELTER AND HIS LICENSORS SHALL NOT BE LIABLE
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 * EVENT WILL J. DANIEL GEZELTER OR HIS LICENSORS BE LIABLE FOR ANY
 * LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF J. DANIEL GEZELTER HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.  
 */

package org.openscience.jmol;

import javax.swing.JTable;
import javax.swing.table.*;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;

import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;
import javax.swing.JToolBar.*;
import javax.swing.JColorChooser;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.*;

import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.io.IOException;
import java.io.File;
import java.util.*;

public class AtomTypeTable extends JDialog implements ActionListener {
    private boolean DEBUG = false;
    private static JmolResourceHandler jrh;
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
    static AtomType defaultAtomType;
    
    static {
        jrh = new JmolResourceHandler("AtomTypeTable");
    } 

    public AtomTypeTable(JFrame fr, File UAF) {
        this(fr);

        try {
            FileInputStream fis = new FileInputStream(UAF);        
            ReadAtypes(fis);
        } catch (Exception e1) {            
            URL url = ClassLoader.getSystemResource(SAU);
            try { 
                InputStream is = url.openStream();
                ReadAtypes(is);
            } catch(Exception e2) {
                System.err.println("Cannot read System AtomTypes: " + 
                                   e2.toString());
            }
        }
    }
    
    public AtomTypeTable(JFrame fr) {
        super(fr, jrh.getString("Title"), true);
        // Create a model of the data.
        atModel = new AtomTypesModel();
        
        poshTableSorter sorter = new poshTableSorter(atModel); 
        JTable tableView = new JTable(sorter);         
        Enumeration enum = tableView.getColumnModel().getColumns();
        while (enum.hasMoreElements()) {
            MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
            TableColumn tc = (TableColumn)enum.nextElement();
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
        buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
        JButton save = new JButton(jrh.getString("saveLabel"));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SaveAtypes();
            }});
        buttonPanel.add(save);
        JButton revert = new JButton(jrh.getString("revertLabel"));
        revert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                atModel.clear();
            }});
        buttonPanel.add(revert);
        JButton cancel = new JButton(jrh.getString("cancelLabel"));
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CancelPressed();
            }});
        buttonPanel.add(cancel);
        JButton OK = new JButton(jrh.getString("OKLabel"));
        OK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OKPressed();
            }});
        buttonPanel.add(OK);
        
        JToolBar toolbar = new JToolBar();

        JButton natb = new JButton(jrh.getIcon("newAtypeImage"));
        natb.setMargin(new Insets(1,1,1,1));
        natb.setToolTipText(jrh.getString("newAtypeTooltip"));
        natb.setActionCommand("newatype");
        natb.addActionListener(this);

        JButton datb = new JButton(jrh.getIcon("delAtypeImage"));
        datb.setMargin(new Insets(1,1,1,1));
        datb.setToolTipText(jrh.getString("delAtypeTooltip"));
        datb.setActionCommand("delatype");
        datb.addActionListener(this);
        
        toolbar.add(natb);
        toolbar.add(datb);

        JPanel container = new JPanel();
        container.setLayout( new BorderLayout() );
        
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
        screenSize.height = screenSize.height/2;
        screenSize.width = screenSize.width/2;
        size.height = size.height/2;
        size.width = size.width/2;
        int y = screenSize.height - size.height;
        int x = screenSize.width - size.width;
        this.setLocation(x,y);
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
            System.out.println("Not yet implemented: User wants to add an atom type!");
        }
        if (arg == "delatype") {
            // ADD CODE HERE TO DELETE AN ATOM TYPE
            System.out.println("Not yet implemented: User wants to delete an atom type!");
        }
    }

    class ColorRenderer extends JLabel implements TableCellRenderer {
        Border unselectedBorder = null;
        Border selectedBorder = null;
        boolean isBordered = true;
        
        public ColorRenderer(boolean isBordered) {
            super();
            this.isBordered = isBordered;
            setOpaque(true); //MUST do this for background to show up.
        }
        
        public Component getTableCellRendererComponent(
                                                       JTable table, 
                                                       Object color, 
                                                       boolean isSelected, 
                                                       boolean hasFocus,
                                                       int row, int column) {
            this.setBackground((Color)color);
            if (isBordered) {
                if (isSelected) {
                    if (selectedBorder == null) {
                        selectedBorder = BorderFactory.createMatteBorder(2,5,
                                                                         2,5,
                                                                         table.getSelectionBackground());
                    }
                    setBorder(selectedBorder);
                } else {
                    if (unselectedBorder == null) {
                        unselectedBorder = BorderFactory.createMatteBorder(2,5,
                                                                           2,5,
                                                                           table.getBackground());
                    }
                    setBorder(unselectedBorder);
                }
            }
            return this;
        }
    }
    private void setUpColorRenderer(JTable table) {
        table.setDefaultRenderer(Color.class,
                                 new ColorRenderer(true));
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
        button.setMargin(new Insets(0,0,0,0));

        //Now create an editor to encapsulate the button, and
        //set it up as the editor for all Color cells.
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
        final JDialog dialog = JColorChooser.createDialog(button,
                                        "Pick a Color",
                                        true,
                                        colorChooser,
                                        okListener,
                                        null); //XXXDoublecheck this is OK

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
                super(new JCheckBox()); //Unfortunately, the constructor
                                        //expects a check box, combo box,
                                        //or text field.
            editorComponent = b;
            setClickCountToStart(1); //This is usually 1 or 2.

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

        public Component getTableCellEditorComponent(JTable table, 
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            ((JButton)editorComponent).setText(value.toString());
            currentColor = (Color)value;
            return editorComponent;
        }
    }

    private void setUpIntegerEditor(JTable table) {
        //Set up the editor for the integer cells.
        final WholeNumberField integerField = new WholeNumberField(0, 5);
        integerField.setHorizontalAlignment(WholeNumberField.RIGHT);

        DefaultCellEditor integerEditor = 
            new DefaultCellEditor(integerField) {
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

        DefaultCellEditor doubleEditor = 
            new DefaultCellEditor(doubleField) {
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
            FileOutputStream fdout =
                new FileOutputStream(Jmol.UserAtypeFile);
            BufferedOutputStream bos =
                new BufferedOutputStream(fdout, 1024);
            PrintWriter pw = new PrintWriter(bos);
            
            int numRows = atModel.getRowCount();
            int numCols = atModel.getColumnCount();
            
            String headline = "#" + atModel.getColumnName(0);
            for (int j = 1; j<numCols-1; j++) {
                headline = headline + "\t";
                
                String str = atModel.getColumnName(j);
                BufferedReader br = new BufferedReader(new StringReader(str));
                String line;
                try {
                    while ((line = br.readLine()) != null) {
                        headline = headline + line + " ";
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            headline = headline + "\tRed\tGreen\tBlue";
            pw.println(headline);
            
            for (int i=0; i < numRows; i++) {
                String outline = (String) atModel.getValueAt(i,0);
                for (int j=1; j < numCols-1; j++) {
                    outline = outline + "\t" + atModel.getValueAt(i,j);
                }
                Color c = (Color)atModel.getValueAt(i,numCols-1);
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
    
    
    void ReadAtypes(InputStream is) throws Exception {        
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1024);
        StringTokenizer st; 

        String s;  
        
        atModel.clear();

        try {
            while (true) {
                s = r.readLine();
                if (s == null) break;
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
                        
                        AtomType at = new AtomType(name, rootType, an, mass, 
                                                   vdw, covalent, rl, gl, bl);

                        atModel.updateAtomType(at);
                        
                    } else {
                        throw new JmolException("AtomTypeTable.ReadAtypes", 
                                                "Wrong Number of fields");
                    }
                }
            }  // end while
            
            is.close();
            
        }  // end Try
        catch( IOException e) {}
        
    }
        
    public AtomType get(String name) {
        return atModel.get(name);
    }
    public AtomType get(int atomicNumber) {
        return atModel.get(atomicNumber);
    }
    
    public synchronized Enumeration elements() {
        return atModel.elements();
    }
}

