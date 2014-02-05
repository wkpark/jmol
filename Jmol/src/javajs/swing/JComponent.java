package javajs.swing;

import javajs.awt.Container;

public abstract class JComponent extends Container {

  protected boolean autoScrolls;

  protected JComponent(String type) {
    super(type);
  }
  
  public void setAutoscrolls(boolean b) {
    autoScrolls = b;
  }
  
}
