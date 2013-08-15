package org.jmol.viewer.binding;

import org.jmol.util.JmolList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;

import org.jmol.api.Event;
import org.jmol.api.Interface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.SB;


abstract public class Binding {

  public final static int WHEEL = Event.MOUSE_WHEEL; 
  public final static int LEFT = Event.MOUSE_LEFT;
  public final static int MIDDLE = Event.MOUSE_MIDDLE;
  public final static int RIGHT = Event.MOUSE_RIGHT; // 4
  public final static int ALT = Event.ALT_MASK; // MIDDLE and ALT are the same
  public final static int CTRL = Event.CTRL_MASK;  // 2
  public final static int SHIFT = Event.SHIFT_MASK; // 1
  public final static int CTRL_ALT = CTRL | ALT;
  public final static int CTRL_SHIFT = CTRL | SHIFT;
  public final static int LEFT_MIDDLE_RIGHT = LEFT | MIDDLE | RIGHT;
  public final static int MAC_COMMAND = LEFT | RIGHT;

  public final static int DOUBLE_CLICK = 2 << 8;
  public final static int SINGLE_CLICK = 1 << 8;
  public final static int DOWN = 4 << 8;
  public final static int CLICK_MASK = 7 << 8; 
  // for status messages:
  public final static int MOVED = 0;
  public final static int DRAGGED = 1;
  public final static int CLICKED = 2;
  public final static int WHEELED = 3;
  public final static int PRESSED = 4;
  public final static int RELEASED = 5;
  public final static int DRAGGED2 = 6;

  private final static int BUTTON_MODIFIER_MASK = 
    CTRL_ALT | SHIFT | LEFT | MIDDLE | RIGHT | WHEEL;
  
  private String name;
  private Map<String, Object> bindings = new Hashtable<String, Object>();
  public Map<String, Object> getBindings() {
    return bindings;
  }
    
  public Binding(String name) {
    this.name = name;  
  }
  
  public final String getName() {
    return name;
  }
  
  public final void bindAction(int mouseAction, int jmolAction) {
    //System.out.println("binding " + mouseAction + "\t" + jmolAction);
    addBinding(mouseAction + "\t" + jmolAction, new int[] {mouseAction, jmolAction});
  }
  
  public void bindName(int mouseAction, String name) {
    addBinding(mouseAction + "\t", Boolean.TRUE);
    addBinding(mouseAction + "\t" + name, new String[] { getMouseActionName(mouseAction, false), name });
  }


  public final void unbindAction(int mouseAction, int jmolAction) {
    if (mouseAction == 0)
      unbindJmolAction(jmolAction);
    else
      removeBinding(null, mouseAction + "\t" + jmolAction);
  }
  
  public final void unbindName(int mouseAction, String name) {
    if (name == null)
      unbindMouseAction(mouseAction);
    else
      removeBinding(null, mouseAction + "\t" + name);
  }
  
