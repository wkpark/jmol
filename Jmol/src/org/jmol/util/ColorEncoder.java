/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:09:49 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7221 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.util;

import java.util.Enumeration;
import java.util.Hashtable;

import org.jmol.viewer.JmolConstants;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.ArrayUtil;

/*
 * 
 * just a simple class using crude color encoding
 * 
 * 
 */


 public class ColorEncoder {
  public ColorEncoder() {
  }
    
  
  private final static String[] colorSchemes = {"roygb", "bgyor", "rwb", "bwr", "low", "high", "user", "resu"}; 
  private final static int ROYGB = 0;
  private final static int BGYOR = 1;
  private final static int RWB   = 2;
  private final static int BWR   = 3;
  private final static int LOW   = 4;
  private final static int HIGH  = 5;
  private final static int USER = -6;
  private final static int RESU = -7;

  private int palette = ROYGB;

  private static int[] userScale = new int[0];
  private static int[] thisScale = new int[0];
  private static String thisName = "scheme";

  private static Hashtable schemes = new Hashtable();

  private static synchronized int makeColorScheme(String name, int[] scale) {
    if (scale == null) {
      schemes.remove(name);
      return getColorScheme(name);
    }
    schemes.put(thisName = name, thisScale = scale);
    return -1;
  }
  
  public int setColorScheme(String colorScheme) {
    return palette = getColorScheme(colorScheme);
  }

  public String getColorSchemeName() {
    return getColorSchemeName(palette);  
  }
  
  public final static String getColorSchemeName(int i) {
    int absi = Math.abs(i);
    return (i == -1 ? thisName : absi < colorSchemes.length && absi >= 0 ? colorSchemes[absi] : null);  
  }
  
  public final static int getColorScheme(String colorScheme) {
    int pt = colorScheme.indexOf("[");
    if (pt >= 0) {
      String name = TextFormat.replaceAllCharacters(
          colorScheme.substring(0, pt), " =:", "");
      int n = 0;
      pt = -1;
      while ((pt = colorScheme.indexOf("[", pt + 1)) >= 0)
        n++;
      if (n == 0)
        return makeColorScheme(name, null);
      int[] scale = new int[n];
      pt = -1;
      n = 0;
      int c;
      while ((pt = colorScheme.indexOf("[", pt + 1)) >= 0) {
        scale[n++] = c = Graphics3D.getArgbFromString(colorScheme.substring(pt,
            pt + 9));
        if (c == 0)
          return ROYGB;
      }
      if (name.equals("user")) {
        setUserScale(scale);
        return USER;
      }
      return makeColorScheme(name, scale);
    }
    if (schemes.containsKey(colorScheme)) {
      thisName = colorScheme;
      thisScale = (int[]) schemes.get(colorScheme);
      return -1;
    }
    for (int i = 0; i < colorSchemes.length; i++)
      if (colorSchemes[i].equalsIgnoreCase(colorScheme))
        return i;
    return ROYGB;
  }

  private final static int ihalf = JmolConstants.argbsRoygbScale.length/3;
  
  public final static void setUserScale(int[] scale) {
    userScale = scale;  
    makeColorScheme("user", scale);
  }
  
  public final static String getState() {
    StringBuffer s = new StringBuffer("");
    Enumeration e = schemes.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (name.length() > 0 & n++ >= 0) 
        s.append("color \"" + name + "=" + getColorSchemeList((int[])schemes.get(name)) + "\";\n");
    }
    //String colors = getColorSchemeList(getColorSchemeArray(USER));
    //if (colors.length() > 0)
      //s.append("userColorScheme = " + colors + ";\n");
    return (n > 0 ? s.append("\n").toString() : "");
  }
  
  public static String getColorSchemeList(int[] scheme) {
    String colors = "";
    for (int i = 0; i < scheme.length; i++)
      colors += (i == 0 ? "" : " ") + Escape.escapeColor(scheme[i]);
    return colors;
  }

  public final static int[] getColorSchemeArray(int palette) {
    switch (palette) {
    /*    case RGB:
     c = quantizeRgb(val, lo, hi, rgbRed, rgbGreen, rgbBlue);
     break;
     */
    case -1:
      return ArrayUtil.arrayCopy(thisScale, 0, -1, false);      
    case ROYGB:
      return ArrayUtil.arrayCopy(JmolConstants.argbsRoygbScale, 0, -1, false);
    case BGYOR:
      return ArrayUtil.arrayCopy(JmolConstants.argbsRoygbScale, 0, -1, true);
    case LOW:
      return ArrayUtil.arrayCopy(JmolConstants.argbsRoygbScale, 0, ihalf, false);
    case HIGH:
      int[] a = ArrayUtil.arrayCopy(JmolConstants.argbsRoygbScale, ihalf, -1, false);
      int[] b = new int[ihalf];
      for (int i = ihalf; --i >= 0;)
        b[i] = a[i + i];
      return b;
    case RWB:
      return ArrayUtil.arrayCopy(JmolConstants.argbsRwbScale, 0, -1, false);
    case BWR:
      return ArrayUtil.arrayCopy(JmolConstants.argbsRwbScale, 0, -1, true);
    case USER:
      return ArrayUtil.arrayCopy(userScale, 0, -1, false);
    case RESU:
      return ArrayUtil.arrayCopy(userScale, 0, -1, true);
    default:
      return null;
    }

  }
  
  public final static short getColorIndexFromPalette(float val, float lo, float hi, int palette) {//, int rgbRed, int rgbGreen, int rgbBlue) {
    int c = 0;
    switch (palette) {
/*    case RGB:
      c = quantizeRgb(val, lo, hi, rgbRed, rgbGreen, rgbBlue);
      break;
*/
    case -1:
      c = thisScale[quantize(val, lo, hi, thisScale.length)];
      break;
    case ROYGB:
      c = JmolConstants.argbsRoygbScale[quantize(val, lo, hi, JmolConstants.argbsRoygbScale.length)];
      break;
    case BGYOR:
      c = JmolConstants.argbsRoygbScale[quantize(-val, -hi, -lo, JmolConstants.argbsRoygbScale.length)];
      break;
    case LOW:
      c = JmolConstants.argbsRoygbScale[quantize(val, lo, hi, ihalf)];
      break;
    case HIGH:
      c = JmolConstants.argbsRoygbScale[ihalf + quantize(val, lo, hi, ihalf) * 2];
      break;
    case RWB:
      c = JmolConstants.argbsRwbScale[quantize(val, lo, hi, JmolConstants.argbsRwbScale.length)];
      break;
    case BWR:
      c = JmolConstants.argbsRwbScale[quantize(-val, -hi, -lo, JmolConstants.argbsRwbScale.length)];
      break;
    case USER:
      c = (userScale.length == 0 ? 0xFF808080 : userScale[quantize(val, lo, hi, userScale.length)]);
      break;
    case RESU:
      c = (userScale.length == 0 ? 0xFF808080 : userScale[quantize(-val, -hi, -lo, userScale.length)]);
      break;
    default:
      c = 0xFF808080; // GRAY
    }
    return getColorIndex(c);
  }


  public final static short getColorIndex(int c) {
    return Graphics3D.getColix(c);
  }

  public final static int quantize(float val, float lo, float hi, int segmentCount) {
    /* oy! Say you have an array with 10 values, so segmentCount=10
     * then we expect 0,1,2,...,9  EVENLY
     * If f = fractional distance from lo to hi, say 0.0 to 10.0 again,
     * then one might expect 10 even placements. BUT:
     * (int) (f * segmentCount + 0.5) gives
     * 
     * 0.0 ---> 0
     * 0.5 ---> 1
     * 1.0 ---> 1
     * 1.5 ---> 2
     * 2.0 ---> 2
     * ...
     * 8.5 ---> 9
     * 9.0 ---> 9
     * 9.5 ---> 10 --> 9
     * 
     * so the first bin is underloaded, and the last bin is overloaded.
     * With integer quantities, one would not notice this, because
     * 0, 1, 2, 3, .... --> 0, 1, 2, 3, .....
     * 
     * but with fractional quantities, it will be noticeable.
     * 
     * What we really want is:
     * 
     * 0.0 ---> 0
     * 0.5 ---> 0
     * 1.0 ---> 1
     * 1.5 ---> 1
     * 2.0 ---> 2
     * ...
     * 8.5 ---> 8
     * 9.0 ---> 9
     * 9.5 ---> 9
     * 
     * that is, no addition of 0.5. 
     * Instead, I add 0.0001, just for discreteness sake.
     * 
     * Bob Hanson, 5/2006
     * 
     */
    float range = hi - lo;
    if (range <= 0 || Float.isNaN(val))
      return segmentCount / 2;
    float t = val - lo;
    if (t <= 0)
      return 0;
    float quanta = range / segmentCount;
    int q = (int)(t / quanta + 0.0001f);  //was 0.5f!
    if (q >= segmentCount)
      q = segmentCount - 1;
    return q;
  }

  public short getColorIndexFromPalette(float val, float lo, float hi) {
    return getColorIndexFromPalette(val, lo, hi, palette);
  }

