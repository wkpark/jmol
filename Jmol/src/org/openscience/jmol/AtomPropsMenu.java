/*
 * @(#)AtomPropsMenu.java    1.0 99/02/03
 *
 * Copyright (c) 1999 J. Daniel Gezelter All Rights Reserved.
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

import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


/**
 * Extends the basic popup menu so that we can modify it in response
 * to new physical properties in a chemical model
 */
public class AtomPropsMenu extends JMenu {

    ButtonGroup bg;
    Vector list = new Vector();
    DisplaySettings settings;

    public AtomPropsMenu(String name, DisplaySettings settings) {
        super(name);
		this.settings = settings;
        bg = new ButtonGroup();
        JRadioButtonMenuItem mi = new JRadioButtonMenuItem("None");
        list.addElement(mi);
        add(mi);
        bg.add(mi);
        mi.setSelected(true);
        mi.addItemListener(propsListener);
    }
    
    public void replaceList(Vector propsList) {
        JRadioButtonMenuItem mi = (JRadioButtonMenuItem)list.elementAt(0);
        mi.setSelected(true);

        for (int i = list.size()-1; i > 0; i--) {
            JRadioButtonMenuItem mi2 = (JRadioButtonMenuItem)list.elementAt(i);
            mi2.removeItemListener(propsListener);
            bg.remove(mi2);
            remove(mi2);
            list.removeElementAt(i);
        }

        for (int i = 0; i < propsList.size(); i++) {
            JRadioButtonMenuItem mi3 = new JRadioButtonMenuItem((String) propsList.elementAt(i));
            list.addElement(mi3);
            add(mi3);
            bg.add(mi3);
            mi3.addItemListener(propsListener);
        }
    }
    
    ItemListener propsListener = new ItemListener() {
        Component c;
        AbstractButton b;
        
        public void itemStateChanged(ItemEvent e) {
            JRadioButtonMenuItem rbmi = (JRadioButtonMenuItem) e.getSource();
            String mode = rbmi.getText();
            if(mode.equals("None")) 
                settings.setPropertyMode("");
            else if (mode.equals("Vector")) {
                settings.setPropertyMode(""); 
                settings.setShowVectors(true);
            } else
                settings.setPropertyMode(rbmi.getText());            
            Jmol.display.repaint();
        }
    };
    
}
