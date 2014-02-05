package javajs.swing;

import javajs.api.SC;
import javajs.awt.Component;
import javajs.util.List;

public class ButtonGroup {
  
  List<SC> list = new List<SC>();
  Component popupMenu;
  private String id;
  
  public ButtonGroup(SC thisPopup) {
    popupMenu = (Component) thisPopup;
    /**
     * @j2sNative
     * 
     *  this.id = SwingController.getMenuID(this, "bg");
     * 
     */
    {}

  }
  
  public void add(SC item) {
    list.addLast(item);
    ((AbstractButton) item).htmlName = this.id;
  }

}