/*  
  //an idea that didn't work
  public short getColorIndexFromPalette(float val, float lo, float hi) {
    return getColorIndexFromPalette(val, lo, hi, palette, rgbRed, rgbGreen, rgbBlue);
  }
  private final static int RGB   = 6;
  private final static String[] colorSchemes = {"roygb", "bgyor", "rwb", "bwr", "low", "high", "rgb"}; 
  private int rgbRed = 0xFFFF0000;
  private int rgbGreen = 0xFF008000;
  private int rgbBlue = 0xFF0000FF;
  
  public void setRgbRed(int color) {
    rgbRed = color;
  }
  
  public void setRgbGreen(int color) {
    rgbGreen = color;
  }
  
  public void setRgbBlue(int color) {
    rgbBlue = color;
  }
  
  public int getColorLow() {
    return rgbRed;
  }

  public int getColorCentral() {
    return rgbGreen;
  }

  public int getColorHigh() {
    return rgbBlue;
  }
  
  public final static int quantizeRgb(float val, float lo, float hi,
                                      int rgbLow, int rgbMid, int rgbHigh) {
    int pt = quantize(val, lo, hi, 256);
    int r, g, b;
    if (pt < 128) {
      r = interpolate(pt, (rgbLow & 0xFF0000) >> 16, (rgbMid & 0xFF0000) >> 16);
      g = interpolate(pt, (rgbLow & 0xFF00) >> 8, (rgbMid & 0xFF00) >> 8);
      b = interpolate(pt, (rgbLow & 0xFF), (rgbMid & 0xFF));
    } else {
      r = interpolate(pt - 128, (rgbMid & 0xFF0000) >> 16,
          (rgbHigh & 0xFF0000) >> 16);
      g = interpolate(pt - 128, (rgbMid & 0xFF00) >> 8, (rgbHigh & 0xFF00) >> 8);
      b = interpolate(pt - 128, (rgbMid & 0xFF), (rgbHigh & 0xFF));
    }
    System.out.println(Integer.toHexString(0xFF000000 | r << 16 | g << 8 | b));
    return 0xFF000000 | r << 16 | g << 8 | b;
  }
  
  private final static int interpolate(int pt, int a, int b) {
    if (pt >= 127)
      return b; //corrects for FF being the upper limit; all others truncated
    return ((int) (pt / 128.0 * (b - a) + a) * 2) & 0xFF;
  }
  
*/
}
