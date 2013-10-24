package org.jmol.awtjs2d;

import java.net.URL;


import org.jmol.api.ApiPlatform;
import org.jmol.api.Interface;
import org.jmol.api.JmolFileInterface;
import org.jmol.api.JmolMouseInterface;
import org.jmol.api.JmolPopupInterface;
import org.jmol.util.JmolFont;

import javajs.util.AjaxURLStreamHandlerFactory;
import javajs.util.P3;
import org.jmol.api.PlatformViewer;

/**
 * JavaScript 2D canvas version requires Ajax-based URL stream processing.
 * 
 * Jmol "display" --> HTML5 "canvas"
 * Jmol "image" --> HTML5 "canvas" (because we need width and height)
 * Jmol "graphics" --> HTML5 "context(2d)" (one for display, one off-screen for fonts)
 * Jmol "font" --> JmolFont
 * Jmol "fontMetrics" --> HTML5 "context(2d)"
 * (Not fully implemented) 
 * 
 * @author Bob Hanson
 *
 */
public class Platform implements ApiPlatform {
  Object canvas;
  PlatformViewer viewer;
  Object context;
  
	public void setViewer(PlatformViewer viewer, Object canvas) {
	  /**
	   * @j2sNative
	   * 
     *     this.viewer = viewer;
     *     this.canvas = canvas;
     *     if (canvas != null) {
	   *       this.context = canvas.getContext("2d");
	   *       canvas.imgdata = this.context.getImageData(0, 0, canvas.width, canvas.height);
	   *       canvas.buf8 = canvas.imgdata.data;
	   *     }
	   */
	  {}
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

  public Object getJsObjectInfo(Object[] jsObject, String method, Object[] args) {
    /**
     * we must use Object[] here to hide [HTMLUnknownElement] and [Attribute] from Java2Script
     * @j2sNative
     * 
     * if (method == "localName")return jsObject[0]["nodeName"];
     * return (args == null ? jsObject[0][method] : jsObject[0][method](args[0]));
     * 
     * 
     */
    {
      return null;
    }
  }

  public boolean isHeadless() {
    return false;
  }

  public JmolMouseInterface getMouseManager(double privateKey, Object display) {
    return new Mouse(privateKey, viewer, display);
  }

  // /// Display

	public void convertPointFromScreen(Object canvas, P3 ptTemp) {
	  // from JmolMultiTouchClientAdapter.fixXY
		Display.convertPointFromScreen(canvas, ptTemp);
	}

	public void getFullScreenDimensions(Object canvas, int[] widthHeight) {
		Display.getFullScreenDimensions(canvas, widthHeight);
	}

  public JmolPopupInterface getMenuPopup(String menuStructure,
                                         char type) {
    String c = (type == 'j' ? "awtjs2d.JSmolPopup" : "awtjs2d.JSModelKitPopup");
    JmolPopupInterface jmolpopup = (JmolPopupInterface) Interface
        .getOptionInterface(c);
    try {
      if (jmolpopup != null)
        jmolpopup.jpiInitialize(viewer, menuStructure);
    } catch (Exception e) {
      c = "Exception creating " + c + ":" + e;
      System.out.println(c);
      return null;
    }
    return jmolpopup;
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
	 * @param context
	 * @param size
	 */
	public void renderScreenImage(Object context, Object size) {
		Display.renderScreenImage(viewer, context, size);
	}

  public void drawImage(Object context, Object canvas, int x, int y, int width,
                        int height) {
    
    // from Viewer.render1
    Image.drawImage(context, canvas, x, y, width, height);
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

	// //// Image

	public Object allocateRgbImage(int windowWidth, int windowHeight,
			int[] pBuffer, int windowSize, boolean backgroundTransparent, boolean isImageWrite) {
		return Image.allocateRgbImage(windowWidth, windowHeight, pBuffer,
				windowSize, backgroundTransparent, (isImageWrite ? null : canvas));
	}

  public void notifyEndOfRendering() {
  }

  /**
   * could be byte[] (from ZIP file) or String (local file name) or URL
   * @param data 
   * @return image object
   * 
   */
	public Object createImage(Object data) {
	  // N/A in JS
	  return null;
	}

	public void disposeGraphics(Object gOffscreen) {
	  // N/A
	}

	public int[] grabPixels(Object canvas, int width, int height, 
                          int[] pixels, int startRow, int nRows) {
	  // from PNG and JPG image creators, also g3d.ImageRenderer.plotImage via drawImageToBuffer
	  
	  /**
	   * 
	   * (might be just an object with buf32 defined -- WRITE IMAGE)
	   * 
	   * @j2sNative
	   * 
	   *     if (canvas.image && (width != canvas.width || height != canvas.height))
     *       Jmol._setCanvasImage(canvas, width, height);
	   *     if (canvas.buf32) return canvas.buf32;
	   */
	  {}
    int[] buf = Image.grabPixels(Image.getGraphics(canvas), width, height); 
    /**
     * @j2sNative
     *  
     *  canvas.buf32 = buf;
     * 
     */
    {}
    return buf;
	}

	public int[] drawImageToBuffer(Object gOffscreen, Object imageOffscreen,
			Object canvas, int width, int height, int bgcolor) {
	  return grabPixels(canvas, width, height, null, 0, 0);
	}

	public int[] getTextPixels(String text, JmolFont font3d, Object context,
			Object image, int width, int height, int ascent) {
		return Image.getTextPixels(text, font3d, context, width, height, ascent);
	}

	public void flushImage(Object imagePixelBuffer) {
	  // N/A
	}

	public Object getGraphics(Object image) {
		return Image.getGraphics(image);
	}

  public int getImageHeight(Object canvas) {
		return (canvas == null ? -1 : Image.getHeight(canvas));
	}

	public int getImageWidth(Object canvas) {
		return (canvas == null ? -1 : Image.getWidth(canvas));
	}

	public Object getStaticGraphics(Object image, boolean backgroundTransparent) {
		return Image.getStaticGraphics(image, backgroundTransparent);
	}

	public Object newBufferedImage(Object image, int w, int h) {
    /**
     * @j2sNative
     * 
     *  if (typeof Jmol != "undefined" && Jmol._getHiddenCanvas)
     *    return Jmol._getHiddenCanvas(this.viewer.applet, "stereoImage", w, h); 
     */
    {}
    return null;
	}

	public Object newOffScreenImage(int w, int h) {
    /**
     * @j2sNative
     * 
     *  if (typeof Jmol != "undefined" && Jmol._getHiddenCanvas)
     *    return Jmol._getHiddenCanvas(this.viewer.applet, "textImage", w, h); 
     */
    {}
    return null;
	}

	public boolean waitForDisplay(Object echoNameAndPath, Object zipBytes)
			throws InterruptedException {
  
	  /**
	   * 
	   * this is important specifically for retrieving images from
	   * files, as in set echo ID myimage "image.gif"
	   * 
	   * return will be immediate, before the image is created, so here there is
	   * no "wait." Instead, we give it a callback 
	   * 
	   * @j2sNative
	   * 
     * if (typeof Jmol == "undefined" || !Jmol._getHiddenCanvas) return false;
	   * var viewer = this.viewer;
	   * var sc = viewer.getEvalContextAndHoldQueue(viewer.eval);
	   * var echoName = echoNameAndPath[0];
	   * return Jmol._loadImage(this, echoNameAndPath, zipBytes, 
	   *   function(canvas, pathOrError) { viewer.loadImageData(canvas, pathOrError, echoName, sc) }
	   * );
	   * 
	   */	  
	  {
	    return false;	    
	  }
	}

	// /// FONT

	public int fontStringWidth(JmolFont font, Object context, String text) {
		return Font.stringWidth(font, context, text);
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
		return Font.newFont(fontFace, isBold, isItalic, fontSize, "px");
	}

  public String getDateFormat() {
    /**
     * 
     * Mon Jan 07 2013 19:54:39 GMT-0600 (Central Standard Time)
     * 
     * @j2sNative
     * 
     * return ("" + (new Date())).split(" (")[0];
     */
    {
      return null;
    }
  }

  public JmolFileInterface newFile(String name) {
    return new JSFile(name);
  }

  public Object getBufferedFileInputStream(String name) {
    // n/a for any applet
    return null; 
  }

  public Object getBufferedURLInputStream(URL url, byte[] outputBytes,
                                          String post) {
    return JSFile.getBufferedURLInputStream(url, outputBytes, post);
  }


}
