package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class DragBinding extends JmolBinding {

  public DragBinding() {
    super("Drag");
    setSelectBindings();
  }
    
  private void setSelectBindings() {
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_selectToggle);
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_selectOr);
    bind(getMouseAction(SINGLE_CLICK,ALT_SHIFT_LEFT), ActionManager.ACTION_selectAndNot);
  }


}

