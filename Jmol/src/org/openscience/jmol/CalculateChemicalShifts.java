
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

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 */
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
        URL url = this.getClass().getClassLoader().getResource(RF);

        // URL url = ClassLoader.getSystemResource(RF);
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
        Enumeration k = shieldings.keys();
        while (k.hasMoreElements()) {
          shieldingNames[i] = (String) k.nextElement();
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

    boolean foundShielding = false;
    for (int frameIndex = 0;
        !foundShielding && (frameIndex < chemFile.getNumberFrames());
          ++frameIndex) {
      ChemFrame frame = chemFile.getFrame(frameIndex);
      for (int atomIndex = 0;
          !foundShielding && (atomIndex < frame.getNumberOfAtoms());
            ++atomIndex) {
        Atom atom = frame.getAtomAt(atomIndex);
        Vector properties = atom.getProperties();
        for (int propertyIndex = 0;
            !foundShielding && (propertyIndex < properties.size());
              ++propertyIndex) {
          if (properties.elementAt(propertyIndex) instanceof NMRShielding) {
            foundShielding = true;
          }
        }
      }
    }
    setEnabled(foundShielding);
  }

  public void actionPerformed(ActionEvent e) {

    dialog.show();
    SharcShielding referenceShielding =
      (SharcShielding) shieldings.get(dialog.getValue());

    if (referenceShielding == null) {
      return;
    }
    for (int f = 0; f < chemFile.getNumberFrames(); ++f) {
      ChemFrame frame = chemFile.getFrame(f);
      for (int i = 0; i < frame.getNumberOfAtoms(); ++i) {
        String element = frame.getAtomAt(i).getType().getName();
        Vector properties = frame.getAtomAt(i).getProperties();
        Enumeration propIter = properties.elements();
        while (propIter.hasMoreElements()) {
          Object prop = propIter.nextElement();
          if (prop instanceof NMRShielding) {
            NMRShielding shield1 = (NMRShielding) prop;

            double value = ((Double) shield1.getProperty()).doubleValue();
            value -= referenceShielding.getShielding(element);

            NMRShielding newShield = new NMRShielding(value);
            newShield.descriptor = propertyLabel;
            frame.getAtomAt(i).addProperty(newShield);
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
