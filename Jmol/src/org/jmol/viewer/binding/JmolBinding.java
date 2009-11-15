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
    bind(getMouseAction(DOUBLE_CLICK,LEFT), ActionManager.ACTION_selectToggle);
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_selectToggle);
  }

  protected void setGeneralBindings() {
    
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_zoom);
    bind(getMouseAction(SINGLE_CLICK,MIDDLE), ActionManager.ACTION_zoom);
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_slideZoom);

    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_rotateXY);

    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_dragSpin);

    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_rotateZ);
    bind(getMouseAction(SINGLE_CLICK,MIDDLE), ActionManager.ACTION_rotateZ);
    bind(getMouseAction(SINGLE_CLICK,SHIFT_RIGHT), ActionManager.ACTION_rotateZ);

    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_rotateMolecule);
    bind(getMouseAction(SINGLE_CLICK,CTRL_ALT_RIGHT), ActionManager.ACTION_rotateMolecule);

    bind(getMouseAction(SINGLE_CLICK,CTRL_ALT_LEFT), ActionManager.ACTION_translateXY);
    bind(getMouseAction(SINGLE_CLICK,CTRL_RIGHT), ActionManager.ACTION_translateXY);
    bind(getMouseAction(DOUBLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_translateXY); 
    bind(getMouseAction(DOUBLE_CLICK,MIDDLE), ActionManager.ACTION_translateXY);

    bind(getMouseAction(SINGLE_CLICK,CTRL_LEFT), ActionManager.ACTION_popupMenu);
    bind(getMouseAction(SINGLE_CLICK,RIGHT), ActionManager.ACTION_popupMenu);
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_clickFrank);

    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_navTranslate);

    bind(getMouseAction(DOUBLE_CLICK,LEFT), ActionManager.ACTION_setMeasure);

    bind(getMouseAction(SINGLE_CLICK,CTRL_SHIFT_LEFT), ActionManager.ACTION_slab);
    bind(getMouseAction(DOUBLE_CLICK,CTRL_SHIFT_LEFT), ActionManager.ACTION_depth); 
    bind(getMouseAction(SINGLE_CLICK,CTRL_ALT_SHIFT_LEFT), ActionManager.ACTION_slabDepth);

    bind(getMouseAction(DOUBLE_CLICK,MIDDLE), ActionManager.ACTION_reset);
    bind(getMouseAction(DOUBLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_reset);
    
  }
  
  protected void setPickBindings() {
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_pickAtom);
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_pickPoint);
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_pickLabel);
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_pickMeasure);
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_pickIsosurface);
  
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_dragSelected);
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_dragSelected);
    bind(getMouseAction(SINGLE_CLICK,ALT_SHIFT_LEFT), ActionManager.ACTION_dragSelected);
    
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_dragDrawObject);
    
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_dragDrawPoint);
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_dragLabel);
  
    bind(getMouseAction(SINGLE_CLICK,LEFT), ActionManager.ACTION_spinDrawObjectCCW);
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_spinDrawObjectCW);
    
    // these are the same as "drag"
    bind(getMouseAction(SINGLE_CLICK,SHIFT_LEFT), ActionManager.ACTION_rubberBandSelectToggle);
    bind(getMouseAction(SINGLE_CLICK,ALT_LEFT), ActionManager.ACTION_rubberBandSelectOr);
    bind(getMouseAction(SINGLE_CLICK,ALT_SHIFT_LEFT), ActionManager.ACTION_rubberBandSelectAndNot);
    
  }
  
}