  public final void unbindJmolAction(int jmolAction) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = "\t" + jmolAction;
    while (e.hasNext()) {
      String key = e.next();
      if (key.endsWith(skey))
        removeBinding(e, key);
    }
  }
  
  private void addBinding(String key, Object value) {
    if (Logger.debugging)
      Logger.debug("adding binding " + key + "\t==\t" + Escape.e(value));
    bindings.put(key, value);
  }
  private void removeBinding(Iterator<String> e, String key) {
    if (Logger.debugging)
      Logger.debug("removing binding " + key);
    if (e == null)
      bindings.remove(key); 
    else
      e.remove();
  }
  
  public final void unbindUserAction(String script) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = "\t" + script;
    while (e.hasNext()) {
      String key = e.next();
      if (key.endsWith(skey))
        removeBinding(e, key);
    }
  }
  
  public final void unbindMouseAction(int mouseAction) {
    Iterator<String> e = bindings.keySet().iterator();
    String skey = mouseAction + "\t";
    while (e.hasNext()) {
      String key = e.next();
      if (key.startsWith(skey))
        removeBinding(e, key);
    }
  }
  
  public final boolean isBound(int mouseAction, int action) {
    if (mouseAction == 1040 && action == 2)
      System.out.println("left-down-2" + bindings.containsKey(mouseAction + "\t" + action));
    return bindings.containsKey(mouseAction + "\t" + action);
  }
  
  public final boolean isUserAction(int mouseAction) {
    return bindings.containsKey(mouseAction + "\t");
  }

  public static int getMouseAction(int clickCount, int modifiers) {
    if (clickCount > 2)
      clickCount = 2;
    return (modifiers & BUTTON_MODIFIER_MASK) | (clickCount == Integer.MIN_VALUE ? DOWN : (clickCount << 8));   
  }

  /**
   * create an action code from a string such as "CTRL-LEFT-double click"
   * @param desc
   * @return      action code
   */
  public static int getMouseActionStr(String desc) {
    if (desc == null)
      return 0;
    int action = 0;
    desc = desc.toUpperCase();

    if (desc.indexOf("MIDDLE") >= 0)
      action |= MIDDLE;
    else if (desc.indexOf("RIGHT") >= 0)
      action |= RIGHT;
    else if (desc.indexOf("WHEEL") >= 0)
      action |= WHEEL;
    else if (desc.indexOf("LEFT") >= 0)
      action |= LEFT;

    boolean isDefaultButton = (action == 0);
    
    boolean isDown = (desc.indexOf("DOWN") >= 0);
    if (desc.indexOf("DOUBLE") >= 0)
      action |= DOUBLE_CLICK;
    else if (action > 0 && (action & WHEEL) == 0 && !isDown || desc.indexOf("SINGLE") >= 0)
      action |= SINGLE_CLICK;
    else if (isDown)
      action |= DOWN;
    
    if (desc.indexOf("CTRL") >= 0)
      action |= CTRL;
    if (desc.indexOf("ALT") >= 0)
      action |= ALT;
    if (desc.indexOf("SHIFT") >= 0)
      action |= SHIFT;          

    if (isDefaultButton && action != 0)
      action |= LEFT;
    
    return action;
  }

  public static int getModifiers(int mouseAction) {
    return mouseAction & BUTTON_MODIFIER_MASK;
  }
  
  public static int getClickCount(int mouseAction) {
    return mouseAction >> 8;
  }

  @SuppressWarnings("unchecked")
  public String getBindingInfo(String[] actionInfo, String[] actionNames, String qualifiers) {
    SB sb = new SB();
    String qlow = (qualifiers == null || qualifiers.equalsIgnoreCase("all") ? null
        : qualifiers.toLowerCase());
    JmolList<String>[] names = new JmolList[actionInfo.length];
    for (int i = 0; i < actionInfo.length; i++)
      names[i] = (qlow == null
          || actionInfo[i].toLowerCase().indexOf(qlow) >= 0 ? new  JmolList<String>()
          : null);
    for (Object obj: bindings.values()) {
      if (!Escape.isAI(obj))
        continue;
      int[] info = (int[]) obj;
      int i = info[1];
      if (names[i] == null)
        continue;
      names[i].addLast(getMouseActionName(info[0], true));
    }
    for (int i = 0; i < actionInfo.length; i++) {
      int n;
      if (names[i] == null || (n = names[i].size()) == 0)
        continue;
      String[] list = names[i].toArray(new String[n]);
      Arrays.sort(list);
      sb.append((actionNames[i] + "                  ").substring(0, 22)).append("\t");
      String sep = "";
      int len = sb.length();
      for (int j = 0; j < n; j++) {
        sb.append(sep).append(list[j].substring(7));
        sep = ", ";
      }
      len = sb.length() - len;
      if (len < 20)
        sb.append("                 ".substring(0, 20 - len));
      sb.append("\t").append(actionInfo[i]).appendC('\n');
    }
    return sb.toString();
  }

  private static boolean includes(int mouseAction, int mod) {
    return ((mouseAction & mod) == mod);
  }
  public static String getMouseActionName(int mouseAction, boolean addSortCode) {
    SB sb = new SB();
    if (mouseAction == 0)
      return "";
    boolean isMiddle = (includes(mouseAction, MIDDLE)
        && !includes(mouseAction, LEFT) && !includes(mouseAction, RIGHT));
    char[] code = "      ".toCharArray();
    if (includes(mouseAction, CTRL)) {
      sb.append("CTRL+");
      code[4] = 'C';
    }
    if (!isMiddle && includes(mouseAction, ALT)) {
      sb.append("ALT+");
      code[3] = 'A';
    }
    if (includes(mouseAction, SHIFT)) {
      sb.append("SHIFT+");
      code[2] = 'S';
    }

    if (includes(mouseAction, LEFT)) {
      code[1] = 'L';
      sb.append("LEFT");
    } else if (includes(mouseAction, RIGHT)) {
      code[1] = 'R';
      sb.append("RIGHT");
    } else if (isMiddle) {
      code[1] = 'W';
      sb.append("MIDDLE");
    } else if (includes(mouseAction, WHEEL)) {
      code[1] = 'W';
      sb.append("WHEEL");
    }
    if (includes(mouseAction, DOUBLE_CLICK)) {
      sb.append("+double-click");
      code[0] = '2';
    } else if (includes(mouseAction, DOWN)) {
      sb.append("+down");
      code[0] = '4';
    }
    return (addSortCode ? new String(code) + ":" + sb.toString() : sb
        .toString());
  }

  public static Binding newBinding(String name) {
    return (Binding) Interface.getOptionInterface("viewer.binding." + name + "Binding");
  }
}
