package javajs.swing;

import javajs.awt.Component;

import javajs.api.SC;

public abstract class AbstractButton extends JComponent implements SC {

  boolean selected;
  String actionCommand;
  Object itemListener;
  SC popupMenu;
  Object applet;

  protected AbstractButton(String type) {
    super(type);
    enabled = true;
  }

  @Override
  public void setSelected(boolean selected) {
    this.selected = selected;
    /**
     * @j2sNative
     * 
     * SwingController.setSelected(this);
     * 
     */
    {
    }
  }

  @Override
  public boolean isSelected() {
    return selected;
  }
  
  @Override
  public String getActionCommand() {
    return actionCommand;
  }
  
  @Override
  public void setActionCommand(String actionCommand) {
    this.actionCommand = actionCommand;
  }

  @Override
  public void addItemListener(Object listener) {
    itemListener = listener;
  }
  
  protected String icon;
  public String htmlName;

  public void setIcon(Object icon) {
    this.icon = (String) icon;
  }

  @Override
  public void init(String text, Object icon, String actionCommand, SC popupMenu) {
    this.text = text;
    this.icon = (String) icon;
    this.actionCommand = actionCommand;
    this.popupMenu = popupMenu;
    /**
     * @j2sNative
     * 
     *  SwingController.initMenuItem(this);
     *  
     */
    {
    }
  }
  
  @Override
  public void add(SC item) {
    addComponent((Component) item);
  }

  @Override
  public void insert(SC subMenu, int index) {
    // JMenu, JPopupMenu only, but implemented here as well
    // for simplicity
    insertComponent((Component) subMenu, index);
  }

  @Override
  public Object getPopupMenu() {
    // JMenu only
    return null;
  }

  protected String getMenuHTML() {
    String label = (this.icon != null ? this.icon
        : this.text != null ? this.text : null);
    String s = (label == null ? "" : "<li><a>" + label + "</a>" + "<ul id=\""
        + this.id + "\" class=\"" + (this.enabled ? "" : "ui-state-disabled")
        + "\">");
    int n = getComponentCount();
    if (n > 0)
      for (int i = 0; i < n; i++)
        s += getComponent(i).toHTML();
    if (label != null)
      s += "</ul></li>";
    return s;
  }

}
