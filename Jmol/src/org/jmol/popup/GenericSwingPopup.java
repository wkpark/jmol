package org.jmol.popup;

import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.jmol.util.Logger;

import javajs.api.GenericMenuInterface;
import javajs.api.SC;
import javajs.util.List;
import javajs.util.PT;
import javajs.util.SB;

public abstract class GenericSwingPopup implements GenericMenuInterface {

  abstract protected Object getImageIcon(String fileName);
  abstract protected void menuShowPopup(SC popup, int x, int y);
  abstract protected String menuSetCheckBoxOption(SC item, String name, String what);

  abstract protected void appCheckItems(String item, SC newMenu);
  abstract protected void appCheckSpecialMenu(String item, SC subMenu, String word);
  abstract protected String appFixLabel(String label);
  abstract protected String appFixScript(String name, String script);
  abstract protected boolean appGetBooleanProperty(String name);
  abstract protected String appGetMenuAsString(String title);
  abstract protected boolean appIsSpecialCheckBox(SC item, String basename, String what,
                                                  boolean TF);
  abstract protected void appRestorePopupMenu();           
  abstract protected void appRunScript(String script);
  abstract protected void appUpdateSpecialCheckBoxValue(SC source,
                                                 String actionCommand,
                                                 boolean selected);
  abstract protected void appUpdateForShow();
  
  protected PopupHelper helper;

  protected String strMenuStructure;

  protected boolean allowSignedFeatures;
  protected boolean isJS, isApplet, isSigned;
  protected int thisx, thisy;

  protected String menuName;
  protected SC popupMenu;
  protected SC thisPopup;
  protected Map<String, SC> htCheckbox = new Hashtable<String, SC>();
  protected Object buttonGroup;
  protected String currentMenuItemId;
  protected Map<String, SC> htMenus = new Hashtable<String, SC>();
  private List<SC> AppletOnly = new List<SC>();
  private List<SC> SignedOnly = new List<SC>();

  protected void initSwing(String title, PopupResource bundle, boolean isJS,
                           boolean isApplet, boolean isSigned) {
      this.isJS = isJS;
      this.isApplet = isApplet;
      this.isSigned = isSigned;
      this.allowSignedFeatures = (!isApplet || isSigned);
      menuName = title;
      popupMenu = helper.menuCreatePopup(title);
      thisPopup = popupMenu;
      htMenus.put(title, popupMenu);
      addMenuItems("", title, popupMenu, bundle);
      try {
        jpiUpdateComputedMenus();
      } catch (NullPointerException e) {
        // ignore -- the frame just wasn't ready yet;
        // updateComputedMenus() will be called again when the frame is ready; 
      }
    }


  protected void addMenuItems(String parentId, String key, SC menu,
                              PopupResource popupResourceBundle) {
      String id = parentId + "." + key;
      String value = popupResourceBundle.getStructure(key);
      if (Logger.debugging)
        Logger.debug(id + " --- " + value);
      if (value == null) {
        menuCreateItem(menu, "#" + key, "", "");
        return;
      }
      // process predefined @terms
      StringTokenizer st = new StringTokenizer(value);
      String item;
      while (value.indexOf("@") >= 0) {
        String s = "";
        while (st.hasMoreTokens())
          s += " "
              + ((item = st.nextToken()).startsWith("@") ? popupResourceBundle
                  .getStructure(item) : item);
        value = s.substring(1);
        st = new StringTokenizer(value);
      }
      while (st.hasMoreTokens()) {
        item = st.nextToken();
        if (!checkKey(item))
          continue;
        if ("-".equals(item)) {
          menuAddSeparator(menu);
          continue;
        }
        String label = popupResourceBundle.getWord(item);
        SC newItem = null;
        String script = "";
        boolean isCB = false;
        label = appFixLabel(label == null ? item : label);
        if (label.equals("null")) {
          // user has taken this menu item out
          continue;
        }
        if (item.indexOf("Menu") >= 0) {
          if (item.indexOf("more") < 0)
            helper.menuAddButtonGroup(null);
          SC subMenu = menuNewSubMenu(label, id + "." + item);
          menuAddSubMenu(menu, subMenu);
          if (item.indexOf("Computed") < 0)
            addMenuItems(id, item, subMenu, popupResourceBundle);
          appCheckSpecialMenu(item, subMenu, label);
          newItem = subMenu;
        } else if (item.endsWith("Checkbox")
            || (isCB = (item.endsWith("CB") || item.endsWith("RD")))) {
          // could be "PRD" -- set picking checkbox
          script = popupResourceBundle.getStructure(item);
          String basename = item.substring(0, item.length() - (!isCB ? 8 : 2));
          boolean isRadio = (isCB && item.endsWith("RD"));
          if (script == null || script.length() == 0 && !isRadio)
            script = "set " + basename + " T/F";
          newItem = menuCreateCheckboxItem(menu, label, basename + ":" + script,
              id + "." + item, false, isRadio);
          rememberCheckbox(basename, newItem);
          if (isRadio)
            helper.menuAddButtonGroup(newItem);
        } else {
          script = popupResourceBundle.getStructure(item);
          if (script == null)
            script = item;
          if (!isJS && item.startsWith("JS"))
            continue;
          newItem = menuCreateItem(menu, label, script, id + "." + item);
        }
        // menus or menu items:
        htMenus.put(item, newItem);
        if (item.indexOf("URL") >= 0)
          AppletOnly.addLast(newItem);
        if (!allowSignedFeatures && item.startsWith("SIGNED"))
          menuEnable(newItem, false);
        if (item.startsWith("SIGNED"))
          SignedOnly.addLast(newItem);
        appCheckItems(item, newItem);
      }
    }

