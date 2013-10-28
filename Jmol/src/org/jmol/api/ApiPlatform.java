package org.jmol.api;


import java.net.URL;


import javajs.api.FontManager;
import javajs.awt.Font;
import javajs.util.P3;

public interface ApiPlatform extends FontManager {

  public final static int CURSOR_DEFAULT = 0;
  public final static int CURSOR_CROSSHAIR = 1;
  public final static int CURSOR_WAIT = 3;
  public final static int CURSOR_ZOOM = 8;
  public final static int CURSOR_HAND = 12;
  public final static int CURSOR_MOVE = 13;

  void setViewer(PlatformViewer viewer, Object display);
  
  /////// Display

  boolean isHeadless();
  
  void convertPointFromScreen(Object display, P3 ptTemp);

  void getFullScreenDimensions(Object display, int[] widthHeight);
  
  boolean hasFocus(Object display);

  String prompt(String label, String data, String[] list, boolean asButtons);

  void repaint(Object display);

  void requestFocusInWindow(Object display);

  void setCursor(int i, Object display);

  void setTransparentCursor(Object display);

  ////  Mouse 

  JmolMouseInterface getMouseManager(double privateKey, Object display);

  ///// core Image handling
  
  Object allocateRgbImage(int windowWidth, int windowHeight, int[] pBuffer,
                          int windowSize, boolean backgroundTransparent, boolean isImageWrite);

  void disposeGraphics(Object graphicForText);

  void drawImage(Object g, Object img, int x, int y, int width, int height);

  int[] drawImageToBuffer(Object gObj, Object imageOffscreen,
                          Object image, int width, int height, int bgcolor);

  void flushImage(Object imagePixelBuffer);

  Object getStaticGraphics(Object image, boolean backgroundTransparent);

  Object getGraphics(Object image);

  int getImageWidth(Object image);

  int getImageHeight(Object image);

  Object newBufferedImage(Object image, int i, int height);

  Object newOffScreenImage(int w, int h);
  
  void renderScreenImage(Object g, Object currentSize);

  int[] getTextPixels(String text, Font font3d, Object gObj,
                      Object image, int mapWidth, int height,
                      int ascent);

  ///// Image creation for export (optional for any platform)

  /**
   * can be ignored (return null) if platform cannot save images
   * 
   * @param ret
   * @return     null only if this platform cannot save images
   */
  Object createImage(Object ret);

  /**
   * 
   * @param image
   * @param width
   * @param height
   * @param pixels 
   * @param startRow 
   * @param nRows 
   * @return         pixels
   */
  int[] grabPixels(Object image, int width, int height, 
                   int[] pixels, int startRow, int nRows);

  /**
   * can be ignored (return false) if platform cannot save images
   * 
   * @param boolIsEcho
   * @param image
   * @return        false only if this platform cannot save images
   * @throws InterruptedException
   */
  boolean waitForDisplay(Object boolIsEcho, Object image) throws InterruptedException;

  JmolPopupInterface getMenuPopup(String menuStructure, char type);

  Object getJsObjectInfo(Object[] jsObject, String method, Object[] args);

  boolean isSingleThreaded();

  void notifyEndOfRendering();

  String getDateFormat();
  
  JmolFileInterface newFile(String name);
  
  Object getBufferedFileInputStream(String name);
  
  Object getBufferedURLInputStream(URL url, byte[] outputBytes, String post);

}
