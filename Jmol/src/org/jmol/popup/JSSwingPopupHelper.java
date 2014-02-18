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


import javajs.api.GenericMenuInterface;
import javajs.api.SC;
import javajs.swing.ButtonGroup;
import javajs.swing.JPopupMenu;
import javajs.swing.JCheckBoxMenuItem;
import javajs.swing.JRadioButtonMenuItem;
import javajs.swing.JMenu;
import javajs.swing.JMenuItem;

/**
 * For menus, popup-related awt/swing class references are in this file.
 * We can ignore all the event/listener references because JSmol will create a
 * simple object with just the elements getSource and getActionCommand.
 * No need to have the entire classes fleshed out. 
 * 
 * 
 */
  public class JSSwingPopupHelper implements PopupHelper {

  //  (on mouse up)       checkMenuClick(e.getSource(), e.getSource().getActionCommand());
  //  (on entry)          checkMenuFocus(item.getName(), item.getActionCommand(), true);
  //  (on exit)           checkMenuFocus(item.getName(), item.getActionCommand(), false);
  //  (on checkbox click) checkBoxStateChanged(e.getSource());   

  /**
   * used here and by SwingController to refer to the Java 
   * class being handled by this helper.
   */
  GenericMenuInterface popup;
  
  private ButtonGroup buttonGroup;

  public JSSwingPopupHelper(GenericMenuInterface popup) {
    this.popup = popup;
  }

  @Override
  public SC menuCreatePopup(String name, Object applet) {
    JPopupMenu j = new JPopupMenu(name);
    j.setInvoker(applet);
    return j;
  }

  @Override
  public SC getMenu(String name) {
    return new JMenu();
  }

  @Override
  public SC getMenuItem(String name) {
    return new JMenuItem(name);
  }

  @Override
  public SC getRadio(String name) {
    return new JRadioButtonMenuItem();
  }

  @Override
  public SC getCheckBox(String name) {
    return new JCheckBoxMenuItem();
  }

  @Override
  public void menuAddButtonGroup(SC item) {
    if (item == null) {
      buttonGroup = null;
      return;
    }
    if (buttonGroup == null)
      buttonGroup = new ButtonGroup();
    buttonGroup.add(item);
  }

  @Override
  public int getItemType(SC m) {
    /**
     * @j2sNative
     * 
     *            return (m.isMenu ? 4: m.isRadio ? 3 : m.isCheckBox ? 2 : m.text ? 1 : 0);
     */
    {
      return 0;
    }
  }

  @Override
  public void menuInsertSubMenu(SC menu, SC subMenu,
                                int index) {
    // not applicable; Frank menu only
  }

  @Override
  public SC getSwingComponent(Object component) {
    return (SC) component;
  }

  @Override
  public void menuClearListeners(SC menu) {
    if (menu != null)
      ((JPopupMenu) menu).disposeMenu();
  }

  public void itemStateChanged(Object e) {
     /**
      * @j2sNative
      * 
      * this.popup.menuCheckBoxCallback(e.getSource());
      * 
      */
    {
      System.out.println(e);
    }
  }

  public void actionPerformed(Object e) {
    /**
     * @j2sNative
     * 
     * this.popup.menuClickCallback(e.getSource(), e.getActionCommand());
     * 
     */
   {
     System.out.println(e);
   }
  }

  @Override
  public Object getButtonGroup() {
    return buttonGroup;
  }

  
}
