/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.viewer.g3d;

import java.awt.Color;

public class Colix {

  /****************************************************************
   * colix support
   *
   * a colix is a color index stored as a short
   ****************************************************************/

  public final static short NULL = 0;
  public final static short BLACK = 1;
  public final static short ORANGE = 2;
  public final static short PINK = 3;
  public final static short BLUE = 4;
  public final static short WHITE = 5;
  public final static short AQUA = 6;
  public final static short CYAN = 6;
  public final static short RED = 7;
  public final static short GREEN = 8;
  public final static short GRAY = 9;
  public final static short SILVER = 10;
  public final static short LIGHTGRAY = 10;
  public final static short LIME = 11;
  public final static short MAROON = 12;
  public final static short NAVY = 13;
  public final static short OLIVE = 14;
  public final static short PURPLE = 15;
  public final static short TEAL = 16;
  public final static short MAGENTA = 17;
  public final static short FUCHSIA = 17;
  public final static short YELLOW = 18;

  static Color[] colorsPredefined = {
    Color.black, Color.orange, Color.pink, Color.blue,
    Color.white, Color.cyan, Color.red, new Color(0, 128, 0),
    Color.gray, Color.lightGray, Color.green, new Color(128, 0, 0),
    new Color(0, 0, 128), new Color(128, 128, 0), new Color(128, 0, 128),
    new Color(0, 128, 128), Color.magenta, Color.yellow
  };

  static short colixMax = 1;
  public static int[] argbs = new int[128];
  public static Color[] colors = new Color[128];
  static int[][] ashades = new int[128][];

  static {
    for (int i = 0; i < colorsPredefined.length; ++i)
      getColix(colorsPredefined[i]);
  }

  public static short getColix(int argb) {
    if (argb == 0)
      return 0;
    argb |= 0xFF000000;
    for (int i = colixMax; --i >= 0; )
      if (argb == argbs[i])
        return (short)i;
    if (colixMax == argbs.length) {
      int oldSize = argbs.length;
      int[] t0 = new int[oldSize * 2];
      System.arraycopy(argbs, 0, t0, 0, oldSize);
      argbs = t0;

      Color[] t1 = new Color[oldSize * 2];
      System.arraycopy(colors, 0, t1, 0, oldSize);
      colors = t1;

      int[][] t2 = new int[oldSize * 2][];
      System.arraycopy(ashades, 0, t2, 0, oldSize);
      ashades = t2;
    }
    argbs[colixMax] = argb;
    return colixMax++;
  }

  public static short getColix(Color color) {
    if (color == null)
      return 0;
    int argb = color.getRGB();
    short colix = getColix(argb);
    if (colors[colix] == null && (argb & 0xFF000000) == 0xFF000000)
      colors[colix] = color;
    return colix;
  }

  public static Color getColor(short colix) {
    if (colix == 0)
      return null;
    Color color = colors[colix];
    if (color == null)
      color = colors[colix] = new Color(argbs[colix]);
    return colors[colix];
  }

  public static int getArgb(short colix) {
    return argbs[colix];
  }

  public static int[] getShades(short colix) {
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = Shade3D.getShades(argbs[colix]);
    return shades;
  }

  public static void flushShades() {
    for (int i = colixMax; --i >= 0; )
      ashades[i] = null;
  }
}
