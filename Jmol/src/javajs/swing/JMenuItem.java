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
    String s = htmlMenuOpener("li");
    if (this.text != null)
      s += "<a>";
    s += htmlLabel();
    if (this.text != null) 
      s = PT.rep(s,"TeXt", this.text) + "</a>";
    return s + "</li>";
  }

  @Override
  protected String getHtmlDisabled() {
    return " class=\"ui-state-disabled\"";  
  }

  protected String htmlLabel() {
    return (this.text != null ? "TeXt" : "");
  }

}
