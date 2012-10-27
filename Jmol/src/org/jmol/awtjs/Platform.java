package org.jmol.awtjs;

import java.net.URL;


import org.jmol.api.ApiPlatform;
import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.api.Interface;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolMouseInterface;
import org.jmol.api.JmolPopupInterface;
import org.jmol.api.JmolViewer;
import org.jmol.util.JmolFont;
import org.jmol.util.Point3f;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;

/**
 * JavaScript version requires Ajax-based URL stream processing.
 * (Not fully implemented) 
 * 
 * @author Bob Hanson
 *
 */
public class Platform implements ApiPlatform {
	public void setViewer(JmolViewer viewer, Object display) {
		//
		try {
		  URL.setURLStreamHandlerFactory(new AjaxURLStreamHandlerFactory());
		} catch (Throwable e) {
		  // that's fine -- already created	
		}
	}

  public boolean isSingleThreaded() {
    return true;
  }
	// /// Display

	public void convertPointFromScreen(Object display, Point3f ptTemp) {
		Display.convertPointFromScreen(display, ptTemp);
	}

	public void getFullScreenDimensions(Object display, int[] widthHeight) {
		Display.getFullScreenDimensions(display, widthHeight);
	}

	public JmolPopupInterface getMenuPopup(Viewer viewer, String menuStructure,
			char type) {
		JmolPopupInterface jmolpopup = (JmolPopupInterface) Interface
				.getOptionInterface(type == 'j' ? "popup.JmolPopup"
						: "modelkit.ModelKitPopup");
		if (jmolpopup != null)
			jmolpopup.initialize(viewer, menuStructure);
		return jmolpopup;
	}

	public boolean hasFocus(Object display) {
		return Display.hasFocus(display);
	}

	public String prompt(String label, String data, String[] list,
			boolean asButtons) {
		return Display.prompt(label, data, list, asButtons);
	}

	/**
	 * legacy apps will use this
	 * 
	 * @param viewer
	 * @param g
	 * @param size
	 */
	public void renderScreenImage(JmolViewer viewer, Object g, Object size) {
		Display.renderScreenImage(viewer, g, size);
	}

	public void requestFocusInWindow(Object display) {
		Display.requestFocusInWindow(display);
	}

	public void repaint(Object display) {
		Display.repaint(display);
	}

	public void setTransparentCursor(Object display) {
		Display.setTransparentCursor(display);
	}

	public void setCursor(int c, Object display) {
		Display.setCursor(c, display);
	}

	// //// Mouse

	public JmolMouseInterface getMouseManager(Viewer viewer, ActionManager actionManager) {
		return new Mouse(viewer, actionManager);
	}

	// //// Image

	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent) {
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent);
	}

	public Object createImage(Object data) {
		return Image.createImage(data);
	}

	public void disposeGraphics(Object gOffscreen) {
		Image.disposeGraphics(gOffscreen);
	}

	public void drawImage(Object g, Object img, int x, int y, int width,
			int height) {
		Image.drawImage(g, img, x, y, width, height);
	}

	public int[] grabPixels(Object imageobj, int width, int height) {
		return Image.grabPixels(imageobj, width, height);
	}

	public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object imageobj, int width, int height, int bgcolor) {
		return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width,
				height, bgcolor);
	}

	public int[] getTextPixels(String text, JmolFont font3d, Object gObj,
			Object image, int width, int height, int ascent) {
		return Image
				.getTextPixels(text, font3d, gObj, image, width, height, ascent);
	}

	public void flushImage(Object imagePixelBuffer) {
		Image.flush(imagePixelBuffer);
	}

	public Object getGraphics(Object image) {
		return Image.getGraphics(image);
	}

	public int getImageHeight(Object image) {
		return Image.getHeight(image);
	}

	public int getImageWidth(Object image) {
		return Image.getWidth(image);
	}

	public Object getJpgImage(Viewer viewer, int quality, String comment) {
		return Image.getJpgImage(this, viewer, quality, comment);
	}

	public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
		return Image.getStaticGraphics(image, backgroundTransparent);
	}

	public Object newBufferedImage(Object image, int w, int h) {
		return Image.newBufferedImage(image, w, h);
	}

	public Object newOffScreenImage(int w, int h) {
		return Image.newBufferedImage(w, h);
	}

	public boolean waitForDisplay(Object display, Object image)
			throws InterruptedException {
		Image.waitForDisplay(display, image);
		return true;
	}

	// /// FONT

	public int fontStringWidth(JmolFont font, Object fontMetrics, String text) {
		return Font.stringWidth(font, fontMetrics, text);
	}

	public int getFontAscent(Object fontMetrics) {
		return Font.getAscent(fontMetrics);
	}

	public int getFontDescent(Object fontMetrics) {
		return Font.getDescent(fontMetrics);
	}

	public Object getFontMetrics(JmolFont font, Object graphics) {
		return Font.getFontMetrics(graphics, font);
	}

	public Object newFont(String fontFace, boolean isBold, boolean isItalic,
			float fontSize) {
		return Font.newFont(fontFace, isBold, isItalic, fontSize);
	}

	// / misc

	public Object getJsObjectInfo(Object jsObject, String method, Object[] args) {
		return null; // DOM XML reader only -- not implemented in JavaScript
	}

	public boolean isHeadless() {
		return false;
	}

  private JmolFileAdapter fileAdapter;

  public JmolFileAdapterInterface getFileAdapter() {
    return (fileAdapter == null  ? fileAdapter = new JmolFileAdapter() : fileAdapter);
  }

	public JmolFileInterface newFile(String name) {
		return new JmolFile(name);
	}

  public void notifyEndOfRendering() {
    // N/A
  }
}
