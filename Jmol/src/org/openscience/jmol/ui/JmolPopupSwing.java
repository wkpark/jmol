/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2004  The Jmol Development Team
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

import org.jmol.api.*;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;

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

  void addMenuSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).addSeparator();
    else
      ((JMenu)menu).addSeparator();
  }

  Object addMenuItem(Object menu, String entry, String script) {
    JMenuItem jmi = new JMenuItem(entry);
    updateMenuItem(jmi, entry, script);
    jmi.addActionListener(mil);
    addToMenu(menu, jmi);
    return jmi;
  }

  void updateMenuItem(Object menuItem, String entry, String script) {
    JMenuItem jmi = (JMenuItem)menuItem;
    jmi.setLabel(entry);
    jmi.setActionCommand(script);
    // miguel 2004 12 03
    // greyed out menu entries are too hard to read
    //    jmi.setEnabled(script != null);
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

  void renameMenu(Object menu, String newMenuName) {
    ((JMenu)menu).setLabel(newMenuName);
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

  void enableMenu(Object menu, boolean enable) {
    ((JMenu)menu).setEnabled(enable);
  }


}
