/*
 * @(#)ImageTyper.java    1.0 99/01/21
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
import javax.swing.event.*;
import java.beans.*;
import java.awt.*;
import java.io.File;

public class ImageTyper extends JPanel {
    
    private String[] Choices = {"GIF", "JPEG", "PPM"};
    private int def = 0;
    private String result = Choices[def];
    private int JpegQuality;
    private JSlider qSlider;
    private JComboBox cb;

    /**
     * A simple panel with a combo box for allowing the user to choose
     * the input file type.
     * 
     * @param fc the file chooser
     */
    public ImageTyper(JFileChooser fc) {
        
        setLayout(new BorderLayout());

        JPanel cbPanel = new JPanel();
        cbPanel.setLayout(new FlowLayout());
        cbPanel.setBorder(new TitledBorder("Image Type"));       
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
                if (result.equals("JPEG")) 
                    qSlider.setEnabled(true);
                else
                    qSlider.setEnabled(false);                
            }
        });

        add(cbPanel, BorderLayout.NORTH);

        JPanel qPanel = new JPanel();
        qPanel.setLayout(new BorderLayout());
        qPanel.setBorder(new TitledBorder("JPEG Quality"));      
        qSlider = new JSlider(JSlider.HORIZONTAL, 0, 10, 8);
        qSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        qSlider.setPaintTicks(true);
        qSlider.setMajorTickSpacing(1);
        qSlider.setPaintLabels(true);
        qSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                JpegQuality = source.getValue();
            }
        });
        // by default, disabled.  Can be turned on with choice of JPEG.
        qSlider.setEnabled(false);
        qPanel.add(qSlider,BorderLayout.SOUTH);        
        add(qPanel, BorderLayout.SOUTH);                           
    }

    /**
     * returns the file type which contains the user's choice 
     */
    public String getType() {
        return result;
    }

    /**
     * returns the quality (on a scale from 0 to 10) of the JPEG 
     * image that is to be generated.  Returns -1 if choice was not JPEG.
     */
    public int getQuality() {
        int qual = -1;
        if (result.equals("JPEG")) 
            qual = JpegQuality;
        return qual;
    }

    /**
     * removes the GIF entry for those times when we have > 8 bit images
     */
    public void disableGIF() {
        
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (((String)cb.getItemAt(i)).equals("GIF")) {
                cb.removeItemAt(i);
            }
        }
    }
                
        
}
