package org.jmol.awt;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Hashtable;
import java.util.Map;

public class Font {

  public static Object newFont(String fontFace, boolean isBold, boolean isItalic, float fontSize) {
    Map<Attribute, Object> fontMap = new Hashtable<Attribute, Object>();
    fontMap.put(TextAttribute.FAMILY, fontFace);
    if (isBold)
      fontMap.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
    if (isItalic)
      fontMap.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
    fontMap.put(TextAttribute.SIZE, new Float(fontSize));
    return new java.awt.Font(fontMap);
  }

  public static Object getFontMetrics(Object graphics, Object font) {
    return ((Graphics) graphics).getFontMetrics((java.awt.Font) font);
  }

  public static int getAscent(Object fontMetrics) {
    return ((FontMetrics) fontMetrics).getAscent();
  }

  public static int getDescent(Object fontMetrics) {
    return ((FontMetrics) fontMetrics).getDescent();
  }

  public static int stringWidth(Object fontMetrics, String text) {
    return ((FontMetrics) fontMetrics).stringWidth(text);
  }
}
