package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class PfaatBinding extends JmolBinding {

  public PfaatBinding() {
    super("Pfaat");
    setSelectBindings();
  }

  private void setSelectBindings() {
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_selectNone);    
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_selectToggle);
    bind(getMouseAction(SINGLE_CLICK,ALT_SHIFT_LEFT), ActionManager.ACTION_selectAndNot);
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_selectOr);
  }


}

