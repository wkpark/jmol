/*
 * @(#)FileTyper.java    1.1 99/02/01
 *
 * Copyright (c) 1999 J. Daniel Gezelter. All Rights Reserved.
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

import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.beans.*;
import java.awt.*;
import java.io.File;

public class FileTyper extends JPanel implements PropertyChangeListener {
    
    File f = null;
    private JComboBox cb;
    private JRadioButton sYes, sNo;
    private boolean GaussianComputeShifts;
    private static boolean UseFileExtensions = true;
    private static JmolResourceHandler jrh;

    static {
        jrh = new JmolResourceHandler("FileTyper");
    }
    private String[] Choices = {jrh.getString("Automatic"),
                                jrh.getString("XYZ"), 
                                jrh.getString("PDB"), 
                                jrh.getString("Gaussian"),
                                jrh.getString("CML")};
    // Default is the first one:
    private int def = 0;
    private String result = Choices[def];

    /**
     * Should we use the file extension to set the file type????
     *
     * @param ufe boolean controlling the behavior of this component
     */
    public static void setUseFileExtensions(boolean ufe) {
        UseFileExtensions = ufe;
    }

    /**
     * Are we using the file extension to set the file type????
     */    
    public static boolean getUseFileExtensions() {
        return UseFileExtensions;
    }

    /**
     * A simple panel with a combo box for allowing the user to choose
     * the input file type.
     * 
     * @param fc the file chooser
     */
    public FileTyper(JFileChooser fc) {
 
        setLayout(new BorderLayout());

        JPanel cbPanel = new JPanel();
        cbPanel.setLayout(new FlowLayout());
        cbPanel.setBorder(new TitledBorder(jrh.getString("Title")));
        cb = new JComboBox();
        for (int i = 0; i < Choices.length; i++) {
            cb.addItem(Choices[i]);
        }        
        cbPanel.add(cb);
        cb.setSelectedIndex(def);
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox source = (JComboBox)e.getSource();
                result = (String)source.getSelectedItem();
                if (result.equals(jrh.getString("Gaussian"))) {
                    sYes.setEnabled(true);
                    sNo.setEnabled(true);
                } else {
                    sYes.setEnabled(false);
                    sNo.setEnabled(false);
                }
            }
        });
        add(cbPanel,BorderLayout.CENTER); // Change to NORTH if other controls
        
        JPanel sPanel = new JPanel();
        sPanel.setLayout(new BoxLayout(sPanel, BoxLayout.Y_AXIS));
        sPanel.setBorder(new TitledBorder(jrh.getString("ShieldTitle")));
        ButtonGroup sGroup = new ButtonGroup();
        sYes = new JRadioButton(jrh.getString("sYesLabel"));
        sNo = new JRadioButton(jrh.getString("sNoLabel"));
        sYes.addItemListener(rbListener);
        sNo.addItemListener(rbListener);
        sGroup.add(sYes);
        sGroup.add(sNo);
        sPanel.add(sYes);
        sPanel.add(sNo);
        sNo.setSelected(true);
        sYes.setEnabled(false);
        sNo.setEnabled(false);
        add(sPanel,BorderLayout.SOUTH);

        fc.addPropertyChangeListener(this);
    }
    
    ItemListener rbListener = new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
            JRadioButton rb = (JRadioButton) e.getSource();
            if(rb.getText().equals(jrh.getString("sYesLabel"))) { 
                GaussianComputeShifts = false;
            } else if(rb.getText().equals(jrh.getString("sNoLabel"))) {
                GaussianComputeShifts = true;
            }
        }
    };
    
    /**
     * returns the file type which contains the user's choice 
     */
    public String getType() {
        return result;
    }

    /**
     * If the selected file is a Gaussian log file, should we compute
     * Chemical Shifts?  Or should we leave the raw shielding values?
     */
    public boolean computeShifts() {
        boolean c = false;
        if (result.equals(jrh.getString("Gaussian"))) {
            c = GaussianComputeShifts;
        }
        return c;
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if(prop == JFileChooser.SELECTED_FILE_CHANGED_PROPERTY) {
            f = (File) e.getNewValue();
            String fname = f.toString().toLowerCase();
            System.out.println(fname);
            if (UseFileExtensions) {
                if (fname.endsWith("xyz")) {
                    cb.setSelectedIndex(1); 
                } else if (fname.endsWith("pdb")) {
                    cb.setSelectedIndex(2);
                } else if (fname.endsWith("log")) {
                    cb.setSelectedIndex(3);
                } else if (fname.endsWith("out")) {
                    cb.setSelectedIndex(0);
                } else if (fname.endsWith("cml")) {
                    cb.setSelectedIndex(4);
                } 
            }            
        }
    }
}

