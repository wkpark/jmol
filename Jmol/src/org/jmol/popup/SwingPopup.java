/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-11 15:47:18 -0500 (Tue, 11 May 2010) $
 * $Revision: 13064 $
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

import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

abstract public class SwingPopup extends GenericPopup {

  protected String imagePath;
  
  protected MenuItemListener mil;
  protected CheckboxMenuItemListener cmil;
  protected JPopupMenu swingPopup;

  public SwingPopup() {
    // required by reflection
  }

  @Override
  protected void set(Viewer viewer) {
    super.set(viewer);
  }

  protected void initialize(Viewer viewer, String title, PopupResource bundle,
      boolean isHorizontal) {
    set(viewer);
    swingPopup = new JPopupMenu(title);
    if (isHorizontal) {
      //BoxLayout bl = new BoxLayout(pm, BoxLayout.LINE_AXIS);
      GridLayout bl = new GridLayout(3, 4);
      swingPopup.setLayout(bl);
    }
    build(title, swingPopup, bundle);
  }

  private void updateButton(AbstractButton b, String entry, String script) {
    ImageIcon icon = null;
    if (entry.startsWith("<")) {
      int pt = entry.indexOf(">");
      icon = getIcon(entry.substring(1, pt));
      entry = entry.substring(pt + 1);
    }

    if (icon != null)
      b.setIcon(icon);
    if (entry != null)
      b.setText(entry);
    if (script != null)
      b.setActionCommand(script);
  }

  private ImageIcon getIcon(String name) {
    // for modelkit only
    if (imagePath == null)
      return null;
    String imageName = imagePath + name;
    URL imageUrl = this.getClass().getClassLoader().getResource(imageName);
    if (imageUrl != null) {
      return new ImageIcon(imageUrl);
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////

  @Override
  protected void addButtonGroupItem(Object newMenu) {
    if (group == null)
      group = new ButtonGroup();
    ((ButtonGroup) group).add((JMenuItem) newMenu);
  }

  @Override
  protected Object addCheckboxMenuItem(Object menu, String entry,
                                       String basename, String id,
                                       boolean state, boolean isRadio) {
    JMenuItem jm;
    if (isRadio) {
      JRadioButtonMenuItem jr = new JRadioButtonMenuItem(entry);
      jm = jr;
      jr.setArmed(state);
    } else {
      JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
      jm = jcmi;
      jcmi.setState(state);
    }
    jm.setSelected(state);
    jm.addItemListener(cmil);
    jm.setActionCommand(basename);
    updateButton(jm, entry, basename);
    jm.setName(id == null ? ((Component) menu).getName() + "." : id);
    addToMenu(menu, jm);
    return jm;
  }

  @Override
  protected Object addMenuItem(Object menu, String entry, String script,
                               String id) {
    JMenuItem jmi = new JMenuItem(entry);
    updateButton(jmi, entry, script);
    jmi.addActionListener(mil);
    jmi.setName(id == null ? ((Component) menu).getName() + "." : id);
    addToMenu(menu, jmi);
    return jmi;
  }

  @Override
  protected void addMenuSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu) menu).addSeparator();
    else
      ((JMenu) menu).addSeparator();
  }

  @Override
  protected void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, subMenu);
  }

  @Override
  protected void addToMenu(Object menu, Object item) {
    if (menu instanceof JPopupMenu) {
      ((JPopupMenu) menu).add((JComponent) item);
    } else if (menu instanceof JMenu) {
      ((JMenu) menu).add((JComponent) item);
    } else {
      Logger.warn("cannot add object to menu: " + menu);
    }
  }

  @Override
  protected void enableMenu(Object menu, boolean enable) {
    if (menu instanceof JMenuItem) {
      enableMenuItem(menu, enable);
      return;
    }
    try {
      ((JMenu) menu).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

  @Override
  protected void enableMenuItem(Object item, boolean enable) {
    try {
      ((JMenuItem) item).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

  @Override
  protected String getId(Object menu) {
    return ((Component) menu).getName();
  }

  @Override
  protected int getMenuItemCount(Object menu) {
    return ((JMenu) menu).getItemCount();
  }

  @Override
  protected Object newMenu(String entry, String id) {
    JMenu jm = new JMenu(entry);
    updateButton(jm, entry, null);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  @Override
  protected void removeMenuItem(Object menu, int index) {
    ((JMenu) menu).remove(index);
  }

  @Override
  protected void removeAll(Object menu) {
    ((JMenu) menu).removeAll();
  }

  @Override
  protected void renameMenu(Object menu, String entry) {
    ((JMenu) menu).setText(entry);
  }

  @Override
  protected void setAutoscrolls(Object menu) {
    ((JMenu) menu).setAutoscrolls(true);
  }

  @Override
  protected void setCheckBoxState(Object item, boolean state) {
    if (item instanceof JCheckBoxMenuItem)
      ((JCheckBoxMenuItem) item).setState(state);
    else
      ((JRadioButtonMenuItem) item).setArmed(state);
    ((JMenuItem) item).setSelected(state);
  }

  @Override
  protected void setCheckBoxValue(Object source) {
    JMenuItem jcmi = (JMenuItem) source;
    setCheckBoxValue(jcmi, jcmi.getActionCommand(), jcmi.isSelected());
  }

  @Override
  protected void setLabel(Object menu, String entry) {
    if (menu instanceof JMenuItem)
      ((JMenuItem) menu).setText(entry);
    else
      ((JMenu) menu).setText(entry);
  }

  @Override
  protected void setMenuListeners() {
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();    
  }
  
  @Override
  protected void showPopupMenu(int x, int y) {
    Component display = (Component) viewer.getDisplay();
    if (display == null)
      return;
    try {
      swingPopup.show(display, x, y);
    } catch (Exception e) {
      Logger.error("popup error: " + e.getMessage());
      // browser in Java 1.6.0_10 is blocking setting WindowAlwaysOnTop 

    }
  }

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      checkMenuClick(e.getSource(), e.getActionCommand());
    }
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      restorePopupMenu();
      setCheckBoxValue(e.getSource());
      String id = getId(e.getSource());
      if (id != null) {
        currentMenuItemId = id;
      }
      //Logger.debug("CheckboxMenuItemListener() " + e.getSource());
    }
  }


}
