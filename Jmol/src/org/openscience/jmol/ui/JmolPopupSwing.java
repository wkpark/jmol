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
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.BitSet;

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
    addVersionAndDate();
    rbWords = null;
    component = viewer.getAwtComponent();
  }

  void addVersionAndDate() {
    addSeparator();
    JMenuItem jmi = new JMenuItem("Jmol " + JmolConstants.version);
    add(jmi);
    jmi = new JMenuItem(JmolConstants.date);
    add(jmi);
  }


  public void showSwing(int x, int y) {
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements(); ) {
      String key = (String)keys.nextElement();
      JCheckBoxMenuItem jcbmi = (JCheckBoxMenuItem)htCheckbox.get(key);
      boolean b = viewer.getBooleanProperty(key);
      //System.out.println("found:" + key + " & it is:" + b);
      jcbmi.setState(b);
    }
    show(component, x, y);
  }

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script != null)
        viewer.evalString(script);
    }
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      //System.out.println("CheckboxMenuItemListener() " + e.getSource());
      JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem)e.getSource();
      viewer.setBooleanProperty(jcmi.getActionCommand(), jcmi.getState());
    }
  }

  void addMenuItems(String key, JComponent menu) {
    String value = getValue(key);
    if (value == null) {
      JMenuItem jmi = new JMenuItem("#" + key);
      addToMenu(menu, jmi);
      return;
    }
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreTokens()) {
      String item = st.nextToken();
      String word = getWord(item);
      if (item.endsWith("Menu")) {
        JMenu subMenu;
        if (item.endsWith("ComputedMenu"))
          subMenu = getComputedMenu(word, item);
        else
          addMenuItems(item, subMenu = new JMenu(word));
        addToMenu(menu, subMenu);
      } else if (item.equals("-")) {
        if (menu instanceof JPopupMenu)
          ((JPopupMenu)menu).addSeparator();
        else
          ((JMenu)menu).addSeparator();
      } else {
        JMenuItem jmi;
        if (item.endsWith("Checkbox")) {
          //System.out.println("I see a cheeckbox named:" + item);
          JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(word);
          String basename = item.substring(0, item.length() - 8);
          jcmi.setActionCommand(getScriptValue(basename));
          jcmi.addItemListener(cmil);
          rememberCheckbox(basename, jcmi);
          jmi = jcmi;
        } else {
          jmi = new JMenuItem(word);
          jmi.addActionListener(mil);
          jmi.setActionCommand(getScriptValue(item));
        }
        addToMenu(menu, jmi);
      }
    }
  }

  private String getValue(String key) {
    try {
      return rbStructure.getString(key);
    } catch (MissingResourceException e) {
      return null;
    }
  }

  private String getScriptValue(String key) {
    return getValue(key);
  }

  private String getWord(String key) {
    String str = key;
    try {
      str = rbWords.getString(key);
    } catch (MissingResourceException e) {
    }
    return str;
  }

  Hashtable htCheckbox = new Hashtable();

  void rememberCheckbox(String key, JCheckBoxMenuItem cbmi) {
    htCheckbox.put(key, cbmi);
  }

  JMenu elementComputedMenu;

  JMenu getComputedMenu(String word, String key) {
    if ("elementComputedMenu".equals(key)) {
      elementComputedMenu = new JMenu(word);
      return elementComputedMenu;
    }
    return new JMenu("unrecognized ComputedMenu:" + key);
  }

  void addToMenu(JComponent menu, JComponent item) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu)menu).add(item);
    else
      ((JMenu)menu).add(item);
  }

  void updateElementComputedMenu(BitSet elementsPresentBitSet) {
    elementComputedMenu.removeAll();
    for (int i = 0; i < JmolConstants.elementNames.length; ++i) {
      if (elementsPresentBitSet.get(i)) {
        String elementName = JmolConstants.elementNames[i];
        String elementSymbol = JmolConstants.elementSymbols[i];
        JMenuItem jmi = new JMenuItem(elementSymbol + " - " + elementName);
        jmi.addActionListener(mil);
        jmi.setActionCommand("select " + elementName);
        addToMenu(elementComputedMenu, jmi);
      }
    }
  }
}
