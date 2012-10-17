package org.jmol.awtjs2d;

import org.jmol.util.JmolFont;

/**
 * methods required by Jmol that access java.awt.Font
 * 
 * private to org.jmol.awt
 * 
 */

class Font {

  static Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    return (isBold ? "bold " : isItalic ? "italic " : "normal ") + fontSize + "px " + fontFace;
  }

  /**
   * @param font 
   * @param context  
   * @return the context
   */
  static Object getFontMetrics(JmolFont font, Object context) {
    /**
     * 
     * @j2sNative
     * 
     * if (context.font != font.font) {
     *  context.font = font.font;
     *  context._fontAscent = font.fontSize; 
     *  context._fontDescent = 0;
     * }
     */
    return context;
  }

  /**
   * @param context  
   * @return height of the font 
   */
  static int getAscent(Object context) {
    /**
     * 
     * @j2sNative
     * 
     * return context._fontAscent;
     */
    {
    return 0;
    }
  }

  /**
   * @param context  
   * @return something other than 0?
   */
  static int getDescent(Object context) {
    /**
     * 
     * @j2sNative
     * 
     * return context._fontDescent
     */
    {
    return 0;
    }
  }

  /**
   * @param context 
   * @param text 
   * @return width
   */
  static int stringWidth(Object context, String text) {
    /**
     * @j2sNative
     * return context.measureText(text).width;
     */
    return 0;
  }
}
