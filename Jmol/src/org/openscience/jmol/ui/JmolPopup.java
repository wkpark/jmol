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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.BitSet;
import java.util.Hashtable;

abstract public class JmolPopup {
  private final static boolean forceAwt = false;

  JmolViewer viewer;
  Component jmolComponent;
  MenuItemListener mil;

  Object elementsComputedMenu;
  Object aaresiduesComputedMenu;

  ResourceBundle rbStructure;
  ResourceBundle rbWords;

  JmolPopup(JmolViewer viewer) {
    this.viewer = viewer;
    jmolComponent = viewer.getAwtComponent();
    mil = new MenuItemListener();
  }

  static public JmolPopup newJmolPopup(JmolViewer viewer) {
    if (! viewer.jvm12orGreater || forceAwt)
      return new JmolPopupAwt(viewer);
    return new JmolPopupSwing(viewer);
  }


  void build(Object popupMenu) {
    rbStructure = ResourceBundle.getBundle("org.openscience.jmol.ui." +
                                           "JmolPopupStructure");
    rbWords = ResourceBundle.getBundle("org.openscience.jmol.ui." +
                                       "JmolPopupWords");

    addMenuItems("popupMenu", popupMenu);
    addVersionAndDate();

    rbWords = null;
    rbStructure = null;
  }

  public void updateComputedMenus() {
    if (elementsComputedMenu != null) {
      BitSet elementsPresentBitSet = viewer.getElementsPresentBitSet();
      updateElementsComputedMenu(elementsPresentBitSet);
    }
    BitSet groupsPresentBitSet = viewer.getGroupsPresentBitSet();
    if (aaresiduesComputedMenu != null)
      updateAaresiduesComputedMenu(groupsPresentBitSet);
  }

  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    removeAll(elementsComputedMenu);
    for (int i = 0; i < JmolConstants.elementNames.length; ++i) {
      if (elementsPresentBitSet.get(i)) {
        String elementName = JmolConstants.elementNames[i];
        String elementSymbol = JmolConstants.elementSymbols[i];
        String entryName = elementSymbol + " - " + elementName;
        String script = "select " + elementName;
        addMenuItem(elementsComputedMenu, entryName, script);
      }
    }
  }
  
  void updateAaresiduesComputedMenu(BitSet groupsPresentBitSet) {
    removeAll(aaresiduesComputedMenu);
    for (int i = 0; i < JmolConstants.RESID_AMINO_MAX; ++i) {
      if (groupsPresentBitSet.get(i)) {
        String aaresidueName = JmolConstants.predefinedGroup3Names[i];
        String script = "select " + aaresidueName;
        addMenuItem(aaresiduesComputedMenu, aaresidueName, script);
      }
    }
  }
  
  private void addVersionAndDate() {
    addMenuSeparator();
    addMenuItem("Jmol " + JmolConstants.version);
    addMenuItem(JmolConstants.date);
  }

  private void addMenuItems(String key, Object menu) {
    String value = getValue(key);
    if (value == null) {
      addMenuItem(menu, "#" + key, null);
      return;
    }
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreTokens()) {
      String item = st.nextToken();
      String word = getWord(item);
      if (item.endsWith("Menu")) {
        Object subMenu = newMenu(word);
        if ("elementsComputedMenu".equals(item))
          elementsComputedMenu = subMenu;
        else if ("aaresiduesComputedMenu".equals(item))
          aaresiduesComputedMenu = subMenu;
        else
          addMenuItems(item, subMenu);
        addMenuSubMenu(menu, subMenu);
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else {
        if (item.endsWith("Checkbox")) {
          String basename = item.substring(0, item.length() - 8);
          addCheckboxMenuItem(menu, word, basename);
       } else {
          addMenuItem(menu, word, getScriptValue(item));
        }
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

  void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key, checkboxMenuItem);
  }

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script != null)
        viewer.evalString(script);
    }
  }

  ////////////////////////////////////////////////////////////////

  abstract public void show(int x, int y);

  abstract void addMenuSeparator();

  abstract void addMenuSeparator(Object menu);

  abstract void addMenuItem(String entry);

  abstract void addMenuItem(Object menu, String entry, String script);

  abstract void addCheckboxMenuItem(Object menu, String entry,String basename);

  abstract void addMenuSubMenu(Object menu, Object subMenu);

  abstract Object newMenu(String menuName);

  abstract void removeAll(Object menu);

}

