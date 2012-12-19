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

import org.jmol.popup.GenericPopup;
import org.jmol.util.StringXBuilder;


/**
 * all popup-related awt/swing class references are in this file.
 * 
 * 
 */
abstract public class JSPopup extends GenericPopup {

  //TODO: jQuery menu actions, entry, and exit need to come back here
  //      to execute checkMenuClick, checkMenuFocus, and checkBoxStateChanged
  
  //  (on mouse up)       checkMenuClick(e.getSource(), e.getSource().getActionCommand());
  //  (on entry)          checkMenuFocus(item.getName(), item.getActionCommand(), true);
  //  (on exit)           checkMenuFocus(item.getName(), item.getActionCommand(), false);
  //  (on checkbox click) checkBoxStateChanged(e.getSource());   
  
  //TODO: JavaScript objects:
  //     new Jmol.Menu.PopupMenu(name)
  //     new Jmol.Menu.SubMenu(entry)
  //     new Jmol.Menu.MenuItem(entry)
  //     new Jmol.Menu.ButtonGroup()
  //     new Jmol.Menu.CheckBoxMenuItem(entry)
  //     new Jmol.Menu.RadioButtonMenuItem(entry)
  // 
  //TODO: Jmol given in j2sNative blocks, for example:  setText, setActionCommand
  
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
    /**
     * @j2sNative
     * 
     *     if (icon != null)
     *      b.setIcon((ImageIcon) icon);
     *     if (entry != null)
     *      b.setText(entry);
     *     if (script != null)
     *      b.setActionCommand(script);
     *
     */
    {
      System.out.println(icon);
    }
  }

  private Object newMenuItem(Object menu, Object item, String text,
                                String script, String id) {
       updateButton(item, text, script);    
       /**
        * @j2sNative
        *    if (id != null && id.startsWith("Focus")) {
        *      item.addMouseListener(this);
        *      id = menu.getName() + "." + id;
        *    }
        *    item.setName(id == null ? menu.getName() + "." : id);
        */
       {
         System.out.println(id);
       }
       menuAddItem(menu, item);
       return item;
     }

  ////////////////////////////////////////////////////////////////
  
  /// JmolAbstractMenu ///

  public void menuAddButtonGroup(Object newMenu) {
    /**
     * @j2sNative
     * 
     *  if (this.buttonGroup == null)
     *    this.buttonGroup = new Jmol.Menu.ButtonGroup();
     *    this.buttonGroup.add(newMenu);
     * 
     */
    {}
  }

  public void menuAddItem(Object menu, Object item) {
    /**
     * @j2sNative
     * 
     *  menu.add(item);
     * 
     */
    {}
  }

  public void menuAddSeparator(Object menu) {
    /**
     * @j2sNative
     * 
     *  menu.addSeparator();
     * 
     */
    {}
  }

  public void menuAddSubMenu(Object menu, Object subMenu) {
    menuAddItem(menu, subMenu);
  }

  public void menuClearListeners(Object menu) {
    // ignored
  }
  
  public Object menuCreateCheckboxItem(Object menu, String entry,
                                       String basename, String id,
                                       boolean state, boolean isRadio) {
    Object item = null;
    
    
    /**
     * @j2sNative
     * 
     *      if (isRadio) {
     *        item = new Jmol.Menu.RadioButtonMenuItem(entry);
     *        item.setArmed(state);
     *      } else {
     *        item = new Jmol.Menu.CheckBoxMenuItem(entry);
     *        item.setState(state);
     *      }
     *      item.setSelected(state);
     *      item.addItemListener(this);
     *     
     */
    { }
    return newMenuItem(menu, item, entry, basename, id);
  }

  public Object menuCreateItem(Object menu, String entry, String script,
                               String id) {
    Object item = null;

    /**
     * @j2sNative
     * 
     *      item = new Jmol.Menu.MenuItem(entry);
     *      item.addActionListener(this);
     *     
     */
    {}
    return newMenuItem(menu, item, entry, script, id);
  }

  public Object menuCreatePopup(String name) {
    /**
     * @j2sNative
     * 
     *   return new Jmol.Menu.PopupMenu(name);
     *     
     */
    {
      return null;
    }
  }

  public void menuEnable(Object menu, boolean enable) {
    /**
     * @j2sNative
     * 
     *   if (menu.isItem) {
     *    this.menuEnableItem(menu, enable);
     *    return;
     *   }
     *   try {
     *    menu.setEnabled(enable);
     *   } catch (e) {
     *   // ignore
     *   }
     *     
     */
    {}
  }

  public void menuEnableItem(Object item, boolean enable) {
    /**
     * @j2sNative
     * 
     *   try {
     *     item.setEnabled(enable);
     *   } catch (e) {
     *   // ignore
     *   }
     *     
     */
    {}
  }

  public void menuGetAsText(StringXBuilder sb, int level, Object menu,
                            String menuName) {
    /**
     * @j2sNative
     * 
     *    var name = menuName;
     *    var subMenus = menu.getComponents();
     *    for (int i = 0; i < subMenus.length; i++) {
     *      var m = subMenus[i];
     *      var flags = null;
     *      if (m.isMenu) {
     *        name = m.getName();
     *        flags = "enabled:" + m.isEnabled();
     *        this.addItemText(sb, 'M', level, name, m.getText(), null, flags);
     *        this.menuGetAsText(sb, level + 1, m.getPopupMenu(), name);
     *      } else if (m.isItem) {
     *        flags = "enabled:" + m.isEnabled();
     *        if (m.isCheckBoxItem)
     *          flags += ";checked:" + m.getState();
     *        var script = this.fixScript(m.getName(), m.getActionCommand());
     *        this.addItemText(sb, 'I', level, m.getName(), m.getText(), script, flags);
     *      } else {
     *        this.addItemText(sb, 'S', level, name, null, null, null);
     *      }
     *    }
     */
    {}
  }

  public String menuGetId(Object menu) {
    /**
     * @j2sNative
     * 
     * return menu.getName();
     * 
     */
    {
      return null;
    }
  }

  public int menuGetItemCount(Object menu) {
    /**
     * @j2sNative
     * 
     * return menu.getItemCount();
     * 
     */
    {
      return 0;
    }
  }

  public Object menuGetParent(Object menu) {
    /**
     * @j2sNative
     * 
     * return menu.getParent();
     * 
     */
    {
      return null;
    }
  }

  public int menuGetPosition(Object menu) {
    /**
     * @j2sNative
     * 
     *    var p = menuGetParent(menu);
     *    if (p != null)
     *      for (var i = p.getItemCount(); --i >= 0;)
     *        if (p.getItem(i) == menu)
     *          return i;
     */
    {}
    return -1;
  }

  public void menuInsertSubMenu(Object menu, Object subMenu, int index) {
    /**
     * @j2sNative
     * 
     *  menu.insert(subMenu, index)
     * 
     */
    {}
  }

  
    public Object menuNewEntry(String entry, String id) {
    /**
     * @j2sNative
     * 
     *    var menu = new Jmol.Menu.SubMenu(entry);
     *    this.updateButton(menu, entry, null);
     *    menu.setName(id);
     *    menu.setAutoscrolls(true);
     *    return menu;
     */
    {
      return null;
    }
  }

  public void menuRemoveItem(Object menu, int index) {
    /**
     * @j2sNative
     * 
     *  menu.remove(index);
     * 
     */
    {}
  }

  public void menuRemoveAll(Object menu) {
    /**
     * @j2sNative
     * 
     *  menu.removeAll();
     * 
     */
    {}
  }

  public void menuSetAutoscrolls(Object menu) {
    /**
     * @j2sNative
     * 
     *  menu.setAutoscrolls(true);
     * 
     */
    {}
  }

  public void menuSetCheckBoxState(Object item, boolean state) {
    /**
     * @j2sNative
     * 
     *     if (item.isCheckBox)
     *       item.setState(state);
     *     else
     *       item.setArmed(state);
     *     item.setSelected(state);
     * 
     */
    {}
  }

  public String menuSetCheckBoxOption(Object item, String name, String what) {
    return null;
  }

  public void menuSetCheckBoxValue(Object source) {
    /**
     * @j2sNative
     * 
     *  this.setCheckBoxValue(source, source.getActionCommand(), source.isSelected());
     * 
     */
    {}
  }

  public void menuSetLabel(Object menu, String entry) {
    /**
     * @j2sNative
     * 
     *  menu.setText(entry);
     * 
     */
    {}
  }

  public void menuSetListeners() {
    // ignored
  }
  
  public void menuShowPopup(Object popup, int x, int y) {
    /**
     * @j2sNative
     * 
     *  popup.show(this.viewer.getDisplay(), x, y);
     * 
     */
    {}
  }

}
