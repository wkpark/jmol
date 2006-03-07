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
import org.jmol.viewer.JmolConstants;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.BitSet;
import java.util.Hashtable;

abstract public class JmolPopup {
  private final static boolean forceAwt = false;

  JmolViewer viewer;
  Component jmolComponent;
  MenuItemListener mil;

  Object elementsComputedMenu;
  Object aaresiduesComputedMenu;
  Object aboutMenu;
  Object consoleMenu;
  Object modelSetInfoMenu;
  String nullModelSetName;
  String hiddenModelSetName;

  JmolPopup(JmolViewer viewer) {
    this.viewer = viewer;
    jmolComponent = viewer.getAwtComponent();
    mil = new MenuItemListener();
  }

  static public JmolPopup newJmolPopup(JmolViewer viewer) {
    if (! viewer.isJvm12orGreater() || forceAwt)
      return new JmolPopupAwt(viewer);
    return new JmolPopupSwing(viewer);
  }


  void build(Object popupMenu) {
    addMenuItems("popupMenu", popupMenu, new PopupResourceBundle());
    addVersionAndDate(popupMenu);
    if (! viewer.isJvm12orGreater() && (consoleMenu != null))
      enableMenu(consoleMenu, false);
  }

  public void updateComputedMenus() {
    updateElementsComputedMenu(viewer.getElementsPresentBitSet());
    updateAaresiduesComputedMenu(viewer.getGroupsPresentBitSet());
    updateModelSetInfoMenu();
  }

  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    if (elementsComputedMenu == null || elementsPresentBitSet == null)
      return;
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
    if (aaresiduesComputedMenu == null || groupsPresentBitSet == null)
      return;
    removeAll(aaresiduesComputedMenu);
    for (int i = 1; i < JmolConstants.GROUPID_AMINO_MAX; ++i) {
      if (groupsPresentBitSet.get(i)) {
        String aaresidueName = JmolConstants.predefinedGroup3Names[i];
        String script = "select " + aaresidueName;
        addMenuItem(aaresiduesComputedMenu, aaresidueName, script);
      }
    }
  }

  void updateModelSetInfoMenu() {
    if (modelSetInfoMenu == null)
      return;
    removeAll(modelSetInfoMenu);
    renameMenu(modelSetInfoMenu, nullModelSetName);
    enableMenu(modelSetInfoMenu, false);
    String modelSetName = viewer.getModelSetName();
    if (modelSetName == null)
      return;
    renameMenu(modelSetInfoMenu,
               viewer.getBooleanProperty("hideNameInPopup")
               ? hiddenModelSetName : modelSetName);
    enableMenu(modelSetInfoMenu, true);
    addMenuItem(modelSetInfoMenu, "atoms:" + viewer.getAtomCount());
    addMenuItem(modelSetInfoMenu, "bonds:" + viewer.getBondCount());
    addMenuSeparator(modelSetInfoMenu);
    addMenuItem(modelSetInfoMenu, "groups:" + viewer.getGroupCount());
    addMenuItem(modelSetInfoMenu, "chains:" + viewer.getChainCount());
    addMenuItem(modelSetInfoMenu, "polymers:" + viewer.getPolymerCount());
    addMenuItem(modelSetInfoMenu, "models:" + viewer.getModelCount());
    if (viewer.showModelSetDownload() &&
        !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuSeparator(modelSetInfoMenu);
      addMenuItem(modelSetInfoMenu,
                  viewer.getModelSetFileName(), viewer.getModelSetPathName());
    }
  }

  private void addVersionAndDate(Object popupMenu) {
    if (aboutMenu != null) {
      addMenuSeparator(aboutMenu);
      addMenuItem(aboutMenu, "Jmol " + JmolConstants.version);
      addMenuItem(aboutMenu, JmolConstants.date);
      addMenuItem(aboutMenu, viewer.getOperatingSystemName());
      addMenuItem(aboutMenu, viewer.getJavaVendor());
      addMenuItem(aboutMenu, viewer.getJavaVersion());
    }
  }

  private void addMenuItems(String key, Object menu,
                            PopupResourceBundle popupResourceBundle) {
    String value = popupResourceBundle.getStructure(key);
    if (value == null) {
      addMenuItem(menu, "#" + key);
      return;
    }
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreTokens()) {
      String item = st.nextToken();
      String word = popupResourceBundle.getWord(item);
      if (item.endsWith("Menu")) {
        Object subMenu = newMenu(word);
        if ("elementsComputedMenu".equals(item))
          elementsComputedMenu = subMenu;
        else if ("aaresiduesComputedMenu".equals(item))
          aaresiduesComputedMenu = subMenu;
        else
          addMenuItems(item, subMenu, popupResourceBundle);
        if ("aboutMenu".equals(item))
          aboutMenu = subMenu;
        else if ("consoleMenu".equals(item))
          consoleMenu = subMenu;
        else if ("modelSetInfoMenu".equals(item)) {
          nullModelSetName = word;
          hiddenModelSetName =
            popupResourceBundle.getWord("hiddenModelSetName");
          modelSetInfoMenu = subMenu;
          enableMenu(modelSetInfoMenu, false);
        }
        addMenuSubMenu(menu, subMenu);
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        String basename = item.substring(0, item.length() - 8);
        addCheckboxMenuItem(menu, word, basename);
      } else {
        addMenuItem(menu, word, popupResourceBundle.getStructure(item));
      }
    }
  }
  
  Hashtable htCheckbox = new Hashtable();

  void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key, checkboxMenuItem);
  }

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script == null || script.length() == 0)
        return;
      if (script.startsWith("http:") || script.startsWith("file:") ||
          script.startsWith("/")) {
        viewer.showUrl(script);
        return;
      }
      viewer.script(script);  
    }
  }

  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItem(menuItem, entry, null);
  }

  ////////////////////////////////////////////////////////////////

  abstract public void show(int x, int y);

  abstract void addMenuSeparator(Object menu);

  abstract Object addMenuItem(Object menu, String entry, String script);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract void addCheckboxMenuItem(Object menu, String entry,String basename);

  abstract void addMenuSubMenu(Object menu, Object subMenu);

  abstract Object newMenu(String menuName);

  abstract void enableMenu(Object menu, boolean enable);

  abstract void renameMenu(Object menu, String menuName);

  abstract void removeAll(Object menu);

}

