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
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.Hashtable;
import java.util.Enumeration;

public class JmolPopupSwing extends JPopupMenu {

  JmolViewer viewer;
  Component component;
  MenuItemListener mil;
  CheckboxMenuItemListener cmil;
  ResourceBundle rbStructure;
  ResourceBundle rbWords;

  public JmolPopupSwing(JmolViewer viewer, Component parent,
                        ResourceBundle rbStructure, ResourceBundle rbWords) {
    super("Jmol");
    this.viewer = viewer;
    this.rbStructure = rbStructure;
    this.rbWords = rbWords;
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
    addMenuItems("popupMenu", this);
    rbWords = null;
    component = viewer.getAwtComponent();
  }

  public void showSwing(int x, int y) {
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements(); ) {
      String key = (String)keys.nextElement();
      JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)htCheckbox.get(key);
      //      System.out.println("found:" + key);
      boolean b = viewer.getBooleanProperty(key);
      jcbmi.setState(b);
    }
    show(component, x, y);
  }

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String script = getValue(e.getActionCommand());
      if (script != null)
        viewer.evalString(script);
    }
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      //  System.out.println("CheckboxMenuItemListener() " + e.getSource());
      JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)e.getSource();
      viewer.setBooleanProperty(jcmi.getActionCommand(), jcmi.getState());
    }
  }

  void addMenuItems(String key, Object menu) {
    String value = getValue(key);
    if (value == null) {
      JMenuItem jmi = new JMenuItem("#" + key);
      if (menu instanceof JPopupMenu)
        ((JPopupMenu)menu).add(jmi);
      else
        ((JMenu)menu).add(jmi);
      return;
    }
    StringTokenizer st = new StringTokenizer(getValue(key));
    while (st.hasMoreTokens()) {
      String item = st.nextToken();
      if (item.endsWith("Menu")) {
        String word = getWord(item);
        JMenu subMenu = new JMenu(word);
        addMenuItems(item, subMenu);
        if (menu instanceof JPopupMenu)
          ((JPopupMenu)menu).add(subMenu);
        else
          ((JMenu)menu).add(subMenu);
      } else if (item.equals("-")) {
        if (menu instanceof JPopupMenu)
          ((JPopupMenu)menu).addSeparator();
        else
          ((JMenu)menu).addSeparator();
      } else {
        String word = getWord(item);
        JMenuItem jmi;
        if (item.endsWith("Checkbox")) {
          JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(word);
          String basename = item.substring(0, item.length() - 8);
          jcmi.setActionCommand(basename);
          jcmi.addItemListener(cmil);
          rememberCheckbox(basename, jcmi);
          jmi = jcmi;
        } else {
          jmi = new JMenuItem(word);
          getValue(item);
          jmi.addActionListener(mil);
          jmi.setActionCommand(item);
        }
        if (menu instanceof JPopupMenu)
          ((JPopupMenu)menu).add(jmi);
        else
          ((JMenu)menu).add(jmi);
      }
    }
  }

  private String getValue(String key) {
    return rbStructure.getString(key);
  }

  private String getWord(String key) {
    return rbWords.getString(key);
  }

  Hashtable htCheckbox = new Hashtable();

  void rememberCheckbox(String key, JCheckBoxMenuItem cbmi) {
    htCheckbox.put(key, cbmi);
  }
}
