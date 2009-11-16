package org.jmol.viewer.binding;

import java.awt.Event;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

abstract public class Binding {

  public final static int WHEEL = 32; 
  public final static int LEFT = 16;
  public final static int MIDDLE = Event.ALT_MASK; // 8 note that MIDDLE
  public final static int ALT = Event.ALT_MASK; // 8 and ALT are the same
  public final static int RIGHT = Event.META_MASK; // 4
  public final static int CTRL = Event.CTRL_MASK; // 2
  public final static int SHIFT = Event.SHIFT_MASK; // 1
  public final static int CTRL_ALT = CTRL | ALT;
  public final static int LEFT_MIDDLE_RIGHT = LEFT | MIDDLE | RIGHT;

  public final static int DOUBLE_CLICK = 2 << 8;
  public final static int SINGLE_CLICK = 1 << 8;

  private final static int BUTTON_MODIFIER_MASK = 
    CTRL_ALT | SHIFT | LEFT | MIDDLE | RIGHT | WHEEL;

  private String name;
  private Hashtable bindings = new Hashtable();
    
  public Binding(String name) {
    this.name = name;  
  }
  
  public final String getName() {
    return name;
  }
  
  public final void bind(int gesture, int action) {
    //System.out.println("binding " + gesture + "_" + action);
    bindings.put(gesture + "_" + action, new int[] {gesture, action});
  }
  
  public final void unbind(int gesture, int action) {
    bindings.remove(gesture + "_" + action);
  }
  
  public final void unbindAction(int action) {
    Enumeration e = bindings.keys();
    String skey = "_" + action;
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.endsWith(skey))
        bindings.remove(key);
    }
  }
  
  public final void unbindGesture(int gesture) {
    Enumeration e = bindings.keys();
    String skey = gesture + "_";
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.startsWith(skey))
        bindings.remove(key);
    }
  }
  
  public final boolean isBound(int gesture, int action) {
    return bindings.containsKey(gesture + "_" + action);
  }
  
  public static int getMouseAction(int clickCount, int modifiers) {
    if (clickCount > 2)
      clickCount = 2;
    return (modifiers & BUTTON_MODIFIER_MASK) | (clickCount << 8);   
  }

  public static int getModifiers(int gesture) {
    return gesture & BUTTON_MODIFIER_MASK;
  }
  
  public static int getClickCount(int gesture) {
    return gesture >> 8;
  }

  public String getBindingInfo(String[] actionNames, String qualifiers) {
    StringBuffer sb = new StringBuffer();
    String qlow = (qualifiers == null || qualifiers.equalsIgnoreCase("all") ? null
        : qualifiers.toLowerCase());
    Vector[] names = new Vector[actionNames.length];
    for (int i = 0; i < actionNames.length; i++)
      names[i] = (qlow == null
          || actionNames[i].toLowerCase().indexOf(qlow) >= 0 ? new Vector()
          : null);
    Enumeration e = bindings.keys();
    while (e.hasMoreElements()) {
      int[] info = (int[]) (bindings.get((String) e.nextElement()));
      int i = info[1];
      if (names[i] == null)
        continue;
      names[i].add(getGestureName(info[0]));
    }
    for (int i = 0; i < actionNames.length; i++) {
      int n;
      if (names[i] == null || (n = names[i].size()) == 0)
        continue;
      Object[] list = names[i].toArray();
      Arrays.sort(list);
      sb.append(actionNames[i]).append('\t');
      String sep = "";
      for (int j = 0; j < n; j++) {
        sb.append(sep);
        sb.append(((String) list[j]).substring(7));
        sep = ", ";
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  private static boolean includes(int gesture, int mod) {
    return ((gesture & mod) == mod);
  }
  private static String getGestureName(int gesture) {
    StringBuffer sb = new StringBuffer();
    if (gesture == 0)
      return "";
    boolean isMiddle = (includes(gesture, MIDDLE) 
        && !includes(gesture, LEFT) 
        && !includes(gesture, RIGHT));
    char[] code = "      ".toCharArray();
    if (includes(gesture, CTRL)) {
      sb.append("CTRL+");
      code[4] = 'C';
    }
    if (!isMiddle && includes(gesture, ALT)) {
      sb.append("ALT+");
      code[3] = 'A';
    }
    if (includes(gesture, SHIFT)) {
      sb.append("SHIFT+");
      code[2] = 'S';
    }
    
    if (includes(gesture, LEFT)) {
      code[1] = 'L';
      sb.append("LEFT");
    } else if (includes(gesture, RIGHT)) {
      code[1] = 'R';
      sb.append("RIGHT");
    } else if (isMiddle) {
      code[1] = 'W';
      sb.append("MIDDLE");
    } else if (includes(gesture, WHEEL)) {
      code[1] = 'W';
      sb.append("WHEEL");
    } 
    if (includes(gesture, DOUBLE_CLICK)) {
      sb.append("+double-click");
      code[0] = '2';
    }
    return new String(code) + ":" + sb.toString();
  }

}
