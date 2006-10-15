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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.StringTokenizer;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

abstract public class JmolPopup {

  private final static boolean forceAwt = false;

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
  private final static boolean dumpList = false;
  
  JmolViewer viewer;
  Component jmolComponent;
  MenuItemListener mil;
  CheckboxMenuItemListener cmil;

  Hashtable htMenus = new Hashtable();
  Object frankPopup;
  
  int aboutComputedMenuBaseCount;
  String nullModelSetName;
  String hiddenModelSetName;

  Vector PDBOnly = new Vector();
  Vector UnitcellOnly = new Vector();
  Vector FramesOnly = new Vector();
  Vector VibrationOnly = new Vector();
  Vector SymmetryOnly = new Vector();
  Vector AppletOnly = new Vector();
  Vector ChargesOnly = new Vector();

  boolean isPDB;
  boolean isSymmetry;
  boolean isUnitCell;
  boolean isMultiFrame;
  boolean isMultiConfiguration;
  boolean isVibration;
  boolean isApplet;
  boolean haveCharges;
  boolean haveAltLocs;
  
  int modelIndex;
  
  JmolPopup(JmolViewer viewer) {
    this.viewer = viewer;
    jmolComponent = viewer.getAwtComponent();
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
  }

  static public JmolPopup newJmolPopup(JmolViewer viewer) {
    if (! viewer.isJvm12orGreater() || forceAwt)
      return new JmolPopupAwt(viewer);
    return new JmolPopupSwing(viewer);
  }

  void build(Object popupMenu) {
    addMenuItems("", "popupMenu", popupMenu, new PopupResourceBundle());
  }

  final static int UPDATE_ALL = 0;
  final static int UPDATE_CONFIG = 1;
  final static int UPDATE_SHOW = 2;
  int updateMode;
  
