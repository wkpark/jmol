/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

package org.jmol.g3d;

import java.awt.Color;

/**
 *<p>
 * Implements a color index model using a colix as a
 * <strong>COLor IndeX</strong>.
 *</p>
 *<p>
 * A colix is a color index represented as a short int.
 *</p>
 *<p>
 * The value 0 is considered a null value ... for no color. In Jmol this
 * generally means that the value is inherited from some other object.
 *</p>
 *<p>
 * The value 1 is used to indicate TRANSLUCENT, but with the color
 * coming from the parent. The value 2 indicates OPAQUE, but with the
 * color coming from the parent.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final class Colix {


  private static int colixMax = 1; // skip the null entry
  private static int[] argbs = new int[128];
  private static Color[] colors = new Color[128];
  private static int[][] ashades = new int[128][];

  final static short getColix(int argb) {
    if (argb == 0)
      return 0;
    int translucent = 0;
    if ((argb & 0xFF000000) != 0xFF000000) {
      if ((argb & 0xFF000000) == 0) {
        System.out.println("zero alpha channel + non-zero rgb not supported");
        throw new IndexOutOfBoundsException();
      }
      argb |= 0xFF000000;
      translucent = Graphics3D.TRANSLUCENT_MASK;
    }
    for (int i = colixMax; --i >= Graphics3D.SPECIAL_COLIX_MAX; )
      if (argb == argbs[i])
        return (short)(i | translucent);
    return (short)(allocateColix(argb, null) | translucent);
  }

  final static short getColix(Color color) {
    if (color == null)
      return 0;
    int argb = color.getRGB();
    if (argb == 0)
      return 0;
    int translucent = 0;
    if ((argb & 0xFF000000) != 0xFF000000) {
      argb |= 0xFF000000;
      translucent = Graphics3D.TRANSLUCENT_MASK;
    }
    for (int i = colixMax; --i >= Graphics3D.SPECIAL_COLIX_MAX; )
      if (argb == argbs[i])
        return (short)(i | translucent);
    return (short)(allocateColix(argb, translucent == 0 ? color : null) |
                   translucent);
  }

  private synchronized static int allocateColix(int argb, Color opaqueColor) {
    // double-check to make sure that someone else did not allocate
    // something of the same color while we were waiting for the lock
    if ((argb & 0xFF000000) != 0xFF000000)
      throw new IndexOutOfBoundsException();
    for (int i = colixMax; --i >= Graphics3D.SPECIAL_COLIX_MAX; )
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
    colors[colixMax] = (opaqueColor != null) ? opaqueColor : new Color(argb);
    return colixMax++;
  }

  final static Color getColor(short colix) {
    if (colix == 0)
      return null;
    return colors[colix & Graphics3D.OPAQUE_MASK];
  }

  final static int getRgb(short colix) {
    return argbs[colix & Graphics3D.OPAQUE_MASK];
  }

  final static boolean isTranslucent(short colix) {
    return (colix & Graphics3D.TRANSLUCENT_MASK) != 0;
  }

  final static int[] getShades(short colix) {
    colix &= Graphics3D.OPAQUE_MASK;
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = Shade3D.getShades(argbs[colix]);
    return shades;
  }

  final static void flushShades() {
    for (int i = colixMax; --i >= 0; )
      ashades[i] = null;
  }
}
