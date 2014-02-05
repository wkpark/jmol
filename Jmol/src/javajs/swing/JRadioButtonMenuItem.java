package javajs.swing;

public class JRadioButtonMenuItem extends JMenuItem {

  public JRadioButtonMenuItem() {
    super("rad",2);
  }

  @Override
  public String htmlLabel() {
    return   "<input id=\"ID-rb\" type=\"radio\" name=\"" + this.htmlName + "\" " 
        + (this.selected ? "checked" : "") + " /><label for=\"ID-rb\">TeXt</label>";
  }

}