  protected void updateSignedAppletItems() {
    for (int i = SignedOnly.size(); --i >= 0;)
      menuEnable(SignedOnly.get(i), isSigned || !isApplet);
    for (int i = AppletOnly.size(); --i >= 0;)
      menuEnable(AppletOnly.get(i), isApplet);
  }

  /**
   * @param key
   * @return true unless a JAVA-only key in JavaScript
   */
  private boolean checkKey(String key) {
    /**
     * @j2sNative
     * 
     *            return (key.indexOf("JAVA") < 0 && !(key.indexOf("NOGL") &&
     *            this.viewer.isWebGL));
     * 
     */
    {
      return true;
    }
  }

  private void rememberCheckbox(String key, SC checkboxMenuItem) {
    htCheckbox.put(key + "::" + htCheckbox.size(), checkboxMenuItem);
  }
  
  protected void updateButton(SC b, String entry, String script) {
    String[] ret = new String[] { entry };    
    Object icon = getEntryIcon(ret);
    entry = ret[0];
    b.init(entry, icon, script, thisPopup);
    helper.taint();
  }

  protected Object getEntryIcon(String[] ret) {
    String entry = ret[0];
    if (!entry.startsWith("<"))
      return null;
    int pt = entry.indexOf(">");
    ret[0] = entry.substring(pt + 1);
    String fileName = entry.substring(1, pt);
    return getImageIcon(fileName);
  }

  protected SC addMenuItem(SC menuItem, String entry) {
    return menuCreateItem(menuItem, entry, "", null);
  }

  protected void menuSetLabel(SC m, String entry) {
    m.setText(entry);
    helper.taint();
  }

  private void menuSetCheckBoxValue(SC source) {
    boolean isSelected = source.isSelected();
    String what = source.getActionCommand();
    checkForCheckBoxScript(source, what, isSelected);
    appUpdateSpecialCheckBoxValue(source, what, isSelected);
    helper.taint();
  }

  /////// run time event-driven methods
  
  
  @Override
  public void menuClickCallback(SC source, String script) {
    appRestorePopupMenu();
    if (script == null || script.length() == 0)
      return;
    if (script.equals("MAIN")) {
      show(thisx, thisy, true);
      return;
    }
    String id = menuGetId(source);
    if (id != null) {
      script = appFixScript(id, script);
      currentMenuItemId = id;
    }
    appRunScript(script);
  }

  @Override
  public void menuCheckBoxCallback(SC source) {
    appRestorePopupMenu();
    menuSetCheckBoxValue(source);
    String id = menuGetId(source);
    if (id != null) {
      currentMenuItemId = id;
    }
  }

  private void checkForCheckBoxScript(SC item, String what, boolean TF) {
    if (!item.isEnabled())
      return;
    if (what.indexOf("##") < 0) {
      int pt = what.indexOf(":");
      if (pt < 0) {
        Logger.error("check box " + item + " IS " + what);
        return;
      }
      // name:trueAction|falseAction
      String basename = what.substring(0, pt);
      if (appIsSpecialCheckBox(item, basename, what, TF))
        return;
      what = what.substring(pt + 1);
      if ((pt = what.indexOf("|")) >= 0)
        what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
      what = PT.rep(what, "T/F", (TF ? " TRUE" : " FALSE"));
    }
    appRunScript(what);
  }

  protected SC menuCreateItem(SC menu, String entry, String script,
                               String id) {
    SC item = helper.getMenuItem(entry);
    item.addActionListener(helper);
    return newMenuItem(item, menu, entry, script, id);
  }

  protected SC menuCreateCheckboxItem(SC menu, String entry,
                                       String basename, String id,
                                       boolean state, boolean isRadio) {
    SC jmi = (isRadio ? helper.getRadio(entry) : helper.getCheckBox(entry));
    jmi.setSelected(state);
    jmi.addItemListener(helper);
    return newMenuItem(jmi, menu, entry, basename, id);
  }

