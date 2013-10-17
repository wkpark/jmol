package javajs.swing;

import javajs.util.SB;

public class JCheckBox extends JComponent {

	public JCheckBox() {
		super("chkJCB");
	}

	private boolean selected;

	public void setSelected(boolean selected) {
		this.selected = selected;
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setSelected(this);
		 * 
		 */
		{
			System.out.println(id + "  " + selected);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	@Override
	public String toHTML() {
		SB sb = new SB();
		sb.append("<input type=checkbox id='" + id + "' class='JCheckBox' style='" + getCSSstyle(0) + "' onclick='Jmol.Dialog.click(this)'>");
		return sb.toString();
	}


}
