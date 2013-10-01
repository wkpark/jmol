package jspecview.common;

import java.awt.Component;

import javax.swing.JDialog;

abstract public class AwtDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	abstract protected int[] getPosXY();

	protected void setPosition(Component panel, int[] posXY) {
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = panel.getLocationOnScreen().x;
				posXY[1] = panel.getLocationOnScreen().y + panel.getHeight() - 20;
			}
			setLocation(posXY[0], posXY[1]);
		}
	}

	


}
