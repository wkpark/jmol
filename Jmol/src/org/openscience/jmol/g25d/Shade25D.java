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

public class Shade25D {

  public static final int shadeNormal = 64;
  public static final int shadeMax = 128;

  Color color;
  int[] shades = new int[shadeMax];

  private Shade25D(Color color) {
    this.color = color;
    initialize(color.getRGB());
  }

  private static Hashtable htCache = new Hashtable();

  public Shade25D getShade(Color color) {
    Shade25D shade = (Shade25D) htCache.get(color);
    if (shade == null) {
      shade = new Shade25D(color);
      htCache.put(color, shade);
    }
    return shade;
  }

  public Shade25D getShade(int rgb) {
    return getShade(new Color(rgb));
  }

  public static int[] getShades(Color color) {
    Shade25D sp = (Shade25D) htCache.get(color);
    if (sp == null) {
      sp = new Shade25D(color);
      htCache.put(color, sp);
    }
    return sp.shades;
  }

  public static int[] getShades(int rgb) {
    return getShades(new Color(rgb));
  }
  
  private void initialize(int rgb) {
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    shades[shadeNormal] = rgb(red, grn, blu);
    for (int i = shadeNormal; --i >= 0; ) {
      shades[i] = rgb(red * i / shadeNormal,
                      grn * i / shadeNormal,
                      blu * i / shadeNormal);
    }

    int nSteps = shadeMax - shadeNormal - 1;
    for (int i = 1; i <= nSteps; ++i) {
      int redT = red + (255 - red) * i / nSteps;
      int grnT = grn + (255 - grn) * i / nSteps;
      int bluT = blu + (255 - blu) * i / nSteps;
      shades[shadeNormal + i] = rgb(redT, grnT, bluT);
    }
  }

  private final static int rgb(int red, int grn, int blu) {
    return 0xFF000000 | (red << 16) | (grn << 8) | blu;
  }

  static String StringFromRgb(int rgb) {
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    return "[" + red + "," + grn + "," + blu + "]";
  }

  public String toString() {
    String str = "Shades for:" + StringFromRgb(shades[shadeNormal]) + "\n";
    for (int shade = 0; shade < shadeMax; ++shade)
      str += StringFromRgb(shades[shade]) + "\n";
    str += "\n";
    return str;
  }
}
