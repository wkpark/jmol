package org.openscience.jmolandroid.api;

import org.jmol.util.P3;

import org.jmol.api.ApiPlatform;
import org.jmol.api.JmolFileAdapterInterface;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolMouseInterface;
import org.jmol.api.JmolPopupInterface;
import org.jmol.api.JmolViewer;
import org.jmol.util.JmolFont;
import org.jmol.viewer.ActionManager;
import org.jmol.viewer.Viewer;

public class Platform implements ApiPlatform {

  public void setViewer(JmolViewer viewer, Object display) {
    ((AndroidUpdateListener) display).setViewer(viewer);
  }
  
  ///// Display -- AndroidUpdateListener
  
  @Override
  public void convertPointFromScreen(Object display, P3 ptTemp) {
    // unnecessary
  }

  @Override
  public void getFullScreenDimensions(Object display, int[] widthHeight) {
    ((AndroidUpdateListener) display).getScreenDimensions(widthHeight);    
  }

  public JmolPopupInterface getMenuPopup(Viewer viewer, String menuStructure,
                                         char type) {
    // ignored
    return null;
  }

  public boolean hasFocus(Object display) {
    // ignored
    return true;
  }

  public String prompt(String label, String data, String[] list,
                       boolean asButtons) {
    // TODO Auto-generated method stub
    return null;
  }

  public void requestFocusInWindow(Object display) {
    // ignored
  }

  public void repaint(Object display) {
    ((AndroidUpdateListener) display).repaint();
  }

  public void renderScreenImage(JmolViewer viewer, Object g, Object size) {
    // ignored -- legacy Java apps only
  }

  public void setTransparentCursor(Object display) {
    // ignored
  }

  public void setCursor(int c, Object display) {
    // ignored
  }

  ////// Mouse

  public JmolMouseInterface getMouseManager(Viewer viewer, ActionManager actionManager) {
  	return new Mouse(viewer, actionManager);
  }

  ////// Image 

	@Override
	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent,
			boolean isImageWrite) {
    return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer, windowSize, backgroundTransparent);
  }

  public Object createImage(Object data) {
    return Image.createImage(data);
  }

  public void disposeGraphics(Object graphicForText) {
    Image.disposeGraphics(graphicForText);
  }

  public void drawImage(Object graphic, Object img, int x, int y, int width, int height) {
    Image.drawImage(graphic, img, x, y, width, height);
  }

  public int[] grabPixels(Object imageobj, int width, int height, 
      int[] pixels, int startRow, int nRows) {
  	// not to be used for PNG or JPG creation in JmolAndroid
    return Image.grabPixels(imageobj, width, height);
  }

  public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
                                 Object imageobj, int width, int height, int bgcolor) {
    return Image.drawImageToBuffer(gOffscreen, imageOffscreen, imageobj, width, height, bgcolor);
  }

  public int[] getTextPixels(String text, JmolFont font3d, Object gObj,
                             Object image, int width, int height, int ascent) {
    return Image.getTextPixels(text, font3d, gObj, image, width, height, ascent);
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
    return Image.newOffScreenImage(w, h);
  }

  public boolean waitForDisplay(Object display, Object image) throws InterruptedException {
    Image.waitForDisplay(display, image);
    return true;
  }

  
  ///// FONT
  
	public int fontStringWidth(JmolFont font, Object fontMetrics, String text) {
    return Font.stringWidth(fontMetrics, text);
  }

  public int getFontAscent(Object fontMetrics) {
    return Font.getAscent(fontMetrics);
  }

  public int getFontDescent(Object fontMetrics) {
    return Font.getDescent(fontMetrics);
  }

  public Object getFontMetrics(JmolFont font, Object graphics) {
    return Font.getFontMetrics(font, graphics);
  }

  public Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    return Font.newFont(fontFace, isBold, isItalic, fontSize);
  }

  public Object getJsObjectInfo(Object[] jsObject, String method, Object[] args) {
    return null;
  }

  public boolean isHeadless() {
    return false;
  }

  public JmolFileAdapterInterface getFileAdapter() {
    return new JmolFileAdapter();
  }

  public JmolFileInterface newFile(String name) {
  	return JmolFileAdapter.newFile(name);
  }
  
  public boolean isSingleThreaded() {
    return false;
  }

  public void notifyEndOfRendering() {
    // N/A
  }
  
	public String getDateFormat() {
		return null;
	}


}


