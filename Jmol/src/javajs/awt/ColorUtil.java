package javajs.awt;

import javajs.api.GenericColor;

public class ColorUtil {

  public static String toRGBHexString(GenericColor c) {
    int rgb = c.getRGB();    
    if (rgb == 0)
      return "000000";
    String r  = "00" + Integer.toHexString((rgb >> 16) & 0xFF);
    r = r.substring(r.length() - 2);
    String g  = "00" + Integer.toHexString((rgb >> 8) & 0xFF);
    g = g.substring(g.length() - 2);
    String b  = "00" + Integer.toHexString(rgb & 0xFF);
    b = b.substring(b.length() - 2);
    return r + g + b;
  }

  public static String toCSSString(GenericColor c) {
    int opacity = c.getOpacity255();
    if (opacity == 255)
      return "#" + toRGBHexString(c);
    int rgb = c.getRGB();
    return "rgba(" + ((rgb>>16)&0xFF) + "," + ((rgb>>8)&0xff) + "," + (rgb&0xff) + "," + opacity/255f  + ")"; 
  }
  
}