  public void updateComputedMenus() {
    updateMode = UPDATE_ALL;
    isApplet = viewer.isApplet();
    modelIndex = viewer.getDisplayModelIndex();
    Hashtable info = viewer.getModelSetAuxiliaryInfo();
    isPDB = checkBoolean(info, "isPDB");
    isSymmetry = checkBoolean(info, "someModelsHaveSymmetry");
    isUnitCell = checkBoolean(info, "someModelsHaveUnitcells");
    isMultiFrame = (viewer.getModelCount() > 1);
    isMultiConfiguration = (viewer.getAltLocListInModel(modelIndex) != null);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex));
    haveCharges = (viewer.havePartialCharges());

    updateSelectMenu();
    updateElementsComputedMenu(viewer.getElementsPresentBitSet());
    updateHeteroComputedMenu(viewer.getHeteroList(modelIndex));
    updateSurfMoComputedMenu((Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,"moData"));
    updateAaresiduesComputedMenu(viewer.getGroupsPresentBitSet());
    updateModelSetComputedMenu();
    updateFileTypeDependentMenus();
    updateAboutSubmenu();
  }

  private void updateForShow() {
    updateMode = UPDATE_SHOW;
    updateSelectMenu();
    updateModelSetComputedMenu();
    updateAboutSubmenu();
  }
  
  boolean checkBoolean(Hashtable info, String key) {
    if (info == null || !info.containsKey(key))
        return false;
    return ((Boolean)(info.get(key))).booleanValue();
  }

  void updateSelectMenu() {
    Object selectMenu = htMenus.get("selectMenu");
    if (selectMenu == null)
      return;
    setLabel(selectMenu, GT._("Select ({0})", viewer.getSelectionCount(), true));
  }
  
  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    Object elementsComputedMenu = htMenus.get("elementsComputedMenu");
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
        addMenuItem(elementsComputedMenu, entryName, elementName, null);
      }
    }
    for (int i = JmolConstants.firstIsotope; i < JmolConstants.altElementMax; ++i) {
      int n = JmolConstants.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = JmolConstants.altElementNumberFromIndex(i);
        String elementName = JmolConstants.elementNameFromNumber(n);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        addMenuItem(elementsComputedMenu, entryName, elementName, null);
      }
    }
    enableMenu(elementsComputedMenu, true);
  }

  void updateHeteroComputedMenu(Hashtable htHetero) {
    Object heteroComputedMenu = htMenus.get("heteroComputedMenu");
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
        addMenuItem(heteroComputedMenu, entryName, heteroCode, null);
        n++;
    }
    enableMenu(heteroComputedMenu, (n > 0));
  }

  void updateSurfMoComputedMenu(Hashtable moData) {
    Object surfMoComputedMenu = htMenus.get("surfMoComputedMenu");
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
      addMenuItem(surfMoComputedMenu, entryName, script, null);
    }
  }

  void updateAaresiduesComputedMenu(BitSet groupsPresentBitSet) {
    Object aaresiduesComputedMenu = htMenus.get("aaresiduesComputedMenu");
    if (aaresiduesComputedMenu == null)
      return;
    removeAll(aaresiduesComputedMenu);
    enableMenu(aaresiduesComputedMenu, false);
    if (groupsPresentBitSet == null)
      return;
    for (int i = 1; i < JmolConstants.GROUPID_AMINO_MAX; ++i) {
      if (groupsPresentBitSet.get(i)) {
        String aaresidueName = JmolConstants.predefinedGroup3Names[i];
        addMenuItem(aaresiduesComputedMenu, aaresidueName, null, null);
      }
    }
    enableMenu(aaresiduesComputedMenu, true);
  }

  Object CONFIGURATIONComputedMenu;
  Object FRAMESbyModelComputedMenu;
  
  void updateFRAMESbyModelComputedMenu() {
    //allowing this in case we move it later
    FRAMESbyModelComputedMenu = htMenus.get("FRAMESbyModelComputedMenu");
    int modelCount = viewer.getModelCount();
    enableMenu(FRAMESbyModelComputedMenu, (modelCount > 1));
    setLabel(FRAMESbyModelComputedMenu, (modelIndex < 0 ? GT._("All {0} models", viewer.getModelCount(), true) : getModelLabel()));
    removeAll(FRAMESbyModelComputedMenu);
    if (modelCount < 2)
      return;
    addCheckboxMenuItem(FRAMESbyModelComputedMenu, GT._("all", true), "frame 0 #", null, (modelIndex < 0));
    for (int i = 0; i < modelCount; i++) {
      String script = "" + viewer.getModelNumber(i);
      String entryName = viewer.getModelName(i);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
      if (entryName.length() > 30)
        entryName = entryName.substring(0, 20) + "...";
      addCheckboxMenuItem(FRAMESbyModelComputedMenu, entryName, "model " + script + " #", null, (modelIndex == i));
    }
  }

  String configurationSelected = "";
  
  void updateCONFIGURATIONComputedMenu() {
    CONFIGURATIONComputedMenu = htMenus.get("CONFIGURATIONComputedMenu");
    enableMenu(CONFIGURATIONComputedMenu, isMultiConfiguration);
    if (!isMultiConfiguration)
      return;
    String altlocs = viewer.getAltLocListInModel(modelIndex);
    int nAltLocs = altlocs.length();
    setLabel(CONFIGURATIONComputedMenu, GT._("Configurations ({0})", nAltLocs, true));
    removeAll(CONFIGURATIONComputedMenu);
    String script = "hide none #CONFIG";
    addCheckboxMenuItem(CONFIGURATIONComputedMenu, GT._("all", true), script, null,
        (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    for (int i = 0; i < nAltLocs; i++) {
      script = "configuration " + (i + 1) + "; hide not selected #CONFIG";
      String entryName = "" + (i + 1) + " -- \"" + altlocs.charAt(i) + "\"";
      addCheckboxMenuItem(CONFIGURATIONComputedMenu, entryName, script, null,
          (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    }
  }

  void updateModelSetComputedMenu() {
    Object modelSetComputedMenu = htMenus.get("modelSetMenu");
    if (modelSetComputedMenu == null)
      return;
    removeAll(modelSetComputedMenu);
    renameMenu(modelSetComputedMenu, nullModelSetName);
    enableMenu(modelSetComputedMenu, false);
    String modelSetName = viewer.getModelSetName();
    if (modelSetName == null)
      return;
    renameMenu(modelSetComputedMenu, viewer
        .getBooleanProperty("hideNameInPopup") ? hiddenModelSetName
        : modelSetName);
    enableMenu(modelSetComputedMenu, true);
    if (isMultiFrame) {
      updateFRAMESbyModelComputedMenu();
      addMenuSubMenu(modelSetComputedMenu, FRAMESbyModelComputedMenu);
    }
    if (isMultiConfiguration) {
      updateMode = UPDATE_CONFIG;
      updateCONFIGURATIONComputedMenu();
      addMenuSubMenu(modelSetComputedMenu, CONFIGURATIONComputedMenu);
    }
    addMenuSeparator(modelSetComputedMenu);
    addMenuItem(modelSetComputedMenu, GT._("atoms: {0}", viewer
        .getAtomCountInModel(modelIndex), true));
    addMenuItem(modelSetComputedMenu, GT._("bonds: {0}", viewer
        .getBondCountInModel(modelIndex), true));
    addMenuSeparator(modelSetComputedMenu);
    if (isPDB) {
      addMenuItem(modelSetComputedMenu, GT._("groups: {0}", viewer
          .getGroupCountInModel(modelIndex), true));
      addMenuItem(modelSetComputedMenu, GT._("chains: {0}", viewer
          .getChainCountInModel(modelIndex), true));
      addMenuItem(modelSetComputedMenu, GT._("polymers: {0}", viewer
          .getPolymerCountInModel(modelIndex), true));
      addMenuSeparator(modelSetComputedMenu);
    }
    if (isApplet && viewer.showModelSetDownload()
        && !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuItem(modelSetComputedMenu, GT._("View {0}", viewer
          .getModelSetFileName(), true), viewer.getModelSetPathName(), null);
    }
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
    for (int i = 0; i < SymmetryOnly.size(); i++)
      enableMenu(SymmetryOnly.get(i), isSymmetry && isUnitCell);
    for (int i = 0; i < AppletOnly.size(); i++)
      enableMenu(AppletOnly.get(i), isApplet);
    for (int i = 0; i < ChargesOnly.size(); i++)
      enableMenu(ChargesOnly.get(i), haveCharges);
  }
  
  String getModelLabel() {
    return GT._("model {0}",(modelIndex + 1) + "/" + viewer.getModelCount(), true);
  }
  
  private void updateAboutSubmenu() {
    Object aboutComputedMenu = htMenus.get("aboutComputedMenu");
    if (aboutComputedMenu == null)
      return;
    for (int i = getMenuItemCount(aboutComputedMenu); --i >= aboutComputedMenuBaseCount; )
      removeMenuItem(aboutComputedMenu, i);
    addMenuSeparator(aboutComputedMenu);
    addMenuItem(aboutComputedMenu, "Jmol " + JmolConstants.version);
    addMenuItem(aboutComputedMenu, JmolConstants.date);
    addMenuItem(aboutComputedMenu, viewer.getOperatingSystemName());
    addMenuItem(aboutComputedMenu, viewer.getJavaVendor());
    addMenuItem(aboutComputedMenu, viewer.getJavaVersion());
    addMenuSeparator(aboutComputedMenu);
    addMenuItem(aboutComputedMenu, GT._("Java memory usage", true));
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long mbTotal = convertToMegabytes(runtime.totalMemory());
    long mbFree = convertToMegabytes(runtime.freeMemory());
    long mbMax = convertToMegabytes(maxMemoryForNewerJvm());
    addMenuItem(aboutComputedMenu, GT._("{0} MB total", new Object[] { new Long(mbTotal) }, true));
    addMenuItem(aboutComputedMenu, GT._("{0} MB free", new Object[] { new Long(mbFree) }, true));
    if (mbMax > 0)
      addMenuItem(aboutComputedMenu, GT._("{0} MB maximum", new Object[] { new Long(mbMax) }, true));
    else
      addMenuItem(aboutComputedMenu, GT._("unknown maximum", true));
    int availableProcessors = availableProcessorsForNewerJvm();
    if (availableProcessors > 0)
      addMenuItem(aboutComputedMenu, (availableProcessors == 1) ?
                  GT._("1 processor", true) :
                  GT._("{0} processors", availableProcessors, true));
    else
      addMenuItem(aboutComputedMenu, GT._("unknown processor count", true));
  }

  private long convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512*1024)
      num += 512*1024;
    return num / (1024*1024);
  }

  Hashtable colorMenus = new Hashtable();
  
  private void addMenuItems(String parentId, String key, Object menu,
                            PopupResourceBundle popupResourceBundle) {
    String id = parentId + "." + key;
    String value = popupResourceBundle.getStructure(key);
    Logger.debug(id + " --- " + value);
    if (value == null) {
      addMenuItem(menu, "#" + key, "", "");
      return;
    }
    boolean isColorScheme = false;
    String colorSet = "";
    String colorCode = "";
    StringTokenizer st = new StringTokenizer(value);
    while (st.hasMoreTokens()) {
      Object newMenu = null;
      String script = "";
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
        popupResourceBundle.addStructure(key, colorSet);
        addMenuItems(parentId, key, menu, popupResourceBundle);
        return;
      }
      String word = popupResourceBundle.getWord(item);
      if (item.indexOf("Menu") >= 0) {
        Object subMenu = newMenu(word, id + "." + item);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle);
        // these will need tweaking:
        if ("aboutComputedMenu".equals(item)) {
          aboutComputedMenuBaseCount = getMenuItemCount(subMenu);
        } else if ("modelSetComputedMenu".equals(item)) {
          nullModelSetName = word;
          hiddenModelSetName = popupResourceBundle
              .getWord("hiddenModelSetName");
          enableMenu(subMenu, false);
        }
        newMenu = subMenu;
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        String basename = item.substring(0, item.length() - 8);
        newMenu = addCheckboxMenuItem(menu, word, basename, id + "."
            + item);
        script = "set " + basename + " [true|false]";
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = addMenuItem(menu, word, script, id + "." + item);
      }

      // menus or menu items:
      if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } else if (item.indexOf("Url") >= 0) {
        AppletOnly.add(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.add(newMenu);
      } else if (item.indexOf("nitCell") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } else if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.add(newMenu);
      }

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t" + word
            + "\t" + fixScript(id + "." + item, script);
        str = "addMenuItem('\t" + str + "\t')";
        Logger.info(str);
      }
    }
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
    String basename = what;
    String extension = "";
    int pt;
    if ((pt = what.indexOf(";")) >=0) {
      basename = what.substring(0, pt);
      extension = what.substring(pt);
    }
    if (what.indexOf("#") > 0)
      viewer.script(what);
    else if (viewer.getBooleanProperty(basename) != TF)
      viewer.script("set " + basename + (TF ? " TRUE":" FALSE" + extension));
    if (what.indexOf("#CONFIG") >= 0) {
      configurationSelected = what;
      this.updateModelSetComputedMenu();
    }
  }
  
  String currentMenuItemId = null;
  
  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      restorePopupMenu();
      String script = e.getActionCommand();
      if (script == null || script.length() == 0)
        return;
      String id = getId(e.getSource());
      if (id != null) {
        script = fixScript(id, script);
        currentMenuItemId = id;
      }
      viewer.script(script);
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

  String fixScript(String id, String script) {
    int pt;
    if (script == "" || id.endsWith("Checkbox"))
      return script;
    if (id.indexOf("Url") >= 0)
      return "show url \"" + script + "\"";
    if (id.indexOf(".setSpin") >= 0) {
      // setSpinFooMenu
      pt = id.lastIndexOf(".setSpin");
      return (pt < 0 ? script : "set spin "
          + id.substring(pt + 8, id.indexOf("Menu", pt)) + " " + script);
    }
    if (id.indexOf("._") >= 0) {
      // setFooMenu
      pt = id.lastIndexOf("._");
      return (pt < 0 ? script : 
          id.substring(pt + 2, id.indexOf("Menu", pt)) + " " + script);
    }
    if (id.indexOf(".set") >= 0) {
      // setFooMenu
      pt = id.lastIndexOf(".set");
      return (pt < 0 ? script : "set "
          + id.substring(pt + 4, id.indexOf("Menu", pt)) + " " + script);
    }
    if (id.indexOf(".select") >= 0) {
      // select item but not selectMenu set selectionHalos
      return (script.indexOf("set") == 0 ? script : "select " + script);
    }
    if (id.indexOf(".color") >= 0) {
      // colorFooMenu
      pt = id.lastIndexOf(".color");
      return (pt < 0 ? script : "color "
          + id.substring(pt + 6, id.indexOf("Menu", pt)) + " " + script);
    }
    return script;
  }
  
  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItem(menuItem, entry, "", null);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename, String id) {
    Object item = addCheckboxMenuItem(menu, entry, basename, id, false); 
    rememberCheckbox(basename, item);
    return item; 
  }

  public void show(int x, int y) {
    String id = currentMenuItemId;
    updateForShow();
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements();) {
      String key = (String) keys.nextElement();
      Object item = htCheckbox.get(key);
      String basename = (key.indexOf(";") >= 0 ? key.substring(0, key
          .indexOf(";")) : key);
      boolean b = viewer.getBooleanProperty(basename);
      //System.out.println("set " + basename + " " + b +"#" + key + " " + getId(item));
      setCheckBoxState(item, b);
    }
    if (x < 0) {
      x = -x;
      setFrankMenu(id);
      if (nFrankList > 0) {
        showFrankMenu(x - 50, y - nFrankList * getMenuItemHeight());
        return;
      }
    } 
    restorePopupMenu();
    showPopupMenu(x, y);
  }

  Object[][] frankList = new Object[10][]; //enough to cover menu drilling
  int nFrankList = 0;
  String currentFrankId = null;
  void setFrankMenu(String id) {
    if (currentFrankId != null && currentFrankId == id && nFrankList > 0)
      return;
    if (frankPopup == null)
      createFrankPopup();
    resetFrankMenu();
    if (id == null)
      return;
    currentFrankId = id;
    nFrankList = 0;
    for (int i = id.indexOf(".", 2) + 1;;) {
      int iNew = id.indexOf(".", i);
      if (iNew < 0)
        break;
      String strMenu = id.substring(i, iNew);
      Object menu = htMenus.get(strMenu);
      frankList[nFrankList++] = new Object[] {getParent(menu), menu, new Integer(getPosition(menu)) };
      addMenuSubMenu(frankPopup, menu);
      i = iNew + 1;
    }
  }

  void restorePopupMenu() {
    if (nFrankList == 0)
      return;
    for (int i = nFrankList; --i >= 0;) {
      insertMenuSubMenu(frankList[i][0], frankList[i][1], ((Integer)frankList[i][2]).intValue());
    }
    nFrankList = 0;
  }
  ////////////////////////////////////////////////////////////////

  abstract void resetFrankMenu();

  abstract Object getParent(Object menu);

  abstract void insertMenuSubMenu(Object menu, Object subMenu, int index);
  
  abstract int getPosition(Object menu);

  abstract void showPopupMenu(int x, int y);

  abstract void showFrankMenu(int x, int y);

  abstract void setCheckBoxState(Object item, boolean state);
  
  abstract void addMenuSeparator(Object menu);

  abstract Object addMenuItem(Object menu, String entry, String script, String id);

  abstract void setLabel(Object menu, String entry);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract Object addCheckboxMenuItem(Object menu, String entry,
                                      String basename, String id,
                                      boolean state);

  abstract void addMenuSubMenu(Object menu, Object subMenu);

  abstract Object newMenu(String menuName, String id);

  abstract void enableMenu(Object menu, boolean enable);

  abstract void enableMenuItem(Object item, boolean enable);

  abstract void renameMenu(Object menu, String menuName);

  abstract void removeAll(Object menu);

  abstract int getMenuItemCount(Object menu);

  abstract void removeMenuItem(Object menu, int index);
  
  abstract String getId(Object menuItem);

  abstract void setCheckBoxValue(Object source);
  
  abstract void createFrankPopup();

  abstract int getMenuItemHeight();

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

