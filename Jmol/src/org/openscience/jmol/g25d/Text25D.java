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

import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.image.PixelGrabber;

public class Text25D {
  /*
    we have a few problems here
    a message is probably going to vary in size with z depth
    a message is probably going to be repeated by more than one atom
    fonts?
      just one?
      a restricted number?
      any font?
      if we want to support more than one then a fontindex is probably
      called for in order to prevent a hashtable lookup
    color - can be applied by the painter
    rep
      array of booleans - uncompressed
      array of bits - uncompressed - i like this
      some type of run-length, using bytes
  */
  String text;
  Font font;
  Component component;
  int height;
  int ascent;
  int width;
  int size;
  int[] pixels;
  boolean[] bits;

  int[] bitmap;

  public Text25D(String text, Font font, Component component) {
    this.text = text;
    this.font = font;
    this.component = component;
    calcMetrics();
    renderImage();
    rasterize();
  }

  static int widthImg = 128;
  static int heightImg = 16;
  static Image img;
  static Graphics g;
  void allocImage() {
    if (g != null)
      g.dispose();
    img = component.createImage(widthImg, heightImg);
    g = img.getGraphics();
  }

  void checkImageSize() {
    boolean realloc = false;
    if (width > widthImg) {
      widthImg = (width + 63) & ~63;
      realloc = true;
    }
    if (height > heightImg) {
      heightImg = (height + 7) & ~7;
      realloc = true;
    }
    if (realloc)
      allocImage();
  }

  void calcMetrics() {
    if (g == null)
      allocImage();
    FontMetrics fontMetrics = g.getFontMetrics(font);
    height = fontMetrics.getHeight();
    width = fontMetrics.stringWidth(text);
    size = width*height;
    ascent = fontMetrics.getAscent();
    // perhaps I should use getMaxAscent + getMaxDescent
    checkImageSize();
  }

  void renderImage() {
    g.setColor(Color.black);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.white);
    g.setFont(font);
    g.drawString(text, 0, ascent);
    g.dispose();
  }

  void rasterize() {
    pixels = new int[size];
    PixelGrabber pixelGrabber = new PixelGrabber(img, 0, 0, width, height,
                                                 pixels, 0, width);
    pixelGrabber.startGrabbing();
    bits = new boolean[size];
    for (int i = 0; i < size; ++i)
      bits[i] = (pixels[i] & 0x00FFFFFF) != 0;

    bitmap = new int[(size + 31) & ~31];

    /*
    int offset = 0;
    int shiftregister = 0;
    while (offset < size) {
    */
    int offset, shifter;
    for (offset = shifter = 0; offset < size; ++offset, shifter <<= 1) {
      //      if (bits[offset])
      if ((pixels[offset] & 0x00FFFFFF) != 0)
        shifter |= 1;
      if ((offset & 31) == 31)
        bitmap[offset >> 5] = shifter;
    }
    if ((offset & 31) != 0) {
      shifter <<= 32 - (offset & 31);
      bitmap[offset >> 5] = shifter;
    }

    for (offset = 0; offset < size; ++offset, shifter <<= 1) {
      if ((offset & 31) == 0)
        shifter = bitmap[offset >> 5];
      if (shifter < 0) {
        if (!bits[offset])
          System.out.println("false positive @" + offset);
      } else {
        if (bits[offset])
          System.out.println("false negative @" + offset);
      }
    }
  }

  public static void plot(int x, int y, int z, int argb,
                          String text, Font font, Graphics25D g25d,
                          Component component) {
    Text25D text25d = new Text25D(text, font, component);
    int offset = 0;
    int shiftregister = 0;
    int i = 0, j = 0;
    while (i < text25d.height) {
      while (j < text25d.width) {
        if ((offset & 31) == 0)
          shiftregister = text25d.bitmap[offset >> 5];
        if (shiftregister == 0) {
          int skip = 32 - (offset & 31);
          j += skip;
          offset += skip;
        } else {
          if (shiftregister < 0)
            g25d.plotPixelClipped(argb, x + j, y + i, z);
          shiftregister <<= 1;
          ++offset;
          ++j;
        }
      }
      while (j >= text25d.width) {
        ++i;
        j -= text25d.width;
      }
    }
  }
}

