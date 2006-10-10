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
import org.jmol.util.Logger;
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

  Object FRAMESbyModelComputedMenu;
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

  Vector PDBOnly = new Vector();
  Vector UnitcellOnly = new Vector();
  Vector FramesOnly = new Vector();
  Vector VibrationOnly = new Vector();

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
    addMenuItems("", "popupMenu", popupMenu, new PopupResourceBundle());
    if (! viewer.isJvm12orGreater() && (consoleMenu != null))
      enableMenu(consoleMenu, false);
  }

  boolean isPDB;
  boolean isSymmetry;
  boolean isUnitCell;
  boolean isMultiFrame;
  boolean isVibration;
  int modelIndex;

  public void updateComputedMenus() {
    modelIndex = viewer.getDisplayModelIndex();
    Hashtable info = viewer.getModelSetAuxiliaryInfo();
    isPDB = checkBoolean(info, "isPDB");
    isSymmetry = checkBoolean(info, "someModelsHaveSymmetry");
    isUnitCell = checkBoolean(info, "someModelsHaveUnitcells");
    isMultiFrame = (viewer.getModelCount() > 1);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex)); 

    updateElementsComputedMenu(viewer.getElementsPresentBitSet());
    updatesurfMEP(viewer.havePartialCharges());
    updateHeteroComputedMenu(viewer.getHeteroList());
    updateSurfMoComputedMenu((Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,"moData"));
    updateAaresiduesComputedMenu(viewer.getGroupsPresentBitSet());
    updateFRAMESbyModelComputedMenu();
    updateModelSetInfoMenu();
    updateFileTypeDependentMenus();
  }

  boolean checkBoolean(Hashtable info, String key) {
    if (info == null || !info.containsKey(key))
        return false;
    return ((Boolean)(info.get(key))).booleanValue();
  }
  void updateFileTypeDependentMenus() {
    for (int i = 0; i < PDBOnly.size(); i++)
      enableMenu(PDBOnly.get(i), isPDB);
    for (int i = 0; i < UnitcellOnly.size(); i++)
      enableMenu(UnitcellOnly.get(i), isUnitCell);
    for (int i = 0; i < FramesOnly.size(); i++)
      enableMenu(FramesOnly.get(i), isMultiFrame);
    for (int i = 0; i < VibrationOnly.size(); i++)
      enableMenu(VibrationOnly.get(i), isVibration);
  }
  
  void updatesurfMEP(boolean haveCharges) {
    if (surfMEP == null)
      return;
    enableMenuItem(surfMEP, haveCharges);
  }

  void updateFRAMESbyModelComputedMenu() {
    if (FRAMESbyModelComputedMenu == null)
      return;
    enableMenu(FRAMESbyModelComputedMenu, false);
    setLabel(FRAMESbyModelComputedMenu, (modelIndex < 0 ? GT._("Models/Frames") : getModelLabel()));
    removeAll(FRAMESbyModelComputedMenu);
    int modelCount = viewer.getModelCount();
    if (modelCount < 2)
      return;
    enableMenu(FRAMESbyModelComputedMenu, true);
    addMenuItemWithId(FRAMESbyModelComputedMenu, "all", "frame 0", null);
    for (int i = 0; i < modelCount; i++) {
      String script = "" + viewer.getModelNumber(i);
      String entryName = viewer.getModelName(i);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
      if (entryName.length() > 30)
        entryName = entryName.substring(0, 20) + "...";
      addMenuItemWithId(FRAMESbyModelComputedMenu, entryName, "model " + script, null);
    }
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
      addMenuItemWithId(surfMoComputedMenu, entryName, script, null);
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
        addMenuItemWithId(elementsComputedMenu, entryName, elementName, null);
      }
    }
    for (int i = JmolConstants.firstIsotope; i < JmolConstants.altElementMax; ++i) {
      int n = JmolConstants.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = JmolConstants.altElementNumberFromIndex(i);
        String elementName = JmolConstants.elementNameFromNumber(n);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        addMenuItemWithId(elementsComputedMenu, entryName, elementName, null);
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
        addMenuItemWithId(heteroComputedMenu, entryName, heteroCode, null);
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
        addMenuItemWithId(aaresiduesComputedMenu, aaresidueName, null, null);
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
        viewer.getBooleanProperty("hideNameInPopup") ? hiddenModelSetName
            : modelSetName);
    enableMenu(modelSetInfoMenu, true);
    addMenuItem(modelSetInfoMenu, GT._("atoms: {0}",
        new Object[] { new Integer(viewer.getAtomCountInModel(modelIndex)) }));
    addMenuItem(modelSetInfoMenu, GT._("bonds: {0}",
        new Object[] { new Integer(viewer.getBondCountInModel(modelIndex)) }));
    addMenuSeparator(modelSetInfoMenu);
    if (isPDB) {
      addMenuItem(modelSetInfoMenu, GT._("groups: {0}",
          new Object[] { new Integer(viewer.getGroupCountInModel(modelIndex)) }));
      addMenuItem(modelSetInfoMenu, GT._("chains: {0}",
          new Object[] { new Integer(viewer.getChainCountInModel(modelIndex)) }));
      addMenuItem(modelSetInfoMenu, GT._("polymers: {0}",
          new Object[] { new Integer(viewer.getPolymerCountInModel(modelIndex)) }));
      addMenuSeparator(modelSetInfoMenu);
    }
    if (isMultiFrame) {
      addMenuItem(modelSetInfoMenu, GT._("models: {0}",
          new Object[] { new Integer(viewer.getModelCount()) }));
      addMenuSeparator(modelSetInfoMenu);
    }
    if (viewer.showModelSetDownload()
        && !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuItemWithId(modelSetInfoMenu, viewer.getModelSetFileName(), viewer
          .getModelSetPathName(), null);
    }
  }

  String getModelLabel() {
    return GT._("model: {0}",
        new Object[] { (modelIndex + 1) + "/" + viewer.getModelCount() });
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

  Hashtable colorMenus = new Hashtable();
  
  private void addMenuItems(String parentId, String key, Object menu,
                            PopupResourceBundle popupResourceBundle) {
    String id = parentId + " " + key;
    String value = popupResourceBundle.getStructure(key);
    Logger.debug(id + " --- " + value);
    if (value == null) {
      addMenuItemWithId(menu, "#" + key, "", "");
      return;
    }
    boolean isColorScheme = false;
    String colorSet = "";
    String colorCode = "";
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreTokens()) {
      Object newMenu = null;
      String item = st.nextToken();
      if (item.equals("@")) {
        isColorScheme = true;
        continue;
      }
      if (isColorScheme) {
        colorCode += item;
        if (colorSet.length() > 0)
          colorSet += " -";
        if (item.equals("INHERIT")) {
          colorSet += " " + PopupResourceBundle.INHERIT;
        } else if (item.equals("COLOR")) {
          colorSet += " " + PopupResourceBundle.COLOR;
        } else if (item.equals("SCHEME")) {
          colorSet += " " + PopupResourceBundle.SCHEME;
        } else if (item.equals("TRANSLUCENCY")) {
          colorSet += " " + PopupResourceBundle.TRANSLUCENCY;
        } else if (item.equals("AXESCOLOR")) {
          colorSet += " " + PopupResourceBundle.AXESCOLOR;
        }
        if (st.hasMoreTokens())
          continue;
        /*
         String colorList = (String) colorMenus.get(colorCode);
         if (colorList == null) {
         popupResourceBundle.addStructure(key, colorSet);
         colorMenus.put(colorCode, colorSet);
         }
         */
        popupResourceBundle.addStructure(key, colorSet);
        addMenuItems(parentId, key, menu, popupResourceBundle);
        return;
      }
      String word = popupResourceBundle.getWord(item);
      if (item.indexOf("Menu") >= 0) {
        Object subMenu = newMenu(word);
        ((Component) subMenu).setName(id + " " + item);
        addMenuSubMenu(menu, subMenu);

        // these will need creating later:
        if ("elementsComputedMenu".equals(item))
          elementsComputedMenu = subMenu;
        else if ("aaresiduesComputedMenu".equals(item))
          aaresiduesComputedMenu = subMenu;
        else if ("heteroComputedMenu".equals(item))
          heteroComputedMenu = subMenu;
        else if ("surfMoComputedMenu".equals(item))
          surfMoComputedMenu = subMenu;
        else if ("FRAMESbyModelComputedMenu".equals(item))
          FRAMESbyModelComputedMenu = subMenu;
        else
          addMenuItems(id, item, subMenu, popupResourceBundle);

        // these will need tweaking:
        if ("aboutMenu".equals(item)) {
          aboutMenu = subMenu;
          aboutMenuBaseCount = getMenuItemCount(subMenu);
        } else if ("consoleMenu".equals(item)) {
          consoleMenu = subMenu;
        } else if ("modelSetInfoMenu".equals(item)) {
          nullModelSetName = word;
          hiddenModelSetName = popupResourceBundle
              .getWord("hiddenModelSetName");
          modelSetInfoMenu = subMenu;
          enableMenu(modelSetInfoMenu, false);
        }
        newMenu = subMenu;
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        String basename = item.substring(0, item.length() - 8);
        newMenu = addCheckboxMenuItemWithId(menu, word, basename, id + " " + item);
      } else {
        String script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = addMenuItemWithId(menu, word, script, id + " " + item);
        // add a menu-path to this item for color context
        // addition items that may need enabling/disabling:
        if ("surfMEP".equals(item))
          surfMEP = newMenu;
      }
      
      // menus or menu items:
      if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } else if (item.indexOf("nitCell") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } else if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      }
    }
  }

  Object addMenuItemWithId(Object menu, String word, String script, String id) {
    Object newMenu = addMenuItem(menu, word, script);
    ((Component)newMenu).setName(id == null ? ((Component)menu).getName() : id);
    return newMenu;
  }
  
  Object addCheckboxMenuItemWithId(Object menu, String entry, String basename,
                                   String id) {
    Object newMenu = addCheckboxMenuItem(menu, entry, basename);
    ((Component) newMenu).setName(id == null ? ((Component)menu).getName() : id);
    return newMenu;
  }
  
  Hashtable htCheckbox = new Hashtable();

  void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key, checkboxMenuItem);
  }

  /**
   * (1) setOption --> set setOption true or set setOption false
   *  
   * @param what option to set
   * @param TF   true or false
   */
  void setCheckBoxValue(String what, boolean TF) {
    if (viewer.getBooleanProperty(what) != TF)
      viewer.setBooleanProperty(what, TF);
  }
  
  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String script = e.getActionCommand();
      if (script == null || script.length() == 0)
        return;
      if (script.startsWith("http:") || script.startsWith("file:")
          || script.startsWith("/")) {
        viewer.showUrl(script);
        return;
      }
      String id = ((Component) e.getSource()).getName();
      System.out.println(id + ": " + script);
      if (id != null) {
        if (id.indexOf(" setSpin") >= 0) {
          // setSpinFooMenu
          int pt = id.lastIndexOf(" setSpin");
          if (pt >= 0)
            script = "set spin " + id.substring(pt + 8, id.indexOf("Menu", pt)) + " " + script;
        } else if (id.indexOf(" set") >= 0) {
          // setFooMenu
          int pt = id.lastIndexOf(" set");
          if (pt >= 0)
            script = "set " + id.substring(pt + 6, id.indexOf("Menu", pt)) + " " + script;
        } else if (id.indexOf(" select") >= 0) {
          // select item but not selectMenu set selectionHalos
          if (script.indexOf("set") != 0)
            script = "select " + script;
        } else if(id.indexOf(" color") >= 0){
          // colorFooMenu
          int pt = id.lastIndexOf(" color");
          if (pt >= 0)
            script = "color " + id.substring(pt + 6, id.indexOf("Menu", pt)) + " " + script;
        }
      }
      viewer.script(script);
    }
  }

  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItemWithId(menuItem, entry, "", null);
  }

  public void show(int x, int y) {
    updateAboutSubmenu();
    showPopup(x, y);
  }

  ////////////////////////////////////////////////////////////////

  abstract void showPopup(int x, int y);

  abstract void addMenuSeparator(Object menu);

  abstract Object addMenuItem(Object menu, String entry, String script);

  abstract void setLabel(Object menu, String entry);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract Object addCheckboxMenuItem(Object menu, String entry,String basename);

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

