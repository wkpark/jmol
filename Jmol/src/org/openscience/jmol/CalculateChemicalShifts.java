/*
 * CalculateChemicalShift.java
 *
 * Copyright (C) 2000  Bradley A. Smith
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.openscience.jmol;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.*;
import java.io.*;
import java.net.URL;

class CalculateChemicalShifts extends AbstractAction {
    ChemFile chemFile;
    AtomPropsMenu propertiesMenu;
    Hashtable shieldings;
    SelectSharcReference dialog;
    
    static String RF = "org/openscience/jmol/Data/refs_c4h12si1_data.sharc";
    static final String propertyLabel = "Chemical shift";

    public CalculateChemicalShifts() {
        super("chemicalShifts");
		setEnabled(false);
	}
    
    public void initialize() {
        if (shieldings == null) {
            try {
                URL url = ClassLoader.getSystemResource(RF);
                InputStreamReader isr = new InputStreamReader(url.openStream());
                BufferedReader br = new BufferedReader(isr);
                SharcReader sr1 = new SharcReader(br);
                shieldings = new Hashtable(); 
                while (sr1.hasNext()) {
                    SharcShielding ss1 = sr1.next();
                    shieldings.put(ss1.getMethod(), ss1);
                }
               
                String[] shieldingNames = new String[shieldings.size()];
                int i = 0;
                for (Enumeration k = shieldings.keys();k.hasMoreElements();) {
                    shieldingNames[i] = (String)k.nextElement();
                    i++;
                }
                
                dialog = new SelectSharcReference(null, shieldingNames, true);
            } catch (Exception ex) {
                shieldings = null;
            }
        }
    }
    
	public void setChemFile(ChemFile file, AtomPropsMenu menu) {
		setEnabled(false);

		propertiesMenu = menu;
		chemFile = file;
		if (chemFile == null) {
			return;
		}

		if (shieldings == null) {
			return;
		}
		Vector atomProperties = chemFile.getAtomPropertyList();
		for (int i=0; i < atomProperties.size(); ++i) {
			Object prop = atomProperties.elementAt(i);
			if (prop instanceof String) {
				String propName = (String)prop;
				if (propName.indexOf("Shielding") >= 0
					|| propName.indexOf("shielding") >= 0) {
					setEnabled(true);
					break;
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		dialog.show();
		SharcShielding referenceShielding = (SharcShielding)shieldings.get(dialog.getValue());

		if (referenceShielding == null) {
			return;
		}
		for (int f=0; f < chemFile.nFrames(); ++f) {
			ChemFrame frame  = chemFile.getFrame(f);
			for (int i=0; i < frame.getNvert(); ++i) {
				String element = frame.getAtomType(i).getBaseAtomType().getName();
				Vector properties = frame.getVertProps(i);
				Enumeration propIter = properties.elements();
				while (propIter.hasMoreElements()) {
					Object prop = propIter.nextElement();
					if (prop instanceof NMRShielding) {
						NMRShielding shield1 = (NMRShielding)prop;

						double value = ((Double)shield1.getProperty()).doubleValue();
						value -= referenceShielding.getShielding(element);

						NMRShielding newShield = new NMRShielding(value);
						newShield.descriptor = propertyLabel;
						frame.addProperty(i, newShield);
						break;
					}
				}
			}
		}
 
		Vector filePL = chemFile.getAtomPropertyList();
		if (filePL.indexOf(propertyLabel) < 0) {
			filePL.addElement(propertyLabel);
		}

		propertiesMenu.replaceList(chemFile.getAtomPropertyList());
	}
}
