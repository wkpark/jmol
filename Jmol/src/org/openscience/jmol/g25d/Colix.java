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

package org.openscience.jmol.g25d;

import org.openscience.jmol.*;

import java.awt.Color;
import java.util.Hashtable;

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

  static Color[] acolorPredefined = {
    Color.black, Color.orange, Color.pink, Color.blue,
    Color.white, Color.cyan, Color.red, new Color(0, 128, 0),
    Color.gray, Color.lightGray, Color.green, new Color(128, 0, 0),
    new Color(0, 0, 128), new Color(128, 128, 0), new Color(128, 0, 128),
    new Color(0, 128, 128), Color.magenta, Color.yellow
  };

  static short colixMax = 1;
  public static Color[] acolor = new Color[128];
  public static short[] acolixDarker = new short[128];
  static int[][] ashades = new int[128][];
  private static Hashtable htColix = new Hashtable();

  static {
    for (int i = 0; i < acolorPredefined.length; ++i)
      getColix(acolorPredefined[i]);
  }

  public static short getColix(int argb) {
    argb |= 0xFF000000;
    return getColix(new Color(argb));
  }

  public static short getColix(Color color) {
    if (color == null)
      return 0;
    if ((color.getRGB() & 0xFF000000) != 0xFF000000)
      color = new Color(color.getRGB() | 0xFF000000);
    Short colixBoxed = (Short) htColix.get(color);
    if (colixBoxed != null)
      return colixBoxed.shortValue();
    if (colixMax == acolor.length) {
      int oldSize = acolor.length;
      Color[] t1 = new Color[oldSize * 2];
      System.arraycopy(acolor, 0, t1, 0, oldSize);
      acolor = t1;

      int[][] t2 = new int[oldSize * 2][];
      System.arraycopy(ashades, 0, t2, 0, oldSize);
      ashades = t2;

      short[] t3 = new short[oldSize * 2];
      System.arraycopy(acolixDarker, 0, t3, 0, oldSize);
      acolixDarker = t3;
    }
    acolor[colixMax] = color;
    htColix.put(color, new Short(colixMax));
    return colixMax++;
  }

  public static Color getColor(short colix) {
    return acolor[colix];
  }

  public static int getArgb(short colix) {
    return acolor[colix].getRGB();
  }

  public static int[] getShades(short colix) {
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = Shade25D.getShades(acolor[colix]);
    return shades;
  }

  public static short getColixDarker(short colix) {
    short darker = acolixDarker[colix];
    if (darker == 0)
      darker = acolixDarker[colix] =
        getColix(getShades(colix)[Shade25D.shadeDarker]);
    return darker;
  }
}
