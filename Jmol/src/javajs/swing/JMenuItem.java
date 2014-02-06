package javajs.swing;

import javajs.util.PT;

public class JMenuItem extends AbstractButton {

  int btnType;

  public JMenuItem(String text) {
    super("btn");
    setText(text);
  }

  public JMenuItem(String type, int i) {
    super(type);
    btnType = i;
  }

  @Override
  public String toHTML() {
      String s = "<li id=\"~ID~\" class=\"" + (this.enabled ? "" : "ui-state-disabled") + "\">";
      if (this.text != null)
        s += "<a>";
      s += htmlLabel();
      s = PT.rep(s, "~ID~", this.id);    
      if (this.text != null) 
        s = PT.rep(s,"TeXt", this.text) + "</a>";
      s += "</li>";
      return s;
  }

  protected String htmlLabel() {
    return (this.text != null ? "TeXt" : "");
  }

}
