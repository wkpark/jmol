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

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Hashtable;

abstract public class GenericPopup {

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
  protected final static boolean dumpList = false;

  //public void finalize() {
  //  System.out.println("JmolPopup " + this + " finalize");
  //}

  protected Viewer viewer;
  protected boolean asPopup = true;

  protected Properties menuText = new Properties();
  
  protected String nullModelSetName, modelSetName;
  protected String modelSetFileName, modelSetRoot;
  
  protected Map<String, Object> modelSetInfo;
  protected Map<String, Object> modelInfo;

  protected Map<String, Object> htMenus = new Hashtable<String, Object>();
  protected List<Object> NotPDB = new ArrayList<Object>();
  protected List<Object> PDBOnly = new ArrayList<Object>();
  protected List<Object> FileUnitOnly = new ArrayList<Object>();
  protected List<Object> FileMolOnly = new ArrayList<Object>();
  protected List<Object> UnitcellOnly = new ArrayList<Object>();
  protected List<Object> SingleModelOnly = new ArrayList<Object>();
  protected List<Object> FramesOnly = new ArrayList<Object>();
  protected List<Object> VibrationOnly = new ArrayList<Object>();
  protected List<Object> SymmetryOnly = new ArrayList<Object>();
  protected List<Object> SignedOnly = new ArrayList<Object>();
  protected List<Object> AppletOnly = new ArrayList<Object>();
  protected List<Object> ChargesOnly = new ArrayList<Object>();
  protected List<Object> TemperatureOnly = new ArrayList<Object>();

  protected boolean fileHasUnitCell;
  protected boolean isPDB;
  protected boolean isSymmetry;
  protected boolean isUnitCell;
  protected boolean isMultiFrame;
  protected boolean isLastFrame;
  protected boolean isMultiConfiguration;
  protected boolean isVibration;
  protected boolean isApplet;
  protected boolean isSigned;
  protected boolean isZapped;
  protected boolean haveCharges;
  protected boolean haveBFactors;
  protected String altlocs;

  protected int modelIndex, modelCount, atomCount;

  GenericPopup() {
    // required by reflection
  }
  
  protected void set(Viewer viewer) {
    asPopup = true;
    this.viewer = viewer;
  }

  private boolean allowSignedFeatures;
  
  protected void build(String title, Object popupMenu, PopupResource bundle) {
    htMenus.put(title, popupMenu);
    allowSignedFeatures = (!viewer.isApplet() || viewer.getBooleanProperty("_signedApplet"));
    addMenuItems("", title, popupMenu, bundle);
  }

  protected int thisx, thisy;

  protected void updateForShow() {
    // depends upon implementation
  }
  
  /**
   * @param x 
   * @param y 
   * @param doPopup  
   */
  public void show(int x, int y, boolean doPopup) {
    thisx = x;
    thisy = y;
    updateForShow();
    for (Map.Entry<String, Object> entry : htCheckbox.entrySet()) {
      String key = entry.getKey();
      Object item = entry.getValue();
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = viewer.getBooleanProperty(basename);
      setCheckBoxState(item, b);
    }
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

  protected String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }

