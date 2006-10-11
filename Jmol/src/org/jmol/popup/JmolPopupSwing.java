/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

import org.jmol.api.*;
import org.jmol.util.Logger;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

import java.awt.Component;

public class JmolPopupSwing extends JmolPopup {

  JPopupMenu swingPopup;
  JMenu elementComputedMenu;

  public JmolPopupSwing(JmolViewer viewer) {
    super(viewer);
    swingPopup = new JPopupMenu("Jmol");
    build(swingPopup);
  }

  void showPopupMenu(int x, int y) {
    swingPopup.show(jmolComponent, x, y);
  }

  void addToMenu(Object menu, JComponent item) {
    if (menu instanceof JPopupMenu) {
      ((JPopupMenu)menu).add(item);
    } else if (menu instanceof JMenu) {
      ((JMenu)menu).add(item);
    } else {
      Logger.warn("cannot add object to menu: " + menu);
    }
  }

  ////////////////////////////////////////////////////////////////

  void addMenuSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).addSeparator();
    else
      ((JMenu)menu).addSeparator();
  }

  Object addMenuItem(Object menu, String entry, String script, String id) {
    JMenuItem jmi = new JMenuItem(entry);
    updateMenuItem(jmi, entry, script);
    jmi.addActionListener(mil);
    jmi.setName(id == null ? ((Component)menu).getName() : id);
    addToMenu(menu, jmi);
    return jmi;
  }

  void setLabel(Object menu, String entry) {
    if (menu instanceof JMenuItem)
      ((JMenuItem) menu).setLabel(entry);
    else
      ((JMenu) menu).setLabel(entry);
  }
  
  String getId(Object menu) {
    return ((Component) menu).getName();
  }
  
  void setCheckBoxValue(Object source) {
    JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem) source;
    setCheckBoxValue(jcmi.getActionCommand(), jcmi.getState());
  }

  void setCheckBoxState(Object item, boolean state) {
    ((JCheckBoxMenuItem) item).setState(state);
  }
 
  void updateMenuItem(Object menuItem, String entry, String script) {
    JMenuItem jmi = (JMenuItem)menuItem;
    jmi.setLabel(entry);
    jmi.setActionCommand(script);
    // miguel 2004 12 03
    // greyed out menu entries are too hard to read
    //    jmi.setEnabled(script != null);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename, String id, boolean state) {
    JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
    jcmi.setState(state);
    jcmi.addItemListener(cmil);
    jcmi.setActionCommand(basename);
    jcmi.setName(id == null ? ((Component)menu).getName() : id);
    addToMenu(menu, jcmi);
    return jcmi;
  }

  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (JMenu)subMenu);
  }

  Object newMenu(String menuName, String id) {
    JMenu jm = new JMenu(menuName);
    jm.setName(id);
    return jm;
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

  int getMenuItemCount(Object menu) {
    return ((JMenu)menu).getItemCount();
  }

  void removeMenuItem(Object menu, int index) {
    ((JMenu)menu).remove(index);
  }

  void removeAll(Object menu) {
    ((JMenu)menu).removeAll();
  }

  void enableMenu(Object menu, boolean enable) {
    if (menu instanceof JMenuItem) {
      enableMenuItem(menu, enable);
      return;
    }
    ((JMenu)menu).setEnabled(enable);
  }

  void enableMenuItem(Object item, boolean enable) {
    ((JMenuItem)item).setEnabled(enable);
  }

  long maxMemoryForNewerJvm() {
    return Runtime.getRuntime().maxMemory();
  }

  int availableProcessorsForNewerJvm() {
    return Runtime.getRuntime().availableProcessors();
  }  
}
