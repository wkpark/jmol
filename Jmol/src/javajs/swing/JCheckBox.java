package javajs.swing;

public class JCheckBox extends AbstractButton {

	public JCheckBox() {
		super("chkJCB");
	}

	@Override
	public String toHTML() {
		String s = "<input type=checkbox id='" + id + "' class='JCheckBox' style='" + getCSSstyle(0, 0) 
    + "' " + (selected ? "checked='checked' " : "") + "onclick='SwingController.click(this)'>"
    + "<label for='" + id + "'>" + text + "</label>";
		return s;
	}


}
