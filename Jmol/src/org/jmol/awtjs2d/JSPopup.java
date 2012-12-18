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
package org.jmol.awtjs2d;

import org.jmol.i18n.GT;
import org.jmol.popup.GenericPopup;
import org.jmol.popup.PopupResource;
import org.jmol.util.Elements;
import org.jmol.util.Logger;
import org.jmol.util.StringXBuilder;
import org.jmol.viewer.Viewer;



/**
 * all popup-related awt/swing class references are in this file.
 */
abstract public class JSPopup extends GenericPopup {

  
  @Override
  public void finalize() {
    System.out.println("SwingPopup Finalize " + this);
  }
  
  //private MenuItemListener mil;
  //private CheckboxMenuItemListener cmil;
  //private MenuMouseListener mfl;

  public JSPopup() {
    // required by reflection
  }

  /**
   * update the button depending upon its type
   * 
   * @param b
   * @param entry
   * @param script
   */
  private void updateButton(Object b, String entry, String script) {
    String[] ret = new String[] { entry };    
    Object icon = getEntryIcon(ret);
    entry = ret[0];
//    if (icon != null)
//      b.setIcon((ImageIcon) icon);
//    if (entry != null)
//      b.setText(entry);
//    if (script != null)
//      b.setActionCommand(script);
  }

  ////////////////////////////////////////////////////////////////
  
//  class MenuItemListener implements ActionListener {
//    public void actionPerformed(ActionEvent e) {
//      checkMenuClick(e.getSource(), e.getActionCommand());
//    }
//  }

//  class MenuMouseListener implements MouseListener {
//
//    public void mouseClicked(MouseEvent e) {
//    }
//
//    public void mouseEntered(MouseEvent e) {
//      checkMenuFocus(e.getSource(), true);
//    }
//
//    public void mouseExited(MouseEvent e) {
//      checkMenuFocus(e.getSource(), false);
//    }
//
//    public void mousePressed(MouseEvent e) {
//    }
//
//    public void mouseReleased(MouseEvent e) {
//    }
//
//  }

//  class CheckboxMenuItemListener implements ItemListener {
//    public void itemStateChanged(ItemEvent e) {
//      restorePopupMenu();
//      setCheckBoxValue(e.getSource());
//      String id = getId(e.getSource());
//      if (id != null) {
//        currentMenuItemId = id;
//      }
//      //Logger.debug("CheckboxMenuItemListener() " + e.getSource());
//    }
//  }

  private Object getEntryIcon(String[] ret) {
    return null;
  }

  //////////////// JmolPopup methods ///////////
    
  private void checkMenuFocus(Object source, boolean isFocus) {
//    if (source instanceof JMenuItem) {
//      String name = ((JMenuItem) source).getName();
//      if (name.indexOf("Focus") < 0)
//        return;
//      if (isFocus) {
//        viewer.script("selectionHalos ON;" + ((JMenuItem) source).getActionCommand());
//      } else {
//        viewer.script("selectionHalos OFF");
//      }
//    }
  }

  /// JmolAbstractMenu ///

  public void menuAddButtonGroup(Object newMenu) {
//    if (buttonGroup == null)
//      buttonGroup = new ButtonGroup();
//    ((ButtonGroup) buttonGroup).add((JMenuItem) newMenu);
  }

  public void menuAddItem(Object menu, Object item) {
//    if (menu instanceof JPopupMenu) {
//      ((JPopupMenu) menu).add((JComponent) item);
//    } else if (menu instanceof JMenu) {
//      ((JMenu) menu).add((JComponent) item);
//    } else {
//      Logger.warn("cannot add object to menu: " + menu);
//    }
  }

  public void menuAddSeparator(Object menu) {
//    if (menu instanceof JPopupMenu)
//      ((JPopupMenu) menu).addSeparator();
//    else
//      ((JMenu) menu).addSeparator();
  }

  public void menuAddSubMenu(Object menu, Object subMenu) {
    menuAddItem(menu, subMenu);
  }

  public void menuClearListeners(Object menu) {
    // TODO
    
  }
  
  public Object menuCreateCheckboxItem(Object menu, String entry,
                                       String basename, String id,
                                       boolean state, boolean isRadio) {
//    JMenuItem jm;
//    if (isRadio) {
//      JRadioButtonMenuItem jr = new JRadioButtonMenuItem(entry);
//      jm = jr;
//      jr.setArmed(state);
//    } else {
//      JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
//      jm = jcmi;
//      jcmi.setState(state);
//    }
//    jm.setSelected(state);
//    jm.addItemListener(cmil);
//    jm.setActionCommand(basename);
//    updateButton(jm, entry, basename);
//    if (id != null && id.startsWith("Focus")) {
//      jm.addMouseListener(mfl);
//      id = ((Component) menu).getName() + "." + id;
//    }
//    jm.setName(id == null ? ((Component) menu).getName() + "." : id);
//    menuAddItem(menu, jm);
//    return jm;
    return null;
  }

  public Object menuCreateItem(Object menu, String entry, String script,
                               String id) {
//    JMenuItem jmi = new JMenuItem(entry);
//    updateButton(jmi, entry, script);
//    jmi.addActionListener(mil);
//    if (id != null && id.startsWith("Focus")) {
//      jmi.addMouseListener(mfl);
//      id = ((Component) menu).getName() + "." + id;
//    }
//    jmi.setName(id == null ? ((Component) menu).getName() + "." : id);
//    menuAddItem(menu, jmi);
//    return jmi;
    return null;
  }

