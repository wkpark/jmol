package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class RasmolBinding extends JmolBinding {

  public RasmolBinding() {
    super("Rasmol");
    setSelectBindings();
  }
    
  private void setSelectBindings() {
    bind(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_selectToggle);
  }

}

