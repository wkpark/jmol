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
import org.jmol.i18n.GT;
import org.jmol.viewer.JmolConstants;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

abstract public class JmolPopup {
  private final static boolean forceAwt = false;

  JmolViewer viewer;
  Component jmolComponent;
  MenuItemListener mil;

  Object elementsComputedMenu;
  Object aaresiduesComputedMenu;
  Object heteroComputedMenu;
  Object surfMoComputedMenu;
  Object surfMEP;
  Object aboutMenu;
  int aboutMenuBaseCount;
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
    if (! viewer.isJvm12orGreater() && (consoleMenu != null))
      enableMenu(consoleMenu, false);
  }

  public void updateComputedMenus() {
    updateElementsComputedMenu(viewer.getElementsPresentBitSet());
    updatesurfMEP(viewer.havePartialCharges());
    updateHeteroComputedMenu(viewer.getHeteroList());
    updateSurfMoComputedMenu((Hashtable) viewer.getModelAuxiliaryInfo(viewer.getDisplayModelIndex(),"moData"));
    updateAaresiduesComputedMenu(viewer.getGroupsPresentBitSet());
    updateModelSetInfoMenu();
  }

  void updatesurfMEP(boolean haveCharges) {
    if (surfMEP == null)
      return;
    enableMenuItem(surfMEP, haveCharges);
  }

  void updateSurfMoComputedMenu(Hashtable moData) {
    if (surfMoComputedMenu == null)
      return;
    enableMenu(surfMoComputedMenu, false);
    removeAll(surfMoComputedMenu);
    if (moData == null)
      return;
    Vector mos = (Vector) (moData.get("mos"));
    int nOrb = (mos == null ? 0 : mos.size());
    if (nOrb == 0)
      return;
    enableMenu(surfMoComputedMenu, true);
    for (int i = nOrb; --i >= 0;) {
      String entryName = "#" + (i + 1) +" " 
      + ((Hashtable)(mos.get(i))).get("energy");
      String script = "mo " + (i + 1);
      addMenuItem(surfMoComputedMenu, entryName, script);
    }
  }

  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    if (elementsComputedMenu == null)
      return;
    removeAll(elementsComputedMenu);
    enableMenu(elementsComputedMenu, false);
    if (elementsPresentBitSet == null)
      return;
    for (int i = 0; i < JmolConstants.elementNumberMax; ++i) {
      if (elementsPresentBitSet.get(i)) {
        String elementName = JmolConstants.elementNameFromNumber(i);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(i);
        String entryName = elementSymbol + " - " + elementName;
        String script = "select " + elementName;
        addMenuItem(elementsComputedMenu, entryName, script);
      }
    }
    for (int i = JmolConstants.firstIsotope; i < JmolConstants.altElementMax; ++i) {
      int n = JmolConstants.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = JmolConstants.altElementNumberFromIndex(i);
        String elementName = JmolConstants.elementNameFromNumber(n);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        String script = "select " + elementName;
        addMenuItem(elementsComputedMenu, entryName, script);
      }
    }
    enableMenu(elementsComputedMenu, true);
  }

  void updateHeteroComputedMenu(Hashtable htHetero) {
    if (heteroComputedMenu == null)
      return;
    removeAll(heteroComputedMenu);
    if (htHetero == null)
      return;
    Enumeration e = htHetero.keys();
    int n = 0;
    while (e.hasMoreElements()) {
        String heteroCode = (String) e.nextElement();
        String heteroName = (String) htHetero.get(heteroCode);
        if (heteroName.length() > 20)
          heteroName = heteroName.substring(0,20) + "...";
        String entryName = heteroCode + " - " + heteroName;
        String script = "select " + heteroCode;
        addMenuItem(heteroComputedMenu, entryName, script);
        n++;
    }
    enableMenu(heteroComputedMenu, (n > 0));
  }

  void updateAaresiduesComputedMenu(BitSet groupsPresentBitSet) {
    if (aaresiduesComputedMenu == null)
      return;
    removeAll(aaresiduesComputedMenu);
    enableMenu(aaresiduesComputedMenu, false);
    if (groupsPresentBitSet == null)
      return;
    for (int i = 1; i < JmolConstants.GROUPID_AMINO_MAX; ++i) {
      if (groupsPresentBitSet.get(i)) {
        String aaresidueName = JmolConstants.predefinedGroup3Names[i];
        String script = "select " + aaresidueName;
        addMenuItem(aaresiduesComputedMenu, aaresidueName, script);
      }
    }
    enableMenu(aaresiduesComputedMenu, true);
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
    addMenuItem(modelSetInfoMenu,
                GT._("atoms: {0}", new Object[] { new Integer(viewer.getAtomCount()) }));
    addMenuItem(modelSetInfoMenu,
                GT._("bonds: {0}", new Object[] { new Integer(viewer.getBondCount()) }));
    addMenuSeparator(modelSetInfoMenu);
    addMenuItem(modelSetInfoMenu,
                GT._("groups: {0}", new Object[] { new Integer(viewer.getGroupCount()) }));
    addMenuItem(modelSetInfoMenu,
                GT._("chains: {0}", new Object[] { new Integer(viewer.getChainCount()) }));
    addMenuItem(modelSetInfoMenu,
                GT._("polymers: {0}", new Object[] { new Integer(viewer.getPolymerCount()) }));
    addMenuItem(modelSetInfoMenu,
                GT._("models: {0}", new Object[] { new Integer(viewer.getModelCount()) }));
    if (viewer.showModelSetDownload() &&
        !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuSeparator(modelSetInfoMenu);
      addMenuItem(modelSetInfoMenu,
                  viewer.getModelSetFileName(), viewer.getModelSetPathName());
    }
  }

  private void updateAboutSubmenu() {
    if (aboutMenu == null)
      return;
    for (int i = getMenuItemCount(aboutMenu); --i >= aboutMenuBaseCount; )
      removeMenuItem(aboutMenu, i);
    addMenuSeparator(aboutMenu);
    addMenuItem(aboutMenu, "Jmol " + JmolConstants.version);
    addMenuItem(aboutMenu, JmolConstants.date);
    addMenuItem(aboutMenu, viewer.getOperatingSystemName());
    addMenuItem(aboutMenu, viewer.getJavaVendor());
    addMenuItem(aboutMenu, viewer.getJavaVersion());
    addMenuSeparator(aboutMenu);
    addMenuItem(aboutMenu, GT._("Java memory usage"));
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long mbTotal = convertToMegabytes(runtime.totalMemory());
    long mbFree = convertToMegabytes(runtime.freeMemory());
    long mbMax = convertToMegabytes(maxMemoryForNewerJvm());
    addMenuItem(aboutMenu, GT._("{0} MB total", new Object[] { new Long(mbTotal) }));
    addMenuItem(aboutMenu, GT._("{0} MB free", new Object[] { new Long(mbFree) }));
    if (mbMax > 0)
      addMenuItem(aboutMenu, GT._("{0} MB maximum", new Object[] { new Long(mbMax) }));
    else
      addMenuItem(aboutMenu, GT._("unknown maximum"));
    int availableProcessors = availableProcessorsForNewerJvm();
    if (availableProcessors > 0)
      addMenuItem(aboutMenu, (availableProcessors == 1) ?
                  GT._("1 processor") :
                  GT._("{0} processors", new Object[] { new Integer(availableProcessors) } ));
    else
      addMenuItem(aboutMenu, GT._("unknown processor count"));
  }

  private long convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512*1024)
      num += 512*1024;
    return num / (1024*1024);
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
      Object newMenu = null;
      String item = st.nextToken();
      String word = popupResourceBundle.getWord(item);
      if (item.endsWith("Menu")) {
        Object subMenu = newMenu(word);
        if ("elementsComputedMenu".equals(item))
          elementsComputedMenu = subMenu;
        else if ("aaresiduesComputedMenu".equals(item))
          aaresiduesComputedMenu = subMenu;
        else if ("heteroComputedMenu".equals(item))
          heteroComputedMenu = subMenu;
        else if ("surfMoComputedMenu".equals(item))
          surfMoComputedMenu = subMenu;
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
        if (subMenu == aboutMenu)
          aboutMenuBaseCount = getMenuItemCount(aboutMenu);
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        String basename = item.substring(0, item.length() - 8);
        addCheckboxMenuItem(menu, word, basename);
      } else {
        newMenu = addMenuItem(menu, word, popupResourceBundle.getStructure(item));
        if ("surfMEP".equals(item))
          surfMEP = newMenu;
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

  public void show(int x, int y) {
    updateAboutSubmenu();
    showPopup(x, y);
  }

  ////////////////////////////////////////////////////////////////

  abstract void showPopup(int x, int y);

  abstract void addMenuSeparator(Object menu);

  abstract Object addMenuItem(Object menu, String entry, String script);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract void addCheckboxMenuItem(Object menu, String entry,String basename);

  abstract void addMenuSubMenu(Object menu, Object subMenu);

  abstract Object newMenu(String menuName);

  abstract void enableMenu(Object menu, boolean enable);

  abstract void enableMenuItem(Object item, boolean enable);

  abstract void renameMenu(Object menu, String menuName);

  abstract void removeAll(Object menu);

  abstract int getMenuItemCount(Object menu);

  abstract void removeMenuItem(Object menu, int index);

  long maxMemoryForNewerJvm() {
    // this method is overridden in JmolPopupSwing for newer Javas
    // JmolPopupAwt does not implement this
    return 0;
  }

  int availableProcessorsForNewerJvm() {
    // this method is overridden in JmolPopupSwing for newer Javas
    // JmolPopupAwt does not implement this
    return 0;
  }
}

