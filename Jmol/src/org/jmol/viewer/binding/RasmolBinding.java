package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class RasmolBinding extends JmolBinding {

  public RasmolBinding() {
    super("Rasmol");
    setSelectBindings();
  }
    
  private void setSelectBindings() {
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_selectToggle);
  }

}

