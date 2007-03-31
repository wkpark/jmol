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
package org.openscience.jvxl;

class ColorEncoder {
  private final static int RED  = 0xFFFF0000;
  private final static int BLUE = 0xFF0000FF;
  private final static int GRAY = 0xFF808080;
  
  private int palette = 0;
  
  private final static String[] colorSchemes = {"roygb", "bgyor", "rwb", "bwr", "low", "high"}; 
  private final static int ROYGB = 0;
  private final static int BGYOR = 1;
  private final static int RWB   = 2;
  private final static int BWR   = 3;
  private final static int LOW   = 4;
  private final static int HIGH  = 5;
  
  ColorEncoder() {
  }
  
  int setColorScheme(String colorScheme) {
    palette = 0;
    for (int i = 0; i < colorSchemes.length; i++)
      if (colorSchemes[i].equalsIgnoreCase(colorScheme))
        return (palette = i);
    return palette;
  }
  
  int getColorNegative() {
    return RED;
  }

  int getColorPositive() {
    return BLUE;
  }
  
  int getColorFromPalette(float val, float lo, float hi) {
    switch (palette) {
    case ROYGB:
      return argbsRoygbScale[quantize(val, lo, hi, argbsRoygbScale.length)];
    case BGYOR:
      return argbsRoygbScale[quantize(-val, -hi, -lo, argbsRoygbScale.length)];
    case LOW:
      return argbsRoygbScale[quantize(val, lo, hi, ihalf)];
    case HIGH:
      return argbsRoygbScale[ihalf + quantize(val, lo, hi, ihalf)];
    case RWB:
      return argbsRwbScale[quantize(val, lo, hi, argbsRwbScale.length)];
    case BWR:
      return argbsRwbScale[quantize(-val, -hi, -lo, argbsRwbScale.length)];
    }
    return GRAY;
  }

  static int quantize(float val, float lo, float hi, int segmentCount) {
    float range = hi - lo;
    if (range <= 0 || Float.isNaN(val))
      return segmentCount / 2;
    float t = val - lo;
    if (t <= 0)
      return 0;
    float quanta = range / segmentCount;
    int q = (int)(t / quanta + 0.0001f);
    if (q >= segmentCount)
      q = segmentCount - 1;
    return q;
  }


  int ihalf = argbsRoygbScale.length/3;

  public final static int[] argbsRwbScale = {
    0xFFFF0000, // red
    0xFFFF1010, //
    0xFFFF2020, //
    0xFFFF3030, //
    0xFFFF4040, //
    0xFFFF5050, //
    0xFFFF6060, //
    0xFFFF7070, //
    0xFFFF8080, //
    0xFFFF9090, //
    0xFFFFA0A0, //
    0xFFFFB0B0, //
    0xFFFFC0C0, //
    0xFFFFD0D0, //
    0xFFFFE0E0, //
    0xFFFFFFFF, // white
    0xFFE0E0FF, //
    0xFFD0D0FF, //
    0xFFC0C0FF, //
    0xFFB0B0FF, //
    0xFFA0A0FF, //
    0xFF9090FF, //
    0xFF8080FF, //
    0xFF7070FF, //
    0xFF6060FF, //
    0xFF5050FF, //
    0xFF4040FF, //
    0xFF3030FF, //
    0xFF2020FF, //
    0xFF1010FF, //
    0xFF0000FF, // blue
  };

  public final static int[] argbsRoygbScale = {
    0xFFFF0000,
    0xFFFF2000,
    0xFFFF4000,
    0xFFFF6000,
    0xFFFF8000,
    0xFFFFA000,
    0xFFFFC000,
    0xFFFFE000,

    0xFFFFF000, // yellow gets compressed, so give it an extra boost

    0xFFFFFF00,
    0xFFF0F000, // yellow gets compressed, so give it a little boost
    0xFFE0FF00,
    0xFFC0FF00,
    0xFFA0FF00,
    0xFF80FF00,
    0xFF60FF00,
    0xFF40FF00,
    0xFF20FF00,

    0xFF00FF00,
    0xFF00FF20,
    0xFF00FF40,
    0xFF00FF60,
    0xFF00FF80,
    0xFF00FFA0,
    0xFF00FFC0,
    0xFF00FFE0,

    0xFF00FFFF,
    0xFF00E0FF,
    0xFF00C0FF,
    0xFF00A0FF,
    0xFF0080FF,
    0xFF0060FF,
    0xFF0040FF,
    0xFF0020FF,

    0xFF0000FF,
  };


}
