
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
			String propertyName = (String) propsList.elementAt(i);
			if (propertyName.equals("Vector")) {
				// Ignore vector properties because a special checkbox menu
				// item already exists in Display menu.
				continue;
			}
            JRadioButtonMenuItem mi3 = new JRadioButtonMenuItem(propertyName);
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
                if(mode.equals("None")) {
                    settings.setPropertyMode("");
                } else
                    settings.setPropertyMode(rbmi.getText());            
                Jmol.display.repaint();
            }
        };
    
}
