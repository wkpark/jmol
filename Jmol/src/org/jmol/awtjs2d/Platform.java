package org.jmol.awtjs2d;

import java.net.URL;


import org.jmol.api.ApiPlatform;
import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolMouseInterface;
import org.jmol.api.JmolPopupInterface;
import org.jmol.api.JmolViewer;
import org.jmol.util.JmolFont;
import org.jmol.util.Point3f;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;

/**
 * JavaScript 2D canvas version requires Ajax-based URL stream processing.
 * 
 * Jmol "display" --> HTML5 "canvas"
 * Jmol "image" --> HTML5 "context"
 * Jmol "graphics" --> HTML5 "context" (one for display, one off-screen for fonts)
 * Jmol "font" --> JmolFont
 * Jmol "fontMetrics" --> HTML5 "context"
 * (Not fully implemented) 
 * 
 * @author Bob Hanson
 *
 */
public class Platform implements ApiPlatform {
  Object canvas;
  JmolViewer viewer;
  
	public void setViewer(JmolViewer viewer, Object canvas) {
	  this.viewer = viewer;
		this.canvas = canvas;
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

	public void convertPointFromScreen(Object canvas, Point3f ptTemp) {
		Display.convertPointFromScreen(canvas, ptTemp);
	}

	public void getFullScreenDimensions(Object canvas, int[] widthHeight) {
		Display.getFullScreenDimensions(canvas, widthHeight);
	}

	public JmolPopupInterface getMenuPopup(Viewer viewer, String menuStructure,
			char type) {
		return null;
	}

	public boolean hasFocus(Object canvas) {
		return Display.hasFocus(canvas);
	}

	public String prompt(String label, String data, String[] list,
			boolean asButtons) {
		return Display.prompt(label, data, list, asButtons);
	}

	/**
	 * legacy apps will use this
	 * 
	 * @param viewer
	 * @param context
	 * @param size
	 */
	public void renderScreenImage(JmolViewer viewer, Object context, Object size) {
		Display.renderScreenImage(viewer, context, size);
	}

	public void requestFocusInWindow(Object canvas) {
		Display.requestFocusInWindow(canvas);
	}

	public void repaint(Object canvas) {
		Display.repaint(canvas);
	}

	public void setTransparentCursor(Object canvas) {
		Display.setTransparentCursor(canvas);
	}

	public void setCursor(int c, Object canvas) {
		Display.setCursor(c, canvas);
	}

	// //// Mouse

	public JmolMouseInterface getMouseManager(Viewer viewer, ActionManager actionManager) {
		return new Mouse(viewer, actionManager);
	}

	// //// Image

	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent) {
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent, this);
	}

	public Object createImage(Object data) {
	  // getFileAsImage
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
	  // only for JpegInfo
		return Image.grabPixels(imageobj, width, height);
	}

	public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object imageobj, int width, int height, int bgcolor) {
		return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width,
				height, bgcolor);
	}

	public int[] getTextPixels(String text, JmolFont font3d, Object context,
			Object image, int width, int height, int ascent) {
		return Image
				.getTextPixels(text, font3d, context, image, width, height, ascent);
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
    /**
     * @j2sNative
     * 
     *  if (typeof Jmol != "undefined" && Jmol._getHiddenContext)
     *    return Jmol._getHiddenContext(this.viewer.htmlName, "textImage", w, h); 
     *  return null;
     */
    {
      return null;
    }
	}

	public boolean waitForDisplay(Object canvas, Object image)
			throws InterruptedException {
		Image.waitForDisplay(canvas, image);
		return true;
	}

	// /// FONT

	public int fontStringWidth(Object context, String text) {
		return Font.stringWidth(context, text);
	}

	public int getFontAscent(Object context) {
		return Font.getAscent(context);
	}

	public int getFontDescent(Object context) {
		return Font.getDescent(context);
	}

	public Object getFontMetrics(JmolFont font, Object context) {
		return Font.getFontMetrics(font, context);
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

}