  protected void menuAddSeparator(SC menu) {
    menu.add(helper.getMenuItem(null));
    helper.taint();
  }

  protected SC menuNewSubMenu(String entry, String id) {
    SC jm = helper.getMenu(entry);
    updateButton(jm, entry, null);
    jm.setName(id);
    jm.setAutoscrolls(true);
    return jm;
  }

  protected void menuRemoveAll(SC menu, int indexFrom) {
    if (indexFrom <= 0)
      menu.removeAll();
    else
      for (int i = menu.getComponentCount(); --i >= indexFrom;)
        menu.remove(i);
    helper.taint();
  }

  private SC newMenuItem(SC item, SC menu,
                                     String text, String script, String id) {
    updateButton(item, text, script);
    if (id != null && id.startsWith("Focus")) {
      item.addMouseListener(helper);
      id = menu.getName() + "." + id;
    }
    item.setName(id == null ? menu.getName() + "." : id);
    menuAddItem(menu, item);
    return item;
  }

  private void menuAddItem(SC menu, SC item) {
    menu.add(item);
    helper.taint();
  }

  protected void menuAddSubMenu(SC menu, SC subMenu) {
    menuAddItem(menu, subMenu);
  }

  protected void menuEnable(SC component, boolean enable) {
    if (component == null)
      return;
    component.setEnabled(enable);
  }

  protected String menuGetId(SC menu) {
    return menu.getName();
  }

  protected void menuSetAutoscrolls(SC menu) {
    menu.setAutoscrolls(true);
    helper.taint();
  }

  protected int menuGetListPosition(SC item) {
    SC p = (SC) item.getParent();
    int i;
    for (i = p.getComponentCount(); --i >= 0;)
      if (helper.getSwingComponent(p.getComponent(i)) == item)
        break;
    return i;
  }

  protected void show(int x, int y, boolean doPopup) {
    thisx = x;
    thisy = y;
    appUpdateForShow();
    updateCheckBoxesForShow();
    if (doPopup)
      menuShowPopup(popupMenu, thisx, thisy);
  }

  private void updateCheckBoxesForShow() {
    for (Map.Entry<String, SC> entry : htCheckbox.entrySet()) {
      String key = entry.getKey();
      SC item = entry.getValue();
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = appGetBooleanProperty(basename);
      if (item.isSelected() != b) {
        item.setSelected(b);
        helper.taint();
      }
    }
  }
  
  @Override
  public String jpiGetMenuAsString(String title) {
    appUpdateForShow();
    int pt = title.indexOf("|");
    if (pt >= 0) {
      String type = title.substring(pt);
      title = title.substring(0, pt);
      if (type.indexOf("current") >= 0) {
        SB sb = new SB();
        SC menu = htMenus.get(menuName);
        menuGetAsText(sb, 0, menu, "PopupMenu");
        return sb.toString();
      }
    }
    return appGetMenuAsString(title);
  }
  
  private void menuGetAsText(SB sb, int level, SC menu, String menuName) {
    String name = menuName;
    Object[] subMenus = menu.getComponents();
    String flags = null;
    String script = null;
    String text = null;
    char key = 'S';
    for (int i = 0; i < subMenus.length; i++) {
      SC m = helper.getSwingComponent(subMenus[i]);
      int type = helper.getItemType(m);
      switch (type) {
      case 4:
        key = 'M';
        name = m.getName();
        flags = "enabled:" + m.isEnabled();
        text = m.getText();
        script = null;
        break;
      case 0:
        key = 'S';
        flags = script = text = null;
        break;
      default:
        key = 'I';
        flags = "enabled:" + m.isEnabled();
        if (type == 2 || type == 3)
          flags += ";checked:" + m.isSelected();
        script = appFixScript(m.getName(),
            m.getActionCommand());
        name = m.getName();
        text = m.getText();
        break;
      }
      addItemText(sb, key, level, name, text, script, flags);
      if (type == 2)
        menuGetAsText(sb, level + 1, helper.getSwingComponent(m.getPopupMenu()), name);
    }
  }

  private static void addItemText(SB sb, char type, int level, String name,
                                    String label, String script, String flags) {
    sb.appendC(type).appendI(level).appendC('\t').append(name);
    if (label == null) {
      sb.append(".\n");
      return;
    }
    sb.append("\t").append(label).append("\t").append(
        script == null || script.length() == 0 ? "-" : script).append("\t")
        .append(flags).append("\n");
  }

  static protected int convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512 * 1024)
      num += 512 * 1024;
    return (int) (num / (1024 * 1024));
  }
  
}
