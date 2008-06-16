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
  
  static int MENUITEM_HEIGHT = 20; 

  public JmolPopupSwing(JmolViewer viewer) {
    super(viewer);
    swingPopup = new JPopupMenu("Jmol");
    build(swingPopup);
  }

  void showPopupMenu(int x, int y) {
    if (jmolComponent == null)
      return;
    try {
      swingPopup.show(jmolComponent, x, y);
    } catch (Exception e) {
      // probably a permissions problem in Java 7
    }
  }

  Object getParent(Object menu) {
    return ((JMenu)menu).getParent();  
  }
  
  int getMenuItemHeight() {
    return MENUITEM_HEIGHT;
  }

  int getPosition(Object menu) {
    Object p = getParent(menu);
    if (p instanceof JPopupMenu) {
      for (int i = ((JPopupMenu) p).getComponentCount(); --i >= 0;)
        if (((JPopupMenu) p).getComponent(i) == menu)
          return i;
    } else {
      for (int i = ((JMenu) p).getItemCount(); --i >= 0;)
        if (((JMenu) p).getItem(i) == menu)
          return i;
    }
    return -1;
  }

  void insertMenuSubMenu(Object menu, Object subMenu, int index) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).insert((JMenu)subMenu, index);
    else
   ((JMenu)menu).insert((JMenu)subMenu, index);
  }
  
  void createFrankPopup() {
    frankPopup = new JPopupMenu("Frank");
  }
  
  void showFrankMenu(int x, int y) {
    if (jmolComponent == null)
      return;
    try {
      ((JPopupMenu)frankPopup).show(jmolComponent, x, y);
    } catch (Exception e) {
      // probably a permissions problem in Java 7
    }
  }

  void resetFrankMenu() {
    JPopupMenu menu = (JPopupMenu) frankPopup;
    menu.removeAll();
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
    jmi.setName(id == null ? ((Component)menu).getName() + ".": id);
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
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename, String id, boolean state) {
    JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
    jcmi.setState(state);
    jcmi.addItemListener(cmil);
    jcmi.setActionCommand(basename);
    jcmi.setName(id == null ? ((Component)menu).getName() + "." : id);
    addToMenu(menu, jcmi);
    return jcmi;
  }

  Object cloneMenu(Object menu) {
    return null;  
  }
  
  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (JMenu)subMenu);
  }

  Object newMenu(String menuName, String id) {
    JMenu jm = new JMenu(menuName);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  void setAutoscrolls(Object menu) {
   ((JMenu) menu).setAutoscrolls(true);  
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
    try{
      ((JMenu)menu).setEnabled(enable);
    } 
    catch(Exception e) {
      //no menu item;
    }
  }

  void enableMenuItem(Object item, boolean enable) {
    try{
      ((JMenuItem)item).setEnabled(enable);
    } 
    catch(Exception e) {
      //no menu item;
    }
  }

  long maxMemoryForNewerJvm() {
    return Runtime.getRuntime().maxMemory();
  }

  int availableProcessorsForNewerJvm() {
    return Runtime.getRuntime().availableProcessors();
  }

  String getMenuCurrent() {
    StringBuffer sb = new StringBuffer();
    JPopupMenu main = (JPopupMenu) htMenus.get("popupMenu");
    getMenuCurrent(sb, 0, main, "PopupMenu");
    return sb.toString();
  }

  private static void getMenuCurrent(StringBuffer sb, int level, JPopupMenu menu, String menuName) {
    String name = menuName;
    Component[] subMenus = ((JPopupMenu) menu).getComponents();
    for (int i = 0; i < subMenus.length; i++) {
      Object m = subMenus[i];
      String flags;
      if (m instanceof JMenu) {
        JMenu jm = (JMenu) m;
        name = jm.getName();
        flags = "enabled:" + jm.isEnabled();
        addCurrentItem(sb, 'M', level, name, jm.getText(), null, flags);
        getMenuCurrent(sb, level + 1, ((JMenu) m).getPopupMenu(), name);
      } else if (m instanceof JMenuItem) {
        JMenuItem jmi = (JMenuItem) m;
        flags = "enabled:" + jmi.isEnabled();
        if (m instanceof JCheckBoxMenuItem) 
          flags += ";checked:" + ((JCheckBoxMenuItem)m).getState();
        String script = fixScript(jmi.getName(), jmi.getActionCommand());
        addCurrentItem(sb, 'I', level, jmi.getName(), jmi.getText(), script, flags);
      } else {
        addCurrentItem(sb, 'S', level, name, null, null, null);
      }
    }
  }
}