  protected static boolean checkBoolean(Map<String, Object> info, String key) {
    Object value = (info == null ? null : info.get(key));
    return !(value == null || value instanceof Boolean && !((Boolean) (value)).booleanValue());
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
      modelInfo = new Hashtable<String, Object>();
    isPDB = checkBoolean(modelSetInfo, "isPDB");
    isMultiFrame = (modelCount > 1);
    isSymmetry = checkBoolean(modelInfo, "hasSymmetry");
    isUnitCell = checkBoolean(modelInfo, "notionalUnitcell");
    fileHasUnitCell = (isPDB && isUnitCell || checkBoolean(modelInfo, "fileHasUnitCell"));
    isLastFrame = (modelIndex == modelCount - 1);
    altlocs = viewer.getAltLocListInModel(modelIndex);
    isMultiConfiguration = (altlocs.length() > 0);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex));
    haveCharges = (viewer.havePartialCharges());
    haveBFactors = (viewer.getBooleanProperty("haveBFactors"));
  }

  protected void updateFileTypeDependentMenus() {
    for (int i = 0; i < NotPDB.size(); i++)
      enableMenu(NotPDB.get(i), !isPDB);
    for (int i = 0; i < PDBOnly.size(); i++)
      enableMenu(PDBOnly.get(i), isPDB);
    for (int i = 0; i < UnitcellOnly.size(); i++)
      enableMenu(UnitcellOnly.get(i), isUnitCell);
    for (int i = 0; i < FileUnitOnly.size(); i++)
      enableMenu(FileUnitOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = 0; i < FileMolOnly.size(); i++)
      enableMenu(FileMolOnly.get(i), isUnitCell || fileHasUnitCell);
    for (int i = 0; i < SingleModelOnly.size(); i++)
      enableMenu(SingleModelOnly.get(i), isLastFrame);
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

  protected Object group;
  
  protected void addMenuItems(String parentId, String key, Object menu,
                            PopupResource popupResourceBundle) {
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
      item = st.nextToken();
      String label = popupResourceBundle.getWord(item);
      Object newMenu = null;
      String script = "";
      boolean isCB = false;
      if (label.equals("null")) {
        // user has taken this menu item out
        continue;
      } else if (item.indexOf("Menu") >= 0) {
        if (item.indexOf("more") < 0)
          group = null;
        Object subMenu = newMenu(label, id + "." + item);        
        addMenuSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle);
        checkSpecialMenu(item, subMenu, label);
        newMenu = subMenu;
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
        continue;
      } else if (item.endsWith("Checkbox") || (isCB = (item.endsWith("CB") || item.endsWith("RD")))) {
        // could be "PRD" -- set picking checkbox
        script = popupResourceBundle.getStructure(item);
        String basename = item.substring(0, item.length() - (!isCB ? 8 : 2));
        boolean isRadio = (isCB && item.endsWith("RD"));
        if (script == null || script.length() == 0 && !isRadio)
          script = "set " + basename + " T/F";
        newMenu = addCheckboxMenuItem(menu, label, basename 
            + ":" + script, id + "." + item, false, isRadio);
        rememberCheckbox(basename, newMenu);
        if (isRadio) 
          addButtonGroupItem(newMenu);
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = addMenuItem(menu, label, script, id + "." + item);
      }

      if (!allowSignedFeatures && item.startsWith("SIGNED"))
        enableMenu(newMenu, false);
      if (item.indexOf("VARIABLE") >= 0)
        htMenus.put(item, newMenu);
      // menus or menu items:
      if (item.indexOf("!PDB") >= 0) {
        NotPDB.add(newMenu);
      } else if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } 
      if (item.indexOf("URL") >= 0) {
        AppletOnly.add(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.add(newMenu);
      } else if (item.indexOf("BFACTORS") >= 0) {
        TemperatureOnly.add(newMenu);
      } else if (item.indexOf("UNITCELL") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FILEUNIT") >= 0) {
        FileUnitOnly.add(newMenu);
      } else if (item.indexOf("FILEMOL") >= 0) {
        FileMolOnly.add(newMenu);
      } 
      
      if (item.indexOf("!FRAMES") >= 0) {
        SingleModelOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } 
      
      if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.add(newMenu);
      }
      if (item.startsWith("SIGNED"))
        SignedOnly.add(newMenu);

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
            + label + "\t" + fixScript(id + "." + item, script);
        str = "addMenuItem('\t" + str + "\t')";
        Logger.info(str);
      }
    }
  }

  /**
   * @param item  
   * @param subMenu 
   * @param word 
   */
  protected void checkSpecialMenu(String item, Object subMenu, String word) {
    // special considerations here
  }

  protected Map<String, Object> htCheckbox = new Hashtable<String, Object>();

  private void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key + "::" + htCheckbox.size(), checkboxMenuItem);
  }

  /**
   * (1) setOption --> set setOption true or set setOption false
   * 
   * @param item
   * 
   * @param what
   *          option to set
   * @param TF
   *          true or false
   */
  protected void setCheckBoxValue(Object item, String what, boolean TF) {
    if (what.indexOf("##") < 0) {
      int pt = what.indexOf(":");
      if (pt < 0) {
        Logger.error("check box " + item + " IS " + what);
        return;
      }
      // name:trueAction|falseAction
      String basename = what.substring(0, pt);
      if (viewer.getBooleanProperty(basename) == TF)
        return;
      if (basename.endsWith("P!")) {
        if (basename.indexOf("??") >= 0) {
          what = setCheckBoxOption(item, basename, what);
        } else {
          if (what == null)
            return;
          if (!TF)
            return;
          what = "set picking " + basename.substring(0, basename.length() - 2);
        }
      } else {
        what = what.substring(pt + 1);
        if ((pt = what.indexOf("|")) >= 0)
          what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
        what = TextFormat.simpleReplace(what, "T/F", (TF ? " TRUE" : " FALSE"));
      }
    }
    viewer.evalStringQuiet(what);
  }

  /**
   * never implemented? For Modelkit
   * @param item  
   * @param name 
   * @param what 
   * @return   option
   */
  protected String setCheckBoxOption(Object item, String name, String what) {
    return null;
  }

  public String currentMenuItemId = null;

  protected String fixScript(String id, String script) {
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

  public void checkMenuClick(Object source, String script) {
    restorePopupMenu();
    if (script == null || script.length() == 0)
      return;
    if (script.equals("MAIN")) {
      show(thisx, thisy, true);
      return;
    }
    String id = getId(source);
    if (id != null) {
      script = fixScript(id, script);
      currentMenuItemId = id;
    }
    viewer.evalStringQuiet(script);
  }

  protected void restorePopupMenu() {
    // main menu only 
  }

  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItem(menuItem, entry, "", null);
  }

  ////////////////////////////////////////////////////////////////

  abstract protected void addButtonGroupItem(Object newMenu);
  abstract protected Object addCheckboxMenuItem(Object menu, String entry, String basename,
                                                String id, boolean state, boolean isRadio);
  abstract protected Object addMenuItem(Object menu, String entry, String script, String id);
  abstract protected void addMenuSeparator(Object menu);
  abstract protected void addMenuSubMenu(Object menu, Object subMenu);
  abstract protected void addToMenu(Object menu, Object item);
  abstract protected void enableMenu(Object menu, boolean enable);
  abstract protected void enableMenuItem(Object item, boolean enable);
  abstract protected String getId(Object menu);
  abstract protected int getMenuItemCount(Object menu);
  abstract protected Object newMenu(String entry, String id);
  abstract protected void removeAll(Object menu);
  abstract protected void removeMenuItem(Object menu, int index);
  abstract protected void renameMenu(Object menu, String entry);
  abstract protected void setAutoscrolls(Object menu);
  abstract protected void setCheckBoxState(Object item, boolean state);
  abstract protected void setCheckBoxValue(Object source);   
  abstract protected void setLabel(Object menu, String entry);
  abstract protected void setMenuListeners();
  abstract protected void showPopupMenu(int x, int y);

}
