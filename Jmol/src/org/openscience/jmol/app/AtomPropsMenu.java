/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
package org.openscience.jmol.app;

import org.openscience.jmol.*;

import java.util.Vector;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JMenu;
import javax.swing.ButtonGroup;


/**
 * Extends the basic popup menu so that we can modify it in response
 * to new physical properties in a chemical model
 */
public class AtomPropsMenu extends JMenu {

  ButtonGroup bg;
  Vector list = new Vector();
  DisplayControl control;

  public AtomPropsMenu(String name, DisplayControl control) {

    super(name);
    this.control = control;
    bg = new ButtonGroup();
    JRadioButtonMenuItem mi = new JRadioButtonMenuItem("None");
    list.addElement(mi);
    add(mi);
    bg.add(mi);
    mi.setSelected(true);
    mi.addItemListener(propsListener);
  }

  public void replaceList(Vector propsList) {

    JRadioButtonMenuItem mi = (JRadioButtonMenuItem) list.elementAt(0);
    mi.setSelected(true);

    for (int i = list.size() - 1; i > 0; i--) {
      JRadioButtonMenuItem mi2 = (JRadioButtonMenuItem) list.elementAt(i);
      mi2.removeItemListener(propsListener);
      bg.remove(mi2);
      remove(mi2);
      list.removeElementAt(i);
    }

    for (int i = 0; i < propsList.size(); i++) {
      String propertyName = (String) propsList.elementAt(i);
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
      if (mode.equals("None")) {
        control.setPropertyStyleString("");
      } else {
        control.setPropertyStyleString(rbmi.getText());
      }
    }
  };

}
