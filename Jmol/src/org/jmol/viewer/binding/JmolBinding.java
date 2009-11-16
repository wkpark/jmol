package org.jmol.viewer.binding;

import org.jmol.viewer.ActionManager;

public class JmolBinding extends Binding {

  public JmolBinding() {
    this("Jmol");
    setSelectBindings();
  }
  
  public JmolBinding(String name) {
    super(name);
    setGeneralBindings();
    setPickBindings();
  }
    
  private void setSelectBindings() {
  }

  protected void setGeneralBindings() {
    
    bind(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_rotateZorZoom);
    bind(SINGLE_CLICK+MIDDLE, ActionManager.ACTION_rotateZorZoom);
    bind(SINGLE_CLICK+WHEEL, ActionManager.ACTION_zoom);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_slideZoom);

    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_rotateXY);

    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_dragSpin);

    bind(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_rotateZ);
    bind(SINGLE_CLICK+SHIFT+RIGHT, ActionManager.ACTION_rotateZ);

    bind(SINGLE_CLICK+CTRL+ALT+LEFT, ActionManager.ACTION_translateXY);
    bind(SINGLE_CLICK+CTRL+RIGHT, ActionManager.ACTION_translateXY);
    bind(DOUBLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_translateXY); 
    bind(DOUBLE_CLICK+MIDDLE, ActionManager.ACTION_translateXY);
    
    bind(SINGLE_CLICK+ALT+SHIFT+LEFT, ActionManager.ACTION_dragSelected);
    bind(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_rotateSelected);

    bind(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_dragLabel);
    
    bind(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_dragDrawPoint);
    bind(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_dragDrawObject);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_spinDrawObjectCCW);
    bind(SINGLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_spinDrawObjectCW);
    
    bind(SINGLE_CLICK+CTRL+LEFT, ActionManager.ACTION_popupMenu);
    bind(SINGLE_CLICK+RIGHT, ActionManager.ACTION_popupMenu);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_clickFrank);

    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_navTranslate);

    bind(SINGLE_CLICK+CTRL+SHIFT+LEFT, ActionManager.ACTION_slab);
    bind(DOUBLE_CLICK+CTRL+SHIFT+LEFT, ActionManager.ACTION_depth); 
    bind(SINGLE_CLICK+CTRL+ALT+SHIFT+LEFT, ActionManager.ACTION_slabDepth);
    
    bind(DOUBLE_CLICK+SHIFT+LEFT, ActionManager.ACTION_reset);
    bind(DOUBLE_CLICK+MIDDLE, ActionManager.ACTION_reset); 
  }
  
  protected void setPickBindings() {
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickAtom);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickPoint);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickLabel);
    bind(SINGLE_CLICK+LEFT, ActionManager.ACTION_pickMeasure);
    bind(DOUBLE_CLICK+LEFT, ActionManager.ACTION_setMeasure);
    bind(SINGLE_CLICK+ALT+LEFT, ActionManager.ACTION_pickIsosurface);      
  }
  
}

