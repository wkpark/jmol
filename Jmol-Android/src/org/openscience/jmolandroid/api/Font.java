package org.openscience.jmolandroid.api;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * methods required by Jmol that access java.awt.Font
 * 
 * private to org.jmol.awt
 * 
 */

class Font {

  static Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    int style = Typeface.NORMAL | (isBold ? Typeface.BOLD : 0) | (isItalic ? Typeface.ITALIC : 0);
    Typeface typeface = Typeface.create(fontFace, style);    
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    paint.setTypeface(typeface);
    paint.setTextSize(fontSize);    
    return paint;
  }

  static Object getFontMetrics(Object graphics, Object paint) {
    // just use Paint object
    return paint;
  }

  static int getAscent(Object paint) {
    return Math.abs((int)((Paint) paint).getFontMetrics().ascent);
  }

  static int getDescent(Object paint) {
    return Math.abs((int)((Paint) paint).getFontMetrics().descent);
  }

  static int stringWidth(Object paint, String text) {
    return (int)((Paint) paint).measureText(text);
  }
}
