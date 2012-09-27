package org.jmol.awtjs;

import javax.vecmath.Point3f;

import org.jmol.api.JmolViewer;

/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to org.jmol.awt
 * 
 */

class Display {

	/**
	 * @param display
	 * @param widthHeight
	 * 
	 */
	static void getFullScreenDimensions(Object display, int[] widthHeight) {
		/**
		 * @j2sNative
		 * widthHeight[0] = display.style.width;
		 * widthHeight[1] = display.style.height; 
		 */
		{}
	}

	static boolean hasFocus(Object display) {
		return true;
	}

	static void requestFocusInWindow(Object display) {
	}

	static void repaint(Object display) {
		System.out.println("repaint display");
	}

	static void renderScreenImage(JmolViewer viewer, Object g, Object size) {
	}

	static void setTransparentCursor(Object display) {
	}

	static void setCursor(int c, Object display) {
	}

	public static String prompt(String label, String data, String[] list,
			boolean asButtons) {
		//TODO
		return "null";
	}

	public static void convertPointFromScreen(Object display, Point3f ptTemp) {
	}

}
