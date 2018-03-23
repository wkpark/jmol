package org.jmol.awtjs.swing;

import javajs.awt.Component;
import javajs.awt.SC;

public class ButtonGroup {
  
  private String id;
  
  public ButtonGroup() {
    id = Component.newID("bg");
  }
  
  public void add(SC item) {
    ((AbstractButton) item).htmlName = this.id;
  }

}