  public Object menuCreatePopup(String name) {
    return null;//new JPopupMenu(name);
  }

  public void menuEnable(Object menu, boolean enable) {
//    if (menu instanceof JMenuItem) {
//      menuEnableItem(menu, enable);
//      return;
//    }
//    try {
//      ((JMenu) menu).setEnabled(enable);
//    } catch (Exception e) {
//      //no menu item;
//    }
  }

  public void menuEnableItem(Object item, boolean enable) {
//    try {
//      ((JMenuItem) item).setEnabled(enable);
//    } catch (Exception e) {
//      //no menu item;
//    }
  }

  public void menuGetAsText(StringXBuilder sb, int level, Object menu,
                            String menuName) {
//    String name = menuName;
//    Component[] subMenus = (menu instanceof JPopupMenu ? ((JPopupMenu) menu)
//        .getComponents() : ((JMenu) menu).getPopupMenu().getComponents());
//    for (int i = 0; i < subMenus.length; i++) {
//      Object m = subMenus[i];
//      String flags;
//      if (m instanceof JMenu) {
//        JMenu jm = (JMenu) m;
//        name = jm.getName();
//        flags = "enabled:" + jm.isEnabled();
//        addItemText(sb, 'M', level, name, jm.getText(), null, flags);
//        menuGetAsText(sb, level + 1, ((JMenu) m).getPopupMenu(), name);
//      } else if (m instanceof JMenuItem) {
//        JMenuItem jmi = (JMenuItem) m;
//        flags = "enabled:" + jmi.isEnabled();
//        if (m instanceof JCheckBoxMenuItem)
//          flags += ";checked:" + ((JCheckBoxMenuItem) m).getState();
//        String script = fixScript(jmi.getName(), jmi.getActionCommand());
//        addItemText(sb, 'I', level, jmi.getName(), jmi.getText(), script, flags);
//      } else {
//        addItemText(sb, 'S', level, name, null, null, null);
//      }
//    }
  }

  public String menuGetId(Object menu) {
    return null;//((Component) menu).getName();
  }

  public int menuGetItemCount(Object menu) {
    return 0;//((JMenu) menu).getItemCount();
  }

  public Object menuGetParent(Object menu) {
    return null;//((JMenu) menu).getParent();
  }

  public int menuGetPosition(Object menu) {
//    Object p = menuGetParent(menu);
//    if (p instanceof JPopupMenu) {
//      for (int i = ((JPopupMenu) p).getComponentCount(); --i >= 0;)
//        if (((JPopupMenu) p).getComponent(i) == menu)
//          return i;
//    } else {
//      for (int i = ((JMenu) p).getItemCount(); --i >= 0;)
//        if (((JMenu) p).getItem(i) == menu)
//          return i;
//    }
    return -1;
  }

  public void menuInsertSubMenu(Object menu, Object subMenu, int index) {
//    if (menu instanceof JPopupMenu)
//      ((JPopupMenu) menu).insert((JMenu) subMenu, index);
//    else
//      ((JMenu) menu).insert((JMenu) subMenu, index);
  }

  public Object menuNewEntry(String entry, String id) {
//    JMenu jm = new JMenu(entry);
//    updateButton(jm, entry, null);
//    jm.setName(id);
//    jm.setAutoscrolls(true);
//    return jm;
    return null;
  }

  public void menuRemoveItem(Object menu, int index) {
//    ((JMenu) menu).remove(index);
  }

  public void menuRemoveAll(Object menu) {
//    if (menu instanceof JMenu)
//      ((JMenu) menu).removeAll();
//    else
//      ((JPopupMenu)menu).removeAll();
  }

  public void menuRenameEntry(Object menu, String entry) {
//    ((JMenu) menu).setText(entry);
  }

  public void menuSetAutoscrolls(Object menu) {
//    ((JMenu) menu).setAutoscrolls(true);
  }

  public void menuSetCheckBoxState(Object item, boolean state) {
//    if (item instanceof JCheckBoxMenuItem)
//      ((JCheckBoxMenuItem) item).setState(state);
//    else
//      ((JRadioButtonMenuItem) item).setArmed(state);
//    ((JMenuItem) item).setSelected(state);
  }

  public String menuSetCheckBoxOption(Object item, String name, String what) {
    return null;
  }

  public void menuSetCheckBoxValue(Object source) {
//    JMenuItem jcmi = (JMenuItem) source;
//    setCheckBoxValue(jcmi, jcmi.getActionCommand(), jcmi.isSelected());
  }

  public void menuSetLabel(Object menu, String entry) {
//    if (menu instanceof JMenuItem)
//      ((JMenuItem) menu).setText(entry);
//    else
//      ((JMenu) menu).setText(entry);
  }

  public void menuSetListeners() {
//    mil = new MenuItemListener();
//    cmil = new CheckboxMenuItemListener();
//    mfl = new MenuMouseListener();
  }
  
  public void menuShowPopup(Object popup, int x, int y) {
//    try {
//      ((JPopupMenu)popup).show((Component) viewer.getDisplay(), x, y);
//    } catch (Exception e) {
//      // ignore
//    }
  }

}
