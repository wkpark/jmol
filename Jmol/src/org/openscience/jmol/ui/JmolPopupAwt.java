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

import org.jmol.viewer.*;

import java.awt.Component;
import java.awt.PopupMenu;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.CheckboxMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.BitSet;

// mth 2003 05 27
// This class is built with awt instead of swing so that it will
// operate as an applet with old JVMs

public class JmolPopupAwt extends JmolPopup {

  PopupMenu awtPopup;
  CheckboxMenuItemListener cmil;
  Menu elementComputedMenu;

  public JmolPopupAwt(JmolViewer viewer) {
    super(viewer);
    awtPopup = new PopupMenu("Jmol");
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
    jmolComponent.add(awtPopup);
    build(awtPopup);
  }

  public void show(int x, int y) {
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements(); ) {
      String key = (String)keys.nextElement();
      CheckboxMenuItem cbmi = (CheckboxMenuItem)htCheckbox.get(key);
      boolean b = viewer.getBooleanProperty(key);
      cbmi.setState(b);
    }
    awtPopup.show(jmolComponent, x, y);
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      CheckboxMenuItem cmi = (CheckboxMenuItem)e.getSource();
      viewer.setBooleanProperty(cmi.getActionCommand(), cmi.getState());
    }
  }

  /*
  void addMenuItems(String key, Menu menu) {
    String value = getValue(key);
    if (value == null) {
      MenuItem mi = new MenuItem("#" + key);
      menu.add(mi);
      return;
    }
    StringTokenizer st = new StringTokenizer(getValue(key));
    while (st.hasMoreTokens()) {
      String item = st.nextToken();
      if (item.endsWith("Menu")) {
        String word = getWord(item);
        Menu subMenu = new Menu(word);
        addMenuItems(item, subMenu);
        menu.add(subMenu);
      } else if (item.equals("-")) {
        menu.addSeparator();
      } else {
        String word = getWord(item);
        MenuItem mi;
        if (item.endsWith("Checkbox")) {
          CheckboxMenuItem cmi = new CheckboxMenuItem(word);
          String basename = item.substring(0, item.length() - 8);
          cmi.addItemListener(cmil);
          rememberCheckbox(basename, cmi);
          cmi.setActionCommand(basename);
          mi = cmi;
        } else {
          mi = new MenuItem(word);
          getValue(item);
          mi.addActionListener(mil);
          mi.setActionCommand(item);
        }
        menu.add(mi);
      }
    }
  }

  void addVersionAndDate() {
    addSeparator();
    MenuItem mi = new MenuItem("Jmol " + JmolConstants.version);
    add(mi);
    mi = new MenuItem(JmolConstants.date);
    add(mi);
  }
  */

  void addToMenu(Object menu, MenuItem item) {
    ((Menu)menu).add(item);
  }

  ////////////////////////////////////////////////////////////////

  void addMenuSeparator() {
    awtPopup.addSeparator();
  }
  
  void addMenuSeparator(Object menu) {
    ((Menu)menu).addSeparator();
  }

  void addMenuItem(String entry) {
    awtPopup.add(new MenuItem(entry));
  }

  void addMenuItem(Object menu, String entry, String script) {
    MenuItem mi = new MenuItem(entry);
    mi.addActionListener(mil);
    mi.setActionCommand(script);
    addToMenu(menu, mi);
  }

  void addCheckboxMenuItem(Object menu, String entry, String basename) {
    CheckboxMenuItem cmi = new CheckboxMenuItem(entry);
    cmi.addItemListener(cmil);
    cmi.setActionCommand(basename);
    addToMenu(menu, cmi);
    rememberCheckbox(basename, cmi);
  }

  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (Menu)subMenu);
  }

  Object newMenu(String menuName) {
    return new Menu(menuName);
  }

  Object newComputedMenu(String key, String word) {
    if ("elementComputedMenu".equals(key)) {
      elementComputedMenu = new Menu(word);
      return elementComputedMenu;
    }
    return new Menu("unrecognized ComputedMenu:" + key);
  }

  void removeAll(Object menu) {
    ((Menu)menu).removeAll();
  }
}
