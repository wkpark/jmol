/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2003  The Jmol Development Team
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
package org.openscience.jmol.ui;

import org.openscience.jmol.viewer.*;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.BitSet;

public class JmolPopupSwing extends JmolPopup {

  JPopupMenu swingPopup;
  CheckboxMenuItemListener cmil;
  JMenu elementComputedMenu;

  public JmolPopupSwing(JmolViewer viewer) {
    super(viewer);
    swingPopup = new JPopupMenu("Jmol");
    cmil = new CheckboxMenuItemListener();
    build(swingPopup);
  }

  public void show(int x, int y) {
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements(); ) {
      String key = (String)keys.nextElement();
      JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)htCheckbox.get(key);
      boolean b = viewer.getBooleanProperty(key);
      //System.out.println("found:" + key + " & it is:" + b);
      jcbmi.setState(b);
    }
    swingPopup.show(jmolComponent, x, y);
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      //System.out.println("CheckboxMenuItemListener() " + e.getSource());
      JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)e.getSource();
      viewer.setBooleanProperty(jcmi.getActionCommand(), jcmi.getState());
    }
  }

  void addToMenu(Object menu, JComponent item) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).add(item);
    else if (menu instanceof JMenu)
      ((JMenu)menu).add(item);
    else
      System.out.println("cannot add object to menu:" + menu);
  }

  ////////////////////////////////////////////////////////////////

  void addMenuSeparator() {
    swingPopup.addSeparator();
  }
  
  void addMenuSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).addSeparator();
    else
      ((JMenu)menu).addSeparator();
  }

  void addMenuItem(String entry) {
    swingPopup.add(new JMenuItem(entry));
  }

  void addMenuItem(Object menu, String entry, String script) {
    JMenuItem jmi = new JMenuItem(entry);
    jmi.addActionListener(mil);
    jmi.setActionCommand(script);
    addToMenu(menu, jmi);
  }

  void addCheckboxMenuItem(Object menu, String entry, String basename) {
    JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
    jcmi.addItemListener(cmil);
    jcmi.setActionCommand(basename);
    addToMenu(menu, jcmi);
    rememberCheckbox(basename, jcmi);
  }

  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (JMenu)subMenu);
  }

  Object newMenu(String menuName) {
    return new JMenu(menuName);
  }

  Object newComputedMenu(String key, String word) {
    if ("elementComputedMenu".equals(key)) {
      elementComputedMenu = new JMenu(word);
      return elementComputedMenu;
    }
    return new JMenu("unrecognized ComputedMenu:" + key);
  }

  void removeAll(Object menu) {
    ((JMenu)menu).removeAll();
  }

}
