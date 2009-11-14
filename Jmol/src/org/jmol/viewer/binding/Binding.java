package org.jmol.viewer.binding;

import java.awt.Event;
import java.util.Enumeration;
import java.util.Hashtable;

abstract public class Binding {

  public final static int LEFT = 16;
  public final static int MIDDLE = Event.ALT_MASK; // 8 note that MIDDLE
  public final static int ALT = Event.ALT_MASK; // 8 and ALT are the same
  public final static int RIGHT = Event.META_MASK; // 4
  public final static int CTRL = Event.CTRL_MASK; // 2
  public final static int SHIFT = Event.SHIFT_MASK; // 1
  public final static int MIDDLE_RIGHT = MIDDLE | RIGHT;
  public final static int LEFT_MIDDLE_RIGHT = LEFT | MIDDLE | RIGHT;
  public final static int CTRL_SHIFT = CTRL | SHIFT;
  public final static int CTRL_ALT = CTRL | ALT;
  public final static int CTRL_LEFT = CTRL | LEFT;
  public final static int CTRL_RIGHT = CTRL | RIGHT;
  public final static int CTRL_MIDDLE = CTRL | MIDDLE;
  public final static int CTRL_ALT_LEFT = CTRL_ALT | LEFT;
  public final static int CTRL_ALT_RIGHT = CTRL_ALT | RIGHT;
  public final static int ALT_LEFT = ALT | LEFT;
  public final static int ALT_SHIFT_LEFT = ALT | SHIFT | LEFT;
  public final static int SHIFT_LEFT = SHIFT | LEFT;
  public final static int CTRL_SHIFT_LEFT = CTRL_SHIFT | LEFT;
  public final static int CTRL_ALT_SHIFT_LEFT = CTRL_ALT | SHIFT | LEFT;
  public final static int SHIFT_MIDDLE = SHIFT | MIDDLE;
  public final static int CTRL_SHIFT_MIDDLE = CTRL_SHIFT | MIDDLE;
  public final static int SHIFT_RIGHT = SHIFT | RIGHT;
  public final static int CTRL_SHIFT_RIGHT = CTRL_SHIFT | RIGHT;
  public final static int CTRL_ALT_SHIFT_RIGHT = CTRL_ALT | SHIFT | RIGHT;
  public final static int DOUBLE_CLICK = 2;
  public final static int SINGLE_CLICK = 1;

  private final static int BUTTON_MODIFIER_MASK = 
    CTRL_ALT | SHIFT | LEFT | MIDDLE | RIGHT;

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
    bindings.put(gesture + "_" + action, Boolean.TRUE);
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

}
