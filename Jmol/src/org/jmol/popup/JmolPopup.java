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
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;
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
  Properties menuText = new Properties();
  
  Object frankPopup;

  int aboutComputedMenuBaseCount;
  String nullModelSetName, modelSetName;
  
  Hashtable modelSetInfo, modelInfo;
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
  boolean isZapped;
  boolean haveCharges;
  String altlocs;

  int modelIndex, modelCount, atomCount;

  final static int MAX_ITEMS = 25;

  static String menuStructure;
  
  JmolPopup(JmolViewer viewer) {
    this.viewer = viewer;
    jmolComponent = viewer.getAwtComponent();
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
  }

  static public JmolPopup newJmolPopup(JmolViewer viewer, boolean doTranslate, String menu) {
    menuStructure = menu;
    GT.setDoTranslate(true);
    JmolPopup popup;
    try {
      popup = (!viewer.isJvm12orGreater() || forceAwt ? (JmolPopup) new JmolPopupAwt(
          viewer)
          : (JmolPopup) new JmolPopupSwing(viewer));
    } catch (Exception e) {
      Logger.error("JmolPopup not loaded");
      return null;
    }
    // long runTime = System.currentTimeMillis() - beginTime;
    // Logger.debug("LoadPopupThread finished " + runTime + " ms");
    try {
      popup.updateComputedMenus();
    } catch (NullPointerException e) {
      // ignore -- the frame just wasn't ready yet;
      // updateComputedMenus() will be called again when the frame is ready; 
    }
    GT.setDoTranslate(doTranslate);
    return popup;
  }

  void build(Object popupMenu) {
    addMenuItems("", "popupMenu", popupMenu, new PopupResourceBundle(menuStructure, menuText), viewer
        .isJvm12orGreater());
  }

  public String getMenu(String title) {
    return (new PopupResourceBundle(menuStructure, null)).getMenu(title);
  }
  
  final static int UPDATE_ALL = 0;
  final static int UPDATE_CONFIG = 1;
  final static int UPDATE_SHOW = 2;
  int updateMode;

  private String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }


  public void updateComputedMenus() {
    updateMode = UPDATE_ALL;
    getViewerData();
    updateSelectMenu();
    updateElementsComputedMenu(viewer.getElementsPresentBitSet(modelIndex));
    updateHeteroComputedMenu(viewer.getHeteroList(modelIndex));
    updateSurfMoComputedMenu((Hashtable) modelInfo.get("moData"));
    updateFileTypeDependentMenus();
    updatePDBComputedMenus();
    updateMode = UPDATE_CONFIG;
    updateConfigurationComputedMenu();
    updateSYMMETRYComputedMenu();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateLanguageSubmenu();
    updateAboutSubmenu();
  }

  private void getViewerData() {
    isApplet = viewer.isApplet();
    modelSetName = viewer.getModelSetName();
    isZapped = (modelSetName == "zapped");
    modelIndex = viewer.getDisplayModelIndex();
    modelCount = viewer.getModelCount();
    atomCount = viewer.getAtomCountInModel(modelIndex);
    modelSetInfo = viewer.getModelSetAuxiliaryInfo();
    modelInfo = viewer.getModelAuxiliaryInfo(modelIndex);
    if (modelInfo == null)
      modelInfo = new Hashtable();
    isPDB = checkBoolean(modelSetInfo, "isPDB");
    isSymmetry = checkBoolean(modelSetInfo, "someModelsHaveSymmetry");
    isUnitCell = checkBoolean(modelSetInfo, "someModelsHaveUnitcells");
    isMultiFrame = (modelCount > 1);
    altlocs = viewer.getAltLocListInModel(modelIndex);
    isMultiConfiguration = (altlocs.length() > 0);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex));
    haveCharges = (viewer.havePartialCharges());
  }

  private void updateForShow() {
    updateMode = UPDATE_SHOW;
    updateSelectMenu();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateAboutSubmenu();
  }

  boolean checkBoolean(Hashtable info, String key) {
    if (info == null || !info.containsKey(key))
      return false;
    return ((Boolean) (info.get(key))).booleanValue();
  }

  void updateSelectMenu() {
    Object menu = htMenus.get("selectMenuText");
    if (menu == null)
      return;
    enableMenu(menu, atomCount != 0);
    setLabel(menu, GT._(getMenuText("selectMenuText"), viewer.getSelectionCount(), true));
  }

  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    Object menu = htMenus.get("elementsComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (elementsPresentBitSet == null)
      return;
    for (int i = 0; i < JmolConstants.elementNumberMax; ++i) {
      if (elementsPresentBitSet.get(i)) {
        String elementName = JmolConstants.elementNameFromNumber(i);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(i);
        String entryName = elementSymbol + " - " + elementName;
        addMenuItem(menu, entryName, elementName, null);
      }
    }
    for (int i = JmolConstants.firstIsotope; i < JmolConstants.altElementMax; ++i) {
      int n = JmolConstants.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = JmolConstants.altElementNumberFromIndex(i);
        String elementName = JmolConstants.elementNameFromNumber(n);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        addMenuItem(menu, entryName, elementName, null);
      }
    }
    enableMenu(menu, true);
  }

  void updateHeteroComputedMenu(Hashtable htHetero) {
    Object menu = htMenus.get("PDBheteroComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (htHetero == null)
      return;
    Enumeration e = htHetero.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String heteroCode = (String) e.nextElement();
      String heteroName = (String) htHetero.get(heteroCode);
      if (heteroName.length() > 20)
        heteroName = heteroName.substring(0, 20) + "...";
      String entryName = heteroCode + " - " + heteroName;
      addMenuItem(menu, entryName, "[" + heteroCode + "]", null);
      n++;
    }
    enableMenu(menu, (n > 0));
  }

  void updateSurfMoComputedMenu(Hashtable moData) {
    Object menu = htMenus.get("surfMoComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, false);
    removeAll(menu);
    if (moData == null)
      return;
    Vector mos = (Vector) (moData.get("mos"));
    int nOrb = (mos == null ? 0 : mos.size());
    if (nOrb == 0)
      return;
    enableMenu(menu, true);
    Object subMenu = menu;
    int nmod = (nOrb % MAX_ITEMS);
    if (nmod == 0)
      nmod = MAX_ITEMS;
    int pt = (nOrb > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = nOrb; --i >= 0;) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        if (pt == nmod + 1)
          nmod = MAX_ITEMS;
        String id = "mo" + pt + "Menu";
        subMenu = newMenu(Math.max(i + 2 - nmod, 1) + "..." + (i + 1),
            getId(menu) + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      Hashtable mo = (Hashtable) mos.get(i);
      String type = (mo.containsKey("type") ? (String)mo.get("type") : "");
      String entryName = "#" + (i + 1) + " " + mo.get("energy") + " " + type;
      String script = "mo " + (i + 1);
      addMenuItem(subMenu, entryName, script, null);
    }
  }

  void updatePDBComputedMenus() {

    Object menu = htMenus.get("PDBaaResiduesComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);

    Object menu1 = htMenus.get("PDBnucleicResiduesComputedMenu");
    if (menu1 == null)
      return;
    removeAll(menu1);
    enableMenu(menu1, false);

    Object menu2 = htMenus.get("PDBcarboResiduesComputedMenu");
    if (menu2 == null)
      return;
    removeAll(menu2);
    enableMenu(menu2, false);
    if (modelSetInfo == null)
      return;
    int n = (modelIndex < 0 ? modelCount : modelIndex);
    String[] lists = ((String[]) modelSetInfo.get("group3Lists"));
    group3List = (lists == null ? null : lists[n]);
    group3Counts = (lists == null ? null : ((int[][]) modelSetInfo
        .get("group3Counts"))[n]);

    if (group3List == null)
      return;
    //next is correct as "<=" because it includes "UNK"
    int nItems = 0;
    for (int i = 1; i <= JmolConstants.GROUPID_AMINO_MAX; ++i)
      nItems += updateGroup3List(menu, JmolConstants.predefinedGroup3Names[i]);
    nItems += augmentGroup3List(menu, "p>", true);
    enableMenu(menu, (nItems > 0));
    enableMenu(htMenus.get("PDBproteinMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu1, "n>", false);
    enableMenu(menu1, nItems > 0);
    enableMenu(htMenus.get("PDBnucleicMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu2, "c>", false);
    enableMenu(menu2, nItems > 0);
    enableMenu(htMenus.get("PDBcarboMenu"), (nItems > 0));
  }

  String group3List;
  int[] group3Counts;

  int updateGroup3List(Object menu, String name) {
    int nItems = 0;
    int n = group3Counts[group3List.indexOf(name) / 6];
    if (n > 0) {
      name += "  (" + n + ")";
      nItems++;
    }
    Object item = addMenuItem(menu, name, null, getId(menu) + "." + name);
    if (n == 0)
      enableMenuItem(item, false);
    return nItems;
  }

  int augmentGroup3List(Object menu, String type, boolean addSeparator) {
    int pt = JmolConstants.GROUPID_AMINO_MAX * 6;
    // ...... p>AFN]o>ODH]n>+T ]
    int nItems = 0;
    while (true) {
      pt = group3List.indexOf(type, pt);
      if (pt < 0)
        break;
      if (nItems++ == 0 && addSeparator)
        addMenuSeparator(menu);
      int n = group3Counts[pt / 6];
      String name = group3List.substring(pt + 2, pt + 5) + "  (" + n + ")";
      addMenuItem(menu, name, null, getId(menu) + "." + name);
      pt++;
    }
    return nItems;
  }

  void updateSYMMETRYComputedMenu() {
    Object menu = htMenus.get("SYMMETRYComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (!isSymmetry || modelIndex < 0)
      return;
    String[] list = (String[]) modelInfo.get("symmetryOperations");
    if (list == null)
      return;
    int[] cellRange = (int[]) modelInfo.get("unitCellRange");
    boolean haveUnitCellRange = (cellRange != null);
    Object subMenu = menu;
    int nmod = MAX_ITEMS;
    int pt = (list.length > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < list.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "symop" + pt + "Menu";
        subMenu = newMenu((i + 1) + "..."
            + Math.min(i + MAX_ITEMS, list.length), getId(menu) + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = "symop=" + (i + 1) + " # " + list[i];
      enableMenuItem(addMenuItem(subMenu, entryName, "symop=" + (i + 1), null),
          haveUnitCellRange);
    }
    enableMenu(menu, true);
  }

  void updateFRAMESbyModelComputedMenu() {
    //allowing this in case we move it later
    Object menu = htMenus.get("FRAMESbyModelComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, (modelCount > 1));
    setLabel(menu, (modelIndex < 0 ? GT._(getMenuText("allModelsText"), modelCount, true)
        : getModelLabel()));
    removeAll(menu);
    if (modelCount < 2)
      return;
    addCheckboxMenuItem(menu, GT._("all", true), "frame 0 ##", null,
        (modelIndex < 0));

    Object subMenu = menu;
    int nmod = MAX_ITEMS;
    int pt = (modelCount > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < modelCount; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "model" + pt + "Menu";
        subMenu = newMenu(
            (i + 1) + "..." + Math.min(i + MAX_ITEMS, modelCount), getId(menu)
                + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String script = "" + viewer.getModelName(-1 - i);
      String entryName = viewer.getModelName(i);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
      if (entryName.length() > 30)
        entryName = entryName.substring(0, 20) + "...";
      addCheckboxMenuItem(subMenu, entryName, "model " + script + " ##", null,
          (modelIndex == i));
    }
  }

  String configurationSelected = "";

  void updateConfigurationComputedMenu() {
    Object menu = htMenus.get("configurationComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, isMultiConfiguration);
    if (!isMultiConfiguration)
      return;
    int nAltLocs = altlocs.length();
    setLabel(menu, GT._(getMenuText("configurationMenuText"), nAltLocs, true));
    removeAll(menu);
    String script = "hide none ##CONFIG";
    addCheckboxMenuItem(menu, GT._("all", true), script, null,
        (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    for (int i = 0; i < nAltLocs; i++) {
      script = "configuration " + (i + 1) + "; hide thisModel and not selected ##CONFIG";
      String entryName = "" + (i + 1) + " -- \"" + altlocs.charAt(i) + "\"";
      addCheckboxMenuItem(menu, entryName, script, null,
          (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    }
  }

  void updateModelSetComputedMenu() {
    Object menu = htMenus.get("modelSetMenu");
    if (menu == null)
      return;
    removeAll(menu);
    renameMenu(menu, nullModelSetName);
    enableMenu(menu, false);
    enableMenu(htMenus.get("surfaceMenu"), !isZapped);
    enableMenu(htMenus.get("measureMenu"), !isZapped);
    enableMenu(htMenus.get("pickingMenu"), !isZapped);
    if (modelSetName == null || isZapped)
      return;
    if (isMultiFrame) {
      modelSetName = GT._(getMenuText("modelSetCollectionText"), modelCount);
    } else if (viewer.getBooleanProperty("hideNameInPopup")) {
      modelSetName = getMenuText("hiddenModelSetText");
    }
    renameMenu(menu, modelSetName);
    enableMenu(menu, true);
    addMenuSeparator(menu);
    addMenuItem(menu, GT._(getMenuText("atomsText"), atomCount, true));
    addMenuItem(menu, GT._(getMenuText("bondsText"),
        viewer.getBondCountInModel(modelIndex), true));
    if (isPDB) {
      addMenuSeparator(menu);
      addMenuItem(menu, GT._(getMenuText("groupsText"), viewer
          .getGroupCountInModel(modelIndex), true));
      addMenuItem(menu, GT._(getMenuText("chainsText"), viewer
          .getChainCountInModel(modelIndex), true));
      addMenuItem(menu, GT._(getMenuText("polymersText"), viewer
          .getPolymerCountInModel(modelIndex), true));
    }
    if (isApplet && viewer.showModelSetDownload()
        && !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuSeparator(menu);
      addMenuItem(menu, GT._(getMenuText("viewMenuText"), viewer.getModelSetFileName(), true),
          "show url", null);
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
    return GT._(getMenuText("modelMenuText"), (modelIndex + 1) + "/" + modelCount, true);
  }

  private void updateAboutSubmenu() {
    Object menu = htMenus.get("aboutComputedMenu");
    if (menu == null)
      return;
    for (int i = getMenuItemCount(menu); --i >= aboutComputedMenuBaseCount;)
      removeMenuItem(menu, i);
    addMenuSeparator(menu);
    addMenuItem(menu, "Jmol " + JmolConstants.version);
    addMenuItem(menu, JmolConstants.date);
    addMenuItem(menu, viewer.getOperatingSystemName());
    addMenuItem(menu, viewer.getJavaVendor());
    addMenuItem(menu, viewer.getJavaVersion());
    addMenuSeparator(menu);
    addMenuItem(menu, GT._("Java memory usage", true));
    Runtime runtime = Runtime.getRuntime();
    //runtime.gc();
    long mbTotal = convertToMegabytes(runtime.totalMemory());
    long mbFree = convertToMegabytes(runtime.freeMemory());
    long mbMax = convertToMegabytes(maxMemoryForNewerJvm());
    addMenuItem(menu, GT._("{0} MB total", new Object[] { new Long(mbTotal) },
        true));
    addMenuItem(menu, GT._("{0} MB free", new Object[] { new Long(mbFree) },
        true));
    if (mbMax > 0)
      addMenuItem(menu, GT._("{0} MB maximum",
          new Object[] { new Long(mbMax) }, true));
    else
      addMenuItem(menu, GT._("unknown maximum", true));
    int availableProcessors = availableProcessorsForNewerJvm();
    if (availableProcessors > 0)
      addMenuItem(menu, (availableProcessors == 1) ? GT._("1 processor", true)
          : GT._("{0} processors", availableProcessors, true));
    else
      addMenuItem(menu, GT._("unknown processor count", true));
  }

  private void updateLanguageSubmenu() {
    Object menu = htMenus.get("languageComputedMenu");
    if (menu == null)
      return;
    for (int i = getMenuItemCount(menu); --i >= 0;)
      removeMenuItem(menu, i);
    String language = GT.getLanguage();
    String id = getId(menu);
    String[][] languages = GT.getLanguageList();
    for (int i = 0; i < languages.length; i++) {
      String code = languages[i][0];
      String name = languages[i][1];
      addCheckboxMenuItem(menu, GT._(name, true) + " (" + code + ")",
          "language = \"" + code + "\" ##" + name, id + "." + code, language
              .equals(code));
    }
  }

  private long convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512 * 1024)
      num += 512 * 1024;
    return num / (1024 * 1024);
  }

  private void addMenuItems(String parentId, String key, Object menu,
                            PopupResourceBundle popupResourceBundle,
                            boolean isJVM12orGreater) {
    String id = parentId + "." + key;
    String value = popupResourceBundle.getStructure(key);
    //Logger.debug(id + " --- " + value);
    if (value == null) {
      addMenuItem(menu, "#" + key, "", "");
      return;
    }
    // process predefined @terms
    StringTokenizer st = new StringTokenizer(value);
    String item;
    while (value.indexOf("@") >= 0) {
      String s = "";
      while (st.hasMoreTokens())
        s += " " + ((item = st.nextToken()).startsWith("@") 
            ? popupResourceBundle.getStructure(item) : item);
      value = s.substring(1);
      st = new StringTokenizer(value);
    }
    while (st.hasMoreTokens()) {
      Object newMenu = null;
      String script = "";
      item = st.nextToken();
      boolean isDisabled = (!isJVM12orGreater && item.indexOf("JVM12") >= 0);
      String word = popupResourceBundle.getWord(item);
      if (item.indexOf("Menu") >= 0) {
        Object subMenu = newMenu(word, id + "." + item);        
        addMenuSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle, isJVM12orGreater);
        // these will need tweaking:
        if ("aboutComputedMenu".equals(item)) {
          aboutComputedMenuBaseCount = getMenuItemCount(subMenu);
        } else if ("modelSetMenu".equals(item)) {
          nullModelSetName = word;
          enableMenu(subMenu, false);
        }
        newMenu = subMenu;
        if (isDisabled)
          enableMenu(newMenu, false);
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        script = popupResourceBundle.getStructure(item);
        String basename = item.substring(0, item.length() - 8);
        if (script == null || script.length() == 0)
          script = "set " + basename + " T/F";
        newMenu = addCheckboxMenuItem(menu, word, basename 
            + ":" + script, id + "." + item);
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = addMenuItem(menu, word, script, id + "." + item);
        if (isDisabled)
          enableMenuItem(newMenu, false);
      }

      // menus or menu items:
      if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } else if (item.indexOf("URL") >= 0) {
        AppletOnly.add(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.add(newMenu);
      } else if (item.indexOf("UNITCELL") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } else if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.add(newMenu);
      }

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
            + word + "\t" + fixScript(id + "." + item, script);
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
    int pt;
    if (what.indexOf("##") < 0) {
      // not a special computed checkbox
      String basename = what.substring(0, (pt = what.indexOf(":")));
      if (viewer.getBooleanProperty(basename) == TF)
        return;
      what = what.substring(pt + 1);
      if ((pt = what.indexOf("|")) >= 0)
        what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
      what = TextFormat.simpleReplace(what, "T/F", (TF ? " TRUE" : " FALSE"));
    }
    viewer.evalStringQuiet(what);
    if (what.indexOf("#CONFIG") >= 0) {
      configurationSelected = what;
      updateConfigurationComputedMenu();
      updateModelSetComputedMenu();
    }
  }

  String currentMenuItemId = null;

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      restorePopupMenu();
      String script = e.getActionCommand();
      if (script == null || script.length() == 0)
        return;
      if (script.equals("MAIN")) {
        show(thisx, thisy);
        return;
      }
      String id = getId(e.getSource());
      if (id != null) {
        script = fixScript(id, script);
        currentMenuItemId = id;
      }
      viewer.evalStringQuiet(script);
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

    if (script.indexOf("SELECT") == 0) {
      return "select thisModel and (" + script.substring(6) + ")";
    }
    
    if ((pt = id.lastIndexOf("[")) >= 0) { 
      // setSpin
      id = id.substring(pt + 1);
      if ((pt = id.indexOf("]")) >= 0)
        id = id.substring(0, pt);
      id = TextFormat.simpleReplace(id, "_", " ");
      if (script.indexOf("[]") < 0)
        script = "[] " + script;
      return TextFormat.simpleReplace(script, "[]", id);
    }
    return script;
  }

  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItem(menuItem, entry, "", null);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename,
                             String id) {
    Object item = addCheckboxMenuItem(menu, entry, basename, id, false);
    rememberCheckbox(basename, item);
    return item;
  }

  int thisx, thisy;

  public void show(int x, int y) {
    thisx = x;
    thisy = y;
    String id = currentMenuItemId;
    updateForShow();
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements();) {
      String key = (String) keys.nextElement();
      Object item = htCheckbox.get(key);
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = viewer.getBooleanProperty(basename);
      setCheckBoxState(item, b);
    }
    if (x < 0) {
      setFrankMenu(id);
      thisx = -x - 50;
      if (nFrankList > 1) {
        thisy = y - nFrankList * getMenuItemHeight();
        showFrankMenu(thisx, thisy);
        return;
      }
    }
    restorePopupMenu();
    showPopupMenu(thisx, thisy);
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
    frankList[nFrankList++] = new Object[] { null, null, null };
    addMenuItem(frankPopup, GT._(getMenuText("mainMenuText")), "MAIN", "");
    for (int i = id.indexOf(".", 2) + 1;;) {
      int iNew = id.indexOf(".", i);
      if (iNew < 0)
        break;
      String strMenu = id.substring(i, iNew);
      Object menu = htMenus.get(strMenu);
      frankList[nFrankList++] = new Object[] { getParent(menu), menu,
          new Integer(getPosition(menu)) };
      addMenuSubMenu(frankPopup, menu);
      i = iNew + 1;
    }
  }

  void restorePopupMenu() {
    if (nFrankList < 2)
      return;
    // first entry is just the main item
    for (int i = nFrankList; --i > 0;) {
      insertMenuSubMenu(frankList[i][0], frankList[i][1],
          ((Integer) frankList[i][2]).intValue());
    }
    nFrankList = 1;
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

  abstract Object addMenuItem(Object menu, String entry, String script,
                              String id);

  abstract void setLabel(Object menu, String entry);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract Object addCheckboxMenuItem(Object menu, String entry,
                                      String basename, String id, boolean state);

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
