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
import java.util.Hashtable;

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
  Component component;
  int height; // this height is just ascent + descent ... no reason for leading
  int ascent;
  int width;
  int size;
  int[] bitmap;

  public Text25D(String text, Font font, Component component) {
    if (g == null)
      checkImageBufferSize(component, 128, 16);
    calcMetrics(text, font);
    checkImageBufferSize(component, width, height);
    renderImage(text, font);
    System.out.println("text:" + text +
                       " appears to have been rendered properly");
    rasterize();
  }

  static int widthBuffer;
  static int heightBuffer;
  static Image img;
  static Graphics g;

  void checkImageBufferSize(Component component, int width, int height) {
    boolean realloc = false;
    int widthT = widthBuffer;
    int heightT = heightBuffer;
    if (width > widthT) {
      widthT = (width + 63) & ~63;
      realloc = true;
    }
    if (height > heightT) {
      heightT = (height + 7) & ~7;
      realloc = true;
    }
    if (realloc) {
      if (g != null)
        g.dispose();
      img = component.createImage(widthT, heightT);
      widthBuffer = widthT;
      heightBuffer = heightT;
      g = img.getGraphics();
    }
  }

  void calcMetrics(String text, Font font) {
    System.out.println("calcMetrics(" + text + "," + font + ")");
    FontMetrics fontMetrics = g.getFontMetrics(font);
    ascent = fontMetrics.getAscent();
    height = ascent + fontMetrics.getDescent();
    width = fontMetrics.stringWidth(text);
    size = width*height;
    System.out.println("CalcMetrics width=" + width + " height=" + height);
  }

  void renderImage(String text, Font font) {
    g.setColor(Color.black);
    g.fillRect(0, 0, width, height);
    g.setColor(Color.white);
    g.setFont(font);
    g.drawString(text, 0, ascent);
  }

  void rasterize() {
    int[] pixels = new int[size];
    System.out.println("img == null : " + (img == null));
    System.out.println("PixelGrabber(width="  + width +
                       " height=" + height +
                       " pixels size=" + size);
    PixelGrabber pixelGrabber = new PixelGrabber(img, 0, 0, width, height,
                                                 pixels, 0, width);
    pixelGrabber.startGrabbing();
    // shifter error checking
    boolean[] bits = new boolean[size];
    for (int i = 0; i < size; ++i)
      bits[i] = (pixels[i] & 0x00FFFFFF) != 0;
    //

    int bitmapSize = (size + 31) >> 5;
    bitmap = new int[bitmapSize];

    int offset, shifter;
    for (offset = shifter = 0; offset < size; ++offset, shifter <<= 1) {
      if ((pixels[offset] & 0x00FFFFFF) != 0)
        shifter |= 1;
      if ((offset & 31) == 31)
        bitmap[offset >> 5] = shifter;
    }
    if ((offset & 31) != 0) {
      shifter <<= 31 - (offset & 31);
      bitmap[offset >> 5] = shifter;
    }

    // error checking
    for (offset = 0; offset < size; ++offset, shifter <<= 1) {
      if ((offset & 31) == 0)
        shifter = bitmap[offset >> 5];
      if (shifter < 0) {
        if (!bits[offset]) {
          System.out.println("false positive @" + offset);
          System.out.println("size = " + size);
        }
      } else {
        if (bits[offset]) {
          System.out.println("false negative @" + offset);
          System.out.println("size = " + size);
        }
      }
    }
    // error checking
  }

  static Hashtable htText = new Hashtable();
  
  // FIXME mth
  // we have a synchronization issue/race condition  here with multiple
  // so only one Text25D can be generated at a time

  synchronized static Text25D getText25D(String text, Font font,
                                         Component component) {
    int size = font.getSize();
    Text25D[] at25d = (Text25D[])htText.get(text);
    if (at25d != null) {
      if (size <= at25d.length) {
        Text25D t25d = at25d[size - 1];
        if (t25d != null)
          return t25d;
      } else {
        Text25D[] at25dNew = new Text25D[size + 8];
        System.arraycopy(at25d, 0, at25dNew, 0, at25d.length);
        at25d = at25dNew;
        htText.put(text, at25d);
      }
    } else {
      at25d = new Text25D[size + 8];
      htText.put(text, at25d);
    }
    return at25d[size - 1] = new Text25D(text, font, component);
  }

  public static void plot(int x, int y, int z, int argb,
                          String text, Font font, Graphics25D g25d,
                          Component component) {
    Text25D text25d = getText25D(text, font, component);
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

