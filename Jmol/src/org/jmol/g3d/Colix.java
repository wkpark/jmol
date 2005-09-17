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

import org.jmol.util.Int2IntHash;
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


  private static int colixMax = Graphics3D.SPECIAL_COLIX_MAX;
  private static int[] argbs = new int[128];
  private static int[] argbsGreyscale;
  private static Color[] colors = new Color[128];
  private static int[][] ashades = new int[128][];
  private static int[][] ashadesGreyscale;
  private static final Int2IntHash colixHash = new Int2IntHash();

  final static short getColix(int argb) {
      return getColix(argb, null);
  }

  final static short getColix(Color color) {
      return color == null ? 0 : getColix(color.getRGB(), color);
  }

  private final static short getColix(int argb, Color opaqueColor) {
    if (argb == 0)
      return 0;
    int translucentMask = 0;
    if ((argb & 0xFF000000) != 0xFF000000) {
      if ((argb & 0xFF000000) == 0) {
        System.out.println("zero alpha channel + non-zero rgb not supported");
        throw new IndexOutOfBoundsException();
      }
      argb |= 0xFF000000;
      translucentMask = Graphics3D.TRANSLUCENT_MASK;
    }
    //    int c = colixHash.get(argb);
    //    if (c > 0)
    //	return (short) (c | translucentMask);
    int c = colixHash.get(argb);
    if (c > 0) {
	/*
	for (int i = colixMax; --i >= Graphics3D.SPECIAL_COLIX_MAX; )
	    if (argb == argbs[i]) {
		if (i != c)
		    throw new NullPointerException();
		return (short)(i | translucentMask);
	    }
	*/
	return (short)(c | translucentMask);
    }
    if (opaqueColor != null &&
	((opaqueColor.getRGB() & 0xFF000000) != 0xFF000000))
	opaqueColor = null;
    return (short)(allocateColix(argb, opaqueColor) | translucentMask);
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
      int newSize = oldSize * 2;
      int[] t0 = new int[newSize];
      System.arraycopy(argbs, 0, t0, 0, oldSize);
      argbs = t0;

      if (argbsGreyscale != null) {
        t0 = new int[newSize];
        System.arraycopy(argbsGreyscale, 0, t0, 0, oldSize);
        argbsGreyscale = t0;
      }

      Color[] t1 = new Color[newSize];
      System.arraycopy(colors, 0, t1, 0, oldSize);
      colors = t1;

      int[][] t2 = new int[newSize][];
      System.arraycopy(ashades, 0, t2, 0, oldSize);
      ashades = t2;

      if (ashadesGreyscale != null) {
        t2 = new int[newSize][];
        System.arraycopy(ashadesGreyscale, 0, t2, 0, oldSize);
        ashadesGreyscale = t2;
      }
    }
    argbs[colixMax] = argb;
    if (argbsGreyscale != null)
      argbsGreyscale[colixMax] = Graphics3D.calcGreyscaleRgbFromRgb(argb);
    colors[colixMax] = (opaqueColor != null) ? opaqueColor : new Color(argb);
    colixHash.put(argb, colixMax);
    return colixMax++;
  }

  private synchronized static void calcArgbsGreyscale() {
    if (argbsGreyscale == null) {
      argbsGreyscale = new int[argbs.length];
      for (int i = argbsGreyscale.length; --i >= 0; )
        argbsGreyscale[i] = Graphics3D.calcGreyscaleRgbFromRgb(argbs[i]);
    }
  }

  final static Color getColor(short colix) {
    if (colix == 0)
      return null;
    return colors[colix & Graphics3D.OPAQUE_MASK];
  }

  final static int getRgb(short colix) {
    return argbs[colix & Graphics3D.OPAQUE_MASK];
  }

  final static int getRgbGreyscale(short colix) {
    if (argbsGreyscale == null)
      calcArgbsGreyscale();
    return argbsGreyscale[colix & Graphics3D.OPAQUE_MASK];
  }

  final static boolean isTranslucent(short colix) {
    return (colix & Graphics3D.TRANSLUCENT_MASK) != 0;
  }

  final static int[] getShades(short colix) {
    colix &= Graphics3D.OPAQUE_MASK;
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = Shade3D.getShades(argbs[colix], false);
    return shades;
  }

  final static int[] getShadesGreyscale(short colix) {
    colix &= Graphics3D.OPAQUE_MASK;
    if (ashadesGreyscale == null)
      ashadesGreyscale = new int[ashades.length][];
    int[] shadesGreyscale = ashadesGreyscale[colix];
    if (shadesGreyscale == null)
      shadesGreyscale = ashadesGreyscale[colix] =
        Shade3D.getShades(argbs[colix], true);
    return shadesGreyscale;
  }

  final static void flushShades() {
    for (int i = colixMax; --i >= 0; )
      ashades[i] = null;
  }

  static int mixCacheCount = 0;
  static int[] mixCacheMixIds = new int[32];
  static short[] mixCacheColixes = new short[32];

  final static short getColixMix(short colixA, short colixB) {
    if (colixA == colixB)
      return colixA;
    if (colixA <= 0)
      return colixB;
    if (colixB <= 0)
      return colixA;
    int mixId = ((colixA < colixB)
                 ? ((colixA << 16) | colixB)
                 : ((colixB << 16) | colixA));
    for (int i = 0; i < mixCacheCount; ++i)
      if (mixId == mixCacheMixIds[i])
        return mixCacheColixes[i];
    int argbA = argbs[colixA];
    int argbB = argbs[colixB];
    int r = (((argbA & 0x00FF0000) + (argbB & 0x00FF0000)) >> 1) & 0x00FF0000;
    int g = (((argbA & 0x0000FF00) + (argbB & 0x0000FF00)) >> 1) & 0x0000FF00;
    int b = (((argbA & 0x000000FF) + (argbB & 0x000000FF)) >> 1);
    int argbMixed = 0xFF000000 | r | g | b;
    short mixedColix = getColix(argbMixed);
    return addMixed(mixId, mixedColix);
  }

  private synchronized static short addMixed(int mixId, short mixedColix) {
    if (mixCacheCount == mixCacheMixIds.length) {
      int[] t1 = new int[2 * mixCacheCount];
      short[] t2 = new short[2 * mixCacheCount];
      for (int i = mixCacheCount; --i >= 0; ) {
        t1[i] = mixCacheMixIds[i];
        t2[i] = mixCacheColixes[i];
      }
      mixCacheMixIds = t1;
      mixCacheColixes = t2;
    }
    mixCacheMixIds[mixCacheCount] = mixId;
    mixCacheColixes[mixCacheCount] = mixedColix;
    ++mixCacheCount;
    return mixedColix;
  }
}
