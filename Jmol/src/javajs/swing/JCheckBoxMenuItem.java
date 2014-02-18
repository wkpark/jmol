package javajs.swing;

public class JCheckBoxMenuItem extends JMenuItem {

  public JCheckBoxMenuItem() {
    super("chk", 1);
  }

  
  @Override
  protected String htmlLabel() {
      return "<input id=\"" + this.id + "-cb\" type=\"checkbox\" " + (this.selected ? "checked" : "") + " /><label for=\"" + this.id + "-cb\">TeXt</label>";
  }

}
