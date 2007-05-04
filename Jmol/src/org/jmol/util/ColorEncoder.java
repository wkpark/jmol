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

import org.jmol.viewer.JmolConstants;
import org.jmol.g3d.Graphics3D;

/*
 * 
 * just a simple class using crude color encoding
 * 
 * 
 */


 public class ColorEncoder {
  public ColorEncoder() {
  }
    
  private int palette = 0;
  
  private final static String[] colorSchemes = {"roygb", "bgyor", "rwb", "bwr", "low", "high"}; 
  private final static int ROYGB = 0;
  private final static int BGYOR = 1;
  private final static int RWB   = 2;
  private final static int BWR   = 3;
  private final static int LOW   = 4;
  private final static int HIGH  = 5;

  public int setColorScheme(String colorScheme) {
    return palette = getColorScheme(colorScheme);
  }

  public final static int getColorScheme(String colorScheme) {
    for (int i = 0; i < colorSchemes.length; i++)
      if (colorSchemes[i].equalsIgnoreCase(colorScheme))
        return i;
    return 0;
  }

  private final static int ihalf = JmolConstants.argbsRoygbScale.length/3;

  public final static short getColorIndexFromPalette(float val, float lo, float hi, int palette) {//, int rgbRed, int rgbGreen, int rgbBlue) {
    int c = 0;
    switch (palette) {
/*    case RGB:
      c = quantizeRgb(val, lo, hi, rgbRed, rgbGreen, rgbBlue);
      break;
*/
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
      c = JmolConstants.argbsRoygbScale[ihalf + quantize(val, lo, hi, ihalf)];
      break;
    case RWB:
      c = JmolConstants.argbsRwbScale[quantize(val, lo, hi, JmolConstants.argbsRwbScale.length)];
      break;
    case BWR:
      c = JmolConstants.argbsRwbScale[quantize(-val, -hi, -lo, JmolConstants.argbsRwbScale.length)];
      break;
    default:
      c = 0x808080; // GRAY
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
