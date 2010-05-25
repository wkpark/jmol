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

import org.jmol.api.*;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class SimplePopup {

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
  protected final static boolean dumpList = false;

  //public void finalize() {
  //  System.out.println("JmolPopup " + this + " finalize");
  //}

  JmolViewer viewer;
  protected Component display;
  protected MenuItemListener mil;
  protected CheckboxMenuItemListener cmil;
  protected boolean asPopup = true;

  protected Properties menuText = new Properties();
  
  protected String nullModelSetName, modelSetName;
  protected String modelSetFileName, modelSetRoot;
  
  protected Hashtable modelSetInfo, modelInfo;
  protected JPopupMenu frankPopup;

  protected Hashtable htMenus = new Hashtable();
  protected Vector PDBOnly = new Vector();
  protected Vector UnitcellOnly = new Vector();
  protected Vector FramesOnly = new Vector();
  protected Vector VibrationOnly = new Vector();
  protected Vector SymmetryOnly = new Vector();
  protected Vector SignedOnly = new Vector();
  protected Vector AppletOnly = new Vector();
  protected Vector ChargesOnly = new Vector();
  protected Vector TemperatureOnly = new Vector();

  protected boolean isPDB;
  protected boolean isSymmetry;
  protected boolean isUnitCell;
  protected boolean isMultiFrame;
  protected boolean isMultiConfiguration;
  protected boolean isVibration;
  protected boolean isApplet;
  protected boolean isSigned;
  protected boolean isZapped;
  protected boolean haveCharges;
  protected boolean haveBFactors;
  protected String altlocs;

  protected int modelIndex, modelCount, atomCount;

  protected JPopupMenu swingPopup;

  SimplePopup(JmolViewer viewer) {
    this.viewer = viewer;
    asPopup = true;
    display = viewer.getDisplay();
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
  }

  public SimplePopup(JmolViewer viewer, String title, PopupResource bundle) {
    this(viewer);
    build(title, swingPopup = new JPopupMenu(title), bundle);
  }

  protected void build(String title, Object popupMenu, PopupResource bundle) {
    htMenus.put(title, popupMenu);
    boolean allowSignedFeatures = (!viewer.isApplet() || viewer.getBooleanProperty("_signedApplet"));
    addMenuItems("", title, popupMenu, bundle, 
        allowSignedFeatures);
  }

  protected int thisx, thisy;

  public void show(int x, int y) {
    thisx = x;
    thisy = y;
    showPopupMenu(thisx, thisy);
  }

  static protected void addCurrentItem(StringBuffer sb, char type, int level, String name, String label, String script, String flags) {
    sb.append(type).append(level).append('\t').append(name);
    if(label == null) {
      sb.append(".\n");
      return;
    }
    sb.append("\t").append(label)
        .append("\t").append(script == null || script.length() == 0 ? "-" : script)
        .append("\t").append(flags)
        .append("\n");
  }

  final static int UPDATE_ALL = 0;
  final static int UPDATE_CONFIG = 1;
  final static int UPDATE_SHOW = 2;
  int updateMode;

  protected String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }

  boolean checkBoolean(Hashtable info, String key) {
    if (info == null || !info.containsKey(key))
      return false;
    return ((Boolean) (info.get(key))).booleanValue();
  }

  protected void getViewerData() {
    isApplet = viewer.isApplet();
    isSigned = (viewer.getBooleanProperty("_signedApplet"));
    modelSetName = viewer.getModelSetName();
    modelSetFileName = viewer.getModelSetFileName();
    int i = modelSetFileName.lastIndexOf(".");
    isZapped = ("zapped".equals(modelSetName));
    if (isZapped || "string".equals(modelSetFileName) 
        || "files".equals(modelSetFileName) 
        || "string[]".equals(modelSetFileName))
      modelSetFileName = "";
    modelSetRoot = modelSetFileName.substring(0, i < 0 ? modelSetFileName.length() : i);
    if (modelSetRoot.length() == 0)
      modelSetRoot = "Jmol";
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
    haveBFactors = (viewer.getBooleanProperty("haveBFactors"));
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
    for (int i = 0; i < SignedOnly.size(); i++)
      enableMenu(SignedOnly.get(i), isSigned || !isApplet);
    for (int i = 0; i < AppletOnly.size(); i++)
      enableMenu(AppletOnly.get(i), isApplet);
    for (int i = 0; i < ChargesOnly.size(); i++)
      enableMenu(ChargesOnly.get(i), haveCharges);
    for (int i = 0; i < TemperatureOnly.size(); i++)
      enableMenu(TemperatureOnly.get(i), haveBFactors);
  }

  protected void addMenuItems(String parentId, String key, Object menu,
                            PopupResource popupResourceBundle,
                            boolean allowSignedFeatures) {
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
      String word = popupResourceBundle.getWord(item);
      if (item.indexOf("Menu") >= 0) {
        if (!allowSignedFeatures && item.startsWith("SIGNED"))
          continue;
        Object subMenu = newMenu(word, id + "." + item);        
        addMenuSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle, allowSignedFeatures);
        checkSpecialMenu(item, subMenu, word);
        newMenu = subMenu;
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
      }

      if (item.indexOf("VARIABLE") >= 0)
        htMenus.put(item, newMenu);
      // menus or menu items:
      if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } else if (item.indexOf("URL") >= 0) {
        AppletOnly.add(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.add(newMenu);
      } else if (item.indexOf("BFACTORS") >= 0) {
        TemperatureOnly.add(newMenu);
      } else if (item.indexOf("UNITCELL") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } else if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.add(newMenu);
      }
      if (item.startsWith("SIGNED"))
        SignedOnly.add(newMenu);

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
            + word + "\t" + fixScript(id + "." + item, script);
        str = "addMenuItem('\t" + str + "\t')";
        Logger.info(str);
      }
    }
  }

  protected void checkSpecialMenu(String item, Object subMenu, String word) {
    // special considerations here
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
   * @return possibly modified script or ""
   */
  String setCheckBoxValue(String what, boolean TF) {
    int pt;
    if (what.indexOf("##") < 0) {
      // not a special computed checkbox
      String basename = what.substring(0, (pt = what.indexOf(":")));
      if (viewer.getBooleanProperty(basename) == TF)
        return "";
      what = what.substring(pt + 1);
      if ((pt = what.indexOf("|")) >= 0)
        what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
      what = TextFormat.simpleReplace(what, "T/F", (TF ? " TRUE" : " FALSE"));
    }
    viewer.evalStringQuiet(what);
    return what;
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
      id = id.replace('_', ' ');
      if (script.indexOf("[]") < 0)
        script = "[] " + script;
      return TextFormat.simpleReplace(script, "[]", id); 
    } else if (script.indexOf("?FILEROOT?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILEROOT?", modelSetRoot);
    } else if (script.indexOf("?FILE?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILE?", modelSetFileName);
    } else if (script.indexOf("?PdbId?") >= 0) {
      script = TextFormat.simpleReplace(script, "PdbId?", "=xxxx");
    }
    return script;
  }

  void restorePopupMenu() {
    // main menu only 
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

  ////////////////////////////////////////////////////////////////

  protected void showPopupMenu(int x, int y) {
    if (display == null)
      return;
    try {
      swingPopup.show(display, x, y);
    } catch (Exception e) {
      Logger.error("popup error: " + e.getMessage());
      // browser in Java 1.6.0_10 is blocking setting WindowAlwaysOnTop 

    }
  }

  void addToMenu(Object menu, JComponent item) {
    if (menu instanceof JPopupMenu) {
      ((JPopupMenu) menu).add(item);
    } else if (menu instanceof JMenu) {
      ((JMenu) menu).add(item);
    } else {
      Logger.warn("cannot add object to menu: " + menu);
    }
  }

  ////////////////////////////////////////////////////////////////

  void addMenuSeparator(Object menu) {
    if (menu instanceof JPopupMenu)
      ((JPopupMenu) menu).addSeparator();
    else
      ((JMenu) menu).addSeparator();
  }

  Object addMenuItem(Object menu, String entry, String script, String id) {
    JMenuItem jmi = new JMenuItem(entry);
    updateMenuItem(jmi, entry, script);
    jmi.addActionListener(mil);
    jmi.setName(id == null ? ((Component) menu).getName() + "." : id);
    addToMenu(menu, jmi);
    return jmi;
  }

  void setLabel(Object menu, String entry) {
    if (menu instanceof JMenuItem)
      ((JMenuItem) menu).setLabel(entry);
    else
      ((JMenu) menu).setLabel(entry);
  }

  String getId(Object menu) {
    return ((Component) menu).getName();
  }

  void setCheckBoxValue(Object source) {
    JCheckBoxMenuItem jcmi = (JCheckBoxMenuItem) source;
    setCheckBoxValue(jcmi.getActionCommand(), jcmi.getState());
  }

  void setCheckBoxState(Object item, boolean state) {
    ((JCheckBoxMenuItem) item).setState(state);
  }

  void updateMenuItem(Object menuItem, String entry, String script) {
    JMenuItem jmi = (JMenuItem) menuItem;
    jmi.setLabel(entry);
    jmi.setActionCommand(script);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename,
                             String id, boolean state) {
    JCheckBoxMenuItem jcmi = new JCheckBoxMenuItem(entry);
    jcmi.setState(state);
    jcmi.addItemListener(cmil);
    jcmi.setActionCommand(basename);
    jcmi.setName(id == null ? ((Component) menu).getName() + "." : id);
    addToMenu(menu, jcmi);
    return jcmi;
  }

  Object cloneMenu(Object menu) {
    return null;
  }

  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (JMenu) subMenu);
  }

  Object newMenu(String menuName, String id) {
    JMenu jm = new JMenu(menuName);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  void setAutoscrolls(Object menu) {
    ((JMenu) menu).setAutoscrolls(true);
  }

  void renameMenu(Object menu, String newMenuName) {
    ((JMenu) menu).setLabel(newMenuName);
  }

  int getMenuItemCount(Object menu) {
    return ((JMenu) menu).getItemCount();
  }

  void removeMenuItem(Object menu, int index) {
    ((JMenu) menu).remove(index);
  }

  void removeAll(Object menu) {
    ((JMenu) menu).removeAll();
  }

  void enableMenu(Object menu, boolean enable) {
    if (menu instanceof JMenuItem) {
      enableMenuItem(menu, enable);
      return;
    }
    try {
      ((JMenu) menu).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

  void enableMenuItem(Object item, boolean enable) {
    try {
      ((JMenuItem) item).setEnabled(enable);
    } catch (Exception e) {
      //no menu item;
    }
  }

}
